# Live agent probe — GPT-6 Astra

## Môi trường và giới hạn

- Server mục tiêu HTTPS, Basic Auth, OpenCode 1.18.30 đã xác minh ở lượt trước.
- Model gửi rõ trong payload: providerID=cliproxy, modelID=gpt-6-astra.
- Người dùng cho phép tạo contract-probe trong workspace server. Bootstrap shell chỉ kiểm tra thư mục cha rồi mkdir -p contract-probe; tool response xác nhận thành công. Shell bootstrap không gọi model (metadata assistant mặc định của shell không phải evidence model inference).
- Session agent dùng directory contract-probe, permission rules giới hạn theo từng probe, prompt tổng hợp. Không đọc source hay sửa file project hiện hữu. Chưa có Android client implementation.

## Question thật

1. Tạo session disposable với deny-all và chỉ allow question.
2. Gửi prompt_async chọn GPT-6 Astra, yêu cầu hỏi “Select probe response” với Alpha/Beta: HTTP 204.
3. Poll GET /question (không mở SSE): nhận request thật có id, sessionID, questions, tool.messageID và callID. Nội dung/choices đúng yêu cầu.
4. POST /question/{requestID}/reply với answers=[["Alpha"]]: HTTP 200.
5. GET pending list: request của session không còn.
6. Reply lại cùng request ID: HTTP 404.
7. Abort 200 và DELETE session 200 để dọn session probe.

Kết luận: PASS cho question tạo bởi model, phát hiện pending qua REST không cần đã nhận event và reply/resolved/stale-reply. Không chứng minh reject question, GUI desktop hoặc event question.replied vì chưa capture SSE ở lượt này.

## Permission thật

1. Tạo session disposable với deny-all và bash=ask.
2. GPT-6 Astra được yêu cầu dùng bash cho pwd, không đọc/sửa file: prompt_async HTTP 204.
3. GET /permission: có pending request permission=bash, patterns=["pwd"], metadata.command=pwd, tool linkage.
4. Gọi GET lại ở lượt request mới, vẫn không dùng SSE: tìm thấy đúng pending ID.
5. POST /permission/{requestID}/reply với reply=reject: HTTP 200. Không cấp quyền chạy lệnh.
6. Pending list không còn request; reply lại ID đã giải quyết trả 404.
7. Abort 200 và DELETE session 200.

Kết luận: PASS cho permission thật, REST phục hồi pending, reject/resolved/stale-reply. Chưa kiểm tra once/always và event permission.replied. Không coi request rỗng là bằng chứng duy nhất như run trước.

## Cleanup

Bootstrap session và hai agent session đều DELETE thành công. Hai agent session được abort trước delete. Thư mục contract-probe được giữ làm vị trí test đã được cho phép; không xóa thư mục hoặc dữ liệu ngoài các session probe. Không thay global permission/server config.

## Mapping và gate

- WI-00e có evidence live mới; TC-00E-01..05 chỉ hoàn thành các bước đã mô tả bên trên, chưa PASS toàn bộ suite.
- “Không có SSE” chứng minh phát hiện qua REST khi bỏ lỡ event; không phải mô phỏng mất mạng ở Android hoặc desktop GUI.
- Còn: question reject, permission once/always, event capture, timeout sau nhận request, delta/race/duplicate/out-of-order, đa project thật và Android transport.
- Gate Phase 0 vẫn chưa PASS đầy đủ; không tự đổi conditional GO thành PASS và chưa mở implementation Feature 01.
