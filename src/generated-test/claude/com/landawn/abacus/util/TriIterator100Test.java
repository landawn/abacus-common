package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

/**
 * Unit tests for TriIterator class
 */
public class TriIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        TriIterator<String, Integer, Double> iter = TriIterator.empty();

        assertFalse(iter.hasNext());

        try {
            iter.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    @Test
    public void testGenerateWithConsumer() {
        AtomicInteger counter = new AtomicInteger(0);
        Consumer<Triple<Integer, String, Double>> output = triple -> {
            int val = counter.getAndIncrement();
            triple.set(val, "str" + val, val * 1.1);
        };

        TriIterator<Integer, String, Double> iter = TriIterator.generate(output);

        // Test first few elements
        assertTrue(iter.hasNext());
        Triple<Integer, String, Double> first = iter.next();
        assertEquals(Integer.valueOf(0), first.left());
        assertEquals("str0", first.middle());
        assertEquals(0.0, first.right(), 0.001);

        Triple<Integer, String, Double> second = iter.next();
        assertEquals(Integer.valueOf(1), second.left());
        assertEquals("str1", second.middle());
        assertEquals(1.1, second.right(), 0.001);
    }

    @Test
    public void testGenerateWithBooleanSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        Consumer<Triple<Integer, String, Double>> output = triple -> {
            int val = counter.getAndIncrement();
            triple.set(val, "str" + val, val * 2.0);
        };

        TriIterator<Integer, String, Double> iter = TriIterator.generate(hasNext, output);

        List<Triple<Integer, String, Double>> result = iter.toList();
        assertEquals(3, result.size());

        assertEquals(Integer.valueOf(0), result.get(0).left());
        assertEquals("str0", result.get(0).middle());
        assertEquals(0.0, result.get(0).right(), 0.001);

        assertEquals(Integer.valueOf(2), result.get(2).left());
        assertEquals("str2", result.get(2).middle());
        assertEquals(4.0, result.get(2).right(), 0.001);
    }

    @Test
    public void testGenerateWithIndexRange() {
        IntObjConsumer<Triple<String, Integer, Boolean>> output = (index, triple) -> {
            triple.set("idx" + index, index * 10, index % 2 == 0);
        };

        TriIterator<String, Integer, Boolean> iter = TriIterator.generate(1, 4, output);

        List<Triple<String, Integer, Boolean>> result = iter.toList();
        assertEquals(3, result.size());

        assertEquals("idx1", result.get(0).left());
        assertEquals(Integer.valueOf(10), result.get(0).middle());
        assertEquals(Boolean.FALSE, result.get(0).right());

        assertEquals("idx3", result.get(2).left());
        assertEquals(Integer.valueOf(30), result.get(2).middle());
        assertEquals(Boolean.FALSE, result.get(2).right());
    }

    @Test
    public void testZipArrays() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> result = iter.toList();
        assertEquals(3, result.size());

        assertEquals("a", result.get(0).left());
        assertEquals(Integer.valueOf(1), result.get(0).middle());
        assertEquals(1.1, result.get(0).right(), 0.001);

        assertEquals("c", result.get(2).left());
        assertEquals(Integer.valueOf(3), result.get(2).middle());
        assertEquals(3.3, result.get(2).right(), 0.001);
    }

    @Test
    public void testZipArraysDifferentLengths() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2, 3, 4 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> result = iter.toList();
        assertEquals(2, result.size()); // Shortest array length
    }

    @Test
    public void testZipArraysWithDefaults() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3, "default", -1, -1.0);

        List<Triple<String, Integer, Double>> result = iter.toList();
        assertEquals(3, result.size()); // Longest array length

        assertEquals("a", result.get(0).left());
        assertEquals(Integer.valueOf(1), result.get(0).middle());
        assertEquals(1.1, result.get(0).right(), 0.001);

        assertEquals("default", result.get(2).left());
        assertEquals(Integer.valueOf(3), result.get(2).middle());
        assertEquals(-1.0, result.get(2).right(), 0.001);
    }

    @Test
    public void testZipIterables() {
        List<String> list1 = Arrays.asList("x", "y", "z");
        Set<Integer> set2 = new HashSet<>(Arrays.asList(10, 20, 30));
        List<Boolean> list3 = Arrays.asList(true, false, true);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(list1, set2, list3);

        assertTrue(iter.hasNext());
        // Note: Set iteration order may vary, so we just check that elements exist
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertTrue(count <= 3); // Limited by shortest collection
    }

    @Test
    public void testZipIterators() {
        {
            Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
            Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
            Iterator<Double> iter3 = Arrays.asList(1.1, 2.2, 3.3, 4.4).iterator();

            TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3);

            List<Triple<String, Integer, Double>> result = triIter.toList();
            assertEquals(2, result.size()); // Limited by shortest iterator
        }
        {
            Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
            Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
            Iterator<Double> iter3 = Arrays.asList(1.1, 2.2, 3.3, 4.4).iterator();

            TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3, "X", 0, 0.0);

            List<String> result = new ArrayList<>();
            while (triIter.hasNext()) {
                Triple<String, Integer, Double> triple = triIter.next();
                result.add(triple.left() + ":" + triple.middle() + ":" + triple.right());
            }

            assertEquals(4, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:0:3.3", result.get(2));
            assertEquals("X:0:4.4", result.get(3));
        }
        {
            Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
            Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
            Iterator<Double> iter3 = Arrays.asList(1.1, 2.2, 3.3, 4.4).iterator();

            TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3, "X", 0, 0.0);

            List<String> result = new ArrayList<>();

            triIter.forEachRemaining(triple -> {
                result.add(triple.left() + ":" + triple.middle() + ":" + triple.right());
            });

            assertEquals(4, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:0:3.3", result.get(2));
            assertEquals("X:0:4.4", result.get(3));
        }
        {
            Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
            Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
            Iterator<Double> iter3 = Arrays.asList(1.1, 2.2, 3.3, 4.4).iterator();

            TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3, "X", 0, 0.0);

            List<String> result = new ArrayList<>();

            triIter.forEachRemaining((a, b, c) -> {
                result.add(a + ":" + b + ":" + c);
            });

            assertEquals(4, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:0:3.3", result.get(2));
            assertEquals("X:0:4.4", result.get(3));
        }
        {
            Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
            Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
            Iterator<Double> iter3 = Arrays.asList(1.1, 2.2, 3.3, 4.4).iterator();

            TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3, "X", 0, 0.0);

            List<String> result = new ArrayList<>();

            triIter.foreachRemaining((a, b, c) -> {
                result.add(a + ":" + b + ":" + c);
            });

            assertEquals(4, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:0:3.3", result.get(2));
            assertEquals("X:0:4.4", result.get(3));
        }
        {
            Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
            Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
            Iterator<Double> iter3 = Arrays.asList(1.1, 2.2, 3.3, 4.4).iterator();

            Iterator<String> triIter = TriIterator.zip(iter1, iter2, iter3, "X", 0, 0.0).map((a, b, c) -> a + ":" + b + ":" + c);

            List<String> result = new ArrayList<>();

            while (triIter.hasNext()) {
                result.add(triIter.next());
            }

            assertEquals(4, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:0:3.3", result.get(2));
            assertEquals("X:0:4.4", result.get(3));
        }
    }

    @Test
    public void testZipWithNullIterators() {
        Iterator<String> iter1 = null;
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Double> iter3 = Arrays.asList(1.1, 2.2).iterator();

        TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3);

        assertFalse(triIter.hasNext());
    }

    @Test
    public void testUnzipIterable() {
        List<String> source = Arrays.asList("a:1:x", "b:2:y", "c:3:z");

        BiConsumer<String, Triple<String, Integer, String>> unzipFunc = (str, triple) -> {
            String[] parts = str.split(":");
            triple.set(parts[0], Integer.parseInt(parts[1]), parts[2]);
        };

        TriIterator<String, Integer, String> iter = TriIterator.unzip(source, unzipFunc);

        List<Triple<String, Integer, String>> result = iter.toList();
        assertEquals(3, result.size());

        assertEquals("a", result.get(0).left());
        assertEquals(Integer.valueOf(1), result.get(0).middle());
        assertEquals("x", result.get(0).right());
    }

    @Test
    public void testForEachRemaining() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<String> lefts = new ArrayList<>();
        List<Integer> middles = new ArrayList<>();
        List<Double> rights = new ArrayList<>();

        TriConsumer<String, Integer, Double> action = (a, b, c) -> {
            lefts.add(a);
            middles.add(b);
            rights.add(c);
        };

        iter.forEachRemaining(action);

        assertEquals(Arrays.asList("a", "b", "c"), lefts);
        assertEquals(Arrays.asList(1, 2, 3), middles);
        assertEquals(Arrays.asList(1.1, 2.2, 3.3), rights);
    }

    @Test
    public void testSkip() {
        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).skip(2);

            List<Triple<String, Integer, Double>> result = iter.toList();
            assertEquals(3, result.size());

            assertEquals("c", result.get(0).left());
            assertEquals(Integer.valueOf(3), result.get(0).middle());
            assertEquals(3.3, result.get(0).right(), 0.001);
        }

        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).skip(2);

            List<String> result = new ArrayList<>();
            while (iter.hasNext()) {
                Triple<String, Integer, Double> triple = iter.next();
                result.add(triple.left() + ":" + triple.middle() + ":" + triple.right());
            }
            assertEquals(3, result.size());
            assertEquals("c:3:3.3", result.get(0));
            assertEquals("d:4:4.4", result.get(1));
            assertEquals("e:5:5.5", result.get(2));
        }

        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).skip(2);

            List<String> result = new ArrayList<>();

            iter.forEachRemaining((a, b, c) -> {
                result.add(a + ":" + b + ":" + c);
            });

            assertEquals(3, result.size());
            assertEquals("c:3:3.3", result.get(0));
            assertEquals("d:4:4.4", result.get(1));
            assertEquals("e:5:5.5", result.get(2));
        }

        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).skip(2);

            List<String> result = new ArrayList<>();

            iter.foreachRemaining((a, b, c) -> {
                result.add(a + ":" + b + ":" + c);
            });

            assertEquals(3, result.size());
            assertEquals("c:3:3.3", result.get(0));
            assertEquals("d:4:4.4", result.get(1));
            assertEquals("e:5:5.5", result.get(2));
        }

        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            Iterator<String> iter = TriIterator.zip(arr1, arr2, arr3).skip(2).map((a, b, c) -> a + ":" + b + ":" + c);

            List<String> result = new ArrayList<>();

            iter.forEachRemaining(e -> {
                result.add(e);
            });

            assertEquals(3, result.size());
            assertEquals("c:3:3.3", result.get(0));
            assertEquals("d:4:4.4", result.get(1));
            assertEquals("e:5:5.5", result.get(2));
        }

    }

    @Test
    public void testSkipMoreThanAvailable() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2 };
        Double[] arr3 = { 1.1, 2.2 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).skip(5);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit() {
        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).limit(3);

            List<Triple<String, Integer, Double>> result = iter.toList();
            assertEquals(3, result.size());

            assertEquals("a", result.get(0).left());
            assertEquals("c", result.get(2).left());
        }

        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).limit(3);

            List<String> result = new ArrayList<>();
            while (iter.hasNext()) {
                Triple<String, Integer, Double> triple = iter.next();
                result.add(triple.left() + ":" + triple.middle() + ":" + triple.right());
            }
            assertEquals(3, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:3:3.3", result.get(2));
        }

        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).limit(3);

            List<String> result = new ArrayList<>();

            iter.forEachRemaining((a, b, c) -> {
                result.add(a + ":" + b + ":" + c);
            });

            assertEquals(3, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:3:3.3", result.get(2));
        }
        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).limit(3);

            List<String> result = new ArrayList<>();

            iter.foreachRemaining((a, b, c) -> {
                result.add(a + ":" + b + ":" + c);
            });

            assertEquals(3, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:3:3.3", result.get(2));
        }

        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            Iterator<String> iter = TriIterator.zip(arr1, arr2, arr3).limit(3).map((a, b, c) -> a + ":" + b + ":" + c);

            List<String> result = new ArrayList<>();

            iter.forEachRemaining(e -> {
                result.add(e);
            });

            assertEquals(3, result.size());
            assertEquals("a:1:1.1", result.get(0));
            assertEquals("b:2:2.2", result.get(1));
            assertEquals("c:3:3.3", result.get(2));
        }
    }

    @Test
    public void testLimitZero() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).limit(0);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter() {
        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriPredicate<String, Integer, Double> predicate = (s, i, d) -> i % 2 == 0;

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).filter(predicate);

            List<Triple<String, Integer, Double>> result = iter.toList();
            assertEquals(2, result.size());

            assertEquals("b", result.get(0).left());
            assertEquals(Integer.valueOf(2), result.get(0).middle());

            assertEquals("d", result.get(1).left());
            assertEquals(Integer.valueOf(4), result.get(1).middle());
        }
        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriPredicate<String, Integer, Double> predicate = (s, i, d) -> i % 2 == 0;

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).filter(predicate);

            List<String> result = new ArrayList<>();
            while (iter.hasNext()) {
                Triple<String, Integer, Double> triple = iter.next();
                result.add(triple.left() + ":" + triple.middle() + ":" + triple.right());
            }
            assertEquals(2, result.size());
            assertEquals("b:2:2.2", result.get(0));
            assertEquals("d:4:4.4", result.get(1));
        }

        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriPredicate<String, Integer, Double> predicate = (s, i, d) -> i % 2 == 0;

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).filter(predicate);

            List<String> result = new ArrayList<>();

            iter.forEachRemaining((a, b, c) -> {
                result.add(a + ":" + b + ":" + c);
            });

            assertEquals(2, result.size());
            assertEquals("b:2:2.2", result.get(0));
            assertEquals("d:4:4.4", result.get(1));
        }
        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriPredicate<String, Integer, Double> predicate = (s, i, d) -> i % 2 == 0;

            TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).filter(predicate);

            List<String> result = new ArrayList<>();

            iter.foreachRemaining((a, b, c) -> {
                result.add(a + ":" + b + ":" + c);
            });

            assertEquals(2, result.size());
            assertEquals("b:2:2.2", result.get(0));
            assertEquals("d:4:4.4", result.get(1));
        }
        {
            String[] arr1 = { "a", "b", "c", "d", "e" };
            Integer[] arr2 = { 1, 2, 3, 4, 5 };
            Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

            TriPredicate<String, Integer, Double> predicate = (s, i, d) -> i % 2 == 0;

            Iterator<String> iter = TriIterator.zip(arr1, arr2, arr3).filter(predicate).map((a, b, c) -> a + ":" + b + ":" + c);

            List<String> result = new ArrayList<>();

            iter.forEachRemaining(e -> {
                result.add(e);
            });

            assertEquals(2, result.size());
            assertEquals("b:2:2.2", result.get(0));
            assertEquals("d:4:4.4", result.get(1));
        }

    }

    @Test
    public void testMap() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriFunction<String, Integer, Double, String> mapper = (s, i, d) -> s + i + ":" + d;

        ObjIterator<String> mappedIter = TriIterator.zip(arr1, arr2, arr3).map(mapper);

        List<String> result = new ArrayList<>();
        mappedIter.forEachRemaining(result::add);

        assertEquals(3, result.size());
        assertEquals("a1:1.1", result.get(0));
        assertEquals("b2:2.2", result.get(1));
        assertEquals("c3:3.3", result.get(2));
    }

    @Test
    public void testFirst() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        Optional<Triple<String, Integer, Double>> first = iter.first();
        assertTrue(first.isPresent());
        assertEquals("a", first.get().left());
        assertEquals(Integer.valueOf(1), first.get().middle());
        assertEquals(1.1, first.get().right(), 0.001);
    }

    @Test
    public void testFirstEmpty() {
        TriIterator<String, Integer, Double> iter = TriIterator.empty();

        Optional<Triple<String, Integer, Double>> first = iter.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        Optional<Triple<String, Integer, Double>> last = iter.last();
        assertTrue(last.isPresent());
        assertEquals("c", last.get().left());
        assertEquals(Integer.valueOf(3), last.get().middle());
        assertEquals(3.3, last.get().right(), 0.001);
    }

    @Test
    public void testLastEmpty() {
        TriIterator<String, Integer, Double> iter = TriIterator.empty();

        Optional<Triple<String, Integer, Double>> last = iter.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testStream() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriFunction<String, Integer, Double, String> mapper = (s, i, d) -> s + i;

        Stream<String> stream = TriIterator.zip(arr1, arr2, arr3).stream(mapper);

        List<String> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals("a1", result.get(0));
        assertEquals("b2", result.get(1));
        assertEquals("c3", result.get(2));
    }

    @Test
    public void testToArray() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        Triple<String, Integer, Double>[] array = iter.toArray();
        assertEquals(3, array.length);
        assertEquals("a", array[0].left());
        assertEquals("c", array[2].left());
    }

    @Test
    public void testToList() {
        String[] arr1 = { "x", "y", "z" };
        Integer[] arr2 = { 10, 20, 30 };
        Double[] arr3 = { 10.1, 20.2, 30.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> list = iter.toList();
        assertEquals(3, list.size());
        assertEquals("x", list.get(0).left());
        assertEquals(Integer.valueOf(20), list.get(1).middle());
        assertEquals(30.3, list.get(2).right(), 0.001);
    }

    @Test
    public void testToMultiList() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        Triple<List<String>, List<Integer>, List<Double>> multiList = iter.toMultiList(ArrayList::new);

        assertEquals(Arrays.asList("a", "b", "c"), multiList.left());
        assertEquals(Arrays.asList(1, 2, 3), multiList.middle());
        assertEquals(3, multiList.right().size());
        assertEquals(1.1, multiList.right().get(0), 0.001);
    }

    @Test
    public void testToMultiSet() {
        String[] arr1 = { "a", "b", "c", "a" };
        Integer[] arr2 = { 1, 2, 3, 1 };
        Double[] arr3 = { 1.1, 2.2, 3.3, 1.1 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        Triple<Set<String>, Set<Integer>, Set<Double>> multiSet = iter.toMultiSet(HashSet::new);

        assertEquals(3, multiSet.left().size()); // "a" appears twice, but set contains unique values
        assertTrue(multiSet.left().contains("a"));
        assertTrue(multiSet.left().contains("b"));
        assertTrue(multiSet.left().contains("c"));

        assertEquals(3, multiSet.middle().size());
        assertEquals(3, multiSet.right().size());
    }

    @Test
    public void testCombinedOperations() {
        // Test chaining multiple operations
        String[] arr1 = { "a", "b", "c", "d", "e", "f" };
        Integer[] arr2 = { 1, 2, 3, 4, 5, 6 };
        Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6 };

        TriPredicate<String, Integer, Double> predicate = (s, i, d) -> i % 2 == 0;

        List<Triple<String, Integer, Double>> result = TriIterator.zip(arr1, arr2, arr3)
                .skip(1) // Skip first element
                .limit(4) // Take next 4 elements
                .filter(predicate) // Keep only even integers
                .toList();

        assertEquals(2, result.size());
        assertEquals("b", result.get(0).left());
        assertEquals(Integer.valueOf(2), result.get(0).middle());
        assertEquals("d", result.get(1).left());
        assertEquals(Integer.valueOf(4), result.get(1).middle());
    }

    @Test
    public void testSkipNegative() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().skip(-1));
    }

    @Test
    public void testLimitNegative() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().limit(-1));
    }

    @Test
    public void testGenerateInvalidIndexRange() {
        IntObjConsumer<Triple<String, Integer, Boolean>> output = (index, triple) -> {
        };
        assertThrows(IndexOutOfBoundsException.class, () -> TriIterator.generate(5, 2, output)); // fromIndex > toIndex
    }
}
