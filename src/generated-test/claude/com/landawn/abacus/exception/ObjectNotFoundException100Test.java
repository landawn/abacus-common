package com.landawn.abacus.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class ObjectNotFoundException100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ObjectNotFoundException exception = new ObjectNotFoundException();
        Assertions.assertNotNull(exception);
        Assertions.assertNull(exception.getMessage());
        Assertions.assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Object not found";
        ObjectNotFoundException exception = new ObjectNotFoundException(message);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertNull(exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Object not found";
        Throwable cause = new RuntimeException("Underlying cause");
        ObjectNotFoundException exception = new ObjectNotFoundException(message, cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        ObjectNotFoundException exception = new ObjectNotFoundException(cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(cause.toString(), exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfIllegalStateException() {
        ObjectNotFoundException exception = new ObjectNotFoundException();
        Assertions.assertTrue(exception instanceof IllegalStateException);
    }

    @Test
    public void testStackTrace() {
        ObjectNotFoundException exception = new ObjectNotFoundException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        // Test that the exception is serializable
        ObjectNotFoundException exception = new ObjectNotFoundException("Test");
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }
}
