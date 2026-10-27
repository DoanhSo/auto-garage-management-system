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
import type { Booking } from '../../../shared/types/contracts';
import { Button, Empty } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';

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
  onVehicleCheck: (booking: Booking) => void;
}) {
  if (!bookings.length) {
    return (
      <Empty
        title="Không tìm thấy lịch hẹn nào"
        description="Thử thay đổi bộ lọc trạng thái hoặc từ khóa tìm kiếm."
      />
    );
  }

  const getStatusBadge = (status: Booking['status']) => {
    switch (status) {
      case 'PENDING':
        return (
          <span className="text-xs px-2 py-0.5 rounded-full font-medium bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300">
            Chờ xác nhận
          </span>
        );
      case 'CONFIRMED':
        return (
          <span className="text-xs px-2 py-0.5 rounded-full font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300">
            Đã xác nhận
          </span>
        );
      case 'RECEIVED':
        return (
          <span className="text-xs px-2 py-0.5 rounded-full font-medium bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300">
            Đã vào xưởng
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="text-xs px-2 py-0.5 rounded-full font-medium bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300">
            Đã hủy
          </span>
        );
      case 'NO_SHOW':
        return (
          <span className="text-xs px-2 py-0.5 rounded-full font-medium bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300">
            Vắng mặt
          </span>
        );
    }
  };

  return (
    <ul className="user-cards" aria-label="Danh sách lịch hẹn dạng thẻ">
      {bookings.map((b) => (
        <li
          key={b.id}
          className="vehicle-card border border-border rounded-lg p-4 bg-card shadow-sm space-y-3"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs font-semibold bg-muted text-muted-foreground px-2 py-0.5 rounded">
                #{b.bookingCode}
              </span>
              <span className="font-mono font-bold text-xs bg-primary/10 text-primary px-2 py-0.5 rounded border border-primary/20">
                {b.licensePlate}
              </span>
            </div>
            {getStatusBadge(b.status)}
          </div>

          <div className="flex items-center gap-1.5 text-xs font-medium text-foreground">
            <Calendar size={13} className="text-primary" />
            <span>Hẹn lúc: {formatDateTime(b.appointmentAt)}</span>
          </div>

          <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
            <div className="flex items-center gap-1.5 truncate">
              <User size={13} className="shrink-0" />
              <span className="truncate">
                {b.customerName} ({b.customerPhone})
              </span>
            </div>

            <div className="flex items-center gap-1.5 truncate">
              <Car size={13} className="shrink-0" />
              <span className="truncate">{b.serviceName || 'Khám tổng quát'}</span>
            </div>
          </div>

          {b.requestDescription && (
            <div className="text-xs text-muted-foreground bg-muted/40 p-2 rounded line-clamp-2">
              {b.requestDescription}
            </div>
          )}

          {b.cancellationReason && (
            <div className="text-xs text-red-600 bg-red-50 dark:bg-red-950/30 p-2 rounded border border-red-200">
              Lý do hủy: {b.cancellationReason}
            </div>
          )}

          <div className="flex flex-wrap items-center justify-end gap-1.5 pt-2 border-t border-border">
            {canManage && b.status === 'PENDING' && (
              <Button
                variant="primary"
                className="button-small h-8 px-2.5 text-xs"
                onClick={() => onConfirm(b)}
              >
                <CheckCircle size={13} className="mr-1" />
                Xác nhận
              </Button>
            )}

            {canManage && b.status === 'CONFIRMED' && (
              <Button
                variant="primary"
                className="button-small h-8 px-2.5 text-xs bg-emerald-600 hover:bg-emerald-700"
                onClick={() => onCheckIn(b)}
              >
                <LogIn size={13} className="mr-1" />
                Tiếp nhận vào xưởng
              </Button>
            )}

            {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
              <>
                <Button
                  variant="ghost"
                  className="button-small h-8 px-2 text-xs"
                  onClick={() => onReschedule(b)}
                >
                  <CalendarClock size={13} className="mr-1 text-amber-500" />
                  Dời lịch
                </Button>

                {canManage && b.status === 'CONFIRMED' && (
                  <Button
                    variant="ghost"
                    className="button-small h-8 px-2 text-xs text-slate-600"
                    onClick={() => onNoShow(b)}
                    title="Đánh dấu khách không đến"
                  >
                    <UserX size={13} className="mr-1" />
                    Vắng mặt
                  </Button>
                )}

                <Button
                  variant="ghost"
                  className="button-small h-8 px-2 text-xs text-red-600 hover:text-red-700"
                  onClick={() => onCancel(b)}
                >
                  <CalendarX size={13} className="mr-1" />
                  Hủy
                </Button>
              </>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
