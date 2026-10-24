# Keycloak: đăng ký, đăng nhập và thu hồi phiên

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
  IdP tắt trong template. Muốn sử dụng, cấu hình OAuth client thật và secret qua môi
  trường Keycloak, bật Google IdP trong Admin Console và đặt
  `VITE_GOOGLE_LOGIN_ENABLED=true` cho frontend. Không dùng các placeholder trong
  template làm credential hoạt động.

Với Keycloak chạy riêng, frontend cần đặt `VITE_KEYCLOAK_PUBLIC_ORIGIN` thành URL
realm đầy đủ mà trình duyệt truy cập được, ví dụ local
`http://localhost:8081/realms/autogarage-realm`. Đăng nhập và đăng ký sẽ điều hướng
trực tiếp tới URL này. `VITE_KEYCLOAK_ISSUER=/realms/autogarage-realm` vẫn có thể dùng
proxy `/realms` của Vite để gửi yêu cầu token/refresh/logout từ frontend.

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
