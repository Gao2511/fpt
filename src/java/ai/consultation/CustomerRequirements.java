package ai.consultation;

import com.google.gson.JsonObject;
import java.io.Serializable;
import java.util.regex.*;

/** Only bounded facts from customer input; never stores instructions or contact PII. */
public final class CustomerRequirements implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long budget;
    public Integer people, devices, selectedPackageId, floors, cameraCount, requestedSpeedMbps, prepaymentMonths;
    public String meshInterest, serviceArea;
    public boolean gaming, streaming, businessUse, student, television, camera, football, smartTv;
    public boolean televisionKnown, cameraKnown, meshPreferenceKnown, meshWanted, registrationFormOpened;
    public boolean amenitiesAsked, locationAsked, consultationStarted, selectionAmbiguous;
    public String style = "formal", stylePreference, address;
    public transient Long previousBudget;
    public transient boolean requirementsChanged;
    public void clear() {
        budget = null; people = null; devices = null; selectedPackageId = null; floors = null; cameraCount = null; requestedSpeedMbps = null; prepaymentMonths = null;
        meshInterest = serviceArea = null;
        gaming = streaming = businessUse = student = television = camera = football = smartTv = false;
        televisionKnown = cameraKnown = meshPreferenceKnown = meshWanted = registrationFormOpened = amenitiesAsked = locationAsked = consultationStarted = selectionAmbiguous = false;
        style = "formal"; stylePreference = address = null;
        previousBudget = null; requirementsChanged = false;
    }
    public void update(String text) {
        String q = ProductCatalog.normalize(text);
        previousBudget = budget;
        requirementsChanged = false;
        if (q.matches("(?s).*(?:muon|can)\\s+(?:lap|dang ky)\\s+(?:mang|internet).*")) consultationStarted = true;
        String counts = q.replaceAll("\\bmot\\b", "1").replaceAll("\\bhai\\b", "2").replaceAll("\\bba\\b", "3")
            .replaceAll("\\bbon\\b", "4").replaceAll("\\bnam(?=\\s+(?:nguoi|dua|may|thiet bi))", "5");
        Matcher persons = Pattern.compile("(?<![0-9])(\\d{1,2})\\s*(?:nguoi|dua)\\b").matcher(counts);
        if (persons.find()) { int n = Integer.parseInt(persons.group(1)); if (n > 0) { people = n; requirementsChanged = true; } }
        else if (q.contains("mot minh")) { people = 1; requirementsChanged = true; }
        Matcher deviceCount = Pattern.compile("(?<![0-9])(\\d{1,2})\\s*(?:may|thiet bi)\\b").matcher(counts);
        if (deviceCount.find()) { int n = Integer.parseInt(deviceCount.group(1)); if (n > 0) { devices = n; requirementsChanged = true; } }
        Matcher floorM = Pattern.compile("(?<![0-9])(\\d{1,2})\\s*tang").matcher(counts);
        if (floorM.find()) { int fl = Integer.parseInt(floorM.group(1)); if (fl > 0) { floors = fl; requirementsChanged = true; } }
        boolean declinesMesh = q.matches("(?s).*(?:khong can|khong muon|khong dung|bo)\\s+(?:dung )?(?:mesh|wifi mesh).*" )
            || q.matches("(?s).*chi (?:muon|can) goi internet.*");
        if (declinesMesh) { meshPreferenceKnown = true; meshWanted = false; meshInterest = null; requirementsChanged = true; }
        else {
            boolean explicitF1 = q.matches("(?s).*\\bf1\\b.*"), explicitF2 = q.matches("(?s).*\\bf2\\b.*");
            if (q.contains("mesh") || explicitF1 || explicitF2) { meshPreferenceKnown = true; meshWanted = true; requirementsChanged = true; }
            if (explicitF1) meshInterest = "F1";
            else if (explicitF2) meshInterest = "F2";
            else if ((!meshPreferenceKnown || meshWanted) && floors != null) meshInterest = floors >= 3 ? "F2" : floors == 2 ? "F1" : meshInterest;
        }
        Matcher money = Pattern.compile("(?:ngan sach|toi co|em co|minh co|chi co|toi chi co|tam|duoi|toi da|tang len|giam xuong|doi thanh)\\s*(\\d{2,3})(?:[.,]000)?\\s*(?:k|nghin|ngan|dong|d)?\\b").matcher(q);
        if (money.find()) { budget = Long.parseLong(money.group(1)) * 1000; requirementsChanged = true; }
        Matcher speed = Pattern.compile("(?<![0-9])(300|500|1000)\\s*(?:mbps|m|meg|gbps|g)\\b").matcher(q.replaceAll("(?<![0-9])1\\s*gbps", "1000mbps"));
        if (speed.find()) { requestedSpeedMbps = Integer.parseInt(speed.group(1)); requirementsChanged = true; }
        Matcher prepay = Pattern.compile("(?:dong|tra)\\s*truoc\\s*(6|12|sau|muoi hai)\\s*thang").matcher(q);
        if (prepay.find()) { prepaymentMonths = prepay.group(1).matches("6|sau") ? 6 : 12; requirementsChanged = true; }
        if (q.matches("(?s).*(?:khong muon|khong can|khong thich).*dong truoc.*")) prepaymentMonths = null;
        if (q.contains("khong choi game")) gaming = false;
        else if (q.contains("game") || q.contains("gaming") || q.contains("valorant") || q.contains("lien minh")) { gaming = true; requirementsChanged = true; }
        if (q.matches("(?s).*(?:xem phim|stream|netflix|youtube).*")) { streaming = true; requirementsChanged = true; }
        if (q.matches("(?s).*(?:kinh doanh|van phong|cua hang|shop (?:cua|nha)|doanh nghiep).*")) { businessUse = true; requirementsChanged = true; }
        if (q.contains("sinh vien")) student = true;
        // A question about a benefit does not establish a purchase requirement.
        boolean asking = q.contains("?") || q.matches("(?s).*\\b(?:khong|ko|hong|khum)\\s*[.!]*$") || q.contains("thi sao") || q.matches("(?s).*camera sao.*");
        if (q.matches("(?s).*(?:khong can|khong muon|bo|khong xem|khong lay|khong)\\s+camera.*")) { camera = false; cameraCount = null; cameraKnown = true; requirementsChanged = true; }
        else if (!asking && q.contains("camera")) { camera = true; cameraKnown = true; requirementsChanged = true; }
        Matcher cameras = Pattern.compile("(?<![0-9])(\\d{1,2})\\s*camera\\b").matcher(counts);
        if (cameras.find()) { int n = Integer.parseInt(cameras.group(1)); if (n > 0) { cameraCount = n; camera = true; cameraKnown = true; requirementsChanged = true; } }
        if (q.matches("(?s).*(?:khong can|khong muon|bo|khong xem)\\s+(?:truyen hinh|tivi|smart tv).*")) { television = false; televisionKnown = true; requirementsChanged = true; }
        else if (!asking && (q.contains("truyen hinh") || q.contains("tivi") || q.contains("smart tv") || q.contains("bong da"))) { television = true; televisionKnown = true; requirementsChanged = true; }
        if (q.contains("smart tv")) { smartTv = true; television = true; televisionKnown = true; requirementsChanged = true; }
        if (q.contains("bong da") || q.contains("ngoai hang anh") || q.contains("premier league")) { football = true; television = true; televisionKnown = true; requirementsChanged = true; }
        Matcher area = Pattern.compile("(?iu)(?:^|[,;.]\\s*)(?:(?:tại|ở)\\s+)?((?:xã|phường|thị trấn|huyện)\\s+[\\p{L}][\\p{L} .'-]{1,70})(?:[.!?]|$)").matcher(text == null ? "" : text.trim());
        if (area.find()) {
            String value = area.group(1).trim().replaceAll("[.!?]+$", "").trim().replaceAll("\\s+", " ");
            if (value.length() <= 80) { serviceArea = value; requirementsChanged = true; }
        }
        ConversationSignals.style(text, this);
    }
    public JsonObject json() {
        JsonObject o = new JsonObject();
        o.addProperty("budgetVnd", budget);
        o.addProperty("people", people);
        o.addProperty("devices", devices);
        o.addProperty("floors", floors);
        o.addProperty("cameraCount", cameraCount);
        o.addProperty("requestedSpeedMbps", requestedSpeedMbps);
        o.addProperty("prepaymentMonths", prepaymentMonths);
        o.addProperty("serviceArea", serviceArea);
        o.addProperty("meshInterest", meshInterest);
        o.addProperty("meshPreferenceKnown", meshPreferenceKnown);
        o.addProperty("meshWanted", meshWanted);
        o.addProperty("gaming", gaming);
        o.addProperty("streaming", streaming);
        o.addProperty("businessUse", businessUse);
        o.addProperty("student", student);
        o.addProperty("television", television);
        o.addProperty("camera", camera);
        o.addProperty("football", football);
        o.addProperty("smartTv", smartTv);
        o.addProperty("selectedPackageId", selectedPackageId);
        o.addProperty("televisionPreferenceKnown", televisionKnown);
        o.addProperty("cameraPreferenceKnown", cameraKnown);
        o.addProperty("communicationStyle", style);
        o.addProperty("preferredAddress", address);
        o.addProperty("registrationFormOpened", registrationFormOpened);
        o.addProperty("selectionAmbiguous", selectionAmbiguous);
        o.addProperty("amenitiesAlreadyAsked", amenitiesAsked);
        o.addProperty("locationAlreadyAsked", locationAsked);
        o.addProperty("consultationStarted", consultationStarted);
        return o;
    }
}
