import { Calculator, UserCog } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { PageHeader } from '../../../shared/components/ui';
import { RelatedLinks } from '../../../shared/components/RelatedLinks';
import { useUrlTab } from '../../../shared/hooks/useUrlTab';
import { SalaryCoefficientManager } from '../../payroll/components/SalaryCoefficientManager';
import { EmployeeDirectory } from '../components/EmployeeDirectory';

const TABS = ['profiles', 'salary'] as const;

export function StaffPage() {
  const { user } = useAuth();
  const [tab, setTab] = useUrlTab('tab', TABS);

  return (
    <div className="page">
      <PageHeader
        eyebrow="Nhân sự & lương"
        title="Quản lý nhân viên"
        description="Hồ sơ, chuyên môn, quyền QC và đơn giá lương của toàn bộ nhân viên xưởng."
      />
      <RelatedLinks
        links={[
          {
            to: '/scheduling?tab=shifts',
            label: 'Khoang & ca làm việc',
            roles: ['ADMIN', 'RECEPTIONIST', 'TECHNICIAN'],
          },
          { to: '/payroll?tab=monthly', label: 'Bảng lương', roles: ['ADMIN', 'RECEPTIONIST'] },
        ]}
      />

      <div className="tabs" role="group" aria-label="Màn hình quản lý nhân viên">
        <button type="button" aria-pressed={tab === 'profiles'} onClick={() => setTab('profiles')}>
          <UserCog size={15} aria-hidden /> Hồ sơ nhân viên
        </button>
        <button type="button" aria-pressed={tab === 'salary'} onClick={() => setTab('salary')}>
          <Calculator size={15} aria-hidden /> Hệ số & đơn giá lương
        </button>
      </div>

      {tab === 'profiles' && <EmployeeDirectory canCreate={user?.role === 'ADMIN'} />}
      {tab === 'salary' && <SalaryCoefficientManager />}
    </div>
  );
}
