package com.Group13_65CNTT.AutoGarage.estimate;

import com.Group13_65CNTT.AutoGarage.inventory.repository.PartRepository;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.estimate.entity.*;
import com.Group13_65CNTT.AutoGarage.estimate.mapper.EstimateItemMapper;
import com.Group13_65CNTT.AutoGarage.estimate.mapper.EstimateMapper;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateItemRepository;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateRepository;
import com.Group13_65CNTT.AutoGarage.estimate.service.impl.EstimateServiceImpl;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskType;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.EstimateApproval;
import com.Group13_65CNTT.AutoGarage.shared.contracts.InventoryOperations;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstimateApprovalServiceTest {

    @Mock
    private EstimateRepository estimateRepository;
    @Mock
    private EstimateItemRepository estimateItemRepository;
    @Mock
    private EstimateMapper estimateMapper;
    @Mock
    private EstimateItemMapper estimateItemMapper;
    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private RepairTaskRepository repairTaskRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private PartRepository partRepository;
    @Mock
    private PaginationPolicy pagination;

    @Mock
    private InventoryOperations inventoryOperations;
    @Mock
    private NotificationAudit notificationAudit;

    @InjectMocks
    private EstimateServiceImpl estimateService;

    private RepairOrder mockOrder;
    private Estimate mockEstimate;
    private EstimateItem itemService;
    private EstimateItem itemPart;

    @BeforeEach
    void setUp() {
        mockOrder = RepairOrder.builder()
                .id(100L)
                .orderCode("RO-20261023-00001")
                .status(RepairOrderStatus.WAITING_APPROVAL)
                .build();

        mockEstimate = Estimate.builder()
                .id(1L)
                .estimateCode("EST-20261023-00001")
                .repairOrderId(100L)
                .versionNo(1)
                .status(EstimateStatus.SENT)
                .build();

        itemService = EstimateItem.builder()
                .id(10L)
                .estimateId(1L)
                .lineNo(1)
                .itemType(ItemType.SERVICE)
                .serviceId(55L)
                .itemName("Thay má phanh trước")
                .quantity(new BigDecimal("1.000"))
                .unitPrice(new BigDecimal("200000.00"))
                .approvalStatus(ApprovalStatus.PENDING)
                .build();

        itemPart = EstimateItem.builder()
                .id(20L)
                .estimateId(1L)
                .lineNo(2)
                .itemType(ItemType.PART)
                .partId(77L)
                .itemName("Má phanh trước Toyota")
                .quantity(new BigDecimal("2.000"))
                .unitPrice(new BigDecimal("450000.00"))
                .approvalStatus(ApprovalStatus.PENDING)
                .build();

        ReflectionTestUtils.setField(estimateService, "inventoryOperations", inventoryOperations);
        ReflectionTestUtils.setField(estimateService, "notificationAudit", notificationAudit);
    }

    @Test
    @DisplayName("I04.decide: Phê duyệt cả SERVICE và PART sinh task và giữ kho thành công")
    void decide_ApproveAll_Success() {
        when(estimateRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockEstimate));
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(estimateItemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(itemService));
        when(estimateItemRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(itemPart));
        when(estimateItemRepository.findByEstimateIdOrderByLineNoAsc(1L)).thenReturn(List.of(itemService, itemPart));
        when(repairTaskRepository.findByEstimateItemId(10L)).thenReturn(Optional.empty());

        List<EstimateApproval.LineDecision> decisions = List.of(
                new EstimateApproval.LineDecision(10L, EstimateApproval.Decision.APPROVED, "Đồng ý thay"),
                new EstimateApproval.LineDecision(20L, EstimateApproval.Decision.APPROVED, "Đồng ý phụ tùng xịn")
        );

        estimateService.decide(1L, 1L, decisions, "REQ-DECIDE-001");

        // 1. Kiểm tra trạng thái dòng
        assertThat(itemService.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(itemPart.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(itemPart.getReservedQuantity()).isEqualTo(new BigDecimal("2.000"));

        // 2. Kiểm tra sinh task từ SERVICE
        verify(repairTaskRepository).save(argThat(task ->
                task.getEstimateItemId() == 10L &&
                task.getTaskType() == TaskType.REPAIR &&
                task.getServiceId() == 55L
        ));

        // 3. Kiểm tra gọi I05 giữ PART
        verify(inventoryOperations).reserve(1L, 20L, new BigDecimal("2.000"));

        // 4. Kiểm tra cập nhật trạng thái Estimate và RepairOrder
        assertThat(mockEstimate.getStatus()).isEqualTo(EstimateStatus.APPROVED);
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.APPROVED);
        verify(estimateRepository).save(mockEstimate);
        verify(repairOrderRepository).save(mockOrder);

        // 5. Kiểm tra audit
        verify(notificationAudit).recordCompletedRequest(eq(1L), eq("DECIDE_ESTIMATE"), eq("ESTIMATE"), eq(1L), any(), any(), any());
    }

    @Test
    @DisplayName("I04.decide: Phê duyệt một phần (PARTIALLY_APPROVED) và từ chối dòng không sinh task/giữ kho")
    void decide_PartialApproval_Success() {
        when(estimateRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockEstimate));
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(estimateItemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(itemService));
        when(estimateItemRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(itemPart));
        when(estimateItemRepository.findByEstimateIdOrderByLineNoAsc(1L)).thenReturn(List.of(itemService, itemPart));
        when(repairTaskRepository.findByEstimateItemId(10L)).thenReturn(Optional.empty());

        List<EstimateApproval.LineDecision> decisions = List.of(
                new EstimateApproval.LineDecision(10L, EstimateApproval.Decision.APPROVED, "Duyệt công thợ"),
                new EstimateApproval.LineDecision(20L, EstimateApproval.Decision.REJECTED, "Tự mang phụ tùng đến")
        );

        estimateService.decide(1L, 1L, decisions, "REQ-DECIDE-002");

        assertThat(itemService.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(itemPart.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        verify(repairTaskRepository).save(any(RepairTask.class));
        verify(inventoryOperations, never()).reserve(anyLong(), anyLong(), any());
        assertThat(mockEstimate.getStatus()).isEqualTo(EstimateStatus.PARTIALLY_APPROVED);
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.APPROVED);
    }

    @Test
    @DisplayName("I04.decide: Idempotency replay dòng đã duyệt không tạo trùng task")
    void decide_IdempotencyReplay_DoesNotDuplicateTask() {
        itemService.setApprovalStatus(ApprovalStatus.APPROVED);

        when(estimateRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockEstimate));
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(estimateItemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(itemService));
        when(estimateItemRepository.findByEstimateIdOrderByLineNoAsc(1L)).thenReturn(List.of(itemService));

        List<EstimateApproval.LineDecision> decisions = List.of(
                new EstimateApproval.LineDecision(10L, EstimateApproval.Decision.APPROVED, "Replay request")
        );

        estimateService.decide(1L, 1L, decisions, "REQ-KEY-REPLAY");

        verify(repairTaskRepository, never()).save(any(RepairTask.class));
    }

    @Test
    @DisplayName("I04.decide: Thất bại khi báo giá đã bị từ chối trước đó")
    void decide_AlreadyRejectedEstimate_ThrowsException() {
        mockEstimate.setStatus(EstimateStatus.REJECTED);
        when(estimateRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockEstimate));

        List<EstimateApproval.LineDecision> decisions = List.of(
                new EstimateApproval.LineDecision(10L, EstimateApproval.Decision.APPROVED, null)
        );

        assertThatThrownBy(() -> estimateService.decide(1L, 1L, decisions, "KEY"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thể phê duyệt");
    }

    @Test
    @DisplayName("I04.decide: Thất bại khi hạng mục không thuộc báo giá")
    void decide_ItemDoesNotBelongToEstimate_ThrowsException() {
        itemService.setEstimateId(999L);
        when(estimateRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockEstimate));
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(estimateItemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(itemService));

        List<EstimateApproval.LineDecision> decisions = List.of(
                new EstimateApproval.LineDecision(10L, EstimateApproval.Decision.APPROVED, null)
        );

        assertThatThrownBy(() -> estimateService.decide(1L, 1L, decisions, "KEY"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thuộc báo giá");
    }
}
