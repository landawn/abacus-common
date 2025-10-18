package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class TooManyElementsException2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        TooManyElementsException exception = new TooManyElementsException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Too many elements";
        TooManyElementsException exception = new TooManyElementsException(message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Too many elements";
        Throwable cause = new RuntimeException("Underlying cause");
        TooManyElementsException exception = new TooManyElementsException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        TooManyElementsException exception = new TooManyElementsException(cause);
        assertNotNull(exception);
        assertEquals(cause.toString(), exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfIllegalStateException() {
        TooManyElementsException exception = new TooManyElementsException();
        assertTrue(exception instanceof IllegalStateException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        TooManyElementsException exception = new TooManyElementsException();
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        TooManyElementsException exception = new TooManyElementsException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        TooManyElementsException exception = new TooManyElementsException("Test");
        assertTrue(exception instanceof java.io.Serializable);
    }
}
