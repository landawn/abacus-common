package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;

@Tag("new-test")
public class Futures100Test extends TestBase {

    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testComposeWithTwoFuturesAndBiFunction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(10);
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("20");

        Throwables.BiFunction<Future<Integer>, Future<String>, String, Exception> zipFunction = (f1, f2) -> f1.get() + f2.get();

        ContinuableFuture<String> result = Futures.compose(cf1, cf2, zipFunction);

        assertEquals("1020", result.get());
        assertTrue(result.isDone());
        assertFalse(result.isCancelled());
    }

    @Test
    public void testComposeWithTwoFuturesAndTimeoutFunction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(5);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(10);

        Throwables.BiFunction<Future<Integer>, Future<Integer>, Integer, Exception> zipFunction = (f1, f2) -> f1.get() + f2.get();
        Throwables.Function<Tuple4<Future<Integer>, Future<Integer>, Long, TimeUnit>, Integer, Exception> timeoutFunction = t -> t._1.get() + t._2.get();

        ContinuableFuture<Integer> result = Futures.compose(cf1, cf2, zipFunction, timeoutFunction);

        assertEquals(15, result.get());
        assertEquals(15, result.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testComposeWithThreeFuturesAndTriFunction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(2);
        CompletableFuture<Integer> cf3 = CompletableFuture.completedFuture(3);

        Throwables.TriFunction<Future<Integer>, Future<Integer>, Future<Integer>, Integer, Exception> zipFunction = (f1, f2, f3) -> f1.get() + f2.get()
                + f3.get();

        ContinuableFuture<Integer> result = Futures.compose(cf1, cf2, cf3, zipFunction);

        assertEquals(6, result.get());
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
    public void testComposeWithCollectionAndFunction() throws Exception {
        List<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.completedFuture(1), CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3));

        Throwables.Function<List<CompletableFuture<Integer>>, Integer, Exception> zipFunction = list -> {
            int sum = 0;
            for (Future<Integer> f : list) {
                sum += f.get();
            }
            return sum;
        };

        ContinuableFuture<Integer> result = Futures.compose(futures, zipFunction);

        assertEquals(6, result.get());
    }

    @Test
    public void testComposeWithCollectionAndTimeoutFunction() throws Exception {
        List<CompletableFuture<String>> futures = Arrays.asList(CompletableFuture.completedFuture("X"), CompletableFuture.completedFuture("Y"),
                CompletableFuture.completedFuture("Z"));

        Throwables.Function<List<CompletableFuture<String>>, String, Exception> zipFunction = list -> {
            StringBuilder sb = new StringBuilder();
            for (Future<String> f : list) {
                sb.append(f.get());
            }
            return sb.toString();
        };
        Throwables.Function<Tuple3<List<CompletableFuture<String>>, Long, TimeUnit>, String, Exception> timeoutFunction = t -> {
            StringBuilder sb = new StringBuilder();
            for (Future<String> f : t._1) {
                sb.append(f.get(t._2, t._3));
            }
            return sb.toString();
        };

        ContinuableFuture<String> result = Futures.compose(futures, zipFunction, timeoutFunction);

        assertEquals("XYZ", result.get(1, TimeUnit.SECONDS));
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
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(10);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(20);
        CompletableFuture<Integer> cf3 = CompletableFuture.completedFuture(30);
        CompletableFuture<Integer> cf4 = CompletableFuture.completedFuture(40);
        CompletableFuture<Integer> cf5 = CompletableFuture.completedFuture(50);
        CompletableFuture<Integer> cf6 = CompletableFuture.completedFuture(60);

        ContinuableFuture<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> result = Futures.combine(cf1, cf2, cf3, cf4, cf5, cf6);
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple = result.get();

        assertEquals(10, tuple._1);
        assertEquals(20, tuple._2);
        assertEquals(30, tuple._3);
        assertEquals(40, tuple._4);
        assertEquals(50, tuple._5);
        assertEquals(60, tuple._6);
    }

    @Test
    public void testCombineSevenFutures() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("1");
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("2");
        CompletableFuture<String> cf3 = CompletableFuture.completedFuture("3");
        CompletableFuture<String> cf4 = CompletableFuture.completedFuture("4");
        CompletableFuture<String> cf5 = CompletableFuture.completedFuture("5");
        CompletableFuture<String> cf6 = CompletableFuture.completedFuture("6");
        CompletableFuture<String> cf7 = CompletableFuture.completedFuture("7");

        ContinuableFuture<Tuple7<String, String, String, String, String, String, String>> result = Futures.combine(cf1, cf2, cf3, cf4, cf5, cf6, cf7);
        Tuple7<String, String, String, String, String, String, String> tuple = result.get();

        assertEquals("1", tuple._1);
        assertEquals("2", tuple._2);
        assertEquals("3", tuple._3);
        assertEquals("4", tuple._4);
        assertEquals("5", tuple._5);
        assertEquals("6", tuple._6);
        assertEquals("7", tuple._7);
    }

    @Test
    public void testCombineTwoFuturesWithBiFunction() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(10);
        CompletableFuture<Integer> cf2 = CompletableFuture.completedFuture(20);

        Throwables.BiFunction<Integer, Integer, Integer, Exception> action = (a, b) -> a + b;

        ContinuableFuture<Integer> result = Futures.combine(cf1, cf2, action);

        assertEquals(30, result.get());
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
    public void testAllOfCollection() throws Exception {
        Collection<CompletableFuture<String>> futures = Arrays.asList(CompletableFuture.completedFuture("Hello"), CompletableFuture.completedFuture("World"),
                CompletableFuture.completedFuture("!"));

        ContinuableFuture<List<String>> result = Futures.allOf(futures);
        List<String> list = result.get();

        assertEquals(3, list.size());
        assertEquals("Hello", list.get(0));
        assertEquals("World", list.get(1));
        assertEquals("!", list.get(2));
    }

    @Test
    @Timeout(5)
    public void testAllOfWithTimeout() throws Exception {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(100);
        CompletableFuture<Integer> cf2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 200;
        });

        ContinuableFuture<List<Integer>> result = Futures.allOf(cf1, cf2);
        List<Integer> list = result.get(2, TimeUnit.SECONDS);

        assertEquals(2, list.size());
        assertEquals(100, list.get(0));
        assertEquals(200, list.get(1));
    }

    @Test
    public void testAllOfWithException() {
        CompletableFuture<Integer> cf1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Integer> cf2 = CompletableFuture.failedFuture(new RuntimeException("Test exception"));

        ContinuableFuture<List<Integer>> result = Futures.allOf(cf1, cf2);

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testAnyOfVarargs() throws Exception {
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Slow";
        });
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("Fast");

        ContinuableFuture<String> result = Futures.anyOf(cf1, cf2);

        assertEquals("Fast", result.get());
    }

    @Test
    public void testAnyOfCollection() throws Exception {
        Collection<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 300;
        }), CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 100;
        }), CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 200;
        }));

        ContinuableFuture<Integer> result = Futures.anyOf(futures);
        Integer value = result.get();

        assertEquals(100, value);
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
    public void testIterateCollection() throws Exception {
        Collection<CompletableFuture<String>> futures = Arrays.asList(CompletableFuture.completedFuture("A"), CompletableFuture.completedFuture("B"),
                CompletableFuture.completedFuture("C"));

        ObjIterator<String> iter = Futures.iterate(futures);

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
    public void testIterateWithTimeout() throws Exception {
        Collection<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.completedFuture(10), CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 20;
        }));

        ObjIterator<Integer> iter = Futures.iterate(futures, 2, TimeUnit.SECONDS);

        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(2, results.size());
        assertTrue(results.contains(10));
        assertTrue(results.contains(20));
    }

    @Test
    public void testIterateWithResultHandler() throws Exception {
        Collection<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.completedFuture(1),
                CompletableFuture.failedFuture(new RuntimeException("Error")), CompletableFuture.completedFuture(3));

        Function<Result<Integer, Exception>, String> resultHandler = result -> {
            if (result.isSuccess()) {
                return "Success: " + result.orElseIfFailure(null);
            } else {
                return "Failure: " + result.getException().getMessage();
            }
        };

        ObjIterator<String> iter = Futures.iterate(futures, resultHandler);

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(s -> s.equals("Success: 1")));
        assertTrue(results.stream().anyMatch(s -> s.equals("Success: 3")));
        assertTrue(results.stream().anyMatch(s -> s.contains("Failure: Error")));
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
    public void testComposeWithEmptyCollectionThrowsException() {
        List<CompletableFuture<Integer>> emptyList = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            Futures.compose(emptyList, list -> 0);
        });
    }

    @Test
    public void testAllOfWithEmptyCollectionThrowsException() {
        List<CompletableFuture<Integer>> emptyList = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            Futures.allOf(emptyList);
        });
    }

    @Test
    public void testAnyOfWithEmptyCollectionThrowsException() {
        List<CompletableFuture<Integer>> emptyList = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            Futures.anyOf(emptyList);
        });
    }

    @Test
    public void testIterateNoSuchElementException() {
        CompletableFuture<Integer> cf = CompletableFuture.completedFuture(1);
        ObjIterator<Integer> iter = Futures.iterate(cf);

        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
        assertFalse(iter.hasNext());

        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testAllOfWithMultipleExceptions() {
        CompletableFuture<Integer> cf1 = CompletableFuture.failedFuture(new RuntimeException("Error 1"));
        CompletableFuture<Integer> cf2 = CompletableFuture.failedFuture(new RuntimeException("Error 2"));

        ContinuableFuture<List<Integer>> result = Futures.allOf(cf1, cf2);

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testAnyOfAllFailed() {
        CompletableFuture<String> cf1 = CompletableFuture.failedFuture(new RuntimeException("Error 1"));
        CompletableFuture<String> cf2 = CompletableFuture.failedFuture(new RuntimeException("Error 2"));

        ContinuableFuture<String> result = Futures.anyOf(cf1, cf2);

        assertThrows(RuntimeException.class, () -> result.get());
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
}
