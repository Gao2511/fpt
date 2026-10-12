# Phase 2.1 — retrieval và chọn nguồn, 2026-10-11

## Phạm vi và trạng thái

Branch `feature/rag-integration`, HEAD `48b700f`; toàn bộ thay đổi vẫn chưa commit.
Giữ kiến trúc Servlet/JSP/JDBC/pgvector và Gemini embedding 001/768 hiện có.
Không gọi Gemini thật, không phát sinh request tính phí, không đổi model/key,
không sửa giá/chính sách FPT, không push/merge/deploy hoặc sửa Supabase production.
`RAG_ENABLED`, model verification và call approval vẫn mặc định false.
Artifact build chỉ nằm trong `.build/dist/`; không thay WAR đang chạy hoặc `dist/` cũ.

## Audit: nguyên nhân và bản sửa

| Vấn đề | Code path trước bản sửa | Thay đổi |
|---|---|---|
| Tiếng lóng không vào retrieval | `ConsultationEngine.ragQuestion` chỉ match một số cụm từ | `RetrievalQuery` dựa trên domain, ý định hỗ trợ, catalog, typed requirements và context có giới hạn; thêm các biến thể thông dụng |
| Follow-up mất chủ đề retrieval | Engine truyền riêng câu hiện tại cho `retrieve` | Rút domain từ tối đa hai lượt khách gần nhất; không copy raw history/assistant instructions vào embedding |
| Thêm camera không gắn với TV/household | Policy chỉ có nhánh hỏi có-camera-không; câu không dấu hỏi có thể trở thành camera preference | Nhánh so sánh camera dùng package snapshot và yêu cầu TV/speed đã có; câu hỏi không được coi là consent |
| Nguồn mâu thuẫn cùng xuất hiện | Service chỉ filter và lấy ba hit, renderer nối các ID model chọn | Chọn theo metadata thẩm quyền/chủ đề/thay thế; conflict giữ lại trước model, kiểm tra peer lại trước render |
| Tên thiết bị bị chặn toàn chunk | `KnowledgeContentPolicy` chặn bare WiFi6/modem/camera | Cho phép hướng dẫn kỹ thuật đã duyệt; vẫn chặn financial/spec/entitlement/guarantee claims |
| Không biết lý do bị loại | `allowed` chỉ trả boolean | Enum reason + counter; log chỉ mã/count, conflict log không chứa nội dung, query hay secret |
| DB test treo | Docker CLI không có giới hạn thời gian | Docker wrapper có deadline và preflight image; bổ sung lựa chọn PostgreSQL local riêng |

System Prompt hiện tại không được viết lại. Câu khách nguyên vẹn vẫn được gửi
model và lưu theo chronological turns. Không suy đoán độ tuổi từ tiếng lóng.
`ChatSessionManager`/retention/process-local storage được giữ nguyên.

## Routing và xử lý context

`RetrievalQuery` không dùng một danh sách slang để quyết định mọi intent.
Nó kết hợp service domains, hỗ trợ kỹ thuật, package identifiers có trong catalog,
typed facts hiện có và tham chiếu ngắn. Canonical variants chỉ bổ sung search terms;
original question/giá/số/mã kỹ thuật không bị thay trong prompt. Các token như
`220K`, `KO-AX3000`, `2.4GHz` được giữ nguyên trong câu gốc; normalization không
thay slang token nằm bên trong technical identifier có dấu nối/chấm/số.

Greeting, social acknowledgement, joke và yêu cầu rõ ràng ngoài phạm vi bỏ qua RAG.
Phí lắp đặt, đăng ký, private-account/human handoff vẫn được xử lý trước retrieval.
Đối với default prompt, business intent có đáp án sẵn dùng existing catalog/policy.
Custom admin prompt vẫn đi qua provider cho consultation thông thường và các
validator/factual rendering hiện có. RAG disabled không truy cập DAO/embedding.

Follow-up chỉ lấy domain labels từ hai lượt CUSTOMER gần nhất (mỗi lượt tối đa
2.000 ký tự), không gửi toàn history hoặc private address/assistant prose cho embedding.
Truy vấn tổng tối đa 2.000 ký tự. Nếu không xác định được tham chiếu, hỏi một câu
ngắn; không chọn ngẫu nhiên gói hoặc tự thêm dữ liệu khách.
Typed budget/household/TV vẫn được engine truyền theo cơ chế hiện có. Ví dụ TV
cho 4 người rồi thêm camera được so sánh từ catalog, không tạo business policy mới.

## Quy tắc source và conflict

Migration **003**, thực hiện thủ công sau 002 trên DB test được duyệt, thêm:

