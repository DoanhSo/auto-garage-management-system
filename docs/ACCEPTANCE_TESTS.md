# Kế hoạch nghiệm thu T01–T38

Mỗi Txx bám UCxx trong báo cáo. Các bài bên dưới là kế hoạch triển khai nghiệp vụ, chưa phải kiểm thử tự động đã chạy. `MigrationSchemaTests` chỉ kiểm thử nền tảng CSDL và các ràng buộc.

## T01 UC01 — Đăng nhập

- **Chủ trì:** TV1.
- **Chuẩn bị:** Tài khoản người dùng đã được tạo trong hệ thống và đang ở trạng thái hoạt động (is_active = true).
- **Thao tác:** Mở trang đăng nhập, nhập tên đăng nhập (username hoặc email) và mật khẩu, nhấn Đăng nhập.; Tiếp nhận dữ liệu, chuẩn hóa định danh và tìm kiếm bản ghi trong bảng users.; So khớp mật khẩu đã mã hóa bằng thuật toán băm (BCrypt) với password_hash.; Kiểm tra trạng thái tài khoản (is_active).; Khởi tạo JWT / phiên đăng nhập, nạp role duy nhất và trả về thông tin người dùng an toàn (không lộ mật khẩu).; Hệ thống chuyển hướng người dùng đến Dashboard tương ứng với vai trò của họ.
- **Kết quả cần đạt:** Người dùng đăng nhập thành công, phiên làm việc được thiết lập, giao diện điều hướng hiển thị đúng theo vai trò.
- **Trường hợp bị từ chối:** Hệ thống từ chối truy cập, ghi nhận nhật ký thất bại, không cấp phiên xác thực.
- **Ngoại lệ:** Hệ thống hiển thị thông báo lỗi chung 'Tên đăng nhập hoặc mật khẩu không chính xác', không tiết lộ cụ thể trường nào sai. Cho phép nhập lại.; Hệ thống từ chối đăng nhập và thông báo 'Tài khoản đã bị tạm khóa. Vui lòng liên hệ quản lý (ADMIN)'.
- **Quy tắc:** BR01 (5 vai trò), BR02 (Kiểm soát quyền), BR23 (Audit).

## T02 UC02 — Đăng xuất

- **Chủ trì:** TV1.
- **Chuẩn bị:** Người dùng đang có phiên đăng nhập hợp lệ trong hệ thống.
- **Thao tác:** Chọn chức năng 'Đăng xuất' từ menu hồ sơ cá nhân.; Xác thực token/phiên hiện tại cùng mã CSRF.; Đưa token vào danh sách thu hồi (blacklist) hoặc hủy session trên server.; Ghi nhận sự kiện đăng xuất vào nhật ký hệ thống (audit_events).; Xóa sạch cookie và trạng thái đăng nhập phía client, điều hướng về màn hình đăng nhập.
- **Kết quả cần đạt:** Phiên làm việc phía server bị thu hồi, cookie/token phía client bị xóa, chuyển về trang đăng nhập.
- **Trường hợp bị từ chối:** Thông báo lỗi nếu kết nối gián đoạn, nhưng vẫn xóa thông tin xác thực lưu ở trình duyệt.
- **Ngoại lệ:** Hệ thống tự động dọn dẹp bộ nhớ cục bộ phía client và chuyển hướng về màn hình đăng nhập.
- **Quy tắc:** BR02 (Kiểm soát quyền), BR23 (Audit).

## T03 UC03 — Quản lý tài khoản và vai trò

- **Chủ trì:** TV1.
- **Chuẩn bị:** ADMIN đã đăng nhập, có quyền quản trị toàn hệ thống.
- **Thao tác:** Mở danh sách tài khoản, chọn 'Tạo tài khoản' hoặc chọn một tài khoản để chỉnh sửa.; Nhập thông tin: username, full_name, email, chọn role (ADMIN, RECEPTIONIST, TECHNICIAN, WAREHOUSE_STAFF, CUSTOMER), mật khẩu ban đầu.; Kiểm tra tính duy nhất của username và email; băm mật khẩu bằng BCrypt.; Lưu thông tin vào bảng users và ghi nhận sự kiện audit_events.; Hiển thị thông báo thành công và cập nhật lại danh sách.
- **Kết quả cần đạt:** Tài khoản được tạo mới hoặc cập nhật trạng thái/vai trò thành công, ghi vết audit.
- **Trường hợp bị từ chối:** Dữ liệu không hợp lệ bị từ chối, không thay đổi trạng thái tài khoản.
- **Ngoại lệ:** Hệ thống báo lỗi cụ thể trường bị trùng, yêu cầu đổi giá trị khác.; ADMIN chọn 'Khóa tài khoản' (is_active = false) thay vì xóa bản ghi vật lý để bảo toàn khóa ngoại lịch sử.
- **Quy tắc:** BR01 (Chỉ 5 role chuẩn), BR02 (Phân quyền đối tượng), BR23 (Audit), BR24 (Bảo toàn lịch sử không xóa cứng).

## T04 UC04 — Quản lý hồ sơ khách hàng

- **Chủ trì:** TV2.
- **Chuẩn bị:** Nhân viên đã đăng nhập và có quyền quản lý hồ sơ khách hàng.
- **Thao tác:** Nhập số điện thoại khách hàng để tra cứu trên hệ thống.; Tìm kiếm trong bảng customers; nếu chưa có, mở biểu mẫu tạo mới hồ sơ.; Nhập họ tên, điện thoại, email, địa chỉ, loại khách (INDIVIDUAL/BUSINESS), công ty, MST, hạn nợ, hạn mức nợ.; Kiểm tra định dạng số điện thoại, gán mã khách hàng tự sinh (customer_code), lưu vào bảng customers.; Trả về thông tin hồ sơ khách hàng để tiếp tục quy trình tiếp nhận.
- **Kết quả cần đạt:** Hồ sơ khách hàng được lưu trữ với mã khách hàng duy nhất; sẵn sàng tiếp nhận hoặc tạo xe.
- **Trường hợp bị từ chối:** Thông báo lỗi nếu sai định dạng điện thoại hoặc vi phạm dữ liệu.
- **Ngoại lệ:** Hệ thống hiển thị hồ sơ hiện tại và danh sách xe đã đăng ký; Lễ tân có thể cập nhật thông tin nếu có thay đổi.; Hệ thống tự động liên kết user_id của tài khoản mới với hồ sơ khách hàng (BR03).
- **Quy tắc:** BR02 (Bảo mật hồ sơ), BR03 (Khách không bắt buộc có user), BR04 (Khách hàng hiện tại vs lịch sử), BR19 (Hạn mức tín dụng).

## T05 UC05 — Quản lý phương tiện

- **Chủ trì:** TV2.
- **Chuẩn bị:** Khách hàng chủ sở hữu đã có hồ sơ trong bảng customers.
- **Thao tác:** Tra cứu biển số xe trên hệ thống.; Xác nhận biển số chưa tồn tại; mở biểu mẫu thêm mới xe.; Nhập thông tin: biển số, VIN, số máy, hãng, model, năm sản xuất, màu, loại nhiên liệu, ODO hiện tại, hạn đăng kiểm.; Chuẩn hóa chuỗi biển số (bỏ dấu cách, viết hoa), kiểm tra tính hợp lệ của mốc đăng kiểm.; Lưu thông tin vào bảng vehicles với customer_id của chủ xe hiện tại.; Hiển thị thông tin xe trong danh sách phương tiện của khách hàng.
- **Kết quả cần đạt:** Xe được lưu vào hệ thống với biển số chuẩn hóa duy nhất và gắn đúng customer_id.
- **Trường hợp bị từ chối:** Báo lỗi trùng biển số hoặc VIN không hợp lệ.
- **Ngoại lệ:** Lễ tân cập nhật customer_id mới trong bảng vehicles. Hệ thống giữ nguyên customer_id lịch sử trong các phiếu sửa chữa và hóa đơn cũ (BR04).; Hệ thống cảnh báo bất thường số km để Lễ tân đối chiếu thực tế trước khi xác nhận.
- **Quy tắc:** BR02 (Quyền sở hữu xe), BR04 (Chủ xe hiện tại vs Lịch sử giao dịch).

