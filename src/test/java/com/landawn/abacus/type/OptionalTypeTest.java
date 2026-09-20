package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.u.Optional;

public class OptionalTypeTest extends TestBase {

    @Test
    public void testOptionalTypeJdbcPrimitiveValuesDistinguishSqlNullFromZero() throws SQLException {
        final OptionalType<Integer> type = new OptionalType<>("int");
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
        final OptionalType<List<String>> type = new OptionalType<>("List<String>");
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("[1]");
        when(rs.getString("value")).thenReturn("[2]");

        assertEquals(List.of("1"), type.get(rs, 1).get());
        assertEquals(List.of("2"), type.get(rs, "value").get());
    }

    private final OptionalType optionalStringType = new OptionalType("String");
    private final OptionalType optionalIntType = new OptionalType("int");

    @Test
    public void testDeclaringName() {
        assertNotNull(optionalStringType.declaringName());
        assertTrue(optionalStringType.declaringName().contains("Optional"));
        assertTrue(optionalStringType.declaringName().contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Optional.class, optionalStringType.javaType());
        assertEquals(Optional.class, optionalIntType.javaType());
    }

    @Test
    public void test_clazz() {
        assertNotNull(optionalStringType.javaType());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(optionalStringType.isParameterizedType());
        assertTrue(optionalIntType.isParameterizedType());
    }

    @Test
    public void testStringOfWithValue() {
        Optional<String> optional = Optional.of("test");
        assertEquals("test", optionalStringType.stringOf(optional));
    }

    @Test
    public void testStringOfWithEmpty() {
        Optional<String> empty = Optional.empty();
        assertNull(optionalStringType.stringOf(empty));
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = optionalStringType.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNotNull(result);
    }

    @Test
    public void testValueOfWithNull() {
        Optional<String> result = optionalStringType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        Optional<String> result = optionalStringType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("", result.get());
    }

    @Test
    public void testValueOfWithValue() {
        Optional<String> result = optionalStringType.valueOf("test");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("test", result.get());
    }

