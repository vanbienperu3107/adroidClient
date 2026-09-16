# OpenCode Integration Backlog

**Nguồn:** [Kế hoạch tích hợp OpenCode 1.4](plan.md)

**Nguyên tắc:** Không thêm OpenCode vào `ApiType`, `ChatRepositoryImpl`, `ChatViewModel` hoặc `ChatScreen`. Tích hợp được xây trong bounded context `data/opencode`, `domain/opencode` và `presentation/ui/opencode`.

## Epic A: Contract Spike

Epic này chặn mọi epic triển khai phía sau.

| ID | Work item | Đầu ra và tiêu chí hoàn thành |
| --- | --- | --- |
| WI-00a | Khóa phiên bản OpenCode server, dựng server test và Tailscale | Ghi nhận version; health thành công qua HTTPS. |
| WI-00b | Xác minh authentication | Fixtures 200/401/403 cho Basic Auth trực tiếp; Bearer qua gateway có sẵn nếu môi trường dùng mode này. |
| WI-00c | Xác minh REST contract | Fixtures đã loại secret cho project, session CRUD, status, message, diff và lỗi. |
| WI-00d | Xác minh project/directory scope | Ma trận endpoint x directory/workspace; ít nhất hai project trên cùng server. |
| WI-00e | Xác minh permission/question | Chốt API reply và API đọc pending list. Nếu không thể phục hồi pending request, báo blocker trước implementation. |
| WI-00f | Xác minh correlation prompt | Chốt cách xác nhận message server, client-supplied ID và idempotency nếu có; fixtures timeout-sau-nhận và desktop gửi xen kẽ. |
| WI-00g | Capture SSE | Fixtures cho event MVP, malformed frame và unknown event; toàn bộ secret được loại bỏ. |
| WI-00h | Go/no-go record | Version, endpoint matrix, auth, scope, correlation và các giới hạn được ghi lại trước Phase 1. |

## Epic B: Nền Tảng An Toàn

| ID | Work item | Phụ thuộc | Tiêu chí hoàn thành |
| --- | --- | --- | --- |
| WI-01 | `OpenCodeCredentialVault` | WI-00b | Key Android Keystore riêng theo server; lỗi tạm thời, invalidation, ciphertext hỏng và HTTP 401 được phân loại; lỗi tạm thời không xóa credential. |
| WI-02 | Server profile và repository | WI-01 | Lưu `serverId`, `baseUrl`, `authMode`, `credentialRef`; không có secret trong Room, route hoặc log; UI add/edit/delete. |
| WI-03 | Backup/restore allowlist | WI-02 | Chỉ export profile/metadata được phép; vault, database nội dung và pending actions bị loại ở cloud backup và device transfer; restore sang `ReauthenticationRequired`. |
| WI-04 | Transport policy | - | Release từ chối URL HTTP và không bypass TLS; debug chỉ cho phép loopback hoặc môi trường test được kiểm soát. |
| WI-05 | Health check và connection state | WI-01, WI-02, WI-04 | Map Connected/Unauthorized/Unreachable/Timeout/Incompatible; test Basic/Bearer, 401/403/404/500, timeout và TLS. |
| WI-06 | Navigation OpenCode | - | Bổ sung route trong `Route.kt`/`NavigationGraph.kt`; route chỉ mang ID, không mang URL hay credential. |

## Epic C: Dữ Liệu, Project Và Session

| ID | Work item | Phụ thuộc | Tiêu chí hoàn thành |
| --- | --- | --- | --- |
| WI-07 | `OpenCodeDatabase` v1 | - | Có server, project, session, message, part, pending prompt, permission, question và sync state; dùng composite keys theo server/directory. |
| WI-08 | Project list/current và project selection | WI-05, WI-07 | Một server hỗ trợ nhiều project; mọi request dùng đúng scope. |
| WI-09 | Session list/status và CRUD | WI-08 | Hiển thị idle/busy/retry và pending badge; rename/delete hoạt động qua contract đã xác minh. |
| WI-10 | REST history sync và reconcile | WI-07, WI-00c | Transactional sync; snapshot đầy đủ xử lý xóa, phân trang không xóa nhầm, pending local không bị snapshot xóa. |
| WI-11 | Chat history UI | WI-10 | Render Markdown, reasoning/tool/unknown fallback; tách phụ thuộc `ApiType` khi tái sử dụng bubble; output/diff lớn có truncate và xem thêm. |

## Epic D: Prompt Và SSE Tối Thiểu

