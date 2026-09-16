# Auth Matrix (OpenCode 1.18.30)

Server bat auth bang `OPENCODE_SERVER_PASSWORD`; username mac dinh `opencode`.

| Case | Request | Ket qua | Ghi chu |
| --- | --- | --- | --- |
| Khong co Authorization | `GET /global/health` | **401** | Server tu choi truoc khi tra data |
| Basic dung (user `opencode` + password dung) | `GET /global/health` | **200** | `{"healthy":true,"version":"1.18.30"}` |
| Basic sai password | `GET /global/health` | **401** | Khong lo credential trong response |
| Basic dung | `GET /session`, `POST /session`, `PATCH`, `DELETE`, `prompt_async`, `abort`, `/event` SSE | 200/204 | Toan bo REST + SSE dung chung Basic header |
| Bearer qua gateway | — | **NOT_RUN** | Khong co gateway bearer trong moi truong spike. Phai chay lai khi co gateway cong ty (TC-00B-04/05) |

## Ket luan cho implementation

- `authMode=Basic`: header `Authorization: Basic base64(username:password)`; ap dung cho ca SSE stream.
- 401 la response nhat quan cho thieu/sai credential — map sang `Unauthorized`, giu credential entry (khong xoa key theo bang Keystore trong plan 1.4).
- `authMode=Bearer`: chua co evidence trong spike nay; giu BLOCKED cho den khi co gateway that.
