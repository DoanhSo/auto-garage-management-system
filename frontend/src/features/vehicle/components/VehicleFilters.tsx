import { useId } from 'react';
import { Search, SlidersHorizontal, RotateCcw, X } from 'lucide-react';
import { Button } from '../../../shared/components/ui';

export function VehicleFilters({
  search,
  isActive,
  onSearchChange,
  onStatusChange,
  onReset,
}: {
  search: string;
  isActive: string;
  onSearchChange: (value: string) => void;
  onStatusChange: (value: string) => void;
  onReset: () => void;
}) {
  const id = useId();
  const filterCount = (search.trim() ? 1 : 0) + (isActive !== 'ALL' ? 1 : 0);

  return (
    <section className="user-filters" aria-label="Bộ lọc phương tiện">
      <div className="user-filters-heading">
        <div>
          <SlidersHorizontal size={16} aria-hidden />
          <h2>Bộ lọc phương tiện</h2>
          {filterCount > 0 && <span className="filter-count">{filterCount} đang áp dụng</span>}
        </div>
        <Button
          variant="ghost"
          className="filter-reset"
          disabled={filterCount === 0}
          onClick={onReset}
        >
          <RotateCcw size={14} />
          Đặt lại
        </Button>
      </div>

      <div className="user-filter-grid">
        <div className="user-filter-field">
          <label htmlFor={`${id}-search`}>Tìm kiếm phương tiện</label>
          <div className="user-search">
            <Search size={17} aria-hidden />
            <input
              id={`${id}-search`}
              type="search"
              className="input"
              placeholder="Tìm theo biển số, VIN, hãng xe, tên chủ xe..."
              value={search}
              onChange={(e) => onSearchChange(e.target.value)}
            />
            {search && (
              <button
                type="button"
                className="search-clear"
                aria-label="Xóa từ khóa tìm kiếm"
                onClick={() => onSearchChange('')}
              >
                <X size={15} />
              </button>
            )}
          </div>
        </div>

        <div className="user-filter-field">
          <label htmlFor={`${id}-status`}>Trạng thái hoạt động</label>
          <select
            id={`${id}-status`}
            className="select"
            value={isActive}
            onChange={(e) => onStatusChange(e.target.value)}
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang hoạt động</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
          </select>
        </div>
      </div>
    </section>
  );
}
