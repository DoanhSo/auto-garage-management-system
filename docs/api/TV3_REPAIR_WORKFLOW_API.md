# Tài liệu API & Hợp đồng Dịch vụ Phân hệ Điều phối, Sửa chữa & Bảo hành (TV3)

Tài liệu bàn giao API và hợp đồng liên module cho toàn bộ 11 gói công việc do **TV3** chủ trì theo các mốc **G1 – G5** (từ TV3-01 đến TV3-11):
- **M06 Scheduling:** Quản lý nhân viên, ca làm, khoang sửa chữa và hợp đồng I02 `BayScheduling`.
- **M08 Repair:** Vòng đời phiếu sửa chữa, task, phân công KTV, timer làm việc, bàn giao đóng phiếu và hợp đồng I03 `RepairWorkflow`.
- **M07 Estimate:** Lập báo giá (INITIAL/SUPPLEMENTAL), snapshot phiên bản, duyệt từng dòng và hợp đồng I04 `EstimateApproval`.
- **M10 Quality:** Kiểm tra chất lượng xe (QC), tăng số lần thử (attempt_no), kích hoạt làm lại (REWORK).
- **M12 Warranty:** Chính sách bảo hành từ hóa đơn, tiếp nhận yêu cầu bảo hành, quản trị viên duyệt và hợp đồng I07 `WarrantyOperations`.

---

## 1. Phân hệ Nhân viên & Khoang Sửa chữa (M06 Scheduling - TV3-01 & TV3-02)

### `POST /api/v1/employees`
* **Mô tả:** Tạo hồ sơ nhân viên kỹ thuật / cố vấn / lễ tân.
* **Quyền:** `ROLE_ADMIN`
* **Payload:**
```json
{
  "userId": 10,
  "employeeCode": "EMP-20261023-01",
  "fullName": "Trần Văn Kỹ Thuật",
  "phone": "0987654321",
  "skillLevel": "SENIOR",
  "hourlyCost": 150000.00,
  "canQc": true,
  "hireDate": "2026-01-15"
}
```

### `POST /api/v1/employee-shifts`
* **Mô tả:** Phân ca làm việc cho nhân viên (kiểm tra chống trùng ca, `endsAt > startsAt`).
* **Quyền:** `ROLE_ADMIN`

### `POST /api/v1/service-bays`
* **Mô tả:** Tạo khoang dịch vụ (STANDARD, LIFT, QUICK_SERVICE, DIAGNOSTIC, WASH).
* **Quyền:** `ROLE_ADMIN`

### `POST /api/v1/bay-reservations`
* **Mô tả:** Đặt giữ khoang (I02 BayScheduling: khóa khoang bi-directional, kiểm tra giao nhau `[starts_at, ends_at)`, tự động hủy khi quá hạn HELD).
* **Quyền:** `ROLE_ADMIN`, `ROLE_RECEPTIONIST`

---

## 2. Vòng đời Phiếu Sửa chữa & Công việc (M08 Repair - TV3-03)

### `POST /api/v1/repair-orders`
* **Mô tả:** Tiếp nhận tạo phiếu sửa chữa mới (NORMAL hoặc WARRANTY), tự động tạo task DIAGNOSIS nội bộ.
* **Quyền:** `ROLE_ADMIN`, `ROLE_RECEPTIONIST`
* **Payload:**
```json
{
  "bookingId": 100,
  "customerId": 1,
  "vehicleId": 2,
  "odometerIn": 25000,
  "customerComplaint": "Tiếng kêu lạ ở phanh trước khi đạp phanh",
  "fuelLevel": "HALF",
  "valuableItems": "Ví tiền, kính mát",
  "requestKey": "REQ-KEY-RECEIVE-001"
}
```

### `PATCH /api/v1/repair-orders/{id}/diagnosis`
* **Mô tả:** Cập nhật kết quả chẩn đoán ban đầu của KTV trước khi lập báo giá.
* **Quyền:** `ROLE_ADMIN`, `ROLE_TECHNICIAN`

### `PATCH /api/v1/repair-orders/{id}/status`
* **Mô tả:** Chuyển trạng thái phiếu sửa chữa theo state machine nghiêm ngặt (NEW -> DIAGNOSING -> ESTIMATING -> APPROVED -> IN_PROGRESS -> WAITING_QC -> COMPLETED -> CLOSED).

---

## 3. Báo giá & Duyệt Từng Dòng (M07 Estimate - TV3-04 & TV3-05)

### `POST /api/v1/estimates`
* **Mô tả:** Lập báo giá mới (`INITIAL` hoặc `SUPPLEMENTAL`), tự động tăng `version_no` dưới khóa pessimistic của phiếu, snapshot đơn giá và chính sách bảo hành.
* **Quyền:** `ROLE_ADMIN`, `ROLE_RECEPTIONIST`