- `policy_key`: ID chủ đề do người duyệt đặt; mặc định rỗng cho bản ghi legacy.
- `source_authority`: `admin_reviewed` hoặc `official`, do trusted admin phân loại.
  Không tự suy ra từ URL, similarity score, thời gian hoặc model output.
- `supersedes_document_id`: liên kết đến tài liệu cũ có cùng policy key, đã từng
  được duyệt, không hạ authority; target ID phải nhỏ hơn để không có cycle.

Đổi metadata qua DAO tạo revision/hash mới, invalidates index và enqueue như
luồng cũ. Default metadata giữ hash legacy; không tự sửa dữ liệu cũ hoặc tạo official
approval. Archive giữ publication proof trước đó để phục vụ reviewed lineage,
nhưng archived vẫn không được retrieve. Không có Admin KB UI mới trong phase này.
Actor integer không tự chứng minh admin identity; caller cần auth/CSRF như Phase 1.

1. Chỉ published + approval actor/time hợp lệ + effective/unexpired + current
   indexed version/hash/model/dimension được đưa vào candidates.
2. Replacement đã duyệt và đang hiệu lực chặn phiên bản cũ, kể cả khi index của
   replacement chưa ready: trả không đủ nguồn an toàn thay vì dùng policy lỗi thời.
   Future hoặc tài liệu chưa từng được duyệt không tự retire prior policy. Một
   replacement đã được duyệt trước expiry và từng tới ngày hiệu lực vẫn chặn
   ancestor sau expiry/archive: không tự hồi sinh policy lỗi thời. Recursive
   lineage còn chặn ancestor sau khi intermediate document được archive.
3. Khi cùng chủ đề, chọn authority đã được duyệt cao hơn, không chọn newer document
   hoặc highest similarity để giải quyết nội dung trái nhau.
4. Hai nguồn có cùng authority và khác nội dung → `SOURCE_CONFLICT`, không gọi model,
   phản hồi yêu cầu tư vấn viên xác nhận. Mixed versions/hash của một document →
   `VERSION_CONFLICT`, cũng giữ lại.
5. Nội dung giống nhau deduplicate; so sánh evidence giữ dấu tiếng Việt, tránh coi
   “dừng” và “dùng” là một. Duplicate doc/chunk không được lặp trong phản hồi.
6. Nhiều chủ đề khác nhau được yêu cầu làm rõ, không ghép ba nguồn theo score.
   Trong một document hợp lệ, score vẫn dùng để chọn đoạn phù hợp và tie-break ID.
7. Trước khi render, DAO kiểm tra lại source/title/approval/authority/key cùng
   version/hash và kiểm tra current same-subject peers ngoài cửa sổ top-12.
   Có peer authoritative mới hoặc cùng authority trái nội dung thì không render
   nguồn đã chọn, kể cả peer không xuất hiện ở candidate window.
8. `RagContextBuilder` cũng kiểm tra selection nếu caller đưa raw candidate list.
   Model chỉ chọn allowed IDs; response chỉ render approved excerpts, không metadata.

Tài liệu `official` phải được quản trị viên xác minh nguồn thật; đây không phải
hệ thống tự xác minh authenticity của mọi URL. Metadata đặt sai chủ đề hoặc tự
mâu thuẫn trong cùng tài liệu vẫn cần editorial review. Không có semantic contradiction
classifier mới hoặc cơ chế tổng hợp policy tự do. Phản hồi extractive được giữ lại.

## Hybrid retrieval

Giữ exact cosine/FTS, model space 001/768, score weights và semantic threshold
hiện tại vì chưa có benchmark live để biện minh thay đổi hệ số.
Canonical query terms và stopwords loại các từ xã giao như shop/ơi/bro và
“hướng dẫn” khỏi FTS, giảm các hit chỉ khớp lời mở đầu. Mã `220 k` được chuẩn
hóa thành `220k` cho search; package selection vẫn qua catalog, không qua RAG.
Source validation xảy ra trước chọn ba đoạn và có content deduplication.
Low-confidence, expired, missing approval và superseded sources có reason riêng.
Embedding failure vẫn có lexical fallback; SQL failure giữ fallback consultation.

## Giải thích năm snapshot corpus

| Snapshot | Trước Phase 2.1 | Sau Phase 2.1 |
|---|---|---|
| LAN/gaming | Qua vì không có finance/spec hoặc bare blocked device keyword | `ACCEPTED` trong reviewed in-memory fixture; live publication vẫn cần duyệt |
| Phí lắp đặt | Bị chặn bởi “phí” | `INVALID_BUSINESS_CLAIM` khi đưa vào RAG; trả từ `business.standardFee`/package DB |
| WiFi 6 | Bị chặn tên WiFi6/modem | `MISSING_APPROVAL` theo review state; nếu giả lập duyệt thì guarantee “không giật lag” vẫn bị chặn |
| Camera | Bị chặn bare camera | `MISSING_APPROVAL` theo review state; marketing 24/7 không tự thành support policy/spec mới |
| Mesh F1 | Chứa cước/giá/phí và số tiền | `INVALID_BUSINESS_CLAIM`; tiếp tục dùng business route hiện có |

