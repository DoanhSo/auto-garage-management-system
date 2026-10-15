# Kế hoạch frontend TV1 — AutoGarage

Ngày lập: 15/10/2026. Trạng thái: P0 đã triển khai; kết quả kiểm chứng thực tế ở TV1_FRONTEND_VALIDATION.md.

## 1. Mục tiêu và căn cứ

Xây dựng giao diện quản trị gara tối giản, cân đối, dễ đọc và nhất quán. Người dùng biết mình đang ở đâu, được làm gì, thao tác đã thành công chưa và cần sửa gì khi có lỗi. Component tái sử dụng phải có quy tắc kích thước, trạng thái, validation và accessibility rõ ràng.

Căn cứ đã đọc: WORK_BREAKDOWN, TV1_IMPLEMENTATION_PLAN, api/SECURITY, api/TV1, CONFIGURATION_AND_ERRORS và DTO/controller/repository hiện tại. Phạm vi TV1 là M01 identity/auth, audit/I01/I08, M13 notification/reminder và M14 reporting. Ứng dụng đã đặt ở `frontend/` trong Git repository chứa pom.xml.

Nguyên tắc: triển khai theo API thật; demo có dữ liệu mẫu phải ghi rõ. Chỉ đánh dấu xong đầu việc khi có UI, xử lý lỗi, test và tích hợp tương ứng. Kết quả backend 145 bài không thay thế kiểm thử frontend.

## 2. Phạm vi sản phẩm và quyền truy cập

### P0: frontend hoạt động với backend hiện có

1. Đăng nhập; đăng xuất; trạng thái hết phiên; trở lại trang hợp lệ sau đăng nhập.
2. Tài khoản cá nhân: xem thông tin hiện tại, đổi mật khẩu; không tự thêm API sửa hồ sơ cá nhân.
3. ADMIN: danh sách tài khoản, tìm kiếm, lọc trạng thái, sort, phân trang, xem chi tiết, tạo, sửa, đổi role, khóa/mở lại.
4. Mọi role: inbox WEB của chính mình, lọc chưa đọc, xem nội dung và đánh dấu một thông báo đã đọc; badge số chưa đọc.
5. ADMIN: nhật ký audit chỉ đọc; lọc actor ID và action; xem metadata an toàn.
6. ADMIN: dashboard tổng hợp, công nợ hóa đơn và tải CSV theo kỳ.
7. Shell dùng chung: menu theo role, breadcrumb, header, thông báo, tài khoản, trang 403/404, loading/error/empty/offline.

ADMIN được quản trị tài khoản, audit, báo cáo; mọi role được xem inbox riêng và đổi mật khẩu. RECEPTIONIST, WAREHOUSE_STAFF, TECHNICIAN, CUSTOMER không được xem dashboard tài chính của ADMIN. Khách hàng và nhân viên dùng chung nền tảng nhưng navigation sẽ được mở rộng theo module của TV2–TV4.

Role là một trong năm giá trị cố định; UI chọn đúng một role. Không dựng màn hình tạo role, permission builder hoặc checkbox quyền tùy ý vì backend không có các contract này. I01 vẫn do backend kiểm tra ownership/assignment/QC; ẩn nút ở UI không thay thế authorization.

### P1: làm sau khi có đủ contract/vận hành

- Soạn thông báo WEB thủ công cho tài khoản do admin chọn; UI lookup user bằng API search hiện có. Không yêu cầu người vận hành nhập raw user ID/JSON/eventKey.
- EMAIL/SMS sau khi kiểm chứng provider và có capability/policy cho phép hiển thị kênh. Nhận customer chưa có account còn phụ thuộc API lookup khách hàng/policy TV2.
- Biểu đồ doanh thu theo ngày/tháng cần API time series; dashboard hiện chỉ trả tổng hợp.
- Quản trị outbox toàn hệ thống, xem lỗi giao vận, gửi lại FAILED cần API admin riêng và audit/RBAC; inbox hiện tại không cung cấp chức năng này.
- Lọc tài khoản theo role, lọc audit theo ngày/entity, mark-all-read, SSE/WebSocket, biểu đồ so sánh kỳ cần API tương ứng.
- Refresh token, nhớ đăng nhập qua reload, quên mật khẩu, reset password bởi admin, MFA và quản lý phiên thiết bị cần thay đổi backend; không vẽ như chức năng đã hoạt động.

## 3. Ngôn ngữ thiết kế

### 3.1. Bố cục và tỷ lệ

