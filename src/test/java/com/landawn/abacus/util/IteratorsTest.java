package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.Function;

public class IteratorsTest extends IteratorsTestSupport {
    @Test
    public void testForEachProcessingWorkersReadSingleSourceAndCompletionUsesCaller() {
        final Thread caller = Thread.currentThread();
        for (final int processThreads : new int[] { 0, 2 }) {
            final java.util.Set<Thread> readers = Collections.synchronizedSet(new java.util.HashSet<>());
            final AtomicInteger processed = new AtomicInteger();
            final AtomicReference<Thread> completionThread = new AtomicReference<>();
            final Iterator<Integer> source = new Iterator<>() {
                private int cursor;

                @Override
                public boolean hasNext() {
                    readers.add(Thread.currentThread());
                    return cursor < 4;
                }

                @Override
                public Integer next() {
                    readers.add(Thread.currentThread());
                    return cursor++;
                }
            };

            Iterators.forEach(source, Iterators.IterateOptions.builder().processThreads(processThreads).build(), value -> processed.incrementAndGet(),
                    () -> completionThread.set(Thread.currentThread()));

            assertEquals(4, processed.get());
            assertFalse(readers.isEmpty());
            assertEquals(processThreads == 0, readers.contains(caller));
            if (processThreads == 0) {
                assertEquals(1, readers.size());
            }
            Assertions.assertSame(caller, completionThread.get());
        }
    }

    @Test
    public void testLimitPreservesQuotaWhenSupplierFailsBeforeProducingValue() {
        for (final int offset : new int[] { 0, 2 }) {
            final int[] delivered = { 0 };
            final AtomicBoolean failed = new AtomicBoolean();
            final ObjIterator<Integer> source = ObjIterator.generate(() -> {
                if (delivered[0] == offset && !failed.getAndSet(true)) {
                    throw new IllegalStateException("temporary source failure");
                }
                return delivered[0]++;
            });
            final ObjIterator<Integer> limited = offset == 0 ? Iterators.limit(source, 1) : Iterators.skipAndLimit(source, offset, 1);

            assertThrows(IllegalStateException.class, limited::next);
            assertTrue(limited.hasNext());
            assertEquals(offset, limited.next());
            assertFalse(limited.hasNext());
            assertThrows(NoSuchElementException.class, limited::next);
            assertEquals(offset + 1, delivered[0]);
        }
    }

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

    @Test
    public void testCount_LongResult() {
        assertEquals(5L, Iterators.count(Arrays.asList(1, 2, 3, 4, 5).iterator()));
        assertEquals(0L, Iterators.count(Collections.emptyList().iterator()));
    }

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

