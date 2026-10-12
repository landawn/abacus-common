package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.WindowAssigner;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.u.Optional;

/** Shared fixtures and lifecycle for the Stream contract tests. */
public abstract class StreamTestSupport extends AbstractTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path tempFolder;

    protected List<Integer> testData;

    protected List<Integer> testList;

    protected Stream<Integer> stream;

    protected List<Integer> mainStreamResults;

    protected List<Integer> subscriberStreamResults;

    protected ExecutorService customExecutor;

    protected List<Stream<?>> streamsToClose = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        testList = Arrays.asList(1, 2, 3, 4, 5);
        testData = testList;
        stream = createStream(testList);
        mainStreamResults = new ArrayList<>();
        subscriberStreamResults = new ArrayList<>();
        customExecutor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    public void tearDown() {
        if (stream != null) {
            stream.close();
        }

        if (customExecutor != null) {
            customExecutor.shutdownNow();
        }

        for (Stream<?> streamToClose : streamsToClose) {
            try {
                streamToClose.close();
            } catch (Exception e) {
            }
        }

        streamsToClose.clear();
    }

    protected Stream<Boolean> createStream(final boolean... data) {
        return Stream.of(data);
    }

    protected Stream<Boolean> createStream(final boolean[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Character> createStream(final char... data) {
        return Stream.of(data);
    }

    protected Stream<Character> createStream(final char[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Byte> createStream(final byte... data) {
        return Stream.of(data);
    }

    protected Stream<Byte> createStream(final byte[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Short> createStream(final short... data) {
        return Stream.of(data);
    }

    protected Stream<Short> createStream(final short[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Integer> createStream(final int... data) {
        return Stream.of(data);
    }

    protected Stream<Integer> createStream(final int[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Long> createStream(final long... data) {
        return Stream.of(data);
    }

    protected Stream<Long> createStream(final long[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Float> createStream(final float... data) {
        return Stream.of(data);
    }

    protected Stream<Float> createStream(final float[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Double> createStream(final double... data) {
        return Stream.of(data);
    }

    protected Stream<Double> createStream(final double[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    @SafeVarargs
    protected final <T> Stream<T> createStream01(final T... data) {
        return Stream.of(data);
    }

    protected <T> Stream<T> createStream(final T[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected <T> Stream<T> createStream(final Iterable<? extends T> data) {
        return Stream.of(data);
    }

    protected <T> Stream<T> createStream(final Collection<? extends T> data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected <T> Stream<T> createStream(final Iterator<? extends T> data) {
        return Stream.of(data);
    }

    protected <K, V> Stream<Map.Entry<K, V>> createStream(final Map<? extends K, ? extends V> map) {
        return Stream.of(map);
    }

    protected <T> Stream<T> createStream(final Optional<T> op) {
        return op == null || op.isEmpty() ? Stream.empty() : Stream.of(op.get());
    }

    protected <T> Stream<T> createStream(final java.util.Optional<T> op) {
        return op == null || op.isEmpty() ? Stream.empty() : Stream.of(op.get());
    }

    protected <T> Stream<T> createStream(final Enumeration<? extends T> enumeration) {
        return Stream.of(enumeration);
    }

    protected <T> Stream<T> trackStream(Stream<T> stream) {
        streamsToClose.add(stream);
        return stream;
    }

    protected static void assertFlatMapClosesMappedStreams(final Stream<Integer> source) {
        final List<Stream<Integer>> mappedStreams = Arrays.asList(Stream.empty(), Stream.of(20));

        assertEquals(1, source.flatMap(mappedStreams::get).count());

        for (final Stream<Integer> mappedStream : mappedStreams) {
            assertThrows(IllegalStateException.class, () -> mappedStream.count());
        }
    }

    protected static void assertByteFlatMapClosesMappedStream(final ByteStream source) {
        final ByteStream mapped = ByteStream.of((byte) 2);
        final ByteStream flattened = source.flatMap(ignored -> mapped);

        flattened.iteratorEx().toArray();
        assertThrows(IllegalStateException.class, mapped::count);
        flattened.close();
    }

    protected static void assertCharFlatMapClosesMappedStream(final CharStream source) {
        final CharStream mapped = CharStream.of('b');
        final CharStream flattened = source.flatMap(ignored -> mapped);

        flattened.iteratorEx().toArray();
        assertThrows(IllegalStateException.class, mapped::count);
        flattened.close();
    }

    protected static void assertShortFlatMapClosesMappedStream(final ShortStream source) {
        final ShortStream mapped = ShortStream.of((short) 2);
        final ShortStream flattened = source.flatMap(ignored -> mapped);

        flattened.iteratorEx().toArray();
        assertThrows(IllegalStateException.class, mapped::count);
        flattened.close();
    }

    protected static void assertIntFlatMapClosesMappedStream(final IntStream source) {
        final IntStream mapped = IntStream.of(2);
        final IntStream flattened = source.flatMap(ignored -> mapped);

        flattened.iteratorEx().toArray();
        assertThrows(IllegalStateException.class, mapped::count);
        flattened.close();
    }

    protected static void assertLongFlatMapClosesMappedStream(final LongStream source) {
        final LongStream mapped = LongStream.of(2L);
        final LongStream flattened = source.flatMap(ignored -> mapped);

        flattened.iteratorEx().toArray();
        assertThrows(IllegalStateException.class, mapped::count);
        flattened.close();
    }

    protected static void assertFloatFlatMapClosesMappedStream(final FloatStream source) {
        final FloatStream mapped = FloatStream.of(2F);
        final FloatStream flattened = source.flatMap(ignored -> mapped);

        flattened.iteratorEx().toArray();
        assertThrows(IllegalStateException.class, mapped::count);
        flattened.close();
    }

    protected static void assertDoubleFlatMapClosesMappedStream(final DoubleStream source) {
        final DoubleStream mapped = DoubleStream.of(2D);
        final DoubleStream flattened = source.flatMap(ignored -> mapped);

        flattened.iteratorEx().toArray();
        assertThrows(IllegalStateException.class, mapped::count);
        flattened.close();
    }

    protected static void assertFlatMapToPrimitiveClosesMappedStreams(final Supplier<Stream<Integer>> sourceFactory) {
        final List<Runnable> charCloseProbes = Collections.synchronizedList(new ArrayList<>());
        CharStream charStream = sourceFactory.get().flatMapToChar(value -> {
            final CharStream mapped = CharStream.of((char) ('a' + value));
            charCloseProbes.add(mapped::count);
            return mapped;
        });
        charStream.iteratorEx().toArray();
        assertMappedStreamsClosed(charCloseProbes, 2);
        charStream.close();

        final List<Runnable> byteCloseProbes = Collections.synchronizedList(new ArrayList<>());
        ByteStream byteStream = sourceFactory.get().flatMapToByte(value -> {
            final ByteStream mapped = ByteStream.of(value.byteValue());
            byteCloseProbes.add(mapped::count);
            return mapped;
        });
        byteStream.iteratorEx().toArray();
        assertMappedStreamsClosed(byteCloseProbes, 2);
        byteStream.close();

        final List<Runnable> shortCloseProbes = Collections.synchronizedList(new ArrayList<>());
        ShortStream shortStream = sourceFactory.get().flatMapToShort(value -> {
            final ShortStream mapped = ShortStream.of(value.shortValue());
            shortCloseProbes.add(mapped::count);
            return mapped;
        });
        shortStream.iteratorEx().toArray();
        assertMappedStreamsClosed(shortCloseProbes, 2);
        shortStream.close();

        final List<Runnable> intCloseProbes = Collections.synchronizedList(new ArrayList<>());
        IntStream intStream = sourceFactory.get().flatMapToInt(value -> {
            final IntStream mapped = IntStream.of(value);
            intCloseProbes.add(mapped::count);
            return mapped;
        });
        intStream.iteratorEx().toArray();
        assertMappedStreamsClosed(intCloseProbes, 2);
        intStream.close();

        final List<Runnable> longCloseProbes = Collections.synchronizedList(new ArrayList<>());
        LongStream longStream = sourceFactory.get().flatMapToLong(value -> {
            final LongStream mapped = LongStream.of(value.longValue());
            longCloseProbes.add(mapped::count);
            return mapped;
        });
        longStream.iteratorEx().toArray();
        assertMappedStreamsClosed(longCloseProbes, 2);
        longStream.close();

        final List<Runnable> floatCloseProbes = Collections.synchronizedList(new ArrayList<>());
        FloatStream floatStream = sourceFactory.get().flatMapToFloat(value -> {
            final FloatStream mapped = FloatStream.of(value.floatValue());
            floatCloseProbes.add(mapped::count);
            return mapped;
        });
        floatStream.iteratorEx().toArray();
        assertMappedStreamsClosed(floatCloseProbes, 2);
        floatStream.close();

        final List<Runnable> doubleCloseProbes = Collections.synchronizedList(new ArrayList<>());
        DoubleStream doubleStream = sourceFactory.get().flatMapToDouble(value -> {
            final DoubleStream mapped = DoubleStream.of(value.doubleValue());
            doubleCloseProbes.add(mapped::count);
            return mapped;
        });
        doubleStream.iteratorEx().toArray();
        assertMappedStreamsClosed(doubleCloseProbes, 2);
        doubleStream.close();
    }

    protected static void assertMappedStreamsClosed(final List<Runnable> closeProbes, final int expectedCount) {
        assertEquals(expectedCount, closeProbes.size());

        for (final Runnable closeProbe : new ArrayList<>(closeProbes)) {
            assertThrows(IllegalStateException.class, closeProbe::run);
        }
    }

    /**
     * Creates a Windows directory junction, or aborts the test if this platform has none.
     *
     * <p>A junction is deliberately used instead of a symbolic link: {@code Files.createSymbolicLink}
     * needs {@code SeCreateSymbolicLinkPrivilege} and fails on a stock Windows account, which is why
     * {@link #testListFiles_RecursiveTerminatesOnACyclicSymlink()} is skipped there - exactly the platform
     * where junctions exist. {@code mklink /J} needs no privilege.
     */
    protected static void createJunctionOrAbort(final File link, final File target) throws Exception {
        if (!IOUtil.IS_OS_WINDOWS) {
            Assumptions.abort("directory junctions are a Windows concept");
        }

        final Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.getAbsolutePath(), target.getAbsolutePath()).redirectErrorStream(true).start();

        p.waitFor();

        if (!(p.exitValue() == 0 && link.exists())) {
            Assumptions.abort("a directory junction could not be created here");
        }
    }

    /** Unlinks a junction without touching its target. Never use a recursive delete on one. */
    protected static void unlinkQuietly(final File link) {
        try {
            java.nio.file.Files.deleteIfExists(link.toPath());
        } catch (final IOException e) {
            // best effort: the temp folder rule cleans up the rest
        }
    }

    protected static void assertCollectionZipClosesSnapshottedSource(final List<?> sources, final Stream<?> zipped, final AtomicInteger closedCount) {
        sources.clear();
        zipped.close();
        assertEquals(1, closedCount.getAndSet(0));
    }

    protected static final class IdentityWindowAssigner extends WindowAssigner {

        @Override
        protected <T> ObjIterator<T> process(final ObjIterator<T> iter) {
            return iter;
        }

        <T> ObjIterator<T> apply(final ObjIterator<T> iter) {
            return process(iter);
        }
    }

    protected static <T> Iterator<T> countingIterator(final List<T> values, final AtomicInteger nextCalls) {
        final Iterator<T> delegate = values.iterator();

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public T next() {
                nextCalls.incrementAndGet();
                return delegate.next();
            }
        };
    }

    protected static <T> Iterator<T> signallingIterator(final List<T> values, final CountDownLatch exhausted) {
        final Iterator<T> delegate = values.iterator();

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                final boolean hasNext = delegate.hasNext();

                if (!hasNext) {
                    exhausted.countDown();
                }

                return hasNext;
            }

            @Override
            public T next() {
                return delegate.next();
            }
        };
    }

    protected static void assertSubscriberOfferPreservesInterrupt(final int operation) {
        final AtomicReference<Runnable> subscriberTask = new AtomicReference<>();
        final com.landawn.abacus.util.Throwables.Consumer<Stream<Integer>, RuntimeException> subscriber = attached -> assertThrows(RuntimeException.class,
                attached::count);
        final Stream<Integer> source = Stream.of(1, 2);
        final Stream<Integer> main = switch (operation) {
            case 0 -> source.addSubscriber(subscriber, 1, 1, subscriberTask::set);
            case 1 -> source.filterWhileAddSubscriber(value -> value == 2, subscriber, 1, 1, subscriberTask::set);
            case 2 -> source.takeWhileAddSubscriber(value -> value < 1, subscriber, subscriberTask::set);
            case 3 -> source.dropWhileAddSubscriber(value -> value < 2, subscriber, 1, 1, subscriberTask::set);
            default -> throw new AssertionError(operation);
        };

        try {
            final ObjIterator<Integer> iterator = main.iterator();
            Thread.currentThread().interrupt();

            if (operation == 2) {
                assertFalse(iterator.hasNext());
            } else {
                assertTrue(iterator.hasNext());
                assertEquals(operation == 0 ? 1 : 2, iterator.next());
            }

            assertTrue(Thread.currentThread().isInterrupted(), "subscriber delivery must preserve caller interruption");
        } finally {
            Thread.interrupted();
            final Runnable task = subscriberTask.get();
            if (task != null) {
                task.run();
            }
            main.close();
        }
    }

    protected static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "parallel zip did not check the exhausted source");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
