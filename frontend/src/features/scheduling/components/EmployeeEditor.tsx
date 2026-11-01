import { useEffect, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useUsersQuery } from '../../identity/hooks/useUsers';
import { useCreateEmployeeMutation, useUpdateEmployeeMutation } from '../hooks/useScheduling';
import { SKILL_LABELS, parseSkills, type Employee } from '../types/scheduling';

export function EmployeeEditor({
  employee,
  takenUserIds,
  open,
  onClose,
  onSaved,
}: {
  employee: Employee | null;
  takenUserIds: Set<string>;
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const notify = useToast();
  const createMutation = useCreateEmployeeMutation();
  const updateMutation = useUpdateEmployeeMutation();
  const users = useUsersQuery({
    page: 0,
    size: 100,
    sortBy: 'fullName',
    sortDir: 'asc',
    active: true,
  });

  const [userId, setUserId] = useState('');
  const [code, setCode] = useState('');
  const [skills, setSkills] = useState<string[]>([]);
  const [canQc, setCanQc] = useState(false);
  const [hourlyCost, setHourlyCost] = useState('0');
  const [baseSalary, setBaseSalary] = useState('0');
  const [overtimeMultiplier, setOvertimeMultiplier] = useState('1.5');
  const [commissionRate, setCommissionRate] = useState('10');

  useEffect(() => {
    if (!open) return;
    setUserId(employee ? String(employee.userId) : '');
    setCode(employee?.employeeCode ?? '');
    setSkills(parseSkills(employee?.skillsJson));
    setCanQc(employee?.canQc ?? false);
    setHourlyCost(String(employee?.hourlyCost ?? 0));
    setBaseSalary(String(employee?.baseSalary ?? 0));
    setOvertimeMultiplier(String(employee?.overtimeMultiplier ?? 1.5));
    setCommissionRate(String(employee?.defaultCommissionRate ?? 10));
  }, [open, employee]);

  const busy = createMutation.isPending || updateMutation.isPending;
  const candidates = (users.data?.content ?? []).filter((u) => u.role !== 'CUSTOMER');

  const toggleSkill = (key: string) =>
    setSkills((prev) => (prev.includes(key) ? prev.filter((s) => s !== key) : [...prev, key]));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cost = Number(hourlyCost);
    if (!Number.isFinite(cost) || cost < 0) {
      notify('Đơn giá giờ công không hợp lệ');
      return;
    }
    const salary = Number(baseSalary);
    const multiplier = Number(overtimeMultiplier);
    const rate = Number(commissionRate);
    if (!Number.isFinite(salary) || salary < 0) {
      notify('Lương cứng không hợp lệ');
      return;
    }
    if (!Number.isFinite(multiplier) || multiplier < 1) {
      notify('Hệ số tăng ca phải từ 1.0 trở lên');
      return;
    }
    if (!Number.isFinite(rate) || rate < 0 || rate > 100) {
      notify('Tỷ lệ hoa hồng phải nằm trong khoảng 0–100%');
      return;
    }
    const skillsJson = JSON.stringify(skills);
    try {
      if (employee) {
        await updateMutation.mutateAsync({
          id: employee.id,
          data: {
            skillsJson,
            canQc,
            hourlyCost: cost,
            baseSalary: salary,
            overtimeMultiplier: multiplier,
            defaultCommissionRate: rate,
          },
        });
        notify('Đã cập nhật hồ sơ kỹ thuật viên');
      } else {
        if (!userId || !code.trim()) {
          notify('Chọn tài khoản và nhập mã kỹ thuật viên');
          return;
        }
        await createMutation.mutateAsync({
          userId: Number(userId),
          employeeCode: code.trim(),
          skillsJson,
          canQc,
          hourlyCost: cost,
          baseSalary: salary,
          overtimeMultiplier: multiplier,
          defaultCommissionRate: rate,
        });
        notify('Đã tạo hồ sơ kỹ thuật viên');
      }
      onSaved();
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const mutationError = createMutation.error ?? updateMutation.error;

  return (
    <Sheet
      open={open}
      placement="center"
      title={employee ? `Sửa hồ sơ ${employee.employeeCode}` : 'Thêm kỹ thuật viên'}
      description="Hồ sơ năng lực dùng để xếp ca và giao việc sửa chữa."
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
        <FormError message={mutationError ? errorMessage(mutationError) : undefined} />
        <Field label="Tài khoản nhân viên" required>
          <select
            className="select"
            value={userId}
            disabled={!!employee || busy}
            onChange={(e) => setUserId(e.target.value)}
          >
            <option value="">-- Chọn tài khoản --</option>
            {candidates
              .filter((u) => !takenUserIds.has(String(u.id)) || String(u.id) === userId)
              .map((u) => (
                <option key={u.id} value={u.id}>
                  {u.fullName} ({u.username})
                </option>
              ))}
          </select>
        </Field>
        <Field label="Mã kỹ thuật viên" required>
          <input
            className="input"
            placeholder="VD: KTV-001"
            maxLength={30}
            value={code}
            disabled={!!employee || busy}
            onChange={(e) => setCode(e.target.value)}
          />
        </Field>
        <Field label="Đơn giá giờ công (đ)" required>
          <input
            type="number"
            min="0"
            step="1000"
            className="input"
            value={hourlyCost}
            disabled={busy}
            onChange={(e) => setHourlyCost(e.target.value)}
          />
        </Field>
        <Field label="Lương cứng cơ bản (đ/tháng)">
          <input
            type="number"
            min="0"
            step="100000"
            className="input"
            value={baseSalary}
            disabled={busy}
            onChange={(e) => setBaseSalary(e.target.value)}
          />
        </Field>
        <Field label="Hệ số tăng ca">
          <select
            className="select"
            value={overtimeMultiplier}
            disabled={busy}
            onChange={(e) => setOvertimeMultiplier(e.target.value)}
          >
            <option value="1">1.0x</option>
            <option value="1.5">1.5x</option>
            <option value="2">2.0x</option>
          </select>
        </Field>
        <Field label="Tỷ lệ hoa hồng tiền công (%)">
          <input
            type="number"
            min="0"
            max="100"
            step="0.5"
            className="input"
            value={commissionRate}
            disabled={busy}
            onChange={(e) => setCommissionRate(e.target.value)}
          />
        </Field>
        <Field label="Quyền kiểm định xuất xưởng">
          <label className="row-actions">
            <input type="checkbox" checked={canQc} onChange={(e) => setCanQc(e.target.checked)} />
            Được phép kiểm định chất lượng (QC)
          </label>
        </Field>
        <div className="full-width-field">
          <Field label="Chuyên môn kỹ thuật">
            <div className="row-actions">
              {Object.entries(SKILL_LABELS).map(([key, label]) => (
                <Button
                  key={key}
                  type="button"
                  variant={skills.includes(key) ? 'primary' : 'secondary'}
                  onClick={() => toggleSkill(key)}
                >
                  {label}
                </Button>
              ))}
            </div>
          </Field>
        </div>
        <div className="form-actions account-editor-actions">
          <Button onClick={onClose}>Hủy</Button>
          <Button variant="primary" type="submit" pending={busy}>
            {employee ? 'Lưu hồ sơ' : 'Tạo hồ sơ'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}
