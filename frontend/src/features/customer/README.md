# M02 Khách hàng (Frontend)

Chủ trì: TV2.

## Cấu trúc module
- `api/customerApi.ts`: Giao tiếp API Backend `/api/v1/customers` (CRUD, tra cứu, kiểm tra quyền và giải mã dữ liệu).
- `schemas/customerSchema.ts`: Zod schema xác thực dữ liệu form (họ tên, số điện thoại, email, MST công ty, hạn mức công nợ).
- `hooks/useCustomers.ts`: React Query hooks (`useCustomersQuery`, `useCustomerQuery`, `useCreateCustomerMutation`, `useUpdateCustomerMutation`, `useDeleteCustomerMutation`).
- `components/`:
  - `CustomerFilters.tsx`: Thanh tìm kiếm thời gian thực (debounced) và bộ lọc loại khách / trạng thái.
  - `CustomerEditor.tsx`: Drawer (Sheet) thêm mới / chỉnh sửa hồ sơ khách hàng, bảo vệ thay đổi chưa lưu (`useUnsavedChangesGuard`).
  - `CustomerCards.tsx`: Giao diện danh sách dạng thẻ cho thiết bị di động (Responsive `< 640px`).
- `pages/CustomersPage.tsx`: Màn hình quản lý khách hàng chính (PageHeader, Search, DataTable, ConfirmDialog ngừng hoạt động).

Phạm vi CSDL: `customers`.
Quy chuẩn giao diện: Thống nhất với hệ thống thiết kế chung (AppShell, Radix UI Dialog, TanStack Table, Lucide Icons).
