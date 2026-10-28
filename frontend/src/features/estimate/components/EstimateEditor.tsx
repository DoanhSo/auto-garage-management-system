import { useState, useEffect } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { money } from '../../../shared/utils/format';
import { Button, ConfirmDialog, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { useRepairOrdersQuery } from '../../repair/hooks/useRepair';
import { useCreateEstimateMutation } from '../hooks/useEstimate';
import type { EstimateKind, EstimateItemCreateInput, ItemType } from '../types/estimate';

export function EstimateEditor({
  open,
  onClose,
  onSaved,
}: {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const notify = useToast();
  const createMutation = useCreateEstimateMutation();
  const [discard, setDiscard] = useState(false);

  // Form states
  const [repairOrderId, setRepairOrderId] = useState<string>('');
  const [estimateKind, setEstimateKind] = useState<EstimateKind>('INITIAL');
  const [notes, setNotes] = useState('');
  const [validDays, setValidDays] = useState(7);

  // Line items state
  const [items, setItems] = useState<EstimateItemCreateInput[]>([
    {
      itemType: 'SERVICE',
      itemName: 'Công khám & chẩn đoán tổng quát',
      unit: 'Gói',
      quantity: 1,
      unitPrice: 200000,
    },
  ]);

  // Active repair orders
  const { data: ordersData } = useRepairOrdersQuery({ size: 100 });
  const orders = ordersData?.content ?? [];

  useEffect(() => {
    if (open) {
      setRepairOrderId('');
      setEstimateKind('INITIAL');
      setNotes('');
      setValidDays(7);
      setItems([
        {
          itemType: 'SERVICE',
          itemName: 'Công khám & chẩn đoán tổng quát',
          unit: 'Gói',
          quantity: 1,
          unitPrice: 200000,
        },
      ]);
      setDiscard(false);
    }
  }, [open]);

  const isDirty = !!repairOrderId || items.length > 1;
  const busy = createMutation.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  const handleAddItem = (type: ItemType) => {
    setItems((prev) => [
      ...prev,
      {
        itemType: type,
        itemName: type === 'SERVICE' ? 'Công dịch vụ mới' : 'Phụ tùng thay thế',
        unit: type === 'SERVICE' ? 'Lượt' : 'Cái',
        quantity: 1,
        unitPrice: type === 'SERVICE' ? 150000 : 350000,
      },
    ]);
  };

  const handleRemoveItem = (index: number) => {
    if (items.length <= 1) return;
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: keyof EstimateItemCreateInput, val: string | number) => {
    setItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, [field]: val } : item)),
    );
  };

  const subtotal = items.reduce((sum, item) => sum + (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0), 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!repairOrderId) {
      notify('Vui lòng chọn Lệnh sửa chữa');
      return;
    }
    if (items.length === 0) {
      notify('Báo giá phải có ít nhất một hạng mục');
      return;
    }

    try {
      await createMutation.mutateAsync({
        repairOrderId: Number(repairOrderId),
        estimateKind,
        notes: notes.trim() || undefined,
        validDays: Number(validDays),
        items: items.map((it) => ({
          ...it,
          quantity: Number(it.quantity),
          unitPrice: Number(it.unitPrice),
        })),
      });
      notify('Lập báo giá thành công');
      onSaved();
      onClose();
    } catch {
      notify('Không thể tạo báo giá');
    }
  };

  const handleClose = () => {
    if (busy) return;
    if (isDirty) setDiscard(true);
    else onClose();
  };

  return (
    <>
      <Sheet
        open={open}
        placement="center"
        title="Lập Báo giá dịch vụ & phụ tùng (M07)"
        description="Lập bản dự toán chi phí sửa chữa và thời hạn bảo hành gửi khách duyệt"
        onClose={handleClose}
      >
        <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
          <FormError message={createMutation.error?.message} />

          <div className="form-section-label">THÔNG TIN CHUNG BÁO GIÁ</div>

          <Field label="Lệnh sửa chữa liên kết" required>
            <select
              className="select"
              value={repairOrderId}
              onChange={(e) => setRepairOrderId(e.target.value)}
              disabled={busy}
              required
            >
              <option value="">-- Chọn phiếu sửa chữa --</option>
              {orders.map((o) => (
                <option key={o.id} value={o.id}>
                  #{o.orderCode} (Xe: {o.vehicleId})
                </option>
              ))}
            </select>
          </Field>

          <Field label="Loại báo giá" required>
            <select
              className="select"
              value={estimateKind}
              onChange={(e) => setEstimateKind(e.target.value as EstimateKind)}
              disabled={busy}
            >
              <option value="INITIAL">Báo giá ban đầu (INITIAL)</option>
              <option value="SUPPLEMENTAL">Báo giá phát sinh thêm (SUPPLEMENTAL)</option>
            </select>
          </Field>

          <Field label="Hiệu lực báo giá (Số ngày)">
            <input
              type="number"
              min="1"
              max="30"
              className="input"
              value={validDays}
              onChange={(e) => setValidDays(Number(e.target.value))}
              disabled={busy}
            />
          </Field>

          <Field label="Ghi chú báo giá">
            <input
              type="text"
              placeholder="Ghi chú điều khoản, bảo hành..."
              className="input"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              disabled={busy}
            />
          </Field>

          {/* Danh sách các dòng báo giá */}
          <div className="full-width-field space-y-2 mt-2">
            <div className="flex justify-between items-center">
              <span className="text-xs font-semibold text-muted-foreground uppercase">Hạng mục báo giá</span>
              <div className="flex gap-2">
                <Button type="button" variant="secondary" onClick={() => handleAddItem('SERVICE')}>
                  <Plus size={13} /> Thêm Dịch vụ
                </Button>
                <Button type="button" variant="secondary" onClick={() => handleAddItem('PART')}>
                  <Plus size={13} /> Thêm Phụ tùng
                </Button>
              </div>
            </div>

            <div className="space-y-2 max-h-[220px] overflow-y-auto border rounded-lg p-2 bg-card/40">
              {items.map((item, idx) => (
                <div key={idx} className="grid grid-cols-1 md:grid-cols-12 gap-2 items-center p-2 border rounded bg-card text-xs">
                  <div className="md:col-span-2">
                    <span className={`px-2 py-0.5 rounded font-medium ${item.itemType === 'SERVICE' ? 'bg-blue-100 text-blue-800' : 'bg-amber-100 text-amber-800'}`}>
                      {item.itemType === 'SERVICE' ? 'Dịch vụ' : 'Phụ tùng'}
                    </span>
                  </div>
                  <div className="md:col-span-4">
                    <input
                      type="text"
                      className="input text-xs py-1"
                      placeholder="Tên hạng mục"
                      value={item.itemName}
                      onChange={(e) => handleItemChange(idx, 'itemName', e.target.value)}
                      required
                    />
                  </div>
                  <div className="md:col-span-2">
                    <input
                      type="number"
                      min="1"
                      className="input text-xs py-1"
                      placeholder="Số lượng"
                      value={item.quantity}
                      onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                      required
                    />
                  </div>
                  <div className="md:col-span-3">
                    <input
                      type="number"
                      step="50000"
                      min="0"
                      className="input text-xs py-1"
                      placeholder="Đơn giá"
                      value={item.unitPrice}
                      onChange={(e) => handleItemChange(idx, 'unitPrice', e.target.value)}
                      required
                    />
                  </div>
                  <div className="md:col-span-1 text-right">
                    <button
                      type="button"
                      className="text-red-500 hover:text-red-700 p-1"
                      onClick={() => handleRemoveItem(idx)}
                      disabled={items.length <= 1}
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="text-right text-sm font-semibold pt-1">
              Tổng cộng dự toán: <span className="text-primary text-base">{money(subtotal)}</span>
            </div>
          </div>

          <div className="form-actions account-editor-actions">
            <Button disabled={busy} onClick={handleClose}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={busy}>
              {busy ? 'Đang tạo…' : 'Lập Báo giá'}
            </Button>
          </div>
        </form>
      </Sheet>

      <ConfirmDialog
        open={discard || blocker.state === 'blocked'}
        title="Bỏ thay đổi chưa lưu?"
        description="Nội dung báo giá đang nhập dở sẽ bị hủy."
        onCancel={() => {
          setDiscard(false);
          blocker.reset?.();
        }}
        onConfirm={() => {
          setDiscard(false);
          blocker.proceed?.();
          onClose();
        }}
      />
    </>
  );
}
