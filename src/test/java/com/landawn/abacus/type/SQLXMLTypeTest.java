package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SQLXMLTypeTest extends TestBase {

    private SQLXMLType sqlXMLType;

    @BeforeEach
    public void setUp() {
        sqlXMLType = (SQLXMLType) createType("SQLXML");
    }

    @Test
    public void testClazz() {
        assertEquals(SQLXML.class, sqlXMLType.javaType());
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

    // FINDING R04-6 (2026-09-08): BlobType/ClobType/NClobType were given a valueOf(Object) identity/null override;
    // SQLXMLType was not, so both null and a genuine SQLXML fell through to AbstractType.valueOf(Object), which
    // renders the value with the handler of its own runtime class and feeds the text to the always-throwing
    // valueOf(String).
    @Test
    public void reviewFixes20260908_valueOfObjectReturnsSameInstanceOrNull() {
        final SQLXML value = mock(SQLXML.class);

        assertSame(value, sqlXMLType.valueOf((Object) value));
        assertNull(sqlXMLType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectRejectsForeignValues() {
        assertThrows(UnsupportedOperationException.class, () -> sqlXMLType.valueOf((Object) "x"));
        assertThrows(UnsupportedOperationException.class, () -> sqlXMLType.valueOf((Object) 42));
        // the String overload is unchanged
        assertThrows(UnsupportedOperationException.class, () -> sqlXMLType.valueOf("x"));
    }
}
