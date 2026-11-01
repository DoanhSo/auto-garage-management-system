import { Badge, Loading, QueryError, Sheet } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';
import { useBayReservationsQuery } from '../hooks/useScheduling';
import {
  reservationStatusLabels,
  reservationTypeLabels,
  type ServiceBay,
} from '../types/scheduling';

export function BayReservationsModal({
  bay,
  open,
  onClose,
}: {
  bay: ServiceBay | null;
  open: boolean;
  onClose: () => void;
}) {
  const query = useBayReservationsQuery(open ? bay?.id : null);
  const rows = query.data?.content ?? [];

  return (
    <Sheet
      open={open}
      placement="center"
      size="lg"
      title={`Lịch đặt khoang ${bay?.bayCode ?? ''}`}
      description="Các lượt giữ chỗ lịch hẹn, sửa chữa và bảo trì của khoang."
      onClose={onClose}
    >
      {query.isLoading ? (
        <Loading label="Đang tải lịch đặt khoang…" />
      ) : query.isError ? (
        <QueryError error={query.error} retry={() => query.refetch()} />
      ) : rows.length === 0 ? (
        <p className="empty-state">Khoang này chưa có lượt đặt chỗ nào.</p>
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Loại</th>
                <th>Thời gian</th>
                <th>Tham chiếu</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td>{reservationTypeLabels[r.reservationType] ?? r.reservationType}</td>
                  <td>
                    {formatDateTime(r.startsAt)} → {formatDateTime(r.endsAt)}
                  </td>
                  <td>
                    {r.repairOrderId
                      ? `Lệnh sửa chữa #${r.repairOrderId}`
                      : r.bookingId
                        ? `Lịch hẹn #${r.bookingId}`
                        : (r.notes ?? '—')}
                  </td>
                  <td>
                    <Badge variant={r.status === 'IN_USE' ? 'primary' : 'secondary'}>
                      {reservationStatusLabels[r.status] ?? r.status}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Sheet>
  );
}
