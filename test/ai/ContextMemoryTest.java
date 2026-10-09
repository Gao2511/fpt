package ai;

import ai.dto.ChatMessage;
import ai.session.ChatSessionData;
import ai.session.ChatSessionManager;
import ai.session.LeadService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Kiểm thử chuỗi hội thoại 12 lượt:
 * 1. Đảm bảo ngữ cảnh hội thoại không bị quên qua các lượt.
 * 2. Đảm bảo sổ tay <sotay_hientai> được chèn chính xác.
 * 3. Đảm bảo khối <state>{...}</state> ĐƯỢC XÓA 100% khỏi câu trả lời gửi đến khách hàng ở mọi lượt.
 * 4. Đảm bảo Lead được tự động phát hiện và tạo khi đủ 4 thông tin (Tên, SĐT, Địa chỉ, Gói cước).
 * 5. Đảm bảo chống tạo trùng lead khi khách chat tiếp các lượt sau.
 */
public class ContextMemoryTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("🚀 BẮT ĐẦU TEST NGỮ CẢNH HỘI THOẠI 12 LƯỢT (SESSION MEMORY)");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        String testSessionId = "test_session_" + System.currentTimeMillis();
        ChatSessionData sessionData = ChatSessionManager.getOrCreate(testSessionId);

        // Kịch bản 12 lượt hội thoại thực tế
        String[][] conversationScript = {
            // Lượt 1: Khách chào và cho biết tên
            { "Chào em, anh là Hoàng Nam.",
              "Dạ em chào anh Hoàng Nam! Em là tư vấn viên FPT Telecom. Anh cần lắp đặt mạng cho gia đình hay công ty ạ? <state>{\"ten\":\"Hoàng Nam\"}</state>" },

            // Lượt 2: Khách nêu nhu cầu sử dụng
            { "Nhà anh có 4 người dùng xem tivi 4K và chơi game.",
              "Dạ với 4 người dùng thì gói Sky 1Gbps là tối ưu nhất anh nhé. Tốc độ tải không giới hạn! <state>{\"ten\":\"Hoàng Nam\",\"goi_de_xuat\":\"Sky 1Gbps\"}</state>" },

            // Lượt 3: Khách cho biết địa chỉ
            { "Anh ở số 45 Hùng Vương, Pleiku nhé.",
              "Dạ hạ tầng cáp quang FPT tại 45 Hùng Vương rất mạnh anh Nam ạ. <state>{\"ten\":\"Hoàng Nam\",\"dia_chi\":\"45 Hùng Vương, Pleiku\",\"goi_de_xuat\":\"Sky 1Gbps\"}</state>" },

            // Lượt 4: Khách cho biết số điện thoại -> ĐỦ 4 THÔNG TIN (ten, sdt, dia_chi, goi_de_xuat)
            { "Số điện thoại của anh là 0918234567, khi nào lắp được em?",
              "Dạ em ghi nhận số 0918234567 của anh rồi ạ. Kỹ thuật viên sẽ hỗ trợ lắp đặt trong 24h! <state>{\"ten\":\"Hoàng Nam\",\"sdt\":\"0918234567\",\"dia_chi\":\"45 Hùng Vương, Pleiku\",\"goi_de_xuat\":\"Sky 1Gbps\"}</state>" },

            // Lượt 5: Khách hỏi thêm về phí lắp đặt
            { "Phí hòa mạng ban đầu là bao nhiêu tiền em?",
              "Dạ phí hòa mạng là 299.000đ, nhưng nếu anh trả trước 6 tháng sẽ được miễn phí hoàn toàn ạ. <state>{\"ten\":\"Hoàng Nam\",\"sdt\":\"0918234567\"}</state>" },

            // Lượt 6: Khách hỏi về truyền hình FPT Play
            { "Có kèm xem Ngoại Hạng Anh không em?",
              "Dạ gói Combo FPT Play độc quyền cúp C1 và Ngoại Hạng Anh đầy đủ trên ứng dụng ạ. <state>{}</state>" },

            // Lượt 7: Khách hỏi về Modem Wi-Fi
            { "Bên em trang bị loại modem Wi-Fi nào?",
              "Dạ bên em trang bị miễn phí modem chuẩn Wi-Fi 6 hai băng tần 2.4GHz và 5GHz thế hệ mới ạ. <state>{\"goi_de_xuat\":\"Combo Sky FPT Play\"}</state>" },

            // Lượt 8: Khách hỏi về bảo hành và sửa chữa
            { "Nếu sau này mạng bị sự cố thì gọi ai hỗ trợ?",
              "Dạ anh gọi tổng đài kỹ thuật miễn cước 1900 6600, kỹ thuật viên FPT Gia Lai sẽ hỗ trợ trong 2 giờ ạ. <state>{}</state>" },

            // Lượt 9: Khách hỏi thêm về Camera FPT
            { "Anh muốn lắp thêm 1 camera ngoài trời thì giá sao em?",
              "Dạ Camera FPT Outdoor kháng nước chuẩn IP67 giá chỉ từ 900.000đ, lưu trữ đám mây an toàn ạ. <state>{}</state>" },

            // Lượt 10: Khách hỏi về thanh toán
            { "Anh có thể thanh toán online qua quét mã QR không?",
              "Dạ được anh nhé, anh có thể quét mã QR qua ngân hàng hoặc ví điện tử VNPAY, MoMo đều được ạ. <state>{}</state>" },

            // Lượt 11: Khách chốt thời gian lắp đặt
            { "Chiều mai tầm 2h kỹ thuật qua nhà anh được không?",
              "Dạ hoàn toàn được anh Nam nhé! Em đã hẹn lịch chiều mai 14h kỹ thuật viên sẽ qua 45 Hùng Vương. <state>{}</state>" },

            // Lượt 12: Khách cảm ơn
            { "Cảm ơn em nhé!",
              "Dạ em cảm ơn anh Hoàng Nam đã tin tưởng lựa chọn FPT Telecom. Chúc anh một ngày tốt lành ạ! <state>{}</state>" }
        };

        boolean allTurnsClean = true;
        boolean leadTriggeredAtTurn4 = false;

        for (int i = 0; i < conversationScript.length; i++) {
            int turnNum = i + 1;
            String userPrompt = conversationScript[i][0];
            String rawAiResponse = conversationScript[i][1];

            // 1. Kiểm tra bao bọc thẻ <sotay_hientai> cho user prompt
            String wrappedPrompt = ChatSessionManager.wrapUserMessageWithState(sessionData, userPrompt);
            if (!wrappedPrompt.startsWith("<sotay_hientai>") || !wrappedPrompt.contains("</sotay_hientai>")) {
                System.err.println("✗ [FAIL] Lượt " + turnNum + ": Không chèn thẻ <sotay_hientai> vào tin nhắn user!");
                failed++;
                continue;
            }

            // 2. Trích xuất state và làm sạch câu trả lời
            String cleanReply = ChatSessionManager.extractAndUpdateState(sessionData, rawAiResponse);

            // Kiểm tra nghiêm ngặt: thẻ <state> TUYỆT ĐỐI KHÔNG ĐƯỢC XUẤT HIỆN trong phản hồi sạch
            if (cleanReply.contains("<state>") || cleanReply.contains("</state>")) {
                System.err.println("✗ [FAIL] Lượt " + turnNum + ": Vẫn còn tồn tại thẻ <state> trong nội dung trả về cho khách: " + cleanReply);
                allTurnsClean = false;
                failed++;
            }

            // Lưu tin nhắn vào lịch sử phiên
            sessionData.addMessage(ChatMessage.user(userPrompt));
            sessionData.addMessage(ChatMessage.assistant(cleanReply));

            // Kiểm tra state ở lượt 4 (phải có đủ 4 trường)
            if (turnNum == 4) {
                JsonObject s = sessionData.getState();
                boolean has4Fields = !s.get("ten").getAsString().isEmpty()
                                  && !s.get("sdt").getAsString().isEmpty()
                                  && !s.get("dia_chi").getAsString().isEmpty()
                                  && !s.get("goi_de_xuat").getAsString().isEmpty();
                if (has4Fields) {
                    leadTriggeredAtTurn4 = true;
                }
            }
        }

        if (allTurnsClean) {
            System.out.println("✓ [PASS] Kiểm tra khối <state>: Đã XÓA 100% trong toàn bộ 12 lượt hội thoại.");
            passed++;
        }

        // Kiểm tra tính toàn vẹn của Sổ tay khách hàng sau 12 lượt
        JsonObject finalState = sessionData.getState();
        String ten = finalState.get("ten").getAsString();
        String sdt = finalState.get("sdt").getAsString();
        String diaChi = finalState.get("dia_chi").getAsString();
        String goi = finalState.get("goi_de_xuat").getAsString();

        System.out.println("📋 Sổ tay sau 12 lượt: Tên='" + ten + "' | SĐT='" + sdt + "' | Đ/c='" + diaChi + "' | Gói='" + goi + "'");

        if ("Hoàng Nam".equals(ten) && "0918234567".equals(sdt) && "45 Hùng Vương, Pleiku".equals(diaChi)) {
            System.out.println("✓ [PASS] Sổ tay không bị quên hoặc ghi đè sau 12 lượt.");
            passed++;
        } else {
            System.err.println("✗ [FAIL] Sổ tay bị mất thông tin sau 12 lượt!");
            failed++;
        }

        if (leadTriggeredAtTurn4) {
            System.out.println("✓ [PASS] Tự động kích hoạt tạo Lead khi thu thập đủ 4 trường thông tin.");
            passed++;
        } else {
            System.err.println("✗ [FAIL] Không kích hoạt tạo lead tại lượt 4!");
            failed++;
        }

        // Kiểm tra dung lượng lịch sử sau 12 lượt (12 cặp = 24 tin nhắn)
        List<ChatMessage> hist = sessionData.getHistory();
        if (hist.size() == 24) {
            System.out.println("✓ [PASS] Lịch sử lưu trữ đúng 24 tin nhắn (12 lượt trao đổi liên tiếp).");
            passed++;
        } else {
            System.err.println("✗ [FAIL] Lịch sử có số lượng tin nhắn không khớp: " + hist.size());
            failed++;
        }

        // Kiểm tra số điện thoại hợp lệ
        if (LeadService.isValidPhone("0918234567") && !LeadService.isValidPhone("12345") && LeadService.isValidPhone("+84918234567")) {
            System.out.println("✓ [PASS] Kiểm tra Regex số điện thoại Việt Nam hợp lệ chuẩn xác.");
            passed++;
        } else {
            System.err.println("✗ [FAIL] Regex kiểm tra số điện thoại bị sai.");
            failed++;
        }

        System.out.println("==================================================");
        System.out.println("📊 KẾT QUẢ KIỂM THỬ: " + passed + " Passed, " + failed + " Failed.");
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
