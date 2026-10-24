import { useMemo, useRef, useState } from 'react';
import {
  Plus,
  SquarePen,
  Trash2,
  Clock,
  ShieldCheck,
  RotateCw,
  Box,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import { useAuth } from '../../../app/providers/AuthProvider';
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
import { money } from '../../../shared/utils/format';
import type { Service, ServiceType } from '../../../shared/types/contracts';
import { useServicesQuery, useDeleteServiceMutation } from '../hooks/useCatalog';
import { ServiceEditor } from '../components/ServiceEditor';
import { ServiceFilters } from '../components/ServiceFilters';
import { ServiceCards } from '../components/ServiceCards';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';

export function ServicesPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [search, setSearch] = useState('');
  const [serviceType, setServiceType] = useState('ALL');
  const [isActive, setIsActive] = useState('ALL');

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [deactivatingService, setDeactivatingService] = useState<Service | null>(null);

  const createTrigger = useRef<HTMLButtonElement>(null);
  const debouncedSearch = useDebouncedValue(search);

  const queryParams = useMemo(() => {
    return {
      search: debouncedSearch.trim() || undefined,
      type: serviceType !== 'ALL' ? (serviceType as ServiceType) : undefined,
      isActive: isActive === 'ACTIVE' ? true : isActive === 'INACTIVE' ? false : undefined,
    };
  }, [debouncedSearch, serviceType, isActive]);

  const { data: services = [], isLoading, isError, error, refetch } = useServicesQuery(queryParams);
  const deleteMutation = useDeleteServiceMutation();

  const handleOpenCreate = () => {
    setEditingId(null);
    setEditorOpen(true);
  };

  const handleOpenEdit = (id: string) => {
    setEditingId(id);
    setEditorOpen(true);
  };

  const handleConfirmDeactivate = async () => {
    if (!deactivatingService) return;
    try {
      await deleteMutation.mutateAsync(deactivatingService.id);
      notify.success(`Đã ngừng áp dụng dịch vụ "${deactivatingService.serviceName}"`);
      setDeactivatingService(null);
    } catch (err: unknown) {
      notify.error(errorMessage(err));
    }
  };

  const columns = useMemo<ColumnDef<Service>[]>(
    () => [
      {
        id: 'serviceCode',
        header: 'Mã DV',
        cell: ({ row }) => (
          <span className="font-mono text-xs font-semibold bg-muted text-muted-foreground px-2 py-0.5 rounded border border-border">
            {row.original.serviceCode}
          </span>
        ),
      },
      {
        id: 'serviceInfo',
        header: 'Tên dịch vụ / Hạng mục',
        cell: ({ row }) => (
          <div>
            <div className="font-semibold text-foreground text-sm">{row.original.serviceName}</div>
            {row.original.description && (
              <div className="text-xs text-muted-foreground mt-0.5 max-w-sm line-clamp-1">
                {row.original.description}
              </div>
            )}
          </div>
        ),
      },
      {
        id: 'serviceType',
        header: 'Phân loại',
        cell: ({ row }) => {
          const isPackage = row.original.serviceType === 'PACKAGE';
          return (
            <span
              className={`badge text-xs px-2 py-0.5 rounded font-medium ${
                isPackage
                  ? 'bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300'
                  : 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300'
              }`}
            >
              {isPackage ? 'Gói bảo dưỡng' : 'Dịch vụ lẻ'}
            </span>
          );
        },
      },
      {
        id: 'unitPrice',
        header: 'Đơn giá tiền công',
        cell: ({ row }) => (
          <div>
            <span className="font-bold text-sm text-primary">
              {money(row.original.unitPrice)}
            </span>
            <span className="text-xs text-muted-foreground ml-1">/ {row.original.unit}</span>
          </div>
        ),
      },
      {
        id: 'standardMinutes',
        header: 'Định mức giờ công',
        cell: ({ row }) => {
          const mins = row.original.standardMinutes;
          const hrs = (mins / 60).toFixed(1);
          return (
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <Clock size={13} />
              <span>{mins} phút (~{hrs}h)</span>
            </div>
          );
        },
      },
      {
        id: 'bayAndWarranty',
        header: 'Khoang & Bảo hành',
        cell: ({ row }) => {
          const s = row.original;
          return (
            <div className="text-xs space-y-0.5">
              {s.requiredBayType ? (
                <div className="flex items-center gap-1 text-muted-foreground">
                  <Box size={12} />
                  <span>{s.requiredBayType}</span>
                </div>
              ) : (
                <span className="text-muted-foreground">Khoang tự do</span>
              )}

              {(s.warrantyDays > 0 || s.warrantyKm > 0) && (
                <div className="flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-medium">
                  <ShieldCheck size={12} />
                  <span>
                    BH: {s.warrantyDays > 0 ? `${s.warrantyDays} ngày` : ''}
                    {s.warrantyDays > 0 && s.warrantyKm > 0 ? ' / ' : ''}
                    {s.warrantyKm > 0 ? `${s.warrantyKm.toLocaleString('vi-VN')} km` : ''}
                  </span>
                </div>
              )}

              {s.serviceType === 'PACKAGE' && (s.intervalKm || s.intervalMonths) && (
                <div className="flex items-center gap-1 text-indigo-600 dark:text-indigo-400">
                  <RotateCw size={12} />
                  <span>
                    {s.intervalKm ? `${s.intervalKm.toLocaleString('vi-VN')} km` : ''}
                    {s.intervalKm && s.intervalMonths ? ' | ' : ''}
                    {s.intervalMonths ? `${s.intervalMonths} th` : ''}
                  </span>
                </div>
              )}
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
          const service = row.original;
          if (!isAdmin) return null;

          return (
            <div className="row-actions flex items-center justify-end gap-1">
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0"
                title="Chỉnh sửa dịch vụ"
                onClick={() => handleOpenEdit(service.id)}
              >
                <SquarePen size={14} />
              </Button>

              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                title="Ngừng áp dụng"
                onClick={() => setDeactivatingService(service)}
              >
                <Trash2 size={14} />
              </Button>
            </div>
          );
        },
      },
    ],
    [isAdmin],
  );

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="MODULE M04"
        title="Danh mục Dịch vụ & Tiền công"
        description="Quản lý bảng giá tiền công kỹ thuật, định mức thời gian tiêu chuẩn và cấu hình các gói bảo dưỡng định kỳ."
        actions={
          isAdmin ? (
            <Button ref={createTrigger} variant="primary" onClick={handleOpenCreate}>
              <Plus size={16} />
              Thêm dịch vụ mới
            </Button>
          ) : undefined
        }
      />

      <ServiceFilters
        search={search}
        serviceType={serviceType}
        isActive={isActive}
        onSearchChange={setSearch}
        onTypeChange={setServiceType}
        onStatusChange={setIsActive}
        onReset={() => {
          setSearch('');
          setServiceType('ALL');
          setIsActive('ALL');
        }}
      />

      {isLoading ? (
        <Loading label="Đang tải danh mục dịch vụ…" />
      ) : isError ? (
        <QueryError
          error={error}
          retry={() => refetch()}
        />
      ) : compact ? (
        <ServiceCards
          services={services}
          onEdit={handleOpenEdit}
          onDeactivate={setDeactivatingService}
        />
      ) : (
        <DataTable columns={columns} data={services} emptyMessage="Không tìm thấy dịch vụ nào." />
      )}

      {editorOpen && (
        <ServiceEditor
          serviceId={editingId}
          open={editorOpen}
          onClose={() => {
            setEditorOpen(false);
            setEditingId(null);
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(deactivatingService)}
        title="Ngừng áp dụng dịch vụ"
        description={`Bạn có chắc muốn chuyển dịch vụ "${deactivatingService?.serviceName}" sang trạng thái ngừng áp dụng? Dịch vụ này sẽ không xuất hiện khi báo giá mới.`}
        confirmLabel="Ngừng áp dụng"
        cancelLabel="Hủy"
        destructive
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setDeactivatingService(null)}
      />
    </div>
  );
}

export default ServicesPage;
