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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.u.Nullable;

@Tag("new-test")
public class NullableType100Test extends TestBase {

    private NullableType<String> nullableStringType;
    private NullableType<Integer> nullableIntType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        nullableStringType = (NullableType<String>) createType("Nullable<String>");
        nullableIntType = (NullableType<Integer>) createType("Nullable<Integer>");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(nullableStringType.declaringName());
        assertTrue(nullableStringType.declaringName().contains("Nullable"));
        assertTrue(nullableStringType.declaringName().contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Nullable.class, nullableStringType.clazz());
        assertEquals(Nullable.class, nullableIntType.clazz());
    }

    @Test
    public void testGetElementType() {
        assertNotNull(nullableStringType.getElementType());
        assertEquals("String", nullableStringType.getElementType().name());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = nullableStringType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
        assertEquals("String", paramTypes[0].name());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(nullableStringType.isGenericType());
        assertTrue(nullableIntType.isGenericType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(nullableStringType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmptyNullable() {
        Nullable<String> empty = Nullable.empty();
        assertNull(nullableStringType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        Nullable<String> nullable = Nullable.of("test");
        assertEquals("test", nullableStringType.stringOf(nullable));
    }

    @Test
    public void testValueOfWithNull() {
        Nullable<String> result = nullableStringType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        Nullable<String> result = nullableStringType.valueOf("");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("", result.get());
    }

    @Test
    public void testValueOfWithValue() {
        Nullable<String> result = nullableStringType.valueOf("test");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        Nullable<String> result = nullableStringType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        Nullable<String> result = nullableStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        Nullable<String> result = nullableStringType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByLabelWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(null);

        Nullable<String> result = nullableStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        nullableStringType.set(stmt, 1, null);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Nullable<String> empty = Nullable.empty();
        nullableStringType.set(stmt, 1, empty);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Nullable<String> nullable = Nullable.of("test");
        nullableStringType.set(stmt, 1, nullable);
        verify(stmt).setObject(1, "test");
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        nullableStringType.set(stmt, "param", null);
        verify(stmt).setObject("param", null);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Nullable<String> empty = Nullable.empty();
        nullableStringType.set(stmt, "param", empty);
        verify(stmt).setObject("param", null);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Nullable<String> nullable = Nullable.of("test");
        nullableStringType.set(stmt, "param", nullable);
        verify(stmt).setObject("param", "test");
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        nullableStringType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        Nullable<String> empty = Nullable.empty();
        nullableStringType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        Nullable<String> nullable = Nullable.of("test");
        nullableStringType.appendTo(sb, nullable);
        assertEquals("test", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        nullableStringType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        Nullable<String> empty = Nullable.empty();
        nullableStringType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }
}
