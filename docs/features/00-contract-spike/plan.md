# Plan Chi Tiet: 00 Contract Spike

**Nguồn:** [plan 1.4](../../plan.md) va [backlog](../../backlog.md)

**Work items:** WI-00a den WI-00h

**Trang thai:** Chua bat dau

## 1. Muc Dich

Contract Spike khong trien khai tinh nang Android. Muc dich la chot contract cua OpenCode server muc tieu truoc khi tao database, credential vault, UI, SSE manager hoac logic prompt.

Ket qua phai tra loi duoc cac cau hoi sau bang bang chung co the tai lap:

- Android se ket noi den server va project nao, qua HTTPS/Tailscale nhu the nao.
- OpenCode server version nao duoc ho tro trong MVP.
- Basic Auth, Bearer Auth qua gateway co san va SSE gui header auth theo contract nao.
- Endpoint, payload, error va directory/workspace scope cua server muc tieu la gi.
- API co ho tro rename/delete session, pending permission/question va reply/reject hay khong.
- Prompt co correlation/idempotency key hay dau hieu an toan nao de chuyen ACCEPTED sang CONFIRMED hay khong.
- Event SSE, ordering, delta va full snapshot phai duoc reducer xu ly nhu the nao.

## 2. Pham Vi

### Bao gom

- Dung OpenCode server test giong server cong ty ve version va auth mode.
- Thu nghiem HTTP/SSE qua HTTPS va Tailscale.
- Thu thap va khuu secret API fixtures, SSE captures va bang endpoint matrix.
- Xac minh contract du cho cac feature da cam ket trong MVP.
- Ghi ket luan go/no-go va cac gioi han da biet.

### Khong bao gom

- Khong them package Android, Ktor SSE, Room, vault, route, UI hay test app.
- Khong luu bearer token, Basic password, cookie, Authorization header, private host/IP hay raw prompt vao repository.
- Khong sua OpenCode server, reverse proxy hoac gateway de lam contract phu hop voi app.
- Khong tu dong commit/push artifact.

## 3. Baseline va diem tich hop sau spike

Codebase tai thoi diem lap plan:

- Index Codebase MCP: `workspace-Project-adroidClient`, 1349 nodes, 4320 edges, HEAD `e91e61e30b195988f9bf83d0f48eedd38276f267`.
- `NetworkClient.kt` co Ktor, JSON, timeout va sanitize Authorization, nhung chua co Ktor SSE.
- `AndroidManifest.xml` dang `usesCleartextTraffic="true"`; transport policy se duoc sua o WI-04 sau spike.
- `backup_rules.xml` va `data_extraction_rules.xml` hien la sample rong; backup allowlist se duoc lam o WI-03 sau spike.
- Chua co OpenCode source, vault, lifecycle coordinator hoac OpenCode route.

Spike chi tao tai lieu va fixtures da redacted trong thu muc feature nay. Ket qua dung de quyet dinh API abstraction cua `data/opencode` o cac phase sau.

## 4. Moi Truong Thu Nghiem

### 4.1 Server test

- Dung OpenCode server cung version voi server cong ty; neu chua the, ghi ro version khac biet va khong dung ket qua lam go/no-go cho production.
- Tao it nhat hai project/directory rieng biet, moi project co it nhat mot session de kiem tra scope.
- Tao mot session co message, part, tool/permission/question neu server ho tro.
- Ghi lai: OpenCode version, command/packaging, API base URL da da chuan hoa va auth mode. Khong ghi secret.

### 4.2 Network va TLS

- Kiem tra Android path: device/emulator -> Tailscale/HTTPS -> OpenCode server hoac gateway.
- TLS certificate phai hop le cho hostname app se dung; khong dung bypass trust manager.
- Thu ca DNS/Tailscale route failure, TLS failure, timeout va server unavailable.
- Chi kiem tra Bearer neu gateway da ton tai. Gateway moi ngoai pham vi MVP.

### 4.3 Auth profiles

