import {
  Car,
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
  vehicles,
  onEdit,
  onTransfer,
  onTimeline,
  onDeactivate,
}: {
  vehicles: Vehicle[];
  onEdit: (id: string) => void;
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
          <li key={v.id} className="vehicle-card border border-border rounded-lg p-4 bg-card shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="font-mono text-base font-bold bg-primary/10 text-primary px-2.5 py-1 rounded border border-primary/20">
                  {v.licensePlate}
                </span>
                <span className="text-xs font-semibold text-muted-foreground">
                  {v.brand} {v.model}
                </span>
              </div>
              <StatusBadge active={v.isActive} />
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="flex items-center gap-1.5 text-muted-foreground">
                <User size={13} className="shrink-0" />
                <span className="truncate">
                  {v.customerName} ({v.customerPhone})
                </span>
              </div>

              <div className="flex items-center gap-1.5 text-muted-foreground">
                <Gauge size={13} className="shrink-0" />
                <span>{v.odometer.toLocaleString('vi-VN')} km</span>
              </div>

              {v.fuelType && (
                <div className="flex items-center gap-1.5 text-muted-foreground">
                  <Fuel size={13} className="shrink-0" />
                  <span>{v.fuelType}</span>
                </div>
              )}

              {v.inspectionDueDate && (
                <div
                  className={`flex items-center gap-1.5 ${
                    isExpired
                      ? 'text-red-500 font-medium'
                      : isExpiringSoon
                        ? 'text-amber-500 font-medium'
                        : 'text-muted-foreground'
                  }`}
                >
                  <Calendar size={13} className="shrink-0" />
                  <span>Đăng kiểm: {formatDate(v.inspectionDueDate)}</span>
                </div>
              )}
            </div>

            {v.vin && (
              <div className="text-[11px] font-mono text-muted-foreground bg-muted/50 px-2 py-0.5 rounded">
                VIN: {v.vin}
              </div>
            )}

            <div className="flex flex-wrap items-center justify-end gap-1.5 pt-2 border-t border-border">
              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs"
                onClick={() => onTimeline(v.id)}
                title="Lịch sử sửa chữa"
              >
                <History size={14} className="mr-1" />
                Lịch sử
              </Button>

              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs"
                onClick={() => onTransfer(v)}
                title="Chuyển nhượng chủ xe"
              >
                <ArrowRightLeft size={14} className="mr-1 text-amber-500" />
                Đổi chủ
              </Button>

              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs"
                onClick={() => onEdit(v.id)}
                title="Sửa xe"
              >
                <SquarePen size={14} className="mr-1" />
                Sửa
              </Button>

              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs text-red-600 hover:text-red-700 hover:bg-red-50"
                onClick={() => onDeactivate(v)}
                title="Ngừng hoạt động"
              >
                <Trash2 size={14} />
              </Button>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
