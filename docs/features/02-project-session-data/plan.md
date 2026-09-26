# Feature 02 — Project, Session và Data

Revision tài liệu: 1.0. Nguồn: [plan tổng thể 1.4](../../plan.md), [backlog](../../backlog.md).
Baseline: `72d41362b7350b8dfa62d785828b20dd125a5a33`, branch `feature/02-project-session-data`, kế thừa Feature 01 chưa merge main. Trạng thái: PLANNED, chưa implementation, testcase NOT_RUN.

## 1. Intent và giới hạn

Người dùng chọn server Basic Auth đã cấu hình, chọn project/directory, xem danh sách session và trạng thái, rename/delete session, đọc lịch sử message/parts từ REST và cache local. App vẫn mở được cache khi offline nhưng thể hiện rõ dữ liệu cũ.

Bao phủ WI-07..11 của backlog tổng thể. Không tạo session mới, không gửi prompt/abort, không SSE, không reply permission/question, không file editor. Reasoning/tool chỉ render lịch sử với fallback an toàn; tương tác và diff chuyên dụng thuộc Feature 05. Pending badge chỉ hiển thị nếu có dữ liệu pending được đồng bộ từ API đã xác minh; chưa biết phải hiện Unknown, không giả số 0.

## 2. Baseline đã khám phá và điều kiện đầu vào

- Codebase MCP đúng worktree: `workspace-Project-worktrees-02-project-session-data`, full index 1934 nodes/6098 edges, 0 skipped; partial parse app/build.gradle.kts và gradlew. Coverage là best-effort, không chứng minh đầy đủ tuyệt đối.
- `OpenCodeProfileRepository` có profiles/save/delete/recordHealth(expectedRevision), chưa có observer/event contract trong interface. Không coi ý tưởng trong plan là implementation.
- `OpenCodeHealthClient` phục vụ health, không phải API session dùng chung. Không gọi lại health cho mỗi request như cơ chế authorization; mọi request phải lấy credential từ vault và áp dụng policy.
- DataStore là nguồn profile duy nhất. Không tạo bảng server config thứ hai và không foreign-key từ Room sang DataStore.
- Kế thừa migration ChatDatabase 1→2 của Feature 01; tuyệt đối không áp dụng hướng dẫn cũ “bump 1→2” trong plan tổng thể lần nữa. Feature 02 tạo database cache riêng version 1.
- Probe server trước đây: directory query scope được list nhưng lookup ID cross-directory có thể trả 200; client phải kiểm tra directory/session ownership trước thao tác ghi. Đây không phải ranh giới phân quyền server.
- Tài liệu spike chưa chứng minh pagination/full-snapshot consistency. PS-01 phải chốt contract bằng schema/fixtures của server target, không mặc định response array là snapshot đầy đủ.
- Bypass runtime tests Feature 01 không tự cấp bypass Feature 02. Các gate runtime phải có evidence hoặc BLOCKED riêng.

## 3. Yêu cầu và acceptance

| Req | Yêu cầu | Acceptance |
| --- | --- | --- |
| R02-01 | Chọn đúng server/project | Credential, directory và cache không trộn giữa server/project; URL không chứa credential |
| R02-02 | Session list/status | Title, updated time, directory, idle/busy/retry/unknown; chỉ suy idle từ status map đầy đủ hợp lệ, không từ lỗi request |
| R02-03 | Rename/delete | Kiểm tra ownership/scope, confirm delete, reconcile sau kết quả mơ hồ; không retry mutation tự động |
| R02-04 | Đọc history | Text/Markdown/reasoning/tool/unknown fallback; thứ tự ổn định, không duplicate message/part |
| R02-05 | Cache và snapshot | Room transaction, không xóa ngoài phạm vi/trang, snapshot cũ không thắng request mới, lỗi không xóa cache |
| R02-06 | Lifecycle/recovery | Đổi/xóa profile hoặc directory hủy request, callback cũ không ghi lại cache; restart dọn orphan cache |
| R02-07 | Data protection | Cache nội dung không backup, không logs/raw exceptions/UI route secret; provider/chat DB hiện hữu không đổi |
| R02-08 | Kiểm chứng | Requirement-test mapping, unit + Room/native + API integration tách biệt, evidence đúng SHA |

## 4. Thiết kế dữ liệu và scope

Đề xuất file dưới `data/opencode/database`, `api`, `dto`, `repository`, `domain/opencode/model`, `presentation/ui/opencode/project|session|history`. Hilt module/qualifier riêng. Tên file là phạm vi dự kiến, không cam kết đã có.

`ServerScope` = serverId + profileRevision + canonical endpoint binding; `ProjectScope` thêm directory từ server, không lowercase path hoặc tự resolve path remote ở Android. Route mang local project key và session ID đã encode, không nhét raw directory vào path route.

Room `OpenCodeCacheDatabase` version 1 đặt ở noBackupFilesDir, export schema vào source control để hỗ trợ migration tương lai:

| Bảng | Key / nội dung |
| --- | --- |
| projects | (serverId,directory), remoteProjectId nullable, display metadata |
| sessions | (serverId,directory,sessionId), title, times, status, statusFreshness |
| messages | (serverId,directory,sessionId,messageId), role, timestamps, metadata allowlist |
| parts | message composite key + partId, type, text/reasoning/tool subset; không archive raw event |
| sync_state | scope + resource, generation, lastSuccess, completeness, continuation token theo contract |

