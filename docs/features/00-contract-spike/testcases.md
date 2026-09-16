# Test Cases: 00 Contract Spike

**Nguồn:** [plan chi tiet](plan.md), [plan 1.4](../../plan.md), [backlog](../../backlog.md)

**Pham vi:** WI-00a den WI-00h. Day la contract/integration test voi OpenCode server test, khong phai Android unit test hoac instrumented test.

**Quy uoc ket qua:** `PASS`, `FAIL`, `BLOCKED`, `NOT_APPLICABLE`.

## 1. Quy Tac Chung

### 1.1 Moi truong chung

- Server test dung cung version va auth mode voi server cong ty; neu khac version, danh dau `BLOCKED` cho go/no-go production.
- Co hai project/directory doc lap: `PROJECT_A` va `PROJECT_B`.
- Moi project co it nhat mot session. `SESSION_A` thuoc `PROJECT_A`; `SESSION_B` thuoc `PROJECT_B`.
- Co mot desktop OpenCode client hoac client thu hai de tao concurrent update, permission/question va thao tac delete.
- Tat ca request production-like dung HTTPS. Khong tat TLS verification.
- Test Bearer chi duoc chay khi gateway bearer da ton tai va duoc cap phep dung.

### 1.2 Quy tac evidence va redaction

- Moi testcase ghi: test ID, server version, auth mode, endpoint/event, result, thoi diem va artifact evidence.
- Khong luu Authorization header, token, password, cookie, private host/IP, tailnet name, raw prompt, message content, tool input/output, diff, file path nhay cam hoac ID co the truy vet he thong that.
- Dung placeholder nhat quan: `<REDACTED_HOST>`, `<REDACTED_TOKEN>`, `<PROJECT_A>`, `<SESSION_A>`, `<MESSAGE_A>`.
- JSON/SSE sau redaction phai van parse hop le.
- Neu redaction chua an toan, testcase la `BLOCKED`; khong duoc luu artifact vao repository.

### 1.3 Template ket qua

```text
ID:
Result: PASS | FAIL | BLOCKED | NOT_APPLICABLE
Server version:
Auth mode:
Executed at:
Evidence:
Redaction reviewed by:
Notes / blocker:
```

## 2. WI-00a: Server, HTTPS va Topology

### TC-00A-01: Khoa OpenCode server version

**Precondition:** Server test dang chay qua HTTPS, co auth profile hop le.

**Steps:**

1. Goi `GET /global/health` bang auth profile hop le.
2. Ghi `healthy`, version va build metadata neu co.
3. Chuan hoa base URL bang placeholder, khong luu host that.
4. So sanh version voi server cong ty.

**Expected:** HTTP 200; version duoc xac dinh ro. Neu version khac server cong ty, ket qua khong duoc dung lam go/no-go production.

**Evidence:** `environment.md`, health fixture da redacted.

### TC-00A-02: REST qua HTTPS/Tailscale

**Precondition:** TC-00A-01 PASS.

**Steps:**

1. Tu thiet bi/emulator tren duong di Android du kien, goi `GET /global/health` qua HTTPS/Tailscale.
2. Xac minh TLS certificate hop le cho hostname.
3. Ghi thoi gian ket noi va HTTP status, khong ghi host/IP.

**Expected:** Ket noi thanh cong ma khong can trust manager tuy chinh hay bypass TLS.

**Evidence:** `environment.md`, fixture health da redacted.

### TC-00A-03: SSE qua HTTPS/Tailscale

**Precondition:** TC-00A-02 PASS; co session activity de tao event.

**Steps:**

1. Mo stream `/event` voi auth va scope duoc du kien.
2. Tao mot thay doi vo hai trong session test.
3. Xac minh stream nhan duoc it nhat mot event hop le.

**Expected:** SSE handshake va event thanh cong qua cung duong HTTPS/Tailscale; khong co redirect lam roi auth header.

**Evidence:** SSE fixture da redacted, `environment.md`.

