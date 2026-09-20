package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.exceptions.misusing.InvalidUseOfMatchersException;
import org.mockito.exceptions.misusing.MissingMethodInvocationException;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.exception.UncheckedParseException;
import com.landawn.abacus.exception.UncheckedReflectiveOperationException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.u.Optional;

public class ExceptionUtilTest extends TestBase {

    public static class CustomCheckedException extends Exception {
        public CustomCheckedException(String message) {
            super(message);
        }

        public CustomCheckedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CustomRuntimeException extends RuntimeException {
        public CustomRuntimeException(String message) {
            super(message);
        }

        public CustomRuntimeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CyclicExecutionException2026 extends ExecutionException {
        public CyclicExecutionException2026() {
        }
    }

    private static final class NotUncheckedIOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private NotUncheckedIOExceptionWrapper(final IOException cause) {
            super(cause);
        }
    }

    @Test
    public void testRegisterRuntimeExceptionMapper() {
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.registerRuntimeExceptionMapper(null, e -> new RuntimeException()));
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.registerRuntimeExceptionMapper(CustomCheckedException.class, null));
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.registerRuntimeExceptionMapper(MockitoException.class, null, true));
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.registerRuntimeExceptionMapper(IOException.class, e -> new RuntimeException(e)));
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.registerRuntimeExceptionMapper(RuntimeException.class, e -> e));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterRuntimeExceptionMapper_InvalidatesDerivedSubclassCache() throws Exception {
        final Field registryField = ExceptionUtil.class.getDeclaredField("toRuntimeExceptionFuncMap");
        final Field resolvedCacheField = ExceptionUtil.class.getDeclaredField("resolvedToRuntimeExceptionFuncCache");
        final Field lockField = ExceptionUtil.class.getDeclaredField("runtimeExceptionMapperLock");
        registryField.setAccessible(true);
        resolvedCacheField.setAccessible(true);
        lockField.setAccessible(true);

        final Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>> registry = (Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>>) registryField
                .get(null);
        final ClassValue<?> resolvedCache = (ClassValue<?>) resolvedCacheField.get(null);
        final Object mapperLock = lockField.get(null);
        final Function<Throwable, RuntimeException> previousRegistration;

        synchronized (mapperLock) {
            previousRegistration = registry.remove(MockitoException.class);
            resolvedCache.remove(InvalidUseOfMatchersException.class);
        }

        try {
            final InvalidUseOfMatchersException beforeRegistration = new InvalidUseOfMatchersException("before");
            assertSame(beforeRegistration, ExceptionUtil.toRuntimeException(beforeRegistration));

            ExceptionUtil.registerRuntimeExceptionMapper(MockitoException.class, e -> new CustomRuntimeException("mapped", e), true);

            final InvalidUseOfMatchersException afterRegistration = new InvalidUseOfMatchersException("after");
            final RuntimeException mapped = ExceptionUtil.toRuntimeException(afterRegistration);
            assertInstanceOf(CustomRuntimeException.class, mapped);
            assertSame(afterRegistration, mapped.getCause());
        } finally {
            synchronized (mapperLock) {
                if (previousRegistration == null) {
                    registry.remove(MockitoException.class);
                } else {
                    registry.put(MockitoException.class, previousRegistration);
                }
                ((ClassValue<?>) resolvedCacheField.get(null)).remove(InvalidUseOfMatchersException.class);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterRuntimeExceptionMapper_WithoutForceIsAtomic() throws Exception {
        final Field registryField = ExceptionUtil.class.getDeclaredField("toRuntimeExceptionFuncMap");
        final Field resolvedCacheField = ExceptionUtil.class.getDeclaredField("resolvedToRuntimeExceptionFuncCache");
        final Field lockField = ExceptionUtil.class.getDeclaredField("runtimeExceptionMapperLock");
        registryField.setAccessible(true);
        resolvedCacheField.setAccessible(true);
        lockField.setAccessible(true);

        final Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>> registry = (Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>>) registryField
                .get(null);
        final ClassValue<?> resolvedCache = (ClassValue<?>) resolvedCacheField.get(null);
        final Object mapperLock = lockField.get(null);
        final Function<Throwable, RuntimeException> previousRegistration;

        synchronized (mapperLock) {
            previousRegistration = registry.remove(MissingMethodInvocationException.class);
            resolvedCache.remove(MissingMethodInvocationException.class);
        }

        try {
            final CountDownLatch ready = new CountDownLatch(2);
            final CountDownLatch start = new CountDownLatch(1);
            final AtomicInteger successes = new AtomicInteger();
            final AtomicInteger duplicates = new AtomicInteger();
            final AtomicInteger unexpectedFailures = new AtomicInteger();
            final Runnable registration = () -> {
                ready.countDown();
                try {
                    start.await();
                    ExceptionUtil.registerRuntimeExceptionMapper(MissingMethodInvocationException.class, e -> new CustomRuntimeException(e.getMessage(), e));
                    successes.incrementAndGet();
                } catch (final IllegalArgumentException e) {
                    duplicates.incrementAndGet();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedFailures.incrementAndGet();
                } catch (final Throwable e) { // NOSONAR -- asserted below
                    unexpectedFailures.incrementAndGet();
                }
            };
            final Thread first = new Thread(registration, "exception-mapper-registration-1");
            final Thread second = new Thread(registration, "exception-mapper-registration-2");
            first.start();
            second.start();
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            first.join(5000);
            second.join(5000);

            assertFalse(first.isAlive());
            assertFalse(second.isAlive());
            assertEquals(1, successes.get());
            assertEquals(1, duplicates.get());
            assertEquals(0, unexpectedFailures.get());
        } finally {
            synchronized (mapperLock) {
                if (previousRegistration == null) {
                    registry.remove(MissingMethodInvocationException.class);
                } else {
                    registry.put(MissingMethodInvocationException.class, previousRegistration);
                }
                ((ClassValue<?>) resolvedCacheField.get(null)).remove(MissingMethodInvocationException.class);
            }
        }
    }

    @Test
    public void testToRuntimeException() {
        RuntimeException runtimeEx = new RuntimeException("runtime");
        assertSame(runtimeEx, ExceptionUtil.toRuntimeException(runtimeEx));

        IOException ioEx = new IOException("io error");
        RuntimeException ioResult = ExceptionUtil.toRuntimeException(ioEx);
        assertInstanceOf(UncheckedIOException.class, ioResult);
        assertEquals(ioEx, ioResult.getCause());
        assertInstanceOf(UncheckedIOException.class, ExceptionUtil.toRuntimeException(new IOException("io2")));

        SQLException sqlEx = new SQLException("sql error", "42", 1054);
        RuntimeException sqlResult = ExceptionUtil.toRuntimeException(sqlEx);
        assertInstanceOf(UncheckedSQLException.class, sqlResult);
        assertEquals(sqlEx, sqlResult.getCause());

        ParseException parseEx = new ParseException("parse error", 10);
        RuntimeException parseResult = ExceptionUtil.toRuntimeException(parseEx);
        assertInstanceOf(UncheckedParseException.class, parseResult);
        assertEquals(parseEx, parseResult.getCause());

        InterruptedException interruptEx = new InterruptedException("interrupted");
        RuntimeException interruptResult = ExceptionUtil.toRuntimeException(interruptEx);
        assertInstanceOf(UncheckedInterruptedException.class, interruptResult);
        assertEquals(interruptEx, interruptResult.getCause());

        ReflectiveOperationException reflectiveEx = new ReflectiveOperationException("reflective error");
        RuntimeException reflectiveResult = ExceptionUtil.toRuntimeException(reflectiveEx);
        assertInstanceOf(UncheckedReflectiveOperationException.class, reflectiveResult);
        assertEquals(reflectiveEx, reflectiveResult.getCause());

        Exception genericEx = new Exception("generic");
        RuntimeException genericResult = ExceptionUtil.toRuntimeException(genericEx);
        assertInstanceOf(UncheckedException.class, genericResult);
        assertEquals(genericEx, genericResult.getCause());

        Error error = new Error("error");
        RuntimeException errorResult = ExceptionUtil.toRuntimeException(error);
        assertInstanceOf(RuntimeException.class, errorResult);
        assertEquals("error", errorResult.getCause().getMessage());
        assertEquals(error, ExceptionUtil.toRuntimeException(error, false, false).getCause());
        assertThrows(Error.class, () -> ExceptionUtil.toRuntimeException(error, false, true));
        assertThrows(Error.class, () -> ExceptionUtil.toRuntimeException(error, true, true));

        IOException cause = new IOException("io cause");
        RuntimeException execResult = ExceptionUtil.toRuntimeException(new ExecutionException(cause));
        assertInstanceOf(UncheckedIOException.class, execResult);
        assertEquals(cause, execResult.getCause());
        RuntimeException execNoCause = ExceptionUtil.toRuntimeException(new ExecutionException(null));
        assertInstanceOf(UncheckedException.class, execNoCause);

        RuntimeException invocResult = ExceptionUtil.toRuntimeException(new InvocationTargetException(cause));
        assertInstanceOf(UncheckedIOException.class, invocResult);
        assertEquals(cause, invocResult.getCause());
        assertInstanceOf(UncheckedException.class, ExceptionUtil.toRuntimeException(new InvocationTargetException(null)));

        RuntimeException undeclaredResult = ExceptionUtil.toRuntimeException(new UndeclaredThrowableException(cause));
        assertInstanceOf(UncheckedIOException.class, undeclaredResult);
        assertEquals(cause, undeclaredResult.getCause());
        UndeclaredThrowableException undeclaredNoCause = new UndeclaredThrowableException(null);
        assertSame(undeclaredNoCause, ExceptionUtil.toRuntimeException(undeclaredNoCause));

        Thread.interrupted();
        RuntimeException interruptTrue = ExceptionUtil.toRuntimeException(new InterruptedException("interrupted"), true);
        assertInstanceOf(UncheckedInterruptedException.class, interruptTrue);
        assertTrue(Thread.interrupted());
        assertInstanceOf(UncheckedIOException.class, ExceptionUtil.toRuntimeException(new IOException("io error"), true));
        assertFalse(Thread.interrupted());

        Thread.interrupted();
        RuntimeException throwableInterrupt = ExceptionUtil.toRuntimeException((Throwable) new InterruptedException("interrupted"), true);
        assertInstanceOf(UncheckedInterruptedException.class, throwableInterrupt);
        assertTrue(Thread.interrupted());
        ExceptionUtil.toRuntimeException(new Error("test error"), true);
        assertFalse(Thread.interrupted());

        Thread.interrupted();
        RuntimeException interruptThrowError = ExceptionUtil.toRuntimeException(new InterruptedException("interrupted"), true, true);
        assertInstanceOf(UncheckedInterruptedException.class, interruptThrowError);
        assertTrue(Thread.interrupted());

        Thread.interrupted();
        ExceptionUtil.toRuntimeException(new ExecutionException(new InterruptedException()), true);
        // An ExecutionException reports what a task threw on *another* thread, so unwrapping it must not
        // interrupt the thread doing the conversion.
        assertFalse(Thread.interrupted());
        ExceptionUtil.toRuntimeException(new ExecutionException(new ExecutionException(new InterruptedException())), true);
        assertFalse(Thread.interrupted());
        // InvocationTargetException and UndeclaredThrowableException are raised on the calling thread and
        // keep their meaning.
        ExceptionUtil.toRuntimeException(new InvocationTargetException(new InterruptedException()), true);
        assertTrue(Thread.interrupted());
        ExceptionUtil.toRuntimeException(new UndeclaredThrowableException(new InterruptedException()), true);
        assertTrue(Thread.interrupted());
        assertThrows(OutOfMemoryError.class, () -> ExceptionUtil.toRuntimeException(new ExecutionException(new OutOfMemoryError("fake")), false, true));

        final CyclicExecutionException2026 e1 = new CyclicExecutionException2026();
        final ExecutionException e2 = new ExecutionException(e1);
        e1.initCause(e2);
        assertNotNull(ExceptionUtil.toRuntimeException(e1));
    }

    @Test
    public void testCauseSearchRejectsNullTargetType() {
        final IOException failure = new IOException("io");

        // The Class overloads validate their second argument exactly like their Predicate twins.
        assertEquals("'targetExceptionType' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.hasCause(failure, (Class<? extends Throwable>) null)).getMessage());
        assertEquals("'targetExceptionType' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.hasCause(null, (Class<? extends Throwable>) null)).getMessage());
        assertEquals("'targetExceptionType' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.findCause(failure, (Class<? extends Throwable>) null)).getMessage());
        assertEquals("'targetExceptionType' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.findCause(null, (Class<? extends Throwable>) null)).getMessage());

        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.hasCause(null, (Predicate<Throwable>) null));
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.findCause(null, (Predicate<Throwable>) null));

        // A valid type still answers normally.
        assertTrue(ExceptionUtil.hasCause(new RuntimeException(failure), IOException.class));
        assertSame(failure, ExceptionUtil.findCause(new RuntimeException(failure), IOException.class).orElseNull());
    }

    @Test
    public void testTryToGetOriginalCheckedException() {
        IOException originalCause = new IOException("original");
        assertEquals(originalCause, ExceptionUtil.tryToGetOriginalCheckedException(new UncheckedException(originalCause)));
        CustomCheckedException customCause = new CustomCheckedException("custom");
        assertEquals(customCause, ExceptionUtil.tryToGetOriginalCheckedException(new UncheckedException(customCause)));
        assertEquals(originalCause, ExceptionUtil.tryToGetOriginalCheckedException(new ExecutionException(new InvocationTargetException(originalCause))));

        RuntimeException runtimeEx = new RuntimeException(new RuntimeException("runtime cause"));
        assertEquals(runtimeEx, ExceptionUtil.tryToGetOriginalCheckedException(runtimeEx));
        RuntimeException noCause = new RuntimeException("no cause");
        assertEquals(noCause, ExceptionUtil.tryToGetOriginalCheckedException(noCause));

        IOException originalIO = new IOException("io error");
        assertEquals(originalIO, ExceptionUtil.tryToGetOriginalCheckedException(new UncheckedIOException(originalIO)));
        SQLException originalSQL = new SQLException("sql error");
        assertEquals(originalSQL, ExceptionUtil.tryToGetOriginalCheckedException(new UncheckedSQLException(originalSQL)));
        ParseException originalParse = new ParseException("parse error", 5);
        assertEquals(originalParse, ExceptionUtil.tryToGetOriginalCheckedException(new UncheckedParseException(originalParse)));

        IOException nestedIo = new IOException("nested io");
        assertEquals(nestedIo, ExceptionUtil.tryToGetOriginalCheckedException(new ExecutionException(nestedIo)));
        SQLException nestedSql = new SQLException("nested sql");
        assertEquals(nestedSql, ExceptionUtil.tryToGetOriginalCheckedException(new InvocationTargetException(nestedSql)));

        RuntimeException outerRuntime = new RuntimeException("outer", new RuntimeException("inner"));
        assertSame(outerRuntime, ExceptionUtil.tryToGetOriginalCheckedException(outerRuntime));

        IOException cause = new IOException("checked cause");
        RuntimeException wrapper = new NotUncheckedIOExceptionWrapper(cause);
        assertSame(wrapper, ExceptionUtil.tryToGetOriginalCheckedException(wrapper));

        IOException checked = new IOException("checked");
        assertSame(checked, ExceptionUtil.tryToGetOriginalCheckedException(checked));
    }

    @Test
    public void testHasCause() {
        IOException ioEx = new IOException("io error");
        SQLException sqlEx = new SQLException("sql error", ioEx);
        RuntimeException runtimeEx = new RuntimeException("runtime error", sqlEx);

        assertTrue(ExceptionUtil.hasCause(runtimeEx, RuntimeException.class));
        assertTrue(ExceptionUtil.hasCause(runtimeEx, SQLException.class));
        assertTrue(ExceptionUtil.hasCause(runtimeEx, IOException.class));
        assertFalse(ExceptionUtil.hasCause(runtimeEx, ParseException.class));
        assertTrue(ExceptionUtil.hasCause(ioEx, IOException.class));
        assertTrue(ExceptionUtil.hasCause(ioEx, Exception.class));
        assertFalse(ExceptionUtil.hasCause(ioEx, SQLException.class));

        RuntimeException noCause = new RuntimeException("no cause");
        assertTrue(ExceptionUtil.hasCause(noCause, RuntimeException.class));
        assertFalse(ExceptionUtil.hasCause(noCause, IOException.class));

        Predicate<Throwable> containsError = ex -> ex.getMessage() != null && ex.getMessage().contains("error");
        assertTrue(ExceptionUtil.hasCause(runtimeEx, containsError));
        assertTrue(ExceptionUtil.hasCause(runtimeEx, ex -> ex instanceof IOException));
        assertFalse(ExceptionUtil.hasCause(runtimeEx, ex -> ex.getMessage() != null && ex.getMessage().contains("foo")));
        assertFalse(ExceptionUtil.hasCause(noCause, ex -> ex instanceof IOException));

        assertTrue(ExceptionUtil.hasSQLCause(sqlEx));
        assertTrue(ExceptionUtil.hasSQLCause(runtimeEx));
        assertTrue(ExceptionUtil.hasSQLCause(new UncheckedSQLException(sqlEx)));
        assertFalse(ExceptionUtil.hasSQLCause(ioEx));
        assertFalse(ExceptionUtil.hasSQLCause(noCause));
        assertTrue(ExceptionUtil.hasIOCause(ioEx));
        assertTrue(ExceptionUtil.hasIOCause(runtimeEx));
        assertTrue(ExceptionUtil.hasIOCause(new UncheckedIOException(ioEx)));
        assertFalse(ExceptionUtil.hasIOCause(new SQLException("sql error")));
        assertFalse(ExceptionUtil.hasIOCause(noCause));

        assertFalse(ExceptionUtil.hasCause((Throwable) null, IOException.class));
        assertFalse(ExceptionUtil.hasCause((Throwable) null, ex -> true));
        assertFalse(ExceptionUtil.hasSQLCause((Throwable) null));
        assertFalse(ExceptionUtil.hasIOCause((Throwable) null));

        RuntimeException circular1 = new RuntimeException("circular1");
        RuntimeException circular2 = new RuntimeException("circular2", circular1);
        try {
            Field causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(circular1, circular2);
            assertTrue(ExceptionUtil.hasCause(circular1, RuntimeException.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testIsNullPointerOrIllegalArgumentException() {
        assertTrue(ExceptionUtil.isNullPointerOrIllegalArgumentException(new NullPointerException("null")));
        assertTrue(ExceptionUtil.isNullPointerOrIllegalArgumentException(new IllegalArgumentException("illegal")));
        assertFalse(ExceptionUtil.isNullPointerOrIllegalArgumentException(new IOException("io")));
        assertFalse(ExceptionUtil.isNullPointerOrIllegalArgumentException(new RuntimeException("runtime")));
    }

    @Test
    public void testListCausesGetRootCauseFindCause() {
        IOException ioEx = new IOException("io");
        SQLException sqlEx = new SQLException("sql", ioEx);
        RuntimeException runtimeEx = new RuntimeException("runtime", sqlEx);

        List<Throwable> causes = ExceptionUtil.listCauses(runtimeEx);
        assertEquals(3, causes.size());
        assertEquals(runtimeEx, causes.get(0));
        assertEquals(sqlEx, causes.get(1));
        assertEquals(ioEx, causes.get(2));
        RuntimeException noCause = new RuntimeException("no cause");
        causes = ExceptionUtil.listCauses(noCause);
        assertEquals(1, causes.size());
        assertEquals(noCause, causes.get(0));
        List<Throwable> nullCauses = ExceptionUtil.listCauses(null);
        assertNotNull(nullCauses);
        assertTrue(nullCauses.isEmpty());

        assertEquals(ioEx, ExceptionUtil.getRootCause(runtimeEx));
        assertEquals(noCause, ExceptionUtil.getRootCause(noCause));
        assertEquals(ioEx, ExceptionUtil.getRootCause(new RuntimeException("single", ioEx)));
        assertNull(ExceptionUtil.getRootCause(null));

        Optional<RuntimeException> foundRuntime = ExceptionUtil.findCause(runtimeEx, RuntimeException.class);
        assertTrue(foundRuntime.isPresent());
        assertEquals(runtimeEx, foundRuntime.get());
        Optional<SQLException> foundSql = ExceptionUtil.findCause(runtimeEx, SQLException.class);
        assertTrue(foundSql.isPresent());
        assertEquals(sqlEx, foundSql.get());
        Optional<IOException> foundIo = ExceptionUtil.findCause(runtimeEx, IOException.class);
        assertTrue(foundIo.isPresent());
        assertEquals(ioEx, foundIo.get());
        assertFalse(ExceptionUtil.findCause(runtimeEx, ParseException.class).isPresent());
        assertFalse(ExceptionUtil.findCause(noCause, IOException.class).isPresent());

        Optional<Throwable> foundByMessage = ExceptionUtil.findCause(runtimeEx, ex -> ex.getMessage() != null && ex.getMessage().contains("io"));
        assertTrue(foundByMessage.isPresent());
        assertEquals(ioEx, foundByMessage.get());
        Optional<Throwable> foundByType = ExceptionUtil.findCause(runtimeEx, ex -> ex instanceof SQLException);
        assertTrue(foundByType.isPresent());
        assertEquals(sqlEx, foundByType.get());
        assertFalse(ExceptionUtil.findCause(runtimeEx, ex -> ex.getMessage() != null && ex.getMessage().contains("foo")).isPresent());
        assertFalse(ExceptionUtil.findCause(noCause, ex -> ex instanceof IOException).isPresent());
        assertFalse(ExceptionUtil.findCause((Throwable) null, IOException.class).isPresent());
        assertFalse(ExceptionUtil.findCause((Throwable) null, ex -> true).isPresent());

        IOException level3 = new IOException("level 3");
        ExecutionException level0 = new ExecutionException("level 0", new RuntimeException("level 1", new SQLException("level 2", level3)));
        assertEquals(level3, ExceptionUtil.findCause(level0, IOException.class).get());
        assertEquals(level3, ExceptionUtil.getRootCause(level0));
        assertEquals(4, ExceptionUtil.listCauses(level0).size());

        RuntimeException circular1 = new RuntimeException("circular1");
        RuntimeException circular2 = new RuntimeException("circular2", circular1);
        try {
            Field causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(circular1, circular2);
            assertNotNull(ExceptionUtil.getRootCause(circular1));
            List<Throwable> circularCauses = ExceptionUtil.listCauses(circular1);
            assertTrue(circularCauses.size() <= 3);
            assertEquals(2, ExceptionUtil.listCauses(circular1).size());
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testCauseQueriesDoNotTruncateDeepAcyclicChains() {
        Throwable chain = new IOException("deep");
        for (int i = 0; i < 150; i++) {
            chain = new RuntimeException((String) null, chain);
        }
        assertTrue(ExceptionUtil.hasCause(chain, IOException.class));
        assertTrue(ExceptionUtil.hasCause(chain, cause -> cause instanceof IOException));
        assertTrue(ExceptionUtil.hasIOCause(chain));
        assertTrue(ExceptionUtil.findCause(chain, IOException.class).isPresent());
        assertTrue(ExceptionUtil.findCause(chain, cause -> cause instanceof IOException).isPresent());
        assertEquals("deep", ExceptionUtil.getErrorMessage(chain));

        Throwable sqlChain = new SQLException("deep-sql");
        for (int i = 0; i < 150; i++) {
            sqlChain = new RuntimeException((String) null, sqlChain);
        }
        assertTrue(ExceptionUtil.hasSQLCause(sqlChain));
    }

    @Test
    public void testGetStackTrace() {
        Exception ex = new Exception("test exception");
        String stackTrace = ExceptionUtil.getStackTrace(ex);
        assertNotNull(stackTrace);
        assertTrue(stackTrace.contains("test exception"));
        assertTrue(stackTrace.contains("Exception"));
        assertTrue(stackTrace.contains("at "));
        assertEquals("", ExceptionUtil.getStackTrace(null));

        String withCause = ExceptionUtil.getStackTrace(new RuntimeException("wrapper", new IOException("cause")));
        assertTrue(withCause.contains("wrapper"));
        assertTrue(withCause.contains("cause"));
        assertTrue(withCause.contains("Caused by"));

        Throwable t = new RuntimeException("root");
        for (int i = 0; i < 50; i++) {
            t = new RuntimeException("level-" + i, t);
        }
        String longTrace = ExceptionUtil.getStackTrace(t);
        assertNotNull(longTrace);
        assertTrue(longTrace.contains("root"));
        assertTrue(longTrace.contains("Caused by"));
    }

    @Test
    public void testGetErrorMessage() {
        assertEquals("error message", ExceptionUtil.getErrorMessage(new Exception("error message")));
        Exception noMsg = new Exception();
        assertEquals(noMsg.getClass().getCanonicalName(), ExceptionUtil.getErrorMessage(noMsg));
        assertEquals("java.lang.Exception: cause message", ExceptionUtil.getErrorMessage(new Exception(new Exception("cause message"))));
        SQLException sqlEx = new SQLException("sql error", "42", 1054);
        assertEquals("1054|sql error", ExceptionUtil.getErrorMessage(sqlEx));
        assertEquals("999|some sql error", ExceptionUtil.getErrorMessage(new SQLException("some sql error", "state", 999), false));
        assertEquals("42|sql msg", ExceptionUtil.getErrorMessage(new SQLException("sql msg", "S0001", 42), false));

        assertEquals("Exception|error message", ExceptionUtil.getErrorMessage(new Exception("error message"), true));
        assertEquals("Exception|" + noMsg.getClass().getCanonicalName(), ExceptionUtil.getErrorMessage(noMsg, true));
        assertEquals("SQLException|1054|sql error", ExceptionUtil.getErrorMessage(sqlEx, true));
        assertEquals("Exception|java.lang.Exception: cause message", ExceptionUtil.getErrorMessage(new Exception(new Exception("cause message")), true));
        String ioWithClass = ExceptionUtil.getErrorMessage(new IOException("io message"), true);
        assertTrue(ioWithClass.contains("IOException"));
        assertTrue(ioWithClass.contains("io message"));
        assertEquals("plain message", ExceptionUtil.getErrorMessage(new IOException("plain message"), false));
        String sqlWithClass = ExceptionUtil.getErrorMessage(new SQLException("sql msg", "S0001", 42), true);
        assertTrue(sqlWithClass.contains("SQLException"));
        assertTrue(sqlWithClass.contains("42"));

        Exception outer = new Exception(new Exception(new Exception()));
        assertEquals(outer.getMessage(), ExceptionUtil.getErrorMessage(outer));
        String nested = ExceptionUtil.getErrorMessage(new Exception(new Exception(new Exception("deep message"))));
        assertNotNull(nested);
        assertFalse(nested.isEmpty());
        RuntimeException empty = new RuntimeException();
        String emptyMsg = ExceptionUtil.getErrorMessage(empty, false);
        assertNotNull(emptyMsg);
        assertFalse(emptyMsg.isEmpty());

        RuntimeException nullMessage = new RuntimeException((String) null, new IOException("cause message")) {
        };
        assertEquals("cause message", ExceptionUtil.getErrorMessage(nullMessage, false));
        assertEquals("", ExceptionUtil.getErrorMessage((Throwable) null));
        assertEquals("", ExceptionUtil.getErrorMessage((Throwable) null, true));
        assertEquals("", ExceptionUtil.getErrorMessage((Throwable) null, false));
    }

    /**
     * An anonymous (or local) exception class has no canonical name - {@code Class.getCanonicalName()}
     * answers {@code null} for it. {@code getErrorMessage} fell back to exactly that when no message was
     * available anywhere in the cause chain, so it returned {@code null} from a method whose javadoc
     * promises the class name and which carries no {@code @MayReturnNull}; with the class-name flag it
     * rendered the literal string {@code "|null"} (the simple name of an anonymous class is {@code ""}).
     */
    @Test
    public void test_getErrorMessage_anonymousExceptionClass_regression_20260918() {
        final Exception anonymous = new RuntimeException() {
            private static final long serialVersionUID = 1L;
        };
        final String expectedName = anonymous.getClass().getName();

        // Before the fix both of these were null / "|null".
        assertNotNull(ExceptionUtil.getErrorMessage(anonymous));
        assertEquals(expectedName, ExceptionUtil.getErrorMessage(anonymous));
        assertEquals("|" + expectedName, ExceptionUtil.getErrorMessage(anonymous, true));

        // A local class has no canonical name either.
        class LocalException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }
        final LocalException local = new LocalException();
        assertNotNull(ExceptionUtil.getErrorMessage(local));
        assertEquals(local.getClass().getName(), ExceptionUtil.getErrorMessage(local));

        // A named class still resolves to its canonical name, exactly as before.
        final Exception named = new Exception();
        assertEquals(named.getClass().getCanonicalName(), ExceptionUtil.getErrorMessage(named));
    }
}
