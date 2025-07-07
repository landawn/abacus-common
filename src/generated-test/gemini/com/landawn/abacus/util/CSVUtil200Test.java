package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CSVUtil200Test extends TestBase {

    @Test
    public void testSetAndGetCSVHeaderParser() {
        Function<String, String[]> defaultParser = CSVUtil.getCurrentHeaderParser();
        Function<String, String[]> customParser = (line) -> new String[] { "custom" };

        CSVUtil.setCSVHeaderParser(customParser);
        assertEquals(customParser, CSVUtil.getCurrentHeaderParser());

        CSVUtil.resetCSVHeaderParser();
        assertEquals(defaultParser, CSVUtil.getCurrentHeaderParser());
    }

    @Test
    public void testSetCSVHeaderParser_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.setCSVHeaderParser(null);
        });
    }

    @Test
    public void testSetAndGetCSVLineParser() {
        BiConsumer<String, String[]> defaultParser = CSVUtil.getCurrentLineParser();
        BiConsumer<String, String[]> customParser = (line, output) -> output[0] = "custom";

        CSVUtil.setCSVLineParser(customParser);
        assertEquals(customParser, CSVUtil.getCurrentLineParser());

        CSVUtil.resetCSVLineParser();
        assertEquals(defaultParser, CSVUtil.getCurrentLineParser());
    }

    @Test
    public void testSetCSVLineParser_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.setCSVLineParser(null);
        });
    }
}
