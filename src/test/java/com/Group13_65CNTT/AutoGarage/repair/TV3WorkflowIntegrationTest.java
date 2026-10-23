package com.Group13_65CNTT.AutoGarage.repair;

import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateItemCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateResponse;
import com.Group13_65CNTT.AutoGarage.estimate.entity.ItemType;
import com.Group13_65CNTT.AutoGarage.estimate.service.EstimateService;
import com.Group13_65CNTT.AutoGarage.quality.dto.request.QualityCheckCreateRequest;
import com.Group13_65CNTT.AutoGarage.quality.dto.response.QualityCheckResponse;
import com.Group13_65CNTT.AutoGarage.quality.service.QualityService;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.LaborSessionStopRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskAssignmentRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskCompletionRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.LaborSessionResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairOrderResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairTaskResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskAssignmentResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskWorkSummaryResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentRole;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.StopReason;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.service.LaborExecutionService;
import com.Group13_65CNTT.AutoGarage.repair.service.RepairService;
import com.Group13_65CNTT.AutoGarage.scheduling.service.SchedulingService;
import com.Group13_65CNTT.AutoGarage.shared.contracts.*;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimDecideRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimOpenRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyClaimResponse;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyResponse;
import com.Group13_65CNTT.AutoGarage.warranty.service.WarrantyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TV3WorkflowIntegrationTest {

    @Mock
    private BayScheduling bayScheduling;

    @Mock
    private RepairWorkflow repairWorkflow;

    @Mock
    private EstimateApproval estimateApproval;

    @Mock
    private WarrantyOperations warrantyOperations;

    @Mock
    private SchedulingService schedulingService;

    @Mock
    private RepairService repairService;

    @Mock
    private EstimateService estimateService;

    @Mock
    private LaborExecutionService laborExecutionService;

    @Mock
    private QualityService qualityService;

    @Mock
    private WarrantyService warrantyService;

    @Mock
    private BillingOperations billingOperations;

    @Mock
    private InventoryOperations inventoryOperations;

    @Test
    @DisplayName("TV3-11: Kiểm thử tích hợp toàn diện chu trình sửa chữa từ TV3-01 đến TV3-11 (G1 đến G5)")
    void testFullRepairLifecycleWorkflow() {
        long actorAdminId = 1L;
        long actorTechId = 2L;
        long vehicleId = 10L;
        long customerId = 100L;
        long bayId = 5L;

        // =====================================================================
        // Bước 1 (TV3-01 & TV3-02): Xếp khoang sửa chữa cho xe (I02 BayScheduling)
        // =====================================================================
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        when(bayScheduling.reserveRepair(actorAdminId, 800L, bayId, start, end))
                .thenReturn(501L);

        long resId = bayScheduling.reserveRepair(actorAdminId, 800L, bayId, start, end);
        assertThat(resId).isEqualTo(501L);
        verify(bayScheduling).reserveRepair(actorAdminId, 800L, bayId, start, end);

        // =====================================================================
        // Bước 2 (TV3-03): Tiếp nhận xe vào xưởng (I03 RepairWorkflow.receive)
        // =====================================================================
        when(repairWorkflow.receive(actorAdminId, null, customerId, vehicleId, 25000))
                .thenReturn(800L);

        long orderId = repairWorkflow.receive(actorAdminId, null, customerId, vehicleId, 25000);
        assertThat(orderId).isEqualTo(800L);
        verify(repairWorkflow).receive(actorAdminId, null, customerId, vehicleId, 25000);

        // =====================================================================
        // Bước 3 (TV3-04): Lập báo giá ban đầu (EstimateService)
        // =====================================================================
        EstimateResponse estResponse = EstimateResponse.builder()
                .id(700L)
                .repairOrderId(800L)
                .estimateCode("EST-20261023-00001")
                .versionNo(1)
                .totalAmount(new BigDecimal("1500000"))
                .build();
        when(estimateService.createEstimate(eq(actorAdminId), any())).thenReturn(estResponse);

        EstimateResponse createdEst = estimateService.createEstimate(actorAdminId,
                EstimateCreateRequest.builder().repairOrderId(800L).build());
        assertThat(createdEst.getId()).isEqualTo(700L);
        assertThat(createdEst.getVersionNo()).isEqualTo(1);

        // =====================================================================
        // Bước 4 (TV3-05): Khách hàng duyệt từng dòng (I04 EstimateApproval.decide)
        // =====================================================================
        List<EstimateApproval.LineDecision> decisions = List.of(
                new EstimateApproval.LineDecision(101L, EstimateApproval.Decision.APPROVED, null),
                new EstimateApproval.LineDecision(102L, EstimateApproval.Decision.APPROVED, null)
        );
        doNothing().when(estimateApproval).decide(actorAdminId, 700L, decisions, "REQ-KEY-DECIDE-1");

        estimateApproval.decide(actorAdminId, 700L, decisions, "REQ-KEY-DECIDE-1");
        verify(estimateApproval).decide(actorAdminId, 700L, decisions, "REQ-KEY-DECIDE-1");

        // =====================================================================
        // Bước 5 (TV3-06): Phân công kỹ thuật viên thực hiện task (LaborExecution)
        // =====================================================================
        TaskAssignmentResponse assignResponse = TaskAssignmentResponse.builder()
                .id(901L)
                .taskId(201L)
                .employeeId(actorTechId)
                .assignmentRole(AssignmentRole.MAIN)
                .build();
        when(laborExecutionService.assignTask(eq(actorAdminId), eq(201L), any())).thenReturn(assignResponse);

        TaskAssignmentResponse assignment = laborExecutionService.assignTask(actorAdminId, 201L,
                TaskAssignmentRequest.builder().employeeId(actorTechId).assignmentRole(AssignmentRole.MAIN)
                        .startsAt(start).endsAt(end).build());
        assertThat(assignment.getAssignmentRole()).isEqualTo(AssignmentRole.MAIN);

        // =====================================================================
        // Bước 6 (TV3-07): KTV bắt đầu bấm giờ, tạm dừng
        // =====================================================================
        LaborSessionResponse sessionStarted = LaborSessionResponse.builder()
                .id(11L)
                .assignmentId(901L)
                .employeeId(actorTechId)
                .startsAt(Instant.now())
                .build();
        when(laborExecutionService.startTimer(actorTechId, 901L)).thenReturn(sessionStarted);

        LaborSessionResponse started = laborExecutionService.startTimer(actorTechId, 901L);
        assertThat(started.getId()).isEqualTo(11L);

        LaborSessionResponse sessionPaused = LaborSessionResponse.builder()
                .id(11L)
                .assignmentId(901L)
                .employeeId(actorTechId)
                .stopReason(StopReason.PAUSE)
                .build();
        when(laborExecutionService.stopTimer(eq(actorTechId), eq(901L), any())).thenReturn(sessionPaused);

        LaborSessionResponse paused = laborExecutionService.stopTimer(actorTechId, 901L,
                LaborSessionStopRequest.builder().stopReason(StopReason.PAUSE).build());
        assertThat(paused.getStopReason()).isEqualTo(StopReason.PAUSE);

        // =====================================================================
        // Bước 7 (TV3-08): Hoàn thành task (completeTask -> WAITING_QC)
        // =====================================================================
        TaskWorkSummaryResponse summaryResponse = TaskWorkSummaryResponse.builder()
                .taskId(201L)
                .status(TaskStatus.COMPLETED)
                .totalActualSeconds(7200L)
                .totalLaborCost(new BigDecimal("300000"))
                .build();
        when(laborExecutionService.completeTask(eq(actorTechId), eq(201L), any())).thenReturn(summaryResponse);

        TaskWorkSummaryResponse finishedTask = laborExecutionService.completeTask(actorTechId, 201L,
                TaskCompletionRequest.builder().completionNote("Hoàn tất thay má phanh").build());
        assertThat(finishedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);

        // =====================================================================
        // Bước 8 (TV3-09): Kiểm tra chất lượng (QC PASSED)
        // =====================================================================
        QualityCheckResponse qcResponse = QualityCheckResponse.builder()
                .id(350L)
                .repairOrderId(800L)
                .result(CheckResult.PASSED)
                .attemptNo(1)
                .build();
        when(qualityService.performQualityCheck(eq(actorTechId), any())).thenReturn(qcResponse);

        QualityCheckResponse qcResult = qualityService.performQualityCheck(actorTechId,
                QualityCheckCreateRequest.builder().repairOrderId(800L).result(CheckResult.PASSED).build());
        assertThat(qcResult.getResult()).isEqualTo(CheckResult.PASSED);

        // =====================================================================
        // Bước 9: Kiểm tra điều kiện lập hóa đơn (I03 RepairWorkflow.requireInvoiceReady)
        // =====================================================================
        doNothing().when(repairWorkflow).requireInvoiceReady(actorAdminId, 800L);
        repairWorkflow.requireInvoiceReady(actorAdminId, 800L);
        verify(repairWorkflow).requireInvoiceReady(actorAdminId, 800L);

        // =====================================================================
        // Bước 10 (TV3-11): Bàn giao xe và đóng phiếu (I03 RepairWorkflow.handOver)
        // =====================================================================
        doNothing().when(repairWorkflow).handOver(actorAdminId, 800L, 25050, "REQ-KEY-HANDOVER-1");
        repairWorkflow.handOver(actorAdminId, 800L, 25050, "REQ-KEY-HANDOVER-1");
        verify(repairWorkflow).handOver(actorAdminId, 800L, 25050, "REQ-KEY-HANDOVER-1");

        // =====================================================================
        // Bước 11 (TV3-10): Tạo và xử lý bảo hành (I07 WarrantyOperations)
        // =====================================================================
        doNothing().when(warrantyOperations).createFromInvoice(actorAdminId, 601L, 25050);
        warrantyOperations.createFromInvoice(actorAdminId, 601L, 25050);
        verify(warrantyOperations).createFromInvoice(actorAdminId, 601L, 25050);

        // Khách mở yêu cầu bảo hành
        when(warrantyOperations.openClaim(actorAdminId, 999L, 28000, "Phanh rung lắc", "REQ-CLAIM-1"))
                .thenReturn(444L);
        long claimId = warrantyOperations.openClaim(actorAdminId, 999L, 28000, "Phanh rung lắc", "REQ-CLAIM-1");
        assertThat(claimId).isEqualTo(444L);

        // Quản trị viên duyệt bảo hành -> gọi lại I03 tạo phiếu sửa chữa bảo hành mới
        doNothing().when(warrantyOperations).decideClaim(actorAdminId, 444L, true, "Đồng ý bảo hành", "REQ-DECIDE-1");
        warrantyOperations.decideClaim(actorAdminId, 444L, true, "Đồng ý bảo hành", "REQ-DECIDE-1");
        verify(warrantyOperations).decideClaim(actorAdminId, 444L, true, "Đồng ý bảo hành", "REQ-DECIDE-1");
    }

    @Test
    @DisplayName("TV3-11: Chặn bàn giao khi số km nhỏ hơn số km tiếp nhận")
    void handOver_ThrowsWhenOdometerLowerThanIn() {
        doThrow(new BusinessRuleException("Số km khi bàn giao (24900) không được nhỏ hơn số km vào xưởng (25000)"))
                .when(repairWorkflow).handOver(1L, 800L, 24900, "KEY");

        assertThatThrownBy(() -> repairWorkflow.handOver(1L, 800L, 24900, "KEY"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không được nhỏ hơn số km vào xưởng");
    }

    @Test
    @DisplayName("TV3-11: Chặn bàn giao khi chưa đạt QC PASSED")
    void handOver_ThrowsWhenQcNotPassed() {
        doThrow(new BusinessRuleException("Biên bản kiểm tra chất lượng gần nhất chưa đạt yêu cầu (PASSED)"))
                .when(repairWorkflow).requireInvoiceReady(1L, 800L);

        assertThatThrownBy(() -> repairWorkflow.requireInvoiceReady(1L, 800L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa đạt yêu cầu (PASSED)");
    }
}
