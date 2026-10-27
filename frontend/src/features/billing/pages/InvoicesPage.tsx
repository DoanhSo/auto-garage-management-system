import { useMemo, useRef, useState } from 'react';
import {
  AlertCircle,
  Ban,
  CheckCircle2,
  Clock,
  CreditCard,
  Eye,
  Plus,
  Receipt,
  RotateCcw,
  Search,
  SlidersHorizontal,
  X,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import {
  Badge,
  Button,
  Field,
  Loading,
  Modal,
  PageHeader,
} from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { errorMessage } from '../../../shared/api/errors';
import {
  useInvoicesQuery,
  useIssueInvoiceMutation,
  useVoidInvoiceMutation,
  useReceivePaymentMutation,
} from '../hooks/useBilling';
import type {
  Invoice,
  InvoiceIssueInput,
  PaymentReceiveInput,
  InvoiceStatus,
  PaymentMethod,
} from '../types/billing';

const sortOptions = [
  ['id:desc', 'Mã hóa đơn · Mới nhất'],
  ['id:asc', 'Mã hóa đơn · Cũ nhất'],
  ['totalAmount:desc', 'Tổng tiền · Cao nhất'],
  ['totalAmount:asc', 'Tổng tiền · Thấp nhất'],
  ['balanceAmount:desc', 'Còn nợ · Nhiều nhất'],
];

export function InvoicesPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();

  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query);
  const [selectedStatus, setSelectedStatus] = useState<InvoiceStatus | ''>('');
  const [sortField, setSortField] = useState('id');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(0);

  const [viewingInvoice, setViewingInvoice] = useState<Invoice | null>(null);
  const [issuing, setIssuing] = useState(false);
  const [voidingInvoice, setVoidingInvoice] = useState<Invoice | null>(null);
  const [voidReason, setVoidReason] = useState('');
  const [payingInvoice, setPayingInvoice] = useState<Invoice | null>(null);

  // Form states cho thu tiền
  const [payAmount, setPayAmount] = useState<number>(0);
  const [payMethod, setPayMethod] = useState<PaymentMethod>('VIETQR');
  const [payReference, setPayReference] = useState('');
  const [payNotes, setPayNotes] = useState('');

  const createTrigger = useRef<HTMLButtonElement>(null);

  const { data, isLoading, isFetching } = useInvoicesQuery({
    status: selectedStatus || undefined,
    query: debouncedQuery.trim() || undefined,
    page,
    size: 20,
    sortBy: sortField,
    sortDir,
  });

  const issueMutation = useIssueInvoiceMutation();
  const voidMutation = useVoidInvoiceMutation();
  const receivePaymentMutation = useReceivePaymentMutation();

  const filterCount = (query.trim() ? 1 : 0) + (selectedStatus !== '' ? 1 : 0);
  const isFiltered = query.length > 0 || selectedStatus !== '' || sortField !== 'id' || sortDir !== 'desc';

  const handleResetFilters = () => {
    setQuery('');
    setSelectedStatus('');
    setSortField('id');
    setSortDir('desc');
    setPage(0);
  };

  const handleIssue = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: InvoiceIssueInput = {
      repairOrderId: Number(formData.get('repairOrderId')),
      taxAmount: Number(formData.get('taxAmount') || 0),
      discountAmount: Number(formData.get('discountAmount') || 0),
    };

    try {
      await issueMutation.mutateAsync(input);
      notify('Đã phát hành hóa đơn dịch vụ thành công.');
      setIssuing(false);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Lỗi khi phát hành hóa đơn.');
    }
  };

  const handleVoid = async () => {
    if (!voidingInvoice || !voidReason.trim()) {
      notify('Vui lòng nhập lý do hủy hóa đơn.');
      return;
    }
    try {
      await voidMutation.mutateAsync({
        id: voidingInvoice.id,
        data: { voidReason: voidReason.trim() },
      });
      notify(`Đã hủy hóa đơn ${voidingInvoice.invoiceCode}.`);
      setVoidingInvoice(null);
      setVoidReason('');
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Không thể hủy hóa đơn.');
    }
  };

  const handleOpenPayment = (inv: Invoice) => {
    setPayingInvoice(inv);
    setPayAmount(Number(inv.balanceAmount || inv.totalAmount || 0));
    setPayMethod('VIETQR');
    setPayReference('');
    setPayNotes('');
  };

  const handleReceivePayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!payingInvoice || payAmount <= 0) {
      notify('Vui lòng nhập số tiền thanh toán hợp lệ lớn hơn 0.');
      return;
    }

    const input: PaymentReceiveInput = {
      invoiceId: payingInvoice.id,
      amount: payAmount,
      paymentMethod: payMethod,
      providerReference: payReference || undefined,
      notes: payNotes || undefined,
    };

    try {
      await receivePaymentMutation.mutateAsync(input);
      notify(`Đã ghi nhận thanh toán ${payAmount.toLocaleString('vi-VN')} đ cho hóa đơn ${payingInvoice.invoiceCode}.`);
      setPayingInvoice(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Ghi nhận thanh toán thất bại.');
    }
  };

  const renderStatusBadge = (status: InvoiceStatus) => {
    switch (status) {
      case 'PAID':
        return (
          <Badge variant="success" className="inline-flex items-center gap-1">
            <CheckCircle2 size={12} /> Đã thu đủ
          </Badge>
        );
      case 'PARTIALLY_PAID':
        return (
          <Badge variant="warning" className="inline-flex items-center gap-1">
            <Clock size={12} /> Thu 1 phần
          </Badge>
        );
      case 'UNPAID':
        return (
          <Badge variant="danger" className="inline-flex items-center gap-1">
            <AlertCircle size={12} /> Chưa thanh toán
          </Badge>
        );
      case 'VOID':
        return <Badge variant="secondary">Đã hủy bỏ</Badge>;
    }
  };

  const columns = useMemo<ColumnDef<Invoice>[]>(
    () => [
      {
        id: 'invoice',
        header: 'Hóa đơn',
        cell: ({ row }) => (
          <div className="account-cell">
            <span
              className="avatar"
              style={{
                background: '#edf3ff',
                color: '#245bdb',
                fontWeight: 700,
              }}
            >
              <Receipt size={16} />
            </span>
            <div>
              <strong style={{ color: '#172338', fontFamily: 'monospace' }}>
                {row.original.invoiceCode}
              </strong>
              <div className="muted" style={{ fontSize: '12px' }}>
                Phiếu sửa chữa: #{row.original.repairOrderId}
              </div>
            </div>
          </div>
        ),
      },
      {
        id: 'customerAndVehicle',
        header: 'Khách hàng & Xe',
        cell: ({ row }) => {
          const cust = row.original.customerSnapshot;
          const veh = row.original.vehicleSnapshot;
          return (
            <div style={{ fontSize: '13px' }}>
              <div style={{ fontWeight: 650, color: '#172338' }}>
                {cust?.fullName || 'Khách vãng lai'}
              </div>
              <div className="muted" style={{ fontSize: '12px' }}>
                {veh?.plateNumber ? (
                  <span style={{ fontWeight: 600, color: '#245bdb' }}>{veh.plateNumber}</span>
                ) : null}
                {veh?.make || veh?.model ? ` (${veh.make || ''} ${veh.model || ''})` : ''}
              </div>
            </div>
          );
        },
      },
      {
        id: 'amounts',
        header: 'Số tiền & Công nợ',
        cell: ({ row }) => {
          const inv = row.original;
          const balance = Number(inv.balanceAmount ?? inv.totalAmount ?? 0);
          return (
            <div style={{ fontSize: '13px' }}>
              <div style={{ fontWeight: 700, color: '#172338' }}>
                {Number(inv.totalAmount).toLocaleString('vi-VN')} đ
              </div>
              <div style={{ fontSize: '11px', marginTop: '2px' }}>
                {balance > 0 ? (
                  <span style={{ color: '#b42335', fontWeight: 600 }}>
                    Còn nợ: {balance.toLocaleString('vi-VN')} đ
                  </span>
                ) : (
                  <span style={{ color: '#059669', fontWeight: 550 }}>Đã thu đủ 100%</span>
                )}
              </div>
            </div>
          );
        },
      },
      {
        accessorKey: 'status',
        header: 'Trạng thái',
        cell: ({ row }) => renderStatusBadge(row.original.status),
      },
      {
        accessorKey: 'issuedAt',
        header: 'Ngày phát hành',
        cell: ({ row }) => (
          <span className="muted" style={{ fontSize: '12px' }}>
            {new Date(row.original.issuedAt).toLocaleDateString('vi-VN', {
              day: '2-digit',
              month: '2-digit',
              year: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            })}
          </span>
        ),
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => {
          const inv = row.original;
          const canPay = inv.status === 'UNPAID' || inv.status === 'PARTIALLY_PAID';
          const canVoid = inv.status === 'UNPAID';

          return (
            <div className="row-actions">
              <Button
                variant="ghost"
                aria-label={`Xem chi tiết ${inv.invoiceCode}`}
                onClick={() => setViewingInvoice(inv)}
              >
                <Eye size={16} />
                <span>Xem</span>
              </Button>
              {canPay && (
                <Button
                  variant="ghost"
                  aria-label={`Thu tiền hóa đơn ${inv.invoiceCode}`}
                  onClick={() => handleOpenPayment(inv)}
                  style={{ color: '#059669' }}
                >
                  <CreditCard size={16} />
                  <span>Thu tiền</span>
                </Button>
              )}
              {canVoid && (
                <Button
                  variant="ghost"
                  className="delete-action"
                  aria-label={`Hủy hóa đơn ${inv.invoiceCode}`}
                  onClick={() => {
                    setVoidingInvoice(inv);
                    setVoidReason('');
                  }}
                >
                  <Ban size={16} />
                  <span>Hủy</span>
                </Button>
              )}
            </div>
          );
        },
      },
    ],
    [],
  );

  const invoicesList = data?.content || [];
  const totalElements = data?.totalElements || 0;
  const totalPages = data?.totalPages || 1;

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ TÀI CHÍNH & THANH TOÁN"
        title="Hóa đơn dịch vụ"
        description="Xuất hóa đơn dịch vụ sửa chữa, theo dõi thanh toán và quản lý công nợ khách hàng."
        actions={
          <Button
            variant="primary"
            ref={createTrigger}
            onClick={() => setIssuing(true)}
          >
            <Plus size={18} />
            <span>Phát hành hóa đơn</span>
          </Button>
        }
      />

      <div className="info-strip">
        <Receipt size={18} />
        <span>
          Hóa đơn dịch vụ lưu snapshot bất biến thông tin khách hàng, thông số xe và đơn giá phụ tùng/tiền công tại thời điểm xuất hóa đơn. Mỗi phiếu sửa chữa chỉ phát hành tối đa 1 hóa đơn.
        </span>
      </div>

      <section className="panel table-panel">
        {/* Bộ lọc hóa đơn */}
        <div className="user-filters" role="search" aria-label="Bộ lọc hóa đơn">
          <div className="user-filters-heading">
            <div>
              <SlidersHorizontal size={16} aria-hidden />
              <h2>Bộ lọc hóa đơn dịch vụ</h2>
              {filterCount > 0 && <span className="filter-count">{filterCount} đang áp dụng</span>}
            </div>
            <Button
              variant="ghost"
              className="filter-reset"
              disabled={!isFiltered}
              onClick={handleResetFilters}
            >
              <RotateCcw size={14} />
              Đặt lại
            </Button>
          </div>

          <div
            className="user-filter-grid"
            style={{
              gridTemplateColumns: 'minmax(220px, 1.3fr) minmax(320px, 1.7fr) minmax(200px, 1fr)',
            }}
          >
            {/* Ô tìm kiếm */}
            <div className="user-filter-field">
              <label htmlFor="inv-search-input">Tìm kiếm</label>
              <div className="user-search">
                <Search size={17} aria-hidden />
                <input
                  id="inv-search-input"
                  className="input"
                  placeholder="Nhập mã hóa đơn, tên khách, biển số xe..."
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    setPage(0);
                  }}
                />
                {query && (
                  <button
                    type="button"
                    className="search-clear"
                    aria-label="Xóa từ khóa tìm kiếm"
                    onClick={() => {
                      setQuery('');
                      setPage(0);
                    }}
                  >
                    <X size={15} />
                  </button>
                )}
              </div>
            </div>

            {/* Trạng thái Segmented */}
            <fieldset className="user-filter-field status-filter">
              <legend>Trạng thái thanh toán</legend>
              <div className="status-segments">
                {[
                  { value: '', label: 'Tất cả' },
                  { value: 'UNPAID', label: 'Chưa thu' },
                  { value: 'PARTIALLY_PAID', label: '1 phần' },
                  { value: 'PAID', label: 'Đã thu đủ' },
                ].map((opt) => (
                  <button
                    key={opt.label}
                    type="button"
                    className={selectedStatus === opt.value ? 'selected' : ''}
                    onClick={() => {
                      setSelectedStatus(opt.value as InvoiceStatus | '');
                      setPage(0);
                    }}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </fieldset>

            {/* Sắp xếp */}
            <div className="user-filter-field">
              <label htmlFor="inv-sort-select">Sắp xếp theo</label>
              <select
                id="inv-sort-select"
                className="input"
                value={`${sortField}:${sortDir}`}
                onChange={(e) => {
                  const [f, d] = e.target.value.split(':');
                  setSortField(f);
                  setSortDir(d);
                  setPage(0);
                }}
              >
                {sortOptions.map(([val, label]) => (
                  <option key={val} value={val}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Nội dung bảng */}
        {isLoading ? (
          <Loading label="Đang tải danh sách hóa đơn…" />
        ) : compact ? (
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {invoicesList.map((inv) => (
              <div
                key={inv.id}
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  padding: '14px',
                  background: 'white',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <strong style={{ fontSize: '15px', fontFamily: 'monospace' }}>{inv.invoiceCode}</strong>
                    <div className="muted" style={{ fontSize: '12px' }}>{inv.customerSnapshot?.fullName}</div>
                  </div>
                  {renderStatusBadge(inv.status)}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                  <span>Tổng tiền: <strong>{Number(inv.totalAmount).toLocaleString('vi-VN')} đ</strong></span>
                  {Number(inv.balanceAmount) > 0 ? (
                    <span style={{ color: '#b42335' }}>Nợ: <strong>{Number(inv.balanceAmount).toLocaleString('vi-VN')} đ</strong></span>
                  ) : (
                    <span style={{ color: '#059669' }}>Đã thu đủ</span>
                  )}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px', borderTop: '1px solid #f0f2f5' }}>
                  <span className="muted" style={{ fontSize: '11px' }}>Biển số: {inv.vehicleSnapshot?.plateNumber || '—'}</span>
                  <div className="row-actions">
                    <Button variant="ghost" onClick={() => setViewingInvoice(inv)}>
                      <Eye size={15} /> Xem
                    </Button>
                    {(inv.status === 'UNPAID' || inv.status === 'PARTIALLY_PAID') && (
                      <Button variant="ghost" onClick={() => handleOpenPayment(inv)} style={{ color: '#059669' }}>
                        <CreditCard size={15} /> Thu
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <DataTable
            caption="Danh sách hóa đơn dịch vụ"
            columns={columns}
            data={invoicesList}
            busy={isFetching}
          />
        )}

        {/* Phân trang */}
        <div className="pagination" style={{ borderTop: '1px solid var(--border)' }}>
          <div className="pagination-info">
            Hiển thị <strong>{invoicesList.length > 0 ? 1 : 0}–{invoicesList.length}</strong> trong <strong>{totalElements}</strong> kết quả
            <span style={{ marginLeft: '12px', color: 'var(--muted)' }}>Trang {page + 1} / {totalPages}</span>
          </div>
          <div className="pagination-controls">
            <Button
              variant="secondary"
              disabled={page === 0 || isFetching}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Trang trước
            </Button>
            <Button
              variant="secondary"
              disabled={page >= totalPages - 1 || isFetching}
              onClick={() => setPage((p) => p + 1)}
            >
              Trang sau
            </Button>
          </div>
        </div>
      </section>

      {/* Modal Phát hành hóa đơn mới */}
      <Modal
        open={issuing}
        onOpenChange={setIssuing}
        title="Phát hành hóa đơn dịch vụ mới"
        description="Xuất hóa đơn từ phiếu sửa chữa đã nghiệm thu QC hoàn tất."
      >
        <form onSubmit={handleIssue} className="modal-form">
          <div className="form-section-label">THÔNG TIN PHÁT HÀNH</div>

          <Field label="Mã phiếu sửa chữa (Repair Order ID)" required hint="Phiếu sửa chữa đã hoàn thành nghiệm thu">
            <input
              name="repairOrderId"
              type="number"
              className="input font-mono"
              placeholder="VD: 101"
              required
              autoFocus
            />
          </Field>

          <Field label="Thuế giá trị gia tăng (VAT đ)" hint="Số tiền thuế VAT tính trên doanh thu">
            <input
              name="taxAmount"
              type="number"
              min="0"
              step="1000"
              defaultValue="0"
              className="input"
            />
          </Field>

          <Field label="Chiết khấu / Giảm giá (đ)" hint="Số tiền giảm trừ trực tiếp cho khách hàng">
            <input
              name="discountAmount"
              type="number"
              min="0"
              step="1000"
              defaultValue="0"
              className="input"
            />
          </Field>

          <div className="modal-form-actions">
            <Button variant="secondary" onClick={() => setIssuing(false)} disabled={issueMutation.isPending}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={issueMutation.isPending}>
              Phát hành hóa đơn
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Thu tiền khách hàng */}
      <Modal
        open={!!payingInvoice}
        onOpenChange={(open) => !open && setPayingInvoice(null)}
        title={`Thu tiền hóa đơn #${payingInvoice?.invoiceCode}`}
        description={`Khách hàng: ${payingInvoice?.customerSnapshot?.fullName || ''} · Biển số: ${payingInvoice?.vehicleSnapshot?.plateNumber || ''}`}
      >
        {payingInvoice && (
          <form onSubmit={handleReceivePayment} className="modal-form">
            <div className="form-section-label">THÔNG TIN PHIẾU THU TIỀN</div>

            <Field label="Số tiền thu (VNĐ)" required hint={`Còn nợ: ${Number(payingInvoice.balanceAmount || payingInvoice.totalAmount).toLocaleString('vi-VN')} đ`}>
              <input
                type="number"
                min="1"
                max={Number(payingInvoice.balanceAmount || payingInvoice.totalAmount)}
                step="1000"
                value={payAmount}
                onChange={(e) => setPayAmount(Number(e.target.value))}
                className="input font-bold"
                required
                autoFocus
              />
            </Field>

            <Field label="Phương thức thanh toán" required>
              <select
                className="input"
                value={payMethod}
                onChange={(e) => setPayMethod(e.target.value as PaymentMethod)}
              >
                <option value="VIETQR">Quét mã QR (VietQR)</option>
                <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
                <option value="CASH">Tiền mặt tại quầy</option>
                <option value="CARD">Cà thẻ qua máy POS</option>
              </select>
            </Field>

            <Field label="Mã giao dịch / Mã chuẩn chi ngân hàng" hint="Bắt buộc nếu chuyển khoản hoặc quét QR">
              <input
                className="input font-mono"
                placeholder="VD: QR-BIDV-102938"
                value={payReference}
                onChange={(e) => setPayReference(e.target.value)}
              />
            </Field>

            <Field label="Ghi chú phiếu thu">
              <input
                className="input"
                placeholder="Khách thanh toán qua VietQR..."
                value={payNotes}
                onChange={(e) => setPayNotes(e.target.value)}
              />
            </Field>

            <div className="modal-form-actions">
              <Button variant="secondary" onClick={() => setPayingInvoice(null)} disabled={receivePaymentMutation.isPending}>
                Hủy
              </Button>
              <Button variant="primary" type="submit" pending={receivePaymentMutation.isPending}>
                Xác nhận thu tiền
              </Button>
            </div>
          </form>
        )}
      </Modal>

      {/* Modal Chi tiết hóa đơn */}
      <Modal
        open={!!viewingInvoice}
        onOpenChange={(open) => !open && setViewingInvoice(null)}
        title={`Hóa đơn dịch vụ #${viewingInvoice?.invoiceCode}`}
        description="Chi tiết snapshot khách hàng, xe và danh mục phụ tùng & tiền công."
      >
        {viewingInvoice && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', padding: '4px 0' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '12px', background: '#f8fafc', padding: '16px', borderRadius: '8px' }}>
              <div>
                <span className="muted" style={{ fontSize: '12px' }}>Khách hàng:</span>
                <div style={{ fontWeight: 650, fontSize: '14px' }}>{viewingInvoice.customerSnapshot?.fullName}</div>
                <div className="muted" style={{ fontSize: '12px' }}>SĐT: {viewingInvoice.customerSnapshot?.phone || '—'}</div>
              </div>
              <div>
                <span className="muted" style={{ fontSize: '12px' }}>Xe sửa chữa:</span>
                <div style={{ fontWeight: 650, fontSize: '14px', color: '#245bdb' }}>
                  {viewingInvoice.vehicleSnapshot?.plateNumber}
                </div>
                <div className="muted" style={{ fontSize: '12px' }}>
                  {viewingInvoice.vehicleSnapshot?.make} {viewingInvoice.vehicleSnapshot?.model}
                </div>
              </div>
              <div>
                <span className="muted" style={{ fontSize: '12px' }}>Trạng thái thanh toán:</span>
                <div style={{ marginTop: '2px' }}>{renderStatusBadge(viewingInvoice.status)}</div>
              </div>
              <div>
                <span className="muted" style={{ fontSize: '12px' }}>Ngày phát hành:</span>
                <div style={{ fontWeight: 550, fontSize: '13px' }}>
                  {new Date(viewingInvoice.issuedAt).toLocaleString('vi-VN')}
                </div>
              </div>
            </div>

            {/* Bảng chi tiết dòng hóa đơn */}
            <div style={{ border: '1px solid var(--border)', borderRadius: '8px', overflow: 'hidden' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                <thead style={{ background: '#f8fafc', borderBottom: '1px solid var(--border)', color: '#526278', fontSize: '11px', fontWeight: 600 }}>
                  <tr>
                    <th style={{ padding: '8px 12px', textAlign: 'left' }}>Hạng mục</th>
                    <th style={{ padding: '8px 12px', width: '80px', textAlign: 'center' }}>Loại</th>
                    <th style={{ padding: '8px 12px', width: '70px', textAlign: 'right' }}>SL</th>
                    <th style={{ padding: '8px 12px', width: '110px', textAlign: 'right' }}>Đơn giá (đ)</th>
                    <th style={{ padding: '8px 12px', width: '120px', textAlign: 'right' }}>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {viewingInvoice.items?.map((item) => (
                    <tr key={item.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                      <td style={{ padding: '8px 12px' }}>
                        <div style={{ fontWeight: 550 }}>{item.itemName}</div>
                        {item.warrantyDays > 0 && (
                          <div className="muted" style={{ fontSize: '11px' }}>
                            Bảo hành: {item.warrantyDays} ngày / {item.warrantyKm} km
                          </div>
                        )}
                      </td>
                      <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                        <Badge variant={item.itemType === 'SERVICE' ? 'primary' : 'secondary'}>
                          {item.itemType === 'SERVICE' ? 'Công' : 'Vật tư'}
                        </Badge>
                      </td>
                      <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 600 }}>
                        {item.quantity} {item.unit || ''}
                      </td>
                      <td style={{ padding: '8px 12px', textAlign: 'right' }}>
                        {Number(item.unitPrice).toLocaleString('vi-VN')} đ
                      </td>
                      <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 700 }}>
                        {Number(item.lineTotal).toLocaleString('vi-VN')} đ
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Tổng hợp tài chính */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'flex-end', paddingRight: '8px', fontSize: '13px' }}>
              <div>Tạm tính: <strong>{Number(viewingInvoice.subtotal).toLocaleString('vi-VN')} đ</strong></div>
              {Number(viewingInvoice.taxAmount) > 0 && (
                <div>Thuế VAT: <strong>+{Number(viewingInvoice.taxAmount).toLocaleString('vi-VN')} đ</strong></div>
              )}
              {Number(viewingInvoice.discountAmount) > 0 && (
                <div style={{ color: '#059669' }}>
                  Giảm giá: <strong>-{Number(viewingInvoice.discountAmount).toLocaleString('vi-VN')} đ</strong>
                </div>
              )}
              <div style={{ fontSize: '16px', fontWeight: 700, color: '#245bdb', marginTop: '4px' }}>
                Tổng cộng: {Number(viewingInvoice.totalAmount).toLocaleString('vi-VN')} đ
              </div>
              <div style={{ fontSize: '13px', marginTop: '2px' }}>
                Đã thanh toán: <strong style={{ color: '#059669' }}>{Number(viewingInvoice.paidAmount || 0).toLocaleString('vi-VN')} đ</strong>
              </div>
              {Number(viewingInvoice.balanceAmount) > 0 && (
                <div style={{ fontSize: '13px', color: '#b42335', fontWeight: 600 }}>
                  Còn nợ: {Number(viewingInvoice.balanceAmount).toLocaleString('vi-VN')} đ
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', paddingTop: '12px', borderTop: '1px solid var(--border)' }}>
              <Button variant="secondary" onClick={() => setViewingInvoice(null)}>
                Đóng
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal Hủy hóa đơn */}
      <Modal
        open={!!voidingInvoice}
        onOpenChange={(open) => !open && setVoidingInvoice(null)}
        title={`Hủy hóa đơn #${voidingInvoice?.invoiceCode}`}
        description="Nhập lý do hủy hóa đơn. Thao tác hủy là bất biến."
      >
        <div className="modal-form">
          <Field label="Lý do hủy hóa đơn" required className="full-width">
            <textarea
              className="input"
              rows={3}
              placeholder="VD: Nhập sai phiếu sửa chữa, khách hủy yêu cầu..."
              value={voidReason}
              onChange={(e) => setVoidReason(e.target.value)}
              autoFocus
            />
          </Field>
          <div className="modal-form-actions">
            <Button variant="secondary" onClick={() => setVoidingInvoice(null)} disabled={voidMutation.isPending}>
              Bỏ qua
            </Button>
            <Button variant="danger" onClick={handleVoid} pending={voidMutation.isPending}>
              Xác nhận hủy hóa đơn
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
