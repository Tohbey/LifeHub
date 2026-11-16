package com.lifehub.finance.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.lifehub.finance.model.dto.SplitRequest;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.starter.web.ApiException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The rules for shared expenses (README, data rule 9): only expenses can be split, each person appears once,
 * and the shares add up exactly to the amount. Membership of the participants is checked separately.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SplitPolicy {

    /** Fails with 400 when the splits break a rule. No splits (null or empty) is always fine. */
    public static void validate(EntryType type, BigDecimal amount, List<SplitRequest> splits) {
        if (splits == null || splits.isEmpty()) {
            return;
        }
        if (type != EntryType.EXPENSE) {
            throw ApiException.badRequest("Only expenses can be split.");
        }
        Set<UUID> people = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (SplitRequest split : splits) {
            if (!people.add(split.userId())) {
                throw ApiException.badRequest("Each person can appear only once in the splits.");
            }
            total = total.add(split.amount());
        }
        if (total.compareTo(amount) != 0) {
            throw ApiException.badRequest("The splits add up to " + total.toPlainString()
                    + " but the amount is " + amount.toPlainString() + ".");
        }
    }
}