| Profile | Dieu kien | Header can kiem tra | Ket qua can ghi |
| --- | --- | --- | --- |
| Basic | OpenCode chuan bat `OPENCODE_SERVER_PASSWORD` | `Authorization: Basic ...` | Username default/custom, 200, 401 va 403 neu co |
| Bearer | Gateway co san xu ly bearer truoc OpenCode | `Authorization: Bearer ...` | 200, 401/403, SSE co nhan header hay khong |
| No auth | Chi dung de doi chieu server behavior trong moi truong co lap | Khong co Authorization | Co canh bao, khong du dieu kien production |

Khong dua auth header, credential, URL co token query string hoac raw request log vao fixture.

## 5. Cong Viec Chi Tiet

### WI-00a: Khoa server va network topology

1. Ghi version OpenCode server bang `/global/health` va thong tin build neu co.
2. Ghi topology da redacted: Android -> HTTPS/Tailscale -> OpenCode hoac gateway -> OpenCode.
3. Kiem tra mot request REST va mot SSE connection qua duong di production du kien.
4. Luu `environment.md` trong thu muc feature voi placeholder hostname, khong co IP/private DNS neu khong can thiet.

**Pass:** Version, topology, TLS va it nhat mot auth profile co bang chung tai lap.

### WI-00b: Xac minh authentication

1. Test Basic Auth dung credential, sai password va username khac default neu server cho phep.
2. Test endpoint REST va `/event` SSE voi Basic Auth.
3. Neu co gateway Bearer, test token dung, token sai/het han va SSE qua gateway.
4. Xac minh 401/403 body va header co thong tin an toan de map loi UI.
5. Kiem tra log fixture da redacted: khong con `Authorization`, base64 Basic payload, token, cookie hay URL query secret.

**Pass:** Chot auth mode, header format, behavior SSE va error mapping cho moi mode duoc ho tro.

**Blocker:** Bearer duoc yeu cau nhung gateway khong ton tai/khong forward SSE Authorization; loai Bearer khoi MVP hoac xin quyet dinh moi.

### WI-00c: Xac minh REST contract

Lap bang endpoint cho tung API duoc goi trong MVP:

| Nhom | Endpoint can probe | Can ghi lai |
| --- | --- | --- |
| Health | `GET /global/health` | Schema, version, auth/error |
| Project | `GET /project`, `GET /project/current` | ID, directory, pagination, scope |
| Session | `GET /session`, `GET /session/{id}`, `PATCH`, `DELETE`, `GET /session/status` | Query scope, payload update, delete behavior, status types |
| Message | `GET /session/{id}/message`, `GET /session/{id}/message/{messageID}` | Pagination/full snapshot, stable IDs, part schema, deletion evidence |
| Prompt/abort | `POST /session/{id}/prompt_async`, `POST /session/{id}/abort` | Request body, 204 semantics, timeout/abort state |
| Diff | `GET /session/{id}/diff` | Empty/large diff, scope, schema |

Voi moi endpoint, fixture phai co: method/path, query keys khong nhay cam, request schema da redacted, response schema, response mau toi thieu, 200/4xx/5xx behavior va pagination/full snapshot semantics.

**Pass:** Co `endpoint-matrix.md` va fixture JSON da redacted cho toan bo endpoint bat buoc.

### WI-00d: Xac minh project/directory scope

1. Lap hai project A/B tren cung server.
2. Goi project, session, status, message, prompt, diff va event voi scope A/B theo contract server.
3. Thu session ID hop le trong project A voi scope B neu co the; ghi ket qua 404/empty/other.
4. Xac minh project identity server tra ve co phai directory, project ID hay ca hai.
5. Chot key app su dung: `serverId + directory + sessionId`.

**Pass:** Ma tran A/B chung minh request khong tron context; danh sach query/path field bat buoc da chot.

**Blocker:** Server khong cung cap cach scope project/directory on dinh trong khi MVP bat buoc mot server nhieu project.

### WI-00e: Permission va question

