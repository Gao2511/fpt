<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!-- =========================================================
     THÀNH PHẦN MODAL: THÔNG BÁO TIẾN TRÌNH / LỖI / THÀNH CÔNG
     Dùng chung cho các trang xác thực
     ========================================================= -->
<div id="loginModal" class="login-modal-overlay" role="dialog" aria-modal="true">
    <div class="login-modal">
        <!-- Nút Đóng modal -->
        <button type="button" id="modalClose" class="login-modal-close hidden" aria-label="Đóng thông báo">
            <svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
        <!-- Icon trạng thái (Spinner loading / Dấu X đỏ / Dấu V xanh) -->
        <div id="modalIcon" class="login-modal-icon"></div>
        <!-- Nội dung thông báo -->
        <div id="modalText" class="login-modal-text"></div>
    </div>
</div>
