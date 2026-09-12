package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple1;

public class TupleTypeTest extends TestBase {

    private Tuple1Type<String> tuple1Type;
    private Tuple1<String> testTuple1;

    @BeforeEach
    public void setUp() {
        tuple1Type = (Tuple1Type<String>) createType("Tuple1<String>");
        testTuple1 = Tuple.of("test");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tuple1Type.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Tuple1"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tuple1Type.javaType();
        assertNotNull(clazz);
        assertEquals(Tuple1.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = tuple1Type.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tuple1Type.isParameterizedType());
    }

    @Test
    public void testStringOf() {
        String result = tuple1Type.stringOf(testTuple1);
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    public void testStringOfNull() {
        String result = tuple1Type.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\"]";
        Tuple1<String> result = tuple1Type.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result._1);
    }

    @Test
    public void testValueOfEmptyString() {
        Tuple1<String> result = tuple1Type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Tuple1<String> result = tuple1Type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tuple1Type.appendTo(writer, testTuple1);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tuple1Type.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tuple1Type.appendTo(sb, testTuple1);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testSerializeTo() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = null;

        tuple1Type.serializeTo(writer, testTuple1, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testSerializeToNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = null;

        tuple1Type.serializeTo(writer, null, config);

        verify(writer).write(any(char[].class));
    }
    // The wrapper/tuple-shaped handlers that route every slot through AbstractTupleType.serializeSlot /
    // AbstractTupleType.appendElement. Each name is paired with a factory that wraps a single payload value.
    private static final String[] SLOT_WRAPPERS = { "Nullable<Object>", "Optional<Object>", "JdkOptional<Object>", "Holder<Object>", "Pair<Object, Object>",
            "Triple<Object, Object, Object>", "Tuple1<Object>", "Tuple2<Object, Object>", "Tuple3<Object, Object, Object>", "Indexed<Object>",
            "Timed<Object>" };

    private static Object wrap(final String wrapper, final Object value) {
        if (wrapper.startsWith("Nullable")) {
            return com.landawn.abacus.util.u.Nullable.of(value);
        } else if (wrapper.startsWith("JdkOptional")) {
            return java.util.Optional.of(value);
        } else if (wrapper.startsWith("Optional")) {
            return com.landawn.abacus.util.u.Optional.of(value);
        } else if (wrapper.startsWith("Holder")) {
            return com.landawn.abacus.util.Holder.of(value);
        } else if (wrapper.startsWith("Pair")) {
            return com.landawn.abacus.util.Pair.of(value, 1);
        } else if (wrapper.startsWith("Triple")) {
            return com.landawn.abacus.util.Triple.of(value, 1, 2);
        } else if (wrapper.startsWith("Tuple1")) {
            return Tuple.of(value);
        } else if (wrapper.startsWith("Tuple2")) {
            return Tuple.of(value, 1);
        } else if (wrapper.startsWith("Tuple3")) {
            return Tuple.of(value, 1, 2);
        } else if (wrapper.startsWith("Indexed")) {
            return com.landawn.abacus.util.Indexed.of(value, 3);
        }

        return com.landawn.abacus.util.Timed.of(value, 3L);
    }

    // R04-5 (2026-09-08): CollectionType.serializeTo and ObjectArrayType.serializeTo were given an explicit
    // "writer instanceof BufferedJsonWriter" guard, because a JsonSerConfig can still arrive on an XML or CSV
    // writer (Type.serializeTo is public API) and the parser would then write raw JSON - unescaped ", < and & -
    // into that format. AbstractTupleType.serializeSlot, the shared slot writer the single-slot and tuple-shaped
    // handlers were routed through in the same pass, handed the writer to the parser unconditionally and so
    // produced unparseable XML/CSV for every one of them.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void reviewFixes20260908_aJsonConfigOnANonJsonWriterIsEscaped() throws IOException {
        final java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("k", "a<&\"");

        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();
        final com.landawn.abacus.parser.XmlSerConfig xsc = com.landawn.abacus.parser.XmlSerConfig.create();

        // what CollectionType - already guarded - writes for the very same embedded map
        final String guardedElement = serializeToXml(Type.of("List<Object>"), com.landawn.abacus.util.N.asList((Object) payload), jsc);
        assertEquals("[" + ESCAPED_SLOT + "]", guardedElement);

        for (final String wrapper : SLOT_WRAPPERS) {
            final Type type = Type.of(wrapper);
            final Object value = wrap(wrapper, payload);

            final String onXml = serializeToXml(type, value, jsc);
            assertFalse(onXml.contains("\""), wrapper + " wrote a raw quote onto an XML writer: " + onXml);
            assertFalse(onXml.contains("<"), wrapper + " wrote a raw '<' onto an XML writer: " + onXml);
            assertTrue(onXml.contains(ESCAPED_SLOT), wrapper + " -> " + onXml);

            // and it is exactly what the same handler writes under an XmlSerConfig, which already escaped
            assertEquals(serializeToXml(type, value, xsc), onXml, wrapper);

            // a CSV writer gets the CSV escaping (quotes doubled) instead of raw JSON
            final String onCsv = serializeToCsv(type, value, jsc);
            assertTrue(onCsv.contains("\"\"k\"\""), wrapper + " wrote raw JSON onto a CSV writer: " + onCsv);

            // the JSON writer is untouched: the structure is still written straight through, not as a string
            assertTrue(serializeToJson(type, value, jsc).contains("{\"k\": \"a<&\\\"\"}"), wrapper);
        }
    }

