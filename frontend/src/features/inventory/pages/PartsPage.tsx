import { useState } from 'react';
import {
  usePartsQuery,
  useLowStockPartsQuery,
  useSuppliersQuery,
  useCreatePartMutation,
  useUpdatePartMutation,
  useDeactivatePartMutation,
} from '../hooks/useInventory';
import type { Part, PartCreateInput, PartUpdateInput } from '../types/inventory';
import { Button, Field, Modal, ConfirmModal, Badge, Loading } from '../../../shared/components/ui';
import { Plus, Search, Edit2, Trash2, Package, AlertTriangle } from 'lucide-react';

export function PartsPage() {
  const [query, setQuery] = useState('');
  const [supplierId, setSupplierId] = useState<string>('');
  const [onlyLowStock, setOnlyLowStock] = useState(false);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Part | null>(null);
  const [creating, setCreating] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const { data: suppliersData } = useSuppliersQuery({ size: 100, active: true });
  const suppliers = suppliersData?.content || [];

  const { data, isLoading } = usePartsQuery({
    query: query || undefined,
    supplierId: supplierId ? Number(supplierId) : undefined,
    page,
    size: 20,
    sortBy: 'id',
    sortDir: 'desc',
  });

  const { data: lowStockParts, isLoading: isLowStockLoading } = useLowStockPartsQuery();

  const createMutation = useCreatePartMutation();
  const updateMutation = useUpdatePartMutation();
  const deactivateMutation = useDeactivatePartMutation();

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: PartCreateInput = {
      sku: formData.get('sku') as string,
      partName: formData.get('partName') as string,
      unit: formData.get('unit') as string,
      description: (formData.get('description') as string) || undefined,
      preferredSupplierId: formData.get('preferredSupplierId')
        ? Number(formData.get('preferredSupplierId'))
        : undefined,
      purchasePrice: Number(formData.get('purchasePrice') || 0),
      sellingPrice: Number(formData.get('sellingPrice') || 0),
      reorderLevel: Number(formData.get('reorderLevel') || 0),
      warrantyDays: Number(formData.get('warrantyDays') || 0),
      warrantyKm: Number(formData.get('warrantyKm') || 0),
      warrantyTerms: (formData.get('warrantyTerms') as string) || undefined,
    };
    await createMutation.mutateAsync(input);
    setCreating(false);
  };

  const handleUpdate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editing) return;
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: PartUpdateInput = {
      partName: formData.get('partName') as string,
      unit: formData.get('unit') as string,
      description: (formData.get('description') as string) || undefined,
      preferredSupplierId: formData.get('preferredSupplierId')
        ? Number(formData.get('preferredSupplierId'))
        : undefined,
      purchasePrice: Number(formData.get('purchasePrice') || 0),
      sellingPrice: Number(formData.get('sellingPrice') || 0),
      reorderLevel: Number(formData.get('reorderLevel') || 0),
      warrantyDays: Number(formData.get('warrantyDays') || 0),
      warrantyKm: Number(formData.get('warrantyKm') || 0),
      warrantyTerms: (formData.get('warrantyTerms') as string) || undefined,
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

  const displayList = onlyLowStock ? (lowStockParts || []) : (data?.content || []);

  return (
    <div className="page-container p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Package className="text-blue-600" size={28} />
            Danh mục Phụ tùng & Tồn kho
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Quản lý mã phụ tùng, định mức tồn tối thiểu và kiểm soát cảnh báo hết hàng
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant={onlyLowStock ? 'primary' : 'secondary'}
            onClick={() => setOnlyLowStock(!onlyLowStock)}
            className="flex items-center gap-1.5"
          >
            <AlertTriangle size={16} className={onlyLowStock ? 'text-white' : 'text-amber-500'} />
            Cảnh báo tồn thấp ({lowStockParts?.length || 0})
          </Button>
          <Button variant="primary" onClick={() => setCreating(true)} className="flex items-center gap-1.5">
            <Plus size={16} />
            Thêm phụ tùng mới
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      {!onlyLowStock && (
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex flex-wrap gap-4 items-center justify-between">
          <div className="flex items-center gap-3 flex-1 min-w-[280px]">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
              <input
                type="text"
                placeholder="Tìm theo SKU, tên phụ tùng..."
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setPage(0);
                }}
                className="w-full pl-9 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <select
              value={supplierId}
              onChange={(e) => {
                setSupplierId(e.target.value);
                setPage(0);
              }}
              className="border rounded-lg px-3 py-2 bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Tất cả nhà cung cấp</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.supplierName}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

      {/* Data Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        {isLoading || (onlyLowStock && isLowStockLoading) ? (
          <div className="py-12 flex justify-center">
            <Loading />
          </div>
        ) : displayList.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <Package size={48} className="mx-auto text-gray-300 mb-2" />
            <p>Không tìm thấy phụ tùng nào</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-600 uppercase">
                  <th className="py-3 px-4">Mã SKU</th>
                  <th className="py-3 px-4">Tên phụ tùng</th>
                  <th className="py-3 px-4">Đơn vị</th>
                  <th className="py-3 px-4 text-right">Giá nhập (VNĐ)</th>
                  <th className="py-3 px-4 text-right">Giá bán (VNĐ)</th>
                  <th className="py-3 px-4 text-right">Tồn kho</th>
                  <th className="py-3 px-4 text-right">Khả dụng</th>
                  <th className="py-3 px-4 text-right">Mức tối thiểu</th>
                  <th className="py-3 px-4 text-center">Trạng thái</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {displayList.map((part) => {
                  const isLow = Number(part.stockQuantity) <= Number(part.reorderLevel);
                  return (
                    <tr key={part.id} className="hover:bg-gray-50/80 transition-colors">
                      <td className="py-3 px-4 font-mono font-medium text-gray-900">{part.sku}</td>
                      <td className="py-3 px-4 font-medium text-gray-800">
                        {part.partName}
                        {part.preferredSupplierName && (
                          <span className="block text-xs text-gray-400 font-normal">
                            NCC: {part.preferredSupplierName}
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-gray-600">{part.unit}</td>
                      <td className="py-3 px-4 text-right text-gray-600">
                        {Number(part.purchasePrice).toLocaleString('vi-VN')}
                      </td>
                      <td className="py-3 px-4 text-right font-medium text-blue-600">
                        {Number(part.sellingPrice).toLocaleString('vi-VN')}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <span
                          className={`font-semibold ${
                            isLow ? 'text-red-600 flex items-center justify-end gap-1' : 'text-gray-800'
                          }`}
                        >
                          {isLow && <AlertTriangle size={14} className="text-red-500" />}
                          {part.stockQuantity}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right text-gray-700 font-medium">
                        {part.availableQuantity}
                      </td>
                      <td className="py-3 px-4 text-right text-gray-500">{part.reorderLevel}</td>
                      <td className="py-3 px-4 text-center">
                        {part.isActive ? (
                          <Badge variant="success">Hoạt động</Badge>
                        ) : (
                          <Badge variant="secondary">Ngừng</Badge>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => setEditing(part)}
                            className="p-1.5 text-gray-500 hover:text-blue-600 rounded-lg hover:bg-blue-50 transition-colors"
                            title="Chỉnh sửa"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            onClick={() => setDeletingId(String(part.id))}
                            className="p-1.5 text-gray-500 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors"
                            title="Ngừng hoạt động"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {!onlyLowStock && data && data.totalPages > 1 && (
          <div className="flex justify-between items-center px-4 py-3 bg-gray-50 border-t border-gray-100">
            <span className="text-xs text-gray-500">
              Trang {page + 1} / {data.totalPages} (Tổng {data.totalElements} phụ tùng)
            </span>
            <div className="flex gap-2">
              <Button
                variant="ghost"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Trước
              </Button>
              <Button
                variant="ghost"
                disabled={page >= data.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Sau
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Modal Thêm mới */}
      <Modal open={creating} onOpenChange={setCreating} title="Khai báo Phụ tùng Mới">
        <form onSubmit={handleCreate} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Field label="Mã SKU" required>
              <input
                name="sku"
                required
                className="w-full border rounded-lg px-3 py-2 uppercase"
                placeholder="VD: OIL-SYN-5W30"
              />
            </Field>
            <Field label="Đơn vị tính" required>
              <input
                name="unit"
                required
                className="w-full border rounded-lg px-3 py-2"
                placeholder="VD: Chai, Cái, Bộ"
              />
            </Field>
          </div>

          <Field label="Tên phụ tùng" required>
            <input
              name="partName"
              required
              className="w-full border rounded-lg px-3 py-2"
              placeholder="VD: Dầu động cơ Castrol EDGE 5W-30 4L"
            />
          </Field>

          <Field label="Nhà cung cấp ưu tiên">
            <select name="preferredSupplierId" className="w-full border rounded-lg px-3 py-2 bg-white">
              <option value="">-- Chưa chỉ định --</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.supplierName} ({s.supplierCode})
                </option>
              ))}
            </select>
          </Field>

          <div className="grid grid-cols-3 gap-4">
            <Field label="Giá nhập (VNĐ)" required>
              <input
                type="number"
                name="purchasePrice"
                required
                min="0"
                className="w-full border rounded-lg px-3 py-2"
                defaultValue={0}
              />
            </Field>
            <Field label="Giá bán (VNĐ)" required>
              <input
                type="number"
                name="sellingPrice"
                required
                min="0"
                className="w-full border rounded-lg px-3 py-2"
                defaultValue={0}
              />
            </Field>
            <Field label="Mức tồn tối thiểu" required>
              <input
                type="number"
                name="reorderLevel"
                required
                min="0"
                className="w-full border rounded-lg px-3 py-2"
                defaultValue={5}
              />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Bảo hành (Ngày)">
              <input
                type="number"
                name="warrantyDays"
                min="0"
                className="w-full border rounded-lg px-3 py-2"
                defaultValue={0}
              />
            </Field>
            <Field label="Bảo hành (Km)">
              <input
                type="number"
                name="warrantyKm"
                min="0"
                className="w-full border rounded-lg px-3 py-2"
                defaultValue={0}
              />
            </Field>
          </div>

          <Field label="Mô tả / Ghi chú">
            <textarea
              name="description"
              rows={2}
              className="w-full border rounded-lg px-3 py-2"
              placeholder="Quy cách đóng gói, thông số kỹ thuật..."
            />
          </Field>

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="ghost" onClick={() => setCreating(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={createMutation.isPending}>
              Lưu phụ tùng
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Sửa */}
      <Modal open={!!editing} onOpenChange={(open: boolean) => !open && setEditing(null)} title="Cập nhật Phụ tùng">
        {editing && (
          <form onSubmit={handleUpdate} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Mã SKU (Bất biến)">
                <input
                  disabled
                  value={editing.sku}
                  className="w-full border rounded-lg px-3 py-2 bg-gray-100 text-gray-500 font-mono"
                />
              </Field>
              <Field label="Đơn vị tính" required>
                <input
                  name="unit"
                  required
                  defaultValue={editing.unit}
                  className="w-full border rounded-lg px-3 py-2"
                />
              </Field>
            </div>

            <Field label="Tên phụ tùng" required>
              <input
                name="partName"
                required
                defaultValue={editing.partName}
                className="w-full border rounded-lg px-3 py-2"
              />
            </Field>

            <Field label="Nhà cung cấp ưu tiên">
              <select
                name="preferredSupplierId"
                defaultValue={editing.preferredSupplierId || ''}
                className="w-full border rounded-lg px-3 py-2 bg-white"
              >
                <option value="">-- Chưa chỉ định --</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.supplierName} ({s.supplierCode})
                  </option>
                ))}
              </select>
            </Field>

            <div className="grid grid-cols-3 gap-4">
              <Field label="Giá nhập (VNĐ)" required>
                <input
                  type="number"
                  name="purchasePrice"
                  required
                  min="0"
                  defaultValue={editing.purchasePrice}
                  className="w-full border rounded-lg px-3 py-2"
                />
              </Field>
              <Field label="Giá bán (VNĐ)" required>
                <input
                  type="number"
                  name="sellingPrice"
                  required
                  min="0"
                  defaultValue={editing.sellingPrice}
                  className="w-full border rounded-lg px-3 py-2"
                />
              </Field>
              <Field label="Mức tồn tối thiểu" required>
                <input
                  type="number"
                  name="reorderLevel"
                  required
                  min="0"
                  defaultValue={editing.reorderLevel}
                  className="w-full border rounded-lg px-3 py-2"
                />
              </Field>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Bảo hành (Ngày)">
                <input
                  type="number"
                  name="warrantyDays"
                  min="0"
                  defaultValue={editing.warrantyDays}
                  className="w-full border rounded-lg px-3 py-2"
                />
              </Field>
              <Field label="Bảo hành (Km)">
                <input
                  type="number"
                  name="warrantyKm"
                  min="0"
                  defaultValue={editing.warrantyKm}
                  className="w-full border rounded-lg px-3 py-2"
                />
              </Field>
            </div>

            <Field label="Trạng thái">
              <select
                name="isActive"
                defaultValue={String(editing.isActive)}
                className="w-full border rounded-lg px-3 py-2 bg-white"
              >
                <option value="true">Đang hoạt động</option>
                <option value="false">Ngừng hoạt động</option>
              </select>
            </Field>

            <div className="flex justify-end gap-3 pt-3">
              <Button variant="ghost" onClick={() => setEditing(null)}>
                Hủy
              </Button>
              <Button variant="primary" type="submit" pending={updateMutation.isPending}>
                Cập nhật
              </Button>
            </div>
          </form>
        )}
      </Modal>

      {/* Confirm Ngừng hoạt động */}
      <ConfirmModal
        open={!!deletingId}
        onOpenChange={(open: boolean) => !open && setDeletingId(null)}
        title="Ngừng hoạt động phụ tùng"
        description="Bạn có chắc muốn ngừng hoạt động phụ tùng này? Phụ tùng này sẽ không xuất hiện khi tạo mới đơn sửa chữa."
        confirmLabel="Ngừng hoạt động"
        variant="danger"
        pending={deactivateMutation.isPending}
        onConfirm={handleDeactivate}
      />
    </div>
  );
}
