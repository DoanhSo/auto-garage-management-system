# M13 — Notification frontend (TV1)

Đã có private WEB inbox/detail/read/unread-count; API/hooks riêng, polling theo cấu hình và pause khi tab ẩn. Payload chỉ render text, không HTML. Không có composer/outbox/provider console trong P0. Scheduler và delivery thuộc backend; UI không tự gửi EMAIL/SMS.
