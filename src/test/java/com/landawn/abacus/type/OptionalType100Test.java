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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class OptionalType100Test extends TestBase {

    private OptionalType<String> optionalStringType;
    private OptionalType<Integer> optionalIntType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalStringType = (OptionalType<String>) createType("Optional<String>");
        optionalIntType = (OptionalType<Integer>) createType("Optional<Integer>");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(optionalStringType.declaringName());
        assertTrue(optionalStringType.declaringName().contains("Optional"));
        assertTrue(optionalStringType.declaringName().contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Optional.class, optionalStringType.clazz());
        assertEquals(Optional.class, optionalIntType.clazz());
    }

    @Test
    public void testGetElementType() {
        assertNotNull(optionalStringType.getElementType());
        assertEquals("String", optionalStringType.getElementType().name());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = optionalStringType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
        assertEquals("String", paramTypes[0].name());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(optionalStringType.isGenericType());
        assertTrue(optionalIntType.isGenericType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalStringType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        Optional<String> empty = Optional.empty();
        assertNull(optionalStringType.stringOf(empty));
    }

    @Test
    public void testStringOfWithValue() {
        Optional<String> optional = Optional.of("test");
        assertEquals("test", optionalStringType.stringOf(optional));
    }

    @Test
    public void testValueOfWithNull() {
        Optional<String> result = optionalStringType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        Optional<String> result = optionalStringType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("", result.get());
    }

    @Test
    public void testValueOfWithValue() {
        Optional<String> result = optionalStringType.valueOf("test");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("test", result.get());
    }

    @Test
    public void testValueOfReturningNull() {
        OptionalType<Object> optionalObjectType = (OptionalType<Object>) createType("Optional<Object>");
        Optional<Object> result = optionalObjectType.valueOf("null");
        assertNotNull(result);
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        Optional<String> result = optionalStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        Optional<String> result = optionalStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        Optional<String> result = optionalStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("test", result.get());
    }

    @Test
    public void testGetFromResultSetByLabelWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn(null);

        Optional<String> result = optionalStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalStringType.set(stmt, 1, null);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Optional<String> empty = Optional.empty();
        optionalStringType.set(stmt, 1, empty);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Optional<String> optional = Optional.of("test");
        optionalStringType.set(stmt, 1, optional);
        verify(stmt).setObject(1, "test");
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalStringType.set(stmt, "param", null);
        verify(stmt).setObject("param", null);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Optional<String> empty = Optional.empty();
        optionalStringType.set(stmt, "param", empty);
        verify(stmt).setObject("param", null);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Optional<String> optional = Optional.of("test");
        optionalStringType.set(stmt, "param", optional);
        verify(stmt).setObject("param", "test");
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalStringType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        Optional<String> empty = Optional.empty();
        optionalStringType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        Optional<String> optional = Optional.of("test");
        optionalStringType.appendTo(sb, optional);
        assertEquals("test", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        optionalStringType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmpty() throws IOException {
        Optional<String> empty = Optional.empty();
        optionalStringType.writeCharacter(writer, empty, config);
        verify(writer).write(any(char[].class));
    }
}
