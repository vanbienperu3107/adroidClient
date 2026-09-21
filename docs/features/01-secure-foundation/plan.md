# Plan Chi Tiet: 01 Secure Foundation

**Nguồn:** [plan 1.4](../../plan.md), [backlog](../../backlog.md), [Contract Spike](../00-contract-spike/go-no-go.md)

**Work items gốc:** WI-01 den WI-06

**Trang thai:** Chua bat dau

**Revision tài liệu:** 1.2 — áp dụng workflow toàn cục: phân rã, impact, coverage và gate Normal → High → XHigh. Đây là kế hoạch, không phải evidence implementation.

**Gate khởi động:** theo quyết định người dùng, testcase phụ thuộc hạ tầng hoặc code chưa tồn tại được đánh dấu `DEFERRED/NOT_RUN`, có owner và phase gate rõ ràng; không coi là PASS. Basic Auth có evidence HTTPS/server thật đủ để bắt đầu implementation Feature 01. Bearer, Android/reducer/backup thực tế không chặn bắt đầu Basic Foundation nhưng chặn release capability tương ứng cho đến khi có evidence.

## 1. Muc Dich va Ket Qua

Feature 01 tao nen tang an toan de Android luu server profile OpenCode va kiem tra ket noi ma khong dua secret vao Room, DataStore, navigation, log hoac backup.

Ket qua cua feature:

- Nguoi dung tao/sua/xoa server profile OpenCode.
- Release Feature 01 hỗ trợ Basic Auth trực tiếp với OpenCode chuẩn. Bearer Auth là capability deferred: không hiển thị selectable và không được lưu profile cho đến khi gateway có sẵn được probe thật.
- Credential nam trong vault ma hoa dung Android Keystore, key rieng theo server profile.
- OpenCode client trong release chỉ chấp nhận HTTPS và không bypass TLS; không thay đổi transport của provider hiện hữu trong feature này.
- Backup chi giu profile/metadata allowlist, restore bat buoc nhap lai credential.
- Health check map duoc Connected, Unauthorized, Forbidden, Unreachable, Timeout, TlsFailure va Incompatible.
- Navigation OpenCode dung ID, khong dua URL hay credential vao route.

Feature nay chua tao OpenCodeDatabase, project/session list, message sync, prompt hoac SSE. Cac feature do thuoc 02-05.

## 2. Dau Vao va Gate

### Contract da co

- OpenCode 1.18.30 da xac minh Basic Auth cho REST va SSE; thieu/sai credential tra 401.
- Bearer Auth chua duoc xac minh vi chua co gateway; mode này là DEFERRED/NOT_AVAILABLE, không phải blocker cho Basic Auth Feature 01. Feature gateway tương lai phải chạy TC-00B-04/05 trước khi bật UI/persistence Bearer.
- Health endpoint: `GET /global/health` tra `healthy` va `version`.
- Release production can HTTPS; Contract Spike local loopback HTTP khong du dieu kien production.

### Gate bat buoc truoc merge

- Khong co plaintext credential trong Room/DataStore/SavedStateHandle/route/log/backup.
- Unit tests cover vault error classification va URL transport policy.
- Backup rules cloud + device transfer loai vault va database noi dung.
- CI unit test, ktlint va debug build PASS.
- UI Compose native phải được kiểm chứng bằng Compose instrumented tests và emulator/device. Browser không thay thế kiểm chứng APK; nếu workflow workspace yêu cầu browser, ghi rõ giới hạn công cụ thay vì báo browser đã kiểm chứng native UI.
- Unit/fake, instrumented/integration và backup/restore thực tế là các lớp bằng chứng riêng. Thiếu môi trường Android/TLS/backup thì ghi BLOCKED cho gate tương ứng, không suy ra PASS từ CI unit xanh.

## 3. Baseline va File Anh Huong

