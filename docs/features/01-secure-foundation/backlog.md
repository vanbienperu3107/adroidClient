# Backlog: 01 Secure Foundation

## Runtime verification follow-up

Người dùng đã cho phép bypass tạm các case chưa có môi trường runtime. Theo dõi SF-FOLLOWUP-01..05 tại [deferred-validation.md](deferred-validation.md). Các task vẫn OPEN/NOT_RUN, không tính PASS. Quyết định cho phép tiếp tục bước tiếp theo, không xóa nghĩa vụ test sau.

**Nguồn:** [plan chi tiet](plan.md) · **Trang thai:** Chua bat dau

**Revision 1.2:** [requirements-map.md](requirements-map.md) là phân rã chi tiết và coverage map; bảng SF bên dưới là epic-level. Đã bổ sung onboarding OpenCode-only và đồng bộ nguồn profile trong docs/plan.md, docs/backlog.md. Gate Phase 0 chưa được coi PASS từ conditional GO; implementation chờ xử lý blocker.

| ID | Work package | Phu thuoc | Output chinh | Done khi |
| --- | --- | --- | --- | --- |
| SF-01 | Types, dependency va URL policy | Contract Spike Basic evidence | Auth/credential/state models, URL validator, dependency catalog | URL policy unit tests PASS |
| SF-02 | Per-server Android Keystore vault | SF-01 | `OpenCodeCredentialVault`, fake vault, error mapping | Isolation/invalidation/401 tests PASS |
| SF-03 | Server profile repository/persistence | SF-02 | Metadata CRUD, atomic vault/profile lifecycle | No plaintext secret, CRUD failure tests PASS |
| SF-04 | OpenCode health client | SF-02, SF-03 | Per-profile HTTP client, auth header, health/error mapping | MockEngine fixtures + all failure mappings PASS |
| SF-05 | Backup allowlist | SF-03 | Metadata export, cloud/device-transfer XML rules | XML/static restore tests PASS |
| SF-06 | Server profile UI/navigation | SF-03, SF-04 | Add/edit/delete/test connection Compose flow | UI verification, no secret in route/state PASS |
| SF-07 | ADR, security review, release gate | SF-01..SF-06 | ADR, test results, CI/release evidence | All checks PASS; Definition of Done met |

## Decision Points

1. **Vault primitive:** Android Keystore AES-GCM, key riêng từng server; ADR chốt IV/tag/AAD/versioned reference trước SF-02, instrumented test bằng key thật.
2. **Profile persistence:** DataStore riêng là nguồn profile chính; export allowlist riêng phục vụ backup. Feature 02 tham chiếu serverId, phải đồng bộ schema tổng thể trước khi tạo bảng server trùng nguồn.
3. **Bearer availability:** DEFERRED. Không triển khai hoặc hiển thị Bearer ở Feature 01; chỉ feature gateway tương lai sau TC-00B-04/05 PASS mới bật.
4. **UI scope:** Compose instrumented/emulator/device; browser không kiểm chứng APK. Thiếu môi trường ghi BLOCKED.

## Bổ sung sau review — revision 1.1

| Work package | Yêu cầu bổ sung | Evidence bắt buộc |
| --- | --- | --- |
| SF-01 | ADR storage/crypto/backup, endpoint binding và phạm vi HTTPS chỉ OpenCode | URL userinfo/query/fragment/port/prefix và authMode matrix |
| SF-02 | AAD binding, key isolation, reference versioning | Key/ciphertext thật; tampering, IV, key mất, lỗi tạm thời |
| SF-03 | Thay “atomic xuyên storage” bằng staged reference switch, tombstone và cleanup sau crash | Fault injection từng điểm create/update/delete; không xóa key đang dùng |
| SF-04 | Revision/generation guard; cancellation; không follow redirect; health false/schema/version/stale | Race tests và HTTPS engine thật |
| SF-05 | Export allowlist riêng; giữ backup provider cũ; restore conflict policy | Payload backup/restore thực tế, không chỉ parse XML |
| SF-06 | Draft tạm, edit metadata không nhập lại secret; đổi binding/authMode phải nhập lại | Recreation, bỏ draft, masking, Test/Edit/Delete race |
| SF-07 | Ba lớp evidence: unit, instrumented/integration, backup thực tế | Gắn revision/build variant/API level; thiếu evidence không PASS |

Các dòng “atomic” ở bảng tóm tắt trên chỉ nghĩa là kết quả nhìn thấy hợp lệ sau phục hồi; không có transaction chung giữa DataStore, file và Keystore. SF-01 phải chốt ADR trước SF-02/SF-03; SF-07 review lại quyết định, không trì hoãn ADR tới cuối.

## Deferred Evidence

Bearer gateway, Android TLS/Keystore/backup thật và reducer/SSE không bị xóa khỏi coverage. Chúng theo mục 12 plan.md: deferred có owner/phase và không được tính PASS cho Feature 01 Basic-only.
