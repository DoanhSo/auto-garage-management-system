import { useMemo, useRef, useState } from 'react';
import {
  Plus,
  SquarePen,
  Trash2,
  Car,
  History,
  ArrowRightLeft,
  Calendar,
  Gauge,
  User,
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
      notify.success(`Đã ngừng hoạt động phương tiện "${deactivatingVehicle.licensePlate}"`);
      setDeactivatingVehicle(null);
    } catch (err: unknown) {
      notify.error(errorMessage(err));
    }
  };

  const columns = useMemo<ColumnDef<Vehicle>[]>(
    () => [
      {
        id: 'licensePlate',
        header: 'Biển số & Định danh',
        cell: ({ row }) => (
          <div>
            <span className="font-mono font-bold text-sm bg-primary/10 text-primary px-2 py-0.5 rounded border border-primary/20 inline-block">
              {row.original.licensePlate}
            </span>
            {row.original.vin && (
              <div className="text-[11px] font-mono text-muted-foreground mt-1">
                VIN: {row.original.vin}
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
            <div className="font-medium text-foreground">
              {row.original.brand} {row.original.model}
            </div>
            <div className="text-xs text-muted-foreground mt-0.5">
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
            <div className="font-medium text-foreground">{row.original.customerName}</div>
            <div className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
              <User size={12} />
              {row.original.customerPhone}
            </div>
          </div>
        ),
      },
      {
        id: 'odometer',
        header: 'Số ODO',
        cell: ({ row }) => (
          <div className="flex items-center gap-1 text-sm font-medium">
            <Gauge size={14} className="text-muted-foreground" />
            <span>{row.original.odometer.toLocaleString('vi-VN')} km</span>
          </div>
        ),
      },
      {
        id: 'inspectionDueDate',
        header: 'Hạn đăng kiểm',
        cell: ({ row }) => {
          const date = row.original.inspectionDueDate;
          if (!date) return <span className="text-muted-foreground text-xs">—</span>;

          const time = new Date(date).getTime();
          const isExpired = time < Date.now();
          const isExpiringSoon = time - Date.now() < 15 * 86400000 && !isExpired;

          return (
            <div
              className={`flex items-center gap-1 text-xs font-medium ${
                isExpired
                  ? 'text-red-500'
                  : isExpiringSoon
                    ? 'text-amber-500'
                    : 'text-foreground'
              }`}
            >
              <Calendar size={13} />
              <span>{formatDate(date)}</span>
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
        header: () => <span className="sr-only">Thao tác</span>,
        cell: ({ row }) => {
          const vehicle = row.original;
          return (
            <div className="row-actions flex items-center justify-end gap-1">
              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs"
                title="Lịch sử bảo dưỡng sửa chữa"
                onClick={() => handleOpenTimeline(vehicle.id)}
              >
                <History size={14} className="mr-1 text-primary" />
                Lịch sử
              </Button>

              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs"
                title="Chuyển nhượng quyền sở hữu"
                onClick={() => handleOpenTransfer(vehicle)}
              >
                <ArrowRightLeft size={14} className="mr-1 text-amber-500" />
                Đổi chủ
              </Button>

              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0"
                title="Chỉnh sửa thông tin"
                onClick={() => handleOpenEdit(vehicle.id)}
              >
                <SquarePen size={14} />
              </Button>

              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                title="Ngừng hoạt động"
                onClick={() => setDeactivatingVehicle(vehicle)}
              >
                <Trash2 size={14} />
              </Button>
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
        eyebrow="MODULE M03"
        title="Quản lý Phương tiện"
        description="Quản lý danh sách phương tiện, theo dõi số ODO, chuyển nhượng quyền sở hữu và tra cứu lịch sử sửa chữa."
        actions={
          <Button ref={createTrigger} variant="primary" onClick={handleOpenCreate}>
            <Plus size={16} />
            Đăng ký xe mới
          </Button>
        }
      />

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
        <QueryError
          error={error}
          retry={() => refetch()}
        />
      ) : compact ? (
        <VehicleCards
          vehicles={vehicles}
          onEdit={handleOpenEdit}
          onTransfer={handleOpenTransfer}
          onTimeline={handleOpenTimeline}
          onDeactivate={setDeactivatingVehicle}
        />
      ) : (
        <DataTable columns={columns} data={vehicles} emptyMessage="Không tìm thấy phương tiện nào." />
      )}

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
        cancelLabel="Hủy"
        destructive
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setDeactivatingVehicle(null)}
      />
    </div>
  );
}

export default VehiclesPage;
