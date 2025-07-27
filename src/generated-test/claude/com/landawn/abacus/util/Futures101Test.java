package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;

public class Futures101Test extends TestBase {

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @Test
    public void testComposeWithBiFunction() throws Exception {
        Future<Integer> future1 = CompletableFuture.completedFuture(5);
        Future<Integer> future2 = CompletableFuture.completedFuture(10);
        
        ContinuableFuture<Integer> composed = Futures.compose(future1, future2, 
            (f1, f2) -> f1.get() + f2.get());
        
        Assertions.assertEquals(15, composed.get());
        
        // Test with timeout
        Assertions.assertEquals(15, composed.get(1, TimeUnit.SECONDS));
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
        
        ContinuableFuture<String> composed = Futures.compose(slowFuture, fastFuture,
            (f1, f2) -> f1.get() + " + " + f2.get(),
            tuple -> {
                try {
                    return "Timeout: " + tuple._2.get(tuple._3, tuple._4);
                } catch (Exception e) {
                    return "Timeout occurred";
                }
            });
        
        // Normal get should work
        Assertions.assertEquals("Slow + Fast", composed.get());
        
        // Test timeout path
        Future<String> verySlowFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Very Slow";
        });
        
        ContinuableFuture<String> composed2 = Futures.compose(verySlowFuture, fastFuture,
            (f1, f2) -> f1.get() + " + " + f2.get(),
            tuple -> "Timeout: " + tuple._2.get());
        
        String result = composed2.get(100, TimeUnit.MILLISECONDS);
        Assertions.assertEquals("Timeout: Fast", result);
    }

    @Test
    public void testComposeThreeFutures() throws Exception {
        Future<String> f1 = CompletableFuture.completedFuture("A");
        Future<String> f2 = CompletableFuture.completedFuture("B");
        Future<String> f3 = CompletableFuture.completedFuture("C");
        
        ContinuableFuture<String> composed = Futures.compose(f1, f2, f3,
            (fu1, fu2, fu3) -> fu1.get() + fu2.get() + fu3.get());
        
        Assertions.assertEquals("ABC", composed.get());
    }

    @Test
    public void testComposeCollection() throws Exception {
        List<Future<Integer>> futures = Arrays.asList(
            CompletableFuture.completedFuture(1),
            CompletableFuture.completedFuture(2),
            CompletableFuture.completedFuture(3)
        );
        
        ContinuableFuture<Integer> sum = Futures.compose(futures, list -> {
            int total = 0;
            for (Future<Integer> f : list) {
                total += f.get();
            }
            return total;
        });
        
        Assertions.assertEquals(6, sum.get());
    }

    @Test
    public void testCombineTwoFutures() throws Exception {
        Future<String> f1 = CompletableFuture.completedFuture("Hello");
        Future<Integer> f2 = CompletableFuture.completedFuture(42);
        
        ContinuableFuture<Tuple2<String, Integer>> combined = Futures.combine(f1, f2);
        Tuple2<String, Integer> result = combined.get();
        
        Assertions.assertEquals("Hello", result._1);
        Assertions.assertEquals(42, result._2);
    }

    @Test
    public void testCombineThreeFutures() throws Exception {
        Future<String> f1 = CompletableFuture.completedFuture("A");
        Future<Integer> f2 = CompletableFuture.completedFuture(1);
        Future<Boolean> f3 = CompletableFuture.completedFuture(true);
        
        ContinuableFuture<Tuple3<String, Integer, Boolean>> combined = Futures.combine(f1, f2, f3);
        Tuple3<String, Integer, Boolean> result = combined.get();
        
        Assertions.assertEquals("A", result._1);
        Assertions.assertEquals(1, result._2);
        Assertions.assertTrue(result._3);
    }

    @Test
    public void testCombineFourToSevenFutures() throws Exception {
        // Test Tuple4
        ContinuableFuture<Tuple4<Integer, Integer, Integer, Integer>> t4 = 
            Futures.combine(
                CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3),
                CompletableFuture.completedFuture(4)
            );
        Assertions.assertEquals(Tuple.of(1, 2, 3, 4), t4.get());
        
        // Test Tuple5
        ContinuableFuture<Tuple5<Integer, Integer, Integer, Integer, Integer>> t5 = 
            Futures.combine(
                CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3),
                CompletableFuture.completedFuture(4),
                CompletableFuture.completedFuture(5)
            );
        Assertions.assertEquals(Tuple.of(1, 2, 3, 4, 5), t5.get());
        
        // Test Tuple6
        ContinuableFuture<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> t6 = 
            Futures.combine(
                CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3),
                CompletableFuture.completedFuture(4),
                CompletableFuture.completedFuture(5),
                CompletableFuture.completedFuture(6)
            );
        Assertions.assertEquals(Tuple.of(1, 2, 3, 4, 5, 6), t6.get());
        
        // Test Tuple7
        ContinuableFuture<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> t7 = 
            Futures.combine(
                CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3),
                CompletableFuture.completedFuture(4),
                CompletableFuture.completedFuture(5),
                CompletableFuture.completedFuture(6),
                CompletableFuture.completedFuture(7)
            );
        Assertions.assertEquals(Tuple.of(1, 2, 3, 4, 5, 6, 7), t7.get());
    }

    @Test
    public void testCombineWithAction() throws Exception {
        Future<Integer> f1 = CompletableFuture.completedFuture(10);
        Future<Integer> f2 = CompletableFuture.completedFuture(20);
        
        ContinuableFuture<Integer> result = Futures.combine(f1, f2, (a, b) -> a + b);
        Assertions.assertEquals(30, result.get());
        
        // Test with three futures
        Future<Integer> f3 = CompletableFuture.completedFuture(30);
        ContinuableFuture<Integer> result3 = Futures.combine(f1, f2, f3, (a, b, c) -> a + b + c);
        Assertions.assertEquals(60, result3.get());
    }

    @Test
    public void testCombineCollectionWithAction() throws Exception {
        List<Future<Integer>> futures = Arrays.asList(
            CompletableFuture.completedFuture(1),
            CompletableFuture.completedFuture(2),
            CompletableFuture.completedFuture(3),
            CompletableFuture.completedFuture(4)
        );
        
        ContinuableFuture<Integer> sum = Futures.combine(futures, 
            list -> list.stream().mapToInt(Integer::intValue).sum());
        
        Assertions.assertEquals(10, sum.get());
    }

    @Test
    public void testAllOf() throws Exception {
        Future<String> f1 = CompletableFuture.completedFuture("A");
        Future<String> f2 = CompletableFuture.completedFuture("B");
        Future<String> f3 = CompletableFuture.completedFuture("C");
        
        // Test array version
        ContinuableFuture<List<String>> allArray = Futures.allOf(f1, f2, f3);
        List<String> resultArray = allArray.get();
        Assertions.assertEquals(Arrays.asList("A", "B", "C"), resultArray);
        
        // Test collection version
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
        
        // Test timeout exceeded
        CompletableFuture<String> f3 = new CompletableFuture<>();
        ContinuableFuture<List<String>> all2 = Futures.allOf(f3);
        Assertions.assertThrows(TimeoutException.class, () -> all2.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAnyOf() throws Exception {
        CompletableFuture<String> f1 = new CompletableFuture<>();
        CompletableFuture<String> f2 = new CompletableFuture<>();
        CompletableFuture<String> f3 = new CompletableFuture<>();
        
        ContinuableFuture<String> any = Futures.anyOf(f1, f2, f3);
        
        // Complete f2 first
        f2.complete("Second");
        Assertions.assertEquals("Second", any.get());
        
        // Complete others later
        f1.complete("First");
        f3.complete("Third");
        
        // Result should still be "Second"
        // Assertions.assertEquals("First", any.get());
    }

    @Test
    public void testAnyOfAllFailed() {
        Future<String> f1 = CompletableFuture.failedFuture(new RuntimeException("Error1"));
        Future<String> f2 = CompletableFuture.failedFuture(new RuntimeException("Error2"));
        Future<String> f3 = CompletableFuture.failedFuture(new RuntimeException("Error3"));
        
        ContinuableFuture<String> any = Futures.anyOf(f1, f2, f3);
        
        // Should throw the last exception
        Assertions.assertThrows(RuntimeException.class, () -> any.get());
    }

    @Test
    public void testIterate() throws Exception {
        Future<Integer> f1 = CompletableFuture.completedFuture(1);
        Future<Integer> f2 = CompletableFuture.completedFuture(2);
        Future<Integer> f3 = CompletableFuture.completedFuture(3);
        
        ObjIterator<Integer> iter = Futures.iterate(f1, f2, f3);
        
        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }
        
        // Results should contain all values (order may vary)
        Assertions.assertEquals(3, results.size());
        Assertions.assertTrue(results.contains(1));
        Assertions.assertTrue(results.contains(2));
        Assertions.assertTrue(results.contains(3));
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
        
        // First completed should come first
        Assertions.assertEquals("Third", results.get(0));
        Assertions.assertEquals("First", results.get(1));
        Assertions.assertEquals("Second", results.get(2));
    }

    @Test
    public void testIterateWithTimeout() throws Exception {
        CompletableFuture<String> f1 = CompletableFuture.completedFuture("Quick");
        CompletableFuture<String> f2 = new CompletableFuture<>(); // Never completes
        
        List<Future<String>> futures = Arrays.asList(f1, f2);
        ObjIterator<String> iter = Futures.iterate(futures, 500, TimeUnit.MILLISECONDS);
        
        // First should complete
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("Quick", iter.next());
        
        // Second should timeout
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
                return "Success: " + result.orElseThrow(() -> new RuntimeException("No value"));
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
    public void testIterateNoSuchElement() {
        Future<String> f1 = CompletableFuture.completedFuture("Only");
        ObjIterator<String> iter = Futures.iterate(f1);
        
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("Only", iter.next());
        Assertions.assertFalse(iter.hasNext());
        
        // Should throw NoSuchElementException
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testCancelBehavior() throws Exception {
        CompletableFuture<String> f1 = new CompletableFuture<>();
        CompletableFuture<String> f2 = new CompletableFuture<>();
        
        ContinuableFuture<List<String>> all = Futures.allOf(f1, f2);
        
        // Cancel should propagate to all futures
        boolean cancelled = all.cancel(true);
        Assertions.assertTrue(cancelled);
        Assertions.assertTrue(all.isCancelled());
        Assertions.assertTrue(f1.isCancelled());
        Assertions.assertTrue(f2.isCancelled());
    }

    @Test
    public void testIsDone() {
        CompletableFuture<String> f1 = new CompletableFuture<>();
        CompletableFuture<String> f2 = CompletableFuture.completedFuture("Done");
        
        ContinuableFuture<List<String>> all = Futures.allOf(f1, f2);
        Assertions.assertFalse(all.isDone());
        
        f1.complete("Also done");
        Assertions.assertTrue(all.isDone());
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
    public void testConvertException() {
        Exception e1 = new RuntimeException("test");
        Assertions.assertEquals(e1, Futures.convertException(e1));
        
        Exception cause = new IllegalArgumentException("cause");
        ExecutionException e2 = new ExecutionException(cause);
        Assertions.assertEquals(cause, Futures.convertException(e2));
        
        ExecutionException e3 = new ExecutionException(new Error("error"));
        Assertions.assertEquals(e3, Futures.convertException(e3));
    }
}