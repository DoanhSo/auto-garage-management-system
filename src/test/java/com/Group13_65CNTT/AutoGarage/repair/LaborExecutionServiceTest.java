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

    @Test
    void assignmentOutsideActiveShiftIsRejected() {
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));
        var request = TaskAssignmentRequest.builder().employeeId(5L).assignmentRole(AssignmentRole.MAIN)
                .startsAt(Instant.now()).endsAt(Instant.now().plusSeconds(3600)).build();
        assertThatThrownBy(() -> laborExecutionService.assignTask(1L, 10L, request))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("ca làm việc");
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    void technicianWithoutRequiredSkillsCannotBeAssigned() {
        mockTask.setServiceId(20L);
        mockEmployee.setSkillsJson("[\"ENGINE\"]");
        var catalogService = new com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity();
        catalogService.setRequiredSkillsJson("[\"BRAKES\"]");
        when(serviceRepository.findById(20L)).thenReturn(Optional.of(catalogService));
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));
        when(shiftRepository.coversAssignment(eq(5L), any(), any())).thenReturn(true);
        var request = TaskAssignmentRequest.builder().employeeId(5L).assignmentRole(AssignmentRole.MAIN)
                .startsAt(Instant.now()).endsAt(Instant.now().plusSeconds(3600)).build();
        assertThatThrownBy(() -> laborExecutionService.assignTask(1L, 10L, request))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("kỹ năng");
        verify(taskAssignmentRepository, never()).save(any());
    }

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

    @Mock
    private com.Group13_65CNTT.AutoGarage.repair.service.WorkLogRecorder workLogRecorder;
    @InjectMocks
    private LaborExecutionServiceImpl laborExecutionService;
    @Mock
    private com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService objectAccess;
    @Mock
    private com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository shiftRepository;
    @Mock
    private com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository serviceRepository;
    @Mock
    private com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderAssignmentRepository workLogRepository;
    @Mock
    private com.Group13_65CNTT.AutoGarage.scheduling.service.DispatchService dispatchService;

    private RepairOrder mockOrder;
    private RepairTask mockTask;
    private Employee mockEmployee;
    private TaskAssignment mockAssignment;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                laborExecutionService, "inventoryOperations", inventoryOperations);
        mockOrder = RepairOrder.builder()
                .id(100L)
                .orderCode("RO-20261023-00001")
                .status(RepairOrderStatus.APPROVED)
                .build();
        lenient().when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));

        mockTask = RepairTask.builder()
                .id(10L)
                .repairOrderId(100L)
                .taskType(TaskType.REPAIR)
                .taskName("Thay lọc gió điều hòa")
                .status(TaskStatus.PENDING)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("150000.00"))
                .build();
        lenient().when(repairTaskRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockTask));

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
        when(shiftRepository.coversAssignment(eq(5L), any(), any())).thenReturn(true);
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

    // ---- Cổng bắt đầu công việc: báo giá đã duyệt và phụ tùng đã xuất kho ----

    @Mock
    private com.Group13_65CNTT.AutoGarage.shared.contracts.InventoryOperations inventoryOperations;

    private void stubTimerBasics() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
    }

    @Test
    @DisplayName("Cổng bắt đầu việc: phiếu chờ duyệt báo giá bị chặn WORK_START_BLOCKED")
    void startTimer_WaitingApproval_Blocked() {
        stubTimerBasics();
        mockOrder.setStatus(RepairOrderStatus.WAITING_APPROVAL);

        assertThatThrownBy(() -> laborExecutionService.startTimer(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("responseCode",
                        com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.WORK_START_BLOCKED)
                .hasMessageContaining("duyệt báo giá");
        verify(laborSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cổng bắt đầu việc: phiếu chờ phụ tùng bị chặn")
    void startTimer_WaitingParts_Blocked() {
        stubTimerBasics();
        mockOrder.setStatus(RepairOrderStatus.WAITING_PARTS);

        assertThatThrownBy(() -> laborExecutionService.startTimer(1L, 1L))
                .hasFieldOrPropertyWithValue("responseCode",
                        com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.WORK_START_BLOCKED)
                .hasMessageContaining("chờ phụ tùng");
    }

    @Test
    @DisplayName("Cổng bắt đầu việc: còn phụ tùng giữ chỗ chưa xuất kho thì nêu tên và dặn xuất kho")
    void startTimer_PartsNotIssued_Blocked() {
        stubTimerBasics();
        doThrow(new ConflictException(com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.WORK_START_BLOCKED,
                "Còn phụ tùng đã giữ chỗ nhưng chưa xuất kho: Lọc dầu. Hãy xuất kho trước khi bắt đầu công việc."))
                .when(inventoryOperations).requireIssuedForWorkStart(100L);

        assertThatThrownBy(() -> laborExecutionService.startTimer(1L, 1L))
                .hasMessageContaining("Lọc dầu")
                .hasMessageContaining("xuất kho");
        verify(laborSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cổng bắt đầu việc: làm lại sau QC trượt vẫn bấm giờ được khi đã xuất đủ")
    void startTimer_QcFailedRework_Allowed() {
        stubTimerBasics();
        mockOrder.setStatus(RepairOrderStatus.QC_FAILED);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));
        when(laborSessionRepository.findTopByEmployeeIdAndEndsAtIsNull(5L)).thenReturn(Optional.empty());
        LaborSession session = LaborSession.builder().id(99L).assignmentId(1L).employeeId(5L).build();
        when(laborSessionRepository.save(any(LaborSession.class))).thenReturn(session);
        when(laborSessionMapper.toResponse(session)).thenReturn(LaborSessionResponse.builder().id(99L).build());

        assertThat(laborExecutionService.startTimer(1L, 1L)).isNotNull();
        verify(inventoryOperations).requireIssuedForWorkStart(100L);
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

    // =========================================================================
    // Phân công kỹ thuật viên QC cho phiếu đang chờ QC
    // =========================================================================

    private com.Group13_65CNTT.AutoGarage.repair.dto.request.QcAssignmentRequest qcRequest(long employeeId) {
        Instant startsAt = Instant.now();
        return com.Group13_65CNTT.AutoGarage.repair.dto.request.QcAssignmentRequest.builder()
                .employeeId(employeeId).startsAt(startsAt).endsAt(startsAt.plus(1, ChronoUnit.HOURS)).build();
    }

    private void orderWaitingQcWithCompletedTask() {
        mockOrder.setStatus(RepairOrderStatus.WAITING_QC);
        mockTask.setStatus(TaskStatus.COMPLETED);
        lenient().when(repairTaskRepository.findByRepairOrderId(100L)).thenReturn(List.of(mockTask));
        mockEmployee.setCanQc(true);
        lenient().when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));
    }

    @Test
    @DisplayName("Phân công QC thành công khi phiếu chờ QC: tạo phân công vai trò QC gắn vào công việc cuối")
    void assignQcInspector_Success() {
        orderWaitingQcWithCompletedTask();
        when(taskAssignmentRepository.findActiveQcForOrder(100L)).thenReturn(List.of());
        when(taskAssignmentRepository.findOverlappingAssignments(eq(5L), any(), any())).thenReturn(List.of());
        when(shiftRepository.coversAssignment(eq(5L), any(), any())).thenReturn(true);
        when(taskAssignmentMapper.toEntity(any(TaskAssignmentRequest.class))).thenReturn(mockAssignment);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(mockAssignment);
        when(taskAssignmentMapper.toResponse(mockAssignment))
                .thenReturn(TaskAssignmentResponse.builder().id(1L).taskId(10L).employeeId(5L).build());

        TaskAssignmentResponse result = laborExecutionService.assignQcInspector(1L, 100L, qcRequest(5L));

        assertThat(result.getEmployeeId()).isEqualTo(5L);
        org.mockito.ArgumentCaptor<TaskAssignmentRequest> mapped =
                org.mockito.ArgumentCaptor.forClass(TaskAssignmentRequest.class);
        verify(taskAssignmentMapper).toEntity(mapped.capture());
        assertThat(mapped.getValue().getAssignmentRole()).isEqualTo(AssignmentRole.QC);
        verify(workLogRecorder, never()).start(any(), any(), any(), any(), any(), any());
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.WAITING_QC);
    }

    @Test
    @DisplayName("Đổi người QC: phân công QC cũ được giải phóng trước khi tạo phân công mới")
    void assignQcInspector_ReplacesPreviousInspector() {
        orderWaitingQcWithCompletedTask();
        TaskAssignment previous = TaskAssignment.builder().id(9L).taskId(10L).employeeId(7L)
                .assignmentRole(AssignmentRole.QC).isActive(true).build();
        when(taskAssignmentRepository.findActiveQcForOrder(100L)).thenReturn(List.of(previous));
        when(taskAssignmentRepository.findOverlappingAssignments(eq(5L), any(), any())).thenReturn(List.of());
        when(shiftRepository.coversAssignment(eq(5L), any(), any())).thenReturn(true);
        when(taskAssignmentMapper.toEntity(any(TaskAssignmentRequest.class))).thenReturn(mockAssignment);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(mockAssignment);
        when(taskAssignmentMapper.toResponse(mockAssignment)).thenReturn(TaskAssignmentResponse.builder().id(1L).build());

        laborExecutionService.assignQcInspector(1L, 100L, qcRequest(5L));

        assertThat(previous.getIsActive()).isFalse();
        assertThat(previous.getReleasedAt()).isNotNull();
        verify(taskAssignmentRepository).saveAll(List.of(previous));
    }

    @Test
    @DisplayName("Không phân công QC khi phiếu chưa ở trạng thái chờ QC")
    void assignQcInspector_RejectsWhenNotWaitingQc() {
        orderWaitingQcWithCompletedTask();
        mockOrder.setStatus(RepairOrderStatus.IN_PROGRESS);

        assertThatThrownBy(() -> laborExecutionService.assignQcInspector(1L, 100L, qcRequest(5L)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("chờ QC");
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không phân công QC khi phiếu chưa có công việc")
    void assignQcInspector_RejectsWhenNoTasks() {
        mockOrder.setStatus(RepairOrderStatus.WAITING_QC);
        when(repairTaskRepository.findByRepairOrderId(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> laborExecutionService.assignQcInspector(1L, 100L, qcRequest(5L)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("chưa có công việc");
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thợ chính của phiếu không được làm kỹ thuật viên QC")
    void assignQcInspector_RejectsMainRepairer() {
        orderWaitingQcWithCompletedTask();
        when(workLogRepository.findByRepairOrderIdAndAssignmentStage(100L, AssignmentStage.REPAIR_MAIN))
                .thenReturn(List.of(RepairOrderAssignment.builder().employeeId(5L).build()));

        assertThatThrownBy(() -> laborExecutionService.assignQcInspector(1L, 100L, qcRequest(5L)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Thợ chính");
        verify(taskAssignmentRepository, never()).saveAll(any());
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nhân viên không có quyền QC không được phân công QC")
    void assignQcInspector_RejectsEmployeeWithoutCanQc() {
        orderWaitingQcWithCompletedTask();
        mockEmployee.setCanQc(false);
        when(taskAssignmentRepository.findActiveQcForOrder(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> laborExecutionService.assignQcInspector(1L, 100L, qcRequest(5L)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("can_qc");
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy phân công QC đang hoạt động: có tên kỹ thuật viên, và null khi chưa có")
    void getQcAssignment_ReturnsActiveOrNull() {
        TaskAssignment active = TaskAssignment.builder().id(9L).taskId(10L).employeeId(5L)
                .assignmentRole(AssignmentRole.QC).isActive(true).build();
        when(taskAssignmentRepository.findActiveQcForOrder(100L)).thenReturn(List.of(active));
        when(taskAssignmentMapper.toResponse(active))
                .thenReturn(TaskAssignmentResponse.builder().id(9L).employeeId(5L).build());
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(mockEmployee));

        assertThat(laborExecutionService.getQcAssignment(100L).getId()).isEqualTo(9L);

        when(taskAssignmentRepository.findActiveQcForOrder(100L)).thenReturn(List.of());
        assertThat(laborExecutionService.getQcAssignment(100L)).isNull();
    }

    // =========================================================================
    // Danh sách ứng viên QC theo ca đang trực
    // =========================================================================

    private com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse onShift(
            long employeeId, String name, boolean canQc, Long bayId, String bayCode) {
        return new com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse(
                employeeId, "NV-" + employeeId, name, 100L + employeeId, null, null,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(3, ChronoUnit.HOURS),
                bayId, bayCode, bayId == null ? null : "Khoang " + bayCode, List.of("ENGINE"), canQc,
                0, 0, BigDecimal.ZERO, false, null,
                com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse.Availability.AVAILABLE);
    }

    @Test
    @DisplayName("Ứng viên QC: chỉ người có quyền QC đang trong ca; người ca gắn khoang vẫn chọn được")
    void getQcCandidates_ListsQcCapableOnShiftIncludingBayFixed() {
        when(repairOrderRepository.existsById(100L)).thenReturn(true);
        when(workLogRepository.findByRepairOrderIdAndAssignmentStage(100L, AssignmentStage.REPAIR_MAIN))
                .thenReturn(List.of());
        when(dispatchService.getOnShiftTechnicians()).thenReturn(List.of(
                onShift(1L, "An", true, 6L, "B06"),
                onShift(2L, "Binh", false, null, null),
                onShift(3L, "Chi", true, null, null)));

        var result = laborExecutionService.getQcCandidates(100L);

        assertThat(result).extracting(c -> c.employeeId()).containsExactly(1L, 3L);
        var bayFixed = result.get(0);
        assertThat(bayFixed.eligible()).isTrue();
        assertThat(bayFixed.reason()).isNull();
        assertThat(bayFixed.serviceBayId()).isEqualTo(6L);
        assertThat(bayFixed.bayCode()).isEqualTo("B06");
    }

    @Test
    @DisplayName("Ứng viên QC: thợ chính của phiếu vẫn hiện nhưng bị khóa với lý do MAIN_REPAIRER, xếp sau người chọn được")
    void getQcCandidates_MarksMainRepairerIneligible() {
        when(repairOrderRepository.existsById(100L)).thenReturn(true);
        when(workLogRepository.findByRepairOrderIdAndAssignmentStage(100L, AssignmentStage.REPAIR_MAIN))
                .thenReturn(List.of(RepairOrderAssignment.builder().employeeId(1L).build()));
        when(dispatchService.getOnShiftTechnicians()).thenReturn(List.of(
                onShift(1L, "An", true, 6L, "B06"),
                onShift(3L, "Chi", true, null, null)));

        var result = laborExecutionService.getQcCandidates(100L);

        assertThat(result).extracting(c -> c.employeeId()).containsExactly(3L, 1L);
        assertThat(result.get(1).eligible()).isFalse();
        assertThat(result.get(1).reason())
                .isEqualTo(com.Group13_65CNTT.AutoGarage.repair.dto.response.QcCandidateResponse.Reason.MAIN_REPAIRER);
    }

    @Test
    @DisplayName("Ứng viên QC: không ai trong ca lúc này (ca chỉ ở ngày đã qua) thì danh sách rỗng")
    void getQcCandidates_EmptyWhenNobodyOnShiftNow() {
        when(repairOrderRepository.existsById(100L)).thenReturn(true);
        when(workLogRepository.findByRepairOrderIdAndAssignmentStage(100L, AssignmentStage.REPAIR_MAIN))
                .thenReturn(List.of());
        when(dispatchService.getOnShiftTechnicians()).thenReturn(List.of());

        assertThat(laborExecutionService.getQcCandidates(100L)).isEmpty();
    }

    @Test
    @DisplayName("Ứng viên QC: phiếu không tồn tại thì báo không tìm thấy")
    void getQcCandidates_UnknownOrder() {
        when(repairOrderRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> laborExecutionService.getQcCandidates(999L))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException.class);
        verifyNoInteractions(dispatchService);
    }
}
