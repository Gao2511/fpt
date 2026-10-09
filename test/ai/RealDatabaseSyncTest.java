package ai;

import ai.consultation.ConsultationEngine;
import ai.consultation.ConsultationPolicy;
import ai.consultation.ProductCatalog;
import ai.dto.ChatOptions;
import ai.dto.ChatResponse;
import ai.session.ChatSessionData;
import dao.PackageDAO;
import dto.PackageDTO;

import java.util.List;

public class RealDatabaseSyncTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== 1. VERIFYING DATABASE PACKAGES (SUPABASE POSTGRESQL) ===");
            PackageDAO packageDAO = new PackageDAO();
            List<PackageDTO> dbPackages = packageDAO.getAll();
            
            if (dbPackages.size() != 6) {
                throw new AssertionError("Expected 6 packages in DB, but got " + dbPackages.size());
            }

            int[] expectedPrices = {195000, 205000, 220000, 230000, 239000, 249000};
            int[] expectedSpeeds = {300, 300, 300, 300, 1000, 1000};

            for (int i = 0; i < 6; i++) {
                PackageDTO p = dbPackages.get(i);
                System.out.printf("Package #%d: Code=%s, Name=%s, Price=%,d VNĐ, Speed=%d Mbps\n",
                    p.getId(), p.getPackageCode(), p.getName(), p.getPrice(), p.getSpeedMbps());
                
                if (p.getPrice() != expectedPrices[i]) {
                    throw new AssertionError("Mismatch price for package " + p.getName() + ": expected " + expectedPrices[i] + " but got " + p.getPrice());
                }
                if (p.getSpeedMbps() != expectedSpeeds[i]) {
                    throw new AssertionError("Mismatch speed for package " + p.getName() + ": expected " + expectedSpeeds[i] + " Mbps but got " + p.getSpeedMbps() + " Mbps");
                }
            }
            System.out.println(">>> PASS: All 6 packages in database have exact reference speeds and prices!");

            System.out.println("\n=== 2. VERIFYING CHATBOT CONSULTATION WITH REAL CATALOG ===");
            ProductCatalog catalog = new ProductCatalog(dbPackages);
            ConsultationEngine engine = new ConsultationEngine();

            // Scenario 1: Gói 239K tốc độ bao nhiêu?
            ChatSessionData s1 = new ChatSessionData("test-speed-239");
            ChatResponse r1 = engine.chat("Gói 239K tốc độ bao nhiêu?", s1, catalog, null, null, new ChatOptions(0.3, 1200));
            System.out.println("\n[Q1]: Gói 239K tốc độ bao nhiêu?\n[A1]: " + r1.getContent());
            if (!r1.getContent().contains("1000") || (!r1.getContent().contains("Mbps") && !r1.getContent().contains("1 Gbps"))) {
                throw new AssertionError("Chatbot must answer 1000 Mbps for 239K: " + r1.getContent());
            }

            // Scenario 2: Gói 220K tốc độ bao nhiêu?
            ChatSessionData s2 = new ChatSessionData("test-speed-220");
            ChatResponse r2 = engine.chat("Gói 220K tốc độ bao nhiêu?", s2, catalog, null, null, new ChatOptions(0.3, 1200));
            System.out.println("\n[Q2]: Gói 220K tốc độ bao nhiêu?\n[A2]: " + r2.getContent());
            if (!r2.getContent().contains("300") || !r2.getContent().contains("Mbps")) {
                throw new AssertionError("Chatbot must answer 300 Mbps for 220K: " + r2.getContent());
            }

            // Scenario 3: Nhà anh 2 tầng nên dùng WiFi nào?
            ChatSessionData s3 = new ChatSessionData("test-mesh-f1");
            ChatResponse r3 = engine.chat("Nhà anh 2 tầng nên dùng WiFi nào?", s3, catalog, null, null, new ChatOptions(0.3, 1200));
            System.out.println("\n[Q3]: Nhà anh 2 tầng nên dùng WiFi nào?\n[A3]: " + r3.getContent());
            if (!r3.getContent().contains("F1") || !r3.getContent().contains("500.000") || !r3.getContent().contains("100.000") || !r3.getContent().contains("Access Point")) {
                throw new AssertionError("Chatbot must recommend Mesh F1 for 2 floors: " + r3.getContent());
            }

            // Scenario 4: Nhà anh 3 tầng dùng F1 được không?
            ChatSessionData s4 = new ChatSessionData("test-mesh-f2");
            ChatResponse r4 = engine.chat("Nhà anh 3 tầng dùng F1 được không?", s4, catalog, null, null, new ChatOptions(0.3, 1200));
            System.out.println("\n[Q4]: Nhà anh 3 tầng dùng F1 được không?\n[A4]: " + r4.getContent());
            if (!r4.getContent().contains("F2") || !r4.getContent().contains("700.000") || !r4.getContent().contains("200.000") || !r4.getContent().contains("Access Point")) {
                throw new AssertionError("Chatbot must explain F1 limits and recommend Mesh F2 for 3 floors: " + r4.getContent());
            }

            System.out.println("\n>>> ALL TESTS PASSED SUCCESSFULLY! FULL DATA SYNCHRONIZATION CONFIRMED.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
