package com.Group13_65CNTT.AutoGarage.quality.service;

import com.Group13_65CNTT.AutoGarage.quality.dto.request.QualityCheckCreateRequest;
import com.Group13_65CNTT.AutoGarage.quality.dto.response.QualityCheckResponse;

import java.util.List;

public interface QualityService {

    QualityCheckResponse performQualityCheck(Long actorUserId, QualityCheckCreateRequest request);

    QualityCheckResponse getQualityCheckById(Long id);

    List<QualityCheckResponse> getQualityChecksByRepairOrderId(Long repairOrderId);

    QualityCheckResponse getLatestQualityCheck(Long repairOrderId);
}
