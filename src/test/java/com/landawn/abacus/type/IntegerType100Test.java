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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class IntegerType100Test extends TestBase {

    private IntegerType integerType;

    @BeforeEach
    public void setUp() {
        integerType = (IntegerType) createType("Integer");
    }

    @Test
    public void testClazz() {
        assertEquals(Integer.class, integerType.clazz());
    }

    @Test
    public void testIsPrimitiveWrapper() {
        assertTrue(integerType.isPrimitiveWrapper());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        assertNull(integerType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByIndex_Integer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Integer value = 42;
        when(rs.getObject(1)).thenReturn(value);

        Integer result = integerType.get(rs, 1);
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Long value = 42L;
        when(rs.getObject(1)).thenReturn(value);

        Integer result = integerType.get(rs, 1);
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_Double() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Double value = 42.5;
        when(rs.getObject(1)).thenReturn(value);

        Integer result = integerType.get(rs, 1);
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String value = "42";
        when(rs.getObject(1)).thenReturn(value);

        Integer result = integerType.get(rs, 1);
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_InvalidString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String value = "invalid";
        when(rs.getObject(1)).thenReturn(value);

        assertThrows(NumberFormatException.class, () -> integerType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("int_column")).thenReturn(null);

        assertNull(integerType.get(rs, "int_column"));
    }

    @Test
    public void testGet_ResultSet_ByLabel_Integer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Integer value = 42;
        when(rs.getObject("int_column")).thenReturn(value);

        Integer result = integerType.get(rs, "int_column");
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Long value = 42L;
        when(rs.getObject("int_column")).thenReturn(value);

        Integer result = integerType.get(rs, "int_column");
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String value = "42";
        when(rs.getObject("int_column")).thenReturn(value);

        Integer result = integerType.get(rs, "int_column");
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_NegativeNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String value = "-42";
        when(rs.getObject("int_column")).thenReturn(value);

        Integer result = integerType.get(rs, "int_column");
        assertEquals(-42, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_Float() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Float value = 42.9f;
        when(rs.getObject("int_column")).thenReturn(value);

        Integer result = integerType.get(rs, "int_column");
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_Byte() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Byte value = (byte) 42;
        when(rs.getObject("int_column")).thenReturn(value);

        Integer result = integerType.get(rs, "int_column");
        assertEquals(42, result);
    }

    @Test
    public void testGet_ResultSet_ByLabel_Short() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Short value = (short) 42;
        when(rs.getObject("int_column")).thenReturn(value);

        Integer result = integerType.get(rs, "int_column");
        assertEquals(42, result);
    }
}
