# Prompt Correlation Rule (OpenCode 1.18.30)

## Evidence tu spike

1. `POST /session/{id}/prompt_async` voi body `{"parts":[{"type":"text","text":"..."}]}` tra **204 No Content** — khong co message ID trong response.
2. Ngay sau do, SSE emit `message.updated` voi user message `info.id` (`msg_...`) va `message.part.updated` voi part `id` (`prt_...`) — ID do **server sinh**.
3. `GET /session/{id}/message` tra user message + assistant message voi ID va `parentID` (assistant tro ve user message id).
4. Schema payload da thu **khong co field client-supplied messageID** cho `prompt_async`; API v1 SDK co `messageID` optional trong prompt payload nhung spike chua xac minh server echo no — de o OPEN.

## Correlation rule cho implementation (WI-13)

| Buoc | Trang thai | Dieu kien |
| --- | --- | --- |
| Gui request | `SENDING` | — |
| Nhan 204 | `ACCEPTED` | HTTP 204 tu prompt_async |
| Xac nhan | `CONFIRMED` | Tim thay user message MOI trong session (qua SSE `message.updated` hoac REST history) thoa: (a) `role=user`, (b) `time.created` >= thoi diem gui, (c) text part khop noi dung da gui, (d) KHONG ton tai truoc snapshot pre-send |
| Khong ro | `UNKNOWN` | Timeout truoc 204, hoac sau reconcile khong phan biet duoc voi message cung noi dung tu client khac |

**Bat buoc:** luu snapshot danh sach message ID truoc khi gui (pre-send snapshot). CONFIRMED chi khi message moi xuat hien so voi snapshot nay. Khong dung `session.idle` lam evidence.

**Han che da biet:** neu desktop gui cung noi dung trong cung khoang thoi gian, rule (c) co the match nham → giu UNKNOWN va yeu cau nguoi dung quyet dinh (dung chinh sach plan 1.4 muc 9).

**Idempotency:** chua co evidence server ho tro idempotency key cho prompt_async → KHONG retry tu dong trong moi truong hop.

## Viec con lai truoc khi code WI-13

- Xac minh field `messageID` trong prompt payload co duoc server chap nhan/echo khong (server cong ty, version khoa).
- Capture `message.part.delta` voi assistant response hoan chinh (khong abort) de chot schema delta.
