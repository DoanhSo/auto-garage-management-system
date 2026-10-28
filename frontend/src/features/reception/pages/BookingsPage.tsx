import { useAuth } from '../../../app/providers/AuthProvider';
import { FRONT_DESK_ROLES } from '../../../app/access';
import { useMemo, useRef, useState, useCallback } from 'react';
import {
  Plus,
  LogIn,
  CheckCircle,
  CalendarClock,
  CalendarX,
  UserX,
  Calendar,
  CalendarCheck,
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
import { BookingCards, BookingStatusBadge } from '../components/BookingCards';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';

export function BookingsPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();
  const { user } = useAuth();
  const canManage = !!user && FRONT_DESK_ROLES.includes(user.role);

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
  const [createdRepairOrder, setCreatedRepairOrder] = useState<{ id: string; code: string } | null>(
    null,
  );
  const [vehicleCheckOpen, setVehicleCheckOpen] = useState(false);

  const createTrigger = useRef<HTMLButtonElement>(null);
  const debouncedSearch = useDebouncedValue(search);

  const queryParams = useMemo(() => {
    return {
      status: status !== 'ALL' ? (status as BookingStatus) : undefined,
    };
  }, [status]);

  const {
    data: allBookings = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useBookingsQuery(queryParams);
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

  const handleConfirm = useCallback(
    async (booking: Booking) => {
      try {
        await confirmMutation.mutateAsync({ id: booking.id });
        notify(`Đã xác nhận lịch hẹn #${booking.bookingCode}`);
      } catch (err: unknown) {
        notify(errorMessage(err) ?? 'Xác nhận lịch hẹn thất bại');
      }
    },
    [confirmMutation, notify],
  );

  const handleMarkNoShow = async () => {
    if (!noShowBooking) return;
    try {
      await noShowMutation.mutateAsync({ id: noShowBooking.id });
      notify(`Đã đánh dấu vắng mặt cho lịch hẹn #${noShowBooking.bookingCode}`);
      setNoShowBooking(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Cập nhật thất bại');
    }
  };

  const columns = useMemo<ColumnDef<Booking>[]>(
    () => [
      {
        id: 'bookingCode',
        header: 'Mã hẹn',
        cell: ({ row }) => (
          <span className="code-tag">#{row.original.bookingCode}</span>
        ),
      },
      {
        id: 'appointmentAt',
        header: 'Thời gian hẹn',
        cell: ({ row }) => (
          <div className="flex items-center gap-1">
            <Calendar size={13} className="muted" />
            <strong className="nowrap">{formatDateTime(row.original.appointmentAt)}</strong>
          </div>
        ),
      },
      {
        id: 'customer',
        header: 'Khách hàng',
        cell: ({ row }) => (
          <div>
            <strong>{row.original.customerName}</strong>
            <div className="muted nowrap">{row.original.customerPhone}</div>
          </div>
        ),
      },
      {
        id: 'vehicle',
        header: 'Phương tiện',
        cell: ({ row }) => (
          <div>
            <span className="plate-badge">{row.original.licensePlate}</span>
            {row.original.vehicleModel && (
              <div className="muted nowrap" style={{ marginTop: 2 }}>
                {row.original.vehicleModel}
              </div>
            )}
          </div>
        ),
      },
      {
        id: 'service',
        header: 'Dịch vụ yêu cầu',
        cell: ({ row }) => (
          <div>
            <span>{row.original.serviceName || 'Khám tổng quát'}</span>
            {row.original.requestDescription && (
              <div className="muted nowrap" style={{ marginTop: 2 }}>
                {row.original.requestDescription}
              </div>
            )}
          </div>
        ),
      },
      {
        id: 'status',
        header: 'Trạng thái',
        cell: ({ row }) => <BookingStatusBadge status={row.original.status} />,
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => {
          const b = row.original;
          return (
            <div className="row-actions">
              {canManage && b.status === 'PENDING' && (
                <Button
                  variant="primary"
                  aria-label={`Xác nhận lịch hẹn ${b.bookingCode}`}
                  onClick={() => handleConfirm(b)}
                >
                  <CheckCircle size={16} />
                  Xác nhận
                </Button>
              )}

              {canManage && b.status === 'CONFIRMED' && (
                <Button
                  variant="primary"
                  aria-label={`Tiếp nhận xe ${b.licensePlate} vào xưởng`}
                  onClick={() => {
                    setCheckInBooking(b);
                    setCheckInOpen(true);
                  }}
                >
                  <LogIn size={16} />
                  Tiếp nhận
                </Button>
              )}

              {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                <>
                  <Button
                    variant="ghost"
                    aria-label={`Dời lịch hẹn ${b.bookingCode}`}
                    onClick={() => {
                      setRescheduleBooking(b);
                      setRescheduleOpen(true);
                    }}
                  >
                    <CalendarClock size={16} />
                    Dời lịch
                  </Button>

                  {canManage && b.status === 'CONFIRMED' && (
                    <Button
                      variant="ghost"
                      aria-label={`Đánh dấu vắng mặt lịch hẹn ${b.bookingCode}`}
                      onClick={() => setNoShowBooking(b)}
                    >
                      <UserX size={16} />
                      Vắng mặt
                    </Button>
                  )}

                  <Button
                    variant="ghost"
                    className="delete-action"
                    aria-label={`Hủy lịch hẹn ${b.bookingCode}`}
                    onClick={() => {
                      setCancelBooking(b);
                      setCancelOpen(true);
                    }}
                  >
                    <CalendarX size={16} />
                    Hủy
                  </Button>
                </>
              )}
            </div>
          );
        },
      },
    ],
    [handleConfirm, canManage],
  );

  return (
    <>
      <PageHeader
        eyebrow="ĐẶT LỊCH HẸN"
        title="Lịch hẹn & Tiếp nhận xe"
        description="Quản lý lịch hẹn dịch vụ, thực hiện quy trình tiếp nhận xe vào xưởng (Check-in) và lập biên bản kiểm tra tình trạng xe."
        actions={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {canManage && (
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
            )}
            <Button ref={createTrigger} variant="primary" onClick={() => setEditorOpen(true)}>
              <Plus size={16} aria-hidden />
              Đặt lịch hẹn mới
            </Button>
          </div>
        }
      />

      <div className="info-strip">
        <CalendarCheck size={18} />
        <span>
          Quy trình tiếp nhận xe: Đặt lịch hẹn → Xác nhận → Tiếp nhận vào xưởng → Lập biên bản kiểm tra & đính kèm ảnh hiện trạng.
        </span>
      </div>

      <section className="panel table-panel">
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
          <QueryError error={error} retry={() => refetch()} />
        ) : compact ? (
          <BookingCards
            canManage={canManage}
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
          />
        ) : (
          <DataTable<Booking>
            columns={columns}
            data={bookings}
            caption="Danh sách lịch hẹn & tiếp nhận"
          />
        )}
      </section>

      {editorOpen && <BookingEditor open={editorOpen} onClose={() => setEditorOpen(false)} />}

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
        danger
        onConfirm={handleMarkNoShow}
        onCancel={() => setNoShowBooking(null)}
      />
    </>
  );
}

export default BookingsPage;
