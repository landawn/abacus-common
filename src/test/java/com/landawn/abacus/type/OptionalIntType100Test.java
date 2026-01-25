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
import com.landawn.abacus.util.u.OptionalInt;

@Tag("new-test")
public class OptionalIntType100Test extends TestBase {

    private OptionalIntType optionalIntType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalIntType = (OptionalIntType) createType("OptionalInt");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalInt.class, optionalIntType.clazz());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalIntType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(optionalIntType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalIntType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalInt empty = OptionalInt.empty();
        assertNull(optionalIntType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        OptionalInt opt = OptionalInt.of(12345);
        assertEquals("12345", optionalIntType.stringOf(opt));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalInt result = optionalIntType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalInt result = optionalIntType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithValidString() {
        OptionalInt result = optionalIntType.valueOf("999");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(999, result.get());
    }

    @Test
    public void testValueOfWithNegativeValue() {
        OptionalInt result = optionalIntType.valueOf("-500");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(-500, result.get());
    }

    @Test
    public void testValueOfWithMaxValue() {
        OptionalInt result = optionalIntType.valueOf(String.valueOf(Integer.MAX_VALUE));
        assertNotNull(result);
        assertEquals(Integer.MAX_VALUE, result.get());
    }

    @Test
    public void testValueOfWithMinValue() {
        OptionalInt result = optionalIntType.valueOf(String.valueOf(Integer.MIN_VALUE));
        assertNotNull(result);
        assertEquals(Integer.MIN_VALUE, result.get());
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> optionalIntType.valueOf("not-a-number"));
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalInt result = optionalIntType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithInteger() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42);

        OptionalInt result = optionalIntType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(42, result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonInteger() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(100L);

        OptionalInt result = optionalIntType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(100, result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(777);

        OptionalInt result = optionalIntType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(777, result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalIntType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.INTEGER);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalInt empty = OptionalInt.empty();
        optionalIntType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.INTEGER);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalInt opt = OptionalInt.of(123);
        optionalIntType.set(stmt, 1, opt);
        verify(stmt).setInt(1, 123);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalIntType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.INTEGER);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalInt empty = OptionalInt.empty();
        optionalIntType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.INTEGER);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalInt opt = OptionalInt.of(-456);
        optionalIntType.set(stmt, "param", opt);
        verify(stmt).setInt("param", -456);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalIntType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalInt empty = OptionalInt.empty();
        optionalIntType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalInt opt = OptionalInt.of(88888);
        optionalIntType.appendTo(sb, opt);
        assertEquals("88888", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        optionalIntType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        OptionalInt empty = OptionalInt.empty();
        optionalIntType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithValue() throws IOException {
        OptionalInt opt = OptionalInt.of(2023);
        optionalIntType.writeCharacter(writer, opt, config);
        verify(writer).writeInt(2023);
    }
}
