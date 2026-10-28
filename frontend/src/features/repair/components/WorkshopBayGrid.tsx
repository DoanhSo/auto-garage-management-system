import { useMemo } from 'react';
import { Activity, Car, CheckCircle2, Clock, Gauge, Layers, Wrench, AlertCircle } from 'lucide-react';
import { Button, Loading, QueryError } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';
import type { RepairOrder } from '../types/repairTypes';
import { RepairOrderStatusBadge } from './RepairOrderCards';
import { useServiceBaysQuery, useActiveReservationsQuery } from '../../scheduling/hooks/useScheduling';
import type { ServiceBay } from '../../scheduling/types/schedulingTypes';

function Metric({
  label,
  value,
  description,
  icon,
  tone,
}: {
  label: string;
  value: string;
  description: string;
  icon: React.ReactNode;
  tone: 'primary' | 'success' | 'warning' | 'info';
}) {
  return (
    <article className={`metric-card metric-tone-${tone}`}>
      <div className="metric-label">
        <span>{label}</span>
        <span className="metric-icon">{icon}</span>
      </div>
      <strong className="metric-value">{value}</strong>
      <p>{description}</p>
    </article>
  );
}

export function WorkshopBayGrid({
  orders,
  onOpenDetail,
}: {
  orders: RepairOrder[];
  onOpenDetail: (order: RepairOrder) => void;
}) {
  const { data: baysData, isLoading: isLoadingBays, isError: isErrorBays, error: errorBays, refetch: refetchBays } = useServiceBaysQuery();
  const { data: reservations = [], isLoading: isLoadingRes, isError: isErrorRes, error: errorRes, refetch: refetchRes } = useActiveReservationsQuery();

  const bays: ServiceBay[] = baysData?.content ?? [];

  // Map each bay to its active reservation and matching repair order
  const bayStatusList = useMemo(() => {
    return bays.map((bay) => {
      const activeRes = reservations.find(
        (r) => r.bayId === bay.id && (r.status === 'IN_USE' || r.status === 'HELD'),
      );
      const matchedOrder = activeRes?.repairOrderId
        ? orders.find((o) => String(o.id) === String(activeRes.repairOrderId)) ?? null
        : null;

      return {
        bay,
        reservation: activeRes ?? null,
        order: matchedOrder,
      };
    });
  }, [bays, reservations, orders]);

  const occupied = bayStatusList.filter((item) => !!item.reservation).length;
  const free = Math.max(0, bays.length - occupied);
  const utilization = bays.length > 0 ? Math.round((occupied / bays.length) * 100) : 0;

  const waitingOrder = useMemo(
    () => orders.find((o) => o.status === 'NEW' || o.status === 'APPROVED'),
    [orders],
  );

  if (isLoadingBays || isLoadingRes) {
    return <Loading label="Đang tải sơ đồ mặt bằng xưởng và trạng thái khoang..." />;
  }

  if (isErrorBays || isErrorRes) {
    return (
      <QueryError
        error={errorBays || errorRes}
        retry={() => {
          void refetchBays();
          void refetchRes();
        }}
      />
    );
  }

  return (
    <div className="bay-board">
      <div className="metric-grid period-metrics">
        <Metric
          label="Tổng số khoang"
          value={`${bays.length}`}
          description="Cầu nâng & khoang hoạt động"
          icon={<Layers size={17} />}
          tone="primary"
        />
        <Metric
          label="Đang có xe"
          value={`${occupied}`}
          description="Cầu nâng đang làm việc"
          icon={<Wrench size={17} />}
          tone="info"
        />
        <Metric
          label="Khoang đang trống"
          value={`${free}`}
          description="Sẵn sàng tiếp nhận xe"
          icon={<CheckCircle2 size={17} />}
          tone="success"
        />
        <Metric
          label="Công suất xưởng"
          value={`${utilization}%`}
          description="Tỷ lệ khoang đang sử dụng"
          icon={<Activity size={17} />}
          tone="warning"
        />
      </div>

      <ul className="bay-grid" aria-label="Sơ đồ khoang sửa chữa">
        {bayStatusList.map(({ bay, reservation, order }) => {
          const isOccupied = !!reservation;

          return (
            <li key={bay.id} className={`bay-card${isOccupied ? '' : ' is-free'}`}>
              <div className="bay-card-head">
                <div>
                  <strong>{bay.bayName}</strong>
                  <span>{bay.description || bay.bayCode}</span>
                </div>
                {order ? (
                  <RepairOrderStatusBadge status={order.status} />
                ) : reservation ? (
                  <span className={`badge ${reservation.status === 'IN_USE' ? 'badge-primary' : 'badge-warning'}`}>
                    {reservation.status === 'IN_USE' ? 'Đang hoạt động' : 'Đã giữ chỗ'}
                  </span>
                ) : (
                  <span className="badge badge-success">Sẵn sàng nhận xe</span>
                )}
              </div>

              {order ? (
                <dl className="bay-card-body">
                  <div>
                    <dt>
                      <Car size={14} aria-hidden /> Mã lệnh
                    </dt>
                    <dd className="code-cell font-bold">#{order.orderCode}</dd>
                  </div>
                  <div>
                    <dt>
                      <Gauge size={14} aria-hidden /> Xe & ODO vào
                    </dt>
                    <dd>Xe #{order.vehicleId} · {order.odometerIn.toLocaleString('vi-VN')} km</dd>
                  </div>
                  {order.customerComplaint && (
                    <div>
                      <dt>
                        <AlertCircle size={14} aria-hidden /> Hiện tượng
                      </dt>
                      <dd className="italic truncate max-w-[200px]" title={order.customerComplaint}>
                        {order.customerComplaint}
                      </dd>
                    </div>
                  )}
                  <div>
                    <dt>
                      <Clock size={14} aria-hidden /> Tiếp nhận lúc
                    </dt>
                    <dd>{formatDateTime(order.receivedAt)}</dd>
                  </div>
                </dl>
              ) : reservation ? (
                <dl className="bay-card-body">
                  <div>
                    <dt>
                      <Car size={14} aria-hidden /> Lệnh sửa chữa
                    </dt>
                    <dd className="code-cell">#{reservation.repairOrderId || 'Đặt lịch'}</dd>
                  </div>
                  <div>
                    <dt>
                      <Clock size={14} aria-hidden /> Thời gian
                    </dt>
                    <dd>{formatDateTime(reservation.startsAt)}</dd>
                  </div>
                  {reservation.notes && (
                    <div>
                      <dt>Ghi chú</dt>
                      <dd className="italic truncate max-w-[200px]">{reservation.notes}</dd>
                    </div>
                  )}
                </dl>
              ) : (
                <p className="bay-card-empty">Khoang cầu nâng đang trống, sẵn sàng tiếp nhận xe vào bảo dưỡng hoặc sửa chữa.</p>
              )}

              <div className="bay-card-foot">
                {order ? (
                  <Button variant="secondary" onClick={() => onOpenDetail(order)}>
                    Chi tiết lệnh sửa chữa
                  </Button>
                ) : (
                  <Button
                    variant="ghost"
                    disabled={!waitingOrder}
                    onClick={() => waitingOrder && onOpenDetail(waitingOrder)}
                  >
                    {waitingOrder ? 'Điều phối xe chờ vào' : 'Không có xe chờ'}
                  </Button>
                )}
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
