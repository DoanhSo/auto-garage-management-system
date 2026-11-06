# Kế Hoạch Admin Ghi QC Thay Kỹ Thuật Viên Và Sửa Lỗi Biển Số Khi Sửa Xe

> **Mã plan**: `qc-admin-on-behalf`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `docs/REPAIR_ORDER_LIFECYCLE.md` mục 2.5 (QC), `bugs/2026-11-vehicle-edit-license-plate-format-stripping.md`.

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng

**Lỗi A: Admin bấm xác nhận QC thì nhận lỗi "không có quyền thực hiện chức năng này" (`FORBIDDEN` 3002).**

- `quality/controller/QualityCheckController.java:35` cho `ADMIN`, `RECEPTIONIST`, `TECHNICIAN` gọi `POST /quality-checks`.
- `identity/service/impl/ObjectAccessServiceImpl.java:86` cho admin qua bước kiểm tra quyền đối tượng.
- Nhưng `quality/service/impl/QualityServiceImpl.java:86-89` lại tìm hồ sơ nhân viên của **chính người bấm** (`employeeRepository.findByUserId(actorId)`), yêu cầu `isActive` và `canQc`. Admin không có hồ sơ nhân viên đủ điều kiện, nên bị ném `BusinessException(ResponseCode.FORBIDDEN)`.
- Các bước sau cũng dựa trên người bấm:
  - kiểm tra đang trong ca (dòng 92-94);
  - kiểm tra không phải thợ chính (dòng 95-100);
  - ghi nhật ký công `QC_INSPECTION` (dòng 133-137);
  - gán `qcTechnicianId`, dùng để tính thưởng QC.
- Giao diện cũng lệch với backend:
  - `features/repair/components/RepairOrderDetailModal.tsx:331-336` hiện nút "Kiểm tra chất lượng (QC)" cho mọi vai trò trong `TECHNICAL_ROLES` (`app/access.ts:13`, gồm cả `RECEPTIONIST`), dù backend chỉ cho `TECHNICIAN` được phân công QC (`ObjectAccessServiceImpl.java:87-89`).
  - Nút còn hiện khi phiếu `QC_FAILED`, trong khi backend chỉ nhận `WAITING_QC` (`QualityServiceImpl.java:71`).

**Lỗi B: Sửa thông tin xe trả lỗi `2001` "Biển số xe không được để trống" (`PUT /api/v1/vehicles/11`).**

- Code hiện tại đã sửa lỗi này:
  - Commit `bd0a38d` bỏ `@NotBlank` khỏi `vehicle/dto/UpdateVehicleRequest.java:10`.
  - `vehicle/service/VehicleServiceImpl.java:123-133` giữ biển số cũ khi request để trống.
  - Commit `426601b` chuẩn hóa định dạng biển số trên UI.
  - Thông báo trên hiện chỉ còn ở `vehicle/dto/CreateVehicleRequest.java:15`.
- **Nguyên nhân container vẫn lỗi:** `Dockerfile:5` chỉ chép `target/AutoGarage-0.0.1-SNAPSHOT.jar` đã build sẵn, và `frontend/Dockerfile:2` chỉ chép `dist/`. Chạy `docker compose build` mà không chạy `mvnw package` và `npm run build` trước thì image vẫn chứa code cũ.

### 1.2. Mục tiêu

- Admin ghi được kết quả QC **thay cho kỹ thuật viên QC đã được phân công** trên phiếu:
  - Người kiểm định (`qcTechnicianId`), nhật ký công `QC_INSPECTION` và thưởng QC vẫn tính cho kỹ thuật viên đó.
  - Biên bản lưu `checkedBy` là admin, để truy vết ai đã bấm.
- Admin nhận thông báo dễ hiểu khi không ghi thay được, thay cho "không có quyền":
  - phiếu chưa phân công QC;
  - kỹ thuật viên QC không còn hoạt động, không trong ca, hoặc là thợ chính.
- Kỹ thuật viên giữ nguyên quy tắc hiện tại: chỉ người được phân công QC, có `canQc`, đang trong ca và không phải thợ chính.
- Nút QC chỉ hiện với người backend cho phép và chỉ khi phiếu `WAITING_QC`.
- Container chạy đúng code mới nhất: sửa xe để trống hoặc giữ nguyên biển số thì lưu thành công.

### 1.3. Ngoài phạm vi

