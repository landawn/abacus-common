package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ShortIterator;

@Tag("slow-test")
class CollectionFlatmapRetentionTest extends TestBase {
    private static java.util.stream.Stream<Arguments> iteratorFlatmaps() {
        return java.util.stream.Stream.of(flatmapCase("object", source -> Stream.of(source).flatmap(List::of)),
                flatmapCase("byte", source -> Stream.of(source).mapToByte(Integer::byteValue).flatmap(List::of)),
                flatmapCase("byte to object", source -> Stream.of(source).mapToByte(Integer::byteValue).flatmapToObj(List::of)),
                flatmapCase("char", source -> Stream.of(source).mapToChar(value -> (char) value.intValue()).flatmap(List::of)),
                flatmapCase("char to object", source -> Stream.of(source).mapToChar(value -> (char) value.intValue()).flatmapToObj(List::of)),
                flatmapCase("short", source -> Stream.of(source).mapToShort(Integer::shortValue).flatmap(List::of)),
                flatmapCase("short to object", source -> Stream.of(source).mapToShort(Integer::shortValue).flatmapToObj(List::of)),
                flatmapCase("int", source -> Stream.of(source).mapToInt(Integer::intValue).flatmap(List::of)),
                flatmapCase("int to object", source -> Stream.of(source).mapToInt(Integer::intValue).flatmapToObj(List::of)),
                flatmapCase("long", source -> Stream.of(source).mapToLong(Integer::longValue).flatmap(List::of)),
                flatmapCase("long to object", source -> Stream.of(source).mapToLong(Integer::longValue).flatmapToObj(List::of)),
                flatmapCase("float", source -> Stream.of(source).mapToFloat(Integer::floatValue).flatmap(List::of)),
                flatmapCase("float to object", source -> Stream.of(source).mapToFloat(Integer::floatValue).flatmapToObj(List::of)),
                flatmapCase("double", source -> Stream.of(source).mapToDouble(Integer::doubleValue).flatmap(List::of)),
                flatmapCase("double to object", source -> Stream.of(source).mapToDouble(Integer::doubleValue).flatmapToObj(List::of)));
    }

    private static Arguments flatmapCase(final String name, final Function<Iterator<Integer>, BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory) {
        return Arguments.of(name, factory);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("iteratorFlatmaps")
    void releasesExhaustedInnerBeforeOuterFailure(final String name, final Function<Iterator<Integer>, BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory)
            throws Exception {
        final IllegalStateException failure = new IllegalStateException("outer hasNext");
        final Iterator<Integer> source = new Iterator<>() {
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

        try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = factory.apply(source)) {
            final Iterator<?> iterator = stream.iterator();
            final Object first = iterator.next();
            assertEquals(1, first instanceof Character value ? value.charValue() : ((Number) first).intValue());
            // Inspect the retained inner directly so the failure does not depend on GC timing.
            final Field current = iterator.getClass().getDeclaredField("cur");
            current.setAccessible(true);
            assertNotNull(current.get(iterator));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
            assertNull(current.get(iterator), "The exhausted mapped collection must be released before querying the outer source");
            final Object second = iterator.next();
            assertEquals(2, second instanceof Character value ? value.charValue() : ((Number) second).intValue());
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    private static final class TrackedList<T> extends ArrayList<T> {
        private Iterator<T> lastIterator;

        TrackedList(final T value) {
            add(value);
        }

        @Override
        public Iterator<T> iterator() {
            lastIterator = super.iterator();
            return lastIterator;
        }
    }

    private static void assertReleased(final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream, final TrackedList<?> mapped) throws Exception {
        try (stream) {
            final Iterator<?> iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            iterator.next();
            assertFalse(iterator.hasNext());
            assertFalse(iterator.hasNext());
            // Inspect only the iterator's own state. Caller-owned mapper captures are unrelated to
            // the extra collection and exhausted-iterator references this regression covers.
            for (final Field field : iterator.getClass().getDeclaredFields()) {
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    assertNotSame(mapped, field.get(iterator), field.toString());
                    assertNotSame(mapped.lastIterator, field.get(iterator), field.toString());
                }
            }
        }
    }

    @Test
    void byteReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<Byte> mapped = new TrackedList<>((byte) 7);
            final ByteStream source = iteratorSource ? ByteStream.of(ByteIterator.of((byte) 1)) : ByteStream.of((byte) 1);
            assertReleased(source.flatmap(value -> mapped), mapped);
        }
    }

