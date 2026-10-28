import { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { money } from '../../../shared/utils/format';
import { useCreateEstimateMutation } from '../../estimate/hooks/useEstimates';
import type { EstimateItemCreate, ItemType } from '../../estimate/types/estimateTypes';

export function CreateEstimateModal({
  orderId,
  open,
  onClose,
}: {
  orderId: string | number;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const createEstimate = useCreateEstimateMutation();

  const [notes, setNotes] = useState('');
  const [items, setItems] = useState<EstimateItemCreate[]>([
    {
      itemType: 'SERVICE',
      itemName: 'Công kiểm tra và sửa chữa',
      unit: 'Gói',
      quantity: 1,
      unitPrice: 200000,
    },
  ]);

  const handleAddItem = (type: ItemType) => {
    setItems((prev) => [
      ...prev,
      {
        itemType: type,
        itemName: type === 'SERVICE' ? 'Dịch vụ bảo dưỡng' : 'Phụ tùng thay thế',
        unit: type === 'SERVICE' ? 'Lần' : 'Cái',
        quantity: 1,
        unitPrice: 100000,
      },
    ]);
  };

  const handleRemoveItem = (index: number) => {
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: keyof EstimateItemCreate, value: unknown) => {
    setItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, [field]: value } : item)),
    );
  };

  const totalAmount = items.reduce(
    (sum, item) => sum + (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0),
    0,
  );

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (items.length === 0) {
      notify('Báo giá phải có ít nhất 1 hạng mục');
      return;
    }

    try {
      await createEstimate.mutateAsync({
        repairOrderId: orderId,
        notes: notes.trim() || undefined,
        items: items.map((item) => ({
          itemType: item.itemType,
          itemName: item.itemName.trim(),
          unit: item.unit.trim() || 'Cái',
          quantity: Number(item.quantity) || 1,
          unitPrice: Number(item.unitPrice) || 0,
        })),
      });
      notify('Đã tạo bản báo giá mới thành công!');
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  }

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="lg"
      title="Lập bản báo giá mới"
      description="Lập danh sách chi phí dịch vụ và phụ tùng dự kiến để gửi khách hàng phê duyệt."
    >
      <form onSubmit={handleSubmit} noValidate className="editor-form space-y-4">
        <FormError message={createEstimate.error ? errorMessage(createEstimate.error) : undefined} />

        <Field label="Ghi chú báo giá">
          <input
            className="input"
            placeholder="VD: Báo giá bảo dưỡng cấp 20.000 km theo khuyến cáo nhà sản xuất..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </Field>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-sm">Danh mục dịch vụ & Phụ tùng ({items.length})</span>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="secondary"
                onClick={() => handleAddItem('SERVICE')}
              >
                <Plus size={14} /> + Tiền công
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => handleAddItem('PART')}
              >
                <Plus size={14} /> + Phụ tùng
              </Button>
            </div>
          </div>

          <div className="table-scroll border rounded-lg overflow-hidden">
            <table className="data-table">
              <thead>
                <tr>
                  <th style={{ width: '14%' }}>Phân loại</th>
                  <th style={{ width: '38%' }}>Tên hạng mục</th>
                  <th style={{ width: '12%' }}>ĐVT</th>
                  <th style={{ width: '10%' }} className="text-right">Số lượng</th>
                  <th style={{ width: '14%' }} className="text-right">Đơn giá</th>
                  <th style={{ width: '16%' }} className="text-right">Thành tiền</th>
                  <th style={{ width: '6%' }} className="text-center">Xóa</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => (
                  <tr key={idx}>
                    <td>
                      <span
                        className={`badge text-xs ${item.itemType === 'SERVICE' ? 'badge-info' : 'badge-neutral'}`}
                      >
                        {item.itemType === 'SERVICE' ? 'Dịch vụ' : 'Phụ tùng'}
                      </span>
                    </td>
                    <td>
                      <input
                        className="input text-sm"
                        placeholder="Tên hạng mục"
                        value={item.itemName}
                        onChange={(e) => handleItemChange(idx, 'itemName', e.target.value)}
                        required
                      />
                    </td>
                    <td>
                      <input
                        className="input text-sm"
                        placeholder="ĐVT"
                        value={item.unit}
                        onChange={(e) => handleItemChange(idx, 'unit', e.target.value)}
                        required
                      />
                    </td>
                    <td className="text-right">
                      <input
                        type="number"
                        min="1"
                        className="input text-sm text-right"
                        value={item.quantity}
                        onChange={(e) =>
                          handleItemChange(idx, 'quantity', Number(e.target.value))
                        }
                        required
                      />
                    </td>
                    <td className="text-right">
                      <input
                        type="number"
                        step="1000"
                        min="0"
                        className="input text-sm text-right"
                        value={item.unitPrice}
                        onChange={(e) =>
                          handleItemChange(idx, 'unitPrice', Number(e.target.value))
                        }
                        required
                      />
                    </td>
                    <td className="text-right font-medium money">
                      {money((Number(item.quantity) || 0) * (Number(item.unitPrice) || 0))}
                    </td>
                    <td className="text-center">
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => handleRemoveItem(idx)}
                        disabled={items.length === 1}
                        title="Xóa dòng"
                      >
                        <Trash2 size={15} className="text-danger" />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex justify-between items-center p-3 bg-muted/15 border rounded-lg font-bold">
            <span className="text-sm">Tổng cộng dự kiến:</span>
            <span className="text-primary text-base money">
              {money(totalAmount)}
            </span>
          </div>
        </div>

        <div className="modal-form-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" pending={createEstimate.isPending}>
            {createEstimate.isPending ? 'Đang tạo…' : 'Lưu bản báo giá'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}
