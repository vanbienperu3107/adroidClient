# Backlog Feature 06

Nguon: [plan.md](plan.md). Moi item hien `NOT_STARTED`; status chi duoc doi khi input da xac minh va acceptance co evidence dung lop. Pham vi du kien la file/module se duoc sua trong implementation, khong phai khang dinh file da thay doi trong tai lieu nay.

| ID | Input -> output | Scope du kien | Phu thuoc | Acceptance / testcase | Trang thai |
| --- | --- | --- | --- | --- | --- |
| HR-01 | Inventory secret/log/transport hien huu -> data-flow inventory va redaction policy allowlist/denylist | `data/network`, `data/opencode`, logging/crash config, `AndroidManifest.xml`, `res/xml` | Feature 01-05 SHA | T06-01..06-05; liet ke source/sink, ownership va khong co exception "debug" trong release | NOT_STARTED |
| HR-02 | HR-01 policy -> release network security config, transport guards va redaction implementation/tests | manifest, `res/xml/network_security_config.xml`, Ktor/OpenCode client, unit tests | HR-01 | T06-01..06-05 PASS; release reject HTTP/TLS bypass/unsafe redirect; logs/reports redacted | NOT_STARTED |
| HR-03 | Storage inventory + platform backup contract -> backup allowlist/excludes va restore state machine | manifest, `res/xml/backup_rules.xml`, `data_extraction_rules.xml`, profile/cache/vault repository, tests | HR-01, Feature 01-05 persisted formats | T06-06..06-09 PASS; ca cloud/D2D va sidecar/staging duoc kiem tra tren device | NOT_STARTED |
| HR-04 | Persisted-schema inventory -> migration matrix, exports, upgrade/downgrade/recovery implementation | Chat Room, OpenCode Room, DataStore/export, `app/schemas`, migrations, tests | Feature 01-05 SHA, HR-03 | T06-10..06-14 PASS; khong destructive reset profile/chat; cache rebuild co scope | NOT_STARTED |
| HR-05 | Test topology + package matrix -> instrumented suite, hermetic fixture/server harness va artifact install checks | `app/src/androidTest`, test fixtures, Gradle tasks, CI test reports | HR-02..04, device/emulator image | T06-15..06-20 PASS; test real keystore/Room, install debug/release va clean up fixture | NOT_STARTED |
| HR-06 | Target device/dataset/release-owner input -> performance/a11y/tablet benchmark specification va budgets duyet | benchmark/profile configs, Compose UI/OpenCode renderers, test docs/reports | Feature 05 UI complete, device/API/budget approval | T06-21..06-25 co precondition ro; budget chua duyet thi BLOCKED, khong tu chon threshold | NOT_STARTED |
| HR-07 | HR-06 spec -> bounded rendering, semantics/focus/tablet fixes va measured reports | OpenCode screens/renderers, navigation, accessibility/performance tests | HR-05, HR-06 | T06-21..06-25 PASS tren device target; khong regress provider chat/navigation | NOT_STARTED |
| HR-08 | CI/release baseline -> hardened required-check pipeline va release manifest | `.github/workflows`, Gradle version config, SBOM/provenance scripts/config, docs release runbook | HR-02..05, CI secret/protected environment | T06-26..06-31 PASS; immutable SHA, SHA-256, cert allowlist, signed-only public artifact | NOT_STARTED |
| HR-09 | Signed RC + all gate evidence -> E2E/staged-release readiness report va go/no-go | E2E harness, CI artifacts, release checklist/evidence | HR-02..08, disposable server/device/desktop | T06-32..06-37 PASS; no fake/unit substitute for E2E/device evidence | NOT_STARTED |
| HR-10 | Approved signed artifact + monitoring/compatibility input -> rollback rehearsal and handoff record | release runbook, monitoring/dashboard config, distribution channel record | HR-08, HR-09, release owner | T06-38..06-40 PASS; rollback artifact verified, no schema/credential destructive action | NOT_STARTED |

## Dependency and status rules

- Sequence: HR-01 -> HR-02 -> HR-03/HR-04 -> HR-05 -> HR-06 -> HR-07 -> HR-08 -> HR-09 -> HR-10. HR-03 va HR-04 co the duoc discovery song song sau HR-01, nhung khong duoc complete neu recovery/backup intersection chua review.
- `BLOCKED` phai ghi exact missing input/evidence va owner. `NOT_RUN` nghia la test chua chay. `DEFERRED` nghia la ra khoi release scope. Khong status nao trong ba status nay duoc dung thay `PASS`.
- HR-08 khong duoc tao tag, push, release hay truy cap signing secret trong pham vi work item planning. Chi khi co giao viec explicit moi duoc thuc hien handoff GitHub.
