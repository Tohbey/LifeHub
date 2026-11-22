package com.lifehub.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class AuditServiceImplTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void findsEveryUserIdInAPayloadIncludingNestedOnes() {
        UUID payer = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var payload = json.readTree("""
                {"transactionId":"%s","payerUserId":"%s","amount":"120.00","description":"not-a-uuid",
                 "splits":[{"userId":"%s","amount":"60.00"},{"userId":"%s","amount":"60.00"}]}
                """.formatted(UUID.randomUUID(), payer, payer, other));

        Set<UUID> ids = new HashSet<>();
        AuditServiceImpl.collectIds(payload, ids);

        assertThat(ids).contains(payer, other).hasSize(3);
    }
}
