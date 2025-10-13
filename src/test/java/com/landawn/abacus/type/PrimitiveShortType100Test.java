package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
public class PrimitiveShortType100Test extends TestBase {

    private PrimitiveShortType primitiveShortType;

    @BeforeEach
    public void setUp() {
        primitiveShortType = (PrimitiveShortType) createType("short");
    }

    @Test
    public void testClazz() {
        assertEquals(short.class, primitiveShortType.clazz());
    }

    @Test
    public void testIsPrimitiveType() {
        assertTrue(primitiveShortType.isPrimitiveType());
    }

    @Test
    public void testDefaultValue() {
        assertEquals(Short.valueOf((short) 0), primitiveShortType.defaultValue());
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getObject(1)).thenReturn(Short.valueOf((short) 123));
        assertEquals(Short.valueOf((short) 123), primitiveShortType.get(rs, 1));

        when(rs.getObject(2)).thenReturn(Integer.valueOf(456));
        assertEquals(Short.valueOf((short) 456), primitiveShortType.get(rs, 2));

        when(rs.getObject(3)).thenReturn("789");
        assertEquals(Short.valueOf((short) 789), primitiveShortType.get(rs, 3));

        when(rs.getObject(4)).thenReturn(null);
        assertNull(primitiveShortType.get(rs, 4));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getObject("col1")).thenReturn(Short.valueOf((short) 123));
        assertEquals(Short.valueOf((short) 123), primitiveShortType.get(rs, "col1"));

        when(rs.getObject("col2")).thenReturn(Integer.valueOf(456));
        assertEquals(Short.valueOf((short) 456), primitiveShortType.get(rs, "col2"));

        when(rs.getObject("col3")).thenReturn("789");
        assertEquals(Short.valueOf((short) 789), primitiveShortType.get(rs, "col3"));

        when(rs.getObject("col4")).thenReturn(null);
        assertNull(primitiveShortType.get(rs, "col4"));
    }
}
