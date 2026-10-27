# Keycloak: đăng ký, đăng nhập và thu hồi phiên

## Form đăng nhập và đăng ký trên AutoGarage

`/login` nhận username hoặc email (bao gồm Gmail) và mật khẩu riêng của tài khoản
AutoGarage. Frontend gửi password grant đến Keycloak qua proxy `/realms`, sau đó
gọi `/api/v1/auth/me` bằng bearer token. Không điều hướng sang trang Keycloak.
Client `autogarage-frontend` cần bật **Direct Access Grants** cho luồng này.
Luồng này được chọn theo trải nghiệm yêu cầu; không hỗ trợ đầy đủ các bước MFA/
challenge của luồng browser. Nếu sau này cần MFA, chuyển sang Authorization Code
+ PKCE thay vì tự bỏ qua bước xác thực.

`POST /api/v1/auth/register` là endpoint công khai với DTO chỉ gồm username,
fullName, email, password. Username bắt buộc, dùng 3–100 chữ/số ASCII và dấu chấm,
gạch dưới/gạch ngang theo cùng hợp đồng tài khoản nhân viên; backend chuẩn hóa
username và email về chữ thường. Backend kiểm tra PasswordPolicy rồi gọi Keycloak Admin API để tạo danh
tính **CUSTOMER**, emailVerified=false. Không liên kết/nâng quyền tài khoản trùng.
Không lưu mật khẩu vào database gara; sau khi đăng ký, frontend tự đăng nhập,
và `/auth/me` dùng cơ chế JIT sẵn có để tạo User/Customer trong gara. Nếu tạo vai
trò Keycloak thất bại, adapter cố xóa danh tính vừa tạo; lỗi hạ tầng trả 503.
`registrationAllowed=false` tắt form đăng ký công khai của Keycloak; Admin API
vẫn tạo tài khoản qua backend, nên không thể bỏ qua PasswordPolicy bằng form cũ.

Giữ `registrationEmailAsUsername=false` và `loginWithEmailAllowed=true`. Username
của khách hàng đăng ký mới do khách hàng tự chọn, độc lập với email. Tài khoản
cũ đã dùng email làm username vẫn giữ nguyên, không đổi tên hoặc reset mật khẩu.
Đăng nhập chấp nhận username hoặc email. Thiếu/sai username trả 400/code 2001;
trùng username hoặc email trả 409/code 2003, không liên kết/nâng quyền danh tính
cũ. Bật Email as username cho cả realm sẽ khiến Keycloak đổi tên
tài khoản khi Admin API tạo/sửa profile, làm sai luồng quản trị nhân viên.
`keycloak-setup` đồng bộ các thiết lập này cho realm đã tồn tại, không reset
password/volume. Không đưa credential quản trị Keycloak vào frontend.

## Khôi phục phiên sau refresh

Sau password grant hoặc PKCE, frontend gửi refresh token một lần đến
`POST /api/v1/auth/session`, kèm Bearer của cùng người dùng. Backend đổi token
với Keycloak, kiểm tra JWT và subject khớp rồi đặt cookie phiên `AG_REFRESH`:
HttpOnly, SameSite=Strict, Path=/api/v1/auth/session, không có Domain/Max-Age.
Response chỉ có accessToken, expiresIn, tokenType trong ApiResponse; refresh
token không xuất hiện trong body. Các API nghiệp vụ vẫn dùng Bearer.

`POST /api/v1/auth/session/refresh` nhận cookie, đổi token và xoay cookie mới.
Ứng dụng chờ bước này và `/auth/me` trước khi render router, giữ nguyên đường dẫn
đang xem. StrictMode dùng chung request khởi tạo; Web Locks tuần tự hóa xoay
cookie giữa các tab trên trình duyệt hỗ trợ. Môi trường không có Web Locks chỉ
tuần tự trong cùng trang; dùng một tab khi provider bật refresh-token rotation
không cho reuse để tránh các tab cũ tranh nhau làm mới.

`DELETE /api/v1/auth/session` xóa cookie và kết thúc phiên Keycloak, kể cả khi
không còn access token. Logout ứng dụng vẫn dùng `/auth/logout` để thu hồi
auth_version. Cookie lỗi/hết hạn trả 401 và được xóa; lỗi provider/DB trả 503 và
giữ phiên để thử lại. Frontend lưu marker ý định logout không chứa token để
không khôi phục nhầm cookie khi người dùng đã logout trong lúc mất mạng.

