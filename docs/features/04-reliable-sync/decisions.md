# Quyết định Feature 04 — Reliable Sync

## RS-D01: Lifecycle và retry policy

**Quyết định người dùng:** áp dụng chính sách đề xuất.

| Hạng mục | Quyết định |
| --- | --- |
| Dirty set | Tối đa 100 session cho mỗi active scope |
| Debounce reconcile | 500 ms, coalesce theo scope/session |
| Retry | Exponential full-jitter từ 1 giây đến tối đa 60 giây |
| Background | Đóng SSE, hủy timer/reconcile chưa bắt đầu; không giữ connection nền |
| Foreground | Health → REST authoritative reconcile → chỉ sau đó mở SSE active scope |
| SSE lỗi liên tục | Sau 5 phút chưa nối lại, chuyển `REST_ONLY`; refresh thủ công và foreground vẫn reconcile; không tự retry mutation |

Dirty overflow không discard state rồi báo synced: đánh dấu scope dirty-all, bỏ event buffer, chạy full REST reconcile. Profile revision/directory/server đổi hủy owner cũ, mọi callback cũ no-op.

## Gate còn lại

RS-001 vẫn BLOCKED cho implementation production: server phải pin SSE endpoint, auth, event envelope, scope, resume/cursor, delete semantics và retry error classes. Quyết định này không biến contract thiếu thành PASS.
