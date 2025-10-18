package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class ByteType100Test extends TestBase {

    private ByteType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (ByteType) createType("Byte");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class result = type.clazz();
        assertEquals(Byte.class, result);
    }

    @Test
    public void testIsPrimitiveWrapper() {
        boolean result = type.isPrimitiveWrapper();
        Assertions.assertTrue(result);
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
}
