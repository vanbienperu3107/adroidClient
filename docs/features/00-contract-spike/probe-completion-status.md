# Trạng thái hoàn tất probe — không thay thế gate Phase 0

Worktree feature/01-secure-foundation, baseline 4894e54. Server mục tiêu HTTPS/Basic, model cliproxy/gpt-6-astra. Kết quả dưới đây từ request thực; chưa có implementation Android được kiểm thử.

## Bổ sung đã chạy

### Question reply có SSE

Session mới deny-all, question=allow. Prompt 204, nhận pending question thật, reply 200. Capture đúng session có question.asked và question.replied. Abort và DELETE đều 200. Bổ sung evidence còn thiếu từ probe-astra-live.md.

### Phạm vi quyền always

- Lần thử đầu chờ pending trong session mới không thấy trong khoảng 60 giây; đã abort/delete. Kết quả INCONCLUSIVE, không phải PASS.
- Lần chẩn đoán tiếp theo, session mới có rules deny-all/bash=ask, model chỉ gọi pwd. Tool bash completed, command đúng pwd; các poll không quan sát pending.
- Trước đó probe đã cấp always cho pwd. Quan sát phù hợp khả năng remembered permission vượt session; chưa đủ chứng minh chính xác phạm vi lưu, độ bền qua restart hoặc loại trừ mọi race polling.
- Không tuyên bố always session-local hoặc cleanup session thu hồi quyền. Chưa sửa/xóa remembered permission vì chưa xác định chính xác record thuộc probe; ghi residual side effect pwd approval có thể còn hiệu lực. Không restart server công việc để thử persistence.
- Session chẩn đoán đã abort/delete 200.

### Hai directory riêng biệt trên server thật

- Bootstrap tạo hai thư mục scope-a/scope-b có suffix duy nhất bên trong contract-probe; chỉ mkdir/ls, không đọc source.
- Tạo mỗi directory một session; cả hai POST 200, directory response khớp.
- GET session list từng directory: có session của mình, không có session directory kia.
- GET trực tiếp ID session A với query directory B: 200. Scope list không phải ranh giới authorization.
- Cả hai session và bootstrap session đã DELETE 200. Thư mục test trống được giữ lại, không xóa dữ liệu khác.
- Hai directory nằm trong cùng workspace Git, không coi đây là evidence hai Git project độc lập hoặc worktree có root commit riêng.

## Ma trận evidence còn thiếu

| Phạm vi | Trạng thái hiện tại | Cần để hoàn tất |
| --- | --- | --- |
| HTTPS/Basic REST và SSE từ container | Có evidence live | Không suy ra Android trust store |
| Question reply/reject và permission once/always/reject | Có evidence live và event cho các nhánh đã ghi | Chốt always scope/durability và cleanup approval |
| Client messageID và response loss | Có evidence noReply=true | Timeout exception/agent execution không được suy ra |
| REST snapshot overlap | Có evidence snapshot cũ và refetch | Android reducer/delta dedup chưa có implementation |
| Multi-directory | Có evidence live | Multi-Git-project chưa kiểm chứng |
| Malformed/duplicate/out-of-order | NOT_RUN | Harness replay và parser/reducer cụ thể; synthetic frames không chứng minh server đã phát chúng |
| Android TLS/UI/process death/backup | BLOCKED | APK implementation, SDK/emulator/device và backup transport; adb/emulator không có trong PATH hiện tại |
| Bearer gateway | BLOCKED | URL/credential gateway test chưa cung cấp; không tự coi Basic là Bearer |

## Review tuần tự

1. **Normal — PASS cho báo cáo:** tách rõ request thật, inference, NOT_RUN và cleanup; không ghi mọi testcase PASS từ probe con.
2. **High — BLOCKED cho gate hoàn tất:** thiếu evidence ở ma trận trên; remembered permission có thể còn tồn tại. Không đáp ứng toàn bộ acceptance Phase 0.
3. **XHigh — NOT_RUN:** không vượt High chưa PASS theo quy tắc toàn cục.

Review báo cáo không đồng nghĩa review implementation. Các case Android thuộc các phase sau không thể chạy bằng cách tạo thêm probe REST. Cần điều chỉnh phân bổ testcase/gate được người dùng chấp thuận hoặc cung cấp implementation/môi trường tương ứng; không tự hạ gate.
