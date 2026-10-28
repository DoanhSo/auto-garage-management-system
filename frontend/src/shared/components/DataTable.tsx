import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from '@tanstack/react-table';
import type { Page, UiPolicy } from '../types/contracts';
import { Button, Empty } from './ui';
import { pageSizes } from '../config/policy';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { pageEntries } from '../utils/pagination';
export function DataTable<T extends { id: string | number }>({
  columns,
  data,
  busy = false,
  caption,
  hideOnMobile = [],
}: {
  columns: ColumnDef<T>[];
  data: T[];
  busy?: boolean;
  caption: string;
  hideOnMobile?: string[];
}) {
  const table = useReactTable({
    columns,
    data,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => String(row.id),
    manualPagination: true,
    manualSorting: true,
  });
  if (!data.length)
    return (
      <Empty
        title="Không có kết quả"
        description="Thử thay đổi bộ lọc hoặc quay lại khi có dữ liệu mới."
      />
    );
  return (
    <div className="table-scroll" aria-busy={busy}>
      <table className="data-table">
        <caption className="sr-only">{caption}</caption>
        <thead>
          {table.getHeaderGroups().map((group) => (
            <tr key={group.id}>
              {group.headers.map((header) => (
                <th
                  key={header.id}
                  scope="col"
                  className={hideOnMobile.includes(header.column.id) ? 'mobile-hidden' : ''}
                >
                  {header.isPlaceholder
                    ? null
                    : flexRender(header.column.columnDef.header, header.getContext())}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          {table.getRowModel().rows.map((row) => (
            <tr key={row.id}>
              {row.getVisibleCells().map((cell) => (
                <td
                  key={cell.id}
                  className={hideOnMobile.includes(cell.column.id) ? 'mobile-hidden' : ''}
                >
                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
export function Pagination({
  page,
  size,
  data,
  policy,
  onChange,
  disabled = false,
}: {
  page: number;
  size: number;
  data?: Pick<Page<unknown>, 'totalElements' | 'totalPages'>;
  policy: UiPolicy;
  onChange: (page: number, size: number) => void;
  disabled?: boolean;
}) {
  const total = BigInt(data?.totalElements ?? '0');
  const offset = BigInt(page) * BigInt(size);
  const from = total === 0n || offset >= total ? 0n : offset + 1n;
  const to = from === 0n ? 0n : offset + BigInt(size) > total ? total : offset + BigInt(size);
  const last = Math.max(0, Math.min((data?.totalPages ?? 1) - 1, policy.paginationMaxPage));
  const canNext = !!data && page < last;
  const format = (value: bigint) => new Intl.NumberFormat('vi-VN').format(value);
  const sizes = [...new Set([...pageSizes(policy), size])].sort((a, b) => a - b);
  return (
    <div className="pagination pagination-redesign">
      <div className="pagination-summary" aria-live="polite">
        {data ? (
          <>
            <span>
              Hiển thị{' '}
              <strong>
                {format(from)}–{format(to)}
              </strong>{' '}
              trong <strong>{format(total)}</strong> kết quả
            </span>
            <small>
              {total === 0n
                ? 'Không có dữ liệu để phân trang.'
                : page > last
                  ? 'Trang không có kết quả. Chọn lại số trang bên cạnh.'
                  : `Trang ${page + 1} / ${data.totalPages}`}
            </small>
          </>
        ) : (
          <span>Đang tải kết quả…</span>
        )}
        {data && data.totalPages - 1 > policy.paginationMaxPage && (
          <small>Thu hẹp bộ lọc để xem các kết quả vượt giới hạn phân trang.</small>
        )}
      </div>
      <div className="pagination-controls">
        <label>
          Dòng/trang
          <select
            className="input size-select"
            aria-label="Số dòng mỗi trang"
            value={size}
            disabled={disabled}
            onChange={(event) => onChange(0, Number(event.target.value))}
          >
            {sizes.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </label>
        <nav className="page-navigation" aria-label="Phân trang">
          <Button
            aria-label="Trang trước"
            disabled={page === 0 || disabled}
            onClick={() => onChange(page - 1, size)}
          >
            <ChevronLeft size={16} />
          </Button>
          {(data && total > 0n ? pageEntries(Math.min(page, last), last) : []).map(
            (entry, index) =>
              entry === 'gap' ? (
                <span className="page-gap" key={`gap-${index}`} aria-hidden>
                  …
                </span>
              ) : (
                <Button
                  key={entry}
                  className={entry === page ? 'page-current' : ''}
                  aria-label={`Trang ${entry + 1}`}
                  aria-current={entry === page ? 'page' : undefined}
                  disabled={disabled || !data || entry === page}
                  onClick={() => onChange(entry, size)}
                >
                  {entry + 1}
                </Button>
              ),
          )}
          <Button
            aria-label="Trang sau"
            disabled={!canNext || disabled}
            onClick={() => onChange(page + 1, size)}
          >
            <ChevronRight size={16} />
          </Button>
        </nav>
      </div>
    </div>
  );
}
