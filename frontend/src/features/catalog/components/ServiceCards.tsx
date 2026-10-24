import { Wrench, Clock, ShieldCheck, RotateCw, SquarePen, Trash2, Box } from 'lucide-react';
import type { Service } from '../../../shared/types/contracts';
import { Button, Empty, StatusBadge } from '../../../shared/components/ui';
import { money } from '../../../shared/utils/format';

export function ServiceCards({
  canManage = false,
  services,
  onEdit,
  onDeactivate,
}: {
  canManage?: boolean;
  services: Service[];
  onEdit: (id: string) => void;
  onDeactivate: (service: Service) => void;
}) {
  if (!services.length) {
    return (
      <Empty
        title="Không tìm thấy dịch vụ nào"
        description="Thử thay đổi từ khóa tìm kiếm hoặc bộ lọc phân loại."
      />
    );
  }

  return (
    <ul className="user-cards" aria-label="Danh sách dịch vụ dạng thẻ">
      {services.map((s) => {
        const isPackage = s.serviceType === 'PACKAGE';
        const hours = (s.standardMinutes / 60).toFixed(1);

        return (
          <li
            key={s.id}
            className="vehicle-card border border-border rounded-lg p-4 bg-card shadow-sm space-y-3"
          >
            <div className="flex items-start justify-between gap-2">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-semibold bg-muted text-muted-foreground px-2 py-0.5 rounded">
                    {s.serviceCode}
                  </span>
                  <span
                    className={`text-xs px-2 py-0.5 rounded font-medium ${
                      isPackage
                        ? 'bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300'
                        : 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300'
                    }`}
                  >
                    {isPackage ? 'Gói bảo dưỡng' : 'Dịch vụ lẻ'}
                  </span>
                </div>
                <h3 className="text-sm font-semibold text-foreground mt-1.5">{s.serviceName}</h3>
              </div>
              <StatusBadge active={s.isActive} />
            </div>

            <div className="flex items-baseline justify-between pt-1 border-t border-border/60">
              <span className="text-xs text-muted-foreground">Tiền công định mức:</span>
              <span className="text-base font-bold text-primary">{money(s.unitPrice)}</span>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
              <div className="flex items-center gap-1.5">
                <Clock size={13} className="shrink-0" />
                <span>
                  {s.standardMinutes} phút (~{hours}h)
                </span>
              </div>

              {s.requiredBayType ? (
                <div className="flex items-center gap-1.5 truncate">
                  <Box size={13} className="shrink-0" />
                  <span className="truncate">{s.requiredBayType}</span>
                </div>
              ) : (
                <div className="flex items-center gap-1.5">
                  <Wrench size={13} className="shrink-0" />
                  <span>Khoang tự do</span>
                </div>
              )}

              {(s.warrantyDays > 0 || s.warrantyKm > 0) && (
                <div className="flex items-center gap-1.5 col-span-2 text-emerald-600 dark:text-emerald-400">
                  <ShieldCheck size={13} className="shrink-0" />
                  <span>
                    Bảo hành: {s.warrantyDays > 0 ? `${s.warrantyDays} ngày` : ''}
                    {s.warrantyDays > 0 && s.warrantyKm > 0 ? ' / ' : ''}
                    {s.warrantyKm > 0 ? `${s.warrantyKm.toLocaleString('vi-VN')} km` : ''}
                  </span>
                </div>
              )}

              {isPackage && (s.intervalKm || s.intervalMonths) && (
                <div className="flex items-center gap-1.5 col-span-2 text-indigo-600 dark:text-indigo-400">
                  <RotateCw size={13} className="shrink-0" />
                  <span>
                    Chu kỳ: {s.intervalKm ? `${s.intervalKm.toLocaleString('vi-VN')} km` : ''}
                    {s.intervalKm && s.intervalMonths ? ' hoặc ' : ''}
                    {s.intervalMonths ? `${s.intervalMonths} tháng` : ''}
                  </span>
                </div>
              )}
            </div>

            <div className="flex items-center justify-end gap-1.5 pt-2 border-t border-border">
              {canManage && (
                <Button
                  variant="ghost"
                  className="button-small h-8 px-2.5 text-xs"
                  onClick={() => onEdit(s.id)}
                >
                  <SquarePen size={14} className="mr-1" />
                  Sửa
                </Button>
              )}

              {canManage && (
                <Button
                  variant="ghost"
                  className="button-small h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                  onClick={() => onDeactivate(s)}
                  title="Ngừng áp dụng"
                >
                  <Trash2 size={14} />
                </Button>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
