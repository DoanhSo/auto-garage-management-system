package com.Group13_65CNTT.AutoGarage.inventory.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.common.config.PaginationProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.InventoryDocumentCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.InventoryDocumentItemRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryDocumentResponse;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.StockReconciliationResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.*;
import com.Group13_65CNTT.AutoGarage.inventory.mapper.InventoryDocumentMapper;
import com.Group13_65CNTT.AutoGarage.inventory.mapper.InventoryTransactionMapper;
import com.Group13_65CNTT.AutoGarage.inventory.repository.*;
import com.Group13_65CNTT.AutoGarage.inventory.service.impl.InventoryServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService unit tests")
class InventoryServiceTest {

    @Mock InventoryDocumentRepository documentRepository;
    @Mock InventoryDocumentItemRepository itemRepository;
    @Mock InventoryTransactionRepository transactionRepository;
    @Mock PartRepository partRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock RepairOrderPartRepository repairOrderPartRepository;
    @Mock InventoryDocumentMapper documentMapper;
    @Mock InventoryTransactionMapper transactionMapper;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock JdbcTemplate jdbcTemplate;
    @Spy Optional<NotificationAudit> notificationAudit = Optional.empty();
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @Spy PaginationPolicy pagination = new PaginationPolicy(new PaginationProperties());

    @InjectMocks InventoryServiceImpl service;

    @BeforeEach
    void setup() {
        lenient().when(currentUserProvider.userId()).thenReturn(100L);
    }

    @Test
    void createDocument_Success() {
        InventoryDocumentItemRequest itemReq = InventoryDocumentItemRequest.builder()
                .partId(1L).quantity(new BigDecimal("10")).unitCost(new BigDecimal("50000")).notes("Import note")
                .build();
        InventoryDocumentCreateRequest req = InventoryDocumentCreateRequest.builder()
                .documentType(DocumentType.IN).supplierId(1L).reason("Importing parts")
                .items(List.of(itemReq))
                .build();

        Part part = Part.builder().id(1L).sku("PART-1").partName("Filter").build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(Supplier.builder().id(1L).build()));
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        InventoryDocument savedDoc = InventoryDocument.builder()
                .id(10L).documentCode("DOC-001").documentType(DocumentType.IN).status(DocumentStatus.DRAFT)
                .items(new ArrayList<>()).build();

        when(documentRepository.save(any(InventoryDocument.class))).thenReturn(savedDoc);
        when(documentMapper.toResponse(savedDoc)).thenReturn(InventoryDocumentResponse.builder().id(10L).documentCode("DOC-001").build());

        InventoryDocumentResponse resp = service.createDocument(100L, req);