- Desktop từ khoảng 1280px: sidebar 232px, header 64px, khoảng đệm nội dung 24–32px. Nội dung fluid, tối đa khoảng 1440px và căn giữa khi màn hình lớn.
- Grid 12 cột, gutter 24px. Form tối đa 2 cột khi các trường có quan hệ ngang hàng; nội dung dài và error chiếm toàn hàng. Không ép mọi card cùng chiều cao khi chức năng khác nhau.
- Tablet khoảng 768–1279px: sidebar thu gọn hoặc drawer, padding 20px, form 1–2 cột tùy nội dung thực tế. Không chỉ dựa vào chiều rộng thiết bị để quyết định mật độ.
- Mobile 360–767px; kiểm tra thêm 320px: menu drawer, padding 16px, form một cột, sheet toàn chiều rộng. Bảng đổi sang item list cho tài khoản/inbox; bảng công nợ có thể cuộn ngang trong vùng riêng và giữ mã hóa đơn dễ theo dõi.
- Khoảng cách theo scale 4/8px: 4, 8, 12, 16, 24, 32, 48. Header trang gồm tiêu đề và mô tả ngắn bên trái; một hành động chính bên phải.
- Font system hỗ trợ tiếng Việt; body 14–16px, heading 20–28px; label luôn hiển thị. Số tiền dùng tabular figures; không thu nhỏ chữ để nhét thêm cột.

### 3.2. Màu sắc và bề mặt

- Nền trung tính sáng, bề mặt trắng, border mảnh; màu nhấn xanh dương trầm cho hành động và focus. Dark mode dùng semantic tokens và kiểm tra tương phản, không đảo màu cơ học.
- Không gradient, shadow dày, dàn card trang trí hoặc mỗi KPI một màu. Chỉ tô màu khi có ý nghĩa trạng thái.
- Active: nhãn “Hoạt động”; inactive: “Đã khóa”; pending: “Đang xử lý”; lỗi: “Cần xử lý”. Màu luôn đi kèm chữ/icon; không chỉ dùng chấm xanh/đỏ.
- Border radius 8px cho input/button, 12px cho panel; cùng component dùng một token. Có thể ưu tiên light mode cho lần đầu, dark mode chỉ nghiệm thu khi đã kiểm tra tương phản đầy đủ.

### 3.3. Nút và tương tác

- Primary: “Đăng nhập”, “Tạo tài khoản”, “Lưu thay đổi”, “Áp dụng bộ lọc”. Mỗi vùng thao tác chỉ một nút primary.
- Secondary: “Hủy”, “Đóng”, “Tải CSV”, “Thử lại”. Ghost: “Xem chi tiết”, “Đặt lại bộ lọc”. Destructive: “Khóa tài khoản”, chỉ xuất hiện trong ngữ cảnh có xác nhận.
- Control desktop cao 40px; nút trong bảng 32–36px nhưng vùng nhấn đủ và tách biệt. Thiết bị cảm ứng ưu tiên vùng tương tác ít nhất 44×44px. Icon 16–20px; không dùng icon-only cho hành động chính.
- Pending giữ nguyên chiều rộng nút, thêm spinner và nhãn “Đang lưu…”. Chặn submit lặp ngay khi bắt đầu request; không đóng form cho tới khi server xác nhận.
- Không vô hiệu submit chỉ vì form ban đầu chưa hợp lệ: cho submit để hiển thị hướng dẫn lỗi; khóa khi pending hoặc không có quyền. Form edit chỉ bật lưu khi có thay đổi hợp lệ/đã được người dùng sửa.
- Thành công dùng toast ngắn và cập nhật nội dung thật. Lỗi field đặt ngay dưới field; lỗi form ở đầu form; lỗi trang có Retry. Không dùng toast làm nơi hiển thị duy nhất của lỗi quan trọng.
- Dialog/sheet có tên truy cập, focus trap, Escape, trả focus về nút mở. Cảnh báo rời form có thay đổi chưa lưu; không lưu draft mật khẩu.

## 4. Bản đồ màn hình

### 4.1. Login — `/login`

Form rộng khoảng 400–440px, một cột: nhận diện AutoGarage, tiêu đề, tên đăng nhập/email, mật khẩu có nút hiện/ẩn, nút đăng nhập và vùng lỗi. Không dùng ảnh trang trí lớn che form. Không thêm nút đăng ký/nhớ đăng nhập/quên mật khẩu khi backend chưa hỗ trợ.

Validate required, identifier tối đa 254; không trim hoặc lowercase mật khẩu. Không áp dụng policy tạo mật khẩu mới vào login: account cũ vẫn phải được đăng nhập. Password quá giới hạn transport/BCrypt được giải thích phù hợp; sai username/password/khóa account dùng cùng thông báo an toàn.

Guest truy cập link có quyền sẽ được giữ `returnTo` nội bộ đã kiểm tra. ADMIN mặc định đến báo cáo; role khác đến inbox trong giai đoạn TV1. Không chuyển non-admin vào dashboard admin rồi để gặp 403.

### 4.2. Shell và tài khoản — `/account/security`

Sidebar theo quyền: ADMIN có Báo cáo, Tài khoản, Nhật ký, Thông báo; tất cả có Tài khoản cá nhân. Header chứa breadcrumb, bell badge, menu người dùng. Module chưa triển khai không xuất hiện như menu hoạt động.

