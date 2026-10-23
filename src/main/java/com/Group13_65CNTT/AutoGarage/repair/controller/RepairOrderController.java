package com.Group13_65CNTT.AutoGarage.repair.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.*;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairOrderResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairTaskResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.service.RepairService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.REPAIR_ORDERS_BASE)
@RequiredArgsConstructor
@Tag(name = "Repair Orders", description = "Quản lý vòng đời phiếu sửa chữa và công việc (TV3)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class RepairOrderController {

    private final RepairService repairService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Tạo phiếu sửa chữa mới từ tiếp nhận xe")
    public ResponseEntity<ApiResponse<RepairOrderResponse>> createRepairOrder(
            @Valid @RequestBody RepairOrderCreateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        RepairOrderResponse response = repairService.createRepairOrder(actorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy chi tiết phiếu sửa chữa theo ID")
    public ResponseEntity<ApiResponse<RepairOrderResponse>> getRepairOrderById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        RepairOrderResponse response = repairService.getRepairOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/code/{orderCode}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy chi tiết phiếu sửa chữa theo mã phiếu")
    public ResponseEntity<ApiResponse<RepairOrderResponse>> getRepairOrderByCode(
            @PathVariable String orderCode,
            HttpServletRequest httpRequest) {
        RepairOrderResponse response = repairService.getRepairOrderByCode(orderCode);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Tìm kiếm và phân trang danh sách phiếu sửa chữa")
    public ResponseEntity<ApiResponse<Page<RepairOrderResponse>>> searchRepairOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RepairOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        Page<RepairOrderResponse> response = repairService.searchRepairOrders(
                keyword, status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Cập nhật thông tin phiếu sửa chữa")
    public ResponseEntity<ApiResponse<RepairOrderResponse>> updateRepairOrder(
            @PathVariable Long id,
            @Valid @RequestBody RepairOrderUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        RepairOrderResponse response = repairService.updateRepairOrder(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PatchMapping("/{id}/diagnosis")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Cập nhật kết quả chẩn đoán ban đầu của xe")
    public ResponseEntity<ApiResponse<RepairOrderResponse>> updateDiagnosis(
            @PathVariable Long id,
            @Valid @RequestBody DiagnosisUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        RepairOrderResponse response = repairService.updateDiagnosis(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Chuyển trạng thái phiếu sửa chữa theo luồng quy trình")
    public ResponseEntity<ApiResponse<RepairOrderResponse>> transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody RepairOrderStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        RepairOrderResponse response = repairService.transitionStatus(
                actorId, id, request.getNewStatus(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/handover")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Bàn giao xe cho khách hàng và đóng phiếu sửa chữa")
    public ResponseEntity<ApiResponse<RepairOrderResponse>> handOverOrder(
            @PathVariable Long id,
            @Valid @RequestBody HandoverRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        RepairOrderResponse response = repairService.handOverOrder(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}/tasks")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy danh sách công việc của phiếu sửa chữa")
    public ResponseEntity<ApiResponse<List<RepairTaskResponse>>> getTasksByRepairOrderId(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        List<RepairTaskResponse> response = repairService.getTasksByRepairOrderId(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/tasks")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Thêm công việc vào phiếu sửa chữa")
    public ResponseEntity<ApiResponse<RepairTaskResponse>> createTask(
            @PathVariable Long id,
            @Valid @RequestBody RepairTaskCreateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        RepairTaskResponse response = repairService.createTask(actorId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Cập nhật công việc trong phiếu sửa chữa")
    public ResponseEntity<ApiResponse<RepairTaskResponse>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody RepairTaskUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        RepairTaskResponse response = repairService.updateTask(actorId, taskId, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Xóa công việc chưa thực hiện khỏi phiếu sửa chữa")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        repairService.deleteTask(actorId, taskId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.DELETED, null, httpRequest.getRequestURI()));
    }
}
