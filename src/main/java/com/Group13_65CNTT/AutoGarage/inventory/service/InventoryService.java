package com.Group13_65CNTT.AutoGarage.inventory.service;

import com.Group13_65CNTT.AutoGarage.inventory.dto.request.InventoryDocumentCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryDocumentResponse;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryTransactionResponse;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.StockReconciliationResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentStatus;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;
import com.Group13_65CNTT.AutoGarage.shared.contracts.InventoryOperations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService extends InventoryOperations {
    InventoryDocumentResponse createDocument(long actorUserId, InventoryDocumentCreateRequest request);

    InventoryDocumentResponse getDocumentById(Long id);

    InventoryDocumentResponse getDocumentByCode(String code);

    Page<InventoryDocumentResponse> searchDocuments(
            DocumentType type,
            DocumentStatus status,
            Long supplierId,
            Long orderId,
            String query,
            Pageable pageable);

    void cancelDocument(long actorUserId, Long documentId);

    Page<InventoryTransactionResponse> getTransactionsByPartId(Long partId, Pageable pageable);

    StockReconciliationResponse reconcilePartStock(Long partId);

    List<StockReconciliationResponse> reconcileAllParts();
}
