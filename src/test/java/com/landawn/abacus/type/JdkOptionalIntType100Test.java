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
import java.util.OptionalInt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class JdkOptionalIntType100Test extends TestBase {

    private JdkOptionalIntType optionalIntType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        optionalIntType = (JdkOptionalIntType) createType("JdkOptionalInt");
        characterWriter = createCharacterWriter();
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
    public void testStringOf_Null() {
        assertNull(optionalIntType.stringOf(null));
    }

    @Test
    public void testStringOf_Empty() {
        assertNull(optionalIntType.stringOf(OptionalInt.empty()));
    }

    @Test
    public void testStringOf_Present() {
        OptionalInt opt = OptionalInt.of(42);
        assertEquals("42", optionalIntType.stringOf(opt));
    }

    @Test
    public void testValueOf_Null() {
        OptionalInt result = optionalIntType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_EmptyString() {
        OptionalInt result = optionalIntType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_ValidString() {
        OptionalInt result = optionalIntType.valueOf("42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testValueOf_NegativeString() {
        OptionalInt result = optionalIntType.valueOf("-42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(-42, result.getAsInt());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalInt result = optionalIntType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Integer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42);

        OptionalInt result = optionalIntType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testGet_ResultSet_ByIndex_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42L);

        OptionalInt result = optionalIntType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("int_column")).thenReturn(null);

        OptionalInt result = optionalIntType.get(rs, "int_column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Integer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("int_column")).thenReturn(42);

        OptionalInt result = optionalIntType.get(rs, "int_column");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalIntType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.INTEGER);
    }

    @Test
    public void testSet_PreparedStatement_Empty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalIntType.set(stmt, 1, OptionalInt.empty());
        verify(stmt).setNull(1, java.sql.Types.INTEGER);
    }

    @Test
    public void testSet_PreparedStatement_Present() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalInt opt = OptionalInt.of(42);

        optionalIntType.set(stmt, 1, opt);
        verify(stmt).setInt(1, 42);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalIntType.set(stmt, "param_name", null);
        verify(stmt).setNull("param_name", java.sql.Types.INTEGER);
    }

    @Test
    public void testSet_CallableStatement_Empty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalIntType.set(stmt, "param_name", OptionalInt.empty());
        verify(stmt).setNull("param_name", java.sql.Types.INTEGER);
    }

    @Test
    public void testSet_CallableStatement_Present() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalInt opt = OptionalInt.of(42);

        optionalIntType.set(stmt, "param_name", opt);
        verify(stmt).setInt("param_name", 42);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalIntType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalIntType.appendTo(sb, OptionalInt.empty());
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Present() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalInt opt = OptionalInt.of(42);

        optionalIntType.appendTo(sb, opt);
        assertEquals("42", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        optionalIntType.writeCharacter(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_Empty() throws IOException {
        optionalIntType.writeCharacter(characterWriter, OptionalInt.empty(), null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_Present() throws IOException {
        OptionalInt opt = OptionalInt.of(42);

        optionalIntType.writeCharacter(characterWriter, opt, null);
        verify(characterWriter).writeInt(42);
    }

    @Test
    public void testWriteCharacter_WithConfig() throws IOException {
        OptionalInt opt = OptionalInt.of(42);
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        optionalIntType.writeCharacter(characterWriter, opt, config);
        verify(characterWriter).writeInt(42);
    }
}
