package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

public class ObjIteratorExTest extends TestBase {

    @Test
    public void testEmptySingleton() {
        ObjIteratorEx<String> empty1 = ObjIteratorEx.empty();
        ObjIteratorEx<Integer> empty2 = ObjIteratorEx.empty();
        Assertions.assertSame(empty1, empty2);
    }

    @Test
    public void testEmpty() {
        ObjIteratorEx<String> iter = ObjIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
        Assertions.assertEquals(0, iter.count());
        iter.closeResource();
    }

    @Test
    public void testEmptyAdvanceAndCount() {
        ObjIteratorEx<String> empty = ObjIteratorEx.empty();
        empty.advance(5); // should not throw
        Assertions.assertEquals(0, empty.count());
        Assertions.assertFalse(empty.hasNext());
    }

    @Test
    public void testOfArrayWithIndices() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("d", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIteratorEx<String> iter = ObjIteratorEx.of(list);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIteratorEx<String> iter = ObjIteratorEx.of(list.iterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIteratorCloseResourceDelegatesToAutoCloseable() {
        final boolean[] closed = { false };

        class CloseableIterator implements Iterator<String>, AutoCloseable {
            private final Iterator<String> iter = Arrays.asList("a").iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public String next() {
                return iter.next();
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        }

        final ObjIteratorEx<String> iter = ObjIteratorEx.of(new CloseableIterator());
        iter.closeResource();

        Assertions.assertTrue(closed[0]);
    }

    @Test
    public void testOfIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIteratorEx<String> iter = ObjIteratorEx.of((Iterable<String>) list);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvance() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals("c", iter.next());

        iter.advance(1);
        Assertions.assertEquals("e", iter.next());

        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testCount() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        String[] result = iter.toArray(new String[0]);
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfCollectionCount() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIteratorEx<String> iter = ObjIteratorEx.of(list);
        // Collection-backed iterator should support count via iteration
        Assertions.assertEquals("a", iter.next());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithSubRangeAdvanceAndCount() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 0, 5);

        iter.advance(2);
        Assertions.assertEquals(3, iter.count()); // remaining after advance
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayToArraySmall() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 0, 3);

        String[] result = iter.toArray(new String[0]);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayToList() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        List<String> result = iter.toList();
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayNull() {
        String[] nullArray = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullArray);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfCollectionNull() {
        Collection<String> nullCollection = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullCollection);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIteratorNull() {
        Iterator<String> nullIterator = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIteratorAlreadyObjIteratorEx() {
        ObjIteratorEx<String> original = ObjIteratorEx.of("a", "b", "c");
        ObjIteratorEx<String> wrapped = ObjIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testOfIterableNull() {
        Iterable<String> nullIterable = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullIterable);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        iter.advance(0);
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testToArrayWithSufficientSize() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        String[] output = new String[5];
        String[] result = iter.toArray(output);
        Assertions.assertSame(output, result);
        Assertions.assertEquals("a", result[0]);
        Assertions.assertEquals("b", result[1]);
        Assertions.assertEquals("c", result[2]);
    }

    @Test
    public void testOfEmptyArray() {
        String[] emptyArray = {};
        ObjIteratorEx<String> iter = ObjIteratorEx.of(emptyArray);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertEquals(0, iter.count());
    }

    @Test
    public void testAdvanceNegative() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        // Advancing by negative or zero should have no effect
        iter.advance(-1);
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testOfArrayToArrayLarge() {
        String[] array = { "x", "y" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 0, 2);

        String[] output = new String[5];
        String[] result = iter.toArray(output);
        Assertions.assertSame(output, result);
        Assertions.assertEquals("x", result[0]);
        Assertions.assertEquals("y", result[1]);
        Assertions.assertNull(result[2]); // null sentinel
    }

    @Test
    public void testOfArray() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        String[] array = { "a", "b", "c" };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, 0, 4));
    }

    @Test
    public void testDefer() {
        List<String> list = Arrays.asList("a", "b", "c");
        Supplier<Iterator<String>> supplier = () -> list.iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDeferWithObjIteratorEx() {
        String[] array = { "a", "b", "c" };
        Supplier<Iterator<String>> supplier = () -> ObjIteratorEx.of(array);
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        iter.advance(1);
        Assertions.assertEquals("b", iter.next());

        Assertions.assertEquals(1, iter.count());

        iter.closeResource();
    }

    @Test
    public void testDeferCloseResourceDelegatesToIteratorEx() {
        final boolean[] closed = { false };

        final IteratorEx<String> source = new IteratorEx<>() {
            private final Iterator<String> iter = Arrays.asList("a").iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public String next() {
                return iter.next();
            }

            @Override
            public void closeResource() {
                closed[0] = true;
            }
        };

        final ObjIteratorEx<String> iter = ObjIteratorEx.defer(() -> source);
        Assertions.assertTrue(iter.hasNext());
        iter.closeResource();

        Assertions.assertTrue(closed[0]);
    }

    @Test
    public void testDeferAdvance_NonObjIteratorEx() {
        List<String> list = Arrays.asList("a", "b", "c");
        // Supplier returns a plain Iterator (not ObjIteratorEx), so defer falls back to super.advance
        Supplier<Iterator<String>> supplier = () -> list.iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        iter.advance(1);
        Assertions.assertEquals("b", iter.next());
    }

    @Test
    public void testDeferCount_NonObjIteratorEx() {
        List<String> list = Arrays.asList("a", "b", "c");
        // Supplier returns a plain Iterator (not ObjIteratorEx)
        Supplier<Iterator<String>> supplier = () -> list.iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDeferLazyInitialization() {
        boolean[] initialized = { false };
        Supplier<Iterator<String>> supplier = () -> {
            initialized[0] = true;
            return Arrays.asList("a", "b").iterator();
        };
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        Assertions.assertFalse(initialized[0]); // not yet initialized
        iter.hasNext(); // triggers init
        Assertions.assertTrue(initialized[0]);
    }

    @Test
    public void testDeferCachesInitializationFailure() {
        final int[] invocationCount = { 0 };
        final IllegalStateException failure = new IllegalStateException("initialization failed");
        final ObjIteratorEx<String> iter = ObjIteratorEx.defer(() -> {
            invocationCount[0]++;
            throw failure;
        });

        Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, iter::hasNext));
        Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, iter::next));
        Assertions.assertEquals(1, invocationCount[0]);
    }

    @Test
    public void testDeferCachesInitializationError() {
        final int[] invocationCount = { 0 };
        final AssertionError failure = new AssertionError("initialization failed");
        final ObjIteratorEx<String> iter = ObjIteratorEx.defer(() -> {
            invocationCount[0]++;
            throw failure;
        });

        // Errors use the same cached-failure path as runtime exceptions; the supplier must not run again.
        Assertions.assertSame(failure, Assertions.assertThrows(AssertionError.class, iter::hasNext));
        Assertions.assertSame(failure, Assertions.assertThrows(AssertionError.class, iter::next));
        Assertions.assertSame(failure, Assertions.assertThrows(AssertionError.class, iter::count));
        Assertions.assertEquals(1, invocationCount[0]);
        iter.closeResource();
    }

    @Test
    public void testDeferRejectsNullIteratorFromSupplier() {
        final int[] invocationCount = { 0 };
        final ObjIteratorEx<String> iter = ObjIteratorEx.defer(() -> {
            invocationCount[0]++;
            return null;
        });

        final IllegalStateException first = Assertions.assertThrows(IllegalStateException.class, iter::hasNext);
        Assertions.assertSame(first, Assertions.assertThrows(IllegalStateException.class, iter::count));
        Assertions.assertEquals(1, invocationCount[0]);
    }

    @Test
    public void testDeferAdvanceZero() {
        String[] array = { "a", "b", "c" };
        Supplier<Iterator<String>> supplier = () -> ObjIteratorEx.of(array);
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        iter.advance(0); // should not advance
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testDeferAdvanceNegative() {
        String[] array = { "a", "b", "c" };
        Supplier<Iterator<String>> supplier = () -> ObjIteratorEx.of(array);
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        iter.advance(-5); // should not advance
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testDeferNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIteratorEx.defer(null).hasNext());
    }

    @Test
    public void testDeferClose_Resource_NonObjIteratorEx() {
        List<String> list = Arrays.asList("a", "b", "c");
        // Supplier returns a plain Iterator (not ObjIteratorEx), close should not throw
        Supplier<Iterator<String>> supplier = () -> list.iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        // Force initialization by calling hasNext
        Assertions.assertTrue(iter.hasNext());
        iter.closeResource();
        Assertions.assertNotNull(iter);
    }

    @Test
    public void testDeferCloseResourceBeforeInit() {
        final boolean[] initialized = { false };

        assertDoesNotThrow(() -> {
            Supplier<Iterator<String>> supplier = () -> {
                initialized[0] = true;
                return Arrays.asList("a").iterator();
            };
            ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);
            iter.closeResource();
        });

        Assertions.assertFalse(initialized[0]);
    }

    @Test
    public void testCloseResource() {
        ObjIteratorEx<String> iter = ObjIteratorEx.of("a", "b", "c");
        iter.closeResource();
        assertNotNull(iter);
    }

    // ---- bug review 2026-09-30 G123 begin ----
    // G123: ObjIteratorEx.defer has the same guards as ObjIterator.defer - a supplier returning the deferred iterator
    // itself, or using it while it is being initialized, fails with IllegalStateException instead of StackOverflowError.
    @Test
    public void testDefer_selfReferenceFailsFast() {
        @SuppressWarnings("unchecked")
        final ObjIteratorEx<Object>[] self = new ObjIteratorEx[1];
        self[0] = ObjIteratorEx.defer(() -> self[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> self[0].hasNext());

        @SuppressWarnings("unchecked")
        final ObjIteratorEx<Object>[] recursive = new ObjIteratorEx[1];
        recursive[0] = ObjIteratorEx.defer(() -> {
            recursive[0].hasNext();
            return java.util.List.of("a").iterator();
        });
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> recursive[0].hasNext());

        // unchanged
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b"),
                com.landawn.abacus.util.stream.Stream.of(ObjIteratorEx.defer(() -> java.util.List.of("a", "b").iterator())).toList());
    }
    // ---- bug review 2026-09-30 G123 end ----

    // ---- bug review 2026-09-30 verify2 G129 begin ----
    // Recursive-access failure is kept (as in ObjIterator.defer) even if the supplier catches it and throws something else.
    @Test
    public void testDefer_recursiveAccessFailureWinsOverSupplierRethrow() {
        @SuppressWarnings("unchecked")
        final ObjIteratorEx<Object>[] iter = new ObjIteratorEx[1];
        iter[0] = ObjIteratorEx.defer(() -> {
            try {
                iter[0].hasNext();
            } catch (final IllegalStateException e) {
                throw new IllegalArgumentException("wrapped", e);
            }
            return List.<Object> of("a").iterator();
        });

        final IllegalStateException first = Assertions.assertThrows(IllegalStateException.class, () -> iter[0].hasNext());
        Assertions.assertEquals("Recursive initialization of deferred iterator", first.getMessage());
        Assertions.assertSame(first, Assertions.assertThrows(IllegalStateException.class, () -> iter[0].next()));
        Assertions.assertSame(first, Assertions.assertThrows(IllegalStateException.class, () -> iter[0].count()));
    }

    // Recursive-access failure swallowed by the supplier still fails the traversal; the iterator is not used.
    @Test
    public void testDefer_recursiveAccessFailureSwallowedBySupplierStillFails() {
        @SuppressWarnings("unchecked")
        final ObjIteratorEx<Object>[] iter = new ObjIteratorEx[1];
        iter[0] = ObjIteratorEx.defer(() -> {
            try {
                iter[0].hasNext();
            } catch (final IllegalStateException e) {
                // ignored on purpose
            }
            return List.<Object> of("a").iterator();
        });

        Assertions.assertThrows(IllegalStateException.class, () -> iter[0].hasNext());
        Assertions.assertThrows(IllegalStateException.class, () -> iter[0].next());
    }

    @Test
    public void testDeferRejectedIteratorExIsClosedAndReleased() throws Exception {
        final AtomicInteger closeCalls = new AtomicInteger();
        final AtomicInteger supplierCalls = new AtomicInteger();
        final AtomicReference<IllegalStateException> recursiveFailure = new AtomicReference<>();
        final IllegalArgumentException closeFailure = new IllegalArgumentException("close failed");
        final Iterator<Object> source = rejectedIterator(true, () -> {
            closeCalls.incrementAndGet();
            throw closeFailure;
        });
        final ObjIteratorEx<Object> deferred = deferredWithRejectedSource(source, recursiveFailure, supplierCalls);

        final IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, deferred::hasNext);
        Assertions.assertSame(recursiveFailure.get(), failure);
        Assertions.assertArrayEquals(new Throwable[] { closeFailure }, failure.getSuppressed());
        assertNoRetainedDelegate(deferred);
        Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, deferred::next));
        deferred.closeResource();
        deferred.closeResource();
        Assertions.assertEquals(1, closeCalls.get());
        Assertions.assertEquals(1, supplierCalls.get());
    }

    @Test
    public void testDeferRejectedAutoCloseableIteratorIsClosedAndReleased() throws Exception {
        final AtomicInteger closeCalls = new AtomicInteger();
        final AtomicInteger supplierCalls = new AtomicInteger();
        final AtomicReference<IllegalStateException> recursiveFailure = new AtomicReference<>();
        final Iterator<Object> source = rejectedIterator(false, closeCalls::incrementAndGet);
        final ObjIteratorEx<Object> deferred = deferredWithRejectedSource(source, recursiveFailure, supplierCalls);

        final IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, deferred::count);
        Assertions.assertSame(recursiveFailure.get(), failure);
        Assertions.assertEquals(0, failure.getSuppressed().length);
        assertNoRetainedDelegate(deferred);
        Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, () -> deferred.advance(1)));
        deferred.closeResource();
        deferred.closeResource();
        Assertions.assertEquals(1, closeCalls.get());
        Assertions.assertEquals(1, supplierCalls.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testDeferRejectedCloseObservesPublishedFailureOutsideInitializationLock(final boolean extended) throws Exception {
        final AtomicReference<ObjIteratorEx<Object>> deferred = new AtomicReference<>();
        final AtomicReference<IllegalStateException> recursiveFailure = new AtomicReference<>();
        final AtomicInteger supplierCalls = new AtomicInteger();
        final AtomicInteger closeCalls = new AtomicInteger();
        final AtomicBoolean heldLock = new AtomicBoolean();
        final AtomicBoolean callbackCompleted = new AtomicBoolean();
        final Iterator<Object> source = rejectedIterator(extended, () -> {
            closeCalls.incrementAndGet();
            heldLock.set(Thread.holdsLock(deferred.get()));

            // Fail deterministically on the old implementation without blocking a test thread on its monitor.
            if (heldLock.get()) {
                return;
            }

            assertDoesNotThrow(() -> assertNoRetainedDelegate(deferred.get()));
            deferred.get().closeResource();
            Assertions.assertSame(recursiveFailure.get(), Assertions.assertThrows(IllegalStateException.class, deferred.get()::hasNext));
            Assertions.assertSame(recursiveFailure.get(), Assertions.assertThrows(IllegalStateException.class, deferred.get()::count));

            final FutureTask<IllegalStateException> observation = new FutureTask<>(
                    () -> Assertions.assertThrows(IllegalStateException.class, deferred.get()::hasNext));
            final Thread observer = new Thread(observation, "defer-rejected-close-observer");
            observer.setDaemon(true);
            observer.start();

            try {
                Assertions.assertSame(recursiveFailure.get(), observation.get(5, TimeUnit.SECONDS));
                callbackCompleted.set(true);
            } catch (final Exception e) {
                throw new AssertionError("A cleanup callback must be able to wait for another traversal of the failed iterator", e);
            } finally {
                observer.interrupt();
            }
        });
        deferred.set(deferredWithRejectedSource(source, recursiveFailure, supplierCalls));

        final IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, deferred.get()::hasNext);
        Assertions.assertFalse(heldLock.get(), "Rejected cleanup must not run under the deferred iterator's initialization monitor");
        Assertions.assertArrayEquals(new Throwable[0], failure.getSuppressed());
        Assertions.assertTrue(callbackCompleted.get());
        Assertions.assertSame(recursiveFailure.get(), failure);
        assertNoRetainedDelegate(deferred.get());
        deferred.get().closeResource();
        Assertions.assertEquals(1, closeCalls.get());
        Assertions.assertEquals(1, supplierCalls.get());
    }

    @ParameterizedTest
    @MethodSource("fatalDeferCloseFailures")
    public void testDeferFatalRejectedClosePropagatesWithInitializationContext(final boolean extended, final Error closeFailure) throws Exception {
        final AtomicInteger closeCalls = new AtomicInteger();
        final AtomicInteger supplierCalls = new AtomicInteger();
        final AtomicReference<IllegalStateException> recursiveFailure = new AtomicReference<>();
        final Iterator<Object> source = rejectedIterator(extended, () -> {
            closeCalls.incrementAndGet();
            throw closeFailure;
        });
        final ObjIteratorEx<Object> deferred = deferredWithRejectedSource(source, recursiveFailure, supplierCalls);

        Assertions.assertSame(closeFailure, Assertions.assertThrows(Error.class, deferred::hasNext));
        Assertions.assertArrayEquals(new Throwable[] { recursiveFailure.get() }, closeFailure.getSuppressed());
        Assertions.assertEquals(0, recursiveFailure.get().getSuppressed().length);
        assertNoRetainedDelegate(deferred);
        Assertions.assertSame(recursiveFailure.get(), Assertions.assertThrows(IllegalStateException.class, deferred::hasNext));
        Assertions.assertSame(recursiveFailure.get(), Assertions.assertThrows(IllegalStateException.class, deferred::next));
        Assertions.assertSame(recursiveFailure.get(), Assertions.assertThrows(IllegalStateException.class, deferred::count));
        Assertions.assertSame(recursiveFailure.get(), Assertions.assertThrows(IllegalStateException.class, () -> deferred.advance(1)));
        deferred.closeResource();
        deferred.closeResource();
        Assertions.assertEquals(1, closeCalls.get());
        Assertions.assertEquals(1, supplierCalls.get());
    }

    @SuppressWarnings("removal")
    private static java.util.stream.Stream<Arguments> fatalDeferCloseFailures() {
        // Construct sentinels directly; never exhaust heap/stack space or stop a real thread.
        return java.util.stream.Stream.of(true, false)
                .flatMap(extended -> java.util.stream.Stream.of(Arguments.of(extended, new ThreadDeath()),
                        Arguments.of(extended, new OutOfMemoryError("close failed")), Arguments.of(extended, new StackOverflowError("close failed"))));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testDeferRejectedCloseSuppressesOrdinaryError(final boolean extended) throws Exception {
        final AtomicInteger closeCalls = new AtomicInteger();
        final AtomicInteger supplierCalls = new AtomicInteger();
        final AtomicReference<IllegalStateException> recursiveFailure = new AtomicReference<>();
        final AssertionError closeFailure = new AssertionError("close failed");
        final ObjIteratorEx<Object> deferred = deferredWithRejectedSource(rejectedIterator(extended, () -> {
            closeCalls.incrementAndGet();
            throw closeFailure;
        }), recursiveFailure, supplierCalls);

        final IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, deferred::hasNext);
        Assertions.assertSame(recursiveFailure.get(), failure);
        Assertions.assertArrayEquals(new Throwable[] { closeFailure }, failure.getSuppressed());
        assertNoRetainedDelegate(deferred);
        deferred.closeResource();
        Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, deferred::hasNext));
        Assertions.assertEquals(1, closeCalls.get());
        Assertions.assertEquals(1, supplierCalls.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testDeferRejectedCloseDoesNotSuppressFailureOnItself(final boolean extended) throws Exception {
        final AtomicInteger closeCalls = new AtomicInteger();
        final AtomicInteger supplierCalls = new AtomicInteger();
        final AtomicReference<IllegalStateException> recursiveFailure = new AtomicReference<>();
        final ObjIteratorEx<Object> deferred = deferredWithRejectedSource(rejectedIterator(extended, () -> {
            closeCalls.incrementAndGet();
            throw recursiveFailure.get();
        }), recursiveFailure, supplierCalls);

        final IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, deferred::hasNext);
        Assertions.assertSame(recursiveFailure.get(), failure);
        Assertions.assertEquals(0, failure.getSuppressed().length);
        assertNoRetainedDelegate(deferred);
        deferred.closeResource();
        Assertions.assertEquals(1, closeCalls.get());
        Assertions.assertEquals(1, supplierCalls.get());
    }

    private static ObjIteratorEx<Object> deferredWithRejectedSource(final Iterator<Object> source,
            final AtomicReference<IllegalStateException> recursiveFailure, final AtomicInteger supplierCalls) {
        final AtomicReference<ObjIteratorEx<Object>> deferred = new AtomicReference<>();
        deferred.set(ObjIteratorEx.defer(() -> {
            supplierCalls.incrementAndGet();
            recursiveFailure.set(Assertions.assertThrows(IllegalStateException.class, deferred.get()::hasNext));
            return source;
        }));
        return deferred.get();
    }

    private static Iterator<Object> rejectedIterator(final boolean extended, final Runnable onClose) {
        if (extended) {
            return new ObjIteratorEx<>() {
                @Override
                public boolean hasNext() {
                    throw new AssertionError("A rejected iterator must never be traversed");
                }

                @Override
                public Object next() {
                    throw new AssertionError("A rejected iterator must never be traversed");
                }

                @Override
                public void closeResource() {
                    onClose.run();
                }
            };
        }

        class CloseableIterator implements Iterator<Object>, AutoCloseable {
            @Override
            public boolean hasNext() {
                throw new AssertionError("A rejected iterator must never be traversed");
            }

            @Override
            public Object next() {
                throw new AssertionError("A rejected iterator must never be traversed");
            }

            @Override
            public void close() {
                onClose.run();
            }
        }

        return new CloseableIterator();
    }

    private static void assertNoRetainedDelegate(final ObjIteratorEx<?> deferred) throws Exception {
        // Inspect ownership directly; this regression does not need GC timing or large allocations.
        for (final String name : List.of("iter", "iterEx")) {
            final Field field = deferred.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Assertions.assertNull(field.get(deferred), "Failed initialization retained " + name);
        }
    }

    // A self-returning deferred iterator fails on every traversal path; closeResource stays a safe no-op.
    @Test
    public void testDefer_selfReferenceFailsOnEveryTraversalPath() {
        @SuppressWarnings("unchecked")
        final ObjIteratorEx<Object>[] self = new ObjIteratorEx[1];
        self[0] = ObjIteratorEx.defer(() -> self[0]);

        final IllegalStateException first = Assertions.assertThrows(IllegalStateException.class, () -> self[0].next());
        Assertions.assertEquals("Iterator supplier returned the deferred iterator itself", first.getMessage());
        Assertions.assertSame(first, Assertions.assertThrows(IllegalStateException.class, () -> self[0].count()));
        Assertions.assertSame(first, Assertions.assertThrows(IllegalStateException.class, () -> self[0].advance(1)));
        assertDoesNotThrow(() -> self[0].closeResource());
    }

    // Concurrent first traversal: the supplier runs once and every element is delivered exactly once.
    @Test
    public void testDefer_concurrentFirstTraversalInvokesSupplierOnce() throws Exception {
        final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        final List<Integer> source = new java.util.ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            source.add(i);
        }
        final ObjIteratorEx<Integer> iter = ObjIteratorEx.defer(() -> {
            calls.incrementAndGet();
            return source.iterator();
        });

        final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicInteger received = new java.util.concurrent.atomic.AtomicInteger();
            final List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
            for (int t = 0; t < 8; t++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    while (true) {
                        synchronized (iter) {
                            if (!iter.hasNext()) {
                                break;
                            }
                            iter.next();
                        }
                        received.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (final java.util.concurrent.Future<?> future : futures) {
                future.get(60, java.util.concurrent.TimeUnit.SECONDS);
            }
            Assertions.assertEquals(1, calls.get());
            Assertions.assertEquals(2000, received.get());
        } finally {
            executor.shutdownNow();
        }
    }
    // ---- bug review 2026-09-30 verify2 G129 end ----

}
