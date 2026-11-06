import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Badge, Button, Field, FormError, Loading, Sheet } from '../../../shared/components/ui';
import { money } from '../../../shared/utils/format';
import {
  useHandoverReadinessQuery,
  useInvoiceByOrderQuery,
  useSetItemWarrantyMutation,
} from '../../billing/hooks/useBilling';
import type { InvoiceItem } from '../../billing/types/billing';
import { useHandoverMutation } from '../hooks/useRepair';
import type { RepairOrder } from '../types/repairTypes';

const WARRANTY_ITEM_TYPES = new Set(['SERVICE', 'PART']);

function toCount(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? Math.floor(parsed) : 0;
}

/** One invoice line: shows its current warranty and lets staff add or change it before handover. */
function WarrantyRow({
  invoiceId,
  item,
  onError,
}: {
  invoiceId: string;
  item: InvoiceItem;
  onError: (message: string) => void;
}) {
  const notify = useToast();
  const save = useSetItemWarrantyMutation();
  const [days, setDays] = useState(String(item.warrantyDays));
  const [km, setKm] = useState(String(item.warrantyKm));
  const [terms, setTerms] = useState(item.warrantyTerms ?? '');

  const dirty =
    toCount(days) !== item.warrantyDays ||
    toCount(km) !== item.warrantyKm ||
    terms.trim() !== (item.warrantyTerms ?? '');

  async function handleSave() {
    try {
      await save.mutateAsync({
        invoiceId,
        itemId: item.id,
        data: { warrantyDays: toCount(days), warrantyKm: toCount(km), warrantyTerms: terms.trim() },
      });
      notify(`Đã lưu bảo hành cho "${item.itemName}"`);
      onError('');
    } catch (err: unknown) {
      onError(errorMessage(err));
    }
  }

  const hasWarranty = item.warrantyDays > 0 || item.warrantyKm > 0;
  return (
    <div className="handover-warranty-row">
      <div className="handover-warranty-name">
        <strong>{item.itemName}</strong>
        <Badge variant={hasWarranty ? 'success' : 'secondary'}>
          {hasWarranty ? 'Có bảo hành' : 'Chưa có bảo hành'}
        </Badge>
      </div>
      <div className="handover-warranty-fields">
        <Field label="Số ngày">
          <input
            type="number"
            min={0}
            max={3650}
            className="input"
            value={days}
            onChange={(e) => setDays(e.target.value)}
          />
        </Field>
        <Field label="Số km">
          <input
            type="number"
            min={0}
            max={1000000}
            className="input"
            value={km}
            onChange={(e) => setKm(e.target.value)}
          />
        </Field>
        <Field label="Điều khoản">
          <input
            className="input"
            maxLength={1000}
            value={terms}
            placeholder="Ví dụ: bảo hành chính hãng"
            onChange={(e) => setTerms(e.target.value)}
          />
        </Field>
        <Button
          type="button"
          variant="secondary"
          disabled={!dirty}
          pending={save.isPending}
          onClick={handleSave}
        >
          Lưu
        </Button>
      </div>
    </div>
  );
}

