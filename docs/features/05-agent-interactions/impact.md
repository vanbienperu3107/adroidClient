# Impact Review Feature 05

Baseline: `08b9bc21d392f045114a180611e7760d33bcbe20`, worktree `feature/05-agent-interactions`. Codebase MCP trace identifies `OpenCodeBrowseScreen -> OpenCodeBrowseViewModel.refresh -> OpenCodeBrowseRepository.sessions`; the repository currently fetches pending lists only for a badge. `OpenCodeReadApi` is read-oriented and rejects non-200/non-JSON responses, so interaction writes cannot silently reuse it without a contract-specific extension. Graph coverage for cited Kotlin files has no recorded issue, best-effort only.

| Area | Impact/risk | Mitigation and regression evidence |
| --- | --- | --- |
| Feature 01 profile/vault | Profile revision/delete during reply can expose old scope or send with old credentials | Profile coordinator + scope generation before/after network; T05-15, T05-18; never persist credential in interaction cache |
| Feature 02 cache/browse badge | Existing badge may falsely show zero/allow interaction from incomplete list; DB migration can affect history cache | Keep `pending=null` as unknown; scoped tables/DAO transaction; T05-02, T05-14, T05-20 |
| Feature 03 prompt/abort | Agent busy and prompt state are not permission/question state; a reply must not confirm a prompt | Separate state machines and no retry coupling; T05-13, T05-21 |
| Feature 04 SSE/reconnect | REST snapshot races events and desktop changes; more than one stream/reducer can duplicate dialogs | One active lifecycle owner, generation/key guards, REST reconciliation, bounded dirty set; T05-11, T05-15, T05-16 |
| Compose navigation/UI | AlertDialog currently local state can survive stale list / rotation; rendering huge content risks jank/OOM | Dialog keyed by repository state and SavedState only IDs; bounded renderer; T05-08, T05-17, T05-19 |
| Network/auth | Endpoint reply contract unresolved; redirect/204/404 semantics may be misclassified; Basic auth must not leak | AI-01 gates actions, dedicated POST response handling, no auto retry/log raw request; T05-03..T05-07 |
| Desktop concurrency | Desktop resolve/reopen/delete while Android shows/submits dialog | Authoritative refresh, resolve terminal states and ignore late result by key/generation; T05-11, T05-12 |
| Backup/privacy | Pending text, question, diff and tool arguments are sensitive remote content | noBackup/cache exclusion/allowlist; T05-20; inspect backup transport separately from fake/unit tests |
| Provider chat/CI | Shared HTTP/DI or Compose changes can regress non-OpenCode chat; green fake tests do not prove live actions | Do not alter provider clients; run targeted regression plus existing suites at feature implementation SHA; T05-23/T05-24 |

## Failure modes and recovery

- HTTP timeout after reply: mark request `STALE`/uncertain, do not retry; reconcile authoritative pending list before presenting another action.
- Process death in `SUBMITTING`: recovery must not resend. Persist enough non-secret state to label uncertainty, then refresh.
- Desktop resolves while action is in-flight: terminal snapshot wins; close dialog and discard result whose key/generation is no longer current.
- SSE event is malformed, unknown, duplicated or out of order: parse fail/unknown is non-fatal; dirty/reconcile rather than applying untrusted delta; dedupe by scope/key/version only when contract establishes version.
- Oversize or adversarial tool/diff payload: reject/mark unavailable before full decode/render, retain existing safe cache, provide explicit bounded preview only.
- Auth/profile switch: cancel stream/actions and reject commit; never use a stale `credentialRef` or cross-directory session ID.

## Rollback and compatibility

Remove interaction route/actions and collectors without remote mutation. Retain server truth and isolate any local cleanup to the OpenCode interaction cache scoped by server/revision. No compatibility shim is planned before a schema has shipped; once shipped, migration/reset is explicit and tested. Feature 05 must be rebased/re-reviewed if Feature 02/03/04 changes cache, prompt, or SSE contracts.
