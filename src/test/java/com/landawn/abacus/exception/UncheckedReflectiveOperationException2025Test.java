package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UncheckedReflectiveOperationException2025Test extends TestBase {

    @Test
    public void testConstructorWithCause() {
        ReflectiveOperationException cause = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Reflection operation failed";
        ReflectiveOperationException cause = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        ReflectiveOperationException cause = new ReflectiveOperationException();
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertTrue(exception instanceof UncheckedException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        ReflectiveOperationException cause = new ReflectiveOperationException();
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        ReflectiveOperationException cause = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        ReflectiveOperationException cause = new ReflectiveOperationException();
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testSuppressedExceptions() {
        ReflectiveOperationException cause = new ReflectiveOperationException("Reflection error");
        RuntimeException suppressed = new RuntimeException("Suppressed");
        cause.addSuppressed(suppressed);

        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(suppressed, exception.getSuppressed()[0]);
    }
}
