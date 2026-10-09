# HƯỚNG DẪN KIẾN TRÚC & MỞ RỘNG AI MULTI-PROVIDER

Hệ thống FPT Sale Manager đã được nâng cấp thành công lên kiến trúc **Đa Nhà Cung Cấp (Multi-Provider Architecture)** theo mô hình **Adapter Pattern**.

---

## 1. CÁC NHÀ CUNG CẤP ĐÃ ĐƯỢC TÍCH HỢP SẴN

1. **Google Gemini:** Kết nối trực tiếp qua Google Generative Language API v1beta (`gemini-1.5-flash`, `gemini-2.0-flash`, `gemini-1.5-flash-8b`, `gemini-1.5-pro`...).
2. **OpenAI:** Kết nối OpenAI API chuẩn (`gpt-4o-mini`, `gpt-4o`, `gpt-4-turbo`...).
3. **DeepSeek:** Kết nối DeepSeek API v1 (`deepseek-chat` / V3, `deepseek-reasoner` / R1).
4. **Anthropic Claude:** Kết nối Anthropic Messages API (`claude-3-5-haiku`, `claude-3-5-sonnet`, `claude-3-opus`).
5. **Groq:** Kết nối Groq LPU siêu tốc (>700 tokens/s) (`llama-3.3-70b-versatile`, `llama-3.1-8b-instant`).
6. **OpenRouter:** Cổng kết nối trung gian đa mô hình (`deepseek/deepseek-chat`, `google/gemini-2.0-flash-001`...).
7. **Custom OpenAI-compatible / Local Ollama:** Cho phép Admin tự nhập Base URL tùy ý để kết nối với Ollama (`http://localhost:11434/v1`), LM Studio, Together AI, Mistral, xAI...

---

## 2. BẢO MẬT & AN TOÀN THÔNG TIN (SECURITY LAYER)

* **Mã hóa AES-256-GCM:** Toàn bộ API Key được mã hóa tự động trước khi ghi vào CSDL PostgreSQL/Supabase. Khóa mã hóa 256-bit được quản lý an toàn.
* **Masking trên Giao diện:** Giao diện chỉ nhận chuỗi đã che mờ (ví dụ `sk-••••••••a1b2`). Trình duyệt không bao giờ thấy API Key nguyên bản. Nếu admin không sửa ô key, hệ thống tự động giữ nguyên key cũ.
* **Chống SSRF (Server-Side Request Forgery):** Module `SSRFValidator` tự động phân giải DNS và chặn mọi yêu cầu Base URL trỏ vào loopback (`127.0.0.1`), mạng nội bộ LAN (`10.x`, `172.16.x`, `192.168.x`) và Cloud Metadata IP (`169.254.169.254`).
* **Output Guardrails:** Lọc bỏ tự động trường hợp AI vô tình in chuỗi API Key hoặc cố tình rò rỉ System Instruction cho khách hàng.
* **Tự động Fallback:** Khi model chính bị lỗi (hết quota 429, nghẽn tải 503, timeout), hệ thống tự động chuyển tiếp request sang Model dự phòng đã cấu hình mà không làm gián đoạn khách hàng.

---

## 3. CÁCH THÊM MỘT NHÀ CUNG CẤP (PROVIDER) MỚI

Nhờ áp dụng **Provider Adapter Pattern**, để thêm một hãng AI mới sau này, bạn chỉ cần thực hiện 2 bước đơn giản:

### Bước 1: Tạo lớp Provider kế thừa `LLMProvider`
Ví dụ tạo `src/java/ai/provider/MyNewAIProvider.java`:

```java
package ai.provider;

import ai.LLMProvider;
import ai.dto.*;
import ai.exception.AIException;
import java.util.List;

public class MyNewAIProvider implements LLMProvider {
    @Override
    public String getId() { return "mynewai"; }

    @Override
    public String getDisplayName() { return "My New AI Service"; }

    @Override
    public String getDefaultBaseUrl() { return "https://api.mynewai.com/v1"; }

    @Override
    public List<ModelInfo> getPresetModels() {
        // Trả về danh sách model gợi ý
    }

    @Override
    public List<ModelInfo> listModels(String apiKey, String baseUrl) throws AIException {
        // Gọi API của hãng lấy danh sách model thật
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, String apiKey, String model,
                             String baseUrl, ChatOptions options) throws AIException {
        // Thực hiện request đến hãng và trả về ChatResponse chuẩn
    }

    @Override
    public void chatStream(List<ChatMessage> messages, String apiKey, String model,
                           String baseUrl, ChatOptions options, StreamCallback callback) {
        // Xử lý Server-Sent Events (SSE)
    }

    @Override
    public ChatResponse testConnection(String apiKey, String model, String baseUrl) throws AIException {
        // Ping test nhanh
    }
}
```

*Lưu ý:* Nếu hãng mới tuân theo chuẩn OpenAI API (như Mistral, Perplexity, xAI), bạn thậm chí **không cần viết code mới**, chỉ cần đăng ký ngay với `OpenAICompatibleProvider`.

### Bước 2: Đăng ký vào `ProviderRegistry.java`
Mở `src/java/ai/ProviderRegistry.java` và thêm 1 dòng:

```java
register(new MyNewAIProvider());
```

Hệ thống sẽ tự động nhận diện hãng mới trên toàn bộ trang Quản trị, Playground, và Chatbot khách hàng!

---

## 4. CHECKLIST KIỂM THỬ THỦ CÔNG TRÊN GIAO DIỆN ADMIN

Khi truy cập vào đường dẫn `/admin/settings`:

1. **Kiểm tra chọn Provider:**
   - Click chọn các card: *Google Gemini*, *OpenAI*, *DeepSeek*, *Claude*, *Custom*.
   - Quan sát ô API Key tự động cập nhật nhãn tương ứng và hiển thị dạng masked `••••••••`.
2. **Kiểm tra Base URL cho Custom:**
   - Khi chọn *Custom OpenAI / Ollama*, ô Base URL xuất hiện. Thử nhập URL như `http://localhost:11434/v1` (cho Ollama) hoặc để trống.
3. **Kiểm tra Đồng bộ từ API Key:**
   - Nhập API Key thật của hãng -> Bấm nút **"Đồng bộ từ API Key"**.
   - Hệ thống tự động nạp danh sách model thật vào dropdown và bật thông báo số lượng model.
4. **Kiểm tra Test Connection:**
   - Bấm nút **"Kiểm tra kết nối (Test Connection)"**.
   - Quan sát kết quả báo thành công kèm độ trễ: `✓ Kết nối thành công (Độ trễ: 185ms)!`.
5. **Kiểm tra Live Playground:**
   - Gõ câu hỏi: *"Gói Sky 1Gbps giá bao nhiêu em?"* -> Bấm **Gửi câu hỏi**.
   - Câu trả lời hiện ra kèm tag: `🏷️ provider / model`, `⚡ Độ trễ ms`, `🪙 Token usage`.
6. **Kiểm tra Lưu Cấu Hình:**
   - Bấm **"LƯU CẤU HÌNH AI"**.
   - Ra trang chủ (`/home`), mở khung chat AI của khách, gửi câu hỏi để kiểm tra câu trả lời mượt mà từ model mới.
