# Test Cases: 01 Secure Foundation

**Trang thai:** Chua chay. Test case nay se duoc chay sau implementation SF-01..SF-07.

| ID | Work package | Scenario | Expected result |
| --- | --- | --- | --- |
| TC-SF-01 | SF-01 | Release HTTPS URL hop le | Canonical URL duoc chap nhan |
| TC-SF-02 | SF-01 | Release HTTP URL | Reject truoc network request |
| TC-SF-03 | SF-01 | Debug loopback HTTP | Chap nhan chi `localhost`/`127.0.0.1`/`10.0.2.2` theo policy |
| TC-SF-04 | SF-02 | Basic vault round-trip | Credential giai ma dung, khong log plaintext; Bearer DEFERRED |
| TC-SF-05 | SF-02 | Key server A invalidated | Chi A reauth; B van doc duoc credential |
| TC-SF-06 | SF-02 | Vault temporary error / HTTP 401 | Ciphertext khong bi xoa |
| TC-SF-07 | SF-03 | Create profile that bai khi vault save fail | Khong co profile half-configured |
| TC-SF-08 | SF-03 | Delete profile A | Xoa metadata/vault A, khong anh huong B |
| TC-SF-09 | SF-04 | Basic health 200 fixture | Connected + version 1.18.30 parse dung |
| TC-SF-10 | SF-04 | 401/403/404/5xx/timeout/TLS | Map dung state, khong co bypass TLS |
| TC-SF-11 | SF-05 | Cloud backup va device transfer | Chi metadata allowlist, khong credential/content/pending |
| TC-SF-12 | SF-05 | Restore profile | `ReauthenticationRequired`, khong tu request ghi |
| TC-SF-13 | SF-06 | Add/edit profile Basic | Password masked, route chi chua `serverId` |
| TC-SF-14 | SF-06 | Connection error UI | Hien dung Unauthorized/Forbidden/Timeout/TLS/Reauth |
| TC-SF-15 | SF-07 | Secret scan | No secret trong Room/DataStore/log/route/fixture/backup |

## Bổ sung review revision 1.1

Tất cả case vẫn NOT_RUN. Unit/fake không thay bằng chứng Android Keystore/TLS/backup thật.

