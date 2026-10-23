import { Component, Suspense, lazy, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  createBrowserRouter,
  Link,
  Navigate,
  Outlet,
  RouterProvider,
  useLocation,
  useRouteError,
} from 'react-router-dom';
import { AuthProvider, useAuth } from './providers/AuthProvider';
import { ToastProvider } from './providers/ToastProvider';
import { AppShell } from './layouts/AppShell';
import { Button, Loading } from '../shared/components/ui';
import { retryQuery } from '../shared/api/errors';
import type { Role } from '../shared/types/contracts';

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
const UsersPage = lazy(() =>
  import('../features/identity/pages/UsersPage').then((module) => ({
    default: module.UsersPage,
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
const DebtLedgerPage = lazy(() =>
  import('../features/billing/pages/DebtLedgerPage').then((module) => ({
    default: module.DebtLedgerPage,
  })),
);

export function RequireAuth() {
  const { user } = useAuth();
  const location = useLocation();
  return user ? (
    <Outlet />
  ) : (
    <Navigate to="/login" replace state={{ returnTo: location.pathname + location.search }} />
  );
}

export function RequireRoles({ allowedRoles }: { allowedRoles: readonly Role[] }) {
  const { user } = useAuth();
  return user && allowedRoles.includes(user.role) ? <Outlet /> : <Navigate to="/403" replace />;
}

export function RequireAdmin() {
  return <RequireRoles allowedRoles={['ADMIN']} />;
}

const STAFF_ROLES: readonly Role[] = ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF', 'TECHNICIAN'];
const WAREHOUSE_ROLES: readonly Role[] = ['ADMIN', 'WAREHOUSE_STAFF'];
const FINANCE_ROLES: readonly Role[] = ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF'];

function Home() {
  const { user } = useAuth();
  if (user?.role === 'ADMIN') return <Navigate to="/admin/reports" replace />;
  if (user?.role === 'WAREHOUSE_STAFF') return <Navigate to="/inventory/parts" replace />;
  if (user?.role === 'RECEPTIONIST') return <Navigate to="/billing/invoices" replace />;
  return <Navigate to="/notifications" replace />;
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
      { path: '/login', element: <LoginPage /> },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppShell />,
            children: [
              { index: true, element: <Home /> },
              { path: 'notifications', element: <NotificationsPage /> },
              { path: 'account/security', element: <AccountPage /> },
              {
                element: <RequireRoles allowedRoles={STAFF_ROLES} />,
                children: [
                  { path: 'inventory/parts', element: <PartsPage /> },
                  { path: 'inventory/documents', element: <InventoryDocumentsPage /> },
                  { path: 'billing/invoices', element: <InvoicesPage /> },
                  { path: 'billing/payments', element: <PaymentsPage /> },
                ],
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
                children: [
                  { path: 'billing/debt-ledger', element: <DebtLedgerPage /> },
                ],
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
