package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UncheckedParseExceptionTest extends TestBase {

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

    @Test
    public void testCauseConstructor() {
        ParseException parseException = new ParseException("Parse error", 10);
        UncheckedParseException exception = new UncheckedParseException(parseException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.text.ParseException: " + parseException.getMessage(), exception.getMessage());
        Assertions.assertEquals(parseException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom parse error message";
        ParseException parseException = new ParseException("Parse error", 10);
        UncheckedParseException exception = new UncheckedParseException(message, parseException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(parseException, exception.getCause());
    }

    @Test
    public void testSerialVersionUID() {
        ParseException parseException = new ParseException("Parse error", 10);
        UncheckedParseException exception = new UncheckedParseException(parseException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithSuppressedExceptions() {
        ParseException parseException = new ParseException("Parse error", 10);
        ParseException suppressed = new ParseException("Suppressed parse error", 20);
        parseException.addSuppressed(suppressed);

        UncheckedParseException exception = new UncheckedParseException(parseException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(1, suppressedExceptions.length);
        Assertions.assertEquals(suppressed, suppressedExceptions[0]);
    }

    @Test
    public void testParseExceptionWithZeroOffset() {
        ParseException parseException = new ParseException("Parse error", 0);
        UncheckedParseException exception = new UncheckedParseException(parseException);
        Assertions.assertEquals(parseException, exception.getCause());
        Assertions.assertEquals(0, ((ParseException) exception.getCause()).getErrorOffset());
    }

    @Test
    public void testParseExceptionWithNegativeOffset() {
        ParseException parseException = new ParseException("Parse error", -1);
        UncheckedParseException exception = new UncheckedParseException(parseException);
        Assertions.assertEquals(parseException, exception.getCause());
        Assertions.assertEquals(-1, ((ParseException) exception.getCause()).getErrorOffset());
    }

    @Test
    public void testEmptyMessage() {
        ParseException parseException = new ParseException("", 10);
        UncheckedParseException exception = new UncheckedParseException(parseException);
        Assertions.assertEquals("java.text.ParseException: ", exception.getMessage());
    }

    @Test
    public void testNullMessage() {
        ParseException parseException = new ParseException(null, 10);
        UncheckedParseException exception = new UncheckedParseException(parseException);
        Assertions.assertEquals("java.text.ParseException", exception.getMessage());
    }

}
