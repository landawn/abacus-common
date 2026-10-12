package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Sheet;

public class SheetKeyOrderReview20261003Test extends TestBase {
    private static Sheet<String, String, Integer> read(final boolean reader, final String json) {
        final JsonParser parser = ParserFactory.createJsonParser();
        final Type<Sheet<String, String, Integer>> type = Type.of("Sheet<String, String, Integer>");
        return reader ? parser.deserialize(new StringReader(json), type) : parser.deserialize(json, type);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void consumedColumnKeysCannotBeReordered(final boolean reader) {
        assertThrows(ParsingException.class,
                () -> read(reader, "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"a\",\"b\"],\"columns\":{\"a\":[1],\"b\":[2]},\"columnKeySet\":[\"b\",\"a\"]}"));
        assertThrows(ParsingException.class, () -> read(reader, "{\"columns\":{},\"columnKeySet\":[]}"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void keyReplacementBeforeConsumptionAndLateRowsRemainSupported(final boolean reader) {
        final Sheet<String, String, Integer> sheet = read(reader,
                "{\"columnKeySet\":[\"a\",\"b\"],\"columnKeySet\":[\"b\",\"a\"],\"columns\":{\"a\":[1],\"b\":[2]},\"rowKeySet\":[\"r\"]}");
        assertEquals(List.of("b", "a"), List.copyOf(sheet.columnKeySet()));
        assertEquals(Integer.valueOf(1), sheet.get("r", "a"));
        assertEquals(Integer.valueOf(2), sheet.get("r", "b"));
    }
}
