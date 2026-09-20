package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.OptionalDouble;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class JdkOptionalDoubleTypeTest extends TestBase {

    private JdkOptionalDoubleType optionalDoubleType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        optionalDoubleType = (JdkOptionalDoubleType) createType("JdkOptionalDouble");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalDouble.class, optionalDoubleType.javaType());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalDoubleType.isComparable());
    }

    @Test
    public void testCompare() {
        assertEquals(0, optionalDoubleType.compare(OptionalDouble.of(1.5), OptionalDouble.of(1.5)));
        assertTrue(optionalDoubleType.compare(OptionalDouble.of(1.5), OptionalDouble.of(2.5)) < 0);
        assertTrue(optionalDoubleType.compare(OptionalDouble.of(2.5), OptionalDouble.of(1.5)) > 0);
        assertTrue(optionalDoubleType.compare(OptionalDouble.empty(), OptionalDouble.of(1.5)) < 0);
        assertEquals(0, optionalDoubleType.compare(OptionalDouble.empty(), OptionalDouble.empty()));
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(optionalDoubleType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOf_Present() {
        OptionalDouble opt = OptionalDouble.of(42.5);
        assertEquals("42.5", optionalDoubleType.stringOf(opt));
    }

    @Test
    public void testStringOf_Empty() {
        assertNull(optionalDoubleType.stringOf(OptionalDouble.empty()));
    }

    @Test
    public void testValueOf_EmptyString() {
        OptionalDouble result = optionalDoubleType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_ValidString() {
        OptionalDouble result = optionalDoubleType.valueOf("42.5");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42.5, result.getAsDouble());
    }

    @Test
    public void testValueOf_IntegerString() {
        OptionalDouble result = optionalDoubleType.valueOf("42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42.0, result.getAsDouble());
    }

    @Test
    public void testValueOf_NegativeString() {
        OptionalDouble result = optionalDoubleType.valueOf("-42.5");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(-42.5, result.getAsDouble());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Double() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42.5);

        OptionalDouble result = optionalDoubleType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42.5, result.getAsDouble());
    }

    @Test
    public void testGet_ResultSet_ByIndex_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42);

        OptionalDouble result = optionalDoubleType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42.0, result.getAsDouble());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("double_column")).thenReturn(null);

        OptionalDouble result = optionalDoubleType.get(rs, "double_column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Double() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("double_column")).thenReturn(42.5);

        OptionalDouble result = optionalDoubleType.get(rs, "double_column");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42.5, result.getAsDouble());
    }

    @Test
    public void testSet_PreparedStatement_Empty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalDoubleType.set(stmt, 1, OptionalDouble.empty());
        verify(stmt).setNull(1, java.sql.Types.DOUBLE);
    }

    @Test
    public void testSet_PreparedStatement_Present() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalDouble opt = OptionalDouble.of(42.5);

        optionalDoubleType.set(stmt, 1, opt);
        verify(stmt).setDouble(1, 42.5);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalDoubleType.set(stmt, "param_name", null);
        verify(stmt).setNull("param_name", java.sql.Types.DOUBLE);
    }

    @Test
    public void testSet_CallableStatement_Empty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalDoubleType.set(stmt, "param_name", OptionalDouble.empty());
        verify(stmt).setNull("param_name", java.sql.Types.DOUBLE);
    }

    @Test
    public void testSet_CallableStatement_Present() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalDouble opt = OptionalDouble.of(42.5);

        optionalDoubleType.set(stmt, "param_name", opt);
        verify(stmt).setDouble("param_name", 42.5);
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalDoubleType.appendTo(sb, OptionalDouble.empty());
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Present() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalDouble opt = OptionalDouble.of(42.5);

        optionalDoubleType.appendTo(sb, opt);
        assertEquals("42.5", sb.toString());
    }

    @Test
    public void testSerializeTo_Null() throws IOException {
        optionalDoubleType.serializeTo(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_Empty() throws IOException {
        optionalDoubleType.serializeTo(characterWriter, OptionalDouble.empty(), null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_Present() throws IOException {
        OptionalDouble opt = OptionalDouble.of(42.5);

        optionalDoubleType.serializeTo(characterWriter, opt, null);
        verify(characterWriter).write(42.5);
    }

    @Test
    public void testSerializeTo_WithConfig() throws IOException {
        OptionalDouble opt = OptionalDouble.of(42.5);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        optionalDoubleType.serializeTo(characterWriter, opt, config);
        verify(characterWriter).write(42.5);
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

    // T5-01 / T6-03 (2026-09-06): serializeTo ignored writeNullNumberAsZero for the optional numeric handlers.
    @Test
    public void reviewFixes20260906_serializeToHonoursWriteNullNumberAsZero() throws java.io.IOException {
        final com.landawn.abacus.parser.JsonSerConfig zero = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true);

        assertEquals("0.0", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(), zero));
        assertEquals("0.0", reviewFixes20260906_ser(optionalDoubleType, null, zero));
        assertEquals("0.0", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(),
                com.landawn.abacus.parser.XmlSerConfig.create().setWriteNullNumberAsZero(true)));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, null, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(), null));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, null, null));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(),
                com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true)));
        assertEquals("7.5", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.of(7.5d), zero));
        assertEquals("7.5", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.of(7.5d), null));
    }

    // T5-06 (2026-09-06, code change REJECTED - pinned): an empty-string column value is coerced by Numbers.toXxx(Object)
    // to a PRESENT zero, exactly as the non-optional handlers (IntegerType.get ...) answer, while valueOf("") is empty.
    @Test
    public void reviewFixes20260906_getEmptyStringColumnIsPresentZeroUnlikeValueOf() throws SQLException {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("");
        when(rs.getObject("c")).thenReturn("");
        when(rs.getObject(2)).thenReturn(" ");
        when(rs.getObject(3)).thenReturn("7");
        when(rs.getObject(4)).thenReturn(null);

        assertTrue(optionalDoubleType.get(rs, 1).isPresent());
        assertEquals(0.0d, optionalDoubleType.get(rs, 1).getAsDouble());
        assertTrue(optionalDoubleType.get(rs, "c").isPresent());
        assertEquals(0.0d, optionalDoubleType.get(rs, "c").getAsDouble());
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> optionalDoubleType.get(rs, 2));
        assertEquals(7.0d, optionalDoubleType.get(rs, 3).getAsDouble());
        assertTrue(optionalDoubleType.get(rs, 4).isEmpty());

        assertTrue(optionalDoubleType.valueOf("").isEmpty());
        assertTrue(optionalDoubleType.valueOf((String) null).isEmpty());
        assertEquals(7.0d, optionalDoubleType.valueOf("7").getAsDouble());
    }
}
