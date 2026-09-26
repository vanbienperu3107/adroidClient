# Feature 05 - Agent Interactions

Revision tài liệu: 1.1. Baseline: `08b9bc21d392f045114a180611e7760d33bcbe20` trên branch `feature/05-agent-interactions`. Trạng thái: PLANNED; chưa có implementation và mọi testcase runtime là NOT_RUN.

## Intent và giới hạn

Feature này hoàn thiện tương tác với agent trong OpenCode bounded context: hiển thị tool trace và agent/session status; trả lời permission theo once/always/reject; trả lời hoặc từ chối question; phục hồi pending khi Android offline; đóng dialog nếu desktop đã resolve; và hiển thị diff an toàn, có giới hạn.

Không sửa provider chat hiện hữu (`ChatRepositoryImpl`, `ChatViewModel`, `ChatScreen`), không tạo offline queue, không tự phát lại mutation, không auto-approve, không thêm editor/file tree, và không sửa credential/backup policy ngoài phần regression kiểm chứng. Prompt/SSE/reconnect nền tảng là dependency của Feature 03/04, không được thay thế bằng polling không kiểm soát.

## Bằng chứng baseline và quyết định

- Codebase MCP đúng worktree `workspace-Project-worktrees-05-agent-interactions`, index ready ở SHA baseline; coverage không có recorded issue cho các source được dẫn chiếu, nhưng đây chỉ là best-effort.
- `OpenCodeBrowseRepository.sessions` hiện chỉ đọc `/permission` và `/question` để tạo badge tổng theo `sessionID`; UI hiện nói reply thuộc Feature 05. Chưa có entity/persistence/action API cho pending.
- `OpenCodeReadApi` chấp nhận duy nhất response HTTP 200 JSON và mutation `PATCH`/`DELETE`; Feature 05 cần transport/action contract riêng để xử lý `POST`, body rỗng và response resolve/expired đã được xác minh.
- Probe target 1.18.30 đã pin legacy `/permission`, `/question`, reply/reject path/body/200-400-404 schema, diff entry và envelope SSE; evidence tại [contract-probe-2026-09-25.md](contract-probe-2026-09-25.md). Empty-list và synthetic 404 không thay thế fixture pending thật: semantics resolution/expiry/pagination/tool payload/budget vẫn `PARTIAL`, không suy diễn từ OpenAPI.
- REST snapshot là nguồn trạng thái cuối; SSE chỉ đưa notification/dirty signal. `permission.asked/replied`, `question.*`, `session.diff` chỉ reducer sau khi AI-01 có fixture/schema version-pinned.

## Requirements và acceptance

| ID | Requirement | Acceptance |
| --- | --- | --- |
| R05-01 | Tool trace/status | Render tool/agent status theo scope, unknown fallback an toàn, stable order/key; không thực thi content và không eager-render payload lớn. |
| R05-02 | Permission | Chỉ pending đã xác minh mới actionable; once/always/reject gửi đúng request một lần, khóa duplicate tap, và outcome mơ hồ/expired không tự retry. |
| R05-03 | Question | Reply có validation/bounds, reject là mutation rõ ràng; không dùng dialog close làm bằng chứng server đã nhận. |
| R05-04 | Offline/recovery | Reconnect/foreground/startup snapshot phục hồi pending phát sinh khi Android offline; request đã desktop-resolve biến mất hoặc disabled sau authoritative reconcile. |
| R05-05 | Desktop concurrency | Event/snapshot cũ không reopen dialog đã resolve; session/server/directory/revision khác không bao giờ nhận request hay commit kết quả. |
| R05-06 | Diff | Loading/empty/error/ready rõ ràng; diff parse/render bounded, text untrusted không chạy HTML/link/command tự động, không lộ credential/content vào log/route. |
| R05-07 | Reliability/security | Profile change, auth failure, process death và cancellation dừng action; pending content/raw event không đi backup; cache/DB không trộn scope. |
| R05-08 | Verification | Mapping requirement-testcase và review Normal -> High -> XHigh có evidence đúng revision; NOT_RUN/BLOCKED không được gọi PASS runtime. |

