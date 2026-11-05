# Lỗi: Mất định dạng biển số xe khi load và gửi form chỉnh sửa phương tiện

> **Slug**: `vehicle-edit-license-plate-format-stripping` · **Ngày**: 2026-11-05 · **Kết luận**: `verified`

## 1. Triệu chứng

- **Người dùng thấy**: Khi người dùng click vào nút chỉnh sửa một phương tiện có biển số đúng chuẩn (ví dụ: `14A-567.89`), dữ liệu trên form hoặc khi gửi cập nhật gặp lỗi báo "Giá trị không hợp lệ. Kiểm tra nội dung và độ dài." hoặc bị bóc tách ký tự phân cách (mất dấu `-`), biến chuỗi thành `14A56789`, không cập nhật được.
- **Kỳ vọng**: Biển số giữ nguyên định dạng chuẩn Việt Nam (`14A-567.89`), trường biển số ở chế độ sửa hiển thị rõ ràng (read-only kèm ghi chú không thay đổi biển số tại form sửa), và form gửi yêu cầu cập nhật thành công mà không bị từ chối validation.

## 2. Tái hiện

- **Cách tái hiện**:
  1. Mở danh sách phương tiện tại `/vehicles`.
  2. Bấm "Sửa" một phương tiện có biển số `14A-567.89`.
  3. Bấm "Lưu thay đổi".
- **Kết quả trước khi sửa**:
  1. Backend `VehicleServiceImpl.normalizePlate` bóc toàn bộ dấu gạch ngang và dấu chấm (`replaceAll("[\\s.-]", "")`), làm méo dữ liệu định dạng biển số.
  2. `UpdateVehicleRequest.licensePlate` yêu cầu `@NotBlank`, nhưng `VehicleEditor.tsx` vô hiệu hóa trường biển số (`disabled`) và loại bỏ `licensePlate`, `vin`, `engineNumber` khỏi payload gửi đi khi gọi mutation `update.mutateAsync`.
  3. Backend trả về lỗi mã `2001` (Validation failure), frontend ánh xạ thành lỗi form `"Giá trị không hợp lệ. Kiểm tra nội dung và độ dài."`.

## 3. Nguyên nhân gốc

- **Chỗ lỗi lộ ra**: `frontend/src/features/vehicle/components/VehicleEditor.tsx:143-162` và `src/main/java/com/Group13_65CNTT/AutoGarage/vehicle/dto/UpdateVehicleRequest.java:23`.
- **Nguyên nhân gốc**:
  1. `VehicleServiceImpl.java`: Phương thức chuẩn hóa biển số dùng regex bóc tách ký tự phân cách làm mất định dạng biển chuẩn Việt Nam.
  2. `UpdateVehicleRequest.java`: Đặt `@NotBlank` bắt buộc phải có biển số trong payload cập nhật, trong khi nghiệp vụ sửa xe cho phép giữ nguyên biển số và UI không cho phép sửa biển số tại form này.
  3. `VehicleEditor.tsx`: Sử dụng `disabled` cho ô input biển số thay vì `readOnly`, đồng thời payload gửi lên bỏ sót `licensePlate`, `vin`, `engineNumber`.

## 4. Phạm vi ảnh hưởng

- Quản lý phương tiện (`VehicleEditor`, `VehicleServiceImpl`, `UpdateVehicleRequest`, `VehicleMapper`).
- Hiển thị biển số đồng bộ trên toàn bộ hệ thống (tiếp nhận, bảo hành, ước tính, công thợ).

## 5. Sửa

| File | Thay đổi |
|---|---|
| `src/main/java/com/Group13_65CNTT/AutoGarage/vehicle/dto/UpdateVehicleRequest.java` | Bỏ `@NotBlank` trên `licensePlate`, giữ `@Size(max = 20)` để tương thích khi cập nhật không đổi biển |
| `src/main/java/com/Group13_65CNTT/AutoGarage/vehicle/mapper/VehicleMapper.java` | Thêm helper `formatPlate` chuẩn hóa biển số về dạng `14A-567.89` khi chuyển đổi response/entity |
| `src/main/java/com/Group13_65CNTT/AutoGarage/vehicle/service/VehicleServiceImpl.java` | Chuẩn hóa `normalizePlate` qua `formatPlate`; fallback về biển số hiện tại nếu `request.getLicensePlate()` rỗng |
| `frontend/src/shared/utils/format.ts` | Bổ sung hàm `formatPlate` chuẩn hóa định dạng biển số xe Việt Nam (5 số `14A-567.89`, 4 số `14A-1234`) |
| `frontend/src/shared/types/contracts.ts` | Bổ sung `licensePlate?: string`, `vin?: string | null`, `engineNumber?: string | null` vào interface `VehicleUpdate` |
| `frontend/src/features/vehicle/components/VehicleEditor.tsx` | Chuyển ô nhập biển số sang `readOnly` kèm gợi ý khi sửa; áp dụng `formatPlate` và truyền đầy đủ trường khi mutate |
| `frontend/src/features/vehicle/pages/VehiclesPage.tsx` | Áp dụng `formatPlate` hiển thị biển số dạng chuẩn trên bảng danh sách, thẻ xe và modal |
| `src/test/java/com/Group13_65CNTT/AutoGarage/vehicle/VehicleServiceTest.java` | Cập nhật định dạng biển trong test và bổ sung test `testUpdateVehicle_PreservesFormattedPlate` |

## 6. Xác minh

- Cách tái hiện ở mục 2: Tái hiện và kiểm chứng qua regression test `testUpdateVehicle_PreservesFormattedPlate` và `testCreateVehicle_Success`.
- Test / build:
  - Backend: `VehicleServiceTest` chạy 12/12 tests PASS (`BUILD SUCCESS`).
  - Frontend: `npm test --prefix frontend` chạy 13 test suites / 111 tests PASS (`111 passed, 111 total`).
  - Frontend: `npm run build --prefix frontend` hoàn thành không lỗi.
- Trình duyệt: Form nạp và giữ nguyên biển số có định dạng (`14A-567.89`), trường biển hiển thị read-only kèm hướng dẫn rõ ràng, cập nhật thông tin phương tiện thành công mà không gặp lỗi validation.

## 7. Ghi chú

- Giữ nguyên ràng buộc ODO không giảm khi cập nhật theo đúng nghiệp vụ TV2.
