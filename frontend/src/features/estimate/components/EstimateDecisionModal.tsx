import { useState, useEffect } from 'react';
import { Car, Check, User, X } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { money, formatDateTime } from '../../../shared/utils/format';
import { Badge, Button, Field, Sheet } from '../../../shared/components/ui';
import { useDecideEstimateMutation, useSendEstimateMutation } from '../hooks/useEstimate';
import type { Estimate, LineDecisionInput } from '../types/estimate';
import { estimateStatusLabels } from '../types/estimate';
import type { Vehicle } from '../../../shared/types/contracts';
import type { RepairOrder } from '../../repair/types/repairTypes';

type Decision = LineDecisionInput['decision'];

export function EstimateDecisionModal({
  estimate,
  repairOrder,
  vehicle,
  open,
  onClose,
}: {
  estimate: Estimate | null;
  repairOrder?: RepairOrder | null;
  vehicle?: Vehicle | null;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const decideMutation = useDecideEstimateMutation();
  const sendMutation = useSendEstimateMutation();

  const [decisions, setDecisions] = useState<Record<string, Decision>>({});
  const [decisionNote, setDecisionNote] = useState('');

  useEffect(() => {
    if (estimate?.items) {
      const initial: Record<string, Decision> = {};
      estimate.items.forEach((item) => {
        initial[item.id] = item.approvalStatus === 'REJECTED' ? 'REJECTED' : 'APPROVED';
      });
      setDecisions(initial);
      setDecisionNote('');
    }
  }, [estimate]);

  if (!estimate) return null;

  const setAll = (decision: Decision) => {
    const next: Record<string, Decision> = {};
    estimate.items.forEach((it) => {
      next[it.id] = decision;
    });
    setDecisions(next);
  };

  const handleSend = async () => {
    try {
      await sendMutation.mutateAsync(estimate.id);
      notify('Đã chuyển báo giá sang trạng thái Đã gửi khách');
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Không thể gửi báo giá');
    }
  };

  const handleSubmit = async (sendFirst = false) => {
    const note = decisionNote.trim() || undefined;
    const pending = new Set(
      estimate.items.filter((it) => it.approvalStatus === 'PENDING').map((it) => it.id),
    );
    const lines: LineDecisionInput[] = Object.entries(decisions)
      .filter(([itemId]) => pending.has(itemId))
      .map(([itemId, decision]) => ({ estimateItemId: Number(itemId), decision, note }));

    try {
      if (sendFirst) await sendMutation.mutateAsync(estimate.id);
      await decideMutation.mutateAsync({ id: estimate.id, data: { lines } });
      notify('Đã lưu quyết định phê duyệt từng dòng báo giá');
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Lỗi khi phê duyệt báo giá');
    }
  };

  const approvedTotal = estimate.items
    .filter((it) => decisions[it.id] === 'APPROVED')
    .reduce((sum, it) => sum + (Number(it.lineTotal) || Number(it.quantity) * Number(it.unitPrice)), 0);

  return (
    <Sheet
      open={open}
      placement="center"
      size="lg"
      title={`Chi tiết & phê duyệt báo giá #${estimate.estimateCode}`}
      description={`Trạng thái: ${estimateStatusLabels[estimate.status] ?? estimate.status} • Bản v${estimate.versionNo}`}
      onClose={onClose}
    >
      <div className="editor-form">
        {(vehicle || repairOrder) && (
          <div className="info-strip">
            <User size={18} aria-hidden />
            <span>
              <strong>{vehicle?.customerName ?? 'Chưa rõ khách hàng'}</strong>
              {vehicle?.customerPhone && ` (${vehicle.customerPhone})`}
            </span>
            <Car size={18} aria-hidden />
            <span>
              <strong>{vehicle?.licensePlate ?? 'Chưa rõ biển số'}</strong>
              {vehicle && ` · ${vehicle.brand} ${vehicle.model}`}
              {repairOrder && ` · ODO vào xưởng ${repairOrder.odometerIn.toLocaleString('vi-VN')} km`}
              {repairOrder?.orderCode && ` · Lệnh #${repairOrder.orderCode}`}
            </span>
          </div>
        )}

        <div className="horizontal-summary-bar" aria-label="Thông số báo giá">
          <div>
            <span>Loại báo giá</span>
            <strong>{estimate.estimateKind === 'INITIAL' ? 'Ban đầu' : 'Phát sinh'}</strong>
          </div>
          <div>
            <span>Gửi khách lúc</span>
            <strong>{estimate.sentAt ? formatDateTime(estimate.sentAt) : 'Chưa gửi'}</strong>
          </div>
          <div>
            <span>Hiệu lực đến</span>
            <strong>
              {estimate.validUntil ? formatDateTime(estimate.validUntil) : 'Không giới hạn'}
            </strong>
          </div>
        </div>

        {estimate.notes && (
          <p className="description">
            <strong>Ghi chú:</strong> {estimate.notes}
          </p>
        )}

        {estimate.status === 'DRAFT' && (
          <div className="info-strip">
            <span>
              Báo giá đang là <strong>bản nháp</strong>. Gửi cho khách để bắt đầu duyệt.
            </span>
            <Button variant="primary" onClick={handleSend} pending={sendMutation.isPending}>
              Gửi khách hàng
            </Button>
          </div>
        )}

        <div className="estimate-lines-head">
          <span>Duyệt từng dòng — dòng được duyệt sẽ tạo việc sửa chữa và giữ phụ tùng trong kho</span>
          <div className="row-actions">
            <Button variant="secondary" onClick={() => setAll('APPROVED')}>
              Duyệt tất cả
            </Button>
            <Button variant="secondary" onClick={() => setAll('REJECTED')}>
              Từ chối tất cả
            </Button>
          </div>
        </div>

        <div className="estimate-lines">
          {estimate.items.map((it) => {
            const current = decisions[it.id] ?? 'APPROVED';
            return (
              <div
                key={it.id}
                className={`estimate-decision-row ${
                  current === 'APPROVED' ? 'is-approved' : 'is-rejected'
                }`}
              >
                <div className="estimate-decision-meta">
                  <div className="estimate-decision-line">
                    <Badge variant={it.itemType === 'SERVICE' ? 'primary' : 'warning'}>
                      {it.itemType === 'SERVICE' ? 'Dịch vụ' : 'Phụ tùng'}
                    </Badge>
                    <strong>{it.itemName}</strong>
                    <span className="muted">
                      • {it.quantity} {it.unit} × {money(it.unitPrice)} ={' '}
                    </span>
                    <strong>{money(it.lineTotal || it.quantity * it.unitPrice)}</strong>
                  </div>
                  {Boolean(it.warrantyDays || it.warrantyKm || it.reservedQuantity) && (
                    <span className="muted">
                      {it.warrantyDays ? `Bảo hành ${it.warrantyDays} ngày` : ''}
                      {it.warrantyKm ? ` / ${it.warrantyKm.toLocaleString('vi-VN')} km` : ''}
                      {it.reservedQuantity ? ` · Đã giữ kho ${it.reservedQuantity} ${it.unit}` : ''}
                    </span>
                  )}
                </div>
                <div className="estimate-decision-actions">
                  <Button
                    variant={current === 'APPROVED' ? 'primary' : 'ghost'}
                    disabled={it.approvalStatus !== 'PENDING'}
                    aria-pressed={current === 'APPROVED'}
                    onClick={() => setDecisions((prev) => ({ ...prev, [it.id]: 'APPROVED' }))}
                  >
                    <Check size={13} /> Duyệt
                  </Button>
                  <Button
                    variant={current === 'REJECTED' ? 'primary' : 'ghost'}
                    className={current === 'REJECTED' ? 'delete-action' : undefined}
                    disabled={it.approvalStatus !== 'PENDING'}
                    aria-pressed={current === 'REJECTED'}
                    onClick={() => setDecisions((prev) => ({ ...prev, [it.id]: 'REJECTED' }))}
                  >
                    <X size={13} /> Bỏ qua
                  </Button>
                </div>
              </div>
            );
          })}
        </div>

        <div className="estimate-total">
          <span>Tổng dự toán được duyệt</span>
          <span>{money(approvedTotal)}</span>
        </div>

        <Field label="Ghi chú phê duyệt">
          <input
            type="text"
            className="input"
            placeholder="Ý kiến của khách hàng, thỏa thuận thanh toán..."
            value={decisionNote}
            onChange={(e) => setDecisionNote(e.target.value)}
          />
        </Field>

        <div className="form-actions account-editor-actions">
          <Button onClick={onClose}>Đóng</Button>
          <Button
            variant="primary"
            pending={decideMutation.isPending || sendMutation.isPending}
            onClick={() => handleSubmit(estimate.status === 'DRAFT')}
          >
            {estimate.status === 'DRAFT' ? 'Gửi khách & lưu phê duyệt' : 'Lưu quyết định phê duyệt'}
          </Button>
        </div>
      </div>
    </Sheet>
  );
}
