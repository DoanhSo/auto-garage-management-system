package com.Group13_65CNTT.AutoGarage.scheduling.service;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.MaintenanceReservationRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayReservationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.EmployeeResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ServiceBayResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SchedulingService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse getEmployeeByUserId(Long userId);

    Page<EmployeeResponse> getEmployees(Boolean isActive, Pageable pageable);

    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);

    void deactivateEmployee(Long id);

    ShiftResponse createShift(Long actorUserId, ShiftCreateRequest request);

    void cancelShift(Long actorUserId, Long shiftId);

    Page<ShiftResponse> getEmployeeShifts(Long employeeId, Pageable pageable);

    Page<ShiftResponse> getShifts(Long employeeId, ShiftStatus status, Pageable pageable);

    ServiceBayResponse createServiceBay(ServiceBayCreateRequest request);

    ServiceBayResponse getServiceBayById(Long id);

    Page<ServiceBayResponse> getServiceBays(Boolean isActive, Pageable pageable);

    ServiceBayResponse updateServiceBay(Long id, ServiceBayUpdateRequest request);

    void deactivateServiceBay(Long id);

    BayReservationResponse scheduleMaintenance(Long actorUserId, MaintenanceReservationRequest request);

    Page<BayReservationResponse> getBayReservations(Long bayId, Pageable pageable);
}
