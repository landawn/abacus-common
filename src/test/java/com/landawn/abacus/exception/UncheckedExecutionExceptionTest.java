package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UncheckedExecutionExceptionTest extends TestBase {

    @Test
    public void testConstructorWithCause() {
        ExecutionException cause = new ExecutionException("Execution failed", new RuntimeException("root cause"));
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Task execution failed";
        ExecutionException cause = new ExecutionException("Execution failed", new RuntimeException("root cause"));
        UncheckedExecutionException exception = new UncheckedExecutionException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        ExecutionException cause = new ExecutionException("fail", new RuntimeException());
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertTrue(exception instanceof UncheckedException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        ExecutionException cause = new ExecutionException("fail", new RuntimeException());
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        ExecutionException cause = new ExecutionException("Execution failed", new RuntimeException());
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        ExecutionException cause = new ExecutionException("fail", new RuntimeException());
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testSuppressedExceptions() {
        ExecutionException cause = new ExecutionException("Execution failed", new RuntimeException());
        RuntimeException suppressed = new RuntimeException("Suppressed");
        cause.addSuppressed(suppressed);

        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(suppressed, exception.getSuppressed()[0]);
    }

    @Test
    public void testMultipleSuppressedExceptions() {
        ExecutionException cause = new ExecutionException("Execution failed", new RuntimeException());
        RuntimeException suppressed1 = new RuntimeException("Suppressed 1");
        RuntimeException suppressed2 = new RuntimeException("Suppressed 2");
        cause.addSuppressed(suppressed1);
        cause.addSuppressed(suppressed2);

        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertEquals(2, exception.getSuppressed().length);
        assertEquals(suppressed1, exception.getSuppressed()[0]);
        assertEquals(suppressed2, exception.getSuppressed()[1]);
    }

    @Test
    public void testCauseMessagePreserved() {
        ExecutionException cause = new ExecutionException("Execution failed", new RuntimeException("root"));
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertEquals("java.util.concurrent.ExecutionException: " + cause.getMessage(), exception.getMessage());
    }

    @Test
    public void testWithCauseChain() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        ExecutionException cause = new ExecutionException("Execution failed", rootCause);
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertEquals(cause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    public void testEmptyMessage() {
        ExecutionException cause = new ExecutionException("", new RuntimeException());
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertEquals("java.util.concurrent.ExecutionException: ", exception.getMessage());
    }

    @Test
    public void testNullCauseInExecutionException() {
        ExecutionException cause = new ExecutionException(null);
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testNullCauseThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new UncheckedExecutionException((ExecutionException) null));
    }

    @Test
    public void testNullCauseWithMessageThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new UncheckedExecutionException("message", null));
    }
}
