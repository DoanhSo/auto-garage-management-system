import { useState } from 'react';
import {
  useDocumentsQuery,
  useCreateDocumentMutation,
  usePostDocumentMutation,
  useCancelDocumentMutation,
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
import { Button, Field, Modal, ConfirmModal, Badge, Loading } from '../../../shared/components/ui';
import {
  FileText,
  Plus,
  CheckCircle,
  XCircle,
  Eye,
  Trash2,
  ArrowDownLeft,
  ArrowUpRight,
  RotateCcw,
  Sliders,
} from 'lucide-react';

export function InventoryDocumentsPage() {
  const [selectedType, setSelectedType] = useState<DocumentType | ''>('');
  const [selectedStatus, setSelectedStatus] = useState<DocumentStatus | ''>('');
  const [page, setPage] = useState(0);
  const [viewingDoc, setViewingDoc] = useState<InventoryDocument | null>(null);
  const [creating, setCreating] = useState(false);
  const [postingId, setPostingId] = useState<string | number | null>(null);
  const [cancellingId, setCancellingId] = useState<string | number | null>(null);

  // Form states for creating document
  const [docType, setDocType] = useState<DocumentType>('IN');
  const [supplierId, setSupplierId] = useState<string>('');
  const [orderId, setOrderId] = useState<string>('');
  const [referenceNumber, setReferenceNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [lines, setLines] = useState<InventoryDocumentItemInput[]>([
    { partId: 0, quantity: 1, unitCost: 0, notes: '' },
  ]);

  const { data: suppliersData } = useSuppliersQuery({ size: 100, active: true });
  const suppliers = suppliersData?.content || [];

  const { data: partsData } = usePartsQuery({ size: 200, active: true });
  const parts = partsData?.content || [];

  const { data, isLoading } = useDocumentsQuery({
    type: selectedType || undefined,
    status: selectedStatus || undefined,
    page,
    size: 20,
    sortBy: 'id',
    sortDir: 'desc',
  });

  const createMutation = useCreateDocumentMutation();
  const postMutation = usePostDocumentMutation();
  const cancelMutation = useCancelDocumentMutation();

  const handleAddLine = () => {
    setLines([...lines, { partId: 0, quantity: 1, unitCost: 0, notes: '' }]);
  };

  const handleRemoveLine = (idx: number) => {
    setLines(lines.filter((_, i) => i !== idx));
  };

  const handleLineChange = (idx: number, field: keyof InventoryDocumentItemInput, value: any) => {
    const updated = [...lines];
    updated[idx] = { ...updated[idx], [field]: value };
    if (field === 'partId') {
      const selectedPart = parts.find((p) => p.id === String(value));
      if (selectedPart) {
        updated[idx].unitCost = selectedPart.purchasePrice;
      }
    }
    setLines(updated);
  };

  const handleCreateDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload: InventoryDocumentCreateInput = {
      documentType: docType,
      supplierId: docType === 'IN' && supplierId ? Number(supplierId) : undefined,
      repairOrderId: (docType === 'OUT' || docType === 'RETURN') && orderId ? Number(orderId) : undefined,
      supplierInvoiceNumber: referenceNumber || undefined,
      reason: notes || undefined,
      items: lines.map((l) => ({
        partId: l.partId,
        quantity: Number(l.quantity),
        unitCost: Number(l.unitCost),
        notes: l.notes || undefined,
      })),
    };

    await createMutation.mutateAsync(payload);
    setCreating(false);
    // Reset form
    setLines([{ partId: 0, quantity: 1, unitCost: 0, notes: '' }]);
    setReferenceNumber('');
    setNotes('');
  };

  const handlePost = async () => {
    if (!postingId) return;
    await postMutation.mutateAsync({ id: postingId });
    setPostingId(null);
  };

  const handleCancel = async () => {
    if (!cancellingId) return;
    await cancelMutation.mutateAsync(cancellingId);
    setCancellingId(null);
  };

  const renderTypeBadge = (type: DocumentType) => {
    switch (type) {
      case 'IN':
        return (
          <Badge variant="primary" className="flex items-center gap-1">
            <ArrowDownLeft size={12} /> Nhập kho
          </Badge>
        );
      case 'OUT':
        return (
          <Badge variant="warning" className="flex items-center gap-1">
            <ArrowUpRight size={12} /> Xuất kho
          </Badge>
        );
      case 'RETURN':
        return (
          <Badge variant="secondary" className="flex items-center gap-1">
            <RotateCcw size={12} /> Trả hàng
          </Badge>
        );
      case 'ADJUSTMENT':
        return (
          <Badge variant="outline" className="flex items-center gap-1">
            <Sliders size={12} /> Kiểm kê / Điều chỉnh
          </Badge>
        );
    }
  };

  const renderStatusBadge = (status: DocumentStatus) => {
    switch (status) {
      case 'DRAFT':
        return <Badge variant="secondary">Bản nháp</Badge>;
      case 'POSTED':
        return <Badge variant="success">Đã ghi sổ</Badge>;
      case 'CANCELLED':
        return <Badge variant="danger">Đã hủy</Badge>;
    }
  };

  return (
    <div className="page-container p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <FileText className="text-blue-600" size={28} />
            Chứng từ Kho & Nhập / Xuất phụ tùng
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Quản lý phiếu nhập kho, phiếu xuất phụ tùng sửa chữa và kiểm kê kho
          </p>
        </div>
        <Button variant="primary" onClick={() => setCreating(true)} className="flex items-center gap-1.5">
          <Plus size={16} />
          Tạo phiếu kho mới
        </Button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex flex-wrap gap-4 items-center">
        <select
          value={selectedType}
          onChange={(e) => {
            setSelectedType(e.target.value as any);
            setPage(0);
          }}
          className="border rounded-lg px-3 py-2 bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Tất cả loại chứng từ</option>
          <option value="IN">Phiếu Nhập kho (IN)</option>
          <option value="OUT">Phiếu Xuất kho (OUT)</option>
          <option value="RETURN">Phiếu Trả hàng (RETURN)</option>
          <option value="ADJUSTMENT">Phiếu Kiểm kê (ADJUSTMENT)</option>
        </select>

        <select
          value={selectedStatus}
          onChange={(e) => {
            setSelectedStatus(e.target.value as any);
            setPage(0);
          }}
          className="border rounded-lg px-3 py-2 bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="DRAFT">Bản nháp (DRAFT)</option>
          <option value="POSTED">Đã ghi sổ (POSTED)</option>
          <option value="CANCELLED">Đã hủy (CANCELLED)</option>
        </select>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        {isLoading ? (
          <div className="py-12 flex justify-center">
            <Loading />
          </div>
        ) : !data?.content || data.content.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <FileText size={48} className="mx-auto text-gray-300 mb-2" />
            <p>Không có chứng từ nào</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-600 uppercase">
                  <th className="py-3 px-4">Số chứng từ</th>
                  <th className="py-3 px-4">Loại phiếu</th>
                  <th className="py-3 px-4">Đối tác / Đơn liên quan</th>
                  <th className="py-3 px-4 text-right">Tổng tiền (VNĐ)</th>
                  <th className="py-3 px-4 text-center">Trạng thái</th>
                  <th className="py-3 px-4">Ngày tạo</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {data.content.map((doc) => (
                  <tr key={doc.id} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-3 px-4 font-mono font-medium text-gray-900">{doc.documentCode}</td>
                    <td className="py-3 px-4">{renderTypeBadge(doc.documentType)}</td>
                    <td className="py-3 px-4 text-gray-700">
                      {doc.supplierName && <div>NCC: {doc.supplierName}</div>}
                      {doc.repairOrderId && <div>Lệnh sửa chữa: #{doc.repairOrderId}</div>}
                      {doc.supplierInvoiceNumber && (
                        <div className="text-xs text-gray-400">Số HĐ/Tham chiếu: {doc.supplierInvoiceNumber}</div>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right font-medium text-blue-600">
                      {Number(doc.totalAmount || 0).toLocaleString('vi-VN')}
                    </td>
                    <td className="py-3 px-4 text-center">{renderStatusBadge(doc.status)}</td>
                    <td className="py-3 px-4 text-gray-500 text-xs">
                      {new Date(doc.createdAt).toLocaleDateString('vi-VN')}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex justify-end items-center gap-1.5">
                        <button
                          onClick={() => setViewingDoc(doc)}
                          className="p-1.5 text-gray-500 hover:text-blue-600 rounded-lg hover:bg-blue-50 transition-colors"
                          title="Xem chi tiết"
                        >
                          <Eye size={16} />
                        </button>
                        {doc.status === 'DRAFT' && (
                          <>
                            <button
                              onClick={() => setPostingId(doc.id)}
                              className="p-1.5 text-green-600 hover:text-green-700 rounded-lg hover:bg-green-50 transition-colors"
                              title="Ghi sổ kho"
                            >
                              <CheckCircle size={16} />
                            </button>
                            <button
                              onClick={() => setCancellingId(doc.id)}
                              className="p-1.5 text-red-500 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors"
                              title="Hủy phiếu"
                            >
                              <XCircle size={16} />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex justify-between items-center px-4 py-3 bg-gray-50 border-t border-gray-100">
            <span className="text-xs text-gray-500">
              Trang {page + 1} / {data.totalPages} (Tổng {data.totalElements} chứng từ)
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

      {/* Modal Tạo Phiếu Kho Mới */}
      <Modal open={creating} onOpenChange={setCreating} title="Lập Chứng từ Kho Mới">
        <form onSubmit={handleCreateDocument} className="space-y-4 max-h-[80vh] overflow-y-auto pr-1">
          <div className="grid grid-cols-2 gap-4">
            <Field label="Loại chứng từ" required>
              <select
                value={docType}
                onChange={(e) => setDocType(e.target.value as DocumentType)}
                className="w-full border rounded-lg px-3 py-2 bg-white"
              >
                <option value="IN">Phiếu Nhập kho (IN)</option>
                <option value="OUT">Phiếu Xuất kho (OUT)</option>
                <option value="RETURN">Phiếu Trả hàng (RETURN)</option>
                <option value="ADJUSTMENT">Phiếu Kiểm kê / Điều chỉnh (ADJUSTMENT)</option>
              </select>
            </Field>

            {docType === 'IN' ? (
              <Field label="Nhà cung cấp" required>
                <select
                  value={supplierId}
                  onChange={(e) => setSupplierId(e.target.value)}
                  required
                  className="w-full border rounded-lg px-3 py-2 bg-white"
                >
                  <option value="">-- Chọn nhà cung cấp --</option>
                  {suppliers.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.supplierName} ({s.supplierCode})
                    </option>
                  ))}
                </select>
              </Field>
            ) : (
              <Field label="Mã lệnh sửa chữa">
                <input
                  type="number"
                  placeholder="ID Lệnh sửa chữa"
                  value={orderId}
                  onChange={(e) => setOrderId(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2"
                />
              </Field>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Số hóa đơn / Số tham chiếu">
              <input
                value={referenceNumber}
                onChange={(e) => setReferenceNumber(e.target.value)}
                placeholder="VD: HĐ-002941"
                className="w-full border rounded-lg px-3 py-2"
              />
            </Field>
            <Field label="Ghi chú">
              <input
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ghi chú chứng từ..."
                className="w-full border rounded-lg px-3 py-2"
              />
            </Field>
          </div>

          {/* Chi tiết phụ tùng */}
          <div className="border-t border-gray-100 pt-4 space-y-3">
            <div className="flex justify-between items-center">
              <h4 className="font-semibold text-gray-800 text-sm">Danh mục phụ tùng</h4>
              <Button variant="ghost" onClick={handleAddLine} className="text-blue-600 text-xs">
                + Thêm dòng
              </Button>
            </div>

            {lines.map((line, idx) => (
              <div key={idx} className="flex gap-2 items-center bg-gray-50 p-2.5 rounded-lg border">
                <div className="flex-1">
                  <select
                    value={line.partId}
                    onChange={(e) => handleLineChange(idx, 'partId', Number(e.target.value))}
                    required
                    className="w-full border rounded-lg px-2 py-1.5 text-xs bg-white"
                  >
                    <option value={0}>-- Chọn phụ tùng --</option>
                    {parts.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.sku} - {p.partName} (Tồn: {p.stockQuantity} {p.unit})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="w-24">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    placeholder="Số lượng"
                    value={line.quantity}
                    onChange={(e) => handleLineChange(idx, 'quantity', Number(e.target.value))}
                    required
                    className="w-full border rounded-lg px-2 py-1.5 text-xs"
                  />
                </div>
                <div className="w-28">
                  <input
                    type="number"
                    min="0"
                    placeholder="Đơn giá"
                    value={line.unitCost}
                    onChange={(e) => handleLineChange(idx, 'unitCost', Number(e.target.value))}
                    required
                    className="w-full border rounded-lg px-2 py-1.5 text-xs text-right"
                  />
                </div>
                <button
                  type="button"
                  disabled={lines.length === 1}
                  onClick={() => handleRemoveLine(idx)}
                  className="p-1.5 text-gray-400 hover:text-red-500 disabled:opacity-30"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
            <Button variant="ghost" onClick={() => setCreating(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={createMutation.isPending}>
              Tạo chứng từ
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Xem Chi Tiết Chứng Từ */}
      <Modal
        open={!!viewingDoc}
        onOpenChange={(open: boolean) => !open && setViewingDoc(null)}
        title={`Chi tiết chứng từ: ${viewingDoc?.documentCode}`}
      >
        {viewingDoc && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4 text-sm bg-gray-50 p-3 rounded-lg border">
              <div>
                <span className="text-gray-500">Loại phiếu: </span>
                <span className="font-semibold">{viewingDoc.documentType}</span>
              </div>
              <div>
                <span className="text-gray-500">Trạng thái: </span>
                <span className="font-semibold">{viewingDoc.status}</span>
              </div>
              <div>
                <span className="text-gray-500">Nhà cung cấp: </span>
                <span className="font-semibold">{viewingDoc.supplierName || '—'}</span>
              </div>
              <div>
                <span className="text-gray-500">Lệnh sửa chữa: </span>
                <span className="font-semibold">#{viewingDoc.repairOrderId || '—'}</span>
              </div>
              <div>
                <span className="text-gray-500">Tham chiếu: </span>
                <span className="font-semibold">{viewingDoc.supplierInvoiceNumber || '—'}</span>
              </div>
              <div>
                <span className="text-gray-500">Tổng giá trị: </span>
                <span className="font-semibold text-blue-600">
                  {Number(viewingDoc.totalAmount).toLocaleString('vi-VN')} VNĐ
                </span>
              </div>
            </div>

            <h4 className="font-semibold text-gray-800 text-sm">Danh sách dòng chi tiết</h4>
            <div className="border rounded-lg overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead className="bg-gray-100 border-b">
                  <tr>
                    <th className="p-2">Mã SKU</th>
                    <th className="p-2">Tên phụ tùng</th>
                    <th className="p-2 text-right">Số lượng</th>
                    <th className="p-2 text-right">Đơn giá</th>
                    <th className="p-2 text-right">Thành tiền</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {viewingDoc.items.map((line) => (
                    <tr key={line.id}>
                      <td className="p-2 font-mono">{line.partSku}</td>
                      <td className="p-2">{line.partName}</td>
                      <td className="p-2 text-right font-medium">{line.quantity}</td>
                      <td className="p-2 text-right">{Number(line.unitCost).toLocaleString('vi-VN')}</td>
                      <td className="p-2 text-right font-semibold text-gray-900">
                        {Number(line.lineTotal).toLocaleString('vi-VN')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-end pt-2">
              <Button variant="secondary" onClick={() => setViewingDoc(null)}>
                Đóng
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Confirm Ghi sổ kho */}
      <ConfirmModal
        open={!!postingId}
        onOpenChange={(open: boolean) => !open && setPostingId(null)}
        title="Ghi sổ kho chứng từ"
        description="Khi ghi sổ, hệ thống sẽ thực hiện cập nhật tức thời số lượng tồn kho và ghi nhận lịch sử vào sổ kho. Thao tác này không thể hoàn tác!"
        confirmLabel="Ghi sổ ngay"
        variant="primary"
        pending={postMutation.isPending}
        onConfirm={handlePost}
      />

      {/* Confirm Hủy phiếu */}
      <ConfirmModal
        open={!!cancellingId}
        onOpenChange={(open: boolean) => !open && setCancellingId(null)}
        title="Hủy chứng từ kho"
        description="Bạn có chắc muốn hủy chứng từ bản nháp này?"
        confirmLabel="Hủy chứng từ"
        variant="danger"
        pending={cancelMutation.isPending}
        onConfirm={handleCancel}
      />
    </div>
  );
}