## T06 UC06 — Quản lý dịch vụ và gói

- **Chủ trì:** TV2.
- **Chuẩn bị:** Người dùng đăng nhập với quyền ADMIN.
- **Thao tác:** Mở danh mục dịch vụ, chọn 'Thêm dịch vụ' hoặc 'Thêm gói bảo dưỡng'.; Nhập: mã dịch vụ, tên, loại (SINGLE/PACKAGE), đơn giá tiền công, thời gian chuẩn (phút), loại khoang yêu cầu, kỹ năng yêu cầu (JSON), chu kỳ km/tháng, điều kiện bảo hành.; Kiểm tra tính duy nhất của service_code, giá tiền không âm, các mốc chu kỳ hợp lệ.; Lưu vào bảng services; cập nhật trạng thái is_active = true.; Hiển thị dịch vụ mới trên bảng giá hệ thống.
- **Kết quả cần đạt:** Dịch vụ/gói bảo dưỡng được lưu trữ và có hiệu lực cho các báo giá/lịch hẹn mới.
- **Trường hợp bị từ chối:** Báo lỗi nếu trùng mã dịch vụ hoặc giá âm.
- **Ngoại lệ:** ADMIN chuyển is_active = false. Dịch vụ không hiển thị khi lập báo giá mới nhưng vẫn giữ nguyên trong các phiếu sửa chữa lịch sử (BR24).
- **Quy tắc:** BR05 (Cấu hình dịch vụ/gói), BR18 (Giá snapshot không đổi khi danh mục đổi), BR24 (Bảo toàn dữ liệu).

## T07 UC07 — Đặt lịch hẹn

- **Chủ trì:** TV2.
- **Chuẩn bị:** Xe đã đăng ký trong hệ thống; gara có cấu hình giờ làm việc và khoang hoạt động.
- **Thao tác:** Chọn xe của mình, chọn dịch vụ chính (hoặc nhập mô tả nhu cầu) và chọn ngày giờ hẹn đến.; Tính toán thời gian dự kiến kết thúc dựa trên standard_minutes của dịch vụ.; Khóa hàng tài nguyên khoang (Pessimistic lock) và quét tình trạng trống trong khung giờ [starts_at, ends_at).; Tạo bản ghi trong bookings (status = PENDING/CONFIRMED).; Tạo bản ghi giữ chỗ trong bay_reservations (reservation_type = 'BOOKING', status = 'HELD', có expires_at).; Tạo thông báo trong notifications cho khách hàng và Lễ tân.; Nhận mã đặt lịch và thông báo xác nhận thành công.
- **Kết quả cần đạt:** Lịch hẹn được tạo ở trạng thái PENDING/CONFIRMED, tạm giữ khoang trong bay_reservations.
- **Trường hợp bị từ chối:** Thông báo hết khoang hoặc xung đột giờ hẹn, yêu cầu chọn khung giờ khác.
- **Ngoại lệ:** Hệ thống thông báo khung giờ đã kín và gợi ý 3 khung giờ gần nhất còn khoang trống phù hợp.; Lễ tân chọn khách hàng, chọn xe và thao tác đặt lịch thay mặt khách hàng.
- **Quy tắc:** BR05 (Một dịch vụ chính), BR06 (Không trùng khoang), BR07 (Giải phóng tài nguyên), BR23 (Thông báo).

## T08 UC08 — Xác nhận dời hoặc hủy lịch

- **Chủ trì:** TV2.
- **Chuẩn bị:** Lịch hẹn đang ở trạng thái PENDING hoặc CONFIRMED.
- **Thao tác:** Mở danh sách lịch hẹn, chọn lịch cần xử lý (Xác nhận / Dời lịch / Hủy lịch).; Kiểm tra phiên bản dữ liệu (optimistic locking) và trạng thái hiện tại của booking.; Chọn thao tác (nếu Dời lịch: chọn ngày giờ mới; nếu Hủy: nhập lý do hủy).; Thực hiện giao dịch nguyên tử: cập nhật booking.status; giải phóng bay_reservations cũ; nếu dời lịch thì kiểm tra và tạo reservation mới.; Gửi thông báo cập nhật lịch hẹn cho khách hàng qua notifications.; Nhận thông báo cập nhật thành công.
- **Kết quả cần đạt:** Lịch hẹn cập nhật trạng thái mới (CONFIRMED/CANCELLED/NO_SHOW); khoang được giải phóng hoặc dời nguyên tử.
- **Trường hợp bị từ chối:** Khung giờ mới bị trùng khoang hoặc lịch đã tiếp nhận không thể hủy tùy tiện.
- **Ngoại lệ:** Lễ tân chuyển trạng thái booking sang NO_SHOW, hệ thống tự động giải phóng khoang giữ chỗ.; Khách hàng chọn 'Hủy lịch hẹn' trước giờ hẹn ít nhất 2 tiếng, hệ thống tự động giải phóng khoang.
- **Quy tắc:** BR06 (Khóa khoang), BR07 (Giải phóng tài nguyên nguyên tử), BR23 (Thông báo).

## T09 UC09 — Tiếp nhận xe và hiện trạng

- **Chủ trì:** TV2.
- **Chuẩn bị:** Khách hàng và xe đã có trong hệ thống (hoặc tạo nhanh tại chỗ).
- **Thao tác:** Nhập biển số xe tiếp nhận; nếu xe có đặt lịch trước, chọn mã booking tương ứng.; Tải thông tin xe và hồ sơ chủ xe; xác thực booking thuộc đúng xe đó.; Nhập ODO tiếp nhận, mức nhiên liệu (%), ghi nhận phản ánh sự cố của khách hàng.; Thực hiện kiểm tra hiện trạng xe theo biểu mẫu checklist (vết trầy xước, đèn, lốp, đồ đạc trên xe) và chụp ảnh tải lên.; Tạo bản ghi repair_orders (status = 'NEW', received_by = Lễ tân, customer_id chốt snapshot).; Lưu checklist vào vehicle_checks (check_type = 'CHECK_IN') và lưu metadata ảnh vào attachments.; Nếu có booking, cập nhật booking.status = 'RECEIVED'.; In biên bản tiếp nhận bàn giao cho khách hàng ký xác nhận.
- **Kết quả cần đạt:** Bản ghi repair_orders được tạo ở trạng thái NEW; lưu checklist tiếp nhận và tệp ảnh đính kèm.
- **Trường hợp bị từ chối:** Báo lỗi nếu thiếu số ODO, sai xe hoặc trùng lặp phiếu đang mở.
- **Ngoại lệ:** Lễ tân tiếp nhận với booking_id = NULL. Hệ thống vẫn tạo repair_orders và kiểm tra hiện trạng đầy đủ (BR08).
- **Quy tắc:** BR03 (Khách không cần user), BR04 (Chốt khách lịch sử), BR08 (Tạo phiếu từ tiếp nhận), BR16 (Checklist & Ảnh).

## T10 UC10 — Điều phối xe đến khoang

- **Chủ trì:** TV3.
- **Chuẩn bị:** Phiếu sửa chữa đang mở; khoang dự kiến còn trống và không bảo trì.
- **Thao tác:** Mở sơ đồ mặt bằng khoang sửa chữa thời gian thực.; Chọn xe/phiếu sửa chữa và chọn khoang đích (loại khoang phù hợp: cầu nâng, điện, đồng sơn...).; Khóa bản ghi khoang; kiểm tra điều kiện không bị giao nhau với lịch khác.; Tạo bản ghi bay_reservations (reservation_type = 'REPAIR', status = 'IN_USE', repair_order_id chốt).; Giải phóng khoang trước đó nếu xe di chuyển giữa các khoang.; Thông báo cho Kỹ thuật viên lái xe vào khoang chỉ định.
- **Kết quả cần đạt:** Tạo hoặc cập nhật bay_reservations loại 'REPAIR' liên kết đúng repair_order_id; khoang chuyển 'IN_USE'.
- **Trường hợp bị từ chối:** Báo lỗi xung đột khoang hoặc khoang đang bảo trì.
- **Ngoại lệ:** Hệ thống chặn điều phối và cảnh báo khoang ở trạng thái MAINTENANCE.
- **Quy tắc:** BR06 (Không trùng khoang), BR07 (Giải phóng tài nguyên), BR24 (Audit điều phối).

