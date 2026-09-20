package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.ParserUtil.XmlEmbeddedJsonConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.u.Nullable;

public class NullableTypeTest extends TestBase {

    @ParameterizedTest
    @ValueSource(strings = { "nullable", "holder", "tuple" })
    public void directValueWritersKeepXmlPresencePolicyInsideStructuredSlots(final String shape) throws IOException {
        final Type<?> type = Type.of(switch (shape) {
            case "nullable" -> "Nullable<Integer>";
            case "holder" -> "Holder<Map<String, Nullable<Integer>>>";
            case "tuple" -> "Tuple1<Map<String, Nullable<Integer>>>";
            default -> throw new AssertionError(shape);
        });
        for (final JsonXmlSerConfig<?> config : List.of(XmlSerConfig.create().setWriteNullNumberAsZero(true),
                new XmlEmbeddedJsonConfig().setWriteNullNumberAsZero(true))) {
            // Type.serializeTo is public too: bypassing XmlParser must not bypass its presence guard.
            assertThrows(ParsingException.class, () -> reviewFixes20260906_ser(type, nullableSlot(shape, Nullable.of((Integer) null)), config));
            assertNotNull(reviewFixes20260906_ser(type, nullableSlot(shape, Nullable.empty()), config));
            assertTrue(reviewFixes20260906_ser(type, nullableSlot(shape, Nullable.of(7)), config).contains("7"));
        }
        // Ordinary JSON deliberately keeps its existing present-null handling.
        assertTrue(reviewFixes20260906_ser(type, nullableSlot(shape, Nullable.of((Integer) null)), JsonSerConfig.create()).contains("null"));
        assertTrue(reviewFixes20260906_ser(type, nullableSlot(shape, Nullable.empty()), JsonSerConfig.create()).contains("null"));
    }

    private static Object nullableSlot(final String shape, final Nullable<Integer> value) {
        return switch (shape) {
            case "nullable" -> value;
            case "holder" -> Holder.of(Map.of("value", value));
            case "tuple" -> Tuple.of(Map.of("value", value));
            default -> throw new AssertionError(shape);
        };
    }

    @Test
    public void testHolderTypeJdbcPrimitiveValuesDistinguishSqlNullFromZero() throws SQLException {
        final HolderType<Integer> type = new HolderType<>("int");
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(0);
        when(rs.getInt("value")).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);

        assertNull(type.get(rs, 1).value());
        assertNull(type.get(rs, "value").value());

        when(rs.wasNull()).thenReturn(false);
        assertEquals(0, type.get(rs, 1).value());
        assertEquals(0, type.get(rs, "value").value());

