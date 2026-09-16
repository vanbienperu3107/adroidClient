# Project/Directory Scope Matrix (OpenCode 1.18.30)

Hai directory test: `<PROJECT_A>` va `<PROJECT_B>` (hai git repo doc lap tren cung server).

| Request | Scope | Ket qua |
| --- | --- | --- |
| `GET /session` (khong query) | Mac dinh theo cwd server | Tra session cua project hien tai (bao gom session tao boi cac client khac cung directory) |
| `GET /session?directory=<PROJECT_B>` | B | `[]` — khong lan session cua A |
| `GET /project/current` | Mac dinh | Project A (`worktree` = `<PROJECT_A>`) |
| `GET /project/current?directory=<PROJECT_B>` | B | Project B (`worktree` = `<PROJECT_B>`) — current project doi theo query |
| `GET /permission?directory=<PROJECT_B>` | B | `[]` scoped |
| `POST /session` (cwd A) | A | Session co `directory` = `<PROJECT_A>`, `projectID` = `global` |

## Ket luan

1. **`directory` query parameter la co che scope chinh** va hoat dong tren session/project/permission.
2. **`projectID` khong du de dinh danh** — instance nay tra `projectID: "global"` cho session trong khi `/project` co nhieu project voi id rieng. App phai dung **`(serverId, directory)`** lam khoa project va **`(serverId, directory, sessionId)`** cho session, dung nhu plan 1.4 muc 5.
3. **Session response luon co `directory`** — event/message co `sessionID`; muon biet directory phai join voi session record.
4. Cross-scope: goi session ID cua A khi current directory la B van tra duoc session (server khong chan theo directory khi truy cap truc tiep bang ID) — client phai tu filter theo directory, khong duoc coi server la nguon enforce scope.
