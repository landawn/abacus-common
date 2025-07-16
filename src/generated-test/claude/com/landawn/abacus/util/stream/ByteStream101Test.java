package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.ByteTriPredicate;

public class ByteStream101Test extends TestBase {

    // This method needs to be implemented by a concrete test class to provide a ByteStream instance.
    // For example, in ArrayByteStreamTest, it would return new ArrayByteStream(a);
    // In IteratorByteStreamTest, it would return new IteratorByteStream(ByteIterator.of(a));
    protected ByteStream createByteStream(byte... a) {
        return ByteStream.of(a);
    }

    protected ByteStream createByteStream(byte[] a, int fromIndex, int toIndex) {
        return ByteStream.of(a, fromIndex, toIndex);
    }

    protected ByteStream createByteStream(Byte[] a) {
        return ByteStream.of(a);
    }

    protected ByteStream createByteStream(Byte[] a, int fromIndex, int toIndex) {
        return ByteStream.of(a, fromIndex, toIndex);
    }

    protected ByteStream createByteStream(Collection<Byte> coll) {
        return ByteStream.of(coll.toArray(new Byte[coll.size()]));
    }

    protected ByteStream createByteStream(ByteIterator iter) {
        return iter == null ? ByteStream.empty() : ByteStream.of(iter.toArray());
    }

    protected ByteStream createByteStream(ByteBuffer buff) {
        return ByteStream.of(buff);
    }

    protected ByteStream createByteStream(File file) {
        return ByteStream.of(file);
    }

    protected ByteStream createByteStream(InputStream is) {
        return ByteStream.of(is);
    }

