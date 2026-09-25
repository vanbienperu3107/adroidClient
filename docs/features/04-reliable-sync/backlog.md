# Feature 04 — Reliable Sync: Backlog

Trạng thái tổng: **PLANNED**; nhiệm vụ hiện tại chỉ tạo docs.

| ID | Input | Output | Scope | Deps | Acceptance | Status |
|---|---|---|---|---|---|---|
| RS-001 | REST hiện hữu, probe target 1.18.30, product lifecycle | Contract/ADR endpoint, envelope, auth, no-resume rule, delete/retry | data/opencode docs | Backend/product | Legacy `/event` Basic + directory + JSON envelope version-pinned; reconnect full REST reconcile; unknown/401/403 and remaining failure fixtures recorded | **PARTIAL:** 403/disconnect/concurrent fixtures pending |
| RS-002 | RS-001, OpenCodeReadApi, profile/url | SSE transport/parser fakeable | data/opencode/OpenCodeSse*, DI | RS-001 | AC-01/02/09: scoped request, frame cap, cancellation closes resource | PLANNED |
| RS-003 | RS-001/002, lifecycle/connectivity | Owner map, generation, bounded dirty/coalesce, full-jitter retry | data/opencode/OpenCodeReliableSync*, DI | RS-002 | AC-01/02/03/04/07: atomic ownership, stale no-op, overflow snapshot | PLANNED |
| RS-004 | RS-003, decoder/DAO | Reconcile transaction, snapshot upsert/prune, point-404 confirmation | CacheDatabase/repository | RS-003 | AC-05/06/09: no duplicate or cross-scope delete | PLANNED |
| RS-005 | RS-003/004, browse VM/navigation | activate/deactivate, refresh/mutate, foreground/background integration | BrowseRepository/ViewModel/DI/UI | RS-004 | AC-01/02/03/07/08: no double stream, convergence | PLANNED |
| RS-006 | RS-001…005, fake server/emulator | Unit/integration/instrumented/E2E evidence | test/androidTest | RS-005 | Each AC mapped; command/SHA/result captured; blocked never PASS | PLANNED |
| RS-007 | Diff/evidence RS-001…006 | Post-implementation review and rollback runbook | review evidence | RS-006 | Normal then High then XHigh PASS on actual SHA | PLANNED |

RS-001 is a hard gate; RS-004 precedes UI wiring; RS-007 cannot be replaced by this design review.
