<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!-- =========================================================
     THÀNH PHẦN NỀN: HIỆU ỨNG GRADIENT BLOBS ĐỘNG (DARK GLASS)
     Dùng chung cho toàn bộ các trang: Login, Register, Forgot, Reset
     ========================================================= -->
<div class="login-bg">
    <svg viewBox="0 0 800 600" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid slice">
        <defs>
            <!-- Blob 1: Gradient Cam FPT -->
            <radialGradient id="blob1" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#f37021" stop-opacity="0.55"/>
                <stop offset="50%" stop-color="#f37021" stop-opacity="0.25"/>
                <stop offset="100%" stop-color="#f37021" stop-opacity="0"/>
            </radialGradient>
            <!-- Blob 2: Gradient Tím Neon -->
            <radialGradient id="blob2" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#a855f7" stop-opacity="0.45"/>
                <stop offset="50%" stop-color="#a855f7" stop-opacity="0.20"/>
                <stop offset="100%" stop-color="#a855f7" stop-opacity="0"/>
            </radialGradient>
            <!-- Blob 3: Gradient Hồng -->
            <radialGradient id="blob3" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#f472b6" stop-opacity="0.40"/>
                <stop offset="50%" stop-color="#f472b6" stop-opacity="0.18"/>
                <stop offset="100%" stop-color="#f472b6" stop-opacity="0"/>
            </radialGradient>
            <!-- Blob 4: Gradient Đỏ Rực -->
            <radialGradient id="blob4" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#ef4444" stop-opacity="0.35"/>
                <stop offset="50%" stop-color="#ef4444" stop-opacity="0.15"/>
                <stop offset="100%" stop-color="#ef4444" stop-opacity="0"/>
            </radialGradient>
            <!-- Filter Gaussian Blur -->
            <filter id="soft1" x="-50%" y="-50%" width="200%" height="200%">
                <feGaussianBlur stdDeviation="20"/>
            </filter>
            <filter id="soft2" x="-50%" y="-50%" width="200%" height="200%">
                <feGaussianBlur stdDeviation="25"/>
            </filter>
        </defs>
        <!-- Nền tối chính -->
        <rect width="800" height="600" fill="#0a0f1e"/>
        <!-- Nhóm vòng tròn động 1 -->
        <g class="bg-group-1" filter="url(#soft1)">
            <circle cx="180" cy="520" r="240" fill="url(#blob1)"/>
            <circle cx="680" cy="150" r="200" fill="url(#blob3)"/>
        </g>
        <!-- Nhóm vòng tròn động 2 -->
        <g class="bg-group-2" filter="url(#soft2)">
            <circle cx="700" cy="480" r="220" fill="url(#blob2)"/>
            <circle cx="120" cy="120" r="180" fill="url(#blob4)"/>
        </g>
    </svg>
</div>