Trang tài khoản chia phần thông tin chỉ đọc và đổi mật khẩu. Form đổi mật khẩu: mật khẩu hiện tại, mới, xác nhận; policy động, byte counter khi gần ngưỡng, nút hiện/ẩn có accessible name. Xác nhận không gửi lên API. Đổi thành công thu hồi token nên xóa session/cache, chuyển login và thông báo cần đăng nhập lại.

### 4.3. Quản trị tài khoản — `/admin/users`

Header “Tài khoản” + mô tả + “Tạo tài khoản”. Toolbar: tìm theo tên/username/email, trạng thái Tất cả/Hoạt động/Đã khóa, reset filters. Chưa có filter role phía server nên không lọc role trên một trang rồi hiển thị như kết quả toàn bộ dữ liệu.

Bảng: Tài khoản (fullName + username), Email, Vai trò, Trạng thái, Ngày tạo, Thao tác. Avatar chữ cái là tùy chọn và không chiếm thêm cột. Nội dung text căn trái, trạng thái gọn, action căn phải. Email dài cắt hợp lý với cách xem đầy đủ bằng bàn phím; không để một giá trị phá chiều rộng bảng.

Sort chỉ cho field backend hỗ trợ: id, username, fullName, role, active, createdAt; chỉ một sort phía server. Pagination phía server, trang UI bắt đầu 1 nhưng API bắt đầu 0. Page size lấy từ policy; đổi filter/size đưa về trang đầu, xóa dữ liệu làm trang hiện tại rỗng thì điều chỉnh trang hợp lệ.

Tạo tài khoản: fullName, username, email tùy chọn, role bắt buộc chọn chủ động, password, xác nhận. Edit: username chỉ đọc; fullName/email/role/active; password không nằm trong PUT. Desktop dùng sheet khoảng 520–600px; mobile dùng trang/sheet toàn chiều rộng. Tải GET user mới trước khi edit để lấy version chính xác.

Khóa tài khoản: dialog ghi rõ tên tài khoản và hệ quả mất quyền đăng nhập/token bị thu hồi. Dùng DELETE + version; mở lại bằng PUT đủ field. Đổi role hoặc khóa chính mình cần cảnh báo riêng; sau success xóa phiên hiện tại ngay. Không suy ra “admin cuối” từ danh sách đang phân trang; backend code 2006 quyết định.

409/2005: giữ draft, thông báo dữ liệu đã thay đổi, cho tải bản mới và xem lại khác biệt; không tự thay version rồi ghi đè. 409/2003: lỗi conflict ở form; backend hiện chưa chỉ rõ field trùng nên không tự đoán username hay email.

### 4.4. Inbox — `/notifications`

Desktop dùng danh sách bên trái và nội dung bên phải; mobile chuyển list → detail. Tabs “Tất cả”/“Chưa đọc” ánh xạ query unread. Item gồm subject/fallback tiêu đề thân thiện, đoạn tóm tắt, thời gian, trạng thái đã đọc; không trộn delivery status với read status.

Badge đếm bằng Page.totalElements của query `unread=true&page=0&size=1`, không đếm phần tử của trang hiện tại. Polling khởi điểm khoảng 60 giây và cấu hình tập trung, chỉ khi tab visible và có session; focus vào cửa sổ có thể refetch. Không giả là realtime/WebSocket.

Mở chi tiết GET đúng ID. Gửi mark-read sau khi đọc thành công; cập nhật UI theo response và invalidate count/list. Mất mạng thì không giả trạng thái đã đọc đã được lưu. Không có mark-all-read trong P0.

payloadJson parse an toàn, allowlist trường như message; hiển thị như text, không render HTML. Nội dung/link lạ không thực thi; deep link chỉ tới route nội bộ có support và vẫn cần authorization. ADMIN cũng chỉ đọc inbox của chính mình.

Nhắc bảo dưỡng/hết bảo hành hiển thị trong inbox theo event/message có sẵn. Chưa dựng calendar bảo dưỡng, trang chỉnh lịch 08:00 hoặc thao tác “chạy scheduler ngay” vì chưa có API UI.

### 4.5. Nhật ký — `/admin/audit`

Bảng: thời gian, hành động, actor, loại đối tượng, ID đối tượng, nút chi tiết. Filter hiện có: actor ID dương, action tối đa 100 và pagination. Label action tiếng Việt lấy từ catalog ánh xạ code; code mới có fallback hiển thị text an toàn.

Actor null thể hiện không xác định/hệ thống tùy action. API hiện chỉ trả actorUserId; không hứa có tên người thực hiện nếu chưa bổ sung projection. Không gọi GET user cho từng dòng tạo N+1 request. Có thể bổ sung resolver batch ở P1.

Chi tiết dùng sheet readonly với các cặp tên/giá trị an toàn từ afterJson, mã sự kiện và timestamp. Không có sửa/xóa audit hay nút lấy replay result. I08 replay là contract nội bộ, không phải API frontend gửi request_key tùy ý. Link sang đối tượng chỉ hiển thị khi module đích đã triển khai.

### 4.6. Báo cáo — `/admin/reports`, `/admin/reports/outstanding-invoices`

