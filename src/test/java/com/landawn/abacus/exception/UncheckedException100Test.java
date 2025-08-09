package com.landawn.abacus.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UncheckedException100Test extends TestBase {

    @Test
    public void testCauseConstructor() {
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.lang.Exception: " + checkedException.getMessage(), exception.getMessage());
        Assertions.assertEquals(checkedException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom error message";
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(message, checkedException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(checkedException, exception.getCause());
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(checkedException);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithSuppressedExceptions() {
        Exception checkedException = new Exception("Checked exception");
        Exception suppressed1 = new Exception("Suppressed exception 1");
        Exception suppressed2 = new Exception("Suppressed exception 2");
        checkedException.addSuppressed(suppressed1);
        checkedException.addSuppressed(suppressed2);

        UncheckedException exception = new UncheckedException(checkedException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(2, suppressedExceptions.length);
        Assertions.assertEquals(suppressed1, suppressedExceptions[0]);
        Assertions.assertEquals(suppressed2, suppressedExceptions[1]);
    }

    @Test
    public void testWithCause() {
        Exception rootCause = new Exception("Root cause");
        Exception checkedException = new Exception("Checked exception", rootCause);
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertEquals(checkedException, exception.getCause());
        Assertions.assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    public void testNullCauseThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new UncheckedException((Throwable) null);
        });
    }

    @Test
    public void testNullCauseWithMessageThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new UncheckedException("Error message", null);
        });
    }

    @Test
    public void testEmptyMessage() {
        Exception checkedException = new Exception("");
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertEquals("java.lang.Exception: ", exception.getMessage());
    }

    @Test
    public void testNullMessage() {
        Exception checkedException = new Exception((String) null);
        UncheckedException exception = new UncheckedException(checkedException);
        Assertions.assertEquals("java.lang.Exception", exception.getMessage());
    }

    @Test
    public void testWithRuntimeException() {
        RuntimeException runtimeException = new RuntimeException("Runtime exception");
        UncheckedException exception = new UncheckedException(runtimeException);
        Assertions.assertEquals("java.lang.RuntimeException: " + runtimeException.getMessage(), exception.getMessage());
        Assertions.assertEquals(runtimeException, exception.getCause());
    }

    @Test
    public void testEmptySuppressedExceptions() {
        Exception checkedException = new Exception("Checked exception");
        UncheckedException exception = new UncheckedException(checkedException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(0, suppressedExceptions.length);
    }
}
