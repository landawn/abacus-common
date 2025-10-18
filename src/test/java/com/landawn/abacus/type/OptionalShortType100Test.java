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
import com.landawn.abacus.util.u.OptionalShort;

@Tag("new-test")
public class OptionalShortType100Test extends TestBase {

    private OptionalShortType optionalShortType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalShortType = (OptionalShortType) createType("OptionalShort");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalShort.class, optionalShortType.clazz());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalShortType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(optionalShortType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalShortType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalShort empty = OptionalShort.empty();
        assertNull(optionalShortType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        OptionalShort opt = OptionalShort.of((short) 1234);
        assertEquals("1234", optionalShortType.stringOf(opt));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalShort result = optionalShortType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalShort result = optionalShortType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithValidString() {
        OptionalShort result = optionalShortType.valueOf("5678");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) 5678, result.get());
    }

    @Test
    public void testValueOfWithNegativeValue() {
        OptionalShort result = optionalShortType.valueOf("-1000");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) -1000, result.get());
    }

    @Test
    public void testValueOfWithMaxValue() {
        OptionalShort result = optionalShortType.valueOf(String.valueOf(Short.MAX_VALUE));
        assertNotNull(result);
        assertEquals(Short.MAX_VALUE, result.get());
    }

    @Test
    public void testValueOfWithMinValue() {
        OptionalShort result = optionalShortType.valueOf(String.valueOf(Short.MIN_VALUE));
        assertNotNull(result);
        assertEquals(Short.MIN_VALUE, result.get());
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> optionalShortType.valueOf("not-a-number"));
        assertThrows(NumberFormatException.class, () -> optionalShortType.valueOf("40000"));
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalShort result = optionalShortType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithShort() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn((short) 42);

        OptionalShort result = optionalShortType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) 42, result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonShort() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(100);

        OptionalShort result = optionalShortType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) 100, result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn((short) 999);

        OptionalShort result = optionalShortType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) 999, result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalShortType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.SMALLINT);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalShort empty = OptionalShort.empty();
        optionalShortType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.SMALLINT);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalShort opt = OptionalShort.of((short) 777);
        optionalShortType.set(stmt, 1, opt);
        verify(stmt).setShort(1, (short) 777);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalShortType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.SMALLINT);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalShort empty = OptionalShort.empty();
        optionalShortType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.SMALLINT);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalShort opt = OptionalShort.of((short) -333);
        optionalShortType.set(stmt, "param", opt);
        verify(stmt).setShort("param", (short) -333);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalShortType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalShort empty = OptionalShort.empty();
        optionalShortType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalShort opt = OptionalShort.of((short) 12345);
        optionalShortType.appendTo(sb, opt);
        assertEquals("12345", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        optionalShortType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        OptionalShort empty = OptionalShort.empty();
        optionalShortType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithValue() throws IOException {
        OptionalShort opt = OptionalShort.of((short) 2023);
        optionalShortType.writeCharacter(writer, opt, config);
        verify(writer).write((short) 2023);
    }
}
