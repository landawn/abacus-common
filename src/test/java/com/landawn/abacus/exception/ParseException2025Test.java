package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ParseException2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ParseException exception = new ParseException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(-2, exception.getToken());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Parse error";
        ParseException exception = new ParseException(message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(-2, exception.getToken());
    }

    @Test
    public void testTokenAndMessageConstructor() {
        int token = 42;
        String message = "Parse error at token";
        ParseException exception = new ParseException(token, message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(token, exception.getToken());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Parse error";
        Throwable cause = new RuntimeException("Underlying cause");
        ParseException exception = new ParseException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(-2, exception.getToken());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        ParseException exception = new ParseException(cause);
        assertNotNull(exception);
        assertEquals(cause.toString(), exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(-2, exception.getToken());
    }

    @Test
    public void testGetToken() {
        ParseException exception1 = new ParseException();
        assertEquals(-2, exception1.getToken());

        int customToken = 100;
        ParseException exception2 = new ParseException(customToken, "Error");
        assertEquals(customToken, exception2.getToken());
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        ParseException exception = new ParseException();
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        ParseException exception = new ParseException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        ParseException exception = new ParseException("Test");
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testNegativeToken() {
        int negativeToken = -100;
        ParseException exception = new ParseException(negativeToken, "Error");
        assertEquals(negativeToken, exception.getToken());
    }

    @Test
    public void testZeroToken() {
        int zeroToken = 0;
        ParseException exception = new ParseException(zeroToken, "Error");
        assertEquals(zeroToken, exception.getToken());
    }
}
