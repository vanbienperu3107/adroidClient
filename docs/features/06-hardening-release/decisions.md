# Quyết định Feature 06 — Release Security Boundary

## HR-D01: Cleartext policy cho legacy Ollama

**Quyết định người dùng:** không tắt cleartext toàn app ở Feature 06.

Lý do: Ollama HTTP nội bộ là capability legacy hiện hữu; cấm cleartext global có thể gây regression ngoài scope OpenCode.

| Bề mặt | Chính sách |
| --- | --- |
| OpenCode | `OpenCodeUrlPolicy` bắt buộc HTTPS release; cấm HTTP, redirect khác origin/scheme và bypass certificate |
| Ollama legacy | Giữ cleartext hiện hữu cho đến khi có migration sản phẩm hoặc Network Security Config allowlist đã được review |
| Provider khác | Không thay đổi transport từ Feature 06 nếu không có requirement riêng |

Feature 06 phải test OpenCode HTTP bị reject trước credential request, đồng thời regression test Ollama HTTP legacy theo môi trường test được cấp. Không claim “toàn app secure transport only” trong release notes.

## HR-D02: Release owner input

Chưa có giá trị cụ thể do người dùng chưa chỉ định. Các input dưới đây là BLOCKED, không tự đoán:

- Kênh promotion (GitHub prerelease/stable, Play Internal/Closed hay cả hai).
- Release owner và rollback owner/on-call.
- Thiết bị/API benchmark matrix.
- Performance budgets p50/p95.
- Artifact/log/report retention và approval policy.

Feature 06 chỉ production-ready sau khi input được cung cấp và HR-06/HR-10 có evidence thực tế.
