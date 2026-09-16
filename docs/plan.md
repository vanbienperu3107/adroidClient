# Kế Hoạch Tích Hợp OpenCode Vào GPT Mobile

**Phiên bản:** 1.4

**Baseline đã xác minh:** fork hiện tại dùng `ChatDatabase` version 1, chưa có `SecretVault` và chưa có Ktor SSE. OpenCode chưa được tích hợp.

**Quyết định phiên bản 1.4:** Giữ Basic Auth chuẩn và bearer token qua gateway có sẵn, HTTPS cho release, nhiều project, SSE chỉ cho server active, permission/question và sử dụng đồng thời desktop/Android. Thống nhất đồng bộ REST/SSE, phục hồi thao tác xóa và pending requests, tách trạng thái gửi prompt khỏi trạng thái agent. Backup chỉ profile/metadata được cho phép; loại credential và nội dung hội thoại. Keystore dùng key riêng theo server, chỉ xóa credential khi xác định lỗi không thể phục hồi.

## 1. Mục tiêu

Cho phép ứng dụng GPT Mobile trên Android:

- Kết nối tới OpenCode server trên máy công ty.
- Hiển thị danh sách project/session.
- Đọc lịch sử message của session.
- Gửi prompt tiếp tục cùng session.
- Hiển thị assistant response, tool status và diff.
- Reconnect/resync khi mất mạng hoặc app được mở lại.

Mô hình:

Android GPT Mobile
    -> HTTPS/SSE hoặc Tailscale
    -> OpenCode server trên máy công ty
    -> Session, project, filesystem, agent và LLM provider

## 2. Quyết định kiến trúc

Không tích hợp OpenCode như một provider LLM thông thường.

Không sửa trực tiếp các luồng provider hiện tại: các API/SDK provider trong `data/network` (hiện có `AnthropicAPI/AnthropicAPIImpl`; OpenAI/Google/Ollama đi qua SDK) và logic completion trong `ChatRepositoryImpl`.

Không đưa logic OpenCode trực tiếp vào:

- ChatRepositoryImpl
- ChatViewModel
- ChatScreen

Tạo bounded context riêng:

- data/opencode
- domain/opencode
- presentation/ui/opencode

OpenCode là remote agent/session backend, không chỉ là model provider.

## 3. API MVP

Sử dụng API hiện hành của OpenCode:

GET  /global/health
GET  /project
GET  /project/current
GET  /session
GET  /session/{sessionID}
PATCH /session/{sessionID}
DELETE /session/{sessionID}
GET  /session/status
GET  /session/{sessionID}/message
GET  /session/{sessionID}/message/{messageID}
POST /session/{sessionID}/prompt_async
POST /session/{sessionID}/abort
GET  /session/{sessionID}/diff
GET  /event

Permission và question:

- Reply permission phải được xác minh theo phiên bản server trong Phase 0. API V1 SDK dùng `POST /session/{sessionID}/permissions/{permissionID}`; server API mới có thể dùng `POST /permission/{requestID}/reply`.
- Question reply/reject phải được xác minh theo phiên bản server và lưu vào API fixtures trước khi triển khai UI.
- Phải xác minh API đọc danh sách pending permission/question và phạm vi của snapshot. Nếu server mục tiêu không hỗ trợ phục hồi các request này, Phase 0 phải báo blocker và xin quyết định về giới hạn trước khi triển khai.
- Mọi endpoint có workspace routing phải truyền đúng `directory` hoặc định danh project theo contract đã xác minh.

SSE chỉ dùng để cập nhật realtime. REST message history là nguồn dữ liệu chính để resync.

API V2 với durable event cursor sẽ để ở phase sau vì contract còn thay đổi.

## 4. Package đề xuất

data/opencode/

- api/OpenCodeApi.kt
- api/OpenCodeApiV1.kt
- api/OpenCodeSseClient.kt
- dto/OpenCodeSessionDto.kt
- dto/OpenCodeMessageDto.kt
- dto/OpenCodePartDto.kt
- dto/OpenCodeEventDto.kt
- dto/OpenCodePermissionDto.kt
- dto/OpenCodeQuestionDto.kt
- repository/OpenCodeRepositoryImpl.kt
- repository/OpenCodeSyncCoordinator.kt
- repository/OpenCodeConnectionManager.kt
- sse/OpenCodeEventParser.kt
- sse/OpenCodeEventReducer.kt
- sse/ReconnectPolicy.kt
- security/OpenCodeCredentialVault.kt