Bộ lọc ngày bắt đầu/kết thúc, preset trong policy và nút Áp dụng; không fetch theo từng phím gõ ngày. “Tải CSV” dùng đúng kỳ đã áp dụng; khi dữ liệu nguồn đổi, file có thể là snapshot mới, không hứa bằng tuyệt đối snapshot đang xem.

Nhóm “Trong kỳ”: doanh thu, tiền thu, tiền chi, giờ công, grid 4 cột desktop/2 tablet/1 mobile. Nhóm riêng “Hiện tại”: công nợ khách, công nợ nhà cung cấp, giá trị tồn kho; hiển thị snapshotAt rõ ràng. Không làm người dùng hiểu công nợ/tồn kho là tổng riêng của kỳ đã chọn.

Không thêm đường tăng trưởng, % so với kỳ trước hoặc biểu đồ tháng từ bảy tổng số hiện có. Số 0 chỉ xuất hiện khi API trả 0; đang tải/lỗi dùng skeleton/error, không dùng 0 giả.

Bảng công nợ: invoiceCode, customerId, tổng hóa đơn, đã thu, còn nợ. Tiền căn phải, format vi-VN/VND theo contract; customerId không tự biến thành tên khách khi API chưa trả tên. Không thêm nút “Thu tiền”/“Sửa hóa đơn” thuộc TV4 khi chưa tích hợp quyền và API.

Ngày chọn trên UI là ngày theo múi giờ gara. Chuyển thành UTC interval `[from, until)`, trong đó ngày kết thúc chọn inclusive trở thành đầu ngày kế tiếp. Đọc garageTimeZone/maxPeriod từ policy; so sánh Duration thực, có test DST; không dùng `new Date('YYYY-MM-DD')` để ngầm lấy múi giờ máy người dùng.

CSV tải bằng fetch có bearer header, kiểm tra status/content-type trước khi tạo Blob. Lỗi JSON không được lưu thành file .csv. Tạo filename an toàn ở client theo kỳ; API CORS hiện chỉ expose Location nên chưa đọc được Content-Disposition cross-origin nếu không bổ sung expose header. Revoke object URL sau download; không lưu báo cáo nhạy cảm vào cache offline.

## 5. Component dùng chung

### Nhóm primitive

Button, IconButton, Input, PasswordInput, Select, Checkbox, Label, FieldDescription, FieldError, Badge, Tooltip, Separator, Skeleton, Spinner, Alert, Toast, Dialog, Sheet, DropdownMenu, Tabs, Pagination. Primitive chịu trách nhiệm semantics/focus/spacing; không fetch API hay biết nghiệp vụ user.

### Nhóm pattern

- `PageHeader`: title, description, breadcrumb, primaryAction và secondaryActions.
- `FilterBar`: search, bộ lọc có thật, reset, số kết quả; đổi thứ tự hiển thị theo mobile.
- `DataTable`: column definition theo từng feature, manual pagination/sort, row identity, pending/refetch/error/empty, action cell; không thành một component với hàng chục boolean cho mọi nghiệp vụ.
- `FormField`/`FormErrorSummary`: label/required/helper/error/aria-describedby, focus lỗi đầu; field lỗi không làm nút hoặc label lân cận lệch hàng.
- `ConfirmActionDialog`: tên đối tượng, hệ quả, cancel/confirm, pending; khóa/đổi quyền không dùng confirm mơ hồ “Bạn chắc chưa?”.
- `StatusBadge`, `RoleBadge`, `DateTimeText`, `MoneyText`, `DetailField`, `QueryState`, `PermissionGate`.
- `EmptyState`: chưa có dữ liệu khác với không có kết quả tìm kiếm; mỗi trường hợp có hành động phù hợp và chỉ hiện Create nếu có quyền.
- `ErrorState`: message an toàn, Retry khi phù hợp; tuyệt đối không hiện stack/SQL/raw response.

### Mọi vùng dữ liệu phải có trạng thái

Initial loading; success có dữ liệu; empty; no-results; refreshing nhưng giữ dữ liệu trước có nhãn; 403; 404; validation; conflict; offline/timeout; 5xx. Không giữ dữ liệu của user trước khi đổi tài khoản. Khi filter đổi mà còn dữ liệu cũ trong lúc tải, ghi rõ đang cập nhật hoặc không cho thao tác lên selection cũ.

## 6. Validation: đồng bộ backend và dễ sửa lỗi

