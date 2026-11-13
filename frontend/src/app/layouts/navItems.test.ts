import { describe, expect, it } from 'vitest';
import { isNavActive } from './navMatch';
import { navForRole } from './navItems';

const hrLabels = (role: Parameters<typeof navForRole>[0]) =>
  navForRole(role)
    .filter((link) => link.section === 'NHÂN SỰ & LƯƠNG')
    .map((link) => link.label);

describe('HR sidebar hubs', () => {
  it('gives managers three hub entries', () => {
    expect(hrLabels('ADMIN')).toEqual(['Quản lý nhân viên', 'Khoang & ca làm việc', 'Bảng lương']);
    expect(hrLabels('RECEPTIONIST')).toEqual([
      'Quản lý nhân viên',
      'Khoang & ca làm việc',
      'Bảng lương',
    ]);
  });

  it('hides employee management from technicians but keeps shifts and their own payslip', () => {
    expect(hrLabels('TECHNICIAN')).toEqual(['Khoang & ca làm việc', 'Phiếu lương của tôi']);
  });

  it('shows no HR entry to customers and warehouse staff', () => {
    expect(hrLabels('CUSTOMER')).toEqual([]);
    expect(hrLabels('WAREHOUSE_STAFF')).toEqual([]);
  });

  it('lights exactly one hub for a page address', () => {
    const allowed = navForRole('ADMIN');
    const lit = (pathname: string, search = '') =>
      allowed.filter((link) => isNavActive(link, pathname, search, allowed)).map((link) => link.to);

    expect(lit('/scheduling', '?tab=shifts')).toEqual(['/scheduling']);
    expect(lit('/payroll')).toEqual(['/payroll']);
    expect(lit('/staff')).toEqual(['/staff']);
  });
});
