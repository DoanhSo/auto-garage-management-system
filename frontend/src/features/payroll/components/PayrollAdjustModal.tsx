import { useEffect, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Modal } from '../../../shared/components/ui';
import { useAdjustPayrollItemMutation } from '../hooks/usePayroll';
import type { PayrollItem } from '../types/payroll';

export function PayrollAdjustModal({
  item,
  onClose,
}: {
  item: PayrollItem | null;
  onClose: () => void;
}) {
  const notify = useToast();
  const adjust = useAdjustPayrollItemMutation();
  const [allowance, setAllowance] = useState('0');
  const [deduction, setDeduction] = useState('0');
  const [note, setNote] = useState('');

  useEffect(() => {
    if (item) {
      setAllowance(String(item.allowanceAmount));
      setDeduction(String(item.deductionAmount));
      setNote(item.notes ?? '');
      adjust.reset();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [item]);

  const save = async () => {
    if (!item) return;
    const allowanceValue = Number(allowance);
    const deductionValue = Number(deduction);
    if (
      !Number.isFinite(allowanceValue) ||
      allowanceValue < 0 ||
      !Number.isFinite(deductionValue) ||
      deductionValue < 0
    ) {
      notify('Phụ cấp và giảm trừ phải là số không âm');
      return;
    }
    try {
      await adjust.mutateAsync({
        periodId: item.payrollPeriodId,
        itemId: item.id,
        data: {
          allowanceAmount: allowanceValue,
          deductionAmount: deductionValue,
          notes: note.trim(),
        },
      });
      notify('Đã cập nhật phiếu lương');
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  return (
    <Modal
      open={!!item}
      title="Điều chỉnh phiếu lương"
      description={item ? `${item.employeeName ?? ''} · ${item.employeeCode ?? ''}` : ''}
      onOpenChange={(open) => !open && !adjust.isPending && onClose()}
    >
      <form
        className="editor-form account-editor-form"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          void save();
        }}
      >
        <FormError message={adjust.error ? errorMessage(adjust.error) : undefined} />
        <Field label="Phụ cấp (đ)">
          <input
            type="number"
            min="0"
            step="10000"
            className="input"
            value={allowance}
            onChange={(e) => setAllowance(e.target.value)}
          />
        </Field>
        <Field label="Giảm trừ / tạm ứng (đ)">
          <input
            type="number"
            min="0"
            step="10000"
            className="input"
            value={deduction}
            onChange={(e) => setDeduction(e.target.value)}
          />
        </Field>
        <div className="full-width-field">
          <Field label="Ghi chú">
            <input
              className="input"
              maxLength={2000}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </Field>
        </div>
        <div className="form-actions account-editor-actions">
          <Button onClick={onClose} disabled={adjust.isPending}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" pending={adjust.isPending}>
            Lưu điều chỉnh
          </Button>
        </div>
      </form>
    </Modal>
  );
}
