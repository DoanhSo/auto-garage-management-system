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
  onEdit: (id: string, trigger: HTMLElement) => void;
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
          <li key={s.id}>
            <div className="user-card-top">
              <span className="code-tag">{s.serviceCode}</span>
              <div>
                <strong>{s.serviceName}</strong>
                <span className={`badge ${isPackage ? 'badge-primary' : 'badge-neutral'}`}>
                  {isPackage ? 'Gói bảo dưỡng' : 'Dịch vụ lẻ'}
                </span>
              </div>
              <StatusBadge active={s.isActive} />
            </div>

            <div className="customer-card-details">
              <div className="info-row credit-row">
                <span className="muted">Tiền công định mức:</span>
                <strong className="credit-amount">{money(s.unitPrice)}</strong>
              </div>

              <div className="info-row">
                <Clock size={14} aria-hidden />
                <span>
                  Thời gian chuẩn: {s.standardMinutes} phút (~{hours} giờ)
                </span>
              </div>

              {s.requiredBayType ? (
                <div className="info-row">
                  <Box size={14} aria-hidden />
                  <span>Yêu cầu khoang: {s.requiredBayType}</span>
                </div>
              ) : (
                <div className="info-row">
                  <Wrench size={14} aria-hidden />
                  <span className="muted">Khoang tự do</span>
                </div>
              )}

              {isPackage && (s.intervalKm || s.intervalMonths) && (
                <div className="info-row">
                  <RotateCw size={14} aria-hidden />
                  <span>
                    Chu kỳ:{' '}
                    {s.intervalKm ? `${s.intervalKm.toLocaleString('vi-VN')} km` : ''}
                    {s.intervalKm && s.intervalMonths ? ' hoặc ' : ''}
                    {s.intervalMonths ? `${s.intervalMonths} tháng` : ''}
                  </span>
                </div>
              )}

              {(s.warrantyDays > 0 || s.warrantyKm > 0) && (
                <div className="info-row">
                  <ShieldCheck size={14} aria-hidden />
                  <span>
                    Bảo hành: {s.warrantyDays > 0 ? `${s.warrantyDays} ngày` : ''}
                    {s.warrantyDays > 0 && s.warrantyKm > 0 ? ' / ' : ''}
                    {s.warrantyKm > 0 ? `${s.warrantyKm.toLocaleString('vi-VN')} km` : ''}
                  </span>
                </div>
              )}

              {s.description && (
                <p className="muted" style={{ marginTop: 4 }}>
                  {s.description}
                </p>
              )}
            </div>

            <div className="user-card-bottom">
              <div className="row-actions">
                {canManage && (
                  <Button
                    variant="ghost"
                    aria-label={`Sửa dịch vụ ${s.serviceName}`}
                    onClick={(e) => onEdit(s.id, e.currentTarget)}
                  >
                    <SquarePen size={16} />
                    Sửa
                  </Button>
                )}

                {canManage && s.isActive && (
                  <Button
                    variant="ghost"
                    className="delete-action"
                    aria-label={`Ngừng áp dụng dịch vụ ${s.serviceName}`}
                    onClick={() => onDeactivate(s)}
                  >
                    <Trash2 size={16} />
                    Ngừng áp dụng
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