export function HandoverModal({
  order,
  open,
  onClose,
}: {
  order: RepairOrder;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const handover = useHandoverMutation();
  const readiness = useHandoverReadinessQuery(order.id, open);
  const invoice = useInvoiceByOrderQuery(order.id, open);

  const [odometerOut, setOdometerOut] = useState<number>(order.odometerIn);
  const [notes, setNotes] = useState('');
  const [warrantyError, setWarrantyError] = useState('');

  const blocked = readiness.data?.allowed === false;
  const warrantyItems = (invoice.data?.items ?? []).filter((item) =>
    WARRANTY_ITEM_TYPES.has(item.itemType),
  );

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (blocked) return;
    if (odometerOut < order.odometerIn) {
      notify(
        `Số km khi bàn giao không được nhỏ hơn số km lúc vào (${order.odometerIn.toLocaleString('vi-VN')} km)`,
      );
      return;
    }

    try {
      await handover.mutateAsync({
        id: order.id,
        odometerOut,
        notes: notes.trim() || undefined,
      });
      notify(`Đã bàn giao xe lệnh #${order.orderCode} thành công! Lệnh sửa chữa đã được đóng.`);
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
      title={`Bàn giao xe cho khách hàng • #${order.orderCode}`}
      description="Kiểm tra thanh toán, bổ sung bảo hành, ghi nhận ODO xuất xưởng và đóng lệnh sửa chữa."
    >
      <form onSubmit={handleSubmit} noValidate className="modal-form">
        <FormError message={handover.error ? errorMessage(handover.error) : undefined} />

        <section className="full-width handover-section" aria-label="Điều kiện thanh toán">
          <h4>Điều kiện thanh toán</h4>
          {readiness.isLoading && <Loading />}
          {readiness.isError && (
            <FormError message="Không kiểm tra được điều kiện thanh toán. Hệ thống vẫn kiểm tra lại khi bàn giao." />
          )}
          {readiness.data && (
            <div className="handover-payment">
              <dl className="handover-metrics">
                <div>
                  <dt>Còn nợ hóa đơn này</dt>
                  <dd>{money(readiness.data.invoiceBalance)}</dd>
                </div>
                <div>
                  <dt>Tổng công nợ của khách</dt>
                  <dd>{money(readiness.data.customerDebt)}</dd>
                </div>
                <div>
                  <dt>Hạn mức nợ</dt>
                  <dd>{money(readiness.data.creditLimit)}</dd>
                </div>
                <div>
                  <dt>Cần thu tối thiểu</dt>
                  <dd>{money(readiness.data.minimumPayment)}</dd>
                </div>
              </dl>
              {readiness.data.allowed ? (
                <Badge variant="success">Đủ điều kiện bàn giao</Badge>
              ) : (
                <>
                  <Badge variant="danger">Chưa đủ điều kiện bàn giao</Badge>
                  <ul className="handover-reasons">
                    {readiness.data.reasons.map((reason) => (
                      <li key={reason}>{reason}</li>
                    ))}
                  </ul>
                  <div className="handover-payment-actions">
                    <Link to="/billing/invoices" onClick={onClose}>
                      Mở Hóa đơn dịch vụ để thu tiền
                    </Link>
                    <Button
                      type="button"
                      variant="secondary"
                      pending={readiness.isFetching}
                      onClick={() => void readiness.refetch()}
                    >
                      Kiểm tra lại
                    </Button>
                  </div>
                </>
              )}
            </div>
          )}
        </section>

        <section className="full-width handover-section" aria-label="Bảo hành">
          <h4>Bảo hành theo dòng hóa đơn</h4>
          <p className="muted">
            Dòng chưa có bảo hành có thể bổ sung ngay bây giờ. Sổ bảo hành được tạo khi xác nhận bàn
            giao, tính từ ngày và số ODO bàn giao.
          </p>
          <FormError message={warrantyError || undefined} />
          {invoice.isLoading && <Loading />}
          {invoice.data && warrantyItems.length === 0 && (
            <p className="muted">Hóa đơn không có dòng dịch vụ hoặc phụ tùng để bảo hành.</p>
          )}
          {invoice.data &&
            warrantyItems.map((item) => (
              <WarrantyRow
                key={`${item.id}-${item.warrantyDays}-${item.warrantyKm}-${item.warrantyTerms ?? ''}`}
                invoiceId={invoice.data.id}
                item={item}
                onError={setWarrantyError}
              />
            ))}
        </section>

        <Field label="Số ODO khi vào (km)">
          <input className="input" disabled value={order.odometerIn.toLocaleString('vi-VN')} />
        </Field>

        <Field label="Số ODO khi bàn giao (km)" required>
          <input
            type="number"
            min={order.odometerIn}
            className="input"
            value={odometerOut}
            onChange={(e) => setOdometerOut(Number(e.target.value))}
            required
          />
        </Field>

        <div className="full-width">
          <Field label="Ghi chú bàn giao xe">
            <textarea
              className="input"
              rows={3}
              placeholder="Ghi nhận đồ dùng, tình trạng xe và dặn dò khách hàng khi nhận xe..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </Field>
        </div>

        <div className="modal-form-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" pending={handover.isPending} disabled={blocked}>
            {handover.isPending ? 'Đang xử lý…' : 'Xác nhận bàn giao xe'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}
