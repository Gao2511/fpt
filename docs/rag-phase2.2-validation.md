# Phase 2.2 — Integration validation status

Ngày kiểm tra: 2026-10-11. Branch `feature/rag-integration`, HEAD `48b700f`.
Các thay đổi Phase 1/2/2.1 hiện có trong working tree được giữ nguyên.

**Kết quả mới nhất: migration 002/003 thành công, 24/24 PostgreSQL integration
tests PASS, 0 FAIL, 0 BLOCKED.** Sau khi người dùng báo Docker đã hoạt động,
đã xác nhận Engine 29.8.0 và chạy trên container mới riêng, PostgreSQL 17.10 /
pgvector 0.8.2. Không cần sửa runtime code hoặc test để đạt kết quả này.

Embedding vẫn là mock, không gọi Gemini thật theo yêu cầu mới nhất.
`RAG_ENABLED=false` giữ nguyên. Kết quả DB không chứng minh end-to-end Gemini
hoặc chất lượng semantic retrieval thật; chưa bật production RAG.

## 1. Kết quả thực tế trong Phase 2.2

| Kiểm tra | Kết quả thực tế |
|---|---|
| Branch/HEAD | `feature/rag-integration` / `48b700f` |
| Biên dịch Java hiện tại | `BUILD SUCCESSFUL`, Java 17, `--release 8` |
| `RagBehaviorTest` | 34 PASS, transport/retrieval mock |
| `RagPhase2CorpusTest` | 13 PASS, corpus/regression offline |
| `RagQualityTest` | 27 PASS, engine/query/source-selection mock |
| Tổng RAG offline chạy lại | **74 PASS** |
| `RagPostgresTest` | **24 PASS / 0 FAIL**, thực thi trên PostgreSQL/pgvector thật, embedding mock |
| `RagTool` | Biên dịch thành công; chưa chạy lệnh gọi API/DB |
| Runner database riêng | Thành công; container mới `fpt-rag-test-ad8985908086`, đã stop và giữ volume |
| Restart được người dùng phê duyệt | CLI timeout; đóng/mở đúng tiến trình Docker, backend crash ở Ingest socket |
| Migration 002/003 vào database thật | **Cả hai COMMIT thành công**, chỉ trong DB test mới |
| Gemini metadata GET / embedding POST / chat generation | **0 / 0 / 0 request thật do lần kiểm tra này thực hiện** |
| `git diff --check` | Exit 0; chỉ có cảnh báo LF/CRLF |

Kết quả 227 Java, 6 frontend, 17 JSP là kết quả Phase 2.1 trước đó;
Phase 2.2 đã chạy 74 RAG offline tests ở lần trước; lần tiếp tục này rebuild source
thành công và thực thi đủ 24 DB tests. Không chạy lại full 227/front-end/JSP suites
vì không sửa runtime/frontend. Không gộp compile thành integration PASS.

Log của lần chạy này, được Git ignore:

- `.build/phase22-offline-tests.log`
- `.build/phase22-postgres-attempt.log`
- `.build/phase22-current-build.log` — build lần tiếp tục mới nhất.
- `.build/phase22-postgres-run1.log` — migrations và đủ 24 PASS, exit 0.

## 2. Môi trường và kết quả mới nhất sau Docker phục hồi

- Branch/HEAD vẫn `feature/rag-integration` / `48b700f`; không commit/push/merge.
- Engine, container-list và image-inspect APIs đều trả thành công. Image digest
  đúng bản pin, không tải image khác hoặc đổi embedding model.
- Bốn container cũ `ceee82a37c26`, `b05edab291c6`, `696e267869e4`,
  `285823fd72d6` (prefix `fpt-rag-test-`) đều `Exited (0)`, mỗi container còn
  mount volume riêng. Trạng thái trước/sau không đổi; không start/reset/delete.
- Hai tên từng timeout `fpt-rag-test-378cc8e126fd` và `fpt-rag-test-7bd40796a8d5`
  không xuất hiện trong current engine. Không kết luận đã xóa hoặc mất dữ liệu;
  lần kiểm tra này không xóa container/volume nào.