| ID | Work package / lớp test | Scenario | Expected result |
| --- | --- | --- | --- |
| TC-SF-16 | SF-01 / unit | URL chứa userinfo/query/fragment, host/port không hợp lệ | Reject trước network; không persist URL chứa credential |
| TC-SF-17 | SF-01,04 / network | Gateway path prefix và port mặc định | Chuẩn hóa identity nhất quán, endpoint không làm mất prefix |
| TC-SF-18 | SF-03,04 / integration | Đổi host/port/path prefix/authMode cùng serverId | Không gửi credential cũ; yêu cầu nhập lại; sửa tên đơn thuần vẫn giữ ref |
| TC-SF-19 | SF-04 / network | Redirect cùng/khác origin và HTTPS xuống HTTP | Không follow tự động, đích redirect không nhận credential |
| TC-SF-20 | SF-03 / fault injection | Crash sau ghi entry mới, trước/sau commit ref và trước cleanup | Old/new ref hợp lệ; orphan cleanup không xóa entry active |
| TC-SF-21 | SF-03 / fault injection | Crash trong delete, lỗi cleanup tạm thời | Tombstone chặn request, restart cleanup idempotent, không hồi sinh profile |
| TC-SF-22 | SF-03,04 / race | Health bị delay rồi đổi token/URL/authMode hoặc xóa profile | Callback cũ bị loại theo revision/generation; không ghi đè state mới |
| TC-SF-23 | SF-04 / unit | Hủy coroutine khi rời màn hình/đổi profile | Propagate cancellation, không map Unreachable hoặc retry |
| TC-SF-24 | SF-04 / network | Health false, HTML 200, malformed JSON, thiếu/version khác | Unhealthy hoặc Incompatible đúng; version chưa kiểm chứng có warning, không giả định API session tương thích |
| TC-SF-25 | SF-03,04 / restart | Mở app với lastHealthCheck Connected cũ | Hiển thị timestamp lịch sử; trạng thái hiện tại NotChecked/reauth |
| TC-SF-26 | SF-06 / Compose instrumented | Edit tên, password trống, đổi authMode, Test rồi Cancel | Giữ secret cũ khi chỉ sửa metadata; mode mới cần secret mới; Test không tự persist |
| TC-SF-27 | SF-06 / Compose instrumented | Rotation/recreation/Save/Cancel/rời form | Draft không restore, không có trong route/SavedStateHandle; bỏ tham chiếu draft |
| TC-SF-28 | SF-02 / Android thật | Mã hóa nhiều lần cùng credential, sửa ciphertext/AAD, hoán đổi ref A/B | IV mới, round-trip đúng; tampering/binding sai bị từ chối, không trả plaintext |
| TC-SF-29 | SF-02 / Android thật | Key bị mất/invalidated; ciphertext cũ hỏng trong lúc có ref mới | Chỉ xử lý entry/server bị ảnh hưởng, không xóa key đang phục vụ ref mới |
| TC-SF-30 | SF-04 / engine thật | Cert hợp lệ, sai hostname, hết hạn/untrusted | TLS handshake đúng; không trust-all; MockEngine không được dùng để kết luận case này |
| TC-SF-31 | SF-05 / backup thực tế | Cloud/device transfer trên <=11 và >=12, restore thiết bị không có key cũ | Chỉ export allowlist, không vault/ref/runtime/content; reauth; thiếu transport ghi BLOCKED |
| TC-SF-32 | SF-05 / integration | Restore lặp, export lỗi/version lạ, serverId va chạm | Validate/idempotent; không ghi đè profile active hoặc gắn nhầm credential |
| TC-SF-33 | SF-07 / regression | Provider Ollama HTTP và backup GPT Mobile trước/sau | Không bị thay đổi ngoài phạm vi; ghi evidence hành vi, không chỉ build |

Mỗi kết quả ghi revision, loại evidence, API level/build variant nếu có, dữ liệu đầu vào, expected/actual và artifact. TC-SF-04/05 cần chạy cả instrumented thực tế; TC-SF-11/12 phải đối chiếu TC-SF-31/32 trước khi nghiệm thu. Case hạ tầng/code chưa có được đánh DEFERRED/NOT_RUN theo plan.md mục 12, không đổi thành PASS.

## Bổ sung impact review revision 1.2

### TC-SF-34 — Onboarding và navigation isolation

- Precondition: emulator/device sạch và các bộ dữ liệu provider-only, OpenCode-only, cả hai, restored-profile không có credential.
- Steps: khởi động từng bộ dữ liệu; đi từ intro/home vào OpenCode; thêm profile; Back, restart và restore; thử thiếu provider API key.
- Expected: người mới có entry OpenCode; OpenCode-only không bị ép setup provider; restored profile yêu cầu nhập lại credential; provider-only giữ luồng hiện tại; cả hai điều hướng qua lại không bị xóa nhầm back stack.
- Evidence: Compose instrumented assertions + ảnh/screen recording, API level/build SHA. Hiện NOT_RUN.

### TC-SF-35 — Contract và traceability review

- Precondition: ADR và requirements-map đã có; giữ bản plan tổng thể/backlog hiện hành để so sánh.
- Steps: map mỗi work item với acceptance/test; kiểm tra nguồn profile duy nhất, delete/revision contract, crypto binding, phạm vi HTTPS/backup; đối chiếu gate Phase 0 với bằng chứng spike.
- Expected: không dùng conditional GO thay PASS; thay schema tổng thể cần đồng bộ trước code liên quan; test result không được suy ra từ test design. Mọi sai lệch có blocker hoặc quyết định rõ ràng.
- Evidence: review.md và fingerprint tài liệu; đây là review tài liệu, không chứng minh runtime.
