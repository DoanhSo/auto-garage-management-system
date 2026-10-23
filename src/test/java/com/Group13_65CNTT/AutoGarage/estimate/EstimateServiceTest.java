package com.Group13_65CNTT.AutoGarage.estimate;

import com.Group13_65CNTT.AutoGarage.catalog.entity.Part;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.catalog.repository.PartRepository;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateItemCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateItemResponse;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateResponse;
import com.Group13_65CNTT.AutoGarage.estimate.entity.*;
import com.Group13_65CNTT.AutoGarage.estimate.mapper.EstimateItemMapper;
import com.Group13_65CNTT.AutoGarage.estimate.mapper.EstimateMapper;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateItemRepository;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateRepository;
import com.Group13_65CNTT.AutoGarage.estimate.service.impl.EstimateServiceImpl;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstimateServiceTest {

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
    private NotificationAudit notificationAudit;

    @InjectMocks
    private EstimateServiceImpl estimateService;

    private RepairOrder mockOrder;
    private ServiceEntity mockService;
    private Part mockPart;
    private Estimate mockEstimate;

    @BeforeEach
    void setUp() {
        mockOrder = RepairOrder.builder()
                .id(100L)
                .orderCode("RO-20261023-00001")
                .status(RepairOrderStatus.DIAGNOSING)
                .build();

        mockService = new ServiceEntity();
        mockService.setId(10L);
        mockService.setServiceName("Bảo dưỡng định kỳ");
        mockService.setUnit("Gói");
        mockService.setUnitPrice(new BigDecimal("300000.00"));
        mockService.setWarrantyDays(30);
        mockService.setWarrantyKm(1000);

        mockPart = Part.builder()
                .id(20L)
                .partName("Dầu nhớt Castrol 5W30")
                .unit("Lít")
                .sellingPrice(new BigDecimal("150000.00"))
                .purchasePrice(new BigDecimal("100000.00"))
                .warrantyDays(60)
                .warrantyKm(5000)
                .build();

        mockEstimate = Estimate.builder()
                .id(1L)
                .estimateCode("EST-20261023-00001")
                .repairOrderId(100L)
                .versionNo(1)
                .estimateKind(EstimateKind.INITIAL)
                .status(EstimateStatus.DRAFT)
                .subtotal(new BigDecimal("900000.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("900000.00"))
                .build();

        ReflectionTestUtils.setField(estimateService, "notificationAudit", notificationAudit);
    }

    @Test
    @DisplayName("Lập báo giá ban đầu (INITIAL) thành công, tính toán giá trị và cập nhật trạng thái phiếu")
    void createEstimate_Initial_Success() {
        EstimateItemCreateRequest svcItem = EstimateItemCreateRequest.builder()
                .itemType(ItemType.SERVICE)
                .serviceId(10L)
                .quantity(new BigDecimal("1.000"))
                .build();

        EstimateItemCreateRequest partItem = EstimateItemCreateRequest.builder()
                .itemType(ItemType.PART)
                .partId(20L)
                .quantity(new BigDecimal("4.000"))
                .build();

        EstimateCreateRequest request = EstimateCreateRequest.builder()
                .repairOrderId(100L)
                .estimateKind(EstimateKind.INITIAL)
                .items(List.of(svcItem, partItem))
                .build();

        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(estimateRepository.findTopByRepairOrderIdOrderByVersionNoDesc(100L)).thenReturn(Optional.empty());
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(mockService));
        when(partRepository.findById(20L)).thenReturn(Optional.of(mockPart));

        EstimateItem item1 = EstimateItem.builder().itemType(ItemType.SERVICE).quantity(new BigDecimal("1.000")).unitPrice(new BigDecimal("300000.00")).build();
        EstimateItem item2 = EstimateItem.builder().itemType(ItemType.PART).quantity(new BigDecimal("4.000")).unitPrice(new BigDecimal("150000.00")).build();
        when(estimateItemMapper.toEntity(svcItem)).thenReturn(item1);
        when(estimateItemMapper.toEntity(partItem)).thenReturn(item2);

        when(estimateRepository.save(any(Estimate.class))).thenReturn(mockEstimate);
        when(estimateItemRepository.saveAll(anyList())).thenReturn(List.of(item1, item2));

        EstimateResponse responseDto = EstimateResponse.builder()
                .id(1L)
                .versionNo(1)
                .status(EstimateStatus.DRAFT)
                .totalAmount(new BigDecimal("900000.00"))
                .build();
        when(estimateMapper.toResponseWithItems(eq(mockEstimate), anyList())).thenReturn(responseDto);

        EstimateResponse response = estimateService.createEstimate(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getVersionNo()).isEqualTo(1);
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.WAITING_APPROVAL);
        verify(repairOrderRepository).save(mockOrder);
    }

    @Test
    @DisplayName("Lập báo giá phát sinh (SUPPLEMENTAL) thành công và tăng version_no")
    void createEstimate_Supplemental_Success() {
        Estimate parent = Estimate.builder().id(1L).repairOrderId(100L).versionNo(1).build();

        EstimateItemCreateRequest svcItem = EstimateItemCreateRequest.builder()
                .itemType(ItemType.SERVICE)
                .serviceId(10L)
                .quantity(new BigDecimal("1.000"))
                .build();

        EstimateCreateRequest request = EstimateCreateRequest.builder()
                .repairOrderId(100L)
                .estimateKind(EstimateKind.SUPPLEMENTAL)
                .parentEstimateId(1L)
                .items(List.of(svcItem))
                .build();

        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(estimateRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(estimateRepository.findTopByRepairOrderIdOrderByVersionNoDesc(100L)).thenReturn(Optional.of(parent));
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(mockService));

        EstimateItem item1 = EstimateItem.builder().itemType(ItemType.SERVICE).quantity(new BigDecimal("1.000")).unitPrice(new BigDecimal("300000.00")).build();
        when(estimateItemMapper.toEntity(svcItem)).thenReturn(item1);

        Estimate suppEstimate = Estimate.builder().id(2L).versionNo(2).build();
        when(estimateRepository.save(any(Estimate.class))).thenReturn(suppEstimate);
        when(estimateItemRepository.saveAll(anyList())).thenReturn(List.of(item1));

        EstimateResponse responseDto = EstimateResponse.builder().id(2L).versionNo(2).build();
        when(estimateMapper.toResponseWithItems(eq(suppEstimate), anyList())).thenReturn(responseDto);

        EstimateResponse response = estimateService.createEstimate(1L, request);

        assertThat(response.getVersionNo()).isEqualTo(2);
    }

    @Test
    @DisplayName("Lập báo giá phát sinh thiếu parentEstimateId bị từ chối")
    void createEstimate_SupplementalWithoutParent_ThrowsException() {
        EstimateCreateRequest request = EstimateCreateRequest.builder()
                .repairOrderId(100L)
                .estimateKind(EstimateKind.SUPPLEMENTAL)
                .parentEstimateId(null)
                .items(List.of())
                .build();

        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> estimateService.createEstimate(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bắt buộc phải có ID báo giá gốc");
    }

    @Test
    @DisplayName("Gửi báo giá cho khách hàng thành công chuyển status sang SENT")
    void sendEstimate_Success() {
        when(estimateRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockEstimate));
        when(estimateRepository.save(mockEstimate)).thenReturn(mockEstimate);
        when(estimateRepository.findById(1L)).thenReturn(Optional.of(mockEstimate));
        when(estimateItemRepository.findByEstimateIdOrderByLineNoAsc(1L)).thenReturn(List.of());

        EstimateResponse res = EstimateResponse.builder().id(1L).status(EstimateStatus.SENT).build();
        when(estimateMapper.toResponseWithItems(eq(mockEstimate), anyList())).thenReturn(res);

        EstimateResponse result = estimateService.sendEstimate(1L, 1L);

        assertThat(mockEstimate.getStatus()).isEqualTo(EstimateStatus.SENT);
        assertThat(mockEstimate.getSentAt()).isNotNull();
        verify(notificationAudit).enqueue(any());
    }

    @Test
    @DisplayName("Gửi báo giá thất bại nếu không phải trạng thái DRAFT")
    void sendEstimate_NotDraft_ThrowsException() {
        mockEstimate.setStatus(EstimateStatus.SENT);
        when(estimateRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockEstimate));

        assertThatThrownBy(() -> estimateService.sendEstimate(1L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Chỉ báo giá ở trạng thái DRAFT mới có thể gửi");
    }
}
