package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import com.landawn.abacus.util.u.OptionalChar;

@Tag("new-test")
public class OptionalCharType100Test extends TestBase {

    private OptionalCharType optionalCharType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalCharType = (OptionalCharType) createType("OptionalChar");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalChar.class, optionalCharType.clazz());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalCharType.isComparable());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalCharType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalChar empty = OptionalChar.empty();
        assertNull(optionalCharType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        OptionalChar opt = OptionalChar.of('A');
        assertEquals("A", optionalCharType.stringOf(opt));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalChar result = optionalCharType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalChar result = optionalCharType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithSingleChar() {
        OptionalChar result = optionalCharType.valueOf("X");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals('X', result.get());
    }

    @Test
    public void testValueOfWithNumberString() {
        OptionalChar result = optionalCharType.valueOf("65");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals('A', result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalChar result = optionalCharType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithCharacter() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn('Z');

        OptionalChar result = optionalCharType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals('Z', result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithInteger() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(97);

        OptionalChar result = optionalCharType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals('a', result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("B");

        OptionalChar result = optionalCharType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals('B', result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn('Q');

        OptionalChar result = optionalCharType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals('Q', result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalCharType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.CHAR);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalChar empty = OptionalChar.empty();
        optionalCharType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.CHAR);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalChar opt = OptionalChar.of('M');
        optionalCharType.set(stmt, 1, opt);
        verify(stmt).setInt(1, 'M');
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalCharType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.CHAR);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalChar empty = OptionalChar.empty();
        optionalCharType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.CHAR);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalChar opt = OptionalChar.of('K');
        optionalCharType.set(stmt, "param", opt);
        verify(stmt).setInt("param", 'K');
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalCharType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalChar empty = OptionalChar.empty();
        optionalCharType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalChar opt = OptionalChar.of('$');
        optionalCharType.appendTo(sb, opt);
        assertEquals("$", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        optionalCharType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        OptionalChar empty = OptionalChar.empty();
        optionalCharType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithValueNoQuotation() throws IOException {
        when(config.getCharQuotation()).thenReturn((char) 0);
        OptionalChar opt = OptionalChar.of('@');
        optionalCharType.writeCharacter(writer, opt, config);
        verify(writer).writeCharacter('@');
    }

    @Test
    public void testWriteCharacterWithValueWithQuotation() throws IOException {
        when(config.getCharQuotation()).thenReturn('\'');
        OptionalChar opt = OptionalChar.of('#');
        optionalCharType.writeCharacter(writer, opt, config);
        verify(writer, times(2)).write('\'');
        verify(writer).writeCharacter('#');
    }

    @Test
    public void testWriteCharacterWithNullConfig() throws IOException {
        OptionalChar opt = OptionalChar.of('*');
        optionalCharType.writeCharacter(writer, opt, null);
        verify(writer).writeCharacter('*');
    }
}
