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

## HR-D02: Release ownership và promotion

**Quyết định người dùng:** promotion mặc định qua GitHub prerelease. Repository owner phê duyệt release và là rollback/on-call owner.

Release record phải chỉ rõ tag, commit SHA, artifact hashes, CI evidence và hướng dẫn rollback về prerelease trước. Không tự động chuyển sang GitHub stable hay Google Play khi chưa có lệnh mới.

## HR-D03: Android runtime evidence

**Quyết định người dùng:** bỏ qua việc cấp thiết bị/emulator ADB trong đợt này.

Native UI, Room runtime, TLS Android engine, process-death và backup/restore phải được ghi `NOT_RUN`, không được suy ra PASS từ unit/CI. Device/API matrix và performance p50/p95 vẫn là input `BLOCKED` nếu Feature 06 cần production-ready. Retention và approval policy ngoài release-owner approval vẫn chưa được cung cấp.

Feature 06 chỉ production-ready sau khi các input còn lại được cung cấp và HR-06/HR-10 có evidence thực tế.
