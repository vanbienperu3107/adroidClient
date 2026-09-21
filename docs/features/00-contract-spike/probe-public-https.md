# Probe HTTPS server mục tiêu

## Phạm vi lượt đầu

Probe chỉ đọc từ container tới server mục tiêu qua public Internet, dùng Basic Auth do người dùng cung cấp. Không tạo session, không gửi prompt, không đổi cấu hình và không đọc nội dung hội thoại. URL thực và credential không lưu trong tài liệu này.

## Kết quả quan sát

| Request | Kết quả |
| --- | --- |
| GET /global/health, không credential (probe trước) | HTTP 401; curl ssl_verify_result=0; không redirect |
| GET /global/health, Basic Auth đúng | HTTP 200, application/json, healthy=true, version=1.18.30 |
| GET /event, Basic Auth đúng | HTTP 200, text/event-stream; event đầu tiên server.connected |

Probe authenticated dùng Python urllib với ssl.create_default_context(), timeout 20 giây và chặn tự follow redirect. Chỉ xuất status/content-type/healthy/version/event type; không xuất Authorization, body lỗi hoặc event payload. Đóng response SSE sau event đầu tiên.

## Giới hạn evidence

- PASS cho kết nối HTTPS với trust store mặc định của container và Basic Auth REST/SSE handshake tới server mục tiêu.
- Đây không phải bằng chứng Android/emulator TLS hoặc stream hoạt động lâu dài/reconnect.
- Chưa thử sai credential trên server mục tiêu; 401 chưa đăng nhập và 401 local không thay cho case sai credential tại đây.
- Chưa có directory/project disposable do người dùng chỉ định. Probe tạo/xóa session, gửi prompt, permission/question và concurrency vẫn BLOCKED cho tới khi chốt scope test.
- Bearer gateway không được xác minh bằng Basic Auth.
- Không dùng kết quả handshake để kết luận mọi event hoặc toàn bộ Contract Spike PASS; gate Phase 0 chưa hoàn thành.

## Lượt bổ sung sau khi người dùng yêu cầu tiếp tục

Đã tạo hai session disposable `contract-probe-*` trong context mặc định của server. Chỉ thao tác trên ID do request tạo session trả về. Prompt dùng text tổng hợp và `noReply=true`, không gọi model/tool. Cả hai session đã DELETE thành công; session CRUD đầu tiên được GET lại và trả 404. Không lưu nội dung session người dùng hoặc credential.

| Probe trực tiếp qua HTTPS | Quan sát |
| --- | --- |
| GET /doc | 200, prompt_async khai báo messageID/noReply/parts |
| Health với credential sai có chủ đích | 401 |
| GET /permission và /question | Cả hai 200, response dạng array; không xuất nội dung |
| Reply permission ID không tồn tại | 404 |
| Reply/reject question ID không tồn tại | Cả hai 404 |
| Session create / PATCH rename | 200 / 200 |
| Hai prompt cùng text, ID khác nhau, noReply=true | Cả hai 204; GET từng message 200 và ID khớp; history có cả hai |
| Gửi lại cùng messageID với part không chỉ định ID | 204; message có 2 parts — không coi là idempotent |
| DELETE message rồi GET history | 200, ID đã xóa không còn trong history |
| GET diff trên session disposable | 200; không chứng minh diff có nội dung |
| SSE capture lọc theo ID session vừa tạo | Quan sát session.created, session.updated, message.updated, message.part.updated |
| Đóng SSE, rename session, mở SSE lại | PATCH 200; stream mới có server.connected; REST xác nhận title thay đổi lúc offline |

### Giới hạn và mapping testcase

- TC-00F-02: client-supplied ID được lưu thật trên server mục tiêu; lặp ID vẫn có mutation part. Chỉ xác minh noReply=true, chưa chứng minh exactly-once thực thi agent.
- TC-00G-01: thêm bằng chứng session.created, không suy ra các event khác không tồn tại.
- TC-00G-05: đã kiểm tra reconnect và REST phục hồi rename offline; không chứng minh durable replay hoặc Android reducer.
- TC-00E: endpoint list/error hoạt động nhưng chưa có request permission/question live. Không đánh PASS E2E từ list rỗng hoặc schema.
- TLS/REST/SSE evidence thuộc Python client trên container, không phải Android device.
- Chưa chạy đa project thật, timeout-sau-nhận, malformed/duplicate/out-of-order injection, snapshot race, agent response/diff có nội dung và permission/question live.
- Gate Phase 0 vẫn BLOCKED cho các acceptance thiếu evidence. Không commit/push hoặc thay đổi cấu hình server trong lượt này.
