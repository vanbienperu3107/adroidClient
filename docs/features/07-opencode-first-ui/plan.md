# Feature 07 - OpenCode-first UI and Direct Cliproxy

## Intent

Make OpenCode sessions the primary workspace experience while preserving existing direct-provider chats. Add a separate direct Cliproxy configuration with a Keystore-backed key and server-derived model catalog.

## Decisions

- Home has separate OpenCode sessions and direct provider chat sections.
- The default project comes from `GET /project/current`; first listed project is fallback.
- OpenCode model and reasoning level are server-provided `providerID`/`modelID` and `variant`, not hard-coded labels.
- Cliproxy direct is independent of OpenCode. Its URL/model metadata lives in preferences; its API key lives only in the Android Keystore vault.

## Work items

| ID | Output | Acceptance |
| --- | --- | --- |
| UI-01 | OpenCode selection in onboarding | OpenCode appears before direct providers and routes to server setup. |
| UI-02 | Split home | OpenCode sessions render before direct chats; each opens its corresponding flow. |
| UI-03 | Default project | Resolve server current project, fall back to first project, keep scope isolated. |
| UI-04 | Conversation composer | Load model catalog from `/api/model`; send server variant through `/api/session/{id}/model`. |
| UI-05 | Cliproxy direct settings/chat | Key stays in vault, `/models` is dynamic, direct chat does not require OpenCode. |

## Known validation gaps

No emulator/device is available for Compose native UI, Keystore runtime, project switching or SSE lifecycle validation. Those cases remain `NOT_RUN`; CI/unit coverage cannot replace them.
