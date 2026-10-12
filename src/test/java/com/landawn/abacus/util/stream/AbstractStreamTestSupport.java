package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Shared fixtures and lifecycle for the AbstractStream contract tests. */
public abstract class AbstractStreamTestSupport extends TestBase {

    protected static Iterator<Integer> sourceFailingOnceAt(final int failureIndex, final boolean failInHasNext, final IllegalStateException failure) {
        return new Iterator<>() {
            private final Iterator<Integer> values = Arrays.asList(0, 1, 2, 3, 4, 5).iterator();
            private int index;
            private boolean failed;

            private void failOnce(final boolean fromHasNext) {
                if (!failed && index == failureIndex && fromHasNext == failInHasNext) {
                    failed = true;
                    throw failure;
                }
            }

            @Override
            public boolean hasNext() {
                failOnce(true);
                return values.hasNext();
            }

            @Override
            public Integer next() {
                failOnce(false);
                final Integer value = values.next();
                index++;
                return value;
            }
        };
    }

    protected Stream<Integer> stream;

    protected Stream<Object> objectStream;

    protected Stream<String> stringStream;

    @TempDir
    Path tempFolder;

    @SafeVarargs
    protected final <T> Stream<T> createStream(final T... elements) {
        return Stream.of(elements);
    }

    protected <T> Stream<T> createStream(final Iterable<? extends T> iter) {
        return Stream.of(iter);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestBean {
        private String name;
        private int age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPerson {
        private int id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestOrder {
        private int personId;
        private String orderName;
    }

    protected static void assertSplitAtExhaustedOuterIteratorDoesNotConsumeSecondStream(
            final java.util.function.Function<Stream<Integer>, Stream<Stream<Integer>>> splitter) {
        for (final boolean advanceFirst : new boolean[] { false, true }) {
            try (Stream<Stream<Integer>> parts = splitter.apply(Stream.of(Arrays.asList(1, 2, 3, 4).iterator()))) {
                final ObjIteratorEx<Stream<Integer>> iterator = parts.iteratorEx();
                try (Stream<Integer> first = iterator.next();
                     Stream<Integer> second = iterator.next()) {
                    assertFalse(iterator.hasNext());
                    if (advanceFirst) {
                        iterator.advance(1);
                    }
                    assertEquals(0L, iterator.count());
                    iterator.advance(1);
                    iterator.advance(Long.MAX_VALUE);
                    assertEquals(0L, iterator.count());
                    assertFalse(iterator.hasNext());
                    assertThrows(java.util.NoSuchElementException.class, iterator::next);
                    assertEquals(Arrays.asList(1, 2), first.toList());
                    assertEquals(Arrays.asList(3, 4), second.toList());
                }
            }
        }
    }

    protected static void assertSplitAtKeepsIteratorSourceOpen(final java.util.function.Function<Stream<Integer>, Stream<Stream<Integer>>> splitter) {
        final AtomicInteger closeCount = new AtomicInteger();
        final Stream<Integer> source = Stream.of(Arrays.asList(1, 2, 3, 4).iterator()).onClose(closeCount::incrementAndGet);
        final List<Stream<Integer>> parts = splitter.apply(source).toList();

        assertEquals(0, closeCount.get());
        assertEquals(Arrays.asList(1, 2), parts.get(0).toList());
        assertEquals(0, closeCount.get());
        assertEquals(Arrays.asList(3, 4), parts.get(1).toList());
        assertEquals(1, closeCount.get());

        final AtomicInteger abandonedCloseCount = new AtomicInteger();
        final Stream<Integer> abandonedSource = Stream.of(Arrays.asList(1, 2, 3, 4).iterator()).onClose(abandonedCloseCount::incrementAndGet);
        final List<Stream<Integer>> firstPartOnly = splitter.apply(abandonedSource).limit(1).toList();

        assertEquals(1, abandonedCloseCount.get());
        assertEquals(Arrays.asList(1, 2), firstPartOnly.get(0).toList());
    }

    // Regression: persist*(OutputStream) must write the data to the caller's stream but must NOT close it
    // (the caller owns the stream, as shown by the try-with-resources usage in the Javadoc examples).
    // persist(...,OutputStream) already uses Objectory.recycle(bw) (no close); persistToCsv/persistToJson
    // must behave the same. See review 2026-06-30 finding S06-06.
    protected static final class CloseCountingOutputStream extends java.io.FilterOutputStream {
        final AtomicInteger closeCount = new AtomicInteger();

        CloseCountingOutputStream(final java.io.OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            closeCount.incrementAndGet();
            super.close();
        }
    }

    protected static Stream<Integer> newCloseTrackingArrayStream(final AtomicInteger closeCount) {
        final ArrayStream<Integer> stream = new ArrayStream<>(new Integer[] { 1, 2 }) {
            @Override
            public synchronized void close() {
                if (!isClosed()) {
                    closeCount.incrementAndGet();
                }

                super.close();
            }
        };

        assertTrue(StreamBase.isEmptyCloseHandlers(stream.closeHandlers()));
        return stream;
    }

    protected static Stream<Integer> rotatedForG086(final Integer[] backing, final int from, final int len, final int source, final int distance) {
        return (source == 0 ? Stream.of(backing, from, from + len) : Stream.of(Arrays.asList(backing).subList(from, from + len).iterator())).rotated(distance);
    }

    // ---- bug review 2026-09-27 verify G122 end ----

    // ---- bug review 2026-10-02 G089 begin ----

    // A target Writer whose write() (failWrite) or flush() always fails; each failure message carries its 1-based number.
    protected static java.io.Writer g089FailingWriter(final boolean failWrite, final AtomicInteger failures) {
        return new java.io.Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                if (failWrite) {
                    throw new IOException("write failed #" + failures.incrementAndGet());
                }
            }

            @Override
            public void flush() throws IOException {
                if (!failWrite) {
                    throw new IOException("flush failed #" + failures.incrementAndGet());
                }
            }

            @Override
            public void close() {
            }
        };
    }

    // ---- bug review 2026-10-02 G089 end ----

    // ---- bug review 2026-10-02 verify G122 begin ----

    // An OutputStream target whose flush() always fails; each failure message carries its 1-based number.
    protected static java.io.OutputStream g122FlushFailingStream(final AtomicInteger failures) {
        return new java.io.OutputStream() {
            @Override
            public void write(final int b) {
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("flush failed #" + failures.incrementAndGet());
            }
        };
    }
}
