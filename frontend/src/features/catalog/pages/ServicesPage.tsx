import { useMemo, useRef, useState } from 'react';
import { Plus, SquarePen, Trash2, Clock, ShieldCheck, RotateCw, Box, Layers } from 'lucide-react';
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
      notify(`Đã ngừng áp dụng dịch vụ "${deactivatingService.serviceName}"`);
      setDeactivatingService(null);
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const columns = useMemo<ColumnDef<Service>[]>(
    () => [
      {
        id: 'serviceCode',
        header: 'Mã DV',
        cell: ({ row }) => (
          <span className="code-tag">{row.original.serviceCode}</span>
        ),
      },
      {
        id: 'serviceInfo',
        header: 'Tên dịch vụ / Hạng mục',
        cell: ({ row }) => (
          <div>
            <strong>{row.original.serviceName}</strong>
            {row.original.description && (
              <div className="muted nowrap" style={{ marginTop: 2 }}>
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
              className={`badge ${isPackage ? 'badge-primary' : 'badge-neutral'}`}
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
            <strong className="credit-amount">{money(row.original.unitPrice)}</strong>
            <span className="muted"> / {row.original.unit}</span>
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
            <div className="flex items-center gap-1 muted">
              <Clock size={13} />
              <span>
                {mins} phút (~{hrs}h)
              </span>
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
            <div className="muted nowrap" style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {s.requiredBayType ? (
                <div className="flex items-center gap-1">
                  <Box size={12} />
                  <span>{s.requiredBayType}</span>
                </div>
              ) : (
                <span>Khoang tự do</span>
              )}

              {(s.warrantyDays > 0 || s.warrantyKm > 0) && (
                <div className="flex items-center gap-1">
                  <ShieldCheck size={12} />
                  <span className="badge badge-success">
                    BH: {s.warrantyDays > 0 ? `${s.warrantyDays} ngày` : ''}
                    {s.warrantyDays > 0 && s.warrantyKm > 0 ? ' / ' : ''}
                    {s.warrantyKm > 0 ? `${s.warrantyKm.toLocaleString('vi-VN')} km` : ''}
                  </span>
                </div>
              )}

              {s.serviceType === 'PACKAGE' && (s.intervalKm || s.intervalMonths) && (
                <div className="flex items-center gap-1">
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
        header: 'Thao tác',
        cell: ({ row }) => {
          const service = row.original;
          if (!isAdmin) return null;

          return (
            <div className="row-actions">
              <Button
                variant="ghost"
                aria-label={`Sửa dịch vụ ${service.serviceName}`}
                onClick={() => handleOpenEdit(service.id)}
              >
                <SquarePen size={16} />
                Sửa
              </Button>

              {service.isActive && (
                <Button
                  variant="ghost"
                  className="delete-action"
                  aria-label={`Ngừng áp dụng dịch vụ ${service.serviceName}`}
                  onClick={() => setDeactivatingService(service)}
                >
                  <Trash2 size={16} />
                  Ngừng áp dụng
                </Button>
              )}
            </div>
          );
        },
      },
    ],
    [isAdmin],
  );

  return (
    <>
      <PageHeader
        eyebrow="TIẾP NHẬN & DỊCH VỤ · M04"
        title="Bảng giá & Dịch vụ"
        description="Quản lý bảng giá tiền công kỹ thuật, định mức thời gian tiêu chuẩn và cấu hình các gói bảo dưỡng định kỳ."
        actions={
          isAdmin ? (
            <Button ref={createTrigger} variant="primary" onClick={handleOpenCreate}>
              <Plus size={16} aria-hidden />
              Thêm dịch vụ mới
            </Button>
          ) : undefined
        }
      />

      <div className="info-strip">
        <Layers size={18} />
        <span>
          Bảng giá tiền công tiêu chuẩn, thời gian định mức và chu kỳ bảo dưỡng định kỳ (Quản trị viên quản lý, Nhân viên tra cứu).
        </span>
      </div>

      <section className="panel table-panel">
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
          <QueryError error={error} retry={() => refetch()} />
        ) : compact ? (
          <ServiceCards
            canManage={isAdmin}
            services={services}
            onEdit={handleOpenEdit}
            onDeactivate={setDeactivatingService}
          />
        ) : (
          <DataTable<Service>
            columns={columns}
            data={services}
            caption="Danh sách dịch vụ & tiền công"
          />
        )}
      </section>

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
        danger
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setDeactivatingService(null)}
      />
    </>
  );
}

export default ServicesPage;