| File/vung | Hien trang | Thay doi du kien |
| --- | --- | --- |
| `app/build.gradle.kts` | Chua co security crypto, lifecycle process hoac Ktor SSE | Them dependency vault/test can thiet; KHONG them SSE o feature nay |
| `gradle/libs.versions.toml` | Catalog dependency tap trung | Thêm dependency test cần thiết; dùng Android Keystore trực tiếp theo ADR, không mặc định thêm Security Crypto |
| `AndroidManifest.xml` | `allowBackup=true`, `usesCleartextTraffic=true`, da tham chieu 2 XML backup rules | Giữ transport provider cũ; enforce HTTPS trong OpenCode client. Backup rules chỉ thay đổi phạm vi OpenCode |
| `res/xml/backup_rules.xml` | Sample rong | Exclude vault va OpenCode content DB/file |
| `res/xml/data_extraction_rules.xml` | Sample rong | Cloud backup + device transfer allowlist/exclusion tuong ung |
| `data/network/NetworkClient.kt` | Singleton global, `expectSuccess=true`, log ALL va sanitize Authorization | Khong sua de them OpenCode auth mutable. Tao OpenCode HTTP factory/client rieng theo server profile |
| `di/NetworkModule.kt` | Cap NetworkClient va Anthropic API | Them OpenCode module rieng; khong sua provider API cu |
| `di/DatabaseModule.kt` | Chi ChatDatabase | Chua them OpenCodeDatabase trong feature nay |
| `presentation/common/Route.kt` | Route chat/setup/settings | Them OpenCode server routes chi mang `serverId` |
| `presentation/common/NavigationGraph.kt` | Graph Compose hien tai | Them graph OpenCode tach rieng neu UI profile nam trong scope |

## 4. Kien Truc va Quy Tac Bao Mat

### 4.1 Bounded context

```text
presentation/ui/opencode/server
  -> domain/opencode
  -> data/opencode
      -> security/OpenCodeCredentialVault
      -> api/OpenCodeHealthClient
      -> profile storage
```

Khong them `OpenCode` vao `ApiType`, `ChatRepositoryImpl`, `ChatViewModel`, `ChatScreen` hoac `SettingDataSourceImpl`.

### 4.2 Server profile metadata

Luu metadata khong nhay cam:

```text
serverId: UUID
displayName: String
baseUrl: canonical HTTPS URL
authMode: BASIC | BEARER
credentialRef: opaque vault reference
lastKnownVersion: String?
profileRevision: Long
lastHealthCheck: { result, checkedAt, profileRevision }?
createdAt / updatedAt
```

`authMode=BEARER` chi duoc cho phep khi build/runtime policy biet gateway bearer da duoc verify. Khong downgrade Basic sang Bearer tu dong.

### 4.3 Vault contract

Interface de xuat:

```kotlin
interface OpenCodeCredentialVault {
    suspend fun save(serverId: String, credential: OpenCodeCredential): CredentialRef
    suspend fun load(serverId: String, ref: CredentialRef): VaultResult<OpenCodeCredential>
    suspend fun delete(serverId: String, ref: CredentialRef)
}
```

`OpenCodeCredential` la sealed type:

- `Basic(username, password)`
- `Bearer(token)`

Quy tac:

- Tao Android Keystore key alias rieng per `serverId`.
- Ciphertext storage tach rieng profile metadata, ten file/key derive tu `credentialRef` khong phai secret.
- Credential giải mã không được cache trong ViewModel/StateFlow/SavedStateHandle. Draft người dùng nhập chỉ tồn tại tạm trong bộ nhớ form, không dùng rememberSaveable/SavedStateHandle; xem mục 4.10.
- Header chi duoc tao trong scope request va khong duoc ghi log.
- Log Ktor phai sanitize `Authorization`; OpenCode client khong log URL query/payload secret.

### 4.4 Phan loai vault/auth error

| Tinh huong | Ket qua | Hanh dong |
| --- | --- | --- |
| Vault access tam thoi fail | `VaultUnavailable` | Dung request, giu credential/key, cho retry thu cong |
| Key invalidated/mat | `ReauthenticationRequired` | Xoa entry cua dung server, dong stream/request, yeu cau nhap lai |
| Ciphertext corrupt | `ReauthenticationRequired` | Xoa entry cua dung server, yeu cau nhap lai |
| HTTP 401 | `Unauthorized` | Giu vault entry; yeu cau nguoi dung cap nhat credential |
| HTTP 403 | `Forbidden` | Giu vault entry; hien chinh sach server/gateway tu choi |
| TLS failure | `TlsFailure` | Khong co bypass; giu credential |
| Timeout/route failure | `Unreachable`/`Timeout` | Giu credential; retry theo thao tac nguoi dung |

