# SSE Event Catalog (OpenCode 1.18.30)

Stream: `GET /event` voi Basic Auth. Format: SSE frame `data: {...}` — payload JSON co `type` va `properties`.

## Event da quan sat (capture that trong spike)

| Event type | Khi nao | Payload chinh |
| --- | --- | --- |
| `server.connected` | Ngay khi stream mo | — |
| `server.heartbeat` | Dinh ky | — |
| `session.updated` | Tao/rename session, cap nhat metadata | `properties.info` = session object day du |
| `session.status` | Doi trang thai | `properties.sessionID`, `properties.status.type` = `idle`/`busy` (idle xuat hien khi ket thuc) |
| `session.idle` | Khi session ve idle (legacy, van duoc emit o 1.18.30) | `properties.sessionID` |
| `message.updated` | User message tao, assistant message tao/ket thuc | `properties.info` = message info (id, sessionID, role, time, error?) |
| `message.part.updated` | Part moi/cap nhat (text part cua user prompt, assistant delta) | `properties.part` (id, sessionID, messageID, type, text) |

## Event trong plan chua quan sat duoc trong lan chay nay

| Event | Ly do |
| --- | --- |
| `session.created` / `session.deleted` | Tao/xoa qua REST co the chi emit `session.updated`; can xac minh them voi capture dai hon |
| `message.removed`, `message.part.removed` | Khong co thao tac xoa message trong capture |
| `message.part.delta` | Assistant bi abort truoc khi stream text; can capture voi response hoan chinh |
| `permission.asked/replied`, `question.*` | Khong tao duoc permission/question trong phien capture ngan |
| `session.error`, `session.diff` | Khong phat sinh trong capture |

## Ket luan cho reducer

1. Envelope on dinh: `{"type": "<event>", "properties": {...}}` — parser tach `type` roi dispatch; unknown type bo qua an toan.
2. `session.idle` **van ton tai o 1.18.30** — plan 1.4 giu tuong thich la dung.
3. `message.updated` mang **info day du** (khong phai delta) — co the upsert truc tiep theo `info.id`.
4. `message.part.updated` mang **part day du** voi `id`/`messageID` — upsert theo `(messageID, partId)`; khong can noi delta trong truong hop nay.
5. `session.status` va `session.idle` co the den gan nhau — reducer phai idempotent.
6. Cac event chua quan sat phai duoc xac minh lai truoc khi code reducer tuong ung (WI-12/WI-16); khong suy dien schema.
