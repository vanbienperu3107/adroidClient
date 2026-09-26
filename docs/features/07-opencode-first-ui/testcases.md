# Feature 07 Testcases

| ID | Precondition | Steps | Expected | Status |
| --- | --- | --- | --- | --- |
| UI-T01 | Fresh install | Select OpenCode in onboarding | Routes to OpenCode server configuration | NOT_RUN native UI |
| UI-T02 | Valid OpenCode profile | Open home | Current project sessions appear before direct chats | NOT_RUN native UI |
| UI-T03 | Multiple projects | Change project then open a session | Directory/session scope remains isolated | NOT_RUN native UI |
| UI-T04 | Model catalog supports variants | Select model and level | Exact provider/model/variant is sent for next server turn | NOT_RUN live server |
| UI-T05 | Cliproxy direct configured | Load models and create direct chat | Dynamic model list and direct stream use vault key only | NOT_RUN native/live |
| UI-T06 | Invalid Cliproxy credential | Load catalog/send prompt | Sanitized auth error; no URL/key/body leak | NOT_RUN native/live |
| UI-T07 | Unit environment | Run `:app:testDebugUnitTest` | All unit tests pass | PASS local |
