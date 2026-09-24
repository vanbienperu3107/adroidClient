# Backlog Feature 02

Nguồn [plan](plan.md). Mọi item NOT_STARTED; input phải được xác minh, không đọc “phụ thuộc” là đã hoàn tất. Đường dẫn ở cột phạm vi là dưới package gptmobile trừ khi ghi docs/res. Test chi tiết: [testcases.md](testcases.md).

| ID | Input → output | Phạm vi dự kiến | Phụ thuộc | Acceptance / test | Trạng thái |
| --- | --- | --- | --- | --- | --- |
| PS-01 | Server schema/probes → contract matrix, completeness/pagination/budget ADR | docs/features/02-project-session-data/fixtures và ADR | HTTPS test/version target | T01; không suy contract từ [] | NOT_STARTED |
| PS-02 | Profile API hiện hữu → observer + revision/deletion coordinator | data/opencode/profile, domain contracts | PS-01, Feature01 | T02; callback cũ không ghi cache | NOT_STARTED |
| PS-03 | Contract scope → typed scope/key models | domain/opencode/model | PS-01 | T03; server/directory/ID không trộn | NOT_STARTED |
| PS-04 | Schemas → DTO và mapper validation | data/opencode/dto | PS-01,03 | T04; malformed/unknown/scope validation | NOT_STARTED |
| PS-05 | Composite models → Room entities/DAO/schema v1 | data/opencode/database, di, app/build.gradle.kts | PS-03,04 | T05; FK/index/noBackup, không ChatDB migration | NOT_STARTED |
| PS-06 | Vault/URL policy → authenticated REST transport | data/opencode/api, di | PS-02,03 | T06; TLS/redirect/errors/cancellation | NOT_STARTED |
| PS-07 | Project REST → cached project list/current | data/opencode/repository/project | PS-04..06 | T07; remote directory identity đúng | NOT_STARTED |
| PS-08 | Project cache → selection UI/ID routes | presentation/ui/opencode/project, NavigationGraph/Route | PS-07 | T08; restore selection và stale states | NOT_STARTED |
| PS-09 | Session REST/status → cache sync | data/opencode/repository/session | PS-02,04..07 | T09; unknown khác idle, stale generation | NOT_STARTED |
| PS-10 | Session cache → list/status UI | presentation/ui/opencode/session | PS-08,09 | T10; loading/empty/error/stale/native | NOT_STARTED |
| PS-11 | Title edit → PATCH + canonical reconcile | api/session, repository/session, session UI | PS-09,10 | T11; không blind retry/rollback desktop | NOT_STARTED |
| PS-12 | Confirm delete → remote result + local cleanup | repository/session, session UI/DAO | PS-09,10 | T12; timeout/404/scope giữ đúng dữ liệu | NOT_STARTED |
| PS-13 | History REST → message/parts transaction | repository/history, database DAOs | PS-02,04..06,09 | T13; partial/full/deletion rules | NOT_STARTED |
| PS-14 | Snapshot jobs → generation/consistency coordinator | repository/sync | PS-02,09,13 | T14; crash/race/deletion không mất cache | NOT_STARTED |
| PS-15 | Parts cache → read-only renderer/history UI | presentation/ui/opencode/history | PS-10,13,14 | T15; ordering/unknown/size/accessibility | NOT_STARTED |
| PS-16 | Profile change/delete/restart → cache invalidation | repository/cache, profile coordinator | PS-02,05,14 | T16; đổi endpoint/auth không lộ cache cũ | NOT_STARTED |
| PS-17 | Scope metadata → backup boundary checks | res/xml, database location, metadata export | PS-05,08,16 | T17; content/WAL/SHM excluded | NOT_STARTED |
| PS-18 | Unit/Room/native/network reports → release readiness | tests, docs evidence, CI khi được giao | PS-01..17 | T18; đúng SHA, không test fake thay runtime | NOT_STARTED |

Thứ tự: PS-01 → PS-02/03/04 → PS-05/06 → PS-07/08/09 → PS-10/11/12/13 → PS-14/15/16/17 → PS-18. UI session và history chia riêng để kiểm chứng độc lập. Mỗi item chỉ complete sau testcase/evidence đúng lớp; không viết sẵn kết quả PASS.

Các bảng pending prompt/permission/question trong WI-07 tổng thể là schema đích nhiều phase, không phải tạo trước trong Feature 02. PS-01 ghi ranh giới chuyển giao Feature 03–05. Pending badge chỉ read-only summary nếu contract có evidence, unknown nếu chưa xác định.
