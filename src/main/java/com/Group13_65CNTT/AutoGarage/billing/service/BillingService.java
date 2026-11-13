package com.Group13_65CNTT.AutoGarage.billing.service;

import com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceIssueRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.PaymentReceiveRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.SupplierPaymentRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceReadinessResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.PaymentResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.SupplierBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceStatus;
import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BillingService extends BillingOperations {
    InvoiceResponse issueInvoice(long actorUserId, InvoiceIssueRequest request);

    /** Read-only check of the rules {@link #issueInvoice} applies, reporting the first blocking reason. */
    InvoiceReadinessResponse getInvoiceReadiness(Long repairOrderId);

    /** Read-only view of the handover rules: debt, credit limit and the minimum still to collect. */
    com.Group13_65CNTT.AutoGarage.billing.dto.response.HandoverReadinessResponse getHandoverReadiness(
            Long repairOrderId);

    /** Sets the warranty policy of one invoice line before handover; the warranty book is created at handover. */
    InvoiceResponse updateInvoiceItemWarranty(
            long actorUserId,
            Long invoiceId,
            Long itemId,
            com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceItemWarrantyRequest request);

    InvoiceResponse getInvoiceById(Long id);

    InvoiceResponse getInvoiceByCode(String code);

    InvoiceResponse getInvoiceByOrderId(Long repairOrderId);

    Page<InvoiceResponse> searchInvoices(
            InvoiceStatus status,
            Long customerId,
            Long orderId,
            String query,
            Pageable pageable);

    void voidInvoice(long actorUserId, Long invoiceId, String voidReason);

    PaymentResponse receivePayment(long actorUserId, PaymentReceiveRequest request);

    PaymentResponse paySupplier(long actorUserId, SupplierPaymentRequest request);

    /** Next unused system transaction code for receipts (IN) or payouts (OUT) of today. */
    String suggestTransactionCode(PaymentDirection direction);

    Page<PaymentResponse> searchPayments(
            PaymentDirection direction,
            Long invoiceId,
            Long documentId,
            String query,
            Pageable pageable);

    List<PaymentResponse> getPaymentsForInvoice(Long invoiceId);

    List<PaymentResponse> getPaymentsForDocument(Long documentId);

    Page<InvoiceBalanceResponse> getCustomerDebtLedger(
            Long customerId,
            Boolean hasBalance,
            String query,
            Pageable pageable);

    Page<SupplierBalanceResponse> getSupplierDebtLedger(
            Boolean hasBalance,
            String query,
            Pageable pageable);
}
