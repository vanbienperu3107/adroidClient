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
| UI-T08 | JVM unit environment | Filter mixed direct/OpenCode history | Case-insensitive title matching and 5-item presentation limit are deterministic | PASS local |
| UI-T09 | JVM unit environment | Read/write default destination | Missing/invalid value resolves to OpenCode; a valid saved value round-trips | PASS local |
| UI-T10 | JVM unit environment | Parse Mermaid/HTML fences and image links | Only supported fences and safe `https`/data image schemes become artifacts | PASS local |
| UI-T11 | JVM unit environment | Inspect HTML WebView policy helper | JavaScript, network, file and content access are disabled | PASS local |
| UI-T12 | Local build | Run lint, unit test, debug compile and debug APK build | KtLint, 54 unit tests, Kotlin compilation and debug APK PASS locally with a 512 MB Gradle heap | PASS local |
| UI-T13 | Device or emulator | Search, See all, default route and artifact rendering | Native UI follows approved preview; HTML cannot execute or load network content | NOT_RUN no device/emulator |
