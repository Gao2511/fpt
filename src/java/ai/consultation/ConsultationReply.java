package ai.consultation;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import ai.dto.ChatResponse;
import java.io.StringReader;
import java.util.*;

/** Strict envelope validation. Invalid prose is retried, never stripped or displayed. */
public final class ConsultationReply {
    public String message, intent, action;
    public Integer recommendedPackageId;
    public final List<Integer> packageIds = new ArrayList<>();

    public static ConsultationReply parse(ChatResponse response, ProductCatalog catalog, String apiKey) throws Exception {
        if (response == null || !Arrays.asList("stop", "STOP", "end_turn").contains(response.getFinishReason())) throw new IllegalArgumentException("Incomplete response");
        String raw = response.getContent();
        if (raw == null || raw.length() > 16000) throw new IllegalArgumentException("Invalid size");
        ConsultationReply reply = new ConsultationReply();
        Set<String> fields = new HashSet<>();
        try (JsonReader r = new JsonReader(new StringReader(raw))) {
            r.setLenient(false); r.beginObject();
            while (r.hasNext()) {
                String key = r.nextName();
                if (!fields.add(key)) throw new IllegalArgumentException("Duplicate field");
                switch (key) {
                    case "message": reply.message = string(r); break;
                    case "intent": reply.intent = string(r); break;
                    case "action": reply.action = string(r); break;
                    case "recommendedPackageId":
                        if (r.peek() == JsonToken.NULL) r.nextNull();
                        else reply.recommendedPackageId = id(r, catalog);
                        break;
                    case "packageIds":
                        r.beginArray();
                        while (r.hasNext()) {
                            int id = id(r, catalog);
                            if (reply.packageIds.size() >= 100 || reply.packageIds.contains(id)) throw new IllegalArgumentException("Invalid package list");
                            reply.packageIds.add(id);
                        }
                        r.endArray(); break;
                    default: throw new IllegalArgumentException("Unknown field");
                }
            }
            r.endObject();
            if (r.peek() != JsonToken.END_DOCUMENT || fields.size() != 5) throw new IllegalArgumentException("Invalid envelope");
        }
        if (!Arrays.asList("consultation", "comparison", "registration", "out_of_scope").contains(reply.intent) ||
            !Arrays.asList("none", "open_registration").contains(reply.action)) throw new IllegalArgumentException("Invalid enum");
        if ("open_registration".equals(reply.action) && (!"registration".equals(reply.intent) || reply.recommendedPackageId == null)) throw new IllegalArgumentException("Invalid action");
        if ("out_of_scope".equals(reply.intent) && (!reply.packageIds.isEmpty() || reply.recommendedPackageId != null || !"none".equals(reply.action))) throw new IllegalArgumentException("Invalid scope");
        validateMessage(reply.message, apiKey);
        return reply;
    }
    private static String string(JsonReader r) throws Exception {
        if (r.peek() != JsonToken.STRING) throw new IllegalArgumentException("Expected string");
        return r.nextString().trim();
    }
    private static int id(JsonReader r, ProductCatalog catalog) throws Exception {
        if (r.peek() != JsonToken.NUMBER) throw new IllegalArgumentException("Expected integer");
        String number = r.nextString();
        if (!number.matches("[1-9][0-9]{0,8}")) throw new IllegalArgumentException("Invalid ID");
        int id = Integer.parseInt(number);
        if (catalog.byId(id) == null) throw new IllegalArgumentException("Unknown package");
        return id;
    }
    static void validateMessage(String message, String apiKey) {
        if (message == null || message.isEmpty() || message.length() > 1800 ||
            !message.matches("(?s).*[\\p{IsLatin}].*") ||
            !message.matches("(?s).*[àáảãạăâèéẻẽẹêìíỉĩịòóỏõọôơùúủũụưỳýỷỹỵđÀÁĐ].*")) throw new IllegalArgumentException("Expected Vietnamese text");
        String q = ProductCatalog.normalize(message);
        // Product facts are rendered separately by the server. Prose cannot assert them.
        if (message.matches("(?s).*[0-9<>{}`].*") || q.matches("(?s).*\\b(?:state tracking|tag constraint|system|instruction|chi dan he thong|suy luan noi bo|trang thai noi bo|sotay|reasoning|analysis|api.?key|password|token|https?://|mbps|gbps|wifi|wi-fi|modem|camera|kenh|ngoai hang|mien phi|uu dai|giam gia|cam ket|hop dong|lap dat trong|da dang ky|dang ky thanh cong|da luu|da gui|da tiep nhan|da chot|ky thuat vien se|bao hanh|phi lap|phi hoa|coverage|i will|i can|let me)\\b.*")) throw new IllegalArgumentException("Unverified prose");
        if (apiKey != null && apiKey.length() > 6 && message.contains(apiKey)) throw new IllegalArgumentException("Secret in response");
        if (!message.matches("(?s).*[.!?…。]$")) throw new IllegalArgumentException("Unfinished sentence");
    }
}
