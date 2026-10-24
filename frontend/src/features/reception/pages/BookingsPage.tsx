import { useMemo, useRef, useState } from 'react';
import {
  Plus,
  LogIn,
  CheckCircle,
  CalendarClock,
  CalendarX,
  UserX,
  Calendar,
  Car,
  User,
  Clock,
  ClipboardCheck,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import {
  Button,
  ConfirmDialog,
  Loading,
  PageHeader,
  QueryError,
} from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { errorMessage } from '../../../shared/api/errors';
import { formatDateTime } from '../../../shared/utils/format';
import type { Booking, BookingStatus } from '../../../shared/types/contracts';
import {
  useBookingsQuery,
  useConfirmBookingMutation,
  useMarkNoShowMutation,
} from '../hooks/useReception';
import { BookingFilters } from '../components/BookingFilters';
import { BookingEditor } from '../components/BookingEditor';
import { CheckInModal } from '../components/CheckInModal';
import { RescheduleModal } from '../components/RescheduleModal';
import { CancelBookingDialog } from '../components/CancelBookingDialog';
import { VehicleCheckModal } from '../components/VehicleCheckModal';
import { BookingCards } from '../components/BookingCards';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';

export function BookingsPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');

  const [editorOpen, setEditorOpen] = useState(false);

  const [checkInBooking, setCheckInBooking] = useState<Booking | null>(null);
  const [checkInOpen, setCheckInOpen] = useState(false);

  const [rescheduleBooking, setRescheduleBooking] = useState<Booking | null>(null);
  const [rescheduleOpen, setRescheduleOpen] = useState(false);

  const [cancelBooking, setCancelBooking] = useState<Booking | null>(null);
  const [cancelOpen, setCancelOpen] = useState(false);

  const [noShowBooking, setNoShowBooking] = useState<Booking | null>(null);

  // Sau khi check-in thành công có thể mở ngay biên bản kiểm tra xe
  const [createdRepairOrder, setCreatedRepairOrder] = useState<{ id: string; code: string } | null>(null);
  const [vehicleCheckOpen, setVehicleCheckOpen] = useState(false);

  const createTrigger = useRef<HTMLButtonElement>(null);
  const debouncedSearch = useDebouncedValue(search);

  const queryParams = useMemo(() => {
    return {
      status: status !== 'ALL' ? (status as BookingStatus) : undefined,
    };
  }, [status]);

  const { data: allBookings = [], isLoading, isError, error, refetch } = useBookingsQuery(queryParams);
  const confirmMutation = useConfirmBookingMutation();
  const noShowMutation = useMarkNoShowMutation();

  // Lọc client-side theo từ khóa search (mã, khách hàng, sđt, biển số)
  const bookings = useMemo(() => {
    if (!debouncedSearch.trim()) return allBookings;
    const q = debouncedSearch.toLowerCase().trim();
    return allBookings.filter(
      (b) =>
        b.bookingCode.toLowerCase().includes(q) ||
        b.customerName.toLowerCase().includes(q) ||
        b.customerPhone.includes(q) ||
        b.licensePlate.toLowerCase().includes(q),
    );
  }, [allBookings, debouncedSearch]);

  const handleConfirm = async (booking: Booking) => {
    try {
      await confirmMutation.mutateAsync({ id: booking.id });
      notify.success(`Đã xác nhận lịch hẹn #${booking.bookingCode}`);
    } catch (err: unknown) {
      notify.error(errorMessage(err) ?? 'Xác nhận lịch hẹn thất bại');
    }
  };

  const handleMarkNoShow = async () => {
    if (!noShowBooking) return;
    try {
      await noShowMutation.mutateAsync({ id: noShowBooking.id });
      notify.success(`Đã đánh dấu vắng mặt cho lịch hẹn #${noShowBooking.bookingCode}`);
      setNoShowBooking(null);
    } catch (err: unknown) {
      notify.error(errorMessage(err) ?? 'Cập nhật thất bại');
    }
  };

  const getStatusBadge = (bStatus: BookingStatus) => {
    switch (bStatus) {
      case 'PENDING':
        return <span className="text-xs px-2.5 py-1 rounded-full font-semibold bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300">Chờ xác nhận</span>;
      case 'CONFIRMED':
        return <span className="text-xs px-2.5 py-1 rounded-full font-semibold bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300">Đã xác nhận</span>;
      case 'RECEIVED':
        return <span className="text-xs px-2.5 py-1 rounded-full font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300">Đã vào xưởng</span>;
      case 'CANCELLED':
        return <span className="text-xs px-2.5 py-1 rounded-full font-semibold bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300">Đã hủy</span>;
      case 'NO_SHOW':
        return <span className="text-xs px-2.5 py-1 rounded-full font-semibold bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300">Vắng mặt</span>;
    }
  };

  const columns = useMemo<ColumnDef<Booking>[]>(
    () => [
      {
        id: 'bookingCode',
        header: 'Mã hẹn',
        cell: ({ row }) => (
          <span className="font-mono text-xs font-semibold bg-muted text-muted-foreground px-2 py-0.5 rounded border border-border">
            #{row.original.bookingCode}
          </span>
        ),
      },
      {
        id: 'appointmentAt',
        header: 'Thời gian hẹn',
        cell: ({ row }) => (
          <div className="flex items-center gap-1.5 text-xs font-medium text-foreground">
            <Calendar size={13} className="text-primary" />
            <span>{formatDateTime(row.original.appointmentAt)}</span>
          </div>
        ),
      },
      {
        id: 'customer',
        header: 'Khách hàng',
        cell: ({ row }) => (
          <div>
            <div className="font-semibold text-foreground text-sm">{row.original.customerName}</div>
            <div className="text-xs text-muted-foreground">{row.original.customerPhone}</div>
          </div>
        ),
      },
      {
        id: 'vehicle',
        header: 'Phương tiện',
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-xs bg-primary/10 text-primary px-2 py-0.5 rounded border border-primary/20 inline-block">
              {row.original.licensePlate}
            </span>
            {row.original.vehicleModel && (
              <div className="text-xs text-muted-foreground mt-0.5">{row.original.vehicleModel}</div>
            )}
          </div>
        ),
      },
      {
        id: 'service',
        header: 'Dịch vụ yêu cầu',
        cell: ({ row }) => (
          <div className="text-xs">
            <span className="font-medium text-foreground">
              {row.original.serviceName || 'Khám tổng quát'}
            </span>
            {row.original.requestDescription && (
              <div className="text-muted-foreground line-clamp-1 mt-0.5">
                {row.original.requestDescription}
              </div>
            )}
          </div>
        ),
      },
      {
        id: 'status',
        header: 'Trạng thái',
        cell: ({ row }) => getStatusBadge(row.original.status),
      },
      {
        id: 'actions',
        header: () => <span className="sr-only">Thao tác</span>,
        cell: ({ row }) => {
          const b = row.original;
          return (
            <div className="row-actions flex items-center justify-end gap-1">
              {b.status === 'PENDING' && (
                <Button
                  variant="primary"
                  size="sm"
                  className="h-8 px-2 text-xs"
                  onClick={() => handleConfirm(b)}
                  title="Xác nhận lịch hẹn"
                >
                  <CheckCircle size={13} className="mr-1" />
                  Xác nhận
                </Button>
              )}

              {b.status === 'CONFIRMED' && (
                <Button
                  variant="primary"
                  size="sm"
                  className="h-8 px-2 text-xs bg-emerald-600 hover:bg-emerald-700"
                  onClick={() => {
                    setCheckInBooking(b);
                    setCheckInOpen(true);
                  }}
                  title="Tiếp nhận xe vào gara"
                >
                  <LogIn size={13} className="mr-1" />
                  Tiếp nhận
                </Button>
              )}

              {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                <>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 w-8 p-0"
                    title="Dời lịch hẹn"
                    onClick={() => {
                      setRescheduleBooking(b);
                      setRescheduleOpen(true);
                    }}
                  >
                    <CalendarClock size={14} className="text-amber-500" />
                  </Button>

                  {b.status === 'CONFIRMED' && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0 text-slate-600"
                      title="Khách không đến (No-show)"
                      onClick={() => setNoShowBooking(b)}
                    >
                      <UserX size={14} />
                    </Button>
                  )}

                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                    title="Hủy lịch hẹn"
                    onClick={() => {
                      setCancelBooking(b);
                      setCancelOpen(true);
                    }}
                  >
                    <CalendarX size={14} />
                  </Button>
                </>
              )}
            </div>
          );
        },
      },
    ],
    [],
  );

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="MODULE M05"
        title="Lịch hẹn & Tiếp nhận xe"
        description="Quản lý lịch hẹn dịch vụ, tiếp nhận xe vào gara, ghi nhận hiện trạng và khởi tạo lệnh sửa chữa."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              onClick={() => {
                setCheckInBooking(null);
                setCheckInOpen(true);
              }}
            >
              <LogIn size={16} />
              Tiếp nhận vãng lai
            </Button>
            <Button ref={createTrigger} variant="primary" onClick={() => setEditorOpen(true)}>
              <Plus size={16} />
              Đặt lịch hẹn mới
            </Button>
          </div>
        }
      />

      <BookingFilters
        search={search}
        status={status}
        onSearchChange={setSearch}
        onStatusChange={setStatus}
        onReset={() => {
          setSearch('');
          setStatus('ALL');
        }}
      />

      {isLoading ? (
        <Loading label="Đang tải danh sách lịch hẹn…" />
      ) : isError ? (
        <QueryError
          error={error}
          retry={() => refetch()}
        />
      ) : compact ? (
        <BookingCards
          bookings={bookings}
          onConfirm={handleConfirm}
          onCheckIn={(b) => {
            setCheckInBooking(b);
            setCheckInOpen(true);
          }}
          onReschedule={(b) => {
            setRescheduleBooking(b);
            setRescheduleOpen(true);
          }}
          onCancel={(b) => {
            setCancelBooking(b);
            setCancelOpen(true);
          }}
          onNoShow={setNoShowBooking}
          onVehicleCheck={() => {}}
        />
      ) : (
        <DataTable columns={columns} data={bookings} emptyMessage="Không tìm thấy lịch hẹn nào." />
      )}

      {editorOpen && (
        <BookingEditor
          open={editorOpen}
          onClose={() => setEditorOpen(false)}
        />
      )}

      {checkInOpen && (
        <CheckInModal
          booking={checkInBooking}
          open={checkInOpen}
          onClose={() => {
            setCheckInOpen(false);
            setCheckInBooking(null);
          }}
          onCheckInSuccess={(orderCode, repairOrderId) => {
            setCreatedRepairOrder({ id: repairOrderId, code: orderCode });
            setVehicleCheckOpen(true);
          }}
        />
      )}

      {rescheduleOpen && rescheduleBooking && (
        <RescheduleModal
          booking={rescheduleBooking}
          open={rescheduleOpen}
          onClose={() => {
            setRescheduleOpen(false);
            setRescheduleBooking(null);
          }}
        />
      )}

      {cancelOpen && cancelBooking && (
        <CancelBookingDialog
          booking={cancelBooking}
          open={cancelOpen}
          onClose={() => {
            setCancelOpen(false);
            setCancelBooking(null);
          }}
        />
      )}

      {vehicleCheckOpen && createdRepairOrder && (
        <VehicleCheckModal
          repairOrderId={createdRepairOrder.id}
          orderCode={createdRepairOrder.code}
          open={vehicleCheckOpen}
          onClose={() => {
            setVehicleCheckOpen(false);
            setCreatedRepairOrder(null);
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(noShowBooking)}
        title="Xác nhận khách vắng mặt (No-show)"
        description={`Đánh dấu khách hàng lịch hẹn #${noShowBooking?.bookingCode} không đến gara? Khoang kỹ thuật đã đặt giữ sẽ tự động được giải phóng.`}
        confirmLabel="Xác nhận vắng mặt"
        cancelLabel="Đóng"
        destructive
        onConfirm={handleMarkNoShow}
        onCancel={() => setNoShowBooking(null)}
      />
    </div>
  );
}

export default BookingsPage;
