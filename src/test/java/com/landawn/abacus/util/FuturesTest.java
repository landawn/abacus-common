package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;

public class FuturesTest extends TestBase {

    @Test
    public void testAnyOfCancellationExceptionFromComputationIsFailure() {
        for (int mode = 0; mode < 3; mode++) {
            final int getMode = mode;
            final CancellationException cause = new CancellationException("computation failed");
            final FutureTask<String> failed = new FutureTask<>(() -> { throw cause; });
            failed.run();
            final ContinuableFuture<String> aggregate = Futures.anyOf(failed);
            final ExecutionException first = assertThrows(ExecutionException.class, () -> {
                if (getMode == 0) {
                    aggregate.get();
                } else {
                    aggregate.get(getMode == 1 ? 1 : 0, TimeUnit.SECONDS);
                }
            });
            Assertions.assertSame(cause, first.getCause());
            assertFalse(failed.isCancelled());
            assertFalse(aggregate.isCancelled());
            assertTrue(aggregate.isDone());
            Assertions.assertSame(cause, assertThrows(ExecutionException.class, aggregate::get).getCause());
            Assertions.assertSame(cause, assertThrows(ExecutionException.class, () -> aggregate.get(0, TimeUnit.SECONDS)).getCause());

            final FutureTask<String> cancelled = new FutureTask<>(() -> "unused");
            cancelled.cancel(false);
            final ContinuableFuture<String> mixed = Futures.anyOf(cancelled, failed);
            final ExecutionException mixedFailure = assertThrows(ExecutionException.class, mixed::get);
            assertInstanceOf(CancellationException.class, mixedFailure.getCause());
            assertFalse(mixed.isCancelled());

            final ContinuableFuture<String> allCancelled = Futures.anyOf(cancelled);
            assertThrows(CancellationException.class, allCancelled::get);
            assertTrue(allCancelled.isCancelled());
            assertThrows(CancellationException.class, () -> allCancelled.get(1, TimeUnit.SECONDS));
        }
    }

    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    public void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    public void testComposeTwoFuturesWithBiFunction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(10);
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("20");

        Throwables.BiFunction<Future<Integer>, Future<String>, String, Exception> zipFunction = (f1, f2) -> f1.get() + f2.get();

        ContinuableFuture<String> result = Futures.compose(cf1, cf2, zipFunction);

