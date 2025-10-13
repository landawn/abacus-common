package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.Collectors;

@Tag("new-test")
public class CharList200Test extends TestBase {

    private CharList list;

    @BeforeEach
    public void setUp() {
        list = new CharList();
    }

    @Test
    public void testConstructors() {
        assertEquals(0, new CharList().size());
        assertEquals(0, new CharList(10).size());
        assertTrue(new CharList(10).isEmpty());

        char[] data = { 'a', 'b', 'c' };
        CharList fromArray = new CharList(data);
        assertEquals(3, fromArray.size());
        assertArrayEquals(data, fromArray.toArray());

        CharList fromArrayWithSize = new CharList(data, 2);
        assertEquals(2, fromArrayWithSize.size());
        assertEquals('a', fromArrayWithSize.get(0));
        assertEquals('b', fromArrayWithSize.get(1));
    }

    @Test
    public void testOf() {
        char[] data = { 'a', 'b', 'c' };
        CharList fromOf = CharList.of(data);
        assertEquals(3, fromOf.size());
        assertArrayEquals(data, fromOf.toArray());

        CharList fromOfWithSize = CharList.of(data, 2);
        assertEquals(2, fromOfWithSize.size());
        assertArrayEquals(new char[] { 'a', 'b' }, fromOfWithSize.toArray());
    }

    @Test
    public void testCopyOf() {
        char[] data = { 'a', 'b', 'c', 'd' };
        CharList copy = CharList.copyOf(data);
        assertEquals(4, copy.size());
        assertArrayEquals(data, copy.toArray());
        assertNotSame(data, copy.array());

        CharList partialCopy = CharList.copyOf(data, 1, 3);
        assertEquals(2, partialCopy.size());
        assertArrayEquals(new char[] { 'b', 'c' }, partialCopy.toArray());
    }

