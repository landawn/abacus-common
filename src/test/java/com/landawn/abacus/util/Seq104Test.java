package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;

public class Seq104Test extends TestBase {

    @TempDir
    Path tempDir;

    @Test
    public void testAllMatch() {
        // Test with all elements matching
        Seq<Integer, RuntimeException> seq1 = Seq.of(2, 4, 6, 8);
        assertTrue(seq1.allMatch(n -> n % 2 == 0));

        // Test with not all elements matching
        Seq<Integer, RuntimeException> seq2 = Seq.of(2, 4, 5, 8);
        assertFalse(seq2.allMatch(n -> n % 2 == 0));

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        assertTrue(seq3.allMatch(n -> n % 2 == 0));

        // Test with single element matching
        Seq<Integer, RuntimeException> seq4 = Seq.of(4);
        assertTrue(seq4.allMatch(n -> n % 2 == 0));

        // Test with single element not matching
        Seq<Integer, RuntimeException> seq5 = Seq.of(5);
        assertFalse(seq5.allMatch(n -> n % 2 == 0));
    }

    @Test
    public void testNoneMatch() {
        // Test with no elements matching
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 3, 5, 7);
        assertTrue(seq1.noneMatch(n -> n % 2 == 0));

        // Test with some elements matching
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 3, 4, 7);
        assertFalse(seq2.noneMatch(n -> n % 2 == 0));

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        assertTrue(seq3.noneMatch(n -> n % 2 == 0));

        // Test with all elements matching
        Seq<Integer, RuntimeException> seq4 = Seq.of(2, 4, 6, 8);
        assertFalse(seq4.noneMatch(n -> n % 2 == 0));
    }

    @Test
    public void testNMatch() {
        // Test with exactly in range
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5, 6);
        assertTrue(seq1.nMatch(2, 4, n -> n % 2 == 0));

        // Test with less than atLeast
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertFalse(seq2.nMatch(2, 4, n -> n % 2 == 0));

        // Test with more than atMost
        Seq<Integer, RuntimeException> seq3 = Seq.of(2, 4, 6, 8, 10);
        assertFalse(seq3.nMatch(2, 4, n -> n % 2 == 0));

        // Test with empty sequence 
        assertFalse(Seq.<Integer, RuntimeException> empty().nMatch(1, 2, n -> n % 2 == 0));
        assertTrue(Seq.<Integer, RuntimeException> empty().nMatch(0, 0, n -> n % 2 == 0));

        // Test with illegal arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).nMatch(-1, 2, n -> true));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).nMatch(2, -1, n -> true));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).nMatch(3, 2, n -> true));
    }

    @Test
    public void testFindFirst() {
        // Test finding first even number
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        Optional<Integer> result1 = seq1.findFirst(n -> n % 2 == 0);
        assertTrue(result1.isPresent());
        assertEquals(2, result1.get());

        // Test with no matching element
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 3, 5, 7);
        Optional<Integer> result2 = seq2.findFirst(n -> n % 2 == 0);
        assertFalse(result2.isPresent());

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Optional<Integer> result3 = seq3.findFirst(n -> n % 2 == 0);
        assertFalse(result3.isPresent());

        // Test with first element matching
        Seq<Integer, RuntimeException> seq4 = Seq.of(2, 3, 4, 5);
        Optional<Integer> result4 = seq4.findFirst(n -> n % 2 == 0);
        assertTrue(result4.isPresent());
        assertEquals(2, result4.get());
    }

    @Test
    public void testFindAny() {
        // Since findAny delegates to findFirst in sequential sequences
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5);
        Optional<Integer> result = seq.findAny(n -> n % 2 == 0);
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testFindLast() {
        // Test finding last even number
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        Optional<Integer> result1 = seq1.findLast(n -> n % 2 == 0);
        assertTrue(result1.isPresent());
        assertEquals(4, result1.get());

        // Test with no matching element
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 3, 5, 7);
        Optional<Integer> result2 = seq2.findLast(n -> n % 2 == 0);
        assertFalse(result2.isPresent());

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Optional<Integer> result3 = seq3.findLast(n -> n % 2 == 0);
        assertFalse(result3.isPresent());

        // Test with last element matching
        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 3, 5, 6);
        Optional<Integer> result4 = seq4.findLast(n -> n % 2 == 0);
        assertTrue(result4.isPresent());
        assertEquals(6, result4.get());
    }

    @Test
    public void testContainsAllArray() {
        // Test with all elements present
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsAll(2, 4));

        // Test with empty array
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertTrue(seq2.containsAll());

        // Test with single element
        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertTrue(seq3.containsAll(2));

        // Test with duplicate elements in array
        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 2, 3);
        assertTrue(seq4.containsAll(2, 2));

        // Test with two different elements
        Seq<Integer, RuntimeException> seq5 = Seq.of(1, 2, 3, 4);
        assertTrue(seq5.containsAll(2, 4));

        // Test with missing element
        Seq<Integer, RuntimeException> seq6 = Seq.of(1, 2, 3);
        assertFalse(seq6.containsAll(2, 5));

        // Test with multiple elements where some are missing
        Seq<Integer, RuntimeException> seq7 = Seq.of(1, 2, 3);
        assertFalse(seq7.containsAll(2, 3, 4));
    }

    @Test
    public void testContainsAllCollection() {
        // Test with all elements present
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsAll(Arrays.asList(2, 3, 4)));

        // Test with empty collection
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertTrue(seq2.containsAll(Collections.emptyList()));

        // Test with single element
        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertTrue(seq3.containsAll(Collections.singletonList(2)));

        // Test with Set input
        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 2, 3, 4);
        assertTrue(seq4.containsAll(new HashSet<>(Arrays.asList(2, 3))));

        // Test with missing elements
        Seq<Integer, RuntimeException> seq5 = Seq.of(1, 2, 3);
        assertFalse(seq5.containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testContainsAnyArray() {
        // Test with some elements present
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsAny(7, 3, 9));

        // Test with empty array
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertFalse(seq2.containsAny());

        // Test with single element present
        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertTrue(seq3.containsAny(2));

        // Test with duplicate elements in array
        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 2, 3);
        assertTrue(seq4.containsAny(2, 2));

        // Test with two different elements
        Seq<Integer, RuntimeException> seq5 = Seq.of(1, 2, 3);
        assertTrue(seq5.containsAny(2, 5));

        // Test with no elements present
        Seq<Integer, RuntimeException> seq6 = Seq.of(1, 2, 3);
        assertFalse(seq6.containsAny(4, 5, 6));

        // Test with more than two elements
        Seq<Integer, RuntimeException> seq7 = Seq.of(1, 2, 3);
        assertTrue(seq7.containsAny(5, 6, 7, 1));
    }

    @Test
    public void testContainsAnyCollection() {
        // Test with some elements present
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsAny(Arrays.asList(7, 3, 9)));

        // Test with empty collection
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertFalse(seq2.containsAny(Collections.emptyList()));

        // Test with single element
        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertTrue(seq3.containsAny(Collections.singletonList(2)));

        // Test with Set input
        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 2, 3);
        assertTrue(seq4.containsAny(new HashSet<>(Arrays.asList(5, 2))));

        // Test with no elements present
        Seq<Integer, RuntimeException> seq5 = Seq.of(1, 2, 3);
        assertFalse(seq5.containsAny(Arrays.asList(4, 5, 6)));
    }

    @Test
    public void testContainsNoneArray() {
        // Test with no elements present
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsNone(6, 7, 8));

        // Test with empty array
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertTrue(seq2.containsNone());

        // Test with some elements present
        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertFalse(seq3.containsNone(4, 2, 5));
    }

    @Test
    public void testContainsNoneCollection() {
        // Test with no elements present
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsNone(Arrays.asList(6, 7, 8)));

        // Test with empty collection
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertTrue(seq2.containsNone(Collections.emptyList()));

        // Test with some elements present
        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertFalse(seq3.containsNone(Arrays.asList(4, 2, 5)));
    }

    @Test
    public void testHasDuplicates() {
        // Test with duplicates
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 2, 4);
        assertTrue(seq1.hasDuplicates());

        // Test without duplicates
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3, 4, 5);
        assertFalse(seq2.hasDuplicates());

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        assertFalse(seq3.hasDuplicates());

        // Test with single element
        Seq<Integer, RuntimeException> seq4 = Seq.of(1);
        assertFalse(seq4.hasDuplicates());
    }

    @Test
    public void testKthLargest() {
        // Test basic kth largest
        Seq<Integer, RuntimeException> seq1 = Seq.of(3, 1, 4, 1, 5, 9, 2, 6);
        Optional<Integer> result1 = seq1.kthLargest(3, Integer::compare);
        assertTrue(result1.isPresent());
        assertEquals(5, result1.get());

        // Test with k=1 (largest)
        Seq<Integer, RuntimeException> seq2 = Seq.of(3, 1, 4, 1, 5);
        Optional<Integer> result2 = seq2.kthLargest(1, Integer::compare);
        assertTrue(result2.isPresent());
        assertEquals(5, result2.get());

        // Test with k larger than sequence size
        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        Optional<Integer> result3 = seq3.kthLargest(5, Integer::compare);
        assertFalse(result3.isPresent());

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq4 = Seq.<Integer, RuntimeException> empty();
        Optional<Integer> result4 = seq4.kthLargest(1, Integer::compare);
        assertFalse(result4.isPresent());

        // Test with null comparator (natural ordering)
        Seq<Integer, RuntimeException> seq5 = Seq.of(3, 1, 4, 1, 5);
        Optional<Integer> result5 = seq5.kthLargest(2, null);
        assertTrue(result5.isPresent());
        assertEquals(4, result5.get());

        // Test with sorted sequence
        Seq<Integer, RuntimeException> seq6 = Seq.<Integer, RuntimeException> of(1, 2, 3, 4, 5).sorted();
        Optional<Integer> result6 = seq6.kthLargest(2, Integer::compare);
        assertTrue(result6.isPresent());
        assertEquals(4, result6.get());

        // Test with illegal k value
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).kthLargest(0, Integer::compare));
    }

    @Test
    public void testPercentiles() {
        // Test with normal sequence
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Optional<Map<Percentage, Integer>> result1 = seq1.percentiles();
        assertTrue(result1.isPresent());
        assertNotNull(result1.get());
        assertFalse(result1.get().isEmpty());

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq2 = Seq.<Integer, RuntimeException> empty();
        Optional<Map<Percentage, Integer>> result2 = seq2.percentiles();
        assertFalse(result2.isPresent());

        // Test with single element
        Seq<Integer, RuntimeException> seq3 = Seq.of(5);
        Optional<Map<Percentage, Integer>> result3 = seq3.percentiles();
        assertTrue(result3.isPresent());
        assertNotNull(result3.get());
    }

    @Test
    public void testPercentilesWithComparator() {
        // Test with string comparator
        Seq<String, RuntimeException> seq1 = Seq.of("apple", "pie", "banana", "zoo");
        Optional<Map<Percentage, String>> result1 = seq1.percentiles(String::compareTo);
        assertTrue(result1.isPresent());
        assertNotNull(result1.get());

        // Test with empty sequence
        Seq<String, RuntimeException> seq2 = Seq.<String, RuntimeException> empty();
        Optional<Map<Percentage, String>> result2 = seq2.percentiles(String::compareTo);
        assertFalse(result2.isPresent());

        // Test with null comparator
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).percentiles(null));
    }

    @Test
    public void testFirst() {
        // Test with multiple elements
        Seq<String, RuntimeException> seq1 = Seq.of("first", "second", "third");
        Optional<String> result1 = seq1.first();
        assertTrue(result1.isPresent());
        assertEquals("first", result1.get());

        // Test with empty sequence
        Seq<String, RuntimeException> seq2 = Seq.<String, RuntimeException> empty();
        Optional<String> result2 = seq2.first();
        assertFalse(result2.isPresent());

        // Test with single element
        Seq<String, RuntimeException> seq3 = Seq.of("only");
        Optional<String> result3 = seq3.first();
        assertTrue(result3.isPresent());
        assertEquals("only", result3.get());
    }

    @Test
    public void testLast() {
        // Test with multiple elements
        Seq<String, RuntimeException> seq1 = Seq.of("first", "second", "third");
        Optional<String> result1 = seq1.last();
        assertTrue(result1.isPresent());
        assertEquals("third", result1.get());

        // Test with empty sequence
        Seq<String, RuntimeException> seq2 = Seq.<String, RuntimeException> empty();
        Optional<String> result2 = seq2.last();
        assertFalse(result2.isPresent());

        // Test with single element
        Seq<String, RuntimeException> seq3 = Seq.of("only");
        Optional<String> result3 = seq3.last();
        assertTrue(result3.isPresent());
        assertEquals("only", result3.get());
    }

    @Test
    public void testElementAt() {
        // Test with valid position
        Seq<String, RuntimeException> seq1 = Seq.of("zero", "one", "two", "three");
        Optional<String> result1 = seq1.elementAt(2);
        assertTrue(result1.isPresent());
        assertEquals("two", result1.get());

        // Test with position 0
        Seq<String, RuntimeException> seq2 = Seq.of("zero", "one", "two");
        Optional<String> result2 = seq2.elementAt(0);
        assertTrue(result2.isPresent());
        assertEquals("zero", result2.get());

        // Test with position beyond sequence length
        Seq<String, RuntimeException> seq3 = Seq.of("zero", "one");
        Optional<String> result3 = seq3.elementAt(5);
        assertFalse(result3.isPresent());

        // Test with negative position
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a", "b").elementAt(-1));
    }

    @Test
    public void testOnlyOne() {
        // Test with single element
        Seq<String, RuntimeException> seq1 = Seq.of("single");
        Optional<String> result1 = seq1.onlyOne();
        assertTrue(result1.isPresent());
        assertEquals("single", result1.get());

        // Test with multiple elements
        Seq<String, RuntimeException> seq2 = Seq.of("first", "second");
        assertThrows(TooManyElementsException.class, () -> seq2.onlyOne());

        // Test with empty sequence
        Seq<String, RuntimeException> seq3 = Seq.<String, RuntimeException> empty();
        Optional<String> result3 = seq3.onlyOne();
        assertFalse(result3.isPresent());
    }

    @Test
    public void testCount() {
        // Test with multiple elements
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c", "d");
        assertEquals(4, seq1.count());

        // Test with empty sequence
        Seq<String, RuntimeException> seq2 = Seq.<String, RuntimeException> empty();
        assertEquals(0, seq2.count());

        // Test with single element
        Seq<String, RuntimeException> seq3 = Seq.of("single");
        assertEquals(1, seq3.count());
    }

    @Test
    public void testToArray() {
        // Test with multiple elements
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        Object[] array1 = seq1.toArray();
        assertArrayEquals(new Object[] { "a", "b", "c" }, array1);

        // Test with empty sequence
        Seq<String, RuntimeException> seq2 = Seq.<String, RuntimeException> empty();
        Object[] array2 = seq2.toArray();
        assertEquals(0, array2.length);
    }

    @Test
    public void testToArrayWithGenerator() {
        // Test with String array
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        String[] array1 = seq1.toArray(String[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, array1);

        // Test with Integer array
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        Integer[] array2 = seq2.toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array2);

        // Test with null generator
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toArray(null));
    }

    @Test
    public void testToList() {
        // Test with multiple elements
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        List<Integer> list1 = seq1.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list1);

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq2 = Seq.<Integer, RuntimeException> empty();
        List<Integer> list2 = seq2.toList();
        assertTrue(list2.isEmpty());
    }

    @Test
    public void testToSet() {
        // Test with duplicates
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 2, 4, 3);
        Set<Integer> set1 = seq1.toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4)), set1);

        // Test with no duplicates
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3, 4);
        Set<Integer> set2 = seq2.toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4)), set2);

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Set<Integer> set3 = seq3.toSet();
        assertTrue(set3.isEmpty());
    }

    @Test
    public void testToCollection() {
        // Test with LinkedList
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        LinkedList<String> list1 = seq1.toCollection(LinkedList::new);
        assertEquals(Arrays.asList("a", "b", "c"), list1);

        // Test with TreeSet
        Seq<Integer, RuntimeException> seq2 = Seq.of(3, 1, 4, 1, 5);
        TreeSet<Integer> set2 = seq2.toCollection(TreeSet::new);
        assertEquals(new TreeSet<>(Arrays.asList(1, 3, 4, 5)), set2);

        // Test with null supplier
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollection(null));
    }

    @Test
    public void testToImmutableList() {
        Seq<String, RuntimeException> seq = Seq.of("a", "b", "c");
        ImmutableList<String> list = seq.toImmutableList();
        assertEquals(Arrays.asList("a", "b", "c"), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add("d"));
    }

    @Test
    public void testToImmutableSet() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 2, 4);
        ImmutableSet<Integer> set = seq.toImmutableSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(5));
    }

    @Test
    public void testToListThenApply() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5);
        int sum = seq.toListThenApply(list -> list.stream().mapToInt(Integer::intValue).sum());
        assertEquals(15, sum);

        // Test with null function
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toListThenApply(null));
    }

    @Test
    public void testToListThenAccept() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "cherry");
        AtomicInteger count = new AtomicInteger(0);
        seq.toListThenAccept(list -> {
            count.set(list.size());
            assertEquals(3, list.size());
        });
        assertEquals(3, count.get());

        // Test with null consumer
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toListThenAccept(null));
    }

    @Test
    public void testToSetThenApply() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 2, 4, 3);
        int distinctCount = seq.toSetThenApply(Set::size);
        assertEquals(4, distinctCount);

        // Test with null function
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toSetThenApply(null));
    }

    @Test
    public void testToSetThenAccept() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "apple", "cherry");
        AtomicInteger count = new AtomicInteger(0);
        seq.toSetThenAccept(set -> {
            count.set(set.size());
            assertEquals(3, set.size());
        });
        assertEquals(3, count.get());

        // Test with null consumer
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toSetThenAccept(null));
    }

    @Test
    public void testToCollectionThenApply() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "cherry");
        String first = seq.toCollectionThenApply(LinkedList::new, list -> list.getFirst());
        assertEquals("apple", first);

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenApply(null, Fn.identity()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenApply(ArrayList::new, null));
    }

    @Test
    public void testToCollectionThenAccept() {
        Seq<Integer, RuntimeException> seq = Seq.of(3, 1, 4, 1, 5);
        List<Integer> result = new ArrayList<>();
        seq.toCollectionThenAccept(TreeSet::new, set -> set.forEach(result::add));
        assertEquals(Arrays.asList(1, 3, 4, 5), result);

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenAccept(null, s -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenAccept(ArrayList::new, null));
    }

    @Test
    public void testToMapWithKeyValueMappers() {
        // Test basic mapping
        Seq<String, RuntimeException> seq1 = Seq.of("apple", "banana", "cherry");
        Map<String, Integer> map1 = seq1.toMap(Fn.identity(), String::length);
        assertEquals(3, map1.size());
        assertEquals(5, map1.get("apple"));
        assertEquals(6, map1.get("banana"));

        // Test with duplicate keys (should throw)
        Seq<String, RuntimeException> seq2 = Seq.of("apple", "apple");
        assertThrows(IllegalStateException.class, () -> seq2.toMap(Fn.identity(), String::length));
    }

    @Test
    public void testToMapWithKeyValueMappersAndMapFactory() {
        // Test with LinkedHashMap
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "cherry");
        Map<String, Integer> map = seq.toMap(Fn.identity(), String::length, Suppliers.ofLinkedHashMap());
        assertEquals(3, map.size());
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), keys);
    }

    @Test
    public void testToMapWithMergeFunction() {
        // Test with duplicate keys and merge function
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        Map<Character, String> map = seq.toMap(s -> s.charAt(0), Fn.identity(), (s1, s2) -> s1 + "," + s2);
        assertEquals(2, map.size());
        assertEquals("apple,apricot", map.get('a'));
        assertEquals("banana", map.get('b'));
    }

    @Test
    public void testToMapWithMergeFunctionAndMapFactory() {
        // Test with all parameters
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5);
        TreeMap<Boolean, Integer> map = seq.toMap(n -> n % 2 == 0, Fn.identity(), Integer::sum, TreeMap::new);
        assertEquals(2, map.size());
        assertEquals(6, map.get(true)); // 2 + 4
        assertEquals(9, map.get(false)); // 1 + 3 + 5

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(null, Fn.identity(), Integer::sum, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(Fn.identity(), null, Integer::sum, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(Fn.identity(), Fn.identity(), null, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(Fn.identity(), Fn.identity(), Integer::sum, null));
    }

    @Test
    public void testToImmutableMapWithKeyValueMappers() {
        Seq<String, RuntimeException> seq = Seq.of("a", "b", "c");
        ImmutableMap<String, Integer> map = seq.toImmutableMap(Fn.identity(), String::length);
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 1));
    }

    @Test
    public void testToImmutableMapWithMergeFunction() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        ImmutableMap<Character, String> map = seq.toImmutableMap(s -> s.charAt(0), Fn.identity(), (s1, s2) -> s1 + "," + s2);
        assertEquals(2, map.size());
        assertEquals("apple,apricot", map.get('a'));
        assertThrows(UnsupportedOperationException.class, () -> map.put('c', "cherry"));
    }

    @Test
    public void testGroupToWithKeyMapper() {
        // Test basic grouping
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana", "blueberry");
        Map<Character, List<String>> map = seq.groupTo(s -> s.charAt(0));
        assertEquals(2, map.size());
        assertEquals(Arrays.asList("apple", "apricot"), map.get('a'));
        assertEquals(Arrays.asList("banana", "blueberry"), map.get('b'));
    }

    @Test
    public void testGroupToWithKeyMapperAndMapFactory() {
        // Test with TreeMap
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5, 6);
        TreeMap<Integer, List<Integer>> map = seq.groupTo(n -> n % 3, Suppliers.ofTreeMap());
        assertEquals(3, map.size());
        assertEquals(Arrays.asList(3, 6), map.get(0));
        assertEquals(Arrays.asList(1, 4), map.get(1));
        assertEquals(Arrays.asList(2, 5), map.get(2));
    }

    @Test
    public void testGroupToWithKeyValueMappers() {
        // Test with value transformation
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "cherry");
        Map<Integer, List<String>> map = seq.groupTo(String::length, String::toUpperCase);
        assertEquals(2, map.size());
        assertEquals(Arrays.asList("APPLE"), map.get(5));
        assertEquals(Arrays.asList("BANANA", "CHERRY"), map.get(6));
    }

    @Test
    public void testGroupToWithKeyValueMappersAndMapFactory() {
        // Test with all parameters
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        LinkedHashMap<Character, List<Integer>> map = seq.groupTo(s -> s.charAt(0), String::length, LinkedHashMap::new);
        assertEquals(2, map.size());
        assertEquals(Arrays.asList(5, 7), map.get('a'));
        assertEquals(Arrays.asList(6), map.get('b'));

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(null, String::length, Suppliers.ofLinkedHashMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), (Throwables.Function) null, Suppliers.ofLinkedHashMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, (Supplier) null));
    }

    @Test
    public void testGroupToWithDownstreamCollector() {
        // Test with counting collector
        Seq<String, RuntimeException> seq1 = Seq.of("apple", "apricot", "banana", "blueberry");
        Map<Character, Long> map1 = seq1.groupTo(s -> s.charAt(0), Collectors.counting());
        assertEquals(2, map1.size());
        assertEquals(2L, map1.get('a'));
        assertEquals(2L, map1.get('b'));

        // Test with averaging collector
        Seq<String, RuntimeException> seq2 = Seq.of("a", "bb", "ccc", "dd");
        Map<Integer, Double> map2 = seq2.groupTo(String::length, Collectors.averagingInt(s -> s.charAt(0)));
        assertEquals(3, map2.size());
    }

    @Test
    public void testGroupToWithDownstreamCollectorAndMapFactory() {
        // Test with joining collector
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        TreeMap<Character, String> map = seq.groupTo(s -> s.charAt(0), Collectors.joining(", "), TreeMap::new);
        assertEquals(2, map.size());
        assertEquals("apple, apricot", map.get('a'));
        assertEquals("banana", map.get('b'));
    }

    @Test
    public void testGroupToWithValueMapperAndDownstreamCollector() {
        // Test with value transformation and collector
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana", "blueberry");
        Map<Character, String> map = seq.groupTo(s -> s.charAt(0), String::toUpperCase, Collectors.joining(", "));
        assertEquals(2, map.size());
        assertEquals("APPLE, APRICOT", map.get('a'));
        assertEquals("BANANA, BLUEBERRY", map.get('b'));
    }

    @Test
    public void testGroupToWithValueMapperDownstreamCollectorAndMapFactory() {
        // Test with all parameters
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        Map<Character, Optional<Integer>> map = seq.groupTo(s -> s.charAt(0), String::length, MoreCollectors.max(), Suppliers.ofLinkedHashMap());
        assertEquals(2, map.size());
        assertEquals(7, map.get('a').get());
        assertEquals(6, map.get('b').get());

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(null, String::length, Collectors.counting(), LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), null, Collectors.counting(), LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, null, LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, Collectors.counting(), null));
    }

    @Test
    public void testPartitionToWithPredicate() {
        // Test basic partitioning
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, List<Integer>> map = seq.partitionTo(n -> n % 2 == 0);
        assertEquals(2, map.size());
        assertEquals(Arrays.asList(2, 4, 6), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionToWithPredicateAndDownstreamCollector() {
        // Test with counting collector
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, Long> map1 = seq1.partitionTo(n -> n >= 4, Collectors.counting());
        assertEquals(2, map1.size());
        assertEquals(3L, map1.get(true));
        assertEquals(3L, map1.get(false));

        // Test with all elements in one partition
        Seq<Integer, RuntimeException> seq2 = Seq.of(2, 4, 6);
        Map<Boolean, Long> map2 = seq2.partitionTo(n -> n % 2 == 0, Collectors.counting());
        assertEquals(2, map2.size());
        assertEquals(3L, map2.get(true));
        assertEquals(0L, map2.get(false));

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Map<Boolean, Long> map3 = seq3.partitionTo(n -> n > 0, Collectors.counting());
        assertEquals(2, map3.size());
        assertEquals(0L, map3.get(true));
        assertEquals(0L, map3.get(false));
    }

    @Test
    public void testToMultimapWithKeyMapper() {
        // Test basic multimap creation
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        ListMultimap<Character, String> multimap = seq.toMultimap(s -> s.charAt(0));
        assertEquals(2, multimap.keySet().size());
        assertEquals(Arrays.asList("apple", "apricot"), multimap.get('a'));
        assertEquals(Arrays.asList("banana"), multimap.get('b'));
    }

    @Test
    public void testToMultimapWithKeyMapperAndMapFactory() {
        // Test with SetMultimap
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Multimap<Integer, Integer, ? extends Collection<Integer>> multimap = seq.toMultimap(n -> n % 3, Suppliers.ofSetMultimap());
        assertEquals(3, multimap.keySet().size());
        assertTrue(multimap.get(0).contains(3));
        assertTrue(multimap.get(0).contains(6));
    }

    @Test
    public void testToMultimapWithKeyValueMappers() {
        // Test with value transformation
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        ListMultimap<Character, Integer> multimap = seq.toMultimap(s -> s.charAt(0), String::length);
        assertEquals(2, multimap.keySet().size());
        assertEquals(Arrays.asList(5, 7), multimap.get('a'));
        assertEquals(Arrays.asList(6), multimap.get('b'));
    }

    @Test
    public void testToMultimapWithKeyValueMappersAndMapFactory() {
        // Test with all parameters
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana", "blueberry");
        Multimap<Integer, String, ? extends Collection<String>> multimap = seq.toMultimap(String::length, String::toUpperCase, Suppliers.ofSetMultimap());
        assertTrue(multimap.get(5).contains("APPLE"));
        assertTrue(multimap.get(6).contains("BANANA"));
        assertTrue(multimap.get(9).contains("BLUEBERRY"));

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(null, String::length, Suppliers.ofListMultimap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(s -> s.charAt(0), null, Suppliers.ofListMultimap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(s -> s.charAt(0), String::length, null));
    }

    @Test
    public void testToMultiset() {
        // Test with duplicates
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "apple", "cherry", "banana", "apple");
        Multiset<String> multiset = seq.toMultiset();
        assertEquals(3, multiset.count("apple"));
        assertEquals(2, multiset.count("banana"));
        assertEquals(1, multiset.count("cherry"));
        assertEquals(0, multiset.count("orange"));
    }

    @Test
    public void testToMultisetWithSupplier() {
        // Test with custom supplier
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 2, 3, 3, 3);
        Multiset<Integer> multiset = seq.toMultiset(Multiset::new);
        assertEquals(1, multiset.count(1));
        assertEquals(2, multiset.count(2));
        assertEquals(3, multiset.count(3));

        // Test with null supplier
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toMultiset(null));
    }

    @Test
    public void testToDataset() throws Exception {
        // Test with objects
        Seq<Map<String, Object>, Exception> seq = Seq.of(N.asMap("name", "John", "age", 25), N.asMap("name", "Jane", "age", 30));
        Dataset dataset = seq.toDataset();
        assertNotNull(dataset);
        assertEquals(2, dataset.size());
    }

    @Test
    public void testToDatasetWithColumnNames() {
        // Test with specified column names
        List<String> columns = Arrays.asList("Name", "Age");
        Seq<List<Object>, RuntimeException> seq = Seq.of(Arrays.asList("John", 25), Arrays.asList("Jane", 30));
        Dataset dataset = seq.toDataset(columns);
        assertNotNull(dataset);
        assertEquals(2, dataset.size());
    }

    @Test
    public void testSumInt() {
        // Test with normal values
        Seq<String, RuntimeException> seq1 = Seq.of("apple", "banana", "cherry");
        long sum1 = seq1.sumInt(String::length);
        assertEquals(17, sum1); // 5 + 6 + 6

        // Test with empty sequence
        Seq<String, RuntimeException> seq2 = Seq.<String, RuntimeException> empty();
        long sum2 = seq2.sumInt(String::length);
        assertEquals(0, sum2);

        // Test with negative values
        Seq<Integer, RuntimeException> seq3 = Seq.of(-1, -2, -3);
        long sum3 = seq3.sumInt(Integer::intValue);
        assertEquals(-6, sum3);
    }

    @Test
    public void testSumLong() {
        // Test with normal values
        Seq<Long, RuntimeException> seq1 = Seq.of(1000000000L, 2000000000L, 3000000000L);
        long sum1 = seq1.sumLong(Long::longValue);
        assertEquals(6000000000L, sum1);

        // Test with empty sequence
        Seq<Long, RuntimeException> seq2 = Seq.<Long, RuntimeException> empty();
        long sum2 = seq2.sumLong(Long::longValue);
        assertEquals(0, sum2);
    }

    @Test
    public void testSumDouble() {
        // Test with normal values
        Seq<Double, RuntimeException> seq1 = Seq.of(1.5, 2.5, 3.5);
        double sum1 = seq1.sumDouble(Double::doubleValue);
        assertEquals(7.5, sum1, 0.0001);

        // Test with empty sequence
        Seq<Double, RuntimeException> seq2 = Seq.<Double, RuntimeException> empty();
        double sum2 = seq2.sumDouble(Double::doubleValue);
        assertEquals(0.0, sum2, 0.0001);
    }

    @Test
    public void testAverageInt() {
        // Test with normal values
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        OptionalDouble avg1 = seq1.averageInt(Integer::intValue);
        assertTrue(avg1.isPresent());
        assertEquals(3.0, avg1.getAsDouble(), 0.0001);

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq2 = Seq.<Integer, RuntimeException> empty();
        OptionalDouble avg2 = seq2.averageInt(Integer::intValue);
        assertFalse(avg2.isPresent());
    }

    @Test
    public void testAverageLong() {
        // Test with normal values
        Seq<Long, RuntimeException> seq1 = Seq.of(10L, 20L, 30L);
        OptionalDouble avg1 = seq1.averageLong(Long::longValue);
        assertTrue(avg1.isPresent());
        assertEquals(20.0, avg1.getAsDouble(), 0.0001);

        // Test with empty sequence
        Seq<Long, RuntimeException> seq2 = Seq.<Long, RuntimeException> empty();
        OptionalDouble avg2 = seq2.averageLong(Long::longValue);
        assertFalse(avg2.isPresent());
    }

    @Test
    public void testAverageDouble() {
        // Test with normal values
        Seq<Double, RuntimeException> seq1 = Seq.of(1.0, 2.0, 3.0, 4.0);
        OptionalDouble avg1 = seq1.averageDouble(Double::doubleValue);
        assertTrue(avg1.isPresent());
        assertEquals(2.5, avg1.getAsDouble(), 0.0001);

        // Test with empty sequence
        Seq<Double, RuntimeException> seq2 = Seq.<Double, RuntimeException> empty();
        OptionalDouble avg2 = seq2.averageDouble(Double::doubleValue);
        assertFalse(avg2.isPresent());
    }

    @Test
    public void testReduceWithAccumulator() {
        // Test sum
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        Optional<Integer> sum = seq1.reduce(Integer::sum);
        assertTrue(sum.isPresent());
        assertEquals(15, sum.get());

        // Test max
        Seq<Integer, RuntimeException> seq2 = Seq.of(3, 1, 4, 1, 5);
        Optional<Integer> max = seq2.reduce(Integer::max);
        assertTrue(max.isPresent());
        assertEquals(5, max.get());

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Optional<Integer> result = seq3.reduce(Integer::sum);
        assertFalse(result.isPresent());

        // Test with single element
        Seq<Integer, RuntimeException> seq4 = Seq.of(42);
        Optional<Integer> single = seq4.reduce(Integer::sum);
        assertTrue(single.isPresent());
        assertEquals(42, single.get());

        // Test with null accumulator
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).reduce(null));
    }

    @Test
    public void testReduceWithIdentityAndAccumulator() {
        // Test sum with identity
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        Integer sum = seq1.reduce(10, Integer::sum);
        assertEquals(25, sum); // 10 + 1 + 2 + 3 + 4 + 5

        // Test string concatenation
        Seq<String, RuntimeException> seq2 = Seq.of("a", "b", "c");
        String result = seq2.reduce("Start:", (acc, str) -> acc + str);
        assertEquals("Start:abc", result);

        // Test with empty sequence
        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Integer empty = seq3.reduce(100, Integer::sum);
        assertEquals(100, empty);

        // Test with null accumulator
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).reduce(0, null));
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        // Test collect to List
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        List<String> list = seq1.collect(ArrayList::new, List::add);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        // Test collect to StringBuilder
        Seq<String, RuntimeException> seq2 = Seq.of("Hello", " ", "World");
        StringBuilder sb = seq2.collect(StringBuilder::new, StringBuilder::append);
        assertEquals("Hello World", sb.toString());

        // Test with empty sequence
        Seq<String, RuntimeException> seq3 = Seq.<String, RuntimeException> empty();
        List<String> empty = seq3.collect(ArrayList::new, List::add);
        assertTrue(empty.isEmpty());

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).collect((Throwables.Supplier<List<Integer>, Exception>) null, (l, i) -> l.add(i)));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).collect(ArrayList::new, null));
    }

    @Test
    public void testCollectWithSupplierAccumulatorAndFinisher() {
        // Test collect with finisher
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        Integer size = seq1.collect(ArrayList::new, List::add, List::size);
        assertEquals(3, size);

        // Test with StringBuilder and uppercase finisher
        Seq<String, RuntimeException> seq2 = Seq.of("hello", " ", "world");
        String result = seq2.collect(StringBuilder::new, StringBuilder::append, sb -> sb.toString().toUpperCase());
        assertEquals("HELLO WORLD", result);

        // Test with null arguments
        assertThrows(IllegalArgumentException.class,
                () -> Seq.of(1).collect((Throwables.Supplier<List<Integer>, Exception>) null, (l, i) -> l.add(i), List::size));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).collect(ArrayList::new, null, List::size));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).collect(ArrayList::new, List::add, null));
    }

    @Test
    public void testCollectWithCollector() {
        // Test with toList collector
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        List<String> list = seq1.collect(Collectors.toList());
        assertEquals(Arrays.asList("a", "b", "c"), list);

        // Test with groupingBy collector
        Seq<String, RuntimeException> seq2 = Seq.of("a", "bb", "ccc", "dd");
        Map<Integer, List<String>> grouped = seq2.collect(Collectors.groupingBy(String::length));
        assertEquals(Arrays.asList("a"), grouped.get(1));
        assertEquals(Arrays.asList("bb", "dd"), grouped.get(2));
        assertEquals(Arrays.asList("ccc"), grouped.get(3));

        // Test with null collector
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).collect(null));
    }

    @Test
    public void testCollectThenApply() {
        // Test collect to List then check contains
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        Boolean contains = seq1.collectThenApply(Collectors.toList(), list -> list.contains("b"));
        assertTrue(contains);

        // Test collect to Set then get size
        Seq<String, RuntimeException> seq2 = Seq.of("a", "b", "a", "c", "b");
        Integer uniqueCount = seq2.collectThenApply(Collectors.toSet(), Set::size);
        assertEquals(3, uniqueCount);

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).collectThenApply((Collector<Integer, Object, List<Integer>>) null, List::size));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).collectThenApply(Collectors.toList(), null));
    }

    @Test
    public void testCollectThenAccept() {
        // Test collect to List then print
        Seq<String, RuntimeException> seq = Seq.of("a", "b", "c");
        AtomicInteger count = new AtomicInteger(0);
        seq.collectThenAccept(Collectors.toList(), list -> count.set(list.size()));
        assertEquals(3, count.get());

        // Test with null arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).collectThenAccept(null, System.out::println));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).collectThenAccept(Collectors.toList(), null));
    }

    @Test
    public void testJoinWithDelimiter() {
        // Test with strings
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        String result1 = seq1.join(", ");
        assertEquals("a, b, c", result1);

        // Test with numbers
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        String result2 = seq2.join("-");
        assertEquals("1-2-3", result2);

        // Test with empty sequence
        Seq<String, RuntimeException> seq3 = Seq.<String, RuntimeException> empty();
        String result3 = seq3.join(", ");
        assertEquals("", result3);

        // Test with single element
        Seq<String, RuntimeException> seq4 = Seq.of("single");
        String result4 = seq4.join(", ");
        assertEquals("single", result4);
    }

    @Test
    public void testJoinWithDelimiterPrefixSuffix() {
        // Test with brackets
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        String result1 = seq1.join(", ", "[", "]");
        assertEquals("[a, b, c]", result1);

        // Test SQL IN clause style
        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        String result2 = seq2.join(",", "WHERE id IN (", ")");
        assertEquals("WHERE id IN (1,2,3)", result2);

        // Test with empty sequence
        Seq<String, RuntimeException> seq3 = Seq.<String, RuntimeException> empty();
        String result3 = seq3.join(", ", "{", "}");
        assertEquals("{}", result3);
    }

    @Test
    public void testJoinTo() {
        // Test with existing Joiner
        Joiner joiner = Joiner.with(", ", "[", "]");
        Seq<String, RuntimeException> seq = Seq.of("a", "b", "c");
        Joiner result = seq.joinTo(joiner);
        assertEquals("[a, b, c]", result.toString());
        assertSame(joiner, result);

        // Test with null joiner
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).joinTo(null));
    }

}