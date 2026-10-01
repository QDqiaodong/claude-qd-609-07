package com.archery.range.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 失物保管与认领交接台的入参与出参。 */
public final class LostFoundDtos {

    private LostFoundDtos() {
    }

    public record ActorReq(
            @NotBlank(message = "请选择当前角色") String role,
            @NotBlank(message = "请填写操作人姓名") String operator,
            @Size(max = 64, message = "请求标识过长") String requestKey) {
    }

    /** 发现登记 + 值班前台生成封签并完成首次保管交接（一个原子事务）。 */
    public record RegisterReq(
            @NotBlank(message = "请选择物品类别") String category,
            @NotBlank(message = "请选择发现区域") String foundAreaType,
            @NotBlank(message = "请填写精确发现位置") @Size(max = 128) String foundAreaDetail,
            @NotNull(message = "请选择发现时间") LocalDateTime foundAt,
            @NotBlank(message = "请填写外观特征") @Size(max = 500) String appearance,
            @NotNull(message = "请上传或登记至少一张现场照片") List<String> photoRefs,
            @NotBlank(message = "请选择发现人身份") String finderType,
            Long finderMemberId,
            @NotBlank(message = "请填写发现人姓名") String finderName,
            String finderContact,
            @NotBlank(message = "请填写当前保管人") String custodian,
            @NotBlank(message = "请填写存放区域") String storageArea,
            boolean highValue,
            boolean identityItem,
            @NotBlank(message = "请选择前台角色") String role,
            @NotBlank(message = "请填写值班前台姓名") String operator,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record TransferReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请填写新保管人") String newCustodian,
            @NotBlank(message = "请填写新存放区域") String newStorageArea,
            @NotBlank(message = "请填写转移原因") @Size(max = 500) String reason,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record InspectReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请填写开封原因") @Size(max = 500) String reason,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record ResealReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请填写新封签号") String newSealNo,
            @NotBlank(message = "请确认存放区域") String storageArea,
            @NotBlank(message = "请填写重新封存原因") @Size(max = 500) String reason,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record ClaimReq(
            @NotBlank String claimantType,
            Long claimantMemberId,
            @NotBlank(message = "请填写认领人姓名") String claimantName,
            @NotBlank(message = "请填写认领人联系方式") String claimantContact,
            @NotBlank(message = "请填写认领描述") @Size(max = 500) String description,
            @NotNull(message = "请选择遗失开始时间") LocalDateTime lostTimeStart,
            @NotNull(message = "请选择遗失结束时间") LocalDateTime lostTimeEnd,
            @NotBlank(message = "请填写可能出现的箭道或课程") @Size(max = 300) String possibleAreas,
            @NotBlank(message = "请填写用于核验的隐私特征") @Size(max = 300) String privacyFeature,
            @NotNull(message = "请登记补充证明") List<String> evidenceRefs,
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record SupplementReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotNull(message = "请上传补证材料") List<String> evidenceRefs,
            @NotBlank(message = "请填写补证说明") @Size(max = 500) String note,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record ReviewReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请给出特征匹配结论") String featureConclusion,
            @NotBlank(message = "请给出时间地点吻合结论") String timePlaceConclusion,
            @NotBlank(message = "请给出补充证明结论") String proofConclusion,
            @NotBlank(message = "请选择审核结果") String result,
            @Size(max = 500) String note,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record ReturnReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请填写另一名现场工作人员") String witness,
            @NotBlank(message = "请填写实际领取人") String actualReceiver,
            @NotBlank(message = "请填写领取人与登记认领人的关系") String receiverRelation,
            @NotBlank(message = "请填写证件核验方式") String idVerifyMethod,
            @Size(max = 500) String note,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record ExceptionReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请选择异常原因") String reason,
            @NotBlank(message = "请填写异常情况") @Size(max = 500) String detail,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record ResolveExceptionReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请选择异常处理方式") String resolution,
            String newSealNo,
            String storageArea,
            String newCustodian,
            @NotBlank(message = "请填写处理说明") @Size(max = 500) String note,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record DisposeReq(
            @NotBlank String role,
            @NotBlank String operator,
            @NotBlank(message = "请选择处置结果") String method,
            @NotBlank(message = "请填写接收方") String receiverName,
            @NotBlank(message = "请填写接收凭证号") String credentialNo,
            @NotNull(message = "请上传接收方凭证") List<String> credentialRefs,
            @Size(max = 500) String note,
            @NotBlank(message = "请求标识不能为空") String requestKey) {
    }

    public record PublicItemView(
            Long id,
            String itemNo,
            String publicCategory,
            String publicCategoryName,
            LocalDate foundDate,
            String foundAreaType,
            String foundAreaName) {
    }

    public record SealView(
            Long id,
            String sealNo,
            String action,
            String actionName,
            String beforeStatus,
            String afterStatus,
            String custodian,
            String storageArea,
            String reason,
            String operator,
            String operatorRole,
            LocalDateTime createdAt) {
    }

    public record ClaimSummary(
            Long id,
            String claimNo,
            Long itemId,
            String itemNo,
            String claimantType,
            String claimantName,
            String claimantContact,
            String status,
            String statusName,
            String submittedBy,
            LocalDateTime submittedAt,
            Integer reviewRound,
            List<String> actions) {
    }

    public record ReviewView(
            Long id,
            Integer reviewRound,
            String featureConclusion,
            String featureConclusionName,
            String timePlaceConclusion,
            String timePlaceConclusionName,
            String proofConclusion,
            String proofConclusionName,
            String result,
            String resultName,
            String note,
            String reviewedBy,
            LocalDateTime reviewedAt) {
    }

    public record SupplementView(
            Long id,
            List<String> evidenceRefs,
            String note,
            String submittedBy,
            LocalDateTime createdAt) {
    }

    public record ExceptionView(
            Long id,
            String exceptionNo,
            Long claimId,
            String reason,
            String reasonName,
            String detail,
            String reportedBy,
            String reporterRole,
            String status,
            String statusName,
            String resolution,
            String resolutionName,
            String resolutionNote,
            String resolvedBy,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt,
            List<String> actions) {
    }

    public record DisposalView(
            Long id,
            String disposalNo,
            String method,
            String methodName,
            String disposalPath,
            String disposalPathName,
            String receiverName,
            String credentialNo,
            List<String> credentialRefs,
            String note,
            String disposedBy,
            LocalDateTime disposedAt) {
    }

    public record ChainView(
            Long id,
            Long claimId,
            String action,
            String actionName,
            String beforeStatus,
            String afterStatus,
            String operator,
            String operatorRole,
            String roleName,
            String detail,
            LocalDateTime createdAt) {
    }

    public record ItemSummary(
            Long id,
            String itemNo,
            String category,
            String categoryName,
            String publicCategory,
            String publicCategoryName,
            String foundAreaType,
            String foundAreaName,
            String foundAreaDetail,
            LocalDateTime foundAt,
            String currentCustodian,
            String storageArea,
            String sealNo,
            String sealStatus,
            String sealStatusName,
            Boolean highValue,
            Boolean identityItem,
            String status,
            String statusName,
            Boolean overdue,
            java.time.LocalDate retentionUntil,
            List<String> actions) {
    }

    public record ClaimDetail(
            ClaimSummary summary,
            String claimantContact,
            String description,
            LocalDateTime lostTimeStart,
            LocalDateTime lostTimeEnd,
            String possibleAreas,
            String privacyFeature,
            List<String> evidenceRefs,
            String actualReceiver,
            String receiverRelation,
            String idVerifyMethod,
            String returnCustodian,
            String returnWitness,
            LocalDateTime returnedAt,
            String returnNote,
            List<ReviewView> reviews,
            List<SupplementView> supplements,
            List<ExceptionView> exceptions,
            List<ChainView> logs) {
    }

    public record ItemDetail(
            ItemSummary summary,
            String appearance,
            List<String> photoRefs,
            String finderType,
            String finderName,
            String finderContact,
            Long finderMemberId,
            List<SealView> seals,
            List<ClaimSummary> claims,
            List<DisposalView> disposals,
            List<ChainView> logs,
            List<String> actions) {
    }
}
