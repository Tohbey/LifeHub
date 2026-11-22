package com.lifehub.app.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Which currencies and timezones the UI lists first. Every valid one is still offered after these.
 *
 * @param preferredCurrencies ISO 4217 codes, in the order to show them
 * @param preferredTimezones  IANA timezone IDs, in the order to show them
 */
@ConfigurationProperties("lifehub.reference")
public record ReferenceDataProperties(
        @DefaultValue({ "GBP", "EUR", "USD", "NGN" }) List<String> preferredCurrencies,
        @DefaultValue({ "Europe/London", "Africa/Lagos", "Europe/Paris", "America/New_York" }) List<String> preferredTimezones) {
}
