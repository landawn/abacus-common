package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.util.CharacterWriter;

public class AbstractCharacterTypeTest extends TestBase {
    private Type<Character> type;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        type = createType(Character.class);
        characterWriter = createCharacterWriter();
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
    public void testValueOf_String_NumericCode() {
        assertEquals('A', type.valueOf("65"));
        assertEquals('a', type.valueOf("97"));
        assertEquals('0', type.valueOf("48"));
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
        assertEquals('\n', type.valueOf("10"));
    }

    @Test
    public void testValueOf_String_InvalidNumericCode() {
        assertThrows(NumberFormatException.class, () -> type.valueOf("abc"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("-1"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("65536"));
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
    public void testAppendTo_Character() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, 'Z');
        assertEquals("Z", sb.toString());
    }

    @Test
    public void testSerializeTo_Character_NoQuotation() throws IOException {
        assertDoesNotThrow(() -> {
            type.serializeTo(characterWriter, 'A', null);
        });
    }

    @Test
    public void testSerializeTo_Character_NoQuotation_WithConfig() throws IOException {
        assertDoesNotThrow(() -> {
            when(config.getCharQuotation()).thenReturn((char) 0);
            type.serializeTo(characterWriter, 'A', config);
        });
    }

    @Test
    public void testSerializeTo_Character_WithDoubleQuotes() throws IOException {
        assertDoesNotThrow(() -> {
            when(config.getCharQuotation()).thenReturn('"');
            type.serializeTo(characterWriter, 'A', config);
        });
    }

    @Test
    public void testSerializeTo_Character_WithSingleQuotes() throws IOException {
        assertDoesNotThrow(() -> {
            when(config.getCharQuotation()).thenReturn('\'');
            type.serializeTo(characterWriter, 'A', config);
        });
    }

    @Test
    public void testSerializeTo_SingleQuote_WithSingleQuoteQuotation() throws IOException {
        assertDoesNotThrow(() -> {
            when(config.getCharQuotation()).thenReturn('\'');
            type.serializeTo(characterWriter, '\'', config);
        });
    }

    // T4-05: valueOf(char[]) parsed a multi-char region with parseInt(char[]) (type suffix tolerated, ASCII digits
    // only) while valueOf(String) used parseChar (Integer.parseInt): "1L" gave U+0001 from JSON and NFE from XML,
    // and Arabic-Indic digits the reverse. Both overloads now share the parseChar grammar.
    @Test
    public void reviewFixes20260906_charArray_sharesStringGrammar_malformedRejectedOnBothPaths() {
        for (String s : new String[] { "1L", "1d", "65L", "0x41", " 1", "1 ", "12x", "abc", "😀" }) {
            char[] cbuf = s.toCharArray();
            assertThrows(NumberFormatException.class, () -> type.valueOf(s), s);
            assertThrows(NumberFormatException.class, () -> type.valueOf(cbuf, 0, cbuf.length), s);
        }
    }

    @Test
    public void reviewFixes20260906_charArray_sharesStringGrammar_values() {
        // Arabic-Indic digits U+0666 U+0665 spell 65; Integer.parseInt accepts them, so both paths now yield 'A'
        String arabicIndic65 = "٦٥";
        assertEquals(Character.valueOf('A'), type.valueOf(arabicIndic65));
        assertEquals(Character.valueOf('A'), type.valueOf(arabicIndic65.toCharArray(), 0, 2));
        assertEquals(Character.valueOf('A'), type.valueOf("+65"));
        assertEquals(Character.valueOf('A'), type.valueOf("+65".toCharArray(), 0, 3));
        assertEquals(Character.valueOf('￿'), type.valueOf("65535".toCharArray(), 0, 5));
        assertEquals(Character.valueOf('\n'), type.valueOf("10".toCharArray(), 0, 2));
        assertEquals(Character.valueOf('A'), type.valueOf("xx65yy".toCharArray(), 2, 2));
        assertEquals(Character.valueOf('c'), type.valueOf("abcde".toCharArray(), 2, 1));
        assertEquals(Character.valueOf(' '), type.valueOf(" ".toCharArray(), 0, 1));
        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(new char[0], 0, 0));
    }

    @Test
    public void reviewFixes20260906_charArray_outOfRange_isIllegalArgumentOnBothPaths() {
        for (String s : new String[] { "-1", "65536", "1234567890" }) {
            char[] cbuf = s.toCharArray();
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(cbuf, 0, cbuf.length), s);
        }
    }

    public static class CharBean {
        private char c;

        public char getC() {
            return c;
        }

        public void setC(final char c) {
            this.c = c;
        }
    }

    @Test
    public void reviewFixes20260906_jsonAndXmlAgree() {
        assertThrows(NumberFormatException.class, () -> ParserFactory.createJsonParser().deserialize("{\"c\": \"1L\"}", CharBean.class));
        assertThrows(NumberFormatException.class, () -> ParserFactory.createXmlParser().deserialize("<charBean><c>1L</c></charBean>", CharBean.class));

        assertEquals('A', ParserFactory.createJsonParser().deserialize("{\"c\": \"٦٥\"}", CharBean.class).getC());
        assertEquals('A', ParserFactory.createXmlParser().deserialize("<charBean><c>٦٥</c></charBean>", CharBean.class).getC());
        assertEquals('A', ParserFactory.createJsonParser().deserialize("{\"c\": \"65\"}", CharBean.class).getC());
        assertEquals('A', ParserFactory.createJsonParser().deserialize("{\"c\": \"A\"}", CharBean.class).getC());
    }
}
