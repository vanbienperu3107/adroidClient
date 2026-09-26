# Feature 03 - Prompt va SSE co ban

Revision tai lieu: 1.1. Baseline source: `08b9bc21d392f045114a180611e7760d33bcbe20`, branch `feature/03-prompt-sse`. Tai lieu nay chi phan ra thiet ke; chua co implementation va tat ca testcase runtime la `NOT_RUN`.

Thay doi revision 1.1: cap nhat theo evidence probe live Feature 00 (`probe-run-2.md`, `probe-public-https.md`) ve client-supplied `messageID`. Xem [decisions.md](decisions.md).

## Intent va gioi han

Feature cho phep gui mot prompt vao session OpenCode dang mo, quan sat cap nhat co ban qua mot ket noi SSE cua scope active, huy agent dang chay, va doi chieu ket qua voi REST history. Feature khong them reconnect/backoff, durable cursor, delta streaming, permission/question, diff, offline queue, hoac dong bo tin cay cua Feature 04/05.

`ActiveScope` la bo ba bat buoc `(serverId, profileRevision, directory)` va session dang mo. Toan ung dung co toi da mot SSE stream; stream chi duoc mo khi scope active da `Ready`, dong truoc khi doi server, revision, directory, session, background, hoac ViewModel bi clear. SSE la notification nhanh, khong la source of truth va khong tu minh xac nhan prompt; REST history/status la co so reconcile.

## Contract da biet va quyet dinh

- Spike OpenCode 1.18.30 ghi `POST /session/{id}/prompt_async` body text tra `204`. Probe live da xac minh optional client-supplied `messageID` duoc luu thanh `info.id` trong history; gui lai cung `messageID` van tao part moi, nen **khong idempotent**.
- Prompt local sinh `clientMessageId` ngau nhien truoc persist va gui no trong field `messageID` cua request. ID nay la khoa correlation giua optimistic UI, pending record va server history; **khong** la idempotency key va khong suy dien server deduplicate.
- Truoc send, luu pre-send snapshot cac server `messageId` cua dung `ActiveScope`/session. `CONFIRMED` chi duoc dat khi history/SSE cung cap message dung `clientMessageId`, dung session va role `user`. Neu history khong co ID sau timeout/loss, giu `UNKNOWN`; khong correlation theo text/timestamp.
- `session.idle`, HTTP 200 cua abort, SSE heartbeat, hoac chi text/timestamp gan dung khong la bang chung confirmation.
- Khong co auto retry cho `prompt_async` hay abort. Timeout, network loss, cancellation sau khi request da bat dau, va process death trong `SENDING` deu co the da den server, nen phai `UNKNOWN` va reconcile truoc bat ky resend co chu dich nao.
- Abort dung `POST /session/{id}/abort` sau khi revalidate session ownership trong dung directory. Abort thanh cong chi kich hoat refresh status/history; partial assistant va `MessageAbortedError` phai duoc giu.
- Event catalog da quan sat co `server.connected`, `server.heartbeat`, `session.updated`, `session.status`, `session.idle`, `message.updated`, `message.part.updated`. Parser chi ap dung envelope JSON hop le, event co ID scope hop le; event unknown/malformed/khong scope thi bo qua an toan va danh dau refresh neu can, khong luu raw payload hay crash.

## Prompt state machine

| State | Vao state | Hanh vi/thoat state |
| --- | --- | --- |
| `PENDING` | Local record da persist, chua bat dau HTTP | Chi chuyen `SENDING` neu scope/revision/session van active va ready. PENDING sau restart khong tu gui. |
| `SENDING` | HTTP `prompt_async` bat dau | `204` -> `ACCEPTED`; loi co bang chung request chua chap nhan -> `FAILED`; timeout/cancel/network/kill -> `UNKNOWN`. |
| `ACCEPTED` | Nhan dung HTTP 204 | Reconcile history/status; evidence duy nhat -> `CONFIRMED`; evidence mo ho sau reconcile -> `UNKNOWN`. |
| `CONFIRMED` | Server message duoc correlation duy nhat | Terminal cho lan send nay; response/event den muon khong duoc ha state. Khong dong nghia agent da idle hay assistant hoan tat. |
| `UNKNOWN` | Khong the biet server nhan hay chua, hoac correlation mo ho | Khong retry. Chi nguoi dung co the chon gui lan moi voi clientMessageId moi va canh bao nguy co duplicate. |
| `FAILED` | Co evidence cu the request khong duoc server chap nhan truoc khi co the xu ly | Terminal. 401/403 khoa scope; validation/4xx phai hien thi error da sanitize. Khong dung khi chi timeout/mat response. |

