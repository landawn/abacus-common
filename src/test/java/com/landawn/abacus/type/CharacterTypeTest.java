package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class CharacterTypeTest extends TestBase {

    private final CharacterType type = new CharacterType();

    @Test
    public void testClazz() {
        Class<Character> result = type.javaType();
        assertEquals(Character.class, result);
    }

    @Test
    public void test_isPrimitiveWrapper() {
        assertTrue(type.isPrimitiveWrapper());
    }

    @Test
    public void testGet_ResultSet_Int_SingleChar() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("A");

        Character result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('A'), result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_Int_MultipleChars() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("ABC");

        Character result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('A'), result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        Character result = type.get(rs, 1);

        assertNull(result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_Int_EmptyString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("");

        Character result = type.get(rs, 1);

        assertNull(result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_Int_SpecialChar() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("\n");

        Character result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('\n'), result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_String_SingleChar() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("charColumn")).thenReturn("X");

        Character result = type.get(rs, "charColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('X'), result);
        verify(rs).getString("charColumn");
    }

    @Test
    public void testGet_ResultSet_String_LongString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("charColumn")).thenReturn("Hello World");

        Character result = type.get(rs, "charColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('H'), result);
        verify(rs).getString("charColumn");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("charColumn")).thenReturn(null);

        Character result = type.get(rs, "charColumn");

        assertNull(result);
        verify(rs).getString("charColumn");
    }

    @Test
    public void testGet_ResultSet_String_EmptyString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("charColumn")).thenReturn("");

        Character result = type.get(rs, "charColumn");

        assertNull(result);
        verify(rs).getString("charColumn");
    }

    @Test
    public void testGet_ResultSet_String_Digit() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("digitColumn")).thenReturn("7");

        Character result = type.get(rs, "digitColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('7'), result);
        verify(rs).getString("digitColumn");
    }

    @Test
    public void testGet_ResultSet_String_Unicode() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("unicodeColumn")).thenReturn("\u20AC");

        Character result = type.get(rs, "unicodeColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('\u20AC'), result);
        verify(rs).getString("unicodeColumn");
    }

    @Test
    public void testGet_ResultSet_String_Space() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("spaceColumn")).thenReturn(" ");

        Character result = type.get(rs, "spaceColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf(' '), result);
        verify(rs).getString("spaceColumn");
    }

    @Test
    public void test_name() {
        assertEquals("Character", type.name());
    }

    @Test
    public void test_isCharacter() {
        assertTrue(type.isCharacter());
    }

    @Test
    public void test_stringOf() {
        assertEquals("A", type.stringOf('A'));
        assertEquals("Z", type.stringOf('Z'));
        assertEquals("0", type.stringOf('0'));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals('A', type.valueOf("A"));
        assertEquals('Z', type.valueOf("Z"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "A".toCharArray();
        assertEquals('A', type.valueOf(chars, 0, 1));

        // Multi-char array, single char extraction
        char[] multiChars = "Hello".toCharArray();
        assertEquals('H', type.valueOf(multiChars, 0, 1));

        // With offset
        assertEquals('e', type.valueOf(multiChars, 1, 1));

        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        type.set(stmt, 1, 'X');
        verify(stmt).setString(1, "X");

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.VARCHAR);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        type.set(stmt, "param1", 'Y');
        verify(stmt).setString("param1", "Y");

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.VARCHAR);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        type.appendTo(sw, 'D');
        assertEquals("D", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter_null() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        type.writeCharacter(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    public void test_writeCharacter_withValue_noConfig() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        type.writeCharacter(writer, 'A', null);
        verify(writer).writeCharacter('A');
    }

    @Test
    public void test_writeCharacter_withValue_withQuotation() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getCharQuotation()).thenReturn('\'');

        type.writeCharacter(writer, 'A', config);
        verify(writer, times(2)).write('\'');
        verify(writer).writeCharacter('A');
    }

}
