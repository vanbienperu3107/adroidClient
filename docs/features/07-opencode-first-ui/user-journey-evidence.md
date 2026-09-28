# OpenCode User-Journey Evidence

## Scope

The manual emulator test uses the same screens a user sees. It starts a new app, chooses **Connect OpenCode**, selects **Add server**, enters the server name, HTTPS URL, Basic username and repository-secret password, presses **Test**, saves the server profile, then opens **Projects**.

## Captured checkpoints

| File | User-visible assertion | Secret handling |
| --- | --- | --- |
| `01-connection-passed.png` | The form reports `Connected: <version>` after pressing Test. | The password input is rendered with `PasswordVisualTransformation`; the raw password is not visible. |
| `02-server-saved.png` | The OpenCode server list contains the saved profile. | The edit form is no longer visible. |
| `03-projects-loaded.png` | The app reaches the OpenCode Projects screen and does not show `No data`. | No credential field or network response body is shown. |

## Boundaries

The journey creates a temporary Android profile only in the disposable GitHub emulator. It does not create, rename or delete a remote session and it does not send a prompt. The screenshots and JUnit report are uploaded only from a successful manual workflow-dispatch run with `live_opencode=true`.

This validates the primary connect-and-browse UX with a real server. It does not validate physical-device behavior, history cache reconciliation, prompt/SSE flows, permissions or HTML interaction.
