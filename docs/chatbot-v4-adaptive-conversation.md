# V4 adaptive conversation report — 2026-10-10

## Findings and root causes

The existing architecture remains Java Servlets/JSP, a shared `ConsultationEngine`, provider adapters, a database-backed `ProductCatalog`, typed in-memory session requirements, and `web/js/ai-chat.js`. Public chat buffers and validates model output before rendering. Registration uses the existing package form and `ContactServlet`; it does not submit leads from model text.

| Evidence before this change | Cause and scoped fix |
| --- | --- |
| `ConsultationPolicy.known` recognized selected phrases but not camera/TV-only follow-ups, greetings, short device statements or budget increases. | Legitimate turns went to the model or generic fallback. Added conservative conversational signals and requirement-based continuation. Unfamiliar messages still use the provider with history. |
| `CustomerRequirements.update` stored digit-based people counts, no devices, and a limited budget expression. Any camera mention set `camera=true`. | Lost short replies, word-based counts and changed budgets; availability questions became purchase preferences. Added bounded device/count parsing, budget updates, explicit preference handling and negative updates. |
| `ConsultationReply.validateMessage` rejected every message containing camera, WiFi or modem. | Normal benefit questions were invalidated even with a correct schema. Allow questions while continuing to reject benefit assertions, numeric product claims, metadata, secrets, malformed envelopes and unsolicited actions. |
| `ConsultationPolicy.render` accepted `out_of_scope` without checking the current turn's service context. | Model classification could publish a refusal for a valid feature follow-up. Such mismatches now trigger the existing bounded repair/fallback path. |
| `ConsultationPolicy.fallback` returned the same needs-collection message after unknown questions or provider failures. HTTP and frontend errors did not provide the specified contact. | Technical errors were unhelpful and indistinguishable. Known greetings, ambiguity, product information and missing data each have their own responses. Unanswered provider/validation failures explicitly report a technical issue and Phone/Zalo 0932 079 469. |
| `registrationRequested` required narrow wording; the previous selected package survived comparisons. | Direct `Đăng ký 249K` and `Oke chốt luôn` could miss registration; an ambiguous close could reuse an old selection. Added direct/implicit intent, negation checks and ambiguity tracking. Registration still opens the form, requires explicit submission consent and never implies persisted success. |
| The speed branch embedded 239K prices, speed and benefits instead of using the catalog. | A duplicate fact source could disagree with database data. Speed answers now read the catalog. Existing Mesh business logic remains intact. |
| The latest test fixture had 300Mbps for 220K/230K, differing from the V4 request's 500Mbps reference. | Corrected only the test fixture/assertions to the supplied validation reference. No business prices, pricing page or database records changed. |

No live production incident or paid model was exercised. These causes are demonstrated by repository code and deterministic tests; live configuration and model quality still need staging verification.

## Context and tone changes

Typed memory now retains devices, spoken people counts, budget changes, camera/TV preferences, current style, explicit style/address preference, selected-package ambiguity, whether amenities were already asked, and whether a registration form was opened. Style adapts to current language unless the customer explicitly requested a lasting preference. Supported addresses are bounded choices (anh, chị, bạn); no age is inferred. Short answers like `2 máy`, `Có truyền hình và camera`, and `Tăng lên 250 nghìn` update the same private session.

History storage is unchanged: chronological user/assistant pairs, at most 20 messages and 24,000 characters, with typed requirements preserved when pairs are removed. Turn locking, two-hour inactivity retention, 2,000-session capacity, reset/expiry cleanup and frontend in-flight guarding remain. Form-open status is distinct from lead persistence and never represents a successful registration.

The default prompt below is shorter and separates conversational guidance from the response protocol. Saved admin prompts remain editable and are never overwritten during configuration reads. Public chat uses the saved prompt; Playground previews unsaved text. To adopt this default after deployment, back up the current prompt, click **Nạp mẫu FPT**, preview, and save. With custom prompts, the provider still controls wording; registration/human handoff and factual outage fallbacks remain server managed. No extra provider calls or retry budget were introduced.

## Files inspected

- `src/java/ai/AIService.java`; all files under `src/java/ai/consultation/`.
- `src/java/ai/session/ChatSessionData.java`, `ChatSessionManager.java` and the existing session-lifecycle tests.
- `src/java/ai/provider/ProviderDeadline.java`, `GeminiProvider.java`, `OpenAICompatibleProvider.java`, `AnthropicProvider.java` and provider HTTP tests.
- `src/java/controller/AIChatServlet.java`, `AdminPlaygroundServlet.java`, `AdminSettingsServlet.java`, `ContactServlet.java` and registration tests.
- `web/js/ai-chat.js`, `web/view/home.jsp`, existing frontend/browser tests and `test/run-chat-tests.ps1`.

## Files modified or added

- `src/java/ai/consultation/ConversationSignals.java` — new conservative conversational/style signals.
- `src/java/ai/consultation/CustomerRequirements.java` — typed context updates and preferences.
- `src/java/ai/consultation/ConsultationPolicy.java` — greetings, playful replies, clarification, continuation, direct registration, catalog-based speed answers and distinct human/technical handling.
- `src/java/ai/consultation/ConsultationEngine.java` — safe handoff routing, address preference and form-open tracking.
- `src/java/ai/consultation/ConsultationReply.java` — legitimate benefit questions and existing strict safety validation.
- `src/java/ai/consultation/ConsultationPrompt.java` — adaptive default and stable schema protocol.
- `src/java/ai/consultation/ProductCatalog.java` — abbreviated numeric comparisons such as `So sánh 195 với 239`.
- `src/java/controller/AIChatServlet.java`; `web/js/ai-chat.js` — technical fallback contact and retry behavior.
- `test/ai/AdaptiveConversationTest.java` — new V4 behavioral scenarios.
- `test/ai/ConsultationBehaviorTest.java`, `test/controller/ChatRegistrationTest.java`, `test-e2e/chatbot-test.cjs`, `test-e2e/chatbot-browser-test.cjs`, `test/run-chat-tests.ps1` — updated regression coverage/runner.
- `dist/fpt-sale.war` — rebuilt existing deployment artifact.
- `docs/chatbot-v4-adaptive-conversation.md` — this report.

