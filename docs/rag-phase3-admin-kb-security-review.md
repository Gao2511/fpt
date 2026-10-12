# PHASE 3.11 — ADMIN KNOWLEDGE BASE & RAG SECURITY REVIEW

**Project:** `D:\miniproject\fpt`  
**Branch:** `feature/rag-integration`  
**Target Website:** FPT Telecom Gia Lai  
**Review Date:** 2026-10-12  
**Security Posture:** **APPROVED FOR LOCAL & OFFLINE TESTING — PRODUCTION RAG HARD-LOCKED (`false`)**

---

## 1. Executive Summary

This security review evaluates the new **Admin Knowledge Base & RAG Management System** (`PHASE 3.1`–`PHASE 3.14`) across seven security domains:
1. Authentication & Authorization (`AdminAuthFilter`, session role validation)
2. Cross-Site Request Forgery (CSRF) & Mutating HTTP Verb Enforcement
3. Cross-Site Scripting (XSS) & Admin UI Output Encoding
4. File Upload & Document Extraction Hardening (`PDF`, `DOCX`, `TXT`, `MD`)
5. SQL Injection & Database Access Controls
6. RAG Content Safety, Grounding & Authority Boundaries
7. Secret Redaction, Audit Logging & Production Lockout

All **17** offline security/unit checks in `RagAdminKbTest`, **10** PostgreSQL 17 + pgvector 0.8.2 integration checks in `RagPhase3PostgresTest`, and **15** existing CMS security checks in `CmsSecurityTest` pass with zero vulnerabilities identified.

---

## 2. Threat Model & Control Verification Matrix