### TC-00A-04: DNS/Tailscale failure

**Precondition:** Co cach mo phong an toan DNS hoac Tailscale route khong kha dung.

**Steps:**

1. Goi health toi endpoint khong the resolve/route.
2. Ghi loai loi, thoi gian timeout va kha nang phan biet voi 401/403.

**Expected:** Loi network duoc phan biet voi auth/server error; khong co credential trong error artifact.

**Evidence:** error matrix da redacted.

### TC-00A-05: TLS failure

**Precondition:** Co endpoint test certificate sai/het han, khong dung production server.

**Steps:**

1. Goi health qua endpoint TLS khong hop le.
2. Xac minh client probe khong tiep tuc request khi certificate validation that bai.

**Expected:** TLS failure ro rang; khong co option bypass trust de dat PASS.

**Evidence:** error matrix da redacted.

## 3. WI-00b: Authentication

### TC-00B-01: Basic Auth REST thanh cong

**Precondition:** OpenCode chuan bat password; co credential Basic hop le.

**Steps:**

1. Goi `GET /global/health` voi `Authorization: Basic ...`.
2. Goi `GET /session` voi cung auth mode.
3. Redact toan bo header/credential truoc khi luu.

**Expected:** Cac request thanh cong; chot username default/custom va header format.

**Evidence:** `auth-matrix.md`, REST fixtures da redacted.

### TC-00B-02: Basic Auth REST bi tu choi

**Precondition:** Co password sai, khong ghi vao artifact.

**Steps:**

1. Goi health voi password sai.
2. Neu server cho phep username custom, lap lai voi username sai.
3. Ghi HTTP status, WWW-Authenticate va error schema neu co.

**Expected:** 401 hoac contract da ghi nhan; response khong lam lo credential.

**Evidence:** auth/error fixture da redacted.

### TC-00B-03: Basic Auth SSE

**Precondition:** Basic Auth REST thanh cong.

**Steps:**

1. Mo `/event` voi Basic Auth.
2. Tao event tu desktop client.
3. Thu mo stream voi credential sai.

**Expected:** Credential dung nhan event; credential sai bi tu choi truoc khi stream active.

**Evidence:** auth matrix, SSE fixtures da redacted.

### TC-00B-04: Bearer qua gateway thanh cong

**Precondition:** Gateway bearer co san, da duoc phep dung; co token hop le.

**Steps:**

1. Goi health va session qua gateway voi `Authorization: Bearer ...`.
2. Mo SSE qua gateway voi cung header.
3. Tao event de kiem tra header duoc forward cho stream.

**Expected:** REST va SSE deu thanh cong qua gateway; khong can Basic Auth tu Android trong mode Bearer.

**Evidence:** `auth-matrix.md`, fixture da redacted.

### TC-00B-05: Bearer bi tu choi

**Precondition:** TC-00B-04 co the chay.

**Steps:**

1. Goi REST va SSE voi token sai/het han.
2. Ghi 401/403 va error schema.

**Expected:** Gateway tu choi nhat quan ca REST va SSE.

**Evidence:** auth/error fixture da redacted.

### TC-00B-06: Redaction gate cho auth artifact

**Precondition:** Co tat ca artifact auth cua WI-00b.

**Steps:**

1. Kiem tra moi file voi danh sach tu cam: `Authorization`, `Bearer `, `Basic `, cookie, token, password, private host/IP.
2. Kiem tra Base64 Basic payload va URL query secret.
3. Parse lai fixture JSON/SSE sau redaction.

**Expected:** Khong con secret; fixture van hop le. Neu phat hien secret, danh `BLOCKED`, xoa artifact local va tao lai ban da redacted.

**Evidence:** `fixture-manifest.md`.

## 4. WI-00c: REST Contract

### TC-00C-01: Project list va current project

**Precondition:** Auth PASS; co `PROJECT_A` va `PROJECT_B`.

**Steps:**

