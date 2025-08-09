package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.ByteTriPredicate;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;

public class ByteStream201Test extends TestBase {

    private static final byte[] TEST_ARRAY = new byte[] { 1, 2, 3, 4, 5 };
    private ByteStream byteStream;

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
        // Initialize the stream for each test.
        // This method will be implemented in concrete test classes to provide specific stream instances.
        byteStream = createByteStream(TEST_ARRAY);
    }

    // Test for map(ByteUnaryOperator mapper)
    @Test
    public void testMap() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.map(b -> (byte) (b * 2)).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 4, (byte) 6), result);
    }

    // Test for mapToInt(ByteToIntFunction mapper)
    @Test
    public void testMapToInt() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.mapToInt(b -> b + 10).toList();
        assertEquals(Arrays.asList(11, 12, 13), result);
    }

    // Test for mapToObj(ByteFunction<? extends T> mapper)
    @Test
    public void testMapToObj() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<String> result = byteStream.mapToObj(b -> "Byte_" + b).toList();
        assertEquals(Arrays.asList("Byte_1", "Byte_2", "Byte_3"), result);
    }

    // Test for flatMap(ByteFunction<? extends ByteStream> mapper)
    @Test
    public void testFlatMap() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.flatMap(b -> createByteStream(b, (byte) (b + 10))).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 11, (byte) 2, (byte) 12), result);
    }

    // Test for flatmap(ByteFunction<byte[]> mapper)
    @Test
    public void testFlatmapByteArray() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.flatmap(b -> new byte[] { b, (byte) (b * 10) }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 10, (byte) 2, (byte) 20), result);
    }

    // Test for flatMapToInt(ByteFunction<? extends IntStream> mapper)
    @Test
    public void testFlatMapToInt() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Integer> result = byteStream.flatMapToInt(b -> IntStream.of(b, b + 10)).toList();
        assertEquals(Arrays.asList(1, 11, 2, 12), result);
    }

    // Test for flatMapToObj(ByteFunction<? extends Stream<? extends T>> mapper)
    @Test
    public void testFlatMapToObj() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<String> result = byteStream.flatMapToObj(b -> Stream.of("A" + b, "B" + b)).toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2"), result);
    }

    // Test for flatmapToObj(ByteFunction<? extends Collection<? extends T>> mapper)
    @Test
    public void testFlatmapToObjCollection() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<String> result = byteStream.flatmapToObj(b -> Arrays.asList("C" + b, "D" + b)).toList();
        assertEquals(Arrays.asList("C1", "D1", "C2", "D2"), result);
    }

    // Test for flattmapToObj(ByteFunction<T[]> mapper)
    @Test
    public void testFlattMapToObj() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<String> result = byteStream.flattmapToObj(b -> new String[] { "E" + b, "F" + b }).toList();
        assertEquals(Arrays.asList("E1", "F1", "E2", "F2"), result);
    }

    // Test for mapPartial(ByteFunction<OptionalByte> mapper)
    @Test
    public void testMapPartial() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 4 });
        List<Byte> result = byteStream.mapPartial(b -> b % 2 == 0 ? OptionalByte.of(b) : OptionalByte.empty()).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result);
    }

    // Test for rangeMap(ByteBiPredicate sameRange, ByteBinaryOperator mapper)
    @Test
    public void testRangeMap() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 10, 11, 20, 21 });
        ByteBiPredicate sameRange = (a, b) -> Math.abs(a - b) < 2;
        ByteBinaryOperator mapper = (first, last) -> (byte) (first + last);
        List<Byte> result = byteStream.rangeMap(sameRange, mapper).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 6, (byte) 21, (byte) 41), result); // (1+2), 3, (10+11), (20+21)
    }

    // Test for rangeMapToObj(ByteBiPredicate sameRange, ByteBiFunction<? extends T> mapper)
    @Test
    public void testRangeMapToObj() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 10, 11, 20, 21 });
        ByteBiPredicate sameRange = (a, b) -> Math.abs(a - b) < 2;
        ByteBiFunction<String> mapper = (first, last) -> "Range[" + first + "-" + last + "]";
        List<String> result = byteStream.rangeMapToObj(sameRange, mapper).toList();
        assertEquals(Arrays.asList("Range[1-2]", "Range[3-3]", "Range[10-11]", "Range[20-21]"), result);
    }

    // Test for collapse(ByteBiPredicate collapsible)
    @Test
    public void testCollapseBiPredicate() {
        byteStream = createByteStream(new byte[] { 1, 2, 5, 6, 7, 10 });
        ByteBiPredicate collapsible = (a, b) -> Math.abs(a - b) < 3;
        List<ByteList> result = byteStream.collapse(collapsible).toList();
        assertEquals(Arrays.asList(ByteList.of((byte) 1, (byte) 2), ByteList.of((byte) 5, (byte) 6, (byte) 7), ByteList.of((byte) 10)), result);
    }

    // Test for collapse(ByteBiPredicate collapsible, ByteBinaryOperator mergeFunction)
    @Test
    public void testCollapseBiPredicateMergeFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 5, 6, 7, 10 });
        ByteBiPredicate collapsible = (a, b) -> Math.abs(a - b) < 3;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);
        List<Byte> result = byteStream.collapse(collapsible, mergeFunction).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 18, (byte) 10), result); // (1+2), (5+6+7), 10
    }

    // Test for collapse(ByteTriPredicate collapsible, ByteBinaryOperator mergeFunction)
    @Test
    public void testCollapseTriPredicateMergeFunction() {
        byteStream = createByteStream(new byte[] { 1, 3, 6, 10, 15, 16 });
        // Predicate: first, last of current group, the next element. Collapse if the next element is >= last.
        ByteTriPredicate collapsible = (first, last, next) -> next >= last;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b); // Sum elements
        List<Byte> result = byteStream.collapse(collapsible, mergeFunction).toList();
        // (1,3,6) -> 1+3+6=10. Next is 10. (first=1, last=6, next=10). 10>=6. So collapse 10. (1,6,10) -> 1+3+6+10=20. Next is 15. (first=1, last=10, next=15). 15>=10. So collapse 15.
        // Group 1: 1, 3, 6. Collapsible(1, 3, 6) true. new group = 1,3. Next is 6. collapsible(1,3,6)
        // Grouping:
        // 1. Current group: [1]. Next: 3. (1,1,3) -> 3 >= 1. Yes. Group: [1,3].
        // 2. Current group: [1,3]. Next: 6. (1,3,6) -> 6 >= 3. Yes. Group: [1,3,6].
        // 3. Current group: [1,3,6]. Next: 10. (1,6,10) -> 10 >= 6. Yes. Group: [1,3,6,10].
        // 4. Current group: [1,3,6,10]. Next: 15. (1,10,15) -> 15 >= 10. Yes. Group: [1,3,6,10,15].
        // 5. Current group: [1,3,6,10,15]. Next: 16. (1,15,16) -> 16 >= 15. Yes. Group: [1,3,6,10,15,16].
        // End of stream. Result: (1+3+6+10+15+16) = 51
        assertEquals(Arrays.asList((byte) 51), result);
    }

    // Test for scan(ByteBinaryOperator accumulator)
    @Test
    public void testScanAccumulator() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 4 });
        List<Byte> result = byteStream.scan((a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 6, (byte) 10), result);
    }

    // Test for scan(byte init, ByteBinaryOperator accumulator)
    @Test
    public void testScanInitAccumulator() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.scan((byte) 10, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 13, (byte) 16), result);
    }

    // Test for scan(byte init, boolean initIncluded, ByteBinaryOperator accumulator)
    @Test
    public void testScanInitIncludedAccumulator() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.scan((byte) 10, true, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 11, (byte) 13, (byte) 16), result);
    }

    // Test for prepend(byte... a)
    @Test
    public void testPrependBytes() {
        byteStream = createByteStream(new byte[] { 4, 5 });
        List<Byte> result = byteStream.prepend((byte) 1, (byte) 2, (byte) 3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    // Test for prepend(ByteStream stream)
    @Test
    public void testPrependByteStream() {
        byteStream = createByteStream(new byte[] { 4, 5 });
        ByteStream prefixStream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = byteStream.prepend(prefixStream).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    // Test for prepend(OptionalByte op)
    @Test
    public void testPrependOptionalByte() {
        byteStream = createByteStream(new byte[] { 2, 3 });
        List<Byte> result = byteStream.prepend(OptionalByte.of((byte) 1)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);

        byteStream = createByteStream(new byte[] { 2, 3 });
        result = byteStream.prepend(OptionalByte.empty()).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 3), result);
    }

    // Test for append(byte... a)
    @Test
    public void testAppendBytes() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.append((byte) 3, (byte) 4, (byte) 5).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    // Test for append(ByteStream stream)
    @Test
    public void testAppendByteStream() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        ByteStream suffixStream = createByteStream((byte) 3, (byte) 4, (byte) 5);
        List<Byte> result = byteStream.append(suffixStream).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    // Test for append(OptionalByte op)
    @Test
    public void testAppendOptionalByte() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.append(OptionalByte.of((byte) 3)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);

        byteStream = createByteStream(new byte[] { 1, 2 });
        result = byteStream.append(OptionalByte.empty()).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2), result);
    }

    // Test for appendIfEmpty(byte... a)
    @Test
    public void testAppendIfEmptyBytes() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.appendIfEmpty((byte) 10, (byte) 11).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2), result);

        byteStream = createByteStream(new byte[] {});
        result = byteStream.appendIfEmpty((byte) 10, (byte) 11).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 11), result);
    }

    // Test for toByteList()
    @Test
    public void testToByteList() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteList result = byteStream.toByteList();
        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3), result);
    }

    // Test for toMap(Throwables.ByteFunction<? extends K, E> keyMapper, Throwables.ByteFunction<? extends V, E2> valueMapper)
    @Test
    public void testToMapKeyValue() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> (int) b * 10);
        assertEquals(3, result.size());
        assertEquals(10, (int) result.get("K1"));
        assertEquals(20, (int) result.get("K2"));
        assertEquals(30, (int) result.get("K3"));
    }

    // Test for toMap(Throwables.ByteFunction<? extends K, E> keyMapper, Throwables.ByteFunction<? extends V, E2> valueMapper, Supplier<? extends M> mapFactory)
    @Test
    public void testToMapKeyValueMapFactory() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> (int) b * 10, () -> N.newHashMap());
        assertEquals(3, result.size());
        assertEquals(10, (int) result.get("K1"));
    }

    // Test for toMap(Throwables.ByteFunction<? extends K, E> keyMapper, Throwables.ByteFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction)
    @Test
    public void testToMapKeyValueMergeFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 1 }); // Duplicate key 'K1'
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> (int) b * 10, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(20, (int) result.get("K1")); // 10 + 10
        assertEquals(20, (int) result.get("K2"));
    }

    // Test for toMap(Throwables.ByteFunction<? extends K, E> keyMapper, Throwables.ByteFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory)
    @Test
    public void testToMapKeyValueMergeFunctionMapFactory() {
        byteStream = createByteStream(new byte[] { 1, 2, 1 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> (int) b * 10, (v1, v2) -> v1 + v2, () -> N.newLinkedHashMap());
        assertEquals(2, result.size());
        assertEquals(20, (int) result.get("K1"));
        assertEquals(20, (int) result.get("K2"));
    }

    // Test for groupTo(Throwables.ByteFunction<? extends K, E> keyMapper, final Collector<? super Byte, ?, D> downstream)
    @Test
    public void testGroupToCollector() {
        byteStream = createByteStream(TEST_ARRAY);
        Map<String, List<Byte>> result = byteStream.groupTo(b -> (b % 2 == 0) ? "Even" : "Odd", Collectors.mapping(b -> b, Collectors.toList()));
        assertEquals(2, result.size());
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result.get("Even"));
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), result.get("Odd"));
    }

    // Test for groupTo(Throwables.ByteFunction<? extends K, E> keyMapper, final Collector<? super Byte, ?, D> downstream, final Supplier<? extends M> mapFactory)
    @Test
    public void testGroupToCollectorMapFactory() {
        byteStream = createByteStream(TEST_ARRAY);
        Map<String, Long> result = byteStream.groupTo(b -> (b % 2 == 0) ? "Even" : "Odd", Collectors.counting(), Suppliers.ofTreeMap());
        // Group by Even and Odd, counting occurrences
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(2), result.get("Even"));
        assertEquals(Long.valueOf(3), result.get("Odd"));
    }

    // Test for reduce(byte identity, ByteBinaryOperator op)
    @Test
    public void testReduceIdentity() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        byte result = byteStream.reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 16, result); // 10 + 1 + 2 + 3
    }

    // Test for reduce(ByteBinaryOperator op)
    @Test
    public void testReduce() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        OptionalByte result = byteStream.reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals((byte) 6, result.get());

        byteStream = createByteStream(new byte[] {});
        result = byteStream.reduce((a, b) -> (byte) (a + b));
        assertFalse(result.isPresent());
    }

    // Test for collect(Supplier<R> supplier, ObjByteConsumer<? super R> accumulator, BiConsumer<R, R> combiner)
    @Test
    public void testCollectFull() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        StringBuilder sb = byteStream.collect(StringBuilder::new, (s, b) -> s.append(b), StringBuilder::append);
        assertEquals("123", sb.toString());
    }

    // Test for collect(Supplier<R> supplier, ObjByteConsumer<? super R> accumulator)
    @Test
    public void testCollectSimplified() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteList list = byteStream.collect(ByteList::new, ByteList::add);
        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3), list);
    }

    // Test for forEach(Throwables.ByteConsumer<E> action)
    @Test
    public void testForEach() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> consumed = new ArrayList<>();
        byteStream.forEach(consumed::add);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), consumed);
    }

    // Test for forEachIndexed(Throwables.IntByteConsumer<E> action)
    @Test
    public void testForEachIndexed() {
        byteStream = createByteStream(new byte[] { 10, 20, 30 });
        List<String> indexedConsumed = new ArrayList<>();
        byteStream.forEachIndexed((idx, b) -> indexedConsumed.add(idx + ":" + b));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), indexedConsumed);
    }

    // Test for anyMatch(Throwables.BytePredicate<E> predicate)
    @Test
    public void testAnyMatch() {
        byteStream = createByteStream(TEST_ARRAY);
        assertTrue(byteStream.anyMatch(b -> b == 3));
        assertFalse(createByteStream(TEST_ARRAY).anyMatch(b -> b == 10));
        assertFalse(createByteStream(new byte[] {}).anyMatch(b -> true));
    }

    // Test for allMatch(Throwables.BytePredicate<E> predicate)
    @Test
    public void testAllMatch() {
        assertTrue(createByteStream(new byte[] { 1, 2, 3 }).allMatch(b -> b > 0));
        assertFalse(createByteStream(new byte[] { 1, 2, 3 }).allMatch(b -> b > 1));
        assertTrue(createByteStream(new byte[] {}).allMatch(b -> false)); // All match true for empty stream
    }

    // Test for noneMatch(Throwables.BytePredicate<E> predicate)
    @Test
    public void testNoneMatch() {
        assertTrue(createByteStream(new byte[] { 1, 2, 3 }).noneMatch(b -> b > 5));
        assertFalse(createByteStream(new byte[] { 1, 2, 3 }).noneMatch(b -> b == 2));
        assertTrue(createByteStream(new byte[] {}).noneMatch(b -> true)); // None match true for empty stream
    }

    // Test for findFirst(Throwables.BytePredicate<E> predicate)
    @Test
    public void testFindFirst() {
        byteStream = createByteStream(new byte[] { 10, 20, 30, 20, 40 });
        OptionalByte result = byteStream.findFirst(b -> b == 20);
        assertTrue(result.isPresent());
        assertEquals((byte) 20, result.get());

        result = createByteStream(new byte[] { 10, 20, 30, 20, 40 }).findFirst(b -> b == 50);
        assertFalse(result.isPresent());
    }

    // Test for findAny(Throwables.BytePredicate<E> predicate)
    @Test
    public void testFindAny() {
        byteStream = createByteStream(new byte[] { 10, 20, 30, 20, 40 });
        OptionalByte result = byteStream.findAny(b -> b == 20);
        assertTrue(result.isPresent());
        assertEquals((byte) 20, result.get()); // Can be any 20

        result = createByteStream(new byte[] { 10, 20, 30, 20, 40 }).findAny(b -> b == 50);
        assertFalse(result.isPresent());
    }

    // Test for findLast(Throwables.BytePredicate<E> predicate)
    @Test
    public void testFindLast() {
        byteStream = createByteStream(new byte[] { 10, 20, 30, 20, 40 });
        OptionalByte result = byteStream.findLast(b -> b == 20);
        assertTrue(result.isPresent());
        assertEquals((byte) 20, result.get()); // Last 20

        result = createByteStream(new byte[] { 10, 20, 30, 20, 40 }).findLast(b -> b == 50);
        assertFalse(result.isPresent());
    }

    // Test for min()
    @Test
    public void testMin() {
        byteStream = createByteStream(new byte[] { 5, 1, 8, 2 });
        OptionalByte result = byteStream.min();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());

        byteStream = createByteStream(new byte[] {});
        result = byteStream.min();
        assertFalse(result.isPresent());
    }

    // Test for max()
    @Test
    public void testMax() {
        byteStream = createByteStream(new byte[] { 5, 1, 8, 2 });
        OptionalByte result = byteStream.max();
        assertTrue(result.isPresent());
        assertEquals((byte) 8, result.get());

        byteStream = createByteStream(new byte[] {});
        result = byteStream.max();
        assertFalse(result.isPresent());
    }

    // Test for kthLargest(int k)
    @Test
    public void testKthLargest() {
        byteStream = createByteStream(new byte[] { 5, 1, 8, 2, 7 }); // Sorted: 1, 2, 5, 7, 8
        OptionalByte result = byteStream.kthLargest(1); // 1st largest = 8
        assertTrue(result.isPresent());
        assertEquals((byte) 8, result.get());

        result = createByteStream(new byte[] { 5, 1, 8, 2, 7 }).kthLargest(3); // 3rd largest = 5
        assertTrue(result.isPresent());
        assertEquals((byte) 5, result.get());

        result = createByteStream(new byte[] { 5, 1, 8, 2, 7 }).kthLargest(5); // 5th largest = 1
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());

        result = createByteStream(new byte[] { 5, 1, 8, 2, 7 }).kthLargest(6); // k out of bounds
        assertFalse(result.isPresent());

        byteStream = createByteStream(new byte[] {});
        result = byteStream.kthLargest(1);
        assertFalse(result.isPresent());
    }

    // Test for sum()
    @Test
    public void testSum() {
        byteStream = createByteStream(TEST_ARRAY);
        int result = byteStream.sum();
        assertEquals(15, result);

        byteStream = createByteStream(new byte[] {});
        result = byteStream.sum();
        assertEquals(0, result);
    }

    // Test for average()
    @Test
    public void testAverage() {
        byteStream = createByteStream(TEST_ARRAY);
        OptionalDouble result = byteStream.average();
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);

        byteStream = createByteStream(new byte[] {});
        result = byteStream.average();
        assertFalse(result.isPresent());
    }

    // Test for summarize()
    @Test
    public void testSummarize() {
        byteStream = createByteStream(new byte[] { 1, 5, 2, 8, 3 });
        ByteSummaryStatistics stats = byteStream.summarize();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(8, stats.getMax());
        assertEquals(19, stats.getSum().longValue());
        assertEquals(3.8, stats.getAverage(), 0.001);

        byteStream = createByteStream(new byte[] {});
        stats = byteStream.summarize();
        assertEquals(0, stats.getCount());
    }

    // Test for summarizeAndPercentiles()
    @Test
    public void testSummarizeAndPercentiles() {
        byteStream = createByteStream(new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = byteStream.summarizeAndPercentiles();

        ByteSummaryStatistics stats = result.left();
        assertEquals(10, stats.getCount());
        assertEquals(10, stats.getMin());
        assertEquals(100, stats.getMax());
        assertEquals(550, stats.getSum().longValue());

        Optional<Map<Percentage, Byte>> percentilesOpt = result.right();
        assertTrue(percentilesOpt.isPresent());
        Map<Percentage, Byte> percentiles = percentilesOpt.get();
        assertEquals((byte) 60, percentiles.get(Percentage._50).byteValue()); // Median
        assertEquals((byte) 100, percentiles.get(Percentage._90).byteValue()); // Median
        assertEquals((byte) 100, percentiles.get(Percentage._99_9999).byteValue());
    }

    // Test for mergeWith(ByteStream b, ByteBiFunction<MergeResult> nextSelector)
    @Test
    public void testMergeWith() {
        byteStream = createByteStream(new byte[] { 1, 3, 5 });
        ByteStream otherStream = createByteStream(new byte[] { 2, 4, 6 });
        // Merge in ascending order
        ByteBiFunction<MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = byteStream.mergeWith(otherStream, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    // Test for zipWith(ByteStream b, ByteBinaryOperator zipFunction)
    @Test
    public void testZipWithTwoStreams() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream otherStream = createByteStream(new byte[] { 10, 20, 30, 40 });
        List<Byte> result = byteStream.zipWith(otherStream, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 33), result); // Zips until shortest stream ends
    }

    // Test for zipWith(ByteStream b, ByteStream c, ByteTernaryOperator zipFunction)
    @Test
    public void testZipWithThreeStreams() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        ByteStream s2 = createByteStream(new byte[] { 10, 20 });
        ByteStream s3 = createByteStream(new byte[] { 50, 60 });
        List<Byte> result = byteStream.zipWith(s2, s3, (a, b, c) -> (byte) (a + b + c)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (2 + 20 + 60)), result);
    }

    // Test for zipWith(ByteStream b, byte valueForNoneA, byte valueForNoneB, ByteBinaryOperator zipFunction)
    @Test
    public void testZipWithTwoStreamsWithDefaults() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        ByteStream otherStream = createByteStream(new byte[] { 10, 20, 30 });
        List<Byte> result = byteStream.zipWith(otherStream, (byte) 99, (byte) 88, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) (99 + 30)), result);
    }

    // Test for zipWith(ByteStream b, ByteStream c, byte valueForNoneA, byte valueForNoneB, byte valueForNoneC, ByteTernaryOperator zipFunction)
    @Test
    public void testZipWithThreeStreamsWithDefaults() {
        byteStream = createByteStream(new byte[] { 1 });
        ByteStream s2 = createByteStream(new byte[] { 10, 20 });
        ByteStream s3 = createByteStream(new byte[] { 50, 60, 70 });
        List<Byte> result = byteStream.zipWith(s2, s3, (byte) 99, (byte) 88, (byte) 77, (a, b, c) -> (byte) (a + b + c)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (99 + 20 + 60), (byte) (99 + 88 + 70)), result);
    }

    // Test for asIntStream()
    @Test
    public void testAsIntStream() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.asIntStream().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // Test for boxed()
    @Test
    public void testBoxed() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    // Test for empty()
    @Test
    public void testEmpty() {
        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.count());
    }

    // Test for defer(Supplier<ByteStream> supplier)
    @Test
    public void testDefer() {
        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        ByteStream deferredStream = ByteStream.defer(() -> {
            supplierCalled.set(true);
            return createByteStream((byte) 1, (byte) 2);
        });
        assertFalse(supplierCalled.get()); // Not called yet

        List<Byte> result = deferredStream.toList();
        assertTrue(supplierCalled.get()); // Now called
        assertEquals(Arrays.asList((byte) 1, (byte) 2), result);
    }

    // Test for ofNullable(Byte e)
    @Test
    public void testOfNullable() {
        List<Byte> result = ByteStream.ofNullable((byte) 10).toList();
        assertEquals(Arrays.asList((byte) 10), result);

        result = ByteStream.ofNullable(null).toList();
        assertTrue(result.isEmpty());
    }

    // Test for of(byte... a)
    @Test
    public void testOfBytes() {
        List<Byte> result = createByteStream((byte) 1, (byte) 2, (byte) 3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertTrue(createByteStream().toList().isEmpty());
    }

    // Test for of(byte[] a, int startIndex, int endIndex)
    @Test
    public void testOfBytesRange() {
        byte[] data = { 10, 20, 30, 40, 50 };
        List<Byte> result = createByteStream(data, 1, 4).toList();
        assertEquals(Arrays.asList((byte) 20, (byte) 30, (byte) 40), result);

        assertTrue(createByteStream(data, 0, 0).toList().isEmpty());
    }

    // Test for of(Byte[] a)
    @Test
    public void testOfBoxedBytes() {
        Byte[] data = { 1, 2, 3 };
        List<Byte> result = createByteStream(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    // Test for of(Byte[] a, int startIndex, int endIndex)
    @Test
    public void testOfBoxedBytesRange() {
        Byte[] data = { 10, 20, 30, 40, 50 };
        List<Byte> result = createByteStream(data, 1, 4).toList();
        assertEquals(Arrays.asList((byte) 20, (byte) 30, (byte) 40), result);
    }

    // Test for of(Collection<Byte> c)
    @Test
    public void testOfCollection() {
        Collection<Byte> data = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = createByteStream(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    // Test for of(ByteIterator iterator)
    @Test
    public void testOfByteIterator() {
        ByteIterator iterator = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = createByteStream(iterator).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    // Test for of(ByteBuffer buf)
    @Test
    public void testOfByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 10, 20, 30, 40 });
        buffer.position(1);
        buffer.limit(3);
        List<Byte> result = ByteStream.of(buffer).toList();
        assertEquals(Arrays.asList((byte) 20, (byte) 30), result);
    }

    // Test for of(File file) - Requires a temporary file
    @Test
    public void testOfFile() throws IOException {
        File tempFile = File.createTempFile("bytestream_test", ".tmp");
        IOUtil.write(new byte[] { 1, 2, 3 }, tempFile);
        try (ByteStream stream = ByteStream.of(tempFile)) {
            List<Byte> result = stream.toList();
            assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        } finally {
            tempFile.delete();
        }
    }

    // Test for of(InputStream is)
    @Test
    public void testOfInputStream() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        try (ByteStream stream = ByteStream.of(is)) {
            List<Byte> result = stream.toList();
            assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        }
    }

    // Test for flatten(byte[][] a)
    @Test
    public void testFlatten2DArray() {
        byte[][] data = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        List<Byte> result = ByteStream.flatten(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    // Test for flatten(byte[][] a, boolean vertically)
    @Test
    public void testFlatten2DArrayVertically() {
        byte[][] data = { { 1, 2, 3 }, { 4, 5, 6 } };
        List<Byte> result = ByteStream.flatten(data, true).toList(); // Column-major
        assertEquals(Arrays.asList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), result);

        result = ByteStream.flatten(data, false).toList(); // Row-major (same as default)
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    // Test for flatten(byte[][] a, byte valueForAlignment, boolean vertically)
    @Test
    public void testFlatten2DArrayWithAlignment() {
        byte[][] data = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        List<Byte> result = ByteStream.flatten(data, (byte) 0, false).toList(); // Row-major with padding
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 0, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 0, (byte) 0), result);

        result = ByteStream.flatten(data, (byte) 0, true).toList(); // Column-major with padding
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0), result);
    }

    // Test for flatten(byte[][][] a)
    @Test
    public void testFlatten3DArray() {
        byte[][][] data = { { { 1, 2 } }, { { 3, 4, 5 } } };
        List<Byte> result = ByteStream.flatten(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    // Test for range(byte startInclusive, byte endExclusive)
    @Test
    public void testRange() {
        List<Byte> result = ByteStream.range((byte) 1, (byte) 5).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
        assertTrue(ByteStream.range((byte) 5, (byte) 1).toList().isEmpty());
    }

    // Test for range(byte startInclusive, byte endExclusive, byte by)
    @Test
    public void testRangeBy() {
        List<Byte> result = ByteStream.range((byte) 1, (byte) 10, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result);

        result = ByteStream.range((byte) 10, (byte) 1, (byte) -2).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 8, (byte) 6, (byte) 4, (byte) 2), result);

        assertTrue(ByteStream.range((byte) 1, (byte) 5, (byte) -1).toList().isEmpty());
    }

    // Test for rangeClosed(byte startInclusive, byte endInclusive)
    @Test
    public void testRangeClosed() {
        List<Byte> result = ByteStream.rangeClosed((byte) 1, (byte) 5).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
        assertTrue(ByteStream.rangeClosed((byte) 5, (byte) 1).toList().isEmpty());
    }

    // Test for rangeClosed(byte startInclusive, byte endInclusive, byte by)
    @Test
    public void testRangeClosedBy() {
        List<Byte> result = ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result); // 10 is not included because it's even

        result = ByteStream.rangeClosed((byte) 1, (byte) 9, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result);

        result = ByteStream.rangeClosed((byte) 10, (byte) 1, (byte) -2).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 8, (byte) 6, (byte) 4, (byte) 2), result);
    }

    // Test for repeat(byte element, long n)
    @Test
    public void testRepeat() {
        List<Byte> result = ByteStream.repeat((byte) 7, 3).toList();
        assertEquals(Arrays.asList((byte) 7, (byte) 7, (byte) 7), result);
        assertTrue(ByteStream.repeat((byte) 7, 0).toList().isEmpty());
    }

    // Test for random()
    @Test
    public void testRandom() {
        List<Byte> result = ByteStream.random().limit(5).toList();
        assertEquals(5, result.size());
        for (byte b : result) {
            assertTrue(b >= Byte.MIN_VALUE && b <= Byte.MAX_VALUE);
        }
    }

    // Test for iterate(BooleanSupplier hasNext, ByteSupplier next)
    @Test
    public void testIterateBooleanSupplierByteSupplier() {
        AtomicReference<Byte> current = new AtomicReference<>((byte) 0);
        List<Byte> result = ByteStream.iterate(() -> current.get() < 3, () -> {
            byte val = current.get();
            current.set((byte) (val + 1));
            return val;
        }).toList();
        assertEquals(Arrays.asList((byte) 0, (byte) 1, (byte) 2), result);
    }

    // Test for iterate(byte init, BooleanSupplier hasNext, ByteUnaryOperator f)
    @Test
    public void testIterateInitBooleanSupplierByteUnaryOperator() {
        AtomicReference<Byte> currentVal = new AtomicReference<>((byte) 1);
        List<Byte> result = ByteStream.iterate((byte) 1, () -> currentVal.get() <= 5, b -> {
            currentVal.set((byte) (b + 1));
            return (byte) (b + 1);
        }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    // Test for iterate(byte init, BytePredicate hasNext, ByteUnaryOperator f)
    @Test
    public void testIterateInitBytePredicateByteUnaryOperator() {
        List<Byte> result = ByteStream.iterate((byte) 1, b -> b < 5, b -> (byte) (b * 2)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 4), result);
    }

    // Test for iterate(byte init, ByteUnaryOperator f)
    @Test
    public void testIterateInitByteUnaryOperator() {
        List<Byte> result = ByteStream.iterate((byte) 1, b -> (byte) (b + 1)).limit(3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    // Test for generate(ByteSupplier s)
    @Test
    public void testGenerate() {
        AtomicReference<Byte> counter = new AtomicReference<>((byte) 0);
        List<Byte> result = ByteStream.generate(() -> {
            byte val = counter.get();
            counter.set((byte) (val + 1));
            return val;
        }).limit(3).toList();
        assertEquals(Arrays.asList((byte) 0, (byte) 1, (byte) 2), result);
    }

    // Test for concat(byte[]... a)
    @Test
    public void testConcatByteArrays() {
        List<Byte> result = ByteStream.concat(new byte[] { 1, 2 }, new byte[] { 3, 4, 5 }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    // Test for concat(ByteIterator... a)
    @Test
    public void testConcatByteIterators() {
        ByteIterator iter1 = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator iter2 = ByteIterator.of((byte) 3, (byte) 4);
        List<Byte> result = ByteStream.concat(iter1, iter2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    // Test for concat(ByteStream... a)
    @Test
    public void testConcatByteStreams() {
        ByteStream s1 = createByteStream((byte) 1, (byte) 2);
        ByteStream s2 = createByteStream((byte) 3, (byte) 4);
        List<Byte> result = ByteStream.concat(s1, s2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    // Test for concat(List<byte[]> c)
    @Test
    public void testConcatListOfByteArrays() {
        List<byte[]> listOfArrays = new ArrayList<>();
        listOfArrays.add(new byte[] { 1, 2 });
        listOfArrays.add(new byte[] { 3, 4, 5 });
        List<Byte> result = ByteStream.concat(listOfArrays).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    // Test for concat(Collection<? extends ByteStream> streams)
    @Test
    public void testConcatCollectionOfByteStreams() {
        Collection<ByteStream> streams = new ArrayList<>();
        streams.add(createByteStream((byte) 1, (byte) 2));
        streams.add(createByteStream((byte) 3, (byte) 4));
        List<Byte> result = ByteStream.concat(streams).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    // Test for concatIterators(Collection<? extends ByteIterator> byteIterators)
    @Test
    public void testConcatIteratorsCollection() {
        Collection<ByteIterator> iterators = new ArrayList<>();
        iterators.add(ByteIterator.of((byte) 1, (byte) 2));
        iterators.add(ByteIterator.of((byte) 3, (byte) 4));
        List<Byte> result = ByteStream.concatIterators(iterators).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    // Test for zip(byte[] a, byte[] b, ByteBinaryOperator zipFunction)
    @Test
    public void testZipByteArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 10, 20, 30, 40 };
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 33), result);
    }

    // Test for zip(byte[] a, byte[] b, byte[] c, ByteTernaryOperator zipFunction)
    @Test
    public void testZipThreeByteArrays() {
        byte[] a = { 1, 2 };
        byte[] b = { 10, 20 };
        byte[] c = { 50, 60 };
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (2 + 20 + 60)), result);
    }

    // Test for zip(ByteIterator a, ByteIterator b, ByteBinaryOperator zipFunction)
    @Test
    public void testZipByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22), result);
    }

    // Test for zip(ByteIterator a, ByteIterator b, ByteIterator c, ByteTernaryOperator zipFunction)
    @Test
    public void testZipThreeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1);
        ByteIterator b = ByteIterator.of((byte) 10);
        ByteIterator c = ByteIterator.of((byte) 50, (byte) 60);
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50)), result);
    }

    // Test for zip(ByteStream a, ByteStream b, ByteBinaryOperator zipFunction)
    @Test
    public void testZipByteStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22), result);
    }

    // Test for zip(ByteStream a, ByteStream b, ByteStream c, ByteTernaryOperator zipFunction)
    @Test
    public void testZipThreeByteStreams() {
        ByteStream a = createByteStream((byte) 1);
        ByteStream b = createByteStream((byte) 10);
        ByteStream c = createByteStream((byte) 50, (byte) 60);
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50)), result);
    }

    // Test for zip(Collection<? extends ByteStream> streams, ByteNFunction<Byte> zipFunction)
    @Test
    public void testZipCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 2), createByteStream((byte) 10, (byte) 20));
        ByteNFunction<Byte> zipFunction = bytes -> (byte) (bytes[0] + bytes[1]);
        List<Byte> result = ByteStream.zip(streams, zipFunction).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22), result);
    }

    // Test for zip(byte[] a, byte[] b, byte valueForNoneA, byte valueForNoneB, ByteBinaryOperator zipFunction)
    @Test
    public void testZipByteArraysWithDefaults() {
        byte[] a = { 1, 2 };
        byte[] b = { 10, 20, 30 };
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    // Test for zip(byte[] a, byte[] b, byte[] c, byte valueForNoneA, byte valueForNoneB, byte valueForNoneC, ByteTernaryOperator zipFunction)
    @Test
    public void testZipThreeByteArraysWithDefaults() {
        byte[] a = { 1 };
        byte[] b = { 10, 20 };
        byte[] c = { 50, 60, 70 };
        List<Byte> result = ByteStream.zip(a, b, c, (byte) 0, (byte) 0, (byte) 0, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (0 + 20 + 60), (byte) (0 + 0 + 70)), result);
    }

    // Test for zip(ByteIterator a, ByteIterator b, byte valueForNoneA, byte valueForNoneB, ByteBinaryOperator zipFunction)
    @Test
    public void testZipByteIteratorsWithDefaults() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    // Test for zip(ByteIterator a, ByteIterator b, ByteIterator c, byte valueForNoneA, byte valueForNoneB, byte valueForNoneC, ByteTernaryOperator zipFunction)
    @Test
    public void testZipThreeByteIteratorsWithDefaults() {
        ByteIterator a = ByteIterator.of((byte) 1);
        ByteIterator b = ByteIterator.of((byte) 10, (byte) 20);
        ByteIterator c = ByteIterator.of((byte) 50, (byte) 60, (byte) 300);
        List<Byte> result = ByteStream.zip(a, b, c, (byte) 0, (byte) 0, (byte) 0, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (0 + 20 + 60), (byte) (0 + 0 + 300)), result);
    }

    // Test for zip(ByteStream a, ByteStream b, byte valueForNoneA, byte valueForNoneB, ByteBinaryOperator zipFunction)
    @Test
    public void testZipByteStreamsWithDefaults() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    // Test for zip(ByteStream a, ByteStream b, ByteStream c, byte valueForNoneA, byte valueForNoneB, byte valueForNoneC, ByteTernaryOperator zipFunction)
    @Test
    public void testZipThreeByteStreamsWithDefaults() {
        ByteStream a = createByteStream((byte) 1);
        ByteStream b = createByteStream((byte) 10, (byte) 20);
        ByteStream c = createByteStream((byte) 50, (byte) 60, (byte) 300);
        List<Byte> result = ByteStream.zip(a, b, c, (byte) 0, (byte) 0, (byte) 0, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (0 + 20 + 60), (byte) (0 + 0 + 300)), result);
    }

    // Test for zip(Collection<? extends ByteStream> streams, byte[] valuesForNone, ByteNFunction<Byte> zipFunction)
    @Test
    public void testZipCollectionOfStreamsWithDefaults() {
        Collection<ByteStream> streams = Arrays.asList(createByteStream((byte) 1), createByteStream((byte) 10, (byte) 20));
        byte[] defaults = { 0, 0 };
        ByteNFunction<Byte> zipFunction = bytes -> (byte) (bytes[0] + bytes[1]);
        List<Byte> result = ByteStream.zip(streams, defaults, zipFunction).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 20), result); // Max length of streams is 2. (1+10), (0+20)
    }

    // Test for merge(byte[] a, byte[] b, ByteBiFunction<MergeResult> nextSelector)
    @Test
    public void testMergeByteArrays() {
        byte[] a = { 1, 5, 10 };
        byte[] b = { 2, 4, 11 };
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 4, (byte) 5, (byte) 10, (byte) 11), result);
    }

    // Test for merge(byte[] a, byte[] b, byte[] c, ByteBiFunction<MergeResult> nextSelector)
    @Test
    public void testMergeThreeByteArrays() {
        byte[] a = { 1, 7 };
        byte[] b = { 2, 5 };
        byte[] c = { 3, 4 };
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, c, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 7), result);
    }

    // Test for merge(ByteIterator a, ByteIterator b, ByteBiFunction<MergeResult> nextSelector)
    @Test
    public void testMergeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 5);
        ByteIterator b = ByteIterator.of((byte) 2, (byte) 6);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 5, (byte) 6), result);
    }

    // Test for merge(ByteIterator a, ByteIterator b, ByteIterator c, ByteBiFunction<MergeResult> nextSelector)
    @Test
    public void testMergeThreeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 7);
        ByteIterator b = ByteIterator.of((byte) 2, (byte) 5);
        ByteIterator c = ByteIterator.of((byte) 3, (byte) 4);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, c, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 7), result);
    }

    // Test for merge(ByteStream a, ByteStream b, ByteBiFunction<MergeResult> nextSelector)
    @Test
    public void testMergeByteStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 5);
        ByteStream b = createByteStream((byte) 2, (byte) 6);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 5, (byte) 6), result);
    }

    // Test for merge(ByteStream a, ByteStream b, ByteStream c, ByteBiFunction<MergeResult> nextSelector)
    @Test
    public void testMergeThreeByteStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 7);
        ByteStream b = createByteStream((byte) 2, (byte) 5);
        ByteStream c = createByteStream((byte) 3, (byte) 4);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, c, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 7), result);
    }

    // Test for merge(Collection<? extends ByteStream> streams, ByteBiFunction<MergeResult> nextSelector)
    @Test
    public void testMergeCollectionOfByteStreams() {
        Collection<ByteStream> streams = new ArrayList<>();
        streams.add(createByteStream((byte) 1, (byte) 8));
        streams.add(createByteStream((byte) 2, (byte) 7));
        streams.add(createByteStream((byte) 3, (byte) 6));
        streams.add(createByteStream((byte) 4, (byte) 5));
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(streams, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8), result);
    }

    @Test
    public void testFilter() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> result = byteStream.filter(b -> b % 2 == 0).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result);
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> droppedItems = new ArrayList<>();
        List<Byte> result = byteStream.filter(b -> b % 2 == 0, droppedItems::add).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), droppedItems);
    }

    @Test
    public void testTakeWhile() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 4, 5, 1, 2 });
        List<Byte> result = byteStream.takeWhile(b -> b < 4).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);

        byteStream = createByteStream(new byte[] { 5, 1, 2 });
        result = byteStream.takeWhile(b -> b < 4).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDropWhile() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 4, 5, 1, 2 });
        List<Byte> result = byteStream.dropWhile(b -> b < 4).toList();
        assertEquals(Arrays.asList((byte) 4, (byte) 5, (byte) 1, (byte) 2), result);

        byteStream = createByteStream(new byte[] { 5, 1, 2 });
        result = byteStream.dropWhile(b -> b < 4).toList();
        assertEquals(Arrays.asList((byte) 5, (byte) 1, (byte) 2), result);
    }

    @Test
    public void testDropWhileWithActionOnDroppedItem() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> droppedItems = new ArrayList<>();
        List<Byte> result = byteStream.dropWhile(b -> b < 3, droppedItems::add).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 4, (byte) 5), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), droppedItems);
    }

    @Test
    public void testSkipUntil() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 4, 5, 1, 2 });
        List<Byte> result = byteStream.skipUntil(b -> b == 4).toList();
        assertEquals(Arrays.asList((byte) 4, (byte) 5, (byte) 1, (byte) 2), result);

        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        result = byteStream.skipUntil(b -> b == 10).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDistinct() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 1, 4, 5 });
        List<Byte> result = byteStream.distinct().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testIntersection() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 4 });
        Collection<Byte> other = Arrays.asList((byte) 2, (byte) 2, (byte) 3, (byte) 5);
        List<Byte> result = byteStream.intersection(other).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testDifference() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 4 });
        Collection<Byte> other = Arrays.asList((byte) 2, (byte) 3, (byte) 5);
        List<Byte> result = byteStream.difference(other).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 4), result);
    }

    @Test
    public void testSymmetricDifference() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 4 });
        Collection<Byte> other = Arrays.asList((byte) 2, (byte) 3, (byte) 3, (byte) 5);
        List<Byte> result = byteStream.symmetricDifference(other).toList();
        List<Byte> expected = Arrays.asList((byte) 1, (byte) 2, (byte) 4, (byte) 3, (byte) 5);
        // The order for symmetric difference might vary slightly based on internal set iteration.
        // Convert to Set for comparison if order is not guaranteed by implementation.
        assertEquals(new HashSet<>(expected), new HashSet<>(result));
    }

    @Test
    public void testReversed() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> result = byteStream.reversed().toList();
        assertEquals(Arrays.asList((byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1), result);
    }

    @Test
    public void testRotated() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> result = byteStream.rotated(2).toList();
        assertEquals(Arrays.asList((byte) 4, (byte) 5, (byte) 1, (byte) 2, (byte) 3), result);

        result = createByteStream(TEST_ARRAY).rotated(-1).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 1), result);
    }

    @Test
    public void testShuffled() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> original = byteStream.toList();
        byteStream = createByteStream(TEST_ARRAY); // Recreate for shuffle
        List<Byte> shuffled = byteStream.shuffled().toList();
        assertEquals(original.size(), shuffled.size());
        assertTrue(original.containsAll(shuffled) && shuffled.containsAll(original));
        assertNotEquals(original, shuffled); // Unlikely to be equal for non-trivial cases
    }

    @Test
    public void testShuffledWithRandom() {
        byteStream = createByteStream(TEST_ARRAY);
        Random rnd = new Random(123);
        List<Byte> shuffled1 = byteStream.shuffled(rnd).toList();

        byteStream = createByteStream(TEST_ARRAY);
        rnd = new Random(123); // Same seed
        List<Byte> shuffled2 = byteStream.shuffled(rnd).toList();
        assertEquals(shuffled1, shuffled2); // Deterministic shuffle
    }

    @Test
    public void testSorted() {
        byteStream = createByteStream(new byte[] { 5, 1, 4, 2, 3 });
        List<Byte> result = byteStream.sorted().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testReverseSorted() {
        byteStream = createByteStream(new byte[] { 5, 1, 4, 2, 3 });
        List<Byte> result = byteStream.reverseSorted().toList();
        assertEquals(Arrays.asList((byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1), result);
    }

    @Test
    public void testCycled() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.cycled().limit(5).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 1, (byte) 2, (byte) 1), result);

        assertTrue(createByteStream(new byte[] {}).cycled().limit(5).toList().isEmpty());
    }

    @Test
    public void testCycledWithRounds() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.cycled(3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 1, (byte) 2, (byte) 1, (byte) 2), result);

        assertTrue(createByteStream(new byte[] {}).cycled(3).toList().isEmpty());
        assertTrue(createByteStream(new byte[] { 1, 2 }).cycled(0).toList().isEmpty());
    }

    @Test
    public void testIndexed() {
        byteStream = createByteStream(new byte[] { 10, 20, 30 });
        List<IndexedByte> result = byteStream.indexed().toList();
        assertEquals(3, result.size());
        assertEquals(IndexedByte.of((byte) 10, 0), result.get(0));
        assertEquals(IndexedByte.of((byte) 20, 1), result.get(1));
        assertEquals(IndexedByte.of((byte) 30, 2), result.get(2));
    }

    @Test
    public void testSkip() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> result = byteStream.skip(2).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 4, (byte) 5), result);

        result = createByteStream(new byte[] { 1, 2 }).skip(5).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipWithAction() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> skippedItems = new ArrayList<>();
        List<Byte> result = byteStream.skip(2, skippedItems::add).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 4, (byte) 5), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skippedItems);
    }

    @Test
    public void testLimit() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> result = byteStream.limit(3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);

        result = createByteStream(new byte[] { 1, 2 }).limit(5).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2), result);
    }

    @Test
    public void testStep() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        List<Byte> result = byteStream.step(2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7), result);

        result = createByteStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }).step(3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 4, (byte) 7), result);
    }

    @Test
    public void testRateLimited() {
        byteStream = createByteStream(TEST_ARRAY);
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.rateLimited(2.0).toList(); // 2 permits per second
        long endTime = System.currentTimeMillis();
        assertEquals(5, result.size());
        assertTrue((endTime - startTime) >= 2000, "Rate limit was not applied or was too fast"); // Expect at least 2 seconds for 5 items at 2/s
    }

    @Test
    public void testRateLimitedWithRateLimiter() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        RateLimiter limiter = RateLimiter.create(1.0); // 1 permit per second
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.rateLimited(limiter).toList();
        long endTime = System.currentTimeMillis();
        assertEquals(3, result.size());
        assertTrue((endTime - startTime) >= 2000, "Rate limiter not working as expected"); // At least 2 seconds for 3 items
    }

    @Test
    public void testDelay() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Duration delay = Duration.ofMillis(50);
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.delay(delay).toList();
        long endTime = System.currentTimeMillis();
        assertEquals(3, result.size());
        // Expect at least (n-1) * delay time, where n is the number of elements.
        // For 3 elements, 2 delays.
        assertTrue((endTime - startTime) >= 2 * 50, "Delay was not applied or was too short");
    }

    @Test
    public void testOnEach() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> peekingList = new ArrayList<>();
        List<Byte> result = byteStream.onEach(peekingList::add).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), peekingList);
    }

    @Test
    public void testPeek() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> peekingList = new ArrayList<>();
        List<Byte> result = byteStream.peek(peekingList::add).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), peekingList);
    }

    @Test
    public void testThrowIfEmpty() {
        byteStream = createByteStream(new byte[] { 1 });
        byteStream.throwIfEmpty().toList(); // Should not throw

        byteStream = createByteStream(new byte[] {});
        try {
            byteStream.throwIfEmpty().toList();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    public void testThrowIfEmptyWithCustomException() {
        byteStream = createByteStream(new byte[] {});
        RuntimeException customException = new RuntimeException("Stream is empty!");
        try {
            byteStream.throwIfEmpty(() -> customException).toList();
            fail("Should throw custom exception");
        } catch (RuntimeException e) {
            assertSame(customException, e);
        }
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2 });
        byteStream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertFalse(actionExecuted.get()); // Not empty, action not executed

        actionExecuted.set(false);
        byteStream = createByteStream(new byte[] {});
        byteStream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertTrue(actionExecuted.get()); // Empty, action executed
    }

    @Test
    public void testJoinDelimiter() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        String result = byteStream.join("-");
        assertEquals("1-2-3", result);

        result = createByteStream(new byte[] { 1 }).join("-");
        assertEquals("1", result);

        result = createByteStream(new byte[] {}).join("-");
        assertEquals("", result);
    }

    @Test
    public void testJoinDelimiterPrefixSuffix() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        String result = byteStream.join("-", "[", "]");
        assertEquals("[1-2-3]", result);

        result = createByteStream(new byte[] { 1 }).join("-", "[", "]");
        assertEquals("[1]", result);

        result = createByteStream(new byte[] {}).join("-", "[", "]");
        assertEquals("[]", result);
    }

    @Test
    public void testJoinToJoiner() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Joiner joiner = Joiner.with(" | ", "<", ">");
        Joiner resultJoiner = byteStream.joinTo(joiner);
        assertEquals("<1 | 2 | 3>", resultJoiner.toString());
    }

    @Test
    public void testPercentiles() {
        byteStream = createByteStream(new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
        Optional<Map<Percentage, Byte>> percentilesOpt = byteStream.percentiles();
        assertTrue(percentilesOpt.isPresent());
        Map<Percentage, Byte> percentiles = percentilesOpt.get();
        assertEquals((byte) 60, percentiles.get(Percentage._50).byteValue());
        assertEquals((byte) 20, percentiles.get(Percentage._10).byteValue());

        percentilesOpt = createByteStream(new byte[] {}).percentiles();
        assertFalse(percentilesOpt.isPresent());
    }

    @Test
    public void testCount() {
        byteStream = createByteStream(TEST_ARRAY);
        assertEquals(5L, byteStream.count());
        assertEquals(0L, createByteStream(new byte[] {}).count());
    }

    @Test
    public void testFirst() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        OptionalByte result = byteStream.first();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());

        result = createByteStream(new byte[] {}).first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        OptionalByte result = byteStream.last();
        assertTrue(result.isPresent());
        assertEquals((byte) 3, result.get());

        result = createByteStream(new byte[] {}).last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt() {
        byteStream = createByteStream(new byte[] { 10, 20, 30 });
        OptionalByte result = byteStream.elementAt(1);
        assertTrue(result.isPresent());
        assertEquals((byte) 20, result.get());

        result = createByteStream(new byte[] { 10, 20, 30 }).elementAt(10); // Out of bounds
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        byteStream = createByteStream(new byte[] { 10 });
        OptionalByte result = byteStream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals((byte) 10, result.get());

        byteStream = createByteStream(new byte[] {});
        result = byteStream.onlyOne();
        assertFalse(result.isPresent());

        byteStream = createByteStream(new byte[] { 1, 2 });
        try {
            byteStream.onlyOne();
            fail("Should throw TooManyElementsException");
        } catch (TooManyElementsException e) {
            // Expected
        }
    }

    @Test
    public void testToArray() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        byte[] result = byteStream.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        result = createByteStream(new byte[] {}).toArray();
        assertArrayEquals(new byte[] {}, result);
    }

    @Test
    public void testToList() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testToSet() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3 });
        Set<Byte> result = byteStream.toSet();
        assertEquals(new HashSet<>(Arrays.asList((byte) 1, (byte) 2, (byte) 3)), result);
    }

    @Test
    public void testToImmutableList() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.toImmutableList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        try {
            result.add((byte) 4);
            fail("Should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testToImmutableSet() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3 });
        Set<Byte> result = byteStream.toImmutableSet();
        assertEquals(new HashSet<>(Arrays.asList((byte) 1, (byte) 2, (byte) 3)), result);
        try {
            result.add((byte) 4);
            fail("Should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testToCollection() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ArrayList<Byte> result = byteStream.toCollection(ArrayList::new);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testToMultiset() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 3, 3 });
        Multiset<Byte> result = byteStream.toMultiset();
        assertEquals(1, result.getCount((byte) 1));
        assertEquals(2, result.getCount((byte) 2));
        assertEquals(3, result.getCount((byte) 3));
    }

    @Test
    public void testToMultisetWithSupplier() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 3, 3 });
        Multiset<Byte> result = byteStream.toMultiset(Suppliers.ofMultiset());
        assertEquals(1, result.getCount((byte) 1));
        assertEquals(2, result.getCount((byte) 2));
        assertEquals(3, result.getCount((byte) 3));
    }

    @Test
    public void testPrintln() {
        // This test only verifies that the method runs without error.
        // Verifying console output directly is generally not recommended in unit tests.
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        byteStream.println();
    }

    @Test
    public void testIterator() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteIterator iterator = byteStream.iterator();
        assertTrue(iterator.hasNext());
        assertEquals((byte) 1, iterator.nextByte());
        assertTrue(iterator.hasNext());
        assertEquals((byte) 2, iterator.nextByte());
        assertTrue(iterator.hasNext());
        assertEquals((byte) 3, iterator.nextByte());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIsParallel() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        assertFalse(byteStream.isParallel());
        assertTrue(byteStream.parallel().isParallel());
    }

    @Test
    public void testSequential() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).parallel();
        assertTrue(byteStream.isParallel());
        assertFalse(byteStream.sequential().isParallel());
    }

    @Test
    public void testParallel() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        assertFalse(byteStream.isParallel());
        assertTrue(byteStream.parallel().isParallel());
    }

    @Test
    public void testParallelMaxThreadNum() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream parallelStream = byteStream.parallel(2);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelExecutor() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        // Using a simple direct executor for testing purposes
        ByteStream parallelStream = byteStream.parallel(Runnable::run);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelMaxThreadNumExecutor() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        // Using a simple direct executor for testing purposes
        ByteStream parallelStream = byteStream.parallel(2, Runnable::run);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelSettings() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream.ParallelSettings settings = PS.create(2);
        ByteStream parallelStream = byteStream.parallel(settings);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testSpsFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(s -> s.mapToInt(b -> b * 10)).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel()); // Original stream remains sequential
    }

    @Test
    public void testSpsIntFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(2, s -> s.mapToInt(b -> b * 10)).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel());
    }

    @Test
    public void testSpsIntExecutorFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(2, Runnable::run, s -> s.mapToInt(b -> b * 10)).toList();
        assertEquals(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel());
    }

    @Test
    public void testPspFunction() {
        byteStream = createByteStream(new byte[] { 3, 1, 2 }).parallel(); // Start parallel
        List<Byte> result = byteStream.psp(s -> s.sorted()).toList(); // Sort sequentially
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertTrue(byteStream.isParallel()); // Stream remains parallel after psp
    }

    @Test
    public void testTransform() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<String> result = byteStream.transform(s -> s.mapToObj(b -> "Item" + b)).toList();
        assertEquals(Arrays.asList("Item1", "Item2", "Item3"), result);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Long> count = createByteStream(new byte[] { 1, 2, 3 }).applyIfNotEmpty(s -> s.count());
        assertTrue(count.isPresent());
        assertEquals(3L, count.get().longValue());

        count = createByteStream(new byte[] {}).applyIfNotEmpty(s -> s.count());
        assertFalse(count.isPresent());
    }

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
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.toList(); // Terminal operation should close the stream
        assertTrue(closed.get());

        closed.set(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.close(); // Explicit close
        assertTrue(closed.get());
    }

    @Test
    public void testCloseIdempotent() {
        AtomicBoolean closed = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.close();
        assertTrue(closed.get());
        closed.set(false); // Reset to check if it's called again
        byteStream.close();
        assertFalse(closed.get()); // Should not be called again
    }
}
