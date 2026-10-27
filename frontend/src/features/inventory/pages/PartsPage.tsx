import { useCallback, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  Edit2,
  Package,
  Plus,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Trash2,
  X,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import {
  Badge,
  Button,
  ConfirmDialog,
  Field,
  Loading,
  Modal,
  PageHeader,
  StatusBadge,
} from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { errorMessage } from '../../../shared/api/errors';
import {
  usePartsQuery,
  useLowStockPartsQuery,
  useSuppliersQuery,
  useCreatePartMutation,
  useUpdatePartMutation,
  useDeactivatePartMutation,
} from '../hooks/useInventory';
import type { Part, PartCreateInput, PartUpdateInput } from '../types/inventory';

const sortOptions = [
  ['id:desc', 'Mã phụ tùng · Mới nhất'],
  ['id:asc', 'Mã phụ tùng · Cũ nhất'],
  ['partName:asc', 'Tên phụ tùng · A → Z'],
  ['partName:desc', 'Tên phụ tùng · Z → A'],
  ['stockQuantity:asc', 'Tồn kho · Thấp nhất trước'],
  ['stockQuantity:desc', 'Tồn kho · Cao nhất trước'],
  ['sellingPrice:desc', 'Giá bán · Cao → Thấp'],
  ['sellingPrice:asc', 'Giá bán · Thấp → Cao'],
];

export function PartsPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();

  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query);
  const [supplierId, setSupplierId] = useState<string>('');
  const [onlyLowStock, setOnlyLowStock] = useState(false);
  const [active, setActive] = useState<boolean | undefined>(undefined);
  const [sortField, setSortField] = useState('id');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(0);

  const [editing, setEditing] = useState<Part | null>(null);
  const [creating, setCreating] = useState(false);
  const [deletingPart, setDeletingPart] = useState<Part | null>(null);

  const createTrigger = useRef<HTMLButtonElement>(null);

  const { data: suppliersData } = useSuppliersQuery({ size: 100, active: true });
  const suppliers = suppliersData?.content || [];

  const { data, isLoading, isFetching } = usePartsQuery({
    query: debouncedQuery.trim() || undefined,
    supplierId: supplierId ? Number(supplierId) : undefined,
    active,
    page,
    size: 20,
    sortBy: sortField,
    sortDir,
  });

  const { data: lowStockParts } = useLowStockPartsQuery();

  const createMutation = useCreatePartMutation();
  const updateMutation = useUpdatePartMutation();
  const deactivateMutation = useDeactivatePartMutation();

  const filterCount =
    (query.trim() ? 1 : 0) +
    (supplierId ? 1 : 0) +
    (onlyLowStock ? 1 : 0) +
    (active !== undefined ? 1 : 0);

  const isFiltered =
    query.length > 0 ||
    supplierId !== '' ||
    onlyLowStock ||
    active !== undefined ||
    sortField !== 'id' ||
    sortDir !== 'desc';

  const handleResetFilters = () => {
    setQuery('');
    setSupplierId('');
    setOnlyLowStock(false);
    setActive(undefined);
    setSortField('id');
    setSortDir('desc');
    setPage(0);
  };

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: PartCreateInput = {
      sku: (formData.get('sku') as string)?.trim().toUpperCase(),
      partName: (formData.get('partName') as string)?.trim(),
      unit: (formData.get('unit') as string)?.trim(),
      description: (formData.get('description') as string)?.trim() || undefined,
      preferredSupplierId: formData.get('preferredSupplierId')
        ? Number(formData.get('preferredSupplierId'))
        : undefined,
      purchasePrice: Number(formData.get('purchasePrice') || 0),
      sellingPrice: Number(formData.get('sellingPrice') || 0),
      reorderLevel: Number(formData.get('reorderLevel') || 0),
      warrantyDays: Number(formData.get('warrantyDays') || 0),
      warrantyKm: Number(formData.get('warrantyKm') || 0),
      warrantyTerms: (formData.get('warrantyTerms') as string)?.trim() || undefined,
    };

    try {
      await createMutation.mutateAsync(input);
      notify('Đã thêm phụ tùng mới.');
      setCreating(false);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Không thể lưu phụ tùng.');
    }
  };

  const handleUpdate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editing) return;
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: PartUpdateInput = {
      partName: (formData.get('partName') as string)?.trim(),
      unit: (formData.get('unit') as string)?.trim(),
      description: (formData.get('description') as string)?.trim() || undefined,
      preferredSupplierId: formData.get('preferredSupplierId')
        ? Number(formData.get('preferredSupplierId'))
        : undefined,
      purchasePrice: Number(formData.get('purchasePrice') || 0),
      sellingPrice: Number(formData.get('sellingPrice') || 0),
      reorderLevel: Number(formData.get('reorderLevel') || 0),
      warrantyDays: Number(formData.get('warrantyDays') || 0),
      warrantyKm: Number(formData.get('warrantyKm') || 0),
      warrantyTerms: (formData.get('warrantyTerms') as string)?.trim() || undefined,
      isActive: formData.get('isActive') === 'true',
      version: editing.version,
    };

    try {
      await updateMutation.mutateAsync({ id: editing.id, data: input });
      notify('Đã cập nhật phụ tùng.');
      setEditing(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Cập nhật thất bại.');
    }
  };

  const handleDeactivate = async () => {
    if (!deletingPart) return;
    try {
      await deactivateMutation.mutateAsync(deletingPart.id);
      notify(`Đã ngừng kinh doanh phụ tùng ${deletingPart.partName}.`);
      setDeletingPart(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Thao tác không thành công.');
    }
  };

  const openEdit = useCallback((part: Part) => {
    setEditing(part);
  }, []);

  const displayList = useMemo(() => {
    if (onlyLowStock) return lowStockParts || [];
    return data?.content || [];
  }, [onlyLowStock, lowStockParts, data]);

  const columns = useMemo<ColumnDef<Part>[]>(
    () => [
      {
        id: 'part',
        header: 'Phụ tùng',
        cell: ({ row }) => (
          <div className="account-cell">
            <span
              className="avatar"
              style={{
                background: '#e0edff',
                color: '#245bdb',
                fontSize: '11px',
                fontWeight: 700,
              }}
            >
              <Package size={16} />
            </span>
            <div>
              <strong style={{ display: 'block', color: '#172338' }}>{row.original.partName}</strong>
              <span className="muted font-mono" style={{ fontSize: '12px' }}>
                {row.original.sku}
              </span>
              {row.original.preferredSupplierName && (
                <span style={{ display: 'block', fontSize: '11px', color: '#64748b' }}>
                  NCC: {row.original.preferredSupplierName}
                </span>
              )}
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'unit',
        header: 'ĐVT',
        cell: ({ row }) => <span style={{ fontWeight: 550 }}>{row.original.unit}</span>,
      },
      {
        id: 'prices',
        header: 'Đơn giá (VNĐ)',
        cell: ({ row }) => (
          <div style={{ fontSize: '13px' }}>
            <div style={{ fontWeight: 650, color: '#172338' }}>
              {Number(row.original.sellingPrice).toLocaleString('vi-VN')} đ
            </div>
            <div className="muted" style={{ fontSize: '11px' }}>
              Vốn: {Number(row.original.purchasePrice).toLocaleString('vi-VN')} đ
            </div>
          </div>
        ),
      },
      {
        id: 'stock',
        header: 'Tồn kho & Khả dụng',
        cell: ({ row }) => {
          const stock = Number(row.original.stockQuantity || 0);
          const reserved = Number(row.original.reservedQuantity || 0);
          const available = Number(row.original.availableQuantity || 0);
          const reorder = Number(row.original.reorderLevel || 0);
          const isLow = stock <= reorder;

          return (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span
                  style={{
                    fontSize: '14px',
                    fontWeight: 700,
                    color: isLow ? '#b42335' : '#172338',
                  }}
                >
                  {available} {row.original.unit}
                </span>
                {isLow && (
                  <Badge variant="warning" className="flex items-center gap-1">
                    <AlertTriangle size={11} /> Cảnh báo
                  </Badge>
                )}
              </div>
              <div className="muted" style={{ fontSize: '11px', marginTop: '2px' }}>
                Tồn: {stock} | Giữ: {reserved} | Min: {reorder}
              </div>
            </div>
          );
        },
      },
      {
        id: 'warranty',
        header: 'Bảo hành',
        cell: ({ row }) => (
          <span className="muted" style={{ fontSize: '12px' }}>
            {row.original.warrantyDays > 0 ? `${row.original.warrantyDays} ngày` : '—'}
            {row.original.warrantyKm > 0 ? ` / ${row.original.warrantyKm} km` : ''}
          </span>
        ),
      },
      {
        accessorKey: 'isActive',
        header: 'Trạng thái',
        cell: ({ row }) => <StatusBadge active={row.original.isActive} />,
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button
              variant="ghost"
              aria-label={`Sửa phụ tùng ${row.original.partName}`}
              onClick={() => openEdit(row.original)}
            >
              <Edit2 size={16} />
              <span>Sửa</span>
            </Button>
            {row.original.isActive && (
              <Button
                variant="ghost"
                className="delete-action"
                aria-label={`Ngừng kinh doanh ${row.original.partName}`}
                onClick={() => setDeletingPart(row.original)}
              >
                <Trash2 size={16} />
                <span>Ngừng</span>
              </Button>
            )}
          </div>
        ),
      },
    ],
    [openEdit],
  );

  const totalElements = onlyLowStock
    ? (lowStockParts?.length || 0)
    : (data?.totalElements || 0);
  const totalPages = onlyLowStock ? 1 : (data?.totalPages || 1);

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ KHO & VẬT TƯ"
        title="Danh mục phụ tùng"
        description="Quản lý mã phụ tùng SKU, đơn vị tính, đơn giá, số lượng tồn kho khả dụng và cảnh báo tái đặt hàng."
        actions={
          <Button
            variant="primary"
            ref={createTrigger}
            onClick={() => setCreating(true)}
          >
            <Plus size={18} />
            <span>Thêm phụ tùng</span>
          </Button>
        }
      />

      <div className="info-strip">
        <Package size={18} />
        <span>
          Lượng khả dụng = Tồn thực tế - Lượng giữ chỗ cho các phiếu sửa chữa đang thực hiện. Tồn kho tự động cập nhật qua chứng từ nhập/xuất kho.
        </span>
      </div>

      <section className="panel table-panel">
        {/* Bộ lọc phụ tùng */}
        <div className="user-filters" role="search" aria-label="Bộ lọc phụ tùng">
          <div className="user-filters-heading">
            <div>
              <SlidersHorizontal size={16} aria-hidden />
              <h2>Bộ lọc phụ tùng</h2>
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
              gridTemplateColumns: 'minmax(220px, 1.4fr) minmax(200px, 1fr) minmax(180px, 1fr) minmax(200px, 1fr)',
            }}
          >
            {/* Tìm kiếm */}
            <div className="user-filter-field">
              <label htmlFor="part-search-input">Tìm kiếm</label>
              <div className="user-search">
                <Search size={17} aria-hidden />
                <input
                  id="part-search-input"
                  className="input"
                  placeholder="Nhập tên phụ tùng hoặc SKU"
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

            {/* Chế độ xem tồn / cảnh báo */}
            <fieldset className="user-filter-field status-filter">
              <legend>Chế độ tồn kho</legend>
              <div className="status-segments">
                <button
                  type="button"
                  className={!onlyLowStock ? 'selected' : ''}
                  onClick={() => setOnlyLowStock(false)}
                >
                  Tất cả tồn
                </button>
                <button
                  type="button"
                  className={onlyLowStock ? 'selected' : ''}
                  onClick={() => setOnlyLowStock(true)}
                  style={onlyLowStock ? { background: '#fff1f2', color: '#b42335' } : {}}
                >
                  Cảnh báo ({lowStockParts?.length || 0})
                </button>
              </div>
            </fieldset>

            {/* Nhà cung cấp */}
            <div className="user-filter-field">
              <label htmlFor="part-supplier-select">Nhà cung cấp</label>
              <select
                id="part-supplier-select"
                className="input"
                value={supplierId}
                onChange={(e) => {
                  setSupplierId(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">Tất cả nhà cung cấp</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.supplierName}
                  </option>
                ))}
              </select>
            </div>

            {/* Sắp xếp */}
            <div className="user-filter-field">
              <label htmlFor="part-sort-select">Sắp xếp theo</label>
              <select
                id="part-sort-select"
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
          <Loading label="Đang tải danh mục phụ tùng…" />
        ) : compact ? (
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {displayList.map((p) => {
              const isLow = Number(p.stockQuantity) <= Number(p.reorderLevel);
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
                      <strong style={{ fontSize: '15px' }}>{p.partName}</strong>
                      <div className="muted font-mono" style={{ fontSize: '12px' }}>{p.sku}</div>
                    </div>
                    {isLow ? (
                      <Badge variant="warning">Tồn thấp</Badge>
                    ) : (
                      <StatusBadge active={p.isActive} />
                    )}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                    <span>Giá bán: <strong>{Number(p.sellingPrice).toLocaleString('vi-VN')} đ</strong></span>
                    <span>Khả dụng: <strong style={{ color: isLow ? '#b42335' : '#245bdb' }}>{p.availableQuantity} {p.unit}</strong></span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px', borderTop: '1px solid #f0f2f5' }}>
                    <span className="muted" style={{ fontSize: '11px' }}>Tồn: {p.stockQuantity} | Giữ: {p.reservedQuantity}</span>
                    <div className="row-actions">
                      <Button variant="ghost" onClick={() => openEdit(p)}>
                        <Edit2 size={15} /> Sửa
                      </Button>
                      {p.isActive && (
                        <Button variant="ghost" className="delete-action" onClick={() => setDeletingPart(p)}>
                          <Trash2 size={15} /> Ngừng
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <DataTable
            caption="Danh sách phụ tùng"
            columns={columns}
            data={displayList}
            busy={isFetching}
          />
        )}

        {/* Phân trang */}
        <div className="pagination" style={{ borderTop: '1px solid var(--border)' }}>
          <div className="pagination-info">
            Hiển thị <strong>{displayList.length > 0 ? 1 : 0}–{displayList.length}</strong> trong <strong>{totalElements}</strong> kết quả
            <span style={{ marginLeft: '12px', color: 'var(--muted)' }}>Trang {page + 1} / {totalPages}</span>
          </div>
          <div className="pagination-controls">
            <Button
              variant="secondary"
              disabled={page === 0 || isFetching || onlyLowStock}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Trang trước
            </Button>
            <Button
              variant="secondary"
              disabled={page >= totalPages - 1 || isFetching || onlyLowStock}
              onClick={() => setPage((p) => p + 1)}
            >
              Trang sau
            </Button>
          </div>
        </div>
      </section>

      {/* Modal Thêm phụ tùng 3 cột */}
      <Modal
        open={creating}
        onOpenChange={setCreating}
        title="Thêm phụ tùng mới"
        description="Khai báo mã phụ tùng, đơn giá, định mức dự trữ và điều kiện bảo hành."
        size="lg"
      >
        <form onSubmit={handleCreate} className="modal-form grid-3col">
          <div className="form-section-label">THÔNG TIN ĐỊNH DANH & QUẢN LÝ KHO</div>

          <div className="col-span-2">
            <Field label="Tên phụ tùng" required>
              <input
                name="partName"
                className="input"
                placeholder="VD: Nhớt tổng hợp Castrol 5W-30 (Can 4L)"
                required
                autoFocus
              />
            </Field>
          </div>

          <Field label="Mã SKU" required>
            <input
              name="sku"
              className="input font-mono"
              placeholder="VD: PRT-OIL-5W30"
              required
            />
          </Field>

          <Field label="Đơn vị tính" required>
            <input name="unit" className="input" placeholder="Ví dụ: Can, Cái, Bộ..." required />
          </Field>

          <Field label="Nhà cung cấp ưu tiên">
            <select name="preferredSupplierId" className="input">
              <option value="">Chọn nhà cung cấp</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.supplierName} ({s.supplierCode})
                </option>
              ))}
            </select>
          </Field>

          <Field label="Mức tồn tối thiểu">
            <input name="reorderLevel" type="number" min="0" defaultValue="5" className="input" />
          </Field>

          <div className="form-section-label">GIÁ BÁN & CHÍNH SÁCH BẢO HÀNH</div>

          <Field label="Giá nhập dự kiến (VNĐ)">
            <input name="purchasePrice" type="number" min="0" step="1000" defaultValue="0" className="input" />
          </Field>

          <Field label="Giá bán niêm yết (VNĐ)" required>
            <input name="sellingPrice" type="number" min="0" step="1000" defaultValue="0" required className="input" />
          </Field>

          <Field label="Thời hạn bảo hành (ngày)">
            <input name="warrantyDays" type="number" min="0" defaultValue="90" className="input" />
          </Field>

          <Field label="Số km bảo hành (km)">
            <input name="warrantyKm" type="number" min="0" defaultValue="5000" className="input" />
          </Field>

          <div className="col-span-2">
            <Field label="Điều khoản bảo hành">
              <input name="warrantyTerms" className="input" placeholder="VD: Bảo hành 1 đổi 1 nếu lỗi từ nhà sản xuất" />
            </Field>
          </div>

          <div className="col-span-3">
            <Field label="Mô tả kỹ thuật / ứng dụng xe">
              <input
                name="description"
                className="input"
                placeholder="Ghi chú tương thích các dòng xe, lưu ý khi lắp ráp..."
              />
            </Field>
          </div>

          <div className="modal-form-actions col-span-3">
            <Button variant="secondary" onClick={() => setCreating(false)} disabled={createMutation.isPending}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={createMutation.isPending}>
              Thêm phụ tùng
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Sửa phụ tùng 3 cột */}
      <Modal
        open={!!editing}
        onOpenChange={(open) => !open && setEditing(null)}
        title="Cập nhật phụ tùng"
        description={`Chỉnh sửa thông tin phụ tùng ${editing?.sku}`}
        size="lg"
      >
        {editing && (
          <form onSubmit={handleUpdate} className="modal-form grid-3col">
            <div className="form-section-label">THÔNG TIN ĐỊNH DANH & QUẢN LÝ KHO</div>

            <div className="col-span-2">
              <Field label="Tên phụ tùng" required>
                <input
                  name="partName"
                  className="input"
                  defaultValue={editing.partName}
                  required
                  autoFocus
                />
              </Field>
            </div>

            <Field label="Mã SKU">
              <input
                name="sku"
                className="input font-mono"
                defaultValue={editing.sku}
                readOnly
                disabled
              />
            </Field>

            <Field label="Đơn vị tính" required>
              <input name="unit" className="input" defaultValue={editing.unit} required />
            </Field>

            <Field label="Nhà cung cấp ưu tiên">
              <select
                name="preferredSupplierId"
                className="input"
                defaultValue={editing.preferredSupplierId || ''}
              >
                <option value="">Chọn nhà cung cấp</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.supplierName} ({s.supplierCode})
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Mức tồn tối thiểu">
              <input
                name="reorderLevel"
                type="number"
                min="0"
                defaultValue={editing.reorderLevel}
                className="input"
              />
            </Field>

            <div className="form-section-label">GIÁ BÁN & CHÍNH SÁCH BẢO HÀNH</div>

            <Field label="Giá nhập tham chiếu (VNĐ)">
              <input
                name="purchasePrice"
                type="number"
                min="0"
                step="1000"
                defaultValue={editing.purchasePrice}
                className="input"
              />
            </Field>

            <Field label="Giá bán niêm yết (VNĐ)" required>
              <input
                name="sellingPrice"
                type="number"
                min="0"
                step="1000"
                defaultValue={editing.sellingPrice}
                required
                className="input"
              />
            </Field>

            <Field label="Thời hạn bảo hành (ngày)">
              <input
                name="warrantyDays"
                type="number"
                min="0"
                defaultValue={editing.warrantyDays}
                className="input"
              />
            </Field>

            <Field label="Số km bảo hành (km)">
              <input
                name="warrantyKm"
                type="number"
                min="0"
                defaultValue={editing.warrantyKm}
                className="input"
              />
            </Field>

            <div className="col-span-2">
              <Field label="Điều khoản bảo hành">
                <input
                  name="warrantyTerms"
                  className="input"
                  defaultValue={editing.warrantyTerms || ''}
                  placeholder="VD: Bảo hành 1 đổi 1..."
                />
              </Field>
            </div>

            <Field label="Trạng thái kinh doanh">
              <select name="isActive" className="input" defaultValue={String(editing.isActive)}>
                <option value="true">Đang kinh doanh (Hoạt động)</option>
                <option value="false">Ngừng kinh doanh</option>
              </select>
            </Field>

            <div className="col-span-2">
              <Field label="Mô tả kỹ thuật">
                <input
                  name="description"
                  className="input"
                  defaultValue={editing.description || ''}
                  placeholder="Ghi chú tương thích các dòng xe..."
                />
              </Field>
            </div>

            <div className="modal-form-actions col-span-3">
              <Button variant="secondary" onClick={() => setEditing(null)} disabled={updateMutation.isPending}>
                Hủy
              </Button>
              <Button variant="primary" type="submit" pending={updateMutation.isPending}>
                Lưu thay đổi
              </Button>
            </div>
          </form>
        )}
      </Modal>

      {/* ConfirmDialog ngừng kinh doanh */}
      <ConfirmDialog
        open={!!deletingPart}
        title="Ngừng kinh doanh phụ tùng?"
        description={`Chuyển phụ tùng "${deletingPart?.partName}" sang trạng thái ngừng kinh doanh. Phụ tùng này sẽ không xuất hiện khi tạo báo giá hoặc phiếu sửa chữa mới, nhưng lịch sử tồn kho và giao dịch được giữ nguyên.`}
        confirmLabel="Ngừng kinh doanh"
        danger
        pending={deactivateMutation.isPending}
        onCancel={() => setDeletingPart(null)}
        onConfirm={handleDeactivate}
      />
    </>
  );
}