## Thiết kế và invariants

`InteractionScope = serverId + profileRevision + directory + sessionId`; permission/question key thêm remote request ID. Tất cả read/write/reducer/cache/UI state phải mang scope này. Không dùng title, text câu hỏi, tool name, hoặc index danh sách làm định danh.

Pending snapshot được persist riêng với trạng thái `PENDING`, `SUBMITTING`, `RESOLVED` hoặc `STALE`; nội dung persistence là allowlist đã cần để render/reply, không raw SSE. Snapshot chỉ invalidate pending thiếu khỏi danh sách khi AI-01 chứng minh response complete cho directory/scope; nếu không, giữ record non-actionable `STALE` rồi point-read/reconcile, không suy deletion.

Mỗi request action có mutex/in-flight key. Chuyển `SUBMITTING` trước network; POST không retry tự động. Success chỉ là confirmed khi contract nói response đó definitive hoặc authoritative refresh thấy request đã resolved; timeout/cancel/process death chuyển `STALE`/uncertain và refresh. `always` chỉ gửi remote semantic đã được pin, không tự tạo local permanent allow rule; Android không được áp dụng allow cũ sang request/tool/path khác.

Tool, question, permission, diff text là untrusted remote content. Renderer dùng text/Markdown sanitizer hiện hữu với size budget version-pinned từ AI-01, preview + explicit expand/load-more; enforce input và decoded byte/line/file-hunk bounds trước allocation. Không tải URL remote với Authorization, không mở intent/command, và không log raw payload. Diff display chỉ mô tả remote change, không tạo thao tác apply/write local.

Dialog là projection của current pending key. Khi snapshot/SSE authoritative resolve, key mất scope, profile revision đổi, hoặc user presses dismiss: đóng dialog và hủy UI collection/action nếu chưa committed; close không gọi reject. Kết quả response muộn chỉ commit nếu generation + scope + request key vẫn current.

## Dependencies, assumptions và blockers

- Phụ thuộc implementation/evidence Feature 03 (`prompt`/session UI) và Feature 04 (single active SSE, lifecycle/reconcile/generation). Nếu chưa được tích hợp vào baseline, Feature 05 chỉ bắt đầu các adapter phù hợp sau khi contract tương ứng được present.
- AI-01 là gate `PARTIAL`: target version đã pin endpoint/method/body, enum once/always/reject, 200/400/404, IDs/schema và SSE names. Fixture pending thật vẫn bắt buộc trước reply/reject/reducer/persistence: xác minh resolution/expiry, directory/session ownership, pagination/completeness, tool payload, max payload và event desktop-resolved.
- Wording đã chốt tại [decisions.md](decisions.md): `Always allow — phạm vi và thời hạn do OpenCode server quyết định.` AI-01 vẫn phải xác minh remote capability; nếu server không có `always`, UI không hiển thị action giả.
- Không đưa secret, raw payload, user prompt, diff/tool output hoặc pending action vào SavedStateHandle, route, logs, analytics, Auto Backup hay device transfer.

## Implementation order

Thứ tự bắt buộc: AI-01 -> AI-02/AI-03 -> AI-04 -> AI-05 -> AI-06 -> AI-07 -> AI-08. AI-09 chạy sau mọi item implementation. Một item chỉ COMPLETE khi acceptance và testcase đúng lớp có evidence; trạng thái trong backlog hiện là NOT_STARTED, không phải completion claim.

## Rollback

Rollback chỉ tắt route/UI interaction mới và dừng collector/SSE interaction; không gửi reject/approve để "dọn" remote. Pending cache có thể bị invalidated theo đúng server/revision sau reconcile, không xóa profile/vault/ChatDatabase. Nếu schema cache đã phát hành, dùng migration hoặc reset riêng OpenCode cache có kiểm soát, không rollback bằng xóa dữ liệu mù.
