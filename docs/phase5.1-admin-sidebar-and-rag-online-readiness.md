# PHASE 5.1 — ADMIN SIDEBAR REDESIGN & ONLINE RAG READINESS REPORT

- **Project:** `D:\miniproject\fpt` (FPT Telecom Gia Lai)
- **Production URL:** `https://fpt.gialai.vn/` (Render)
- **Working Branch:** `feature/rag-integration`
- **Execution Mode:** Local Development & Offline Readiness Verification (`0` Live Gemini API Calls, `0` Production Database Writes)

---

## 1. Current Admin Navigation Audit

### 1.1 Existing Navigation & Layout Architecture (Pre-Phase 5.1)
Prior to Phase 5.1, the Admin CMS used a horizontal top navigation bar defined in [`web/view/admin/admin-header.jsp`](file:///d:/miniproject/fpt/web/view/admin/admin-header.jsp) and styled by [`web/assets/admin/admin-cms.css`](file:///d:/miniproject/fpt/web/assets/admin/admin-cms.css):
- All top-level admin links (`Tổng quan`, `Nội dung trang`, `Thông tin kinh doanh`, `Gói cước`, `Đăng ký tư vấn`, `Lịch sử Email`, `Cài đặt & AI`, `Knowledge Base & RAG`, `AI Playground`, `Tài khoản`) were crowded into a single wrapping horizontal row (`.header-nav`).
- As Phase 3 and Phase 4 added the 9-tab **AI Knowledge Base & RAG Management System** ([`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp)) and **AI Playground** ([`web/view/admin/ai-playground.jsp`](file:///d:/miniproject/fpt/web/view/admin/ai-playground.jsp)), the horizontal bar wrapped across multiple lines on desktop and became difficult to scan on tablet/mobile screens.
- All 12 Admin JSP views include `/view/admin/admin-header.jsp` with `<jsp:param name="activeTab" value="..."/>`:
  1. [`web/view/admin/dashboard.jsp`](file:///d:/miniproject/fpt/web/view/admin/dashboard.jsp) (`activeTab=dashboard`)
  2. [`web/view/admin/content.jsp`](file:///d:/miniproject/fpt/web/view/admin/content.jsp) (`activeTab=content` or `business`)
  3. [`web/view/admin/packages.jsp`](file:///d:/miniproject/fpt/web/view/admin/packages.jsp) (`activeTab=packages`)
  4. [`web/view/admin/package-form.jsp`](file:///d:/miniproject/fpt/web/view/admin/package-form.jsp) (`activeTab=packages`)
  5. [`web/view/admin/customers.jsp`](file:///d:/miniproject/fpt/web/view/admin/customers.jsp) (`activeTab=customers`)
  6. [`web/view/admin/customer-detail.jsp`](file:///d:/miniproject/fpt/web/view/admin/customer-detail.jsp) (`activeTab=customers`)
  7. [`web/view/admin/email-logs.jsp`](file:///d:/miniproject/fpt/web/view/admin/email-logs.jsp) (`activeTab=email-logs`)
  8. [`web/view/admin/settings.jsp`](file:///d:/miniproject/fpt/web/view/admin/settings.jsp) (`activeTab=settings`)
  9. [`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp) (`activeTab=knowledge-base`, with internal `kbTab` sub-routes)
  10. [`web/view/admin/ai-playground.jsp`](file:///d:/miniproject/fpt/web/view/admin/ai-playground.jsp) (`activeTab=ai-playground`)
  11. [`web/view/admin/users.jsp`](file:///d:/miniproject/fpt/web/view/admin/users.jsp) (`activeTab=users`)
  12. [`web/view/admin/user-form.jsp`](file:///d:/miniproject/fpt/web/view/admin/user-form.jsp) (`activeTab=users`)

### 1.2 Security & Demo Mode Constraints Preserved
In [`src/java/filter/AdminFilter.java`](file:///d:/miniproject/fpt/src/java/filter/AdminFilter.java):
- Unauthenticated requests to `/admin/*` redirect `302` to `/login`.
- Direct requests to `/view/admin/*.jsp` return `403 Forbidden`.
- All state-changing `POST` requests require a valid session CSRF token (`csrf_token` parameter or `X-CSRF-Token` header) verified by [`src/java/security/AdminSecurity.java`](file:///d:/miniproject/fpt/src/java/security/AdminSecurity.java).
- In **Demo Mode** (`role = demo_admin`, `demoMode = true`), only read-only fixture routes (`/admin/dashboard`, `/admin/content`, `/admin/business`, `/admin/packages`, `/admin/customers`, `/admin/customer-detail`) are permitted; routes such as `/admin/knowledge-base`, `/admin/ai-playground`, `/admin/settings`, `/admin/users`, and `/admin/email-logs` return `403` by design and are hidden from the sidebar when `demoMode` is active.

---

## 2. New Admin Left Sidebar Architecture

The Admin navigation in [`web/view/admin/admin-header.jsp`](file:///d:/miniproject/fpt/web/view/admin/admin-header.jsp) and [`web/assets/admin/admin-cms.css`](file:///d:/miniproject/fpt/web/assets/admin/admin-cms.css) has been redesigned into a two-part SaaS Admin shell:

1. **Collapsible Fixed Left Sidebar (`aside#adminSidebar.admin-sidebar`)**:
   - **Expanded width (Desktop `> 1024px`)**: `256px` (`body.has-admin-sidebar` applies `padding-left: 256px`).
   - **Collapsed width (Desktop `> 1024px`)**: `68px` (`body.has-admin-sidebar.admin-sidebar-collapsed` applies `padding-left: 68px`), toggled via `#adminSidebarCollapseBtn` and persisted in `localStorage.getItem('fpt.admin.sidebar.collapsed')`.
   - **Tablet / Mobile Drawer (`<= 1024px`)**: Off-canvas drawer (`transform: translateX(-105%)`) opened via `#adminMobileToggle` in the top bar, with a backdrop overlay (`#adminSidebarOverlay`), `Escape` key closing, and automatic close on link selection.
   - **FPT Telecom Dark + Orange Brand Styling**: Deep slate surface (`#0b1120` to `#0f172a`), active route accent bar and gradient highlight (`#f37021` / `#ff8c42`), and crisp SVG iconography.

2. **Sticky Top Header Bar (`header.admin-header.admin-topbar`)**:
   - Displays the mobile drawer hamburger button (`#adminMobileToggle`), current section group + page title breadcrumb, runtime status pill (`Production Safe` or `Demo Read-Only`), quick action link (`Xem trang chủ`), authenticated administrator badge, and logout button.

---

## 3. Updated Admin Menu Tree

```text
FPT Telecom — Admin CMS & AI Console
├── 1. Dashboard
│   └── Tổng quan                        -> /admin/dashboard
│
├── 2. Quản lý Website
│   ├── Nội dung trang                   -> /admin/content
│   ├── Gói cước / Sản phẩm              -> /admin/packages
│   ├── Media / Hình ảnh                 -> /admin/content#cms-images
│   ├── Thông tin kinh doanh             -> /admin/business
│   ├── Khách hàng                       -> /admin/customers
│   └── Thông báo & Email [Full Admin]   -> /admin/email-logs
│
├── 3. AI & Chatbot [Full Admin — Expandable Submenu]
│   ├── Tổng quan AI                     -> /admin/ai-playground
│   ├── Knowledge Base                   -> /admin/knowledge-base?tab=dashboard
│   ├── Tài liệu & Upload                -> /admin/knowledge-base?tab=documents (active on documents, upload, imports)
│   ├── FAQ Management                   -> /admin/knowledge-base?tab=faqs
│   ├── Indexing & Sync                  -> /admin/knowledge-base?tab=indexing
│   ├── RAG Playground                   -> /admin/knowledge-base?tab=playground
│   ├── AI Model Settings                -> /admin/settings#ai-settings
│   ├── RAG Settings                     -> /admin/knowledge-base?tab=rag-config
│   └── Monitoring & Audit Logs          -> /admin/knowledge-base?tab=audit
│
└── 4. Hệ thống
    ├── Cài đặt chung [Full Admin]       -> /admin/settings
    ├── Quản lý tài khoản [Full Admin]   -> /admin/users
    └── Đăng xuất                        -> /logout
```

---

## 4. UI/UX & Responsive Verification

- **Desktop (`1440px`)**: Left sidebar fixed at `256px` (or `68px` when collapsed); sticky top header bar and main content area (`main.admin-main`) align cleanly with zero horizontal scroll (`document.documentElement.scrollWidth <= innerWidth`).
- **Tablet (`768px`) & Mobile (`375px`)**: Body left padding resets to `0`; sidebar becomes an off-canvas slide-out drawer (`width: 272px`, `max-width: 86vw`) triggered by `#adminMobileToggle`.
- **Active State Highlighting**:
  - Top-level routes highlight via `activeTab` (`dashboard`, `content`, `business`, `packages`, `customers`, `email-logs`, `settings`, `users`).
  - AI & Chatbot routes automatically expand `<details id="sidebarAiGroup">` when `activeTab` is `knowledge-base` or `ai-playground`, and highlight the exact sub-item matching `kbTab` (`dashboard`, `documents`/`upload`/`imports`, `faqs`, `indexing`, `playground`, `rag-config`, `audit`).

---

## 5. Root Cause Analysis: Why Knowledge Base / RAG Appears OFFLINE on Render

Auditing the production deployment pipeline and runtime classes revealed **5 distinct root causes** explaining why the Admin Knowledge Base and RAG system show `OFFLINE / DISABLED` on Render (`https://fpt.gialai.vn/`):

1. **Root Cause 1 — Hardcoded Safe Defaults in [`Dockerfile`](file:///d:/miniproject/fpt/Dockerfile#L14)**:
   - Line 14 of `Dockerfile` explicitly sets:
     ```dockerfile
     ENV RAG_ENABLED=false RAG_RUNTIME_MODE=OFFLINE_MOCK RAG_EMBEDDING_CALLS_APPROVED=false RAG_LOCAL_GEMINI_AUTHORIZED=false
     ```
   - Additionally, line 5 (`COPY dist/fpt-sale.war /usr/local/tomcat/webapps/ROOT.war`) deploys the pre-built `dist/fpt-sale.war` committed in Git.

2. **Root Cause 2 — Previously Static Offline Banner in [`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp)**:
   - Previously, lines 28–31 and 155–157 of `knowledge-base.jsp` rendered static text (`● OFFLINE TEST MODE — 0 GEMINI API CALLS | Production RAG: LOCKED OFF`) regardless of actual database or environment configuration. (Resolved in Phase 5.1 by wiring dynamic `systemStatus` indicators.)

3. **Root Cause 3 — Intentional Server-Side Production Release Gates**:
   - [`src/java/dto/AdminKnowledgeDTO.java`](file:///d:/miniproject/fpt/src/java/dto/AdminKnowledgeDTO.java#L411): `RagAdminSettings` constructor hard-locks `this.productionRagEnabled = false;`.
   - [`src/java/dao/AdminKnowledgeDAO.java`](file:///d:/miniproject/fpt/src/java/dao/AdminKnowledgeDAO.java#L1395-L1403): `saveRagSettings()` throws `SecurityException` if `productionRagEnabled=true` is submitted, and persists `rag_production_enabled = "false"`.
   - [`src/java/ai/AIService.java`](file:///d:/miniproject/fpt/src/java/ai/AIService.java#L194-L214): Public customer chat (`askAI`) calls `KnowledgeRetrievalService.disabled()` unless `GeminiRuntime.active()` is true.
   - [`src/java/rag/runtime/GeminiRuntimeConfig.java`](file:///d:/miniproject/fpt/src/java/rag/runtime/GeminiRuntimeConfig.java#L44-L82): Enforces `RAG_RUNTIME_MODE=LOCAL_GEMINI`, `RAG_ENABLED=false`, `DB_HOST=127.0.0.1`, and `DB_NAME=fpt_rag_test`. It intentionally rejects remote hosts (`production.invalid` / Supabase) to prevent accidental live API/DB consumption during testing.

4. **Root Cause 4 — Unapplied Supabase Migrations (`002`–`006`) & `pgvector` Extension**:
   - The Admin Knowledge Base requires PostgreSQL `vector` extension (`pgvector`) and migrations [`002-rag-mvp.sql`](file:///d:/miniproject/fpt/database/migrations/002-rag-mvp.sql), [`003-rag-source-policy.sql`](file:///d:/miniproject/fpt/database/migrations/003-rag-source-policy.sql), [`004-admin-knowledge-base.sql`](file:///d:/miniproject/fpt/database/migrations/004-admin-knowledge-base.sql), [`005-knowledge-document-imports.sql`](file:///d:/miniproject/fpt/database/migrations/005-knowledge-document-imports.sql), and [`006-rag-gemini-runtime.sql`](file:///d:/miniproject/fpt/database/migrations/006-rag-gemini-runtime.sql). Until these are applied on the target PostgreSQL/Supabase instance, KB tables do not exist.

5. **Root Cause 5 — Missing Render Environment Variables & Ephemeral Filesystem**:
   - Render does not have `RAG_EMBEDDING_API_KEY` or `RAG_CHAT_TEST_API_KEY` set by default.
   - Furthermore, uploaded source files (`knowledge_files`) are stored on the container filesystem (`DocumentExtractionService.defaultStorageRoot()`), which is ephemeral across Render container restarts unless backed by persistent storage or database byte storage (while extracted text in `knowledge_documents` and `knowledge_import_sections` is persisted in PostgreSQL).

---

## 6. Operational Status Matrix

### 6.1 Five Independent Operational Dimensions
The system now tracks and displays 5 independent dimensions:
1. **AI Chatbot Enabled** (`ai_chatbot_enabled` in `settings` table)
2. **Customer RAG Enabled** (`RAG_ENABLED` env + `rag_production_enabled` release gate)
3. **Gemini Indexing Mode** (`LOCAL_GEMINI` verified runtime vs `OFFLINE_MOCK` deterministic 768d vectors)
4. **Knowledge Base Database Connected** (PostgreSQL connection + `pgvector` + migrations `002`–`004`+)
5. **Production Release Authorization** (Server-side hard-lock gate)

### 6.2 Six-Component Status Matrix
| Component | State 1 (Healthy/Ready) | State 2 (Configured / Pending) | State 3 (Disabled /Missing) | State 4 (Error / Failed) |
| :--- | :--- | :--- | :--- | :--- |
| **1. Gemini Chat** | `Connected` (Journal `COMPLETED` evidence) | `Not Verified` (Key + `gemini-3.8-flash` set, 0 live calls) | `Configuration Missing` (No API key) | `Error` (Journal `FAILED` / `UNCERTAIN`) |
| **2. Gemini Embedding** | `Connected` (Journal `COMPLETED` + `768d` verified) | `Not Verified` (`RAG_EMBEDDING_API_KEY` set, unverified) | `Configuration Missing` (Missing key; uses Mock offline) | `Error` (Invalid model/dim or journal failure) |
| **3. Knowledge Base** | `Database Ready` (`pgvector` + migrations `002`–`004`+) | `Migration Missing` (DB reachable, missing `pgvector` or `002`–`006`) | — | `Error` (PostgreSQL connection failed) |
| **4. RAG** | `Enabled` (Authorized & enabled) | `Blocked by Release Gate` (`RAG_ENABLED=false` / hard-locked) | `Disabled` (All RAG & local indexing off) | — |
| **5. Indexing** | `Ready` (DB ready, 0 failed/waiting docs) | `Processing` / `Waiting for Quota` | — | `Failed` (DB missing or failed document index jobs) |
| **6. Backup Model** | `Ready` (Verified fallback) | `Unverified` (`gemini-3.5-flash-lite` enabled, unverified) | `Disabled` (`ai_fallback_enabled=false`) | `Error` |

---

## 7. Admin Status Panel Improvements

Implemented in [`src/java/dto/AdminKnowledgeDTO.java`](file:///d:/miniproject/fpt/src/java/dto/AdminKnowledgeDTO.java#L692-L760), [`src/java/dao/AdminKnowledgeDAO.java`](file:///d:/miniproject/fpt/src/java/dao/AdminKnowledgeDAO.java#L1470-L1730), [`src/java/controller/AdminKnowledgeBaseServlet.java`](file:///d:/miniproject/fpt/src/java/controller/AdminKnowledgeBaseServlet.java#L61-L66), and [`web/view/admin/knowledge-base.jsp`](file:///d:/miniproject/fpt/web/view/admin/knowledge-base.jsp#L36-L157):
- Renders the 5 operational dimension pills and 6 component cards (`#adminStatusPanel`) at the top of `/admin/knowledge-base`.
- Performs **0 live Gemini API calls** when loading the Admin page.
- Evaluates `systemStatus` **before** querying KB tables so that if a migration is missing, the panel gracefully reports `Migration Missing` with the exact list of missing SQL files (`pgvector`, `002-rag-mvp.sql`, `003-rag-source-policy.sql`, `004-admin-knowledge-base.sql`, `006-rag-gemini-runtime.sql`) instead of crashing.
- Never reports `Connected` merely because an API key environment variable is present; without completed journal evidence in `rag_api_requests`, it reports `Not Verified`.

---

## 8. Render Environment Variable Checklist

### 8.1 Core Database & Security Variables (Required in Production)
| Variable | Expected Value on Render | Purpose |
| :--- | :--- | :--- |
| `DB_HOST` | `<supabase-pooler-or-db-host>` | PostgreSQL host |
| `DB_PORT` | `5432` (or `6543`) | PostgreSQL port |
| `DB_NAME` | `postgres` | Database name |
| `DB_USER` | `<supabase-db-user>` | Database user |
| `DB_PASSWORD` | `<secret>` | Database password |
| `DB_SSLMODE` | `require` | Enforced TLS for non-loopback hosts |
| `AI_ENCRYPTION_KEY` | `<32+ char secret>` | AES encryption key for `settings` table secrets |
| `APP_BASE_URL` | `https://fpt.gialai.vn` | Canonical public URL |

### 8.2 AI, Knowledge Base & RAG Control Variables (Keep Safe Defaults Until Authorized)
| Variable | Safe Production Default | Future Controlled Online Value | Notes |
| :--- | :--- | :--- | :--- |
| `RAG_ENABLED` | `false` | `false` (until Phase 5.2+ sign-off) | Public customer RAG master switch |
| `RAG_RUNTIME_MODE` | `OFFLINE_MOCK` | `OFFLINE_MOCK` | Keep `OFFLINE_MOCK` on Render until production runtime mode is unlocked |
| `RAG_EMBEDDING_MODEL` | `gemini-embedding-001` | `gemini-embedding-001` | Must match `vector(768)` column |
| `RAG_EMBEDDING_DIMENSIONS` | `768` | `768` | Must be `768` |
| `RAG_EMBEDDING_API_KEY` | *(unset)* | `<dedicated-embedding-api-key>` | Separate from Chat API key |
| `RAG_CHAT_TEST_API_KEY` | *(unset)* | `<dedicated-chat-api-key>` | Or configure via Admin Settings (`ai_provider_gemini_key`) |
| `KB_STORAGE_DIR` | *(optional)* | `/var/data/fpt_kb_uploads` | Optional persistent disk path for uploaded raw files |

---

## 9. Supabase Database & `pgvector` Migration Checklist

Before using the Admin Knowledge Base on Supabase/Render, run the following migrations **manually in order** via the Supabase SQL Editor or `psql` (do **not** auto-run from the web app):

1. **Verify / Enable `pgvector`**:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
   ```
2. **Apply Migrations in Order**:
   - `database/migrations/002-rag-mvp.sql` (`knowledge_documents`, `knowledge_chunks` with `embedding vector(768)`, `knowledge_index_jobs`)
   - `database/migrations/003-rag-source-policy.sql` (source policy & authority metadata columns)
   - `database/migrations/004-admin-knowledge-base.sql` (`knowledge_document_revisions`, `knowledge_document_reviews`, `knowledge_files`, `knowledge_faqs`, `knowledge_faq_revisions`, `admin_audit_logs`)
   - `database/migrations/005-knowledge-document-imports.sql` (`knowledge_import_sections` for segmented document import review)
   - `database/migrations/006-rag-gemini-runtime.sql` (`rag_runtime_plans`, `rag_api_requests` ledger, and dual-index columns)
3. **Post-Migration Verification Query**:
   ```sql
   SELECT table_name
   FROM information_schema.tables
   WHERE table_schema = 'public'
     AND table_name IN (
       'knowledge_documents', 'knowledge_chunks', 'knowledge_index_jobs',
       'knowledge_document_revisions', 'knowledge_document_reviews',
       'knowledge_files', 'knowledge_faqs', 'knowledge_faq_revisions',
       'admin_audit_logs', 'knowledge_import_sections',
       'rag_runtime_plans', 'rag_api_requests'
     )
   ORDER BY table_name;
   ```
   *(All 12 tables must be returned, causing the Admin Status Panel to report `Knowledge Base: Database Ready`.)*

---

## 10. Controlled Production Activation Plan for AI / RAG

To transition from `OFFLINE_MOCK` to online RAG safely without unexpected Free Tier quota exhaustion:

- **Stage 1 — Database & Admin UI Deployment (0 API Cost)**:
  1. Apply migrations `002`–`006` on Supabase.
  2. Build and update `dist/fpt-sale.war` before merging `feature/rag-integration` into `main`.
  3. Keep `RAG_ENABLED=false` and `RAG_RUNTIME_MODE=OFFLINE_MOCK`.
  4. Verify on Render that `/admin/knowledge-base` shows `Knowledge Base: Database Ready` and allows uploading, reviewing, segmenting, and publishing documents using `MockEmbeddingProvider` (0 Gemini calls).
- **Stage 2 — Controlled Gemini Embedding Indexing**:
  1. Configure `RAG_EMBEDDING_API_KEY` on the target environment.
  2. Index approved `PUBLISHED` documents one by one using the bounded `rag_runtime_plans` + `rag_api_requests` ledger so every `gemini-embedding-001` (`768d`) request is audited and capped within Free Tier RPM/RPD limits.
- **Stage 3 — Admin Playground Retrieval Verification**:
  1. Test real vector retrieval inside `/admin/knowledge-base?tab=playground` (Admin-only) while public customer RAG remains `OFF`.
- **Stage 4 — Explicit Public Release Gate Unlock**:
  1. Only after Administrator sign-off on retrieval accuracy and quota bounds, unlock `productionRagEnabled` and set `RAG_ENABLED=true`.

---

## 11. Git Branch & Render Auto-Deploy Safety Check

- **Current Branch**: `feature/rag-integration`
- **Remote Tracking**: `origin/main` is at commit `1fdc8a7` (`Fix local PostgreSQL SSL mode handling and add test`).
- **Auto-Deploy Isolation**: Render is connected to the deployment branch (`main`). Working on `feature/rag-integration` does **not** trigger Render auto-deploy until explicitly merged and pushed to `origin/main`.
- **Critical Packaging Note ([`Dockerfile`](file:///d:/miniproject/fpt/Dockerfile#L5))**:
  - `Dockerfile` copies `dist/fpt-sale.war` into `/usr/local/tomcat/webapps/ROOT.war`.
  - Meanwhile, `build-portable.xml` builds `.build/dist/fpt-sale-rag.war`.
  - Therefore, prior to any production merge/push to `main`, `dist/fpt-sale.war` must be updated from the verified portable WAR (or `Dockerfile` updated to build from source via Ant) so Render deploys the latest compiled classes and JSPs.
- **Git Status Discipline**: No commits, pushes, merges, or deployments were executed during Phase 5.1.

---

## 12. Final Readiness Status

| Area | Status | Summary |
| :--- | :--- | :--- |
| **Sidebar UI** | **READY** | Collapsible Left Sidebar (`256px` / `68px` desktop, off-canvas drawer `<= 1024px`) + sticky Top Header Bar implemented across all 12 Admin pages; 32/32 JSPs compiled with 0 errors; E2E responsive & regression tests PASS. |
| **Admin Status Panel** | **READY** | 6-component status cards + 5 operational dimensions implemented with `0` live Gemini API calls; accurately distinguishes `Connected`, `Not Verified`, `Configuration Missing`, `Migration Missing`, and `Blocked by Release Gate`. |
| **Online RAG Readiness** | **READY FOR CONTROLLED CONFIG** | Root causes documented; Supabase migration checklist (`002`–`006`) and Render env checklist prepared; production safety gates (`RAG_ENABLED=false`, `production_rag_enabled=false`) remain strictly locked until Administrator authorization. |