        assertThat(resp.getDocumentCode()).isEqualTo("DOC-001");
        verify(documentRepository).save(any(InventoryDocument.class));
    }

    @Test
    void postDocument_IncomingIncreasesStockAndCreatesTransactions() {
        Part part = Part.builder().id(1L).sku("PART-1").stockQuantity(new BigDecimal("5")).reservedQuantity(BigDecimal.ZERO).purchasePrice(BigDecimal.ZERO).build();
        InventoryDocument doc = InventoryDocument.builder()
                .id(10L).documentCode("DOC-001").documentType(DocumentType.IN).status(DocumentStatus.DRAFT).version(0L)
                .items(new ArrayList<>())
                .build();
        InventoryDocumentItem item = InventoryDocumentItem.builder()
                .id(101L).document(doc).part(part).quantity(new BigDecimal("10")).unitCost(new BigDecimal("50000")).lineTotal(new BigDecimal("500000"))
                .build();
        doc.getItems().add(item);

        when(documentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(doc));
        when(itemRepository.findByDocumentIdOrderByLineNoAsc(10L)).thenReturn(List.of(item));
        when(partRepository.findAllByIdInOrderByIdAscWithLock(List.of(1L))).thenReturn(List.of(part));
        when(documentRepository.save(doc)).thenReturn(doc);

        service.post(100L, 10L, "REQ-01");

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);
        assertThat(part.getStockQuantity()).isEqualByComparingTo("15");
        verify(transactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void postDocument_OutgoingWithInsufficientStock_ThrowsBadRequest() {
        Part part = Part.builder().id(1L).sku("PART-1").stockQuantity(new BigDecimal("5")).reservedQuantity(BigDecimal.ZERO).purchasePrice(BigDecimal.ZERO).build();
        InventoryDocument doc = InventoryDocument.builder()
                .id(10L).repairOrderId(100L).documentCode("DOC-OUT-001").documentType(DocumentType.OUT).status(DocumentStatus.DRAFT).version(0L)
                .items(new ArrayList<>())
                .build();
        InventoryDocumentItem item = InventoryDocumentItem.builder()
                .id(101L).estimateItemId(12L).document(doc).part(part).quantity(new BigDecimal("10")).unitCost(new BigDecimal("50000")).lineTotal(new BigDecimal("500000"))
                .build();
        doc.getItems().add(item);

        when(jdbcTemplate.queryForList(contains("SELECT status FROM repair_orders"), eq(100L))).thenReturn(List.of(Map.of("status", "IN_PROGRESS")));
        when(jdbcTemplate.queryForList(contains("SELECT ei.part_id"), eq(12L))).thenReturn(List.of(Map.of("part_id", 1L, "repair_order_id", 100L, "approval_status", "APPROVED", "item_type", "PART", "reserved_quantity", new BigDecimal("10"), "unit_price", new BigDecimal("60000"))));
        when(documentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(doc));
        when(itemRepository.findByDocumentIdOrderByLineNoAsc(10L)).thenReturn(List.of(item));
        when(partRepository.findAllByIdInOrderByIdAscWithLock(List.of(1L))).thenReturn(List.of(part));

        assertThatThrownBy(() -> service.post(100L, 10L, "REQ-02"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.BAD_REQUEST);
    }

    @Test
    void reserveForOrder_AcquiresLocksInAscendingOrderPreventingDeadlocks() {
        Part p1 = Part.builder().id(1L).stockQuantity(new BigDecimal("10")).reservedQuantity(new BigDecimal("1")).build();

        Map<String, Object> estimateItem = new HashMap<>();
        estimateItem.put("id", 1L);
        estimateItem.put("part_id", 1L);
        estimateItem.put("approval_status", "APPROVED");
        estimateItem.put("item_type", "PART");
        estimateItem.put("reserved_quantity", BigDecimal.ZERO);
        estimateItem.put("quantity", new BigDecimal("2"));

        when(jdbcTemplate.queryForList(contains("SELECT id, part_id"), eq(1L)))
                .thenReturn(List.of(estimateItem));
        when(partRepository.findByIdWithLock(1L)).thenReturn(Optional.of(p1));

        service.reserve(100L, 1L, new BigDecimal("2"));

        assertThat(p1.getReservedQuantity()).isEqualByComparingTo("3");
        verify(partRepository).findByIdWithLock(1L);
        verify(partRepository).save(p1);
    }

    @Test
    void releaseForOrder_ReleasesReservedStock() {
        Part p1 = Part.builder().id(1L).stockQuantity(new BigDecimal("10")).reservedQuantity(new BigDecimal("5")).build();

        Map<String, Object> estimateItem = new HashMap<>();
        estimateItem.put("id", 1L);
        estimateItem.put("part_id", 1L);
        estimateItem.put("reserved_quantity", new BigDecimal("2"));

        when(jdbcTemplate.queryForList(contains("FROM estimate_items ei"), eq(1001L)))
                .thenReturn(List.of(estimateItem));
        when(partRepository.findAllByIdInOrderByIdAscWithLock(List.of(1L))).thenReturn(List.of(p1));

        service.releaseForOrder(100L, 1001L);

        assertThat(p1.getReservedQuantity()).isEqualByComparingTo("3");
        verify(partRepository).saveAll(any());
    }

    @Test
    void reconcilePartStock_CalculatesDiscrepancy() {
        Part part = Part.builder().id(1L).sku("P-1").partName("Brake pad").stockQuantity(new BigDecimal("10")).reservedQuantity(BigDecimal.ZERO).build();
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));
        when(transactionRepository.calculateLedgerQuantityForPart(1L)).thenReturn(new BigDecimal("12"));

        StockReconciliationResponse resp = service.reconcilePartStock(1L);

        assertThat(resp.getStockQuantity()).isEqualByComparingTo("10");
        assertThat(resp.getLedgerQuantity()).isEqualByComparingTo("12");
        assertThat(resp.getDiscrepancy()).isEqualByComparingTo("-2");
        assertThat(resp.isMatched()).isFalse();
    }
}
