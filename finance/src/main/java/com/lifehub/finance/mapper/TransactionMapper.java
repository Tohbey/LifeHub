package com.lifehub.finance.mapper;

import java.util.List;

import com.lifehub.finance.model.dto.SplitDTO;
import com.lifehub.finance.model.dto.TransactionDTO;
import com.lifehub.finance.model.entity.Transaction;
import com.lifehub.finance.model.entity.TransactionSplit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TransactionMapper {

    /** Maps a transaction and its splits to a DTO. */
    public static TransactionDTO toDto(Transaction t, List<TransactionSplit> splits) {
        return new TransactionDTO(t.getId(), t.getType(), t.getDescription(), t.getAmount(), t.getCurrency(),
                t.getCategoryId(), t.getPayerUserId(), t.getOccurredAt(), t.getCreatedByUserId(),
                splits.stream().map(s -> new SplitDTO(s.getUserId(), s.getAmount())).toList());
    }
}
