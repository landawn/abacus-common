package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class JdkOptionalTypeTest extends TestBase {

    @Test
    public void testJdkOptionalTypeJdbcPrimitiveValuesDistinguishSqlNullFromZero() throws SQLException {
        final JdkOptionalType<Integer> type = new JdkOptionalType<>("int");
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(0);
        when(rs.getInt("value")).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);

        assertTrue(type.get(rs, 1).isEmpty());
        assertTrue(type.get(rs, "value").isEmpty());

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
        final JdkOptionalType<List<String>> type = new JdkOptionalType<>("List<String>");
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("[1]");
        when(rs.getString("value")).thenReturn("[2]");

        assertEquals(List.of("1"), type.get(rs, 1).get());
        assertEquals(List.of("2"), type.get(rs, "value").get());
    }

    private JdkOptionalType<String> optionalStringType;
    private JdkOptionalType<Integer> optionalIntegerType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        optionalStringType = (JdkOptionalType<String>) createType("JdkOptional<String>");
        optionalIntegerType = (JdkOptionalType<Integer>) createType("JdkOptional<Integer>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(optionalStringType.declaringName());
        assertTrue(optionalStringType.declaringName().contains("JdkOptional"));
    }

    @Test
    public void testClazz() {
        assertEquals(Optional.class, optionalStringType.javaType());
        assertEquals(Optional.class, optionalIntegerType.javaType());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(optionalStringType.isParameterizedType());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(optionalStringType.stringOf(null));
    }

    @Test
    public void testStringOf_Empty() {
        assertNull(optionalStringType.stringOf(Optional.empty()));
    }

    @Test
    public void testStringOf_Present_String() {
        Optional<String> opt = Optional.of("test");
        assertNotNull(optionalStringType.stringOf(opt));
    }

    @Test
    public void testStringOf_Present_Integer() {
        Optional<Integer> opt = Optional.of(42);
        assertNotNull(optionalIntegerType.stringOf(opt));
    }

    @Test
    public void testValueOf_ValidString() {
        Optional<String> result = optionalStringType.valueOf("test");
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    public void testGetElementType() {
        assertNotNull(optionalStringType.elementType());
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = optionalStringType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1, String.class)).thenReturn(null);

        Optional<String> result = optionalStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Present() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        Optional<String> result = optionalStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column", String.class)).thenReturn(null);

        Optional<String> result = optionalStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Present() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        Optional<String> result = optionalStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    public void testSet_PreparedStatement_Empty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalStringType.set(stmt, 1, Optional.empty());
        verify(stmt).setString(1, null);
    }

    @Test
    public void testSet_PreparedStatement_Present() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Optional<String> opt = Optional.of("test");

        optionalStringType.set(stmt, 1, opt);
        verify(stmt).setString(1, "test");
    }

    @Test
    public void testSet_CallableStatement_Empty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalStringType.set(stmt, "param", Optional.empty());
        verify(stmt).setString("param", null);
    }

    @Test
    public void testSet_CallableStatement_Present() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Optional<String> opt = Optional.of("test");

        optionalStringType.set(stmt, "param", opt);
        verify(stmt).setString("param", "test");
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalStringType.appendTo(sb, Optional.empty());
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Present() throws IOException {
        StringBuilder sb = new StringBuilder();
        Optional<String> opt = Optional.of("test");

        optionalStringType.appendTo(sb, opt);
        assertNotNull(sb.toString());
    }

    @Test
    public void testSerializeTo_Null() throws IOException {
        optionalStringType.serializeTo(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_Empty() throws IOException {
        optionalStringType.serializeTo(characterWriter, Optional.empty(), null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_Present() throws IOException {
        Optional<String> opt = Optional.of("test");
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        optionalStringType.serializeTo(characterWriter, opt, config);
        verify(characterWriter, times(1)).writeCharacter(anyString());
    }

    @Test
    public void testStringOfUsesElementType() {
        java.util.Optional<Integer> opt = java.util.Optional.of(42);
        String result = optionalIntegerType.stringOf(opt);
        assertNotNull(result);
        assertEquals("42", result);

        java.util.Optional<Integer> empty = java.util.Optional.empty();
        assertNull(optionalIntegerType.stringOf(empty));

        assertNull(optionalIntegerType.stringOf(null));
    }

    @Test
    public void testStringOfUsesDeclaredElementTypeForSubtype() {
        TypeFactory.registerType(JdkOptionalBaseValue.class, value -> "base:" + value.value, str -> new JdkOptionalBaseValue(str.substring(5)));
        TypeFactory.registerType(JdkOptionalDerivedValue.class, value -> "derived:" + value.value, str -> new JdkOptionalDerivedValue(str.substring(8)));

        final JdkOptionalType<JdkOptionalBaseValue> type = new JdkOptionalType<>(TypeFactory.getType(JdkOptionalBaseValue.class).name());
        final JdkOptionalBaseValue value = new JdkOptionalDerivedValue("test");

        final String str = type.stringOf(Optional.of(value));
        final Optional<JdkOptionalBaseValue> roundTripped = type.valueOf(str);

        assertEquals("base:test", str);
        assertEquals("test", roundTripped.orElseThrow().value);
    }

    public static class JdkOptionalBaseValue {
        final String value;

        JdkOptionalBaseValue(final String value) {
            this.value = value;
        }
    }

    public static final class JdkOptionalDerivedValue extends JdkOptionalBaseValue {
        JdkOptionalDerivedValue(final String value) {
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

    // T6-03 (2026-09-06): the empty branch is delegated to the declared element handler, so its null-substitution flags apply.
    @Test
    public void reviewFixes20260906_emptyOptionalHonoursElementNullFlags() throws IOException {
        final com.landawn.abacus.parser.JsonSerConfig zero = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true);
        final com.landawn.abacus.parser.JsonSerConfig falseCfg = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true);
        final com.landawn.abacus.parser.JsonSerConfig emptyStr = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullStringAsEmpty(true);

        assertEquals("0", reviewFixes20260906_ser(optionalIntegerType, Optional.empty(), zero));
        assertEquals("0", reviewFixes20260906_ser(optionalIntegerType, null, zero));
        assertEquals("false", reviewFixes20260906_ser(Type.of("JdkOptional<Boolean>"), Optional.empty(), falseCfg));
        assertEquals("\"\"", reviewFixes20260906_ser(optionalStringType, Optional.empty(), emptyStr));
        assertEquals("null", reviewFixes20260906_ser(optionalStringType, Optional.empty(), zero));
        assertEquals("\"0\"", reviewFixes20260906_ser(Type.of("JdkOptional<Long>"), Optional.empty(),
                com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true).setWriteLongAsString(true)));
        assertEquals("null", reviewFixes20260906_ser(optionalIntegerType, Optional.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalIntegerType, Optional.empty(), null));
        assertEquals("null", reviewFixes20260906_ser(optionalIntegerType, null, null));
        assertEquals("7", reviewFixes20260906_ser(optionalIntegerType, Optional.of(7), zero));
    }

    // T6-01 (2026-09-06): an Object slot dispatches on the runtime class; a non-serializable handler writes embedded JSON.
    @Test
    public void reviewFixes20260906_objectSlotUsesRuntimeTypeAndEmbeddedJson() throws IOException {
        final Type<?> type = Type.of("JdkOptional<Object>");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        assertEquals("1", reviewFixes20260906_ser(type, Optional.of(1), jsc));
        assertEquals("true", reviewFixes20260906_ser(type, Optional.of(true), jsc));
        assertEquals("\"s\"", reviewFixes20260906_ser(type, Optional.of("s"), jsc));
        assertEquals("[1]", reviewFixes20260906_ser(type, Optional.of(com.landawn.abacus.util.N.asList(1)), jsc));
        assertEquals("{\"k\": 1}", reviewFixes20260906_ser(type, Optional.of(com.landawn.abacus.util.N.asMap("k", 1)), jsc));
        assertEquals("3", reviewFixes20260906_ser(type, Optional.of(Optional.of(3)), jsc));
        assertEquals("null", reviewFixes20260906_ser(type, Optional.empty(), jsc));
        assertEquals("1", reviewFixes20260906_ser(type, Optional.of(1), null));
        assertEquals("[1]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Optional.of(1))));
        assertEquals("{\"x\": [8]}", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asMap("x", Optional.of(com.landawn.abacus.util.N.asList(8)))));
    }

    // R12 (2026-09-08): serializeTo got the Object-slot runtime dispatch but appendTo did not, so an Object-declared
    // optional holding a map/collection/bean appended ObjectType's JSON stringOf form ({"k": 1}) instead of the
    // toString()-style form ({k:1}) appendTo documents and the bare Map handler produces.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void reviewFixes20260908_appendToObjectSlotUsesRuntimeType() throws IOException {
        final Type objectSlot = Type.of("JdkOptional<Object>");
        final java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("k", 1);
        map.put("s", "v");
        final List<Object> list = new java.util.ArrayList<>(java.util.Arrays.asList(1, "a"));

        assertEquals("{k:1, s:v}", appendToString(objectSlot, Optional.of(map)));
        assertEquals("[1, a]", appendToString(objectSlot, Optional.of(list)));

        // an Object slot must append exactly what the bare value's own handler appends
        for (final Object value : new Object[] { map, list, new int[] { 1, 2 }, 7, 1.5d, true, "q", com.landawn.abacus.util.Pair.of(1, "a") }) {
            final Type runtimeType = Type.of(value.getClass());

            assertEquals(appendToString(runtimeType, value), appendToString(objectSlot, Optional.of(value)), "value " + value);
        }

        // a declared (non-Object) element type keeps its own handler
        assertEquals("{k:1, s:v}", appendToString(Type.of("JdkOptional<Map<String, Object>>"), Optional.of(map)));
        assertEquals("3", appendToString(Type.of("JdkOptional<Integer>"), Optional.of(3)));

        // empty / null still write the null literal
        assertEquals("null", appendToString(objectSlot, Optional.empty()));
        assertEquals("null", appendToString(objectSlot, null));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String appendToString(final Type type, final Object value) throws IOException {
        final StringBuilder sb = new StringBuilder();
        type.appendTo(sb, value);
        return sb.toString();
    }
}
