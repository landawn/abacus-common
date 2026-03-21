package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UncheckedIOExceptionTest extends TestBase {

    @Test
    public void testConstructorWithCause() {
        IOException cause = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "File operation failed";
        IOException cause = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        IOException cause = new IOException();
        UncheckedIOException exception = new UncheckedIOException(cause);
        assertTrue(exception instanceof UncheckedException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        IOException cause = new IOException();
        UncheckedIOException exception = new UncheckedIOException(cause);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        IOException cause = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(cause);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        IOException cause = new IOException();
        UncheckedIOException exception = new UncheckedIOException(cause);
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testSuppressedExceptions() {
        IOException cause = new IOException("IO error");
        RuntimeException suppressed = new RuntimeException("Suppressed");
        cause.addSuppressed(suppressed);

        UncheckedIOException exception = new UncheckedIOException(cause);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(suppressed, exception.getSuppressed()[0]);
    }

    @Test
    public void testCauseConstructor() {
        IOException ioException = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.io.IOException: " + ioException.getMessage(), exception.getMessage());
        Assertions.assertEquals(ioException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom IO error message";
        IOException ioException = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(message, ioException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(ioException, exception.getCause());
    }

    @Test
    public void testSerialVersionUID() {
        IOException ioException = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithSuppressedExceptions() {
        IOException ioException = new IOException("IO error");
        IOException suppressed = new IOException("Suppressed IO error");
        ioException.addSuppressed(suppressed);

        UncheckedIOException exception = new UncheckedIOException(ioException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(1, suppressedExceptions.length);
        Assertions.assertEquals(suppressed, suppressedExceptions[0]);
    }

    @Test
    public void testWithCause() {
        IOException rootCause = new IOException("Root cause");
        IOException ioException = new IOException("IO error", rootCause);
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertEquals(ioException, exception.getCause());
        Assertions.assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    public void testEmptyMessage() {
        IOException ioException = new IOException("");
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertEquals("java.io.IOException: ", exception.getMessage());
    }

    @Test
    public void testNullMessage() {
        IOException ioException = new IOException((String) null);
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertEquals("java.io.IOException", exception.getMessage());
    }

    @Test
    public void testGetCause() {
        IOException cause = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(cause);
        IOException returnedCause = exception.getCause();
        assertNotNull(returnedCause);
        assertEquals(cause, returnedCause);
        assertTrue(returnedCause instanceof IOException);
    }

    @Test
    public void testGetCause_WithMessageConstructor() {
        IOException cause = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException("wrapper message", cause);
        IOException returnedCause = exception.getCause();
        assertNotNull(returnedCause);
        assertEquals(cause, returnedCause);
        assertEquals("IO error", returnedCause.getMessage());
    }

}
