package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class MapEntryTypeTest extends TestBase {

    private MapEntryType<String, Integer> mapEntryType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        mapEntryType = (MapEntryType<String, Integer>) createType("Map.Entry<String, Integer>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = mapEntryType.declaringName();
        Assertions.assertNotNull(declaringName);
        Assertions.assertTrue(declaringName.contains("Map.Entry"));
    }

    @Test
    public void testClazz() {
        Class<Map.Entry<String, Integer>> clazz = mapEntryType.javaType();
        Assertions.assertNotNull(clazz);
        assertEquals(Map.Entry.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = mapEntryType.parameterTypes();
        Assertions.assertNotNull(paramTypes);
        assertEquals(2, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        boolean isGeneric = mapEntryType.isParameterizedType();
        Assertions.assertTrue(isGeneric);
    }

    @Test
    public void testStringOfNull() {
        String result = mapEntryType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        String result = mapEntryType.stringOf(entry);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("123"));
    }

    @Test
    public void testValueOfNull() {
        Map.Entry<String, Integer> result = mapEntryType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        Map.Entry<String, Integer> result = mapEntryType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyObject() {
        Map.Entry<String, Integer> result = mapEntryType.valueOf("{}");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidJson() {
        Map.Entry<String, Integer> result = mapEntryType.valueOf("{\"key\":123}");
        Assertions.assertNotNull(result);
        assertEquals("key", result.getKey());
        assertEquals(123, result.getValue());
    }

    @Test
    public void testValueOfWithMultipleEntriesThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> mapEntryType.valueOf("{\"a\":1,\"b\":2}"));
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mapEntryType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        mapEntryType.appendTo(writer, entry);
        String result = writer.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("123"));
    }

    @Test
    public void testAppendToWithStringBuilder() throws IOException {
        StringBuilder sb = new StringBuilder();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        mapEntryType.appendTo(sb, entry);
        String result = sb.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("123"));
    }

    @Test
    public void testAppendToPropagatesWriterIOException() {
        Writer writer = newFailingWriter();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        assertThrows(IOException.class, () -> mapEntryType.appendTo(writer, entry));
    }

    @Test
    public void testAppendToDoesNotMaskRuntimeFailureDuringCleanup() {
        final RuntimeException writeFailure = new IllegalStateException("write failure");
        final IOException cleanupFailure = new IOException("cleanup failure");
        final Writer writer = new Writer() {
            private int writeCount;

            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                if (writeCount++ == 0) {
                    throw writeFailure;
                }

                throw cleanupFailure;
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() {
                // no-op
            }
        };
        final Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);

        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> mapEntryType.appendTo(writer, entry));

        assertSame(writeFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        assertDoesNotThrow(() -> {
            mapEntryType.serializeTo(characterWriter, null, null);
        });
    }

    @Test
    public void testSerializeToWithNonNull() throws IOException {
        assertDoesNotThrow(() -> {
            Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
            JsonXmlSerConfig<?> config = null;
            mapEntryType.serializeTo(characterWriter, entry, config);
        });
    }

    @Test
    public void testSerializeToQuotesNumericMapKeyByDefault() throws IOException {
        MapEntryType<Integer, String> type = (MapEntryType<Integer, String>) createType("Map.Entry<Integer, String>");
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, new AbstractMap.SimpleEntry<>(1, "a"), JsonSerConfig.create());
            assertEquals("{\"1\":\"a\"}", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }

    // --- review fixes 2026-09-06 (T10-06 pin, T10-08) ---

    private static String serialize(final MapEntryType<String, Object> type, final Object value, final JsonSerConfig config) throws IOException {
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, new AbstractMap.SimpleEntry<>("k", value), config);
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void reviewFixes20260906_T1006_objectSlotDispatchesOnRuntimeType() throws IOException {
        // pins the documented contract: an Object-typed value keeps its JSON shape and honours the config, so
        // serializeTo agrees with stringOf and with a Map<String, Object> holding the same value
        final MapEntryType<String, Object> type = (MapEntryType<String, Object>) createType("Map.Entry<String, Object>");
        final JsonSerConfig dflt = JsonSerConfig.create();
        final JsonSerConfig isoTs = JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

        assertEquals("{\"k\":1}", serialize(type, 1, dflt));
        assertEquals("{\"k\":true}", serialize(type, true, dflt));
        assertEquals("{\"k\":\"s\"}", serialize(type, "s", dflt));
        assertEquals("{\"k\":null}", serialize(type, null, dflt));
        assertEquals("{\"k\":0}", serialize(type, new Date(0), dflt));
        assertEquals("{\"k\":\"1970-01-01T00:00:00.000Z\"}", serialize(type, new Date(0), isoTs));

        assertEquals("{\"k\": 1}", type.stringOf(new AbstractMap.SimpleEntry<>("k", 1)));
        assertEquals(Integer.valueOf(1), type.valueOf(serialize(type, 1, dflt)).getValue());
        assertEquals(Boolean.TRUE, type.valueOf(serialize(type, true, dflt)).getValue());
        Assertions.assertNull(type.valueOf(serialize(type, null, dflt)).getValue());
    }

    @Test
    public void reviewFixes20260906_T1008_malformedInputExceptionsAsDocumented() {
        // blank -> null (documented alongside null/empty/"{}")
        Assertions.assertNull(mapEntryType.valueOf(" "));
        Assertions.assertNull(mapEntryType.valueOf("{}"));
        Assertions.assertNull(mapEntryType.valueOf(""));

        // not a JSON object / not convertible: the documented RuntimeException kinds
        // P2-12: a structural mismatch is reported as ParsingException (it used to escape as a raw ClassCastException).
        assertThrows(ParsingException.class, () -> mapEntryType.valueOf("[1,2]"));
        assertThrows(ParsingException.class, () -> mapEntryType.valueOf("\"a\""));
        assertThrows(ParsingException.class, () -> mapEntryType.valueOf("{\"a\":1}}"));
        assertThrows(NumberFormatException.class, () -> mapEntryType.valueOf("{\"a\":\"x\"}"));

        // duplicate key: last value wins (documented)
        assertEquals(Integer.valueOf(2), mapEntryType.valueOf("{\"a\":1,\"a\":2}").getValue());
    }

    private static Writer newFailingWriter() {
        return new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void close() throws IOException {
                throw new IOException("boom");
            }
        };
    }

    @Test
    public void reviewFixes20260907_declaredContainerValueSlotIsStructuralLikeItsSiblings() throws IOException {
        // The javadoc promises serializeTo agrees with stringOf and with a Map<K, V> holding the same value. That was
        // true only for an Object slot: a DECLARED container value type is not serializable, so its handler quoted its
        // whole JSON rendering as one string ({"k":"{\"x\": 1}"}), unlike Pair/Triple/Tuple/Optional after T6-01.
        final MapEntryType<String, Map<String, Integer>> type = (MapEntryType<String, Map<String, Integer>>) createType(
                "Map.Entry<String, Map<String, Integer>>");
        final Map<String, Integer> inner = new LinkedHashMap<>();
        inner.put("x", 1);
        final Map.Entry<String, Map<String, Integer>> entry = new AbstractMap.SimpleEntry<>("k", inner);

        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, entry, JsonSerConfig.create());
            assertEquals("{\"k\":{\"x\": 1}}", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }

        // agrees with stringOf, with the Pair sibling, and reads back as a Map (not as a String)
        assertEquals("{\"k\": {\"x\": 1}}", type.stringOf(entry));
        assertEquals(inner, type.valueOf(type.stringOf(entry)).getValue());

        // an empty map, a null value and a nested empty structure keep working
        final BufferedJsonWriter w2 = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(w2, new AbstractMap.SimpleEntry<>("k", new LinkedHashMap<>()), JsonSerConfig.create());
            assertEquals("{\"k\":{}}", w2.toString());
        } finally {
            Objectory.recycle(w2);
        }

        final BufferedJsonWriter w3 = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(w3, new AbstractMap.SimpleEntry<>("k", null), JsonSerConfig.create());
            assertEquals("{\"k\":null}", w3.toString());
        } finally {
            Objectory.recycle(w3);
        }

        // a Unicode key and value survive the embedded write
        final Map<String, Integer> uni = new LinkedHashMap<>();
        uni.put("é中", 2);
        final MapEntryType<String, Map<String, Integer>> t2 = type;
        assertEquals("{\"é\": {\"é中\": 2}}", t2.stringOf(new AbstractMap.SimpleEntry<>("é", uni)));
    }
    // R05-2 sibling (2026-09-08): serializeTo writes the value through AbstractTupleType.serializeSlot, which
    // dispatches on the runtime class for a declared Object slot; appendTo went straight to the declared handler,
    // so a Map.Entry<String, Object> holding a map appended ObjectType's JSON stringOf form ({"k": 1}) where the
    // bare map, and this type's own serializeTo, use the toString()-style form ({k:1}).
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void reviewFixes20260908_appendToObjectValueSlotUsesRuntimeType() throws IOException {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", 1);
        map.put("s", "v");

        final Type objectSlot = Type.of("Map.Entry<String, Object>");
        final Map.Entry<String, Object> entry = new AbstractMap.SimpleEntry<>("v", map);

        assertEquals("{v:{k:1, s:v}}", appendToStringR052(objectSlot, entry));
        assertEquals("{v:{k:1, s:v}}", appendToWriterR052(objectSlot, entry));

        // an Object value slot appends exactly what the bare value's own handler appends
        for (final Object value : new Object[] { map, List.of(1, "a"), 7, 1.5d, true, "q" }) {
            assertEquals("{v:" + appendToStringR052(Type.of(value.getClass()), value) + "}",
                    appendToStringR052(objectSlot, new AbstractMap.SimpleEntry<>("v", value)), String.valueOf(value));
        }

        // a declared (non-Object) value type keeps its own handler, and a null value still writes the literal
        assertEquals("{v:{k:1, s:v}}", appendToStringR052(Type.of("Map.Entry<String, Map<String, Object>>"), entry));
        assertEquals("{v:null}", appendToStringR052(objectSlot, new AbstractMap.SimpleEntry<>("v", null)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String appendToStringR052(final Type type, final Object value) throws IOException {
        final StringBuilder sb = new StringBuilder();
        type.appendTo(sb, value);
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String appendToWriterR052(final Type type, final Object value) throws IOException {
        final StringWriter sw = new StringWriter();
        type.appendTo(sw, value);
        return sw.toString();
    }
}
