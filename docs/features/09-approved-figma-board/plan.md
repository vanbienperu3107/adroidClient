# Feature 09 - Approved Full Figma Board

## Approved source

- Figma board: `https://www.figma.com/design/X5IJaFPvFc5O6DjZqEpfSM/GPT-Mobile---OpenCode-Conversation-Redesign?node-id=0-1`.
- Approved interactive prototype: `https://view.hangocthanh.io.vn/preview-36492379803-367/`.
- Primary fidelity nodes: Chats `45:2`, OpenCode chat `39:2`, Settings hub `49:2`.

## Required behavior

- The neutral white/black/gray board system replaces the green Material presentation.
- `Connect OpenCode -> Add server -> Save server -> Chats` is the setup completion path.
- Opening server edit from Settings returns to Settings after save.
- Chats separates server-backed OpenCode rows from direct/local rows.
- OpenCode chat shows loading/cache state, user-right/AI-left timeline blocks, tools/artifacts and composer model/reasoning controls.
- Both model and server reasoning level are interactive bottom sheets.

## Test mapping

| ID | Layer | Expected evidence |
| --- | --- | --- |
| F09-T01 | JVM | Setup edit route encodes `returnToChats=true`; settings edit encodes `false`. |
| F09-T02 | API 34 Compose | Board Chats rows route OpenCode scope and Direct local chat; New chat choice works. |
| F09-T03 | API 34 Compose | Board Settings exposes Theme, four provider screens, Cliproxy, OpenCode server and About. |
| F09-T04 | API 34 Compose | Model picker search/selection and reasoning picker default/variant selection work. |
| F09-T05 | API 34 Room | Cache history retains server position order. |
| F09-T06 | API 34 live read-only | Project/session/history GET path succeeds without a prompt or mutation. |
| F09-T07 | Visual | API 34 screenshots at 390dp are inspected against the approved prototype for Chats, OpenCode chat and Settings. |

## Evidence state

| ID | Status | Evidence |
| --- | --- | --- |
| F09-T01 | PASS local | `OpenCodeSetupNavigationTest`. |
| F09-T02 | PENDING KVM | `ApprovedUiUserJourneyInstrumentedTest` is now included in the offline emulator workflow. |
| F09-T03 | PENDING KVM | `ApprovedUiUserJourneyInstrumentedTest`. |
| F09-T04 | PENDING KVM | `OpenCodeModelPickerInstrumentedTest` covers model search plus reasoning default/xhigh. |
| F09-T05 | PENDING KVM | `OpenCodeCacheInstrumentedTest`. |
| F09-T06 | PENDING KVM live | `LiveOpenCodeInstrumentedTest`. |
| F09-T07 | PENDING KVM screenshot | Requires exact-sha emulator screenshot capture/review before release. |
