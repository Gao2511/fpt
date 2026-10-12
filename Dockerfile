FROM tomcat:9-jre17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY dist/fpt-sale.war /usr/local/tomcat/webapps/ROOT.war

RUN echo 'org.apache.catalina.loader.WebappClassLoaderBase.clearReferencesThreadLocals=false' >> /usr/local/tomcat/conf/catalina.properties

RUN echo 'org.apache.catalina.startup.ContextConfig.jarsToSkip=*.jar' >> /usr/local/tomcat/conf/catalina.properties

EXPOSE 8080

ENV PORT=8080
ENV RAG_ENABLED=false RAG_RUNTIME_MODE=OFFLINE_MOCK RAG_EMBEDDING_CALLS_APPROVED=false RAG_LOCAL_GEMINI_AUTHORIZED=false
ENV CATALINA_OPTS="-Dserver.port=${PORT}"

CMD ["catalina.sh", "run"]
