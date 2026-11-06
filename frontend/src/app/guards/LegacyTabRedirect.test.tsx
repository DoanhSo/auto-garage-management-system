import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { LegacyTabRedirect } from './LegacyTabRedirect';

function open(entry: string) {
  const router = createMemoryRouter(
    [
      {
        path: '/scheduling',
        element: (
          <LegacyTabRedirect moved={{ employees: '/staff?tab=profiles' }}>
            <p>Khoang và ca</p>
          </LegacyTabRedirect>
        ),
      },
      {
        path: '/payroll',
        element: (
          <LegacyTabRedirect
            moved={{ employees: '/staff?tab=profiles', coefficients: '/staff?tab=salary' }}
          >
            <p>Bảng lương</p>
          </LegacyTabRedirect>
        ),
      },
      { path: '/staff', element: <p>Quản lý nhân viên</p> },
    ],
    { initialEntries: [entry] },
  );
  render(<RouterProvider router={router} />);
  return router;
}

describe('LegacyTabRedirect', () => {
  it('sends the old employees tab of the workshop page to the staff screen', async () => {
    const router = open('/scheduling?tab=employees');
    expect(await screen.findByText('Quản lý nhân viên')).toBeVisible();
    expect(router.state.location.pathname + router.state.location.search).toBe(
      '/staff?tab=profiles',
    );
  });

  it('sends the old payroll tabs to the matching staff tab and keeps other parameters', async () => {
    const router = open('/payroll?tab=coefficients&employeeId=5');
    expect(await screen.findByText('Quản lý nhân viên')).toBeVisible();
    expect(router.state.location.search).toBe('?tab=salary&employeeId=5');
  });

  it('leaves the tabs that stayed untouched', async () => {
    open('/payroll?tab=bonus');
    expect(await screen.findByText('Bảng lương')).toBeVisible();
    open('/scheduling?tab=shifts');
    expect(await screen.findByText('Khoang và ca')).toBeVisible();
  });
});
