package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fn.BiFunctions;
import com.landawn.abacus.util.Fn.BiPredicates;
import com.landawn.abacus.util.Fn.Consumers;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.Entries;
import com.landawn.abacus.util.Fn.FB;
import com.landawn.abacus.util.Fn.FC;
import com.landawn.abacus.util.Fn.FD;
import com.landawn.abacus.util.Fn.FF;
import com.landawn.abacus.util.Fn.FI;
import com.landawn.abacus.util.Fn.FL;
import com.landawn.abacus.util.Fn.FS;
import com.landawn.abacus.util.Fn.Functions;
import com.landawn.abacus.util.Fn.Predicates;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.IntBiObjConsumer;
import com.landawn.abacus.util.function.IntBiObjFunction;
import com.landawn.abacus.util.function.IntBiObjPredicate;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.IntObjPredicate;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBinaryOperator;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.Runnable;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.function.UnaryOperator;
import com.landawn.abacus.util.stream.Collectors;

public class FnTest extends TestBase {

    public static class MyCloseable implements AutoCloseable {
        private final AtomicInteger closeCount = new AtomicInteger(0);

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }

        public int getCloseCount() {
            return closeCount.get();
        }
    }

    @Nested
    public class SuppliersTest {
    }

    @Nested
    public class IntFunctionsTest {
    }

    @Nested
    public class PredicatesTest {
    }

    @Nested
    public class BiPredicatesTest {
    }

    @Nested
    public class FnnTest {
    }

    @Nested
    public class BinaryOperatorsTest {
    }

    @Nested
    public class EntriesTest {
    }

    @Nested
    public class FITest {
    }

    @Nested
    public class FLTest {
    }

    @Test
    public void testMemoizeSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        java.util.function.Supplier<Integer> javaSupplier = () -> counter.incrementAndGet();

        Supplier<Integer> memoized = Fn.memoize(javaSupplier);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void memoize() {
        Supplier<Integer> supplier = Fn.memoize(() -> 1);
        assertThat(supplier.get()).isEqualTo(1);
    }

    @Test
    public void memoizeFunction() {
        Function<Integer, Integer> function = Fn.memoize(i -> i + 1);
        assertThat(function.apply(1)).isEqualTo(2);
        assertThat(function.apply(1)).isEqualTo(2);
    }

    @Test
    public void testMemoizeFunction() {
        AtomicInteger counter = new AtomicInteger(0);
        java.util.function.Function<String, Integer> func = (s) -> {
            counter.incrementAndGet();
            return s == null ? -1 : s.length();
        };

        Function<String, Integer> memoized = Fn.memoize(func);

        assertEquals(3, memoized.apply("abc"));
        assertEquals(3, memoized.apply("abc"));
        assertEquals(1, counter.get());

        assertEquals(4, memoized.apply("abcd"));
        assertEquals(4, memoized.apply("abcd"));
        assertEquals(2, counter.get());

        assertEquals(-1, memoized.apply(null));
        assertEquals(-1, memoized.apply(null));
        assertEquals(3, counter.get());
    }

    @Test
    public void testMemoizeFunctionWithNull() {
        Function<String, String> memoized = Fn.memoize(s -> s == null ? "null" : s.toUpperCase());
        assertEquals("null", memoized.apply(null));
        assertEquals("null", memoized.apply(null));
        assertEquals("HELLO", memoized.apply("hello"));
    }

    @Test
    public void testMemoize() {
        AtomicInteger counter = new AtomicInteger(0);
        Throwables.Supplier<Integer, IOException> supplier = () -> {
            counter.incrementAndGet();
            return 1;
        };
        Throwables.Supplier<Integer, IOException> memoized = Fnn.memoize(supplier);

        assertDoesNotThrow(() -> {
            assertEquals(1, memoized.get());
            assertEquals(1, memoized.get());
        });
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoize_Supplier() {
        AtomicInteger counter = new AtomicInteger();
        Throwables.Supplier<String, Exception> original = () -> "Value-" + counter.incrementAndGet();

        Throwables.Supplier<String, Exception> memoized = Fnn.memoize(original);

        try {
            String result1 = memoized.get();
            String result2 = memoized.get();
            String result3 = memoized.get();

            assertEquals("Value-1", result1);
            assertEquals("Value-1", result2);
            assertEquals("Value-1", result3);
            assertEquals(1, counter.get());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testMemoize_Function() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Throwables.Function<String, String, Exception> original = s -> s + "-" + counter.incrementAndGet();

        Throwables.Function<String, String, Exception> memoized = Fnn.memoize(original);

        assertEquals("A-1", memoized.apply("A"));
        assertEquals("A-1", memoized.apply("A"));
        assertEquals("B-2", memoized.apply("B"));
        assertEquals("B-2", memoized.apply("B"));
        assertEquals("A-1", memoized.apply("A"));

        assertEquals(2, counter.get());

        assertEquals("null-3", memoized.apply(null));
        assertEquals("null-3", memoized.apply(null));
    }

    @Test
    public void testMemoizeWithExpiration_Duration() {
        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoizeWithExpiration(() -> callCount.incrementAndGet(), Duration.ofSeconds(10));
        assertEquals(Integer.valueOf(1), memoized.get());
        assertEquals(Integer.valueOf(1), memoized.get());
        assertEquals(1, callCount.get());
    }

    @Test
    public void testGet() {
        // Tests the anonymous Supplier.get() in memoizeWithExpiration
        AtomicInteger counter = new AtomicInteger(0);
        java.util.function.Supplier<String> supplier = () -> "value_" + counter.incrementAndGet();
        Supplier<String> memoized = Fn.memoizeWithExpiration(supplier, 5, TimeUnit.SECONDS);

        String val1 = memoized.get();
        assertEquals("value_1", val1);
        // Cached - should return same value
        String val2 = memoized.get();
        assertEquals("value_1", val2);
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoizeWithExpiration() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        java.util.function.Supplier<Integer> javaSupplier = counter::incrementAndGet;

        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(javaSupplier, 0, TimeUnit.MILLISECONDS));

        Supplier<Integer> memoized = Fn.memoizeWithExpiration(javaSupplier, 100, TimeUnit.MILLISECONDS);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());

        Thread.sleep(150);

        assertEquals(2, memoized.get());
        assertEquals(2, memoized.get());
        assertEquals(2, counter.get());
    }

    @Test
    public void memoizeWithExpiration() throws InterruptedException {
        Supplier<Integer> supplier = Fn.memoizeWithExpiration(() -> 1, 1, TimeUnit.SECONDS);
        assertThat(supplier.get()).isEqualTo(1);
        Thread.sleep(1100);
        assertThat(supplier.get()).isEqualTo(1);
    }

    @Test
    public void testMemoizeWithExpirationInvalidArgument() {
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> 1, -1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> 1, 0, TimeUnit.SECONDS));
    }

    @Test
    public void testMemoizeWithExpirationConcurrency() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoizeWithExpiration(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return counter.incrementAndGet();
        }, 200, TimeUnit.MILLISECONDS);

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(CompletableFuture.supplyAsync(memoized::get));
        }

        List<Integer> results = futures.stream().map(CompletableFuture::join).collect(java.util.stream.Collectors.toList());

        assertTrue(results.stream().allMatch(r -> r.equals(1)));
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoizeWithExpirationEdgeCase() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoizeWithExpiration(counter::incrementAndGet, 1, TimeUnit.NANOSECONDS);

        int first = memoized.get();
        Thread.sleep(1);
        int second = memoized.get();

        assertNotEquals(first, second);
    }

    @Test
    public void testClose() {
        MyCloseable closeable = new MyCloseable();
        Runnable closer = Fn.close(closeable);
        assertEquals(0, closeable.getCloseCount());
        closer.run();
        assertEquals(1, closeable.getCloseCount());
        closer.run();
        assertEquals(1, closeable.getCloseCount());
    }

    @Test
    public void testCloseConsumer() {
        MyCloseable c = new MyCloseable();
        Consumer<MyCloseable> consumer = Fn.close();
        consumer.accept(c);
        assertEquals(1, c.getCloseCount());
    }

    @Test
    public void testRun() {
        // Tests the anonymous Runnable.run() methods in close/closeAll/closeQuietly/closeAllQuietly/shutDown
        // close(AutoCloseable) run()
        MyCloseable c1 = new MyCloseable();
        Runnable closeRunnable = Fn.close(c1);
        closeRunnable.run();
        assertEquals(1, c1.getCloseCount());
        closeRunnable.run(); // second call should be no-op
        assertEquals(1, c1.getCloseCount());

        // closeAll(AutoCloseable...) run()
        MyCloseable c2 = new MyCloseable();
        MyCloseable c3 = new MyCloseable();
        Runnable closeAllRunnable = Fn.closeAll(c2, c3);
        closeAllRunnable.run();
        assertEquals(1, c2.getCloseCount());
        assertEquals(1, c3.getCloseCount());
        closeAllRunnable.run(); // no-op
        assertEquals(1, c2.getCloseCount());

        // closeAll(Collection) run()
        MyCloseable c4 = new MyCloseable();
        Runnable closeAllCollRunnable = Fn.closeAll(Arrays.asList(c4));
        closeAllCollRunnable.run();
        assertEquals(1, c4.getCloseCount());
        closeAllCollRunnable.run();
        assertEquals(1, c4.getCloseCount());

        // closeQuietly run()
        MyCloseable c5 = new MyCloseable();
        Runnable closeQuietlyRunnable = Fn.closeQuietly(c5);
        closeQuietlyRunnable.run();
        assertEquals(1, c5.getCloseCount());
        closeQuietlyRunnable.run();
        assertEquals(1, c5.getCloseCount());

        // closeAllQuietly(AutoCloseable...) run()
        MyCloseable c6 = new MyCloseable();
        Runnable closeAllQArr = Fn.closeAllQuietly(c6);
        closeAllQArr.run();
        assertEquals(1, c6.getCloseCount());
        closeAllQArr.run();
        assertEquals(1, c6.getCloseCount());

        // closeAllQuietly(Collection) run()
        MyCloseable c7 = new MyCloseable();
        Runnable closeAllQColl = Fn.closeAllQuietly(Arrays.asList(c7));
        closeAllQColl.run();
        assertEquals(1, c7.getCloseCount());
        closeAllQColl.run();
        assertEquals(1, c7.getCloseCount());

        // shutDown(ExecutorService) run()
        ExecutorService es1 = Executors.newSingleThreadExecutor();
        Runnable shutdownRun = Fn.shutDown(es1);
        shutdownRun.run();
        assertTrue(es1.isShutdown());
        shutdownRun.run(); // no-op
        assertTrue(es1.isShutdown());

        // shutDown(ExecutorService, long, TimeUnit) run()
        ExecutorService es2 = Executors.newSingleThreadExecutor();
        Runnable shutdownTimeoutRun = Fn.shutDown(es2, 100, TimeUnit.MILLISECONDS);
        shutdownTimeoutRun.run();
        assertTrue(es2.isShutdown());
        shutdownTimeoutRun.run(); // no-op
        assertTrue(es2.isShutdown());
    }

    @Test
    public void close() {
        AutoCloseable closeable = mock(AutoCloseable.class);
        Fn.close(closeable).run();
        try {
            verify(closeable, times(1)).close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testCloseAllArray() {
        MyCloseable c1 = new MyCloseable();
        MyCloseable c2 = new MyCloseable();
        Runnable closer = Fn.closeAll(c1, c2);
        assertEquals(0, c1.getCloseCount());
        assertEquals(0, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
    }

    @Test
    public void testCloseAllCollection() {
        MyCloseable c1 = new MyCloseable();
        MyCloseable c2 = new MyCloseable();
        Runnable closer = Fn.closeAll(Arrays.asList(c1, c2));
        assertEquals(0, c1.getCloseCount());
        assertEquals(0, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
    }

    @Test
    public void closeAll() {
        AutoCloseable closeable1 = mock(AutoCloseable.class);
        AutoCloseable closeable2 = mock(AutoCloseable.class);
        Fn.closeAll(closeable1, closeable2).run();
        try {
            verify(closeable1, times(1)).close();
            verify(closeable2, times(1)).close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testCloseAll() throws Exception {
        AtomicBoolean closed1 = new AtomicBoolean(false);
        AtomicBoolean closed2 = new AtomicBoolean(false);
        AutoCloseable closeable1 = () -> closed1.set(true);
        AutoCloseable closeable2 = () -> closed2.set(true);

        Runnable closer = Fn.closeAll(closeable1, closeable2);
        assertFalse(closed1.get());
        assertFalse(closed2.get());

        closer.run();
        assertTrue(closed1.get());
        assertTrue(closed2.get());
    }

    @Test
    public void testCloseQuietly() {
        MyCloseable c = new MyCloseable();
        Runnable closer = Fn.closeQuietly(c);
        closer.run();
        assertEquals(1, c.getCloseCount());
        closer.run();
        assertEquals(1, c.getCloseCount());
    }

    @Test
    public void testCloseQuietlyConsumer() {
        MyCloseable c = new MyCloseable();
        Consumer<MyCloseable> consumer = Fn.closeQuietly();
        consumer.accept(c);
        assertEquals(1, c.getCloseCount());
    }

    @Test
    public void testCloseAllQuietlyArray() {
        MyCloseable c1 = new MyCloseable();
        MyCloseable c2 = new MyCloseable();
        Runnable closer = Fn.closeAllQuietly(c1, c2);
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
    }

    @Test
    public void testCloseAllQuietlyCollection() {
        MyCloseable c1 = new MyCloseable();
        MyCloseable c2 = new MyCloseable();
        Runnable closer = Fn.closeAllQuietly(Arrays.asList(c1, c2));
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
    }

    @Test
    public void closeAllQuietly() {
        AutoCloseable closeable1 = mock(AutoCloseable.class);
        AutoCloseable closeable2 = mock(AutoCloseable.class);
        Fn.closeAllQuietly(closeable1, closeable2).run();
        try {
            verify(closeable1, times(1)).close();
            verify(closeable2, times(1)).close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testCloseAllQuietly() {
        AutoCloseable failing1 = () -> {
            throw new Exception("Test exception 1");
        };
        AutoCloseable failing2 = () -> {
            throw new Exception("Test exception 2");
        };

        Runnable closer = Fn.closeAllQuietly(failing1, failing2);

        assertDoesNotThrow(() -> closer.run());
    }

    @Test
    public void testEmptyAction() {
        assertDoesNotThrow(() -> Fn.emptyAction().run());
    }

    @Test
    public void emptyAction() {
        assertDoesNotThrow(() -> {
            Fn.emptyAction().run();
        });
    }

    @Test
    public void testShutDown() {
        ExecutorService mockService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
            @Override
            public void shutdown() {
                super.shutdown();
            }
        };
        Runnable shutdown = Fn.shutDown(mockService);
        assertFalse(mockService.isShutdown());
        shutdown.run();
        assertTrue(mockService.isShutdown());
        shutdown.run();
    }

    @Test
    public void testShutDownWithTimeout() throws InterruptedException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Runnable shutdown = Fn.shutDown(service, 100, TimeUnit.MILLISECONDS);
        assertFalse(service.isShutdown());
        shutdown.run();
        assertTrue(service.isShutdown());
        assertTrue(service.awaitTermination(200, TimeUnit.MILLISECONDS));
    }

    @Test
    public void doNothing() {
        assertDoesNotThrow(() -> {
            Fn.emptyConsumer().accept(1);
        });
    }

    @Test
    public void testDoNothing() {
        Throwables.Consumer<String, RuntimeException> doNothing = Fnn.doNothing();
        Assertions.assertDoesNotThrow(() -> doNothing.accept("test"));
        Assertions.assertDoesNotThrow(() -> doNothing.accept(null));
    }

    @Test
    public void testEmptyConsumer() {
        assertDoesNotThrow(() -> Fn.emptyConsumer().accept("test"));
        assertDoesNotThrow(() -> Fn.doNothing().accept("test"));
    }

    @Test
    public void testThrowRuntimeException() {
        Exception e = assertThrows(RuntimeException.class, () -> Fn.throwRuntimeException("error").accept("test"));
        assertEquals("error", e.getMessage());
    }

    @Test
    public void throwRuntimeException() {
        assertThatThrownBy(() -> Fn.throwRuntimeException("error").accept(1)).isInstanceOf(RuntimeException.class).hasMessage("error");
    }

    @Test
    public void testThrowException() {
        assertThrows(IllegalStateException.class, () -> Fn.throwException(IllegalStateException::new).accept("test"));
    }

    @Test
    public void throwException() {
        assertThatThrownBy(() -> Fn.throwException(() -> new RuntimeException("error")).accept(1)).isInstanceOf(RuntimeException.class).hasMessage("error");
    }

    @Test
    public void testThrowExceptionWithSupplier() {
        Throwables.Consumer<String, IllegalStateException> thrower = Fnn.throwException(() -> new IllegalStateException("Supplied error"));
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> thrower.accept("test"));
        Assertions.assertEquals("Supplied error", ex.getMessage());
    }

    @Test
    public void testToRuntimeException() {
        Function<Throwable, RuntimeException> func = Fn.toRuntimeException();
        RuntimeException rte = new RuntimeException("test");
        assertSame(rte, func.apply(rte));

        Exception e = new Exception("test");
        RuntimeException result = func.apply(e);
        assertEquals("java.lang.Exception: test", result.getMessage());
        assertSame(e, result.getCause());
    }

    @Test
    public void testSleep() {
        long start = System.currentTimeMillis();
        Fn.sleep(10).accept(null);
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 10);
    }

    @Test
    public void sleep() {
        assertDoesNotThrow(() -> {
            Fn.sleep(10).accept(1);
        });
    }

    @Test
    public void testOfCurrentTimeMillis() {
        LongSupplier supplier = Fn.LongSuppliers.ofCurrentTimeMillis();
        Assertions.assertNotNull(supplier);

        long time1 = supplier.getAsLong();
        Assertions.assertTrue(time1 > 0);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long time2 = supplier.getAsLong();
        Assertions.assertTrue(time2 >= time1);
    }

    @Test
    public void testLongSuppliersCurrentTimeMillis() {
        LongSupplier currentTime = Fn.LongSuppliers.ofCurrentTimeMillis();

        long time1 = currentTime.getAsLong();
        assertTrue(time1 > 0);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }

        long time2 = currentTime.getAsLong();
        assertTrue(time2 >= time1);
    }

    @Test
    public void testSleepConsumer() throws InterruptedException {
        Consumer<String> sleeper = Fn.sleep(50);

        long start = System.currentTimeMillis();
        sleeper.accept("test");
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 50);
        assertTrue(duration < 100);
    }

    @Test
    public void testSleepUninterruptibly() {
        long start = System.currentTimeMillis();
        Fn.sleepUninterruptibly(10).accept(null);
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 10);
    }

    @Test
    public void sleepUninterruptibly() {
        assertDoesNotThrow(() -> {
            Fn.sleepUninterruptibly(10).accept(1);
        });
    }

    @Test
    public void testRateLimiterWithRateLimiterInstance() {
        RateLimiter rateLimiter = RateLimiter.create(2.0);
        Consumer<String> rateLimited = Fn.rateLimiter(rateLimiter);

        long start = System.currentTimeMillis();
        rateLimited.accept("1");
        rateLimited.accept("2");
        rateLimited.accept("3");
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 500);
    }

    @Test
    public void testRateLimiterWithInstance() {
        RateLimiter rateLimiter = RateLimiter.create(10.0);
        Consumer<String> rateLimited = Fn.rateLimiter(rateLimiter);

        rateLimited.accept("test");
        assertNotNull(rateLimited);
    }

    @Test
    public void testRateLimiter() {
        assertDoesNotThrow(() -> Fn.rateLimiter(10.0).accept("test"));
        RateLimiter limiter = RateLimiter.create(10);
        assertDoesNotThrow(() -> Fn.rateLimiter(limiter).accept("test"));
    }

    @Test
    public void testRateLimiterWithPermitsPerSecond() throws Exception {
        Throwables.Consumer<String, RuntimeException> rateLimited = Fnn.rateLimiter(10.0);
        long start = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            rateLimited.accept("test");
        }

        long duration = System.currentTimeMillis() - start;
        Assertions.assertTrue(duration >= 180);
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> Fn.println().accept("test"));
    }

    @Test
    public void testPrintlnWithSeparator() {
        assertDoesNotThrow(() -> Fn.println("=").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(":").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(": ").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println("-").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println("_").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(",").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(", ").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println("").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(" custom ").accept("key", "value"));
    }

    @Test
    public void println() {
        assertDoesNotThrow(() -> {
            Fn.println().accept("hello");
        });
    }

    @Test
    public void printlnWithSeparator() {
        assertDoesNotThrow(() -> {
            Fn.println("=").accept("key", "value");
        });
    }

    @Test
    public void testPrintlnSeparatorWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Fn.println(null));
    }

    @Test
    public void toStr() {
        assertThat(Fn.toStr().apply(1)).isEqualTo("1");
    }

    @Test
    public void testToStr() {
        assertEquals("123", Fn.toStr().apply(123));
        assertEquals("null", Fn.toStr().apply(null));
    }

    @Test
    public void testToLowerCase() {
        assertEquals("foo", Fn.toLowerCase().apply("FOO"));
    }

    @Test
    public void toLowerCase() {
        assertThat(Fn.toLowerCase().apply("HELLO")).isEqualTo("hello");
    }

    @Test
    public void testToUpperCase() {
        assertEquals("FOO", Fn.toUpperCase().apply("foo"));
    }

    @Test
    public void toUpperCase() {
        assertThat(Fn.toUpperCase().apply("hello")).isEqualTo("HELLO");
    }

    @Test
    public void testExceptionHandlingInDepth() {
        Throwables.Function<String, String, IOException> throwableFunc = s -> {
            if (s.equals("error")) {
                throw new IOException("Test error");
            }
            return s.toUpperCase();
        };

        Function<String, String> safeFunc = Fn.ff(throwableFunc, "DEFAULT");
        assertEquals("HELLO", safeFunc.apply("hello"));
        assertEquals("DEFAULT", safeFunc.apply("error"));

        Function<String, String> propagatingFunc = Fn.ff(throwableFunc);
        assertEquals("HELLO", propagatingFunc.apply("hello"));
        assertThrows(RuntimeException.class, () -> propagatingFunc.apply("error"));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("fooBar", Fn.toCamelCase().apply("foo_bar"));
    }

    @Test
    public void toCamelCase() {
        assertThat(Fn.toCamelCase().apply("hello_world")).isEqualTo("helloWorld");
    }

    @Test
    public void testToSnakeCase() {
        assertEquals("foo_bar", Fn.toSnakeCase().apply("fooBar"));
    }

    @Test
    public void toSnakeCase() {
        assertThat(Fn.toSnakeCase().apply("helloWorld")).isEqualTo("hello_world");
    }

    @Test
    public void testToScreamingSnakeCase() {
        assertEquals("FOO_BAR", Fn.toScreamingSnakeCase().apply("fooBar"));
    }

    @Test
    public void toScreamingSnakeCase() {
        assertThat(Fn.toScreamingSnakeCase().apply("helloWorld")).isEqualTo("HELLO_WORLD");
    }

    @Test
    public void testToJson() {
        assertEquals("[1, 2]", Fn.toJson().apply(Arrays.asList(1, 2)));
    }

    @Test
    public void toJson() {
        assertThat(Fn.toJson().apply(new int[] { 1, 2 })).isEqualTo("[1, 2]");
    }

    @Test
    public void testToXml() {
        assertEquals("<list>[1, 2]</list>", Fn.toXml().apply(Arrays.asList(1, 2)));
    }

    @Test
    public void toXml() {
        assertThat(Fn.toXml().apply(new int[] { 1, 2 })).isNotEmpty();
    }

    @Test
    public void identity() {
        assertThat(Fn.identity().apply(1)).isEqualTo(1);
    }

    // FI identity and comparison methods
    @Test
    public void testFI_notEqual() {
        assertTrue(FI.notEqual().test(1, 2));
        assertFalse(FI.notEqual().test(1, 1));
    }

    @Test
    public void testIdentity() {
        String s = "test";
        assertSame(s, Fn.identity().apply(s));
        assertNull(Fn.identity().apply(null));
    }

    //
    //    @Test
    //    public void testTapWithEmptyConsumerReturnsCanonicalIdentity() {
    //        assertSame(Fn.identity(), Fn.tap(Fn.emptyConsumer()));
    //    }

    //
    //
    //
    @Test
    public void testKeyed() {
        Function<String, Keyed<Integer, String>> keyedFunc = Fn.keyed(String::length);
        Keyed<Integer, String> result = keyedFunc.apply("hello");
        assertEquals(5, result.key());
        assertEquals("hello", result.val());
    }

    @Test
    public void keyed() {
        assertThat(Fn.keyed(i -> i).apply(1).val()).isEqualTo(1);
    }

    @Test
    public void testFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.keyed(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.wrap(null, (a, b) -> true));
        assertThrows(IllegalArgumentException.class, () -> Fn.wrap(String::hashCode, null));
        assertThrows(IllegalArgumentException.class, () -> Fn.cast(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.instanceOf(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.subtypeOf(null));
    }

    @Test
    public void testVal() {
        Keyed<Integer, String> keyed = Keyed.of(5, "hello");
        assertEquals("hello", Fn.<Integer, String> val().apply(keyed));
    }

    @Test
    public void valOfKeyed() {
        assertThat(Fn.val().apply(Fn.keyed(i -> i).apply(1))).isEqualTo(1);
    }

    @Test
    public void testKkv() {
        Keyed<String, Integer> keyedKey = Keyed.of("key", 123);
        Map.Entry<Keyed<String, Integer>, String> entry = CommonUtil.newEntry(keyedKey, "value");
        assertEquals(123, Fn.<String, Integer, String> kkv().apply(entry));
    }

    @Test
    public void testWrapAndUnwrap() {
        String s = "test";
        Wrapper<String> wrapped = Fn.<String> wrap().apply(s);
        assertEquals(s, wrapped.value());
        assertEquals(s, Fn.<String> unwrap().apply(wrapped));
    }

    @Test
    public void testWrapWithFunctions() {
        Function<String, Wrapper<String>> wrapFunc = Fn.wrap(s -> 1, String::equals);
        Wrapper<String> wrapped = wrapFunc.apply("test");
        assertEquals(1, wrapped.hashCode());
        assertTrue(wrapped.equals(Wrapper.of("test")));
    }

    @Test
    public void wrap() {
        assertThat(Fn.wrap().apply(1).value()).isEqualTo(1);
    }

    @Test
    public void testWrap() {
        Function<String, Wrapper<String>> wrap = Fn.wrap();
        Wrapper<String> wrapped = wrap.apply("test");
        assertEquals("test", wrapped.value());
    }

    @Test
    public void testJoin() {
        Function<DisposableObjArray, String> function = Fn.Disposables.join(", ");

        Object[] array = new Object[] { "a", "b", "c" };
        DisposableObjArray disposable = DisposableObjArray.wrap(array);

        String result = function.apply(disposable);
        assertEquals("a, b, c", result);
    }

    @Test
    public void testWrapWithCustomFunctions() {
        Function<String, Wrapper<String>> wrap = Fn.wrap(String::length, (a, b) -> a.equalsIgnoreCase(b));
        Wrapper<String> wrapped = wrap.apply("test");
        assertEquals("test", wrapped.value());
    }

    @Test
    public void testCloneArray() {
        Function<DisposableObjArray, Object[]> function = Fn.Disposables.cloneArray();

        Object[] original = new Object[] { "a", "b", "c" };
        DisposableObjArray disposable = DisposableObjArray.wrap(original);

        Object[] cloned = function.apply(disposable);
        assertArrayEquals(original, cloned);
        assertNotSame(original, cloned);
    }

    @Test
    public void testWrapWithHashEquals() {
        Function<String, Wrapper<String>> wrap = Fn.wrap(str -> str == null ? 0 : str.toUpperCase().hashCode(),
                (a, b) -> a == null ? b == null : a.equalsIgnoreCase(b));

        Wrapper<String> wrapped1 = wrap.apply("hello");
        Wrapper<String> wrapped2 = wrap.apply("HELLO");

        assertEquals(wrapped1.hashCode(), wrapped2.hashCode());
        assertTrue(wrapped1.equals(wrapped2));
    }

    @Test
    public void unwrap() {
        assertThat(Fn.unwrap().apply(Fn.wrap().apply(1))).isEqualTo(1);
    }

    @Test
    public void testUnwrap() {
        Function<Wrapper<String>, String> unwrap = Fn.unwrap();
        Wrapper<String> wrapped = Wrapper.of("test");
        assertEquals("test", unwrap.apply(wrapped));
    }

    @Test
    public void testKeyAndValue() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("key", 123);
        assertEquals("key", Fn.<String, Integer> key().apply(entry));
        assertEquals(123, Fn.<String, Integer> value().apply(entry));
    }

    @Test
    public void testKeyVal() {
        BiPredicate<String, Integer> p = (s, i) -> s.length() == i;
        assertThat(Fn.testKeyVal(p).test(Fn.<String, Integer> entry().apply("abc", 1))).isFalse();
    }

    @Test
    public void key() {
        assertThat(Fn.key().apply(Fn.entry().apply("a", 1))).isEqualTo("a");
    }

    @Test
    public void testKey() {
        Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
        Throwables.Function<Map.Entry<String, Integer>, String, RuntimeException> keyFunc = Fnn.key();
        Assertions.assertEquals("key", keyFunc.apply(entry));
    }

    @Test
    public void value() {
        assertThat(Fn.value().apply(Fn.entry().apply("a", 1))).isEqualTo(1);
    }

    @Test
    public void testValue() {
        Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
        Throwables.Function<Map.Entry<String, Integer>, Integer, RuntimeException> valueFunc = Fnn.value();
        Assertions.assertEquals(100, valueFunc.apply(entry));
    }

    @Test
    public void testLeftAndRight() {
        Pair<String, Integer> pair = Pair.of("left", 123);
        assertEquals("left", Fn.<String, Integer> left().apply(pair));
        assertEquals(123, Fn.<String, Integer> right().apply(pair));
    }

    @Test
    public void testLeft() {
        Function<Pair<String, Integer>, String> leftFunc = Fn.left();
        Pair<String, Integer> pair = Pair.of("left", 123);
        assertEquals("left", leftFunc.apply(pair));
    }

    @Test
    public void testRight() {
        Function<Pair<String, Integer>, Integer> rightFunc = Fn.right();
        Pair<String, Integer> pair = Pair.of("left", 123);
        assertEquals(123, rightFunc.apply(pair));
    }

    @Test
    public void testInverse() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("key", 123);
        Map.Entry<Integer, String> inverted = Fn.<String, Integer> invert().apply(entry);
        assertEquals(123, inverted.getKey());
        assertEquals("key", inverted.getValue());
    }

    @Test
    public void invert() {
        assertThat(Fn.invert().apply(Fn.entry().apply("a", 1)).getKey()).isEqualTo(1);
    }

    @Test
    public void testEntry() {
        Map.Entry<String, Integer> entry = Fn.<String, Integer> entry().apply("key", 123);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void testEntryFunctions() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("key", 10);
        assertTrue(Fn.<String, Integer> testByKey(k -> k.equals("key")).test(entry));
        assertTrue(Fn.<String, Integer> testByValue(v -> v > 5).test(entry));

        AtomicReference<String> keyRef = new AtomicReference<>();
        Fn.<String, Integer> acceptByKey(keyRef::set).accept(entry);
        assertEquals("key", keyRef.get());

        AtomicReference<Integer> valRef = new AtomicReference<>();
        Fn.<String, Integer> acceptByValue(valRef::set).accept(entry);
        assertEquals(10, valRef.get());

        assertEquals(3, Fn.<String, Integer, Integer> applyByKey(String::length).apply(entry));
        assertEquals(20, Fn.<String, Integer, Integer> applyByValue(v -> v * 2).apply(entry));
    }

    @Test
    public void testEntryKeyVal() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("key", 3);
        assertTrue(Fn.<String, Integer> testKeyVal((k, v) -> k.length() == v).test(entry));

        AtomicReference<String> ref = new AtomicReference<>();
        Fn.<String, Integer> acceptKeyVal((k, v) -> ref.set(k + v)).accept(entry);
        assertEquals("key3", ref.get());

        String result = Fn.<String, Integer, String> applyKeyVal((k, v) -> k + v).apply(entry);
        assertEquals("key3", result);
    }

    @Test
    public void entry() {
        assertThat(Fn.entry().apply("a", 1).getKey()).isEqualTo("a");
    }

    @Test
    public void testDeprecatedEntry() {
        Function<Integer, Map.Entry<String, Integer>> entryFunc = Fn.entry("fixedKey");
        Map.Entry<String, Integer> entry = entryFunc.apply(123);
        assertEquals("fixedKey", entry.getKey());
        assertEquals(123, entry.getValue());

        Function<String, Map.Entry<Integer, String>> entryFunc2 = Fn.entry(String::length);
        Map.Entry<Integer, String> entry2 = entryFunc2.apply("hello");
        assertEquals(5, entry2.getKey());
        assertEquals("hello", entry2.getValue());
    }

    @Test
    public void testEntryFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.entryByKeyMapper(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.entryByValueMapper(null));
    }

    @Test
    public void testEntryWithKey() {
        Map.Entry<String, Integer> entry = Fn.<String, Integer> entryWithKey("fixed").apply(123);
        assertEquals("fixed", entry.getKey());
        assertEquals(123, entry.getValue());

        entry = Fn.<String, Integer> entry("fixed").apply(123);
        assertEquals("fixed", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void entryWithKey() {
        assertThat(Fn.entryWithKey("a").apply(1).getKey()).isEqualTo("a");
    }

    @Test
    public void testEntryByKeyMapper() {
        Function<Integer, Map.Entry<String, Integer>> func = Fn.entryByKeyMapper(i -> "key" + i);
        Map.Entry<String, Integer> entry = func.apply(123);
        assertEquals("key123", entry.getKey());
        assertEquals(123, entry.getValue());

        func = Fn.entry(i -> "key" + i);
        entry = func.apply(123);
        assertEquals("key123", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void entryByKeyMapper() {
        assertThat(Fn.entryByKeyMapper(Object::toString).apply(1).getKey()).isEqualTo("1");
    }

    @Test
    public void testEntryWithValue() {
        Map.Entry<String, Integer> entry = Fn.<String, Integer> entryWithValue(123).apply("key");
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void entryWithValue() {
        assertThat(Fn.entryWithValue(1).apply("a").getValue()).isEqualTo(1);
    }

    @Test
    public void testEntryByValueMapper() {
        Function<String, Map.Entry<String, Integer>> func = Fn.entryByValueMapper(s -> s.length());
        Map.Entry<String, Integer> entry = func.apply("key");
        assertEquals("key", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    public void entryByValueMapper() {
        assertThat(Fn.<String, Integer> entryByValueMapper(Integer::parseInt).apply("1").getValue()).isEqualTo(1);
    }

    @Test
    public void testPair() {
        Pair<String, Integer> pair = Fn.<String, Integer> pair().apply("key", 123);
        assertEquals("key", pair.left());
        assertEquals(123, pair.right());
    }

    @Test
    public void pair() {
        assertThat(Fn.pair().apply("a", 1).left()).isEqualTo("a");
    }

    @Test
    public void testTriple() {
        Triple<String, Integer, Boolean> triple = Fn.<String, Integer, Boolean> triple().apply("key", 123, true);
        assertEquals("key", triple.left());
        assertEquals(123, triple.middle());
        assertEquals(true, triple.right());
    }

    @Test
    public void triple() {
        assertThat(Fn.triple().apply("a", 1, true).left()).isEqualTo("a");
    }

    @Test
    public void testTuples() {
        assertEquals(Tuple.of("a"), Fn.tuple1().apply("a"));
        assertEquals(Tuple.of("a", 1), Fn.tuple2().apply("a", 1));
        assertEquals(Tuple.of("a", 1, true), Fn.tuple3().apply("a", 1, true));
        assertEquals(Tuple.of("a", 1, true, 2.0), Fn.tuple4().apply("a", 1, true, 2.0));
    }

    @Test
    public void tuple1() {
        assertThat(Fn.tuple1().apply(1)._1).isEqualTo(1);
    }

    @Test
    public void testTuple1() {
        Throwables.Function<String, Tuple1<String>, RuntimeException> tuple1Func = Fnn.tuple1();
        Tuple1<String> tuple = tuple1Func.apply("value");
        Assertions.assertEquals("value", tuple._1);
    }

    @Test
    public void tuple2() {
        assertThat(Fn.tuple2().apply(1, "a")._1).isEqualTo(1);
    }

    @Test
    public void testTuple2() {
        Throwables.BiFunction<String, Integer, Tuple2<String, Integer>, RuntimeException> tuple2Func = Fnn.tuple2();
        Tuple2<String, Integer> tuple = tuple2Func.apply("first", 100);
        Assertions.assertEquals("first", tuple._1);
        Assertions.assertEquals(100, tuple._2);
    }

    @Test
    public void tuple3() {
        assertThat(Fn.tuple3().apply(1, "a", true)._1).isEqualTo(1);
    }

    @Test
    public void testTuple3() {
        Throwables.TriFunction<String, Integer, Double, Tuple3<String, Integer, Double>, RuntimeException> tuple3Func = Fnn.tuple3();
        Tuple3<String, Integer, Double> tuple = tuple3Func.apply("first", 100, 3.14);
        Assertions.assertEquals("first", tuple._1);
        Assertions.assertEquals(100, tuple._2);
        Assertions.assertEquals(3.14, tuple._3);
    }

    @Test
    public void tuple4() {
        assertThat(Fn.tuple4().apply(1, "a", true, 2.0)._1).isEqualTo(1);
    }

    @Test
    public void testTuple4() {
        QuadFunction<String, Integer, Boolean, Double, Tuple4<String, Integer, Boolean, Double>> tupleFunc = Fn.tuple4();
        Tuple4<String, Integer, Boolean, Double> tuple = tupleFunc.apply("first", 123, true, 3.14);
        assertEquals("first", tuple._1);
        assertEquals(123, tuple._2);
        assertEquals(true, tuple._3);
        assertEquals(3.14, tuple._4);
    }

    @Test
    public void trim() {
        assertThat(Fn.trim().apply(" a ")).isEqualTo("a");
    }

    @Test
    public void testTrim() {
        UnaryOperator<String> trim = Fn.trim();
        assertEquals("hello", trim.apply("  hello  "));
    }

    @Test
    public void testStringOperators() {
        assertEquals("a", Fn.trim().apply(" a "));
        assertEquals("a", Fn.trimToEmpty().apply(" a "));
        assertEquals("", Fn.trimToEmpty().apply("  "));
        assertEquals("a", Fn.trimToNull().apply(" a "));
        assertNull(Fn.trimToNull().apply("  "));

        assertEquals("a", Fn.strip().apply(" a "));
        assertEquals("a", Fn.stripToEmpty().apply(" a "));
        assertEquals("", Fn.stripToEmpty().apply("  "));
        assertEquals("a", Fn.stripToNull().apply(" a "));
        assertNull(Fn.stripToNull().apply("  "));

        assertEquals("", Fn.nullToEmpty().apply(null));
        assertEquals("a", Fn.nullToEmpty().apply("a"));
    }

    @Test
    public void trimToEmpty() {
        assertThat(Fn.trimToEmpty().apply(" a ")).isEqualTo("a");
    }

    @Test
    public void testTrimToEmpty() {
        UnaryOperator<String> trimToEmpty = Fn.trimToEmpty();
        assertEquals("hello", trimToEmpty.apply("  hello  "));
        assertEquals("", trimToEmpty.apply(null));
    }

    @Test
    public void trimToNull() {
        assertThat(Fn.trimToNull().apply(" ")).isNull();
    }

    @Test
    public void testTrimToNull() {
        UnaryOperator<String> trimToNull = Fn.trimToNull();
        assertEquals("hello", trimToNull.apply("  hello  "));
        assertNull(trimToNull.apply("  "));
        assertNull(trimToNull.apply(null));
    }

    @Test
    public void strip() {
        assertThat(Fn.strip().apply(" a ")).isEqualTo("a");
    }

    @Test
    public void testStrip() {
        UnaryOperator<String> strip = Fn.strip();
        assertEquals("hello", strip.apply("  hello  "));
    }

    @Test
    public void stripToEmpty() {
        assertThat(Fn.stripToEmpty().apply(" a ")).isEqualTo("a");
    }

    @Test
    public void testStripToEmpty() {
        UnaryOperator<String> stripToEmpty = Fn.stripToEmpty();
        assertEquals("hello", stripToEmpty.apply("  hello  "));
        assertEquals("", stripToEmpty.apply(null));
    }

    @Test
    public void stripToNull() {
        assertThat(Fn.stripToNull().apply(" ")).isNull();
    }

    @Test
    public void testStripToNull() {
        UnaryOperator<String> stripToNull = Fn.stripToNull();
        assertEquals("hello", stripToNull.apply("  hello  "));
        assertNull(stripToNull.apply("  "));
    }

    @Test
    public void testNullToEmptyCollections() {
        assertTrue(Fn.nullToEmptyList().apply(null).isEmpty());
        assertTrue(Fn.nullToEmptySet().apply(null).isEmpty());
        assertTrue(Fn.nullToEmptyMap().apply(null).isEmpty());

        List<String> list = new ArrayList<>();
        assertSame(list, Fn.<String> nullToEmptyList().apply(list));
    }

    @Test
    public void nullToEmpty() {
        assertThat(Fn.nullToEmpty().apply(null)).isEmpty();
    }

    @Test
    public void testNullToEmpty() {
        UnaryOperator<String> nullToEmpty = Fn.nullToEmpty();
        assertEquals("", nullToEmpty.apply(null));
        assertEquals("hello", nullToEmpty.apply("hello"));
    }

    @Test
    public void nullToEmptyList() {
        assertThat(Fn.nullToEmptyList().apply(null)).isNotNull();
    }

    @Test
    public void testNullToEmptyList() {
        UnaryOperator<List<String>> nullToEmptyList = Fn.nullToEmptyList();
        assertTrue(nullToEmptyList.apply(null).isEmpty());
        List<String> list = Arrays.asList("a", "b");
        assertSame(list, nullToEmptyList.apply(list));
    }

    @Test
    public void nullToEmptySet() {
        assertThat(Fn.nullToEmptySet().apply(null)).isNotNull();
    }

    @Test
    public void testNullToEmptySet() {
        UnaryOperator<Set<String>> nullToEmptySet = Fn.nullToEmptySet();
        assertTrue(nullToEmptySet.apply(null).isEmpty());
        Set<String> set = new HashSet<>(Arrays.asList("a", "b"));
        assertSame(set, nullToEmptySet.apply(set));
    }

    @Test
    public void nullToEmptyMap() {
        assertThat(Fn.nullToEmptyMap().apply(null)).isNotNull();
    }

    @Test
    public void testNullToEmptyMap() {
        UnaryOperator<Map<String, Integer>> nullToEmptyMap = Fn.nullToEmptyMap();
        assertTrue(nullToEmptyMap.apply(null).isEmpty());
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertSame(map, nullToEmptyMap.apply(map));
    }

    @Test
    public void len() {
        assertThat(Fn.len().apply(new Integer[] { 1, 2 })).isEqualTo(2);
    }

    @Test
    public void testLenLengthSize() {
        assertEquals(3, Fn.<String> len().apply(new String[] { "a", "b", "c" }));
        assertEquals(0, Fn.<String> len().apply(null));

        assertEquals(3, Fn.<String> length().apply("abc"));
        assertEquals(0, Fn.<String> length().apply(""));

        assertEquals(2, Fn.<Collection<Integer>> size().apply(Arrays.asList(1, 2)));
        assertEquals(0, Fn.<Collection<Integer>> size().apply(null));

        assertEquals(1, Fn.<Map<String, Integer>> mapSize().apply(Collections.singletonMap("a", 1)));
        assertEquals(0, Fn.<Map<String, Integer>> mapSize().apply(null));
    }

    @Test
    public void testLen() {
        assertEquals(3, FI.len().apply(new int[] { 1, 2, 3 }));
        assertEquals(0, FI.len().apply(null));
    }

    @Test
    public void length() {
        assertThat(Fn.length().apply("abc")).isEqualTo(3);
    }

    @Test
    public void testLength() {
        Function<String, Integer> length = Fn.length();
        assertEquals(5, length.apply("hello"));
        assertEquals(0, length.apply(""));
    }

    @Test
    public void testOfEmptyString() {
        Supplier<String> supplier = Suppliers.ofEmptyString();
        Assertions.assertNotNull(supplier);

        String str = supplier.get();
        Assertions.assertNotNull(str);
        Assertions.assertEquals("", str);
        Assertions.assertEquals(0, str.length());

        Assertions.assertSame(str, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyString());
    }

    @Test
    public void testOfStringBuilder() {
        Supplier<StringBuilder> supplier = Suppliers.ofStringBuilder();
        Assertions.assertNotNull(supplier);

        StringBuilder sb1 = supplier.get();
        StringBuilder sb2 = supplier.get();

        Assertions.assertNotNull(sb1);
        Assertions.assertNotNull(sb2);
        Assertions.assertNotSame(sb1, sb2);
        Assertions.assertEquals(0, sb1.length());

        Assertions.assertSame(supplier, Suppliers.ofStringBuilder());
    }

    @Test
    public void testBiPredicateNot() {
        BiPredicate<String, Integer> lengthEq = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> notLengthEq = Fn.not(lengthEq);
        assertFalse(notLengthEq.test("hello", 5));
        assertTrue(notLengthEq.test("hello", 4));
        assertThrows(IllegalArgumentException.class, () -> Fn.not((java.util.function.BiPredicate<?, ?>) null));
    }

    @Test
    public void testCollectionFactories() {
        assertEquals(0, ((ArrayList<?>) IntFunctions.ofList().apply(10)).size());
        assertTrue(IntFunctions.ofSet().apply(10) instanceof HashSet);
        assertTrue(IntFunctions.ofMap().apply(10) instanceof HashMap);
    }

    @Test
    public void size() {
        assertThat(Fn.size().apply(java.util.Arrays.asList(1, 2))).isEqualTo(2);
    }

    @Test
    public void sizeM() {
        assertThat(Fn.mapSize().apply(java.util.Map.of("a", 1))).isEqualTo(1);
    }

    @Test
    public void testOfAddAlll() {
        IntList list1 = new IntList();
        list1.add(1);

        IntList list2 = new IntList();
        list2.add(2);
        list2.add(3);

        BiConsumer<IntList, IntList> consumer = Fn.BiConsumers.ofAddAlll();
        consumer.accept(list1, list2);

        assertEquals(3, list1.size());
        assertEquals(1, list1.get(0));
        assertEquals(2, list1.get(1));
        assertEquals(3, list1.get(2));
    }

    @Test
    public void testOfRemoveAlll() {
        IntList list1 = new IntList();
        list1.add(1);
        list1.add(2);
        list1.add(3);

        IntList list2 = new IntList();
        list2.add(2);

        BiConsumer<IntList, IntList> consumer = Fn.BiConsumers.ofRemoveAlll();
        consumer.accept(list1, list2);

        assertEquals(2, list1.size());
        assertEquals(1, list1.get(0));
        assertEquals(3, list1.get(1));
    }

    @Test
    public void testOfPutAll() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);
        map2.put("c", 3);

        BiConsumer<Map<String, Integer>, Map<String, Integer>> consumer = Fn.BiConsumers.ofPutAll();
        consumer.accept(map1, map2);

        assertEquals(3, map1.size());
        assertEquals(1, map1.get("a"));
        assertEquals(2, map1.get("b"));
        assertEquals(3, map1.get("c"));
    }

    @Test
    public void testOfRemoveByKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        BiConsumer<Map<String, Integer>, String> consumer = Fn.BiConsumers.ofRemoveByKey();
        consumer.accept(map, "a");

        assertEquals(1, map.size());
        assertFalse(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testToSet() {
        Function<Pair<String, String>, Set<String>> function = Fn.Pairs.toSet();

        Pair<String, String> pair1 = Pair.of("a", "b");
        Set<String> set1 = function.apply(pair1);
        assertEquals(2, set1.size());
        assertTrue(set1.contains("a"));
        assertTrue(set1.contains("b"));

        Pair<String, String> pair2 = Pair.of("x", "x");
        Set<String> set2 = function.apply(pair2);
        assertEquals(1, set2.size());
        assertTrue(set2.contains("x"));
    }

    // Triples.toList / toSet
    @Test
    public void testTriples_toList() {
        Triple<String, String, String> triple = Triple.of("a", "b", "c");
        List<String> list = Fn.Triples.<String> toList().apply(triple);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void testTriples_toSet() {
        Triple<String, String, String> triple = Triple.of("a", "b", "c");
        Set<String> set = Fn.Triples.<String> toSet().apply(triple);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testBiFunctions_ofAdd() {
        List<Integer> list = new ArrayList<>();
        BiFunctions.<Integer, List<Integer>> ofAdd().apply(list, 42);
        assertEquals(1, list.size());
        assertEquals(42, (int) list.get(0));
    }

    @Test
    public void testBiFunctions_ofAddAll() {
        List<Integer> list1 = new ArrayList<>(Arrays.asList(1, 2));
        List<Integer> list2 = new ArrayList<>(Arrays.asList(3, 4));
        BiFunctions.<Integer, List<Integer>> ofAddAll().apply(list1, list2);
        assertEquals(4, list1.size());
        assertTrue(list1.contains(3));
    }

    @Test
    public void testBiFunctions_ofPutAll() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        BiFunctions.<String, Integer, Map<String, Integer>> ofPutAll().apply(map1, map2);
        assertEquals(2, map1.size());
        assertEquals(1, (int) map1.get("a"));
    }

    @Test
    public void testOfBooleanList() {
        Supplier<BooleanList> supplier = Suppliers.ofBooleanList();
        Assertions.assertNotNull(supplier);

        BooleanList list1 = supplier.get();
        BooleanList list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofBooleanList());
    }

    @Test
    public void testOfCharList() {
        Supplier<CharList> supplier = Suppliers.ofCharList();
        Assertions.assertNotNull(supplier);

        CharList list1 = supplier.get();
        CharList list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofCharList());
    }

    @Test
    public void testOfByteList() {
        Supplier<ByteList> supplier = Suppliers.ofByteList();
        Assertions.assertNotNull(supplier);

        ByteList list1 = supplier.get();
        ByteList list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofByteList());
    }

    @Test
    public void testOfShortList() {
        Supplier<ShortList> supplier = Suppliers.ofShortList();
        Assertions.assertNotNull(supplier);

        ShortList list1 = supplier.get();
        ShortList list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofShortList());
    }

    @Test
    public void testOfIntList() {
        Supplier<IntList> supplier = Suppliers.ofIntList();
        Assertions.assertNotNull(supplier);

        IntList list1 = supplier.get();
        IntList list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofIntList());
    }

    @Test
    public void testOfLongList() {
        Supplier<LongList> supplier = Suppliers.ofLongList();
        Assertions.assertNotNull(supplier);

        LongList list1 = supplier.get();
        LongList list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofLongList());
    }

    @Test
    public void testOfFloatList() {
        Supplier<FloatList> supplier = Suppliers.ofFloatList();
        Assertions.assertNotNull(supplier);

        FloatList list1 = supplier.get();
        FloatList list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofFloatList());
    }

    @Test
    public void testOfDoubleList() {
        Supplier<DoubleList> supplier = Suppliers.ofDoubleList();
        Assertions.assertNotNull(supplier);

        DoubleList list1 = supplier.get();
        DoubleList list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofDoubleList());
    }

    @Test
    public void testOfList() {
        Supplier<List<String>> supplier = Suppliers.ofList();
        Assertions.assertNotNull(supplier);

        List<String> list1 = supplier.get();
        List<String> list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());
        Assertions.assertTrue(list1 instanceof ArrayList);

        Assertions.assertSame(supplier, Suppliers.ofList());
    }

    @Test
    public void testOfLinkedList() {
        Supplier<LinkedList<String>> supplier = Suppliers.ofLinkedList();
        Assertions.assertNotNull(supplier);

        LinkedList<String> list1 = supplier.get();
        LinkedList<String> list2 = supplier.get();

        Assertions.assertNotNull(list1);
        Assertions.assertNotNull(list2);
        Assertions.assertNotSame(list1, list2);
        Assertions.assertEquals(0, list1.size());

        Assertions.assertSame(supplier, Suppliers.ofLinkedList());
    }

    @Test
    public void testOfSet() {
        Supplier<Set<String>> supplier = Suppliers.ofSet();
        Assertions.assertNotNull(supplier);

        Set<String> set1 = supplier.get();
        Set<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());
        Assertions.assertTrue(set1 instanceof HashSet);

        Assertions.assertSame(supplier, Suppliers.ofSet());
    }

    @Test
    public void testOfLinkedHashSet() {
        Supplier<Set<String>> supplier = Suppliers.ofLinkedHashSet();
        Assertions.assertNotNull(supplier);

        Set<String> set1 = supplier.get();
        Set<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());
        Assertions.assertTrue(set1 instanceof LinkedHashSet);

        Assertions.assertSame(supplier, Suppliers.ofLinkedHashSet());
    }

    @Test
    public void testOfSortedSet() {
        Supplier<SortedSet<String>> supplier = Suppliers.ofSortedSet();
        Assertions.assertNotNull(supplier);

        SortedSet<String> set1 = supplier.get();
        SortedSet<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());
        Assertions.assertTrue(set1 instanceof TreeSet);

        Assertions.assertSame(supplier, Suppliers.ofSortedSet());
    }

    @Test
    public void testOfNavigableSet() {
        Supplier<NavigableSet<String>> supplier = Suppliers.ofNavigableSet();
        Assertions.assertNotNull(supplier);

        NavigableSet<String> set1 = supplier.get();
        NavigableSet<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());
        Assertions.assertTrue(set1 instanceof TreeSet);

        Assertions.assertSame(supplier, Suppliers.ofNavigableSet());
    }

    @Test
    public void testOfTreeSet() {
        Supplier<TreeSet<String>> supplier = Suppliers.ofTreeSet();
        Assertions.assertNotNull(supplier);

        TreeSet<String> set1 = supplier.get();
        TreeSet<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());

        Assertions.assertSame(supplier, Suppliers.ofTreeSet());
    }

    @Test
    public void testOfQueue() {
        Supplier<Queue<String>> supplier = Suppliers.ofQueue();
        Assertions.assertNotNull(supplier);

        Queue<String> queue1 = supplier.get();
        Queue<String> queue2 = supplier.get();

        Assertions.assertNotNull(queue1);
        Assertions.assertNotNull(queue2);
        Assertions.assertNotSame(queue1, queue2);
        Assertions.assertEquals(0, queue1.size());
        Assertions.assertTrue(queue1 instanceof LinkedList);

        Assertions.assertSame(supplier, Suppliers.ofQueue());
    }

    @Test
    public void testOfDeque() {
        Supplier<Deque<String>> supplier = Suppliers.ofDeque();
        Assertions.assertNotNull(supplier);

        Deque<String> deque1 = supplier.get();
        Deque<String> deque2 = supplier.get();

        Assertions.assertNotNull(deque1);
        Assertions.assertNotNull(deque2);
        Assertions.assertNotSame(deque1, deque2);
        Assertions.assertEquals(0, deque1.size());
        Assertions.assertTrue(deque1 instanceof LinkedList);

        Assertions.assertSame(supplier, Suppliers.ofDeque());
    }

    @Test
    public void testOfArrayDeque() {
        Supplier<ArrayDeque<String>> supplier = Suppliers.ofArrayDeque();
        Assertions.assertNotNull(supplier);

        ArrayDeque<String> deque1 = supplier.get();
        ArrayDeque<String> deque2 = supplier.get();

        Assertions.assertNotNull(deque1);
        Assertions.assertNotNull(deque2);
        Assertions.assertNotSame(deque1, deque2);
        Assertions.assertEquals(0, deque1.size());

        Assertions.assertSame(supplier, Suppliers.ofArrayDeque());
    }

    @Test
    public void testOfLinkedBlockingQueue() {
        Supplier<LinkedBlockingQueue<String>> supplier = Suppliers.ofLinkedBlockingQueue();
        Assertions.assertNotNull(supplier);

        LinkedBlockingQueue<String> queue1 = supplier.get();
        LinkedBlockingQueue<String> queue2 = supplier.get();

        Assertions.assertNotNull(queue1);
        Assertions.assertNotNull(queue2);
        Assertions.assertNotSame(queue1, queue2);
        Assertions.assertEquals(0, queue1.size());

        Assertions.assertSame(supplier, Suppliers.ofLinkedBlockingQueue());
    }

    @Test
    public void testOfLinkedBlockingDeque() {
        Supplier<LinkedBlockingDeque<String>> supplier = Suppliers.ofLinkedBlockingDeque();
        Assertions.assertNotNull(supplier);

        LinkedBlockingDeque<String> deque1 = supplier.get();
        LinkedBlockingDeque<String> deque2 = supplier.get();

        Assertions.assertNotNull(deque1);
        Assertions.assertNotNull(deque2);
        Assertions.assertNotSame(deque1, deque2);
        Assertions.assertEquals(0, deque1.size());

        Assertions.assertSame(supplier, Suppliers.ofLinkedBlockingDeque());
    }

    @Test
    public void testOfConcurrentLinkedQueue() {
        Supplier<ConcurrentLinkedQueue<String>> supplier = Suppliers.ofConcurrentLinkedQueue();
        Assertions.assertNotNull(supplier);

        ConcurrentLinkedQueue<String> queue1 = supplier.get();
        ConcurrentLinkedQueue<String> queue2 = supplier.get();

        Assertions.assertNotNull(queue1);
        Assertions.assertNotNull(queue2);
        Assertions.assertNotSame(queue1, queue2);
        Assertions.assertEquals(0, queue1.size());

        Assertions.assertSame(supplier, Suppliers.ofConcurrentLinkedQueue());
    }

    @Test
    public void testOfPriorityQueue() {
        Supplier<PriorityQueue<String>> supplier = Suppliers.ofPriorityQueue();
        Assertions.assertNotNull(supplier);

        PriorityQueue<String> queue1 = supplier.get();
        PriorityQueue<String> queue2 = supplier.get();

        Assertions.assertNotNull(queue1);
        Assertions.assertNotNull(queue2);
        Assertions.assertNotSame(queue1, queue2);
        Assertions.assertEquals(0, queue1.size());

        Assertions.assertSame(supplier, Suppliers.ofPriorityQueue());
    }

    @Test
    public void testOfMap() {
        Supplier<Map<String, Integer>> supplier = Suppliers.ofMap();
        Assertions.assertNotNull(supplier);

        Map<String, Integer> map1 = supplier.get();
        Map<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());
        Assertions.assertTrue(map1 instanceof HashMap);

        Assertions.assertSame(supplier, Suppliers.ofMap());
    }

    @Test
    public void testOfLinkedHashMap() {
        Supplier<Map<String, Integer>> supplier = Suppliers.ofLinkedHashMap();
        Assertions.assertNotNull(supplier);

        Map<String, Integer> map1 = supplier.get();
        Map<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());
        Assertions.assertTrue(map1 instanceof LinkedHashMap);

        Assertions.assertSame(supplier, Suppliers.ofLinkedHashMap());
    }

    @Test
    public void testOfIdentityHashMap() {
        Supplier<IdentityHashMap<String, Integer>> supplier = Suppliers.ofIdentityHashMap();
        Assertions.assertNotNull(supplier);

        IdentityHashMap<String, Integer> map1 = supplier.get();
        IdentityHashMap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());

        Assertions.assertSame(supplier, Suppliers.ofIdentityHashMap());
    }

    @Test
    public void testOfSortedMap() {
        Supplier<SortedMap<String, Integer>> supplier = Suppliers.ofSortedMap();
        Assertions.assertNotNull(supplier);

        SortedMap<String, Integer> map1 = supplier.get();
        SortedMap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());
        Assertions.assertTrue(map1 instanceof TreeMap);

        Assertions.assertSame(supplier, Suppliers.ofSortedMap());
    }

    @Test
    public void testOfNavigableMap() {
        Supplier<NavigableMap<String, Integer>> supplier = Suppliers.ofNavigableMap();
        Assertions.assertNotNull(supplier);

        NavigableMap<String, Integer> map1 = supplier.get();
        NavigableMap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());
        Assertions.assertTrue(map1 instanceof TreeMap);

        Assertions.assertSame(supplier, Suppliers.ofNavigableMap());
    }

    @Test
    public void testOfTreeMap() {
        Supplier<TreeMap<String, Integer>> supplier = Suppliers.ofTreeMap();
        Assertions.assertNotNull(supplier);

        TreeMap<String, Integer> map1 = supplier.get();
        TreeMap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());

        Assertions.assertSame(supplier, Suppliers.ofTreeMap());
    }

    @Test
    public void testOfConcurrentMap() {
        Supplier<ConcurrentMap<String, Integer>> supplier = Suppliers.ofConcurrentMap();
        Assertions.assertNotNull(supplier);

        ConcurrentMap<String, Integer> map1 = supplier.get();
        ConcurrentMap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());
        Assertions.assertTrue(map1 instanceof ConcurrentHashMap);

        Assertions.assertSame(supplier, Suppliers.ofConcurrentMap());
    }

    @Test
    public void testOfConcurrentHashMap() {
        Supplier<ConcurrentHashMap<String, Integer>> supplier = Suppliers.ofConcurrentHashMap();
        Assertions.assertNotNull(supplier);

        ConcurrentHashMap<String, Integer> map1 = supplier.get();
        ConcurrentHashMap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());

        Assertions.assertSame(supplier, Suppliers.ofConcurrentHashMap());
    }

    @Test
    public void testOfConcurrentHashSet() {
        Supplier<Set<String>> supplier = Suppliers.ofConcurrentHashSet();
        Assertions.assertNotNull(supplier);

        Set<String> set1 = supplier.get();
        Set<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());

        Assertions.assertSame(supplier, Suppliers.ofConcurrentHashSet());
    }

    @Test
    public void testOfBiMap() {
        Supplier<BiMap<String, Integer>> supplier = Suppliers.ofBiMap();
        Assertions.assertNotNull(supplier);

        BiMap<String, Integer> map1 = supplier.get();
        BiMap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.size());

        Assertions.assertSame(supplier, Suppliers.ofBiMap());
    }

    @Test
    public void testOfMultiset() {
        Supplier<Multiset<String>> supplier = Suppliers.ofMultiset();
        Assertions.assertNotNull(supplier);

        Multiset<String> set1 = supplier.get();
        Multiset<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());

        Assertions.assertSame(supplier, Suppliers.ofMultiset());
    }

    @Test
    public void testOfMultisetWithMapType() {
        Supplier<Multiset<String>> supplier = Suppliers.ofMultiset(LinkedHashMap.class);
        Assertions.assertNotNull(supplier);

        Multiset<String> set1 = supplier.get();
        Multiset<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());
    }

    @Test
    public void testOfMultisetWithMapSupplier() {
        java.util.function.Supplier<Map<String, ?>> mapSupplier = LinkedHashMap::new;
        Supplier<Multiset<String>> supplier = Suppliers.ofMultiset(mapSupplier);
        Assertions.assertNotNull(supplier);

        Multiset<String> set1 = supplier.get();
        Multiset<String> set2 = supplier.get();

        Assertions.assertNotNull(set1);
        Assertions.assertNotNull(set2);
        Assertions.assertNotSame(set1, set2);
        Assertions.assertEquals(0, set1.size());
    }

    @Test
    public void testOfArrayBlockingQueue() {
        IntFunction<ArrayBlockingQueue<String>> func = IntFunctions.ofArrayBlockingQueue();
        Assertions.assertNotNull(func);

        ArrayBlockingQueue<String> queue1 = func.apply(5);
        ArrayBlockingQueue<String> queue2 = func.apply(10);

        Assertions.assertNotNull(queue1);
        Assertions.assertNotNull(queue2);
        Assertions.assertNotSame(queue1, queue2);
        Assertions.assertEquals(0, queue1.size());
        Assertions.assertEquals(5, queue1.remainingCapacity());
        Assertions.assertEquals(10, queue2.remainingCapacity());

        Assertions.assertSame(func, IntFunctions.ofArrayBlockingQueue());
    }

    @Test
    public void testSize() {
        Function<List<String>, Integer> size = Fn.size();
        assertEquals(3, size.apply(Arrays.asList("a", "b", "c")));
        assertEquals(0, size.apply(Collections.emptyList()));
    }

    @Test
    public void testSizeM() {
        Function<Map<String, Integer>, Integer> sizeM = Fn.mapSize();
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, sizeM.apply(map));
        assertEquals(0, sizeM.apply(Collections.emptyMap()));
    }

    @Test
    public void testOfPutAll_Deprecated() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);
        map2.put("c", 3);

        BinaryOperator<Map<String, Integer>> operator = Fn.BinaryOperators.ofPutAll();
        Map<String, Integer> result = operator.apply(map1, map2);

        assertSame(map1, result);
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
    }

    @Test
    public void testOfPutAllToFirst() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);
        map2.put("c", 3);

        BinaryOperator<Map<String, Integer>> operator = Fn.BinaryOperators.ofPutAllToFirst();
        Map<String, Integer> result = operator.apply(map1, map2);

        assertSame(map1, result);
        assertEquals(3, result.size());
    }

    @Test
    public void testOfPutAllToBigger() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("d", 4);
        map2.put("e", 5);

        BinaryOperator<Map<String, Integer>> operator = Fn.BinaryOperators.ofPutAllToBigger();

        Map<String, Integer> result1 = operator.apply(map1, map2);
        assertSame(map1, result1);
        assertEquals(5, result1.size());

        map1 = new HashMap<>();
        map1.put("a", 1);

        map2 = new HashMap<>();
        map2.put("b", 2);
        map2.put("c", 3);

        Map<String, Integer> result2 = operator.apply(map1, map2);
        assertSame(map2, result2);
        assertEquals(3, result2.size());
    }

    // ==================== Tests for gap analysis coverage ====================

    @Test
    public void testMapSize() {
        Function<Map, Integer> mapSizeFunc = Fn.mapSize();
        Map<String, Integer> map = new HashMap<>();
        assertEquals(0, (int) mapSizeFunc.apply(map));
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, (int) mapSizeFunc.apply(map));
        assertEquals(0, (int) mapSizeFunc.apply(Collections.emptyMap()));
    }

    @Test
    public void cast() {
        assertThat(Fn.cast(String.class).apply("a")).isInstanceOf(String.class);
    }

    @Test
    public void testCast() {
        Object obj = "string";
        String s = Fn.cast(String.class).apply(obj);
        assertEquals("string", s);
        assertThrows(ClassCastException.class, () -> {
            Object o = 123;
            Fn.cast(String.class).apply(o);
        });
    }

    @Test
    public void alwaysTrue() {
        assertThat(Fn.alwaysTrue().test(1)).isTrue();
    }

    @Test
    public void testBooleanPredicates() {
        assertTrue(Fn.alwaysTrue().test(null));
        assertFalse(Fn.alwaysFalse().test("any"));
    }

    @Test
    public void testAlways() {
        assertTrue(BiPredicates.alwaysTrue().test(null, null));
        assertFalse(BiPredicates.alwaysFalse().test("a", "b"));
    }

    @Test
    public void testAlwaysTrue() {
        Throwables.Predicate<Object, RuntimeException> predicate = Fnn.alwaysTrue();
        Assertions.assertTrue(predicate.test("anything"));
        Assertions.assertTrue(predicate.test(null));
        Assertions.assertTrue(predicate.test(123));
    }

    @Test
    public void alwaysFalse() {
        assertThat(Fn.alwaysFalse().test(1)).isFalse();
    }

    @Test
    public void testAlwaysFalse() {
        Throwables.Predicate<Object, RuntimeException> predicate = Fnn.alwaysFalse();
        Assertions.assertFalse(predicate.test("anything"));
        Assertions.assertFalse(predicate.test(null));
        Assertions.assertFalse(predicate.test(123));
    }

    @Test
    public void testNullEmptyBlankPredicates() {
        assertTrue(Fn.isNull().test(null));
        assertFalse(Fn.isNull().test(""));
        assertTrue(Fn.<String> isNull(Strings::trimToNull).test(" "));
        assertFalse(Fn.isNull(s -> s).test("a"));

        assertTrue(Fn.isEmpty().test(""));
        assertFalse(Fn.isEmpty().test(" "));
        assertTrue(Fn.isEmpty(s -> (String) s).test(""));
        assertFalse(Fn.isEmpty(s -> (String) s).test("a"));

        assertTrue(Fn.isBlank().test(" "));
        assertFalse(Fn.isBlank().test("a"));
        assertTrue(Fn.isBlank(s -> (String) s).test(" "));
        assertFalse(Fn.isBlank(s -> (String) s).test("a"));

        assertTrue(Fn.isEmptyArray().test(new String[0]));
        assertTrue(Fn.isEmptyCollection().test(new ArrayList<>()));
        assertTrue(Fn.isEmptyMap().test(new HashMap<>()));

        assertFalse(Fn.notNull().test(null));
        assertTrue(Fn.notNull().test(""));
        assertTrue(Fn.notNull(s -> s).test("a"));
        assertFalse(Fn.<String> notNull(Strings::trimToNull).test(" "));

        assertTrue(Fn.notEmpty().test(" "));
        assertFalse(Fn.notEmpty().test(""));
        assertTrue(Fn.notEmpty(s -> (String) s).test("a"));
        assertFalse(Fn.notEmpty(s -> (String) s).test(""));

        assertTrue(Fn.notBlank().test("a"));
        assertFalse(Fn.notBlank().test(" "));
        assertTrue(Fn.notBlank(s -> (String) s).test("a"));
        assertFalse(Fn.notBlank(s -> (String) s).test(" "));

        assertTrue(Fn.notEmptyArray().test(new String[1]));
        assertTrue(Fn.notEmptyCollection().test(Arrays.asList(1)));
        assertTrue(Fn.notEmptyMap().test(Collections.singletonMap("a", 1)));
    }

    @Test
    public void isNull() {
        assertThat(Fn.isNull().test(null)).isTrue();
    }

    @Test
    public void testIsNullWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.isNull(Map.Entry::getValue);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", null)));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testIsNullWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueIsNull = Fn.isNull(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", null);
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");

        assertTrue(valueIsNull.test(entry1));
        assertFalse(valueIsNull.test(entry2));
    }

    @Test
    public void testIsNull() {
        Throwables.Predicate<Object, RuntimeException> isNull = Fnn.isNull();
        Assertions.assertTrue(isNull.test(null));
        Assertions.assertFalse(isNull.test("not null"));
        Assertions.assertFalse(isNull.test(123));
    }

    @Test
    public void isEmpty() {
        assertThat(Fn.isEmpty().test("")).isTrue();
    }

    @Test
    public void testIsEmptyWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.isEmpty(Map.Entry::getValue);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "")));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testIsEmptyWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueIsEmpty = Fn.isEmpty(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", "");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");
        Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>("key", null);

        assertTrue(valueIsEmpty.test(entry1));
        assertFalse(valueIsEmpty.test(entry2));
        assertTrue(valueIsEmpty.test(entry3));
    }

    @Test
    public void testIsEmpty() {
        Throwables.Predicate<CharSequence, RuntimeException> isEmpty = Fnn.isEmpty();
        Assertions.assertTrue(isEmpty.test(""));
        Assertions.assertFalse(isEmpty.test("not empty"));
        Assertions.assertFalse(isEmpty.test(" "));
    }

    @Test
    public void isBlank() {
        assertThat(Fn.isBlank().test(" ")).isTrue();
    }

    @Test
    public void testIsBlankWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.isBlank(Map.Entry::getValue);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "   ")));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testIsBlankWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueIsBlank = Fn.isBlank(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", "   ");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");
        Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>("key", null);

        assertTrue(valueIsBlank.test(entry1));
        assertFalse(valueIsBlank.test(entry2));
        assertTrue(valueIsBlank.test(entry3));
    }

    @Test
    public void testIsBlank() {
        Throwables.Predicate<CharSequence, RuntimeException> isBlank = Fnn.isBlank();
        Assertions.assertTrue(isBlank.test(""));
        Assertions.assertTrue(isBlank.test(" "));
        Assertions.assertTrue(isBlank.test("\t\n\r"));
        Assertions.assertFalse(isBlank.test("not blank"));
        Assertions.assertFalse(isBlank.test(" a "));
    }

    @Test
    public void testIsEmptyArray() {
        Throwables.Predicate<String[], RuntimeException> isEmptyArray = Fnn.isEmptyArray();
        Assertions.assertTrue(isEmptyArray.test(null));
        Assertions.assertTrue(isEmptyArray.test(new String[0]));
        Assertions.assertFalse(isEmptyArray.test(new String[] { "a" }));
    }

    @Test
    public void testIsEmptyCollection() {
        Throwables.Predicate<java.util.List<String>, RuntimeException> isEmptyCollection = Fnn.isEmptyCollection();
        Assertions.assertTrue(isEmptyCollection.test(null));
        Assertions.assertTrue(isEmptyCollection.test(new java.util.ArrayList<>()));
        Assertions.assertFalse(isEmptyCollection.test(java.util.Arrays.asList("a")));
    }

    @Test
    public void testIsEmptyMap() {
        Throwables.Predicate<Map<String, Integer>, RuntimeException> isEmptyMap = Fnn.isEmptyMap();
        Assertions.assertTrue(isEmptyMap.test(null));
        Assertions.assertTrue(isEmptyMap.test(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertFalse(isEmptyMap.test(map));
    }

    @Test
    public void notNull() {
        assertThat(Fn.notNull().test(1)).isTrue();
    }

    @Test
    public void testNotNullWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.notNull(Map.Entry::getValue);
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", null)));
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testNotNullWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueNotNull = Fn.notNull(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", null);
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");

        assertFalse(valueNotNull.test(entry1));
        assertTrue(valueNotNull.test(entry2));
    }

    @Test
    public void testNotNull() {
        Throwables.Predicate<Object, RuntimeException> notNull = Fnn.notNull();
        Assertions.assertFalse(notNull.test(null));
        Assertions.assertTrue(notNull.test("not null"));
        Assertions.assertTrue(notNull.test(123));
    }

    @Test
    public void notEmpty() {
        assertThat(Fn.notEmpty().test("a")).isTrue();
    }

    @Test
    public void testNotEmptyWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.notEmpty(Map.Entry::getValue);
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "")));
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testNotEmptyWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueNotEmpty = Fn.notEmpty(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", "");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");
        Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>("key", null);

        assertFalse(valueNotEmpty.test(entry1));
        assertTrue(valueNotEmpty.test(entry2));
        assertFalse(valueNotEmpty.test(entry3));
    }

    @Test
    public void testNotEmpty() {
        Throwables.Predicate<CharSequence, RuntimeException> notEmpty = Fnn.notEmpty();
        Assertions.assertFalse(notEmpty.test(""));
        Assertions.assertTrue(notEmpty.test("not empty"));
        Assertions.assertTrue(notEmpty.test(" "));
    }

    @Test
    public void testNotEmptyA() {
        Throwables.Predicate<String[], RuntimeException> notEmptyArray = Fnn.notEmptyArray();
        Assertions.assertFalse(notEmptyArray.test(null));
        Assertions.assertFalse(notEmptyArray.test(new String[0]));
        Assertions.assertTrue(notEmptyArray.test(new String[] { "a" }));
    }

    @Test
    public void testNotEmptyC() {
        Throwables.Predicate<java.util.List<String>, RuntimeException> notEmptyCollection = Fnn.notEmptyCollection();
        Assertions.assertFalse(notEmptyCollection.test(null));
        Assertions.assertFalse(notEmptyCollection.test(new java.util.ArrayList<>()));
        Assertions.assertTrue(notEmptyCollection.test(java.util.Arrays.asList("a")));
    }

    @Test
    public void testNotEmptyM() {
        Throwables.Predicate<Map<String, Integer>, RuntimeException> notEmptyMap = Fnn.notEmptyMap();
        Assertions.assertFalse(notEmptyMap.test(null));
        Assertions.assertFalse(notEmptyMap.test(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertTrue(notEmptyMap.test(map));
    }

    @Test
    public void notBlank() {
        assertThat(Fn.notBlank().test("a")).isTrue();
    }

    @Test
    public void testNotBlankWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.notBlank(Map.Entry::getValue);
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "   ")));
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testNotBlankWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueNotBlank = Fn.notBlank(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", "   ");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");
        Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>("key", null);

        assertFalse(valueNotBlank.test(entry1));
        assertTrue(valueNotBlank.test(entry2));
        assertFalse(valueNotBlank.test(entry3));
    }

    @Test
    public void testNotBlank() {
        Throwables.Predicate<CharSequence, RuntimeException> notBlank = Fnn.notBlank();
        Assertions.assertFalse(notBlank.test(""));
        Assertions.assertFalse(notBlank.test(" "));
        Assertions.assertFalse(notBlank.test("\t\n\r"));
        Assertions.assertTrue(notBlank.test("not blank"));
        Assertions.assertTrue(notBlank.test(" a "));
    }

    @Test
    public void testNotEmptyArray() {
        Predicate<String[]> pred = Fn.notEmptyArray();
        assertFalse(pred.test(null));
        assertFalse(pred.test(new String[0]));
        assertTrue(pred.test(new String[] { "a" }));
        assertTrue(pred.test(new String[] { "a", "b" }));
    }

    @Test
    public void testNotEmptyCollection() {
        Predicate<List> pred = Fn.notEmptyCollection();
        assertFalse(pred.test(null));
        assertFalse(pred.test(new ArrayList<>()));
        assertTrue(pred.test(Arrays.asList("a")));
        assertTrue(pred.test(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testNotEmptyMap() {
        Predicate<Map> pred = Fn.notEmptyMap();
        assertFalse(pred.test(null));
        assertFalse(pred.test(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 1);
        assertTrue(pred.test(map));
    }

    @Test
    public void isFile() {
        java.io.File file = mock(java.io.File.class);
        when(file.isFile()).thenReturn(true);
        assertThat(Fn.isFile().test(file)).isTrue();
    }

    @Test
    public void testFilePredicates(@TempDir Path tempDir) throws IOException {
        File file = File.createTempFile("test", ".txt", tempDir.toFile());
        File dir = tempDir.resolve("subdir").toFile();
        dir.mkdir();

        assertTrue(Fn.isFile().test(file));
        assertFalse(Fn.isFile().test(dir));
        assertFalse(Fn.isFile().test(null));

        assertTrue(Fn.isDirectory().test(dir));
        assertFalse(Fn.isDirectory().test(file));
        assertFalse(Fn.isDirectory().test(null));

        file.delete();
        dir.delete();
    }

    @Test
    public void testIsFile() throws IOException {
        Predicate<File> pred = Fn.isFile();
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        assertTrue(pred.test(tempFile));
        assertFalse(pred.test(new File(System.getProperty("java.io.tmpdir"))));
    }

    @Test
    public void isDirectory() {
        java.io.File file = mock(java.io.File.class);
        when(file.isDirectory()).thenReturn(true);
        assertThat(Fn.isDirectory().test(file)).isTrue();
    }

    @Test
    public void testIsDirectory() {
        Predicate<File> pred = Fn.isDirectory();
        assertTrue(pred.test(new File(System.getProperty("java.io.tmpdir"))));
        assertFalse(pred.test(new File("nonexistent.txt")));
    }

    @Test
    public void testBiPredicateFactories() {
        assertTrue(Fn.equal().test("a", "a"));
        assertFalse(Fn.notEqual().test("a", "a"));
        assertTrue(Fn.<Integer> greaterThan().test(10, 5));
        assertTrue(Fn.<Integer> greaterThanOrEqual().test(5, 5));
        assertTrue(Fn.<Integer> lessThan().test(5, 10));
        assertTrue(Fn.<Integer> lessThanOrEqual().test(5, 5));
    }

    @Test
    public void testEqual() {
        assertTrue(FI.equal().test(1, 1));
        assertFalse(FI.equal().test(1, 2));
    }

    @Test
    public void equal() {
        assertThat(Fn.equal(1).test(1)).isTrue();
    }

    @Test
    public void equalBiPredicate() {
        assertThat(Fn.equal().test(1, 1)).isTrue();
    }

    @Test
    public void testBinaryPredicates() {
        assertTrue(Fn.FB.equal().test((byte) 5, (byte) 5));
        assertFalse(Fn.FB.equal().test((byte) 5, (byte) 6));

        assertTrue(Fn.FB.notEqual().test((byte) 5, (byte) 6));
        assertFalse(Fn.FB.notEqual().test((byte) 5, (byte) 5));

        assertTrue(Fn.FB.greaterThan().test((byte) 6, (byte) 5));
        assertFalse(Fn.FB.greaterThan().test((byte) 5, (byte) 5));

        assertTrue(Fn.FB.greaterThanOrEqual().test((byte) 5, (byte) 5));
        assertTrue(Fn.FB.greaterThanOrEqual().test((byte) 6, (byte) 5));

        assertTrue(Fn.FB.lessThan().test((byte) 5, (byte) 6));
        assertFalse(Fn.FB.lessThan().test((byte) 5, (byte) 5));

        assertTrue(Fn.FB.lessThanOrEqual().test((byte) 5, (byte) 5));
        assertTrue(Fn.FB.lessThanOrEqual().test((byte) 5, (byte) 6));
    }

    @Test
    public void testEqualityPredicates() {
        assertTrue(Fn.equal("a").test("a"));
        assertFalse(Fn.equal("a").test("b"));
        assertTrue(Fn.equal(null).test(null));

        assertTrue(Fn.eqOr("a", "b").test("a"));
        assertTrue(Fn.eqOr("a", "b").test("b"));
        assertFalse(Fn.eqOr("a", "b").test("c"));

        assertTrue(Fn.eqOr("a", "b", "c").test("c"));
        assertFalse(Fn.eqOr("a", "b", "c").test("d"));

        assertTrue(Fn.notEqual("a").test("b"));
        assertFalse(Fn.notEqual("a").test("a"));
    }

    @Test
    public void testEqualBiPredicate() {
        BiPredicate<String, String> pred = Fn.equal();
        assertTrue(pred.test("test", "test"));
        assertFalse(pred.test("test", "other"));
        assertTrue(pred.test(null, null));
    }

    @Test
    public void testBiPredicateEqual() {
        BiPredicate<String, String> equal = Fn.equal();

        assertTrue(equal.test("hello", "hello"));
        assertFalse(equal.test("hello", "world"));
        assertTrue(equal.test(null, null));
        assertFalse(equal.test(null, "hello"));
    }

    @Test
    public void testEqOr() {
        Predicate<String> pred2 = Fn.eqOr("a", "b");
        assertTrue(pred2.test("a"));
        assertTrue(pred2.test("b"));
        assertFalse(pred2.test("c"));

        Predicate<String> pred3 = Fn.eqOr("a", "b", "c");
        assertTrue(pred3.test("a"));
        assertTrue(pred3.test("b"));
        assertTrue(pred3.test("c"));
        assertFalse(pred3.test("d"));
    }

    @Test
    public void testEqOr2() {
        Predicate<String> eqOr = Fn.eqOr("test1", "test2");
        assertTrue(eqOr.test("test1"));
        assertTrue(eqOr.test("test2"));
        assertFalse(eqOr.test("test3"));
        assertFalse(eqOr.test(null));
    }

    @Test
    public void testEqOr3() {
        Predicate<String> eqOr = Fn.eqOr("test1", "test2", "test3");
        assertTrue(eqOr.test("test1"));
        assertTrue(eqOr.test("test2"));
        assertTrue(eqOr.test("test3"));
        assertFalse(eqOr.test("test4"));
        assertFalse(eqOr.test(null));
    }

    @Test
    public void notEqual() {
        assertThat(Fn.notEqual(1).test(2)).isTrue();
    }

    @Test
    public void notEqualBiPredicate() {
        assertThat(Fn.notEqual().test(1, 2)).isTrue();
    }

    @Test
    public void testNotEqual() {
        CharBiPredicate predicate = Fn.FC.notEqual();
        Assertions.assertTrue(predicate.test('a', 'b'));
        Assertions.assertTrue(predicate.test('A', 'a'));
        Assertions.assertFalse(predicate.test('a', 'a'));
        Assertions.assertFalse(predicate.test('\0', '\0'));
    }

    @Test
    public void testNotEqualBiPredicate() {
        BiPredicate<String, String> pred = Fn.notEqual();
        assertFalse(pred.test("test", "test"));
        assertTrue(pred.test("test", "other"));
        assertFalse(pred.test(null, null));
    }

    @Test
    public void testBiPredicateNotEqual() {
        BiPredicate<String, String> notEqual = Fn.notEqual();

        assertFalse(notEqual.test("hello", "hello"));
        assertTrue(notEqual.test("hello", "world"));
        assertFalse(notEqual.test(null, null));
        assertTrue(notEqual.test(null, "hello"));
    }

    @Test
    public void testComparisonPredicates() {
        assertTrue(Fn.greaterThan(5).test(10));
        assertFalse(Fn.greaterThan(5).test(5));

        assertTrue(Fn.greaterThanOrEqual(5).test(5));
        assertFalse(Fn.greaterThanOrEqual(5).test(4));

        assertTrue(Fn.lessThan(5).test(0));
        assertFalse(Fn.lessThan(5).test(5));

        assertTrue(Fn.lessThanOrEqual(5).test(5));
        assertFalse(Fn.lessThanOrEqual(5).test(6));
    }

    @Test
    public void greaterThan() {
        assertThat(Fn.greaterThan(1).test(2)).isTrue();
    }

    @Test
    public void greaterThanBiPredicate() {
        assertThat(Fn.<Integer> greaterThan().test(2, 1)).isTrue();
    }

    @Test
    public void testGreaterThan() {
        CharBiPredicate predicate = Fn.FC.greaterThan();
        Assertions.assertTrue(predicate.test('b', 'a'));
        Assertions.assertTrue(predicate.test('z', 'a'));
        Assertions.assertFalse(predicate.test('a', 'a'));
        Assertions.assertFalse(predicate.test('a', 'b'));
    }

    @Test
    public void testGreaterThanBiPredicate() {
        BiPredicate<Integer, Integer> pred = Fn.greaterThan();
        assertTrue(pred.test(5, 3));
        assertFalse(pred.test(3, 5));
        assertFalse(pred.test(3, 3));
    }

    @Test
    public void testBiPredicateGreaterThan() {
        BiPredicate<Integer, Integer> greaterThan = Fn.greaterThan();

        assertTrue(greaterThan.test(5, 3));
        assertFalse(greaterThan.test(3, 5));
        assertFalse(greaterThan.test(5, 5));
    }

    @Test
    public void greaterThanOrEqual() {
        assertThat(Fn.greaterThanOrEqual(1).test(1)).isTrue();
    }

    @Test
    public void greaterThanOrEqualBiPredicate() {
        assertThat(Fn.<Integer> greaterThanOrEqual().test(1, 1)).isTrue();
    }

    @Test
    public void testGreaterThanOrEqual() {
        CharBiPredicate predicate = Fn.FC.greaterThanOrEqual();
        Assertions.assertTrue(predicate.test('b', 'a'));
        Assertions.assertTrue(predicate.test('a', 'a'));
        Assertions.assertFalse(predicate.test('a', 'b'));
    }

    @Test
    public void testGreaterThanOrEqualBiPredicate() {
        BiPredicate<Integer, Integer> pred = Fn.greaterThanOrEqual();
        assertTrue(pred.test(5, 3));
        assertFalse(pred.test(3, 5));
        assertTrue(pred.test(3, 3));
    }

    @Test
    public void testBiPredicateGreaterThanOrEqual() {
        BiPredicate<Integer, Integer> greaterThanOrEqual = Fn.greaterThanOrEqual();

        assertTrue(greaterThanOrEqual.test(5, 3));
        assertFalse(greaterThanOrEqual.test(3, 5));
        assertTrue(greaterThanOrEqual.test(5, 5));
    }

    @Test
    public void lessThan() {
        assertThat(Fn.lessThan(2).test(1)).isTrue();
    }

    @Test
    public void lessThanBiPredicate() {
        assertThat(Fn.<Integer> lessThan().test(1, 2)).isTrue();
    }

    @Test
    public void testLessThan() {
        CharBiPredicate predicate = Fn.FC.lessThan();
        Assertions.assertTrue(predicate.test('a', 'b'));
        Assertions.assertTrue(predicate.test('a', 'z'));
        Assertions.assertFalse(predicate.test('a', 'a'));
        Assertions.assertFalse(predicate.test('b', 'a'));
    }

    @Test
    public void testLessThanBiPredicate() {
        BiPredicate<Integer, Integer> pred = Fn.lessThan();
        assertTrue(pred.test(3, 5));
        assertFalse(pred.test(5, 3));
        assertFalse(pred.test(3, 3));
    }

    @Test
    public void testBiPredicateLessThan() {
        BiPredicate<Integer, Integer> lessThan = Fn.lessThan();

        assertTrue(lessThan.test(3, 5));
        assertFalse(lessThan.test(5, 3));
        assertFalse(lessThan.test(5, 5));
    }

    @Test
    public void lessThanOrEqual() {
        assertThat(Fn.lessThanOrEqual(1).test(1)).isTrue();
    }

    @Test
    public void lessThanOrEqualBiPredicate() {
        assertThat(Fn.<Integer> lessThanOrEqual().test(1, 1)).isTrue();
    }

    @Test
    public void testLessThanOrEqual() {
        CharBiPredicate predicate = Fn.FC.lessThanOrEqual();
        Assertions.assertTrue(predicate.test('a', 'b'));
        Assertions.assertTrue(predicate.test('a', 'a'));
        Assertions.assertFalse(predicate.test('b', 'a'));
    }

    @Test
    public void testLessThanOrEqualBiPredicate() {
        BiPredicate<Integer, Integer> pred = Fn.lessThanOrEqual();
        assertTrue(pred.test(3, 5));
        assertFalse(pred.test(5, 3));
        assertTrue(pred.test(3, 3));
    }

    @Test
    public void testBiPredicateLessThanOrEqual() {
        BiPredicate<Integer, Integer> lessThanOrEqual = Fn.lessThanOrEqual();

        assertTrue(lessThanOrEqual.test(3, 5));
        assertFalse(lessThanOrEqual.test(5, 3));
        assertTrue(lessThanOrEqual.test(5, 5));
    }

    @Test
    public void testRangePredicates() {
        assertTrue(Fn.gtAndLt(5, 10).test(7));
        assertFalse(Fn.gtAndLt(5, 10).test(5));
        assertFalse(Fn.gtAndLt(5, 10).test(10));

        assertTrue(Fn.geAndLt(5, 10).test(5));
        assertFalse(Fn.geAndLt(5, 10).test(10));

        assertTrue(Fn.geAndLe(5, 10).test(5));
        assertTrue(Fn.geAndLe(5, 10).test(10));
        assertFalse(Fn.geAndLe(5, 10).test(11));

        assertTrue(Fn.gtAndLe(5, 10).test(10));
        assertFalse(Fn.gtAndLe(5, 10).test(5));

        assertTrue(Fn.between(5, 10).test(7));
    }

    @Test
    public void testGtAndLt() {
        Predicate<Integer> pred = Fn.gtAndLt(1, 5);
        assertTrue(pred.test(2));
        assertTrue(pred.test(3));
        assertTrue(pred.test(4));
        assertFalse(pred.test(1));
        assertFalse(pred.test(5));
    }

    @Test
    public void testGeAndLt() {
        Predicate<Integer> pred = Fn.geAndLt(1, 5);
        assertTrue(pred.test(1));
        assertTrue(pred.test(2));
        assertTrue(pred.test(4));
        assertFalse(pred.test(0));
        assertFalse(pred.test(5));
    }

    @Test
    public void testGeAndLe() {
        Predicate<Integer> pred = Fn.geAndLe(1, 5);
        assertTrue(pred.test(1));
        assertTrue(pred.test(3));
        assertTrue(pred.test(5));
        assertFalse(pred.test(0));
        assertFalse(pred.test(6));
    }

    @Test
    public void testGtAndLe() {
        Predicate<Integer> pred = Fn.gtAndLe(1, 5);
        assertTrue(pred.test(2));
        assertTrue(pred.test(3));
        assertTrue(pred.test(5));
        assertFalse(pred.test(1));
        assertFalse(pred.test(6));
    }

    @Test
    public void between() {
        assertThat(Fn.between(1, 3).test(2)).isTrue();
    }

    @Test
    public void testBetween() {
        Predicate<Integer> pred = Fn.between(1, 5);
        assertTrue(pred.test(2));
        assertTrue(pred.test(3));
        assertTrue(pred.test(4));
        assertFalse(pred.test(1));
        assertFalse(pred.test(5));
    }

    @Test
    public void testMembershipPredicates() {
        Collection<String> coll = Arrays.asList("a", "b");
        assertTrue(Fn.in(coll).test("a"));
        assertFalse(Fn.in(coll).test("c"));

        assertTrue(Fn.notIn(coll).test("c"));
        assertFalse(Fn.notIn(coll).test("a"));
    }

    @Test
    public void in() {
        assertThat(Fn.in(java.util.Arrays.asList(1, 2, 3)).test(2)).isTrue();
    }

    @Test
    public void testIn() {
        List<String> list = Arrays.asList("a", "b", "c");
        Predicate<String> pred = Fn.in(list);
        assertTrue(pred.test("a"));
        assertTrue(pred.test("b"));
        assertFalse(pred.test("d"));

        Predicate<String> emptyPred = Fn.in(Collections.emptyList());
        assertFalse(emptyPred.test("a"));
    }

    @Test
    public void testPredicatesWithNullCollections() {
        assertThrows(IllegalArgumentException.class, () -> Fn.in(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notIn(null));
    }

    @Test
    public void notIn() {
        assertThat(Fn.notIn(java.util.Arrays.asList(1, 2, 3)).test(4)).isTrue();
    }

    @Test
    public void testNotIn() {
        List<String> list = Arrays.asList("a", "b", "c");
        Predicate<String> pred = Fn.notIn(list);
        assertFalse(pred.test("a"));
        assertFalse(pred.test("b"));
        assertTrue(pred.test("d"));

        Predicate<String> emptyPred = Fn.notIn(Collections.emptyList());
        assertTrue(emptyPred.test("a"));
    }

    @Test
    public void testTypePredicates() {
        assertTrue(Fn.instanceOf(String.class).test("a"));
        assertFalse(Fn.instanceOf(String.class).test(123));

        assertTrue(Fn.subtypeOf(Object.class).test(String.class));
        assertFalse(Fn.subtypeOf(String.class).test(Object.class));
    }

    @Test
    public void instanceOf() {
        assertThat(Fn.instanceOf(String.class).test("a")).isTrue();
    }

    @Test
    public void testInstanceOf() {
        Predicate<Object> pred = Fn.instanceOf(String.class);
        assertTrue(pred.test("test"));
        assertFalse(pred.test(123));
        assertFalse(pred.test(null));
    }

    @Test
    public void subtypeOf() {
        assertThat(Fn.subtypeOf(Object.class).test(String.class)).isTrue();
    }

    @Test
    public void testSubtypeOf() {
        Predicate<Class<?>> pred = Fn.subtypeOf(Number.class);
        assertTrue(pred.test(Integer.class));
        assertTrue(pred.test(Double.class));
        assertFalse(pred.test(String.class));
    }

    @Test
    public void testStringContentPredicates() {
        assertTrue(Fn.startsWith("pre").test("prefix"));
        assertFalse(Fn.startsWith("pre").test("postfix"));

        assertTrue(Fn.endsWith("fix").test("prefix"));
        assertFalse(Fn.endsWith("fix").test("prelude"));

        assertTrue(Fn.contains("fix").test("prefix"));
        assertFalse(Fn.contains("fix").test("prelude"));

        assertFalse(Fn.notStartsWith("pre").test("prefix"));
        assertTrue(Fn.notStartsWith("pre").test("postfix"));

        assertFalse(Fn.notEndsWith("fix").test("prefix"));
        assertTrue(Fn.notEndsWith("fix").test("prelude"));

        assertFalse(Fn.notContains("fix").test("prefix"));
        assertTrue(Fn.notContains("fix").test("prelude"));
    }

    @Test
    public void startsWith() {
        assertThat(Fn.startsWith("a").test("abc")).isTrue();
    }

    @Test
    public void testStartsWith() {
        Predicate<String> pred = Fn.startsWith("pre");
        assertTrue(pred.test("prefix"));
        assertFalse(pred.test("suffix"));
        assertFalse(pred.test(null));
    }

    @Test
    public void testStringPredicatesWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Fn.startsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.endsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.contains(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notStartsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notEndsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notContains(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.matches(null));
    }

    @Test
    public void endsWith() {
        assertThat(Fn.endsWith("c").test("abc")).isTrue();
    }

    @Test
    public void testEndsWith() {
        Predicate<String> pred = Fn.endsWith("fix");
        assertTrue(pred.test("suffix"));
        assertTrue(pred.test("prefix"));
        assertFalse(pred.test(null));
    }

    @Test
    public void contains() {
        assertThat(Fn.contains("b").test("abc")).isTrue();
    }

    @Test
    public void testBiFunctions_ofRemove() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        BiFunctions.<Integer, List<Integer>> ofRemove().apply(list, 2);
        assertFalse(list.contains(2));
        assertEquals(2, list.size());
    }

    @Test
    public void testBiFunctions_ofRemoveAll() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        List<Integer> toRemove = new ArrayList<>(Arrays.asList(2, 3));
        BiFunctions.<Integer, List<Integer>> ofRemoveAll().apply(list, toRemove);
        assertFalse(list.contains(2));
        assertFalse(list.contains(3));
        assertEquals(2, list.size());
    }

    @Test
    public void testContains() {
        Predicate<String> pred = Fn.contains("sub");
        assertTrue(pred.test("substring"));
        assertFalse(pred.test("string"));
        assertFalse(pred.test(null));
    }

    @Test
    public void testOfMergeToBigger() {
        Joiner joiner1 = Joiner.with(",");
        joiner1.append("a").append("b").append("c");

        Joiner joiner2 = Joiner.with(",");
        joiner2.append("d");

        BinaryOperator<Joiner> operator = Fn.BinaryOperators.ofMergeToBigger();

        Joiner result1 = operator.apply(joiner1, joiner2);
        assertSame(joiner1, result1);
        assertTrue(result1.toString().contains("a,b,c"));

        joiner1 = Joiner.with(",");
        joiner1.append("a");

        joiner2 = Joiner.with(",");
        joiner2.append("b").append("c").append("d");

        Joiner result2 = operator.apply(joiner1, joiner2);
        assertSame(joiner2, result2);
        assertTrue(result2.toString().contains("b,c,d"));
    }

    @Test
    public void testBiFunctions_ofMerge() {
        Joiner j1 = Joiner.with(",").append("a");
        Joiner j2 = Joiner.with(",").append("b");
        Joiner result = BiFunctions.ofMerge().apply(j1, j2);
        assertNotNull(result);
        assertTrue(result.toString().contains("a"));
    }

    @Test
    public void notStartsWith() {
        assertThat(Fn.notStartsWith("b").test("abc")).isTrue();
    }

    @Test
    public void testNotStartsWith() {
        Predicate<String> pred = Fn.notStartsWith("pre");
        assertFalse(pred.test("prefix"));
        assertTrue(pred.test("suffix"));
        assertTrue(pred.test(null));
    }

    @Test
    public void notEndsWith() {
        assertThat(Fn.notEndsWith("b").test("abc")).isTrue();
    }

    @Test
    public void testNotEndsWith() {
        Predicate<String> pred = Fn.notEndsWith("fix");
        assertFalse(pred.test("suffix"));
        assertFalse(pred.test("prefix"));
        assertTrue(pred.test(null));
    }

    @Test
    public void notContains() {
        assertThat(Fn.notContains("d").test("abc")).isTrue();
    }

    @Test
    public void testNotContains() {
        Predicate<String> pred = Fn.notContains("sub");
        assertFalse(pred.test("substring"));
        assertTrue(pred.test("string"));
        assertTrue(pred.test(null));
    }

    @Test
    public void testMatches() {
        Predicate<CharSequence> p = Fn.matches(Pattern.compile("\\d+"));
        assertTrue(p.test("123"));
        assertFalse(p.test("abc"));
    }

    @Test
    public void matches() {
        assertThat(Fn.matches(java.util.regex.Pattern.compile("a.c")).test("abc")).isTrue();
    }

    @Test
    public void testMatches_withNull() {
        Predicate<CharSequence> p = Fn.matches(Pattern.compile("\\d+"));
        assertFalse(p.test(null));
        assertTrue(p.test("123"));
        assertFalse(p.test("abc"));
    }

    @Test
    public void notPredicate() {
        Predicate<Integer> p = i -> i > 1;
        assertThat(Fn.not(p).test(1)).isTrue();
    }

    @Test
    public void notBiPredicate() {
        BiPredicate<Integer, Integer> p = (i, j) -> i > j;
        assertThat(Fn.not(p).test(1, 2)).isTrue();
    }

    @Test
    public void testNot() {
        Predicate<String> isEmpty = String::isEmpty;
        Predicate<String> isNotEmpty = Fn.not(isEmpty);
        assertTrue(isNotEmpty.test("a"));
        assertFalse(isNotEmpty.test(""));
    }

    @Test
    public void testNotNegative() {
        assertTrue(FI.notNegative().test(1));
        assertTrue(FI.notNegative().test(0));
        assertFalse(FI.notNegative().test(-1));
    }

    @Test
    public void testNot_TriPredicate() {
        TriPredicate<Integer, Integer, Integer> allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        TriPredicate<Integer, Integer, Integer> notAllPositive = Fn.not(allPositive);
        assertFalse(notAllPositive.test(1, 2, 3));
        assertTrue(notAllPositive.test(1, -1, 3));
    }

    @Test
    public void testNotBiPredicate() {
        Throwables.BiPredicate<Integer, Integer, RuntimeException> isGreater = (a, b) -> a > b;
        Throwables.BiPredicate<Integer, Integer, RuntimeException> notGreater = Fnn.not(isGreater);

        Assertions.assertTrue(notGreater.test(5, 10));
        Assertions.assertTrue(notGreater.test(5, 5));
        Assertions.assertFalse(notGreater.test(10, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.BiPredicate<Integer, Integer, RuntimeException>) null));
    }

    @Test
    public void testNotTriPredicate() {
        Throwables.TriPredicate<Integer, Integer, Integer, RuntimeException> allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        Throwables.TriPredicate<Integer, Integer, Integer, RuntimeException> notAllPositive = Fnn.not(allPositive);

        Assertions.assertTrue(notAllPositive.test(-1, 2, 3));
        Assertions.assertTrue(notAllPositive.test(1, -2, 3));
        Assertions.assertTrue(notAllPositive.test(1, 2, -3));
        Assertions.assertFalse(notAllPositive.test(1, 2, 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.TriPredicate<Integer, Integer, Integer, RuntimeException>) null));
    }

    @Test
    public void testLogicalOperationsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.not((Predicate<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.not((BiPredicate<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.not((TriPredicate<String, String, String>) null));

        assertThrows(IllegalArgumentException.class, () -> Fn.and((BooleanSupplier) null, () -> true));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(() -> true, (BooleanSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.and((Collection<Predicate<String>>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(Collections.<Predicate<String>> emptyList()));

        assertThrows(IllegalArgumentException.class, () -> Fn.or((BooleanSupplier) null, () -> true));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(() -> true, (BooleanSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.or((Collection<Predicate<String>>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(Collections.<Predicate<String>> emptyList()));
    }

    @Test
    public void testNotPredicate() throws Exception {
        Throwables.Predicate<Integer, Exception> isPositive = i -> i > 0;
        Throwables.Predicate<Integer, Exception> notPositive = Fnn.not(isPositive);

        assertFalse(notPositive.test(5));
        assertTrue(notPositive.test(-5));
        assertTrue(notPositive.test(0));

        assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.Predicate<?, ?>) null));
    }

    @Test
    public void testAndOr() {
        Predicate<String> p1 = s -> s.startsWith("a");
        Predicate<String> p2 = s -> s.length() > 2;
        assertTrue(Fn.and(p1, p2).test("abc"));
        assertFalse(Fn.and(p1, p2).test("ab"));
        assertFalse(Fn.and(p1, p2).test("bc"));

        assertTrue(Fn.or(p1, p2).test("abc"));
        assertTrue(Fn.or(p1, p2).test("ab"));
        assertFalse(Fn.or(p1, p2).test("bc"));
    }

    @Test
    public void and() {
        Predicate<Integer> p1 = i -> i > 0;
        Predicate<Integer> p2 = i -> i < 2;
        assertThat(Fn.and(p1, p2).test(1)).isTrue();
        assertThat(Fn.and(p1, p2).test(2)).isFalse();
    }

    @Test
    public void testAndBooleanSupplier() {
        BooleanSupplier and2 = Fn.and(() -> true, () -> true);
        assertTrue(and2.getAsBoolean());

        and2 = Fn.and(() -> true, () -> false);
        assertFalse(and2.getAsBoolean());

        BooleanSupplier and3 = Fn.and(() -> true, () -> true, () -> true);
        assertTrue(and3.getAsBoolean());

        and3 = Fn.and(() -> true, () -> false, () -> true);
        assertFalse(and3.getAsBoolean());
    }

    @Test
    public void testAndPredicateCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(x -> x > 0, x -> x < 10, x -> x % 2 == 0);
        Predicate<Integer> combined = Fn.and(predicates);
        assertTrue(combined.test(4));
        assertFalse(combined.test(5));
        assertFalse(combined.test(12));
    }

    @Test
    public void testAndBiPredicate() {
        BiPredicate<String, Integer> pred1 = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> pred2 = (s, i) -> i > 0;

        BiPredicate<String, Integer> and2 = Fn.and(pred1, pred2);
        assertTrue(and2.test("hello", 5));
        assertFalse(and2.test("hello", 4));
        assertFalse(and2.test("", 0));

        BiPredicate<String, Integer> pred3 = (s, i) -> s.startsWith("h");
        BiPredicate<String, Integer> and3 = Fn.and(pred1, pred2, pred3);
        assertTrue(and3.test("hello", 5));
        assertFalse(and3.test("world", 5));
    }

    @Test
    public void testAndBiPredicateList() {
        List<BiPredicate<String, Integer>> predicates = Arrays.asList((s, i) -> s.length() == i, (s, i) -> i > 0);
        BiPredicate<String, Integer> combined = Fn.and(predicates);
        assertTrue(combined.test("hello", 5));
        assertFalse(combined.test("hello", 4));
    }

    @Test
    public void testAndBooleanSupplier2() {
        BooleanSupplier and = Fn.and(() -> true, () -> true);
        assertTrue(and.getAsBoolean());

        and = Fn.and(() -> true, () -> false);
        assertFalse(and.getAsBoolean());

        and = Fn.and(() -> false, () -> true);
        assertFalse(and.getAsBoolean());
    }

    @Test
    public void testAndBooleanSupplier3() {
        BooleanSupplier and = Fn.and(() -> true, () -> true, () -> true);
        assertTrue(and.getAsBoolean());

        and = Fn.and(() -> true, () -> false, () -> true);
        assertFalse(and.getAsBoolean());
    }

    @Test
    public void testAndPredicate2() {
        Predicate<Integer> and = Fn.and(i -> i > 0, i -> i < 10);
        assertTrue(and.test(5));
        assertFalse(and.test(-1));
        assertFalse(and.test(15));
    }

    @Test
    public void testAndPredicate3() {
        Predicate<Integer> and = Fn.and(i -> i > 0, i -> i < 10, i -> i % 2 == 0);
        assertTrue(and.test(4));
        assertFalse(and.test(5));
        assertFalse(and.test(-2));
    }

    @Test
    public void testAndBiPredicate2() {
        BiPredicate<Integer, Integer> and = Fn.and((a, b) -> a > 0, (a, b) -> b > 0);
        assertTrue(and.test(1, 2));
        assertFalse(and.test(-1, 2));
        assertFalse(and.test(1, -2));
    }

    @Test
    public void testAndBiPredicate3() {
        BiPredicate<Integer, Integer> and = Fn.and((a, b) -> a > 0, (a, b) -> b > 0, (a, b) -> a + b < 10);
        assertTrue(and.test(2, 3));
        assertFalse(and.test(5, 6));
    }

    @Test
    public void testAndCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(n -> n > 0, n -> n < 100, n -> n % 2 == 0);

        Predicate<Integer> combined = Fn.and(predicates);

        assertTrue(combined.test(50));
        assertFalse(combined.test(51));
        assertFalse(combined.test(0));
        assertFalse(combined.test(100));
    }

    @Test
    public void testAndBooleanSupplier_threeLazy() {
        BooleanSupplier and3 = Fn.and(() -> true, () -> true, () -> true);
        assertTrue(and3.getAsBoolean());
        BooleanSupplier and3False = Fn.and(() -> true, () -> false, () -> true);
        assertFalse(and3False.getAsBoolean());
    }

    @Test
    public void testAndPredicate() {
        Predicate<String> notNull = Fn.notNull();
        Predicate<String> notEmpty = Fn.notEmpty();

        Predicate<String> and2 = Fn.and(notNull, notEmpty);
        assertTrue(and2.test("test"));
        assertFalse(and2.test(""));
        assertFalse(and2.test(null));

        Predicate<String> notBlank = Fn.notBlank();
        Predicate<String> and3 = Fn.and(notNull, notEmpty, notBlank);
        assertTrue(and3.test("test"));
        assertFalse(and3.test("   "));
    }

    @Test
    public void testAndThreePredicates() {
        Predicate<String> notNull = Fn.notNull();
        Predicate<String> notEmpty = Fn.notEmpty();
        Predicate<String> longerThan3 = s -> s.length() > 3;

        Predicate<String> combined = Fn.and(notNull, notEmpty, longerThan3);

        assertTrue(combined.test("hello"));
        assertFalse(combined.test("hi"));
        assertFalse(combined.test(""));
        assertFalse(combined.test(null));
    }

    @Test
    public void testAndBiPredicateThree() {
        BiPredicate<String, Integer> notNullAndPositive = (s, n) -> s != null && n > 0;
        BiPredicate<String, Integer> longerThanValue = (s, n) -> s.length() > n;
        BiPredicate<String, Integer> containsA = (s, n) -> s.contains("a");

        BiPredicate<String, Integer> combined = Fn.and(notNullAndPositive, longerThanValue, containsA);

        assertTrue(combined.test("banana", 3));
        assertFalse(combined.test("bnn", 2));
        assertFalse(combined.test("a", 2));
    }

    @Test
    public void testAnd() {
        Predicate<String> notNull = Fn.notNull();
        Predicate<String> notEmpty = Fn.notEmpty();
        Predicate<String> notNullAndNotEmpty = Fn.and(notNull, notEmpty);

        assertTrue(notNullAndNotEmpty.test("hello"));
        assertFalse(notNullAndNotEmpty.test(null));
        assertFalse(notNullAndNotEmpty.test(""));
    }

    @Test
    public void or() {
        Predicate<Integer> p1 = i -> i > 2;
        Predicate<Integer> p2 = i -> i < 1;
        assertThat(Fn.or(p1, p2).test(3)).isTrue();
        assertThat(Fn.or(p1, p2).test(0)).isTrue();
        assertThat(Fn.or(p1, p2).test(1)).isFalse();
    }

    @Test
    public void testOrBooleanSupplier() {
        BooleanSupplier or2 = Fn.or(() -> false, () -> false);
        assertFalse(or2.getAsBoolean());

        or2 = Fn.or(() -> true, () -> false);
        assertTrue(or2.getAsBoolean());

        BooleanSupplier or3 = Fn.or(() -> false, () -> false, () -> false);
        assertFalse(or3.getAsBoolean());

        or3 = Fn.or(() -> false, () -> true, () -> false);
        assertTrue(or3.getAsBoolean());
    }

    @Test
    public void testOrPredicateCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(x -> x < 0, x -> x > 10, x -> x == 5);
        Predicate<Integer> combined = Fn.or(predicates);
        assertTrue(combined.test(-1));
        assertTrue(combined.test(15));
        assertTrue(combined.test(5));
        assertFalse(combined.test(3));
    }

    @Test
    public void testOrBiPredicateList() {
        List<BiPredicate<String, Integer>> predicates = Arrays.asList((s, i) -> s.length() == i, (s, i) -> i == 0);
        BiPredicate<String, Integer> combined = Fn.or(predicates);
        assertTrue(combined.test("hello", 5));
        assertTrue(combined.test("world", 0));
        assertFalse(combined.test("hello", 4));
    }

    @Test
    public void testOrBooleanSupplier2() {
        BooleanSupplier or = Fn.or(() -> true, () -> false);
        assertTrue(or.getAsBoolean());

        or = Fn.or(() -> false, () -> false);
        assertFalse(or.getAsBoolean());
    }

    @Test
    public void testOrBooleanSupplier3() {
        BooleanSupplier or = Fn.or(() -> false, () -> false, () -> true);
        assertTrue(or.getAsBoolean());

        or = Fn.or(() -> false, () -> false, () -> false);
        assertFalse(or.getAsBoolean());
    }

    @Test
    public void testOrPredicate2() {
        Predicate<Integer> or = Fn.or(i -> i < 0, i -> i > 10);
        assertTrue(or.test(-5));
        assertTrue(or.test(15));
        assertFalse(or.test(5));
    }

    @Test
    public void testOrPredicate3() {
        Predicate<Integer> or = Fn.or(i -> i < 0, i -> i > 10, i -> i == 5);
        assertTrue(or.test(-5));
        assertTrue(or.test(15));
        assertTrue(or.test(5));
        assertFalse(or.test(3));
    }

    @Test
    public void testOrBiPredicate2() {
        BiPredicate<Integer, Integer> or = Fn.or((a, b) -> a < 0, (a, b) -> b < 0);
        assertTrue(or.test(-1, 2));
        assertTrue(or.test(1, -2));
        assertFalse(or.test(1, 2));
    }

    @Test
    public void testOrBiPredicate3() {
        BiPredicate<Integer, Integer> or = Fn.or((a, b) -> a < 0, (a, b) -> b < 0, (a, b) -> a + b > 10);
        assertTrue(or.test(-1, 2));
        assertTrue(or.test(6, 6));
        assertFalse(or.test(2, 3));
    }

    @Test
    public void testOrCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(n -> n < 0, n -> n > 100, n -> n == 50);

        Predicate<Integer> combined = Fn.or(predicates);

        assertTrue(combined.test(-5));
        assertTrue(combined.test(150));
        assertTrue(combined.test(50));
        assertFalse(combined.test(25));
    }

    @Test
    public void testOrBiPredicate3_threeArgs() {
        BiPredicate<Integer, Integer> or = Fn.or((a, b) -> a < 0, (a, b) -> b < 0, (a, b) -> a + b > 100);
        assertTrue(or.test(-1, 1));
        assertTrue(or.test(1, -1));
        assertTrue(or.test(60, 50));
        assertFalse(or.test(1, 1));
    }

    @Test
    public void testOrPredicate() {
        Predicate<String> isNull = Fn.isNull();
        Predicate<String> isEmpty = Fn.isEmpty();

        Predicate<String> or2 = Fn.or(isNull, isEmpty);
        assertTrue(or2.test(null));
        assertTrue(or2.test(""));
        assertFalse(or2.test("test"));

        Predicate<String> isBlank = Fn.isBlank();
        Predicate<String> or3 = Fn.or(isNull, isEmpty, isBlank);
        assertTrue(or3.test("   "));
    }

    @Test
    public void testOrBiPredicate() {
        BiPredicate<String, Integer> pred1 = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> pred2 = (s, i) -> i == 0;

        BiPredicate<String, Integer> or2 = Fn.or(pred1, pred2);
        assertTrue(or2.test("hello", 5));
        assertTrue(or2.test("world", 0));
        assertFalse(or2.test("hello", 4));

        BiPredicate<String, Integer> pred3 = (s, i) -> s.isEmpty();
        BiPredicate<String, Integer> or3 = Fn.or(pred1, pred2, pred3);
        assertTrue(or3.test("", 10));
    }

    @Test
    public void testOrThreePredicates() {
        Predicate<String> isEmpty = Fn.isEmpty();
        Predicate<String> isBlank = Fn.isBlank();
        Predicate<String> equalsNone = s -> "none".equals(s);

        Predicate<String> combined = Fn.or(isEmpty, isBlank, equalsNone);

        assertTrue(combined.test(""));
        assertTrue(combined.test("   "));
        assertTrue(combined.test("none"));
        assertFalse(combined.test("hello"));
    }

    @Test
    public void testOr() {
        Predicate<String> isNull = Fn.isNull();
        Predicate<String> isEmpty = Fn.isEmpty();
        Predicate<String> isNullOrEmpty = Fn.or(isNull, isEmpty);

        assertTrue(isNullOrEmpty.test(null));
        assertTrue(isNullOrEmpty.test(""));
        assertFalse(isNullOrEmpty.test("hello"));
    }

    @Test
    public void testByKey() {
        Predicate<String> p = s -> s.startsWith("a");
        assertThat(Fn.<String, Integer> testByKey(p).test(Fn.<String, Integer> entry().apply("abc", 1))).isTrue();
    }

    @Test
    public void testTestByKey() {
        Map.Entry<String, Integer> entry1 = CommonUtil.newImmutableEntry("key1", 100);
        Map.Entry<String, Integer> entry2 = CommonUtil.newImmutableEntry("key2", 200);

        Throwables.Predicate<Map.Entry<String, Integer>, RuntimeException> predicate = Fnn.testByKey(k -> k.equals("key1"));

        Assertions.assertTrue(predicate.test(entry1));
        Assertions.assertFalse(predicate.test(entry2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.testByKey(null));
    }

    @Test
    public void testMapEntryFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.testByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.testByValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptByValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyByValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.mapKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.mapValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.testKeyVal(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptKeyVal(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyKeyVal(null));
    }

    @Test
    public void testByValue() {
        Predicate<Integer> p = i -> i > 0;
        assertThat(Fn.<String, Integer> testByValue(p).test(Fn.<String, Integer> entry().apply("a", 1))).isTrue();
    }

    @Test
    public void testTestByValue() {
        Map.Entry<String, Integer> entry1 = CommonUtil.newImmutableEntry("key1", 100);
        Map.Entry<String, Integer> entry2 = CommonUtil.newImmutableEntry("key2", 200);

        Throwables.Predicate<Map.Entry<String, Integer>, RuntimeException> predicate = Fnn.testByValue(v -> v > 150);

        Assertions.assertFalse(predicate.test(entry1));
        Assertions.assertTrue(predicate.test(entry2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.testByValue(null));
    }

    @Test
    public void acceptByKey() {
        assertDoesNotThrow(() -> {
            Fn.<String, Integer> acceptByKey(System.out::println).accept(Fn.<String, Integer> entry().apply("abc", 1));
        });
    }

    @Test
    public void testAcceptByKey() {
        Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
        final String[] result = { "" };

        Throwables.Consumer<Map.Entry<String, Integer>, RuntimeException> consumer = Fnn.acceptByKey(k -> result[0] = k);

        consumer.accept(entry);
        Assertions.assertEquals("key", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.acceptByKey(null));
    }

    @Test
    public void acceptByValue() {
        assertDoesNotThrow(() -> {
            Fn.<String, Integer> acceptByValue(System.out::println).accept(Fn.<String, Integer> entry().apply("abc", 1));
        });
    }

    @Test
    public void testAcceptByValue() {
        Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
        final int[] result = { 0 };

        Throwables.Consumer<Map.Entry<String, Integer>, RuntimeException> consumer = Fnn.acceptByValue(v -> result[0] = v);

        consumer.accept(entry);
        Assertions.assertEquals(100, result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.acceptByValue(null));
    }

    @Test
    public void applyByKey() {
        Function<String, Integer> f = String::length;
        assertThat(Fn.<String, Integer, Integer> applyByKey(f).apply(Fn.<String, Integer> entry().apply("abc", 1))).isEqualTo(3);
    }

    @Test
    public void testApplyByKey() {
        Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);

        Throwables.Function<Map.Entry<String, Integer>, String, RuntimeException> func = Fnn.applyByKey(k -> k.toUpperCase());

        Assertions.assertEquals("KEY", func.apply(entry));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.applyByKey(null));
    }

    @Test
    public void applyByValue() {
        Function<Integer, Integer> f = i -> i + 1;
        assertThat(Fn.<String, Integer, Integer> applyByValue(f).apply(Fn.<String, Integer> entry().apply("abc", 1))).isEqualTo(2);
    }

    @Test
    public void testApplyByValue() {
        Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);

        Throwables.Function<Map.Entry<String, Integer>, Integer, RuntimeException> func = Fnn.applyByValue(v -> v * 2);

        Assertions.assertEquals(200, func.apply(entry));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.applyByValue(null));
    }

    @Test
    public void testMapKeyAndValue() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("key", 10);
        Map.Entry<Integer, Integer> newKeyEntry = Fn.<String, Integer, Integer> mapKey(String::length).apply(entry);
        assertEquals(3, newKeyEntry.getKey());
        assertEquals(10, newKeyEntry.getValue());

        Map.Entry<String, String> newValEntry = Fn.<String, Integer, String> mapValue(Object::toString).apply(entry);
        assertEquals("key", newValEntry.getKey());
        assertEquals("10", newValEntry.getValue());
    }

    @Test
    public void testMapKey() {
        Function<Map.Entry<String, Integer>, Map.Entry<Integer, Integer>> func = Fn.mapKey(String::length);
        Map.Entry<Integer, Integer> result = func.apply(new AbstractMap.SimpleEntry<>("hello", 123));
        assertEquals(5, result.getKey());
        assertEquals(123, result.getValue());
    }

    @Test
    public void mapKey() {
        Function<String, String> f = String::toUpperCase;
        assertThat(Fn.<String, Integer, String> mapKey(f).apply(Fn.<String, Integer> entry().apply("abc", 1)).getKey()).isEqualTo("ABC");
    }

    @Test
    public void mapValue() {
        Function<Integer, Integer> f = i -> i + 1;
        assertThat(Fn.<String, Integer, Integer> mapValue(f).apply(Fn.<String, Integer> entry().apply("abc", 1)).getValue()).isEqualTo(2);
    }

    @Test
    public void testMapValue() {
        Function<Map.Entry<String, Integer>, Map.Entry<String, String>> func = Fn.mapValue(Object::toString);
        Map.Entry<String, String> result = func.apply(new AbstractMap.SimpleEntry<>("key", 123));
        assertEquals("key", result.getKey());
        assertEquals("123", result.getValue());
    }

    @Test
    public void testTestKeyVal() {
        Predicate<Map.Entry<String, Integer>> pred = Fn.testKeyVal((k, v) -> k.length() == v);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("hello", 5)));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("hello", 4)));
    }

    @Test
    public void testAcceptKeyVal() {
        Map<String, Integer> map = new HashMap<>();
        Consumer<Map.Entry<String, Integer>> consumer = Fn.acceptKeyVal(map::put);
        consumer.accept(new AbstractMap.SimpleEntry<>("key1", 1));
        consumer.accept(new AbstractMap.SimpleEntry<>("key2", 2));
        assertEquals(2, map.size());
        assertEquals(1, map.get("key1").intValue());
    }

    @Test
    public void acceptKeyVal() {
        assertDoesNotThrow(() -> {
            Fn.<String, Integer> acceptKeyVal((s, i) -> {
            }).accept(Fn.<String, Integer> entry().apply("abc", 1));
        });
    }

    @Test
    public void applyKeyVal() {
        assertThat(Fn.<String, Integer, String> applyKeyVal((s, i) -> s + i).apply(Fn.<String, Integer> entry().apply("abc", 1))).isEqualTo("abc1");
    }

    @Test
    public void testApplyKeyVal() {
        Function<Map.Entry<String, Integer>, String> func = Fn.applyKeyVal((k, v) -> k + "=" + v);
        assertEquals("key=123", func.apply(new AbstractMap.SimpleEntry<>("key", 123)));
    }

    @Test
    public void testConditionalAccept() {
        List<String> list = new ArrayList<>();
        Consumer<String> consumer = Fn.acceptIfNotNull(list::add);
        Stream.of("a", null, "b").forEach(consumer);
        assertEquals(Arrays.asList("a", "b"), list);

        list.clear();
        Consumer<String> consumer2 = Fn.acceptIf(s -> s.length() > 1, list::add);
        Stream.of("a", "bb", "c", "dd").forEach(consumer2);
        assertEquals(Arrays.asList("bb", "dd"), list);

        List<String> trueList = new ArrayList<>();
        List<String> falseList = new ArrayList<>();
        Consumer<String> consumer3 = Fn.acceptIfOrElse(s -> s.length() > 1, trueList::add, falseList::add);
        Stream.of("a", "bb", "c", "dd").forEach(consumer3);
        assertEquals(Arrays.asList("bb", "dd"), trueList);
        assertEquals(Arrays.asList("a", "c"), falseList);
    }

    @Test
    public void testAcceptIfNotNull() {
        List<String> list = new ArrayList<>();
        Consumer<String> consumer = Fn.acceptIfNotNull(list::add);
        consumer.accept("test");
        consumer.accept(null);
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void acceptIfNotNull() {
        assertDoesNotThrow(() -> {
            Fn.acceptIfNotNull(System.out::println).accept("a");
            Fn.acceptIfNotNull(System.out::println).accept(null);
        });
    }

    @Test
    public void testConditionalFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfNotNull(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIf(null, System.out::println));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIf(x -> true, null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfOrElse(null, System.out::println, System.out::println));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfOrElse(x -> true, null, System.out::println));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfOrElse(x -> true, System.out::println, null));
    }

    @Test
    public void testAcceptIf() {
        List<Integer> list = new ArrayList<>();
        Consumer<Integer> consumer = Fn.acceptIf(i -> i > 5, list::add);
        consumer.accept(3);
        consumer.accept(7);
        consumer.accept(10);
        assertEquals(Arrays.asList(7, 10), list);
    }

    @Test
    public void acceptIf() {
        Predicate<Integer> p = i -> i > 0;
        Fn.acceptIf(p, System.out::println).accept(1);
        Fn.acceptIf(p, System.out::println).accept(0);
        assertNotNull(p);
    }

    @Test
    public void testAcceptIfOrElse() {
        List<Integer> evens = new ArrayList<>();
        List<Integer> odds = new ArrayList<>();
        Consumer<Integer> consumer = Fn.acceptIfOrElse(i -> i % 2 == 0, evens::add, odds::add);
        consumer.accept(2);
        consumer.accept(3);
        consumer.accept(4);
        assertEquals(Arrays.asList(2, 4), evens);
        assertEquals(Arrays.asList(3), odds);
    }

    @Test
    public void acceptIfOrElse() {
        Predicate<Integer> p = i -> i > 0;
        Fn.acceptIfOrElse(p, System.out::println, System.out::println).accept(1);
        assertNotNull(p);
    }

    @Test
    public void testConditionalApply() {
        Function<String, Collection<String>> f1 = Fn.applyIfNotNullOrEmpty(s -> Arrays.asList(s, s));
        assertEquals(Collections.emptyList(), f1.apply(null));
        assertEquals(Arrays.asList("a", "a"), f1.apply("a"));
    }

    @Test
    public void applyIfNotNullOrEmpty() {
        Function<List<Integer>, List<Integer>> f = l -> {
            l.add(1);
            return l;
        };
        assertThat(Fn.applyIfNotNullOrEmpty(f).apply((List<Integer>) null)).isNotNull();
    }

    @Test
    public void testApplyIfNotNullOrEmpty() {
        Function<List<String>, Collection<String>> func = Fn.applyIfNotNullOrEmpty(l -> l.subList(0, 2));
        assertEquals(Arrays.asList("a", "b"), func.apply(Arrays.asList("a", "b", "c")));
        assertTrue(func.apply(null).isEmpty());
    }

    @Test
    public void applyIfNotNullOrDefault() {
        Function<String, Integer> f1 = String::length;
        Function<Integer, Integer> f2 = i -> i + 1;
        assertThat(Fn.applyIfNotNullOrDefault(f1, f2, 0).apply(null)).isZero();
        assertThat(Fn.applyIfNotNullOrDefault(f1, f2, 0).apply("a")).isEqualTo(2);
    }

    @Test
    public void testApplyIfNotNullOrDefault2() {
        Function<String, String> func = Fn.applyIfNotNullOrDefault(String::trim, String::toUpperCase, "DEFAULT");
        assertEquals("HELLO", func.apply("  hello  "));
        assertEquals("DEFAULT", func.apply(null));
        assertEquals("", func.apply("  "));
    }

    @Test
    public void testApplyIfNotNullOrDefault3() {
        Function<String, Integer> func = Fn.applyIfNotNullOrDefault(String::trim, String::toUpperCase, String::length, -1);
        assertEquals(5, func.apply("  hello  "));
        assertEquals(-1, func.apply(null));
        assertEquals(0, func.apply("  "));
    }

    @Test
    public void testApplyIfNotNullOrDefault4() {
        Function<String, Character> func = Fn.applyIfNotNullOrDefault(String::trim, String::toUpperCase, s -> s.substring(0, 1), s -> s.charAt(0), '?');
        assertEquals('H', func.apply("  hello  "));
        assertEquals('?', func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrDefault() {
        Function<String, Integer> getLengthOrDefault = Fn.applyIfNotNullOrDefault(s -> s, String::length, -1);

        assertEquals(Integer.valueOf(5), getLengthOrDefault.apply("hello"));
        assertEquals(Integer.valueOf(-1), getLengthOrDefault.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrDefault3Functions() {
        Function<String, Integer> complexFunction = Fn.applyIfNotNullOrDefault(s -> s.toUpperCase(), s -> s.substring(0, 3), String::length, -1);

        assertEquals(Integer.valueOf(3), complexFunction.apply("hello"));
        assertEquals(Integer.valueOf(-1), complexFunction.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrDefault_nullIntermediateB() {
        // mapperA returns null - should return default
        Function<String, Integer> func = Fn.applyIfNotNullOrDefault((String s) -> null, // mapperA returns null
                (Object b) -> 99, // mapperB should not be called
                -1);
        assertEquals(-1, (int) func.apply("anything"));
        assertEquals(-1, (int) func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrDefault3_nullIntermediates() {
        // 3-mapper version with null intermediates
        Function<String, Integer> func = Fn.applyIfNotNullOrDefault((String s) -> s.isEmpty() ? null : s.trim(), // returns null for empty
                (String b) -> b, (String c) -> c.length(), -1);
        assertEquals(-1, (int) func.apply("")); // mapperA returns null
        assertEquals(5, (int) func.apply("hello"));
        assertEquals(-1, (int) func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrDefault4_nullIntermediates() {
        // 4-mapper version
        Function<String, Character> func = Fn.applyIfNotNullOrDefault((String s) -> s.isEmpty() ? null : s.trim(),
                (String b) -> b.length() == 0 ? null : b.toUpperCase(), (String c) -> c.substring(0, 1), (String d) -> d.charAt(0), '?');
        assertEquals('?', (char) func.apply(null));
        assertEquals('?', (char) func.apply(""));
        assertEquals('H', (char) func.apply("hello"));
    }

    @Test
    public void testApplyIfNotNullOrDefaultWithNullMappers() {
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(null, String::length, 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(String::trim, null, 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(null, String::trim, String::length, 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(String::trim, null, String::length, 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(String::trim, String::toUpperCase, null, 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(null, s -> s, s -> s, s -> s, 'a'));
    }

    @Test
    public void applyIfNotNullOrElseGet() {
        Function<String, Integer> f1 = String::length;
        Function<Integer, Integer> f2 = i -> i + 1;
        assertThat(Fn.applyIfNotNullOrElseGet(f1, f2, () -> 0).apply(null)).isZero();
        assertThat(Fn.applyIfNotNullOrElseGet(f1, f2, () -> 0).apply("a")).isEqualTo(2);
    }

    @Test
    public void testApplyIfNotNullOrElseGet2() {
        Function<String, String> func = Fn.applyIfNotNullOrElseGet(String::trim, String::toUpperCase, () -> "SUPPLIED");
        assertEquals("HELLO", func.apply("  hello  "));
        assertEquals("SUPPLIED", func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet3() {
        Function<String, Integer> func = Fn.applyIfNotNullOrElseGet(String::trim, String::toUpperCase, String::length, () -> -1);
        assertEquals(5, func.apply("  hello  "));
        assertEquals(-1, func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet4() {
        Function<String, Character> func = Fn.applyIfNotNullOrElseGet(String::trim, String::toUpperCase, s -> s.substring(0, 1), s -> s.charAt(0), () -> '?');
        assertEquals('H', func.apply("  hello  "));
        assertEquals('?', func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> getLengthOrSupplied = Fn.applyIfNotNullOrElseGet(s -> s, String::length, counter::incrementAndGet);

        assertEquals(Integer.valueOf(5), getLengthOrSupplied.apply("hello"));
        assertEquals(Integer.valueOf(1), getLengthOrSupplied.apply(null));
        assertEquals(Integer.valueOf(2), getLengthOrSupplied.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet_nullIntermediateB() {
        AtomicInteger supplierCount = new AtomicInteger(0);
        Function<String, Integer> func = Fn.applyIfNotNullOrElseGet((String s) -> null, // mapperA returns null
                (Object b) -> 99, () -> {
                    supplierCount.incrementAndGet();
                    return -1;
                });
        assertEquals(-1, (int) func.apply("anything"));
        assertEquals(1, supplierCount.get());
        assertEquals(-1, (int) func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet4_nullIntermediates() {
        Function<String, Character> func = Fn.applyIfNotNullOrElseGet((String s) -> s.isEmpty() ? null : s.trim(),
                (String b) -> b.length() == 0 ? null : b.toUpperCase(), (String c) -> c.substring(0, 1), (String d) -> d.charAt(0), () -> '?');
        assertEquals('?', (char) func.apply(null));
        assertEquals('?', (char) func.apply(""));
        assertEquals('H', (char) func.apply("hello"));
    }

    @Test
    public void testApplyIfNotNullOrElseGetWithNullMappers() {
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(null, String::length, () -> 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(String::trim, null, () -> 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(null, String::trim, String::length, () -> 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(String::trim, null, String::length, () -> 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(String::trim, String::toUpperCase, null, () -> 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(null, s -> s, s -> s, s -> s, () -> 'a'));
    }

    @Test
    public void applyIfOrElseDefault() {
        Predicate<Integer> p = i -> i > 0;
        Function<Integer, Integer> f = i -> i + 1;
        assertThat(Fn.applyIfOrElseDefault(p, f, 0).apply(1)).isEqualTo(2);
        assertThat(Fn.applyIfOrElseDefault(p, f, 0).apply(0)).isZero();
    }

    @Test
    public void testApplyIfOrElseDefault() {
        Function<Integer, String> func = Fn.applyIfOrElseDefault(i -> i > 0, i -> "positive: " + i, "non-positive");
        assertEquals("positive: 5", func.apply(5));
        assertEquals("non-positive", func.apply(-3));
        assertEquals("non-positive", func.apply(0));
    }

    @Test
    public void testApplyIfFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseDefault(null, x -> x, "default"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseDefault(x -> true, null, "default"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseGet(null, x -> x, () -> "default"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseGet(x -> true, null, () -> "default"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseGet(x -> true, x -> x, null));
    }

    @Test
    public void applyIfOrElseGet() {
        Predicate<Integer> p = i -> i > 0;
        Function<Integer, Integer> f = i -> i + 1;
        assertThat(Fn.applyIfOrElseGet(p, f, () -> 0).apply(1)).isEqualTo(2);
        assertThat(Fn.applyIfOrElseGet(p, f, () -> 0).apply(0)).isZero();
    }

    @Test
    public void testApplyIfOrElseGet() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, String> func = Fn.applyIfOrElseGet(i -> i > 0, i -> "positive: " + i, () -> "supplied: " + counter.incrementAndGet());
        assertEquals("positive: 5", func.apply(5));
        assertEquals("supplied: 1", func.apply(-3));
        assertEquals("supplied: 2", func.apply(0));
    }

    @Test
    public void flatmapValue() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));
        assertThat(Fn.<String, Integer> flatmapValue().apply(map)).hasSize(2);
    }

    @Test
    public void testFlatmapValue() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5, 6));
        map.put("c", Arrays.asList(7, 8));

        Function<Map<String, ? extends Collection<Integer>>, List<Map<String, Integer>>> func = Fn.flatmapValue();
        List<Map<String, Integer>> result = func.apply(map);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).get("a").intValue());
        assertEquals(4, result.get(0).get("b").intValue());
        assertEquals(7, result.get(0).get("c").intValue());
    }

    @Test
    public void testParsingFunctions() {
        assertEquals((byte) 12, Fn.parseByte().applyAsByte("12"));
        assertEquals((short) 123, Fn.parseShort().applyAsShort("123"));
        assertEquals(1234, Fn.parseInt().applyAsInt("1234"));
        assertEquals(12345L, Fn.parseLong().applyAsLong("12345"));
        assertEquals(12.3f, Fn.parseFloat().applyAsFloat("12.3"), 0.01);
        assertEquals(12.34, Fn.parseDouble().applyAsDouble("12.34"), 0.01);
        assertEquals(123, Fn.createNumber().apply("123"));
    }

    @Test
    public void parseByte() {
        assertThat(Fn.parseByte().applyAsByte("1")).isEqualTo((byte) 1);
    }

    @Test
    public void testParseByte() {
        ToByteFunction<String> parser = Fn.parseByte();
        assertEquals((byte) 123, parser.applyAsByte("123"));
        assertEquals((byte) -45, parser.applyAsByte("-45"));
    }

    @Test
    public void parseShort() {
        assertThat(Fn.parseShort().applyAsShort("1")).isEqualTo((short) 1);
    }

    @Test
    public void testParseShort() {
        ToShortFunction<String> parser = Fn.parseShort();
        assertEquals((short) 12345, parser.applyAsShort("12345"));
        assertEquals((short) -678, parser.applyAsShort("-678"));
    }

    @Test
    public void parseInt() {
        assertThat(Fn.parseInt().applyAsInt("1")).isEqualTo(1);
    }

    @Test
    public void testParseInt() {
        ToIntFunction<String> parser = Fn.parseInt();
        assertEquals(123456, parser.applyAsInt("123456"));
        assertEquals(-789, parser.applyAsInt("-789"));
    }

    @Test
    public void parseLong() {
        assertThat(Fn.parseLong().applyAsLong("1")).isEqualTo(1L);
    }

    @Test
    public void testParseLong() {
        ToLongFunction<String> parser = Fn.parseLong();
        assertEquals(123456789L, parser.applyAsLong("123456789"));
        assertEquals(-987654321L, parser.applyAsLong("-987654321"));
    }

    @Test
    public void parseFloat() {
        assertThat(Fn.parseFloat().applyAsFloat("1.0")).isEqualTo(1.0f);
    }

    @Test
    public void testParseFloat() {
        ToFloatFunction<String> parser = Fn.parseFloat();
        assertEquals(123.45f, parser.applyAsFloat("123.45"), 0.001f);
        assertEquals(-67.89f, parser.applyAsFloat("-67.89"), 0.001f);
    }

    @Test
    public void parseDouble() {
        assertThat(Fn.parseDouble().applyAsDouble("1.0")).isEqualTo(1.0);
    }

    @Test
    public void testParseDouble() {
        ToDoubleFunction<String> parser = Fn.parseDouble();
        assertEquals(123.456789, parser.applyAsDouble("123.456789"), 0.000001);
        assertEquals(-987.654321, parser.applyAsDouble("-987.654321"), 0.000001);
    }

    @Test
    public void createNumber() {
        assertThat(Fn.createNumber().apply("1")).isEqualTo(1);
    }

    @Test
    public void testCreateNumber() {
        Function<String, Number> creator = Fn.createNumber();
        assertTrue(creator.apply("123") instanceof Integer);
        assertTrue(creator.apply("123456789012") instanceof Long);
        assertTrue(creator.apply("123.45") instanceof Double);
        assertNull(creator.apply(""));
        assertNull(creator.apply(null));
    }

    @Test
    public void testNumToPrimitives() {
        assertEquals(123, Fn.numToInt().applyAsInt(123.45));
        assertEquals(123L, Fn.numToLong().applyAsLong(123.45));
        assertEquals(123.45, Fn.numToDouble().applyAsDouble(123.45f), 0.001);
    }

    @Test
    public void numToInt() {
        assertThat(Fn.numToInt().applyAsInt(1.0)).isEqualTo(1);
    }

    @Test
    public void testNumToInt() {
        ToIntFunction<Number> converter = Fn.numToInt();
        assertEquals(123, converter.applyAsInt(123));
        assertEquals(123, converter.applyAsInt(123L));
        assertEquals(123, converter.applyAsInt(123.45));
    }

    @Test
    public void numToLong() {
        assertThat(Fn.numToLong().applyAsLong(1.0)).isEqualTo(1L);
    }

    @Test
    public void testNumToLong() {
        ToLongFunction<Number> converter = Fn.numToLong();
        assertEquals(123L, converter.applyAsLong(123));
        assertEquals(123456789012L, converter.applyAsLong(123456789012L));
        assertEquals(123L, converter.applyAsLong(123.45));
    }

    @Test
    public void numToDouble() {
        assertThat(Fn.numToDouble().applyAsDouble(1)).isEqualTo(1.0);
    }

    @Test
    public void testNumToDouble() {
        ToDoubleFunction<Number> converter = Fn.numToDouble();
        assertEquals(123.0, converter.applyAsDouble(123), 0.001);
        assertEquals(123.45, converter.applyAsDouble(123.45), 0.001);
        assertEquals(123.0, converter.applyAsDouble(123L), 0.001);
    }

    @Test
    public void testAtMost() {
        Predicate<Object> p = Fn.atMost(2);
        assertTrue(p.test("a"));
        assertTrue(p.test("b"));
        assertFalse(p.test("c"));
        assertFalse(p.test("d"));
    }

    @Test
    public void atMost() {
        Predicate<Integer> p = Fn.atMost(1);
        assertThat(p.test(1)).isTrue();
        assertThat(p.test(1)).isFalse();
    }

    @Test
    public void testLimitThenFilter() {
        Predicate<Integer> p = Fn.limitThenFilter(3, i -> i % 2 == 0);
        long count = IntStream.range(0, 10).filter(p::test).count();
        assertEquals(2, count);
        Predicate<Integer> p2 = Fn.limitThenFilter(3, i -> i > 0);
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).filter(p2).count());
    }

    @Test
    public void limitThenFilter() {
        Predicate<Integer> p = Fn.limitThenFilter(1, i -> i > 0);
        assertThat(p.test(1)).isTrue();
        assertThat(p.test(1)).isFalse();
    }

    @Test
    public void testLimitThenFilterPredicate() {
        Predicate<Integer> predicate = Fn.limitThenFilter(3, i -> i % 2 == 0);

        assertTrue(predicate.test(2));
        assertFalse(predicate.test(3));
        assertTrue(predicate.test(4));
        assertFalse(predicate.test(6));

        Predicate<String> zeroLimitPredicate = Fn.limitThenFilter(0, s -> true);
        assertFalse(zeroLimitPredicate.test("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(-1, i -> true));

        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(1, (Predicate<Integer>) null));
    }

    @Test
    public void testLimitThenFilterBiPredicate() {
        BiPredicate<Integer, String> biPredicate = Fn.limitThenFilter(2, (i, s) -> s.length() == i);

        assertTrue(biPredicate.test(3, "abc"));
        assertFalse(biPredicate.test(2, "abc"));
        assertFalse(biPredicate.test(4, "test"));

        BiPredicate<Integer, String> zeroLimitBiPredicate = Fn.limitThenFilter(0, (i, s) -> true);
        assertFalse(zeroLimitBiPredicate.test(1, "test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(-1, (BiPredicate<Integer, String>) (i, s) -> true));

        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(1, (BiPredicate<Integer, String>) null));
    }

    @Test
    public void testThreadSafetyScenarios() throws InterruptedException {
        final int threadCount = 10;
        final int iterationsPerThread = 100;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger totalPassed = new AtomicInteger(0);

        Predicate<Integer> limitPredicate = Fn.limitThenFilter(50, i -> true);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    if (limitPredicate.test(j)) {
                        totalPassed.incrementAndGet();
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(50, totalPassed.get());
    }

    @Test
    public void filterThenLimit() {
        Predicate<Integer> p = Fn.filterThenLimit(i -> i > 0, 1);
        assertThat(p.test(1)).isTrue();
        assertThat(p.test(1)).isFalse();
    }

    @Test
    public void testFilterThenLimit() {
        Predicate<Integer> evenThenLimit = Fn.filterThenLimit(n -> n % 2 == 0, 2);

        assertFalse(evenThenLimit.test(1));
        assertTrue(evenThenLimit.test(2));
        assertFalse(evenThenLimit.test(3));
        assertTrue(evenThenLimit.test(4));
        assertFalse(evenThenLimit.test(6));
    }

    @Test
    public void testFilterThenLimitPredicate() {
        Predicate<Integer> predicate = Fn.filterThenLimit(i -> i % 2 == 0, 2);

        assertFalse(predicate.test(1));
        assertTrue(predicate.test(2));
        assertFalse(predicate.test(3));
        assertTrue(predicate.test(4));
        assertFalse(predicate.test(6));

        Predicate<String> zeroLimitPredicate = Fn.filterThenLimit(s -> true, 0);
        assertFalse(zeroLimitPredicate.test("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit((Predicate) null, 1));

        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit(i -> true, -1));
    }

    @Test
    public void testFilterThenLimitBiPredicate() {
        BiPredicate<Integer, String> biPredicate = Fn.filterThenLimit((i, s) -> s.length() == i, 2);

        assertFalse(biPredicate.test(2, "abc"));
        assertTrue(biPredicate.test(3, "abc"));
        assertTrue(biPredicate.test(4, "test"));
        assertFalse(biPredicate.test(5, "hello"));

        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit((BiPredicate<Integer, String>) null, 1));

        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit((i, s) -> true, -1));
    }

    @Test
    public void testTimeLimit() throws InterruptedException {
        Predicate<Object> p = Fn.timeLimit(100);
        assertTrue(p.test(null));
        Thread.sleep(150);
        assertFalse(p.test(null));
    }

    @Test
    public void testTimeLimitMillis() throws InterruptedException {
        Predicate<String> timeLimitPredicate = Fn.timeLimit(100);
        assertTrue(timeLimitPredicate.test("test1"));
        Thread.sleep(50);
        assertTrue(timeLimitPredicate.test("test2"));
        Thread.sleep(80);
        assertFalse(timeLimitPredicate.test("test3"));

        Predicate<Integer> zeroTimePredicate = Fn.timeLimit(0);
        assertFalse(zeroTimePredicate.test(1));

        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit(-1));
    }

    @Test
    public void testTimeLimitDuration() throws InterruptedException {
        Duration duration = Duration.ofMillis(100);
        Predicate<String> timeLimitPredicate = Fn.timeLimit(duration);
        assertTrue(timeLimitPredicate.test("test1"));
        Thread.sleep(200);
        assertFalse(timeLimitPredicate.test("test2"));

        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit(null));
    }

    @Test
    public void testMemoryLeakPrevention() throws InterruptedException {
        Predicate<String> timeLimitPred = Fn.timeLimit(50);
        assertTrue(timeLimitPred.test("test"));

        Thread.sleep(100);
        assertFalse(timeLimitPred.test("test"));

        for (int i = 0; i < 100; i++) {
            Fn.timeLimit(10);
        }
        Thread.sleep(50);
    }

    @Test
    public void testTimeLimitWithDuration() throws InterruptedException {
        Predicate<String> timeLimit = Fn.timeLimit(Duration.ofMillis(100));

        assertTrue(timeLimit.test("first"));
        Thread.sleep(50);
        assertTrue(timeLimit.test("second"));
        Thread.sleep(89);
        assertFalse(timeLimit.test("third"));
    }

    @Test
    public void testIndexed() {
        Function<String, Indexed<String>> func = Fn.indexed();
        assertEquals(Indexed.of("a", 0), func.apply("a"));
        assertEquals(Indexed.of("b", 1), func.apply("b"));
    }

    @Test
    public void testIndexedFunction() {
        Function<String, Indexed<String>> indexedFunc = Fn.indexed();

        Indexed<String> first = indexedFunc.apply("a");
        assertEquals("a", first.value());
        assertEquals(0, first.index());

        Indexed<String> second = indexedFunc.apply("b");
        assertEquals("b", second.value());
        assertEquals(1, second.index());

        Indexed<String> third = indexedFunc.apply("c");
        assertEquals("c", third.value());
        assertEquals(2, third.index());
    }

    @Test
    public void testIndexedPredicate() {
        Predicate<String> indexedPredicate = Fn.indexed((index, value) -> index % 2 == 0);

        assertTrue(indexedPredicate.test("a"));
        assertFalse(indexedPredicate.test("b"));
        assertTrue(indexedPredicate.test("c"));
        assertFalse(indexedPredicate.test("d"));
    }

    @Test
    public void testPerformanceCharacteristics() {
        Function<String, Indexed<String>> indexedFunc = Fn.indexed();
        List<Indexed<String>> results = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            results.add(indexedFunc.apply("item" + i));
        }

        for (int i = 0; i < results.size(); i++) {
            assertEquals(i, results.get(i).index());
            assertEquals("item" + i, results.get(i).value());
        }
    }

    @Test
    public void testAccept() {
        // Tests anonymous Consumer.accept() in Consumers.indexed()
        List<String> results = new ArrayList<>();
        Consumer<String> indexedConsumer = Consumers.indexed((idx, s) -> results.add(idx + ":" + s));
        indexedConsumer.accept("hello");
        indexedConsumer.accept("world");
        assertEquals(2, results.size());
        assertEquals("0:hello", results.get(0));
        assertEquals("1:world", results.get(1));

        // Tests anonymous BiConsumer.accept() in BiConsumers.indexed()
        List<String> biResults = new ArrayList<>();
        BiConsumer<String, Integer> indexedBiConsumer = BiConsumers.indexed((idx, s, i) -> biResults.add(idx + ":" + s + "=" + i));
        indexedBiConsumer.accept("a", 1);
        indexedBiConsumer.accept("b", 2);
        assertEquals(2, biResults.size());
        assertEquals("0:a=1", biResults.get(0));
        assertEquals("1:b=2", biResults.get(1));
    }

    @Test
    public void indexed() {
        assertThat(Fn.indexed().apply("a").index()).isZero();
    }

    @Test
    public void testApply() {
        // Tests the anonymous Function.apply() in indexed() -> Indexed
        Function<String, Indexed<String>> indexedFunc = Fn.indexed();
        Indexed<String> r0 = indexedFunc.apply("first");
        assertEquals(0, r0.index());
        assertEquals("first", r0.value());
        Indexed<String> r1 = indexedFunc.apply("second");
        assertEquals(1, r1.index());
        assertEquals("second", r1.value());

        // Tests the anonymous BiFunction.apply() in alternate() -> MergeResult
        BiFunction<String, String, MergeResult> altFunc = Fn.alternate();
        assertEquals(MergeResult.TAKE_FIRST, altFunc.apply("a", "b"));
        assertEquals(MergeResult.TAKE_SECOND, altFunc.apply("a", "b"));
        assertEquals(MergeResult.TAKE_FIRST, altFunc.apply("a", "b"));

        // Tests the anonymous BinaryOperator.apply() in minByKey/maxByKey/minByValue/maxByValue
        BinaryOperator<Map.Entry<String, Integer>> minByKeyOp = Fn.minByKey();
        Map.Entry<String, Integer> entryA = new AbstractMap.SimpleEntry<>("a", 2);
        Map.Entry<String, Integer> entryB = new AbstractMap.SimpleEntry<>("b", 1);
        assertEquals(entryA, minByKeyOp.apply(entryA, entryB));
        assertEquals(entryA, minByKeyOp.apply(entryB, entryA));

        BinaryOperator<Map.Entry<String, Integer>> maxByKeyOp = Fn.maxByKey();
        assertEquals(entryB, maxByKeyOp.apply(entryA, entryB));
        assertEquals(entryB, maxByKeyOp.apply(entryB, entryA));

        BinaryOperator<Map.Entry<String, Integer>> minByValueOp = Fn.minByValue();
        assertEquals(entryB, minByValueOp.apply(entryA, entryB));
        assertEquals(entryB, minByValueOp.apply(entryB, entryA));

        BinaryOperator<Map.Entry<String, Integer>> maxByValueOp = Fn.maxByValue();
        assertEquals(entryA, maxByValueOp.apply(entryA, entryB));
        assertEquals(entryA, maxByValueOp.apply(entryB, entryA));

        // Tests Functions.indexed() anonymous apply()
        Function<String, String> indexedFuncFn = Functions.indexed((idx, s) -> idx + ":" + s);
        assertEquals("0:hello", indexedFuncFn.apply("hello"));
        assertEquals("1:world", indexedFuncFn.apply("world"));

        // Tests BiFunctions.indexed() anonymous apply()
        BiFunction<String, Integer, String> indexedBiFunc = BiFunctions.indexed((idx, s, i) -> idx + ":" + s + "=" + i);
        assertEquals("0:a=1", indexedBiFunc.apply("a", 1));
        assertEquals("1:b=2", indexedBiFunc.apply("b", 2));

        // Tests FC.alternate() anonymous apply()
        CharBiFunction<MergeResult> charAlt = FC.alternate();
        assertEquals(MergeResult.TAKE_FIRST, charAlt.apply('a', 'b'));
        assertEquals(MergeResult.TAKE_SECOND, charAlt.apply('a', 'b'));

        // Tests FB.alternate() anonymous apply()
        ByteBiFunction<MergeResult> byteAlt = FB.alternate();
        assertEquals(MergeResult.TAKE_FIRST, byteAlt.apply((byte) 1, (byte) 2));
        assertEquals(MergeResult.TAKE_SECOND, byteAlt.apply((byte) 1, (byte) 2));

        // Tests FS.alternate() anonymous apply()
        ShortBiFunction<MergeResult> shortAlt = FS.alternate();
        assertEquals(MergeResult.TAKE_FIRST, shortAlt.apply((short) 1, (short) 2));
        assertEquals(MergeResult.TAKE_SECOND, shortAlt.apply((short) 1, (short) 2));

        // Tests FI.alternate() anonymous apply() - already tested but need the name match
        IntBiFunction<MergeResult> intAlt = FI.alternate();
        assertEquals(MergeResult.TAKE_FIRST, intAlt.apply(1, 2));
        assertEquals(MergeResult.TAKE_SECOND, intAlt.apply(1, 2));

        // Tests FL.alternate() anonymous apply()
        LongBiFunction<MergeResult> longAlt = FL.alternate();
        assertEquals(MergeResult.TAKE_FIRST, longAlt.apply(1L, 2L));
        assertEquals(MergeResult.TAKE_SECOND, longAlt.apply(1L, 2L));

        // Tests FF.alternate() anonymous apply()
        FloatBiFunction<MergeResult> floatAlt = FF.alternate();
        assertEquals(MergeResult.TAKE_FIRST, floatAlt.apply(1.0f, 2.0f));
        assertEquals(MergeResult.TAKE_SECOND, floatAlt.apply(1.0f, 2.0f));

        // Tests FD.alternate() anonymous apply()
        DoubleBiFunction<MergeResult> doubleAlt = FD.alternate();
        assertEquals(MergeResult.TAKE_FIRST, doubleAlt.apply(1.0, 2.0));
        assertEquals(MergeResult.TAKE_SECOND, doubleAlt.apply(1.0, 2.0));
    }

    @Test
    public void testTest() {
        // Tests anonymous Predicate.test() in Predicates.indexed()
        Predicate<String> indexedPred = Predicates.indexed((idx, s) -> idx % 2 == 0);
        assertTrue(indexedPred.test("a")); // idx=0, even -> true
        assertFalse(indexedPred.test("b")); // idx=1, odd -> false
        assertTrue(indexedPred.test("c")); // idx=2, even -> true

        // Tests anonymous BiPredicate.test() in BiPredicates.indexed()
        BiPredicate<String, Integer> indexedBiPred = BiPredicates.indexed((idx, s, i) -> idx == 0);
        assertTrue(indexedBiPred.test("a", 1)); // idx=0 -> true
        assertFalse(indexedBiPred.test("b", 2)); // idx=1 -> false

        // Tests anonymous Predicate.test() in Predicates.distinct()
        Predicate<String> distinctPred = Predicates.distinct();
        assertTrue(distinctPred.test("a"));
        assertTrue(distinctPred.test("b"));
        assertFalse(distinctPred.test("a")); // already seen

        // Tests anonymous Predicate.test() in Predicates.distinctBy()
        Predicate<String> distinctByPred = Predicates.distinctBy(String::length);
        assertTrue(distinctByPred.test("a")); // length=1, new
        assertFalse(distinctByPred.test("b")); // length=1, already seen
        assertTrue(distinctByPred.test("ab")); // length=2, new

        // Tests anonymous Predicate.test() in Predicates.concurrentDistinct()
        Predicate<String> concDistPred = Predicates.concurrentDistinct();
        assertTrue(concDistPred.test("x"));
        assertFalse(concDistPred.test("x"));
        assertTrue(concDistPred.test("y"));
        assertTrue(concDistPred.test(null));
        assertFalse(concDistPred.test(null));

        // Tests anonymous Predicate.test() in Predicates.concurrentDistinctBy()
        Predicate<String> concDistByPred = Predicates.concurrentDistinctBy(String::length);
        assertTrue(concDistByPred.test("a"));
        assertFalse(concDistByPred.test("b")); // same length
        assertTrue(concDistByPred.test("ab"));

        // Tests anonymous Predicate.test() in Predicates.skipRepeats()
        Predicate<String> skipRepeatsPred = Predicates.skipRepeats();
        assertTrue(skipRepeatsPred.test("a"));
        assertFalse(skipRepeatsPred.test("a"));
        assertTrue(skipRepeatsPred.test("b"));
        assertTrue(skipRepeatsPred.test("a")); // non-consecutive repeat is ok
    }

    @Test
    public void selectFirst() {
        assertThat(Fn.selectFirst().apply(1, 2)).isEqualTo(1);
    }

    // BiFunctions
    @Test
    public void testBiFunctions_selectFirst() {
        assertEquals("first", BiFunctions.<String, Integer> selectFirst().apply("first", 2));
    }

    @Test
    public void testSelectFirst() {
        Throwables.BinaryOperator<String, RuntimeException> selector = Fnn.selectFirst();
        Assertions.assertEquals("first", selector.apply("first", "second"));
        Assertions.assertEquals(null, selector.apply(null, "second"));
    }

    @Test
    public void selectSecond() {
        assertThat(Fn.selectSecond().apply(1, 2)).isEqualTo(2);
    }

    @Test
    public void testSelectSecond() {
        Throwables.BinaryOperator<String, RuntimeException> selector = Fnn.selectSecond();
        Assertions.assertEquals("second", selector.apply("first", "second"));
        Assertions.assertEquals(null, selector.apply("first", null));
    }

    @Test
    public void min() {
        assertThat(Fn.<Integer> min().apply(1, 2)).isEqualTo(1);
    }

    @Test
    public void testNullHandlingConsistency() {
        BinaryOperator<String> min = Fn.min();
        BinaryOperator<String> max = Fn.max();

        assertEquals("test", min.apply("test", null));
        assertEquals("test", min.apply(null, "test"));
        assertNull(min.apply(null, null));

        assertEquals("test", max.apply("test", null));
        assertEquals("test", max.apply(null, "test"));
        assertNull(max.apply(null, null));
    }

    @Test
    public void testMin_Comparator() {
        BinaryOperator<String> minOp = Fn.min(Comparator.naturalOrder());
        assertEquals("apple", minOp.apply("apple", "banana"));
        assertEquals("apple", minOp.apply("banana", "apple"));
    }

    @Test
    public void testMin() {
        Throwables.BinaryOperator<Integer, RuntimeException> minOp = Fnn.min();
        Assertions.assertEquals(5, minOp.apply(5, 10));
        Assertions.assertEquals(5, minOp.apply(10, 5));
        Assertions.assertEquals(5, minOp.apply(5, 5));
    }

    @Test
    public void testMinWithComparator() {
        Throwables.BinaryOperator<String, RuntimeException> minOp = Fnn.min(String.CASE_INSENSITIVE_ORDER);
        Assertions.assertEquals("apple", minOp.apply("apple", "Banana"));
        Assertions.assertEquals("apple", minOp.apply("Banana", "apple"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.min(null));
    }

    @Test
    public void testMinByFunction() {
        BinaryOperator<String> minByLength = Fn.minBy(String::length);
        assertEquals("ab", minByLength.apply("ab", "abcd"));
        assertEquals("ab", minByLength.apply("abcd", "ab"));
    }

    @Test
    public void testMinBy() {
        Throwables.BinaryOperator<String, RuntimeException> minOp = Fnn.minBy(String::length);
        Assertions.assertEquals("cat", minOp.apply("cat", "elephant"));
        Assertions.assertEquals("cat", minOp.apply("elephant", "cat"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.minBy(null));
    }

    @Test
    public void testMinByKey() {
        Map.Entry<Integer, String> entry1 = CommonUtil.newImmutableEntry(10, "ten");
        Map.Entry<Integer, String> entry2 = CommonUtil.newImmutableEntry(5, "five");

        Throwables.BinaryOperator<Map.Entry<Integer, String>, RuntimeException> minOp = Fnn.minByKey();
        Assertions.assertEquals(entry2, minOp.apply(entry1, entry2));
        Assertions.assertEquals(entry2, minOp.apply(entry2, entry1));
    }

    @Test
    public void testMinByValue() {
        Map.Entry<String, Integer> entry1 = CommonUtil.newImmutableEntry("ten", 10);
        Map.Entry<String, Integer> entry2 = CommonUtil.newImmutableEntry("five", 5);

        Throwables.BinaryOperator<Map.Entry<String, Integer>, RuntimeException> minOp = Fnn.minByValue();
        Assertions.assertEquals(entry2, minOp.apply(entry1, entry2));
        Assertions.assertEquals(entry2, minOp.apply(entry2, entry1));
    }

    @Test
    public void max() {
        assertThat(Fn.<Integer> max().apply(1, 2)).isEqualTo(2);
    }

    @Test
    public void testMax_Comparator() {
        BinaryOperator<String> maxOp = Fn.max(Comparator.naturalOrder());
        assertEquals("banana", maxOp.apply("apple", "banana"));
        assertEquals("banana", maxOp.apply("banana", "apple"));
    }

    @Test
    public void testMax() {
        Throwables.BinaryOperator<Integer, RuntimeException> maxOp = Fnn.max();
        Assertions.assertEquals(10, maxOp.apply(5, 10));
        Assertions.assertEquals(10, maxOp.apply(10, 5));
        Assertions.assertEquals(5, maxOp.apply(5, 5));
    }

    @Test
    public void testMaxWithComparator() {
        Throwables.BinaryOperator<String, RuntimeException> maxOp = Fnn.max(String.CASE_INSENSITIVE_ORDER);
        Assertions.assertEquals("Banana", maxOp.apply("apple", "Banana"));
        Assertions.assertEquals("Banana", maxOp.apply("Banana", "apple"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.max(null));
    }

    @Test
    public void testMaxByFunction() {
        BinaryOperator<String> maxByLength = Fn.maxBy(String::length);
        assertEquals("abcd", maxByLength.apply("ab", "abcd"));
        assertEquals("abcd", maxByLength.apply("abcd", "ab"));
    }

    @Test
    public void testMaxBy() {
        Throwables.BinaryOperator<String, RuntimeException> maxOp = Fnn.maxBy(String::length);
        Assertions.assertEquals("elephant", maxOp.apply("cat", "elephant"));
        Assertions.assertEquals("elephant", maxOp.apply("elephant", "cat"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.maxBy(null));
    }

    @Test
    public void testMaxByKey() {
        Map.Entry<Integer, String> entry1 = CommonUtil.newImmutableEntry(10, "ten");
        Map.Entry<Integer, String> entry2 = CommonUtil.newImmutableEntry(5, "five");

        Throwables.BinaryOperator<Map.Entry<Integer, String>, RuntimeException> maxOp = Fnn.maxByKey();
        Assertions.assertEquals(entry1, maxOp.apply(entry1, entry2));
        Assertions.assertEquals(entry1, maxOp.apply(entry2, entry1));
    }

    @Test
    public void testMaxByValue() {
        Map.Entry<String, Integer> entry1 = CommonUtil.newImmutableEntry("ten", 10);
        Map.Entry<String, Integer> entry2 = CommonUtil.newImmutableEntry("five", 5);

        Throwables.BinaryOperator<Map.Entry<String, Integer>, RuntimeException> maxOp = Fnn.maxByValue();
        Assertions.assertEquals(entry1, maxOp.apply(entry1, entry2));
        Assertions.assertEquals(entry1, maxOp.apply(entry2, entry1));
    }

    @Test
    public void testCompareTo() {
        Function<Integer, Integer> compareToFive = Fn.compareTo(5);

        assertTrue(compareToFive.apply(3) < 0);
        assertEquals(0, compareToFive.apply(5));
        assertTrue(compareToFive.apply(7) > 0);

        assertTrue(compareToFive.apply(null) < 0);
    }

    @Test
    public void testCompareToWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Function<String, Integer> compareToHello = Fn.compareTo("hello", lengthComparator);

        assertTrue(compareToHello.apply("hi") < 0);
        assertEquals(0, compareToHello.apply("world"));
        assertTrue(compareToHello.apply("goodbye") > 0);

        Function<Integer, Integer> compareToFive = Fn.compareTo(5, null);
        assertEquals(0, compareToFive.apply(5));
    }

    @Test
    public void testCompare() {
        assertEquals(0, Fn.<Integer> compare().apply(5, 5));
        assertTrue(Fn.<Integer> compare().apply(10, 5) > 0);
        assertTrue(Fn.<Integer> compare(Comparator.reverseOrder()).apply(10, 5) < 0);
    }

    @Test
    public void testCompareNoArg() {
        com.landawn.abacus.util.function.BiFunction<Integer, Integer, Integer> cmp = Fn.compare();
        assertTrue(cmp.apply(1, 2) < 0);
        assertEquals(0, (int) cmp.apply(3, 3));
        assertTrue(cmp.apply(5, 2) > 0);
    }

    @Test
    public void testCompareWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        BiFunction<String, String, Integer> compareByLength = Fn.compare(lengthComparator);

        assertTrue(compareByLength.apply("hi", "hello") < 0);
        assertEquals(0, compareByLength.apply("hello", "world"));
        assertTrue(compareByLength.apply("goodbye", "hi") > 0);

        BiFunction<Integer, Integer, Integer> compareNatural = Fn.compare(null);
        assertEquals(0, compareNatural.apply(5, 5));
    }

    @Test
    public void testFutureGetOrDefaultOnError() throws Exception {
        CompletableFuture<String> successFuture = CompletableFuture.completedFuture("success");
        Function<Future<String>, String> getOrDefault = Fn.futureGetOrDefaultOnError("default");
        assertEquals("success", getOrDefault.apply(successFuture));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("error"));
        assertEquals("default", getOrDefault.apply(failedFuture));
    }

    @Test
    public void testFutureGet() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> "done");
        assertEquals("done", Fn.<String> futureGet().apply(future));

        Future<String> failedFuture = executor.submit(() -> {
            throw new IOException("fail");
        });
        assertThrows(RuntimeException.class, () -> Fn.<String> futureGet().apply(failedFuture));
        executor.shutdown();
    }

    @Test
    public void testFrom() {
        java.util.function.Supplier<String> jdkSupplier = () -> "hello";
        Supplier<String> abacusSupplier = Fn.from(jdkSupplier);
        assertEquals("hello", abacusSupplier.get());

        java.util.function.Predicate<String> jdkPredicate = s -> s.length() > 3;
        Predicate<String> abacusPredicate = Fn.from(jdkPredicate);
        assertTrue(abacusPredicate.test("hello"));
        assertFalse(abacusPredicate.test("hi"));

        java.util.function.Function<String, Integer> jdkFunction = String::length;
        Function<String, Integer> abacusFunction = Fn.from(jdkFunction);
        assertEquals(Integer.valueOf(5), abacusFunction.apply("hello"));
    }

    @Test
    public void testFrom_BiPredicate() {
        java.util.function.BiPredicate<String, Integer> jdkPred = (s, n) -> s.length() == n;
        BiPredicate<String, Integer> abacusPred = Fn.from(jdkPred);
        assertTrue(abacusPred.test("hello", 5));
        assertFalse(abacusPred.test("hi", 5));
    }

    @Test
    public void testFrom_BiConsumer() {
        List<String> results = new ArrayList<>();
        java.util.function.BiConsumer<String, Integer> jdkCons = (s, n) -> results.add(s + n);
        BiConsumer<String, Integer> abacusCons = Fn.from(jdkCons);
        abacusCons.accept("val", 42);
        assertEquals("val42", results.get(0));
    }

    @Test
    public void testFrom_BiFunction() {
        java.util.function.BiFunction<String, Integer, String> jdkFunc = (s, n) -> s.repeat(n);
        BiFunction<String, Integer, String> abacusFunc = Fn.from(jdkFunc);
        assertEquals("abab", abacusFunc.apply("ab", 2));
    }

    @Test
    public void testFrom_BinaryOperator() {
        java.util.function.BinaryOperator<Integer> jdkOp = Integer::sum;
        BinaryOperator<Integer> abacusOp = Fn.from(jdkOp);
        assertEquals(Integer.valueOf(7), abacusOp.apply(3, 4));
    }

    @Test
    public void testFrom_IntFunction() {
        java.util.function.IntFunction<String> jdkFunc = n -> "num" + n;
        IntFunction<String> abacusFunc = Fn.from(jdkFunc);
        assertEquals("num42", abacusFunc.apply(42));
    }

    @Test
    public void testFrom_UnaryOperator() {
        java.util.function.UnaryOperator<String> jdkOp = String::toUpperCase;
        UnaryOperator<String> abacusOp = Fn.from(jdkOp);
        assertEquals("HELLO", abacusOp.apply("hello"));
    }

    @Test
    public void testFrom_Supplier_alreadyAbacus() {
        Supplier<String> abacusSupplier = () -> "abacus";
        Supplier<String> result = Fn.from(abacusSupplier);
        assertSame(abacusSupplier, result);
    }

    @Test
    public void testFrom_Predicate_alreadyAbacus() {
        Predicate<String> abacusPredicate = s -> !s.isEmpty();
        Predicate<String> result = Fn.from(abacusPredicate);
        assertSame(abacusPredicate, result);
    }

    @Test
    public void testFrom_IntFunction_alreadyAbacus() {
        IntFunction<String> abacusFunc = n -> "n=" + n;
        IntFunction<String> result = Fn.from(abacusFunc);
        assertSame(abacusFunc, result);
    }

    @Test
    public void testFromSupplier() {
        java.util.function.Supplier<String> javaSupplier = () -> "result";
        Throwables.Supplier<String, RuntimeException> throwableSupplier = Fnn.from(javaSupplier);
        Assertions.assertEquals("result", throwableSupplier.get());

    }

    @Test
    public void testFromIntFunction() {
        java.util.function.IntFunction<String> javaFunc = i -> "value" + i;
        Throwables.IntFunction<String, RuntimeException> throwableFunc = Fnn.from(javaFunc);
        Assertions.assertEquals("value5", throwableFunc.apply(5));
    }

    @Test
    public void testFromPredicate() {
        java.util.function.Predicate<String> javaPred = s -> s.length() > 3;
        Throwables.Predicate<String, RuntimeException> throwablePred = Fnn.from(javaPred);
        Assertions.assertTrue(throwablePred.test("hello"));
        Assertions.assertFalse(throwablePred.test("hi"));
    }

    @Test
    public void testFromBiPredicate() {
        java.util.function.BiPredicate<String, Integer> javaPred = (s, i) -> s.length() == i;
        Throwables.BiPredicate<String, Integer, RuntimeException> throwablePred = Fnn.from(javaPred);
        Assertions.assertTrue(throwablePred.test("hello", 5));
        Assertions.assertFalse(throwablePred.test("hello", 3));
    }

    @Test
    public void testFromConsumer() {
        final String[] result = { "" };
        java.util.function.Consumer<String> javaConsumer = s -> result[0] = s;
        Throwables.Consumer<String, RuntimeException> throwableConsumer = Fnn.from(javaConsumer);
        throwableConsumer.accept("test");
        Assertions.assertEquals("test", result[0]);
    }

    @Test
    public void testFromBiConsumer() {
        final String[] result = { "" };
        java.util.function.BiConsumer<String, Integer> javaBiConsumer = (s, i) -> result[0] = s + i;
        Throwables.BiConsumer<String, Integer, RuntimeException> throwableBiConsumer = Fnn.from(javaBiConsumer);
        throwableBiConsumer.accept("test", 123);
        Assertions.assertEquals("test123", result[0]);
    }

    @Test
    public void testFromFunction() {
        java.util.function.Function<String, Integer> javaFunc = String::length;
        Throwables.Function<String, Integer, RuntimeException> throwableFunc = Fnn.from(javaFunc);
        Assertions.assertEquals(5, throwableFunc.apply("hello"));
    }

    @Test
    public void testFromBiFunction() {
        java.util.function.BiFunction<String, Integer, String> javaBiFunc = (s, i) -> s + i;
        Throwables.BiFunction<String, Integer, String, RuntimeException> throwableBiFunc = Fnn.from(javaBiFunc);
        Assertions.assertEquals("test123", throwableBiFunc.apply("test", 123));
    }

    @Test
    public void testFromUnaryOperator() {
        java.util.function.UnaryOperator<String> javaOp = s -> s.toUpperCase();
        Throwables.UnaryOperator<String, RuntimeException> throwableOp = Fnn.from(javaOp);
        Assertions.assertEquals("HELLO", throwableOp.apply("hello"));
    }

    @Test
    public void testFromBinaryOperator() {
        java.util.function.BinaryOperator<String> javaOp = (a, b) -> a + b;
        Throwables.BinaryOperator<String, RuntimeException> throwableOp = Fnn.from(javaOp);
        Assertions.assertEquals("helloworld", throwableOp.apply("hello", "world"));
    }

    @Test
    public void testSWithFunction() {
        Function<String, Integer> func = String::length;
        Supplier<Integer> supplier = Fn.s("hello", func);
        assertEquals(5, supplier.get());
    }

    @Test
    public void testTypeInferenceHelpers() {
        var supplier = Fn.s(() -> "test");
        assertEquals("test", supplier.get());

        var predicate = Fn.p((String s) -> s.length() > 3);
        assertTrue(predicate.test("hello"));

        var consumer = Fn.c((String s) -> {
        });
        consumer.accept("test");

        var function = Fn.f((String s) -> s.length());
        assertEquals(5, function.apply("hello"));
    }

    @Test
    public void testSWithArgument() {
        Supplier<Integer> supplier = Fn.s("hello", String::length);
        assertEquals(5, supplier.get());
    }

    @Test
    public void testSSupplier() {
        Throwables.Supplier<String, RuntimeException> supplier = () -> "test";
        Assertions.assertSame(supplier, Fnn.s(supplier));
    }

    @Test
    public void testSWithPartialApplication() {
        Throwables.Function<String, String, RuntimeException> func = s -> s.toUpperCase();
        Throwables.Supplier<String, RuntimeException> supplier = Fnn.s("hello", func);
        Assertions.assertEquals("HELLO", supplier.get());
    }

    @Test
    public void testS_Supplier() {
        Throwables.Supplier<String, Exception> supplier = () -> "test";
        assertSame(supplier, Fnn.s(supplier));
    }

    @Test
    public void testS_WithFunction() {
        String input = "test";
        Throwables.Function<String, Integer, Exception> func = String::length;
        Throwables.Supplier<Integer, Exception> supplier = Fnn.s(input, func);

        try {
            assertEquals(4, supplier.get());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testP() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("a", 1);
        assertTrue(Entries.<String, Integer> p((k, v) -> k.equals("a") && v == 1).test(entry));
    }

    @Test
    public void testPPredicateWithArgument() {
        Predicate<String> predicate = Fn.p("test", String::equals);
        assertTrue(predicate.test("test"));
        assertFalse(predicate.test("other"));
    }

    @Test
    public void testPBiPredicateWithArgument() {
        BiPredicate<Integer, Integer> biPredicate = Fn.p(10, (base, a, b) -> a + b > base);
        assertTrue(biPredicate.test(6, 5));
        assertFalse(biPredicate.test(3, 4));
    }

    @Test
    public void testPPredicateWithTwoArguments() {
        Predicate<Integer> predicate = Fn.p(1, 10, (min, max, val) -> val >= min && val <= max);
        assertTrue(predicate.test(5));
        assertFalse(predicate.test(0));
        assertFalse(predicate.test(15));
    }

    @Test
    public void testPPredicate() {
        Throwables.Predicate<String, RuntimeException> pred = s -> s.length() > 3;
        Assertions.assertSame(pred, Fnn.p(pred));
    }

    @Test
    public void testPWithBiPredicate() {
        Throwables.BiPredicate<String, Integer, RuntimeException> biPred = (s, i) -> s.length() == i;
        Throwables.Predicate<Integer, RuntimeException> pred = Fnn.p("hello", biPred);
        Assertions.assertTrue(pred.test(5));
        Assertions.assertFalse(pred.test(3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.p("hello", (Throwables.BiPredicate) null));
    }

    @Test
    public void testPWithTriPredicate() {
        Throwables.TriPredicate<String, Integer, Boolean, RuntimeException> triPred = (s, i, b) -> s.length() == i && b;
        Throwables.Predicate<Boolean, RuntimeException> pred = Fnn.p("hello", 5, triPred);
        Assertions.assertTrue(pred.test(true));
        Assertions.assertFalse(pred.test(false));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.p("hello", 5, null));
    }

    @Test
    public void testPBiPredicate() {
        Throwables.BiPredicate<String, Integer, RuntimeException> biPred = (s, i) -> s.length() == i;
        Assertions.assertSame(biPred, Fnn.p(biPred));
    }

    @Test
    public void testPBiPredicateWithPartial() {
        Throwables.TriPredicate<String, Integer, Boolean, RuntimeException> triPred = (s, i, b) -> s.length() == i && b;
        Throwables.BiPredicate<Integer, Boolean, RuntimeException> biPred = Fnn.p("hello", triPred);
        Assertions.assertTrue(biPred.test(5, true));
        Assertions.assertFalse(biPred.test(5, false));
        Assertions.assertFalse(biPred.test(3, true));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Fnn.p("hello", (Throwables.TriPredicate<String, Integer, Boolean, RuntimeException>) null));
    }

    @Test
    public void testPTriPredicate() {
        Throwables.TriPredicate<String, Integer, Boolean, RuntimeException> triPred = (s, i, b) -> s.length() == i && b;
        Assertions.assertSame(triPred, Fnn.p(triPred));
    }

    @Test
    public void testPBiPredicateWithTriPredicate() {
        TriPredicate<String, String, Integer> triPred = (prefix, s, len) -> s.startsWith(prefix) && s.length() == len;
        BiPredicate<String, Integer> biPredicate = Fn.p("he", triPred);
        assertTrue(biPredicate.test("hello", 5));
        assertFalse(biPredicate.test("world", 5));

        assertThrows(IllegalArgumentException.class, () -> Fn.p("test", (TriPredicate<String, String, String>) null));
    }

    @Test
    public void testP_Predicate() {
        Throwables.Predicate<String, Exception> predicate = s -> s.length() > 3;
        assertSame(predicate, Fnn.p(predicate));
    }

    @Test
    public void testP_WithBiPredicate() {
        String fixed = "test";
        Throwables.BiPredicate<String, String, Exception> biPredicate = String::equals;
        Throwables.Predicate<String, Exception> predicate = Fnn.p(fixed, biPredicate);

        try {
            assertTrue(predicate.test("test"));
            assertFalse(predicate.test("other"));
        } catch (Exception e) {
            fail("Should not throw exception");
        }

        assertThrows(IllegalArgumentException.class, () -> Fnn.p("a", (Throwables.BiPredicate<String, String, Exception>) null));
    }

    @Test
    public void testP_WithTriPredicate() {
        String fixed1 = "a";
        String fixed2 = "b";
        Throwables.TriPredicate<String, String, String, Exception> triPredicate = (a, b, c) -> a.equals(fixed1) && b.equals(fixed2) && c.equals("c");
        Throwables.Predicate<String, Exception> predicate = Fnn.p(fixed1, fixed2, triPredicate);

        try {
            assertTrue(predicate.test("c"));
            assertFalse(predicate.test("d"));
        } catch (Exception e) {
            fail("Should not throw exception");
        }

        assertThrows(IllegalArgumentException.class, () -> Fnn.p("a", "b", null));
    }

    @Test
    public void testC() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("a", 1);
        AtomicReference<String> ref = new AtomicReference<>();
        Entries.<String, Integer> c((k, v) -> ref.set(k + v)).accept(entry);
        assertEquals("a1", ref.get());
    }

    @Test
    public void testCConsumerWithArgument() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = Fn.c(results, (result, val) -> result.add(val));
        consumer.accept("test");
        assertEquals(1, results.size());
        assertEquals("test", results.get(0));
    }

    @Test
    public void testCConsumerWithTwoArguments() {
        List<String> results = new ArrayList<>();
        Consumer<Integer> consumer = Fn.c("prefix-", results, (prefix, list, val) -> list.add(prefix + val));
        consumer.accept(123);
        assertEquals(1, results.size());
        assertEquals("prefix-123", results.get(0));
    }

    @Test
    public void testCBiConsumerWithArgument() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> biConsumer = Fn.c(results, (list, s, i) -> list.add(s + ":" + i));
        biConsumer.accept("test", 5);
        assertEquals(1, results.size());
        assertEquals("test:5", results.get(0));
    }

    @Test
    public void testCConsumer() {
        Throwables.Consumer<String, RuntimeException> consumer = s -> {
        };
        Assertions.assertSame(consumer, Fnn.c(consumer));
    }

    @Test
    public void testCWithBiConsumer() {
        final String[] result = { "" };
        Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
        Throwables.Consumer<Integer, RuntimeException> consumer = Fnn.c("test", biConsumer);
        consumer.accept(123);
        Assertions.assertEquals("test123", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c("test", (Throwables.BiConsumer<String, Integer, RuntimeException>) null));
    }

    @Test
    public void testCWithTriConsumer() {
        final String[] result = { "" };
        Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> result[0] = s + i + b;
        Throwables.Consumer<Boolean, RuntimeException> consumer = Fnn.c("test", 123, triConsumer);
        consumer.accept(true);
        Assertions.assertEquals("test123true", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c("test", 123, null));
    }

    @Test
    public void testCBiConsumer() {
        Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> {
        };
        Assertions.assertSame(biConsumer, Fnn.c(biConsumer));
    }

    @Test
    public void testCBiConsumerWithPartial() {
        final String[] result = { "" };
        Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> result[0] = s + i + b;
        Throwables.BiConsumer<Integer, Boolean, RuntimeException> biConsumer = Fnn.c("test", triConsumer);
        biConsumer.accept(123, true);
        Assertions.assertEquals("test123true", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c("test", (Throwables.TriConsumer<String, Integer, Boolean, RuntimeException>) null));
    }

    @Test
    public void testCTriConsumer() {
        Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> {
        };
        Assertions.assertSame(triConsumer, Fnn.c(triConsumer));
    }

    @Test
    public void testC2jc() throws Exception {
        Throwables.Callable<String, RuntimeException> throwableCallable = () -> "result";
        java.util.concurrent.Callable<String> javaCallable = Fnn.c2jc(throwableCallable);

        Assertions.assertEquals("result", javaCallable.call());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2jc(null));
    }

    @Test
    public void testCBiConsumerWithTriConsumer() {
        StringBuilder sb = new StringBuilder();
        TriConsumer<StringBuilder, String, String> triConsumer = (builder, s1, s2) -> builder.append(s1).append(s2);
        BiConsumer<String, String> biConsumer = Fn.c(sb, triConsumer);
        biConsumer.accept("hello", " world");
        assertEquals("hello world", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> Fn.c("test", (TriConsumer<String, String, String>) null));
    }

    @Test
    public void testC_Consumer() {
        Throwables.Consumer<String, Exception> consumer = s -> {
        };
        assertSame(consumer, Fnn.c(consumer));
    }

    @Test
    public void testF() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("a", 1);
        assertEquals("a1", Entries.<String, Integer, String> f((k, v) -> k + v).apply(entry));
    }

    @Test
    public void testFFunctionWithArgument() {
        Function<Integer, String> function = Fn.f("test", (str, i) -> str + ":" + i);
        assertEquals("test:5", function.apply(5));
    }

    @Test
    public void testFFunctionWithTwoArguments() {
        Function<Boolean, String> function = Fn.f("hello", 5, (s, i, b) -> b ? s : String.valueOf(i));
        assertEquals("hello", function.apply(true));
        assertEquals("5", function.apply(false));
    }

    @Test
    public void testFBiFunctionWithArgument() {
        BiFunction<String, Integer, String> biFunction = Fn.f("-", (sep, s, i) -> s + sep + i);
        assertEquals("test-5", biFunction.apply("test", 5));
    }

    @Test
    public void testFI_greaterThan() {
        assertTrue(FI.greaterThan().test(3, 2));
        assertFalse(FI.greaterThan().test(2, 3));
        assertFalse(FI.greaterThan().test(2, 2));
    }

    @Test
    public void testFI_greaterThanOrEqual() {
        assertTrue(FI.greaterThanOrEqual().test(3, 2));
        assertTrue(FI.greaterThanOrEqual().test(2, 2));
        assertFalse(FI.greaterThanOrEqual().test(1, 2));
    }

    @Test
    public void testFI_lessThan() {
        assertTrue(FI.lessThan().test(1, 2));
        assertFalse(FI.lessThan().test(2, 1));
        assertFalse(FI.lessThan().test(2, 2));
    }

    @Test
    public void testFI_lessThanOrEqual() {
        assertTrue(FI.lessThanOrEqual().test(1, 2));
        assertTrue(FI.lessThanOrEqual().test(2, 2));
        assertFalse(FI.lessThanOrEqual().test(3, 2));
    }

    @Test
    public void testFI_p() {
        assertTrue(FI.p(n -> n > 0).test(5));
        assertFalse(FI.p(n -> n > 0).test(-1));
    }

    @Test
    public void testFI_f() {
        assertEquals("42", FI.f(n -> String.valueOf(n)).apply(42));
    }

    @Test
    public void testFI_c() {
        AtomicInteger holder = new AtomicInteger();
        FI.c(n -> holder.set(n)).accept(99);
        assertEquals(99, holder.get());
    }

    @Test
    public void testFL_equal() {
        assertTrue(FL.equal().test(5L, 5L));
        assertFalse(FL.equal().test(5L, 6L));
    }

    @Test
    public void testFL_notEqual() {
        assertTrue(FL.notEqual().test(1L, 2L));
        assertFalse(FL.notEqual().test(1L, 1L));
    }

    @Test
    public void testFL_greaterThan() {
        assertTrue(FL.greaterThan().test(3L, 2L));
        assertFalse(FL.greaterThan().test(2L, 3L));
    }

    @Test
    public void testFL_greaterThanOrEqual() {
        assertTrue(FL.greaterThanOrEqual().test(3L, 3L));
        assertFalse(FL.greaterThanOrEqual().test(2L, 3L));
    }

    @Test
    public void testFL_lessThan() {
        assertTrue(FL.lessThan().test(1L, 2L));
        assertFalse(FL.lessThan().test(2L, 2L));
    }

    @Test
    public void testFL_lessThanOrEqual() {
        assertTrue(FL.lessThanOrEqual().test(2L, 2L));
        assertFalse(FL.lessThanOrEqual().test(3L, 2L));
    }

    @Test
    public void testFL_p() {
        assertTrue(FL.p(n -> n > 0L).test(5L));
        assertFalse(FL.p(n -> n > 0L).test(-1L));
    }

    @Test
    public void testFL_f() {
        assertEquals("42", FL.f(n -> String.valueOf(n)).apply(42L));
    }

    @Test
    public void testFL_c() {
        AtomicInteger holder = new AtomicInteger();
        FL.c(n -> holder.set((int) n)).accept(7L);
        assertEquals(7, holder.get());
    }

    @Test
    public void testFL_sum() {
        assertEquals(6L, (long) FL.sum().apply(new long[] { 1L, 2L, 3L }));
    }

    @Test
    public void testFL_average() {
        assertEquals(2.0, FL.average().apply(new long[] { 1L, 2L, 3L }), 0.001);
    }

    @Test
    public void testFD_equal() {
        assertTrue(FD.equal().test(1.0, 1.0));
        assertFalse(FD.equal().test(1.0, 2.0));
    }

    @Test
    public void testFD_notEqual() {
        assertTrue(FD.notEqual().test(1.0, 2.0));
        assertFalse(FD.notEqual().test(1.0, 1.0));
    }

    @Test
    public void testFD_greaterThan() {
        assertTrue(FD.greaterThan().test(3.0, 2.0));
        assertFalse(FD.greaterThan().test(2.0, 3.0));
    }

    @Test
    public void testFD_greaterThanOrEqual() {
        assertTrue(FD.greaterThanOrEqual().test(2.0, 2.0));
        assertFalse(FD.greaterThanOrEqual().test(1.0, 2.0));
    }

    @Test
    public void testFD_lessThan() {
        assertTrue(FD.lessThan().test(1.0, 2.0));
        assertFalse(FD.lessThan().test(2.0, 1.0));
    }

    @Test
    public void testFD_lessThanOrEqual() {
        assertTrue(FD.lessThanOrEqual().test(2.0, 2.0));
        assertFalse(FD.lessThanOrEqual().test(3.0, 2.0));
    }

    @Test
    public void testFD_p() {
        assertTrue(FD.p(n -> n > 0.0).test(5.0));
        assertFalse(FD.p(n -> n > 0.0).test(-1.0));
    }

    @Test
    public void testFD_f() {
        assertEquals("3.14", FD.f(n -> String.valueOf(n)).apply(3.14));
    }

    @Test
    public void testFD_c() {
        AtomicInteger holder = new AtomicInteger();
        FD.c(n -> holder.set((int) n)).accept(7.0);
        assertEquals(7, holder.get());
    }

    @Test
    public void testFD_sum() {
        assertEquals(6.0, FD.sum().apply(new double[] { 1.0, 2.0, 3.0 }), 0.001);
    }

    @Test
    public void testFD_average() {
        assertEquals(2.0, FD.average().apply(new double[] { 1.0, 2.0, 3.0 }), 0.001);
    }

    @Test
    public void testFS_equal() {
        assertTrue(FS.equal().test((short) 5, (short) 5));
        assertFalse(FS.equal().test((short) 5, (short) 6));
    }

    @Test
    public void testFS_notEqual() {
        assertTrue(FS.notEqual().test((short) 1, (short) 2));
        assertFalse(FS.notEqual().test((short) 1, (short) 1));
    }

    @Test
    public void testFS_greaterThan() {
        assertTrue(FS.greaterThan().test((short) 3, (short) 2));
        assertFalse(FS.greaterThan().test((short) 2, (short) 3));
    }

    @Test
    public void testFS_greaterThanOrEqual() {
        assertTrue(FS.greaterThanOrEqual().test((short) 2, (short) 2));
        assertFalse(FS.greaterThanOrEqual().test((short) 1, (short) 2));
    }

    @Test
    public void testFS_lessThan() {
        assertTrue(FS.lessThan().test((short) 1, (short) 2));
        assertFalse(FS.lessThan().test((short) 2, (short) 2));
    }

    @Test
    public void testFS_lessThanOrEqual() {
        assertTrue(FS.lessThanOrEqual().test((short) 2, (short) 2));
        assertFalse(FS.lessThanOrEqual().test((short) 3, (short) 2));
    }

    @Test
    public void testFS_p() {
        assertTrue(FS.p(n -> n > 0).test((short) 5));
        assertFalse(FS.p(n -> n > 0).test((short) -1));
    }

    @Test
    public void testFS_f() {
        assertEquals("42", FS.f(n -> String.valueOf(n)).apply((short) 42));
    }

    @Test
    public void testFS_c() {
        AtomicInteger holder = new AtomicInteger();
        FS.c(n -> holder.set(n)).accept((short) 9);
        assertEquals(9, holder.get());
    }

    @Test
    public void testFS_sum() {
        assertEquals(6, (int) FS.sum().apply(new short[] { 1, 2, 3 }));
    }

    @Test
    public void testFS_average() {
        assertEquals(2.0, FS.average().apply(new short[] { 1, 2, 3 }), 0.001);
    }

    @Test
    public void testComplexScenarios() {
        TriFunction<String, String, String, String> triFunc = (a, b, c) -> a + b + c;
        BiFunction<String, String, String> biFunc = Fn.f("Hello ", triFunc);
        Function<String, String> func = Fn.f("World", biFunc);
        assertEquals("Hello World!", func.apply("!"));

        BiPredicate<String, String> nullSafeBiPred = (s1, s2) -> s1 != null && s2 != null && s1.equals(s2);
        Predicate<String> pred = Fn.p(null, nullSafeBiPred);
        assertFalse(pred.test("test"));
        assertFalse(pred.test(null));
    }

    // FL (Long) methods
    @Test
    public void testFL_positive() {
        assertTrue(FL.positive().test(1L));
        assertFalse(FL.positive().test(0L));
        assertFalse(FL.positive().test(-1L));
    }

    @Test
    public void testFL_notNegative() {
        assertTrue(FL.notNegative().test(1L));
        assertTrue(FL.notNegative().test(0L));
        assertFalse(FL.notNegative().test(-1L));
    }

    @Test
    public void testFL_len() {
        assertEquals(3, (int) FL.len().apply(new long[] { 1L, 2L, 3L }));
        assertEquals(0, (int) FL.len().apply(null));
    }

    // FD (Double) methods
    @Test
    public void testFD_positive() {
        assertTrue(FD.positive().test(1.0));
        assertFalse(FD.positive().test(0.0));
        assertFalse(FD.positive().test(-1.0));
    }

    @Test
    public void testFD_notNegative() {
        assertTrue(FD.notNegative().test(0.0));
        assertFalse(FD.notNegative().test(-1.0));
    }

    @Test
    public void testFD_len() {
        assertEquals(3, (int) FD.len().apply(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals(0, (int) FD.len().apply(null));
    }

    // FS (Short) methods
    @Test
    public void testFS_positive() {
        assertTrue(FS.positive().test((short) 1));
        assertFalse(FS.positive().test((short) 0));
        assertFalse(FS.positive().test((short) -1));
    }

    @Test
    public void testFS_notNegative() {
        assertTrue(FS.notNegative().test((short) 0));
        assertFalse(FS.notNegative().test((short) -1));
    }

    @Test
    public void testFS_len() {
        assertEquals(3, (int) FS.len().apply(new short[] { 1, 2, 3 }));
        assertEquals(0, (int) FS.len().apply(null));
    }

    @Test
    public void testFFunction() {
        Throwables.Function<String, Integer, RuntimeException> func = String::length;
        Assertions.assertSame(func, Fnn.f(func));
    }

    @Test
    public void testFWithBiFunction() {
        Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
        Throwables.Function<Integer, String, RuntimeException> func = Fnn.f("test", biFunc);
        Assertions.assertEquals("test123", func.apply(123));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.f("test", (Throwables.BiFunction) null));
    }

    @Test
    public void testFWithTriFunction() {
        Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> triFunc = (s, i, b) -> s + i + b;
        Throwables.Function<Boolean, String, RuntimeException> func = Fnn.f("test", 123, triFunc);
        Assertions.assertEquals("test123true", func.apply(true));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.f("test", 123, null));
    }

    @Test
    public void testFBiFunction() {
        Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
        Assertions.assertSame(biFunc, Fnn.f(biFunc));
    }

    @Test
    public void testFBiFunctionWithPartial() {
        Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> triFunc = (s, i, b) -> s + i + b;
        Throwables.BiFunction<Integer, Boolean, String, RuntimeException> biFunc = Fnn.f("test", triFunc);
        Assertions.assertEquals("test123true", biFunc.apply(123, true));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Fnn.f("test", (Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException>) null));
    }

    @Test
    public void testFTriFunction() {
        Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> triFunc = (s, i, b) -> s + i + b;
        Assertions.assertSame(triFunc, Fnn.f(triFunc));
    }

    @Test
    public void testFBiFunctionWithTriFunction() {
        TriFunction<String, Integer, Integer, String> triFunction = (s, start, end) -> s.substring(start, end);
        BiFunction<Integer, Integer, String> biFunction = Fn.f("hello world", triFunction);
        assertEquals("hello", biFunction.apply(0, 5));

        assertThrows(IllegalArgumentException.class, () -> Fn.f("test", (TriFunction<String, String, String, String>) null));
    }

    @Test
    public void testF_Function() {
        Throwables.Function<String, Integer, Exception> function = String::length;
        assertSame(function, Fnn.f(function));
    }

    @Test
    public void testOUnaryOperator() {
        Throwables.UnaryOperator<String, RuntimeException> op = s -> s.toUpperCase();
        Assertions.assertSame(op, Fnn.o(op));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.o((Throwables.UnaryOperator<String, RuntimeException>) null));
    }

    @Test
    public void testOBinaryOperator() {
        Throwables.BinaryOperator<String, RuntimeException> op = (a, b) -> a + b;
        Assertions.assertSame(op, Fnn.o(op));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.o((Throwables.BinaryOperator<String, RuntimeException>) null));
    }

    @Test
    public void testO_UnaryOperator() {
        Throwables.UnaryOperator<String, Exception> operator = s -> s.toUpperCase();
        assertSame(operator, Fnn.o(operator));

        assertThrows(IllegalArgumentException.class, () -> Fnn.o((Throwables.UnaryOperator<?, ?>) null));
    }

    @Test
    public void testO_BinaryOperator() {
        Throwables.BinaryOperator<Integer, Exception> operator = Integer::sum;
        assertSame(operator, Fnn.o(operator));

        assertThrows(IllegalArgumentException.class, () -> Fnn.o((Throwables.BinaryOperator<?, ?>) null));
    }

    @Test
    public void testMc() {
        Throwables.BiConsumer<String, java.util.function.Consumer<Character>, RuntimeException> mapper = (s, consumer) -> {
            for (char c : s.toCharArray()) {
                consumer.accept(c);
            }
        };

        Throwables.BiConsumer<String, java.util.function.Consumer<Character>, RuntimeException> result = Fnn.mc(mapper);
        Assertions.assertSame(mapper, result);

        java.util.List<Character> chars = new java.util.ArrayList<>();
        result.accept("test", chars::add);
        Assertions.assertEquals(java.util.Arrays.asList('t', 'e', 's', 't'), chars);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.mc(null));
    }

    @Test
    public void testSs() {
        Throwables.Supplier<String, IOException> supplier = () -> {
            throw new IOException("e");
        };
        assertThrows(RuntimeException.class, () -> Fn.ss(supplier).get());

        Throwables.Function<String, Integer, IOException> func = s -> {
            throw new IOException("e");
        };
        assertThrows(RuntimeException.class, () -> Fn.ss("a", func).get());
    }

    @Test
    public void testSsThrowableSupplier() {
        Throwables.Supplier<String, IOException> throwableSupplier = () -> "test";
        Supplier<String> supplier = Fn.ss(throwableSupplier);
        assertEquals("test", supplier.get());

        Throwables.Supplier<String, IOException> exceptionSupplier = () -> {
            throw new IOException("error");
        };
        Supplier<String> errorSupplier = Fn.ss(exceptionSupplier);
        assertThrows(RuntimeException.class, errorSupplier::get);

        assertThrows(IllegalArgumentException.class, () -> Fn.ss((Throwables.Supplier<String, Exception>) null));
    }

    @Test
    public void testSsWithFunction() {
        Throwables.Function<String, Integer, IOException> throwableFunc = String::length;
        Supplier<Integer> supplier = Fn.ss("hello", throwableFunc);
        assertEquals(5, supplier.get());

        Throwables.Function<String, Integer, IOException> exceptionFunc = s -> {
            throw new IOException("error");
        };
        Supplier<Integer> errorSupplier = Fn.ss("test", exceptionFunc);
        assertThrows(RuntimeException.class, errorSupplier::get);

        assertThrows(IllegalArgumentException.class, () -> Fn.ss("test", (Throwables.Function<String, String, Exception>) null));
    }

    @Test
    public void testSsWithArgument() {
        Supplier<Integer> supplier = Fn.ss("hello", s -> s.length());
        assertEquals(5, supplier.get());

        Supplier<Integer> throwingSupplier = Fn.ss("hello", s -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, throwingSupplier::get);
    }

    @Test
    public void testPpWithArgument() {
        Predicate<String> predicate = Fn.pp("test", (ref, s) -> ref.equals(s));
        assertTrue(predicate.test("test"));
        assertFalse(predicate.test("other"));
    }

    @Test
    public void testPpBiPredicateWithArgument() {
        BiPredicate<Integer, Integer> biPredicate = Fn.pp(10, (base, a, b) -> a + b > base);
        assertTrue(biPredicate.test(6, 5));
        assertFalse(biPredicate.test(3, 4));
    }

    // Entries.ff/pp/cc (Throwable wrappers)
    @Test
    public void testEntries_ff() {
        Map.Entry<String, Integer> entry = new java.util.AbstractMap.SimpleEntry<>("key", 1);
        Function<Map.Entry<String, Integer>, String> fn = Entries.ff((k, v) -> k + "=" + v);
        assertEquals("key=1", fn.apply(entry));
    }

    @Test
    public void testPpWithTwoArguments() {
        Predicate<Integer> predicate = Fn.pp(1, 10, (min, max, val) -> val >= min && val <= max);
        assertTrue(predicate.test(5));
        assertFalse(predicate.test(0));
    }

    @Test
    public void testPp() {
        Throwables.Predicate<String, IOException> predicate = s -> {
            throw new IOException("e");
        };
        assertThrows(RuntimeException.class, () -> Fn.pp(predicate).test("a"));

        Throwables.BiPredicate<String, String, IOException> biPredicate = (s1, s2) -> {
            throw new IOException("e");
        };
        assertThrows(RuntimeException.class, () -> Fn.pp("a", biPredicate).test("b"));
    }

    @Test
    public void testPpPredicate() {
        Predicate<String> pred = s -> s.length() > 3;
        Throwables.Predicate<String, RuntimeException> result = Fnn.pp(pred);
        Assertions.assertSame(pred, result);
        Assertions.assertTrue(result.test("hello"));
        Assertions.assertFalse(result.test("hi"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp((Predicate<String>) null));
    }

    @Test
    public void testPpWithBiPredicate() {
        java.util.function.BiPredicate<String, Integer> biPred = (s, i) -> s.length() == i;
        Throwables.Predicate<Integer, RuntimeException> pred = Fnn.pp("hello", biPred);
        Assertions.assertTrue(pred.test(5));
        Assertions.assertFalse(pred.test(3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp("hello", (java.util.function.BiPredicate<String, Integer>) null));
    }

    @Test
    public void testPpWithTriPredicate() {
        TriPredicate<String, Integer, Boolean> triPred = (s, i, b) -> s.length() == i && b;
        Throwables.Predicate<Boolean, RuntimeException> pred = Fnn.pp("hello", 5, triPred);
        Assertions.assertTrue(pred.test(true));
        Assertions.assertFalse(pred.test(false));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp("hello", 5, (TriPredicate<String, Integer, Boolean>) null));
    }

    @Test
    public void testPpBiPredicate() {
        BiPredicate<String, Integer> biPred = (s, i) -> s.length() == i;
        Throwables.BiPredicate<String, Integer, RuntimeException> result = Fnn.pp(biPred);
        Assertions.assertSame(biPred, result);
        Assertions.assertTrue(result.test("hello", 5));
        Assertions.assertFalse(result.test("hello", 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp((BiPredicate<String, Integer>) null));
    }

    @Test
    public void testPpBiPredicateWithPartial() {
        TriPredicate<String, Integer, Boolean> triPred = (s, i, b) -> s.length() == i && b;
        Throwables.BiPredicate<Integer, Boolean, RuntimeException> biPred = Fnn.pp("hello", triPred);
        Assertions.assertTrue(biPred.test(5, true));
        Assertions.assertFalse(biPred.test(5, false));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp("hello", (TriPredicate<String, Integer, Boolean>) null));
    }

    @Test
    public void testPpTriPredicate() {
        TriPredicate<String, Integer, Boolean> triPred = (s, i, b) -> s.length() == i && b;
        Throwables.TriPredicate<String, Integer, Boolean, RuntimeException> result = Fnn.pp(triPred);
        Assertions.assertSame(triPred, result);
        Assertions.assertTrue(result.test("hello", 5, true));
        Assertions.assertFalse(result.test("hello", 5, false));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp((TriPredicate<String, Integer, Boolean>) null));
    }

    @Test
    public void testPpThrowablePredicate() {
        Throwables.Predicate<String, IOException> throwablePred = s -> s.length() > 3;
        Predicate<String> predicate = Fn.pp(throwablePred);
        assertTrue(predicate.test("hello"));
        assertFalse(predicate.test("hi"));

        Throwables.Predicate<String, IOException> exceptionPred = s -> {
            throw new IOException("error");
        };
        Predicate<String> errorPredicate = Fn.pp(exceptionPred);
        assertThrows(RuntimeException.class, () -> errorPredicate.test("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp((Throwables.Predicate<String, Exception>) null));
    }

    @Test
    public void testPpThrowableBiPredicate() {
        Throwables.BiPredicate<String, Integer, IOException> throwableBiPred = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> biPredicate = Fn.pp(throwableBiPred);
        assertTrue(biPredicate.test("hello", 5));
        assertFalse(biPredicate.test("hello", 3));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp((Throwables.BiPredicate<String, String, Exception>) null));
    }

    @Test
    public void testPpBiPredicateWithTriPredicate() {
        Throwables.TriPredicate<String, String, Integer, IOException> throwableTriPred = (prefix, s, len) -> s.startsWith(prefix) && s.length() == len;
        BiPredicate<String, Integer> biPredicate = Fn.pp("he", throwableTriPred);
        assertTrue(biPredicate.test("hello", 5));
        assertFalse(biPredicate.test("world", 5));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp("test", (Throwables.TriPredicate<String, String, String, Exception>) null));
    }

    @Test
    public void testPpThrowableTriPredicate() {
        Throwables.TriPredicate<String, Integer, Boolean, IOException> throwableTriPred = (s, i, b) -> b && s.length() == i;
        TriPredicate<String, Integer, Boolean> triPredicate = Fn.pp(throwableTriPred);
        assertTrue(triPredicate.test("hello", 5, true));
        assertFalse(triPredicate.test("hello", 5, false));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp((Throwables.TriPredicate<String, String, String, Exception>) null));
    }

    @Test
    public void testCcWithArgument() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = Fn.cc(results, (list, s) -> list.add(s));
        consumer.accept("test");
        assertEquals(1, results.size());
    }

    @Test
    public void testCcWithTwoArguments() {
        List<String> results = new ArrayList<>();
        Consumer<Integer> consumer = Fn.cc("prefix-", results, (prefix, list, val) -> list.add(prefix + val));
        consumer.accept(123);
        assertEquals(1, results.size());
        assertEquals("prefix-123", results.get(0));
    }

    @Test
    public void testCcBiConsumerWithArgument() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> biConsumer = Fn.cc(results, (list, s, i) -> list.add(s + ":" + i));
        biConsumer.accept("test", 5);
        assertEquals(1, results.size());
    }

    @Test
    public void testCc() {
        Throwables.Consumer<String, IOException> consumer = s -> {
            throw new IOException("e");
        };
        assertThrows(RuntimeException.class, () -> Fn.cc(consumer).accept("a"));

        Throwables.Callable<String, IOException> callable = () -> {
            throw new IOException("e");
        };
        assertThrows(RuntimeException.class, () -> Fn.cc(callable).call());
    }

    @Test
    public void testCcConsumer() {
        Consumer<String> consumer = s -> {
        };
        Throwables.Consumer<String, RuntimeException> result = Fnn.cc(consumer);
        Assertions.assertSame(consumer, result);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc((Consumer<String>) null));
    }

    @Test
    public void testCcWithBiConsumer() {
        final String[] result = { "" };
        java.util.function.BiConsumer<String, Integer> biConsumer = (s, i) -> result[0] = s + i;
        Throwables.Consumer<Integer, RuntimeException> consumer = Fnn.cc("test", biConsumer);
        consumer.accept(123);
        Assertions.assertEquals("test123", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc("test", (java.util.function.BiConsumer<String, Integer>) null));
    }

    @Test
    public void testCcWithTriConsumer() {
        final String[] result = { "" };
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> result[0] = s + i + b;
        Throwables.Consumer<Boolean, RuntimeException> consumer = Fnn.cc("test", 123, triConsumer);
        consumer.accept(true);
        Assertions.assertEquals("test123true", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc("test", 123, (TriConsumer<String, Integer, Boolean>) null));
    }

    @Test
    public void testCcBiConsumer() {
        BiConsumer<String, Integer> biConsumer = (s, i) -> {
        };
        Throwables.BiConsumer<String, Integer, RuntimeException> result = Fnn.cc(biConsumer);
        Assertions.assertSame(biConsumer, result);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc((BiConsumer<String, Integer>) null));
    }

    @Test
    public void testCcBiConsumerWithPartial() {
        final String[] result = { "" };
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> result[0] = s + i + b;
        Throwables.BiConsumer<Integer, Boolean, RuntimeException> biConsumer = Fnn.cc("test", triConsumer);
        biConsumer.accept(123, true);
        Assertions.assertEquals("test123true", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc("test", (TriConsumer<String, Integer, Boolean>) null));
    }

    @Test
    public void testCcTriConsumer() {
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
        };
        Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> result = Fnn.cc(triConsumer);
        Assertions.assertSame(triConsumer, result);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc((TriConsumer<String, Integer, Boolean>) null));
    }

    @Test
    public void testCcThrowableConsumer() {
        List<String> list = new ArrayList<>();
        Throwables.Consumer<String, IOException> throwableConsumer = list::add;
        Consumer<String> consumer = Fn.cc(throwableConsumer);
        consumer.accept("test");
        assertEquals(1, list.size());

        Throwables.Consumer<String, IOException> exceptionConsumer = s -> {
            throw new IOException("error");
        };
        Consumer<String> errorConsumer = Fn.cc(exceptionConsumer);
        assertThrows(RuntimeException.class, () -> errorConsumer.accept("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.cc((Throwables.Consumer<String, Exception>) null));
    }

    @Test
    public void testCcThrowableBiConsumer() {
        Map<String, Integer> map = new HashMap<>();
        Throwables.BiConsumer<String, Integer, IOException> throwableBiConsumer = map::put;
        BiConsumer<String, Integer> biConsumer = Fn.cc(throwableBiConsumer);
        biConsumer.accept("key", 5);
        assertEquals(1, map.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.cc((Throwables.BiConsumer<String, String, Exception>) null));
    }

    @Test
    public void testCcBiConsumerWithTriConsumer() {
        StringBuilder sb = new StringBuilder();
        Throwables.TriConsumer<StringBuilder, String, String, IOException> throwableTriConsumer = (builder, s1, s2) -> builder.append(s1).append(s2);
        BiConsumer<String, String> biConsumer = Fn.cc(sb, throwableTriConsumer);
        biConsumer.accept("hello", " world");
        assertEquals("hello world", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> Fn.cc("test", (Throwables.TriConsumer<String, String, String, Exception>) null));
    }

    @Test
    public void testCcThrowableTriConsumer() {
        Throwables.TriConsumer<String, Integer, Boolean, IOException> throwableTriConsumer = (s, i, b) -> {
        };
        TriConsumer<String, Integer, Boolean> triConsumer = Fn.cc(throwableTriConsumer);
        assertNotNull(triConsumer);

        assertThrows(IllegalArgumentException.class, () -> Fn.cc((Throwables.TriConsumer<String, String, String, Exception>) null));
    }

    @Test
    public void testCcThrowableCallable() throws Exception {
        Throwables.Callable<String, IOException> throwableCallable = () -> "test";
        Callable<String> callable = Fn.cc(throwableCallable);
        assertEquals("test", callable.call());

        Throwables.Callable<String, IOException> exceptionCallable = () -> {
            throw new IOException("error");
        };
        Callable<String> errorCallable = Fn.cc(exceptionCallable);
        assertThrows(RuntimeException.class, errorCallable::call);
    }

    @Test
    public void testCc_Callable() {
        Callable<String> javaCallable = () -> "test";
        Throwables.Callable<String, Exception> throwableCallable = Fnn.cc(javaCallable);

        assertNotNull(throwableCallable);
    }

    @Test
    public void testCcCallable() {
        Callable<String> callable = Fn.cc(() -> "test");
        assertEquals("test", callable.call());

        Callable<String> throwingCallable = Fn.cc(() -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, throwingCallable::call);
    }

    @Test
    public void testFfWithArgument() {
        Function<Integer, String> function = Fn.ff("test", (str, i) -> str + ":" + i);
        assertEquals("test:5", function.apply(5));
    }

    @Test
    public void testFfWithTwoArguments() {
        Function<Boolean, String> function = Fn.ff("hello", 5, (s, i, b) -> b ? s : String.valueOf(i));
        assertEquals("hello", function.apply(true));
        assertEquals("5", function.apply(false));
    }

    @Test
    public void testFfBiFunctionWithArgument() {
        BiFunction<String, Integer, String> biFunction = Fn.ff("-", (sep, s, i) -> s + sep + i);
        assertEquals("test-5", biFunction.apply("test", 5));
    }

    @Test
    public void testFF_equal() {
        assertTrue(FF.equal().test(1.0f, 1.0f));
        assertFalse(FF.equal().test(1.0f, 2.0f));
    }

    @Test
    public void testFF_notEqual() {
        assertTrue(FF.notEqual().test(1.0f, 2.0f));
        assertFalse(FF.notEqual().test(1.0f, 1.0f));
    }

    @Test
    public void testFF_greaterThan() {
        assertTrue(FF.greaterThan().test(3.0f, 2.0f));
        assertFalse(FF.greaterThan().test(2.0f, 3.0f));
    }

    @Test
    public void testFF_greaterThanOrEqual() {
        assertTrue(FF.greaterThanOrEqual().test(2.0f, 2.0f));
        assertFalse(FF.greaterThanOrEqual().test(1.0f, 2.0f));
    }

    @Test
    public void testFF_lessThan() {
        assertTrue(FF.lessThan().test(1.0f, 2.0f));
        assertFalse(FF.lessThan().test(2.0f, 1.0f));
    }

    @Test
    public void testFF_lessThanOrEqual() {
        assertTrue(FF.lessThanOrEqual().test(2.0f, 2.0f));
        assertFalse(FF.lessThanOrEqual().test(3.0f, 2.0f));
    }

    @Test
    public void testFF_p() {
        assertTrue(FF.p(n -> n > 0.0f).test(5.0f));
        assertFalse(FF.p(n -> n > 0.0f).test(-1.0f));
    }

    @Test
    public void testFF_f() {
        assertEquals("3.14", FF.f(n -> String.valueOf(n)).apply(3.14f));
    }

    @Test
    public void testFF_c() {
        AtomicInteger holder = new AtomicInteger();
        FF.c(n -> holder.set((int) n)).accept(7.0f);
        assertEquals(7, holder.get());
    }

    @Test
    public void testFF_sum() {
        assertEquals(6.0f, FF.sum().apply(new float[] { 1.0f, 2.0f, 3.0f }), 0.001f);
    }

    @Test
    public void testFF_average() {
        assertEquals(2.0, FF.average().apply(new float[] { 1.0f, 2.0f, 3.0f }), 0.001);
    }

    // FF (Float) methods
    @Test
    public void testFF_positive() {
        assertTrue(FF.positive().test(1.0f));
        assertFalse(FF.positive().test(0.0f));
        assertFalse(FF.positive().test(-1.0f));
    }

    @Test
    public void testFF_notNegative() {
        assertTrue(FF.notNegative().test(0.0f));
        assertFalse(FF.notNegative().test(-1.0f));
    }

    @Test
    public void testFF_len() {
        assertEquals(3, (int) FF.len().apply(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals(0, (int) FF.len().apply(null));
    }

    @Test
    public void testFf() {
        Throwables.Function<String, Integer, IOException> func = s -> {
            throw new IOException("e");
        };
        assertThrows(RuntimeException.class, () -> Fn.ff(func).apply("a"));

        assertEquals(Integer.valueOf(-1), Fn.ff(func, -1).apply("a"));
    }

    @Test
    public void testFfFunction() {
        Function<String, Integer> func = String::length;
        Throwables.Function<String, Integer, RuntimeException> result = Fnn.ff(func);
        Assertions.assertSame(func, result);
        Assertions.assertEquals(5, result.apply("hello"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff((Function<String, Integer>) null));
    }

    @Test
    public void testFfWithBiFunction() {
        java.util.function.BiFunction<String, Integer, String> biFunc = (s, i) -> s + i;
        Throwables.Function<Integer, String, RuntimeException> func = Fnn.ff("test", biFunc);
        Assertions.assertEquals("test123", func.apply(123));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff("test", (java.util.function.BiFunction<String, Integer, String>) null));
    }

    @Test
    public void testFfWithTriFunction() {
        TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> s + i + b;
        Throwables.Function<Boolean, String, RuntimeException> func = Fnn.ff("test", 123, triFunc);
        Assertions.assertEquals("test123true", func.apply(true));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff("test", 123, (TriFunction<String, Integer, Boolean, String>) null));
    }

    @Test
    public void testFfBiFunction() {
        BiFunction<String, Integer, String> biFunc = (s, i) -> s + i;
        Throwables.BiFunction<String, Integer, String, RuntimeException> result = Fnn.ff(biFunc);
        Assertions.assertSame(biFunc, result);
        Assertions.assertEquals("test123", result.apply("test", 123));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff((BiFunction<String, Integer, String>) null));
    }

    @Test
    public void testFfBiFunctionWithPartial() {
        TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> s + i + b;
        Throwables.BiFunction<Integer, Boolean, String, RuntimeException> biFunc = Fnn.ff("test", triFunc);
        Assertions.assertEquals("test123true", biFunc.apply(123, true));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff("test", (TriFunction<String, Integer, Boolean, String>) null));
    }

    @Test
    public void testFfTriFunction() {
        TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> s + i + b;
        Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> result = Fnn.ff(triFunc);
        Assertions.assertSame(triFunc, result);
        Assertions.assertEquals("test123true", result.apply("test", 123, true));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff((TriFunction<String, Integer, Boolean, String>) null));
    }

    @Test
    public void testFfThrowableFunction() {
        Throwables.Function<String, Integer, IOException> throwableFunc = String::length;
        Function<String, Integer> function = Fn.ff(throwableFunc);
        assertEquals(5, function.apply("hello"));

        Throwables.Function<String, Integer, IOException> exceptionFunc = s -> {
            throw new IOException("error");
        };
        Function<String, Integer> errorFunction = Fn.ff(exceptionFunc);
        assertThrows(RuntimeException.class, () -> errorFunction.apply("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.Function<String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableFunctionWithDefault() {
        Throwables.Function<String, Integer, IOException> throwableFunc = String::length;
        Function<String, Integer> function = Fn.ff(throwableFunc, -1);
        assertEquals(5, function.apply("hello"));

        Throwables.Function<String, Integer, IOException> exceptionFunc = s -> {
            throw new IOException("error");
        };
        Function<String, Integer> errorFunction = Fn.ff(exceptionFunc, -1);
        assertEquals(-1, errorFunction.apply("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.Function<String, Integer, Exception>) null, -1));
    }

    @Test
    public void testFfThrowableBiFunction() {
        Throwables.BiFunction<String, Integer, Character, IOException> throwableBiFunc = (s, i) -> s.charAt(i);
        BiFunction<String, Integer, Character> biFunction = Fn.ff(throwableBiFunc);
        assertEquals('e', biFunction.apply("hello", 1));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.BiFunction<String, String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableBiFunctionWithDefault() {
        Throwables.BiFunction<String, Integer, Character, IOException> throwableBiFunc = (s, i) -> s.charAt(i);
        BiFunction<String, Integer, Character> biFunction = Fn.ff(throwableBiFunc, '?');
        assertEquals('e', biFunction.apply("hello", 1));

        Throwables.BiFunction<String, Integer, Character, IOException> exceptionFunc = (s, i) -> {
            throw new IOException("error");
        };
        BiFunction<String, Integer, Character> errorFunction = Fn.ff(exceptionFunc, '?');
        assertEquals('?', errorFunction.apply("test", 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.BiFunction<String, Integer, Character, Exception>) null, '?'));
    }

    @Test
    public void testFfBiFunctionWithTriFunction() {
        Throwables.TriFunction<String, Integer, Integer, String, IOException> throwableTriFunc = (s, start, end) -> s.substring(start, end);
        BiFunction<Integer, Integer, String> biFunction = Fn.ff("hello world", throwableTriFunc);
        assertEquals("hello", biFunction.apply(0, 5));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff("test", (Throwables.TriFunction<String, String, String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableTriFunction() {
        Throwables.TriFunction<String, Integer, Boolean, String, IOException> throwableTriFunc = (s, i, b) -> b ? s.substring(i) : s;
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.ff(throwableTriFunc);
        assertEquals("llo", triFunction.apply("hello", 2, true));
        assertEquals("hello", triFunction.apply("hello", 2, false));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.TriFunction<String, String, String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableTriFunctionWithDefault() {
        Throwables.TriFunction<String, Integer, Boolean, String, IOException> throwableTriFunc = (s, i, b) -> b ? s.substring(i) : s;
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.ff(throwableTriFunc, "default");
        assertEquals("llo", triFunction.apply("hello", 2, true));

        Throwables.TriFunction<String, Integer, Boolean, String, IOException> exceptionFunc = (s, i, b) -> {
            throw new IOException("error");
        };
        TriFunction<String, Integer, Boolean, String> errorFunction = Fn.ff(exceptionFunc, "default");
        assertEquals("default", errorFunction.apply("test", 0, true));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.TriFunction<String, Integer, Boolean, String, Exception>) null, "default"));
    }

    @Test
    public void testFfWithDefault() {
        Function<String, Integer> function = Fn.ff(s -> {
            if (s == null)
                throw new Exception("null");
            return s.length();
        }, -1);

        assertEquals(5, function.apply("hello"));
        assertEquals(-1, function.apply(null));
    }

    @Test
    public void testFfBiFunctionWithDefault() {
        BiFunction<String, Integer, String> biFunction = Fn.ff((s, i) -> {
            if (s == null)
                throw new Exception("null");
            return s + ":" + i;
        }, "default");

        assertEquals("test:5", biFunction.apply("test", 5));
        assertEquals("default", biFunction.apply(null, 5));
    }

    @Test
    public void testFfTriFunctionWithDefault() {
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.ff((s, i, b) -> {
            if (s == null)
                throw new Exception("null");
            return s + ":" + i + ":" + b;
        }, "default");

        assertEquals("test:5:true", triFunction.apply("test", 5, true));
        assertEquals("default", triFunction.apply(null, 5, true));
    }

    @Test
    public void testSp() {
        Object mutex = new Object();
        AtomicInteger counter = new AtomicInteger(0);
        Predicate<Integer> p = i -> {
            counter.incrementAndGet();
            N.sleep(10);
            return true;
        };
        Predicate<Integer> sp = Fn.sp(mutex, p);

        IntStream.range(0, 5).parallel().forEach(sp::test);
        assertEquals(5, counter.get());
    }

    @Test
    public void testSpPredicateWithArgument() {
        Object mutex = new Object();
        Predicate<String> predicate = Fn.sp(mutex, "test", String::equals);
        assertTrue(predicate.test("test"));
        assertFalse(predicate.test("other"));
    }

    @Test
    public void testSpTriConsumer() {
        Object mutex = new Object();
        List<String> results = new ArrayList<>();
        TriConsumer<String, Integer, Boolean> triConsumer = Fn.sc(mutex, (s, i, b) -> results.add(s + ":" + i + ":" + b));
        triConsumer.accept("test", 5, true);
        assertEquals(1, results.size());
    }

    @Test
    public void testSpPredicate() {
        final Object mutex = new Object();
        final int[] callCount = { 0 };
        Throwables.Predicate<String, RuntimeException> pred = s -> {
            callCount[0]++;
            return s.length() > 3;
        };

        Throwables.Predicate<String, RuntimeException> syncPred = Fnn.sp(mutex, pred);
        Assertions.assertTrue(syncPred.test("hello"));
        Assertions.assertEquals(1, callCount[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(null, pred));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(mutex, (Throwables.Predicate<String, RuntimeException>) null));
    }

    @Test
    public void testSpWithBiPredicate() {
        final Object mutex = new Object();
        Throwables.BiPredicate<String, Integer, RuntimeException> biPred = (s, i) -> s.length() == i;
        Throwables.Predicate<Integer, RuntimeException> syncPred = Fnn.sp(mutex, "hello", biPred);

        Assertions.assertTrue(syncPred.test(5));
        Assertions.assertFalse(syncPred.test(3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(null, "hello", biPred));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(mutex, "hello", null));
    }

    @Test
    public void testSpBiPredicate() {
        final Object mutex = new Object();
        Throwables.BiPredicate<String, Integer, RuntimeException> biPred = (s, i) -> s.length() == i;
        Throwables.BiPredicate<String, Integer, RuntimeException> syncBiPred = Fnn.sp(mutex, biPred);

        Assertions.assertTrue(syncBiPred.test("hello", 5));
        Assertions.assertFalse(syncBiPred.test("hello", 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(null, biPred));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(mutex, (Throwables.BiPredicate<String, Integer, RuntimeException>) null));
    }

    @Test
    public void testSpTriPredicate() {
        Object mutex = new Object();
        TriPredicate<String, Integer, Boolean> triPred = (s, i, b) -> b && s.length() == i;
        TriPredicate<String, Integer, Boolean> syncTriPred = Fn.sp(mutex, triPred);
        assertTrue(syncTriPred.test("hello", 5, true));
        assertFalse(syncTriPred.test("hello", 5, false));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(null, triPred));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(mutex, (TriPredicate<String, String, String>) null));
    }

    @Test
    public void testSc() {
        Object mutex = new Object();
        List<Integer> list = new ArrayList<>();
        Consumer<Integer> c = i -> {
            list.add(i);
            N.sleep(1);
        };
        Consumer<Integer> sc = Fn.sc(mutex, c);

        IntStream.range(0, 100).parallel().forEach(sc::accept);
        assertEquals(100, list.size());
    }

    @Test
    public void testScConsumerWithArgument() {
        Object mutex = new Object();
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = Fn.sc(mutex, results, List::add);
        consumer.accept("test");
        assertEquals(1, results.size());
    }

    @Test
    public void testScConsumer() {
        final Object mutex = new Object();
        final String[] result = { "" };
        Throwables.Consumer<String, RuntimeException> consumer = s -> result[0] = s;
        Throwables.Consumer<String, RuntimeException> syncConsumer = Fnn.sc(mutex, consumer);

        syncConsumer.accept("test");
        Assertions.assertEquals("test", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(null, consumer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(mutex, (Throwables.Consumer<String, RuntimeException>) null));
    }

    @Test
    public void testScWithBiConsumer() {
        final Object mutex = new Object();
        final String[] result = { "" };
        Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
        Throwables.Consumer<Integer, RuntimeException> syncConsumer = Fnn.sc(mutex, "test", biConsumer);

        syncConsumer.accept(123);
        Assertions.assertEquals("test123", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(null, "test", biConsumer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(mutex, "test", null));
    }

    @Test
    public void testScBiConsumer() {
        final Object mutex = new Object();
        final String[] result = { "" };
        Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
        Throwables.BiConsumer<String, Integer, RuntimeException> syncBiConsumer = Fnn.sc(mutex, biConsumer);

        syncBiConsumer.accept("test", 123);
        Assertions.assertEquals("test123", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(null, biConsumer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(mutex, (Throwables.BiConsumer<String, Integer, RuntimeException>) null));
    }

    @Test
    public void testScTriConsumer() {
        Object mutex = new Object();
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
        };
        TriConsumer<String, Integer, Boolean> syncTriConsumer = Fn.sc(mutex, triConsumer);
        assertNotNull(syncTriConsumer);

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(null, triConsumer));

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(mutex, (TriConsumer<String, String, String>) null));
    }

    @Test
    public void testSf() {
        Object mutex = new Object();
        List<Integer> list = new ArrayList<>();
        Function<Integer, Integer> f = i -> {
            list.add(i);
            N.sleep(1);
            return i;
        };
        Function<Integer, Integer> sf = Fn.sf(mutex, f);

        List<Integer> result = IntStream.range(0, 100).parallel().mapToObj(sf::apply).collect(Collectors.toList());
        assertEquals(100, list.size());
        assertEquals(100, result.size());
    }

    @Test
    public void testSfTriFunction() {
        Object mutex = new Object();
        TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> b ? s.substring(i) : s;
        TriFunction<String, Integer, Boolean, String> syncTriFunc = Fn.sf(mutex, triFunc);
        assertEquals("llo", syncTriFunc.apply("hello", 2, true));
        assertEquals("hello", syncTriFunc.apply("hello", 2, false));
    }

    @Test
    public void testSfFunctionWithArgument() {
        Object mutex = new Object();
        Function<Integer, String> function = Fn.sf(mutex, "test", (str, i) -> str + ":" + i);
        assertEquals("test:5", function.apply(5));
    }

    @Test
    public void testSfFunction() {
        final Object mutex = new Object();
        Throwables.Function<String, Integer, RuntimeException> func = String::length;
        Throwables.Function<String, Integer, RuntimeException> syncFunc = Fnn.sf(mutex, func);

        Assertions.assertEquals(5, syncFunc.apply("hello"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(null, func));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(mutex, (Throwables.Function<String, Integer, RuntimeException>) null));
    }

    @Test
    public void testSfWithBiFunction() {
        final Object mutex = new Object();
        Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
        Throwables.Function<Integer, String, RuntimeException> syncFunc = Fnn.sf(mutex, "test", biFunc);

        Assertions.assertEquals("test123", syncFunc.apply(123));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(null, "test", biFunc));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(mutex, "test", null));
    }

    @Test
    public void testSfBiFunction() {
        final Object mutex = new Object();
        Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
        Throwables.BiFunction<String, Integer, String, RuntimeException> syncBiFunc = Fnn.sf(mutex, biFunc);

        Assertions.assertEquals("test123", syncBiFunc.apply("test", 123));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(null, biFunc));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(mutex, (Throwables.BiFunction<String, Integer, String, RuntimeException>) null));
    }

    @Test
    public void testC2fWithReturn() {
        List<String> results = new ArrayList<>();
        Function<String, Integer> function = Fn.c2f(e -> results.add(e), 123);
        assertEquals(123, function.apply("test"));
        assertEquals(1, results.size());
    }

    @Test
    public void testC2f() {
        List<String> results = new ArrayList<>();
        Function<String, Void> function = Fn.c2f(e -> results.add(e));
        assertNull(function.apply("test"));
        assertEquals(1, results.size());
    }

    @Test
    public void testC2fConsumerToFunction() {
        final String[] result = { "" };
        Throwables.Consumer<String, RuntimeException> consumer = s -> result[0] = s;
        Throwables.Function<String, Void, RuntimeException> func = Fnn.c2f(consumer);

        Assertions.assertNull(func.apply("test"));
        Assertions.assertEquals("test", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<String, RuntimeException>) null));
    }

    @Test
    public void testC2fConsumerToFunctionWithValue() {
        final String[] result = { "" };
        Throwables.Consumer<String, RuntimeException> consumer = s -> result[0] = s;
        Throwables.Function<String, Integer, RuntimeException> func = Fnn.c2f(consumer, 42);

        Assertions.assertEquals(42, func.apply("test"));
        Assertions.assertEquals("test", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<String, RuntimeException>) null, 42));
    }

    @Test
    public void testC2fBiConsumerToBiFunction() {
        final String[] result = { "" };
        Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
        Throwables.BiFunction<String, Integer, Void, RuntimeException> biFunc = Fnn.c2f(biConsumer);

        Assertions.assertNull(biFunc.apply("test", 123));
        Assertions.assertEquals("test123", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.BiConsumer<String, Integer, RuntimeException>) null));
    }

    @Test
    public void testC2fBiConsumerToBiFunctionWithValue() {
        final String[] result = { "" };
        Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
        Throwables.BiFunction<String, Integer, Double, RuntimeException> biFunc = Fnn.c2f(biConsumer, 3.14);

        Assertions.assertEquals(3.14, biFunc.apply("test", 123));
        Assertions.assertEquals("test123", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.BiConsumer<String, Integer, RuntimeException>) null, 3.14));
    }

    @Test
    public void testC2fTriConsumerToTriFunction() {
        final String[] result = { "" };
        Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> result[0] = s + i + b;
        Throwables.TriFunction<String, Integer, Boolean, Void, RuntimeException> triFunc = Fnn.c2f(triConsumer);

        Assertions.assertNull(triFunc.apply("test", 123, true));
        Assertions.assertEquals("test123true", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.TriConsumer<String, Integer, Boolean, RuntimeException>) null));
    }

    @Test
    public void testC2fTriConsumerToTriFunctionWithValue() {
        final String[] result = { "" };
        Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> result[0] = s + i + b;
        Throwables.TriFunction<String, Integer, Boolean, Long, RuntimeException> triFunc = Fnn.c2f(triConsumer, 999L);

        Assertions.assertEquals(999L, triFunc.apply("test", 123, true));
        Assertions.assertEquals("test123true", result[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.TriConsumer<String, Integer, Boolean, RuntimeException>) null, 999L));
    }

    @Test
    public void testC2fConsumer() {
        List<String> list = new ArrayList<>();
        java.util.function.Consumer<String> consumer = list::add;
        Function<String, Void> function = Fn.c2f(consumer);
        assertNull(function.apply("test"));
        assertEquals(1, list.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((java.util.function.Consumer<String>) null));
    }

    @Test
    public void testC2fConsumerWithReturn() {
        List<String> list = new ArrayList<>();
        java.util.function.Consumer<String> consumer = list::add;
        Function<String, Integer> function = Fn.c2f(consumer, 42);
        assertEquals(42, function.apply("test"));
        assertEquals(1, list.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((java.util.function.Consumer<String>) null, 42));
    }

    @Test
    public void testC2fBiConsumer() {
        Map<String, Integer> map = new HashMap<>();
        java.util.function.BiConsumer<String, Integer> biConsumer = map::put;
        BiFunction<String, Integer, Void> biFunction = Fn.c2f(biConsumer);
        assertNull(biFunction.apply("key", 5));
        assertEquals(1, map.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((java.util.function.BiConsumer<String, Integer>) null));
    }

    @Test
    public void testC2fBiConsumerWithReturn() {
        Map<String, Integer> map = new HashMap<>();
        java.util.function.BiConsumer<String, Integer> biConsumer = map::put;
        BiFunction<String, Integer, String> biFunction = Fn.c2f(biConsumer, "done");
        assertEquals("done", biFunction.apply("key", 5));
        assertEquals(1, map.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((java.util.function.BiConsumer<String, Integer>) null, "done"));
    }

    @Test
    public void testC2fTriConsumer() {
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
        };
        TriFunction<String, Integer, Boolean, Void> triFunction = Fn.c2f(triConsumer);
        assertNull(triFunction.apply("test", 1, true));

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((TriConsumer<String, Integer, Boolean>) null));
    }

    @Test
    public void testC2fTriConsumerWithReturn() {
        StringBuilder sb = new StringBuilder();
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
            if (b)
                sb.append(s).append(i);
        };
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.c2f(triConsumer, "result");
        assertEquals("result", triFunction.apply("test", 1, true));
        assertEquals("test1", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((TriConsumer<String, Integer, Boolean>) null, "result"));
    }

    @Test
    public void testC2f_Consumer() throws Exception {
        List<String> consumed = new ArrayList<>();
        Throwables.Consumer<String, Exception> consumer = consumed::add;

        Throwables.Function<String, Void, Exception> function = Fnn.c2f(consumer);

        assertNull(function.apply("test"));
        assertEquals(Arrays.asList("test"), consumed);

        assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<?, ?>) null));
    }

    @Test
    public void testC2f_ConsumerWithReturn() throws Exception {
        List<String> consumed = new ArrayList<>();
        Throwables.Consumer<String, Exception> consumer = consumed::add;

        Throwables.Function<String, Integer, Exception> function = Fnn.c2f(consumer, 123);

        assertEquals(123, function.apply("test"));
        assertEquals(Arrays.asList("test"), consumed);

        assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<String, Exception>) null, 123));
    }

    @Test
    public void testF2cFunctionToConsumer() {
        Throwables.Function<String, Integer, RuntimeException> func = String::length;
        Throwables.Consumer<String, RuntimeException> consumer = Fnn.f2c(func);

        Assertions.assertDoesNotThrow(() -> consumer.accept("test"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.Function<String, Integer, RuntimeException>) null));
    }

    @Test
    public void testF2cBiFunctionToBiConsumer() {
        Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
        Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = Fnn.f2c(biFunc);

        Assertions.assertDoesNotThrow(() -> biConsumer.accept("test", 123));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.BiFunction<String, Integer, String, RuntimeException>) null));
    }

    @Test
    public void testF2cTriFunctionToTriConsumer() {
        Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> triFunc = (s, i, b) -> s + i + b;
        Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = Fnn.f2c(triFunc);

        Assertions.assertDoesNotThrow(() -> triConsumer.accept("test", 123, true));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Fnn.f2c((Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException>) null));
    }

    @Test
    public void testF2cFunction() {
        java.util.function.Function<String, Integer> func = String::length;
        Consumer<String> consumer = Fn.f2c(func);
        consumer.accept("hello");

        assertThrows(IllegalArgumentException.class, () -> Fn.f2c((java.util.function.Function<String, Integer>) null));
    }

    @Test
    public void testF2cBiFunction() {
        java.util.function.BiFunction<String, String, String> biFunc = String::concat;
        BiConsumer<String, String> biConsumer = Fn.f2c(biFunc);
        biConsumer.accept("hello", "world");

        assertThrows(IllegalArgumentException.class, () -> Fn.f2c((java.util.function.BiFunction<String, String, String>) null));
    }

    @Test
    public void testF2cTriFunction() {
        TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> b ? s.substring(i) : s;
        TriConsumer<String, Integer, Boolean> triConsumer = Fn.f2c(triFunc);
        triConsumer.accept("hello", 2, true);

        assertThrows(IllegalArgumentException.class, () -> Fn.f2c((TriFunction<String, Integer, Boolean, String>) null));
    }

    @Test
    public void testF2c() throws Exception {
        Throwables.Function<String, Integer, Exception> function = String::length;
        Throwables.Consumer<String, Exception> consumer = Fnn.f2c(function);

        consumer.accept("test");

        assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.Function<String, Integer, Exception>) null));
    }

    @Test
    public void testRr() {
        com.landawn.abacus.util.function.Runnable runnable = () -> {
        };
        Throwables.Runnable<RuntimeException> result = Fnn.rr(runnable);
        Assertions.assertSame(runnable, result);
    }

    @Test
    public void testRrThrowableRunnable() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.Runnable<IOException> throwableRunnable = () -> executed.set(true);
        Runnable runnable = Fn.rr(throwableRunnable);
        runnable.run();
        assertTrue(executed.get());

        Throwables.Runnable<IOException> exceptionRunnable = () -> {
            throw new IOException("error");
        };
        Runnable errorRunnable = Fn.rr(exceptionRunnable);
        assertThrows(RuntimeException.class, errorRunnable::run);
    }

    @Test
    public void testR() {
        Throwables.Runnable<Exception> runnable = () -> {
        };
        Assertions.assertSame(runnable, Fnn.r(runnable));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.r(null));
    }

    @Test
    public void testR2jr() {
        final boolean[] executed = { false };
        Throwables.Runnable<Exception> throwableRunnable = () -> {
            executed[0] = true;
        };
        java.lang.Runnable javaRunnable = Fnn.r2jr(throwableRunnable);

        javaRunnable.run();
        Assertions.assertTrue(executed[0]);

        Throwables.Runnable<Exception> throwingRunnable = () -> {
            throw new IOException("Test exception");
        };
        java.lang.Runnable wrappedRunnable = Fnn.r2jr(throwingRunnable);
        Assertions.assertThrows(RuntimeException.class, wrappedRunnable::run);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.r2jr(null));
    }

    @Test
    public void testJr() {
        java.lang.Runnable javaRunnable = () -> {
        };
        assertSame(javaRunnable, Fn.jr(javaRunnable));

        assertThrows(IllegalArgumentException.class, () -> Fn.jr(null));
    }

    @Test
    public void testJc() {
        java.util.concurrent.Callable<String> javaCallable = () -> "test";
        assertSame(javaCallable, Fn.jc(javaCallable));

        assertThrows(IllegalArgumentException.class, () -> Fn.jc(null));
    }

    @Test
    public void testR2cRunnableToCallable() {
        final boolean[] executed = { false };
        Throwables.Runnable<RuntimeException> runnable = () -> executed[0] = true;
        Throwables.Callable<Void, RuntimeException> callable = Fnn.r2c(runnable);

        Assertions.assertNull(callable.call());
        Assertions.assertTrue(executed[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.r2c(null));
    }

    @Test
    public void testR2cRunnableToCallableWithValue() {
        final boolean[] executed = { false };
        Throwables.Runnable<RuntimeException> runnable = () -> executed[0] = true;
        Throwables.Callable<String, RuntimeException> callable = Fnn.r2c(runnable, "result");

        Assertions.assertEquals("result", callable.call());
        Assertions.assertTrue(executed[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.r2c(null, "result"));
    }

    @Test
    public void testR2cRunnable() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        java.lang.Runnable runnable = () -> executed.set(true);
        Callable<Void> callable = Fn.r2c(runnable);
        assertNull(callable.call());
        assertTrue(executed.get());

        assertThrows(IllegalArgumentException.class, () -> Fn.r2c((java.lang.Runnable) null));
    }

    @Test
    public void testR2cRunnableWithReturn() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        java.lang.Runnable runnable = () -> executed.set(true);
        Callable<String> callable = Fn.r2c(runnable, "done");
        assertEquals("done", callable.call());
        assertTrue(executed.get());

        assertThrows(IllegalArgumentException.class, () -> Fn.r2c((java.lang.Runnable) null, "done"));
    }

    @Test
    public void testR2c() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Throwables.Runnable<Exception> runnable = counter::incrementAndGet;

        Throwables.Callable<Void, Exception> callable = Fnn.r2c(runnable);

        assertNull(callable.call());
        assertEquals(1, counter.get());

        assertThrows(IllegalArgumentException.class, () -> Fnn.r2c(null));
    }

    @Test
    public void testR2cWithReturn() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Throwables.Runnable<Exception> runnable = counter::incrementAndGet;

        Throwables.Callable<String, Exception> callable = Fnn.r2c(runnable, "done");

        assertEquals("done", callable.call());
        assertEquals(1, counter.get());

        assertThrows(IllegalArgumentException.class, () -> Fnn.r2c(null, "done"));
    }

    @Test
    public void testC2rCallableToRunnable() {
        Throwables.Callable<String, RuntimeException> callable = () -> "result";
        Throwables.Runnable<RuntimeException> runnable = Fnn.c2r(callable);

        Assertions.assertDoesNotThrow(() -> runnable.run());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2r(null));
    }

    @Test
    public void testC2r() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Callable<String> callable = () -> {
            executed.set(true);
            return "test";
        };
        Runnable runnable = Fn.jc2r(callable);
        runnable.run();
        assertTrue(executed.get());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2r(null));
    }

    @Test
    public void testJr2r() {
        final boolean[] executed = { false };
        java.lang.Runnable javaRunnable = () -> executed[0] = true;
        Throwables.Runnable<RuntimeException> throwableRunnable = Fnn.jr2r(javaRunnable);

        throwableRunnable.run();
        Assertions.assertTrue(executed[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.jr2r(null));
    }

    @Test
    public void testJr2r_Fn() {
        boolean[] executed = { false };
        java.lang.Runnable javaRunnable = () -> executed[0] = true;
        Runnable abacusRunnable = Fn.jr2r(javaRunnable);
        abacusRunnable.run();
        assertTrue(executed[0]);
        assertThrows(IllegalArgumentException.class, () -> Fn.jr2r(null));
    }

    @Test
    public void testJc2c() throws Exception {
        java.util.concurrent.Callable<String> javaCallable = () -> "result";
        Throwables.Callable<String, Exception> throwableCallable = Fnn.jc2c(javaCallable);

        Assertions.assertEquals("result", throwableCallable.call());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.jc2c(null));
    }

    @Test
    public void testJc2c_Fn() throws Exception {
        java.util.concurrent.Callable<String> javaCallable = () -> "result";
        Callable<String> abacusCallable = Fn.jc2c(javaCallable);
        assertEquals("result", abacusCallable.call());
        assertThrows(IllegalArgumentException.class, () -> Fn.jc2c(null));
    }

    @Test
    public void testJc2r() {
        AtomicBoolean executed = new AtomicBoolean(false);
        java.util.concurrent.Callable<String> callable = () -> {
            executed.set(true);
            return "test";
        };
        Runnable runnable = Fn.jc2r(callable);
        runnable.run();
        assertTrue(executed.get());

        java.util.concurrent.Callable<String> exceptionCallable = () -> {
            throw new Exception("error");
        };
        Runnable errorRunnable = Fn.jc2r(exceptionCallable);
        assertThrows(RuntimeException.class, errorRunnable::run);

        assertThrows(IllegalArgumentException.class, () -> Fn.jc2r(null));
    }

    @Test
    public void testThrowingMerger() {
        assertThrows(IllegalStateException.class, () -> Fn.throwingMerger().apply("a", "b"));
    }

    @Test
    public void testIgnoringMerger() {
        assertEquals("a", Fn.ignoringMerger().apply("a", "b"));
    }

    @Test
    public void testReplacingMerger() {
        assertEquals("b", Fn.replacingMerger().apply("a", "b"));
    }

    @Test
    public void testGetIfPresentOrElseNull() {
        Function<u.Optional<String>, String> getIfPresent = Fn.getIfPresentOrElseNull();

        u.Optional<String> present = u.Optional.of("value");
        assertEquals("value", getIfPresent.apply(present));

        u.Optional<String> empty = u.Optional.empty();
        assertNull(getIfPresent.apply(empty));
    }

    @Test
    public void testGetIfPresentOrElseNullJdk() {
        Function<java.util.Optional<String>, String> getIfPresent = Fn.getIfPresentOrElseNullJdk();

        java.util.Optional<String> present = java.util.Optional.of("value");
        assertEquals("value", getIfPresent.apply(present));

        java.util.Optional<String> empty = java.util.Optional.empty();
        assertNull(getIfPresent.apply(empty));
    }

    @Test
    public void testIsPresentPrimitives() {
        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(com.landawn.abacus.util.u.OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(com.landawn.abacus.util.u.OptionalBoolean.empty()));
        assertTrue(Fn.IS_PRESENT_CHAR.test(com.landawn.abacus.util.u.OptionalChar.of('a')));
        assertFalse(Fn.IS_PRESENT_CHAR.test(com.landawn.abacus.util.u.OptionalChar.empty()));
        assertTrue(Fn.IS_PRESENT_BYTE.test(com.landawn.abacus.util.u.OptionalByte.of((byte) 1)));
        assertFalse(Fn.IS_PRESENT_BYTE.test(com.landawn.abacus.util.u.OptionalByte.empty()));
        assertTrue(Fn.IS_PRESENT_SHORT.test(com.landawn.abacus.util.u.OptionalShort.of((short) 1)));
        assertFalse(Fn.IS_PRESENT_SHORT.test(com.landawn.abacus.util.u.OptionalShort.empty()));
        assertTrue(Fn.IS_PRESENT_INT.test(com.landawn.abacus.util.u.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT.test(com.landawn.abacus.util.u.OptionalInt.empty()));
        assertTrue(Fn.IS_PRESENT_LONG.test(com.landawn.abacus.util.u.OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG.test(com.landawn.abacus.util.u.OptionalLong.empty()));
        assertTrue(Fn.IS_PRESENT_FLOAT.test(com.landawn.abacus.util.u.OptionalFloat.of(1.0f)));
        assertFalse(Fn.IS_PRESENT_FLOAT.test(com.landawn.abacus.util.u.OptionalFloat.empty()));
        assertTrue(Fn.IS_PRESENT_DOUBLE.test(com.landawn.abacus.util.u.OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE.test(com.landawn.abacus.util.u.OptionalDouble.empty()));

        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));
        assertTrue(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.empty()));
        assertTrue(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.empty()));
    }

    @Test
    public void testIsPresent() {
        Predicate<u.Optional<String>> isPresent = Fn.isPresent();

        u.Optional<String> present = u.Optional.of("value");
        assertTrue(isPresent.test(present));

        u.Optional<String> empty = u.Optional.empty();
        assertFalse(isPresent.test(empty));
    }

    @Test
    public void testIsPresentPredicates() {
        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.empty()));

        assertTrue(Fn.IS_PRESENT_CHAR.test(OptionalChar.of('a')));
        assertFalse(Fn.IS_PRESENT_CHAR.test(OptionalChar.empty()));

        assertTrue(Fn.IS_PRESENT_BYTE.test(OptionalByte.of((byte) 1)));
        assertFalse(Fn.IS_PRESENT_BYTE.test(OptionalByte.empty()));

        assertTrue(Fn.IS_PRESENT_SHORT.test(OptionalShort.of((short) 1)));
        assertFalse(Fn.IS_PRESENT_SHORT.test(OptionalShort.empty()));

        assertTrue(Fn.IS_PRESENT_INT.test(OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT.test(OptionalInt.empty()));

        assertTrue(Fn.IS_PRESENT_LONG.test(OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG.test(OptionalLong.empty()));

        assertTrue(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.of(1.0f)));
        assertFalse(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.empty()));

        assertTrue(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.empty()));

        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));

        assertTrue(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.empty()));

        assertTrue(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.empty()));
    }

    @Test
    public void testIsPresentInt() {
        assertTrue(Fn.IS_PRESENT_INT.test(OptionalInt.of(42)));
        assertFalse(Fn.IS_PRESENT_INT.test(OptionalInt.empty()));
    }

    @Test
    public void testIsPresentLong() {
        assertTrue(Fn.IS_PRESENT_LONG.test(OptionalLong.of(42L)));
        assertFalse(Fn.IS_PRESENT_LONG.test(OptionalLong.empty()));
    }

    @Test
    public void testIsPresentDouble() {
        assertTrue(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.of(42.5)));
        assertFalse(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.empty()));
    }

    @Test
    public void testIsPresentJdk() {
        Predicate<java.util.Optional<String>> isPresent = Fn.isPresentJdk();

        java.util.Optional<String> present = java.util.Optional.of("value");
        assertTrue(isPresent.test(present));

        java.util.Optional<String> empty = java.util.Optional.empty();
        assertFalse(isPresent.test(empty));
    }

    @Test
    public void testAlternate() {
        IntBiFunction<MergeResult> func = FI.alternate();
        assertEquals(MergeResult.TAKE_FIRST, func.apply(1, 2));
        assertEquals(MergeResult.TAKE_SECOND, func.apply(1, 2));
        assertEquals(MergeResult.TAKE_FIRST, func.apply(1, 2));
    }

    @Test
    public void testOfInstance() {
        String s = "instance";
        Supplier<String> supplier = Suppliers.ofInstance(s);
        assertSame(s, supplier.get());
    }

    @Test
    public void testCollectionSuppliers() {
        assertTrue(Suppliers.ofList().get() instanceof ArrayList);
        assertTrue(Suppliers.ofSet().get() instanceof HashSet);
        assertTrue(Suppliers.ofMap().get() instanceof HashMap);
        assertTrue(Suppliers.ofCollection(LinkedList.class).get() instanceof LinkedList);
        assertTrue(Suppliers.ofMap(TreeMap.class).get() instanceof TreeMap);
    }

    @Test
    public void testRegister() {
        assertThrows(IllegalArgumentException.class, () -> Suppliers.registerForCollection(ArrayList.class, ArrayList::new));
        assertThrows(IllegalArgumentException.class, () -> Suppliers.registerForMap(HashMap.class, HashMap::new));

    }

    @Test
    public void testArrayFactories() {
        assertArrayEquals(new int[5], IntFunctions.ofIntArray().apply(5));
        assertArrayEquals(new String[3], IntFunctions.ofStringArray().apply(3));
    }

    @Test
    public void testDistinct() {
        Predicate<String> p = Predicates.distinct();
        assertTrue(p.test("a"));
        assertTrue(p.test("b"));
        assertFalse(p.test("a"));
        assertTrue(p.test("c"));
    }

    @Test
    public void testDistinctBy() {
        Predicate<String> p = Predicates.distinctBy(String::length);
        assertTrue(p.test("a"));
        assertTrue(p.test("bb"));
        assertFalse(p.test("c"));
        assertTrue(p.test("ddd"));
    }

    @Test
    public void testConcurrentDistinct() {
        Predicate<String> p = Predicates.concurrentDistinct();
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "a");
        long count = list.parallelStream().filter(p).count();
        assertEquals(3, count);
    }

    @Test
    public void testSkipRepeats() {
        Predicate<String> p = Predicates.skipRepeats();
        assertTrue(p.test("a"));
        assertFalse(p.test("a"));
        assertTrue(p.test("b"));
        assertTrue(p.test("c"));
        assertFalse(p.test("c"));
    }

    @Test
    public void testPositive() {
        assertTrue(FI.positive().test(1));
        assertFalse(FI.positive().test(0));
        assertFalse(FI.positive().test(-1));
    }

    @Test
    public void testUnbox() {
        assertEquals(1, FI.unbox().applyAsInt(1));
    }

    @Test
    public void testSum() {
        assertEquals(6, FI.sum().apply(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testAverage() {
        assertEquals(2.0, FI.average().apply(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testIntBinaryOperators() {
        assertEquals(1, FI.IntBinaryOperators.MIN.applyAsInt(1, 2));
        assertEquals(2, FI.IntBinaryOperators.MAX.applyAsInt(1, 2));
    }

    @Test
    public void testGetAsPrimitives() {
        assertEquals(true, Fn.GET_AS_BOOLEAN.applyAsBoolean(com.landawn.abacus.util.u.OptionalBoolean.of(true)));
        assertEquals('a', Fn.GET_AS_CHAR.applyAsChar(com.landawn.abacus.util.u.OptionalChar.of('a')));
        assertEquals((byte) 1, Fn.GET_AS_BYTE.applyAsByte(com.landawn.abacus.util.u.OptionalByte.of((byte) 1)));
        assertEquals((short) 1, Fn.GET_AS_SHORT.applyAsShort(com.landawn.abacus.util.u.OptionalShort.of((short) 1)));
        assertEquals(1, Fn.GET_AS_INT.applyAsInt(com.landawn.abacus.util.u.OptionalInt.of(1)));
        assertEquals(1L, Fn.GET_AS_LONG.applyAsLong(com.landawn.abacus.util.u.OptionalLong.of(1L)));
        assertEquals(1.0f, Fn.GET_AS_FLOAT.applyAsFloat(com.landawn.abacus.util.u.OptionalFloat.of(1.0f)));
        assertEquals(1.0, Fn.GET_AS_DOUBLE.applyAsDouble(com.landawn.abacus.util.u.OptionalDouble.of(1.0)));

        assertEquals(1, Fn.GET_AS_INT_JDK.applyAsInt(java.util.OptionalInt.of(1)));
        assertEquals(1L, Fn.GET_AS_LONG_JDK.applyAsLong(java.util.OptionalLong.of(1L)));
        assertEquals(1.0, Fn.GET_AS_DOUBLE_JDK.applyAsDouble(java.util.OptionalDouble.of(1.0)));
    }

    @Test
    public void testIsZero() {
        CharPredicate predicate = Fn.FC.isZero();
        Assertions.assertTrue(predicate.test('\0'));
        Assertions.assertFalse(predicate.test('a'));
        Assertions.assertFalse(predicate.test(' '));
        Assertions.assertFalse(predicate.test('1'));
    }

    @Test
    public void testIsWhitespace() {
        CharPredicate predicate = Fn.FC.isWhitespace();
        Assertions.assertTrue(predicate.test(' '));
        Assertions.assertTrue(predicate.test('\t'));
        Assertions.assertTrue(predicate.test('\n'));
        Assertions.assertTrue(predicate.test('\r'));
        Assertions.assertFalse(predicate.test('a'));
        Assertions.assertFalse(predicate.test('1'));
        Assertions.assertFalse(predicate.test('\0'));
    }

    @Test
    public void testCharBinaryOperatorsMIN() {
        CharBinaryOperator minOp = Fn.FC.CharBinaryOperators.MIN;
        Assertions.assertEquals('a', minOp.applyAsChar('a', 'b'));
        Assertions.assertEquals('a', minOp.applyAsChar('b', 'a'));
        Assertions.assertEquals('a', minOp.applyAsChar('a', 'a'));
        Assertions.assertEquals('\0', minOp.applyAsChar('\0', 'a'));
        Assertions.assertEquals('A', minOp.applyAsChar('A', 'a'));
    }

    @Test
    public void testCharBinaryOperatorsMAX() {
        CharBinaryOperator maxOp = Fn.FC.CharBinaryOperators.MAX;
        Assertions.assertEquals('b', maxOp.applyAsChar('a', 'b'));
        Assertions.assertEquals('b', maxOp.applyAsChar('b', 'a'));
        Assertions.assertEquals('a', maxOp.applyAsChar('a', 'a'));
        Assertions.assertEquals('a', maxOp.applyAsChar('\0', 'a'));
        Assertions.assertEquals('a', maxOp.applyAsChar('A', 'a'));
    }

    @Test
    public void testByteBinaryOperatorsMIN() {
        ByteBinaryOperator minOp = Fn.FB.ByteBinaryOperators.MIN;
        Assertions.assertEquals((byte) 5, minOp.applyAsByte((byte) 5, (byte) 10));
        Assertions.assertEquals((byte) 5, minOp.applyAsByte((byte) 10, (byte) 5));
        Assertions.assertEquals((byte) 5, minOp.applyAsByte((byte) 5, (byte) 5));
        Assertions.assertEquals((byte) -10, minOp.applyAsByte((byte) -10, (byte) 5));
    }

    @Test
    public void testByteBinaryOperatorsMAX() {
        ByteBinaryOperator maxOp = Fn.FB.ByteBinaryOperators.MAX;
        Assertions.assertEquals((byte) 10, maxOp.applyAsByte((byte) 5, (byte) 10));
        Assertions.assertEquals((byte) 10, maxOp.applyAsByte((byte) 10, (byte) 5));
        Assertions.assertEquals((byte) 5, maxOp.applyAsByte((byte) 5, (byte) 5));
        Assertions.assertEquals((byte) 5, maxOp.applyAsByte((byte) -10, (byte) 5));
    }

    @Test
    public void testShortBinaryOperatorsMIN() {
        ShortBinaryOperator minOp = Fn.FS.ShortBinaryOperators.MIN;
        Assertions.assertEquals((short) 50, minOp.applyAsShort((short) 50, (short) 100));
        Assertions.assertEquals((short) 50, minOp.applyAsShort((short) 100, (short) 50));
        Assertions.assertEquals((short) 50, minOp.applyAsShort((short) 50, (short) 50));
        Assertions.assertEquals((short) -100, minOp.applyAsShort((short) -100, (short) 50));
    }

    @Test
    public void testShortBinaryOperatorsMAX() {
        ShortBinaryOperator maxOp = Fn.FS.ShortBinaryOperators.MAX;
        Assertions.assertEquals((short) 100, maxOp.applyAsShort((short) 50, (short) 100));
        Assertions.assertEquals((short) 100, maxOp.applyAsShort((short) 100, (short) 50));
        Assertions.assertEquals((short) 50, maxOp.applyAsShort((short) 50, (short) 50));
        Assertions.assertEquals((short) 50, maxOp.applyAsShort((short) -100, (short) 50));
    }

    @Test
    public void testIntBinaryOperatorsMIN() {
        IntBinaryOperator minOp = Fn.FI.IntBinaryOperators.MIN;
        Assertions.assertEquals(500, minOp.applyAsInt(500, 1000));
        Assertions.assertEquals(500, minOp.applyAsInt(1000, 500));
        Assertions.assertEquals(500, minOp.applyAsInt(500, 500));
        Assertions.assertEquals(-1000, minOp.applyAsInt(-1000, 500));

        Assertions.assertEquals(Integer.MIN_VALUE, minOp.applyAsInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testIntBinaryOperatorsMAX() {
        IntBinaryOperator maxOp = Fn.FI.IntBinaryOperators.MAX;
        Assertions.assertEquals(1000, maxOp.applyAsInt(500, 1000));
        Assertions.assertEquals(1000, maxOp.applyAsInt(1000, 500));
        Assertions.assertEquals(500, maxOp.applyAsInt(500, 500));
        Assertions.assertEquals(500, maxOp.applyAsInt(-1000, 500));

        Assertions.assertEquals(Integer.MAX_VALUE, maxOp.applyAsInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testLongBinaryOperatorsMIN() {
        LongBinaryOperator minOp = Fn.FL.LongBinaryOperators.MIN;
        Assertions.assertEquals(500000L, minOp.applyAsLong(500000L, 1000000L));
        Assertions.assertEquals(500000L, minOp.applyAsLong(1000000L, 500000L));
        Assertions.assertEquals(500000L, minOp.applyAsLong(500000L, 500000L));
        Assertions.assertEquals(-1000000L, minOp.applyAsLong(-1000000L, 500000L));

        Assertions.assertEquals(Long.MIN_VALUE, minOp.applyAsLong(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    @Test
    public void testLongBinaryOperatorsMAX() {
        LongBinaryOperator maxOp = Fn.FL.LongBinaryOperators.MAX;
        Assertions.assertEquals(1000000L, maxOp.applyAsLong(500000L, 1000000L));
        Assertions.assertEquals(1000000L, maxOp.applyAsLong(1000000L, 500000L));
        Assertions.assertEquals(500000L, maxOp.applyAsLong(500000L, 500000L));
        Assertions.assertEquals(500000L, maxOp.applyAsLong(-1000000L, 500000L));

        Assertions.assertEquals(Long.MAX_VALUE, maxOp.applyAsLong(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    @Test
    public void testFloatBinaryOperatorsMIN() {
        FloatBinaryOperator minOp = Fn.FF.FloatBinaryOperators.MIN;
        Assertions.assertEquals(5.5f, minOp.applyAsFloat(5.5f, 10.5f), 0.0001f);
        Assertions.assertEquals(5.5f, minOp.applyAsFloat(10.5f, 5.5f), 0.0001f);
        Assertions.assertEquals(5.5f, minOp.applyAsFloat(5.5f, 5.5f), 0.0001f);
        Assertions.assertEquals(-10.5f, minOp.applyAsFloat(-10.5f, 5.5f), 0.0001f);

        Assertions.assertEquals(-Float.MAX_VALUE, minOp.applyAsFloat(-Float.MAX_VALUE, Float.MAX_VALUE), 0.0001f);
        Assertions.assertTrue(Float.isNaN(minOp.applyAsFloat(Float.NaN, 1.0f)));
    }

    @Test
    public void testFloatBinaryOperatorsMAX() {
        FloatBinaryOperator maxOp = Fn.FF.FloatBinaryOperators.MAX;
        Assertions.assertEquals(10.5f, maxOp.applyAsFloat(5.5f, 10.5f), 0.0001f);
        Assertions.assertEquals(10.5f, maxOp.applyAsFloat(10.5f, 5.5f), 0.0001f);
        Assertions.assertEquals(5.5f, maxOp.applyAsFloat(5.5f, 5.5f), 0.0001f);
        Assertions.assertEquals(5.5f, maxOp.applyAsFloat(-10.5f, 5.5f), 0.0001f);

        Assertions.assertEquals(Float.MAX_VALUE, maxOp.applyAsFloat(-Float.MAX_VALUE, Float.MAX_VALUE), 0.0001f);
        Assertions.assertTrue(Float.isNaN(maxOp.applyAsFloat(Float.NaN, 1.0f)));
    }

    @Test
    public void testDoubleBinaryOperatorsMIN() {
        DoubleBinaryOperator minOp = Fn.FD.DoubleBinaryOperators.MIN;
        Assertions.assertEquals(5.5, minOp.applyAsDouble(5.5, 10.5), 0.0001);
        Assertions.assertEquals(5.5, minOp.applyAsDouble(10.5, 5.5), 0.0001);
        Assertions.assertEquals(5.5, minOp.applyAsDouble(5.5, 5.5), 0.0001);
        Assertions.assertEquals(-10.5, minOp.applyAsDouble(-10.5, 5.5), 0.0001);

        Assertions.assertEquals(-Double.MAX_VALUE, minOp.applyAsDouble(-Double.MAX_VALUE, Double.MAX_VALUE), 0.0001);
        Assertions.assertTrue(Double.isNaN(minOp.applyAsDouble(Double.NaN, 1.0)));
    }

    @Test
    public void testDoubleBinaryOperatorsMAX() {
        DoubleBinaryOperator maxOp = Fn.FD.DoubleBinaryOperators.MAX;
        Assertions.assertEquals(10.5, maxOp.applyAsDouble(5.5, 10.5), 0.0001);
        Assertions.assertEquals(10.5, maxOp.applyAsDouble(10.5, 5.5), 0.0001);
        Assertions.assertEquals(5.5, maxOp.applyAsDouble(5.5, 5.5), 0.0001);
        Assertions.assertEquals(5.5, maxOp.applyAsDouble(-10.5, 5.5), 0.0001);

        Assertions.assertEquals(Double.MAX_VALUE, maxOp.applyAsDouble(-Double.MAX_VALUE, Double.MAX_VALUE), 0.0001);
        Assertions.assertTrue(Double.isNaN(maxOp.applyAsDouble(Double.NaN, 1.0)));
    }

    @Test
    public void testThrowIOException() {
        Throwables.Consumer<String, IOException> thrower = Fnn.throwIOException("IO Error");
        IOException ex = Assertions.assertThrows(IOException.class, () -> thrower.accept("test"));
        Assertions.assertEquals("IO Error", ex.getMessage());
    }

    @Test
    public void testOfCurrentTimeMillisMultipleCalls() {
        LongSupplier supplier1 = Fn.LongSuppliers.ofCurrentTimeMillis();
        LongSupplier supplier2 = Fn.LongSuppliers.ofCurrentTimeMillis();

        Assertions.assertSame(supplier1, supplier2);
    }

    @Test
    public void testOf_Supplier() {
        Supplier<String> original = () -> "test";
        Supplier<String> result = Suppliers.of(original);
        Assertions.assertSame(original, result);
        Assertions.assertEquals("test", result.get());
    }

    @Test
    public void testOf_WithFunction() {
        String input = "test";
        Function<String, Integer> func = String::length;
        Supplier<Integer> supplier = Suppliers.of(input, func);

        Assertions.assertNotNull(supplier);
        Assertions.assertEquals(4, supplier.get());
        Assertions.assertEquals(4, supplier.get());
    }

    @Test
    public void testOfUUID() {
        Supplier<String> supplier = Suppliers.ofUuid();
        Assertions.assertNotNull(supplier);

        String uuid1 = supplier.get();
        String uuid2 = supplier.get();

        Assertions.assertNotNull(uuid1);
        Assertions.assertNotNull(uuid2);
        Assertions.assertNotEquals(uuid1, uuid2);

        Assertions.assertSame(supplier, Suppliers.ofUuid());
    }

    @Test
    public void testOfGUID() {
        Supplier<String> supplier = Suppliers.ofUuidWithoutHyphens();
        Assertions.assertNotNull(supplier);

        String guid1 = supplier.get();
        String guid2 = supplier.get();

        Assertions.assertNotNull(guid1);
        Assertions.assertNotNull(guid2);
        Assertions.assertNotEquals(guid1, guid2);

        Assertions.assertSame(supplier, Suppliers.ofUuidWithoutHyphens());
    }

    @Test
    public void testOfEmptyBooleanArray() {
        Supplier<boolean[]> supplier = Suppliers.ofEmptyBooleanArray();
        Assertions.assertNotNull(supplier);

        boolean[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyBooleanArray());
    }

    @Test
    public void testOfEmptyCharArray() {
        Supplier<char[]> supplier = Suppliers.ofEmptyCharArray();
        Assertions.assertNotNull(supplier);

        char[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyCharArray());
    }

    @Test
    public void testOfEmptyByteArray() {
        Supplier<byte[]> supplier = Suppliers.ofEmptyByteArray();
        Assertions.assertNotNull(supplier);

        byte[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyByteArray());
    }

    @Test
    public void testOfEmptyShortArray() {
        Supplier<short[]> supplier = Suppliers.ofEmptyShortArray();
        Assertions.assertNotNull(supplier);

        short[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyShortArray());
    }

    @Test
    public void testOfEmptyIntArray() {
        Supplier<int[]> supplier = Suppliers.ofEmptyIntArray();
        Assertions.assertNotNull(supplier);

        int[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyIntArray());
    }

    @Test
    public void testOfEmptyLongArray() {
        Supplier<long[]> supplier = Suppliers.ofEmptyLongArray();
        Assertions.assertNotNull(supplier);

        long[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyLongArray());
    }

    @Test
    public void testOfEmptyFloatArray() {
        Supplier<float[]> supplier = Suppliers.ofEmptyFloatArray();
        Assertions.assertNotNull(supplier);

        float[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyFloatArray());
    }

    @Test
    public void testOfEmptyDoubleArray() {
        Supplier<double[]> supplier = Suppliers.ofEmptyDoubleArray();
        Assertions.assertNotNull(supplier);

        double[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyDoubleArray());
    }

    @Test
    public void testOfEmptyStringArray() {
        Supplier<String[]> supplier = Suppliers.ofEmptyStringArray();
        Assertions.assertNotNull(supplier);

        String[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyStringArray());
    }

    @Test
    public void testOfEmptyObjectArray() {
        Supplier<Object[]> supplier = Suppliers.ofEmptyObjectArray();
        Assertions.assertNotNull(supplier);

        Object[] array = supplier.get();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);

        Assertions.assertSame(array, supplier.get());
        Assertions.assertSame(supplier, Suppliers.ofEmptyObjectArray());
    }

    @Test
    public void testOfListMultimap() {
        Supplier<ListMultimap<String, Integer>> supplier = Suppliers.ofListMultimap();
        Assertions.assertNotNull(supplier);

        ListMultimap<String, Integer> map1 = supplier.get();
        ListMultimap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());

        Assertions.assertSame(supplier, Suppliers.ofListMultimap());
    }

    @Test
    public void testOfListMultimapWithMapType() {
        Supplier<ListMultimap<String, Integer>> supplier = Suppliers.ofListMultimap(LinkedHashMap.class);
        Assertions.assertNotNull(supplier);

        ListMultimap<String, Integer> map1 = supplier.get();
        ListMultimap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());
    }

    @Test
    public void testOfListMultimapWithMapAndValueType() {
        Supplier<ListMultimap<String, Integer>> supplier = Suppliers.ofListMultimap(HashMap.class, LinkedList.class);
        Assertions.assertNotNull(supplier);

        ListMultimap<String, Integer> map1 = supplier.get();
        ListMultimap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());
    }

    @Test
    public void testOfListMultimapWithSuppliers() {
        java.util.function.Supplier<Map<String, List<Integer>>> mapSupplier = HashMap::new;
        java.util.function.Supplier<List<Integer>> valueSupplier = ArrayList::new;
        Supplier<ListMultimap<String, Integer>> supplier = Suppliers.ofListMultimap(mapSupplier, valueSupplier);
        Assertions.assertNotNull(supplier);

        ListMultimap<String, Integer> map1 = supplier.get();
        ListMultimap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());
    }

    @Test
    public void testOfSetMultimap() {
        Supplier<SetMultimap<String, Integer>> supplier = Suppliers.ofSetMultimap();
        Assertions.assertNotNull(supplier);

        SetMultimap<String, Integer> map1 = supplier.get();
        SetMultimap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());

        Assertions.assertSame(supplier, Suppliers.ofSetMultimap());
    }

    @Test
    public void testOfSetMultimapWithMapType() {
        Supplier<SetMultimap<String, Integer>> supplier = Suppliers.ofSetMultimap(LinkedHashMap.class);
        Assertions.assertNotNull(supplier);

        SetMultimap<String, Integer> map1 = supplier.get();
        SetMultimap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());
    }

    @Test
    public void testOfSetMultimapWithMapAndValueType() {
        Supplier<SetMultimap<String, Integer>> supplier = Suppliers.ofSetMultimap(HashMap.class, LinkedHashSet.class);
        Assertions.assertNotNull(supplier);

        SetMultimap<String, Integer> map1 = supplier.get();
        SetMultimap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());
    }

    @Test
    public void testOfSetMultimapWithSuppliers() {
        java.util.function.Supplier<Map<String, Set<Integer>>> mapSupplier = HashMap::new;
        java.util.function.Supplier<Set<Integer>> valueSupplier = HashSet::new;
        Supplier<SetMultimap<String, Integer>> supplier = Suppliers.ofSetMultimap(mapSupplier, valueSupplier);
        Assertions.assertNotNull(supplier);

        SetMultimap<String, Integer> map1 = supplier.get();
        SetMultimap<String, Integer> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());
    }

    @Test
    public void testOfMultimap() {
        java.util.function.Supplier<Map<String, List<Integer>>> mapSupplier = HashMap::new;
        java.util.function.Supplier<List<Integer>> valueSupplier = ArrayList::new;
        Supplier<Multimap<String, Integer, List<Integer>>> supplier = Suppliers.ofMultimap(mapSupplier, valueSupplier);
        Assertions.assertNotNull(supplier);

        Multimap<String, Integer, List<Integer>> map1 = supplier.get();
        Multimap<String, Integer, List<Integer>> map2 = supplier.get();

        Assertions.assertNotNull(map1);
        Assertions.assertNotNull(map2);
        Assertions.assertNotSame(map1, map2);
        Assertions.assertEquals(0, map1.totalValueCount());
    }

    @Test
    public void testOfCollection() {
        Supplier<? extends Collection<String>> supplier = Suppliers.<String> ofCollection(ArrayList.class);
        Assertions.assertNotNull(supplier);
        Collection<String> coll = supplier.get();
        Assertions.assertTrue(coll instanceof ArrayList);

        supplier = Suppliers.<String> ofCollection(LinkedList.class);
        coll = supplier.get();
        Assertions.assertTrue(coll instanceof LinkedList);

        supplier = Suppliers.<String> ofCollection(HashSet.class);
        coll = supplier.get();
        Assertions.assertTrue(coll instanceof HashSet);

        supplier = Suppliers.<String> ofCollection(Collection.class);
        coll = supplier.get();
        Assertions.assertTrue(coll instanceof ArrayList);

        Supplier<? extends Collection<String>> supplier2 = Suppliers.ofCollection(ArrayList.class);
        Assertions.assertSame(supplier2, Suppliers.ofCollection(ArrayList.class));
    }

    @Test
    public void testOfCollectionWithInvalidType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.ofCollection((Class) Map.class);
        });
    }

    @Test
    public void testOfMap_Class() {
        Supplier<? extends Map<String, Integer>> supplier = Suppliers.ofMap(HashMap.class);
        Assertions.assertNotNull(supplier);
        Map<String, Integer> map = supplier.get();
        Assertions.assertTrue(map instanceof HashMap);

        supplier = Suppliers.ofMap(LinkedHashMap.class);
        map = supplier.get();
        Assertions.assertTrue(map instanceof LinkedHashMap);

        supplier = Suppliers.ofMap(TreeMap.class);
        map = supplier.get();
        Assertions.assertTrue(map instanceof TreeMap);

        supplier = Suppliers.ofMap(Map.class);
        map = supplier.get();
        Assertions.assertTrue(map instanceof HashMap);

        Supplier<? extends Map<String, Integer>> supplier2 = Suppliers.ofMap(HashMap.class);
        Assertions.assertSame(supplier2, Suppliers.ofMap(HashMap.class));
    }

    @Test
    public void testOfMapWithInvalidType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.ofMap((Class) List.class);
        });
    }

    @Test
    public void testRegisterForCollection() {
        class CustomCollection<T> extends ArrayList<T> {
        }

        java.util.function.Supplier<CustomCollection> customSupplier = CustomCollection::new;

        Assertions.assertThrows(IllegalArgumentException.class, () -> Suppliers.registerForCollection(CustomCollection.class, customSupplier));
    }

    @Test
    public void testRegisterForCollectionWithBuiltinClass() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.registerForCollection(ArrayList.class, ArrayList::new);
        });
    }

    @Test
    public void testRegisterForMap() {
        class CustomMap<K, V> extends HashMap<K, V> {
        }

        java.util.function.Supplier<CustomMap> customSupplier = CustomMap::new;

        Assertions.assertThrows(IllegalArgumentException.class, () -> Suppliers.registerForMap(CustomMap.class, customSupplier));

    }

    @Test
    public void testRegisterForMapWithBuiltinClass() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.registerForMap(HashMap.class, HashMap::new);
        });
    }

    @Test
    public void testOfImmutableList() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableList();
        });
    }

    @Test
    public void testOfImmutableSet() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableSet();
        });
    }

    @Test
    public void testOfImmutableMap() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableMap();
        });
    }

    @Test
    public void testNewException() {
        Supplier<Exception> supplier = Suppliers.newException();
        Assertions.assertNotNull(supplier);

        Exception ex1 = supplier.get();
        Exception ex2 = supplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2);

        Assertions.assertSame(supplier, Suppliers.newException());
    }

    @Test
    public void testNewRuntimeException() {
        Supplier<RuntimeException> supplier = Suppliers.newRuntimeException();
        Assertions.assertNotNull(supplier);

        RuntimeException ex1 = supplier.get();
        RuntimeException ex2 = supplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2);

        Assertions.assertSame(supplier, Suppliers.newRuntimeException());
    }

    @Test
    public void testNewNoSuchElementException() {
        Supplier<NoSuchElementException> supplier = Suppliers.newNoSuchElementException();
        Assertions.assertNotNull(supplier);

        NoSuchElementException ex1 = supplier.get();
        NoSuchElementException ex2 = supplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2);

        Assertions.assertSame(supplier, Suppliers.newNoSuchElementException());
    }

    @Test
    public void testOf() {
        IntFunction<String> original = size -> "Size: " + size;
        IntFunction<String> result = IntFunctions.of(original);
        Assertions.assertSame(original, result);
        Assertions.assertEquals("Size: 5", result.apply(5));
    }

    @Test
    public void testOfBooleanArray() {
        IntFunction<boolean[]> func = IntFunctions.ofBooleanArray();
        Assertions.assertNotNull(func);

        boolean[] array1 = func.apply(5);
        boolean[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (boolean b : array1) {
            Assertions.assertFalse(b);
        }

        Assertions.assertSame(func, IntFunctions.ofBooleanArray());
    }

    @Test
    public void testOfCharArray() {
        IntFunction<char[]> func = IntFunctions.ofCharArray();
        Assertions.assertNotNull(func);

        char[] array1 = func.apply(5);
        char[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (char c : array1) {
            Assertions.assertEquals('\u0000', c);
        }

        Assertions.assertSame(func, IntFunctions.ofCharArray());
    }

    @Test
    public void testOfByteArray() {
        IntFunction<byte[]> func = IntFunctions.ofByteArray();
        Assertions.assertNotNull(func);

        byte[] array1 = func.apply(5);
        byte[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (byte b : array1) {
            Assertions.assertEquals((byte) 0, b);
        }

        Assertions.assertSame(func, IntFunctions.ofByteArray());
    }

    @Test
    public void testOfShortArray() {
        IntFunction<short[]> func = IntFunctions.ofShortArray();
        Assertions.assertNotNull(func);

        short[] array1 = func.apply(5);
        short[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (short s : array1) {
            Assertions.assertEquals((short) 0, s);
        }

        Assertions.assertSame(func, IntFunctions.ofShortArray());
    }

    @Test
    public void testOfIntArray() {
        IntFunction<int[]> func = IntFunctions.ofIntArray();
        Assertions.assertNotNull(func);

        int[] array1 = func.apply(5);
        int[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (int i : array1) {
            Assertions.assertEquals(0, i);
        }

        Assertions.assertSame(func, IntFunctions.ofIntArray());
    }

    @Test
    public void testOfLongArray() {
        IntFunction<long[]> func = IntFunctions.ofLongArray();
        Assertions.assertNotNull(func);

        long[] array1 = func.apply(5);
        long[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (long l : array1) {
            Assertions.assertEquals(0L, l);
        }

        Assertions.assertSame(func, IntFunctions.ofLongArray());
    }

    @Test
    public void testOfFloatArray() {
        IntFunction<float[]> func = IntFunctions.ofFloatArray();
        Assertions.assertNotNull(func);

        float[] array1 = func.apply(5);
        float[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (float f : array1) {
            Assertions.assertEquals(0.0f, f);
        }

        Assertions.assertSame(func, IntFunctions.ofFloatArray());
    }

    @Test
    public void testOfDoubleArray() {
        IntFunction<double[]> func = IntFunctions.ofDoubleArray();
        Assertions.assertNotNull(func);

        double[] array1 = func.apply(5);
        double[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (double d : array1) {
            Assertions.assertEquals(0.0, d);
        }

        Assertions.assertSame(func, IntFunctions.ofDoubleArray());
    }

    @Test
    public void testOfStringArray() {
        IntFunction<String[]> func = IntFunctions.ofStringArray();
        Assertions.assertNotNull(func);

        String[] array1 = func.apply(5);
        String[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (String s : array1) {
            Assertions.assertNull(s);
        }

        Assertions.assertSame(func, IntFunctions.ofStringArray());
    }

    @Test
    public void testOfObjectArray() {
        IntFunction<Object[]> func = IntFunctions.ofObjectArray();
        Assertions.assertNotNull(func);

        Object[] array1 = func.apply(5);
        Object[] array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertEquals(5, array1.length);
        Assertions.assertEquals(10, array2.length);
        Assertions.assertNotSame(array1, array2);

        for (Object o : array1) {
            Assertions.assertNull(o);
        }

        Assertions.assertSame(func, IntFunctions.ofObjectArray());
    }

    @Test
    public void testOfDisposableArray() {
        IntFunction<DisposableObjArray> func = IntFunctions.ofDisposableArray();
        Assertions.assertNotNull(func);

        DisposableObjArray array1 = func.apply(5);
        DisposableObjArray array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertSame(array1, array2);
    }

    @Test
    public void testOfDisposableArrayWithComponentType() {
        IntFunction<DisposableArray<String>> func = IntFunctions.ofDisposableArray(String.class);
        Assertions.assertNotNull(func);

        DisposableArray<String> array1 = func.apply(5);
        DisposableArray<String> array2 = func.apply(10);

        Assertions.assertNotNull(array1);
        Assertions.assertNotNull(array2);
        Assertions.assertSame(array1, array2);
    }

    @Test
    public void testGetAsBooleanConstant() {
        OptionalBoolean ob = OptionalBoolean.of(true);
        assertTrue(Fn.GET_AS_BOOLEAN.applyAsBoolean(ob));
    }

    @Test
    public void testGetAsCharConstant() {
        OptionalChar oc = OptionalChar.of('a');
        assertEquals('a', Fn.GET_AS_CHAR.applyAsChar(oc));
    }

    @Test
    public void testGetAsByteConstant() {
        OptionalByte ob = OptionalByte.of((byte) 5);
        assertEquals((byte) 5, Fn.GET_AS_BYTE.applyAsByte(ob));
    }

    @Test
    public void testGetAsShortConstant() {
        OptionalShort os = OptionalShort.of((short) 10);
        assertEquals((short) 10, Fn.GET_AS_SHORT.applyAsShort(os));
    }

    @Test
    public void testGetAsIntConstant() {
        OptionalInt oi = OptionalInt.of(100);
        assertEquals(100, Fn.GET_AS_INT.applyAsInt(oi));
    }

    @Test
    public void testGetAsLongConstant() {
        OptionalLong ol = OptionalLong.of(1000L);
        assertEquals(1000L, Fn.GET_AS_LONG.applyAsLong(ol));
    }

    @Test
    public void testGetAsFloatConstant() {
        OptionalFloat of = OptionalFloat.of(1.5f);
        assertEquals(1.5f, Fn.GET_AS_FLOAT.applyAsFloat(of));
    }

    @Test
    public void testGetAsDoubleConstant() {
        OptionalDouble od = OptionalDouble.of(2.5);
        assertEquals(2.5, Fn.GET_AS_DOUBLE.applyAsDouble(od));
    }

    @Test
    public void testGetAsIntJDKConstant() {
        java.util.OptionalInt oi = java.util.OptionalInt.of(100);
        assertEquals(100, Fn.GET_AS_INT_JDK.applyAsInt(oi));
    }

    @Test
    public void testGetAsLongJDKConstant() {
        java.util.OptionalLong ol = java.util.OptionalLong.of(1000L);
        assertEquals(1000L, Fn.GET_AS_LONG_JDK.applyAsLong(ol));
    }

    @Test
    public void testGetAsDoubleJDKConstant() {
        java.util.OptionalDouble od = java.util.OptionalDouble.of(2.5);
        assertEquals(2.5, Fn.GET_AS_DOUBLE_JDK.applyAsDouble(od));
    }

    @Test
    public void testOfEmptyArraySuppliers() {
        assertArrayEquals(new boolean[0], Suppliers.ofEmptyBooleanArray().get());
        assertArrayEquals(new char[0], Suppliers.ofEmptyCharArray().get());
        assertArrayEquals(new byte[0], Suppliers.ofEmptyByteArray().get());
        assertArrayEquals(new short[0], Suppliers.ofEmptyShortArray().get());
        assertArrayEquals(new int[0], Suppliers.ofEmptyIntArray().get());
        assertArrayEquals(new long[0], Suppliers.ofEmptyLongArray().get());
        assertArrayEquals(new float[0], Suppliers.ofEmptyFloatArray().get(), 0.0f);
        assertArrayEquals(new double[0], Suppliers.ofEmptyDoubleArray().get(), 0.0);
        assertArrayEquals(new String[0], Suppliers.ofEmptyStringArray().get());
        assertArrayEquals(new Object[0], Suppliers.ofEmptyObjectArray().get());
    }

    @Test
    public void testOfPrimitiveListSuppliers() {
        assertTrue(Suppliers.ofBooleanList().get() instanceof BooleanList);
        assertTrue(Suppliers.ofCharList().get() instanceof CharList);
        assertTrue(Suppliers.ofByteList().get() instanceof ByteList);
        assertTrue(Suppliers.ofShortList().get() instanceof ShortList);
        assertTrue(Suppliers.ofIntList().get() instanceof IntList);
        assertTrue(Suppliers.ofLongList().get() instanceof LongList);
        assertTrue(Suppliers.ofFloatList().get() instanceof FloatList);
        assertTrue(Suppliers.ofDoubleList().get() instanceof DoubleList);
    }

    @Test
    public void testOfCollectionSuppliers() {
        assertTrue(Suppliers.ofList().get() instanceof ArrayList);
        assertTrue(Suppliers.ofLinkedList().get() instanceof LinkedList);
        assertTrue(Suppliers.ofSet().get() instanceof HashSet);
        assertTrue(Suppliers.ofLinkedHashSet().get() instanceof LinkedHashSet);
        assertTrue(Suppliers.ofSortedSet().get() instanceof TreeSet);
        assertTrue(Suppliers.ofNavigableSet().get() instanceof TreeSet);
        assertTrue(Suppliers.ofTreeSet().get() instanceof TreeSet);
        assertTrue(Suppliers.ofQueue().get() instanceof Queue);
        assertTrue(Suppliers.ofDeque().get() instanceof Deque);
        assertTrue(Suppliers.ofArrayDeque().get() instanceof ArrayDeque);
    }

    @Test
    public void testOfConcurrentCollectionSuppliers() {
        assertTrue(Suppliers.ofLinkedBlockingQueue().get() instanceof LinkedBlockingQueue);
        assertTrue(Suppliers.ofLinkedBlockingDeque().get() instanceof LinkedBlockingDeque);
        assertTrue(Suppliers.ofConcurrentLinkedQueue().get() instanceof ConcurrentLinkedQueue);
        assertTrue(Suppliers.ofPriorityQueue().get() instanceof PriorityQueue);
        assertTrue(Suppliers.ofConcurrentHashSet().get() instanceof Set);
    }

    @Test
    public void testOfMapSuppliers() {
        assertTrue(Suppliers.ofMap().get() instanceof HashMap);
        assertTrue(Suppliers.ofLinkedHashMap().get() instanceof LinkedHashMap);
        assertTrue(Suppliers.ofIdentityHashMap().get() instanceof IdentityHashMap);
        assertTrue(Suppliers.ofSortedMap().get() instanceof TreeMap);
        assertTrue(Suppliers.ofNavigableMap().get() instanceof TreeMap);
        assertTrue(Suppliers.ofTreeMap().get() instanceof TreeMap);
        assertTrue(Suppliers.ofConcurrentMap().get() instanceof ConcurrentHashMap);
        assertTrue(Suppliers.ofConcurrentHashMap().get() instanceof ConcurrentHashMap);
    }

    @Test
    public void testOfMultimapSuppliers() {
        assertTrue(Suppliers.ofBiMap().get() instanceof BiMap);
        assertTrue(Suppliers.ofMultiset().get() instanceof Multiset);
        assertTrue(Suppliers.ofListMultimap().get() instanceof ListMultimap);
        assertTrue(Suppliers.ofSetMultimap().get() instanceof SetMultimap);
    }

    @Test
    public void testOfListMultimapVariants() {
        Supplier<ListMultimap<String, Integer>> supplier1 = Suppliers.ofListMultimap(LinkedHashMap.class);
        assertNotNull(supplier1.get());

        Supplier<ListMultimap<String, Integer>> supplier2 = Suppliers.ofListMultimap(HashMap.class, ArrayList.class);
        assertNotNull(supplier2.get());

        Supplier<ListMultimap<String, Integer>> supplier3 = Suppliers.ofListMultimap(HashMap::new, ArrayList::new);
        assertNotNull(supplier3.get());
    }

    @Test
    public void testOfSetMultimapVariants() {
        Supplier<SetMultimap<String, Integer>> supplier1 = Suppliers.ofSetMultimap(LinkedHashMap.class);
        assertNotNull(supplier1.get());

        Supplier<SetMultimap<String, Integer>> supplier2 = Suppliers.ofSetMultimap(HashMap.class, HashSet.class);
        assertNotNull(supplier2.get());

        Supplier<SetMultimap<String, Integer>> supplier3 = Suppliers.ofSetMultimap(HashMap::new, HashSet::new);
        assertNotNull(supplier3.get());
    }

    @Test
    public void testDeprecatedImmutableMethods() {
        assertThrows(UnsupportedOperationException.class, Suppliers::ofImmutableList);
        assertThrows(UnsupportedOperationException.class, Suppliers::ofImmutableSet);
        assertThrows(UnsupportedOperationException.class, Suppliers::ofImmutableMap);
    }

    @Test
    public void testOfArrayFunctions() {
        assertArrayEquals(new boolean[5], IntFunctions.ofBooleanArray().apply(5));
        assertArrayEquals(new char[5], IntFunctions.ofCharArray().apply(5));
        assertArrayEquals(new byte[5], IntFunctions.ofByteArray().apply(5));
        assertArrayEquals(new short[5], IntFunctions.ofShortArray().apply(5));
        assertArrayEquals(new int[5], IntFunctions.ofIntArray().apply(5));
        assertArrayEquals(new long[5], IntFunctions.ofLongArray().apply(5));
        assertArrayEquals(new float[5], IntFunctions.ofFloatArray().apply(5), 0.0f);
        assertArrayEquals(new double[5], IntFunctions.ofDoubleArray().apply(5), 0.0);
        assertArrayEquals(new String[5], IntFunctions.ofStringArray().apply(5));
        assertArrayEquals(new Object[5], IntFunctions.ofObjectArray().apply(5));
    }

    @Test
    public void testOfCollectionFunctions() {
        assertTrue(IntFunctions.ofList().apply(10) instanceof ArrayList);
        assertTrue(IntFunctions.ofLinkedList().apply(10) instanceof LinkedList);

        assertTrue(IntFunctions.ofSet().apply(10) instanceof HashSet);
        assertTrue(IntFunctions.ofLinkedHashSet().apply(10) instanceof LinkedHashSet);
        assertTrue(IntFunctions.ofSortedSet().apply(10) instanceof TreeSet);
        assertTrue(IntFunctions.ofNavigableSet().apply(10) instanceof TreeSet);
        assertTrue(IntFunctions.ofTreeSet().apply(10) instanceof TreeSet);

        assertTrue(IntFunctions.ofQueue().apply(10) instanceof Queue);
        assertTrue(IntFunctions.ofDeque().apply(10) instanceof Deque);
        assertTrue(IntFunctions.ofArrayDeque().apply(10) instanceof ArrayDeque);
    }

    @Test
    public void testOfConcurrentCollectionFunctions() {
        assertTrue(IntFunctions.ofLinkedBlockingQueue().apply(10) instanceof LinkedBlockingQueue);
        assertEquals(10, ((LinkedBlockingQueue<?>) IntFunctions.ofLinkedBlockingQueue().apply(10)).remainingCapacity());

        assertTrue(IntFunctions.ofArrayBlockingQueue().apply(10) instanceof ArrayBlockingQueue);
        assertEquals(10, ((ArrayBlockingQueue<?>) IntFunctions.ofArrayBlockingQueue().apply(10)).remainingCapacity());

        assertTrue(IntFunctions.ofLinkedBlockingDeque().apply(10) instanceof LinkedBlockingDeque);
        assertTrue(IntFunctions.ofConcurrentLinkedQueue().apply(10) instanceof ConcurrentLinkedQueue);
        assertTrue(IntFunctions.ofPriorityQueue().apply(10) instanceof PriorityQueue);
    }

    @Test
    public void testOfMapFunctions() {
        assertTrue(IntFunctions.ofMap().apply(10) instanceof HashMap);
        assertTrue(IntFunctions.ofLinkedHashMap().apply(10) instanceof LinkedHashMap);
        assertTrue(IntFunctions.ofIdentityHashMap().apply(10) instanceof IdentityHashMap);
        assertTrue(IntFunctions.ofSortedMap().apply(10) instanceof TreeMap);
        assertTrue(IntFunctions.ofNavigableMap().apply(10) instanceof TreeMap);
        assertTrue(IntFunctions.ofTreeMap().apply(10) instanceof TreeMap);
        assertTrue(IntFunctions.ofConcurrentMap().apply(10) instanceof ConcurrentHashMap);
        assertTrue(IntFunctions.ofConcurrentHashMap().apply(10) instanceof ConcurrentHashMap);
    }

    @Test
    public void testOfMultimapFunctions() {
        assertTrue(IntFunctions.ofBiMap().apply(10) instanceof BiMap);
        assertTrue(IntFunctions.ofMultiset().apply(10) instanceof Multiset);
        assertTrue(IntFunctions.ofListMultimap().apply(10) instanceof ListMultimap);
        assertTrue(IntFunctions.ofSetMultimap().apply(10) instanceof SetMultimap);
    }

    @Test
    public void testOfDisposableArrayWithType() {
        IntFunction<DisposableArray<String>> func = IntFunctions.ofDisposableArray(String.class);
        DisposableArray<String> array1 = func.apply(5);
        DisposableArray<String> array2 = func.apply(10);

        assertNotNull(array1);
        assertNotNull(array2);
        assertSame(array1, array2);
    }

    @Test
    public void testFactoryExtendsIntFunctions() {
        assertTrue(IntFunctions.ofList().apply(10) instanceof ArrayList);
        assertTrue(IntFunctions.ofSet().apply(10) instanceof HashSet);
        assertTrue(IntFunctions.ofMap().apply(10) instanceof HashMap);
    }

    @Test
    public void testConcurrentDistinctBy() {
        Predicate<String> predicate = Fn.Predicates.concurrentDistinctBy(String::length);

        assertTrue(predicate.test("a"));
        assertTrue(predicate.test("ab"));
        assertFalse(predicate.test("c"));
        assertFalse(predicate.test("de"));
        assertTrue(predicate.test("abc"));
    }

    @Test
    public void testOfAdd() {
        List<String> list = new ArrayList<>();
        BiConsumer<List<String>, String> consumer = Fn.BiConsumers.ofAdd();

        consumer.accept(list, "a");
        consumer.accept(list, "b");

        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testOfAddAll() {
        List<String> list1 = new ArrayList<>();
        list1.add("a");

        List<String> list2 = Arrays.asList("b", "c");

        BiConsumer<List<String>, List<String>> consumer = Fn.BiConsumers.ofAddAll();
        consumer.accept(list1, list2);

        assertEquals(Arrays.asList("a", "b", "c"), list1);
    }

    @Test
    public void testOfRemove() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        BiConsumer<List<String>, String> consumer = Fn.BiConsumers.ofRemove();

        consumer.accept(list, "b");
        assertEquals(Arrays.asList("a", "c"), list);
    }

    @Test
    public void testOfRemoveAll() {
        List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        List<String> list2 = Arrays.asList("b", "d");

        BiConsumer<List<String>, List<String>> consumer = Fn.BiConsumers.ofRemoveAll();
        consumer.accept(list1, list2);

        assertEquals(Arrays.asList("a", "c"), list1);
    }

    @Test
    public void testOfPut() {
        Map<String, Integer> map = new HashMap<>();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);

        BiConsumer<Map<String, Integer>, Map.Entry<String, Integer>> consumer = Fn.BiConsumers.ofPut();
        consumer.accept(map, entry);

        assertEquals(123, map.get("key"));
    }

    @Test
    public void testOfMerge() {
        Joiner joiner1 = Joiner.with(",");
        joiner1.append("a").append("b");

        Joiner joiner2 = Joiner.with(",");
        joiner2.append("c").append("d");

        BiConsumer<Joiner, Joiner> consumer = Fn.BiConsumers.ofMerge();
        consumer.accept(joiner1, joiner2);

        assertEquals("a,b,c,d", joiner1.toString());
    }

    @Test
    public void testOfAppend() {
        StringBuilder sb = new StringBuilder("Hello");
        BiConsumer<StringBuilder, String> consumer = Fn.BiConsumers.ofAppend();

        consumer.accept(sb, " World");
        assertEquals("Hello World", sb.toString());
    }

    @Test
    public void testClassExists() {
        assertNotNull(Fn.TriConsumers.class);
    }

    @Test
    public void testOfAddAll_Deprecated() {
        List<String> list1 = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> list2 = Arrays.asList("c", "d");

        BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofAddAll();
        List<String> result = operator.apply(list1, list2);

        assertSame(list1, result);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testOfAddAllToFirst() {
        List<String> list1 = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> list2 = Arrays.asList("c", "d");

        BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofAddAllToFirst();
        List<String> result = operator.apply(list1, list2);

        assertSame(list1, result);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testOfAddAllToBigger() {
        List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> list2 = new ArrayList<>(Arrays.asList("d", "e"));

        BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofAddAllToBigger();

        List<String> result1 = operator.apply(list1, list2);
        assertSame(list1, result1);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result1);

        list1 = new ArrayList<>(Arrays.asList("a"));
        list2 = new ArrayList<>(Arrays.asList("b", "c"));

        List<String> result2 = operator.apply(list1, list2);
        assertSame(list2, result2);
        assertEquals(Arrays.asList("b", "c", "a"), result2);
    }

    @Test
    public void testOfRemoveAll_Deprecated() {
        List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        List<String> list2 = Arrays.asList("b", "d");

        BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofRemoveAll();
        List<String> result = operator.apply(list1, list2);

        assertSame(list1, result);
        assertEquals(Arrays.asList("a", "c"), result);
    }

    @Test
    public void testOfRemoveAllFromFirst() {
        List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        List<String> list2 = Arrays.asList("b", "d");

        BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofRemoveAllFromFirst();
        List<String> result = operator.apply(list1, list2);

        assertSame(list1, result);
        assertEquals(Arrays.asList("a", "c"), result);
    }

    @Test
    public void testOfMerge_Deprecated() {
        Joiner joiner1 = Joiner.with(",");
        joiner1.append("a").append("b");

        Joiner joiner2 = Joiner.with(",");
        joiner2.append("c").append("d");

        BinaryOperator<Joiner> operator = Fn.BinaryOperators.ofMerge();
        Joiner result = operator.apply(joiner1, joiner2);

        assertSame(joiner1, result);
        assertEquals("a,b,c,d", result.toString());
    }

    @Test
    public void testOfMergeToFirst() {
        Joiner joiner1 = Joiner.with(",");
        joiner1.append("a").append("b");

        Joiner joiner2 = Joiner.with(",");
        joiner2.append("c").append("d");

        BinaryOperator<Joiner> operator = Fn.BinaryOperators.ofMergeToFirst();
        Joiner result = operator.apply(joiner1, joiner2);

        assertSame(joiner1, result);
        assertEquals("a,b,c,d", result.toString());
    }

    @Test
    public void testOfAppend_Deprecated() {
        StringBuilder sb1 = new StringBuilder("Hello");
        StringBuilder sb2 = new StringBuilder(" World");

        BinaryOperator<StringBuilder> operator = Fn.BinaryOperators.ofAppend();
        StringBuilder result = operator.apply(sb1, sb2);

        assertSame(sb1, result);
        assertEquals("Hello World", result.toString());
    }

    @Test
    public void testOfAppendToFirst() {
        StringBuilder sb1 = new StringBuilder("Hello");
        StringBuilder sb2 = new StringBuilder(" World");

        BinaryOperator<StringBuilder> operator = Fn.BinaryOperators.ofAppendToFirst();
        StringBuilder result = operator.apply(sb1, sb2);

        assertSame(sb1, result);
        assertEquals("Hello World", result.toString());
    }

    @Test
    public void testOfAppendToBigger() {
        StringBuilder sb1 = new StringBuilder("Hello World");
        StringBuilder sb2 = new StringBuilder("!");

        BinaryOperator<StringBuilder> operator = Fn.BinaryOperators.ofAppendToBigger();

        StringBuilder result1 = operator.apply(sb1, sb2);
        assertSame(sb1, result1);
        assertEquals("Hello World!", result1.toString());

        sb1 = new StringBuilder("Hi");
        sb2 = new StringBuilder("Hello World");

        StringBuilder result2 = operator.apply(sb1, sb2);
        assertSame(sb2, result2);
        assertEquals("HiHello World", result2.toString());
    }

    @Test
    public void testOfConcat() {
        BinaryOperator<String> operator = Fn.BinaryOperators.ofConcat();
        assertEquals("HelloWorld", operator.apply("Hello", "World"));
        assertEquals("", operator.apply("", ""));
        assertEquals("test", operator.apply("test", ""));
    }

    @Test
    public void testOfAddInt() {
        BinaryOperator<Integer> operator = Fn.BinaryOperators.ofAddInt();
        assertEquals(5, operator.apply(2, 3));
        assertEquals(0, operator.apply(0, 0));
        assertEquals(-1, operator.apply(1, -2));
    }

    @Test
    public void testOfAddLong() {
        BinaryOperator<Long> operator = Fn.BinaryOperators.ofAddLong();
        assertEquals(5L, operator.apply(2L, 3L));
        assertEquals(0L, operator.apply(0L, 0L));
        assertEquals(-1L, operator.apply(1L, -2L));
    }

    @Test
    public void testOfAddDouble() {
        BinaryOperator<Double> operator = Fn.BinaryOperators.ofAddDouble();
        assertEquals(5.5, operator.apply(2.2, 3.3), 0.0001);
        assertEquals(0.0, operator.apply(0.0, 0.0), 0.0001);
        assertEquals(-1.0, operator.apply(1.5, -2.5), 0.0001);
    }

    @Test
    public void testOfAddBigInteger() {
        BinaryOperator<BigInteger> operator = Fn.BinaryOperators.ofAddBigInteger();

        BigInteger bi1 = new BigInteger("12345678901234567890");
        BigInteger bi2 = new BigInteger("98765432109876543210");
        BigInteger expected = new BigInteger("111111111011111111100");

        assertEquals(expected, operator.apply(bi1, bi2));
        assertEquals(BigInteger.ZERO, operator.apply(BigInteger.ZERO, BigInteger.ZERO));
    }

    @Test
    public void testOfAddBigDecimal() {
        BinaryOperator<BigDecimal> operator = Fn.BinaryOperators.ofAddBigDecimal();

        BigDecimal bd1 = new BigDecimal("123.456");
        BigDecimal bd2 = new BigDecimal("876.544");
        BigDecimal expected = new BigDecimal("1000.000");

        assertEquals(expected, operator.apply(bd1, bd2));
        assertEquals(BigDecimal.ZERO, operator.apply(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    public void testEf() {
        Throwables.BiFunction<String, Integer, String, Exception> biFunc = (k, v) -> k + ":" + v;
        Throwables.Function<Map.Entry<String, Integer>, String, Exception> function = Fn.Entries.ef(biFunc);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        try {
            assertEquals("key:123", function.apply(entry));
        } catch (Exception e) {
            fail("Should not throw exception");
        }

        assertThrows(IllegalArgumentException.class, () -> Fn.Entries.ef(null));
    }

    @Test
    public void testEp() {
        Throwables.BiPredicate<String, Integer, Exception> biPred = (k, v) -> v > 100;
        Throwables.Predicate<Map.Entry<String, Integer>, Exception> predicate = Fn.Entries.ep(biPred);

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 123);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", 50);

        try {
            assertTrue(predicate.test(entry1));
            assertFalse(predicate.test(entry2));
        } catch (Exception e) {
            fail("Should not throw exception");
        }

        assertThrows(IllegalArgumentException.class, () -> Fn.Entries.ep(null));
    }

    @Test
    public void testEc() {
        List<String> consumed = new ArrayList<>();
        Throwables.BiConsumer<String, Integer, Exception> biCons = (k, v) -> consumed.add(k + ":" + v);
        Throwables.Consumer<Map.Entry<String, Integer>, Exception> consumer = Fn.Entries.ec(biCons);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        try {
            consumer.accept(entry);
        } catch (Exception e) {
            fail("Should not throw exception");
        }

        assertEquals(Arrays.asList("key:123"), consumed);

        assertThrows(IllegalArgumentException.class, () -> Fn.Entries.ec(null));
    }

    @Test
    public void testToList() {
        Function<Pair<String, String>, List<String>> function = Fn.Pairs.toList();

        Pair<String, String> pair = Pair.of("left", "right");
        List<String> list = function.apply(pair);

        assertEquals(Arrays.asList("left", "right"), list);
    }

    @Test
    public void testCharBinaryOperators() {
        assertEquals('a', Fn.FC.CharBinaryOperators.MIN.applyAsChar('a', 'b'));
        assertEquals('a', Fn.FC.CharBinaryOperators.MIN.applyAsChar('b', 'a'));

        assertEquals('b', Fn.FC.CharBinaryOperators.MAX.applyAsChar('a', 'b'));
        assertEquals('b', Fn.FC.CharBinaryOperators.MAX.applyAsChar('b', 'a'));
    }

    @Test
    public void testByteBinaryOperators() {
        assertEquals((byte) 3, Fn.FB.ByteBinaryOperators.MIN.applyAsByte((byte) 3, (byte) 5));
        assertEquals((byte) 5, Fn.FB.ByteBinaryOperators.MAX.applyAsByte((byte) 3, (byte) 5));
    }

    @Test
    public void testShortBinaryOperators() {
        assertEquals((short) 3, Fn.FS.ShortBinaryOperators.MIN.applyAsShort((short) 3, (short) 5));
        assertEquals((short) 5, Fn.FS.ShortBinaryOperators.MAX.applyAsShort((short) 3, (short) 5));
    }

    @Test
    public void testLongBinaryOperators() {
        assertEquals(3L, Fn.FL.LongBinaryOperators.MIN.applyAsLong(3L, 5L));
        assertEquals(5L, Fn.FL.LongBinaryOperators.MAX.applyAsLong(3L, 5L));
    }

    @Test
    public void testFloatBinaryOperators() {
        assertEquals(3.0f, Fn.FF.FloatBinaryOperators.MIN.applyAsFloat(3.0f, 5.0f));
        assertEquals(5.0f, Fn.FF.FloatBinaryOperators.MAX.applyAsFloat(3.0f, 5.0f));
    }

    @Test
    public void testDoubleBinaryOperators() {
        assertEquals(3.0, Fn.FD.DoubleBinaryOperators.MIN.applyAsDouble(3.0, 5.0));
        assertEquals(5.0, Fn.FD.DoubleBinaryOperators.MAX.applyAsDouble(3.0, 5.0));
    }

    @Test
    public void testPublicStaticFieldsAsGetters() {
        assertEquals(true, Fn.GET_AS_BOOLEAN.applyAsBoolean(OptionalBoolean.of(true)));

        assertEquals('a', Fn.GET_AS_CHAR.applyAsChar(OptionalChar.of('a')));

        assertEquals((byte) 5, Fn.GET_AS_BYTE.applyAsByte(OptionalByte.of((byte) 5)));

        assertEquals((short) 10, Fn.GET_AS_SHORT.applyAsShort(OptionalShort.of((short) 10)));

        assertEquals(100, Fn.GET_AS_INT.applyAsInt(OptionalInt.of(100)));

        assertEquals(1000L, Fn.GET_AS_LONG.applyAsLong(OptionalLong.of(1000L)));

        assertEquals(3.14f, Fn.GET_AS_FLOAT.applyAsFloat(OptionalFloat.of(3.14f)), 0.001f);

        assertEquals(2.718, Fn.GET_AS_DOUBLE.applyAsDouble(OptionalDouble.of(2.718)), 0.001);

        assertEquals(100, Fn.GET_AS_INT_JDK.applyAsInt(java.util.OptionalInt.of(100)));

        assertEquals(1000L, Fn.GET_AS_LONG_JDK.applyAsLong(java.util.OptionalLong.of(1000L)));

        assertEquals(2.718, Fn.GET_AS_DOUBLE_JDK.applyAsDouble(java.util.OptionalDouble.of(2.718)), 0.001);
    }

    @Test
    public void testPublicStaticFieldsAsPredicates() {
        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.empty()));

        assertTrue(Fn.IS_PRESENT_CHAR.test(OptionalChar.of('a')));
        assertFalse(Fn.IS_PRESENT_CHAR.test(OptionalChar.empty()));

        assertTrue(Fn.IS_PRESENT_BYTE.test(OptionalByte.of((byte) 5)));
        assertFalse(Fn.IS_PRESENT_BYTE.test(OptionalByte.empty()));

        assertTrue(Fn.IS_PRESENT_SHORT.test(OptionalShort.of((short) 10)));
        assertFalse(Fn.IS_PRESENT_SHORT.test(OptionalShort.empty()));

        assertTrue(Fn.IS_PRESENT_INT.test(OptionalInt.of(100)));
        assertFalse(Fn.IS_PRESENT_INT.test(OptionalInt.empty()));

        assertTrue(Fn.IS_PRESENT_LONG.test(OptionalLong.of(1000L)));
        assertFalse(Fn.IS_PRESENT_LONG.test(OptionalLong.empty()));

        assertTrue(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.of(3.14f)));
        assertFalse(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.empty()));

        assertTrue(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.of(2.718)));
        assertFalse(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.empty()));

        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(100)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));

        assertTrue(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.of(1000L)));
        assertFalse(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.empty()));

        assertTrue(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.of(2.718)));
        assertFalse(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.empty()));
    }

    @Test
    public void testOptionalOperations() {
        OptionalBoolean ob = OptionalBoolean.of(true);
        assertTrue(Fn.GET_AS_BOOLEAN.applyAsBoolean(ob));

        OptionalChar oc = OptionalChar.of('A');
        assertEquals('A', Fn.GET_AS_CHAR.applyAsChar(oc));

        OptionalByte oby = OptionalByte.of((byte) 42);
        assertEquals(42, Fn.GET_AS_BYTE.applyAsByte(oby));

        OptionalShort os = OptionalShort.of((short) 42);
        assertEquals(42, Fn.GET_AS_SHORT.applyAsShort(os));

        OptionalFloat of = OptionalFloat.of(42.5f);
        assertEquals(42.5f, Fn.GET_AS_FLOAT.applyAsFloat(of), 0.001f);

        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.empty()));

        assertTrue(Fn.IS_PRESENT_CHAR.test(OptionalChar.of('A')));
        assertFalse(Fn.IS_PRESENT_CHAR.test(OptionalChar.empty()));

        assertTrue(Fn.IS_PRESENT_BYTE.test(OptionalByte.of((byte) 1)));
        assertFalse(Fn.IS_PRESENT_BYTE.test(OptionalByte.empty()));

        assertTrue(Fn.IS_PRESENT_SHORT.test(OptionalShort.of((short) 1)));
        assertFalse(Fn.IS_PRESENT_SHORT.test(OptionalShort.empty()));

        assertTrue(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.of(1.0f)));
        assertFalse(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.empty()));
    }

    @Test
    public void testJdkOptionalOperations() {
        java.util.OptionalInt oi = java.util.OptionalInt.of(42);
        assertEquals(42, Fn.GET_AS_INT_JDK.applyAsInt(oi));

        java.util.OptionalLong ol = java.util.OptionalLong.of(42L);
        assertEquals(42L, Fn.GET_AS_LONG_JDK.applyAsLong(ol));

        java.util.OptionalDouble od = java.util.OptionalDouble.of(42.5);
        assertEquals(42.5, Fn.GET_AS_DOUBLE_JDK.applyAsDouble(od), 0.001);

        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));

        assertTrue(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.empty()));

        assertTrue(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.empty()));
    }

    @Test
    public void testGetAsInt() {
        OptionalInt opt = OptionalInt.of(42);
        assertEquals(42, Fn.GET_AS_INT.applyAsInt(opt));
    }

    @Test
    public void testGetAsLong() {
        OptionalLong opt = OptionalLong.of(42L);
        assertEquals(42L, Fn.GET_AS_LONG.applyAsLong(opt));
    }

    @Test
    public void testGetAsDouble() {
        OptionalDouble opt = OptionalDouble.of(42.5);
        assertEquals(42.5, Fn.GET_AS_DOUBLE.applyAsDouble(opt), 0.001);
    }

    @Test
    public void testEntries_pp() {
        Map.Entry<String, Integer> entry = new java.util.AbstractMap.SimpleEntry<>("key", 1);
        Predicate<Map.Entry<String, Integer>> pred = Entries.pp((k, v) -> v > 0);
        assertTrue(pred.test(entry));
    }

    @Test
    public void testEntries_cc() {
        Map.Entry<String, Integer> entry = new java.util.AbstractMap.SimpleEntry<>("key", 42);
        AtomicInteger holder = new AtomicInteger();
        Consumer<Map.Entry<String, Integer>> consumer = Entries.cc((k, v) -> holder.set(v));
        consumer.accept(entry);
        assertEquals(42, holder.get());
    }

    @Test
    public void testBiFunctions_ofPut() {
        Map<String, Integer> map = new HashMap<>();
        Map.Entry<String, Integer> entry = new java.util.AbstractMap.SimpleEntry<>("key", 99);
        BiFunctions.<String, Integer, Map<String, Integer>, Map.Entry<String, Integer>> ofPut().apply(map, entry);
        assertEquals(99, (int) map.get("key"));
    }

    @Test
    public void testBiFunctions_ofRemoveByKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        BiFunctions.<String, Integer, Map<String, Integer>> ofRemoveByKey().apply(map, "x");
        assertFalse(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
    }

    @Test
    public void testBiFunctions_ofAppend() {
        StringBuilder sb = new StringBuilder("hello");
        BiFunctions.<String> ofAppend().apply(sb, " world");
        assertEquals("hello world", sb.toString());
    }

}