### 4.5 Transport policy

- OpenCode release client: chỉ `https://`; reject `http://` trước khi gọi network. Đây là phạm vi OpenCode, không phải thay đổi policy toàn app làm hỏng Ollama HTTP hiện hữu.
- Debug: `http://` chi khi host la `localhost`, `127.0.0.1`, `10.0.2.2` hoac allowlist test ro rang.
- Khong custom TrustManager de chap nhan cert sai.
- Tailscale khong thay the HTTPS trong release policy.

### 4.6 Backup/restore

Backup allowlist OpenCode chi co profile metadata:

- display name, baseUrl, authMode, serverId, selection metadata.

Khong backup:

- vault/ciphertext, password, bearer token và mọi credentialRef/key alias/runtime revision.
- OpenCode content database, message/part/tool/diff/reasoning.
- pending prompt, permission/question, runtime state, connection state.

Sau restore, moi profile duoc imported o trang thai `ReauthenticationRequired`; user phai nhap lai credential va health check thanh cong truoc khi feature sau duoc phep ghi request.

### 4.7 Persistence và backup boundary — chốt thiết kế

- Chọn DataStore file riêng làm nguồn dữ liệu chính cho profile OpenCode; không tạo Room metadata DB tạm thời. Feature 02 tham chiếu serverId qua repository này, không tạo nguồn profile độc lập thứ hai. Khi làm Feature 02 phải đồng bộ lại mục `open_code_servers` của schema tổng thể trước implementation.
- DataStore hoạt động chứa credentialRef/revision/trạng thái cleanup nên bị loại khỏi backup. File export riêng, có schemaVersion và allowlist metadata, là đầu vào backup duy nhất của OpenCode.
- Vault/ciphertext đặt trong vùng không backup. Backup rules loại cả storage OpenCode đang hoạt động và content DB tương lai; không đưa allowlist toàn app chỉ chứa export khiến chat/settings GPT Mobile bị loại ngoài ý muốn.
- Giữ chính sách backup provider hiện hữu; phạm vi bảo đảm không credential backup ở đây là credential OpenCode. Credential provider cũ cần đánh giá riêng, không tuyên bố đã được bảo vệ bởi feature này.
- Restore validate schema, URL và authMode, import idempotent; không ghi đè credential/profile đang hoạt động khi ID va chạm. Entry xung đột cần được giải quyết rõ ràng; không tự gắn ref cũ. Health thành công ở Feature 01 chỉ mở khóa health/profile; tính năng ghi session tương lai vẫn phải qua reconcile của plan tổng thể.

### 4.8 Vault primitive và vòng đời chịu process death

- Chọn Android Keystore với key AES riêng từng server, mã hóa AES-GCM; ciphertext lưu file riêng bằng thao tác thay file atomic. ADR chốt tham số trước SF-02: IV mới mỗi lần mã hóa, authentication tag, format version và AAD gắn serverId + credentialRef + endpointBinding + authMode.
- Không tự triển khai thuật toán mã hóa; dùng primitive nền tảng. Không giả định hardware-backed trên mọi thiết bị; ghi khả năng thực tế trong evidence.
- Không có transaction chung giữa DataStore, file và Keystore. Serialize mutation theo serverId và dùng protocol phục hồi thay vì hứa atomic xuyên storage.
- Create/update: ghi entry mới với reference có phiên bản -> xác minh đọc lại -> commit metadata chuyển reference -> dọn entry cũ. Nếu commit metadata thất bại, reference cũ vẫn hợp lệ; entry mới chưa được tham chiếu được cleanup sau restart.
- Chỉ xóa key khi không còn entry được tham chiếu. Ciphertext cũ hỏng không được làm xóa key đang dùng cho entry mới của cùng server.
- Delete: ghi tombstone/revision, chặn request mới và hủy request cũ -> xóa vault -> xóa metadata/export. Cleanup idempotent; lỗi tạm thời giữ tombstone để tiếp tục sau restart, không phục hồi profile đã xóa.
- Startup recovery đối chiếu metadata, entry mồ côi và tombstone dưới cùng cơ chế khóa. Không xóa entry đang staging; rollback không xóa dữ liệu người dùng để che lỗi.

