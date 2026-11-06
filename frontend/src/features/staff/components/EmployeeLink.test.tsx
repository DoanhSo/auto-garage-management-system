import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { EmployeeLink } from './EmployeeLink';

let role = 'ADMIN';
vi.mock('../../../app/providers/AuthProvider', () => ({
  useAuth: () => ({ user: { role } }),
}));

function show() {
  render(
    <MemoryRouter>
      <EmployeeLink id={5}>Nguyễn Văn An</EmployeeLink>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  role = 'ADMIN';
});

describe('EmployeeLink', () => {
  it.each(['ADMIN', 'RECEPTIONIST'])('links %s to the employee profile panel', (who) => {
    role = who;
    show();
    expect(screen.getByRole('link', { name: 'Nguyễn Văn An' })).toHaveAttribute(
      'href',
      '/staff?tab=profiles&employeeId=5',
    );
  });

  it.each(['TECHNICIAN', 'CUSTOMER', 'WAREHOUSE_STAFF'])('shows plain text to %s', (who) => {
    role = who;
    show();
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.getByText('Nguyễn Văn An')).toBeVisible();
  });
});
