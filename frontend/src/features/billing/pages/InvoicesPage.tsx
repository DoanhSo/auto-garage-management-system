import { useState } from 'react';
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
import { Button, Field, Modal, Badge, Loading } from '../../../shared/components/ui';
import {
  Receipt,
  Plus,
  Search,
  Eye,
  Ban,
  CreditCard,
  CheckCircle2,
  Clock,
  AlertCircle,
} from 'lucide-react';

export function InvoicesPage() {
  const [selectedStatus, setSelectedStatus] = useState<InvoiceStatus | ''>('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);

  const [viewingInvoice, setViewingInvoice] = useState<Invoice | null>(null);
  const [issuing, setIssuing] = useState(false);
  const [voidingInvoice, setVoidingInvoice] = useState<Invoice | null>(null);
  const [voidReason, setVoidReason] = useState('');
  const [payingInvoice, setPayingInvoice] = useState<Invoice | null>(null);

  // Payment form states
  const [payAmount, setPayAmount] = useState<number>(0);
  const [payMethod, setPayMethod] = useState<PaymentMethod>('CASH');
  const [payReference, setPayReference] = useState('');
  const [payNotes, setPayNotes] = useState('');

  const { data, isLoading } = useInvoicesQuery({
    status: selectedStatus || undefined,
    query: query || undefined,
    page,
    size: 20,
    sortBy: 'id',
    sortDir: 'desc',
  });

  const issueMutation = useIssueInvoiceMutation();
  const voidMutation = useVoidInvoiceMutation();
  const receivePaymentMutation = useReceivePaymentMutation();

  const handleIssue = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    const input: InvoiceIssueInput = {
      repairOrderId: Number(formData.get('repairOrderId')),
      taxAmount: Number(formData.get('taxAmount') || 0),
      discountAmount: Number(formData.get('discountAmount') || 0),
    };
    await issueMutation.mutateAsync(input);
    setIssuing(false);
  };

  const handleVoid = async () => {
    if (!voidingInvoice || !voidReason) return;
    await voidMutation.mutateAsync({
      id: voidingInvoice.id,
      data: { voidReason },
    });
    setVoidingInvoice(null);
    setVoidReason('');
  };

  const handleReceivePayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!payingInvoice) return;
    const input: PaymentReceiveInput = {
      invoiceId: payingInvoice.id,
      amount: payAmount,
      paymentMethod: payMethod,
      providerReference: payReference || undefined,
      notes: payNotes || undefined,
    };
    await receivePaymentMutation.mutateAsync(input);
    setPayingInvoice(null);
  };

  const renderStatusBadge = (status: InvoiceStatus) => {
    switch (status) {
      case 'PAID':
        return (
          <Badge variant="success" className="inline-flex items-center gap-1">
            <CheckCircle2 size={12} /> Đã thanh toán
          </Badge>
        );
      case 'PARTIALLY_PAID':
        return (
          <Badge variant="warning" className="inline-flex items-center gap-1">
            <Clock size={12} /> Thanh toán 1 phần
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

  return (
    <div className="page-container p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Receipt className="text-blue-600" size={28} />
            Quản lý Hóa đơn Sửa chữa (UC23)
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Xuất hóa đơn dịch vụ sửa chữa, theo dõi thanh toán và công nợ khách hàng
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setIssuing(true)}
          className="flex items-center gap-1.5"
        >
          <Plus size={16} />
          Xuất hóa đơn mới
        </Button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex flex-wrap gap-4 items-center">
        <div className="relative flex-1 min-w-[240px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Tìm theo số hóa đơn, khách hàng..."
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            className="w-full pl-9 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <select
          value={selectedStatus}
          onChange={(e) => {
            setSelectedStatus(e.target.value as typeof selectedStatus);
            setPage(0);
          }}
          className="border rounded-lg px-3 py-2 bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="UNPAID">Chưa thanh toán (UNPAID)</option>
          <option value="PARTIALLY_PAID">Thanh toán 1 phần (PARTIALLY_PAID)</option>
          <option value="PAID">Đã thanh toán (PAID)</option>
          <option value="VOID">Đã hủy (VOID)</option>
        </select>
      </div>

      {/* Invoices Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        {isLoading ? (
          <div className="py-12 flex justify-center">
            <Loading />
          </div>
        ) : !data?.content || data.content.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <Receipt size={48} className="mx-auto text-gray-300 mb-2" />
            <p>Không có hóa đơn nào</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-600 uppercase">
                  <th className="py-3 px-4">Số hóa đơn</th>
                  <th className="py-3 px-4">Lệnh sửa chữa</th>
                  <th className="py-3 px-4">Khách hàng</th>
                  <th className="py-3 px-4 text-right">Tổng tiền (VNĐ)</th>
                  <th className="py-3 px-4 text-right">Đã trả (VNĐ)</th>
                  <th className="py-3 px-4 text-right">Còn nợ (VNĐ)</th>
                  <th className="py-3 px-4 text-center">Trạng thái</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {data.content.map((inv) => {
                  const paid = Number(inv.paidAmount || 0);
                  const bal = Number(inv.balanceAmount || 0);
                  const custName = inv.customerSnapshot?.fullName || `KH #${inv.customerId}`;
                  const custPhone = inv.customerSnapshot?.phone;

                  return (
                    <tr key={inv.id} className="hover:bg-gray-50/80 transition-colors">
                      <td className="py-3 px-4 font-mono font-medium text-gray-900">
                        {inv.invoiceCode}
                      </td>
                      <td className="py-3 px-4 font-mono text-gray-700">#{inv.repairOrderId}</td>
                      <td className="py-3 px-4">
                        <div className="font-medium text-gray-800">{custName}</div>
                        {custPhone && <div className="text-xs text-gray-400">{custPhone}</div>}
                      </td>
                      <td className="py-3 px-4 text-right font-semibold text-gray-900">
                        {Number(inv.totalAmount).toLocaleString('vi-VN')}
                      </td>
                      <td className="py-3 px-4 text-right text-green-600 font-medium">
                        {paid.toLocaleString('vi-VN')}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <span
                          className={`font-semibold ${bal > 0 ? 'text-red-600' : 'text-gray-400'}`}
                        >
                          {bal.toLocaleString('vi-VN')}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-center">{renderStatusBadge(inv.status)}</td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex justify-end items-center gap-1.5">
                          <button
                            onClick={() => setViewingInvoice(inv)}
                            className="p-1.5 text-gray-500 hover:text-blue-600 rounded-lg hover:bg-blue-50 transition-colors"
                            title="Xem chi tiết"
                          >
                            <Eye size={16} />
                          </button>
                          {inv.status !== 'PAID' && inv.status !== 'VOID' && (
                            <button
                              onClick={() => {
                                setPayingInvoice(inv);
                                setPayAmount(bal);
                              }}
                              className="p-1.5 text-green-600 hover:text-green-700 rounded-lg hover:bg-green-50 transition-colors"
                              title="Thu tiền hóa đơn"
                            >
                              <CreditCard size={16} />
                            </button>
                          )}
                          {inv.status !== 'PAID' && inv.status !== 'VOID' && (
                            <button
                              onClick={() => setVoidingInvoice(inv)}
                              className="p-1.5 text-red-500 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors"
                              title="Hủy hóa đơn"
                            >
                              <Ban size={16} />
                            </button>
                          )}
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
        {data && data.totalPages > 1 && (
          <div className="flex justify-between items-center px-4 py-3 bg-gray-50 border-t border-gray-100">
            <span className="text-xs text-gray-500">
              Trang {page + 1} / {data.totalPages} (Tổng {data.totalElements} hóa đơn)
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

      {/* Modal Xuất Hóa Đơn Mới */}
      <Modal
        open={issuing}
        onOpenChange={(open: boolean) => !open && setIssuing(false)}
        title="Xuất Hóa Đơn Sửa Chữa Mới"
      >
        <form onSubmit={handleIssue} className="space-y-4">
          <Field label="Mã lệnh sửa chữa (Repair Order ID)" required>
            <input
              type="number"
              name="repairOrderId"
              required
              className="w-full border rounded-lg px-3 py-2"
              placeholder="VD: 101"
            />
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Tiền thuế GTGT / VAT (VNĐ)">
              <input
                type="number"
                name="taxAmount"
                defaultValue={0}
                min="0"
                className="w-full border rounded-lg px-3 py-2"
              />
            </Field>
            <Field label="Chiết khấu giảm giá (VNĐ)">
              <input
                type="number"
                name="discountAmount"
                defaultValue={0}
                min="0"
                className="w-full border rounded-lg px-3 py-2"
              />
            </Field>
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="ghost" onClick={() => setIssuing(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={issueMutation.isPending}>
              Xuất hóa đơn
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Xem Chi Tiết Hóa Đơn */}
      <Modal
        open={!!viewingInvoice}
        onOpenChange={(open: boolean) => !open && setViewingInvoice(null)}
        title={`Chi tiết Hóa đơn: ${viewingInvoice?.invoiceCode}`}
      >
        {viewingInvoice && (
          <div className="space-y-4 max-h-[80vh] overflow-y-auto pr-1">
            <div className="grid grid-cols-2 gap-3 text-sm bg-gray-50 p-3 rounded-lg border">
              <div>
                <span className="text-gray-500">Mã lệnh SC: </span>
                <span className="font-semibold">#{viewingInvoice.repairOrderId}</span>
              </div>
              <div>
                <span className="text-gray-500">Trạng thái: </span>
                {renderStatusBadge(viewingInvoice.status)}
              </div>
              <div>
                <span className="text-gray-500">Khách hàng: </span>
                <span className="font-semibold">
                  {viewingInvoice.customerSnapshot?.fullName || `KH #${viewingInvoice.customerId}`}
                </span>
              </div>
              <div>
                <span className="text-gray-500">Số điện thoại: </span>
                <span className="font-semibold">
                  {viewingInvoice.customerSnapshot?.phone || '—'}
                </span>
              </div>
            </div>

            {/* Bảng chi tiết dòng */}
            <h4 className="font-semibold text-gray-800 text-sm">Các mục thanh toán</h4>
            <div className="border rounded-lg overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead className="bg-gray-100 border-b">
                  <tr>
                    <th className="p-2">Loại</th>
                    <th className="p-2">Hạng mục</th>
                    <th className="p-2 text-right">SL</th>
                    <th className="p-2 text-right">Đơn giá</th>
                    <th className="p-2 text-right">Thành tiền</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {viewingInvoice.items?.map((line) => (
                    <tr key={line.id}>
                      <td className="p-2">
                        <Badge variant={line.itemType === 'PART' ? 'primary' : 'secondary'}>
                          {line.itemType === 'PART' ? 'Phụ tùng' : 'Tiền công'}
                        </Badge>
                      </td>
                      <td className="p-2 font-medium">{line.itemName}</td>
                      <td className="p-2 text-right">{line.quantity}</td>
                      <td className="p-2 text-right">
                        {Number(line.unitPrice).toLocaleString('vi-VN')}
                      </td>
                      <td className="p-2 text-right font-semibold">
                        {Number(line.lineTotal).toLocaleString('vi-VN')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Bảng tổng tiền */}
            <div className="space-y-1.5 text-sm bg-gray-50 p-3 rounded-lg border">
              <div className="flex justify-between">
                <span className="text-gray-600">Tổng tiền trước thuế:</span>
                <span className="font-medium">
                  {Number(viewingInvoice.subtotal).toLocaleString('vi-VN')} VNĐ
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Giảm giá:</span>
                <span className="font-medium text-amber-600">
                  -{Number(viewingInvoice.discountAmount).toLocaleString('vi-VN')} VNĐ
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Tiền thuế:</span>
                <span className="font-medium">
                  {Number(viewingInvoice.taxAmount).toLocaleString('vi-VN')} VNĐ
                </span>
              </div>
              <div className="flex justify-between border-t border-gray-200 pt-2 text-base font-bold text-gray-900">
                <span>Tổng cộng:</span>
                <span className="text-blue-600">
                  {Number(viewingInvoice.totalAmount).toLocaleString('vi-VN')} VNĐ
                </span>
              </div>
              <div className="flex justify-between text-xs pt-1">
                <span className="text-green-600 font-medium">Đã thanh toán:</span>
                <span className="text-green-600 font-medium">
                  {Number(viewingInvoice.paidAmount || 0).toLocaleString('vi-VN')} VNĐ
                </span>
              </div>
              <div className="flex justify-between text-xs font-bold text-red-600">
                <span>Còn nợ:</span>
                <span>{Number(viewingInvoice.balanceAmount || 0).toLocaleString('vi-VN')} VNĐ</span>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <Button variant="secondary" onClick={() => setViewingInvoice(null)}>
                Đóng
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal Thu Tiền Hóa Đơn */}
      <Modal
        open={!!payingInvoice}
        onOpenChange={(open: boolean) => !open && setPayingInvoice(null)}
        title={`Thu tiền Hóa đơn: ${payingInvoice?.invoiceCode}`}
      >
        {payingInvoice && (
          <form onSubmit={handleReceivePayment} className="space-y-4">
            <div className="bg-blue-50 p-3 rounded-lg text-sm text-blue-900">
              Khách hàng:{' '}
              <span className="font-bold">
                {payingInvoice.customerSnapshot?.fullName || `KH #${payingInvoice.customerId}`}
              </span>
              <br />
              Số dư nợ còn lại:{' '}
              <span className="font-bold text-red-600">
                {Number(payingInvoice.balanceAmount || 0).toLocaleString('vi-VN')} VNĐ
              </span>
            </div>

            <Field label="Số tiền thu (VNĐ)" required>
              <input
                type="number"
                min="1000"
                max={Number(payingInvoice.balanceAmount || 0)}
                value={payAmount}
                onChange={(e) => setPayAmount(Number(e.target.value))}
                required
                className="w-full border rounded-lg px-3 py-2 font-bold text-lg text-blue-600"
              />
            </Field>

            <Field label="Hình thức thanh toán" required>
              <select
                value={payMethod}
                onChange={(e) => setPayMethod(e.target.value as PaymentMethod)}
                className="w-full border rounded-lg px-3 py-2 bg-white"
              >
                <option value="CASH">Tiền mặt (CASH)</option>
                <option value="BANK_TRANSFER">Chuyển khoản ngân hàng (BANK_TRANSFER)</option>
                <option value="CARD">Quẹt thẻ POS (CARD)</option>
                <option value="VIETQR">Mã VietQR</option>
              </select>
            </Field>

            <Field label="Mã chuẩn chi / Mã giao dịch ngân hàng">
              <input
                value={payReference}
                onChange={(e) => setPayReference(e.target.value)}
                placeholder="VD: FT240981928"
                className="w-full border rounded-lg px-3 py-2"
              />
            </Field>

            <Field label="Ghi chú">
              <input
                value={payNotes}
                onChange={(e) => setPayNotes(e.target.value)}
                placeholder="Ghi chú phiếu thu..."
                className="w-full border rounded-lg px-3 py-2"
              />
            </Field>

            <div className="flex justify-end gap-3 pt-3">
              <Button variant="ghost" onClick={() => setPayingInvoice(null)}>
                Hủy
              </Button>
              <Button variant="primary" type="submit" pending={receivePaymentMutation.isPending}>
                Xác nhận thu tiền
              </Button>
            </div>
          </form>
        )}
      </Modal>

      {/* Modal Hủy Hóa Đơn */}
      <Modal
        open={!!voidingInvoice}
        onOpenChange={(open: boolean) => !open && setVoidingInvoice(null)}
        title={`Hủy bỏ Hóa đơn: ${voidingInvoice?.invoiceCode}`}
      >
        <div className="space-y-4">
          <p className="text-sm text-gray-600">
            Hóa đơn sẽ bị chuyển sang trạng thái <strong>VOID (Hủy)</strong> và toàn bộ công nợ liên
            quan sẽ được hoàn nguyên. Vui lòng nhập lý do hủy:
          </p>

          <Field label="Lý do hủy" required>
            <textarea
              value={voidReason}
              onChange={(e) => setVoidReason(e.target.value)}
              required
              rows={3}
              placeholder="Nhập lý do sai sót thông tin, khách đổi ý..."
              className="w-full border rounded-lg px-3 py-2"
            />
          </Field>

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="ghost" onClick={() => setVoidingInvoice(null)}>
              Quay lại
            </Button>
            <Button
              variant="danger"
              disabled={!voidReason.trim()}
              pending={voidMutation.isPending}
              onClick={handleVoid}
            >
              Xác nhận hủy hóa đơn
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