domain/opencode/

- model/OpenCodeServer.kt
- model/OpenCodeProject.kt
- model/OpenCodeSession.kt
- model/OpenCodeMessage.kt
- model/OpenCodePart.kt
- model/OpenCodeConnectionState.kt
- model/OpenCodePermission.kt
- model/OpenCodeQuestion.kt
- repository/OpenCodeRepository.kt

presentation/ui/opencode/

- server/OpenCodeServerScreen.kt
- server/OpenCodeServerViewModel.kt
- session/OpenCodeSessionListScreen.kt
- session/OpenCodeSessionListViewModel.kt
- chat/OpenCodeChatScreen.kt
- chat/OpenCodeChatViewModel.kt
- chat/OpenCodePartRenderer.kt
- chat/OpenCodePermissionDialog.kt
- chat/OpenCodeQuestionDialog.kt
- chat/OpenCodeDiffBlock.kt

## 5. Database

Khuyến nghị tạo OpenCodeDatabase riêng, không trộn ngay vào ChatDatabase hiện tại.

Các bảng:

- open_code_servers
- open_code_projects
- open_code_sessions
- open_code_messages
- open_code_parts
- open_code_pending_prompts
- open_code_permissions
- open_code_questions
- open_code_sync_state

Khóa dữ liệu phải được namespace theo server và project/directory:

- Project: `(serverId, directory)`.
- Session: `(serverId, directory, sessionId)`.
- Message: `(serverId, directory, sessionId, messageId)`.
- Part: `(serverId, directory, sessionId, messageId, partId)`.
- Permission: `(serverId, directory, sessionId, permissionId)`.
- Question: `(serverId, directory, sessionId, questionId)`.

Quy tắc reconcile:

- Snapshot đầy đủ: upsert và loại các record server đã xóa, chỉ trong phạm vi server/directory/session của snapshot.
- Snapshot phân trang: chỉ cập nhật trang nhận được; không suy ra record ngoài trang đã bị xóa. Chỉ dọn record khi có bằng chứng xóa hoặc hoàn thành snapshot đầy đủ theo contract.
- Pending prompt/optimistic message local tách khỏi record server, không bị xóa theo snapshot history.
- Permission/question: đối chiếu danh sách pending đầy đủ; request không còn hiệu lực được vô hiệu hóa, không tiếp tục hiển thị như có thể trả lời.
- Các lần ghi cùng session phải tuần tự và dùng transaction cho snapshot, message/parts và trạng thái liên quan.

Lý do:

- Schema OpenCode khác chat local.
- Dễ xóa cache OpenCode riêng.
- Không ảnh hưởng database hiện tại.
- Dễ thay đổi theo phiên bản OpenCode.
- Giảm rủi ro migration ChatDatabase.

Nếu bắt buộc dùng chung database, phải tăng version hiện tại từ 1 lên 2 và viết migration đầy đủ. Tuy nhiên, OpenCodeDatabase riêng vẫn là phương án ưu tiên.

## 6. Credential và bảo mật

Fork hiện tại chưa có SecretVault. Tạo `OpenCodeCredentialVault` dùng Android Keystore hoặc encrypted storage trước khi lưu bất kỳ credential OpenCode nào.

MVP hỗ trợ hai chế độ xác thực:

- `Basic`: tương thích trực tiếp với OpenCode chuẩn, gồm username và password. Username mặc định có thể là `opencode` nhưng phải cho phép cấu hình.
- `Bearer`: chỉ dùng khi OpenCode được đặt sau reverse proxy/gateway có sẵn hỗ trợ bearer token. Ứng dụng không triển khai gateway trong MVP.

Server profile phải lưu `authMode` nhưng không lưu credential plaintext.

Đối với cấu hình xác thực, Room chỉ lưu metadata:

- serverId
- baseUrl
- authMode
- credentialRef

OpenCodeCredentialVault lưu:

- username/password cho Basic Auth
- bearer token cho Bearer Auth

Không lưu secret trong:

- Room plaintext
- DataStore plaintext
- SavedStateHandle
- navigation route
- logcat
- notification
- raw event payload
- Android Auto Backup
- device-to-device transfer

### Backup và restore

