# Feature 06 - Hardening & Release

Revision tai lieu: 1.0. Baseline source: `08b9bc21d392f045114a180611e7760d33bcbe20` tren branch `feature/06-hardening-release`. Trang thai: `PLANNED`; khong co source, test hay release nao duoc thay doi/chay trong pham vi phan ra nay.

## 1. Intent va ranh gioi

Feature 06 la gate production cho toan bo OpenCode flow da duoc xay o Feature 01-05. Pham vi bao gom security/redaction/log/backup, migration, package/instrumented/E2E, performance/accessibility/tablet, va CI/signing/version/rollback. Ket qua mong doi la mot release candidate co artifact truy xuat duoc, chi ky dung key cho phep, duoc kiem tra tren thiet bi that, va co rollback da dien tap.

Khong mo rong chuc nang OpenCode, giao thuc server, UI product, migration destructive, commit/push/tag/release trong feature nay. Khong coi build xanh, test fake, hay tag workflow hien tai la bang chung production readiness.

## 2. Discovery va baseline

- Codebase MCP dung worktree: `workspace-Project-worktrees-06-hardening-release`, index `ready`, 2,179 nodes/7,254 edges. `app/build.gradle.kts` co parse-partial tai dong 30 va 37; file nay da duoc doc truc tiep. Coverage la best-effort, khong chung minh day du tuyet doi.
- App la Android/Kotlin, `minSdk 28`, `targetSdk 34`, version hien tai `versionCode 14`, `versionName 1.1.0-rc.1`. `ChatDatabase` dang v2; `OpenCodeCacheDatabase` dang v1, schema export bat va database nam trong `noBackupFilesDir`.
- Vault hien tai dung AES-GCM va Android Keystore theo server; profile export co allowlist va khong dua credentialRef/password vao export. Test don vi chi kiem tra XML co exclude vault/profile, chua chung minh payload backup/restore tren thiet bi.
- `AndroidManifest.xml` hien dat `usesCleartextTraffic="true"`. `NetworkClient` dat Ktor `LogLevel.ALL` va chi redact header `Authorization`; Feature 06 phai dong cac duong ro ri request/response, URL query, exception va telemetry truoc release.
- Workflow `release-build.yml` build unsigned truoc khi ky, dung MD5 trong summary va tu ky thu cong. `version-build.yml` phat hanh APK release unsigned khi push tag. Day la baseline rui ro, khong phai release contract chap nhan duoc.
- Unit workflow chi chay `testDebugUnitTest`; instrumented, E2E, migration/device backup, performance va accessibility chua co bang chung runtime trong baseline.

## 3. Requirements va acceptance

| Req | Yeu cau | Acceptance feature |
| --- | --- | --- |
| R06-01 | Security, redaction, logging | Khong co credential, prompt/content, raw SSE, URL query secret, keystore bytes hay header nhay cam trong log, crash/analytics, artifact hay report; release chan cleartext va TLS bypass. |
| R06-02 | Backup va restore | Cloud backup va device transfer chi co allowlist metadata; vault, DataStore credential reference, OpenCode cache/database sidecars, content, pending action va runtime state bi loai; restore bat buoc reauthenticate va khong gui mutation. |
| R06-03 | Migration va recovery | Tat ca schema dang ho tro co upgrade path da test, rollback app khong lam mat du lieu nguon, migration failure/corruption co xu ly an toan va cache co the rebuild ma khong xoa profile/chat. |
| R06-04 | Package, instrumented va E2E | Debug/release package duoc xac minh tren device/emulator phu hop; instrumented test dung AndroidKeyStore/Room that; E2E bao phu auth, sync, prompt, desktop conflict, restart va offline/recovery. |
| R06-05 | Performance, accessibility, tablet | History/tool/diff lon nam trong budget duoc phe duyet tren thiet bi muc tieu; TalkBack, font scale, contrast, focus, landscape/tablet va navigation khong block thao tac. |
| R06-06 | CI, signing, version va rollback | CI build tu SHA bat bien, phat hanh chi artifact signed da verify SHA-256/certificate, version/tag mapping mot-mot; co promotion gate, provenance/SBOM, rollback rehearsal va owner/on-call record. |

## 4. Thiet ke gate va quyet dinh

