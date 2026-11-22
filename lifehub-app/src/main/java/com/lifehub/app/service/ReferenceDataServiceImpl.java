package com.lifehub.app.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.lifehub.app.config.ReferenceDataProperties;
import com.lifehub.app.model.dto.ReferenceDataDTO;
import com.lifehub.app.model.dto.ReferenceDataDTO.CurrencyDTO;
import com.lifehub.app.model.dto.ReferenceDataDTO.TimezoneDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class ReferenceDataServiceImpl implements ReferenceDataService {

    /** Codes that are not money people spend: precious metals, test and fund codes. */
    private static final Set<String> NOT_SPENDABLE = Set.of("XAG", "XAU", "XBA", "XBB", "XBC", "XBD", "XDR", "XPD",
            "XPT", "XSU", "XTS", "XUA", "XXX", "BOV", "CHE", "CHW", "CLF", "COU", "MXV", "USN", "USS", "UYI", "UYW");

    /** Region/City zones only; leaves out legacy aliases such as "EST" or "SystemV/...". */
    private static final Pattern REGION_ZONE =
            Pattern.compile("^(Africa|America|Antarctica|Arctic|Asia|Atlantic|Australia|Europe|Indian|Pacific)/.+");

    private final ReferenceDataProperties properties;
    private final Clock clock = Clock.systemUTC();

    /** Builds both lists. Offsets are worked out now, so they follow daylight saving. */
    @Override
    public ReferenceDataDTO getReferenceData() {
        return new ReferenceDataDTO(currencies(), timezones());
    }

    /** Spendable ISO 4217 currencies: preferred first in their configured order, then the rest by code. */
    private List<CurrencyDTO> currencies() {
        List<String> preferred = properties.preferredCurrencies();
        return Currency.getAvailableCurrencies().stream()
                .filter(c -> c.getDefaultFractionDigits() >= 0 && !NOT_SPENDABLE.contains(c.getCurrencyCode()))
                .map(c -> new CurrencyDTO(c.getCurrencyCode(), c.getDisplayName(Locale.ENGLISH), c.getSymbol(Locale.UK),
                        preferred.contains(c.getCurrencyCode())))
                .sorted(Comparator.comparingInt((CurrencyDTO c) -> rank(preferred, c.code())).thenComparing(CurrencyDTO::code))
                .toList();
    }

    /** IANA Region/City zones plus UTC: preferred first, then by ID. */
    private List<TimezoneDTO> timezones() {
        List<String> preferred = properties.preferredTimezones();
        Instant now = clock.instant();
        return Stream.concat(Stream.of("UTC"), ZoneId.getAvailableZoneIds().stream().filter(id -> REGION_ZONE.matcher(id).matches()))
                .distinct()
                .map(id -> new TimezoneDTO(id, offset(id, now), preferred.contains(id)))
                .sorted(Comparator.comparingInt((TimezoneDTO z) -> rank(preferred, z.id())).thenComparing(TimezoneDTO::id))
                .toList();
    }

    /** The zone's UTC offset at this instant, "+00:00" for UTC. */
    private static String offset(String id, Instant now) {
        String offset = ZoneId.of(id).getRules().getOffset(now).getId();
        return "Z".equals(offset) ? "+00:00" : offset;
    }

    /** Position in the preferred list, or after all of them. */
    private static int rank(List<String> preferred, String value) {
        int index = preferred.indexOf(value);
        return index < 0 ? preferred.size() : index;
    }
}
