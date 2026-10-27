import {
  Calendar,
  Gauge,
  History,
  ArrowRightLeft,
  SquarePen,
  Trash2,
  User,
  Fuel,
} from 'lucide-react';
import type { Vehicle } from '../../../shared/types/contracts';
import { Button, Empty, StatusBadge } from '../../../shared/components/ui';
import { formatDate } from '../../../shared/utils/format';

export function VehicleCards({
  canManage = false,
  vehicles,
  onEdit,
  onTransfer,
  onTimeline,
  onDeactivate,
}: {
  canManage?: boolean;
  vehicles: Vehicle[];
  onEdit: (id: string, trigger: HTMLElement) => void;
  onTransfer: (vehicle: Vehicle) => void;
  onTimeline: (id: string) => void;
  onDeactivate: (vehicle: Vehicle) => void;
}) {
  if (!vehicles.length) {
    return (
      <Empty
        title="Không tìm thấy phương tiện"
        description="Thử thay đổi từ khóa hoặc bộ lọc để tìm phương tiện phù hợp."
      />
    );
  }

  return (
    <ul className="user-cards" aria-label="Danh sách phương tiện dạng thẻ">
      {vehicles.map((v) => {
        const isExpiringSoon =
          v.inspectionDueDate &&
          new Date(v.inspectionDueDate).getTime() - Date.now() < 15 * 86400000 &&
          new Date(v.inspectionDueDate).getTime() > Date.now();
        const isExpired =
          v.inspectionDueDate && new Date(v.inspectionDueDate).getTime() < Date.now();

        return (
          <li key={v.id}>
            <div className="user-card-top">
              <span className="plate-badge">{v.licensePlate}</span>
              <div>
                <strong>
                  {v.brand} {v.model}
                </strong>
                <span className="muted">
                  {v.customerName} ({v.customerPhone})
                </span>
              </div>
              <StatusBadge active={v.isActive} />
            </div>

            <div className="customer-card-details">
              <div className="info-row">
                <Gauge size={14} aria-hidden />
                <span>Odo: {v.odometer.toLocaleString('vi-VN')} km</span>
              </div>

              {v.fuelType && (
                <div className="info-row">
                  <Fuel size={14} aria-hidden />
                  <span>Nhiên liệu: {v.fuelType}</span>
                </div>
              )}

              {v.inspectionDueDate && (
                <div className="info-row">
                  <Calendar size={14} aria-hidden />
                  <span
                    className={
                      isExpired ? 'badge badge-danger' : isExpiringSoon ? 'badge badge-warning' : ''
                    }
                  >
                    Đăng kiểm: {formatDate(v.inspectionDueDate)}
                    {isExpired ? ' (Đã hết hạn)' : isExpiringSoon ? ' (Sắp hết hạn)' : ''}
                  </span>
                </div>
              )}

              {v.vin && (
                <div className="info-row">
                  <span className="code-tag">VIN: {v.vin}</span>
                </div>
              )}
            </div>

            <div className="user-card-bottom">
              <div className="row-actions">
                <Button
                  variant="ghost"
                  aria-label={`Lịch sử kỹ thuật xe ${v.licensePlate}`}
                  onClick={() => onTimeline(v.id)}
                >
                  <History size={16} />
                  Lịch sử
                </Button>

                {canManage && (
                  <Button
                    variant="ghost"
                    aria-label={`Chuyển nhượng xe ${v.licensePlate}`}
                    onClick={() => onTransfer(v)}
                  >
                    <ArrowRightLeft size={16} />
                    Đổi chủ
                  </Button>
                )}

                {canManage && (
                  <Button
                    variant="ghost"
                    aria-label={`Sửa thông tin xe ${v.licensePlate}`}
                    onClick={(e) => onEdit(v.id, e.currentTarget)}
                  >
                    <SquarePen size={16} />
                    Sửa
                  </Button>
                )}

                {canManage && v.isActive && (
                  <Button
                    variant="ghost"
                    className="delete-action"
                    aria-label={`Ngừng hoạt động xe ${v.licensePlate}`}
                    onClick={() => onDeactivate(v)}
                  >
                    <Trash2 size={16} />
                    Ngừng HĐ
                  </Button>
                )}
              </div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
