import { useState, useEffect } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { money } from '../../../shared/utils/format';
import {
  Badge,
  Button,
  ConfirmDialog,
  Field,
  FormError,
  Sheet,
} from '../../../shared/components/ui';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { useRepairOrdersQuery } from '../../repair/hooks/useRepair';
import { useCreateEstimateMutation } from '../hooks/useEstimate';
import { useEstimatesQuery } from '../hooks/useEstimates';
import { newEstimateMode } from '../../repair/utils/stageLocks';
import type { EstimateItemCreateInput, ItemType } from '../types/estimate';
import { EstimateLinePicker, type PickedCatalogLine } from './EstimateLinePicker';

const DEFAULT_VALID_DAYS = 7;
const MS_PER_DAY = 24 * 60 * 60 * 1000;

const emptyItem = (itemType: ItemType): EstimateItemCreateInput => ({
  itemType,
  serviceId: null,
  partId: null,
  itemName: '',
  unit: '',
  quantity: 1,
  unitPrice: 0,
});

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

  const [repairOrderId, setRepairOrderId] = useState<string>('');
  const [notes, setNotes] = useState('');
  const [validDays, setValidDays] = useState(DEFAULT_VALID_DAYS);
  const [items, setItems] = useState<EstimateItemCreateInput[]>([emptyItem('SERVICE')]);

  const { data: ordersData } = useRepairOrdersQuery({ size: 100 });
  // Only orders that can still take a new estimate (before QC); the kind follows the order's stage.
  const orders = (ordersData?.content ?? []).filter((o) =>
    [
      'NEW',
      'DIAGNOSING',
      'WAITING_APPROVAL',
      'APPROVED',
      'IN_PROGRESS',
      'WAITING_PARTS',
      'QC_FAILED',
    ].includes(o.status),
  );
  const selectedOrder = orders.find((o) => String(o.id) === repairOrderId);
  const { data: orderEstimates = [] } = useEstimatesQuery(repairOrderId || null);
  const mode = selectedOrder ? newEstimateMode(selectedOrder.status, orderEstimates) : null;

  useEffect(() => {
    if (open) {
      setRepairOrderId('');
      setNotes('');
      setValidDays(DEFAULT_VALID_DAYS);
      setItems([emptyItem('SERVICE')]);
      setDiscard(false);
    }
  }, [open]);

  const isDirty = !!repairOrderId || items.length > 1 || items.some((item) => item.itemName);
  const busy = createMutation.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  const updateItem = (index: number, patch: Partial<EstimateItemCreateInput>) =>
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, ...patch } : item)));

  const pickCatalog = (index: number, line: PickedCatalogLine) => updateItem(index, line);

  const handleRemoveItem = (index: number) => {
    if (items.length <= 1) return;
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const subtotal = items.reduce(
    (sum, item) => sum + (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0),
    0,
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!repairOrderId) {
      notify('Vui lòng chọn Lệnh sửa chữa');
      return;
    }
    if (!mode) {
      notify('Phiếu này không còn lập được báo giá ở giai đoạn hiện tại.');
      return;
    }
    const missing = items.findIndex((item) =>
      item.itemType === 'SERVICE' ? !item.serviceId : !item.partId,
    );
    if (missing >= 0) {
      notify(`Hạng mục ${missing + 1}: hãy chọn dịch vụ hoặc phụ tùng từ danh mục.`);
      return;
    }

    try {
      await createMutation.mutateAsync({
        repairOrderId: Number(repairOrderId),
        estimateKind: mode.kind,
        parentEstimateId: mode.kind === 'SUPPLEMENTAL' ? Number(mode.parentEstimateId) : null,
        notes: notes.trim() || undefined,
        validUntil: new Date(Date.now() + Number(validDays) * MS_PER_DAY).toISOString(),
        items: items.map((item) => ({
          ...item,
          quantity: Number(item.quantity),
          unitPrice: Number(item.unitPrice),
        })),
      });
      notify('Lập báo giá thành công');
      onSaved();
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Không thể tạo báo giá');
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
        size="lg"
        title="Lập báo giá dịch vụ & phụ tùng"
        description="Lập bản dự toán chi phí sửa chữa và thời hạn bảo hành gửi khách duyệt"
        onClose={handleClose}
      >
        <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
          <FormError
            message={createMutation.error ? errorMessage(createMutation.error) : undefined}
          />

          <div className="form-section-label">THÔNG TIN CHUNG BÁO GIÁ</div>

          <Field label="Lệnh sửa chữa liên kết" required>
            <select
              className="select"
              value={repairOrderId}
              onChange={(e) => setRepairOrderId(e.target.value)}
              disabled={busy}
              required
            >
              <option value="">Chọn phiếu sửa chữa</option>
              {orders.map((o) => (
                <option key={o.id} value={o.id}>
                  #{o.orderCode} · {o.status}
                </option>
              ))}
            </select>
          </Field>

          <Field
            label="Loại báo giá"
            hint={
              mode?.kind === 'SUPPLEMENTAL'
                ? 'Khách đã duyệt báo giá trước, hạng mục mới được lập thành báo giá phát sinh.'
                : undefined
            }
          >
            <input
              className="input"
              readOnly
              tabIndex={-1}
              value={
                !selectedOrder
                  ? 'Chọn phiếu để xác định'
                  : mode?.kind === 'SUPPLEMENTAL'
                    ? 'Báo giá phát sinh thêm'
                    : mode
                      ? 'Báo giá ban đầu'
                      : 'Phiếu không còn lập được báo giá'
              }
            />
          </Field>

          <Field label="Hiệu lực báo giá (số ngày)">
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

          <div className="full-width-field estimate-lines">
            <div className="estimate-lines-head">
              <span>Hạng mục báo giá ({items.length})</span>
              <div className="row-actions">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setItems((prev) => [...prev, emptyItem('SERVICE')])}
                >
                  <Plus size={13} /> Thêm dịch vụ
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setItems((prev) => [...prev, emptyItem('PART')])}
                >
                  <Plus size={13} /> Thêm phụ tùng
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
                    disabled={busy}
                    onPick={(line) => pickCatalog(idx, line)}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    className="delete-action"
                    aria-label={`Xóa hạng mục ${idx + 1}`}
                    onClick={() => handleRemoveItem(idx)}
                    disabled={items.length <= 1 || busy}
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
                      disabled={busy}
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
                      disabled={busy}
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
              <span>Tổng cộng dự toán</span>
              <span>{money(subtotal)}</span>
            </div>
          </div>

          <div className="form-actions account-editor-actions">
            <Button disabled={busy} onClick={handleClose}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={busy}>
              {busy ? 'Đang tạo…' : 'Lập báo giá'}
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
