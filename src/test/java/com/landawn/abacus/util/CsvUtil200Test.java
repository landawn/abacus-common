package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CsvUtil200Test extends TestBase {

    @Test
    public void testSetAndGetCSVHeaderParser() {
        Function<String, String[]> defaultParser = CsvUtil.getCurrentHeaderParser();
        Function<String, String[]> customParser = (line) -> new String[] { "custom" };

        CsvUtil.setHeaderParser(customParser);
        assertEquals(customParser, CsvUtil.getCurrentHeaderParser());

        CsvUtil.resetHeaderParser();
        assertEquals(defaultParser, CsvUtil.getCurrentHeaderParser());
    }

    @Test
    public void testSetCSVHeaderParser_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            CsvUtil.setHeaderParser(null);
        });
    }

    @Test
    public void testSetAndGetCSVLineParser() {
        BiConsumer<String, String[]> defaultParser = CsvUtil.getCurrentLineParser();
        BiConsumer<String, String[]> customParser = (line, output) -> output[0] = "custom";

        CsvUtil.setLineParser(customParser);
        assertEquals(customParser, CsvUtil.getCurrentLineParser());

        CsvUtil.resetLineParser();
        assertEquals(defaultParser, CsvUtil.getCurrentLineParser());
    }

    @Test
    public void testSetCSVLineParser_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            CsvUtil.setLineParser(null);
        });
    }
}