## T11 UC11 — Phân công kỹ thuật viên

- **Chủ trì:** TV3.
- **Chuẩn bị:** Task tồn tại; nhân viên đang có ca làm việc hoạt động (employee_shifts) và đủ kỹ năng.
- **Thao tác:** Mở danh sách công việc cần phân công của phiếu sửa chữa.; Gợi ý danh sách nhân viên kỹ thuật phù hợp theo kỹ năng (skills_json) và ca làm việc (employee_shifts).; Chọn nhân viên, chọn vai trò (MAIN, ASSISTANT, QC), nhập thời gian dự kiến bắt đầu/kết thúc.; Kiểm tra ràng buộc: một task chỉ có tối đa 1 phân công MAIN đang active (BR09).; Lưu vào bảng task_assignments với is_active = true.; Gửi thông báo công việc mới đến Kỹ thuật viên qua kênh WEB (notifications).
- **Kết quả cần đạt:** Bản ghi task_assignments được tạo hợp lệ; chỉ duy nhất 1 thợ chính active cho mỗi task.
- **Trường hợp bị từ chối:** Báo lỗi nếu nhân viên không có ca, thiếu kỹ năng hoặc trùng 2 thợ chính.
- **Ngoại lệ:** Lễ tân thu hồi phân công cũ (đặt is_active = false) và tạo phân công mới. Các labor_sessions cũ vẫn được bảo lưu (BR24).
- **Quy tắc:** BR09 (Điều kiện phân công & 1 Thợ chính), BR10 (Timer), BR24 (Bảo toàn lịch sử phân công).

## T12 UC12 — Ghi chẩn đoán kỹ thuật

- **Chủ trì:** TV3.
- **Chuẩn bị:** Kỹ thuật viên được phân công công việc kiểm tra của phiếu sửa chữa.
- **Thao tác:** Mở phiếu sửa chữa trên giao diện làm việc của KTV.; Nhập kết quả chẩn đoán: hiện tượng, nguyên nhân gốc rễ, mức độ rủi ro, đề xuất công việc và phụ tùng cần thay.; Tải kèm hình ảnh/video bằng chứng vị trí hỏng hóc (nếu có).; Cập nhật trường repair_orders.diagnosis; lưu tệp vào attachments.; Ghi nhận sự kiện vào audit_events và thông báo cho Lễ tân lập báo giá.
- **Kết quả cần đạt:** Nội dung chẩn đoán được lưu vào repair_orders.diagnosis; ghi vết audit.
- **Trường hợp bị từ chối:** Từ chối nếu người thực hiện không được phân công hoặc phiếu đã đóng.
- **Ngoại lệ:** Kỹ thuật viên ghi chú bổ sung; hệ thống kích hoạt điểm mở rộng cho Use Case UC15 (Lập báo giá phát sinh).
- **Quy tắc:** BR02 (Quyền KTV), BR08 (Hồ sơ phiếu), BR12 (Căn cứ lập báo giá).

## T13 UC13 — Lập và gửi báo giá

- **Chủ trì:** TV3.
- **Chuẩn bị:** Phiếu sửa chữa đang mở; danh mục dịch vụ và phụ tùng có sẵn giá hiện hành.
- **Thao tác:** Mở phiếu sửa chữa, chọn 'Tạo báo giá' (estimate_kind = 'INITIAL').; Thêm từng dòng báo giá: chọn loại SERVICE (tiền công) hoặc PART (phụ tùng thay thế), nhập số lượng, đơn giá chốt.; Nhập mức giảm giá (discount_amount) và thuế (tax_amount) cấp chứng từ.; Khóa phiếu sửa chữa, cấp version_no tự tăng cho phiếu; tính line_total từng dòng và total_amount của báo giá.; Lưu estimates và estimate_items; chuyển status = 'SENT', ghi nhận sent_at.; Gửi thông báo qua WEB/Email kèm đường dẫn duyệt trực tuyến đến Khách hàng.
- **Kết quả cần đạt:** Báo giá (estimates) và các dòng (estimate_items) được tạo ở trạng thái SENT; gửi thông báo duyệt cho khách.
- **Trường hợp bị từ chối:** Báo lỗi nếu số lượng <= 0, giảm giá vượt tổng tiền hoặc dòng thiếu định danh.
- **Ngoại lệ:** Hệ thống đánh dấu phiên bản cũ là SUPERSEDED và tạo phiên bản mới với version_no kế tiếp (BR12).
- **Quy tắc:** BR12 (Phiên bản báo giá), BR13 (Bóc tách dòng Service/Part), BR18 (Tính toán tiền tệ), BR23 (Thông báo).

## T14 UC14 — Khách duyệt từng hạng mục

- **Chủ trì:** TV3.
- **Chuẩn bị:** Báo giá đang ở trạng thái SENT và thuộc đúng khách hàng sở hữu xe.
- **Thao tác:** Mở liên kết báo giá từ thông báo hoặc ứng dụng.; Xác thực danh tính chủ sở hữu xe và tải phiên bản báo giá mới nhất.; Xem xét từng dòng và chọn: Đồng ý (APPROVED) hoặc Từ chối (REJECTED), nhập lý do nếu từ chối.; Nhấn 'Xác nhận phê duyệt'.; Khóa bản ghi báo giá; ghi decided_by, decided_at cho từng dòng.; Cập nhật trạng thái báo giá: APPROVED (nếu duyệt hết) hoặc PARTIALLY_APPROVED (duyệt 1 phần).; Với mỗi dòng SERVICE được APPROVED: tự động sinh 1 bản ghi repair_tasks.; Với mỗi dòng PART được APPROVED: kích hoạt giữ hàng trong kho (UC23).; Gửi thông báo cho Lễ tân và Kỹ thuật viên để bắt đầu triển khai.
- **Kết quả cần đạt:** Báo giá cập nhật status (APPROVED / PARTIALLY_APPROVED / REJECTED); dòng APPROVED tự động sinh task và giữ phụ tùng.
- **Trường hợp bị từ chối:** Báo lỗi nếu phiên bản báo giá đã bị thay thế hoặc người duyệt không phải chủ xe.
- **Ngoại lệ:** Báo giá chuyển REJECTED; Lễ tân liên hệ tìm phương án khác hoặc đóng phiếu trả xe.
- **Quy tắc:** BR02 (Quyền duyệt), BR12 (Version), BR13 (Chỉ APPROVED mới sinh việc), BR14 (Đồng bộ tồn giữ), BR23 (Audit).

## T15 UC15 — Lập báo giá phát sinh

- **Chủ trì:** TV3.
- **Chuẩn bị:** Phiếu sửa chữa đang ở trạng thái IN_PROGRESS; báo giá ban đầu đã được duyệt.
- **Thao tác:** Mở phiếu sửa chữa, chọn 'Lập báo giá phát sinh'.; Thiết lập estimate_kind = 'SUPPLEMENTAL' và gán parent_estimate_id trỏ về báo giá trước đó.; Nhập các hạng mục dịch vụ và phụ tùng phát sinh kèm hình ảnh/lý do kỹ thuật.; Tính toán subtotal, thuế, tổng tiền cho riêng phần bổ sung; lưu estimates và estimate_items mới.; Chuyển status = 'SENT' và gửi thông báo yêu cầu khách hàng duyệt phần phát sinh.; Thực hiện duyệt theo quy trình UC14 đối với riêng các hạng mục phát sinh.
- **Kết quả cần đạt:** Báo giá mới loại SUPPLEMENTAL được tạo, trỏ parent_estimate_id về báo giá gốc, sẵn sàng gửi khách duyệt.
- **Trường hợp bị từ chối:** Từ chối nếu parent_estimate_id không thuộc cùng phiếu sửa chữa.
- **Ngoại lệ:** Chỉ hủy các hạng mục bổ sung; các công việc đã duyệt ở báo giá gốc vẫn tiếp tục thực hiện.
- **Quy tắc:** BR12 (Báo giá bổ sung có parent cùng phiếu), BR13 (Duyệt theo dòng), BR23 (Audit).

