package ai.consultation;

import dto.PackageDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.text.Normalizer;
import java.util.*;

/** Typed, read-only snapshot of the same packages used by the pricing page. */
public final class ProductCatalog {
    public static final class Product {
        public final int id, speed;
        public final long price;
        public final boolean wifi6, camera, tv180, premierLeague;
        Product(PackageDTO p) {
            id = p.getId(); price = p.getPrice(); speed = p.getSpeedMbps();
            String details = normalize(p.getDescription() + " " + p.getLongDescription());
            wifi6 = details.matches("(?s).*wi[ -]?fi\\s*6.*");
            camera = details.matches("(?s).*(?:1|mot)\\s+camera.*");
            tv180 = details.matches("(?s).*180\\s*(?:kenh|channels).*" );
            premierLeague = details.contains("ngoai hang anh") || details.contains("premier league");
        }
        public String label() { return price % 1000 == 0 ? (price / 1000) + "K" : price + "đ"; }
        public String display() {
            StringBuilder b = new StringBuilder("Gói " + label() + ": " + String.format(Locale.US, "%,d", price).replace(',', '.') + "đ/tháng, " + speed + "Mbps");
            if (wifi6) b.append(", modem WiFi 6");
            if (camera) b.append(", 1 Camera");
            if (tv180) b.append(", TV 180 kênh");
            if (premierLeague) b.append(", nội dung Ngoại Hạng Anh");
            return b.append('.').toString();
        }
    }
    private final List<Product> products = new ArrayList<>();
    public ProductCatalog(List<PackageDTO> rows) {
        Set<Integer> seen = new HashSet<>();
        if (rows != null) for (PackageDTO p : rows) {
            if (p != null && p.getId() > 0 && p.getPrice() > 0 && p.getSpeedMbps() > 0 && seen.add(p.getId())) products.add(new Product(p));
        }
        products.sort(Comparator.comparingLong(p -> p.price));
        if (products.size() > 100) throw new IllegalArgumentException("Catalog too large");
    }
    public List<Product> all() { return Collections.unmodifiableList(products); }
    public Product byId(int id) { for (Product p : products) if (p.id == id) return p; return null; }
    public List<Product> mentioned(String question) {
        String q = normalize(question);
        List<Product> found = new ArrayList<>();
        for (Product p : products) {
            String k = Long.toString(p.price / 1000);
            if (q.matches("(?s).*(?<![0-9])" + k + "\\s*(?:k|nghin|ngan)(?![a-z0-9]).*") ||
                q.matches("(?s).*goi\\s+" + k + "(?![0-9]).*") ||
                (q.contains("so sanh") && q.matches("(?s).*(?<![0-9])" + k + "(?![0-9]).*"))) found.add(p);
        }
        return found;
    }
    public JsonArray json() {
        JsonArray a = new JsonArray();
        for (Product p : products) {
            JsonObject o = new JsonObject(); o.addProperty("id", p.id); o.addProperty("priceVnd", p.price);
            o.addProperty("speedMbps", p.speed); o.addProperty("wifi6", p.wifi6); o.addProperty("oneCamera", p.camera);
            o.addProperty("tv180", p.tv180); o.addProperty("premierLeague", p.premierLeague); a.add(o);
        }
        return a;
    }
    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
