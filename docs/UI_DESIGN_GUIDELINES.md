# Quy chuẩn thiết kế giao diện AutoGarage

## Phạm vi và phong cách

Áp dụng cho mọi màn hình frontend và mọi form thêm, sửa, xác nhận xóa. Đây là
quy chuẩn bắt buộc cho code mới và phần UI được chỉnh sửa; tài liệu không khẳng
định tất cả màn hình hiện tại đã đáp ứng quy chuẩn.

- **Nguyên tắc xuyên suốt:** Giao diện phải được thiết kế cho người sử dụng phần mềm, không phải để thể hiện cấu trúc phân công nhóm hay tài liệu use case.
- Phong cách minimalism: bố cục rõ, khoảng trắng hợp lý, ít trang trí, ưu tiên thông tin và hành động chính.
- Toàn ứng dụng dùng chung typography, bảng màu, khoảng cách, bo góc, đường viền, kích thước control, icon và trạng thái tương tác.
- Design token và quy tắc layout nằm ở CSS dùng chung; không khai báo palette hoặc kích thước control riêng rải rác trong từng màn.
- Các màn có thể khác nội dung và bố cục nghiệp vụ, nhưng cùng một loại thành phần phải có cùng hình thức và hành vi. Trang đăng nhập/đăng ký vẫn theo hệ thống nhận diện chung.

## Quy chuẩn nội dung, ngôn ngữ và đặt tên hướng người dùng

### Đặt tên theo chức năng
- Tuyệt đối không hiển thị mã UC (UC01, UC02...), tên thành viên phụ trách (thành viên 1, thành viên 2...), tên module kỹ thuật hoặc mã task (TASK-123...) trên bất kỳ thành phần nào của giao diện (menu, breadcrumb, tiêu đề trang, nút, bảng, dialog).
- Tên màn hình và tính năng phải đặt theo góc nhìn nghiệp vụ thực tế của người dùng cuối.

| Không nên | Nên sử dụng |
| :--- | :--- |
| UC01 – Quản lý khách hàng | Quản lý khách hàng |
| Màn hình của thành viên 2 | Quản lý lịch hẹn |
| UC07 – Add Repair Order | Tạo phiếu sửa chữa |
| CRUD phụ tùng | Quản lý phụ tùng |

### Mô tả ngắn, dễ hiểu
- Chỉ bổ sung mô tả khi cần giải thích mục đích hoặc cách sử dụng chức năng.
- Tránh nội dung dài dòng, sáo rỗng hoặc lặp lại nguyên văn tiêu đề.
- *Ví dụ mô tả của trang Quản lý lịch hẹn:*  
  > *"Theo dõi, tạo mới và cập nhật lịch hẹn bảo dưỡng của khách hàng."*

### Ngôn ngữ hướng đến người dùng
- Nút bấm, nhãn trường (label), chỉ dẫn (placeholder/hint), thông báo lỗi và thông báo thành công phải rõ nghĩa, tự nhiên và bám sát nghiệp vụ.
- Tuyệt đối không dùng thuật ngữ lập trình khó hiểu (tránh dùng `CRUD`, `Payload`, `Schema`, `Endpoint`, `Null`, `DB Error`, `Primary Key`, `Foreign Key`, `Boolean`...).

### Trải nghiệm thao tác (UX Flow)
- **Dễ tìm chức năng:** Cấu trúc thanh điều hướng phân nhóm khoa học, công cụ tìm kiếm và bộ lọc trực quan.
- **Biết đang ở đâu:** Nhận diện rõ vị trí hiện tại trong hệ thống thông qua trạng thái active của menu, breadcrumb và tiêu đề trang.
- **Hiểu kết quả thao tác:** Mọi hành động (thêm, sửa, xóa, chuyển trạng thái) đều có phản hồi tức thì (toast/alert) rõ ràng về kết quả.
- **Không có bước thừa:** Tối ưu luồng thao tác, loại bỏ các bước xác nhận hoặc chuyển hướng trung gian không cần thiết.

## Component dùng chung

