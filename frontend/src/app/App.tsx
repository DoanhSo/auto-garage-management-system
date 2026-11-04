import { Component, Suspense, lazy, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  createBrowserRouter,
  Link,
  Navigate,
  RouterProvider,
  useRouteError,
} from 'react-router-dom';
import { AuthProvider, useAuth } from './providers/AuthProvider';
import { ToastProvider } from './providers/ToastProvider';
import { AppShell } from './layouts/AppShell';
import { Button, Loading } from '../shared/components/ui';
import { retryQuery } from '../shared/api/errors';
import { RequireAdmin, RequireAuth, RequireRoles } from './guards/AuthGuard';
import {
  STAFF_ROLES,
  WAREHOUSE_ROLES,
  FINANCE_ROLES,
  FRONT_DESK_ROLES,
  BOOKING_ROLES,
  REPAIR_VIEW_ROLES,
  INVOICE_VIEW_ROLES,
  PAYROLL_VIEW_ROLES,
  ESTIMATE_VIEW_ROLES,
  WARRANTY_ROLES,
  homePathForRole,
} from './access';

const HomePage = lazy(() =>
  import('../features/home/pages/HomePage').then((module) => ({
    default: module.HomePage,
  })),
);
const LoginPage = lazy(() =>
  import('../features/auth/pages/LoginPage').then((module) => ({
    default: module.LoginPage,
  })),
);
const AccountPage = lazy(() =>
  import('../features/auth/pages/AccountPage').then((module) => ({
    default: module.AccountPage,
  })),
);
const RegistrationPage = lazy(() =>
  import('../features/auth/pages/RegistrationPage').then((module) => ({
    default: module.RegistrationPage,
  })),
);
const UsersPage = lazy(() =>
  import('../features/identity/pages/UsersPage').then((module) => ({
    default: module.UsersPage,
  })),
);
const CustomersPage = lazy(() =>
  import('../features/customer/pages/CustomersPage').then((module) => ({
    default: module.CustomersPage,
  })),
);
const VehiclesPage = lazy(() =>
  import('../features/vehicle/pages/VehiclesPage').then((module) => ({
    default: module.VehiclesPage,
  })),
);
const ServicesPage = lazy(() =>
  import('../features/catalog/pages/ServicesPage').then((module) => ({
    default: module.ServicesPage,
  })),
);
const BookingsPage = lazy(() =>
  import('../features/reception/pages/BookingsPage').then((module) => ({
    default: module.BookingsPage,
  })),
);
const AuditPage = lazy(() =>
  import('../features/audit/pages/AuditPage').then((module) => ({
    default: module.AuditPage,
  })),
);
const NotificationsPage = lazy(() =>
  import('../features/notification/pages/NotificationsPage').then((module) => ({
    default: module.NotificationsPage,
  })),
);
const ReportsPage = lazy(() =>
  import('../features/reporting/pages/ReportsPage').then((module) => ({
    default: module.ReportsPage,
  })),
);
const PartsPage = lazy(() =>
  import('../features/inventory/pages/PartsPage').then((module) => ({
    default: module.PartsPage,
  })),
);
const SuppliersPage = lazy(() =>
  import('../features/inventory/pages/SuppliersPage').then((module) => ({
    default: module.SuppliersPage,
  })),
);
const InventoryDocumentsPage = lazy(() =>
  import('../features/inventory/pages/InventoryDocumentsPage').then((module) => ({
    default: module.InventoryDocumentsPage,
  })),
);
const StockReconciliationPage = lazy(() =>
  import('../features/inventory/pages/StockReconciliationPage').then((module) => ({
    default: module.StockReconciliationPage,
  })),
);
const InvoicesPage = lazy(() =>
  import('../features/billing/pages/InvoicesPage').then((module) => ({
    default: module.InvoicesPage,
  })),
);
const PaymentsPage = lazy(() =>
  import('../features/billing/pages/PaymentsPage').then((module) => ({
    default: module.PaymentsPage,
  })),
);
const PayrollPage = lazy(() =>
  import('../features/payroll/pages/PayrollPage').then((module) => ({
    default: module.PayrollPage,
  })),
);
const DebtLedgerPage = lazy(() =>
  import('../features/billing/pages/DebtLedgerPage').then((module) => ({
    default: module.DebtLedgerPage,
  })),
);
const RepairOrdersPage = lazy(() =>
  import('../features/repair/pages/RepairOrdersPage').then((module) => ({
    default: module.RepairOrdersPage,
  })),
);
const EstimatesPage = lazy(() =>
  import('../features/estimate/pages/EstimatesPage').then((module) => ({
    default: module.EstimatesPage,
  })),
);
const SchedulingPage = lazy(() =>
  import('../features/scheduling/pages/SchedulingPage').then((module) => ({
    default: module.SchedulingPage,
  })),
);
const WarrantiesPage = lazy(() =>
  import('../features/warranty/pages/WarrantiesPage').then((module) => ({
    default: module.WarrantiesPage,
  })),
);

function Home() {
  const { user } = useAuth();
  return <Navigate to={homePathForRole(user?.role)} replace />;
}

