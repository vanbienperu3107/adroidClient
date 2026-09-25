# Feature 04 — Reliable Sync: Review thiết kế

## Context
Review object là năm design docs, không thay app source. Revision: `feature/04-reliable-sync` @ `08b9bc21d392f045114a180611e7760d33bcbe20`. Codebase MCP indexed đúng worktree; caveat coverage ở impact.md.

## Normal — PASS
Review intent, scope, AC và decomposition.
- **N-01 resolved:** nguy cơ hiểu SSE event là cache payload. Plan xác định SSE dirty-only và REST authoritative; AC-03/05/06, TC-RS-03/06 cover.
- **Verdict PASS:** bao quát stream duy nhất, generation, REST/SSE, dirty cap, reconcile, duplicate/out-of-order/delta, delete, jitter, lifecycle, desktop concurrency. Work items đủ ID/input/output/scope/deps/acceptance/status.

## High — PASS
Review architecture/dependencies/callers/shared state/network/database/auth/test coverage.
- **H-01 resolved:** BrowseRepository có mutex, ViewModel có job/generation; singleton mới có thể double-own. Plan chỉ định coordinator sole owner + atomic map; TC-RS-01/02/03 cover.
- **H-02 resolved:** history nêu page absence không là deletion. Design giữ invariant; chỉ complete snapshot/current owned 404 prune; TC-RS-06 cover.
- **Verdict PASS:** modules/callers, DI/cache/lifecycle/auth risk và AC→testcase documented; runtime cases NOT_RUN, không giả PASS.

## XHigh — PASS (design only)
Review failure mode, race/process death, loss, credential boundary, evidence reliability.
- **X-01 resolved:** SSE contract/resume/threshold không có evidence. RS-001 hard blocker; không đoán endpoint/Last-Event-ID; runtime test NOT_RUN.
- **X-02 resolved:** không có desktop module. Desktop là external concurrent client; TC-RS-09 E2E bắt buộc.
- **Verdict PASS cho design docs**, không phải quyền implement. Chỉ implement sau RS-001 và post-diff Normal→High→XHigh PASS trên đúng SHA.

## Deferred/blockers
1. Backend endpoint/auth/envelope/version, resume semantics, event completeness.
2. Product/backend dirty cap/debounce/backoff/lifecycle/fallback thresholds.
3. Controlled server, desktop client, Android lifecycle environment cho TC-RS-08/09/12.

## Handoff
- **Intent:** reliable convergent browse sync; REST authoritative, SSE scoped trigger.
- **Findings:** generation OpenCodeSessionSync.kt:18–45; browse/history/mutate OpenCodeBrowseRepository.kt:84–184; UI OpenCodeBrowseViewModel.kt:82–108; cache OpenCodeCacheDatabase.kt:103–123.
- **Decisions:** one owner per serverId/revision/directory; stale no-op; bounded coalesce; no delete event/page absence; jitter; background closes stream.
- **Revision:** 08b9bc21d392f045114a180611e7760d33bcbe20.
- **Verification:** Normal/High/XHigh design PASS; executable tests NOT_RUN because source changes prohibited.
- **Next:** obtain RS-001, separately authorize implementation, run matrix and post-diff gates.
