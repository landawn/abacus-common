package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.TypeReference;

public class SheetTypeTest extends TestBase {

    private SheetType<String, String, Integer> sheetType;

    @BeforeEach
    public void setUp() {
        sheetType = new SheetType<>("String", "String", "Integer");
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(sheetType.declaringName());
        assertTrue(sheetType.declaringName().contains("Sheet"));
    }

    @Test
    public void testClazz() {
        assertEquals(Sheet.class, sheetType.javaType());
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = sheetType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(3, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(sheetType.isParameterizedType());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(sheetType.isSerializable());
    }

    @Test
    public void testGetSerializationType() {
        assertEquals(Type.SerializationType.SHEET, sheetType.serializationType());
    }

    @Test
    public void testStringOf() {
        assertNull(sheetType.stringOf(null));

        Sheet<String, String, Integer> sheet = Sheet.rows(N.toList("r1", "r2", "r3"), N.toList("c1", "c2"),
                new Integer[][] { { 1, 2 }, { null, 4 }, { 5, 6 } });

        String result = sheetType.stringOf(sheet);
        assertNotNull(result);
    }

    @Test
    public void testValueOf() {
        assertNull(sheetType.valueOf(null));
        assertNull(sheetType.valueOf(""));
        assertNull(sheetType.valueOf(" "));
    }

    @Test
    public void testRoundTripPreservesDeclaredKeyAndValueTypes() {
        SheetType<String, Integer, Long> typedSheetType = new SheetType<>("String", "Integer", "Long");
        Sheet<String, Integer, Long> source = Sheet.rows(List.of("row"), List.of(7), new Long[][] { { 9L } });

        Sheet<String, Integer, Long> parsed = typedSheetType.valueOf(typedSheetType.stringOf(source));

        assertEquals(String.class, parsed.rowKeySet().iterator().next().getClass());
        assertEquals(Integer.class, parsed.columnKeySet().iterator().next().getClass());
        assertEquals(Long.class, parsed.get("row", 7).getClass());
        assertEquals(9L, parsed.get("row", 7));
    }

    @ParameterizedTest
    @ValueSource(strings = { "type", "json", "json-types", "xml", "xml-dom", "abacus-xml", "abacus-xml-dom" })
    public void arrayKeyRoundTripPreservesContentsAndUsesParsedKeyIdentities(final String format) throws Exception {
        final SheetType<int[], String[], Integer> type = new SheetType<>("int[]", "String[]", "Integer");
        final int[] row = { 1, 2 };
        final String[] column = { "c", "<&>" };
        // Distinct keys with equal contents must survive as two rows and two columns.
        final Sheet<int[], String[], Integer> source = Sheet.rows(List.of(row, row.clone()), List.of(column, column.clone()),
                new Integer[][] { { 1, 2 }, { 3, 4 } });
        source.freeze();
        final Sheet<int[], String[], Integer> parsed;
        if ("type".equals(format)) {
            parsed = type.valueOf(type.stringOf(source));
        } else if (format.startsWith("json")) {
            final var parser = ParserFactory.createJsonParser();
            parsed = parser.deserialize(
                    parser.serialize(source,
                            JsonSerConfig.create().setWriteRowColumnKeyType(format.endsWith("-types")).setWriteColumnType(format.endsWith("-types"))),
                    null, type);
        } else {
            final XmlParser parser = format.startsWith("abacus") ? ParserFactory.createAbacusXmlParser() : ParserFactory.createXmlParser();
            // Embed the Sheet as JSON in a tuple slot inside an XML list; Sheet is not a supported XML root.
            final Type<List<Tuple1<Sheet<int[], String[], Integer>>>> listType = Type.of(new TypeReference<List<Tuple1<Sheet<int[], String[], Integer>>>>() {
            });
            final String xml = parser.serialize(List.of(Tuple.of(source)));
            final List<Tuple1<Sheet<int[], String[], Integer>>> restored = format.endsWith("-dom") ? parser.deserialize(
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement(), listType)
                    : parser.deserialize(xml, null, listType);
            parsed = restored.get(0)._1;
        }

        assertEquals(2, parsed.rowCount());
        assertEquals(2, parsed.columnCount());
        assertTrue(parsed.isFrozen());
        assertNotEquals(source, parsed);
        final List<int[]> rows = new ArrayList<>(parsed.rowKeySet());
        final List<String[]> columns = new ArrayList<>(parsed.columnKeySet());
        assertNotSame(rows.get(0), rows.get(1));
        assertNotSame(columns.get(0), columns.get(1));
        for (int i = 0; i < 2; i++) {
            assertArrayEquals(row, rows.get(i));
            assertArrayEquals(column, columns.get(i));
            assertNotSame(row, rows.get(i));
            assertNotSame(column, columns.get(i));
            for (int j = 0; j < 2; j++) {
                assertEquals(source.getAt(i, j), parsed.get(rows.get(i), columns.get(j)));
            }
        }
        assertFalse(parsed.containsRow(row));
        assertFalse(parsed.containsColumn(column));
        assertThrows(IllegalArgumentException.class, () -> parsed.get(row, columns.get(0)));
        assertThrows(IllegalArgumentException.class, () -> parsed.get(rows.get(0), column));
    }

    @Test
    public void arrayColumnNamesResolveByContentsAndConsumeDuplicateOccurrences() {
        final var parser = ParserFactory.createJsonParser();
        final SheetType<String, int[], Integer> type = new SheetType<>("String", "int[]", "Integer");
        final String prefix = "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[[1],[2],[1]],\"columns\":";
        final Sheet<String, int[], Integer> parsed = parser.deserialize(prefix + "{\"[2]\":[20],\"[1]\":[10],\"[1]\":[30]}}", null, type);
        final List<int[]> keys = new ArrayList<>(parsed.columnKeySet());
        assertEquals(3, parsed.columnCount());
        assertNotSame(keys.get(0), keys.get(2));
        assertEquals(10, parsed.get("r", keys.get(0)));
        assertEquals(20, parsed.get("r", keys.get(1)));
        assertEquals(30, parsed.get("r", keys.get(2)));
        // Equal-content names carry no identity: swapping their occurrences swaps the associated values.
        final Sheet<String, int[], Integer> reordered = type.valueOf(prefix + "{\"[1]\":[30],\"[2]\":[20],\"[1]\":[10]}}");
        final List<int[]> reorderedKeys = new ArrayList<>(reordered.columnKeySet());
        assertEquals(30, reordered.get("r", reorderedKeys.get(0)));
        assertEquals(20, reordered.get("r", reorderedKeys.get(1)));
        assertEquals(10, reordered.get("r", reorderedKeys.get(2)));
        assertThrows(ParsingException.class, () -> parser.deserialize(prefix + "{\"[3]\":[10]}}", null, type));
        assertThrows(ParsingException.class, () -> parser.deserialize(prefix + "{\"[1]\":[10],\"[1]\":[20],\"[1]\":[30]}}", null, type));
    }

    @Test
    public void repeatedArrayColumnNamesUseTheMatchedOccurrenceType() {
        final SheetType<String, int[], Object> type = new SheetType<>("String", "int[]", "Object");
        // Type hints belong to key-set slots, not property order or the first content-equal key.
        final String json = "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[[1],[2],[1]]," + "\"columnTypes\":[\"Integer\",\"Boolean\",\"String\"],"
                + "\"columns\":{\"[2]\":[true],\"[1]\":[7],\"[1]\":[\"last\"]}}";
        final Sheet<String, int[], Object> parsed = type.valueOf(json);
        final List<int[]> keys = new ArrayList<>(parsed.columnKeySet());
        assertNotSame(keys.get(0), keys.get(2));
        assertEquals(Integer.valueOf(7), parsed.get("r", keys.get(0)));
        assertEquals(Boolean.TRUE, parsed.get("r", keys.get(1)));
        assertEquals("last", parsed.get("r", keys.get(2)));
        assertThrows(IllegalArgumentException.class, () -> parsed.get("r", new int[] { 1 }));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    public void emptyAndAllNullColumnsStillConsumeTheirArrayKeyOccurrence(final int rowCount) {
        final SheetType<String, int[], Integer> type = new SheetType<>("String", "int[]", "Integer");
        final String prefix = "{\"rowKeySet\":" + (rowCount == 0 ? "[]" : "[\"r\"]") + ",\"columnKeySet\":[[1],[1]],\"columns\":{";
        final String first = "\"[1]\":" + (rowCount == 0 ? "[]" : "[null]");
        final String second = "\"[1]\":" + (rowCount == 0 ? "[]" : "[7]");
        final String valid = prefix + first + "," + second + "}}";
        // Empty contents are still a supplied column; only an unfilled slot is available to the next occurrence.
        assertThrows(ParsingException.class, () -> type.valueOf(prefix + first + "," + second + "," + second + "}}"));
        final Sheet<String, int[], Integer> parsed = type.valueOf(valid);
        final List<int[]> keys = new ArrayList<>(parsed.columnKeySet());
        assertEquals(rowCount, parsed.rowCount());
        assertEquals(2, parsed.columnCount());
        assertNotSame(keys.get(0), keys.get(1));
        if (rowCount != 0) {
            assertNull(parsed.get("r", keys.get(0)));
            assertEquals(Integer.valueOf(7), parsed.get("r", keys.get(1)));
        }
    }

    @Test
    public void testGetTypeName() {
        String typeName = SheetType.getTypeName(Sheet.class, "String", "String", "Integer", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Sheet"));

        String declaringName = SheetType.getTypeName(Sheet.class, "String", "String", "Integer", true);
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Sheet"));
    }
}
