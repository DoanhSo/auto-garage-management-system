import { useAuth } from '../../../app/providers/AuthProvider';
import { FRONT_DESK_ROLES } from '../../../app/access';
import { useMemo, useRef, useState } from 'react';
import {
  Plus,
  SquarePen,
  Trash2,
  History,
  ArrowRightLeft,
  Calendar,
  Gauge,
  User,
  Car,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import {
  Button,
  ConfirmDialog,
  Loading,
  PageHeader,
  QueryError,
  StatusBadge,
} from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { errorMessage } from '../../../shared/api/errors';
import { formatDate } from '../../../shared/utils/format';
import type { Vehicle } from '../../../shared/types/contracts';
import { useVehiclesQuery, useDeleteVehicleMutation } from '../hooks/useVehicles';
import { VehicleEditor } from '../components/VehicleEditor';
import { VehicleFilters } from '../components/VehicleFilters';
import { VehicleCards } from '../components/VehicleCards';
import { TransferOwnershipDialog } from '../components/TransferOwnershipDialog';
import { VehicleTimelineModal } from '../components/VehicleTimelineModal';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';

export function VehiclesPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();
  const { user } = useAuth();
  const canManage = !!user && FRONT_DESK_ROLES.includes(user.role);

  const [search, setSearch] = useState('');
  const [isActive, setIsActive] = useState('ALL');

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);

  const [transferVehicle, setTransferVehicle] = useState<Vehicle | null>(null);
  const [transferOpen, setTransferOpen] = useState(false);

  const [timelineVehicleId, setTimelineVehicleId] = useState<string | null>(null);
  const [timelineOpen, setTimelineOpen] = useState(false);

  const [deactivatingVehicle, setDeactivatingVehicle] = useState<Vehicle | null>(null);

  const createTrigger = useRef<HTMLButtonElement>(null);
  const debouncedSearch = useDebouncedValue(search);

  const queryParams = useMemo(() => {
    return {
      search: debouncedSearch.trim() || undefined,
      isActive: isActive === 'ACTIVE' ? true : isActive === 'INACTIVE' ? false : undefined,
    };
  }, [debouncedSearch, isActive]);

  const { data: vehicles = [], isLoading, isError, error, refetch } = useVehiclesQuery(queryParams);
  const deleteMutation = useDeleteVehicleMutation();

  const handleOpenCreate = () => {
    setEditingId(null);
    setEditorOpen(true);
  };

  const handleOpenEdit = (id: string) => {
    setEditingId(id);
    setEditorOpen(true);
  };

  const handleOpenTransfer = (vehicle: Vehicle) => {
    setTransferVehicle(vehicle);
    setTransferOpen(true);
  };

  const handleOpenTimeline = (id: string) => {
    setTimelineVehicleId(id);
    setTimelineOpen(true);
  };

  const handleConfirmDeactivate = async () => {
    if (!deactivatingVehicle) return;
    try {
      await deleteMutation.mutateAsync(deactivatingVehicle.id);
      notify(`Đã ngừng hoạt động phương tiện "${deactivatingVehicle.licensePlate}"`);
      setDeactivatingVehicle(null);
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const columns = useMemo<ColumnDef<Vehicle>[]>(
    () => [
      {
        id: 'licensePlate',
        header: 'Biển số & Định danh',
        cell: ({ row }) => (
          <div>
            <span className="plate-badge">{row.original.licensePlate}</span>
            {row.original.vin && (
              <div className="muted nowrap" style={{ marginTop: 4 }}>
                <span className="code-tag">VIN: {row.original.vin}</span>
              </div>
            )}
          </div>
        ),
      },
      {
        id: 'vehicleInfo',
        header: 'Thông số xe',
        cell: ({ row }) => (
          <div>
            <strong>
              {row.original.brand} {row.original.model}
            </strong>
            <div className="muted nowrap">
              {[
                row.original.manufactureYear ? `Năm ${row.original.manufactureYear}` : null,
                row.original.color,
                row.original.fuelType,
              ]
                .filter(Boolean)
                .join(' • ')}
            </div>
          </div>
        ),
      },
      {
        id: 'owner',
        header: 'Chủ sở hữu hiện tại',
        cell: ({ row }) => (
          <div>
            <strong>{row.original.customerName}</strong>
            <div className="muted nowrap flex items-center gap-1" style={{ marginTop: 2 }}>
              <User size={12} />
              <span>{row.original.customerPhone}</span>
            </div>
          </div>
        ),
      },
      {
        id: 'odometer',
        header: 'Số ODO',
        cell: ({ row }) => (
          <div className="flex items-center gap-1">
            <Gauge size={14} className="muted" />
            <span>{row.original.odometer.toLocaleString('vi-VN')} km</span>
          </div>
        ),
      },
      {
        id: 'inspectionDueDate',
        header: 'Hạn đăng kiểm',
        cell: ({ row }) => {
          const date = row.original.inspectionDueDate;
          if (!date) return <span className="muted">—</span>;

          const time = new Date(date).getTime();
          const isExpired = time < Date.now();
          const isExpiringSoon = time - Date.now() < 15 * 86400000 && !isExpired;

          return (
            <div className="flex items-center gap-1">
              <Calendar size={13} className="muted" />
              <span
                className={
                  isExpired ? 'badge badge-danger' : isExpiringSoon ? 'badge badge-warning' : 'muted nowrap'
                }
              >
                {formatDate(date)}
              </span>
            </div>
          );
        },
      },
      {
        id: 'status',
        header: 'Trạng thái',
        cell: ({ row }) => <StatusBadge active={row.original.isActive} />,
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => {
          const vehicle = row.original;
          return (
            <div className="row-actions">
              <Button
                variant="ghost"
                aria-label={`Lịch sử kỹ thuật xe ${vehicle.licensePlate}`}
                onClick={() => handleOpenTimeline(vehicle.id)}
              >
                <History size={16} />
                Lịch sử
              </Button>

              {canManage && (
                <Button
                  variant="ghost"
                  aria-label={`Chuyển nhượng quyền sở hữu xe ${vehicle.licensePlate}`}
                  onClick={() => handleOpenTransfer(vehicle)}
                >
                  <ArrowRightLeft size={16} />
                  Đổi chủ
                </Button>
              )}

              {canManage && (
                <Button
                  variant="ghost"
                  aria-label={`Sửa thông tin xe ${vehicle.licensePlate}`}
                  onClick={() => handleOpenEdit(vehicle.id)}
                >
                  <SquarePen size={16} />
                  Sửa
                </Button>
              )}

              {canManage && vehicle.isActive && (
                <Button
                  variant="ghost"
                  className="delete-action"
                  aria-label={`Ngừng hoạt động xe ${vehicle.licensePlate}`}
                  onClick={() => setDeactivatingVehicle(vehicle)}
                >
                  <Trash2 size={16} />
                  Ngừng HĐ
                </Button>
              )}
            </div>
          );
        },
      },
    ],
    [canManage],
  );

  return (
    <>
      <PageHeader
        eyebrow="TIẾP NHẬN & DỊCH VỤ · M03"
        title="Quản lý phương tiện"
        description="Quản lý danh sách phương tiện, theo dõi số ODO, chuyển nhượng quyền sở hữu (BR04) và tra cứu lịch sử sửa chữa (UC38)."
        actions={
          canManage && (
            <Button ref={createTrigger} variant="primary" onClick={handleOpenCreate}>
              <Plus size={16} aria-hidden />
              Đăng ký xe mới
            </Button>
          )
        }
      />

      <div className="info-strip">
        <Car size={18} />
        <span>
          Đăng ký thông tin phương tiện, theo dõi số ODO, chuyển nhượng quyền sở hữu (BR04) và tra cứu cây lịch sử sửa chữa (UC38).
        </span>
      </div>

      <section className="panel table-panel">
        <VehicleFilters
          search={search}
          isActive={isActive}
          onSearchChange={setSearch}
          onStatusChange={setIsActive}
          onReset={() => {
            setSearch('');
            setIsActive('ALL');
          }}
        />

        {isLoading ? (
          <Loading label="Đang tải danh sách phương tiện…" />
        ) : isError ? (
          <QueryError error={error} retry={() => refetch()} />
        ) : compact ? (
          <VehicleCards
            canManage={canManage}
            vehicles={vehicles}
            onEdit={handleOpenEdit}
            onTransfer={handleOpenTransfer}
            onTimeline={handleOpenTimeline}
            onDeactivate={setDeactivatingVehicle}
          />
        ) : (
          <DataTable<Vehicle>
            columns={columns}
            data={vehicles}
            caption="Danh sách phương tiện"
          />
        )}
      </section>

      {editorOpen && (
        <VehicleEditor
          vehicleId={editingId}
          open={editorOpen}
          onClose={() => {
            setEditorOpen(false);
            setEditingId(null);
          }}
        />
      )}

      {transferOpen && transferVehicle && (
        <TransferOwnershipDialog
          vehicle={transferVehicle}
          open={transferOpen}
          onClose={() => {
            setTransferOpen(false);
            setTransferVehicle(null);
          }}
        />
      )}

      {timelineOpen && timelineVehicleId && (
        <VehicleTimelineModal
          vehicleId={timelineVehicleId}
          open={timelineOpen}
          onClose={() => {
            setTimelineOpen(false);
            setTimelineVehicleId(null);
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(deactivatingVehicle)}
        title="Xác nhận ngừng hoạt động phương tiện"
        description={`Bạn có chắc muốn chuyển phương tiện biển số "${deactivatingVehicle?.licensePlate}" sang trạng thái ngừng hoạt động? Xe này sẽ không thể tạo lệnh sửa chữa mới.`}
        confirmLabel="Ngừng hoạt động"
        danger
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setDeactivatingVehicle(null)}
      />
    </>
  );
}

export default VehiclesPage;
