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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.u.OptionalDouble;

@Tag("new-test")
public class OptionalDoubleType100Test extends TestBase {

    private OptionalDoubleType optionalDoubleType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalDoubleType = (OptionalDoubleType) createType("OptionalDouble");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalDouble.class, optionalDoubleType.clazz());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalDoubleType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(optionalDoubleType.isNonQuotableCsvType());
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
    public void testStringOfWithValue() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        assertEquals("3.14159", optionalDoubleType.stringOf(opt));
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
    public void testWriteCharacterWithNull() throws IOException {
        optionalDoubleType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        OptionalDouble empty = OptionalDouble.empty();
        optionalDoubleType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithValue() throws IOException {
        OptionalDouble opt = OptionalDouble.of(1.5);
        optionalDoubleType.writeCharacter(writer, opt, config);
        verify(writer).write(1.5);
    }
}
