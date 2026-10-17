package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ShortIterator;

class CollectionFlatmapTest extends TestBase {
    @Test
    void innerIteratorFailureCanBeRetriedWithoutSkippingPendingValues() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final boolean primitive : new boolean[] { false, true }) {
                final IllegalStateException failure = new IllegalStateException("inner hasNext");
                final AtomicInteger mapped = new AtomicInteger();
                final Collection<Integer> collection = new AbstractCollection<>() {
                    @Override
                    public int size() {
                        return 2;
                    }

                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<>() {
                            private int next = 10;
                            private boolean failed;

                            @Override
                            public boolean hasNext() {
                                if (next == 11 && !failed) {
                                    failed = true;
                                    throw failure;
                                }
                                return next <= 11;
                            }

                            @Override
                            public Integer next() {
                                if (!hasNext()) {
                                    throw new NoSuchElementException();
                                }
                                return next++;
                            }
                        };
                    }
                };
                final Stream<Integer> source = iteratorSource ? Stream.of(List.of(1, 2).iterator()) : Stream.of(1, 2);
                final com.landawn.abacus.util.function.Function<Integer, Collection<Integer>> mapper = value -> {
                    mapped.incrementAndGet();
                    return value == 1 ? collection : List.of(20);
                };

                try (Stream<Integer> stream = primitive ? source.mapToInt(Integer::intValue).flatmap(mapper::apply).boxed() : source.flatmap(mapper)) {
                    final Iterator<Integer> iterator = stream.iterator();
                    assertEquals(10, iterator.next());
                    assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
                    assertEquals(1, mapped.get());
                    assertTrue(iterator.hasNext());
                    assertEquals(11, iterator.next());
                    assertEquals(20, iterator.next());
                    assertFalse(iterator.hasNext());
                    assertEquals(2, mapped.get());
                }
            }
        }
    }

    @Test
    void pendingInnerValuesDelayAnOuterFailureAndRetryKeepsTheNextSource() {
        for (final boolean primitive : new boolean[] { false, true }) {
            final IllegalStateException failure = new IllegalStateException("outer hasNext");
            final Iterator<Integer> sourceIterator = new Iterator<>() {
                private int next = 1;
                private boolean failed;

                @Override
                public boolean hasNext() {
                    if (next == 2 && !failed) {
                        failed = true;
                        throw failure;
                    }
                    return next <= 2;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return next++;
                }
            };
            final Stream<Integer> source = Stream.of(sourceIterator);
            final com.landawn.abacus.util.function.Function<Integer, Collection<Integer>> mapper = value -> value == 1 ? List.of(10, 11) : List.of(20);

            try (Stream<Integer> stream = primitive ? source.mapToInt(Integer::intValue).flatmap(mapper::apply).boxed() : source.flatmap(mapper)) {
                final Iterator<Integer> iterator = stream.iterator();
                assertEquals(10, iterator.next());
                assertTrue(iterator.hasNext());
                assertTrue(iterator.hasNext());
                assertEquals(11, iterator.next());
                assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
                assertEquals(20, iterator.next());
                assertFalse(iterator.hasNext());
            }
        }
    }

    @Test
    void byteFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final ByteStream source = iteratorSource ? ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4))
                        : ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (ByteStream flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmap(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null
                                            : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList((byte) 7, null, (byte) 8) : List.of((byte) 9);
                                })) {
                    final Iterator<Byte> iterator = flattened.iterator();
                    final List<Byte> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList((byte) 0, (byte) 7, (byte) 8, (byte) 9), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void byteToObjectFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final ByteStream source = iteratorSource ? ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4))
                        : ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (Stream<String> flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmapToObj(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList("a", null, "b") : List.of("c");
                                })) {
                    final Iterator<String> iterator = flattened.iterator();
                    final List<String> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(null, "a", "b", "c"), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void charFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final CharStream source = iteratorSource ? CharStream.of(CharIterator.of((char) 1, (char) 2, (char) 3, (char) 4))
                        : CharStream.of((char) 1, (char) 2, (char) 3, (char) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (CharStream flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmap(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null
                                            : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList((char) 7, null, (char) 8) : List.of((char) 9);
                                })) {
                    final Iterator<Character> iterator = flattened.iterator();
                    final List<Character> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList((char) 0, (char) 7, (char) 8, (char) 9), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void charToObjectFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final CharStream source = iteratorSource ? CharStream.of(CharIterator.of((char) 1, (char) 2, (char) 3, (char) 4))
                        : CharStream.of((char) 1, (char) 2, (char) 3, (char) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (Stream<String> flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmapToObj(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList("a", null, "b") : List.of("c");
                                })) {
                    final Iterator<String> iterator = flattened.iterator();
                    final List<String> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(null, "a", "b", "c"), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void shortFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final ShortStream source = iteratorSource ? ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4))
                        : ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (ShortStream flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmap(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null
                                            : value == 2 ? Collections.emptyList()
                                                    : value == 3 ? Arrays.asList((short) 7, null, (short) 8) : List.of((short) 9);
                                })) {
                    final Iterator<Short> iterator = flattened.iterator();
                    final List<Short> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList((short) 0, (short) 7, (short) 8, (short) 9), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void shortToObjectFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final ShortStream source = iteratorSource ? ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3, (short) 4))
                        : ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (Stream<String> flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmapToObj(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList("a", null, "b") : List.of("c");
                                })) {
                    final Iterator<String> iterator = flattened.iterator();
                    final List<String> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(null, "a", "b", "c"), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void intFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final IntStream source = iteratorSource ? IntStream.of(IntIterator.of(1, 2, 3, 4)) : IntStream.of(1, 2, 3, 4);
                final AtomicInteger calls = new AtomicInteger();
                try (IntStream flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmap(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList(7, null, 8) : List.of(9);
                                })) {
                    final Iterator<Integer> iterator = flattened.iterator();
                    final List<Integer> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(0, 7, 8, 9), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void intToObjectFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final IntStream source = iteratorSource ? IntStream.of(IntIterator.of(1, 2, 3, 4)) : IntStream.of(1, 2, 3, 4);
                final AtomicInteger calls = new AtomicInteger();
                try (Stream<String> flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmapToObj(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList("a", null, "b") : List.of("c");
                                })) {
                    final Iterator<String> iterator = flattened.iterator();
                    final List<String> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(null, "a", "b", "c"), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void longFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final LongStream source = iteratorSource ? LongStream.of(LongIterator.of((long) 1, (long) 2, (long) 3, (long) 4))
                        : LongStream.of((long) 1, (long) 2, (long) 3, (long) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (LongStream flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmap(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null
                                            : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList((long) 7, null, (long) 8) : List.of((long) 9);
                                })) {
                    final Iterator<Long> iterator = flattened.iterator();
                    final List<Long> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList((long) 0, (long) 7, (long) 8, (long) 9), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void longToObjectFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final LongStream source = iteratorSource ? LongStream.of(LongIterator.of((long) 1, (long) 2, (long) 3, (long) 4))
                        : LongStream.of((long) 1, (long) 2, (long) 3, (long) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (Stream<String> flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmapToObj(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList("a", null, "b") : List.of("c");
                                })) {
                    final Iterator<String> iterator = flattened.iterator();
                    final List<String> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(null, "a", "b", "c"), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void floatFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final FloatStream source = iteratorSource ? FloatStream.of(FloatIterator.of((float) 1, (float) 2, (float) 3, (float) 4))
                        : FloatStream.of((float) 1, (float) 2, (float) 3, (float) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (FloatStream flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmap(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null
                                            : value == 2 ? Collections.emptyList()
                                                    : value == 3 ? Arrays.asList((float) 7, null, (float) 8) : List.of((float) 9);
                                })) {
                    final Iterator<Float> iterator = flattened.iterator();
                    final List<Float> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList((float) 0, (float) 7, (float) 8, (float) 9), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void floatToObjectFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final FloatStream source = iteratorSource ? FloatStream.of(FloatIterator.of((float) 1, (float) 2, (float) 3, (float) 4))
                        : FloatStream.of((float) 1, (float) 2, (float) 3, (float) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (Stream<String> flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmapToObj(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList("a", null, "b") : List.of("c");
                                })) {
                    final Iterator<String> iterator = flattened.iterator();
                    final List<String> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(null, "a", "b", "c"), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void doubleFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final DoubleStream source = iteratorSource ? DoubleStream.of(DoubleIterator.of((double) 1, (double) 2, (double) 3, (double) 4))
                        : DoubleStream.of((double) 1, (double) 2, (double) 3, (double) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (DoubleStream flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmap(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null
                                            : value == 2 ? Collections.emptyList()
                                                    : value == 3 ? Arrays.asList((double) 7, null, (double) 8) : List.of((double) 9);
                                })) {
                    final Iterator<Double> iterator = flattened.iterator();
                    final List<Double> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList((double) 0, (double) 7, (double) 8, (double) 9), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void doubleToObjectFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final DoubleStream source = iteratorSource ? DoubleStream.of(DoubleIterator.of((double) 1, (double) 2, (double) 3, (double) 4))
                        : DoubleStream.of((double) 1, (double) 2, (double) 3, (double) 4);
                final AtomicInteger calls = new AtomicInteger();
                try (Stream<String> flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmapToObj(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList("a", null, "b") : List.of("c");
                                })) {
                    final Iterator<String> iterator = flattened.iterator();
                    final List<String> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(null, "a", "b", "c"), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void objectFlatmapHandlesNullEmptyAndExhaustion() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final int mode : new int[] { 0, 1, 2 }) {
                final Stream<Integer> source = iteratorSource ? Stream.of(List.of(1, 2, 3, 4).iterator()) : Stream.of(1, 2, 3, 4);
                final AtomicInteger calls = new AtomicInteger();
                try (Stream<String> flattened = (mode == 0 ? source
                        : source.parallel(BaseStream.ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(mode == 1 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR)
                                .build())).flatmap(value -> {
                                    calls.incrementAndGet();
                                    return value == 1 ? null : value == 2 ? Collections.emptyList() : value == 3 ? Arrays.asList("a", null, "b") : List.of("c");
                                })) {
                    final Iterator<String> iterator = flattened.iterator();
                    final List<String> actual = new ArrayList<>();
                    iterator.forEachRemaining(actual::add);
                    actual.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    assertEquals(Arrays.asList(null, "a", "b", "c"), actual);
                    assertEquals(4, calls.get());
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    void limitDoesNotMapAnotherCollection() {
        final AtomicInteger calls = new AtomicInteger();
        assertEquals(List.of(1), Stream.of(1, 2, 3).flatmap(value -> {
            calls.incrementAndGet();
            return List.of(value, value);
        }).limit(1).toList());
        assertEquals(1, calls.get());
    }
}
