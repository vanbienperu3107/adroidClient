# Fixture Manifest

**Server:** OpenCode 1.18.30, local `opencode serve`, Basic Auth.
**Executed:** 2026-09-16. **Redaction:** da kiem tra theo testcases.md muc 1.2.

| Fixture | Testcase nguon | Noi dung | Redaction |
| --- | --- | --- | --- |
| `fixtures/rest/health.json` | TC-00A-01, TC-00B-01 | Health 200 + version | OK — khong co secret |
| `fixtures/rest/session-create.json` | TC-00C-02 | POST /session schema | OK — session ID/slug/directory thay placeholder |
| `fixtures/rest/session-status-busy.json` | TC-00C-02, TC-00F-01 | Status map busy/idle semantics | OK |
| `fixtures/rest/message-history-after-abort.json` | TC-00C-04, TC-00F-01 | Message/part schema + abort error | OK — content/model/provider redacted |
| `fixtures/rest/errors.json` | TC-00C-06, TC-00B-02 | 404/401/unknown-path matrix | OK |
| `fixtures/sse/event-stream-sample.txt` | TC-00G-01 | Event envelope + ordering thuc te | OK — ID/content redacted, giu cau truc |

## Kiem tra redaction da thuc hien

- Khong co `Authorization`, password, token, cookie trong fixture.
- Session/message/part ID thay bang ID gia nhat quan (REDACTED*).
- Prompt text, model/provider name, slug thay placeholder.
- Directory thay `<PROJECT_A>`/`<PROJECT_B>`.
- JSON parse hop le sau redaction (kiem tra bang python3 json.load).
