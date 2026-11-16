package com.lifehub.finance.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.lifehub.finance.model.dto.SplitRequest;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.starter.web.ApiException;

class SplitPolicyTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void splitsThatAddUpAreFine() {
        assertThatCode(() -> SplitPolicy.validate(EntryType.EXPENSE, new BigDecimal("120.00"),
                List.of(split(a, "60.00"), split(b, "60.00")))).doesNotThrowAnyException();
        assertThatCode(() -> SplitPolicy.validate(EntryType.EXPENSE, new BigDecimal("100"),
                List.of(split(a, "33.33"), split(b, "66.67")))).doesNotThrowAnyException();
    }

    @Test
    void noSplitsIsAPersonalTransaction() {
        assertThatCode(() -> SplitPolicy.validate(EntryType.INCOME, BigDecimal.TEN, null)).doesNotThrowAnyException();
        assertThatCode(() -> SplitPolicy.validate(EntryType.EXPENSE, BigDecimal.TEN, List.of())).doesNotThrowAnyException();
    }

    @Test
    void splitsMustAddUpExactly() {
        assertThatThrownBy(() -> SplitPolicy.validate(EntryType.EXPENSE, new BigDecimal("120.00"),
                List.of(split(a, "60.00"), split(b, "59.99"))))
                .isInstanceOf(ApiException.class)
                .hasMessage("The splits add up to 119.99 but the amount is 120.00.");
    }

    @Test
    void aPersonAppearsOnceAndIncomeIsNeverSplit() {
        assertThatThrownBy(() -> SplitPolicy.validate(EntryType.EXPENSE, new BigDecimal("20"),
                List.of(split(a, "10"), split(a, "10")))).hasMessageContaining("only once");
        assertThatThrownBy(() -> SplitPolicy.validate(EntryType.INCOME, new BigDecimal("20"),
                List.of(split(a, "20")))).hasMessage("Only expenses can be split.");
    }

    /** A split of this amount for this user. */
    private static SplitRequest split(UUID user, String amount) {
        return new SplitRequest(user, new BigDecimal(amount));
    }
}
