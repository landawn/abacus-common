package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;

public class DataSourceUtilTest extends TestBase {

    @Test
    public void testReleaseConnectionWithNull() {
        assertDoesNotThrow(() -> {
            DataSourceUtil.releaseConnection(null, null);
        });
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
        assertDoesNotThrow(() -> {
            DataSourceUtil.close((ResultSet) null);
        });
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
    public void testCloseResultSetPreservesLookupFailureWhenCloseAlsoFails() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        when(mockRs.getStatement()).thenThrow(new SQLException("statement lookup"));
        doThrow(new SQLException("result set close")).when(mockRs).close();

        UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> DataSourceUtil.close(mockRs, true));

        assertEquals("statement lookup", thrown.getCause().getMessage());
        assertEquals(1, thrown.getCause().getSuppressed().length);
        assertEquals("result set close", thrown.getCause().getSuppressed()[0].getMessage());
        verify(mockRs).close();
    }

    @Test
    public void testCloseResultSetDoesNotSuppressExceptionOnItself() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        SQLException sharedFailure = new SQLException("shared failure");
        when(mockRs.getStatement()).thenThrow(sharedFailure);
        doThrow(sharedFailure).when(mockRs).close();

        UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> DataSourceUtil.close(mockRs, true));

        assertSame(sharedFailure, thrown.getCause());
        assertEquals(0, sharedFailure.getSuppressed().length);
        verify(mockRs).close();
    }

    @Test
    public void testCloseResultSetInvalidArguments() {
        ResultSet mockRs = mock(ResultSet.class);
        assertThrows(IllegalArgumentException.class, () -> DataSourceUtil.close(mockRs, false, true));
    }

    @Test
    public void testCloseStatement() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        DataSourceUtil.close(mockStmt);
        verify(mockStmt).close();
    }

    @Test
    public void testCloseStatementNull() {
        assertDoesNotThrow(() -> {
            DataSourceUtil.close((Statement) null);
        });
    }

    @Test
    public void testCloseConnection() throws SQLException {
        Connection mockConn = mock(Connection.class);
        DataSourceUtil.close(mockConn);
        verify(mockConn).close();
    }

    @Test
    public void testCloseConnectionNull() {
        assertDoesNotThrow(() -> {
            DataSourceUtil.close((Connection) null);
        });
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
    public void testCloseAllPreservesFirstCloseExceptionAndSuppressesLaterFailures() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        Connection mockConn = mock(Connection.class);

        doThrow(new SQLException("result set")).when(mockRs).close();
        doThrow(new SQLException("statement")).when(mockStmt).close();
        doThrow(new SQLException("connection")).when(mockConn).close();

        UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> DataSourceUtil.close(mockRs, mockStmt, mockConn));

        assertEquals("result set", thrown.getCause().getMessage());
        assertEquals(2, thrown.getCause().getSuppressed().length);
        assertEquals("statement", thrown.getCause().getSuppressed()[0].getMessage());
        assertEquals("connection", thrown.getCause().getSuppressed()[1].getMessage());
        verify(mockRs).close();
        verify(mockStmt).close();
        verify(mockConn).close();
    }

    @Test
    public void testCloseQuietlyResultSet() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        doThrow(new SQLException("Test exception")).when(mockRs).close();

        DataSourceUtil.closeQuietly(mockRs);
        verify(mockRs).close();
    }

    @Test
    public void testCloseQuietlyResultSetNull() {
        assertDoesNotThrow(() -> {
            DataSourceUtil.closeQuietly((ResultSet) null);
        });
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

        DataSourceUtil.closeQuietly(mockStmt);
        verify(mockStmt).close();
    }

    @Test
    public void testCloseQuietlyStatementNull() {
        assertDoesNotThrow(() -> {
            DataSourceUtil.closeQuietly((Statement) null);
        });
    }

    @Test
    public void testCloseQuietlyConnection() throws SQLException {
        Connection mockConn = mock(Connection.class);
        doThrow(new SQLException("Test exception")).when(mockConn).close();

        DataSourceUtil.closeQuietly(mockConn);
        verify(mockConn).close();
    }

    @Test
    public void testCloseQuietlyConnectionNull() {
        assertDoesNotThrow(() -> {
            DataSourceUtil.closeQuietly((Connection) null);
        });
    }

    @Test
    public void testCloseQuietlyResultSetAndStatement() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);

        doThrow(new SQLException("Test exception")).when(mockRs).close();
        doThrow(new SQLException("Test exception")).when(mockStmt).close();

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

        DataSourceUtil.closeQuietly(mockRs, mockStmt, mockConn);

        verify(mockRs).close();
        verify(mockStmt).close();
        verify(mockConn).close();
    }

    @Test
    public void testCloseQuietlyWithNullResources() {
        assertDoesNotThrow(() -> {
            DataSourceUtil.closeQuietly(null, null, null);
        });
    }

    @Test
    public void testCloseQuietlyResultSetWithStatementException() throws SQLException {
        for (Exception failure : new Exception[] { new SQLException("Test exception"), new IllegalStateException("Lookup failed") }) {
            ResultSet mockRs = mock(ResultSet.class);
            when(mockRs.getStatement()).thenThrow(failure);

            assertDoesNotThrow(() -> DataSourceUtil.closeQuietly(mockRs, true));

            verify(mockRs).close();
        }
    }

    @Test
    public void testCloseQuietlyResultSetWithConnectionException() throws SQLException {
        for (Exception failure : new Exception[] { new SQLException("Test exception"), new IllegalStateException("Lookup failed") }) {
            ResultSet mockRs = mock(ResultSet.class);
            Statement mockStmt = mock(Statement.class);

            when(mockRs.getStatement()).thenReturn(mockStmt);
            when(mockStmt.getConnection()).thenThrow(failure);

            assertDoesNotThrow(() -> DataSourceUtil.closeQuietly(mockRs, true, true));

            verify(mockRs).close();
            verify(mockStmt).close();
        }
    }

    @Test
    public void testCloseQuietlyLookupErrorsStillPropagateAfterCleanup() throws SQLException {
        for (boolean failConnectionLookup : new boolean[] { false, true }) {
            ResultSet mockRs = mock(ResultSet.class);
            Statement mockStmt = mock(Statement.class);
            AssertionError failure = new AssertionError("Lookup error");
            if (failConnectionLookup) {
                when(mockRs.getStatement()).thenReturn(mockStmt);
                when(mockStmt.getConnection()).thenThrow(failure);
            } else {
                when(mockRs.getStatement()).thenThrow(failure);
            }

            assertSame(failure, assertThrows(AssertionError.class, () -> DataSourceUtil.closeQuietly(mockRs, true, true)));
            verify(mockRs).close();
            if (failConnectionLookup) {
                verify(mockStmt).close();
            }
        }
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

        verify(mockStmt).clearBatch();
    }

    @Test
    public void testExecuteBatchWithClearBatchException() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        int[] expected = { 1, 2, 3 };
        when(mockStmt.executeBatch()).thenReturn(expected);
        doThrow(new SQLException("Clear batch exception")).when(mockStmt).clearBatch();

        int[] result = DataSourceUtil.executeBatch(mockStmt);

        assertArrayEquals(expected, result);
        verify(mockStmt).executeBatch();
        verify(mockStmt).clearBatch();
    }

    @Test
    public void testCloseQuietlyClosesRemainingResourcesAfterCloseError() throws SQLException {
        ResultSet mockRs = mock(ResultSet.class);
        Statement mockStmt = mock(Statement.class);
        Connection mockConn = mock(Connection.class);
        AssertionError rsFailure = new AssertionError("ResultSet close error");
        AssertionError connFailure = new AssertionError("Connection close error");
        doThrow(rsFailure).when(mockRs).close();
        doThrow(new SQLException("Statement close failure")).when(mockStmt).close();
        doThrow(connFailure).when(mockConn).close();

        assertSame(rsFailure, assertThrows(AssertionError.class, () -> DataSourceUtil.closeQuietly(mockRs, mockStmt, mockConn)));
        verify(mockStmt).close();
        verify(mockConn).close();
        assertArrayEquals(new Throwable[] { connFailure }, rsFailure.getSuppressed());

        ResultSet rs2 = mock(ResultSet.class);
        Statement stmt2 = mock(Statement.class);
        AssertionError rs2Failure = new AssertionError("ResultSet close error");
        doThrow(rs2Failure).when(rs2).close();
        assertSame(rs2Failure, assertThrows(AssertionError.class, () -> DataSourceUtil.closeQuietly(rs2, stmt2)));
        verify(stmt2).close();

        Statement stmt3 = mock(Statement.class);
        Connection conn3 = mock(Connection.class);
        AssertionError stmt3Failure = new AssertionError("Statement close error");
        doThrow(stmt3Failure).when(stmt3).close();
        assertSame(stmt3Failure, assertThrows(AssertionError.class, () -> DataSourceUtil.closeQuietly(stmt3, conn3)));
        verify(conn3).close();
    }

    /** Builds a JDBC proxy whose close() counts calls and optionally throws; getStatement/getConnection return the given objects. */
    @SuppressWarnings("unchecked")
    static <T> T jdbc(final Class<T> type, final Throwable closeFailure, final Object linked, final AtomicInteger closes) {
        return (T) Proxy.newProxyInstance(DataSourceUtilTest.class.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "close":
                    closes.incrementAndGet();
                    if (closeFailure != null) {
                        throw closeFailure;
                    }
                    return null;
                case "getStatement":
                case "getConnection":
                    return linked;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return type.getSimpleName() + "Proxy";
                case "isClosed":
                    return false;
                default:
                    return null;
            }
        });
    }

    @Test
    public void testCloseResultSetFlags() {
        final AtomicInteger closes = new AtomicInteger();
        DataSourceUtil.close((ResultSet) null, false, false);

        final ResultSet rs = jdbc(ResultSet.class, null, null, closes);
        DataSourceUtil.close(rs, false, false);
        assertEquals(1, closes.get());

        // getStatement() returns null: only the ResultSet is closed
        DataSourceUtil.close(rs, true, true);
        assertEquals(2, closes.get());

        final SQLException failure = new SQLException("rs");
        final ResultSet bad = jdbc(ResultSet.class, failure, null, closes);
        final UncheckedSQLException ex = assertThrows(UncheckedSQLException.class, () -> DataSourceUtil.close(bad, false, false));
        assertSame(failure, ex.getCause());
    }

    @Test
    public void testCloseStatementAndConnectionFailures() {
        final AtomicInteger closes = new AtomicInteger();
        final Statement stmt = jdbc(Statement.class, new SQLException("s"), null, closes);
        assertThrows(UncheckedSQLException.class, () -> DataSourceUtil.close(stmt));
        final Connection conn = jdbc(Connection.class, new SQLException("c"), null, closes);
        assertThrows(UncheckedSQLException.class, () -> DataSourceUtil.close(conn));
        assertEquals(2, closes.get());
    }

    @Test
    public void testClosePairsAndTriples() {
        final AtomicInteger closes = new AtomicInteger();
        DataSourceUtil.close((ResultSet) null, (Statement) null);
        DataSourceUtil.close((Statement) null, (Connection) null);

        final RuntimeException rsFailure = new IllegalStateException("rs");
        final ResultSet rs = jdbc(ResultSet.class, rsFailure, null, closes);
        final Statement stmt = jdbc(Statement.class, new SQLException("s"), null, closes);
        final RuntimeException thrown = assertThrows(IllegalStateException.class, () -> DataSourceUtil.close(rs, stmt));
        assertSame(rsFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);

        final Connection conn = jdbc(Connection.class, new AssertionError("c"), null, closes);
        final Statement okStmt = jdbc(Statement.class, null, null, closes);
        assertThrows(AssertionError.class, () -> DataSourceUtil.close(okStmt, conn));

        final ResultSet okRs = jdbc(ResultSet.class, null, null, closes);
        final Connection badConn = jdbc(Connection.class, new SQLException("c"), null, closes);
        assertThrows(UncheckedSQLException.class, () -> DataSourceUtil.close(okRs, okStmt, badConn));
        assertTrue(closes.get() >= 7);
    }

    @Test
    public void testCloseQuietlyResultSetFlags() {
        final AtomicInteger closes = new AtomicInteger();
        assertDoesNotThrow(() -> DataSourceUtil.closeQuietly((ResultSet) null, false, false));
        final ResultSet rs = jdbc(ResultSet.class, null, null, closes);
        DataSourceUtil.closeQuietly(rs, false, false);
        DataSourceUtil.closeQuietly(rs, true, true);
        assertEquals(2, closes.get());
        assertThrows(IllegalArgumentException.class, () -> DataSourceUtil.closeQuietly(rs, false, true));
    }
}
