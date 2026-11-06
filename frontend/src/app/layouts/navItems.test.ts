import { describe, expect, it } from 'vitest';
import { isChildActive, isNavActive } from './navMatch';
import { links, navForRole } from './navItems';

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

  it('lights exactly one hub and one tab for a page address', () => {
    const allowed = navForRole('ADMIN');
    const lit = (pathname: string, search = '') =>
      allowed.filter((link) => isNavActive(link, pathname, search, allowed)).map((link) => link.to);

    expect(lit('/scheduling', '?tab=shifts')).toEqual(['/scheduling']);
    expect(lit('/payroll')).toEqual(['/payroll']);
    expect(lit('/staff')).toEqual(['/staff']);

    const scheduling = links.find((link) => link.to === '/scheduling')!;
    const tabs = (search: string) =>
      scheduling
        .children!.filter((child) => isChildActive(scheduling, child, '/scheduling', search))
        .map((child) => child.tab);
    expect(tabs('?tab=bays')).toEqual(['bays']);
    expect(tabs('?tab=shifts')).toEqual(['shifts']);
    expect(tabs('')).toEqual(['dispatch']);
    expect(tabs('?tab=unknown')).toEqual(['dispatch']);
  });
});
