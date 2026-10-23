# AutoGarage — frontend TV1

Ứng dụng React/TypeScript tích hợp API TV1. UI tiếng Việt, light mode; desktop dùng bảng, mobile dùng thẻ tài khoản; form thêm/sửa là dialog ở giữa, hai cột, vừa khung nhìn.

## Chạy local

Cần **Node.js 24.15 trở lên thuộc nhánh 24** và npm. Node 16 không chạy Vite 8. Chạy backend theo README gốc trước, có MySQL/Flyway và tài khoản được cấp từ cấu hình riêng.

```powershell
cd frontend
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Mở `http://127.0.0.1:5173`. Vite chuyển `/api` đến `DEV_API_PROXY`, mặc định `http://127.0.0.1:8080`, giữ cặp Host/Origin của trình duyệt để các request ghi cùng nguồn không bị nhận nhầm là CORS. Proxy không xóa hoặc giả mạo Origin. Nếu gọi API khác origin trực tiếp, backend cần allow origin tương ứng trong `AUTH_CORS_ORIGINS`. Source không chứa tài khoản/mật khẩu đăng nhập mặc định.

`VITE_API_BASE_URL`, `VITE_NOTIFICATION_POLL_MS`, `VITE_REQUEST_TIMEOUT_MS` là biến công khai lúc build. `DEV_API_PROXY` chỉ dùng ở dev server. Không đặt signing key, DB/SMTP/SMS credential vào `VITE_*`.

Policy mật khẩu, phân trang, kỳ báo cáo, múi giờ lấy từ **GET `/api/v1/public/ui-policy`** cùng backend phiên bản này. Policy lỗi: login vẫn hoạt động; trang cần policy có lỗi và Retry, không fallback giới hạn giả.

## Chức năng

- `/login`: username/email, validation, safe returnTo, lỗi credential an toàn.
- `/account/security`: thông tin cá nhân và đổi mật khẩu, mọi role đã đăng nhập.
- `/admin/users`: ADMIN tìm/lọc/sort/phân trang, thêm/sửa, đổi role, khóa/mở khóa và xóa. Khóa/Mở khóa dùng cùng nút đổi theo trạng thái; xóa loại khỏi danh sách quản trị, giữ lịch sử và không thể khôi phục qua Sửa. PUT chỉ gửi field allowlist và version; email trống gửi null. 409 giữ draft, chỉ thay bằng bản server sau lựa chọn rõ ràng. Có lỗi last-admin và xác nhận đổi quyền chính mình.
- `/notifications`: WEB inbox của principal, lọc chưa đọc, chi tiết text và lưu đã đọc. Badge dùng tổng unread từ server; polling dừng ở tab ẩn, chậm lại sau lỗi.
- `/admin/audit`: ADMIN lọc action/actor ID, xem metadata; không sửa/xóa.
- `/admin/reports`: ADMIN chọn ngày theo garage timezone, KPI trong kỳ, số dư hiện tại, công nợ và CSV có bearer. End-date gồm cả ngày; API dùng UTC `[from,until)`.
- Trang 403/404, loading/empty/error/retry. Backend quyết định quyền cuối cùng.

Token chỉ nằm trong memory: **reload phải đăng nhập lại** vì backend chưa có refresh token. Không lưu token/password trong URL, localStorage, sessionStorage hoặc persisted cache. Logout xóa private cache; BroadcastChannel chỉ truyền sự kiện logout. 401/3001,3003 kết thúc phiên; 3005 khi sai mật khẩu hiện tại và 503 giữ phiên. Mutation retry=0; mất kết nối lúc ghi báo chưa xác nhận kết quả.

## Cấu trúc và cách mở rộng

```text
src/app/                 router, shell, auth/toast providers
src/features/auth/       API, hooks, schema, login/account
src/features/identity/   API, hooks, schema, UserEditor/UserCards/UserFilters, users page
src/features/notification/  inbox/detail/read/count
src/features/audit/      readonly audit
src/features/reporting/  dashboard/debt/CSV
src/shared/api/          HTTP, safe errors, lossless JSON
src/shared/config/       validated env và public policy
src/shared/components/   Field/Button/Sheet/Dialog, states, DataTable/Pagination
src/shared/hooks/        debounce, URL paging, responsive, dirty guard
src/shared/types/        contracts và Zod response schemas
src/shared/utils/        exact money, timezone/date, safe navigation
src/test/                MSW và component/integration tests
e2e/                     Chromium với API thật
```

Component gọi hook/API của feature; không fetch rải rác. Query riêng tư có session trong key; GET dùng AbortSignal; mutation allowlist DTO và invalidate đúng feature. ID/version/BigDecimal giữ string chính xác tại JSON boundary; version gửi lại dạng JSON number bằng lossless-json. Không log credential hoặc lỗi server thô. MapStruct/JPA/ServiceImpl là quy tắc backend, không áp máy móc vào React.

Tái sử dụng primitives, nhãn field, keyboard/focus, pending và dirty guard. Không thêm menu/filter/nút theo API chưa có. Các skeleton TV2–TV4 được giữ để nhóm phát triển tiếp.

## Kiểm tra

```powershell
npm run typecheck
npm run lint
npm run format:check
npm test
npm run test:proxy
npm run build
```

E2E ghi dữ liệu: dùng backend/DB test riêng, worker/reminder/provider tắt. Suite tạo users và WEB notices, không xóa audit. Hủy DB test sau sử dụng; không chạy với DB nghiệp vụ.

```powershell
$env:E2E_API_URL='http://127.0.0.1:55818/api/v1'
$env:E2E_IDENTIFIER='<admin test của bạn>'
$env:E2E_PASSWORD='<mật khẩu test riêng>'
$env:E2E_ALLOW_MUTATIONS='true'
npx playwright install chromium
npm run test:e2e
```

Runner chỉ nhận backend `127.0.0.1`, tự mở Vite cổng **5183**; backend cần allow origin `http://127.0.0.1:5183`. Không chạy hai suite cùng fixture. Report ở `playwright-report/`, ảnh ở `output/playwright/`, đều Git ignore. Không lưu auth storage state; trace mặc định tắt. Muốn dùng dev server đã chạy cùng cấu hình, chủ động đặt `E2E_USE_RUNNING_SERVER=true`.

## Build và triển khai

`npm run build` tạo `dist/`. Static server cần SPA fallback về `index.html` cho route UI; `/api/` proxy riêng, giữ JSON/status, không fallback HTML. Nginx location `/` có thể dùng `try_files $uri $uri/ /index.html`; cấu hình upstream từ môi trường. Cache dài cho assets hash, `index.html` revalidate.

Dùng HTTPS và CORS origin rõ ràng. Vite dev/preview chỉ phục vụ local. API origin khác cần `VITE_API_BASE_URL` lúc build và backend allow origin frontend. Config startup sai có màn hình lỗi. Chưa deploy hosting trong task này.

P1 chưa triển khai: refresh/MFA/quên mật khẩu, soạn thông báo/outbox console, EMAIL/SMS UI, time series chart, role/audit-date filters và mark-all-read. Kế hoạch/kết quả thực tế ở `../docs/frontend/`.


Bộ lọc tài khoản có trạng thái chọn trực tiếp, sắp xếp đủ chiều, chips và nút Đặt lại. Pagination hiển thị khoảng bản ghi/tổng, số trang và chọn trang trực tiếp. Validation chỉ hiển thị một lỗi đỏ cho mỗi field, ẩn hint khi field đang lỗi.
