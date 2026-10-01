package com.archery.range.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.archery.range.common.BizException;
import com.archery.range.common.ForbiddenException;
import com.archery.range.domain.LostFoundChainLog;
import com.archery.range.domain.LostFoundClaim;
import com.archery.range.domain.LostFoundClaimSupplement;
import com.archery.range.domain.LostFoundDisposal;
import com.archery.range.domain.LostFoundException;
import com.archery.range.domain.LostFoundItem;
import com.archery.range.domain.LostFoundReview;
import com.archery.range.domain.LostFoundSeal;
import com.archery.range.dto.LostFoundDtos;
import com.archery.range.repository.LostFoundChainLogRepository;
import com.archery.range.repository.LostFoundClaimRepository;
import com.archery.range.repository.LostFoundClaimSupplementRepository;
import com.archery.range.repository.LostFoundDisposalRepository;
import com.archery.range.repository.LostFoundExceptionRepository;
import com.archery.range.repository.LostFoundItemRepository;
import com.archery.range.repository.LostFoundReviewRepository;
import com.archery.range.repository.LostFoundSealRepository;
import com.archery.range.repository.MemberRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 失物保管与认领交接台。
 *
 * 原登记和每次交接均以追加记录保留：物品表只保存当前现状，封签、审核、异常、处置和统一 chain log
 * 都只插入不更新；状态推进与交接链写入在同一事务内，校验失败整体回滚，重试不会产生第二条交接。
 */
@Service
public class LostFoundService {

    private static final DateTimeFormatter NO_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> STAFF = Set.of("FRONT", "MANAGER", "COACH");
    private static final Set<String> CATEGORIES = Set.of(
            "ELECTRONICS", "CARD", "CLOTHING", "EQUIPMENT", "ACCESSORY", "STATIONERY", "OTHER");
    private static final Set<String> AREAS = Set.of("LANE", "COURSE", "PUBLIC");
    private static final Set<String> FINDERS = Set.of("MEMBER", "STAFF", "GUEST");
    private static final Set<String> CLAIMANTS = Set.of("MEMBER", "GUEST");
    private static final Set<String> REVIEW_CONCLUSIONS = Set.of("PASS", "PARTIAL", "FAIL");
    private static final Set<String> EXCEPTION_REASONS = Set.of(
            "BROKEN_SEAL", "ITEM_MISMATCH", "RECEIVER_CHANGE", "CUSTODIAN_ABSENT");

    private final LostFoundItemRepository itemRepository;
    private final LostFoundSealRepository sealRepository;
    private final LostFoundClaimRepository claimRepository;
    private final LostFoundReviewRepository reviewRepository;
    private final LostFoundClaimSupplementRepository supplementRepository;
    private final LostFoundExceptionRepository exceptionRepository;
    private final LostFoundDisposalRepository disposalRepository;
    private final LostFoundChainLogRepository chainRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    public LostFoundService(LostFoundItemRepository itemRepository,
            LostFoundSealRepository sealRepository,
            LostFoundClaimRepository claimRepository,
            LostFoundReviewRepository reviewRepository,
            LostFoundClaimSupplementRepository supplementRepository,
            LostFoundExceptionRepository exceptionRepository,
            LostFoundDisposalRepository disposalRepository,
            LostFoundChainLogRepository chainRepository,
            MemberRepository memberRepository,
            ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.sealRepository = sealRepository;
        this.claimRepository = claimRepository;
        this.reviewRepository = reviewRepository;
        this.supplementRepository = supplementRepository;
        this.exceptionRepository = exceptionRepository;
        this.disposalRepository = disposalRepository;
        this.chainRepository = chainRepository;
        this.memberRepository = memberRepository;
        this.objectMapper = objectMapper;
    }

    // ---------------- 查询与脱敏 ----------------

