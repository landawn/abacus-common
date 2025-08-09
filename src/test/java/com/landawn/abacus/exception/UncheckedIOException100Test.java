package com.landawn.abacus.exception;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UncheckedIOException100Test extends TestBase {

    @Test
    public void testCauseConstructor() {
        IOException ioException = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.io.IOException: " + ioException.getMessage(), exception.getMessage());
        Assertions.assertEquals(ioException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom IO error message";
        IOException ioException = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(message, ioException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(ioException, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        IOException ioException = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertTrue(exception instanceof com.landawn.abacus.exception.UncheckedException);
    }

    @Test
    public void testStackTrace() {
        IOException ioException = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(ioException);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        IOException ioException = new IOException("IO error");
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithSuppressedExceptions() {
        IOException ioException = new IOException("IO error");
        IOException suppressed = new IOException("Suppressed IO error");
        ioException.addSuppressed(suppressed);

        UncheckedIOException exception = new UncheckedIOException(ioException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(1, suppressedExceptions.length);
        Assertions.assertEquals(suppressed, suppressedExceptions[0]);
    }

    @Test
    public void testWithCause() {
        IOException rootCause = new IOException("Root cause");
        IOException ioException = new IOException("IO error", rootCause);
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertEquals(ioException, exception.getCause());
        Assertions.assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    public void testEmptyMessage() {
        IOException ioException = new IOException("");
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertEquals("java.io.IOException: ", exception.getMessage());
    }

    @Test
    public void testNullMessage() {
        IOException ioException = new IOException((String) null);
        UncheckedIOException exception = new UncheckedIOException(ioException);
        Assertions.assertEquals("java.io.IOException", exception.getMessage());
    }
}
