package com.lifehub.starter.events;

import java.time.Clock;
import java.time.Duration;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.backoff.ExponentialBackOff;

import com.lifehub.starter.membership.MembershipAutoConfiguration;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Outbox publishing for every service, plus idempotency and dead-lettering for services that consume
 * events ({@code lifehub.events.consumer.enabled=true}). The tables come from the starter's Liquibase
 * changesets, which each service includes in its own changelog.
 */
@AutoConfiguration(after = MembershipAutoConfiguration.class)
@EnableConfigurationProperties(OutboxProperties.class)
public class EventsAutoConfiguration {

    /** Provides the publisher services use to write events to their outbox. */
    @Bean
    @ConditionalOnMissingBean
    DomainEventPublisher domainEventPublisher(JdbcClient jdbc, JsonMapper jsonMapper, Clock clock) {
        return new DomainEventPublisher(jdbc, jsonMapper, clock);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(name = "lifehub.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
    static class RelayConfiguration {

        /** Provides the scheduled relay that sends outbox rows to Kafka. */
        @Bean
        @ConditionalOnMissingBean
        OutboxRelay outboxRelay(JdbcClient jdbc, KafkaTemplate<String, String> kafka,
                PlatformTransactionManager transactionManager, OutboxProperties properties, Clock clock) {
            return new OutboxRelay(jdbc, kafka, new TransactionTemplate(transactionManager), properties, clock);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(name = "lifehub.events.consumer.enabled", havingValue = "true")
    static class ConsumerConfiguration {

        /** Topic retention is 14 days, so no duplicate can arrive after that. */
        private static final Duration PROCESSED_EVENTS_RETENTION = Duration.ofDays(15);

        /** Provides the duplicate check consumers call before handling an event. */
        @Bean
        @ConditionalOnMissingBean
        ProcessedEvents processedEvents(JdbcClient jdbc, Clock clock) {
            return new ProcessedEvents(jdbc, clock);
        }

        /**
         * Retries a failing record a few times with backoff, then parks it on {@code <topic>.dlt}.
         * Picked up by Spring Boot's listener container factory.
         */
        @Bean
        @ConditionalOnMissingBean
        CommonErrorHandler lifehubKafkaErrorHandler(KafkaTemplate<String, String> kafka) {
            var recoverer = new DeadLetterPublishingRecoverer(kafka,
                    (record, ex) -> new TopicPartition(record.topic() + ".dlt", -1));
            var backOff = new ExponentialBackOff(1_000, 2.0);
            backOff.setMaxElapsedTime(30_000);
            return new DefaultErrorHandler(recoverer, backOff);
        }

        /** Provides the nightly cleanup of old processed-event records. */
        @Bean
        ProcessedEventsCleanup processedEventsCleanup(ProcessedEvents processedEvents) {
            return new ProcessedEventsCleanup(processedEvents);
        }

        @RequiredArgsConstructor
        static class ProcessedEventsCleanup {

            private final ProcessedEvents processedEvents;

            /** Runs nightly: deletes processed-event records older than the topic retention. */
            @Scheduled(cron = "${lifehub.events.consumer.cleanup-cron:0 30 3 * * *}")
            void cleanup() {
                processedEvents.deleteOlderThan(PROCESSED_EVENTS_RETENTION);
            }
        }
    }
}
