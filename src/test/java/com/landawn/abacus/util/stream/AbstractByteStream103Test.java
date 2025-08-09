package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteTriPredicate;

public class AbstractByteStream103Test extends TestBase {

    private ByteStream stream;
    private byte[] testData;

    // This method needs to be implemented by a concrete test class to provide a ByteStream instance.
    // For example, in ArrayByteStreamTest, it would return new ArrayByteStream(a);
    // In IteratorByteStreamTest, it would return new IteratorByteStream(ByteIterator.of(a));
    protected ByteStream createByteStream(byte... a) {
        return ByteStream.of(a).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(byte[] a, int fromIndex, int toIndex) {
        return ByteStream.of(a, fromIndex, toIndex).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Byte[] a) {
        return ByteStream.of(a).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Byte[] a, int fromIndex, int toIndex) {
        return ByteStream.of(a, fromIndex, toIndex).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Collection<Byte> coll) {
        return ByteStream.of(coll.toArray(new Byte[coll.size()])).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(ByteIterator iter) {
        return iter == null ? ByteStream.empty() : ByteStream.of(iter.toArray()).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(ByteBuffer buff) {
        return ByteStream.of(buff).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(File file) {
        return ByteStream.of(file).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(InputStream is) {
        return ByteStream.of(is).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(InputStream is, boolean closeInputStreamOnClose) {
        return ByteStream.of(is, closeInputStreamOnClose).map(e -> (byte) (e + 0));
    }

    @BeforeEach
    public void setUp() {
        testData = new byte[] { 1, 2, 3, 4, 5 };
        stream = createByteStream(testData);
    }

    // Additional edge cases for skip with action
    @Test
    public void testSkipWithActionZeroElements() {
        List<Byte> skipped = new ArrayList<>();
        ByteConsumer action = skipped::add;
        ByteStream result = stream.skip(0, action);
        assertArrayEquals(testData, result.toArray());
        assertEquals(0, skipped.size());
    }

    @Test
    public void testSkipWithActionMoreThanSize() {
        List<Byte> skipped = new ArrayList<>();
        ByteConsumer action = skipped::add;
        ByteStream result = stream.skip(10, action);
        assertArrayEquals(new byte[] {}, result.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), skipped);
    }

    @Test
    public void testSkipWithActionNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> stream.skip(-1, value -> {
        }));
    }

    @Test
    public void testSkipWithActionNullAction() {
        assertThrows(IllegalArgumentException.class, () -> stream.skip(2, null));
    }

    // Parallel stream scenarios for skip with action
    @Test
    public void testSkipWithActionParallel() {
        List<Byte> skipped = new ArrayList<>();
        ByteConsumer action = value -> {
            synchronized (skipped) {
                skipped.add(value);
            }
        };
        byte[] resultArray = stream.parallel().skip(3, action).sorted().toArray();

        // Results should be consistent even in parallel
        assertEquals(2, resultArray.length);
        // assertTrue(resultArray[0] == 4 || resultArray[0] == 5);
        // assertTrue(resultArray[1] == 4 || resultArray[1] == 5);
        assertEquals(3, skipped.size());
    }

    // Edge cases for collapse methods
    @Test
    public void testCollapseEmptyStream() {
        stream = ByteStream.empty();
        ByteBiPredicate collapsible = (a, b) -> true;
        Stream<ByteList> result = stream.collapse(collapsible);
        assertEquals(0, result.count());
    }

    @Test
    public void testCollapseSingleElement() {
        stream = createByteStream((byte) 42);
        ByteBiPredicate collapsible = (a, b) -> true;
        Stream<ByteList> result = stream.collapse(collapsible);
        List<ByteList> lists = result.toList();
        assertEquals(1, lists.size());
        assertArrayEquals(new byte[] { 42 }, lists.get(0).toArray());
    }

    @Test
    public void testCollapseNonConsecutive() {
        byte[] data = { 1, 3, 5, 7, 9 };
        stream = createByteStream(data);
        ByteBiPredicate collapsible = (a, b) -> b - a == 2;
        Stream<ByteList> result = stream.collapse(collapsible);
        List<ByteList> lists = result.toList();
        assertEquals(1, lists.size());
        assertArrayEquals(data, lists.get(0).toArray());
    }

    @Test
    public void testCollapseWithBinaryOperatorGroups() {
        byte[] data = { 1, 2, 3, 10, 11, 20 };
        stream = createByteStream(data);
        ByteBiPredicate collapsible = (a, b) -> Math.abs(b - a) <= 1;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) Math.max(a, b);
        ByteStream result = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 3, 11, 20 }, result.toArray());
    }

