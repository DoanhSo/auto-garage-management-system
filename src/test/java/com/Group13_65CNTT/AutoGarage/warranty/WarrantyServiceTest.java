package com.Group13_65CNTT.AutoGarage.warranty;

import com.Group13_65CNTT.AutoGarage.billing.entity.Invoice;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceItem;
import com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceItemRepository;
import com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceRepository;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.contracts.RepairWorkflow;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimDecideRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimOpenRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyClaimResponse;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyResponse;
import com.Group13_65CNTT.AutoGarage.warranty.entity.ClaimStatus;
import com.Group13_65CNTT.AutoGarage.warranty.entity.Warranty;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyClaim;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyStatus;
import com.Group13_65CNTT.AutoGarage.warranty.mapper.WarrantyClaimMapper;
import com.Group13_65CNTT.AutoGarage.warranty.mapper.WarrantyMapper;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyClaimRepository;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyRepository;
import com.Group13_65CNTT.AutoGarage.warranty.service.impl.WarrantyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class WarrantyServiceTest {

    @Mock
    private WarrantyRepository warrantyRepository;
    @Mock
    private WarrantyClaimRepository warrantyClaimRepository;
    @Mock
    private WarrantyMapper warrantyMapper;
    @Mock
    private WarrantyClaimMapper warrantyClaimMapper;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private RepairWorkflow repairWorkflow;
    @Mock
    private PaginationPolicy pagination;
    @Mock
    private NotificationAudit notificationAudit;

    @Mock
    private com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;
    @Mock
    private com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService objectAccess;
    @Mock
    private com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider currentUser;

    @InjectMocks
    private WarrantyServiceImpl warrantyService;

    private Invoice mockInvoice;
    private RepairOrder mockOrder;
    private Warranty mockWarranty;
    private WarrantyClaim mockClaim;

    @BeforeEach
    void setUp() {
        mockOrder = RepairOrder.builder()
                .id(10L)
                .orderCode("RO-001")
                .vehicleId(200L)
                .customerId(300L)
                .build();

        mockInvoice = Invoice.builder()
                .id(1L)
                .invoiceCode("INV-001")
                .repairOrderId(10L)
                .build();

        mockWarranty = Warranty.builder()
                .id(100L)
                .warrantyCode("WAR-20261023-11111")
                .invoiceItemId(50L)
                .vehicleId(200L)
                .customerId(300L)
                .status(WarrantyStatus.ACTIVE)
                .startsAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .expiresAt(Instant.now().plus(80, ChronoUnit.DAYS))
                .startOdometer(10000)
                .endOdometer(20000)
                .warrantyDays(90)
                .warrantyKm(10000)
                .build();

        mockClaim = WarrantyClaim.builder()
                .id(500L)
                .claimCode("CLM-20261023-22222")
                .warrantyId(100L)
                .status(ClaimStatus.OPEN)
                .currentOdometer(15000)
                .description("Phanh kêu rít bất thường")
                .build();
    }

    @Test
    @DisplayName("createFromInvoice: Tạo bảo hành thành công từ invoice items có chính sách bảo hành")
    void createFromInvoice_Success() {
        InvoiceItem item1 = InvoiceItem.builder()
                .id(51L)
                .invoice(mockInvoice)
                .itemName("Thay má phanh trước")
                .warrantyDays(90)
                .warrantyKm(5000)
                .warrantyTerms("Bảo hành phanh chính hãng")
                .build();

        InvoiceItem item2 = InvoiceItem.builder()
                .id(52L)
                .invoice(mockInvoice)
                .itemName("Rửa xe")
                .warrantyDays(0)
                .warrantyKm(0)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));
        when(repairOrderRepository.findById(10L)).thenReturn(Optional.of(mockOrder));
        when(invoiceItemRepository.findByInvoiceId(1L)).thenReturn(List.of(item1, item2));
        when(warrantyRepository.findByInvoiceItemId(51L)).thenReturn(Optional.empty());

        warrantyService.createFromInvoice(1L, 1L, 25000);

        verify(warrantyRepository, times(1)).save(any(Warranty.class));
    }

    @Test
    @DisplayName("openClaim: Tạo yêu cầu bảo hành thành công khi còn hạn ngày và số km")
    void openClaim_Success() {
        when(warrantyRepository.findById(100L)).thenReturn(Optional.of(mockWarranty));
        when(warrantyClaimRepository.save(any(WarrantyClaim.class))).thenAnswer(invocation -> {
            WarrantyClaim c = invocation.getArgument(0);
            c.setId(501L);
            return c;
        });

        long claimId = warrantyService.openClaim(2L, 100L, 15000, "Đĩa phanh rung giật", "KEY-001");

        assertThat(claimId).isEqualTo(501L);
        verify(warrantyClaimRepository, times(1)).save(any(WarrantyClaim.class));
    }

    @Test
    @DisplayName("openClaim: Báo lỗi khi bảo hành đã hết hạn về thời gian")
    void openClaim_FailsWhenExpiredByTime() {
        mockWarranty.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(warrantyRepository.findById(100L)).thenReturn(Optional.of(mockWarranty));

        assertThatThrownBy(() -> warrantyService.openClaim(2L, 100L, 15000, "Hỏng hóc", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hết hạn về thời gian");

        assertThat(mockWarranty.getStatus()).isEqualTo(WarrantyStatus.EXPIRED);
        verify(warrantyRepository).save(mockWarranty);
    }

    @Test
    @DisplayName("openClaim: Báo lỗi khi số km vượt quá hạn mức bảo hành")
    void openClaim_FailsWhenExpiredByOdometer() {
        when(warrantyRepository.findById(100L)).thenReturn(Optional.of(mockWarranty));

        assertThatThrownBy(() -> warrantyService.openClaim(2L, 100L, 20000, "Hỏng hóc", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hết hạn về số km");
    }

    @Test
    @DisplayName("openClaim: Báo lỗi khi bảo hành không ở trạng thái ACTIVE")
    void openClaim_FailsWhenStatusNotActive() {
        mockWarranty.setStatus(WarrantyStatus.VOID);
        when(warrantyRepository.findById(100L)).thenReturn(Optional.of(mockWarranty));

        assertThatThrownBy(() -> warrantyService.openClaim(2L, 100L, 15000, "Hỏng hóc", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không ở trạng thái ACTIVE");
    }

    @Test
    @DisplayName("decideClaim: Phê duyệt claim thành công -> chuyển APPROVED và gọi I03 createWarrantyOrder")
    void decideClaim_Approved_Success() {
        when(warrantyClaimRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(mockClaim));
        when(repairWorkflow.createWarrantyOrder(1L, 500L)).thenReturn(999L);

        warrantyService.decideClaim(1L, 500L, true, "Lỗi từ linh kiện, chấp thuận bảo hành", "KEY-DECIDE-1");

        assertThat(mockClaim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(mockClaim.getDecisionReason()).contains("Lỗi từ linh kiện");
        verify(repairWorkflow).createWarrantyOrder(1L, 500L);
        verify(warrantyClaimRepository).save(mockClaim);
    }

    @Test
    @DisplayName("decideClaim: Từ chối claim thành công -> chuyển REJECTED và đóng claim")
    void decideClaim_Rejected_Success() {
        when(warrantyClaimRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(mockClaim));

        warrantyService.decideClaim(1L, 500L, false, "Do người dùng tự can thiệp sửa chữa ngoài", null);

        assertThat(mockClaim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(mockClaim.getClosedAt()).isNotNull();
        verify(repairWorkflow, never()).createWarrantyOrder(anyLong(), anyLong());
        verify(warrantyClaimRepository).save(mockClaim);
    }

    @Test
    @DisplayName("decideClaim: Báo lỗi khi claim không ở trạng thái OPEN")
    void decideClaim_FailsWhenNotOpen() {
        mockClaim.setStatus(ClaimStatus.APPROVED);
        when(warrantyClaimRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(mockClaim));

        assertThatThrownBy(() -> warrantyService.decideClaim(1L, 500L, true, "Đồng ý", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không ở trạng thái chờ duyệt (OPEN)");
    }

    @Test
    @DisplayName("getWarrantyById: Ném ResourceNotFoundException nếu không tìm thấy")
    void getWarrantyById_NotFound() {
        when(warrantyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warrantyService.getWarrantyById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getClaimById: Ném ResourceNotFoundException nếu không tìm thấy")
    void getClaimById_NotFound() {
        when(warrantyClaimRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warrantyService.getClaimById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
