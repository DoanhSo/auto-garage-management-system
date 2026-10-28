import { useMemo, useRef, useState } from 'react';
import {
  ArrowDownLeft,
  ArrowUpRight,
  DollarSign,
  Plus,
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
import { usePaymentsQuery, usePaySupplierMutation } from '../hooks/useBilling';
import { useSuppliersQuery, useDocumentsQuery } from '../../inventory/hooks/useInventory';
import type {
  Payment,
  PaymentDirection,
  PaymentMethod,
  SupplierPaymentInput,
} from '../types/billing';

const sortOptions = [
  ['id:desc', 'Mã giao dịch · Mới nhất'],
  ['id:asc', 'Mã giao dịch · Cũ nhất'],
  ['paidAt:desc', 'Ngày thu chi · Mới nhất'],
  ['paidAt:asc', 'Ngày thu chi · Cũ nhất'],
  ['amount:desc', 'Số tiền · Cao nhất'],
  ['amount:asc', 'Số tiền · Thấp nhất'],
];

export function PaymentsPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();

  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query);
  const [direction, setDirection] = useState<PaymentDirection | ''>('');
  const [sortField, setSortField] = useState('id');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(0);

  // Modal Chi trả NCC
  const [payingSupplier, setPayingSupplier] = useState(false);
  const [selectedSupplierId, setSelectedSupplierId] = useState<string>('');
  const [selectedDocumentId, setSelectedDocumentId] = useState<string>('');
  const [payAmount, setPayAmount] = useState<number>(0);
  const [payMethod, setPayMethod] = useState<PaymentMethod>('BANK_TRANSFER');
  const [payReference, setPayReference] = useState('');
  const [payNotes, setPayNotes] = useState('');

  const createTrigger = useRef<HTMLButtonElement>(null);

  const { data: suppliersData } = useSuppliersQuery({ size: 100, active: true });
  const suppliers = suppliersData?.content || [];

  const { data: documentsData } = useDocumentsQuery({
    supplierId: selectedSupplierId ? Number(selectedSupplierId) : undefined,
    status: 'POSTED',
    size: 50,
  });
  const supplierDocuments = documentsData?.content || [];

  const { data, isLoading, isFetching } = usePaymentsQuery({
    direction: direction || undefined,
    query: debouncedQuery.trim() || undefined,
    page,
    size: 20,
    sortBy: sortField,
    sortDir,
  });

  const paySupplierMutation = usePaySupplierMutation();

  const filterCount = (query.trim() ? 1 : 0) + (direction !== '' ? 1 : 0);
  const isFiltered = query.length > 0 || direction !== '' || sortField !== 'id' || sortDir !== 'desc';

  const handleResetFilters = () => {
    setQuery('');
    setDirection('');
    setSortField('id');
    setSortDir('desc');
    setPage(0);
  };

  const handlePaySupplier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDocumentId || payAmount <= 0) {
      notify('Vui lòng chọn chứng từ nhập kho và nhập số tiền thanh toán hợp lệ.');
      return;
    }

    const payload: SupplierPaymentInput = {
      inventoryDocumentId: Number(selectedDocumentId),
      amount: payAmount,
      paymentMethod: payMethod,
      providerReference: payReference || undefined,
      notes: payNotes || undefined,
    };

    try {
      await paySupplierMutation.mutateAsync(payload);
      notify(`Đã lập phiếu chi trả ${payAmount.toLocaleString('vi-VN')} đ cho nhà cung cấp.`);
      setPayingSupplier(false);
      setSelectedSupplierId('');
      setSelectedDocumentId('');
      setPayAmount(0);
      setPayReference('');
      setPayNotes('');
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Lập phiếu chi thất bại.');
    }
  };

  const renderMethodBadge = (method: PaymentMethod) => {
    switch (method) {
      case 'CASH':
        return <Badge variant="secondary">Tiền mặt</Badge>;
      case 'BANK_TRANSFER':
        return <Badge variant="primary">Chuyển khoản</Badge>;
      case 'CARD':
        return <Badge variant="outline">Thẻ POS</Badge>;
      case 'VIETQR':
        return <Badge variant="primary">VietQR</Badge>;
    }
  };

  const columns = useMemo<ColumnDef<Payment>[]>(
    () => [
      {
        id: 'transaction',
        header: 'Giao dịch',
        cell: ({ row }) => {
          const isIncome = row.original.direction === 'IN';
          return (
            <div className="account-cell">
              <span
                className="avatar"
                style={{
                  background: isIncome ? '#ecfdf5' : '#fff1f2',
                  color: isIncome ? '#059669' : '#b42335',
                  fontWeight: 700,
                }}
              >
                {isIncome ? <ArrowDownLeft size={16} /> : <ArrowUpRight size={16} />}
              </span>
              <div>
                <strong style={{ color: '#172338', fontFamily: 'monospace' }}>
                  {row.original.transactionCode}
                </strong>
                <div style={{ marginTop: '2px' }}>
                  <Badge variant={isIncome ? 'success' : 'danger'}>
                    {isIncome ? 'Thu tiền (IN)' : 'Chi tiền (OUT)'}
                  </Badge>
                </div>
              </div>
            </div>
          );
        },
      },
      {
        id: 'reference',
        header: 'Tham chiếu chứng từ',
        cell: ({ row }) => {
          const p = row.original;
          return (
            <div style={{ fontSize: '13px' }}>
              {p.invoiceCode && (
                <div style={{ fontWeight: 600, color: '#245bdb' }}>HĐ: {p.invoiceCode}</div>
              )}
              {p.documentCode && (
                <div style={{ fontWeight: 600, color: '#36485f' }}>Phiếu kho: {p.documentCode}</div>
              )}
              {p.providerReference && (
                <div className="muted font-mono" style={{ fontSize: '11px' }}>
                  Mã GD: {p.providerReference}
                </div>
              )}
              {!p.invoiceCode && !p.documentCode && <span className="muted">—</span>}
            </div>
          );
        },
      },
      {
        id: 'amount',
        header: 'Số tiền (VNĐ)',
        cell: ({ row }) => {
          const isIncome = row.original.direction === 'IN';
          return (
            <span
              style={{
                fontSize: '14px',
                fontWeight: 700,
                color: isIncome ? '#059669' : '#b42335',
              }}
            >
              {isIncome ? '+' : '-'}{Number(row.original.amount).toLocaleString('vi-VN')} đ
            </span>
          );
        },
      },
      {
        accessorKey: 'paymentMethod',
        header: 'Phương thức',
        cell: ({ row }) => renderMethodBadge(row.original.paymentMethod),
      },
      {
        accessorKey: 'paidAt',
        header: 'Thời gian',
        cell: ({ row }) => (
          <span className="muted" style={{ fontSize: '12px' }}>
            {new Date(row.original.paidAt).toLocaleDateString('vi-VN', {
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
        accessorKey: 'notes',
        header: 'Ghi chú / Người lập',
        cell: ({ row }) => (
          <div style={{ fontSize: '12px' }}>
            <div style={{ color: '#172338' }}>{row.original.notes || '—'}</div>
            {row.original.recordedByName && (
              <div className="muted" style={{ fontSize: '11px' }}>
                Bởi: {row.original.recordedByName}
              </div>
            )}
          </div>
        ),
      },
    ],
    [],
  );

  const paymentsList = data?.content || [];
  const totalElements = data?.totalElements || 0;
  const totalPages = data?.totalPages || 1;

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ TÀI CHÍNH & THANH TOÁN"
        title="Sổ quỹ Thu / Chi"
        description="Nhật ký giao dịch thu tiền khách hàng và thanh toán tiền hàng nhà cung cấp."
        actions={
          <Button
            variant="primary"
            ref={createTrigger}
            onClick={() => setPayingSupplier(true)}
          >
            <Plus size={18} />
            <span>Lập phiếu chi trả NCC</span>
          </Button>
        }
      />

      <div className="info-strip">
        <DollarSign size={18} />
        <span>
          Mọi giao dịch thu tiền (IN) và chi trả (OUT) đều phát sinh mã giao dịch duy nhất bất biến và khóa tham chiếu chứng từ tương ứng, chống thanh toán vượt hoặc lặp giao dịch.
        </span>
      </div>

      <section className="panel table-panel">
        {/* Bộ lọc sổ quỹ */}
        <div className="user-filters" role="search" aria-label="Bộ lọc sổ quỹ">
          <div className="user-filters-heading">
            <div>
              <SlidersHorizontal size={16} aria-hidden />
              <h2>Bộ lọc sổ quỹ</h2>
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
              gridTemplateColumns: 'minmax(220px, 1.4fr) minmax(280px, 1.4fr) minmax(200px, 1fr)',
            }}
          >
            {/* Ô tìm kiếm */}
            <div className="user-filter-field">
              <label htmlFor="pay-search-input">Tìm kiếm</label>
              <div className="user-search">
                <Search size={17} aria-hidden />
                <input
                  id="pay-search-input"
                  className="input"
                  placeholder="Nhập mã giao dịch, số tham chiếu..."
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

            {/* Dòng tiền Segments */}
            <fieldset className="user-filter-field status-filter">
              <legend>Dòng tiền</legend>
              <div className="status-segments">
                {[
                  { value: '', label: 'Tất cả' },
                  { value: 'IN', label: 'Thu tiền (IN)' },
                  { value: 'OUT', label: 'Chi trả (OUT)' },
                ].map((opt) => (
                  <button
                    key={opt.label}
                    type="button"
                    className={direction === opt.value ? 'selected' : ''}
                    onClick={() => {
                      setDirection(opt.value as PaymentDirection | '');
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
              <label htmlFor="pay-sort-select">Sắp xếp theo</label>
              <select
                id="pay-sort-select"
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
          <Loading label="Đang tải dữ liệu sổ quỹ…" />
        ) : compact ? (
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {paymentsList.map((p) => {
              const isIncome = p.direction === 'IN';
              return (
                <div
                  key={p.id}
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
                      <strong style={{ fontSize: '15px', fontFamily: 'monospace' }}>{p.transactionCode}</strong>
                      <div className="muted" style={{ fontSize: '12px' }}>{p.invoiceCode || p.documentCode}</div>
                    </div>
                    <Badge variant={isIncome ? 'success' : 'danger'}>
                      {isIncome ? '+ Thu tiền' : '- Chi tiền'}
                    </Badge>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span
                      style={{
                        fontSize: '16px',
                        fontWeight: 700,
                        color: isIncome ? '#059669' : '#b42335',
                      }}
                    >
                      {Number(p.amount).toLocaleString('vi-VN')} đ
                    </span>
                    {renderMethodBadge(p.paymentMethod)}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#64748b', paddingTop: '6px', borderTop: '1px solid #f0f2f5' }}>
                    <span>{new Date(p.paidAt).toLocaleString('vi-VN')}</span>
                    <span>{p.recordedByName || ''}</span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <DataTable
            caption="Sổ quỹ và nhật ký thanh toán"
            columns={columns}
            data={paymentsList}
            busy={isFetching}
          />
        )}

        {/* Phân trang */}
        <div className="pagination" style={{ borderTop: '1px solid var(--border)' }}>
          <div className="pagination-info">
            Hiển thị <strong>{paymentsList.length > 0 ? 1 : 0}–{paymentsList.length}</strong> trong <strong>{totalElements}</strong> kết quả
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

      {/* Modal Lập phiếu chi trả NCC */}
      <Modal
        open={payingSupplier}
        onOpenChange={setPayingSupplier}
        title="Lập phiếu chi trả nhà cung cấp"
        description="Thanh toán tiền hàng cho đối tác cung ứng dựa trên chứng từ nhập kho đã ghi sổ."
        size="lg"
      >
        <form onSubmit={handlePaySupplier} className="modal-form grid-3col">
          <div className="form-section-label">THÔNG TIN ĐỐI TÁC & CHỨNG TỪ CHI TRẢ</div>

          <Field label="Nhà cung cấp" required>
            <select
              className="input"
              value={selectedSupplierId}
              onChange={(e) => {
                setSelectedSupplierId(e.target.value);
                setSelectedDocumentId('');
              }}
              required
              autoFocus
            >
              <option value="">Chọn nhà cung cấp</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.supplierName} ({s.supplierCode})
                </option>
              ))}
            </select>
          </Field>

          <Field label="Chứng từ nhập kho cần thanh toán" required>
            <select
              className="input"
              value={selectedDocumentId}
              onChange={(e) => {
                setSelectedDocumentId(e.target.value);
                const doc = supplierDocuments.find((d) => String(d.id) === e.target.value);
                if (doc) setPayAmount(Number(doc.totalAmount || 0));
              }}
              required
              disabled={!selectedSupplierId}
            >
              <option value="">Chọn chứng từ nhập kho (POSTED)</option>
              {supplierDocuments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.documentCode} · {Number(d.totalAmount).toLocaleString('vi-VN')} đ
                </option>
              ))}
            </select>
          </Field>

          <Field label="Số tiền chi trả (VNĐ)" required>
            <input
              type="number"
              min="1"
              step="1000"
              value={payAmount}
              onChange={(e) => setPayAmount(Number(e.target.value))}
              className="input font-bold"
              required
            />
          </Field>

          <div className="form-section-label">PHƯƠNG THỨC & CHỨNG TỪ GIAO DỊCH</div>

          <Field label="Phương thức thanh toán" required>
            <select
              className="input"
              value={payMethod}
              onChange={(e) => setPayMethod(e.target.value as PaymentMethod)}
            >
              <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
              <option value="CASH">Tiền mặt</option>
              <option value="VIETQR">Quét mã VietQR</option>
            </select>
          </Field>

          <Field label="Mã giao dịch / Ủy nhiệm chi">
            <input
              className="input font-mono"
              placeholder="VD: FT2629810239"
              value={payReference}
              onChange={(e) => setPayReference(e.target.value)}
            />
          </Field>

          <Field label="Ghi chú phiếu chi">
            <input
              className="input"
              placeholder="Thanh toán tiền phụ tùng theo hợp đồng..."
              value={payNotes}
              onChange={(e) => setPayNotes(e.target.value)}
            />
          </Field>

          <div className="modal-form-actions col-span-3">
            <Button variant="secondary" onClick={() => setPayingSupplier(false)} disabled={paySupplierMutation.isPending}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={paySupplierMutation.isPending}>
              Xác nhận chi trả
            </Button>
          </div>
        </form>
      </Modal>
    </>
  );
}
