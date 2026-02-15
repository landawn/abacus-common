package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;

@Tag("new-test")
public class AbstractCharStream201Test extends TestBase {

    private static final char[] TEST_ARRAY = new char[] { 'a', 'b', 'c', 'd', 'e' };
    private CharStream stream;

    protected CharStream createCharStream(char[] array) {
        return CharStream.of(array);
    }

    @BeforeEach
    public void setUp() {
        stream = createCharStream(TEST_ARRAY);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(100);
        long startTime = System.nanoTime();
        createCharStream(
                new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' })
                        .rateLimited(rateLimiter)
                        .forEach(c -> {
                        });
        long endTime = System.nanoTime();
        assertTrue(TimeUnit.NANOSECONDS.toMillis(endTime - startTime) > 200);
    }

    @Test
    public void testDelay() {
        long startTime = System.nanoTime();
        createCharStream(new char[] { 'a', 'b', 'c' }).delay(Duration.ofMillis(100)).forEach(c -> {
        });
        long endTime = System.nanoTime();
        assertTrue(TimeUnit.NANOSECONDS.toMillis(endTime - startTime) < 300);
    }

    @Test
    public void testSkipUntil() {
        char[] data = { 'a', 'b', 'c', 'd', 'e' };
        CharStream s = createCharStream(data);
        List<Character> result = s.skipUntil(c -> c == 'c').boxed().toList();
        assertEquals(Arrays.asList('c', 'd', 'e'), result);

        result = createCharStream(data).skipUntil(c -> c == 'z').boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] {}).skipUntil(c -> c == 'c').boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testDistinct() {
        List<Character> result = createCharStream(new char[] { 'a', 'b', 'a', 'c', 'b', 'd' }).distinct().boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);

