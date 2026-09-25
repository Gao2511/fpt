package controller;

import config.OAuthConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/google-login")
public class GoogleOAuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String clientId = OAuthConfig.getGoogleClientId();
        String redirectUri = OAuthConfig.getGoogleRedirectUri();

        if (clientId == null || clientId.isEmpty() || clientId.startsWith("xxxxx")) {
            response.sendError(500, "Chưa cấu hình Google Client ID trong oauth.properties");
            return;
        }

        String state = UUID.randomUUID().toString();
        request.getSession().setAttribute("oauth_state", state);

        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + URLEncoder.encode(clientId, "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8")
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("openid email profile", "UTF-8")
                + "&state=" + state
                + "&access_type=offline"
                + "&prompt=select_account";

        response.sendRedirect(googleAuthUrl);
    }
}