- Không đổi quy tắc bốn mắt hay điều kiện QC (mọi công việc xong, không còn đồng hồ chạy).
- Không cho `RECEPTIONIST` ghi QC.
- Không thêm màn chọn kỹ thuật viên QC trong form QC. Admin phân công QC ở chức năng phân công hiện có.
- Không chuyển Dockerfile sang build nhiều giai đoạn. Nhánh `dev_duong` (commit `e793ce6`) đang làm việc này. Plan chỉ ghi quy trình build đúng.
- Không sửa lại logic biển số đã có ở develop.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Admin bấm QC trên phiếu `WAITING_QC` đã phân công đúng một kỹ thuật viên QC hợp lệ thì tạo được biên bản:
  - `qcTechnicianId` là kỹ thuật viên đó, `checkedBy` là user admin;
  - nhật ký `QC_INSPECTION` ghi cho kỹ thuật viên đó;
  - phiếu chuyển `COMPLETED` (đạt) hoặc `QC_FAILED` (không đạt).

  _Kiểm chứng_: unit test `QualityCheckServiceTest`, cùng thao tác trên trình duyệt với tài khoản admin.
- **AC-2**: Admin bấm QC trên phiếu chưa phân công QC thì nhận lỗi `QC_INSPECTOR_NOT_ASSIGNED` (HTTP 409) với thông báo "Phiếu chưa phân công kỹ thuật viên QC", không phải `FORBIDDEN`. _Kiểm chứng_: unit test.
- **AC-3**: Admin ghi QC thay trong các trường hợp sau thì nhận lỗi nghiệp vụ rõ ràng (HTTP 400/409), không tạo biên bản:
  - kỹ thuật viên QC được phân công không hoạt động, không có `canQc`, không trong ca, hoặc là thợ chính của phiếu;
  - phiếu có nhiều hơn một kỹ thuật viên QC đang hoạt động.

  _Kiểm chứng_: unit test.
- **AC-4**: Kỹ thuật viên ghi QC giữ nguyên hành vi cũ. Mọi test QC hiện có vẫn qua: `QualityCheckServiceTest`, `TV3WorkflowIntegrationTest`, `BusinessWorkflowIntegrationTests`. _Kiểm chứng_: `./mvnw test`.
- **AC-5**: Nút "Kiểm tra chất lượng (QC)" chỉ hiện với `ADMIN` và `TECHNICIAN`, và chỉ khi phiếu `WAITING_QC`. Lễ tân và phiếu `QC_FAILED` không thấy nút. _Kiểm chứng_: test Vitest/RTL hoặc thao tác trình duyệt bằng hai tài khoản.
- **AC-6**: Sau khi build lại đúng quy trình, sửa xe bằng tài khoản admin hoặc lễ tân thì lưu thành công, kể cả khi giữ nguyên biển số. `PUT /api/v1/vehicles/{id}` không còn trả "Biển số xe không được để trống". _Kiểm chứng_: thao tác trình duyệt trên container mới, cùng `VehicleServiceTest`.
- **AC-7**: Quy trình build container trong `README.md` ghi rõ thứ tự: build jar, build frontend, rồi `docker compose build`. _Kiểm chứng_: đọc README.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `common/enums/ResponseCode.java` | sau dòng 52 (`BAY_UNAVAILABLE`, 2019) | Thêm `QC_INSPECTOR_NOT_ASSIGNED` (2020) và `QC_INSPECTOR_INVALID` (2021) |
| `quality/service/impl/QualityServiceImpl.java` | 41-56 (dependency), 84-100 (chọn người kiểm định), 120 (`checkedBy`) | Tách hàm `resolveInspector(actorId, order)`. Admin lấy kỹ thuật viên QC được phân công, kỹ thuật viên giữ logic cũ |
| `repair/repository/TaskAssignmentRepository.java` | 19-21 | Dùng lại `findActiveQcForOrder`. Không cần query mới |
| `common/security/CurrentUserProvider.java` | 47 (`role()`) | Chỉ đọc, dùng để biết người bấm có phải `ADMIN` |
| `src/test/java/.../quality/QualityCheckServiceTest.java` | cuối file | Thêm test cho AC-1, AC-2, AC-3 |
| `frontend/src/features/repair/components/RepairOrderDetailModal.tsx` | 88-91 (biến quyền), 331-336 (nút QC) | Thêm `canPerformQc` (vai trò `ADMIN`/`TECHNICIAN`), chỉ hiện khi `WAITING_QC` |
| `frontend/src/app/access.ts` | 13-15 | Thêm `QC_ROLES: readonly Role[] = ['ADMIN', 'TECHNICIAN']` |
| `frontend/src/features/repair/components/PerformQCModal.tsx` | đầu Sheet | Với admin, thêm một dòng mô tả "Kết quả được ghi thay cho kỹ thuật viên QC đã phân công" |
| `docs/REPAIR_ORDER_LIFECYCLE.md` | mục 2.5, dòng 131-139 | Thêm dòng quy tắc: admin ghi QC thay kỹ thuật viên được phân công |
| `README.md` | mục chạy Docker | Ghi quy trình build đúng (AC-7) |

