package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;

@Tag("2025")
public class Futures2025Test extends TestBase {

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

        Throwables.Function<List<Future<Integer>>, Integer, Exception> zipFunction = list -> {
            int sum = 0;
            for (Future<Integer> f : list) {
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

        Throwables.Function<List<Future<Integer>>, Integer, Exception> zipFunction = list -> 0;

        assertThrows(IllegalArgumentException.class, () -> Futures.compose(cfs, zipFunction));
    }

    @Test
    public void testComposeCollectionNull() {
        Throwables.Function<List<Future<Integer>>, Integer, Exception> zipFunction = list -> 0;

        assertThrows(IllegalArgumentException.class, () -> Futures.compose(null, zipFunction));
    }

    @Test
    public void testComposeCollectionWithTimeoutFunction() throws Exception {
        List<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(10), CompletableFuture.completedFuture(20));

        Throwables.Function<List<Future<Integer>>, Integer, Exception> zipFunction = list -> {
            int sum = 0;
            for (Future<Integer> f : list) {
                sum += f.get();
            }
            return sum;
        };

        Throwables.Function<Tuple3<List<Future<Integer>>, Long, TimeUnit>, Integer, Exception> timeoutFunction = t -> {
            int sum = 0;
            for (Future<Integer> f : t._1) {
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
        Throwables.Function<List<Future<Integer>>, Integer, Exception> zipFunction = list -> 0;

        assertThrows(IllegalArgumentException.class, () -> Futures.compose(cfs, null, null));
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
    public void testAllOfVarargs() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(2);
        CompletableFuture<Integer> cf3 = CompletableFuture.completedFuture(3);

        ContinuableFuture<List<Integer>> result = Futures.allOf(cf1, cf2, cf3);

        List<Integer> list = result.get();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
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
    public void testAllOfVarargsCancel() {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ContinuableFuture<List<Integer>> result = Futures.allOf(cf1, cf2);

        assertTrue(result.cancel(true));
        assertTrue(cf1.isCancelled());
        assertTrue(cf2.isCancelled());
    }

    @Test
    public void testAllOfVarargsIsDone() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ContinuableFuture<List<Integer>> result = Futures.allOf(cf1, cf2);

        assertFalse(result.isDone());
        cf2.complete(2);
        assertTrue(result.isDone());
    }

    @Test
    public void testAllOfCollection() throws Exception {
        Collection<Future<Integer>> cfs = Arrays.asList(CompletableFuture.completedFuture(10), CompletableFuture.completedFuture(20),
                CompletableFuture.completedFuture(30));

        ContinuableFuture<List<Integer>> result = Futures.allOf(cfs);

        List<Integer> list = result.get();
        assertEquals(3, list.size());
        assertEquals(10, list.get(0));
        assertEquals(20, list.get(1));
        assertEquals(30, list.get(2));
    }

    @Test
    public void testAllOfCollectionEmpty() {
        Collection<Future<Integer>> cfs = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> Futures.allOf(cfs));
    }

    @Test
    public void testAllOfCollectionWithException() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();
        cf2.completeExceptionally(new RuntimeException("Test error"));

        Collection<Future<Integer>> cfs = Arrays.asList(cf1, cf2);
        ContinuableFuture<List<Integer>> result = Futures.allOf(cfs);

        assertThrows(ExecutionException.class, () -> result.get());
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
    public void testAnyOfVarargsCancel() {
        CompletableFuture<Integer> cf1 = new CompletableFuture<>();
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ContinuableFuture<Integer> result = Futures.anyOf(cf1, cf2);

        assertTrue(result.cancel(true));
        assertTrue(cf1.isCancelled());
        assertTrue(cf2.isCancelled());
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
    public void testAnyOfVarargsIsDone() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = new CompletableFuture<>();

        ContinuableFuture<Integer> result = Futures.anyOf(cf1, cf2);

        assertTrue(result.isDone());
    }

    @Test
    public void testAnyOfCollection() throws Exception {
        Collection<Future<String>> cfs = Arrays.asList(CompletableFuture.completedFuture("Result"), new CompletableFuture<>(), new CompletableFuture<>());

        ContinuableFuture<String> result = Futures.anyOf(cfs);

        assertEquals("Result", result.get());
    }

    @Test
    public void testAnyOfCollectionEmpty() {
        Collection<Future<Integer>> cfs = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> Futures.anyOf(cfs));
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
        CompletableFuture<Integer> cf1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 1;
        }, executor);

        CompletableFuture<Integer> cf2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 2;
        }, executor);

        ObjIterator<Integer> iter = Futures.iterate(cf1, cf2);

        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertEquals(2, results.get(0));
        assertEquals(1, results.get(1));
    }
}
