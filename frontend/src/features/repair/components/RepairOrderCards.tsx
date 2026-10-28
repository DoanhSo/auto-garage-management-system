import { Eye, Wrench } from 'lucide-react';
import { Button, Empty } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';
import type { RepairOrder, RepairOrderStatus } from '../types/repairTypes';
import { REPAIR_STATUS_LABELS } from './RepairOrderFilters';

const STATUS_TONE: Record<RepairOrderStatus, string> = {
  NEW: 'badge-neutral',
  DIAGNOSING: 'badge-warning',
  WAITING_APPROVAL: 'badge-warning',
  APPROVED: 'badge-primary',
  IN_PROGRESS: 'badge-primary',
  WAITING_PARTS: 'badge-neutral',
  WAITING_QC: 'badge-warning',
  QC_FAILED: 'badge-danger',
  COMPLETED: 'badge-success',
  INVOICED: 'badge-primary',
  CLOSED: 'badge-success',
  CANCELLED: 'badge-neutral',
};

export function RepairOrderStatusBadge({ status }: { status: RepairOrderStatus }) {
  return (
    <span className={`badge ${STATUS_TONE[status] ?? 'badge-neutral'}`}>
      {REPAIR_STATUS_LABELS[status] ?? status}
    </span>
  );
}

export function RepairOrderCards({
  orders,
  onOpenDetail,
}: {
  orders: RepairOrder[];
  onOpenDetail: (order: RepairOrder) => void;
}) {
  if (!orders.length)
    return (
      <Empty title="Không có kết quả" description="Thử thay đổi bộ lọc để tìm lệnh sửa chữa." />
    );
  return (
    <ul className="user-cards" aria-label="Danh sách lệnh sửa chữa">
      {orders.map((order) => (
        <li key={order.id}>
          <div className="user-card-top">
            <span className="avatar" aria-hidden>
              <Wrench size={16} />
            </span>
            <div>
              <strong>{order.orderCode}</strong>
              <span>Xe #{order.vehicleId}</span>
            </div>
            <RepairOrderStatusBadge status={order.status} />
          </div>
          <p className="user-card-email">
            {order.customerComplaint || 'Chưa ghi nhận triệu chứng'}
          </p>
          <div className="user-card-bottom">
            <span className="muted">{formatDateTime(order.receivedAt)}</span>
            <div className="row-actions">
              <Button
                variant="ghost"
                aria-label={`Xem lệnh sửa chữa ${order.orderCode}`}
                onClick={() => onOpenDetail(order)}
              >
                <Eye size={17} />
                Chi tiết
              </Button>
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
}
