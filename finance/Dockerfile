# Build the jar first: ./mvnw -pl finance -am package
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 lifehub
USER lifehub
WORKDIR /app
COPY target/finance-*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+ExitOnOutOfMemoryError"
EXPOSE 8083
# Health check: see deploy/docker-compose.lifehub.yml (the JRE image has no curl, so it uses bash).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
