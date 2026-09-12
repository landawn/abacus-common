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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class LongTypeTest extends TestBase {

    private LongType longType;

    @BeforeEach
    public void setUp() {
        longType = (LongType) createType("Long");
    }

    @Test
    public void testClazz() {
        assertEquals(Long.class, longType.javaType());
    }

    @Test
    public void testIsPrimitiveWrapper() {
        assertTrue(longType.isPrimitiveWrapper());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        assertNull(longType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByIndex_Long() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Long value = 42L;
        when(rs.getObject(1)).thenReturn(value);

        Long result = longType.get(rs, 1);
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Integer value = 42;
        when(rs.getObject(1)).thenReturn(value);

        Long result = longType.get(rs, 1);
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_Double() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Double value = 42.5;
        when(rs.getObject(1)).thenReturn(value);

        Long result = longType.get(rs, 1);
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String value = "42";
        when(rs.getObject(1)).thenReturn(value);

        Long result = longType.get(rs, 1);
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_InvalidString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String value = "invalid";
        when(rs.getObject(1)).thenReturn(value);

        assertThrows(NumberFormatException.class, () -> longType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("long_column")).thenReturn(null);

        assertNull(longType.get(rs, "long_column"));
    }

    @Test
    public void testGet_ResultSet_ByLabel_Long() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Long value = 42L;
        when(rs.getObject("long_column")).thenReturn(value);

        Long result = longType.get(rs, "long_column");
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Integer value = 42;
        when(rs.getObject("long_column")).thenReturn(value);

        Long result = longType.get(rs, "long_column");
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String value = "42";
        when(rs.getObject("long_column")).thenReturn(value);

        Long result = longType.get(rs, "long_column");
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_NegativeNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String value = "-42";
        when(rs.getObject("long_column")).thenReturn(value);

        Long result = longType.get(rs, "long_column");
        assertEquals(-42L, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_Float() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Float value = 42.9f;
        when(rs.getObject("long_column")).thenReturn(value);

        Long result = longType.get(rs, "long_column");
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_Byte() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Byte value = (byte) 42;
        when(rs.getObject("long_column")).thenReturn(value);

        Long result = longType.get(rs, "long_column");
        assertEquals(42L, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_Short() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Short value = (short) 42;
        when(rs.getObject("long_column")).thenReturn(value);

        Long result = longType.get(rs, "long_column");
        assertEquals(42L, result);
    }

    @Test
    public void test_name() {
        assertEquals("Long", longType.name());
    }

    @Test
    public void test_isLong() {
        assertTrue(longType.isLong());
    }

    @Test
    public void test_stringOf() {
        assertEquals("1000", longType.stringOf(1000L));
        assertEquals("-500", longType.stringOf(-500L));
        assertEquals("0", longType.stringOf(0L));
        assertEquals("9223372036854775807", longType.stringOf(Long.MAX_VALUE));
        assertEquals("-9223372036854775808", longType.stringOf(Long.MIN_VALUE));
        assertNull(longType.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(1000L, longType.valueOf("1000"));
        assertEquals(-500L, longType.valueOf("-500"));
        assertEquals(0L, longType.valueOf("0"));
        assertEquals(Long.MAX_VALUE, longType.valueOf("9223372036854775807"));
        assertEquals(Long.MIN_VALUE, longType.valueOf("-9223372036854775808"));
        assertNull(longType.valueOf((String) null));
        assertNull(longType.valueOf(""));
    }

    @Test
    public void test_valueOf_String_withSuffix() {
        assertEquals(42L, longType.valueOf("42L"));
        assertEquals(42L, longType.valueOf("42l"));
        assertEquals(42L, longType.valueOf("42F"));
        assertEquals(42L, longType.valueOf("42D"));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "9876543210".toCharArray();
        assertEquals(9876543210L, longType.valueOf(chars, 0, 10));

        char[] negChars = "-1234".toCharArray();
        assertEquals(-1234L, longType.valueOf(negChars, 0, 5));

        assertNull(longType.valueOf((char[]) null, 0, 0));
        assertNull(longType.valueOf(chars, 0, 0));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        longType.set(stmt, 1, 88888L);
        verify(stmt).setLong(1, 88888L);

        // Test with null
        longType.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.BIGINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        longType.set(stmt, "param1", 77777L);
        verify(stmt).setLong("param1", 77777L);

        // Test with null
        longType.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.BIGINT);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        longType.appendTo(sw, 66666L);
        assertEquals("66666", sw.toString());

        // Test null
        sw = new StringWriter();
        longType.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_serializeTo_null() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        longType.serializeTo(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    public void test_serializeTo_withValue() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        longType.serializeTo(writer, 42L, null);
        verify(writer).write(42L);
    }

    // T4-01: valueOf(char[]) rejected every hex token up to 18 chars (the long fast path), valueOf(String) accepted them
    @Test
    public void reviewFixes20260906_valueOf_charArray_radixPrefix_matchesStringPath() {
        for (String s : new String[] { "0x1F", "#1F", "-0x1F", "+0x1F", "0X1f", "0x1F000000", "0x7FFFFFFFFFFFFFFF", "-0x8000000000000000", "0xFFFFFFFF" }) {
            char[] cbuf = s.toCharArray();
            assertEquals(longType.valueOf(s), longType.valueOf(cbuf, 0, cbuf.length), s);
        }

        assertEquals(31L, longType.valueOf("0x1F".toCharArray(), 0, 4));
        assertEquals(31L, longType.valueOf("#1F".toCharArray(), 0, 3));
        assertEquals(-31L, longType.valueOf("-0x1F".toCharArray(), 0, 5));
        assertEquals(520093696L, longType.valueOf("0x1F000000".toCharArray(), 0, 10));
        assertEquals(Long.MAX_VALUE, longType.valueOf("0x7FFFFFFFFFFFFFFF".toCharArray(), 0, 18));
        assertEquals(Long.MIN_VALUE, longType.valueOf("-0x8000000000000000".toCharArray(), 0, 19));
        assertEquals(4294967295L, longType.valueOf("0xFFFFFFFF".toCharArray(), 0, 10));
        assertEquals(31L, longType.valueOf("xx0x1Fyy".toCharArray(), 2, 4));
        assertEquals(1L, longType.valueOf("1L".toCharArray(), 0, 2));
    }

    // T4-03: overflow is ArithmeticException (documented now), malformed text stays NumberFormatException, both paths
    @Test
    public void reviewFixes20260906_valueOf_overflowIsArithmetic_malformedIsNfe_onBothPaths() {
        for (String s : new String[] { "9223372036854775808", "-9223372036854775809", "0x8000000000000000", "0xFFFFFFFFFFFFFFFF" }) {
            char[] cbuf = s.toCharArray();
            assertThrows(ArithmeticException.class, () -> longType.valueOf(s), s);
            assertThrows(ArithmeticException.class, () -> longType.valueOf(cbuf, 0, cbuf.length), s);
        }

        for (String s : new String[] { "0x", "0x1G", "#", "12x", "L", " 1", "1 ", "1.0" }) {
            char[] cbuf = s.toCharArray();
            assertThrows(NumberFormatException.class, () -> longType.valueOf(s), s);
            assertThrows(NumberFormatException.class, () -> longType.valueOf(cbuf, 0, cbuf.length), s);
        }

        assertEquals(Long.MAX_VALUE, longType.valueOf("9223372036854775807".toCharArray(), 0, 19));
        assertEquals(Long.MIN_VALUE, longType.valueOf("-9223372036854775808".toCharArray(), 0, 20));
        assertNull(longType.valueOf((char[]) null, 0, 0));
        assertNull(longType.valueOf(new char[0], 0, 0));
    }

    // T4-06 (documented contract, not changed): empty string column -> 0 via Numbers.toLong(String); blank -> NFE
    @Test
    public void reviewFixes20260906_get_emptyStringColumnReadsAsZero() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("");
        when(rs.getObject(2)).thenReturn(" ");
        when(rs.getObject(3)).thenReturn("42");
        when(rs.getObject("e")).thenReturn("");
        when(rs.getObject("b")).thenReturn(" ");
        when(rs.getObject("v")).thenReturn("42");

        assertEquals(0L, longType.get(rs, 1));
        assertThrows(NumberFormatException.class, () -> longType.get(rs, 2));
        assertEquals(42L, longType.get(rs, 3));
        assertEquals(0L, longType.get(rs, "e"));
        assertThrows(NumberFormatException.class, () -> longType.get(rs, "b"));
        assertEquals(42L, longType.get(rs, "v"));
        assertNull(longType.valueOf(""));
    }
}
