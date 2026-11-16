package com.lifehub.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Starts the whole application the way {@code spring-boot:run} does, but with MySQL and Kafka offline: no
 * connection is opened, yet every bean is wired, and every setting bound. A missing bean or setting fails here instead of at deployment.
 * Runs without Docker.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:1/offline",
        "spring.datasource.username=offline",
        "spring.datasource.password=offline",
        "spring.datasource.hikari.initialization-fail-timeout=-1",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.kafka.listener.auto-startup=false",
        "eureka.client.enabled=false",
        "lifehub.outbox.relay-enabled=false",
        "lifehub.planner.scheduler-enabled=false",
        "lifehub.security.issuer=https://auth.lifehub.test/oidc",
        "lifehub.security.audience=https://api.lifehub.test",
        "lifehub.security.internal-audience=https://internal.lifehub.test",
        "lifehub.security.ca-certificate=",
        "lifehub.service-auth.client-id=test",
        "lifehub.service-auth.client-secret=test" })
class ApplicationStartupTest {

    @Autowired
    ApplicationContext context;

    @Test
    void starts() {
        assertThat(context.getBean(AuditApplication.class)).isNotNull();
    }
}
