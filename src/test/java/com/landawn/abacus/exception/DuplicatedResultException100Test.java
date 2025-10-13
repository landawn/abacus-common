package com.landawn.abacus.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class DuplicatedResultException100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        DuplicatedResultException exception = new DuplicatedResultException();
        Assertions.assertNotNull(exception);
        Assertions.assertNull(exception.getMessage());
        Assertions.assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Duplicate result found";
        DuplicatedResultException exception = new DuplicatedResultException(message);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertNull(exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Duplicate result found";
        Throwable cause = new RuntimeException("Underlying cause");
        DuplicatedResultException exception = new DuplicatedResultException(message, cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        DuplicatedResultException exception = new DuplicatedResultException(cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(cause.toString(), exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfIllegalStateException() {
        DuplicatedResultException exception = new DuplicatedResultException();
        Assertions.assertTrue(exception instanceof IllegalStateException);
    }

    @Test
    public void testStackTrace() {
        DuplicatedResultException exception = new DuplicatedResultException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        DuplicatedResultException exception = new DuplicatedResultException("Test");
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }
}
