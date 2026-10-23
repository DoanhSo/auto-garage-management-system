import { useState } from 'react';
import { useReconciliationQuery } from '../hooks/useInventory';
import { Button, Badge, Loading } from '../../../shared/components/ui';
import { CheckCircle2, AlertTriangle, RefreshCw, Scale } from 'lucide-react';

export function StockReconciliationPage() {
  const [filterDiscrepancyOnly, setFilterDiscrepancyOnly] = useState(false);
  const { data, isLoading, refetch, isFetching } = useReconciliationQuery();

  const reconciliations = data || [];
  const matchedCount = reconciliations.filter((r) => r.matched).length;
  const discrepancyCount = reconciliations.filter((r) => !r.matched).length;

  const displayList = filterDiscrepancyOnly
    ? reconciliations.filter((r) => !r.matched)
    : reconciliations;

  return (
    <div className="page-container p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Scale className="text-blue-600" size={28} />
            Đối soát Sổ kho & Tồn thực tế (UC38)
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Kiểm tra và đối soát tính toàn vẹn giữa số tồn thực tế (parts) và tổng giao dịch nhập xuất trong sổ kho (inventory_transactions)
          </p>
        </div>
        <Button
          variant="secondary"
          onClick={() => refetch()}
          pending={isFetching}
          className="flex items-center gap-1.5"
        >
          <RefreshCw size={16} className={isFetching ? 'spin' : ''} />
          Đối soát lại toàn bộ
        </Button>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-blue-50 text-blue-600 rounded-lg">
            <Scale size={24} />
          </div>
          <div>
            <div className="text-xs text-gray-500 font-medium">Tổng phụ tùng đối soát</div>
            <div className="text-2xl font-bold text-gray-900">{reconciliations.length}</div>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-green-50 text-green-600 rounded-lg">
            <CheckCircle2 size={24} />
          </div>
          <div>
            <div className="text-xs text-gray-500 font-medium">Khớp số liệu (100% cân bằng)</div>
            <div className="text-2xl font-bold text-green-600">{matchedCount}</div>
          </div>
        </div>

        <div
          onClick={() => setFilterDiscrepancyOnly(!filterDiscrepancyOnly)}
          className={`cursor-pointer bg-white p-5 rounded-xl border shadow-sm flex items-center gap-4 transition-all ${
            filterDiscrepancyOnly
              ? 'border-amber-500 ring-2 ring-amber-100'
              : 'border-gray-100 hover:border-amber-200'
          }`}
        >
          <div className="p-3 bg-amber-50 text-amber-600 rounded-lg">
            <AlertTriangle size={24} />
          </div>
          <div>
            <div className="text-xs text-gray-500 font-medium">Có chênh lệch (Cần kiểm kê)</div>
            <div className="text-2xl font-bold text-amber-600">{discrepancyCount}</div>
          </div>
        </div>
      </div>

      {/* Reconciliation Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        {isLoading ? (
          <div className="py-12 flex justify-center">
            <Loading />
          </div>
        ) : displayList.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <CheckCircle2 size={48} className="mx-auto text-green-500 mb-2" />
            <p className="font-medium text-gray-800">Tất cả dữ liệu sổ kho đều hoàn toàn chuẩn xác!</p>
            <p className="text-xs text-gray-400 mt-1">Không phát hiện bất kỳ sai lệch nào giữa số tồn và lịch sử giao dịch kho.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-600 uppercase">
                  <th className="py-3 px-4">Mã SKU</th>
                  <th className="py-3 px-4">Tên phụ tùng</th>
                  <th className="py-3 px-4 text-right">Tồn kho hiện tại</th>
                  <th className="py-3 px-4 text-right">Tổng phát sinh sổ kho</th>
                  <th className="py-3 px-4 text-right">Chênh lệch</th>
                  <th className="py-3 px-4 text-center">Kết quả đối soát</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {displayList.map((item) => (
                  <tr key={item.partId} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-3 px-4 font-mono font-medium text-gray-900">{item.sku}</td>
                    <td className="py-3 px-4 font-medium text-gray-800">{item.partName}</td>
                    <td className="py-3 px-4 text-right font-medium text-gray-800">
                      {item.stockQuantity}
                    </td>
                    <td className="py-3 px-4 text-right font-medium text-gray-800">
                      {item.ledgerQuantity}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <span
                        className={`font-semibold ${
                          Number(item.discrepancy) !== 0 ? 'text-red-600' : 'text-gray-400'
                        }`}
                      >
                        {Number(item.discrepancy) > 0 ? `+${item.discrepancy}` : item.discrepancy}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      {item.matched ? (
                        <Badge variant="success" className="inline-flex items-center gap-1">
                          <CheckCircle2 size={12} /> Khớp hoàn toàn
                        </Badge>
                      ) : (
                        <Badge variant="danger" className="inline-flex items-center gap-1">
                          <AlertTriangle size={12} /> Lệch số liệu
                        </Badge>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
