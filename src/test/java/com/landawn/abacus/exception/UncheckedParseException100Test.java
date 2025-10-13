package com.landawn.abacus.exception;

import java.text.ParseException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class UncheckedParseException100Test extends TestBase {

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
    public void testIsInstanceOfUncheckedException() {
        ParseException parseException = new ParseException("Parse error", 10);
        UncheckedParseException exception = new UncheckedParseException(parseException);
        Assertions.assertTrue(exception instanceof com.landawn.abacus.exception.UncheckedException);
    }

    @Test
    public void testStackTrace() {
        ParseException parseException = new ParseException("Parse error", 10);
        UncheckedParseException exception = new UncheckedParseException(parseException);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
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
