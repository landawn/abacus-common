package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LongType100Test extends TestBase {

    private LongType longType;

    @BeforeEach
    public void setUp() {
        longType = (LongType) createType("Long");
    }

    @Test
    public void testClazz() {
        assertEquals(Long.class, longType.clazz());
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
}
