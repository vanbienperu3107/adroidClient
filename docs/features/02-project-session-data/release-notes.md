# v1.1.0-rc.1 — Feature 02 test release

versionCode: 14. Prerelease from feature/02-project-session-data; does not merge Feature 01/02 into main.

Includes Basic Auth server profiles, project selection, session list/status, history cache, read-only pending badge, rename and confirmed server delete. History provides a conservative Markdown subset; no prompt/SSE agent interaction UI.

## Validation and limitations

- Unit tests and APK compilation are checked for this revision; instrumented APK compilation is not device execution.
- Native UI, real Room runtime/backup/restore and Android TLS verification remain NOT_RUN/BLOCKED. This is a testing build, not a declaration of production readiness.
- Session load-more increases the requested limit to 10,000; not a durable snapshot cursor. History never prunes from a partial page alone; point deletion checks are bounded to 20 misses per refresh.
- Some full Feature 02 acceptance remains open (full Markdown, durable selection beyond saved-instance restoration, exhaustive concurrency/device evidence).
- DELETE permanently affects the server session on all devices; use disposable sessions for testing. No automatic retry of mutations.
- No credentials or server endpoint are embedded in this build. Existing signing identity is reused.

Native verification bypass for Feature 01 is not represented as a PASS for Feature 02. Full release/merge gate remains open; this artifact is provided at the user's explicit request to build a version.
