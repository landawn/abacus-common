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

public class FloatTypeTest extends TestBase {

    private final FloatType type = new FloatType();

    @Test
    public void test_clazz() {
        assertEquals(Float.class, type.javaType());
    }

    @Test
    public void test_isPrimitiveWrapper() {
        assertTrue(type.isPrimitiveWrapper());
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject(1)).thenReturn(null);
        assertNull(type.get(rs, 1));

        // Test with Float
        when(rs.getObject(2)).thenReturn(42.5f);
        assertEquals(42.5f, type.get(rs, 2));

        // Test with Number (Double)
        when(rs.getObject(3)).thenReturn(100.75);
        assertEquals(100.75f, type.get(rs, 3));

        // Test with String
        when(rs.getObject(4)).thenReturn("50.25");
        assertEquals(50.25f, type.get(rs, 4));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));

        // Test with Float
        when(rs.getObject("floatCol")).thenReturn(75.5f);
        assertEquals(75.5f, type.get(rs, "floatCol"));

        // Test with Number
        when(rs.getObject("doubleCol")).thenReturn(999.99);
        assertEquals(999.99f, type.get(rs, "doubleCol"));
    }

    @Test
    public void test_name() {
        assertEquals("Float", type.name());
    }

    @Test
    public void test_isFloat() {
        assertTrue(type.isFloat());
    }

    @Test
    public void test_stringOf() {
        assertEquals("10.5", type.stringOf(10.5f));
        assertEquals("-5.25", type.stringOf(-5.25f));
        assertEquals("0.0", type.stringOf(0.0f));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(10.5f, type.valueOf("10.5"));
        assertEquals(-5.25f, type.valueOf("-5.25"));
        assertEquals(0.0f, type.valueOf("0"));
        assertEquals(Float.MAX_VALUE, type.valueOf(String.valueOf(Float.MAX_VALUE)));
        assertEquals(Float.MIN_VALUE, type.valueOf(String.valueOf(Float.MIN_VALUE)));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_Object() {
        // Float input
        assertEquals(20.5f, type.valueOf(Float.valueOf(20.5f)));

        // Number inputs
        assertEquals(50.0f, type.valueOf(Integer.valueOf(50)));
        assertEquals(300.5f, type.valueOf(Double.valueOf(300.5)));

        // String input
        assertEquals(15.75f, type.valueOf("15.75"));

        // Null input
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "123.45".toCharArray();
        assertEquals(123.45f, type.valueOf(chars, 0, 6));

        char[] negChars = "-99.99".toCharArray();
        assertEquals(-99.99f, type.valueOf(negChars, 0, 6));

        assertNull(type.valueOf((char[]) null, 0, 0));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        type.set(stmt, 1, 88.88f);
        verify(stmt).setFloat(1, 88.88f);

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.REAL);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        type.set(stmt, "param1", 77.77f);
        verify(stmt).setFloat("param1", 77.77f);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.REAL);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        type.appendTo(sw, 66.66f);
        assertEquals("66.66", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_serializeTo_null() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        type.serializeTo(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    public void test_valueOf_String_withSuffix() {
        assertEquals(42.0f, type.valueOf("42L"));
        assertEquals(42.0f, type.valueOf("42l"));
        assertEquals(42.0f, type.valueOf("42D"));
    }

    // R-T02 (documented, not changed): stringOf is the argument's own toString, a foreign Number is not narrowed
    @Test
    public void reviewFixes20260906_stringOf_isArgumentsOwnToString() {
        assertEquals("42", type.stringOf(Integer.valueOf(42)));
        assertEquals("42", type.stringOf(Long.valueOf(42L)));
        assertEquals("9223372036854775807", type.stringOf(Long.MAX_VALUE));
        assertEquals("1.5", type.stringOf(1.5f));
        assertEquals("1.5", type.stringOf(1.5d));
        assertEquals("0.1", type.stringOf(0.1d));
        assertEquals("-0.0", type.stringOf(-0.0f));
        assertEquals("NaN", type.stringOf(Float.NaN));
        assertNull(type.stringOf(null));
        // and every such string is still accepted by valueOf(String)
        assertEquals(42.0f, type.valueOf(type.stringOf(Integer.valueOf(42))));
        assertEquals(9.223372E18f, type.valueOf(type.stringOf(Long.MAX_VALUE)));
    }

    // T4-10 (documented, not changed): only null and "" give the default; a blank string trims to "" and is rejected
    @Test
    public void reviewFixes20260906_valueOf_blankStringIsRejected() {
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
        assertThrows(NumberFormatException.class, () -> type.valueOf("  "));
        assertThrows(NumberFormatException.class, () -> type.valueOf("\t"));
        assertEquals(42.0f, type.valueOf(" 42 "));
    }

    // T4-06 (documented contract, not changed): empty string column -> 0.0f via Numbers.toFloat(Object); blank -> NFE
    @Test
    public void reviewFixes20260906_get_emptyStringColumnReadsAsZero() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("");
        when(rs.getObject(2)).thenReturn(" ");
        when(rs.getObject(3)).thenReturn("42");
        when(rs.getObject(4)).thenReturn(" 42 ");
        when(rs.getObject("e")).thenReturn("");
        when(rs.getObject("b")).thenReturn(" ");
        when(rs.getObject("v")).thenReturn("42");

        assertEquals(0.0f, type.get(rs, 1));
        assertThrows(NumberFormatException.class, () -> type.get(rs, 2));
        assertEquals(42.0f, type.get(rs, 3));
        assertEquals(42.0f, type.get(rs, 4));
        assertEquals(0.0f, type.get(rs, "e"));
        assertThrows(NumberFormatException.class, () -> type.get(rs, "b"));
        assertEquals(42.0f, type.get(rs, "v"));
        assertNull(type.valueOf(""));
    }
}
