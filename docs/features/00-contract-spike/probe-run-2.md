# Probe run 2 — bổ sung bằng chứng local

Baseline ứng dụng: `4894e54`. Worktree: `feature/01-secure-foundation`. Server thực tế `/global/health`: OpenCode **1.18.30**, HTTP loopback với Basic Auth, XDG config/data/cache riêng. Không dùng project/session hiện hữu; payload tổng hợp, `noReply=true` để không gọi LLM hoặc tool. Không phải evidence HTTPS/Tailscale/server công ty.

## Các probe đã chạy

| Probe | Quan sát thực tế | Kết luận giới hạn |
| --- | --- | --- |
| GET /doc | OpenAPI 3.1.0; prompt_async có messageID, noReply và parts | Schema thực tế khác nhận định cũ chưa hỗ trợ client ID |
| POST prompt_async với messageID=msg_probe_r2_client_a, noReply=true | 204; history user info.id đúng ID gửi | PASS local cho lưu/đối chiếu client ID, chưa chứng minh agent execution |
| Prompt thứ hai cùng text nhưng messageID=msg_probe_r2_client_b | 204; history có hai ID riêng | Có thể phân biệt theo ID, không dựa vào text/timestamp; chưa test desktop concurrency |
| Gửi lại ID a với text part mới không chỉ định part ID | 204; message a có 2 parts, b có 1 | Message ID không bảo đảm idempotency của payload; không tự retry |
| GET /event với password sai | 401 | PASS local cho nhánh SSE unauthorized |
| GET /session scope B | Danh sách rỗng | Scope list hoạt động trong hai directory test; cả repo chưa có root commit nên không chứng minh identity hai Git project độc lập |
| GET session A trực tiếp dưới directory B | 200 | Directory không là access-control boundary cho lookup bằng ID |
| DELETE message b rồi GET history | 200; history chỉ còn a | Có bằng chứng message deletion trong snapshot; chưa chạy Android reconcile |
| POST permission reply với request ID không tồn tại | 404 | Error path thật; không chứng minh pending/reply end-to-end |
| POST question reply/reject với request ID không tồn tại | Cả hai 404 | Error paths thật; chưa chứng minh question live |

## Contract lấy từ OpenAPI đang chạy

- `POST /session/{sessionID}/prompt_async`: optional `messageID` (pattern `^msg`), `noReply`; required `parts`.
- `GET /permission`, `GET /question`: list APIs.
- `POST /permission/{requestID}/reply`: required `reply` thuộc once/always/reject, optional message.
- `POST /question/{requestID}/reply`: required `answers` là array theo thứ tự câu hỏi; mỗi answer là array nhãn được chọn.
- `POST /question/{requestID}/reject`: không body.
- Contract schema quan sát không thay thế kiểm thử request pending thật.

## Cách tái lập

1. Khởi động `opencode serve --pure --hostname 127.0.0.1 --port <PORT>` với XDG config/data/cache test riêng và Basic password test.
2. GET health và /doc bằng Basic Auth; kiểm tra JSON, không suy ra endpoint từ HTTP 200 của fallback HTML.
3. POST /session?directory=<A> với title tổng hợp.
4. POST prompt_async hai lần với messageID a/b khác nhau, cùng text, noReply=true; GET history đối chiếu ID.
5. POST lại ID a với part không có ID; GET history đếm parts để kiểm tra mutation khi retry.
6. GET /event với password sai; GET session list B và lookup ID A trong B.
7. DELETE message b, GET history; POST reply/reject cho permission/question ID không tồn tại.
8. DELETE session test, dừng đúng PID server đã tạo. Không đưa password hoặc raw Authorization vào evidence.

## Điều chỉnh kết luận run trước

- Không dùng pre-send snapshot + text/timestamp làm quy tắc CONFIRMED. Dùng client messageID có bằng chứng lưu trên server; timeout/noReply=false và cạnh tranh desktop vẫn cần probe riêng.
- Không coi endpoint pending trả [] là PASS toàn bộ phục hồi permission/question.
- Fixture SSE viết lại bằng tay ở run trước không chứng minh wire ordering hoặc đầy đủ schema. Không dùng fixture tests xanh thay live protocol tests.
- Conditional GO cũ không đáp ứng gate Phase 0 của plan 1.4. **Gate production/implementation vẫn BLOCKED**; không chuyển sang Feature 01 implementation chỉ vì các probe local trên PASS.

## Chưa chạy / thiếu đầu vào

- HTTPS/Tailscale từ device/emulator: thiếu URL test/certificate route và version server công ty để so khớp; tailscale CLI không có trong container.
- Bearer: chưa có gateway/base URL/token test. Không dựng mock rồi ghi PASS gateway thật.
- Permission/question end-to-end, pending khi offline: cần môi trường agent/provider có thể tạo request thật trong session disposable.
- Timeout sau khi server nhận request, SSE delta/duplicate/out-of-order/snapshot overlap: cần harness fault injection và capture có provenance; chưa chạy.
- Process death Android và reducer convergence: chưa có implementation Android tương ứng, NOT_RUN.

Không suy ra tổng số PASS testcase từ các probe con. Testcase nhiều bước chỉ PASS khi đủ tất cả bằng chứng được yêu cầu.
