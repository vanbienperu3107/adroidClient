# Core implementation — repository increment

Worktree feature/01-secure-foundation, baseline commit 550b613; source changes chưa commit.

## Increment đã kiểm chứng

- Mutex serialize toàn bộ mutation vì tất cả profile dùng chung một DataStore document; lock per-server riêng lẻ không ngăn lost update trên document chung.
- Edit ID không tồn tại bị reject trước khi ghi vault; không tự tạo profile thay thế sau delete.
- Metadata-only edit giữ credentialRef và tăng revision.
- Vault save thất bại không chuyển metadata sang reference mới.

## Evidence

Command: `JAVA_HOME=/workspace/.tools/jdk17 ANDROID_HOME=/workspace/.tools/android-sdk PATH=/workspace/.tools/jdk17/bin:$PATH ./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8' testDebugUnitTest`

BUILD SUCCESSFUL, 1m28s. XML reports: 19 tests, 0 failure/error/skipped. Repository 4, URL policy 5, health 3, contract fixtures 6, sample 1.

Repository fingerprint: c62bef55e615df9df750ccacd4712719121443abc82be7a4c923f980be4c2012

Repository test fingerprint: 4c11eb2d58cfd1cae287c4f96559135224f6019082bac60898732661a7034490

MemoryStore/FakeVault tests chứng minh logic repository trong process, không chứng minh persistence Android hoặc crash durability.

## Review gate

Normal cho increment concurrency/stale edit: PASS (implementation/test khớp phạm vi vừa sửa).

High cho toàn core: REQUEST_CHANGES. Chưa có persisted cleanup journal/tombstone/orphan recovery, backup export/restore và request generation guard đầy đủ. Không dùng test xanh để tuyên bố SF-01..05 đã hoàn tất.

XHigh toàn core: NOT_RUN, chờ High PASS. UI/onboarding chưa triển khai. Room migration 1→2 compile thành công nhưng chưa có runtime upgrade test; không coi unit suite này chứng minh không mất lịch sử.

## Basic-only UI and backup increment

- Start Screen provides a `Connect OpenCode` entry for users without an enabled provider.
- Settings provides an `OpenCode servers` entry for provider-only users; routes carry serverId only.
- Basic server list/edit screen supports draft health test, save, metadata-only edit, delete, and typed status. Draft password is Compose memory only, not SavedStateHandle or navigation.
- Profile health result uses a request generation guard; stale result after a newer test/save/delete is ignored.
- Profile export in filesDir contains server metadata only. Runtime DataStore, vault and credentialRef stay under no-backup storage. Export replacement is atomic when supported.

Latest local check: `testDebugUnitTest` PASS after this increment. Local assembleDebug remains NOT_RUN due daemon termination at mergeExtDexDebug; PR CI debug APK check is the packaging evidence. Android emulator/device UI, Keystore process death, real backup transfer and Bearer remain DEFERRED as documented in review.md.
