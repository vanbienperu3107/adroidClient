# Feature 07 Review - Normal -> High -> XHigh

## Normal - PASS

OpenCode onboarding, split home, default project routing, session conversation, model/variant controls and direct Cliproxy are explicitly mapped.

## High - PASS

OpenCode and Cliproxy have separate credential boundaries. Legacy direct chats remain persisted through existing ChatRoom data and are not deleted by the new home section.

## XHigh - PASS with gaps

Model/variant catalog is scoped by OpenCode directory. Cliproxy key is not stored in preferences or navigation state. Native device and live server behavior remain `NOT_RUN`; they are not release PASS evidence.

## Implementation review - approved UI expansion

Revision reviewed: working tree based on `83e03ac` before commit.

### Normal - PASS

The home screen now has the approved row-style New chat action, title search, a five-item presentation cap for each history category and See all dialogs. The new DataStore-backed OpenCode/direct destination selector appears before, and does not replace, Theme, four legacy platform entries, Cliproxy, OpenCode servers or About. OpenCode without a configured profile routes New chat to server setup rather than silently creating a direct chat.

### High - PASS

Direct history still originates from the existing newest-first `ChatRoomDao` list. Filtering retains the source chat ID, and selection maps back through that ID before delete/open actions. OpenCode navigation continues to encode server ID, case-sensitive directory and session ID in the existing route; no scope or credential is placed in the new preference. The change is additive: no Room entity, cache schema, server API or credential storage behavior changed.

### XHigh - PASS with gaps

Only text/reasoning parts already accepted by `OpenCodeHistoryDecoder` can become artifacts. Mermaid is rendered by a bounded native text-only preview. HTML is loaded in a WebView with JavaScript, window opening, network loads/images, file access, content access and DOM storage disabled, and every navigation is intercepted. Images are restricted to validated `https` URLs or bounded supported base64 data URI schemes. Tests cover the parser, disallowed schemes and sandbox policy. Runtime native UI/WebView behavior, Keystore behavior and live OpenCode/Cliproxy artifact payloads remain `NOT_RUN` because no device/emulator or controlled live credentials are available.

Evidence: `git diff --check`, KtLint on `app/src/main/kotlin` and `app/src/test/kotlin`, `:app:testDebugUnitTest` (54 tests), `:app:compileDebugKotlin`, and `:app:assembleDebug` all PASS locally with JDK 17, Android SDK and `-Xmx512m`.
