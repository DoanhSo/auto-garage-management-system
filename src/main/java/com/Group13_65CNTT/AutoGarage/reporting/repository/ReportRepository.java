package com.Group13_65CNTT.AutoGarage.reporting.repository;

import com.Group13_65CNTT.AutoGarage.reporting.entity.InvoiceBalance;
import com.Group13_65CNTT.AutoGarage.reporting.repository.projection.DashboardTotals;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ReportRepository extends JpaRepository<InvoiceBalance, Long> {
    @Query(
            value =
                    """
SELECT
  (SELECT COALESCE(SUM(total_amount),0) FROM invoices WHERE status<>'VOID' AND issued_at>=:from AND issued_at<:until) revenue,
  (SELECT COALESCE(SUM(p.amount),0) FROM payments p JOIN invoices i ON i.id=p.invoice_id WHERE p.direction='IN' AND i.status<>'VOID' AND p.paid_at>=:from AND p.paid_at<:until) cashIn,
  (SELECT COALESCE(SUM(p.amount),0) FROM payments p JOIN inventory_documents d ON d.id=p.inventory_document_id WHERE p.direction='OUT' AND d.document_type='IN' AND d.status='POSTED' AND p.paid_at>=:from AND p.paid_at<:until) cashOut,
  (SELECT COALESCE(SUM(balance_amount),0) FROM v_invoice_balances) customerDebt,
  (SELECT COALESCE(SUM(balance_amount),0) FROM v_supplier_balances) supplierDebt,
  (SELECT COALESCE(SUM(quantity_change*unit_cost),0) FROM inventory_transactions) stockValue,
  (SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND,GREATEST(starts_at,:from),LEAST(ends_at,:until)))/3600.0,0) FROM labor_sessions WHERE ends_at IS NOT NULL AND starts_at<:until AND ends_at>:from) laborHours
""",
            nativeQuery = true)
    DashboardTotals dashboard(@Param("from") Instant from, @Param("until") Instant until);

    @Query("SELECT b FROM InvoiceBalance b WHERE b.balanceAmount > 0 ORDER BY b.id")
    Page<InvoiceBalance> outstandingInvoices(Pageable pageable);
}
