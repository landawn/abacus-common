package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Indexed;

public class IndexedTypeTest extends TestBase {

    private IndexedType<String> indexedType;
    private CharacterWriter characterWriter;

    @Mock
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        indexedType = (IndexedType<String>) createType("Indexed<String>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = indexedType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Indexed"));
        assertTrue(declaringName.contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Indexed.class, indexedType.javaType());
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = indexedType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(indexedType.isParameterizedType());
    }

    @Test
    public void testStringOf() {
        assertNull(indexedType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(indexedType.valueOf(null));
        assertNull(indexedType.valueOf(""));

    }

    @Test
    public void testValueOfRequiresExactlyTwoElements() {
        assertThrows(IllegalArgumentException.class, () -> indexedType.valueOf("[5]"));
        assertThrows(IllegalArgumentException.class, () -> indexedType.valueOf("[5, \"value\", \"unexpected\"]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValueOfParameterizedSlotConvertsElements() {
        IndexedType<List<Long>> type = (IndexedType<List<Long>>) createType("Indexed<List<Long>>");

        Indexed<List<Long>> result = type.valueOf("[5,[1,2]]");

        assertEquals(5, result.index());
        Long first = result.value().get(0);
        assertEquals(1L, first);
        assertEquals(2L, result.value().get(1));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        indexedType.appendTo(writer, null);
        assertEquals("null", writer.toString());

        writer = new StringWriter();
        Indexed<String> indexed = Indexed.of("value", 5);
        indexedType.appendTo(writer, indexed);
        String result = writer.toString();
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
        assertTrue(result.contains("5"));
    }

    @Test
    public void testSerializeTo() throws IOException {
        indexedType.serializeTo(characterWriter, null, config);

        Indexed<String> indexed = Indexed.of("value", 10);
        indexedType.serializeTo(characterWriter, indexed, config);
        assertNotNull(indexed);
    }

    @Test
    public void testGetTypeName() {
        String typeName = IndexedType.getTypeName("String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Indexed"));
        assertTrue(typeName.contains("String"));

        typeName = IndexedType.getTypeName("String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Indexed"));
        assertTrue(typeName.contains("String"));
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

    // T6-04 (2026-09-06): the index slot ignored writeLongAsString.
    @Test
    public void reviewFixes20260906_indexSlotHonoursWriteLongAsString() throws IOException {
        final com.landawn.abacus.parser.JsonSerConfig las = com.landawn.abacus.parser.JsonSerConfig.create().setWriteLongAsString(true);

        assertEquals("[\"7\", \"v\"]", reviewFixes20260906_ser(indexedType, Indexed.of("v", 7L), las));
        assertEquals("[\"0\", \"v\"]", reviewFixes20260906_ser(indexedType, Indexed.of("v", 0L), las));
        assertEquals("[7, \"v\"]", reviewFixes20260906_ser(indexedType, Indexed.of("v", 7L), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("[7, v]", reviewFixes20260906_ser(indexedType, Indexed.of("v", 7L), null));
        assertEquals("[7, v]",
                reviewFixes20260906_ser(indexedType, Indexed.of("v", 7L), com.landawn.abacus.parser.XmlSerConfig.create().setWriteLongAsString(true)));
        assertEquals("[\"9223372036854775807\", \"v\"]", reviewFixes20260906_ser(indexedType, Indexed.of("v", Long.MAX_VALUE), las));
        assertEquals("[9223372036854775807, \"v\"]",
                reviewFixes20260906_ser(indexedType, Indexed.of("v", Long.MAX_VALUE), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("[\"7\", \"5\"]", reviewFixes20260906_ser(Type.of("Indexed<Long>"), Indexed.of(5L, 7L), las));
        assertEquals("null", reviewFixes20260906_ser(indexedType, null, las));

        // the quoted form round-trips through valueOf and through the real parser into a bean field
        final Indexed<String> back = indexedType.valueOf(reviewFixes20260906_ser(indexedType, Indexed.of("v", Long.MAX_VALUE), las));
        assertEquals(Long.MAX_VALUE, back.longIndex());
        assertEquals("v", back.value());
        final String json = com.landawn.abacus.util.N.toJson(new ReviewFixesIndexedBean(), las);
        assertEquals("{\"ix\": [\"9223372036854775807\", \"v\"]}", json);
        assertEquals(Long.MAX_VALUE, com.landawn.abacus.util.N.fromJson(json, ReviewFixesIndexedBean.class).ix.longIndex());
        assertEquals("{\"ix\": [9223372036854775807, \"v\"]}", com.landawn.abacus.util.N.toJson(new ReviewFixesIndexedBean()));
    }

    // T6-01 (2026-09-06): an Object value slot dispatches on the runtime class.
    @Test
    public void reviewFixes20260906_objectValueSlotUsesRuntimeType() throws IOException {
        final Type<?> type = Type.of("Indexed<Object>");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        assertEquals("[5, 1]", reviewFixes20260906_ser(type, Indexed.of(1, 5L), jsc));
        assertEquals("[5, [1]]", reviewFixes20260906_ser(type, Indexed.of(com.landawn.abacus.util.N.asList(1), 5L), jsc));
        assertEquals("[5, {\"k\": 1}]", reviewFixes20260906_ser(type, Indexed.of(com.landawn.abacus.util.N.asMap("k", 1), 5L), jsc));
        assertEquals("[5, \"s\"]", reviewFixes20260906_ser(type, Indexed.of("s", 5L), jsc));
        assertEquals("[5, null]", reviewFixes20260906_ser(type, Indexed.of(null, 5L), jsc));
        assertEquals("[[5, 1]]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Indexed.of(1, 5L))));
    }

    // T6-07 (2026-09-06): documented exception types of valueOf.
    @Test
    public void reviewFixes20260906_valueOfExceptionTypes() {
        assertNull(indexedType.valueOf(""));
        assertThrows(IllegalArgumentException.class, () -> indexedType.valueOf("[-1, \"a\"]"));
        assertThrows(IllegalArgumentException.class, () -> indexedType.valueOf(" "));
        assertThrows(IllegalArgumentException.class, () -> indexedType.valueOf("[1]"));
        assertThrows(NumberFormatException.class, () -> indexedType.valueOf("[1.5, \"a\"]"));
        assertThrows(NumberFormatException.class, () -> indexedType.valueOf("[1e2, \"a\"]"));
        assertThrows(ArithmeticException.class, () -> indexedType.valueOf("[9223372036854775808, \"a\"]"));
        assertEquals(5L, indexedType.valueOf("[\"5\", \"a\"]").longIndex());
        assertEquals(0L, indexedType.valueOf("[null, \"a\"]").longIndex());
    }

    public static class ReviewFixesIndexedBean {
        public Indexed<String> ix = Indexed.of("v", Long.MAX_VALUE);
    }
}
