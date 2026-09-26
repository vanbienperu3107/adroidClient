# Impact Feature 03 - Prompt va SSE co ban

Baseline da review: `08b9bc21d392f045114a180611e7760d33bcbe20` trong worktree `feature/03-prompt-sse`. Codebase MCP xac nhan `OpenCodeBrowseRepository.history()` duoc goi tu `OpenCodeBrowseViewModel.refresh()` va `NavigationGraph`; history hien tai xac minh ownership directory truoc doc va persist message/part cache. `OpenCodeReadApi` hien chi nhan `200 application/json`, khong phu hop de tai su dung nguyen trang cho `204` prompt hoac `text/event-stream` SSE.

| Vung anh huong | Tac dong/risks | Regression can kiem | Rollback |
| --- | --- | --- | --- |
| OpenCode transport va DI | Them POST 204/SSE co auth; nham Accept/content-type, redirect hay retry co the lam mat/hay lap side effect | Prompt chi 1 POST; 204 map ACCEPTED; malformed SSE khong crash; no retry; cancel dong call | Rut binding transport/SSE va route Feature 03; khong sua provider transport |
| Profile/vault/auth | Stream song lau co credential, profile revision co the doi khi callback den | 401/403 dong stream/khoa thao tac; credential khong vao URL/log/raw event; revision cu khong write | Dong stream, purge runtime scope; khong xoa vault tru khi Feature 01 policy xac dinh permanent failure |
| Directory/session ownership | Endpoint co the tra ID cross-directory; SSE co the global hoac thieu directory | Prompt/abort/event cua A khong ap dung B du trung session ID; khong normalize case path remote | Bo reducer/action scope moi; cache scope cu giu nguyen va chi refresh an toan |
| History/cache Feature 02 | Optimistic/pending state co the bi history overwrite, duplicate event co the duplicate part | Server history van source truth; pending local tach server row; partial aborted assistant duoc giu; late callback khong revive state | Xoa chi pending runtime/cache migration Feature 03 theo scope sau khi user/contract cho phep, rehydrate history REST |
| OpenCode Browse ViewModel/navigation | Hien dang cancellation/generation cho history; chat prompt them lifecycle, send/abort UI | Back/rotate/scope switch dong stream; route chi ID, khong prompt body/secret; state UI khong double-send | Remove UI action/them state holder, quay lai history read-only |
| Process death/backup | Persist SENDING co nguy co replay; prompt content/ID co the vao backup | Restart PENDING/SENDING -> UNKNOWN, khong request; backup/D2D exclude pending/body/raw event | Disable/clear pending recovery code, khong xoa server history hay credential |
| Desktop concurrent client | Cung text va event thu tu khac nhau lam confirm nham | Text/idle khong confirm; ambiguity UNKNOWN; event/HTTP out-of-order idempotent | Giu UNKNOWN va buoc user quyet dinh, khong automatic resend |
| Feature 04 reliable sync | Feature 03 co SSE toi thieu nhung chua co reconnect/dirty-set | Disconnect khong tu backoff/replay; UI trung thuc disconnected va REST refresh explicit | Dong stream; Feature 04 sau nay thay coordinator sau review contract |
| Existing providers/chat | Plan tong the cam sua `ChatRepositoryImpl`, `ChatViewModel`, `ChatScreen` | Anthropic/OpenAI/Google/Ollama completion va ChatDatabase khong doi | No source sharing; revert bounded-context change rieng |
| CI/testing | Can fake transport, Room, device/live SSE va process kill; unit xanh khong du | Evidence dung SHA, lop test va redaction; live test mutates chi disposable session | Mark NOT_RUN/BLOCKED, khong coi CI baseline la pass |

## Boundary va evidence

- `OpenCodeBrowseRepository.history()` hien recheck `owned(profile, directory, session)` truoc GET history; Feature 03 phai ap dung cung hoac manh hon cho POST prompt/abort va reducer event.
- `OpenCodeHistoryDecoder.decode()` reject foreign session/part ownership va duplicate IDs; SSE reducer khong duoc bo qua cac invariant nay khi upsert.
- `OpenCodeSessionSync.refresh()` da dung generation `(serverId, directory)` cho read sync. Feature 03 can mo rong identity voi `profileRevision` va session/prompt generation, khong gia dinh guard nay bao phu SSE/prompt.
- Docs la scope excluded theo Codebase MCP; cac ket luan docs spike duoc kiem direct source. Source paths tren co coverage `no_recorded_issue`; do la best-effort, khong phai bang chung day du.

Khong co migration, auth scheme moi, navigation onboarding moi, hay thay doi public server contract nao duoc phep tu tai lieu nay. Moi thay doi schema cache/pending phai co migration/recovery test truoc implementation.
