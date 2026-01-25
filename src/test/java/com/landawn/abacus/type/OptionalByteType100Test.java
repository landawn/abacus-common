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
import com.landawn.abacus.util.u.OptionalByte;

@Tag("new-test")
public class OptionalByteType100Test extends TestBase {

    private OptionalByteType optionalByteType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalByteType = (OptionalByteType) createType("OptionalByte");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalByte.class, optionalByteType.clazz());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalByteType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(optionalByteType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalByteType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalByte empty = OptionalByte.empty();
        assertNull(optionalByteType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        OptionalByte opt = OptionalByte.of((byte) 42);
        assertEquals("42", optionalByteType.stringOf(opt));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalByte result = optionalByteType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalByte result = optionalByteType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithValidString() {
        OptionalByte result = optionalByteType.valueOf("123");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) 123, result.get());
    }

    @Test
    public void testValueOfWithNegativeValue() {
        OptionalByte result = optionalByteType.valueOf("-50");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) -50, result.get());
    }

    @Test
    public void testValueOfWithMaxValue() {
        OptionalByte result = optionalByteType.valueOf(String.valueOf(Byte.MAX_VALUE));
        assertNotNull(result);
        assertEquals(Byte.MAX_VALUE, result.get());
    }

    @Test
    public void testValueOfWithMinValue() {
        OptionalByte result = optionalByteType.valueOf(String.valueOf(Byte.MIN_VALUE));
        assertNotNull(result);
        assertEquals(Byte.MIN_VALUE, result.get());
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> optionalByteType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> optionalByteType.valueOf("256"));
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalByte result = optionalByteType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithByte() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn((byte) 42);

        OptionalByte result = optionalByteType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) 42, result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonByte() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(100);

        OptionalByte result = optionalByteType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) 100, result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn((byte) 25);

        OptionalByte result = optionalByteType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) 25, result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalByteType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.TINYINT);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalByte empty = OptionalByte.empty();
        optionalByteType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.TINYINT);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalByte opt = OptionalByte.of((byte) 99);
        optionalByteType.set(stmt, 1, opt);
        verify(stmt).setByte(1, (byte) 99);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalByteType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.TINYINT);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalByte empty = OptionalByte.empty();
        optionalByteType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.TINYINT);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalByte opt = OptionalByte.of((byte) -10);
        optionalByteType.set(stmt, "param", opt);
        verify(stmt).setByte("param", (byte) -10);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalByteType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalByte empty = OptionalByte.empty();
        optionalByteType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalByte opt = OptionalByte.of((byte) 127);
        optionalByteType.appendTo(sb, opt);
        assertEquals("127", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        optionalByteType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        OptionalByte empty = OptionalByte.empty();
        optionalByteType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithValue() throws IOException {
        OptionalByte opt = OptionalByte.of((byte) 64);
        optionalByteType.writeCharacter(writer, opt, config);
        verify(writer).write((byte) 64);
    }
}
