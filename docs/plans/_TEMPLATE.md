# Kế Hoạch <Tên tính năng>

> **Mã plan**: `<ma-plan-viet-thuong-co-gach>`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: _(nếu có)_ `plans/<file>.md`, mục còn thiếu nào.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
- Vấn đề người dùng gặp, kèm bằng chứng `file:dòng`.

### 1.2. Mục tiêu
- Kết quả mong muốn, viết theo góc nhìn người dùng.

### 1.3. Ngoài phạm vi
- Những gì plan này **không** làm, để converge không tính là thiếu.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

Mỗi tiêu chí phải kiểm chứng được bằng test, lệnh hoặc thao tác trên trình duyệt.

- **AC-1**: Khi <tình huống>, người dùng <vai trò> thấy/làm được <kết quả>. _Kiểm chứng_: <test / lệnh / thao tác>.
- **AC-2**: ...

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `src/main/java/...` | 40–80 | ... |
| `frontend/src/...` | | ... |

## 4. Hợp Đồng Kỹ Thuật

- Chữ ký hàm, DTO, endpoint (`ApiPaths`), `@PreAuthorize`, `ResponseCode` cho `BusinessException`.
- Component UI dùng chung sẽ tái sử dụng.

## 5. Checklist Thực Thi

Mỗi bước ghi rõ những AC mà nó đáp ứng.

- [ ] **Step 1: ...** _(AC-1)_
- [ ] **Step 2: ...** _(AC-2)_
- [ ] **Step N: Kiểm thử & xác minh** _(tất cả AC)_

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->
