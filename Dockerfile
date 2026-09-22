# ============================================================
# RUN — Dùng Tomcat 9 để chạy file WAR có sẵn
# ============================================================
FROM tomcat:9-jre17-temurin

# Xóa webapps mặc định
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy file WAR
COPY dist/fpt-sale.war /usr/local/tomcat/webapps/ROOT.war

# ⭐ Cấu hình Tomcat: bỏ qua một số warning, tăng timeout
RUN echo 'org.apache.catalina.loader.WebappClassLoaderBase.clearReferencesThreadLocals=false' \
    >> /usr/local/tomcat/conf/catalina.properties

# Tắt cảnh báo memory leak
RUN echo 'org.apache.catalina.startup.ContextConfig.jarsToSkip=*.jar' \
    >> /usr/local/tomcat/conf/catalina.properties

# Mở port
EXPOSE 8080

# ⭐ QUAN TRỌNG: Render cần biết port động
ENV PORT=8080
ENV CATALINA_OPTS="-Dserver.port=${PORT}"

# Chạy Tomcat
CMD ["catalina.sh", "run"]