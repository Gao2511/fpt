package controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * MainController - Front Controller chính điều hướng toàn bộ action của hệ thống FPT Sale.
 * Mọi request kèm theo tham số 'action' sẽ được điều phối tới Servlet hoặc Resource tương ứng.
 * 
 * URL Patterns: /MainController, /main
 */
@WebServlet(name = "MainController", urlPatterns = {"/MainController", "/main"})
public class MainController extends HttpServlet {

    // =========================================================================
    // CÁC HẰNG SỐ ĐƯỜNG DẪN / ACTION CONTROLLER
    // =========================================================================
    private static final String ERROR = "home";
    private static final String HOME = "home";

    // 1. Xác thực & Tài khoản
    private static final String LOGIN = "login";
    private static final String LOGOUT = "logout";
    private static final String REGISTER = "register";
    private static final String FORGOT_PASSWORD = "forgot-password";
    private static final String RESET_PASSWORD = "reset-password";
    private static final String CHANGE_PASSWORD = "change-password";
    private static final String PROFILE = "profile";
    private static final String GOOGLE_LOGIN = "google-login";
    private static final String GOOGLE_CALLBACK = "google-callback";

    // 2. Chức năng Người dùng / Khách hàng
    private static final String CONTACT_PAGE = "contact";
    private static final String CONTACT_SUBMIT = "ContactServlet";
    private static final String PACKAGE_DETAIL = "package-detail";
    private static final String MY_ORDERS = "my-orders";
    private static final String AI_CHAT = "ai-chat";

    // 3. Quản trị viên (Admin)
    private static final String DASHBOARD = "admin/dashboard";
    private static final String ADMIN_CUSTOMERS = "admin/customers";
    private static final String ADMIN_CUSTOMER_DETAIL = "admin/customer-detail";
    private static final String ADMIN_PACKAGES = "admin/packages";
    private static final String ADMIN_USERS = "admin/users";
    private static final String ADMIN_SETTINGS = "admin/settings";
    private static final String ADMIN_EMAIL_LOGS = "admin/email-logs";

    /**
     * Phương thức xử lý chung cho cả GET và POST
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        String url = HOME;
        try {
            String action = request.getParameter("action");

            if (action == null || action.trim().isEmpty()) {
                url = HOME;
            } else {
                switch (action) {
                    // ==========================================
                    // 1. NHÓM ACTION AUTHENTICATION & TÀI KHOẢN
                    // ==========================================
                    case "Login":
                    case "login":
                    case "SignIn":
                        url = LOGIN;
                        break;

                    case "Logout":
                    case "logout":
                    case "SignOut":
                        url = LOGOUT;
                        break;

                    case "Register":
                    case "register":
                    case "SignUp":
                        url = REGISTER;
                        break;

                    case "ForgotPassword":
                    case "forgot-password":
                    case "forgotPassword":
                        url = FORGOT_PASSWORD;
                        break;

                    case "ResetPassword":
                    case "reset-password":
                    case "resetPassword":
                        url = RESET_PASSWORD;
                        break;

                    case "ChangePassword":
                    case "change-password":
                    case "changePassword":
                        url = CHANGE_PASSWORD;
                        break;

                    case "Profile":
                    case "profile":
                        url = PROFILE;
                        break;

                    case "GoogleLogin":
                    case "google-login":
                        url = GOOGLE_LOGIN;
                        break;

                    case "GoogleCallback":
                    case "google-callback":
                        url = GOOGLE_CALLBACK;
                        break;

                    // ==========================================
                    // 2. NHÓM ACTION NGƯỜI DÙNG & TƯ VẤN
                    // ==========================================
                    case "Home":
                    case "home":
                        url = HOME;
                        break;

                    case "Contact":
                    case "contact":
                    case "ContactPage":
                        url = CONTACT_PAGE;
                        break;

                    case "SendContact":
                    case "send-contact":
                    case "SubmitContact":
                    case "submit-contact":
                    case "ContactServlet":
                        url = CONTACT_SUBMIT;
                        break;

                    case "PackageDetail":
                    case "package-detail":
                    case "ViewPackage":
                        url = PACKAGE_DETAIL;
                        break;

                    case "MyOrders":
                    case "my-orders":
                    case "orders":
                        url = MY_ORDERS;
                        break;

                    case "AIChat":
                    case "ai-chat":
                    case "chat":
                        url = AI_CHAT;
                        break;

                    // ==========================================
                    // 3. NHÓM ACTION QUẢN TRỊ ADMIN
                    // ==========================================
                    case "Dashboard":
                    case "dashboard":
                    case "AdminDashboard":
                    case "admin-dashboard":
                        url = DASHBOARD;
                        break;

                    case "AdminCustomers":
                    case "admin-customers":
                    case "ManageCustomers":
                    case "CustomerList":
                        url = ADMIN_CUSTOMERS;
                        break;

                    case "CustomerDetail":
                    case "customer-detail":
                    case "AdminCustomerDetail":
                        url = ADMIN_CUSTOMER_DETAIL;
                        break;

                    case "AdminPackages":
                    case "admin-packages":
                    case "ManagePackages":
                    case "PackageList":
                        url = ADMIN_PACKAGES;
                        break;

                    case "AdminUsers":
                    case "admin-users":
                    case "ManageUsers":
                    case "UserList":
                        url = ADMIN_USERS;
                        break;

                    case "AdminSettings":
                    case "admin-settings":
                    case "ManageSettings":
                        url = ADMIN_SETTINGS;
                        break;

                    case "AdminEmailLogs":
                    case "admin-email-logs":
                    case "ManageEmailLogs":
                    case "EmailLogs":
                        url = ADMIN_EMAIL_LOGS;
                        break;

                    default:
                        // Action không xác định -> điều hướng về trang chủ
                        log("[MainController] Unknown action: " + action + ", redirecting to HOME.");
                        url = HOME;
                        break;
                }
            }
        } catch (Exception e) {
            log("[MainController] Error processing request: " + e.getMessage(), e);
            url = ERROR;
        } finally {
            request.getRequestDispatcher(url).forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "MainController - Front Controller for FPT Sale Website";
    }
}