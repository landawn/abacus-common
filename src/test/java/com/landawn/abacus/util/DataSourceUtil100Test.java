package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;

public class DataSourceUtil100Test extends TestBase {

    @Test
    public void testReleaseConnectionWithNull() {
        // Should not throw exception
        DataSourceUtil.releaseConnection(null, null);
    }

    @Test
    public void testReleaseConnectionWithoutDataSource() throws SQLException {
        Connection mockConn = mock(Connection.class);
        DataSourceUtil.releaseConnection(mockConn, null);
        verify(mockConn).close();
    }

    @Test
    public void testCloseResultSet() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        DataSourceUtil.close(mockRs);
        verify(mockRs).close();
    }

    @Test
    public void testCloseResultSetNull() {
        // Should not throw exception
        DataSourceUtil.close((ResultSet)null);
    }

    @Test
    public void testCloseResultSetWithSQLException() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        doThrow(new SQLException("Test exception")).when(mockRs).close();
        
        assertThrows(UncheckedSQLException.class, () -> DataSourceUtil.close(mockRs));
    }

    @Test
    public void testCloseResultSetWithStatement() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        when(mockRs.getStatement()).thenReturn(mockStmt);
        
        DataSourceUtil.close(mockRs, true);
        
        verify(mockRs).close();
        verify(mockStmt).close();
    }

    @Test
    public void testCloseResultSetWithStatementAndConnection() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        Connection mockConn = mock(Connection.class);
        
        when(mockRs.getStatement()).thenReturn(mockStmt);
        when(mockStmt.getConnection()).thenReturn(mockConn);
        
        DataSourceUtil.close(mockRs, true, true);
        
        verify(mockRs).close();
        verify(mockStmt).close();
        verify(mockConn).close();
    }

    @Test
    public void testCloseResultSetInvalidArguments() {
        ResultSet mockRs = mock(ResultSet.class);
        assertThrows(IllegalArgumentException.class, 
            () -> DataSourceUtil.close(mockRs, false, true));
    }

    @Test
    public void testCloseStatement() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        DataSourceUtil.close(mockStmt);
        verify(mockStmt).close();
    }

    @Test
    public void testCloseStatementNull() {
        // Should not throw exception
        DataSourceUtil.close((Statement)null);
    }

    @Test
    public void testCloseConnection() throws SQLException {
        Connection mockConn = mock(Connection.class);
        DataSourceUtil.close(mockConn);
        verify(mockConn).close();
    }

    @Test
    public void testCloseConnectionNull() {
        // Should not throw exception
        DataSourceUtil.close((Connection)null);
    }

    @Test
    public void testCloseResultSetAndStatement() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        
        DataSourceUtil.close(mockRs, mockStmt);
        
        verify(mockRs).close();
        verify(mockStmt).close();
    }

    @Test
    public void testCloseStatementAndConnection() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        Connection mockConn = mock(Connection.class);
        
        DataSourceUtil.close(mockStmt, mockConn);
        
        verify(mockStmt).close();
        verify(mockConn).close();
    }

    @Test
    public void testCloseAll() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        Connection mockConn = mock(Connection.class);
        
        DataSourceUtil.close(mockRs, mockStmt, mockConn);
        
        verify(mockRs).close();
        verify(mockStmt).close();
        verify(mockConn).close();
    }

    @Test
    public void testCloseQuietlyResultSet() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        doThrow(new SQLException("Test exception")).when(mockRs).close();

        // Should not throw exception
        DataSourceUtil.closeQuietly(mockRs);
        verify(mockRs).close();
    }

    @Test
    public void testCloseQuietlyResultSetNull() {
        // Should not throw exception
        DataSourceUtil.closeQuietly((ResultSet) null);
    }

    @Test
    public void testCloseQuietlyResultSetWithStatement() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        when(mockRs.getStatement()).thenReturn(mockStmt);

        DataSourceUtil.closeQuietly(mockRs, true);

        verify(mockRs).close();
        verify(mockStmt).close();
    }

    @Test
    public void testCloseQuietlyResultSetWithStatementAndConnection() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        Connection mockConn = mock(Connection.class);

        when(mockRs.getStatement()).thenReturn(mockStmt);
        when(mockStmt.getConnection()).thenReturn(mockConn);

        DataSourceUtil.closeQuietly(mockRs, true, true);

        verify(mockRs).close();
        verify(mockStmt).close();
        verify(mockConn).close();
    }

    @Test
    public void testCloseQuietlyInvalidArguments() {
        ResultSet mockRs = mock(ResultSet.class);
        assertThrows(IllegalArgumentException.class, () -> DataSourceUtil.closeQuietly(mockRs, false, true));
    }

    @Test
    public void testCloseQuietlyStatement() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        doThrow(new SQLException("Test exception")).when(mockStmt).close();

        // Should not throw exception
        DataSourceUtil.closeQuietly(mockStmt);
        verify(mockStmt).close();
    }

    @Test
    public void testCloseQuietlyStatementNull() {
        // Should not throw exception
        DataSourceUtil.closeQuietly((Statement) null);
    }

    @Test
    public void testCloseQuietlyConnection() throws SQLException {
        Connection mockConn = mock(Connection.class);
        doThrow(new SQLException("Test exception")).when(mockConn).close();

        // Should not throw exception
        DataSourceUtil.closeQuietly(mockConn);
        verify(mockConn).close();
    }

    @Test
    public void testCloseQuietlyConnectionNull() {
        // Should not throw exception
        DataSourceUtil.closeQuietly((Connection) null);
    }

    @Test
    public void testCloseQuietlyResultSetAndStatement() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);

        doThrow(new SQLException("Test exception")).when(mockRs).close();
        doThrow(new SQLException("Test exception")).when(mockStmt).close();

        // Should not throw exception
        DataSourceUtil.closeQuietly(mockRs, mockStmt);

        verify(mockRs).close();
        verify(mockStmt).close();
    }

    @Test
    public void testCloseQuietlyStatementAndConnection() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        Connection mockConn = mock(Connection.class);

        doThrow(new SQLException("Test exception")).when(mockStmt).close();
        doThrow(new SQLException("Test exception")).when(mockConn).close();

        // Should not throw exception
        DataSourceUtil.closeQuietly(mockStmt, mockConn);

        verify(mockStmt).close();
        verify(mockConn).close();
    }

    @Test
    public void testCloseQuietlyAll() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        Connection mockConn = mock(Connection.class);

        doThrow(new SQLException("Test exception")).when(mockRs).close();
        doThrow(new SQLException("Test exception")).when(mockStmt).close();
        doThrow(new SQLException("Test exception")).when(mockConn).close();

        // Should not throw exception
        DataSourceUtil.closeQuietly(mockRs, mockStmt, mockConn);

        verify(mockRs).close();
        verify(mockStmt).close();
        verify(mockConn).close();
    }

    @Test
    public void testExecuteBatch() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        int[] expected = { 1, 2, 3 };
        when(mockStmt.executeBatch()).thenReturn(expected);

        int[] result = DataSourceUtil.executeBatch(mockStmt);

        assertArrayEquals(expected, result);
        verify(mockStmt).executeBatch();
        verify(mockStmt).clearBatch();
    }

    @Test
    public void testExecuteBatchWithException() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        when(mockStmt.executeBatch()).thenThrow(new SQLException("Test exception"));

        assertThrows(SQLException.class, () -> DataSourceUtil.executeBatch(mockStmt));

        // clearBatch should still be called
        verify(mockStmt).clearBatch();
    }

    @Test
    public void testExecuteBatchWithClearBatchException() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        int[] expected = { 1, 2, 3 };
        when(mockStmt.executeBatch()).thenReturn(expected);
        doThrow(new SQLException("Clear batch exception")).when(mockStmt).clearBatch();

        // Should not throw exception from clearBatch
        int[] result = DataSourceUtil.executeBatch(mockStmt);

        assertArrayEquals(expected, result);
        verify(mockStmt).executeBatch();
        verify(mockStmt).clearBatch();
    }

    @Test
    public void testCloseQuietlyWithNullResources() {
        // All null resources should not throw exception
        DataSourceUtil.closeQuietly(null, null, null);
    }

    @Test
    public void testCloseQuietlyResultSetWithStatementException() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        when(mockRs.getStatement()).thenThrow(new SQLException("Test exception"));

        // Should not throw exception
        DataSourceUtil.closeQuietly(mockRs, true);

        verify(mockRs).close();
    }

    @Test
    public void testCloseQuietlyResultSetWithConnectionException() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);

        when(mockRs.getStatement()).thenReturn(mockStmt);
        when(mockStmt.getConnection()).thenThrow(new SQLException("Test exception"));

        // Should not throw exception
        DataSourceUtil.closeQuietly(mockRs, true, true);

        verify(mockRs).close();
        verify(mockStmt).close();
    }
}