1. Goi `GET /project`.
2. Goi `GET /project/current` theo scope A va B neu contract cho phep.
3. Ghi ID, directory, pagination va field optional sau redaction.

**Expected:** Chot duoc project identity, directory/workspace field va semantics current project.

**Evidence:** `endpoint-matrix.md`, fixtures project.

### TC-00C-02: Session list/get/status

**Precondition:** Co session A/B.

**Steps:**

1. Goi `GET /session`, `GET /session/{id}`, `GET /session/status` trong scope A.
2. Lap lai trong scope B.
3. Tao busy/retry/idle neu server cho phep.
4. Ghi status types va behavior pagination/full list.

**Expected:** Chot duoc session schema, status enum va scope query bat buoc.

**Evidence:** fixtures session/status.

### TC-00C-03: Session rename va delete

**Precondition:** Co disposable session test.

**Steps:**

1. Goi `PATCH /session/{id}` voi title gia.
2. Xac minh bang GET list/get va SSE neu co.
3. Goi `DELETE /session/{id}`.
4. Xac minh GET sau delete va list snapshot.

**Expected:** Payload rename, response va delete semantics duoc chot; xac dinh co soft-delete/archive hay hard delete.

**Evidence:** session CRUD fixture da redacted.

### TC-00C-04: Message history va part schema

**Precondition:** Session test co user/assistant message va nhieu part neu co.

**Steps:**

1. Goi `GET /session/{id}/message`.
2. Goi `GET /session/{id}/message/{messageID}`.
3. Ghi stable IDs, part type, content field, ordering, pagination va field optional.
4. Xoa mot message/part tu desktop neu API cho phep va tai lai history.

**Expected:** Chot snapshot co day du hay phan trang; co bang chung xoa record de implement reconcile.

**Evidence:** message fixtures da redacted.

### TC-00C-05: Diff contract

**Precondition:** Session disposable co thay doi file; khong dung file nhay cam.

**Steps:**

1. Goi `GET /session/{id}/diff` khi khong co diff.
2. Goi lai khi co diff nho.
3. Tao hoac mo phong diff lon de ghi schema/size behavior, sau do redact content.

**Expected:** Chot empty response, diff schema, scope va cach nhan biet payload lon.

**Evidence:** diff fixtures da redacted.

### TC-00C-06: Error matrix REST

**Precondition:** Co target test khong ton tai va request duoc phep fail.

**Steps:**

1. Thu session/message khong ton tai.
2. Thu request bi forbidden neu co policy test.
3. Thu 5xx tu server test hoac test double.
4. Thu request timeout.

**Expected:** Ghi duoc status/body/schema de map 401, 403, 404, 5xx, timeout; khong danh dong nhat moi loi la Unauthorized.

**Evidence:** error matrix da redacted.

## 5. WI-00d: Project va Directory Scope

### TC-00D-01: Session list tach biet A/B

**Precondition:** `SESSION_A` va `SESSION_B` thuoc hai project khac nhau.

**Steps:**

1. List session trong scope A.
2. List session trong scope B.
3. So sanh ket qua voi session da tao.

**Expected:** Scope A khong hien session chi thuoc B va nguoc lai, tru khi server contract ghi ro session la global; trong truong hop global, phai co field project/directory de filter an toan.

**Evidence:** `project-scope-matrix.md`.

### TC-00D-02: Cross-scope session access

**Precondition:** TC-00D-01 PASS.

**Steps:**

1. Goi get/message/prompt/diff cua `SESSION_A` trong scope B.
2. Ghi ket qua cho tung endpoint.

**Expected:** Chot behavior 404, empty, forbidden hoac global access. MVP chi GO neu app co cach ngan context nham theo contract.

**Evidence:** project scope matrix, error fixtures.

### TC-00D-03: SSE scope A/B

**Precondition:** Co hai client SSE hoac test lap lai tuan tu.

**Steps:**

