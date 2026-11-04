import { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Badge, Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { money } from '../../../shared/utils/format';
import { useCreateEstimateMutation } from '../../estimate/hooks/useEstimates';
import { EstimateLinePicker } from '../../estimate/components/EstimateLinePicker';
import type { EstimateItemCreate, ItemType } from '../../estimate/types/estimateTypes';

const emptyItem = (itemType: ItemType): EstimateItemCreate => ({
  itemType,
  serviceId: null,
  partId: null,
  itemName: '',
  unit: '',
  quantity: 1,
  unitPrice: 0,
});

export function CreateEstimateModal({
  orderId,
  open,
  onClose,
  parentEstimateId,
}: {
  orderId: string | number;
  open: boolean;
  onClose: () => void;
  /** Set once the customer has approved: the new estimate is then a supplement to this one. */
  parentEstimateId?: string | number | null;
}) {
  const supplemental = parentEstimateId != null;
  const notify = useToast();
  const createEstimate = useCreateEstimateMutation();

  const [notes, setNotes] = useState('');
  const [items, setItems] = useState<EstimateItemCreate[]>([emptyItem('SERVICE')]);

  const updateItem = (index: number, patch: Partial<EstimateItemCreate>) =>
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, ...patch } : item)));

  const totalAmount = items.reduce(
    (sum, item) => sum + (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0),
    0,
  );

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const missing = items.findIndex((item) =>
      item.itemType === 'SERVICE' ? !item.serviceId : !item.partId,
    );
    if (missing >= 0) {
      notify(`Hạng mục ${missing + 1}: hãy chọn dịch vụ hoặc phụ tùng từ danh mục.`);
      return;
    }

    try {
      await createEstimate.mutateAsync({
        repairOrderId: orderId,
        estimateKind: supplemental ? 'SUPPLEMENTAL' : 'INITIAL',
        parentEstimateId: supplemental ? parentEstimateId : null,
        notes: notes.trim() || undefined,
        items: items.map((item) => ({
          itemType: item.itemType,
          serviceId: item.serviceId,
          partId: item.partId,
          itemName: item.itemName.trim(),
          unit: item.unit.trim() || 'Cái',
          quantity: Number(item.quantity) || 1,
          unitPrice: Number(item.unitPrice) || 0,
        })),
      });
      notify(supplemental ? 'Đã lập báo giá phát sinh.' : 'Đã tạo bản báo giá mới thành công!');
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
      title={supplemental ? 'Lập báo giá phát sinh' : 'Lập bản báo giá mới'}
      description={
        supplemental
          ? 'Hạng mục phát sinh trong lúc sửa chữa, gửi khách duyệt riêng. Báo giá đã duyệt giữ nguyên.'
          : 'Chọn dịch vụ và phụ tùng từ danh mục để gửi khách hàng phê duyệt.'
      }
    >
      <form onSubmit={handleSubmit} noValidate className="editor-form">
        <FormError
          message={createEstimate.error ? errorMessage(createEstimate.error) : undefined}
        />

        <Field label="Ghi chú báo giá">
          <input
            className="input"
            placeholder="VD: Báo giá bảo dưỡng cấp 20.000 km theo khuyến cáo nhà sản xuất..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </Field>

        <div className="estimate-lines">
          <div className="estimate-lines-head">
            <span>Dịch vụ & phụ tùng ({items.length})</span>
            <div className="row-actions">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setItems((prev) => [...prev, emptyItem('SERVICE')])}
              >
                <Plus size={14} /> Thêm dịch vụ
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => setItems((prev) => [...prev, emptyItem('PART')])}
              >
                <Plus size={14} /> Thêm phụ tùng
              </Button>
            </div>
          </div>

          {items.map((item, idx) => (
            <div key={idx} className="estimate-line">
              <div className="estimate-line-main">
                <Badge variant={item.itemType === 'SERVICE' ? 'primary' : 'warning'}>
                  {item.itemType === 'SERVICE' ? 'Dịch vụ' : 'Phụ tùng'}
                </Badge>
                <EstimateLinePicker
                  itemType={item.itemType}
                  serviceId={item.serviceId}
                  partId={item.partId}
                  onPick={(line) => updateItem(idx, line)}
                />
                <Button
                  type="button"
                  variant="ghost"
                  className="delete-action"
                  aria-label={`Xóa hạng mục ${idx + 1}`}
                  onClick={() => setItems((prev) => prev.filter((_, i) => i !== idx))}
                  disabled={items.length === 1}
                >
                  <Trash2 size={15} />
                </Button>
              </div>
              <div className="estimate-line-fields">
                <label>
                  Đơn vị
                  <input className="input" value={item.unit} readOnly tabIndex={-1} />
                </label>
                <label>
                  Số lượng
                  <input
                    type="number"
                    min="1"
                    className="input"
                    value={item.quantity}
                    onChange={(e) => updateItem(idx, { quantity: Number(e.target.value) })}
                  />
                </label>
                <label>
                  Đơn giá
                  <input
                    type="number"
                    min="0"
                    step="1000"
                    className="input"
                    value={item.unitPrice}
                    onChange={(e) => updateItem(idx, { unitPrice: Number(e.target.value) })}
                  />
                </label>
                <label>
                  Thành tiền
                  <input
                    className="input"
                    readOnly
                    tabIndex={-1}
                    value={money((Number(item.quantity) || 0) * (Number(item.unitPrice) || 0))}
                  />
                </label>
              </div>
            </div>
          ))}

          <div className="estimate-total">
            <span>Tổng cộng dự kiến</span>
            <span>{money(totalAmount)}</span>
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
