# Android Emulator Validation

## Offline pull-request test

The Ubuntu KVM workflow runs API 34 instrumented tests for Room cache isolation, Android Keystore binding/isolation and the model picker search/selection UI. It uses synthetic data only and does not access a network server.

## Live manual test

The workflow dispatch input `live_opencode=true` starts a separate emulator and reads only `OPEN_CODE_PASSWORD` from repository secrets. The server URL and Basic username are non-secret workflow constants approved for this test.

The live test saves a temporary Android Keystore credential, reads `/project` and the directory-scoped `/api/model` catalog, then removes the temporary credential. It does not create, rename, delete, prompt, abort, subscribe to SSE or modify any OpenCode server state. Credential values, HTTP headers, URLs with query values and response bodies must not be logged or uploaded.

## Evidence boundaries

An emulator provides native Android/Keystore/Compose evidence but does not establish physical-device performance, accessibility or OEM compatibility. The live smoke test proves only health-compatible Basic Auth and read-only project/model access for the configured server at the run time.

## Review

### Normal - PASS

The model picker replaces the unbounded dialog with the approved searchable sheet pattern. The offline instrumentation set covers the existing cache/vault tests plus model search and selection. The workflow target is Ubuntu API 34 with KVM.

### High - PASS

The model picker preserves server-provided provider IDs, model IDs and variants. Selecting a model with variants waits for the user to choose a reasoning level, so it does not issue two model mutations. The live test has no session mutation, prompt, SSE, diff, rename or delete path.

### XHigh - PASS with gaps

The only secret is `OPEN_CODE_PASSWORD`, scoped to a manual-dispatch-only job. It is supplied as an instrumentation argument, cleaned from Android Keystore in `finally`, and no command writes a credential, response or logcat to artifacts. GitHub masks exact secret output but cannot guarantee external server behavior; the live test is therefore a narrow read-only smoke test, not full end-to-end coverage.
