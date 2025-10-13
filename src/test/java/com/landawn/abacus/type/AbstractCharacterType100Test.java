package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AbstractCharacterType100Test extends TestBase {
    private Type<Character> type;
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
        type = createType(Character.class);
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOf_Character() {
        assertEquals("a", type.stringOf('a'));
        assertEquals("A", type.stringOf('A'));
        assertEquals("1", type.stringOf('1'));
        assertEquals(" ", type.stringOf(' '));
        assertEquals("\n", type.stringOf('\n'));
    }

    @Test
    public void testValueOf_String_Null() {
        Character result = type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOf_String_Empty() {
        Character result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOf_String_SingleChar() {
        assertEquals('a', type.valueOf("a"));
        assertEquals('Z', type.valueOf("Z"));
        assertEquals('9', type.valueOf("9"));
        assertEquals(' ', type.valueOf(" "));
    }

    @Test
    public void testValueOf_String_NumericCode() {
        assertEquals('A', type.valueOf("65"));
        assertEquals('a', type.valueOf("97"));
        assertEquals('0', type.valueOf("48"));
    }

    @Test
    public void testValueOf_CharArray_Null() {
        Character result = type.valueOf(null, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        Character result = type.valueOf(cbuf, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_SingleChar() {
        char[] cbuf = { 'x' };
        assertEquals('x', type.valueOf(cbuf, 0, 1));
    }

    @Test
    public void testValueOf_CharArray_NumericCode() {
        char[] cbuf = "65".toCharArray();
        assertEquals('A', type.valueOf(cbuf, 0, 2));

        cbuf = "97".toCharArray();
        assertEquals('a', type.valueOf(cbuf, 0, 2));
    }

    @Test
    public void testValueOf_CharArray_WithOffset() {
        char[] cbuf = "abcde".toCharArray();
        assertEquals('c', type.valueOf(cbuf, 2, 1));

        cbuf = "xx65yy".toCharArray();
        assertEquals('A', type.valueOf(cbuf, 2, 2));
    }

    @Test
    public void testGet_ResultSet_ByIndex_NonNull() throws SQLException {
        when(resultSet.getString(1)).thenReturn("X");
        assertEquals('X', type.get(resultSet, 1));
        verify(resultSet).getString(1);
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        when(resultSet.getString(1)).thenReturn(null);
        assertEquals(null, type.get(resultSet, 1));
        verify(resultSet).getString(1);
    }

    @Test
    public void testGet_ResultSet_ByLabel_NonNull() throws SQLException {
        when(resultSet.getString("char_col")).thenReturn("Y");
        assertEquals('Y', type.get(resultSet, "char_col"));
        verify(resultSet).getString("char_col");
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        when(resultSet.getString("char_col")).thenReturn(null);
        assertEquals(null, type.get(resultSet, "char_col"));
        verify(resultSet).getString("char_col");
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        type.set(preparedStatement, 1, null);
        verify(preparedStatement).setNull(1, Types.VARCHAR);
    }

    @Test
    public void testSet_PreparedStatement_Character() throws SQLException {
        type.set(preparedStatement, 1, 'A');
        verify(preparedStatement).setString(1, "A");
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        type.set(callableStatement, "param", null);
        verify(callableStatement).setNull("param", Types.VARCHAR);
    }

    @Test
    public void testSet_CallableStatement_Character() throws SQLException {
        type.set(callableStatement, "param", 'B');
        verify(callableStatement).setString("param", "B");
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Character() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, 'Z');
        assertEquals("Z", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        type.writeCharacter(characterWriter, null, null);
    }

    @Test
    public void testWriteCharacter_Character_NoQuotation() throws IOException {
        type.writeCharacter(characterWriter, 'A', null);
    }

    @Test
    public void testWriteCharacter_Character_NoQuotation_WithConfig() throws IOException {
        when(config.getCharQuotation()).thenReturn((char) 0);
        type.writeCharacter(characterWriter, 'A', config);
    }

    @Test
    public void testWriteCharacter_Character_WithDoubleQuotes() throws IOException {
        when(config.getCharQuotation()).thenReturn('"');
        type.writeCharacter(characterWriter, 'A', config);
    }

    @Test
    public void testWriteCharacter_Character_WithSingleQuotes() throws IOException {
        when(config.getCharQuotation()).thenReturn('\'');
        type.writeCharacter(characterWriter, 'A', config);
    }

    @Test
    public void testWriteCharacter_SingleQuote_WithSingleQuoteQuotation() throws IOException {
        when(config.getCharQuotation()).thenReturn('\'');
        type.writeCharacter(characterWriter, '\'', config);
    }
}