### 4.9 Endpoint binding và request generation

- Chuẩn hóa baseUrl bằng URL parser; reject userinfo, query và fragment. Giữ gateway path prefix hợp lệ; nối endpoint không được làm mất prefix. Port mặc định được chuẩn hóa; reject URL/host không hợp lệ trước network.
- Binding gồm canonical scheme/host/effective port/path prefix và authMode. Đổi binding yêu cầu credential mới hoặc xác nhận nhập lại; không gửi credential cũ sang endpoint mới. Chỉ sửa displayName không yêu cầu nhập lại.
- Tắt tự follow redirect cho OpenCode authenticated request trong MVP. Trả lỗi cấu hình redirect; tuyệt đối không forward credential qua khác origin hoặc HTTPS xuống HTTP. Kiểm tra cả URL ban đầu và URL request thực tế.
- Mỗi mutation profile tăng profileRevision; mỗi health attempt có request generation. Response chỉ được áp dụng nếu server còn tồn tại, revision/binding/generation vẫn khớp; phản hồi cũ bị bỏ qua.
- Update credential, đổi URL/authMode và delete hủy request cũ. Cancellation được truyền đúng theo coroutine, không map thành Unreachable hoặc kích hoạt retry.

### 4.10 UI credential và health state

- Tách sửa metadata với thay credential. Form edit không nạp secret đang lưu; password/token để trống nghĩa là giữ entry cũ khi binding không đổi, không phải xóa credential.
- Đổi authMode/binding bắt buộc credential tương ứng. Không tự downgrade mode. Test connection dùng draft tạm và chỉ Save mới persist; lỗi test không tự xóa credential đang có.
- Draft chỉ nằm trong form memory, không được backup/state restoration. Save/Cancel/rời màn hình/recreation bỏ draft và yêu cầu nhập lại. Bỏ tham chiếu không được mô tả là bảo đảm xóa mọi bản sao String khỏi JVM.
- Connected chỉ khi HTTP thành công, JSON health hợp lệ và healthy=true. healthy=false là Unhealthy; HTML 200/JSON sai hoặc thiếu trường bắt buộc là Incompatible; 5xx là ServerError. Không đưa body lỗi nguyên bản vào UI/log.
- Version phải là chuỗi hợp lệ theo contract. 1.18.30 là baseline đã probe; version khác được ghi Unverified compatibility, không tự tuyên bố hỗ trợ session API. Health hợp lệ vẫn có thể Connected kèm compatibility warning; schema không hợp lệ mới Incompatible.
- lastHealthCheck chỉ hiển thị kết quả lịch sử có timestamp. Khi app mở lại hoặc profile thay đổi, trạng thái hiện tại là NotChecked/ReauthenticationRequired, không lấy Connected cũ làm bằng chứng.

## 5. Phan Ra Work Package

### SF-01: Dependency va policy foundation

**Muc tieu:** them dependency toi thieu va policy type an toan, chua co UI.

- Ghi ADR xác nhận thiết kế mục 4.7–4.10 trước implementation; chốt format storage/AAD/export và verify API hỗ trợ trên minSdk 28.
- Them version catalog aliases va dependencies.
- Tao `OpenCodeAuthMode`, `OpenCodeCredential`, `OpenCodeConnectionState`, `VaultResult` trong domain.
- Tao `OpenCodeUrlPolicy` parse/canonicalize URL va enforce release/debug policy.

**Acceptance:** unit test URL matrix: HTTPS valid; HTTP production reject; HTTP debug loopback allow; host ngoai allowlist reject; malformed URL reject.

### SF-02: Credential vault

**Muc tieu:** luu/load/delete Basic/Bearer credential bang key rieng theo server.

- Implement `OpenCodeCredentialVault` va vault storage.
- Key alias co namespace app + `serverId`; khong dung mot key global.
- Map exception sang error contract muc 4.4.
- Xoa ciphertext/key chi khi invalidation/corruption xac dinh; khong xoa voi network/401/vault temporary.
- Tao fake vault cho unit test.