    @Test
    public void testCollapseWithTriPredicateComplex() {
        byte[] data = { 1, 2, 4, 8, 16, 17, 32 };
        stream = createByteStream(data);
        ByteTriPredicate collapsible = (first, prev, curr) -> curr == prev * 2 || curr == prev + 1;
        ByteBinaryOperator mergeFunction = (a, b) -> b; // Take last
        ByteStream result = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 17, 32 }, result.toArray());
    }

    // Edge cases for scan methods
    @Test
    public void testScanEmptyStream() {
        stream = ByteStream.empty();
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a + b);
        ByteStream result = stream.scan(accumulator);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testScanSingleElement() {
        stream = createByteStream((byte) 42);
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a * b);
        ByteStream result = stream.scan(accumulator);
        assertArrayEquals(new byte[] { 42 }, result.toArray());
    }

    @Test
    public void testScanWithInitNotIncluded() {
        byte init = 100;
        ByteBinaryOperator accumulator = (a, b) -> (byte) (a - b);
        ByteStream result = stream.scan(init, false, accumulator);
        // 100-1=99, 99-2=97, 97-3=94, 94-4=90, 90-5=85
        assertArrayEquals(new byte[] { 99, 97, 94, 90, 85 }, result.toArray());
    }

    // Edge cases for step
    @Test
    public void testStepGreaterThanSize() {
        ByteStream result = stream.step(10);
        assertArrayEquals(new byte[] { 1 }, result.toArray());
    }

    @Test
    public void testStepExactSize() {
        ByteStream result = stream.step(5);
        assertArrayEquals(new byte[] { 1 }, result.toArray());
    }

    // Edge cases for rotated
    @Test
    public void testRotatedZero() {
        ByteStream result = stream.rotated(0);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testRotatedFullCycle() {
        ByteStream result = stream.rotated(5);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testRotatedMultipleCycles() {
        ByteStream result = stream.rotated(12); // 12 % 5 = 2
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testRotatedEmptyStream() {
        stream = ByteStream.empty();
        ByteStream result = stream.rotated(5);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    // Edge cases for cycled
    @Test
    public void testCycledZeroRounds() {
        ByteStream result = stream.cycled(0);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testCycledOneRound() {
        ByteStream result = stream.cycled(1);
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testCycledEmptyStream() {
        stream = ByteStream.empty();
        ByteStream result = stream.cycled().limit(5);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testCycledNegativeRounds() {
        assertThrows(IllegalArgumentException.class, () -> stream.cycled(-1));
    }

    // Edge cases for intersection/difference/symmetricDifference
    @Test
    public void testIntersectionEmptyCollection() {
        ByteStream result = stream.intersection(new ArrayList<>());
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testIntersectionWithDuplicates() {
        byte[] data = { 1, 2, 2, 3, 3, 3 };
        stream = createByteStream(data);
        List<Byte> c = Arrays.asList((byte) 2, (byte) 2, (byte) 3);
        ByteStream result = stream.intersection(c);
        assertArrayEquals(new byte[] { 2, 2, 3 }, result.toArray());
    }

    @Test
    public void testDifferenceEmptyCollection() {
        ByteStream result = stream.difference(new ArrayList<>());
        assertArrayEquals(testData, result.toArray());
    }

    @Test
    public void testDifferenceWithDuplicates() {
        byte[] data = { 1, 2, 2, 3, 3, 3 };
        stream = createByteStream(data);
        List<Byte> c = Arrays.asList((byte) 2, (byte) 3, (byte) 3);
        ByteStream result = stream.difference(c);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        stream = ByteStream.empty();
        List<Byte> c = Arrays.asList((byte) 1, (byte) 2);
        ByteStream result = stream.symmetricDifference(c);
        assertArrayEquals(new byte[] { 1, 2 }, result.toArray());
    }

    // Test sorted on already sorted stream
    @Test
    public void testSortedAlreadySorted() {
        // The stream might be marked as sorted internally
        ByteStream sorted1 = stream.sorted();
        ByteStream sorted2 = sorted1.sorted(); // Should be optimized
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, sorted2.toArray());
    }

    // Test parallel scenarios
    @Test
    public void testReversedParallel() {
        ByteStream result = stream.parallel().reversed();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, result.toArray());
    }

    @Test
    public void testSortedParallel() {
        byte[] unsorted = { 5, 1, 4, 2, 3 };
        stream = createByteStream(unsorted);
        ByteStream result = stream.parallel().sorted();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    // Edge cases for rangeMap
    @Test
    public void testRangeMapEmptyStream() {
        stream = ByteStream.empty();
        ByteBiPredicate sameRange = (a, b) -> true;
        ByteBinaryOperator mapper = (a, b) -> a;
        ByteStream result = stream.rangeMap(sameRange, mapper);
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testRangeMapSingleElement() {
        stream = createByteStream((byte) 42);
        ByteBiPredicate sameRange = (a, b) -> true;
        ByteBinaryOperator mapper = (a, b) -> (byte) (a + b);
        ByteStream result = stream.rangeMap(sameRange, mapper);
        assertArrayEquals(new byte[] { 84 }, result.toArray()); // 42 + 42
    }

    @Test
    public void testRangeMapNoMatches() {
        ByteBiPredicate sameRange = (a, b) -> false; // Never group
        ByteBinaryOperator mapper = (a, b) -> a;
        ByteStream result = stream.rangeMap(sameRange, mapper);
        assertArrayEquals(testData, result.toArray()); // Each element mapped to itself
    }

    // Test advanced collapse scenarios
    @Test
    public void testCollapseAlternating() {
        byte[] data = { 1, 2, 1, 2, 1, 2 };
        stream = createByteStream(data);
        AtomicInteger counter = new AtomicInteger(0);
        ByteBiPredicate collapsible = (a, b) -> {
            counter.incrementAndGet();
            return b != a; // Collapse when different
        };
        Stream<ByteList> result = stream.collapse(collapsible);
        List<ByteList> lists = result.toList();
        assertEquals(1, lists.size());
        assertArrayEquals(new byte[] { 1, 2, 1, 2, 1, 2 }, lists.get(0).toArray());
    }

    // Test indexed with different stream sizes
    @Test
    public void testIndexedEmptyStream() {
        stream = ByteStream.empty();
        Stream<IndexedByte> result = stream.indexed();
        assertEquals(0, result.count());
    }

    @Test
    public void testIndexedSingleElement() {
        stream = createByteStream((byte) 42);
        Stream<IndexedByte> result = stream.indexed();
        List<IndexedByte> indexed = result.toList();
        assertEquals(1, indexed.size());
        assertEquals(0, indexed.get(0).index());
        assertEquals(42, indexed.get(0).value());
    }

    // Test shuffled with empty stream
    @Test
    public void testShuffledEmptyStream() {
        stream = ByteStream.empty();
        ByteStream result = stream.shuffled(new Random());
        assertArrayEquals(new byte[] {}, result.toArray());
    }

    @Test
    public void testShuffledNullRandom() {
        assertThrows(IllegalArgumentException.class, () -> stream.shuffled(null));
    }

    // Test collect edge cases
    @Test
    public void testCollectWithNullSupplier() {
        try {
            stream.collect(null, (list, value) -> {
            });
            fail("Should throw exception");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testCollectWithNullAccumulator() {
        try {
            stream.collect(ArrayList::new, null);
            fail("Should throw exception");
        } catch (Exception e) {
            // Expected
        }
    }

    // Test additional combinations
    @Test
    public void testChainedOperations() {
        ByteStream result = stream.filter(v -> v % 2 == 1) // 1, 3, 5
                .map(v -> (byte) (v * 2)) // 2, 6, 10
                .sorted() // 2, 6, 10
                .reversed(); // 10, 6, 2
        assertArrayEquals(new byte[] { 10, 6, 2 }, result.toArray());
    }

    @Test
    public void testComplexPipeline() {
        byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        stream = createByteStream(data);

        ByteStream result = stream.filter(v -> v % 2 == 0) // 2, 4, 6, 8, 10
                .step(2) // 2, 6, 10
                .scan((a, b) -> (byte) (a + b)) // 2, 8, 18
                .distinct(); // 2, 8, 18

        assertArrayEquals(new byte[] { 2, 8, 18 }, result.toArray());
    }
}