- Runner tạo **container mới** `fpt-rag-test-ad8985908086`, label
  `com.fpt.rag.test=true`, DB `fpt_rag_test`, credential riêng ngẫu nhiên,
  bind `127.0.0.1` port động. Java suite kiểm tra ba bảng ban đầu rỗng.
- Chỉ container mới được cài `vector` và áp dụng 002 → 003; log ghi đủ
  `BEGIN`/DDL/`COMMIT` hai lần. Không chạy migration 001 hoặc chạm DB website.
- PostgreSQL **17.10**, pgvector **0.8.2**, vector 768 chiều synthetic từ
  `MockEmbedding`; persistence/cosine/FTS/transactions/RLS là database thật.
- Tất cả 24 case PASS ngay lần chạy đầu sau Docker phục hồi. Log
  `[RAG indexing] error=DATABASE` là write-failure injection ở rollback test;
  `SOURCE_CONFLICT` là kịch bản kiểm tra mâu thuẫn/authority. Không có test fail.
- Runner `finally` đã stop container mới; inspect sau chạy xác nhận
  `state=exited`, exit 0, mount volume
  `3390afca6686125315d22deb7ca2e2cabbe9e8d3aee7f6e1cc1c3bc760db5c51` vẫn tồn tại.
  Không xóa rows/role/volume để giữ kết quả kiểm tra theo yêu cầu người dùng.
- Process runner ép `RAG_ENABLED=false`, `RAG_EMBEDDING_CALLS_APPROVED=false`,
  sau chạy khôi phục env caller. `RagPostgresTest` inject mock trực tiếp;
  không tạo Gemini adapter hoặc LLM request, không lấy key từ website settings.
- Credential file `.local/rag-tests/fpt-rag-test-ad8985908086/postgres.env`
  và build/test logs đã xác nhận Git ignored. Không in password.

Lệnh đã thực thi sau build source bằng portable Ant:

```powershell
./test/run-rag-isolated.ps1
```

## Lịch sử Docker diagnosis trước khi phục hồi

Các đoạn dưới đây ghi nhận lần bị chặn và restart trước đó; không phải trạng thái
hiện tại. Các container đã được kiểm tra lại như mục 2 ở trên.

Đã kiểm tra CLI, service/process, context, WSL, trạng thái Desktop, log backend
và VM; lệnh Docker có deadline, log chỉ báo loại/số lượng lỗi, không in payload.

| Probe | Quan sát |
|---|---|
| Docker Desktop/backend process | Đang chạy |
| Docker CLI `info` version | `29.8.0` |
| Context | `desktop-linux` |
| Endpoint | `npipe:////./pipe/dockerDesktopLinuxEngine` |
| `wsl --list --verbose` | `docker-desktop`: **Stopped**, WSL 2 |
| `docker desktop status` | **starting** |
| `docker desktop start --timeout 20` | Báo Desktop đã chạy; không chuyển engine sang running |
| Filtered test-container list | Timeout, chưa xác nhận trạng thái |
| Pinned pgvector image inspect | Timeout, chưa xác nhận image có sẵn |
| Backend log | Nhiều `deadline exceeded`, phần lớn liên quan IPC/engine |

Đây là lỗi khả dụng engine/backend trước khi chạy SQL. Lệnh lấy version thành
công **không chứng minh** image/container API hoạt động. Chưa xác định được lý
do sâu hơn khiến WSL engine dừng; không suy diễn image thiếu, hết dung lượng,
pgvector lỗi hoặc test SQL fail từ timeout này.

Runner `test/run-rag-isolated.ps1` đã được thử và dừng ở image preflight,
**trước bước tạo credential/container mới và trước migration**. Không xóa
container, image, volume trong lần audit ban đầu; chưa restart ở lần đó.
Người dùng sau đó trả lời **“Cho phép restart Docker Desktop”**. Kết quả thực hiện
được ghi riêng dưới đây; không thực hiện `wsl --shutdown` hoặc factory reset.

### Cập nhật sau khi được duyệt restart

1. `docker desktop restart --timeout 45` không hoàn tất trong deadline; status
   vẫn starting và PID không đổi.
