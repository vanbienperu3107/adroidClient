# Endpoint Matrix (OpenCode 1.18.30, Basic Auth)

Tat ca endpoint yeu cau `Authorization: Basic ...` khi server bat password. Khong co auth -> `401`.

| Endpoint | Method | Ket qua probe | Ghi chu schema |
| --- | --- | --- | --- |
| `/global/health` | GET | 200 | `{"healthy":true,"version":"1.18.30"}` |
| `/project` | GET | 200 | Array: `id`, `worktree`, `vcs?`, `time{created,updated}`, `sandboxes[]`. Liet ke MOI project server biet, khong chi project hien tai |
| `/project/current` | GET | 200 | Mot project object. Ho tro `?directory=<path>`: current project doi theo directory query | 
| `/session` | GET | 200 | Array session. Ho tro `?directory=<path>` de scope; directory khac tra `[]` |
| `/session` | POST | 200 | Body `{"title": "..."}`. Response co `id`, `slug`, `projectID`, `directory`, `title`, `version`, `time` |
| `/session/{id}` | GET | 200 / 404 | 404 body: `{"name":"NotFoundError","data":{"message":"Session not found: <id>"}}` |
| `/session/{id}` | PATCH | 200 | Body `{"title": "..."}` doi title; response session day du |
| `/session/{id}` | DELETE | 200 | Hard delete: GET sau do tra 404 |
| `/session/status` | GET | 200 | Map `sessionID -> {"type":"idle"|"busy"|"retry"}`; session idle KHONG xuat hien trong map (empty `{}` = tat ca idle) |
| `/session/{id}/message` | GET | 200 | Array `{info, parts[]}`. `info`: `id`, `sessionID`, `role`, `time`, `error?`, `parentID?`, token/cost fields. `parts[]`: `id`, `sessionID`, `messageID`, `type`, `text?` |
| `/session/{id}/prompt_async` | POST | **204** | Body `{"parts":[{"type":"text","text":"..."}]}`. Fire-and-forget |
| `/session/{id}/abort` | POST | 200 | Sau abort: status ve idle; assistant message co `error: {"name":"MessageAbortedError"}` |
| `/session/{id}/diff` | GET | 200 | `[]` khi khong co thay doi |
| `/permission` | GET | 200 | `[]` khi khong co pending. Ho tro `?directory=` |
| `/question` | GET | 200 | `[]` khi khong co pending |
| `/event` | GET (SSE) | 200 | Xem `sse-event-catalog.md` |

## Phat hien quan trong

1. **`prompt_async` = 204** dung nhu plan gia dinh.
2. **Delete la hard delete** — GET tra 404 ngay sau DELETE. Reconcile phai xu ly session bien mat.
3. **`session/status` chi chua session khong-idle.** Empty map nghia la moi session idle. App KHONG duoc hieu "khong co trong map" la "khong ton tai".
4. **Assistant message giu `error.name = MessageAbortedError` sau abort** — partial/aborted response co evidence ro rang trong history.
5. **User message tu `prompt_async` co `id` (`msg_...`) va parts co `id` (`prt_...`) do server sinh.** Payload gui di KHONG co field client-supplied ID trong schema da thu — xem `prompt-correlation.md`.
6. **Unknown path tra 200** (SPA/fallback behavior) — client KHONG duoc dung status code cua path la de detect endpoint ton tai; can kiem tra content-type/schema.
7. **`GET /permission` va `GET /question` ton tai va tra pending list** — dap ung yeu cau phuc hoi pending request cua plan 1.4 (WI-00e).
8. **Error 404 co schema `{"name": "...", "data": {"message": "..."}}`** — dung duoc cho error mapping.