| ID | Threat / Attack Vector | Control Implemented | Source Location | Verification Test | Status |
|---|---|---|---|---|---|
| **SEC-01** | Unauthenticated or non-admin access to `/admin/knowledge-base` | Covered by `AdminAuthFilter` (`/admin/*`) + defense-in-depth `AdminSecurity.requireAdmin(req, resp)` check inside `AdminKnowledgeBaseServlet.doGet` and `doPost`. | `src/java/filter/AdminAuthFilter.java`<br>`src/java/controller/AdminKnowledgeBaseServlet.java` | `CmsSecurityTest` | **PASS** |
| **SEC-02** | CSRF attack triggering state-changing KB actions (publish, delete/archive, reindex, settings change) | Every state-changing action requires `HTTP POST` + valid session-bound `csrfToken` verified via `AdminSecurity.verifyCsrf(req, resp)` using constant-time `MessageDigest.isEqual`. `GET` requests never mutate state. | `src/java/security/AdminSecurity.java`<br>`src/java/controller/AdminKnowledgeBaseServlet.java` | `CmsSecurityTest` | **PASS** |
| **SEC-03** | Open Redirect after POST actions | `AdminSecurity.redirect(req, resp, path)` allowlists `/admin/knowledge-base` and blocks external URLs, `//`, `\`, schemes, and control characters. | `src/java/security/AdminSecurity.java` | `CmsSecurityTest` | **PASS** |
| **SEC-04** | Stored or Reflected XSS via document title, content, tags, rejection reason, FAQ answer, or playground query | All dynamic outputs in `web/view/admin/knowledge-base.jsp` are rendered exclusively through `<c:out value="..."/>` or `fn:escapeXml(...)`. Zero unescaped `${...}` expressions exist in user-supplied fields. | `web/view/admin/knowledge-base.jsp` | Jasper compilation (`18/18` JSPs) | **PASS** |
| **SEC-05** | Malicious File Upload: Executable / Script / HTML / SVG / Macro-enabled Office upload (`.exe`, `.jsp`, `.html`, `.docm`, `.xlsm`, `.svg`) | Strict extension allowlist (`.pdf`, `.docx`, `.txt`, `.md` only) + explicit denylist for executable/script/macro extensions + magic-byte signature verification (`%PDF-`, `PK\x03\x04`, valid UTF-8 text without NUL bytes). | `src/java/rag/extract/DocumentExtractionService.java` | `RagAdminKbTest` (#5, #6, #7, #8) | **PASS** |
| **SEC-06** | Path Traversal or Double Extension in uploaded filename (`../../shell.jsp`, `policy.jsp.pdf`, `doc.exe.txt`) | `sanitizeOriginalName` strips directory components, rejects `..` segments, and counts dots (`dots > 1` -> rejected as double extension). Stored disk filename is a random UUID (`UUID.randomUUID() + ext`). | `src/java/rag/extract/DocumentExtractionService.java` | `RagAdminKbTest` (#5, #6) | **PASS** |
| **SEC-07** | Direct Web Execution of Uploaded Files | Uploaded files are stored outside the web root (`${java.io.tmpdir}/fpt-kb-uploads` or `FPT_KB_UPLOAD_DIR`) and are never served directly by Tomcat's default servlet. | `src/java/rag/extract/DocumentExtractionService.java` | `RagAdminKbTest` (#1–#4) | **PASS** |
| **SEC-08** | Oversized Upload / ZIP Bomb / XML External Entity (XXE) in `.docx` | `@MultipartConfig` caps upload size at `10 MB` (`MAX_FILE_BYTES = 10_485_760`). `.docx` reader caps ZIP entries (`<= 250`), uncompressed entry size (`<= 16 MB`), total uncompressed size (`<= 24 MB`), rejects `vbaProject.bin` macros, and configures `DocumentBuilderFactory` with `disallow-doctype-decl=true`, `external-general-entities=false`, `external-parameter-entities=false`. | `src/java/rag/extract/DocumentExtractionService.java` | `RagAdminKbTest` (#2, #9) | **PASS** |
| **SEC-09** | Embedded JavaScript / Launch Actions inside PDF | PDF parser rejects byte streams containing `/JavaScript`, `/JS`, `/Launch`, `/EmbeddedFiles`, or `/OpenAction` coupled with `/JS`. | `src/java/rag/extract/DocumentExtractionService.java` | `RagAdminKbTest` (#3, #10) | **PASS** |
| **SEC-10** | Unreviewed / Premature Auto-Publish on Upload | Uploaded files always create a `DRAFT` document (`status='draft'`, `workflow_status='DRAFT'`, `indexing_status='pending'`) and are **never** auto-published or auto-indexed. Publishing from `DRAFT` or `PENDING_REVIEW` throws `IllegalStateException`. | `src/java/dao/AdminKnowledgeDAO.java` | `RagPhase3PostgresTest` (#2, #3) | **PASS** |
| **SEC-11** | Prompt Injection / Unsafe Business Claims in RAG Chunks | Both live retrieval (`PgVectorKnowledgeStore`) and the Offline Playground (`AdminKnowledgeDAO.runOfflinePlayground`) enforce `KnowledgeContentPolicy.reason(chunk, now)` and `KnowledgeSourcePolicy.select(...)`, blocking prompt injection tokens, unapproved/expired/draft chunks, and unauthorized price/package claims (`INVALID_BUSINESS_CLAIM`). | `src/java/rag/KnowledgeContentPolicy.java`<br>`src/java/rag/KnowledgeSourcePolicy.java`<br>`src/java/dao/AdminKnowledgeDAO.java` | `RagQualityTest`<br>`RagPhase3PostgresTest` (#2, #6, #8, #9) | **PASS** |
| **SEC-12** | Accidental Enabling of Production RAG or Live Gemini Calls from Admin UI | `AdminKnowledgeDAO.saveRagSettings` hard-locks `production_rag_enabled = false` on the server side (`IllegalArgumentException` + `BLOCKED_PRODUCTION_RAG_ENABLE` audit log if `productionRagEnabled=true` is submitted). All Phase 3 indexing and playground features use `MockEmbeddingProvider` (`0` network calls). | `src/java/dao/AdminKnowledgeDAO.java`<br>`src/java/rag/MockEmbeddingProvider.java` | `RagAdminKbTest` (#17)<br>`RagPhase3PostgresTest` (#10) | **PASS** |
| **SEC-13** | Secret / Embedding Leakage in Audit Logs | `AdminKnowledgeDAO.redactMap(...)` automatically redacts any key matching `key`, `secret`, `token`, `password`, `credential`, `authorization`, `embedding`, or `vector` to `"[REDACTED]"` before writing JSONB to `public.admin_audit_logs`. | `src/java/dao/AdminKnowledgeDAO.java` | `RagAdminKbTest` (#17)<br>`RagPhase3PostgresTest` (#10) | **PASS** |
| **SEC-14** | SQL Injection in Search, Filters, Workflow Transitions, or Vector Queries | 100% of queries in `AdminKnowledgeDAO` and `PgVectorKnowledgeStore` use `PreparedStatement` with bound `?` parameters (`escapeLike` for `ILIKE` search patterns). | `src/java/dao/AdminKnowledgeDAO.java`<br>`src/java/rag/PgVectorKnowledgeStore.java` | `RagPhase3PostgresTest` (#1–#10) | **PASS** |

---

## 3. Compliance with Phase 2.4.9 / Phase 3 Safety Locks

- **Preserved Artifacts:** All 97 files in `.build/phase242-preserve-e5fd204e3c94450abccf7a10bc616e7f.json` remain byte-identical.
- **Runtime Flags:**
  - `RAG_ENABLED=false`
  - `production_rag_enabled=false` (hard-locked server-side)
  - `approved=false` and `executionAuthorized=false` in `.local/rag-live/phase24-live-plan.properties`
- **External Network Activity:** `0` real Gemini API calls; `0` Supabase production connections.
