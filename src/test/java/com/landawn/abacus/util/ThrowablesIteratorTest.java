package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Throwables.Iterator;
import com.landawn.abacus.util.u.Nullable;

public class ThrowablesIteratorTest extends ThrowablesTestSupport {

    @Test
    public void testEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertTrue(iter.toList().isEmpty());
        assertEquals(0, iter.count());
        assertEquals(0, iter.toArray().length);
    }

    @Test
    public void testJust() throws Exception {
        Iterator<String, Exception> iter = Iterator.just("value");
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals("value", iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertFalse(Iterator.just("value") instanceof Immutable);
        assertEquals(1, Iterator.<String, Exception> just("value").count());

        Iterator<String, Exception> nil = Iterator.just(null);
        assertTrue(nil.hasNext());
        assertNull(nil.next());
    }

    @Test
    public void testOf() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertFalse(Iterator.of().hasNext());
        assertFalse(Iterator.of((String[]) null).hasNext());
        assertEquals("one", Iterator.of("one").next());
        assertEquals(3, Iterator.of("one", "two", "three").count());

        Iterator<String, Exception> withNulls = Iterator.of("One", null, "Three");
        assertEquals("One", withNulls.next());
        assertNull(withNulls.next());
        assertEquals("Three", withNulls.next());

        assertEquals(List.of("one", "two", "three"), Iterator.<String, Exception> of(Arrays.asList("one", "two", "three")).toList());
        assertFalse(Iterator.<String, Exception> of((Iterable<String>) null).hasNext());
        assertFalse(Iterator.<String, Exception> of(Collections.emptyList()).hasNext());
        assertEquals(List.of("one", "two", "three"), Iterator.of(Arrays.asList("one", "two", "three").iterator()).toList());
        assertFalse(Iterator.of((java.util.Iterator<String>) null).hasNext());
        assertFalse(Iterator.of(Collections.emptyIterator()).hasNext());
    }

    @Test
    public void testOf_Range() throws Exception {
        String[] arr = { "zero", "one", "two", "three", "four" };
        Iterator<String, Exception> iter = Iterator.of(arr, 1, 4);
        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertFalse(iter.hasNext());
        assertFalse(Iterator.of(arr, 1, 1).hasNext());
        assertEquals(3, Iterator.of(arr, 1, 4).count());
        assertThrows(IndexOutOfBoundsException.class, () -> Iterator.of(arr, 1, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterator.of(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterator.of(arr, 2, 1));

        Iterator<String, Exception> advanced = Iterator.of(arr, 0, 5);
        advanced.advance(2);
        assertEquals("two", advanced.next());
        advanced.advance(10);
        assertFalse(advanced.hasNext());
        Iterator<String, Exception> zero = Iterator.of(arr, 0, 3);
        zero.advance(0);
        assertEquals("zero", zero.next());
        zero.advance(-5);
        assertEquals("one", zero.next());
    }

    @Test
    public void testDefer() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Iterator<String, Exception> iter = Iterator.defer(() -> {
            calls.incrementAndGet();
            return Iterator.of("one", "two");
        });
        assertEquals(0, calls.get());
        assertTrue(iter.hasNext());
        assertEquals(1, calls.get());
        assertEquals("one", iter.next());
        assertEquals(List.of("two"), iter.toList());
        assertEquals(1, calls.get());

        AtomicBoolean onNext = new AtomicBoolean();
        Iterator<String, Exception> nextInit = Iterator.defer(() -> {
            onNext.set(true);
            return Iterator.of("one");
        });
        assertEquals("one", nextInit.next());
        assertTrue(onNext.get());

        AtomicBoolean onAdvance = new AtomicBoolean();
        Iterator<String, Exception> advanceInit = Iterator.defer(() -> {
            onAdvance.set(true);
            return Iterator.of("one", "two", "three");
        });
        advanceInit.advance(1);
        assertTrue(onAdvance.get());
        assertEquals("two", advanceInit.next());

        AtomicBoolean onCount = new AtomicBoolean();
        Iterator<String, Exception> countInit = Iterator.defer(() -> {
            onCount.set(true);
            return Iterator.of("one", "two");
        });
        assertEquals(2, countInit.count());
        assertTrue(onCount.get());

        AtomicBoolean skipAdvance = new AtomicBoolean();
        Iterator<String, Exception> advanceZero = Iterator.defer(() -> {
            skipAdvance.set(true);
            return Iterator.of("one");
        });
        advanceZero.advance(0);
        assertFalse(skipAdvance.get());

        Iterator<String, Exception> deferred = Iterator.defer(() -> Iterator.of("A", "B", "C", "D"));
        deferred.advance(1);
        assertEquals("B", deferred.next());
        assertEquals(2, deferred.count());

        AtomicInteger attempts = new AtomicInteger();
        Iterator<String, Exception> retry = Iterator.defer(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first attempt");
            }
            return Iterator.just("value");
        });
        assertEquals("first attempt", assertThrows(IllegalStateException.class, retry::hasNext).getMessage());
        assertTrue(retry.hasNext());
        assertEquals("value", retry.next());
        assertEquals(2, attempts.get());

        IllegalStateException nullResult = assertThrows(IllegalStateException.class, () -> Iterator.defer(() -> null).hasNext());
        assertEquals("Iterator supplier returned null", nullResult.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Iterator.defer(null));

        AtomicBoolean initialized = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        Iterator<String, Exception> closedBeforeInit = Iterator.defer(() -> {
            initialized.set(true);
            return new Iterator<String, Exception>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public String next() {
                    throw new NoSuchElementException();
                }

                @Override
                protected void closeResourceInternal() {
                    closed.set(true);
                }
            };
        });
        closedBeforeInit.closeResource();
        assertFalse(initialized.get());
        assertFalse(closed.get());
        // After close a deferred iterator reports itself exhausted, exactly as concat/filter/map do,
        // and it still must never invoke the supplier.
        assertFalse(closedBeforeInit.hasNext());
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, closedBeforeInit::next).getMessage());
        closedBeforeInit.advance(5);
        assertEquals(0, closedBeforeInit.count());
        assertFalse(initialized.get());

        AtomicBoolean closedAfterInit = new AtomicBoolean();
        Iterator<String, Exception> afterInit = Iterator.defer(() -> new Iterator<String, Exception>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                throw new NoSuchElementException();
            }

            @Override
            protected void closeResourceInternal() {
                closedAfterInit.set(true);
            }
        });
        afterInit.hasNext();
        afterInit.closeResource();
        assertTrue(closedAfterInit.get());
        assertFalse(afterInit.hasNext());
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, afterInit::next).getMessage());
        afterInit.advance(3);
        assertEquals(0, afterInit.count());
    }

    @Test
    public void testPostCloseContractIsUniformAcrossCombinators() throws Exception {
        // concat/defer/filter/map must agree on what a closed iterator reports; defer used to throw
        // IllegalStateException where the other three reported exhaustion.
        final Iterator<String, Exception> concatenated = Iterator.<String, Exception> concat(Iterator.<String, Exception> of("a", "b"));
        final Iterator<String, Exception> deferred = Iterator.defer(() -> Iterator.<String, Exception> of("a", "b"));
        final Iterator<String, Exception> filtered = Iterator.<String, Exception> of("a", "b").filter(s -> true);
        final Iterator<String, Exception> mapped = Iterator.<String, Exception> of("a", "b").map(String::toUpperCase);

        for (final Iterator<String, Exception> iter : Arrays.asList(concatenated, deferred, filtered, mapped)) {
            assertTrue(iter.hasNext());
            iter.closeResource();

            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
            iter.advance(2);
            assertEquals(0, iter.count());
            assertTrue(iter.toList().isEmpty());
        }
    }

    @Test
    public void testConcat() throws Exception {
        @SuppressWarnings("unchecked")
        Iterator<String, Exception> empty = Iterator.concat();
        assertFalse(empty.hasNext());
        assertEquals(List.of("one", "two"), Iterator.concat(Iterator.of("one", "two")).toList());
        assertEquals(List.of("one", "two", "three", "four", "five"),
                Iterator.concat(Iterator.of("one", "two"), Iterator.of("three"), Iterator.of("four", "five")).toList());
        assertEquals(List.of("one", "two"),
                Iterator.concat(Iterator.empty(), Iterator.of("one"), Iterator.empty(), Iterator.of("two"), Iterator.empty()).toList());
        assertFalse(Iterator.concat((List<Iterator<String, Exception>>) null).hasNext());
        assertFalse(Iterator.concat(Collections.emptyList()).hasNext());
        assertEquals(List.of("one", "two", "three", "four", "five"),
                Iterator.concat(Arrays.asList(Iterator.of("one", "two"), Iterator.of("three"), Iterator.of("four", "five"))).toList());
        assertEquals(List.of("one"), Iterator.concat(null, Iterator.of("one"), null).toList());
        assertEquals(List.of("two"), Iterator.concat(Arrays.asList(null, Iterator.of("two"), null)).toList());

        Iterator<String, Exception> multiHasNext = Iterator.concat(Arrays.asList(Iterator.of("one"), Iterator.of("two")));
        assertTrue(multiHasNext.hasNext());
        assertTrue(multiHasNext.hasNext());
        assertEquals("one", multiHasNext.next());

        AtomicInteger closeCount = new AtomicInteger();
        Iterator<String, Exception> concatenated = Iterator.concat(closeTrackingIterator(closeCount), closeTrackingIterator(closeCount));
        assertTrue(concatenated.hasNext());
        concatenated.closeResource();
        assertEquals(2, closeCount.get());
        assertFalse(concatenated.hasNext());
        assertThrows(NoSuchElementException.class, concatenated::next);

        AtomicInteger failingCloses = new AtomicInteger();
        RuntimeException primary = new RuntimeException("primary");
        RuntimeException secondary = new RuntimeException("secondary");
        Iterator<String, Exception> failing = Iterator.concat(closeFailingIterator(failingCloses, primary), closeFailingIterator(failingCloses, primary),
                closeFailingIterator(failingCloses, secondary), closeTrackingIterator(failingCloses));
        RuntimeException thrown = assertThrows(RuntimeException.class, failing::closeResource);
        assertSame(primary, thrown);
        assertEquals(4, failingCloses.get());
        assertArrayEquals(new Throwable[] { secondary }, thrown.getSuppressed());
    }

    @Test
    public void testOfLines() throws Exception {
        assertFalse(Iterator.ofLines(null).hasNext());
        assertFalse(Iterator.ofLines(new StringReader("")).hasNext());
        assertThrows(NoSuchElementException.class, () -> Iterator.ofLines(new StringReader("")).next());

        Iterator<String, IOException> single = Iterator.ofLines(new StringReader("line1"));
        assertTrue(single.hasNext());
        assertTrue(single.hasNext());
        assertEquals("line1", single.next());
        assertFalse(single.hasNext());
        assertEquals("line1", Iterator.ofLines(new StringReader("line1")).next());

        Iterator<String, IOException> lines = Iterator.ofLines(new StringReader("line1\nline2\nline3"));
        assertEquals("line1", lines.next());
        assertEquals("line2", lines.next());
        assertEquals("line3", lines.next());
        assertFalse(lines.hasNext());

        Iterator<String, IOException> emptyLines = Iterator.ofLines(new StringReader("\n\n"));
        assertEquals("", emptyLines.next());
        assertEquals("", emptyLines.next());
        assertFalse(emptyLines.hasNext());

        Iterator<String, IOException> afterEnd = Iterator.ofLines(new StringReader("Single line"));
        afterEnd.next();
        assertThrows(NoSuchElementException.class, afterEnd::next);

        Iterator<String, IOException> closable = Iterator.ofLines(new StringReader("line1\nline2\nline3"));
        closable.next();
        closable.closeResource();
        closable.closeResource();
        assertNotNull(closable);

        IOException closeFailure = new IOException("close");
        Reader reader = new Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) {
                return -1;
            }

            @Override
            public void close() throws IOException {
                throw closeFailure;
            }
        };
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> Iterator.ofLines(reader).closeResource());
        assertSame(closeFailure, thrown.getCause());
    }

    @Test
    public void testAdvanceAndCount() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three", "four");
        iter.advance(2);
        assertEquals("three", iter.next());
        Iterator<String, Exception> zero = Iterator.of("one", "two");
        zero.advance(0);
        assertEquals("one", zero.next());
        Iterator<String, Exception> negative = Iterator.of("one", "two");
        negative.advance(-1);
        assertEquals("one", negative.next());
        Iterator<String, Exception> pastEnd = Iterator.of("one", "two");
        pastEnd.advance(10);
        assertFalse(pastEnd.hasNext());
        Iterator<String, Exception> exact = Iterator.of("one", "two", "three");
        exact.advance(3);
        assertFalse(exact.hasNext());

        assertEquals(0, Iterator.<String, Exception> empty().count());
        Iterator<String, Exception> partial = Iterator.of("one", "two", "three", "four");
        partial.next();
        partial.next();
        assertEquals(2, partial.count());
        Iterator<String, Exception> consumed = Iterator.of("one", "two");
        consumed.count();
        assertEquals(0, consumed.count());
        Iterator<String, Exception> afterAdvance = Iterator.of("A", "B", "C", "D", "E");
        afterAdvance.advance(2);
        assertEquals(3, afterAdvance.count());
    }

    @Test
    public void testClose() throws Exception {
        AtomicInteger closeCount = new AtomicInteger();
        Iterator<String, Exception> iter = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                throw new NoSuchElementException();
            }

            @Override
            protected void closeResourceInternal() {
                closeCount.incrementAndGet();
            }
        };
        iter.closeResource();
        iter.closeResource();
        iter.closeResource();
        assertEquals(1, closeCount.get());

        AtomicBoolean closed = new AtomicBoolean();
        Iterator<Integer, Exception> closable = new Iterator<>() {
            private final Iterator<Integer, Exception> internal = Iterator.of(1, 2, 3);

            @Override
            public boolean hasNext() throws Exception {
                return internal.hasNext();
            }

            @Override
            public Integer next() throws Exception {
                return internal.next();
            }

            @Override
            protected void closeResourceInternal() {
                closed.set(true);
            }
        };
        try {
            assertEquals(3, closable.count());
            closable.closeResource();
        } catch (Throwable e) {
            fail("Should not throw exception on close");
        }
        assertTrue(closed.get());
    }

    @Test
    public void testFilter() throws Exception {
        assertEquals(List.of(2, 4, 6, 8), Iterator.of(2, 4, 6, 8).filter(n -> n % 2 == 0).toList());
        assertEquals(List.of(2, 4, 6), Iterator.of(1, 2, 3, 4, 5, 6).filter(n -> n % 2 == 0).toList());
        assertFalse(Iterator.of(1, 3, 5, 7).filter(n -> n % 2 == 0).hasNext());
        assertFalse(Iterator.<Integer, Exception> empty().filter(n -> true).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterator.empty().filter(null));

        Iterator<Integer, Exception> filtered = Iterator.of(1, 2, 3).filter(n -> n == 2);
        assertTrue(filtered.hasNext());
        assertTrue(filtered.hasNext());
        assertEquals(2, filtered.next());

        Object sentinel = CommonUtil.NULL_SENTINEL;
        Iterator<Object, Exception> sentinelIter = Iterator.of(sentinel).filter(value -> true);
        assertTrue(sentinelIter.hasNext());
        assertSame(sentinel, sentinelIter.next());
        assertFalse(sentinelIter.hasNext());

        AtomicInteger closeCount = new AtomicInteger();
        Iterator<String, Exception> closed = closeTrackingIterator(closeCount).filter(value -> true);
        assertTrue(closed.hasNext());
        closed.closeResource();
        assertEquals(1, closeCount.get());
        assertFalse(closed.hasNext());
        assertThrows(NoSuchElementException.class, closed::next);
    }

    @Test
    public void testMap() throws Exception {
        assertEquals(List.of("num1", "num2", "num3"), Iterator.of(1, 2, 3).map(n -> "num" + n).toList());
        assertFalse(Iterator.<Integer, Exception> empty().map(n -> "num" + n).hasNext());
        Iterator<String, Exception> toNull = Iterator.of(1, 2).map(n -> null);
        assertNull(toNull.next());
        assertNull(toNull.next());
        assertFalse(toNull.hasNext());
        assertEquals(List.of(1, 2, 3), Iterator.of("1", "2", "3").map(Integer::parseInt).toList());
        assertThrows(IllegalArgumentException.class, () -> Iterator.empty().map(null));

        AtomicInteger closeCount = new AtomicInteger();
        Iterator<Integer, Exception> mapped = closeTrackingIterator(closeCount).map(String::length);
        mapped.closeResource();
        assertEquals(1, closeCount.get());
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, mapped::next);
    }

    @Test
    public void testFirstAndLast() throws Exception {
        Nullable<String> first = Iterator.of("one", "two", "three").first();
        assertTrue(first.isPresent());
        assertEquals("one", first.get());
        assertFalse(Iterator.<String, Exception> empty().first().isPresent());
        Nullable<String> nullFirst = Iterator.<String, Exception> just(null).first();
        assertTrue(nullFirst.isPresent());
        assertNull(nullFirst.get());

        assertEquals("one", Iterator.of("one", "two", "three").firstNonNull().get());
        assertEquals("three", Iterator.of(null, null, "three", "four").firstNonNull().get());
        assertFalse(Iterator.of(null, null, null).firstNonNull().isPresent());
        assertFalse(Iterator.<String, Exception> empty().firstNonNull().isPresent());

        Nullable<String> last = Iterator.of("one", "two", "three").last();
        assertTrue(last.isPresent());
        assertEquals("three", last.get());
        assertFalse(Iterator.<String, Exception> empty().last().isPresent());
        assertEquals("one", Iterator.<String, Exception> just("one").last().get());
        Nullable<String> nullLast = Iterator.of("one", null).last();
        assertTrue(nullLast.isPresent());
        assertNull(nullLast.get());
    }

    @Test
    public void testToArrayAndToList() throws Exception {
        assertArrayEquals(new Object[] { "one", "two", "three" }, Iterator.of("one", "two", "three").toArray());
        assertEquals(0, Iterator.<String, Exception> empty().toArray().length);
        assertArrayEquals(new String[] { "one", "two", "three" }, Iterator.of("one", "two", "three").toArray(new String[0]));
        assertEquals(0, Iterator.<String, Exception> empty().toArray(new String[0]).length);
        String[] larger = Iterator.of("one", "two").toArray(new String[5]);
        assertEquals(5, larger.length);
        assertEquals("one", larger[0]);
        assertEquals("two", larger[1]);
        assertNull(larger[2]);

        Iterator<String, Exception> validates = Iterator.of("one", "two");
        assertThrows(IllegalArgumentException.class, () -> validates.toArray(null));
        assertEquals("one", validates.next());

        assertEquals(List.of("one", "two", "three"), Iterator.of("one", "two", "three").toList());
        List<String> withNulls = Iterator.of("one", null, "three").toList();
        assertEquals(3, withNulls.size());
        assertNull(withNulls.get(1));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testForeachRemaining() throws Exception {
        List<String> boxed = new ArrayList<>();
        Iterator.of("one", "two", "three").forEachRemaining(boxed::add);
        assertEquals(List.of("one", "two", "three"), boxed);

        List<String> values = new ArrayList<>();
        Iterator.of("one", "two", "three").foreachRemaining(values::add);
        assertEquals(List.of("one", "two", "three"), values);

        Iterator<String, Exception> partial = Iterator.of("one", "two", "three");
        partial.next();
        List<String> remaining = new ArrayList<>();
        partial.forEachRemaining(remaining::add);
        assertEquals(List.of("two", "three"), remaining);

        List<String> emptyBoxed = new ArrayList<>();
        Iterator.<String, Exception> empty().forEachRemaining(emptyBoxed::add);
        Iterator.<String, Exception> empty().foreachRemaining(emptyBoxed::add);
        assertTrue(emptyBoxed.isEmpty());

        assertThrows(IOException.class, () -> Iterator.of("one", "two").foreachRemaining(s -> {
            throw new IOException("Test");
        }));
        assertThrows(IllegalArgumentException.class, () -> Iterator.empty().forEachRemaining(null));
        assertThrows(IllegalArgumentException.class, () -> Iterator.empty().foreachRemaining(null));
    }

    @Test
    public void testForeachIndexed() throws Exception {
        List<Pair<Integer, String>> collected = new ArrayList<>();
        Iterator.of("one", "two", "three").foreachIndexed((idx, val) -> collected.add(Pair.of(idx, val)));
        assertEquals(3, collected.size());
        assertEquals(0, collected.get(0).left());
        assertEquals("one", collected.get(0).right());
        assertEquals(2, collected.get(2).left());
        assertEquals("three", collected.get(2).right());

        Iterator<String, Exception> partial = Iterator.of("one", "two", "three");
        partial.next();
        List<Pair<Integer, String>> remaining = new ArrayList<>();
        partial.foreachIndexed((idx, val) -> remaining.add(Pair.of(idx, val)));
        assertEquals(0, remaining.get(0).left());
        assertEquals("two", remaining.get(0).right());
        assertEquals(1, remaining.get(1).left());
        assertEquals("three", remaining.get(1).right());

        List<Pair<Integer, String>> empty = new ArrayList<>();
        Iterator.<String, Exception> empty().foreachIndexed((idx, val) -> empty.add(Pair.of(idx, val)));
        assertTrue(empty.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Iterator.empty().foreachIndexed(null));
    }

    @Test
    public void testEmpty_SharedInstanceHoldsNoUserDataAndClosingItReleasesNothing() throws Exception {
        final Iterator<String, Exception> first = Iterator.empty();
        final Iterator<String, Exception> second = Iterator.empty();
        assertSame(first, second);

        final List<String> fieldNames = new ArrayList<>();
        for (final java.lang.reflect.Field field : Throwables.Iterator.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldNames.add(field.getName());
            }
        }
        assertEquals(1, fieldNames.size());
        assertEquals("isClosed", fieldNames.get(0));

        // The anonymous empty subclass adds no state of its own, so `isClosed` is the entire mutable state of
        // the shared instance - which is both the "holds no user data" half of the contract and what makes the
        // reset in the finally below a complete undo.
        for (final java.lang.reflect.Field field : first.getClass().getDeclaredFields()) {
            assertTrue(field.isSynthetic(), "the shared empty iterator must hold no state of its own: " + field);
        }

        final java.lang.reflect.Field isClosed = Throwables.Iterator.class.getDeclaredField("isClosed");
        isClosed.setAccessible(true);

        // Iterator.empty() hands the SAME instance to every caller in the JVM, and Seq.empty() wraps that very
        // instance - so by the time this test runs, any earlier test that closed an empty Seq has already set the
        // flag. The incoming value is therefore not ours to assert; capture it, drive the flag ourselves, and put
        // it back, so the test neither depends on execution order nor changes it for anyone else.
        final boolean flagOnEntry = (Boolean) isClosed.get(first);
        try {
            isClosed.set(first, Boolean.FALSE);
            assertFalse((Boolean) isClosed.get(first));

            first.closeResource();
            assertTrue((Boolean) isClosed.get(first));

            assertFalse(second.hasNext());
            assertThrows(NoSuchElementException.class, second::next);
            assertEquals(0, second.count());
            assertEquals(0, second.toArray().length);

            second.closeResource();
            assertFalse(second.hasNext());
        } finally {
            isClosed.set(first, flagOnEntry);
        }

        assertEquals(flagOnEntry, isClosed.get(Iterator.empty()));
    }

    @Test
    public void testIterator_HasNextAndAdvanceJavadocExamples() {
        // Deliberately declares no `throws` clause: these are the hasNext()/advance() javadoc examples, and the
        // point of the fix is that they bind E through a variable so they compile in an ordinary method body.
        final Throwables.Iterator<String, RuntimeException> none = Throwables.Iterator.empty();
        assertFalse(none.hasNext());

        final Throwables.Iterator<Integer, RuntimeException> unmoved = Throwables.Iterator.of(1, 2, 3);
        unmoved.advance(0);
        assertEquals(1, unmoved.next());
    }
}
