# Phase 5.5 — Complete Admin UI/UX Reorganization & GitHub Auto-Deploy Readiness

## 1. Current Admin UI/UX Issues Found
During the Phase 5.5 audit of the FPT Sale Manager Admin CMS (`web/view/admin/*` and `web/assets/admin/*`), the following visual clutter and information architecture issues were identified:
1. **Sidebar Group Mixing (`admin-header.jsp`)**:
   - Previously, the left sidebar mixed customer management (`Khách hàng`, `Thông báo & Email`) inside the `Quản lý Website` group alongside `Nội dung trang`, `Gói cước / Sản phẩm`, `Media / Hình ảnh`, and `Thông tin kinh doanh`.
   - The 9-item `AI & Chatbot` submenu defaulted to expanded on every page visit, pushing `Hệ thống` links off-screen on shorter viewports.
2. **Knowledge Base Diagnostic Overload (`knowledge-base.jsp`)**:
   - The 7-card technical readiness grid (`#adminStatusPanel`) and 5 operational dimension pills were rendered unconditionally above every operational tab (`documents`, `upload`, `imports`, `faqs`, `indexing`, `rag-config`, `audit`, `playground`), forcing administrators to scroll past ~600px of infrastructure diagnostics before reaching primary actions.
   - The AI Dashboard (`tab=dashboard`) displayed 9 separate metric cards plus 7 status cards simultaneously (`16` cards above the fold).
3. **Dense Multi-Field Forms Without Progressive Disclosure**:
   - **Document Editor (`tab=documents`)**: Displayed 12 form fields (`title`, `category`, `source`, `sourceAuthority`, `policyKey`, `supersedesDocumentId`, `tags`, `department`, `chunkSize`, `chunkOverlap`, `changeNote`, `preservePublishedState`) at once alongside revision tables and diffs.
   - **Upload Form (`tab=upload`)**: Displayed optional metadata (`policyKey`, `tags`, `department`) inline with primary file upload fields, without a visual step indicator connecting `Upload` (`?tab=upload`) and `Section Review` (`?tab=imports`).
   - **FAQ Management (`tab=faqs`)**: Lacked a quick search and status filter bar above the FAQ table, and displayed optional metadata (`sourceAuthority`, `tags`, `policyKey`) inline with core question/answer fields.
   - **Indexing & Sync (`tab=indexing`) & Playground (`tab=playground`)**: Rendered `knowledge-gemini-runtime.jsp` (lease recovery and live quota forms) and raw `constructedContextJson` inline without progressive disclosure.
   - **Audit Logs (`tab=audit`)**: Displayed raw `previousState` and `newState` strings inline without filter controls (`auditResource`, `auditAction`, `auditPage`) in the UI despite backend support in `AdminKnowledgeBaseServlet`.

---

