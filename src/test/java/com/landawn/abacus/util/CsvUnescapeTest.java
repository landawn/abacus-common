package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class CsvUnescapeTest {
    @Test
    void everyValidFieldDecodesRegardlessOfWhetherQuotesWereRequired() throws Exception {
        for (final String value : List.of("", " ", "plain", "\uD83D\uDE00", "\u03B1", ",", "\r", "\n", "\"", "a\"b", "\"\"")) {
            final String encoded = "\"" + value.replace("\"", "\"\"") + "\"";
            assertEquals(value, EscapeUtil.unescapeCsv(encoded));
            assertEquals(value, EscapeUtil.unescapeCsv(EscapeUtil.escapeCsv(value)));
            final StringWriter out = new StringWriter();
            assertEquals(encoded.codePointCount(0, encoded.length()), new EscapeUtil.CsvUnescaper().translate(encoded, 0, out));
            assertEquals(value, out.toString());
        }
    }

    @Test
    void unquotedMalformedAndNullInputsKeepTheirPermissivePolicy() {
        assertNull(EscapeUtil.unescapeCsv(null));
        for (final String value : List.of("", "plain", "\"", "\"abc", "abc\"", "a\"b")) {
            assertEquals(value, EscapeUtil.unescapeCsv(value));
        }
        assertEquals("a\"b", EscapeUtil.unescapeCsv("\"a\"b\""));
    }
}
