import { useId } from 'react';
import { Search, SlidersHorizontal, RotateCcw, X } from 'lucide-react';
import { Button } from '../../../shared/components/ui';

export function ServiceFilters({
  search,
  serviceType,
  isActive,
  onSearchChange,
  onTypeChange,
  onStatusChange,
  onReset,
}: {
  search: string;
  serviceType: string;
  isActive: string;
  onSearchChange: (value: string) => void;
  onTypeChange: (value: string) => void;
  onStatusChange: (value: string) => void;
  onReset: () => void;
}) {
  const id = useId();
  const filterCount =
    (search.trim() ? 1 : 0) +
    (serviceType !== 'ALL' ? 1 : 0) +
    (isActive !== 'ALL' ? 1 : 0);

  return (
    <section className="user-filters" aria-label="Bộ lọc danh mục dịch vụ">
      <div className="user-filters-heading">
        <div>
          <SlidersHorizontal size={16} aria-hidden />
          <h2>Bộ lọc dịch vụ & tiền công</h2>
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
          <label htmlFor={`${id}-search`}>Tìm kiếm dịch vụ</label>
          <div className="user-search">
            <Search size={17} aria-hidden />
            <input
              id={`${id}-search`}
              type="search"
              className="input"
              placeholder="Tìm theo tên dịch vụ, mã DV..."
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
          <label htmlFor={`${id}-type`}>Phân loại dịch vụ</label>
          <select
            id={`${id}-type`}
            className="select"
            value={serviceType}
            onChange={(e) => onTypeChange(e.target.value)}
          >
            <option value="ALL">Tất cả phân loại</option>
            <option value="SINGLE">Dịch vụ đơn lẻ (Single)</option>
            <option value="PACKAGE">Gói bảo dưỡng định kỳ (Package)</option>
          </select>
        </div>

        <div className="user-filter-field">
          <label htmlFor={`${id}-status`}>Trạng thái áp dụng</label>
          <select
            id={`${id}-status`}
            className="select"
            value={isActive}
            onChange={(e) => onStatusChange(e.target.value)}
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang áp dụng</option>
            <option value="INACTIVE">Tạm dừng áp dụng</option>
          </select>
        </div>
      </div>
    </section>
  );
}