### `POST /api/v1/estimates/{id}/decide`
* **Mô tả:** Khách hàng / cố vấn duyệt từng dòng báo giá (I04 `EstimateApproval`).
  * Dòng SERVICE `APPROVED` -> Tự động sinh `RepairTask` loại `REPAIR`.
  * Dòng PART `APPROVED` -> Gọi I05 `InventoryOperations.reserve` giữ vật tư kho.
  * Hỗ trợ idempotency replay (cùng request_key không sinh trùng lặp).
* **Quyền:** `ROLE_ADMIN`, `ROLE_RECEPTIONIST`
* **Payload:**
```json
{
  "requestKey": "REQ-DECIDE-001",
  "decisions": [
    {
      "estimateItemId": 101,
      "decision": "APPROVED",
      "note": "Khách đồng ý thay má phanh"
    },
    {
      "estimateItemId": 102,
      "decision": "REJECTED",
      "note": "Khách từ chối gói vệ sinh kim phun"
    }
  ]
}
```

---

## 4. Phân công KTV, Bấm giờ & Hoàn thành Task (M08 - TV3-06, TV3-07, TV3-08)

### `POST /api/v1/repair-tasks/{taskId}/assignments`
* **Mô tả:** Phân công kỹ thuật viên (tối đa 1 MAIN active trên mỗi task, kiểm tra quyền `can_qc` khi phân công QC, chống chồng ca công việc).
* **Quyền:** `ROLE_ADMIN`, `ROLE_RECEPTIONIST`

### `POST /api/v1/repair-tasks/assignments/{assignmentId}/start-timer`
* **Mô tả:** KTV bắt đầu bấm giờ làm việc (chỉ duy nhất 1 timer mở mỗi KTV, snapshot `hourly_cost`).
* **Quyền:** `ROLE_ADMIN`, `ROLE_TECHNICIAN`

### `POST /api/v1/repair-tasks/assignments/{assignmentId}/stop-timer`
* **Mô tả:** Dừng bấm giờ với lý do `PAUSE`, `COMPLETE`, `CANCEL` (PAUSE tính giờ thực tế, resume tạo session mới).
* **Quyền:** `ROLE_ADMIN`, `ROLE_TECHNICIAN`

### `POST /api/v1/repair-tasks/{taskId}/complete`
* **Mô tả:** Hoàn thành công việc: đóng mọi timer mở, giải phóng phân công, chuyển task sang `COMPLETED`, khi hết task tự động chuyển RepairOrder sang `WAITING_QC`.
* **Quyền:** `ROLE_ADMIN`, `ROLE_TECHNICIAN`

---

## 5. Kiểm tra Chất lượng (QC) & Yêu cầu Làm lại (M10 - TV3-09)

### `POST /api/v1/quality-checks`
* **Mô tả:** Thực hiện kiểm tra chất lượng (QC):
  * Kiểm tra KTV phải có quyền `can_qc`.
  * Tự động tăng `attempt_no` theo từng lần kiểm tra của phiếu.
  * `PASSED`: Chuyển phiếu sửa chữa sang `COMPLETED`.
  * `FAILED`: Chuyển phiếu sửa chữa sang `QC_FAILED`, đồng thời chuyển các task trong `reworkTaskIds` sang trạng thái `REWORK` để KTV làm lại.
* **Quyền:** `ROLE_ADMIN`, `ROLE_TECHNICIAN`
* **Payload:**
```json
{
  "repairOrderId": 800,
  "result": "PASSED",
  "checklistJson": "{\"phanh\": \"ĐẠT\", \"gầm\": \"ĐẠT\", \"đèn\": \"ĐẠT\"}",
  "odometer": 25050,
  "notes": "Kiểm tra xe hoàn tất, đạt tiêu chuẩn xuất xưởng"
}
```

---

## 6. Chính sách & Yêu cầu Bảo hành (M12 Warranty - TV3-10)

### `GET /api/v1/warranties/{id}` & `GET /api/v1/warranties/code/{code}`
* **Mô tả:** Tra cứu chi tiết chính sách bảo hành.
* **Quyền:** `ROLE_ADMIN`, `ROLE_RECEPTIONIST`, `ROLE_TECHNICIAN`, `ROLE_WAREHOUSE_STAFF`

### `POST /api/v1/warranties/claims`
* **Mô tả:** Mở yêu cầu bảo hành mới (I07 `WarrantyOperations.openClaim`):
  * Kiểm tra song song cả 2 chiều: số ngày và số km.
  * Mốc bằng hạn được tính là hết hạn.
  * Chính sách phải ở trạng thái `ACTIVE`.
