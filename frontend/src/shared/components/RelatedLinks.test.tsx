import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RelatedLinks } from './RelatedLinks';

let role = 'ADMIN';
vi.mock('../../app/providers/AuthProvider', () => ({
  useAuth: () => ({ user: { role } }),
}));

const links = [
  { to: '/staff', label: 'Quản lý nhân viên', roles: ['ADMIN', 'RECEPTIONIST'] as const },
  { to: '/payroll', label: 'Phiếu lương của tôi', roles: ['TECHNICIAN'] as const },
];

function show() {
  render(
    <MemoryRouter>
      <RelatedLinks links={links} />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  role = 'ADMIN';
});

describe('RelatedLinks', () => {
  it('lists only the links the role may open', () => {
    show();
    expect(screen.getByRole('link', { name: 'Quản lý nhân viên' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Phiếu lương của tôi' })).toBeNull();
  });

  it('shows technicians their own payslip link instead', () => {
    role = 'TECHNICIAN';
    show();
    expect(screen.getByRole('link', { name: 'Phiếu lương của tôi' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Quản lý nhân viên' })).toBeNull();
  });

  it('renders nothing when no link is allowed', () => {
    role = 'CUSTOMER';
    show();
    expect(screen.queryByRole('navigation', { name: 'Liên quan' })).toBeNull();
  });
});
