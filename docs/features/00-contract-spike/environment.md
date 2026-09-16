# Environment: Contract Spike Execution

**Executed:** 2026-09-16

**Server:** OpenCode `1.18.30` (`opencode serve`), local loopback instance dung lam server test.

**Topology thuc te cua lan chay nay:**

```text
probe (curl) -> http://127.0.0.1:<PORT> -> opencode serve (OPENCODE_SERVER_PASSWORD bat)
```

**Khac biet so voi production topology du kien:**

| Muc | Spike nay | Production du kien |
| --- | --- | --- |
| Transport | HTTP loopback | HTTPS/Tailscale |
| TLS | NOT_RUN (khong co cert tren loopback) | Bat buoc, khong bypass |
| Gateway Bearer | NOT_RUN (khong co gateway trong moi truong) | Tuy cau hinh cong ty |
| Server version | 1.18.30 | Phai khoa lai khi co server cong ty |

**Auth mode da xac minh:** HTTP Basic (`OPENCODE_SERVER_PASSWORD`, username mac dinh `opencode`).

**Project layout:**

- `<PROJECT_A>` = worktree `/workspace/.spike/project_a` (git repo)
- `<PROJECT_B>` = worktree `/workspace/.spike/project_b` (git repo)

**Ghi chu quan trong:** server local liet ke ca cac project/worktree khac cung user (`GET /project` tra ve moi project server biet). Response co field `id` va `worktree`; mot so project dung `id: "global"` — identity project can dua tren `worktree` (directory), khong chi `projectID`.

**Gioi han cua lan chay:** vi la loopback HTTP va khong co gateway, cac ket qua TLS/Tailscale/Bearer o testcases.md la `NOT_RUN`/`BLOCKED` va phai chay lai voi server cong ty truoc khi ket luan go/no-go production.
