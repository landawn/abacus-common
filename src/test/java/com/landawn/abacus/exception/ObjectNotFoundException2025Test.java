package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjectNotFoundException2025Test extends TestBase {

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
    public void testIsInstanceOfIllegalStateException() {
        ObjectNotFoundException exception = new ObjectNotFoundException();
        assertTrue(exception instanceof IllegalStateException);
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