- Tái sử dụng `frontend/src/shared/components/ui.tsx`, `DataTable.tsx` và các thành phần chung hiện có trước khi tạo component mới.
- Dùng chung `Button`, `Field`, `PasswordInput`, `FormError`, trạng thái loading/empty/error, badge, bảng và phân trang. Không tự dựng lại mỗi loại ở từng feature.
- Form thêm/sửa dùng `Modal` hoặc `Sheet` với `placement="center"`. Không bỏ `placement` rồi vô tình dùng chế độ `side` mặc định cho form CRUD.
- Xóa dùng `ConfirmDialog` hoặc `ConfirmModal` với biến thể danger; không dùng `window.confirm`, drawer hoặc modal tự dựng riêng cho từng màn.
- Khi thiếu khả năng cần thiết, bổ sung prop/variant hoặc component chung có mục đích rõ ràng. Không copy component rồi thay màu/khoảng cách cho riêng một trang.
- Khi sửa CSS dùng chung, xét tất cả nơi sử dụng; không chồng thêm selector theo tên trang để che lỗi của component gốc.

## Bố cục form thêm và sửa

- Dialog nằm giữa màn hình theo cả chiều ngang và dọc, có overlay và lề an toàn với viewport. Không ghim form sát cạnh trái/phải, không dùng slide-over/drawer cho CRUD.
- Dùng chiều rộng theo kích thước dialog chuẩn và co giãn theo viewport; chiều cao theo nội dung. Không cố định chiều cao bằng chiều cao màn hình.
- Desktop dùng CSS Grid hai cột bằng nhau: `repeat(2, minmax(0, 1fr))`. Các trường ngắn chia đều theo hàng, label/input và khoảng cách thẳng hàng.
- Trên màn hình hẹp chuyển một cột theo breakpoint chung. Không ép hai cột khiến label, input hoặc thông báo lỗi bị chật/tràn.
- Textarea, nội dung dài, nhóm động, thông báo lỗi chung và hàng nút có thể chiếm cả hàng. Không kéo rộng mọi field hoặc thêm field rỗng chỉ để lấp cột.
- Nhóm và sắp xếp trường theo nghiệp vụ, đọc từ trái sang phải rồi xuống hàng. Form thêm và sửa cùng đối tượng giữ thứ tự, tên trường và nhóm trường nhất quán.
- Label nằm trên control, có dấu bắt buộc thống nhất và hint khi cần; không dùng placeholder thay label. Trạng thái readonly, disabled và lỗi phải phân biệt rõ.

## Chiều cao và cuộn

- Form thông thường phải vừa viewport desktop hỗ trợ, thấy được tiêu đề, nội dung và hàng nút; không đặt thanh cuộn nội bộ làm bố cục mặc định.
- Không cuộn ngang form, không chia hai panel nhập liệu cuộn riêng và không ghim form hai bên màn hình.
- Với form nhiều trường, ưu tiên nhóm thông tin và chia thành bước ngắn có ý nghĩa; bảo toàn dữ liệu khi đổi bước và chỉ lưu khi người dùng xác nhận.
- Không chia bước tùy tiện cho form ngắn. Không giấu trường bắt buộc, cắt nội dung bằng `overflow: hidden` hoặc thu nhỏ font/control để ép vừa.
- Khi viewport thấp, zoom lớn, mở bàn phím hoặc lỗi validation làm tăng chiều cao, cho phép một vùng cuộn dọc tối thiểu để người dùng vẫn đến được mọi trường và nút. Không cuộn lồng nhau, không khóa cuộn khiến nội dung mất khả năng truy cập.
- Giữ focus và khả năng truy cập trường lỗi ở bước tương ứng; không đánh đổi khả năng thao tác chỉ để loại bỏ thanh cuộn.

## Hành động và phản hồi

