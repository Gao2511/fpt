FROM tomcat:9.0-jdk11

RUN rm -rf /usr/local/tomcat/webapps/*

COPY dist/fpt-sale.war /usr/local/tomcat/webapps/fpt-sale.war

EXPOSE 8080

CMD ["catalina.sh", "run"]