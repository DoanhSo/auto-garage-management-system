import { useCallback, useMemo, useRef, useState } from 'react';
import {
  Building2,
  Edit2,
  Mail,
  Phone,
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
import { initials } from '../../../shared/utils/format';
import {
  useSuppliersQuery,
  useCreateSupplierMutation,
  useUpdateSupplierMutation,
  useDeactivateSupplierMutation,
} from '../hooks/useInventory';
import type { Supplier, SupplierCreateInput, SupplierUpdateInput } from '../types/inventory';

const sortOptions = [
  ['id:desc', 'Mã NCC · Giảm dần'],
  ['id:asc', 'Mã NCC · Tăng dần'],
  ['supplierName:asc', 'Tên nhà cung cấp · A → Z'],
  ['supplierName:desc', 'Tên nhà cung cấp · Z → A'],
  ['paymentTermDays:asc', 'Hạn nợ · Tăng dần'],
  ['paymentTermDays:desc', 'Hạn nợ · Giảm dần'],
];

export function SuppliersPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();

  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query);
  const [active, setActive] = useState<boolean | undefined>(undefined);
  const [sortField, setSortField] = useState('id');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(0);

  const [editing, setEditing] = useState<Supplier | null>(null);
  const [creating, setCreating] = useState(false);
  const [deletingSupplier, setDeletingSupplier] = useState<Supplier | null>(null);

  const createTrigger = useRef<HTMLButtonElement>(null);

  const { data, isLoading, isFetching } = useSuppliersQuery({
    query: debouncedQuery.trim() || undefined,
    active,
    page,
    size: 20,
    sortBy: sortField,
    sortDir,
  });

  const createMutation = useCreateSupplierMutation();
  const updateMutation = useUpdateSupplierMutation();
  const deactivateMutation = useDeactivateSupplierMutation();

  const filterCount = (query.trim() ? 1 : 0) + (active !== undefined ? 1 : 0);
  const isFiltered = query.length > 0 || active !== undefined || sortField !== 'id' || sortDir !== 'desc';

  const handleResetFilters = () => {
    setQuery('');
    setActive(undefined);
    setSortField('id');
    setSortDir('desc');
    setPage(0);
  };

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: SupplierCreateInput = {
      supplierCode: (formData.get('supplierCode') as string)?.trim().toUpperCase(),
      supplierName: (formData.get('supplierName') as string)?.trim(),
      phone: (formData.get('phone') as string)?.trim() || undefined,
      email: (formData.get('email') as string)?.trim() || undefined,
      address: (formData.get('address') as string)?.trim() || undefined,
      taxCode: (formData.get('taxCode') as string)?.trim() || undefined,
      paymentTermDays: Number(formData.get('paymentTermDays') || 0),
    };

    try {
      await createMutation.mutateAsync(input);
      notify('Đã thêm nhà cung cấp thành công.');
      setCreating(false);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Không thể lưu nhà cung cấp.');
    }
  };

  const handleUpdate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editing) return;
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: SupplierUpdateInput = {
      supplierName: (formData.get('supplierName') as string)?.trim(),
      phone: (formData.get('phone') as string)?.trim() || undefined,
      email: (formData.get('email') as string)?.trim() || undefined,
      address: (formData.get('address') as string)?.trim() || undefined,
      taxCode: (formData.get('taxCode') as string)?.trim() || undefined,
      paymentTermDays: Number(formData.get('paymentTermDays') || 0),
      isActive: formData.get('isActive') === 'true',
      version: editing.version,
    };

    try {
      await updateMutation.mutateAsync({ id: editing.id, data: input });
      notify('Đã cập nhật nhà cung cấp.');
      setEditing(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Cập nhật thất bại.');
    }
  };

  const handleDeactivate = async () => {
    if (!deletingSupplier) return;
    try {
      await deactivateMutation.mutateAsync(deletingSupplier.id);
      notify(`Đã chuyển nhà cung cấp ${deletingSupplier.supplierName} sang ngừng hợp tác.`);
      setDeletingSupplier(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Thao tác không thành công.');
    }
  };

  const openEdit = useCallback((supplier: Supplier) => {
    setEditing(supplier);
  }, []);

  const columns = useMemo<ColumnDef<Supplier>[]>(
    () => [
      {
        id: 'supplier',
        header: 'Nhà cung cấp',
        cell: ({ row }) => (
          <div className="account-cell">
            <span className="avatar">{initials(row.original.supplierName)}</span>
            <div>
              <strong>{row.original.supplierName}</strong>
              <span className="muted font-mono">{row.original.supplierCode}</span>
            </div>
          </div>
        ),
      },
      {
        id: 'contact',
        header: 'Liên hệ',
        cell: ({ row }) => (
          <div className="text-sm">
            {row.original.phone && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#172338' }}>
                <Phone size={14} className="muted" />
                <span>{row.original.phone}</span>
              </div>
            )}
            {row.original.email && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#526278', fontSize: '12px', marginTop: '2px' }}>
                <Mail size={14} className="muted" />
                <span>{row.original.email}</span>
              </div>
            )}
            {!row.original.phone && !row.original.email && <span className="muted">Chưa cung cấp</span>}
          </div>
        ),
      },
      {
        id: 'taxAndAddress',
        header: 'MST / Địa chỉ',
        cell: ({ row }) => (
          <div style={{ fontSize: '13px' }}>
            {row.original.taxCode && (
              <div style={{ fontWeight: 550, color: '#36485f' }}>MST: {row.original.taxCode}</div>
            )}
            <div className="muted" style={{ maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {row.original.address || '—'}
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'paymentTermDays',
        header: 'Hạn nợ',
        cell: ({ row }) => (
          <span style={{ fontWeight: 600, color: '#172338' }}>
            {row.original.paymentTermDays} ngày
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
              aria-label={`Sửa nhà cung cấp ${row.original.supplierName}`}
              onClick={() => openEdit(row.original)}
            >
              <Edit2 size={16} />
              <span>Sửa</span>
            </Button>
            {row.original.isActive && (
              <Button
                variant="ghost"
                className="delete-action"
                aria-label={`Ngừng hợp tác ${row.original.supplierName}`}
                onClick={() => setDeletingSupplier(row.original)}
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

  const suppliersList = data?.content || [];
  const totalElements = data?.totalElements || 0;
  const totalPages = data?.totalPages || 1;

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ KHO & VẬT TƯ"
        title="Nhà cung cấp"
        description="Quản lý danh bạ đối tác cung cấp phụ tùng, chính sách công nợ và thông tin xuất hóa đơn."
        actions={
          <Button
            variant="primary"
            ref={createTrigger}
            onClick={() => setCreating(true)}
          >
            <Plus size={18} />
            <span>Thêm nhà cung cấp</span>
          </Button>
        }
      />

      <div className="info-strip">
        <Building2 size={18} />
        <span>
          Mã nhà cung cấp duy nhất trên toàn hệ thống. Đối tác đã phát sinh chứng từ nhập kho sẽ chuyển sang ngừng hợp tác thay vì xóa dữ liệu.
        </span>
      </div>

      <section className="panel table-panel">
        {/* Bộ lọc chuẩn giao diện */}
        <div className="user-filters" role="search" aria-label="Bộ lọc nhà cung cấp">
          <div className="user-filters-heading">
            <div>
              <SlidersHorizontal size={16} aria-hidden />
              <h2>Bộ lọc nhà cung cấp</h2>
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

          <div className="user-filter-grid">
            {/* Ô tìm kiếm */}
            <div className="user-filter-field">
              <label htmlFor="supplier-search-input">Tìm kiếm</label>
              <div className="user-search">
                <Search size={17} aria-hidden />
                <input
                  id="supplier-search-input"
                  className="input"
                  placeholder="Nhập tên, mã NCC, SĐT hoặc mã số thuế"
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

            {/* Trạng thái Segmented control */}
            <fieldset className="user-filter-field status-filter">
              <legend>Trạng thái</legend>
              <div className="status-segments">
                {[
                  { value: undefined, label: 'Tất cả' },
                  { value: true, label: 'Hoạt động' },
                  { value: false, label: 'Ngừng' },
                ].map((opt) => (
                  <button
                    key={opt.label}
                    type="button"
                    className={active === opt.value ? 'selected' : ''}
                    onClick={() => {
                      setActive(opt.value);
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
              <label htmlFor="supplier-sort-select">Sắp xếp theo</label>
              <select
                id="supplier-sort-select"
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
          <Loading label="Đang tải danh sách nhà cung cấp…" />
        ) : compact ? (
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {suppliersList.map((s) => (
              <div
                key={s.id}
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
                    <strong style={{ fontSize: '15px' }}>{s.supplierName}</strong>
                    <div className="muted font-mono" style={{ fontSize: '12px' }}>{s.supplierCode}</div>
                  </div>
                  <StatusBadge active={s.isActive} />
                </div>
                {s.phone && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px' }}>
                    <Phone size={13} className="muted" /> <span>{s.phone}</span>
                  </div>
                )}
                {s.email && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px' }} className="muted">
                    <Mail size={13} /> <span>{s.email}</span>
                  </div>
                )}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px', borderTop: '1px solid #f0f2f5' }}>
                  <span style={{ fontSize: '12px', color: '#526278' }}>Hạn nợ: <strong>{s.paymentTermDays} ngày</strong></span>
                  <div className="row-actions">
                    <Button variant="ghost" onClick={() => openEdit(s)}>
                      <Edit2 size={15} /> Sửa
                    </Button>
                    {s.isActive && (
                      <Button variant="ghost" className="delete-action" onClick={() => setDeletingSupplier(s)}>
                        <Trash2 size={15} /> Ngừng
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <DataTable
            caption="Danh sách nhà cung cấp"
            columns={columns}
            data={suppliersList}
            busy={isFetching}
          />
        )}

        {/* Thanh phân trang chuẩn */}
        <div className="pagination" style={{ borderTop: '1px solid var(--border)' }}>
          <div className="pagination-info">
            Hiển thị <strong>{suppliersList.length > 0 ? 1 : 0}–{suppliersList.length}</strong> trong <strong>{totalElements}</strong> kết quả
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

      {/* Modal Thêm nhà cung cấp chuẩn 2 cột */}
      <Modal
        open={creating}
        onOpenChange={setCreating}
        title="Thêm nhà cung cấp"
        description="Khai báo đối tác cung ứng phụ tùng mới và điều khoản công nợ."
      >
        <form onSubmit={handleCreate} className="modal-form">
          <div className="form-section-label">THÔNG TIN DOANH NGHIỆP</div>

          <Field label="Tên nhà cung cấp" required>
            <input
              name="supplierName"
              className="input"
              placeholder="VD: Bosch Vietnam Co."
              required
              autoFocus
            />
          </Field>

          <Field label="Mã nhà cung cấp" required hint="3–30 ký tự, viết hoa (VD: SUP-BOSCH)">
            <input
              name="supplierCode"
              className="input font-mono"
              placeholder="VD: SUP-BOSCH"
              required
            />
          </Field>

          <Field label="Số điện thoại" hint="Số hotline hoặc bàn">
            <input name="phone" className="input" placeholder="028 3838 3838" />
          </Field>

          <Field label="Email liên hệ">
            <input name="email" type="email" className="input" placeholder="sales@ncc.vn" />
          </Field>

          <Field label="Mã số thuế" hint="Dùng khi xuất hóa đơn đầu vào">
            <input name="taxCode" className="input" placeholder="0300123456" />
          </Field>

          <Field label="Thời hạn công nợ (ngày)" hint="Số ngày được phép trả chậm sau khi nhận hàng">
            <input
              name="paymentTermDays"
              type="number"
              min="0"
              max="365"
              defaultValue="30"
              className="input"
            />
          </Field>

          <Field label="Địa chỉ trụ sở / kho hàng" className="full-width">
            <input name="address" className="input" placeholder="Số nhà, đường, quận/huyện, tỉnh/thành phố" />
          </Field>

          <div className="modal-form-actions">
            <Button variant="secondary" onClick={() => setCreating(false)} disabled={createMutation.isPending}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={createMutation.isPending}>
              Thêm nhà cung cấp
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Sửa nhà cung cấp chuẩn 2 cột */}
      <Modal
        open={!!editing}
        onOpenChange={(open) => !open && setEditing(null)}
        title="Cập nhật nhà cung cấp"
        description={`Chỉnh sửa thông tin đối tác ${editing?.supplierCode}`}
      >
        {editing && (
          <form onSubmit={handleUpdate} className="modal-form">
            <div className="form-section-label">THÔNG TIN DOANH NGHIỆP</div>

            <Field label="Tên nhà cung cấp" required>
              <input
                name="supplierName"
                className="input"
                defaultValue={editing.supplierName}
                required
                autoFocus
              />
            </Field>

            <Field label="Mã nhà cung cấp" hint="Mã định danh không thay đổi">
              <input
                name="supplierCode"
                className="input font-mono"
                defaultValue={editing.supplierCode}
                readOnly
                disabled
              />
            </Field>

            <Field label="Số điện thoại">
              <input name="phone" className="input" defaultValue={editing.phone || ''} />
            </Field>

            <Field label="Email liên hệ">
              <input name="email" type="email" className="input" defaultValue={editing.email || ''} />
            </Field>

            <Field label="Mã số thuế">
              <input name="taxCode" className="input" defaultValue={editing.taxCode || ''} />
            </Field>

            <Field label="Thời hạn công nợ (ngày)">
              <input
                name="paymentTermDays"
                type="number"
                min="0"
                max="365"
                defaultValue={editing.paymentTermDays}
                className="input"
              />
            </Field>

            <Field label="Địa chỉ trụ sở / kho" className="full-width">
              <input name="address" className="input" defaultValue={editing.address || ''} />
            </Field>

            <Field label="Trạng thái hợp tác" className="full-width">
              <select name="isActive" className="input" defaultValue={String(editing.isActive)}>
                <option value="true">Đang hợp tác (Hoạt động)</option>
                <option value="false">Ngừng hợp tác</option>
              </select>
            </Field>

            <div className="modal-form-actions">
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

      {/* ConfirmDialog ngừng hợp tác */}
      <ConfirmDialog
        open={!!deletingSupplier}
        title="Ngừng hợp tác nhà cung cấp?"
        description={`Chuyển đối tác "${deletingSupplier?.supplierName}" sang trạng thái ngừng hợp tác. Nhà cung cấp này sẽ không hiển thị khi tạo chứng từ nhập kho mới nhưng lịch sử giao dịch vẫn được giữ nguyên để đối soát.`}
        confirmLabel="Ngừng hợp tác"
        danger
        pending={deactivateMutation.isPending}
        onCancel={() => setDeletingSupplier(null)}
        onConfirm={handleDeactivate}
      />
    </>
  );
}
