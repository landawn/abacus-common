package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UncheckedSQLException2025Test extends TestBase {

    @Test
    public void testConstructorWithCause() {
        SQLException cause = new SQLException("SQL error");
        UncheckedSQLException exception = new UncheckedSQLException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Database operation failed";
        SQLException cause = new SQLException("SQL error");
        UncheckedSQLException exception = new UncheckedSQLException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        SQLException cause = new SQLException();
        UncheckedSQLException exception = new UncheckedSQLException(cause);
        assertTrue(exception instanceof UncheckedException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        SQLException cause = new SQLException();
        UncheckedSQLException exception = new UncheckedSQLException(cause);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        SQLException cause = new SQLException("SQL error");
        UncheckedSQLException exception = new UncheckedSQLException(cause);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        SQLException cause = new SQLException();
        UncheckedSQLException exception = new UncheckedSQLException(cause);
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testSuppressedExceptions() {
        SQLException cause = new SQLException("SQL error");
        RuntimeException suppressed = new RuntimeException("Suppressed");
        cause.addSuppressed(suppressed);

        UncheckedSQLException exception = new UncheckedSQLException(cause);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(suppressed, exception.getSuppressed()[0]);
    }
}
