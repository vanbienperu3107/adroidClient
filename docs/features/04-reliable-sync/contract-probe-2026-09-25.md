# RS-001 Contract Probe - 2026-09-25

Target: `https://opencode.hangocthanh.io.vn`; disposable directory under the approved contract-probe sandbox. Credentials are intentionally omitted. `GET /global/health` returned `{"healthy":true,"version":"1.18.30"}`.

## Observed contract

| Item | Result |
| --- | --- |
| Stream | `GET /event?directory=<opaque-directory>` with HTTP Basic Auth returns `200 text/event-stream` |
| Envelope | Observed frames use `data: {"id":"evt_...","type":"server.connected|server.heartbeat", "properties":{...}}` |
| Event IDs | Event IDs are present inside JSON, not SSE `id:` fields |
| Resume | `/doc` declares only optional `directory` and `workspace` query parameters. No `Last-Event-ID`, cursor, or resume parameter is documented for this legacy endpoint. |
| Delete | A disposable `POST /session` returned `200`; `DELETE /session/{id}` returned `200` body `true`; the active stream observed `session.created` then `session.deleted` for that ID. |
| Authentication failure | Request without Basic credentials returned `401` at the public proxy. |

`/doc` SHA-256: `46db986090aae41846cd6dbe16225a1d883f0bbcb4c48814008d3f6ce140aa5c`. Health response SHA-256: `4aca1e6c3c3977c0bd9ec70a19e1386f423975a80e797babf7d6c243830d6d66`.

## Implementation boundary

The selected baseline is legacy `/event`, with REST reconciliation after every connect/reconnect because it has no durable replay contract. Treat every valid event as a dirty notification only. `session.deleted` is evidence to request a current owned REST point-read or full snapshot, not permission to blindly prune cache.

## Remaining limits

No target fixture exercised a non-empty stream payload beyond create/delete, 403 authorization denial, abrupt network close, or concurrent desktop mutation. Those cases remain required test evidence; this probe does not make runtime validation PASS.