## 4. Hợp Đồng Kỹ Thuật

**ResponseCode mới** (`common/enums/ResponseCode.java`):

```java
QC_INSPECTOR_NOT_ASSIGNED(2020, "Repair order has no active QC inspector assigned", HttpStatus.CONFLICT),
QC_INSPECTOR_INVALID(2021, "Assigned QC inspector cannot perform this inspection", HttpStatus.CONFLICT),
```

Ném kèm message tiếng Việt cho người dùng, ví dụ `new BusinessException(ResponseCode.QC_INSPECTOR_NOT_ASSIGNED, "Phiếu chưa phân công kỹ thuật viên QC")`.

**Chọn người kiểm định** (`QualityServiceImpl`, hàm private mới):

```java
private Employee resolveInspector(long actorId, RepairOrder order, Instant inspectionTime)
```

- Người bấm là `ADMIN` (`currentUserProvider.role() == Role.ADMIN`, hoặc đọc `User.role` như `ObjectAccessServiceImpl.actor`):
  - Lấy `assignmentRepository.findActiveQcForOrder(order.getId())`, rồi lấy các `employeeId` khác nhau.
  - Không có ai: ném `QC_INSPECTOR_NOT_ASSIGNED`, message "Phiếu chưa phân công kỹ thuật viên QC".
  - Nhiều hơn một người: ném `QC_INSPECTOR_INVALID`, message "Phiếu có nhiều kỹ thuật viên QC đang được phân công, hãy giữ lại một người".
  - Nạp `Employee` theo id. Nếu không `isActive` hoặc không `canQc`: ném `QC_INSPECTOR_INVALID`, message "Kỹ thuật viên QC được phân công không còn đủ điều kiện kiểm định".
- Người bấm không phải admin: giữ nguyên `employeeRepository.findByUserId(actorId)` và filter, ném `FORBIDDEN` như cũ.
- Sau đó, cho cả hai nhánh, giữ các kiểm tra trên `inspector`:
  - đang trong ca (`workLogRecorder.activeShift`);
  - không phải thợ chính (`REPAIR_MAIN`).

  Message giữ nguyên.
- `check.setCheckedBy(actorId)` giữ nguyên, tức là admin. `order.setQcTechnicianId(inspector.getId())` và `workLogRecorder.start/finish(..., inspector.getId(), QC_INSPECTION, ...)` dùng kỹ thuật viên được chọn.
- `@PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)` ở controller giữ nguyên. `ObjectAccessServiceImpl.requireQualityCheckAccess` giữ nguyên, admin đã được cho qua.

**Frontend**:

```ts
// app/access.ts
export const QC_ROLES: readonly Role[] = ['ADMIN', 'TECHNICIAN'];
// RepairOrderDetailModal.tsx
const canPerformQc = !!user && QC_ROLES.includes(user.role);
{canPerformQc && order.status === 'WAITING_QC' && (<Button ...>Kiểm tra chất lượng (QC)</Button>)}
```

- Lỗi API hiển thị qua `errorMessage(err)` sẵn có trong `PerformQCModal`. Không tự tạo thông báo riêng.
- Dòng mô tả cho admin dùng class `description` sẵn có, không thêm style mới.

**Quy trình build container** (Lỗi B, AC-6, AC-7). Chạy từ thư mục `AutoGarage/`:

```bash
./mvnw -q -DskipTests package
npm --prefix frontend run build
docker compose build backend frontend
docker compose up -d --wait backend frontend
```

## 5. Checklist Thực Thi

- [x] **Step 1: Build lại container đúng quy trình và xác nhận Lỗi B đã hết** _(AC-6)_
  - Chạy 4 lệnh ở mục 4.
  - Vào trình duyệt, sửa một xe và giữ nguyên biển số, xác nhận lưu thành công.
  - Nếu vẫn lỗi, mở hồ sơ `/fix-bug` mới cho xe, không sửa trong plan này.
