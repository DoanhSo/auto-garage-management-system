import { useState, useEffect } from 'react';
import { Check, X } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { money } from '../../../shared/utils/format';
import { Button, Field, Sheet } from '../../../shared/components/ui';
import { useDecideEstimateMutation, useSendEstimateMutation } from '../hooks/useEstimate';
import type { Estimate, LineDecisionInput } from '../types/estimate';
import { estimateStatusLabels } from '../types/estimate';

export function EstimateDecisionModal({
  estimate,
  open,
  onClose,
}: {
  estimate: Estimate | null;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const decideMutation = useDecideEstimateMutation();
  const sendMutation = useSendEstimateMutation();

  const [decisions, setDecisions] = useState<Record<string, 'APPROVED' | 'REJECTED'>>({});
  const [decisionNote, setDecisionNote] = useState('');

  useEffect(() => {
    if (estimate?.items) {
      const initial: Record<string, 'APPROVED' | 'REJECTED'> = {};
      estimate.items.forEach((item) => {
        initial[item.id] = item.approvalStatus === 'REJECTED' ? 'REJECTED' : 'APPROVED';
      });
      setDecisions(initial);
      setDecisionNote('');
    }
  }, [estimate]);

  if (!estimate) return null;

  const handleToggle = (itemId: string, status: 'APPROVED' | 'REJECTED') => {
    setDecisions((prev) => ({ ...prev, [itemId]: status }));
  };

  const handleApproveAll = () => {
    const next: Record<string, 'APPROVED' | 'REJECTED'> = {};
    estimate.items.forEach((it) => {
      next[it.id] = 'APPROVED';
    });
    setDecisions(next);
  };

  const handleRejectAll = () => {
    const next: Record<string, 'APPROVED' | 'REJECTED'> = {};
    estimate.items.forEach((it) => {
      next[it.id] = 'REJECTED';
    });
    setDecisions(next);
  };

  const handleSend = async () => {
    try {
      await sendMutation.mutateAsync(estimate.id);
      notify('Đã chuyển trạng thái báo giá sang ĐÃ GỬI (SENT)');
    } catch {
      notify('Không thể gửi báo giá');
    }
  };

  const handleSubmit = async () => {
    const payload: LineDecisionInput[] = Object.entries(decisions).map(([itemId, status]) => ({
      itemId: Number(itemId),
      status,
    }));

    try {
      await decideMutation.mutateAsync({
        id: estimate.id,
        data: {
          decisions: payload,
          decisionNote: decisionNote.trim() || undefined,
        },
      });
      notify('Đã lưu quyết định phê duyệt từng dòng báo giá');
      onClose();
    } catch {
      notify('Lỗi khi phê duyệt báo giá');
    }
  };

  const approvedTotal = estimate.items
    .filter((it) => decisions[it.id] === 'APPROVED')
    .reduce((sum, it) => sum + (Number(it.lineTotal) || (Number(it.quantity) * Number(it.unitPrice))), 0);

  return (
    <Sheet
      open={open}
      placement="center"
      title={`Chi tiết & Phê duyệt Báo giá #${estimate.estimateCode}`}
      description={`Trạng thái: ${estimateStatusLabels[estimate.status]} • Bản số: v${estimate.versionNo} (${estimate.estimateKind})`}
      onClose={onClose}
    >
      <div className="space-y-4">
        {estimate.status === 'DRAFT' && (
          <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 rounded-lg flex justify-between items-center text-xs">
            <span>Báo giá đang ở trạng thái <strong>Bản nháp</strong>. Gửi cho khách để chính thức bắt đầu duyệt.</span>
            <Button variant="primary" onClick={handleSend} pending={sendMutation.isPending}>
              Gửi khách hàng
            </Button>
          </div>
        )}

        <div className="flex justify-between items-center text-xs text-muted-foreground pt-1">
          <span>Duyệt từng dòng: Dòng <strong>APPROVED</strong> sẽ tự động tạo Task sửa chữa & giữ phụ tùng trong kho.</span>
          <div className="flex gap-2">
            <Button variant="secondary" onClick={handleApproveAll}>
              Duyệt tất cả
            </Button>
            <Button variant="secondary" onClick={handleRejectAll}>
              Từ chối tất cả
            </Button>
          </div>
        </div>

        {/* Danh sách dòng duyệt */}
        <div className="space-y-2 max-h-[300px] overflow-y-auto border rounded-lg p-2 bg-card">
          {estimate.items.map((it) => {
            const currentDecision = decisions[it.id] || 'APPROVED';
            return (
              <div
                key={it.id}
                className={`p-3 border rounded-lg flex flex-wrap justify-between items-center gap-2 transition-colors ${
                  currentDecision === 'APPROVED' ? 'bg-green-50/40 dark:bg-green-950/20 border-green-200' : 'bg-red-50/40 dark:bg-red-950/20 border-red-200'
                }`}
              >
                <div>
                  <div className="font-semibold text-sm flex items-center gap-2">
                    <span className={`text-xs px-2 py-0.5 rounded font-normal ${it.itemType === 'SERVICE' ? 'bg-blue-100 text-blue-800' : 'bg-amber-100 text-amber-800'}`}>
                      {it.itemType === 'SERVICE' ? 'Dịch vụ' : 'Phụ tùng'}
                    </span>
                    {it.itemName}
                  </div>
                  <div className="text-xs text-muted-foreground mt-0.5">
                    Số lượng: {it.quantity} {it.unit} • Đơn giá: {money(it.unitPrice)} • Thành tiền: <strong className="text-foreground">{money(it.lineTotal || it.quantity * it.unitPrice)}</strong>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => handleToggle(it.id, 'APPROVED')}
                    className={`px-3 py-1 text-xs rounded-md font-medium flex items-center gap-1 transition-all ${
                      currentDecision === 'APPROVED'
                        ? 'bg-green-600 text-white shadow-sm'
                        : 'bg-muted text-muted-foreground hover:bg-green-100'
                    }`}
                  >
                    <Check size={13} /> Duyệt
                  </button>
                  <button
                    type="button"
                    onClick={() => handleToggle(it.id, 'REJECTED')}
                    className={`px-3 py-1 text-xs rounded-md font-medium flex items-center gap-1 transition-all ${
                      currentDecision === 'REJECTED'
                        ? 'bg-red-600 text-white shadow-sm'
                        : 'bg-muted text-muted-foreground hover:bg-red-100'
                    }`}
                  >
                    <X size={13} /> Bỏ qua
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        <div className="p-3 bg-muted/40 rounded-lg flex justify-between items-center text-sm">
          <span>Tổng dự toán được duyệt:</span>
          <span className="text-lg font-bold text-green-600 dark:text-green-400">{money(approvedTotal)}</span>
        </div>

        <Field label="Ghi chú phê duyệt">
          <input
            type="text"
            className="input"
            placeholder="Ghi chú ý kiến khách hàng, thỏa thuận thanh toán..."
            value={decisionNote}
            onChange={(e) => setDecisionNote(e.target.value)}
          />
        </Field>

        <div className="form-actions account-editor-actions pt-2 border-t">
          <Button onClick={onClose}>Đóng</Button>
          <Button
            variant="primary"
            pending={decideMutation.isPending}
            onClick={handleSubmit}
          >
            Lưu quyết định phê duyệt
          </Button>
        </div>
      </div>
    </Sheet>
  );
}