**Acceptance:** test Basic/Bearer round-trip; server A/B dung alias khac; delete A khong anh huong B; temporary error giu ciphertext; invalidation A chi xoa A; 401 khong xoa vault.

### SF-03: Profile repository va persistence

**Muc tieu:** CRUD profile metadata tach credential.

- Dùng DataStore profile riêng theo mục 4.7; backup export không phải nguồn dữ liệu hoạt động thứ hai.
- Implement staged entry, metadata reference switch, tombstone và orphan cleanup theo mục 4.8; không giả định transaction xuyên Keystore/file/DataStore.
- Update/delete áp dụng revision/generation guard; failure ở từng bước phải phục hồi được sau restart.
- Restore import metadata chuyen `ReauthenticationRequired`, khong copy credentialRef cu neu co nguy co tro den entry khong ton tai.

**Acceptance:** không plaintext secret trong profile; fault injection/process death tại từng điểm create/update/delete; old/new reference hợp lệ hoặc reauth rõ ràng, không profile half-configured và không xóa entry đang dùng.

### SF-04: HTTP health client va error mapping

**Muc tieu:** request theo profile, tao auth header ephemera va map loi cho UI.

- Tao `OpenCodeHealthClient`/factory rieng, khong mutate `NetworkClient` global.
- Load vault credential ngay truoc request; inject Basic/Bearer header theo authMode.
- Enforce endpoint binding, không follow redirect, giữ path prefix; kiểm tra revision/generation trước khi gửi và trước khi ghi response. Cancellation không phải lỗi network.
- Goi `GET /global/health`; parse `healthy`/`version` tu fixture Contract Spike.
- Map 401/403/404/5xx/TLS/timeout/JSON schema mismatch.
- Kiem tra `expectSuccess=true` cua client cu khong che mat HTTP status OpenCode; OpenCode client phai co response validation rieng.

**Acceptance:** MockEngine kiểm tra mapping, health false/HTML 200/version khác và stale response; TLS phải kiểm chứng thêm với engine thật và test HTTPS endpoint, không lấy MockEngine làm bằng chứng handshake.

### SF-05: Backup configuration

**Muc tieu:** allowlist profile metadata va exclude noi dung/secret cho ca cloud backup/device transfer.

- Tao storage files theo domain de backup XML co the include/exclude bang file, khong dung mot DB tron metadata va content.
- Cap nhat `backup_rules.xml` cho Android <= 11.
- Cap nhat `data_extraction_rules.xml` cho Android 12+ o ca `cloud-backup` va `device-transfer`.
- Document restore behavior va regression fixture.
- Loại DataStore hoạt động, vault, cleanup state; chỉ export allowlist có schemaVersion. Kiểm tra cả backup GPT Mobile trước/sau, không làm mất phạm vi backup cũ ngoài ý muốn.

**Acceptance:** static XML/schema checks cộng backup/restore thực tế trên Android <=11 và >=12 cho cloud/device transfer khi môi trường hỗ trợ. Thiếu transport kiểm chứng thì BLOCKED, không suy ra PASS từ XML. Payload không có credential/ref/runtime/content; restore luôn reauth.

### SF-06: Server profile UI va navigation

**Muc tieu:** UI toi thieu de add/edit/delete/test connection, tach chat provider.

- Them `OPEN_CODE_SERVERS`, `OPEN_CODE_SERVER_EDIT/{serverId}` routes.
- Them nav graph OpenCode; route chi `serverId`, khong URL/credential.
- Thêm entry OpenCode từ màn giới thiệu cho người chưa cấu hình provider; không bắt nhập API key provider để tạo profile OpenCode.
- Startup phân biệt provider-only, OpenCode-only, cả hai và chưa setup. Profile restored thiếu credential dẫn tới reauthentication, không bị xem là chưa setup. Không thay đổi hành vi provider-only. Nếu cả hai có cấu hình, giữ home hiện hữu và có entry OpenCode; không tự xóa back stack khi chuyển khu vực.
- Form: display name, URL, auth mode, Basic username/password hoac Bearer token; password field khong expose value khi edit.
- Test connection: show Connected/version, Unauthorized, Forbidden, Unreachable, Timeout, TlsFailure, Incompatible, ReauthenticationRequired.
- Delete confirmation va loading/error states.
- Thực hiện lifecycle draft và semantics giữ/thay credential theo mục 4.10; verify race Test connection -> Edit/Delete.

