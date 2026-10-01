package com.archery.range;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.archery.range.common.ForbiddenException;
import com.archery.range.dto.LostFoundDtos;
import com.archery.range.repository.LostFoundItemRepository;
import com.archery.range.service.LostFoundService;

/** 失物保管与认领交接台关键状态机与最小可见性集成测试。 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class LostFoundFlowIntegrationTest {

    @Autowired
    private LostFoundService service;
    @Autowired
    private LostFoundItemRepository itemRepository;

    private LostFoundDtos.RegisterReq registerReq(String requestKey) {
        return new LostFoundDtos.RegisterReq(
                "ELECTRONICS", "LANE", "A02 等候椅", LocalDateTime.now().minusHours(2),
                "黑色手机壳，边角有划痕", List.of("photo-001.jpg"),
                "STAFF", null, "教练-林舟", null,
                "前台-马倩", "前台失物柜 A 格", false, false,
                "FRONT", "前台-马倩", requestKey);
    }

    private LostFoundDtos.ClaimReq claimReq(Long itemId, String key) {
        LocalDateTime now = LocalDateTime.now();
        return new LostFoundDtos.ClaimReq(
                "GUEST", null, "访客王一", "13800001111", "黑色手机，壳内夹蓝色箭馆小票",
                now.minusHours(4), now.minusHours(1), "A02 / 反曲弓体验课",
                "锁屏壁纸是一只白猫", List.of("evidence-001.jpg"),
                "FRONT", "前台-马倩", key);
    }

    @Test
    void register_review_exception_doubleReturn_appendsChain() {
        var item = service.register(registerReq("lf-test-register"));
        Long itemId = item.summary().id();
        assertThat(item.summary().sealStatus()).isEqualTo("INTACT");
        assertThat(item.logs()).extracting(LostFoundDtos.ChainView::action).contains("REGISTER");

        // 教练不能看完整照片和联系方式。
        var coachView = service.detail(itemId, "COACH");
        assertThat(coachView.photoRefs()).isEmpty();
        assertThat(coachView.finderContact()).isNull();
        assertThatThrownBy(() -> service.list("MEMBER")).isInstanceOf(ForbiddenException.class);

        // 同一 requestKey 重试不能再产生一条交接。
        var transfer = new LostFoundDtos.TransferReq(
                "FRONT", "前台-马倩", "前台-周宁", "失物柜 B 格", "夜班交接", "lf-test-transfer");
        service.transfer(itemId, transfer);
        assertThatThrownBy(() -> service.transfer(itemId, transfer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("未重复生成");

        var claim = service.createClaim(itemId, claimReq(itemId, "lf-test-claim"));
        Long claimId = claim.summary().id();

        var approved = service.review(claimId, new LostFoundDtos.ReviewReq(
                "MANAGER", "经理-陈默", "PASS", "PASS", "PASS", "APPROVED", "三项均吻合",
                "lf-test-review"));
        assertThat(approved.summary().status()).isEqualTo("APPROVED");

        // 封签破损转入异常，已通过审核保留但双人交还暂停。
        var exception = service.reportException(claimId, new LostFoundDtos.ExceptionReq(
                "FRONT", "前台-周宁", "BROKEN_SEAL", "取件时发现外封签边缘开裂",
                "lf-test-exception"));
        assertThat(exception.summary().status()).isEqualTo("EXCEPTION");

        // 异常未解除不能双人交还。
        var returnReq = new LostFoundDtos.ReturnReq(
                "FRONT", "前台-周宁", "教练-林舟", "访客王一", "本人",
                "身份证原件 + 手机号", null, "lf-test-return-early");
        assertThatThrownBy(() -> service.completeReturn(claimId, returnReq))
                .hasMessageContaining("异常");

        var exceptionId = exception.exceptions().get(0).id();
        service.resolveException(exceptionId, new LostFoundDtos.ResolveExceptionReq(
                "MANAGER", "经理-陈默", "RESEAL", "S-RES-001", "失物柜 B 格", null,
                "核对实物后重新封存", "lf-test-resolve"));

        var returned = service.completeReturn(claimId, new LostFoundDtos.ReturnReq(
                "FRONT", "前台-周宁", "教练-林舟", "访客王一", "本人",
                "身份证原件 + 手机号", null, "lf-test-return"));
        assertThat(returned.summary().status()).isEqualTo("RETURNED");
        assertThat(returned.returnCustodian()).isEqualTo("前台-周宁");
        assertThat(returned.returnWitness()).isEqualTo("教练-林舟");
        assertThat(service.detail(itemId, "FRONT").summary().status()).isEqualTo("RETURNED");
    }

    @Test
    void rejectedClaimCannotReturnAndHighValueOverdueMustGoPolice() {
        var item = service.register(registerReq("lf-test-reject-item"));
        Long itemId = item.summary().id();
        var claim = service.createClaim(itemId, claimReq(itemId, "lf-test-reject-claim"));

        var rejected = service.review(claim.summary().id(), new LostFoundDtos.ReviewReq(
                "MANAGER", "经理-陈默", "FAIL", "PASS", "PARTIAL", "REJECTED", "隐私特征不符",
                "lf-test-reject-review"));
        assertThat(rejected.summary().status()).isEqualTo("REJECTED");

        assertThatThrownBy(() -> service.completeReturn(claim.summary().id(), new LostFoundDtos.ReturnReq(
                "FRONT", "前台-马倩", "教练-林舟", "访客王一", "本人", "身份证", null,
                "lf-test-reject-return"))).hasMessageContaining("待交还");

        itemRepository.findByIdForUpdate(itemId).ifPresent(entity -> {
            entity.setStatus("CUSTODY");
            entity.setRetentionUntil(LocalDate.now().minusDays(1));
            entity.setHighValue(true);
            itemRepository.save(entity);
        });
        assertThatThrownBy(() -> service.dispose(itemId, new LostFoundDtos.DisposeReq(
                "MANAGER", "经理-陈默", "DONATE", "慈善机构", "D-001",
                List.of("receipt.jpg"), null, "lf-test-dispose")))
                .hasMessageContaining("公安");
    }
}
