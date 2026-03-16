package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UncheckedExceptionTest extends TestBase {

    @Test
    public void testConstructorWithCause() {
        IOException cause = new IOException("IO error");
        UncheckedException exception = new UncheckedException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Operation failed";
        IOException cause = new IOException("IO error");
        UncheckedException exception = new UncheckedException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testSuppressedExceptions() {
        IOException cause = new IOException("IO error");
        RuntimeException suppressed = new RuntimeException("Suppressed");
        cause.addSuppressed(suppressed);

        UncheckedException exception = new UncheckedException(cause);
        assertNotNull(exception);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(suppressed, exception.getSuppressed()[0]);
    }

    @Test
    public void testMultipleSuppressedExceptions() {
        IOException cause = new IOException("IO error");
        RuntimeException suppressed1 = new RuntimeException("Suppressed 1");
        RuntimeException suppressed2 = new RuntimeException("Suppressed 2");
        cause.addSuppressed(suppressed1);
        cause.addSuppressed(suppressed2);

        UncheckedException exception = new UncheckedException(cause);
        assertNotNull(exception);
        assertEquals(2, exception.getSuppressed().length);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        IOException cause = new IOException();
        UncheckedException exception = new UncheckedException(cause);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        IOException cause = new IOException("IO error");
        UncheckedException exception = new UncheckedException(cause);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        IOException cause = new IOException();
        UncheckedException exception = new UncheckedException(cause);
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testNullCauseThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new UncheckedException((Throwable) null));
    }

    @Test
    public void testCauseWithNullMessage() {
        IOException cause = new IOException();
        UncheckedException exception = new UncheckedException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.lang.Exception: " + checkedException.getMessage(), exception.getMessage());
        Assertions.assertEquals(checkedException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom error message";
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(message, checkedException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(checkedException, exception.getCause());
    }

    @Test
    public void testSerialVersionUID() {
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithSuppressedExceptions() {
        Exception checkedException = new Exception("Checked exception");
        Exception suppressed1 = new Exception("Suppressed exception 1");
        Exception suppressed2 = new Exception("Suppressed exception 2");
        checkedException.addSuppressed(suppressed1);
        checkedException.addSuppressed(suppressed2);

        UncheckedException exception = new UncheckedException(checkedException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(2, suppressedExceptions.length);
        Assertions.assertEquals(suppressed1, suppressedExceptions[0]);
        Assertions.assertEquals(suppressed2, suppressedExceptions[1]);
    }

    @Test
    public void testWithCause() {
        Exception rootCause = new Exception("Root cause");
        Exception checkedException = new Exception("Checked exception", rootCause);
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertEquals(checkedException, exception.getCause());
        Assertions.assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    public void testNullCauseWithMessageThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new UncheckedException("Error message", null);
        });
    }

    @Test
    public void testEmptyMessage() {
        Exception checkedException = new Exception("");
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertEquals("java.lang.Exception: ", exception.getMessage());
    }

    @Test
    public void testNullMessage() {
        Exception checkedException = new Exception((String) null);
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertEquals("java.lang.Exception", exception.getMessage());
    }

    @Test
    public void testWithRuntimeException() {
        RuntimeException runtimeException = new RuntimeException("Runtime exception");
        UncheckedException exception = new UncheckedException(runtimeException);
        Assertions.assertEquals("java.lang.RuntimeException: " + runtimeException.getMessage(), exception.getMessage());
        Assertions.assertEquals(runtimeException, exception.getCause());
    }

    @Test
    public void testEmptySuppressedExceptions() {
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(checkedException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(0, suppressedExceptions.length);
    }

}