2. Xác minh executable paths nằm trong installation
   `%LOCALAPPDATA%/Programs/DockerDesktop`, rồi đóng riêng `Docker Desktop`,
   `com.docker.backend`, `com.docker.build`. Không đóng WSL service, PostgreSQL,
   hoặc tiến trình nằm ngoài installation đã xác minh.
3. Mở lại `Docker Desktop.exe` với `WindowStyle Hidden`; theo dõi startup có
   deadline. Backend thoát với monitor `exit status 150`; lệnh list/image/info
   đều chưa dùng được. Không suy exit 150 thành thiếu .NET: chưa có bằng chứng
   runtime thiếu; log xác định lỗi Ingest dưới đây.
4. Log backend/monitor chỉ rõ:
   `starting services: initializing Ingest server`, rename
   `sailor-ingest.sock` → `sailor-ingest.sock.stale` bị Windows từ chối:
   **The file cannot be accessed by the system**.
5. Hai đường dẫn nằm trong `%LOCALAPPDATA%/Docker/run`, đều 0 byte,
   Attributes 1056 (Archive + ReparsePoint), thư mục cha là directory thường.
   `fsutil reparsepoint query` trả **Error 1920** cho cả hai. Chưa đọc được tag,
   nên không khẳng định tag AF_UNIX cụ thể hoặc đã chứng minh corruption/ACL.
   Ổ C được Windows báo NTFS/Healthy; hypervisor hiện diện, firmware
   virtualization enabled. Không đổi BIOS, Windows features, driver hoặc .NET.
6. Khi không có backend active, đã thử `Rename-Item -LiteralPath` riêng tệp
   `.stale` 0 byte sang tên dự phòng duy nhất sau khi kiểm tra đường dẫn/type.
   Rename bị `IOException`; không có backup file mới. **Không tiếp tục delete,
   clear reparse tag, sửa ACL, reset hoặc thay VHDX.**
7. Log giao diện ghi sự kiện `Reset to factory defaults` lúc 12:52 ngày kiểm tra.
   Agent không gửi thao tác reset; chưa xác định người/thành phần phát sự kiện
   hoặc reset có thực thi việc xóa dữ liệu không. Không dùng sự kiện này như
   bằng chứng dữ liệu đã mất hoặc còn nguyên.
8. Kiểm tra chỉ đọc sau crash: `docker_data.vhdx` vẫn tồn tại, size
   6,573,522,944 bytes, mtime 11:23:37; `main/ext4.vhdx` size 100,663,296 bytes,
   mtime 11:29:38. Sự hiện diện file không chứng minh integrity/container contents.
   Trạng thái cuối: Desktop/backend không còn chạy, WSL distro stopped; **24 DB
   cases vẫn chưa thực thi**. Không có migration/test writes sau restart.

