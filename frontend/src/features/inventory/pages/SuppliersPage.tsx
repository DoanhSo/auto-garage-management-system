import { useState } from 'react';
import {
  useSuppliersQuery,
  useCreateSupplierMutation,
  useUpdateSupplierMutation,
  useDeactivateSupplierMutation,
} from '../hooks/useInventory';
import type { Supplier, SupplierCreateInput, SupplierUpdateInput } from '../types/inventory';
import { Button, Field, Modal, ConfirmModal, Badge, Loading } from '../../../shared/components/ui';
import { Plus, Search, Edit2, Trash2, Building2, Phone, Mail } from 'lucide-react';

export function SuppliersPage() {
  const [query, setQuery] = useState('');
  const [active, setActive] = useState<boolean | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [creating, setCreating] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const { data, isLoading } = useSuppliersQuery({
    query: query || undefined,
    active,
    page,
    size: 20,
    sortBy: 'id',
    sortDir: 'desc',
  });

  const createMutation = useCreateSupplierMutation();
  const updateMutation = useUpdateSupplierMutation();
  const deactivateMutation = useDeactivateSupplierMutation();

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: SupplierCreateInput = {
      supplierCode: formData.get('supplierCode') as string,
      supplierName: formData.get('supplierName') as string,
      phone: (formData.get('phone') as string) || undefined,
      email: (formData.get('email') as string) || undefined,
      address: (formData.get('address') as string) || undefined,
      taxCode: (formData.get('taxCode') as string) || undefined,
      paymentTermDays: Number(formData.get('paymentTermDays') || 0),
    };
    await createMutation.mutateAsync(input);
    setCreating(false);
  };

  const handleUpdate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editing) return;
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: SupplierUpdateInput = {
      supplierName: formData.get('supplierName') as string,
      phone: (formData.get('phone') as string) || undefined,
      email: (formData.get('email') as string) || undefined,
      address: (formData.get('address') as string) || undefined,
      taxCode: (formData.get('taxCode') as string) || undefined,
      paymentTermDays: Number(formData.get('paymentTermDays') || 0),
      isActive: formData.get('isActive') === 'true',
      version: editing.version,
    };
    await updateMutation.mutateAsync({ id: editing.id, data: input });
    setEditing(null);
  };

  const handleDeactivate = async () => {
    if (!deletingId) return;
    await deactivateMutation.mutateAsync(deletingId);
    setDeletingId(null);
  };

  return (
    <div className="page-container">
      <div className="page-header flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Building2 className="text-blue-600" size={28} />
            Quản lý Nhà cung cấp
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            Danh bạ đối tác cung cấp phụ tùng và vật tư sửa chữa
          </p>
        </div>
        <Button variant="primary" onClick={() => setCreating(true)}>
          <Plus size={18} />
          <span>Thêm nhà cung cấp</span>
        </Button>
      </div>

      <div className="filter-bar flex gap-4 mb-6 bg-white p-4 rounded-xl border border-gray-200">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-2.5 text-gray-400" size={18} />
          <input
            type="text"
            className="input pl-10 w-full"
            placeholder="Tìm theo tên, mã NCC, SĐT hoặc mã số thuế..."
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
          />
        </div>
        <select
          className="input w-48"
          value={active === undefined ? '' : String(active)}
          onChange={(e) => {
            setActive(e.target.value === '' ? undefined : e.target.value === 'true');
            setPage(0);
          }}
        >
          <option value="">Tất cả trạng thái</option>
          <option value="true">Đang hoạt động</option>
          <option value="false">Ngừng hợp tác</option>
        </select>
      </div>

      {isLoading ? (
        <Loading label="Đang tải danh sách nhà cung cấp..." />
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
          <table className="data-table w-full">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                <th className="py-3 px-4">Mã NCC</th>
                <th className="py-3 px-4">Tên nhà cung cấp</th>
                <th className="py-3 px-4">Liên hệ</th>
                <th className="py-3 px-4">MST / Địa chỉ</th>
                <th className="py-3 px-4">Hạn nợ</th>
                <th className="py-3 px-4">Trạng thái</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 text-sm">
              {data?.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-gray-500">
                    Không tìm thấy nhà cung cấp nào.
                  </td>
                </tr>
              ) : (
                data?.content.map((supplier) => (
                  <tr key={supplier.id} className="hover:bg-gray-50 transition-colors">
                    <td className="py-3 px-4 font-mono font-medium text-blue-600">
                      {supplier.supplierCode}
                    </td>
                    <td className="py-3 px-4 font-semibold text-gray-900">
                      {supplier.supplierName}
                    </td>
                    <td className="py-3 px-4">
                      {supplier.phone && (
                        <div className="flex items-center gap-1.5 text-gray-700">
                          <Phone size={14} className="text-gray-400" />
                          <span>{supplier.phone}</span>
                        </div>
                      )}
                      {supplier.email && (
                        <div className="flex items-center gap-1.5 text-gray-500 text-xs mt-0.5">
                          <Mail size={14} className="text-gray-400" />
                          <span>{supplier.email}</span>
                        </div>
                      )}
                    </td>
                    <td className="py-3 px-4 text-xs text-gray-600">
                      {supplier.taxCode && <div>MST: {supplier.taxCode}</div>}
                      <div className="truncate max-w-xs">{supplier.address || '—'}</div>
                    </td>
                    <td className="py-3 px-4 font-medium text-gray-700">
                      {supplier.paymentTermDays} ngày
                    </td>
                    <td className="py-3 px-4">
                      {supplier.isActive ? (
                        <Badge variant="success">Hoạt động</Badge>
                      ) : (
                        <Badge variant="secondary">Ngừng</Badge>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right space-x-2">
                      <Button
                        variant="ghost"
                        onClick={() => setEditing(supplier)}
                        aria-label="Sửa thông tin"
                      >
                        <Edit2 size={16} />
                      </Button>
                      {supplier.isActive && (
                        <Button
                          variant="ghost"
                          onClick={() => setDeletingId(supplier.id)}
                          aria-label="Ngừng hợp tác"
                        >
                          <Trash2 size={16} className="text-red-500" />
                        </Button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          {data && data.totalPages > 1 && (
            <div className="flex justify-between items-center p-4 border-t border-gray-200">
              <span className="text-sm text-gray-500">
                Trang {page + 1} / {data.totalPages} ({data.totalElements} nhà cung cấp)
              </span>
              <div className="space-x-2">
                <Button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Trước
                </Button>
                <Button
                  disabled={page >= data.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Sau
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modal Thêm NCC */}
      <Modal
        open={creating}
        onOpenChange={setCreating}
        title="Thêm nhà cung cấp mới"
        description="Nhập thông tin cơ bản của nhà cung cấp phụ tùng."
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Field label="Mã nhà cung cấp" required>
            <input name="supplierCode" className="input" placeholder="Ví dụ: SUP-BOSCH" required />
          </Field>
          <Field label="Tên nhà cung cấp" required>
            <input name="supplierName" className="input" placeholder="Ví dụ: Bosch Vietnam Co." required />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Số điện thoại">
              <input name="phone" className="input" placeholder="0901234567" />
            </Field>
            <Field label="Email">
              <input name="email" type="email" className="input" placeholder="ncc@example.com" />
            </Field>
          </div>
          <Field label="Địa chỉ">
            <input name="address" className="input" placeholder="Địa chỉ trụ sở / kho" />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Mã số thuế">
              <input name="taxCode" className="input" placeholder="0300123456" />
            </Field>
            <Field label="Thời hạn công nợ (ngày)">
              <input name="paymentTermDays" type="number" min="0" defaultValue="30" className="input" />
            </Field>
          </div>
          <div className="modal-actions flex justify-end gap-3 pt-4 border-t border-gray-100">
            <Button variant="secondary" onClick={() => setCreating(false)}>Hủy</Button>
            <Button variant="primary" type="submit" pending={createMutation.isPending}>
              Lưu nhà cung cấp
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Sửa NCC */}
      <Modal
        open={!!editing}
        onOpenChange={(open: boolean) => !open && setEditing(null)}
        title="Cập nhật nhà cung cấp"
        description={`Sửa thông tin cho mã: ${editing?.supplierCode}`}
      >
        {editing && (
          <form onSubmit={handleUpdate} className="space-y-4">
            <Field label="Tên nhà cung cấp" required>
              <input name="supplierName" className="input" defaultValue={editing.supplierName} required />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Số điện thoại">
                <input name="phone" className="input" defaultValue={editing.phone || ''} />
              </Field>
              <Field label="Email">
                <input name="email" type="email" className="input" defaultValue={editing.email || ''} />
              </Field>
            </div>
            <Field label="Địa chỉ">
              <input name="address" className="input" defaultValue={editing.address || ''} />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Mã số thuế">
                <input name="taxCode" className="input" defaultValue={editing.taxCode || ''} />
              </Field>
              <Field label="Thời hạn công nợ (ngày)">
                <input name="paymentTermDays" type="number" min="0" defaultValue={editing.paymentTermDays} className="input" />
              </Field>
            </div>
            <Field label="Trạng thái">
              <select name="isActive" className="input" defaultValue={String(editing.isActive)}>
                <option value="true">Đang hoạt động</option>
                <option value="false">Ngừng hợp tác</option>
              </select>
            </Field>
            <div className="modal-actions flex justify-end gap-3 pt-4 border-t border-gray-100">
              <Button variant="secondary" onClick={() => setEditing(null)}>Hủy</Button>
              <Button variant="primary" type="submit" pending={updateMutation.isPending}>
                Cập nhật
              </Button>
            </div>
          </form>
        )}
      </Modal>

      {/* Modal Xác nhận ngừng hợp tác */}
      <ConfirmModal
        open={!!deletingId}
        onOpenChange={(open: boolean) => !open && setDeletingId(null)}
        title="Ngừng hợp tác nhà cung cấp?"
        description="Nhà cung cấp sẽ được chuyển sang trạng thái ngừng hợp tác và không xuất hiện khi lập đơn nhập hàng mới."
        confirmLabel="Ngừng hợp tác"
        variant="danger"
        pending={deactivateMutation.isPending}
        onConfirm={handleDeactivate}
      />
    </div>
  );
}
