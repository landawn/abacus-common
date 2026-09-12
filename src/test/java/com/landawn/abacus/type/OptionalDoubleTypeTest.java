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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.u.OptionalDouble;

public class OptionalDoubleTypeTest extends TestBase {

    private OptionalDoubleType optionalDoubleType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalDoubleType = (OptionalDoubleType) createType("OptionalDouble");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalDoubleType.isComparable());
    }

    @Test
    public void testStringOfWithValue() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        assertEquals("3.14159", optionalDoubleType.stringOf(opt));
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalDoubleType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalDouble empty = OptionalDouble.empty();
        assertNull(optionalDoubleType.stringOf(empty));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalDouble result = optionalDoubleType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalDouble result = optionalDoubleType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithValidString() {
        OptionalDouble result = optionalDoubleType.valueOf("123.456");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(123.456, result.get());
    }

    @Test
    public void testValueOfWithScientificNotation() {
        OptionalDouble result = optionalDoubleType.valueOf("1.23e4");
        assertNotNull(result);
        assertEquals(12300.0, result.get());
    }

    @Test
    public void testValueOfWithSpecialValues() {
        assertEquals(Double.POSITIVE_INFINITY, optionalDoubleType.valueOf("Infinity").get());
        assertEquals(Double.NEGATIVE_INFINITY, optionalDoubleType.valueOf("-Infinity").get());
        assertTrue(Double.isNaN(optionalDoubleType.valueOf("NaN").get()));
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> optionalDoubleType.valueOf("abc"));
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalDouble result = optionalDoubleType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithDouble() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(99.99);

        OptionalDouble result = optionalDoubleType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(99.99, result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonDouble() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(100);

        OptionalDouble result = optionalDoubleType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(100.0, result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(2.71828);

        OptionalDouble result = optionalDoubleType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2.71828, result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalDoubleType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.DOUBLE);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalDouble empty = OptionalDouble.empty();
        optionalDoubleType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.DOUBLE);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalDouble opt = OptionalDouble.of(42.0);
        optionalDoubleType.set(stmt, 1, opt);
        verify(stmt).setDouble(1, 42.0);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalDoubleType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.DOUBLE);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalDouble empty = OptionalDouble.empty();
        optionalDoubleType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.DOUBLE);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalDouble opt = OptionalDouble.of(-123.456);
        optionalDoubleType.set(stmt, "param", opt);
        verify(stmt).setDouble("param", -123.456);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalDoubleType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalDouble empty = OptionalDouble.empty();
        optionalDoubleType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalDouble opt = OptionalDouble.of(999.888);
        optionalDoubleType.appendTo(sb, opt);
        assertEquals("999.888", sb.toString());
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        optionalDoubleType.serializeTo(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithEmpty() throws IOException {
        OptionalDouble empty = OptionalDouble.empty();
        optionalDoubleType.serializeTo(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithValue() throws IOException {
        OptionalDouble opt = OptionalDouble.of(1.5);
        optionalDoubleType.serializeTo(writer, opt, config);
        verify(writer).write(1.5);
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

    // T5-01 / T6-03 (2026-09-06): serializeTo ignored writeNullNumberAsZero for the optional numeric handlers.
    @Test
    public void reviewFixes20260906_serializeToHonoursWriteNullNumberAsZero() throws java.io.IOException {
        final com.landawn.abacus.parser.JsonSerConfig zero = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true);

        assertEquals("0.0", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(), zero));
        assertEquals("0.0", reviewFixes20260906_ser(optionalDoubleType, null, zero));
        assertEquals("0.0", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(), com.landawn.abacus.parser.XmlSerConfig.create().setWriteNullNumberAsZero(true)));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, null, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(), null));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, null, null));
        assertEquals("null", reviewFixes20260906_ser(optionalDoubleType, OptionalDouble.empty(), com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true)));
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
        assertEquals(0.0d, optionalDoubleType.get(rs, 1).get());
        assertTrue(optionalDoubleType.get(rs, "c").isPresent());
        assertEquals(0.0d, optionalDoubleType.get(rs, "c").get());
        assertThrows(NumberFormatException.class, () -> optionalDoubleType.get(rs, 2));
        assertEquals(7.0d, optionalDoubleType.get(rs, 3).get());
        assertTrue(optionalDoubleType.get(rs, 4).isEmpty());

        assertTrue(optionalDoubleType.valueOf("").isEmpty());
        assertTrue(optionalDoubleType.valueOf((String) null).isEmpty());
        assertEquals(7.0d, optionalDoubleType.valueOf("7").get());
    }
}