- Chỉ backup server profile và metadata cho phép rõ ràng: serverId, tên profile, baseUrl không chứa credential, authMode, project/directory, session ID và lựa chọn điều hướng gần nhất.
- Không backup vault, credential, message/part content, prompt body, tool input/output, reasoning, diff, raw events, nội dung permission/question hoặc session title sinh từ hội thoại.
- Không backup pending actions và các trạng thái runtime như Connected, Ready, Synced hay trạng thái request đang gửi.
- Loại database OpenCode chứa nội dung cùng các file phụ khỏi backup; dùng bản export profile/metadata riêng theo allowlist. Không giả định backup rules có thể loại từng bảng trong cùng một database.
- Sau restore: profile/metadata chỉ là thông tin cũ; server chuyển sang `ReauthenticationRequired`. Không tự gửi prompt, abort hoặc trả lời permission/question.
- Sau khi nhập lại credential, chạy quy trình đồng bộ ở mục 8, tải lại nội dung từ server rồi mới cho phép thao tác ghi. Không tự phát lại pending action từ bản backup cũ.

Cấu hình Android backup rules cho cả Android 12+ và các phiên bản cũ hơn được ứng dụng hỗ trợ; thêm kiểm thử xác nhận dữ liệu vault không xuất hiện trong backup payload hoặc device-to-device transfer.

### Keystore và xác thực lại

Dùng key riêng cho mỗi server profile để giới hạn phạm vi sự cố.

| Lỗi | Xử lý |
| --- | --- |
| Truy cập vault thất bại tạm thời | Dừng request xác thực, giữ key/ciphertext, cho phép thử lại; không xóa credential |
| Key mất hoặc invalidated vĩnh viễn | Vô hiệu hóa và xóa entry không thể phục hồi của server đó; yêu cầu nhập lại credential và tạo key thay thế |
| Ciphertext hỏng được xác nhận | Vô hiệu hóa entry đó, yêu cầu nhập lại; không ảnh hưởng profile khác |
| HTTP 401 | Dừng thao tác xác thực, yêu cầu cập nhật credential; giữ entry, không coi là Keystore invalidation |

Không gửi credential rỗng/lỗi và không lặp reconnect vô hạn khi lỗi xác thực. Khi cần nhập lại, chuyển server sang `ReauthenticationRequired`, đóng SSE và dừng request cần xác thực. Giữ profile và dữ liệu local để reconcile, không tự phục hồi credential từ backup. Sau khi thay credential/key, test connection và chạy quy trình mục 8 trước khi cho phép ghi.

Kết nối cá nhân nên dùng:

- Tailscale
- HTTPS
- Basic Auth theo OpenCode chuẩn hoặc bearer token qua reverse proxy/gateway có sẵn

Không mở trực tiếp port OpenCode ra Internet nếu không có authentication và reverse proxy.

Release build chỉ chấp nhận URL `https://`. URL `http://` chỉ được phép trong debug build cho loopback hoặc môi trường kiểm thử được kiểm soát. Không cho phép bỏ qua TLS certificate validation trong release.

## 7. Network và SSE

Tái sử dụng một phần NetworkClient.kt vì đã có:

- Ktor
- JSON serialization
- timeout
- logging
- header sanitization

Fork hiện tại chưa có Ktor SSE. Cần thêm dependency `ktor-client-sse` và cài SSE client riêng cho OpenCode.

Tối đa một SSE connection tại một thời điểm cho toàn ứng dụng; chi tiết lifecycle ở cuối mục này.

Các event MVP:

- server.connected
- server.heartbeat
- session.created
- session.updated
- session.deleted
- session.status
- session.error
- message.updated
- message.removed
- message.part.updated
- message.part.delta
- message.part.removed
- permission.asked
- permission.replied
- question.asked
- question.replied
- question.rejected
- session.diff
- session.idle để tương thích với server cũ dù event này đã deprecated

Event flow:

SSE
    -> parse
    -> lọc theo session
    -> reducer
    -> Room transaction
    -> StateFlow
    -> Compose UI

Unknown event không được làm app crash.

MVP chỉ duy trì một SSE connection cho server đang active, đúng project/directory đang chọn theo contract Phase 0. Khi đổi server/project, đóng stream cũ và chạy quy trình mục 8. Server không active dùng cache và được resync khi mở lại.

## 8. Reconnect và resync

API V1 không có durable event replay ổn định.

