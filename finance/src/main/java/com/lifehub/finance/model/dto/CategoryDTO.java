package com.lifehub.finance.model.dto;

import java.util.UUID;

import com.lifehub.finance.model.entity.EntryType;

public record CategoryDTO(UUID id, EntryType type, String name) {
}
