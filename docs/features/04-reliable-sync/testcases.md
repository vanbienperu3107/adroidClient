# Feature 04 — Reliable Sync: Testcases

Evidence class: U deterministic unit/fake; I fake HTTP/SSE + Room integration; A Android lifecycle/instrumented; E controlled real OpenCode + desktop. Tất cả **NOT_RUN** vì chưa có implementation/runtime evidence.

| ID/AC | Preconditions | Steps | Expected | Evidence/status |
|---|---|---|---|---|
| TC-RS-01 AC-01 | Fake stream counts connections, one scope | activate 3 lần; interleave refresh/navigation | max concurrent 1, owner cũ closed, one scheduler | U, NOT_RUN: counters/log |
| TC-RS-02 AC-02 | Delayable REST/SSE; scopes A/B | start A, switch revision/project B, release A late | A has zero cache/UI/dirty/retry side effect | U/I, NOT_RUN: generation trace/DB |
| TC-RS-03 AC-03 | valid event + REST fixture | emit, advance debounce | event dirties; one REST reconcile; cache only snapshot | U/I, NOT_RUN: network/DAO log |
| TC-RS-04 AC-04 | small configured cap | emit cap+ IDs | queue bounded; one full snapshot; converges | U, NOT_RUN: size/call assertion |
| TC-RS-05 AC-05 | cache old rows, duplicate/out-of-order/partial deltas | deltas then authoritative snapshot | no duplicates/rollback; prune only snapshot scope | I, NOT_RUN: exact Room rows |
| TC-RS-06 AC-06 | cached row; delete event, partial page, point 404 | execute each path | event/page absence retain; current owned 404/complete snapshot deletes | I, NOT_RUN: DB/network |
| TC-RS-07 AC-07 | fake clock/random; transient, 401/403, malformed | disconnect/retry; advance clock | bounded full jitter one timer; deactivate cancels; auth no retry | U/I, NOT_RUN: delay trace/log |
| TC-RS-08 AC-07 | lifecycle/connectivity harness | active→background→foreground; offline→online | close background stream; bootstrap then stream; stale cache readable | A, NOT_RUN: lifecycle/UI/cache |
| TC-RS-09 AC-08 | controlled server + desktop client | desktop rename/delete/add while Android views | Android eventually equals server; no duplicate/cross-project | E, NOT_RUN: server/client/DB |
| TC-RS-10 AC-09 | two profiles/scopes; request capture | inject foreign event; rotate profile | no cross-scope request/write; no secrets logs | U/I, NOT_RUN: capture/log scan |
| TC-RS-11 regression | existing browse fixtures/offline fake | projects→sessions→history; failure/retry | stale/locked/error preserved | U/I, NOT_RUN: assertions |
| TC-RS-12 recovery | persisted cache/profile | kill between dirty/reconcile; relaunch | no old owner; bootstrap converges | A/E, NOT_RUN: process/DB/server |

Sau implementation ghi SHA/config/device/server; chạy U→I→A/E. Không dùng U thay lifecycle/E2E. Browser **NOT_APPLICABLE** trừ khi web UI đổi. Đính command, exit result, redacted logs, assertion/DB artifacts, screenshots native. Blocked/NOT_RUN không bao giờ PASS.
