package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.u.OptionalShort;


public class ShortStream200Test extends TestBase {

    private ShortStream emptyStream;
    private ShortStream singleElementStream;
    private ShortStream singleElementStream2;
    private ShortStream multiElementStream;
    private ShortStream multiElementStream2;
    private ShortStream multiElementStream3;
    private ShortStream multiElementStream4;
    private ShortStream multiElementStream5;
    private ShortStream duplicateElementStream;
    private ShortStream sortedStream;

    // This method is a placeholder as requested, but we will use createShortStream() for actual setup.
    ShortStream createShortStream(short... data) {
        return ShortStream.of(data);
    }

    ShortStream createShortStream(short[] data, int fromIndex, int toIndex) {
        return ShortStream.of(data, fromIndex, toIndex);
    }

    ShortStream createShortStream(Short[] data) {
        return ShortStream.of(data);
    }

    ShortStream createShortStream(Short[] data, int fromIndex, int toIndex) {
        return ShortStream.of(data, fromIndex, toIndex);
    }

    ShortStream createShortStream(Collection<Short> data) {
        return ShortStream.of(data);
    }

    ShortStream createShortStream(ShortIterator data) {
        return ShortStream.of(data);
    }

    ShortStream createShortStream(ShortBuffer data) {
        return ShortStream.of(data);
    }

    @BeforeEach
    public void setUp() {
        emptyStream = createShortStream(new short[] {});
        singleElementStream = createShortStream(new short[] { 5 });
        singleElementStream2 = createShortStream(new short[] { 5 });
        multiElementStream = createShortStream(new short[] { 1, 2, 3, 4, 5 });
        multiElementStream2 = createShortStream(new short[] { 1, 2, 3, 4, 5 });
        multiElementStream3 = createShortStream(new short[] { 1, 2, 3, 4, 5 });
        multiElementStream4 = createShortStream(new short[] { 1, 2, 3, 4, 5 });
        multiElementStream5 = createShortStream(new short[] { 1, 2, 3, 4, 5 });
        duplicateElementStream = createShortStream(new short[] { 1, 2, 2, 3, 3, 3 });
        sortedStream = createShortStream(new short[] { 1, 2, 3, 4, 5, 6 });
    }

    @Nested
    @DisplayName("Static Factory Methods")
    public class FactoryMethods {

        @Test
        public void testEmpty() {
            assertEquals(0, ShortStream.empty().count());
        }

        @Test
        public void testDefer() {
            AtomicBoolean supplierCalled = new AtomicBoolean(false);
            Supplier<ShortStream> supplier = () -> {
                supplierCalled.set(true);
                return createShortStream((short) 1, (short) 2);
            };
            ShortStream stream = ShortStream.defer(supplier);
            assertFalse(supplierCalled.get(), "Supplier should not be called before terminal operation");
            assertEquals(2, stream.count());
            assertTrue(supplierCalled.get(), "Supplier should be called after terminal operation");
        }

        @Test
        public void testOfNullable() {
            assertEquals(1, ShortStream.ofNullable((short) 1).count());
            assertEquals(0, ShortStream.ofNullable(null).count());
        }

        @Test
        public void testOfVarargs() {
            assertEquals(0, createShortStream().count());
            assertArrayEquals(new short[] { 1, 2, 3 }, createShortStream((short) 1, (short) 2, (short) 3).toArray());
        }

        @Test
        public void testOfArrayWithRange() {
            short[] arr = { 1, 2, 3, 4, 5 };
            assertArrayEquals(new short[] { 2, 3, 4 }, createShortStream(arr, 1, 4).toArray());
            assertEquals(0, createShortStream(arr, 1, 1).count());
            assertThrows(IndexOutOfBoundsException.class, () -> createShortStream(arr, -1, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> createShortStream(arr, 0, 6));
            assertThrows(IndexOutOfBoundsException.class, () -> createShortStream(arr, 3, 2));
        }

        @Test
        public void testOfBoxedArray() {
            assertArrayEquals(new short[] { 1, 2 }, createShortStream(new Short[] { 1, 2 }).toArray());
            assertEquals(0, createShortStream(new Short[] {}).count());
        }

        @Test
        public void testOfCollection() {
            assertArrayEquals(new short[] { 1, 2 }, createShortStream(List.of((short) 1, (short) 2)).toArray());
            assertEquals(0, createShortStream(new ArrayList<>()).count());
        }

        @Test
        public void testOfIterator() {
            ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
            assertArrayEquals(new short[] { 1, 2, 3 }, createShortStream(iter).toArray());
        }

        @Test
        public void testOfBuffer() {
            ShortBuffer buffer = ShortBuffer.wrap(new short[] { 1, 2, 3, 4, 5 });
            buffer.position(1).limit(4);
            assertArrayEquals(new short[] { 2, 3, 4 }, createShortStream(buffer).toArray());
        }

        @Test
        public void testFlatten2D() {
            short[][] a = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, ShortStream.flatten(a).toArray());
            assertArrayEquals(new short[] { 1, 3, 6, 2, 4, 5 }, ShortStream.flatten(a, true).toArray());
        }

