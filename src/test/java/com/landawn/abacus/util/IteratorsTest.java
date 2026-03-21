package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

public class IteratorsTest extends TestBase {

    private List<Integer> intList;
    private List<Integer> testList;
    private List<Integer> emptyList;
    private Iterator<Integer> testIterator;

    @BeforeEach
    public void setUp() {
        testList = Arrays.asList(1, 2, 3, 4, 5);
        intList = testList;
        emptyList = Collections.emptyList();
        testIterator = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).iterator();
    }

    @SafeVarargs
    private static <T> List<T> list(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    // ===================== elementAt Dedicated Tests =====================

    @Test
    public void testElementAt() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        Nullable<String> result = Iterators.elementAt(iter, 1);
        assertTrue(result.isPresent());
        assertEquals("b", result.get());
    }

    @Test
    public void testElementAt_OutOfBounds() {
        Iterator<String> iter = Arrays.asList("a", "b").iterator();
        Nullable<String> result = Iterators.elementAt(iter, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_NullIterator() {
        Nullable<String> result = Iterators.elementAt(null, 0);
        assertFalse(result.isPresent());
    }

    // ===================== elementAt Additional Edge Cases =====================

    @Test
    public void testElementAt_FirstElement() {
        Nullable<String> result = Iterators.elementAt(Arrays.asList("x", "y", "z").iterator(), 0);
        assertTrue(result.isPresent());
        assertEquals("x", result.get());
    }

    @Test
    public void testElementAt_LastElement() {
        Nullable<String> result = Iterators.elementAt(Arrays.asList("x", "y", "z").iterator(), 2);
        assertTrue(result.isPresent());
        assertEquals("z", result.get());
    }

    @Test
    public void testElementAt_EmptyIterator() {
        Nullable<String> result = Iterators.elementAt(Collections.<String> emptyIterator(), 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_SingleElement() {
        Nullable<Integer> result = Iterators.elementAt(Arrays.asList(42).iterator(), 0);
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testElementAt_NullElement() {
        Nullable<String> result = Iterators.elementAt(Arrays.asList("a", null, "c").iterator(), 1);
        assertTrue(result.isPresent());
        assertEquals(null, result.orElse("fallback"));
    }

    @Test
    public void testGet() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "d").iterator();

        Nullable<String> result = Iterators.elementAt(iter, 0);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());

        iter = Arrays.asList("a", "b", "c", "d").iterator();
        result = Iterators.elementAt(iter, 2);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());

        iter = Arrays.asList("a", "b", "c").iterator();
        result = Iterators.elementAt(iter, 10);
        assertFalse(result.isPresent());

        result = Iterators.elementAt(null, 0);
        assertFalse(result.isPresent());

        assertThrows(IllegalArgumentException.class, () -> Iterators.elementAt(Arrays.asList("a").iterator(), -1));
    }

    @Test
    public void testGetNegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.elementAt(testIterator, -1));
    }

    @Test
    public void testElementAt_NegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.elementAt(Arrays.asList("a").iterator(), -1));
    }

    // ===================== frequency Dedicated Tests =====================

    @Test
    public void testFrequency() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 2, 4, 2, 5);
        assertEquals(3, Iterators.frequency(numbers.iterator(), 2));
        assertEquals(0, Iterators.frequency(numbers.iterator(), 99));
    }

    @Test
    public void testFrequency_AllMatch() {
        assertEquals(3, Iterators.frequency(Arrays.asList("a", "a", "a").iterator(), "a"));
    }

    @Test
    public void testFrequency_NoMatch() {
        assertEquals(0, Iterators.frequency(Arrays.asList("a", "b", "c").iterator(), "z"));
    }

    @Test
    public void testOccurrencesOf() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 2, 4, 2, 5);
        assertEquals(3, Iterators.frequency(numbers.iterator(), 2));
        assertEquals(1, Iterators.frequency(numbers.iterator(), 5));
        assertEquals(0, Iterators.frequency(numbers.iterator(), 6));

        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");
        assertEquals(2, Iterators.frequency(withNulls.iterator(), null));

        assertEquals(0, Iterators.frequency(null, "test"));
    }

    @Test
    public void testFrequency_NullElement() {
        List<String> withNulls = Arrays.asList("a", null, "b", null);
        assertEquals(2, Iterators.frequency(withNulls.iterator(), null));
    }

    @Test
    public void testFrequency_NullIterator() {
        assertEquals(0, Iterators.frequency(null, "test"));
    }

    // ===================== frequency Additional Edge Cases =====================

    @Test
    public void testFrequency_EmptyIterator() {
        assertEquals(0, Iterators.frequency(Collections.emptyIterator(), "a"));
    }

    @Test
    public void testFrequency_NullInList() {
        assertEquals(0, Iterators.frequency(Arrays.asList(null, null, null).iterator(), "x"));
    }

    @Test
    public void testCountWithPredicate_AllMatch() {
        assertEquals(3, Iterators.count(Arrays.asList(2, 4, 6).iterator(), n -> n % 2 == 0));
    }

    @Test
    public void testCount() {
        assertEquals(5, Iterators.count(Arrays.asList(1, 2, 3, 4, 5).iterator()));
        assertEquals(0, Iterators.count(null));
        assertEquals(0, Iterators.count(new ArrayList<>().iterator()));
    }

    @Test
    public void testCountIterator() {
        assertEquals(0, Iterators.count(null));
        assertEquals(0, Iterators.count(Collections.emptyIterator()));
        assertEquals(3, Iterators.count(list("a", "b", "c").iterator()));
        assertEquals(1, Iterators.count(list("a").iterator()));
    }

    @Test
    public void testCountNoParams() {
        assertEquals(0, Iterators.count(null));

        assertEquals(0, Iterators.count(Collections.emptyIterator()));

        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Iterators.count(list.iterator()));

        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            numbers.add(i);
        }
        assertEquals(100, Iterators.count(numbers.iterator()));
    }

    // ===================== count long overload =====================

    @Test
    public void testCount_LongResult() {
        assertEquals(5L, Iterators.count(Arrays.asList(1, 2, 3, 4, 5).iterator()));
        assertEquals(0L, Iterators.count(Collections.emptyList().iterator()));
    }

    // ===================== count Additional Edge Cases =====================

    @Test
    public void testCount_SingleElement() {
        assertEquals(1, Iterators.count(Arrays.asList("a").iterator()));
    }

    @Test
    public void testCountWithPredicate_NoneMatch() {
        assertEquals(0, Iterators.count(Arrays.asList(1, 3, 5).iterator(), n -> n % 2 == 0));
    }

    @Test
    public void testCountWithPredicate() {
        Predicate<Integer> isEven = n -> n % 2 == 0;
        assertEquals(2, Iterators.count(Arrays.asList(1, 2, 3, 4, 5).iterator(), isEven));
        assertEquals(3, Iterators.count(Arrays.asList(2, 4, 6).iterator(), isEven));
        assertEquals(0, Iterators.count(null, isEven));

        assertThrows(IllegalArgumentException.class, () -> Iterators.count(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testCountIteratorWithPredicate() {
        Predicate<String> isA = "a"::equals;
        assertEquals(0, Iterators.count(null, isA));
        assertEquals(0, Iterators.count(Collections.emptyIterator(), isA));
        assertEquals(2, Iterators.count(list("a", "b", "a", "c").iterator(), isA));
        assertEquals(0, Iterators.count(list("b", "c").iterator(), isA));

        Predicate<Integer> isEven = x -> x % 2 == 0;
        assertEquals(2, Iterators.count(list(1, 2, 3, 4, 5).iterator(), isEven));

        assertThrows(IllegalArgumentException.class, () -> Iterators.count(list("a").iterator(), null));
    }

    @Test
    public void testCountWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.count(testIterator, null));
    }

    @Test
    public void testIndexOf_LastElement() {
        assertEquals(2, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "c"));
    }

    @Test
    public void testIndexOf() {
        assertEquals(2, Iterators.indexOf(Arrays.asList("a", "b", "c", "d").iterator(), "c"));
        assertEquals(0, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "a"));
        assertEquals(-1, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "z"));
        assertEquals(-1, Iterators.indexOf(null, "a"));

        List<String> withNulls = Arrays.asList("a", null, "b", null);
        assertEquals(1, Iterators.indexOf(withNulls.iterator(), null));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        assertEquals(3, Iterators.indexOf(Arrays.asList("a", "b", "c", "a", "d").iterator(), "a", 1));
        assertEquals(-1, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "c", 3));
        assertEquals(2, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "c", 0));
        assertEquals(-1, Iterators.indexOf(null, "a", 0));
    }

    @Test
    public void testIndexOfTwoParams() {
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(null, "test"));

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(Collections.emptyIterator(), "test"));

        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(0, Iterators.indexOf(list.iterator(), "a"));
        assertEquals(1, Iterators.indexOf(list.iterator(), "b"));
        assertEquals(4, Iterators.indexOf(list.iterator(), "d"));

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "e"));

        List<String> listWithNull = Arrays.asList("a", null, "b", null);
        assertEquals(1, Iterators.indexOf(listWithNull.iterator(), null));
    }

    @Test
    public void testIndexOfThreeParams() {
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(null, "test", 0));

        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(1, Iterators.indexOf(list.iterator(), "b", 0));

        assertEquals(3, Iterators.indexOf(list.iterator(), "b", 2));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "a", 1));

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "a", 10));
    }

    // ===================== indexOf with fromIndex =====================

    @Test
    public void testIndexOf_WithFromIndex() {
        assertEquals(3, Iterators.indexOf(Arrays.asList("a", "b", "c", "a").iterator(), "a", 1));
        assertEquals(-1, Iterators.indexOf(Arrays.asList("a", "b").iterator(), "a", 5));
    }

    // ===================== indexOf Additional Edge Cases =====================

    @Test
    public void testIndexOf_EmptyIterator() {
        assertEquals(-1, Iterators.indexOf(Collections.emptyIterator(), "a"));
    }

    @Test
    public void testIndexOfWithFromIndex_AtFromIndex() {
        assertEquals(2, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "c", 2));
    }

    @Test
    public void testIndexOfWithFromIndex_NegativeFromIndex() {
        assertEquals(0, Iterators.indexOf(Arrays.asList("a").iterator(), "a", -1));
    }

    // ===================== equalsInOrder Dedicated Tests =====================

    @Test
    public void testEqualsInOrder() {
        assertTrue(Iterators.equalsInOrder(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 3).iterator()));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 4).iterator()));
    }

    @Test
    public void testEqualsInOrder_DifferentLengths() {
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1, 2).iterator(), Arrays.asList(1, 2, 3).iterator()));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2).iterator()));
    }

    @Test
    public void testElementsEqual() {
        assertTrue(Iterators.equalsInOrder(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 3).iterator()));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 4).iterator()));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1, 2).iterator(), Arrays.asList(1, 2, 3).iterator()));
        assertTrue(Iterators.equalsInOrder(null, null));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testEqualsInOrder_BothNull() {
        assertTrue(Iterators.equalsInOrder(null, null));
    }

    @Test
    public void testEqualsInOrder_OneNull() {
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1).iterator(), null));
        assertFalse(Iterators.equalsInOrder(null, Arrays.asList(1).iterator()));
    }

    @Test
    public void testEqualsInOrder_Empty() {
        assertTrue(Iterators.equalsInOrder(Collections.<Integer> emptyList().iterator(), Collections.<Integer> emptyList().iterator()));
    }

    // ===================== equalsInOrder Additional Edge Cases =====================

    @Test
    public void testEqualsInOrder_SingleElement() {
        assertTrue(Iterators.equalsInOrder(Arrays.asList(1).iterator(), Arrays.asList(1).iterator()));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1).iterator(), Arrays.asList(2).iterator()));
    }

    @Test
    public void testEqualsInOrder_WithNullElements() {
        assertTrue(Iterators.equalsInOrder(Arrays.asList(null, "a").iterator(), Arrays.asList(null, "a").iterator()));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(null, "a").iterator(), Arrays.asList(null, "b").iterator()));
    }

    @Test
    public void testRepeatElement() {
        ObjIterator<String> iter = Iterators.repeat("hello", 3);
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("hello", 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElementLong() {
        ObjIterator<String> iter = Iterators.repeat("hello", 3L);
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());
    }

    // ===================== repeat Additional Edge Cases =====================

    @Test
    public void testRepeatInt_One() {
        ObjIterator<String> iter = Iterators.repeat("x", 1);
        assertTrue(iter.hasNext());
        assertEquals("x", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatLong_One() {
        ObjIterator<String> iter = Iterators.repeat("x", 1L);
        assertTrue(iter.hasNext());
        assertEquals("x", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatInt_NullElement() {
        ObjIterator<String> iter = Iterators.repeat(null, 2);
        assertEquals(null, iter.next());
        assertEquals(null, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_repeat() throws Exception {
        Stream.of(Iterators.repeatElements(CommonUtil.toList(1, 2, 3), 3)).println();
        Stream.of(Iterators.cycle(CommonUtil.toList(1, 2, 3), 3)).println();
        Stream.of(Iterators.repeatElementsToSize(CommonUtil.toList(1, 2, 3), 7)).println();
        Stream.of(Iterators.cycleToSize(CommonUtil.toList(1, 2, 3), 5)).println();

        assertEquals(3, CommonUtil.repeatElementsToSize(CommonUtil.toList(1, 2, 3, 4, 5, 6), 3).size());
        assertEquals(8, CommonUtil.repeatElementsToSize(CommonUtil.toList(1, 2, 3, 4, 5, 6), 8).size());

        assertEquals(3, CommonUtil.cycleToSize(CommonUtil.toList(1, 2, 3, 4, 5, 6), 3).size());
        assertEquals(8, CommonUtil.cycleToSize(CommonUtil.toList(1, 2, 3, 4, 5, 6), 8).size());
    }

    @Test
    public void testRepeatInt() {
        ObjIterator<String> iter = Iterators.repeat("a", 3);
        assertEquals(Arrays.asList("a", "a", "a"), iter.toList());

        iter = Iterators.repeat("x", 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -1));
    }

    @Test
    public void testRepeatLong() {
        ObjIterator<String> iter = Iterators.repeat("b", 4L);
        assertEquals(Arrays.asList("b", "b", "b", "b"), iter.toList());

        iter = Iterators.repeat("y", 0L);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -1L));
    }

    @Test
    public void testRepeatCollection() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(1, 2), 3);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2), iter.toList());

        iter = Iterators.cycle(Arrays.asList(1, 2, 3), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(null, 2);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatCollectionToSize() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2), 5);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), iter.toList());

        iter = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 8);
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1, 2), iter.toList());

        iter = Iterators.cycleToSize(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(null, 5));

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatWithInt() {
        {
            ObjIterator<String> iter = Iterators.repeat("test", 0);
            assertFalse(iter.hasNext());
        }

        {
            ObjIterator<String> iter = Iterators.repeat("hello", 3);
            assertTrue(iter.hasNext());
            assertEquals("hello", iter.next());
            assertEquals("hello", iter.next());
            assertEquals("hello", iter.next());
            assertFalse(iter.hasNext());

            assertThrows(NoSuchElementException.class, () -> iter.next());

            assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1));
        }
    }

    @Test
    public void testRepeatWithLong() {
        ObjIterator<String> iter = Iterators.repeat("test", 0L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("hello", 3L);
        assertTrue(iter.hasNext());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("x", 1000L);
        int count = 0;
        while (iter.hasNext()) {
            assertEquals("x", iter.next());
            count++;
        }
        assertEquals(1000, count);

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1L));
    }

    @Test
    public void testRepeatElementNegative() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("hello", -1));
    }

    // ===================== repeatElements Dedicated Tests =====================

    @Test
    public void testRepeatElements_Dedicated() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2), 3);
        assertEquals(Arrays.asList(1, 1, 1, 2, 2, 2), iter.toList());
    }

    @Test
    public void testRepeatElements_OneRepeat() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2, 3), 1);
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testRepeatElements_Zero() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElements_NullIterable() {
        ObjIterator<Integer> iter = Iterators.repeatElements(null, 2);
        assertFalse(iter.hasNext());
    }

    // ===================== repeatElements Additional Edge Cases =====================

    @Test
    public void testRepeatElements_SingleElement() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(5), 3);
        assertEquals(Arrays.asList(5, 5, 5), iter.toList());
    }

    @Test
    public void testRepeatElements_EmptyCollection() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Collections.emptyList(), 5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElements() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2, 3), 2);
        assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3), iter.toList());

        iter = Iterators.repeatElements(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatElements(null, 2);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElements(Arrays.asList(1), -1));
    }

    // ===================== repeatElementsToSize Dedicated Tests =====================

    @Test
    public void testRepeatElementsToSize_Dedicated() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 7);
        List<Integer> result = iter.toList();
        assertEquals(7, result.size());
    }

    @Test
    public void testRepeatElementsToSize_SmallerThanCollection() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 2);
        List<Integer> result = iter.toList();
        assertEquals(2, result.size());
    }

    // ===================== repeatElementsToSize Additional Edge Cases =====================

    @Test
    public void testRepeatElementsToSize_ExactSize() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 3);
        List<Integer> result = iter.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testRepeatElementsToSize_LargerThanCollection() {
        ObjIterator<Integer> result = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 7);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(7, list.size());
    }

    @Test
    public void testRepeatElementsToSize_TruncatesToSize() {
        ObjIterator<Integer> result = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 2);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(2, list.size());
    }

    @Test
    public void testRepeatElementsToSize_ZeroSize() {
        ObjIterator<Integer> result = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testRepeatElementsToSize() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2), 5);
        assertEquals(Arrays.asList(1, 1, 1, 2, 2), iter.toList());

        iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 7);
        assertEquals(Arrays.asList(1, 1, 1, 2, 2, 3, 3), iter.toList());

        iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(null, 5));

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatElementsToSize_EmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Collections.emptyList(), 5));
    }

    @Test
    public void testRepeatElementsToSize_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(null, 5));
    }

    @Test
    public void testCycleVarargs() {
        ObjIterator<String> iter = Iterators.cycle("a", "b", "c");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c", "a"), result);

        iter = Iterators.cycle();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleVarArgs() {
        ObjIterator<String> iter = Iterators.cycle("a", "b");
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());

        assertFalse(Iterators.cycle().hasNext());
    }

    @Test
    public void testCycleArray() {
        ObjIterator<String> iter = Iterators.cycle();
        assertFalse(iter.hasNext());

        iter = Iterators.cycle("a", "b", "c");
        for (int i = 0; i < 10; i++) {
            assertTrue(iter.hasNext());
            assertEquals("a", iter.next());
            assertEquals("b", iter.next());
            assertEquals("c", iter.next());
        }
    }

    @Test
    public void testCycleWithRounds_OneRound() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(1, 2, 3), 1);
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testCycleIterable() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(1, 2));
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), result);

        iter = Iterators.cycle(Collections.emptyList());
        assertFalse(iter.hasNext());

        iter = Iterators.cycle((Iterable<Integer>) null);
        assertFalse(iter.hasNext());
    }

    // ===================== cycle Additional Edge Cases =====================

    @Test
    public void testCycleVarargs_SingleElement() {
        ObjIterator<String> iter = Iterators.cycle("x");
        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            assertEquals("x", iter.next());
        }
    }

    @Test
    public void testCycleIterable_SingleElement() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(7));
        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            assertEquals(7, iter.next());
        }
    }

    @Test
    public void testCycleWithRounds_EmptyCollection() {
        ObjIterator<Integer> iter = Iterators.cycle(Collections.emptyList(), 3);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_cycle() {
        assertDoesNotThrow(() -> {
            N.println(Iterators.cycle(1, 2, 3).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2, 3)).limit(10).toList());
            N.println(Strings.repeat("#", 80));
            N.println(Iterators.cycle(CommonUtil.toSet(1), 0).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1), 1).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1), 2).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1), 3).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1), 4).limit(10).toList());
            N.println(Strings.repeat("#", 80));
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2), 0).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2), 1).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2), 2).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2), 3).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2), 4).limit(10).toList());
            N.println(Strings.repeat("#", 80));
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2, 3), 0).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2, 3), 1).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2, 3), 2).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2, 3), 3).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.toSet(1, 2, 3), 4).limit(10).toList());
            N.println(Strings.repeat("#", 80));
            N.println(Stream.of(CommonUtil.toSet(1, 2, 3)).cycled(0).limit(10).toList());
            N.println(Stream.of(CommonUtil.toSet(1, 2, 3)).cycled(1).limit(10).toList());
            N.println(Stream.of(CommonUtil.toSet(1, 2, 3)).cycled(2).limit(10).toList());
            N.println(Stream.of(CommonUtil.toSet(1, 2, 3)).cycled(3).limit(10).toList());
            N.println(Stream.of(CommonUtil.toSet(1, 2, 3)).cycled(4).limit(10).toList());
            N.println(Strings.repeat("#", 80));

            N.println(Iterators.cycle(CommonUtil.EMPTY_CHAR_OBJ_ARRAY).limit(10).toList());
            N.println(Iterators.cycle(CommonUtil.emptyList()).limit(10).toList());
        });
    }

    @Test
    public void testCycleWithRounds() {
        ObjIterator<String> iter = Iterators.cycle(Arrays.asList("x", "y"), 3);
        assertEquals(Arrays.asList("x", "y", "x", "y", "x", "y"), iter.toList());

        iter = Iterators.cycle(Arrays.asList("a", "b"), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(null, 3);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(Arrays.asList("a"), -1));
    }

    @Test
    public void testCycleIterableWithRounds() {
        ObjIterator<String> iter = Iterators.cycle(list("a", "b"), 2L);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.cycle(list("a", "b"), 0L).hasNext());
        assertFalse(Iterators.cycle(list(), 2L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(list("a"), -1L));

        ObjIterator<String> iterOneRound = Iterators.cycle(list("a", "b"), 1L);
        assertEquals("a", iterOneRound.next());
        assertEquals("b", iterOneRound.next());
        assertFalse(iterOneRound.hasNext());
    }

    // ===================== cycleToSize Dedicated Tests =====================

    @Test
    public void testCycleToSize() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 7);
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), iter.toList());
    }

    @Test
    public void testCycleToSize_SizeSmallerThanCollection() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2, 3, 4, 5), 3);
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testCycleToSize_LargerThanCollection() {
        ObjIterator<Integer> result = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 7);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(7, list.size());
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), list);
    }

    @Test
    public void testCycleToSize_SmallerThanCollection() {
        ObjIterator<Integer> result = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 2);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(2, list.size());
        assertEquals(Arrays.asList(1, 2), list);
    }

    @Test
    public void testCycleToSize_Zero() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());
    }

    // ===================== cycleToSize Additional Edge Cases =====================

    @Test
    public void testCycleToSize_ExactMultiple() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2), 4);
        assertEquals(Arrays.asList(1, 2, 1, 2), iter.toList());
    }

    @Test
    public void testCycleToSize_SingleElement() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(5), 3);
        assertEquals(Arrays.asList(5, 5, 5), iter.toList());
    }

    @Test
    public void testCycleToSize_ZeroSize() {
        ObjIterator<Integer> result = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testCycleToSize_NullCollection() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(null, 5));
    }

    @Test
    public void testCycleToSize_NegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(Arrays.asList(1), -1));
    }

    @Test
    public void testCycleToSize_EmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(Collections.emptyList(), 5));
    }

    @Test
    public void testCycleToSize_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(null, 5));
    }

    @Test
    public void testConcatBiIterator_NextWithAction() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));
        pairs.add(Pair.of("b", 2));

        BiIterator<String, Integer> iter = createBiIterator(pairs);
        BiIterator<String, Integer> result = Iterators.concat(iter);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        result.next((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        assertEquals(1, keys.size());
        assertEquals("a", keys.get(0));
        assertEquals(1, values.get(0));
    }

    @Test
    public void testConcatBiIterator_ForEachRemaining() {
        List<Pair<String, Integer>> pairs1 = new ArrayList<>();
        pairs1.add(Pair.of("a", 1));
        pairs1.add(Pair.of("b", 2));

        List<Pair<String, Integer>> pairs2 = new ArrayList<>();
        pairs2.add(Pair.of("c", 3));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs1);
        BiIterator<String, Integer> iter2 = createBiIterator(pairs2);
        BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        BiConsumer<String, Integer> action = (k, v) -> {
            keys.add(k);
            values.add(v);
        };

        result.forEachRemaining(action);

        assertEquals(3, keys.size());
        assertEquals(List.of("a", "b", "c"), keys);
        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    public void testConcatBiIterator_Map() {
        List<Pair<String, Integer>> pairs1 = new ArrayList<>();
        pairs1.add(Pair.of("a", 1));
        pairs1.add(Pair.of("b", 2));

        List<Pair<String, Integer>> pairs2 = new ArrayList<>();
        pairs2.add(Pair.of("c", 3));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs1);
        BiIterator<String, Integer> iter2 = createBiIterator(pairs2);
        BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);

        BiFunction<String, Integer, String> mapper = (k, v) -> k + v;
        ObjIterator<String> mapped = result.map(mapper);

        assertTrue(mapped.hasNext());
        assertEquals("a1", mapped.next());
        assertEquals("b2", mapped.next());
        assertEquals("c3", mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testConcatTriIterator_NextWithAction() {
        List<Triple<String, Integer, Double>> triples = new ArrayList<>();
        triples.add(Triple.of("a", 1, 1.1));
        triples.add(Triple.of("b", 2, 2.2));

        TriIterator<String, Integer, Double> iter = createTriIterator(triples);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter);

        List<String> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();
        List<Double> third = new ArrayList<>();

        result.next((a, b, c) -> {
            first.add(a);
            second.add(b);
            third.add(c);
        });

        assertEquals(1, first.size());
        assertEquals("a", first.get(0));
        assertEquals(1, second.get(0));
        assertEquals(1.1, third.get(0));
    }

    @Test
    public void testConcatTriIterator_ForEachRemaining() {
        List<Triple<String, Integer, Double>> triples1 = new ArrayList<>();
        triples1.add(Triple.of("a", 1, 1.1));
        triples1.add(Triple.of("b", 2, 2.2));

        List<Triple<String, Integer, Double>> triples2 = new ArrayList<>();
        triples2.add(Triple.of("c", 3, 3.3));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples1);
        TriIterator<String, Integer, Double> iter2 = createTriIterator(triples2);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, iter2);

        List<String> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();
        List<Double> third = new ArrayList<>();

        TriConsumer<String, Integer, Double> action = (a, b, c) -> {
            first.add(a);
            second.add(b);
            third.add(c);
        };

        result.forEachRemaining(action);

        assertEquals(3, first.size());
        assertEquals(List.of("a", "b", "c"), first);
        assertEquals(List.of(1, 2, 3), second);
        assertEquals(List.of(1.1, 2.2, 3.3), third);
    }

    @Test
    public void testConcatTriIterator_Map() {
        List<Triple<String, Integer, Double>> triples1 = new ArrayList<>();
        triples1.add(Triple.of("a", 1, 1.1));
        triples1.add(Triple.of("b", 2, 2.2));

        List<Triple<String, Integer, Double>> triples2 = new ArrayList<>();
        triples2.add(Triple.of("c", 3, 3.3));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples1);
        TriIterator<String, Integer, Double> iter2 = createTriIterator(triples2);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, iter2);

        TriFunction<String, Integer, Double, String> mapper = (a, b, c) -> a + b + c;
        ObjIterator<String> mapped = result.map(mapper);

        assertTrue(mapped.hasNext());
        assertEquals("a11.1", mapped.next());
        assertEquals("b22.2", mapped.next());
        assertEquals("c33.3", mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testConcatArrays() {
        boolean[] arr1 = { true, false };
        boolean[] arr2 = { true };
        BooleanIterator iter = Iterators.concat(arr1, arr2);

        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCollection() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c", "d").iterator());

        ObjIterator<String> result = Iterators.concat(iterators);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIteratorCollection() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator(), Arrays.asList("d", "e").iterator());

        ObjIterator<String> result = Iterators.concat(iterators);

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), collected);
    }

    @Test
    public void testConcatPrimitiveArrays() {
        char[] charArr1 = { 'a', 'b' };
        char[] charArr2 = { 'c', 'd' };
        CharIterator charIter = Iterators.concat(charArr1, charArr2);
        assertEquals('a', charIter.nextChar());
        assertEquals('b', charIter.nextChar());
        assertEquals('c', charIter.nextChar());
        assertEquals('d', charIter.nextChar());
        assertFalse(charIter.hasNext());

        byte[] byteArr1 = { 1, 2 };
        byte[] byteArr2 = { 3, 4 };
        ByteIterator byteIter = Iterators.concat(byteArr1, byteArr2);
        assertEquals((byte) 1, byteIter.nextByte());
        assertEquals((byte) 2, byteIter.nextByte());
        assertEquals((byte) 3, byteIter.nextByte());
        assertEquals((byte) 4, byteIter.nextByte());
        assertFalse(byteIter.hasNext());

        short[] shortArr1 = { 10, 20 };
        short[] shortArr2 = { 30, 40 };
        ShortIterator shortIter = Iterators.concat(shortArr1, shortArr2);
        assertEquals((short) 10, shortIter.nextShort());
        assertEquals((short) 20, shortIter.nextShort());
        assertEquals((short) 30, shortIter.nextShort());
        assertEquals((short) 40, shortIter.nextShort());
        assertFalse(shortIter.hasNext());

        long[] longArr1 = { 100L, 200L };
        long[] longArr2 = { 300L, 400L };
        LongIterator longIter = Iterators.concat(longArr1, longArr2);
        assertEquals(100L, longIter.nextLong());
        assertEquals(200L, longIter.nextLong());
        assertEquals(300L, longIter.nextLong());
        assertEquals(400L, longIter.nextLong());
        assertFalse(longIter.hasNext());

        float[] floatArr1 = { 1.1f, 2.2f };
        float[] floatArr2 = { 3.3f, 4.4f };
        FloatIterator floatIter = Iterators.concat(floatArr1, floatArr2);
        assertEquals(1.1f, floatIter.nextFloat(), 0.001f);
        assertEquals(2.2f, floatIter.nextFloat(), 0.001f);
        assertEquals(3.3f, floatIter.nextFloat(), 0.001f);
        assertEquals(4.4f, floatIter.nextFloat(), 0.001f);
        assertFalse(floatIter.hasNext());

        double[] doubleArr1 = { 1.11, 2.22 };
        double[] doubleArr2 = { 3.33, 4.44 };
        DoubleIterator doubleIter = Iterators.concat(doubleArr1, doubleArr2);
        assertEquals(1.11, doubleIter.nextDouble(), 0.001);
        assertEquals(2.22, doubleIter.nextDouble(), 0.001);
        assertEquals(3.33, doubleIter.nextDouble(), 0.001);
        assertEquals(4.44, doubleIter.nextDouble(), 0.001);
        assertFalse(doubleIter.hasNext());
    }

    @Test
    public void testTriIteratorConcat() {
        TriIterator<String, Integer, Boolean> tri1 = TriIterator.zip(CommonUtil.toList("a"), CommonUtil.toList(1), CommonUtil.toList(true));
        TriIterator<String, Integer, Boolean> tri2 = TriIterator.zip(CommonUtil.toList("b"), CommonUtil.toList(2), CommonUtil.toList(false));

        TriIterator<String, Integer, Boolean> result = Iterators.concat(tri1, tri2);

        Triple<String, Integer, Boolean> triple1 = result.next();
        assertEquals("a", triple1.left());
        assertEquals(Integer.valueOf(1), triple1.middle());
        assertEquals(true, triple1.right());

        Triple<String, Integer, Boolean> triple2 = result.next();
        assertEquals("b", triple2.left());
        assertEquals(Integer.valueOf(2), triple2.middle());
        assertEquals(false, triple2.right());

        assertFalse(result.hasNext());
    }

    // ===================== concat Map overload =====================

    @Test
    public void testConcatMaps_Dedicated() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("b", 2);

        @SuppressWarnings("unchecked")
        ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(m1, m2);
        List<Map.Entry<String, Integer>> result = iter.toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testConcatBooleanArrays() {
        BooleanIterator iter = Iterators.concat(new boolean[] { true, false }, new boolean[] { true }, new boolean[] { false, false });
        List<Boolean> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextBoolean());
        }
        assertEquals(Arrays.asList(true, false, true, false, false), result);

        iter = Iterators.concat(new boolean[0], new boolean[0]);
        assertFalse(iter.hasNext());

        iter = Iterators.concat((boolean[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCharArrays() {
        CharIterator iter = Iterators.concat(new char[] { 'a', 'b' }, new char[] { 'c' });
        List<Character> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextChar());
        }
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        iter = Iterators.concat((char[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatByteArrays() {
        ByteIterator iter = Iterators.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 });
        List<Byte> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextByte());
        }
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);

        iter = Iterators.concat((byte[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatShortArrays() {
        ShortIterator iter = Iterators.concat(new short[] { 1, 2 }, new short[] { 3 });
        List<Short> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextShort());
        }
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), result);

        iter = Iterators.concat((short[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIntArrays() {
        IntIterator iter = Iterators.concat(new int[] { 1, 2 }, new int[] { 3, 4, 5 });
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextInt());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        iter = Iterators.concat((int[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatLongArrays() {
        LongIterator iter = Iterators.concat(new long[] { 1L, 2L }, new long[] { 3L });
        List<Long> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextLong());
        }
        assertEquals(Arrays.asList(1L, 2L, 3L), result);

        iter = Iterators.concat((long[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatFloatArrays() {
        FloatIterator iter = Iterators.concat(new float[] { 1.0f, 2.0f }, new float[] { 3.0f });
        List<Float> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextFloat());
        }
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), result);

        iter = Iterators.concat((float[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatDoubleArrays() {
        DoubleIterator iter = Iterators.concat(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 });
        List<Double> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextDouble());
        }
        assertEquals(Arrays.asList(1.0, 2.0, 3.0, 4.0), result);

        iter = Iterators.concat((double[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatBooleanIterators() {
        BooleanIterator iter1 = BooleanIterator.of(true, false);
        BooleanIterator iter2 = BooleanIterator.of(true);
        BooleanIterator result = Iterators.concat(iter1, iter2);

        List<Boolean> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextBoolean());
        }
        assertEquals(Arrays.asList(true, false, true), list);

        result = Iterators.concat((BooleanIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatCharIterators() {
        CharIterator iter1 = CharIterator.of('a', 'b');
        CharIterator iter2 = CharIterator.of('c');
        CharIterator result = Iterators.concat(iter1, iter2);

        List<Character> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextChar());
        }
        assertEquals(Arrays.asList('a', 'b', 'c'), list);

        result = Iterators.concat((CharIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatByteIterators() {
        ByteIterator iter1 = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator iter2 = ByteIterator.of((byte) 3);
        ByteIterator result = Iterators.concat(iter1, iter2);

        List<Byte> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextByte());
        }
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);

        result = Iterators.concat((ByteIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatShortIterators() {
        ShortIterator iter1 = ShortIterator.of((short) 1, (short) 2);
        ShortIterator iter2 = ShortIterator.of((short) 3);
        ShortIterator result = Iterators.concat(iter1, iter2);

        List<Short> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextShort());
        }
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);

        result = Iterators.concat((ShortIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIntIterators() {
        IntIterator iter1 = IntIterator.of(1, 2);
        IntIterator iter2 = IntIterator.of(3, 4);
        IntIterator result = Iterators.concat(iter1, iter2);

        List<Integer> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextInt());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4), list);

        result = Iterators.concat((IntIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatLongIterators() {
        LongIterator iter1 = LongIterator.of(1L, 2L);
        LongIterator iter2 = LongIterator.of(3L);
        LongIterator result = Iterators.concat(iter1, iter2);

        List<Long> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextLong());
        }
        assertEquals(Arrays.asList(1L, 2L, 3L), list);

        result = Iterators.concat((LongIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatFloatIterators() {
        FloatIterator iter1 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator iter2 = FloatIterator.of(3.0f);
        FloatIterator result = Iterators.concat(iter1, iter2);

        List<Float> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextFloat());
        }
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), list);

        result = Iterators.concat((FloatIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatDoubleIterators() {
        DoubleIterator iter1 = DoubleIterator.of(1.0, 2.0);
        DoubleIterator iter2 = DoubleIterator.of(3.0);
        DoubleIterator result = Iterators.concat(iter1, iter2);

        List<Double> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextDouble());
        }
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);

        result = Iterators.concat((DoubleIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatObjectArrays() {
        ObjIterator<String> iter = Iterators.concat(new String[] { "a", "b" }, new String[] { "c" }, new String[] { "d", "e" });
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), iter.toList());

        iter = Iterators.concat((String[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterators() {
        ObjIterator<Integer> iter = Iterators.concat(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5).iterator());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), iter.toList());

        iter = Iterators.concat((Iterator<Integer>[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);

        ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(map1, map2);
        assertEquals(3, iter.toList().size());

        iter = Iterators.concat((Map<String, Integer>[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCollectionOfIterators() {
        Collection<Iterator<String>> collection = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator());
        ObjIterator<String> iter = Iterators.concat(collection);
        assertEquals(Arrays.asList("a", "b", "c"), iter.toList());

        iter = Iterators.concat((Collection<Iterator<String>>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatBiIterators() {
        BiIterator<String, Integer> iter1 = createBiIterator(Arrays.asList(Pair.of("a", 1), Pair.of("b", 2)));
        BiIterator<String, Integer> iter2 = createBiIterator(Arrays.asList(Pair.of("c", 3)));

        BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);
        List<Pair<String, Integer>> list = new ArrayList<>();
        result.forEachRemaining((l, r) -> list.add(Pair.of(l, r)));

        assertEquals(3, list.size());
        assertEquals("a", list.get(0).left());
        assertEquals(1, list.get(0).right());

        result = Iterators.concat((BiIterator<String, Integer>[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterators() {
        TriIterator<String, Integer, Boolean> iter1 = createTriIterator(Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false)));
        TriIterator<String, Integer, Boolean> iter2 = createTriIterator(Arrays.asList(Triple.of("c", 3, true)));

        TriIterator<String, Integer, Boolean> result = Iterators.concat(iter1, iter2);
        List<Triple<String, Integer, Boolean>> list = new ArrayList<>();
        result.forEachRemaining((l, m, r) -> list.add(Triple.of(l, m, r)));

        assertEquals(3, list.size());
        assertEquals("a", list.get(0).left());

        result = Iterators.concat((TriIterator<String, Integer, Boolean>[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatBiIterator_SingleIterator() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));
        pairs.add(Pair.of("b", 2));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs);
        BiIterator<String, Integer> result = Iterators.concat(iter1);

        assertTrue(result.hasNext());
        assertEquals(Pair.of("a", 1), result.next());
        assertTrue(result.hasNext());
        assertEquals(Pair.of("b", 2), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatBiIterator_MultipleIterators() {
        List<Pair<String, Integer>> pairs1 = new ArrayList<>();
        pairs1.add(Pair.of("a", 1));
        pairs1.add(Pair.of("b", 2));

        List<Pair<String, Integer>> pairs2 = new ArrayList<>();
        pairs2.add(Pair.of("c", 3));
        pairs2.add(Pair.of("d", 4));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs1);
        BiIterator<String, Integer> iter2 = createBiIterator(pairs2);
        BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);

        assertTrue(result.hasNext());
        assertEquals(Pair.of("a", 1), result.next());
        assertEquals(Pair.of("b", 2), result.next());
        assertEquals(Pair.of("c", 3), result.next());
        assertEquals(Pair.of("d", 4), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatBiIterator_WithEmptyIterators() {
        List<Pair<String, Integer>> pairs1 = new ArrayList<>();
        pairs1.add(Pair.of("a", 1));

        List<Pair<String, Integer>> emptyPairs = new ArrayList<>();

        List<Pair<String, Integer>> pairs2 = new ArrayList<>();
        pairs2.add(Pair.of("b", 2));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs1);
        BiIterator<String, Integer> emptyIter = createBiIterator(emptyPairs);
        BiIterator<String, Integer> iter2 = createBiIterator(pairs2);
        BiIterator<String, Integer> result = Iterators.concat(iter1, emptyIter, iter2);

        assertTrue(result.hasNext());
        assertEquals(Pair.of("a", 1), result.next());
        assertTrue(result.hasNext());
        assertEquals(Pair.of("b", 2), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterator_SingleIterator() {
        List<Triple<String, Integer, Double>> triples = new ArrayList<>();
        triples.add(Triple.of("a", 1, 1.1));
        triples.add(Triple.of("b", 2, 2.2));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1);

        assertTrue(result.hasNext());
        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertTrue(result.hasNext());
        assertEquals(Triple.of("b", 2, 2.2), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterator_MultipleIterators() {
        List<Triple<String, Integer, Double>> triples1 = new ArrayList<>();
        triples1.add(Triple.of("a", 1, 1.1));
        triples1.add(Triple.of("b", 2, 2.2));

        List<Triple<String, Integer, Double>> triples2 = new ArrayList<>();
        triples2.add(Triple.of("c", 3, 3.3));
        triples2.add(Triple.of("d", 4, 4.4));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples1);
        TriIterator<String, Integer, Double> iter2 = createTriIterator(triples2);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, iter2);

        assertTrue(result.hasNext());
        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertEquals(Triple.of("b", 2, 2.2), result.next());
        assertEquals(Triple.of("c", 3, 3.3), result.next());
        assertEquals(Triple.of("d", 4, 4.4), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterator_WithEmptyIterators() {
        List<Triple<String, Integer, Double>> triples1 = new ArrayList<>();
        triples1.add(Triple.of("a", 1, 1.1));

        List<Triple<String, Integer, Double>> emptyTriples = new ArrayList<>();

        List<Triple<String, Integer, Double>> triples2 = new ArrayList<>();
        triples2.add(Triple.of("b", 2, 2.2));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples1);
        TriIterator<String, Integer, Double> emptyIter = createTriIterator(emptyTriples);
        TriIterator<String, Integer, Double> iter2 = createTriIterator(triples2);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, emptyIter, iter2);

        assertTrue(result.hasNext());
        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertTrue(result.hasNext());
        assertEquals(Triple.of("b", 2, 2.2), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatBiIterator_HasNextMultipleCalls() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));

        BiIterator<String, Integer> iter = createBiIterator(pairs);
        BiIterator<String, Integer> result = Iterators.concat(iter);

        assertTrue(result.hasNext());
        assertTrue(result.hasNext());
        assertTrue(result.hasNext());

        assertEquals(Pair.of("a", 1), result.next());
        assertFalse(result.hasNext());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterator_HasNextMultipleCalls() {
        List<Triple<String, Integer, Double>> triples = new ArrayList<>();
        triples.add(Triple.of("a", 1, 1.1));

        TriIterator<String, Integer, Double> iter = createTriIterator(triples);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter);

        assertTrue(result.hasNext());
        assertTrue(result.hasNext());
        assertTrue(result.hasNext());

        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertFalse(result.hasNext());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatPrimitiveIterators() {
        IntIterator intIter1 = IntIterator.of(1, 2);
        IntIterator intIter2 = IntIterator.of(3, 4);
        IntIterator concatenated = Iterators.concat(intIter1, intIter2);
        assertEquals(1, concatenated.nextInt());
        assertEquals(2, concatenated.nextInt());
        assertEquals(3, concatenated.nextInt());
        assertEquals(4, concatenated.nextInt());
        assertFalse(concatenated.hasNext());

        IntIterator empty = IntIterator.empty();
        IntIterator nonEmpty = IntIterator.of(5, 6);
        concatenated = Iterators.concat(empty, nonEmpty);
        assertEquals(5, concatenated.nextInt());
        assertEquals(6, concatenated.nextInt());
        assertFalse(concatenated.hasNext());
    }

    // ===================== concat Additional Edge Cases =====================

    @Test
    public void testConcatObjectArrays_SingleArray() {
        ObjIterator<String> iter = Iterators.concat(new String[] { "a", "b" });
        assertEquals(Arrays.asList("a", "b"), iter.toList());
    }

    @Test
    public void testConcatObjectArrays_EmptyArrays() {
        ObjIterator<String> iter = Iterators.concat(new String[0], new String[0]);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterators_SingleIterator() {
        @SuppressWarnings("unchecked")
        ObjIterator<Integer> iter = Iterators.concat(Arrays.asList(1, 2, 3).iterator());
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testConcatIterators_EmptyIterators() {
        @SuppressWarnings("unchecked")
        ObjIterator<Integer> iter = Iterators.concat(Collections.<Integer> emptyIterator(), Collections.<Integer> emptyIterator());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCollection_EmptyCollection() {
        ObjIterator<String> iter = Iterators.concat(Collections.<Iterator<String>> emptyList());
        assertFalse(iter.hasNext());
    }

    // ===================== concat Primitive Iterator Empty Edge Cases =====================

    @Test
    public void testConcatBooleanIterators_Empty() {
        BooleanIterator result = Iterators.concat(BooleanIterator.empty(), BooleanIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatCharIterators_Empty() {
        CharIterator result = Iterators.concat(CharIterator.empty(), CharIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatByteIterators_Empty() {
        ByteIterator result = Iterators.concat(ByteIterator.empty(), ByteIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatShortIterators_Empty() {
        ShortIterator result = Iterators.concat(ShortIterator.empty(), ShortIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIntIterators_Empty() {
        IntIterator result = Iterators.concat(IntIterator.empty(), IntIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatLongIterators_Empty() {
        LongIterator result = Iterators.concat(LongIterator.empty(), LongIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatFloatIterators_Empty() {
        FloatIterator result = Iterators.concat(FloatIterator.empty(), FloatIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatDoubleIterators_Empty() {
        DoubleIterator result = Iterators.concat(DoubleIterator.empty(), DoubleIterator.empty());
        assertFalse(result.hasNext());
    }

    // ===================== concat Primitive Array Empty Edge Cases =====================

    @Test
    public void testConcatBooleanArrays_Empty() {
        BooleanIterator result = Iterators.concat(new boolean[0], new boolean[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatCharArrays_Empty() {
        CharIterator result = Iterators.concat(new char[0], new char[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatByteArrays_Empty() {
        ByteIterator result = Iterators.concat(new byte[0], new byte[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatShortArrays_Empty() {
        ShortIterator result = Iterators.concat(new short[0], new short[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIntArrays_Empty() {
        IntIterator result = Iterators.concat(new int[0], new int[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatLongArrays_Empty() {
        LongIterator result = Iterators.concat(new long[0], new long[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatFloatArrays_Empty() {
        FloatIterator result = Iterators.concat(new float[0], new float[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatDoubleArrays_Empty() {
        DoubleIterator result = Iterators.concat(new double[0], new double[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatMaps_MultipleMapsCoverage() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);

        @SuppressWarnings("unchecked")
        ObjIterator<Map.Entry<String, Integer>> result = Iterators.concat(map1, map2);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        result.forEachRemaining(entries::add);
        assertEquals(3, entries.size());
    }

    @Test
    public void testConcatMaps_EmptyMapArrayCoverage() {
        @SuppressWarnings("unchecked")
        ObjIterator<Map.Entry<String, Integer>> result = Iterators.concat(new Map[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatMaps_SingleMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 10);

        @SuppressWarnings("unchecked")
        ObjIterator<Map.Entry<String, Integer>> result = Iterators.concat(map);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        result.forEachRemaining(entries::add);
        assertEquals(1, entries.size());
        assertEquals("x", entries.get(0).getKey());
        assertEquals(10, entries.get(0).getValue());
    }

    @Test
    public void testConcatBiIterator_EmptyArray() {
        BiIterator<String, Integer> result = Iterators.concat(new BiIterator[0]);
        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
    }

    @Test
    public void testConcatBiIterator_MapWithEmptyIterators() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));

        List<Pair<String, Integer>> emptyPairs = new ArrayList<>();

        BiIterator<String, Integer> iter1 = createBiIterator(pairs);
        BiIterator<String, Integer> emptyIter = createBiIterator(emptyPairs);
        BiIterator<String, Integer> result = Iterators.concat(iter1, emptyIter);

        BiFunction<String, Integer, String> mapper = (k, v) -> k + v;
        ObjIterator<String> mapped = result.map(mapper);

        assertTrue(mapped.hasNext());
        assertEquals("a1", mapped.next());
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, () -> mapped.next());
    }

    @Test
    public void testConcatTriIterator_EmptyArray() {
        TriIterator<String, Integer, Double> result = Iterators.concat(new TriIterator[0]);
        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
    }

    @Test
    public void testConcatTriIterator_MapWithEmptyIterators() {
        List<Triple<String, Integer, Double>> triples = new ArrayList<>();
        triples.add(Triple.of("a", 1, 1.1));

        List<Triple<String, Integer, Double>> emptyTriples = new ArrayList<>();

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples);
        TriIterator<String, Integer, Double> emptyIter = createTriIterator(emptyTriples);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, emptyIter);

        TriFunction<String, Integer, Double, String> mapper = (a, b, c) -> a + b + c;
        ObjIterator<String> mapped = result.map(mapper);

        assertTrue(mapped.hasNext());
        assertEquals("a11.1", mapped.next());
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, () -> mapped.next());
    }

    @Test
    public void testConcatBiIterator_AllEmptyIterators() {
        BiIterator<String, Integer> empty1 = BiIterator.empty();
        BiIterator<String, Integer> empty2 = BiIterator.empty();
        BiIterator<String, Integer> empty3 = BiIterator.empty();

        BiIterator<String, Integer> result = Iterators.concat(empty1, empty2, empty3);

        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
    }

    @Test
    public void testConcatTriIterator_AllEmptyIterators() {
        TriIterator<String, Integer, Double> empty1 = TriIterator.empty();
        TriIterator<String, Integer, Double> empty2 = TriIterator.empty();
        TriIterator<String, Integer, Double> empty3 = TriIterator.empty();

        TriIterator<String, Integer, Double> result = Iterators.concat(empty1, empty2, empty3);

        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
    }

    @Test
    public void testConcatIterables_02() {
        List<Iterable<String>> iterables = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));

        ObjIterator<String> result = Iterators.concatIterables(iterables);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIterables() {
        ObjIterator<String> iter = Iterators.concat(Arrays.asList("a", "b"), Arrays.asList("c"), Collections.emptyList());
        assertEquals(Arrays.asList("a", "b", "c"), iter.toList());

        iter = Iterators.concat((Iterable<String>[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterablesCollection() {
        Collection<Iterable<Integer>> collection = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
        ObjIterator<Integer> iter = Iterators.concatIterables(collection);
        assertEquals(Arrays.asList(1, 2, 3, 4), iter.toList());

        iter = Iterators.concatIterables(null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterablesEdgeCases() {
        List<Iterable<String>> withNull = new ArrayList<>();
        withNull.add(Arrays.asList("a", "b"));
        withNull.add(null);
        withNull.add(Arrays.asList("c", "d"));

        ObjIterator<String> result = Iterators.concatIterables(withNull);
        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), collected);
    }

    @Test
    public void testConcatIterables_EmptyIterables() {
        @SuppressWarnings("unchecked")
        ObjIterator<String> iter = Iterators.concat(Collections.<String> emptyList(), Collections.<String> emptyList());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterablesCollection_EmptyCollection() {
        ObjIterator<String> iter = Iterators.concatIterables(Collections.<Iterable<String>> emptyList());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeIterators() {
        Iterator<Integer> a = list(1, 3, 5).iterator();
        Iterator<Integer> b = list(2, 4, 6).iterator();
        BiFunction<Integer, Integer, MergeResult> selector = (i1, i2) -> (i1 <= i2) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ObjIterator<Integer> merged = Iterators.merge(a, b, selector);

        assertEquals(1, merged.next());
        assertEquals(2, merged.next());
        assertEquals(3, merged.next());
        assertEquals(4, merged.next());
        assertEquals(5, merged.next());
        assertEquals(6, merged.next());
        assertFalse(merged.hasNext());

        a = list(1, 3).iterator();
        b = list(new Integer[0]).iterator();
        merged = Iterators.merge(a, b, selector);
        assertEquals(1, merged.next());
        assertEquals(3, merged.next());
        assertFalse(merged.hasNext());
    }

    @Test
    public void testMerge() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(iter1, iter2, selector);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    // ===================== merge with Iterable overloads =====================

    @Test
    public void testMergeIterables_Dedicated() {
        List<Integer> a = Arrays.asList(1, 3, 5);
        List<Integer> b = Arrays.asList(2, 4, 6);
        ObjIterator<Integer> iter = Iterators.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), iter.toList());
    }

    @Test
    public void testMergeCollection_TwoIterators() {
        List<Integer> list1 = Arrays.asList(1, 3, 5);
        List<Integer> list2 = Arrays.asList(2, 4, 6);
        List<Iterator<? extends Integer>> iterators = Arrays.asList(list1.iterator(), list2.iterator());

        ObjIterator<Integer> result = Iterators.merge(iterators, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> merged = new ArrayList<>();
        result.forEachRemaining(merged::add);
        assertEquals(6, merged.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), merged);
    }

    @Test
    public void testMergeCollection_ThreeIterators() {
        List<Integer> list1 = Arrays.asList(1, 4);
        List<Integer> list2 = Arrays.asList(2, 5);
        List<Integer> list3 = Arrays.asList(3, 6);
        List<Iterator<? extends Integer>> iterators = Arrays.asList(list1.iterator(), list2.iterator(), list3.iterator());

        ObjIterator<Integer> result = Iterators.merge(iterators, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> merged = new ArrayList<>();
        result.forEachRemaining(merged::add);
        assertEquals(6, merged.size());
    }

    @Test
    public void testMergeTwoIterables() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6), selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.merge((Iterable<Integer>) null, Arrays.asList(1), selector);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeCollectionOfIterators() {
        List<Iterator<Integer>> iterators = list(list(1, 5).iterator(), list(2, 4).iterator(), list(3, 6).iterator());
        BiFunction<Integer, Integer, MergeResult> selector = (i1, i2) -> (i1 <= i2) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ObjIterator<Integer> merged = Iterators.merge(iterators, selector);
        List<Integer> result = new ArrayList<>();
        merged.forEachRemaining(result::add);
        assertEquals(list(1, 2, 3, 4, 5, 6), result);

        assertFalse(Iterators.merge((Collection<Iterator<Integer>>) null, selector).hasNext());
    }

    // ===================== merge Additional Edge Cases =====================

    @Test
    public void testMerge_BothNull() {
        java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;
        ObjIterator<Integer> result = Iterators.merge((Iterator<Integer>) null, (Iterator<Integer>) null, selector);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMerge_FirstEmpty() {
        java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;
        ObjIterator<Integer> result = Iterators.merge(Collections.<Integer> emptyIterator(), Arrays.asList(1, 2).iterator(), selector);
        assertEquals(Arrays.asList(1, 2), result.toList());
    }

    @Test
    public void testMerge_SecondEmpty() {
        java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;
        ObjIterator<Integer> result = Iterators.merge(Arrays.asList(1, 2).iterator(), Collections.<Integer> emptyIterator(), selector);
        assertEquals(Arrays.asList(1, 2), result.toList());
    }

    @Test
    public void testMergeCollection_Empty() {
        ObjIterator<Integer> result = Iterators.merge(Collections.emptyList(), (a, b) -> MergeResult.TAKE_FIRST);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeCollection_SingleIterator() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        List<Iterator<? extends Integer>> iterators = Collections.singletonList(list.iterator());

        ObjIterator<Integer> result = Iterators.merge(iterators, (a, b) -> MergeResult.TAKE_FIRST);
        List<Integer> merged = new ArrayList<>();
        result.forEachRemaining(merged::add);
        assertEquals(Arrays.asList(1, 2, 3), merged);
    }

    @Test
    public void testMergeTwoIterators() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator(), selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.merge((Iterator<Integer>) null, Arrays.asList(1).iterator(), selector);
        assertEquals(Arrays.asList(1), result.toList());

        assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));
    }

    @Test
    public void testMergeCollection() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterator<Integer>> collection = Arrays.asList(Arrays.asList(1, 4).iterator(), Arrays.asList(2, 5).iterator(),
                Arrays.asList(3, 6).iterator());

        ObjIterator<Integer> result = Iterators.merge(collection, selector);
        assertNotNull(result);

        result = Iterators.merge((Collection<Iterator<Integer>>) null, selector);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(Arrays.asList(1).iterator()), null));
    }

    @Test
    public void testMergeIterablesCollection_Dedicated() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6));
        ObjIterator<Integer> iter = Iterators.mergeIterables(iterables, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> result = iter.toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testMergeIterablesCollection() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterable<Integer>> collection = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5));

        ObjIterator<Integer> result = Iterators.mergeIterables(collection, selector);
        assertNotNull(result);

        result = Iterators.mergeIterables(null, selector);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeIterablesEdgeCases() {
        List<Iterable<Integer>> empty = new ArrayList<>();
        ObjIterator<Integer> result = Iterators.mergeIterables(empty, (a, b) -> MergeResult.TAKE_FIRST);
        assertFalse(result.hasNext());

        List<Iterable<Integer>> single = Arrays.asList(Arrays.asList(1, 2, 3));
        result = Iterators.mergeIterables(single, (a, b) -> MergeResult.TAKE_FIRST);
        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeIterables_BothNull() {
        java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;
        ObjIterator<Integer> result = Iterators.merge((Iterable<Integer>) null, (Iterable<Integer>) null, selector);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeIterables() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.mergeIterables(Arrays.asList(Arrays.asList(1)), null));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterable<Integer>> coll = Collections.emptyList();
        ObjIterator<Integer> iter = Iterators.mergeIterables(coll, selector);
        assertFalse(iter.hasNext());

        List<Iterable<Integer>> iterables = new ArrayList<>();
        iterables.add(Arrays.asList(1, 4));
        iterables.add(Arrays.asList(2, 5));
        iterables.add(Arrays.asList(3, 6));

        iter = Iterators.mergeIterables(iterables, selector);
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeSortedIterablesWithComparator() {
        Comparator<Integer> cmp = Integer::compareTo;

        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3), Arrays.asList(2, 4), cmp);
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());
    }

    @Test
    public void testMergeSortedIteratorsComparable() {
        Iterator<Integer> a = list(1, 3, 5, 8).iterator();
        Iterator<Integer> b = list(2, 3, 6, 7).iterator();
        ObjIterator<Integer> merged = Iterators.mergeSorted(a, b);

        assertEquals(1, merged.next());
        assertEquals(2, merged.next());
        assertEquals(3, merged.next());
        assertEquals(3, merged.next());
        assertEquals(5, merged.next());
        assertEquals(6, merged.next());
        assertEquals(7, merged.next());
        assertEquals(8, merged.next());
        assertFalse(merged.hasNext());
    }

    @Test
    public void testMergeSortedIteratorsComparator() {
        Iterator<Integer> a = list(5, 3, 1).iterator();
        Iterator<Integer> b = list(6, 4, 2).iterator();
        ObjIterator<Integer> merged = Iterators.mergeSorted(a, b, Comparator.reverseOrder());

        assertEquals(6, merged.next());
        assertEquals(5, merged.next());
        assertEquals(4, merged.next());
        assertEquals(3, merged.next());
        assertEquals(2, merged.next());
        assertEquals(1, merged.next());
        assertFalse(merged.hasNext());
    }

    @Test
    public void testMergeSorted() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedIterables() {
        Iterable<Integer> iter1 = Arrays.asList(1, 3, 5);
        Iterable<Integer> iter2 = Arrays.asList(2, 4, 6);

        ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    // ===================== mergeSorted with Iterable overloads =====================

    @Test
    public void testMergeSortedIterables_Dedicated() {
        ObjIterator<Integer> iter = Iterators.mergeSorted(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), iter.toList());
    }

    @Test
    public void testMergeSortedIterablesWithComparator_Dedicated() {
        ObjIterator<Integer> iter = Iterators.mergeSorted(Arrays.asList(5, 3, 1), Arrays.asList(6, 4, 2), Comparator.reverseOrder());
        assertEquals(Arrays.asList(6, 5, 4, 3, 2, 1), iter.toList());
    }

    @Test
    public void testMergeSorted_DuplicateElements() {
        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 3).iterator());
        assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3), result.toList());
    }

    @Test
    public void testMergeSortedComparable() {
        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.mergeSorted(Arrays.asList(1).iterator(), (Iterator<Integer>) null);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeSortedWithComparator() {
        Comparator<Integer> cmp = Integer::compareTo;

        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator(), cmp);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.mergeSorted((Iterator<Integer>) null, Arrays.asList(1).iterator(), cmp);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeSortedIterablesComparable() {
        ObjIterator<String> result = Iterators.mergeSorted(Arrays.asList("a", "c", "e"), Arrays.asList("b", "d"));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result.toList());

        result = Iterators.mergeSorted(Arrays.asList("x"), (Iterable<String>) null);
        assertEquals(Arrays.asList("x"), result.toList());
    }

    // ===================== mergeSorted Additional Edge Cases =====================

    @Test
    public void testMergeSorted_BothNull() {
        ObjIterator<Integer> result = Iterators.mergeSorted((Iterator<Integer>) null, (Iterator<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSorted_BothEmpty() {
        ObjIterator<Integer> result = Iterators.mergeSorted(Collections.<Integer> emptyIterator(), Collections.<Integer> emptyIterator());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedIterables_BothNull() {
        ObjIterator<Integer> result = Iterators.mergeSorted((Iterable<Integer>) null, (Iterable<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void test_mergeSorted() throws Exception {
        ObjIterator<Integer> a = ObjIterator.of(1, 3, 5);
        ObjIterator<Integer> b = ObjIterator.of(2, 4, 6);

        Iterators.mergeSorted(a, b, Comparators.NATURAL_ORDER).foreachRemaining(Fn.println());

        N.println("==========================================================================");

        a = ObjIterator.of(1, 3, 5);
        b = ObjIterator.of(2, 4, 6);

        com.google.common.collect.Iterators.mergeSorted(CommonUtil.toList(a, b), Comparators.NATURAL_ORDER).forEachRemaining(Fn.println());
        assertNotNull(b);
    }

    @Test
    public void testMergeSortedIteratorsWithComparator() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.mergeSorted(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));

        Comparator<String> cmp = (a, b) -> a.compareTo(b);
        Iterator<String> iter1 = Arrays.asList("a", "c", "e").iterator();
        Iterator<String> iter2 = Arrays.asList("b", "d", "f").iterator();

        ObjIterator<String> iter = Iterators.mergeSorted(iter1, iter2, cmp);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertEquals("f", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeSortedWithComparator_NullComparator() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.mergeSorted(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));
    }

    @Test
    public void testZipTwoIterables() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), zipFn);
        assertEquals(Arrays.asList("1a", "2b"), result.toList());
    }

    @Test
    public void testZipTwoIteratorsWithDefaultValues() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b", "c").iterator(), 0, "x", zipFn);
        assertEquals(Arrays.asList("1a", "2b", "0c"), result.toList());

        result = Iterators.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a").iterator(), 9, "z", zipFn);
        assertEquals(Arrays.asList("1a", "2z", "3z"), result.toList());
    }

    @Test
    public void testZipTwoIterablesWithDefaultValues() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), 0, "x", zipFn);
        assertEquals(Arrays.asList("1a", "2b", "0c"), result.toList());
    }

    @Test
    public void testZipThreeIterators() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true, false).iterator(),
                zipFn);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), result.toList());

        result = Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true).iterator(), zipFn);
        assertEquals(Arrays.asList("1atrue"), result.toList());
    }

    @Test
    public void testZipThreeIterables() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(true, false), zipFn);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), result.toList());
    }

    @Test
    public void testZipThreeIteratorsWithDefaultValues() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true, false, true).iterator(),
                0, "x", false, zipFn);
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0xtrue"), result.toList());
    }

    @Test
    public void testZipThreeIterablesWithDefaultValues() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1), Arrays.asList("a", "b"), Arrays.asList(true), 0, "x", false, zipFn);
        assertEquals(Arrays.asList("1atrue", "0bfalse"), result.toList());
    }

    @Test
    public void testZipIterators() {
        Iterator<String> s = list("a", "b").iterator();
        Iterator<Integer> i = list(1, 2, 3).iterator();
        ObjIterator<String> zipped = Iterators.zip(s, i, (str, num) -> str + num);

        assertEquals("a1", zipped.next());
        assertEquals("b2", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipIterablesWithDefaults() {
        Iterable<String> s = list("a", "b");
        Iterable<Integer> i = list(1, 2, 3, 4);
        ObjIterator<String> zipped = Iterators.zip(s, i, "defaultS", 0, (str, num) -> str + num);

        assertEquals("a1", zipped.next());
        assertEquals("b2", zipped.next());
        assertEquals("defaultS3", zipped.next());
        assertEquals("defaultS4", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipThreeIterablesWithDefaults() {
        Iterable<String> s = list("a");
        Iterable<Integer> i = list(1, 2);
        Iterable<Boolean> bl = list(true, false, true);

        ObjIterator<String> zipped = Iterators.zip(s, i, bl, "defS", 0, false, (str, num, boolVal) -> str + num + boolVal);
        assertEquals("a1true", zipped.next());
        assertEquals("defS2false", zipped.next());
        assertEquals("defS0true", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipTwoIteratorsWithDefaults() {
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;

        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1).iterator();

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b0", iter.next());
        assertEquals("c0", iter.next());
        assertFalse(iter.hasNext());

        iter1 = Arrays.asList("a").iterator();
        iter2 = Arrays.asList(1, 2, 3).iterator();

        iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("X2", iter.next());
        assertEquals("X3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipTwoIterablesWithDefaults() {
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;

        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1);

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b0", iter.next());
        assertEquals("c0", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() {
        TriFunction<String, Integer, Boolean, String> zipFunc = (a, b, c) -> a + b + c;

        Iterator<String> iter1 = Arrays.asList("a").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false, true).iterator();

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, iter3, "X", 0, false, zipFunc);
        assertEquals("a1true", iter.next());
        assertEquals("X2false", iter.next());
        assertEquals("X0true", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZip() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c3", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipIterables() {
        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1, 2, 3);

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c3", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipWithDefaultValues() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "default", -1, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c-1", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipIterablesWithDefaultValues() {
        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1, 2);

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "default", -1, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c-1", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThreeWithDefaultValues() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true).iterator();

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + b;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, "default", -1, false, zipFunction);

        assertEquals("a1true", result.next());
        assertEquals("b2false", result.next());
        assertEquals("c-1false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThree() {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false).iterator();

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + ":" + b;

        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, zipFunction);

        assertEquals("a1:true", result.next());
        assertEquals("b2:false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipWithDefaults() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "X", 0, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c0", result.next());
        assertFalse(result.hasNext());
    }

    // ===================== zip with Iterable overloads =====================

    @Test
    public void testZipIterables_Dedicated() {
        ObjIterator<String> iter = Iterators.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"), (a, b) -> a + b);
        assertEquals(Arrays.asList("1a", "2b", "3c"), iter.toList());
    }

    @Test
    public void testZipThreeIterables_Dedicated() {
        ObjIterator<String> iter = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(true, false), (a, b, c) -> a + b + c);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), iter.toList());
    }

    @Test
    public void testZipIterablesWithDefaults_Dedicated() {
        ObjIterator<String> iter = Iterators.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b"), 0, "z", (a, b) -> a + b);
        assertEquals(Arrays.asList("1a", "2b", "3z"), iter.toList());
    }

    @Test
    public void testZipThreeIterablesWithDefaults_Dedicated() {
        ObjIterator<String> iter = Iterators.zip(Arrays.asList(1), Arrays.asList("a", "b"), Arrays.asList(true, false, true), 0, "z", false,
                (a, b, c) -> a + b + c);
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0ztrue"), iter.toList());
    }

    @Test
    public void testZipTwoIterators_UnequalLengths() {
        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2, 3, 4).iterator(), Arrays.asList("a", "b").iterator(), (a, b) -> a + b);
        assertEquals(Arrays.asList("1a", "2b"), result.toList());
    }

    // ===================== zip Additional Edge Cases =====================

    @Test
    public void testZipTwoIterators_BothEmpty() {
        ObjIterator<String> result = Iterators.zip(Collections.<Integer> emptyIterator(), Collections.<String> emptyIterator(), (a, b) -> a + b);
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipTwoIterators_NullIterators() {
        ObjIterator<String> result = Iterators.zip((Iterator<Integer>) null, (Iterator<String>) null, (a, b) -> a + "" + b);
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThreeIterators_BothEmpty() {
        ObjIterator<String> result = Iterators.zip(Collections.<Integer> emptyIterator(), Collections.<String> emptyIterator(),
                Collections.<Boolean> emptyIterator(), (a, b, c) -> a + "" + b + c);
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipTwoIteratorsWithDefaults_BothEmpty() {
        ObjIterator<String> result = Iterators.zip(Collections.<Integer> emptyIterator(), Collections.<String> emptyIterator(), 0, "x", (a, b) -> a + b);
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipTwoIterators() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a", "b", "c").iterator(), zipFn);
        assertEquals(Arrays.asList("1a", "2b", "3c"), result.toList());

        result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b", "c").iterator(), zipFn);
        assertEquals(Arrays.asList("1a", "2b"), result.toList());

        assertThrows(IllegalArgumentException.class, () -> Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a").iterator(), null));
    }

    @Test
    public void testUnzipIterable() {
        BiConsumer<String, Pair<String, Integer>> unzipFn = (str, pair) -> {
            pair.setLeft(str.substring(0, 1));
            pair.setRight(Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> result = Iterators.unzip(Arrays.asList("x1", "y2"), unzipFn);

        List<Pair<String, Integer>> list = new ArrayList<>();
        result.forEachRemaining((l, r) -> list.add(Pair.of(l, r)));

        assertEquals(2, list.size());
    }

    @Test
    public void testUnzip() {
        Iterator<String> iter = Arrays.asList("a1", "b2", "c3").iterator();

        BiConsumer<String, Pair<String, Integer>> unzip = (s, pair) -> {
            pair.setLeft(s.substring(0, 1));
            pair.setRight(Integer.parseInt(s.substring(1)));
        };

        BiIterator<String, Integer> result = Iterators.unzip(iter, unzip);

        Pair<String, Integer> pair1 = result.next();
        assertEquals("a", pair1.left());
        assertEquals(Integer.valueOf(1), pair1.right());

        Pair<String, Integer> pair2 = result.next();
        assertEquals("b", pair2.left());
        assertEquals(Integer.valueOf(2), pair2.right());

        assertTrue(result.hasNext());
    }

    // ===================== unzip with Iterable overloads =====================

    @Test
    public void testUnzipIterable_Dedicated() {
        com.landawn.abacus.util.function.BiConsumer<String, Pair<String, Integer>> unzipFn = (str, pair) -> {
            pair.setLeft(str.substring(0, 1));
            pair.setRight(Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> result = Iterators.unzip(Arrays.asList("a1", "b2"), unzipFn);
        assertTrue(result.hasNext());
    }

    @Test
    public void testUnzipIteratorToBiIterator() {
        Iterator<String> source = list("a:1", "b:2").iterator();
        BiConsumer<String, Pair<String, Integer>> unzipper = (str, pair) -> {
            String[] parts = str.split(":");
            pair.setLeft(parts[0]);
            pair.setRight(Integer.parseInt(parts[1]));
        };

        assertNotNull(Iterators.unzip(source, unzipper));
    }

    // ===================== unzip Additional Edge Cases =====================

    @Test
    public void testUnzip_EmptyIterator() {
        com.landawn.abacus.util.function.BiConsumer<String, Pair<String, Integer>> unzipFn = (str, pair) -> {
            pair.setLeft(str);
            pair.setRight(0);
        };
        BiIterator<String, Integer> result = Iterators.unzip(Collections.<String> emptyIterator(), unzipFn);
        assertFalse(result.hasNext());
    }

    @Test
    public void testUnzip_NullIterator() {
        com.landawn.abacus.util.function.BiConsumer<String, Pair<String, Integer>> unzipFn = (str, pair) -> {
            pair.setLeft(str);
            pair.setRight(0);
        };
        BiIterator<String, Integer> result = Iterators.unzip((Iterator<String>) null, unzipFn);
        assertFalse(result.hasNext());
    }

    @Test
    public void testUnzipIterator() {
        BiConsumer<String, Pair<String, Integer>> unzipFn = (str, pair) -> {
            pair.setLeft(str.substring(0, 1));
            pair.setRight(Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> result = Iterators.unzip(Arrays.asList("a1", "b2", "c3").iterator(), unzipFn);

        List<Pair<String, Integer>> list = new ArrayList<>();
        result.forEachRemaining((l, r) -> list.add(Pair.of(l, r)));

        assertEquals(3, list.size());
        assertEquals("a", list.get(0).left());
        assertEquals(1, list.get(0).right());

        assertThrows(IllegalArgumentException.class, () -> Iterators.unzip(Arrays.asList("a1").iterator(), null));
    }

    @Test
    public void testUnzip_NullUnzipFunction() {
        assertThrows(IllegalArgumentException.class,
                () -> Iterators.unzip(Arrays.asList("a").iterator(), (com.landawn.abacus.util.function.BiConsumer<String, Pair<String, Integer>>) null));
    }

    @Test
    public void testUnzippIterator() {
        BiConsumer<String, Triple<String, Integer, Boolean>> unzipFn = (str, triple) -> {
            triple.setLeft(str.substring(0, 1));
            triple.setMiddle(Integer.parseInt(str.substring(1, 2)));
            triple.setRight(str.charAt(2) == 't');
        };

        TriIterator<String, Integer, Boolean> result = Iterators.unzip3(Arrays.asList("a1t", "b2f").iterator(), unzipFn);

        List<Triple<String, Integer, Boolean>> list = new ArrayList<>();
        result.forEachRemaining((l, m, r) -> list.add(Triple.of(l, m, r)));

        assertEquals(2, list.size());
        assertEquals("a", list.get(0).left());
        assertEquals(1, list.get(0).middle());
        assertTrue(list.get(0).right());
    }

    @Test
    public void testUnzippIterable() {
        BiConsumer<String, Triple<String, Integer, Boolean>> unzipFn = (str, triple) -> {
            triple.setLeft(str.substring(0, 1));
            triple.setMiddle(Integer.parseInt(str.substring(1, 2)));
            triple.setRight(str.charAt(2) == 't');
        };

        TriIterator<String, Integer, Boolean> result = Iterators.unzip3(Arrays.asList("x1t"), unzipFn);

        assertTrue(result.hasNext());
    }

    // ===================== unzip3 Dedicated Tests =====================

    @Test
    public void testUnzip3_Iterator() {
        com.landawn.abacus.util.function.BiConsumer<String, Triple<String, Integer, Boolean>> unzipFn = (str, triple) -> {
            triple.setLeft(str.substring(0, 1));
            triple.setMiddle(Integer.parseInt(str.substring(1, 2)));
            triple.setRight(str.charAt(2) == 't');
        };

        TriIterator<String, Integer, Boolean> result = Iterators.unzip3(Arrays.asList("a1t", "b2f").iterator(), unzipFn);
        assertTrue(result.hasNext());

        List<Triple<String, Integer, Boolean>> list = new ArrayList<>();
        result.forEachRemaining((l, m, r) -> list.add(Triple.of(l, m, r)));

        assertEquals(2, list.size());
        assertEquals("a", list.get(0).left());
        assertEquals(1, list.get(0).middle());
        assertTrue(list.get(0).right());
        assertEquals("b", list.get(1).left());
        assertEquals(2, list.get(1).middle());
        assertFalse(list.get(1).right());
    }

    @Test
    public void testUnzip3_Iterable() {
        com.landawn.abacus.util.function.BiConsumer<String, Triple<String, Integer, Boolean>> unzipFn = (str, triple) -> {
            triple.setLeft(str.substring(0, 1));
            triple.setMiddle(Integer.parseInt(str.substring(1, 2)));
            triple.setRight(str.charAt(2) == 't');
        };

        TriIterator<String, Integer, Boolean> result = Iterators.unzip3(Arrays.asList("x1t"), unzipFn);
        assertTrue(result.hasNext());
    }

    @Test
    public void testUnzip3_EmptyIterator() {
        com.landawn.abacus.util.function.BiConsumer<String, Triple<String, Integer, Boolean>> unzipFn = (str, triple) -> {
            triple.setLeft(str);
            triple.setMiddle(0);
            triple.setRight(false);
        };
        TriIterator<String, Integer, Boolean> result = Iterators.unzip3(Collections.<String> emptyIterator(), unzipFn);
        assertFalse(result.hasNext());
    }

    @Test
    public void testUnzip3_NullIterator() {
        com.landawn.abacus.util.function.BiConsumer<String, Triple<String, Integer, Boolean>> unzipFn = (str, triple) -> {
            triple.setLeft(str);
            triple.setMiddle(0);
            triple.setRight(false);
        };
        TriIterator<String, Integer, Boolean> result = Iterators.unzip3((Iterator<String>) null, unzipFn);
        assertFalse(result.hasNext());
    }

    @Test
    public void testUnzip3_NullUnzipFunction() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.unzip3(Arrays.asList("a").iterator(),
                (com.landawn.abacus.util.function.BiConsumer<String, Triple<String, Integer, Boolean>>) null));
    }

    @Test
    public void testAdvanceMoreThanAvailable() {
        Iterator<Integer> iter = testList.iterator();
        long advanced = Iterators.advance(iter, 10);

        assertEquals(5, advanced);
        assertFalse(iter.hasNext());
    }

    // ===================== advance Additional Edge Cases =====================

    @Test
    public void testAdvance_ExactSize() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();
        assertEquals(3, Iterators.advance(iter, 3));
        assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        Iterator<Integer> iter = testList.iterator();
        long advanced = Iterators.advance(iter, 0);

        assertEquals(0, advanced);
        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
    }

    @Test
    public void testAdvanceEmptyIterator() {
        Iterator<Integer> iter = emptyList.iterator();
        long advanced = Iterators.advance(iter, 5);

        assertEquals(0, advanced);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvance_NullIterator() {
        assertEquals(0, Iterators.advance(null, 10));
    }

    @Test
    public void testAdvance() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        assertEquals(3, Iterators.advance(iter, 3));
        assertTrue(iter.hasNext());
        assertEquals(4, iter.next());

        iter = Arrays.asList(1, 2).iterator();
        assertEquals(2, Iterators.advance(iter, 5));
        assertFalse(iter.hasNext());

        assertEquals(0, Iterators.advance(null, 5));

        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testAdvanceNegativeThrowsException() {
        Iterator<Integer> iter = testList.iterator();
        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(iter, -1));
    }

    @Test
    public void testAdvanceNegative() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(testIterator, -1));
    }

    @Test
    public void testSkipMoreThanSize() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> skipped = Iterators.skip(iter, 10);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testLazyEvaluationSkip() {
        AtomicInteger callCount = new AtomicInteger(0);
        Iterator<Integer> countingIterator = new Iterator<>() {
            private int current = 1;

            @Override
            public boolean hasNext() {
                return current <= 5;
            }

            @Override
            public Integer next() {
                callCount.incrementAndGet();
                return current++;
            }
        };

        ObjIterator<Integer> skipped = Iterators.skip(countingIterator, 2);
        assertEquals(0, callCount.get());

        skipped.next();
        assertEquals(3, callCount.get());
    }

    @Test
    public void testChainedOperations() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        ObjIterator<Integer> iter1 = Iterators.skip(numbers.iterator(), 2);
        ObjIterator<Integer> iter2 = Iterators.filter(iter1, n -> n % 2 == 0);
        ObjIterator<String> iter3 = Iterators.map(iter2, Object::toString);
        ObjIterator<String> iter4 = Iterators.limit(iter3, 3);

        List<String> result = new ArrayList<>();
        while (iter4.hasNext()) {
            result.add(iter4.next());
        }
        assertEquals(Arrays.asList("4", "6", "8"), result);
    }

    // ===================== skip Additional Edge Cases =====================

    @Test
    public void testSkip_ExactSize() {
        ObjIterator<Integer> result = Iterators.skip(Arrays.asList(1, 2, 3).iterator(), 3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testChainedOperations_SkipThenLimitThenFilter() {
        ObjIterator<Integer> skipped = Iterators.skip(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).iterator(), 2);
        ObjIterator<Integer> limited = Iterators.limit(skipped, 5);
        ObjIterator<Integer> filtered = Iterators.filter(limited, n -> n % 2 == 0);
        assertEquals(Arrays.asList(4, 6), filtered.toList());
    }

    @Test
    public void testSkipZero() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> skipped = Iterators.skip(iter, 0);

        List<Integer> result = new ArrayList<>();
        while (skipped.hasNext()) {
            result.add(skipped.next());
        }

        assertEquals(testList, result);
    }

    @Test
    public void testSkipNullIterator() {
        ObjIterator<Integer> skipped = Iterators.skip(null, 2);
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testNullIteratorHandling() {
        assertFalse(Iterators.skip(null, 5).hasNext());
        assertFalse(Iterators.limit(null, 5).hasNext());
        assertFalse(Iterators.filter((Iterator<String>) null, n -> true).hasNext());
        assertFalse(Iterators.distinct((Iterator<String>) null).hasNext());
        assertFalse(Iterators.map((Iterator<String>) null, Function.identity()).hasNext());
    }

    @Test
    public void testEmptyIteratorHandling() {
        Iterator<String> empty = new ArrayList<String>().iterator();

        assertFalse(Iterators.skip(empty, 5).hasNext());
        assertFalse(Iterators.limit(empty, 5).hasNext());
        assertFalse(Iterators.filter(empty, n -> true).hasNext());
        assertFalse(Iterators.distinct(empty).hasNext());
    }

    @Test
    public void testSkip_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.skip(Collections.<Integer> emptyIterator(), 5);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkip() {
        ObjIterator<Integer> result = Iterators.skip(Arrays.asList(1, 2, 3, 4, 5).iterator(), 2);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());

        result = Iterators.skip(Arrays.asList(1, 2).iterator(), 5);
        assertFalse(result.hasNext());

        result = Iterators.skip((Iterator<Integer>) null, 2);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testSkipNegativeThrowsException() {
        Iterator<Integer> iter = testList.iterator();
        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(iter, -1));
    }

    @Test
    public void testLimitMoreThanSize() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> limited = Iterators.limit(iter, 10);

        List<Integer> result = new ArrayList<>();
        while (limited.hasNext()) {
            result.add(limited.next());
        }

        assertEquals(testList, result);
    }

    // ===================== limit Additional Edge Cases =====================

    @Test
    public void testLimit_ExactSize() {
        ObjIterator<Integer> result = Iterators.limit(Arrays.asList(1, 2, 3).iterator(), 3);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testLimit_Iterator() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        ObjIterator<Integer> result = Iterators.limit(iter, 3);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testLimit_Iterator_ExceedsSize() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();
        ObjIterator<Integer> result = Iterators.limit(iter, 100);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testLimitZero() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> limited = Iterators.limit(iter, 0);

        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitNullIterator() {
        ObjIterator<Integer> limited = Iterators.limit(null, 3);
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimit_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.limit(Collections.<Integer> emptyIterator(), 5);
        assertFalse(result.hasNext());
    }

    @Test
    public void testLimit_Iterator_Zero() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();
        ObjIterator<Integer> result = Iterators.limit(iter, 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testLimit() {
        ObjIterator<Integer> result = Iterators.limit(Arrays.asList(1, 2, 3, 4, 5).iterator(), 3);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());

        result = Iterators.limit(Arrays.asList(1, 2).iterator(), 5);
        assertEquals(Arrays.asList(1, 2), result.toList());

        result = Iterators.limit((Iterator<Integer>) null, 3);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.limit(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testLimitNegativeThrowsException() {
        Iterator<Integer> iter = testList.iterator();
        assertThrows(IllegalArgumentException.class, () -> Iterators.limit(iter, -1));
    }

    @Test
    public void testNoSuchElementException() {
        ObjIterator<String> iter = Iterators.limit(Arrays.asList("a").iterator(), 1);
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());

        ObjIterator<Integer> iter2 = Iterators.filter(Arrays.asList(1, 2, 3).iterator(), n -> n > 10);
        assertThrows(NoSuchElementException.class, () -> iter2.next());
    }

    @Test
    public void testSkipAndLimit() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 1, 3);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(2, 3, 4), resultList);
    }

    // ===================== skipAndLimit with Iterable overload =====================

    @Test
    public void testSkipAndLimitIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.skipAndLimit(Arrays.asList(1, 2, 3, 4, 5), 1, 3);
        assertEquals(Arrays.asList(2, 3, 4), iter.toList());
    }

    // ===================== skipAndLimit Additional Edge Cases =====================

    @Test
    public void testSkipAndLimit_OffsetBeyondSize() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3).iterator(), 10, 5);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimit_Iterator() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 2, 3);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(Arrays.asList(3, 4, 5), list);
    }

    @Test
    public void testSkipAndLimitIterator() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3);
        assertEquals(Arrays.asList(2, 3, 4), result.toList());

        result = Iterators.skipAndLimit(Arrays.asList(1, 2).iterator(), 0, 1);
        assertEquals(Arrays.asList(1), result.toList());

        result = Iterators.skipAndLimit((Iterator<Integer>) null, 1, 2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimitIterable() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3, 4, 5), 2, 2);
        assertEquals(Arrays.asList(3, 4), result.toList());

        result = Iterators.skipAndLimit((Iterable<Integer>) null, 1, 2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimitZeroOffset() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 0, 3);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2, 3), resultList);
    }

    @Test
    public void testSkipAndLimitMaxCount() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 2, Long.MAX_VALUE);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(3, 4, 5), resultList);
    }

    @Test
    public void testSkipAndLimit_ZeroCount() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3).iterator(), 0, 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimitIterable_NullIterable() {
        ObjIterator<Integer> result = Iterators.skipAndLimit((Iterable<Integer>) null, 0, 5);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimit_ZeroOffset() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 0, 3);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testSkipAndLimit_ZeroCountResult() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 2, 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNullsIterable() {
        ObjIterator<String> result = Iterators.skipNulls(Arrays.asList("a", null, "b", null, "c"));
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());

        result = Iterators.skipNulls(Arrays.asList(null, null));
        assertFalse(result.hasNext());

        result = Iterators.skipNulls((Iterable<String>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNullsIterator() {
        ObjIterator<String> result = Iterators.skipNulls(Arrays.asList("a", null, "b", null, "c").iterator());
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());

        result = Iterators.skipNulls((Iterator<String>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNullsAllNulls() {
        List<String> allNulls = Arrays.asList(null, null, null);
        ObjIterator<String> result = Iterators.skipNulls(allNulls);

        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNulls() {
        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");
        ObjIterator<String> iter = Iterators.skipNulls(withNulls.iterator());

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.skipNulls(withNulls);
        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    // ===================== skipNulls with Iterable overload =====================

    @Test
    public void testSkipNullsIterable_Dedicated() {
        ObjIterator<String> iter = Iterators.skipNulls(Arrays.asList("a", null, "b", null, "c"));
        assertEquals(Arrays.asList("a", "b", "c"), iter.toList());
    }

    // ===================== skipNulls Additional Edge Cases =====================

    @Test
    public void testSkipNulls_NoNulls() {
        ObjIterator<String> result = Iterators.skipNulls(Arrays.asList("a", "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());
    }

    @Test
    public void testSkipNulls_EmptyIterable() {
        ObjIterator<String> result = Iterators.skipNulls(Collections.<String> emptyList());
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNulls_EmptyIterator() {
        ObjIterator<String> result = Iterators.skipNulls(Collections.<String> emptyIterator());
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinct() {
        List<Integer> withDuplicates = Arrays.asList(1, 2, 3, 2, 4, 3, 5);
        ObjIterator<Integer> iter = Iterators.distinct(withDuplicates.iterator());

        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        iter = Iterators.distinct(withDuplicates);
        result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    // ===================== distinct with Iterable overload =====================

    @Test
    public void testDistinctIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.distinct(Arrays.asList(1, 2, 2, 3, 3, 3));
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testDistinct_AlreadyDistinct() {
        ObjIterator<Integer> result = Iterators.distinct(Arrays.asList(1, 2, 3).iterator());
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testDistinctIterable() {
        ObjIterator<Integer> result = Iterators.distinct(Arrays.asList(1, 2, 2, 3, 1, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        result = Iterators.distinct(Collections.emptyList());
        assertFalse(result.hasNext());

        result = Iterators.distinct((Iterable<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctIterator() {
        ObjIterator<Integer> result = Iterators.distinct(Arrays.asList(1, 2, 2, 3, 1, 4).iterator());
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        result = Iterators.distinct((Iterator<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctNullIterator() {
        ObjIterator<Integer> result = Iterators.distinct((Iterator<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctNullIterable() {
        ObjIterator<Integer> result = Iterators.distinct((Iterable<Integer>) null);
        assertFalse(result.hasNext());
    }

    // ===================== distinct Additional Edge Cases =====================

    @Test
    public void testDistinct_AllSame() {
        ObjIterator<Integer> result = Iterators.distinct(Arrays.asList(1, 1, 1, 1).iterator());
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testDistinct_WithNulls() {
        ObjIterator<String> result = Iterators.distinct(Arrays.asList("a", null, "a", null, "b").iterator());
        List<String> list = result.toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testDistinct_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.distinct(Collections.<Integer> emptyIterator());
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctBy() {
        List<String> words = Arrays.asList("apple", "apricot", "banana", "berry", "cherry");
        Function<String, Character> firstLetter = s -> s.charAt(0);

        ObjIterator<String> iter = Iterators.distinctBy(words.iterator(), firstLetter);

        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    // ===================== distinctBy with Iterable overload =====================

    @Test
    public void testDistinctByIterable_Dedicated() {
        ObjIterator<String> iter = Iterators.distinctBy(Arrays.asList("abc", "aXX", "bYY", "bZZ"), s -> s.charAt(0));
        assertEquals(Arrays.asList("abc", "bYY"), iter.toList());
    }

    // ===================== distinctBy Additional Edge Cases =====================

    @Test
    public void testDistinctBy_AllSameKey() {
        ObjIterator<String> result = Iterators.distinctBy(Arrays.asList("a1", "a2", "a3").iterator(), s -> s.charAt(0));
        List<String> list = result.toList();
        assertEquals(1, list.size());
        assertEquals("a1", list.get(0));
    }

    @Test
    public void testDistinctBy_EmptyIterator() {
        ObjIterator<String> result = Iterators.distinctBy(Collections.<String> emptyIterator(), s -> s.charAt(0));
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctByIterable() {
        ObjIterator<String> result = Iterators.distinctBy(Arrays.asList("a", "ab", "b", "abc", "c"), String::length);
        List<String> list = result.toList();
        assertEquals(3, list.size());

        result = Iterators.distinctBy((Iterable<String>) null, String::length);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a"), null));
    }

    @Test
    public void testDistinctByIterator() {
        ObjIterator<String> result = Iterators.distinctBy(Arrays.asList("a", "ab", "b", "abc").iterator(), String::length);
        List<String> list = result.toList();
        assertEquals(3, list.size());

        result = Iterators.distinctBy((Iterator<String>) null, String::length);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a").iterator(), null));
    }

    @Test
    public void testDistinctByNullKeyExtractor() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(testList, null));
    }

    @Test
    public void testFilterLazyEvaluation() {
        AtomicInteger callCount = new AtomicInteger(0);
        Iterator<Integer> countingIterator = new Iterator<>() {
            private int current = 1;

            @Override
            public boolean hasNext() {
                return current <= 5;
            }

            @Override
            public Integer next() {
                callCount.incrementAndGet();
                return current++;
            }
        };

        ObjIterator<Integer> filtered = Iterators.filter(countingIterator, x -> x % 2 == 0);
        assertEquals(0, callCount.get());

        filtered.next();
        assertTrue(callCount.get() >= 2);
    }

    @Test
    public void testFilter() {
        Predicate<Integer> isEven = n -> n % 2 == 0;
        ObjIterator<Integer> iter = Iterators.filter(intList.iterator(), isEven);

        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(4), iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.filter(intList, isEven);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(2, 4), result);
    }

    // ===================== filter with Iterable overload =====================

    @Test
    public void testFilterIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.filter(Arrays.asList(1, 2, 3, 4, 5), n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4), iter.toList());
    }

    // ===================== filter Additional Edge Cases =====================

    @Test
    public void testFilter_AllMatch() {
        ObjIterator<Integer> result = Iterators.filter(Arrays.asList(2, 4, 6).iterator(), n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4, 6), result.toList());
    }

    @Test
    public void testFilterNullIterable() {
        Predicate<Integer> predicate = x -> true;
        ObjIterator<Integer> result = Iterators.filter((Iterable<Integer>) null, predicate);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFilterNullIterator() {
        Predicate<Integer> predicate = x -> true;
        ObjIterator<Integer> result = Iterators.filter((Iterator<Integer>) null, predicate);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFilter_NoneMatch() {
        ObjIterator<Integer> result = Iterators.filter(Arrays.asList(1, 3, 5).iterator(), n -> n % 2 == 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFilter_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.filter(Collections.<Integer> emptyIterator(), n -> true);
        assertFalse(result.hasNext());
    }

    @Test
    public void test_filter_map() throws Exception {

        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.map(iter, it -> it + "1").forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.flatMap(iter, it -> CommonUtil.toList(it + "1", it + "2")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.flatmap(iter, it -> CommonUtil.asArray(it + "1", it + "2")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
    }

    @Test
    public void test_filter_map_2() throws Exception {

        {
            final List<String> list = CommonUtil.toList("a", "b", "c");
            N.map(list, it -> it + "1").forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.toList("a", "b", "c");
            N.flatMap(list, it -> CommonUtil.toList(it + "1", it + "2")).forEach(Fn.println());
            N.println("==========================================================================");
        }
    }

    @Test
    public void test_filter() throws Exception {
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.filter(iter, it -> !it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.takeWhile(iter, it -> !it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.takeWhileInclusive(iter, it -> !it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.dropWhile(iter, it -> !it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.skipUntil(iter, it -> it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
    }

    @Test
    public void test_filter_2() throws Exception {
        {
            final List<String> list = CommonUtil.toList("a", "b", "c");
            N.filter(list, it -> !it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.toList("a", "b", "c");
            N.takeWhile(list, it -> !it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.toList("a", "b", "c");
            N.takeWhileInclusive(list, it -> !it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.toList("a", "b", "c");
            N.dropWhile(list, it -> !it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.toList("a", "b", "c");
            N.skipUntil(list, it -> it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
    }

    @Test
    public void testFilterIterable() {
        Predicate<Integer> isEven = n -> n % 2 == 0;

        ObjIterator<Integer> result = Iterators.filter(Arrays.asList(1, 2, 3, 4, 5, 6), isEven);
        assertEquals(Arrays.asList(2, 4, 6), result.toList());

        result = Iterators.filter((Iterable<Integer>) null, isEven);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.filter(Arrays.asList(1), null));
    }

    @Test
    public void testFilterIterator() {
        Predicate<Integer> isOdd = n -> n % 2 != 0;

        ObjIterator<Integer> result = Iterators.filter(Arrays.asList(1, 2, 3, 4, 5).iterator(), isOdd);
        assertEquals(Arrays.asList(1, 3, 5), result.toList());

        result = Iterators.filter((Iterator<Integer>) null, isOdd);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.filter(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testFilter_NullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.filter(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testTakeWhile() {
        Predicate<Integer> lessThan4 = n -> n < 4;
        ObjIterator<Integer> iter = Iterators.takeWhile(intList.iterator(), lessThan4);

        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.takeWhile(intList, lessThan4);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // ===================== takeWhile with Iterable overload =====================

    @Test
    public void testTakeWhileIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.takeWhile(Arrays.asList(1, 2, 3, 4, 5), n -> n < 4);
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    // ===================== takeWhile Additional Edge Cases =====================

    @Test
    public void testTakeWhile_AllMatch() {
        ObjIterator<Integer> result = Iterators.takeWhile(Arrays.asList(1, 2, 3).iterator(), n -> n < 10);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testTakeWhileIterator() {
        Predicate<Integer> lessThan3 = n -> n < 3;

        ObjIterator<Integer> result = Iterators.takeWhile(Arrays.asList(1, 2, 3, 4).iterator(), lessThan3);
        assertEquals(Arrays.asList(1, 2), result.toList());

        result = Iterators.takeWhile((Iterator<Integer>) null, lessThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTakeWhileNone() {
        Predicate<Integer> greaterThanTen = x -> x > 10;
        ObjIterator<Integer> result = Iterators.takeWhile(testList, greaterThanTen);

        assertFalse(result.hasNext());
    }

    @Test
    public void testTakeWhile_NoneMatch() {
        ObjIterator<Integer> result = Iterators.takeWhile(Arrays.asList(5, 6, 7).iterator(), n -> n < 1);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTakeWhile_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.takeWhile(Collections.<Integer> emptyIterator(), n -> true);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTakeWhileIterable() {
        Predicate<Integer> lessThan4 = n -> n < 4;

        ObjIterator<Integer> result = Iterators.takeWhile(Arrays.asList(1, 2, 3, 4, 5), lessThan4);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());

        result = Iterators.takeWhile(Arrays.asList(5, 6, 7), lessThan4);
        assertFalse(result.hasNext());

        result = Iterators.takeWhile((Iterable<Integer>) null, lessThan4);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.takeWhile(Arrays.asList(1), null));
    }

    @Test
    public void testTakeWhileInclusive() {
        Predicate<Integer> lessThan4 = n -> n < 4;
        ObjIterator<Integer> iter = Iterators.takeWhileInclusive(intList.iterator(), lessThan4);

        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(4), iter.next());
        assertFalse(iter.hasNext());
    }

    // ===================== takeWhileInclusive with Iterable overload =====================

    @Test
    public void testTakeWhileInclusiveIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.takeWhileInclusive(Arrays.asList(1, 2, 3, 4, 5), n -> n < 4);
        assertEquals(Arrays.asList(1, 2, 3, 4), iter.toList());
    }

    // ===================== takeWhileInclusive Additional Edge Cases =====================

    @Test
    public void testTakeWhileInclusive_AllMatch() {
        ObjIterator<Integer> result = Iterators.takeWhileInclusive(Arrays.asList(1, 2, 3).iterator(), n -> n < 10);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testTakeWhileInclusive_FirstFails() {
        ObjIterator<Integer> result = Iterators.takeWhileInclusive(Arrays.asList(5, 6, 7).iterator(), n -> n < 1);
        assertEquals(Arrays.asList(5), result.toList());
    }

    @Test
    public void testTakeWhileInclusiveIterable() {
        Predicate<Integer> lessThan4 = n -> n < 4;

        ObjIterator<Integer> result = Iterators.takeWhileInclusive(Arrays.asList(1, 2, 3, 4, 5), lessThan4);
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        result = Iterators.takeWhileInclusive(Arrays.asList(5, 6), lessThan4);
        assertEquals(Arrays.asList(5), result.toList());

        result = Iterators.takeWhileInclusive((Iterable<Integer>) null, lessThan4);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        Predicate<Integer> lessThan3 = n -> n < 3;

        ObjIterator<Integer> result = Iterators.takeWhileInclusive(Arrays.asList(1, 2, 3, 4).iterator(), lessThan3);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());

        result = Iterators.takeWhileInclusive((Iterator<Integer>) null, lessThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTakeWhileInclusive_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.takeWhileInclusive(Collections.<Integer> emptyIterator(), n -> true);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDropWhileAll() {
        Predicate<Integer> alwaysTrue = x -> true;
        ObjIterator<Integer> result = Iterators.dropWhile(testList, alwaysTrue);

        assertFalse(result.hasNext());
    }

    @Test
    public void testDropWhile() {
        Predicate<Integer> lessThan3 = n -> n < 3;
        ObjIterator<Integer> iter = Iterators.dropWhile(intList.iterator(), lessThan3);

        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(4), iter.next());
        assertEquals(Integer.valueOf(5), iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.dropWhile(intList, lessThan3);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    // ===================== dropWhile with Iterable overload =====================

    @Test
    public void testDropWhileIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.dropWhile(Arrays.asList(1, 2, 3, 4, 5), n -> n < 4);
        assertEquals(Arrays.asList(4, 5), iter.toList());
    }

    @Test
    public void testDropWhile_AllMatch() {
        ObjIterator<Integer> result = Iterators.dropWhile(Arrays.asList(1, 2, 3).iterator(), n -> n < 10);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDropWhileIterable() {
        Predicate<Integer> lessThan3 = n -> n < 3;

        ObjIterator<Integer> result = Iterators.dropWhile(Arrays.asList(1, 2, 3, 4, 5), lessThan3);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());

        result = Iterators.dropWhile(Arrays.asList(1, 2), lessThan3);
        assertFalse(result.hasNext());

        result = Iterators.dropWhile((Iterable<Integer>) null, lessThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDropWhileIterator() {
        Predicate<Integer> lessThan4 = n -> n < 4;

        ObjIterator<Integer> result = Iterators.dropWhile(Arrays.asList(1, 2, 3, 4, 5).iterator(), lessThan4);
        assertEquals(Arrays.asList(4, 5), result.toList());

        result = Iterators.dropWhile((Iterator<Integer>) null, lessThan4);
        assertFalse(result.hasNext());
    }

    // ===================== dropWhile Additional Edge Cases =====================

    @Test
    public void testDropWhile_NoneMatch() {
        ObjIterator<Integer> result = Iterators.dropWhile(Arrays.asList(5, 6, 7).iterator(), n -> n < 1);
        assertEquals(Arrays.asList(5, 6, 7), result.toList());
    }

    @Test
    public void testDropWhile_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.dropWhile(Collections.<Integer> emptyIterator(), n -> true);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntilNeverTrue() {
        Predicate<Integer> greaterThanTen = x -> x > 10;
        ObjIterator<Integer> result = Iterators.skipUntil(testList, greaterThanTen);

        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntil() {
        Predicate<Integer> greaterThan2 = n -> n > 2;
        ObjIterator<Integer> iter = Iterators.skipUntil(intList.iterator(), greaterThan2);

        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(4), iter.next());
        assertEquals(Integer.valueOf(5), iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.skipUntil(intList, greaterThan2);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    // ===================== skipUntil with Iterable overload =====================

    @Test
    public void testSkipUntilIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5), n -> n >= 4);
        assertEquals(Arrays.asList(4, 5), iter.toList());
    }

    // ===================== skipUntil Additional Edge Cases =====================

    @Test
    public void testSkipUntil_FirstMatch() {
        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(5, 6, 7).iterator(), n -> n >= 5);
        assertEquals(Arrays.asList(5, 6, 7), result.toList());
    }

    @Test
    public void testSkipUntilIterable() {
        Predicate<Integer> greaterThan2 = n -> n > 2;

        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5), greaterThan2);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());

        result = Iterators.skipUntil(Arrays.asList(1, 2), greaterThan2);
        assertFalse(result.hasNext());

        result = Iterators.skipUntil((Iterable<Integer>) null, greaterThan2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntilIterator() {
        Predicate<Integer> greaterThan3 = n -> n > 3;

        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5).iterator(), greaterThan3);
        assertEquals(Arrays.asList(4, 5), result.toList());

        result = Iterators.skipUntil((Iterator<Integer>) null, greaterThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntil_NoneMatch() {
        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(1, 2, 3).iterator(), n -> n > 10);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntil_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.skipUntil(Collections.<Integer> emptyIterator(), n -> true);
        assertFalse(result.hasNext());
    }

    @Test
    public void test_skipUntil() throws Exception {
        {
            final int[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            final List<Integer> list = CommonUtil.toList(a);
            final List<Integer> ret = N.skipUntil(list, it -> it > 3);
            assertEquals(CommonUtil.toList(4, 5, 6, 7, 8, 9), ret);
        }
        {
            final int[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            final List<Integer> list = CommonUtil.toList(a);
            final List<Integer> ret = Stream.of(list).skipUntil(it -> it > 3).toList();
            assertEquals(CommonUtil.toList(4, 5, 6, 7, 8, 9), ret);
            final int[] b = IntStream.of(a).skipUntil(it -> it > 3).toArray();
            assertEquals(CommonUtil.toList(4, 5, 6, 7, 8, 9), CommonUtil.toList(b));
        }
    }

    @Test
    public void testMap() {
        Function<Integer, String> toString = Object::toString;
        ObjIterator<String> iter = Iterators.map(intList.iterator(), toString);

        assertEquals("1", iter.next());
        assertEquals("2", iter.next());
        assertEquals("3", iter.next());
        assertEquals("4", iter.next());
        assertEquals("5", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.map(intList, toString);
        assertTrue(iter.hasNext());
        assertEquals("1", iter.next());
    }

    // ===================== map with Iterable overload =====================

    @Test
    public void testMapIterable_Dedicated() {
        ObjIterator<String> iter = Iterators.map(Arrays.asList(1, 2, 3), n -> "v" + n);
        assertEquals(Arrays.asList("v1", "v2", "v3"), iter.toList());
    }

    @Test
    public void testMap_IdentityMapper() {
        ObjIterator<Integer> result = Iterators.map(Arrays.asList(1, 2, 3).iterator(), java.util.function.Function.identity());
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    // ===================== Chained Operations =====================

    @Test
    public void testChainedOperations_FilterThenMap() {
        ObjIterator<String> result = Iterators.map(Iterators.filter(Arrays.asList(1, 2, 3, 4, 5).iterator(), n -> n > 3), Object::toString);
        assertEquals(Arrays.asList("4", "5"), result.toList());
    }

    @Test
    public void testChainedOperations_DistinctThenMap() {
        ObjIterator<String> result = Iterators.map(Iterators.distinct(Arrays.asList(1, 2, 2, 3, 3, 3).iterator()), n -> "v" + n);
        assertEquals(Arrays.asList("v1", "v2", "v3"), result.toList());
    }

    @Test
    public void testMapIterator() {
        Function<String, Integer> mapper = String::length;

        ObjIterator<Integer> result = Iterators.map(Arrays.asList("a", "ab", "abc").iterator(), mapper);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());

        result = Iterators.map((Iterator<String>) null, mapper);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMapNullIterable() {
        Function<Integer, String> mapper = Object::toString;
        ObjIterator<String> result = Iterators.map((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMapNullIterator() {
        Function<Integer, String> mapper = Object::toString;
        ObjIterator<String> result = Iterators.map((Iterator<Integer>) null, mapper);
        assertFalse(result.hasNext());
    }

    // ===================== map Additional Edge Cases =====================

    @Test
    public void testMap_EmptyIterator() {
        ObjIterator<String> result = Iterators.map(Collections.<Integer> emptyIterator(), Object::toString);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMapIterable() {
        Function<Integer, String> mapper = n -> "n" + n;

        ObjIterator<String> result = Iterators.map(Arrays.asList(1, 2, 3), mapper);
        assertEquals(Arrays.asList("n1", "n2", "n3"), result.toList());

        result = Iterators.map((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.map(Arrays.asList(1), null));
    }

    @Test
    public void testMap_NullMapper() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.map(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testFlatMapIteratorToIterable() {
        Function<String, Iterable<Character>> toChars = s -> s.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        ObjIterator<Character> iter = Iterators.flatMap(list("ab", "", "c").iterator(), toChars);
        assertEquals('a', iter.next());
        assertEquals('b', iter.next());
        assertEquals('c', iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatmapIteratorToArray() {
        Function<String, Character[]> toCharsArray = s -> {
            Character[] arr = new Character[s.length()];
            for (int i = 0; i < s.length(); i++)
                arr[i] = s.charAt(i);
            return arr;
        };
        ObjIterator<Character> iter = Iterators.flatmap(list("ab", "", "c").iterator(), toCharsArray);
        assertEquals('a', iter.next());
        assertEquals('b', iter.next());
        assertEquals('c', iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatMap() {
        Function<Integer, List<String>> duplicateAsString = n -> Arrays.asList(String.valueOf(n), String.valueOf(n));

        ObjIterator<String> iter = Iterators.flatMap(Arrays.asList(1, 2, 3).iterator(), duplicateAsString);

        assertEquals("1", iter.next());
        assertEquals("1", iter.next());
        assertEquals("2", iter.next());
        assertEquals("2", iter.next());
        assertEquals("3", iter.next());
        assertEquals("3", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.flatMap(Arrays.asList(1, 2), duplicateAsString);
        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("1", "1", "2", "2"), result);
    }

    @Test
    public void testFlatmapArray() {
        Function<Integer, String[]> duplicateAsArray = n -> new String[] { String.valueOf(n), String.valueOf(n) + "!" };

        ObjIterator<String> iter = Iterators.flatmap(Arrays.asList(1, 2).iterator(), duplicateAsArray);

        assertEquals("1", iter.next());
        assertEquals("1!", iter.next());
        assertEquals("2", iter.next());
        assertEquals("2!", iter.next());
        assertFalse(iter.hasNext());
    }

    // ===================== flatMap with Iterable overload =====================

    @Test
    public void testFlatMapIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.flatMap(Arrays.asList(1, 2, 3), n -> Arrays.asList(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), iter.toList());
    }

    // ===================== flatmap with Iterable overload =====================

    @Test
    public void testFlatmapIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.flatmap(Arrays.asList(1, 2), n -> new Integer[] { n, n * 10 });
        assertEquals(Arrays.asList(1, 10, 2, 20), iter.toList());
    }

    @Test
    public void testFlatMapIterator() {
        Function<String, Iterable<String>> mapper = s -> Arrays.asList(s, s.toUpperCase());

        ObjIterator<String> result = Iterators.flatMap(Arrays.asList("a", "b").iterator(), mapper);
        assertEquals(Arrays.asList("a", "A", "b", "B"), result.toList());

        result = Iterators.flatMap((Iterator<String>) null, mapper);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFlatmapIterator() {
        Function<String, String[]> mapper = s -> new String[] { s, s + s };

        ObjIterator<String> result = Iterators.flatmap(Arrays.asList("a", "b").iterator(), mapper);
        assertEquals(Arrays.asList("a", "aa", "b", "bb"), result.toList());

        result = Iterators.flatmap((Iterator<String>) null, mapper);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFlatmapIterableArray() {
        ObjIterator<String> iter = Iterators.flatmap((Iterable<String>) null, s -> new String[] { s });
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.flatmap(list, s -> new String[] { s + "1", s + "2" });
        assertEquals("a1", iter.next());
        assertEquals("a2", iter.next());
        assertEquals("b1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c1", iter.next());
        assertEquals("c2", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.flatmap(list, s -> new String[0]);
        assertFalse(iter.hasNext());

        iter = Iterators.flatmap(list, s -> null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatmapIteratorArray() {
        ObjIterator<String> iter = Iterators.flatmap((Iterator<String>) null, s -> new String[] { s });
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.flatmap(list.iterator(), s -> new String[] { s + "1", s + "2" });
        assertEquals("a1", iter.next());
        assertEquals("a2", iter.next());
        assertEquals("b1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c1", iter.next());
        assertEquals("c2", iter.next());
        assertFalse(iter.hasNext());
    }

    // ===================== flatMap Additional Edge Cases =====================

    @Test
    public void testFlatMap_EmptyResults() {
        ObjIterator<Integer> result = Iterators.flatMap(Arrays.asList(1, 2, 3).iterator(), n -> Collections.<Integer> emptyList());
        assertFalse(result.hasNext());
    }

    @Test
    public void testFlatMap_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.flatMap(Collections.<Integer> emptyIterator(), n -> Arrays.asList(n));
        assertFalse(result.hasNext());
    }

    // ===================== flatmap Additional Edge Cases =====================

    @Test
    public void testFlatmap_EmptyResults() {
        ObjIterator<Integer> result = Iterators.flatmap(Arrays.asList(1, 2, 3).iterator(), n -> new Integer[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFlatmap_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.flatmap(Collections.<Integer> emptyIterator(), n -> new Integer[] { n });
        assertFalse(result.hasNext());
    }

    @Test
    public void testFlatMapIterable() {
        Function<Integer, Iterable<Integer>> mapper = n -> Arrays.asList(n, n * 10);

        ObjIterator<Integer> result = Iterators.flatMap(Arrays.asList(1, 2, 3), mapper);
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());

        result = Iterators.flatMap((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.flatMap(Arrays.asList(1), null));
    }

    @Test
    public void testFlatmapIterable() {
        Function<Integer, Integer[]> mapper = n -> new Integer[] { n, n * 2 };

        ObjIterator<Integer> result = Iterators.flatmap(Arrays.asList(1, 2), mapper);
        assertEquals(Arrays.asList(1, 2, 2, 4), result.toList());

        result = Iterators.flatmap((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.flatmap(Arrays.asList(1), null));
    }

    @Test
    public void testFlatMap_NullMapper() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.flatMap(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testFlatmap_NullMapper() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.flatmap(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testForEachIteratorWithOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachIteratorWithOffsetAndCount() {
        AtomicInteger sum = new AtomicInteger(0);
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3, sum::addAndGet);
        assertEquals(9, sum.get());

        sum.set(0);
        Iterators.forEach(Arrays.asList(1, 2).iterator(), 0, 1, sum::addAndGet);
        assertEquals(1, sum.get());
    }

    @Test
    public void testForEachIteratorWithOffsetCountAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 2, 2, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(7, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachIteratorWithThreads() {
        AtomicInteger sum = new AtomicInteger(0);
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 0, 5, 1, 1, sum::addAndGet);
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEachIteratorWithThreadsAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), 0, 3, 1, 1, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionOfIteratorsWithOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3).iterator());

        Iterators.forEach(iterators, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionWithOffsetAndCount() {
        AtomicInteger sum = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5).iterator());

        Iterators.forEach(iterators, 1, 3, sum::addAndGet);
        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, 0, 3, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionWithThreads() {
        AtomicInteger sum = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, 1, 1, 1, sum::addAndGet);
        assertEquals(10, sum.get());
    }

    @Test
    public void testForEachCollectionWithThreadsAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3).iterator());

        Iterators.forEach(iterators, 1, 1, 1, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachIteratorWithOffsetCountThreadsAndQueueSize() {
        AtomicInteger sum = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3, 1, 1, sum::addAndGet);

        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachIteratorWithOffsetCountThreadsQueueSizeAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3, 1, 1, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(9, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountReadThreadsProcessThreadsAndQueueSize() {
        AtomicLong sum = new AtomicLong(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5).iterator());

        Iterators.forEach(iterators, 1, 3, 1, 1, 1, n -> sum.addAndGet(n));
        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountReadThreadsProcessThreadsQueueSizeAndOnComplete() {
        AtomicLong sum = new AtomicLong(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, 0, 3, 1, 1, 1, n -> sum.addAndGet(n), () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachIterator() {
        AtomicInteger sum = new AtomicInteger(0);
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), sum::addAndGet);
        assertEquals(15, sum.get());

        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach((Iterator<Integer>) null, n -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEachCollectionOfIterators() {
        AtomicInteger sum = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, sum::addAndGet);
        assertEquals(10, sum.get());

        sum.set(0);
        Iterators.forEach((Collection<Iterator<Integer>>) null, sum::addAndGet);
        assertEquals(0, sum.get());
    }

    @Test
    public void test_forEachPair_2() throws Exception {

        final Throwables.BiConsumer<Integer, Integer, RuntimeException> action2 = (a, b) -> N.println(a + " - " + b);

        Stream.of(IntList.range(1, 10).toList().iterator()).forEachPair(action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachPair(1, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachPair(2, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachPair(3, action2);
        N.println("==========================================================================");

        final Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException> action = (a, b, c) -> N.println(a + " - " + b + " - " + c);

        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(1, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(2, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(3, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(4, action);
        assertNotNull(action);
    }

    @Test
    public void test_forEachPair_2_1() throws Exception {

        final Throwables.BiConsumer<Integer, Integer, RuntimeException> action2 = (a, b) -> N.println(a + " - " + b);

        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachPair(action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachPair(1, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachPair(2, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachPair(3, action2);
        N.println("==========================================================================");

        final Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException> action = (a, b, c) -> N.println(a + " - " + b + " - " + c);

        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(1, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(2, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(3, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(4, action);
        assertNotNull(action);
    }

    @Test
    public void test_forEachPair_3() throws Exception {

        final Throwables.BiConsumer<Integer, Integer, RuntimeException> action2 = (a, b) -> N.println(a + " - " + b);

        Stream.of(IntList.range(1, 10).toList()).forEachPair(action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).forEachPair(1, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).forEachPair(2, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).forEachPair(3, action2);
        N.println("==========================================================================");

        final Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException> action = (a, b, c) -> N.println(a + " - " + b + " - " + c);

        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(1, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(2, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(3, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(4, action);
        N.println("==========================================================================");
        assertNotNull(action);
    }

    @Test
    public void test_forEachPair_3_1() throws Exception {

        final Throwables.BiConsumer<Integer, Integer, RuntimeException> action2 = (a, b) -> N.println(a + " - " + b);

        Stream.of(IntList.range(1, 10).toList()).parallel(2).forEachPair(action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).forEachPair(1, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).forEachPair(2, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).forEachPair(3, action2);
        N.println("==========================================================================");

        final Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException> action = (a, b, c) -> N.println(a + " - " + b + " - " + c);

        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(1, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(2, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(3, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(4, action);
        N.println("==========================================================================");
        assertNotNull(action);
    }

    @Test
    public void test_forEachPair_4_1() throws Exception {

        new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b) {
                return a + b;
            }
        };

        final BiFunction<Integer, Integer, String> action2_1 = (a, b) -> a + "-" + b;

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(1, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(2, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(3, action2_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(1, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(3, true, action2_1).println();
        N.println("==========================================================================");

        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(1, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(2, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(3, action2_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(1, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(2, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(3, true, action2_1).println();
        N.println("==========================================================================");

        new TriFunction<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b, final Integer c) {
                return a + b + c;
            }
        };

        final TriFunction<Integer, Integer, Integer, String> action3_1 = (a, b, c) -> a + "-" + b + "-" + c;

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(1, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(2, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(3, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(4, action3_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(1, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(3, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(4, true, action3_1).println();

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(1, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(2, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(3, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(4, action3_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(1, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(2, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(3, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(4, true, action3_1).println();
        assertNotNull(action3_1);
    }

    @Test
    public void test_forEachPair_4_2() throws Exception {

        new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b) {
                return a + b;
            }
        };

        final BiFunction<Integer, Integer, String> action2_1 = (a, b) -> a + "-" + b;

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).slidingMap(1, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(2, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(3, action2_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).slidingMap(1, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(2, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(3, true, action2_1).println();
        N.println("==========================================================================");

        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(1, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(2, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(3, action2_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(1, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(2, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(3, true, action2_1).println();
        N.println("==========================================================================");

        new TriFunction<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b, final Integer c) {
                return a + b + c;
            }
        };

        final TriFunction<Integer, Integer, Integer, String> action3_1 = (a, b, c) -> a + "-" + b + "-" + c;

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).slidingMap(1, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(2, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(3, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(4, action3_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).slidingMap(1, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(2, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(3, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(4, true, action3_1).println();

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(1, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(2, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(3, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(4, action3_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(1, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(2, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(3, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(4, true, action3_1).println();
        assertNotNull(action3_1);
    }

    @Test
    public void test_forEachPair_4_3() throws Exception {

        new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b) {
                return a + b;
            }
        };

        final BiFunction<Integer, Integer, String> action2_1 = (a, b) -> a + "-" + b;

        N.println("==========================================================================");
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, action2_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, action2_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(3, action2_1).stream().println();
        N.println("==========================================================================");
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, true, action2_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, action2_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(3, true, action2_1).stream().println();
        N.println("==========================================================================");

        new TriFunction<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b, final Integer c) {
                return a + b + c;
            }
        };

        final TriFunction<Integer, Integer, Integer, String> action3_1 = (a, b, c) -> a + "-" + b + "-" + c;

        N.println("==========================================================================");
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(3, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(4, action3_1).stream().println();
        N.println("==========================================================================");
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, true, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(3, true, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(4, true, action3_1).stream().println();
        assertNotNull(action3_1);
    }

    @Test
    public void testForEachIteratorConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        Throwables.Consumer<String, Exception> consumer = result::add;
        Iterators.forEach(list("a", "b").iterator(), consumer);
        assertEquals(list("a", "b"), result);
    }

    @Test
    public void testForEachIteratorConsumerOnComplete() throws Exception {
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        Iterators.forEach(list("a", "b").iterator(), consumer, onComplete);
        assertEquals(list("a", "b"), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachIteratorOffsetCount() throws Exception {
        List<String> result = new ArrayList<>();
        Throwables.Consumer<String, Exception> consumer = result::add;
        Iterators.forEach(list("a", "b", "c", "d").iterator(), 1, 2, consumer);
        assertEquals(list("b", "c"), result);
    }

    @Test
    public void testForEachIteratorParallelSequentialPath() throws Exception {
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        Iterators.forEach(list("x", "y", "z").iterator(), 0, 3, 0, 0, consumer, onComplete);
        assertEquals(list("x", "y", "z"), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachCollectionOfIteratorsSequentialPath() throws Exception {
        Collection<Iterator<String>> iterators = list(list("a", "b").iterator(), list("c").iterator());
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        Iterators.forEach(iterators, 0, Long.MAX_VALUE, 0, 0, 0, consumer, onComplete);
        assertEquals(list("a", "b", "c"), result);
        assertTrue(completed.get());
    }

    @Disabled("Parallel forEach tests require more setup or a test environment that can handle threads properly.")
    @Test
    public void testForEachIteratorParallelPath() throws Exception {
    }

    @Test
    public void testForEachIteratorWithOffsetCountConsumerAndOnComplete() throws Exception {
        List<Integer> result = new ArrayList<>();
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(testIterator, 2L, 5L, (Throwables.Consumer<Integer, Exception>) result::add,
                (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Assertions.assertEquals(Arrays.asList(3, 4, 5, 6, 7), result);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    @Test
    public void testForEachIteratorWithZeroOffset() throws Exception {
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(testIterator, 0L, 3L, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> {
        });

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachIteratorWithOffsetExceedingSize() throws Exception {
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(testIterator, 15L, 5L, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> {
        });

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachIteratorWithThreadsNoOnComplete() throws Exception {
        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());

        Iterators.forEach(testIterator, 1L, 4L, 2, 10, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(2, 3, 4, 5), synchronizedResult);
    }

    @Test
    public void testForEachIteratorWithZeroThreads() throws Exception {
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(testIterator, 0L, 3L, 0, 0, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> {
        });

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachCollectionBasic() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator(),
                Arrays.asList(7, 8, 9).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testForEachEmptyCollection() throws Exception {
        List<Iterator<Integer>> emptyIterators = new ArrayList<>();
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(emptyIterators, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachCollectionWithOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        List<Integer> result = new ArrayList<>();
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCount() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4).iterator(), Arrays.asList(5, 6, 7, 8).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, 2L, 4L, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(3, 4, 5, 6), result);
    }

    @Test
    public void testForEachCollectionWithThreadsNoOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        Iterators.forEach(iterators, 2, 2, 10, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), synchronizedResult);
    }

    @Test
    public void testForEachCollectionFullParamsNoOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5).iterator(), Arrays.asList(6, 7, 8, 9, 10).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        Iterators.forEach(iterators, 2L, 5L, 1, 3, 10, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(3, 4, 5, 6, 7), synchronizedResult);
    }

    @Test
    public void testForEachCollectionFullParams() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(100, 101, 102).iterator(), Arrays.asList(103, 104, 105).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(iterators, 1L, 4L, 1, 2, 10, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add,
                (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(101, 102, 103, 104), synchronizedResult);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    @Test
    public void testNegativeOffset() {
        List<Iterator<Integer>> iterators = Arrays.asList(testIterator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Iterators.forEach(iterators, -1L, 5L, 0, 0, 0, (Throwables.Consumer<Integer, Exception>) e -> {
            }, (Throwables.Runnable<Exception>) () -> {
            });
        });
    }

    @Test
    public void testNegativeCount() {
        List<Iterator<Integer>> iterators = Arrays.asList(testIterator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Iterators.forEach(iterators, 0L, -1L, 0, 0, 0, (Throwables.Consumer<Integer, Exception>) e -> {
            }, (Throwables.Runnable<Exception>) () -> {
            });
        });
    }

    @Test
    public void testConsumerException() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator());

        Assertions.assertThrows(RuntimeException.class, () -> {
            Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) e -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testOnCompleteException() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1).iterator());

        Assertions.assertThrows(RuntimeException.class, () -> {
            Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) e -> {
            }, (Throwables.Runnable<Exception>) () -> {
                throw new RuntimeException("OnComplete exception");
            });
        });
    }

    @Test
    public void testMultiThreadedExceptionHandling() {
        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5).iterator());

        AtomicInteger counter = new AtomicInteger(0);

        Assertions.assertThrows(RuntimeException.class, () -> {
            Iterators.forEach(iterators, 0L, 5L, 1, 2, 10, e -> {
                if (counter.incrementAndGet() == 3) {
                    throw new RuntimeException("Multi-threaded exception");
                }
            }, () -> {
            });
        });
    }

    @Test
    public void testLargeCountValue() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, 0L, Long.MAX_VALUE, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testZeroCountValue() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, 0L, 0L, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyIteratorInCollection() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Collections.<Integer> emptyIterator(), Arrays.asList(1, 2).iterator(),
                Collections.<Integer> emptyIterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testForEachOneParam() throws Exception {
        List<String> result = new ArrayList<>();
        List<String> list = Arrays.asList("a", "b", "c");

        Iterators.forEach(list.iterator(), result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result.clear();
        Iterators.forEach(Collections.<String> emptyIterator(), result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachTwoParams() throws Exception {
        List<String> result = new ArrayList<>();
        List<String> list = Arrays.asList("a", "b", "c");
        boolean[] completed = { false };

        Iterators.forEach(list.iterator(), result::add, () -> completed[0] = true);
        assertEquals(Arrays.asList("a", "b", "c"), result);
        assertTrue(completed[0]);

        result.clear();
        completed[0] = false;
        Iterators.forEach(Collections.<String> emptyIterator(), result::add, () -> completed[0] = true);
        assertTrue(result.isEmpty());
        assertTrue(completed[0]);
    }

    @Test
    public void testForEachThreeParams() throws Exception {
        List<String> result = new ArrayList<>();
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Iterators.forEach(list.iterator(), 1L, 3L, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        result.clear();
        Iterators.forEach(list.iterator(), 10L, 3L, result::add);
        assertTrue(result.isEmpty());

        result.clear();
        Iterators.forEach(list.iterator(), 0L, 0L, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachFourParams() throws Exception {
        List<String> result = new ArrayList<>();
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        boolean[] completed = { false };

        Iterators.forEach(list.iterator(), 1L, 3L, result::add, () -> completed[0] = true);
        assertEquals(Arrays.asList("b", "c", "d"), result);
        assertTrue(completed[0]);

        result.clear();
        completed[0] = false;
        Iterators.forEach(list.iterator(), 2L, 10L, result::add, () -> completed[0] = true);
        assertEquals(Arrays.asList("c", "d", "e"), result);
        assertTrue(completed[0]);
    }

    @Test
    public void testForEachBasic() {
        List<Integer> processed = new ArrayList<>();
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), consumer);

        assertEquals(testList, processed);
    }

    @Test
    public void testForEachWithOnComplete() {
        List<Integer> processed = new ArrayList<>();
        AtomicInteger completeCalled = new AtomicInteger(0);

        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;
        Throwables.Runnable<RuntimeException> onComplete = () -> completeCalled.incrementAndGet();

        Iterators.forEach(testList.iterator(), consumer, onComplete);

        assertEquals(testList, processed);
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachWithOffsetAndCount() {
        List<Integer> processed = new ArrayList<>();
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), 1, 3, consumer);

        assertEquals(Arrays.asList(2, 3, 4), processed);
    }

    @Test
    public void testForEachWithThreads() {
        List<Integer> processed = Collections.synchronizedList(new ArrayList<>());
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), 0, Long.MAX_VALUE, 2, 4, consumer);

        processed.sort(Integer::compareTo);
        assertEquals(testList, processed);
    }

    @Test
    public void testForEach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Iterators.forEach(intList.iterator(), collected::add);
        assertEquals(intList, collected);

        collected.clear();
        Iterators.forEach(intList.iterator(), 1, 3, collected::add);
        assertEquals(Arrays.asList(2, 3, 4), collected);

        AtomicInteger completeCount = new AtomicInteger(0);
        collected.clear();
        Iterators.forEach(intList.iterator(), collected::add, completeCount::incrementAndGet);
        assertEquals(intList, collected);
        assertEquals(1, completeCount.get());
    }

    @Test
    public void testForEachMultipleIterators() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5, 6).iterator());

        List<Integer> collected = new ArrayList<>();
        Iterators.forEach(iterators, collected::add);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), collected);

        iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());
        collected.clear();
        Iterators.forEach(iterators, 2, 3, collected::add);
        assertEquals(Arrays.asList(3, 4, 5), collected);
    }

    // ===================== forEach with offset/count/threads edge cases =====================

    @Test
    public void testForEach_EmptyIterator() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach(Collections.<Integer> emptyList().iterator(), v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEach_WithOffset() throws Exception {
        List<Integer> result = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 2, 2, v -> result.add(v));
        assertEquals(Arrays.asList(3, 4), result);
    }

    @Test
    public void testForEach_ZeroCount() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), 0, 0, v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    // ===================== forEach Additional Edge Cases =====================

    @Test
    public void testForEach_NullIterator() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach((Iterator<Integer>) null, v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEach_NullIteratorWithOnComplete() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        Iterators.forEach((Iterator<Integer>) null, v -> count.incrementAndGet(), () -> completed.set(true));
        assertEquals(0, count.get());
        assertTrue(completed.get());
    }

    @Test
    public void testForEach_OffsetAndCount_OffsetBeyondSize() throws Exception {
        List<Integer> result = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), 10, 5, v -> result.add(v));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEach_OffsetAndCount_CountExceedsRemaining() throws Exception {
        List<Integer> result = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 3, 10, v -> result.add(v));
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testForEachCollectionOfIterators_EmptyCollection() throws Exception {
        List<Integer> result = new ArrayList<>();
        Iterators.forEach(Collections.<Iterator<Integer>> emptyList(), v -> result.add(v));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachCollectionOfIterators_NullCollection() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach((Collection<Iterator<Integer>>) null, v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEachCollection_WithOffset() throws Exception {
        List<Iterator<? extends Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());
        List<Integer> collected = new ArrayList<>();
        Iterators.forEach(iterators, 1, 4, collected::add);
        assertEquals(4, collected.size());
    }

    @Test
    public void testForEachCollection_WithThreads() throws Exception {
        List<Iterator<? extends Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5).iterator());
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach(iterators, 0, Long.MAX_VALUE, 0, 0, 0, e -> count.incrementAndGet(), null);
        assertEquals(5, count.get());
    }

    @Test
    public void test_ObjListIterator() {
        final ObjListIterator<String> iterA = ObjListIterator.empty();
        final ListIterator<Object> iterB = List.of().listIterator();

        assertEquals(iterB.previousIndex(), iterA.previousIndex());
        assertEquals(iterB.nextIndex(), iterA.nextIndex());
        assertEquals(iterB.previousIndex(), iterA.previousIndex());
        assertEquals(iterB.nextIndex(), iterA.nextIndex());
    }

}