Mọi POST/DELETE của session bridge yêu cầu Origin khớp allowlist
`AUTH_CORS_ORIGINS` (bao gồm request cùng origin); thiếu Origin cũng bị từ chối.
Chỉ các endpoint session cho CORS credentials, các API Bearer khác giữ cấu hình
không credentials. Không đưa token vào localStorage/sessionStorage.

- `KEYCLOAK_TOKEN_URI`: endpoint token backend truy cập được; mặc định từ issuer.
  Docker dùng hostname nội bộ `keycloak`, không dùng localhost trong container.
- `KEYCLOAK_BROWSER_CLIENT_ID`: mặc định autogarage-frontend.
- `AUTH_SESSION_REQUEST_TIMEOUT`: mặc định 10s, phải dương.
- `AUTH_SESSION_COOKIE_SECURE`: mặc định true khi triển khai HTTPS. Chỉ đặt false
  cho HTTP local; compose local đã đặt rõ false. Không thay đổi timeout/lifetime
  phiên của Keycloak; cookie phiên không tự tạo cơ chế "nhớ đăng nhập vĩnh viễn".

Google OAuth để sau; nhập địa chỉ Gmail vào form hiện tại không đăng nhập Google
và không sử dụng mật khẩu Google. OTP/link/xác minh email cần SMTP của Keycloak,
chưa bật trong bộ local này.

Backend yêu cầu `KEYCLOAK_ADMIN_SERVER_URL`, `KEYCLOAK_ADMIN_REALM`,
`KEYCLOAK_ADMIN_USERNAME` và `KEYCLOAK_ADMIN_PASSWORD` khi
`KEYCLOAK_ADMIN_ENABLED=true` (mặc định). Cấu hình thiếu dừng khởi động; lỗi Admin
API trả HTTP 503 / code 4002 thay vì báo thành công nhưng bỏ qua đồng bộ. URL và realm
lấy từ `KEYCLOAK_ADMIN_SERVER_URL` và `KEYCLOAK_ADMIN_REALM`. Khi backend chạy trong
container, URL Admin API phải dùng hostname nội bộ của Keycloak; issuer công khai
của access token vẫn phải khớp `KEYCLOAK_ISSUER_URI`.

`KEYCLOAK_ADMIN_CONNECT_TIMEOUT` mặc định 5s, `KEYCLOAK_ADMIN_REQUEST_TIMEOUT`
mặc định 10s; cả hai phải dương và connect không vượt request. `KEYCLOAK_ADMIN_ENABLED=false`
chỉ dành cho test dùng issuer giả. Môi trường chạy ứng dụng thật cần bật adapter để
cấp tài khoản, đồng bộ `auth_version` và kết thúc phiên Keycloak khi logout.

## Realm mới và quản trị viên đầu tiên

Import `docker/keycloak/realm-export.json` cho realm mới. File không chứa tài khoản
hay mật khẩu quản trị viên gara dùng chung. Trong Keycloak Admin Console:

1. Tạo tài khoản quản trị viên gara với mật khẩu riêng; gán duy nhất role gara `ADMIN`.
   Gỡ role mặc định có composite `CUSTOMER` khỏi tài khoản nhân sự để token không
   chứa hai role gara. Các tài khoản đăng ký công khai chỉ nhận `CUSTOMER`.
2. Lấy ID của tài khoản đó làm `AUTH_BOOTSTRAP_IDENTITY_ID`; đặt
   `AUTH_BOOTSTRAP_USERNAME`, `AUTH_BOOTSTRAP_FULL_NAME`, `AUTH_BOOTSTRAP_ENABLED=true`
   rồi khởi động backend. Bootstrap không ghi đè hoặc nâng quyền tài khoản sẵn có.
3. Sau khi khởi tạo, tắt `AUTH_BOOTSTRAP_ENABLED`.

Tài khoản nhân sự tạo qua ứng dụng cần mật khẩu ban đầu theo `PasswordPolicy` dùng
chung (mặc định ít nhất 12 ký tự). Nếu Keycloak đã có cùng username, ứng dụng trả
409 thay vì tự liên kết/nâng quyền tài khoản đó. Muốn liên kết một danh tính có sẵn,
quản trị viên phải cung cấp `identityId` rõ ràng trong yêu cầu tạo tài khoản.

## Realm đã tồn tại

`--import-realm` không cập nhật realm đã có. Cập nhật các cấu hình sau bằng Admin
Console hoặc Admin REST API trước khi sử dụng bản code này:

