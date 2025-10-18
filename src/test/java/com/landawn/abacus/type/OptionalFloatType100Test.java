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
import com.landawn.abacus.util.u.OptionalFloat;

@Tag("new-test")
public class OptionalFloatType100Test extends TestBase {

    private OptionalFloatType optionalFloatType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalFloatType = (OptionalFloatType) createType("OptionalFloat");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalFloat.class, optionalFloatType.clazz());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalFloatType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(optionalFloatType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalFloatType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalFloat empty = OptionalFloat.empty();
        assertNull(optionalFloatType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        assertEquals("3.14", optionalFloatType.stringOf(opt));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalFloat result = optionalFloatType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalFloat result = optionalFloatType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithValidString() {
        OptionalFloat result = optionalFloatType.valueOf("123.45");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(123.45f, result.get());
    }

    @Test
    public void testValueOfWithScientificNotation() {
        OptionalFloat result = optionalFloatType.valueOf("1.5e3");
        assertNotNull(result);
        assertEquals(1500.0f, result.get());
    }

    @Test
    public void testValueOfWithSpecialValues() {
        assertEquals(Float.POSITIVE_INFINITY, optionalFloatType.valueOf("Infinity").get());
        assertEquals(Float.NEGATIVE_INFINITY, optionalFloatType.valueOf("-Infinity").get());
        assertTrue(Float.isNaN(optionalFloatType.valueOf("NaN").get()));
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> optionalFloatType.valueOf("xyz"));
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalFloat result = optionalFloatType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithFloat() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(99.9f);

        OptionalFloat result = optionalFloatType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(99.9f, result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonFloat() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(50);

        OptionalFloat result = optionalFloatType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(50.0f, result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(2.718f);

        OptionalFloat result = optionalFloatType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2.718f, result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalFloatType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.FLOAT);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalFloat empty = OptionalFloat.empty();
        optionalFloatType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.FLOAT);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalFloat opt = OptionalFloat.of(42.5f);
        optionalFloatType.set(stmt, 1, opt);
        verify(stmt).setFloat(1, 42.5f);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalFloatType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.FLOAT);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalFloat empty = OptionalFloat.empty();
        optionalFloatType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.FLOAT);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalFloat opt = OptionalFloat.of(-7.5f);
        optionalFloatType.set(stmt, "param", opt);
        verify(stmt).setFloat("param", -7.5f);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalFloatType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalFloat empty = OptionalFloat.empty();
        optionalFloatType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalFloat opt = OptionalFloat.of(999.5f);
        optionalFloatType.appendTo(sb, opt);
        assertEquals("999.5", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        optionalFloatType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        OptionalFloat empty = OptionalFloat.empty();
        optionalFloatType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithValue() throws IOException {
        OptionalFloat opt = OptionalFloat.of(1.25f);
        optionalFloatType.writeCharacter(writer, opt, config);
        verify(writer).write(1.25f);
    }
}
