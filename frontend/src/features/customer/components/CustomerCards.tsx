import { Building2, Phone, Mail, MapPin, SquarePen, UserX, CreditCard } from 'lucide-react';
import type { Customer } from '../../../shared/types/contracts';
import { Button, Empty, StatusBadge } from '../../../shared/components/ui';
import { initials } from '../../../shared/utils/format';

export function CustomerCards({
  customers,
  onEdit,
  onDeactivate,
}: {
  customers: Customer[];
  onEdit: (id: string, trigger: HTMLElement) => void;
  onDeactivate: (customer: Customer) => void;
}) {
  if (!customers.length) {
    return (
      <Empty
        title="Không tìm thấy khách hàng"
        description="Thử thay đổi từ khóa hoặc bộ lọc để tìm hồ sơ khách hàng."
      />
    );
  }

  const formatVnd = (value: string | number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(
      Number(value),
    );
  };

  return (
    <ul className="user-cards" aria-label="Danh sách khách hàng dạng thẻ">
      {customers.map((cust) => (
        <li key={cust.id}>
          <div className="user-card-top">
            <span className="avatar">{initials(cust.fullName)}</span>
            <div>
              <strong>{cust.fullName}</strong>
              <span className="code-tag">{cust.customerCode}</span>
            </div>
            <StatusBadge active={cust.isActive} />
          </div>

          <div className="customer-card-details">
            <div className="info-row">
              <span className={`badge ${cust.customerType === 'BUSINESS' ? 'badge-primary' : 'badge-neutral'}`}>
                {cust.customerType === 'BUSINESS' ? 'Doanh nghiệp' : 'Cá nhân'}
              </span>
              {cust.companyName && (
                <span className="company-text">
                  <Building2 size={14} aria-hidden /> {cust.companyName}
                </span>
              )}
            </div>

            <div className="info-row">
              <Phone size={14} aria-hidden />
              <span>{cust.phone}</span>
            </div>

            {cust.email && (
              <div className="info-row">
                <Mail size={14} aria-hidden />
                <span>{cust.email}</span>
              </div>
            )}

            {cust.address && (
              <div className="info-row">
                <MapPin size={14} aria-hidden />
                <span>{cust.address}</span>
              </div>
            )}

            <div className="info-row credit-row">
              <CreditCard size={14} aria-hidden />
              <span>
                Hạn mức: <strong>{formatVnd(cust.creditLimit)}</strong> ({cust.paymentTermDays} ngày)
              </span>
            </div>
          </div>

          <div className="user-card-bottom">
            <div className="row-actions">
              <Button
                variant="ghost"
                aria-label={`Sửa khách hàng ${cust.fullName}`}
                onClick={(e) => onEdit(cust.id, e.currentTarget)}
              >
                <SquarePen size={17} />
                Sửa
              </Button>
              {cust.isActive && (
                <Button
                  variant="ghost"
                  className="delete-action"
                  aria-label={`Ngừng hoạt động khách hàng ${cust.fullName}`}
                  onClick={() => onDeactivate(cust)}
                >
                  <UserX size={17} />
                  Ngừng hoạt động
                </Button>
              )}
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
}
