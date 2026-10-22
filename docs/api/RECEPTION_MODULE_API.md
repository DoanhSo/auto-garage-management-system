# Tài liệu API Phân hệ Khách hàng, Phương tiện & Tiếp nhận xe (TV2)

Tài liệu bàn giao API cho các phân hệ do **TV2** chủ trì theo mốc **G1 – G5** (M02 Customer, M03 Vehicle, M04 Catalog, M05 Reception).

---

## 1. Phân hệ Khách hàng (M02 Customer - UC04)

### `POST /api/v1/customers`
* **Mô tả:** Tạo hồ sơ khách hàng mới (cá nhân hoặc doanh nghiệp). Khách trực tiếp (walk-in) không bắt buộc tài khoản user.
* **Payload:**
```json
{
  "userId": null,
  "customerCode": "CUS-20261016-1001",
  "fullName": "Nguyễn Văn A",
  "customerType": "INDIVIDUAL",
  "phone": "0901234567",
  "email": "nguyenvana@gmail.com",
  "address": "123 Đường Nguyễn Huệ, Q1, TP.HCM",
  "companyName": null,
  "taxCode": null,
  "creditLimit": 10000000.00,
  "paymentTermDays": 30
}
```
* **Mã trả về:** `201 Created` / `400 Bad Request` / `409 Conflict` (trùng số điện thoại hoặc mã).

### `GET /api/v1/customers`
* **Mô tả:** Tìm kiếm danh sách khách hàng theo tên, số điện thoại hoặc mã.
* **Query Parameters:** `search`, `isActive`.

### `GET /api/v1/customers/{id}` & `GET /api/v1/customers/code/{code}`
* **Mô tả:** Lấy chi tiết thông tin khách hàng.

### `PUT /api/v1/customers/{id}`
* **Mô tả:** Cập nhật thông tin khách hàng.

### `DELETE /api/v1/customers/{id}`
* **Mô tả:** Soft delete khách hàng (`isActive = false`).

---

## 2. Phân hệ Danh mục Dịch vụ & Gói (M04 Catalog - UC06)

### `POST /api/v1/services`
* **Mô tả:** Thêm dịch vụ đơn lẻ (`SINGLE`) hoặc gói bảo dưỡng (`PACKAGE`).
* **Payload:**
```json
{
  "serviceCode": "SRV-BD-10K",
  "serviceName": "Bảo dưỡng định kỳ 10.000 km",
  "serviceType": "PACKAGE",
  "description": "Thay nhớt máy, lọc nhớt, vệ sinh phanh, kiểm tra gầm",
  "unit": "gói",
  "unitPrice": 850000.00,
  "standardMinutes": 90,
  "requiredBayType": "STANDARD",
  "intervalKm": 10000,
  "intervalMonths": 6,
  "warrantyDays": 30,
  "warrantyKm": 1000
}
```
* **Mã trả về:** `201 Created` / `400 Bad Request` / `409 Conflict`.

### `GET /api/v1/services`
* **Mô tả:** Tìm kiếm danh mục dịch vụ.
* **Query Parameters:** `search`, `type`, `isActive`.

---

## 3. Phân hệ Hồ sơ Phương tiện & Chuyển nhượng (M03 Vehicle - UC05, UC38)

### `POST /api/v1/vehicles`
* **Mô tả:** Đăng ký phương tiện mới gắn với khách hàng sở hữu.
* **Payload:**
```json
{
  "customerId": 1,
  "licensePlate": "51H-999.99",
  "vin": "1HGCR2F83HA123456",
  "brand": "Honda",
  "model": "CR-V 1.5 Turbo",
  "manufactureYear": 2022,
  "color": "Trắng",
  "fuelType": "Xăng",
  "odometer": 15000
}
```
* **Mã trả về:** `201 Created` / `400 Bad Request` / `409 Conflict`.

### `POST /api/v1/vehicles/{id}/transfer` (TV2-03)
* **Mô tả:** Chuyển nhượng phương tiện sang chủ sở hữu mới.
* **Payload:**
```json
{
  "newCustomerId": 2,
  "transferNote": "Mua lại xe theo hợp đồng công chứng số 123"
}
```
* **Quy tắc:** Bảo toàn nguyên vẹn khách hàng lịch sử trong các phiếu sửa chữa và hóa đơn cũ.

