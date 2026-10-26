package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.N;

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
    public void testImplementationHandlerPreservesJdbcValuesAndNulls() throws SQLException {
        final SQLXML value = mock(SQLXML.class);
        final Type<SQLXML> implementationType = TypeFactory.getType(value.getClass());
        final ResultSet rs = mock(ResultSet.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final CallableStatement callableStatement = mock(CallableStatement.class);
        when(rs.getSQLXML(1)).thenReturn(value);
        when(rs.getSQLXML("value")).thenReturn(value);

        assertEquals(value.getClass(), implementationType.javaType());
        assertSame(value, implementationType.get(rs, 1));
        assertSame(value, implementationType.get(rs, "value"));
        assertNull(implementationType.get(rs, 2));
        assertNull(implementationType.get(rs, "empty"));

        implementationType.set(statement, 1, value);
        implementationType.set(statement, 2, null);
        implementationType.set(callableStatement, "value", value);
        implementationType.set(callableStatement, "empty", null);

        verify(statement).setSQLXML(1, value);
        verify(statement).setSQLXML(2, null);
        verify(callableStatement).setSQLXML("value", value);
        verify(callableStatement).setSQLXML("empty", null);
        verifyNoInteractions(value);
    }

    @Test
    public void testIsSerializable() {
        assertFalse(sqlXMLType.isSerializable());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "<root/>", "<?xml version=\"1.0\"?>\n<root name=\"x\">a &amp; b</root>" })
    public void testStringOf(final String xmlText) throws SQLException {
        final SQLXML value = mock(SQLXML.class);
        when(value.getString()).thenReturn(xmlText);
        final Type<SQLXML> implementationType = TypeFactory.getType(value.getClass());

        assertEquals(xmlText, sqlXMLType.stringOf(value));
        assertEquals(xmlText, implementationType.stringOf(value));
        assertEquals(xmlText, N.stringOf(value));
        verify(value, times(3)).getString();
        verifyNoMoreInteractions(value);
    }

    @Test
    public void testStringOfNull() {
        assertNull(sqlXMLType.stringOf(null));
    }

    @Test
    public void testStringOfWrapsSqlException() throws SQLException {
        final SQLXML value = mock(SQLXML.class);
        final SQLException failure = new SQLException("Cannot read JDBC value", "HY000", 17);
        when(value.getString()).thenThrow(failure);
        final Type<SQLXML> implementationType = TypeFactory.getType(value.getClass());

        for (final Type<SQLXML> handler : java.util.List.of(sqlXMLType, implementationType)) {
            final UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> handler.stringOf(value));
            assertSame(failure, thrown.getCause());
            assertEquals("HY000", thrown.getSQLState());
            assertEquals(17, thrown.getErrorCode());
        }
        verify(value, times(2)).getString();
        verifyNoMoreInteractions(value);
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

        assertSame(value, sqlXMLType.valueOf(value));
        assertNull(sqlXMLType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectRejectsForeignValues() {
        assertThrows(UnsupportedOperationException.class, () -> sqlXMLType.valueOf((Object) "x"));
        assertThrows(UnsupportedOperationException.class, () -> sqlXMLType.valueOf(42));
        // the String overload is unchanged
        assertThrows(UnsupportedOperationException.class, () -> sqlXMLType.valueOf("x"));
    }
}