Điểm chặn đã cụ thể hơn: lỗi truy cập socket runtime trên Windows ngăn backend
khởi động, không phải SQL/pgvector. Sửa sâu filesystem/reparse/driver hoặc reboot
Windows nằm ngoài thao tác restart Docker đã duyệt. Operator cần lưu công việc,
thử reboot Windows rồi mở Docker; nếu vẫn lỗi, xử lý socket với Windows/Docker
support sau khi sao lưu disk. Không tự reset/reinstall hoặc upload diagnostics.
[Docker troubleshooting](https://docs.docker.com/desktop/troubleshoot-and-support/troubleshoot/),
[Backup khi Desktop không khởi động](https://docs.docker.com/desktop/settings-and-maintenance/backup-and-restore/).

Các tên test còn chưa xác nhận trạng thái từ các phase trước:

- `fpt-rag-test-378cc8e126fd`
- `fpt-rag-test-7bd40796a8d5`

Không thể xác nhận database trong hai container này còn tồn tại hay dùng được.
Không đọc `docker inspect` toàn bộ vì phần Env có thể chứa password. Khi engine
phục hồi, chỉ inspect Name/State/Image/Mounts/Ports/label trước; không tự remove.
Runner integration yêu cầu database rỗng, nên không chạy lại lên DB chứa kết quả
cũ. Dùng container mới qua runner để giữ dữ liệu cũ cho kiểm tra.

### Database test dự kiến khi Docker hoạt động

- Image giữ nguyên: `pgvector/pgvector:0.8.2-pg17`.
- Digest đã pin:
  `sha256:feb68f4f15446397d8cac7f4fe48fe4586de83160d1fc48b46283312d1a33966`.
- DB `fpt_rag_test`, container mới `fpt-rag-test-<random>`, label
  `com.fpt.rag.test=true`, port ngẫu nhiên chỉ bind `127.0.0.1`.
- Credential test ngẫu nhiên lưu `.local/rag-tests/<name>/postgres.env`, ignored;
  không dùng `.local/runtime.json` hoặc credential website.
- Role `fpt_rag_user` là role quản trị **cluster test disposable**, không phải
  runtime website; quyền tạo extension/role phục vụ setup/RLS probe ở test.
- Runner dùng embedding mock, ép `RAG_ENABLED=false` và
  `RAG_EMBEDDING_CALLS_APPROVED=false` trong process rồi khôi phục env.
- Stop container trong `finally`, giữ container/data để inspect. Stop không phải
  xóa dữ liệu. Nếu daemon không trả lời, trạng thái stop phải báo chưa xác nhận.

### Phương án thay thế đã xác định, chưa provision

1. PostgreSQL native local: service PostgreSQL 18 đang chạy nhưng kiểm tra lại
   không có `share/extension/vector.control` hoặc `lib/vector.dll`. Process test
   không có credential/DB test riêng. Không cài binary, tạo DB/role hoặc thay auth.
   Chỉ dùng `test/run-rag-local-postgres.ps1` khi admin cung cấp DB **rỗng riêng**
   `fpt_rag_test`, pgvector tương thích và quyền setup test phù hợp.
2. Cloud staging: một project PostgreSQL/Supabase staging **tách khỏi production**,
   hoặc Supabase branch riêng do admin phê duyệt, có pgvector và JDBC SSL.
   [Supabase pgvector](https://supabase.com/docs/guides/database/extensions/pgvector),
   [Supabase branching](https://supabase.com/docs/guides/deployment/branching).
   Chưa có staging endpoint/credential/quyền hay ngân sách được xác nhận;
   **chưa tạo dịch vụ hoặc branch cloud**. Admin phải kiểm tra chi phí trước.
   Runner hiện chỉ nhận loopback development và đúng DB name; không bỏ guard
   để trỏ production. Cần runner staging riêng có allowlist đích/approval trước
   khi thực thi ở cloud.

## 3. Migration inspection

Đã đọc và thực thi `database/migrations/002-rag-mvp.sql` rồi
`database/migrations/003-rag-source-policy.sql` trên container test mới.
Cả hai transaction COMMIT thành công. Không áp dụng vào production.

| Hạng mục | Bằng chứng source / kết luận inspection |
|---|---|
| Ordering | pgvector được operator cài trong DB test trước, rồi 002, rồi 003 |
| Atomicity | Mỗi migration có `BEGIN`/`COMMIT`; không chạy ở startup |
| Idempotency | 002/003 là migration một lần, **không idempotent**; không có `IF NOT EXISTS` cho tables/columns. Không rerun trên schema đã có |
| FK | Chunks/jobs FK document `ON DELETE CASCADE`; supersedes FK document |
| Constraints | Status/category/index/job/attempt enums, hashes, độ dài, dates, dimensions 768, unique revision/ordinal và unique job revision/model/dim |
| Publication | Schema yêu cầu approval không null khi published; DAO/retrieval còn yêu cầu actor > 0, timestamp không ở tương lai |
| Source authority | 003 enum `admin_reviewed`/`official`; mặc định reviewed, không tự nâng thành official |
| Supersession | Key đúng format, target ID nhỏ hơn ID mới, key không rỗng; DAO kiểm tra same key và không downgrade official |
| Vector | Cột `vector(768)`; application cấu hình cứng 001/768 và chuẩn hóa L2 |
| Model consistency | DAO ghi model/dim; worker kiểm tra job với adapter, cache theo model/dim; retrieval so chunks/doc/model/dim. SQL không có CHECK khóa mọi dòng thành model 001 |
| Revision filtering | Published, ready, indexed_version/version/hash hiện tại; source revalidation trước render |
| Dates | Dùng DB clock cho effective/expiry/approval; archived/draft không phục vụ |
| Lineage/conflict | Recursive supersession và peer conflict query ở `KnowledgeChunkDAO`; integration cases 19–24 đã PASS |
| Security | RLS cả ba bảng, revoke PUBLIC/anon/authenticated; không tự thêm backend grant/RLS policy production |

Same-key/no-authority-downgrade là kiểm tra DAO, không phải đầy đủ constraint
SQL. Vì vậy chỉ authenticated admin backend được phép cập nhật metadata; không
cho khách hoặc model tự gán `official`/approval/supersedes.

MVP dùng exact cosine + GIN FTS, chưa có ANN index. Không thay threshold/weights
khi chưa có corpus/embedding thật để benchmark.

## 4. Từng integration case

Tất cả case dưới đây đã thực thi: **24 PASS / 0 FAIL / 0 BLOCKED**.
PostgreSQL/pgvector thật, embeddings mock; không phát sinh lỗi cần vá.

| # | Case trong `RagPostgresTest` | Trạng thái |
|---|---|---|
| 1 | Insert/find/list draft và revision đầu tiên | PASS |
| 2 | Invalid actor bị từ chối, không ghi dữ liệu | PASS |
| 3 | Publish/enqueue atomic và reject stale edit | PASS |
| 4 | Index current revision, chunk/vector persistence | PASS |
| 5 | Unpublished không retrieve | PASS |
| 6 | Withdraw publication dù chunks còn tồn tại | PASS |
| 7 | Source-only revision dùng embedding cache, source revalidation | PASS |
| 8 | Expires filter dùng DB clock | PASS |
| 9 | Future effective filter | PASS |
| 10 | Real cosine/FTS với vector mock; semantic/keyword/hybrid SQL | PASS |
| 11 | Archive không retrieve/revalidate | PASS |
| 12 | Partial index write failure rollback | PASS |
| 13 | Hai worker claim hai job riêng | PASS |
| 14 | Update lúc embedding không publish stale chunks | PASS |
| 15 | Lease hết hạn không overwrite claim mới | PASS |
| 16 | Crashed/exhausted job báo failed, retry hữu hạn | PASS |
| 17 | RLS deny role untrusted dù accidental SELECT grants | PASS |
| 18 | Metadata/approval proof roundtrip | PASS |
| 19 | Replacement loại policy cũ trước/sau index | PASS |
| 20 | Future replacement chờ; expired replacement không hồi sinh policy cũ | PASS |
| 21 | Transitive lineage khi intermediate archive | PASS |
| 22 | Reject wrong key, authority downgrade, self-cycle | PASS |
| 23 | Same-authority conflict; peer revalidation và withdrawal | PASS |
| 24 | Official thắng reviewed, không tin recency | PASS |

Coverage lưu ý: case 23 chạy peer-check SQL nhưng **không dựng hơn 12 candidate**
để đo chính xác tình huống ngoài top-12. Đây còn là test cần mở rộng trong phase
sau. Case 10 dùng synthetic vectors: chứng minh SQL, không chứng minh
semantic recall của Gemini. Suite chưa kiểm thử roundtrip Servlet/Tomcat thật.

Cleanup: suite yêu cầu ba bảng ban đầu rỗng và không có production connection.
Hiện suite để lại document/chunk/job và role test `fpt_rag_untrusted` để inspect;
runner stop cluster test, không tự delete. **Chưa có assertion cleanup dữ liệu cuối
suite.** Stop/volume retention đã được inspect xác nhận sau chạy. Không cấp CREATEROLE/superuser cho
website runtime để đáp ứng test; RLS probe cần setup-role riêng trong test.

## 5. Gemini Embedding verification

Source đọc: `GeminiEmbeddingService`, `RagConfig`, `RagFactory`, `RagTool`,
`.env.example`, `AIService`; không in/decrypt credential.

- Model hiện tại `gemini-embedding-001`, output 768. Google vẫn liệt kê model
  này, input 2,048 tokens và dimensions 128–3072; 768 thuộc mức khuyến nghị.
  001 cần normalize khi dùng 768; adapter đã L2 normalize.
  [Embedding model documentation](https://ai.google.dev/gemini-api/docs/embeddings).
- Endpoint fixed `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent`;
  auth header `x-goog-api-key`, không đặt key trong URL.
- Body: `model`, `content.parts[].text`, `embedContentConfig` gồm
  `taskType=RETRIEVAL_DOCUMENT/RETRIEVAL_QUERY`, `outputDimensionality=768`,
  `autoTruncate=false`; document có `title`. Response đọc `embedding.values`,
  yêu cầu numeric, hữu hạn, không zero và đúng 768.
  [REST contract](https://ai.google.dev/api/embeddings).
- Key ưu tiên `RAG_EMBEDDING_API_KEY`; runtime factory có thể dùng key Gemini
  hiện hữu đã được AIService decrypt khi được cấu hình cho RAG. Operator
  `RagTool` chỉ dùng env key; không tự lấy DB key. Chưa kết nối DB chứa settings
  nên không xác nhận key đã lưu có quyền model này.
- Process audit/test hiện thiếu `RAG_EMBEDDING_API_KEY`; các approval/model
  verified flags unset, hiệu lực false. Không đọc key backup hoặc website env.
- Không gọi cả metadata GET dù adapter cho phép GET trước cost-approval:
  Phase 2.2 yêu cầu explicit authorization trước **mọi real API request**.
- Có verify-model GET để kiểm tra `models/<id>` và `supportedGenerationMethods`
  chứa `embedContent`; GET không chứng minh quota còn, output dimensions trả
  thật hoặc chi phí. [Models API](https://ai.google.dev/api/models).
- HTTP: tổng deadline 10s, mỗi socket tối đa 5s, tối đa 3 attempts, retry
  429/5xx/network; 400/401/403/404 configuration error không retry; response
  giới hạn 64 KiB, không redirect, không log body/key.

Quyền, quota, free-tier và billing **chưa được xác nhận theo account**. Rate limit
phụ thuộc project/tier, xem trong AI Studio; không suy từ việc website chat dùng
Gemini thành embedding chắc chắn miễn phí/quyền truy cập đầy đủ.
[Quota documentation](https://ai.google.dev/gemini-api/docs/rate-limits).
Bảng giá công khai hiện đọc không có mục riêng 001; không dùng giá embedding-2
để ước tính 001. Admin phải xác nhận giá model trong project trước khi duyệt.
[Pricing](https://ai.google.dev/gemini-api/docs/pricing).
Kiểm tra key restriction/API access theo project; không tự rotate/tạo key hoặc
đổi restriction hiện hữu. [API key guidance](https://ai.google.dev/gemini-api/docs/api-key).

### Tên biến cấu hình (không có secret values)

- DB: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE`.
- Test guard: `RAG_ENVIRONMENT`, `RAG_TEST_DATABASE_APPROVED`.
- RAG: `RAG_ENABLED`, `RAG_EMBEDDING_MODEL`, `RAG_EMBEDDING_DIMENSIONS`,
  `RAG_EMBEDDING_MODEL_VERIFIED`, `RAG_EMBEDDING_CALLS_APPROVED`,
  `RAG_EMBEDDING_API_KEY`, `RAG_DATABASE_APPROVED`.
- Nếu tái sử dụng settings encrypted: `AI_ENCRYPTION_KEY` giữ nguyên effective
  key; không yêu cầu đổi/rotate để chạy test.

`.env.example` là tài liệu mẫu, không tự load env. Chỉ nạp vào process test riêng;
không dùng `setx`/đổi cấu hình toàn máy hoặc website để bật thử RAG.

## 6. Quality / end-to-end evaluation

Các assertions offline kiểm tra hành vi thật của engine/planner/validator với
provider/DAO mock. Chưa có customer → Servlet → JDBC thật → Gemini → UI result.

| Nhóm câu hỏi | Bằng chứng offline đã chạy | Source IDs thật / relevance / correctness thật / latency |
|---|---|---|
| Formal support | Engine route và render nguồn fake đã duyệt | Chưa đo |
| Slang: shop/mạng lag/wifi yếu/bro | 5 regression route, giữ nguyên câu hỏi | Chưa đo |
| Follow-up | Domain từ customer history, reference mơ hồ hỏi lại | Chưa đo |
| WiFi 6 / Camera | Technical guide không bị blanket-block; marketing chưa duyệt bị loại | Chưa đo |
| Mesh | Business facts ở catalog; không tự dùng KB làm bảng giá | Chưa đo |
| Installation | Server-owned 300.000đ/lần, không cộng monthly; RAG off giữ hành vi | Chưa đo qua DB/catalog production hoặc Gemini |
| Missing information | Empty/DAO error fallback an toàn | Chưa đo |
| Conflicts | Equal authority withholding; official priority; accent không dedup sai | DB cases 23/24 PASS; chưa đo chất lượng live Gemini |
| Injection | Reject toàn chunk chứa instruction, không render metadata | Chưa đo với Gemini thật |
| RAG disabled | Không query DAO, giữ consultation và session isolation | PASS trong mock-engine test, chưa chạy deployment |

| Metric cần báo sau live test | Phase 2.2 hiện tại |
|---|---|
| Retrieved real source IDs | Doc/chunk IDs được assert trong 24 DB cases; chưa có source-ID report theo câu hỏi live |
| Retrieval relevance / recall | Chưa đo |
| Correct answer rate trên câu hỏi approved | Chưa đo |
| Unsupported claims từ live answers | Chưa đo, không báo 0% |
| Average response latency | Chưa đo |
| Real provider requests do task này thực hiện | 0 |
| Actual usage tokens / cost | Không có usage/billing result; không gán giá 001 hoặc quota giả |

Fixture `test/rag/fixtures/project-knowledge.json` chỉ có nội dung gaming từ
project có thể đề nghị duyệt để live; fee/Mesh thuộc business source, WiFi/Camera
marketing cần admin xác nhận. Fake guides/approval trong regression không phải
chính sách FPT được phép publish/live index.

## 7. Exact administrator actions / controlled test proposal

1. Docker đã healthy; bốn container cũ đã inspect, không cần restart/reset nữa.
   Không dùng production hoặc xóa dữ liệu test cũ. Hai tên timeout trước đó không
   có trong current engine; không tự phục hồi/xóa cấu hình riêng của chúng.
2. DB validation đã hoàn tất. Container mới đã stop, dữ liệu được giữ. Không cần
   admin tạo tài khoản, cấp superuser cho website hoặc bật pgvector production
   để hoàn thành lần kiểm thử này.
3. Khi cần tái lập, chạy runner từ repository bằng PowerShell 7/Java 17:

   ```powershell
   ./test/run-rag-isolated.ps1
   ```

   Runner tạo cluster test mới, cài vector và chạy 002→003; chạy 24 assertions.
   Kết quả mới nhất đã có actual log 24 PASS và stop/volume retention xác nhận.
   Nếu cần cleanup/re-run cùng cluster, phải kiểm tra dữ liệu/role và duyệt riêng;
   không tự drop/reset.
Các bước 4–9 dưới đây là kế hoạch live test trong tương lai, **không thực hiện**
theo yêu cầu lần tiếp tục này: không gọi Gemini API thật.

4. Sau DB PASS, admin duyệt **nội dung cụ thể** gaming hoặc tài liệu kỹ thuật
   chính thức WiFi/Camera bổ sung. Không dùng fixture marketing chưa duyệt.
5. Trong Google AI Studio project hiện tại, admin xác nhận model 001 access,
   key restrictions, RPM/TPM/RPD, tier và giá. Nạp key bằng secret env riêng của
   process test, không gửi key qua chat; không rotate/encryption change.
6. Duyệt ngân sách request cụ thể. **Đề xuất chưa được phê duyệt:** 1 tài liệu,
   tối đa 1 chunk và 5 câu hỏi; 1 document embedding + tối đa 5 query embeddings,
   metadata GET và tối đa 5 chat invocations. Dùng chat model hiện hữu.
   Hard cap đề nghị **20 HTTP requests tổng**, tính cả metadata/retry; dừng khi
   hết cap hoặc lỗi/quota, không bulk index. Admin duyệt mức chi phí tối đa theo
   billing thực tế trước chạy.
7. Trước live execution cần harness đếm request thật (bao gồm retries) và chặn
   trước gửi nếu hết cap. `RagTool index -Limit 1` chỉ giới hạn **job**, không
   giới hạn chunks/API requests; không dùng nó như cost cap. Harness budget/
   metrics chưa được triển khai trong phase audit/test bị chặn này.
8. Sau khi duyệt riêng metadata GET:

   ```powershell
   ./tools/run-rag-tool.ps1 -Operation verify-model
   ```

   Chỉ đặt `RAG_EMBEDDING_MODEL_VERIFIED=true` trong process test sau kết quả
   xác minh; chỉ bật callsApproved sau duyệt chi phí. RAG enabled chỉ trong
   isolated test process, RAG disabled control dùng process/config riêng.
9. Ghi question ID, real chunk/document IDs, source validity, human relevance,
   correct/unsupported claims, wall-clock toàn pipeline, actual HTTP attempts,
   status/error enums và usage metadata nếu API trả. Adapter hiện chưa expose
   embedding usage metadata/counters; không lấy chat latency làm RAG total latency.
   Giữ source metadata nội bộ, UI chỉ nhận customer-facing text.

## 8. Readiness Phase 3 — Admin Knowledge Base

Có thể tiếp tục thiết kế CMS và permission UX từ DAO/DTO hiện tại. **DB gate đã
đạt:** migrations 002/003 và 24 integration cases PASS với mock embeddings.
Các điều kiện còn thiếu trước production rollout/live indexing:
- Chưa chứng minh model access/dimensions/quota/billing theo account.
- Chưa có real embedding/retrieval/chat quality benchmark hoặc Servlet roundtrip.
- Chưa có hard API-budget/usage recorder cho controlled live evaluation.
- Chưa có cuối-suite cleanup assertions và test peer ngoài >12 candidate.
- Publication endpoint/UI cần authenticated admin, CSRF, optimistic versions,
  authority/supersession review, audit trail và runtime least-privilege grants/RLS;
  không mở DAO actor integer thành public API.

Không có lỗi integration cần vá; chưa thay đổi chức năng đang hoạt động.
Sau explicit Gemini approval mới bổ sung harness và đo live; không thay
chat model, package facts, CMS/pricing UI hoặc registration logic để qua test.

## 9. Files / safety

Lần tiếp tục này chỉ cập nhật **`docs/rag-phase2.2-validation.md`**. Không sửa runtime code,
migrations, prompt, keys, business facts hoặc feature defaults. Artifacts compile/
logs nằm trong `.build` ignored. Các thay đổi chưa commit từ phase trước được giữ.

Files inspected: migrations 002/003; `.env.example`; `GeminiEmbeddingService`,
`RagConfig`, `RagFactory`, `RagDatabase`, `VectorValues`, `KnowledgeIndexingService`,
`KnowledgeRetrievalService`, `RagContextBuilder`, `KnowledgeSourceSelection`,
`KnowledgeContentPolicy`, `RetrievalQuery`; document/chunk/job DAOs, DTOs;
`AIService`, `ConsultationEngine`, `DBUtils`; `RagTool`, operator runner;
isolated/local/PostgreSQL/offline runners, bounded Docker helper; `RagPostgresTest`,
offline quality/corpus/behavior tests, project knowledge fixture và phase reports.

Không deploy, commit, push, merge, rotate hoặc sửa production database. Default
`RAG_ENABLED=false` giữ nguyên. Không gọi real Gemini, tạo cloud staging, xóa
test container/volume hoặc đổi database auth. Chưa có production rollout cần
rollback; nếu thử staging sau này, rollback bằng tắt RAG/calls và dừng worker,
giữ schema/data để kiểm tra, không tự drop.