    @Transactional(readOnly = true)
    public List<LostFoundDtos.PublicItemView> publicList() {
        return itemRepository.findByStatusOrderByIdDesc("CUSTODY").stream()
                .map(item -> new LostFoundDtos.PublicItemView(
                        item.getId(), item.getItemNo(), item.getPublicCategory(),
                        name("publicCategory", item.getPublicCategory()), item.getFoundAt().toLocalDate(),
                        item.getFoundAreaType(), name("area", item.getFoundAreaType())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LostFoundDtos.ItemSummary> list(String role) {
        requireStaff(role);
        return itemRepository.findAllByOrderByIdDesc().stream().map(item -> toSummary(item, role)).toList();
    }

    @Transactional(readOnly = true)
    public List<LostFoundDtos.ClaimSummary> claims(String role, String status) {
        requireStaff(role);
        List<LostFoundClaim> claims = status == null || status.isBlank()
                ? claimRepository.findAllByOrderByIdDesc()
                : claimRepository.findByStatusOrderByIdDesc(status);
        return claims.stream().map(claim -> toClaimSummary(claim, role)).toList();
    }

    @Transactional(readOnly = true)
    public List<LostFoundDtos.DisposalView> disposals(String role) {
        requireStaff(role);
        return disposalRepository.findAllByOrderByIdDesc().stream().map(this::toDisposal).toList();
    }

    @Transactional(readOnly = true)
    public LostFoundDtos.ItemDetail detail(Long id, String role) {
        requireStaff(role);
        LostFoundItem item = itemRepository.findById(id)
                .orElseThrow(() -> new BizException("失物不存在：id=" + id));
        List<LostFoundSeal> seals = sealRepository.findByItemIdOrderByIdAsc(id);
        List<LostFoundClaim> claims = claimRepository.findByItemIdOrderByIdDesc(id);
        List<LostFoundDisposal> disposals = disposalRepository.findByItemIdOrderByIdDesc(id);
        List<LostFoundChainLog> logs = chainRepository.findByItemIdOrderByIdAsc(id);

        boolean frontOnly = "FRONT".equals(role);
        boolean staffDetail = frontOnly || "MANAGER".equals(role);
        return new LostFoundDtos.ItemDetail(
                toSummary(item, role),
                staffDetail ? item.getAppearance() : "当前角色不能查看完整外观特征",
                frontOnly ? readList(item.getPhotoRefs()) : List.of(),
                item.getFinderType(),
                item.getFinderName(),
                frontOnly ? item.getFinderContact() : null,
                staffDetail ? item.getFinderMemberId() : null,
                seals.stream().map(this::toSeal).toList(),
                claims.stream().map(claim -> toClaimSummary(claim, role)).toList(),
                disposals.stream().map(this::toDisposal).toList(),
                logs.stream().map(this::toChain).toList(),
                itemActions(item, role));
    }

    @Transactional(readOnly = true)
    public LostFoundDtos.ClaimDetail claimDetail(Long id, String role) {
        requireStaff(role);
        LostFoundClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new BizException("认领申请不存在：id=" + id));
        boolean privileged = "FRONT".equals(role) || "MANAGER".equals(role);
        String hidden = "仅值班经理与办理前台在必要时可见";
        List<LostFoundException> exceptions = exceptionRepository.findByClaimIdOrderByIdDesc(id);
        return new LostFoundDtos.ClaimDetail(
                toClaimSummary(claim, role),
                "FRONT".equals(role) ? claim.getClaimantContact() : hidden,
                privileged ? claim.getDescription() : hidden,
                claim.getLostTimeStart(),
                claim.getLostTimeEnd(),
                privileged ? claim.getPossibleAreas() : hidden,
                privileged ? claim.getPrivacyFeature() : hidden,
                privileged ? readList(claim.getEvidenceRefs()) : List.of(),
                claim.getActualReceiver(),
                claim.getReceiverRelation(),
                claim.getIdVerifyMethod(),
                claim.getReturnCustodian(),
                claim.getReturnWitness(),
                claim.getReturnedAt(),
                claim.getReturnNote(),
                reviewRepository.findByClaimIdOrderByIdAsc(id).stream().map(this::toReview).toList(),
                supplementRepository.findByClaimIdOrderByIdAsc(id).stream()
                        .map(s -> new LostFoundDtos.SupplementView(
                                s.getId(), privileged ? readList(s.getEvidenceRefs()) : List.of(),
                                s.getNote(), s.getSubmittedBy(), s.getCreatedAt()))
                        .toList(),
                exceptions.stream().map(e -> toException(e, role, item)).toList(),
                chainRepository.findByItemIdOrderByIdAsc(claim.getItemId()).stream()
                        .filter(log -> log.getClaimId() == null || log.getClaimId().equals(id))
                        .map(this::toChain).toList());
    }

    // ---------------- 登记、首次封存与保管 ----------------

    @Transactional
    public LostFoundDtos.ItemDetail register(LostFoundDtos.RegisterReq req) {
        requireRole(req.role(), "FRONT");
        assertFresh(req.requestKey());
        String operator = name(req.operator());
        String category = valid(req.category(), CATEGORIES, "物品类别");
        String areaType = valid(req.foundAreaType(), AREAS, "发现区域");
        String finderType = valid(req.finderType(), FINDERS, "发现人身份");
        if (req.foundAt() == null || req.foundAt().isAfter(LocalDateTime.now())) {
            throw new BizException("发现时间不能晚于当前时间");
        }
        List<String> photos = cleanRefs(req.photoRefs(), "现场照片");
        String detail = trim(req.foundAreaDetail(), "精确发现位置");
        String appearance = trim(req.appearance(), "外观特征");
        String finderName = name(req.finderName());
        Long memberId = null;
        if ("MEMBER".equals(finderType)) {
            if (req.finderMemberId() == null) {
                throw new BizException("发现人为会员时请选择会员档案");
            }
            memberId = memberRepository.findByIdAndStatus(req.finderMemberId(), "ACTIVE")
                    .orElseThrow(() -> new BizException("所选会员不存在或已封存")).getId();
        }

        LocalDateTime now = LocalDateTime.now();
        LostFoundItem item = new LostFoundItem();
        item.setItemNo(nextNo("LF", itemRepository.count()));
        item.setCategory(category);
        item.setPublicCategory(publicCategory(category));
        item.setFoundAreaType(areaType);
        item.setFoundAreaDetail(detail);
        item.setFoundAt(req.foundAt());
        item.setAppearance(appearance);
        item.setPhotoRefs(writeList(photos));
        item.setFinderType(finderType);
        item.setFinderMemberId(memberId);
        item.setFinderName(finderName);
        item.setFinderContact(trimToNull(req.finderContact()));
        item.setCurrentCustodian(name(req.custodian()));
        item.setStorageArea(trim(req.storageArea(), "存放区域"));
        item.setSealNo("S" + now.format(NO_STAMP) + "-" + (itemRepository.count() + 1));
        item.setSealStatus("INTACT");
        item.setSealedAt(now);
        item.setHighValue(req.highValue());
        item.setIdentityItem(req.identityItem());
        item.setStatus("CUSTODY");
        item.setRetentionUntil(LocalDate.now().plusDays(90));
        item.setRegisteredBy(operator);
        item.setCreatedAt(now);
        itemRepository.save(item);

        createSeal(item, "INITIAL", null, "INTACT", item.getCurrentCustodian(), item.getStorageArea(),
                "首次登记后生成失物封签", operator, "FRONT", now);
        chain(item, null, "REGISTER", null, "CUSTODY", operator, "FRONT",
                "发现人 " + finderName + " 在" + name("area", areaType) + " " + detail
                        + " 发现物品；前台生成封签 " + item.getSealNo()
                        + "，首次移交保管人 " + item.getCurrentCustodian() + " / " + item.getStorageArea(),
                req.requestKey(), now);
        return detail(item.getId(), "FRONT");
    }

    @Transactional
    public LostFoundDtos.ItemDetail transfer(Long id, LostFoundDtos.TransferReq req) {
        requireRole(req.role(), "FRONT");
        assertFresh(req.requestKey());
        LostFoundItem item = lockItem(id);
        requireCustody(item);
        String operator = name(req.operator());
        if (!operator.equals(item.getCurrentCustodian())) {
            throw new BizException("转移必须由当前保管人 " + item.getCurrentCustodian() + " 发起");
        }
        if (hasOpenException(item.getId(), null)) {
            throw new BizException("存在未处理的交接异常，不能转移，请先由值班经理处理");
        }
        String next = name(req.newCustodian());
        String storage = trim(req.newStorageArea(), "新存放区域");
        if (next.equals(item.getCurrentCustodian()) && storage.equals(item.getStorageArea())) {
            throw new BizException("新保管人与存放区域不能与现状完全相同");
        }
        String oldCustodian = item.getCurrentCustodian();
        String oldStorage = item.getStorageArea();
        item.setCurrentCustodian(next);
        item.setStorageArea(storage);
        itemRepository.save(item);
        chain(item, null, "TRANSFER", "CUSTODY", "CUSTODY", operator, "FRONT",
                "保管转移：" + oldCustodian + " / " + oldStorage + " → " + next + " / " + storage
                        + "；原因：" + trim(req.reason(), "转移原因"),
                req.requestKey(), LocalDateTime.now());
        return detail(id, "FRONT");
    }

    @Transactional
    public LostFoundDtos.ItemDetail inspect(Long id, LostFoundDtos.InspectReq req) {
        requireRole(req.role(), "FRONT");
        assertFresh(req.requestKey());
        LostFoundItem item = lockItem(id);
        requireCustody(item);
        String operator = name(req.operator());
        if (!operator.equals(item.getCurrentCustodian())) {
            throw new BizException("开封查看必须由当前保管人 " + item.getCurrentCustodian() + " 执行");
        }
        if (!"INTACT".equals(item.getSealStatus())) {
            throw new BizException("封签当前为「" + sealName(item.getSealStatus()) + "」，不能重复开封");
        }
        String reason = trim(req.reason(), "开封原因");
        LocalDateTime now = LocalDateTime.now();
        item.setSealStatus("OPEN");
        itemRepository.save(item);
        createSeal(item, "OPEN", "INTACT", "OPEN", item.getCurrentCustodian(), item.getStorageArea(),
                reason, operator, "FRONT", now);
        chain(item, null, "INSPECT", "CUSTODY", "CUSTODY", operator, "FRONT",
                "当前保管人现场开封查看：" + reason, req.requestKey(), now);
        return detail(id, "FRONT");
    }

    @Transactional
    public LostFoundDtos.ItemDetail reseal(Long id, LostFoundDtos.ResealReq req) {
        requireRole(req.role(), "FRONT");
        assertFresh(req.requestKey());
        LostFoundItem item = lockItem(id);
        requireCustody(item);
        String operator = name(req.operator());
        if (!operator.equals(item.getCurrentCustodian())) {
            throw new BizException("重新封存必须由当前保管人 " + item.getCurrentCustodian() + " 执行");
        }
        if (!"OPEN".equals(item.getSealStatus())) {
            throw new BizException("只有已开封状态可以重新封存；破损封签须走异常处理");
        }
        String sealNo = trim(req.newSealNo(), "新封签号");
        String storage = trim(req.storageArea(), "存放区域");
        LocalDateTime now = LocalDateTime.now();
        item.setSealNo(sealNo);
        item.setSealStatus("INTACT");
        item.setSealedAt(now);
        item.setStorageArea(storage);
        itemRepository.save(item);
        createSeal(item, "RESEAL", "OPEN", "INTACT", item.getCurrentCustodian(), storage,
                trim(req.reason(), "重新封存原因"), operator, "FRONT", now);
        chain(item, null, "RESEAL", "CUSTODY", "CUSTODY", operator, "FRONT",
                "查看后重新封存，新封签 " + sealNo + "，存放于 " + storage, req.requestKey(), now);
        return detail(id, "FRONT");
    }

    // ---------------- 认领与三项审核 ----------------

    @Transactional
    public LostFoundDtos.ClaimDetail createClaim(Long itemId, LostFoundDtos.ClaimReq req) {
        requireRole(req.role(), "FRONT");
        assertFresh(req.requestKey());
        LostFoundItem item = lockItem(itemId);
        requireCustody(item);
        String claimantType = valid(req.claimantType(), CLAIMANTS, "认领人身份");
        Long memberId = null;
        if ("MEMBER".equals(claimantType)) {
            if (req.claimantMemberId() == null) {
                throw new BizException("认领人为会员时请选择会员档案");
            }
            memberId = memberRepository.findByIdAndStatus(req.claimantMemberId(), "ACTIVE")
                    .orElseThrow(() -> new BizException("所选认领会员不存在或已封存")).getId();
        }
        if (req.lostTimeStart() == null || req.lostTimeEnd() == null
                || req.lostTimeStart().isAfter(req.lostTimeEnd())) {
            throw new BizException("遗失时间范围不完整或起止顺序错误");
        }
        if (req.lostTimeEnd().isAfter(LocalDateTime.now())) {
            throw new BizException("遗失时间范围不能晚于当前时间");
        }
        if (hasActiveClaim(itemId)) {
            throw new BizException("该物品已有审核中、待交还或异常处理中的认领申请");
        }
        List<String> evidence = cleanRefs(req.evidenceRefs(), "补充证明");
        LocalDateTime now = LocalDateTime.now();
        LostFoundClaim claim = new LostFoundClaim();
        claim.setClaimNo(nextNo("CL", claimRepository.count()));
        claim.setItemId(itemId);
        claim.setClaimantType(claimantType);
        claim.setClaimantMemberId(memberId);
        claim.setClaimantName(name(req.claimantName()));
        claim.setClaimantContact(trim(req.claimantContact(), "认领人联系方式"));
        claim.setDescription(trim(req.description(), "认领描述"));
        claim.setLostTimeStart(req.lostTimeStart());
        claim.setLostTimeEnd(req.lostTimeEnd());
        claim.setPossibleAreas(trim(req.possibleAreas(), "可能出现的箭道或课程"));
        claim.setPrivacyFeature(trim(req.privacyFeature(), "隐私特征"));
        claim.setEvidenceRefs(writeList(evidence));
        claim.setStatus("PENDING");
        claim.setSubmittedBy(name(req.operator()));
        claim.setSubmittedAt(now);
        claimRepository.save(claim);
        chain(item, claim.getId(), "CLAIM_SUBMIT", "CUSTODY", "CUSTODY", claim.getSubmittedBy(), "FRONT",
                "认领人 " + claim.getClaimantName() + " 提交认领，等待值班经理按特征、时间地点、补充证明分别审核",
                req.requestKey(), now);
        return claimDetail(claim.getId(), "FRONT");
    }

    @Transactional
    public LostFoundDtos.ClaimDetail supplement(Long claimId, LostFoundDtos.SupplementReq req) {
        requireRole(req.role(), "FRONT");
        assertFresh(req.requestKey());
        LostFoundClaim claim = lockClaim(claimId);
        if (!"SUPPLEMENT".equals(claim.getStatus())) {
            throw new BizException("只有经理要求补证的申请可以提交补证材料");
        }
        LostFoundItem item = lockItem(claim.getItemId());
        List<String> refs = cleanRefs(req.evidenceRefs(), "补证材料");
        LostFoundClaimSupplement supplement = new LostFoundClaimSupplement();
        supplement.setClaimId(claimId);
        supplement.setItemId(claim.getItemId());
        supplement.setEvidenceRefs(writeList(refs));
        supplement.setNote(trim(req.note(), "补证说明"));
        supplement.setSubmittedBy(name(req.operator()));
        supplement.setCreatedAt(LocalDateTime.now());
        supplementRepository.save(supplement);
        claim.setStatus("PENDING");
        claimRepository.save(claim);
        chain(item, claimId, "SUPPLEMENT", "SUPPLEMENT", "PENDING", supplement.getSubmittedBy(), "FRONT",
                "认领人补充证明 " + refs.size() + " 份：" + supplement.getNote(),
                req.requestKey(), supplement.getCreatedAt());
        return claimDetail(claimId, "FRONT");
    }

    @Transactional
    public LostFoundDtos.ClaimDetail review(Long claimId, LostFoundDtos.ReviewReq req) {
        requireRole(req.role(), "MANAGER");
        assertFresh(req.requestKey());
        LostFoundClaim claim = lockClaim(claimId);
        if (!"PENDING".equals(claim.getStatus())) {
            throw new BizException("申请当前为「" + claimStatusName(claim.getStatus()) + "」，不能审核");
        }
        LostFoundItem item = lockItem(claim.getItemId());
        requireCustody(item);
        String feature = valid(req.featureConclusion(), REVIEW_CONCLUSIONS, "特征匹配结论");
        String timePlace = valid(req.timePlaceConclusion(), REVIEW_CONCLUSIONS, "时间地点结论");
        String proof = valid(req.proofConclusion(), REVIEW_CONCLUSIONS, "补充证明结论");
        String result = switch (req.result()) {
            case "APPROVED", "SUPPLEMENT", "REJECTED" -> req.result();
            default -> throw new BizException("审核结果只能是通过、要求补证或驳回");
        };
        if ("APPROVED".equals(result) && (!"PASS".equals(feature) || !"PASS".equals(timePlace) || !"PASS".equals(proof))) {
            throw new BizException("三项结论全部通过时才能审核通过");
        }
        if (("SUPPLEMENT".equals(result) && "FAIL".equals(feature))
                || ("SUPPLEMENT".equals(result) && "FAIL".equals(timePlace))) {
            throw new BizException("特征或时间地点已明确不符时不能仅要求补证，应驳回");
        }
        if ("REJECTED".equals(result) && (req.note() == null || req.note().isBlank())) {
            throw new BizException("驳回必须填写原因");
        }

        int round = reviewRepository.countByClaimId(claimId) + 1;
        LostFoundReview review = new LostFoundReview();
        review.setClaimId(claimId);
        review.setItemId(claim.getItemId());
        review.setReviewRound(round);
        review.setFeatureConclusion(feature);
        review.setTimePlaceConclusion(timePlace);
        review.setProofConclusion(proof);
        review.setResult(result);
        review.setNote(trimToNull(req.note()));
        review.setReviewedBy(name(req.operator()));
        review.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(review);

        String before = claim.getStatus();
        claim.setStatus(result);
        claimRepository.save(claim);
        chain(item, claimId, "REVIEW", before, result, review.getReviewedBy(), "MANAGER",
                "第 " + round + " 轮审核：特征=" + conclusionName(feature)
                        + "，时间地点=" + conclusionName(timePlace)
                        + "，补充证明=" + conclusionName(proof)
                        + "；结论=" + claimStatusName(result)
                        + (review.getNote() == null ? "" : "；意见=" + review.getNote()),
                req.requestKey(), review.getReviewedAt());
        return claimDetail(claimId, "MANAGER");
    }

    // ---------------- 双人现场交还与异常 ----------------

    @Transactional
    public LostFoundDtos.ClaimDetail reportException(Long claimId, LostFoundDtos.ExceptionReq req) {
        if (!"FRONT".equals(req.role()) && !"COACH".equals(req.role())) {
            throw new ForbiddenException("只有参与现场交还的前台或第二名工作人员可以登记异常");
        }
        assertFresh(req.requestKey());
        LostFoundClaim claim = lockClaim(claimId);
        if (!"APPROVED".equals(claim.getStatus())) {
            throw new BizException("只有审核通过、待现场交还的申请可以登记交接异常");
        }
        LostFoundItem item = lockItem(claim.getItemId());
        String reason = valid(req.reason(), EXCEPTION_REASONS, "异常原因");
        if (hasOpenException(item.getId(), claimId)) {
            throw new BizException("该申请已有未处理异常，不能重复登记");
        }
        LocalDateTime now = LocalDateTime.now();
        LostFoundException exception = new LostFoundException();
        exception.setExceptionNo(nextNo("EX", exceptionRepository.count()));
        exception.setItemId(item.getId());
        exception.setClaimId(claimId);
        exception.setReason(reason);
        exception.setDetail(trim(req.detail(), "异常情况"));
        exception.setReportedBy(name(req.operator()));
        exception.setReporterRole(req.role());
        exception.setStatus("OPEN");
        exception.setCreatedAt(now);
        exceptionRepository.save(exception);

        if ("BROKEN_SEAL".equals(reason) && !"BROKEN".equals(item.getSealStatus())) {
            createSeal(item, "DAMAGE", item.getSealStatus(), "BROKEN", item.getCurrentCustodian(),
                    item.getStorageArea(), "异常登记：" + exception.getDetail(),
                    exception.getReportedBy(), req.role(), now);
            item.setSealStatus("BROKEN");
            itemRepository.save(item);
        }
        claim.setStatus("EXCEPTION");
        claimRepository.save(claim);
        chain(item, claimId, "EXCEPTION", "APPROVED", "EXCEPTION", exception.getReportedBy(), req.role(),
                "现场认领交接暂停：" + exceptionReasonName(reason) + "；" + exception.getDetail()
                        + "。原审核结果保留，等待值班经理处理。",
                req.requestKey(), now);
        return claimDetail(claimId, "MANAGER");
    }

    @Transactional
    public LostFoundDtos.ClaimDetail resolveException(Long exceptionId, LostFoundDtos.ResolveExceptionReq req) {
        requireRole(req.role(), "MANAGER");
        assertFresh(req.requestKey());
        LostFoundException exception = exceptionRepository.findByIdForUpdate(exceptionId)
                .orElseThrow(() -> new BizException("异常记录不存在"));
        if (!"OPEN".equals(exception.getStatus())) {
            throw new BizException("异常已处理，不能重复处理");
        }
        LostFoundClaim claim = lockClaim(exception.getClaimId());
        LostFoundItem item = lockItem(claim.getItemId());
        String resolution = switch (req.resolution()) {
            case "RESEAL", "REVERIFY_PASS", "TERMINATE" -> req.resolution();
            default -> throw new BizException("异常处理只能选择重新封存、补充核验通过或终止本次认领");
        };
        String note = trim(req.note(), "处理说明");
        LocalDateTime now = LocalDateTime.now();
        String before = claim.getStatus();
        String oldSealStatus = item.getSealStatus();

        if ("RESEAL".equals(resolution)) {
            String sealNo = trim(req.newSealNo(), "新封签号");
            String storage = req.storageArea() == null || req.storageArea().isBlank()
                    ? item.getStorageArea() : req.storageArea().trim();
            if (!"BROKEN".equals(item.getSealStatus()) && !"OPEN".equals(item.getSealStatus())) {
                throw new BizException("当前封签并未破损或开封，不需要重新封存");
            }
            item.setSealNo(sealNo);
            item.setSealStatus("INTACT");
            item.setSealedAt(now);
            item.setStorageArea(storage);
            itemRepository.save(item);
            createSeal(item, "RESEAL", oldSealStatus, "INTACT", item.getCurrentCustodian(), storage,
                    "异常处理后重新封存：" + note, name(req.operator()), "MANAGER", now);
            exception.setStatus("RESOLVED");
            claim.setStatus("APPROVED");
            chain(item, claim.getId(), "EXCEPTION_RESOLVE", before, "APPROVED", name(req.operator()), "MANAGER",
                    "异常处理：重新封存，封签 " + sealNo + "；原审核结果保留，回到待双人交还。" + note,
                    req.requestKey(), now);
        } else if ("REVERIFY_PASS".equals(resolution)) {
            if (!"INTACT".equals(item.getSealStatus())) {
                throw new BizException("补充核验通过后仍需封签完好；破损情况请先重新封存");
            }
            if (req.newCustodian() != null && !req.newCustodian().isBlank()) {
                item.setCurrentCustodian(req.newCustodian().trim());
            }
            if (req.storageArea() != null && !req.storageArea().isBlank()) {
                item.setStorageArea(req.storageArea().trim());
            }
            itemRepository.save(item);
            exception.setStatus("RESOLVED");
            claim.setStatus("APPROVED");
            chain(item, claim.getId(), "EXCEPTION_RESOLVE", before, "APPROVED", name(req.operator()), "MANAGER",
                    "异常处理：补充核验通过，原审核结果保留，回到待双人交还。" + note,
                    req.requestKey(), now);
        } else {
            exception.setStatus("TERMINATED");
            claim.setStatus("TERMINATED");
            chain(item, claim.getId(), "CLAIM_TERMINATE", before, "TERMINATED", name(req.operator()), "MANAGER",
                    "异常处理：终止本次认领，物品继续保管；不得把该申请直接改成已交还。" + note,
                    req.requestKey(), now);
        }
        exception.setResolution(resolution);
        exception.setResolutionNote(note);
        exception.setResolvedBy(name(req.operator()));
        exception.setResolvedAt(now);
        exceptionRepository.save(exception);
        claimRepository.save(claim);
        return claimDetail(claim.getId(), "MANAGER");
    }

    @Transactional
    public LostFoundDtos.ClaimDetail completeReturn(Long claimId, LostFoundDtos.ReturnReq req) {
        requireRole(req.role(), "FRONT");
        assertFresh(req.requestKey());
        LostFoundClaim claim = lockClaim(claimId);
        if (!"APPROVED".equals(claim.getStatus())) {
            throw new BizException("只有审核通过的待交还申请可以现场交还");
        }
        LostFoundItem item = lockItem(claim.getItemId());
        requireCustody(item);
        if (hasOpenException(item.getId(), claimId)) {
            throw new BizException("存在未处理异常，双人交还已暂停");
        }
        if (!"INTACT".equals(item.getSealStatus())) {
            throw new BizException("封签不是完好状态，请先登记异常并由经理处理，不能直接交还");
        }
        String custodian = name(req.operator());
        String witness = name(req.witness());
        if (!custodian.equals(item.getCurrentCustodian())) {
            throw new BizException("交还必须由当前保管人 " + item.getCurrentCustodian() + " 到场执行");
        }
        if (custodian.equals(witness)) {
            throw new BizException("当前保管人与另一名工作人员不能是同一人");
        }
        String receiver = name(req.actualReceiver());
        boolean receiverChangedAllowed = exceptionRepository.findByClaimIdOrderByIdDesc(claimId).stream()
                .anyMatch(e -> "RECEIVER_CHANGE".equals(e.getReason()) && "RESOLVED".equals(e.getStatus()));
        if (!receiver.equals(claim.getClaimantName()) && !receiverChangedAllowed) {
            throw new BizException("实际领取人与登记认领人不同，必须先登记“领取人临时变更”异常并由经理补充核验");
        }
        LocalDateTime now = LocalDateTime.now();
        claim.setActualReceiver(receiver);
        claim.setReceiverRelation(trim(req.receiverRelation(), "领取人与登记认领人的关系"));
        claim.setIdVerifyMethod(trim(req.idVerifyMethod(), "证件核验方式"));
        claim.setReturnCustodian(custodian);
        claim.setReturnWitness(witness);
        claim.setReturnedAt(now);
        claim.setReturnNote(trimToNull(req.note()));
        claim.setStatus("RETURNED");
        claimRepository.save(claim);

        item.setStatus("RETURNED");
        item.setReturnedAt(now);
        itemRepository.save(item);
        chain(item, claimId, "RETURN", "APPROVED", "RETURNED", custodian, "FRONT",
                "双人现场交还完成：当前保管人 " + custodian + " 与工作人员 " + witness
                        + " 共同确认；实际领取人 " + receiver + "（" + claim.getReceiverRelation()
                        + "），证件核验：" + claim.getIdVerifyMethod(),
                req.requestKey(), now);
        return claimDetail(claimId, "FRONT");
    }

    // ---------------- 逾期处置 ----------------

    @Transactional
    public LostFoundDtos.ItemDetail dispose(Long itemId, LostFoundDtos.DisposeReq req) {
        requireRole(req.role(), "MANAGER");
        assertFresh(req.requestKey());
        LostFoundItem item = lockItem(itemId);
        requireCustody(item);
        if (item.getRetentionUntil() == null || !item.getRetentionUntil().isBefore(LocalDate.now())) {
            throw new BizException("物品尚未超过保管期限，不能进入处置清单");
        }
        if (hasActiveClaim(itemId)) {
            throw new BizException("仍有未终结的认领申请，不能处置");
        }
        String method = switch (req.method()) {
            case "POLICE", "DONATE", "SCRAP" -> req.method();
            default -> throw new BizException("处置结果只能是返还公安、捐赠或报废");
        };
        String path;
        if (Boolean.TRUE.equals(item.getHighValue()) && Boolean.TRUE.equals(item.getIdentityItem())) {
            path = "POLICE_JOINT";
        } else if (Boolean.TRUE.equals(item.getHighValue())) {
            path = "POLICE_HIGH_VALUE";
        } else if (Boolean.TRUE.equals(item.getIdentityItem())) {
            path = "POLICE_IDENTITY";
        } else {
            path = "STANDARD";
        }
        if (!"STANDARD".equals(path) && !"POLICE".equals(method)) {
            throw new BizException("高价值或带身份信息物品必须返还公安，不能捐赠或报废");
        }
        if ("POLICE".equals(method) && "STANDARD".equals(path)) {
            path = "STANDARD_POLICE";
        }
        List<String> refs = cleanRefs(req.credentialRefs(), "接收方凭证");
        LocalDateTime now = LocalDateTime.now();
        LostFoundDisposal disposal = new LostFoundDisposal();
        disposal.setDisposalNo(nextNo("DP", disposalRepository.count()));
        disposal.setItemId(itemId);
        disposal.setMethod(method);
        disposal.setDisposalPath(path);
        disposal.setReceiverName(trim(req.receiverName(), "接收方"));
        disposal.setCredentialNo(trim(req.credentialNo(), "接收凭证号"));
        disposal.setCredentialRefs(writeList(refs));
        disposal.setNote(trimToNull(req.note()));
        disposal.setDisposedBy(name(req.operator()));
        disposal.setDisposedAt(now);
        disposalRepository.save(disposal);

        item.setStatus("DISPOSED");
        item.setDisposedAt(now);
        itemRepository.save(item);
        chain(item, null, "DISPOSE", "CUSTODY", "DISPOSED", disposal.getDisposedBy(), "MANAGER",
                "逾期处置：" + disposalPathName(path) + "；接收方 " + disposal.getReceiverName()
                        + "，凭证号 " + disposal.getCredentialNo(),
                req.requestKey(), now);
        return detail(itemId, "MANAGER");
    }

    // ---------------- 映射与辅助 ----------------

    private LostFoundDtos.ItemSummary toSummary(LostFoundItem item, String role) {
        boolean overdue = "CUSTODY".equals(item.getStatus())
                && item.getRetentionUntil() != null && item.getRetentionUntil().isBefore(LocalDate.now());
        return new LostFoundDtos.ItemSummary(
                item.getId(), item.getItemNo(), item.getCategory(), categoryName(item.getCategory()),
                item.getPublicCategory(), name("publicCategory", item.getPublicCategory()),
                item.getFoundAreaType(), name("area", item.getFoundAreaType()),
                item.getFoundAreaDetail(), item.getFoundAt(), item.getCurrentCustodian(),
                item.getStorageArea(), item.getSealNo(), item.getSealStatus(), sealName(item.getSealStatus()),
                item.getHighValue(), item.getIdentityItem(), item.getStatus(), itemStatusName(item.getStatus()),
                overdue, item.getRetentionUntil(), itemActions(item, role));
    }

    private LostFoundDtos.ClaimSummary toClaimSummary(LostFoundClaim claim, String role) {
        LostFoundItem item = itemRepository.findById(claim.getItemId()).orElse(null);
        return new LostFoundDtos.ClaimSummary(
                claim.getId(), claim.getClaimNo(), claim.getItemId(),
                item == null ? String.valueOf(claim.getItemId()) : item.getItemNo(),
                claim.getClaimantType(), claim.getClaimantName(),
                "FRONT".equals(role) ? claim.getClaimantContact() : "***",
                claim.getStatus(), claimStatusName(claim.getStatus()),
                claim.getSubmittedBy(), claim.getSubmittedAt(),
                reviewRepository.countByClaimId(claim.getId()), claimActions(claim, role));
    }

    private LostFoundDtos.SealView toSeal(LostFoundSeal seal) {
        return new LostFoundDtos.SealView(seal.getId(), seal.getSealNo(), seal.getAction(),
                sealActionName(seal.getAction()), seal.getBeforeStatus(), seal.getAfterStatus(),
                seal.getCustodian(), seal.getStorageArea(), seal.getReason(), seal.getOperator(),
                seal.getOperatorRole(), seal.getCreatedAt());
    }

    private LostFoundDtos.ReviewView toReview(LostFoundReview review) {
        return new LostFoundDtos.ReviewView(review.getId(), review.getReviewRound(),
                review.getFeatureConclusion(), conclusionName(review.getFeatureConclusion()),
                review.getTimePlaceConclusion(), conclusionName(review.getTimePlaceConclusion()),
                review.getProofConclusion(), conclusionName(review.getProofConclusion()),
                review.getResult(), claimStatusName(review.getResult()),
                review.getNote(), review.getReviewedBy(), review.getReviewedAt());
    }

    private LostFoundDtos.ExceptionView toException(LostFoundException e, String role, LostFoundItem item) {
        return new LostFoundDtos.ExceptionView(e.getId(), e.getExceptionNo(), e.getClaimId(),
                e.getReason(), exceptionReasonName(e.getReason()), e.getDetail(), e.getReportedBy(),
                e.getReporterRole(), e.getStatus(), exceptionStatusName(e.getStatus()),
                e.getResolution(), e.getResolution() == null ? null : resolutionName(e.getResolution()),
                e.getResolutionNote(), e.getResolvedBy(), e.getCreatedAt(), e.getResolvedAt(),
                "MANAGER".equals(role) && "OPEN".equals(e.getStatus()) ? List.of("RESOLVE_EXCEPTION") : List.of());
    }

    private LostFoundDtos.DisposalView toDisposal(LostFoundDisposal d) {
        return new LostFoundDtos.DisposalView(d.getId(), d.getDisposalNo(), d.getMethod(),
                disposalMethodName(d.getMethod()), d.getDisposalPath(), disposalPathName(d.getDisposalPath()),
                d.getReceiverName(), d.getCredentialNo(), readList(d.getCredentialRefs()),
                d.getNote(), d.getDisposedBy(), d.getDisposedAt());
    }

    private LostFoundDtos.ChainView toChain(LostFoundChainLog log) {
        return new LostFoundDtos.ChainView(log.getId(), log.getClaimId(), log.getAction(),
                actionName(log.getAction()), log.getBeforeStatus(), log.getAfterStatus(),
                log.getOperator(), log.getOperatorRole(), roleName(log.getOperatorRole()),
                log.getDetail(), log.getCreatedAt());
    }

    private List<String> itemActions(LostFoundItem item, String role) {
        List<String> actions = new ArrayList<>();
        if (!"CUSTODY".equals(item.getStatus())) {
            return actions;
        }
        boolean openException = hasOpenException(item.getId(), null);
        if ("FRONT".equals(role) && !openException) {
            actions.add("TRANSFER");
            actions.add("CREATE_CLAIM");
            if ("INTACT".equals(item.getSealStatus())) {
                actions.add("INSPECT");
            }
            if ("OPEN".equals(item.getSealStatus())) {
                actions.add("RESEAL");
            }
        }
        boolean overdue = item.getRetentionUntil() != null && item.getRetentionUntil().isBefore(LocalDate.now());
        if ("MANAGER".equals(role) && overdue && !hasActiveClaim(item.getId())) {
            actions.add("DISPOSE");
        }
        return actions;
    }

    private List<String> claimActions(LostFoundClaim claim, String role) {
        List<String> actions = new ArrayList<>();
        String status = claim.getStatus();
        if ("MANAGER".equals(role) && ("PENDING".equals(status) || "SUPPLEMENT".equals(status))) {
            actions.add("REVIEW");
        }
        if ("FRONT".equals(role) && "SUPPLEMENT".equals(status)) {
            actions.add("SUBMIT_SUPPLEMENT");
        }
        if ("APPROVED".equals(status) && ("FRONT".equals(role) || "COACH".equals(role))) {
            actions.add("REPORT_EXCEPTION");
        }
        if ("APPROVED".equals(status) && "FRONT".equals(role)) {
            actions.add("RETURN");
        }
        if ("EXCEPTION".equals(status) && "MANAGER".equals(role) && hasOpenException(claim.getItemId(), claim.getId())) {
            actions.add("RESOLVE_EXCEPTION");
        }
        return actions;
    }

    private void createSeal(LostFoundItem item, String action, String before, String after,
            String custodian, String storage, String reason, String operator, String role, LocalDateTime at) {
        LostFoundSeal seal = new LostFoundSeal();
        seal.setItemId(item.getId());
        seal.setSealNo(item.getSealNo());
        seal.setAction(action);
        seal.setBeforeStatus(before);
        seal.setAfterStatus(after);
        seal.setCustodian(custodian);
        seal.setStorageArea(storage);
        seal.setReason(reason);
        seal.setOperator(operator);
        seal.setOperatorRole(role);
        seal.setCreatedAt(at);
        sealRepository.save(seal);
    }

    private void chain(LostFoundItem item, Long claimId, String action, String before, String after,
            String operator, String role, String detail, String requestKey, LocalDateTime at) {
        LostFoundChainLog log = new LostFoundChainLog();
        log.setItemId(item.getId());
        log.setClaimId(claimId);
        log.setAction(action);
        log.setBeforeStatus(before);
        log.setAfterStatus(after);
        log.setOperator(operator);
        log.setOperatorRole(role);
        log.setDetail(detail);
        log.setRequestKey(blankToNull(requestKey));
        log.setCreatedAt(at);
        chainRepository.save(log);
    }

    private LostFoundItem lockItem(Long id) {
        return itemRepository.findByIdForUpdate(id).orElseThrow(() -> new BizException("失物不存在：id=" + id));
    }

    private LostFoundClaim lockClaim(Long id) {
        return claimRepository.findByIdForUpdate(id).orElseThrow(() -> new BizException("认领申请不存在：id=" + id));
    }

    private void requireCustody(LostFoundItem item) {
        if (!"CUSTODY".equals(item.getStatus())) {
            throw new BizException("物品当前为「" + itemStatusName(item.getStatus()) + "」，不能继续保管或认领操作");
        }
    }

    private boolean hasActiveClaim(Long itemId) {
        return claimRepository.findByItemIdAndStatus(itemId, "PENDING").stream().findAny().isPresent()
                || claimRepository.findByItemIdAndStatus(itemId, "SUPPLEMENT").stream().findAny().isPresent()
                || claimRepository.findByItemIdAndStatus(itemId, "APPROVED").stream().findAny().isPresent()
                || claimRepository.findByItemIdAndStatus(itemId, "EXCEPTION").stream().findAny().isPresent();
    }

    private boolean hasOpenException(Long itemId, Long claimId) {
        if (claimId == null) {
            return !exceptionRepository.findByItemIdAndStatusOrderByIdDesc(itemId, "OPEN").isEmpty();
        }
        return exceptionRepository.findByClaimIdOrderByIdDesc(claimId).stream()
                .anyMatch(e -> "OPEN".equals(e.getStatus()));
    }

    private void assertFresh(String requestKey) {
        String key = blankToNull(requestKey);
        if (key != null && chainRepository.findByRequestKey(key).isPresent()) {
            throw new BizException("该交接请求已处理，系统未重复生成交接记录");
        }
    }

    private void requireStaff(String role) {
        if (!STAFF.contains(role)) {
            throw new ForbiddenException("当前角色不能浏览内部失物资料，只能查看公开失物列表");
        }
    }

    private void requireRole(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new ForbiddenException("该动作仅限" + roleName(expected) + "执行");
        }
    }

    private String valid(String value, Set<String> allowed, String label) {
        if (value == null || !allowed.contains(value)) {
            throw new BizException(label + "不合法");
        }
        return value;
    }

    private String name(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BizException("请填写操作人姓名");
        }
        return value.trim();
    }

