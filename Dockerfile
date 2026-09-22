# ============================================================
# RUN — Dùng Tomcat 9 để chạy file WAR có sẵn
# ============================================================
FROM tomcat:9-jre17-temurin

# Xóa webapps mặc định
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy file WAR đã build sẵn vào Tomcat
COPY dist/fpt-sale.war /usr/local/tomcat/webapps/ROOT.war

# Render tự set PORT
EXPOSE 8080

# Chạy Tomcat
CMD ["catalina.sh", "run"]