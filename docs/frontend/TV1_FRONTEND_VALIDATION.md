# TV1 frontend — kết quả triển khai và kiểm chứng

Ngày 15/10/2026. Source thực tế trong `frontend/`, kết nối API Java/Spring TV1 hiện tại. Không dùng dữ liệu giả trong các trang ứng dụng; fixtures chỉ nằm trong test.

## Đã triển khai

- React 19, TypeScript strict, Vite 8, React Router 7 data router; lazy routes, ErrorBoundary, shell/menu theo role, 403/404.
- TanStack Query/Table; React Hook Form/Zod; Radix dialog/sheet; shared Field/Button/Pagination/states, responsive user cards và dirty guard.
- Login/me/logout/password, admin user CRUD/role/soft-lock/reactivation, private WEB inbox/read/count, audit readonly, dashboard/debt/CSV.
- Typed error catalog, không hiển thị lỗi server thô; GET cancellation, limited retry cho network/5xx, mutation retry=0, trạng thái chưa xác nhận khi lỗi mạng lúc ghi.
- Session memory và cache isolation; logout broadcast không truyền token; 401/3005 và 503 không logout nhầm. Safe returnTo được áp dụng nhất quán kể cả khi React render lại sau login.
- ID/version/BigDecimal giữ chính xác bằng lossless JSON; version gửi dạng numeric token. Tiền dùng Decimal.js. Password codepoint/UTF-8 byte checks; password không trim/lowercase.
- Kỳ báo cáo theo garage IANA timezone, inclusive UI end-date → UTC end-exclusive; KPI kỳ và snapshot hiện tại tách riêng. CSV kiểm tra content type/JSON errors, tên file lấy ngày gara.
- Public GET `/api/v1/public/ui-policy` lấy 7 trường từ typed config; Security chỉ permit GET này, không mở các endpoint khác. Không thêm dependency backend. Sau yêu cầu tách Khóa/Xóa, bổ sung V8 users.deleted_at và CHECK; giữ nguyên V1–V7.

## Lệnh và kết quả thực tế

Frontend sử dụng Node **24.19.0** và npm 12 trên máy thực thi; package.json yêu cầu nhánh Node 24 từ 24.15. Máy có shim Node cũ, nên vòng kiểm chứng gọi trực tiếp runtime Node 24; không đổi cấu hình global.

- TypeScript `tsc -b`: pass.
- ESLint `eslint . --max-warnings 0`: pass.
- Prettier format/check cho source/config/tests: pass.
- Vitest: **51 tests pass / 4 files**, không skip; đã chạy lại trước khi push nhánh dev_doanh.
- Production `vite build`: pass, có route chunks, không sourcemap production.
- Chromium E2E với Java backend/MySQL thật: Suite **16 tests pass**, không retry/skip, gồm add/edit/lock/unlock/delete và dialog căn giữa không cuộn. Sau thiết kế lại bộ lọc/phân trang, **3 E2E tập trung pass**, trong đó có một case mới kiểm tra filter, reset page, chips và mobile; tổng cộng 17 case riêng biệt đã được chạy qua các vòng.
- Backend `.\mvnw.cmd -B -ntp package`: **148 tests, 0 failures/errors/skipped**; package thành công sau khi dừng JAR preview đang giữ file trên Windows, gồm H2 và MySQL test riêng.
- `npm install`/audit tại vòng cài dependency: **0 vulnerabilities** theo registry thời điểm chạy; không phải cam kết không có CVE tương lai.

## Hành vi được kiểm tra

Vitest/MSW kiểm tra: bigint/decimal vượt độ chính xác JS, numeric version, timezone/DST/max duration, password Unicode/72-byte/confirm, login không áp min mới, safe returnTo, safe audit/notification text, bearer/public headers, session expiry 3001/3003, giữ phiên 3005/403/503, request cũ và body streaming sau logout, JSON lỗi export, malformed contract, validation/anti-double-submit, DTO allowlist, giữ conflict draft, discard cancel, last-admin error, sai password hiện tại, mark-read text không thực thi HTML.

Chromium/API thật kiểm tra: create/edit/email-null/lock/reactivate, CSV, ngày đảo ngược, audit detail readonly, WEB read persisted, inbox isolation và UI/API RBAC cho bốn role non-admin; admin qua các flow còn lại. Kiểm tra password change/revocation, reload không giữ token/storage, logout revocation, mobile drawer/focus, actual concurrent update và explicit reload, self-demotion confirmation/revocation, auth infrastructure 503/retry.

axe-core chạy tags WCAG 2 A/AA, 2.1 A/AA và 2.2 AA trên login, reports, users, create drawer, inbox, account: không có violation tự động trong các trạng thái đã kiểm tra. Kiểm tra viewport 320/360/768/1024/1440px và mobile 390px: không tràn trang, Save vẫn hiển thị, Escape/focus restore hoạt động. Đây chưa thay thế audit accessibility thủ công đầy đủ.