- [x] **Step 2: Thêm `ResponseCode` QC** _(AC-2, AC-3)_
  - Thêm 2 mã 2020 và 2021 theo mục 4.
- [x] **Step 3: Admin ghi QC thay trong `QualityServiceImpl`** _(AC-1, AC-2, AC-3, AC-4)_
  - Tách `resolveInspector`.
  - Thêm dependency `CurrentUserProvider` (hoặc `UserRepository`).
  - Giữ nguyên nhánh kỹ thuật viên.
- [x] **Step 4: Test backend** _(AC-1 → AC-4)_
  - Thêm test vào `QualityCheckServiceTest`:
    - admin ghi thay thành công (kiểm tra `qcTechnicianId`, `checkedBy`, `workLogRecorder` nhận id kỹ thuật viên);
    - chưa phân công;
    - nhiều kỹ thuật viên QC;
    - kỹ thuật viên không `canQc`;
    - kỹ thuật viên không trong ca;
    - kỹ thuật viên là thợ chính.
  - Chạy `./mvnw test -Dtest=QualityCheckServiceTest,TV3WorkflowIntegrationTest,BusinessWorkflowIntegrationTests`.
- [x] **Step 5: Frontend gate nút QC** _(AC-5)_
  - Thêm `QC_ROLES` và `canPerformQc`.
  - Chỉ hiện nút khi `WAITING_QC`.
  - Thêm dòng mô tả cho admin trong `PerformQCModal`.
  - Chạy `npm --prefix frontend run lint`, `tsc` và `test`.
- [x] **Step 6: Cập nhật tài liệu** _(AC-7)_
  - Ghi quy tắc admin ghi QC thay vào `docs/REPAIR_ORDER_LIFECYCLE.md` mục 2.5.
  - Ghi quy trình build vào `README.md`.
- [x] **Step 7: Kiểm thử & xác minh** _(tất cả AC)_
  - Build lại container theo Step 1.
  - Trên trình duyệt:
    - admin QC một phiếu `WAITING_QC` đã phân công QC;
    - admin QC một phiếu chưa phân công thì thấy thông báo rõ;
    - đăng nhập lễ tân thì không thấy nút QC.

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-05. Kết luận: **partial** (mọi mục trong phạm vi đã có code và test; còn hai mục chưa xác minh trên giao diện).

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 Admin ghi QC thay kỹ thuật viên được phân công | Đạt | `QualityServiceImpl.java:65-90,119`; test `adminRecordsQcOnBehalfOfAssignedInspector` kiểm tra `qcTechnicianId`, `checkedBy` = admin, nhật ký công theo kỹ thuật viên |
| AC-2 Chưa phân công QC báo `QC_INSPECTOR_NOT_ASSIGNED` | Đạt | `QualityServiceImpl.java:77`; test `adminCannotRecordQcWithoutAssignedInspector` |
| AC-3 Các trường hợp không hợp lệ bị từ chối rõ ràng | Đạt | Nhiều kỹ thuật viên QC, thiếu `canQc`, ngoài ca, là thợ chính đều có test riêng |
| AC-4 Kỹ thuật viên giữ hành vi cũ | Đạt | `QualityCheckServiceTest` 15/15, `TV3WorkflowIntegrationTest` 3/3, `BusinessWorkflowIntegrationTests` 6/6 (cần MySQL test bật) |
| AC-5 Nút QC chỉ hiện cho admin/kỹ thuật viên khi `WAITING_QC` | Chưa xác minh | Code: `RepairOrderDetailModal.tsx:91,332`, `access.ts:14`. Chưa có test RTL và chưa thử trên trình duyệt với hai tài khoản |
| AC-6 Sửa xe không còn báo biển số trống | Chưa xác minh | `VehicleServiceTest` 12/12; jar trong container không còn thông báo cũ. Chưa thử `PUT /vehicles/{id}` trên giao diện |
| AC-7 README ghi quy trình build đúng thứ tự | Đạt | `README.md:47-50` |

Lỗi phát hiện trong lúc làm: lần build container đầu tiên của phiên này (trước plan) chỉ chạy `docker compose build` nên image chứa code cũ; đã sửa bằng quy trình ở AC-7.

Còn lại cần người kiểm tra tay: Step 7 phần trình duyệt (admin QC phiếu đã/chưa phân công QC, lễ tân không thấy nút, sửa xe giữ nguyên biển số).
