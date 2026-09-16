# Test Results: Contract Spike Run 1

**Executed:** 2026-09-16 · **Server:** OpenCode 1.18.30 local, Basic Auth · **Revision:** feature/00-contract-spike @ 3b95177

| Test ID | Result | Ghi chu |
| --- | --- | --- |
| TC-00A-01 | **PASS** | Health 200, version 1.18.30 khoa cho spike nay |
| TC-00A-02 | **NOT_RUN** | Loopback HTTP; can HTTPS/Tailscale cong ty |
| TC-00A-03 | **PASS mot phan** | SSE handshake + event qua loopback; chua qua HTTPS |
| TC-00A-04 | **NOT_RUN** | Chua mo phong DNS/Tailscale failure |
| TC-00A-05 | **NOT_RUN** | Khong co endpoint TLS sai trong moi truong |
| TC-00B-01 | **PASS** | Basic dung: health + session 200 |
| TC-00B-02 | **PASS** | Sai password: 401; khong lo credential |
| TC-00B-03 | **PASS mot phan** | SSE voi Basic dung nhan event; chua thu stream voi credential sai |
| TC-00B-04 | **BLOCKED** | Khong co gateway bearer |
| TC-00B-05 | **BLOCKED** | Nhu tren |
| TC-00B-06 | **PASS** | Scan fixture: khong con Authorization/token/password |
| TC-00C-01 | **PASS** | /project + /project/current?directory= |
| TC-00C-02 | **PASS** | list/get/status; status chi chua non-idle |
| TC-00C-03 | **PASS** | PATCH title 200; DELETE 200 -> GET 404 (hard delete) |
| TC-00C-04 | **PASS mot phan** | Schema message/part + abort error; chua thu xoa message rieng le |
| TC-00C-05 | **PASS mot phan** | Diff empty `[]`; chua tao diff that |
| TC-00C-06 | **PASS mot phan** | 404/401 verified; unknown path = 200 fallback (phat hien quan trong); 5xx/timeout chua mo phong |
| TC-00D-01 | **PASS** | `?directory=` scope session dung |
| TC-00D-02 | **PASS mot phan** | Truy cap truc tiep bang session ID khong bi chan theo directory — client phai tu filter |
| TC-00D-03 | **NOT_RUN** | Chua chay 2 stream A/B song song |
| TC-00E-01..03 | **NOT_RUN** | GET /permission ton tai (`[]`) nhung chua tao duoc permission that trong phien ngan |
| TC-00E-04 | **NOT_RUN** | GET /question ton tai (`[]`); chua tao question that |
| TC-00E-05 | **NOT_RUN** | Can phien offline dai voi desktop client |
| TC-00F-01 | **PASS** | 204 -> SSE message.updated voi server-generated ID -> history co user+assistant |
| TC-00F-02 | **OPEN** | Chua xac minh client-supplied messageID; khong co idempotency evidence |
| TC-00F-03..04 | **NOT_RUN** | Can harness cat response + desktop client |
| TC-00F-05 | **PASS (model)** | Rule process-death cho vao prompt-correlation.md |
| TC-00G-01 | **PASS mot phan** | 7 event types captured; delta/permission/question/diff chua co |
| TC-00G-02..06 | **NOT_RUN** | Can test double/harness |
| TC-00H-01 | **PASS** | fixture-manifest.md; JSON valid; redaction clean |
| TC-00H-02 | **PASS** | Capability gate trong go-no-go.md |
| TC-00H-03 | **PASS** | Verdict: GO co dieu kien cho Phase 1 |

## Tong ket

- PASS/PASS mot phan: 19 · NOT_RUN: 14 · BLOCKED: 2 · OPEN: 1
- **khong co FAIL**
- Verdict: **GO co dieu kien** — Phase 1 duoc phep; Phase 4-6 cho cac muc NOT_RUN/BLOCKED voi server cong ty.