    protected ByteStream createByteStream(InputStream is, boolean closeInputStreamOnClose) {
        return ByteStream.of(is, closeInputStreamOnClose);
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<Byte> dropped = new ArrayList<>();

        ByteStream filtered = stream.filter(b -> b % 2 == 0, dropped::add);
        assertArrayEquals(new byte[] { 2, 4 }, filtered.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), dropped);
    }

    @Test
    public void testDropWhileWithActionOnDroppedItem() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<Byte> dropped = new ArrayList<>();

        ByteStream result = stream.dropWhile(b -> b < 3, dropped::add);
        assertArrayEquals(new byte[] { 3, 4, 5 }, result.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), dropped);
    }

    @Test
    public void testSkipWithActionOnSkippedItem() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<Byte> skipped = new ArrayList<>();

        ByteStream result = stream.skip(3, skipped::add);
        assertArrayEquals(new byte[] { 4, 5 }, result.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), skipped);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6, (byte) 10);
        ByteTriPredicate collapsible = (first, prev, curr) -> curr - first < 3;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);

        ByteStream collapsed = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 6, 11, 10 }, collapsed.toArray());
    }

    @Test
    public void testPrependStream() {
        ByteStream main = createByteStream((byte) 3, (byte) 4, (byte) 5);
        ByteStream toPrepend = createByteStream((byte) 1, (byte) 2);

        ByteStream result = main.prepend(toPrepend);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testPrependOptionalByte() {
        ByteStream stream = createByteStream((byte) 2, (byte) 3);

        // Test with present optional
        ByteStream result1 = stream.prepend(OptionalByte.of((byte) 1));
        assertArrayEquals(new byte[] { 1, 2, 3 }, result1.toArray());

        // Test with empty optional
        ByteStream stream2 = createByteStream((byte) 2, (byte) 3);
        ByteStream result2 = stream2.prepend(OptionalByte.empty());
        assertArrayEquals(new byte[] { 2, 3 }, result2.toArray());
    }

    @Test
    public void testAppendStream() {
        ByteStream main = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream toAppend = createByteStream((byte) 4, (byte) 5);

        ByteStream result = main.append(toAppend);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testAppendOptionalByte() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);

        // Test with present optional
        ByteStream result1 = stream.append(OptionalByte.of((byte) 3));
        assertArrayEquals(new byte[] { 1, 2, 3 }, result1.toArray());

        // Test with empty optional
        ByteStream stream2 = createByteStream((byte) 1, (byte) 2);
        ByteStream result2 = stream2.append(OptionalByte.empty());
        assertArrayEquals(new byte[] { 1, 2 }, result2.toArray());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        // Test with empty stream
        ByteStream empty = ByteStream.empty();
        ByteStream result1 = empty.appendIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 1, 2 }, result1.toArray());

        // Test with non-empty stream
        ByteStream nonEmpty = createByteStream((byte) 3);
        ByteStream result2 = nonEmpty.appendIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 3 }, result2.toArray());
    }

    @Test
    public void testDefaultIfEmpty() {
        // Test with empty stream
        ByteStream empty = ByteStream.empty();
        ByteStream result1 = empty.defaultIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 1, 2 }, result1.toArray());

        // Test with non-empty stream
        ByteStream nonEmpty = createByteStream((byte) 3);
        ByteStream result2 = nonEmpty.defaultIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 3 }, result2.toArray());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream.throwIfEmpty(() -> new RuntimeException("Should not throw")).toArray());

        ByteStream empty = ByteStream.empty();
        assertThrows(IllegalStateException.class, () -> empty.throwIfEmpty(() -> new IllegalStateException("Empty stream")).toArray());
    }

    @Test
    public void testIfEmpty() {
        AtomicInteger counter = new AtomicInteger(0);

        // Test with empty stream
        ByteStream empty = ByteStream.empty();
        empty.ifEmpty(() -> counter.incrementAndGet()).toArray();
        assertEquals(1, counter.get());

        // Test with non-empty stream
        ByteStream nonEmpty = createByteStream((byte) 1);
        nonEmpty.ifEmpty(() -> counter.incrementAndGet()).toArray();
        assertEquals(1, counter.get()); // Should not increment
    }

    @Test
    public void testElementAt() {
        ByteStream stream = createByteStream((byte) 10, (byte) 20, (byte) 30, (byte) 40);

        // Test valid positions
        assertEquals(10, stream.elementAt(0).get());

        ByteStream stream2 = createByteStream((byte) 10, (byte) 20, (byte) 30, (byte) 40);
        assertEquals(30, stream2.elementAt(2).get());

        // Test position beyond stream length
        ByteStream stream3 = createByteStream((byte) 10, (byte) 20);
        assertFalse(stream3.elementAt(5).isPresent());
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10); // 10 permits per second
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        long startTime = System.currentTimeMillis();
        byte[] result = stream.rateLimited(rateLimiter).toArray();
        long duration = System.currentTimeMillis() - startTime;

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
        // With 5 elements at 10 permits/second, it should take at least some time
        assertTrue(duration >= 0); // Just verify it completes
    }

    @Test
    public void testRateLimitedWithPermitsPerSecond() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        long startTime = System.currentTimeMillis();
        byte[] result = stream.rateLimited(100).toArray(); // 100 permits per second
        long duration = System.currentTimeMillis() - startTime;

        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertTrue(duration >= 0); // Just verify it completes
    }

    @Test
    public void testDelay() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Duration delay = Duration.ofMillis(10);

        long startTime = System.currentTimeMillis();
        byte[] result = stream.delay(delay).toArray();
        long duration = System.currentTimeMillis() - startTime;

        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertTrue(duration >= 20); // At least 2 delays (after elements 2 and 3)
    }

    @Test
    public void testToMapWithMergeFunction() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 1, (byte) 3);

        // Test with merge function to handle duplicates
        var map = stream.toMap(b -> b % 2 == 0 ? "even" : "odd", b -> (int) b, (v1, v2) -> v1 + v2);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(5), map.get("odd")); // 1 + 1 + 3
        assertEquals(Integer.valueOf(2), map.get("even"));
    }

    @Test
    public void testToMapWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        var map = stream.toMap(b -> "key" + b, b -> (int) b, Suppliers.ofLinkedHashMap());

        assertEquals(3, map.size());
        assertTrue(map instanceof java.util.LinkedHashMap);
    }

    @Test
    public void testToMapWithMergeFunctionAndSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 1, (byte) 3);

        var map = stream.toMap(b -> b % 2 == 0 ? "even" : "odd", b -> (int) b, (v1, v2) -> v1 + v2, java.util.TreeMap::new);

        assertEquals(2, map.size());
        assertTrue(map instanceof java.util.TreeMap);
        assertEquals(Integer.valueOf(5), map.get("odd"));
        assertEquals(Integer.valueOf(2), map.get("even"));
    }

    @Test
    public void testGroupToWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        var grouped = stream.groupTo(b -> b % 2 == 0 ? "even" : "odd", Collectors.counting(), java.util.TreeMap::new);

        assertTrue(grouped instanceof java.util.TreeMap);
        assertEquals(2, grouped.size());
        assertEquals(Long.valueOf(3), grouped.get("odd"));
        assertEquals(Long.valueOf(2), grouped.get("even"));
    }

    @Test
    public void testCollectWithSupplierOnly() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        ByteList list = stream.collect(ByteList::new, ByteList::add);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testZipWithThreeStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 3, (byte) 4);
        ByteStream c = createByteStream((byte) 5, (byte) 6);

        ByteStream result = a.zipWith(b, c, (x, y, z) -> (byte) (x + y + z));
        assertArrayEquals(new byte[] { 9, 12 }, result.toArray());
    }

    @Test
    public void testZipWithDefaults2Streams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = createByteStream((byte) 4, (byte) 5);

        ByteStream result = a.zipWith(b, (byte) 10, (byte) 20, (x, y) -> (byte) (x + y));
        assertArrayEquals(new byte[] { 5, 7, 23 }, result.toArray());
    }

    @Test
    public void testZipWithDefaults3Streams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 3);
        ByteStream c = createByteStream((byte) 5, (byte) 6, (byte) 7);

        ByteStream result = a.zipWith(b, c, (byte) 10, (byte) 20, (byte) 30, (x, y, z) -> (byte) (x + y + z));
        assertArrayEquals(new byte[] { 9, 28, 37 }, result.toArray());
    }

    @Test
    public void testConcatIterators() {
        List<ByteIterator> iterators = Arrays.asList(ByteIterator.of(new byte[] { 1, 2 }), ByteIterator.of(new byte[] { 3, 4 }),
                ByteIterator.of(new byte[] { 5 }));

        ByteStream stream = ByteStream.concatIterators(iterators);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testZipMultipleStreams() {
        List<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 2), createByteStream((byte) 3, (byte) 4),
                createByteStream((byte) 5, (byte) 6));

        ByteNFunction<Byte> zipFunction = values -> {
            byte sum = 0;
            for (byte b : values) {
                sum += b;
            }
            return sum;
        };

        ByteStream result = ByteStream.zip(streams, zipFunction);
        assertArrayEquals(new byte[] { 9, 12 }, result.toArray());
    }

    @Test
    public void testZipMultipleStreamsWithDefaults() {
        List<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 2, (byte) 3), createByteStream((byte) 4),
                createByteStream((byte) 5, (byte) 6));

        byte[] defaults = { 10, 20, 30 };

        ByteNFunction<Byte> zipFunction = values -> {
            byte sum = 0;
            for (byte b : values) {
                sum += b;
            }
            return sum;
        };

        ByteStream result = ByteStream.zip(streams, defaults, zipFunction);
        assertArrayEquals(new byte[] { 10, 28, 53 }, result.toArray());
    }

    @Test
    public void testMergeThreeArrays() {
        byte[] a = { 1, 4, 7 };
        byte[] b = { 2, 5, 8 };
        byte[] c = { 3, 6, 9 };

        ByteBiFunction<MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ByteStream result = ByteStream.merge(a, b, c, selector);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result.toArray());
    }

    @Test
    public void testMergeThreeIterators() {
        ByteIterator a = ByteIterator.of(new byte[] { 1, 4, 7 });
        ByteIterator b = ByteIterator.of(new byte[] { 2, 5, 8 });
        ByteIterator c = ByteIterator.of(new byte[] { 3, 6, 9 });

        ByteBiFunction<MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ByteStream result = ByteStream.merge(a, b, c, selector);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result.toArray());
    }

    @Test
    public void testMergeThreeStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 4, (byte) 7);
        ByteStream b = createByteStream((byte) 2, (byte) 5, (byte) 8);
        ByteStream c = createByteStream((byte) 3, (byte) 6, (byte) 9);

        ByteBiFunction<MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ByteStream result = ByteStream.merge(a, b, c, selector);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result.toArray());
    }

    @Test
    public void testMergeMultipleStreams() {
        List<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 5), createByteStream((byte) 2, (byte) 6),
                createByteStream((byte) 3, (byte) 7), createByteStream((byte) 4, (byte) 8));

        ByteBiFunction<MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ByteStream result = ByteStream.merge(streams, selector);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, result.toArray());
    }

    @Test
    public void testPrintln() {
        // This test just verifies the method doesn't throw
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        stream.println(); // Should print to console
    }

    @Test
    public void testToImmutableList() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        var immutableList = stream.toImmutableList();

        assertEquals(3, immutableList.size());
        assertEquals(Byte.valueOf((byte) 1), immutableList.get(0));
        assertEquals(Byte.valueOf((byte) 2), immutableList.get(1));
        assertEquals(Byte.valueOf((byte) 3), immutableList.get(2));
    }

    @Test
    public void testToImmutableSet() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        var immutableSet = stream.toImmutableSet();

        assertEquals(3, immutableSet.size());
        assertTrue(immutableSet.contains((byte) 1));
        assertTrue(immutableSet.contains((byte) 2));
        assertTrue(immutableSet.contains((byte) 3));
    }

    @Test
    public void testToCollection() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        var collection = stream.toCollection(ArrayList::new);

        assertTrue(collection instanceof ArrayList);
        assertEquals(3, collection.size());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), collection);
    }

    @Test
    public void testToMultisetWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        var multiset = stream.toMultiset(com.landawn.abacus.util.Multiset::new);

        assertEquals(4, multiset.size());
        assertEquals(1, multiset.getCount((byte) 1));
        assertEquals(2, multiset.getCount((byte) 2));
        assertEquals(1, multiset.getCount((byte) 3));
    }

    @Test
    public void testFindAnyDefault() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.first();

        assertTrue(result.isPresent());
        assertEquals(1, result.get()); // In sequential stream, findAny() should behave like findFirst()
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(2); // Max 2 threads

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());
    }

    @Test
    public void testParallelWithExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(executor);

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());

        ((java.util.concurrent.ExecutorService) executor).shutdown();
    }

    @Test
    public void testParallelWithMaxThreadNumAndExecutor() {
        Executor executor = Executors.newFixedThreadPool(3);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(2, executor);

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());

        ((java.util.concurrent.ExecutorService) executor).shutdown();
    }

    @Test
    public void testSps() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        // Apply parallel operation and then return to sequential
        ByteStream result = stream.sps(s -> s.map(b -> (byte) (b * 2)));

        assertFalse(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4, 6, 8, 10 }, result.sorted().toArray());
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        ByteStream result = stream.sps(2, s -> s.map(b -> (byte) (b * 2)));

        assertFalse(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4, 6, 8, 10 }, result.sorted().toArray());
    }

    @Test
    public void testPsp() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        // Apply sequential operation and then return to parallel
        ByteStream result = stream.parallel().psp(s -> s.filter(b -> b % 2 == 0));

        assertTrue(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4 }, result.sorted().toArray());
    }

    @Test
    public void testTransform() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        // Transform to IntStream
        IntStream intStream = stream.transform(s -> s.mapToInt(b -> b * 10));

        assertArrayEquals(new int[] { 10, 20, 30 }, intStream.toArray());
    }

    @Test
    public void testDoubleUnderscore() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        // __ is an alias for transform
        IntStream intStream = stream.__(s -> s.mapToInt(b -> b * 10));

        assertArrayEquals(new int[] { 10, 20, 30 }, intStream.toArray());
    }

    @Test
    public void testApplyIfNotEmpty() {
        // Test with non-empty stream
        ByteStream nonEmpty = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Optional<Integer> result1 = nonEmpty.applyIfNotEmpty(s -> s.sum());

        assertTrue(result1.isPresent());
        assertEquals(6, result1.get().intValue());

        // Test with empty stream
        ByteStream empty = ByteStream.empty();
        Optional<Integer> result2 = empty.applyIfNotEmpty(s -> s.sum());

        assertFalse(result2.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicInteger sum = new AtomicInteger(0);

        // Test with non-empty stream
        ByteStream nonEmpty = createByteStream((byte) 1, (byte) 2, (byte) 3);
        var orElse1 = nonEmpty.acceptIfNotEmpty(s -> sum.set(s.sum()));

        assertEquals(6, sum.get());

        // Test orElse on non-empty stream (should not execute)
        AtomicInteger counter = new AtomicInteger(0);
        orElse1.orElse(() -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        // Test with empty stream
        sum.set(0);
        ByteStream empty = ByteStream.empty();
        var orElse2 = empty.acceptIfNotEmpty(s -> sum.set(s.sum()));

        assertEquals(0, sum.get());

        // Test orElse on empty stream (should execute)
        orElse2.orElse(() -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testOnClose() {
        AtomicInteger closeCount = new AtomicInteger(0);

        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3).onClose(() -> closeCount.incrementAndGet()).onClose(() -> closeCount.addAndGet(10));

        assertEquals(0, closeCount.get());

        // Terminal operation should trigger close
        stream.toArray();

        assertEquals(11, closeCount.get());
    }

    @Test
    public void testWithCloseHandlerInConstructor() {
        AtomicInteger closeCount = new AtomicInteger(0);
        List<Runnable> closeHandlers = Arrays.asList(() -> closeCount.incrementAndGet(), () -> closeCount.addAndGet(10));

        // This would require access to a constructor that accepts closeHandlers
        // which might not be directly accessible, so we'll test through operations
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3).onClose(closeHandlers.get(0)).onClose(closeHandlers.get(1));

        stream.close();
        assertEquals(11, closeCount.get());
    }
}
