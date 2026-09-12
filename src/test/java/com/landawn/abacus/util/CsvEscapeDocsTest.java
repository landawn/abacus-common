package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class CsvEscapeDocsTest {
    @Test
    void explicitEscapeOnlyEscapesQuoteOrItselfInsideQuotes() {
        for (final CsvParser parser : List.of(new CsvParser(',', '"', '\\'), new CsvParser(',', '"', '\\', false), new CsvParser(',', '"', '\\', false, true),
                new CsvParser(',', '"', '\\', false, true, false))) {
            assertEquals(List.of("a\\", "b"), parser.parseLine("a\\,b"));
            assertEquals(List.of("a\\,b"), parser.parseLine("\"a\\,b\""));
            assertEquals(List.of("a\"b"), parser.parseLine("\"a\\\"b\""));
            assertEquals(List.of("a\\b"), parser.parseLine("\"a\\\\b\""));
            assertEquals(List.of("\uD83D\uDE00\\x"), parser.parseLine("\"\uD83D\uDE00\\x\""));
        }
    }

    @Test
    void defaultEscapeIsDisabledWhileQuoteDoublingStillWorks() {
        final CsvParser parser = new CsvParser();
        assertEquals('\0', parser.getEscape());
        assertEquals(List.of("a\\\\b"), parser.parseLine("\"a\\\\b\""));
        assertEquals(List.of("a\"b"), parser.parseLine("\"a\"\"b\""));
    }
}
