package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class URLType100Test extends TestBase {

    private URLType urlType;
    private URL testURL;

    @BeforeEach
    public void setUp() throws Exception {
        urlType = (URLType) createType(URL.class);
        testURL = new URL("http://example.com/path");
    }

    @Test
    public void testClazz() {
        Class<?> clazz = urlType.clazz();
        assertNotNull(clazz);
        assertEquals(URL.class, clazz);
    }

    @Test
    public void testStringOf() {
        String result = urlType.stringOf(testURL);
        assertNotNull(result);
        assertEquals("http://example.com/path", result);
    }

    @Test
    public void testStringOfNull() {
        String result = urlType.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        URL result = urlType.valueOf("http://example.com/path");
        assertNotNull(result);
        assertEquals(testURL, result);
    }

    @Test
    public void testValueOfEmptyString() {
        URL result = urlType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        URL result = urlType.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOfInvalidURL() {
        assertThrows(RuntimeException.class, () -> {
            urlType.valueOf("not a valid url");
        });
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getURL(1)).thenReturn(testURL);

        URL result = urlType.get(rs, 1);
        assertNotNull(result);
        assertEquals(testURL, result);
    }

    @Test
    public void testGetFromResultSetByIndexNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getURL(1)).thenReturn(null);

        URL result = urlType.get(rs, 1);
        assertNull(result);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getURL("url_column")).thenReturn(testURL);

        URL result = urlType.get(rs, "url_column");
        assertNotNull(result);
        assertEquals(testURL, result);
    }

    @Test
    public void testGetFromResultSetByLabelNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getURL("url_column")).thenReturn(null);

        URL result = urlType.get(rs, "url_column");
        assertNull(result);
    }

    @Test
    public void testSetInPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        urlType.set(stmt, 1, testURL);

        verify(stmt).setURL(1, testURL);
    }

    @Test
    public void testSetInPreparedStatementNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        urlType.set(stmt, 1, null);

        verify(stmt).setURL(1, null);
    }

    @Test
    public void testSetInCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        urlType.set(stmt, "url_param", testURL);

        verify(stmt).setURL("url_param", testURL);
    }

    @Test
    public void testSetInCallableStatementNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        urlType.set(stmt, "url_param", null);

        verify(stmt).setURL("url_param", null);
    }
}
