package com.landawn.abacus.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UncheckedInterruptedException100Test extends TestBase {

    @Test
    public void testCauseConstructor() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.lang.InterruptedException: " + interruptedException.getMessage(), exception.getMessage());
        Assertions.assertEquals(interruptedException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom interruption message";
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(message, interruptedException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(interruptedException, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertTrue(exception instanceof com.landawn.abacus.exception.UncheckedException);
    }

    @Test
    public void testStackTrace() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerialVersionUID() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithSuppressedExceptions() {
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        InterruptedException suppressed = new InterruptedException("Suppressed interruption");
        interruptedException.addSuppressed(suppressed);

        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(1, suppressedExceptions.length);
        Assertions.assertEquals(suppressed, suppressedExceptions[0]);
    }

    @Test
    public void testEmptyMessage() {
        InterruptedException interruptedException = new InterruptedException("");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertEquals("java.lang.InterruptedException: ", exception.getMessage());
    }

    @Test
    public void testNullMessage() {
        InterruptedException interruptedException = new InterruptedException(null);
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertEquals("java.lang.InterruptedException", exception.getMessage());
    }

    @Test
    public void testInterruptedStatus() {
        // Note: This test demonstrates that wrapping InterruptedException doesn't automatically
        // restore the interrupted status. Users should manually call Thread.currentThread().interrupt()
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);

        // The interrupted status is not automatically restored
        Assertions.assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testWithCause() {
        Exception rootCause = new Exception("Root cause");
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");
        interruptedException.initCause(rootCause);

        UncheckedInterruptedException exception = new UncheckedInterruptedException(interruptedException);
        Assertions.assertEquals(interruptedException, exception.getCause());
        Assertions.assertEquals(rootCause, exception.getCause().getCause());
    }
}