## T16 UC16 — Bắt đầu công việc và timer

- **Chủ trì:** TV3.
- **Chuẩn bị:** KTV có phân công active cho task; KTV không có phiên timer nào khác đang chạy.
- **Thao tác:** Mở danh sách 'Công việc của tôi', chọn công việc được giao và nhấn 'Bắt đầu'.; Kiểm tra quyền phân công trong task_assignments.; Kiểm tra ràng buộc active_employee_id: đảm bảo KTV không có phiên nào đang mở (ends_at IS NULL) (BR10).; Lấy thời gian chuẩn UTC từ máy chủ làm starts_at; tạo bản ghi mới trong labor_sessions.; Cập nhật repair_tasks.status = 'IN_PROGRESS' (nếu đang PENDING).; Hiển thị đồng hồ đếm thời gian thực trên màn hình KTV.
- **Kết quả cần đạt:** Tạo bản ghi labor_sessions với ends_at = NULL; task chuyển IN_PROGRESS; đồng hồ bắt đầu chạy.
- **Trường hợp bị từ chối:** Chặn lại nếu KTV đang có timer ở công việc khác chưa dừng.
- **Ngoại lệ:** Hệ thống sử dụng cơ chế chống trùng request_key để trả về phiên hiện tại, không tạo bản ghi đúp.
- **Quy tắc:** BR09 (Đúng người được giao), BR10 (Mỗi KTV chỉ chạy tối đa 1 timer tại một thời điểm), BR11 (Giờ công máy chủ).

## T17 UC17 — Tạm dừng công việc

- **Chủ trì:** TV3.
- **Chuẩn bị:** Đang có bản ghi labor_sessions mở (ends_at IS NULL) của chính KTV đó.
- **Thao tác:** Nhấn nút 'Tạm dừng' trên giao diện đếm giờ công việc.; Chọn lý do tạm dừng: PAUSE (nghỉ ngơi/chuyển việc) hoặc WAITING_PARTS (chờ phụ tùng).; Lấy thời gian hiện tại từ server ghi vào ends_at; gán stop_reason tương ứng.; Cập nhật active_employee_id = NULL để giải phóng cờ bận cho KTV.; Nếu lý do là WAITING_PARTS, cập nhật repair_tasks.status = 'WAITING_PARTS'.; Dừng đồng hồ và hiển thị tổng thời gian đã tích lũy của công việc.
- **Kết quả cần đạt:** Đóng phiên làm việc hiện tại, ghi ends_at và stop_reason; không tính thời gian nghỉ vào giờ công.
- **Trường hợp bị từ chối:** Báo lỗi nếu không có phiên đang chạy.
- **Ngoại lệ:** Hệ thống ghi nhận thời điểm yêu cầu từ client và đồng bộ đối soát với server khi có mạng lại.
- **Quy tắc:** BR10 (Đóng timer), BR11 (Thời gian tạm dừng bị loại khỏi giờ công tính lương/hiệu suất).

## T18 UC18 — Tiếp tục công việc

- **Chủ trì:** TV3.
- **Chuẩn bị:** Task đang ở trạng thái PAUSED hoặc WAITING_PARTS; KTV không có timer mở khác.
- **Thao tác:** Mở công việc đang tạm dừng, nhấn 'Tiếp tục'.; Kiểm tra KTV không có timer mở khác.; Tạo bản ghi labor_sessions mới với starts_at = giờ server, ends_at = NULL.; Cập nhật repair_tasks.status = 'IN_PROGRESS'.; Đồng hồ hiển thị tiếp tục chạy, cộng dồn các khoảng làm việc trước đó.
- **Kết quả cần đạt:** Tạo phiên labor_sessions mới độc lập; nối tiếp tích lũy giờ công.
- **Trường hợp bị từ chối:** Báo lỗi nếu KTV đang bận ở công việc khác.
- **Ngoại lệ:** Thợ mới được phân công (task_assignments) có thể bấm tiếp tục; hệ thống tạo labor_sessions gắn employee_id của thợ mới.
- **Quy tắc:** BR10 (Chỉ 1 timer), BR11 (Tổng giờ công là tổng độ dài các phiên labor_sessions).

## T19 UC19 — Hoàn thành công việc

- **Chủ trì:** TV3.
- **Chuẩn bị:** Task đang IN_PROGRESS; đã hoàn tất các thao tác kỹ thuật.
- **Thao tác:** Nhấn 'Hoàn thành công việc' trên giao diện.; Nhập ghi chú kỹ thuật, kết quả đo đạc (nếu có).; Đóng timer hiện tại của KTV với stop_reason = 'COMPLETE'.; Kiểm tra xem còn thợ phụ nào đang mở timer trên task này không; nếu không còn, chuyển repair_tasks.status = 'COMPLETED', ghi completed_at.; Kiểm tra nếu toàn bộ task trong phiếu đã COMPLETED: cập nhật repair_orders.status = 'QC_PENDING'.; Gửi thông báo cho KTV có quyền QC để tiến hành nghiệm thu chất lượng.
- **Kết quả cần đạt:** Đóng timer cuối cùng với stop_reason = 'COMPLETE'; task chuyển COMPLETED; cập nhật completed_at.
- **Trường hợp bị từ chối:** Báo lỗi nếu còn phụ tùng chưa xuất đủ hoặc KTV khác vẫn đang chạy timer trên task.
- **Ngoại lệ:** Hệ thống chỉ đóng timer của KTV thực hiện và nhắc nhở thợ phụ cần đóng phiên trước khi chốt task hoàn thành.
- **Quy tắc:** BR11 (Chốt giờ công), BR17 (Phải xong task mới được QC), BR24 (Bảo toàn dữ liệu).

## T20 UC20 — Quản lý nhà cung cấp

- **Chủ trì:** TV4.
- **Chuẩn bị:** Người dùng đăng nhập với quyền Quản lý kho hoặc ADMIN.
- **Thao tác:** Mở danh mục Nhà cung cấp, chọn 'Thêm mới'.; Nhập thông tin: mã NCC, tên công ty, số điện thoại, email, địa chỉ, MST, người liên hệ, số ngày được nợ (payment_term_days).; Kiểm tra tính duy nhất của supplier_code; kiểm tra payment_term_days >= 0.; Lưu vào bảng suppliers; hiển thị trên danh sách đối tác.
- **Kết quả cần đạt:** Bản ghi suppliers được lưu trữ với supplier_code duy nhất.
- **Trường hợp bị từ chối:** Báo lỗi nếu trùng mã nhà cung cấp hoặc sai định dạng MST.
- **Ngoại lệ:** Đổi is_active = false. Không xóa cứng bản ghi để bảo toàn lịch sử hóa đơn mua và sổ kho (BR24).
- **Quy tắc:** BR20 (Chi nhà cung cấp), BR24 (Bảo toàn lịch sử quan hệ).

## T21 UC21 — Quản lý phụ tùng vật tư

