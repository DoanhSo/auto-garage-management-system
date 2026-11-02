import { useEffect, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Modal } from '../../../shared/components/ui';
import { money } from '../../../shared/utils/format';
import { useUpdateEmployeeMutation } from '../../scheduling/hooks/useScheduling';
import type { Employee } from '../../scheduling/types/scheduling';
import {
  SALARY_COEFFICIENT_MAX,
  SALARY_COEFFICIENT_MIN,
  SALARY_COEFFICIENT_PRESETS,
  STANDARD_SHIFT_HOURS,
  STANDARD_WORKDAYS_PER_MONTH,
} from '../types/payroll';

const RATE_ROUNDING = 500;
const DEFAULT_OVERTIME_MULTIPLIER = 1.5;

/** Hourly rate at coefficient 1.0, so a new coefficient can be applied to the same base. */
function baseRateOf(employee: Employee): number {
  const coefficient = Number(employee.salaryCoefficient ?? 1) || 1;
  return Number(employee.hourlyCost ?? 0) / coefficient;
}

function suggestedRate(employee: Employee, coefficient: number): number {
  return Math.round((baseRateOf(employee) * coefficient) / RATE_ROUNDING) * RATE_ROUNDING;
}

export function SalaryCoefficientModal({
  employee,
  onClose,
}: {
  employee: Employee | null;
  onClose: () => void;
}) {
  const notify = useToast();
  const update = useUpdateEmployeeMutation();
  const [coefficient, setCoefficient] = useState('1');
  const [rate, setRate] = useState('0');

  useEffect(() => {
    if (!employee) return;
    setCoefficient(String(Number(employee.salaryCoefficient ?? 1)));
    setRate(String(employee.hourlyCost ?? 0));
    update.reset();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [employee]);

  const factor = Number(coefficient);
  const hourly = Number(rate);
  const validFactor =
    Number.isFinite(factor) && factor >= SALARY_COEFFICIENT_MIN && factor <= SALARY_COEFFICIENT_MAX;
  const validRate = Number.isFinite(hourly) && hourly >= 0;
  const overtime = Number(employee?.overtimeMultiplier ?? DEFAULT_OVERTIME_MULTIPLIER);

  const pickCoefficient = (value: number) => {
    setCoefficient(String(value));
    if (employee) setRate(String(suggestedRate(employee, value)));
  };

  const save = async () => {
    if (!employee) return;
    if (!validFactor) {
      notify(
        `Hệ số lương phải nằm trong khoảng ${SALARY_COEFFICIENT_MIN}–${SALARY_COEFFICIENT_MAX}`,
      );
      return;
    }
    if (!validRate) {
      notify('Đơn giá giờ công không hợp lệ');
      return;
    }
    try {
      await update.mutateAsync({
        id: employee.id,
        data: { salaryCoefficient: factor, hourlyCost: hourly },
      });
      notify('Đã cập nhật hệ số và đơn giá lương');
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  return (
    <Modal
      open={!!employee}
      title="Điều chỉnh hệ số lương"
      description={employee ? `${employee.fullName ?? ''} · ${employee.employeeCode}` : ''}
      onOpenChange={(open) => !open && !update.isPending && onClose()}
    >
      <form
        className="editor-form account-editor-form"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          void save();
        }}
      >
        <FormError message={update.error ? errorMessage(update.error) : undefined} />
        <Field
          label="Hệ số lương"
          hint={`Chọn bậc chuẩn hoặc nhập ${SALARY_COEFFICIENT_MIN}–${SALARY_COEFFICIENT_MAX}`}
        >
          <input
            type="number"
            min={SALARY_COEFFICIENT_MIN}
            max={SALARY_COEFFICIENT_MAX}
            step="0.05"
            className="input"
            value={coefficient}
            onChange={(e) => setCoefficient(e.target.value)}
          />
        </Field>
        <Field label="Đơn giá giờ công (đ/giờ)" hint="Tự gợi ý khi chọn bậc hệ số; có thể sửa tay">
          <input
            type="number"
            min="0"
            step={RATE_ROUNDING}
            className="input"
            value={rate}
            onChange={(e) => setRate(e.target.value)}
          />
        </Field>
        <div
          className="full-width-field coeff-preset-row"
          role="group"
          aria-label="Bậc hệ số chuẩn"
        >
          {SALARY_COEFFICIENT_PRESETS.map((preset) => (
            <Button
              key={preset.value}
              variant={factor === preset.value ? 'primary' : 'secondary'}
              aria-pressed={factor === preset.value}
              onClick={() => pickCoefficient(preset.value)}
            >
              {preset.value.toFixed(2)}x · {preset.label}
            </Button>
          ))}
        </div>
        <div className="full-width-field coeff-preview-grid" aria-live="polite">
          <div>
            <span>1 giờ thường</span>
            <strong>{validRate ? money(hourly) : '—'}</strong>
          </div>
          <div>
            <span>1 giờ tăng ca ({overtime}x)</span>
            <strong>{validRate ? money(Math.round(hourly * overtime)) : '—'}</strong>
          </div>
          <div>
            <span>1 ca chuẩn ({STANDARD_SHIFT_HOURS} giờ)</span>
            <strong>{validRate ? money(hourly * STANDARD_SHIFT_HOURS) : '—'}</strong>
          </div>
          <div>
            <span>Ước tính {STANDARD_WORKDAYS_PER_MONTH} công/tháng</span>
            <strong>
              {validRate ? money(hourly * STANDARD_SHIFT_HOURS * STANDARD_WORKDAYS_PER_MONTH) : '—'}
            </strong>
          </div>
        </div>
        <div className="form-actions account-editor-actions">
          <Button onClick={onClose} disabled={update.isPending}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" pending={update.isPending}>
            Lưu thay đổi
          </Button>
        </div>
      </form>
    </Modal>
  );
}
