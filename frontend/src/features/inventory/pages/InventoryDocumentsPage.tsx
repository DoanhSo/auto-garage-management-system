import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  ArrowDownLeft,
  ArrowUpRight,
  CheckCircle2,
  Eye,
  FileText,
  Plus,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Trash2,
  X,
  XCircle,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import { useAuth } from '../../../app/providers/AuthProvider';
import { WAREHOUSE_ROLES } from '../../../app/access';
import {
  Badge,
  Button,
  Combobox,
  ConfirmDialog,
  Field,
  Loading,
  Modal,
  PageHeader,
  QueryError,
} from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { errorMessage } from '../../../shared/api/errors';
import { useRepairOrdersQuery } from '../../repair/hooks/useRepair';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import { formatPlate } from '../../../shared/utils/format';
import {
  useDocumentsQuery,
  useCreateDocumentMutation,
  usePostDocumentMutation,
  useCancelDocumentMutation,
  useIssueCandidatesQuery,
  usePartsQuery,
  useSuppliersQuery,
} from '../hooks/useInventory';
import type {
  InventoryDocument,
  InventoryDocumentCreateInput,
  InventoryDocumentItemInput,
  DocumentType,
  DocumentStatus,
} from '../types/inventory';

const sortOptions = [
  ['id:desc', 'Mã chứng từ · Mới nhất'],
  ['id:asc', 'Mã chứng từ · Cũ nhất'],
  ['createdAt:desc', 'Ngày tạo · Mới nhất'],
  ['createdAt:asc', 'Ngày tạo · Cũ nhất'],
  ['totalAmount:desc', 'Tổng tiền · Cao nhất'],
  ['totalAmount:asc', 'Tổng tiền · Thấp nhất'],
];

