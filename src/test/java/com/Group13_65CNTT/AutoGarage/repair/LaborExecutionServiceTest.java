package com.Group13_65CNTT.AutoGarage.repair;

import com.Group13_65CNTT.AutoGarage.repair.dto.request.LaborSessionStopRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskAssignmentRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskCompletionRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.LaborSessionResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskAssignmentResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskWorkSummaryResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.*;
import com.Group13_65CNTT.AutoGarage.repair.mapper.LaborSessionMapper;
import com.Group13_65CNTT.AutoGarage.repair.mapper.TaskAssignmentMapper;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.TaskAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.repair.service.impl.LaborExecutionServiceImpl;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaborExecutionServiceTest {

    @Mock
    private RepairTaskRepository repairTaskRepository;
    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private LaborSessionRepository laborSessionRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TaskAssignmentMapper taskAssignmentMapper;
    @Mock
    private LaborSessionMapper laborSessionMapper;

    @InjectMocks
    private LaborExecutionServiceImpl laborExecutionService;

    private RepairOrder mockOrder;
    private RepairTask mockTask;
    private Employee mockEmployee;
    private TaskAssignment mockAssignment;

    @BeforeEach
    void setUp() {
        mockOrder = RepairOrder.builder()
                .id(100L)
                .orderCode("RO-20261023-00001")
                .status(RepairOrderStatus.APPROVED)
                .build();

        mockTask = RepairTask.builder()
                .id(10L)
                .repairOrderId(100L)
                .taskType(TaskType.REPAIR)
                .taskName("Thay lọc gió điều hòa")
                .status(TaskStatus.PENDING)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("150000.00"))
                .build();

        mockEmployee = Employee.builder()
                .id(5L)
                .userId(50L)
                .employeeCode("EMP-005")
                .canQc(false)
                .hourlyCost(new BigDecimal("80000.00"))
                .isActive(true)
                .build();

        mockAssignment = TaskAssignment.builder()
                .id(1L)
                .taskId(10L)
                .employeeId(5L)
                .assignmentRole(AssignmentRole.MAIN)
                .isActive(true)
                .startsAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .endsAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
    }

    // =========================================================================
    // TV3-06 Phân công KTV
    // =========================================================================

    @Test
    @DisplayName("TV3-06: Phân công thợ chính (MAIN) thành công, cập nhật trạng thái task và order")
    void assignTask_Main_Success() {
        Instant startsAt = Instant.now();
        Instant endsAt = startsAt.plus(2, ChronoUnit.HOURS);
        TaskAssignmentRequest req = TaskAssignmentRequest.builder()
                .employeeId(5L)
                .assignmentRole(AssignmentRole.MAIN)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        when(repairTaskRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockTask));
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));
        when(taskAssignmentRepository.findByTaskIdAndAssignmentRoleAndIsActiveTrue(10L, AssignmentRole.MAIN)).thenReturn(Optional.empty());
        when(taskAssignmentRepository.findOverlappingAssignments(eq(5L), any(), any())).thenReturn(List.of());
        when(taskAssignmentMapper.toEntity(req)).thenReturn(mockAssignment);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(mockAssignment);

        TaskAssignmentResponse resDto = TaskAssignmentResponse.builder().id(1L).taskId(10L).employeeId(5L).build();
        when(taskAssignmentMapper.toResponse(mockAssignment)).thenReturn(resDto);

        TaskAssignmentResponse result = laborExecutionService.assignTask(1L, 10L, req);

        assertThat(result).isNotNull();
        assertThat(mockTask.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.IN_PROGRESS);
        verify(repairTaskRepository).save(mockTask);
        verify(repairOrderRepository).save(mockOrder);
    }

    @Test
    @DisplayName("TV3-06: Phân công QC thất bại nếu KTV không có quyền can_qc")
    void assignTask_QcWithoutCanQc_ThrowsException() {
        Instant startsAt = Instant.now();
        Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);
        TaskAssignmentRequest req = TaskAssignmentRequest.builder()
                .employeeId(5L)
                .assignmentRole(AssignmentRole.QC)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        when(repairTaskRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockTask));
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));

        assertThatThrownBy(() -> laborExecutionService.assignTask(1L, 10L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("can_qc = false");
    }

    @Test
    @DisplayName("TV3-06: Phân công thất bại khi công việc đã có thợ chính (MAIN) active")
    void assignTask_DuplicateMain_ThrowsConflict() {
        Instant startsAt = Instant.now();
        Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);
        TaskAssignmentRequest req = TaskAssignmentRequest.builder()
                .employeeId(5L)
                .assignmentRole(AssignmentRole.MAIN)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        when(repairTaskRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockTask));
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));
        when(taskAssignmentRepository.findByTaskIdAndAssignmentRoleAndIsActiveTrue(10L, AssignmentRole.MAIN))
                .thenReturn(Optional.of(mockAssignment));

        assertThatThrownBy(() -> laborExecutionService.assignTask(1L, 10L, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã có thợ chính (MAIN) đang phụ trách");
    }

    @Test
    @DisplayName("TV3-06: Phân công thất bại khi KTV bị trùng ca/thời gian với việc khác")
    void assignTask_OverlappingSchedule_ThrowsConflict() {
        Instant startsAt = Instant.now();
        Instant endsAt = startsAt.plus(2, ChronoUnit.HOURS);
        TaskAssignmentRequest req = TaskAssignmentRequest.builder()
                .employeeId(5L)
                .assignmentRole(AssignmentRole.ASSISTANT)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        when(repairTaskRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockTask));
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));
        when(taskAssignmentRepository.findOverlappingAssignments(eq(5L), any(), any()))
                .thenReturn(List.of(mockAssignment));

        assertThatThrownBy(() -> laborExecutionService.assignTask(1L, 10L, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã có phân công công việc khác trong khoảng thời gian này");
    }

    // =========================================================================
    // TV3-07 Timer KTV
    // =========================================================================

    @Test
    @DisplayName("TV3-07: Bắt đầu bấm giờ (Timer) thành công, snapshot chi phí công và chuyển task IN_PROGRESS")
    void startTimer_Success() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));
        when(laborSessionRepository.findTopByEmployeeIdAndEndsAtIsNull(5L)).thenReturn(Optional.empty());

        LaborSession session = LaborSession.builder()
                .id(99L)
                .assignmentId(1L)
                .employeeId(5L)
                .startsAt(Instant.now())
                .hourlyCostSnapshot(new BigDecimal("80000.00"))
                .build();
        when(laborSessionRepository.save(any(LaborSession.class))).thenReturn(session);
        when(repairTaskRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockTask));

        LaborSessionResponse resDto = LaborSessionResponse.builder().id(99L).build();
        when(laborSessionMapper.toResponse(session)).thenReturn(resDto);

        LaborSessionResponse response = laborExecutionService.startTimer(1L, 1L);

        assertThat(response).isNotNull();
        assertThat(mockTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(repairTaskRepository).save(mockTask);
    }

    @Test
    @DisplayName("TV3-07: Bắt đầu bấm giờ thất bại nếu KTV đang có một timer khác chưa kết thúc")
    void startTimer_AlreadyHasOpenTimer_ThrowsConflict() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));

        LaborSession openSession = LaborSession.builder().id(88L).employeeId(5L).build();
        when(laborSessionRepository.findTopByEmployeeIdAndEndsAtIsNull(5L)).thenReturn(Optional.of(openSession));

        assertThatThrownBy(() -> laborExecutionService.startTimer(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đang có một phiên bấm giờ chưa kết thúc");
    }

    @Test
    @DisplayName("TV3-07: Tạm dừng bấm giờ (PAUSE) cập nhật stop_reason và chuyển task sang PAUSED")
    void stopTimer_Pause_Success() {
        LaborSession openSession = LaborSession.builder()
                .id(99L)
                .assignmentId(1L)
                .employeeId(5L)
                .startsAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                .build();

        mockTask.setStatus(TaskStatus.IN_PROGRESS);

        when(laborSessionRepository.findTopByAssignmentIdAndEndsAtIsNull(1L)).thenReturn(Optional.of(openSession));
        when(laborSessionRepository.save(openSession)).thenReturn(openSession);
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(repairTaskRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockTask));

        LaborSessionResponse resDto = LaborSessionResponse.builder().id(99L).stopReason(StopReason.PAUSE).build();
        when(laborSessionMapper.toResponse(openSession)).thenReturn(resDto);

        LaborSessionResponse response = laborExecutionService.stopTimer(1L, 1L, new LaborSessionStopRequest(StopReason.PAUSE));

        assertThat(openSession.getEndsAt()).isNotNull();
        assertThat(openSession.getStopReason()).isEqualTo(StopReason.PAUSE);
        assertThat(mockTask.getStatus()).isEqualTo(TaskStatus.PAUSED);
    }

    // =========================================================================
    // TV3-08 Hoàn thành công việc
    // =========================================================================

    @Test
    @DisplayName("TV3-08: Hoàn thành công việc đóng timer, giải phóng phân công và chuyển phiếu sang WAITING_QC khi hết việc")
    void completeTask_Success() {
        LaborSession openSession = LaborSession.builder().id(99L).assignmentId(1L).employeeId(5L).startsAt(Instant.now().minus(1, ChronoUnit.HOURS)).build();
        when(repairTaskRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockTask));
        when(laborSessionRepository.findOpenSessionsByTaskId(10L)).thenReturn(List.of(openSession));
        when(taskAssignmentRepository.findByTaskIdAndIsActiveTrue(10L)).thenReturn(List.of(mockAssignment));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(100L, TaskStatus.COMPLETED)).thenReturn(false);
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));

        when(repairTaskRepository.findById(10L)).thenReturn(Optional.of(mockTask));
        when(taskAssignmentRepository.findByTaskId(10L)).thenReturn(List.of(mockAssignment));
        when(laborSessionRepository.findByAssignmentIdIn(anyList())).thenReturn(List.of(openSession));

        openSession.setEndsAt(Instant.now());
        TaskWorkSummaryResponse summary = laborExecutionService.completeTask(1L, 10L, new TaskCompletionRequest("Thay thế hoàn tất"));

        assertThat(mockTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(mockTask.getCompletedAt()).isNotNull();
        assertThat(mockAssignment.getIsActive()).isFalse();
        assertThat(openSession.getStopReason()).isEqualTo(StopReason.COMPLETE);
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.WAITING_QC);
        verify(repairOrderRepository).save(mockOrder);
    }
}