    @Test
    public void testIndexOf_WithFromIndex() {
        assertEquals(3, Iterators.indexOf(Arrays.asList("a", "b", "c", "a").iterator(), "a", 1));
        assertEquals(-1, Iterators.indexOf(Arrays.asList("a", "b").iterator(), "a", 5));
    }

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

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.unzip(Arrays.asList("a1").iterator(), null));
    }

    @Test
    public void testUnzip_NullUnzipFunction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
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
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.unzip3(Arrays.asList("a").iterator(),
                (com.landawn.abacus.util.function.BiConsumer<String, Triple<String, Integer, Boolean>>) null));
    }

    @Test
    public void testAdvanceMoreThanAvailable() {
        Iterator<Integer> iter = testList.iterator();
        long advanced = Iterators.advance(iter, 10);

        assertEquals(5, advanced);
        assertFalse(iter.hasNext());
    }

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

    @Test
    public void testChainedOperations_SkipThenLimitThenFilter() {
        ObjIterator<Integer> skipped = Iterators.skip(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).iterator(), 2);
        ObjIterator<Integer> limited = Iterators.limit(skipped, 5);
        ObjIterator<Integer> filtered = Iterators.filter(limited, n -> n % 2 == 0);
        assertEquals(Arrays.asList(4, 6), filtered.toList());
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
    public void testLimitMoreThanSize() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> limited = Iterators.limit(iter, 10);

        List<Integer> result = new ArrayList<>();
        while (limited.hasNext()) {
            result.add(limited.next());
        }

        assertEquals(testList, result);
    }

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

    @Test
    public void testDistinctByIterable_Dedicated() {
        ObjIterator<String> iter = Iterators.distinctBy(Arrays.asList("abc", "aXX", "bYY", "bZZ"), s -> s.charAt(0));
        assertEquals(Arrays.asList("abc", "bYY"), iter.toList());
    }

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

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a"), null));
    }

    @Test
    public void testDistinctByIterator() {
        ObjIterator<String> result = Iterators.distinctBy(Arrays.asList("a", "ab", "b", "abc").iterator(), String::length);
        List<String> list = result.toList();
        assertEquals(3, list.size());

        result = Iterators.distinctBy((Iterator<String>) null, String::length);
        assertFalse(result.hasNext());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a").iterator(), null));
    }

    @Test
    public void testDistinctByNullKeyExtractor() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(testList, null));
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

    @Test
    public void testFilterIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.filter(Arrays.asList(1, 2, 3, 4, 5), n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4), iter.toList());
    }

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
    public void testFilterIterable() {
        Predicate<Integer> isEven = n -> n % 2 == 0;

        ObjIterator<Integer> result = Iterators.filter(Arrays.asList(1, 2, 3, 4, 5, 6), isEven);
        assertEquals(Arrays.asList(2, 4, 6), result.toList());

        result = Iterators.filter((Iterable<Integer>) null, isEven);
        assertFalse(result.hasNext());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.filter(Arrays.asList(1), null));
    }

    @Test
    public void testFilterIterator() {
        Predicate<Integer> isOdd = n -> n % 2 != 0;

        ObjIterator<Integer> result = Iterators.filter(Arrays.asList(1, 2, 3, 4, 5).iterator(), isOdd);
        assertEquals(Arrays.asList(1, 3, 5), result.toList());

        result = Iterators.filter((Iterator<Integer>) null, isOdd);
        assertFalse(result.hasNext());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.filter(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testFilter_NullPredicate() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.filter(Arrays.asList(1).iterator(), null));
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

    @Test
    public void testTakeWhileIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.takeWhile(Arrays.asList(1, 2, 3, 4, 5), n -> n < 4);
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

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

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.takeWhile(Arrays.asList(1), null));
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

    @Test
    public void testTakeWhileInclusiveIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.takeWhileInclusive(Arrays.asList(1, 2, 3, 4, 5), n -> n < 4);
        assertEquals(Arrays.asList(1, 2, 3, 4), iter.toList());
    }

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

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.map(Arrays.asList(1), null));
    }

    @Test
    public void testMap_NullMapper() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.map(Arrays.asList(1).iterator(), null));
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
            for (int i = 0; i < s.length(); i++) {
                arr[i] = s.charAt(i);
            }
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

    @Test
    public void testFlatMapIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.flatMap(Arrays.asList(1, 2, 3), n -> Arrays.asList(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), iter.toList());
    }

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

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.flatMap(Arrays.asList(1), null));
    }

    @Test
    public void testFlatmapIterable() {
        Function<Integer, Integer[]> mapper = n -> new Integer[] { n, n * 2 };

        ObjIterator<Integer> result = Iterators.flatmap(Arrays.asList(1, 2), mapper);
        assertEquals(Arrays.asList(1, 2, 2, 4), result.toList());

        result = Iterators.flatmap((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.flatmap(Arrays.asList(1), null));
    }

    @Test
    public void testFlatMap_NullMapper() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.flatMap(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testFlatmap_NullMapper() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.flatmap(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testNegativeOffset() {
        List<Iterator<Integer>> iterators = Arrays.asList(testIterator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Iterators.forEach(iterators, Iterators.IterateOptions.builder().offset(-1L).count(5L).readThreads(0).processThreads(0).queueSize(0).build(),
                    (Throwables.Consumer<Integer, Exception>) e -> {
                    }, (Throwables.Runnable<Exception>) () -> {
                    });
        });
    }

    @Test
    public void testNegativeCount() {
        List<Iterator<Integer>> iterators = Arrays.asList(testIterator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Iterators.forEach(iterators, Iterators.IterateOptions.builder().offset(0L).count(-1L).readThreads(0).processThreads(0).queueSize(0).build(),
                    (Throwables.Consumer<Integer, Exception>) e -> {
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
    public void testSingleThreadedForEachPropagatesCheckedException() {
        final List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1).iterator());

        assertThrows(IOException.class, () -> Iterators.forEach(iterators, (Throwables.Consumer<Integer, IOException>) e -> {
            throw new IOException("checked");
        }));
    }

    /**
     * The parallel path used to wrap a checked exception in a {@code RuntimeException}, which made
     * {@code catch (IOException e)} - still demanded by the compiler, since E is inferred from the consumer -
     * stop matching as soon as {@code processThreads} was raised. It now propagates as {@code E}, unchanged.
     */
    @Test
    public void testParallelForEachPropagatesCheckedExceptionUnwrapped() {
        final List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1).iterator());

        final IOException thrown = assertThrows(IOException.class,
                () -> Iterators.forEach(iterators,
                        Iterators.IterateOptions.builder().offset(0L).count(1L).readThreads(0).processThreads(1).queueSize(0).build(),
                        (Throwables.Consumer<Integer, IOException>) e -> {
                            throw new IOException("checked");
                        }, (Throwables.Runnable<Exception>) () -> {
                        }));
        assertEquals("checked", thrown.getMessage());
    }

    @Test
    public void testMultiThreadedExceptionHandling() {
        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5).iterator());

        AtomicInteger counter = new AtomicInteger(0);

        Assertions.assertThrows(RuntimeException.class, () -> {
            Iterators.forEach(iterators, Iterators.IterateOptions.builder().offset(0L).count(5L).readThreads(1).processThreads(2).queueSize(10).build(), e -> {
                if (counter.incrementAndGet() == 3) {
                    throw new RuntimeException("Multi-threaded exception");
                }
            }, () -> {
            });
        });
    }

    @Test
    public void testInterruptedParallelForEachStopsWorkersAndPreservesInterrupt() throws Exception {
        final CountDownLatch workersStarted = new CountDownLatch(2);
        final CountDownLatch releaseWorkers = new CountDownLatch(1);
        final CountDownLatch workersExited = new CountDownLatch(2);
        final AtomicInteger interruptedWorkers = new AtomicInteger();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicBoolean coordinatorInterrupted = new AtomicBoolean();

        final Thread coordinator = new Thread(() -> {
            try {
                final Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator());

                Iterators.forEach(iterators,
                        Iterators.IterateOptions.builder().offset(0L).count(Long.MAX_VALUE).readThreads(0).processThreads(2).queueSize(0).build(),
                        (Throwables.Consumer<Integer, Exception>) value -> {
                            workersStarted.countDown();

                            try {
                                releaseWorkers.await();
                            } catch (final InterruptedException e) {
                                interruptedWorkers.incrementAndGet();
                                throw e;
                            } finally {
                                workersExited.countDown();
                            }
                        }, (Throwables.Runnable<Exception>) () -> {
                        });
            } catch (final Throwable e) {
                failure.set(e);
                coordinatorInterrupted.set(Thread.currentThread().isInterrupted());
            }
        }, "IteratorsTest-interrupted-coordinator");

        coordinator.start();

        try {
            assertTrue(workersStarted.await(5, TimeUnit.SECONDS));
            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));

            assertFalse(coordinator.isAlive());
            assertNotNull(failure.get());
            assertTrue(coordinatorInterrupted.get());
            assertTrue(workersExited.await(5, TimeUnit.SECONDS));
            assertEquals(2, interruptedWorkers.get());
        } finally {
            // Also makes the regression fail cleanly against the old implementation, whose
            // shutdown() left both workers blocked after the coordinator returned.
            releaseWorkers.countDown();
            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));
            workersExited.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testInterruptedParallelForEachKeepsPriorWorkerFailureReachable() throws Exception {
        final CountDownLatch blockedWorkerStarted = new CountDownLatch(1);
        final CountDownLatch workersStarted = new CountDownLatch(3);
        final CountDownLatch releaseBlockedWorker = new CountDownLatch(1);
        final AtomicReference<Throwable> coordinatorFailure = new AtomicReference<>();
        final RuntimeException firstWorkerFailure = new RuntimeException("first prior worker failure");
        final RuntimeException secondWorkerFailure = new RuntimeException("second prior worker failure");
        final Thread coordinator = new Thread(() -> {
            try {
                final Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator());

                Iterators.forEach(iterators,
                        Iterators.IterateOptions.builder().offset(0L).count(Long.MAX_VALUE).readThreads(0).processThreads(3).queueSize(0).build(),
                        (Throwables.Consumer<Integer, Exception>) value -> {
                            workersStarted.countDown();
                            assertTrue(workersStarted.await(5, TimeUnit.SECONDS));

                            if (value <= 2) {
                                throw value == 1 ? firstWorkerFailure : secondWorkerFailure;
                            }

                            blockedWorkerStarted.countDown();
                            releaseBlockedWorker.await();
                        }, (Throwables.Runnable<Exception>) () -> {
                        });
            } catch (final Throwable e) {
                coordinatorFailure.set(e);
            }
        }, "IteratorsTest-prior-failure-interrupted-coordinator");

        try {
            coordinator.start();
            assertTrue(workersStarted.await(5, TimeUnit.SECONDS));
            assertTrue(blockedWorkerStarted.await(5, TimeUnit.SECONDS));
            final Throwable priorWorkerFailure = awaitAggregatedWorkerFailure(firstWorkerFailure, secondWorkerFailure, 5, TimeUnit.SECONDS);
            assertNotNull(priorWorkerFailure, "Both worker failures must be recorded before interruption");

            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));

            assertFalse(coordinator.isAlive());
            final Throwable interruption = findCause(coordinatorFailure.get(), InterruptedException.class);
            assertNotNull(interruption);
            assertTrue(Arrays.asList(interruption.getSuppressed()).contains(priorWorkerFailure),
                    "The interruption returned to the caller must retain the earlier worker failure");
        } finally {
            releaseBlockedWorker.countDown();
            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));
        }
    }

    @Test
    public void testInterruptedParallelForEachWaitingWorkerDoesNotPullAnotherElement() throws Exception {
        final CountDownLatch firstHasNextEntered = new CountDownLatch(1);
        final CountDownLatch releaseFirstHasNext = new CountDownLatch(1);
        final AtomicReference<Thread> firstWorker = new AtomicReference<>();
        final AtomicReference<Thread> waitingWorker = new AtomicReference<>();
        final AtomicInteger hasNextCalls = new AtomicInteger();
        final AtomicInteger consumerCalls = new AtomicInteger();
        final Iterator<Integer> source = new Iterator<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                if (hasNextCalls.incrementAndGet() == 1) {
                    firstWorker.set(Thread.currentThread());
                    firstHasNextEntered.countDown();

                    boolean released = false;
                    while (!released) {
                        try {
                            releaseFirstHasNext.await();
                            released = true;
                        } catch (final InterruptedException ignored) {
                            // Keep the first worker inside hasNext so the second remains queued on
                            // the shared iterator monitor until the coordinator publishes cancellation.
                        }
                    }
                }

                return cursor < 2;
            }

            @Override
            public Integer next() {
                return ++cursor;
            }
        };
        final AtomicReference<Throwable> coordinatorFailure = new AtomicReference<>();
        final Thread coordinator = new Thread(() -> {
            try {
                Iterators.forEach(Arrays.asList(source),
                        Iterators.IterateOptions.builder().offset(0L).count(Long.MAX_VALUE).readThreads(0).processThreads(2).queueSize(0).build(),
                        (Throwables.Consumer<Integer, Exception>) value -> consumerCalls.incrementAndGet(), (Throwables.Runnable<Exception>) () -> {
                        });
            } catch (final Throwable e) {
                coordinatorFailure.set(e);
            }
        }, "IteratorsTest-cancellation-race-coordinator");

        try {
            coordinator.start();
            assertTrue(firstHasNextEntered.await(5, TimeUnit.SECONDS));
            assertTrue(awaitBlockedIteratorsWorker(firstWorker.get(), waitingWorker, 5, TimeUnit.SECONDS),
                    "Second worker must pass the cancellation check and contend for the iterator monitor");

            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse(coordinator.isAlive());
            assertNotNull(findCause(coordinatorFailure.get(), InterruptedException.class));
        } finally {
            releaseFirstHasNext.countDown();
            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));

            if (firstWorker.get() != null) {
                firstWorker.get().join(TimeUnit.SECONDS.toMillis(5));
            }

            if (waitingWorker.get() != null) {
                waitingWorker.get().join(TimeUnit.SECONDS.toMillis(5));
            }
        }

        assertEquals(1, consumerCalls.get(), "Only the worker already inside iterator access may finish after cancellation");
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
    public void test_ObjListIterator() {
        final ObjListIterator<String> iterA = ObjListIterator.empty();
        final ListIterator<Object> iterB = List.of().listIterator();

        assertEquals(iterB.previousIndex(), iterA.previousIndex());
        assertEquals(iterB.nextIndex(), iterA.nextIndex());
        assertEquals(iterB.previousIndex(), iterA.previousIndex());
        assertEquals(iterB.nextIndex(), iterA.nextIndex());
    }

    @Test
    public void testDistinct_HandlesNullsExactlyOnce() {
        Iterator<String> in = Arrays.asList("a", null, "b", null, "a").iterator();
        ObjIterator<String> r = Iterators.distinct(in);
        List<String> out = r.toList();
        assertEquals(3, out.size());
        assertEquals("a", out.get(0));
        assertEquals(null, out.get(1));
        assertEquals("b", out.get(2));
    }

    @Test
    public void testDistinctBy_NullKey() {
        Iterator<String> in = Arrays.asList("a", "bb", null, "ccc", "dd").iterator();
        ObjIterator<String> r = Iterators.distinctBy(in, s -> s == null ? null : s.length());
        List<String> out = r.toList();
        // Keys: 1, 2, null, 3 -> all distinct (4 items kept)
        assertEquals(4, out.size());
    }

    // ===================== filter / map / take/drop =====================

    @Test
    public void testFilter_PredicateMatchesNullElements() {
        Iterator<String> in = Arrays.asList("a", null, "b").iterator();
        ObjIterator<String> r = Iterators.filter(in, s -> s == null);
        List<String> out = r.toList();
        assertEquals(1, out.size());
        assertNull(out.get(0));
    }

    @Test
    public void testMap_NullIterator_Empty() {
        ObjIterator<Integer> r = Iterators.map((Iterator<String>) null, String::length);
        assertFalse(r.hasNext());
    }

    @Test
    public void testTakeWhile_StopsAtFirstFalse() {
        ObjIterator<Integer> r = Iterators.takeWhile(Arrays.asList(1, 2, 3, 1, 2).iterator(), i -> i < 3);
        assertEquals(Arrays.asList(1, 2), r.toList());
    }

    @Test
    public void testDropWhile_DropsLeadingMatching() {
        ObjIterator<Integer> r = Iterators.dropWhile(Arrays.asList(1, 2, 3, 1, 2).iterator(), i -> i < 3);
        assertEquals(Arrays.asList(3, 1, 2), r.toList());
    }

    @Test
    public void testLimit_Zero_ReturnsEmpty() {
        ObjIterator<Integer> r = Iterators.limit(Arrays.asList(1, 2, 3).iterator(), 0);
        assertFalse(r.hasNext());
    }

    @Test
    public void testLimit_GreaterThanSize_ReturnsAll() {
        ObjIterator<Integer> r = Iterators.limit(Arrays.asList(1, 2, 3).iterator(), 100);
        assertEquals(Arrays.asList(1, 2, 3), r.toList());
    }

    @Test
    public void testLimit_NegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.limit(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testConsumeOnce_AfterToList_IsEmpty() {
        ObjIterator<Integer> r = Iterators.filter(Arrays.asList(1, 2, 3).iterator(), i -> true);
        assertEquals(Arrays.asList(1, 2, 3), r.toList());
        // Re-consuming a consume-once iterator is empty.
        assertFalse(r.hasNext());
    }

    @Test
    public void testCount_NullPredicateThrows() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.count(Arrays.asList(1, 2).iterator(), null));
    }

    @Test
    public void testParallelForEachPropagatesConsumerErrors() {
        final java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean();

        final AssertionError error = assertThrows(AssertionError.class, () -> Iterators.forEach(Arrays.asList(1, 2, 3).iterator(),
                Iterators.IterateOptions.builder().offset(0).count(Long.MAX_VALUE).processThreads(2).queueSize(4).build(), value -> {
                    throw new AssertionError("consumer failed");
                }, () -> completed.set(true)));

        assertEquals("consumer failed", error.getMessage());
        assertFalse(completed.get());
    }

    @Test
    public void testIterateOptions_builderDefaults() throws Exception {
        // @Builder.Default must carry the documented defaults: offset=0, count=Long.MAX_VALUE (process all)
        final Iterators.IterateOptions opts = Iterators.IterateOptions.builder().build();
        assertEquals(0L, opts.offset());
        assertEquals(Long.MAX_VALUE, opts.count());
        assertEquals(0, opts.readThreads());
        assertEquals(0, opts.processThreads());
        assertEquals(0, opts.queueSize());

        final List<Iterator<Integer>> iters = Arrays.asList(Arrays.asList(1, 2, 3).iterator());
        final List<Integer> result = new ArrayList<>();
        Iterators.forEach(iters, opts, result::add);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testIterateOptions_rejectNegativeConcurrencySettings() {
        final Collection<Iterator<Integer>> empty = Collections.emptyList();

        assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(empty, Iterators.IterateOptions.builder().readThreads(-1).build(), value -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(empty, Iterators.IterateOptions.builder().processThreads(-1).build(), value -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(empty, Iterators.IterateOptions.builder().queueSize(-1).build(), value -> {
        }));
    }

    /** B3: {@code map} used to let the source's own (message-less) NoSuchElementException escape. */
    @Test
    public void testMap_nextPastEndThrowsNoSuchElement() {
        final ObjIterator<Integer> iter = Iterators.map(Arrays.asList(1).iterator(), x -> x + 1);
        assertEquals(2, iter.next());
        assertFalse(iter.hasNext());
        assertNotNull(assertThrows(NoSuchElementException.class, iter::next).getMessage());

        assertThrows(NoSuchElementException.class, () -> Iterators.map(Collections.<Integer> emptyIterator(), x -> x).next());
    }

    /** The two *ToSize methods read their source lazily; shrinking it must fail with the standard message. */
    @Test
    public void testToSizeIterators_sourceEmptiedAfterCreation() {
        // Emptied before anything is pulled: both iterators obtain a fresh (empty) source iterator and must report
        // the shortfall with the standard message rather than letting the source's bare exception escape.
        final List<String> emptiedBeforeUse = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final ObjIterator<String> cycleFromStart = Iterators.cycleToSize(emptiedBeforeUse, 6);
        final ObjIterator<String> repeatFromStart = Iterators.repeatElementsToSize(emptiedBeforeUse, 6);
        emptiedBeforeUse.clear();

        assertTrue(cycleFromStart.hasNext());
        assertNotNull(assertThrows(NoSuchElementException.class, cycleFromStart::next).getMessage());
        assertTrue(repeatFromStart.hasNext());
        assertNotNull(assertThrows(NoSuchElementException.class, repeatFromStart::next).getMessage());

        // cycleToSize re-reads the source once per round, so it hits the same guard mid-iteration.
        final java.util.Queue<String> src = new java.util.concurrent.ConcurrentLinkedQueue<>(Arrays.asList("a", "b", "c"));
        final ObjIterator<String> cycleIter = Iterators.cycleToSize(src, 6);
        assertEquals(Arrays.asList("a", "b", "c"), takeFrom(cycleIter, 3));
        src.clear();
        assertTrue(cycleIter.hasNext());
        assertNotNull(assertThrows(NoSuchElementException.class, cycleIter::next).getMessage());
    }

    /** D2/D3: negative settings are rejected by build(), and the message names the public property. */
    @Test
    public void testIterateOptions_builderRejectsNegativeValuesWithThePublicPropertyName() {
        assertEquals("offset", quotedNameInMessage(() -> Iterators.IterateOptions.builder().offset(-1).build()));
        assertEquals("count", quotedNameInMessage(() -> Iterators.IterateOptions.builder().count(-1).build()));
        assertEquals("readThreads", quotedNameInMessage(() -> Iterators.IterateOptions.builder().readThreads(-1).build()));
        assertEquals("processThreads", quotedNameInMessage(() -> Iterators.IterateOptions.builder().processThreads(-1).build()));
        assertEquals("queueSize", quotedNameInMessage(() -> Iterators.IterateOptions.builder().queueSize(-1).build()));

        // zero and positive values still build, and the documented defaults survive the explicit constructor
        assertEquals(0, Iterators.IterateOptions.builder().readThreads(0).build().readThreads());
        assertEquals(Long.MAX_VALUE, Iterators.IterateOptions.builder().offset(5).build().count());
        assertEquals(5L, Iterators.IterateOptions.builder().offset(5).build().offset());
    }

    /** D7: the slicing and forEach paths must report a negative offset/count the same way. */
    @Test
    public void testNegativeOffsetCountMessageIsConsistent() {
        final String fromSlice = assertThrows(IllegalArgumentException.class, () -> Iterators.skipAndLimit(Arrays.asList(1).iterator(), -1, 1)).getMessage();
        final String fromForEach = assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(Arrays.asList(1).iterator(), -1L, 1L, x -> {
        })).getMessage();
        assertEquals(fromSlice, fromForEach);
        assertTrue(fromSlice.contains("'offset'"), fromSlice);
    }

    // ================================================================================================

    /**
     * B1: seven "nothing to do" fast paths hand back a bare {@code ObjIterator.of(iter)}, and that wrapper
     * delegates {@code next()} to the source, so the source's own - usually message-less - exception escapes.
     * Normalising it to {@code ERROR_MSG_FOR_NO_SUCH_EX} was tried and deliberately reverted, so what is pinned
     * here is the part that must stay true regardless: once {@code hasNext()} is {@code false}, every one of
     * these paths raises a {@link NoSuchElementException} (never some other type), carrying the source's own
     * message rather than one of this library's.
     */
    @Test
    public void testFastPaths_nextPastEndThrowsTheSourcesOwnNoSuchElement() {
        final java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        final Map<String, Iterator<?>> fastPaths = new java.util.LinkedHashMap<>();
        fastPaths.put("skip(iter, 0)", Iterators.skip(Collections.<Integer> emptyIterator(), 0));
        fastPaths.put("limit(iter, MAX_VALUE)", Iterators.limit(Collections.<Integer> emptyIterator(), Long.MAX_VALUE));
        fastPaths.put("skipAndLimit(iter, 0, MAX_VALUE)", Iterators.skipAndLimit(Collections.<Integer> emptyIterator(), 0, Long.MAX_VALUE));
        fastPaths.put("merge(1-element collection)",
                Iterators.merge(Collections.<Iterator<? extends Integer>> singletonList(Collections.<Integer> emptyIterator()), selector));
        fastPaths.put("mergeIterables(1-element collection)",
                Iterators.mergeIterables(Collections.<Iterable<? extends Integer>> singletonList(Collections.<Integer> emptyList()), selector));
        fastPaths.put("ObjIterator.of(iter)", ObjIterator.of(Collections.<Integer> emptyIterator()));

        for (final Map.Entry<String, Iterator<?>> entry : fastPaths.entrySet()) {
            final Iterator<?> iter = entry.getValue();
            assertFalse(iter.hasNext(), entry.getKey());
            // the JDK's empty iterators are message-less, and the wrapper does not substitute one
            assertNull(assertThrows(NoSuchElementException.class, iter::next, entry.getKey()).getMessage(),
                    entry.getKey() + " must surface the source's own bare exception, not a library message");
        }

        // cycle(.., 1) hands back the source directly; drain its single element first.
        final ObjIterator<Integer> cycleOnce = Iterators.cycle(Arrays.asList(1), 1L);
        assertEquals(1, cycleOnce.next());
        assertNull(assertThrows(NoSuchElementException.class, cycleOnce::next).getMessage(), "cycle(Collection, 1)");

        final Iterable<Integer> notACollection = () -> Arrays.asList(1).iterator();
        final ObjIterator<Integer> cycleIterableOnce = Iterators.cycle(notACollection, 1L);
        assertEquals(1, cycleIterableOnce.next());
        assertNull(assertThrows(NoSuchElementException.class, cycleIterableOnce::next).getMessage(), "cycle(Iterable, 1)");
    }

    /** B1: removing the guard must not change what a non-exhausted iterator yields, including {@code null}. */
    @Test
    public void testObjIteratorOf_stillYieldsEveryElementIncludingNulls() {
        assertEquals(Arrays.asList("a", null, "b"), drainToList(ObjIterator.of(Arrays.asList("a", null, "b").iterator())));
        assertEquals(Arrays.asList(1, 2, 3), drainToList(Iterators.skip(Arrays.asList(1, 2, 3).iterator(), 0)));
        assertEquals(Arrays.asList(1, 2, 3), drainToList(Iterators.limit(Arrays.asList(1, 2, 3).iterator(), Long.MAX_VALUE)));
        assertFalse(ObjIterator.of((Iterator<String>) null).hasNext());
    }

    /** O3: an exhausted adapter must not keep the last element (or the last mapped array) alive. */
    @Test
    public void testExhaustedAdaptersDoNotRetainTheLastElement() throws Exception {
        final String last = new String("last"); // NOSONAR - a distinct instance is the point of the test

        final ObjIterator<String> filtered = Iterators.filter(Arrays.asList("a", last).iterator(), x -> true);
        assertEquals(Arrays.asList("a", last), drainToList(filtered));
        assertNoFieldRetains(filtered, last);

        final ObjIterator<String> taken = Iterators.takeWhile(Arrays.asList("a", last).iterator(), x -> true);
        assertEquals(Arrays.asList("a", last), drainToList(taken));
        assertNoFieldRetains(taken, last);

        final ObjIterator<String> takenInclusive = Iterators.takeWhileInclusive(Arrays.asList("a", last).iterator(), x -> true);
        assertEquals(Arrays.asList("a", last), drainToList(takenInclusive));
        assertNoFieldRetains(takenInclusive, last);

        final ObjIterator<String> flat = Iterators.flatmap(Arrays.asList("x").iterator(), s -> new String[] { s, last });
        assertEquals(Arrays.asList("x", last), drainToList(flat));
        assertNoFieldRetains(flat, last);
    }

    /** D4: the shared default instance must equal a freshly-built one, and be what a {@code null} options means. */
    @Test
    public void testIterateOptionsDefault() throws Exception {
        assertEquals(Iterators.IterateOptions.builder().build(), Iterators.IterateOptions.DEFAULT);
        assertEquals(0, Iterators.IterateOptions.DEFAULT.offset());
        assertEquals(Long.MAX_VALUE, Iterators.IterateOptions.DEFAULT.count());
        assertEquals(0, Iterators.IterateOptions.DEFAULT.readThreads());
        assertEquals(0, Iterators.IterateOptions.DEFAULT.processThreads());
        assertEquals(0, Iterators.IterateOptions.DEFAULT.queueSize());

        final List<Integer> viaNull = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), (Iterators.IterateOptions) null, viaNull::add);

        final List<Integer> viaDefault = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), Iterators.IterateOptions.DEFAULT, viaDefault::add);

        assertEquals(viaDefault, viaNull);
        assertEquals(Arrays.asList(1, 2, 3), viaNull);
    }

    /**
     * J2: these three compare with {@code Objects.equals}, so array elements match by identity, not content.
     * Undocumented until now, and easy to get wrong after {@code N.equals(Object, Object)} was narrowed.
     */
    @Test
    public void testSearchMethods_compareArrayElementsByIdentity() {
        final int[] value = { 1, 2 };
        final int[] equalButDistinct = { 1, 2 };

        assertEquals(0, Iterators.frequency(Arrays.asList((Object) value).iterator(), equalButDistinct));
        assertEquals(1, Iterators.frequency(Arrays.asList((Object) value).iterator(), value));

        assertEquals(-1, Iterators.indexOf(Arrays.asList((Object) value).iterator(), equalButDistinct));
        assertEquals(0, Iterators.indexOf(Arrays.asList((Object) value).iterator(), value));

        assertFalse(Iterators.equalsInOrder(Arrays.asList((Object) value).iterator(), Arrays.asList((Object) equalButDistinct).iterator()));
        assertTrue(Iterators.equalsInOrder(Arrays.asList((Object) value).iterator(), Arrays.asList((Object) value).iterator()));
    }

    /** J4: {@code takeWhile} pulls the stopping element in order to test it, and then discards it. */
    @Test
    public void testTakeWhile_consumesAndDiscardsTheStoppingElement() {
        final Iterator<Integer> source = Arrays.asList(1, 2, 3, 4, 5).iterator();
        assertEquals(Arrays.asList(1, 2), drainToList(Iterators.takeWhile(source, n -> n < 3)));
        assertEquals(Arrays.asList(4, 5), drainToList(source), "3 was consumed while testing it and is not emitted");

        // takeWhileInclusive is the variant that emits it instead.
        final Iterator<Integer> source2 = Arrays.asList(1, 2, 3, 4, 5).iterator();
        assertEquals(Arrays.asList(1, 2, 3), drainToList(Iterators.takeWhileInclusive(source2, n -> n < 3)));
        assertEquals(Arrays.asList(4, 5), drainToList(source2));
    }

    @Test
    public void reviewFixes20260906_sourceCountComesFromTheIteratorNotFromIsEmpty() {
        // merge(Collection, ..) was explicitly changed to decide the source count by iterating; concat,
        // concatIterables and forEach still short-circuited on Collection.isEmpty() and dropped EVERY source.
        final java.util.Collection<java.util.Iterator<Integer>> lying = new ReviewFixes20260906LyingSizeCollection<>(
                java.util.Arrays.asList(CommonUtil.asList(0).iterator(), CommonUtil.asList(1).iterator()), 0);
        assertTrue(lying.isEmpty());
        assertEquals(CommonUtil.asList(0, 1), CommonUtil.toList(Iterators.concat(lying)));

        final java.util.Collection<Iterable<Integer>> lyingIterables = new ReviewFixes20260906LyingSizeCollection<>(
                java.util.Arrays.<Iterable<Integer>> asList(CommonUtil.asList(0), CommonUtil.asList(1)), 0);
        assertEquals(CommonUtil.asList(0, 1), CommonUtil.toList(Iterators.concatIterables(lyingIterables)));

        final java.util.List<Integer> hits = new java.util.ArrayList<>();
        Iterators.forEach(
                new ReviewFixes20260906LyingSizeCollection<>(java.util.Arrays.asList(CommonUtil.asList(0).iterator(), CommonUtil.asList(1).iterator()), 0),
                hits::add);
        assertEquals(CommonUtil.asList(0, 1), hits);

        // A genuinely empty collection, and null, still short-circuit.
        assertFalse(Iterators.concat(new java.util.ArrayList<java.util.Iterator<Integer>>()).hasNext());
        assertFalse(Iterators.concat((java.util.Collection<java.util.Iterator<Integer>>) null).hasNext());
        assertFalse(Iterators.concatIterables(new java.util.ArrayList<Iterable<Integer>>()).hasNext());
        assertFalse(Iterators.concatIterables((java.util.Collection<Iterable<Integer>>) null).hasNext());

        final java.util.List<Integer> none = new java.util.ArrayList<>();
        Iterators.forEach(new java.util.ArrayList<java.util.Iterator<Integer>>(), none::add);
        assertTrue(none.isEmpty());

        // Honest collections are unaffected.
        assertEquals(CommonUtil.asList(0, 1),
                CommonUtil.toList(Iterators.concat(java.util.Arrays.asList(CommonUtil.asList(0).iterator(), CommonUtil.asList(1).iterator()))));
    }

    @Test
    public void reviewFixes20260906_cycleProbesTheSourceEagerly() {
        // The Lazy Evaluation bullet named only repeatElements(Iterable, long) as calling hasNext() at
        // construction. Both cycle(Iterable) overloads do the same for a non-Collection source, which is exactly
        // the case the bullet cares about: an I/O-backed Iterable performs one read before the caller pulls.
        final ReviewFixes20260906ProbingIterable<String> forCycle = new ReviewFixes20260906ProbingIterable<>(CommonUtil.asList("a", "b"));
        Iterators.cycle(forCycle);
        assertEquals(1, forCycle.hasNextCalls);

        final ReviewFixes20260906ProbingIterable<String> forCycleRounds = new ReviewFixes20260906ProbingIterable<>(CommonUtil.asList("a", "b"));
        Iterators.cycle(forCycleRounds, 3L);
        assertEquals(1, forCycleRounds.hasNextCalls);

        final ReviewFixes20260906ProbingIterable<String> forRepeat = new ReviewFixes20260906ProbingIterable<>(CommonUtil.asList("a", "b"));
        Iterators.repeatElements(forRepeat, 2L);
        assertEquals(1, forRepeat.hasNextCalls);

        // Control: an adapter that only obtains the iterator does not probe it.
        final ReviewFixes20260906ProbingIterable<String> forMap = new ReviewFixes20260906ProbingIterable<>(CommonUtil.asList("a", "b"));
        Iterators.map(forMap, s -> s);
        assertEquals(0, forMap.hasNextCalls);
    }

    @Test
    public void reviewFixes20260906_flatMapSkipsANullFromTheMapper() {
        // A null mapped result is treated as an empty source and skipped - on all four overloads, none of whose
        // javadocs said so. Only flatMap(Iterator, Function) was pinned before.
        final java.util.function.Function<String, Iterable<String>> nullingIterable = s -> "a".equals(s) ? null : CommonUtil.asList(s);
        final java.util.function.Function<String, String[]> nullingArray = s -> "a".equals(s) ? null : new String[] { s };

        assertEquals(CommonUtil.asList("b"), CommonUtil.toList(Iterators.flatMap(CommonUtil.asList("a", "b").iterator(), nullingIterable)));
        assertEquals(CommonUtil.asList("b"), CommonUtil.toList(Iterators.flatMap((Iterable<String>) CommonUtil.asList("a", "b"), nullingIterable)));
        assertEquals(CommonUtil.asList("b"), CommonUtil.toList(Iterators.flatmap(CommonUtil.asList("a", "b").iterator(), nullingArray)));
        assertEquals(CommonUtil.asList("b"), CommonUtil.toList(Iterators.flatmap((Iterable<String>) CommonUtil.asList("a", "b"), nullingArray)));

        // An all-null mapper yields nothing rather than throwing, and an empty result is skipped the same way.
        final java.util.function.Function<String, Iterable<String>> allNull = s -> null;
        assertTrue(CommonUtil.toList(Iterators.flatMap(CommonUtil.asList("a", "b").iterator(), allNull)).isEmpty());
        final java.util.function.Function<String, Iterable<String>> emptying = s -> "a".equals(s) ? Collections.<String> emptyList() : CommonUtil.asList(s);
        assertEquals(CommonUtil.asList("b"), CommonUtil.toList(Iterators.flatMap(CommonUtil.asList("a", "b").iterator(), emptying)));

        // Control: a null mapper itself is still rejected outright.
        assertThrows(IllegalArgumentException.class,
                () -> Iterators.flatMap(CommonUtil.asList("a").iterator(), (java.util.function.Function<String, Iterable<String>>) null));
    }

    @Test
    public void reviewFixes20260906_cycleVarargsSnapshotsTheArray() {
        // cycle(T...) clones its varargs array, so later writes to the caller's array are not observed. It was the
        // one cycle overload whose javadoc did not say which way it goes.
        final String[] src = { "a", "b" };
        final ObjIterator<String> snapshot = Iterators.cycle(src);
        src[0] = "z";
        assertEquals("a", snapshot.next());
        assertEquals("b", snapshot.next());
        assertEquals("a", snapshot.next());

        // Control: cycle(Collection) is documented as a live view and still is.
        final List<String> live = new ArrayList<>(CommonUtil.asList("a", "b"));
        final ObjIterator<String> view = Iterators.cycle(live);
        assertEquals("a", view.next());
        live.set(1, "Z");
        assertEquals("Z", view.next());
    }

    /**
     * {@code frequency}'s "Comparison" note: elements are compared with
     * {@link java.util.Objects#equals(Object, Object)}, so array elements match by identity, not by content. The
     * note used to illustrate the point with a call to {@code iterOf(..)}, which exists nowhere in the tree.
     */
    @Test
    public void testFrequency_arrayElementsMatchByIdentityNotContent() {
        assertEquals(0, Iterators.frequency(ObjIterator.of(new int[][] { { 1, 2 } }), new int[] { 1, 2 }));

        final int[] sought = { 1, 2 };
        assertEquals(1, Iterators.frequency(ObjIterator.of(new int[][] { sought }), sought));

        // the workaround the same paragraph recommends
        assertEquals(1, Iterators.count(ObjIterator.of(new int[][] { { 1, 2 } }), value -> N.deepEquals(value, new int[] { 1, 2 })));
    }

    /**
     * The Memory note now carried by {@code distinct(Iterable)} and {@code distinctBy(Iterable, Function)}: every
     * distinct element - or key - seen so far is retained for the lifetime of the returned iterator, so a
     * duplicate far from its first occurrence is still dropped. Both overloads delegate to the same retaining
     * implementation as their {@code Iterator} siblings.
     */
    @Test
    public void testDistinctIterable_retainsEveryElementSeenForTheLifetimeOfTheIterator() {
        final List<Integer> source = new ArrayList<>();

        for (int i = 0; i < 500; i++) {
            source.add(i);
        }

        source.add(0);
        source.add(499);

        final List<Integer> distinct = Iterators.distinct(source).toList();
        assertEquals(500, distinct.size());
        assertEquals(Integer.valueOf(0), distinct.get(0));
        assertEquals(Integer.valueOf(499), distinct.get(499));

        final List<String> words = Arrays.asList("alpha", "beta", "gamma", "aardvark", "beetle");
        assertEquals(Arrays.asList("alpha", "beta", "gamma"), Iterators.distinctBy(words, s -> s.charAt(0)).toList());
    }

    /**
     * All four {@code unzip}/{@code unzip3} overloads - and {@code cycle(Iterable)} - are null-tolerant and return
     * an empty result, while a {@code null} unzip function is still rejected. Their javadoc now says so.
     */
    @Test
    public void testUnzipAndUnzip3_NullSourceReturnsAnEmptyResult() {
        final BiConsumer<String, Pair<String, Integer>> unzip = (str, pair) -> {
            pair.setLeft(str);
            pair.setRight(str.length());
        };

        assertFalse(Iterators.unzip((Iterator<String>) null, unzip).hasNext());
        assertFalse(Iterators.unzip((Iterable<String>) null, unzip).hasNext());

        final BiConsumer<String, Triple<String, Integer, Boolean>> unzip3 = (str, triple) -> {
            triple.setLeft(str);
            triple.setMiddle(str.length());
            triple.setRight(true);
        };

        assertFalse(Iterators.unzip3((Iterator<String>) null, unzip3).hasNext());
        assertFalse(Iterators.unzip3((Iterable<String>) null, unzip3).hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.unzip((Iterator<String>) null, (BiConsumer<String, Pair<String, Integer>>) null));
        assertFalse(Iterators.cycle((Iterable<String>) null).hasNext());
    }
}