- Tiêu đề rõ hành động và đối tượng: Thêm…, Sửa…, Xóa…. Form có một hành động chính, dùng cùng từ ngữ trên các màn.
- Hàng nút cuối form chiếm trọn hàng; Hủy ở trước, Tạo/Lưu thay đổi ở sau. Trên mobile dùng cùng quy tắc responsive của component chung.
- Xác nhận xóa nêu đúng tên/mã đối tượng và hệ quả; dùng nút Hủy và Xóa, màu danger chỉ dành cho hành động phá hủy.
- Trong lúc gửi, hiển thị pending và ngăn gửi lặp. Khi thất bại, giữ dữ liệu đã nhập trừ trường nhạy cảm cần xóa theo quy tắc bảo mật.
- Validation cạnh trường; lỗi API chung qua `FormError` và bộ chuyển đổi lỗi dùng chung. Không hiển thị stack trace hoặc payload thô.
- Thông báo thành công dùng cơ chế chung; đóng form và cập nhật danh sách sau khi server xác nhận thành công.

## Khả năng truy cập và kiểm chứng

- Dialog có tên/mô tả phù hợp, quản lý focus, Tab không thoát ra nền và trả focus về nút mở khi đóng. Giữ các bảo vệ dữ liệu chưa lưu và trạng thái đang gửi.
- Nút chỉ có icon phải có tên truy cập; focus nhìn thấy rõ. Error gắn với control tương ứng, không chỉ dùng màu để truyền trạng thái.
- Không thu nhỏ chữ và vùng bấm dưới token chung để làm form vừa. Không tắt zoom hoặc che nội dung đang được focus.
- **Checklist nghiệm thu nội dung & UI:**
  - [ ] **Menu & Breadcrumb:** Tên mục thể hiện đúng chức năng nghiệp vụ; không chứa mã UC (UCxx), tên thành viên phụ trách hay mã module kỹ thuật.
  - [ ] **Tiêu đề & Mô tả:** Tiêu đề rõ ràng theo chức năng; mô tả ngắn gọn, giải thích đúng mục đích/cách dùng, không lặp lại tiêu đề.
  - [ ] **Bảng dữ liệu:** Tên cột ngắn gọn, dễ hiểu; trạng thái rỗng (empty state) có chỉ dẫn hành động tiếp theo; phân trang rõ ràng.
  - [ ] **Form & Dialog:** Label trường đặt phía trên control, dùng từ ngữ đời thường, không dùng thuật ngữ kỹ thuật khó hiểu; thông báo lỗi và thành công giải thích rõ ràng.
  - [ ] Form thêm/sửa dùng `Modal` hoặc `Sheet` với `placement="center"`; dialog căn giữa khi đủ không gian.
  - [ ] Form xóa dùng `ConfirmDialog`/`ConfirmModal` dạng `danger`, nêu đúng đối tượng và hệ quả.
  - [ ] Desktop đủ rộng hiển thị hai cột cân đối; màn hình hẹp chuyển một cột đúng breakpoint chung.
  - [ ] Field thẳng hàng, không có field rỗng để lấp layout; field dài được phép span toàn hàng.
  - [ ] Không tràn ngang form; không có cuộn dọc lồng nhau; khi thiếu chiều cao mọi field và nút vẫn truy cập được.
  - [ ] Trật tự field và thuật ngữ nhất quán giữa Thêm và Sửa.
  - [ ] Label, required, hint, readonly, disabled và lỗi dễ phân biệt.
  - [ ] Hàng nút đúng thứ tự; pending chặn gửi trùng; lỗi giữ dữ liệu; thành công chỉ sau phản hồi server.
  - [ ] Focus, Tab, Escape (khi được phép), trả focus và cảnh báo dữ liệu chưa lưu hoạt động đúng.
  - [ ] Không có CSS riêng ghi đè để che lỗi của component dùng chung.
- Kiểm tra form thêm, sửa, xóa trên desktop và mobile; thêm màn hình thấp/zoom và trường hợp nhiều lỗi validation khi layout có thay đổi.
- Chạy lint/typecheck/build và test phù hợp với thay đổi component/hành vi. Chỉnh riêng tài liệu không yêu cầu chạy lại bộ test ứng dụng.