- **Chủ trì:** TV4.
- **Chuẩn bị:** Người dùng có quyền quản lý kho hàng.
- **Thao tác:** Mở danh mục Phụ tùng, chọn 'Khai báo phụ tùng'.; Nhập: mã SKU (part_code), tên, mã OEM, thương hiệu, nhà sản xuất, đơn vị tính (cái/bộ/lít...), NCC ưu tiên, mã vị trí kệ kho (location_code), giá vốn chuẩn, giá bán niêm yết, ngưỡng cảnh báo tồn (min_stock_quantity), thời hạn bảo hành.; Kiểm tra tính duy nhất của part_code; kiểm tra ràng buộc purchase_price >= 0, sale_price >= 0.; Khởi tạo tồn kho stock_quantity = 0, reserved_quantity = 0; lưu vào bảng parts.; Hiển thị phụ tùng mới trong danh mục kho.
- **Kết quả cần đạt:** Phụ tùng được lưu vào bảng parts với tồn kho ban đầu = 0, sẵn sàng cho nhập kho.
- **Trường hợp bị từ chối:** Báo lỗi nếu trùng mã part_code hoặc giá bán âm.
- **Ngoại lệ:** Cập nhật trường sale_price trong parts. Giá này áp dụng cho các báo giá mới, không làm thay đổi giá trên các báo giá và hóa đơn cũ (BR18).
- **Quy tắc:** BR14 (Tồn thực & Tồn giữ), BR18 (Snapshot giá), BR24 (Bảo toàn danh mục).

## T22 UC22 — Nhập kho mua hàng hoặc đầu kỳ

- **Chủ trì:** TV4.
- **Chuẩn bị:** Nhà cung cấp và danh mục phụ tùng đã được khai báo.
- **Thao tác:** Chọn 'Lập phiếu nhập kho' (document_type = 'IN').; Chọn nhà cung cấp, nhập số hóa đơn NCC (supplier_invoice_number), ngày hóa đơn, hạn thanh toán.; Thêm các dòng phụ tùng: chọn SKU, nhập số lượng thực nhận, đơn giá mua (unit_cost).; Tính subtotal, thuế, tổng tiền chứng từ nhập; lưu ở trạng thái DRAFT.; Kiểm đếm xong và nhấn 'Ghi sổ nhập kho' (POST).; Bắt đầu transaction CSDL: Khóa các dòng parts liên quan theo thứ tự id.; Cập nhật parts.stock_quantity += quantity; cập nhật giá vốn bình quân gia quyền purchase_price.; Tạo các bản ghi biến động trong inventory_transactions (transaction_type = 'IN').; Chuyển chứng từ sang POSTED, ghi posted_by, posted_at.; Ghi nhận công nợ phải trả NCC tương ứng với total_amount của chứng từ.
- **Kết quả cần đạt:** Chứng từ inventory_documents chuyển POSTED; tạo inventory_transactions; tăng parts.stock_quantity; ghi nhận công nợ NCC.
- **Trường hợp bị từ chối:** Báo lỗi nếu số lượng <= 0, trùng số hóa đơn NCC hoặc post chứng từ 2 lần.
- **Ngoại lệ:** Chọn is_opening_balance = true, supplier_id = NULL. Hệ thống tăng tồn kho nhưng không tạo khoản công nợ phải trả NCC (BR20).
- **Quy tắc:** BR14 (Tăng tồn), BR15 (Ghi sổ 1 lần duy nhất), BR20 (Nợ NCC chỉ sinh từ IN mua có supplier).

## T23 UC23 — Giữ phụ tùng sau khi khách duyệt

- **Chủ trì:** TV4.
- **Chuẩn bị:** Dòng báo giá được duyệt; phụ tùng có trong kho.
- **Thao tác:** Nhận sự kiện dòng báo giá phụ tùng được APPROVED.; Khóa bản ghi phụ tùng trong bảng parts.; Tính tồn khả dụng = stock_quantity - reserved_quantity.; Nếu tồn khả dụng >= số lượng cần: tăng parts.reserved_quantity += quantity và estimate_items.reserved_quantity = quantity trong cùng giao dịch.; Ghi log audit_events về việc giữ kho thành công.
- **Kết quả cần đạt:** estimate_items.reserved_quantity và parts.reserved_quantity tăng tương ứng; tồn khả dụng giảm an toàn.
- **Trường hợp bị từ chối:** Thông báo thiếu hàng nếu tồn khả dụng không đủ đáp ứng.
- **Ngoại lệ:** Hệ thống giữ tối đa lượng đang có (nếu chính sách cho phép), ghi nhận số lượng thiếu và gửi thông báo cảnh báo 'Cần nhập bổ sung phụ tùng' cho Quản lý kho.
- **Quy tắc:** BR14 (Đồng bộ tuyệt đối giữa parts.reserved_quantity và tổng estimate_items.reserved_quantity).

## T24 UC24 — Xuất phụ tùng cho sửa chữa

- **Chủ trì:** TV4.
- **Chuẩn bị:** Hạng mục phụ tùng đã được duyệt (APPROVED) và có phiếu sửa chữa hợp lệ.
- **Thao tác:** Nhập mã phiếu sửa chữa, chọn các hạng mục phụ tùng cần xuất kho.; Kiểm tra số lượng xuất <= số lượng khách duyệt trên estimate_items.; Xác nhận tạo chứng từ xuất kho (document_type = 'OUT').; Khóa các bản ghi parts liên quan.; Trừ tồn kho: stock_quantity -= qty; trừ lượng giữ: reserved_quantity -= qty.; Trừ estimate_items.reserved_quantity -= qty tương ứng.; Tạo bản ghi biến động trong inventory_transactions (transaction_type = 'OUT').; Tạo hoặc cộng dồn vào bảng repair_order_parts (chốt số lượng ròng, đơn giá bán, đơn giá vốn lịch sử).; Chuyển chứng từ sang POSTED và in phiếu xuất kho cho KTV ký nhận.
- **Kết quả cần đạt:** Tạo chứng từ OUT đã POSTED; giảm stock_quantity và reserved_quantity; ghi nhận chi phí vào repair_order_parts.
- **Trường hợp bị từ chối:** Từ chối xuất nếu vượt quá số lượng được duyệt hoặc tồn kho không đủ.
- **Ngoại lệ:** Quản lý kho có thể xuất trước một phần số lượng; phần còn lại tiếp tục được giữ trong kho cho đến đợt xuất tiếp theo.
- **Quy tắc:** BR13 (Chỉ xuất dòng APPROVED), BR14 (Trừ đồng bộ tồn và giữ), BR15 (Ghi sổ OUT 1 lần), BR18 (Snapshot giá vốn).

## T25 UC25 — Trả phụ tùng chưa sử dụng

- **Chủ trì:** TV4.
- **Chuẩn bị:** Đã có chứng từ xuất kho OUT trước đó; hóa đơn khách chưa phát hành.
- **Thao tác:** Mở chức năng 'Trả phụ tùng', chọn phiếu sửa chữa và dòng phụ tùng nguồn.; Kiểm tra số lượng còn có thể trả = tổng đã xuất - tổng đã trả trước đó.; Nhập số lượng trả lại kho và lý do trả; nhấn 'Xác nhận trả kho'.; Tạo chứng từ RETURN (source_document_id trỏ về chứng từ xuất cũ).; Khóa bản ghi parts: tăng lại stock_quantity += return_qty.; Tạo bản ghi biến động trong inventory_transactions (transaction_type = 'RETURN').; Cập nhật giảm repair_order_parts.quantity -= return_qty (nếu về 0 thì giữ nguyên dòng với quantity = 0 để bảo toàn dấu vết lịch sử).; Chuyển chứng từ RETURN sang POSTED.
- **Kết quả cần đạt:** Tạo chứng từ RETURN đã POSTED; tăng lại stock_quantity; giảm số lượng ròng trong repair_order_parts.
- **Trường hợp bị từ chối:** Từ chối nếu số lượng trả vượt quá số lượng đã xuất hoặc hóa đơn đã chốt.
- **Ngoại lệ:** Hệ thống chặn chức năng trả kho trực tiếp; yêu cầu quy trình xử lý ngoại lệ theo chính sách tài chính.
- **Quy tắc:** BR14 (Tăng lại tồn), BR15 (Ghi sổ RETURN), BR18 (Lượng ròng trong repair_order_parts), BR24 (Giữ dòng số lượng 0).

## T26 UC26 — Điều chỉnh tồn kho