**Acceptance:** Compose instrumented tests/emulator hoặc device: add/edit/delete/test, rotation/recreation mất draft, password masking, route/state không có secret, lỗi và stale result đúng. Browser không thay cho native UI evidence.

### SF-07: ADR, tests va release gate

**Muc tieu:** chot quyet dinh va dam bao regression.

- Review ADR đã chốt từ SF-01; không trì hoãn quyết định storage/crypto tới cuối feature.
- Unit tests SF-01..SF-05; UI/instrumented tests SF-06.
- Security review no-secret log/storage/backups.
- Chay `testDebugUnitTest`, ktlint, debug build, release build khi feature complete.

**Acceptance:** tat ca CI xanh; release HTTP policy verified; docs cap nhat contract/backlog status.

## 6. Thu Tu Thuc Hien va Phu Thuoc

```text
SF-01 (types + URL policy)
  -> SF-02 (vault)
  -> SF-03 (profile repository)
  -> SF-04 (health client)
  -> SF-05 (backup rules) in parallel voi SF-04
  -> SF-06 (profile UI/navigation)
  -> SF-07 (ADR, security review, release gate)
```

`SF-04` va `SF-05` co the song song sau `SF-03`. `SF-06` chi bat dau khi repository + health mapping on dinh.

## 7. Test Plan

| Layer | Cases bat buoc |
| --- | --- |
| URL policy | HTTPS release allow; HTTP release reject; debug loopback allow; host/URL invalid reject |
| Vault | Basic/Bearer round-trip; per-server isolation; delete; temporary failure; invalidation; corruption; 401 no delete |
| Repository | Staged reference switch, fault injection/process death, tombstone, orphan cleanup, generation guard và restore conflict |
| Health | Basic/Bearer header selection; health fixture parse; 401/403/404/5xx/timeout/TLS/malformed response |
| Backup | cloud + device transfer XML; allowlist metadata only; content/vault/pending exclude |
| UI | Add/edit/delete; connection states; password masking; no credential in route; reauth prompt |
| Regression | Existing chat provider setup/settings/chat still build and tests pass |

Ba lớp evidence bắt buộc:

1. Unit/fake: policy, error mapping, repository recovery, request race; không chứng minh Keystore thật hoặc TLS handshake.
2. Android instrumented/integration: key/ciphertext thật, tampering/AAD isolation, recreation/process restart, TLS engine thật; ghi API level, build variant và revision.
3. Backup/restore thực tế: kiểm tra payload export, vault/content exclusion và import trên thiết bị không có key cũ. Ghi transport/platform và phần chưa kiểm chứng.

Regression provider phải bao gồm kết nối Ollama HTTP và backup dữ liệu GPT Mobile được giữ lại; build pass một mình không chứng minh hai hành vi này.

## 8. Risk va Rollback

| Risk | Giam thieu | Rollback |
| --- | --- | --- |
| Security Crypto deprecated/khong phu hop | ADR chot primitive theo API 28; wrapper interface | Xoa feature module/dependency, profile code khong cham provider chat |
| Vault error xoa nham credential | Phan loai error, test per-server key | Giu ciphertext voi error tam thoi; chi xoa entry co evidence invalidation/corruption |
| Backup lo content | File separation + export allowlist + kiểm tra payload thực tế | Disable OpenCode metadata export cho den khi rules dung |
| HTTP loophole release | URL policy truoc client + test | Reject toan bo HTTP neu build type policy khong xac dinh |
| Bearer gateway khong san sang | Feature flag/auth mode unavailable | Basic Auth van la path MVP chuan |

## 9. Definition of Done

- WI-01..WI-06 dat acceptance trong feature plan va backlog.
- Basic Auth end-to-end dùng profile + vault + health check.
- Bearer mode không được hiển thị/selectable trong release Feature 01; chỉ feature gateway tương lai với probe TC-00B-04/05 PASS mới được bật.
- No plaintext secret trong source, logs, Room/DataStore, route, backup artifact va test fixture.
- OpenCode release client reject HTTP và redirect, credential binding/revision guard đã kiểm chứng; provider transport cũ không bị siết ngoài phạm vi.
- Recovery sau process death được kiểm tra; không tuyên bố atomic xuyên storage. Keystore thật, TLS thật và backup/restore có evidence hoặc DEFERRED gate rõ ràng; không phát hành claim capability tương ứng khi evidence chưa có.

