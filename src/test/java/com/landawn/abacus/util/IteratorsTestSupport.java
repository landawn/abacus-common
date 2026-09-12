package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;

public abstract class IteratorsTestSupport extends TestBase {

    protected List<Integer> intList;
    protected List<Integer> testList;
    protected List<Integer> emptyList;
    protected Iterator<Integer> testIterator;

    @BeforeEach
    public void setUp() {
        testList = Arrays.asList(1, 2, 3, 4, 5);
        intList = testList;
        emptyList = Collections.emptyList();
        testIterator = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).iterator();
    }

    @SafeVarargs
    protected static <T> List<T> list(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    protected static Throwable awaitAggregatedWorkerFailure(final Throwable firstFailure, final Throwable secondFailure, final long timeout,
            final TimeUnit unit) throws InterruptedException {
        final long deadline = System.nanoTime() + unit.toNanos(timeout);

        while (System.nanoTime() - deadline < 0) {
            if (Arrays.asList(firstFailure.getSuppressed()).contains(secondFailure)) {
                return firstFailure;
            } else if (Arrays.asList(secondFailure.getSuppressed()).contains(firstFailure)) {
                return secondFailure;
            }

            Thread.sleep(1);
        }

        return null;
    }

    protected static boolean awaitBlockedIteratorsWorker(final Thread firstWorker, final AtomicReference<Thread> waitingWorker, final long timeout,
            final TimeUnit unit) {
        final long deadline = System.nanoTime() + unit.toNanos(timeout);

        while (System.nanoTime() - deadline < 0) {
            for (final Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                final Thread thread = entry.getKey();

                if (thread == firstWorker || thread.getState() != Thread.State.BLOCKED) {
                    continue;
                }

                for (final StackTraceElement frame : entry.getValue()) {
                    if (Iterators.class.getName().equals(frame.getClassName())) {
                        waitingWorker.set(thread);
                        return true;
                    }
                }
            }

            Thread.yield();
        }

        return false;
    }

    protected static Throwable findCause(final Throwable failure, final Class<? extends Throwable> causeType) {
        Throwable current = failure;

        while (current != null && !causeType.isInstance(current)) {
            current = current.getCause();
        }

        return current;
    }

    /** Records which threads pulled from it, so a test can tell caller-thread reading from pool reading. */
    protected static final class ThreadRecordingIterator implements Iterator<Integer> {
        protected final int size;
        protected final java.util.Set<String> readerThreads = java.util.concurrent.ConcurrentHashMap.newKeySet();
        protected int cursor = 0;

        protected ThreadRecordingIterator(final int size) {
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            readerThreads.add(Thread.currentThread().getName());
            return cursor < size;
        }

        @Override
        public Integer next() {
            readerThreads.add(Thread.currentThread().getName());
            return cursor++;
        }
    }

    protected static String quotedNameInMessage(final Runnable builder) {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::run);
        final String message = ex.getMessage();
        assertNotNull(message);
        final int from = message.indexOf('\'');
        final int to = message.indexOf('\'', from + 1);
        assertTrue(from >= 0 && to > from, message);
        return message.substring(from + 1, to);
    }

    protected static List<Integer> drainInts(final IntIterator iter) {
        final List<Integer> result = new ArrayList<>();

        while (iter.hasNext()) {
            result.add(iter.nextInt());
        }

        return result;
    }

    protected static void assertNoFieldRetains(final Object iterator, final Object element) throws Exception {
        for (final java.lang.reflect.Field field : iterator.getClass().getDeclaredFields()) {
            if (field.getType().isPrimitive()) {
                continue;
            }

            field.setAccessible(true); // NOSONAR - reading an anonymous class's own fields in a white-box test
            final Object value = field.get(iterator);

            Assertions.assertNotSame(element, value, "field '" + field.getName() + "' still references the last element");

            if (value instanceof Object[]) {
                for (final Object each : (Object[]) value) {
                    Assertions.assertNotSame(element, each, "field '" + field.getName() + "' still holds the last element");
                }
            }
        }
    }

    protected static <T> List<T> drainToList(final Iterator<T> iter) {
        final List<T> result = new ArrayList<>();

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    protected static <T> List<T> takeFrom(final Iterator<T> iter, final int n) {
        final List<T> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            result.add(iter.next());
        }

        return result;
    }

    protected static List<Integer> collectForEach(final Iterator<Integer> iter, final long offset, final long count) throws Exception {
        final List<Integer> result = new ArrayList<>();
        Iterators.forEach(iter, offset, count, result::add);
        return result;
    }

    /** An AbstractCollection whose size() is whatever the constructor was told; the iterator tells the truth. */
    protected static final class ReviewFixes20260906LyingSizeCollection<T> extends java.util.AbstractCollection<T> {
        protected final java.util.Collection<T> delegate;
        protected final int lyingSize;

        protected ReviewFixes20260906LyingSizeCollection(final java.util.Collection<T> delegate, final int lyingSize) {
            this.delegate = delegate;
            this.lyingSize = lyingSize;
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return lyingSize;
        }
    }

    /** An Iterable - deliberately not a Collection - that counts hasNext() on the iterators it hands out. */
    protected static final class ReviewFixes20260906ProbingIterable<T> implements Iterable<T> {
        protected final List<T> backing;

        protected int hasNextCalls = 0;

        protected ReviewFixes20260906ProbingIterable(final List<T> backing) {
            this.backing = backing;
        }

        @Override
        public Iterator<T> iterator() {
            final Iterator<T> delegate = backing.iterator();

            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    hasNextCalls++;
                    return delegate.hasNext();
                }

                @Override
                public T next() {
                    return delegate.next();
                }
            };
        }
    }
}
