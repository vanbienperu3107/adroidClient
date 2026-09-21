# Review Feature 01 — workflow tuần tự

Worktree: `feature/01-secure-foundation`, baseline `4894e54`. Review tài liệu, không phải test runtime hoặc chứng nhận feature đã hoàn thành.

## Fingerprint

- plan.md: `84452fda2215d3c72d6ecd2db8e127c249cc3a55a2a82daa5ae3a9bd016cba84`
- requirements-map.md: `058386080f80072b7b0c95c88bbb77917d57de6f00f6325f3e83fdcd2ea74c1b`
- testcases.md: `aeed544e4e065a1012ec4cfab73605ac882c9485dbf3b558d61c1f97fcba7149`

## Normal — PASS

Đã kiểm tra intent, scope WI-01..06, SF-01..07 và 17 work item con trong requirements-map. Onboarding OpenCode-only đã có acceptance; provider-only giữ hành vi cũ. Persistence DataStore đã đồng bộ với schema/cache của Feature 02. Feature 01 không nhận thêm session/prompt/SSE implementation.

PASS này chỉ áp dụng cấu trúc/phạm vi tài liệu tại fingerprint trên.

## High — PASS WITH DEFERRED GATES

1. **Coverage chi tiết:** testcase map và deferred gates đã được cập nhật. Case crash/backup/TLS Android vẫn phải được chi tiết hóa khi component tồn tại; không gọi chúng đã chạy.
2. **Phase 0:** các probe HTTPS Basic, client messageID, pending question/permission, reply/reject và SSE event thực đã được bổ sung. User quyết định testcase thiếu hạ tầng/code được skip tạm dưới dạng DEFERRED/NOT_RUN. Bearer không thuộc release Basic-only.

PASS này chỉ mở implementation Basic Foundation. Không mở UI/persistence Bearer hoặc claim Android TLS/Keystore/backup production trước evidence.

## XHigh — PASS cho plan Basic-only với deferred gates

Plan hiện chứa endpoint binding, redirect deny, per-server key, staged reference switch, tombstone/orphan cleanup, revision/generation guard và ba lớp evidence. Failure modes Android thật/backup/Feature 04 reducer được deferred rõ ràng theo owner/phase, không bị xóa hoặc coi PASS. Implementation vẫn phải review XHigh lại trên diff/evidence thực tế.

## Trạng thái bàn giao

- Worktree và phân rã: có.
- Impact review: đã bổ sung onboarding, provider transport/backup, DI, cache Feature 02 và request lifecycle.
- Test coverage: có mapping; Android/hạ tầng cases DEFERRED, chưa chạy.
- Runtime tests/CI/build mới: NOT_RUN; chưa sửa code.
- Implementation Basic-only: được mở gate, phải lặp Normal → High → XHigh trên diff trước merge.

---

## Implementation review — source chưa commit

Fresh Codebase MCP index: `workspace-Project-worktrees-01-secure-foundation`, 1.892 nodes / 5.880 edges. Compile/test evidence: `testDebugUnitTest` PASS local với JDK 17 và Android SDK 34; APK local bị daemon hệ thống kill tại mergeExtDexDebug, nên Android package evidence chuyển CI GitHub.

### Normal — PASS

- Intent Basic-only được giữ: model chỉ có `BASIC`, UI không có Bearer selector; Basic URL/credential/profile/health có bounded context riêng.
- OpenCode entry có tại Start Screen cho người mới và Settings cho provider-only; route chỉ truyền serverId, không truyền URL/credential.
- Room warning tồn tại được sửa có migration 1→2, index foreign key và exportSchema=false.
- Unit tests cover URL policy, health mapping, profile concurrent creates/stale edit/vault failure/metadata edit/export secrecy và backup XML.

### High — PASS WITH DEFERRED GATES

- Client riêng không mutate provider client; redirect disabled, HTTPS policy riêng OpenCode không làm thay đổi Ollama HTTP.
- DataStore profile có mutex mutation; staged vault reference, generation guard tại ViewModel, per-server key/AAD, noBackup vault và export metadata không chứa credential reference/password.
- Backup rules avoid global includes, giảm regression backup GPT Mobile. Static verification PASS.
- Deferred: Android Keystore real process death, actual cloud/device backup restore, Compose emulator UI, HTTPS engine on Android, Bearer gateway. Các mục theo plan 1.2 section 12, không claim PASS.

### XHigh — PASS FOR BASIC-ONLY SOURCE WITH DEFERRED GATES

- Endpoint binding is AAD-bound, redirects blocked, profile mutation/request generation protects stale health response; delete removes vault entry/key; no credential entered in SavedStateHandle/navigation.
- Residual: profile delete currently commits metadata before vault cleanup; cleanup failure needs a persistent tombstone/retry for crash durability. This is not evidenced and remains DEFERRED/NOT_RUN instead of PASS.
- Export write after metadata commit is best effort by design; profile correctness is preserved while export recovery/retry is deferred. No release claim of backup restore durability.
- No app Android UI/browser evidence. Browser cannot substitute native Compose testing.

### Verdict

Source increment is suitable for PR/CI Basic-only review, provided the PR description retains deferred gates. Feature 01 is **not complete** until CI package build and the explicitly deferred runtime gates are either executed or kept excluded from release capability claims.

---

## Final implementation review — commit bc768cc

Evidence: local `testDebugUnitTest` PASS (JDK 17 / Android SDK 34); GitHub PR #6 head `bc768cc` checks PASS: Debug APK run 35669410646, Kotlin Lint run 35669410686, Unit Tests runs 35669409574 and 35669411877. Local assembleDebug remains environment-limited at mergeExtDexDebug; GitHub Debug APK is packaging evidence.

### Normal — PASS

Implementation matches Basic-only intent: URL policy, Basic vault/profile/health, server management entry points, no credential route arguments, backup exclusion/export, and Room migration. Bearer is absent from selectable UI. Unit tests cover 20 tests with no failure according to the local report.

### High — PASS

Separate OpenCode DI/profile store and OkHttp client avoid mutation of existing provider clients. OpenCode URL restrictions do not change global cleartext policy, so existing Ollama transport remains outside this diff. Migration 1→2 is supplied for the new messages chat_id index. CI debug package, lint and unit checks are all green on the reviewed commit.

### XHigh — PASS WITH EXPLICIT DEFERRED GATES

Credential binding uses per-server Keystore alias plus GCM AAD; redirect follow is disabled; profile edits/deletes invalidate stale health results. Profile export omits credential reference/password. Remaining runtime claims are deliberately deferred: real Android Keystore invalidation/process death, actual cloud/device backup restore, emulator UI, Android TLS and Bearer gateway. These are not marked PASS and must stay excluded from release capability claims until evidence exists.

### Final verdict

**PASS for Basic-only source/CI scope.** Deferred gates remain recorded, not waived. UI device evidence is DEFERRED under the user-approved policy; no browser evidence is claimed for native Compose.
