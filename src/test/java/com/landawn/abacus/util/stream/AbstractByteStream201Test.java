package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.ObjByteConsumer;

@Tag("new-test")
public class AbstractByteStream201Test extends TestBase {

    private ByteStream stream;
    private ByteStream stream2;
    private ByteStream stream3;
    private ByteStream stream4;
    private ByteStream stream5;
    private ByteStream emptyStream;

    @BeforeEach
    public void setUp() {
        final byte[] bytes = { 1, 2, 3, 4, 5 };
        stream = ByteStream.of(bytes);
        stream2 = ByteStream.of(bytes);
        stream3 = ByteStream.of(bytes);
        stream4 = ByteStream.of(bytes);
        stream4 = ByteStream.of(bytes);
        stream5 = ByteStream.of(bytes);
        emptyStream = ByteStream.empty();
    }

    @Test
    public void reduceWithIdentity() {
        byte result = stream.reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 25, result);

        byte emptyResult = emptyStream.reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 10, emptyResult);
    }

    @Test
    public void reduce() {
        OptionalByte result = stream.reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals((byte) 15, result.get());

        OptionalByte emptyResult = emptyStream.reduce((a, b) -> (byte) (a + b));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void collectWithCombiner() {
        Supplier<List<Byte>> supplier = ArrayList::new;
        ObjByteConsumer<List<Byte>> accumulator = List::add;
        BiConsumer<List<Byte>, List<Byte>> combiner = List::addAll;

        List<Byte> result = stream.collect(supplier, accumulator, combiner);
        assertEquals(5, result.size());
        assertTrue(result.containsAll(List.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)));

        List<Byte> emptyResult = emptyStream.collect(supplier, accumulator, combiner);
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void anyMatch() {
        assertTrue(stream.anyMatch(b -> b == 3));
        assertFalse(stream2.anyMatch(b -> b == 10));
        assertFalse(emptyStream.anyMatch(b -> b == 1));
    }

    @Test
    public void allMatch() {
        assertTrue(stream.allMatch(b -> b > 0));
        assertFalse(stream2.allMatch(b -> b > 3));
        assertTrue(emptyStream.allMatch(b -> b > 0));
    }

    @Test
    public void noneMatch() {
        assertTrue(stream.noneMatch(b -> b > 10));
        assertFalse(stream2.noneMatch(b -> b == 3));
        assertTrue(emptyStream.noneMatch(b -> b == 1));
    }

    @Test
    public void findFirstWithPredicate() {
        OptionalByte result = stream.findFirst(b -> b > 3);
        assertTrue(result.isPresent());
        assertEquals((byte) 4, result.get());

        OptionalByte notFound = stream2.findFirst(b -> b > 10);
        assertFalse(notFound.isPresent());

        OptionalByte emptyResult = emptyStream.findFirst(b -> b > 0);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void findLast() {
        OptionalByte result = stream.findLast(b -> b < 4);
        assertTrue(result.isPresent());
        assertEquals((byte) 3, result.get());

        OptionalByte notFound = stream2.findLast(b -> b < 1);
        assertFalse(notFound.isPresent());

        OptionalByte emptyResult = emptyStream.findLast(b -> b > 0);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void min() {
        OptionalByte result = stream.min();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());

        OptionalByte emptyResult = emptyStream.min();
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void max() {
        OptionalByte result = stream.max();
        assertTrue(result.isPresent());
        assertEquals((byte) 5, result.get());

        OptionalByte emptyResult = emptyStream.max();
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void kthLargest() {
        OptionalByte secondLargest = stream.kthLargest(2);
        assertTrue(secondLargest.isPresent());
        assertEquals((byte) 4, secondLargest.get());

        OptionalByte largest = stream2.kthLargest(1);
        assertTrue(largest.isPresent());
        assertEquals((byte) 5, largest.get());

        OptionalByte fifthLargest = stream3.kthLargest(5);
        assertTrue(fifthLargest.isPresent());
        assertEquals((byte) 1, fifthLargest.get());

        OptionalByte outOfBounds = stream4.kthLargest(6);
        assertFalse(outOfBounds.isPresent());

        assertThrows(IllegalArgumentException.class, () -> stream5.kthLargest(0));

        OptionalByte emptyResult = emptyStream.kthLargest(1);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void sum() {
        assertEquals(15, stream.sum());
        assertEquals(0, emptyStream.sum());
    }

    @Test
    public void average() {
        OptionalDouble result = stream.average();
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);

        OptionalDouble emptyResult = emptyStream.average();
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void summarize() {
        ByteSummaryStatistics stats = stream.summarize();
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);

        ByteSummaryStatistics emptyStats = emptyStream.summarize();
        assertEquals(0, emptyStats.getCount());
        assertEquals(0, emptyStats.getSum());
        assertEquals(Byte.MAX_VALUE, emptyStats.getMin());
        assertEquals(Byte.MIN_VALUE, emptyStats.getMax());
        assertEquals(0.0, emptyStats.getAverage(), 0.001);
    }

    @Test
    public void toByteList() {
        ByteList result = stream.toByteList();
        assertEquals(5, result.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());

        ByteList emptyResult = emptyStream.toByteList();
        assertTrue(emptyResult.isEmpty());
    }
}
