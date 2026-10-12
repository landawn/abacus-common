package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.ObjIteratorEx.BufferedIterator;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
// Close and failure behaviour is covered in the regular suite by StreamParallelConcatCloseReview20261010Test.
@Tag("slow-test")
class StreamParallelConcatMemoryReview20261010Test extends TestBase {
    @Test
    void closingReleasesQueuedAndPrefetchedValues() throws Exception {
        final CountDownLatch queued = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Iterator<Object> source = new Iterator<>() {
            private int count;

            @Override
            public boolean hasNext() {
                if (count == 4) {
                    queued.countDown();
                    await(release);
                }
                return count < 4;
            }

            @Override
            public Object next() {
                count++;
                return new Object();
            }
        };
        final Stream<Object> combined = Stream.parallelConcat(List.of(Stream.of(source)), 1, 8);
        try {
            final ObjIteratorEx<Object> iterator = combined.iteratorEx();
            assertTrue(iterator.hasNext());
            await(queued);
            final BufferedIterator<?> buffered = findBuffer(iterator);
            final BlockingQueue<?> queue = fieldByType(buffered, BlockingQueue.class);
            assertEquals(3, queue.size());
            combined.close();
            assertTrue(queue.isEmpty());
            assertNull(valueField(buffered, "next"));
            assertFalse(buffered.hasNext());
        } finally {
            release.countDown();
            combined.close();
        }
    }

    @Test
    void valueReturningFromWorkerAfterCloseIsDiscarded() throws Exception {
        final CountDownLatch readingSecond = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Iterator<Object> source = new Iterator<>() {
            private int count;

            @Override
            public boolean hasNext() {
                return count < 2;
            }

            @Override
            public Object next() {
                if (++count == 2) {
                    readingSecond.countDown();
                    await(release);
                }
                return new Object();
            }
        };
        final Stream<Object> combined = Stream.parallelConcat(List.of(Stream.of(source)), 1, 8);
        try {
            final ObjIteratorEx<Object> iterator = combined.iteratorEx();
            assertTrue(iterator.hasNext());
            await(readingSecond);
            final BufferedIterator<?> buffered = findBuffer(iterator);
            final BlockingQueue<?> queue = fieldByType(buffered, BlockingQueue.class);
            final AtomicInteger workers = fieldByType(buffered, AtomicInteger.class);
            combined.close();
            release.countDown();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (workers.get() != 0 && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            assertEquals(0, workers.get(), "worker did not finish");
            assertTrue(queue.isEmpty(), "a late offer must not repopulate a closed buffer");
            assertNull(valueField(buffered, "next"));
        } finally {
            release.countDown();
            combined.close();
        }
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "latch timed out");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static BufferedIterator<?> findBuffer(final Object root) throws Exception {
        final ArrayDeque<Object> pending = new ArrayDeque<>();
        final IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            final Object value = pending.removeFirst();
            if (visited.put(value, Boolean.TRUE) != null) {
                continue;
            }
            if (value instanceof BufferedIterator<?> buffered) {
                return buffered;
            }
            for (Class<?> type = value.getClass(); type != null && type.getPackageName().equals(Stream.class.getPackageName()); type = type.getSuperclass()) {
                for (final Field field : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                        field.setAccessible(true);
                        final Object child = field.get(value);
                        if (child != null && child.getClass().getPackageName().equals(Stream.class.getPackageName())) {
                            pending.add(child);
                        }
                    }
                }
            }
        }
        throw new AssertionError("No buffered iterator found");
    }

    private static Object valueField(final Object target, final String name) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static <T> T fieldByType(final Object target, final Class<T> type) throws Exception {
        for (final Field field : target.getClass().getDeclaredFields()) {
            if (type.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return type.cast(field.get(target));
            }
        }
        throw new AssertionError("No field of type " + type.getName());
    }
}