1. Tao permission va question tu session test theo cach server ho tro.
2. Thu event `permission.asked`, `permission.replied`, `question.asked`, `question.replied`, `question.rejected`.
3. Xac minh endpoint reply/reject thuc te va payload/valid reply values.
4. Xac minh API list pending requests, pagination, scope va cach phan biet resolved/expired request.
5. Tra loi mot request tu desktop, sau do kiem tra Android client probe co the phat hien request da het hieu luc.

**Pass:** Co contract day du de tao, lay pending, reply/reject va reconcile permission/question.

**Blocker:** Khong co API hoac event du de khoi phuc pending request sau offline. Khong bat dau WI-21/WI-22 cho den khi co quyet dinh scope thay the.

### WI-00f: Prompt correlation va idempotency

1. Gui prompt qua `prompt_async`, ghi nhan response 204, SSE va REST history.
2. Kiem tra client co the gui message/part ID hay correlation field hay khong.
3. Neu client ID co ho tro, kiem tra server co giu lai va co thanh idempotency key hay khong.
4. Gia lap timeout sau khi server da nhan request; tai history de xac dinh co bang chung correlation nao.
5. Gui hai prompt cung noi dung tu Android probe va desktop/session khac thoi diem; xac minh content va `session.idle` khong du de correlation.
6. Chot dieu kien ACCEPTED -> CONFIRMED, UNKNOWN va FAILED.

**Pass:** Co correlation rule dua tren ID/server evidence, hoac ket luan ro cac truong hop phai giu UNKNOWN.

**Blocker:** MVP yeu cau tu dong CONFIRMED nhung server khong co evidence correlation an toan. Can doi acceptance criteria hoac gateway/server contract.

### WI-00g: Capture SSE

1. Mo stream dung project/directory/auth scope.
2. Capture event MVP: session create/update/delete/status/error, message update/remove, part update/delta/remove, permission/question, diff, heartbeat va `session.idle` neu xuat hien.
3. Ghi envelope, event name, payload schema, ordering quan sat, duplicate/retry behavior va ID fields.
4. Thu malformed frame, unknown event va stream disconnect/reconnect.
5. Thuc hien capture trong luc REST history snapshot dang chay de tao fixture race.
6. Redact truoc khi luu: message text, tool input/output, diff, file path nhay cam, token, cookie, host noi bo va metadata co the dinh danh.

**Pass:** Co `sse-event-catalog.md`, fixture da redacted va ghi chu reducer cho unknown/duplicate/out-of-order.

### WI-00h: Go/no-go

1. Tong hop endpoint matrix, fixture inventory, auth matrix, project scope, correlation rule va event catalog.
2. Liet ke contract version-specific va dieu gi co the thay doi khi nang cap server.
3. Danh gia tung capability `PASS`, `FAIL`, `BLOCKED` hoac `NOT_APPLICABLE`.
4. Quyet dinh go/no-go cho Phase 1 va cap nhat backlog neu co work item bi loai/doi scope.

## 6. Cau Truc Artifact

```text
docs/features/00-contract-spike/
├── plan.md
├── environment.md                 # topology/version da redacted
├── endpoint-matrix.md             # method, scope, auth, schema, errors
├── auth-matrix.md                 # Basic/Bearer/no-auth test outcomes
├── project-scope-matrix.md        # project A/B behavior
├── prompt-correlation.md          # ACCEPTED/CONFIRMED/UNKNOWN rule
├── sse-event-catalog.md           # event schema va reducer notes
├── fixture-manifest.md            # source/version/redaction checklist
├── go-no-go.md                    # verdict va blockers
└── fixtures/
    ├── rest/
    └── sse/
```

Fixture chi duoc luu khi da qua checklist redaction. Dung placeholder nhu `<REDACTED_TOKEN>`, `<REDACTED_HOST>`, `<REDACTED_CONTENT>`; khong dung secret that trong committed file.

## 7. Redaction va bao mat artifact

Truoc khi luu moi artifact, kiem tra va xoa:

