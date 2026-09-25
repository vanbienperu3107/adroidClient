# Feature 04 — Reliable Sync: Kế hoạch thiết kế

## Intent
Đưa đồng bộ OpenCode browse/session/history sang mô hình **SSE dẫn hướng + REST snapshot có thẩm quyền**. Mục tiêu là cập nhật kịp thời nhưng không để event trùng, đảo thứ tự, lỗi mạng, lifecycle hay thao tác desktop concurrent ghi đè dữ liệu đúng.

## Acceptance criteria và bất biến
- **AC-01:** Mỗi khóa `(serverId, profileRevision, directory/project)` chỉ có một SSE connection active trong process; đổi scope hủy owner cũ.
- **AC-02:** REST/SSE/retry mang generation token; callback stale không được ghi cache, UI, dirty set hay lịch retry.
- **AC-03:** SSE chỉ mark dirty và schedule REST reconcile coalesced; event payload không là cache source authoritative.
- **AC-04:** Dirty set, queue và fan-out có cap; overflow chuyển thành một full-scope snapshot.
- **AC-05:** Reconcile idempotent trước duplicate/out-of-order/delta thiếu; REST snapshot authoritative upsert/prune đúng scope.
- **AC-06:** Chỉ delete qua complete snapshot authoritative hoặc current owned point-GET 404; delete event/page absence chỉ mark dirty.
- **AC-07:** Transient reconnect exponential full-jitter với một timer owner-scoped; auth/contract error không retry mù; foreground/online phục hồi convergence.
- **AC-08:** Local và desktop concurrent writers hội tụ ở server snapshot, không duplicate/cross-project contamination.
- **AC-09:** SSE tuân profile/url/credential policy và không log credential/message body, không cross server/revision/directory.

## Thiết kế
### Coordinator singleton
Thêm `OpenCodeReliableSyncCoordinator` qua DI: mutex, owner map theo scope, monotonic generation, stream/reconcile/retry job, bounded dirty set và telemetry redacted. `activate` tăng generation, structured-cancel owner cũ, bootstrap REST snapshot rồi mở stream. `deactivate`, profile/revision change, background và process teardown hủy owner. Callback check `isCurrent(scope,generation)` trước scheduling và trong cache transaction.

### SSE transport/parser
Abstraction fakeable; request dùng profile/url policy như REST. Parser chỉ nhận envelope được chốt contract, giới hạn frame, xử lý malformed/unknown không crash. Không suy đoán endpoint, cursor hoặc Last-Event-ID.

### Dirty/reconcile
Map event thành ScopeDirty, SessionDirty hoặc HistoryDirty. Delta thiếu nâng cấp scope. Set/map bounded coalesce theo debounce; overflow clear IDs, đặt fullSnapshotRequired, chạy một snapshot. Chỉ snapshot REST upsert/prune; local mutation cũng mark dirty/reconcile.

### Cache/UI/lifecycle
DAO có reconcile transaction scoped server-directory-session-revision, cấm prune scope khác. Repository/ViewModel activate/reconcile thay vì tạo stream parallel. Foreground/online bootstrap snapshot rồi stream; background đóng socket/hủy retry; cache vẫn render stale/offline.

## Trình tự
RS-001 contract; RS-002 transport/parser; RS-003 coordinator; RS-004 DAO reconcile; RS-005 wiring lifecycle/UI; RS-006 tests; RS-007 post-diff Normal→High→XHigh.

## Deferred
Endpoint/schema/auth/resume SSE vẫn chưa có evidence. Chính sách product đã chốt tại [decisions.md](decisions.md): dirty cap 100, debounce 500 ms, full-jitter 1–60 giây, background close và REST_ONLY sau 5 phút. Không có desktop module trong worktree; desktop là external client. Không thay app source, server contract, credential policy, UX, commit/push/merge trong nhiệm vụ này.