### Quy trình chung

Dùng cùng một quy trình khi mở app, foreground, đổi server/project, reconnect và xác thực lại:

1. Hủy kết nối cũ, tạo connection generation mới gắn với server/project; loại callback REST/SSE từ generation cũ.
2. Kiểm tra credential và health. Lỗi auth chờ người dùng xử lý, không tự retry vô hạn.
3. Mở SSE đúng scope, bắt đầu ghi nhận thay đổi trong lúc tải snapshot; trạng thái `Syncing`.
4. Tải REST snapshot session/status, message history của session đang mở, pending permissions và questions.
5. Ghi snapshot theo transaction và quy tắc reconcile ở mục 5. Serialize các lần ghi cùng session.
6. Với dữ liệu bị thay đổi trong lúc tải, đánh dấu cần tải lại và reconcile bổ sung. Chỉ áp dụng delta trực tiếp khi biết baseline tương ứng; không replay delta mù quáng lên snapshot có thể đã chứa delta đó.
7. Khi hoàn thành reconcile và xử lý các thay đổi chồng lấn, chuyển `Ready`. Nếu chưa hội tụ hoặc stream lại mất, giữ `Syncing/Reconnecting`, lên lịch reconcile tiếp và không báo đã synced.

Buffer/dirty set có giới hạn; khi vượt giới hạn, bỏ buffer và yêu cầu full reconcile, không âm thầm đánh dấu synced. Chi tiết điều phối snapshot/event và giới hạn tài nguyên phải được kiểm chứng bằng fixtures trong Phase 0 và race tests ở Phase 5.

REST quyết định trạng thái cuối; SSE giúp cập nhật nhanh và báo dữ liệu cần tải lại. Không cam kết giao event exactly-once. Khi session idle, reconcile history lần nữa.

Khi mất kết nối mạng, retry với backoff `1s, 2s, 4s, 8s, 16s, 30s, 60s` có jitter. Khi background, đóng SSE; foreground chạy lại quy trình chung.

### Phục hồi permission/question

- Tải lại pending requests khi reconnect/foreground, kể cả requests phát sinh lúc Android offline.
- Chỉ bật trả lời sau khi trạng thái được xác minh; request đã được desktop xử lý là trạng thái kết thúc.
- Nếu request hết hiệu lực trong lúc gửi reply, đóng/vô hiệu hóa dialog và refresh pending list; không tự lặp gửi reply.

Prompt UNKNOWN được xử lý theo mục 9, không tự retry sau reconnect.

## 9. Gửi prompt

Dùng:

POST /session/{sessionID}/prompt_async

### Trạng thái gửi prompt

| Trạng thái | Ý nghĩa |
| --- | --- |
| PENDING | Đã lưu local cho lần gửi hiện tại, chưa gửi request; không phải offline queue |
| SENDING | Đang gửi request |
| ACCEPTED | HTTP 204; chưa đủ bằng chứng đối chiếu message |
| CONFIRMED | Đã xác định message tương ứng trên server |
| UNKNOWN | Không xác định được server đã nhận hay chưa |
| FAILED | Có bằng chứng request không được chấp nhận |

Trạng thái agent/session được theo dõi riêng: idle, busy, retry, chờ permission/question, lỗi. CONFIRMED không có nghĩa agent đã chạy xong; idle không chứng minh prompt cụ thể được nhận.

Trình tự:

1. Kết nối ở Ready và SSE đúng scope. Kiểm tra local send guard và trạng thái session gần nhất.
2. Lưu pending prompt và optimistic user message, chuyển PENDING rồi SENDING trước khi gửi.
3. Gọi prompt_async; HTTP 204 chuyển ACCEPTED.
4. Đối chiếu message qua SSE/REST bằng ID/correlation đã xác minh ở Phase 0, không chỉ dựa vào nội dung prompt hoặc session idle.
5. Khi có bằng chứng tương ứng, chuyển CONFIRMED. Response 204 đến muộn không được hạ CONFIRMED về ACCEPTED.
6. Session idle/abort xong kích hoạt reconcile history/status, không tự xác nhận prompt. HTTP success của `POST /abort` không được coi là session đã idle; chờ `session.status` hoặc reconcile status.

Phase 0 phải xác minh client-supplied message ID và semantics idempotency nếu có; không mặc nhiên coi message ID là idempotency key. Nếu không đủ bằng chứng xác nhận, giữ UNKNOWN.

