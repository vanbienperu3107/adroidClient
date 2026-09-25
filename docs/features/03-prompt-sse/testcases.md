# Testcases Feature 03 - tat ca NOT_RUN

Tien dieu kien chung: server disposable dung version/auth target; hai directory khac nhau A/B tren cung server; session disposable A; desktop client thu hai; test proxy co the delay/cut response; transport SSE fake co the inject frame; Android device/emulator cho native/process kill. Tat ca artifact redact Authorization, credential, host/IP private, prompt body va raw event. Moi ket qua ghi ID, SHA, precondition thuc te, steps, expected/actual, evidence path va lop evidence. `NOT_RUN`/`BLOCKED` khong phai PASS.

| ID / requirements | Precondition | Steps | Expected | Evidence class | Status |
| --- | --- | --- | --- | --- | --- |
| T03-01 / R03-01,R03-07 | G03-03 PASS; fake SSE va A/B | Mo A; doi directory, server revision, session, background va clear ViewModel; phat event/callback cu | Toi da 1 stream; stream cu cancel truoc stream moi; event generation cu khong ghi state moi | Unit coordinator + transport integration + native lifecycle | NOT_RUN; BLOCKED G03-03 |
| T03-02 / R03-02 | Fixture frame valid, unknown, JSON loi, truncated, oversized | Feed tung frame va heartbeat; quan sat parser/reducer/log | Valid envelope dung scope dispatch; unknown/malformed bi bo qua/dirty an toan; khong crash/raw payload log | Parser unit + sanitized log audit | NOT_RUN |
| T03-03 / R03-03,R03-04 | Owned session A, pre-history, POST harness 204 | Tao clientMessageId/snapshot; send prompt; tra 204; phat history/SSE co dung messageID | Persist PENDING truoc HTTP, SENDING truoc call, ACCEPTED chi sau 204; gui client ID trong `messageID` de correlation, khong coi la idempotency va khong retry | Repository unit + HTTP request assertion + live disposable integration | NOT_RUN |
| T03-04 / R03-03,R03-04,R03-06 | SSE/history fixture voi duplicate, reverse order | Cho event message den truoc/sau 204, lap event; REST history sau do | CONFIRMED mot lan khi evidence server duy nhat; late 204 khong ha state; duplicate khong duplicate UI/cache | Reducer unit + Room integration | NOT_RUN |
| T03-05 / R03-04,R03-06 | Desktop va Android fake cung gui text giong nhau | Snapshot; gui gan nhau; capture SSE/history/idle; reconcile | Khong confirm bang text/timestamp/idle; pending Android la UNKNOWN neu khong phan biet duoc | Concurrent live integration + decision UI native | NOT_RUN |
| T03-06 / R03-03,R03-06 | Proxy forward request roi timeout/cancel/mat mang | Forward prompt, cat 204; reconnect va history reconcile | UNKNOWN cho den evidence duy nhat; khong auto retry/no second POST | Transport fault integration + network trace redact | NOT_RUN |
| T03-07 / R03-05 | Agent dang chay o session owned A; abort fixture | Send abort; delay status/history; kiem tra partial assistant va error; thu A/B | Abort mot lan dung scope; HTTP 200 khong coi idle; refresh status/history; retain partial va MessageAbortedError; B khong bi mutate | HTTP integration + repository/Room + native UI | NOT_RUN |
| T03-08 / R03-06,R03-07 | Persisted state tai cac moc PENDING/SENDING/ACCEPTED | Kill process truoc call, dang call, sau 204 truoc event; launch; reconcile | PENDING/SENDING -> UNKNOWN va khong POST; ACCEPTED reconcile -> CONFIRMED/UNKNOWN; khong FAILED chi mot read miss | Instrumented process-death + DB inspection | NOT_RUN; BLOCKED G03-05 |
| T03-09 / R03-07 | A/B, profile revision swap, delayed HTTP/SSE | Bat dau send/stream A; doi directory/profile; release callback cu; inspect cache/UI | Ownership/revision/generation recheck chan cross-scope write; directory case/Unicode giu nguyen; secret khong lo | Barrier unit + Room integration + security audit | NOT_RUN |
| T03-10 / R03-01..07 | WI-03 implementation va disposable live server | Native flow: mo session, send, abort, background/foreground, desktop concurrent update, restart | UI state trung thuc, no auto retry, no duplicate, history cuoi cung khop server trong pham vi co ban | Native UI/E2E + screenshot/video + sanitized server evidence | NOT_RUN; BLOCKED G03-03,G03-05 |

## Coverage map

- Happy path: T03-01, T03-03, T03-04, T03-07, T03-10.
- Negative/boundary: T03-02, T03-03, T03-06, T03-07, T03-09.
- Concurrency/out-of-order: T03-04, T03-05, T03-09.
- Restart/recovery: T03-08, T03-10.
- Security/directory/auth: T03-01, T03-03, T03-07, T03-09.

Feature 04 se them testcase reconnect/backoff, snapshot-event overlap va delta. T03-10 khong duoc dung lam bang chung cho cac capability do.
