package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UncheckedException2025Test extends TestBase {

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
}
