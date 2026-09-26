# Phân rã và coverage Feature 01 — revision 1.2

Nguồn: [plan](plan.md), [testcases](testcases.md). Các work item dưới đây là đơn vị triển khai/kiểm chứng; tất cả NOT_STARTED. Acceptance là expected của testcase được map, chưa phải test result.

| ID | Input → output / vùng triển khai | Phụ thuộc | Acceptance / testcase |
| --- | --- | --- | --- |
| SF-01a | Quyết định review → ADR storage/crypto/backup, domain models | Gate Phase 0 | TC-SF-35; các quyết định không mâu thuẫn plan tổng thể |
| SF-01b | URL + build policy → canonical binding, domain URL policy | 01a | TC-SF-01..03,16..18 |
| SF-02a | Server identity → key generation + ciphertext envelope, data/security | 01a | TC-SF-04,28,29 |
| SF-02b | Vault exception → typed error, data/security | 02a | TC-SF-05,06,29 |
| SF-03a | Metadata → DataStore profile repository, data/profile | 01a | TC-SF-07,25,32 |
| SF-03b | Credential update → staged ref switch + recovery journal | 02a,03a | TC-SF-20 |
| SF-03c | Delete/restart → tombstone + orphan cleanup + events | 03b | TC-SF-08,21,29 |
| SF-04a | Binding/credential → isolated authenticated client | 01b,02b | TC-SF-17..19,30 |
| SF-04b | Health response → typed result + timestamp | 04a | TC-SF-09,10,24,25 |
| SF-04c | Profile/request revision → cancellation/stale-result guard | 03b,04b | TC-SF-22,23 |
| SF-05a | Profile metadata → versioned backup export, data/backup | 03a,03c | TC-SF-11,31,32 |
| SF-05b | Export + runtime files → cloud/D2D rules, res/xml | 05a | TC-SF-11,31,33 |
| SF-05c | Restored export → validated profile without credential | 05a,03a | TC-SF-12,32 |
| SF-06a | Startup setup state → provider/OpenCode entry routes | 03a | TC-SF-34 |
| SF-06b | Form draft → Save/Test/Cancel/edit actions, Compose server UI | 03b,04c,06a | TC-SF-13,14,26,27 |
| SF-06c | Delete/reauth events → stable navigation and UI | 03c,06b | TC-SF-08,14,22,34 |
| SF-07a | Unit/instrumented/TLS/backup evidence → requirement report | các mục trên | TC-SF-15,28..35; không thay evidence lớp này bằng lớp khác |

Không coi thao tác code từng dòng là work item độc lập. Việc thêm client không yêu cầu sửa client provider cũ; thiếu thông tin contract phải báo blocker thay vì tự đổi acceptance.

## Impact cần kiểm chứng

| Tính năng khác | Tác động | Regression |
| --- | --- | --- |
| Onboarding provider | Startup redirect có thể chặn người chỉ dùng OpenCode | TC-SF-34, provider-only giữ luồng cũ |
| Ollama HTTP / API providers | Manifest/client dùng chung có thể bị siết hoặc gắn nhầm auth | TC-SF-33, qualifier/client riêng |
| Backup chat/settings cũ | XML ở cấp ứng dụng có thể loại dữ liệu cũ | TC-SF-31,33, so payload trước/sau |
| Feature 02 cache | Nguồn server profile DataStore khác schema Room cũ | TC-SF-35; đồng bộ schema, delete/revision events và startup orphan reconcile |
| Feature 03–05 prompt/SSE | Đổi credential/endpoint phải hủy request/stream cũ | TC-SF-22, contract ProfileChanged/ProfileDeleting |
| CI/release | Unit xanh chưa chứng minh native/TLS/backup | Evidence matrix và gate SF-07 |