    private String trim(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BizException("请填写" + label);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private List<String> cleanRefs(List<String> refs, String label) {
        List<String> result = refs == null ? List.of() : refs.stream()
                .filter(r -> r != null && !r.isBlank()).map(String::trim).distinct().toList();
        if (result.isEmpty()) {
            throw new BizException("请至少登记一份" + label);
        }
        return result;
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new BizException("附件信息格式有误");
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private synchronized String nextNo(String prefix, long count) {
        String base = prefix + LocalDateTime.now().format(NO_STAMP);
        String candidate = base;
        int suffix = (int) (count % 90 + 1);
        while (numberExists(prefix, candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean numberExists(String prefix, String no) {
        return switch (prefix) {
            case "LF" -> itemRepository.existsByItemNo(no);
            case "CL" -> claimRepository.existsByClaimNo(no);
            case "EX" -> exceptionRepository.existsByExceptionNo(no);
            case "DP" -> disposalRepository.existsByDisposalNo(no);
            default -> false;
        };
    }

    private String publicCategory(String category) {
        return switch (category) {
            case "ELECTRONICS", "CARD" -> "VALUABLE_SMALL";
            case "CLOTHING" -> "CLOTHING";
            case "EQUIPMENT" -> "SPORTS_ITEM";
            case "ACCESSORY", "STATIONERY" -> "PERSONAL_ITEM";
            default -> "OTHER";
        };
    }

    public static Map<String, Object> options() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("roles", List.of(
                Map.of("code", "FRONT", "name", "值班前台"),
                Map.of("code", "MANAGER", "name", "值班经理"),
                Map.of("code", "COACH", "name", "普通教练"),
                Map.of("code", "MEMBER", "name", "会员/访客")));
        options.put("categories", List.of(
                Map.of("code", "ELECTRONICS", "name", "电子产品"),
                Map.of("code", "CARD", "name", "证件/卡片"),
                Map.of("code", "CLOTHING", "name", "衣物"),
                Map.of("code", "EQUIPMENT", "name", "射箭器材/护具"),
                Map.of("code", "ACCESSORY", "name", "随身配饰"),
                Map.of("code", "STATIONERY", "name", "文具/书本"),
                Map.of("code", "OTHER", "name", "其他物品")));
        options.put("publicCategories", List.of(
                Map.of("code", "VALUABLE_SMALL", "name", "小型贵重物品"),
                Map.of("code", "CLOTHING", "name", "衣物"),
                Map.of("code", "SPORTS_ITEM", "name", "运动用品"),
                Map.of("code", "PERSONAL_ITEM", "name", "个人用品"),
                Map.of("code", "OTHER", "name", "其他物品")));
        options.put("areas", List.of(
                Map.of("code", "LANE", "name", "箭道区"),
                Map.of("code", "COURSE", "name", "课程区"),
                Map.of("code", "PUBLIC", "name", "公共区域")));
        options.put("finderTypes", List.of(
                Map.of("code", "MEMBER", "name", "会员"),
                Map.of("code", "STAFF", "name", "工作人员"),
                Map.of("code", "GUEST", "name", "未建档访客")));
        options.put("conclusions", List.of(
                Map.of("code", "PASS", "name", "匹配/通过"),
                Map.of("code", "PARTIAL", "name", "部分匹配，需补证"),
                Map.of("code", "FAIL", "name", "不符")));
        options.put("exceptionReasons", List.of(
                Map.of("code", "BROKEN_SEAL", "name", "封签破损"),
                Map.of("code", "ITEM_MISMATCH", "name", "实物与登记不符"),
                Map.of("code", "RECEIVER_CHANGE", "name", "领取人临时变更"),
                Map.of("code", "CUSTODIAN_ABSENT", "name", "保管人无法到场")));
        options.put("disposalMethods", List.of(
                Map.of("code", "POLICE", "name", "返还公安"),
                Map.of("code", "DONATE", "name", "捐赠"),
                Map.of("code", "SCRAP", "name", "报废")));
        return options;
    }

    private String name(String type, String code) {
        return Optional.ofNullable((List<Map<String, Object>>) options().get(switch (type) {
            case "area" -> "areas";
            case "publicCategory" -> "publicCategories";
            default -> type + "s";
        })).flatMap(list -> list.stream().filter(x -> code.equals(x.get("code"))).findFirst())
                .map(x -> String.valueOf(x.get("name"))).orElse(code);
    }

    private String categoryName(String code) {
        return name("categorie", code);
    }

    private String roleName(String code) {
        return switch (code) {
            case "FRONT" -> "值班前台";
            case "MANAGER" -> "值班经理";
            case "COACH" -> "普通教练";
            case "MEMBER" -> "会员/访客";
            default -> code;
        };
    }

    private String itemStatusName(String code) {
        return switch (code) {
            case "CUSTODY" -> "保管中";
            case "RETURNED" -> "已交还";
            case "DISPOSED" -> "已处置";
            default -> code;
        };
    }

    private String claimStatusName(String code) {
        return switch (code) {
            case "PENDING" -> "待审核";
            case "SUPPLEMENT" -> "待补证";
            case "APPROVED" -> "待交还";
            case "EXCEPTION" -> "异常暂停";
            case "RETURNED" -> "已交还";
            case "REJECTED" -> "已驳回";
            case "TERMINATED" -> "已终止";
            default -> code;
        };
    }

    private String sealName(String code) {
        return switch (code) {
            case "INTACT" -> "完好";
            case "OPEN" -> "已开封";
            case "BROKEN" -> "破损";
            default -> code;
        };
    }

    private String sealActionName(String code) {
        return switch (code) {
            case "INITIAL" -> "首次封存";
            case "OPEN" -> "开封";
            case "DAMAGE" -> "异常破损";
            case "RESEAL" -> "重新封存";
            default -> code;
        };
    }

    private String conclusionName(String code) {
        return switch (code) {
            case "PASS" -> "通过";
            case "PARTIAL" -> "部分匹配";
            case "FAIL" -> "不符";
            default -> code;
        };
    }

    private String exceptionReasonName(String code) {
        return name("exceptionReason", code);
    }

    private String exceptionStatusName(String code) {
        return "OPEN".equals(code) ? "待经理处理" : "TERMINATED".equals(code) ? "已终止认领" : "已处理";
    }

    private String resolutionName(String code) {
        return switch (code) {
            case "RESEAL" -> "重新封存";
            case "REVERIFY_PASS" -> "补充核验通过";
            case "TERMINATE" -> "终止认领";
            default -> code;
        };
    }

    private String disposalMethodName(String code) {
        return switch (code) {
            case "POLICE" -> "返还公安";
            case "DONATE" -> "捐赠";
            case "SCRAP" -> "报废";
            default -> code;
        };
    }

    private String disposalPathName(String code) {
        return switch (code) {
            case "STANDARD" -> "普通物品标准处置路径";
            case "STANDARD_POLICE" -> "普通物品公安接收路径";
            case "POLICE_HIGH_VALUE" -> "高价值物品公安接收路径";
            case "POLICE_IDENTITY" -> "带身份信息物品公安移交路径";
            case "POLICE_JOINT" -> "高价值且带身份信息物品专项公安移交路径";
            default -> code;
        };
    }

    private String actionName(String code) {
        return switch (code) {
            case "REGISTER" -> "登记与首次封存";
            case "TRANSFER" -> "保管转移";
            case "INSPECT" -> "开封查看";
            case "RESEAL" -> "重新封存";
            case "EXCEPTION_RESOLVE" -> "异常解除";
            case "CLAIM_SUBMIT" -> "提交认领";
            case "SUPPLEMENT" -> "补充证明";
            case "REVIEW" -> "认领审核";
            case "EXCEPTION" -> "异常暂停";
            case "RETURN" -> "双人交还";
            case "DISPOSE" -> "逾期处置";
            case "CLAIM_TERMINATE" -> "终止认领";
            default -> code;
        };
    }
}