    @Test
    public void testValueOfReturningNull() {
        OptionalType<Object> optionalObjectType = (OptionalType<Object>) createType("Optional<Object>");
        Optional<Object> result = optionalObjectType.valueOf("null");
        assertNotNull(result);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = optionalStringType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
        assertEquals("String", paramTypes.get(0).name());
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> optionalStringType.get(rs, 1));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> optionalStringType.get(rs, "col"));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        Optional<String> result = optionalStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        Optional<String> result = optionalStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        Optional<String> result = optionalStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByLabelWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(null);

        Optional<String> result = optionalStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> optionalStringType.set(stmt, 1, null));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> optionalStringType.set(stmt, "param", null));
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalStringType.set(stmt, 1, null);
        verify(stmt).setString(1, null);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Optional<String> empty = Optional.empty();
        optionalStringType.set(stmt, 1, empty);
        verify(stmt).setString(1, null);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Optional<String> optional = Optional.of("test");
        optionalStringType.set(stmt, 1, optional);
        verify(stmt).setString(1, "test");
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalStringType.set(stmt, "param", null);
        verify(stmt).setString("param", null);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Optional<String> empty = Optional.empty();
        optionalStringType.set(stmt, "param", empty);
        verify(stmt).setString("param", null);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Optional<String> optional = Optional.of("test");
        optionalStringType.set(stmt, "param", optional);
        verify(stmt).setString("param", "test");
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();
        optionalStringType.appendTo(sw, null);
        assertNotNull(sw.toString());
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalStringType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        Optional<String> empty = Optional.empty();
        optionalStringType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        Optional<String> optional = Optional.of("test");
        optionalStringType.appendTo(sb, optional);
        assertEquals("test", sb.toString());
    }

    @Test
    public void test_name() {
        assertNotNull(optionalStringType.name());
        assertFalse(optionalStringType.name().isEmpty());
    }

    /**
     * Regression test for bug: OptionalType.stringOf was using N.stringOf(x.get())
     * instead of elementType.stringOf(x.get()), which bypassed type-specific formatting.
     *
     * For example, a Timestamp stored in Optional<Timestamp> must be serialized using
     * TimestampType's formatting logic, not Timestamp.toString() from N.stringOf().
     * The two produce different results: TimestampType uses Dates.format() while
     * Timestamp.toString() uses JDBC escape format.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testStringOf_usesElementTypeStringOf_notNStringOf() {
        // Build Optional<Integer> type handler
        OptionalType<Integer> optionalIntegerType = new OptionalType<>("Integer");
        Type<Integer> elementType = optionalIntegerType.elementType();

        Integer value = 42;
        Optional<Integer> opt = Optional.of(value);

        // stringOf must delegate to elementType.stringOf, not N.stringOf
        String fromOptionalType = optionalIntegerType.stringOf(opt);
        String fromElementType = elementType.stringOf(value);

        assertEquals(fromElementType, fromOptionalType, "OptionalType.stringOf must delegate to elementType.stringOf, not N.stringOf");
    }

    /**
     * Regression test: verifies that Optional<Timestamp> stringOf produces the same result
     * as TimestampType.stringOf, rather than Timestamp.toString().
     * Before the fix, N.stringOf(timestamp) called toString() which uses JDBC escape format,
     * while TimestampType uses Dates.format() which uses the configured date pattern.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testStringOf_Timestamp_usesTimestampTypeFormatting() {
        OptionalType<Timestamp> optionalTimestampType = new OptionalType<>("Timestamp");
        Type<Timestamp> timestampType = optionalTimestampType.elementType();

        Timestamp ts = new Timestamp(1703502645000L); // 2023-12-25T10:30:45Z in millis
        Optional<Timestamp> opt = Optional.of(ts);

        String fromOptionalType = optionalTimestampType.stringOf(opt);
        String fromTimestampType = timestampType.stringOf(ts);

        assertNotNull(fromOptionalType);
        assertEquals(fromTimestampType, fromOptionalType,
                "OptionalType<Timestamp>.stringOf must use TimestampType.stringOf formatting, not Timestamp.toString()");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStringOf_null_returnsNull() {
        OptionalType<String> type = new OptionalType<>("String");
        assertNull(type.stringOf(null));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStringOf_empty_returnsNull() {
        OptionalType<String> type = new OptionalType<>("String");
        assertNull(type.stringOf(Optional.empty()));
    }

    @Test
    public void testStringOf_usesDeclaredElementTypeForSubtype() {
        registerOptionalSubtypeFixtures();

        final OptionalType<OptionalBaseValue> type = new OptionalType<>(TypeFactory.getType(OptionalBaseValue.class).name());
        final OptionalBaseValue value = new OptionalDerivedValue("test");

        final String str = type.stringOf(Optional.of(value));
        final Optional<OptionalBaseValue> roundTripped = type.valueOf(str);

        assertEquals("base:test", str);
        assertEquals("test", roundTripped.get().value);
    }

    private static void registerOptionalSubtypeFixtures() {
        try {
            TypeFactory.registerType(OptionalBaseValue.class, value -> "base:" + value.value, str -> new OptionalBaseValue(str.substring(5)));
        } catch (IllegalArgumentException ignore) {
            // already registered by a sibling test in this class
        }
        try {
            TypeFactory.registerType(OptionalDerivedValue.class, value -> "derived:" + value.value, str -> new OptionalDerivedValue(str.substring(8)));
        } catch (IllegalArgumentException ignore) {
            // already registered by a sibling test in this class
        }
    }

    public static class OptionalBaseValue {
        final String value;

        OptionalBaseValue(final String value) {
            this.value = value;
        }
    }

    public static final class OptionalDerivedValue extends OptionalBaseValue {
        OptionalDerivedValue(final String value) {
            super(value);
        }
    }

    // Bug: serializeTo/appendTo used runtime Type.of(getClass()) instead of declared elementType,
    // so subtype formatting diverged from stringOf (which already uses elementType).
    @Test
    public void testSerializeTo_usesDeclaredElementTypeForSubtype() throws java.io.IOException {
        registerOptionalSubtypeFixtures();

        final OptionalType<OptionalBaseValue> type = new OptionalType<>(TypeFactory.getType(OptionalBaseValue.class).name());
        final OptionalBaseValue value = new OptionalDerivedValue("test");

        final StringBuilder sb = new StringBuilder();
        type.appendTo(sb, Optional.of(value));
        assertEquals("base:test", sb.toString(), "appendTo must use declared elementType, not runtime class");

        final CharacterWriter writer = createCharacterWriter();
        assertNotNull(type);
        type.serializeTo(writer, Optional.of(value), null);
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
        final com.landawn.abacus.parser.JsonSerConfig zeroAndFalse = com.landawn.abacus.parser.JsonSerConfig.create()
                .setWriteNullNumberAsZero(true)
                .setWriteNullBooleanAsFalse(true);
        final Type<?> intType = Type.of("Optional<Integer>");
        final Type<?> boolType = Type.of("Optional<Boolean>");
        final Type<?> strType = Type.of("Optional<String>");

        assertEquals("0", reviewFixes20260906_ser(intType, Optional.empty(), zero));
        assertEquals("0", reviewFixes20260906_ser(intType, null, zero));
        assertEquals("0.0", reviewFixes20260906_ser(Type.of("Optional<Double>"), Optional.empty(), zero));
        assertEquals("false", reviewFixes20260906_ser(boolType, Optional.empty(), falseCfg));
        assertEquals("false", reviewFixes20260906_ser(boolType, null, zeroAndFalse));
        assertEquals("null", reviewFixes20260906_ser(strType, Optional.empty(), zeroAndFalse));
        assertEquals("\"\"", reviewFixes20260906_ser(strType, Optional.empty(), emptyStr));
        assertEquals("\"0\"", reviewFixes20260906_ser(Type.of("Optional<Long>"), Optional.empty(),
                com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true).setWriteLongAsString(true)));
        assertEquals("null", reviewFixes20260906_ser(Type.of("Optional<List<Integer>>"), Optional.empty(), zeroAndFalse));
        assertEquals("null", reviewFixes20260906_ser(Type.of("Optional<" + ReviewFixesBean.class.getName() + ">"), Optional.empty(), zeroAndFalse));
        // no flag / no config: unchanged
        assertEquals("null", reviewFixes20260906_ser(intType, Optional.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(intType, Optional.empty(), null));
        assertEquals("null", reviewFixes20260906_ser(intType, null, null));
        assertEquals("null", reviewFixes20260906_ser(boolType, Optional.empty(), zero));
        assertEquals("null", reviewFixes20260906_ser(intType, Optional.empty(), falseCfg));
        // present values unchanged
        assertEquals("5", reviewFixes20260906_ser(intType, Optional.of(5), zero));
        assertEquals("true", reviewFixes20260906_ser(boolType, Optional.of(true), falseCfg));
        assertEquals("\"\"", reviewFixes20260906_ser(strType, Optional.of(""), emptyStr));
    }

    // T6-01 (2026-09-06): an Object slot dispatches on the runtime class; a non-serializable handler writes embedded JSON.
    @SuppressWarnings("unchecked")
    @Test
    public void reviewFixes20260906_objectSlotUsesRuntimeTypeAndEmbeddedJson() throws IOException {
        final Type<Object> type = (Type<Object>) Type.of("Optional<Object>");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        assertEquals("1", reviewFixes20260906_ser(type, Optional.of(1), jsc));
        assertEquals("true", reviewFixes20260906_ser(type, Optional.of(true), jsc));
        assertEquals("1.5", reviewFixes20260906_ser(type, Optional.of(1.5d), jsc));
        assertEquals("\"q\\\"x\"", reviewFixes20260906_ser(type, Optional.of("q\"x"), jsc));
        assertEquals("[1]", reviewFixes20260906_ser(type, Optional.of(com.landawn.abacus.util.N.asList(1)), jsc));
        assertEquals("{\"k\": 1}", reviewFixes20260906_ser(type, Optional.of(com.landawn.abacus.util.N.asMap("k", 1)), jsc));
        assertEquals("{\"a\": 1, \"s\": \"x\"}", reviewFixes20260906_ser(type, Optional.of(new ReviewFixesBean()), jsc));
        assertEquals("3", reviewFixes20260906_ser(type, Optional.of(Optional.of(3)), jsc));
        assertEquals("[1, \"a\"]", reviewFixes20260906_ser(type, Optional.of(com.landawn.abacus.util.Tuple.of(1, "a")), jsc));
        assertEquals("[1, 2]", reviewFixes20260906_ser(type, Optional.of(com.landawn.abacus.util.Pair.of(1, 2)), jsc));
        assertEquals("[[1, \"a\"]]",
                reviewFixes20260906_ser(type, Optional.of(com.landawn.abacus.util.N.asList(com.landawn.abacus.util.N.asList(1, "a"))), jsc));
        assertEquals("null", reviewFixes20260906_ser(type, Optional.empty(), jsc));
        // an unregistered plain object keeps the quoted toString() form and does not throw
        final Object plain = new Object();
        assertEquals("\"" + plain + "\"", reviewFixes20260906_ser(type, Optional.of(plain), jsc));
        // null config: no quotation; the structure's JSON text is still written
        assertEquals("1", reviewFixes20260906_ser(type, Optional.of(1), null));
        assertEquals("q\\\"x", reviewFixes20260906_ser(type, Optional.of("q\"x"), null));
        assertEquals("[1]", reviewFixes20260906_ser(type, Optional.of(com.landawn.abacus.util.N.asList(1)), null));
        // serializeTo now agrees with stringOf for every shape
        for (final Object v : new Object[] { 1, true, "s", com.landawn.abacus.util.N.asList(1), com.landawn.abacus.util.N.asMap("k", 1), new ReviewFixesBean(),
                Optional.of(3) }) {
            final String expected = v instanceof String ? "\"" + v + "\"" : type.stringOf(Optional.of(v));
            assertEquals(expected, reviewFixes20260906_ser(type, Optional.of(v), jsc), "value " + v);
        }
        // XML config: the writer's own escaping applies to the stringOf text, no JSON parser involved
        final com.landawn.abacus.util.BufferedXmlWriter xmlWriter = com.landawn.abacus.util.Objectory.createBufferedXmlWriter();

        try {
            type.serializeTo(xmlWriter, Optional.of(com.landawn.abacus.util.N.asMap("k", 1)), com.landawn.abacus.parser.XmlSerConfig.create());
            xmlWriter.write('|');
            type.serializeTo(xmlWriter, Optional.of(1), com.landawn.abacus.parser.XmlSerConfig.create());
            assertEquals("{&quot;k&quot;: 1}|1", xmlWriter.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(xmlWriter);
        }
    }

    // T6-01 (2026-09-06): the real parser no longer quotes numbers / stringifies containers inside optionals.
    @SuppressWarnings("unchecked")
    @Test
    public void reviewFixes20260906_parserWritesOptionalInObjectSlotsTyped() {
        assertEquals("[5, [1, 2]]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Optional.of(5), com.landawn.abacus.util.Pair.of(1, 2))));
        assertEquals("{\"x\": [8]}", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asMap("x", Optional.of(com.landawn.abacus.util.N.asList(8)))));
        assertEquals("[1]", com.landawn.abacus.util.N.toJson(new Object[] { Optional.of(1) }));

        final java.util.Map<String, Object> back = com.landawn.abacus.util.N.fromJson(
                com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asMap("x", Optional.of(com.landawn.abacus.util.N.asList(8)))), java.util.Map.class);
        assertTrue(back.get("x") instanceof List, "a List must come back, not the String \"[8]\"");
        final List<Object> lo = com.landawn.abacus.util.N.fromJson(com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Optional.of(5))),
                List.class);
        assertEquals(5, lo.get(0));

        // fully DECLARED shapes: Optional<Map>, Optional<Bean>, Tuple2<Bean,Integer>, List<Object> field
        final ReviewFixesDeclared declared = new ReviewFixesDeclared();
        final String json = com.landawn.abacus.util.N.toJson(declared);
        assertEquals("{\"om\": {\"k\": 1}, \"ob\": {\"a\": 1, \"s\": \"x\"}, \"tb\": [{\"a\": 1, \"s\": \"x\"}, 2], \"lo\": [5, [1, 2]]}", json);
        final ReviewFixesDeclared back2 = com.landawn.abacus.util.N.fromJson(json, ReviewFixesDeclared.class);
        assertEquals(Integer.valueOf(1), back2.om.get().get("k"));
        assertEquals(1, back2.ob.get().a);
        assertEquals("x", back2.tb._1.s);
        assertEquals(Integer.valueOf(2), back2.tb._2);
        assertEquals(5, back2.lo.get(0));
        assertTrue(back2.lo.get(1) instanceof List);

        // R9 (2026-09-07): under prettyFormat the embedded structure is written COMPACTLY - the type layer is not told
        // the caller's indentation, so propagating prettyFormat restarted the nested object at the left margin.
        final String pretty = com.landawn.abacus.util.N.toJson(declared, com.landawn.abacus.parser.JsonSerConfig.create().setPrettyFormat(true));
        assertEquals("{\n" //
                + "    \"om\": {\"k\": 1},\n" //
                + "    \"ob\": {\"a\": 1, \"s\": \"x\"},\n" //
                + "    \"tb\": [{\"a\": 1, \"s\": \"x\"}, 2],\n" //
                + "    \"lo\": [\n" //
                + "        5,\n" //
                + "        [1, 2]\n" //
                + "    ]\n" //
                + "}", pretty);
        assertEquals(declared.om.get(), com.landawn.abacus.util.N.fromJson(pretty, ReviewFixesDeclared.class).om.get());
    }

    // R12 (2026-09-08): serializeTo got the Object-slot runtime dispatch but appendTo did not, so an Object-declared
    // optional holding a map/collection/bean appended ObjectType's JSON stringOf form ({"k": 1}) instead of the
    // toString()-style form ({k:1}) appendTo documents and the bare Map handler produces.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void reviewFixes20260908_appendToObjectSlotUsesRuntimeType() throws IOException {
        final Type objectSlot = Type.of("Optional<Object>");
        final java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("k", 1);
        map.put("s", "v");
        final List<Object> list = new java.util.ArrayList<>(java.util.Arrays.asList(1, "a"));

        assertEquals("{k:1, s:v}", reviewFixes20260908_appendToString(objectSlot, Optional.of(map)));
        assertEquals("[1, a]", reviewFixes20260908_appendToString(objectSlot, Optional.of(list)));

        // an Object slot must append exactly what the bare value's own handler appends
        for (final Object value : new Object[] { map, list, new int[] { 1, 2 }, 7, 1.5d, true, "q", com.landawn.abacus.util.Pair.of(1, "a") }) {
            final Type runtimeType = Type.of(value.getClass());

            assertEquals(reviewFixes20260908_appendToString(runtimeType, value), reviewFixes20260908_appendToString(objectSlot, Optional.of(value)),
                    "value " + value);
        }

        // a declared (non-Object) element type keeps its own handler
        assertEquals("{k:1, s:v}", reviewFixes20260908_appendToString(Type.of("Optional<Map<String, Object>>"), Optional.of(map)));
        assertEquals("3", reviewFixes20260908_appendToString(Type.of("Optional<Integer>"), Optional.of(3)));

        // empty / null still write the null literal
        assertEquals("null", reviewFixes20260908_appendToString(objectSlot, Optional.empty()));
        assertEquals("null", reviewFixes20260908_appendToString(objectSlot, null));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String reviewFixes20260908_appendToString(final Type type, final Object value) throws IOException {
        final StringBuilder sb = new StringBuilder();
        type.appendTo(sb, value);
        return sb.toString();
    }

    public static class ReviewFixesBean {
        public int a = 1;
        public String s = "x";
    }

    public static class ReviewFixesDeclared {
        public Optional<java.util.Map<String, Integer>> om = Optional.of(com.landawn.abacus.util.N.asMap("k", 1));
        public Optional<ReviewFixesBean> ob = Optional.of(new ReviewFixesBean());
        public com.landawn.abacus.util.Tuple.Tuple2<ReviewFixesBean, Integer> tb = com.landawn.abacus.util.Tuple.of(new ReviewFixesBean(), 2);
        public List<Object> lo = com.landawn.abacus.util.N.asList(Optional.of(5), com.landawn.abacus.util.Pair.of(1, 2));
    }
}
