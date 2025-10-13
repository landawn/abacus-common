package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class SQLArrayType100Test extends TestBase {

    private SQLArrayType sqlArrayType;

    @BeforeEach
    public void setUp() {
        sqlArrayType = (SQLArrayType) createType("SQLArray");
    }

    @Test
    public void testClazz() {
        assertEquals(Array.class, sqlArrayType.clazz());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(sqlArrayType.isSerializable());
    }

    @Test
    public void testStringOf() {
        Array array = mock(Array.class);
        assertThrows(UnsupportedOperationException.class, () -> sqlArrayType.stringOf(array));
    }

    @Test
    public void testValueOf() {
        assertThrows(UnsupportedOperationException.class, () -> sqlArrayType.valueOf("test"));
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Array array = mock(Array.class);
        when(rs.getArray(1)).thenReturn(array);

        assertEquals(array, sqlArrayType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Array array = mock(Array.class);
        when(rs.getArray("column")).thenReturn(array);

        assertEquals(array, sqlArrayType.get(rs, "column"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Array array = mock(Array.class);

        sqlArrayType.set(stmt, 1, array);
        verify(stmt).setArray(1, array);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Array array = mock(Array.class);

        sqlArrayType.set(stmt, "param", array);
        verify(stmt).setObject("param", array);
    }
}
