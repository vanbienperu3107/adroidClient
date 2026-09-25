# Quyết định Feature 05 — Permission Always

## AI-D01: Wording cho `Always`

**Quyết định người dùng:** tiếp tục cho phép hiển thị action `Always` khi target server contract xác minh action này hợp lệ.

UI phải ghi rõ:

> Always allow — phạm vi và thời hạn do OpenCode server quyết định.

Không được ghi hoặc suy diễn vĩnh viễn, chỉ session hiện tại, chỉ command/path hiện tại, hoặc có thể thu hồi từ Android.

Nếu contract server mục tiêu không trả action `always`, UI không hiển thị nút giả. Nếu request không mô tả đủ scope/tool/pattern, action `Always` bị vô hiệu hóa và user chỉ có reject hoặc chờ desktop.

`Always` chỉ gửi remote enum/payload đã version-pin ở AI-01. Không tạo local cached permanent allow, không áp dụng quyền cũ cho request/tool/path mới và không auto-approve sau restart.

## AI-D02: Target API baseline

Probe target OpenCode `1.18.30` đã pin legacy reply enum `once|always|reject`, path/body/200-400-404 của permission/question, empty-list behavior, diff schema và event names. Xem [contract-probe-2026-09-25.md](contract-probe-2026-09-25.md).

## Gate còn lại

AI-01 là **PARTIAL**, không phải PASS actionable UI: thiếu pending fixture thật cho resolution/expiry/desktop race/pagination, tool payload và byte/line/hunk budget. Quyết định wording không thay thế evidence server; `Always` chỉ bật khi fixture xác minh request có scope/tool/pattern đủ để người dùng hiểu remote action.
