package com.Group13_65CNTT.AutoGarage.estimate.service;

import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateDecisionRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateResponse;
import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EstimateService {

    EstimateResponse createEstimate(Long actorUserId, EstimateCreateRequest request);

    EstimateResponse getEstimateById(Long id);

    EstimateResponse getEstimateByCode(String estimateCode);

    List<EstimateResponse> getEstimatesByRepairOrderId(Long repairOrderId);

    Page<EstimateResponse> searchEstimates(Long repairOrderId, EstimateStatus status, int page, int size, String sortBy, String sortDir);

    EstimateResponse sendEstimate(Long actorUserId, Long id);

    EstimateResponse decideEstimate(Long actorUserId, Long id, EstimateDecisionRequest request);
}
