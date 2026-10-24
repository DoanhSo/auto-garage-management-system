import { useMemo, useRef, useState } from 'react';
import { Plus, SquarePen, UserX, Phone, Mail, Building2 } from 'lucide-react';
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
import { initials } from '../../../shared/utils/format';
import { errorMessage } from '../../../shared/api/errors';
import type { Customer } from '../../../shared/types/contracts';
import { useCustomersQuery, useDeleteCustomerMutation } from '../hooks/useCustomers';
import { CustomerEditor } from '../components/CustomerEditor';
import { CustomerFilters } from '../components/CustomerFilters';
import { CustomerCards } from '../components/CustomerCards';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';

export function CustomersPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();

  const [search, setSearch] = useState('');
  const [customerType, setCustomerType] = useState('ALL');
  const [isActive, setIsActive] = useState('ALL');

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [deactivatingCustomer, setDeactivatingCustomer] = useState<Customer | null>(null);

  const lastTrigger = useRef<HTMLElement | null>(null);
  const createTrigger = useRef<HTMLButtonElement>(null);

  const debouncedSearch = useDebouncedValue(search);

  const queryParams = useMemo(() => {
    return {
      search: debouncedSearch.trim() || undefined,
      isActive: isActive === 'true' ? true : isActive === 'false' ? false : undefined,
    };
  }, [debouncedSearch, isActive]);

  const { data: allCustomers, isLoading, isError, error, refetch } = useCustomersQuery(queryParams);
  const deleteMutation = useDeleteCustomerMutation();

  // Filter client-side by customerType if selected
  const filteredCustomers = useMemo(() => {
    if (!allCustomers) return [];
    if (customerType === 'ALL') return allCustomers;
    return allCustomers.filter((c) => c.customerType === customerType);
  }, [allCustomers, customerType]);

  const handleOpenCreate = () => {
    lastTrigger.current = createTrigger.current;
    setEditingId(null);
    setEditorOpen(true);
  };

  const handleOpenEdit = (id: string, trigger: HTMLElement) => {
    lastTrigger.current = trigger;
    setEditingId(id);
    setEditorOpen(true);
  };

  const handleCloseEditor = () => {
    setEditorOpen(false);
    setEditingId(null);
  };

  const handleConfirmDeactivate = async () => {
    if (!deactivatingCustomer) return;
    try {
      await deleteMutation.mutateAsync(deactivatingCustomer.id);
      notify(`Đã ngừng hoạt động khách hàng "${deactivatingCustomer.fullName}"`);
      setDeactivatingCustomer(null);
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const formatVnd = (value: string | number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(
      Number(value),
    );
  };

  const columns = useMemo<ColumnDef<Customer>[]>(
    () => [
      {
        id: 'customer',
        header: 'Khách hàng',
        cell: ({ row }) => (
          <div className="account-cell">
            <span className="avatar">{initials(row.original.fullName)}</span>
            <div>
              <strong>{row.original.fullName}</strong>
              <span className="code-tag">{row.original.customerCode}</span>
            </div>
          </div>
        ),
      },
      {
        id: 'type',
        header: 'Phân loại',
        cell: ({ row }) => {
          const isBusiness = row.original.customerType === 'BUSINESS';
          return (
            <div>
              <span className={`badge ${isBusiness ? 'badge-primary' : 'badge-neutral'}`}>
                {isBusiness ? 'Doanh nghiệp' : 'Cá nhân'}
              </span>
              {row.original.companyName && (
                <div className="company-subtext">
                  <Building2 size={13} aria-hidden />
                  <span>{row.original.companyName}</span>
                </div>
              )}
            </div>
          );
        },
      },
      {
        id: 'contact',
        header: 'Liên hệ',
        cell: ({ row }) => (
          <div className="contact-cell">
            <div className="contact-item">
              <Phone size={13} aria-hidden />
              <span>{row.original.phone}</span>
            </div>
            {row.original.email && (
              <div className="contact-item subtext">
                <Mail size={13} aria-hidden />
                <span>{row.original.email}</span>
              </div>
            )}
          </div>
        ),
      },
      {
        id: 'credit',
        header: 'Hạn mức nợ',
        cell: ({ row }) => (
          <div className="credit-cell">
            <strong className="credit-amount">{formatVnd(row.original.creditLimit)}</strong>
            <span className="credit-days">Hạn: {row.original.paymentTermDays} ngày</span>
          </div>
        ),
      },
      {
        accessorKey: 'isActive',
        header: 'Trạng thái',
        cell: ({ row }) => <StatusBadge active={row.original.isActive} />,
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button
              variant="ghost"
              aria-label={`Sửa thông tin khách hàng ${row.original.fullName}`}
              onClick={(e) => handleOpenEdit(row.original.id, e.currentTarget)}
            >
              <SquarePen size={16} />
              Sửa
            </Button>
            {row.original.isActive && (
              <Button
                variant="ghost"
                className="delete-action"
                aria-label={`Ngừng hoạt động khách hàng ${row.original.fullName}`}
                onClick={() => setDeactivatingCustomer(row.original)}
              >
                <UserX size={16} />
                Ngừng HĐ
              </Button>
            )}
          </div>
        ),
      },
    ],
    [],
  );

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="TIẾP NHẬN & DỊCH VỤ · M02"
        title="Quản lý khách hàng"
        description="Tra cứu hồ sơ liên hệ, phân loại khách hàng cá nhân / doanh nghiệp và chính sách hạn mức nợ dịch vụ."
        actions={
          <Button ref={createTrigger} variant="primary" onClick={handleOpenCreate}>
            <Plus size={16} aria-hidden />
            Thêm khách hàng
          </Button>
        }
      />

      <CustomerFilters
        search={search}
        customerType={customerType}
        isActive={isActive}
        onSearchChange={setSearch}
        onTypeChange={setCustomerType}
        onStatusChange={setIsActive}
        onReset={() => {
          setSearch('');
          setCustomerType('ALL');
          setIsActive('ALL');
        }}
      />

      {isLoading && <Loading label="Đang tải danh sách khách hàng…" />}

      {isError && <QueryError error={error} retry={() => refetch()} />}

      {!isLoading &&
        !isError &&
        (compact ? (
          <CustomerCards
            customers={filteredCustomers}
            onEdit={handleOpenEdit}
            onDeactivate={setDeactivatingCustomer}
          />
        ) : (
          <DataTable<Customer>
            data={filteredCustomers}
            columns={columns}
            caption="Danh sách khách hàng"
          />
        ))}

      <CustomerEditor
        customerId={editingId}
        open={editorOpen}
        onClose={handleCloseEditor}
        onRestoreFocus={() => lastTrigger.current?.focus()}
      />

      <ConfirmDialog
        open={Boolean(deactivatingCustomer)}
        title="Ngừng hoạt động khách hàng?"
        description={`Bạn có chắc chắn muốn chuyển trạng thái khách hàng "${deactivatingCustomer?.fullName}" sang ngừng hoạt động? Khách hàng sẽ không thể tạo lịch hẹn hay tiếp nhận mới.`}
        confirmLabel="Ngừng hoạt động"
        danger
        pending={deleteMutation.isPending}
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setDeactivatingCustomer(null)}
      />
    </div>
  );
}