export function InventoryDocumentsPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();
  const { user } = useAuth();
  const canManage = !!user && WAREHOUSE_ROLES.includes(user.role);

  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query);
  const [selectedType, setSelectedType] = useState<DocumentType | ''>('');
  const [selectedStatus, setSelectedStatus] = useState<DocumentStatus | ''>('');
  const [sortField, setSortField] = useState('id');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(0);

  const [viewingDoc, setViewingDoc] = useState<InventoryDocument | null>(null);
  const [creating, setCreating] = useState(false);
  const [postingDoc, setPostingDoc] = useState<InventoryDocument | null>(null);
  const [cancellingDoc, setCancellingDoc] = useState<InventoryDocument | null>(null);

  // Form states for creating document
  const [docType, setDocType] = useState<DocumentType>('IN');
  const [supplierId, setSupplierId] = useState<string>('');
  const [orderId, setOrderId] = useState<string>('');
  const [referenceNumber, setReferenceNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [lines, setLines] = useState<InventoryDocumentItemInput[]>([
    { partId: 0, quantity: 1, unitCost: 0, notes: '' },
  ]);

  const [searchParams, setSearchParams] = useSearchParams();
  const urlIssueOrderId = searchParams.get('issueOrderId') ?? '';

  useEffect(() => {
    if (!urlIssueOrderId || !canManage) return;
    setDocType('OUT');
    setOrderId(urlIssueOrderId);
    setCreating(true);
    const next = new URLSearchParams(searchParams);
    next.delete('issueOrderId');
    setSearchParams(next, { replace: true });
  }, [urlIssueOrderId, canManage, searchParams, setSearchParams]);

  // Xuất kho sửa chữa: chọn lệnh, nạp phụ tùng đã giữ chỗ, chọn dòng và số lượng cần xuất.
  const [issuePick, setIssuePick] = useState<
    Record<string, { checked: boolean; quantity: number }>
  >({});

  const createTrigger = useRef<HTMLButtonElement>(null);

  const { data: suppliersData } = useSuppliersQuery({ size: 100, active: true });
  const suppliers = suppliersData?.content || [];

  const { data: ordersData } = useRepairOrdersQuery({ size: 100, sortBy: 'id', sortDir: 'desc' });
  const { data: vehiclesData = [] } = useVehiclesQuery();
  const issueOrderOptions = useMemo(() => {
    const plateOf = new Map(vehiclesData.map((v) => [String(v.id), v.licensePlate] as const));
    return (ordersData?.content ?? [])
      .filter(
        (order) => !['INVOICED', 'CLOSED', 'CANCELLED', 'NEW', 'DIAGNOSING'].includes(order.status),
      )
      .map((order) => {
        const plate = plateOf.get(String(order.vehicleId));
        return {
          value: String(order.id),
          label: `#${order.orderCode}${plate ? ` · ${formatPlate(plate)}` : ''}`,
          description: new Date(order.receivedAt).toLocaleDateString('vi-VN'),
        };
      });
  }, [ordersData, vehiclesData]);
  const issueCandidatesQuery = useIssueCandidatesQuery(
    docType === 'OUT' && orderId ? orderId : null,
    creating,
  );
  const issueCandidates = useMemo(
    () => issueCandidatesQuery.data ?? [],
    [issueCandidatesQuery.data],
  );

  useEffect(() => {
    const next: Record<string, { checked: boolean; quantity: number }> = {};
    issueCandidates.forEach((c) => {
      next[String(c.estimateItemId)] = { checked: !c.shortage, quantity: c.reservedQuantity };
    });
    setIssuePick(next);
  }, [issueCandidates]);

  const { data: partsData } = usePartsQuery({ size: 100, active: true });
  const parts = partsData?.content || [];

  const { data, isLoading, isFetching, isError, error, refetch } = useDocumentsQuery({
    type: selectedType || undefined,
    status: selectedStatus || undefined,
    query: debouncedQuery.trim() || undefined,
    page,
    size: 20,
    sortBy: sortField,
    sortDir,
  });

  const createMutation = useCreateDocumentMutation();
  const postMutation = usePostDocumentMutation();
  const cancelMutation = useCancelDocumentMutation();

  const filterCount =
    (query.trim() ? 1 : 0) + (selectedType !== '' ? 1 : 0) + (selectedStatus !== '' ? 1 : 0);

  const isFiltered =
    query.length > 0 ||
    selectedType !== '' ||
    selectedStatus !== '' ||
    sortField !== 'id' ||
    sortDir !== 'desc';

  const handleResetFilters = () => {
    setQuery('');
    setSelectedType('');
    setSelectedStatus('');
    setSortField('id');
    setSortDir('desc');
    setPage(0);
  };

  const handleAddLine = () => {
    setLines([...lines, { partId: 0, quantity: 1, unitCost: 0, notes: '' }]);
  };

  const handleRemoveLine = (idx: number) => {
    if (lines.length <= 1) return;
    setLines(lines.filter((_, i) => i !== idx));
  };

  const handleLineChange = <K extends keyof InventoryDocumentItemInput>(
    idx: number,
    field: K,
    value: InventoryDocumentItemInput[K],
  ) => {
    const updated = [...lines];
    updated[idx] = { ...updated[idx], [field]: value };
    if (field === 'partId') {
      const selectedPart = parts.find((p) => p.id === String(value));
      if (selectedPart) {
        updated[idx].unitCost = Number(selectedPart.purchasePrice || 0);
      }
    }
    setLines(updated);
  };

  const handleCreateDocument = async (e: React.FormEvent) => {
    e.preventDefault();

    let items: InventoryDocumentItemInput[];
    if (docType === 'OUT') {
      if (!orderId) {
        notify('Chọn lệnh sửa chữa cần xuất kho.');
        return;
      }
      const picked = issueCandidates.filter((c) => issuePick[String(c.estimateItemId)]?.checked);
      if (picked.length === 0) {
        notify('Chọn ít nhất một phụ tùng để xuất kho.');
        return;
      }
      const invalid = picked.find((c) => {
        const qty = Number(issuePick[String(c.estimateItemId)]?.quantity);
        return !(qty > 0) || qty > c.reservedQuantity;
      });
      if (invalid) {
        notify(
          `Số lượng xuất của ${invalid.partName} phải lớn hơn 0 và không vượt số đã giữ chỗ (${invalid.reservedQuantity}).`,
        );
        return;
      }
      items = picked.map((c) => ({
        partId: c.partId,
        estimateItemId: c.estimateItemId,
        quantity: Number(issuePick[String(c.estimateItemId)].quantity),
        unitCost: c.unitCost,
      }));
    } else {
      if (lines.some((l) => !l.partId || Number(l.quantity) <= 0)) {
        notify('Vui lòng chọn phụ tùng và nhập số lượng lớn hơn 0 cho tất cả các dòng.');
        return;
      }
      items = lines.map((l) => ({
        partId: l.partId,
        quantity: Number(l.quantity),
        unitCost: Number(l.unitCost),
        notes: l.notes || undefined,
      }));
    }

    const payload: InventoryDocumentCreateInput = {
      documentType: docType,
      supplierId: docType === 'IN' && supplierId ? Number(supplierId) : undefined,
      repairOrderId:
        (docType === 'OUT' || docType === 'RETURN') && orderId ? Number(orderId) : undefined,
      supplierInvoiceNumber: referenceNumber || undefined,
      reason: notes || undefined,
      items,
    };

    try {
      await createMutation.mutateAsync(payload);
      notify('Đã tạo chứng từ kho ở trạng thái Bản nháp (DRAFT).');
      setCreating(false);
      setLines([{ partId: 0, quantity: 1, unitCost: 0, notes: '' }]);
      setReferenceNumber('');
      setNotes('');
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Lỗi khi tạo chứng từ kho.');
    }
  };

  const handlePost = async () => {
    if (!postingDoc) return;
    try {
      await postMutation.mutateAsync({ id: postingDoc.id });
      notify(`Đã ghi sổ chứng từ ${postingDoc.documentCode} thành công. Tồn kho đã được cập nhật.`);
      setPostingDoc(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Ghi sổ chứng từ thất bại.');
    }
  };

  const handleCancel = async () => {
    if (!cancellingDoc) return;
    try {
      await cancelMutation.mutateAsync(cancellingDoc.id);
      notify(`Đã hủy chứng từ ${cancellingDoc.documentCode}.`);
      setCancellingDoc(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Hủy chứng từ thất bại.');
    }
  };

  const renderTypeBadge = (type: DocumentType) => {
    switch (type) {
      case 'IN':
        return (
          <Badge variant="primary" className="flex items-center gap-1">
            <ArrowDownLeft size={12} /> Nhập mua
          </Badge>
        );
      case 'OUT':
        return (
          <Badge variant="warning" className="flex items-center gap-1">
            <ArrowUpRight size={12} /> Xuất sửa chữa
          </Badge>
        );
      case 'RETURN':
        return (
          <Badge variant="secondary" className="flex items-center gap-1">
            <RotateCcw size={12} /> Trả hàng
          </Badge>
        );
      case 'ADJUSTMENT':
        return <Badge variant="outline">Điều chỉnh</Badge>;
      case 'OPENING':
        return <Badge variant="secondary">Tồn đầu kỳ</Badge>;
    }
  };

  const renderStatusBadge = (status: DocumentStatus) => {
    switch (status) {
      case 'DRAFT':
        return <Badge variant="secondary">Bản nháp</Badge>;
      case 'POSTED':
        return (
          <Badge variant="success" className="flex items-center gap-1">
            <CheckCircle2 size={12} /> Đã ghi sổ
          </Badge>
        );
      case 'CANCELLED':
        return (
          <Badge variant="danger" className="flex items-center gap-1">
            <XCircle size={12} /> Đã hủy
          </Badge>
        );
    }
  };

  const columns = useMemo<ColumnDef<InventoryDocument>[]>(
    () => [
      {
        id: 'document',
        header: 'Chứng từ kho',
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
              <FileText size={16} />
            </span>
            <div>
              <strong style={{ color: '#172338', fontFamily: 'monospace' }}>
                {row.original.documentCode}
              </strong>
              <div style={{ marginTop: '2px' }}>{renderTypeBadge(row.original.documentType)}</div>
            </div>
          </div>
        ),
      },
      {
        id: 'reference',
        header: 'Đối tác / Tham chiếu',
        cell: ({ row }) => {
          const doc = row.original;
          return (
            <div style={{ fontSize: '13px' }}>
              {doc.supplierName && (
                <div style={{ fontWeight: 600, color: '#172338' }}>{doc.supplierName}</div>
              )}
              {doc.repairOrderId && (
                <div style={{ fontWeight: 600, color: '#245bdb' }}>Phiếu: #{doc.repairOrderId}</div>
              )}
              {doc.supplierInvoiceNumber && (
                <div className="muted" style={{ fontSize: '12px' }}>
                  HĐ số: {doc.supplierInvoiceNumber}
                </div>
              )}
              {!doc.supplierName && !doc.repairOrderId && !doc.supplierInvoiceNumber && (
                <span className="muted">—</span>
              )}
            </div>
          );
        },
      },
      {
        id: 'quantityAndAmount',
        header: 'Số lượng & Giá trị',
        cell: ({ row }) => (
          <div style={{ fontSize: '13px' }}>
            <div style={{ fontWeight: 650, color: '#172338' }}>
              {Number(row.original.totalAmount || 0).toLocaleString('vi-VN')} đ
            </div>
            <div className="muted" style={{ fontSize: '11px' }}>
              {row.original.items?.length || 0} mặt hàng
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'status',
        header: 'Trạng thái',
        cell: ({ row }) => renderStatusBadge(row.original.status),
      },
      {
        accessorKey: 'createdAt',
        header: 'Ngày lập',
        cell: ({ row }) => (
          <span className="muted" style={{ fontSize: '12px' }}>
            {new Date(row.original.createdAt).toLocaleDateString('vi-VN', {
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
          const doc = row.original;
          return (
            <div className="row-actions">
              <Button
                variant="ghost"
                aria-label={`Xem chi tiết ${doc.documentCode}`}
                onClick={() => setViewingDoc(doc)}
              >
                <Eye size={16} />
                <span>Xem</span>
              </Button>
              {canManage && doc.status === 'DRAFT' && (
                <>
                  <Button
                    variant="ghost"
                    aria-label={`Ghi sổ chứng từ ${doc.documentCode}`}
                    onClick={() => setPostingDoc(doc)}
                    style={{ color: '#245bdb' }}
                  >
                    <CheckCircle2 size={16} />
                    <span>Ghi sổ</span>
                  </Button>
                  <Button
                    variant="ghost"
                    className="delete-action"
                    aria-label={`Hủy chứng từ ${doc.documentCode}`}
                    onClick={() => setCancellingDoc(doc)}
                  >
                    <XCircle size={16} />
                    <span>Hủy</span>
                  </Button>
                </>
              )}
            </div>
          );
        },
      },
    ],
    [canManage],
  );

  const documentsList = data?.content || [];
  const totalElements = data?.totalElements || 0;
  const totalPages = data?.totalPages || 1;

  const totalCalculated =
    docType === 'OUT'
      ? issueCandidates.reduce((sum, c) => {
          const pick = issuePick[String(c.estimateItemId)];
          return pick?.checked ? sum + (Number(pick.quantity) || 0) * c.unitCost : sum;
        }, 0)
      : lines.reduce((sum, l) => sum + (Number(l.quantity) || 0) * (Number(l.unitCost) || 0), 0);

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ KHO & VẬT TƯ"
        title="Chứng từ kho"
        description="Quản lý phiếu nhập mua, xuất kho sửa chữa, trả hàng nhà cung cấp và điều chỉnh tồn kho."
        actions={
          canManage ? (
            <Button variant="primary" ref={createTrigger} onClick={() => setCreating(true)}>
              <Plus size={18} />
              <span>Lập chứng từ kho</span>
            </Button>
          ) : undefined
        }
      />

      <div className="info-strip">
        <FileText size={18} />
        <span>
          Chứng từ ở trạng thái Bản nháp chưa làm thay đổi tồn kho. Khi nhấn Ghi sổ, số lượng tồn và
          giá vốn bình quân được cập nhật vào sổ kho.
        </span>
      </div>

      <section className="panel table-panel">
        {/* Bộ lọc chứng từ */}
        <div className="user-filters" role="search" aria-label="Bộ lọc chứng từ kho">
          <div className="user-filters-heading">
            <div>
              <SlidersHorizontal size={16} aria-hidden />
              <h2>Bộ lọc chứng từ kho</h2>
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
              gridTemplateColumns:
                'minmax(220px, 1.4fr) minmax(260px, 1.3fr) minmax(180px, 1fr) minmax(200px, 1fr)',
            }}
          >
            {/* Ô tìm kiếm */}
            <div className="user-filter-field">
              <label htmlFor="doc-search-input">Tìm kiếm</label>
              <div className="user-search">
                <Search size={17} aria-hidden />
                <input
                  id="doc-search-input"
                  className="input"
                  placeholder="Nhập mã chứng từ, hóa đơn..."
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

            {/* Loại chứng từ Segments */}
            <fieldset className="user-filter-field status-filter">
              <legend>Loại chứng từ</legend>
              <div className="status-segments">
                {[
                  { value: '', label: 'Tất cả' },
                  { value: 'IN', label: 'Nhập mua' },
                  { value: 'OUT', label: 'Xuất kho' },
                  { value: 'RETURN', label: 'Trả hàng' },
                ].map((opt) => (
                  <button
                    key={opt.label}
                    type="button"
                    className={selectedType === opt.value ? 'selected' : ''}
                    aria-pressed={selectedType === opt.value}
                    onClick={() => {
                      setSelectedType(opt.value as DocumentType | '');
                      setPage(0);
                    }}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </fieldset>

            {/* Trạng thái */}
            <div className="user-filter-field">
              <label htmlFor="doc-status-select">Trạng thái ghi sổ</label>
              <select
                id="doc-status-select"
                className="input"
                value={selectedStatus}
                onChange={(e) => {
                  setSelectedStatus(e.target.value as DocumentStatus | '');
                  setPage(0);
                }}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="DRAFT">Bản nháp (DRAFT)</option>
                <option value="POSTED">Đã ghi sổ (POSTED)</option>
                <option value="CANCELLED">Đã hủy (CANCELLED)</option>
              </select>
            </div>

            {/* Sắp xếp */}
            <div className="user-filter-field">
              <label htmlFor="doc-sort-select">Sắp xếp theo</label>
              <select
                id="doc-sort-select"
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

        {/* Bảng danh sách */}
        {isLoading ? (
          <Loading label="Đang tải danh sách chứng từ kho…" />
        ) : isError ? (
          <QueryError error={error} retry={() => refetch()} />
        ) : compact ? (
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {documentsList.map((doc) => (
              <div
                key={doc.id}
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
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                  }}
                >
                  <div>
                    <strong style={{ fontSize: '15px', fontFamily: 'monospace' }}>
                      {doc.documentCode}
                    </strong>
                    <div style={{ marginTop: '4px' }}>{renderTypeBadge(doc.documentType)}</div>
                  </div>
                  {renderStatusBadge(doc.status)}
                </div>
                <div style={{ fontSize: '13px', color: '#526278' }}>
                  {doc.supplierName && (
                    <div>
                      NCC: <strong>{doc.supplierName}</strong>
                    </div>
                  )}
                  {doc.repairOrderId && (
                    <div>
                      Phiếu: <strong>#{doc.repairOrderId}</strong>
                    </div>
                  )}
                </div>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    paddingTop: '8px',
                    borderTop: '1px solid #f0f2f5',
                  }}
                >
                  <span style={{ fontSize: '13px', fontWeight: 650, color: '#172338' }}>
                    {Number(doc.totalAmount || 0).toLocaleString('vi-VN')} đ
                  </span>
                  <div className="row-actions">
                    <Button variant="ghost" onClick={() => setViewingDoc(doc)}>
                      <Eye size={15} /> Xem
                    </Button>
                    {canManage && doc.status === 'DRAFT' && (
                      <Button
                        variant="ghost"
                        onClick={() => setPostingDoc(doc)}
                        style={{ color: '#245bdb' }}
                      >
                        <CheckCircle2 size={15} /> Ghi sổ
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <DataTable
            caption="Danh sách chứng từ kho"
            columns={columns}
            data={documentsList}
            busy={isFetching}
          />
        )}

        {/* Phân trang */}
        <div className="pagination" style={{ borderTop: '1px solid var(--border)' }}>
          <div className="pagination-info">
            Hiển thị{' '}
            <strong>
              {documentsList.length > 0 ? 1 : 0}–{documentsList.length}
            </strong>{' '}
            trong <strong>{totalElements}</strong> kết quả
            <span style={{ marginLeft: '12px', color: 'var(--muted)' }}>
              Trang {page + 1} / {totalPages}
            </span>
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

      {/* Modal Lập chứng từ kho mới */}
      <Modal
        open={creating}
        onOpenChange={setCreating}
        title="Lập chứng từ kho mới"
        description="Tạo phiếu nhập mua, xuất kho hoặc trả hàng (chứng từ tạo mới ở trạng thái DRAFT)."
        size="xl"
      >
        <form onSubmit={handleCreateDocument} className="modal-form">
          <div className="form-section-label">THÔNG TIN CHỨNG TỪ KHO</div>

          <Field label="Loại chứng từ" required>
            <select
              className="input"
              value={docType}
              onChange={(e) => setDocType(e.target.value as DocumentType)}
            >
              <option value="IN">Nhập mua phụ tùng</option>
              <option value="OUT">Xuất kho sửa chữa</option>
              <option value="RETURN">Trả hàng nhà cung cấp (RETURN)</option>
              <option value="ADJUSTMENT">Điều chỉnh kiểm kê (ADJUSTMENT)</option>
              <option value="OPENING">Khai báo tồn đầu kỳ (OPENING)</option>
            </select>
          </Field>

          {docType === 'IN' ? (
            <Field label="Nhà cung cấp" required>
              <select
                className="input"
                value={supplierId}
                onChange={(e) => setSupplierId(e.target.value)}
                required
              >
                <option value="">Chọn nhà cung cấp</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.supplierName} ({s.supplierCode})
                  </option>
                ))}
              </select>
            </Field>
          ) : docType === 'OUT' ? (
            <Field
              label="Lệnh sửa chữa cần xuất kho"
              required
              hint="Gõ mã lệnh hoặc biển số để tìm. Chỉ lệnh còn xuất kho được mới hiện."
            >
              <Combobox
                value={orderId}
                onChange={setOrderId}
                options={issueOrderOptions}
                placeholder="Gõ mã lệnh hoặc biển số…"
                aria-required
              />
            </Field>
          ) : docType === 'RETURN' ? (
            <Field label="Mã lệnh sửa chữa" required>
              <input
                className="input font-mono"
                placeholder="VD: 101"
                value={orderId}
                onChange={(e) => setOrderId(e.target.value)}
                required
              />
            </Field>
          ) : (
            <Field label="Phân loại nghiệp vụ">
              <input className="input" value="Nghiệp vụ nội bộ gara" disabled />
            </Field>
          )}

          <Field label="Số hóa đơn NCC / Chứng từ gốc">
            <input
              className="input"
              placeholder="VD: HD-001928"
              value={referenceNumber}
              onChange={(e) => setReferenceNumber(e.target.value)}
            />
          </Field>

          <div className="full-width">
            <Field label="Lý do / Diễn giải chứng từ">
              <input
                className="input"
                placeholder="Nhập ghi chú hoặc lý do phát sinh chứng từ kho..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </Field>
          </div>

          {docType === 'OUT' && (
            <div className="full-width" style={{ marginTop: '4px' }}>
              <span className="form-section-label" style={{ margin: 0, padding: 0 }}>
                PHỤ TÙNG CHỜ XUẤT CỦA LỆNH
              </span>
              {!orderId ? (
                <p className="muted" style={{ margin: '8px 0 0' }}>
                  Chọn lệnh sửa chữa để xem các phụ tùng đã duyệt và giữ chỗ chờ xuất kho.
                </p>
              ) : issueCandidatesQuery.isLoading ? (
                <Loading label="Đang tải phụ tùng chờ xuất…" />
              ) : issueCandidatesQuery.isError ? (
                <p className="field-error" role="alert">
                  {errorMessage(issueCandidatesQuery.error)}
                </p>
              ) : issueCandidates.length === 0 ? (
                <p className="muted" style={{ margin: '8px 0 0' }}>
                  Lệnh này không còn phụ tùng nào chờ xuất kho.
                </p>
              ) : (
                <div
                  style={{
                    border: '1px solid var(--border)',
                    borderRadius: '8px',
                    overflow: 'hidden',
                    marginTop: '8px',
                  }}
                >
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                    <thead
                      style={{
                        background: '#f8fafc',
                        borderBottom: '1px solid var(--border)',
                        color: '#526278',
                        fontSize: '11px',
                        fontWeight: 600,
                      }}
                    >
                      <tr>
                        <th style={{ padding: '8px 12px', width: '40px' }}></th>
                        <th style={{ padding: '8px 12px', textAlign: 'left' }}>Phụ tùng</th>
                        <th style={{ padding: '8px 12px', width: '110px', textAlign: 'right' }}>
                          Đã giữ chỗ
                        </th>
                        <th style={{ padding: '8px 12px', width: '110px', textAlign: 'right' }}>
                          Tồn kho
                        </th>
                        <th style={{ padding: '8px 12px', width: '110px', textAlign: 'right' }}>
                          Số lượng xuất
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {issueCandidates.map((c) => {
                        const key = String(c.estimateItemId);
                        const pick = issuePick[key] ?? {
                          checked: false,
                          quantity: c.reservedQuantity,
                        };
                        return (
                          <tr key={key} style={{ borderBottom: '1px solid #f1f5f9' }}>
                            <td style={{ padding: '6px 12px', textAlign: 'center' }}>
                              <input
                                type="checkbox"
                                aria-label={`Chọn xuất ${c.partName}`}
                                checked={pick.checked}
                                disabled={c.shortage}
                                onChange={(e) =>
                                  setIssuePick((prev) => ({
                                    ...prev,
                                    [key]: { ...pick, checked: e.target.checked },
                                  }))
                                }
                              />
                            </td>
                            <td style={{ padding: '6px 12px' }}>
                              <strong>{c.partName}</strong>{' '}
                              <span className="muted">({c.partCode})</span>
                              {c.shortage && (
                                <div>
                                  <Badge variant="danger">
                                    Thiếu hàng, tồn còn {c.stockQuantity} {c.unit}
                                  </Badge>
                                </div>
                              )}
                            </td>
                            <td style={{ padding: '6px 12px', textAlign: 'right' }}>
                              {c.reservedQuantity} {c.unit}
                            </td>
                            <td style={{ padding: '6px 12px', textAlign: 'right' }}>
                              {c.stockQuantity} {c.unit}
                            </td>
                            <td style={{ padding: '6px 12px' }}>
                              <input
                                type="number"
                                min="1"
                                max={c.reservedQuantity}
                                className="input"
                                style={{ textAlign: 'right' }}
                                aria-label={`Số lượng xuất ${c.partName}`}
                                value={pick.quantity}
                                disabled={c.shortage || !pick.checked}
                                onChange={(e) =>
                                  setIssuePick((prev) => ({
                                    ...prev,
                                    [key]: { ...pick, quantity: Number(e.target.value) },
                                  }))
                                }
                              />
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Danh sách mặt hàng chi tiết */}
          {docType !== 'OUT' && (
            <div className="full-width" style={{ marginTop: '4px' }}>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '8px',
                }}
              >
                <span className="form-section-label" style={{ margin: 0, padding: 0 }}>
                  CHI TIẾT DANH MỤC PHỤ TÙNG
                </span>
                <Button
                  variant="secondary"
                  onClick={handleAddLine}
                  style={{ minHeight: '30px', padding: '3px 10px', fontSize: '12px' }}
                >
                  <Plus size={14} /> Thêm dòng
                </Button>
              </div>

              <div
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: '8px',
                  overflow: 'hidden',
                  maxHeight: '180px',
                  overflowY: 'auto',
                }}
              >
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                  <thead
                    style={{
                      background: '#f8fafc',
                      borderBottom: '1px solid var(--border)',
                      color: '#526278',
                      fontSize: '11px',
                      fontWeight: 600,
                      position: 'sticky',
                      top: 0,
                      zIndex: 1,
                    }}
                  >
                    <tr>
                      <th style={{ padding: '8px 12px', textAlign: 'left' }}>Phụ tùng</th>
                      <th style={{ padding: '8px 12px', width: '110px', textAlign: 'right' }}>
                        Số lượng
                      </th>
                      <th style={{ padding: '8px 12px', width: '150px', textAlign: 'right' }}>
                        Đơn giá (đ)
                      </th>
                      <th style={{ padding: '8px 12px', width: '150px', textAlign: 'right' }}>
                        Thành tiền
                      </th>
                      <th style={{ padding: '8px 12px', width: '50px', textAlign: 'center' }}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {lines.map((line, idx) => {
                      const rowTotal = (Number(line.quantity) || 0) * (Number(line.unitCost) || 0);
                      return (
                        <tr key={idx} style={{ borderBottom: '1px solid #f1f5f9' }}>
                          <td style={{ padding: '6px 12px' }}>
                            <select
                              className="input"
                              value={line.partId}
                              onChange={(e) =>
                                handleLineChange(idx, 'partId', Number(e.target.value))
                              }
                              required
                            >
                              <option value={0}>Chọn phụ tùng...</option>
                              {parts.map((p) => (
                                <option key={p.id} value={p.id}>
                                  {p.partName} ({p.sku})
                                </option>
                              ))}
                            </select>
                          </td>
                          <td style={{ padding: '6px 12px' }}>
                            <input
                              type="number"
                              min="1"
                              className="input"
                              style={{ textAlign: 'right' }}
                              value={line.quantity}
                              onChange={(e) =>
                                handleLineChange(idx, 'quantity', Number(e.target.value))
                              }
                              required
                            />
                          </td>
                          <td style={{ padding: '6px 12px' }}>
                            <input
                              type="number"
                              min="0"
                              step="1000"
                              className="input"
                              style={{ textAlign: 'right' }}
                              value={line.unitCost}
                              onChange={(e) =>
                                handleLineChange(idx, 'unitCost', Number(e.target.value))
                              }
                              required
                            />
                          </td>
                          <td style={{ padding: '6px 12px', textAlign: 'right', fontWeight: 600 }}>
                            {rowTotal.toLocaleString('vi-VN')} đ
                          </td>
                          <td style={{ padding: '6px 12px', textAlign: 'center' }}>
                            {lines.length > 1 && (
                              <button
                                type="button"
                                onClick={() => handleRemoveLine(idx)}
                                style={{
                                  background: 'transparent',
                                  border: 0,
                                  color: 'var(--danger)',
                                  cursor: 'pointer',
                                }}
                              >
                                <Trash2 size={16} />
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'flex-end',
                  alignItems: 'center',
                  marginTop: '10px',
                  gap: '12px',
                }}
              >
                <span style={{ fontSize: '13px', color: '#526278' }}>Tổng cộng ước tính:</span>
                <strong style={{ fontSize: '17px', color: '#245bdb' }}>
                  {totalCalculated.toLocaleString('vi-VN')} đ
                </strong>
              </div>
            </div>
          )}

          <div className="modal-form-actions full-width">
            <Button
              variant="secondary"
              onClick={() => setCreating(false)}
              disabled={createMutation.isPending}
            >
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={createMutation.isPending}>
              Lưu bản nháp (DRAFT)
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Xem chi tiết chứng từ kho */}
      <Modal
        open={!!viewingDoc}
        onOpenChange={(open) => !open && setViewingDoc(null)}
        title={`Chi tiết chứng từ #${viewingDoc?.documentCode}`}
        description="Xem thông tin chi tiết chứng từ và danh sách phụ tùng đi kèm."
        size="xl"
      >
        {viewingDoc && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', padding: '4px 0' }}>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
                gap: '12px 16px',
                background: '#f8fafc',
                padding: '14px 18px',
                borderRadius: '8px',
              }}
            >
              <div>
                <span className="muted" style={{ fontSize: '12px' }}>
                  Loại chứng từ:
                </span>
                <div style={{ marginTop: '2px' }}>{renderTypeBadge(viewingDoc.documentType)}</div>
              </div>
              <div>
                <span className="muted" style={{ fontSize: '12px' }}>
                  Trạng thái:
                </span>
                <div style={{ marginTop: '2px' }}>{renderStatusBadge(viewingDoc.status)}</div>
              </div>
              <div>
                <span className="muted" style={{ fontSize: '12px' }}>
                  {viewingDoc.supplierName
                    ? 'Nhà cung cấp:'
                    : viewingDoc.repairOrderId
                      ? 'Mã phiếu SC:'
                      : 'Đối tác:'}
                </span>
                <div style={{ fontWeight: 600, marginTop: '2px' }}>
                  {viewingDoc.supplierName ||
                    (viewingDoc.repairOrderId ? `#${viewingDoc.repairOrderId}` : '—')}
                </div>
              </div>
              <div>
                <span className="muted" style={{ fontSize: '12px' }}>
                  Hóa đơn gốc:
                </span>
                <div style={{ marginTop: '2px' }}>{viewingDoc.supplierInvoiceNumber || '—'}</div>
              </div>
              {viewingDoc.reason && (
                <div
                  style={{
                    gridColumn: '1 / -1',
                    paddingTop: '4px',
                    borderTop: '1px solid #eef2f6',
                  }}
                >
                  <span className="muted" style={{ fontSize: '12px' }}>
                    Lý do / Diễn giải:{' '}
                  </span>
                  <span style={{ fontSize: '13px' }}>{viewingDoc.reason}</span>
                </div>
              )}
            </div>

            {/* Bảng danh sách dòng chứng từ */}
            <div
              style={{
                border: '1px solid var(--border)',
                borderRadius: '8px',
                overflow: 'hidden',
                maxHeight: '220px',
                overflowY: 'auto',
              }}
            >
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                <thead
                  style={{
                    background: '#f8fafc',
                    borderBottom: '1px solid var(--border)',
                    color: '#526278',
                    fontSize: '11px',
                    fontWeight: 600,
                    position: 'sticky',
                    top: 0,
                    zIndex: 1,
                  }}
                >
                  <tr>
                    <th style={{ padding: '8px 12px', textAlign: 'left' }}>STT</th>
                    <th style={{ padding: '8px 12px', textAlign: 'left' }}>Mã SKU</th>
                    <th style={{ padding: '8px 12px', textAlign: 'left' }}>Tên phụ tùng</th>
                    <th style={{ padding: '8px 12px', textAlign: 'right' }}>Số lượng</th>
                    <th style={{ padding: '8px 12px', textAlign: 'right' }}>Đơn giá (đ)</th>
                    <th style={{ padding: '8px 12px', textAlign: 'right' }}>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {viewingDoc.items?.map((item, idx) => (
                    <tr key={item.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                      <td style={{ padding: '8px 12px', color: '#64748b' }}>{idx + 1}</td>
                      <td style={{ padding: '8px 12px', fontFamily: 'monospace', fontWeight: 600 }}>
                        {item.partSku}
                      </td>
                      <td style={{ padding: '8px 12px' }}>{item.partName}</td>
                      <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 600 }}>
                        {item.quantity} {item.unit}
                      </td>
                      <td style={{ padding: '8px 12px', textAlign: 'right' }}>
                        {Number(item.unitCost).toLocaleString('vi-VN')} đ
                      </td>
                      <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 700 }}>
                        {Number(item.lineTotal).toLocaleString('vi-VN')} đ
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div
              style={{
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                gap: '12px',
                paddingRight: '8px',
              }}
            >
              <span style={{ fontSize: '13px', color: '#526278' }}>Tổng giá trị chứng từ:</span>
              <strong style={{ fontSize: '18px', color: '#245bdb' }}>
                {Number(viewingDoc.totalAmount).toLocaleString('vi-VN')} đ
              </strong>
            </div>

            <div
              style={{
                display: 'flex',
                justifyContent: 'flex-end',
                paddingTop: '10px',
                borderTop: '1px solid var(--border)',
              }}
            >
              <Button variant="secondary" onClick={() => setViewingDoc(null)}>
                Đóng
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* ConfirmDialog Ghi sổ */}
      <ConfirmDialog
        open={!!postingDoc}
        title="Ghi sổ chứng từ kho?"
        description={`Xác nhận ghi sổ cho chứng từ "${postingDoc?.documentCode}". Thao tác này sẽ ghi nhận bất biến vào sổ kho, cập nhật số lượng tồn thực tế và giá vốn bình quân (MAC). Sau khi ghi sổ không thể sửa đổi.`}
        confirmLabel="Ghi sổ (POST)"
        pending={postMutation.isPending}
        onCancel={() => setPostingDoc(null)}
        onConfirm={handlePost}
      />

      {/* ConfirmDialog Hủy */}
      <ConfirmDialog
        open={!!cancellingDoc}
        title="Hủy chứng từ kho?"
        description={`Hủy bỏ bản nháp chứng từ "${cancellingDoc?.documentCode}". Chứng từ sẽ chuyển sang trạng thái CANCELLED.`}
        confirmLabel="Hủy chứng từ"
        danger
        pending={cancelMutation.isPending}
        onCancel={() => setCancellingDoc(null)}
        onConfirm={handleCancel}
      />
    </>
  );
}