Foreign keys/cascade giữa các bảng Room dùng đủ composite key. Profile bên DataStore là logical owner, không FK xuyên storage. Không tạo pending_prompts/permission/question schema chết ở feature này; đưa vào migration của feature sở hữu khi implement. Badge dùng optional pending summary read-only, không thêm reply semantics.

Không ghi health Connected vào session status. HTTP 401/403 giữ cache nhưng khóa thao tác remote; cache phải ghi stale. Xóa profile sẽ xóa cache; đổi endpoint phải invalidate/xóa cache cũ trước khi sử dụng endpoint mới. Đổi credential cũng đổi revision và buộc revalidation; policy bảo thủ purge cache để tránh dùng dữ liệu thuộc identity cũ.

## 5. REST và đồng bộ

PS-01 tạo matrix version-pinned cho GET project/current, GET project, GET session/list/status/detail, PATCH title, DELETE session, GET history/single message. Ghi param directory, pagination, field optional, bounds, content-type, 404 semantics. Không giả lịch sử array đầy đủ. Không nhầm web fallback HTML 200 là thành công API.

Request đọc lấy snapshot profile revision, project scope và refresh generation. Network chạy ngoài transaction; trước write phải recheck scope/revision/generation. Trong process, profile update/delete và cache commit dùng coordinator guard chung; event là notification, không đủ làm atomic boundary. Startup recheck ownership/revision từ nguồn profile, dọn orphan trước khi expose dữ liệu; xử lý khoảng hở crash giữa hai storage.

Snapshot rules:

1. Partial/paginated response chỉ upsert các record trả về, không đánh dấu ngoài trang deleted.
2. Chỉ prune khi completeness và consistency được contract chứng minh cho đúng scope. Pagination hết trang không tự chứng minh snapshot nhất quán nếu desktop sửa trong quá trình tải.
3. Nếu server không có snapshot token/consistency guarantee, giữ cache ngoài tập trả về là stale và dùng point lookup/authoritative deletion trước khi prune; không suy xóa từ lỗi/response truncated.
4. Messages và parts của một snapshot nhất quán cùng transaction; duplicate IDs hoặc parent ownership sai làm batch fail validation, giữ cache cũ.
5. Không để response generation cũ hoặc của server revision cũ ghi vào cache. Retry GET có giới hạn; mutation không tự retry.

Rename/delete là online-only, thực hiện trên session đã xác minh directory. Rename timeout không rollback đè thay đổi desktop; fetch lại trạng thái chuẩn. Delete timeout không coi remote đã xóa; khóa thao tác lặp, fetch authoritative status. 404 chỉ được coi deleted sau khi xác minh endpoint/scope/auth hợp lệ. Cancel dialog không gửi request.

## 6. UI/lifecycle

Luồng: server list → project selection → session list → history read-only. Tích hợp cả Start Screen và Settings hiện có. Reauth quay về profile rồi tiếp tục target nếu còn tồn tại. Không ép người dùng provider-only vào OpenCode.

States bắt buộc: loading, empty, ready, stale/offline, unauthorized, forbidden, notFound, error, unknown status. Refresh giữ cache khi lỗi. Không hiện send/abort/approve button giả hoạt động.

Dùng LazyColumn với stable keys; Markdown/tool output/part lạ render như text an toàn, không thực thi HTML/command. Payload quá lớn bị giới hạn theo budget cấu hình trong PS-01; không cắt JSON rồi persist như đầy đủ. Collapsed/tool preview có indicator và load-more theo budget. Không tải ảnh/link remote tự động mang Authorization. Lifecycle hủy job khi đổi scope; state holder không giữ plaintext credential.

## 7. Impact, rollback và việc chưa chốt

Xem [impact.md](impact.md) và [backlog.md](backlog.md). Rollback routing/module về baseline chỉ loại feature mới; không destructive migration ChatDatabase. Cache có thể bị bỏ/rebuild sau xác thực, nhưng cần xin phép trước khi xóa dữ liệu chưa xác định là cache. Không rollback DataStore profile bằng bản copy cũ.

Các quyết định kỹ thuật PS-01 (snapshot completeness, list filtering, pagination, limits) phải có ADR/fixtures trước code phụ thuộc. Nếu API không hỗ trợ capability đã cam kết, xin quyết định thay scope, không tự bỏ requirement. Branch kế thừa Feature 01: trước PR/merge phải cập nhật base và review lại nếu Feature 01 đổi contract; không merge Feature 01 trong nhiệm vụ phân rã này.

## 8. Gate

Review tài liệu có thể PASS nếu phân rã/impact/coverage đầy đủ và các prerequisites được chặn rõ. Runtime tests tất cả NOT_RUN tại thời điểm lập kế hoạch. Chỉ mở implementation từng work item khi dependency contract cần cho nó đã được xác minh; hoàn tất feature cần test evidence ở đúng tầng. Không dùng bypass Feature 01 hoặc CI baseline thay PASS Feature 02.
