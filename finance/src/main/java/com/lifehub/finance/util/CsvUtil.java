package com.lifehub.finance.util;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Writes CSV (RFC 4180) that is safe to open in a spreadsheet. A value that a spreadsheet would run as a formula
 * (starting with =, +, -, @, tab or carriage return) gets a leading apostrophe, so a transaction described as
 * "=HYPERLINK(...)" is shown as text instead of executed (CSV injection).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CsvUtil {

    /** One CSV line, ending in CRLF, with every value escaped. */
    public static String row(List<String> values) {
        return values.stream().map(CsvUtil::cell).collect(Collectors.joining(",")) + "\r\n";
    }

    /** One escaped value: neutralised if formula-like, quoted if it contains a comma, quote or line break. */
    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
