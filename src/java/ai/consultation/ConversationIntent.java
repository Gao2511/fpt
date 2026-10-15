package ai.consultation;

/** Conservative server-side intent used to select response depth and suppress unrelated catalog output. */
public enum ConversationIntent {
    GREETING,
    DIRECT_PRICE_QUESTION,
    PACKAGE_COMPARISON,
    INTERNET_RECOMMENDATION,
    MESH_COVERAGE,
    TECHNICAL_EXPLANATION,
    TV_AND_FOOTBALL,
    CAMERA,
    INSTALLATION_COST,
    PROMOTION,
    PRICE_OBJECTION_INSTALLATION,
    PRICE_OBJECTION_MONTHLY,
    COMPETITOR_COMPARISON,
    PREPAYMENT_REJECTION,
    REGISTRATION,
    CUSTOMER_SUPPORT,
    COMPLAINT,
    WIFI_TROUBLESHOOTING,
    BUSINESS_CONSULTATION,
    RENTAL_CONSULTATION,
    SERVICE_RELOCATION,
    SERVICE_CANCELLATION,
    PRIVACY,
    AI_IDENTITY,
    OUT_OF_SCOPE,
    GENERAL;

    public static ConversationIntent classify(String input, CustomerRequirements memory, ProductCatalog catalog) {
        String q = ProductCatalog.normalize(input);
        if (ConsultationPolicy.registrationRequested(input)) return REGISTRATION;
        if (ConversationSignals.outside(input)) return OUT_OF_SCOPE;
        if (ConversationSignals.greeting(input)) return GREETING;
        if (q.matches("(?s).*(?:co phai|la)\\s+(?:nguoi(?: that)?|robot|ai).*")) return AI_IDENTITY;
        if (q.matches("(?s).*(?:bao mat|du lieu ca nhan|quyen rieng tu|xoa du lieu).*")) return PRIVACY;
        if (q.matches("(?s).*(?:khieu nai|that vong|qua te|buc minh|khong hai long).*")) return COMPLAINT;
        if (q.matches("(?s).*(?:chuyen nha|chuyen dia chi|chuyen dia diem).*")) return SERVICE_RELOCATION;
        if (q.matches("(?s).*(?:huy dich vu|ngung dich vu|tam ngung|cat mang).*")) return SERVICE_CANCELLATION;
        if (q.matches("(?s).*(?:mat mang|khong vao mang|wifi yeu|song yeu|chap chon|bao hong).*")) return WIFI_TROUBLESHOOTING;
        if (q.matches("(?s).*(?:kinh doanh|cua hang|quan ca phe|homestay|khach san|van phong|doanh nghiep).*")) return BUSINESS_CONSULTATION;
        if (q.matches("(?s).*\\b(?:phong tro|o tro|nha thue|thue nha)\\b.*")) return RENTAL_CONSULTATION;
        if (ConversationSignals.handoff(input) || q.matches("(?s).*(?:mat mang|khong vao mang|bao hong|ho tro ky thuat).*")) return CUSTOMER_SUPPORT;
        if (q.matches("(?s).*(?:khong muon|khong can|khong thich).*?(?:dong|tra) truoc.*")) return PREPAYMENT_REJECTION;
        if (q.matches("(?s).*(?:viettel|vnpt|nha mang khac|ben khac).*(?:gia|phi|re|dat|so sanh|thiet bi|[0-9]{3}\\s*k).*")) return COMPETITOR_COMPARISON;
        if (q.matches("(?s).*(?:700\\s*k|700[.]?000|phi lap f2|phi f2).*(?:cao|dat|mac|re|giam|tiet kiem).*")) return PRICE_OBJECTION_INSTALLATION;
        if (q.matches("(?s).*(?:259\\s*k|259[.]?000|cuoc|hang thang|moi thang).*(?:cao|dat|mac|re|giam|tiet kiem).*")) return PRICE_OBJECTION_MONTHLY;
        if (q.matches("(?s).*(?:uu dai|khuyen mai|tra truoc|dong truoc).*")) return PROMOTION;
        if (q.matches("(?s).*(?:phi lap|phi hoa mang|gia lap|lap dat.*bao nhieu|f[12].*bao nhieu).*")) return INSTALLATION_COST;
        if (q.contains("so sanh") || q.contains("khac nhau") || q.contains("tat ca") || q.contains("6 goi") || catalog.mentioned(input).size() > 1) return PACKAGE_COMPARISON;
        if (q.matches("(?s).*(?:camera|ghi hinh).*")) return CAMERA;
        if (q.matches("(?s).*(?:truyen hinh|tivi|smart tv|bong da|ngoai hang anh|fpt play).*")) return TV_AND_FOOTBALL;
        if (q.matches("(?s).*(?:mesh|wifi yeu|song yeu|tang tren|nhieu tang|f1|f2|goc chet|tuong day).*")) return MESH_COVERAGE;
        if (q.matches("(?s).*(?:1000\\s*mbps|1\\s*gbps|toc do|bang thong|ping|do tre|wifi 6|wifi 5).*")) return TECHNICAL_EXPLANATION;
        if (q.matches("(?s).*(?:bao nhieu|gia|cuoc|moi thang|/thang).*")) return DIRECT_PRICE_QUESTION;
        if (memory.requirementsChanged || q.matches("(?s).*(?:tu van|chon goi|goi nao|muon lap mang|can lap mang).*")) return INTERNET_RECOMMENDATION;
        return GENERAL;
    }

    public boolean allowsCatalogDetails() {
        return this == PACKAGE_COMPARISON || this == INTERNET_RECOMMENDATION || this == DIRECT_PRICE_QUESTION
            || this == TV_AND_FOOTBALL || this == CAMERA;
    }
}
