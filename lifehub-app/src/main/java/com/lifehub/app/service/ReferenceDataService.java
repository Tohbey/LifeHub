package com.lifehub.app.service;

import com.lifehub.app.model.dto.ReferenceDataDTO;

/** Currencies and timezones for the UI's dropdowns. Built from the JDK's ISO 4217 and IANA data; no database. */
public interface ReferenceDataService {

    /** Every usable currency and timezone, the preferred ones first. */
    ReferenceDataDTO getReferenceData();
}
