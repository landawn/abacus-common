package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UncheckedInterruptedException2025Test extends TestBase {

    @Test
    public void testConstructorWithCause() {
        InterruptedException cause = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Operation interrupted";
        InterruptedException cause = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        InterruptedException cause = new InterruptedException();
        UncheckedInterruptedException exception = new UncheckedInterruptedException(cause);
        assertTrue(exception instanceof UncheckedException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        InterruptedException cause = new InterruptedException();
        UncheckedInterruptedException exception = new UncheckedInterruptedException(cause);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        InterruptedException cause = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(cause);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        InterruptedException cause = new InterruptedException();
        UncheckedInterruptedException exception = new UncheckedInterruptedException(cause);
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testSuppressedExceptions() {
        InterruptedException cause = new InterruptedException("Thread interrupted");
        RuntimeException suppressed = new RuntimeException("Suppressed");
        cause.addSuppressed(suppressed);

        UncheckedInterruptedException exception = new UncheckedInterruptedException(cause);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(suppressed, exception.getSuppressed()[0]);
    }
}
