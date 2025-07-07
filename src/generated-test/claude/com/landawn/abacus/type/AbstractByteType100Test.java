package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class AbstractByteType100Test extends TestBase {
    private Type<Byte> type;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        type = createType(Byte.class);
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOf_ByteValue() {
        assertEquals("0", type.stringOf((byte) 0));
        assertEquals("127", type.stringOf((byte) 127));
        assertEquals("-128", type.stringOf((byte) -128));
        assertEquals("42", type.stringOf((byte) 42));
    }

    @Test
    public void testValueOf_String_Null() {
        Byte result = type.valueOf((String) null);
        assertNull(result); // Should return default value
    }

    @Test
    public void testValueOf_String_Empty() {
        Byte result = type.valueOf("");
        assertNull(result); // Should return default value
    }

    @Test
    public void testValueOf_String_ValidByte() {
        assertEquals((byte) 0, type.valueOf("0"));
        assertEquals((byte) 127, type.valueOf("127"));
        assertEquals((byte) -128, type.valueOf("-128"));
        assertEquals((byte) 42, type.valueOf("42"));
    }

    @Test
    public void testValueOf_String_WithSuffix() {
        assertEquals((byte) 42, type.valueOf("42l"));
        assertEquals((byte) 42, type.valueOf("42L"));
        assertEquals((byte) 42, type.valueOf("42f"));
        assertEquals((byte) 42, type.valueOf("42F"));
        assertEquals((byte) 42, type.valueOf("42d"));
        assertEquals((byte) 42, type.valueOf("42D"));
    }

    @Test
    public void testValueOf_String_InvalidFormat() {
        assertThrows(NumberFormatException.class, () -> type.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> type.valueOf("12.34"));
    }

    @Test
    public void testValueOf_String_OutOfRange() {
        assertThrows(NumberFormatException.class, () -> type.valueOf("128"));
        assertThrows(NumberFormatException.class, () -> type.valueOf("-129"));
        assertThrows(NumberFormatException.class, () -> type.valueOf("256"));
    }

    @Test
    public void testValueOf_CharArray_Null() {
        Byte result = type.valueOf(null, 0, 0);
        assertNull(result); // Should return default value
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        Byte result = type.valueOf(cbuf, 0, 0);
        assertNull(result); // Should return default value
    }

    @Test
    public void testValueOf_CharArray_ValidByte() {
        char[] cbuf = "127".toCharArray();
        assertEquals((byte) 127, type.valueOf(cbuf, 0, 3));

        cbuf = "-128".toCharArray();
        assertEquals((byte) -128, type.valueOf(cbuf, 0, 4));

        cbuf = "0".toCharArray();
        assertEquals((byte) 0, type.valueOf(cbuf, 0, 1));
    }

    @Test
    public void testValueOf_CharArray_WithOffset() {
        char[] cbuf = "xx42yy".toCharArray();
        assertEquals((byte) 42, type.valueOf(cbuf, 2, 2));
    }

    @Test
    public void testValueOf_CharArray_OutOfRange() {
        char[] cbuf = "128".toCharArray();
        assertThrows(NumberFormatException.class, () -> type.valueOf(cbuf, 0, 3));

        char[] cbuf2 = "-129".toCharArray();
        assertThrows(NumberFormatException.class, () -> type.valueOf(cbuf2, 0, 4));
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        when(resultSet.getObject(1)).thenReturn((byte) 42);
        assertEquals((byte) 42, type.get(resultSet, 1));
        verify(resultSet).getObject(1);
    }

    @Test
    public void testGet_ResultSet_ByLabel() throws SQLException {
        when(resultSet.getObject("value")).thenReturn((byte) -10);
        assertEquals((byte) -10, type.get(resultSet, "value"));
        verify(resultSet).getObject("value");
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        type.set(preparedStatement, 1, null);
        verify(preparedStatement).setNull(1, Types.TINYINT);
    }

    @Test
    public void testSet_PreparedStatement_ByteValue() throws SQLException {
        type.set(preparedStatement, 1, (byte) 42);
        verify(preparedStatement).setByte(1, (byte) 42);
    }

    @Test
    public void testSet_PreparedStatement_NumberValue() throws SQLException {
        type.set(preparedStatement, 1, (byte) 100);
        verify(preparedStatement).setByte(1, (byte) 100);

        type.set(preparedStatement, 2, (byte) 42);
        verify(preparedStatement).setByte(2, (byte) 42);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        type.set(callableStatement, "param", null);
        verify(callableStatement).setNull("param", Types.TINYINT);
    }

    @Test
    public void testSet_CallableStatement_ByteValue() throws SQLException {
        type.set(callableStatement, "param", (byte) -5);
        verify(callableStatement).setByte("param", (byte) -5);
    }

    @Test
    public void testSet_CallableStatement_NumberValue() throws SQLException {
        type.set(callableStatement, "param", (byte) 127);
        verify(callableStatement).setByte("param", (byte) 127);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_ByteValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, (byte) 42);
        assertEquals("42", sb.toString());
    }

    @Test
    public void testAppendTo_NumberValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, (byte) 127);
        assertEquals("127", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null_NoConfig() throws IOException {
        type.writeCharacter(characterWriter, null, null);
        // Verify writer.write(NULL_CHAR_ARRAY) was called
    }

    @Test
    public void testWriteCharacter_ByteValue_NoConfig() throws IOException {
        type.writeCharacter(characterWriter, (byte) 42, null);
        // Verify writer.write(42) was called
    }

    @Test
    public void testWriteCharacter_Null_WithWriteNullNumberAsZero() throws IOException {
        when(config.writeNullNumberAsZero()).thenReturn(true);
        type.writeCharacter(characterWriter, null, config);
        // Verify writer.write(0) was called
    }

    @Test
    public void testWriteCharacter_Null_WithoutWriteNullNumberAsZero() throws IOException {
        when(config.writeNullNumberAsZero()).thenReturn(false);
        type.writeCharacter(characterWriter, null, config);
        // Verify writer.write(NULL_CHAR_ARRAY) was called
    }

    @Test
    public void testWriteCharacter_NumberValue() throws IOException {
        type.writeCharacter(characterWriter, (byte) 100, config);
        // Verify writer.write(100) was called
    }
}
