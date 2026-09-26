# Feature 04 — Reliable Sync: Impact và evidence

## Discovery evidence
- Worktree đúng: `/workspace/Project/worktrees/04-reliable-sync`; branch `feature/04-reliable-sync`; HEAD `08b9bc21d392f045114a180611e7760d33bcbe20`.
- Codebase MCP moderate index: 1,735 nodes / 6,648 edges. Coverage best-effort không có recorded issue trên các Kotlin/test paths trích dẫn; docs excluded by design. `app/build.gradle.kts` và `gradlew` parse-partial, không dùng làm evidence.
- Graph search SSE trong main Kotlin trả 0. Fixture `sseFixtureFramesAreValidJsonWithTypeEnvelope` ở `OpenCodeContractFixtureTest.kt:92–108` không đủ chứng minh transport contract.

## Affected modules/callers
| Evidence | Hiện trạng | Impact |
|---|---|---|
| OpenCodeSessionSync.kt:18–45 refresh | REST session/status; mutex + generation theo serverId,directory; stale trả StaleScope | Scope phải thêm revision và bao phủ stream/retry/reconcile; graph không thấy callers, cần verify wiring |
| OpenCodeBrowseRepository.kt:84–115 sessions | mutex; REST sessions/status/permission/question, decode/upsert | Coordinator tránh refresh song song; snapshot authoritative |
| OpenCodeBrowseRepository.kt:129–166 history | page store; cached-miss tối đa 20 point GET; chỉ delete 404 + owned | Giữ non-deletion-by-absence; bounded dirty/current generation rule |
| OpenCodeBrowseRepository.kt:168–184 mutate | PATCH/DELETE; cache delete sau true | Mark dirty/reconcile để hội tụ desktop concurrent write |
| OpenCodeCacheDatabase.kt:103–108,119–123 | transaction upsert; delete message + session row | Thêm scoped reconcile/prune chống orphan/cross-scope loss |
| OpenCodeBrowseViewModel.kt:82–108 | cancel job, UI generation, routes browse | Phối hợp navigation/manual refresh với coordinator |
| OpenCodeBrowseModule.kt:26–27 | singleton repository profile/API/DB | Wire coordinator/transport singleton owner lifecycle |

Inbound graph của history: BrowseViewModel.refresh hop 1; BrowseScreen hop 2; openCodeNavigation hop 3; SetupNavGraph hop 4. Edges heuristic confidence thấp phải verify source khi implement.

## Risk / rollback
| Risk | Level | Mitigation | Rollback |
|---|---|---|---|
| stale callback write | Critical | generation check async/transaction boundary | disable SSE/coordinator; REST refresh |
| partial delta/page prunes history | Critical | only complete snapshot/current owned 404 | disable prune, retain cache |
| double stream/retry | High | atomic owner map/cancellation counters | close owners/reset map |
| retry herd/background socket | High | full jitter/cap/lifecycle cancellation | disable auto reconnect |
| auth leak/cross-server | Critical | profile policy/redacted logs/scope tests | disable SSE, existing reauth |
| malformed contract | High | versioned parser/frame cap/REST fallback | feature flag REST only |
| desktop divergence | Medium | server snapshot/stale indication | manual refresh |

Deferred: no backend SSE contract, live trace, lifecycle device trace, or desktop client evidence. Đây là **NOT PASS**.