| ID | Work item | Phụ thuộc | Tiêu chí hoàn thành |
| --- | --- | --- | --- |
| WI-12 | SSE client/parser tối thiểu | WI-00g | Thêm Ktor SSE; connect theo scope, parse envelope; malformed/unknown event không crash. |
| WI-13 | Prompt state machine | WI-10, WI-12, WI-00f | `PENDING`, `SENDING`, `ACCEPTED`, `CONFIRMED`, `UNKNOWN`, `FAILED`; agent state riêng; 204 đến muộn không hạ `CONFIRMED`. |
| WI-14 | UNKNOWN và process death | WI-13 | Không tự gửi lại; reconcile trước; người dùng chọn gửi lại có cảnh báo duplicate; process chết không phát lại prompt. |
| WI-15 | Abort | WI-13 | HTTP success không đồng nghĩa idle; chờ session status/reconcile; giữ partial response. |

## Epic E: Đồng Bộ Tin Cậy

| ID | Work item | Phụ thuộc | Tiêu chí hoàn thành |
| --- | --- | --- | --- |
| WI-16 | Điều phối snapshot/SSE | WI-10, WI-12 | Generation guard, SSE trước snapshot, dirty set giới hạn, tuần tự write theo session; event chồng lấn snapshot không mất hoặc nối delta hai lần. |
| WI-17 | Reconnect và lifecycle | WI-16 | Backoff có jitter; background đóng SSE, foreground reconcile; không ghi callback context cũ khi đổi server/project. |
| WI-18 | SSE server active | WI-16 | Tối đa một stream ứng dụng; chỉ server/project active dùng realtime, server khác resync khi mở. |
| WI-19 | Phục hồi xóa và pending request | WI-16, WI-00e | Desktop xóa khi Android offline được phản ánh; permission/question pending được tải lại và request đã resolved không còn actionable. |

## Epic F: Tương Tác Agent

| ID | Work item | Phụ thuộc | Tiêu chí hoàn thành |
| --- | --- | --- | --- |
| WI-20 | Tool trace và agent status | WI-11 | Render trạng thái tool an toàn, không render toàn bộ payload lớn ngay lập tức. |
| WI-21 | Permission | WI-16, WI-00e | Dialog allow once/always/reject; desktop reply đóng dialog; reply hết hiệu lực không tự retry. |
| WI-22 | Question | WI-16, WI-00e | Dialog reply/reject; request tạo khi Android offline xuất hiện sau reconcile. |
| WI-23 | Diff | WI-16 | REST/SSE diff; empty/error/loading state; giới hạn render payload lớn. |

## Epic G: Hardening Và Release

| ID | Work item | Phụ thuộc | Tiêu chí hoàn thành |
| --- | --- | --- | --- |
| WI-24 | Security/log review | WI-01-WI-23 | Credential không có trong log, database hay backup; HTTPS/TLS policy được kiểm chứng. |
| WI-25 | Backup/restore E2E | WI-03 | Restore chỉ có profile/metadata; không có credential, content hoặc pending action; không phát sinh request ghi tự động. |
| WI-26 | Keystore E2E | WI-01 | Lỗi tạm thời giữ credential; invalidation chỉ ảnh hưởng server tương ứng; HTTP 401 không xóa key. |
| WI-27 | Performance và accessibility | WI-11, WI-20, WI-23 | Tool/diff lớn không gây lag/OOM; tablet và accessibility pass. |
| WI-28 | Release build | WI-24-WI-27 | Migration, regression suite và release build pass. |

## Đường Găng

```text
WI-00a..WI-00h
  -> WI-01 -> WI-02 -> WI-05
  -> WI-07 -> WI-08 -> WI-09 -> WI-10
  -> WI-12 -> WI-13 -> WI-16
  -> WI-19 -> WI-21/WI-22
  -> WI-24..WI-28
```

## Regression Bắt Buộc

- Event đến trong REST snapshot không được mất thay đổi hoặc nối text hai lần.
- Desktop xóa session/message/part khi Android offline phải được phản ánh khi reconnect.
- Permission/question được tạo hoặc giải quyết trên desktop phải hội tụ đúng trên Android.
- Timeout sau khi server nhận prompt và process death trong `SENDING`/`ACCEPTED` không được tự gửi lại request.
- Prompt giống nội dung từ desktop/Android không được correlation bằng nội dung text hoặc session idle.
- Đổi server/project khi request hoặc SSE cũ chạy không được ghi dữ liệu vào context mới.
- Backup loại content/vault/pending action; restore không tự gửi hay trả lời.
- Lỗi vault tạm thời không xóa credential; invalidation thật chỉ ảnh hưởng đúng profile.