Mỗi session chỉ có một lượt gửi active trên Android. Đây là local guard, không bảo đảm khóa desktop/server. Thay đổi từ desktop phải được reconcile, không dùng trạng thái local làm bằng chứng độc quyền session.

Timeout/mất kết nối sau khi bắt đầu gửi chuyển UNKNOWN, không tự retry. Khi process chết ở SENDING, lần mở sau chuyển UNKNOWN và reconcile; PENDING còn sót cũng không tự gửi. ACCEPTED được reconcile trước khi quyết định tiếp.

Sau reconcile vẫn mơ hồ, người dùng chọn giữ UNKNOWN hoặc chủ động gửi lại với cảnh báo có thể tạo thêm lượt thực thi. Ghi liên hệ với lần gửi cũ; không tự đổi lần cũ thành FAILED chỉ vì không thấy message trong một lần tải.

Không tạo duplicate do tự retry, reducer hoặc reconcile. Không cam kết tránh lượt thực thi trùng khi người dùng chủ động gửi lại UNKNOWN.

## 10. UI

### Server screen

- Danh sách server.
- Add/edit/delete.
- Test connection.
- Hiển thị OpenCode version.
- Hiển thị Connected/Disconnected/Unauthorized.

### Session screen

- Session title.
- Project/directory.
- Updated time.
- Status idle/busy/retry.
- Permission pending badge.
- Rename/delete/refresh.
- Cho phép chọn project/directory trước khi mở danh sách session; một server profile có thể quản lý nhiều project.

### OpenCode chat screen

- Server/project/session ở top bar.
- User/assistant message.
- Markdown.
- Reasoning collapsed.
- Tool trace.
- Patch/diff.
- Error.
- Reconnect banner.
- Send và abort.
- Permission dialog với allow once, always allow và reject.
- Question dialog với reply và reject.
- Tool output và diff phải được giới hạn/truncate khi vượt ngưỡng kích thước, có thao tác xem thêm; không render toàn bộ nội dung rất lớn trong một lần để tránh lag/OOM.

Có thể tái sử dụng:

- Theme hiện tại.
- Markdown dependency.
- Một phần `UserChatBubble` và `OpponentChatBubble` sau khi tách phụ thuộc `ApiType` nếu cần.

Phải xây mới:

- Reasoning block.
- Tool trace block.
- Agent/session status block.
- Permission dialog.
- Question dialog.
- Diff renderer.

Không mở rộng quá nhiều ChatScreen hiện tại.

## 11. Roadmap

### Phase 0: Spike

- Chốt phiên bản OpenCode server.
- Xác minh response contract thực tế cho health, project, session, message, diff và event.
- Xác minh `directory`/workspace routing và cách liệt kê nhiều project.
- Cấu hình server trên máy công ty.
- Kiểm tra Tailscale.
- Gọi health/session/message.
- Gửi prompt thử.
- Đọc SSE.
- Lưu API fixtures.
- Xác minh Basic Auth trực tiếp với OpenCode chuẩn.
- Xác minh bearer token qua reverse proxy/gateway có sẵn nếu môi trường sử dụng chế độ này.
- Xác minh API rename/delete session, permission reply và question reply/reject.
- Xác minh API pending permission/question, tính đầy đủ và phân trang của snapshot; không hỗ trợ phục hồi phải báo blocker trước khi triển khai.
- Xác minh các lỗi 401/403/404/500, TLS, timeout và format event unknown.

Điều kiện hoàn thành Phase 0:

- Khóa phiên bản OpenCode server mục tiêu.
- Có API fixtures và SSE capture đã loại bỏ secret.
- Có ma trận endpoint, auth mode, request/response và error mapping.
- Xác minh `prompt_async` trả 204 và ghi nhận cách tương quan pending prompt với message server.
- Chốt bằng chứng correlation, hỗ trợ client-supplied ID và semantics idempotency; có fixtures cho desktop gửi xen kẽ và timeout sau khi nhận prompt.
- Có fixtures mô tả snapshot/event chồng lấn, dữ liệu bị xóa và pending request phát sinh lúc offline.
- Xác minh directory scoping bằng ít nhất hai project trên cùng server.
- Có quyết định go/no-go trước khi bắt đầu Phase 1.

### Phase 1: Server profile

