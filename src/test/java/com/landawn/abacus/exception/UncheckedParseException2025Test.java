package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UncheckedParseException2025Test extends TestBase {

    @Test
    public void testConstructorWithCause() {
        java.text.ParseException cause = new java.text.ParseException("Parse error", 0);
        UncheckedParseException exception = new UncheckedParseException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Date parsing failed";
        java.text.ParseException cause = new java.text.ParseException("Parse error", 0);
        UncheckedParseException exception = new UncheckedParseException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        java.text.ParseException cause = new java.text.ParseException("Parse error", 0);
        UncheckedParseException exception = new UncheckedParseException(cause);
        assertTrue(exception instanceof UncheckedException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        java.text.ParseException cause = new java.text.ParseException("Parse error", 0);
        UncheckedParseException exception = new UncheckedParseException(cause);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        java.text.ParseException cause = new java.text.ParseException("Parse error", 0);
        UncheckedParseException exception = new UncheckedParseException(cause);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        java.text.ParseException cause = new java.text.ParseException("Parse error", 0);
        UncheckedParseException exception = new UncheckedParseException(cause);
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testSuppressedExceptions() {
        java.text.ParseException cause = new java.text.ParseException("Parse error", 0);
        RuntimeException suppressed = new RuntimeException("Suppressed");
        cause.addSuppressed(suppressed);

        UncheckedParseException exception = new UncheckedParseException(cause);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(suppressed, exception.getSuppressed()[0]);
    }
}
