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

## RS-D02: SSE baseline target

Probe target version `1.18.30` đã pin legacy `GET /event?directory=...`: Basic Auth, `text/event-stream`, frame `data` JSON `{id,type,properties}`, và không có cursor/resume. Xem [contract-probe-2026-09-25.md](contract-probe-2026-09-25.md).

Do không có replay, mọi reconnect/foreground phải REST authoritative reconcile toàn scope trước stream. `session.deleted` chỉ mark dirty; complete snapshot hoặc owned point-read 404 mới prune cache.

## Gate còn lại

RS-001 là **PARTIAL**, chưa PASS production: chưa có fixture live cho payload non-empty của history/session, 403, abrupt disconnect và concurrent desktop mutation. Không được thêm resume/Last-Event-ID theo suy đoán; các case còn thiếu phải ở fake/controlled server hoặc evidence target trước release.
