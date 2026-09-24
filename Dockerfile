FROM tomcat:9-jre17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY dist/fpt-sale.war /usr/local/tomcat/webapps/ROOT.war

RUN echo 'org.apache.catalina.loader.WebappClassLoaderBase.clearReferencesThreadLocals=false' >> /usr/local/tomcat/conf/catalina.properties

RUN echo 'org.apache.catalina.startup.ContextConfig.jarsToSkip=*.jar' >> /usr/local/tomcat/conf/catalina.properties

EXPOSE 8080

ENV PORT=8080
ENV CATALINA_OPTS="-Dserver.port=${PORT}"

CMD ["catalina.sh", "run"]