package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;

@Tag("new-test")
public class IteratorCharStream200Test extends TestBase {

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

    @Test
    public void testFilter() {
        List<Character> result = stream.filter(c -> c % 2 == 1).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e'), result);

        result = createCharStream(new char[] {}).filter(c -> true).boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] { 'x', 'y', 'z' }).filter(c -> c == 'y').boxed().toList();
        assertEquals(Arrays.asList('y'), result);
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
        List<Character> result = stream.dropWhile(c -> c < 'd').boxed().toList();
        assertEquals(Arrays.asList('d', 'e'), result);

        result = createCharStream('a', 'b', 'c').dropWhile(c -> c == 'x').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream(new char[] {}).dropWhile(c -> true).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testStep() {
        List<Character> result = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' }).step(2).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e', 'g'), result);

        result = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' }).step(3).boxed().toList();
        assertEquals(Arrays.asList('a', 'd', 'g'), result);

        result = createCharStream(new char[] { 'a' }).step(1).boxed().toList();
        assertEquals(Arrays.asList('a'), result);

        result = createCharStream(new char[] {}).step(1).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testStepWithZero() {
        assertThrows(IllegalArgumentException.class, () -> createCharStream(new char[] { 'a', 'b' }).step(0));
    }

    @Test
    public void testStepWithNegative() {
        assertThrows(IllegalArgumentException.class, () -> createCharStream(new char[] { 'a', 'b' }).step(-1));
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
        List<Integer> result = stream.mapToInt(c -> (int) c).boxed().toList();
        assertEquals(Arrays.asList(97, 98, 99, 100, 101), result);

        result = createCharStream(new char[] {}).mapToInt(c -> c).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testMapToObj() {
        List<String> result = stream.mapToObj(c -> String.valueOf(c) + "!").toList();
        assertEquals(Arrays.asList("a!", "b!", "c!", "d!", "e!"), result);

        result = createCharStream(new char[] {}).mapToObj(c -> String.valueOf(c)).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testFlatMap() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).flatMap(c -> createCharStream(new char[] { c, Character.toUpperCase(c) }))
                .boxed()
                .toList();
        assertEquals(Arrays.asList('a', 'A', 'b', 'B'), result);

        result = createCharStream(new char[] {}).flatMap(c -> createCharStream(new char[] { c })).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testFlatmapCharArray() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).flatmap(c -> new char[] { c, Character.toUpperCase(c) }).boxed().toList();
        assertEquals(Arrays.asList('a', 'A', 'b', 'B'), result);

        result = createCharStream(new char[] {}).flatmap(c -> new char[] { c }).boxed().toList();
        assertEquals(0, result.size());
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
        List<String> result = createCharStream(new char[] { 'x', 'y' })
                .flatMapToObj(c -> Stream.of(String.valueOf(c), String.valueOf(Character.toUpperCase(c))))
                .toList();
        assertEquals(Arrays.asList("x", "X", "y", "Y"), result);

        result = createCharStream(new char[] {}).flatMapToObj(c -> Stream.of(String.valueOf(c))).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testFlatmapToObjCollection() {
        List<String> result = createCharStream(new char[] { 'a', 'b' })
                .flatmapToObj(c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c))))
                .toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);

        result = createCharStream(new char[] {}).flatmapToObj(c -> Arrays.asList(String.valueOf(c))).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testToCharList() {
        CharList list = stream.toCharList();
        assertEquals(CharList.of('a', 'b', 'c', 'd', 'e'), list);

        list = createCharStream(new char[] {}).toCharList();
        assertEquals(0, list.size());
    }

    @Test
    public void testToList() {
        List<Character> list = stream.toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), list);

        list = createCharStream(new char[] {}).toList();
        assertTrue(list.isEmpty());
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
        List<Character> list = createCharStream('a', 'b', 'c').toCollection(ArrayList::new);
        assertEquals(Arrays.asList('a', 'b', 'c'), list);

        List<Character> emptyList = createCharStream(new char[] {}).toCollection(Suppliers.ofList());
        assertTrue(emptyList.isEmpty());
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
    public void testToMultisetWithSupplier() {
        Multiset<Character> multiset = createCharStream(new char[] { 'a', 'b', 'a' }).toMultiset(N::newMultiset);
        assertEquals(3, multiset.size());
        assertEquals(2, multiset.count('a'));
        assertEquals(1, multiset.count('b'));
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
    public void testToMapKeyAndValueMapperDuplicateKeys() {
        assertThrows(IllegalStateException.class, () -> createCharStream(new char[] { 'a', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c));
    }

    @Test
    public void testToMapKeyAndValueMapperAndMapFactory() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b' }).toMap(c -> String.valueOf(c), c -> (int) c, () -> N.newHashMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testToMapKeyAndValueMapperAndMergeFunction() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(97 + 97, (int) result.get("a"));
        assertEquals(98, (int) result.get("b"));
    }

    @Test
    public void testToMapKeyAndValueMapperMergeFunctionAndMapFactory() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1 + v2,
                () -> N.newLinkedHashMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Character>> result = createCharStream(new char[] { 'a', 'b', 'c', 'd' }).groupTo(c -> c % 2 == 0, Collectors.toList());
        assertEquals(2, result.size());
        assertEquals(Arrays.asList('b', 'd'), result.get(true));
        assertEquals(Arrays.asList('a', 'c'), result.get(false));
    }

    @Test
    public void testGroupToWithMapFactory() {
        Map<Boolean, List<Character>> result = createCharStream(new char[] { 'a', 'b', 'c', 'd' }).groupTo(c -> c % 2 == 0, Collectors.toList(),
                Suppliers.ofTreeMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testFirst() {
        OptionalChar first = stream.first();
        assertTrue(first.isPresent());
        assertEquals('a', first.get());

        first = createCharStream(new char[] {}).first();
        assertFalse(first.isPresent());
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
    public void testElementAtNegativePosition() {
        assertThrows(IllegalArgumentException.class, () -> stream.elementAt(-1));
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
    public void testOnlyOneTooManyElements() {
        assertThrows(TooManyElementsException.class, () -> createCharStream(new char[] { 'x', 'y' }).onlyOne());
    }

    @Test
    public void testReduceIdentity() {
        char sum = createCharStream(new char[] { '1', '2', '3' }).reduce('0', (c1, c2) -> (char) (c1 + c2 - '0'));
        assertEquals((char) ('0' + '1' + '2' + '3' - '0' - '0' - '0'), sum);

        sum = createCharStream(new char[] {}).reduce('X', (c1, c2) -> c1);
        assertEquals('X', sum);
    }

    @Test
    public void testReduce() {
        OptionalChar sum = createCharStream(new char[] { '1', '2', '3' }).reduce((c1, c2) -> (char) (c1 + c2 - '0'));
        assertTrue(sum.isPresent());
        assertEquals((char) ('1' + '2' + '3' - '0' - '0'), sum.get());

        OptionalChar emptySum = createCharStream(new char[] {}).reduce((c1, c2) -> c1);
        assertFalse(emptySum.isPresent());
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
        OptionalChar min = stream.min();
        assertTrue(min.isPresent());
        assertEquals('a', min.get());

        min = createCharStream(new char[] {}).min();
        assertFalse(min.isPresent());

        min = createCharStream(new char[] { 'z', 'y', 'x' }).min();
        assertEquals('x', min.get());
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

    @Test
    public void testKthLargest() {
        OptionalChar kl = createCharStream(new char[] { 'a', 'c', 'b', 'e', 'd' }).kthLargest(1);
        assertTrue(kl.isPresent());
        assertEquals('e', kl.get());

        kl = createCharStream(new char[] { 'a', 'c', 'b', 'e', 'd' }).kthLargest(3);
        assertTrue(kl.isPresent());
        assertEquals('c', kl.get());

        kl = createCharStream(new char[] { 'a' }).kthLargest(1);
        assertEquals('a', kl.get());

        kl = createCharStream(new char[] { 'a' }).kthLargest(2);
        assertFalse(kl.isPresent());

        kl = createCharStream(new char[] {}).kthLargest(1);
        assertFalse(kl.isPresent());
    }

    @Test
    public void testKthLargestInvalidK() {
        assertThrows(IllegalArgumentException.class, () -> createCharStream(new char[] { 'a' }).kthLargest(0));
    }

    @Test
    public void testSum() {
        int sum = createCharStream(new char[] { '1', '2', '3' }).sum();
        assertEquals(150, sum);

        sum = createCharStream(new char[] {}).sum();
        assertEquals(0, sum);
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
    public void testCount() {
        long count = stream.count();
        assertEquals(5, count);

        count = createCharStream(new char[] {}).count();
        assertEquals(0, count);
    }

    @Test
    public void testsummaryStatistics() {
        CharSummaryStatistics stats = createCharStream(new char[] { 'z', 'y', 'x', 'w' }).summaryStatistics();
        assertEquals(4, stats.getCount());
        assertEquals('w', stats.getMin());
        assertEquals('z', stats.getMax());
        assertEquals((int) 'z' + (int) 'y' + (int) 'x' + (int) 'w', stats.getSum().intValue());

        stats = createCharStream(new char[] {}).summaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testAnyMatch() {
        assertTrue(stream.anyMatch(c -> c == 'e'));
        assertFalse(stream2.anyMatch(c -> c == 'x'));
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
        assertTrue(stream.noneMatch(c -> c == 'z'));
        assertFalse(stream2.noneMatch(c -> c == 'c'));
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
        OptionalChar last = stream.findLast(c -> c == 'c');
        assertTrue(last.isPresent());
        assertEquals('c', last.get());

        last = stream2.findLast(c -> c == 'e');
        assertTrue(last.isPresent());
        assertEquals('e', last.get());

        last = stream3.findLast(c -> c == 'x');
        assertFalse(last.isPresent());

        last = createCharStream(new char[] {}).findLast(c -> true);
        assertFalse(last.isPresent());
    }

    @Test
    public void testAsIntStream() {
        List<Integer> result = createCharStream(new char[] { 'A', 'B', 'C' }).asIntStream().boxed().toList();
        assertEquals(Arrays.asList(65, 66, 67), result);

        result = createCharStream(new char[] {}).asIntStream().boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        List<Character> result = createCharStream(new char[] {}).appendIfEmpty(() -> createCharStream(new char[] { 'x', 'y' })).boxed().toList();
        assertEquals(Arrays.asList('x', 'y'), result);

        result = createCharStream(new char[] { 'a', 'b' }).appendIfEmpty(() -> createCharStream(new char[] { 'x', 'y' })).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);

        result = createCharStream(new char[] {}).appendIfEmpty(() -> createCharStream(new char[] {})).boxed().toList();
        assertEquals(0, result.size());
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
    public void testCycled() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).cycled().limit(5).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'a', 'b', 'a'), result);

        result = createCharStream(new char[] {}).cycled().limit(5).boxed().toList();
        assertEquals(0, result.size());
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
    public void testCycledNegativeRounds() {
        assertThrows(IllegalArgumentException.class, () -> createCharStream(new char[] { 'a' }).cycled(-1));
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
    public void testAcceptIfNotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);
        stream.acceptIfNotEmpty(s -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        counter.set(0);
        createCharStream(new char[] {}).acceptIfNotEmpty(s -> counter.incrementAndGet());
        assertEquals(0, counter.get());
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
    public void testEmpty() {
        CharStream emptyStream = CharStream.empty();
        assertFalse(emptyStream.iterator().hasNext());
        assertEquals(0, emptyStream.count());
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
    public void testOfNullable() {
        CharStream stream1 = CharStream.ofNullable('a');
        assertEquals(Arrays.asList('a'), stream1.boxed().toList());

        CharStream stream2 = CharStream.ofNullable(null);
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharArray() {
        CharStream stream1 = createCharStream('a', 'b', 'c');
        assertEquals(Arrays.asList('a', 'b', 'c'), stream1.boxed().toList());

        CharStream stream2 = createCharStream();
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharArrayRange() {
        char[] data = { 'a', 'b', 'c', 'd', 'e' };
        CharStream stream1 = createCharStream(data, 1, 4);
        assertEquals(Arrays.asList('b', 'c', 'd'), stream1.boxed().toList());

        CharStream stream2 = createCharStream(data, 0, 0);
        assertTrue(stream2.boxed().toList().isEmpty());

        try {
            createCharStream(data, -1, 2);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            createCharStream(data, 2, 1);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            createCharStream(data, 0, 10);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testOfCharSequence() {
        CharStream stream1 = createCharStream("hello");
        assertEquals(Arrays.asList('h', 'e', 'l', 'l', 'o'), stream1.boxed().toList());

        CharStream stream2 = createCharStream("");
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharSequenceRange() {
        CharSequence str = "world";
        CharStream stream1 = createCharStream(str, 1, 4);
        assertEquals(Arrays.asList('o', 'r', 'l'), stream1.boxed().toList());

        CharStream stream2 = createCharStream(str, 0, 0);
        assertTrue(stream2.boxed().toList().isEmpty());

        try {
            createCharStream(str, -1, 2);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
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
    public void testOfCharacterArrayRange() {
        Character[] data = { 'A', 'B', 'C', 'D' };
        CharStream stream1 = createCharStream(data, 1, 3);
        assertEquals(Arrays.asList('B', 'C'), stream1.boxed().toList());
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
    public void testOfCharIterator() {
        CharIterator iterator = CharList.of('x', 'y').iterator();
        CharStream stream1 = createCharStream(iterator);
        assertEquals(Arrays.asList('x', 'y'), stream1.boxed().toList());

        CharStream stream2 = createCharStream((CharIterator) null);
        assertTrue(stream2.boxed().toList().isEmpty());
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
    public void testOfFile() throws IOException {
        File tempFile = File.createTempFile("charstream_test", ".txt");
        tempFile.deleteOnExit();
        IOUtil.write("Hello World".toCharArray(), tempFile);

        CharStream stream1 = createCharStream(tempFile);
        assertEquals(Arrays.asList('H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'), stream1.boxed().toList());
    }

    @Test
    public void testOfReader() throws IOException {
        Reader reader = new java.io.StringReader("abc");
        CharStream stream1 = createCharStream(reader);
        assertEquals(Arrays.asList('a', 'b', 'c'), stream1.boxed().toList());
        reader.read();
    }

    @Test
    public void testOfReaderWithCloseOption() throws IOException {
        java.io.StringReader reader = new java.io.StringReader("xyz");
        CharStream stream1 = createCharStream(reader, true);
        assertEquals(Arrays.asList('x', 'y', 'z'), stream1.boxed().toList());

        try {
            reader.read();
            fail("Expected IOException due to closed reader");
        } catch (IOException e) {
        }
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
    public void testFlatten2DArrayVertically() {
        char[][] data = { { 'a', 'b' }, { 'c', 'd', 'e' } };
        List<Character> result = CharStream.flatten(data, true).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'b', 'd', 'e'), result);

        result = CharStream.flatten(new char[][] { { 'x' } }, true).boxed().toList();
        assertEquals(Arrays.asList('x'), result);
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
    public void testFlatten3DArray() {
        char[][][] data = { { { 'a', 'b' }, { 'c' } }, { { 'd' } } };
        List<Character> result = CharStream.flatten(data).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);

        result = CharStream.flatten(new char[][][] {}).boxed().toList();
        assertTrue(result.isEmpty());
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
    public void testRangeWithStep() {
        List<Character> result = CharStream.range('a', 'g', 2).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e'), result);

        result = CharStream.range('g', 'a', -2).boxed().toList();
        assertEquals(Arrays.asList('g', 'e', 'c'), result);

        result = CharStream.range('a', 'a', 1).boxed().toList();
        assertTrue(result.isEmpty());

        result = CharStream.range('a', 'b', 5).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.range('a', 'c', 0));
    }

    @Test
    public void testRangeClosed() {
        List<Character> result = CharStream.rangeClosed('a', 'c').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = CharStream.rangeClosed('a', 'a').boxed().toList();
        assertEquals(Arrays.asList('a'), result);

        result = CharStream.rangeClosed('c', 'a').boxed().toList();
        assertTrue(result.isEmpty());
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
    public void testRangeClosedWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.rangeClosed('a', 'c', 0));
    }

    @Test
    public void testRepeat() {
        List<Character> result = CharStream.repeat('X', 3).boxed().toList();
        assertEquals(Arrays.asList('X', 'X', 'X'), result);

        result = CharStream.repeat('Y', 0).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.repeat('A', -1));
    }

    @Test
    public void testRandom() {
        assertEquals(5, CharStream.random().limit(5).count());
        CharStream.random().limit(5).forEach(c -> assertTrue(c >= Character.MIN_VALUE && c <= Character.MAX_VALUE));
    }

    @Test
    public void testRandomRange() {
        char start = 'a';
        char end = 'z';
        assertEquals(10, CharStream.random(start, end).limit(10).count());
        CharStream.random(start, end).limit(10).forEach(c -> assertTrue(c >= start && c < end));
    }

    @Test
    public void testRandomInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.random('c', 'a'));
    }

    @Test
    public void testRandomCandidates() {
        char[] candidates = { 'x', 'y', 'z' };
        assertEquals(10, CharStream.random(candidates).limit(10).count());
        CharStream.random(candidates).limit(10).forEach(c -> assertTrue(c == 'x' || c == 'y' || c == 'z'));

        CharStream emptyRandom = CharStream.random(new char[] {});
        assertTrue(emptyRandom.boxed().toList().isEmpty());
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
    public void testIterateBooleanSupplierCharSupplierNull() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.iterate(null, () -> 'a'));
    }

    @Test
    public void testIterateInitBooleanSupplierCharUnaryOperator() {
        AtomicInteger callCount = new AtomicInteger(0);
        CharStream iteratedStream = CharStream.iterate('A', () -> callCount.incrementAndGet() <= 3, c -> (char) (c + 1));
        List<Character> result = iteratedStream.boxed().toList();
        assertEquals(Arrays.asList('A', 'B', 'C'), result);
    }

    @Test
    public void testIterateInitCharPredicateCharUnaryOperator() {
        CharStream iteratedStream = CharStream.iterate('A', c -> c <= 'C', c -> (char) (c + 1));
        List<Character> result = iteratedStream.boxed().toList();
        assertEquals(Arrays.asList('A', 'B', 'C'), result);

        result = CharStream.iterate('Z', c -> false, c -> c).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIterateInitCharUnaryOperator() {
        List<Character> result = CharStream.iterate('0', c -> (char) (c + 1)).limit(5).boxed().toList();
        assertEquals(Arrays.asList('0', '1', '2', '3', '4'), result);
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Character> result = CharStream.generate(() -> (char) ('X' + counter.getAndIncrement())).limit(3).boxed().toList();
        assertEquals(Arrays.asList('X', 'Y', 'Z'), result);
    }

    @Test
    public void testGenerateNull() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.generate(null));
    }

    @Test
    public void testConcatCharArrays() {
        char[] arr1 = { '1', '2' };
        char[] arr2 = { '3', '4', '5' };
        List<Character> result = CharStream.concat(arr1, arr2).boxed().toList();
        assertEquals(Arrays.asList('1', '2', '3', '4', '5'), result);

        result = CharStream.concat(new char[0][0]).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatCharIterators() {
        CharIterator iter1 = CharList.of('a', 'b').iterator();
        CharIterator iter2 = CharList.of('c', 'd').iterator();
        List<Character> result = CharStream.concat(iter1, iter2).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);
    }

    @Test
    public void testConcatCharStreams() {
        CharStream stream1 = createCharStream(new char[] { 'x', 'y' });
        CharStream stream2 = createCharStream(new char[] { 'z' });
        List<Character> result = CharStream.concat(stream1, stream2).boxed().toList();
        assertEquals(Arrays.asList('x', 'y', 'z'), result);
    }

    @Test
    public void testConcatListOfCharArrays() {
        List<char[]> listOfArrays = Arrays.asList(new char[] { 'h', 'i' }, new char[] { 'j', 'k' });
        List<Character> result = CharStream.concat(listOfArrays).boxed().toList();
        assertEquals(Arrays.asList('h', 'i', 'j', 'k'), result);
    }

    @Test
    public void testConcatCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { 'o', 'n', 'e' }), createCharStream(new char[] { 't', 'w', 'o' }));
        List<Character> result = CharStream.concat(streams).boxed().toList();
        assertEquals(Arrays.asList('o', 'n', 'e', 't', 'w', 'o'), result);
    }

    @Test
    public void testConcatCollectionOfCharIterators() {
        Collection<CharIterator> iterators = Arrays.asList(CharList.of('u', 'v').iterator(), CharList.of('w').iterator());
        List<Character> result = CharStream.concatIterators(iterators).boxed().toList();
        assertEquals(Arrays.asList('u', 'v', 'w'), result);
    }

    @Test
    public void testZipTwoCharArrays() {
        char[] arr1 = { '1', '2', '3' };
        char[] arr2 = { 'a', 'b' };
        List<Character> result = CharStream.zip(arr1, arr2, (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a'), (char) ('2' + 'b')), result);

        result = CharStream.zip(new char[] {}, new char[] { 'a' }, (c1, c2) -> c1).boxed().toList();
        assertTrue(result.isEmpty());
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
    public void testZipTwoCharIterators() {
        CharIterator iter1 = CharList.of('A', 'B', 'C').iterator();
        CharIterator iter2 = CharList.of('D', 'E').iterator();
        List<Character> result = CharStream.zip(iter1, iter2, (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('A' + 'D'), (char) ('B' + 'E')), result);
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
    public void testZipTwoCharStreams() {
        CharStream stream1 = createCharStream(new char[] { 'P', 'Q' });
        CharStream stream2 = createCharStream(new char[] { 'R', 'S', 'T' });
        List<Character> result = CharStream.zip(stream1, stream2, (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('P' + 'R'), (char) ('Q' + 'S')), result);
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
    public void testZipCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { 'A', 'B' }), createCharStream(new char[] { '1', '2', '3' }));
        List<Character> result = CharStream.zip(streams, chars -> (char) (chars[0] + chars[1])).boxed().toList();
        assertEquals(Arrays.asList((char) ('A' + '1'), (char) ('B' + '2')), result);
    }

    @Test
    public void testZipTwoCharArraysWithNoneValues() {
        char[] arr1 = { '1', '2' };
        char[] arr2 = { 'a', 'b', 'c' };
        List<Character> result = CharStream.zip(arr1, arr2, 'X', 'Y', (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a'), (char) ('2' + 'b'), (char) ('X' + 'c')), result);
    }

    @Test
    public void testZipThreeCharArraysWithNoneValues() {
        char[] arr1 = { '1', '2' };
        char[] arr2 = { 'a', 'b', 'c' };
        char[] arr3 = { 'x' };
        List<Character> result = CharStream.zip(arr1, arr2, arr3, 'X', 'Y', 'Z', (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('2' + 'b' + 'Z'), (char) ('X' + 'c' + 'Z')), result);
    }

    @Test
    public void testZipTwoCharIteratorsWithNoneValues() {
        CharIterator iter1 = CharList.of('A', 'B').iterator();
        CharIterator iter2 = CharList.of('C', 'D', 'E').iterator();
        List<Character> result = CharStream.zip(iter1, iter2, 'X', 'Y', (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('A' + 'C'), (char) ('B' + 'D'), (char) ('X' + 'E')), result);
    }

    @Test
    public void testZipThreeCharIteratorsWithNoneValues() {
        CharIterator iter1 = CharList.of('1').iterator();
        CharIterator iter2 = CharList.of('a', 'b').iterator();
        CharIterator iter3 = CharList.of('x', 'y', 'z').iterator();
        List<Character> result = CharStream.zip(iter1, iter2, iter3, 'X', 'Y', 'Z', (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('X' + 'b' + 'y'), (char) ('X' + 'Y' + 'z')), result);
    }

    @Test
    public void testZipTwoCharStreamsWithNoneValues() {
        CharStream stream1 = createCharStream(new char[] { 'F', 'G' });
        CharStream stream2 = createCharStream(new char[] { 'H', 'I', 'J' });
        List<Character> result = CharStream.zip(stream1, stream2, 'U', 'V', (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('F' + 'H'), (char) ('G' + 'I'), (char) ('U' + 'J')), result);
    }

    @Test
    public void testZipThreeCharStreamsWithNoneValues() {
        CharStream stream1 = createCharStream(new char[] { 'X' });
        CharStream stream2 = createCharStream(new char[] { 'Y', 'Z' });
        CharStream stream3 = createCharStream('a', 'b', 'c');
        List<Character> result = CharStream.zip(stream1, stream2, stream3, 'D', 'E', 'F', (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('X' + 'Y' + 'a'), (char) ('D' + 'Z' + 'b'), (char) ('D' + 'E' + 'c')), result);
    }

    @Test
    public void testZipCollectionOfCharStreamsWithNoneValues() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { '1' }), createCharStream(new char[] { 'a', 'b' }));
        char[] valuesForNone = { 'X', 'Y' };
        List<Character> result = CharStream.zip(streams, valuesForNone, chars -> (char) (chars[0] + chars[1])).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a'), (char) ('X' + 'b')), result);
    }

    @Test
    public void testMergeTwoCharArrays() {
        char[] arr1 = { 'a', 'c', 'e' };
        char[] arr2 = { 'b', 'd', 'f' };
        List<Character> result = CharStream.merge(arr1, arr2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);

        result = CharStream.merge(new char[] { 'x' }, new char[] {}, (c1, c2) -> MergeResult.TAKE_FIRST).boxed().toList();
        assertEquals(Arrays.asList('x'), result);

        result = CharStream.merge(new char[] {}, new char[] { 'y' }, (c1, c2) -> MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('y'), result);
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
    public void testMergeTwoCharIterators() {
        CharIterator iter1 = CharList.of('A', 'C').iterator();
        CharIterator iter2 = CharList.of('B', 'D').iterator();
        List<Character> result = CharStream.merge(iter1, iter2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('A', 'B', 'C', 'D'), result);
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
    public void testMergeTwoCharStreams() {
        CharStream stream1 = createCharStream(new char[] { 'X', 'Z' });
        CharStream stream2 = createCharStream(new char[] { 'Y' });
        List<Character> result = CharStream.merge(stream1, stream2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('X', 'Y', 'Z'), result);
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
    public void testMergeCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { '1', '5' }), createCharStream(new char[] { '2', '6' }),
                createCharStream(new char[] { '3', '4' }));
        List<Character> result = CharStream.merge(streams, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('1', '2', '3', '4', '5', '6'), result);
    }

}
