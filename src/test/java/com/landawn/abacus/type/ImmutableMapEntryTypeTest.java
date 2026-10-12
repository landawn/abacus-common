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
import java.util.List;
import java.util.Map;

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

    // ---- deep review 2026-09-25 G014 begin ----

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String serializeEntryG014(final Type type, final Object key, final JsonSerConfig config) throws IOException {
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, new AbstractMap.SimpleImmutableEntry<>(key, "x"), config);
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    // G014-01 (sibling): serializeTo wrote a config-sensitive key (date/time, long, BigDecimal) with stringOf, ignoring the
    // dateTimeFormat / writeLongAsString / writeBigDecimalAsPlain options the JSON map writer applies to the same key.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSerializeTo_configSensitiveKeyMatchesMapWriter() throws IOException {
        final Date key = new Date(1600000000123L);
        final Type dateKeyType = createType("Map.ImmutableEntry<JUDate, String>");
        final JsonSerConfig isoTs = JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

        final String json = serializeEntryG014(dateKeyType, key, isoTs);
        assertEquals("{\"2020-09-13T12:26:40.123Z\":\"x\"}", json);
        assertTrue(com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asMap(key, "x"), isoTs).contains("\"2020-09-13T12:26:40.123Z\""));
        assertEquals(key.getTime(), ((Map.Entry<Date, String>) dateKeyType.valueOf(json)).getKey().getTime());

        // the default config (DateTimeFormat.LONG) keeps the type's own text for a date key, like the map writer
        assertEquals("{\"2020-09-13T12:26:40Z\":\"x\"}", serializeEntryG014(dateKeyType, key, JsonSerConfig.create()));

        // writeLongAsString keeps a long key quoted even when quoteMapKey is off, like the map writer
        final Type longKeyType = createType("Map.ImmutableEntry<Long, String>");
        final JsonSerConfig longAsString = JsonSerConfig.create().setWriteLongAsString(true).setQuoteMapKey(false);
        assertEquals("{\"5\":\"x\"}", serializeEntryG014(longKeyType, 5L, longAsString));
        assertEquals("{5:\"x\"}", serializeEntryG014(longKeyType, 5L, JsonSerConfig.create().setQuoteMapKey(false)));

        // writeBigDecimalAsPlain applies to a BigDecimal key
        final Type bigDecimalKeyType = createType("Map.ImmutableEntry<BigDecimal, String>");
        assertEquals("{\"1000\":\"x\"}",
                serializeEntryG014(bigDecimalKeyType, new java.math.BigDecimal("1E+3"), JsonSerConfig.create().setWriteBigDecimalAsPlain(true)));
    }

    // ---- deep review 2026-09-25 G014 end ----

    // ---- bug review 2026-09-27 verify G123 begin ----

    // G123-01: with stringQuotation '\'' a config-sensitive key the type already quoted ('5', an ISO date) was wrapped
    // in another pair of double quotes ("'5'"), so the entry no longer read back.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSerializeTo_singleQuotedConfigSensitiveKeyNotDoubleQuoted() throws IOException {
        final Type longKeyType = createType("Map.ImmutableEntry<Long, String>");
        final JsonSerConfig longAsString = JsonSerConfig.create().setStringQuotation('\'').setWriteLongAsString(true);
        final String json = serializeEntryG014(longKeyType, 5L, longAsString);
        assertEquals("{'5':'x'}", json);
        assertEquals(5L, ((Map.Entry<Long, String>) longKeyType.valueOf(json)).getKey());

        final Type atomicLongKeyType = createType("Map.ImmutableEntry<AtomicLong, String>");
        assertEquals("{'7':'x'}", serializeEntryG014(atomicLongKeyType, new java.util.concurrent.atomic.AtomicLong(7),
                JsonSerConfig.create().setStringQuotation('\'').setWriteLongAsString(true)));

        final Date key = new Date(1600000000123L);
        final Type dateKeyType = createType("Map.ImmutableEntry<JUDate, String>");
        final JsonSerConfig isoTs = JsonSerConfig.create().setStringQuotation('\'').setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);
        final String dateJson = serializeEntryG014(dateKeyType, key, isoTs);
        assertEquals("{'2020-09-13T12:26:40.123Z':'x'}", dateJson);
        assertEquals(key.getTime(), ((Map.Entry<Date, String>) dateKeyType.valueOf(dateJson)).getKey().getTime());
    }

    // G123-01 neighbours: '"' and no quotation are unchanged, quoteMapKey off still keeps the type's quotes, and a String key
    // whose text is wrapped in quotes is still written as a quoted String.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSerializeTo_configSensitiveKeyUnderEachStringQuotation() throws IOException {
        final Type longKeyType = createType("Map.ImmutableEntry<Long, String>");
        assertEquals("{'5':'x'}",
                serializeEntryG014(longKeyType, 5L, JsonSerConfig.create().setStringQuotation('\'').setWriteLongAsString(true).setQuoteMapKey(false)));
        assertEquals("{\"5\":\"x\"}", serializeEntryG014(longKeyType, 5L, JsonSerConfig.create().setStringQuotation('"').setWriteLongAsString(true)));
        assertEquals("{\"5\":x}", serializeEntryG014(longKeyType, 5L, JsonSerConfig.create().setStringQuotation((char) 0).setWriteLongAsString(true)));
        assertEquals("{5:x}",
                serializeEntryG014(longKeyType, 5L, JsonSerConfig.create().setStringQuotation((char) 0).setWriteLongAsString(true).setQuoteMapKey(false)));

        final Type stringKeyType = createType("Map.ImmutableEntry<String, String>");
        final String json = serializeEntryG014(stringKeyType, "'k'", JsonSerConfig.create().setStringQuotation('\'').setWriteLongAsString(true));
        assertEquals("{\"'k'\":'x'}", json);
        assertEquals("'k'", ((Map.Entry<String, String>) stringKeyType.valueOf(json)).getKey());
    }

    // ---- bug review 2026-09-27 verify G123 end ----

    // ---- bug review 2026-10-02 G114 begin ----

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String serializeRawEntryG114(final Type type, final Object key, final Object value, final JsonSerConfig config) throws IOException {
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, new AbstractMap.SimpleImmutableEntry<>(key, value), config);
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    // G114-03: a key declared as Object (a raw SimpleImmutableEntry property) was written by the value writer of its runtime handler, so the
    // map-key quoting rule was skipped: {5:"x"} under the default config (invalid JSON), {a b:1} under a null config.
    // It is now quoted by the rule of its runtime class, as the JSON map writer quotes the same key of a Map<Object, ..>.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSerializeTo_objectKeyFollowsMapKeyQuoting() throws IOException {
        final Type objectKeyType = createType("Map.ImmutableEntry<Object, Object>");
        final JsonSerConfig defaultConfig = JsonSerConfig.create();

        assertEquals("{\"5\":\"x\"}", serializeRawEntryG114(objectKeyType, 5, "x", defaultConfig));
        assertEquals("{\"true\":1}", serializeRawEntryG114(objectKeyType, true, 1, defaultConfig));
        assertEquals("{\"2020-09-13T12:26:40Z\":\"y\"}", serializeRawEntryG114(objectKeyType, new Date(1600000000123L), "y", defaultConfig));
        // the same text as a declared key type gives
        assertEquals(serializeRawEntryG114(createType("Map.ImmutableEntry<Integer, Object>"), 5, "x", defaultConfig),
                serializeRawEntryG114(objectKeyType, 5, "x", defaultConfig));
        // an entry held in an Object slot (list element) goes through the same handler
        assertEquals("[{\"5\":\"x\"}]", com.landawn.abacus.util.N.toJson(List.of(new AbstractMap.SimpleImmutableEntry<>(5, "x"))));

        // a String key stays quoted under a null config, so the text reads back
        final String json = serializeRawEntryG114(objectKeyType, "a b", 1, null);
        assertEquals("{\"a b\":1}", json);
        assertEquals("a b", ((Map.Entry<Object, Object>) objectKeyType.valueOf(json)).getKey());

        // quoteMapKey=false still leaves a number bare; a config-sensitive key keeps its configured text
        assertEquals("{5:\"x\"}", serializeRawEntryG114(objectKeyType, 5, "x", JsonSerConfig.create().setQuoteMapKey(false)));
        assertEquals("{\"5\":\"x\"}", serializeRawEntryG114(objectKeyType, 5L, "x", JsonSerConfig.create().setWriteLongAsString(true).setQuoteMapKey(false)));
        // a null key and an enum key are unchanged
        assertEquals("{\"null\":\"x\"}", serializeRawEntryG114(objectKeyType, null, "x", defaultConfig));
        assertEquals("{\"MONDAY\":\"x\"}", serializeRawEntryG114(objectKeyType, java.time.DayOfWeek.MONDAY, "x", defaultConfig));
    }

    // ---- bug review 2026-10-02 G114 end ----

    // ---- bug review 2026-10-02 verify G121 begin ----

    public static class G121RawImmutableEntryBean {
        @SuppressWarnings("rawtypes")
        private AbstractMap.SimpleImmutableEntry entry;

        @SuppressWarnings("rawtypes")
        public AbstractMap.SimpleImmutableEntry getEntry() {
            return entry;
        }

        @SuppressWarnings("rawtypes")
        public void setEntry(final AbstractMap.SimpleImmutableEntry entry) {
            this.entry = entry;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String serializeEntryG121(final Type type, final Object entry, final JsonXmlSerConfig<?> config) throws IOException {
        if (config instanceof com.landawn.abacus.parser.XmlSerConfig) {
            final com.landawn.abacus.util.BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();

            try {
                type.serializeTo(writer, entry, config);
                return writer.toString();
            } finally {
                Objectory.recycle(writer);
            }
        }

        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, entry, config);
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    // G114-03: an Object-typed scalar key is written exactly as the same key under its own declared key type, for every config
    // (null, default, quoteMapKey=false, writeLongAsString, single quotes, XML); a collection key keeps its structural form.
    @SuppressWarnings({ "rawtypes" })
    @Test
    public void testSerializeTo_objectKeyMatchesDeclaredKeyType() throws IOException {
        final Type objectKeyType = createType("Map.ImmutableEntry<Object, Object>");
        final Object[] keys = { 5, 5L, 1.5d, true, 'c', "a b", new java.math.BigDecimal("1E+3"), java.time.LocalDate.of(2020, 1, 2) };
        final List<JsonXmlSerConfig<?>> configs = java.util.Arrays.asList(null, JsonSerConfig.create(), JsonSerConfig.create().setQuoteMapKey(false),
                JsonSerConfig.create().setWriteLongAsString(true), JsonSerConfig.create().setStringQuotation('\'').setCharQuotation('\''),
                com.landawn.abacus.parser.XmlSerConfig.create());

        for (final Object key : keys) {
            final Type declaredKeyType = createType("Map.ImmutableEntry<" + Type.of(key.getClass()).name() + ", Object>");

            for (final JsonXmlSerConfig<?> config : configs) {
                assertEquals(serializeEntryG121(declaredKeyType, new AbstractMap.SimpleImmutableEntry<>(key, "x"), config),
                        serializeEntryG121(objectKeyType, new AbstractMap.SimpleImmutableEntry<>(key, "x"), config), key + " / " + config);
            }
        }

        assertEquals("{\"5\":x}",
                serializeEntryG121(objectKeyType, new AbstractMap.SimpleImmutableEntry<>(5, "x"), com.landawn.abacus.parser.XmlSerConfig.create()));
        // unchanged: a collection key is still written by the generic object handler
        assertEquals("{[1, 2]:\"x\"}", serializeEntryG121(objectKeyType, new AbstractMap.SimpleImmutableEntry<>(List.of(1, 2), "x"), JsonSerConfig.create()));
    }

    // G114-03: the original failing input - a raw SimpleImmutableEntry bean property was written as {"entry": {5:"x"}} (invalid JSON).
    // The key now reads back as the String "5", as a Map<Object, ..> key does; text written by the old code is still read as before.
    @Test
    public void testToJson_rawEntryPropertyKeyIsQuoted() {
        final G121RawImmutableEntryBean bean = new G121RawImmutableEntryBean();
        bean.setEntry(new AbstractMap.SimpleImmutableEntry<>(5, "x"));

        final String json = com.landawn.abacus.util.N.toJson(bean);
        assertEquals("{\"entry\": {\"5\":\"x\"}}", json);
        assertEquals("5", com.landawn.abacus.util.N.fromJson(json, G121RawImmutableEntryBean.class).getEntry().getKey());
        assertEquals("{\"entry\": {5:\"x\"}}", com.landawn.abacus.util.N.toJson(bean, JsonSerConfig.create().setQuoteMapKey(false)));

        assertEquals(5, com.landawn.abacus.util.N.fromJson("{\"entry\": {5:\"x\"}}", G121RawImmutableEntryBean.class).getEntry().getKey());
    }

    // ---- bug review 2026-10-02 verify G121 end ----

    // ---- bug review 2026-10-02 final G128 begin ----

    // G114-03 also covers a declared key type served by the generic object handler (Serializable, Comparable): its scalar key
    // is quoted by the rule of its runtime class, exactly as an Object-typed key; a key type with its own handler is unchanged.
    @SuppressWarnings({ "rawtypes" })
    @Test
    public void testSerializeTo_genericObjectHandlerKeyFollowsMapKeyQuoting() throws IOException {
        final JsonSerConfig defaultConfig = JsonSerConfig.create();
        final Type objectKeyType = createType("Map.ImmutableEntry<Object, Object>");

        for (final String keyTypeName : new String[] { "java.io.Serializable", "Comparable" }) {
            final Type keyType = createType("Map.ImmutableEntry<" + keyTypeName + ", Object>");

            assertEquals("{\"5\":\"x\"}", serializeRawEntryG114(keyType, 5, "x", defaultConfig), keyTypeName);
            assertEquals(serializeRawEntryG114(objectKeyType, true, 1, defaultConfig), serializeRawEntryG114(keyType, true, 1, defaultConfig), keyTypeName);
            assertEquals("{5:\"x\"}", serializeRawEntryG114(keyType, 5, "x", JsonSerConfig.create().setQuoteMapKey(false)), keyTypeName);
            assertEquals("{\"a b\":1}", serializeRawEntryG114(keyType, "a b", 1, null), keyTypeName);
        }

        // a declared Number key keeps its own handler (unchanged): the same quoted text as before
        assertEquals("{\"5\":\"x\"}", serializeRawEntryG114(createType("Map.ImmutableEntry<Number, Object>"), 5, "x", defaultConfig));
    }

    // ---- bug review 2026-10-02 final G128 end ----
}
