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
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class JdkOptionalLongType100Test extends TestBase {

    private JdkOptionalLongType optionalLongType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        optionalLongType = (JdkOptionalLongType) createType("JdkOptionalLong");
        characterWriter = createCharacterWriter();
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
    public void testStringOf_Null() {
        assertNull(optionalLongType.stringOf(null));
    }

    @Test
    public void testStringOf_Empty() {
        assertNull(optionalLongType.stringOf(OptionalLong.empty()));
    }

    @Test
    public void testStringOf_Present() {
        OptionalLong opt = OptionalLong.of(42L);
        assertEquals("42", optionalLongType.stringOf(opt));
    }

    @Test
    public void testValueOf_Null() {
        OptionalLong result = optionalLongType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_EmptyString() {
        OptionalLong result = optionalLongType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_ValidString() {
        OptionalLong result = optionalLongType.valueOf("42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testValueOf_NegativeString() {
        OptionalLong result = optionalLongType.valueOf("-42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(-42L, result.getAsLong());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalLong result = optionalLongType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Long() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42L);

        OptionalLong result = optionalLongType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testGet_ResultSet_ByIndex_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42);

        OptionalLong result = optionalLongType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("long_column")).thenReturn(null);

        OptionalLong result = optionalLongType.get(rs, "long_column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Long() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("long_column")).thenReturn(42L);

        OptionalLong result = optionalLongType.get(rs, "long_column");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalLongType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_PreparedStatement_Empty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalLongType.set(stmt, 1, OptionalLong.empty());
        verify(stmt).setNull(1, java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_PreparedStatement_Present() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalLong opt = OptionalLong.of(42L);

        optionalLongType.set(stmt, 1, opt);
        verify(stmt).setLong(1, 42L);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalLongType.set(stmt, "param_name", null);
        verify(stmt).setNull("param_name", java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_CallableStatement_Empty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalLongType.set(stmt, "param_name", OptionalLong.empty());
        verify(stmt).setNull("param_name", java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_CallableStatement_Present() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalLong opt = OptionalLong.of(42L);

        optionalLongType.set(stmt, "param_name", opt);
        verify(stmt).setLong("param_name", 42L);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalLongType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalLongType.appendTo(sb, OptionalLong.empty());
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Present() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalLong opt = OptionalLong.of(42L);

        optionalLongType.appendTo(sb, opt);
        assertEquals("42", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        optionalLongType.writeCharacter(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_Empty() throws IOException {
        optionalLongType.writeCharacter(characterWriter, OptionalLong.empty(), null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_Present() throws IOException {
        OptionalLong opt = OptionalLong.of(42L);

        optionalLongType.writeCharacter(characterWriter, opt, null);
        verify(characterWriter).write(42L);
    }

    @Test
    public void testWriteCharacter_WithConfig() throws IOException {
        OptionalLong opt = OptionalLong.of(42L);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        optionalLongType.writeCharacter(characterWriter, opt, config);
        verify(characterWriter).write(42L);
    }
}
