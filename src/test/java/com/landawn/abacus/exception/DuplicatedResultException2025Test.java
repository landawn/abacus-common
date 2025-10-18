package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DuplicatedResultException2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        DuplicatedResultException exception = new DuplicatedResultException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Duplicated result";
        DuplicatedResultException exception = new DuplicatedResultException(message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Duplicated result";
        Throwable cause = new RuntimeException("Underlying cause");
        DuplicatedResultException exception = new DuplicatedResultException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        DuplicatedResultException exception = new DuplicatedResultException(cause);
        assertNotNull(exception);
        assertEquals(cause.toString(), exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfIllegalStateException() {
        DuplicatedResultException exception = new DuplicatedResultException();
        assertTrue(exception instanceof IllegalStateException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        DuplicatedResultException exception = new DuplicatedResultException();
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        DuplicatedResultException exception = new DuplicatedResultException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        DuplicatedResultException exception = new DuplicatedResultException("Test");
        assertTrue(exception instanceof java.io.Serializable);
    }
}
