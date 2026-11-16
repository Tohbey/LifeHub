package com.lifehub.identityworkspace.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code defaultCurrency} defaults to GBP and {@code timezone} to Europe/London. */
public record CreateHouseholdRequest(
        @NotBlank @Size(max = 100) String name,
        @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO 4217 code such as GBP") String defaultCurrency,
        @Size(max = 64) String timezone) {
}
