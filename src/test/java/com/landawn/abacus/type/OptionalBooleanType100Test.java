package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.u.OptionalBoolean;

@Tag("new-test")
public class OptionalBooleanType100Test extends TestBase {

    private OptionalBooleanType optionalBooleanType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalBooleanType = (OptionalBooleanType) createType("OptionalBoolean");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalBoolean.class, optionalBooleanType.clazz());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalBooleanType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(optionalBooleanType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalBooleanType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalBoolean empty = OptionalBoolean.empty();
        assertNull(optionalBooleanType.stringOf(empty));
    }

    @Test
    public void testStringOfWithTrue() {
        OptionalBoolean optTrue = OptionalBoolean.of(true);
        assertEquals("true", optionalBooleanType.stringOf(optTrue));
    }

    @Test
    public void testStringOfWithFalse() {
        OptionalBoolean optFalse = OptionalBoolean.of(false);
        assertEquals("false", optionalBooleanType.stringOf(optFalse));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalBoolean result = optionalBooleanType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalBoolean result = optionalBooleanType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithTrue() {
        OptionalBoolean result = optionalBooleanType.valueOf("true");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get());
    }

    @Test
    public void testValueOfWithFalse() {
        OptionalBoolean result = optionalBooleanType.valueOf("false");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.get());
    }

    @Test
    public void testValueOfWithOtherValues() {
        assertTrue(optionalBooleanType.valueOf("TRUE").get());
        assertTrue(optionalBooleanType.valueOf("True").get());
        assertTrue(optionalBooleanType.valueOf("1").get());
        assertFalse(optionalBooleanType.valueOf("yes").get());
        assertTrue(optionalBooleanType.valueOf("Y").get());
        assertTrue(optionalBooleanType.valueOf("y").get());

        assertFalse(optionalBooleanType.valueOf("FALSE").get());
        assertFalse(optionalBooleanType.valueOf("False").get());
        assertFalse(optionalBooleanType.valueOf("0").get());
        assertFalse(optionalBooleanType.valueOf("no").get());
        assertFalse(optionalBooleanType.valueOf("N").get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalBoolean result = optionalBooleanType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithBoolean() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(Boolean.TRUE);

        OptionalBoolean result = optionalBooleanType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonBoolean() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("true");

        OptionalBoolean result = optionalBooleanType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get());
    }

    @Test
    public void testGetFromResultSetByLabelWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(null);

        OptionalBoolean result = optionalBooleanType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByLabelWithBoolean() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(Boolean.FALSE);

        OptionalBoolean result = optionalBooleanType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalBooleanType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.BOOLEAN);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalBoolean empty = OptionalBoolean.empty();
        optionalBooleanType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.BOOLEAN);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalBoolean opt = OptionalBoolean.of(true);
        optionalBooleanType.set(stmt, 1, opt);
        verify(stmt).setBoolean(1, true);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalBooleanType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.BOOLEAN);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalBoolean empty = OptionalBoolean.empty();
        optionalBooleanType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.BOOLEAN);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalBoolean opt = OptionalBoolean.of(false);
        optionalBooleanType.set(stmt, "param", opt);
        verify(stmt).setBoolean("param", false);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalBooleanType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalBoolean empty = OptionalBoolean.empty();
        optionalBooleanType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithTrue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalBoolean opt = OptionalBoolean.of(true);
        optionalBooleanType.appendTo(sb, opt);
        assertEquals("true", sb.toString());
    }

    @Test
    public void testAppendToWithFalse() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalBoolean opt = OptionalBoolean.of(false);
        optionalBooleanType.appendTo(sb, opt);
        assertEquals("false", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        optionalBooleanType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        OptionalBoolean empty = OptionalBoolean.empty();
        optionalBooleanType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithTrue() throws IOException {
        OptionalBoolean opt = OptionalBoolean.of(true);
        optionalBooleanType.writeCharacter(writer, opt, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithFalse() throws IOException {
        OptionalBoolean opt = OptionalBoolean.of(false);
        optionalBooleanType.writeCharacter(writer, opt, config);
        verify(writer).write(any(char[].class));
    }
}
