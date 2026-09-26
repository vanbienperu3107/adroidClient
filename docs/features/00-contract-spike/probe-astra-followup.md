# Live probe tiếp theo — reject/allow, SSE và response loss

Baseline worktree: Feature 01 tại 4894e54; model cliproxy/gpt-6-astra, server mục tiêu HTTPS/Basic. Phạm vi là session disposable trong contract-probe đã được người dùng cho phép. Báo cáo không chứa credential hoặc ID/session người dùng.

## 1. Question reject thật + SSE

- Session deny-all, chỉ question=allow; prompt_async 204.
- Model tạo question; POST reject 200; pending request biến mất.
- Tool question kết thúc state=error (do reject); retry cùng request trả 404.
- SSE lọc riêng session test quan sát question.asked, question.rejected, message.updated, message.part.updated, session.created/updated/status/idle/diff.
- Abort 200 và DELETE session 200.

## 2. Permission once và always

Hai session test độc lập, deny-all và bash=ask. Trước khi cấp quyền kiểm tra metadata.command chính xác là pwd; không cấp quyền nếu khác.

| Reply | HTTP | Tool | Pending | Stale reply |
| --- | --- | --- | --- | --- |
| once | 200 | bash completed | Đã loại request | 404 |
| always | 200 | bash completed | Đã loại request | 404 |

Ở cả hai session SSE có permission.asked, permission.replied, message.part.delta, message.part.updated, message.updated, session.created/updated/status/idle/diff. Cả hai được abort 200 và DELETE 200.

“always” mới được xác minh accepted và tool hoàn thành; không suy ra phạm vi hoặc độ bền của remembered permission sau restart. Không sửa global permission config; chỉ trả lời request pwd ở session test. Không gọi đây là test idempotency.

## 3. Client bỏ response trước khi đọc

- Tạo session mới, gửi một HTTP POST prompt_async qua socket TLS có certificate verification.
- Body có client messageID duy nhất, noReply=true; gửi xong, đợi 0.5 giây rồi đóng socket mà không đọc byte response nào.
- Client không biết status của POST đó. REST GET message theo ID sau đó trả đúng user message.
- Không resend request; số automatic resend=0. DELETE session 200.

PASS cho thí nghiệm response-loss và phục hồi bằng ID. Không phải test timeout exception của Android/Ktor; không chứng minh noReply=false được thực thi đúng một lần. Không báo đã quan sát 204 của POST bị bỏ response.

## 4. Snapshot bị cũ khi có thay đổi xen kẽ

- Tạo message a bằng noReply=true; mở GET history và giữ response chưa đọc.
- Gửi message b trong lúc response cũ đang được giữ; sau đó đọc response đầu và gọi GET lại.
- Snapshot đầu: 1 message, thiếu b. Snapshot sau: 2 message, có b.
- DELETE session 200.

Có bằng chứng cần reconcile bổ sung sau snapshot cũ. Đây là thí nghiệm REST snapshot overlap; chưa test Android Room reducer, dirty-set hoặc delta dedup. Không đánh PASS toàn bộ TC-00G-06 từ kết quả này.

## 5. Phạm vi còn thiếu và gate

- Đã thêm nhánh question reject, permission once/always và event capture thật.
- Question reply từng PASS ở lượt trước nhưng chưa có capture question.replied tương ứng.
- Chưa test remembered permission scope/durability, multi-project thật, malformed/duplicate/out-of-order injection, Android TLS/UI/process death và reducer convergence.
- Không có gateway Bearer test; không coi Basic Auth chứng minh Bearer.
- Tất cả 5 session mới trong lượt này đã DELETE thành công. Không xóa thư mục contract-probe hoặc sửa dữ liệu project hiện hữu.
- Gate Phase 0 chưa PASS đầy đủ. Các kết quả trên là probe con với giới hạn được ghi rõ, không thay bằng chứng runtime Feature 01.
