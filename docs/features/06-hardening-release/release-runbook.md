# GitHub Prerelease Runbook

## Preconditions

- Repository owner approves the candidate commit and is rollback/on-call owner.
- Candidate is on a feature or release branch, never pushed directly to `main`.
- CI unit, lint, debug package and signed release evidence all refer to the exact candidate SHA.
- Signed artifact SHA-256 and certificate SHA-256 are recorded with the prerelease.
- Known device, benchmark and backup/restore gaps are marked `NOT_RUN` or `BLOCKED`; they cannot be rewritten as PASS.

## Promotion

1. Dispatch `Generate Release Version` for the approved candidate SHA.
2. Verify `apksigner` output, APK/AAB SHA-256 and source SHA from the job summary.
3. Repository owner creates a GitHub prerelease that points to that SHA and attaches only signed APK/AAB artifacts.
4. Record the release tag, source SHA, artifact SHA-256, certificate fingerprint, CI URLs and open validation gaps.

## Rollback

1. Stop prerelease promotion and identify the last signed, compatible prerelease artifact.
2. Repository owner publishes or directs users to that prior artifact; do not use `git reset`, destructive database downgrade, vault reset or automated backup restore.
3. Record the incident, affected candidate SHA, selected rollback artifact SHA-256 and compatibility decision.
