# Kế Hoạch Phân Công Kỹ Thuật Viên QC Ngay Tại Màn Lệnh Sửa Chữa

> **Mã plan**: `qc-assignment-on-order`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-qc-admin-on-behalf.md`. Plan đó cho admin ghi QC thay kỹ thuật viên được phân công, nhưng chưa có đường nào trên giao diện để phân công QC khi phiếu đã chờ QC.

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng

**Phiếu `WAITING_QC` không còn chỗ nào trên giao diện để phân công kỹ thuật viên QC.**

- Chỗ phân công duy nhất là nút "Phân công" trong `features/repair/components/TaskWorkPanel.tsx:96`, nằm trong từng công việc ở tab "Công việc". Nút bị ẩn khi `finished` (công việc `COMPLETED`/`CANCELLED`, dòng 63).
- Phiếu chỉ sang `WAITING_QC` khi mọi công việc đã `COMPLETED`, nên đúng lúc cần QC thì mọi nút "Phân công" đều biến mất.
- Backend lại cho phép: `repair/service/impl/LaborExecutionServiceImpl.java:69` cố ý bỏ qua điều kiện "công việc đã hoàn thành" khi vai trò là QC.
- Hệ quả: admin bấm QC trên phiếu chưa có người QC bị từ chối `QC_INSPECTOR_NOT_ASSIGNED` (2020, `quality/service/impl/QualityServiceImpl.java:77`) mà không có cách xử lý từ giao diện. Kỹ thuật viên cũng bị chặn tương tự ở `identity/service/impl/ObjectAccessServiceImpl.java:89` (`assignedToQc`).
- Phân công QC gắn với một công việc cụ thể (`POST /repair-tasks/{taskId}/assignments`), người dùng khó hiểu "QC của phiếu" lại chọn qua "một công việc".
- Tab QC (`features/repair/components/RepairOrderDetailModal.tsx:896-910`) chỉ liệt kê tên thợ QC đang trực ca, không cho phân công và không cho biết ai đã được phân công.
- Các nút "Bắt đầu kiểm tra QC" ở dòng 913 và khoảng dòng 925 vẫn dùng `canTechnical` và không kiểm tra trạng thái, lệch với nút QC ở dòng 332 (đã giới hạn theo `canPerformQc` và `WAITING_QC`).
- Đổi người QC hiện phải gỡ phân công cũ rồi tạo mới trên công việc. Nếu phiếu có hai phân công QC đang hoạt động, admin ghi QC bị từ chối `QC_INSPECTOR_INVALID` (2021).

### 1.2. Mục tiêu

- Ngay ở tab QC của lệnh sửa chữa, admin và lễ tân thấy ai đang được phân công QC và phân công hoặc đổi người bằng một thao tác, khi phiếu `WAITING_QC`.
- Chỉ chọn được kỹ thuật viên đủ điều kiện: đang hoạt động, có quyền QC, đang trong ca, không phải thợ chính của phiếu. Mọi lúc phiếu chỉ có tối đa một kỹ thuật viên QC đang hoạt động.
- Sau khi phân công, admin hoặc chính kỹ thuật viên đó ghi QC được ngay, không phải sang màn Khoang & lịch ca.
- Các nút "Bắt đầu kiểm tra QC" trong tab QC nhất quán với nút ở đầu phiếu: chỉ hiện cho `ADMIN`/`TECHNICIAN` khi phiếu `WAITING_QC`.

### 1.3. Ngoài phạm vi

- Không đổi quy tắc ghi QC của plan `qc-admin-on-behalf` (admin ghi thay, bốn mắt, điều kiện ca).
- Không cho `TECHNICIAN` tự phân công QC cho mình. Chỉ `ADMIN` và `RECEPTIONIST` phân công, giống `assignTask` hiện tại.
- Không đổi phân công thợ chính và thợ phụ, không đổi `TaskWorkPanel`.
- Không cho admin ghi QC khi chưa phân công QC (đã chốt giữ yêu cầu phân công).
- Không đổi màn Khoang & lịch ca. Xếp ca cho kỹ thuật viên vẫn làm ở đó.
- Không thêm migration. Dữ liệu vẫn nằm ở `task_assignments`.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: `POST /api/v1/repair-orders/{id}/qc-assignment` khi phiếu `WAITING_QC` tạo một phân công vai trò `QC` đang hoạt động cho kỹ thuật viên đủ điều kiện, gắn vào công việc cuối cùng của phiếu. Phản hồi 201. _Kiểm chứng_: unit test `LaborExecutionServiceTest`.
- **AC-2**: Gọi lại với kỹ thuật viên khác thì phân công QC cũ được giải phóng và chỉ còn đúng một phân công QC đang hoạt động. _Kiểm chứng_: unit test kiểm tra `isActive=false`, `releasedAt` ở bản cũ và `findActiveQcForOrder` trả một bản.
- **AC-3**: Bị từ chối bằng lỗi nghiệp vụ rõ ràng và không đổi dữ liệu khi:
  - phiếu không ở `WAITING_QC`;
  - phiếu chưa có công việc nào;
  - kỹ thuật viên không hoạt động, không có `canQc` hoặc không trong ca (các lỗi này đã có ở `assignTask`);
  - kỹ thuật viên là thợ chính (`REPAIR_MAIN`) của phiếu.

  _Kiểm chứng_: unit test từng trường hợp.
- **AC-4**: `GET /api/v1/repair-orders/{id}/qc-assignment` trả phân công QC đang hoạt động (có tên kỹ thuật viên) hoặc `data: null` khi chưa có. _Kiểm chứng_: unit test và gọi API.
- **AC-5**: Chỉ `ADMIN`/`RECEPTIONIST` gọi được `POST`. `TECHNICIAN` và `CUSTOMER` nhận 403. _Kiểm chứng_: kiểm tra `@PreAuthorize` bằng test hoặc gọi API với hai vai trò.
- **AC-6**: Tab QC của phiếu `WAITING_QC` hiện dòng "Kỹ thuật viên QC được phân công: <tên>" hoặc "Chưa phân công". Admin và lễ tân thấy nút "Phân công QC" (hoặc "Đổi người"). Vai trò khác không thấy nút. _Kiểm chứng_: thao tác trình duyệt với admin, lễ tân và kỹ thuật viên.
- **AC-7**: Modal phân công QC chỉ liệt kê kỹ thuật viên có `canQc` đang trong ca. Phân công thành công thì tab QC cập nhật ngay, lỗi API hiển thị bằng `errorMessage`. _Kiểm chứng_: thao tác trình duyệt.
- **AC-8**: Sau khi phân công, admin bấm "Kiểm tra chất lượng (QC)" ghi được biên bản (không còn lỗi 2020). _Kiểm chứng_: thao tác trình duyệt trên container mới.
- **AC-9**: Các nút "Bắt đầu kiểm tra QC" trong tab QC chỉ hiện với `ADMIN`/`TECHNICIAN` khi phiếu `WAITING_QC`. _Kiểm chứng_: thao tác trình duyệt và `npm run lint`/`tsc`.
- **AC-10**: Test hiện có vẫn qua: `LaborExecutionServiceTest`, `QualityCheckServiceTest`, `TV3WorkflowIntegrationTest`, `BusinessWorkflowIntegrationTests`, Vitest. _Kiểm chứng_: `./mvnw test` (bật `mysql-test` cho test tích hợp) và `npm --prefix frontend test`.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `repair/dto/request/QcAssignmentRequest.java` | file mới | DTO `employeeId`, `startsAt`, `endsAt` với Jakarta Validation có message |
| `repair/service/LaborExecutionService.java` | 15-17 | Thêm `assignQcInspector` và `getQcAssignment` |
| `repair/service/impl/LaborExecutionServiceImpl.java` | 60-153 (`assignTask`), 157-177 (`releaseAssignment`) | Thêm 2 hàm mới. Tái dùng kiểm tra của `assignTask` bằng cách tách phần tạo phân công thành hàm private dùng chung |
| `repair/controller/RepairOrderController.java` | gần 134-150 (`/{id}/tasks`) | Thêm `POST` và `GET /{id}/qc-assignment`, `@PreAuthorize`, `@Operation` |
| `repair/repository/TaskAssignmentRepository.java` | 19-21 | Dùng lại `findActiveQcForOrder`. Không cần query mới |
| `src/test/.../repair/LaborExecutionServiceTest.java` | cuối file | Test cho AC-1 → AC-4 |
| `frontend/src/features/repair/api/repairApi.ts` | 94-117 | Thêm `assignQcInspector`, `getQcAssignment` |
| `frontend/src/features/repair/hooks/useRepair.ts` | cạnh `useAssignTaskMutation` | Thêm `useQcAssignmentQuery`, `useAssignQcInspectorMutation` (invalidate query của phiếu) |
| `frontend/src/features/repair/utils/assignmentWindow.ts` | file mới | Chuyển `toLocalInput` và `DEFAULT_ASSIGNMENT_HOURS` từ `AssignTaskModal.tsx:10-22` sang để dùng chung |
| `frontend/src/features/repair/components/AssignTaskModal.tsx` | 10-22 | Import từ `assignmentWindow.ts`, không đổi hành vi |
| `frontend/src/features/repair/components/AssignQcModal.tsx` | file mới | Modal chọn kỹ thuật viên QC và khoảng thời gian |
| `frontend/src/features/repair/components/RepairOrderDetailModal.tsx` | 896-935 (tab QC), 1057 | Thêm khối phân công QC, gắn `AssignQcModal`, đổi điều kiện hai nút "Bắt đầu kiểm tra QC" sang `canPerformQc && WAITING_QC` |
| `docs/REPAIR_ORDER_LIFECYCLE.md` | mục 2.5 | Ghi cách phân công QC tại tab QC |

## 4. Hợp Đồng Kỹ Thuật

**Endpoint** (đường dẫn dùng `ApiPaths.REPAIR_ORDERS_BASE`):

```
POST /api/v1/repair-orders/{id}/qc-assignment   @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)   -> 201 ApiResponse<TaskAssignmentResponse>
GET  /api/v1/repair-orders/{id}/qc-assignment   @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF) -> 200 ApiResponse<TaskAssignmentResponse | null>
```

**DTO** `QcAssignmentRequest`:

```java
@NotNull(message = "ID nhân viên không được để trống") private Long employeeId;
@NotNull(message = "Thời gian bắt đầu không được để trống") private Instant startsAt;
@NotNull(message = "Thời gian dự kiến kết thúc không được để trống") private Instant endsAt;
```

**Service** (`LaborExecutionService`):

```java
TaskAssignmentResponse assignQcInspector(Long actorUserId, Long orderId, QcAssignmentRequest request);
TaskAssignmentResponse getQcAssignment(Long orderId); // null khi chưa có
```

Quy tắc `assignQcInspector` (`@Transactional`, khóa phiếu bằng `findByIdForUpdate`):

1. Phiếu phải `WAITING_QC`, nếu không ném `BusinessRuleException("Chỉ phân công kỹ thuật viên QC khi phiếu đang chờ QC")`.
2. Lấy công việc cuối cùng của phiếu làm điểm gắn. Không có công việc: `BusinessRuleException("Phiếu chưa có công việc để gắn phân công QC")`.
3. Nếu kỹ thuật viên là thợ chính của phiếu (`RepairOrderAssignmentRepository.findByRepairOrderIdAndAssignmentStage(..., REPAIR_MAIN)`): `BusinessRuleException("Thợ chính đã sửa chữa không được làm kỹ thuật viên QC của phiếu này")`.
4. Giải phóng mọi phân công QC đang hoạt động của phiếu (`findActiveQcForOrder`): `isActive=false`, `releasedAt=now`. Đóng timer mở nếu có, theo cách `releaseAssignment` đang làm.
5. Tạo phân công mới bằng đường dùng chung của `assignTask` với vai trò `QC`. Phần này đã có các kiểm tra hoạt động, `canQc`, chồng lịch, ca làm việc, kỹ năng. Lỗi nào xảy ra thì cả giao dịch rollback, phân công cũ giữ nguyên.

Không thêm `ResponseCode` mới, dùng `BusinessRuleException` và `ConflictException` có thông báo tiếng Việt như `assignTask`.

**Frontend**:

```ts
assignQcInspector(orderId, { employeeId, startsAt, endsAt }): Promise<TaskAssignment>
getQcAssignment(orderId): Promise<TaskAssignment | null>
```

- `AssignQcModal` dùng `Modal`, `Field`, `Button`, `FormError` chung và `useOnShiftTechniciansQuery`, lọc `canQc`. Bố cục hai cột theo `UI_DESIGN_GUIDELINES.md`, không có ô chọn vai trò.
- Khối trong tab QC dùng class có sẵn (`text-xs muted`, `Button variant="secondary"`), không thêm style mới.
- Điều kiện hiện nút: `canFrontDesk && order.status === 'WAITING_QC'`.

## 5. Checklist Thực Thi

- [x] **Step 1: DTO và service phân công QC** _(AC-1, AC-2, AC-3, AC-4)_
  - Thêm `QcAssignmentRequest`.
  - Thêm `assignQcInspector`, `getQcAssignment` vào `LaborExecutionService` và `LaborExecutionServiceImpl`.
  - Tách phần tạo phân công của `assignTask` thành hàm private dùng chung, giữ nguyên hành vi của `assignTask`.
- [x] **Step 2: Endpoint trong `RepairOrderController`** _(AC-1, AC-4, AC-5)_
  - Thêm `POST` và `GET /{id}/qc-assignment` với `@PreAuthorize` và `@Operation` tiếng Việt.
- [x] **Step 3: Test backend** _(AC-1 → AC-5, AC-10)_
  - Thêm vào `LaborExecutionServiceTest`: phân công thành công, đổi người (chỉ còn một bản đang hoạt động), phiếu không `WAITING_QC`, phiếu không có công việc, thợ chính, kỹ thuật viên không `canQc`, `getQcAssignment` có và không có.
  - Chạy `./mvnw test -Dtest=LaborExecutionServiceTest,QualityCheckServiceTest,TV3WorkflowIntegrationTest`.
- [x] **Step 4: API client, hook và tiện ích dùng chung** _(AC-6, AC-7)_
  - Thêm hàm vào `repairApi.ts`, hook vào `useRepair.ts`.
  - Tách `assignmentWindow.ts` và cập nhật `AssignTaskModal.tsx`.
- [x] **Step 5: `AssignQcModal` và khối phân công trong tab QC** _(AC-6, AC-7, AC-9)_
  - Tạo `AssignQcModal.tsx`.
  - Thêm khối "Kỹ thuật viên QC được phân công" và nút "Phân công QC"/"Đổi người".
  - Đổi điều kiện hai nút "Bắt đầu kiểm tra QC" sang `canPerformQc && WAITING_QC`.
  - Chạy `npm --prefix frontend run lint`, `tsc` và `test`.
- [x] **Step 6: Cập nhật tài liệu** _(AC-6)_
  - Ghi cách phân công QC tại tab QC vào `docs/REPAIR_ORDER_LIFECYCLE.md` mục 2.5.
- [x] **Step 7: Kiểm thử & xác minh** _(tất cả AC)_
  - Bật `mysql-test`, chạy `BusinessWorkflowIntegrationTests`.
  - Build lại container theo `README.md` (jar, frontend, `docker compose build`, `up`).
  - Trên trình duyệt với admin, lễ tân, kỹ thuật viên: phân công QC, đổi người, admin ghi QC thành công, vai trò không đủ quyền không thấy nút.

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-05. Kết luận: **partial** (mọi mục trong phạm vi đã có code; các mục kiểm chứng bằng giao diện và phân quyền chưa xác minh).

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 Phân công QC khi phiếu `WAITING_QC` | Đạt | `LaborExecutionServiceImpl.java:171`; test `assignQcInspector_Success` (vai trò QC, gắn công việc cuối, không ghi nhật ký công) |
| AC-2 Đổi người giải phóng phân công cũ | Đạt | Test `assignQcInspector_ReplacesPreviousInspector` kiểm tra `isActive=false`, `releasedAt` |
| AC-3 Từ chối các trường hợp không hợp lệ | Đạt | Có test: sai trạng thái, chưa có công việc, thợ chính, thiếu `canQc`. Không trong ca và không hoạt động dùng chung kiểm tra của `assignTask` (đã có test riêng ở `assignmentOutsideActiveShiftIsRejected`), chưa có test riêng cho đường QC |
| AC-4 `GET` trả phân công hoặc `null` | Đạt | `LaborExecutionServiceImpl.java:216`; test `getQcAssignment_ReturnsActiveOrNull`. Chưa gọi thử qua HTTP |
| AC-5 Chỉ `ADMIN`/`RECEPTIONIST` gọi `POST` | Chưa xác minh | `@PreAuthorize(HAS_FRONT_DESK)` ở `RepairOrderController.java:138`, chưa có test hay lệnh gọi API với vai trò khác |
| AC-6 Tab QC hiện người được phân công và nút | Chưa xác minh | `RepairOrderDetailModal.tsx:138,911-919`; chưa thử trên trình duyệt |
| AC-7 Modal chỉ liệt kê người QC trong ca | Chưa xác minh | `AssignQcModal.tsx` lọc `canQc` từ danh sách đang trong ca; chưa thử trên trình duyệt |
| AC-8 Admin ghi QC được sau khi phân công | Chưa xác minh | Đường backend đã có test ở `qc-admin-on-behalf` (`adminRecordsQcOnBehalfOfAssignedInspector`); chưa chạy trọn luồng trên container |
| AC-9 Nút "Bắt đầu kiểm tra QC" theo vai trò và trạng thái | Đạt (code) / chưa xác minh UI | `RepairOrderDetailModal.tsx:938,952` dùng `canPerformQc && WAITING_QC`; lint, `tsc`, build qua |
| AC-10 Test hiện có vẫn qua | Đạt | `./mvnw test` 41/41 (gồm `BusinessWorkflowIntegrationTests` với MySQL test), Vitest 111/111 |

Điều chỉnh so với plan: kiểm tra kỹ năng dịch vụ được bỏ qua cho vai trò QC (`createAssignment(..., checkServiceSkills=false)`), vì người kiểm định không cần kỹ năng của dịch vụ gắn vào công việc cuối. `assignTask` thường giữ nguyên.

Còn lại cần kiểm tay trên container mới: phân công/đổi QC bằng admin và lễ tân, kỹ thuật viên không thấy nút, admin ghi QC thành công sau khi phân công, gọi `POST` bằng vai trò kỹ thuật viên nhận 403.
