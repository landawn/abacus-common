package com.landawn.abacus.type;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

public class ClobType100Test extends TestBase {

    private ClobType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (ClobType) createType("Clob");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class<Clob> result = type.clazz();
        assertEquals(Clob.class, result);
    }

    @Test
    public void testStringOf() {
        Clob clob = mock(Clob.class);
        // Assertions.assertThrows(UnsupportedOperationException.class, () -> type.stringOf(clob));
        type.stringOf(clob);
    }

    @Test
    public void testStringOf_Null() {
        // Assertions.assertThrows(UnsupportedOperationException.class, () -> type.stringOf(null));

        assertNull(type.stringOf(null));
    }

    @Test
    public void testValueOf() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf("test"));
    }

    @Test
    public void testValueOf_Null() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyString() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf(""));
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Clob expectedClob = mock(Clob.class);
        when(rs.getClob(1)).thenReturn(expectedClob);

        Clob result = type.get(rs, 1);

        assertEquals(expectedClob, result);
        verify(rs).getClob(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getClob(1)).thenReturn(null);

        Clob result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getClob(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Clob expectedClob = mock(Clob.class);
        when(rs.getClob("columnName")).thenReturn(expectedClob);

        Clob result = type.get(rs, "columnName");

        assertEquals(expectedClob, result);
        verify(rs).getClob("columnName");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getClob("columnName")).thenReturn(null);

        Clob result = type.get(rs, "columnName");

        Assertions.assertNull(result);
        verify(rs).getClob("columnName");
    }

    @Test
    public void testSet_PreparedStatement_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Clob clob = mock(Clob.class);

        type.set(stmt, 1, clob);

        verify(stmt).setClob(1, clob);
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setClob(1, (Clob) null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Clob clob = mock(Clob.class);

        type.set(stmt, "paramName", clob);

        verify(stmt).setClob("paramName", clob);
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setClob("paramName", (Clob) null);
    }

    @Test
    public void testMultipleOperations() throws SQLException {
        // Test multiple operations on the same type instance
        ResultSet rs = mock(ResultSet.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        Clob clob1 = mock(Clob.class);
        Clob clob2 = mock(Clob.class);

        when(rs.getClob(1)).thenReturn(clob1);
        when(rs.getClob(2)).thenReturn(clob2);

        Clob result1 = type.get(rs, 1);
        Clob result2 = type.get(rs, 2);

        assertEquals(clob1, result1);
        assertEquals(clob2, result2);

        type.set(stmt, 1, result1);
        type.set(stmt, 2, result2);

        verify(stmt).setClob(1, clob1);
        verify(stmt).setClob(2, clob2);
    }
}