        when(rs.getInt(1)).thenReturn(7);
        when(rs.getInt("value")).thenReturn(7);
        assertEquals(7, type.get(rs, 1).value());
        assertEquals(7, type.get(rs, "value").value());
    }

    @Test
    public void testNullableTypeJdbcPrimitiveValuesDistinguishSqlNullFromZero() throws SQLException {
        final NullableType<Integer> type = new NullableType<>("int");
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(0);
        when(rs.getInt("value")).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);

        assertTrue(type.get(rs, 1).isNull());
        assertTrue(type.get(rs, "value").isNull());

        when(rs.wasNull()).thenReturn(false);
        assertEquals(0, type.get(rs, 1).get());
        assertEquals(0, type.get(rs, "value").get());

        when(rs.getInt(1)).thenReturn(7);
        when(rs.getInt("value")).thenReturn(7);
        assertEquals(7, type.get(rs, 1).get());
        assertEquals(7, type.get(rs, "value").get());
    }

    @Test
    public void testJdbcReadsPreserveNestedGenericTypes() throws SQLException {
        final NullableType<List<String>> type = new NullableType<>("List<String>");
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("[1]");
        when(rs.getString("value")).thenReturn("[2]");

        assertEquals(List.of("1"), type.get(rs, 1).get());
        assertEquals(List.of("2"), type.get(rs, "value").get());
    }

    @Test
    public void testHolderJdbcReadsPreserveNestedGenericTypes() throws SQLException {
        final HolderType<List<String>> type = new HolderType<>("List<String>");
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("[1]");
        when(rs.getString("value")).thenReturn("[2]");

        assertEquals(List.of("1"), type.get(rs, 1).value());
        assertEquals(List.of("2"), type.get(rs, "value").value());
    }

    private NullableType<String> nullableStringType;
    private NullableType<Integer> nullableIntType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        nullableStringType = (NullableType<String>) createType("Nullable<String>");
        nullableIntType = (NullableType<Integer>) createType("Nullable<Integer>");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(nullableStringType.declaringName());
        assertTrue(nullableStringType.declaringName().contains("Nullable"));
        assertTrue(nullableStringType.declaringName().contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Nullable.class, nullableStringType.javaType());
        assertEquals(Nullable.class, nullableIntType.javaType());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(nullableStringType.isParameterizedType());
        assertTrue(nullableIntType.isParameterizedType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(nullableStringType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmptyNullable() {
        Nullable<String> empty = Nullable.empty();
        assertNull(nullableStringType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        Nullable<String> nullable = Nullable.of("test");
        assertEquals("test", nullableStringType.stringOf(nullable));
    }

    @Test
    public void testValueOfWithNull() {
        Nullable<String> result = nullableStringType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        Nullable<String> result = nullableStringType.valueOf("");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("", result.get());
    }

    @Test
    public void testValueOfWithValue() {
        Nullable<String> result = nullableStringType.valueOf("test");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetElementType() {
        assertNotNull(nullableStringType.elementType());
        assertEquals("String", nullableStringType.elementType().name());
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = nullableStringType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
        assertEquals("String", paramTypes.get(0).name());
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        Nullable<String> result = nullableStringType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        Nullable<String> result = nullableStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isNull());
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        Nullable<String> result = nullableStringType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByLabelWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(null);

        Nullable<String> result = nullableStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isNull());
        assertFalse(result.isEmpty());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        nullableStringType.set(stmt, 1, null);
        verify(stmt).setString(1, null);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Nullable<String> empty = Nullable.empty();
        nullableStringType.set(stmt, 1, empty);
        verify(stmt).setString(1, null);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Nullable<String> nullable = Nullable.of("test");
        nullableStringType.set(stmt, 1, nullable);
        verify(stmt).setString(1, "test");
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        nullableStringType.set(stmt, "param", null);
        verify(stmt).setString("param", null);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Nullable<String> empty = Nullable.empty();
        nullableStringType.set(stmt, "param", empty);
        verify(stmt).setString("param", null);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Nullable<String> nullable = Nullable.of("test");
        nullableStringType.set(stmt, "param", nullable);
        verify(stmt).setString("param", "test");
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        nullableStringType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        Nullable<String> empty = Nullable.empty();
        nullableStringType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        Nullable<String> nullable = Nullable.of("test");
        nullableStringType.appendTo(sb, nullable);
        assertEquals("test", sb.toString());
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        nullableStringType.serializeTo(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithEmpty() throws IOException {
        Nullable<String> empty = Nullable.empty();
        nullableStringType.serializeTo(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testStringOfUsesDeclaredElementTypeForSubtype() {
        TypeFactory.registerType(NullableBaseValue.class, value -> "base:" + value.value, str -> new NullableBaseValue(str.substring(5)));
        TypeFactory.registerType(NullableDerivedValue.class, value -> "derived:" + value.value, str -> new NullableDerivedValue(str.substring(8)));

        final NullableType<NullableBaseValue> type = new NullableType<>(TypeFactory.getType(NullableBaseValue.class).name());
        final NullableBaseValue value = new NullableDerivedValue("test");

        final String str = type.stringOf(Nullable.of(value));
        final Nullable<NullableBaseValue> roundTripped = type.valueOf(str);

        assertEquals("base:test", str);
        assertEquals("test", roundTripped.get().value);
    }

    public static class NullableBaseValue {
        final String value;

        NullableBaseValue(final String value) {
            this.value = value;
        }
    }

    public static final class NullableDerivedValue extends NullableBaseValue {
        NullableDerivedValue(final String value) {
            super(value);
        }
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

    // T6-03 (2026-09-06): empty / null-holding / null Nullable are written by the element handler as a null value.
    @Test
    public void reviewFixes20260906_emptyOrNullHoldingNullableHonoursElementNullFlags() throws IOException {
        final com.landawn.abacus.parser.JsonSerConfig zero = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true);
        final com.landawn.abacus.parser.JsonSerConfig falseCfg = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true);
        final com.landawn.abacus.parser.JsonSerConfig emptyStr = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullStringAsEmpty(true);

        assertEquals("0", reviewFixes20260906_ser(nullableIntType, Nullable.of((Integer) null), zero));
        assertEquals("0", reviewFixes20260906_ser(nullableIntType, Nullable.empty(), zero));
        assertEquals("0", reviewFixes20260906_ser(nullableIntType, null, zero));
        assertEquals("false", reviewFixes20260906_ser(Type.of("Nullable<Boolean>"), Nullable.empty(), falseCfg));
        assertEquals("\"\"", reviewFixes20260906_ser(nullableStringType, Nullable.of((String) null), emptyStr));
        assertEquals("null", reviewFixes20260906_ser(nullableStringType, Nullable.empty(), zero));
        assertEquals("null", reviewFixes20260906_ser(nullableIntType, Nullable.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(nullableIntType, Nullable.of((Integer) null), null));
        assertEquals("null", reviewFixes20260906_ser(nullableIntType, null, null));
        assertEquals("7", reviewFixes20260906_ser(nullableIntType, Nullable.of(7), zero));
        // R9 (2026-09-07): a raw List<Object> element carries no numeric element type, so there is no null policy for the
        // flag to apply - the parser writes null. The flag reaches the element handler only through a DECLARED slot.
        assertEquals("[null]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Nullable.of((Integer) null)), zero));
        assertEquals("{\"ln\": [0], \"n\": 0}", com.landawn.abacus.util.N.toJson(new ReviewFixesNullableBean(), zero));
        assertEquals("{\"ln\": [null], \"n\": null}", com.landawn.abacus.util.N.toJson(new ReviewFixesNullableBean()));
    }

    // T6-01 (2026-09-06): an Object slot dispatches on the runtime class; a non-serializable handler writes embedded JSON.
    @Test
    public void reviewFixes20260906_objectSlotUsesRuntimeTypeAndEmbeddedJson() throws IOException {
        final Type<?> type = Type.of("Nullable<Object>");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        assertEquals("1", reviewFixes20260906_ser(type, Nullable.of(1), jsc));
        assertEquals("true", reviewFixes20260906_ser(type, Nullable.of(true), jsc));
        assertEquals("\"s\"", reviewFixes20260906_ser(type, Nullable.of("s"), jsc));
        assertEquals("[1]", reviewFixes20260906_ser(type, Nullable.of(com.landawn.abacus.util.N.asList(1)), jsc));
        assertEquals("{\"k\": 1}", reviewFixes20260906_ser(type, Nullable.of(com.landawn.abacus.util.N.asMap("k", 1)), jsc));
        assertEquals("3", reviewFixes20260906_ser(type, Nullable.of(Nullable.of(3)), jsc));
        assertEquals("null", reviewFixes20260906_ser(type, Nullable.of((Object) null), jsc));
        assertEquals("1", reviewFixes20260906_ser(type, Nullable.of(1), null));
        assertEquals("[1]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Nullable.of(1))));
        assertEquals("{\"x\": [8]}", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asMap("x", Nullable.of(com.landawn.abacus.util.N.asList(8)))));
    }

    // T6-06 (2026-09-06): a non-null string the element parses to null yields a PRESENT null (documented now).
    @Test
    public void reviewFixes20260906_valueOfEmptyStringYieldsPresentNullForNumericElement() {
        final Nullable<Integer> parsed = nullableIntType.valueOf("");
        assertTrue(parsed.isPresent());
        assertTrue(parsed.isNull());
        assertFalse(nullableIntType.valueOf((String) null).isPresent());
        assertEquals("", nullableStringType.valueOf("").get());
        assertTrue(((com.landawn.abacus.util.u.Optional<?>) Type.of("Optional<Integer>").valueOf("")).isEmpty());
        assertTrue(((java.util.Optional<?>) Type.of("JdkOptional<Integer>").valueOf("")).isEmpty());
    }

    public static class ReviewFixesNullableBean {
        public java.util.List<Nullable<Integer>> ln = com.landawn.abacus.util.N.asList(Nullable.of((Integer) null));
        public Nullable<Integer> n = Nullable.of((Integer) null);
    }

    // G11-21 (2026-09-08): HolderType.serializeTo now goes through AbstractTupleType.serializeSlot like every other
    // single-slot wrapper, so a null value is written BY THE DECLARED HANDLER and its null-substitution flags apply.
    @Test
    public void reviewFixes20260908_nullHolderIsWrittenByTheDeclaredElementHandler() throws IOException {
        final Type<?> holderIntType = Type.of("Holder<Integer>");
        final com.landawn.abacus.parser.JsonSerConfig zero = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true);

        assertEquals("0", reviewFixes20260906_ser(holderIntType, Holder.of((Integer) null), zero));
        assertEquals("0", reviewFixes20260906_ser(holderIntType, null, zero));
        assertEquals("0", reviewFixes20260906_ser(holderIntType, Holder.of((Integer) null),
                com.landawn.abacus.parser.XmlSerConfig.create().setWriteNullNumberAsZero(true)));
        assertEquals("false", reviewFixes20260906_ser(Type.of("Holder<Boolean>"), Holder.of((Boolean) null),
                com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true)));
        assertEquals("\"\"", reviewFixes20260906_ser(Type.of("Holder<String>"), Holder.of((String) null),
                com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullStringAsEmpty(true)));

        // Without such a flag - and with no config at all - the literal null is still written.
        assertEquals("null", reviewFixes20260906_ser(holderIntType, Holder.of((Integer) null), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(holderIntType, Holder.of((Integer) null), null));
        assertEquals("null", reviewFixes20260906_ser(holderIntType, null, null));
        assertEquals("7", reviewFixes20260906_ser(holderIntType, Holder.of(7), zero));
    }

    // G11-21 (2026-09-08): a structured declared element type is written as embedded JSON, not as a quoted JSON string.
    @Test
    public void reviewFixes20260908_structuredHolderElementIsWrittenAsEmbeddedJson() throws IOException {
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();
        final ReviewFixesHolderBean bean = new ReviewFixesHolderBean();
        bean.x = 3;

        assertEquals("{\"x\": 3}", reviewFixes20260906_ser(Type.of("Holder<" + ReviewFixesHolderBean.class.getCanonicalName() + ">"), Holder.of(bean), jsc));
        assertEquals("{\"k\": 1}", reviewFixes20260906_ser(Type.of("Holder<Map<String, Integer>>"), Holder.of(com.landawn.abacus.util.N.asMap("k", 1)), jsc));
        assertEquals("[\"a\"]", reviewFixes20260906_ser(Type.of("Holder<List<String>>"), Holder.of(com.landawn.abacus.util.N.asList("a")), jsc));

        // An Object slot still dispatches on the runtime class, exactly as the other single-slot wrappers do.
        final Type<?> holderObjectType = Type.of("Holder<Object>");
        assertEquals("1", reviewFixes20260906_ser(holderObjectType, Holder.of(1), jsc));
        assertEquals("\"s\"", reviewFixes20260906_ser(holderObjectType, Holder.of("s"), jsc));
        assertEquals("[1]", reviewFixes20260906_ser(holderObjectType, Holder.of(com.landawn.abacus.util.N.asList(1)), jsc));
    }

    // G18-60 (2026-09-08): appendTo dispatches on the runtime class for an Object slot, exactly as serializeTo does.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void reviewFixes20260908_appendToObjectSlotUsesRuntimeType() throws IOException {
        final Type objectSlot = Type.of("Nullable<Object>");
        final java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("k", 1);
        map.put("s", "v");
        final List<Object> list = new java.util.ArrayList<>(java.util.Arrays.asList(1, "a"));

        assertEquals("{k:1, s:v}", reviewFixes20260908_appendToString(objectSlot, Nullable.of(map)));
        assertEquals("[1, a]", reviewFixes20260908_appendToString(objectSlot, Nullable.of(list)));

        // an Object slot must append exactly what the bare value's own handler appends
        for (final Object value : new Object[] { map, list, new int[] { 1, 2 }, 7, 1.5d, true, "q", com.landawn.abacus.util.Pair.of(1, "a") }) {
            final Type runtimeType = Type.of(value.getClass());

            assertEquals(reviewFixes20260908_appendToString(runtimeType, value), reviewFixes20260908_appendToString(objectSlot, Nullable.of(value)),
                    "value " + value);
        }

        // a declared (non-Object) element type keeps its own handler
        assertEquals("{k:1, s:v}", reviewFixes20260908_appendToString(Type.of("Nullable<Map<String, Object>>"), Nullable.of(map)));
        assertEquals("3", reviewFixes20260908_appendToString(Type.of("Nullable<Integer>"), Nullable.of(3)));

        // empty, null-holding and null still write the null literal
        assertEquals("null", reviewFixes20260908_appendToString(objectSlot, Nullable.empty()));
        assertEquals("null", reviewFixes20260908_appendToString(objectSlot, Nullable.of(null)));
        assertEquals("null", reviewFixes20260908_appendToString(objectSlot, null));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String reviewFixes20260908_appendToString(final Type type, final Object value) throws IOException {
        final StringBuilder sb = new StringBuilder();
        type.appendTo(sb, value);
        return sb.toString();
    }

    public static class ReviewFixesHolderBean {
        public int x;
    }
}
