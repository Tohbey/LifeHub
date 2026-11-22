package com.lifehub.app.model.dto;

import java.util.List;

/** The lists the UI offers in dropdowns: currencies and timezones, the preferred ones first. */
public record ReferenceDataDTO(List<CurrencyDTO> currencies, List<TimezoneDTO> timezones) {

    /** An ISO 4217 currency. {@code common} marks the preferred ones, listed first. */
    public record CurrencyDTO(String code, String name, String symbol, boolean common) {
    }

    /** An IANA timezone with its current UTC offset, for example "+01:00". */
    public record TimezoneDTO(String id, String offset, boolean common) {
    }
}
