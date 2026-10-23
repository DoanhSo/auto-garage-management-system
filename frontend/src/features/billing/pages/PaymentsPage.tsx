import { useState } from 'react';
import {
  usePaymentsQuery,
  usePaySupplierMutation,
} from '../hooks/useBilling';
import { useSuppliersQuery, useDocumentsQuery } from '../../inventory/hooks/useInventory';
import type { PaymentDirection, PaymentMethod, SupplierPaymentInput } from '../types/billing';
import { Button, Field, Modal, Badge, Loading } from '../../../shared/components/ui';
import {
  DollarSign,
  Plus,
  Search,
  ArrowDownLeft,
  ArrowUpRight,
} from 'lucide-react';

export function PaymentsPage() {
  const [direction, setDirection] = useState<PaymentDirection | ''>('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);

  // Modal Chi trả NCC
  const [payingSupplier, setPayingSupplier] = useState(false);
  const [selectedSupplierId, setSelectedSupplierId] = useState<string>('');
  const [selectedDocumentId, setSelectedDocumentId] = useState<string>('');
  const [payAmount, setPayAmount] = useState<number>(0);
  const [payMethod, setPayMethod] = useState<PaymentMethod>('BANK_TRANSFER');
  const [payReference, setPayReference] = useState('');
  const [payNotes, setPayNotes] = useState('');

  const { data: suppliersData } = useSuppliersQuery({ size: 100, active: true });
  const suppliers = suppliersData?.content || [];

  const { data: documentsData } = useDocumentsQuery({
    supplierId: selectedSupplierId ? Number(selectedSupplierId) : undefined,
    status: 'POSTED',
    size: 50,
  });
  const supplierDocuments = documentsData?.content || [];

  const { data, isLoading } = usePaymentsQuery({
    direction: direction || undefined,
    query: query || undefined,
    page,
    size: 20,
    sortBy: 'id',
    sortDir: 'desc',
  });

  const paySupplierMutation = usePaySupplierMutation();

  const handlePaySupplier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDocumentId || payAmount <= 0) return;

    const payload: SupplierPaymentInput = {
      inventoryDocumentId: Number(selectedDocumentId),
      amount: payAmount,
      paymentMethod: payMethod,
      providerReference: payReference || undefined,
      notes: payNotes || undefined,
    };

    await paySupplierMutation.mutateAsync(payload);
    setPayingSupplier(false);
    // Reset form
    setSelectedSupplierId('');
    setSelectedDocumentId('');
    setPayAmount(0);
    setPayReference('');
    setPayNotes('');
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

  return (
    <div className="page-container p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <DollarSign className="text-blue-600" size={28} />
            Sổ Quỹ & Giao dịch Thu / Chi (UC24)
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Quản lý dòng tiền vào (thu tiền hóa đơn khách) và dòng tiền ra (chi trả tiền nhập hàng NCC)
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setPayingSupplier(true)}
          className="flex items-center gap-1.5"
        >
          <Plus size={16} />
          Lập phiếu chi trả NCC
        </Button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex flex-wrap gap-4 items-center">
        <div className="relative flex-1 min-w-[240px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Tìm theo mã phiếu, mã tham chiếu..."
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            className="w-full pl-9 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <select
          value={direction}
          onChange={(e) => {
            setDirection(e.target.value as any);
            setPage(0);
          }}
          className="border rounded-lg px-3 py-2 bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Tất cả giao dịch (Thu & Chi)</option>
          <option value="IN">Dòng tiền VÀO - Thu tiền khách (IN)</option>
          <option value="OUT">Dòng tiền RA - Chi trả NCC (OUT)</option>
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
            <DollarSign size={48} className="mx-auto text-gray-300 mb-2" />
            <p>Không có giao dịch thanh toán nào</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-600 uppercase">
                  <th className="py-3 px-4">Mã giao dịch</th>
                  <th className="py-3 px-4">Loại giao dịch</th>
                  <th className="py-3 px-4">Đối tượng & Chứng từ liên quan</th>
                  <th className="py-3 px-4">Hình thức</th>
                  <th className="py-3 px-4 text-right">Số tiền (VNĐ)</th>
                  <th className="py-3 px-4">Mã tham chiếu ngân hàng</th>
                  <th className="py-3 px-4">Ngày giao dịch</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {data.content.map((pay) => (
                  <tr key={pay.id} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-3 px-4 font-mono font-medium text-gray-900">{pay.transactionCode}</td>
                    <td className="py-3 px-4">
                      {pay.direction === 'IN' ? (
                        <Badge variant="success" className="inline-flex items-center gap-1">
                          <ArrowDownLeft size={12} /> Thu tiền khách
                        </Badge>
                      ) : (
                        <Badge variant="danger" className="inline-flex items-center gap-1">
                          <ArrowUpRight size={12} /> Chi trả NCC
                        </Badge>
                      )}
                    </td>
                    <td className="py-3 px-4 text-gray-700">
                      {pay.invoiceCode && (
                        <div>
                          Hóa đơn: <span className="font-mono font-medium">{pay.invoiceCode}</span>
                        </div>
                      )}
                      {pay.documentCode && (
                        <div>
                          Phiếu kho: <span className="font-mono font-medium">{pay.documentCode}</span>
                        </div>
                      )}
                      {pay.notes && <div className="text-xs text-gray-400">{pay.notes}</div>}
                    </td>
                    <td className="py-3 px-4">{renderMethodBadge(pay.paymentMethod)}</td>
                    <td className="py-3 px-4 text-right font-bold">
                      <span className={pay.direction === 'IN' ? 'text-green-600' : 'text-red-600'}>
                        {pay.direction === 'IN' ? '+' : '-'}
                        {Number(pay.amount).toLocaleString('vi-VN')}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-mono text-xs text-gray-500">
                      {pay.providerReference || '—'}
                    </td>
                    <td className="py-3 px-4 text-gray-500 text-xs">
                      {new Date(pay.paidAt).toLocaleDateString('vi-VN')}
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
              Trang {page + 1} / {data.totalPages} (Tổng {data.totalElements} giao dịch)
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

      {/* Modal Lập Phiếu Chi Trả NCC */}
      <Modal
        open={payingSupplier}
        onOpenChange={(open: boolean) => !open && setPayingSupplier(false)}
        title="Lập Phiếu Chi Tiền Trả Nhà Cung Cấp"
      >
        <form onSubmit={handlePaySupplier} className="space-y-4">
          <Field label="Nhà cung cấp nhận thanh toán" required>
            <select
              value={selectedSupplierId}
              onChange={(e) => setSelectedSupplierId(e.target.value)}
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

          {selectedSupplierId && (
            <Field label="Gán vào phiếu nhập kho" required>
              <select
                value={selectedDocumentId}
                onChange={(e) => setSelectedDocumentId(e.target.value)}
                required
                className="w-full border rounded-lg px-3 py-2 bg-white"
              >
                <option value="">-- Chọn phiếu nhập kho --</option>
                {supplierDocuments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.documentCode} - {Number(d.totalAmount).toLocaleString('vi-VN')} VNĐ
                  </option>
                ))}
              </select>
            </Field>
          )}

          <div className="grid grid-cols-2 gap-4">
            <Field label="Số tiền chi (VNĐ)" required>
              <input
                type="number"
                min="1000"
                value={payAmount}
                onChange={(e) => setPayAmount(Number(e.target.value))}
                required
                className="w-full border rounded-lg px-3 py-2 font-bold text-lg text-red-600"
              />
            </Field>

            <Field label="Phương thức chi" required>
              <select
                value={payMethod}
                onChange={(e) => setPayMethod(e.target.value as PaymentMethod)}
                className="w-full border rounded-lg px-3 py-2 bg-white"
              >
                <option value="BANK_TRANSFER">Chuyển khoản công ty</option>
                <option value="CASH">Tiền mặt tại quỹ</option>
                <option value="CARD">Thẻ công ty</option>
                <option value="VIETQR">Mã VietQR</option>
              </select>
            </Field>
          </div>

          <Field label="Mã giao dịch ngân hàng / Uỷ nhiệm chi">
            <input
              value={payReference}
              onChange={(e) => setPayReference(e.target.value)}
              placeholder="VD: UNC-20241022-091"
              className="w-full border rounded-lg px-3 py-2"
            />
          </Field>

          <Field label="Ghi chú chi tiền">
            <textarea
              value={payNotes}
              onChange={(e) => setPayNotes(e.target.value)}
              rows={2}
              placeholder="Nội dung thanh toán..."
              className="w-full border rounded-lg px-3 py-2"
            />
          </Field>

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="ghost" onClick={() => setPayingSupplier(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={paySupplierMutation.isPending}>
              Xác nhận chi tiền
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
