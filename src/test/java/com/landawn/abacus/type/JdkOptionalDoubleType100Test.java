package com.landawn.abacus.type;

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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class JdkOptionalDoubleType100Test extends TestBase {

    private JdkOptionalDoubleType optionalDoubleType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        optionalDoubleType = (JdkOptionalDoubleType) createType("JdkOptionalDouble");
        characterWriter = createCharacterWriter();
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
    public void testStringOf_Null() {
        assertNull(optionalDoubleType.stringOf(null));
    }

    @Test
    public void testStringOf_Empty() {
        assertNull(optionalDoubleType.stringOf(OptionalDouble.empty()));
    }

    @Test
    public void testStringOf_Present() {
        OptionalDouble opt = OptionalDouble.of(42.5);
        assertEquals("42.5", optionalDoubleType.stringOf(opt));
    }

    @Test
    public void testValueOf_Null() {
        OptionalDouble result = optionalDoubleType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
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
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalDouble result = optionalDoubleType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
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
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalDoubleType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.DOUBLE);
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
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalDoubleType.appendTo(sb, null);
        assertEquals("null", sb.toString());
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
    public void testWriteCharacter_Null() throws IOException {
        optionalDoubleType.writeCharacter(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_Empty() throws IOException {
        optionalDoubleType.writeCharacter(characterWriter, OptionalDouble.empty(), null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_Present() throws IOException {
        OptionalDouble opt = OptionalDouble.of(42.5);

        optionalDoubleType.writeCharacter(characterWriter, opt, null);
        verify(characterWriter).write(42.5);
    }

    @Test
    public void testWriteCharacter_WithConfig() throws IOException {
        OptionalDouble opt = OptionalDouble.of(42.5);
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        optionalDoubleType.writeCharacter(characterWriter, opt, config);
        verify(characterWriter).write(42.5);
    }
}
