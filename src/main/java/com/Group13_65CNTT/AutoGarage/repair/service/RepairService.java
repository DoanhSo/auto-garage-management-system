package com.Group13_65CNTT.AutoGarage.repair.service;

import com.Group13_65CNTT.AutoGarage.repair.dto.request.*;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairOrderResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairTaskResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RepairService {

    RepairOrderResponse createRepairOrder(Long actorUserId, RepairOrderCreateRequest request);

    RepairOrderResponse getRepairOrderById(Long id);

    RepairOrderResponse getRepairOrderByCode(String orderCode);

    Page<RepairOrderResponse> searchRepairOrders(String keyword, RepairOrderStatus status, int page, int size, String sortBy, String sortDir);

    RepairOrderResponse updateRepairOrder(Long actorUserId, Long id, RepairOrderUpdateRequest request);

    RepairOrderResponse updateDiagnosis(Long actorUserId, Long id, DiagnosisUpdateRequest request);

    RepairOrderResponse transitionStatus(Long actorUserId, Long id, RepairOrderStatus newStatus, String note);

    RepairOrderResponse handOverOrder(Long actorUserId, Long id, HandoverRequest request);

    List<RepairTaskResponse> getTasksByRepairOrderId(Long repairOrderId);

    RepairTaskResponse createTask(Long actorUserId, Long repairOrderId, RepairTaskCreateRequest request);

    RepairTaskResponse updateTask(Long actorUserId, Long taskId, RepairTaskUpdateRequest request);

    void deleteTask(Long actorUserId, Long taskId);
}
