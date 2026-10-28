import { useMemo, useState } from 'react';
import { Plus, SquarePen, Trash2, CalendarRange, Wrench, RefreshCw, XCircle } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import { Button, PageHeader, Loading, QueryError, ConfirmDialog } from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { formatDateTime } from '../../../shared/utils/format';
import {
  useServiceBaysQuery,
  useDeleteServiceBayMutation,
  useShiftsQuery,
  useCancelShiftMutation,
} from '../hooks/useScheduling';
import { ServiceBayEditor } from '../components/ServiceBayEditor';
import { ShiftEditor } from '../components/ShiftEditor';
import type { ServiceBay, Shift } from '../types/scheduling';
import { shiftStatusLabels, bayTypeLabels } from '../types/scheduling';

export function SchedulingPage() {
  const notify = useToast();
  const [activeTab, setActiveTab] = useState<'bays' | 'shifts'>('bays');

  // Modals state
  const [bayModalOpen, setBayModalOpen] = useState(false);
  const [editingBay, setEditingBay] = useState<ServiceBay | null>(null);
  const [deletingBayId, setDeletingBayId] = useState<number | string | null>(null);

  const [shiftModalOpen, setShiftModalOpen] = useState(false);
  const [cancelingShiftId, setCancelingShiftId] = useState<number | string | null>(null);

  // Queries
  const {
    data: baysData,
    isLoading: isLoadingBays,
    isError: isErrorBays,
    error: errorBays,
    refetch: refetchBays,
    isFetching: isFetchingBays,
  } = useServiceBaysQuery();

  const {
    data: shiftsData,
    isLoading: isLoadingShifts,
    isError: isErrorShifts,
    error: errorShifts,
    refetch: refetchShifts,
    isFetching: isFetchingShifts,
  } = useShiftsQuery();

  const deleteBayMutation = useDeleteServiceBayMutation();
  const cancelShiftMutation = useCancelShiftMutation();

  const bays = useMemo(() => baysData?.content ?? [], [baysData?.content]);
  const shifts = useMemo(() => shiftsData?.content ?? [], [shiftsData?.content]);

  // Bay Columns
  const bayColumns = useMemo<ColumnDef<ServiceBay>[]>(() => [
    {
      accessorKey: 'bayCode',
      header: 'Mã khoang',
      cell: ({ row }) => <span className="font-semibold text-primary">{row.original.bayCode}</span>,
    },
    {
      accessorKey: 'bayName',
      header: 'Tên khoang',
      cell: ({ row }) => <span>{row.original.bayName}</span>,
    },
    {
      accessorKey: 'bayType',
      header: 'Loại khoang',
      cell: ({ row }) => (
        <span className="text-xs px-2 py-0.5 rounded-full bg-muted font-medium">
          {bayTypeLabels[row.original.bayType] || row.original.bayType}
        </span>
      ),
    },
    {
      accessorKey: 'isActive',
      header: 'Trạng thái',
      cell: ({ row }) => (
        <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${row.original.isActive ? 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300' : 'bg-red-100 text-red-800'}`}>
          {row.original.isActive ? 'Đang hoạt động' : 'Tạm dừng'}
        </span>
      ),
    },
    {
      accessorKey: 'description',
      header: 'Mô tả trang bị',
      cell: ({ row }) => <span className="text-xs text-muted-foreground">{row.original.description || '-'}</span>,
    },
    {
      id: 'actions',
      header: 'Thao tác',
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button
            variant="secondary"
            onClick={() => {
              setEditingBay(row.original);
              setBayModalOpen(true);
            }}
          >
            <SquarePen size={13} /> Sửa
          </Button>
          <Button
            variant="danger"
            onClick={() => setDeletingBayId(row.original.id)}
          >
            <Trash2 size={13} /> Xóa
          </Button>
        </div>
      ),
    },
  ], []);

  // Shift Columns
  const shiftColumns = useMemo<ColumnDef<Shift>[]>(() => [
    {
      accessorKey: 'employeeId',
      header: 'Mã nhân viên (ID)',
      cell: ({ row }) => <span className="font-semibold">NV #{row.original.employeeId}</span>,
    },
    {
      accessorKey: 'startsAt',
      header: 'Bắt đầu ca',
      cell: ({ row }) => <span className="text-xs">{formatDateTime(row.original.startsAt)}</span>,
    },
    {
      accessorKey: 'endsAt',
      header: 'Kết thúc ca',
      cell: ({ row }) => <span className="text-xs">{formatDateTime(row.original.endsAt)}</span>,
    },
    {
      accessorKey: 'status',
      header: 'Trạng thái ca',
      cell: ({ row }) => {
        const s = row.original.status;
        const color =
          s === 'COMPLETED' ? 'bg-green-100 text-green-800' : s === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-800' : s === 'CANCELLED' ? 'bg-red-100 text-red-800' : 'bg-muted text-muted-foreground';
        return (
          <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${color}`}>
            {shiftStatusLabels[s] || s}
          </span>
        );
      },
    },
    {
      accessorKey: 'notes',
      header: 'Ghi chú',
      cell: ({ row }) => <span className="text-xs text-muted-foreground">{row.original.notes || '-'}</span>,
    },
    {
      id: 'actions',
      header: 'Thao tác',
      cell: ({ row }) => (
        row.original.status !== 'CANCELLED' && (
          <Button
            variant="danger"
            onClick={() => setCancelingShiftId(row.original.id)}
          >
            <XCircle size={13} /> Hủy ca
          </Button>
        )
      ),
    },
  ], []);

  const handleDeleteBay = async () => {
    if (!deletingBayId) return;
    try {
      await deleteBayMutation.mutateAsync(deletingBayId);
      notify('Đã xóa khoang sửa chữa thành công');
      setDeletingBayId(null);
    } catch {
      notify('Không thể xóa khoang sửa chữa');
    }
  };

  const handleCancelShift = async () => {
    if (!cancelingShiftId) return;
    try {
      await cancelShiftMutation.mutateAsync({ id: cancelingShiftId, reason: 'Điều chỉnh lịch' });
      notify('Đã hủy ca làm việc');
      setCancelingShiftId(null);
    } catch {
      notify('Không thể hủy ca làm việc');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Khoang sửa chữa & Lịch ca làm việc"
        description="Quản lý vị trí cầu nâng, phân công ca làm việc và điều phối tài nguyên xưởng dịch vụ"
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              onClick={() => (activeTab === 'bays' ? refetchBays() : refetchShifts())}
              pending={activeTab === 'bays' ? isFetchingBays : isFetchingShifts}
            >
              <RefreshCw size={15} /> Làm mới
            </Button>
            {activeTab === 'bays' ? (
              <Button
                variant="primary"
                onClick={() => {
                  setEditingBay(null);
                  setBayModalOpen(true);
                }}
              >
                <Plus size={16} /> Thêm Khoang
              </Button>
            ) : (
              <Button variant="primary" onClick={() => setShiftModalOpen(true)}>
                <Plus size={16} /> Xếp Ca làm việc
              </Button>
            )}
          </div>
        }
      />

      {/* Tabs */}
      <div className="flex border-b gap-6">
        <button
          type="button"
          className={`pb-3 text-sm font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'bays' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground'
          }`}
          onClick={() => setActiveTab('bays')}
        >
          <Wrench size={16} /> Khoang sửa chữa ({bays.length})
        </button>
        <button
          type="button"
          className={`pb-3 text-sm font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'shifts' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground'
          }`}
          onClick={() => setActiveTab('shifts')}
        >
          <CalendarRange size={16} /> Lịch ca làm việc KTV ({shifts.length})
        </button>
      </div>

      {activeTab === 'bays' && (
        isLoadingBays ? (
          <Loading label="Đang tải danh sách khoang…" />
        ) : isErrorBays ? (
          <QueryError error={errorBays} retry={() => refetchBays()} />
        ) : (
          <DataTable<ServiceBay>
            caption="Danh sách khoang sửa chữa"
            columns={bayColumns}
            data={bays}
            busy={isFetchingBays}
          />
        )
      )}

      {activeTab === 'shifts' && (
        isLoadingShifts ? (
          <Loading label="Đang tải danh sách ca làm việc…" />
        ) : isErrorShifts ? (
          <QueryError error={errorShifts} retry={() => refetchShifts()} />
        ) : (
          <DataTable<Shift>
            caption="Danh sách ca làm việc"
            columns={shiftColumns}
            data={shifts}
            busy={isFetchingShifts}
          />
        )
      )}

      {/* Modals */}
      <ServiceBayEditor
        bay={editingBay}
        open={bayModalOpen}
        onClose={() => setBayModalOpen(false)}
        onSaved={() => refetchBays()}
      />

      <ShiftEditor
        open={shiftModalOpen}
        onClose={() => setShiftModalOpen(false)}
        onSaved={() => refetchShifts()}
      />

      {/* Xóa khoang confirm */}
      <ConfirmDialog
        open={!!deletingBayId}
        title="Xác nhận xóa khoang sửa chữa?"
        description="Khoang sửa chữa sẽ bị xóa khỏi hệ thống. Các phiếu sửa chữa trong quá khứ vẫn được lưu lịch sử."
        danger
        onCancel={() => setDeletingBayId(null)}
        onConfirm={handleDeleteBay}
      />

      {/* Hủy ca confirm */}
      <ConfirmDialog
        open={!!cancelingShiftId}
        title="Xác nhận hủy ca làm việc?"
        description="Ca làm việc này sẽ được chuyển sang trạng thái ĐÃ HỦY (CANCELLED)."
        danger
        onCancel={() => setCancelingShiftId(null)}
        onConfirm={handleCancelShift}
      />
    </div>
  );
}
