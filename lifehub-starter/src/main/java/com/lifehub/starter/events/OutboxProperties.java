package com.lifehub.starter.events;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param relayEnabled  turn off in tests that have no Kafka
 * @param batchSize     rows sent per transaction
 * @param sendTimeout   how long to wait for Kafka to acknowledge one record
 * @param retention     how long sent rows are kept before cleanup
 */
@ConfigurationProperties("lifehub.outbox")
public record OutboxProperties(
        @DefaultValue("true") boolean relayEnabled,
        @DefaultValue("100") int batchSize,
        @DefaultValue("10s") Duration sendTimeout,
        @DefaultValue("14d") Duration retention) {
}
