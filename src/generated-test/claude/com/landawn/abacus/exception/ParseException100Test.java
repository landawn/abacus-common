package com.landawn.abacus.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class ParseException100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ParseException exception = new ParseException();
        Assertions.assertNotNull(exception);
        Assertions.assertNull(exception.getMessage());
        Assertions.assertNull(exception.getCause());
        Assertions.assertEquals(-2, exception.getToken());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Parse error";
        ParseException exception = new ParseException(message);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertNull(exception.getCause());
        Assertions.assertEquals(-2, exception.getToken());
    }

    @Test
    public void testTokenAndMessageConstructor() {
        int token = 42;
        String message = "Parse error at token";
        ParseException exception = new ParseException(token, message);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertNull(exception.getCause());
        Assertions.assertEquals(token, exception.getToken());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Parse error";
        Throwable cause = new RuntimeException("Underlying cause");
        ParseException exception = new ParseException(message, cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
        Assertions.assertEquals(-2, exception.getToken());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        ParseException exception = new ParseException(cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(cause.toString(), exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
        Assertions.assertEquals(-2, exception.getToken());
    }

    @Test
    public void testGetToken() {
        ParseException exception1 = new ParseException();
        Assertions.assertEquals(-2, exception1.getToken());

        int customToken = 100;
        ParseException exception2 = new ParseException(customToken, "Error");
        Assertions.assertEquals(customToken, exception2.getToken());
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        ParseException exception = new ParseException();
        Assertions.assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        ParseException exception = new ParseException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        // Test that the exception is serializable
        ParseException exception = new ParseException("Test");
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testNegativeToken() {
        int negativeToken = -100;
        ParseException exception = new ParseException(negativeToken, "Error");
        Assertions.assertEquals(negativeToken, exception.getToken());
    }

    @Test
    public void testZeroToken() {
        int zeroToken = 0;
        ParseException exception = new ParseException(zeroToken, "Error");
        Assertions.assertEquals(zeroToken, exception.getToken());
    }
}
