package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
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
import com.landawn.abacus.util.Pair;

public class PairTypeTest extends TestBase {

    private PairType<String, Integer> stringIntPairType;
    private PairType<Double, Boolean> doubleBoolPairType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        stringIntPairType = (PairType<String, Integer>) createType("Pair<String, Integer>");
        doubleBoolPairType = (PairType<Double, Boolean>) createType("Pair<Double, Boolean>");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(stringIntPairType.declaringName());
        assertTrue(stringIntPairType.declaringName().contains("Pair"));
        assertTrue(stringIntPairType.declaringName().contains("String"));
        assertTrue(stringIntPairType.declaringName().contains("Integer"));
    }

    @Test
    public void testClazz() {
        assertEquals(Pair.class, stringIntPairType.javaType());
        assertEquals(Pair.class, doubleBoolPairType.javaType());
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = stringIntPairType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.size());
        assertEquals("String", paramTypes.get(0).name());
        assertEquals("Integer", paramTypes.get(1).name());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(stringIntPairType.isParameterizedType());
        assertTrue(doubleBoolPairType.isParameterizedType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(stringIntPairType.stringOf(null));
    }

    @Test
    public void testStringOfWithPair() {
        Pair<String, Integer> pair = Pair.of("test", 123);
        String result = stringIntPairType.stringOf(pair);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void testStringOfWithNullElements() {
        Pair<String, Integer> pair = Pair.of(null, null);
        String result = stringIntPairType.stringOf(pair);
        assertNotNull(result);
        assertTrue(result.contains("null"));
    }

    @Test
    public void testComplexPairTypes() {
        PairType<Object, Object> objectPairType = (PairType<Object, Object>) createType("Pair<Object, Object>");
        Pair<Object, Object> pair = Pair.of("string", 123);
        String result = objectPairType.stringOf(pair);
        assertNotNull(result);

        Pair<Object, Object> parsed = objectPairType.valueOf(result);
        assertNotNull(parsed);
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(stringIntPairType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        assertNull(stringIntPairType.valueOf(""));
    }

    @Test
    public void testValueOfWithValidJsonArray() {
        String json = "[\"hello\", 42]";
        Pair<String, Integer> result = stringIntPairType.valueOf(json);
        assertNotNull(result);
        assertEquals("hello", result.left());
        assertEquals(Integer.valueOf(42), result.right());
    }

    @Test
    public void testValueOfWithNullElements() {
        String json = "[null, null]";
        Pair<String, Integer> result = stringIntPairType.valueOf(json);
        assertNotNull(result);
        assertNull(result.left());
        assertNull(result.right());
    }

    @Test
    public void testValueOfWithTypeConversion() {
        String json = "[\"123\", \"456\"]";
        Pair<String, Integer> result = stringIntPairType.valueOf(json);
        assertNotNull(result);
        assertEquals("123", result.left());
        assertEquals(Integer.valueOf(456), result.right());
    }

    @Test
    public void testValueOfRequiresExactlyTwoElements() {
        assertThrows(IllegalArgumentException.class, () -> stringIntPairType.valueOf("[\"only-left\"]"));
        assertThrows(IllegalArgumentException.class, () -> stringIntPairType.valueOf("[\"left\", 1, \"unexpected\"]"));
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        stringIntPairType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithPair() throws IOException {
        StringBuilder sb = new StringBuilder();
        Pair<String, Integer> pair = Pair.of("test", 123);
        stringIntPairType.appendTo(sb, pair);
        String result = sb.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void testAppendToWithWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        Pair<String, Integer> pair = Pair.of("abc", 789);
        stringIntPairType.appendTo(stringWriter, pair);
        String result = stringWriter.toString();
        assertTrue(result.contains("abc"));
        assertTrue(result.contains("789"));
    }

    @Test
    public void testAppendToPreservesCheckedIOException() {
        final IOException failure = new IOException("write failed");
        Writer failingWriter = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw failure;
            }

            @Override
            public void flush() {
                // No-op.
            }

            @Override
            public void close() {
                // No-op.
            }
        };

        assertSame(failure, org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> stringIntPairType.appendTo(failingWriter, Pair.of("a", 1))));
    }

    @Test
    public void testAppendToWithNullElements() throws IOException {
        StringBuilder sb = new StringBuilder();
        Pair<String, Integer> pair = Pair.of(null, null);
        stringIntPairType.appendTo(sb, pair);
        String result = sb.toString();
        assertTrue(result.contains("null"));
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        stringIntPairType.serializeTo(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithPair() throws IOException {
        Pair<String, Integer> pair = Pair.of("test", 456);
        stringIntPairType.serializeTo(writer, pair, config);
        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testSerializeToPreservesCheckedIOException() throws IOException {
        com.landawn.abacus.util.BufferedJsonWriter failingWriter = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        failingWriter.close();

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> stringIntPairType.serializeTo(failingWriter, Pair.of("a", 1), config));
    }

    @Test
    public void testSerializeToWithNullElements() throws IOException {
        Pair<String, Integer> pair = Pair.of(null, null);
        stringIntPairType.serializeTo(writer, pair, config);
        verify(writer, atLeastOnce()).write(any(char[].class));
    }

    @Test
    public void testGetTypeName() {
        PairType<Long, String> longStringPairType = (PairType<Long, String>) createType("Pair<Long, String>");
        assertNotNull(longStringPairType);
        assertTrue(longStringPairType.name().contains("Long"));
        assertTrue(longStringPairType.name().contains("String"));
    }

    @Test
    public void testAppendTo_unquotedToStringForm() throws IOException {
        StringBuilder sb = new StringBuilder();
        stringIntPairType.appendTo(sb, Pair.of("a", 1));
        // appendTo emits the plain, toString()-style form: the String element is NOT quoted
        assertEquals("[a, 1]", sb.toString());
    }

    @Test
    public void testSerializeTo_jsonQuotedForm() throws IOException {
        Pair<String, Integer> pair = Pair.of("a", 1);
        com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        stringIntPairType.serializeTo(writer, pair, com.landawn.abacus.parser.JsonSerConfig.create());
        String json = writer.toString();
        com.landawn.abacus.util.Objectory.recycle(writer);

        // serializeTo emits JSON: the String element IS quoted; equals stringOf and differs from appendTo
        assertEquals("[\"a\", 1]", json);
        assertEquals(stringIntPairType.stringOf(pair), json);

        StringBuilder sb = new StringBuilder();
        stringIntPairType.appendTo(sb, pair);
        org.junit.jupiter.api.Assertions.assertNotEquals(sb.toString(), json);
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testValueOfParameterizedSlotConvertsElements() {
        // regression: the raw-assignability shortcut kept parser-default element types (Integer,
        // LinkedHashMap, ...) for parameterized slots like List<Long>, breaking the round trip
        final Type<com.landawn.abacus.util.Pair<java.util.List<Long>, String>> type = TypeFactory.getType("Pair<List<Long>, String>");

        final com.landawn.abacus.util.Pair<java.util.List<Long>, String> p = type
                .valueOf(type.stringOf(com.landawn.abacus.util.Pair.of(com.landawn.abacus.util.N.asList(1L, 2L), "x")));

        org.junit.jupiter.api.Assertions.assertEquals(Long.class, p.left().get(0).getClass());
        org.junit.jupiter.api.Assertions.assertEquals(com.landawn.abacus.util.N.asList(1L, 2L), p.left());
        org.junit.jupiter.api.Assertions.assertEquals("x", p.right());
    }

    @SuppressWarnings("unchecked")
    private static String reviewFixes20260906_ser(final Type<?> type, final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config)
            throws java.io.IOException {
        final com.landawn.abacus.util.BufferedJsonWriter jsonWriter = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            ((Type<Object>) type).serializeTo(jsonWriter, value, config);
            return jsonWriter.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(jsonWriter);
        }
    }

    // T6-01 (2026-09-06): Object slots dispatch on the runtime class; non-serializable handlers write embedded JSON.
    @SuppressWarnings("unchecked")
    @Test
    public void reviewFixes20260906_objectSlotsUseRuntimeTypeAndEmbeddedJson() throws IOException {
        final Type<Object> type = (Type<Object>) createType("Pair<Object, Object>");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        assertEquals("[1, \"a\"]", reviewFixes20260906_ser(type, Pair.of(1, "a"), jsc));
        assertEquals("[true, [2]]", reviewFixes20260906_ser(type, Pair.of(true, com.landawn.abacus.util.N.asList(2)), jsc));
        assertEquals("[{\"k\": 1}, null]", reviewFixes20260906_ser(type, Pair.of(com.landawn.abacus.util.N.asMap("k", 1), null), jsc));
        assertEquals("[[1, 2], 3]", reviewFixes20260906_ser(type, Pair.of(Pair.of(1, 2), 3), jsc));
        assertEquals("[1, a]", reviewFixes20260906_ser(type, Pair.of(1, "a"), null));
        assertEquals(type.stringOf(Pair.of(1, com.landawn.abacus.util.N.asList(2))),
                reviewFixes20260906_ser(type, Pair.of(1, com.landawn.abacus.util.N.asList(2)), jsc));
        assertEquals("null", reviewFixes20260906_ser(type, null, jsc));
        // declared slots keep their declared handler; a declared null slot honours the handler's null flag
        assertEquals("[\"a\", 0]", reviewFixes20260906_ser(stringIntPairType, Pair.of("a", null),
                com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true)));
        assertEquals("[\"a\", null]", reviewFixes20260906_ser(stringIntPairType, Pair.of("a", null), jsc));
        assertEquals("[{\"k\": 1}, 2]",
                reviewFixes20260906_ser(createType("Pair<Map<String, Integer>, Integer>"), Pair.of(com.landawn.abacus.util.N.asMap("k", 1), 2), jsc));
        // real parser
        assertEquals("[[1, 2]]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Pair.of(1, 2))));
        assertEquals("{\"p\": [1, [2]]}",
                com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asMap("p", Pair.of(1, com.landawn.abacus.util.N.asList(2)))));
    }

    // T6-08 (2026-09-06): documented exception types of valueOf.
    @Test
    public void reviewFixes20260906_valueOfExceptionTypes() {
        assertNull(stringIntPairType.valueOf(""));
        assertNull(stringIntPairType.valueOf((String) null));
        assertThrows(IllegalArgumentException.class, () -> stringIntPairType.valueOf(" "));
        assertThrows(IllegalArgumentException.class, () -> stringIntPairType.valueOf("[\"a\"]"));
        assertThrows(IllegalArgumentException.class, () -> stringIntPairType.valueOf("[\"a\", 1, 2]"));
        // Unquoted numeric payloads use JSON numeric conversion; quoted values use the declared text handler.
        assertEquals(Pair.of("a", 1), stringIntPairType.valueOf("[\"a\", 1.0]"));
        assertEquals(Pair.of("\u6c49\ud83d\ude42", -1), stringIntPairType.valueOf("[\"\u6c49\ud83d\ude42\", -1.5]"));
        assertEquals(Pair.of("", Integer.MAX_VALUE), stringIntPairType.valueOf("[\"\", 2147483647.9]"));
        assertThrows(ArithmeticException.class, () -> stringIntPairType.valueOf("[null, 2147483648]"));
        assertThrows(NumberFormatException.class, () -> stringIntPairType.valueOf("[\"a\", \"1.5\"]"));
        assertThrows(NumberFormatException.class, () -> stringIntPairType.valueOf("[\"a\", \"bad\"]"));
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> createType("Pair<Integer, List<Integer>>").valueOf("[1, [2\"]\", 3]]"));
    }
}
