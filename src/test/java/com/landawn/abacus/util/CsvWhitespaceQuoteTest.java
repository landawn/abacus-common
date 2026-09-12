package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class CsvWhitespaceQuoteTest {
    @Test
    void whitespaceQuoteSyntaxWinsInEveryParsingEntryPoint() {
        for (final char quote : new char[] { ' ', '\t', '\u2003' }) {
            for (final boolean strict : new boolean[] { false, true }) {
                final var parser = new CsvParser(',', quote, '\0', strict, true);
                final String field = quote + "a,\uD83D\uDE00" + quote;
                assertEquals(List.of("a,\uD83D\uDE00"), parser.parseLine(field));
                assertArrayEquals(new String[] { "a,\uD83D\uDE00" }, parser.parseLineToArray(field));
                final String[] output = new String[2];
                parser.parseLineInto(field + "," + quote + "" + quote, output);
                assertArrayEquals(new String[] { "a,\uD83D\uDE00", "" }, output);
                assertEquals(List.of("a,\uD83D\uDE00", "b,c"), parser.parseLine(field + "," + quote + "b,c" + quote));
                assertEquals(List.of("a" + quote + "b"), parser.parseLine(quote + "a" + quote + quote + "b" + quote));
            }
        }
    }

    @Test
    void ordinaryWhitespaceAndDisabledQuotingKeepTheirPolicies() {
        assertEquals(List.of("a", "b"), new CsvParser().parseLine("  a  ,  b  "));
        assertEquals(List.of("a", "b"), new CsvParser(',', '\t', '\0', false, true, true).parseLine("\ta,b\t"));
        assertEquals(List.of("", "", ""), new CsvParser('\t', '"', '\0', false, true).parseLine("\t\t"));
    }
}