- **Chủ trì:** TV4.
- **Chuẩn bị:** Người dùng có quyền kho; có biên bản kiểm kê và lý do chênh lệch rõ ràng.
- **Thao tác:** Tạo phiếu điều chỉnh tồn kho, chọn phụ tùng cần điều chỉnh.; Nhập số lượng chênh lệch, chọn chiều biến động (ADJUSTMENT_IN nếu thừa, ADJUSTMENT_OUT nếu thiếu/hỏng), nhập đơn giá vốn và lý do chi tiết.; Kiểm tra ràng buộc: nếu ADJUSTMENT_OUT thì stock_quantity mới không được < reserved_quantity (BR14).; Quản lý kiểm tra tính hợp lý của lý do và phê duyệt chứng từ.; Khóa parts: cập nhật lại stock_quantity; tạo inventory_transactions tương ứng; chuyển chứng từ sang POSTED.; Ghi nhận sự kiện kiểm toán nghiêm ngặt vào audit_events.
- **Kết quả cần đạt:** Tạo chứng từ ADJUSTMENT_IN hoặc ADJUSTMENT_OUT đã POSTED; tồn kho khớp số kiểm kê thực tế.
- **Trường hợp bị từ chối:** Từ chối nếu số điều chỉnh làm tồn kho âm hoặc nhỏ hơn lượng đang giữ (reserved_quantity).
- **Ngoại lệ:** Hệ thống từ chối cho đến khi các phiếu đang giữ linh kiện được hủy hoặc điều chỉnh giảm lượng giữ tương ứng.
- **Quy tắc:** BR14 (0 <= reserved <= stock), BR15 (Ghi sổ kiểm kê), BR24 (Bảo toàn lịch sử audit).

## T27 UC27 — Kiểm tra chất lượng và yêu cầu làm lại

- **Chủ trì:** TV3.
- **Chuẩn bị:** Tất cả repair_tasks đã hoàn thành, không còn timer nào đang mở; KTV thực hiện có can_qc = true.
- **Thao tác:** Mở phiếu sửa chữa đang chờ nghiệm thu chất lượng.; Kiểm tra thực tế các hạng mục trên xe theo biểu mẫu checklist_json chuyên dụng cho QC.; Đánh giá từng hạng mục và chọn kết luận chung: PASSED hoặc FAILED; nhập ghi chú chi tiết.; Lưu vào bảng vehicle_checks (check_type = 'QC', attempt_no tự tăng cho lần kiểm tra này).; Cập nhật repair_orders.latest_qc_check_id trỏ về bản ghi QC mới nhất này (BR17).; Nếu PASSED: chuyển repair_orders.status = 'COMPLETED', cho phép Lễ tân lập hóa đơn.; Nếu FAILED: chuyển repair_orders.status = 'QC_FAILED', gửi thông báo yêu cầu thợ chính kiểm tra và làm lại.
- **Kết quả cần đạt:** Tạo vehicle_checks loại QC; nếu PASS: cập nhật latest_qc_check_id và mở khóa lập hóa đơn; nếu FAIL: yêu cầu làm lại.
- **Trường hợp bị từ chối:** Người kiểm tra không đủ thẩm quyền QC hoặc biểu mẫu checklist chưa đánh giá đủ các mục bắt buộc.
- **Ngoại lệ:** Người QC mở kiểm tra lại; hệ thống tạo attempt_no = 2 mới, không ghi đè lần kiểm tra thất bại trước đó.
- **Quy tắc:** BR09 (Thẩm quyền can_qc), BR16 (Checklist JSON), BR17 (QC PASSED mới nhất là điều kiện tiên quyết cho hóa đơn và bàn giao).

## T28 UC28 — Lập và phát hành hóa đơn khách

- **Chủ trì:** TV4.
- **Chuẩn bị:** repair_orders.status = 'COMPLETED'; latest_qc_check_id là PASSED; chưa có hóa đơn trước đó.
- **Thao tác:** Mở phiếu sửa chữa đã hoàn thành, chọn 'Lập hóa đơn'.; Kiểm tra điều kiện: latest_qc_check_id đạt PASSED, không còn timer mở, không có phụ tùng xuất dở dang (BR17).; Tự động gom toàn bộ repair_tasks (loại SERVICE) và repair_order_parts có quantity > 0 (loại PART) của đúng phiếu đó.; Kiểm tra bảng tính tiền, nhập mức giảm giá hoặc thuế suất nếu có.; Khóa phiếu sửa chữa; chụp toàn bộ dữ liệu khách hàng (tên, MST, địa chỉ) vào customer_snapshot và xe vào vehicle_snapshot.; Tạo bản ghi duy nhất trong invoices (invoice_code tự sinh, status = 'UNPAID').; Tạo các dòng snapshot trong invoice_items (chốt tên mục, số lượng, đơn giá bán, đơn giá vốn lịch sử).; Cập nhật repair_orders.status = 'INVOICED' và hiển thị bản in hóa đơn chuẩn.
- **Kết quả cần đạt:** Bản ghi invoices và các dòng invoice_items được tạo; chốt snapshot thông tin khách và xe; status = 'UNPAID'.
- **Trường hợp bị từ chối:** Từ chối nếu QC chưa đạt, còn công việc đang mở hoặc phiếu đã phát hành hóa đơn rồi.
- **Ngoại lệ:** Hệ thống tự động đánh dấu invoice.status = 'PAID' mà không tạo bản ghi thanh toán payments giá trị 0 (BR18).
- **Quy tắc:** BR17 (QC trước hóa đơn), BR18 (Snapshot bất biến dữ liệu hóa đơn), BR19 (Thu khách).

## T29 UC29 — Ghi nhận thanh toán khách

- **Chủ trì:** TV4.
- **Chuẩn bị:** Hóa đơn đang ở trạng thái UNPAID hoặc PARTIALLY_PAID; tiền đã vào tài khoản/két gara.
- **Thao tác:** Tra cứu hóa đơn của khách hàng, xem số tiền còn phải thanh toán.; Thực hiện thanh toán bằng tiền mặt, thẻ, chuyển khoản hoặc VietQR.; Xác nhận tiền đã vào tài khoản gara, nhập số tiền thanh toán, chọn phương thức và nhập mã tham chiếu giao dịch.; Khóa bản ghi hóa đơn (Pessimistic lock); kiểm tra: 0 < amount <= số tiền còn nợ (BR19).; Tạo bản ghi trong payments (direction = 'IN', invoice_id = ID hóa đơn, transaction_code tự sinh duy nhất).; Tính toán lại tổng đã thu; nếu đã thu đủ total_amount: cập nhật invoices.status = 'PAID'; nếu thu một phần: cập nhật 'PARTIALLY_PAID'.; In phiếu thu cho khách hàng và gửi thông báo xác nhận thanh toán.
- **Kết quả cần đạt:** Tạo bản ghi payments (direction = 'IN'); cập nhật trạng thái hóa đơn sang PARTIALLY_PAID hoặc PAID.
- **Trường hợp bị từ chối:** Từ chối nếu số tiền <= 0, thu vượt quá số dư còn nợ hoặc sai mã hóa đơn.
- **Ngoại lệ:** Khóa hàng CSDL đảm bảo giao dịch thứ hai chỉ được thu tối đa số dư nợ còn lại sau giao dịch thứ nhất.
- **Quy tắc:** BR19 (Thu tiền dương, không thu vượt nợ, direction = IN), BR20 (Phân biệt rạch ròi thu khách và chi NCC).

## T30 UC30 — Thanh toán nhà cung cấp

