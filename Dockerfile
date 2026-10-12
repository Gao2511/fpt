# Reproducible multi-stage source build for Render (JDK 17 compiling --release 8 + Tomcat 9)
FROM eclipse-temurin:17-jdk AS build
RUN apt-get update && apt-get install -y --no-install-recommends ant curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY tools/dependencies.lock tools/dependencies.lock
COPY tools/bootstrap-libs.sh tools/bootstrap-libs.sh
RUN sed -i 's/\r$//' tools/bootstrap-libs.sh tools/dependencies.lock && sh tools/bootstrap-libs.sh
COPY src src
COPY web web
COPY config config
COPY build-portable.xml build-portable.xml
ARG RENDER_GIT_COMMIT=source-build
ARG BUILD_VERSION=5.3.0-online-rag
RUN ant -f build-portable.xml -Dbuild.commit=${RENDER_GIT_COMMIT} -Dbuild.version=${BUILD_VERSION} war

FROM tomcat:9-jre17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/.build/dist/fpt-sale-rag.war /usr/local/tomcat/webapps/ROOT.war

RUN echo 'org.apache.catalina.loader.WebappClassLoaderBase.clearReferencesThreadLocals=false' >> /usr/local/tomcat/conf/catalina.properties

RUN echo 'org.apache.catalina.startup.ContextConfig.jarsToSkip=*.jar' >> /usr/local/tomcat/conf/catalina.properties

EXPOSE 8080

ENV PORT=8080
ENV RAG_ENVIRONMENT=PRODUCTION RAG_ENABLED=false RAG_RUNTIME_MODE=OFFLINE_MOCK RAG_EMBEDDING_CALLS_APPROVED=false RAG_LOCAL_GEMINI_AUTHORIZED=false RAG_ONLINE_GEMINI_AUTHORIZED=false
ENV CATALINA_OPTS="-Dserver.port=${PORT}"

CMD ["catalina.sh", "run"]

