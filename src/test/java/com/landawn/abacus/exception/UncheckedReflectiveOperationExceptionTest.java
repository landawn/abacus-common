package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UncheckedReflectiveOperationExceptionTest extends TestBase {

    @Test
    public void testConstructorWithCause() {
        ReflectiveOperationException cause = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Reflection operation failed";
        ReflectiveOperationException cause = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(message, cause);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testIsInstanceOfUncheckedException() {
        ReflectiveOperationException cause = new ReflectiveOperationException();
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertTrue(exception instanceof UncheckedException);
    }

    @Test
    public void testIsInstanceOfRuntimeException() {
        ReflectiveOperationException cause = new ReflectiveOperationException();
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testStackTrace() {
        ReflectiveOperationException cause = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testSerializable() {
        ReflectiveOperationException cause = new ReflectiveOperationException();
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testSuppressedExceptions() {
        ReflectiveOperationException cause = new ReflectiveOperationException("Reflection error");
        RuntimeException suppressed = new RuntimeException("Suppressed");
        cause.addSuppressed(suppressed);

        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(cause);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(suppressed, exception.getSuppressed()[0]);
    }

    @Test
    public void testCauseConstructor() {
        ReflectiveOperationException reflectiveException = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(reflectiveException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("java.lang.ReflectiveOperationException: " + reflectiveException.getMessage(), exception.getMessage());
        Assertions.assertEquals(reflectiveException, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Custom reflection error message";
        ReflectiveOperationException reflectiveException = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(message, reflectiveException);
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertEquals(reflectiveException, exception.getCause());
    }

    @Test
    public void testSerialVersionUID() {
        ReflectiveOperationException reflectiveException = new ReflectiveOperationException("Reflection error");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(reflectiveException);
        Assertions.assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    public void testWithClassNotFoundException() {
        ClassNotFoundException classNotFoundException = new ClassNotFoundException("Class not found");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(classNotFoundException);
        Assertions.assertEquals(classNotFoundException, exception.getCause());
    }

    @Test
    public void testWithNoSuchMethodException() {
        NoSuchMethodException noSuchMethodException = new NoSuchMethodException("Method not found");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(noSuchMethodException);
        Assertions.assertEquals(noSuchMethodException, exception.getCause());
    }

    @Test
    public void testWithNoSuchFieldException() {
        NoSuchFieldException noSuchFieldException = new NoSuchFieldException("Field not found");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(noSuchFieldException);
        Assertions.assertEquals(noSuchFieldException, exception.getCause());
    }

    @Test
    public void testWithIllegalAccessException() {
        IllegalAccessException illegalAccessException = new IllegalAccessException("Access denied");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(illegalAccessException);
        Assertions.assertEquals(illegalAccessException, exception.getCause());
    }

    @Test
    public void testWithInstantiationException() {
        InstantiationException instantiationException = new InstantiationException("Cannot instantiate");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(instantiationException);
        Assertions.assertEquals(instantiationException, exception.getCause());
    }

    @Test
    public void testWithInvocationTargetException() {
        Exception targetException = new RuntimeException("Target exception");
        InvocationTargetException invocationTargetException = new InvocationTargetException(targetException, "Invocation failed");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(invocationTargetException);
        Assertions.assertEquals(invocationTargetException, exception.getCause());
        Assertions.assertEquals(targetException, ((InvocationTargetException) exception.getCause()).getTargetException());
    }

    @Test
    public void testWithSuppressedExceptions() {
        ReflectiveOperationException reflectiveException = new ReflectiveOperationException("Reflection error");
        ReflectiveOperationException suppressed = new ReflectiveOperationException("Suppressed reflection error");
        reflectiveException.addSuppressed(suppressed);

        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(reflectiveException);
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assertions.assertNotNull(suppressedExceptions);
        Assertions.assertEquals(1, suppressedExceptions.length);
        Assertions.assertEquals(suppressed, suppressedExceptions[0]);
    }

    @Test
    public void testEmptyMessage() {
        ReflectiveOperationException reflectiveException = new ReflectiveOperationException("");
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(reflectiveException);
        Assertions.assertEquals("java.lang.ReflectiveOperationException: ", exception.getMessage());
    }

    @Test
    public void testNullMessage() {
        ReflectiveOperationException reflectiveException = new ReflectiveOperationException((String) null);
        UncheckedReflectiveOperationException exception = new UncheckedReflectiveOperationException(reflectiveException);
        Assertions.assertEquals("java.lang.ReflectiveOperationException", exception.getMessage());
    }

}
