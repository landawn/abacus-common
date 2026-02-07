package com.landawn.abacus.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ParseException100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ParsingException exception = new ParsingException();
        Assertions.assertNotNull(exception);
        Assertions.assertNull(exception.getMessage());
        Assertions.assertNull(exception.getCause());
        Assertions.assertEquals(-2, exception.getErrorToken());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Parse error";
        ParsingException exception = new ParsingException(message);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertNull(exception.getCause());
        Assertions.assertEquals(-2, exception.getErrorToken());
    }

    @Test
    public void testTokenAndMessageConstructor() {
        int token = 42;
        String message = "Parse error at token";
        ParsingException exception = new ParsingException(token, message);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertNull(exception.getCause());
        Assertions.assertEquals(token, exception.getErrorToken());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Parse error";
        Throwable cause = new RuntimeException("Underlying cause");
        ParsingException exception = new ParsingException(message, cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
        Assertions.assertEquals(-2, exception.getErrorToken());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        ParsingException exception = new ParsingException(cause);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(cause.toString(), exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
        Assertions.assertEquals(-2, exception.getErrorToken());
    }

    @Test
    public void testGetToken() {
        ParsingException exception1 = new ParsingException();
        Assertions.assertEquals(-2, exception1.getErrorToken());

        int customToken = 100;
        ParsingException exception2 = new ParsingException(customToken, "Error");
        Assertions.assertEquals(customToken, exception2.getErrorToken());
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        ParsingException exception = new ParsingException();
        Assertions.assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        ParsingException exception = new ParsingException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        ParsingException exception = new ParsingException("Test");
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testNegativeToken() {
        int negativeToken = -100;
        ParsingException exception = new ParsingException(negativeToken, "Error");
        Assertions.assertEquals(negativeToken, exception.getErrorToken());
    }

    @Test
    public void testZeroToken() {
        int zeroToken = 0;
        ParsingException exception = new ParsingException(zeroToken, "Error");
        Assertions.assertEquals(zeroToken, exception.getErrorToken());
    }
}