## Môi trường và artifacts

MySQL 8.4 trong container riêng `autogarage-tv1-frontend-test`, bind loopback 55917, DB `autogarage_test`. Backend thử nghiệm loopback 55818, frontend 5183; không dùng DB nghiệp vụ. Signing key/bootstrap password sinh ngẫu nhiên; worker/reminder/EMAIL/SMS đều tắt.

Logs cục bộ (Git ignore): `target/tv1-frontend-backend-check.log`, `frontend/.cache/{vitest,e2e,build}.log`; report `frontend/playwright-report/`; ảnh `frontend/output/playwright/`. Không commit credentials/logs/artifacts. Theo yêu cầu chạy thử, các dịch vụ test được giữ chạy. Người dùng yêu cầu tài khoản thử **admin/admin**; chỉ fixture trong DB test được đặt BCrypt hash này, policy tạo password trong source không bị hạ.

Ảnh desktop/mobile đã quan sát và chỉnh layout/filter width, focus, mobile cards và tương phản chữ phụ. UI chạy trong in-app browser và đã đăng nhập thử thành công.

## Giới hạn chưa nghiệm thu

- Chưa deploy production hoặc xác thực Nginx/hosting thật; SPA fallback, HTTPS/CORS/cache được hướng dẫn trong frontend/README.md.
- Chưa kiểm tra Firefox/WebKit, screen reader, zoom 200%, toàn bộ trạng thái WCAG thủ công hoặc tải lớn.
- Provider EMAIL/SMS UI, composer/outbox, refresh/MFA/forgot-password, time series và filters chưa có API thuộc P1.
- Không đánh dấu toàn bộ T01–T38 hoặc tích hợp nghiệp vụ TV2–TV4 hoàn thành từ kết quả này. FE-14/15 còn phần QA/production nêu trên.

Hướng dẫn tái chạy, routes, cấu hình và quy tắc mở rộng: [frontend/README.md](../../frontend/README.md).

## Cập nhật theo phản hồi giao diện

- Thêm/sửa tài khoản dùng dialog giữa màn hình, grid hai cột. Đo vị trí giữa, bounds đủ 6 trường và scrollHeight <= clientHeight tại 1366×768, 1280×720, 1024×600, 390×844, 430×429, 320×740. Trường hợp lỗi xung đột nhiều nội dung hoặc viewport cực nhỏ vẫn giữ overflow dự phòng để không cắt mất thông tin.
- Field chỉ hiện lỗi đỏ khi invalid, ẩn hint tại thời điểm đó và aria-describedby chỉ tới phần còn hiển thị. Không hiển thị lặp hint và lỗi password/username.
- Nút Khóa đổi thành Mở khóa khi inactive; PUT active=false/true có version và thu hồi token. DELETE ghi deleted_at, inactive, giữ FK/audit, loại khỏi list/get/update; không restore bằng Sửa, không tái sử dụng username/email cũ. V8 bổ sung index và CHECK ngăn active tài khoản đã xóa.
- UserFilters có nhãn riêng, search-clear, trạng thái dạng segmented buttons, sort có cả tên trường/chiều, chips bỏ từng bộ lọc và Đặt lại. Mọi thay đổi filter/sort reset page=0; reset giữ page size đang chọn.
- Pagination hiển thị khoảng thực tế/tổng chính xác, trang hiện tại/tổng trang, nút số trang và Trước/Sau có aria-label. Tối đa 5 nút số: trang đầu, trang trước hiện tại, trang hiện tại, trang sau hiện tại và trang cuối; gộp nút trùng ở đầu/cuối, dùng dấu … khi có khoảng cách. Đổi số dòng reset trang đầu, zero results không tạo trang giả, giới hạn trang lấy từ server. Kiểm tra số lớn giữ đủ chữ số; 16 test phân trang pass, gồm điều hướng về trang đầu và trạng thái disabled.
- Backend 148 bài pass trên DB mới riêng cổng 55918; schema V8 được nâng tiếp trên DB preview cổng 55917. Sau khi test xong, container kiểm thử cổng 55918 được dọn; preview 5183/55818 được giữ chạy, tài khoản admin/admin vẫn dùng được.
- Source và log cập nhật: UserFilters.tsx, DataTable.tsx, pagination.ts, UserEditor/UserCards/UsersPage, Field/Sheet; V8 và identity/auth/token checks. Logs ở .cache/{account-dialog-backend,filter-tests,filter-e2e} và target/account-dialog-*.log; ảnh accounts-centered-{add,edit,delete}.png và accounts-filters.png trong output/playwright/.
