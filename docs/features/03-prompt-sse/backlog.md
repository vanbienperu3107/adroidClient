# Backlog Feature 03 - Prompt va SSE co ban

Tat ca item la `NOT_STARTED`; chi sua cac module du kien neu va khi duoc giao implementation. `BLOCKED` dependency phai duoc giai quyet truoc khi bat dau item phu thuoc, khong duoc thay bang suy doan.

| ID | Input | Output | Scope file/module du kien | Dependencies | Acceptance | Status |
| --- | --- | --- | --- | --- | --- | --- |
| WI-03-01 | Profile credential vault, `ActiveScope`, SSE fixture da redact | SSE transport mot stream, parser envelope va lifecycle close/cancel | `data/opencode/*Sse*`, `data/opencode/OpenCodeReadApi.kt` hoac transport rieng, DI | Feature 01; G03-03 | Accept `text/event-stream`, auth khong log, event malformed/unknown khong crash; stream chi active scope va dong truoc scope switch/background | NOT_STARTED; BLOCKED G03-03 |
| WI-03-02 | Session ownership validator, active scope, prompt fixture 204 | Prompt request transport va model `PendingPrompt` co `clientMessageId` | `data/opencode/*Prompt*`, domain model, Room cache migration neu can | WI-03-01, G03-01, G03-02 | Pre-send snapshot va ID persist truoc HTTP; POST dung directory/session owned va gui ID trong `messageID`; 204 -> ACCEPTED; no auto retry va khong coi ID nhu idempotency | NOT_STARTED |
| WI-03-03 | Pending prompt, SSE message.updated/part.updated, REST history | Reducer/reconcile correlation va state transitions | `data/opencode/*Prompt*`, `OpenCodeBrowseRepository.kt`, cache DAO | WI-03-02, Feature 02 history | Chi CONFIRMED khi history/SSE co dung `clientMessageId`; event/204 out-of-order idempotent; idle/text don le khong confirm; khong co ID sau uncertainty -> UNKNOWN | NOT_STARTED |
| WI-03-04 | Persisted pending records va history/status read | Startup recovery va UI UNKNOWN decision | repository/coordinator, ViewModel/Compose OpenCode chat | WI-03-03 | PENDING/SENDING sau kill khong auto-send; ACCEPTED reconcile; resend chi thao tac user moi co canh bao duplicate va ID local moi | NOT_STARTED |
| WI-03-05 | Active owned session va abort contract | Abort action/state, history/status refresh | prompt repository, OpenCode chat UI | WI-03-02, G03-01 | Revalidate directory ownership; mot abort in-flight/session; khong retry; 200 chi refresh; retain partial/MessageAbortedError | NOT_STARTED |
| WI-03-06 | WI-03-01..05 implementation, fake/live fixtures | Unit, transport, Room, native UI, E2E evidence manifest | `app/src/test`, `app/src/androidTest`, test fixtures | WI-03-01..05, G03-05 | Tat ca R03-01..07 co testcase ket qua dung lop; secrets redacted; failures/blockers ghi ro theo SHA | NOT_STARTED; BLOCKED G03-05 |

## Thu tu thuc hien

`WI-03-01 -> WI-03-02 -> WI-03-03 -> WI-03-04/WI-03-05 -> WI-03-06`.

Feature 04 so huu reconnect/backoff, durable reconcile, snapshot/SSE overlap dirty-set va delta. Feature 03 chi dat dirty/yeu cau REST refresh khi event vuot contract, khong tu implement co che Feature 04.
