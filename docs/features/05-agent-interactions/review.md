# Design Review Feature 05

Revision reviewed: `08b9bc21d392f045114a180611e7760d33bcbe20` plus documentation revision 1.1 in this worktree. Scope of this review is design/docs only; no application implementation or runtime test was executed. Runtime testcase status remains NOT_RUN, not PASS.

## Normal

### Round 1

Verdict: REQUEST_CHANGES.

| Finding | Impact | Correction and verification |
| --- | --- | --- |
| Permission/question reply endpoint and `always` semantics were not evidenced by spike; a direct UI plan would invent behavior. | Could approve/reject incorrectly or claim permanent authorization falsely. | Added AI-01 hard gate, explicit blocker and contract requirements in `plan.md`; verify T05-01 before AI-02..AI-07. |
| Requirement coverage did not make desktop dialog closure and diff limits independently auditable. | Core requested behavior could be omitted by a broad UI item. | Split R05-05/R05-06 and AI-07/AI-08; added T05-12/T05-17/T05-22. |

### Round 2

Verdict: PASS.

Evidence: `plan.md` defines seven behavior requirements plus verification, boundaries and invariants; `backlog.md` gives every item ID, input, output, scope, dependency, acceptance and status; `testcases.md` maps each requirement to named cases. All requested feature slices are explicit without expanding into editor/offline queue/provider work.

## High

### Round 1

Verdict: REQUEST_CHANGES.

| Finding | Impact | Correction and verification |
| --- | --- | --- |
| Existing browse flow aggregates pending by session, while requests/actions require request identity and scope. | Cross-directory/server reply or desktop-resolution race possible. | Added `InteractionScope`, request composite key, scoped persistence and T05-02/T05-15. |
| REST/SSE ownership was insufficiently explicit. | Event/snapshot overlap can duplicate dialogs or lose desktop resolution. | Declared REST authoritative, SSE notification/dirty signal, bounded reconcile and AI-04; verify T05-11/T05-14/T05-16. |
| Existing generic read API only models 200 JSON, unlike likely POST interaction outcomes. | A 204/expired response could be mishandled as failure/success. | Isolated AI-03 transport/action coordinator and response matrix in AI-01; verify T05-06. |

### Round 2

Verdict: PASS.

Evidence: impact matrix ties Feature 01-04, UI, network, backup and provider regression to mitigations/tests. Backlog ordering prevents UI actions before schema/contract/reconcile. The dependency on Feature 03/04 is explicit rather than assumed merged.

## XHigh

### Round 1

Verdict: REQUEST_CHANGES.

| Finding | Impact | Correction and verification |
| --- | --- | --- |
| Timeout/process death after action had no terminal-state policy. | Automatic replay could duplicate unsafe agent action. | `SUBMITTING -> STALE/uncertain`, no auto retry/replay, authoritative refresh; T05-07. |
| Remote tool/diff/pending content had no pre-allocation bounds or execution boundary. | OOM, UI hang, link/command abuse, content leakage. | AI-01 pins budgets; plan requires bounds before decode/render and no auto execution/load/logging; T05-17/T05-19/T05-22. |
| Desktop resolution could race a late Android action result. | Resolved dialog might reopen or commit an obsolete result. | Generation/scope/request-key guard and terminal snapshot precedence; T05-12/T05-15. |

### Round 2

Verdict: PASS.

Evidence: plan now specifies failure policy for timeout, cancellation, process death, malformed/overflow events, profile switch and untrusted content. Test design covers concurrency, recovery, credential boundary, backup and payload limits. No conditional runtime claim is made: contract and runtime work remain NOT_STARTED/NOT_RUN until implementation.

## Final design verdict

PASS for documentation design at the stated revision. The target-server probe now makes AI-01 PARTIAL: endpoint/path/body, enum and 200/400/404 are pinned, but live pending-resolution, pagination, desktop race, tool payload and render budgets are not. This is not authorization to mark implementation, actionable API capability, runtime validation or release readiness PASS.
