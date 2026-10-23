import { useState } from 'react';
import { useCustomerDebtsQuery, useSupplierDebtsQuery } from '../hooks/useBilling';
import { Button, Badge, Loading } from '../../../shared/components/ui';
import {
  CreditCard,
  Users,
  Building2,
  AlertTriangle,
  CheckCircle2,
  TrendingDown,
  TrendingUp,
} from 'lucide-react';

export function DebtLedgerPage() {
  const [activeTab, setActiveTab] = useState<'customer' | 'supplier'>('customer');
  const [hasBalanceOnly, setHasBalanceOnly] = useState(false);

  const { data: customerDebtsData, isLoading: isCustLoading } = useCustomerDebtsQuery({
    hasBalance: hasBalanceOnly ? true : undefined,
    size: 50,
  });

  const { data: supplierDebtsData, isLoading: isSuppLoading } = useSupplierDebtsQuery({
    hasBalance: hasBalanceOnly ? true : undefined,
    size: 50,
  });

  const customerDebts = customerDebtsData?.content || [];
  const supplierDebts = supplierDebtsData?.content || [];

  const totalReceivables = customerDebts.reduce((sum, c) => sum + Number(c.balanceAmount || 0), 0);
  const totalPayables = supplierDebts.reduce((sum, s) => sum + Number(s.balanceAmount || 0), 0);

  return (
    <div className="page-container p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <CreditCard className="text-blue-600" size={28} />
            Sổ Quản lý Công nợ Phải thu / Phải trả (TV4-06)
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Theo dõi công nợ khách hàng (hạn mức tín dụng) và công nợ nhà cung cấp phụ tùng
          </p>
        </div>
        <Button
          variant={hasBalanceOnly ? 'primary' : 'secondary'}
          onClick={() => setHasBalanceOnly(!hasBalanceOnly)}
        >
          {hasBalanceOnly ? 'Hiển thị tất cả' : 'Chỉ xem đối tượng còn nợ'}
        </Button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-red-50 text-red-600 rounded-lg">
              <TrendingUp size={24} />
            </div>
            <div>
              <div className="text-xs text-gray-500 font-medium">Tổng nợ phải thu (Khách hàng nợ gara)</div>
              <div className="text-2xl font-bold text-red-600">
                {totalReceivables.toLocaleString('vi-VN')} VNĐ
              </div>
            </div>
          </div>
          <Badge variant="danger">
            {customerDebts.filter((c) => Number(c.balanceAmount) > 0).length} khách còn nợ
          </Badge>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-amber-50 text-amber-600 rounded-lg">
              <TrendingDown size={24} />
            </div>
            <div>
              <div className="text-xs text-gray-500 font-medium">Tổng nợ phải trả (Gara nợ nhà cung cấp)</div>
              <div className="text-2xl font-bold text-amber-600">
                {totalPayables.toLocaleString('vi-VN')} VNĐ
              </div>
            </div>
          </div>
          <Badge variant="warning">
            {supplierDebts.filter((s) => Number(s.balanceAmount) > 0).length} NCC còn nợ
          </Badge>
        </div>
      </div>

      {/* Tabs navigation */}
      <div className="flex border-b border-gray-200">
        <button
          onClick={() => setActiveTab('customer')}
          className={`flex items-center gap-2 px-6 py-3 font-semibold text-sm border-b-2 transition-colors ${
            activeTab === 'customer'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Users size={18} />
          Công nợ Khách hàng
        </button>
        <button
          onClick={() => setActiveTab('supplier')}
          className={`flex items-center gap-2 px-6 py-3 font-semibold text-sm border-b-2 transition-colors ${
            activeTab === 'supplier'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Building2 size={18} />
          Công nợ Nhà cung cấp
        </button>
      </div>

      {/* Tab 1: Công nợ khách hàng */}
      {activeTab === 'customer' && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          {isCustLoading ? (
            <div className="py-12 flex justify-center">
              <Loading />
            </div>
          ) : customerDebts.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <Users size={48} className="mx-auto text-gray-300 mb-2" />
              <p>Không có dữ liệu công nợ khách hàng</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-600 uppercase">
                    <th className="py-3 px-4">Mã KH</th>
                    <th className="py-3 px-4">Tên khách hàng</th>
                    <th className="py-3 px-4">Số điện thoại</th>
                    <th className="py-3 px-4 text-right">Tổng phát sinh (VNĐ)</th>
                    <th className="py-3 px-4 text-right">Đã thanh toán (VNĐ)</th>
                    <th className="py-3 px-4 text-right">Dư nợ hiện tại (VNĐ)</th>
                    <th className="py-3 px-4 text-right">Hạn mức tín dụng</th>
                    <th className="py-3 px-4 text-center">Tình trạng tín dụng</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 text-sm">
                  {customerDebts.map((c) => {
                    const bal = Number(c.balanceAmount || 0);
                    const limit = Number(c.creditLimit || 0);
                    const isOverLimit = limit > 0 && bal > limit;

                    return (
                      <tr key={c.customerId} className="hover:bg-gray-50/80 transition-colors">
                        <td className="py-3 px-4 font-mono font-medium text-gray-600">#{c.customerId}</td>
                        <td className="py-3 px-4 font-medium text-gray-900">{c.customerName}</td>
                        <td className="py-3 px-4 text-gray-600 font-mono text-xs">{c.phone || '—'}</td>
                        <td className="py-3 px-4 text-right text-gray-700">
                          {Number(c.totalInvoiced).toLocaleString('vi-VN')}
                        </td>
                        <td className="py-3 px-4 text-right text-green-600">
                          {Number(c.totalPaid).toLocaleString('vi-VN')}
                        </td>
                        <td className="py-3 px-4 text-right">
                          <span className={`font-bold ${bal > 0 ? 'text-red-600' : 'text-gray-400'}`}>
                            {bal.toLocaleString('vi-VN')}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-right font-medium text-gray-700">
                          {limit.toLocaleString('vi-VN')}
                        </td>
                        <td className="py-3 px-4 text-center">
                          {isOverLimit ? (
                            <Badge variant="danger" className="inline-flex items-center gap-1">
                              <AlertTriangle size={12} /> Vượt hạn mức
                            </Badge>
                          ) : (
                            <Badge variant="success" className="inline-flex items-center gap-1">
                              <CheckCircle2 size={12} /> Trong an toàn
                            </Badge>
                          )}
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

      {/* Tab 2: Công nợ nhà cung cấp */}
      {activeTab === 'supplier' && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          {isSuppLoading ? (
            <div className="py-12 flex justify-center">
              <Loading />
            </div>
          ) : supplierDebts.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <Building2 size={48} className="mx-auto text-gray-300 mb-2" />
              <p>Không có dữ liệu công nợ nhà cung cấp</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-600 uppercase">
                    <th className="py-3 px-4">Mã NCC</th>
                    <th className="py-3 px-4">Tên nhà cung cấp</th>
                    <th className="py-3 px-4">Số điện thoại</th>
                    <th className="py-3 px-4 text-right">Tổng hàng nhập (VNĐ)</th>
                    <th className="py-3 px-4 text-right">Gara đã trả (VNĐ)</th>
                    <th className="py-3 px-4 text-right">Gara còn nợ (VNĐ)</th>
                    <th className="py-3 px-4 text-center">Thời hạn nợ</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 text-sm">
                  {supplierDebts.map((s) => {
                    const bal = Number(s.balanceAmount || 0);

                    return (
                      <tr key={s.supplierId} className="hover:bg-gray-50/80 transition-colors">
                        <td className="py-3 px-4 font-mono font-medium text-gray-600">#{s.supplierId}</td>
                        <td className="py-3 px-4 font-medium text-gray-900">{s.supplierName}</td>
                        <td className="py-3 px-4 text-gray-600 font-mono text-xs">{s.phone || '—'}</td>
                        <td className="py-3 px-4 text-right text-gray-700">
                          {Number(s.totalPurchased).toLocaleString('vi-VN')}
                        </td>
                        <td className="py-3 px-4 text-right text-green-600">
                          {Number(s.totalPaid).toLocaleString('vi-VN')}
                        </td>
                        <td className="py-3 px-4 text-right">
                          <span className={`font-bold ${bal > 0 ? 'text-amber-600' : 'text-gray-400'}`}>
                            {bal.toLocaleString('vi-VN')}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-center">
                          <Badge variant="outline">{s.paymentTermDays} ngày</Badge>
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
    </div>
  );
}
