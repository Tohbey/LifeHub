package com.lifehub.finance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lifehub.finance.util.CsvUtil;

class CsvUtilTest {

    @Test
    void quotesCommasQuotesAndLineBreaks() {
        assertThat(CsvUtil.row(List.of("Food, drink", "He said \"hi\"", "two\nlines", "plain")))
                .isEqualTo("\"Food, drink\",\"He said \"\"hi\"\"\",\"two\nlines\",plain\r\n");
    }

    @Test
    void neutralisesValuesASpreadsheetWouldRunAsFormulas() {
        assertThat(CsvUtil.row(List.of("=HYPERLINK(\"http://evil\")", "+1", "-2", "@SUM(A1)", "120.00")))
                .isEqualTo("\"'=HYPERLINK(\"\"http://evil\"\")\",'+1,'-2,'@SUM(A1),120.00\r\n");
    }

    @Test
    void writesNothingForMissingValues() {
        assertThat(CsvUtil.row(java.util.Arrays.asList("a", null, "b"))).isEqualTo("a,,b\r\n");
    }
}