        @Test
        public void testFlatten3D() {
            short[][][] a = { { { 1 }, { 2, 3 } }, { { 4, 5, 6 } } };
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, ShortStream.flatten(a).toArray());
        }

        @Test
        public void testRange() {
            assertArrayEquals(new short[] { 2, 3, 4 }, ShortStream.range((short) 2, (short) 5).toArray());
            assertEquals(0, ShortStream.range((short) 5, (short) 2).count());
            assertEquals(0, ShortStream.range((short) 5, (short) 5).count());
        }

        @Test
        public void testRangeWithStep() {
            assertArrayEquals(new short[] { 2, 4, 6 }, ShortStream.range((short) 2, (short) 8, (short) 2).toArray());
            assertArrayEquals(new short[] { 8, 6, 4 }, ShortStream.range((short) 8, (short) 2, (short) -2).toArray());
            assertThrows(IllegalArgumentException.class, () -> ShortStream.range((short) 1, (short) 5, (short) 0));
        }

        @Test
        public void testRangeClosed() {
            assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.rangeClosed((short) 2, (short) 5).toArray());
            assertEquals(0, ShortStream.rangeClosed((short) 5, (short) 2).count());
            assertArrayEquals(new short[] { 5 }, ShortStream.rangeClosed((short) 5, (short) 5).toArray());
        }

        @Test
        public void testRepeat() {
            assertArrayEquals(new short[] { 7, 7, 7 }, ShortStream.repeat((short) 7, 3).toArray());
            assertEquals(0, ShortStream.repeat((short) 7, 0).count());
            assertThrows(IllegalArgumentException.class, () -> ShortStream.repeat((short) 7, -1));
        }

        @Test
        public void testGenerate() {
            AtomicInteger i = new AtomicInteger(0);
            short[] result = ShortStream.generate(() -> (short) i.getAndIncrement()).limit(5).toArray();
            assertArrayEquals(new short[] { 0, 1, 2, 3, 4 }, result);
        }

        @Test
        public void testIterateWithPredicate() {
            short[] result = ShortStream.iterate((short) 1, i -> i < 10, i -> (short) (i * 2)).toArray();
            assertArrayEquals(new short[] { 1, 2, 4, 8 }, result);
        }

        @Test
        public void testIterateUnaryOperator() {
            short[] result = ShortStream.iterate((short) 1, i -> (short) (i + 1)).limit(5).toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
        }

        @Test
        public void testConcat() {
            ShortStream s1 = createShortStream((short) 1, (short) 2);
            ShortStream s2 = createShortStream((short) 3, (short) 4);
            ShortStream s3 = createShortStream((short) 5, (short) 6);
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, ShortStream.concat(s1, s2, s3).toArray());
        }

        @Test
        public void testZip() {
            short[] a = { 1, 2, 3 };
            short[] b = { 4, 5, 6 };
            short[] result = ShortStream.zip(a, b, (x, y) -> (short) (x + y)).toArray();
            assertArrayEquals(new short[] { 5, 7, 9 }, result);
        }

        @Test
        public void testZipWithDefault() {
            short[] a = { 1, 2 };
            short[] b = { 4, 5, 6, 7 };
            short[] result = ShortStream.zip(a, b, (short) 99, (short) 100, (x, y) -> (short) (x + y)).toArray();
            assertArrayEquals(new short[] { 5, 7, 105, 106 }, result);
        }

        @Test
        public void testMerge() {
            short[] a = { 1, 3, 5 };
            short[] b = { 2, 4, 6 };
            short[] result = ShortStream
                    .merge(a, b,
                            (x, y) -> N.compare(x, y) <= 0 ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result);
        }
    }

    @Nested
    @DisplayName("Intermediate Operations")
    public class IntermediateOps {

        @Test
        public void testMap() {
            short[] result = multiElementStream.map(x -> (short) (x * x)).toArray();
            assertArrayEquals(new short[] { 1, 4, 9, 16, 25 }, result);
        }

        @Test
        public void testMapToInt() {
            int[] result = multiElementStream.mapToInt(x -> x).toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
        }

        @Test
        public void testMapToObj() {
            String[] result = multiElementStream.mapToObj(x -> "v" + x).toArray(String[]::new);
            assertArrayEquals(new String[] { "v1", "v2", "v3", "v4", "v5" }, result);
        }

        @Test
        public void testFlatMap() {
            short[] result = createShortStream((short) 1, (short) 3).flatMap(x -> createShortStream(x, (short) (x + 1))).toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
        }

        @Test
        public void testFilter() {
            short[] result = multiElementStream.filter(x -> x % 2 == 0).toArray();
            assertArrayEquals(new short[] { 2, 4 }, result);
        }

        @Test
        public void testDistinct() {
            short[] result = duplicateElementStream.distinct().toArray();
            assertArrayEquals(new short[] { 1, 2, 3 }, result);
        }

        @Test
        public void testSorted() {
            short[] result = createShortStream((short) 3, (short) 1, (short) 4, (short) 2).sorted().toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
        }

        @Test
        public void testReverseSorted() {
            short[] result = createShortStream((short) 3, (short) 1, (short) 4, (short) 2).reverseSorted().toArray();
            assertArrayEquals(new short[] { 4, 3, 2, 1 }, result);
        }

        @Test
        public void testPeek() {
            List<Short> peeked = new ArrayList<>();
            short[] result = multiElementStream.peek(peeked::add).toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
            assertEquals(List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), peeked);
        }

        @Test
        public void testLimit() {
            assertArrayEquals(new short[] { 1, 2, 3 }, multiElementStream.limit(3).toArray());
            assertEquals(5, multiElementStream2.limit(10).count());
            assertEquals(0, multiElementStream3.limit(0).count());
        }

        @Test
        public void testSkip() {
            assertArrayEquals(new short[] { 4, 5 }, multiElementStream.skip(3).toArray());
            assertEquals(0, multiElementStream2.skip(10).count());
            assertEquals(5, multiElementStream3.skip(0).count());
        }

        @Test
        public void testTakeWhile() {
            short[] result = sortedStream.takeWhile(x -> x < 4).toArray();
            assertArrayEquals(new short[] { 1, 2, 3 }, result);
        }

        @Test
        public void testDropWhile() {
            short[] result = sortedStream.dropWhile(x -> x < 4).toArray();
            assertArrayEquals(new short[] { 4, 5, 6 }, result);
        }

        @Test
        public void testScan() {
            short[] result = multiElementStream.scan((a, b) -> (short) (a + b)).toArray();
            assertArrayEquals(new short[] { 1, 3, 6, 10, 15 }, result);
        }

        @Test
        public void testScanWithInitialValue() {
            short[] result = multiElementStream.scan((short) 10, (a, b) -> (short) (a + b)).toArray();
            assertArrayEquals(new short[] { 11, 13, 16, 20, 25 }, result);
        }

        @Test
        public void testCycled() {
            short[] result = createShortStream((short) 1, (short) 2).cycled().limit(5).toArray();
            assertArrayEquals(new short[] { 1, 2, 1, 2, 1 }, result);
        }

        @Test
        public void testPrepend() {
            short[] result = multiElementStream.prepend((short) 8, (short) 9).toArray();
            assertArrayEquals(new short[] { 8, 9, 1, 2, 3, 4, 5 }, result);
        }

        @Test
        public void testAppend() {
            short[] result = multiElementStream.append((short) 8, (short) 9).toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 8, 9 }, result);
        }

        @Test
        public void testAppendIfEmpty() {
            short[] ifEmpty = { 9, 8, 7 };
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, multiElementStream.appendIfEmpty(ifEmpty).toArray());
            assertArrayEquals(ifEmpty, emptyStream.appendIfEmpty(ifEmpty).toArray());
        }

        @Test
        public void testTop() {
            ShortStream stream = createShortStream((short) 5, (short) 2, (short) 8, (short) 1, (short) 9);
            short[] result = stream.top(3).sorted().toArray();
            assertArrayEquals(new short[] { 5, 8, 9 }, result);
        }

        @Test
        public void testBoxed() {
            List<Short> result = multiElementStream.boxed().toList();
            assertEquals(List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), result);
        }
    }

    @Nested
    @DisplayName("Terminal Operations")
    public class TerminalOps {

        @Test
        public void testForEach() {
            List<Short> list = new ArrayList<>();
            multiElementStream.forEach(list::add);
            assertEquals(List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), list);
        }

        @Test
        public void testForEachIndexed() {
            List<String> list = new ArrayList<>();
            multiElementStream.forEachIndexed((idx, val) -> list.add(idx + ":" + val));
            assertEquals(List.of("0:1", "1:2", "2:3", "3:4", "4:5"), list);
        }

        @Test
        public void testToArray() {
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, multiElementStream.toArray());
            assertArrayEquals(new short[] {}, emptyStream.toArray());
        }

        @Test
        public void testToShortList() {
            ShortList list = multiElementStream.toShortList();
            assertEquals(5, list.size());
            assertEquals((short) 3, list.get(2));
        }

        @Test
        public void testReduceWithIdentity() {
            short sum = multiElementStream.reduce((short) 0, (a, b) -> (short) (a + b));
            assertEquals(15, sum);
            short product = multiElementStream2.reduce((short) 1, (a, b) -> (short) (a * b));
            assertEquals(120, product);
        }

        @Test
        public void testReduce() {
            OptionalShort sum = multiElementStream.reduce((a, b) -> (short) (a + b));
            assertTrue(sum.isPresent());
            assertEquals((short) 15, sum.get());
            assertFalse(emptyStream.reduce((a, b) -> (short) (a + b)).isPresent());
        }

        @Test
        public void testCollect() {
            List<Short> list = multiElementStream.collect(ArrayList::new, (l, e) -> l.add(e));
            assertEquals(List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), list);
        }

        @Test
        public void testToMap() {
            Map<String, Short> map = multiElementStream.toMap(s -> "k" + s, s -> s);
            assertEquals(5, map.size());
            assertEquals((short) 3, map.get("k3"));
        }

        @Test
        public void testSum() {
            assertEquals(15, multiElementStream.sum());
            assertEquals(0, emptyStream.sum());
        }

        @Test
        public void testAverage() {
            assertTrue(multiElementStream.average().isPresent());
            assertEquals(3.0, multiElementStream2.average().getAsDouble(), 0.001);
            assertFalse(emptyStream.average().isPresent());
        }

        @Test
        public void testSummarize() {
            ShortSummaryStatistics stats = multiElementStream.summarize();
            assertEquals(5, stats.getCount());
            assertEquals(15, stats.getSum());
            assertEquals(1, stats.getMin());
            assertEquals(5, stats.getMax());
            assertEquals(3.0, stats.getAverage(), 0.001);
        }

        @Test
        public void testCount() {
            assertEquals(5, multiElementStream.count());
            assertEquals(0, emptyStream.count());
        }

        @Test
        public void testAnyMatch() {
            assertTrue(multiElementStream.anyMatch(x -> x == 3));
            assertFalse(multiElementStream2.anyMatch(x -> x == 10));
            assertFalse(emptyStream.anyMatch(x -> true));
        }

        @Test
        public void testAllMatch() {
            assertTrue(multiElementStream.allMatch(x -> x > 0));
            assertFalse(multiElementStream2.allMatch(x -> x > 3));
            assertTrue(emptyStream.allMatch(x -> false));
        }

        @Test
        public void testNoneMatch() {
            assertTrue(multiElementStream.noneMatch(x -> x == 10));
            assertFalse(multiElementStream2.noneMatch(x -> x == 3));
            assertTrue(emptyStream.noneMatch(x -> true));
        }

        @Test
        public void testFirst() {
            assertTrue(multiElementStream.first().isPresent());
            assertEquals((short) 1, multiElementStream2.first().get());
            assertFalse(emptyStream.first().isPresent());
        }

        @Test
        public void testLast() {
            assertTrue(multiElementStream.last().isPresent());
            assertEquals((short) 5, multiElementStream2.last().get());
            assertFalse(emptyStream.last().isPresent());
        }

        @Test
        public void testElementAt() {
            assertTrue(multiElementStream.elementAt(2).isPresent());
            assertEquals((short) 3, multiElementStream2.elementAt(2).get());
            assertFalse(multiElementStream3.elementAt(10).isPresent());
            assertFalse(emptyStream.elementAt(0).isPresent());
        }

        @Test
        public void testOnlyOne() {
            assertTrue(singleElementStream.onlyOne().isPresent());
            assertEquals((short) 5, singleElementStream2.onlyOne().get());
            assertFalse(emptyStream.onlyOne().isPresent());
            assertThrows(com.landawn.abacus.exception.TooManyElementsException.class, () -> multiElementStream.onlyOne());
        }

        @Test
        public void testFindFirst() {
            OptionalShort result = multiElementStream.findFirst(x -> x > 3);
            assertTrue(result.isPresent());
            assertEquals((short) 4, result.get());
        }

        @Test
        public void testMin() {
            assertTrue(multiElementStream.min().isPresent());
            assertEquals((short) 1, multiElementStream2.min().get());
            assertFalse(emptyStream.min().isPresent());
        }

        @Test
        public void testMax() {
            assertTrue(multiElementStream.max().isPresent());
            assertEquals((short) 5, multiElementStream2.max().get());
            assertFalse(emptyStream.max().isPresent());
        }

        @Test
        public void testKthLargest() {
            assertTrue(multiElementStream.kthLargest(1).isPresent());
            assertEquals((short) 5, multiElementStream2.kthLargest(1).get());
            assertEquals((short) 3, multiElementStream3.kthLargest(3).get());
            assertFalse(multiElementStream4.kthLargest(10).isPresent());
            assertThrows(IllegalArgumentException.class, () -> multiElementStream5.kthLargest(0));
        }

        @Test
        public void testOnClose() {
            AtomicBoolean closed = new AtomicBoolean(false);
            try (ShortStream stream = multiElementStream.onClose(() -> closed.set(true))) {
                stream.count();
            }
            assertTrue(closed.get(), "onClose handler should be called when stream is closed");
        }
    }

    @Nested
    @DisplayName("Inherited from BaseStream")
    public class BaseStreamMethods {

        @Test
        public void testReversed() {
            assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, multiElementStream.reversed().toArray());
        }

        @Test
        public void testRotated() {
            assertArrayEquals(new short[] { 4, 5, 1, 2, 3 }, multiElementStream.rotated(2).toArray());
            assertArrayEquals(new short[] { 2, 3, 4, 5, 1 }, multiElementStream2.rotated(-1).toArray());
        }

        @Test
        public void testShuffled() {
            // Use a fixed seed for deterministic shuffling
            short[] result = multiElementStream.shuffled(new Random(42)).toArray();
            short[] original = multiElementStream2.toArray();
            Arrays.sort(result);
            Arrays.sort(original);
            assertArrayEquals(original, result, "Shuffled stream should contain the same elements");
        }

        @Test
        public void testIntersection() {
            List<Short> other = List.of((short) 2, (short) 2, (short) 4, (short) 6);
            ShortStream stream = createShortStream((short) 1, (short) 2, (short) 4, (short) 4);
            short[] result = stream.intersection(other).toArray();
            assertArrayEquals(new short[] { 2, 4 }, result);
        }

        @Test
        public void testDifference() {
            List<Short> other = List.of((short) 2, (short) 4, (short) 6);
            short[] result = multiElementStream.difference(other).toArray();
            assertArrayEquals(new short[] { 1, 3, 5 }, result);
        }

        @Test
        public void testSymmetricDifference() {
            List<Short> other = List.of((short) 4, (short) 5, (short) 6, (short) 7);
            short[] result = multiElementStream.symmetricDifference(other).sorted().toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 6, 7 }, result);
        }

        @Test
        public void testJoin() {
            assertEquals("1, 2, 3, 4, 5", multiElementStream.join(", "));
            assertEquals("[1-2-3-4-5]", createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).join("-", "[", "]"));
        }

    }
}
