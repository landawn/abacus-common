package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

public class OptionalTypeTest extends TestBase {

    private final OptionalType optionalStringType = new OptionalType("String");
    private final OptionalType optionalIntType = new OptionalType("int");

    @Test
    public void testDeclaringName() {
        assertNotNull(optionalStringType.declaringName());
        assertTrue(optionalStringType.declaringName().contains("Optional"));
        assertTrue(optionalStringType.declaringName().contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Optional.class, optionalStringType.javaType());
        assertEquals(Optional.class, optionalIntType.javaType());
    }

    @Test
    public void test_clazz() {
        assertNotNull(optionalStringType.javaType());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(optionalStringType.isParameterizedType());
        assertTrue(optionalIntType.isParameterizedType());
    }

    @Test
    public void testStringOfWithValue() {
        Optional<String> optional = Optional.of("test");
        assertEquals("test", optionalStringType.stringOf(optional));
    }

    @Test
    public void testStringOfWithEmpty() {
        Optional<String> empty = Optional.empty();
        assertNull(optionalStringType.stringOf(empty));
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = optionalStringType.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNotNull(result);
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
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = optionalStringType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
        assertEquals("String", paramTypes[0].name());
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> optionalStringType.get(rs, 1));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> optionalStringType.get(rs, "col"));
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
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> optionalStringType.set(stmt, 1, null));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> optionalStringType.set(stmt, "param", null));
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
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();
        optionalStringType.appendTo(sw, null);
        assertNotNull(sw.toString());
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
    public void test_name() {
        assertNotNull(optionalStringType.name());
        assertFalse(optionalStringType.name().isEmpty());
    }

}
