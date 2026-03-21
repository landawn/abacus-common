package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class ShortTypeTest extends TestBase {

    private final ShortType shortType = new ShortType();

    @Test
    public void test_clazz() {
        assertEquals(Short.class, shortType.javaType());
    }

    @Test
    public void test_isPrimitiveWrapper() {
        assertTrue(shortType.isPrimitiveWrapper());
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with value
        when(rs.getShort(1)).thenReturn((short) 42);
        when(rs.wasNull()).thenReturn(false);
        assertEquals((short) 42, shortType.get(rs, 1));

        // Test with null
        when(rs.getShort(2)).thenReturn((short) 0);
        when(rs.wasNull()).thenReturn(true);
        assertNull(shortType.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with value
        when(rs.getShort("shortCol")).thenReturn((short) 99);
        when(rs.wasNull()).thenReturn(false);
        assertEquals((short) 99, shortType.get(rs, "shortCol"));

        // Test with null
        when(rs.getShort("nullCol")).thenReturn((short) 0);
        when(rs.wasNull()).thenReturn(true);
        assertNull(shortType.get(rs, "nullCol"));
    }

    @Test
    public void test_name() {
        assertEquals("Short", shortType.name());
    }

    @Test
    public void test_isShort() {
        assertTrue(shortType.isShort());
    }

    @Test
    public void test_defaultValue() {
        assertNull(shortType.defaultValue());
    }

    @Test
    public void test_stringOf() {
        assertEquals("100", shortType.stringOf((short) 100));
        assertEquals("-200", shortType.stringOf((short) -200));
        assertEquals("0", shortType.stringOf((short) 0));
        assertEquals("32767", shortType.stringOf(Short.MAX_VALUE));
        assertEquals("-32768", shortType.stringOf(Short.MIN_VALUE));
        assertNull(shortType.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals((short) 100, shortType.valueOf("100"));
        assertEquals((short) -200, shortType.valueOf("-200"));
        assertEquals((short) 0, shortType.valueOf("0"));
        assertEquals(Short.MAX_VALUE, shortType.valueOf("32767"));
        assertEquals(Short.MIN_VALUE, shortType.valueOf("-32768"));
        assertNull(shortType.valueOf((String) null));
        assertNull(shortType.valueOf(""));
    }

    @Test
    public void test_valueOf_String_withSuffix() {
        assertEquals((short) 42, shortType.valueOf("42L"));
        assertEquals((short) 42, shortType.valueOf("42l"));
        assertEquals((short) 42, shortType.valueOf("42F"));
        assertEquals((short) 42, shortType.valueOf("42D"));
    }

    @Test
    public void test_valueOf_String_invalid() {
        assertThrows(NumberFormatException.class, () -> shortType.valueOf("abc"));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "12345".toCharArray();
        assertEquals((short) 12345, shortType.valueOf(chars, 0, 5));

        char[] negChars = "-9999".toCharArray();
        assertEquals((short) -9999, shortType.valueOf(negChars, 0, 5));

        // With offset
        char[] offsetChars = "xx100yy".toCharArray();
        assertEquals((short) 100, shortType.valueOf(offsetChars, 2, 3));

        assertNull(shortType.valueOf((char[]) null, 0, 0));
        assertNull(shortType.valueOf(chars, 0, 0));
    }

    @Test
    public void test_valueOf_charArray_outOfRange() {
        char[] chars = "99999".toCharArray();
        assertThrows(NumberFormatException.class, () -> shortType.valueOf(chars, 0, 5));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        shortType.set(stmt, 1, (short) 400);
        verify(stmt).setShort(1, (short) 400);

        // Test with null
        shortType.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.SMALLINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        shortType.set(stmt, "param1", (short) 500);
        verify(stmt).setShort("param1", (short) 500);

        // Test with null
        shortType.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.SMALLINT);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        shortType.appendTo(sw, (short) 600);
        assertEquals("600", sw.toString());

        // Test null
        sw = new StringWriter();
        shortType.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter_withoutConfig() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        // Test null
        shortType.writeCharacter(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    public void test_writeCharacter_withValue() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        shortType.writeCharacter(writer, (short) 42, null);
        verify(writer).write((short) 42);
    }

}
