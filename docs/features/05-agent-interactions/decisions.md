# Quyết định Feature 05 — Permission Always

## AI-D01: Wording cho `Always`

**Quyết định người dùng:** tiếp tục cho phép hiển thị action `Always` khi target server contract xác minh action này hợp lệ.

UI phải ghi rõ:

> Always allow — phạm vi và thời hạn do OpenCode server quyết định.

Không được ghi hoặc suy diễn vĩnh viễn, chỉ session hiện tại, chỉ command/path hiện tại, hoặc có thể thu hồi từ Android.

Nếu contract server mục tiêu không trả action `always`, UI không hiển thị nút giả. Nếu request không mô tả đủ scope/tool/pattern, action `Always` bị vô hiệu hóa và user chỉ có reject hoặc chờ desktop.

`Always` chỉ gửi remote enum/payload đã version-pin ở AI-01. Không tạo local cached permanent allow, không áp dụng quyền cũ cho request/tool/path mới và không auto-approve sau restart.

## Gate còn lại

AI-01 vẫn BLOCKED đến khi target server xác minh endpoint/payload/error cho permission/question, status resolved, event desktop-resolved và payload tool/diff. Quyết định wording không thay thế evidence server.