    // R05-2 (2026-09-08): the "an Object slot has no usable declared handler" dispatch that OptionalType,
    // NullableType, JdkOptionalType and HolderType were given in appendTo applies verbatim to every tuple-shaped
    // handler - their serializeTo already dispatches on the runtime class through serializeSlot - and none of
    // them had it, so a Pair<Object, Object> holding a map appended ObjectType's JSON stringOf form
    // ({"k": 1}) where the bare map, and the four single-slot wrappers, append {k:1}.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void reviewFixes20260908_appendToObjectSlotUsesRuntimeType() throws IOException {
        final java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("k", 1);
        map.put("s", "v");
        final List<Object> list = new java.util.ArrayList<>(java.util.Arrays.asList(1, "a"));

        for (final String wrapper : SLOT_WRAPPERS) {
            final Type type = Type.of(wrapper);

            // an Object slot appends exactly what the bare value's own handler appends
            for (final Object value : new Object[] { map, list, new int[] { 1, 2 }, 7, 1.5d, true, "q" }) {
                final String bare = appendToString(Type.of(value.getClass()), value);

                assertTrue(appendToString(type, wrap(wrapper, value)).contains(bare), wrapper + " / " + value);
            }
        }

        // the exact shapes, so a future change to the shared helper cannot pass by appending nothing at all
        assertEquals("[{k:1, s:v}, 1]", appendToString(Type.of("Pair<Object, Object>"), com.landawn.abacus.util.Pair.of(map, 1)));
        assertEquals("[{k:1, s:v}, 1, 2]", appendToString(Type.of("Triple<Object, Object, Object>"), com.landawn.abacus.util.Triple.of(map, 1, 2)));
        assertEquals("[{k:1, s:v}, 1]", appendToString(Type.of("Tuple2<Object, Object>"), Tuple.of(map, 1)));
        assertEquals("[3, {k:1, s:v}]", appendToString(Type.of("Indexed<Object>"), com.landawn.abacus.util.Indexed.of(map, 3)));
        assertEquals("[3, {k:1, s:v}]", appendToString(Type.of("Timed<Object>"), com.landawn.abacus.util.Timed.of(map, 3L)));

        // the Writer branch of appendTo (Pair/Triple/Timed/TupleN buffer through a Writer) resolves the same way
        assertEquals("[{k:1, s:v}, 1]", appendToWriter(Type.of("Pair<Object, Object>"), com.landawn.abacus.util.Pair.of(map, 1)));
        assertEquals("[{k:1, s:v}, 1, 2]", appendToWriter(Type.of("Triple<Object, Object, Object>"), com.landawn.abacus.util.Triple.of(map, 1, 2)));
        assertEquals("[{k:1, s:v}, 1]", appendToWriter(Type.of("Tuple2<Object, Object>"), Tuple.of(map, 1)));
        assertEquals("[3, {k:1, s:v}]", appendToWriter(Type.of("Timed<Object>"), com.landawn.abacus.util.Timed.of(map, 3L)));

        // a declared (non-Object) element type keeps its own handler, and a null slot still writes the literal
        assertEquals("[{k:1, s:v}, 1]", appendToString(Type.of("Pair<Map<String, Object>, Object>"), com.landawn.abacus.util.Pair.of(map, 1)));
        assertEquals("[null, 1]", appendToString(Type.of("Pair<Object, Object>"), com.landawn.abacus.util.Pair.of(null, 1)));
        assertEquals("[3, null]", appendToString(Type.of("Timed<Object>"), com.landawn.abacus.util.Timed.of(null, 3L)));
    }

    private static final String ESCAPED_SLOT = "{&quot;k&quot;: &quot;a&lt;&amp;\\&quot;&quot;}";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String serializeToXml(final Type type, final Object value, final JsonXmlSerConfig<?> config) throws IOException {
        final com.landawn.abacus.util.BufferedXmlWriter writer = com.landawn.abacus.util.Objectory.createBufferedXmlWriter();

        try {
            type.serializeTo(writer, value, config);
            return writer.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(writer);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String serializeToCsv(final Type type, final Object value, final JsonXmlSerConfig<?> config) throws IOException {
        final com.landawn.abacus.util.BufferedCsvWriter writer = com.landawn.abacus.util.Objectory.createBufferedCsvWriter();

        try {
            type.serializeTo(writer, value, config);
            return writer.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(writer);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String serializeToJson(final Type type, final Object value, final JsonXmlSerConfig<?> config) throws IOException {
        final com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, value, config);
            return writer.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(writer);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String appendToString(final Type type, final Object value) throws IOException {
        final StringBuilder sb = new StringBuilder();
        type.appendTo(sb, value);
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String appendToWriter(final Type type, final Object value) throws IOException {
        final StringWriter sw = new StringWriter();
        type.appendTo(sw, value);
        return sw.toString();
    }
}
