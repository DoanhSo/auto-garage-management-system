import { describe, expect, it } from 'vitest';
import { isNavActive, navHref, type NavTarget } from './navMatch';

const payroll: NavTarget[] = [
  { to: '/payroll', tab: 'employees' },
  { to: '/payroll', tab: 'monthly', defaultTab: true },
  { to: '/payroll', tab: 'coefficients' },
  { to: '/payroll', tab: 'bonus' },
];
const scheduling: NavTarget[] = [
  { to: '/scheduling', tab: 'dispatch', alsoTabs: ['bays'], defaultTab: true },
  { to: '/scheduling', tab: 'shifts' },
  { to: '/scheduling', tab: 'employees' },
];
const all: NavTarget[] = [...payroll, ...scheduling, { to: '/estimates' }];

function active(pathname: string, search = '') {
  return all.filter((link) => isNavActive(link, pathname, search, all)).map(navHref);
}

describe('isNavActive', () => {
  it('highlights exactly the entry of the open tab', () => {
    expect(active('/payroll', '?tab=employees')).toEqual(['/payroll?tab=employees']);
    expect(active('/payroll', '?tab=bonus')).toEqual(['/payroll?tab=bonus']);
    expect(active('/scheduling', '?tab=shifts')).toEqual(['/scheduling?tab=shifts']);
  });

  it('falls back to the default entry without a tab or with an unknown tab', () => {
    expect(active('/payroll')).toEqual(['/payroll?tab=monthly']);
    expect(active('/payroll', '?tab=nope')).toEqual(['/payroll?tab=monthly']);
    expect(active('/scheduling')).toEqual(['/scheduling?tab=dispatch']);
  });

  it('keeps an entry lit for its alias tabs', () => {
    expect(active('/scheduling', '?tab=bays')).toEqual(['/scheduling?tab=dispatch']);
  });

  it('matches plain entries by path and leaves other pages unlit', () => {
    expect(active('/estimates')).toEqual(['/estimates']);
    expect(active('/estimates/3')).toEqual(['/estimates']);
    expect(active('/vehicles')).toEqual([]);
  });

  it('builds hrefs with the tab only for tab entries', () => {
    expect(navHref({ to: '/payroll', tab: 'employees' })).toBe('/payroll?tab=employees');
    expect(navHref({ to: '/estimates' })).toBe('/estimates');
  });
});