## 12. Deferred Gates

| Gate | Trạng thái | Owner / điều kiện mở | Chặn gì |
| --- | --- | --- | --- |
| Bearer gateway REST/SSE | DEFERRED | Hạ tầng cung cấp gateway URL + token test; chạy TC-00B-04/05 | UI/persistence/release Bearer, không chặn Basic |
| Android TLS thật | DEFERRED | Feature 01 có client Android + emulator/device HTTPS endpoint | Claim TLS Android và release gate security |
| Keystore/process death thật | DEFERRED | Feature 01 có vault + emulator/device | Claim vault production-ready |
| Backup/device transfer thật | DEFERRED | Feature 01 export/rules + Android transport | Claim backup/restore capability |
| SSE reducer/race | DEFERRED | Feature 04 có reducer/Room | Feature 04 release, không chặn Feature 01 |
| Permission/question offline | DEFERRED | Feature 05 có persistence/UI | Feature 05 release, không chặn Feature 01 |

Mọi gate DEFERRED phải giữ testcase và owner. Không xóa test, không đổi thành PASS vì chưa chạy, không đưa capability vào release notes trước evidence.
- ADR, testcase/result va docs duoc cap nhat.
- Chua them project/session/message/prompt/SSE implementation ngoai health check.

## 10. Hợp đồng với Feature 02 và phạm vi ảnh hưởng

- Profile DataStore là nguồn duy nhất. Đề xuất thay `open_code_servers` trong schema tổng thể bằng repository profile; database cache Feature 02 dùng serverId làm khóa logic, không có foreign key xuyên DataStore/Room. Đây là thay đổi cần đồng bộ tài liệu tổng thể trước implementation liên quan, không âm thầm tạo hai nguồn dữ liệu.
- Repository cung cấp sự kiện ProfileChanged/ProfileDeleting có revision. Feature 02 trở đi phải vô hiệu request, dọn cache theo serverId và đối chiếu orphan cache khi startup nếu lỡ event delete.
- Client/storage OpenCode có Hilt qualifier riêng; không thay binding HttpClient/DataStore/provider hiện hữu.
- Phạm vi sửa onboarding gồm MainViewModel/MainActivity và entry screen/navigation; thêm testcase cho provider-only và OpenCode-only. Shared backup XML phải có baseline payload trước/sau.
- Phạm vi “không secret” của feature là credential OpenCode do ứng dụng quản lý; không tuyên bố đã sửa cách lưu credential provider cũ.

## 11. Hợp đồng vault bổ sung

Interface mẫu ở mục 4.3 phải được cụ thể hóa trước code: save/load nhận endpointBinding + authMode từ profile đã validate, không lấy binding chỉ từ ciphertext không tin cậy. CredentialRef xác định entry có phiên bản và key generation. Sau invalidation, tạo generation key mới; cleanup reference cũ không xóa key mới.

MVP dùng AES-256-GCM, IV ngẫu nhiên 96 bit mới mỗi lần do primitive nền tảng sinh và tag 128 bit; format có schemaVersion/keyGeneration. AAD được encode có cấu trúc, xác định duy nhất, tránh nối chuỗi nhập nhằng. Test hoán đổi ciphertext/ref/binding, không chỉ round-trip. Không yêu cầu biometric cho mỗi request; nếu thiết bị không hỗ trợ cấu hình thì báo VaultUnavailable, không fallback plaintext.

Khi restore gặp ID đã tồn tại: giữ profile hiện hữu, bỏ entry import trùng và báo conflict để người dùng thêm profile mới nếu cần. Không tự thay URL/authMode/credentialRef. Export được cập nhật sau commit metadata bằng replace-file atomic; cleanup/export pending có thể tiếp tục sau restart. Backup cũ có thể chứa metadata profile đã xóa nhưng không được phục hồi credential hoặc phát sinh request tự động.