- **Chủ trì:** TV4.
- **Chuẩn bị:** Chứng từ nhập kho (document_type = 'IN') đã POSTED, có supplier_id, không phải tồn đầu kỳ, còn dư nợ.
- **Thao tác:** Mở sổ theo dõi công nợ nhà cung cấp, chọn chứng từ nhập kho cần thanh toán.; Kiểm tra quyền ADMIN và tính toán số tiền còn nợ của chứng từ (total_amount - tổng payments OUT đã trả).; Nhập số tiền chi trả, chọn phương thức (chuyển khoản ngân hàng) và mã giao dịch ngân hàng.; Khóa chứng từ nhập kho; kiểm tra 0 < amount <= số dư nợ còn lại.; Tạo bản ghi trong payments (direction = 'OUT', inventory_document_id = ID chứng từ, invoice_id = NULL).; Cập nhật sổ công nợ nhà cung cấp và ghi vết kiểm toán vào audit_events.; In phiếu chi lưu hồ sơ kế toán.
- **Kết quả cần đạt:** Tạo bản ghi payments (direction = 'OUT'); giảm công nợ phải trả NCC.
- **Trường hợp bị từ chối:** Từ chối nếu nhân viên Lễ tân thực hiện (sai quyền) hoặc chi vượt số tiền nợ trên chứng từ.
- **Ngoại lệ:** ADMIN nhập số tiền trả đợt 1; số nợ còn lại tiếp tục được theo dõi cho các đợt thanh toán tiếp theo.
- **Quy tắc:** BR01 (Chỉ ADMIN được chi tiền), BR19 (Khoản chi dương), BR20 (payments OUT gắn chứng từ mua IN đã POSTED).

## T31 UC31 — Bàn giao xe và đóng phiếu

- **Chủ trì:** TV3.
- **Chuẩn bị:** QC đạt PASSED; hóa đơn đã phát hành; khách hàng đã thanh toán đủ (hoặc được bảo lãnh nợ trong hạn mức).
- **Thao tác:** Kiểm tra điều kiện bàn giao: QC mới nhất đạt PASSED, tình trạng thanh toán của hóa đơn.; Cùng khách hàng kiểm tra lại xe và điền biên bản bàn giao (checklist_json loại 'HANDOVER').; Ký nhận bàn giao xe và đồ đạc nguyên vẹn.; Nhấn 'Xác nhận bàn giao & Đóng phiếu' trên hệ thống.; Cập nhật repair_orders: handed_over_by, handed_over_at, closed_at, status = 'CLOSED'.; Giải phóng tài nguyên khoang sửa chữa liên quan trong bay_reservations (chuyển RELEASED).; Quét các dòng invoice_items có chính sách bảo hành; tự động tạo các bản ghi tương ứng trong bảng warranties (chụp thời hạn, mốc km, điều kiện).; Gửi tin nhắn cảm ơn và sổ bảo hành điện tử cho khách hàng qua notifications.
- **Kết quả cần đạt:** repair_orders chuyển CLOSED; giải phóng khoang; kích hoạt chính sách bảo hành trong warranties.
- **Trường hợp bị từ chối:** Từ chối nếu còn nợ vượt hạn mức tín dụng hoặc chưa có xác nhận bàn giao.
- **Ngoại lệ:** Hệ thống kiểm tra tổng nợ của khách hàng không vượt credit_limit và trong payment_term_days; nếu thỏa mãn thì cho phép bàn giao xe (BR19).
- **Quy tắc:** BR07 (Giải phóng tài nguyên), BR17 (QC trước bàn giao), BR19 (Điều kiện tài chính), BR21 (Chụp chính sách bảo hành).

## T32 UC32 — Tiếp nhận yêu cầu bảo hành

- **Chủ trì:** TV3.
- **Chuẩn bị:** Hạng mục sửa chữa có hợp đồng bảo hành còn hiệu lực trong bảng warranties.
- **Thao tác:** Mở danh sách 'Bảo hành của tôi', chọn hạng mục phụ tùng/dịch vụ cần yêu cầu bảo hành.; Hiển thị thông tin bảo hành gốc: ngày bắt đầu, ngày hết hạn, số km bảo hành, các điều khoản loại trừ.; Nhập số ODO hiện tại của xe, mô tả chi tiết lỗi gặp phải và tải ảnh/video hiện trạng.; Kiểm tra sơ bộ điều kiện còn hạn (thời gian hiện tại < expires_at và ODO hiện tại < end_odometer) (BR21).; Tạo bản ghi trong warranty_claims (claim_code tự sinh, status = 'OPEN', requested_at = hiện tại).; Gửi thông báo đến Quản lý xưởng để tiến hành thẩm định và xử lý.
- **Kết quả cần đạt:** Tạo bản ghi warranty_claims ở trạng thái OPEN, gắn đúng warranty_id và xe.
- **Trường hợp bị từ chối:** Từ chối nếu khách hàng khiếu nại cho xe không thuộc quyền sở hữu của họ.
- **Ngoại lệ:** Lễ tân tra cứu mã bảo hành theo biển số xe và nhập thông tin khiếu nại thay mặt khách hàng.
- **Quy tắc:** BR02 (Kiểm soát quyền), BR21 (Điều kiện bảo hành kép Ngày & Km), BR22 (Quy trình claim).

## T33 UC33 — Quyết định và xử lý bảo hành

- **Chủ trì:** TV3.
- **Chuẩn bị:** Yêu cầu bảo hành tồn tại; người duyệt có vai trò ADMIN.
- **Thao tác:** Mở danh sách yêu cầu bảo hành, xem nội dung phản ánh và lịch sử sửa chữa gốc.; Xem kết quả kiểm tra thực tế từ Kỹ thuật viên (kiểm tra tem bảo hành, hao mòn tự nhiên, tai nạn...).; Ra quyết định: CHẤP THUẬN (APPROVED) hoặc TỪ CHỐI (REJECTED), bắt buộc nhập lý do chi tiết (decision_reason).; Cập nhật warranty_claims: status, decided_by, decided_at, decision_reason.; Nếu APPROVED: tự động tạo một phiếu sửa chữa mới (handling_order_id), liên kết với xe đó để thực hiện xử lý (BR22).; Gửi thông báo kết quả thẩm định kèm hướng dẫn đến khách hàng qua notifications.
- **Kết quả cần đạt:** Cập nhật warranty_claims (APPROVED/REJECTED); nếu APPROVED thì tạo repair_orders mới xử lý bảo hành.
- **Trường hợp bị từ chối:** Từ chối nếu thiếu lý do thẩm định hoặc vi phạm điều khoản bảo hành.
- **Ngoại lệ:** Hệ thống ghi nhận lý do từ chối (ví dụ: hỏng do va đập ngoại lực, quá số km quy định) và thông báo cho khách hàng; claim chuyển sang REJECTED.
- **Quy tắc:** BR01 (ADMIN quyết định), BR21 (Điều kiện loại trừ), BR22 (Tạo lần xử lý mới, không sửa lần cũ).

## T34 UC34 — Chăm sóc khách và theo dõi thông báo

- **Chủ trì:** TV1.
- **Chuẩn bị:** Hệ thống có các bản ghi thông báo trong bảng notifications.
- **Thao tác:** Nhận sự kiện từ nghiệp vụ (tiếp nhận xe, duyệt giá, hoàn thành sửa chữa, mời nhận xe).; Tạo bản ghi trong notifications ở trạng thái PENDING cùng transaction với sự kiện gốc (BR23).; Tiến trình chạy nền quét các thông báo PENDING, khóa dòng bằng locked_until và gọi dịch vụ gửi (WEB/EMAIL/SMS).; Nếu thành công: cập nhật status = 'SENT', sent_at = thời điểm gửi, lưu provider_reference.; Xem thông báo trên màn hình chuông thông báo; hệ thống cập nhật read_at.; Xem nhật ký thông báo để hỗ trợ gọi điện trực tiếp cho các khách hàng chưa xem thông báo.
- **Kết quả cần đạt:** Thông báo được gửi tới đúng người qua đúng kênh (WEB/EMAIL/SMS); cập nhật trạng thái SENT hoặc đọc (read_at).
- **Trường hợp bị từ chối:** Ghi nhận lỗi FAILED và tự động xếp lịch thử lại (retry) nếu lỗi mạng bên thứ ba.
- **Ngoại lệ:** Worker tăng attempt_count, lưu last_error, đặt next_attempt_at để thử lại sau; giao dịch nghiệp vụ chính trước đó không bị ảnh hưởng (BR23).
- **Quy tắc:** BR02 (Không xem trộm thông báo), BR23 (Queue PENDING cùng commit, worker gửi sau commit có lease/retry).

## T35 UC35 — Nhắc bảo dưỡng và hạn bảo hành

