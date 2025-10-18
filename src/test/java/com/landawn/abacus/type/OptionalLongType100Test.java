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
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("new-test")
public class OptionalLongType100Test extends TestBase {

    private OptionalLongType optionalLongType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalLongType = (OptionalLongType) createType("OptionalLong");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalLong.class, optionalLongType.clazz());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalLongType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(optionalLongType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalLongType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalLong empty = OptionalLong.empty();
        assertNull(optionalLongType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        OptionalLong opt = OptionalLong.of(123456789L);
        assertEquals("123456789", optionalLongType.stringOf(opt));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalLong result = optionalLongType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalLong result = optionalLongType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithValidString() {
        OptionalLong result = optionalLongType.valueOf("9876543210");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(9876543210L, result.get());
    }

    @Test
    public void testValueOfWithNegativeValue() {
        OptionalLong result = optionalLongType.valueOf("-1000000");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(-1000000L, result.get());
    }

    @Test
    public void testValueOfWithMaxValue() {
        OptionalLong result = optionalLongType.valueOf(String.valueOf(Long.MAX_VALUE));
        assertNotNull(result);
        assertEquals(Long.MAX_VALUE, result.get());
    }

    @Test
    public void testValueOfWithMinValue() {
        OptionalLong result = optionalLongType.valueOf(String.valueOf(Long.MIN_VALUE));
        assertNotNull(result);
        assertEquals(Long.MIN_VALUE, result.get());
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> optionalLongType.valueOf("invalid"));
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalLong result = optionalLongType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithLong() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42L);

        OptionalLong result = optionalLongType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(42L, result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonLong() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(100);

        OptionalLong result = optionalLongType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(100L, result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(999999L);

        OptionalLong result = optionalLongType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(999999L, result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalLongType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.BIGINT);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalLong empty = OptionalLong.empty();
        optionalLongType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.BIGINT);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalLong opt = OptionalLong.of(123456L);
        optionalLongType.set(stmt, 1, opt);
        verify(stmt).setLong(1, 123456L);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalLongType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.BIGINT);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalLong empty = OptionalLong.empty();
        optionalLongType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.BIGINT);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalLong opt = OptionalLong.of(-789012L);
        optionalLongType.set(stmt, "param", opt);
        verify(stmt).setLong("param", -789012L);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalLongType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalLong empty = OptionalLong.empty();
        optionalLongType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalLong opt = OptionalLong.of(555555555L);
        optionalLongType.appendTo(sb, opt);
        assertEquals("555555555", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        optionalLongType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        OptionalLong empty = OptionalLong.empty();
        optionalLongType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithValue() throws IOException {
        OptionalLong opt = OptionalLong.of(2023L);
        optionalLongType.writeCharacter(writer, opt, config);
        verify(writer).write(2023L);
    }
}
