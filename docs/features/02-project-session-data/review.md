# Review phân rã Feature 02 — Normal → High → XHigh

Chỉ review kế hoạch, phạm vi ảnh hưởng và thiết kế testcase. Không review implementation chưa tồn tại, không chạy test runtime. Không dùng bypass Feature01 để đóng test Feature02.

## Revision review

Base source `72d41362b7350b8dfa62d785828b20dd125a5a33`, worktree `feature/02-project-session-data`.

| Tài liệu | SHA-256 |
| --- | --- |
| plan.md | 470d9c434fbbe61388b15dfb881df32987edabbc268a1bd46e38f9797184b97b |
| backlog.md | 08711c7ce58f1fd16886472a8523a4b14952d06e35cef9a9854f99a540d4de35 |
| impact.md | b8d7f10fa822c9f48c35c2cec8d34d154999bac04559b0bae1040b27e270b918 |
| testcases.md | 44e89e43636fd12120993b952d63363171f71fe9c99ea634f38dd435eef69b2d |

## 1. Normal — PASS cho phân rã

- Intent WI-07..11 được map R02-01..08 và PS-01..18; mỗi work item có input/output, phạm vi, dependency, acceptance, status.
- T01..18 có precondition, steps, expected và evidence class. Tất cả NOT_RUN; không nhầm test design với test result.
- Scope read-only history tách khỏi prompt/abort/SSE/permission reply. Rename/delete nằm trong scope rõ ràng, không thêm create session như chức năng sản phẩm.
- Quyết định kỹ thuật chưa đủ contract được gom vào PS-01, có gate trước code phụ thuộc. Không bắt đầu implementation trong yêu cầu phân tích.

## 2. High — PASS cho kiến trúc/impact kế hoạch

Chỉ thực hiện sau Normal PASS.

- DataStore profile là nguồn chính; cache Room riêng, không FK xuyên storage, không đụng ChatDatabase v2 hiện hữu. Work item bổ sung observer/coordinator vì baseline chưa có interface event như plan giả định.
- Impact matrix gồm onboarding/provider/client/backup/DI và Feature03–05; mỗi vùng có regression và rollback.
- Profile changes và DB commit phải có coordinator chung, không chỉ event async để chống TOCTOU. Startup orphan reconciliation xử lý sự kiện lỡ do crash.
- Coverage có network thật, Room instrumented, native UI và backup; CI unit không được thay bằng chứng runtime.
- Branch kế thừa Feature01 chưa merge đã ghi rõ; thay đổi parent contract làm mất hiệu lực review liên quan, phải review lại trước tích hợp.

## 3. XHigh — PASS cho failure-mode design

Chỉ thực hiện sau High PASS.

- Thất bại auth/network không biến thành empty snapshot và không xóa cache. Không prune ngoài page; hết pagination không tự chứng minh snapshot consistency khi desktop đồng thời ghi.
- Composite identity và directory validation không nhầm scope list với authorization. Đổi endpoint/credential phải invalidate cache revision cũ, late response không hồi sinh dữ liệu.
- Rename/delete timeout không auto-retry, không rollback đè remote desktop; query authoritative state trước khi kết luận.
- Message/parts commit cùng transaction, crash không được ghi lastSuccess giả; future pending local không bị snapshot server xóa.
- Bounded render/network, unknown part fallback, no automatic authenticated remote-resource loading và backup exclusion có test mapping.
- Các semantics không thể khẳng định bằng tài liệu được yêu cầu probe ở PS-01, nếu thiếu thì BLOCKED đúng work item, không tự hạ requirement.

## Verdict và handoff

PASS **cho phân rã/impact/testcase design** tại fingerprint trên, không phải production readiness. Có thể bắt đầu PS-01 khi được giao triển khai; PS-02..18 vẫn NOT_STARTED và chờ dependency tương ứng. Chưa commit/push/merge hoặc thay source ứng dụng.

Trước đóng Feature02: cập nhật coverage thành kết quả đã chạy, review implementation Normal → High → XHigh đúng diff/SHA và ghi riêng blocker/runtime evidence thiếu. Không dùng báo cáo này thay approval implementation.