- **Chủ trì:** TV1.
- **Chuẩn bị:** Có dữ liệu dịch vụ đã thực hiện (có interval_km / interval_months) hoặc bảo hành trong warranties.
- **Thao tác:** Quét danh sách các xe đã từng làm dịch vụ có mốc chu kỳ (services.interval_km / interval_months) và các hợp đồng warranties sắp hết hạn trong 15 ngày.; Tính toán mốc thời gian và mốc km dự kiến dựa trên thói quen di chuyển trung bình của xe.; Tạo mã sự kiện event_key duy nhất (ví dụ: 'REMIND_MAINT_V123_202610').; Kiểm tra chống trùng theo uq_notice_event_channel; tạo bản ghi notifications mới (channel = 'WEB', 'EMAIL', 'SMS').; Xem danh sách gợi ý chăm sóc khách hàng trong ngày để chủ động gọi điện tư vấn và hỗ trợ đặt lịch hẹn (UC07).
- **Kết quả cần đạt:** Tạo các thông báo nhắc nhở PENDING không trùng lặp, gửi đến khách hàng.
- **Trường hợp bị từ chối:** Bỏ qua các xe chưa đủ mốc hoặc đã được gửi nhắc nhở trong cùng chu kỳ.
- **Ngoại lệ:** Ràng buộc duy nhất chặn tạo bản ghi đúp; tiến trình bỏ qua xe đó an toàn.
- **Quy tắc:** BR05 (Chu kỳ dịch vụ), BR21 (Thời hạn bảo hành), BR23 (Deduplication event_key).

## T36 UC36 — Xem dashboard và báo cáo

- **Chủ trì:** TV1.
- **Chuẩn bị:** Người dùng đăng nhập với quyền hạn phù hợp phạm vi báo cáo.
- **Thao tác:** Mở màn hình Báo cáo & Thống kê, chọn loại báo cáo: Doanh thu theo thời gian, Công nợ khách hàng, Báo cáo tồn kho, Năng suất kỹ thuật viên.; Chọn khoảng thời gian (từ ngày, đến ngày) và các tiêu chí lọc.; Kiểm tra quyền truy cập; thực hiện các truy vấn tổng hợp chỉ đọc (Read-only queries).; Tổng hợp payments IN (tiền thu thực tế), payments OUT (tiền chi NCC), invoices (doanh thu ghi nhận), loại bỏ các hóa đơn VOID.; Tính toán giờ công KTV từ tổng các labor_sessions thực tế (không tính thời gian tạm dừng) (BR11).; Hiển thị biểu đồ trực quan kèm bảng số liệu chi tiết và thời điểm cập nhật mới nhất.; Chọn 'Xuất báo cáo' (Excel / PDF) để phục vụ họp ban quản trị.
- **Kết quả cần đạt:** Hiển thị các chỉ số tài chính và vận hành chính xác từ dữ liệu nguồn; hỗ trợ xuất Excel/PDF.
- **Trường hợp bị từ chối:** Từ chối nếu tài khoản không đủ quyền xem số liệu tài chính.
- **Ngoại lệ:** Chỉ hiển thị số liệu xuất/nhập/tồn và giá trị tồn kho; ẩn số liệu doanh thu dịch vụ tổng gara.
- **Quy tắc:** BR02 (Phân quyền dữ liệu), BR11 (Giờ công thực tế), BR18 (Doanh số hóa đơn), BR19/BR20 (Tách bạch thu/chi).

## T37 UC37 — Quản lý ca làm và tài nguyên khoang

- **Chủ trì:** TV3.
- **Chuẩn bị:** Nhân viên và khoang đã có trong hệ thống; người dùng có quyền ADMIN.
- **Thao tác:** Mở màn hình Quản lý ca làm và khoang sửa chữa.; Tạo ca làm việc cho KTV: chọn nhân viên, nhập giờ bắt đầu (starts_at), giờ kết thúc (ends_at), trạng thái ACTIVE.; Kiểm tra khoang bảo dưỡng: chọn khoang, tạo lịch bảo trì (reservation_type = 'MAINTENANCE', chọn khoảng thời gian).; Kiểm tra tính hợp lệ: ends_at > starts_at, không được xếp bảo trì nếu khoang đang có xe sửa chữa (BR06).; Lưu thông tin vào employee_shifts và bay_reservations.; Cập nhật dữ liệu để Lễ tân có căn cứ xếp lịch hẹn (UC07) và phân công thợ (UC11).
- **Kết quả cần đạt:** Ca làm việc (employee_shifts) và lịch bảo trì (bay_reservations) được lưu trữ hợp lệ.
- **Trường hợp bị từ chối:** Báo lỗi nếu giờ kết thúc <= giờ bắt đầu hoặc trùng lịch phân ca.
- **Ngoại lệ:** ADMIN chọn CANCELLED ca làm; hệ thống kiểm tra và cảnh báo nếu nhân viên đang có task phân công chưa hoàn thành.
- **Quy tắc:** BR06 (Không trùng khoang), BR07 (Giải phóng tài nguyên), BR09 (Phân công đúng ca làm).

## T38 UC38 — Tra cứu lịch sử xe và hồ sơ hậu mãi

- **Chủ trì:** TV2.
- **Chuẩn bị:** Xe đã từng thực hiện ít nhất một phiếu sửa chữa trong hệ thống.
- **Thao tác:** Nhập biển số xe cần tra cứu lịch sử.; Kiểm tra quyền sở hữu đối tượng: nếu là CUSTOMER thì xe phải thuộc customer_id của họ (BR02).; Truy vấn danh sách các repair_orders đã đóng (CLOSED) của xe, sắp xếp theo thời gian giảm dần.; Với mỗi lần sửa: hiển thị ngày vào, số ODO, hiện trạng, chẩn đoán, các công việc đã làm (repair_tasks), phụ tùng đã thay (repair_order_parts) và hóa đơn (invoices).; Hiển thị danh sách các chính sách bảo hành còn hiệu lực trong bảng warranties.; Xem chi tiết biên bản nghiệm thu hoặc tải hóa đơn điện tử cũ dạng PDF.
- **Kết quả cần đạt:** Hiển thị đầy đủ cây lịch sử sửa chữa chính xác từ dữ liệu nguồn; không tạo bảng dư thừa.
- **Trường hợp bị từ chối:** Từ chối truy cập nếu khách hàng cố tình tra cứu biển số xe của người khác.
- **Ngoại lệ:** Chủ mới chỉ được xem thông số kỹ thuật xe và lịch sử bảo dưỡng kỹ thuật; hệ thống ẩn thông tin cá nhân và hóa đơn thanh toán của chủ cũ (BR04).
- **Quy tắc:** BR02 (Bảo mật quyền xem xe), BR04 (Lịch sử xe độc lập với đổi chủ), BR21 (Tra cứu bảo hành).

## Kiểm thử tích hợp bổ sung tại G3–G5

- Hai kết nối riêng tranh cùng khoang: một request thành công, một trả 409; lịch tiếp giáp được nhận.
- Hai request START timer của cùng thợ trên hai task: tối đa một phiên mở; PAUSE và RESUME không tính khoảng nghỉ.
- Hai request duyệt/giữ phụ tùng với lượng khả dụng không đủ tổng: không âm tồn; tổng lượng giữ khớp các dòng.
- POST lại chứng từ kho hoặc gửi lại request_key: sổ kho chỉ có một dòng cho mỗi dòng chứng từ.
- Hai lần thu/chi đồng thời khi số dư không đủ tổng: tổng thanh toán không vượt nợ; trả 409 cho request còn lại.
- Rollback giữa bước handover: không đóng phiếu, không giải phóng một phần tài nguyên, không tạo một phần bảo hành hay gửi thông báo.
- Worker mất kết nối sau gửi: lease/retry và idempotency của provider không gửi trùng khi provider hỗ trợ; log rõ trường hợp provider không hỗ trợ.
- Sửa danh mục và đổi chủ sau phát hành hóa đơn: snapshot và khách lịch sử không đổi; chủ mới không đọc thông tin tài chính chủ cũ.