### `GET /api/v1/vehicles/{id}/timeline` (TV2-09 UC38)
* **Mô tả:** Tra cứu cây lịch sử sửa chữa/kỹ thuật của phương tiện.
* **Query Parameters:** `requestingCustomerId`, `role`.
* **Quy tắc BR04:** Chủ xe mới chỉ xem được thông số kỹ thuật và nhật ký bảo dưỡng; thông tin tài chính/hóa đơn của chủ cũ được ẩn tự động.

---

## 4. Phân hệ Đặt lịch hẹn (M05 Reception - UC07, UC08)

### `POST /api/v1/bookings` (TV2-05)
* **Mô tả:** Đặt lịch hẹn mới. Tính toán `expectedEndAt` tự động và gọi `BayScheduling` (I02) để giữ khoang.
* **Payload:**
```json
{
  "customerId": 1,
  "vehicleId": 1,
  "serviceId": 1,
  "bayId": 2,
  "appointmentAt": "2026-10-20T09:00:00Z",
  "requestDescription": "Thay dầu và kiểm tra tiếng kêu gầm",
  "createdBy": 1
}
```

### `POST /api/v1/bookings/{id}/confirm` (TV2-06)
* **Mô tả:** Chuyển trạng thái lịch từ `PENDING` sang `CONFIRMED`.

### `POST /api/v1/bookings/{id}/reschedule` (TV2-06)
* **Mô tả:** Dời lịch hẹn sang khung giờ mới, giải phóng khoang cũ và đặt giữ khoang mới.
* **Payload:**
```json
{
  "newAppointmentAt": "2026-10-21T10:00:00Z",
  "newBayId": 3,
  "actorUserId": 1
}
```

### `POST /api/v1/bookings/{id}/cancel` (TV2-06)
* **Mô tả:** Hủy lịch hẹn, nhập lý do và giải phóng khoang.
* **Payload:**
```json
{
  "cancellationReason": "Khách hàng bận công tác đột xuất",
  "actorUserId": 1
}
```

### `POST /api/v1/bookings/{id}/no-show` (TV2-06)
* **Mô tả:** Đánh dấu khách vắng mặt và giải phóng tài nguyên khoang.

---

## 5. Tiếp nhận xe vào xưởng & Checklist, Ảnh chụp (M05 Reception - UC09, BR16)

### `POST /api/v1/reception/check-in` (TV2-07)
* **Mô tả:** Tiếp nhận xe vào gara (hỗ trợ cả xe có hẹn trước và xe vãng lai), cập nhật ODO xe, cập nhật booking sang `RECEIVED`, gọi `RepairWorkflow.receive()` (I03) mở lệnh sửa chữa `NEW`.
* **Payload:**
```json
{
  "bookingId": 1,
  "vehicleId": 1,
  "odometerIn": 15200,
  "fuelLevelPercent": 75.0,
  "customerComplaint": "Gầm xe có tiếng lọc cọc khi qua gờ giảm tốc",
  "actorUserId": 1
}
```

### `POST /api/v1/vehicle-checks` (TV2-08)
* **Mô tả:** Ghi nhận biên bản kiểm tra hiện trạng xe ban đầu (`CHECK_IN`), kiểm định chất lượng (`QC`) hoặc bàn giao (`HANDOVER`).
* **Payload:**
```json
{
  "repairOrderId": 1,
  "checkType": "CHECK_IN",
  "checklistJson": "{\"exterior\":{\"scratches\":[\"rear_bumper\"]},\"interior\":{\"warning_lights\":[]}}",
  "odometer": 15200,
  "notes": "Trầy xước nhẹ cản sau",
  "checkedBy": 1
}
```

### `POST /api/v1/attachments/upload` (TV2-08)
* **Mô tả:** Tải lên tệp ảnh/video bằng chứng (Form-data: `file`, `category`, `repairOrderId`, `vehicleCheckId`).
* **Tiêu chuẩn an toàn BR16:**
  * Tính mã băm `sha256` của tệp để chống sửa đổi.
  * Sinh `storageKey` ngẫu nhiên an toàn, tuyệt đối không dùng tên file tùy ý của người dùng để tránh lỗ hổng Directory Traversal.
  * Kiểm duyệt MIME Type: Chỉ chấp nhận `image/jpeg`, `image/png`, `image/webp`, `image/heic`, `application/pdf`. Chặn file thực thi nguy hiểm.
