import { useMemo, useState } from 'react';
import {
  Plus,
  SquarePen,
  Trash2,
  CalendarRange,
  Wrench,
  RefreshCw,
  XCircle,
  Warehouse,
  Users,
  CalendarClock,
  History,
  Hammer,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import { useAuth } from '../../../app/providers/AuthProvider';
import { errorMessage } from '../../../shared/api/errors';
import {
  Badge,
  Button,
  PageHeader,
  Loading,
  QueryError,
  ConfirmDialog,
} from '../../../shared/components/ui';
import { DataTable, Pagination } from '../../../shared/components/DataTable';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import { useUiPolicy } from '../../../shared/config/policy';
import { useUrlTab } from '../../../shared/hooks/useUrlTab';
import { formatDate, money } from '../../../shared/utils/format';
import {
  useServiceBaysQuery,
  useDeleteServiceBayMutation,
  useShiftsQuery,
  useCancelShiftMutation,
  useSchedulingEmployeesQuery,
  useActiveReservationsQuery,
  useUpdateEmployeeMutation,
} from '../hooks/useScheduling';
import { ServiceBayEditor } from '../components/ServiceBayEditor';
import { ShiftEditor } from '../components/ShiftEditor';
import { BayMaintenanceModal } from '../components/BayMaintenanceModal';
import { BayReservationsModal } from '../components/BayReservationsModal';
import { EmployeeEditor } from '../components/EmployeeEditor';
import { LiveDispatchBoard } from '../components/LiveDispatchBoard';
import { ShiftRegistrationModal } from '../components/ShiftRegistrationModal';
import type { BayReservation, Employee, ServiceBay, Shift } from '../types/scheduling';
import { bayTypeLabels, parseSkills, SKILL_LABELS, shiftStatusLabels } from '../types/scheduling';
import type { UiPolicy } from '../../../shared/types/contracts';

const TABS = ['dispatch', 'bays', 'shifts', 'employees'] as const;
type Tab = (typeof TABS)[number];
type BayFilter = 'ALL' | 'FREE' | 'BUSY' | 'INACTIVE';

const hhmm = (iso: string) =>
  new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

function Occupancy({ reservation }: { reservation?: BayReservation }) {
  if (!reservation) return <Badge variant="success">Sẵn sàng nhận xe</Badge>;
  const range = `${hhmm(reservation.startsAt)}–${hhmm(reservation.endsAt)}`;
  if (reservation.reservationType === 'REPAIR')
    return (
      <Badge variant="primary">
        Đang sửa xe · #{reservation.repairOrderId} · {range}
      </Badge>
    );
  if (reservation.reservationType === 'BOOKING')
    return (
      <Badge variant="warning">
        Giữ chỗ hẹn · #{reservation.bookingId} · {range}
      </Badge>
    );
  return <Badge variant="danger">Đang bảo trì · {range}</Badge>;
}

function liveShiftState(shift: Shift, now: number) {
  if (shift.status === 'CANCELLED') return { label: 'Đã hủy', variant: 'danger' as const };
  if (now < new Date(shift.startsAt).getTime())
    return { label: 'Sắp tới (đã xếp lịch)', variant: 'primary' as const };
  if (now <= new Date(shift.endsAt).getTime())
    return { label: 'Đang trong ca trực', variant: 'success' as const };
  return { label: 'Đã hoàn thành ca', variant: 'secondary' as const };
}

function ReadyScheduling({ policy }: { policy: UiPolicy }) {
  const notify = useToast();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const isTechnician = user?.role === 'TECHNICIAN';
  const [activeTab, setActiveTab] = useUrlTab<Tab>('tab', TABS);
  // Paging belongs to one tab; switching tabs from the sidebar tree starts again at page 1.
  const [paging, setPaging] = useState<{ tab: Tab; page: number }>({ tab: activeTab, page: 0 });
  const page = paging.tab === activeTab ? paging.page : 0;
  const setPage = (next: number) => setPaging({ tab: activeTab, page: next });
  const [size, setSize] = useState(policy.paginationDefaultSize);

  const [bayFilter, setBayFilter] = useState<BayFilter>('ALL');
  const [bayKeyword, setBayKeyword] = useState('');
  const [shiftEmployee, setShiftEmployee] = useState('');
  const [shiftDay, setShiftDay] = useState('');
  const [shiftStatus, setShiftStatus] = useState<'ALL' | 'ACTIVE' | 'CANCELLED'>('ALL');

  const [bayModalOpen, setBayModalOpen] = useState(false);
  const [editingBay, setEditingBay] = useState<ServiceBay | null>(null);
  const [deletingBayId, setDeletingBayId] = useState<number | string | null>(null);
  const [maintenanceBay, setMaintenanceBay] = useState<ServiceBay | null>(null);
  const [reservationsBay, setReservationsBay] = useState<ServiceBay | null>(null);

  const [shiftModalOpen, setShiftModalOpen] = useState(false);
  const [registerModalOpen, setRegisterModalOpen] = useState(false);
  const [shiftPresetEmployee, setShiftPresetEmployee] = useState<string | number | null>(null);
  const [shiftPresetBay, setShiftPresetBay] = useState<string | number | null>(null);
  const [cancelingShiftId, setCancelingShiftId] = useState<number | string | null>(null);

  const [employeeModalOpen, setEmployeeModalOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null);

  const baysQuery = useServiceBaysQuery();
  const shiftsQuery = useShiftsQuery({ size: 100 });
  const reservationsQuery = useActiveReservationsQuery();
  const employeesQuery = useSchedulingEmployeesQuery();

  const deleteBayMutation = useDeleteServiceBayMutation();
  const cancelShiftMutation = useCancelShiftMutation();
  const updateEmployee = useUpdateEmployeeMutation();

  const employees = useMemo(() => employeesQuery.data ?? [], [employeesQuery.data]);
  const employeeMap = useMemo(
    () => new Map(employees.map((e) => [String(e.id), e] as const)),
    [employees],
  );
  const userMap = useMemo(
    () =>
      new Map(
        employees.map(
          (e) =>
            [
              String(e.userId),
              { fullName: e.fullName ?? undefined, email: e.email, username: e.username },
            ] as const,
        ),
      ),
    [employees],
  );
  const takenUserIds = useMemo(() => new Set(employees.map((e) => String(e.userId))), [employees]);

  const allBays = useMemo(() => baysQuery.data?.content ?? [], [baysQuery.data?.content]);
  const allShifts = useMemo(() => shiftsQuery.data?.content ?? [], [shiftsQuery.data?.content]);
  const reservations = useMemo(() => reservationsQuery.data ?? [], [reservationsQuery.data]);

  const now = Date.now();
  const currentByBay = useMemo(() => {
    const map = new Map<string, BayReservation>();
    const t = Date.now();
    reservations
      .filter((r) => new Date(r.startsAt).getTime() <= t && t <= new Date(r.endsAt).getTime())
      .forEach((r) => map.set(String(r.bayId), r));
    return map;
  }, [reservations]);

  const activeBayCount = allBays.filter((b) => b.isActive).length;
  const busyBayCount = allBays.filter((b) => currentByBay.has(String(b.id))).length;
  const heldCount = reservations.filter((r) => r.status === 'HELD').length;
  const maintenanceCount = reservations.filter((r) => r.reservationType === 'MAINTENANCE').length;
  const activeEmployees = employees.filter((e) => e.isActive);
  const liveShiftCount = allShifts.filter(
    (s) => s.status !== 'CANCELLED' && new Date(s.endsAt).getTime() >= now,
  ).length;
  const shiftCountByEmployee = useMemo(() => {
    const map = new Map<string, number>();
    allShifts
      .filter((s) => s.status !== 'CANCELLED' && new Date(s.endsAt).getTime() >= Date.now())
      .forEach((s) => map.set(String(s.employeeId), (map.get(String(s.employeeId)) ?? 0) + 1));
    return map;
  }, [allShifts]);

  const employeeName = (id: string | number) => {
    const emp = employeeMap.get(String(id));
    return emp ? (userMap.get(String(emp.userId))?.fullName ?? emp.employeeCode) : `NV #${id}`;
  };

  const bays = useMemo(() => {
    const term = bayKeyword.trim().toLowerCase();
    return allBays.filter((b) => {
      if (term && !`${b.bayCode} ${b.bayName}`.toLowerCase().includes(term)) return false;
      const busy = currentByBay.has(String(b.id));
      if (bayFilter === 'FREE') return b.isActive && !busy;
      if (bayFilter === 'BUSY') return busy;
      if (bayFilter === 'INACTIVE') return !b.isActive;
      return true;
    });
  }, [allBays, bayKeyword, bayFilter, currentByBay]);

  const shifts = useMemo(
    () =>
      allShifts.filter((s) => {
        if (shiftEmployee && String(s.employeeId) !== shiftEmployee) return false;
        if (shiftStatus !== 'ALL' && (s.status === 'CANCELLED') !== (shiftStatus === 'CANCELLED'))
          return false;
        if (shiftDay && new Date(s.startsAt).toLocaleDateString('en-CA') !== shiftDay) return false;
        return true;
      }),
    [allShifts, shiftEmployee, shiftStatus, shiftDay],
  );

  const pageRows = <T,>(rows: T[]) => rows.slice(page * size, page * size + size);
  const pageInfo = (total: number) => ({
    totalElements: String(total),
    totalPages: Math.max(1, Math.ceil(total / size)),
  });
  const onPageChange = (nextPage: number, nextSize: number) => {
    setPage(nextPage);
    setSize(nextSize);
  };

  const bayColumns = useMemo<ColumnDef<ServiceBay>[]>(
    () => [
      {
        accessorKey: 'bayCode',
        header: 'Mã & tên khoang',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>{row.original.bayCode}</strong>
              <span>{row.original.bayName}</span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'bayType',
        header: 'Loại khoang',
        cell: ({ row }) => (
          <Badge variant="outline">
            {bayTypeLabels[row.original.bayType] || row.original.bayType}
          </Badge>
        ),
      },
      {
        id: 'occupancy',
        header: 'Tình trạng hiện tại',
        cell: ({ row }) =>
          row.original.isActive ? (
            <Occupancy reservation={currentByBay.get(String(row.original.id))} />
          ) : (
            <Badge variant="secondary">Tạm ngưng</Badge>
          ),
      },
      {
        accessorKey: 'description',
        header: 'Trang bị & mô tả',
        cell: ({ row }) => <span className="muted clamp-2">{row.original.description || '—'}</span>,
      },
      {
        accessorKey: 'isActive',
        header: 'Cấu hình',
        cell: ({ row }) => (
          <Badge variant={row.original.isActive ? 'success' : 'danger'}>
            {row.original.isActive ? 'Đang hoạt động' : 'Tạm dừng'}
          </Badge>
        ),
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button variant="ghost" onClick={() => setReservationsBay(row.original)}>
              <History size={14} /> Lịch đặt
            </Button>
            <Button variant="ghost" onClick={() => setMaintenanceBay(row.original)}>
              <Hammer size={14} /> Bảo trì
            </Button>
            <Button
              variant="ghost"
              onClick={() => {
                setEditingBay(row.original);
                setBayModalOpen(true);
              }}
            >
              <SquarePen size={14} /> Sửa
            </Button>
            <Button
              variant="ghost"
              className="delete-action"
              onClick={() => setDeletingBayId(row.original.id)}
            >
              <Trash2 size={14} /> Xóa
            </Button>
          </div>
        ),
      },
    ],
    [currentByBay],
  );

  const skillBadges = (emp?: Employee) => {
    const skills = parseSkills(emp?.skillsJson);
    return (
      <div className="row-actions">
        {skills.map((s) => (
          <Badge key={s} variant="outline">
            {SKILL_LABELS[s] ?? s}
          </Badge>
        ))}
        {emp?.canQc && <Badge variant="primary">KTV kiểm định (QC)</Badge>}
        {skills.length === 0 && !emp?.canQc && <span className="muted">—</span>}
      </div>
    );
  };

  const shiftColumns = useMemo<ColumnDef<Shift>[]>(
    () => [
      {
        accessorKey: 'employeeId',
        header: 'Kỹ thuật viên',
        cell: ({ row }) => {
          const emp = employeeMap.get(String(row.original.employeeId));
          const user = emp ? userMap.get(String(emp.userId)) : undefined;
          return (
            <div className="account-cell">
              <div>
                <strong>{employeeName(row.original.employeeId)}</strong>
                <span>
                  {emp?.employeeCode}
                  {user?.username && ` · ${user.username}`}
                </span>
              </div>
            </div>
          );
        },
      },
      {
        id: 'skills',
        header: 'Chuyên môn & QC',
        cell: ({ row }) => skillBadges(employeeMap.get(String(row.original.employeeId))),
      },
      {
        id: 'window',
        header: 'Ngày & khung giờ',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>
                {new Date(row.original.startsAt).toLocaleDateString('vi-VN', {
                  weekday: 'short',
                  day: '2-digit',
                  month: '2-digit',
                  year: 'numeric',
                })}
              </strong>
              <span>
                {hhmm(row.original.startsAt)} → {hhmm(row.original.endsAt)}
              </span>
            </div>
          </div>
        ),
      },
      {
        id: 'duration',
        header: 'Thời lượng & giá công',
        cell: ({ row }) => {
          const hours =
            (new Date(row.original.endsAt).getTime() - new Date(row.original.startsAt).getTime()) /
            3_600_000;
          const emp = employeeMap.get(String(row.original.employeeId));
          return (
            <div className="account-cell">
              <div>
                <strong>{hours.toFixed(1)} giờ</strong>
                <span>{emp?.hourlyCost != null ? `${money(emp.hourlyCost)}/giờ` : ''}</span>
              </div>
            </div>
          );
        },
      },
      {
        accessorKey: 'status',
        header: 'Tình trạng ca',
        cell: ({ row }) => {
          const s = liveShiftState(row.original, Date.now());
          return (
            <Badge variant={s.variant}>{s.label || shiftStatusLabels[row.original.status]}</Badge>
          );
        },
      },
      {
        accessorKey: 'notes',
        header: 'Ghi chú',
        cell: ({ row }) => <span className="muted clamp-2">{row.original.notes || '—'}</span>,
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) =>
          row.original.status !== 'CANCELLED' &&
          new Date(row.original.endsAt).getTime() >= Date.now() && (
            <div className="row-actions">
              <Button
                variant="ghost"
                className="delete-action"
                onClick={() => setCancelingShiftId(row.original.id)}
              >
                <XCircle size={14} /> Hủy ca
              </Button>
            </div>
          ),
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [employeeMap, userMap],
  );

  const employeeColumns = useMemo<ColumnDef<Employee>[]>(
    () => [
      {
        accessorKey: 'employeeCode',
        header: 'Mã KTV & họ tên',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>{userMap.get(String(row.original.userId))?.fullName ?? '—'}</strong>
              <span>{row.original.employeeCode}</span>
            </div>
          </div>
        ),
      },
      {
        id: 'contact',
        header: 'Liên hệ',
        cell: ({ row }) => {
          const u = userMap.get(String(row.original.userId));
          return <span className="muted">{u?.email ?? u?.username ?? '—'}</span>;
        },
      },
      { id: 'skills', header: 'Chuyên môn & QC', cell: ({ row }) => skillBadges(row.original) },
      {
        id: 'joinedAt',
        header: 'Ngày vào làm',
        cell: ({ row }) => (
          <span>{row.original.joinedAt ? formatDate(row.original.joinedAt) : '—'}</span>
        ),
      },
      {
        id: 'bank',
        header: 'Ngân hàng & STK',
        cell: ({ row }) =>
          row.original.bankAccountNumber ? (
            <div className="account-cell">
              <div>
                <strong>{row.original.bankName ?? '—'}</strong>
                <span>{row.original.bankAccountNumber}</span>
              </div>
            </div>
          ) : (
            <span className="muted">Chưa cập nhật STK</span>
          ),
      },
      {
        accessorKey: 'hourlyCost',
        header: 'Giá giờ công & hệ số',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>
                {row.original.hourlyCost != null ? money(row.original.hourlyCost) : '—'}
              </strong>
              <span>
                {row.original.salaryCoefficient != null
                  ? `Hệ số ${Number(row.original.salaryCoefficient).toFixed(2)}x`
                  : '—'}
              </span>
            </div>
          </div>
        ),
      },
      {
        id: 'shifts',
        header: 'Ca hiệu lực',
        cell: ({ row }) => <span>{shiftCountByEmployee.get(String(row.original.id)) ?? 0} ca</span>,
      },
      {
        accessorKey: 'isActive',
        header: 'Trạng thái',
        cell: ({ row }) => (
          <Badge variant={row.original.isActive ? 'success' : 'secondary'}>
            {row.original.isActive ? 'Đang hoạt động' : 'Ngưng hoạt động'}
          </Badge>
        ),
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button
              variant="ghost"
              disabled={!row.original.isActive}
              onClick={() => {
                setShiftPresetEmployee(row.original.id);
                setShiftPresetBay(null);
                setShiftModalOpen(true);
              }}
            >
              <CalendarClock size={14} /> Xếp ca
            </Button>
            <Button
              variant="ghost"
              onClick={() => {
                setEditingEmployee(row.original);
                setEmployeeModalOpen(true);
              }}
            >
              <SquarePen size={14} /> Sửa
            </Button>
            <Button
              variant="ghost"
              className="delete-action"
              onClick={() => toggleEmployeeActive(row.original)}
            >
              {row.original.isActive ? 'Ngưng' : 'Kích hoạt'}
            </Button>
          </div>
        ),
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [userMap, shiftCountByEmployee],
  );

  async function toggleEmployeeActive(emp: Employee) {
    try {
      await updateEmployee.mutateAsync({ id: emp.id, data: { isActive: !emp.isActive } });
      notify(emp.isActive ? 'Đã ngưng hoạt động kỹ thuật viên' : 'Đã kích hoạt lại kỹ thuật viên');
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  }

  const handleDeleteBay = async () => {
    if (!deletingBayId) return;
    try {
      await deleteBayMutation.mutateAsync(deletingBayId);
      notify('Đã xóa khoang sửa chữa thành công');
      setDeletingBayId(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Không thể xóa khoang sửa chữa');
    }
  };

  const handleCancelShift = async () => {
    if (!cancelingShiftId) return;
    try {
      await cancelShiftMutation.mutateAsync({ id: cancelingShiftId, reason: 'Điều chỉnh lịch' });
      notify('Đã hủy ca làm việc');
      setCancelingShiftId(null);
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Không thể hủy ca làm việc');
    }
  };

  const current =
    activeTab === 'bays' || activeTab === 'dispatch'
      ? baysQuery
      : activeTab === 'shifts'
        ? shiftsQuery
        : employeesQuery;
  const switchTab = (tab: Tab) => {
    setActiveTab(tab);
    setPage(0);
  };

  const primaryAction = isTechnician ? (
    <Button variant="primary" onClick={() => setRegisterModalOpen(true)}>
      <CalendarClock size={16} /> Đăng ký ca làm việc
    </Button>
  ) : !isAdmin ? null : activeTab === 'dispatch' ? null : activeTab === 'bays' ? (
    <Button
      variant="primary"
      onClick={() => {
        setEditingBay(null);
        setBayModalOpen(true);
      }}
    >
      <Plus size={16} /> Thêm khoang
    </Button>
  ) : activeTab === 'shifts' ? (
    <Button
      variant="primary"
      onClick={() => {
        setShiftPresetEmployee(null);
        setShiftPresetBay(null);
        setShiftModalOpen(true);
      }}
    >
      <Plus size={16} /> Xếp ca làm việc
    </Button>
  ) : (
    <Button
      variant="primary"
      onClick={() => {
        setEditingEmployee(null);
        setEmployeeModalOpen(true);
      }}
    >
      <Plus size={16} /> Thêm kỹ thuật viên
    </Button>
  );

  const filtersFor = (tab: Tab) =>
    tab === 'bays' ? (
      <div className="user-filter-grid">
        <div className="user-filter-field">
          <label htmlFor="bay-keyword">Tìm khoang</label>
          <input
            id="bay-keyword"
            className="input"
            placeholder="Mã hoặc tên khoang"
            value={bayKeyword}
            onChange={(e) => {
              setBayKeyword(e.target.value);
              setPage(0);
            }}
          />
        </div>
        <div className="user-filter-field">
          <label htmlFor="bay-state">Tình trạng</label>
          <select
            id="bay-state"
            className="select"
            value={bayFilter}
            onChange={(e) => {
              setBayFilter(e.target.value as BayFilter);
              setPage(0);
            }}
          >
            <option value="ALL">Tất cả</option>
            <option value="FREE">Đang trống</option>
            <option value="BUSY">Đang có xe / giữ chỗ / bảo trì</option>
            <option value="INACTIVE">Tạm ngưng</option>
          </select>
        </div>
      </div>
    ) : tab === 'shifts' ? (
      <div className="user-filter-grid">
        <div className="user-filter-field">
          <label htmlFor="shift-employee">Kỹ thuật viên</label>
          <select
            id="shift-employee"
            className="select"
            value={shiftEmployee}
            onChange={(e) => {
              setShiftEmployee(e.target.value);
              setPage(0);
            }}
          >
            <option value="">Tất cả kỹ thuật viên</option>
            {employees.map((e) => (
              <option key={e.id} value={e.id}>
                {employeeName(e.id)} · {e.employeeCode}
              </option>
            ))}
          </select>
        </div>
        <div className="user-filter-field">
          <label htmlFor="shift-day">Ngày làm việc</label>
          <input
            id="shift-day"
            type="date"
            className="input"
            value={shiftDay}
            onChange={(e) => {
              setShiftDay(e.target.value);
              setPage(0);
            }}
          />
        </div>
        <div className="user-filter-field">
          <label htmlFor="shift-status">Trạng thái ca</label>
          <select
            id="shift-status"
            className="select"
            value={shiftStatus}
            onChange={(e) => {
              setShiftStatus(e.target.value as 'ALL' | 'ACTIVE' | 'CANCELLED');
              setPage(0);
            }}
          >
            <option value="ALL">Tất cả</option>
            <option value="ACTIVE">Hiệu lực</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
      </div>
    ) : null;

  const tabButton = (tab: Tab, icon: React.ReactNode, label: string, count: number) => (
    <button type="button" aria-pressed={activeTab === tab} onClick={() => switchTab(tab)}>
      {icon} {label} ({count})
    </button>
  );

  return (
    <>
      <PageHeader
        eyebrow="DỊCH VỤ & XƯỞNG"
        title="Khoang sửa chữa & lịch ca làm việc"
        description="Điều phối cầu nâng, đội ngũ kỹ thuật viên và ca trực của xưởng dịch vụ."
        actions={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                void current.refetch();
                void reservationsQuery.refetch();
              }}
              pending={current.isFetching}
            >
              <RefreshCw size={15} /> Làm mới
            </Button>
            {primaryAction}
          </>
        }
      />

      <MetricGrid
        label="Tổng quan xưởng"
        items={[
          {
            label: 'Khoang sẵn sàng',
            value: `${Math.max(activeBayCount - busyBayCount, 0)}/${allBays.length}`,
            hint: `${busyBayCount} khoang đang có xe / giữ chỗ`,
            tone: 'success',
            icon: <Warehouse size={18} />,
          },
          {
            label: 'Giữ chỗ & bảo trì',
            value: `${heldCount} / ${maintenanceCount}`,
            hint: 'Lượt giữ chỗ / lượt bảo trì',
            tone: 'warning',
            icon: <Hammer size={18} />,
          },
          {
            label: 'Kỹ thuật viên hoạt động',
            value: activeEmployees.length,
            hint: `${activeEmployees.filter((e) => e.canQc).length} người có quyền QC`,
            icon: <Users size={18} />,
          },
          {
            label: 'Ca làm việc hiệu lực',
            value: liveShiftCount,
            hint: 'Đang trong ca và sắp tới',
            tone: 'info',
            icon: <CalendarClock size={18} />,
          },
        ]}
      />

      <div className="info-strip">
        <Warehouse size={18} />
        <span>
          Quy tắc giao việc: kỹ thuật viên phải có ca làm việc hiệu lực bao phủ khung giờ phân công
          và đúng chuyên môn mới được giao việc trong lệnh sửa chữa.
        </span>
      </div>

      <section className="panel table-panel">
        <div className="tabs" role="group" aria-label="Chọn nội dung hiển thị">
          {tabButton(
            'dispatch',
            <Hammer size={15} aria-hidden />,
            'Điều phối xưởng',
            allBays.filter((bay) => currentByBay.has(String(bay.id))).length,
          )}
          {tabButton('bays', <Wrench size={15} aria-hidden />, 'Khoang sửa chữa', allBays.length)}
          {tabButton(
            'shifts',
            <CalendarRange size={15} aria-hidden />,
            'Lịch ca làm việc',
            allShifts.length,
          )}
          {tabButton(
            'employees',
            <Users size={15} aria-hidden />,
            'Đội ngũ kỹ thuật viên',
            employees.length,
          )}
        </div>

        {activeTab !== 'employees' && activeTab !== 'dispatch' && (
          <div className="user-filters" role="search" aria-label="Bộ lọc">
            {filtersFor(activeTab)}
          </div>
        )}

        {current.isLoading ? (
          <Loading label="Đang tải dữ liệu…" />
        ) : current.isError ? (
          <QueryError error={current.error} retry={() => current.refetch()} />
        ) : activeTab === 'dispatch' ? (
          <LiveDispatchBoard
            bays={allBays}
            currentByBay={currentByBay}
            onAssignShift={(bay) => {
              setShiftPresetEmployee(null);
              setShiftPresetBay(bay.id);
              setShiftModalOpen(true);
            }}
          />
        ) : activeTab === 'bays' ? (
          <>
            <DataTable<ServiceBay>
              caption="Danh sách khoang sửa chữa"
              columns={bayColumns}
              data={pageRows(bays)}
              busy={baysQuery.isFetching}
            />
            <Pagination
              page={page}
              size={size}
              data={pageInfo(bays.length)}
              policy={policy}
              onChange={onPageChange}
            />
          </>
        ) : activeTab === 'shifts' ? (
          <>
            <DataTable<Shift>
              caption="Danh sách ca làm việc"
              columns={shiftColumns}
              data={pageRows(shifts)}
              busy={shiftsQuery.isFetching}
            />
            <Pagination
              page={page}
              size={size}
              data={pageInfo(shifts.length)}
              policy={policy}
              onChange={onPageChange}
            />
          </>
        ) : (
          <>
            <DataTable<Employee>
              caption="Danh sách kỹ thuật viên"
              columns={employeeColumns}
              data={pageRows(employees)}
              busy={employeesQuery.isFetching}
            />
            <Pagination
              page={page}
              size={size}
              data={pageInfo(employees.length)}
              policy={policy}
              onChange={onPageChange}
            />
          </>
        )}
      </section>

      <ServiceBayEditor
        bay={editingBay}
        open={bayModalOpen}
        onClose={() => setBayModalOpen(false)}
        onSaved={() => baysQuery.refetch()}
      />

      <BayMaintenanceModal
        bay={maintenanceBay}
        open={!!maintenanceBay}
        onClose={() => setMaintenanceBay(null)}
        onSaved={() => reservationsQuery.refetch()}
      />

      <BayReservationsModal
        bay={reservationsBay}
        open={!!reservationsBay}
        onClose={() => setReservationsBay(null)}
      />

      <ShiftEditor
        open={shiftModalOpen}
        initialEmployeeId={shiftPresetEmployee}
        initialServiceBayId={shiftPresetBay}
        onClose={() => setShiftModalOpen(false)}
        onSaved={() => shiftsQuery.refetch()}
      />

      <ShiftRegistrationModal
        open={registerModalOpen}
        onClose={() => setRegisterModalOpen(false)}
      />

      <EmployeeEditor
        employee={editingEmployee}
        takenUserIds={takenUserIds}
        open={employeeModalOpen}
        onClose={() => setEmployeeModalOpen(false)}
        onSaved={() => employeesQuery.refetch()}
      />

      <ConfirmDialog
        open={!!deletingBayId}
        title="Xác nhận xóa khoang sửa chữa?"
        description="Khoang sửa chữa sẽ bị xóa khỏi hệ thống. Các phiếu sửa chữa trong quá khứ vẫn được lưu lịch sử."
        danger
        onCancel={() => setDeletingBayId(null)}
        onConfirm={handleDeleteBay}
      />

      <ConfirmDialog
        open={!!cancelingShiftId}
        title="Xác nhận hủy ca làm việc?"
        description="Ca làm việc này sẽ được chuyển sang trạng thái Đã hủy."
        danger
        onCancel={() => setCancelingShiftId(null)}
        onConfirm={handleCancelShift}
      />
    </>
  );
}

export function SchedulingPage() {
  const policy = useUiPolicy();
  return policy.isPending ? (
    <Loading />
  ) : policy.error ? (
    <QueryError
      error={policy.error}
      retry={() => {
        void policy.refetch();
      }}
    />
  ) : (
    <ReadyScheduling policy={policy.data} />
  );
}
