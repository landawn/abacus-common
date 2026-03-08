package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
        assertEquals(short.class, primitiveShortType.javaType());
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(primitiveShortType.isPrimitive());
    }

    @Test
    public void testDefaultValue() {
        assertEquals(Short.valueOf((short) 0), primitiveShortType.defaultValue());
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getShort(1)).thenReturn(Short.valueOf((short) 123));
        assertEquals(Short.valueOf((short) 123), primitiveShortType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getShort("col1")).thenReturn(Short.valueOf((short) 123));
        assertEquals(Short.valueOf((short) 123), primitiveShortType.get(rs, "col1"));
    }
}
