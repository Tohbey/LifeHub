# Build the jar first: ./mvnw -pl lifehub-app -am package
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 lifehub
USER lifehub
WORKDIR /app
COPY target/lifehub-app-*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
# No HEALTHCHECK yet: this JRE image ships without curl or wget. Health checks come with the Compose files.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