`expectedRejectionReason` được thêm vào fixture và được test trên validator thật.
Không thiếu source path ở năm snapshot, không hết hạn và không duplicate trong
bộ này. Unapproved WiFi/Camera là review status của fixture, không kết luận về
approval trong DB thật. Approved technical guidance có tên WiFi6/Camera/modem
được regression test riêng và được chấp nhận; không ép cả năm mẫu marketing/fee pass.
Các reason khác: `MISSING_SOURCE`, `INVALID_METADATA`, `EXPIRED`, `NOT_EFFECTIVE`,
`SUPERSEDED`, `LOW_CONFIDENCE`, `UNSAFE_CONTENT`, `DUPLICATE`.

## Test đã thực thi

Chạy trên Java 17, compile target 8, Ant portable build:

```powershell
./tools/build-portable.ps1 -AntHome '<AntHome>' -Test
```

**BUILD SUCCESSFUL**, 227 numbered Java checks:

| Suite | Pass |
|---|---:|
| CmsSecurityTest | 34 |
| CmsBehaviorTest | 24 |
| LocalDatabaseTest | 9 |
| ConsultationBehaviorTest | 26 |
| AdaptiveConversationTest | 21 |
| InstallationFeeTest | 17 |
| ChatRegistrationTest | 9 |
| ProviderResponseTest | 7 |
| MultiProviderTest | 6 |
| RagBehaviorTest | 34 |
| RagPhase2CorpusTest | 13 |
| RagQualityTest | 27 |

EditorLayoutTest cũng pass (field/group coverage), không tính là một numbered
check trong tổng trên. Lần đầu full suite bị sandbox chặn loopback socket của
HTTP mock; chạy lại trong môi trường có quyền loopback đã pass. Không phải lỗi
Gemini thật. Log offline nằm trong `.build/phase21-full-tests.log` ignored.

Ngoài ra: **17 JSP** CMS/public compile qua Jasper với **0 errors**; **6 frontend
tests pass**, chỉ hiển thị validated customer text và không submit đăng ký qua chat.
Offline regressions cover slang/formal/follow-up/ambiguous/budget/identifiers,
source versions/expiry/supersession/conflicts/metadata, empty/failure/injection,
session isolation, RAG disabled và business fact priority.

`RagPostgresTest` được mở rộng từ 17 lên **24 integration cases và đã compile**,
nhưng **chưa chạy thành công Phase 2.1**. Không tính chúng vào 227 pass, không
lấy kết quả lịch sử Phase 1 làm kết quả của migration 003.

## Database test recovery: hai lựa chọn

### A — Docker, pgvector/PostgreSQL 17 riêng

```powershell
./test/run-rag-isolated.ps1
```

PowerShell 7; Docker Desktop phải hoạt động, image pin đã có sẵn. Script preflight
daemon/image trước khi tạo credential; yêu cầu pull image thủ công nếu chưa có.
Image mặc định giữ nguyên `pgvector/pgvector:0.8.2-pg17` và digest đã pin Phase 1.
Docker client có deadline; bind loopback random port, DB mới `fpt_rag_test`,
credential ngẫu nhiên chỉ lưu file ignored. Script cài extension/chạy 002+003
**chỉ trong container mới**, dùng embedding mock, stop container trong finally.
Không load `.local/runtime.json`, không tạo service cloud.

Trong lần thử này, Docker có thể trả version/context nhưng image/container API
không phản hồi; run timeout trước khi nhận container ID. Chưa có migration/test
DB result. Cần quản trị viên phục hồi Docker Desktop/daemon, không tự restart
toàn daemon vì có thể ảnh hưởng container khác. Các tên test chưa xác nhận sau
timeout cần kiểm tra riêng: `fpt-rag-test-378cc8e126fd` (Phase 2.1),
`fpt-rag-test-7bd40796a8d5` (lần thử Phase 2 trước đó). Không xóa tự động.
Đã thử stop đúng hai tên này với deadline; Docker vẫn không xác nhận trạng thái.

### B — PostgreSQL local riêng có sẵn

```powershell
./test/run-rag-local-postgres.ps1
```

