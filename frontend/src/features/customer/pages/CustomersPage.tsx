import { useCallback, useMemo, useRef, useState } from 'react';
import {
  Plus,
  SquarePen,
  UserX,
  Phone,
  Mail,
  Building2,
  Contact,
  RotateCcw,
  KeyRound,
  Link2,
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
import { initials } from '../../../shared/utils/format';
import { AccountStatusBadge } from '../../../shared/components/AccountStatusBadge';
import { errorMessage } from '../../../shared/api/errors';
import type { Customer } from '../../../shared/types/contracts';
import {
  useCustomersQuery,
  useDeleteCustomerMutation,
  useActivateCustomerMutation,
} from '../hooks/useCustomers';
import { CustomerEditor } from '../components/CustomerEditor';
import { CustomerFilters } from '../components/CustomerFilters';
import { CustomerCards } from '../components/CustomerCards';
import { CustomerAccountModal } from '../components/CustomerAccountModal';
import { LinkAccountModal } from '../components/LinkAccountModal';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';

export function CustomersPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const notify = useToast();

  const [search, setSearch] = useState('');
  const [customerType, setCustomerType] = useState('ALL');
  const [isActive, setIsActive] = useState('ALL');
  const [hasAccount, setHasAccount] = useState('ALL');
  const [accountFor, setAccountFor] = useState<Customer | null>(null);
  const [linkFor, setLinkFor] = useState<Customer | null>(null);

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
      hasAccount: hasAccount === 'true' ? true : hasAccount === 'false' ? false : undefined,
    };
  }, [debouncedSearch, isActive, hasAccount]);

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

  const activateMutation = useActivateCustomerMutation();
  const activateCustomer = activateMutation.mutateAsync;
  const activatingId = activateMutation.isPending ? activateMutation.variables : null;
  // Reopening is reversible, so it runs without a confirmation step.
  const handleActivate = useCallback(
    async (customer: Customer) => {
      try {
        await activateCustomer(customer.id);
        notify(`Đã mở lại khách hàng "${customer.fullName}"`);
      } catch (err: unknown) {
        notify(errorMessage(err));
      }
    },
    [activateCustomer, notify],
  );

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
        id: 'account',
        header: 'Tài khoản',
        cell: ({ row }) => (
          <div className="account-status">
            <AccountStatusBadge account={row.original.account} />
            {row.original.account?.status === 'NONE' && (
              <span className="field-hint">
                Tạo tài khoản cho khách, hoặc Nối tài khoản nếu khách đã tự đăng ký
              </span>
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
            {row.original.isActive && row.original.account?.status === 'NONE' && (
              <>
                <Button
                  variant="ghost"
                  aria-label={`Tạo tài khoản cho ${row.original.fullName}`}
                  onClick={() => setAccountFor(row.original)}
                >
                  <KeyRound size={16} />
                  Tạo tài khoản
                </Button>
                <Button
                  variant="ghost"
                  aria-label={`Nối tài khoản cho ${row.original.fullName}`}
                  onClick={() => setLinkFor(row.original)}
                >
                  <Link2 size={16} />
                  Nối tài khoản
                </Button>
              </>
            )}
            {!row.original.isActive && (
              <Button
                variant="ghost"
                aria-label={`Mở lại khách hàng ${row.original.fullName}`}
                pending={activatingId === row.original.id}
                onClick={() => handleActivate(row.original)}
              >
                <RotateCcw size={16} />
                Mở lại
              </Button>
            )}
          </div>
        ),
      },
    ],
    [activatingId, handleActivate],
  );

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ KHÁCH HÀNG"
        title="Quản lý khách hàng"
        description="Tra cứu hồ sơ liên hệ, phân loại khách hàng cá nhân / doanh nghiệp và chính sách hạn mức nợ dịch vụ."
        actions={
          <Button ref={createTrigger} variant="primary" onClick={handleOpenCreate}>
            <Plus size={16} aria-hidden />
            Thêm khách hàng
          </Button>
        }
      />

      <div className="info-strip">
        <Contact size={18} />
        <span>
          Quản lý thông tin liên hệ, phân loại cá nhân hoặc doanh nghiệp và theo dõi hạn mức công nợ
          khách hàng.
        </span>
      </div>

      <section className="panel table-panel">
        <CustomerFilters
          search={search}
          customerType={customerType}
          isActive={isActive}
          hasAccount={hasAccount}
          onSearchChange={setSearch}
          onTypeChange={setCustomerType}
          onStatusChange={setIsActive}
          onAccountChange={setHasAccount}
          onReset={() => {
            setSearch('');
            setCustomerType('ALL');
            setIsActive('ALL');
            setHasAccount('ALL');
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
              onActivate={handleActivate}
              onCreateAccount={setAccountFor}
              onLinkAccount={setLinkFor}
              activatingId={activatingId}
            />
          ) : (
            <DataTable<Customer>
              data={filteredCustomers}
              columns={columns}
              caption="Danh sách khách hàng"
            />
          ))}
      </section>

      <CustomerAccountModal customer={accountFor} onClose={() => setAccountFor(null)} />
      <LinkAccountModal customer={linkFor} onClose={() => setLinkFor(null)} />

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
    </>
  );
}
