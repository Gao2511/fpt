package ai.consultation;

import com.google.gson.JsonObject;
import java.io.Serializable;
import java.util.regex.*;

/** Only bounded facts from customer input; never stores instructions or contact PII. */
public final class CustomerRequirements implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long budget;
    public Integer people, selectedPackageId, floors;
    public String meshInterest;
    public boolean gaming, student, television, camera;
    public void clear() {
        budget = null; people = null; selectedPackageId = null; floors = null; meshInterest = null;
        gaming = student = television = camera = false;
    }
    public void update(String text) {
        String q = ProductCatalog.normalize(text);
        Matcher persons = Pattern.compile("(?<![0-9])(\\d{1,2})\\s*nguoi").matcher(q);
        if (persons.find()) { int n = Integer.parseInt(persons.group(1)); if (n > 0) people = n; }
        Matcher floorM = Pattern.compile("(?<![0-9])(\\d{1,2})\\s*tang").matcher(q);
        if (floorM.find()) { int fl = Integer.parseInt(floorM.group(1)); if (fl > 0) floors = fl; }
        if (q.contains("f1") || (floors != null && floors == 2)) meshInterest = "F1";
        if (q.contains("f2") || (floors != null && floors >= 3)) meshInterest = "F2";
        Matcher money = Pattern.compile("(?:ngan sach|toi co|minh co|chi co|toi chi co|tam|duoi|toi da)\\s*(\\d{2,3})(?:[.,]000)?\\s*(?:k|nghin|ngan|dong|d)?\\b").matcher(q);
        if (money.find()) budget = Long.parseLong(money.group(1)) * 1000;
        if (q.contains("khong choi game")) gaming = false;
        else if (q.contains("game") || q.contains("gaming")) gaming = true;
        if (q.contains("sinh vien")) student = true;
        if (q.contains("khong can camera")) camera = false;
        else if (q.contains("camera")) camera = true;
        if (q.contains("khong can truyen hinh") || q.contains("khong xem tivi")) television = false;
        else if (q.contains("truyen hinh") || q.contains("tivi") || q.contains("bong da")) television = true;
    }
    public JsonObject json() {
        JsonObject o = new JsonObject();
        o.addProperty("budgetVnd", budget);
        o.addProperty("people", people);
        o.addProperty("floors", floors);
        o.addProperty("meshInterest", meshInterest);
        o.addProperty("gaming", gaming);
        o.addProperty("student", student);
        o.addProperty("television", television);
        o.addProperty("camera", camera);
        o.addProperty("selectedPackageId", selectedPackageId);
        return o;
    }
}