## 2. New Sidebar Information Architecture (5 Logical Groups)
[`web/view/admin/admin-header.jsp`](file:///d:/miniproject/fpt/web/view/admin/admin-header.jsp) and [`web/assets/admin/admin-cms.css`](file:///d:/miniproject/fpt/web/assets/admin/admin-cms.css) were reorganized into 5 distinct, collapsible logical groups:

| Group | Group Label | Menu Items & Routes | Behavior |
| :--- | :--- | :--- | :--- |
| **Group 1** | **Tổng quan** | • `Dashboard` (`/admin/dashboard`) | Always accessible primary overview |
| **Group 2** | **Quản lý Website** | • `Nội dung trang` (`/admin/content`)<br>• `Gói cước / Sản phẩm` (`/admin/packages`)<br>• `Media / Hình ảnh` (`/admin/content#cms-images`)<br>• `Thông tin kinh doanh` (`/admin/business`) | Collapsible group header (`data-group-toggle="website"`) |
| **Group 3** | **Khách hàng** | • `Danh sách khách hàng` (`/admin/customers`)<br>• `Thông báo / Lịch sử Email` (`/admin/email-logs`, guarded by `${!demoMode}`) | Dedicated customer operations group (`data-group-toggle="customers"`) |
| **Group 4** | **AI & Chatbot** | • `Tổng quan AI` (`/admin/knowledge-base?tab=dashboard`)<br>• `Tài liệu / Knowledge Base` (`/admin/knowledge-base?tab=documents`)<br>• `Upload & Import` (`/admin/knowledge-base?tab=upload`)<br>• `FAQ` (`/admin/knowledge-base?tab=faqs`)<br>• `Indexing` (`/admin/knowledge-base?tab=indexing`)<br>• `RAG Playground` (`/admin/knowledge-base?tab=playground`)<br>• `AI Model Settings` (`/admin/settings`)<br>• `RAG Settings` (`/admin/knowledge-base?tab=rag-config`)<br>• `Monitoring / Audit` (`/admin/knowledge-base?tab=audit`) | Collapsible submenu (`#sidebarGroupAi`), guarded by `${!demoMode}`; auto-expands **only** when an AI/KB route is active or when toggled open by the admin |
| **Group 5** | **Hệ thống** | • `Cài đặt chung` (`/admin/business#cms-contact`)<br>• `Quản lý tài khoản` (`/admin/users`, guarded by `${!demoMode}`)<br>• `Đăng xuất` (`/logout`) | System & account controls (`data-group-toggle="system"`) |

Technical capabilities preserved and enhanced:
- Active route highlighting (`.sidebar-link.active`) and secondary hash-target highlighting (`.hash-active`).
- Desktop icon-only rail collapse (`#adminSidebarCollapseBtn`, `68px` width) with floating `data-tooltip` labels.
- Tablet/mobile slide-out drawer (`#adminMobileMenuBtn`, `#adminSidebarBackdrop`, `Escape` key handler).
- Independent vertical scroll inside `.admin-sidebar-nav` and build metadata footer (`#adminBuildMeta`).

---

## 3. Knowledge Base Layout Reorganization & Progressive Disclosure
[`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp), [`web/view/admin/knowledge-gemini-runtime.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-gemini-runtime.jsp), [`web/view/admin/knowledge-import.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-import.jsp), and [`web/assets/admin/admin-kb.css`](file:///d:/miniproject/fpt/web/assets/admin/admin-kb.css) were restructured around progressive disclosure:

1. **Tổng quan AI (`tab=dashboard`)**:
   - Displays a clean **6-Card Summary KPI Grid** (`.kb-metrics-grid-6`):
     1. `1. Đã duyệt (Approved)`
     2. `2. Đã xuất bản (Published)`
     3. `3. Đã lập chỉ mục (Indexed)`
     4. `4. Chờ / Lỗi xử lý (Pending / Failed)`
     5. `5. Trạng thái RAG (RAG Status)`
     6. `6. Gemini Runtime Status`
   - The 7-component technical readiness grid (`#adminStatusPanel`) is housed inside `<details class="kb-panel kb-readiness-panel kb-advanced-details" ${kbTab == 'dashboard' ? 'open' : ''}>`: open on `dashboard` for full visibility, and collapsed into a compact 1-line summary bar on all other operational tabs.
2. **Tài liệu (`tab=documents`)**:
   - Top search & filter bar (`q`, `category`, `workflowStatus`, `indexingFilter`).
   - Primary document editor shows core fields (`title`, `category`, `source`, `sourceAuthority`, `content`) while placing 8 advanced fields (`policyKey`, `supersedesDocumentId`, `tags`, `department`, `chunkSize`, `chunkOverlap`, `changeNote`, `preservePublishedState`) inside `<details class="kb-advanced-details">`.
   - Right workflow panel adds a 5-step visual lifecycle stepper (`1. DRAFT → 2. PENDING_REVIEW → 3. APPROVED → 4. PUBLISHED → 5. INDEXED`) and collapsible revision diff view.
3. **Upload & Import (`tab=upload` & `tab=imports`)**:
   - Added a unified **6-Step Workflow Stepper** (`.kb-workflow-stepper`): `1. Chọn tệp → 2. Tải lên & Trích xuất → 3. Review phân mục → 4. Phê duyệt → 5. Xuất bản → 6. Lập chỉ mục`.
   - Optional upload metadata (`policyKey`, `tags`, `department`) moved into `<details class="kb-advanced-details">`.
4. **FAQ (`tab=faqs`)**:
   - Added instant client-side search (`#faqQuickSearch`) and status filter (`#faqStatusFilter`) above the FAQ table (`#kbFaqTable`).
   - Optional FAQ metadata (`sourceAuthority`, `tags`, `policyKey`) placed inside `<details class="kb-advanced-details">`.
5. **Indexing (`tab=indexing`)**:
   - Added a 4-card Indexing Summary Strip (`Ready`, `Pending / Stale`, `Failed`, `Active Provider`) above the batch control and stale documents table.
   - Wrapped advanced Gemini runtime & lease recovery controls (`knowledge-gemini-runtime.jsp`) inside `<details class="kb-panel kb-advanced-details" id="kbGeminiRuntimeAdvanced">`.
6. **RAG Playground (`tab=playground`)**:
   - Primary view focuses on query input, grounding/selection status, mock answer, and candidate chunks table.
   - Raw `constructedContextJson` (`RagContextBuilder`) and live Gemini preview controls are neatly tucked inside `<details class="kb-advanced-details">`.
7. **Monitoring & Audit (`tab=audit`)**:
   - Added resource/action filter bar (`auditResource`, `auditAction`) and pagination links (`auditPage`) above the audit table, and wrapped `previousState` / `newState` payloads in compact `<details class="kb-audit-state-details">` disclosures.

---

## 4. Modified Files List
1. [`web/view/admin/admin-header.jsp`](file:///d:/miniproject/fpt/web/view/admin/admin-header.jsp) — 5-group sidebar architecture, active-only default expansion for AI submenu, collapsible group toggles.
2. [`web/assets/admin/admin-cms.css`](file:///d:/miniproject/fpt/web/assets/admin/admin-cms.css) — Collapsible sidebar group header buttons, carets, dividers, and responsive rules.
3. [`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp) — 6-KPI AI overview, collapsible 7-card readiness diagnostics, workflow steppers, progressive disclosure forms, FAQ quick filter, and Audit Log filter/pagination UI.
4. [`web/view/admin/knowledge-gemini-runtime.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-gemini-runtime.jsp) — Progressive disclosure `<details>` wrapper for lease recovery and Gemini quota controls.
5. [`web/view/admin/knowledge-import.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-import.jsp) — 6-step Upload & Import workflow progression header and clean section review disclosures.
6. [`web/assets/admin/admin-kb.css`](file:///d:/miniproject/fpt/web/assets/admin/admin-kb.css) — Styles for `.kb-metrics-grid-6`, `.kb-advanced-details`, `.kb-workflow-stepper`, and responsive breakpoints.
7. [`test/rag/RagAdminKbTest.java`](file:///d:/miniproject/fpt/test/rag/RagAdminKbTest.java) — Added Phase 5.5 verification test for the 5-group sidebar and KB progressive disclosure structure.
8. [`test/cms/CmsBrowserServer.java`](file:///d:/miniproject/fpt/test/cms/CmsBrowserServer.java) & [`test-e2e/cms-browser-test.cjs`](file:///d:/miniproject/fpt/test-e2e/cms-browser-test.cjs) — Mounted `/admin/knowledge-base` in the offline browser server and verified `1440px`, `1024px`, `768px`, and `375px` responsive layouts across CMS and KB tabs.

---

## 5. Preserved Features & Route Matrix
100% of existing routes, servlet actions, form field names, CSRF tokens, RBAC checks, and Demo Mode guards remain intact:
- `/admin/dashboard`, `/admin/content`, `/admin/business`, `/admin/packages`, `/admin/customers`, `/admin/customer-detail`, `/admin/email-logs`, `/admin/settings`, `/admin/users`, `/admin/media`, `/admin/knowledge-base`, `/logout`.
- All `AdminKnowledgeBaseServlet` POST actions (`createDocument`, `updateDocument`, `submitReview`, `approveDocument`, `rejectDocument`, `publishDocument`, `unpublishDocument`, `archiveDocument`, `rollbackDocument`, `reindexDocument`, `runOfflineIndexingBatch`, `recoverExpiredLeases`, `requestGeminiIndex`, `uploadDocument`, `prepareImport`, `reviewImportSection`, `createImportDraft`, `createFaq`, `updateFaq`, `submitFaqReview`, `approveFaq`, `publishFaq`, `unpublishFaq`, `archiveFaq`, `saveRagConfig`, `runPlaygroundQuery`, `runGeminiPreview`).

---

## 6. Responsive Verification Results (1440px / 1024px / 768px / 375px)
Verified via headless Chromium in [`test-e2e/cms-browser-test.cjs`](file:///d:/miniproject/fpt/test-e2e/cms-browser-test.cjs):
- **`1440px` (Desktop)**: Full `256px` left sidebar with 5 groups, collapsible into `68px` icon rail; zero horizontal overflow (`scrollWidth <= innerWidth + 1`) across `/home`, `/admin/content`, `/admin/business`, and `/admin/knowledge-base?tab=dashboard..audit`.
- **`1024px` (Tablet Landscape)**: Sidebar transitions cleanly to slide-out drawer with topbar toggle button (`#adminMobileMenuBtn`); `.kb-grid-2` collapses to single column; `.kb-metrics-grid-6` adapts to 2 columns; zero horizontal overflow.
- **`768px` (Tablet Portrait)**: Compact topbar, scrollable `.kb-table-wrap` tables, drawer backdrop overlay; zero horizontal overflow.
- **`375px` (Mobile)**: Single-column cards and forms, full-height drawer navigation; zero horizontal overflow.

---

## 7. Build & Test Results
All offline verification suites passed with `0` errors and `0` external API calls:
- `ant -f build-portable.xml war test`: **PASS** (`RagBehaviorTest`: 35/35, `RagPhase2CorpusTest`: 13/13, `RagQualityTest`: 27/27, `RagAdminKbTest`: 23/23).
- `./test/compile-cms-jsps.ps1`: **PASS** (`32/32` JSPs compiled with `0` errors).
- `./test/test-jstl-runtime.ps1`: **PASS** (WAR JSTL runtime compatibility verified).
- `./test/run-rag-phase4-offline.ps1`: **PASS** (`46/46` runtime safety and quota gate tests passed; `real API requests=0`).
- `./test/run-rag-phase316-offline.ps1`: **PASS** (`53/53` Vietnamese policy & section import tests + `23/23` Admin KB tests passed; `0 real API calls`).
- `./test/run-cms-tests.ps1 -Browser`: **PASS** (`CmsSecurityTest`: 34/34, `CmsBehaviorTest`: 24/24, `LocalDatabaseTest`: 9/9, `EditorLayoutTest`: PASS, Playwright E2E Browser Suite: PASS across `1440px`, `1024px`, `768px`, `375px`).

---

## 8. Git Branch, Commit & Push Status
- **Repository**: `https://github.com/Gao2511/fpt.git`
- **Working Branch**: `feature/rag-integration`
- **Commit & Push Details**: Recorded after staging only verified Phase 5.4/5.5 files (no temporary build outputs, no `.env` or secret files).

---

## 9. Render Auto-Deploy Impact Assessment
- The `Dockerfile` uses a multi-stage JDK 17 build (`eclipse-temurin:17-jdk` -> `sh tools/bootstrap-libs.sh` -> `ant -f build-portable.xml war`) and deploys the compiled `ROOT.war` into `tomcat:9.0-jre17-temurin`.
- Public customer website pages (`home.jsp`, `package-detail.jsp`, etc.) were **not** modified.
- `RAG_ENABLED` remains `false` by default in code, database migration `004`/`007`, and `AdminKnowledgeBaseServlet.saveRagConfig()`.
- Pushing to the branch connected to Render triggers a clean container rebuild with the reorganized Admin CMS UI while keeping public customer RAG locked off.

---

## 10. Post-Deploy Verification Checklist
1. Open `https://fpt.gialai.vn/login` and sign in as an Administrator.
2. Verify the left sidebar displays the 5 groups (`Tổng quan`, `Quản lý Website`, `Khách hàng`, `AI & Chatbot`, `Hệ thống`) and `#adminBuildMeta` shows the latest commit SHA.
3. Navigate to `/admin/knowledge-base?tab=dashboard` and verify the 6 summary KPI cards and the collapsible 7-card readiness diagnostic panel (`#adminStatusPanel`).
4. Navigate through `Tài liệu`, `Upload & Import`, `FAQ`, `Indexing`, `RAG Playground`, `RAG Settings`, and `Monitoring / Audit` and verify that `#adminStatusPanel` is collapsed by default and advanced form fields expand cleanly on click.
5. Verify on mobile/tablet viewport (`<= 1024px`) that the hamburger menu (`#adminMobileMenuBtn`) opens and closes the sidebar drawer smoothly.

---

## 11. Rollback Plan
If any UI regression is observed after deployment:
1. Revert the Phase 5.5 commit via `git revert <commit-sha>` and push to the deployment branch, or
2. In the Render Dashboard, select the previous known-good deploy (`1fdc8a7`) and click **Rollback to this deploy**.
Because Phase 5.5 introduces zero database schema changes and zero backend route breaking changes, rollback is instantaneous and stateless.

---

## 12. Remaining Items Before Enabling Real Gemini / Public RAG
1. **Render Environment Variables**: Confirm `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE=require`, `RAG_ENVIRONMENT=PRODUCTION`, `RAG_RUNTIME_MODE=OFFLINE_MOCK` (or `PRODUCTION` when authorized), and `RAG_ENABLED=false` are configured in Render.
2. **Controlled 4-Request Live Smoke Validation**: Only when the administrator explicitly sets `"approved": true` and `"executionAuthorized": true` in `config/phase52-online-smoke-plan.json` (with fresh quota timestamps and separate `RAG_EMBEDDING_API_KEY` / `GEMINI_API_KEY`).
3. **Public RAG Gate**: Keep `RAG_ENABLED=false` until the corpus is reviewed, indexed in production, and explicitly authorized by the administrator.
