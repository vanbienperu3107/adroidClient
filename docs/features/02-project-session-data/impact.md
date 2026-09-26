# Impact review Feature 02

Baseline đúng worktree: 72d4136, phụ thuộc Feature 01 chưa merge. Discovery graph tìm OpenCodeProfileRepository/OpenCodeHealthClient, snippet interface và trace inbound profiles cho thấy caller MainViewModel; đây là best-effort, không phải danh sách caller đầy đủ.

| Vùng/tính năng | Thay đổi / rủi ro | Cách giảm / regression |
| --- | --- | --- |
| Profile DataStore/vault Feature01 | Thêm observe/revision guard; đổi endpoint có cache cũ | PS-02/16, T02/T16; không lưu credential vào Room |
| Health client | Chưa phải general API; lỗi refactor có thể làm hỏng Test connection | T06/T18; transport chung phải giữ contract health và no-redirect |
| Onboarding/Settings/navigation | Thêm destination, restore target và back stack | T08/T10/T18; provider-only/OpenCode-only/cả hai |
| ChatDatabase hiện hữu v2 | Nguy cơ nhầm schema/builder/cache reset | Database tên riêng, qualifier riêng, không migration chat ở Feature02; T05/T18 |
| Provider chat và Ollama HTTP | Network policy/global DI bị siết ngoài ý muốn | Không đổi ApiType/provider client/manifest cleartext global; T06/T18 |
| Backup Feature01 | Room WAL/SHM có nội dung; metadata export vô tình chứa title | Cache noBackup + rules defense-in-depth; T17, không backup messages/parts/title |
| Desktop cùng session | Rename/delete/update giữa list và snapshot | T11..14; generation và authoritative reconcile, không text matching |
| Feature03 prompt | Future optimistic message bị full snapshot xóa | Giao contract phân biệt server-owned/local pending; không tạo pending feature ở đây |
| Feature04 SSE | Hai nơi ghi cache, delta chạy đè REST | Repository/sync serializes writes, future reducer dùng cùng writer; T14 |
| Feature05 interactions | Unknown parts/badge bị coi đã resolved | Fallback read-only, không auto approve/question reply |
| CI/release | Test fake xanh không chứng minh Room/TLS/native | T18 map lớp evidence, không đổi signed release workflow trong nhiệm vụ docs |

## Failure modes và rollback

- Profile xóa trong request: reject commit bằng revision/ownership; event bị lỡ do crash được sửa qua startup reconciliation.
- Empty list do 401/timeout không được ghi thành authoritative empty snapshot. Nếu DB write fail, rollback transaction, giữ lastSuccess cũ.
- Tránh nửa snapshot: validate toàn batch trước transaction; không đánh synced trước commit; record loading generation bị ngắt phải stale khi restart.
- Cache bị corrupted: báo lỗi cache; không xóa DataStore profile hoặc ChatDatabase. Kế hoạch reset cache cần giới hạn đúng DB và không phát sinh mutation server.
- Rollback code chỉ đóng route mới và quay phiên bản repository trước; schema cache version thay đổi phải migration hoặc reset cache có kiểm soát. Không tự rollback user data hoặc key.

## Giới hạn

Không có implementation Feature02 được chạy. Native regressions, Room snapshot stress, server API mutation và restore tests đều NOT_RUN. PR #6 chưa merge nên trước khi code tích hợp phải review lại baseline nếu Feature01 đổi; không coi parent CI xanh là evidence Feature02.
