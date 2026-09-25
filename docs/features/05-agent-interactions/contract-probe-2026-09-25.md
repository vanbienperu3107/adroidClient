# AI-01 Contract Probe - 2026-09-25

Target: `https://opencode.hangocthanh.io.vn`, OpenCode `1.18.30` from `GET /global/health`. Probe used only an approved disposable sandbox directory; credentials and payload bodies are omitted.

## Version-pinned legacy API

| Capability | Contract from `/doc` and live probe |
| --- | --- |
| Pending permissions | `GET /permission?directory=...` returns an array. The probe returned `[]`. Schema: `id`, `sessionID`, `permission`, `patterns`, `metadata`, `always`, optional `tool.messageID/callID`. |
| Permission reply | `POST /permission/{requestID}/reply?directory=...` body `{"reply":"once"|"always"|"reject","message"?:string}`; documented `200` boolean, `400`, `404`. A synthetic missing ID returned `404`. |
| Pending questions | `GET /question?directory=...` returns an array. The probe returned `[]`. Schema: `id`, `sessionID`, `questions`, optional `tool`. |
| Question reply/reject | `POST /question/{requestID}/reply?directory=...` body `{"answers":[...]}` and `POST /question/{requestID}/reject?directory=...`; documented `200` boolean, `400`, `404`. Synthetic missing reply ID returned `404`. |
| Diff | `GET /session/{sessionID}/diff?directory=...&messageID?=...` returns `200` array. A diff entry has optional `file`, `patch`, `status` (`added|deleted|modified`) and required numeric `additions`, `deletions`. |
| Events | `/event?directory=...` uses JSON `{id,type,properties}`. `/doc` includes `permission.asked/replied`, `question.asked/replied/rejected`, `message.updated`, `message.part.updated`, `session.status`, `session.idle`, and `session.diff`. |

The remote permission enum establishes that `always` is a real server action. It does not establish a durable scope, expiry, or outcome without an actual pending-permission fixture. UI wording remains: `Always allow — phạm vi và thời hạn do OpenCode server quyết định.`

`/doc` SHA-256: `46db986090aae41846cd6dbe16225a1d883f0bbcb4c48814008d3f6ce140aa5c`; health SHA-256: `4aca1e6c3c3977c0bd9ec70a19e1386f423975a80e797babf7d6c243830d6d66`.

## Gate status

AI-01 is **PARTIAL**. Endpoints, request shapes, 200/400/404 outcomes, empty-list behavior, schemas and event names are pinned. Still required before actionable UI: a safe live pending permission/question fixture proving `once`/`always`/`reject` resolution, pagination/completeness, desktop resolution behavior, tool payload and byte/line/hunk budgets. No default rendering budget is invented.
