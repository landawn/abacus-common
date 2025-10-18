package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class SQLXMLType100Test extends TestBase {

    private SQLXMLType sqlXMLType;

    @BeforeEach
    public void setUp() {
        sqlXMLType = (SQLXMLType) createType("SQLXML");
    }

    @Test
    public void testClazz() {
        assertEquals(SQLXML.class, sqlXMLType.clazz());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(sqlXMLType.isSerializable());
    }

    @Test
    public void testStringOf() {
        SQLXML sqlxml = mock(SQLXML.class);
        assertThrows(UnsupportedOperationException.class, () -> sqlXMLType.stringOf(sqlxml));
    }

    @Test
    public void testValueOf() {
        assertThrows(UnsupportedOperationException.class, () -> sqlXMLType.valueOf("test"));
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        SQLXML sqlxml = mock(SQLXML.class);
        when(rs.getSQLXML(1)).thenReturn(sqlxml);

        assertEquals(sqlxml, sqlXMLType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        SQLXML sqlxml = mock(SQLXML.class);
        when(rs.getSQLXML("column")).thenReturn(sqlxml);

        assertEquals(sqlxml, sqlXMLType.get(rs, "column"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        SQLXML sqlxml = mock(SQLXML.class);

        sqlXMLType.set(stmt, 1, sqlxml);
        verify(stmt).setSQLXML(1, sqlxml);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        SQLXML sqlxml = mock(SQLXML.class);

        sqlXMLType.set(stmt, "param", sqlxml);
        verify(stmt).setSQLXML("param", sqlxml);
    }
}
