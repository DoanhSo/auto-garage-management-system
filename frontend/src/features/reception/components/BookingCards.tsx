import {
  Calendar,
  User,
  Car,
  CheckCircle,
  LogIn,
  CalendarClock,
  CalendarX,
  UserX,
} from 'lucide-react';
import type { Booking, BookingStatus } from '../../../shared/types/contracts';
import { Button, Empty } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';

export function BookingStatusBadge({ status }: { status: BookingStatus }) {
  switch (status) {
    case 'PENDING':
      return <span className="badge badge-warning">Chờ xác nhận</span>;
    case 'CONFIRMED':
      return <span className="badge badge-primary">Đã xác nhận</span>;
    case 'RECEIVED':
      return <span className="badge badge-success">Đã vào xưởng</span>;
    case 'CANCELLED':
      return <span className="badge badge-danger">Đã hủy</span>;
    case 'NO_SHOW':
      return <span className="badge badge-neutral">Vắng mặt</span>;
  }
}

export function BookingCards({
  canManage = false,
  bookings,
  onConfirm,
  onCheckIn,
  onReschedule,
  onCancel,
  onNoShow,
}: {
  canManage?: boolean;
  bookings: Booking[];
  onConfirm: (booking: Booking) => void;
  onCheckIn: (booking: Booking) => void;
  onReschedule: (booking: Booking) => void;
  onCancel: (booking: Booking) => void;
  onNoShow: (booking: Booking) => void;
  onVehicleCheck?: (booking: Booking) => void;
}) {
  if (!bookings.length) {
    return (
      <Empty
        title="Không tìm thấy lịch hẹn nào"
        description="Thử thay đổi bộ lọc trạng thái hoặc từ khóa tìm kiếm."
      />
    );
  }

  return (
    <ul className="user-cards" aria-label="Danh sách lịch hẹn dạng thẻ">
      {bookings.map((b) => (
        <li key={b.id}>
          <div className="user-card-top">
            <span className="code-tag">#{b.bookingCode}</span>
            <span className="plate-badge">{b.licensePlate}</span>
            <div style={{ marginLeft: 'auto' }}>
              <BookingStatusBadge status={b.status} />
            </div>
          </div>

          <div className="customer-card-details">
            <div className="info-row">
              <Calendar size={14} aria-hidden />
              <strong>Hẹn lúc: {formatDateTime(b.appointmentAt)}</strong>
            </div>

            <div className="info-row">
              <User size={14} aria-hidden />
              <span>
                {b.customerName} ({b.customerPhone})
              </span>
            </div>

            <div className="info-row">
              <Car size={14} aria-hidden />
              <span>{b.serviceName || 'Kiểm tra & bảo dưỡng tổng quát'}</span>
            </div>

            {b.requestDescription && (
              <p className="muted" style={{ marginTop: 4 }}>
                {b.requestDescription}
              </p>
            )}

            {b.cancellationReason && (
              <div className="info-row">
                <span className="badge badge-danger">Lý do hủy: {b.cancellationReason}</span>
              </div>
            )}
          </div>

          <div className="user-card-bottom">
            <div className="row-actions">
              {canManage && b.status === 'PENDING' && (
                <Button
                  variant="primary"
                  aria-label={`Xác nhận lịch hẹn ${b.bookingCode}`}
                  onClick={() => onConfirm(b)}
                >
                  <CheckCircle size={16} />
                  Xác nhận
                </Button>
              )}

              {canManage && b.status === 'CONFIRMED' && (
                <Button
                  variant="primary"
                  aria-label={`Tiếp nhận xe ${b.licensePlate} vào xưởng`}
                  onClick={() => onCheckIn(b)}
                >
                  <LogIn size={16} />
                  Tiếp nhận vào xưởng
                </Button>
              )}

              {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                <>
                  <Button
                    variant="ghost"
                    aria-label={`Dời lịch hẹn ${b.bookingCode}`}
                    onClick={() => onReschedule(b)}
                  >
                    <CalendarClock size={16} />
                    Dời lịch
                  </Button>

                  {canManage && b.status === 'CONFIRMED' && (
                    <Button
                      variant="ghost"
                      aria-label={`Đánh dấu vắng mặt lịch hẹn ${b.bookingCode}`}
                      onClick={() => onNoShow(b)}
                    >
                      <UserX size={16} />
                      Vắng mặt
                    </Button>
                  )}

                  <Button
                    variant="ghost"
                    className="delete-action"
                    aria-label={`Hủy lịch hẹn ${b.bookingCode}`}
                    onClick={() => onCancel(b)}
                  >
                    <CalendarX size={16} />
                    Hủy
                  </Button>
                </>
              )}
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
}
