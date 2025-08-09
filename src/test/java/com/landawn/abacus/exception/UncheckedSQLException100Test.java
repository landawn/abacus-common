package com.landawn.abacus.exception;

import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class UncheckedSQLException100Test extends TestBase {

    @Test
    public void testCauseConstructor() {
        SQLException sqlException = new SQLException("SQL error", "42000", 1234);
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.sql.SQLException: " + sqlException.getMessage(), exception.getMessage());
        Assertions.assertEquals(sqlException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom error message";
        SQLException sqlException = new SQLException("SQL error", "42000", 1234);
        UncheckedSQLException exception = new UncheckedSQLException(message, sqlException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(sqlException, exception.getCause());
    }

    @Test
    public void testGetSQLState() {
        String sqlState = "42000";
        SQLException sqlException = new SQLException("SQL error", sqlState, 1234);
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        Assertions.assertEquals(sqlState, exception.getSQLState());
    }

    @Test
    public void testGetErrorCode() {
        int errorCode = 1234;
        SQLException sqlException = new SQLException("SQL error", "42000", errorCode);
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        Assertions.assertEquals(errorCode, exception.getErrorCode());
    }

    @Test
    public void testGetSQLStateNull() {
        SQLException sqlException = new SQLException("SQL error");
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        Assertions.assertNull(exception.getSQLState());
    }

    @Test
    public void testGetErrorCodeDefault() {
        SQLException sqlException = new SQLException("SQL error");
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        Assertions.assertEquals(0, exception.getErrorCode());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        SQLException sqlException = new SQLException("SQL error");
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        Assertions.assertTrue(exception instanceof com.landawn.abacus.exception.UncheckedException);
    }

    @Test
    public void testStackTrace() {
        SQLException sqlException = new SQLException("SQL error");
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        SQLException sqlException = new SQLException("SQL error");
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithSuppressedExceptions() {
        SQLException sqlException = new SQLException("SQL error");
        SQLException suppressed = new SQLException("Suppressed error");
        sqlException.addSuppressed(suppressed);
        
        UncheckedSQLException exception = new UncheckedSQLException(sqlException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(1, suppressedExceptions.length);
        Assertions.assertEquals(suppressed, suppressedExceptions[0]);
    }

    @Test
    public void testComplexSQLException() {
        SQLException sqlException = new SQLException("SQL error", "23505", 2627);
        UncheckedSQLException exception = new UncheckedSQLException("Unique constraint violation", sqlException);
        Assertions.assertEquals("Unique constraint violation", exception.getMessage());
        Assertions.assertEquals("23505", exception.getSQLState());
        Assertions.assertEquals(2627, exception.getErrorCode());
    }
}
