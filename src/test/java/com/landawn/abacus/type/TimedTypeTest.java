package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Timed;

public class TimedTypeTest extends TestBase {

    private TimedType<String> timedType;

    @BeforeEach
    public void setUp() {
        timedType = (TimedType<String>) createType("Timed<String>");
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(timedType.declaringName());
        assertTrue(timedType.declaringName().contains("Timed"));
    }

    @Test
    public void testClazz() {
        assertEquals(Timed.class, timedType.javaType());
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = timedType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(timedType.isParameterizedType());
    }

    @Test
    public void testStringOf() {
        Timed<String> timed = Timed.of("test", 123456789L);
        String result = timedType.stringOf(timed);
        assertNotNull(result);
        assertTrue(result.contains("123456789"));
        assertTrue(result.contains("test"));

        assertNull(timedType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        String json = "[123456789, \"test\"]";
        Timed<String> result = timedType.valueOf(json);
        assertNotNull(result);
        assertEquals(123456789L, result.timestamp());
        assertEquals("test", result.value());

        assertNull(timedType.valueOf(null));
        assertNull(timedType.valueOf(""));
        assertThrows(IllegalArgumentException.class, () -> timedType.valueOf(" "));
        assertThrows(IllegalArgumentException.class, () -> timedType.valueOf("[123456789]"));
        assertThrows(IllegalArgumentException.class, () -> timedType.valueOf("[123456789,\"test\",\"unexpected\"]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValueOfParameterizedSlotConvertsElements() {
        TimedType<List<Long>> type = (TimedType<List<Long>>) createType("Timed<List<Long>>");

        Timed<List<Long>> result = type.valueOf("[123456789,[1,2]]");

        assertEquals(123456789L, result.timestamp());
        Long first = result.value().get(0);
        assertEquals(1L, first);
        assertEquals(2L, result.value().get(1));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        Timed<String> timed = Timed.of("test", 123456789L);
        timedType.appendTo(writer, timed);
        String result = writer.toString();
        assertTrue(result.contains("123456789"));
        assertTrue(result.contains("test"));

        writer = new StringWriter();
        timedType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToPreservesCheckedFailureAndSuppressesCleanupFailure() {
        final IOException writeFailure = new IOException("write failure");
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

        final IOException thrown = assertThrows(IOException.class, () -> timedType.appendTo(writer, Timed.of("test", 1L)));

        assertSame(writeFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
    }

    @Test
    public void testSerializeTo() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        Timed<String> timed = Timed.of("test", 123456789L);
        timedType.serializeTo(writer, timed, config);

        timedType.serializeTo(writer, null, config);
        assertNotNull(timed);
    }

    @Test
    public void testSerializeToPropagatesCheckedIOException() throws IOException {
        final IOException failure = new IOException("write failure");
        final CharacterWriter writer = createCharacterWriter();
        doThrow(failure).when(writer).write(anyChar());

        assertSame(failure, assertThrows(IOException.class, () -> timedType.serializeTo(writer, Timed.of("test", 1L), null)));
    }

    @Test
    public void testGetTypeName() {
        String typeName = TimedType.getTypeName("String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Timed"));
        assertTrue(typeName.contains("String"));

        String declaringName = TimedType.getTypeName("String", true);
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Timed"));
        assertTrue(declaringName.contains("String"));
    }

    @SuppressWarnings("unchecked")
    private static String reviewFixes20260906_ser(final Type<?> type, final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config) throws java.io.IOException {
        final com.landawn.abacus.util.BufferedJsonWriter jsonWriter = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            ((Type<Object>) type).serializeTo(jsonWriter, value, config);
            return jsonWriter.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(jsonWriter);
        }
    }

    // T6-04 (2026-09-06): the timestamp slot ignored writeLongAsString.
    @Test
    public void reviewFixes20260906_timestampSlotHonoursWriteLongAsString() throws IOException {
        final com.landawn.abacus.parser.JsonSerConfig las = com.landawn.abacus.parser.JsonSerConfig.create().setWriteLongAsString(true);

        assertEquals("[\"5\", \"v\"]", reviewFixes20260906_ser(timedType, Timed.of("v", 5L), las));
        assertEquals("[5, \"v\"]", reviewFixes20260906_ser(timedType, Timed.of("v", 5L), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("[5, v]", reviewFixes20260906_ser(timedType, Timed.of("v", 5L), null));
        assertEquals("[5, v]", reviewFixes20260906_ser(timedType, Timed.of("v", 5L), com.landawn.abacus.parser.XmlSerConfig.create().setWriteLongAsString(true)));
        assertEquals("[\"-9223372036854775808\", \"v\"]", reviewFixes20260906_ser(timedType, Timed.of("v", Long.MIN_VALUE), las));
        assertEquals("[\"9223372036854775807\", \"v\"]", reviewFixes20260906_ser(timedType, Timed.of("v", Long.MAX_VALUE), las));
        assertEquals("[\"7\", \"5\"]", reviewFixes20260906_ser(Type.of("Timed<Long>"), Timed.of(5L, 7L), las));
        assertEquals("[7, 5]", reviewFixes20260906_ser(Type.of("Timed<Long>"), Timed.of(5L, 7L), null));
        assertEquals("null", reviewFixes20260906_ser(timedType, null, las));

        // the quoted form round-trips through valueOf and through the real parser into a bean field
        final Timed<String> back = timedType.valueOf(reviewFixes20260906_ser(timedType, Timed.of("v", Long.MIN_VALUE), las));
        assertEquals(Long.MIN_VALUE, back.timestamp());
        assertEquals("v", back.value());
        final String json = com.landawn.abacus.util.N.toJson(new ReviewFixesTimedBean(), las);
        assertEquals("{\"tm\": [\"9223372036854775807\", \"v\"]}", json);
        assertEquals(Long.MAX_VALUE, com.landawn.abacus.util.N.fromJson(json, ReviewFixesTimedBean.class).tm.timestamp());
        assertEquals("[[\"7\", \"5\"]]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Timed.of(5L, 7L)), las));
    }

    // T6-01 (2026-09-06): an Object value slot dispatches on the runtime class.
    @Test
    public void reviewFixes20260906_objectValueSlotUsesRuntimeType() throws IOException {
        final Type<?> type = Type.of("Timed<Object>");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        assertEquals("[5, 1]", reviewFixes20260906_ser(type, Timed.of(1, 5L), jsc));
        assertEquals("[5, [1]]", reviewFixes20260906_ser(type, Timed.of(com.landawn.abacus.util.N.asList(1), 5L), jsc));
        assertEquals("[5, {\"k\": 1}]", reviewFixes20260906_ser(type, Timed.of(com.landawn.abacus.util.N.asMap("k", 1), 5L), jsc));
        assertEquals("[5, \"s\"]", reviewFixes20260906_ser(type, Timed.of("s", 5L), jsc));
        assertEquals("[5, null]", reviewFixes20260906_ser(type, Timed.of(null, 5L), jsc));
        assertEquals("[[5, 1]]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Timed.of(1, 5L))));
    }

    // T6-07 (2026-09-06): documented exception types of valueOf; negative timestamps are accepted.
    @Test
    public void reviewFixes20260906_valueOfExceptionTypes() {
        assertNull(timedType.valueOf(""));
        assertEquals(-1L, timedType.valueOf("[-1, \"a\"]").timestamp());
        assertThrows(IllegalArgumentException.class, () -> timedType.valueOf(" "));
        assertThrows(IllegalArgumentException.class, () -> timedType.valueOf("[1]"));
        assertThrows(NumberFormatException.class, () -> timedType.valueOf("[1.5, \"a\"]"));
        assertThrows(NumberFormatException.class, () -> timedType.valueOf("[1e2, \"a\"]"));
        assertThrows(ArithmeticException.class, () -> timedType.valueOf("[9223372036854775808, \"a\"]"));
        assertEquals(5L, timedType.valueOf("[\"5\", \"a\"]").timestamp());
    }

    public static class ReviewFixesTimedBean {
        public Timed<String> tm = Timed.of("v", Long.MAX_VALUE);
    }
}
