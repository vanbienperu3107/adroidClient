# Review Phan Ra Feature 06 - Normal -> High -> XHigh

Pham vi review: plan, backlog, impact va testcase design cua Feature 06; khong review implementation, khong chay runtime test, khong tag/release. Revision source: `08b9bc21d392f045114a180611e7760d33bcbe20`, branch/worktree: `feature/06-hardening-release` / `/workspace/Project/worktrees/06-hardening-release`.

## Evidence discovery

- Codebase MCP project `workspace-Project-worktrees-06-hardening-release`: status ready, 2,179 nodes/7,254 edges. Coverage ghi `app/build.gradle.kts` parse-partial tai dong 30,37; da doc source truc tiep. No skipped files; coverage van best-effort.
- Baseline da doi chieu: manifest cleartext global; Ktor `LogLevel.ALL` chi sanitize `Authorization`; profile/vault/cache va backup XML; ChatDatabase v2/OpenCode cache v1; unit/instrumented test hien huu; workflows unit/debug/lint/release/version.
- Tai lieu nay la design evidence. Tat ca testcase runtime trong `testcases.md` la `NOT_RUN`; khong co verdict runtime hay release PASS.

## 1. Normal - PASS

Verdict: **PASS cho phan ra tai lieu** tai revision tren.

- Intent duoc gioi han dung WI-24..28: security/redaction/log/backup, migrations, package/instrumented/E2E, performance/accessibility/tablet, CI/signing/version/rollback. Khong chen feature product moi, source implementation hay GitHub handoff.
- R06-01..06 map den HR-01..10. Moi work item co ID, input/output, scope, dependency, acceptance/testcase va status `NOT_STARTED` trong `backlog.md`.
- T06-01..40 deu co precondition, steps, expected, evidence/lop test. Tai lieu neu ro `NOT_RUN`, `BLOCKED`, `DEFERRED` khong la PASS va tách evidence unit/instrumented/E2E/package.
- Finding N-01 (CLOSED): ban nhap backlog thieu owner/evidence cho rollback. Tac dong: co the goi release xanh ma khong khôi phuc an toan. Da sua tai `backlog.md` HR-10 va `testcases.md` T06-38..40; xac minh bang rollback rehearsal record, compatibility matrix va signed handoff manifest. Khong con finding Normal mo.
## 2. High - PASS

Chi review sau khi Normal PASS. Verdict: **PASS cho architecture/impact/test coverage design** tai revision tren.

- Impact matrix lien ket bounded context OpenCode voi transport/provider legacy, vault/DataStore, backup, Chat/OpenCode Room, navigation/UI, Android runtime, CI va distribution. Moi vung co regression va/hoac rollback cu the.
- Design xu ly xung dot quan trong: manifest cleartext co the anh huong provider HTTP; backup can cover runtime path + WAL/SHM/staging; cache rebuild khac reset profile/chat; test fake khong thay thiet bi/E2E.
- CI design tach untrusted PR va protected signing context, immutable SHA, signed-only artifact, SHA-256/certificate/SBOM/provenance. Baseline `pull_request_target` va public unsigned tag release duoc coi la risk can sua, khong duoc chap nhan nguyen trang.
- Finding H-01 (CLOSED): ban nhap khong co target device/dataset/budget, nen acceptance performance/a11y khong the do. Tac dong: release gate co the green bang ket qua khong dai dien. Da sua tai `plan.md` gate 5, `backlog.md` HR-06/07 va `testcases.md` T06-21..25; xac minh bang report Perfetto/Macrobenchmark, scanner/TalkBack va screenshot matrix tren device duoc phe duyet.
- Khong con finding High mo trong pham vi tai lieu. Runtime, secret, device va release-owner input van la prerequisites cua implementation, khong la PASS runtime.
## 3. XHigh - PASS

Chi review sau khi High PASS. Verdict: **PASS cho failure-mode va evidence design** tai revision tren.

- Secret exposure plan yeu cau dung promotion, rotation/quarantine va rebuild; khong coi redact muon la remediation du. Redaction cover body/query/Cookie/exception/raw SSE/report/artifact va encoded/nested fixture.
- Backup/restore plan kiem tra cloud va device-transfer tren device, khong chi parse XML; restore khong auto send/approve/reply va reauth truoc sync/mutation.
- Migration/recovery cover crash, corruption, disk/IO failure, prior-app compatibility va rollback. Only named cache co the rebuild; generic wipe DataStore/vault/ChatDatabase bi cam.
- Race/process death duoc map cho vault cleanup, prompt/snapshot, desktop conflict, stale callback va post-restore lifecycle. Required evidence la device/E2E khi claim lien quan Android/runtime; unit xanh khong du.
- Finding XH-01 (CLOSED): baseline tag workflow co the public artifact unsigned va release workflow dung MD5. Tac dong: integrity/signing chain khong du cho production, co rui ro supply-chain. Da sua thiet ke tai `plan.md` gate 6-7, `backlog.md` HR-08 va `testcases.md` T06-26..31; xac minh bang CI protected-context, `apksigner`/`jarsigner`, SHA-256, certificate allowlist, SBOM/provenance va staging release inspection.
- Release integrity yeu cau exact source SHA, version/tag/artifact map, certificate allowlist, SHA-256 va independently reproducible handoff. Rollback la artifact da signed-compatible, khong `git reset`, downgrade destructive hay restore credential.
- Khong con finding XHigh mo trong thiet ke. Neu protected CI, signing certificate, backup transport, device/benchmark budget hay target server khong duoc cap, work item/testcase lien quan phai dung `BLOCKED` va release khong duoc pass.

## Verdict va handoff

**PASS chi cho design phan ra/impact/testcase** tai source revision `08b9bc21d392f045114a180611e7760d33bcbe20`. Sau khi implementation bat dau hoac tai lieu doi, fingerprint/evidence nay het hieu luc o phan bi anh huong va phai review lai Normal -> High -> XHigh tren diff va evidence thuc te.

Feature 06 production/release readiness: **NOT_RUN**, khong phai PASS. Da chot GitHub prerelease va repository owner cho approval/rollback; dieu kien mo gate HR-01 van la SHA tich hop Feature 01-05 va input CI/device/server con lai duoc liet ke trong `plan.md`. Khong co tag hay release nao duoc thuc hien trong pham vi nay.