1. Mo SSE scope A, tao event chi cho session A va sau do session B.
2. Lap lai voi SSE scope B.
3. Ghi event co/khong co directory/project field.

**Expected:** Chot stream global hay scoped va quy tac client filter. Event context khong duoc suy ra bang session ID don le.

**Evidence:** SSE event catalog va project scope matrix.

## 6. WI-00e: Permission va Question

### TC-00E-01: Permission ask va pending list

**Precondition:** Co cach tao permission request an toan tren session test.

**Steps:**

1. Tao permission request tu desktop/session test.
2. Capture `permission.asked`.
3. Goi API list pending request theo scope.
4. So sanh ID, session, pattern, metadata an toan va resolved state.

**Expected:** Android co the phuc hoi permission pending neu offline; chot pagination va scope.

**Evidence:** permission REST/SSE fixture da redacted.

### TC-00E-02: Permission reply

**Precondition:** Permission pending tu TC-00E-01.

**Steps:**

1. Gui moi valid reply server ho tro: once, always, reject hoac gia tri version-specific.
2. Capture response va `permission.replied` neu co.
3. Refresh pending list.

**Expected:** Request resolved bien mat khoi pending hoac co resolved marker; reply payload duoc chot.

**Evidence:** permission fixture, endpoint matrix.

### TC-00E-03: Permission resolved tu desktop

**Precondition:** Permission dang pending; desktop client truy cap duoc.

**Steps:**

1. Mo pending list tu probe Android.
2. Reply request tu desktop.
3. Refresh pending list va kiem tra SSE.
4. Thu reply lai tu probe.

**Expected:** Android phat hien request het hieu luc; UI sau nay phai dong dialog; khong tu retry reply cu.

**Evidence:** resolved-state fixture da redacted.

### TC-00E-04: Question ask, pending list va reply/reject

**Precondition:** Server co ho tro question.

**Steps:**

1. Tao question request.
2. Capture `question.asked` va tai pending list.
3. Gui reply va reject tren request rieng biet.
4. Capture event resolved va refresh pending list.

**Expected:** Chot API, payload, event va semantics reject. Neu server khong ho tro list pending, danh BLOCKED cho question MVP.

**Evidence:** question fixtures va endpoint matrix.

### TC-00E-05: Pending request khi probe offline

**Precondition:** Co cach dong SSE/probe network ma khong anh huong server.

**Steps:**

1. Ngat probe khoi SSE/network.
2. Tao permission va question tu desktop.
3. Ket noi lai probe.
4. Goi API pending list.

**Expected:** Cac request van duoc tim thay va co the phan biet pending/resolved; neu khong, BLOCKED theo gate WI-00e.

**Evidence:** offline/reconnect fixture da redacted.

## 7. WI-00f: Prompt Correlation va Idempotency

### TC-00F-01: Prompt 204 va message evidence

**Precondition:** Session A ranh; SSE va REST co san.

**Steps:**

1. Ghi snapshot history truoc prompt.
2. Gui `POST /session/{id}/prompt_async` voi payload da duoc phep.
3. Ghi HTTP response; capture event; tai history sau khi session idle.
4. So sanh ID/server evidence cua user message moi va pending request.

**Expected:** Xac dinh chinh xac bang chung can de ACCEPTED -> CONFIRMED; khong dung text/idle don le.

**Evidence:** `prompt-correlation.md`, REST/SSE fixture da redacted.

### TC-00F-02: Client-supplied ID va idempotency

**Precondition:** Contract payload co field ID/correlation hoac da chot khong co.

**Steps:**

1. Gui prompt voi client ID neu API cho phep.
2. Tai message history va event sau request.
3. Kiem tra ID co duoc server echo/persist hay khong.
4. Gui lai cung request chi trong moi truong disposable neu contract cho phep test idempotency.

**Expected:** Chot ro field nao chi la ID va field nao thuc su co idempotency semantics. Khong suy dien idempotency tu viec server chap nhan request.

