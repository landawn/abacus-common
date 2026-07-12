package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjectNotFoundExceptionTest extends TestBase {

    private static final class InitCauseSuppressingException extends ObjectNotFoundException {

        InitCauseSuppressingException(final String message, final Throwable cause) {
            super(message, cause);
        }

        InitCauseSuppressingException(final Throwable cause) {
            super(cause);
        }

        @Override
        public synchronized Throwable initCause(final Throwable cause) {
            return this;
        }
    }

    @Test
    public void testDefaultConstructor() {
        ObjectNotFoundException exception = new ObjectNotFoundException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Object not found";
        ObjectNotFoundException exception = new ObjectNotFoundException(message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Object not found";
        Throwable cause = new RuntimeException("Underlying cause");
        ObjectNotFoundException exception = new ObjectNotFoundException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        ObjectNotFoundException exception = new ObjectNotFoundException(cause);
        assertNotNull(exception);
        assertEquals(cause.toString(), exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testNullCauseConstructorsLockCauseSlot() {
        ObjectNotFoundException withMessage = new ObjectNotFoundException("Object not found", null);
        ObjectNotFoundException causeOnly = new ObjectNotFoundException((Throwable) null);

        assertNull(withMessage.getCause());
        assertNull(causeOnly.getCause());
        assertThrows(IllegalStateException.class, () -> withMessage.initCause(new RuntimeException("later")));
        assertThrows(IllegalStateException.class, () -> causeOnly.initCause(new RuntimeException("later")));
    }

    @Test
    public void testCauseConstructorsDoNotInvokeSubclassOverride() {
        Throwable cause = new RuntimeException("Underlying cause");

        assertSame(cause, new InitCauseSuppressingException("Object not found", cause).getCause());
        assertSame(cause, new InitCauseSuppressingException(cause).getCause());
    }

    @Test
    public void testIsInstanceOfNoSuchElementException() {
        ObjectNotFoundException exception = new ObjectNotFoundException();
        assertTrue(exception instanceof java.util.NoSuchElementException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        ObjectNotFoundException exception = new ObjectNotFoundException();
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        ObjectNotFoundException exception = new ObjectNotFoundException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        ObjectNotFoundException exception = new ObjectNotFoundException("Test");
        assertTrue(exception instanceof java.io.Serializable);
    }
}
