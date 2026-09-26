# Impact Review Feature 06

Baseline: `08b9bc21d392f045114a180611e7760d33bcbe20`, worktree `feature/06-hardening-release`. Discovery bang Codebase MCP va doc source truc tiep. Graph coverage co parse-partial `app/build.gradle.kts` dong 30,37; cac ket luan ve file nay da doi chieu source va van la best-effort.

## Impact matrix

| Vung / bang chung baseline | Thay doi va rui ro | Giam thieu / regression / rollback |
| --- | --- | --- |
| Transport va secrets: `NetworkClient.kt` dung `LogLevel.ALL`, redact duy nhat `Authorization`; `OpenCodeReadApi`, health va browse goi qua policy | Body, query, Cookie, exception hoac raw event co the ro secret/content; tat logging toan cuc co the lam mat diagnostics | HR-01/02, T06-01..05. Dung structured allowlist va secret scanner; rollback chi ve config log an toan truoc do, khong bat lai body logging |
| URL/TLS: `OpenCodeUrlPolicy.canonicalize` da chan HTTP release, nhung manifest co `usesCleartextTraffic=true` | Provider HTTP/Ollama legacy co the bi anh huong neu tat cleartext toan cuc; policy app va transport diverge | Inventory provider contracts truoc HR-02; debug-only exception explicit, release deny; T06-01/02 bao phu OpenCode va provider regression |
| Vault/profile: `AndroidKeyStoreCredentialVault`, `DataStoreOpenCodeProfileRepository` co cleanup/recover/export | Redaction/backup/migration sai co the lo hoac xoa credential reference, profile export hay cleanup tombstone; crash giua export/cleanup | HR-01/03/04, T06-04,06-06..14. Khong reset DataStore/vault khi recovery; giu tombstone retry, reauth khi key mat |
| Backup: XML chi exclude vault va profile path; cache DB nam `noBackupFilesDir` | Rule path khong khop runtime file, WAL/SHM/staging hoac legacy storage co the vao cloud/D2D backup; unit XML presence test khong chung minh payload | HR-03, T06-06..09 tren transport that. Rollback chi cap artifact signed cu sau compatibility check; khong restore backup de rollback |
| Room: `ChatDatabase` v2, `OpenCodeCacheDatabase` v1 va schema OpenCode export | Migration feature moi co the khong co schema fixture, reset cache qua rong, hu Chat history, hoac old app khong doc duoc du lieu | HR-04, T06-10..14. Cache co the delete/rebuild theo database name/scope; profile/chat can migration/recovery khong destructive |
| Navigation/UI: `OpenCodeBrowseScreen`, `OpenCodeServerScreens`, `NavigationGraph`; LazyColumn o chat/home/OpenCode | Them semantics/bounded renderer co the vo focus, back stack, state restore, phone layout, provider chat; large content co jank/OOM | HR-06/07, T06-21..25; test portrait/landscape/tablet/TalkBack/font scale, provider regression. Rollback UI artifact, khong migrate data |
| Android runtime: instrumented tests hien co cache isolation va real keystore binding | In-memory Room va one vault case chua chung minh APK install, backup, invalidated key, process death, OEM/API variance | HR-05/09, T06-15..20,32..37; capture device/API/ABI/build SHA, khong dung unit xanh thay device evidence |
| CI: unit workflow chi unit; `debug-build.yml`/`ktlint.yml` dung `pull_request_target`; release workflow ky thu cong; tag workflow publish unsigned | PR from fork co privilege/supply-chain risk; checksum MD5 yeu; artifact unsigned public; tag/commit/version provenance khong ro | HR-08, T06-26..31. Dung least privilege, immutable SHA, pin action by commit SHA theo policy, signed-only promotion, SHA-256/cert/SBOM/provenance; rollback workflow phai duoc rehearsal |
| Release/distribution | VersionCode/versionName va tag co the lech, keystore secret co the lo log, rollback co the giao app cu voi schema moi | HR-08..10, T06-28..31,38..40. Manifest mapping version/tag/SHA/artifact; staged rollout, compatibility window, artifact da duoc verify |
| Feature 03-05 sync/prompt/agent | E2E hardening co the accidentally retry mutation, replay pending prompt, hoac auto-reply sau restore | HR-03/05/09, T06-09,32..37. Disposable fixtures, assert zero unexpected writes, no auto retry/approve/reply |

## Failure modes va recovery

1. **Secret hit in log/report/artifact.** Stop promotion, revoke/rotate affected secret theo owner, xoa/quarantine artifact/report theo retention procedure, create new signed build; khong chi redact sau khi da public.
2. **Backup restore includes prohibited bytes.** Stop release, disable backup route if can lam an toan, delete test backup, fix allowlist va rerun cloud/D2D restore. Do not rely on `noBackupFilesDir` alone or XML parsing unit test.
3. **Migration crash/corruption/full disk.** Transaction/backup semantics must leave prior valid state or explicit recovery state. Only named OpenCode cache may be rebuilt; never wipe profile, credential material, ChatDatabase or unrelated provider state as generic recovery.
4. **Keystore invalidation / process death.** Mark only bound profile reauthentication-required, preserve other server profiles, do not issue remote write during restart; verify tombstone/export recovery is idempotent.
5. **Large history/tool/diff / TalkBack tablet failure.** Limit/batch rendering and preserve accessible load-more/focus. OOM, ANR, lost focus or inaccessible destructive action blocks promotion even if unit checks pass.
6. **CI compromise or signing mismatch.** Refuse artifact when build SHA, checksum, certificate, version or provenance differs from manifest. Secrets only in protected context; fork PR cannot obtain them. Rebuild from reviewed immutable SHA.
7. **Post-release regression.** Halt staged rollout, use previously signed compatible artifact, collect redacted diagnostics and preserve data. Never use `git reset`, automatic database downgrade or credential restore as rollback.

## Regression scope and exit criteria

- Required non-OpenCode regression: provider chat (OpenAI/Anthropic/Gemini/Ollama), existing ChatDatabase, setup/settings/navigation, debug build and release install.
- Required OpenCode regression: two server/profile isolation, HTTPS/re-auth, project/session/history, prompt UNKNOWN/restart, SSE/reconcile, desktop mutation and permission/question resolution.
- Release is `BLOCKED` until exact SHA evidence demonstrates all required tests in [testcases.md](testcases.md); an implementation review must repeat Normal -> High -> XHigh on the final diff. This impact review identifies design risks only and is not runtime evidence.
