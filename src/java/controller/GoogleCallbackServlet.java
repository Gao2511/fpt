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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");

        if (error != null) {
            response.sendRedirect(request.getContextPath() + "/login?error=cancelled");
            return;
        }

        HttpSession session = request.getSession();
        String savedState = (String) session.getAttribute("oauth_state");
        if (savedState == null || !savedState.equals(state)) {
            response.sendRedirect(request.getContextPath() + "/login?error=invalid_state");
            return;
        }
        session.removeAttribute("oauth_state");

        try {
            JsonObject tokenResponse = exchangeCodeForToken(code);
            String accessToken = tokenResponse.get("access_token").getAsString();

            JsonObject userInfo = getUserInfo(accessToken);
            String googleId = userInfo.get("sub").getAsString();
            String email = userInfo.has("email") ? userInfo.get("email").getAsString() : null;
            String name = userInfo.has("name") ? userInfo.get("name").getAsString() : email;
            String picture = userInfo.has("picture") ? userInfo.get("picture").getAsString() : null;

            UserDAO userDAO = new UserDAO();
            UserDTO user = userDAO.findByGoogleId(googleId);

            if (user == null) {
                if (email != null) user = userDAO.getByEmail(email);

                if (user != null) {
                    userDAO.linkGoogleAccount(user.getId(), googleId, picture);
                    user = userDAO.getById(user.getId());
                } else {
                    UserDTO newUser = new UserDTO();
                    newUser.setUsername(generateUsername(email, name));
                    newUser.setEmail(email);
                    newUser.setFullName(name);
                    newUser.setAvatarUrl(picture);
                    newUser.setGoogleId(googleId);
                    newUser.setRole("customer");
                    newUser.setActive(true);

                    int newId = userDAO.insertGoogleUser(newUser);
                    if (newId > 0) user = userDAO.getById(newId);
                }
            }

            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login?error=create_failed");
                return;
            }

            session.setAttribute("user", user);
            session.setMaxInactiveInterval(30 * 60);

            response.sendRedirect(request.getContextPath() + "/home");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/login?error=oauth_failed");
        }
    }

    private JsonObject exchangeCodeForToken(String code) throws IOException {
        String url = "https://oauth2.googleapis.com/token";
        String params = "code=" + URLEncoder.encode(code, "UTF-8")
                + "&client_id=" + URLEncoder.encode(OAuthConfig.getGoogleClientId(), "UTF-8")
                + "&client_secret=" + URLEncoder.encode(OAuthConfig.getGoogleClientSecret(), "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(OAuthConfig.getGoogleRedirectUri(), "UTF-8")
                + "&grant_type=authorization_code";
        return postJson(url, params);
    }

    private JsonObject getUserInfo(String accessToken) throws IOException {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token="
                + URLEncoder.encode(accessToken, "UTF-8");
        return getJson(url);
    }

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

    private String generateUsername(String email, String name) {
        if (email != null && email.contains("@")) {
            String base = email.substring(0, email.indexOf("@"));
            UserDAO dao = new UserDAO();
            String uname = base;
            int i = 1;
            while (dao.getByUsername(uname) != null) {
                uname = base + i++;
            }
            return uname;
        }
        return "user_" + System.currentTimeMillis();
    }
}