- `Authorization`, cookie, bearer token, Basic base64 credential, password, username neu nhay cam.
- Query parameter co secret, private hostname/IP, tailnet name, server directory nhay cam.
- Prompt/message content, tool input/output, stack trace, diff, file path va metadata co the chua ma nguon/noi dung cong ty.
- UUID/session ID neu co the lien ket nguoc ve he thong that; thay bang ID gia nhat quan trong fixture.

Fixture phai giu lai schema, field optional, event type, ordering va status code can thiet cho implementation. Redaction khong duoc lam fixture tro thanh JSON/SSE khong hop le.

## 8. Test Matrix

| Capability | Positive | Negative/race | Evidence |
| --- | --- | --- | --- |
| HTTPS/Tailscale | Health 200 | DNS, TLS, timeout | environment + endpoint fixture |
| Basic Auth | REST/SSE 200 | 401, 403 neu co | auth matrix |
| Bearer gateway | REST/SSE 200 | 401/403, header forwarding | auth matrix |
| Project scope | A chi thay A | A ID voi B scope | project scope matrix |
| Session CRUD | List/get/update/delete | 404, forbidden, stale session | REST fixtures |
| History | Full snapshot/part ID | pagination, deleted message | REST fixtures |
| Prompt | 204 + message evidence | timeout sau nhan, duplicate text, desktop xen ke | correlation report |
| Abort | Request accepted | HTTP success khi status van busy | REST/SSE fixtures |
| Permission/question | Ask/list/reply/reject | offline, desktop resolved, expired | REST/SSE fixtures |
| SSE | Event schema | unknown, malformed, duplicate, out-of-order, reconnect | event catalog |
| Snapshot race | Final state hoi tu | event khi REST dang tai | race fixture |

## 9. Gate va quy tac quyet dinh

### Go

Phase 1 duoc phep bat dau khi tat ca dieu kien sau dung:

- Server version va HTTPS deployment target da khoa.
- Basic Auth hoat dong end-to-end; Bearer chi duoc bat neu gateway da xac minh.
- Project/directory scope da xac minh voi hai project.
- REST and SSE schemas, errors va auth headers co fixture da redacted.
- Permission/question co cach list pending va resolve state, hoac scope MVP da duoc dieu chinh ro rang.
- Prompt co correlation rule du de khong tu dong xac nhan bang content/idle.
- Snapshot/event race, delete va reconnect co fixture de lam test regression sau nay.
- `go-no-go.md` ket luan GO va nêu owner cho moi risk con lai.

### No-go / Blocked

Khong bat dau Phase 1 neu bat ky dieu kien nao sau day xay ra ma chua co quyet dinh bang van ban:

- Khong co HTTPS path hop le cho release.
- Basic Auth khong ket noi duoc OpenCode server chuan.
- MVP yeu cau Bearer nhung gateway khong ho tro REST va SSE.
- Khong the phan biet project/directory cho mot server nhieu project.
- Khong co API/event de phuc hoi permission/question pending sau offline.
- Khong co evidence de correlation prompt nhung van yeu cau tu dong chuyen CONFIRMED.
- Fixture chua duoc redacted hoac co nguy co chua secret/noi dung nhay cam.

## 10. Rollback

Spike khong sua Android app va khong thay doi server production. Rollback la:

1. Dung server test/probe va xoa credential tam thoi khoi may chay probe.
2. Xoa artifact local co secret neu phat hien truoc khi commit; thay fixture bang ban da redacted.
3. Ghi BLOCKED trong `go-no-go.md`; khong tao implementation workaround lam thay doi contract server.

## 11. Ket Qua Ban Giao

- Toan bo artifact o muc 6, da redacted va co manifest.
- Go/no-go verdict, server version, auth mode duoc phep, scope va correlation rule.
- Danh sach capability BLOCKED/NOT_APPLICABLE co tac dong den WI-01 den WI-23.
- Danh sach fixture du de viet unit/network/regression tests cho cac phase sau.
