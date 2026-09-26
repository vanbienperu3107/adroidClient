# Testcases Feature 02 — tất cả NOT_RUN

Tiền điều kiện chung: server test được cấp quyền, hai directory disposable A/B và hai profile server S1/S2; dữ liệu tổng hợp, không dùng session công việc. Fixture gắn version, loại LIVE/SYNTHETIC và khử secret; test mutation chỉ trên session test. Unit/fake, Room instrumented, native UI, network engine thật là các lớp riêng.

Mỗi kết quả phải ghi ID, SHA, precondition thực tế, steps, expected/actual, device/API/build variant/transport khi liên quan và evidence path. Chưa có môi trường ghi BLOCKED; không tính NOT_RUN là PASS.

| ID / requirement | Precondition | Steps | Expected result | Evidence / lớp |
| --- | --- | --- | --- | --- |
| T01 / R01..06 | Schema version-pinned, fixture live được cấp | Probe project/current/list/status/history/update/delete; kiểm tra pagination/end markers, HTML fallback, payload limit; ghi consistency guarantee | Contract matrix ghi đủ và chặn những semantics chưa biết trước code phụ thuộc; không mặc định [] chứng minh full snapshot | Sanitized API matrix + fixture provenance; live integration |
| T02 / R06 | Fake profile observable + controlled request barrier | Start request revision 1; đổi credential/endpoint hoặc xóa; release response cũ; lặp với crash trước invalidation event | Không commit/expose cache revision cũ; startup loại orphan; không data resurrection | Unit coordinator + integration DataStore/Room |
| T03 / R01,R05 | Models S1/S2 và A/B, cùng remote IDs | Insert/query cùng session/message IDs ở bốn scope; đường dẫn chữ hoa, Unicode, space, trailing separator theo contract | Keys độc lập; không tự lowercase/normalize remote path sai semantics | Unit + Room key assertions |
| T04 / R04,R05 | Fixtures role/part types known và unknown | Parse valid, field lạ, null bắt buộc, duplicate ID, parent/scope sai, truncated JSON | Unknown optional an toàn; required/schema lỗi reject batch; cache cũ không bị xóa | Mapper unit report |
| T05 / R05,R07 | Android DB test sạch và chat DB có records | Open cache v1, insert FK chain; delete scope A; kiểm tra indices/FKs; reopen | Cascade đúng scope; DB chat/provider không đổi; schema export có version; cache noBackup | Room instrumented schema+record assertions |
| T06 / R01,R07 | Credential fake, real HTTPS test endpoint | Basic success/wrong auth; 403/404/500/timeout; cert sai, redirect khác host; hủy request; path prefix | Error phân biệt, không forward secret, cancellation không thành retry; health Feature01 vẫn đúng | Unit headers/mapping + TLS engine integration, sanitized logs |
| T07 / R01 | Project API fixture có ID global, A/B, empty/current | Refresh project list, select current, mất mạng, project không còn | Directory là scope có evidence, không chỉ projectID; cached list stale, không lẫn server | Repository integration |
| T08 / R01,R06 | Native UI với 2 profiles/projects | Select A, open session, Back, rotate/restart, đổi S2, restore missing credential | Route local IDs đúng; selection không bypass reauth, không route raw secret/directory | Compose/device assertions + screenshots |
| T09 / R02,R05 | Controlled list/status responses | Map busy/retry; valid empty status map; status request 401/timeout; swap response order | Idle chỉ khi status snapshot hợp lệ đủ; lỗi=unknown/stale; latest generation thắng | Sync unit/integration |
| T10 / R02 | Session list data empty/ready/offline/error | Native refresh, scroll list lớn, open session, quay lại | States đúng; cached items giữ khi lỗi; pending unknown không bị hiển thị như 0 | Native UI report |
| T11 / R03 | Disposable session owned by A | Rename; reject blank title; desktop đổi title xen kẽ; timeout sau PATCH; reconnect | Không blind retry hoặc rollback đè title desktop; canonical fetch; không mutate scope B | Live API + mutation controller tests |
| T12 / R03,R05 | Disposable A/B, confirm dialog | Cancel; confirm delete A; timeout/mất response; request 404 với scope/auth sai; refresh | Cancel không gửi; không xóa cache B; timeout giữ uncertainty tới authoritative lookup; không suy mọi 404 là deleted | Native + live/injected transport |
| T13 / R04,R05 | History nhiều page và part fixture | Load first page rồi trang tiếp, refresh full; message/part bị xóa; trả trang lỗi giữa chừng | Partial không prune ngoài trang; full consistent mới prune; message/parts transaction; stable ordering | Room integration assertions |
| T14 / R05,R06 | Barrier giữa network response và DB commit | Snapshot cũ đến muộn; desktop thêm/xóa giữa các page; kill transaction; đổi scope; duplicate refresh | Không mất message do pagination không consistent; DB rollback; no stale generation writes; restart stale rồi reconcile | Unit barriers + Room/native crash tests |
| T15 / R04 | Parts Markdown/reasoning/tool/unknown/large | Open history, collapse, copy, load more, accessibility/font scale | Read-only, không chạy tool/HTML/remote content tự động; stable keys; bounded memory/render, không sender controls | Native screenshots/performance trace + renderer unit |
| T16 / R06,R07 | Cache S1/A và S2/B; profile changes | Delete S1, mất event rồi restart; đổi endpoint/credential cùng S2 ID; late response | Cache revoked bị purge/invalidate trước expose; S2 khác không ảnh hưởng; không lấy credential cũ | Repository/Room integration |
| T17 / R07 | Cache có synthetic secret markers, export Feature01 | Cloud/D2D backup theo platform, restore trên device mới; inspect payload | Không cache DB/WAL/SHM/message/title; metadata allowlist thôi; reauth trước network; legacy chat backup giữ | Backup transport report, redacted inventory |
| T18 / R08 và regression | Diff Feature02 + parent baseline | Chạy unit, Room/native/API checks; setup provider và Ollama HTTP; upgrade/reopen chat cũ; check SHA CI/review N→H→X | Không dùng CI parent thay feature; failures xử lý; provider/history không đổi; mọi requirement có result/evidence đúng lớp | Evidence manifest, regression/CI URLs, review verdict |

## Coverage và boundary

- Happy/empty/error: T04,T06..13; security: T02,T03,T06,T16,T17; concurrency: T02,T11,T14; restart/recovery: T05,T08,T14,T16,T17; payload boundary: T01,T04,T10,T15.
- T01 phải cụ thể hóa các giá trị budget và API pagination sau probe. Test dùng boundary limit-1/limit/limit+1, không chốt ngưỡng sản phẩm bằng suy đoán.
- Test mutation/cleanup phải ghi danh sách session do test tạo; chỉ xóa fixture do test sở hữu. Không auto-approve permission hoặc gọi tool agent ngoài scope.
- Mapping work item nằm trong backlog.md. Mọi acceptance dựa endpoint chưa pin phải BLOCKED tới PS-01; thiết kế testcase không chứng minh API live đã PASS.
