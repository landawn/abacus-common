package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;

public class IteratorCharStreamTest extends TestBase {

    private static final char[] TEST_ARRAY = new char[] { 'a', 'b', 'c', 'd', 'e' };
    private CharStream stream;
    private CharStream stream2;
    private CharStream stream3;

    @BeforeEach
    public void setUp() {
        stream = createCharStream(TEST_ARRAY);
        stream2 = createCharStream(TEST_ARRAY);
        stream3 = createCharStream(TEST_ARRAY);
    }

    protected CharStream createCharStream(char... array) {
        return CharStream.of(array).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(char[] array, int fromIndex, int toIndex) {
        return CharStream.of(array, fromIndex, toIndex).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(Character[] array) {
        return CharStream.of(array).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(Character[] array, int fromIndex, int toIndex) {
        return CharStream.of(array, fromIndex, toIndex).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(Collection<Character> coll) {
        return CharStream.of(coll).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(Collection<Character> coll, int fromIndex, int toIndex) {
        return CharStream.of(coll.toArray(new Character[coll.size()]), fromIndex, toIndex).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(CharIterator iter) {
        return CharStream.of(iter).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(File file) {
        return CharStream.of(file).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(Reader reader) {
        return CharStream.of(reader).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(Reader reader, boolean closeReader) {
        return CharStream.of(reader, closeReader).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(CharSequence str) {
        return CharStream.of(str).map(c -> (char) (c + 0));
    }

    protected CharStream createCharStream(CharSequence str, int fromIndex, int toIndex) {
        return CharStream.of(str, fromIndex, toIndex).map(c -> (char) (c + 0));
    }

    // Covers IteratorCharStream.kthLargest() on sorted stream
    @Test
    public void testKthLargest_Sorted() {
        OptionalChar result = createCharStream('c', 'a', 'b').sorted().kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals('b', result.get());
    }

    // Covers IteratorCharStream.distinct() - empty path
    @Test
    public void testDistinct_Empty() {
        assertEquals(0, createCharStream().distinct().count());
    }

    // Covers IteratorCharStream.flatmap(CharFunction<char[]>)
    @Test
    public void testFlatmap_NonEmpty() {
        java.util.List<Character> result = createCharStream('a', 'b').flatmap(c -> new char[] { c, Character.toUpperCase(c) }).boxed().toList();
        assertEquals(4, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('A'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('B'));
    }

    @Test
    public void testFilter() {
        List<Character> result = createCharStream('a', 'b', 'c', 'd', 'e').filter(c -> c > 'c').boxed().toList();
        assertEquals(Arrays.asList('d', 'e'), result);

        result = createCharStream(new char[] {}).filter(c -> true).boxed().toList();
        assertTrue(result.isEmpty());

        result = createCharStream('a', 'b', 'c').filter(c -> c > 'z').boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTakeWhile() {
        List<Character> result = stream.takeWhile(c -> c < 'd').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream('a', 'b', 'c').takeWhile(c -> c > 'z').boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] {}).takeWhile(c -> true).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testDropWhile() {
        List<Character> result = createCharStream('a', 'b', 'c', 'd', 'e').dropWhile(c -> c < 'c').boxed().toList();
        assertEquals(Arrays.asList('c', 'd', 'e'), result);

        result = createCharStream('a', 'b', 'c').dropWhile(c -> c < 'z').boxed().toList();
        assertTrue(result.isEmpty());

        result = createCharStream('a', 'b', 'c').dropWhile(c -> c > 'z').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testMap() {
        List<Character> result = stream.map(c -> Character.toUpperCase(c)).boxed().toList();
        assertEquals(Arrays.asList('A', 'B', 'C', 'D', 'E'), result);

        result = createCharStream(new char[] {}).map(c -> c).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testMapToInt() {
        List<Integer> result = createCharStream('A', 'B', 'C').mapToInt(c -> c - 'A').boxed().toList();
        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testMapToObj() {
        List<String> result = stream.mapToObj(c -> String.valueOf(c) + "!").toList();
        assertEquals(Arrays.asList("a!", "b!", "c!", "d!", "e!"), result);

        result = createCharStream(new char[] {}).mapToObj(c -> String.valueOf(c)).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<String> result = stream.applyIfNotEmpty(s -> s.mapToObj(c -> String.valueOf(c)).join(""));
        assertTrue(result.isPresent());
        assertEquals("abcde", result.get());

        result = createCharStream(new char[] {}).applyIfNotEmpty(s -> "not empty");
        assertFalse(result.isPresent());
    }

    @Test
    public void testFlatMap() {
        List<Character> result = createCharStream('a', 'b').flatMap(c -> CharStream.of(c, Character.toUpperCase(c))).boxed().toList();
        assertEquals(Arrays.asList('a', 'A', 'b', 'B'), result);

        result = createCharStream(new char[] {}).flatMap(c -> CharStream.of(c)).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmap() {
        List<Character> result = createCharStream('a', 'b').flatmap(c -> new char[] { c, Character.toUpperCase(c) }).boxed().toList();
        assertEquals(Arrays.asList('a', 'A', 'b', 'B'), result);
    }

    @Test
    public void testFlatMapToInt() {
        List<Integer> result = createCharStream(new char[] { '1', '2' }).flatMapToInt(c -> IntStream.of(c - '0', (c - '0') * 10)).boxed().toList();
        assertEquals(Arrays.asList(1, 10, 2, 20), result);

        result = createCharStream(new char[] {}).flatMapToInt(c -> IntStream.of(c)).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testFlatMapToObj() {
        List<String> result = createCharStream('a', 'b').flatMapToObj(c -> Stream.of(String.valueOf(c), String.valueOf(c).toUpperCase())).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);
    }

    @Test
    public void testFlatmapToObj() {
        List<String> result = createCharStream('a', 'b').flatmapToObj(c -> Arrays.asList(String.valueOf(c), String.valueOf(c).toUpperCase())).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);
    }

    @Test
    public void testDistinct() {
        List<Character> result = createCharStream('a', 'b', 'a', 'c', 'b').distinct().boxed().toList();
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList('a', 'b', 'c')));

        result = createCharStream(new char[] {}).distinct().boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIterateInitCharUnaryOperator() {
        List<Character> result = CharStream.iterate('0', c -> (char) (c + 1)).limit(5).boxed().toList();
        assertEquals(Arrays.asList('0', '1', '2', '3', '4'), result);
    }

    @Test
    public void testOfCharBuffer() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 't', 'e', 's', 't' });
        buffer.position(1).limit(3);
        CharStream stream1 = createCharStream(buffer);
        assertEquals(Arrays.asList('e', 's'), stream1.boxed().toList());

        CharStream stream2 = createCharStream((CharBuffer) null);
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testRandom() {
        assertEquals(5, CharStream.random().limit(5).count());
        CharStream.random().limit(5).forEach(c -> assertTrue(c >= Character.MIN_VALUE && c <= Character.MAX_VALUE));
    }

    @Test
    public void testLimit() {
        List<Character> result = createCharStream('a', 'b', 'c', 'd', 'e').limit(3).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream('a', 'b').limit(0).boxed().toList();
        assertTrue(result.isEmpty());

        result = createCharStream('a', 'b').limit(10).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testSkip() {
        List<Character> result = createCharStream('a', 'b', 'c', 'd', 'e').skip(2).boxed().toList();
        assertEquals(Arrays.asList('c', 'd', 'e'), result);

        result = createCharStream('a', 'b', 'c').skip(0).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream('a', 'b').skip(10).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOnEach() {
        List<Character> sideEffect = new java.util.ArrayList<>();
        List<Character> result = createCharStream('a', 'b', 'c').onEach(c -> sideEffect.add(c)).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
        assertEquals(Arrays.asList('a', 'b', 'c'), sideEffect);
    }

    @Test
    public void testForEach() {
        List<Character> result = new java.util.ArrayList<>();
        createCharStream('a', 'b', 'c').forEach(c -> result.add(c));
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result.clear();
        createCharStream(new char[] {}).forEach(c -> result.add(c));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToArray() {
        char[] result = createCharStream('x', 'y', 'z').toArray();
        assertEquals(3, result.length);
        assertEquals('x', result[0]);
        assertEquals('y', result[1]);
        assertEquals('z', result[2]);

        result = createCharStream(new char[] {}).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testToCharList() {
        CharList list = stream.toCharList();
        assertEquals(CharList.of('a', 'b', 'c', 'd', 'e'), list);

        list = createCharStream(new char[] {}).toCharList();
        assertEquals(0, list.size());
    }

    @Test
    public void testCycledRounds() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).cycled(3).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'a', 'b', 'a', 'b'), result);

        result = createCharStream(new char[] { 'a', 'b' }).cycled(0).boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] { 'a', 'b' }).cycled(1).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);

        result = createCharStream(new char[] {}).cycled(5).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        CharStream deferredStream = CharStream.defer(() -> {
            counter.incrementAndGet();
            return createCharStream(new char[] { 'd', 'e', 'f' });
        });
        assertEquals(0, counter.get());
        List<Character> result = deferredStream.boxed().toList();
        assertEquals(1, counter.get());
        assertEquals(Arrays.asList('d', 'e', 'f'), result);
    }

    @Test
    public void testFlatten2DArrayWithAlignment() {
        char[][] data = { { 'a', 'b' }, { 'c', 'd', 'e' } };
        char padding = '-';
        List<Character> result = CharStream.flatten(data, padding, false).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', '-', 'c', 'd', 'e'), result);

        result = CharStream.flatten(data, padding, true).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'b', 'd', '-', 'e'), result);

        result = CharStream.flatten(new char[][] { { 'x' } }, padding, false).boxed().toList();
        assertEquals(Arrays.asList('x'), result);
    }

    @Test
    public void testRangeClosedWithStep() {
        List<Character> result = CharStream.rangeClosed('a', 'e', 2).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e'), result);

        result = CharStream.rangeClosed('e', 'a', -2).boxed().toList();
        assertEquals(Arrays.asList('e', 'c', 'a'), result);

        result = CharStream.rangeClosed('a', 'b', 5).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testIterateInitBooleanSupplierCharUnaryOperator() {
        AtomicInteger callCount = new AtomicInteger(0);
        CharStream iteratedStream = CharStream.iterate('A', () -> callCount.incrementAndGet() <= 3, c -> (char) (c + 1));
        List<Character> result = iteratedStream.boxed().toList();
        assertEquals(Arrays.asList('A', 'B', 'C'), result);
    }

    @Test
    public void testConcatCharIterators() {
        CharIterator iter1 = CharList.of('a', 'b').iterator();
        CharIterator iter2 = CharList.of('c', 'd').iterator();
        List<Character> result = CharStream.concat(iter1, iter2).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);
    }

    @Test
    public void testConcatListOfCharArrays() {
        List<char[]> listOfArrays = Arrays.asList(new char[] { 'h', 'i' }, new char[] { 'j', 'k' });
        List<Character> result = CharStream.concat(listOfArrays).boxed().toList();
        assertEquals(Arrays.asList('h', 'i', 'j', 'k'), result);
    }

    @Test
    public void testConcatCollectionOfCharIterators() {
        Collection<CharIterator> iterators = Arrays.asList(CharList.of('u', 'v').iterator(), CharList.of('w').iterator());
        List<Character> result = CharStream.concatIterators(iterators).boxed().toList();
        assertEquals(Arrays.asList('u', 'v', 'w'), result);
    }

    @Test
    public void testZipThreeCharArrays() {
        char[] arr1 = { '1', '2', '3' };
        char[] arr2 = { 'a', 'b' };
        char[] arr3 = { 'x', 'y', 'z' };
        List<Character> result = CharStream.zip(arr1, arr2, arr3, (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('2' + 'b' + 'y')), result);
    }

    @Test
    public void testZipThreeCharIterators() {
        CharIterator iter1 = CharList.of('1', '2').iterator();
        CharIterator iter2 = CharList.of('a', 'b').iterator();
        CharIterator iter3 = CharList.of('X').iterator();
        List<Character> result = CharStream.zip(iter1, iter2, iter3, (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'X')), result);
    }

    @Test
    public void testZipThreeCharStreams() {
        CharStream stream1 = createCharStream(new char[] { '1', '2', '3' });
        CharStream stream2 = createCharStream(new char[] { 'a', 'b' });
        CharStream stream3 = createCharStream(new char[] { 'x', 'y', 'z' });
        List<Character> result = CharStream.zip(stream1, stream2, stream3, (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('2' + 'b' + 'y')), result);
    }

    @Test
    public void testMergeThreeCharArrays() {
        char[] arr1 = { '1', '4' };
        char[] arr2 = { '2', '5' };
        char[] arr3 = { '3', '6' };
        List<Character> result = CharStream.merge(arr1, arr2, arr3, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('1', '2', '3', '4', '5', '6'), result);
    }

    @Test
    public void testMergeThreeCharIterators() {
        CharIterator iter1 = CharList.of('a', 'd').iterator();
        CharIterator iter2 = CharList.of('b', 'e').iterator();
        CharIterator iter3 = CharList.of('c', 'f').iterator();
        List<Character> result = CharStream.merge(iter1, iter2, iter3, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);
    }

    @Test
    public void testMergeThreeCharStreams() {
        CharStream stream1 = createCharStream(new char[] { '1', '2' });
        CharStream stream2 = createCharStream(new char[] { '3' });
        CharStream stream3 = createCharStream(new char[] { '4', '5' });
        List<Character> result = CharStream.merge(stream1, stream2, stream3, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .boxed()
                .toList();
        assertEquals(Arrays.asList('1', '2', '3', '4', '5'), result);
    }

    @Test
    public void testStep() {
        List<Character> result = createCharStream('a', 'b', 'c', 'd', 'e').step(2).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e'), result);

        result = createCharStream('a', 'b', 'c').step(5).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testBoxed() {
        List<Character> result = createCharStream('a', 'b', 'c').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testOfCharArray() {
        CharStream stream1 = createCharStream('a', 'b', 'c');
        assertEquals(Arrays.asList('a', 'b', 'c'), stream1.boxed().toList());

        CharStream stream2 = createCharStream();
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharSequence() {
        CharStream stream1 = createCharStream("hello");
        assertEquals(Arrays.asList('h', 'e', 'l', 'l', 'o'), stream1.boxed().toList());

        CharStream stream2 = createCharStream("");
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharacterArray() {
        Character[] data = { 'X', 'Y', 'Z' };
        CharStream stream1 = createCharStream(data);
        assertEquals(Arrays.asList('X', 'Y', 'Z'), stream1.boxed().toList());

        CharStream stream2 = createCharStream(new Character[] {});
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharacterCollection() {
        List<Character> list = Arrays.asList('1', '2', '3');
        CharStream stream1 = createCharStream(list);
        assertEquals(Arrays.asList('1', '2', '3'), stream1.boxed().toList());

        CharStream stream2 = createCharStream(Arrays.asList());
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testFlatten2DArray() {
        char[][] data = { { 'a', 'b' }, { 'c', 'd', 'e' } };
        List<Character> result = CharStream.flatten(data).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);

        result = CharStream.flatten(new char[][] {}).boxed().toList();
        assertTrue(result.isEmpty());

        result = CharStream.flatten(new char[][] { { 'a' }, {} }).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testRange() {
        List<Character> result = CharStream.range('a', 'd').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = CharStream.range('d', 'a').boxed().toList();
        assertTrue(result.isEmpty());

        result = CharStream.range('a', 'a').boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeat() {
        List<Character> result = CharStream.repeat('X', 3).boxed().toList();
        assertEquals(Arrays.asList('X', 'X', 'X'), result);

        result = CharStream.repeat('Y', 0).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIterateBooleanSupplierCharSupplier() {
        AtomicInteger count = new AtomicInteger(0);
        CharStream iteratedStream = CharStream.iterate(() -> count.get() < 3, () -> (char) ('a' + count.getAndIncrement()));
        List<Character> result = iteratedStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = CharStream.iterate(() -> false, () -> 'x').boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoCharArraysWithNoneValues() {
        char[] arr1 = { '1', '2' };
        char[] arr2 = { 'a', 'b', 'c' };
        List<Character> result = CharStream.zip(arr1, arr2, 'X', 'Y', (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a'), (char) ('2' + 'b'), (char) ('X' + 'c')), result);
    }

    @Test
    public void testZipTwoCharIteratorsWithNoneValues() {
        CharIterator iter1 = CharList.of('A', 'B').iterator();
        CharIterator iter2 = CharList.of('C', 'D', 'E').iterator();
        List<Character> result = CharStream.zip(iter1, iter2, 'X', 'Y', (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('A' + 'C'), (char) ('B' + 'D'), (char) ('X' + 'E')), result);
    }

    @Test
    public void testZipTwoCharStreamsWithNoneValues() {
        CharStream stream1 = createCharStream(new char[] { 'F', 'G' });
        CharStream stream2 = createCharStream(new char[] { 'H', 'I', 'J' });
        List<Character> result = CharStream.zip(stream1, stream2, 'U', 'V', (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('F' + 'H'), (char) ('G' + 'I'), (char) ('U' + 'J')), result);
    }

    @Test
    public void testZipCollectionOfCharStreamsWithNoneValues() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { '1' }), createCharStream(new char[] { 'a', 'b' }));
        char[] valuesForNone = { 'X', 'Y' };
        List<Character> result = CharStream.zip(streams, valuesForNone, chars -> (char) (chars[0] + chars[1])).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a'), (char) ('X' + 'b')), result);
    }

    @Test
    public void testToList() {
        List<Character> result = createCharStream('a', 'b', 'c').toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream(new char[] {}).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSorted() {
        List<Character> result = createCharStream('c', 'a', 'b').sorted().boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream(new char[] {}).sorted().boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOfReader() throws IOException {
        Reader reader = new java.io.StringReader("abc");
        CharStream stream1 = createCharStream(reader);
        assertEquals(Arrays.asList('a', 'b', 'c'), stream1.boxed().toList());
        reader.read();
    }

    @Test
    public void testToSet() {
        Set<Character> set = createCharStream(new char[] { 'a', 'b', 'a', 'c' }).toSet();
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList('a', 'b', 'c')));

        set = createCharStream(new char[] {}).toSet();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testToCollection() {
        java.util.LinkedList<Character> result = createCharStream('a', 'b', 'c').toCollection(java.util.LinkedList::new);
        assertTrue(result instanceof java.util.LinkedList);
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testToMultisetWithSupplier() {
        Multiset<Character> result = createCharStream('a', 'b', 'a').toMultiset(Multiset::new);
        assertEquals(2, result.count('a'));
        assertEquals(1, result.count('b'));
    }

    @Test
    public void testToMultiset() {
        Multiset<Character> multiset = createCharStream(new char[] { 'a', 'b', 'a', 'c', 'b' }).toMultiset();
        assertEquals(5, multiset.size());
        assertEquals(2, multiset.count('a'));
        assertEquals(2, multiset.count('b'));
        assertEquals(1, multiset.count('c'));

        multiset = createCharStream(new char[] {}).toMultiset();
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testToMapKeyAndValueMapper() {
        Map<String, Integer> result = createCharStream('a', 'b', 'c').toMap(c -> String.valueOf(c), c -> (int) c);
        assertEquals(3, result.size());
        assertEquals(97, (int) result.get("a"));
        assertEquals(98, (int) result.get("b"));
        assertEquals(99, (int) result.get("c"));
    }

    @Test
    public void testToMapKeyAndValueMapperAndMapFactory() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b' }).toMap(c -> String.valueOf(c), c -> (int) c, () -> N.newHashMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testToMapKeyAndValueMapperMergeFunctionAndMapFactory() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1 + v2,
                () -> N.newLinkedHashMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupToWithMapFactory() {
        Map<Boolean, List<Character>> result = createCharStream(new char[] { 'a', 'b', 'c', 'd' }).groupTo(c -> c % 2 == 0, Collectors.toList(),
                Suppliers.ofTreeMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Character>> result = createCharStream('a', 'b', 'c', 'd').groupTo(c -> c <= 'b', java.util.stream.Collectors.toList(),
                Suppliers.ofLinkedHashMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testReduceIdentity() {
        char sum = createCharStream(new char[] { '1', '2', '3' }).reduce('0', (c1, c2) -> (char) (c1 + c2 - '0'));
        assertEquals((char) ('0' + '1' + '2' + '3' - '0' - '0' - '0'), sum);

        sum = createCharStream(new char[] {}).reduce('X', (c1, c2) -> c1);
        assertEquals('X', sum);
    }

    @Test
    public void testReduceNoIdentity() {
        OptionalChar result = createCharStream('a', 'b', 'c').reduce((c1, c2) -> (char) Math.max(c1, c2));
        assertTrue(result.isPresent());
        assertEquals('c', result.get());

        result = createCharStream(new char[] {}).reduce((c1, c2) -> c1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testCollectSupplierAccumulatorCombiner() {
        StringBuilder sb = createCharStream('a', 'b', 'c').collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals("abc", sb.toString());

        sb = createCharStream(new char[] {}).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals("", sb.toString());
    }

    @Test
    public void testMin() {
        OptionalChar min = createCharStream('c', 'a', 'b').min();
        assertTrue(min.isPresent());
        assertEquals('a', min.get());

        min = createCharStream(new char[] {}).min();
        assertFalse(min.isPresent());
    }

    // Covers the isSorted() path in IteratorCharStream.min()
    @Test
    public void testMin_SortedStream() {
        OptionalChar result = createCharStream('c', 'a', 'b').sorted().min();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testMax() {
        OptionalChar max = stream.max();
        assertTrue(max.isPresent());
        assertEquals('e', max.get());

        max = createCharStream(new char[] {}).max();
        assertFalse(max.isPresent());

        max = createCharStream(new char[] { 'a', 'c', 'b' }).max();
        assertEquals('c', max.get());
    }

    // Covers the isSorted() path in IteratorCharStream.max() - uses sorted iterator stream
    @Test
    public void testMax_SortedStream() {
        OptionalChar result = createCharStream('c', 'a', 'b').sorted().max();
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testKthLargest() {
        OptionalChar result = createCharStream('a', 'c', 'e', 'b', 'd').kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals('d', result.get());

        result = createCharStream('a').kthLargest(2);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargestInvalidK() {
        assertThrows(IllegalArgumentException.class, () -> createCharStream(new char[] { 'a' }).kthLargest(0));
    }

    @Test
    public void testSum() {
        int sum = createCharStream('a', 'b', 'c').sum();
        assertEquals('a' + 'b' + 'c', sum);
    }

    @Test
    public void testAverage() {
        OptionalDouble avg = createCharStream(new char[] { '1', '2', '3' }).average();
        assertTrue(avg.isPresent());
        assertEquals(50.0, avg.getAsDouble(), 0.001);

        avg = createCharStream(new char[] {}).average();
        assertFalse(avg.isPresent());
    }

    @Test
    public void testOnClose() {
        AtomicInteger closeCount = new AtomicInteger(0);
        CharStream closableStream = createCharStream('a', 'b', 'c').onClose(() -> closeCount.incrementAndGet());
        closableStream.count();
        assertEquals(1, closeCount.get());

        closeCount.set(0);
        closableStream = createCharStream('a', 'b', 'c').onClose(() -> closeCount.incrementAndGet()).onClose(() -> closeCount.incrementAndGet());
        closableStream.toList();
        assertEquals(2, closeCount.get());

        closeCount.set(0);
        closableStream = createCharStream(new char[] { 'a' }).onClose(() -> closeCount.incrementAndGet());
        closableStream = closableStream.filter(c -> true);
        closableStream.forEach(c -> {
        });
        assertEquals(1, closeCount.get());

        closeCount.set(0);
        createCharStream(new char[] {}).onClose(() -> closeCount.incrementAndGet()).count();
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testCount() {
        assertEquals(5, createCharStream('a', 'b', 'c', 'd', 'e').count());
        assertEquals(0, createCharStream(new char[] {}).count());
    }

    @Test
    public void testsummaryStatistics() {
        CharSummaryStatistics stats = createCharStream(new char[] { 'z', 'y', 'x', 'w' }).summaryStatistics();
        assertEquals(4, stats.getCount());
        assertEquals('w', stats.getMin());
        assertEquals('z', stats.getMax());
        assertEquals('z' + 'y' + 'x' + 'w', stats.getSum());

        stats = createCharStream(new char[] {}).summaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createCharStream('a', 'b', 'c').anyMatch(c -> c == 'b'));
        assertFalse(createCharStream('a', 'b', 'c').anyMatch(c -> c == 'z'));
        assertFalse(createCharStream(new char[] {}).anyMatch(c -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(stream.allMatch(c -> c >= 'a' && c <= 'e'));
        assertFalse(stream2.allMatch(c -> c == 'c'));
        assertTrue(createCharStream(new char[] {}).allMatch(c -> true));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createCharStream('a', 'b', 'c').noneMatch(c -> c == 'z'));
        assertFalse(createCharStream('a', 'b', 'c').noneMatch(c -> c == 'b'));
        assertTrue(createCharStream(new char[] {}).noneMatch(c -> true));
    }

    @Test
    public void testFindFirst() {
        OptionalChar first = stream.findFirst(c -> c == 'c');
        assertTrue(first.isPresent());
        assertEquals('c', first.get());

        first = stream2.findFirst(c -> c == 'x');
        assertFalse(first.isPresent());

        first = createCharStream(new char[] {}).findFirst(c -> true);
        assertFalse(first.isPresent());
    }

    @Test
    public void testFindLast() {
        OptionalChar result = createCharStream('a', 'b', 'c', 'd', 'e').findLast(c -> c < 'd');
        assertTrue(result.isPresent());
        assertEquals('c', result.get());

        result = createCharStream('a', 'b').findLast(c -> c == 'z');
        assertFalse(result.isPresent());

        result = createCharStream(new char[] {}).findLast(c -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAsIntStream() {
        List<Integer> result = createCharStream(new char[] { 'A', 'B', 'C' }).asIntStream().boxed().toList();
        assertEquals(Arrays.asList(65, 66, 67), result);

        result = createCharStream(new char[] {}).asIntStream().boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testAppendIfEmpty() {
        List<Character> result = createCharStream(new char[] {}).appendIfEmpty(() -> CharStream.of('x', 'y')).boxed().toList();
        assertEquals(Arrays.asList('x', 'y'), result);

        result = createCharStream('a', 'b').appendIfEmpty(() -> CharStream.of('x', 'y')).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testIfEmpty() {
        AtomicInteger counter = new AtomicInteger(0);
        createCharStream(new char[] {}).ifEmpty(() -> counter.incrementAndGet()).count();
        assertEquals(1, counter.get());

        counter.set(0);
        stream.ifEmpty(() -> counter.incrementAndGet()).count();
        assertEquals(0, counter.get());
    }

    @Test
    public void testParallel() {
        CharStream pStream = createCharStream('a', 'b', 'c').parallel();
        assertTrue(pStream.isParallel());
        assertEquals(3, pStream.count());
    }

    @Test
    public void testStepWithZero() {
        assertThrows(IllegalArgumentException.class, () -> createCharStream(new char[] { 'a', 'b' }).step(0));
    }

    @Test
    public void testLast() {
        OptionalChar last = stream.last();
        assertTrue(last.isPresent());
        assertEquals('e', last.get());

        last = createCharStream(new char[] { 'x' }).last();
        assertTrue(last.isPresent());
        assertEquals('x', last.get());

        last = createCharStream(new char[] {}).last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testElementAt() {
        OptionalChar element = stream.elementAt(2);
        assertTrue(element.isPresent());
        assertEquals('c', element.get());

        element = stream2.elementAt(0);
        assertTrue(element.isPresent());
        assertEquals('a', element.get());

        element = stream3.elementAt(4);
        assertTrue(element.isPresent());
        assertEquals('e', element.get());

        element = createCharStream(new char[] {}).elementAt(0);
        assertFalse(element.isPresent());
    }

    @Test
    public void testOnlyOne() {
        OptionalChar one = createCharStream(new char[] { 'x' }).onlyOne();
        assertTrue(one.isPresent());
        assertEquals('x', one.get());

        one = createCharStream(new char[] {}).onlyOne();
        assertFalse(one.isPresent());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.range('a', 'c', 0));
    }

    @Test
    public void testRandomInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.random('c', 'a'));
    }

    @Test
    public void testGenerateNull() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.generate(null));
    }
}
