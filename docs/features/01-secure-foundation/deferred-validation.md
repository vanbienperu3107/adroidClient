# Test runtime tạm bypass — cần chạy lại

## Quyết định

Người dùng cho phép tạm bypass các kiểm chứng runtime Android còn thiếu trong báo cáo recovery-validation.md để tiếp tục bước tiếp theo. Đây là miễn chặn tiến độ tạm thời, không phải test PASS và không cho phép bỏ qua lỗi code đã biết. Không tự cấp quyền merge hoặc release từ quyết định này.

Owner thực hiện: agent triển khai Feature 01; owner cung cấp môi trường: maintainer dự án. Tất cả task bên dưới OPEN / NOT_RUN. Phải xem lại trước lần phát hành tiếp theo có claim production-ready cho các capability tương ứng.

| ID | Test cần chạy sau | Điều kiện mở lại | Các bước và expected | Evidence cần có |
| --- | --- | --- | --- | --- |
| SF-FOLLOWUP-01 | Native UI/onboarding Basic-only | Device/emulator có APK mới | Add/edit/Test/Cancel/delete; cold start provider-only/OpenCode-only/cả hai; rotation; stale callbacks. Không mất draft ngoài policy, không persist Test, back stack đúng | API level, SHA, instrumented report, screenshot/video |
| SF-FOLLOWUP-02 | Keystore thật và process death | Device/emulator có APK test | Chạy OpenCodeVaultInstrumentedTest; kill app ở staging/reference switch/delete rồi mở lại. AAD/key isolation đúng, không mất credential active hoặc hồi sinh profile đã xóa | connectedDebugAndroidTest report, fault-injection evidence |
| SF-FOLLOWUP-03 | Room migration 1→2 | Database v1 có lịch sử chat và APK mới | Upgrade, đọc lại chat/message, kiểm tra index chat_id và foreign key; dữ liệu được giữ nguyên | Schema/index query, record assertions, APK SHA |
| SF-FOLLOWUP-04 | TLS engine Android | Device/emulator và HTTPS test endpoints | Cert hợp lệ/sai hostname/untrusted; HTTP release/redirect downgrade bị từ chối, không gửi credential sai origin | Network assertions và sanitized logs |
| SF-FOLLOWUP-05 | Backup/restore và device transfer | Android <=11 và >=12 với transport kiểm chứng được | Backup payload chỉ chứa export OpenCode allowlist; restore không key cũ -> reauth; GPT Mobile backup cũ được giữ; không có vault/ref/runtime/content | Payload đã khử secret, restore report, build/API/transport |

## Điều kiện đóng task

- Chạy đúng lớp evidence, không dùng unit/fake hoặc compile APK thay runtime.
- Ghi PASS/FAIL/BLOCKED từng task, kèm revision. Nếu FAIL: sửa, chạy lại và review các mức bị ảnh hưởng.
- Thiếu môi trường vẫn OPEN/BLOCKED; không xóa testcase hoặc tự đóng task do đã bypass.
- Yêu cầu browser của workspace không được đáp ứng bằng HTML mock của UI native. Nếu chưa có cách điều khiển APK qua browser, giữ giới hạn này trong báo cáo verification.

## Ngoài phạm vi bypass

Bearer gateway vẫn deferred capability riêng. Các lỗi source, lint, compile, unit test, credential binding và recovery logic có thể kiểm chứng hiện tại vẫn phải được sửa. Không đổi acceptance để che failure.