        result = createCharStream(new char[] {}).distinct().boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] { 'a', 'a', 'a' }).distinct().boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testFlatmapCharArray() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).flatmap(c -> new char[] { c, Character.toUpperCase(c) }).boxed().toList();
        assertEquals(Arrays.asList('a', 'A', 'b', 'B'), result);

        result = createCharStream(new char[] {}).flatmap(c -> new char[] { c }).boxed().toList();
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
    public void testFlattMapToObjArray() {
        List<String> result = createCharStream(new char[] { 'a', 'b' })
                .flattmapToObj(c -> new String[] { String.valueOf(c), String.valueOf(Character.toUpperCase(c)) })
                .toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);

        result = createCharStream(new char[] {}).flattmapToObj(c -> new String[] { String.valueOf(c) }).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testMapPartial() {
        List<Character> result = createCharStream(new char[] { 'a', 'b', 'c' })
                .mapPartial(c -> c == 'b' ? OptionalChar.empty() : OptionalChar.of(Character.toUpperCase(c)))
                .boxed()
                .toList();
        assertEquals(Arrays.asList('A', 'C'), result);

        result = createCharStream(new char[] {}).mapPartial(OptionalChar::of).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testRangeMap() {
        List<Character> result = createCharStream(new char[] { 'a', 'b', 'c', 'e', 'f', 'g', 'i' })
                .rangeMap((c1, c2) -> (c2 - c1) == 1, (first, last) -> (char) (first + last))
                .boxed()
                .toList();
        assertEquals(Arrays.asList((char) ('a' + 'b'), (char) ('c' + 'c'), (char) ('e' + 'f'), (char) ('g' + 'g'), (char) ('i' + 'i')), result);

        result = createCharStream(new char[] { '1', '2', '3', ':', ';', '>', 'A' })
                .rangeMap((c1, c2) -> (c2 - c1) == 1, (first, last) -> (char) ((last - '0') - (first - '0') + '0'))
                .boxed()
                .toList();
        assertEquals(Arrays.asList((char) (('2' - '0') - ('1' - '0') + '0'), (char) (('3' - '0') - ('3' - '0') + '0'), (char) ((';' - '0') - (':' - '0') + '0'),
                (char) (('>' - '0') - ('>' - '0') + '0'), (char) (('A' - '0') - ('A' - '0') + '0')), result);
    }

    @Test
    public void testRangeMapToObj() {
        List<String> result = createCharStream(new char[] { 'a', 'b', 'c', 'e', 'f', 'g', 'i' })
                .rangeMapToObj((c1, c2) -> (c2 - c1) == 1, (first, last) -> String.format("%c-%c", first, last))
                .toList();
        assertEquals(Arrays.asList("a-b", "c-c", "e-f", "g-g", "i-i"), result);

        result = createCharStream(new char[] {}).rangeMapToObj((c1, c2) -> true, (c1, c2) -> "").toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testCollapseCharList() {
        List<CharList> result = createCharStream(new char[] { 'a', 'b', 'd', 'e', 'f', 'h' }).collapse((c1, c2) -> (c2 - c1) <= 1).toList();
        assertEquals(Arrays.asList(CharList.of('a', 'b'), CharList.of('d', 'e', 'f'), CharList.of('h')), result);

        result = createCharStream(new char[] {}).collapse((c1, c2) -> true).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testCollapseBinaryOperator() {
        List<Character> result = createCharStream(new char[] { 'a', 'b', 'd', 'e', 'f', 'h' })
                .collapse((c1, c2) -> (c2 - c1) <= 1, (c1, c2) -> (char) (c1 + c2))
                .boxed()
                .toList();
        assertEquals(Arrays.asList((char) ('a' + 'b'), (char) ('d' + 'e' + 'f'), 'h'), result);

        result = createCharStream(new char[] {}).collapse((c1, c2) -> true, (c1, c2) -> c1).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testCollapseTriPredicate() {
        List<Character> result = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'f', 'g', 'h', 'i' })
                .collapse((first, prev, current) -> (current - prev) == 1 && (current - first) < 3, (prev, current) -> (char) (prev + current))
                .boxed()
                .toList();
        assertEquals(Arrays.asList((char) ('a' + 'b' + 'c'), 'd', (char) ('f' + 'g' + 'h'), 'i'), result);

        result = createCharStream(new char[] {}).collapse((f, p, c) -> true, (p, c) -> p).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testSkipWithAction() {
        CharList droppedItems = new CharList();
        CharStream s = createCharStream(TEST_ARRAY);
        List<Character> result = s.skip(2, c -> droppedItems.add(c)).boxed().toList();

        assertEquals(Arrays.asList('c', 'd', 'e'), result);
        assertEquals(CharList.of('a', 'b'), droppedItems);

        droppedItems.clear();
        result = createCharStream(new char[] { 'a' }).skip(1, c -> droppedItems.add(c)).boxed().toList();
        assertEquals(0, result.size());
        assertEquals(CharList.of('a'), droppedItems);

        droppedItems.clear();
        result = createCharStream(new char[] {}).skip(1, c -> droppedItems.add(c)).boxed().toList();
        assertEquals(0, result.size());
        assertEquals(0, droppedItems.size());
    }

    @Test
    public void testFilterWithDroppedItemAction() {
        CharList droppedItems = new CharList();
        CharStream s = createCharStream(TEST_ARRAY);
        List<Character> result = s.filter(c -> c % 2 == 1, c -> droppedItems.add(c)).boxed().toList();

        assertEquals(Arrays.asList('a', 'c', 'e'), result);
        assertEquals(CharList.of('b', 'd'), droppedItems);

        droppedItems.clear();
        result = createCharStream(new char[] {}).filter(c -> true, c -> droppedItems.add(c)).boxed().toList();
        assertEquals(0, result.size());
        assertEquals(0, droppedItems.size());
    }

    @Test
    public void testDropWhileWithAction() {
        CharList droppedItems = new CharList();
        CharStream s = createCharStream(TEST_ARRAY);
        List<Character> result = s.dropWhile(c -> c != 'c', c -> droppedItems.add(c)).boxed().toList();

        assertEquals(Arrays.asList('c', 'd', 'e'), result);
        assertEquals(CharList.of('a', 'b'), droppedItems);

        droppedItems.clear();
        result = createCharStream(new char[] { 'a', 'b' }).dropWhile(c -> c == 'a', c -> droppedItems.add(c)).boxed().toList();
        assertEquals(Arrays.asList('b'), result);
        assertEquals(CharList.of('a'), droppedItems);

        droppedItems.clear();
        result = createCharStream(new char[] {}).dropWhile(c -> true, c -> droppedItems.add(c)).boxed().toList();
        assertEquals(0, result.size());
        assertEquals(0, droppedItems.size());
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
    public void testScanAccumulator() {
        List<Character> result = createCharStream(new char[] { '1', '2', '3' }).scan((c1, c2) -> (char) (c1 + c2 - '0')).boxed().toList();
        assertEquals(Arrays.asList('1', (char) ('1' + '2' - '0'), (char) ('1' + '2' + '3' - '0' - '0')), result);

        result = createCharStream(new char[] {}).scan((c1, c2) -> c1).boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] { 'a' }).scan((c1, c2) -> c1).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testScanInitAccumulator() {
        List<Character> result = createCharStream(new char[] { '1', '2', '3' }).scan('0', (c1, c2) -> (char) (c1 + c2 - '0')).boxed().toList();
        assertEquals(Arrays.asList((char) ('0' + '1' - '0'), (char) ('0' + '1' + '2' - '0' - '0'), (char) ('0' + '1' + '2' + '3' - '0' - '0' - '0')), result);

        result = createCharStream(new char[] {}).scan('x', (c1, c2) -> c1).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testScanInitIncludedAccumulator() {
        List<Character> result = createCharStream(new char[] { '1', '2', '3' }).scan('0', true, (c1, c2) -> (char) (c1 + c2 - '0')).boxed().toList();
        assertEquals(Arrays.asList('0', (char) ('0' + '1' - '0'), (char) ('0' + '1' + '2' - '0' - '0'), (char) ('0' + '1' + '2' + '3' - '0' - '0' - '0')),
                result);

        result = createCharStream(new char[] {}).scan('x', true, (c1, c2) -> c1).boxed().toList();
        assertEquals(Arrays.asList('x'), result);
    }

    @Test
    public void testIntersection() {
        Collection<Character> other = Arrays.asList('b', 'd', 'f');
        List<Character> result = createCharStream(TEST_ARRAY).intersection(other).boxed().toList();
        assertEquals(Arrays.asList('b', 'd'), result);

        result = createCharStream(new char[] {}).intersection(other).boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] { 'a', 'b' }).intersection(Arrays.asList()).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testDifference() {
        Collection<Character> other = Arrays.asList('b', 'd', 'f');
        List<Character> result = createCharStream(TEST_ARRAY).difference(other).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e'), result);

        result = createCharStream(new char[] {}).difference(other).boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] { 'a', 'b' }).difference(Arrays.asList()).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testSymmetricDifference() {
        Collection<Character> other = Arrays.asList('b', 'd', 'f', 'g');
        List<Character> result = createCharStream(TEST_ARRAY).symmetricDifference(other).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e', 'f', 'g'), result);

        result = createCharStream(new char[] {}).symmetricDifference(other).boxed().toList();
        assertEquals(Arrays.asList('b', 'd', 'f', 'g'), result);

        result = createCharStream(new char[] { 'a', 'b' }).symmetricDifference(Arrays.asList()).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testReversed() {
        List<Character> result = createCharStream(new char[] { 'a', 'b', 'c' }).reversed().boxed().toList();
        assertEquals(Arrays.asList('c', 'b', 'a'), result);

        result = createCharStream(new char[] {}).reversed().boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testRotated() {
        List<Character> result = createCharStream(TEST_ARRAY).rotated(2).boxed().toList();
        assertEquals(Arrays.asList('d', 'e', 'a', 'b', 'c'), result);

        result = createCharStream(TEST_ARRAY).rotated(-2).boxed().toList();
        assertEquals(Arrays.asList('c', 'd', 'e', 'a', 'b'), result);

        result = createCharStream(new char[] {}).rotated(1).boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] { 'a' }).rotated(1).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testShuffled() {
        char[] original = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> result = createCharStream(original).shuffled(new Random(0)).boxed().toList();
        assertEquals(original.length, result.size());
        assertTrue(result.containsAll(Arrays.asList('a', 'b', 'c', 'd', 'e')));
        assertTrue(Arrays.asList('a', 'b', 'c', 'd', 'e').containsAll(result));
        assertNotEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);
    }

    @Test
    public void testSorted() {
        List<Character> result = createCharStream(new char[] { 'c', 'a', 'e', 'b', 'd' }).sorted().boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);

        result = createCharStream(new char[] {}).sorted().boxed().toList();
        assertEquals(0, result.size());

        result = createCharStream(new char[] { 'a', 'b', 'c' }).sorted().boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testReverseSorted() {
        List<Character> result = createCharStream(new char[] { 'c', 'a', 'e', 'b', 'd' }).reverseSorted().boxed().toList();
        assertEquals(Arrays.asList('e', 'd', 'c', 'b', 'a'), result);

        result = createCharStream(new char[] {}).reverseSorted().boxed().toList();
        assertEquals(0, result.size());
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
    public void testIndexed() {
        List<IndexedChar> result = createCharStream(new char[] { 'a', 'b', 'c' }).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(IndexedChar.of('a', 0), result.get(0));
        assertEquals(IndexedChar.of('b', 1), result.get(1));
        assertEquals(IndexedChar.of('c', 2), result.get(2));

        result = createCharStream(new char[] {}).indexed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testBoxed() {
        List<Character> result = createCharStream(new char[] { 'a', 'b', 'c' }).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream(new char[] {}).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testPrependChars() {
        List<Character> result = createCharStream(new char[] { 'c', 'd' }).prepend('a', 'b').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);

        result = createCharStream(new char[] {}).prepend('a', 'b').boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);

        result = createCharStream(new char[] { 'c', 'd' }).prepend().boxed().toList();
        assertEquals(Arrays.asList('c', 'd'), result);
    }

    @Test
    public void testPrependCharStream() {
        List<Character> result = createCharStream(new char[] { 'c', 'd' }).prepend(createCharStream(new char[] { 'a', 'b' })).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);

        result = createCharStream(new char[] {}).prepend(createCharStream(new char[] { 'a', 'b' })).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);

        result = createCharStream(new char[] { 'c', 'd' }).prepend(createCharStream(new char[] {})).boxed().toList();
        assertEquals(Arrays.asList('c', 'd'), result);
    }

    @Test
    public void testPrependOptionalChar() {
        List<Character> result = createCharStream(new char[] { 'b', 'c' }).prepend(OptionalChar.of('a')).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream(new char[] { 'a', 'b' }).prepend(OptionalChar.empty()).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testAppendChars() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).append('c', 'd').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);

        result = createCharStream(new char[] {}).append('a', 'b').boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);

        result = createCharStream(new char[] { 'a', 'b' }).append().boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testAppendCharStream() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).append(createCharStream(new char[] { 'c', 'd' })).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);

        result = createCharStream(new char[] {}).append(createCharStream(new char[] { 'c', 'd' })).boxed().toList();
        assertEquals(Arrays.asList('c', 'd'), result);

        result = createCharStream(new char[] { 'a', 'b' }).append(createCharStream(new char[] {})).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testAppendOptionalChar() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).append(OptionalChar.of('c')).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = createCharStream(new char[] { 'a', 'b' }).append(OptionalChar.empty()).boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testAppendIfEmptyChars() {
        List<Character> result = createCharStream(new char[] {}).appendIfEmpty('a', 'b').boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);

        result = createCharStream(new char[] { 'x', 'y' }).appendIfEmpty('a', 'b').boxed().toList();
        assertEquals(Arrays.asList('x', 'y'), result);

        result = createCharStream(new char[] {}).appendIfEmpty().boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testMergeWith() {
        CharStream s1 = createCharStream(new char[] { '1', '3', '5' });
        CharStream s2 = createCharStream(new char[] { '2', '4', '6' });

        List<Character> result = s1.mergeWith(s2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('1', '2', '3', '4', '5', '6'), result);

        result = createCharStream(new char[] { 'a' }).mergeWith(createCharStream(new char[] {}), (c1, c2) -> MergeResult.TAKE_FIRST).boxed().toList();
        assertEquals(Arrays.asList('a'), result);

        result = createCharStream(new char[] {}).mergeWith(createCharStream(new char[] { 'b' }), (c1, c2) -> MergeResult.TAKE_FIRST).boxed().toList();
        assertEquals(Arrays.asList('b'), result);
    }

    @Test
    public void testZipWithBinaryOperator() {
        CharStream s1 = createCharStream(new char[] { '1', '2', '3' });
        CharStream s2 = createCharStream(new char[] { 'a', 'b', 'c', 'd' });

        List<Character> result = s1.zipWith(s2, (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a'), (char) ('2' + 'b'), (char) ('3' + 'c')), result);

        result = createCharStream(new char[] { '1' }).zipWith(createCharStream(new char[] {}), (c1, c2) -> c1).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testZipWithTernaryOperator() {
        CharStream s1 = createCharStream(new char[] { '1', '2', '3' });
        CharStream s2 = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream s3 = createCharStream(new char[] { 'x', 'y' });

        List<Character> result = s1.zipWith(s2, s3, (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('2' + 'b' + 'y')), result);

        result = createCharStream(new char[] {}).zipWith(s2, s3, (c1, c2, c3) -> c1).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        CharStream s1 = createCharStream(new char[] { '1', '2' });
        CharStream s2 = createCharStream(new char[] { 'a', 'b', 'c' });

        List<Character> result = s1.zipWith(s2, 'X', 'Y', (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a'), (char) ('2' + 'b'), (char) ('X' + 'c')), result);

        result = createCharStream(new char[] {}).zipWith(createCharStream(new char[] {}), 'X', 'Y', (c1, c2) -> c1).boxed().toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        CharStream s1 = createCharStream(new char[] { '1', '2' });
        CharStream s2 = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream s3 = createCharStream(new char[] { 'x' });

        List<Character> result = s1.zipWith(s2, s3, 'X', 'Y', 'Z', (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('2' + 'b' + 'Z'), (char) ('X' + 'c' + 'Z')), result);

        result = createCharStream(new char[] {}).zipWith(createCharStream(new char[] {}), createCharStream(new char[] {}), 'X', 'Y', 'Z', (c1, c2, c3) -> c1)
                .boxed()
                .toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testToMapKeyAndValueMapper() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'c' }).toMap(c -> String.valueOf(c), c -> (int) c);
        assertEquals(3, result.size());
        assertEquals(97, (int) result.get("a"));
        assertEquals(98, (int) result.get("b"));
        assertEquals(99, (int) result.get("c"));

        result = createCharStream(new char[] {}).toMap(c -> String.valueOf(c), c -> (int) c);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToMapKeyAndValueMapperDuplicateKeys() {
        assertThrows(IllegalStateException.class, () -> createCharStream(new char[] { 'a', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c));
    }

    @Test
    public void testToMapKeyAndValueMapperAndMapFactory() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'c' }).toMap(c -> String.valueOf(c), c -> (int) c, () -> N.newHashMap());
        assertEquals(3, result.size());
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
        Map<Boolean, List<Character>> result = createCharStream(new char[] { 'a', 'b', 'c', 'd' }).groupTo(c -> c % 2 == 0,
                java.util.stream.Collectors.toList());
        assertEquals(2, result.size());
        assertEquals(Arrays.asList('b', 'd'), result.get(true));
        assertEquals(Arrays.asList('a', 'c'), result.get(false));

        result = createCharStream(new char[] {}).groupTo(c -> true, java.util.stream.Collectors.toList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupToWithMapFactory() {
        Map<Boolean, List<Character>> result = createCharStream(new char[] { 'a', 'b', 'c', 'd' }).groupTo(c -> c % 2 == 0,
                java.util.stream.Collectors.toList(), Suppliers.ofTreeMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testForEachIndexed() {
        CharList processed = new CharList();
        createCharStream(new char[] { 'a', 'b', 'c' }).forEachIndexed((idx, c) -> processed.add((char) (c + idx)));
        assertEquals(CharList.of('a', (char) ('b' + 1), (char) ('c' + 2)), processed);

        processed.clear();
        createCharStream(new char[] {}).forEachIndexed((idx, c) -> processed.add(c));
        assertEquals(0, processed.size());
    }

    @Test
    public void testFirst() {
        OptionalChar first = createCharStream(new char[] { 'a', 'b', 'c' }).first();
        assertTrue(first.isPresent());
        assertEquals('a', first.get());

        first = createCharStream(new char[] {}).first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        OptionalChar last = createCharStream(new char[] { 'a', 'b', 'c' }).last();
        assertTrue(last.isPresent());
        assertEquals('c', last.get());

        last = createCharStream(new char[] { 'x' }).last();
        assertTrue(last.isPresent());
        assertEquals('x', last.get());

        last = createCharStream(new char[] {}).last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testOnlyOne() {
        OptionalChar one = createCharStream(new char[] { 'a' }).onlyOne();
        assertTrue(one.isPresent());
        assertEquals('a', one.get());

        one = createCharStream(new char[] {}).onlyOne();
        assertFalse(one.isPresent());
    }

    @Test
    public void testOnlyOneTooManyElements() {
        assertThrows(TooManyElementsException.class, () -> createCharStream(new char[] { 'a', 'b' }).onlyOne());
    }

    @Test
    public void testFindAny() {
        OptionalChar result = createCharStream(new char[] { 'a', 'b', 'c' }).findAny(c -> c == 'b');
        assertTrue(result.isPresent());
        assertEquals('b', result.get());

        result = createCharStream(new char[] { 'a', 'b', 'c' }).findAny(c -> c == 'x');
        assertFalse(result.isPresent());

        result = createCharStream(new char[] {}).findAny(c -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> summary = createCharStream(new char[] { '1', '2', '3', '4', '5' })
                .summaryStatisticsAndPercentiles();
        CharSummaryStatistics stats = summary.left();
        Optional<Map<Percentage, Character>> percentiles = summary.right();

        assertEquals(5, stats.getCount());
        assertEquals('1', stats.getMin());
        assertEquals('5', stats.getMax());
        assertEquals((long) ((int) '1' + (int) '2' + (int) '3' + (int) '4' + (int) '5'), stats.getSum().longValue());
        assertTrue(percentiles.isPresent());

        summary = createCharStream(new char[] {}).summaryStatisticsAndPercentiles();
        stats = summary.left();
        percentiles = summary.right();
        assertEquals(0, stats.getCount());
        assertFalse(percentiles.isPresent());
    }

    @Test
    public void testJoinDelimiterPrefixSuffix() {
        String result = createCharStream(new char[] { 'a', 'b', 'c' }).join("-", "[", "]");
        assertEquals("[a-b-c]", result);

        result = createCharStream(new char[] { 'a' }).join("-", "[", "]");
        assertEquals("[a]", result);

        result = createCharStream(new char[] {}).join("-", "[", "]");
        assertEquals("[]", result);
    }

    @Test
    public void testJoinToJoiner() {
        Joiner joiner = Joiner.with(" - ", "<<", ">>");
        Joiner resultJoiner = createCharStream(new char[] { 'a', 'b', 'c' }).joinTo(joiner);
        assertEquals("<<a - b - c>>", resultJoiner.toString());

        joiner = Joiner.with(" - ", "<<", ">>");
        resultJoiner = createCharStream(new char[] {}).joinTo(joiner);
        assertEquals("<<>>", resultJoiner.toString());
    }

    @Test
    public void testCollectSupplierAccumulator() {
        StringBuilder sb = createCharStream(new char[] { 'a', 'b', 'c' }).collect(StringBuilder::new, StringBuilder::append);
        assertEquals("abc", sb.toString());

        sb = createCharStream(new char[] {}).collect(StringBuilder::new, StringBuilder::append);
        assertEquals("", sb.toString());
    }

    @Test
    public void testIterator() {
        CharIterator iterator = createCharStream(new char[] { 'x', 'y', 'z' }).iterator();
        assertTrue(iterator.hasNext());
        assertEquals('x', iterator.nextChar());
        assertTrue(iterator.hasNext());
        assertEquals('y', iterator.nextChar());
        assertTrue(iterator.hasNext());
        assertEquals('z', iterator.nextChar());
        assertFalse(iterator.hasNext());
    }
}
