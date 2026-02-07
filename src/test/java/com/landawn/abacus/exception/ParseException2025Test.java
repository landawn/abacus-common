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
        ParsingException exception = new ParsingException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(-2, exception.getErrorToken());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Parse error";
        ParsingException exception = new ParsingException(message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(-2, exception.getErrorToken());
    }

    @Test
    public void testTokenAndMessageConstructor() {
        int token = 42;
        String message = "Parse error at token";
        ParsingException exception = new ParsingException(token, message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(token, exception.getErrorToken());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Parse error";
        Throwable cause = new RuntimeException("Underlying cause");
        ParsingException exception = new ParsingException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(-2, exception.getErrorToken());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("Underlying cause");
        ParsingException exception = new ParsingException(cause);
        assertNotNull(exception);
        assertEquals(cause.toString(), exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(-2, exception.getErrorToken());
    }

    @Test
    public void testGetToken() {
        ParsingException exception1 = new ParsingException();
        assertEquals(-2, exception1.getErrorToken());

        int customToken = 100;
        ParsingException exception2 = new ParsingException(customToken, "Error");
        assertEquals(customToken, exception2.getErrorToken());
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        ParsingException exception = new ParsingException();
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        ParsingException exception = new ParsingException("Test message");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        ParsingException exception = new ParsingException("Test");
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testNegativeToken() {
        int negativeToken = -100;
        ParsingException exception = new ParsingException(negativeToken, "Error");
        assertEquals(negativeToken, exception.getErrorToken());
    }

    @Test
    public void testZeroToken() {
        int zeroToken = 0;
        ParsingException exception = new ParsingException(zeroToken, "Error");
        assertEquals(zeroToken, exception.getErrorToken());
    }
}
