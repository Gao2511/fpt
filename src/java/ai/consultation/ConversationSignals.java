package ai.consultation;

/** Conservative current-turn signals; unfamiliar text still goes to the model with history. */
public final class ConversationSignals {
    private ConversationSignals() {}
    public static String text(String input) {
        return ProductCatalog.normalize(input).trim().replaceAll("\\s+", " ");
    }
    public static boolean greeting(String input) {
        return text(input).matches("(?:hi(?: a)?|hello(?: shop)?|alo|e shop|shop oi|hi|chao(?: em|ban|shop)?|good morning|co ai o day khong)[.!? ]*");
    }
    public static boolean social(String input) {
        return text(input).matches("(?:haha(?: dua thoi)?|dua thoi|cam on(?: em|shop)?|thanks|oke|ok|ua vay ha)[.!? ]*");
    }
    public static boolean playful(String input) {
        String q = text(input);
        return q.matches(".*(?:mang|lap|shop|goi).*(?:mat trang|pro player|cao thu|kiep sau|phep thuat).*");
    }
    public static boolean handoff(String input) {
        return text(input).matches(".*(?:gap|muon|can|noi chuyen voi|lien he|goi cho).*(?:nguoi that|nhan vien|tu van vien|con nguoi).*|.*(?:so hotline|so dien thoai tu van|zalo|tai khoan|hoa don|cuoc no|khieu nai|hop dong).*" );
    }
    public static boolean outside(String input) {
        // Only complete, high-confidence unrelated tasks. No substring ban on game/film/etc.
        return text(input).matches(".*(?:viet|lam|giai|lap trinh).*(?:code java|code python|bai tap|bai van|phan mem|chuong trinh java).*" );
    }
    public static boolean ambiguous(String input) {
        return text(input).matches("(?:cho|lay|chon) (?:cai|goi) (?:kia|do|nay)[.!? ]*|(?:co|khong|uh|um|vang)[.!? ]*");
    }
    public static boolean service(String input, CustomerRequirements memory, ProductCatalog catalog) {
        String q = text(input);
        if (outside(input)) return false;
        if (!catalog.mentioned(input).isEmpty() || q.matches(".*(?:internet|mang|goi|camera|truyen hinh|tivi|wifi|wi-fi|mesh|game|ngan sach|nghin|thiet bi|nguoi|may|sinh vien|o tro).*")) return true;
        return memory.selectedPackageId != null && q.matches("(?:cai nay|goi nay|vay|the).{0,45}");
    }
    public static void style(String input, CustomerRequirements memory) {
        String q = text(input);
        if (q.contains("goi toi la ban") || q.contains("goi minh la ban") || q.contains("goi em la ban")) memory.address = "bạn";
        else if (q.contains("goi toi la anh")) memory.address = "anh";
        else if (q.contains("goi toi la chi")) memory.address = "chị";
        if (q.matches(".*(?:noi|tra loi|tu van) (?:lich su|trang trong).*")) memory.stylePreference = "formal";
        else if (q.matches(".*(?:noi|tra loi|tu van) (?:than thien|thoai mai|tu nhien).*")) memory.stylePreference = "casual";
        else if (q.matches(".*(?:noi|tra loi) (?:ngan gon|ngan thoi).*")) memory.stylePreference = "direct";
        if (memory.stylePreference != null) memory.style = memory.stylePreference;
        else if (q.matches(".*\\b(?:toi|nho em|gia dinh)\\b.*")) memory.style = "formal";
        else if (playful(input) || q.matches(".*\\b(?:hong|khum|ko|on ap|ngon bo re|dua|shop|hi a|dua thoi)\\b.*")) memory.style = "casual";
        else if (q.matches("(?:gia .*|so sanh .*|dang ky .*|gui bang gia|co camera khong)[.!? ]*")) memory.style = "direct";
    }
    public static String address(CustomerRequirements memory) { return memory.address == null ? "anh/chị" : memory.address; }
    public static String opening(CustomerRequirements memory) {
        return "casual".equals(memory.style) ? "Dạ, mình xem thử nhé! " : "direct".equals(memory.style) ? "" : "Dạ, ";
    }
}
