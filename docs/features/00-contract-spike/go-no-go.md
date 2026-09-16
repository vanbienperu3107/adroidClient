# Go/No-Go: Contract Spike

**Executed:** 2026-09-16 · **Server:** OpenCode 1.18.30 local (`opencode serve`, Basic Auth) · **Branch:** `feature/00-contract-spike`

## Capability Gate (TC-00H-02)

| Capability | Ket qua | Evidence |
| --- | --- | --- |
| REST contract (health/project/session/message/prompt/abort/diff) | **PASS** | `endpoint-matrix.md`, `fixtures/rest/` |
| Basic Auth REST + 401 mapping | **PASS** | `auth-matrix.md` |
| Basic Auth SSE | **PASS** | SSE stream authenticated thanh cong |
| `prompt_async` 204 + status busy/idle + abort semantics | **PASS** | `fixtures/rest/session-status-busy.json`, `message-history-after-abort.json` |
| Session rename (PATCH) / delete (hard, 404 sau delete) | **PASS** | `endpoint-matrix.md` |
| Project/directory scope qua `?directory=` | **PASS** | `project-scope-matrix.md` |
| Pending permission/question list API (`GET /permission`, `GET /question`) | **PASS** (endpoint ton tai, tra `[]`) | `endpoint-matrix.md` |
| Prompt correlation rule (pre-send snapshot + server-generated ID) | **PASS voi han che** | `prompt-correlation.md` — khong co idempotency key; UNKNOWN policy bat buoc |
| SSE event catalog (7 event types quan sat) | **PASS mot phan** | `sse-event-catalog.md` — delta/permission/question/diff event chua capture |
| HTTPS/TLS/Tailscale | **NOT_RUN** | Spike chay loopback HTTP; can server cong ty |
| Bearer qua gateway | **NOT_RUN** | Khong co gateway trong moi truong |
| Permission/question end-to-end (asked -> reply -> resolved) | **NOT_RUN** | Chua tao duoc permission trong phien capture ngan |
| Snapshot/event race, deletion khi offline | **NOT_RUN** | Can harness/desktop client trong phien dai hon |

## Verdict

**GO co dieu kien** cho Phase 1 (WI-01..WI-06):

- Nen tang an toan (vault, profile, Basic Auth, transport policy, navigation) co du contract evidence de bat dau.
- Cac muc **NOT_RUN** khong chan Phase 1 nhung **chan Phase 4-6**: truoc khi code WI-13 (prompt), WI-16 (sync), WI-21/22 (permission/question) phai chay lai cac testcase NOT_RUN voi server cong ty/HTTPS/gateway.

## Blockers con lai (owner: nguoi dung / ha tang cong ty)

1. Server cong ty + HTTPS/Tailscale de chay TC-00A-02..05.
2. Gateway bearer (neu MVP can Bearer) de chay TC-00B-04/05.
3. Phien capture dai voi desktop client de chay TC-00E-*, TC-00F-03/04, TC-00G-02..06.

## Ghi chu version

Moi ket qua gan voi **OpenCode 1.18.30**. Khi khoa version server cong ty khac, phai chay lai toan bo matrix.
