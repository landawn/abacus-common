package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("unit")
class InstructionCoverageLiftStreamTest extends TestBase {

    @Test
    void flattmapIsUnsupported() {
        final IntStream stream = IntStream.of(1, 2);

        final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> stream.flattmap(value -> List.of(value)));

        assertTrue(ex.getMessage().contains("flattmap"));
    }

    @Test
    void bufferedIteratorDrainsAndReusesAnExistingBuffer() {
        final ObjIteratorEx.BufferedIterator<String> buffered = Stream.buffered(List.of("a", "b").iterator());

        try {
            assertEquals("a", buffered.next());
            assertEquals("b", buffered.next());
            assertFalse(buffered.hasNext());
            assertSame(buffered, Stream.buffered(buffered));
        } finally {
            buffered.closeResource();
        }
    }

    @Test
    void newBoundedQueueUsesArrayThenLinkedStorage() {
        final BlockingQueue<String> small = Stream.newBoundedQueue(2);
        final BlockingQueue<String> large = Stream.newBoundedQueue(Integer.MAX_VALUE);

        assertEquals(2, small.remainingCapacity());
        assertTrue(large.remainingCapacity() > 2);
    }

    @Test
    void failureAtomicAdvanceFollowsTheIteratorType() {
        final Iterator<String> plain = List.of("a").iterator();

        assertFalse(ObjIteratorEx.supportsFailureAtomicAdvance(plain));
        assertFalse(ObjIteratorEx.supportsFailureAtomicAdvance(null));

        final ObjIteratorEx<String> objects = ObjIteratorEx.of("a");
        final CharIteratorEx chars = CharIteratorEx.of('a');
        final ByteIteratorEx bytes = ByteIteratorEx.of((byte) 1);
        final ShortIteratorEx shorts = ShortIteratorEx.of((short) 1);
        final IntIteratorEx ints = IntIteratorEx.of(1);
        final LongIteratorEx longs = LongIteratorEx.of(1L);
        final FloatIteratorEx floats = FloatIteratorEx.of(1F);
        final DoubleIteratorEx doubles = DoubleIteratorEx.of(1D);

        assertEquals(objects.supportsFailureAtomicAdvance(), ObjIteratorEx.supportsFailureAtomicAdvance(objects));
        assertEquals(chars.supportsFailureAtomicAdvance(), ObjIteratorEx.supportsFailureAtomicAdvance(chars));
        assertEquals(bytes.supportsFailureAtomicAdvance(), ObjIteratorEx.supportsFailureAtomicAdvance(bytes));
        assertEquals(shorts.supportsFailureAtomicAdvance(), ObjIteratorEx.supportsFailureAtomicAdvance(shorts));
        assertEquals(ints.supportsFailureAtomicAdvance(), ObjIteratorEx.supportsFailureAtomicAdvance(ints));
        assertEquals(longs.supportsFailureAtomicAdvance(), ObjIteratorEx.supportsFailureAtomicAdvance(longs));
        assertEquals(floats.supportsFailureAtomicAdvance(), ObjIteratorEx.supportsFailureAtomicAdvance(floats));
        assertEquals(doubles.supportsFailureAtomicAdvance(), ObjIteratorEx.supportsFailureAtomicAdvance(doubles));
        assertTrue(objects.supportsFailureAtomicAdvance());
    }

    @Test
    void parallelPartialMapsAndThrowingMapMultiUseTheAbstractBodies() {
        assertEquals(List.of(2), Stream.of(1, 2).parallel().mapPartial(value -> value > 1 ? Optional.of(value) : Optional.empty()).toList());
        assertArrayEquals(new int[] { 2 },
                Stream.of(1, 2).parallel().mapPartialToInt(value -> value > 1 ? OptionalInt.of(value) : OptionalInt.empty()).toArray());
        assertArrayEquals(new long[] { 2L },
                Stream.of(1, 2).parallel().mapPartialToLong(value -> value > 1 ? OptionalLong.of(value) : OptionalLong.empty()).toArray());
        assertArrayEquals(new double[] { 2D },
                Stream.of(1, 2).parallel().mapPartialToDouble(value -> value > 1D ? OptionalDouble.of(value) : OptionalDouble.empty()).toArray(), 0D);
        assertEquals(List.of(2),
                Stream.of(1, 2).parallel().mapPartialJdk(value -> value > 1 ? java.util.Optional.of(value) : java.util.Optional.empty()).toList());
        assertArrayEquals(new int[] { 2 },
                Stream.of(1, 2).parallel().mapPartialToIntJdk(value -> value > 1 ? java.util.OptionalInt.of(value) : java.util.OptionalInt.empty()).toArray());
        assertArrayEquals(new long[] { 2L },
                Stream.of(1L, 2L)
                        .parallel()
                        .mapPartialToLongJdk(value -> value > 1L ? java.util.OptionalLong.of(value) : java.util.OptionalLong.empty())
                        .toArray());
        assertArrayEquals(new double[] { 2D },
                Stream.of(1D, 2D)
                        .parallel()
                        .mapPartialToDoubleJdk(value -> value > 1D ? java.util.OptionalDouble.of(value) : java.util.OptionalDouble.empty())
                        .toArray(),
                0D);

        assertEquals(2L, Stream.of("a").parallel().mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value);
        }).count());
    }

    @Test
    void mapMultiDropsPartialOutputWhenTheMapperFailsAndResetsALargeBuffer() {
        assertThrows(IllegalStateException.class, () -> Stream.of("a").mapMulti((value, consumer) -> {
            consumer.accept(value);
            throw new IllegalStateException("stop");
        }).toList());
        assertThrows(IllegalStateException.class, () -> Stream.of(1).mapMultiToInt((value, consumer) -> {
            consumer.accept(value);
            throw new IllegalStateException("stop");
        }).toArray());
        assertThrows(IllegalStateException.class, () -> Stream.of(1L).mapMultiToLong((value, consumer) -> {
            consumer.accept(value);
            throw new IllegalStateException("stop");
        }).toArray());
        assertThrows(IllegalStateException.class, () -> Stream.of(1D).mapMultiToDouble((value, consumer) -> {
            consumer.accept(value);
            throw new IllegalStateException("stop");
        }).toArray());

        final int fanOut = StreamBase.MAX_BUFFERED_SIZE + 1;
        assertEquals(fanOut, Stream.of(1).mapMultiToInt((value, consumer) -> {
            for (int i = 0; i < fanOut; i++) {
                consumer.accept(1);
            }
        }).sum());
        assertEquals((long) fanOut, Stream.of(1L).mapMultiToLong((value, consumer) -> {
            for (int i = 0; i < fanOut; i++) {
                consumer.accept(1L);
            }
        }).sum());
        assertEquals((double) fanOut, Stream.of(1D).mapMultiToDouble((value, consumer) -> {
            for (int i = 0; i < fanOut; i++) {
                consumer.accept(1D);
            }
        }).sum(), 0D);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void defaultCollectingCombinerMergesEachSupportedContainer() {
        final BiConsumer combiner = StreamBase.collectingCombiner;

        final List<String> list = new java.util.ArrayList<>(List.of("a"));
        combiner.accept(list, List.of("b"));
        assertEquals(List.of("a", "b"), list);

        final Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        final Map<String, String> more = new LinkedHashMap<>();
        more.put("b", "2");
        combiner.accept(map, more);
        assertEquals("2", map.get("b"));

        final StringBuilder builder = new StringBuilder("a");
        combiner.accept(builder, new StringBuilder("b"));
        assertEquals("ab", builder.toString());

        final Multiset<String> multiset = Multiset.of("a");
        combiner.accept(multiset, Multiset.of("b"));
        assertEquals(2, multiset.size());

        final ListMultimap<String, String> multimap = N.newListMultimap();
        multimap.put("a", "1");
        final ListMultimap<String, String> extra = N.newListMultimap();
        extra.put("b", "2");
        combiner.accept(multimap, extra);
        assertEquals(List.of("2"), multimap.get("b"));

        final BooleanList booleans = BooleanList.of(true);
        combiner.accept(booleans, BooleanList.of(false));
        assertEquals(2, booleans.size());
        final CharList chars = CharList.of('a');
        combiner.accept(chars, CharList.of('b'));
        assertEquals(2, chars.size());
        final ByteList bytes = ByteList.of((byte) 1);
        combiner.accept(bytes, ByteList.of((byte) 2));
        assertEquals(2, bytes.size());
        final ShortList shorts = ShortList.of((short) 1);
        combiner.accept(shorts, ShortList.of((short) 2));
        assertEquals(2, shorts.size());
        final IntList ints = IntList.of(1);
        combiner.accept(ints, IntList.of(2));
        assertEquals(2, ints.size());
        final LongList longs = LongList.of(1L);
        combiner.accept(longs, LongList.of(2L));
        assertEquals(2, longs.size());
        final FloatList floats = FloatList.of(1F);
        combiner.accept(floats, FloatList.of(2F));
        assertEquals(2, floats.size());
        final DoubleList doubles = DoubleList.of(1D);
        combiner.accept(doubles, DoubleList.of(2D));
        assertEquals(2, doubles.size());

        assertThrows(IllegalArgumentException.class, () -> combiner.accept("a", "b"));
    }
}
