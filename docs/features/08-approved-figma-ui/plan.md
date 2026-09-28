# Feature 08 - Approved Figma UI Implementation

## Design source

Approved review file: `https://www.figma.com/design/X5IJaFPvFc5O6DjZqEpfSM`.

## User model

- Chats is the common home after setup.
- An OpenCode chat row opens its server-backed history using its internal `(serverId, directory, sessionId)` scope.
- A direct chat row opens its existing local `ChatRoom` history.
- Session IDs remain an implementation detail; user-facing labels use chat title, provider/model and project context.
- A project is configured as the server's default project in Settings and is used only when creating a new OpenCode chat.
- New chat presents an explicit OpenCode/direct choice. It is not a persistent global Settings toggle.

## Work items

| ID | Scope | Acceptance |
| --- | --- | --- |
| UI-01 | Chats home and search | Search/settings entry points are visible. OpenCode chats and direct chats render in separate groups, capped at five, with See all. |
| UI-02 | Chat routing | Existing OpenCode rows navigate to OpenCode history; direct rows navigate to local direct history. Loading/stale states name the correct source. |
| UI-03 | OpenCode conversation | Selected chat title is the header; user turns are right-aligned and OpenCode turns left-aligned. Tools, interactions and artifacts are separate timeline blocks. |
| UI-04 | Settings detail flows | Keep Theme, all legacy providers, Cliproxy, OpenCode server management, About and License. Remove the global route selector. |
| UI-05 | Server profile management | Reject duplicate canonical HTTPS URLs before vault writes. Expose project/edit/delete through a narrow-screen-safe action menu. Persist a server default project. |
| UI-06 | Cache correctness | Read cached OpenCode messages using stored server position, not message ID. Surface stale state without treating a partial page as deletion. |
| UI-07 | Test and release | Map every acceptance item to JVM or API 34 KVM emulator evidence. Publish a signed prerelease only after required CI checks pass. |

## Test mapping

| ID | Layer | Expected evidence |
| --- | --- | --- |
| F08-T01 | JVM | Canonical duplicate server URL is rejected before a new vault credential is written. |
| F08-T02 | Android Room emulator | Cached message read order follows `position` even when message IDs sort differently. |
| F08-T03 | KVM UI | Chats exposes search and Settings; route controls are visible and stable on 390dp width. |
| F08-T04 | KVM live read-only | A selected OpenCode chat loads history using its server scope; no prompt/mutation is sent. |
| F08-T05 | KVM UI | Direct chat row opens local direct chat path. |
| F08-T06 | KVM UI | Settings opens each visible settings branch and OpenCode profile actions include Delete. |
| F08-T07 | Release | KtLint, JVM tests, debug APK, KVM instrumentation and signed APK/AAB all pass on the release SHA. |

## Execution record

| ID | Status | Evidence |
| --- | --- | --- |
| F08-T01 | PASS local | `OpenCodeProfileRepositoryTest` rejects a canonical duplicate before a second vault credential is written. |
| F08-T02 | NOT_RUN | Added to `OpenCodeCacheInstrumentedTest`; requires the API 34 KVM run. |
| F08-T03 | NOT_RUN | Requires an API 34 KVM UI journey against the new Chats screen. |
| F08-T04 | NOT_RUN | Requires manual live OpenCode emulator evidence after routing is merged. |
| F08-T05 | NOT_RUN | Requires API 34 KVM UI journey. |
| F08-T06 | NOT_RUN | Requires API 34 KVM UI journey for profile menu/default project/delete. |
| F08-T07 | PARTIAL | KtLint, JVM tests and Kotlin compilation pass locally; debug APK/KVM/CI evidence pending final diff. |

## Constraints

- No password, API key, HTTP authorization header or server response body is captured in tests, reports or release artifacts.
- The existing OpenCode profile/vault scope and server contract stay unchanged except for additive default-project metadata.
- A real device test remains required for physical-device UX/performance evidence; emulator evidence is not substituted for it.