1. **Security boundary.** OpenCode release transport phai bat HTTPS va chan HTTP truoc credential request; khong theo redirect sang host/scheme khac va khong tat certificate validation. Feature 06 khong tat cleartext globally vi Ollama HTTP legacy van duoc ho tro; thay doi global policy can requirement/migration va regression review rieng. Logging production mac dinh OFF hoac metadata allowlist; cam xuat body, URL query, `Authorization`, `Cookie`, credential, prompt, SSE payload, stacktrace co request. Redactor phai duoc unit-test voi case nested/encoded va exception. Xem [decisions.md](decisions.md).
2. **Backup boundary.** `noBackupFilesDir` la phong thu thu nhat, XML rules la phong thu thu hai; work item phai inventory ca `opencode-cache.db`, `-wal`, `-shm`, vault, staging va profile export. Khong dua profile export vao backup truoc khi allowlist duoc kiem tra end-to-end. Restore bo qua runtime/pending state, danh dau `ReauthenticationRequired`, va chi doc/refresh sau khi nguoi dung cap credential.
3. **Data migration.** Inventory schema truoc khi code: ChatDatabase v2, OpenCode cache v1 va DataStore preferences/export. Moi schema co persisted user data can co source schema fixture, migration test upgrade, downgrade/older-app behavior va recovery decision. Cache co the delete/rebuild theo scope; profile, credential va chat user data khong duoc reset nhu mot fallback migration.
4. **Evidence separation.** Unit/fake chi chung minh logic; instrumented chi chung minh Android runtime; E2E chi chung minh flow tich hop; package/signature chi chung minh artifact. Mot lop xanh khong thay lop khac.
5. **Performance/a11y gate.** HR-06 phai chot device/API/build variant, dataset dai dien va ngan sach p50/p95, startup/memory/jank truoc khi HR-07 code/toi uu. Khong tu dat nguong san pham bang doan; khi ngan sach chua duoc product/release owner duyet, work item va release gate lien quan la `BLOCKED`.
6. **Release promotion.** Tag khong tu dong tao public release unsigned. CI chi promotion commit tren protected release branch/tag, checkout SHA day du, chay required checks, tao AAB/APK signed trong job tach quyen toi thieu, verify bang `apksigner`/`jarsigner`, checksum SHA-256, certificate fingerprint allowlist, dependency/SBOM va provenance. Secrets khong xuat hien trong command log, artifact hay summary. Artifact unsigned neu can chi la private intermediate co retention ngan, khong attach release.
7. **Version va rollback.** `versionCode` tang don dieu va tag `v<versionName>` tro den dung commit/artifact manifest. Rollback la rollout mot artifact signed da tung duoc phe duyet, kem compatibility matrix va monitoring window; khong git reset, downgrade schema destructive, hay restore credential/backup tu dong.

## 5. Phu thuoc, gia dinh va blocker

- HR-01..08 chi bat dau sau khi Feature 01-05 da co SHA tich hop va contract OpenCode target duoc pin. Baseline hien tai chua phai bang chung tat ca feature truoc da merge/verified.
- Da chot: GitHub prerelease la kenh promotion mac dinh; repository owner la release approval va rollback/on-call owner. Van can release owner cung cap: thiet bi/API target, benchmark budget, keystore/certificate fingerprint allowlist, retention policy va changelog. Day la input bat buoc, khong tu y suy doan.
- Can ha tang: emulator/device farm, test OpenCode server co du lieu synthetic, desktop peer disposable, backup transport/restore-capable device, secret manager va protected CI environment. Thieu bat ky input nao thi testcase tuong ung `BLOCKED`, khong phai `PASS`.
- Chi co the phat hanh sau khi moi testcase release-required co evidence dung SHA va ca ba review implementation Normal -> High -> XHigh PASS. `NOT_RUN`, `DEFERRED`, skipped, artifact unsigned va CI cua revision khac khong duoc coi la PASS.

## 6. Ke hoach thuc hien

Xem phan ra tai [backlog.md](backlog.md), impact/regression/rollback tai [impact.md](impact.md), va test mapping tai [testcases.md](testcases.md). Thu tu gate: HR-01 -> HR-02 -> HR-03 -> HR-04/HR-05 -> HR-06 -> HR-07 -> HR-08 -> HR-09. HR-10 chi co the hoan tat sau khi release candidate da qua cac gate truoc va co dien tap rollback thanh cong.

Review tai lieu nay co the PASS cho **thiet ke phan ra** neu dependency, acceptance va testcase ro rang. Day khong la production readiness va khong cap phep tag/release.
