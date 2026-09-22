# ============================================================
# STAGE 1: BUILD — Dùng JDK + Ant để build file WAR
# ============================================================
FROM eclipse-temurin:17-jdk AS builder

# Cài Ant
RUN apt-get update && \
    apt-get install -y ant && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy toàn bộ source code vào
COPY . .

# Build file WAR bằng Ant
# NetBeans Ant mặc định build ra dist/fpt-sale.war
RUN ant clean dist

# ============================================================
# STAGE 2: RUN — Dùng Tomcat 9 để chạy file WAR
# ============================================================
FROM tomcat:9-jre17-temurin

# Xóa webapps mặc định của Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy file WAR từ stage build sang Tomcat
COPY --from=builder /app/dist/fpt-sale.war /usr/local/tomcat/webapps/ROOT.war

# Cấu hình port (Render thường dùng 10000, nhưng Tomcat mặc định 8080)
# Render sẽ tự detect port từ biến môi trường PORT
ENV PORT=8080

EXPOSE 8080

# Chạy Tomcat
CMD ["catalina.sh", "run"]