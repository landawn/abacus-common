package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UncheckedInterruptedExceptionTest extends TestBase {

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

    @Test
    public void testCauseConstructor() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.lang.InterruptedException: " + interruptedException.getMessage(), exception.getMessage());
        Assertions.assertEquals(interruptedException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom interruption message";
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(message, interruptedException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(interruptedException, exception.getCause());
    }

    @Test
    public void testSerialVersionUID() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithSuppressedExceptions() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        InterruptedException suppressed = new InterruptedException("Suppressed interruption");
        interruptedException.addSuppressed(suppressed);

        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(1, suppressedExceptions.length);
        Assertions.assertEquals(suppressed, suppressedExceptions[0]);
    }

    @Test
    public void testEmptyMessage() {
        InterruptedException interruptedException = new InterruptedException("");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertEquals("java.lang.InterruptedException: ", exception.getMessage());
    }

    @Test
    public void testNullMessage() {
        InterruptedException interruptedException = new InterruptedException(null);
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertEquals("java.lang.InterruptedException", exception.getMessage());
    }

    @Test
    public void testInterruptedStatus() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);

        Assertions.assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testWithCause() {
        Exception rootCause = new Exception("Root cause");
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        interruptedException.initCause(rootCause);

        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertEquals(interruptedException, exception.getCause());
        Assertions.assertEquals(rootCause, exception.getCause().getCause());
    }

}
