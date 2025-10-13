package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class TriIterator102Test extends TestBase {

    @Test
    public void testEmpty() {
        TriIterator<String, Integer, Double> iter = TriIterator.empty();

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());

        List<String> results = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> results.add(a + "," + b + "," + c));
        assertTrue(results.isEmpty());

        ObjIterator<String> mapped = iter.map((a, b, c) -> a + b + c);
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testGenerateWithConsumer() {
        List<Triple<Integer, String, Double>> results = new ArrayList<>();
        Consumer<Triple<Integer, String, Double>> output = triple -> {
            triple.set(results.size(), "item" + results.size(), results.size() * 1.5);
        };

        TriIterator<Integer, String, Double> iter = TriIterator.generate(output);

        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            Triple<Integer, String, Double> triple = iter.next();
            assertEquals(i, triple.left());
            assertEquals("item" + i, triple.middle());
            assertEquals(i * 1.5, triple.right());
            results.add(triple);
        }
    }

    @Test
    public void testGenerateWithBooleanSupplierAndConsumer() {
        MutableInt counter = MutableInt.of(0);
        BooleanSupplier hasNext = () -> counter.value() < 3;
        Consumer<Triple<Integer, String, Boolean>> output = triple -> {
            triple.set(counter.value(), "value" + counter.value(), counter.value() % 2 == 0);
            counter.increment();
        };

        TriIterator<Integer, String, Boolean> iter = TriIterator.generate(hasNext, output);

        List<Triple<Integer, String, Boolean>> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(3, results.size());
        assertEquals(0, results.get(0).left());
        assertEquals("value0", results.get(0).middle());
        assertTrue(results.get(0).right());
        assertEquals(1, results.get(1).left());
        assertEquals("value1", results.get(1).middle());
        assertFalse(results.get(1).right());

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testGenerateWithIndexRange() {
        IntObjConsumer<Triple<String, Integer, Boolean>> output = (index, triple) -> {
            triple.set("idx" + index, index * 10, index % 2 == 0);
        };

        TriIterator<String, Integer, Boolean> iter = TriIterator.generate(2, 5, output);

        List<Triple<String, Integer, Boolean>> results = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> results.add(Triple.of(a, b, c)));

        assertEquals(3, results.size());
        assertEquals("idx2", results.get(0).left());
        assertEquals(20, results.get(0).middle());
        assertTrue(results.get(0).right());
        assertEquals("idx3", results.get(1).left());
        assertEquals(30, results.get(1).middle());
        assertFalse(results.get(1).right());
        assertEquals("idx4", results.get(2).left());
        assertEquals(40, results.get(2).middle());
        assertTrue(results.get(2).right());
    }

    @Test
    public void testGenerateWithInvalidIndexRange() {
        IntObjConsumer<Triple<String, Integer, Boolean>> output = (index, triple) -> {
        };

        assertThrows(IndexOutOfBoundsException.class, () -> TriIterator.generate(5, 2, output));
    }

    @Test
    public void testZipArrays() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> results = iter.toList();
        assertEquals(3, results.size());
        assertEquals("a", results.get(0).left());
        assertEquals(1, results.get(0).middle());
        assertEquals(1.1, results.get(0).right());
        assertEquals("c", results.get(2).left());
        assertEquals(3, results.get(2).middle());
        assertEquals(3.3, results.get(2).right());
    }

    @Test
    public void testZipArraysWithDifferentLengths() {
        String[] arr1 = { "a", "b", "c", "d" };
        Integer[] arr2 = { 1, 2 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> results = iter.toList();
        assertEquals(2, results.size());
    }

    @Test
    public void testZipArraysWithDefaults() {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2, 3, 4 };
        Double[] arr3 = { 1.1 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3, "default", -1, 0.0);

        List<Triple<String, Integer, Double>> results = iter.toList();
        assertEquals(4, results.size());
        assertEquals("default", results.get(2).left());
        assertEquals(3, results.get(2).middle());
        assertEquals(0.0, results.get(2).right());
    }

    @Test
    public void testZipIterables() {
        List<String> list1 = Arrays.asList("x", "y", "z");
        List<Integer> list2 = Arrays.asList(10, 20, 30);
        List<Boolean> list3 = Arrays.asList(true, false, true);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(list1, list2, list3);

        List<Triple<String, Integer, Boolean>> results = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> results.add(Triple.of(a, b, c)));

        assertEquals(3, results.size());
        assertEquals("y", results.get(1).left());
        assertEquals(20, results.get(1).middle());
        assertFalse(results.get(1).right());
    }

    @Test
    public void testZipIterablesWithNulls() {
        List<String> list1 = Arrays.asList("a", "b");
        List<Integer> list2 = null;
        List<Double> list3 = Arrays.asList(1.0, 2.0);

        TriIterator<String, Integer, Double> iter = TriIterator.zip(list1, list2, list3);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipIterators() {
        Iterator<String> iter1 = Arrays.asList("p", "q", "r").iterator();
        Iterator<Integer> iter2 = Arrays.asList(100, 200, 300).iterator();
        Iterator<Character> iter3 = Arrays.asList('A', 'B', 'C').iterator();

        TriIterator<String, Integer, Character> triIter = TriIterator.zip(iter1, iter2, iter3);

        assertTrue(triIter.hasNext());
        Triple<String, Integer, Character> first = triIter.next();
        assertEquals("p", first.left());
        assertEquals(100, first.middle());
        assertEquals('A', first.right());

        ObjIterator<String> mapped = triIter.map((a, b, c) -> a + b + c);
        assertEquals("q200B", mapped.next());
        assertEquals("r300C", mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        Iterator<String> iter1 = Arrays.asList("a").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();
        Iterator<Double> iter3 = Arrays.asList(1.1, 2.2).iterator();

        TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3, "missing", 0, 0.0);

        List<Triple<String, Integer, Double>> results = triIter.toList();
        assertEquals(3, results.size());
        assertEquals("missing", results.get(1).left());
        assertEquals(2, results.get(1).middle());
        assertEquals(2.2, results.get(1).right());
        assertEquals("missing", results.get(2).left());
        assertEquals(3, results.get(2).middle());
        assertEquals(0.0, results.get(2).right());
    }

    @Test
    public void testUnzipIterable() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        TriIterator<String, Integer, String> iter = TriIterator.unzip(pairs, (pair, triple) -> {
            triple.set(pair.left(), pair.right(), pair.left().toUpperCase());
        });

        List<Triple<String, Integer, String>> results = iter.toList();
        assertEquals(3, results.size());
        assertEquals("b", results.get(1).left());
        assertEquals(2, results.get(1).middle());
        assertEquals("B", results.get(1).right());
    }

    @Test
    public void testUnzipIterator() {
        Iterator<String> stringIter = Arrays.asList("hello", "world", "test").iterator();

        TriIterator<Character, Integer, String> iter = TriIterator.unzip(stringIter, (str, triple) -> {
            triple.set(str.charAt(0), str.length(), str.toUpperCase());
        });

        assertTrue(iter.hasNext());
        Triple<Character, Integer, String> first = iter.next();
        assertEquals('h', first.left());
        assertEquals(5, first.middle());
        assertEquals("HELLO", first.right());

        List<Triple<Character, Integer, String>> remaining = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> remaining.add(Triple.of(a, b, c)));
        assertEquals(2, remaining.size());
    }

    @Test
    public void testSkip() {
        TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 10, (i, triple) -> {
            triple.set(i, "str" + i, i * 0.5);
        });

        TriIterator<Integer, String, Double> skipped = iter.skip(3);

        Triple<Integer, String, Double> first = skipped.next();
        assertEquals(3, first.left());
        assertEquals("str3", first.middle());
        assertEquals(1.5, first.right());

        TriIterator<Integer, String, Double> iter2 = TriIterator.generate(0, 5, (i, triple) -> {
            triple.set(i, "s" + i, i * 1.0);
        });
        TriIterator<Integer, String, Double> notSkipped = iter2.skip(0);
        assertEquals(0, notSkipped.next().left());
    }

    @Test
    public void testSkipMoreThanAvailable() {
        TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 3, (i, triple) -> {
            triple.set(i, "str" + i, i * 0.5);
        });

        TriIterator<Integer, String, Double> skipped = iter.skip(5);
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testLimit() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.generate(0, 10, (i, triple) -> {
            triple.set(i, "item" + i, i % 2 == 0);
        });

        TriIterator<Integer, String, Boolean> limited = iter.limit(3);

        List<Triple<Integer, String, Boolean>> results = limited.toList();
        assertEquals(3, results.size());
        assertEquals(0, results.get(0).left());
        assertEquals(2, results.get(2).left());
    }

    @Test
    public void testLimitWithZero() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.generate(0, 10, (i, triple) -> {
            triple.set(i, "item" + i, true);
        });

        TriIterator<Integer, String, Boolean> limited = iter.limit(0);
        assertFalse(limited.hasNext());
    }

    @Test
    public void testFilter() {
        TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 10, (i, triple) -> {
            triple.set(i, "str" + i, i * 1.5);
        });

        TriPredicate<Integer, String, Double> predicate = (a, b, c) -> a % 2 == 0 && c > 3.0;
        TriIterator<Integer, String, Double> filtered = iter.filter(predicate);

        List<Triple<Integer, String, Double>> results = filtered.toList();
        assertEquals(3, results.size());
        assertEquals(4, results.get(0).left());
        assertEquals(6.0, results.get(0).right());
        assertEquals(8, results.get(2).left());
    }

    @Test
    public void testMap() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.generate(0, 3, (i, triple) -> {
            triple.set("a" + i, i * 10, i % 2 == 0);
        });

        ObjIterator<String> mapped = iter.map((a, b, c) -> a + "-" + b + "-" + c);

        List<String> results = new ArrayList<>();
        while (mapped.hasNext()) {
            results.add(mapped.next());
        }

        assertEquals(3, results.size());
        assertEquals("a0-0-true", results.get(0));
        assertEquals("a1-10-false", results.get(1));
        assertEquals("a2-20-true", results.get(2));
    }

    @Test
    public void testFirst() {
        TriIterator<String, Integer, Double> iter = TriIterator.generate(0, 3, (i, triple) -> {
            triple.set("first" + i, i, i * 0.1);
        });

        Optional<Triple<String, Integer, Double>> first = iter.first();
        assertTrue(first.isPresent());
        assertEquals("first0", first.get().left());
        assertEquals(0, first.get().middle());
        assertEquals(0.0, first.get().right());

        TriIterator<String, Integer, Double> emptyIter = TriIterator.empty();
        assertFalse(emptyIter.first().isPresent());
    }

    @Test
    public void testLast() {
        TriIterator<String, Integer, Character> iter = TriIterator.generate(0, 4, (i, triple) -> {
            triple.set("last" + i, i * 2, (char) ('A' + i));
        });

        Optional<Triple<String, Integer, Character>> last = iter.last();
        assertTrue(last.isPresent());
        assertEquals("last3", last.get().left());
        assertEquals(6, last.get().middle());
        assertEquals('D', last.get().right());

        TriIterator<String, Integer, Character> emptyIter = TriIterator.empty();
        assertFalse(emptyIter.last().isPresent());
    }

    @Test
    public void testStream() {
        TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 3, (i, triple) -> {
            triple.set(i, "s" + i, i * 2.5);
        });

        Stream<String> stream = iter.stream((a, b, c) -> a + ":" + b + ":" + c);
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertEquals("0:s0:0.0", results.get(0));
        assertEquals("1:s1:2.5", results.get(1));
        assertEquals("2:s2:5.0", results.get(2));
    }

    @Test
    public void testToArray() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.generate(0, 3, (i, triple) -> {
            triple.set("arr" + i, i * 3, i > 0);
        });

        Triple<String, Integer, Boolean>[] array = iter.toArray();

        assertEquals(3, array.length);
        assertEquals("arr0", array[0].left());
        assertEquals(0, array[0].middle());
        assertFalse(array[0].right());
        assertEquals("arr2", array[2].left());
        assertEquals(6, array[2].middle());
        assertTrue(array[2].right());
    }

    @Test
    public void testToList() {
        TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 4, (i, triple) -> {
            triple.set(i * 10, "list" + i, i / 2.0);
        });

        List<Triple<Integer, String, Double>> list = iter.toList();

        assertEquals(4, list.size());
        assertEquals(20, list.get(2).left());
        assertEquals("list2", list.get(2).middle());
        assertEquals(1.0, list.get(2).right());
    }

    @Test
    public void testToMultiList() {
        TriIterator<String, Integer, Character> iter = TriIterator.generate(0, 3, (i, triple) -> {
            triple.set("ml" + i, i * 5, (char) ('X' + i));
        });

        Triple<List<String>, List<Integer>, List<Character>> multiList = iter.toMultiList(ArrayList::new);

        assertEquals(3, multiList.left().size());
        assertEquals(3, multiList.middle().size());
        assertEquals(3, multiList.right().size());

        assertEquals("ml1", multiList.left().get(1));
        assertEquals(5, multiList.middle().get(1));
        assertEquals('Y', multiList.right().get(1));
    }

    @Test
    public void testToMultiSet() {
        TriIterator<String, Integer, Double> iter = TriIterator.generate(0, 4, (i, triple) -> {
            triple.set("set" + (i % 2), i % 3, (i % 2) * 1.5);
        });

        Triple<Set<String>, Set<Integer>, Set<Double>> multiSet = iter.toMultiSet(HashSet::new);

        assertEquals(2, multiSet.left().size());
        assertEquals(3, multiSet.middle().size());
        assertEquals(2, multiSet.right().size());

        assertTrue(multiSet.left().contains("set0"));
        assertTrue(multiSet.left().contains("set1"));
        assertTrue(multiSet.middle().contains(0));
        assertTrue(multiSet.middle().contains(1));
        assertTrue(multiSet.middle().contains(2));
    }

    @Test
    public void testForEachRemaining() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.generate(0, 5, (i, triple) -> {
            triple.set(i, "for" + i, i % 2 == 1);
        });

        iter.next();
        iter.next();

        List<String> results = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> results.add(a + "-" + b + "-" + c));

        assertEquals(3, results.size());
        assertEquals("2-for2-false", results.get(0));
        assertEquals("3-for3-true", results.get(1));
        assertEquals("4-for4-false", results.get(2));
    }

    @Test
    public void testForeachRemaining() throws Exception {
        TriIterator<String, Integer, Double> iter = TriIterator.generate(0, 4, (i, triple) -> {
            triple.set("each" + i, i * 2, i * 0.25);
        });

        iter.next();

        List<Triple<String, Integer, Double>> results = new ArrayList<>();
        iter.foreachRemaining((a, b, c) -> results.add(Triple.of(a, b, c)));

        assertEquals(3, results.size());
        assertEquals("each1", results.get(0).left());
        assertEquals(2, results.get(0).middle());
        assertEquals(0.25, results.get(0).right());
    }

    @Test
    public void testComplexChaining() {
        TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 20, (i, triple) -> {
            triple.set(i, "val" + i, i * 0.5);
        });

        List<String> results = iter.skip(2).limit(10).filter((a, b, c) -> a % 3 == 0).map((a, b, c) -> b + "=" + c).toList();

        assertEquals(3, results.size());
        assertEquals("val3=1.5", results.get(0));
        assertEquals("val6=3.0", results.get(1));
        assertEquals("val9=4.5", results.get(2));
    }

    @Test
    public void testNullHandling() {
        String[] arr1 = { "a", null, "c" };
        Integer[] arr2 = { 1, 2, null };
        Double[] arr3 = { null, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> results = iter.toList();
        assertEquals(3, results.size());
        assertNull(results.get(0).right());
        assertNull(results.get(1).left());
        assertNull(results.get(2).middle());
    }

    @Test
    public void testArgumentValidation() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null, (Consumer<Triple<String, Integer, Double>>) null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().skip(-1));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().limit(-1));

        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(2, 5, (IntObjConsumer<Triple<String, Integer, Double>>) null));
    }
}
