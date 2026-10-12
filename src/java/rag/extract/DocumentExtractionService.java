package rag.extract;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * Phase 3.3: Secure Document Upload & Text Extraction Service.
 * Supports PDF, DOCX, TXT, and Markdown (.md) with strict signature verification,
 * ZIP bomb / macro / XXE / PDF-JS protection, and storage outside the web root.
 */
public final class DocumentExtractionService {
    public static final long MAX_UPLOAD_BYTES = 10L * 1024L * 1024L; // 10 MB
    private static final long MAX_UNCOMPRESSED_BYTES = 25L * 1024L * 1024L; // 25 MB
    private static final int MAX_ZIP_ENTRIES = 250;
    private static final int MAX_COMPRESSION_RATIO = 50;
    private static final int MAX_EXTRACTED_CHARS = 60000;

    private static final Set<String> ALLOWED_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("pdf", "docx", "txt", "md")));

    private static final Set<String> FORBIDDEN_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("exe", "bat", "cmd", "ps1", "sh", "jsp", "jspx", "jar", "war",
                    "class", "java", "html", "htm", "svg", "js", "vbs", "docm", "xlsm", "pptm", "zip", "rar", "7z")));

    public static final class ExtractionResult {
        public final UUID fileId;
        public final String originalFilename;
        public final String storedFilename;
        public final String storageRelPath;
        public final String detectedMimeType;
        public final String extension;
        public final long sizeBytes;
        public final String sha256Hash;
        public final String status; // "EXTRACTED" or "PROCESSING_FAILED"
        public final String extractedText;
        public final int charCount;
        public final String warningMessage;

        public ExtractionResult(UUID fileId, String originalFilename, String storedFilename,
                String storageRelPath, String detectedMimeType, String extension, long sizeBytes,
                String sha256Hash, String status, String extractedText, int charCount, String warningMessage) {
            this.fileId = fileId;
            this.originalFilename = originalFilename;
            this.storedFilename = storedFilename;
            this.storageRelPath = storageRelPath;
            this.detectedMimeType = detectedMimeType;
            this.extension = extension;
            this.sizeBytes = sizeBytes;
            this.sha256Hash = sha256Hash;
            this.status = status;
            this.extractedText = extractedText;
            this.charCount = charCount;
            this.warningMessage = warningMessage == null ? "" : warningMessage;
        }
    }

    private final Path storageRoot;

    public DocumentExtractionService() {
        this(resolveDefaultStorageRoot());
    }

    public DocumentExtractionService(Path storageRoot) {
        if (storageRoot == null) {
            throw new IllegalArgumentException("Storage root required");
        }
        Path normalized = storageRoot.toAbsolutePath().normalize();
        String lower = normalized.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (lower.contains("/web/") || lower.endsWith("/web") || lower.contains("/.build/web")) {
            throw new IllegalArgumentException("Upload storage must reside outside the web root");
        }
        this.storageRoot = normalized;
    }

    private static Path resolveDefaultStorageRoot() {
        String custom = System.getProperty("fpt.kb.upload.dir");
        if (custom != null && !custom.trim().isEmpty()) {
            return Paths.get(custom.trim());
        }
        return Paths.get(System.getProperty("java.io.tmpdir"), "fpt-kb-uploads");
    }

    public Path getStorageRoot() {
        return storageRoot;
    }

    /**
     * Validates, stores outside web root, and extracts UTF-8 text from an uploaded file stream.
     * Throws IllegalArgumentException for security policy violations (size, spoofed signature, traversal, macro, etc.).
     * Returns ExtractionResult with status="PROCESSING_FAILED" if a structurally valid PDF/DOCX yields no extractable text.
     */
    public ExtractionResult storeAndExtract(String rawFilename, String declaredContentType, InputStream input) throws IOException {
        String cleanFilename = sanitizeFilename(rawFilename);
        String ext = extractExtension(cleanFilename);
        if (FORBIDDEN_EXTENSIONS.contains(ext) || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Định dạng tệp không được phép (chỉ hỗ trợ .pdf, .docx, .txt, .md): " + ext);
        }
        if (input == null) {
            throw new IllegalArgumentException("Không có dữ liệu tệp tải lên.");
        }

        byte[] bytes = readBounded(input, MAX_UPLOAD_BYTES);
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Tệp tải lên rỗng (0 byte).");
        }

        rejectDangerousSignatures(bytes, ext);
        String detectedMime = verifySignatureAndDetectMime(bytes, ext, declaredContentType);
        String sha256 = sha256Hex(bytes);

        UUID fileId = UUID.randomUUID();
        String storedFilename = fileId.toString() + "." + ext;
        Files.createDirectories(storageRoot);
        Path target = storageRoot.resolve(storedFilename).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Phát hiện đường dẫn lưu trữ không hợp lệ.");
        }

        String extracted = "";
        String status = "EXTRACTED";
        String warning = "";

        try {
            if ("txt".equals(ext) || "md".equals(ext)) {
                extracted = extractPlainText(bytes, "md".equals(ext));
            } else if ("docx".equals(ext)) {
                extracted = extractDocxText(bytes);
            } else if ("pdf".equals(ext)) {
                extracted = extractPdfText(bytes);
            }
        } catch (SecurityException se) {
            // Security violations (macros, XXE, ZIP bomb, PDF JS) are rejected outright.
            throw new IllegalArgumentException(se.getMessage());
        } catch (Exception ex) {
            status = "PROCESSING_FAILED";
            warning = ex.getMessage() != null ? ex.getMessage() : "Không thể trích xuất văn bản từ tệp.";
        }

        extracted = normalizeExtractedText(extracted);
        if ("EXTRACTED".equals(status) && extracted.isEmpty()) {
            status = "PROCESSING_FAILED";
            warning = "Không trích xuất được nội dung văn bản (tệp có thể chỉ chứa hình ảnh scan hoặc mã hóa font đặc biệt).";
        }
        if (extracted.length() > MAX_EXTRACTED_CHARS) {
            extracted = extracted.substring(0, MAX_EXTRACTED_CHARS).trim();
            warning = warning.isEmpty()
                    ? "Nội dung đã được cắt giới hạn tối đa 60.000 ký tự."
                    : warning + " Nội dung cắt giới hạn 60.000 ký tự.";
        }

        Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        return new ExtractionResult(
                fileId,
                cleanFilename,
                storedFilename,
                storedFilename,
                detectedMime,
                ext,
                bytes.length,
                sha256,
                status,
                extracted,
                extracted.length(),
                warning);
    }

    public static String sanitizeFilename(String rawFilename) {
        if (rawFilename == null || rawFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        }
        if (rawFilename.indexOf('\0') >= 0 || rawFilename.contains("..")
                || rawFilename.contains("/") || rawFilename.contains("\\") || rawFilename.contains(":")) {
            throw new IllegalArgumentException("Tên tệp chứa ký tự đường dẫn không an toàn.");
        }
        String trimmed = rawFilename.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isISOControl(c)) {
                throw new IllegalArgumentException("Tên tệp chứa ký tự điều khiển không hợp lệ.");
            }
        }
        if (trimmed.startsWith(".") || trimmed.length() > 200) {
            throw new IllegalArgumentException("Tên tệp không hợp lệ hoặc vượt quá 200 ký tự.");
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        // Reject double extensions hiding executable/script types (e.g. payload.jsp.txt or malware.exe.pdf)
        String[] parts = lower.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Tệp phải có phần mở rộng hợp lệ (.pdf, .docx, .txt, .md).");
        }
        for (int i = 1; i < parts.length - 1; i++) {
            if (FORBIDDEN_EXTENSIONS.contains(parts[i])) {
                throw new IllegalArgumentException("Tên tệp chứa phần mở rộng kép bị cấm: ." + parts[i]);
            }
        }
        return trimmed;
    }

    private static String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            throw new IllegalArgumentException("Tệp thiếu phần mở rộng.");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readBounded(InputStream in, long maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        long total = 0;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > maxBytes) {
                throw new IllegalArgumentException("Kích thước tệp vượt quá giới hạn cho phép (tối đa 10 MB).");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static void rejectDangerousSignatures(byte[] bytes, String ext) {
        // PE/MZ executable
        if (bytes.length >= 2 && bytes[0] == 'M' && bytes[1] == 'Z') {
            throw new IllegalArgumentException("Từ chối tệp thực thi (MZ/PE header).");
        }
        // ELF binary
        if (bytes.length >= 4 && bytes[0] == 0x7F && bytes[1] == 'E' && bytes[2] == 'L' && bytes[3] == 'F') {
            throw new IllegalArgumentException("Từ chối tệp nhị phân thực thi (ELF header).");
        }
        // Java class file (CAFEBABE)
        if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0xCA && (bytes[1] & 0xFF) == 0xFE
                && (bytes[2] & 0xFF) == 0xBA && (bytes[3] & 0xFF) == 0xBE) {
            throw new IllegalArgumentException("Từ chối tệp Java bytecode (.class).");
        }
        // Inspect leading ASCII for JSP/HTML/SVG/script spoofing
        int headLen = Math.min(bytes.length, 512);
        String head = new String(bytes, 0, headLen, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
        if (head.contains("<%") || head.contains("<jsp:") || head.contains("<script")
                || head.contains("<!doctype html") || head.contains("<html") || head.contains("<svg")) {
            throw new IllegalArgumentException("Từ chối tệp chứa mã kịch bản HTML/JSP/SVG/Script.");
        }
    }

    private static String verifySignatureAndDetectMime(byte[] bytes, String ext, String declaredMime) {
        if ("pdf".equals(ext)) {
            if (bytes.length < 8 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-') {
                throw new IllegalArgumentException("Chữ ký tệp không khớp định dạng PDF (%PDF- header).");
            }
            String tail = new String(bytes, Math.max(0, bytes.length - 1024), Math.min(1024, bytes.length), StandardCharsets.ISO_8859_1);
            if (!tail.contains("%%EOF")) {
                throw new IllegalArgumentException("Tệp PDF không hợp lệ hoặc bị cắt cụt (thiếu %%EOF).");
            }
            return "application/pdf";
        }
        if ("docx".equals(ext)) {
            if (bytes.length < 4 || (bytes[0] & 0xFF) != 0x50 || (bytes[1] & 0xFF) != 0x4B
                    || (bytes[2] & 0xFF) != 0x03 || (bytes[3] & 0xFF) != 0x04) {
                throw new IllegalArgumentException("Chữ ký tệp không khớp định dạng DOCX (ZIP/OOXML header).");
            }
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if ("txt".equals(ext) || "md".equals(ext)) {
            // Must not be a disguised PDF or ZIP
            if (bytes.length >= 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F') {
                throw new IllegalArgumentException("Tệp văn bản không được chứa chữ ký nhị phân PDF.");
            }
            if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0x50 && (bytes[1] & 0xFF) == 0x4B
                    && (bytes[2] & 0xFF) == 0x03 && (bytes[3] & 0xFF) == 0x04) {
                throw new IllegalArgumentException("Tệp văn bản không được chứa chữ ký nén ZIP.");
            }
            for (byte b : bytes) {
                int v = b & 0xFF;
                if (v == 0x00) {
                    throw new IllegalArgumentException("Tệp văn bản chứa byte nhị phân (NULL byte) không hợp lệ.");
                }
            }
            return "md".equals(ext) ? "text/markdown" : "text/plain";
        }
        throw new IllegalArgumentException("Định dạng không được hỗ trợ.");
    }

    private static String extractPlainText(byte[] bytes, boolean markdown) {
        int offset = 0;
        // Strip UTF-8 BOM if present
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        final String decoded;
        try {
            CharBuffer cb = decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset));
            decoded = cb.toString();
        } catch (Exception e) {
            throw new SecurityException("Tệp văn bản phải sử dụng bảng mã UTF-8 hợp lệ.");
        }
        for (int i = 0; i < decoded.length(); i++) {
            char c = decoded.charAt(i);
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') {
                throw new SecurityException("Tệp văn bản chứa ký tự điều khiển không an toàn.");
            }
        }
        if (!markdown) {
            return decoded;
        }
        // Normalize simple Markdown syntax while keeping readable Vietnamese structure
        return decoded
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("`([^`]+)`", "$1");
    }

    private static String extractDocxText(byte[] bytes) throws Exception {
        byte[] documentXml = null;
        boolean hasContentTypes = false;
        long totalUncompressed = 0;
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw new SecurityException("Tệp DOCX chứa quá nhiều mục bên trong (nghi ngờ ZIP bomb).");
                }
                String name = entry.getName();
                if (name == null || name.contains("..") || name.startsWith("/") || name.startsWith("\\") || name.contains(":")) {
                    throw new SecurityException("Tệp DOCX chứa đường dẫn nội bộ không an toàn (Zip Slip).");
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.endsWith("vbaproject.bin") || lower.endsWith("vbadata.xml") || lower.contains("macro")) {
                    throw new SecurityException("Từ chối tệp DOCX có chứa macro VBA.");
                }
                if ("[content_types].xml".equals(lower)) {
                    hasContentTypes = true;
                }
                ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
                int n;
                long entryUncompressed = 0;
                while ((n = zis.read(buf)) != -1) {
                    entryUncompressed += n;
                    totalUncompressed += n;
                    if (totalUncompressed > MAX_UNCOMPRESSED_BYTES) {
                        throw new SecurityException("Tệp DOCX vượt quá giới hạn giải nén an toàn (ZIP bomb).");
                    }
                    if (entryUncompressed > (long) bytes.length * MAX_COMPRESSION_RATIO && entryUncompressed > 1048576L) {
                        throw new SecurityException("Tỷ lệ nén của tệp DOCX vượt ngưỡng an toàn (ZIP bomb).");
                    }
                    if ("word/document.xml".equals(lower) || "[content_types].xml".equals(lower)) {
                        entryOut.write(buf, 0, n);
                    }
                }
                if ("[content_types].xml".equals(lower)) {
                    String ct = new String(entryOut.toByteArray(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                    if (ct.contains("macroenabled") || ct.contains("vba")) {
                        throw new SecurityException("Từ chối tệp Office chứa khai báo Macro.");
                    }
                } else if ("word/document.xml".equals(lower)) {
                    documentXml = entryOut.toByteArray();
                }
                zis.closeEntry();
            }
        }

        if (!hasContentTypes || documentXml == null) {
            throw new SecurityException("Cấu trúc tệp DOCX không hợp lệ (thiếu [Content_Types].xml hoặc word/document.xml).");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        dbf.setNamespaceAware(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new ByteArrayInputStream(documentXml)));
        StringBuilder out = new StringBuilder();
        extractWordNodes(doc.getDocumentElement(), out);
        return out.toString();
    }

    private static void extractWordNodes(Node node, StringBuilder out) {
        if (node == null) return;
        String local = node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
        if (local.endsWith(":t") || "t".equals(local)) {
            String text = node.getTextContent();
            if (text != null) out.append(text);
            return;
        }
        if (local.endsWith(":tab") || "tab".equals(local)) {
            out.append(' ');
            return;
        }
        if (local.endsWith(":br") || "br".equals(local) || local.endsWith(":cr") || "cr".equals(local)) {
            out.append('\n');
            return;
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            extractWordNodes(children.item(i), out);
        }
        if (local.endsWith(":p") || "p".equals(local) || local.endsWith(":tr") || "tr".equals(local)) {
            out.append('\n');
        } else if (local.endsWith(":tc") || "tc".equals(local)) {
            out.append(" | ");
        }
    }

    private static String extractPdfText(byte[] bytes) throws Exception {
        String rawIso = new String(bytes, StandardCharsets.ISO_8859_1);
        if (rawIso.contains("/JavaScript") || rawIso.contains("/JS ") || rawIso.contains("/JS(")
                || rawIso.contains("/JS<") || rawIso.contains("/Launch") || rawIso.contains("/EmbeddedFiles")
                || rawIso.contains("/RichMedia")) {
            throw new SecurityException("Từ chối tệp PDF có chứa JavaScript, Launch action hoặc tệp nhúng.");
        }
        if (rawIso.contains("/Encrypt")) {
            throw new IllegalStateException("Tệp PDF được mã hóa mật khẩu, không thể trích xuất tự động.");
        }

        StringBuilder extracted = new StringBuilder();
        int searchPos = 0;
        while (searchPos < rawIso.length()) {
            int streamIdx = rawIso.indexOf("stream", searchPos);
            if (streamIdx < 0) break;
            // Verify 'stream' is not 'endstream'
            if (streamIdx >= 3 && "end".equals(rawIso.substring(streamIdx - 3, streamIdx))) {
                searchPos = streamIdx + 6;
                continue;
            }
            int dataStart = streamIdx + 6;
            if (dataStart < rawIso.length() && rawIso.charAt(dataStart) == '\r') dataStart++;
            if (dataStart < rawIso.length() && rawIso.charAt(dataStart) == '\n') dataStart++;

            int endStreamIdx = rawIso.indexOf("endstream", dataStart);
            if (endStreamIdx < 0) break;

            int dictStart = Math.max(0, streamIdx - 512);
            String dictHeader = rawIso.substring(dictStart, streamIdx);
            int dataEnd = endStreamIdx;
            if (dataEnd > dataStart && rawIso.charAt(dataEnd - 1) == '\n') dataEnd--;
            if (dataEnd > dataStart && rawIso.charAt(dataEnd - 1) == '\r') dataEnd--;

            if (dataEnd > dataStart) {
                byte[] streamBytes = Arrays.copyOfRange(bytes, dataStart, dataEnd);
                String contentStream = null;
                if (dictHeader.contains("/FlateDecode")) {
                    byte[] inflated = inflateBounded(streamBytes);
                    if (inflated != null) {
                        contentStream = new String(inflated, StandardCharsets.ISO_8859_1);
                    }
                } else if (!dictHeader.contains("/DCTDecode") && !dictHeader.contains("/Filter")) {
                    contentStream = new String(streamBytes, StandardCharsets.ISO_8859_1);
                }
                if (contentStream != null) {
                    parsePdfTextOperators(contentStream, extracted);
                }
            }
            searchPos = endStreamIdx + 9;
        }
        return extracted.toString();
    }

    private static byte[] inflateBounded(byte[] compressed) {
        try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            long total = 0;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_UNCOMPRESSED_BYTES) {
                    throw new SecurityException("Tệp PDF chứa luồng nén vượt giới hạn an toàn.");
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (SecurityException se) {
            throw se;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void parsePdfTextOperators(String stream, StringBuilder out) {
        int i = 0;
        int len = stream.length();
        while (i < len) {
            char c = stream.charAt(i);
            if (c == '(') {
                StringBuilder lit = new StringBuilder();
                int depth = 1;
                i++;
                while (i < len && depth > 0) {
                    char ch = stream.charAt(i++);
                    if (ch == '\\' && i < len) {
                        char esc = stream.charAt(i++);
                        if (esc == 'n') lit.append('\n');
                        else if (esc == 'r') lit.append('\r');
                        else if (esc == 't') lit.append('\t');
                        else if (esc == '(' || esc == ')' || esc == '\\') lit.append(esc);
                        else if (esc >= '0' && esc <= '7') {
                            StringBuilder oct = new StringBuilder().append(esc);
                            if (i < len && stream.charAt(i) >= '0' && stream.charAt(i) <= '7') oct.append(stream.charAt(i++));
                            if (i < len && stream.charAt(i) >= '0' && stream.charAt(i) <= '7') oct.append(stream.charAt(i++));
                            lit.append((char) Integer.parseInt(oct.toString(), 8));
                        } else {
                            lit.append(esc);
                        }
                    } else if (ch == '(') {
                        depth++;
                        lit.append(ch);
                    } else if (ch == ')') {
                        depth--;
                        if (depth > 0) lit.append(ch);
                    } else {
                        lit.append(ch);
                    }
                }
                String decoded = decodePdfStringBytes(lit.toString().getBytes(StandardCharsets.ISO_8859_1));
                if (!decoded.isEmpty()) {
                    out.append(decoded).append(' ');
                }
            } else if (c == '<' && i + 1 < len && stream.charAt(i + 1) != '<') {
                int endHex = stream.indexOf('>', i + 1);
                if (endHex > i + 1) {
                    String hex = stream.substring(i + 1, endHex).replaceAll("\\s+", "");
                    if (hex.matches("[0-9A-Fa-f]+") && hex.length() >= 4) {
                        byte[] raw = hexToBytes(hex);
                        String decoded = decodePdfStringBytes(raw);
                        if (!decoded.isEmpty()) {
                            out.append(decoded).append(' ');
                        }
                    }
                    i = endHex + 1;
                } else {
                    i++;
                }
            } else if (c == 'E' && i + 1 < len && stream.charAt(i + 1) == 'T') {
                out.append('\n');
                i += 2;
            } else {
                i++;
            }
        }
    }

    private static byte[] hexToBytes(String hex) {
        String padded = (hex.length() % 2 == 0) ? hex : (hex + "0");
        byte[] out = new byte[padded.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(padded.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static String decodePdfStringBytes(byte[] raw) {
        if (raw == null || raw.length == 0) return "";
        if (raw.length >= 2 && (raw[0] & 0xFF) == 0xFE && (raw[1] & 0xFF) == 0xFF) {
            return new String(raw, 2, raw.length - 2, StandardCharsets.UTF_16BE);
        }
        // Try UTF-8 first; fallback to ISO-8859-1 if invalid UTF-8
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw))
                    .toString();
        } catch (Exception e) {
            return new String(raw, StandardCharsets.ISO_8859_1);
        }
    }

    public static String normalizeExtractedText(String text) {
        if (text == null) return "";
        String cleaned = text.replace("\r\n", "\n").replace('\r', '\n');
        // Preserve markup/instructions for review. Policy decides eligibility; extraction must not bypass it.
        StringBuilder sb = new StringBuilder(cleaned.length());
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '\n' || c == '\t' || c >= 0x20) {
                sb.append(c);
            }
        }
        return sb.toString()
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) {
                out.append(String.format(Locale.ROOT, "%02x", b & 0xFF));
            }
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
