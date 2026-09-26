# Feature 07 Review - Normal -> High -> XHigh

## Normal - PASS

OpenCode onboarding, split home, default project routing, session conversation, model/variant controls and direct Cliproxy are explicitly mapped.

## High - PASS

OpenCode and Cliproxy have separate credential boundaries. Legacy direct chats remain persisted through existing ChatRoom data and are not deleted by the new home section.

## XHigh - PASS with gaps

Model/variant catalog is scoped by OpenCode directory. Cliproxy key is not stored in preferences or navigation state. Native device and live server behavior remain `NOT_RUN`; they are not release PASS evidence.

## Implementation review

Normal: **PASS**. OpenCode appears in onboarding, history is promoted to the home screen, and direct provider chats remain separate.

High: **PASS**. Home navigation carries server, directory and session ID; the default directory resolves `/project/current` before falling back to the project list. Cliproxy configuration uses its own repository and does not reuse the legacy token preference path.

XHigh: **PASS with gaps**. The model picker clears any variant to preserve server default; the reasoning picker sends only server-listed variants. KtLint, `git diff --check` and `:app:testDebugUnitTest` pass locally. Android native UI, Keystore runtime and live Cliproxy/OpenCode action flows remain `NOT_RUN` without an emulator/device and controlled credentials.
