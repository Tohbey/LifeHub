# Build the jar first: ./mvnw -pl identity-workspace -am package
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 lifehub
USER lifehub
WORKDIR /app
COPY target/identity-workspace-*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
# Health check: see deploy/docker-compose.lifehub.yml (the JRE image has no curl, so it uses bash).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
