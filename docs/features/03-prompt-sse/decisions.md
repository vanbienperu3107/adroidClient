# Feature 03 Decisions

## D03-01 - Client message ID

- **Decision:** Sinh `clientMessageId` mot lan truoc persist, gui no qua field `messageID` cua `prompt_async`, va dung no de correlation voi REST history/SSE.
- **Evidence:** Probe live OpenCode 1.18.30 ghi `POST prompt_async` voi `messageID`, sau do `GET history` tra `info.id` trung ID nay. Gui lai cung ID van duoc `204` va tao part moi.
- **Boundary:** `messageID` khong la idempotency key. Khong auto retry sau timeout, cancel, network loss hay process death. Khong tim message bang text/timestamp khi khong thay ID; giu `UNKNOWN`.
- **Follow-up:** Re-probe server target khi implementation de pin version va schema payload.
