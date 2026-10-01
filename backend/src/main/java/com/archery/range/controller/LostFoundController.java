package com.archery.range.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.archery.range.common.ApiResponse;
import com.archery.range.dto.LostFoundDtos;
import com.archery.range.service.LostFoundService;

import jakarta.validation.Valid;

/** 失物保管、认领审核、双人交还和逾期处置。 */
@RestController
@RequestMapping("/api/lost-found")
public class LostFoundController {

    private final LostFoundService service;

    public LostFoundController(LostFoundService service) {
        this.service = service;
    }

    @GetMapping("/options")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.success(LostFoundService.options());
    }

    /** 对外公开列表：仅模糊类别、发现日期、馆内区域。 */
    @GetMapping("/public-items")
    public ApiResponse<List<LostFoundDtos.PublicItemView>> publicList() {
        return ApiResponse.success(service.publicList());
    }

    @GetMapping("/items")
    public ApiResponse<List<LostFoundDtos.ItemSummary>> list(@RequestParam String role) {
        return ApiResponse.success(service.list(role));
    }

    @GetMapping("/items/{id}")
    public ApiResponse<LostFoundDtos.ItemDetail> detail(@PathVariable Long id, @RequestParam String role) {
        return ApiResponse.success(service.detail(id, role));
    }

    @PostMapping("/items")
    public ApiResponse<LostFoundDtos.ItemDetail> register(@Valid @RequestBody LostFoundDtos.RegisterReq req) {
        return ApiResponse.success("失物已登记并完成首次封存交接", service.register(req));
    }

    @PostMapping("/items/{id}/transfer")
    public ApiResponse<LostFoundDtos.ItemDetail> transfer(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.TransferReq req) {
        return ApiResponse.success("保管转移已追加到交接链", service.transfer(id, req));
    }

    @PostMapping("/items/{id}/inspect")
    public ApiResponse<LostFoundDtos.ItemDetail> inspect(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.InspectReq req) {
        return ApiResponse.success("开封记录已追加，查看后请重新封存", service.inspect(id, req));
    }

    @PostMapping("/items/{id}/reseal")
    public ApiResponse<LostFoundDtos.ItemDetail> reseal(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.ResealReq req) {
        return ApiResponse.success("已使用新封签重新封存", service.reseal(id, req));
    }

    @PostMapping("/items/{id}/claims")
    public ApiResponse<LostFoundDtos.ClaimDetail> createClaim(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.ClaimReq req) {
        return ApiResponse.success("认领申请已提交，等待值班经理审核", service.createClaim(id, req));
    }

    @GetMapping("/claims")
    public ApiResponse<List<LostFoundDtos.ClaimSummary>> claims(@RequestParam String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(service.claims(role, status));
    }

    @GetMapping("/claims/{id}")
    public ApiResponse<LostFoundDtos.ClaimDetail> claimDetail(@PathVariable Long id, @RequestParam String role) {
        return ApiResponse.success(service.claimDetail(id, role));
    }

    @PostMapping("/claims/{id}/supplement")
    public ApiResponse<LostFoundDtos.ClaimDetail> supplement(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.SupplementReq req) {
        return ApiResponse.success("补证材料已追加，重新进入审核", service.supplement(id, req));
    }

    @PostMapping("/claims/{id}/review")
    public ApiResponse<LostFoundDtos.ClaimDetail> review(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.ReviewReq req) {
        return ApiResponse.success("审核结论已分别记录", service.review(id, req));
    }

    @PostMapping("/claims/{id}/return")
    public ApiResponse<LostFoundDtos.ClaimDetail> completeReturn(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.ReturnReq req) {
        return ApiResponse.success("双人现场认领交接已完成", service.completeReturn(id, req));
    }

    @PostMapping("/claims/{id}/exceptions")
    public ApiResponse<LostFoundDtos.ClaimDetail> reportException(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.ExceptionReq req) {
        return ApiResponse.success("已转入异常交接，原审核结果保留并暂停交还", service.reportException(id, req));
    }

    @PostMapping("/exceptions/{id}/resolve")
    public ApiResponse<LostFoundDtos.ClaimDetail> resolveException(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.ResolveExceptionReq req) {
        return ApiResponse.success("异常处理结果已记录", service.resolveException(id, req));
    }

    @GetMapping("/disposals")
    public ApiResponse<List<LostFoundDtos.DisposalView>> disposals(@RequestParam String role) {
        return ApiResponse.success(service.disposals(role));
    }

    @PostMapping("/items/{id}/dispose")
    public ApiResponse<LostFoundDtos.ItemDetail> dispose(@PathVariable Long id,
            @Valid @RequestBody LostFoundDtos.DisposeReq req) {
        return ApiResponse.success("逾期物品处置结果与接收凭证已留存", service.dispose(id, req));
    }
}
