# Báo cáo gốc (trích văn bản)

> Trích tự động từ `BaoCao_PhanTich_Va_ThietKe_HeThong_QuanLy_GaraOto.docx` (14/10/2026). Hình ảnh không kèm; nhãn [HeadingN] là kiểu đoạn trong Word.

BÁO CÁO PHÂN TÍCH VÀ THIẾT KẾHỆ THỐNG QUẢN LÝ GARA Ô TÔ
Một chi nhánh • 30 bảng nghiệp vụ • 5 vai trò • Nhóm 4 thành viênFrontend React & TypeScript • Backend Spring Boot • CSDL MySQL 8.4 (Flyway V1–V4)Tích hợp trọn bộ Sơ đồ ERD Chen và Biểu đồ 38 Use CaseNgày hoàn thiện: 14 tháng 10 năm 2026
Nội dung
Kết quả mô tả & Biểu đồ đính kèm
Phân tích bài toán
Bối cảnh, mục tiêu, 5 tác nhân, phạm vi và 24 quy tắc nghiệp vụ (BR01–BR24)
Mô hình dữ liệu
1 ERD Chen tổng quan + 7 ERD Chen chi tiết (kèm ảnh minh họa), 30 bảng (373 cột), 71 FK
Use Case
38 Use Case hoàn chỉnh: Mỗi Use Case 1 ảnh sơ đồ UML đính kèm + Bảng đặc tả chi tiết
Triển khai & Kiểm thử
Flyway V1–V4, phân công 42 đầu việc, 8 hợp đồng service, 38 kịch bản nghiệm thu T01–T38
[Heading1] MỤC LỤC TỔNG QUAN
[ListBullet] Chương 1: Giới thiệu bài toán chung và các giả định thiết kế
[ListBullet] Chương 2: Các chức năng và luồng hoạt động (24 Quy tắc nghiệp vụ BR01–BR24)
[ListBullet] Chương 3: Thiết kế ERD tổng quan và chi tiết theo chuẩn Chen (Đính kèm 8 ảnh sơ đồ)
[ListBullet] Chương 4: Ánh xạ ERD sang mô hình cơ sở dữ liệu quan hệ và 2 view công nợ
[ListBullet] Chương 5: Thiết kế chi tiết ba mươi bảng dữ liệu (Data Dictionary 373 cột)
[ListBullet] Chương 6: Mô hình Use Case và đặc tả chi tiết 38 Use Cases (Đính kèm 38 ảnh sơ đồ)
[ListBullet] Chương 7: Kiến trúc triển khai và quản lý cơ sở dữ liệu (Flyway V1–V4)
[ListBullet] Chương 8: Phân công module cho bốn thành viên và 8 hợp đồng service
[ListBullet] Chương 9: Kế hoạch kiểm thử và 38 kịch bản nghiệm thu dự kiến (T01–T38)
[ListBullet] Chương 10: Tài liệu tham chiếu và các tham số cấu hình hệ thống
[Heading1] 1 Giới thiệu bài toán chung
[Heading2] 1.1 Bối cảnh và vấn đề cần giải quyết
Gara ô tô một chi nhánh tiếp nhận xe đặt trước và xe đến trực tiếp. Mỗi xe có thể trải qua kiểm tra hiện trạng, chẩn đoán, báo giá, nhiều công việc sửa chữa và thay phụ tùng, kiểm định chất lượng (QC), thu tiền, bàn giao và bảo hành. Trong cùng một thời điểm, nhiều xe cùng sử dụng khoang và nhiều kỹ thuật viên phối hợp; dữ liệu khách, chi phí, vật tư và thời gian phải được kết nối chặt chẽ theo từng lần sửa. Nếu chỉ ghi trên giấy hoặc các bảng rời, việc điều phối khoang, phân công thợ, giữ và xuất kho phụ tùng, thu chi từng phần và bảo hành sẽ xảy ra sai sót, thất thoát dữ liệu.
[Heading2] 1.2 Mục tiêu và kết quả mong đợi
[ListBullet] Tập trung toàn bộ hồ sơ khách hàng, phương tiện và từng lần sửa chữa để mọi bộ phận sử dụng chung một nguồn dữ liệu gốc (Single Source of Truth).
[ListBullet] Lễ tân điều phối khoang sửa chữa và phân công thợ kỹ thuật theo ca làm việc, tay nghề; ngăn chặn triệt để tình trạng xếp trùng tài nguyên khi thao tác đồng thời.
[ListBullet] Khách hàng theo dõi minh bạch và phê duyệt từng hạng mục công việc/phụ tùng trước khi triển khai, kể cả các báo giá phát sinh trong quá trình sửa chữa.
[ListBullet] Theo dõi chính xác tồn kho thực tế, lượng vật tư đã tạm giữ cho xe, lượng xuất dùng, lượng trả lại và giá vốn bình quân; mọi biến động đều có chứng từ và người chịu trách nhiệm.
[ListBullet] Bấm giờ (timer) tính giờ công thợ từ các khoảng làm việc thực tế, tự động loại trừ thời gian tạm dừng nghỉ ngơi; lưu kết quả nghiệm thu QC và biên bản bàn giao.
[ListBullet] Chụp snapshot dữ liệu lịch sử hóa đơn bất biến, ghi nhận thu chi nhiều lần theo đúng đối tượng và kiểm soát chặt chẽ công nợ khách hàng và nhà cung cấp.
[ListBullet] Tự động quản lý bảo hành theo từng linh kiện bàn giao, hỗ trợ nhắc bảo dưỡng định kỳ và cung cấp báo cáo quản trị trực quan.
[Heading2] 1.3 Phạm vi và các giả định thiết kế
Phạm vi thiết kế là một gara tại một chi nhánh, một tồn kho tổng, nhiều khoang sửa chữa và nhân viên. Cơ sở dữ liệu gồm đúng 30 bảng nghiệp vụ cốt lõi (Flyway quản lý thêm 1 bảng kỹ thuật flyway_schema_history, tổng 31 bảng vật lý). Hệ thống vận hành với 5 vai trò người dùng chuẩn hóa.
[Heading2] 1.4 Tác nhân và trách nhiệm (5 Vai trò)
Mã Role
Tác nhân
Trách nhiệm chính trong hệ thống
ADMIN
Quản lý và Quản trị viên
Quản trị tài khoản, phân quyền, cấu hình hệ thống, duyệt báo giá ngoại lệ, chi tiền nhà cung cấp, giải quyết khiếu nại bảo hành và xem toàn bộ báo cáo tài chính/vận hành.
RECEPTIONIST
Lễ tân (Cố vấn dịch vụ)
Tiếp đón khách hàng, tạo hồ sơ xe, tiếp nhận hiện trạng, đặt và điều phối lịch hẹn, xếp khoang, phân công thợ, lập báo giá gửi khách, lập hóa đơn, thu tiền khách và bàn giao xe.
WAREHOUSE_STAFF
Quản lý kho
Quản lý danh mục phụ tùng, nhà cung cấp, lập chứng từ nhập kho mua hàng, giữ phụ tùng sau khi khách duyệt, xuất kho cho sửa chữa, nhập trả kho và điều chỉnh tồn kho.
TECHNICIAN
Kỹ thuật viên
Tiếp nhận công việc được phân công, ghi chẩn đoán hư hỏng, thực hiện bắt đầu/tạm dừng/tiếp tục timer giờ công; thực hiện kiểm định chất lượng (QC) nếu có quyền can_qc = true.
CUSTOMER
Khách hàng
Quản lý xe cá nhân, đặt lịch hẹn bảo dưỡng trực tuyến, xem và phê duyệt từng hạng mục báo giá, theo dõi tiến độ sửa chữa, xem hóa đơn và gửi yêu cầu bảo hành.
[Heading1] 2 Các chức năng và quy tắc nghiệp vụ
[Heading2] 2.1 Quy tắc nghiệp vụ cốt lõi (BR01 – BR24)
Mã
Tên quy tắc
Nội dung kiểm soát nghiệp vụ
BR01
Năm vai trò chuẩn
Hệ thống chỉ gồm đúng 5 vai trò: ADMIN, RECEPTIONIST, WAREHOUSE_STAFF, TECHNICIAN, CUSTOMER. ADMIN hợp nhất quản lý xưởng và quản trị hệ thống; chi tiền NCC thuộc quyền ADMIN.
BR02
Kiểm soát quyền đối tượng
Backend kiểm tra role và quyền truy cập trên từng khách hàng/xe/phiếu/ảnh/thông báo. Không dựa vào việc ẩn menu UI để làm bảo mật.
BR03
Khách hàng không bắt buộc tài khoản
customers.user_id cho phép NULL. Khách trực tiếp có thể không cần tài khoản online. Một user gắn tối đa 1 khách hàng và tối đa 1 nhân viên.
BR04
Chủ xe hiện tại vs Khách hàng lịch sử
vehicles.customer_id là người quản lý xe hiện tại. repair_orders.customer_id và customer_snapshot trên hóa đơn là khách hàng lúc giao dịch, bất biến khi xe đổi chủ.
BR05
Dịch vụ và gói bảo dưỡng
Một booking chọn 1 dịch vụ chính và mô tả nhu cầu. Gói bảo dưỡng PACKAGE được quản lý như 1 dịch vụ có mô tả phạm vi và mốc chu kỳ km/tháng.
BR06
Khóa khoang không trùng lặp
Khoảng thời gian dùng [starts_at, ends_at). Khóa hàng khoang trước khi kiểm tra giao nhau; lịch kế tiếp có thể bắt đầu đúng thời điểm lịch trước kết thúc.
BR07
Giải phóng tài nguyên nguyên tử
Dời lịch, hủy lịch hoặc bàn giao xe phải giải phóng bay_reservations và lượng hàng giữ tương ứng trong cùng giao dịch CSDL.
BR08
Phiếu sửa chữa từ tiếp nhận
repair_orders được tạo ngay khi xe vào xưởng. Một booking sinh tối đa 1 phiếu; khách đến trực tiếp có booking_id = NULL. Chưa duyệt báo giá không được triển khai sửa.
BR09
Phân công đủ điều kiện
Kiểm tra kỹ năng, ca làm việc và lịch không trùng. QC yêu cầu can_qc = true. Mỗi task chỉ có tối đa duy nhất 1 thợ chính (MAIN) đang hoạt động.
BR10
Một timer mở cho mỗi thợ
Bảng labor_sessions kiểm soát qua active_employee_id UNIQUE; một kỹ thuật viên chỉ được mở tối đa 1 timer bấm giờ tại một thời điểm.
BR11
Giờ công thực tế loại trừ nghỉ
Tạm dừng công việc (PAUSE) sẽ chốt phiên; tiếp tục (RESUME) tạo phiên mới. Giờ công là tổng các khoảng làm việc thực tế, loại trừ thời gian nghỉ.
BR12
Phiên bản báo giá độc lập
version_no tăng dần trong cùng phiếu sửa chữa. Báo giá phát sinh sau triển khai lập bản mới loại SUPPLEMENTAL trỏ parent_estimate_id về báo giá gốc.
BR13
Duyệt theo từng dòng
Dòng SERVICE/PART chọn đúng một nguồn. Chỉ dòng APPROVED mới sinh repair_tasks hoặc giữ phụ tùng trong kho. Dòng REJECTED/PENDING không được triển khai.
BR14
Đồng bộ tồn thực và lượng giữ
Tồn khả dụng = stock_quantity - reserved_quantity. Tổng estimate_items.reserved_quantity phải khớp chính xác với parts.reserved_quantity dưới khóa CSDL.
BR15
Ghi sổ kho bất biến 1 lần
Chứng từ DRAFT không đổi tồn. Chỉ khi POSTED mới sinh đúng 1 bản ghi inventory_transactions cho mỗi dòng chứng từ. Không sửa sổ kho cũ.
BR16
Checklist và tệp bằng chứng
Checklist tiếp nhận, QC và bàn giao lưu dạng snapshot JSON có xác thực schema. Tệp ảnh lưu storage, CSDL quản lý metadata trong attachments.
BR17
QC PASSED trước hóa đơn và bàn giao
Chỉ khi lần kiểm tra QC mới nhất (latest_qc_check_id) đạt PASSED, các task hoàn thành và không còn timer mở thì mới cho phép lập hóa đơn và bàn giao xe.
BR18
Snapshot giá và tính toán số tiền
Tính toán bằng BigDecimal. Hóa đơn và chi tiết hóa đơn chụp nguyên vẹn tên, đơn vị, giá bán và giá vốn tại thời điểm phát hành; không bị thay đổi khi danh mục đổi giá.
BR19
Thu tiền khách và giới hạn công nợ
payments direction = IN gắn với hóa đơn khách. Số tiền > 0, mã giao dịch duy nhất. Khóa hóa đơn để không thu vượt dư nợ. Khách nợ phải nằm trong hạn mức credit_limit.
BR20
Chi tiền nhà cung cấp
payments direction = OUT chỉ gắn với chứng từ mua hàng (document_type = 'IN') đã POSTED có supplier_id, không áp dụng cho tồn đầu kỳ; không chi vượt nợ.
BR21
Bảo hành chụp từ hóa đơn
warranties chụp chính sách từ invoice_items lúc bàn giao. Hiệu lực bảo hành kiểm tra song song cả thời gian và số km; vượt 1 trong 2 mốc thì hết hạn.
BR22
Lần xử lý bảo hành mới
Yêu cầu bảo hành được duyệt sẽ khởi tạo một phiếu sửa chữa mới để xử lý; không ghi đè lên phiếu cũ; vẫn ghi nhận xuất kho vật tư và giá vốn.
BR23
Chống lệnh trùng và hàng đợi thông báo
Lệnh nhạy cảm có request_key và request_hash lưu audit_events. Thông báo ghi PENDING cùng transaction với nghiệp vụ; worker gửi sau commit có lease và retry.
BR24
Bảo toàn lịch sử dữ liệu
Không xóa vật lý (hard delete) các bản ghi danh mục có lịch sử; sử dụng cờ is_active = false để ngừng hoạt động nhằm bảo toàn khóa ngoại.
[Heading1] 3 Thiết kế ERD tổng quan và chi tiết theo chuẩn Chen
Mô hình thực thể liên kết (ERD) được thiết kế tuân thủ nghiêm ngặt chuẩn Peter Chen: Thực thể được biểu diễn bằng hình chữ nhật, Mối quan hệ được biểu diễn bằng hình thoi, và Thuộc tính được biểu diễn bằng hình oval. Khóa chính được gạch chân hoặc ký hiệu PK. Thuộc tính của mối quan hệ Nhiều - Nhiều (như số lượng linh kiện xuất dùng) được gắn trực tiếp lên hình thoi quan hệ.
[Heading2] 3.1 Lược đồ ERD Chen Tổng quan
Lược đồ tổng quan thể hiện cấu trúc 15 thực thể trung tâm và các quan hệ nòng cốt của xưởng gara ô tô:
Hình 3.1: Sơ đồ ERD Tổng quan Hệ thống Quản lý Gara Ô tô (Ký hiệu Peter Chen)
[Heading2] 3.2 ERD Chen Chi tiết theo 7 Nhóm Dữ liệu (Đủ 30 Bảng)
[Heading3] ERD Chen Nhóm 1 — Khách hàng, Phương tiện, Lịch hẹn, Dịch vụ & Tài khoản
Nhóm 1 gồm 5 thực thể: users, customers, vehicles, services, bookings. Quản lý tài khoản đăng nhập, hồ sơ chủ xe cá nhân/doanh nghiệp, xe và đặt hẹn dịch vụ.
Hình 3.2.1: ERD Chen Nhóm 1 — Khách hàng, Phương tiện, Lịch hẹn, Dịch vụ & Tài khoản
[Heading3] ERD Chen Nhóm 2 — Tiếp nhận, Hiện trạng & Hồ sơ sửa chữa
Nhóm 2 gồm 3 thực thể: repair_orders, vehicle_checks, attachments. Quản lý việc lập phiếu ngay khi tiếp nhận xe, lưu ODO, mức xăng, checklist trầy xước và ảnh chụp bằng chứng.
Hình 3.2.2: ERD Chen Nhóm 2 — Tiếp nhận, Hiện trạng & Hồ sơ sửa chữa
[Heading3] ERD Chen Nhóm 3 — Nhân viên, Ca làm việc & Khoang sửa chữa
Nhóm 3 gồm 4 thực thể: employees, employee_shifts, service_bays, bay_reservations. Quản lý năng lực kỹ thuật của thợ, ca làm việc và điều phối khoang sửa chữa không trùng lặp.
Hình 3.2.3: ERD Chen Nhóm 3 — Nhân viên, Ca làm việc & Khoang sửa chữa
[Heading3] ERD Chen Nhóm 4 — Báo giá, Công việc sửa chữa, Phân công & Giờ công
Nhóm 4 gồm 5 thực thể: estimates, estimate_items, repair_tasks, task_assignments, labor_sessions. Quản lý chi tiết báo giá, phân công thợ và bấm giờ timer ghi nhận giờ công thực tế.
Hình 3.2.4: ERD Chen Nhóm 4 — Báo giá, Công việc sửa chữa, Phân công & Giờ công
[Heading3] ERD Chen Nhóm 5 — Nhà cung cấp, Phụ tùng, Chứng từ kho & Sổ kho
Nhóm 5 gồm 6 thực thể: suppliers, parts, repair_order_parts, inventory_documents, inventory_document_items, inventory_transactions. Quản lý danh mục linh kiện, giữ hàng, xuất dùng, trả kho và sổ kho bất biến.
Hình 3.2.5: ERD Chen Nhóm 5 — Nhà cung cấp, Phụ tùng, Chứng từ kho & Sổ kho
[Heading3] ERD Chen Nhóm 6 — Hóa đơn, Thanh toán thu chi & Bảo hành
Nhóm 6 gồm 5 thực thể: invoices, invoice_items, payments, warranties, warranty_claims. Chụp snapshot hóa đơn bất biến, thu tiền nhiều lần, thanh toán nhà cung cấp và xử lý khiếu nại bảo hành.
Hình 3.2.6: ERD Chen Nhóm 6 — Hóa đơn, Thanh toán thu chi & Bảo hành
[Heading3] ERD Chen Nhóm 7 — Thông báo & Nhật ký kiểm toán
Nhóm 7 gồm 2 thực thể: notifications, audit_events. Quản lý hàng đợi gửi thông báo đa kênh (Web, Email, SMS) có retry và nhật ký kiểm toán chống lệnh trùng.
Hình 3.2.7: ERD Chen Nhóm 7 — Thông báo & Nhật ký kiểm toán
[Heading1] 4 Thiết kế chi tiết ba mươi bảng dữ liệu
Cơ sở dữ liệu vật lý bao gồm đúng 30 bảng nghiệp vụ chuẩn hóa, tương ứng với 373 cột dữ liệu. Mỗi bảng được phân quyền chủ trì theo 4 thành viên phát triển (TV1: Nền tảng & Báo cáo; TV2: Khách hàng & Tiếp nhận; TV3: Điều phối, Báo giá & Bảo hành; TV4: Phụ tùng, Kho & Tài chính).
STT
Tên Bảng
Ý Nghĩa Dữ Liệu
Chủ Trì
01
users
Tài khoản đăng nhập, vai trò chính và trạng thái hoạt động
TV1
02
customers
Hồ sơ khách hàng cá nhân hoặc doanh nghiệp, hạn mức tín dụng
TV2
03
vehicles
Danh sách xe, biển số, số khung (VIN), ODO hiện tại
TV2
04
services
Danh mục tiền công và gói bảo dưỡng định kỳ (PACKAGE)
TV2
05
employees
Hồ sơ nhân viên kỹ thuật, mã nhân viên, khả năng QC (can_qc)
TV3
06
employee_shifts
Ca làm việc của nhân viên theo khoảng thời gian [starts_at, ends_at)
TV3
07
service_bays
Danh mục tài nguyên khoang sửa chữa (cầu nâng, khoang chung, sơn...)
TV3
08
bookings
Lịch hẹn trước của khách hàng, dịch vụ chính và thời gian đến
TV2
09
repair_orders
Hồ sơ phiếu sửa chữa trung tâm từ tiếp nhận đến bàn giao
TV3
10
bay_reservations
Lịch giữ chỗ và sử dụng khoang sửa chữa theo khoảng thời gian
TV3
11
suppliers
Hồ sơ nhà cung cấp phụ tùng và chính sách công nợ thanh toán
TV4
12
parts
Danh mục phụ tùng, tồn thực tế (stock) và lượng đang giữ (reserved)
TV4
13
estimates
Phiên bản báo giá ban đầu (INITIAL) hoặc bổ sung (SUPPLEMENTAL)
TV3
14
estimate_items
Chi tiết dòng báo giá tiền công hoặc linh kiện, quyết định duyệt
TV3
15
repair_tasks
Các đầu việc sửa chữa sinh ra từ các dòng dịch vụ đã được duyệt
TV3
16
task_assignments
Phân công nhân viên thực hiện (thợ chính MAIN, thợ phụ, người QC)
TV3
17
labor_sessions
Các khoảng bấm giờ (timer) thực tế, loại trừ thời gian tạm dừng
TV3
18
repair_order_parts
Chi tiết số lượng linh kiện ròng thực dùng và giá vốn lịch sử
TV4
19
inventory_documents
Header chứng từ kho: Nhập mua (IN), Xuất (OUT), Trả (RETURN), Điều chỉnh
TV4
20
inventory_document_items
Các dòng chi tiết phụ tùng trên chứng từ nhập/xuất/trả kho
TV4
21
inventory_transactions
Sổ nhật ký biến động kho bất biến ghi nhận khi post chứng từ
TV4
22
vehicle_checks
Biên bản kiểm tra checklist xe: Tiếp nhận, QC và Bàn giao
TV2
23
attachments
Metadata ảnh và tệp đính kèm hiện trạng xe lưu trữ an toàn
TV2
24
invoices
Hóa đơn bán hàng snapshot bất biến thông tin khách và xe
TV4
25
invoice_items
Các dòng dịch vụ và phụ tùng chốt trên hóa đơn tài chính
TV4
26
payments
Khoản thu tiền từ khách (IN) hoặc chi trả nhà cung cấp (OUT)
TV4
27
warranties
Hợp đồng bảo hành chụp chính sách theo ngày và số km
TV3
28
warranty_claims
Tiếp nhận khiếu nại bảo hành và phiếu sửa chữa xử lý mới
TV3
29
notifications
Hàng đợi thông báo đa kênh (Web, Email, SMS) kèm cơ chế retry
TV1
30
audit_events
Nhật ký kiểm toán hệ thống và khóa chống xử lý lệnh trùng lặp
TV1
[Heading1] 5 Mô hình Use Case và đặc tả chi tiết 38 Use Cases
Hệ thống bao gồm đúng 38 Use Case nghiệp vụ độc lập, bao quát toàn bộ hoạt động từ tiếp nhận, chẩn đoán, báo giá, thi công, quản lý kho, thanh toán, đến bàn giao và hậu mãi. Mỗi Use Case dưới đây đều được trang bị 1 sơ đồ Use Case Diagram (UML) trực quan kèm bảng đặc tả chi tiết:
[Heading2] UC01: Đăng nhập
Hình 6.1: Sơ đồ Use Case Diagram — UC01 (Đăng nhập)
Mã & Tên Use Case
UC01 — Đăng nhập
Tác nhân chính (Primary)
Cả 5 vai trò (ADMIN, RECEPTIONIST, TECHNICIAN, WAREHOUSE_STAFF, CUSTOMER)
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Xác thực danh tính người dùng và cấp phiên làm việc theo vai trò.
Điều kiện kích hoạt (Trigger)
Người dùng truy cập hệ thống và nhập thông tin đăng nhập.
Tiền điều kiện (Preconditions)
Tài khoản người dùng đã được tạo trong hệ thống và đang ở trạng thái hoạt động (is_active = true).
Hậu điều kiện Thành công
Người dùng đăng nhập thành công, phiên làm việc được thiết lập, giao diện điều hướng hiển thị đúng theo vai trò.
Hậu điều kiện Thất bại
Hệ thống từ chối truy cập, ghi nhận nhật ký thất bại, không cấp phiên xác thực.
Quy tắc nghiệp vụ liên quan
BR01 (5 vai trò), BR02 (Kiểm soát quyền), BR23 (Audit).
Bảng CSDL tác động
users, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Người dùng
Mở trang đăng nhập, nhập tên đăng nhập (username hoặc email) và mật khẩu, nhấn Đăng nhập.
Hệ thống
Tiếp nhận dữ liệu, chuẩn hóa định danh và tìm kiếm bản ghi trong bảng users.
Hệ thống
So khớp mật khẩu đã mã hóa bằng thuật toán băm (BCrypt) với password_hash.
Hệ thống
Kiểm tra trạng thái tài khoản (is_active).
Hệ thống
Khởi tạo JWT / phiên đăng nhập, nạp role duy nhất và trả về thông tin người dùng an toàn (không lộ mật khẩu).
Người dùng
Hệ thống chuyển hướng người dùng đến Dashboard tương ứng với vai trò của họ.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Sai tài khoản hoặc mật khẩu
Hệ thống hiển thị thông báo lỗi chung 'Tên đăng nhập hoặc mật khẩu không chính xác', không tiết lộ cụ thể trường nào sai. Cho phép nhập lại.
3a. Tài khoản bị khóa (is_active = false)
Hệ thống từ chối đăng nhập và thông báo 'Tài khoản đã bị tạm khóa. Vui lòng liên hệ quản lý (ADMIN)'.
[Heading2] UC02: Đăng xuất
Hình 6.2: Sơ đồ Use Case Diagram — UC02 (Đăng xuất)
Mã & Tên Use Case
UC02 — Đăng xuất
Tác nhân chính (Primary)
Cả 5 vai trò
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Hủy bỏ phiên làm việc hiện tại một cách an toàn và ngăn chặn truy cập trái phép.
Điều kiện kích hoạt (Trigger)
Người dùng nhấn nút 'Đăng xuất' trên thanh điều hướng.
Tiền điều kiện (Preconditions)
Người dùng đang có phiên đăng nhập hợp lệ trong hệ thống.
Hậu điều kiện Thành công
Phiên làm việc phía server bị thu hồi, cookie/token phía client bị xóa, chuyển về trang đăng nhập.
Hậu điều kiện Thất bại
Thông báo lỗi nếu kết nối gián đoạn, nhưng vẫn xóa thông tin xác thực lưu ở trình duyệt.
Quy tắc nghiệp vụ liên quan
BR02 (Kiểm soát quyền), BR23 (Audit).
Bảng CSDL tác động
users, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Người dùng
Chọn chức năng 'Đăng xuất' từ menu hồ sơ cá nhân.
Hệ thống
Xác thực token/phiên hiện tại cùng mã CSRF.
Hệ thống
Đưa token vào danh sách thu hồi (blacklist) hoặc hủy session trên server.
Hệ thống
Ghi nhận sự kiện đăng xuất vào nhật ký hệ thống (audit_events).
Hệ thống
Xóa sạch cookie và trạng thái đăng nhập phía client, điều hướng về màn hình đăng nhập.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Phiên đã hết hạn trước khi đăng xuất
Hệ thống tự động dọn dẹp bộ nhớ cục bộ phía client và chuyển hướng về màn hình đăng nhập.
[Heading2] UC03: Quản lý tài khoản và vai trò
Hình 6.3: Sơ đồ Use Case Diagram — UC03 (Quản lý tài khoản và vai trò)
Mã & Tên Use Case
UC03 — Quản lý tài khoản và vai trò
Tác nhân chính (Primary)
Quản lý và Quản trị viên (ADMIN)
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Tạo mới tài khoản nhân viên, phân đúng 1 vai trò chính, cập nhật thông tin hoặc khóa tài khoản.
Điều kiện kích hoạt (Trigger)
ADMIN vào màn hình Quản lý tài khoản.
Tiền điều kiện (Preconditions)
ADMIN đã đăng nhập, có quyền quản trị toàn hệ thống.
Hậu điều kiện Thành công
Tài khoản được tạo mới hoặc cập nhật trạng thái/vai trò thành công, ghi vết audit.
Hậu điều kiện Thất bại
Dữ liệu không hợp lệ bị từ chối, không thay đổi trạng thái tài khoản.
Quy tắc nghiệp vụ liên quan
BR01 (Chỉ 5 role chuẩn), BR02 (Phân quyền đối tượng), BR23 (Audit), BR24 (Bảo toàn lịch sử không xóa cứng).
Bảng CSDL tác động
users, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
ADMIN
Mở danh sách tài khoản, chọn 'Tạo tài khoản' hoặc chọn một tài khoản để chỉnh sửa.
ADMIN
Nhập thông tin: username, full_name, email, chọn role (ADMIN, RECEPTIONIST, TECHNICIAN, WAREHOUSE_STAFF, CUSTOMER), mật khẩu ban đầu.
Hệ thống
Kiểm tra tính duy nhất của username và email; băm mật khẩu bằng BCrypt.
Hệ thống
Lưu thông tin vào bảng users và ghi nhận sự kiện audit_events.
Hệ thống
Hiển thị thông báo thành công và cập nhật lại danh sách.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Trùng username hoặc email
Hệ thống báo lỗi cụ thể trường bị trùng, yêu cầu đổi giá trị khác.
3a. Khóa tài khoản nhân viên cũ
ADMIN chọn 'Khóa tài khoản' (is_active = false) thay vì xóa bản ghi vật lý để bảo toàn khóa ngoại lịch sử.
[Heading2] UC04: Quản lý hồ sơ khách hàng
Hình 6.4: Sơ đồ Use Case Diagram — UC04 (Quản lý hồ sơ khách hàng)
Mã & Tên Use Case
UC04 — Quản lý hồ sơ khách hàng
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER)
Mục tiêu (Goal)
Tạo mới, cập nhật hồ sơ khách hàng cá nhân hoặc doanh nghiệp, quản lý chính sách công nợ.
Điều kiện kích hoạt (Trigger)
Khách hàng liên hệ hoặc đến xưởng trực tiếp.
Tiền điều kiện (Preconditions)
Nhân viên đã đăng nhập và có quyền quản lý hồ sơ khách hàng.
Hậu điều kiện Thành công
Hồ sơ khách hàng được lưu trữ với mã khách hàng duy nhất; sẵn sàng tiếp nhận hoặc tạo xe.
Hậu điều kiện Thất bại
Thông báo lỗi nếu sai định dạng điện thoại hoặc vi phạm dữ liệu.
Quy tắc nghiệp vụ liên quan
BR02 (Bảo mật hồ sơ), BR03 (Khách không bắt buộc có user), BR04 (Khách hàng hiện tại vs lịch sử), BR19 (Hạn mức tín dụng).
Bảng CSDL tác động
customers, users, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Nhập số điện thoại khách hàng để tra cứu trên hệ thống.
Hệ thống
Tìm kiếm trong bảng customers; nếu chưa có, mở biểu mẫu tạo mới hồ sơ.
Lễ tân
Nhập họ tên, điện thoại, email, địa chỉ, loại khách (INDIVIDUAL/BUSINESS), công ty, MST, hạn nợ, hạn mức nợ.
Hệ thống
Kiểm tra định dạng số điện thoại, gán mã khách hàng tự sinh (customer_code), lưu vào bảng customers.
Hệ thống
Trả về thông tin hồ sơ khách hàng để tiếp tục quy trình tiếp nhận.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Khách hàng đã tồn tại
Hệ thống hiển thị hồ sơ hiện tại và danh sách xe đã đăng ký; Lễ tân có thể cập nhật thông tin nếu có thay đổi.
3a. Khách tự đăng ký tài khoản online
Hệ thống tự động liên kết user_id của tài khoản mới với hồ sơ khách hàng (BR03).
[Heading2] UC05: Quản lý phương tiện
Hình 6.5: Sơ đồ Use Case Diagram — UC05 (Quản lý phương tiện)
Mã & Tên Use Case
UC05 — Quản lý phương tiện
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER)
Mục tiêu (Goal)
Quản lý danh sách xe, biển số, số khung (VIN), thông số kỹ thuật và chủ sở hữu hiện tại.
Điều kiện kích hoạt (Trigger)
Khách hàng đăng ký xe mới hoặc mang xe đến gara lần đầu.
Tiền điều kiện (Preconditions)
Khách hàng chủ sở hữu đã có hồ sơ trong bảng customers.
Hậu điều kiện Thành công
Xe được lưu vào hệ thống với biển số chuẩn hóa duy nhất và gắn đúng customer_id.
Hậu điều kiện Thất bại
Báo lỗi trùng biển số hoặc VIN không hợp lệ.
Quy tắc nghiệp vụ liên quan
BR02 (Quyền sở hữu xe), BR04 (Chủ xe hiện tại vs Lịch sử giao dịch).
Bảng CSDL tác động
vehicles, customers, repair_orders
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Tra cứu biển số xe trên hệ thống.
Hệ thống
Xác nhận biển số chưa tồn tại; mở biểu mẫu thêm mới xe.
Lễ tân
Nhập thông tin: biển số, VIN, số máy, hãng, model, năm sản xuất, màu, loại nhiên liệu, ODO hiện tại, hạn đăng kiểm.
Hệ thống
Chuẩn hóa chuỗi biển số (bỏ dấu cách, viết hoa), kiểm tra tính hợp lệ của mốc đăng kiểm.
Hệ thống
Lưu thông tin vào bảng vehicles với customer_id của chủ xe hiện tại.
Hệ thống
Hiển thị thông tin xe trong danh sách phương tiện của khách hàng.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Xe đã tồn tại nhưng đổi chủ sở hữu
Lễ tân cập nhật customer_id mới trong bảng vehicles. Hệ thống giữ nguyên customer_id lịch sử trong các phiếu sửa chữa và hóa đơn cũ (BR04).
3a. ODO mới nhỏ hơn ODO lịch sử
Hệ thống cảnh báo bất thường số km để Lễ tân đối chiếu thực tế trước khi xác nhận.
[Heading2] UC06: Quản lý dịch vụ và gói
Hình 6.6: Sơ đồ Use Case Diagram — UC06 (Quản lý dịch vụ và gói)
Mã & Tên Use Case
UC06 — Quản lý dịch vụ và gói
Tác nhân chính (Primary)
Quản lý và Quản trị viên (ADMIN)
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Thiết lập danh mục dịch vụ tiền công, định mức thời gian, yêu cầu khoang/kỹ năng và các gói bảo dưỡng định kỳ.
Điều kiện kích hoạt (Trigger)
Gara cập nhật bảng giá hoặc bổ sung dịch vụ mới.
Tiền điều kiện (Preconditions)
Người dùng đăng nhập với quyền ADMIN.
Hậu điều kiện Thành công
Dịch vụ/gói bảo dưỡng được lưu trữ và có hiệu lực cho các báo giá/lịch hẹn mới.
Hậu điều kiện Thất bại
Báo lỗi nếu trùng mã dịch vụ hoặc giá âm.
Quy tắc nghiệp vụ liên quan
BR05 (Cấu hình dịch vụ/gói), BR18 (Giá snapshot không đổi khi danh mục đổi), BR24 (Bảo toàn dữ liệu).
Bảng CSDL tác động
services, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
ADMIN
Mở danh mục dịch vụ, chọn 'Thêm dịch vụ' hoặc 'Thêm gói bảo dưỡng'.
ADMIN
Nhập: mã dịch vụ, tên, loại (SINGLE/PACKAGE), đơn giá tiền công, thời gian chuẩn (phút), loại khoang yêu cầu, kỹ năng yêu cầu (JSON), chu kỳ km/tháng, điều kiện bảo hành.
Hệ thống
Kiểm tra tính duy nhất của service_code, giá tiền không âm, các mốc chu kỳ hợp lệ.
Hệ thống
Lưu vào bảng services; cập nhật trạng thái is_active = true.
Hệ thống
Hiển thị dịch vụ mới trên bảng giá hệ thống.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Ngừng cung cấp dịch vụ cũ
ADMIN chuyển is_active = false. Dịch vụ không hiển thị khi lập báo giá mới nhưng vẫn giữ nguyên trong các phiếu sửa chữa lịch sử (BR24).
[Heading2] UC07: Đặt lịch hẹn
Hình 6.7: Sơ đồ Use Case Diagram — UC07 (Đặt lịch hẹn)
Mã & Tên Use Case
UC07 — Đặt lịch hẹn
Tác nhân chính (Primary)
Khách hàng (CUSTOMER)
Tác nhân phụ (Secondary)
Lễ tân (RECEPTIONIST) thực hiện thay khách
Mục tiêu (Goal)
Đặt trước lịch hẹn dịch vụ, tự động kiểm tra năng lực phục vụ và tạm giữ khoang sửa chữa.
Điều kiện kích hoạt (Trigger)
Khách hàng có nhu cầu bảo dưỡng/sửa chữa xe trước khi đến.
Tiền điều kiện (Preconditions)
Xe đã đăng ký trong hệ thống; gara có cấu hình giờ làm việc và khoang hoạt động.
Hậu điều kiện Thành công
Lịch hẹn được tạo ở trạng thái PENDING/CONFIRMED, tạm giữ khoang trong bay_reservations.
Hậu điều kiện Thất bại
Thông báo hết khoang hoặc xung đột giờ hẹn, yêu cầu chọn khung giờ khác.
Quy tắc nghiệp vụ liên quan
BR05 (Một dịch vụ chính), BR06 (Không trùng khoang), BR07 (Giải phóng tài nguyên), BR23 (Thông báo).
Bảng CSDL tác động
bookings, bay_reservations, service_bays, services, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Khách hàng
Chọn xe của mình, chọn dịch vụ chính (hoặc nhập mô tả nhu cầu) và chọn ngày giờ hẹn đến.
Hệ thống
Tính toán thời gian dự kiến kết thúc dựa trên standard_minutes của dịch vụ.
Hệ thống
Khóa hàng tài nguyên khoang (Pessimistic lock) và quét tình trạng trống trong khung giờ [starts_at, ends_at).
Hệ thống
Tạo bản ghi trong bookings (status = PENDING/CONFIRMED).
Hệ thống
Tạo bản ghi giữ chỗ trong bay_reservations (reservation_type = 'BOOKING', status = 'HELD', có expires_at).
Hệ thống
Tạo thông báo trong notifications cho khách hàng và Lễ tân.
Khách hàng
Nhận mã đặt lịch và thông báo xác nhận thành công.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Khung giờ đã kín khoang
Hệ thống thông báo khung giờ đã kín và gợi ý 3 khung giờ gần nhất còn khoang trống phù hợp.
1a. Lễ tân đặt lịch thay khách gọi điện
Lễ tân chọn khách hàng, chọn xe và thao tác đặt lịch thay mặt khách hàng.
[Heading2] UC08: Xác nhận dời hoặc hủy lịch
Hình 6.8: Sơ đồ Use Case Diagram — UC08 (Xác nhận dời hoặc hủy lịch)
Mã & Tên Use Case
UC08 — Xác nhận dời hoặc hủy lịch
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER) hủy/dời lịch của mình
Mục tiêu (Goal)
Xác nhận lịch hẹn đã đặt, đổi khung giờ hẹn hoặc hủy lịch và lập tức giải phóng khoang.
Điều kiện kích hoạt (Trigger)
Lễ tân duyệt lịch hoặc khách hàng báo hoãn/hủy lịch.
Tiền điều kiện (Preconditions)
Lịch hẹn đang ở trạng thái PENDING hoặc CONFIRMED.
Hậu điều kiện Thành công
Lịch hẹn cập nhật trạng thái mới (CONFIRMED/CANCELLED/NO_SHOW); khoang được giải phóng hoặc dời nguyên tử.
Hậu điều kiện Thất bại
Khung giờ mới bị trùng khoang hoặc lịch đã tiếp nhận không thể hủy tùy tiện.
Quy tắc nghiệp vụ liên quan
BR06 (Khóa khoang), BR07 (Giải phóng tài nguyên nguyên tử), BR23 (Thông báo).
Bảng CSDL tác động
bookings, bay_reservations, notifications, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Mở danh sách lịch hẹn, chọn lịch cần xử lý (Xác nhận / Dời lịch / Hủy lịch).
Hệ thống
Kiểm tra phiên bản dữ liệu (optimistic locking) và trạng thái hiện tại của booking.
Lễ tân
Chọn thao tác (nếu Dời lịch: chọn ngày giờ mới; nếu Hủy: nhập lý do hủy).
Hệ thống
Thực hiện giao dịch nguyên tử: cập nhật booking.status; giải phóng bay_reservations cũ; nếu dời lịch thì kiểm tra và tạo reservation mới.
Hệ thống
Gửi thông báo cập nhật lịch hẹn cho khách hàng qua notifications.
Lễ tân
Nhận thông báo cập nhật thành công.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Khách hàng không đến (Quá giờ hẹn)
Lễ tân chuyển trạng thái booking sang NO_SHOW, hệ thống tự động giải phóng khoang giữ chỗ.
3b. Khách hàng tự hủy trên cổng online
Khách hàng chọn 'Hủy lịch hẹn' trước giờ hẹn ít nhất 2 tiếng, hệ thống tự động giải phóng khoang.
[Heading2] UC09: Tiếp nhận xe và hiện trạng
Hình 6.9: Sơ đồ Use Case Diagram — UC09 (Tiếp nhận xe và hiện trạng)
Mã & Tên Use Case
UC09 — Tiếp nhận xe và hiện trạng
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER)
Mục tiêu (Goal)
Khởi tạo hồ sơ phiếu sửa chữa ngay khi xe vào xưởng, ghi nhận ODO, mức xăng, tình trạng thân vỏ và checklist ảnh.
Điều kiện kích hoạt (Trigger)
Xe ô tô di chuyển vào khu vực tiếp nhận của xưởng gara.
Tiền điều kiện (Preconditions)
Khách hàng và xe đã có trong hệ thống (hoặc tạo nhanh tại chỗ).
Hậu điều kiện Thành công
Bản ghi repair_orders được tạo ở trạng thái NEW; lưu checklist tiếp nhận và tệp ảnh đính kèm.
Hậu điều kiện Thất bại
Báo lỗi nếu thiếu số ODO, sai xe hoặc trùng lặp phiếu đang mở.
Quy tắc nghiệp vụ liên quan
BR03 (Khách không cần user), BR04 (Chốt khách lịch sử), BR08 (Tạo phiếu từ tiếp nhận), BR16 (Checklist & Ảnh).
Bảng CSDL tác động
repair_orders, vehicle_checks, attachments, bookings, customers, vehicles
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Nhập biển số xe tiếp nhận; nếu xe có đặt lịch trước, chọn mã booking tương ứng.
Hệ thống
Tải thông tin xe và hồ sơ chủ xe; xác thực booking thuộc đúng xe đó.
Lễ tân
Nhập ODO tiếp nhận, mức nhiên liệu (%), ghi nhận phản ánh sự cố của khách hàng.
Lễ tân
Thực hiện kiểm tra hiện trạng xe theo biểu mẫu checklist (vết trầy xước, đèn, lốp, đồ đạc trên xe) và chụp ảnh tải lên.
Hệ thống
Tạo bản ghi repair_orders (status = 'NEW', received_by = Lễ tân, customer_id chốt snapshot).
Hệ thống
Lưu checklist vào vehicle_checks (check_type = 'CHECK_IN') và lưu metadata ảnh vào attachments.
Hệ thống
Nếu có booking, cập nhật booking.status = 'RECEIVED'.
Lễ tân
In biên bản tiếp nhận bàn giao cho khách hàng ký xác nhận.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Khách đến trực tiếp không đặt trước
Lễ tân tiếp nhận với booking_id = NULL. Hệ thống vẫn tạo repair_orders và kiểm tra hiện trạng đầy đủ (BR08).
[Heading2] UC10: Điều phối xe đến khoang
Hình 6.10: Sơ đồ Use Case Diagram — UC10 (Điều phối xe đến khoang)
Mã & Tên Use Case
UC10 — Điều phối xe đến khoang
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Kỹ thuật viên (TECHNICIAN)
Mục tiêu (Goal)
Chỉ định khoang kỹ thuật phù hợp cho xe và cập nhật trạng thái sử dụng khoang.
Điều kiện kích hoạt (Trigger)
Xe tiếp nhận xong hoặc chuyển đổi công đoạn sửa chữa/sơn/rửa xe.
Tiền điều kiện (Preconditions)
Phiếu sửa chữa đang mở; khoang dự kiến còn trống và không bảo trì.
Hậu điều kiện Thành công
Tạo hoặc cập nhật bay_reservations loại 'REPAIR' liên kết đúng repair_order_id; khoang chuyển 'IN_USE'.
Hậu điều kiện Thất bại
Báo lỗi xung đột khoang hoặc khoang đang bảo trì.
Quy tắc nghiệp vụ liên quan
BR06 (Không trùng khoang), BR07 (Giải phóng tài nguyên), BR24 (Audit điều phối).
Bảng CSDL tác động
service_bays, bay_reservations, repair_orders, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Mở sơ đồ mặt bằng khoang sửa chữa thời gian thực.
Lễ tân
Chọn xe/phiếu sửa chữa và chọn khoang đích (loại khoang phù hợp: cầu nâng, điện, đồng sơn...).
Hệ thống
Khóa bản ghi khoang; kiểm tra điều kiện không bị giao nhau với lịch khác.
Hệ thống
Tạo bản ghi bay_reservations (reservation_type = 'REPAIR', status = 'IN_USE', repair_order_id chốt).
Hệ thống
Giải phóng khoang trước đó nếu xe di chuyển giữa các khoang.
Lễ tân
Thông báo cho Kỹ thuật viên lái xe vào khoang chỉ định.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Khoang bị bảo trì đột xuất
Hệ thống chặn điều phối và cảnh báo khoang ở trạng thái MAINTENANCE.
[Heading2] UC11: Phân công kỹ thuật viên
Hình 6.11: Sơ đồ Use Case Diagram — UC11 (Phân công kỹ thuật viên)
Mã & Tên Use Case
UC11 — Phân công kỹ thuật viên
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Kỹ thuật viên (TECHNICIAN)
Mục tiêu (Goal)
Phân công thợ chính (MAIN), thợ phụ (ASSISTANT) hoặc người kiểm tra (QC) cho từng công việc.
Điều kiện kích hoạt (Trigger)
Hạng mục công việc đã được duyệt hoặc cần kiểm tra xe.
Tiền điều kiện (Preconditions)
Task tồn tại; nhân viên đang có ca làm việc hoạt động (employee_shifts) và đủ kỹ năng.
Hậu điều kiện Thành công
Bản ghi task_assignments được tạo hợp lệ; chỉ duy nhất 1 thợ chính active cho mỗi task.
Hậu điều kiện Thất bại
Báo lỗi nếu nhân viên không có ca, thiếu kỹ năng hoặc trùng 2 thợ chính.
Quy tắc nghiệp vụ liên quan
BR09 (Điều kiện phân công & 1 Thợ chính), BR10 (Timer), BR24 (Bảo toàn lịch sử phân công).
Bảng CSDL tác động
repair_tasks, task_assignments, employees, employee_shifts, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Mở danh sách công việc cần phân công của phiếu sửa chữa.
Hệ thống
Gợi ý danh sách nhân viên kỹ thuật phù hợp theo kỹ năng (skills_json) và ca làm việc (employee_shifts).
Lễ tân
Chọn nhân viên, chọn vai trò (MAIN, ASSISTANT, QC), nhập thời gian dự kiến bắt đầu/kết thúc.
Hệ thống
Kiểm tra ràng buộc: một task chỉ có tối đa 1 phân công MAIN đang active (BR09).
Hệ thống
Lưu vào bảng task_assignments với is_active = true.
Hệ thống
Gửi thông báo công việc mới đến Kỹ thuật viên qua kênh WEB (notifications).
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Thay đổi người thực hiện
Lễ tân thu hồi phân công cũ (đặt is_active = false) và tạo phân công mới. Các labor_sessions cũ vẫn được bảo lưu (BR24).
[Heading2] UC12: Ghi chẩn đoán kỹ thuật
Hình 6.12: Sơ đồ Use Case Diagram — UC12 (Ghi chẩn đoán kỹ thuật)
Mã & Tên Use Case
UC12 — Ghi chẩn đoán kỹ thuật
Tác nhân chính (Primary)
Kỹ thuật viên (TECHNICIAN)
Tác nhân phụ (Secondary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Mục tiêu (Goal)
Ghi nhận kết quả kiểm tra kỹ thuật, chẩn đoán nguyên nhân hư hỏng và đề xuất phương án xử lý.
Điều kiện kích hoạt (Trigger)
Kỹ thuật viên hoàn thành kiểm tra sơ bộ xe tại khoang.
Tiền điều kiện (Preconditions)
Kỹ thuật viên được phân công công việc kiểm tra của phiếu sửa chữa.
Hậu điều kiện Thành công
Nội dung chẩn đoán được lưu vào repair_orders.diagnosis; ghi vết audit.
Hậu điều kiện Thất bại
Từ chối nếu người thực hiện không được phân công hoặc phiếu đã đóng.
Quy tắc nghiệp vụ liên quan
BR02 (Quyền KTV), BR08 (Hồ sơ phiếu), BR12 (Căn cứ lập báo giá).
Bảng CSDL tác động
repair_orders, attachments, audit_events, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Kỹ thuật viên
Mở phiếu sửa chữa trên giao diện làm việc của KTV.
Kỹ thuật viên
Nhập kết quả chẩn đoán: hiện tượng, nguyên nhân gốc rễ, mức độ rủi ro, đề xuất công việc và phụ tùng cần thay.
Kỹ thuật viên
Tải kèm hình ảnh/video bằng chứng vị trí hỏng hóc (nếu có).
Hệ thống
Cập nhật trường repair_orders.diagnosis; lưu tệp vào attachments.
Hệ thống
Ghi nhận sự kiện vào audit_events và thông báo cho Lễ tân lập báo giá.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Phát hiện hư hỏng phát sinh sau khi đã tháo dỡ linh kiện
Kỹ thuật viên ghi chú bổ sung; hệ thống kích hoạt điểm mở rộng cho Use Case UC15 (Lập báo giá phát sinh).
[Heading2] UC13: Lập và gửi báo giá
Hình 6.13: Sơ đồ Use Case Diagram — UC13 (Lập và gửi báo giá)
Mã & Tên Use Case
UC13 — Lập và gửi báo giá
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER)
Mục tiêu (Goal)
Lập bảng báo giá chi tiết gồm tiền công và linh kiện phụ tùng, tính thuế/giảm giá và gửi khách hàng.
Điều kiện kích hoạt (Trigger)
Đã có kết quả chẩn đoán kỹ thuật từ KTV.
Tiền điều kiện (Preconditions)
Phiếu sửa chữa đang mở; danh mục dịch vụ và phụ tùng có sẵn giá hiện hành.
Hậu điều kiện Thành công
Báo giá (estimates) và các dòng (estimate_items) được tạo ở trạng thái SENT; gửi thông báo duyệt cho khách.
Hậu điều kiện Thất bại
Báo lỗi nếu số lượng <= 0, giảm giá vượt tổng tiền hoặc dòng thiếu định danh.
Quy tắc nghiệp vụ liên quan
BR12 (Phiên bản báo giá), BR13 (Bóc tách dòng Service/Part), BR18 (Tính toán tiền tệ), BR23 (Thông báo).
Bảng CSDL tác động
estimates, estimate_items, repair_orders, services, parts, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Mở phiếu sửa chữa, chọn 'Tạo báo giá' (estimate_kind = 'INITIAL').
Lễ tân
Thêm từng dòng báo giá: chọn loại SERVICE (tiền công) hoặc PART (phụ tùng thay thế), nhập số lượng, đơn giá chốt.
Lễ tân
Nhập mức giảm giá (discount_amount) và thuế (tax_amount) cấp chứng từ.
Hệ thống
Khóa phiếu sửa chữa, cấp version_no tự tăng cho phiếu; tính line_total từng dòng và total_amount của báo giá.
Hệ thống
Lưu estimates và estimate_items; chuyển status = 'SENT', ghi nhận sent_at.
Hệ thống
Gửi thông báo qua WEB/Email kèm đường dẫn duyệt trực tuyến đến Khách hàng.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Báo giá thay thế cho bản chưa duyệt
Hệ thống đánh dấu phiên bản cũ là SUPERSEDED và tạo phiên bản mới với version_no kế tiếp (BR12).
[Heading2] UC14: Khách duyệt từng hạng mục
Hình 6.14: Sơ đồ Use Case Diagram — UC14 (Khách duyệt từng hạng mục)
Mã & Tên Use Case
UC14 — Khách duyệt từng hạng mục
Tác nhân chính (Primary)
Khách hàng (CUSTOMER)
Tác nhân phụ (Secondary)
Lễ tân (RECEPTIONIST) ghi nhận thay nếu khách xác nhận trực tiếp
Mục tiêu (Goal)
Khách hàng xem chi tiết và phê duyệt từng hạng mục (đồng ý, duyệt một phần hoặc từ chối).
Điều kiện kích hoạt (Trigger)
Khách hàng nhận được thông báo báo giá hoặc xem trên Cổng khách hàng.
Tiền điều kiện (Preconditions)
Báo giá đang ở trạng thái SENT và thuộc đúng khách hàng sở hữu xe.
Hậu điều kiện Thành công
Báo giá cập nhật status (APPROVED / PARTIALLY_APPROVED / REJECTED); dòng APPROVED tự động sinh task và giữ phụ tùng.
Hậu điều kiện Thất bại
Báo lỗi nếu phiên bản báo giá đã bị thay thế hoặc người duyệt không phải chủ xe.
Quy tắc nghiệp vụ liên quan
BR02 (Quyền duyệt), BR12 (Version), BR13 (Chỉ APPROVED mới sinh việc), BR14 (Đồng bộ tồn giữ), BR23 (Audit).
Bảng CSDL tác động
estimates, estimate_items, repair_tasks, parts, repair_orders, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Khách hàng
Mở liên kết báo giá từ thông báo hoặc ứng dụng.
Hệ thống
Xác thực danh tính chủ sở hữu xe và tải phiên bản báo giá mới nhất.
Khách hàng
Xem xét từng dòng và chọn: Đồng ý (APPROVED) hoặc Từ chối (REJECTED), nhập lý do nếu từ chối.
Khách hàng
Nhấn 'Xác nhận phê duyệt'.
Hệ thống
Khóa bản ghi báo giá; ghi decided_by, decided_at cho từng dòng.
Hệ thống
Cập nhật trạng thái báo giá: APPROVED (nếu duyệt hết) hoặc PARTIALLY_APPROVED (duyệt 1 phần).
Hệ thống
Với mỗi dòng SERVICE được APPROVED: tự động sinh 1 bản ghi repair_tasks.
Hệ thống
Với mỗi dòng PART được APPROVED: kích hoạt giữ hàng trong kho (UC23).
Hệ thống
Gửi thông báo cho Lễ tân và Kỹ thuật viên để bắt đầu triển khai.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Khách hàng từ chối toàn bộ
Báo giá chuyển REJECTED; Lễ tân liên hệ tìm phương án khác hoặc đóng phiếu trả xe.
[Heading2] UC15: Lập báo giá phát sinh
Hình 6.15: Sơ đồ Use Case Diagram — UC15 (Lập báo giá phát sinh)
Mã & Tên Use Case
UC15 — Lập báo giá phát sinh
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Kỹ thuật viên (cung cấp bằng chứng), Khách hàng (duyệt)
Mục tiêu (Goal)
Lập bổ sung báo giá cho các hư hỏng mới phát hiện trong quá trình sửa chữa mà không ghi đè báo giá cũ.
Điều kiện kích hoạt (Trigger)
Kỹ thuật viên phát hiện thêm sự cố sau khi tháo dỡ kiểm tra chi tiết.
Tiền điều kiện (Preconditions)
Phiếu sửa chữa đang ở trạng thái IN_PROGRESS; báo giá ban đầu đã được duyệt.
Hậu điều kiện Thành công
Báo giá mới loại SUPPLEMENTAL được tạo, trỏ parent_estimate_id về báo giá gốc, sẵn sàng gửi khách duyệt.
Hậu điều kiện Thất bại
Từ chối nếu parent_estimate_id không thuộc cùng phiếu sửa chữa.
Quy tắc nghiệp vụ liên quan
BR12 (Báo giá bổ sung có parent cùng phiếu), BR13 (Duyệt theo dòng), BR23 (Audit).
Bảng CSDL tác động
estimates, estimate_items, repair_orders, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Mở phiếu sửa chữa, chọn 'Lập báo giá phát sinh'.
Hệ thống
Thiết lập estimate_kind = 'SUPPLEMENTAL' và gán parent_estimate_id trỏ về báo giá trước đó.
Lễ tân
Nhập các hạng mục dịch vụ và phụ tùng phát sinh kèm hình ảnh/lý do kỹ thuật.
Hệ thống
Tính toán subtotal, thuế, tổng tiền cho riêng phần bổ sung; lưu estimates và estimate_items mới.
Hệ thống
Chuyển status = 'SENT' và gửi thông báo yêu cầu khách hàng duyệt phần phát sinh.
Khách hàng
Thực hiện duyệt theo quy trình UC14 đối với riêng các hạng mục phát sinh.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
6a. Khách từ chối hạng mục phát sinh
Chỉ hủy các hạng mục bổ sung; các công việc đã duyệt ở báo giá gốc vẫn tiếp tục thực hiện.
[Heading2] UC16: Bắt đầu công việc và timer
Hình 6.16: Sơ đồ Use Case Diagram — UC16 (Bắt đầu công việc và timer)
Mã & Tên Use Case
UC16 — Bắt đầu công việc và timer
Tác nhân chính (Primary)
Kỹ thuật viên (TECHNICIAN)
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Bắt đầu bấm giờ ghi nhận thời gian làm việc thực tế cho một công việc được phân công.
Điều kiện kích hoạt (Trigger)
Kỹ thuật viên bắt đầu bắt tay vào sửa chữa hạng mục.
Tiền điều kiện (Preconditions)
KTV có phân công active cho task; KTV không có phiên timer nào khác đang chạy.
Hậu điều kiện Thành công
Tạo bản ghi labor_sessions với ends_at = NULL; task chuyển IN_PROGRESS; đồng hồ bắt đầu chạy.
Hậu điều kiện Thất bại
Chặn lại nếu KTV đang có timer ở công việc khác chưa dừng.
Quy tắc nghiệp vụ liên quan
BR09 (Đúng người được giao), BR10 (Mỗi KTV chỉ chạy tối đa 1 timer tại một thời điểm), BR11 (Giờ công máy chủ).
Bảng CSDL tác động
labor_sessions, repair_tasks, task_assignments, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Kỹ thuật viên
Mở danh sách 'Công việc của tôi', chọn công việc được giao và nhấn 'Bắt đầu'.
Hệ thống
Kiểm tra quyền phân công trong task_assignments.
Hệ thống
Kiểm tra ràng buộc active_employee_id: đảm bảo KTV không có phiên nào đang mở (ends_at IS NULL) (BR10).
Hệ thống
Lấy thời gian chuẩn UTC từ máy chủ làm starts_at; tạo bản ghi mới trong labor_sessions.
Hệ thống
Cập nhật repair_tasks.status = 'IN_PROGRESS' (nếu đang PENDING).
Hệ thống
Hiển thị đồng hồ đếm thời gian thực trên màn hình KTV.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. KTV bấm nhầm nút bắt đầu 2 lần liên tiếp
Hệ thống sử dụng cơ chế chống trùng request_key để trả về phiên hiện tại, không tạo bản ghi đúp.
[Heading2] UC17: Tạm dừng công việc
Hình 6.17: Sơ đồ Use Case Diagram — UC17 (Tạm dừng công việc)
Mã & Tên Use Case
UC17 — Tạm dừng công việc
Tác nhân chính (Primary)
Kỹ thuật viên (TECHNICIAN)
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Tạm dừng bấm giờ khi nghỉ trưa, chuyển ca hoặc chờ linh kiện phụ tùng.
Điều kiện kích hoạt (Trigger)
Kỹ thuật viên ngừng thao tác kỹ thuật trên xe.
Tiền điều kiện (Preconditions)
Đang có bản ghi labor_sessions mở (ends_at IS NULL) của chính KTV đó.
Hậu điều kiện Thành công
Đóng phiên làm việc hiện tại, ghi ends_at và stop_reason; không tính thời gian nghỉ vào giờ công.
Hậu điều kiện Thất bại
Báo lỗi nếu không có phiên đang chạy.
Quy tắc nghiệp vụ liên quan
BR10 (Đóng timer), BR11 (Thời gian tạm dừng bị loại khỏi giờ công tính lương/hiệu suất).
Bảng CSDL tác động
labor_sessions, repair_tasks, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Kỹ thuật viên
Nhấn nút 'Tạm dừng' trên giao diện đếm giờ công việc.
Kỹ thuật viên
Chọn lý do tạm dừng: PAUSE (nghỉ ngơi/chuyển việc) hoặc WAITING_PARTS (chờ phụ tùng).
Hệ thống
Lấy thời gian hiện tại từ server ghi vào ends_at; gán stop_reason tương ứng.
Hệ thống
Cập nhật active_employee_id = NULL để giải phóng cờ bận cho KTV.
Hệ thống
Nếu lý do là WAITING_PARTS, cập nhật repair_tasks.status = 'WAITING_PARTS'.
Hệ thống
Dừng đồng hồ và hiển thị tổng thời gian đã tích lũy của công việc.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Mất kết nối mạng khi nhấn tạm dừng
Hệ thống ghi nhận thời điểm yêu cầu từ client và đồng bộ đối soát với server khi có mạng lại.
[Heading2] UC18: Tiếp tục công việc
Hình 6.18: Sơ đồ Use Case Diagram — UC18 (Tiếp tục công việc)
Mã & Tên Use Case
UC18 — Tiếp tục công việc
Tác nhân chính (Primary)
Kỹ thuật viên (TECHNICIAN)
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Bấm giờ tiếp tục làm việc sau khi tạm dừng, tạo một khoảng làm việc mới.
Điều kiện kích hoạt (Trigger)
Kỹ thuật viên quay lại làm việc sau thời gian dừng.
Tiền điều kiện (Preconditions)
Task đang ở trạng thái PAUSED hoặc WAITING_PARTS; KTV không có timer mở khác.
Hậu điều kiện Thành công
Tạo phiên labor_sessions mới độc lập; nối tiếp tích lũy giờ công.
Hậu điều kiện Thất bại
Báo lỗi nếu KTV đang bận ở công việc khác.
Quy tắc nghiệp vụ liên quan
BR10 (Chỉ 1 timer), BR11 (Tổng giờ công là tổng độ dài các phiên labor_sessions).
Bảng CSDL tác động
labor_sessions, repair_tasks, task_assignments
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Kỹ thuật viên
Mở công việc đang tạm dừng, nhấn 'Tiếp tục'.
Hệ thống
Kiểm tra KTV không có timer mở khác.
Hệ thống
Tạo bản ghi labor_sessions mới với starts_at = giờ server, ends_at = NULL.
Hệ thống
Cập nhật repair_tasks.status = 'IN_PROGRESS'.
Hệ thống
Đồng hồ hiển thị tiếp tục chạy, cộng dồn các khoảng làm việc trước đó.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Đổi thợ khác tiếp tục làm
Thợ mới được phân công (task_assignments) có thể bấm tiếp tục; hệ thống tạo labor_sessions gắn employee_id của thợ mới.
[Heading2] UC19: Hoàn thành công việc
Hình 6.19: Sơ đồ Use Case Diagram — UC19 (Hoàn thành công việc)
Mã & Tên Use Case
UC19 — Hoàn thành công việc
Tác nhân chính (Primary)
Kỹ thuật viên (TECHNICIAN)
Tác nhân phụ (Secondary)
Quản lý / ADMIN duyệt ngoại lệ
Mục tiêu (Goal)
Đóng phiên làm việc cuối cùng, chốt hoàn thành công việc và sẵn sàng cho khâu QC.
Điều kiện kích hoạt (Trigger)
Kỹ thuật viên thi công xong toàn bộ nội dung công việc.
Tiền điều kiện (Preconditions)
Task đang IN_PROGRESS; đã hoàn tất các thao tác kỹ thuật.
Hậu điều kiện Thành công
Đóng timer cuối cùng với stop_reason = 'COMPLETE'; task chuyển COMPLETED; cập nhật completed_at.
Hậu điều kiện Thất bại
Báo lỗi nếu còn phụ tùng chưa xuất đủ hoặc KTV khác vẫn đang chạy timer trên task.
Quy tắc nghiệp vụ liên quan
BR11 (Chốt giờ công), BR17 (Phải xong task mới được QC), BR24 (Bảo toàn dữ liệu).
Bảng CSDL tác động
repair_tasks, labor_sessions, repair_orders, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Kỹ thuật viên
Nhấn 'Hoàn thành công việc' trên giao diện.
Kỹ thuật viên
Nhập ghi chú kỹ thuật, kết quả đo đạc (nếu có).
Hệ thống
Đóng timer hiện tại của KTV với stop_reason = 'COMPLETE'.
Hệ thống
Kiểm tra xem còn thợ phụ nào đang mở timer trên task này không; nếu không còn, chuyển repair_tasks.status = 'COMPLETED', ghi completed_at.
Hệ thống
Kiểm tra nếu toàn bộ task trong phiếu đã COMPLETED: cập nhật repair_orders.status = 'QC_PENDING'.
Hệ thống
Gửi thông báo cho KTV có quyền QC để tiến hành nghiệm thu chất lượng.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Còn thợ phụ đang chạy timer
Hệ thống chỉ đóng timer của KTV thực hiện và nhắc nhở thợ phụ cần đóng phiên trước khi chốt task hoàn thành.
[Heading2] UC20: Quản lý nhà cung cấp
Hình 6.20: Sơ đồ Use Case Diagram — UC20 (Quản lý nhà cung cấp)
Mã & Tên Use Case
UC20 — Quản lý nhà cung cấp
Tác nhân chính (Primary)
Quản lý kho (WAREHOUSE_STAFF), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Quản lý danh sách nhà cung cấp phụ tùng, thông tin liên hệ và chính sách hạn nợ thanh toán.
Điều kiện kích hoạt (Trigger)
Ký kết hợp đồng cung ứng mới hoặc cập nhật đối tác.
Tiền điều kiện (Preconditions)
Người dùng đăng nhập với quyền Quản lý kho hoặc ADMIN.
Hậu điều kiện Thành công
Bản ghi suppliers được lưu trữ với supplier_code duy nhất.
Hậu điều kiện Thất bại
Báo lỗi nếu trùng mã nhà cung cấp hoặc sai định dạng MST.
Quy tắc nghiệp vụ liên quan
BR20 (Chi nhà cung cấp), BR24 (Bảo toàn lịch sử quan hệ).
Bảng CSDL tác động
suppliers, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Quản lý kho
Mở danh mục Nhà cung cấp, chọn 'Thêm mới'.
Quản lý kho
Nhập thông tin: mã NCC, tên công ty, số điện thoại, email, địa chỉ, MST, người liên hệ, số ngày được nợ (payment_term_days).
Hệ thống
Kiểm tra tính duy nhất của supplier_code; kiểm tra payment_term_days >= 0.
Hệ thống
Lưu vào bảng suppliers; hiển thị trên danh sách đối tác.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Ngừng hợp tác
Đổi is_active = false. Không xóa cứng bản ghi để bảo toàn lịch sử hóa đơn mua và sổ kho (BR24).
[Heading2] UC21: Quản lý phụ tùng vật tư
Hình 6.21: Sơ đồ Use Case Diagram — UC21 (Quản lý phụ tùng vật tư)
Mã & Tên Use Case
UC21 — Quản lý phụ tùng vật tư
Tác nhân chính (Primary)
Quản lý kho (WAREHOUSE_STAFF), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Không có
Mục tiêu (Goal)
Khai báo SKU phụ tùng, mã OEM, đơn vị tính, vị trí kho, giá niêm yết và định mức tồn tối thiểu.
Điều kiện kích hoạt (Trigger)
Gara nhập mặt hàng mới vào danh mục kinh doanh.
Tiền điều kiện (Preconditions)
Người dùng có quyền quản lý kho hàng.
Hậu điều kiện Thành công
Phụ tùng được lưu vào bảng parts với tồn kho ban đầu = 0, sẵn sàng cho nhập kho.
Hậu điều kiện Thất bại
Báo lỗi nếu trùng mã part_code hoặc giá bán âm.
Quy tắc nghiệp vụ liên quan
BR14 (Tồn thực & Tồn giữ), BR18 (Snapshot giá), BR24 (Bảo toàn danh mục).
Bảng CSDL tác động
parts, suppliers, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Quản lý kho
Mở danh mục Phụ tùng, chọn 'Khai báo phụ tùng'.
Quản lý kho
Nhập: mã SKU (part_code), tên, mã OEM, thương hiệu, nhà sản xuất, đơn vị tính (cái/bộ/lít...), NCC ưu tiên, mã vị trí kệ kho (location_code), giá vốn chuẩn, giá bán niêm yết, ngưỡng cảnh báo tồn (min_stock_quantity), thời hạn bảo hành.
Hệ thống
Kiểm tra tính duy nhất của part_code; kiểm tra ràng buộc purchase_price >= 0, sale_price >= 0.
Hệ thống
Khởi tạo tồn kho stock_quantity = 0, reserved_quantity = 0; lưu vào bảng parts.
Hệ thống
Hiển thị phụ tùng mới trong danh mục kho.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Cập nhật giá bán mới
Cập nhật trường sale_price trong parts. Giá này áp dụng cho các báo giá mới, không làm thay đổi giá trên các báo giá và hóa đơn cũ (BR18).
[Heading2] UC22: Nhập kho mua hàng hoặc đầu kỳ
Hình 6.22: Sơ đồ Use Case Diagram — UC22 (Nhập kho mua hàng hoặc đầu kỳ)
Mã & Tên Use Case
UC22 — Nhập kho mua hàng hoặc đầu kỳ
Tác nhân chính (Primary)
Quản lý kho (WAREHOUSE_STAFF)
Tác nhân phụ (Secondary)
Quản lý / ADMIN duyệt chứng từ
Mục tiêu (Goal)
Ghi nhận nhập kho linh kiện từ nhà cung cấp kèm hóa đơn đầu vào, hoặc nhập số dư đầu kỳ; cập nhật tồn và giá vốn.
Điều kiện kích hoạt (Trigger)
Xe chở linh kiện từ nhà cung cấp về tới xưởng gara.
Tiền điều kiện (Preconditions)
Nhà cung cấp và danh mục phụ tùng đã được khai báo.
Hậu điều kiện Thành công
Chứng từ inventory_documents chuyển POSTED; tạo inventory_transactions; tăng parts.stock_quantity; ghi nhận công nợ NCC.
Hậu điều kiện Thất bại
Báo lỗi nếu số lượng <= 0, trùng số hóa đơn NCC hoặc post chứng từ 2 lần.
Quy tắc nghiệp vụ liên quan
BR14 (Tăng tồn), BR15 (Ghi sổ 1 lần duy nhất), BR20 (Nợ NCC chỉ sinh từ IN mua có supplier).
Bảng CSDL tác động
inventory_documents, inventory_document_items, inventory_transactions, parts, suppliers
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Quản lý kho
Chọn 'Lập phiếu nhập kho' (document_type = 'IN').
Quản lý kho
Chọn nhà cung cấp, nhập số hóa đơn NCC (supplier_invoice_number), ngày hóa đơn, hạn thanh toán.
Quản lý kho
Thêm các dòng phụ tùng: chọn SKU, nhập số lượng thực nhận, đơn giá mua (unit_cost).
Hệ thống
Tính subtotal, thuế, tổng tiền chứng từ nhập; lưu ở trạng thái DRAFT.
Quản lý kho
Kiểm đếm xong và nhấn 'Ghi sổ nhập kho' (POST).
Hệ thống
Bắt đầu transaction CSDL: Khóa các dòng parts liên quan theo thứ tự id.
Hệ thống
Cập nhật parts.stock_quantity += quantity; cập nhật giá vốn bình quân gia quyền purchase_price.
Hệ thống
Tạo các bản ghi biến động trong inventory_transactions (transaction_type = 'IN').
Hệ thống
Chuyển chứng từ sang POSTED, ghi posted_by, posted_at.
Hệ thống
Ghi nhận công nợ phải trả NCC tương ứng với total_amount của chứng từ.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Nhập số dư đầu kỳ
Chọn is_opening_balance = true, supplier_id = NULL. Hệ thống tăng tồn kho nhưng không tạo khoản công nợ phải trả NCC (BR20).
[Heading2] UC23: Giữ phụ tùng sau khi khách duyệt
Hình 6.23: Sơ đồ Use Case Diagram — UC23 (Giữ phụ tùng sau khi khách duyệt)
Mã & Tên Use Case
UC23 — Giữ phụ tùng sau khi khách duyệt
Tác nhân chính (Primary)
Hệ thống tự động kích hoạt
Tác nhân phụ (Secondary)
Lễ tân (khi duyệt báo giá), Quản lý kho
Mục tiêu (Goal)
Tạm giữ lượng phụ tùng khả dụng trong kho ngay khi khách hàng phê duyệt báo giá để tránh bị xuất cho xe khác.
Điều kiện kích hoạt (Trigger)
Khách hàng duyệt dòng phụ tùng (estimate_items.approval_status = 'APPROVED').
Tiền điều kiện (Preconditions)
Dòng báo giá được duyệt; phụ tùng có trong kho.
Hậu điều kiện Thành công
estimate_items.reserved_quantity và parts.reserved_quantity tăng tương ứng; tồn khả dụng giảm an toàn.
Hậu điều kiện Thất bại
Thông báo thiếu hàng nếu tồn khả dụng không đủ đáp ứng.
Quy tắc nghiệp vụ liên quan
BR14 (Đồng bộ tuyệt đối giữa parts.reserved_quantity và tổng estimate_items.reserved_quantity).
Bảng CSDL tác động
parts, estimate_items, estimates, audit_events, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Hệ thống
Nhận sự kiện dòng báo giá phụ tùng được APPROVED.
Hệ thống
Khóa bản ghi phụ tùng trong bảng parts.
Hệ thống
Tính tồn khả dụng = stock_quantity - reserved_quantity.
Hệ thống
Nếu tồn khả dụng >= số lượng cần: tăng parts.reserved_quantity += quantity và estimate_items.reserved_quantity = quantity trong cùng giao dịch.
Hệ thống
Ghi log audit_events về việc giữ kho thành công.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
4a. Kho không đủ hàng khả dụng
Hệ thống giữ tối đa lượng đang có (nếu chính sách cho phép), ghi nhận số lượng thiếu và gửi thông báo cảnh báo 'Cần nhập bổ sung phụ tùng' cho Quản lý kho.
[Heading2] UC24: Xuất phụ tùng cho sửa chữa
Hình 6.24: Sơ đồ Use Case Diagram — UC24 (Xuất phụ tùng cho sửa chữa)
Mã & Tên Use Case
UC24 — Xuất phụ tùng cho sửa chữa
Tác nhân chính (Primary)
Quản lý kho (WAREHOUSE_STAFF)
Tác nhân phụ (Secondary)
Kỹ thuật viên (người nhận vật tư)
Mục tiêu (Goal)
Xuất kho vật tư giao cho thợ thi công theo đúng dòng báo giá đã duyệt; trừ tồn kho và trừ lượng giữ.
Điều kiện kích hoạt (Trigger)
Kỹ thuật viên mang phiếu yêu cầu vật tư đến quầy kho.
Tiền điều kiện (Preconditions)
Hạng mục phụ tùng đã được duyệt (APPROVED) và có phiếu sửa chữa hợp lệ.
Hậu điều kiện Thành công
Tạo chứng từ OUT đã POSTED; giảm stock_quantity và reserved_quantity; ghi nhận chi phí vào repair_order_parts.
Hậu điều kiện Thất bại
Từ chối xuất nếu vượt quá số lượng được duyệt hoặc tồn kho không đủ.
Quy tắc nghiệp vụ liên quan
BR13 (Chỉ xuất dòng APPROVED), BR14 (Trừ đồng bộ tồn và giữ), BR15 (Ghi sổ OUT 1 lần), BR18 (Snapshot giá vốn).
Bảng CSDL tác động
inventory_documents, inventory_document_items, inventory_transactions, parts, repair_order_parts, estimate_items
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Quản lý kho
Nhập mã phiếu sửa chữa, chọn các hạng mục phụ tùng cần xuất kho.
Hệ thống
Kiểm tra số lượng xuất <= số lượng khách duyệt trên estimate_items.
Quản lý kho
Xác nhận tạo chứng từ xuất kho (document_type = 'OUT').
Hệ thống
Khóa các bản ghi parts liên quan.
Hệ thống
Trừ tồn kho: stock_quantity -= qty; trừ lượng giữ: reserved_quantity -= qty.
Hệ thống
Trừ estimate_items.reserved_quantity -= qty tương ứng.
Hệ thống
Tạo bản ghi biến động trong inventory_transactions (transaction_type = 'OUT').
Hệ thống
Tạo hoặc cộng dồn vào bảng repair_order_parts (chốt số lượng ròng, đơn giá bán, đơn giá vốn lịch sử).
Hệ thống
Chuyển chứng từ sang POSTED và in phiếu xuất kho cho KTV ký nhận.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Xuất thành nhiều đợt
Quản lý kho có thể xuất trước một phần số lượng; phần còn lại tiếp tục được giữ trong kho cho đến đợt xuất tiếp theo.
[Heading2] UC25: Trả phụ tùng chưa sử dụng
Hình 6.25: Sơ đồ Use Case Diagram — UC25 (Trả phụ tùng chưa sử dụng)
Mã & Tên Use Case
UC25 — Trả phụ tùng chưa sử dụng
Tác nhân chính (Primary)
Quản lý kho (WAREHOUSE_STAFF)
Tác nhân phụ (Secondary)
Kỹ thuật viên (người trả lại)
Mục tiêu (Goal)
Nhập lại kho phụ tùng đã xuất nhưng không dùng hết hoặc khách đổi ý trước khi lập hóa đơn.
Điều kiện kích hoạt (Trigger)
Kỹ thuật viên mang phụ tùng còn nguyên vẹn trả lại kho.
Tiền điều kiện (Preconditions)
Đã có chứng từ xuất kho OUT trước đó; hóa đơn khách chưa phát hành.
Hậu điều kiện Thành công
Tạo chứng từ RETURN đã POSTED; tăng lại stock_quantity; giảm số lượng ròng trong repair_order_parts.
Hậu điều kiện Thất bại
Từ chối nếu số lượng trả vượt quá số lượng đã xuất hoặc hóa đơn đã chốt.
Quy tắc nghiệp vụ liên quan
BR14 (Tăng lại tồn), BR15 (Ghi sổ RETURN), BR18 (Lượng ròng trong repair_order_parts), BR24 (Giữ dòng số lượng 0).
Bảng CSDL tác động
inventory_documents, inventory_document_items, inventory_transactions, parts, repair_order_parts
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Quản lý kho
Mở chức năng 'Trả phụ tùng', chọn phiếu sửa chữa và dòng phụ tùng nguồn.
Hệ thống
Kiểm tra số lượng còn có thể trả = tổng đã xuất - tổng đã trả trước đó.
Quản lý kho
Nhập số lượng trả lại kho và lý do trả; nhấn 'Xác nhận trả kho'.
Hệ thống
Tạo chứng từ RETURN (source_document_id trỏ về chứng từ xuất cũ).
Hệ thống
Khóa bản ghi parts: tăng lại stock_quantity += return_qty.
Hệ thống
Tạo bản ghi biến động trong inventory_transactions (transaction_type = 'RETURN').
Hệ thống
Cập nhật giảm repair_order_parts.quantity -= return_qty (nếu về 0 thì giữ nguyên dòng với quantity = 0 để bảo toàn dấu vết lịch sử).
Hệ thống
Chuyển chứng từ RETURN sang POSTED.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Hóa đơn của phiếu đã phát hành
Hệ thống chặn chức năng trả kho trực tiếp; yêu cầu quy trình xử lý ngoại lệ theo chính sách tài chính.
[Heading2] UC26: Điều chỉnh tồn kho
Hình 6.26: Sơ đồ Use Case Diagram — UC26 (Điều chỉnh tồn kho)
Mã & Tên Use Case
UC26 — Điều chỉnh tồn kho
Tác nhân chính (Primary)
Quản lý kho (WAREHOUSE_STAFF) lập
Tác nhân phụ (Secondary)
Quản lý / ADMIN duyệt
Mục tiêu (Goal)
Điều chỉnh số lượng tồn kho thực tế sau khi kiểm kê phát hiện thừa/thiếu, hao hụt hoặc hư hỏng.
Điều kiện kích hoạt (Trigger)
Kết thúc đợt kiểm kê định kỳ hoặc kiểm tra đột xuất tại kho.
Tiền điều kiện (Preconditions)
Người dùng có quyền kho; có biên bản kiểm kê và lý do chênh lệch rõ ràng.
Hậu điều kiện Thành công
Tạo chứng từ ADJUSTMENT_IN hoặc ADJUSTMENT_OUT đã POSTED; tồn kho khớp số kiểm kê thực tế.
Hậu điều kiện Thất bại
Từ chối nếu số điều chỉnh làm tồn kho âm hoặc nhỏ hơn lượng đang giữ (reserved_quantity).
Quy tắc nghiệp vụ liên quan
BR14 (0 <= reserved <= stock), BR15 (Ghi sổ kiểm kê), BR24 (Bảo toàn lịch sử audit).
Bảng CSDL tác động
inventory_documents, inventory_document_items, inventory_transactions, parts, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Quản lý kho
Tạo phiếu điều chỉnh tồn kho, chọn phụ tùng cần điều chỉnh.
Quản lý kho
Nhập số lượng chênh lệch, chọn chiều biến động (ADJUSTMENT_IN nếu thừa, ADJUSTMENT_OUT nếu thiếu/hỏng), nhập đơn giá vốn và lý do chi tiết.
Hệ thống
Kiểm tra ràng buộc: nếu ADJUSTMENT_OUT thì stock_quantity mới không được < reserved_quantity (BR14).
ADMIN
Quản lý kiểm tra tính hợp lý của lý do và phê duyệt chứng từ.
Hệ thống
Khóa parts: cập nhật lại stock_quantity; tạo inventory_transactions tương ứng; chuyển chứng từ sang POSTED.
Hệ thống
Ghi nhận sự kiện kiểm toán nghiêm ngặt vào audit_events.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Tồn sau điều chỉnh nhỏ hơn lượng đang giữ
Hệ thống từ chối cho đến khi các phiếu đang giữ linh kiện được hủy hoặc điều chỉnh giảm lượng giữ tương ứng.
[Heading2] UC27: Kiểm tra chất lượng và yêu cầu làm lại
Hình 6.27: Sơ đồ Use Case Diagram — UC27 (Kiểm tra chất lượng và yêu cầu làm lại)
Mã & Tên Use Case
UC27 — Kiểm tra chất lượng và yêu cầu làm lại
Tác nhân chính (Primary)
Kỹ thuật viên có quyền QC (can_qc = true), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Kỹ thuật viên thi công (làm lại nếu FAIL)
Mục tiêu (Goal)
Nghiệm thu chất lượng xe sau khi hoàn tất các hạng mục sửa chữa; quyết định Đạt (PASS) để thanh toán hoặc Không đạt (FAIL) để làm lại.
Điều kiện kích hoạt (Trigger)
Toàn bộ công việc trong phiếu chuyển sang COMPLETED.
Tiền điều kiện (Preconditions)
Tất cả repair_tasks đã hoàn thành, không còn timer nào đang mở; KTV thực hiện có can_qc = true.
Hậu điều kiện Thành công
Tạo vehicle_checks loại QC; nếu PASS: cập nhật latest_qc_check_id và mở khóa lập hóa đơn; nếu FAIL: yêu cầu làm lại.
Hậu điều kiện Thất bại
Người kiểm tra không đủ thẩm quyền QC hoặc biểu mẫu checklist chưa đánh giá đủ các mục bắt buộc.
Quy tắc nghiệp vụ liên quan
BR09 (Thẩm quyền can_qc), BR16 (Checklist JSON), BR17 (QC PASSED mới nhất là điều kiện tiên quyết cho hóa đơn và bàn giao).
Bảng CSDL tác động
vehicle_checks, repair_orders, repair_tasks, employees, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Người QC
Mở phiếu sửa chữa đang chờ nghiệm thu chất lượng.
Người QC
Kiểm tra thực tế các hạng mục trên xe theo biểu mẫu checklist_json chuyên dụng cho QC.
Người QC
Đánh giá từng hạng mục và chọn kết luận chung: PASSED hoặc FAILED; nhập ghi chú chi tiết.
Hệ thống
Lưu vào bảng vehicle_checks (check_type = 'QC', attempt_no tự tăng cho lần kiểm tra này).
Hệ thống
Cập nhật repair_orders.latest_qc_check_id trỏ về bản ghi QC mới nhất này (BR17).
Hệ thống
Nếu PASSED: chuyển repair_orders.status = 'COMPLETED', cho phép Lễ tân lập hóa đơn.
Hệ thống
Nếu FAILED: chuyển repair_orders.status = 'QC_FAILED', gửi thông báo yêu cầu thợ chính kiểm tra và làm lại.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
7a. Thợ làm lại xong và yêu cầu kiểm tra lần 2
Người QC mở kiểm tra lại; hệ thống tạo attempt_no = 2 mới, không ghi đè lần kiểm tra thất bại trước đó.
[Heading2] UC28: Lập và phát hành hóa đơn khách
Hình 6.28: Sơ đồ Use Case Diagram — UC28 (Lập và phát hành hóa đơn khách)
Mã & Tên Use Case
UC28 — Lập và phát hành hóa đơn khách
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER)
Mục tiêu (Goal)
Tổng hợp toàn bộ công việc và phụ tùng thực dùng để phát hành hóa đơn tài chính snapshot bất biến.
Điều kiện kích hoạt (Trigger)
Phiếu sửa chữa đã qua kiểm định QC đạt tiêu chuẩn (PASSED).
Tiền điều kiện (Preconditions)
repair_orders.status = 'COMPLETED'; latest_qc_check_id là PASSED; chưa có hóa đơn trước đó.
Hậu điều kiện Thành công
Bản ghi invoices và các dòng invoice_items được tạo; chốt snapshot thông tin khách và xe; status = 'UNPAID'.
Hậu điều kiện Thất bại
Từ chối nếu QC chưa đạt, còn công việc đang mở hoặc phiếu đã phát hành hóa đơn rồi.
Quy tắc nghiệp vụ liên quan
BR17 (QC trước hóa đơn), BR18 (Snapshot bất biến dữ liệu hóa đơn), BR19 (Thu khách).
Bảng CSDL tác động
invoices, invoice_items, repair_orders, repair_tasks, repair_order_parts, customers, vehicles
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Mở phiếu sửa chữa đã hoàn thành, chọn 'Lập hóa đơn'.
Hệ thống
Kiểm tra điều kiện: latest_qc_check_id đạt PASSED, không còn timer mở, không có phụ tùng xuất dở dang (BR17).
Hệ thống
Tự động gom toàn bộ repair_tasks (loại SERVICE) và repair_order_parts có quantity > 0 (loại PART) của đúng phiếu đó.
Lễ tân
Kiểm tra bảng tính tiền, nhập mức giảm giá hoặc thuế suất nếu có.
Hệ thống
Khóa phiếu sửa chữa; chụp toàn bộ dữ liệu khách hàng (tên, MST, địa chỉ) vào customer_snapshot và xe vào vehicle_snapshot.
Hệ thống
Tạo bản ghi duy nhất trong invoices (invoice_code tự sinh, status = 'UNPAID').
Hệ thống
Tạo các dòng snapshot trong invoice_items (chốt tên mục, số lượng, đơn giá bán, đơn giá vốn lịch sử).
Hệ thống
Cập nhật repair_orders.status = 'INVOICED' và hiển thị bản in hóa đơn chuẩn.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
5a. Hóa đơn bảo hành miễn phí (Tổng tiền = 0)
Hệ thống tự động đánh dấu invoice.status = 'PAID' mà không tạo bản ghi thanh toán payments giá trị 0 (BR18).
[Heading2] UC29: Ghi nhận thanh toán khách
Hình 6.29: Sơ đồ Use Case Diagram — UC29 (Ghi nhận thanh toán khách)
Mã & Tên Use Case
UC29 — Ghi nhận thanh toán khách
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER)
Mục tiêu (Goal)
Ghi nhận số tiền thanh toán thực tế của khách hàng (thu 1 lần hoặc nhiều lần, nhiều phương thức).
Điều kiện kích hoạt (Trigger)
Khách hàng trả tiền mặt, chuyển khoản, quẹt thẻ hoặc quét VietQR.
Tiền điều kiện (Preconditions)
Hóa đơn đang ở trạng thái UNPAID hoặc PARTIALLY_PAID; tiền đã vào tài khoản/két gara.
Hậu điều kiện Thành công
Tạo bản ghi payments (direction = 'IN'); cập nhật trạng thái hóa đơn sang PARTIALLY_PAID hoặc PAID.
Hậu điều kiện Thất bại
Từ chối nếu số tiền <= 0, thu vượt quá số dư còn nợ hoặc sai mã hóa đơn.
Quy tắc nghiệp vụ liên quan
BR19 (Thu tiền dương, không thu vượt nợ, direction = IN), BR20 (Phân biệt rạch ròi thu khách và chi NCC).
Bảng CSDL tác động
payments, invoices, audit_events, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Tra cứu hóa đơn của khách hàng, xem số tiền còn phải thanh toán.
Khách hàng
Thực hiện thanh toán bằng tiền mặt, thẻ, chuyển khoản hoặc VietQR.
Lễ tân
Xác nhận tiền đã vào tài khoản gara, nhập số tiền thanh toán, chọn phương thức và nhập mã tham chiếu giao dịch.
Hệ thống
Khóa bản ghi hóa đơn (Pessimistic lock); kiểm tra: 0 < amount <= số tiền còn nợ (BR19).
Hệ thống
Tạo bản ghi trong payments (direction = 'IN', invoice_id = ID hóa đơn, transaction_code tự sinh duy nhất).
Hệ thống
Tính toán lại tổng đã thu; nếu đã thu đủ total_amount: cập nhật invoices.status = 'PAID'; nếu thu một phần: cập nhật 'PARTIALLY_PAID'.
Hệ thống
In phiếu thu cho khách hàng và gửi thông báo xác nhận thanh toán.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Hai lễ tân cùng thu tiền đồng thời trên một hóa đơn
Khóa hàng CSDL đảm bảo giao dịch thứ hai chỉ được thu tối đa số dư nợ còn lại sau giao dịch thứ nhất.
[Heading2] UC30: Thanh toán nhà cung cấp
Hình 6.30: Sơ đồ Use Case Diagram — UC30 (Thanh toán nhà cung cấp)
Mã & Tên Use Case
UC30 — Thanh toán nhà cung cấp
Tác nhân chính (Primary)
Quản lý và Quản trị viên (ADMIN)
Tác nhân phụ (Secondary)
Nhà cung cấp (SUPPLIER)
Mục tiêu (Goal)
Chi trả tiền mua phụ tùng cho nhà cung cấp dựa trên các chứng từ nhập kho mua hàng đã ghi sổ.
Điều kiện kích hoạt (Trigger)
Đến kỳ hạn thanh toán công nợ theo hợp đồng nhà cung cấp.
Tiền điều kiện (Preconditions)
Chứng từ nhập kho (document_type = 'IN') đã POSTED, có supplier_id, không phải tồn đầu kỳ, còn dư nợ.
Hậu điều kiện Thành công
Tạo bản ghi payments (direction = 'OUT'); giảm công nợ phải trả NCC.
Hậu điều kiện Thất bại
Từ chối nếu nhân viên Lễ tân thực hiện (sai quyền) hoặc chi vượt số tiền nợ trên chứng từ.
Quy tắc nghiệp vụ liên quan
BR01 (Chỉ ADMIN được chi tiền), BR19 (Khoản chi dương), BR20 (payments OUT gắn chứng từ mua IN đã POSTED).
Bảng CSDL tác động
payments, inventory_documents, suppliers, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
ADMIN
Mở sổ theo dõi công nợ nhà cung cấp, chọn chứng từ nhập kho cần thanh toán.
Hệ thống
Kiểm tra quyền ADMIN và tính toán số tiền còn nợ của chứng từ (total_amount - tổng payments OUT đã trả).
ADMIN
Nhập số tiền chi trả, chọn phương thức (chuyển khoản ngân hàng) và mã giao dịch ngân hàng.
Hệ thống
Khóa chứng từ nhập kho; kiểm tra 0 < amount <= số dư nợ còn lại.
Hệ thống
Tạo bản ghi trong payments (direction = 'OUT', inventory_document_id = ID chứng từ, invoice_id = NULL).
Hệ thống
Cập nhật sổ công nợ nhà cung cấp và ghi vết kiểm toán vào audit_events.
ADMIN
In phiếu chi lưu hồ sơ kế toán.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Thanh toán từng đợt
ADMIN nhập số tiền trả đợt 1; số nợ còn lại tiếp tục được theo dõi cho các đợt thanh toán tiếp theo.
[Heading2] UC31: Bàn giao xe và đóng phiếu
Hình 6.31: Sơ đồ Use Case Diagram — UC31 (Bàn giao xe và đóng phiếu)
Mã & Tên Use Case
UC31 — Bàn giao xe và đóng phiếu
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER)
Mục tiêu (Goal)
Nghiệm thu bàn giao xe cho khách hàng, giải phóng khoang, kích hoạt bảo hành và chính thức đóng phiếu sửa chữa.
Điều kiện kích hoạt (Trigger)
Khách hàng đến nhận xe sau khi sửa chữa và thanh toán xong.
Tiền điều kiện (Preconditions)
QC đạt PASSED; hóa đơn đã phát hành; khách hàng đã thanh toán đủ (hoặc được bảo lãnh nợ trong hạn mức).
Hậu điều kiện Thành công
repair_orders chuyển CLOSED; giải phóng khoang; kích hoạt chính sách bảo hành trong warranties.
Hậu điều kiện Thất bại
Từ chối nếu còn nợ vượt hạn mức tín dụng hoặc chưa có xác nhận bàn giao.
Quy tắc nghiệp vụ liên quan
BR07 (Giải phóng tài nguyên), BR17 (QC trước bàn giao), BR19 (Điều kiện tài chính), BR21 (Chụp chính sách bảo hành).
Bảng CSDL tác động
repair_orders, vehicle_checks, invoices, bay_reservations, warranties, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Lễ tân
Kiểm tra điều kiện bàn giao: QC mới nhất đạt PASSED, tình trạng thanh toán của hóa đơn.
Lễ tân
Cùng khách hàng kiểm tra lại xe và điền biên bản bàn giao (checklist_json loại 'HANDOVER').
Khách hàng
Ký nhận bàn giao xe và đồ đạc nguyên vẹn.
Lễ tân
Nhấn 'Xác nhận bàn giao & Đóng phiếu' trên hệ thống.
Hệ thống
Cập nhật repair_orders: handed_over_by, handed_over_at, closed_at, status = 'CLOSED'.
Hệ thống
Giải phóng tài nguyên khoang sửa chữa liên quan trong bay_reservations (chuyển RELEASED).
Hệ thống
Quét các dòng invoice_items có chính sách bảo hành; tự động tạo các bản ghi tương ứng trong bảng warranties (chụp thời hạn, mốc km, điều kiện).
Hệ thống
Gửi tin nhắn cảm ơn và sổ bảo hành điện tử cho khách hàng qua notifications.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Khách hàng doanh nghiệp được nợ theo hợp đồng
Hệ thống kiểm tra tổng nợ của khách hàng không vượt credit_limit và trong payment_term_days; nếu thỏa mãn thì cho phép bàn giao xe (BR19).
[Heading2] UC32: Tiếp nhận yêu cầu bảo hành
Hình 6.32: Sơ đồ Use Case Diagram — UC32 (Tiếp nhận yêu cầu bảo hành)
Mã & Tên Use Case
UC32 — Tiếp nhận yêu cầu bảo hành
Tác nhân chính (Primary)
Khách hàng (CUSTOMER)
Tác nhân phụ (Secondary)
Lễ tân (RECEPTIONIST) ghi nhận thay khi khách đến gara
Mục tiêu (Goal)
Ghi nhận yêu cầu khiếu nại bảo hành đối với các hạng mục phụ tùng hoặc dịch vụ đã sửa chữa trước đó.
Điều kiện kích hoạt (Trigger)
Xe gặp sự cố tái phát trong thời hạn bảo hành.
Tiền điều kiện (Preconditions)
Hạng mục sửa chữa có hợp đồng bảo hành còn hiệu lực trong bảng warranties.
Hậu điều kiện Thành công
Tạo bản ghi warranty_claims ở trạng thái OPEN, gắn đúng warranty_id và xe.
Hậu điều kiện Thất bại
Từ chối nếu khách hàng khiếu nại cho xe không thuộc quyền sở hữu của họ.
Quy tắc nghiệp vụ liên quan
BR02 (Kiểm soát quyền), BR21 (Điều kiện bảo hành kép Ngày & Km), BR22 (Quy trình claim).
Bảng CSDL tác động
warranties, warranty_claims, vehicles, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Khách hàng
Mở danh sách 'Bảo hành của tôi', chọn hạng mục phụ tùng/dịch vụ cần yêu cầu bảo hành.
Hệ thống
Hiển thị thông tin bảo hành gốc: ngày bắt đầu, ngày hết hạn, số km bảo hành, các điều khoản loại trừ.
Khách hàng
Nhập số ODO hiện tại của xe, mô tả chi tiết lỗi gặp phải và tải ảnh/video hiện trạng.
Hệ thống
Kiểm tra sơ bộ điều kiện còn hạn (thời gian hiện tại < expires_at và ODO hiện tại < end_odometer) (BR21).
Hệ thống
Tạo bản ghi trong warranty_claims (claim_code tự sinh, status = 'OPEN', requested_at = hiện tại).
Hệ thống
Gửi thông báo đến Quản lý xưởng để tiến hành thẩm định và xử lý.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Khách mang xe trực tiếp đến gara khiếu nại
Lễ tân tra cứu mã bảo hành theo biển số xe và nhập thông tin khiếu nại thay mặt khách hàng.
[Heading2] UC33: Quyết định và xử lý bảo hành
Hình 6.33: Sơ đồ Use Case Diagram — UC33 (Quyết định và xử lý bảo hành)
Mã & Tên Use Case
UC33 — Quyết định và xử lý bảo hành
Tác nhân chính (Primary)
Quản lý và Quản trị viên (ADMIN)
Tác nhân phụ (Secondary)
Kỹ thuật viên (thực hiện kiểm tra thực tế)
Mục tiêu (Goal)
Thẩm định yêu cầu bảo hành, ra quyết định chấp thuận hoặc từ chối, và khởi tạo phiếu sửa chữa bảo hành mới.
Điều kiện kích hoạt (Trigger)
Có yêu cầu bảo hành mới ở trạng thái OPEN cần phê duyệt.
Tiền điều kiện (Preconditions)
Yêu cầu bảo hành tồn tại; người duyệt có vai trò ADMIN.
Hậu điều kiện Thành công
Cập nhật warranty_claims (APPROVED/REJECTED); nếu APPROVED thì tạo repair_orders mới xử lý bảo hành.
Hậu điều kiện Thất bại
Từ chối nếu thiếu lý do thẩm định hoặc vi phạm điều khoản bảo hành.
Quy tắc nghiệp vụ liên quan
BR01 (ADMIN quyết định), BR21 (Điều kiện loại trừ), BR22 (Tạo lần xử lý mới, không sửa lần cũ).
Bảng CSDL tác động
warranty_claims, warranties, repair_orders, audit_events, notifications
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
ADMIN
Mở danh sách yêu cầu bảo hành, xem nội dung phản ánh và lịch sử sửa chữa gốc.
ADMIN
Xem kết quả kiểm tra thực tế từ Kỹ thuật viên (kiểm tra tem bảo hành, hao mòn tự nhiên, tai nạn...).
ADMIN
Ra quyết định: CHẤP THUẬN (APPROVED) hoặc TỪ CHỐI (REJECTED), bắt buộc nhập lý do chi tiết (decision_reason).
Hệ thống
Cập nhật warranty_claims: status, decided_by, decided_at, decision_reason.
Hệ thống
Nếu APPROVED: tự động tạo một phiếu sửa chữa mới (handling_order_id), liên kết với xe đó để thực hiện xử lý (BR22).
Hệ thống
Gửi thông báo kết quả thẩm định kèm hướng dẫn đến khách hàng qua notifications.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
3a. Từ chối bảo hành
Hệ thống ghi nhận lý do từ chối (ví dụ: hỏng do va đập ngoại lực, quá số km quy định) và thông báo cho khách hàng; claim chuyển sang REJECTED.
[Heading2] UC34: Chăm sóc khách và theo dõi thông báo
Hình 6.34: Sơ đồ Use Case Diagram — UC34 (Chăm sóc khách và theo dõi thông báo)
Mã & Tên Use Case
UC34 — Chăm sóc khách và theo dõi thông báo
Tác nhân chính (Primary)
Lễ tân (RECEPTIONIST), Quản lý (ADMIN)
Tác nhân phụ (Secondary)
Khách hàng (CUSTOMER) nhận thông báo
Mục tiêu (Goal)
Quản lý việc gửi thông báo trạng thái, theo dõi tỷ lệ gửi thành công và chăm sóc khách hàng.
Điều kiện kích hoạt (Trigger)
Có sự kiện nghiệp vụ phát sinh hoặc Lễ tân liên hệ chăm sóc khách.
Tiền điều kiện (Preconditions)
Hệ thống có các bản ghi thông báo trong bảng notifications.
Hậu điều kiện Thành công
Thông báo được gửi tới đúng người qua đúng kênh (WEB/EMAIL/SMS); cập nhật trạng thái SENT hoặc đọc (read_at).
Hậu điều kiện Thất bại
Ghi nhận lỗi FAILED và tự động xếp lịch thử lại (retry) nếu lỗi mạng bên thứ ba.
Quy tắc nghiệp vụ liên quan
BR02 (Không xem trộm thông báo), BR23 (Queue PENDING cùng commit, worker gửi sau commit có lease/retry).
Bảng CSDL tác động
notifications, users, customers, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Hệ thống
Nhận sự kiện từ nghiệp vụ (tiếp nhận xe, duyệt giá, hoàn thành sửa chữa, mời nhận xe).
Hệ thống
Tạo bản ghi trong notifications ở trạng thái PENDING cùng transaction với sự kiện gốc (BR23).
Worker
Tiến trình chạy nền quét các thông báo PENDING, khóa dòng bằng locked_until và gọi dịch vụ gửi (WEB/EMAIL/SMS).
Worker
Nếu thành công: cập nhật status = 'SENT', sent_at = thời điểm gửi, lưu provider_reference.
Khách hàng
Xem thông báo trên màn hình chuông thông báo; hệ thống cập nhật read_at.
Lễ tân
Xem nhật ký thông báo để hỗ trợ gọi điện trực tiếp cho các khách hàng chưa xem thông báo.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
4a. Nhà cung cấp Email/SMS gặp sự cố mạng
Worker tăng attempt_count, lưu last_error, đặt next_attempt_at để thử lại sau; giao dịch nghiệp vụ chính trước đó không bị ảnh hưởng (BR23).
[Heading2] UC35: Nhắc bảo dưỡng và hạn bảo hành
Hình 6.35: Sơ đồ Use Case Diagram — UC35 (Nhắc bảo dưỡng và hạn bảo hành)
Mã & Tên Use Case
UC35 — Nhắc bảo dưỡng và hạn bảo hành
Tác nhân chính (Primary)
Tác vụ hệ thống tự động (SYSTEM WORKER)
Tác nhân phụ (Secondary)
Lễ tân (theo dõi và gọi chăm sóc)
Mục tiêu (Goal)
Tự động phân tích lịch sử sửa chữa để gửi tin nhắn nhắc nhở khách hàng khi sắp đến kỳ bảo dưỡng hoặc sắp hết hạn bảo hành.
Điều kiện kích hoạt (Trigger)
Đến lịch quét định kỳ hàng ngày (Cron job lúc 08:00 sáng).
Tiền điều kiện (Preconditions)
Có dữ liệu dịch vụ đã thực hiện (có interval_km / interval_months) hoặc bảo hành trong warranties.
Hậu điều kiện Thành công
Tạo các thông báo nhắc nhở PENDING không trùng lặp, gửi đến khách hàng.
Hậu điều kiện Thất bại
Bỏ qua các xe chưa đủ mốc hoặc đã được gửi nhắc nhở trong cùng chu kỳ.
Quy tắc nghiệp vụ liên quan
BR05 (Chu kỳ dịch vụ), BR21 (Thời hạn bảo hành), BR23 (Deduplication event_key).
Bảng CSDL tác động
services, repair_tasks, warranties, notifications, vehicles, customers
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Worker
Quét danh sách các xe đã từng làm dịch vụ có mốc chu kỳ (services.interval_km / interval_months) và các hợp đồng warranties sắp hết hạn trong 15 ngày.
Worker
Tính toán mốc thời gian và mốc km dự kiến dựa trên thói quen di chuyển trung bình của xe.
Worker
Tạo mã sự kiện event_key duy nhất (ví dụ: 'REMIND_MAINT_V123_202610').
Worker
Kiểm tra chống trùng theo uq_notice_event_channel; tạo bản ghi notifications mới (channel = 'WEB', 'EMAIL', 'SMS').
Lễ tân
Xem danh sách gợi ý chăm sóc khách hàng trong ngày để chủ động gọi điện tư vấn và hỗ trợ đặt lịch hẹn (UC07).
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
4a. Đã gửi nhắc trong tháng này
Ràng buộc duy nhất chặn tạo bản ghi đúp; tiến trình bỏ qua xe đó an toàn.
[Heading2] UC36: Xem dashboard và báo cáo
Hình 6.36: Sơ đồ Use Case Diagram — UC36 (Xem dashboard và báo cáo)
Mã & Tên Use Case
UC36 — Xem dashboard và báo cáo
Tác nhân chính (Primary)
Quản lý và Quản trị viên (ADMIN)
Tác nhân phụ (Secondary)
Lễ tân (xem doanh thu ngày), Quản lý kho (xem xuất nhập tồn)
Mục tiêu (Goal)
Cung cấp số liệu tổng quan trực quan: doanh thu, chi phí, công nợ, hiệu suất khoang, năng suất thợ và cảnh báo tồn kho.
Điều kiện kích hoạt (Trigger)
Ban quản lý mở bảng điều khiển (Dashboard) hoặc xuất báo cáo định kỳ.
Tiền điều kiện (Preconditions)
Người dùng đăng nhập với quyền hạn phù hợp phạm vi báo cáo.
Hậu điều kiện Thành công
Hiển thị các chỉ số tài chính và vận hành chính xác từ dữ liệu nguồn; hỗ trợ xuất Excel/PDF.
Hậu điều kiện Thất bại
Từ chối nếu tài khoản không đủ quyền xem số liệu tài chính.
Quy tắc nghiệp vụ liên quan
BR02 (Phân quyền dữ liệu), BR11 (Giờ công thực tế), BR18 (Doanh số hóa đơn), BR19/BR20 (Tách bạch thu/chi).
Bảng CSDL tác động
invoices, payments, inventory_transactions, labor_sessions, parts, v_invoice_balances, v_supplier_balances
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
ADMIN
Mở màn hình Báo cáo & Thống kê, chọn loại báo cáo: Doanh thu theo thời gian, Công nợ khách hàng, Báo cáo tồn kho, Năng suất kỹ thuật viên.
ADMIN
Chọn khoảng thời gian (từ ngày, đến ngày) và các tiêu chí lọc.
Hệ thống
Kiểm tra quyền truy cập; thực hiện các truy vấn tổng hợp chỉ đọc (Read-only queries).
Hệ thống
Tổng hợp payments IN (tiền thu thực tế), payments OUT (tiền chi NCC), invoices (doanh thu ghi nhận), loại bỏ các hóa đơn VOID.
Hệ thống
Tính toán giờ công KTV từ tổng các labor_sessions thực tế (không tính thời gian tạm dừng) (BR11).
Hệ thống
Hiển thị biểu đồ trực quan kèm bảng số liệu chi tiết và thời điểm cập nhật mới nhất.
ADMIN
Chọn 'Xuất báo cáo' (Excel / PDF) để phục vụ họp ban quản trị.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
1a. Quản lý kho xem báo cáo
Chỉ hiển thị số liệu xuất/nhập/tồn và giá trị tồn kho; ẩn số liệu doanh thu dịch vụ tổng gara.
[Heading2] UC37: Quản lý ca làm và tài nguyên khoang
Hình 6.37: Sơ đồ Use Case Diagram — UC37 (Quản lý ca làm và tài nguyên khoang)
Mã & Tên Use Case
UC37 — Quản lý ca làm và tài nguyên khoang
Tác nhân chính (Primary)
Quản lý và Quản trị viên (ADMIN)
Tác nhân phụ (Secondary)
Lễ tân (xem lịch để điều phối xe)
Mục tiêu (Goal)
Lập lịch ca làm việc cho nhân viên, quản lý danh sách khoang sửa chữa và thiết lập lịch bảo trì định kỳ cho khoang.
Điều kiện kích hoạt (Trigger)
Lên lịch làm việc đầu tuần/tháng hoặc khoang cần bảo dưỡng thiết bị.
Tiền điều kiện (Preconditions)
Nhân viên và khoang đã có trong hệ thống; người dùng có quyền ADMIN.
Hậu điều kiện Thành công
Ca làm việc (employee_shifts) và lịch bảo trì (bay_reservations) được lưu trữ hợp lệ.
Hậu điều kiện Thất bại
Báo lỗi nếu giờ kết thúc <= giờ bắt đầu hoặc trùng lịch phân ca.
Quy tắc nghiệp vụ liên quan
BR06 (Không trùng khoang), BR07 (Giải phóng tài nguyên), BR09 (Phân công đúng ca làm).
Bảng CSDL tác động
employee_shifts, service_bays, bay_reservations, employees, audit_events
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
ADMIN
Mở màn hình Quản lý ca làm và khoang sửa chữa.
ADMIN
Tạo ca làm việc cho KTV: chọn nhân viên, nhập giờ bắt đầu (starts_at), giờ kết thúc (ends_at), trạng thái ACTIVE.
ADMIN
Kiểm tra khoang bảo dưỡng: chọn khoang, tạo lịch bảo trì (reservation_type = 'MAINTENANCE', chọn khoảng thời gian).
Hệ thống
Kiểm tra tính hợp lệ: ends_at > starts_at, không được xếp bảo trì nếu khoang đang có xe sửa chữa (BR06).
Hệ thống
Lưu thông tin vào employee_shifts và bay_reservations.
Hệ thống
Cập nhật dữ liệu để Lễ tân có căn cứ xếp lịch hẹn (UC07) và phân công thợ (UC11).
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Hủy ca làm việc đột xuất
ADMIN chọn CANCELLED ca làm; hệ thống kiểm tra và cảnh báo nếu nhân viên đang có task phân công chưa hoàn thành.
[Heading2] UC38: Tra cứu lịch sử xe và hồ sơ hậu mãi
Hình 6.38: Sơ đồ Use Case Diagram — UC38 (Tra cứu lịch sử xe và hồ sơ hậu mãi)
Mã & Tên Use Case
UC38 — Tra cứu lịch sử xe và hồ sơ hậu mãi
Tác nhân chính (Primary)
Khách hàng (CUSTOMER) xem xe của mình
Tác nhân phụ (Secondary)
Lễ tân, Quản lý (xem toàn bộ xe để tư vấn)
Mục tiêu (Goal)
Tra cứu toàn bộ hồ sơ quá trình sửa chữa, các phụ tùng đã thay thế, hóa đơn và tình trạng bảo hành của xe qua các năm.
Điều kiện kích hoạt (Trigger)
Khách hàng muốn xem lại lịch sử bảo dưỡng hoặc Lễ tân chuẩn bị tiếp nhận xe cũ.
Tiền điều kiện (Preconditions)
Xe đã từng thực hiện ít nhất một phiếu sửa chữa trong hệ thống.
Hậu điều kiện Thành công
Hiển thị đầy đủ cây lịch sử sửa chữa chính xác từ dữ liệu nguồn; không tạo bảng dư thừa.
Hậu điều kiện Thất bại
Từ chối truy cập nếu khách hàng cố tình tra cứu biển số xe của người khác.
Quy tắc nghiệp vụ liên quan
BR02 (Bảo mật quyền xem xe), BR04 (Lịch sử xe độc lập với đổi chủ), BR21 (Tra cứu bảo hành).
Bảng CSDL tác động
vehicles, repair_orders, repair_tasks, repair_order_parts, invoices, warranties, customers
Luồng sự kiện chính (Main Success Scenario):
Bên thực hiện
Hành động xử lý
Người dùng
Nhập biển số xe cần tra cứu lịch sử.
Hệ thống
Kiểm tra quyền sở hữu đối tượng: nếu là CUSTOMER thì xe phải thuộc customer_id của họ (BR02).
Hệ thống
Truy vấn danh sách các repair_orders đã đóng (CLOSED) của xe, sắp xếp theo thời gian giảm dần.
Hệ thống
Với mỗi lần sửa: hiển thị ngày vào, số ODO, hiện trạng, chẩn đoán, các công việc đã làm (repair_tasks), phụ tùng đã thay (repair_order_parts) và hóa đơn (invoices).
Hệ thống
Hiển thị danh sách các chính sách bảo hành còn hiệu lực trong bảng warranties.
Người dùng
Xem chi tiết biên bản nghiệm thu hoặc tải hóa đơn điện tử cũ dạng PDF.
Các luồng rẽ nhánh & Ngoại lệ (Alternative & Exception Flows):
Mã nhánh
Mô tả xử lý
2a. Xe đã đổi chủ sở hữu
Chủ mới chỉ được xem thông số kỹ thuật xe và lịch sử bảo dưỡng kỹ thuật; hệ thống ẩn thông tin cá nhân và hóa đơn thanh toán của chủ cũ (BR04).
[Heading1] 6 Kiến trúc triển khai và quản lý cơ sở dữ liệu
Hệ thống được xây dựng trên mô hình phân lớp rõ ràng giữa React/TypeScript ở Frontend và Spring Boot 3.5 ở Backend. Cơ sở dữ liệu MySQL 8.4 được quản lý migration tiến hóa thông qua Flyway (V1 đến V4). Cấu trúc dự án tổ chức theo module tách biệt, giao tiếp liên module thông qua các Service Contract (Hợp đồng I01–I08).
[Heading1] 7 Phân công công việc nhóm 4 thành viên
Thành viên
Phân hệ chủ trì
Module
Số bảng
Khối lượng việc
TV1
Nền tảng, Phân quyền, Thông báo & Báo cáo
M01, M13, M14
3 bảng
10 đầu việc
TV2
Khách hàng, Phương tiện, Dịch vụ & Tiếp nhận
M02, M03, M04, M05
6 bảng
10 đầu việc
TV3
Điều phối, Báo giá, Sửa chữa, QC & Bảo hành
M06, M07, M08, M10, M12
12 bảng
11 đầu việc
TV4
Phụ tùng, Chứng từ kho, Hóa đơn & Thu chi
M09, M11
9 bảng
11 đầu việc
[Heading1] 8 Kiểm thử và tiêu chí nghiệm thu
Kế hoạch kiểm thử bao gồm 6 mốc nghiệm thu (G0 đến G5) và 38 kịch bản kiểm thử tích hợp (T01 đến T38). Các bài kiểm thử bao phủ toàn diện: kiểm soát quyền truy cập trên từng bản ghi, ngăn chặn tranh chấp tài nguyên khoang/thợ, bảo toàn số lượng tồn kho và lượng giữ dưới khóa CSDL, kiểm soát QC PASSED trước khi phát hành hóa đơn và bàn giao xe.
