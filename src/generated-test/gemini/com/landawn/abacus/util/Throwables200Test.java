package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;

/**
 * Comprehensive unit tests for the Throwables class.
 */
public class Throwables200Test extends TestBase {

    //region 'run' methods Tests

    @Test
    public void testRun_withoutException() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.run(() -> executed.set(true));
        assertTrue(executed.get(), "Runnable should have been executed.");
    }

    @Test
    public void testRun_withCheckedException() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.run(() -> {
                throw new IOException("Test Exception");
            });
        }, "A checked exception should be wrapped in a RuntimeException.");
    }

    @Test
    public void testRun_withRuntimeException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Throwables.run(() -> {
                throw new IllegalArgumentException("Test Runtime Exception");
            });
        }, "A RuntimeException should be rethrown as is.");
    }

    @Test
    public void testRun_withActionOnError_noException() {
        final AtomicBoolean cmdExecuted = new AtomicBoolean(false);
        final AtomicBoolean errorActionExecuted = new AtomicBoolean(false);

        Throwables.run(() -> cmdExecuted.set(true), e -> errorActionExecuted.set(true));

        assertTrue(cmdExecuted.get(), "Command should have been executed.");
        assertFalse(errorActionExecuted.get(), "Error action should not have been executed.");
    }

    @Test
    public void testRun_withActionOnError_withException() {
        final AtomicBoolean errorActionExecuted = new AtomicBoolean(false);
        final Exception testException = new IOException("Test");

        Throwables.run(() -> {
            throw testException;
        }, e -> {
            errorActionExecuted.set(true);
            assertSame(testException, e, "The correct exception should be passed to the error action.");
        });

        assertTrue(errorActionExecuted.get(), "Error action should have been executed.");
    }
    //endregion

    //region 'call' methods Tests

    @Test
    public void testCall_withoutException() {
        String result = Throwables.call(() -> "success");
        assertEquals("success", result, "The result of the callable should be returned.");
    }

    @Test
    public void testCall_withCheckedException() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                if (true)
                    throw new IOException("Test Exception");
                return "failure";
            });
        }, "A checked exception should be wrapped in a RuntimeException.");
    }

    @Test
    public void testCall_withActionOnError() {
        String result = Throwables.call(() -> {
            throw new IOException("Test");
        }, () -> "handled");
        assertEquals("handled", result, "The actionOnError function should provide the return value.");
    }

    @Test
    public void testCall_withSupplier() {
        String result = Throwables.call(() -> {
            throw new Exception("Test");
        }, () -> "supplied");
        assertEquals("supplied", result, "The supplier should provide the return value on error.");
    }

    @Test
    public void testCall_withDefaultValue() {
        String result = Throwables.call(() -> {
            throw new Exception("Test");
        }, "default");
        assertEquals("default", result, "The default value should be returned on error.");
    }

    @Test
    public void testCall_withPredicateAndSupplier_predicateTrue() {
        String result = Throwables.call(() -> {
            throw new IOException("IO Test");
        }, e -> e instanceof IOException, () -> "supplied_on_io");
        assertEquals("supplied_on_io", result, "Supplier should be used when predicate is true.");
    }

    @Test
    public void testCall_withPredicateAndSupplier_predicateFalse() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                throw new IllegalArgumentException("Arg Test");
            }, e -> e instanceof IOException, () -> "supplied_on_io");
        }, "Exception should be rethrown when predicate is false.");
    }

    @Test
    public void testCall_withPredicateAndDefaultValue_predicateTrue() {
        String result = Throwables.call(() -> {
            throw new IOException("IO Test");
        }, e -> e instanceof IOException, "default_on_io");
        assertEquals("default_on_io", result, "Default value should be used when predicate is true.");
    }

    @Test
    public void testCall_withPredicateAndDefaultValue_predicateFalse() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                throw new IllegalArgumentException("Arg Test");
            }, e -> e instanceof IOException, "default_on_io");
        }, "Exception should be rethrown when predicate is false.");
    }
    //endregion

    //region Iterator Tests

    @Test
    public void testIterator_empty() throws Throwable {
        Throwables.Iterator<Object, ?> emptyIterator = Throwables.Iterator.empty();
        assertFalse(emptyIterator.hasNext(), "empty iterator should not have next.");
        assertThrows(NoSuchElementException.class, emptyIterator::next);
        assertEquals(0, emptyIterator.count());
    }

    @Test
    public void testIterator_just() throws Throwable {
        Throwables.Iterator<String, ?> singleIterator = Throwables.Iterator.just("one");
        assertTrue(singleIterator.hasNext());
        assertEquals("one", singleIterator.next());
        assertFalse(singleIterator.hasNext());
        assertThrows(NoSuchElementException.class, singleIterator::next);
    }

    @Test
    public void testIterator_ofArray() throws Throwable {
        Throwables.Iterator<Integer, ?> iterator = Throwables.Iterator.of(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), iterator.toList());
    }

    @Test
    public void testIterator_ofArraySlice() throws Throwable {
        Integer[] source = { 0, 1, 2, 3, 4, 5 };
        Throwables.Iterator<Integer, ?> iterator = Throwables.Iterator.of(source, 2, 4); // should contain {2, 3}
        assertEquals(Arrays.asList(2, 3), iterator.toList());
    }

    @Test
    public void testIterator_ofIterable() throws Throwable {
        List<String> sourceList = Arrays.asList("a", "b", "c");
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of(sourceList);
        assertEquals(sourceList, iterator.toList());
    }

    @Test
    public void testIterator_ofJavaIterator() throws Throwable {
        List<String> sourceList = Arrays.asList("a", "b", "c");
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of(sourceList.iterator());
        assertEquals(sourceList, iterator.toList());
    }

    @Test
    public void testIterator_ofLines() throws IOException {
        StringReader reader = new StringReader("line 1\nline 2\nline 3");
        Throwables.Iterator<String, IOException> lineIterator = Throwables.Iterator.ofLines(reader);
        assertEquals(Arrays.asList("line 1", "line 2", "line 3"), lineIterator.toList());
    }

    @Test
    public void testIterator_defer() throws Throwable {
        AtomicInteger supplierCalls = new AtomicInteger(0);
        Throwables.Iterator<Integer, ?> deferred = Throwables.Iterator.defer(() -> {
            supplierCalls.incrementAndGet();
            return Throwables.Iterator.of(1, 2);
        });

        assertEquals(0, supplierCalls.get(), "Supplier should not be called on creation.");

        assertTrue(deferred.hasNext(), "hasNext should trigger initialization.");
        assertEquals(1, supplierCalls.get(), "Supplier should be called on first hasNext.");

        assertEquals(1, deferred.next());
        assertEquals(1, supplierCalls.get(), "Supplier should not be called again.");

        assertEquals(Arrays.asList(2), deferred.toList());
        assertEquals(1, supplierCalls.get(), "Supplier should not be called again during consumption.");
    }

    @Test
    public void testIterator_concat() throws Throwable {
        Throwables.Iterator<Integer, Exception> iter1 = Throwables.Iterator.of(1, 2);
        Throwables.Iterator<Integer, Exception> iter2 = Throwables.Iterator.empty();
        Throwables.Iterator<Integer, Exception> iter3 = Throwables.Iterator.of(3, 4);

        Throwables.Iterator<Integer, ?> concatenated = Throwables.Iterator.concat(iter1, iter2, iter3);

        assertEquals(Arrays.asList(1, 2, 3, 4), concatenated.toList());
    }

    @Test
    public void testIterator_close() {
        final AtomicBoolean closed = new AtomicBoolean(false);
        Throwables.Iterator<Integer, Exception> closableIterator = new Throwables.Iterator<>() {
            private final Throwables.Iterator<Integer, Exception> internal = Throwables.Iterator.of(1, 2, 3);

            @Override
            public boolean hasNext() throws Exception {
                return internal.hasNext();
            }

            @Override
            public Integer next() throws Exception {
                return internal.next();
            }

            @Override
            protected void closeResource() {
                closed.set(true);
            }
        };

        try (closableIterator) {
            assertEquals(3, closableIterator.count());
        } catch (Throwable e) {
            fail("Should not throw exception on close");
        }

        assertTrue(closed.get(), "closeResource should be called when iterator is closed.");
    }

    @Test
    public void testIterator_forEachRemaining() throws Throwable {
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of("x", "y", "z");
        List<String> items = new ArrayList<>();
        iterator.forEachRemaining(items::add);
        assertEquals(Arrays.asList("x", "y", "z"), items);
    }

    @Test
    public void testIterator_foreachRemaining_throwable() throws Throwable {
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of("x", "y", "z");
        List<String> items = new ArrayList<>();
        iterator.foreachRemaining(items::add);
        assertEquals(Arrays.asList("x", "y", "z"), items);
    }

    @Test
    public void testIterator_foreachRemaining_throwable_withException() {
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of("x", "y", "z");
        assertThrows(SQLException.class, () -> {
            iterator.foreachRemaining(item -> {
                if (item.equals("y")) {
                    throw new SQLException("Test SQL Exception");
                }
            });
        });
    }

    @Test
    public void testIterator_instanceMethods() throws Throwable {
        Throwables.Iterator<Integer, Exception> original = Throwables.Iterator.of(1, 2, 3, 4, 5, null);

        // filter
        Throwables.Iterator<Integer, ?> filtered = original.filter(i -> i != null && i % 2 != 0);
        assertEquals(Arrays.asList(1, 3, 5), filtered.toList());

        // map
        original = Throwables.Iterator.of(1, 2, 3);
        Throwables.Iterator<String, ?> mapped = original.map(i -> "v" + i);
        assertEquals(Arrays.asList("v1", "v2", "v3"), mapped.toList());

        // first
        original = Throwables.Iterator.of(1, 2, 3);
        assertEquals(Nullable.of(1), original.first());
        assertEquals(Nullable.empty(), Throwables.Iterator.empty().first());

        // firstNonNull
        original = Throwables.Iterator.of(null, null, 1, 2);
        assertEquals(com.landawn.abacus.util.u.Optional.of(1), original.firstNonNull());

        // last
        original = Throwables.Iterator.of(1, 2, 3);
        assertEquals(Nullable.of(3), original.last());

        // toArray
        original = Throwables.Iterator.of(1, 2, 3);
        assertArrayEquals(new Object[] { 1, 2, 3 }, original.toArray());

        // toArray(T[])
        original = Throwables.Iterator.of(4, 5, 6);
        assertArrayEquals(new Integer[] { 4, 5, 6 }, original.toArray(new Integer[0]));

        // advance
        original = Throwables.Iterator.of(1, 2, 3, 4, 5);
        original.advance(2);
        assertEquals(3, original.next());

        // foreachIndexed
        Throwables.Iterator<String, Exception> original2 = Throwables.Iterator.of("a", "b");
        final List<String> indexedItems = new ArrayList<>();
        original2.foreachIndexed((idx, item) -> indexedItems.add(idx + ":" + item));
        assertEquals(Arrays.asList("0:a", "1:b"), indexedItems);
    }
    //endregion

    //region Functional Interfaces Tests

    @Test
    public void testRunnable_unchecked() {
        Throwables.Runnable<IOException> throwableRunnable = () -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Runnable uncheckedRunnable = throwableRunnable.unchecked();
        assertThrows(RuntimeException.class, uncheckedRunnable::run);
    }

    @Test
    public void testCallable_unchecked() {
        Throwables.Callable<String, IOException> throwableCallable = () -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Callable<String> uncheckedCallable = throwableCallable.unchecked();
        assertThrows(RuntimeException.class, uncheckedCallable::call);
    }

    @Test
    public void testFunction_unchecked() {
        Throwables.Function<String, Integer, IOException> throwableFunction = s -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Function<String, Integer> uncheckedFunction = throwableFunction.unchecked();
        assertThrows(RuntimeException.class, () -> uncheckedFunction.apply("test"));
    }

    @Test
    public void testBiFunction_unchecked() {
        Throwables.BiFunction<String, Integer, Boolean, IOException> throwableBiFunction = (s, i) -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.BiFunction<String, Integer, Boolean> uncheckedBiFunction = throwableBiFunction.unchecked();
        assertThrows(RuntimeException.class, () -> uncheckedBiFunction.apply("test", 123));
    }

    @Test
    public void testPredicate_unchecked() {
        Throwables.Predicate<String, IOException> throwablePredicate = s -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Predicate<String> uncheckedPredicate = throwablePredicate.unchecked();
        assertThrows(RuntimeException.class, () -> uncheckedPredicate.test("test"));
    }

    @Test
    public void testPredicate_negate() throws Throwable {
        Throwables.Predicate<Integer, ?> isEven = i -> i % 2 == 0;
        Throwables.Predicate<Integer, ?> isOdd = isEven.negate();

        assertFalse(isEven.test(3));
        assertTrue(isOdd.test(3));

        assertTrue(isEven.test(4));
        assertFalse(isOdd.test(4));
    }

    @Test
    public void testConsumer_unchecked() {
        Throwables.Consumer<String, IOException> throwableConsumer = s -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Consumer<String> uncheckedConsumer = throwableConsumer.unchecked();
        assertThrows(RuntimeException.class, () -> uncheckedConsumer.accept("test"));
    }

    @Test
    public void testNFunction_andThen() throws Throwable {
        Throwables.NFunction<Integer, Integer, ?> sum = args -> {
            int total = 0;
            for (int i : args)
                total += i;
            return total;
        };

        Throwables.NFunction<Integer, String, ?> sumAndStringify = sum.andThen(result -> "Sum is " + result);

        String result = sumAndStringify.apply(1, 2, 3, 4);
        assertEquals("Sum is 10", result);
    }

    //endregion

    //region LazyInitializer Tests

    @Test
    public void testLazyInitializer() throws Throwable {
        final AtomicInteger supplierCalls = new AtomicInteger(0);
        final Throwables.Supplier<String, Exception> supplier = () -> {
            supplierCalls.incrementAndGet();
            return "initialized";
        };

        Throwables.Supplier<String, ?> lazy = N.lazyInitialize(supplier);

        assertEquals(0, supplierCalls.get(), "Supplier should not be called on creation.");

        String value1 = lazy.get();
        assertEquals("initialized", value1);
        assertEquals(1, supplierCalls.get(), "Supplier should be called on first get().");

        String value2 = lazy.get();
        assertEquals("initialized", value2);
        assertEquals(1, supplierCalls.get(), "Supplier should not be called on subsequent get() calls.");
        assertSame(value1, value2, "Should return the same cached instance.");
    }

    @Test
    public void testLazyInitializer_threadSafety() throws InterruptedException {
        final AtomicInteger supplierCalls = new AtomicInteger(0);
        final Throwables.Supplier<String, Exception> supplier = () -> {
            try {
                // Simulate work
                Thread.sleep(100);
            } catch (InterruptedException e) {
                fail("Sleep interrupted");
            }
            supplierCalls.incrementAndGet();
            return "initialized";
        };

        final Throwables.Supplier<String, Exception> lazy = N.lazyInitialize(supplier);
        final int numThreads = 10;
        final Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                try {
                    assertEquals("initialized", lazy.get());
                } catch (Throwable e) {
                    fail("Exception in thread", e);
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(1, supplierCalls.get(), "Supplier must be called exactly once in a multi-threaded environment.");
    }

    //endregion
}
