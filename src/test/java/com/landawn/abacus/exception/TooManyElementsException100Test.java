package com.landawn.abacus.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class TooManyElementsException100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        TooManyElementsException exception = new TooManyElementsException();
        Assertions.assertNotNull(exception);
        Assertions.assertNull(exception.getMessage());
        Assertions.assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Too many elements found";
        TooManyElementsException exception = new TooManyElementsException(message);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertNull(exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Too many elements found";
        Throwable cause = new RuntimeException("Underlying cause");
        TooManyElementsException exception = new TooManyElementsException(message, cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        TooManyElementsException exception = new TooManyElementsException(cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(cause.toString(), exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfIllegalStateException() {
        TooManyElementsException exception = new TooManyElementsException();
        Assertions.assertTrue(exception instanceof IllegalStateException);
    }

    @Test
    public void testStackTrace() {
        TooManyElementsException exception = new TooManyElementsException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        TooManyElementsException exception = new TooManyElementsException("Test");
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }
}
