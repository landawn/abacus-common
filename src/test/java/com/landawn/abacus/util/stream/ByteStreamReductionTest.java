package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;

/** ByteStream reduction contracts. */
public class ByteStreamReductionTest extends ByteStreamTestSupport {

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        createByteStream(new byte[] { 1, 2 }).acceptIfNotEmpty(s -> actionExecuted.set(true)).orElse(() -> fail("Else should not be called"));
        assertTrue(actionExecuted.get());

        actionExecuted.set(false);
        createByteStream(new byte[] {}).acceptIfNotEmpty(s -> fail("Action should not be called")).orElse(() -> actionExecuted.set(true));
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testAllMatch() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(stream.allMatch(b -> b > 0));

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(stream2.allMatch(b -> b > 2));

        ByteStream emptyStream = ByteStream.empty();
        assertTrue(emptyStream.allMatch(b -> b > 0));
    }

    @Test
    public void testAnyMatch() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(stream.anyMatch(b -> b > 2));

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(stream2.anyMatch(b -> b > 10));

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.anyMatch(b -> b > 0));
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Long> count = createByteStream(new byte[] { 1, 2, 3 }).applyIfNotEmpty(s -> s.count());
        assertTrue(count.isPresent());
        assertEquals(3L, count.get().longValue());

        count = createByteStream(new byte[] {}).applyIfNotEmpty(s -> s.count());
        assertFalse(count.isPresent());
    }

    // average

    @Test
    public void testAverage() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalDouble avg = stream.average();
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.getAsDouble(), 0.001);

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.average().isPresent());
    }

    @Test
    public void testAverage_EmptyStream() {
        OptionalDouble result = ByteStream.empty().average();
        assertFalse(result.isPresent());
    }

    @Test
    public void testAverage_largeStream_noOverflow() {
        // Regression: average() must not throw when the byte sum exceeds Integer.MAX_VALUE (#63).
        final int n = 17_000_000;
        final byte[] arr = new byte[n];
        java.util.Arrays.fill(arr, (byte) 127);
        org.junit.jupiter.api.Assertions.assertEquals(127.0, com.landawn.abacus.util.stream.ByteStream.of(arr).average().getAsDouble(), 0.0001);
    }

    @Test
    public void testCount() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertEquals(5, stream.count());

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.count());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertEquals(3, stream2.filter(b -> b > 2).count());
    }

    @Test
    public void testCount_EmptyStream() {
        assertEquals(0, ByteStream.empty().count());
    }

    // elementAt

    @Test
    public void testElementAt() {
        byteStream = createByteStream(new byte[] { 10, 20, 30 });
        OptionalByte result = byteStream.elementAt(1);
        assertTrue(result.isPresent());
        assertEquals((byte) 20, result.get());

        result = createByteStream(new byte[] { 10, 20, 30 }).elementAt(10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_FirstElement() {
        ByteStream stream = createByteStream((byte) 10, (byte) 20, (byte) 30);
        OptionalByte result = stream.elementAt(0);
        assertTrue(result.isPresent());
        assertEquals((byte) 10, result.get());
    }

    @Test
    public void testElementAt_NegativePosition() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IllegalArgumentException.class, () -> stream.elementAt(-1));
    }

    @Test
    public void testElementAt_OutOfBounds() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        OptionalByte result = stream.elementAt(5);
        assertFalse(result.isPresent());
    }

    // findAny

    @Test
    public void testFindAny() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findAny(b -> b > 2);
        assertTrue(result.isPresent());
        assertTrue(result.get() >= 3);

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        OptionalByte noMatch = stream2.findAny(b -> b > 10);
        assertFalse(noMatch.isPresent());
    }

    @Test
    public void testFindAny_NoPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate() {
        OptionalByte result = createByteStream((byte) 7, (byte) 8).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate_EmptyStream() {
        OptionalByte result = ByteStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyDefault() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.first();

        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    // findFirst

    @Test
    public void testFindFirst() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findFirst(b -> b > 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        OptionalByte noMatch = stream2.findFirst(b -> b > 10);
        assertFalse(noMatch.isPresent());
    }

    @Test
    public void testFindFirst_NoPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.findFirst();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());
    }

    @Test
    public void testFindFirstNoPredicate() {
        OptionalByte result = createByteStream((byte) 7, (byte) 8, (byte) 9).findFirst();
        assertTrue(result.isPresent());
        assertEquals((byte) 7, result.getAsByte());
    }

    @Test
    public void testFindFirstNoPredicate_EmptyStream() {
        OptionalByte result = ByteStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findFirst(b -> b > 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        ByteStream stream2 = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte notFound = stream2.findFirst(b -> b > 10);
        assertFalse(notFound.isPresent());
    }

    // findLast

    @Test
    public void testFindLast() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        OptionalByte result = stream.findLast(b -> b > 2);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        OptionalByte noMatch = stream2.findLast(b -> b > 10);
        assertFalse(noMatch.isPresent());
    }

    @Test
    public void testFindLast_NoMatch() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.findLast(b -> b > 100);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_WithPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findLast(b -> b < 3);
        assertTrue(result.isPresent());
        assertEquals((byte) 2, result.get());
    }

    @Test
    public void testFirst() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(1, stream.first().get());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.first().isPresent());
    }

    @Test
    public void testFirst_EmptyStream() {
        OptionalByte result = ByteStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testForEach() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> collected = new ArrayList<>();
        stream.forEach(collected::add);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), collected);
    }

    @Test
    public void testForEachIndexed() {
        ByteStream stream = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        List<String> collected = new ArrayList<>();
        stream.forEachIndexed((idx, b) -> collected.add(idx + ":" + b));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), collected);
    }

    @Test
    public void testForEachIndexed_Verify() {
        ByteStream stream = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<String> collected = new ArrayList<>();
        stream.forEachIndexed((idx, b) -> collected.add(idx + ":" + b));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), collected);
    }

    // kthLargest

    @Test
    public void testKthLargest() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2, (byte) 5);
        OptionalByte result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertFalse(stream2.kthLargest(5).isPresent());
    }

    @Test
    public void testKthLargest_First() {
        ByteStream stream = createByteStream((byte) 1, (byte) 3, (byte) 5, (byte) 2, (byte) 4);
        OptionalByte result = stream.kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals((byte) 5, result.get());
    }

    @Test
    public void testKthLargest_Second() {
        ByteStream stream = createByteStream((byte) 1, (byte) 3, (byte) 5, (byte) 2, (byte) 4);
        OptionalByte result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals((byte) 4, result.get());
    }

    @Test
    public void testLast() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream.last().get());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.last().isPresent());

        ByteStream stream2 = ByteStream.of((byte) 42);
        assertEquals(42, stream2.last().get());
    }

    @Test
    public void testLast_EmptyStream() {
        OptionalByte result = ByteStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2);
        OptionalByte max = stream.max();
        assertTrue(max.isPresent());
        assertEquals(4, max.get());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.max().isPresent());
    }

    @Test
    public void testMax_EmptyStream() {
        OptionalByte result = ByteStream.empty().max();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2);
        OptionalByte min = stream.min();
        assertTrue(min.isPresent());
        assertEquals(1, min.get());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.min().isPresent());
    }

    @Test
    public void testMin_EmptyStream() {
        OptionalByte result = ByteStream.empty().min();
        assertFalse(result.isPresent());
    }

    @Test
    public void testNoneMatch() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(stream.noneMatch(b -> b > 10));

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(stream2.noneMatch(b -> b > 2));

        ByteStream emptyStream = ByteStream.empty();
        assertTrue(emptyStream.noneMatch(b -> b > 0));
    }

    @Test
    public void testPercentiles() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10);
        Map<Percentage, Byte> percentiles = stream.percentiles().get();
        assertNotNull(percentiles);
        assertTrue(percentiles.size() > 0);
    }

    // reduce

    @Test
    public void testReduce() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte sum = stream.reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals(10, sum);

        ByteStream stream2 = ByteStream.of((byte) 5, (byte) 10);
        assertEquals(115, stream2.reduce((byte) 100, (a, b) -> (byte) (a + b)));
    }

    @Test
    public void testReduceIdentity() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        byte result = byteStream.reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 16, result);
    }

    @Test
    public void testReduceOptional() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals(6, result.get());

        ByteStream emptyStream = ByteStream.empty();
        OptionalByte emptyResult = emptyStream.reduce((a, b) -> (byte) (a + b));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testReduceWithIdentityOnEmpty() {
        byte result = ByteStream.empty().reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 10, result);
    }

    @Test
    public void testReduceWithoutIdentity() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals(10, result.get());

        ByteStream empty = ByteStream.empty();
        OptionalByte emptyResult = empty.reduce((a, b) -> (byte) (a + b));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testSum() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        int sum = stream.sum();
        assertEquals(10, sum);

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.sum());
    }

    @Test
    public void testSum_EmptyStream() {
        int result = ByteStream.empty().sum();
        assertEquals(0, result);
    }

    @Test
    public void testsummaryStatistics() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteSummaryStatistics stats = stream.summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummaryStatistics_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(3, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(3, stats.getMax());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertNotNull(result.left());
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
    }

}
