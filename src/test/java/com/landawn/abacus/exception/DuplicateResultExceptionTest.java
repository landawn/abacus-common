package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DuplicateResultExceptionTest extends TestBase {

    @Test
    public void testDefaultConstructor() {
        DuplicateResultException exception = new DuplicateResultException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Duplicated result";
        DuplicateResultException exception = new DuplicateResultException(message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Duplicated result";
        Throwable cause = new RuntimeException("Underlying cause");
        DuplicateResultException exception = new DuplicateResultException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        DuplicateResultException exception = new DuplicateResultException(cause);
        assertNotNull(exception);
        assertEquals(cause.toString(), exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfIllegalStateException() {
        DuplicateResultException exception = new DuplicateResultException();
        assertTrue(exception instanceof IllegalStateException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        DuplicateResultException exception = new DuplicateResultException();
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        DuplicateResultException exception = new DuplicateResultException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        DuplicateResultException exception = new DuplicateResultException("Test");
        assertTrue(exception instanceof java.io.Serializable);
    }

}