- Server entity/DAO.
- OpenCodeCredentialVault dựa trên Android Keystore hoặc encrypted storage.
- Basic Auth và bearer token authentication theo `authMode`.
- Backup exclusion rules và kiểm thử backup payload.
- Export backup profile/metadata theo allowlist; loại vault, database nội dung và pending actions.
- Key riêng theo server; phân loại lỗi tạm thời, lỗi vĩnh viễn, ciphertext hỏng và HTTP 401.
- Add/edit/delete.
- Health check.
- Connection state.
- Release từ chối HTTP URL và không cho phép bỏ qua TLS validation.

### Phase 2: Session list

- Project current.
- Project list và project/directory selection.
- Session list.
- Session status.
- Room persistence.
- Session navigation.
- Composite keys theo server và directory.

### Phase 3: Message history

- Message/part entities.
- REST sync.
- Reconcile deletion, phân biệt snapshot đầy đủ với phân trang; bảo toàn pending local.
- Room transaction.
- Chat UI.
- Markdown/reasoning/tool fallback.

### Phase 4: Prompt và SSE tối thiểu

- SSE connection/parser tối thiểu và điều phối snapshot cơ bản để đáp ứng điều kiện Ready trước gửi.
- Pending prompt.
- prompt_async.
- Abort.
- Timeout/UNKNOWN handling.
- CONFIRMED theo correlation; ACCEPTED tách khỏi trạng thái agent.
- Process death, response/event đến ngược thứ tự và UNKNOWN do người dùng quyết định.
- Xử lý thay đổi đồng thời từ desktop và Android trên cùng session.

### Phase 5: Đồng bộ tin cậy và reconnect

- SSE manager.
- Event parser/reducer.
- Delta streaming.
- Backoff.
- Foreground/background reconnect.
- Reconcile.
- Chỉ duy trì SSE cho server active.
- Jitter và thay đổi server trong lúc reconnect.
- Generation guard loại callback cũ; serialize write và xử lý snapshot/event chồng lấn.
- Buffer/dirty set có giới hạn, full reconcile khi vượt giới hạn; không nối delta hai lần.
- Reconcile deletion và phục hồi pending permission/question sau offline.

### Phase 6: Tool/permission/diff

- Tool state.
- Permission dialog.
- Approve/reject.
- Question dialog.
- Question reply/reject.
- Diff screen.
- Session error.
- Đồng bộ permission/question đã được xử lý từ desktop.
- Tải pending requests khi mở lại, xử lý reply hết hiệu lực và không tự gửi lại.

### Phase 7: Production hardening

- Migration tests.
- Security review.
- Kiểm thử Keystore invalidation, lỗi vault tạm thời và luồng nhập lại credential.
- Xác minh credential không có trong Auto Backup hoặc device transfer.
- Tailscale HTTPS.
- TLS certificate failure và release HTTP rejection.
- Log redaction.
- Performance test.
- Tablet/accessibility test.
- Release build.

## 12. Test plan

Unit test:

- DTO parsing.
- Unknown fields.
- Event parser.
- Delta reducer.
- Duplicate events.
- Reconnect policy.
- Error mapping.
- Prompt state machine.
- Auth mode mapping.
- Keystore invalidation mapping.
- Pending prompt reconcile khi desktop và Android cùng thao tác.
- Event duplicate và out-of-order.
- Unknown part/event fallback.

Room test:

- Composite keys.
- Upsert message/part.
- Transaction.
- Delete server cache.
- Pending prompt.
- Migration.
- Composite keys giữa nhiều server/project.
- Restore profile/metadata không có credential, message content hoặc pending actions.
- Permission/question upsert và resolved state.

Network test:

- Health 200.
- 401/403/404/500.
- Timeout.
- SSE heartbeat.
- SSE malformed frame.
- SSE reconnect.
- prompt_async 204.
- Basic Auth thành công/thất bại.
- Bearer Auth qua gateway thành công/thất bại.
- TLS certificate failure.
- Release từ chối HTTP URL.
- Permission reply và question reply/reject.
- Directory scoping giữa ít nhất hai project.

UI test:

- Add server.
- Test connection.
- Session list.
- Message rendering.
- Reconnect banner.
- Send/abort.
- Permission dialog.
- Question dialog.
- ReauthenticationRequired.
- Project selection.
- Prompt UNKNOWN yêu cầu người dùng quyết định.

