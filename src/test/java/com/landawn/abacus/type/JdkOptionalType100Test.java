package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class JdkOptionalType100Test extends TestBase {

    private JdkOptionalType<String> optionalStringType;
    private JdkOptionalType<Integer> optionalIntegerType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        optionalStringType = (JdkOptionalType<String>) createType("JdkOptional<String>");
        optionalIntegerType = (JdkOptionalType<Integer>) createType("JdkOptional<Integer>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        assertEquals(Optional.class, optionalStringType.clazz());
        assertEquals(Optional.class, optionalIntegerType.clazz());
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(optionalStringType.declaringName());
        assertTrue(optionalStringType.declaringName().contains("JdkOptional"));
    }

    @Test
    public void testGetElementType() {
        assertNotNull(optionalStringType.getElementType());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = optionalStringType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(optionalStringType.isGenericType());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(optionalStringType.stringOf(null));
    }

    @Test
    public void testStringOf_Empty() {
        assertNull(optionalStringType.stringOf(Optional.empty()));
    }

    @Test
    public void testStringOf_Present_String() {
        Optional<String> opt = Optional.of("test");
        assertNotNull(optionalStringType.stringOf(opt));
    }

    @Test
    public void testStringOf_Present_Integer() {
        Optional<Integer> opt = Optional.of(42);
        assertNotNull(optionalIntegerType.stringOf(opt));
    }

    @Test
    public void testValueOf_Null() {
        Optional<String> result = optionalStringType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_ValidString() {
        Optional<String> result = optionalStringType.valueOf("test");
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1, String.class)).thenReturn(null);

        Optional<String> result = optionalStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Present() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        Optional<String> result = optionalStringType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column", String.class)).thenReturn(null);

        Optional<String> result = optionalStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Present() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        Optional<String> result = optionalStringType.get(rs, "column");
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalStringType.set(stmt, 1, null);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSet_PreparedStatement_Empty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalStringType.set(stmt, 1, Optional.empty());
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSet_PreparedStatement_Present() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Optional<String> opt = Optional.of("test");

        optionalStringType.set(stmt, 1, opt);
        verify(stmt).setObject(1, "test");
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalStringType.set(stmt, "param", null);
        verify(stmt).setObject("param", null);
    }

    @Test
    public void testSet_CallableStatement_Empty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalStringType.set(stmt, "param", Optional.empty());
        verify(stmt).setObject("param", null);
    }

    @Test
    public void testSet_CallableStatement_Present() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Optional<String> opt = Optional.of("test");

        optionalStringType.set(stmt, "param", opt);
        verify(stmt).setObject("param", "test");
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalStringType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalStringType.appendTo(sb, Optional.empty());
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Present() throws IOException {
        StringBuilder sb = new StringBuilder();
        Optional<String> opt = Optional.of("test");

        optionalStringType.appendTo(sb, opt);
        assertNotNull(sb.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        optionalStringType.writeCharacter(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_Empty() throws IOException {
        optionalStringType.writeCharacter(characterWriter, Optional.empty(), null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_Present() throws IOException {
        Optional<String> opt = Optional.of("test");
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        optionalStringType.writeCharacter(characterWriter, opt, config);
        verify(characterWriter, times(1)).writeCharacter(anyString());
    }
}
