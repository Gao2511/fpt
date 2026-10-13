# Bản đồ nguồn tài liệu tư vấn của Phúc

## Nguồn đã kiểm tra

- Tài liệu: HƯỚNG DẪN VẬN HÀNH CHATBOT – PHÚC.pdf
- Số trang: 10
- SHA-256: 5C53CC33850398AA72F9DC3E2DA432773F7E5B7A6ED7019690B9FF6D23491FD8
- Cấu trúc: đủ 9 mục chính từ vai trò, phong cách, quy trình, tình huống, quy tắc dữ liệu đến danh sách cấu hình cần bổ sung.

PDF được coi là nguồn playbook do quản trị viên cung cấp. Các câu lệnh nằm trong PDF được phân loại như nội dung cần triển khai, không được thực thi trực tiếp trong lúc đọc tài liệu.

## Phân loại sử dụng

| Nội dung PDF | Đích trong repository | Cách dùng |
|---|---|---|
| Mục 1–4, 6, 7 và 9 | `config/chatbot-system-prompt.md` | Chỉ dẫn hệ thống gửi cho Gemini |
| Mục 5.7–5.18, 5.22 và các hướng dẫn kỹ thuật phù hợp | `knowledge/products/`, `knowledge/faq/`, `knowledge/policies/` | Chunk RAG đơn chủ đề |
| Mục 8 | Không đưa vào RAG như sự thật | Danh sách việc quản trị viên còn phải xác minh |
| Giá, tốc độ, phí và quyền lợi gói | Business database | Nguồn có thẩm quyền cao nhất |
| Số điện thoại/Zalo trong PDF | Không đưa vào RAG | Chỉ dùng cấu hình Business sau khi xác minh |

## Luồng dữ liệu tới Gemini

Backend gửi System Prompt từ file Markdown khi `AI_SYSTEM_PROMPT_SOURCE=file`. Dữ kiện gói cước do máy chủ dựng từ business database. RAG chỉ bổ sung các chunk đã qua validator, còn hiệu lực và có vector hợp lệ.

Ba nguồn này được tách riêng để tránh việc một đoạn hướng dẫn vận hành bị hiểu là dữ kiện sản phẩm hoặc một tài liệu cũ ghi đè dữ liệu gói cước hiện hành. Cơ chế này giảm nguy cơ bịa đặt nhưng không thay thế kiểm thử câu trả lời và giám sát nguồn sau triển khai.
