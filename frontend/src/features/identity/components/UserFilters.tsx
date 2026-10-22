import { useId } from 'react';
import { Search, SlidersHorizontal, RotateCcw, X } from 'lucide-react';
import { Button } from '../../../shared/components/ui';

const sortOptions = [
  ['id:asc', 'Mã tài khoản · Tăng dần'],
  ['id:desc', 'Mã tài khoản · Giảm dần'],
  ['createdAt:desc', 'Ngày tạo · Mới nhất'],
  ['createdAt:asc', 'Ngày tạo · Cũ nhất'],
  ['fullName:asc', 'Họ và tên · A → Z'],
  ['fullName:desc', 'Họ và tên · Z → A'],
  ['username:asc', 'Tên đăng nhập · A → Z'],
  ['username:desc', 'Tên đăng nhập · Z → A'],
  ['role:asc', 'Vai trò · Tăng dần'],
  ['role:desc', 'Vai trò · Giảm dần'],
  ['active:desc', 'Trạng thái · Hoạt động trước'],
  ['active:asc', 'Trạng thái · Đã khóa trước'],
];
export function UserFilters({
  keyword,
  active,
  sortBy,
  sortDir,
  onChange,
}: {
  keyword: string;
  active?: boolean;
  sortBy: string;
  sortDir: string;
  onChange: (values: Record<string, string | number | undefined>) => void;
}) {
  const id = useId();
  const count = (keyword.trim() ? 1 : 0) + (active === undefined ? 0 : 1);
  const changed =
    keyword.length > 0 || active !== undefined || sortBy !== 'id' || sortDir !== 'asc';
  return (
    <section className="user-filters" aria-label="Bộ lọc tài khoản">
      <div className="user-filters-heading">
        <div>
          <SlidersHorizontal size={16} aria-hidden />
          <h2>Bộ lọc tài khoản</h2>
          {count > 0 && <span className="filter-count">{count} đang áp dụng</span>}
        </div>
        <Button
          variant="ghost"
          className="filter-reset"
          disabled={!changed}
          onClick={() =>
            onChange({
              keyword: undefined,
              active: undefined,
              sortBy: undefined,
              sortDir: undefined,
              page: 0,
            })
          }
        >
          <RotateCcw size={14} />
          Đặt lại
        </Button>
      </div>
      <div className="user-filter-grid">
        <div className="user-filter-field">
          <label htmlFor={`${id}-search`}>Tìm kiếm</label>
          <div className="user-search">
            <Search size={17} aria-hidden />
            <input
              id={`${id}-search`}
              className="input"
              aria-label="Tìm tài khoản"
              value={keyword}
              maxLength={254}
              placeholder="Nhập họ tên, tên đăng nhập hoặc email"
              onChange={(event) => onChange({ keyword: event.target.value, page: 0 })}
            />
            {keyword && (
              <button
                type="button"
                className="search-clear"
                aria-label="Xóa từ khóa tìm kiếm"
                onClick={() => onChange({ keyword: undefined, page: 0 })}
              >
                <X size={15} />
              </button>
            )}
          </div>
        </div>
        <fieldset className="user-filter-field status-filter">
          <legend>Trạng thái</legend>
          <div className="status-segments">
            {(
              [
                { value: undefined, label: 'Tất cả' },
                { value: true, label: 'Hoạt động' },
                { value: false, label: 'Đã khóa' },
              ] as const
            ).map((option) => (
              <button
                key={option.label}
                type="button"
                aria-label={`Trạng thái tài khoản: ${option.label}`}
                aria-pressed={active === option.value}
                onClick={() =>
                  onChange({
                    active: option.value === undefined ? undefined : String(option.value),
                    page: 0,
                  })
                }
              >
                {option.label}
              </button>
            ))}
          </div>
        </fieldset>
        <div className="user-filter-field">
          <label htmlFor={`${id}-sort`}>Sắp xếp theo</label>
          <select
            id={`${id}-sort`}
            className="input"
            value={`${sortBy}:${sortDir}`}
            onChange={(event) => {
              const [field, direction] = event.target.value.split(':');
              onChange({ sortBy: field, sortDir: direction, page: 0 });
            }}
          >
            {sortOptions.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
      </div>
      {count > 0 && (
        <div className="active-filters" aria-label="Bộ lọc đang áp dụng">
          <span>Đang lọc:</span>
          {keyword.trim() && (
            <button
              type="button"
              className="filter-chip"
              aria-label="Bỏ lọc từ khóa"
              onClick={() => onChange({ keyword: undefined, page: 0 })}
            >
              <span>
                Từ khóa: <strong>{keyword.trim()}</strong>
              </span>
              <X size={13} />
            </button>
          )}
          {active !== undefined && (
            <button
              type="button"
              className="filter-chip"
              aria-label="Bỏ lọc trạng thái"
              onClick={() => onChange({ active: undefined, page: 0 })}
            >
              <span>
                Trạng thái: <strong>{active ? 'Hoạt động' : 'Đã khóa'}</strong>
              </span>
              <X size={13} />
            </button>
          )}
        </div>
      )}
    </section>
  );
}
