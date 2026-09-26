# Recovery và kiểm chứng bổ sung

## Cập nhật quyền tiếp tục

Người dùng chấp thuận bypass tạm các test runtime còn thiếu. Danh sách owner, điều kiện và evidence để test lại được lưu tại [deferred-validation.md](deferred-validation.md), SF-FOLLOWUP-01..05. Kết quả runtime vẫn NOT_RUN/BLOCKED; được tiếp tục review source/lint/unit/build trong phạm vi hiện có, không tuyên bố runtime đã PASS.

Các thay đổi đang ở worktree Feature 01, chưa commit/push trong lượt này.

## Code đã sửa

- Cleanup journal ghi trước vault write; chuyển reference cùng transaction với enqueue cleanup cũ.
- Tombstone delete lưu cùng transaction xóa profile; startup/read retry cleanup idempotent.
- Export pending được lưu bền và retry sau restart; lỗi export không bị coi đã hoàn thành.
- Vault delete kiểm tra lỗi file, dọn staging; load không tự tạo key khi key bị mất.
- recordHealth có expectedRevision: response profile cũ không ghi đè metadata mới.
- Draft test khác credential/endpoint không persist kết quả vào profile đang có.
- Health validate URL, credentialRef restored rỗng và kiểu JSON của healthy/version.
- Startup hỗ trợ OpenCode-only; replay startup event, collector Compose chạy một lần và giữ navigation restored.
- UI thêm confirm delete, scroll và refresh danh sách khi trở lại.

## Kiểm chứng đã chạy

JDK 17, SDK 34, Gradle max-workers=1, compiler in-process.

- testDebugUnitTest: PASS sau các sửa đổi trên (heap 512 MB).
- assembleDebug: PASS local, đã vượt mergeExtDexDebug.
- assembleDebugAndroidTest: PASS, có test Keystore thật trong APK test.
- connectedDebugAndroidTest: NOT_RUN. adb không thấy thiết bị; không có emulator/system image hay /dev/kvm.
- Android UI screenshot và browser verification: BLOCKED; không có runtime APK được phục vụ/điều khiển qua browser. Không tạo mock HTML để thay evidence native.

Instrumented test mới kiểm tra key A/B isolation, sai endpoint AAD, ciphertext không plaintext và key bị xóa. Compile APK test không có nghĩa test này đã chạy.

## Còn phải làm trước gate hoàn tất

- Native UI và Keystore execution trên emulator/device hoặc CI được cấp môi trường.
- Migration Room 1→2 với dữ liệu thật, TLS engine thật và backup/restore payload thực tế.
- Review coverage/diff cuối và xử lý các findings còn lại; không dựa vào approval cũ.
- Không merge PR #6 dựa riêng trên những build/test unit này.