        assertEquals("1020", result.get());
        assertTrue(result.isDone());
        assertFalse(result.isCancelled());
    }

    @Test
    public void testComposeTwoFuturesWithBiFunctionException() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(10);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();
        cf2.completeExceptionally(new RuntimeException("Test exception"));

        Throwables.BiFunction<Future<Integer>, Future<Integer>, Integer, Exception> zipFunction = (f1, f2) -> f1.get() + f2.get();

        ContinuableFuture<Integer> result = Futures.compose(cf1, cf2, zipFunction);

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testComposeTwoFuturesWithTimeoutFunction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(5);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(10);

        Throwables.BiFunction<Future<Integer>, Future<Integer>, Integer, Exception> zipFunction = (f1, f2) -> f1.get() + f2.get();
        Throwables.Function<Tuple4<Future<Integer>, Future<Integer>, Long, TimeUnit>, Integer, Exception> timeoutFunction = t -> t._1.get(t._3, t._4)
                + t._2.get(t._3, t._4);

        ContinuableFuture<Integer> result = Futures.compose(cf1, cf2, zipFunction, timeoutFunction);

        assertEquals(15, result.get());
        assertEquals(15, result.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testComposeTwoFuturesCancel() {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        Throwables.BiFunction<Future<Integer>, Future<Integer>, Integer, Exception> zipFunction = (f1, f2) -> f1.get() + f2.get();

        ContinuableFuture<Integer> result = Futures.compose(cf1, cf2, zipFunction);

        assertTrue(result.cancel(true));
        assertTrue(cf1.isCancelled());
        assertTrue(cf2.isCancelled());
        assertTrue(result.isCancelled());
    }

    @Test
    public void testComposeTwoFuturesIsDone() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        Throwables.BiFunction<Future<Integer>, Future<Integer>, Integer, Exception> zipFunction = (f1, f2) -> f1.get() + f2.get();

        ContinuableFuture<Integer> result = Futures.compose(cf1, cf2, zipFunction);

        assertFalse(result.isDone());
        cf2.complete(2);
        assertTrue(result.isDone());
    }

    @Test
    public void testComposeThreeFuturesWithTriFunction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(2);
        CompletableFuture<Integer> cf3 = CompletableFuture.completedFuture(3);

        Throwables.TriFunction<Future<Integer>, Future<Integer>, Future<Integer>, Integer, Exception> zipFunction = (f1, f2, f3) -> f1.get() + f2.get()
                + f3.get();

        ContinuableFuture<Integer> result = Futures.compose(cf1, cf2, cf3, zipFunction);

        assertEquals(6, result.get());
        assertTrue(result.isDone());
    }

    @Test
    public void testComposeThreeFuturesCancel() {
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new CompletableFuture<>();
        CompletableFuture<String> cf3 = new CompletableFuture<>();

        Throwables.TriFunction<Future<String>, Future<String>, Future<String>, String, Exception> zipFunction = (f1, f2, f3) -> f1.get() + f2.get() + f3.get();

        ContinuableFuture<String> result = Futures.compose(cf1, cf2, cf3, zipFunction);

        assertTrue(result.cancel(true));
        assertTrue(cf1.isCancelled());
        assertTrue(cf2.isCancelled());
        assertTrue(cf3.isCancelled());
    }

    @Test
    public void testComposeThreeFuturesWithTimeoutFunction() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("A");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("B");
        CompletableFuture<String> cf3 = CompletableFuture.completedFuture("C");

        Throwables.TriFunction<Future<String>, Future<String>, Future<String>, String, Exception> zipFunction = (f1, f2, f3) -> f1.get() + f2.get() + f3.get();
        Throwables.Function<Tuple5<Future<String>, Future<String>, Future<String>, Long, TimeUnit>, String, Exception> timeoutFunction = t -> t._1.get(t._4,
                t._5) + t._2.get(t._4, t._5) + t._3.get(t._4, t._5);

        ContinuableFuture<String> result = Futures.compose(cf1, cf2, cf3, zipFunction, timeoutFunction);

        assertEquals("ABC", result.get());
        assertEquals("ABC", result.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testComposeCollectionWithFunction() throws Exception {
        List<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(1), CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3));

        Throwables.Function<List<Future<? extends Integer>>, Integer, Exception> zipFunction = list -> {
            int sum = 0;
            for (Future<? extends Integer> f : list) {
                sum += f.get();
            }
            return sum;
        };

        ContinuableFuture<Integer> result = Futures.compose(cfs, zipFunction);

        assertEquals(6, result.get());
    }

    @Test
    public void testComposeCollectionEmpty() {
        List<Future<Integer>> cfs = new ArrayList<>();

        Throwables.Function<List<Future<? extends Integer>>, Integer, Exception> zipFunction = list -> 0;

        assertThrows(IllegalArgumentException.class, () -> Futures.compose(cfs, zipFunction));
    }

    @Test
    public void testComposeCollectionNull() {
        Throwables.Function<List<Future<? extends Integer>>, Integer, Exception> zipFunction = list -> 0;

        assertThrows(IllegalArgumentException.class, () -> Futures.compose(null, zipFunction));
    }

    @Test
    public void testComposeCollectionWithTimeoutFunction() throws Exception {
        List<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(10), CompletableFuture.completedFuture(20));

        Throwables.Function<List<Future<? extends Integer>>, Integer, Exception> zipFunction = list -> {
            int sum = 0;
            for (Future<? extends Integer> f : list) {
                sum += f.get();
            }
            return sum;
        };

        Throwables.Function<Tuple3<List<Future<? extends Integer>>, Long, TimeUnit>, Integer, Exception> timeoutFunction = t -> {
            int sum = 0;
            for (Future<? extends Integer> f : t._1) {
                sum += f.get(t._2, t._3);
            }
            return sum;
        };

        ContinuableFuture<Integer> result = Futures.compose(cfs, zipFunction, timeoutFunction);

        assertEquals(30, result.get());
        assertEquals(30, result.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testComposeCollectionWithTimeoutFunctionNullArgs() {
        List<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(1));
        Throwables.Function<List<Future<? extends Integer>>, Integer, Exception> zipFunction = list -> 0;

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Futures.compose(cfs, null, null));
    }

    @Test
    public void testComposeWithBiFunctionAndTimeoutFunction() throws Exception {
        Future<String> slowFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Slow";
        });
        Future<String> fastFuture = CompletableFuture.completedFuture("Fast");

        ContinuableFuture<String> composed = Futures.compose(slowFuture, fastFuture, (f1, f2) -> f1.get() + " + " + f2.get(), tuple -> {
            try {
                return "Timeout: " + tuple._2.get(tuple._3, tuple._4);
            } catch (Exception e) {
                return "Timeout occurred";
            }
        });

        Assertions.assertEquals("Slow + Fast", composed.get());

        Future<String> verySlowFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Very Slow";
        });

        ContinuableFuture<String> composed2 = Futures.compose(verySlowFuture, fastFuture, (f1, f2) -> f1.get() + " + " + f2.get(),
                tuple -> "Timeout: " + tuple._2.get());

        String result = composed2.get(100, TimeUnit.MILLISECONDS);
        Assertions.assertEquals("Timeout: Fast", result);
    }

    @Test
    public void testComposeWithThreeFuturesAndTimeoutFunction() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("A");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("B");
        CompletableFuture<String> cf3 = CompletableFuture.completedFuture("C");

        Throwables.TriFunction<Future<String>, Future<String>, Future<String>, String, Exception> zipFunction = (f1, f2, f3) -> f1.get() + f2.get() + f3.get();
        Throwables.Function<Tuple5<Future<String>, Future<String>, Future<String>, Long, TimeUnit>, String, Exception> timeoutFunction = t -> t._1.get()
                + t._2.get() + t._3.get();

        ContinuableFuture<String> result = Futures.compose(cf1, cf2, cf3, zipFunction, timeoutFunction);

        assertEquals("ABC", result.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testComposeWithCollectionAndTimeoutFunction() throws Exception {
        List<CompletableFuture<String>> futures = Arrays.asList(CompletableFuture.completedFuture("X"), CompletableFuture.completedFuture("Y"),
                CompletableFuture.completedFuture("Z"));

        Throwables.Function<List<Future<? extends String>>, String, Exception> zipFunction = list -> {
            StringBuilder sb = new StringBuilder();
            for (Future<? extends String> f : list) {
                sb.append(f.get());
            }
            return sb.toString();
        };
        Throwables.Function<Tuple3<List<Future<? extends String>>, Long, TimeUnit>, String, Exception> timeoutFunction = t -> {
            StringBuilder sb = new StringBuilder();
            for (Future<? extends String> f : t._1) {
                sb.append(f.get(t._2, t._3));
            }
            return sb.toString();
        };

        ContinuableFuture<String> result = Futures.compose(futures, zipFunction, timeoutFunction);

        assertEquals("XYZ", result.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testComposeTwoFuturesReturnsNull() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("A");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("B");

        ContinuableFuture<String> result = Futures.compose(cf1, cf2, (f1, f2) -> null);

        assertEquals(null, result.get());
    }

    @Test
    public void testComposeThreeFuturesWithException() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.failedFuture(new RuntimeException("fail"));
        CompletableFuture<Integer> cf3 = CompletableFuture.completedFuture(3);

        ContinuableFuture<Integer> result = Futures.compose(cf1, cf2, cf3, (f1, f2, f3) -> f1.get() + f2.get() + f3.get());

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testComposeCollectionSingleElement() throws Exception {
        List<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.completedFuture(42));

        ContinuableFuture<Integer> result = Futures.compose(futures, list -> list.get(0).get());

        assertEquals(42, result.get());
    }

    @Test
    public void testCombineTwoFutures() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(10);
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("Hello");

        ContinuableFuture<Tuple2<Integer, String>> result = Futures.combine(cf1, cf2);

        Tuple2<Integer, String> tuple = result.get();
        assertEquals(10, tuple._1);
        assertEquals("Hello", tuple._2);
    }

    @Test
    public void testCombineThreeFutures() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("Two");
        CompletableFuture<Double> cf3 = CompletableFuture.completedFuture(3.0);

        ContinuableFuture<Tuple3<Integer, String, Double>> result = Futures.combine(cf1, cf2, cf3);

        Tuple3<Integer, String, Double> tuple = result.get();
        assertEquals(1, tuple._1);
        assertEquals("Two", tuple._2);
        assertEquals(3.0, tuple._3);
    }

    @Test
    public void testCombineFourFutures() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(2);
        CompletableFuture<Integer> cf3 = CompletableFuture.completedFuture(3);
        CompletableFuture<Integer> cf4 = CompletableFuture.completedFuture(4);

        ContinuableFuture<Tuple4<Integer, Integer, Integer, Integer>> result = Futures.combine(cf1, cf2, cf3, cf4);

        Tuple4<Integer, Integer, Integer, Integer> tuple = result.get();
        assertEquals(1, tuple._1);
        assertEquals(2, tuple._2);
        assertEquals(3, tuple._3);
        assertEquals(4, tuple._4);
    }

    @Test
    public void testCombineFiveFutures() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("A");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("B");
        CompletableFuture<String> cf3 = CompletableFuture.completedFuture("C");
        CompletableFuture<String> cf4 = CompletableFuture.completedFuture("D");
        CompletableFuture<String> cf5 = CompletableFuture.completedFuture("E");

        ContinuableFuture<Tuple5<String, String, String, String, String>> result = Futures.combine(cf1, cf2, cf3, cf4, cf5);

        Tuple5<String, String, String, String, String> tuple = result.get();
        assertEquals("A", tuple._1);
        assertEquals("B", tuple._2);
        assertEquals("C", tuple._3);
        assertEquals("D", tuple._4);
        assertEquals("E", tuple._5);
    }

    @Test
    public void testCombineSixFutures() throws Exception {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            futures.add(CompletableFuture.completedFuture(i));
        }

        ContinuableFuture<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> result = Futures.combine(futures.get(0), futures.get(1), futures.get(2),
                futures.get(3), futures.get(4), futures.get(5));

        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple = result.get();
        assertEquals(1, tuple._1);
        assertEquals(2, tuple._2);
        assertEquals(3, tuple._3);
        assertEquals(4, tuple._4);
        assertEquals(5, tuple._5);
        assertEquals(6, tuple._6);
    }

    @Test
    public void testCombineSevenFutures() throws Exception {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        String[] values = { "A", "B", "C", "D", "E", "F", "G" };
        for (String val : values) {
            futures.add(CompletableFuture.completedFuture(val));
        }

        ContinuableFuture<Tuple7<String, String, String, String, String, String, String>> result = Futures.combine(futures.get(0), futures.get(1),
                futures.get(2), futures.get(3), futures.get(4), futures.get(5), futures.get(6));

        Tuple7<String, String, String, String, String, String, String> tuple = result.get();
        assertEquals("A", tuple._1);
        assertEquals("B", tuple._2);
        assertEquals("C", tuple._3);
        assertEquals("D", tuple._4);
        assertEquals("E", tuple._5);
        assertEquals("F", tuple._6);
        assertEquals("G", tuple._7);
    }

    @Test
    public void testCombineTwoFuturesWithAction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(10);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(20);

        Throwables.BiFunction<Integer, Integer, Integer, Exception> action = (a, b) -> a + b;

        ContinuableFuture<Integer> result = Futures.combine(cf1, cf2, action);

        assertEquals(30, result.get());
    }

    @Test
    public void testCombineThreeFuturesWithAction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(5);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(10);
        CompletableFuture<Integer> cf3 = CompletableFuture.completedFuture(15);

        Throwables.TriFunction<Integer, Integer, Integer, Integer, Exception> action = (a, b, c) -> a + b + c;

        ContinuableFuture<Integer> result = Futures.combine(cf1, cf2, cf3, action);

        assertEquals(30, result.get());
    }

    @Test
    public void testCombineCollectionWithAction() throws Exception {
        List<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(1), CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3), CompletableFuture.completedFuture(4));

        Throwables.Function<List<Integer>, Integer, Exception> action = list -> list.stream().mapToInt(Integer::intValue).sum();

        ContinuableFuture<Integer> result = Futures.combine(cfs, action);

        assertEquals(10, result.get());
    }

    @Test
    public void testCombineFourToSevenFutures() throws Exception {
        ContinuableFuture<Tuple4<Integer, Integer, Integer, Integer>> t4 = Futures.combine(CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2), CompletableFuture.completedFuture(3), CompletableFuture.completedFuture(4));
        Assertions.assertEquals(Tuple.of(1, 2, 3, 4), t4.get());

        ContinuableFuture<Tuple5<Integer, Integer, Integer, Integer, Integer>> t5 = Futures.combine(CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2), CompletableFuture.completedFuture(3), CompletableFuture.completedFuture(4),
                CompletableFuture.completedFuture(5));
        Assertions.assertEquals(Tuple.of(1, 2, 3, 4, 5), t5.get());

        ContinuableFuture<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> t6 = Futures.combine(CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2), CompletableFuture.completedFuture(3), CompletableFuture.completedFuture(4),
                CompletableFuture.completedFuture(5), CompletableFuture.completedFuture(6));
        Assertions.assertEquals(Tuple.of(1, 2, 3, 4, 5, 6), t6.get());

        ContinuableFuture<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> t7 = Futures.combine(CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2), CompletableFuture.completedFuture(3), CompletableFuture.completedFuture(4),
                CompletableFuture.completedFuture(5), CompletableFuture.completedFuture(6), CompletableFuture.completedFuture(7));
        Assertions.assertEquals(Tuple.of(1, 2, 3, 4, 5, 6, 7), t7.get());
    }

    @Test
    public void testCombineWithAction() throws Exception {
        Future<Integer> f1 = CompletableFuture.completedFuture(10);
        Future<Integer> f2 = CompletableFuture.completedFuture(20);

        ContinuableFuture<Integer> result = Futures.combine(f1, f2, (a, b) -> a + b);
        Assertions.assertEquals(30, result.get());

        Future<Integer> f3 = CompletableFuture.completedFuture(30);
        ContinuableFuture<Integer> result3 = Futures.combine(f1, f2, f3, (a, b, c) -> a + b + c);
        Assertions.assertEquals(60, result3.get());
    }

    @Test
    public void testCombineThreeFuturesWithTriFunction() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("A");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("B");
        CompletableFuture<String> cf3 = CompletableFuture.completedFuture("C");

        Throwables.TriFunction<String, String, String, String, Exception> action = (a, b, c) -> a + b + c;

        ContinuableFuture<String> result = Futures.combine(cf1, cf2, cf3, action);

        assertEquals("ABC", result.get());
    }

    @Test
    public void testCombineCollectionWithFunction() throws Exception {
        Collection<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.completedFuture(1), CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3), CompletableFuture.completedFuture(4));

        Throwables.Function<List<Integer>, Integer, Exception> action = list -> list.stream().mapToInt(Integer::intValue).sum();

        ContinuableFuture<Integer> result = Futures.combine(futures, action);

        assertEquals(10, result.get());
    }

    @Test
    public void testCombineTwoFuturesWithException() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.failedFuture(new RuntimeException("error"));

        ContinuableFuture<Tuple2<Integer, Integer>> result = Futures.combine(cf1, cf2);

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testCombineThreeFuturesWithException() {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("A");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("B");
        CompletableFuture<String> cf3 = CompletableFuture.failedFuture(new RuntimeException("fail"));

        ContinuableFuture<Tuple3<String, String, String>> result = Futures.combine(cf1, cf2, cf3);

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testCombineTwoFuturesWithActionException() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.failedFuture(new RuntimeException("fail"));

        ContinuableFuture<Integer> result = Futures.combine(cf1, cf2, (Throwables.BiFunction<Integer, Integer, Integer, Exception>) (a, b) -> a + b);

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testCombineCollectionWithActionException() {
        Collection<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.completedFuture(1),
                CompletableFuture.failedFuture(new RuntimeException("fail")));

        ContinuableFuture<Integer> result = Futures.combine(futures,
                (Throwables.Function<List<Integer>, Integer, Exception>) list -> list.stream().mapToInt(Integer::intValue).sum());

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testAllOfVarargsWithTimeout() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("A");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("B");

        ContinuableFuture<List<String>> result = Futures.allOf(cf1, cf2);

        List<String> list = result.get(1, TimeUnit.SECONDS);
        assertEquals(2, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
    }

    @Test
    public void testAllOf() throws Exception {
        Future<String> f1 = CompletableFuture.completedFuture("A");
        Future<String> f2 = CompletableFuture.completedFuture("B");
        Future<String> f3 = CompletableFuture.completedFuture("C");

        ContinuableFuture<List<String>> allArray = Futures.allOf(f1, f2, f3);
        List<String> resultArray = allArray.get();
        Assertions.assertEquals(Arrays.asList("A", "B", "C"), resultArray);

        List<Future<String>> futures = Arrays.asList(f1, f2, f3);
        ContinuableFuture<List<String>> allCollection = Futures.allOf(futures);
        List<String> resultCollection = allCollection.get();
        Assertions.assertEquals(Arrays.asList("A", "B", "C"), resultCollection);
    }

    @Test
    public void testAllOfWithFailure() {
        Future<String> f1 = CompletableFuture.completedFuture("A");
        Future<String> f2 = CompletableFuture.failedFuture(new RuntimeException("Failed"));
        Future<String> f3 = CompletableFuture.completedFuture("C");

        ContinuableFuture<List<String>> all = Futures.allOf(f1, f2, f3);

        Assertions.assertThrows(ExecutionException.class, () -> all.get());
    }

    @Test
    public void testAllOfWithTimeout() throws Exception {
        CompletableFuture<String> f1 = new CompletableFuture<>();
        CompletableFuture<String> f2 = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                Thread.sleep(100);
                f1.complete("A");
                Thread.sleep(100);
                f2.complete("B");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        ContinuableFuture<List<String>> all = Futures.allOf(f1, f2);
        List<String> result = all.get(500, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(Arrays.asList("A", "B"), result);

        CompletableFuture<String> f3 = new CompletableFuture<>();
        ContinuableFuture<List<String>> all2 = Futures.allOf(f3);
        Assertions.assertThrows(TimeoutException.class, () -> all2.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testEmptyCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Futures.allOf(Collections.emptyList());
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Futures.anyOf(Collections.emptyList());
        });
    }

    @Test
    public void testCancelCompositeFuture() throws Exception {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ContinuableFuture<List<Integer>> allOfFuture = Futures.allOf(cf1, cf2);

        assertFalse(allOfFuture.isDone());
        assertFalse(allOfFuture.isCancelled());

        boolean cancelled = allOfFuture.cancel(true);

        assertTrue(cancelled);
        assertTrue(allOfFuture.isCancelled());
        assertTrue(cf1.isCancelled());
        assertTrue(cf2.isCancelled());
    }

    @Test
    public void testIsDoneForAllOf() throws Exception {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(2);

        ContinuableFuture<List<Integer>> allOfFuture = Futures.allOf(cf1, cf2);

        assertFalse(allOfFuture.isDone());

        cf1.complete(1);

        assertTrue(allOfFuture.isDone());
        assertEquals(Arrays.asList(1, 2), allOfFuture.get());
    }

    @Test
    public void testAllOfWithMultipleExceptions() {
        CompletableFuture<Integer> cf1 = CompletableFuture.failedFuture(new RuntimeException("Error 1"));
        CompletableFuture<Integer> cf2 = CompletableFuture.failedFuture(new RuntimeException("Error 2"));

        ContinuableFuture<List<Integer>> result = Futures.allOf(cf1, cf2);

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    @Timeout(10)
    public void testAllOfWithManyFutures() throws Exception {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            final int value = i;
            futures.add(CompletableFuture.supplyAsync(() -> value, executor));
        }

        ContinuableFuture<List<Integer>> result = Futures.allOf(futures);
        List<Integer> list = result.get(5, TimeUnit.SECONDS);

        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    public void testAllOfVarargsSingleFuture() throws Exception {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("only");

        ContinuableFuture<List<String>> result = Futures.allOf(cf);

        assertEquals(Arrays.asList("only"), result.get());
    }

    @Test
    public void testAnyOfVarargsCancel() {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ContinuableFuture<Integer> result = Futures.anyOf(cf1, cf2);

        assertTrue(result.cancel(true));
        assertTrue(cf1.isCancelled());
        assertTrue(cf2.isCancelled());
    }

    @Test
    @Timeout(10)
    public void testAnyOfReturnsSiblingSuccessWhenOneFutureFailsWithInterruptedCause() throws Exception {
        // An input future that FAILED with an InterruptedException cause must not be mistaken for a
        // caller-side interruption: anyOf must still return a sibling future's successful result.
        // ('failed' is registered first, so its result is enqueued and observed before 'ok'.)
        final CompletableFuture<String> failed = CompletableFuture.failedFuture(new InterruptedException("boom"));
        final CompletableFuture<String> ok = CompletableFuture.completedFuture("ok");

        final String result = Futures.anyOf(failed, ok).get();
        assertEquals("ok", result);
        assertFalse(Thread.currentThread().isInterrupted(), "caller thread must not be left interrupted");
    }

    @Test
    public void testAnyOfVarargsIsCancelled() {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();
        cf1.cancel(true);
        cf2.cancel(true);

        ContinuableFuture<Integer> result = Futures.anyOf(cf1, cf2);

        assertTrue(result.isCancelled());
    }

    @Test
    public void testAnyOfTimedGetDistinguishesInputTimeoutFailureFromDeadline() throws Exception {
        final TimeoutException inputFailure = new TimeoutException("input failed before the aggregate deadline");
        final CompletableFuture<String> failed = CompletableFuture.failedFuture(inputFailure);
        final CompletableFuture<String> successful = CompletableFuture.completedFuture("ok");

        assertEquals("ok", Futures.anyOf(failed, successful).get(1, TimeUnit.SECONDS));

        final ExecutionException failure = assertThrows(ExecutionException.class, () -> Futures.anyOf(failed).get(1, TimeUnit.SECONDS));
        assertEquals(inputFailure, failure.getCause());
        assertThrows(TimeoutException.class, () -> Futures.anyOf(new CompletableFuture<String>()).get(1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAnyOfInputTimeoutFailureAcrossFutureImplementationsAndPolls() throws Exception {
        for (final long timeout : new long[] { 0, 10 }) {
            final TimeoutException firstFailure = new TimeoutException("input timeout");
            final IllegalStateException secondFailure = new IllegalStateException("second input failure");
            final java.util.concurrent.FutureTask<String> failed = new java.util.concurrent.FutureTask<>(() -> {
                throw firstFailure;
            });
            failed.run();

            assertEquals("ok", Futures.anyOf(Arrays.asList(failed, CompletableFuture.completedFuture("ok"))).get(timeout, TimeUnit.SECONDS));
            final ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> Futures.anyOf(Arrays.asList(failed, CompletableFuture.failedFuture(secondFailure))).get(timeout, TimeUnit.SECONDS));
            // Completed CompletableFuture callbacks and ordinary Future reads may be enqueued in either order.
            final Throwable cause = failure.getCause();
            assertTrue(cause == firstFailure || cause == secondFailure);
            Assertions.assertArrayEquals(new Throwable[] { cause == firstFailure ? secondFailure : firstFailure }, cause.getSuppressed());
        }
    }

    @Test
    public void testAnyOfVarargsIsDone() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ContinuableFuture<Integer> result = Futures.anyOf(cf1, cf2);

        assertTrue(result.isDone());
    }

    @Test
    public void testAnyOfVarargs() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(100);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();
        CompletableFuture<Integer> cf3 = new CompletableFuture<>();

        ContinuableFuture<Integer> result = Futures.anyOf(cf1, cf2, cf3);

        assertEquals(100, result.get());
        assertTrue(result.isDone());
    }

    @Test
    public void testAnyOfVarargsMultipleCompleted() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("First");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("Second");

        ContinuableFuture<String> result = Futures.anyOf(cf1, cf2);

        String value = result.get();
        assertTrue("First".equals(value) || "Second".equals(value));
    }

    @Test
    public void testAnyOfVarargsAllFailed() {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();
        cf1.completeExceptionally(new RuntimeException("Error 1"));
        cf2.completeExceptionally(new RuntimeException("Error 2"));

        ContinuableFuture<Integer> result = Futures.anyOf(cf1, cf2);

        assertThrows(Exception.class, () -> result.get());
    }

    @Test
    public void testAnyOfCollection() throws Exception {
        Collection<Future<String>> cfs = Arrays.asList(CompletableFuture.completedFuture("Result"), new CompletableFuture<>(), new CompletableFuture<>());

        ContinuableFuture<String> result = Futures.anyOf(cfs);

        assertEquals("Result", result.get());
    }

    @Test
    public void testAllOfCapturesInputMembership() throws Exception {
        final List<Future<Integer>> inputs = new ArrayList<>();
        inputs.add(CompletableFuture.completedFuture(1));

        final ContinuableFuture<List<Integer>> result = Futures.allOf(inputs);
        inputs.add(new CompletableFuture<>());

        assertTrue(result.isDone());
        assertEquals(Arrays.asList(1), result.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAnyOfCapturesInputMembership() throws Exception {
        final List<Future<String>> inputs = new ArrayList<>();
        inputs.add(CompletableFuture.completedFuture("first"));

        final ContinuableFuture<String> result = Futures.anyOf(inputs);
        inputs.clear();

        assertTrue(result.isDone());
        assertEquals("first", result.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAnyOfCollectionWithTimeout() throws Exception {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(42);

        Collection<Future<Integer>> cfs = Arrays.asList(cf1, cf2);
        ContinuableFuture<Integer> result = Futures.anyOf(cfs);

        assertEquals(42, result.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAnyOfNonPositiveTimeoutPollsImmediately() throws Exception {
        ContinuableFuture<String> completed = Futures.anyOf(CompletableFuture.completedFuture("ok"));
        assertEquals("ok", completed.get(0, TimeUnit.NANOSECONDS));
        assertEquals("ok", completed.get(-1, TimeUnit.NANOSECONDS));
        assertThrows(NullPointerException.class, () -> completed.get(0, null));

        java.util.concurrent.FutureTask<String> completedTask = new java.util.concurrent.FutureTask<>(() -> "plain");
        completedTask.run();
        assertEquals("plain", Futures.anyOf(completedTask).get(0, TimeUnit.NANOSECONDS));

        ContinuableFuture<String> pending = Futures.anyOf(new CompletableFuture<>());
        assertThrows(TimeoutException.class, () -> pending.get(0, TimeUnit.NANOSECONDS));
        assertThrows(TimeoutException.class, () -> pending.get(-1, TimeUnit.NANOSECONDS));

        ContinuableFuture<String> allFailed = Futures.anyOf(CompletableFuture.<String> failedFuture(new IllegalStateException("first")),
                CompletableFuture.<String> failedFuture(new IllegalArgumentException("second")));
        // Future.get contract: a computation failure surfaces as ExecutionException, with the first
        // failure as its cause and the remaining ones attached to that cause as suppressed.
        ExecutionException ee = assertThrows(ExecutionException.class, () -> allFailed.get(0, TimeUnit.NANOSECONDS));
        assertInstanceOf(IllegalStateException.class, ee.getCause());
        assertEquals("first", ee.getCause().getMessage());
        assertEquals(1, ee.getCause().getSuppressed().length);
        assertInstanceOf(IllegalArgumentException.class, ee.getCause().getSuppressed()[0]);
    }

    @Test
    public void testAnyOf() throws Exception {
        CompletableFuture<String> f1 = new CompletableFuture<>();
        CompletableFuture<String> f2 = new CompletableFuture<>();
        CompletableFuture<String> f3 = new CompletableFuture<>();

        ContinuableFuture<String> any = Futures.anyOf(f1, f2, f3);

        f2.complete("Second");
        Assertions.assertEquals("Second", any.get());

        f1.complete("First");
        f3.complete("Third");

    }

    @Test
    public void testAnyOfAllFailed() {
        Future<String> f1 = CompletableFuture.failedFuture(new RuntimeException("Error1"));
        Future<String> f2 = CompletableFuture.failedFuture(new RuntimeException("Error2"));
        Future<String> f3 = CompletableFuture.failedFuture(new RuntimeException("Error3"));

        ContinuableFuture<String> any = Futures.anyOf(f1, f2, f3);

        // Future.get contract: a computation failure surfaces as ExecutionException, never as a bare
        // unchecked exception (a standard `catch (ExecutionException)` used to miss it entirely).
        ExecutionException ee = Assertions.assertThrows(ExecutionException.class, () -> any.get());
        Assertions.assertEquals("Error1", ee.getCause().getMessage());
        Assertions.assertEquals(2, ee.getCause().getSuppressed().length);
    }

    @Test
    @Timeout(5)
    public void testAnyOfWithTimeout() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Never";
        });
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("Immediate");

        ContinuableFuture<String> result = Futures.anyOf(cf1, cf2);

        assertEquals("Immediate", result.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testIsDoneForAnyOf() throws Exception {
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new CompletableFuture<>();

        ContinuableFuture<String> anyOfFuture = Futures.anyOf(cf1, cf2);

        assertFalse(anyOfFuture.isDone());

        cf2.complete("Second");

        assertTrue(anyOfFuture.isDone());
        assertEquals("Second", anyOfFuture.get());
    }

    @Test
    public void testAnyOfIsDoneWaitsForSuccessAfterFailure() throws Exception {
        CompletableFuture<String> failed = new CompletableFuture<>();
        CompletableFuture<String> pending = new CompletableFuture<>();

        ContinuableFuture<String> anyOfFuture = Futures.anyOf(failed, pending);

        failed.completeExceptionally(new IllegalStateException("boom"));

        assertFalse(anyOfFuture.isDone());

        pending.complete("Second");

        assertTrue(anyOfFuture.isDone());
        assertEquals("Second", anyOfFuture.get());
    }

    @Test
    public void testAnyOfSingleFuture() throws Exception {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("only");

        ContinuableFuture<String> result = Futures.anyOf(cf);

        assertEquals("only", result.get());
    }

    @Test
    public void testIterateVarargs() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(2);
        CompletableFuture<Integer> cf3 = CompletableFuture.completedFuture(3);

        ObjIterator<Integer> iter = Futures.iterate(cf1, cf2, cf3);

        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(3, results.size());
        assertTrue(results.contains(1));
        assertTrue(results.contains(2));
        assertTrue(results.contains(3));
    }

    @Test
    public void testIterateVarargsNoSuchElement() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);

        ObjIterator<Integer> iter = Futures.iterate(cf1);

        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterateCollection() throws Exception {
        Collection<Future<String>> cfs = Arrays.asList(CompletableFuture.completedFuture("A"), CompletableFuture.completedFuture("B"),
                CompletableFuture.completedFuture("C"));

        ObjIterator<String> iter = Futures.iterate(cfs);

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(3, results.size());
        assertTrue(results.contains("A"));
        assertTrue(results.contains("B"));
        assertTrue(results.contains("C"));
    }

    @Test
    public void testIterateCollectionWithTimeout() throws Exception {
        Collection<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(10), CompletableFuture.completedFuture(20));

        ObjIterator<Integer> iter = Futures.iterate(cfs, 1, TimeUnit.SECONDS);

        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertTrue(results.contains(10));
        assertTrue(results.contains(20));
    }

    @Test
    public void testIterateCollectionWithTimeoutExceeded() throws Exception {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                Thread.sleep(200);
                cf1.complete(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Collection<Future<Integer>> cfs = Arrays.asList(cf1);
        ObjIterator<Integer> iter = Futures.iterate(cfs, 50, TimeUnit.MILLISECONDS);

        assertTrue(iter.hasNext());
        assertThrows(RuntimeException.class, () -> iter.next());
    }

    @Test
    public void testIterateCollectionWithResultHandler() throws Exception {
        Collection<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(5), CompletableFuture.completedFuture(10));

        Function<Result<Integer, Exception>, String> handler = result -> {
            if (result.isSuccess()) {
                return "Success: " + result.orElseIfFailure(0);
            } else {
                return "Failed";
            }
        };

        ObjIterator<String> iter = Futures.iterate(cfs, handler);

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertTrue(results.contains("Success: 5"));
        assertTrue(results.contains("Success: 10"));
    }

    @Test
    public void testIterateCollectionWithResultHandlerForFailure() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();
        cf2.completeExceptionally(new RuntimeException("Error"));

        Collection<Future<Integer>> cfs = Arrays.asList(cf1, cf2);

        Function<Result<Integer, Exception>, String> handler = result -> {
            if (result.isSuccess()) {
                return "Success: " + result.orElseIfFailure(0);
            } else {
                return "Failed: " + result.getException().getMessage();
            }
        };

        ObjIterator<String> iter = Futures.iterate(cfs, handler);

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(s -> s.startsWith("Success:")));
        assertTrue(results.stream().anyMatch(s -> s.startsWith("Failed:")));
    }

    @Test
    public void testIterateCollectionWithTimeoutAndResultHandler() throws Exception {
        Collection<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(100), CompletableFuture.completedFuture(200));

        Function<Result<Integer, Exception>, Integer> handler = result -> {
            if (result.isSuccess()) {
                return result.orElseIfFailure(0) * 2;
            } else {
                return -1;
            }
        };

        ObjIterator<Integer> iter = Futures.iterate(cfs, 1, TimeUnit.SECONDS, handler);

        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertTrue(results.contains(200));
        assertTrue(results.contains(400));
    }

    @Test
    public void testIterateWithAsyncFutures() throws Exception {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ObjIterator<Integer> iter = Futures.iterate(cf1, cf2);

        executor.submit(() -> {
            try {
                Thread.sleep(5);
                cf2.complete(2);
                Thread.sleep(5);
                cf1.complete(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertEquals(2, results.get(0));
        assertEquals(1, results.get(1));
    }

    @Test
    public void testIterateWithCompletableFuturesCompletedOutOfInputOrder() {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ObjIterator<Integer> iter = Futures.iterate(cf1, cf2);

        cf2.complete(2);
        cf1.complete(1);

        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(Arrays.asList(2, 1), results);
    }

    @Test
    public void testIterateWithDelay() throws Exception {
        CompletableFuture<String> f1 = new CompletableFuture<>();
        CompletableFuture<String> f2 = new CompletableFuture<>();
        CompletableFuture<String> f3 = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                Thread.sleep(300);
                f3.complete("Third");
                Thread.sleep(200);
                f1.complete("First");
                Thread.sleep(100);
                f2.complete("Second");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<Future<String>> futures = Arrays.asList(f1, f2, f3);
        ObjIterator<String> iter = Futures.iterate(futures);

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        Assertions.assertEquals("Third", results.get(0));
        Assertions.assertEquals("First", results.get(1));
        Assertions.assertEquals("Second", results.get(2));
    }

    @Test
    public void testIterateWithTimeout() throws Exception {
        CompletableFuture<String> f1 = CompletableFuture.completedFuture("Quick");
        CompletableFuture<String> f2 = new CompletableFuture<>();

        List<Future<String>> futures = Arrays.asList(f1, f2);
        ObjIterator<String> iter = Futures.iterate(futures, 500, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("Quick", iter.next());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertThrows(RuntimeException.class, () -> iter.next());
    }

    @Test
    public void testIterateWithResultHandler() throws Exception {
        Future<Integer> f1 = CompletableFuture.completedFuture(42);
        Future<Integer> f2 = CompletableFuture.failedFuture(new RuntimeException("Error"));

        Collection<Future<Integer>> futures = Arrays.asList(f1, f2);
        ObjIterator<String> iter = Futures.iterate(futures, result -> {
            if (result.isSuccess()) {
                return "Success: " + result.orElseThrow((Supplier<? extends RuntimeException>) () -> new RuntimeException("No value"));
            } else {
                return "Failed: " + result.getException().getMessage();
            }
        });

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        Assertions.assertEquals(2, results.size());
        Assertions.assertTrue(results.contains("Success: 42"));
        Assertions.assertTrue(results.contains("Failed: Error"));
    }

    @Test
    public void testIterateWithTimeoutAndResultHandler() throws Exception {
        Collection<CompletableFuture<String>> futures = Arrays.asList(CompletableFuture.completedFuture("Quick"), CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Delayed";
        }));

        Function<Result<String, Exception>, String> resultHandler = result -> {
            if (result.isSuccess()) {
                return result.orElseIfFailure(null).toUpperCase();
            } else {
                return "ERROR";
            }
        };

        ObjIterator<String> iter = Futures.iterate(futures, 2, TimeUnit.SECONDS, resultHandler);

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertTrue(results.contains("QUICK"));
        assertTrue(results.contains("DELAYED"));
    }

    @Test
    public void testIterateCollectionWithResultHandlerAllFailures() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.failedFuture(new RuntimeException("Error1"));
        CompletableFuture<Integer> cf2 = CompletableFuture.failedFuture(new RuntimeException("Error2"));

        Collection<Future<Integer>> cfs = Arrays.asList(cf1, cf2);

        Function<Result<Integer, Exception>, String> handler = result -> {
            if (result.isSuccess()) {
                return "OK";
            } else {
                return "FAIL: " + result.getException().getMessage();
            }
        };

        ObjIterator<String> iter = Futures.iterate(cfs, handler);

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(s -> s.startsWith("FAIL:")));
    }

    @Test
    @Timeout(5)
    public void testIterateCollectionWithTimeoutExceededAndResultHandler() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        Collection<Future<Integer>> cfs = Arrays.asList(cf1, cf2);

        Function<Result<Integer, Exception>, String> handler = result -> {
            if (result.isSuccess()) {
                return "OK: " + result.orElseIfFailure(0);
            } else {
                return "TIMEOUT";
            }
        };

        ObjIterator<String> iter = Futures.iterate(cfs, 200, TimeUnit.MILLISECONDS, handler);

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertTrue(results.contains("OK: 1"));
        assertTrue(results.contains("TIMEOUT"));
    }

    @Test
    @Timeout(10)
    public void testIterateFirstOutOrder() throws Exception {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int value = i;
            final int delay = (5 - i) * 200;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return value;
            }, executor));
        }

        ObjIterator<Integer> iter = Futures.iterate(futures);
        List<Integer> results = new ArrayList<>();

        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(5, results.size());
        assertEquals(4, results.get(0));
        assertEquals(3, results.get(1));
        assertEquals(2, results.get(2));
        assertEquals(1, results.get(3));
        assertEquals(0, results.get(4));
    }

    @Test
    public void testConvertException() {
        Exception e1 = new RuntimeException("test");
        Assertions.assertEquals(e1, Futures.convertException(e1));

        Exception cause = new IllegalArgumentException("cause");
        ExecutionException e2 = new ExecutionException(cause);
        Assertions.assertEquals(cause, Futures.convertException(e2));

        ExecutionException e3 = new ExecutionException(new Error("error"));
        Assertions.assertEquals(e3, Futures.convertException(e3));
    }

    @Test
    public void testCancelDoesNotSuppressExceptionOnItselfForDuplicateFuture() {
        final IllegalStateException sharedFailure = new IllegalStateException("shared cancel failure");
        final Future<Integer> future = new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                throw sharedFailure;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Integer get() {
                return 1;
            }

            @Override
            public Integer get(final long timeout, final TimeUnit unit) {
                return 1;
            }
        };

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> Futures.allOf(future, future).cancel(true));
        Assertions.assertSame(sharedFailure, thrown);
        assertEquals(0, thrown.getSuppressed().length);
    }

    @Test
    public void testIterateReportsErrorThrownDirectlyByCustomFuture() {
        Future<String> brokenFuture = new Future<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public String get() {
                throw new AssertionError("boom");
            }

            @Override
            public String get(long timeout, TimeUnit unit) {
                throw new AssertionError("boom");
            }
        };

        ObjIterator<Result<String, Exception>> iter = Futures.iterate(Collections.singletonList(brokenFuture), 1, TimeUnit.SECONDS, result -> result);
        Result<String, Exception> result = iter.next();

        assertTrue(result.getException() instanceof ExecutionException);
        assertTrue(result.getException().getCause() instanceof AssertionError);
        assertFalse(iter.hasNext());
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testAnyOfTimedGetThrowsTimeoutException() {
        // regression: a total timeout was wrapped in UncheckedException instead of the
        // TimeoutException documented by Future.get(timeout, unit) (and thrown by allOf)
        final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

        try {
            final java.util.concurrent.Future<String> never = executor.submit(() -> {
                Thread.sleep(60_000);
                return "never";
            });

            Assertions.assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> Futures.anyOf(java.util.Arrays.asList(never)).get(200, java.util.concurrent.TimeUnit.MILLISECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    public void testAnyOfUntimedGetThrowsInterruptedExceptionWhenInterrupted() throws Exception {
        // regression: when the thread blocked in anyOf(...).get() was interrupted, the synthetic
        // InterruptedException was wrapped in an unchecked exception instead of being rethrown
        // per the Future.get() contract (the timed overload already rethrew it).
        final CompletableFuture<String> never = new CompletableFuture<>();
        final java.util.concurrent.atomic.AtomicReference<Throwable> thrown = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);

        final Thread worker = new Thread(() -> {
            try {
                Futures.anyOf(Arrays.asList(never)).get();
            } catch (Throwable e) {
                thrown.set(e);
            } finally {
                done.countDown();
            }
        });

        worker.start();
        // A pending interrupt makes the iterator's blocking take() throw immediately, so this is
        // deterministic regardless of whether the worker has reached the blocking call yet.
        worker.interrupt();

        assertTrue(done.await(5, TimeUnit.SECONDS), "worker thread did not finish");
        assertTrue(thrown.get() instanceof InterruptedException, "expected InterruptedException but got: " + thrown.get());

        never.complete("unblock");
    }

    @Test
    public void testAllOf_isCancelledImpliesIsDone() throws Exception {
        // Future contract: isCancelled() must imply isDone(). Cancelling only one child while another
        // is still running must not report isCancelled()==true with isDone()==false.
        final CompletableFuture<String> first = new CompletableFuture<>();
        final CompletableFuture<String> second = new CompletableFuture<>();
        final ContinuableFuture<List<String>> all = Futures.allOf(first, second);

        first.cancel(true);
        assertFalse(all.isCancelled(), "composite must not report cancelled while a sibling is still running");
        assertFalse(all.isDone());

        second.complete("ok");
        assertTrue(all.isDone());
        assertTrue(all.isCancelled(), "once all constituents are done and one was cancelled, composite is cancelled");
    }

    @Test
    public void testCompose_partialCancellationCanStillSucceed() throws Exception {
        final CompletableFuture<String> cancelled = new CompletableFuture<>();
        final CompletableFuture<String> completed = CompletableFuture.completedFuture("ok");
        cancelled.cancel(true);

        final ContinuableFuture<String> composed = Futures.compose(Arrays.asList(cancelled, completed), futures -> futures.get(1).get());

        assertTrue(composed.isDone());
        assertFalse(composed.isCancelled(), "a compose function may ignore a cancelled constituent");
        assertEquals("ok", composed.get());
    }

    @Test
    public void testCompose_externalCancellationOfAllInputsCanStillSucceed() throws Exception {
        final CompletableFuture<String> first = new CompletableFuture<>();
        final CompletableFuture<String> second = new CompletableFuture<>();
        first.cancel(true);
        second.cancel(true);

        final ContinuableFuture<String> composed = Futures.compose(Arrays.asList(first, second), futures -> "fallback");

        assertTrue(composed.isDone());
        assertFalse(composed.isCancelled(), "input cancellation does not cancel a compose function that can provide a fallback");
        assertEquals("fallback", composed.get());
    }

    @Test
    public void testCompose_successfulCancelCancelsComposite() throws Exception {
        final CompletableFuture<String> first = new CompletableFuture<>();
        final CompletableFuture<String> second = new CompletableFuture<>();
        final ContinuableFuture<String> composed = Futures.compose(Arrays.asList(first, second), futures -> "ignored");

        assertTrue(composed.cancel(true));
        assertTrue(composed.isCancelled());
        assertTrue(composed.isDone());
        assertThrows(CancellationException.class, composed::get);
        assertThrows(CancellationException.class, () -> composed.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void cancelPendingRelays_interruptsAbandonedPlainFutureRelay() throws Exception {
        final java.util.concurrent.CountDownLatch enteredGet = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicBoolean getWasInterrupted = new java.util.concurrent.atomic.AtomicBoolean();
        final Future<Integer> blocking = new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Integer get() throws InterruptedException {
                enteredGet.countDown();
                try {
                    Thread.sleep(60_000L);
                    return 1;
                } catch (final InterruptedException e) {
                    getWasInterrupted.set(true);
                    throw e;
                }
            }

            @Override
            public Integer get(final long timeout, final TimeUnit unit) throws InterruptedException {
                return get();
            }
        };

        final ObjIterator<Integer> iter = Futures.iterate(List.of(CompletableFuture.completedFuture(2), blocking));
        assertTrue(iter.hasNext());
        assertEquals(2, iter.next());
        assertTrue(enteredGet.await(2, TimeUnit.SECONDS));
        Futures.cancelPendingRelays(iter);

        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!getWasInterrupted.get() && System.nanoTime() < deadline) {
            Thread.sleep(10L);
        }
        assertTrue(getWasInterrupted.get());
    }

    // --- regression tests for the 2026-09-11 Futures review (G46) ---

    @Test
    public void testAnyOfIsDoneDoesNotBlockOnALazilyMappedInput() {
        // G46-001: Future.isDone() is a state query - it must neither block nor run the caller's code.
        final java.util.concurrent.atomic.AtomicInteger mapperCalls = new java.util.concurrent.atomic.AtomicInteger();
        final ContinuableFuture<String> lazy = ContinuableFuture.completed("base").map(v -> {
            mapperCalls.incrementAndGet();
            Thread.sleep(1_500);
            return v;
        });
        final ContinuableFuture<String> any = Futures.anyOf(Arrays.asList(lazy));

        Assertions.assertTimeoutPreemptively(java.time.Duration.ofMillis(500), () -> assertTrue(any.isDone()));
        assertEquals(0, mapperCalls.get(), "isDone() must not run the caller's mapper");
    }

    @Test
    public void testAnyOfIsDoneDoesNotBlockOnADoneInputWhoseGetBlocks() {
        // G46-001: the non-positive-timeout branch documents "poll without blocking", so an already-done input
        // has to be polled with get(0, NANOSECONDS), never with the unbounded no-arg get().
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        final Future<String> doneButSlow = new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public String get() throws InterruptedException {
                release.await(5, TimeUnit.SECONDS);
                return "slow";
            }

            @Override
            public String get(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
                if (!release.await(timeout, unit)) {
                    throw new TimeoutException();
                }

                return "slow";
            }
        };
        final Future<String> pending = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(Arrays.asList(pending, doneButSlow));

        try {
            Assertions.assertTimeoutPreemptively(java.time.Duration.ofMillis(500), () -> assertFalse(any.isDone()));
        } finally {
            release.countDown();
        }
    }

    @Test
    public void testIterateSnapshotsACollectionWhoseSizeDisagreesWithItsIterator() {
        // G46-002: reading cfs.size() and then iterating cfs again left the iterator waiting for a relay that
        // was never registered, and the untimed overload then blocked in take() forever.
        final List<Future<? extends String>> actual = Collections.singletonList(CompletableFuture.completedFuture("a"));
        final Collection<Future<? extends String>> skewed = new java.util.AbstractCollection<Future<? extends String>>() {
            @Override
            public java.util.Iterator<Future<? extends String>> iterator() {
                return actual.iterator();
            }

            @Override
            public int size() {
                // What a CopyOnWriteArrayList/ConcurrentLinkedQueue reports when it loses an element between
                // the size() call and the iteration.
                return actual.size() + 1;
            }
        };

        Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), () -> {
            final ObjIterator<String> iter = Futures.iterate(skewed);

            assertTrue(iter.hasNext());
            assertEquals("a", iter.next());
            assertFalse(iter.hasNext());
        });
    }

    @Test
    public void testIterateDeliversAlreadyQueuedResultsAfterTheDeadline() throws Exception {
        // G46-004: a result that arrived inside the budget is within the caller's budget by definition, so the
        // post-deadline branch has to poll the queue instead of fabricating a TimeoutException.
        final ObjIterator<String> iter = Futures.iterate(Collections.singletonList(CompletableFuture.completedFuture("READY")), 50, TimeUnit.MILLISECONDS);

        Thread.sleep(150);

        assertTrue(iter.hasNext());
        assertEquals("READY", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testAnyOfSuppressionIsIdempotentAcrossAggregatesAndDuplicates() {
        // G46-011: the primary is the input future's own exception object, so re-aggregating the same inputs
        // must not append the same siblings again.
        final IllegalStateException e1 = new IllegalStateException("first");
        final IllegalArgumentException e2 = new IllegalArgumentException("second");
        final RuntimeException e3 = new RuntimeException("third");
        final CompletableFuture<String> f1 = CompletableFuture.failedFuture(e1);
        final CompletableFuture<String> f2 = CompletableFuture.failedFuture(e2);
        final CompletableFuture<String> f3 = CompletableFuture.failedFuture(e3);

        for (int i = 0; i < 3; i++) {
            assertThrows(ExecutionException.class, () -> Futures.anyOf(f1, f2, f3).get());
        }

        assertEquals(2, e1.getSuppressed().length, "one entry per distinct sibling, however many aggregates ran");
        Assertions.assertSame(e2, e1.getSuppressed()[0]);
        Assertions.assertSame(e3, e1.getSuppressed()[1]);

        // The same failure instance also reaches ONE aggregate twice through two futures derived from one
        // failed source.
        final RuntimeException boom = new RuntimeException("boom");
        final CompletableFuture<String> src = CompletableFuture.failedFuture(boom);
        final IllegalStateException lead = new IllegalStateException("lead");
        final ExecutionException ee = assertThrows(ExecutionException.class,
                () -> Futures.anyOf(CompletableFuture.<String> failedFuture(lead), src.thenApply(v -> v), src.thenApply(v -> v)).get(0, TimeUnit.NANOSECONDS));

        Assertions.assertSame(lead, ee.getCause());
        assertEquals(1, lead.getSuppressed().length);
        Assertions.assertSame(boom, lead.getSuppressed()[0]);
    }

    @Test
    public void testAnyOfPublishesAnErrorFailureAsTheDirectCause() {
        // G46-003: an Error cannot travel as a Result<T, Exception> failure, so it stays inside the transport
        // ExecutionException; publishing must unwrap it rather than wrap it a second time.
        final StackOverflowError boom = new StackOverflowError("boom");
        final CompletableFuture<String> failed = CompletableFuture.failedFuture(boom);

        final ExecutionException fromAnyOf = assertThrows(ExecutionException.class, () -> Futures.anyOf(Arrays.asList(failed)).get());
        Assertions.assertSame(boom, fromAnyOf.getCause());

        // ... which is what allOf and a plain Future.get() already report for the same input.
        final ExecutionException fromAllOf = assertThrows(ExecutionException.class, () -> Futures.allOf(Arrays.asList(failed)).get());
        Assertions.assertSame(boom, fromAllOf.getCause());
    }

    @Test
    public void testAnyOfTimedGetRetriesShareOneCompletionRegistration() throws Exception {
        // G46-010: a fresh iterate(...) per call registered another whenComplete dependent on the input, and a
        // CompletableFuture offers no way to unregister one.
        final CompletableFuture<String> never = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(Arrays.asList(never));

        for (int i = 0; i < 50; i++) {
            assertThrows(TimeoutException.class, () -> any.get(1, TimeUnit.MILLISECONDS));
        }

        assertTrue(never.getNumberOfDependents() <= 1, "dependents after 50 abandoned timed get(): " + never.getNumberOfDependents());

        never.complete("done");
        assertEquals("done", any.get());
    }

    @Test
    public void testAllOf_cancelledInputThrowsCancellationExceptionWhileCompositeIsNotCancelled() {
        // G46-005 (documented contract): a cancelled input is reported as cancellation, not as a failure, and
        // it surfaces while the composite itself still reports neither done nor cancelled.
        final CompletableFuture<String> cancelled = new CompletableFuture<>();
        final CompletableFuture<String> pending = new CompletableFuture<>();
        cancelled.cancel(true);

        final ContinuableFuture<List<String>> all = Futures.allOf(cancelled, pending);

        assertFalse(all.isDone());
        assertFalse(all.isCancelled());
        assertThrows(CancellationException.class, all::get);
        assertThrows(CancellationException.class, () -> all.get(50, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(10)
    public void testIterateConsumerInterruptionYieldsOneFinalOutcomeAndRestoresTheInterruptFlag() throws Exception {
        // G46-006 (documented contract): the consuming thread's interruption becomes one fabricated final
        // outcome that belongs to no input future, and the interrupt status is restored for the caller.
        final CompletableFuture<String> never = new CompletableFuture<>();
        final java.util.concurrent.atomic.AtomicReference<Throwable> thrown = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicBoolean flagAfter = new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicBoolean moreAfter = new java.util.concurrent.atomic.AtomicBoolean(true);
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);

        final Thread worker = new Thread(() -> {
            final ObjIterator<String> iter = Futures.iterate(Collections.singletonList(never));

            try {
                iter.next();
            } catch (final Throwable e) {
                thrown.set(e);
            } finally {
                flagAfter.set(Thread.currentThread().isInterrupted());
                moreAfter.set(iter.hasNext());
                done.countDown();
            }
        });

        worker.start();
        worker.interrupt();

        assertTrue(done.await(5, TimeUnit.SECONDS), "worker thread did not finish");
        assertInstanceOf(com.landawn.abacus.exception.UncheckedInterruptedException.class, thrown.get());
        assertInstanceOf(InterruptedException.class, thrown.get().getCause());
        assertTrue(flagAfter.get(), "the iterator must restore the consumer's interrupt status");
        assertFalse(moreAfter.get(), "no further outcomes are produced after the interruption");

        never.complete("unblock");
    }

    @Test
    public void testCompositeCancelPropagatesToEveryInputEvenWhenItReturnsFalse() throws Exception {
        // G46-009 / G46-012 (documented contract): cancel(...) is attempted on EVERY input and returns true
        // only if all of them accepted it; the inputs are cancelled even when it returns false.
        final CompletableFuture<String> completed = CompletableFuture.completedFuture("done");
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<List<String>> all = Futures.allOf(completed, pending);

        assertFalse(all.cancel(true), "an input that already completed refuses cancellation");
        assertFalse(completed.isCancelled());
        assertTrue(pending.isCancelled(), "the remaining inputs are cancelled anyway");
        assertTrue(all.isCancelled());
        assertThrows(CancellationException.class, all::get);

        // ... and each composite keeps its own isCancelled() rule: anyOf reports cancelled only when EVERY
        // candidate was cancelled.
        final CompletableFuture<String> completed2 = CompletableFuture.completedFuture("done");
        final CompletableFuture<String> pending2 = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(completed2, pending2);

        assertFalse(any.cancel(true));
        assertTrue(pending2.isCancelled());
        assertFalse(any.isCancelled());
        assertEquals("done", any.get());
    }

    @Test
    public void testComposeCollectionRejectsNullOrEmptyCollection() {
        // G46-008 (documented contract): the two-argument overload validates the collection too.
        assertThrows(IllegalArgumentException.class, () -> Futures.compose(Collections.<Future<String>> emptyList(), l -> "x"));
        assertThrows(IllegalArgumentException.class, () -> Futures.compose((Collection<Future<String>>) null, l -> "x"));
    }

    @Test
    public void testIterateRethrowsAnUncheckedInputFailureUnwrapped() {
        // G46-013 (documented contract): an already-unchecked failure is rethrown as itself, so getCause() is
        // null for it; only a checked failure is wrapped.
        final IllegalStateException boom = new IllegalStateException("boom");
        final ObjIterator<String> iter = Futures.iterate(Collections.singletonList(CompletableFuture.<String> failedFuture(boom)));

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, iter::next);
        Assertions.assertSame(boom, thrown);
        Assertions.assertNull(thrown.getCause());

        final java.io.IOException io = new java.io.IOException("io");
        final ObjIterator<String> checkedIter = Futures.iterate(Collections.singletonList(CompletableFuture.<String> failedFuture(io)));
        final RuntimeException wrapped = assertThrows(RuntimeException.class, checkedIter::next);

        Assertions.assertSame(io, wrapped.getCause());
    }

    /** Relay threads (see {@code Futures.RELAY_EXECUTOR}) currently blocked inside a {@code Futures} frame. */
    private static int relayThreadsInsideFutures() {
        int count = 0;

        for (final java.util.Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            if (!entry.getKey().getName().startsWith("abacus-futures-relay-")) {
                continue;
            }

            for (final StackTraceElement frame : entry.getValue()) {
                if (frame.getClassName().startsWith(Futures.class.getName())) {
                    count++;
                    break;
                }
            }
        }

        return count;
    }

    @Test
    @Timeout(30)
    public void testAnyOfAbandonedTimedGetReleasesItsRelayThreads() throws Exception {
        // G46-F01: a timed get() whose deadline expires publishes nothing, so the relays have to be released
        // when the last waiter leaves and not only when a terminal outcome is published - otherwise one
        // RELAY_EXECUTOR thread stays blocked inside the input's own get() for as long as that input runs.
        // A plain Future is required here: a CompletableFuture input uses whenComplete, not a relay task.
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        final FutureTask<String> input = new FutureTask<>(() -> {
            release.await(20, TimeUnit.SECONDS);
            return "late";
        });
        final Thread inputThread = new Thread(input, "futures-test-input");
        inputThread.setDaemon(true);
        inputThread.start();

        try {
            final int baseline = relayThreadsInsideFutures();
            final ContinuableFuture<String> any = Futures.anyOf(Arrays.asList(input));

            assertThrows(TimeoutException.class, () -> any.get(20, TimeUnit.MILLISECONDS));

            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (relayThreadsInsideFutures() > baseline && System.nanoTime() < deadline) {
                Thread.sleep(10L);
            }
            assertEquals(baseline, relayThreadsInsideFutures(), "an abandoned timed get() must not leave a relay blocked in its input");

            // The released relay wakes with its own InterruptedException, which must NOT be recorded as the
            // input's outcome: a later get() still sees the real one.
            release.countDown();
            assertEquals("late", any.get());
        } finally {
            release.countDown();
        }
    }

    @Test
    @Timeout(30)
    public void testAnyOfAbandonedTimedGetKeepsTheInputsRealOutcome() throws Exception {
        // G46-F01: releasing the relays must not publish anything. Repeated expiring get()s over a
        // still-pending input keep reporting TimeoutException - never a terminal failure built from the
        // cancelled relay's InterruptedException - and the input's real value still wins afterwards.
        final java.util.concurrent.CountDownLatch hold = new java.util.concurrent.CountDownLatch(1);
        final FutureTask<String> pending = new FutureTask<>(() -> {
            hold.await(20, TimeUnit.SECONDS);
            return "v";
        });
        final Thread inputThread = new Thread(pending, "futures-test-input-2");
        inputThread.setDaemon(true);
        inputThread.start();

        try {
            final ContinuableFuture<String> any = Futures.anyOf(Arrays.asList(pending));

            for (int i = 0; i < 5; i++) {
                assertThrows(TimeoutException.class, () -> any.get(20, TimeUnit.MILLISECONDS));
            }

            hold.countDown();
            assertEquals("v", any.get());
        } finally {
            hold.countDown();
        }
    }

    @Test
    @Timeout(30)
    public void testAnyOfIsDoneAndALazilyMappedSiblingsMapper() throws Exception {
        // G46-F01 (documented contract, green on base by design): isDone() polls with get(0, NANOSECONDS) and a
        // lazily mapped ContinuableFuture ignores that deadline. If its mapper SUCCEEDS the value becomes this
        // aggregate's published result and it is not run again; if it THROWS nothing is published, so every
        // isDone() call re-runs it. Both need a still-pending sibling, or the all-settled short-circuit answers
        // isDone() without reading any input at all.
        final java.util.concurrent.atomic.AtomicInteger okCalls = new java.util.concurrent.atomic.AtomicInteger();
        final ContinuableFuture<String> lazyOk = ContinuableFuture.completed("base").map(v -> {
            okCalls.incrementAndGet();
            return v + "!";
        });
        final CompletableFuture<String> sibling = new CompletableFuture<>();
        final ContinuableFuture<String> withSuccess = Futures.anyOf(Arrays.asList(sibling, lazyOk));

        for (int i = 0; i < 5; i++) {
            assertTrue(withSuccess.isDone());
        }

        assertEquals(1, okCalls.get(), "a mapper whose value was published must not run again");
        assertEquals("base!", withSuccess.get());

        final java.util.concurrent.atomic.AtomicInteger failCalls = new java.util.concurrent.atomic.AtomicInteger();
        final ContinuableFuture<String> lazyFail = ContinuableFuture.completed("base").map(v -> {
            failCalls.incrementAndGet();
            throw new IllegalStateException("mapper boom");
        });
        final CompletableFuture<String> sibling2 = new CompletableFuture<>();
        final ContinuableFuture<String> withFailure = Futures.anyOf(Arrays.asList(sibling2, lazyFail));

        for (int i = 0; i < 5; i++) {
            assertFalse(withFailure.isDone());
        }

        assertEquals(5, failCalls.get(), "a failing mapper publishes nothing, so every isDone() re-runs it");

        sibling.complete("x");
        sibling2.complete("x");
    }

    @Test
    @Timeout(30)
    public void testIterateDoesNotDeliverAResultThatArrivedAfterTheDeadline() throws Exception {
        // G46-F01: the post-deadline poll exists to hand over a result that arrived INSIDE the budget and was
        // not consumed yet. An outcome produced after the deadline is outside the caller's budget, so queuing
        // it would let a consumer slower than its inputs escape the total timeout entirely.
        final CompletableFuture<String> late1 = new CompletableFuture<>();
        final CompletableFuture<String> late2 = new CompletableFuture<>();
        final ObjIterator<String> iter = Futures.iterate(Arrays.asList(late1, late2), 50, TimeUnit.MILLISECONDS);

        // Both inputs complete well past the 50 ms budget, and the consumer only looks afterwards.
        Thread.sleep(300);
        late1.complete("L1");
        late2.complete("L2");
        Thread.sleep(100);

        assertTrue(iter.hasNext());

        final RuntimeException thrown = assertThrows(RuntimeException.class, iter::next);

        assertInstanceOf(TimeoutException.class, thrown.getCause(), "the expired budget must be reported, not a post-deadline result");
        assertFalse(iter.hasNext());
        // The in-budget half of the boundary is pinned by testIterateDeliversAlreadyQueuedResultsAfterTheDeadline,
        // which is RED on base; this one is GREEN on base and RED on the unfixed change, so keep them apart.
    }

    @Test
    @Timeout(120)
    public void testAnyOfConcurrentGettersAgreeOnOnePublishedOutcome() throws Exception {
        // G46-F01: the anyOf fan-out (subscribe/offerOutcome/unsubscribe/terminalSignal) is shared by every
        // concurrent get(...) on one aggregate, and the pass-3 rewrite was only ever exercised single-threaded.
        // Every getter must observe the SAME single published outcome, and none may hang. Fixed seed, so a
        // failure is reproducible.
        final java.util.Random rnd = new java.util.Random(20260911L);
        final ExecutorService pool = Executors.newFixedThreadPool(8);

        try {
            for (int round = 0; round < 150; round++) {
                final int inputCount = 1 + rnd.nextInt(3);
                // Half the rounds let every input settle (so the all-failure path runs too); the other half mix
                // in never-completing inputs, which need one guaranteed success or an untimed get() may never
                // return - anyOf waits for a success while any candidate is still pending.
                final boolean allSettle = rnd.nextBoolean();
                final int winner = allSettle ? -1 : rnd.nextInt(inputCount);
                final List<Future<? extends String>> inputs = new ArrayList<>(inputCount);

                for (int i = 0; i < inputCount; i++) {
                    final int value = i;
                    final int kind = i == winner ? rnd.nextInt(2) : rnd.nextInt(allSettle ? 5 : 6);

                    switch (kind) {
                        case 0 -> inputs.add(CompletableFuture.completedFuture("v" + value));
                        case 1 -> inputs.add(pool.submit(() -> "v" + value));
                        case 2 -> inputs.add(CompletableFuture.failedFuture(new IllegalStateException("f" + value)));
                        case 3 -> inputs.add(pool.submit(() -> {
                            throw new IllegalStateException("f" + value);
                        }));
                        case 4 -> {
                            final CompletableFuture<String> cancelled = new CompletableFuture<>();
                            cancelled.cancel(true);
                            inputs.add(cancelled);
                        }
                        default -> inputs.add(new CompletableFuture<String>());
                    }
                }

                final ContinuableFuture<String> any = Futures.anyOf(inputs);
                final int getterCount = 2 + rnd.nextInt(2);
                final List<java.util.concurrent.Callable<String>> getters = new ArrayList<>(getterCount);

                for (int g = 0; g < getterCount; g++) {
                    getters.add(() -> describeOutcome(any));
                }

                final List<java.util.concurrent.Future<String>> observed = pool.invokeAll(getters, 30, TimeUnit.SECONDS);
                final String first = observed.get(0).get();

                for (final java.util.concurrent.Future<String> o : observed) {
                    assertEquals(first, o.get(), "round " + round + ": concurrent getters disagreed");
                }

                assertEquals(first, describeOutcome(any), "round " + round + ": a later get() disagreed with the published outcome");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    public void testAnyOfSlowMapperDoesNotHoldOtherGettersOrSiblingSuccess() throws Exception {
        // Registration must not run a lazy mapper while holding a lock needed by every getter.
        final java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        final ContinuableFuture<String> slow = ContinuableFuture.completed("slow").map(value -> {
            entered.countDown();
            release.await();
            return value;
        });
        final CompletableFuture<String> sibling = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(slow, sibling);
        final FutureTask<String> getter = new FutureTask<>(any::get);
        final Thread thread = new Thread(getter, "futures-slow-mapper-getter");
        thread.setDaemon(true);
        thread.start();

        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(1),
                    () -> assertThrows(TimeoutException.class, () -> any.get(20, TimeUnit.MILLISECONDS)));
            sibling.complete("winner");
            assertEquals("winner", getter.get(2, TimeUnit.SECONDS));
            assertEquals("winner", any.get());
            assertFalse(slow.isCancelled(), "releasing a relay must not cancel the user's input");
        } finally {
            release.countDown();
            thread.interrupt();
            thread.join(2_000);
        }
    }

    @Test
    @Timeout(10)
    public void testAnyOfPollingPublicationWakesAnUntimedGetter() throws Exception {
        // A polling get can publish a later successful evaluation of a lazy input. Existing waiters
        // must be signalled even though the polling caller never subscribed to the relay queues.
        final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        final ContinuableFuture<String> lazy = ContinuableFuture.completed("winner").map(value -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("first evaluation failed");
            }
            return value;
        });
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(lazy, pending);
        final FutureTask<String> getter = new FutureTask<>(any::get);
        final Thread thread = new Thread(getter, "futures-poll-publication-getter");
        thread.setDaemon(true);
        thread.start();

        try {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while ((calls.get() == 0 || thread.getState() != Thread.State.WAITING) && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            assertEquals(1, calls.get());
            assertEquals(Thread.State.WAITING, thread.getState());
            assertEquals("winner", any.get(0, TimeUnit.NANOSECONDS));
            assertEquals("winner", getter.get(2, TimeUnit.SECONDS));
            assertFalse(pending.isDone());
        } finally {
            pending.complete("cleanup");
            thread.interrupt();
            thread.join(2_000);
        }
    }

    @Test
    @Timeout(10)
    public void testAnyOfTimedGetBoundsALazyMapperAndCanRetry() throws Exception {
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        final ContinuableFuture<String> lazy = ContinuableFuture.completed("ready").map(value -> {
            release.await();
            return value;
        });
        final ContinuableFuture<String> any = Futures.anyOf(lazy);

        try {
            Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(1),
                    () -> assertThrows(TimeoutException.class, () -> any.get(20, TimeUnit.MILLISECONDS)));
            assertFalse(lazy.isCancelled());
            release.countDown();
            assertEquals("ready", any.get(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
        }
    }

    @Test
    public void testAnyOfDirectUncheckedFailuresAreConsistentAcrossGetModes() throws Exception {
        for (final Throwable failure : Arrays.asList(new IllegalStateException("direct"), new AssertionError("direct"))) {
            final FutureTask<String> broken = new FutureTask<>(() -> "unused") {
                @Override
                public String get() {
                    if (failure instanceof Error error) {
                        throw error;
                    }
                    throw (RuntimeException) failure;
                }

                @Override
                public String get(final long timeout, final TimeUnit unit) {
                    return get();
                }
            };
            broken.run();

            for (int mode = 0; mode < 3; mode++) {
                final int getMode = mode;
                final ContinuableFuture<String> failed = Futures.anyOf(broken);
                final ExecutionException thrown = assertThrows(ExecutionException.class, () -> getAnyOf(failed, getMode));
                Assertions.assertSame(failure, thrown.getCause());
                assertEquals("winner", getAnyOf(Futures.anyOf(broken, CompletableFuture.completedFuture("winner")), getMode));
            }
            final CompletableFuture<String> pending = new CompletableFuture<>();
            assertFalse(Futures.anyOf(broken, pending).isDone(), "a failed input cannot finish a still-pending race");
        }
    }

    @Test
    public void testAnyOfMixedErrorSuppressionIsUnwrappedAndStable() {
        final AssertionError first = new AssertionError("first");
        final LinkageError second = new LinkageError("second");
        final IllegalStateException third = new IllegalStateException("third");
        final List<Future<? extends String>> failures = Arrays.asList(CompletableFuture.failedFuture(first),
                CompletableFuture.failedFuture(second), CompletableFuture.failedFuture(third), CompletableFuture.failedFuture(first));

        for (int mode = 0; mode < 3; mode++) {
            final int getMode = mode;
            final ContinuableFuture<String> any = Futures.anyOf(failures);
            final ExecutionException thrown = assertThrows(ExecutionException.class, () -> getAnyOf(any, getMode));
            Assertions.assertSame(first, thrown.getCause());
            Assertions.assertArrayEquals(new Throwable[] { second, third }, first.getSuppressed());
            Assertions.assertSame(thrown, assertThrows(ExecutionException.class, any::get));
        }
    }

    @Test
    @Timeout(10)
    public void testAnyOfExpiringWaiterDoesNotInterruptAnotherWaitersRelay() throws Exception {
        final java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicInteger interruptions = new java.util.concurrent.atomic.AtomicInteger();
        final ContinuableFuture<String> lazy = ContinuableFuture.completed("ready").map(value -> {
            entered.countDown();
            try {
                release.await();
            } catch (final InterruptedException e) {
                interruptions.incrementAndGet();
                throw e;
            }
            return value;
        });
        final ContinuableFuture<String> any = Futures.anyOf(lazy);
        final FutureTask<String> getter = new FutureTask<>(any::get);
        final Thread thread = new Thread(getter, "futures-shared-relay-getter");
        thread.setDaemon(true);
        thread.start();

        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> any.get(20, TimeUnit.MILLISECONDS));
            assertEquals(0, interruptions.get());
            release.countDown();
            assertEquals("ready", getter.get(2, TimeUnit.SECONDS));
            assertEquals(0, interruptions.get());
        } finally {
            release.countDown();
            thread.interrupt();
            thread.join(2_000);
        }
    }

    @Test
    public void testTimedIterateDrainsInBudgetFailureThenReportsTimeoutForLateSuccess() throws Exception {
        final IllegalArgumentException failure = new IllegalArgumentException("early");
        final CompletableFuture<String> late = new CompletableFuture<>();
        final ObjIterator<Result<String, Exception>> iter = Futures.iterate(
                Arrays.asList(CompletableFuture.<String> failedFuture(failure), late), 100, TimeUnit.MILLISECONDS, Function.identity());

        Thread.sleep(200);
        late.complete("late");
        Assertions.assertSame(failure, iter.next().getException());
        assertInstanceOf(TimeoutException.class, iter.next().getException());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    private static String getAnyOf(final ContinuableFuture<String> any, final int mode) throws Exception {
        return mode == 0 ? any.get() : any.get(mode == 1 ? 2 : 0, TimeUnit.SECONDS);
    }

    @Test
    @Timeout(15)
    public void testAnyOfConcurrentAggregatesDoNotDuplicateSuppressedFailures() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            for (int round = 0; round < 30; round++) {
                final IllegalStateException primary = new IllegalStateException("first");
                final List<Throwable> secondaries = new ArrayList<>();
                final List<CompletableFuture<String>> inputs = new ArrayList<>();
                inputs.add(CompletableFuture.failedFuture(primary));
                for (int i = 0; i < 32; i++) {
                    final RuntimeException failure = new RuntimeException("sibling " + i);
                    secondaries.add(failure);
                    inputs.add(CompletableFuture.failedFuture(failure));
                }
                final java.util.concurrent.CyclicBarrier start = new java.util.concurrent.CyclicBarrier(8);
                final List<Future<?>> getters = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    getters.add(pool.submit(() -> {
                        start.await(2, TimeUnit.SECONDS);
                        Assertions.assertSame(primary, assertThrows(ExecutionException.class, () -> Futures.anyOf(inputs).get()).getCause());
                        return null;
                    }));
                }
                for (final Future<?> getter : getters) {
                    getter.get(2, TimeUnit.SECONDS);
                }
                Assertions.assertArrayEquals(secondaries.toArray(new Throwable[0]), primary.getSuppressed());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    public void testTimedIterateRejectsLatePlainFutureAndInlineOutcomes() throws Exception {
        // Exercise both non-CompletableFuture producers: a background relay and an already-done
        // input whose get still performs work. Neither may admit an outcome after the budget.
        for (final boolean initiallyDone : new boolean[] { false, true }) {
            final java.util.concurrent.CountDownLatch returned = new java.util.concurrent.CountDownLatch(1);
            final FutureTask<String> late = new FutureTask<>(() -> "unused") {
                @Override
                public String get() throws InterruptedException {
                    Thread.sleep(200);
                    returned.countDown();
                    return "late";
                }
            };
            if (initiallyDone) {
                late.run();
            }
            final ObjIterator<Result<String, Exception>> iter = Futures.iterate(List.of(late), 50, TimeUnit.MILLISECONDS, Function.identity());
            assertTrue(returned.await(2, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, iter.next().getException());
            assertFalse(iter.hasNext());
            assertFalse(late.isCancelled());
        }
    }

    @Test
    public void testTimedIterateHandlerReceivesOneFinalConsumerInterruption() {
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ObjIterator<Result<String, Exception>> iter = Futures.iterate(List.of(pending), 5, TimeUnit.SECONDS, Function.identity());
        try {
            Thread.currentThread().interrupt();
            assertInstanceOf(InterruptedException.class, iter.next().getException());
            assertTrue(Thread.currentThread().isInterrupted());
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
            assertFalse(pending.isCancelled());
        } finally {
            Thread.interrupted();
            pending.complete("cleanup");
        }
    }

    @Test
    public void testCompositeCancellationContinuesAndDeduplicatesDistinctExceptions() {
        final IllegalStateException first = new IllegalStateException("first cancel failure");
        final IllegalArgumentException second = new IllegalArgumentException("second cancel failure");
        final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        final FutureTask<String> firstInput = new FutureTask<>(() -> "unused") {
            @Override
            public boolean cancel(final boolean interrupt) {
                assertTrue(interrupt);
                calls.incrementAndGet();
                throw first;
            }
        };
        final FutureTask<String> secondInput = new FutureTask<>(() -> "unused") {
            @Override
            public boolean cancel(final boolean interrupt) {
                assertTrue(interrupt);
                calls.incrementAndGet();
                throw second;
            }
        };
        final List<ContinuableFuture<?>> composites = Arrays.asList(
                Futures.compose(firstInput, secondInput, (a, b) -> "unused"),
                Futures.compose(firstInput, secondInput, secondInput, (a, b, c) -> "unused"),
                Futures.compose(List.of(firstInput, secondInput, secondInput), inputs -> "unused"),
                Futures.allOf(firstInput, secondInput), Futures.anyOf(firstInput, secondInput), Futures.combine(firstInput, secondInput));

        for (final ContinuableFuture<?> composite : composites) {
            calls.set(0);
            Assertions.assertSame(first, assertThrows(IllegalStateException.class, () -> composite.cancel(true)));
            assertTrue(calls.get() >= 2, "later inputs must be attempted after the first cancel throws");
            Assertions.assertArrayEquals(new Throwable[] { second }, first.getSuppressed());
        }
    }

    @Test
    @Timeout(10)
    public void testAnyOfExpiredDeadlineCarriesTheObservedFailuresWithoutGrowingThem() throws Exception {
        // The caller-deadline TimeoutException is this caller's own failure, so the input failures observed
        // before it expired ride along as suppressed - unwrapped exactly like a published cause, so an Error
        // input is not reported one wrapper deeper here than it would be by get().
        final IllegalStateException checkedStyle = new IllegalStateException("boom");
        final AssertionError errorStyle = new AssertionError("boom-error");
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures
                .anyOf(Arrays.asList(CompletableFuture.<String> failedFuture(checkedStyle), CompletableFuture.<String> failedFuture(errorStyle), pending));

        try {
            final TimeoutException expired = assertThrows(TimeoutException.class, () -> any.get(80, TimeUnit.MILLISECONDS));

            Assertions.assertArrayEquals(new Throwable[] { checkedStyle, errorStyle }, expired.getSuppressed(),
                    "the failures observed inside the caller's budget ride along, the Error unwrapped from its carrier");

            // Every expiring get() builds a FRESH TimeoutException, so repeating it must not accumulate
            // anything on the inputs' own exception objects - they are the user's instances, shared with
            // whoever else observes those futures.
            for (int i = 0; i < 20; i++) {
                assertThrows(TimeoutException.class, () -> any.get(5, TimeUnit.MILLISECONDS));
            }

            assertEquals(0, checkedStyle.getSuppressed().length, "an aggregate that never published must not touch its inputs' failures");
            assertEquals(0, errorStyle.getSuppressed().length);

            // The aggregate is still usable afterwards: the deadline published nothing.
            pending.complete("winner");
            assertEquals("winner", any.get());
        } finally {
            pending.complete("cleanup");
        }
    }

    @Test
    @Timeout(10)
    public void testAnyOfCanRetryAfterCompletionListenerRegistrationFails() throws Exception {
        // Like a rejected relay submission, failed listener registration must release its observation
        // claim. No listener exists to settle this input, even if the CompletableFuture completes later.
        for (final Throwable failure : Arrays.asList(new java.util.concurrent.RejectedExecutionException("registration rejected"),
                new AssertionError("registration failed"))) {
            final java.util.concurrent.atomic.AtomicInteger registrations = new java.util.concurrent.atomic.AtomicInteger();
            final CompletableFuture<String> input = new CompletableFuture<>() {
                @Override
                public CompletableFuture<String> whenComplete(final java.util.function.BiConsumer<? super String, ? super Throwable> action) {
                    if (registrations.incrementAndGet() == 1) {
                        if (failure instanceof Error error) {
                            throw error;
                        }
                        throw (RuntimeException) failure;
                    }
                    return super.whenComplete(action);
                }
            };
            final CompletableFuture<String> earlier = new CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(earlier, input);

            Assertions.assertSame(failure, assertThrows(failure.getClass(), () -> any.get(1, TimeUnit.SECONDS)));
            assertFalse(input.isCancelled());
            assertEquals(0, input.getNumberOfDependents());
            assertEquals(1, earlier.getNumberOfDependents(), "successful earlier registrations must survive a later rejection");
            earlier.completeExceptionally(new IllegalStateException("earlier input failed"));
            input.complete("winner");
            assertEquals("winner", any.get(1, TimeUnit.SECONDS));
            assertEquals(2, registrations.get());
            assertEquals("winner", any.get());
        }
    }

    /** One {@code anyOf} outcome rendered so that two getters can be compared for exact agreement. */
    private static String describeOutcome(final ContinuableFuture<String> any) {
        try {
            return "OK:" + any.get();
        } catch (final ExecutionException e) {
            return "EE:" + System.identityHashCode(e.getCause()) + ":" + e.getCause();
        } catch (final CancellationException e) {
            return "CE";
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return "INTERRUPTED";
        }
    }
}
