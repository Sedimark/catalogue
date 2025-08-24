
# --- Build Stage ---
FROM maven:3.9.4-eclipse-temurin-21 AS build

WORKDIR /build

# Copy Maven project files
COPY ../../pom.xml ./
COPY ../../src ./src

# Build the JAR file (skip tests for speed, remove if you want tests)
RUN mvn clean package -DskipTests \
    && mvn dependency:copy-dependencies -DoutputDirectory=target/dependency

# --- Runtime Stage ---
FROM eclipse-temurin:21-jdk

# Labels for image metadata
LABEL org.opencontainers.image.title="SEDIMARK Catalogue Server"
LABEL org.opencontainers.image.description="A customized Jena Fuseki server for SEDIMARK offerings"
LABEL org.opencontainers.image.vendor="SEDIMARK"
LABEL org.opencontainers.image.licenses="EUPL-1.2"

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage
# COPY --from=build /build/target/catalogue-1.0.jar /app/sedimark-catalogue-server.jar
COPY --from=build /build/target/catalogue-1.0.jar /app/
COPY --from=build /build/target/dependency /app/lib

# Create a directory for TDB2 storage
RUN mkdir -p /data/sedimark-tdb

# Set environment variables with defaults (can be overridden at runtime)
ENV SERVER_PORT=3030
ENV STORAGE_TYPE=memory
ENV TDB_PATH=/data/sedimark-tdb
ENV LOAD_EXAMPLES=false
ENV DEBUG=false

# Expose the server port
EXPOSE ${SERVER_PORT}

# Add a healthcheck
#HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
#    CMD curl -f http://localhost:${SERVER_PORT}/catalogue/test || exit 1

# Create a volume for persistent data
VOLUME /data

# Create an entrypoint script that uses the environment variables
RUN echo '#!/bin/bash \n\
JAVA_OPTS="-Xmx1g -Xms512m" \n\
CMD_OPTS="" \n\
\n\
# Set up storage type \n\
if [ "$STORAGE_TYPE" = "tdb" ]; then \n\
    CMD_OPTS="$CMD_OPTS --tdb $TDB_PATH" \n\
elif [ "$STORAGE_TYPE" = "memory" ]; then \n\
    CMD_OPTS="$CMD_OPTS --memory" \n\
fi \n\
\n\
# Add other options \n\
if [ "$LOAD_EXAMPLES" = "true" ]; then \n\
    CMD_OPTS="$CMD_OPTS --load-examples" \n\
fi \n\
\n\
if [ "$DEBUG" = "true" ]; then \n\
    CMD_OPTS="$CMD_OPTS --debug" \n\
fi \n\
\n\
# Set the port \n\
CMD_OPTS="$CMD_OPTS --port $SERVER_PORT" \n\
\n\
# Run the Java application with all options \n\
exec java $JAVA_OPTS -cp "/app/catalogue-1.0.jar:/app/lib/*" eu.sedimark.catalogue.CatalogueServerLauncher $CMD_OPTS $@' > /app/docker-entrypoint.sh \
    && chmod +x /app/docker-entrypoint.sh

# Set the entrypoint
ENTRYPOINT ["/app/docker-entrypoint.sh"]
