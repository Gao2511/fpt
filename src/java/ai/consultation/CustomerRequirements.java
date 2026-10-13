package ai.consultation;

import com.google.gson.JsonObject;
import java.io.Serializable;
import java.util.regex.*;

/** Only bounded facts from customer input; never stores instructions or contact PII. */
public final class CustomerRequirements implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long budget;
    public Integer people, devices, selectedPackageId, floors, cameraCount;
    public String meshInterest;
    public boolean gaming, streaming, businessUse, student, television, camera;
    public boolean televisionKnown, cameraKnown, meshPreferenceKnown, meshWanted, registrationFormOpened;
    public boolean amenitiesAsked, selectionAmbiguous;
    public String style = "formal", stylePreference, address;
    public transient Long previousBudget;
    public transient boolean requirementsChanged;
    public void clear() {
        budget = null; people = null; devices = null; selectedPackageId = null; floors = null; cameraCount = null; meshInterest = null;
        gaming = streaming = businessUse = student = television = camera = false;
        televisionKnown = cameraKnown = meshPreferenceKnown = meshWanted = registrationFormOpened = amenitiesAsked = selectionAmbiguous = false;
        style = "formal"; stylePreference = address = null;
        previousBudget = null; requirementsChanged = false;
    }
    public void update(String text) {
        String q = ProductCatalog.normalize(text);
        previousBudget = budget;
        requirementsChanged = false;
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
            if (q.contains("mesh") || q.matches("(?s).*\\bf[12]\\b.*")) { meshPreferenceKnown = true; meshWanted = true; requirementsChanged = true; }
            if (q.matches("(?s).*\\bf1\\b.*") || (!meshPreferenceKnown || meshWanted) && floors != null && floors == 2) meshInterest = "F1";
            if (q.matches("(?s).*\\bf2\\b.*") || (!meshPreferenceKnown || meshWanted) && floors != null && floors >= 3) meshInterest = "F2";
        }
        Matcher money = Pattern.compile("(?:ngan sach|toi co|em co|minh co|chi co|toi chi co|tam|duoi|toi da|tang len|giam xuong|doi thanh)\\s*(\\d{2,3})(?:[.,]000)?\\s*(?:k|nghin|ngan|dong|d)?\\b").matcher(q);
        if (money.find()) { budget = Long.parseLong(money.group(1)) * 1000; requirementsChanged = true; }
        if (q.contains("khong choi game")) gaming = false;
        else if (q.contains("game") || q.contains("gaming") || q.contains("valorant") || q.contains("lien minh")) { gaming = true; requirementsChanged = true; }
        if (q.matches("(?s).*(?:xem phim|stream|netflix|youtube).*")) { streaming = true; requirementsChanged = true; }
        if (q.matches("(?s).*(?:kinh doanh|van phong|cua hang|shop (?:cua|nha)|doanh nghiep).*")) { businessUse = true; requirementsChanged = true; }
        if (q.contains("sinh vien")) student = true;
        // A question about a benefit does not establish a purchase requirement.
        boolean asking = q.contains("?") || q.matches("(?s).*\\b(?:khong|ko|hong|khum)\\s*[.!]*$") || q.contains("thi sao") || q.matches("(?s).*camera sao.*");
        if (q.matches("(?s).*(?:khong can|khong muon|bo|khong xem)\\s+camera.*")) { camera = false; cameraCount = null; cameraKnown = true; requirementsChanged = true; }
        else if (!asking && q.contains("camera")) { camera = true; cameraKnown = true; requirementsChanged = true; }
        Matcher cameras = Pattern.compile("(?<![0-9])(\\d{1,2})\\s*camera\\b").matcher(counts);
        if (cameras.find()) { int n = Integer.parseInt(cameras.group(1)); if (n > 0) { cameraCount = n; camera = true; cameraKnown = true; requirementsChanged = true; } }
        if (q.matches("(?s).*(?:khong can|khong muon|bo|khong xem)\\s+(?:truyen hinh|tivi).*")) { television = false; televisionKnown = true; requirementsChanged = true; }
        else if (!asking && (q.contains("truyen hinh") || q.contains("tivi") || q.contains("bong da"))) { television = true; televisionKnown = true; requirementsChanged = true; }
        ConversationSignals.style(text, this);
    }
    public JsonObject json() {
        JsonObject o = new JsonObject();
        o.addProperty("budgetVnd", budget);
        o.addProperty("people", people);
        o.addProperty("devices", devices);
        o.addProperty("floors", floors);
        o.addProperty("cameraCount", cameraCount);
        o.addProperty("meshInterest", meshInterest);
        o.addProperty("meshPreferenceKnown", meshPreferenceKnown);
        o.addProperty("meshWanted", meshWanted);
        o.addProperty("gaming", gaming);
        o.addProperty("streaming", streaming);
        o.addProperty("businessUse", businessUse);
        o.addProperty("student", student);
        o.addProperty("television", television);
        o.addProperty("camera", camera);
        o.addProperty("selectedPackageId", selectedPackageId);
        o.addProperty("televisionPreferenceKnown", televisionKnown);
        o.addProperty("cameraPreferenceKnown", cameraKnown);
        o.addProperty("communicationStyle", style);
        o.addProperty("preferredAddress", address);
        o.addProperty("registrationFormOpened", registrationFormOpened);
        o.addProperty("selectionAmbiguous", selectionAmbiguous);
        o.addProperty("amenitiesAlreadyAsked", amenitiesAsked);
        return o;
    }
}
