package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.eventbus.EventBus;
import com.landawn.abacus.eventbus.Subscribe;

@Tag("unit")
public class OpenUtilContractsTest extends TestBase {
    @Test
    void fnnAdaptersPreserveFailuresAndMutableGroupingContracts() throws Exception {
        final IllegalStateException conversion = new IllegalStateException("conversion");
        final Object input = new Object() {
            @Override
            public String toString() {
                throw conversion;
            }
        };
        assertSame(conversion, assertThrows(IllegalStateException.class, () -> Fnn.toStr().apply(input)));
        assertEquals("null", Fnn.toStr().apply(null));
        final var closer = Fnn.<AutoCloseable, Exception> closeQuietly();
        closer.accept(null);
        closer.accept(() -> {
            throw new IOException("suppressed");
        });
        final AssertionError error = new AssertionError("close");
        assertSame(error, assertThrows(AssertionError.class, () -> closer.accept(() -> {
            throw error;
        })));
        final boolean interrupted = Thread.interrupted();
        try {
            closer.accept(() -> {
                throw new InterruptedException("restore");
            });
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (interrupted)
                Thread.currentThread().interrupt();
        }
        final Pair<String, String> pair = Fnn.<String, String, Exception> pair().apply(null, "");
        pair.setLeft("甲🙂");
        pair.setRight(null);
        assertEquals("甲🙂", pair.getLeft());
        assertNull(pair.getRight());
        final Triple<String, String, String> triple = Fnn.<String, String, String, Exception> triple().apply(null, "", "甲🙂");
        triple.setLeft("");
        triple.setMiddle("乙🙂");
        triple.setRight(null);
        assertEquals("", triple.getLeft());
        assertEquals("乙🙂", triple.getMiddle());
        assertNull(triple.getRight());
    }

    public static class Listener {
        Thread thread;
        String value;

        @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
        public void receive(String event) {
            thread = Thread.currentThread();
            value = event;
        }
    }

    @Test
    void executorDispatchCanRunOnThePostingThread() {
        final EventBus bus = EventBus.create("direct-contract", Runnable::run);
        final Listener listener = new Listener();
        bus.register(listener);
        try {
            bus.post("你好🙂");
            assertSame(Thread.currentThread(), listener.thread);
            assertEquals("你好🙂", listener.value);
        } finally {
            bus.unregister(listener);
        }
    }

    @Test
    void ownedAndBorrowedExecutorsHaveDifferentDirectWorkTerminationScopes() throws Exception {
        for (boolean owned : new boolean[] { true, false }) {
            final var pool = Executors.newSingleThreadExecutor();
            final AsyncExecutor wrapper = owned ? new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS) : new AsyncExecutor(pool);
            final CountDownLatch started = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            final CountDownLatch finished = new CountDownLatch(1);
            wrapper.getExecutor().execute(() -> {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS));
                wrapper.shutdown();
                assertEquals(!owned, wrapper.isTerminated());
            } finally {
                release.countDown();
                assertTrue(finished.await(5, TimeUnit.SECONDS));
                wrapper.shutdownAndAwait(5, TimeUnit.SECONDS);
                pool.shutdownNow();
                assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            }
            assertTrue(wrapper.isTerminated());
        }
    }

    @Test
    void allComposeOverloadsExposeZipFailuresThroughExecutionException() throws Exception {
        final Future<Integer> input = CompletableFuture.completedFuture(1);
        for (final Exception cause : List.of(new IOException("读取"), new IllegalStateException("计算"))) {
            final List<Future<?>> composed = List.of(Futures.compose(input, input, (a, b) -> {
                throw cause;
            }), Futures.compose(input, input, (a, b) -> {
                throw cause;
            }, tuple -> {
                throw cause;
            }), Futures.compose(input, input, input, (a, b, c) -> {
                throw cause;
            }), Futures.compose(input, input, input, (a, b, c) -> {
                throw cause;
            }, tuple -> {
                throw cause;
            }), Futures.compose(List.of(input), values -> {
                throw cause;
            }), Futures.compose(List.of(input), values -> {
                throw cause;
            }, tuple -> {
                throw cause;
            }));
            for (final Future<?> future : composed) {
                assertSame(cause, assertThrows(ExecutionException.class, future::get).getCause());
                assertSame(cause, assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS)).getCause());
            }
        }
        final java.util.concurrent.CancellationException cancellation = new java.util.concurrent.CancellationException("cancelled");
        final Future<?> cancelled = Futures.compose(input, input, (a, b) -> {
            throw cancellation;
        });
        assertSame(cancellation, assertThrows(java.util.concurrent.CancellationException.class, cancelled::get));
    }

    @Test
    @ResourceLock("SYSTEM_PROPERTIES")
    void malformedSpecificationPropertyDoesNotPoisonFreshJavaVersionInitialization() throws Exception {
        final String previous = System.getProperty("java.specification.version");
        try {
            for (String value : List.of("not-a-version", "版本", " ", "25", "")) {
                System.setProperty("java.specification.version", value);
                final String name = JavaVersion.class.getName();
                final ClassLoader loader = new ClassLoader(JavaVersion.class.getClassLoader()) {
                    @Override
                    protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
                        if (!requested.equals(name))
                            return super.loadClass(requested, resolve);
                        synchronized (getClassLoadingLock(requested)) {
                            Class<?> result = findLoadedClass(requested);
                            if (result == null) {
                                try (var input = getParent().getResourceAsStream(requested.replace('.', '/') + ".class")) {
                                    final byte[] bytes = input.readAllBytes();
                                    result = defineClass(requested, bytes, 0, bytes.length);
                                } catch (IOException e) {
                                    throw new ClassNotFoundException(requested, e);
                                }
                            }
                            if (resolve)
                                resolveClass(result);
                            return result;
                        }
                    }
                };
                final Class<?> fresh = Class.forName(name, true, loader);
                assertNotNull(fresh.getField("JAVA_RECENT").get(null));
                final Method max = fresh.getDeclaredMethod("maxVersion");
                max.setAccessible(true);
                assertEquals(value.equals("25") ? 25f : 99f, max.invoke(null));
            }
        } finally {
            if (previous == null)
                System.clearProperty("java.specification.version");
            else
                System.setProperty("java.specification.version", previous);
        }
    }
}
