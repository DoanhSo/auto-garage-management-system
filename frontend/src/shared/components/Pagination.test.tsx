import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Pagination } from './DataTable';
import { policy } from '../../test/fixtures';
describe('Pagination controls', () => {
  it.each([
    { page: 0, totalPages: 1, visible: [1] },
    { page: 1, totalPages: 4, visible: [1, 2, 3, 4] },
    { page: 0, totalPages: 7, visible: [1, 2, 7] },
    { page: 1, totalPages: 7, visible: [1, 2, 3, 7] },
    { page: 2, totalPages: 7, visible: [1, 2, 3, 4, 7] },
    { page: 4, totalPages: 7, visible: [1, 4, 5, 6, 7] },
    { page: 5, totalPages: 7, visible: [1, 5, 6, 7] },
    { page: 6, totalPages: 7, visible: [1, 6, 7] },
    { page: 4, totalPages: 5, visible: [1, 2, 3, 4, 5] },
    { page: 19, totalPages: 20, visible: [1, 19, 20] },
    { page: 4, totalPages: 8, visible: [1, 4, 5, 6, 8] },
    { page: 4, totalPages: 20, visible: [1, 4, 5, 6, 20] },
  ])(
    'limits page buttons near index $page of $totalPages pages',
    ({ page, totalPages, visible }) => {
      const change = vi.fn();
      render(
        <Pagination
          page={page}
          size={20}
          data={{ totalElements: String(totalPages * 20), totalPages }}
          policy={policy}
          onChange={change}
        />,
      );
      const buttons = screen.getAllByRole('button', { name: /^Trang \d+$/ });
      expect(buttons.map((button) => Number(button.textContent))).toEqual(visible);
      expect(buttons.length).toBeLessThanOrEqual(5);
      expect(screen.getByRole('button', { name: 'Trang 1' })).toBeVisible();
      expect(screen.getByRole('button', { name: `Trang ${totalPages}` })).toBeVisible();
      expect(screen.getByRole('button', { name: `Trang ${page + 1}` })).toHaveAttribute(
        'aria-current',
        'page',
      );
      if (page < totalPages - 1) {
        fireEvent.click(screen.getByRole('button', { name: `Trang ${totalPages}` }));
        expect(change).toHaveBeenCalledWith(totalPages - 1, 20);
      }
    },
  );

  it('navigates from the first page to the last and back without changing page size', () => {
    const change = vi.fn();
    const props = {
      size: 20,
      data: { totalElements: '137', totalPages: 7 },
      policy,
      onChange: change,
    };
    const { rerender } = render(<Pagination {...props} page={0} />);
    fireEvent.click(screen.getByRole('button', { name: 'Trang 7' }));
    expect(change).toHaveBeenLastCalledWith(6, 20);

    rerender(<Pagination {...props} page={6} />);
    expect(screen.getByRole('button', { name: 'Trang sau' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Trang 1' }));
    expect(change).toHaveBeenLastCalledWith(0, 20);

    change.mockClear();
    rerender(<Pagination {...props} page={6} disabled />);
    expect(screen.getByRole('button', { name: 'Trang 1' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Trang 1' }));
    expect(change).not.toHaveBeenCalled();
  });

  it('shows the viewed range, current page and direct page navigation', () => {
    const change = vi.fn();
    render(
      <Pagination
        page={3}
        size={20}
        data={{ totalElements: '117', totalPages: 6 }}
        policy={policy}
        onChange={change}
      />,
    );
    expect(screen.getByText('61–80')).toBeVisible();
    expect(screen.getByText('117')).toBeVisible();
    expect(screen.getByText('Trang 4 / 6')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Trang 4' })).toHaveAttribute('aria-current', 'page');
    fireEvent.click(screen.getByRole('button', { name: 'Trang 5' }));
    expect(change).toHaveBeenCalledWith(4, 20);
    fireEvent.change(screen.getByLabelText('Số dòng mỗi trang'), { target: { value: '10' } });
    expect(change).toHaveBeenLastCalledWith(0, 10);
  });
  it('keeps totals exact and respects the server maximum page', () => {
    render(
      <Pagination
        page={2}
        size={20}
        data={{ totalElements: '9007199254740993', totalPages: 2147483647 }}
        policy={{ ...policy, paginationMaxPage: 2 }}
        onChange={() => {}}
      />,
    );
    expect(screen.getByText('9.007.199.254.740.993')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Trang sau' })).toBeDisabled();
  });
  it('does not invent a result range or numbered page for an empty list', () => {
    render(
      <Pagination
        page={0}
        size={20}
        data={{ totalElements: '0', totalPages: 0 }}
        policy={policy}
        onChange={() => {}}
      />,
    );
    expect(screen.getByText('0–0')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Trang trước' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Trang sau' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Trang 1' })).not.toBeInTheDocument();
  });
});
