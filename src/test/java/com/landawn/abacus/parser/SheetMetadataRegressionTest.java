package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Sheet;

@Tag("unit")
class SheetMetadataRegressionTest extends TestBase {
    @Test
    void rejectsColumnTypesThatArriveAfterColumns() {
        final JsonParser parser = ParserFactory.createJsonParser();
        final String json = "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"c\"],\"columns\":{\"c\":[1]},\"columnTypes\":[\"Long\"]}";

        assertThrows(ParsingException.class, () -> parser.deserialize(json, Sheet.class));
        assertThrows(ParsingException.class, () -> parser.deserialize("{\"columns\":{},\"columnTypes\":[]}", Sheet.class));
    }

    @Test
    void sheetColumnNamesMatchConfiguredBigDecimalKeyRepresentation() {
        final JsonParser parser = ParserFactory.createJsonParser();
        final Sheet<String, BigDecimal, Integer> sheet = Sheet.columns(List.of("r"), List.of(new BigDecimal("1E+3")), List.of(List.of(7)));
        final String json = parser.serialize(sheet, JsonSerConfig.create().setWriteBigDecimalAsPlain(true));
        final Sheet<String, BigDecimal, Integer> parsed = parser.deserialize(json, Type.of("Sheet<String, java.math.BigDecimal, Integer>"));

        assertEquals(Integer.valueOf(7), parsed.get("r", new BigDecimal("1000")));

        final String defaultJson = parser.serialize(sheet);
        final Sheet<String, BigDecimal, Integer> defaultParsed = parser.deserialize(defaultJson, Type.of("Sheet<String, java.math.BigDecimal, Integer>"));

        assertEquals(Integer.valueOf(7), defaultParsed.get("r", new BigDecimal("1E+3")));
    }

    // ---- bug review 2026-10-02 final G127 begin ----
    // G127 guard: columnTypes before columns (anywhere before) is still applied, and other metadata may still follow columns.
    @Test
    void columnTypesBeforeColumnsStillApplied() {
        final JsonParser parser = ParserFactory.createJsonParser();
        final Sheet<Object, Object, Object> typed = parser.deserialize(
                "{\"columnTypes\":[\"Long\"],\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"c\"],\"columns\":{\"c\":[1]},\"isFrozen\":true}", Sheet.class);

        assertEquals(Long.valueOf(1), typed.get("r", "c"));
        assertEquals(true, typed.isFrozen());
    }

    // G127: with writeBigDecimalAsPlain a scientific-notation column key also reads back into an untyped Sheet (its column
    // names are compared as strings there), and a key whose plain and default texts agree is written unchanged.
    @Test
    void plainBigDecimalColumnKeysReadBackUntypedAndKeepPlainText() {
        final JsonParser parser = ParserFactory.createJsonParser();
        final JsonSerConfig plain = JsonSerConfig.create().setWriteBigDecimalAsPlain(true);
        final Sheet<String, BigDecimal, Integer> sheet = Sheet.columns(List.of("r"), List.of(new BigDecimal("-1E+2"), new BigDecimal("1.50")),
                List.of(List.of(7), List.of(8)));
        final String json = parser.serialize(sheet, plain);

        assertEquals("{\"rowKeySet\": [\"r\"], \"columnKeySet\": [-100, 1.50], \"columns\": {\"-100\": [7], \"1.50\": [8]}}", json);

        final Sheet<Object, Object, Object> untyped = parser.deserialize(json, Sheet.class);
        assertEquals(Integer.valueOf(7), untyped.get("r", "-100"));
        assertEquals(Integer.valueOf(8), untyped.get("r", "1.50"));
    }
    // ---- bug review 2026-10-02 final G127 end ----
}
