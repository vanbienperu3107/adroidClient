# Feature 07 - Approved UI Implementation

## Approved prototype

The approved immutable preview is `https://view.hangocthanh.io.vn/preview-36313472476-20814/`.

## Scope

| ID | Input | Output | Files/modules | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- |
| UI-06 | Approved Chats screens | Search, top five OpenCode sessions and top five direct chats, See all views, row-style New chat | Home screen, HomeViewModel, navigation | Existing history stores | Search filters titles; home has at most 5 rows per category; full lists remain reachable; opening a row preserves the existing destination. |
| UI-07 | Approved Settings screen | Persisted OpenCode/direct default destination | SettingDataSource, SettingRepository, Settings, Home | DataStore | New preference defaults to OpenCode; all existing Settings rows and screens remain available; Direct falls back to the existing provider picker if no sole provider is enabled. |
| UI-08 | Approved conversation screens | Safe image, Mermaid and HTML artifacts | Direct bubble, OpenCode browse, rich-content parser/renderer | Existing stored text/image data | Mermaid/HTML code fences show a labelled render and source; images accept only data URI or HTTPS; HTML cannot execute script or access network/files/content. |
| UI-09 | New behavior | Automated evidence and review records | JVM tests and feature docs | UI-06 to UI-08 | Parser/policy/default tests pass; build, lint and unit tests pass; post-diff review records evidence and limits. |

## Impact and rollback

The changes do not alter ChatRoom, Message, OpenCode cache schemas or server contracts. Existing direct chats remain ordered by the existing DAO and continue to open the existing ChatScreen. Existing OpenCode session fetch, reconciliation and authorization behavior remain unchanged.

Rollback is a normal app update that stops consuming the new DataStore key. The default key is additive and safe to retain; no user data conversion is required.

## Test mapping

| Test ID | Preconditions | Steps | Expected evidence |
| --- | --- | --- | --- |
| UI-T08 | JVM test | Filter mixed direct/OpenCode history | Case-insensitive title matching and 5-item limit are deterministic. |
| UI-T09 | JVM test | Read/write default destination | Missing/invalid value resolves to OpenCode; a valid saved value round-trips. |
| UI-T10 | JVM test | Parse text with fenced Mermaid/HTML and image links | Only supported fenced blocks and safe image schemes become artifacts; unsupported/unsafe input stays text. |
| UI-T11 | JVM test | Inspect HTML WebView policy helper | JavaScript, network, file and content access are disabled. |
| UI-T12 | Local build | Run ktlint, unit test and compile | All checks pass on the implementation revision. |
| UI-T13 | Android device/emulator | Search, See all, Settings default and artifact rendering | NOT_RUN until a device/emulator exists; browser evidence for approved prototype does not substitute native evidence. |

## Pre-implementation review

### Normal - PASS

The approved prototype requirements map one-to-one to UI-06 through UI-09. Existing Settings are explicitly retained rather than redesigned out of the application.

### High - PASS

History limits are presentation-only, leaving repository ordering and direct chat deletion untouched. The new setting is an additive DataStore value. OpenCode session navigation retains server, directory and session scope, while direct chat keeps the current ChatRoom route.

### XHigh - PASS

No new server payload type is trusted. Rich content is derived solely from the existing text parts accepted by the decoder. HTML is sandboxed without JavaScript, network, file or content access. Remote images are restricted to HTTPS and all unsupported/unsafe content falls back to selectable text. Native WebView/device evidence is correctly tracked as NOT_RUN, not PASS.