    @Test
    public void testRange() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, CharList.range('a', 'd').toArray());
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharList.range('a', 'f', 2).toArray());
    }

    @Test
    public void testRangeClosed() {
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, CharList.rangeClosed('a', 'd').toArray());
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharList.rangeClosed('a', 'e', 2).toArray());
    }

    @Test
    public void testRepeat() {
        CharList repeated = CharList.repeat('z', 5);
        assertEquals(5, repeated.size());
        assertArrayEquals(new char[] { 'z', 'z', 'z', 'z', 'z' }, repeated.toArray());
    }

    @Test
    public void testRandom() {
        assertEquals(10, CharList.random(10).size());
        CharList randomList = CharList.random('a', 'z', 5);
        assertEquals(5, randomList.size());
        for (char c : randomList.toArray()) {
            assertTrue(c >= 'a' && c < 'z');
        }

        char[] candidates = { 'x', 'y', 'z' };
        CharList randomFromCandidates = CharList.random(candidates, 3);
        assertEquals(3, randomFromCandidates.size());
        for (char c : randomFromCandidates.toArray()) {
            assertTrue(c == 'x' || c == 'y' || c == 'z');
        }
    }

    @Test
    public void testAddAndGet() {
        list.add('a');
        list.add('b');
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));

        list.add(1, 'c');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('b', list.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
    }

    @Test
    public void testSet() {
        list.add('a');
        char old = list.set(0, 'z');
        assertEquals('a', old);
        assertEquals('z', list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(1, 'y'));
    }

    @Test
    public void testAddAll() {
        list.addAll(CharList.of('a', 'b'));
        assertEquals(2, list.size());

        list.addAll(1, CharList.of('c', 'd'));
        assertArrayEquals(new char[] { 'a', 'c', 'd', 'b' }, list.toArray());
    }

    @Test
    public void testAddAllArray() {
        list.addAll(new char[] { 'a', 'b' });
        assertEquals(2, list.size());

        list.addAll(1, new char[] { 'c', 'd' });
        assertArrayEquals(new char[] { 'a', 'c', 'd', 'b' }, list.toArray());
    }

    @Test
    public void testRemove() {
        list.add('a');
        list.add('b');
        list.add('a');
        assertTrue(list.remove('a'));
        assertEquals(2, list.size());
        assertArrayEquals(new char[] { 'b', 'a' }, list.toArray());
        assertFalse(list.remove('z'));
    }

    @Test
    public void testRemoveAllOccurrences() {
        list.addAll(CharList.of('a', 'b', 'a', 'c', 'a'));
        assertTrue(list.removeAllOccurrences('a'));
        assertArrayEquals(new char[] { 'b', 'c' }, list.toArray());
        assertFalse(list.removeAllOccurrences('d'));
    }

    @Test
    public void testRemoveIf() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        assertTrue(list.removeIf(c -> c % 2 == 0));
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, list.toArray());
    }

    @Test
    public void testRemoveDuplicates() {
        list.addAll(CharList.of('a', 'b', 'a', 'c', 'b'));
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, list.toArray());
        assertFalse(CharList.of('x', 'y', 'z').removeDuplicates());
    }

    @Test
    public void testDelete() {
        list.addAll(CharList.of('a', 'b', 'c'));
        assertEquals('b', list.delete(1));
        assertEquals(2, list.size());
        assertArrayEquals(new char[] { 'a', 'c' }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.deleteAllByIndices(1, 3);
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, list.trimToSize().toArray());
    }

    @Test
    public void testDeleteRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.deleteRange(1, 4);
        assertArrayEquals(new char[] { 'a', 'e' }, list.trimToSize().toArray());
    }

    @Test
    public void testClearAndIsEmpty() {
        list.add('a');
        assertFalse(list.isEmpty());
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testContains() {
        list.add('a');
        assertTrue(list.contains('a'));
        assertFalse(list.contains('b'));
    }

    @Test
    public void testContainsAny() {
        list.addAll(CharList.of('a', 'b', 'c'));
        assertTrue(list.containsAny(CharList.of('c', 'd', 'e')));
        assertFalse(list.containsAny(CharList.of('x', 'y', 'z')));
    }

    @Test
    public void testContainsAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        assertTrue(list.containsAll(CharList.of('b', 'd')));
        assertFalse(list.containsAll(CharList.of('b', 'e')));
    }

    @Test
    public void testIndexOf() {
        list.addAll(CharList.of('a', 'b', 'c', 'b'));
        assertEquals(1, list.indexOf('b'));
        assertEquals(3, list.lastIndexOf('b'));
        assertEquals(-1, list.indexOf('z'));
    }

    @Test
    public void testToArray() {
        char[] data = { 'a', 'b', 'c' };
        list.addAll(CharList.of(data));
        assertArrayEquals(data, list.toArray());
        assertNotSame(data, list.toArray());
    }

    @Test
    public void testSort() {
        list.addAll(CharList.of('d', 'a', 'c', 'b'));
        list.sort();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, list.toArray());
    }

    @Test
    public void testReverse() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        list.reverse();
        assertArrayEquals(new char[] { 'd', 'c', 'b', 'a' }, list.toArray());
    }

    @Test
    public void testShuffle() {
        CharList original = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
        CharList shuffled = original.copy();
        shuffled.shuffle();
        assertEquals(original.size(), shuffled.size());
        assertTrue(shuffled.containsAll(original));
        assertFalse(Arrays.equals(original.toArray(), shuffled.toArray()));
    }

    @Test
    public void testSwap() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        list.swap(1, 3);
        assertArrayEquals(new char[] { 'a', 'd', 'c', 'b' }, list.toArray());
    }

    @Test
    public void testCopy() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList copy = list.copy();
        assertEquals(list.size(), copy.size());
        assertArrayEquals(list.toArray(), copy.toArray());
        assertNotSame(list.array(), copy.array());
    }

    @Test
    public void testBoxed() {
        list.addAll(CharList.of('a', 'b', 'c'));
        List<Character> boxedList = list.boxed();
        assertEquals(3, boxedList.size());
        assertEquals(Character.valueOf('a'), boxedList.get(0));
        assertEquals(Character.valueOf('b'), boxedList.get(1));
        assertEquals(Character.valueOf('c'), boxedList.get(2));
    }

    @Test
    public void testFirstAndLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());

        list.addAll(CharList.of('a', 'b', 'c'));
        assertEquals('a', list.getFirst());
        assertEquals('c', list.getLast());
    }

    @Test
    public void testAddFirstLast() {
        list.addFirst('b');
        list.addFirst('a');
        list.addLast('c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, list.toArray());
    }

    @Test
    public void testRemoveFirstLast() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        assertEquals('a', list.removeFirst());
        assertEquals('d', list.removeLast());
        assertArrayEquals(new char[] { 'b', 'c' }, list.toArray());
        assertEquals(2, list.size());
    }

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());
        list.add('a');
        assertEquals("[a]", list.toString());
        list.add('b');
        list.add('c');
        assertEquals("[a, b, c]", list.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = CharList.of('a', 'b', 'c');
        CharList list3 = CharList.of('a', 'b', 'd');

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertEquals(list1.hashCode(), list2.hashCode());
        assertNotEquals(list1.hashCode(), list3.hashCode());
        assertNotEquals(list1, new Object());
    }

    @Test
    public void testReplaceAll() {
        list.addAll(CharList.of('a', 'b', 'a', 'c'));
        int replacements = list.replaceAll('a', 'z');
        assertEquals(2, replacements);
        assertArrayEquals(new char[] { 'z', 'b', 'z', 'c' }, list.toArray());
    }

    @Test
    public void testReplaceAllWithOperator() {
        list.addAll(CharList.of('a', 'b', 'c'));
        list.replaceAll(c -> (char) (c + 1));
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, list.toArray());
    }

    @Test
    public void testReplaceIf() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        assertTrue(list.replaceIf(c -> c > 'b', 'x'));
        assertArrayEquals(new char[] { 'a', 'b', 'x', 'x' }, list.toArray());
        assertFalse(list.replaceIf(c -> c > 'z', 'y'));
    }

    @Test
    public void testFill() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        list.fill('x');
        assertArrayEquals(new char[] { 'x', 'x', 'x', 'x' }, list.toArray());
        list.fill(1, 3, 'y');
        assertArrayEquals(new char[] { 'x', 'y', 'y', 'x' }, list.toArray());
    }

    @Test
    public void testIntersection() {
        list.addAll(CharList.of('a', 'b', 'b', 'c'));
        CharList other = CharList.of('b', 'c', 'd', 'b');
        CharList intersection = list.intersection(other);
        assertArrayEquals(new char[] { 'b', 'b', 'c' }, intersection.toArray());
    }

    @Test
    public void testDifference() {
        list.addAll(CharList.of('a', 'b', 'b', 'c'));
        CharList other = CharList.of('b', 'c', 'd');
        CharList difference = list.difference(other);
        assertArrayEquals(new char[] { 'a', 'b' }, difference.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList other = CharList.of('c', 'd', 'e');
        CharList symDifference = list.symmetricDifference(other);
        symDifference.sort();
        assertArrayEquals(new char[] { 'a', 'b', 'd', 'e' }, symDifference.toArray());
    }

    @Test
    public void testMinMax() {
        assertTrue(list.min().isEmpty());
        assertTrue(list.max().isEmpty());

        list.addAll(CharList.of('d', 'a', 'e', 'c', 'b'));
        assertEquals('a', list.min().get());
        assertEquals('e', list.max().get());
        assertEquals('a', list.min(1, 4).get());
    }

    @Test
    public void testMedian() {
        assertTrue(list.median().isEmpty());
        list.addAll(CharList.of('e', 'a', 'c', 'd', 'b'));
        assertEquals('c', list.median().get());
    }

    @Test
    public void testStream() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        long count = list.stream().filter(c -> c > 'b').count();
        assertEquals(2, count);
    }

    @Test
    public void testIsSorted() {
        assertTrue(CharList.of().isSorted());
        assertTrue(CharList.of('a').isSorted());
        assertTrue(CharList.of('a', 'b', 'c').isSorted());
        assertFalse(CharList.of('a', 'c', 'b').isSorted());
    }

    @Test
    public void testRangeAndRangeClosed() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, CharList.range('a', 'd').toArray());
        assertArrayEquals(new char[] {}, CharList.range('a', 'a').toArray());
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharList.range('a', 'f', 2).toArray());

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, CharList.rangeClosed('a', 'd').toArray());
        assertArrayEquals(new char[] { 'a' }, CharList.rangeClosed('a', 'a').toArray());
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharList.rangeClosed('a', 'e', 2).toArray());
    }

    @Test
    public void testRemoveAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'c'));
        assertTrue(list.removeAll(CharList.of('c', 'e', 'x')));
        assertArrayEquals(new char[] { 'a', 'b', 'd' }, list.toArray());
        assertFalse(list.removeAll(CharList.of('x', 'y')));
    }

    @Test
    public void testRetainAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'c'));
        assertTrue(list.retainAll(CharList.of('c', 'e', 'x')));
        assertArrayEquals(new char[] { 'c', 'e', 'c' }, list.toArray());
        assertFalse(list.retainAll(CharList.of('c', 'e', 'x')));
    }

    @Test
    public void testMoveRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'f'));
        list.moveRange(2, 4, 0);
        assertArrayEquals(new char[] { 'c', 'd', 'a', 'b', 'e', 'f' }, list.toArray());

        list = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
        list.moveRange(0, 2, 4);
        assertArrayEquals(new char[] { 'c', 'd', 'e', 'f', 'a', 'b' }, list.toArray());
    }

    @Test
    public void testReplaceRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.replaceRange(2, 4, CharList.of('x', 'y', 'z'));
        assertArrayEquals(new char[] { 'a', 'b', 'x', 'y', 'z', 'e' }, list.toArray());

        list.replaceRange(1, 4, CharList.of('m'));
        assertArrayEquals(new char[] { 'a', 'm', 'z', 'e' }, list.toArray());

        list.replaceRange(1, 2, CharList.of());
        assertArrayEquals(new char[] { 'a', 'z', 'e' }, list.toArray());
    }

    @Test
    public void testDisjoint() {
        list.addAll(CharList.of('a', 'b', 'c'));
        assertFalse(list.disjoint(CharList.of('c', 'd', 'e')));
        assertFalse(list.disjoint(new char[] { 'x', 'a' }));
        assertTrue(list.disjoint(CharList.of('x', 'y', 'z')));
        assertTrue(list.disjoint(new char[] {}));
    }

    @Test
    public void testOccurrencesOf() {
        list.addAll(CharList.of('a', 'b', 'a', 'c', 'a'));
        assertEquals(3, list.occurrencesOf('a'));
        assertEquals(1, list.occurrencesOf('c'));
        assertEquals(0, list.occurrencesOf('z'));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        list.addAll(CharList.of('a', 'b', 'c', 'b', 'd'));
        assertEquals(1, list.indexOf('b'));
        assertEquals(1, list.indexOf('b', 0));
        assertEquals(3, list.indexOf('b', 2));
        assertEquals(-1, list.indexOf('b', 4));

        assertEquals(3, list.lastIndexOf('b'));
        assertEquals(1, list.lastIndexOf('b', 2));
        assertEquals(3, list.lastIndexOf('b', 4));
        assertEquals(-1, list.lastIndexOf('a', -1));
    }

    @Test
    public void testFirstLastAndOptionalFirstLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());
        assertTrue(list.first().isEmpty());
        assertTrue(list.last().isEmpty());

        list.addAll(CharList.of('a', 'b', 'c'));
        assertEquals('a', list.getFirst());
        assertEquals('c', list.getLast());
        assertEquals('a', list.first().get());
        assertEquals('c', list.last().get());
    }

    @Test
    public void testHasDuplicates() {
        assertFalse(list.hasDuplicates());
        list.add('a');
        assertFalse(list.hasDuplicates());
        list.add('b');
        assertFalse(list.hasDuplicates());
        list.add('a');
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void testMinMaxMedian() {
        assertTrue(list.min().isEmpty());
        assertTrue(list.max().isEmpty());
        assertTrue(list.median().isEmpty());

        list.addAll(CharList.of('d', 'a', 'e', 'b', 'c'));
        assertEquals('a', list.min().get());
        assertEquals('e', list.max().get());
        assertEquals('c', list.median().get());

        assertEquals('a', list.min(1, 4).get());
        assertEquals('a', list.min(0, 2).get());
        assertEquals('e', list.max(1, 4).get());
        assertEquals('c', list.median(0, 5).get());
    }

    @Test
    public void testIntersectionAndDifference() {
        list.addAll(CharList.of('a', 'b', 'b', 'c'));
        CharList other = CharList.of('b', 'c', 'd', 'b');

        CharList intersection = list.intersection(other);
        intersection.sort();
        assertArrayEquals(new char[] { 'b', 'b', 'c' }, intersection.toArray());

        CharList difference = list.difference(other);
        assertArrayEquals(new char[] { 'a' }, difference.toArray());

        CharList differenceFromArray = list.difference(new char[] { 'b', 'x' });
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, differenceFromArray.toArray());
    }

    @Test
    public void testForEach() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        StringBuilder sb = new StringBuilder();
        CharConsumer consumer = c -> sb.append(c);

        list.forEach(consumer);
        assertEquals("abcd", sb.toString());

        sb.setLength(0);
        list.forEach(1, 3, consumer);
        assertEquals("bc", sb.toString());

        sb.setLength(0);
        list.forEach(2, -1, consumer);
        assertEquals("cba", sb.toString());
    }

    @Test
    public void testIterator() {
        list.addAll(CharList.of('x', 'y', 'z'));
        com.landawn.abacus.util.CharIterator it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals('x', it.nextChar());
        assertTrue(it.hasNext());
        assertEquals('y', it.nextChar());
        assertTrue(it.hasNext());
        assertEquals('z', it.nextChar());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.nextChar());
    }

    @Test
    public void testStreamAndSubStream() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        long count = list.stream().filter(c -> c > 'b').count();
        assertEquals(3, count);

        CharStream subStream = list.stream(1, 4);
        String result = subStream.mapToObj(c -> String.valueOf(c)).collect(Collectors.joining());
        assertEquals("bcd", result);
    }

    @Test
    public void testArray() {
        list.addAll(CharList.of('q', 'w', 'e'));
        char[] internalArray = list.array();
        assertEquals(3, list.size());

        assertTrue(internalArray.length >= 3);
        assertEquals('q', internalArray[0]);
        assertEquals('w', internalArray[1]);
        assertEquals('e', internalArray[2]);
    }

    @Test
    public void testBoxedAndToList() {
        list.addAll(CharList.of('a', 'b', 'c'));
        List<Character> boxedList = list.boxed();
        assertEquals(Arrays.asList('a', 'b', 'c'), boxedList);

        List<Character> subList = list.boxed(1, 3);
        assertEquals(Arrays.asList('b', 'c'), subList);
    }

    @Test
    public void testToCollectionAndToSet() {
        list.addAll(CharList.of('a', 'b', 'a', 'c'));

        ArrayList<Character> arrayList = list.toCollection(ArrayList::new);
        assertEquals(Arrays.asList('a', 'b', 'a', 'c'), arrayList);

        java.util.Set<Character> set = list.toSet();
        assertEquals(new java.util.HashSet<>(Arrays.asList('a', 'b', 'c')), set);
    }

    @Test
    public void testToMultiset() {
        list.addAll(CharList.of('a', 'b', 'a', 'c', 'b', 'a'));
        Multiset<Character> multiset = list.toMultiset();
        assertEquals(3, multiset.count('a'));
        assertEquals(2, multiset.count('b'));
        assertEquals(1, multiset.count('c'));
    }

    @Test
    public void testToIntList() {
        list.addAll(CharList.of('a', 'b', 'c'));
        com.landawn.abacus.util.IntList intList = list.toIntList();
        assertArrayEquals(new int[] { 97, 98, 99 }, intList.toArray());
    }

    @Test
    public void testReverseSort() {
        list.addAll(CharList.of('d', 'a', 'c', 'b'));
        list.reverseSort();
        assertArrayEquals(new char[] { 'd', 'c', 'b', 'a' }, list.toArray());
    }

    @Test
    public void testBinarySearch() {
        list.addAll(CharList.of('b', 'd', 'f', 'h', 'j'));
        assertEquals(2, list.binarySearch('f'));
        assertTrue(list.binarySearch('e') < 0);

        assertEquals(2, list.binarySearch(1, 4, 'f'));
        assertTrue(list.binarySearch(0, 2, 'f') < 0);
    }

    @Test
    public void testRotate() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.rotate(2);
        assertArrayEquals(new char[] { 'd', 'e', 'a', 'b', 'c' }, list.toArray());

        list.rotate(-3);
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e', 'a' }, list.toArray());
    }

    @Test
    public void testShuffleWithSeededRandom() {
        CharList list1 = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
        CharList list2 = list1.copy();

        list1.shuffle(new Random(12345L));
        list2.shuffle(new Random(12345L));

        assertArrayEquals(list1.toArray(), list2.toArray());
    }

    @Test
    public void testSplit() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'));
        List<CharList> chunks = list.split(3);
        assertEquals(3, chunks.size());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, chunks.get(0).toArray());
        assertArrayEquals(new char[] { 'd', 'e', 'f' }, chunks.get(1).toArray());
        assertArrayEquals(new char[] { 'g', 'h' }, chunks.get(2).toArray());

        List<CharList> chunks2 = list.split(1, 7, 2);
        assertEquals(3, chunks2.size());
        assertArrayEquals(new char[] { 'b', 'c' }, chunks2.get(0).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, chunks2.get(1).toArray());
        assertArrayEquals(new char[] { 'f', 'g' }, chunks2.get(2).toArray());
    }

    @Test
    public void testTrimToSize() {
        CharList listWithCapacity = new CharList(20);
        listWithCapacity.addAll(CharList.of('a', 'b', 'c'));
        listWithCapacity.trimToSize();
        CharList copy = listWithCapacity.copy();
        assertEquals(copy.array().length, listWithCapacity.array().length);
    }

}