**Evidence:** correlation report.

### TC-00F-03: Timeout sau khi server nhan prompt

**Precondition:** Co proxy/test harness co the cat response sau khi forward request.

**Steps:**

1. Gui prompt qua harness de server nhan request nhung probe timeout truoc response.
2. Kiem tra SSE/history.
3. Thu reconnect va reconcile.

**Expected:** Neu co evidence ID, chot CONFIRMED; neu khong co, chot UNKNOWN. Khong gui lai request tu dong.

**Evidence:** timeout correlation fixture.

### TC-00F-04: Prompt trung noi dung tu hai client

**Precondition:** Android probe va desktop client co cung session.

**Steps:**

1. Gui hai prompt cung text tai hai thoi diem gan nhau tu hai client.
2. Capture SSE/history/status.
3. Thu correlation tung prompt.

**Expected:** Text, timestamp gan dung hoac `session.idle` khong du de xac nhan prompt. Neu khong co ID/server evidence, ca hai request can duoc xu ly theo UNKNOWN policy.

**Evidence:** concurrent prompt fixture da redacted.

### TC-00F-05: Process death state recovery model

**Precondition:** Co log/probe state cho PENDING, SENDING va ACCEPTED; khong can Android implementation.

**Steps:**

1. Lap bang state cho app bi kill o tung diem: truoc send, dang send, sau 204 truoc event.
2. Mo phong launch lai bang REST/SSE reconcile.
3. Xac dinh state sau restore cho tung truong hop.

**Expected:** PENDING/SENDING khong tu gui; SENDING chuyen UNKNOWN; ACCEPTED reconcile truoc khi quyet dinh; khong tao offline queue.

**Evidence:** prompt correlation report.

## 8. WI-00g: SSE Contract

### TC-00G-01: Event catalog MVP

**Precondition:** Stream SSE authenticated dang active.

**Steps:**

1. Tao cac thao tac de sinh event: session create/update/delete/status/error; message/part update/remove/delta; permission/question; diff; heartbeat; idle.
2. Luu envelope va payload schema da redacted.
3. Ghi ID fields, scope field, ordering quan sat va event co the khong tao duoc.

**Expected:** `sse-event-catalog.md` liet ke event quan sat va event khong ho tro/khong tao duoc co ly do.

**Evidence:** `fixtures/sse/` da redacted.

### TC-00G-02: Unknown event

**Precondition:** Co test double hoac fixture inject unknown event hop le theo SSE framing.

**Steps:**

1. Gui event type khong co trong catalog.
2. Xac dinh parser/reducer policy du kien.

**Expected:** Unknown event duoc bo qua/ghi metric an toan, khong crash va khong lam hong state da co.

**Evidence:** event catalog, unknown event fixture.

### TC-00G-03: Malformed SSE frame

**Precondition:** Co test double co the gui frame loi.

**Steps:**

1. Gui JSON khong hop le, event thieu ID va frame bi cat ngan.
2. Quan sat stream close/reconnect behavior.

**Expected:** Frame loi khong duoc ap dung vao state; policy close/reconnect duoc chot; khong lam lo raw payload nhay cam trong log.

**Evidence:** malformed fixture, error matrix.

### TC-00G-04: Duplicate va out-of-order event

**Precondition:** Co capture thuc te hoac test double chinh xac.

**Steps:**

1. Gui lai cung update/delta hai lan.
2. Gui full update va delta cu theo thu tu dao nguoc.
3. Ghi reducer rule du kien.

**Expected:** Xac dinh duoc ID/version/order signal server cung cap. Neu khong co signal, reducer chi co the dat dirty va full reconcile, khong tu doan thu tu.

**Evidence:** event catalog, reducer notes.

### TC-00G-05: Disconnect va reconnect

**Precondition:** SSE stream active.

**Steps:**

1. Cat stream.
2. Tao update khi probe offline.
3. Mo lai stream va tai REST snapshot.
4. So sanh final state voi server.