* **Quyền:** `ROLE_ADMIN`, `ROLE_RECEPTIONIST`
* **Payload:**
```json
{
  "warrantyId": 100,
  "currentOdometer": 28000,
  "description": "Má phanh phát ra tiếng kêu bất thường sau 3.000 km",
  "requestKey": "REQ-KEY-CLAIM-001"
}
```

### `POST /api/v1/warranties/claims/{claimId}/decide`
* **Mô tả:** Phê duyệt hoặc từ chối claim (Chỉ Quản trị viên):
  * Khi APPROVED: Tự động gọi I03 `RepairWorkflow.createWarrantyOrder` sinh phiếu sửa chữa bảo hành mới (loại `WARRANTY`) và gắn `handling_order_id`.
  * Khi REJECTED: Chuyển trạng thái sang `REJECTED`, ghi nhận lý do và đóng claim.
* **Quyền:** `ROLE_ADMIN`
* **Payload:**
```json
{
  "approved": true,
  "reason": "Lỗi hao mòn bất thường do linh kiện xuất xưởng, duyệt thay thế theo bảo hành",
  "requestKey": "REQ-DECIDE-CLAIM-001"
}
```

---

## 7. Bàn giao Xe & Nghiệm thu Đóng phiếu (M08 - TV3-11)

### `POST /api/v1/repair-orders/{id}/handover`
* **Mô tả:** Bàn giao xe cho khách hàng và đóng phiếu sửa chữa (I03 `RepairWorkflow.handOver`):
  * Kiểm tra điều kiện tiên quyết: QC PASSED, tất cả task COMPLETED, không còn timer mở.
  * Kiểm tra hạn mức nợ và điều kiện thanh toán (gọi I06 `BillingOperations.requireHandoverAllowed`).
  * Kiểm tra Odometer bàn giao $\ge$ Odometer vào xưởng.
  * Cập nhật phiếu: `status = CLOSED`, `odometer_out`, `handed_over_by`, `handed_over_at`, `closed_at`.
  * Cập nhật Odometer của phương tiện trong CSDL.
  * Giải phóng khoang sửa chữa (I02 `BayScheduling.releaseForRepair`).
  * Giải phóng lượng giữ vật tư trong kho (I05 `InventoryOperations.releaseForOrder`).
  * Tự động tạo chính sách bảo hành cho các hạng mục đủ điều kiện từ hóa đơn (I07 `WarrantyOperations.createFromInvoice`).
  * Ghi biên bản kiểm tra bàn giao (`VehicleCheck` loại `HANDOVER`).
  * Ghi nhận audit và outbox idempotency qua I08 `NotificationAudit`.
* **Quyền:** `ROLE_ADMIN`, `ROLE_RECEPTIONIST`
* **Payload:**
```json
{
  "odometerOut": 25050,
  "requestKey": "REQ-HANDOVER-800"
}
```

---

## 8. Kết quả Kiểm thử Nghiệm thu

Toàn bộ **82 unit tests và integration tests** của phân hệ TV3 đã chạy thành công 100% với Maven wrapper:
1. `TV3WorkflowIntegrationTest`: Kiểm thử tích hợp toàn trình liên thông 11 bước từ Xếp khoang $\rightarrow$ Tiếp nhận $\rightarrow$ Lập báo giá $\rightarrow$ Duyệt từng dòng $\rightarrow$ Phân công $\rightarrow$ Timer $\rightarrow$ Hoàn thành task $\rightarrow$ QC $\rightarrow$ Bàn giao $\rightarrow$ Kích hoạt bảo hành $\rightarrow$ Duyệt claim bảo hành.
2. `WarrantyServiceTest`: Kiểm thử trọn vẹn chính sách bảo hành, hết hạn ngày/km, duyệt/từ chối claim, sinh phiếu bảo hành mới.
3. `QualityCheckServiceTest`: Kiểm thử QC PASSED, QC FAILED kích hoạt REWORK, chặn khi còn timer/task mở.
4. `LaborExecutionServiceTest`: Kiểm thử phân công, start/stop timer, hoàn thành task, tính tổng công và snapshot cost.
5. `RepairWorkflowServiceTest`: Kiểm thử vòng đời phiếu, chuyển trạng thái, bàn giao đóng phiếu, giải phóng kho/khoang.
6. `EstimateApprovalServiceTest` & `EstimateServiceTest`: Kiểm thử lập báo giá, version_no tăng dưới khóa phiếu, duyệt từng dòng và idempotency replay.
7. `BaySchedulingServiceTest` & `ServiceBayServiceTest`: Kiểm thử đặt khoang, chống chồng lấn thời gian, pessimistic lock, release khoang.
8. `EmployeeShiftServiceTest` & `EmployeeServiceTest`: Kiểm thử CRUD nhân viên, ca làm, chống trùng ca, kỹ năng can_qc.