Moi session co toi da mot send Android active (`PENDING`/`SENDING`/`ACCEPTED`); day la local UX guard, khong khoa desktop/server. Agent state (idle/busy/retry/error) la state rieng.

## Process death va bao mat

- Persist toi thieu: scope key, session ID, clientMessageId, digest/representation noi dung can thiet de correlate, state, timestamp, pre-send server message IDs, va uncertainty reason. Khong persist raw SSE, Authorization, credential, hay optimistic content vao SavedState/route/log/backup. Kiem tra allowlist backup truoc khi them local prompt storage.
- Khoi dong lai: PENDING -> `UNKNOWN` (khong auto-send); SENDING -> `UNKNOWN`; ACCEPTED giu `ACCEPTED` trong khi reconcile, sau do `CONFIRMED` hoac `UNKNOWN`. Khong co nhanh tu dong chuyen sang `FAILED` chi vi mot history read khong thay message.
- Moi HTTP/SSE callback phai mang generation cua `ActiveScope`; truoc UI/DB write recheck profile hien hanh, directory chinh xac, ownership session, va generation. Dong stream/cancel call khi scope doi; callback cu chi bi bo qua.
- Directory la input remote opaque: khong lowercase/normalize. Session ID trong route khong duoc cap quyen; truoc prompt/abort va ap dung event phai xac minh session thuoc dung directory. Credential chi doc qua vault va khong duoc dua vao URL/log/event cache.

## Gate va blocker

| Gate | Trang thai | Anh huong |
| --- | --- | --- |
| G03-01: endpoint prompt/abort, Basic auth va directory scope | PASS theo fixture spike 1.18.30 | Cho phep thiet ke WI-03-02/05; implementation phai test lai server target. |
| G03-02: client-supplied `messageID` persistence/idempotency | PASS correlation-only theo probe live OpenCode 1.18.30 | Gui ID client de correlation history/SSE; khong retry, khong coi server deduplicate. Implementation phai re-probe server target/version pin. |
| G03-03: SSE stream scope A/B va auth/header voi transport Android duoc chon | BLOCKED | WI-03-01 implementation khong duoc bat dau cho den khi probe server target. |
| G03-04: schema assistant `message.part.delta` hoan chinh | DEFERRED sang Feature 04 | Ngoai scope Feature 03; khong render/apply delta. |
| G03-05: live process-death/backup evidence | NOT_RUN | Chan dong Feature implementation, khong chan PASS cua thiet ke tai lieu. |

`BLOCKED`, `DEFERRED`, va `NOT_RUN` khong phai `PASS` va khong duoc bo qua khi mo work item phu thuoc.

## Acceptance requirements

| ID | Yeu cau/acceptance |
| --- | --- |
| R03-01 | Chi co mot SSE cho `ActiveScope`; scope doi/nen app dong stream va callback cu khong ghi state moi. |
| R03-02 | Parser co ban xu ly dung envelope da pin, event unknown/malformed an toan, va khong luu raw payload/secret. |
| R03-03 | Prompt dung state machine day du; `clientMessageId` gui trong `messageID` de correlation, khong la idempotency va khong auto retry. |
| R03-04 | `ACCEPTED` chi tu HTTP 204; `CONFIRMED` chi tu correlation server duy nhat, tach agent state. |
| R03-05 | Abort chi dung session/directory da xac minh; HTTP success khong tu coi idle; partial aborted history duoc giu. |
| R03-06 | Timeout, event/response out-of-order, desktop concurrent send va process death khong tao request lap hay confirmation nham. |
| R03-07 | Secret/directory scope duoc bao ve; stale scope/callback khong lam lo hoac ghi cheo du lieu. |

Xem [backlog.md](backlog.md), [impact.md](impact.md), [testcases.md](testcases.md) va [review.md](review.md).
