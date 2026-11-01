import { useEffect, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Combobox, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useUsersQuery } from '../../identity/hooks/useUsers';
import { useCreateEmployeeMutation, useUpdateEmployeeMutation } from '../hooks/useScheduling';
import {
  BANK_NAMES,
  SKILL_LABELS,
  hourlyCostFromSalary,
  parseSkills,
  salaryRoleTemplates,
  type Employee,
} from '../types/scheduling';

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
  const [joinedAt, setJoinedAt] = useState('');
  const [bankName, setBankName] = useState('');
  const [bankAccountNumber, setBankAccountNumber] = useState('');
  const [bankAccountHolder, setBankAccountHolder] = useState('');
  const [role, setRole] = useState('');
  const [coefficient, setCoefficient] = useState('1');
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
    setJoinedAt(employee?.joinedAt ?? '');
    setBankName(employee?.bankName ?? '');
    setBankAccountNumber(employee?.bankAccountNumber ?? '');
    setBankAccountHolder(employee?.bankAccountHolder ?? '');
    setRole('');
    setCoefficient(String(employee?.salaryCoefficient ?? 1));
    setOvertimeMultiplier(String(employee?.overtimeMultiplier ?? 1.5));
    setCommissionRate(String(employee?.defaultCommissionRate ?? 10));
  }, [open, employee]);

  const busy = createMutation.isPending || updateMutation.isPending;
  const candidates = (users.data?.content ?? []).filter((u) => u.role !== 'CUSTOMER');

  const applyRole = (key: string) => {
    setRole(key);
    const template = salaryRoleTemplates[key];
    if (!template) return;
    const salary = Math.round(template.frameSalary * template.coefficient);
    setCoefficient(String(template.coefficient));
    setBaseSalary(String(salary));
    setHourlyCost(String(hourlyCostFromSalary(salary)));
    setCommissionRate(String(template.commissionRate));
    if (key === 'QC') setCanQc(true);
  };

  const changeCoefficient = (value: string) => {
    setCoefficient(value);
    const template = salaryRoleTemplates[role];
    const next = Number(value);
    if (!template || !Number.isFinite(next)) return;
    const salary = Math.round(template.frameSalary * next);
    setBaseSalary(String(salary));
    setHourlyCost(String(hourlyCostFromSalary(salary)));
  };

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
    const factor = Number(coefficient);
    if (!Number.isFinite(factor) || factor < 0.5 || factor > 3) {
      notify('Hệ số lương phải nằm trong khoảng 0.5–3.0');
      return;
    }
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
    if (bankAccountNumber && !/^\d{6,30}$/.test(bankAccountNumber.trim())) {
      notify('Số tài khoản ngân hàng chỉ gồm chữ số (6–30 ký tự)');
      return;
    }
    const hr = {
      joinedAt: joinedAt || undefined,
      bankName: bankName || undefined,
      bankAccountNumber: bankAccountNumber.trim() || undefined,
      bankAccountHolder: bankAccountHolder.trim().toUpperCase() || undefined,
    };
    const skillsJson = JSON.stringify(skills);
    try {
      if (employee) {
        await updateMutation.mutateAsync({
          id: employee.id,
          data: {
            ...hr,
            skillsJson,
            canQc,
            hourlyCost: cost,
            baseSalary: salary,
            salaryCoefficient: factor,
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
          ...hr,
          skillsJson,
          canQc,
          hourlyCost: cost,
          baseSalary: salary,
          salaryCoefficient: factor,
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
        <Field label="Ngày tham gia công ty">
          <input
            type="date"
            className="input"
            value={joinedAt}
            max={new Date().toISOString().slice(0, 10)}
            disabled={busy}
            onChange={(e) => setJoinedAt(e.target.value)}
          />
        </Field>
        <Field label="Ngân hàng nhận lương">
          <Combobox
            value={bankName}
            onChange={setBankName}
            disabled={busy}
            placeholder="Gõ để tìm ngân hàng…"
            options={BANK_NAMES.map((name) => ({ value: name, label: name }))}
          />
        </Field>
        <Field label="Số tài khoản">
          <input
            className="input"
            inputMode="numeric"
            maxLength={30}
            value={bankAccountNumber}
            disabled={busy}
            onChange={(e) => setBankAccountNumber(e.target.value)}
          />
        </Field>
        <Field label="Tên chủ tài khoản" hint="Viết hoa, không dấu">
          <input
            className="input"
            maxLength={100}
            value={bankAccountHolder}
            disabled={busy}
            onChange={(e) => setBankAccountHolder(e.target.value)}
          />
        </Field>
        <div className="full-width-field">
          <Field
            label="Vai trò / cấp bậc"
            hint="Chọn mẫu để tự điền khung lương, hệ số, đơn giá giờ công và hoa hồng; vẫn chỉnh tay được."
          >
            <select
              className="select"
              value={role}
              disabled={busy}
              onChange={(e) => applyRole(e.target.value)}
            >
              <option value="">-- Tự nhập (không theo mẫu) --</option>
              {Object.entries(salaryRoleTemplates).map(([key, t]) => (
                <option key={key} value={key}>
                  {t.label} · {t.frameSalary.toLocaleString('vi-VN')} đ × {t.coefficient}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <Field label="Hệ số lương" hint="Từ 0.5 đến 3.0, bước 0.05">
          <input
            type="number"
            min="0.5"
            max="3"
            step="0.05"
            className="input"
            value={coefficient}
            disabled={busy}
            onChange={(e) => changeCoefficient(e.target.value)}
          />
        </Field>
        <Field label="Đơn giá giờ công (đ)" required hint={`Lương cơ bản ÷ ${208} giờ công chuẩn`}>
          <div className="row-actions">
            <input
              type="number"
              min="0"
              step="500"
              className="input grow"
              value={hourlyCost}
              disabled={busy}
              onChange={(e) => setHourlyCost(e.target.value)}
            />
            <Button
              type="button"
              variant="secondary"
              disabled={busy}
              onClick={() => setHourlyCost(String(hourlyCostFromSalary(Number(baseSalary) || 0)))}
            >
              Tự quy đổi
            </Button>
          </div>
        </Field>
        <Field label="Lương cơ bản áp dụng (đ/tháng)">
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