    @Test
    void byteToObjectReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<String> mapped = new TrackedList<>("mapped");
            final ByteStream source = iteratorSource ? ByteStream.of(ByteIterator.of((byte) 1)) : ByteStream.of((byte) 1);
            assertReleased(source.flatmapToObj(value -> mapped), mapped);
        }
    }

    @Test
    void charReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<Character> mapped = new TrackedList<>((char) 7);
            final CharStream source = iteratorSource ? CharStream.of(CharIterator.of((char) 1)) : CharStream.of((char) 1);
            assertReleased(source.flatmap(value -> mapped), mapped);
        }
    }

    @Test
    void charToObjectReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<String> mapped = new TrackedList<>("mapped");
            final CharStream source = iteratorSource ? CharStream.of(CharIterator.of((char) 1)) : CharStream.of((char) 1);
            assertReleased(source.flatmapToObj(value -> mapped), mapped);
        }
    }

    @Test
    void shortReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<Short> mapped = new TrackedList<>((short) 7);
            final ShortStream source = iteratorSource ? ShortStream.of(ShortIterator.of((short) 1)) : ShortStream.of((short) 1);
            assertReleased(source.flatmap(value -> mapped), mapped);
        }
    }

    @Test
    void shortToObjectReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<String> mapped = new TrackedList<>("mapped");
            final ShortStream source = iteratorSource ? ShortStream.of(ShortIterator.of((short) 1)) : ShortStream.of((short) 1);
            assertReleased(source.flatmapToObj(value -> mapped), mapped);
        }
    }

    @Test
    void intReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<Integer> mapped = new TrackedList<>(7);
            final IntStream source = iteratorSource ? IntStream.of(IntIterator.of(1)) : IntStream.of(1);
            assertReleased(source.flatmap(value -> mapped), mapped);
        }
    }

    @Test
    void intToObjectReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<String> mapped = new TrackedList<>("mapped");
            final IntStream source = iteratorSource ? IntStream.of(IntIterator.of(1)) : IntStream.of(1);
            assertReleased(source.flatmapToObj(value -> mapped), mapped);
        }
    }

    @Test
    void longReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<Long> mapped = new TrackedList<>((long) 7);
            final LongStream source = iteratorSource ? LongStream.of(LongIterator.of((long) 1)) : LongStream.of((long) 1);
            assertReleased(source.flatmap(value -> mapped), mapped);
        }
    }

    @Test
    void longToObjectReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<String> mapped = new TrackedList<>("mapped");
            final LongStream source = iteratorSource ? LongStream.of(LongIterator.of((long) 1)) : LongStream.of((long) 1);
            assertReleased(source.flatmapToObj(value -> mapped), mapped);
        }
    }

    @Test
    void floatReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<Float> mapped = new TrackedList<>((float) 7);
            final FloatStream source = iteratorSource ? FloatStream.of(FloatIterator.of((float) 1)) : FloatStream.of((float) 1);
            assertReleased(source.flatmap(value -> mapped), mapped);
        }
    }

    @Test
    void floatToObjectReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<String> mapped = new TrackedList<>("mapped");
            final FloatStream source = iteratorSource ? FloatStream.of(FloatIterator.of((float) 1)) : FloatStream.of((float) 1);
            assertReleased(source.flatmapToObj(value -> mapped), mapped);
        }
    }

    @Test
    void doubleReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<Double> mapped = new TrackedList<>((double) 7);
            final DoubleStream source = iteratorSource ? DoubleStream.of(DoubleIterator.of((double) 1)) : DoubleStream.of((double) 1);
            assertReleased(source.flatmap(value -> mapped), mapped);
        }
    }

    @Test
    void doubleToObjectReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<String> mapped = new TrackedList<>("mapped");
            final DoubleStream source = iteratorSource ? DoubleStream.of(DoubleIterator.of((double) 1)) : DoubleStream.of((double) 1);
            assertReleased(source.flatmapToObj(value -> mapped), mapped);
        }
    }

    @Test
    void objectReleasesExhaustedCollection() throws Exception {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            final TrackedList<String> mapped = new TrackedList<>("mapped");
            final Stream<Integer> source = iteratorSource ? Stream.of(List.of(1).iterator()) : Stream.of(1);
            assertReleased(source.flatmap(value -> mapped), mapped);
        }
    }
}
