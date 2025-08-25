# --- Build Stage ---
FROM maven:3.9.4-eclipse-temurin-21 AS build

WORKDIR /build

COPY ../../pom.xml ./
COPY ../../src ./src

RUN mvn clean package -DskipTests

# --- Runtime Stage ---
FROM eclipse-temurin:21-jdk

LABEL org.opencontainers.image.title="SEDIMARK Catalogue Server"
LABEL org.opencontainers.image.description="A customized Jena Fuseki server for SEDIMARK offerings"
LABEL org.opencontainers.image.vendor="SEDIMARK"
LABEL org.opencontainers.image.licenses="EUPL-1.2"

WORKDIR /app

COPY --from=build /build/target/catalogue-1.0.jar /app/
COPY --from=build /build/target/dependency /app/lib

RUN mkdir -p /data/sedimark-tdb

ENV SERVER_PORT=3030
ENV STORAGE_TYPE=memory
ENV TDB_PATH=/data/sedimark-tdb
ENV LOAD_EXAMPLES=false
ENV DEBUG=false

EXPOSE ${SERVER_PORT}

VOLUME /data

RUN echo '#!/bin/bash \n\
JAVA_OPTS="-Xmx1g -Xms512m" \n\
CMD_OPTS="" \n\
if [ "$STORAGE_TYPE" = "tdb" ]; then \n\
    CMD_OPTS="$CMD_OPTS --tdb $TDB_PATH" \n\
elif [ "$STORAGE_TYPE" = "memory" ]; then \n\
    CMD_OPTS="$CMD_OPTS --memory" \n\
fi \n\
if [ "$LOAD_EXAMPLES" = "true" ]; then \n\
    CMD_OPTS="$CMD_OPTS --load-examples" \n\
fi \n\
if [ "$DEBUG" = "true" ]; then \n\
    CMD_OPTS="$CMD_OPTS --debug" \n\
fi \n\
CMD_OPTS="$CMD_OPTS --port $SERVER_PORT" \n\
exec java $JAVA_OPTS -cp "/app/catalogue-1.0.jar:/app/lib/*" eu.sedimark.catalogue.CatalogueServerLauncher $CMD_OPTS $@' > /app/docker-entrypoint.sh \
    && chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]