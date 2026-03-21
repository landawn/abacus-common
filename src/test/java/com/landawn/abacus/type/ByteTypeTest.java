package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
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

public class ByteTypeTest extends TestBase {

    private final ByteType type = new ByteType();

    @Test
    public void test_clazz() {
        assertEquals(Byte.class, type.javaType());
    }

    @Test
    public void testClazz() {
        Class result = type.javaType();
        assertEquals(Byte.class, result);
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject(1)).thenReturn(null);
        assertNull(type.get(rs, 1));

        // Test with Byte
        when(rs.getObject(2)).thenReturn((byte) 42);
        assertEquals((byte) 42, type.get(rs, 2));

        // Test with Number (Integer)
        when(rs.getObject(3)).thenReturn(100);
        assertEquals((byte) 100, type.get(rs, 3));

        // Test with String
        when(rs.getObject(4)).thenReturn("50");
        assertEquals((byte) 50, type.get(rs, 4));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));

        // Test with Byte
        when(rs.getObject("byteCol")).thenReturn((byte) 15);
        assertEquals((byte) 15, type.get(rs, "byteCol"));

        // Test with Number
        when(rs.getObject("intCol")).thenReturn(75);
        assertEquals((byte) 75, type.get(rs, "intCol"));
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        Byte result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_ByteNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Byte expectedByte = Byte.valueOf((byte) 42);
        when(rs.getObject(1)).thenReturn(expectedByte);

        Byte result = type.get(rs, 1);

        assertEquals(expectedByte, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_IntegerNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Integer intValue = Integer.valueOf(127);
        when(rs.getObject(1)).thenReturn(intValue);

        Byte result = type.get(rs, 1);

        assertEquals(Byte.valueOf((byte) 127), result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_LongNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Long longValue = Long.valueOf(-128L);
        when(rs.getObject(1)).thenReturn(longValue);

        Byte result = type.get(rs, 1);

        assertEquals(Byte.valueOf((byte) -128), result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_DoubleNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Double doubleValue = Double.valueOf(100.7);
        when(rs.getObject(1)).thenReturn(doubleValue);

        Byte result = type.get(rs, 1);

        assertEquals(Byte.valueOf((byte) 100), result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("25");

        Byte result = type.get(rs, 1);

        assertEquals(Byte.valueOf((byte) 25), result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_StringNegative() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("-100");

        Byte result = type.get(rs, 1);

        assertEquals(Byte.valueOf((byte) -100), result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn(null);

        Byte result = type.get(rs, "columnName");

        Assertions.assertNull(result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_ByteNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Byte expectedByte = Byte.valueOf((byte) -1);
        when(rs.getObject("columnName")).thenReturn(expectedByte);

        Byte result = type.get(rs, "columnName");

        assertEquals(expectedByte, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_ShortNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Short shortValue = Short.valueOf((short) 50);
        when(rs.getObject("columnName")).thenReturn(shortValue);

        Byte result = type.get(rs, "columnName");

        assertEquals(Byte.valueOf((byte) 50), result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_FloatNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Float floatValue = Float.valueOf(99.9f);
        when(rs.getObject("columnName")).thenReturn(floatValue);

        Byte result = type.get(rs, "columnName");

        assertEquals(Byte.valueOf((byte) 99), result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_StringValue() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn("0");

        Byte result = type.get(rs, "columnName");

        assertEquals(Byte.valueOf((byte) 0), result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_BoundaryValues() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getObject("maxCol")).thenReturn("127");
        Byte maxResult = type.get(rs, "maxCol");
        assertEquals(Byte.MAX_VALUE, maxResult);

        when(rs.getObject("minCol")).thenReturn("-128");
        Byte minResult = type.get(rs, "minCol");
        assertEquals(Byte.MIN_VALUE, minResult);
    }

    @Test
    public void testGet_ResultSet_String_Overflow() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("overflowCol")).thenReturn(256);

        Byte result = type.get(rs, "overflowCol");

        assertEquals(Byte.valueOf((byte) 0), result);
        verify(rs).getObject("overflowCol");
    }

    @Test
    public void test_name() {
        assertEquals("Byte", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("10", type.stringOf((byte) 10));
        assertEquals("-5", type.stringOf((byte) -5));
        assertEquals("0", type.stringOf((byte) 0));
        assertEquals("127", type.stringOf(Byte.MAX_VALUE));
        assertEquals("-128", type.stringOf(Byte.MIN_VALUE));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals((byte) 10, type.valueOf("10"));
        assertEquals((byte) -5, type.valueOf("-5"));
        assertEquals((byte) 0, type.valueOf("0"));
        assertEquals(Byte.MAX_VALUE, type.valueOf("127"));
        assertEquals(Byte.MIN_VALUE, type.valueOf("-128"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_Object() {
        // Byte input
        assertEquals((byte) 10, type.valueOf(Byte.valueOf((byte) 10)));

        // Number inputs
        assertEquals((byte) 5, type.valueOf(Integer.valueOf(5)));
        assertEquals((byte) 20, type.valueOf(Long.valueOf(20L)));

        // String input
        assertEquals((byte) 15, type.valueOf("15"));

        // Null input
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "42".toCharArray();
        assertEquals((byte) 42, type.valueOf(chars, 0, 2));

        char[] negChars = "-10".toCharArray();
        assertEquals((byte) -10, type.valueOf(negChars, 0, 3));

        // With offset
        char[] offsetChars = "xx25yy".toCharArray();
        assertEquals((byte) 25, type.valueOf(offsetChars, 2, 2));

        assertNull(type.valueOf((char[]) null, 0, 0));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        type.set(stmt, 1, (byte) 20);
        verify(stmt).setByte(1, (byte) 20);

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.TINYINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        type.set(stmt, "param1", (byte) 30);
        verify(stmt).setByte("param1", (byte) 30);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.TINYINT);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        type.appendTo(sw, (byte) 25);
        assertEquals("25", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

}
