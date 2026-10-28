package com.Group13_65CNTT.AutoGarage.scheduling.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayReservationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.BayReservation;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.BayReservationMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.BayReservationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(ApiPaths.BAY_RESERVATIONS_BASE)
@RequiredArgsConstructor
@Tag(name = "Bay Reservations", description = "Query bay reservations and workshop bay allocation")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class BayReservationController {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "bayId", "startsAt", "endsAt", "status", "createdAt");

    private final BayReservationRepository bayReservationRepository;
    private final BayReservationMapper bayReservationMapper;
    private final PaginationPolicy pagination;

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "List bay reservations with optional bayId and status filters")
    public ResponseEntity<ApiResponse<Page<BayReservationResponse>>> getReservations(
            @RequestParam(required = false) Long bayId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "startsAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        Pageable pageable = pagination.page(
                page,
                size != null ? size : pagination.defaultSize(),
                sortBy,
                sortDir,
                SORT_FIELDS);

        Page<BayReservation> result;
        if (bayId != null && status != null) {
            result = bayReservationRepository.findByBayIdAndStatus(bayId, status, pageable);
        } else if (bayId != null) {
            result = bayReservationRepository.findByBayId(bayId, pageable);
        } else if (status != null) {
            result = bayReservationRepository.findByStatus(status, pageable);
        } else {
            result = bayReservationRepository.findAll(pageable);
        }

        Page<BayReservationResponse> responses = result.map(bayReservationMapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, responses, httpRequest.getRequestURI()));
    }

    @GetMapping("/active")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "List all active bay reservations (IN_USE and HELD)")
    public ResponseEntity<ApiResponse<List<BayReservationResponse>>> getActiveReservations(
            HttpServletRequest httpRequest) {
        List<BayReservation> active = bayReservationRepository.findByStatusIn(
                Set.of(ReservationStatus.IN_USE, ReservationStatus.HELD));
        List<BayReservationResponse> responses = active.stream()
                .map(bayReservationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, responses, httpRequest.getRequestURI()));
    }
}