E2E test:

1. Start OpenCode server.
2. Kết nối Android.
3. Mở session.
4. Gửi prompt.
5. Nhận SSE.
6. Tắt mạng.
7. Bật mạng.
8. Reconcile.
9. Kiểm tra không mất/duplicate message.
10. Restart app.
11. Tiếp tục session.
12. Thao tác đồng thời từ desktop và Android rồi reconcile.
13. Trả lời permission/question từ desktop và xác minh Android đóng dialog hết hiệu lực.
14. Restore backup, xác minh chỉ profile/metadata được phép còn lại; yêu cầu xác thực và tải lại nội dung, không phát sinh request ghi tự động.

Các ca regression bắt buộc:

- Event xuất hiện trong lúc REST snapshot tải: không mất thay đổi, không nối text hai lần và cuối cùng khớp server.
- Desktop xóa session/message/part khi Android offline: reconnect loại đúng dữ liệu cũ; phân trang không xóa nhầm dữ liệu ngoài trang.
- Permission/question phát sinh lúc offline phải xuất hiện sau reconcile; đã trả lời trên desktop thì không còn dialog có thể thao tác.
- Server nhận prompt nhưng Android timeout trước 204: không tự gửi lại; chỉ xác nhận khi có correlation.
- Kill process trong SENDING/ACCEPTED: mở lại reconcile, không phát lại request; PENDING còn sót không trở thành offline queue.
- Desktop/Android gửi cùng nội dung xen kẽ: không xác nhận nhầm prompt bằng text hoặc idle.
- Đổi server/project khi REST/SSE cũ đang xử lý: callback cũ không ghi vào context mới.
- Backup lúc có pending prompt/permission: payload loại nội dung/pending actions; restore không tự gửi hoặc trả lời, kể cả khi gặp dữ liệu runtime cũ.
- Lỗi vault tạm thời không xóa key/ciphertext; invalidation thật chỉ ảnh hưởng đúng profile; HTTP 401 không xóa key.

## 13. Tiêu chí nghiệm thu

MVP đạt khi:

- Android kết nối được OpenCode server.
- Nhìn thấy session đang làm trên máy công ty.
- Chọn đúng project/directory khi một server có nhiều project.
- Đọc được lịch sử message.
- Gửi prompt vào đúng session.
- Nhận được assistant response.
- Hiển thị status cơ bản.
- Mất mạng rồi reconnect không mất message.
- Restart app vẫn mở lại được session.
- Không tạo duplicate do tự retry, reducer hoặc reconcile; gửi lại UNKNOWN có chủ đích của người dùng được cảnh báo có thể tạo thêm lượt thực thi.
- Credential không nằm trong log/database/backup.
- Chỉ profile/metadata theo allowlist được restore; credential và nội dung hội thoại/pending actions không có trong backup. Sau xác thực lại phải reconcile trước khi ghi.
- Snapshot/SSE hội tụ về trạng thái server, bao gồm thao tác xóa; không cam kết exactly-once event delivery.
- Permission/question phát sinh hoặc được giải quyết lúc offline được phục hồi đúng khi mở lại.
- ACCEPTED/CONFIRMED tách khỏi trạng thái agent; process death không tự gửi lại prompt.
- Lỗi vault tạm thời giữ credential; lỗi không thể phục hồi yêu cầu nhập lại đúng phạm vi server.
- Basic Auth hoạt động trực tiếp với OpenCode chuẩn.
- Bearer token hoạt động khi server được đặt sau gateway hỗ trợ bearer.
- Release build từ chối HTTP URL và không bỏ qua TLS validation.
- Permission và question có thể được xử lý từ Android hoặc desktop mà không giữ dialog hết hiệu lực.
- Desktop và Android có thể tiếp tục cùng session với eventual consistency, không làm hỏng lịch sử.
- OpenCode server không bị expose trực tiếp ra Internet.

## 14. Không làm trong MVP

- Chạy OpenCode trực tiếp trên Android.
- Terminal shell UI đầy đủ.
- File editor.
- File tree.
- FCM notification.
- Offline prompt queue.
- V2 durable event replay.
- Auto-approve permission.
- Xây dựng gateway public mới; MVP chỉ tích hợp với reverse proxy/gateway bearer đã có sẵn.
- Đồng bộ raw event log không giới hạn.