**Expected:** Chot duoc event replay co/khong co. Neu khong replay, REST snapshot la nguon phuc hoi; khong duoc dua vao SSE de tu khoi phuc.

**Evidence:** reconnect fixture, event catalog.

### TC-00G-06: Snapshot/event overlap

**Precondition:** Co history lon hoac harness lam cham REST response.

**Steps:**

1. Mo SSE.
2. Bat dau tai REST history snapshot.
3. Trong luc response dang cho, tao message update/delta/delete tu desktop.
4. Hoan thanh snapshot va tai lai history neu dirty.

**Expected:** Final state khop server, khong mat update, khong append delta hai lan. Chot dirty-set/buffer rule cho WI-16.

**Evidence:** race fixture, `sse-event-catalog.md`, `prompt-correlation.md` neu co lien quan.

## 9. WI-00h: Go/No-Go

### TC-00H-01: Fixture inventory va redaction audit

**Precondition:** Tat ca testcase truoc da co evidence.

**Steps:**

1. Liet ke moi fixture trong `fixture-manifest.md`: testcase nguon, server version, auth mode, schema/event va redaction status.
2. Kiem tra artifact theo quy tac 1.2.
3. Parse lai JSON/SSE sau redaction.

**Expected:** Moi fixture co truy vet testcase, da redacted va parse hop le; artifact vi pham bi xoa/thay the truoc review.

**Evidence:** `fixture-manifest.md`.

### TC-00H-02: Capability gate

**Precondition:** TC-00A den TC-00G da co ket qua.

**Steps:**

1. Danh gia: HTTPS, Basic, Bearer (neu ap dung), project scope, REST schema, pending permission/question, correlation prompt, SSE, deletion va snapshot race.
2. Gan `PASS`, `FAIL`, `BLOCKED` hoac `NOT_APPLICABLE` cho tung capability.
3. Lien ket failure/blocker voi WI sau bi anh huong.

**Expected:** Khong co capability bat buoc bi bo qua trong `go-no-go.md`.

**Evidence:** `go-no-go.md`.

### TC-00H-03: Go/no-go cho Phase 1

**Precondition:** TC-00H-02 hoan thanh.

**Steps:**

1. Kiem tra tat ca gate GO trong `plan.md` muc 9.
2. Kiem tra cac blocker: HTTPS, Basic, Bearer gateway neu bat, project scope, pending request recovery, prompt correlation, redaction.
3. Ghi verdict va owner/han xu ly cho risk con lai.

**Expected:**

- `GO`: moi dieu kien bat buoc PASS; risk con lai co owner.
- `NO_GO`: mot capability bat buoc FAIL.
- `BLOCKED`: thieu server/gateway/API hoac fixture an toan de ket luan.

**Evidence:** `go-no-go.md`.

## 10. Traceability

| Work item | Test cases |
| --- | --- |
| WI-00a | TC-00A-01 den TC-00A-05 |
| WI-00b | TC-00B-01 den TC-00B-06 |
| WI-00c | TC-00C-01 den TC-00C-06 |
| WI-00d | TC-00D-01 den TC-00D-03 |
| WI-00e | TC-00E-01 den TC-00E-05 |
| WI-00f | TC-00F-01 den TC-00F-05 |
| WI-00g | TC-00G-01 den TC-00G-06 |
| WI-00h | TC-00H-01 den TC-00H-03 |

## 11. Thuc Thi va Bao Cao

- Chay testcase theo thu tu A -> H. Khong danh GO khi WI-00e hoac WI-00f con BLOCKED.
- Mot testcase chi PASS khi artifact evidence da redacted va duoc ghi trong fixture manifest.
- Khong dua output thuc co secret vao issue, PR, GitHub Actions log hay repository.
- Bao cao cuoi cung phai co server version, auth modes da xac minh, ket qua theo test ID, artifact path, blocker va verdict GO/NO_GO/BLOCKED.