- Trong client `autogarage-frontend`, thêm mapper `autogarage-auth-version` từ
  `protocolMappers` của file realm: User Attribute `auth_version`, Token Claim Name
  `auth_version`, JSON Type `long`, Multivalued Off, Add to access token On.
- Trong Realm Settings → User Profile, thêm thuộc tính `auth_version` với **view và
  edit chỉ dành cho admin**, không bắt buộc; giữ Unmanaged attributes Disabled.
  Cấu hình đầy đủ của profile mới nằm trong `components` của file realm. Không cho
  người dùng sửa thuộc tính này qua Account Console/API.
- Đối chiếu `users.auth_version` ở database gara với thuộc tính `auth_version` của
  từng danh tính Keycloak đã liên kết. Tài khoản chưa có thuộc tính dùng version 0;
  tài khoản đã từng logout/đổi quyền cần được đồng bộ về version hiện tại trước lần
  đăng nhập tiếp theo. Không đặt lại database version về 0 vì sẽ làm token cũ hợp lệ.
- Kiểm tra redirect URI và web origin của client khớp URL frontend thực tế. Google
  IdP tắt trong template. Luồng Google OAuth trong giao diện để triển khai sau;
  khi triển khai cần OAuth client thật và secret tại Keycloak rồi bật IdP.
  Giao diện hiện tại không hiển thị nút Google. Không dùng các placeholder trong
  template làm credential hoạt động.

Với Keycloak chạy riêng, frontend cần đặt `VITE_KEYCLOAK_PUBLIC_ORIGIN` thành URL
realm đầy đủ mà trình duyệt truy cập được, ví dụ local
`http://localhost:8081/realms/autogarage-realm` cho các helper PKCE nếu dùng.
Form hiện tại dùng password grant qua `VITE_KEYCLOAK_ISSUER=/realms/autogarage-realm`
và proxy `/realms`, không mở giao diện Keycloak. Khôi phục phiên/làm mới token
đi qua session bridge của backend được mô tả ở trên.

Chỉ dùng đường dẫn tương đối cho `VITE_KEYCLOAK_PUBLIC_ORIGIN` khi đã có reverse
proxy cùng origin hoàn chỉnh, chuyển tiếp cả `/realms` và tài nguyên `/resources`,
đồng thời public hostname của Keycloak khớp URL bên ngoài. Proxy `/realms` đơn lẻ
của Vite không bảo đảm đầy đủ luồng điều hướng, tài nguyên giao diện và cookie của
Keycloak; tránh trộn `localhost` với `127.0.0.1` giữa các bước của cùng luồng login.

Logout và thay đổi role/active tăng version cục bộ, đồng bộ thuộc tính Keycloak,
và kết thúc các refresh session cũ. Access token cũ bị API từ chối; token của lần
đăng nhập mới mang version mới. Không bỏ mapper: nếu thiếu mapper, token mới luôn
thiếu version và bị từ chối sau lần thu hồi đầu tiên.

Database gara và Keycloak không có transaction phân tán. Khi một bước Admin API
đã thành công nhưng bước tiếp theo hoặc commit database thất bại, trạng thái bên
Keycloak có thể đã thay đổi; cần đối chiếu role/active/version và danh tính đã tạo
trước khi thử lại. HTTP 503 không có nghĩa mọi side effect bên Keycloak đã rollback.

Đăng ký trùng username của một customer nội bộ chưa liên kết không chiếm tài khoản
đó; hệ thống tạo danh tính customer riêng. Liên kết hồ sơ khách hàng theo email chỉ
được thực hiện khi token xác nhận **chính email đang liên kết** đã verified.

## Kiểm tra vận hành

Trên môi trường thử nghiệm riêng, xác nhận đăng ký → `/auth/me`, đăng nhập bằng
PKCE, logout → token cũ bị từ chối → đăng nhập lại thành công; tiếp tục đổi role,
khóa/mở khóa và đăng nhập lại. Kiểm tra khách hàng không sửa được `auth_version`.

Unit test dùng HTTP provider giả kiểm tra đồng bộ thuộc tính, thu hồi refresh
session, lỗi 503, username collision và phân quyền role. Đây không thay thế kiểm
tra import/SMTP/Google/PKCE trên một Keycloak thật.

Tham khảo chính thức: [Protocol mappers](https://www.keycloak.org/admin-api/protocol-mappers)
và [User profile](https://www.keycloak.org/docs/latest/server_admin/index.html).
