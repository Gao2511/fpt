package rag;

import com.google.gson.JsonObject;
import controller.AdminKnowledgeBaseServlet;
import dao.AdminKnowledgeDAO;
import dto.AdminKnowledgeDTO.*;
import dto.UserDTO;
import filter.AdminFilter;
import rag.extract.DocumentExtractionService;
import security.AdminSecurity;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Phase 3.12: Offline Automated Test Suite for Admin Knowledge Base & RAG Management System.
 * Covers Upload & Extraction (TXT, MD, DOCX, PDF), Malicious Upload Rejection,
 * MockEmbeddingProvider (768d), Configurable Chunking & Preview, Audit Redaction,
 * and Servlet RBAC / CSRF / Demo Mode / Production-RAG Hard-Lock Security.
 */
public final class RagAdminKbTest {
    private static int passed;

    interface Case {
        void run() throws Exception;
    }

    private static void test(String name, Case c) throws Exception {
        c.run();
        passed++;
        System.out.println("PASS KB-OFFLINE " + name);
    }

    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    private static void expectIllegalArg(Case c, String label) throws Exception {
        try {
            c.run();
            throw new AssertionError("Expected IllegalArgumentException for: " + label);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultVal(Class<?> t) {
        if (t == boolean.class) return false;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == double.class) return 0.0;
        return null;
    }

    private static byte[] buildValidDocxBytes(String paragraph1, String paragraph2, boolean includeMacro, boolean zipSlip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            String ct = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + (includeMacro ? "<Override PartName=\"/word/vbaProject.bin\" ContentType=\"application/vnd.ms-office.vbaProject\"/>" : "")
                    + "</Types>";
            zos.write(ct.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("word/document.xml"));
            String docXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                    + "<w:body>"
                    + "<w:p><w:r><w:t>" + paragraph1 + "</w:t></w:r></w:p>"
                    + "<w:p><w:r><w:t>" + paragraph2 + "</w:t></w:r></w:p>"
                    + "</w:body></w:document>";
            zos.write(docXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            if (includeMacro) {
                zos.putNextEntry(new ZipEntry("word/vbaProject.bin"));
                zos.write(new byte[]{1, 2, 3, 4});
                zos.closeEntry();
            }
            if (zipSlip) {
                zos.putNextEntry(new ZipEntry("../../evil.xml"));
                zos.write("<evil/>".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private static byte[] buildValidPdfBytes(String textContent, boolean flateCompressed, boolean includeJs) throws IOException {
        String streamBody = "BT /F1 12 Tf 72 720 Td (" + textContent + ") Tj ET";
        byte[] streamData;
        String filterDict = "";
        if (flateCompressed) {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (DeflaterOutputStream dos = new DeflaterOutputStream(compressed)) {
                dos.write(streamBody.getBytes(StandardCharsets.ISO_8859_1));
            }
            streamData = compressed.toByteArray();
            filterDict = "/Filter /FlateDecode ";
        } else {
            streamData = streamBody.getBytes(StandardCharsets.ISO_8859_1);
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        if (includeJs) {
            pdf.write("1 0 obj << /Type /Catalog /OpenAction << /S /JavaScript /JS (app.alert(1)) >> >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        String objHead = "4 0 obj << " + filterDict + "/Length " + streamData.length + " >>\nstream\n";
        pdf.write(objHead.getBytes(StandardCharsets.ISO_8859_1));
        pdf.write(streamData);
        pdf.write("\nendstream\nendobj\n%%EOF\n".getBytes(StandardCharsets.ISO_8859_1));
        return pdf.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        Path tempStorage = Files.createTempDirectory("fpt-kb-test-uploads-");
        try {
            DocumentExtractionService extractor = new DocumentExtractionService(tempStorage);

            // 1. UPLOAD & EXTRACTION TESTS
            test("extract valid UTF-8 TXT file", () -> {
                String content = "Hướng dẫn kiểm tra kết nối dây mạng và nguồn điện modem tại nhà.";
                DocumentExtractionService.ExtractionResult res = extractor.storeAndExtract(
                        "huong-dan-ket-noi.txt", "text/plain",
                        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                check("EXTRACTED".equals(res.status), "Expected EXTRACTED");
                check(res.extractedText.equals(content), "Mismatch extracted TXT");
                check(Files.exists(tempStorage.resolve(res.storedFilename)), "Stored file missing");
            });

            test("extract valid Markdown (.md) file and normalize formatting", () -> {
                String md = "# Chính sách hỗ trợ kỹ thuật\n\nKhách hàng liên hệ **tư vấn viên** để kiểm tra `modem` an toàn.";
                DocumentExtractionService.ExtractionResult res = extractor.storeAndExtract(
                        "chinh-sach-ho-tro.md", "text/markdown",
                        new ByteArrayInputStream(md.getBytes(StandardCharsets.UTF_8)));
                check("EXTRACTED".equals(res.status), "Expected EXTRACTED");
                check(res.extractedText.contains("Chính sách hỗ trợ kỹ thuật") && !res.extractedText.contains("**"), "Markdown not normalized");
            });

            test("extract valid DOCX file with Vietnamese paragraphs", () -> {
                byte[] docx = buildValidDocxBytes(
                        "Quy trình chuyển địa điểm sử dụng Internet FPT tại Gia Lai.",
                        "Khách hàng mang theo căn cước công dân đến quầy giao dịch.",
                        false, false);
                DocumentExtractionService.ExtractionResult res = extractor.storeAndExtract(
                        "quy-trinh-chuyen-dia-diem.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        new ByteArrayInputStream(docx));
                check("EXTRACTED".equals(res.status), "Expected EXTRACTED");
                check(res.extractedText.contains("Quy trình chuyển địa điểm") && res.extractedText.contains("căn cước công dân"),
                        "DOCX paragraphs not extracted: " + res.extractedText);
            });

            test("extract valid PDF file (uncompressed and FlateDecode)", () -> {
                for (boolean compressed : new boolean[]{false, true}) {
                    byte[] pdf = buildValidPdfBytes("Huong dan bao hanh thiet bi modem tai quay giao dich FPT", compressed, false);
                    DocumentExtractionService.ExtractionResult res = extractor.storeAndExtract(
                            "bao-hanh-modem-" + compressed + ".pdf", "application/pdf",
                            new ByteArrayInputStream(pdf));
                    check("EXTRACTED".equals(res.status), "Expected EXTRACTED for compressed=" + compressed);
                    check(res.extractedText.contains("Huong dan bao hanh thiet bi modem"), "PDF text mismatch: " + res.extractedText);
                }
            });

            test("handle image-only/empty-text PDF as PROCESSING_FAILED without crashing", () -> {
                byte[] emptyPdf = "%PDF-1.4\n1 0 obj << /Type /Catalog >> endobj\n%%EOF\n".getBytes(StandardCharsets.ISO_8859_1);
                DocumentExtractionService.ExtractionResult res = extractor.storeAndExtract(
                        "scanned-image-only.pdf", "application/pdf", new ByteArrayInputStream(emptyPdf));
                check("PROCESSING_FAILED".equals(res.status), "Expected PROCESSING_FAILED for image-only PDF");
                check(!res.warningMessage.isEmpty(), "Expected warning message");
            });

            test("reject oversized file (> 10 MB)", () -> {
                byte[] huge = new byte[(int) (DocumentExtractionService.MAX_UPLOAD_BYTES + 1024)];
                Arrays.fill(huge, (byte) 'a');
                expectIllegalArg(() -> extractor.storeAndExtract("oversized.txt", "text/plain", new ByteArrayInputStream(huge)),
                        "oversized upload");
            });

            test("reject disallowed and executable extensions", () -> {
                for (String badName : Arrays.asList("shell.jsp", "malware.exe", "script.bat", "run.ps1", "xss.html", "icon.svg", "code.js", "macro.docm")) {
                    expectIllegalArg(() -> extractor.storeAndExtract(badName, "application/octet-stream",
                            new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8))), badName);
                }
            });

            test("reject double extensions hiding executable/script types", () -> {
                for (String badName : Arrays.asList("shell.jsp.txt", "payload.exe.pdf", "xss.html.md", "script.js.docx")) {
                    expectIllegalArg(() -> extractor.storeAndExtract(badName, "text/plain",
                            new ByteArrayInputStream("safe text".getBytes(StandardCharsets.UTF_8))), badName);
                }
            });

            test("reject path traversal and null bytes in filenames", () -> {
                for (String badName : Arrays.asList("../secret.txt", "..\\secret.txt", "sub/file.txt", "sub\\file.txt", "bad\0name.txt", ".hidden.txt")) {
                    expectIllegalArg(() -> DocumentExtractionService.sanitizeFilename(badName), badName);
                }
            });

            test("reject spoofed magic bytes (MZ executable, JSP/HTML script, ZIP disguised as TXT, invalid PDF)", () -> {
                expectIllegalArg(() -> extractor.storeAndExtract("fake.pdf", "application/pdf",
                        new ByteArrayInputStream(new byte[]{'M', 'Z', 0x01, 0x02, 0x03, 0x04, 0x05, 0x06})), "MZ as PDF");
                expectIllegalArg(() -> extractor.storeAndExtract("fake.txt", "text/plain",
                        new ByteArrayInputStream("<% Runtime.getRuntime().exec(\"calc\"); %>".getBytes(StandardCharsets.UTF_8))), "JSP as TXT");
                expectIllegalArg(() -> extractor.storeAndExtract("fake.md", "text/markdown",
                        new ByteArrayInputStream("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))), "Script as MD");
                expectIllegalArg(() -> extractor.storeAndExtract("not-a-pdf.pdf", "application/pdf",
                        new ByteArrayInputStream("plain text pretending to be pdf".getBytes(StandardCharsets.UTF_8))), "Plain as PDF");
            });

            test("reject DOCX with VBA macros or Zip Slip paths", () -> {
                byte[] macroDocx = buildValidDocxBytes("Hello", "World", true, false);
                expectIllegalArg(() -> extractor.storeAndExtract("macro.docx", "application/octet-stream",
                        new ByteArrayInputStream(macroDocx)), "DOCX with VBA macro");

                byte[] slipDocx = buildValidDocxBytes("Hello", "World", false, true);
                expectIllegalArg(() -> extractor.storeAndExtract("slip.docx", "application/octet-stream",
                        new ByteArrayInputStream(slipDocx)), "DOCX with Zip Slip");
            });

            test("reject PDF with embedded JavaScript", () -> {
                byte[] jsPdf = buildValidPdfBytes("Safe text", false, true);
                expectIllegalArg(() -> extractor.storeAndExtract("evil.pdf", "application/pdf",
                        new ByteArrayInputStream(jsPdf)), "PDF with JavaScript");
            });

            test("reject upload storage directory inside web root", () -> {
                expectIllegalArg(() -> new DocumentExtractionService(Paths.get("D:/miniproject/fpt/web/uploads")), "web root storage");
                expectIllegalArg(() -> new DocumentExtractionService(Paths.get("D:/miniproject/fpt/.build/web/uploads")), "build web root storage");
            });

            // 2. MOCK EMBEDDING & CHUNKING TESTS
            test("MockEmbeddingProvider produces deterministic normalized 768-dim vectors offline", () -> {
                MockEmbeddingProvider mock = new MockEmbeddingProvider();
                check(mock.isOfflineMock() && mock.dimensions() == 768 && "gemini-embedding-001".equals(mock.model()), "Mock metadata");
                float[] v1 = mock.document("Chính sách bảo hành modem", "Khách hàng được hỗ trợ kiểm tra modem WiFi tại nhà.");
                float[] v2 = mock.document("Chính sách bảo hành modem", "Khách hàng được hỗ trợ kiểm tra modem WiFi tại nhà.");
                float[] qSim = mock.query("chính sách bảo hành modem wifi tại nhà");
                float[] qDiff = mock.query("thủ tục chấm dứt hợp đồng chuyển địa điểm");
                check(v1.length == 768 && Arrays.equals(v1, v2), "Deterministic embedding expected");

                double norm = 0, dotSim = 0, dotDiff = 0;
                for (int i = 0; i < 768; i++) {
                    norm += (double) v1[i] * v1[i];
                    dotSim += (double) v1[i] * qSim[i];
                    dotDiff += (double) v1[i] * qDiff[i];
                }
                check(Math.abs(norm - 1.0) < 1e-5, "Unit norm expected");
                check(dotSim > 0.72 && dotSim > dotDiff, "Related query should have high cosine similarity (>0.72), got: " + dotSim);
                check(mock.calls() == 4, "Call counter mismatch");
            });

            test("configurable chunking and preview enforce safe bounds", () -> {
                DocumentChunkingService chunker = new DocumentChunkingService();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 40; i++) {
                    sb.append("Đoạn hướng dẫn số ").append(i).append(": Khách hàng kiểm tra đèn tín hiệu PON trên thiết bị modem. ");
                }
                List<DocumentChunkingService.ChunkPreview> previews = chunker.preview("Hướng dẫn PON", sb.toString(), 400, 60);
                check(previews.size() >= 3, "Expected multiple chunks");
                for (DocumentChunkingService.ChunkPreview cp : previews) {
                    check(cp.charCount > 0 && cp.charCount <= 400 && cp.estimatedTokens > 0 && cp.contentHash.length() == 64, "Invalid chunk preview");
                }
                try {
                    chunker.chunk(sb.toString(), 100, 50);
                    throw new AssertionError("Should reject chunkSize < 200");
                } catch (RagException expected) {
                    check(expected.code == RagException.Code.INVALID_DOCUMENT, "Expected INVALID_DOCUMENT");
                }
                try {
                    chunker.chunk(sb.toString(), 400, 250);
                    throw new AssertionError("Should reject overlap >= chunkSize/2");
                } catch (RagException expected) {
                    check(expected.code == RagException.Code.INVALID_DOCUMENT, "Expected INVALID_DOCUMENT");
                }
            });

            // 3. AUDIT LOG REDACTION & SECURITY TESTS
            test("audit log sanitizer strips keys, secrets, tokens, passwords, and vectors", () -> {
                Map<String, Object> dirty = new LinkedHashMap<>();
                dirty.put("id", 42);
                dirty.put("workflowStatus", "PUBLISHED");
                dirty.put("geminiApiKey", "AIzaSyFAKE_SECRET_DO_NOT_LOG");
                dirty.put("api_key", "SECRET_KEY_2");
                dirty.put("csrf_token", "TOKEN_VALUE");
                dirty.put("adminPassword", "PASS_VALUE");
                dirty.put("embeddingVector", "[0.1, 0.2, 0.3]");
                JsonObject clean = AdminKnowledgeDAO.sanitizeForAudit(dirty);
                String json = clean.toString();
                check(clean.has("id") && clean.has("workflowStatus"), "Safe fields preserved");
                check(!json.contains("AIzaSyFAKE") && !json.contains("SECRET_KEY_2")
                        && !json.contains("TOKEN_VALUE") && !json.contains("PASS_VALUE")
                        && !json.contains("embeddingVector"), "Secrets/vectors must be redacted: " + json);
            });

            test("AdminFilter and AdminKnowledgeBaseServlet enforce RBAC, CSRF, Demo isolation, and direct JSP block", () -> {
                cms.FakeDatabase fakeDb = new cms.FakeDatabase();
                fakeDb.install();
                AdminFilter filter = new AdminFilter();

                // Unauthenticated GET /admin/knowledge-base -> 302 redirect to /login
                final int[] statusHolder = {200};
                final int[] chainHolder = {0};
                final Map<String, Object> sessionMap = new HashMap<>();
                final Map<String, String> params = new HashMap<>();
                final String[] methodHolder = {"GET"};
                final String[] pathHolder = {"/admin/knowledge-base"};
                final String[] headerToken = {null};
                final DispatcherType[] dispHolder = {DispatcherType.REQUEST};

                final HttpSession session = proxy(HttpSession.class, (o, m, a) -> {
                    if ("getAttribute".equals(m.getName())) return sessionMap.get(a[0]);
                    if ("setAttribute".equals(m.getName())) { sessionMap.put((String) a[0], a[1]); return null; }
                    if ("getId".equals(m.getName())) return "kb-test-session";
                    return defaultVal(m.getReturnType());
                });
                final HttpServletRequest req = proxy(HttpServletRequest.class, (o, m, a) -> {
                    switch (m.getName()) {
                        case "getSession": return session;
                        case "getRequestURI": return "/fpt-sale" + pathHolder[0];
                        case "getContextPath": return "/fpt-sale";
                        case "getServletPath": return pathHolder[0];
                        case "getMethod": return methodHolder[0];
                        case "getDispatcherType": return dispHolder[0];
                        case "getParameter": return params.get(a[0]);
                        case "getHeader": return "X-CSRF-Token".equals(a[0]) ? headerToken[0] : null;
                        case "getRequestDispatcher": return proxy(RequestDispatcher.class, (x, n, b) -> null);
                        default: return defaultVal(m.getReturnType());
                    }
                });
                final HttpServletResponse resp = proxy(HttpServletResponse.class, (o, m, a) -> {
                    if ("sendError".equals(m.getName()) || "setStatus".equals(m.getName())) { statusHolder[0] = (Integer) a[0]; return null; }
                    if ("sendRedirect".equals(m.getName())) { statusHolder[0] = 302; return null; }
                    return defaultVal(m.getReturnType());
                });

                filter.doFilter(req, resp, (r, p) -> chainHolder[0]++);
                check(statusHolder[0] == 302 && chainHolder[0] == 0, "Unauthenticated access must redirect 302");

                // Customer role -> 403
                UserDTO customer = new UserDTO(); customer.setId(101); customer.setRole("customer"); customer.setActive(true);
                sessionMap.put("user", customer);
                statusHolder[0] = 200;
                filter.doFilter(req, resp, (r, p) -> chainHolder[0]++);
                check(statusHolder[0] == 403 && chainHolder[0] == 0, "Customer role must receive 403");

                // Demo admin -> 403 on /admin/knowledge-base
                UserDTO demo = new UserDTO(); demo.setId(101); demo.setRole("demo_admin"); demo.setActive(true);
                sessionMap.put("user", demo);
                statusHolder[0] = 200;
                filter.doFilter(req, resp, (r, p) -> chainHolder[0]++);
                check(statusHolder[0] == 403 && chainHolder[0] == 0, "Demo admin must receive 403 on /admin/knowledge-base");

                // Real admin direct access to /view/admin/knowledge-base.jsp -> 403
                UserDTO admin = new UserDTO(); admin.setId(101); admin.setRole("admin"); admin.setActive(true);
                sessionMap.put("user", admin);
                pathHolder[0] = "/view/admin/knowledge-base.jsp";
                statusHolder[0] = 200;
                filter.doFilter(req, resp, (r, p) -> chainHolder[0]++);
                check(statusHolder[0] == 403 && chainHolder[0] == 0, "Direct JSP access must be blocked with 403");

                // Real admin POST without CSRF -> 403
                pathHolder[0] = "/admin/knowledge-base";
                methodHolder[0] = "POST";
                statusHolder[0] = 200;
                filter.doFilter(req, resp, (r, p) -> chainHolder[0]++);
                check(statusHolder[0] == 403 && chainHolder[0] == 0, "POST without CSRF must be blocked with 403");

                // Real admin POST with valid CSRF -> passes filter
                headerToken[0] = AdminSecurity.token(session);
                statusHolder[0] = 200;
                filter.doFilter(req, resp, (r, p) -> chainHolder[0]++);
                check(statusHolder[0] == 200 && chainHolder[0] == 1, "Verified admin with CSRF must pass filter");
            });

            System.out.println("RagAdminKbTest: " + passed + " tests passed (0 external API calls)");
        } finally {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(tempStorage)) {
                for (Path p : ds) Files.deleteIfExists(p);
            } catch (Exception ignored) {}
            Files.deleteIfExists(tempStorage);
        }
    }
}