`test-e2e/admin-prompt-browser-test.cjs` was already untracked at the start of this task; it was preserved and executed without modification.

## Executed validation

| Suite | Passed named cases |
| --- | ---: |
| ConsultationBehaviorTest | 26 |
| AdaptiveConversationTest | 21 |
| ChatRegistrationTest | 9 |
| ProviderResponseTest | 7 |
| chatbot-test.cjs | 6 |
| chatbot-browser-test.cjs | 4 |
| admin-prompt-browser-test.cjs | 3 |
| **Total** | **76** |

Tests exercise actual engine output, state updates, provider request/response handling, HTTP error status, mocked persistence/duplicate prevention, safe UI rendering and editable prompt submission. Coverage includes casual/attention greetings, formal tone switching, humor, shorthand comparison, household plus amenities, word counts/device ranges, budget increases, camera availability vs preference, gaming follow-up, direct/implicit registration, ambiguous close, human escalation, unrelated tasks, explicit address preference, incorrect model scope repair, benefit questions vs assertions, outages/malformed output, trimmed memory and customer isolation. All network responses/providers are mocked; no production database, email or AI API was used.

`build.bat dist` completed successfully. `git diff --check` completed without whitespace errors. No JSP markup was changed in V4, so the existing prompt-editor browser regression was reused.

## Local verification

From the repository root:

```powershell
.\test\run-chat-tests.ps1
node test-e2e/chatbot-browser-test.cjs
node test-e2e/admin-prompt-browser-test.cjs
.\build.bat dist
```

The runner accepts `-TomcatHome` for a Tomcat 9 installation. Browser tests use installed Chromium (Chrome by default), with `FPT_TEST_BROWSER_BIN` available as an override. Existing dependencies are reused; no new environment variables or dependencies are required.

## Limitations, deployment and rollback

- Signals deliberately cover common phrasing; arbitrary dialect, bare answers such as `5` without a unit, complex sarcasm and large written budgets may still need provider interpretation or clarification. Mock tests verify behavior, not natural-language model quality.
- Device counts inform consultation, but the site has no verified household-capacity or latency guarantee. Recommendations do not invent such guarantees.
- Context is process-local; restart or multiple instances without sticky sessions can lose history. This infrastructure remains unchanged.
- Custom saved prompts may still conflict with the response contract or tone guidance. Validate them in Playground/staging. Model responses are subject to strict validation and bounded retries; factual fallbacks can have simpler wording than a valid model response.
- Production catalog contents were not queried. Confirm 220K/230K speeds and benefit descriptions against approved business data before release. The existing Mesh configuration was preserved, with no new prices or installation terms introduced.
- No commit, push, merge, deployment or production data modification was performed.

Release only after approval: back up the current WAR/image and saved prompt; stage the rebuilt WAR on the existing infrastructure with existing environment configuration. Use a fresh session, test the example conversations plus registration form consent and two-session isolation, and verify the live provider/model and approved package records. Preview/save the default prompt only if desired. Then release the approved artifact and monitor errors, bounded retry behavior and handoff correctness without logging customer PII or keys.

Rollback by redeploying the saved pre-V4 WAR/image. If an administrator changed the saved prompt, restore its backup separately through settings. No database migration needs rollback. Restarting clears process-local chat memory, so test with fresh sessions after either release or rollback.

## Improved default system prompt

```text
Bạn là tư vấn viên FPT Telecom Gia Lai. Trả lời tự nhiên bằng tiếng Việt, mặc định xưng em và gọi anh/chị.
Điều chỉnh theo ngôn ngữ hiện tại và cách xưng hô khách yêu cầu, không suy đoán tuổi. Thân thiện khi khách nói thoải mái; lịch sự khi khách nói trang trọng; hỏi giá, so sánh và đăng ký thì trả lời trực tiếp. Hài hước nhẹ, không trêu khách hay lạm dụng emoji.
Hiểu chào hỏi, tiếng lóng, câu ngắn và đùa vô hại. Dùng lịch sử cùng yêu cầu đã ghi nhận để hiểu câu tiếp theo. Không lặp chào, hỏi lại thông tin đã có hay thúc ép mua. Chưa rõ tham chiếu thì hỏi một câu ngắn.
Tư vấn theo danh mục máy chủ: một gói phù hợp và một lựa chọn khác nếu hữu ích. So sánh đúng các gói khách hỏi. Không tự suy ra phí, ưu đãi, hạ tầng, điều khoản, xếp hạng bán chạy hay bảo đảm ping/phủ sóng. Khi thiếu dữ liệu, nói rõ.
Chỉ từ chối ngắn gọn các công việc thực sự ngoài dịch vụ; không từ chối lời chào hay câu tiếp nối về gói mạng. Yêu cầu cần người thật thì hướng dẫn liên hệ; không nói đã gọi lại, gửi tin hay đăng ký thành công. Yêu cầu đăng ký sẽ do máy chủ mở biểu mẫu và kiểm tra sự đồng ý riêng.
```

The application protocol appended separately requires the existing five-field JSON envelope, validated Vietnamese message text, package IDs for server-rendered facts, and `action=none` from the model. Server code controls registration intent and consent.
