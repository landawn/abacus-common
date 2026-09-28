package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Tests for the 2026-09-25 fix pass on {@link Throwables} and {@link ExceptionUtil} (findings U21-01, U21-02, U21-03 of
 * the line-by-line review of the 2026-09-24 changes).
 */
public class ThrowablesReview20260925Test extends TestBase {

    @AfterEach
    public void clearInterrupt() {
        Thread.interrupted();
    }

    private static Throwable wrapInvocation(final int depth, final Throwable inner) {
        Throwable t = inner;

        for (int i = 0; i < depth; i++) {
            t = new InvocationTargetException(t);
        }

        return t;
    }

    // ---------------------------------------------------------------- U21-02 (LST/C-057): BinaryOperator.unchecked() failure paths

    @Test
    public void testBinaryOperatorUnchecked_convertsFailures() {
        final Throwables.BinaryOperator<String, IOException> failing = (a, b) -> {
            throw new IOException("boom");
        };
        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> failing.unchecked().apply("x", "y"));
        assertEquals("boom", e.getCause().getMessage());

        // a RuntimeException is rethrown as the same instance
        final IllegalStateException ise = new IllegalStateException();
        final Throwables.BinaryOperator<String, RuntimeException> rte = (a, b) -> {
            throw ise;
        };
        assertSame(ise, assertThrows(IllegalStateException.class, () -> rte.unchecked().apply("x", "y")));

        // an Error is converted to a runtime exception, exactly like UnaryOperator.unchecked()
        final Throwables.BinaryOperator<String, RuntimeException> err = (a, b) -> {
            throw new AssertionError("e");
        };
        assertThrows(RuntimeException.class, () -> err.unchecked().apply("x", "y"));

        // a null result passes through
        final Throwables.BinaryOperator<String, RuntimeException> nul = (a, b) -> null;
        assertNull(nul.unchecked().apply("x", "y"));

        final Throwables.BinaryOperator<String, RuntimeException> concat = (a, b) -> a + b;
        assertEquals("xy", concat.unchecked().apply("x", "y"));
    }

    // ---------------------------------------------------------------- U21-01 (LST/C-005): documented erasure change

    @Test
    public void testCall_defaultValueOverloadsEraseToObject() throws Exception {
        final Method twoArg = Throwables.class.getMethod("call", Throwables.Callable.class, Object.class);
        assertEquals(Object.class, twoArg.getParameterTypes()[1]);

        final Method threeArg = Throwables.class.getMethod("call", Throwables.Callable.class, java.util.function.Predicate.class, Object.class);
        assertEquals(Object.class, threeArg.getParameterTypes()[2]);

        // the pre-1.1.2 descriptors no longer exist: callers compiled against them must be recompiled
        assertThrows(NoSuchMethodException.class, () -> Throwables.class.getMethod("call", Throwables.Callable.class, Comparable.class));
        assertThrows(NoSuchMethodException.class,
                () -> Throwables.class.getMethod("call", Throwables.Callable.class, java.util.function.Predicate.class, Comparable.class));

        // and a non-Comparable default is accepted at the source level
        final List<String> fallback = Collections.emptyList();
        assertSame(fallback, Throwables.call(() -> {
            throw new IOException();
        }, fallback));
        assertSame(fallback, Throwables.call(() -> {
            throw new IOException();
        }, ex -> ex instanceof IOException, fallback));
    }

    // ---------------------------------------------------------------- U21-01 (L12/C-180): JDK-module exception classes are refused

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testRegisterRuntimeExceptionMapper_rejectsJdkModuleExceptionClasses() throws Exception {
        // a java.base class whose package starts with neither "java." nor "javax."
        final Class validator = Class.forName("sun.security.validator.ValidatorException");
        assertEquals("java.base", validator.getModule().getName());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ExceptionUtil.registerRuntimeExceptionMapper(validator, t -> new RuntimeException((Throwable) t)));
        assertTrue(e.getMessage().contains("built-in"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class,
                () -> ExceptionUtil.registerRuntimeExceptionMapper(validator, t -> new RuntimeException((Throwable) t), true));
        assertTrue(e.getMessage().contains("built-in"), e.getMessage());

        // the package rule is unchanged
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.registerRuntimeExceptionMapper(IOException.class, t -> new RuntimeException(t)));
    }

    // ---------------------------------------------------------------- U21-03 (LST/C-055): interrupt restore is bounded at 100 wrappers

    @Test
    public void testRestoreInterruptedStatus_boundedAtHundredWrappers() {
        for (final int depth : new int[] { 0, 1, 50, 99, 100 }) {
            Thread.interrupted();
            final Throwable t = wrapInvocation(depth, new InterruptedException());
            assertEquals("d", Throwables.call(() -> {
                throw t;
            }, "d"));
            assertTrue(Thread.interrupted(), "depth " + depth);
        }

        Thread.interrupted();
        final Throwable mixed = new UndeclaredThrowableException(new InvocationTargetException(new InterruptedException()));
        assertEquals("d", Throwables.call(() -> {
            throw mixed;
        }, "d"));
        assertTrue(Thread.interrupted());

        Thread.interrupted();
        final Throwable tooDeep = wrapInvocation(101, new InterruptedException());
        assertEquals("d", Throwables.call(() -> {
            throw tooDeep;
        }, "d"));
        assertFalse(Thread.interrupted(), "101 wrappers exceed the bound");

        // ExceptionUtil's rethrow path uses the same bound, which is what the helper mirrors
        Thread.interrupted();
        ExceptionUtil.toRuntimeException(wrapInvocation(100, new InterruptedException()), true);
        assertTrue(Thread.interrupted());

        Thread.interrupted();
        ExceptionUtil.toRuntimeException(wrapInvocation(101, new InterruptedException()), true);
        assertFalse(Thread.interrupted());
    }
}
