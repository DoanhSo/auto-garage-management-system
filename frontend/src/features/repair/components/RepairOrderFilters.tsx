import { useId } from 'react';
import { RotateCcw, Search, SlidersHorizontal, X } from 'lucide-react';
import { Button } from '../../../shared/components/ui';
import type { RepairOrderStatus } from '../types/repairTypes';

export const REPAIR_STATUS_LABELS: Record<RepairOrderStatus, string> = {
  NEW: 'Mới tiếp nhận',
  DIAGNOSING: 'Đang chẩn đoán',
  WAITING_APPROVAL: 'Chờ duyệt báo giá',
  APPROVED: 'Đã duyệt báo giá',
  IN_PROGRESS: 'Đang sửa chữa',
  WAITING_PARTS: 'Chờ phụ tùng',
  WAITING_QC: 'Chờ kiểm tra chất lượng',
  QC_FAILED: 'Cần làm lại',
  COMPLETED: 'Đã sửa xong',
  INVOICED: 'Đã lập hóa đơn',
  CLOSED: 'Đã bàn giao xe',
  CANCELLED: 'Đã hủy',
};

const sortOptions = [
  ['createdAt:desc', 'Ngày tạo · Mới nhất'],
  ['createdAt:asc', 'Ngày tạo · Cũ nhất'],
  ['receivedAt:desc', 'Thời gian nhận xe · Mới nhất'],
  ['receivedAt:asc', 'Thời gian nhận xe · Cũ nhất'],
  ['orderCode:asc', 'Mã lệnh · A → Z'],
  ['orderCode:desc', 'Mã lệnh · Z → A'],
  ['status:asc', 'Trạng thái · Tăng dần'],
];

export function RepairOrderFilters({
  keyword,
  status,
  sortBy,
  sortDir,
  onChange,
}: {
  keyword: string;
  status?: RepairOrderStatus;
  sortBy: string;
  sortDir: string;
  onChange: (values: Record<string, string | number | undefined>) => void;
}) {
  const id = useId();
  const count = (keyword.trim() ? 1 : 0) + (status ? 1 : 0);
  const changed =
    keyword.length > 0 || !!status || sortBy !== 'createdAt' || sortDir !== 'desc';
  return (
    <section className="user-filters" aria-label="Bộ lọc lệnh sửa chữa">
      <div className="user-filters-heading">
        <div>
          <SlidersHorizontal size={16} aria-hidden />
          <h2>Bộ lọc lệnh sửa chữa</h2>
          {count > 0 && <span className="filter-count">{count} đang áp dụng</span>}
        </div>
        <Button
          variant="ghost"
          className="filter-reset"
          disabled={!changed}
          onClick={() =>
            onChange({
              keyword: undefined,
              status: undefined,
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
              aria-label="Tìm lệnh sửa chữa"
              value={keyword}
              maxLength={254}
              placeholder="Nhập mã lệnh hoặc mô tả triệu chứng"
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
        <div className="user-filter-field">
          <label htmlFor={`${id}-status`}>Trạng thái</label>
          <select
            id={`${id}-status`}
            className="input"
            value={status ?? ''}
            onChange={(event) => onChange({ status: event.target.value || undefined, page: 0 })}
          >
            <option value="">Tất cả trạng thái</option>
            {(Object.keys(REPAIR_STATUS_LABELS) as RepairOrderStatus[]).map((value) => (
              <option key={value} value={value}>
                {REPAIR_STATUS_LABELS[value]}
              </option>
            ))}
          </select>
        </div>
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
          {status && (
            <button
              type="button"
              className="filter-chip"
              aria-label="Bỏ lọc trạng thái"
              onClick={() => onChange({ status: undefined, page: 0 })}
            >
              <span>
                Trạng thái: <strong>{REPAIR_STATUS_LABELS[status]}</strong>
              </span>
              <X size={13} />
            </button>
          )}
        </div>
      )}
    </section>
  );
}
