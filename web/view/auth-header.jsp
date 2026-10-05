<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!-- =========================================================
     THÀNH PHẦN HEADER: NÚT QUAY LẠI TRANG CHỦ & LOGO FPT TELECOM
     Dùng chung cho các trang xác thực
     ========================================================= -->
<!-- Nút Quay lại trang chủ (Góc trên bên trái) -->
<div class="back-home-wrap">
    <a href="${pageContext.request.contextPath}/home" class="btn-back-glass" title="Trở về trang chủ FPT Telecom">
        <svg viewBox="0 0 24 24"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
        Quay lại trang chủ
    </a>
</div>

<!-- Logo FPT Telecom (Căn giữa phía trên) -->
<a href="${pageContext.request.contextPath}/home" class="login-logo" title="FPT Telecom">
    <img src="${pageContext.request.contextPath}/assets/images/fpt-logo.jpg" 
         alt="FPT Telecom" 
         class="login-logo-img">
</a>

