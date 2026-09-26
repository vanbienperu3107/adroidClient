# Implementation progress — Feature 02

## Quyết định người dùng

- Delete xóa session trên server sau confirm rõ tác động mọi thiết bị.
- Offline có thể đọc cache; 401 yêu cầu xác thực lại trước khi hiển thị nội dung.
- Pending badge read-only, trả lời trên desktop tới Feature05. Không tải được phải unknown, không 0.

## Contract được kiểm tra trực tiếp

Đọc /doc qua HTTPS Basic Auth server mục tiêu, không đổi dữ liệu. Schema trả:

- /project, /project/current: directory/workspace optional; Project array/object.
- /session GET: directory, workspace, scope=project, path, roots, start, search, limit. Không suy start là offset vì schema chỉ ghi number.
- /session/{sessionID}/message GET: directory, workspace, limit integer, before string; response array {info,parts}.
- /session/status: map SessionStatus; /permission và /question: array pending.
- /session/{sessionID}: GET/PATCH/DELETE; scope query directory/workspace.

OpenAPI không cam kết consistent snapshot hay durable cursor. Không prune dựa một page/list. Docs Context7 hiện có cả API v1/v2/spec proposal; chỉ schema target được dùng cho đường dẫn ở trên. Chưa chốt boundary limit và semantics before từ live multi-page probe.

## Code increment

- OpenCodeScope/server revision/directory và OpenCodeSessionKey: giữ case/path remote, không dùng projectID làm scope duy nhất.
- OpenCodeHistoryDecoder: parse message/part DTO, validate ownership/duplicate IDs, unknown field/type fallback không giữ raw payload; page không đánh dấu authoritative full snapshot.
- Budget 2M chars là guard parser mặc định có thể inject, chưa phải HTTP byte limit hoặc ngưỡng sản phẩm đã đo. Transport phải enforce byte budget trước khi read body toàn bộ.
- 7 tests: text, foreign session, foreign parent, duplicate messages, unknown part, scope case isolation và oversized payload.

## Evidence và giới hạn

## Increment browsing tích hợp

- UI Projects → Sessions → History được nối từ server list; rename/delete có confirmation xóa trên server; pending badge chỉ đọc.
- Cache có project/session/message/part/access gate, Room schema export v1 (chưa phát hành); cache ở noBackupFilesDir.
- REST async cancel gọi Call.cancel; payload byte limit; mutation không retry; kiểm tra ownership trước PATCH/DELETE.
- Profile-revision guard dùng cùng lock với mutation profile trước cache read/write; purge cache obsolete khi mở repository. 401/403 giữ khóa cache qua restart ở cùng revision, không tự mở lại bằng request thành công khác.
- Tests: unit suite PASS; assembleDebug và assembleDebugAndroidTest PASS trước sửa cuối auth latch, unit suite PASS lại sau sửa. Room instrumented test mới đã compile, chưa chạy trên Android.

## Gate implementation — chưa hoàn tất

### Tiến độ bổ sung sau findings Normal

- Đã thêm profile revisions Flow: khi revision đổi/xóa, ViewModel hủy request và khóa nội dung đang hiển thị.
- Lưu lựa chọn directory/session trong SavedStateHandle, không lưu credential/nội dung message.
- Session load-more tăng giới hạn mỗi lần 100 (trần 10.000), không giả cursor từ tham số start chưa xác minh.
- History reconcile tối đa 20 cached misses mỗi refresh bằng point lookup; chỉ xóa khi 404 và ownership session còn xác minh được, không xóa chỉ vì vắng trong page.
- Markdown block subset an toàn: heading/list/quote/fenced code, không HTML/image loading; không claim hỗ trợ toàn bộ Markdown inline/table.
- Đã chạy lại testDebugUnitTest + assembleDebug + assembleDebugAndroidTest: PASS. Instrumented test mới chỉ compile.
- Môi trường xác minh native vẫn BLOCKED: không /dev/kvm, emulator/system-images, adb devices rỗng. Browser không thể thay evidence native APK.

Normal: REQUEST_CHANGES cho toàn Feature02. Những phần chưa đáp ứng đầy đủ gồm Markdown renderer (hiện text an toàn), session pagination/load-more vượt 100 records, restore selection, message deletion reconciliation qua authoritative lookup, lifecycle profile observer khi màn đã mở và đầy đủ repository/native testcase. High/XHigh implementation chưa được mở trước khi Normal PASS.

UI runtime/browser verification: BLOCKED — chưa có emulator/device hiển thị APK. Không dùng build APK hoặc HTML mô phỏng thay evidence native. Không chuyển bypass runtime Feature01 sang Feature02 khi chưa được giao rõ.

Không commit/push trong increment này. Không gọi feature đã hoàn tất hoặc sẵn sàng release chỉ vì build/unit xanh.

Increment tiếp theo đã có OpenCodeReadApi (GET-only, Basic vault, prefix/query encoding, redirect/retry off, byte budget, timeout/TLS mapping) và session cache Room skeleton v1 ở noBackupFilesDir với schema export. 3 transport tests xác minh prefix/directory, HTML/oversize rejection và 401 không thành empty success. Chưa nối transport vào UI hay coordinator; chưa claim cancellation/generation hoàn chỉnh. Cache mới chỉ có session table, không phải schema cuối đã phát hành.

`testDebugUnitTest` local PASS (JDK17/SDK34, heap512m, max-workers=1, Kotlin in-process). Đây là DTO/scope increment, không phải Room sync hoặc UI. Feature02 chưa hoàn tất; REST transport, profile coordinator, cache database, UI, native tests và final gates còn pending.
