package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import testfixtures.ExceptionMapperCacheFixtures.DirectFailure;
import testfixtures.ExceptionMapperCacheFixtures.FailingMapperFailure;
import testfixtures.ExceptionMapperCacheFixtures.RacingFailure;
import testfixtures.ExceptionMapperCacheFixtures.ReentrantFailure;
import testfixtures.ExceptionMapperCacheFixtures.StatefulFailure;
import testfixtures.ExceptionMapperCacheFixtures.VariableFailure;

@Tag("unit")
class ExceptionUtilMapperCacheTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void warmedRecognitionDoesNotHideMapperFailure(final boolean throwFailure) throws Exception {
        try (Registration registration = new Registration(FailingMapperFailure.class)) {
            final AtomicBoolean fail = new AtomicBoolean();
            final IllegalStateException expected = new IllegalStateException("mapper failed after a previous match");
            ExceptionUtil.registerRuntimeExceptionMapper(FailingMapperFailure.class, failure -> {
                if (!fail.get()) {
                    return new FirstWrapper(failure);
                }
                if (throwFailure) {
                    throw expected;
                }
                return null;
            }, true);
            final FailingMapperFailure cause = new FailingMapperFailure();
            final FirstWrapper wrapper = new FirstWrapper(cause);
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(wrapper));

