package com.Group13_65CNTT.AutoGarage.quality;

import com.Group13_65CNTT.AutoGarage.quality.dto.request.QualityCheckCreateRequest;
import com.Group13_65CNTT.AutoGarage.quality.dto.response.QualityCheckResponse;
import com.Group13_65CNTT.AutoGarage.quality.mapper.QualityCheckMapper;
import com.Group13_65CNTT.AutoGarage.quality.service.impl.QualityServiceImpl;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityCheckServiceTest {
    @Mock private com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService objectAccess;
    @Mock private com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;

    @Mock
    private VehicleCheckRepository vehicleCheckRepository;
    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private RepairTaskRepository repairTaskRepository;
    @Mock
    private LaborSessionRepository laborSessionRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private QualityCheckMapper qualityCheckMapper;

    @Mock
    private com.Group13_65CNTT.AutoGarage.repair.repository.TaskAssignmentRepository assignmentRepository;

    @InjectMocks
    private QualityServiceImpl qualityService;

    private RepairOrder mockOrder;
    private Employee mockEmployee;

    @BeforeEach
    void setUp() {
        mockOrder = RepairOrder.builder()
                .id(100L)
                .orderCode("RO-20261023-00001")
                .status(RepairOrderStatus.WAITING_QC)
                .build();

        mockEmployee = Employee.builder()
                .id(1L)
                .userId(10L)
                .canQc(true)
                .build();
    }

    @Test
    void missingEmployeeCannotRecordQc() {
        QualityCheckCreateRequest request = QualityCheckCreateRequest.builder()
                .repairOrderId(100L).result(CheckResult.PASSED).build();
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qualityService.performQualityCheck(10L, request))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.common.exception.BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.FORBIDDEN);

        verify(vehicleCheckRepository, never()).save(any());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    void qcAssignmentFailureIsCheckedBeforeAnyMutation() {
        QualityCheckCreateRequest request = QualityCheckCreateRequest.builder()
                .repairOrderId(100L).result(CheckResult.PASSED).build();
        doThrow(new com.Group13_65CNTT.AutoGarage.common.exception.BusinessException(
                com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.FORBIDDEN))
                .when(objectAccess).requireQualityCheckAccess(10L, 100L);

        assertThatThrownBy(() -> qualityService.performQualityCheck(10L, request))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.common.exception.BusinessException.class);

        verifyNoInteractions(repairOrderRepository, vehicleCheckRepository, employeeRepository);
    }

    @Test
    @DisplayName("QC PASSED thành công: chuyển phiếu sang COMPLETED và cập nhật latest_qc_check_id")
    void performQualityCheck_Passed_Success() {
        QualityCheckCreateRequest req = QualityCheckCreateRequest.builder()
                .repairOrderId(100L)
                .result(CheckResult.PASSED)
                .checklistJson("{\"brakes\": \"OK\", \"engine\": \"OK\"}")
                .odometer(55000)
                .notes("Xe đạt chuẩn chất lượng")
                .build();

        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(100L, TaskStatus.COMPLETED)).thenReturn(false);
        when(laborSessionRepository.countOpenSessionsByRepairOrderId(100L)).thenReturn(0L);
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(mockEmployee));
        when(vehicleCheckRepository.findMaxAttemptNo(100L, CheckType.QC)).thenReturn(0);

        VehicleCheck savedCheck = new VehicleCheck();
        savedCheck.setId(500L);
        savedCheck.setAttemptNo(1);
        savedCheck.setResult(CheckResult.PASSED);
        when(vehicleCheckRepository.save(any(VehicleCheck.class))).thenReturn(savedCheck);

        QualityCheckResponse resDto = QualityCheckResponse.builder().id(500L).result(CheckResult.PASSED).build();
        when(qualityCheckMapper.toResponse(savedCheck)).thenReturn(resDto);

        QualityCheckResponse response = qualityService.performQualityCheck(10L, req);

        assertThat(response).isNotNull();
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.COMPLETED);
        assertThat(mockOrder.getLatestQcCheckId()).isEqualTo(500L);
        verify(repairOrderRepository).save(mockOrder);
    }

    @Test
    @DisplayName("QC FAILED: chuyển phiếu sang QC_FAILED và chuyển task sang REWORK để làm lại")
    void performQualityCheck_Failed_Rework_Success() {
        QualityCheckCreateRequest req = QualityCheckCreateRequest.builder()
                .repairOrderId(100L)
                .result(CheckResult.FAILED)
                .checklistJson("{\"brakes\": \"FAIL\"}")
                .notes("Má phanh lắp chưa khớp")
                .reworkTaskIds(List.of(20L))
                .build();

        RepairTask task = RepairTask.builder()
                .id(20L)
                .repairOrderId(100L)
                .status(TaskStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();

        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(100L, TaskStatus.COMPLETED)).thenReturn(false);
        when(laborSessionRepository.countOpenSessionsByRepairOrderId(100L)).thenReturn(0L);
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(mockEmployee));
        when(vehicleCheckRepository.findMaxAttemptNo(100L, CheckType.QC)).thenReturn(0);

        VehicleCheck savedCheck = new VehicleCheck();
        savedCheck.setId(501L);
        savedCheck.setAttemptNo(1);
        savedCheck.setResult(CheckResult.FAILED);
        when(vehicleCheckRepository.save(any(VehicleCheck.class))).thenReturn(savedCheck);

        when(repairTaskRepository.findAllById(List.of(20L))).thenReturn(List.of(task));

        QualityCheckResponse resDto = QualityCheckResponse.builder().id(501L).result(CheckResult.FAILED).build();
        when(qualityCheckMapper.toResponse(savedCheck)).thenReturn(resDto);

        QualityCheckResponse response = qualityService.performQualityCheck(10L, req);

        assertThat(response).isNotNull();
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.QC_FAILED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.REWORK);
        assertThat(task.getCompletedAt()).isNull();
        verify(repairTaskRepository).saveAll(List.of(task));
    }

    @Test
    @DisplayName("QC bị chặn khi vẫn còn công việc chưa hoàn thành")
    void performQualityCheck_UnfinishedTasks_ThrowsException() {
        QualityCheckCreateRequest req = QualityCheckCreateRequest.builder()
                .repairOrderId(100L)
                .result(CheckResult.PASSED)
                .build();

        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(100L, TaskStatus.COMPLETED)).thenReturn(true);

        assertThatThrownBy(() -> qualityService.performQualityCheck(10L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Vẫn còn công việc chưa hoàn thành");
    }

    @Test
    @DisplayName("QC bị chặn khi nhân viên không có thẩm quyền can_qc")
    void performQualityCheck_WithoutCanQc_ThrowsException() {
        mockEmployee.setCanQc(false);
        QualityCheckCreateRequest req = QualityCheckCreateRequest.builder()
                .repairOrderId(100L)
                .result(CheckResult.PASSED)
                .build();

        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(100L, TaskStatus.COMPLETED)).thenReturn(false);
        when(laborSessionRepository.countOpenSessionsByRepairOrderId(100L)).thenReturn(0L);
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(mockEmployee));

        assertThatThrownBy(() -> qualityService.performQualityCheck(10L, req))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.common.exception.BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.FORBIDDEN);
    }

    @Test
    @DisplayName("QC tự động tăng attempt_no cho các lần kiểm tra tiếp theo")
    void performQualityCheck_IncrementAttemptNo() {
        QualityCheckCreateRequest req = QualityCheckCreateRequest.builder()
                .repairOrderId(100L)
                .result(CheckResult.PASSED)
                .build();

        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(100L, TaskStatus.COMPLETED)).thenReturn(false);
        when(laborSessionRepository.countOpenSessionsByRepairOrderId(100L)).thenReturn(0L);
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(mockEmployee));
        when(vehicleCheckRepository.findMaxAttemptNo(100L, CheckType.QC)).thenReturn(2);

        when(vehicleCheckRepository.save(any(VehicleCheck.class))).thenAnswer(inv -> {
            VehicleCheck vc = inv.getArgument(0);
            assertThat(vc.getAttemptNo()).isEqualTo(3);
            vc.setId(502L);
            return vc;
        });

        qualityService.performQualityCheck(10L, req);
    }
}
