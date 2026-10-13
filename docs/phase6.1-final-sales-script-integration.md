# Phase 6.1 — Final sales script integration

Date: 2026-10-14 (Asia/Ho_Chi_Minh)  
Branch inspected: `feature/rag-integration`  
Source document: `kịch bản chatbot testfinal.pdf` (8 pages, supplied by the administrator)

## Outcome

The useful, confirmed facts from the final sales script have been integrated into the existing business configuration, Markdown knowledge and deterministic consultation backend. Existing package selection, registration form persistence and RAG architecture were retained. The PDF was treated as business source material; its long prompt and sample conversations were not copied into the system prompt.

## New information added

- Public business configuration now includes the assistant name `Tư vấn viên AI của Phúc`, responsible consultant `Phúc`, chatbot availability `24/24`, direct consultation hours `07:00–22:00` (Vietnam time), and the consultant registration URL.
- The supplied registration URL was checked once on 2026-10-14. `https://fpt.vn/sale/phucbn2-18` returned HTTP 302 from the official `fpt.vn` domain to an FPT AM tracking URL. No registration was submitted. `SiteContent` additionally restricts this field to HTTPS URLs under `fpt.vn/sale/`.
- The prepayment policy confirmed by Phúc on 2026-10-13 is structured in Business settings: a 200,000 VND installation discount for 6 or 12 months prepaid, plus one service month for 12 months prepaid. It applies to standard, F1 and F2 installation totals.
- Conversation memory now keeps the customer's selected 6- or 12-month prepayment option. It does not store contact PII.
- Registration replies identify the base package, included benefits, Mesh option, monthly arithmetic, applicable installation total, official registration channels and the current no-handoff limitation.

## Existing behavior preserved

- Package prices and specifications continue to come from the server package catalog.
- Mesh installation replaces the standard installation fee; the two fees are never added together.
- The existing website registration form remains the only chat action. The chatbot does not submit or persist a lead by itself.
- The backend continues to avoid claiming coverage, installation completion, lead transfer or order confirmation before the corresponding system action succeeds.
- Customer contact details remain outside server-managed consultation memory.
- The source-authority, prompt-injection and unpublished/superseded document protections remain active.

## Contradictions resolved

- The previous Markdown policy said Mesh promotion eligibility was unconfirmed. It now records the 2026-10-13 confirmation: standard installation becomes 100,000 VND, F1 becomes 300,000 VND and F2 becomes 500,000 VND when an eligible 6- or 12-month prepayment option is selected.
- A combined monthly total is no longer silently interpreted as a standalone package. For example, 220K + F1 is rendered as `220,000 + 10,000 = 230,000 VND/month` without camera, while standalone 230K retains its camera. The same rule covers 239K + F1 versus standalone 249K, and 239K + F2 at 259K.
- When 230K or 249K can mean either a standalone package or a remembered Mesh combination and the base package is not known, the server asks for clarification instead of opening registration.
- The old generic statement that Mesh discounts were unknown was removed from deterministic replies and active knowledge.

## Markdown files updated

- `knowledge/policies/contract-payment-safety.md` — version 3, effective 2026-10-13; confirmed prepayment arithmetic and no invented expiry.
- `knowledge/products/wifi-multifloor-coverage.md` — version 3, effective 2026-10-13; confirmed F1/F2 discounted installation totals.
- `knowledge/products/internet-packages.md` — version 2, effective 2026-10-13; base-package/combined-total distinction and transparent arithmetic.
- `knowledge/faq/new-internet-registration.md` — version 3; safe registration channels and explicit meaning of an opened form versus a persisted submission.

Contact values and the official URL stay in approved Business configuration rather than being duplicated in file knowledge. This follows the repository policy that rejects phone numbers and operational configuration from automatically retrieved Markdown.

## Backend classes modified in Phase 6.1

- `src/java/cms/SiteContent.java` — structured, editable Business fields and registration URL validation.
- `src/java/cms/EditorLayout.java` — groups the new assistant, consultant, hours and registration fields in the existing Business editor.
- `src/java/ai/consultation/CustomerRequirements.java` — bounded prepayment memory.
- `src/java/ai/consultation/CommercialPriceValidator.java` — server-side combined-total match validation.
- `src/java/ai/consultation/RecommendedSolution.java` — confirmed promotion calculation and final installation fee.
- `src/java/ai/consultation/ConsultationPolicy.java` — promotion replies, package/combo disambiguation, complete registration summary, public hours and no-handoff statements.
- `src/java/ai/consultation/ConsultationEngine.java` — exposes only the approved new Business fields to the model as server data.

## Commercial policy verification

All customer-visible arithmetic is calculated by Java from the package catalog and approved Business values:

| Scenario | Base installation | Confirmed discount | Final installation |
| --- | ---: | ---: | ---: |
| Standard Internet, prepaid 6/12 months | 300,000 | 200,000 | 100,000 VND |
| Mesh F1, prepaid 6/12 months | 500,000 | 200,000 | 300,000 VND |
| Mesh F2, prepaid 6/12 months | 700,000 | 200,000 | 500,000 VND |

The 12-month option also reports one additional service month. No expiration date is invented. If the structured confirmation fields are absent or invalid, the backend refuses to calculate a discounted total.

## Model configuration review

The PDF proposes Gemini 2.5 Flash with 2,048 output tokens, zero thinking budget and a 4,096-token retry. The repository's current runtime contract instead defaults to primary `gemini-3.8-flash` with `medium` thinking and backup `gemini-3.5-flash-lite` with `low` thinking; the active production model is read from persisted AI settings. Phase 6.1 did not change a model, thinking level, token budget, retry policy or credential. The actual active setting still needs to be checked in the target environment before deployment.

## Focused verification executed

- Phase 6.1 final script acceptance: **10/10 passed**.
- Phase 6 commercial regression: **14/14 passed**.
- Combined Internet + Mesh regression: **10/10 passed**.
- Ant `dist`: **BUILD SUCCESSFUL**; repository system prompt and **18 knowledge files** validated offline.
- External Gemini calls: **0**.
- PostgreSQL/Supabase reads or writes: **0**.
- Registration submissions: **0**.

The focused tests cover promotion eligibility, F1/F2 arithmetic, 230K ambiguity, standalone package identity, transparent combo rendering, registration/handoff honesty, conversation context and Markdown catalog validation.

## RAG sync status

The file catalog parses all 18 Markdown sources and computes content hashes successfully. Version metadata was increased on every changed source. No database sync, embedding generation or production reindex was run, so these changes are **RAG sync ready but not live**. Use only the existing authorized file-sync and changed-document indexing workflow after review; confirm the new document versions and 768-dimensional chunks are ready before enabling customer retrieval.

## Remaining business questions

- The promotion expiration date is still unconfirmed and intentionally remains `none`.
- There is still no confirmed automatic lead handoff. Direct contact and the existing persisted registration form remain the supported flows.
- Address-level service availability requires an actual infrastructure check; a province, ward or commune name alone is not confirmation.
- The official URL responds on the FPT domain, but an end-to-end registration submission was not performed in this offline phase.

## Deployment readiness

**Code and knowledge are ready for review and staging sync. Production is not yet updated.** Before release, verify the active AI settings and Business values in the target environment, run the approved changed-file knowledge sync/reindex, confirm retrieval excludes superseded versions, and smoke-test the registration form without using customer data. Keep a copy of the current deployment artifact and database migration state for rollback. No commit, push or deployment was performed in this phase.

**FINAL SALES KNOWLEDGE INTEGRATED — BACKEND UPDATED — NO DUPLICATE POLICIES.**