- Dùng React Hook Form + schema Zod tạo theo public policy; mode onBlur, sau lỗi revalidate onChange, submit kiểm tra toàn form. Có thể dùng validate async khi backend thực sự có endpoint tương ứng; hiện chưa có endpoint check username/email trống.
- `fullName`: required, không chỉ whitespace, tối đa 150 theo Java String length. `username`: 3–100 ký tự ASCII theo `[A-Za-z0-9._-]`, canonical lowercase. `email`: optional, blank → null ở boundary, trim/lowercase, định dạng và tối đa 254. Không thay đổi password bằng trim/case conversion.
- Password mới: minLength từ backend policy, đếm Unicode code point bằng `Array.from(value).length`; byte limit bằng `new TextEncoder().encode(value).length <= 72`. Dùng chung schema cho create/change. Confirm phải trùng; không gửi confirmPassword. Chỉ đếm .length không đủ cho mật khẩu Unicode.
- Role thuộc đúng enum năm giá trị. Active boolean, version không âm; submit PUT đủ fullName/email/role/active/version. Email null là chủ động xóa email, không để undefined tạo hiểu nhầm PATCH.
- Tất cả ID filter/route phải hợp lệ trước fetch; null/blank ID không gọi API. Chốt transport cho BIGINT/BigDecimal lớn ở mốc contract: không qua Number làm mất chính xác rồi mới format; ưu tiên decimal/id dạng string trong contract hoặc parser lossless tại adapter. UI không tự cộng/đối soát sổ tiền từ số đã làm tròn.
- Frontend hướng dẫn sớm, backend vẫn là nguồn quyết định. Không tự xây thêm regex password “chữ hoa/số/ký tự đặc biệt” khác policy hiện có.
- 2001: map error.data chỉ vào allowlist field của form; object error vào form summary. Code 2003 generic vào form-level conflict. Code 2007 vào password mới và refetch policy nếu nghi drift. Dịch message theo catalog code, có fallback an toàn cho mã mới.
- Khi sửa field, xóa server error tương ứng; không xóa lỗi toàn form nếu chưa có hành động khắc phục. Không log form values chứa mật khẩu.

## 7. Kiến trúc frontend và hooks

### 7.1. Stack đề xuất

React + TypeScript strict + Vite cho SPA nội bộ; React Router cho routing; Tailwind CSS + shadcn/ui dựa trên Radix cho primitive; TanStack Query cho dữ liệu server; TanStack Table cho bảng; React Hook Form + Zod + resolver cho form. Chốt các phiên bản tương thích, commit một npm lockfile và dùng npm ci trong CI; không ghi “latest” thành phiên bản cố định trong kế hoạch.

Vitest + Testing Library cho logic/component, MSW cho API contract scenarios, Playwright cho E2E. Chỉ thêm thư viện date/decimal khi contract và timezone yêu cầu; tránh tải nhiều thư viện cùng giải quyết một việc. Chưa cần Redux/Zustand: Context cho auth tối thiểu, Query cho server state, URL cho filters, local state cho sheet, RHF cho form.