            fail.set(true);
            if (throwFailure) {
                assertSame(expected, assertThrows(IllegalStateException.class, () -> ExceptionUtil.tryToGetOriginalCheckedException(wrapper)));
            } else {
                assertThrows(NullPointerException.class, () -> ExceptionUtil.tryToGetOriginalCheckedException(wrapper));
            }
        }
    }

    @Test
    void mapperCanChooseWrapperClassFromExceptionData() throws Exception {
        try (Registration registration = new Registration(VariableFailure.class)) {
            ExceptionUtil.registerRuntimeExceptionMapper(VariableFailure.class,
                    failure -> "first".equals(failure.getMessage()) ? new FirstWrapper(failure) : new SecondWrapper(failure), true);
            final VariableFailure firstCause = new VariableFailure("first");
            final VariableFailure secondCause = new VariableFailure("second");
            assertSame(firstCause, ExceptionUtil.tryToGetOriginalCheckedException(new FirstWrapper(firstCause)));

            final FirstWrapper unmatchedFirst = new FirstWrapper(secondCause);
            assertSame(unmatchedFirst, ExceptionUtil.tryToGetOriginalCheckedException(unmatchedFirst));
            assertSame(secondCause, ExceptionUtil.tryToGetOriginalCheckedException(new SecondWrapper(secondCause)));
            final SecondWrapper unmatchedSecond = new SecondWrapper(firstCause);
            assertSame(unmatchedSecond, ExceptionUtil.tryToGetOriginalCheckedException(unmatchedSecond));
        }
    }

    @Test
    void mapperStateChangesApplyToTheSameCause() throws Exception {
        try (Registration registration = new Registration(StatefulFailure.class)) {
            final AtomicBoolean useFirst = new AtomicBoolean(true);
            ExceptionUtil.registerRuntimeExceptionMapper(StatefulFailure.class,
                    failure -> useFirst.get() ? new FirstWrapper(failure) : new SecondWrapper(failure), true);
            final StatefulFailure cause = new StatefulFailure();
            final FirstWrapper first = new FirstWrapper(cause);
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(first));

            useFirst.set(false);
            assertSame(first, ExceptionUtil.tryToGetOriginalCheckedException(first));
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(new SecondWrapper(cause)));
        }
    }

    @Test
    void mapperReplacingItselfCannotPopulateReplacementCache() throws Exception {
        try (Registration registration = new Registration(ReentrantFailure.class)) {
            final ReentrantFailure cause = new ReentrantFailure();
            final FirstWrapper first = new FirstWrapper(cause);
            ExceptionUtil.registerRuntimeExceptionMapper(ReentrantFailure.class, failure -> {
                ExceptionUtil.registerRuntimeExceptionMapper(ReentrantFailure.class, SecondWrapper::new, true);
                return new FirstWrapper(failure);
            }, true);

            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(first));
            assertSame(first, ExceptionUtil.tryToGetOriginalCheckedException(first));
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(new SecondWrapper(cause)));
        }
    }

    @Test
    void replacingMapperInvalidatesCachedWrapperPairing() throws Exception {
        try (Registration registration = new Registration(DirectFailure.class)) {
            final DirectFailure cause = new DirectFailure();
            final FirstWrapper first = new FirstWrapper(cause);
            ExceptionUtil.registerRuntimeExceptionMapper(DirectFailure.class, FirstWrapper::new, true);
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(first));
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(first));

            ExceptionUtil.registerRuntimeExceptionMapper(DirectFailure.class, SecondWrapper::new, true);
            assertSame(first, ExceptionUtil.tryToGetOriginalCheckedException(first));
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(new SecondWrapper(cause)));

            ExceptionUtil.registerRuntimeExceptionMapper(DirectFailure.class, FirstWrapper::new, true);
            final SecondWrapper second = new SecondWrapper(cause);
            assertSame(second, ExceptionUtil.tryToGetOriginalCheckedException(second));
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(first));
        }
    }

    @Test
    @Timeout(15)
    void oldInFlightMapperCannotPopulateReplacementCache() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        try (Registration registration = new Registration(RacingFailure.class);
             var executor = Executors.newSingleThreadExecutor()) {
            final RacingFailure cause = new RacingFailure();
            final FirstWrapper first = new FirstWrapper(cause);
            ExceptionUtil.registerRuntimeExceptionMapper(RacingFailure.class, failure -> {
                entered.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS), "replacement registration completed");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                return new FirstWrapper(failure);
            }, true);

            final var pending = executor.submit(() -> ExceptionUtil.tryToGetOriginalCheckedException(first));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS), "original mapper started");
                ExceptionUtil.registerRuntimeExceptionMapper(RacingFailure.class, SecondWrapper::new, true);
            } finally {
                release.countDown();
            }
            assertSame(cause, pending.get(5, TimeUnit.SECONDS));
            assertSame(first, ExceptionUtil.tryToGetOriginalCheckedException(first));
            assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(new SecondWrapper(cause)));
        } finally {
            release.countDown();
        }
    }

    private static final class FirstWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

        FirstWrapper(final Throwable cause) {
            super(cause);
        }
    }

    private static final class SecondWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

        SecondWrapper(final Throwable cause) {
            super(cause);
        }
    }

    // Registration has no public removal API; restore the private registry to keep these tests isolated.
    private static final class Registration implements AutoCloseable {
        private final Class<? extends Throwable> type;
        private final Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>> registry;
        private final Object lock;
        private final Function<Throwable, RuntimeException> previous;

        @SuppressWarnings("unchecked")
        Registration(final Class<? extends Throwable> type) throws Exception {
            this.type = type;
            registry = (Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>>) field("toRuntimeExceptionFuncMap").get(null);
            lock = field("runtimeExceptionMapperLock").get(null);
            synchronized (lock) {
                previous = registry.get(type);
            }
        }

        @Override
        public void close() throws Exception {
            synchronized (lock) {
                if (previous == null) {
                    registry.remove(type);
                } else {
                    registry.put(type, previous);
                }
                final Method newCache = ExceptionUtil.class.getDeclaredMethod("newResolutionCache");
                newCache.setAccessible(true);
                // Replace the generation so inherited resolutions cannot outlive the restored registration.
                field("resolvedToRuntimeExceptionFuncCache").set(null, newCache.invoke(null));
            }
        }

        private static Field field(final String name) throws Exception {
            final Field field = ExceptionUtil.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
    }
}
