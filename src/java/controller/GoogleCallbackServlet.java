package controller;

import config.OAuthConfig;
import dao.UserDAO;
import dto.UserDTO;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/google-callback")
public class GoogleCallbackServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code  = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");

        // ===== User huỷ trên Google =====
        if (error != null) {
            response.sendRedirect(request.getContextPath() + "/login?error=google_cancelled");
            return;
        }

        // ===== Kiểm tra state (CSRF) =====
        HttpSession session = request.getSession();
        String savedState = (String) session.getAttribute("oauth_state");
        if (savedState == null || !savedState.equals(state)) {
            response.sendRedirect(request.getContextPath() + "/login?error=invalid_state");
            return;
        }
        session.removeAttribute("oauth_state");

        try {
            // ===== 1. Đổi code → access_token =====
            JsonObject tokenResponse = exchangeCodeForToken(code);
            String accessToken = tokenResponse.get("access_token").getAsString();

            // ===== 2. Lấy user info từ Google =====
            JsonObject userInfo = getUserInfo(accessToken);
            String googleId = userInfo.get("sub").getAsString();
            String email    = userInfo.has("email") ? userInfo.get("email").getAsString() : null;
            String name     = userInfo.has("name") ? userInfo.get("name").getAsString() : email;
            String picture  = userInfo.has("picture") ? userInfo.get("picture").getAsString() : null;

            if (email == null || email.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/login?error=google_no_email");
                return;
            }

            email = email.trim().toLowerCase();

            // =========================================================
            // ⭐ 3. LOGIC CHẶN CHÉO
            // =========================================================

            // Bước 3.1: Tìm theo google_id
            UserDTO user = userDAO.findByGoogleId(googleId);

            if (user == null) {
                // Bước 3.2: Chưa có google_id → kiểm tra email
                UserDTO existing = userDAO.getByEmail(email);

                if (existing != null) {
                    // ⭐ EMAIL ĐÃ TỒN TẠI
                    // Kiểm tra: có phải tài khoản LOCAL (đăng ký thường có password) không?
                    boolean isLocalAccount = existing.isLocalUser()
                        || "local".equalsIgnoreCase(existing.getAuthProvider())
                        || (existing.getPassword() != null && !existing.getPassword().isEmpty());

                    if (isLocalAccount) {
                        // ❌ Tài khoản LOCAL → CHẶN, KHÔNG link Google
                        System.out.println("⛔ [OAuth] Email đã đăng ký bằng tài khoản thường: " + email);
                        response.sendRedirect(request.getContextPath()
                            + "/login?error=email_is_local");
                        return;
                    }

                    // ⭐ Tài khoản Google đã tồn tại nhưng chưa link google_id (hiếm)
                    // → link vào tài khoản đó
                    userDAO.linkGoogleAccount(existing.getId(), googleId, picture);
                    user = userDAO.getById(existing.getId());
                    System.out.println("✅ [OAuth] Link Google vào tài khoản cũ: " + email);
                } else {
                    // ⭐ Email CHƯA tồn tại → tạo user mới
                    UserDTO newUser = new UserDTO();
                    newUser.setUsername(generateUsername(email, name));
                    newUser.setEmail(email);
                    newUser.setFullName(name);
                    newUser.setAvatarUrl(picture);
                    newUser.setGoogleId(googleId);
                    newUser.setAuthProvider("google");
                    newUser.setEmailVerified(true);
                    newUser.setRole("customer");
                    newUser.setActive(true);

                    int newId = userDAO.insertGoogleUser(newUser);
                    if (newId > 0) {
                        user = userDAO.getById(newId);
                        System.out.println("✅ [OAuth] Tạo user Google mới: " + email);
                    }
                }
            } else {
                System.out.println("✅ [OAuth] User Google đã tồn tại: " + email);
            }

            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login?error=create_failed");
                return;
            }

            // Kiểm tra tài khoản có bị khóa không
            if (!user.isActive()) {
                response.sendRedirect(request.getContextPath() + "/login?error=account_locked");
                return;
            }

            // ===== 4. Đăng nhập =====
            session.setAttribute("user", user);
            session.setMaxInactiveInterval(30 * 60);

            System.out.println("==========================================");
            System.out.println("✅ [OAuth] Đăng nhập Google thành công");
            System.out.println("   - Email: " + email);
            System.out.println("   - Role: " + user.getRole());
            System.out.println("==========================================");

            // ===== 5. Redirect =====
            Object redirectUrl = session.getAttribute("redirectAfterLogin");
            if (redirectUrl != null) {
                session.removeAttribute("redirectAfterLogin");
                response.sendRedirect((String) redirectUrl);
            } else {
                response.sendRedirect(request.getContextPath() + "/dashboard");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/login?error=oauth_failed");
        }
    }

    // =========================================================
    // Đổi code → access_token
    // =========================================================
    private JsonObject exchangeCodeForToken(String code) throws IOException {
        String url = "https://oauth2.googleapis.com/token";
        String params = "code=" + URLEncoder.encode(code, "UTF-8")
                + "&client_id=" + URLEncoder.encode(OAuthConfig.getGoogleClientId(), "UTF-8")
                + "&client_secret=" + URLEncoder.encode(OAuthConfig.getGoogleClientSecret(), "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(OAuthConfig.getGoogleRedirectUri(), "UTF-8")
                + "&grant_type=authorization_code";
        return postJson(url, params);
    }

    // =========================================================
    // Lấy user info từ Google
    // =========================================================
    private JsonObject getUserInfo(String accessToken) throws IOException {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token="
                + URLEncoder.encode(accessToken, "UTF-8");
        return getJson(url);
    }

    // =========================================================
    // HTTP helpers
    // =========================================================
    private JsonObject postJson(String urlStr, String postData) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }
        return new Gson().fromJson(readResponse(conn), JsonObject.class);
    }

    private JsonObject getJson(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return new Gson().fromJson(readResponse(conn), JsonObject.class);
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() >= 400
                ? conn.getErrorStream() : conn.getInputStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // =========================================================
    // Sinh username duy nhất từ email
    // =========================================================
    private String generateUsername(String email, String name) {
        if (email != null && email.contains("@")) {
            String base = email.substring(0, email.indexOf("@"));
            String uname = base;
            int i = 1;
            while (userDAO.getByUsername(uname) != null) {
                uname = base + i++;
            }
            return uname;
        }
        return "user_" + System.currentTimeMillis();
    }
}