function StatusPage({ forbidden = false }: { forbidden?: boolean }) {
  return (
    <div className="status-page">
      <p className="eyebrow">{forbidden ? '403 · GIỚI HẠN TRUY CẬP' : '404 · KHÔNG TÌM THẤY'}</p>
      <h1>{forbidden ? 'Bạn chưa có quyền truy cập' : 'Trang này không tồn tại'}</h1>
      <p>
        {forbidden
          ? 'Liên hệ quản trị viên nếu bạn cần quyền cho công việc này.'
          : 'Kiểm tra lại địa chỉ hoặc quay về không gian làm việc.'}
      </p>
      <Link className="button button-primary" to="/">
        Về trang chính
      </Link>
    </div>
  );
}

function RouteError() {
  useRouteError();
  return (
    <div className="status-page">
      <h1>Chưa thể mở trang</h1>
      <p>Đã xảy ra lỗi hiển thị. Hãy thử tải lại trang.</p>
      <Button onClick={() => window.location.reload()}>Tải lại</Button>
    </div>
  );
}

class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  render() {
    return this.state.failed ? (
      <div className="status-page">
        <h1>Ứng dụng cần được tải lại</h1>
        <p>Không thể hiển thị nội dung hiện tại.</p>
        <Button onClick={() => window.location.reload()}>Tải lại</Button>
      </div>
    ) : (
      this.props.children
    );
  }
}

const router = createBrowserRouter([
  {
    errorElement: <RouteError />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/home', element: <Navigate to="/" replace /> },
      { path: '/landing', element: <Navigate to="/" replace /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegistrationPage /> },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppShell />,
            children: [
              { path: 'workspace', element: <Home /> },
              {
                element: <RequireRoles allowedRoles={BOOKING_ROLES} />,
                children: [
                  { path: 'reception', element: <BookingsPage /> },
                  { path: 'vehicles', element: <VehiclesPage /> },
                ],
              },
              {
                element: <RequireRoles allowedRoles={FRONT_DESK_ROLES} />,
                children: [{ path: 'customers', element: <CustomersPage /> }],
              },
              { path: 'services', element: <ServicesPage /> },
              { path: 'notifications', element: <NotificationsPage /> },
              { path: 'account/security', element: <AccountPage /> },
              {
                element: <RequireRoles allowedRoles={REPAIR_VIEW_ROLES} />,
                children: [{ path: 'repair-orders', element: <RepairOrdersPage /> }],
              },
              {
                element: <RequireRoles allowedRoles={INVOICE_VIEW_ROLES} />,
                children: [{ path: 'billing/invoices', element: <InvoicesPage /> }],
              },
              {
                element: <RequireRoles allowedRoles={STAFF_ROLES} />,
                children: [
                  { path: 'repairs', element: <RepairOrdersPage /> },
                  { path: 'scheduling', element: <SchedulingPage /> },
                  { path: 'inventory/parts', element: <PartsPage /> },
                  { path: 'inventory/documents', element: <InventoryDocumentsPage /> },
                ],
              },
              {
                element: <RequireRoles allowedRoles={ESTIMATE_VIEW_ROLES} />,
                children: [{ path: 'estimates', element: <EstimatesPage /> }],
              },
              {
                element: <RequireRoles allowedRoles={WARRANTY_ROLES} />,
                children: [{ path: 'warranties', element: <WarrantiesPage /> }],
              },
              {
                element: <RequireRoles allowedRoles={FINANCE_ROLES} />,
                children: [{ path: 'billing/payments', element: <PaymentsPage /> }],
              },
              {
                element: <RequireRoles allowedRoles={WAREHOUSE_ROLES} />,
                children: [
                  { path: 'inventory/suppliers', element: <SuppliersPage /> },
                  { path: 'inventory/reconciliation', element: <StockReconciliationPage /> },
                ],
              },
              {
                element: <RequireRoles allowedRoles={FINANCE_ROLES} />,
                children: [{ path: 'billing/debt-ledger', element: <DebtLedgerPage /> }],
              },
              {
                element: <RequireRoles allowedRoles={PAYROLL_VIEW_ROLES} />,
                children: [{ path: 'payroll', element: <PayrollPage /> }],
              },
              {
                element: <RequireAdmin />,
                children: [
                  { path: 'admin/users', element: <UsersPage /> },
                  { path: 'admin/audit', element: <AuditPage /> },
                  { path: 'admin/reports', element: <ReportsPage /> },
                ],
              },
              { path: '403', element: <StatusPage forbidden /> },
              { path: '*', element: <StatusPage /> },
            ],
          },
        ],
      },
    ],
  },
]);

const client = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15000,
      retry: retryQuery,
      refetchOnWindowFocus: true,
    },
    mutations: { retry: 0 },
  },
});

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={client}>
        <AuthProvider>
          <ToastProvider>
            <a className="skip-link" href="#main-content">
              Đến nội dung chính
            </a>
            <Suspense fallback={<Loading label="Đang mở không gian làm việc…" />}>
              <RouterProvider router={router} />
            </Suspense>
          </ToastProvider>
        </AuthProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