Quản trị viên chuẩn bị **một DB rỗng riêng** tên `fpt_rag_test` trên loopback,
role test riêng và pgvector tương thích. Nạp private process env:
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE`,
`RAG_ENVIRONMENT`, `RAG_TEST_DATABASE_APPROVED`.
Runner yêu cầu development, approved=true, host 127.0.0.1 và đúng test DB name;
refuse nếu public schema có table/view. Không tự lấy website credential.
RLS probe của suite có `CREATE ROLE` cho role test NOLOGIN; cần quyền này trên
cluster local test riêng. Không cấp quyền quản lý role cho backend website.
Sau guard, cài extension và chạy 002+003 trong DB test, rồi chạy suite.
Nếu tables đã tồn tại, script không reset/drop để chạy lại; dùng DB test mới
được duyệt hoặc chỉ run integration suite khi cả ba tables còn rỗng.

Kiểm tra thực tế máy này: service PostgreSQL 18 đang chạy nhưng `vector.control`
và `vector.dll` chưa có; không có pgpass và probe không password bị từ chối.
Chưa xác nhận được DB/role test riêng. **Chưa chạy DDL hoặc integration ở server này**.
Không cài extension binaries, tạo credential hoặc thay cấu hình auth tự động.
Quản trị viên cần cấu hình những mục thiếu riêng cho test hoặc phục hồi lựa chọn A.

## File thay đổi trong Phase 2.1

- `src/java/rag/RetrievalQuery.java` (mới), `KnowledgeSourceSelection.java` (mới).
- `src/java/rag/KnowledgeContentPolicy.java`, `KnowledgeRetrievalService.java`,
  `RagContextBuilder.java`, `KnowledgeIndexingService.java`.
- `src/java/ai/consultation/ConsultationEngine.java`, `ConsultationPolicy.java`,
  `CustomerRequirements.java`.
- `src/java/dto/KnowledgeChunkDTO.java`, `KnowledgeDocumentDTO.java`.
- `src/java/dao/KnowledgeChunkDAO.java`, `KnowledgeDocumentDAO.java`.
- `database/migrations/003-rag-source-policy.sql` (mới).
- `tools/java/rag/RagTool.java`: read-only check cho schema 003.
- `build-portable.xml`: thêm suites corpus/quality vào required offline tests.
- `test/rag/RagBehaviorTest.java`, `RagPhase2CorpusTest.java`, `RagPostgresTest.java`.
- `test/rag/RagQualityTest.java`, `RagLocalDatabaseSetup.java` (mới).
- `test/rag/fixtures/project-knowledge.json`.
- `test/run-rag-phase2-offline.ps1`, `test/run-rag-isolated.ps1`.
- `test/rag-docker-command.ps1`, `test/run-rag-local-postgres.ps1` (mới).
- `docs/rag-phase1.md`, `docs/rag-phase2-validation.md`: đánh dấu báo cáo lịch sử.
- `docs/rag-phase2.1-quality.md` (file này, mới).

Migrations 002, `.env.example`, chat model/provider, encryption/DB credentials,
CMS pages/pricing UI và registration persistence không bị sửa trong Phase 2.1.
Các thay đổi khác đã có từ Phase 1 được giữ nguyên.

## Giới hạn và bước tiếp theo

1. Migration 003, recursive supersession SQL và peer revalidation còn cần chạy
   24 test PostgreSQL thật khi môi trường riêng sẵn sàng; hiện chỉ compile/offline pass.
   Không triển khai hoặc bật RAG trước khi integration thành công.
2. Chọn nguồn bảo thủ: các document cùng authority khác wording có thể cần xác
   nhận dù không thật sự mâu thuẫn; nhiều chủ đề có thể dẫn đến clarification.
   Cần admin gán policy key/authority/supersession rõ ràng, không phán đoán qua score.
3. Context chỉ hai lượt khách và domain labels; tham chiếu phức tạp vẫn cần hỏi lại.
   Chưa có generative query rewrite, semantic conflict detector hoặc shared history DB.
4. Không có benchmark embedding thật: relevance, correct-answer rate, latency,
   usage/cost chưa đo. Threshold/vector weights không được tự điều chỉnh theo mock.
5. Curation và metadata trust vẫn bắt buộc; content filter không chứng minh mọi
   diễn đạt đều đúng. Admin KB publication UI/authenticated controller chưa được xây.

Sau khi integration pass: duyệt tài liệu kỹ thuật thật, xác minh model access/quota/
giá trong project Gemini, nạp key vào process test và phê duyệt budget cụ thể.
Sau đó mới chạy embedding thật + retrieve + chat trên DB test riêng, ghi token/
attempts/latency và đánh giá nguồn. Không tự bật billing, đổi embedding space,
thay chat model hoặc enable RAG trên website.

Rollback code/staging: giữ RAG và embedding-call approval false, dừng worker test;
giữ schema/data, không drop extension/tables. Chưa có production rollout để rollback.
