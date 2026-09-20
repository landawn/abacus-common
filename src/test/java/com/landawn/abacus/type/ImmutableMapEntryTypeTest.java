package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class ImmutableMapEntryTypeTest extends TestBase {

    private ImmutableMapEntryType<String, Integer> immutableMapEntryType;
    private CharacterWriter characterWriter;

    @Mock
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        immutableMapEntryType = (ImmutableMapEntryType<String, Integer>) createType("Map.ImmutableEntry<String, Integer>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = immutableMapEntryType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Map.ImmutableEntry"));
        assertTrue(declaringName.contains("String"));
        assertTrue(declaringName.contains("Integer"));
    }

    @Test
    public void testClazz() {
        assertEquals(AbstractMap.SimpleImmutableEntry.class, immutableMapEntryType.javaType());
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = immutableMapEntryType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(immutableMapEntryType.isParameterizedType());
    }

    // Finding 58 (2026-09-08): ImmutableListType/ImmutableMapType/ImmutableSetType were given isImmutable() ==
    // true; this fourth immutable handler in the same package was missed and still inherited AbstractType's false,
    // although AbstractMap.SimpleImmutableEntry has final fields and a setValue that always throws.
    @Test
    public void reviewFixes20260908_immutableEntryHandlerReportsItsValuesImmutable() {
        assertTrue(immutableMapEntryType.isImmutable());
        assertTrue(createType("Map.ImmutableEntry<String, Object>").isImmutable());

        // the reason it is true: the handled value really cannot be modified
        assertThrows(UnsupportedOperationException.class, () -> new AbstractMap.SimpleImmutableEntry<>("k", 1).setValue(2));

        // ... and it now agrees with the three sibling immutable handlers
        assertTrue(createType("ImmutableList<String>").isImmutable());
        assertTrue(createType("ImmutableSet<String>").isImmutable());
        assertTrue(createType("ImmutableMap<String, Integer>").isImmutable());

        // a mutable Map.Entry handler is still reported mutable
        assertFalse(createType("Map.Entry<String, Integer>").isImmutable());
    }

    @Test
    public void testStringOf() {
        assertNull(immutableMapEntryType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(immutableMapEntryType.valueOf(null));
        assertNull(immutableMapEntryType.valueOf(""));
        assertNull(immutableMapEntryType.valueOf("{}"));

    }

    @Test
    public void testValueOfWithMultipleEntriesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> immutableMapEntryType.valueOf("{\"a\":1,\"b\":2}"));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        immutableMapEntryType.appendTo(writer, null);
        assertEquals("null", writer.toString());

    }

    @Test
    public void testAppendToPropagatesWriterIOException() {
        Writer writer = newFailingWriter();
        AbstractMap.SimpleImmutableEntry<String, Integer> entry = new AbstractMap.SimpleImmutableEntry<>("key", 123);
        assertThrows(IOException.class, () -> immutableMapEntryType.appendTo(writer, entry));
    }

    @Test
    public void testSerializeTo() throws IOException {
        assertDoesNotThrow(() -> {
            immutableMapEntryType.serializeTo(characterWriter, null, config);
        });
    }

    @Test
    public void testSerializeToQuotesNumericMapKeyByDefault() throws IOException {
        ImmutableMapEntryType<Integer, String> type = (ImmutableMapEntryType<Integer, String>) createType("Map.ImmutableEntry<Integer, String>");
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, new AbstractMap.SimpleImmutableEntry<>(1, "a"), JsonSerConfig.create());
            assertEquals("{\"1\":\"a\"}", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void testGetTypeName() {
        String typeName = ImmutableMapEntryType.getTypeName("String", "Integer", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Map.ImmutableEntry"));
        assertTrue(typeName.contains("String"));
        assertTrue(typeName.contains("Integer"));

        typeName = ImmutableMapEntryType.getTypeName("String", "Integer", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Map.ImmutableEntry"));
        assertTrue(typeName.contains("String"));
        assertTrue(typeName.contains("Integer"));
    }

    // --- review fixes 2026-09-06 (T10-06 pin, T10-08) ---

    private static String serialize(final ImmutableMapEntryType<String, Object> type, final Object value, final JsonSerConfig config) throws IOException {
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, new AbstractMap.SimpleImmutableEntry<>("k", value), config);
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void reviewFixes20260906_T1006_objectSlotDispatchesOnRuntimeType() throws IOException {
        final ImmutableMapEntryType<String, Object> type = (ImmutableMapEntryType<String, Object>) createType("Map.ImmutableEntry<String, Object>");
        final JsonSerConfig dflt = JsonSerConfig.create();
        final JsonSerConfig isoTs = JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

        assertEquals("{\"k\":1}", serialize(type, 1, dflt));
        assertEquals("{\"k\":true}", serialize(type, true, dflt));
        assertEquals("{\"k\":\"s\"}", serialize(type, "s", dflt));
        assertEquals("{\"k\":null}", serialize(type, null, dflt));
        assertEquals("{\"k\":0}", serialize(type, new Date(0), dflt));
        assertEquals("{\"k\":\"1970-01-01T00:00:00.000Z\"}", serialize(type, new Date(0), isoTs));

        assertEquals("{\"k\": 1}", type.stringOf(new AbstractMap.SimpleImmutableEntry<>("k", 1)));
        assertEquals(Integer.valueOf(1), type.valueOf(serialize(type, 1, dflt)).getValue());
        assertEquals(Boolean.TRUE, type.valueOf(serialize(type, true, dflt)).getValue());
        assertNull(type.valueOf(serialize(type, null, dflt)).getValue());
    }

    @Test
    public void reviewFixes20260906_T1008_malformedInputExceptionsAsDocumented() {
        assertNull(immutableMapEntryType.valueOf(" "));
        assertNull(immutableMapEntryType.valueOf("{}"));
        assertNull(immutableMapEntryType.valueOf(""));

        // P2-12: a structural mismatch is reported as ParsingException (it used to escape as a raw ClassCastException).
        assertThrows(ParsingException.class, () -> immutableMapEntryType.valueOf("[1,2]"));
        assertThrows(ParsingException.class, () -> immutableMapEntryType.valueOf("\"a\""));
        assertThrows(ParsingException.class, () -> immutableMapEntryType.valueOf("{\"a\":1}}"));
        assertThrows(NumberFormatException.class, () -> immutableMapEntryType.valueOf("{\"a\":\"x\"}"));

        assertEquals(Integer.valueOf(2), immutableMapEntryType.valueOf("{\"a\":1,\"a\":2}").getValue());
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
        final ImmutableMapEntryType<String, Map<String, Integer>> type = (ImmutableMapEntryType<String, Map<String, Integer>>) createType(
                "Map.ImmutableEntry<String, Map<String, Integer>>");
        final Map<String, Integer> inner = new LinkedHashMap<>();
        inner.put("x", 1);
        final AbstractMap.SimpleImmutableEntry<String, Map<String, Integer>> entry = new AbstractMap.SimpleImmutableEntry<>("k", inner);

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
            type.serializeTo(w2, new AbstractMap.SimpleImmutableEntry<>("k", new LinkedHashMap<>()), JsonSerConfig.create());
            assertEquals("{\"k\":{}}", w2.toString());
        } finally {
            Objectory.recycle(w2);
        }

        final BufferedJsonWriter w3 = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(w3, new AbstractMap.SimpleImmutableEntry<>("k", null), JsonSerConfig.create());
            assertEquals("{\"k\":null}", w3.toString());
        } finally {
            Objectory.recycle(w3);
        }

        // a Unicode key and value survive the embedded write
        final Map<String, Integer> uni = new LinkedHashMap<>();
        uni.put("é中", 2);
        final ImmutableMapEntryType<String, Map<String, Integer>> t2 = type;
        assertEquals("{\"é\": {\"é中\": 2}}", t2.stringOf(new AbstractMap.SimpleImmutableEntry<>("é", uni)));
    }

    // R05-2 sibling (2026-09-08): serializeTo writes the value through AbstractTupleType.serializeSlot, which
    // dispatches on the runtime class for a declared Object slot; appendTo went straight to the declared handler,
    // so an immutable map entry holding a map appended ObjectType's JSON stringOf form ({"k": 1}) where the
    // bare map, and this type's own serializeTo, use the toString()-style form ({k:1}).
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void reviewFixes20260908_appendToObjectValueSlotUsesRuntimeType() throws IOException {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", 1);
        map.put("s", "v");

        final Type objectSlot = Type.of("Map.ImmutableEntry<String, Object>");
        final AbstractMap.SimpleImmutableEntry<String, Object> entry = new AbstractMap.SimpleImmutableEntry<>("v", map);

        assertEquals("{v:{k:1, s:v}}", appendToStringR052(objectSlot, entry));
        assertEquals("{v:{k:1, s:v}}", appendToWriterR052(objectSlot, entry));

        // an Object value slot appends exactly what the bare value's own handler appends
        for (final Object value : new Object[] { map, List.of(1, "a"), 7, 1.5d, true, "q" }) {
            assertEquals("{v:" + appendToStringR052(Type.of(value.getClass()), value) + "}",
                    appendToStringR052(objectSlot, new AbstractMap.SimpleImmutableEntry<>("v", value)), String.valueOf(value));
        }

        // a declared (non-Object) value type keeps its own handler, and a null value still writes the literal
        assertEquals("{v:{k:1, s:v}}", appendToStringR052(Type.of("Map.ImmutableEntry<String, Map<String, Object>>"), entry));
        assertEquals("{v:null}", appendToStringR052(objectSlot, new AbstractMap.SimpleImmutableEntry<>("v", null)));
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