Đây là quyết định thiết kế đề xuất cho dự án, không phải thay đổi đã cài đặt. Hướng dẫn chính thức về [phân rã component/state](https://react.dev/learn/thinking-in-react), [data table](https://ui.shadcn.com/docs/components/radix/data-table), [RHF resolver/Zod](https://github.com/react-hook-form/resolvers) và [query cancellation](https://github.com/TanStack/query/blob/main/docs/framework/react/guides/query-cancellation.md) được dùng để kiểm tra tính phù hợp. API của thư viện sẽ được đối chiếu theo phiên bản đã khóa khi triển khai.

### 7.2. Cấu trúc dự kiến

```text
frontend/
  public/
  src/
    app/
      providers/                 # router, query, auth, theme
      router/                    # routes, authenticated/role boundaries
      layouts/                   # AppShell, AuthLayout
    shared/
      api/                       # client, envelope, page adapter, ApiError
      config/                    # public runtime configuration, ui policy
      constants/                 # routes, roles, error labels, query defaults
      ui/                        # primitives
      components/                # DataTable, QueryState, FormErrorSummary
      hooks/                     # thật sự chia sẻ và có React lifecycle
      lib/                       # format/date/validation helpers thuần
      styles/                    # tokens, base styles
    features/
      auth/{api,hooks,schemas,components,pages}/
      users/{api,hooks,schemas,components,pages}/
      notifications/{api,hooks,schemas,components,pages}/
      audit/{api,hooks,components,pages}/
      reports/{api,hooks,schemas,components,pages}/
    test/                        # setup, mock handlers, fixtures
  e2e/
  package.json
  package-lock.json
```

Feature sở hữu DTO type, schema, query keys, hook, column definitions và màn hình. Shared không import ngược feature. Component không gọi fetch trực tiếp; API module không import React/UI. Không ép Entity/JpaRepository/ServiceImpl của Java lên frontend.

### 7.3. Danh mục hooks và trách nhiệm

- Auth: `useAuth`, `useLoginMutation`, `useMeQuery`, `useLogoutMutation`, `useChangePasswordMutation`. Auth có state anonymous/authenticating/authenticated/unavailable, không dùng một boolean đánh đồng loading với chưa đăng nhập.
- Users: `useUsersQuery(params)`, `useUserQuery(id)`, `useCreateUserMutation`, `useUpdateUserMutation`, `useDeactivateUserMutation`, `useUserForm(policy, mode)`. Mutations chỉ gửi DTO allowlist và invalidate đúng list/detail/me bị tác động.
- Notification: `useInboxQuery`, `useUnreadCountQuery`, `useNotificationQuery`, `useMarkReadMutation`; `useEnqueueNotificationMutation` ở P1. Query key bao gồm principal ID; markRead cập nhật đúng notice và count.
- Audit: `useAuditEventsQuery(params)`. Detail lấy từ row response khi đủ, không tự gọi endpoint chi tiết chưa có.
- Reports: `useDashboardQuery(appliedRange)`, `useOutstandingInvoicesQuery`, `useExportDashboard` với pending/error. Export là binary operation, không unwrap như JSON.
- Shared: `useUiPolicy`, `useDebouncedValue` khoảng 350ms, `useTableSearchParams`, `useUnsavedChangesGuard`. `normalizeApiError`, `applyServerErrors`, `formatMoney`, `toUtcRange` là hàm thuần khi không cần React lifecycle, không đặt tên use cho mọi helper.

Mỗi query hook quy định inputs, enabled condition, query key, AbortSignal, stale/refetch policy và safe retry. Mỗi mutation hook quy định DTO, pending lock, error mapping, invalidation và kết quả thành công. Test hành vi người dùng/cancellation/cache isolation, không test chỉ để xác nhận một hook gọi đúng hook khác.

### 7.4. API và cache

Typed fetch client đọc status và envelope `{code,message,success,data,timestamp,path}`; Page adapter chỉ phụ thuộc content/totalElements/number/size theo contract đã kiểm chứng. Không rải `.data.data` hoặc fetch interceptor trong component.

GET search truyền AbortSignal; debounce + cancellation ngăn kết quả request cũ ghi đè từ khóa mới. URL giữ keyword/active/page/size/sortBy/sortDir; password/token không ở URL. Key factory gồm filter chuẩn hóa và principal khi dữ liệu riêng tư.

GET chỉ retry có giới hạn cho network/5xx; không retry 400/401/403/404. Mutation retry=0. Khóa submit UI không thay thế idempotency server: không tự gắn requestKey rồi cho rằng API user CRUD đã hỗ trợ replay I08. Mất response sau mutation: báo chưa xác nhận, refetch trạng thái phù hợp; không tự gửi lại tạo user/đổi mật khẩu.

## 8. Bảo mật session và xử lý lỗi

- MVP giữ access token trong memory. Reload yêu cầu đăng nhập lại vì backend chưa có refresh/cookie session; trình bày đúng giới hạn này, không thêm checkbox nhớ đăng nhập. Không lưu token trong localStorage/URL hay persist Query cache.
- Nếu cần phiên bền vững, tạo một hạng mục backend refresh/BFF + HttpOnly cookie riêng, xem lại CSRF/CORS; không đổi backend bearer hiện có một cách âm thầm. Tham chiếu rủi ro lưu session identifier trong browser storage tại [OWASP HTML5 Security](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html).
- Server me là nguồn role/profile cho UI; decode JWT chỉ hỗ trợ UX expiry, không coi claim do client đọc là kiểm chứng quyền. Protected route chờ xác định phiên, không flash màn hình admin trong lúc loading.
- 401/3001 hoặc 3003 ở protected request: xử lý hết phiên một lần, cancel queries, xóa cache/session, đóng form bí mật, đưa về login. Nhiều request lỗi đồng thời không tạo nhiều toast/redirect.
- 401/3005 là sai credential ở login hoặc sai password hiện tại: hiện lỗi form, không mặc định logout một phiên hợp lệ chỉ vì đổi mật khẩu nhập sai.
- 403: trang/thao tác không đủ quyền, giữ session. 404: đối tượng không tồn tại, quay về list. 409/2005: reload/review version; 2006: last admin; 2010: xung đột thao tác, không tự overwrite. 503 ở token lookup không bị hiểu là token hết hạn: giữ session và cho thử lại.
- Logout/change-password thành công: clear session + private cache. Có thể dùng BroadcastChannel chỉ báo sự kiện logout giữa tab, không broadcast token. Logout mất mạng vẫn clear local; thông báo chưa xác nhận thu hồi trên server, không hứa token khác đã bị vô hiệu.
- Không render HTML từ notification/audit/server error. Không console log auth headers/password/response token. Không có logging form payload hoặc analytics capture input bí mật.
- Mutation auth không persist variables; cấu hình gcTime/reset phù hợp để giảm thời gian giữ password trong mutation state, clear form sau hoàn tất và tắt devtools dữ liệu nhạy cảm ở production.
- Base URL và public UI config được validate ở startup; không bao giờ có signing key/DB password/SMS secret trong biến VITE_* hay bundle frontend. CORS origin triển khai phải nằm trong allowlist backend.

## 9. Contract cần chốt trước triển khai

### DEP-01 — Public UI policy (P0, đã triển khai)

Đã bổ sung API read-only `GET /api/v1/public/ui-policy`: 7 giới hạn công khai lấy từ typed configuration, GET anonymous được phép, method khác vẫn được bảo vệ. HTTP test xác nhận policy tùy chỉnh và không có trường bí mật.

Chỉ trả thông tin công khai cần thiết: password minLength/maxUtf8Bytes, pagination default/max/allowed sizes, report maxPeriodSeconds, garageTimeZone. Không trả secret, connection string, token/gateway credential hay chi tiết vận hành nhạy cảm. Nếu thêm API public cần khai báo security matcher, typed response, cache/version policy và test giá trị lấy từ configuration.

Không tải được policy: login vẫn hoạt động với validation credential cơ bản; tạo user/đổi password/báo cáo dùng trạng thái lỗi cấu hình có Retry, không âm thầm fallback 12/100/366 rồi hứa đồng bộ backend.

### DEP-02 — Khả năng hiển thị tên và số chính xác

Audit hiện có actorUserId, debt có customerId; ưu tiên hiển thị ID rõ ràng ở P0, tên cần projection/lookup batch của module sở hữu. Đã dùng lossless-json để giữ Long/version/BigDecimal dạng string tại boundary và Decimal.js để hiển thị số tiền chính xác; có kiểm thử số vượt giới hạn an toàn JavaScript.

### DEP-03 — Thông báo thủ công và vận hành

Admin send dùng stable eventKey trong cùng draft/retry; thay payload sau trạng thái chưa xác nhận phải xử lý như ý định mới sau đối soát, tránh cùng key khác payload gây 409. P0 không có admin console outbox; provider capability và customer lookup thuộc P1. Deep link thông báo cần hợp đồng route/resource với TV2–TV4.

Các gap khác không chặn P0: time-series report, role filter, audit date filter, mark-all-read, refresh token và Content-Disposition expose nếu muốn tên file từ server.

## 10. Kế hoạch triển khai theo mốc

Ước lượng để lập kế hoạch: một lập trình viên toàn thời gian, backend TV1 hiện có, có người phản hồi UX/API. Khoảng 17 ngày công thực hiện + 3 ngày dự phòng, tương đương khoảng 4 tuần làm việc. Không tính thời gian chờ TV2–TV4 hoặc provision provider. Ước lượng sẽ cập nhật sau DEP-01/02, không phải cam kết deadline.

### Mốc A — Chốt contract và thiết kế: 1 ngày

- [x] FE-01: ghi ADR stack/version/session; chốt DEP-01/02, nguồn timezone/policy và naming route.
- [x] FE-02: wireframe login/users/inbox/audit/reports/account; chốt tokens, trạng thái và breakpoints.
- Đầu ra: contract checklist và bộ component mẫu. Nghiệm thu: không có nút/filter dựa trên API chưa tồn tại trong P0.

### Mốc B — Foundation và shell: 3 ngày

- [x] FE-03: tạo frontend Vite/TS strict, alias, lint/format, scripts, lockfile và test setup.
- [x] FE-04: primitive/pattern, app shell, router, responsive layout, ErrorBoundary và 403/404.
- [x] FE-05: typed API client, Query provider, Page adapter, error catalog, MSW và public policy loading.
- Nghiệm thu: component có keyboard/focus/disabled/pending/error; app build/typecheck đạt; không có credential trong bundle.

### Mốc C — Auth/session: 2 ngày

- [x] FE-06: login/me, route/role guards, safe returnTo, logout và cache isolation.
- [x] FE-07: trang account/change password; Unicode/byte validation, 3005 không logout nhầm, success revokes session.
- Nghiệm thu: login cả năm role; direct URL bị chặn đúng; nhiều 401 xử lý một lần; 503 không xóa phiên.

### Mốc D — Tài khoản: 3 ngày

- [x] FE-08: users table, URL filters, debounce/abort, sort/page phía server, loading/empty/error.
- [x] FE-09: create/edit sheet, allowlist DTO, fresh version, conflict/last-admin, deactivate/reactivate và self-change.
- Nghiệm thu: không submit lặp, không ghi đè stale version, null email đúng contract; unauthorized không gọi mutation.

### Mốc E — Inbox và audit: 2,5 ngày

- [x] FE-10: private inbox/detail/read/count, polling có pause/backoff, payload text an toàn.
- [x] FE-11: audit list/filter/detail readonly, action labels, actor null, unknown action và metadata safe.
- Nghiệm thu: inbox A/B tách biệt kể cả ADMIN; count là tổng thật; lỗi markRead không giả thành success.

### Mốc F — Báo cáo: 2 ngày

- [x] FE-12: date range/preset, UTC interval/policy, KPI kỳ và snapshot hiện tại riêng biệt.
- [x] FE-13: debt table, số tiền, binary CSV và errors; không có biểu đồ giả từ dữ liệu không đủ.
- Nghiệm thu: đúng timezone/end-exclusive/maxDuration, export lỗi JSON không tải thành CSV; non-admin không truy cập.

### Mốc G — QA, tích hợp và bàn giao: 3,5 ngày

- [ ] FE-14: hoàn thiện component/integration/E2E + accessibility theo rủi ro; xử lý race/cache/offline.
- [ ] FE-15: tích hợp API thật trên DB test, kiểm tra desktop/tablet/mobile và refresh direct route; build production/reverse proxy/CORS.
- [x] FE-16: tài liệu setup, .env.example công khai, route/component/hook conventions, checklist nghiệm thu và demo flow cho nhóm.
- Nghiệm thu: các cổng chất lượng ở mục 11 đạt; lỗi còn lại có bằng chứng và owner; không đánh dấu T01–T38 xuyên module đã xong chỉ vì TV1 frontend chạy được.

P1 được lên estimate riêng sau P0; không lồng thêm outbox console, dashboard chart hay role builder vào các mốc trên.

## 11. Kiểm thử và tiêu chí hoàn thành

### Unit/schema/util

Password ASCII/Unicode/code point/72 byte boundary, confirm mismatch, email blank → null, login không áp min mới, ID/version/range invalid, ngày qua tháng/năm/DST, chính xác số lớn, error mapping theo code, safe returnTo, query key isolation.

### Component/integration với API mô phỏng

Field và object errors, unknown error code, 409 stale giữ draft, last admin, pending chống double click, keyboard/focus dialog, 320/360px không tràn trang, abort request cũ, pagination reset, 403 và 503 đúng UX, JSON/payload độc hại chỉ hiện như text, CSV endpoint trả lỗi.

### E2E với backend thật và dữ liệu test riêng

1. Login thành công/thất bại cho năm role; admin vs non-admin route và API.
2. Create user, trùng dữ liệu, edit, email null, deactivate/reactivate.
3. Hai phiên mở cùng user: stale version không overwrite; admin cuối không bị khóa/hạ quyền.
4. Self-demotion/self-deactivate đổi session đúng; sai current password không logout, đổi đúng bắt login lại.
5. Inbox A/B/private admin, markRead/count và logout/đổi tài khoản không rò cache.
6. Audit filter/detail chỉ đọc; không xuất password/hash/token/replay payload.
7. Report số liệu có fixture thật, phân biệt kỳ/snapshot; timezone/maxDuration và CSV có bearer.
8. Network timeout, 401 đồng thời, 403, 409, 500/503; không tự gửi lại mutation chưa biết kết quả.

### Cổng chất lượng

- [ ] Lint, TypeScript check, unit/component tests, production build và E2E trọng yếu đạt; ghi kết quả thực tế và artifacts.
- [ ] Không lỗi console, không request ngoài quyền, không dữ liệu mẫu lẫn dữ liệu API, không secret trong bundle/log/storage.
- [ ] Loading/empty/error/success có ở từng màn hình; keyboard, focus, nhãn và tương phản đạt mục tiêu WCAG 2.2 AA qua kiểm tra tự động và thủ công. Target 44px là lựa chọn UX cho touch; không diễn giải sai 44px là mức tối thiểu của mọi tiêu chí AA. [W3C target size](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html).
- [ ] Kiểm tra 320/360/768/1024/1440px và zoom 200%; cuộn bảng chỉ nằm trong vùng bảng; form không mất nút Save hoặc che field lỗi.
- [ ] Mỗi PR gắn FE ticket, hành vi, validation, test và ảnh desktop/mobile phù hợp; không tách PR khiến UI phụ thuộc contract chưa có.
- [ ] Có hướng dẫn chạy frontend/backend, route fallback SPA, CORS và giới hạn session; đội TV2–TV4 có thể dùng cùng primitives/auth/error hooks.

## 12. Lịch sử bản kế hoạch và trạng thái hiện tại

Đã đối chiếu phạm vi và contract từ source; đã lập thiết kế, backlog, hooks/validation, gap API và tiêu chí nghiệm thu. Phác thảo tương tác trong cuộc trò chuyện chỉ minh họa bố cục và hành vi, dùng account mẫu, không gọi backend. Chưa tạo ứng dụng React, cài dependency, thay API hoặc tuyên bố các bài frontend đã chạy đạt.

Phác thảo đã kiểm tra trên trình duyệt: lỗi required/username/role/password, tạo tài khoản mẫu và cập nhật danh sách, chuyển hồ sơ/lưu ở phương án thứ hai; không có console error được ghi nhận trong các thao tác này. Ở viewport 360px, vùng nội dung và form không tràn ngang. Đây là kiểm chứng prototype, không phải nghiệm thu E2E/frontend production.


P0 đã triển khai và tích hợp API thật ngày 15/10/2026. FE-14/15 và các cổng QA giữ mở cho kiểm tra manual accessibility toàn diện, zoom 200%, production reverse proxy/hosting và trình duyệt khác Chromium; phần đã chạy được ghi cụ thể trong [TV1_FRONTEND_VALIDATION.md](TV1_FRONTEND_VALIDATION.md). Những mô tả chưa triển khai ở trên là lịch sử prototype trước task này, không còn là trạng thái ứng dụng hiện tại.
