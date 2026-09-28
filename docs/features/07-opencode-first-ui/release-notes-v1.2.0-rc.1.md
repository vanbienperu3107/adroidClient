# v1.2.0-rc.1 - Approved OpenCode-first UI prerelease

Android version: `1.2.0` (versionCode 16).

## Included

- OpenCode sessions and direct provider chats are presented as separate home sections.
- Chats supports title search, five recent entries per home section and access to the full history.
- New chat defaults to OpenCode or a direct provider according to the new additive Settings preference.
- Existing Theme, provider, Cliproxy, OpenCode server and About settings remain available.
- Assistant messages can render approved Mermaid, HTML and image artifacts. HTML is isolated with JavaScript, network, file/content access, storage and navigation disabled.

## Validation and limitations

- Kotlin format, unit tests, debug compilation and debug APK build are validated by CI on the exact release commit. Local validation is resource-constrained in this worktree because the Gradle daemon was killed during Kotlin compilation.
- Signed APK/AAB validation, artifact SHA-256 and certificate fingerprint are added to the GitHub prerelease after the signed workflow completes for the exact release SHA.
- Android device/emulator UI, WebView runtime, Keystore runtime, backup/restore, performance/accessibility and live OpenCode/Cliproxy artifact validation remain `NOT_RUN` without a controlled device and credentials.
- This is a prerelease. No credentials, proxy keys or endpoints are embedded in the artifact.
