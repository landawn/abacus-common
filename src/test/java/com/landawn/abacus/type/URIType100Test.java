package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class URIType100Test extends TestBase {

    private URIType uriType;
    private URI testURI;

    @BeforeEach
    public void setUp() throws Exception {
        uriType = (URIType) createType(URI.class);
        testURI = URI.create("http://example.com/path");
    }

    @Test
    public void testClazz() {
        Class<?> clazz = uriType.clazz();
        assertNotNull(clazz);
        assertEquals(URI.class, clazz);
    }

    @Test
    public void testStringOf() {
        String result = uriType.stringOf(testURI);
        assertNotNull(result);
        assertEquals("http://example.com/path", result);
    }

    @Test
    public void testStringOfNull() {
        String result = uriType.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        URI result = uriType.valueOf("http://example.com/path");
        assertNotNull(result);
        assertEquals(testURI, result);
    }

    @Test
    public void testValueOfEmptyString() {
        URI result = uriType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        URI result = uriType.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOfInvalidURI() {
        assertThrows(IllegalArgumentException.class, () -> {
            uriType.valueOf("://invalid uri");
        });
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("http://example.com/path");

        URI result = uriType.get(rs, 1);
        assertNotNull(result);
        assertEquals(testURI, result);
    }

    @Test
    public void testGetFromResultSetByIndexNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        URI result = uriType.get(rs, 1);
        assertNull(result);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("uri_column")).thenReturn("http://example.com/path");

        URI result = uriType.get(rs, "uri_column");
        assertNotNull(result);
        assertEquals(testURI, result);
    }

    @Test
    public void testGetFromResultSetByLabelNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("uri_column")).thenReturn(null);

        URI result = uriType.get(rs, "uri_column");
        assertNull(result);
    }

    @Test
    public void testSetInPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        uriType.set(stmt, 1, testURI);

        verify(stmt).setString(1, "http://example.com/path");
    }

    @Test
    public void testSetInPreparedStatementNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        uriType.set(stmt, 1, null);

        verify(stmt).setString(1, null);
    }

    @Test
    public void testSetInCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        uriType.set(stmt, "uri_param", testURI);

        verify(stmt).setString("uri_param", "http://example.com/path");
    }

    @Test
    public void testSetInCallableStatementNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        uriType.set(stmt, "uri_param", null);

        verify(stmt).setString("uri_param", null);
    }
}
