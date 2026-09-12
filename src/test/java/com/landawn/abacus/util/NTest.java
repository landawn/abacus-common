package com.landawn.abacus.util;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XmlDeserConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.stream.Stream;

import testfixtures.entity.extendDirty.basic.Account;

public class NTest extends NTestSupport {
    @Test
    public void testHasMatchCountBetween() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6);
        assertTrue(N.hasMatchCountBetween(list, 3, 3, IS_EVEN_INTEGER));
        assertTrue(N.hasMatchCountBetween(list, 1, 2, x -> x > 5));
        assertTrue(N.hasMatchCountBetween(list.iterator(), 3, 3, IS_EVEN_INTEGER));

        assertTrue(N.hasMatchCountBetween(integerArray, 2, 3, i -> i % 2 == 0));
        assertFalse(N.hasMatchCountBetween(integerArray, 3, 4, i -> i % 2 == 0));
        assertTrue(N.hasMatchCountBetween(new Integer[0], 0, 0, i -> true));

        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        assertTrue(N.hasMatchCountBetween(arr, 3, 3, IS_EVEN_INTEGER));
        assertTrue(N.hasMatchCountBetween(arr, 2, 4, IS_EVEN_INTEGER));
        assertFalse(N.hasMatchCountBetween(arr, 4, 5, IS_EVEN_INTEGER));
        assertTrue(N.hasMatchCountBetween(arr, 0, 0, x -> x > 10));
        assertTrue(N.hasMatchCountBetween((Integer[]) null, 0, 0, IS_EVEN_INTEGER));
        assertFalse(N.hasMatchCountBetween((Integer[]) null, 1, 1, IS_EVEN_INTEGER));

        assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(arr, -1, 2, IS_EVEN_INTEGER));
        assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(arr, 3, 1, IS_EVEN_INTEGER));
        assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(integerArray, 1, -1, i -> true));
    }

    @Test
    public void test_frequency() throws Exception {
        assertEquals(1, N.frequency("aaa", "aa"));
        assertEquals(1, N.frequency("ababaab", "aa"));
        assertEquals(4, N.frequency("ababaab", "a"));

    }

    @Test
    public void test_frequencyMap() {
        assertEquals(Map.of("a", 3, "b", 2, "c", 1, "D", 1), N.frequencyMap(CommonUtil.toList("a", "b", "a", "c", "a", "D", "b")));
    }

    @Test
    public void testFrequencyMapWritesEachDistinctKeyOnce() {
        final class CountingMap<K, V> extends HashMap<K, V> {
            private int putCount;

            @Override
            public V put(final K key, final V value) {
                putCount++;
                return super.put(key, value);
            }
        }

        final CountingMap<String, Integer> iterableMap = new CountingMap<>();
        final Map<String, Integer> iterableResult = N.frequencyMap(Arrays.asList("a", "a", "a", "b", "b", "c"), () -> iterableMap);
        assertSame(iterableMap, iterableResult);
        assertEquals(3, iterableMap.putCount);
        assertEquals(Map.of("a", 3, "b", 2, "c", 1), iterableResult);

        final CountingMap<String, Integer> iteratorMap = new CountingMap<>();
        final Map<String, Integer> iteratorResult = N.frequencyMap(Arrays.asList("a", "a", "a", "b", "b", "c").iterator(), () -> iteratorMap);
        assertSame(iteratorMap, iteratorResult);
        assertEquals(3, iteratorMap.putCount);
        assertEquals(Map.of("a", 3, "b", 2, "c", 1), iteratorResult);
    }

    @Test
    public void testFrequencyMapPreservesEncounterOrderWithOrderedSupplier() {
        // All three overloads must agree: with an order-preserving supplier, distinct keys
        // appear in first-encounter order (the array overload has always behaved this way).
        final List<String> words = Arrays.asList("c", "a", "b", "a", "c");

        final Map<String, Integer> fromArray = N.frequencyMap(new String[] { "c", "a", "b", "a", "c" }, LinkedHashMap::new);
        assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(fromArray.keySet()));

        final Map<String, Integer> fromIterable = N.frequencyMap(words, LinkedHashMap::new);
        assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(fromIterable.keySet()));
        assertEquals(Map.of("c", 2, "a", 2, "b", 1), fromIterable);

        final Map<String, Integer> fromIterator = N.frequencyMap(words.iterator(), LinkedHashMap::new);
        assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(fromIterator.keySet()));
        assertEquals(Map.of("c", 2, "a", 2, "b", 1), fromIterator);
    }

    @Test
    public void test_ParserUtil() {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        assertTrue(CommonUtil.toSet(beanInfo).contains(beanInfo));
        final PropInfo propInfo = beanInfo.getPropInfo("firstName");
        assertTrue(CommonUtil.toSet(propInfo).contains(propInfo));
    }

    @Test
    public void test_indexOf() {
        {
            final double[] a = { Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY };
            assertTrue(N.contains(a, Double.POSITIVE_INFINITY));
            assertTrue(N.contains(a, Double.NEGATIVE_INFINITY));
            assertTrue(N.contains(a, Double.NaN));
        }

        {
            final float[] a = { Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY };
            assertTrue(N.contains(a, Float.POSITIVE_INFINITY));
            assertTrue(N.contains(a, Float.NEGATIVE_INFINITY));
            assertTrue(N.contains(a, Float.NaN));
        }
    }

    @Test
    public void testCustomPredicatesAndFunctions() {
        String[] words = { "hello", "world", "java", "programming" };

        Predicate<String> complexPredicate = new Predicate<>() {
            @Override
            public boolean test(String s) {
                return s.length() > 4 && s.contains("o") && !s.startsWith("p");
            }
        };

        List<String> result = N.filter(words, complexPredicate);
        assertEquals(Arrays.asList("hello", "world"), result);

        Function<String, String> statefulMapper = new Function<>() {
            private int counter = 0;

            @Override
            public String apply(String s) {
                return s + "_" + (counter++);
            }
        };

        List<String> mapped = N.map(words, statefulMapper);
        assertEquals("hello_0", mapped.get(0));
        assertEquals("programming_3", mapped.get(3));
    }

    @Test
    public void testNewHashSet() {
        Set<String> set = CommonUtil.newHashSet(10);
        assertNotNull(set);
        assertTrue(set instanceof HashSet);
        assertEquals(0, set.size());

        set = CommonUtil.newHashSet(Arrays.asList("a", "b", "c"));
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testCommonSet_twoCollections() {
        Collection<String> a = Arrays.asList("a", "b", "c", "c");
        Collection<?> b = Arrays.asList("b", "c", "d", "c", "e");
        Set<String> expected = new HashSet<>(Arrays.asList("b", "c"));
        assertEquals(expected, N.commonSet(a, b));

        assertTrue(N.commonSet(null, b).isEmpty());
        assertTrue(N.commonSet(a, null).isEmpty());
        assertTrue(N.commonSet(a, Collections.emptyList()).isEmpty());
    }

    @Test
    public void testCommonSet_collectionOfCollections() {
        Collection<String> c1 = Arrays.asList("a", "b", "c", "c");
        Collection<String> c2 = Arrays.asList("b", "c", "d", "c");
        Collection<String> c3 = Arrays.asList("c", "a", "b", "c", "b");
        Set<String> expected = new HashSet<>(Arrays.asList("b", "c"));

        List<Collection<String>> listOfColls = Arrays.asList(c1, c2, c3);
        assertEquals(expected, N.commonSet(listOfColls));

        assertTrue(N.commonSet((Collection<Collection<String>>) null).isEmpty());
        assertEquals(new HashSet<>(c1), N.commonSet(Collections.singletonList(c1)));
        assertTrue(N.commonSet(Arrays.asList(c1, Collections.emptyList())).isEmpty());

        Collection<String> lc1 = new LinkedHashSet<>(Arrays.asList("z", "y", "x"));
        Collection<String> lc2 = new LinkedHashSet<>(Arrays.asList("y", "x", "w"));
        Set<String> expectedLinked = new LinkedHashSet<>(Arrays.asList("y", "x"));
        List<Collection<String>> listOfLinked = Arrays.asList(lc1, lc2);
        Set<String> actualLinked = N.commonSet(listOfLinked);
        assertEquals(expectedLinked, actualLinked);
        assertTrue(actualLinked instanceof LinkedHashSet);

        List<Integer> first = Arrays.asList(3, 2, 1, 4);
        List<Integer> smaller = Arrays.asList(1, 2, 3);
        Set<Integer> ordered = N.commonSet(Arrays.asList(first, smaller));
        assertEquals(Arrays.asList(3, 2, 1), new ArrayList<>(ordered));

        Set<Integer> orderedSingle = N.commonSet(Collections.singletonList(first));
        assertTrue(orderedSingle instanceof LinkedHashSet);
        assertEquals(first, new ArrayList<>(orderedSingle));
    }

    @Test
    public void testCommonSet() {
        List<String> a = Arrays.asList("a", "b", "c", "a");
        List<String> b = Arrays.asList("b", "c", "d", "c");
        Set<String> result = N.commonSet(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));

        assertEquals(new HashSet<>(), N.commonSet(new ArrayList<>(), b));
        assertEquals(new HashSet<>(), N.commonSet(a, new ArrayList<>()));
        assertEquals(new HashSet<>(), N.commonSet(null, b));
        assertEquals(new HashSet<>(), N.commonSet(a, null));
    }

    @Test
    public void testCommonSetMultipleCollections() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("b", "c", "d");
        List<String> c = Arrays.asList("c", "d", "e");
        List<Collection<String>> collections = Arrays.asList(a, b, c);
        Set<String> result = N.commonSet(collections);
        assertEquals(1, result.size());
        assertTrue(result.contains("c"));

        assertEquals(new HashSet<>(), N.commonSet(new ArrayList<Collection<String>>()));

        List<Collection<String>> singleCollection = Arrays.asList(a);
        assertEquals(new HashSet<>(a), N.commonSet(singleCollection));

        List<Collection<String>> withEmpty = Arrays.asList(a, new ArrayList<>(), c);
        assertEquals(new HashSet<>(), N.commonSet(withEmpty));
    }

    @Test
    public void testCommonSetThreeCollections() {
        List<Integer> a = Arrays.asList(1, 2, 3, 4);
        List<Integer> b = Arrays.asList(2, 3, 4, 5);
        List<Integer> c = Arrays.asList(3, 4, 5, 6);
        Set<Integer> result = N.commonSet(Arrays.asList(a, b, c));
        assertEquals(new HashSet<>(Arrays.asList(3, 4)), result);

        // One collection is empty
        Set<Integer> empty = N.commonSet(Arrays.asList(a, new ArrayList<Integer>(), c));
        assertEquals(0, empty.size());
    }

    @Test
    public void testExclude() {
        Collection<String> coll = Arrays.asList("a", "b", "a", "c");
        assertEquals(Arrays.asList("b", "c"), N.exclude(coll, "a"));
        assertEquals(Arrays.asList("a", "b", "a", "c"), N.exclude(coll, "d"));
        assertEquals(Arrays.asList("a", "a", "c"), N.exclude(coll, "b"));
        assertEquals(Arrays.asList("a", "b", "a", "c"), N.exclude(coll, null));

        Collection<String> collWithNull = Arrays.asList("a", null, "b", null);
        assertEquals(Arrays.asList("a", "b"), N.exclude(collWithNull, null));

        assertTrue(N.exclude(null, "a").isEmpty());
        assertTrue(N.exclude(Collections.emptyList(), "a").isEmpty());
    }

    @Test
    public void testExcludeToSet() {
        Collection<String> coll = Arrays.asList("a", "b", "a", "c");
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), N.excludeToSet(coll, "a"));
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), N.excludeToSet(coll, "d"));

        Collection<String> linkedColl = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        Set<String> resultLinked = N.excludeToSet(linkedColl, "a");
        assertEquals(new LinkedHashSet<>(Arrays.asList("c", "b")), resultLinked);
        assertTrue(resultLinked instanceof LinkedHashSet);

        assertTrue(N.excludeToSet(null, "a").isEmpty());
    }

    @Test
    public void testExcludeAll() {
        Collection<String> main = Arrays.asList("a", "b", "c", "a", "d");
        Collection<?> toExclude = Arrays.asList("a", "c", "e");
        assertEquals(Arrays.asList("b", "d"), N.excludeAll(main, toExclude));

        assertEquals(new ArrayList<>(main), N.excludeAll(main, null));
        assertEquals(new ArrayList<>(main), N.excludeAll(main, Collections.emptyList()));
        assertTrue(N.excludeAll(null, toExclude).isEmpty());

        assertEquals(Arrays.asList("b", "c", "d"), N.excludeAll(main, Collections.singletonList("a")));
    }

    @Test
    public void testExcludeAllToSet_LinkedHashSetSource_UsesLinkedHashSetResult() {
        // L6371: c instanceof LinkedHashSet -> newLinkedHashSet
        LinkedHashSet<String> main = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        Set<String> result = N.excludeAllToSet(main, Arrays.asList("b", "d"));
        // Should be a LinkedHashSet preserving insertion order
        assertFalse(result.contains("b"));
        assertFalse(result.contains("d"));
        assertEquals(3, result.size());
    }

    @Test
    public void testExcludeAllToSet() {
        Collection<String> main = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        Collection<?> toExclude = Arrays.asList("a", "c", "e");
        assertEquals(new LinkedHashSet<>(Arrays.asList("b", "d")), N.excludeAllToSet(main, toExclude));

        assertEquals(new HashSet<>(main), N.excludeAllToSet(main, null));
        assertTrue(N.excludeAllToSet(null, toExclude).isEmpty());
    }

    // ========== excludeAllToSet with single-element collection ==========

    @Test
    public void testExcludeAllToSet_SingleElementToExclude_UsesExcludeToSetPath() {
        // L6366: objsToExclude.size() == 1 -> excludeToSet(c, firstOrNullIfEmpty(objsToExclude))
        List<String> main = Arrays.asList("a", "b", "c", "d");
        Set<String> result = N.excludeAllToSet(main, Collections.singletonList("b"));
        assertEquals(new HashSet<>(Arrays.asList("a", "c", "d")), result);
    }

    @Test
    public void testLargeArrayOperations() {
        int[] largeArray = new int[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i % 100;
        }

        int count = N.replaceIf(largeArray, val -> val < 50, -1);
        assertEquals(500, count);

        int[] unique = N.removeDuplicates(largeArray);
        assertEquals(51, unique.length);
    }

    @Test
    public void testEmptyArrayOperations() {
        assertEquals(0, N.replaceIf(new int[0], val -> true, 1));

        assertEquals(0, N.replaceAll(new int[0], 1, 2));

        assertArrayEquals(new int[] { 1 }, N.add(new int[0], 1));

        assertArrayEquals(new int[0], N.remove(new int[0], 1));

        assertArrayEquals(new int[0], N.removeAll(new int[0], 1, 2));

        assertArrayEquals(new int[0], N.removeDuplicates(new int[0]));
    }

    @Test
    public void testNullArrayOperations() {
        assertEquals(0, N.replaceIf((int[]) null, val -> true, 1));

        assertEquals(0, N.replaceAll((int[]) null, 1, 2));

        assertArrayEquals(new int[] { 1 }, N.add((int[]) null, 1));

        assertArrayEquals(new int[0], N.remove((int[]) null, 1));

        assertArrayEquals(new int[0], N.removeAll((int[]) null, 1, 2));

        assertArrayEquals(new int[0], N.removeDuplicates((int[]) null));
    }

    @Test
    public void test_applyToEach() {
        final String[] a = CommonUtil.asArray("a ", "b", " c");
        N.replaceAll(a, Strings::trim);
        assertArrayEquals(new String[] { "a", "b", "c" }, a);

        final List<String> list = CommonUtil.toList("a ", "b", " c");
        N.replaceAll(list, Strings::trim);
        assertEquals(List.of("a", "b", "c"), list);

        final List<String> linked = CommonUtil.toLinkedList("a ", "b", " c");
        N.replaceAll(linked, Strings::trim);
        assertEquals(List.of("a", "b", "c"), linked);
    }

    @Test
    public void testPerformanceConsiderations() {
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            arrayList.add(i);
        }
        N.replaceAll(arrayList, val -> val * 2);

        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            linkedList.add(i);
        }
        N.replaceAll(linkedList, val -> val * 2);

        assertEquals(arrayList, linkedList);
    }

    @Test
    public void testCollectionWithNullElements() {
        List<String> list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));

        int count = N.replaceAll(list, null, "x");
        assertEquals(2, count);
        assertEquals(Arrays.asList("a", "x", "b", "x", "c"), list);

        list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));
        boolean removed = N.remove(list, null);
        assertTrue(removed);
        assertEquals(Arrays.asList("a", "b", null, "c"), list);

        list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));
        removed = N.removeAllOccurrences(list, null);
        assertTrue(removed);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testSpecialFloatingPointValues() {
        float[] floatArr = { 1.0f, Float.NaN, 2.0f, Float.NaN };
        int count = N.replaceAll(floatArr, Float.NaN, 0.0f);
        assertEquals(2, count);

        double[] doubleArr = { 1.0, Double.NaN, 2.0, Double.NaN };
        count = N.replaceAll(doubleArr, Double.NaN, 0.0);
        assertEquals(2, count);

        floatArr = new float[] { 1.0f, Float.POSITIVE_INFINITY, 2.0f, Float.NEGATIVE_INFINITY };
        count = N.replaceAll(floatArr, Float.POSITIVE_INFINITY, 999.0f);
        assertEquals(1, count);
        assertArrayEquals(new float[] { 1.0f, 999.0f, 2.0f, Float.NEGATIVE_INFINITY }, floatArr, 0.001f);

        doubleArr = new double[] { 1.0, Double.POSITIVE_INFINITY, 2.0, Double.NEGATIVE_INFINITY };
        count = N.replaceAll(doubleArr, Double.POSITIVE_INFINITY, 999.0);
        assertEquals(1, count);
        assertArrayEquals(new double[] { 1.0, 999.0, 2.0, Double.NEGATIVE_INFINITY }, doubleArr, 0.001);
    }

    @Test
    public void testUpdateAllArray() throws IOException {
        String[] arr = { "a", "b" };
        Throwables.UnaryOperator<String, IOException> operator = s -> {
            if ("b".equals(s)) {
                throw new IOException("Test Exception");
            }
            return s.toUpperCase();
        };

        assertThrows(IOException.class, () -> N.updateAll(arr, operator));
        assertEquals("A", arr[0]);
        assertEquals("b", arr[1]);

        String[] arr2 = { "c", "d" };
        Throwables.UnaryOperator<String, IOException> noExceptionOp = String::toUpperCase;
        N.updateAll(arr2, noExceptionOp);
        assertArrayEquals(new String[] { "C", "D" }, arr2);

        N.updateAll((String[]) null, noExceptionOp);
        N.updateAll(new String[0], noExceptionOp);
    }

    @Test
    public void testUpdateAllList() throws IOException {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Throwables.UnaryOperator<String, IOException> operator = s -> {
            if ("b".equals(s)) {
                throw new IOException("Test Exception on List");
            }
            return s.toUpperCase();
        };

        assertThrows(IOException.class, () -> N.updateAll(list, operator));
        assertEquals("A", list.get(0));
        assertEquals("b", list.get(1));

        List<String> list2 = new LinkedList<>(Arrays.asList("c", "d"));
        Throwables.UnaryOperator<String, IOException> noExceptionOp = String::toUpperCase;
        N.updateAll(list2, noExceptionOp);
        assertEquals(Arrays.asList("C", "D"), list2);

        N.updateAll((List<String>) null, noExceptionOp);
        N.updateAll(new ArrayList<String>(), noExceptionOp);
    }

    @Test
    public void testUpdateAllUsingReplaceAllInstead() {
        assertThrows(UnsupportedOperationException.class, N::updateAllUsingReplaceAllInstead);
    }

    @Test
    public void testUpdateIfUsingReplaceIfInstead() {
        assertThrows(UnsupportedOperationException.class, N::updateIfUsingReplaceIfInstead);
    }

    @Test
    public void testBoundaryIndices() {
        int[] arr = { 1, 2, 3, 4, 5 };

        int[] result = N.insert(arr, 0, 0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, result);

        result = N.insert(arr, 5, 6);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

        result = N.removeAt(arr, 0);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, result);

        result = N.removeAt(arr, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);

        result = N.removeDuplicates(arr, 2, 3, false);
        assertArrayEquals(new int[] { 3 }, result);
    }

    @Test
    public void testLinkedListOperations() {
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        boolean result = N.removeAt(linkedList, 1, 3);
        assertTrue(result);
        assertEquals(Arrays.asList("a", "c", "e"), linkedList);

        linkedList = new LinkedList<>(Arrays.asList("a", "b", "a", "c"));
        int count = N.replaceAll(linkedList, "a", "x");
        assertEquals(2, count);
        assertEquals(Arrays.asList("x", "b", "x", "c"), linkedList);
    }

    @Test
    public void testMoveRangeGenericArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 0);
        assertArrayEquals(new Integer[] { 2, 3, 1, 4, 5 }, arr);

        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        N.moveRange(arr2, 0, 2, 3);
        assertArrayEquals(new Integer[] { 3, 4, 5, 1, 2 }, arr2);
    }

    @Test
    public void testMoveRange_boolean() {
        boolean[] arr = { true, false, true, false, true };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr);
    }

    @Test
    public void testMoveRange_char() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new char[] { 'a', 'd', 'e', 'b', 'c' }, arr);

        char[] arr2 = { 'a', 'b', 'c' };
        N.moveRange(arr2, 1, 1, 0);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, arr2);

        N.moveRange(arr2, 1, 2, 1);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, arr2);
    }

    @Test
    public void testMoveRange_byte() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new byte[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_short() {
        short[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new short[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_int() {
        int[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new int[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_long() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new long[] { 1L, 4L, 5L, 2L, 3L }, arr);
    }

    @Test
    public void testMoveRange_float() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f, 2.0f, 3.0f }, arr);
    }

    @Test
    public void testMoveRange_double() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new double[] { 1.0, 4.0, 5.0, 2.0, 3.0 }, arr);
    }

    @Test
    public void testMoveRange_generic() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new Integer[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_List() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(N.moveRange(list, 1, 3, 3));
        assertEquals(Arrays.asList(1, 4, 5, 2, 3), list);

        assertFalse(N.moveRange(list, 1, 1, 0));
    }

    @Test
    public void testMoveRange_String_method() {
        String result = N.moveRange("hello", 1, 3, 3);
        assertEquals("hloel", result);
    }

    @Test
    public void testMoveRangeBooleanArray() {
        boolean[] arr = { true, false, true, false, true };
        N.moveRange(arr, 1, 3, 0);
        assertArrayEquals(new boolean[] { false, true, true, false, true }, arr);

        boolean[] arr2 = { true, false, true, false, true };
        N.moveRange(arr2, 0, 2, 3);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, arr2);

        boolean[] arr3 = { true, false, true };
        N.moveRange(arr3, 0, 1, 1);
        assertArrayEquals(new boolean[] { false, true, true }, arr3);

        boolean[] arr4 = { true, false, true, false, true };
        N.moveRange(arr4, 0, 0, 0);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);
        N.moveRange(arr4, 1, 1, 0);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);

        N.moveRange(arr4, 1, 2, 1);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true }, -1, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true }, 0, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true, false }, 0, 1, 2));
    }

    @Test
    public void testMoveRangeList() {
        List<String> list = toMutableList("a", "b", "c", "d", "e");
        assertTrue(N.moveRange(list, 1, 3, 0));
        assertEquals(toMutableList("b", "c", "a", "d", "e"), list);

        List<String> list2 = toMutableList("a", "b", "c", "d", "e");
        assertTrue(N.moveRange(list2, 0, 2, 3));
        assertEquals(toMutableList("c", "d", "e", "a", "b"), list2);

        List<String> list3 = toMutableList("a", "b", "c");
        assertFalse(N.moveRange(list3, 0, 0, 0));
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertFalse(N.moveRange(list3, 1, 1, 0));
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertFalse(N.moveRange(list3, 0, 1, 0));
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(toMutableList("a"), -1, 0, 0));
        N.moveRange((List<String>) null, 0, 0, 0);
    }

    @Test
    public void testMoveRangeString() {
        assertEquals("bcade", N.moveRange("abcde", 1, 3, 0));
        assertEquals("cdeab", N.moveRange("abcde", 0, 2, 3));
        assertEquals("abc", N.moveRange("abc", 0, 0, 0));
        assertEquals("abc", N.moveRange("abc", 1, 1, 0));
        assertEquals("abc", N.moveRange("abc", 0, 1, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange("a", -1, 0, 0));
    }

    @Test
    public void testSkipRange_Collection() {
        List<Integer> result = N.skipRange(integerList, 1, 3);
        assertEquals(Arrays.asList(1, 4, 5), result);

        Set<Integer> resultSet = N.skipRange(integerList, 1, 3, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList(1, 4, 5)), resultSet);
    }

    @Test
    public void testSkipRange_generic() {
        Integer[] result = N.skipRange(integerArray, 1, 3);
        assertArrayEquals(new Integer[] { 1, 4, 5 }, result);

        // generic T[] skipRange delegates to removeRange, which now enforces @NotNull
        assertThrows(IllegalArgumentException.class, () -> N.skipRange((Integer[]) null, 0, 0));

        assertArrayEquals(integerArray.clone(), N.skipRange(integerArray, 0, 0));
    }

    @Test
    public void testSkipRangeGenericArray() {
        // generic T[] skipRange delegates to removeRange, which now enforces @NotNull
        assertThrows(IllegalArgumentException.class, () -> N.skipRange((Integer[]) null, 0, 0));
        Integer[] emptyArr = {};
        assertArrayEquals(emptyArr, N.skipRange(emptyArr, 0, 0));

        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, N.skipRange(arr, 2, 2));
        assertArrayEquals(arr, N.skipRange(arr, 2, 2));

        assertArrayEquals(new Integer[] { 3, 4, 5 }, N.skipRange(arr, 0, 2));
        assertArrayEquals(new Integer[] { 1, 2 }, N.skipRange(arr, 2, 5));
        assertArrayEquals(new Integer[] { 1, 5 }, N.skipRange(arr, 1, 4));
        assertArrayEquals(new Integer[] {}, N.skipRange(arr, 0, 5));

        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, 3, 1));
    }

    @Test
    public void testSkipRangeCollection() {
        Collection<Integer> coll = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.skipRange(coll, 2, 2));
        assertEquals(Arrays.asList(3, 4, 5), N.skipRange(coll, 0, 2));
        assertEquals(Arrays.asList(1, 2), N.skipRange(coll, 2, 5));
        assertEquals(Arrays.asList(1, 5), N.skipRange(coll, 1, 4));
        assertEquals(Collections.emptyList(), N.skipRange(coll, 0, 5));

        Collection<Integer> emptyColl = Collections.emptyList();
        assertEquals(Collections.emptyList(), N.skipRange(emptyColl, 0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(coll, -1, 2));

        Set<Integer> resultSet = N.skipRange(coll, 1, 3, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList(1, 4, 5)), resultSet);

        Collection<Integer> nonList = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        List<Integer> skippedNonList = N.skipRange(nonList, 1, 3);
        assertEquals(nonList.size() - 2, skippedNonList.size());
    }

    @Test
    public void testContainsDuplicates() {
        assertFalse(N.containsDuplicates((boolean[]) null));
        assertFalse(N.containsDuplicates(new boolean[] { true, false }));
        assertTrue(N.containsDuplicates(new boolean[] { true, false, true }));

        assertFalse(N.containsDuplicates(new byte[] { 1, 2, 3 }));
        assertTrue(N.containsDuplicates(new byte[] { 1, 2, 1 }));
        assertTrue(N.containsDuplicates(new byte[] { 1, 1, 2 }, true));

        assertFalse(N.containsDuplicates(new short[] { 1, 2, 3 }));
        assertTrue(N.containsDuplicates(new short[] { 1, 2, 1 }));
        assertTrue(N.containsDuplicates(new short[] { 1, 1, 2 }, true));

        assertFalse(N.containsDuplicates(new int[] { 1, 2, 3 }));
        assertTrue(N.containsDuplicates(new int[] { 1, 2, 1 }));
        assertTrue(N.containsDuplicates(new int[] { 1, 1, 2 }, true));

        assertFalse(N.containsDuplicates(new long[] { 1L, 2L, 3L }));
        assertTrue(N.containsDuplicates(new long[] { 1L, 2L, 1L }));
        assertTrue(N.containsDuplicates(new long[] { 1L, 1L, 2L }, true));

        assertFalse(N.containsDuplicates((char[]) null));
        assertFalse(N.containsDuplicates(new char[] { 'a', 'b', 'c' }));
        assertTrue(N.containsDuplicates(new char[] { 'a', 'b', 'a' }));
        assertTrue(N.containsDuplicates(new char[] { 'a', 'a', 'b' }, true));
        assertFalse(N.containsDuplicates(new char[] { 'd', 'c', 'b', 'a' }, false));

        assertFalse(N.containsDuplicates((float[]) null));
        assertTrue(N.containsDuplicates(new float[] { 1.0f, 2.0f, 1.0f }));
        assertTrue(N.containsDuplicates(new float[] { Float.NaN, Float.NaN }));
        assertTrue(N.containsDuplicates(new float[] { 1.0f, 1.0f, 2.0f }, true));

        assertFalse(N.containsDuplicates((double[]) null));
        assertTrue(N.containsDuplicates(new double[] { 1.0, 2.0, 1.0 }));
        assertTrue(N.containsDuplicates(new double[] { Double.NaN, Double.NaN }));
        assertTrue(N.containsDuplicates(new double[] { 1.0, 1.0, 2.0 }, true));

        assertFalse(N.containsDuplicates((Integer[]) null));
        assertTrue(N.containsDuplicates(new Integer[] { 1, 2, 1 }));
        assertTrue(N.containsDuplicates(new Integer[] { null, null }));
        assertFalse(N.containsDuplicates(new Integer[] { 1, null }));
        assertTrue(N.containsDuplicates(new String[] { "a", "a", "b" }, true));

        assertFalse(N.containsDuplicates((Collection<?>) null));
        assertFalse(N.containsDuplicates(Collections.emptyList()));
        assertFalse(N.containsDuplicates(Arrays.asList("a", "b", "c")));
        assertTrue(N.containsDuplicates(Arrays.asList("a", "b", "a")));
        assertTrue(N.containsDuplicates(Arrays.asList(null, null)));
        assertTrue(N.containsDuplicates(Arrays.asList(1, 2, 2, 3), true));
        assertFalse(N.containsDuplicates(new HashSet<>(Arrays.asList(1, 2, 3)), true));
    }

    @Test
    public void testRetainAll() {
        Collection<Integer> main = toMutableList(1, 2, 3, 4, 5);
        Collection<Integer> keep = Arrays.asList(3, 5, 6);
        assertTrue(N.retainAll(main, keep));
        assertEquals(toMutableList(3, 5), main);

        Collection<String> main2 = toMutableList("a", "b", "c");
        Collection<String> keep2 = Arrays.asList("x", "y");
        assertTrue(N.retainAll(main2, keep2));
        assertTrue(main2.isEmpty());

        Collection<Integer> main3 = toMutableList(1, 2, 3);
        Collection<Integer> keep3 = Arrays.asList(1, 2, 3, 4);
        assertFalse(N.retainAll(main3, keep3));
        assertEquals(toMutableList(1, 2, 3), main3);

        Collection<Integer> main4 = toMutableList(1, 2, 3);
        assertTrue(N.retainAll(main4, Collections.emptyList()));
        assertTrue(main4.isEmpty());

        Collection<Integer> emptyMain = new ArrayList<>();
        assertFalse(N.retainAll(emptyMain, Arrays.asList(1, 2)));
        assertTrue(emptyMain.isEmpty());

        HashSet<Integer> mainHashSet = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        List<Integer> keepList = Arrays.asList(1, 5, 10, 11);
        assertTrue(N.retainAll(mainHashSet, keepList));
        assertEquals(new HashSet<>(Arrays.asList(1, 5, 10)), mainHashSet);

        N.retainAll(null, keep);
        Collection<Integer> mainForNullKeep = toMutableList(1, 2, 3);
        assertTrue(N.retainAll(mainForNullKeep, null));
        assertTrue(mainForNullKeep.isEmpty());
    }

    @Test
    public void testNaNHandling() {
        assertEquals(Float.NaN, N.min(1.0f, Float.NaN), 0.001f);
        assertEquals(Float.NaN, N.max(1.0f, Float.NaN), 0.001f);
        assertEquals(Double.NaN, N.min(1.0, Double.NaN), 0.001);
        assertEquals(Double.NaN, N.max(1.0, Double.NaN), 0.001);

        // NaN propagates per Math.min/max contract — any NaN in the array → NaN result.
        assertEquals(Float.NaN, N.min(new float[] { 1.0f, Float.NaN, 3.0f }), 0.001f);
        assertEquals(Float.NaN, N.max(new float[] { 1.0f, Float.NaN, 3.0f }), 0.001f);
    }

    @Test
    public void testComparatorEdgeCases() {
        Comparator<Integer> reverseComp = Comparator.reverseOrder();
        assertEquals(Integer.valueOf(3), N.min(new Integer[] { 1, 2, 3 }, reverseComp));
        assertEquals(Integer.valueOf(1), N.max(new Integer[] { 1, 2, 3 }, reverseComp));

        assertThrows(IllegalArgumentException.class, () -> N.min(new Integer[] { 1, 2, 3 }, null));
        assertThrows(IllegalArgumentException.class, () -> N.max(new Integer[] { 1, 2, 3 }, null));
    }

    @Test
    public void testThreadSafety() {
        final int[] array = { 1, 2, 3, 4, 5 };
        final int iterations = 1000;
        final int threadCount = 10;

        Thread[] threads = new Thread[threadCount];
        final boolean[] errors = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        assertEquals(1, N.min(array));
                        assertEquals(5, N.max(array));
                        assertEquals(3, N.lowerMedian(array));
                        assertEquals(15, N.sum(array));
                        assertEquals(3.0, N.average(array), 0.001);
                    }
                } catch (Exception e) {
                    errors[threadIndex] = true;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail("Thread interrupted");
            }
        }

        for (boolean error : errors) {
            assertFalse(error, "Thread safety issue detected");
        }
    }

    @Test
    public void test_kthLargest() {
        {
            int[] a = { 1 };
            CommonUtil.shuffle(a);
            assertEquals(1, N.kthLargest(a, 1));

            a = Array.of(1, 2);
            CommonUtil.shuffle(a);
            assertEquals(2, N.kthLargest(a, 1));
            assertEquals(1, N.kthLargest(a, 2));

            a = Array.of(1, 2, 3);
            CommonUtil.shuffle(a);
            assertEquals(3, N.kthLargest(a, 1));
            assertEquals(2, N.kthLargest(a, 2));
            assertEquals(1, N.kthLargest(a, 3));
        }
    }

    @Test
    public void testKthLargest() {
        assertEquals('d', N.kthLargest(new char[] { 'a', 'b', 'c', 'd', 'e' }, 2));
        assertEquals((byte) 4, N.kthLargest(new byte[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals((short) 4, N.kthLargest(new short[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals(4, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals(4L, N.kthLargest(new long[] { 1L, 2L, 3L, 4L, 5L }, 2));
        assertEquals(4.0f, N.kthLargest(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2), 0.001f);
        assertEquals(4.0, N.kthLargest(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2), 0.001);
        assertEquals("d", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 2));
        assertEquals("d", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 2, Comparator.naturalOrder()));

        assertEquals(5, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 1));

        assertEquals(1, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 5));

        assertEquals('c', N.kthLargest(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4, 2));
        assertEquals((byte) 3, N.kthLargest(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals((short) 3, N.kthLargest(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals(3, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals(3L, N.kthLargest(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2));
        assertEquals(3.0f, N.kthLargest(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2), 0.001f);
        assertEquals(3.0, N.kthLargest(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2), 0.001);
        assertEquals("c", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 1, 4, 2));
        assertEquals("c", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 1, 4, 2, Comparator.naturalOrder()));
    }

    @Test
    public void testKthLargestCollection() {
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 2));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 2));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 2, Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 2, Comparator.naturalOrder()));
    }

    @Test
    public void testLargeArrayPerformance() {
        int[] largeArray = new int[10000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i;
        }

        assertEquals(9999, N.kthLargest(largeArray, 1));
        assertEquals(5000, N.kthLargest(largeArray, 5000));
        assertEquals(0, N.kthLargest(largeArray, 10000));

        int[] top100 = N.top(largeArray, 100);
        assertEquals(100, top100.length);
        assertEquals(9900, top100[0]);
    }

    @Test
    public void testKthLargestGeneric() {
        assertEquals("cherry", N.kthLargest(new String[] { "apple", "cherry", "banana" }, 1, String.CASE_INSENSITIVE_ORDER));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(5, 1, 4, 2, 3, null), 2));

        List<Integer> listWithNulls = Arrays.asList(null, 1, 5, null, 3);
        assertEquals(Integer.valueOf(5), N.kthLargest(listWithNulls, 1));
        assertEquals(Integer.valueOf(1), N.kthLargest(listWithNulls, 3));
        assertNull(N.kthLargest(listWithNulls, 4));

        Comparator<Integer> nullsLargest = Comparator.nullsLast(Comparator.reverseOrder());
        Comparator<Integer> forKthLargestNullsLargest = Comparator.nullsFirst(Comparator.reverseOrder());

        assertEquals(Integer.valueOf(3), N.kthLargest(listWithNulls, 2, forKthLargestNullsLargest));
        Comparator<Integer> cmpNullMax = Comparator.nullsLast(Comparator.naturalOrder());
        List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        assertEquals(Integer.valueOf(6), N.kthLargest(numbers, 2));
    }

    @Test
    public void testKthLargestPrimitives() {
        assertEquals(4, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 2));
        assertEquals(1, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 5));
        assertEquals(5, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 1));

        assertEquals(3.0f, N.kthLargest(new float[] { 1f, 5f, 2f, 4f, 3f }, 3), DELTA);

        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] {}, 1));
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2 }, 3));
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2 }, 0));
    }

    @Test
    public void testKthLargestArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] {}, 1));
    }

    @Test
    public void testKthLargestInvalidK() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2, 3 }, 4));
    }

    @Test
    public void testKthLargest_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new char[] {}, 1));

        assertEquals('e', N.kthLargest(charArray, 1));
        assertEquals('a', N.kthLargest(charArray, 5));
        assertEquals('c', N.kthLargest(charArray, 3));
        assertEquals('d', N.kthLargest(charArray, 1, 4, 1));

        assertEquals((byte) 5, N.kthLargest(byteArray, 1));
        assertEquals((byte) 1, N.kthLargest(byteArray, 5));
        assertEquals((byte) 3, N.kthLargest(byteArray, 3));

        assertEquals((short) 5, N.kthLargest(shortArray, 1));
        assertEquals((short) 1, N.kthLargest(shortArray, 5));
        assertEquals((short) 3, N.kthLargest(shortArray, 3));

        assertEquals(5, N.kthLargest(intArray, 1));
        assertEquals(1, N.kthLargest(intArray, 5));
        assertEquals(3, N.kthLargest(intArray, 3));

        assertEquals(5L, N.kthLargest(longArray, 1));
        assertEquals(1L, N.kthLargest(longArray, 5));
        assertEquals(3L, N.kthLargest(longArray, 3));

        assertEquals(5.0f, N.kthLargest(floatArray, 1), 0.001);
        assertEquals(1.0f, N.kthLargest(floatArray, 5), 0.001);
        assertEquals(3.0f, N.kthLargest(floatArray, 3), 0.001);

        assertEquals(5.0, N.kthLargest(doubleArray, 1), 0.001);
        assertEquals(1.0, N.kthLargest(doubleArray, 5), 0.001);
        assertEquals(3.0, N.kthLargest(doubleArray, 3), 0.001);
    }

    @Test
    public void testKthLargest_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new String[] {}, 1));

        String[] arr = { "a", "b", "c", "d", "e" };
        assertEquals("e", N.kthLargest(arr, 1));
        assertEquals("a", N.kthLargest(arr, 5));
        assertEquals("c", N.kthLargest(arr, 3));
        assertEquals("d", N.kthLargest(arr, 1, 4, 1));
        assertEquals("e", N.kthLargest(arr, 1, Comparator.naturalOrder()));
        assertEquals("c", N.kthLargest(arr, 0, 5, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testKthLargest_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(Collections.emptyList(), 1));

        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals("e", N.kthLargest(list, 1));
        assertEquals("a", N.kthLargest(list, 5));
        assertEquals("c", N.kthLargest(list, 3));
        assertEquals("d", N.kthLargest(list, 1, 4, 1));
        assertEquals("e", N.kthLargest(list, 1, Comparator.naturalOrder()));
        assertEquals("c", N.kthLargest(list, 0, 5, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testTopPrimitives() {
        assertArrayEquals(new int[] { 5, 9, 6 }, N.top(new int[] { 3, 1, 5, 9, 2, 6 }, 3));
        List<Integer> top3 = CommonUtil.toList(N.top(new int[] { 3, 1, 5, 9, 2, 6 }, 3));
        assertTrue(top3.containsAll(Arrays.asList(5, 9, 6)) && top3.size() == 3);

        assertArrayEquals(new int[] {}, N.top(new int[] { 1, 2, 3 }, 0));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.top(new int[] { 1, 2, 3 }, 5));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.top(new int[] { 1, 2, 3 }, 3));
    }

    @Test
    public void testTopInt() {
        assertArrayEquals(new int[] { 4, 5 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 2));
        assertArrayEquals(new int[] { 2, 1 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new int[] { 3, 4 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertArrayEquals(new int[] { 3, 2 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2, Comparator.reverseOrder()));
    }

    @Test
    public void testTopLong() {
        assertArrayEquals(new long[] { 4L, 5L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 2));
        assertArrayEquals(new long[] { 2L, 1L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new long[] { 3L, 4L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2));
        assertArrayEquals(new long[] { 3L, 2L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2, Comparator.reverseOrder()));
    }

    @Test
    public void testTopFloat() {
        assertArrayEquals(new float[] { 4.0f, 5.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2), 0.001f);
        assertArrayEquals(new float[] { 2.0f, 1.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2, Comparator.reverseOrder()), 0.001f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2), 0.001f);
        assertArrayEquals(new float[] { 3.0f, 2.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2, Comparator.reverseOrder()), 0.001f);
    }

    @Test
    public void testTopDouble() {
        assertArrayEquals(new double[] { 4.0, 5.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2), 0.001);
        assertArrayEquals(new double[] { 2.0, 1.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2, Comparator.reverseOrder()), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2), 0.001);
        assertArrayEquals(new double[] { 3.0, 2.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2, Comparator.reverseOrder()), 0.001);
    }

    @Test
    public void testTopGeneric() {
        List<String> top2 = N.top(new String[] { "apple", "banana", "cherry", "date" }, 2);
        assertTrue(top2.containsAll(Arrays.asList("date", "cherry")) && top2.size() == 2);

        List<String> top2Sorted = N.top(new String[] { "apple", "banana", "cherry", "date" }, 2, Comparator.naturalOrder());
        assertTrue(top2Sorted.containsAll(Arrays.asList("date", "cherry")) && top2Sorted.size() == 2);

        List<Integer> numbers = Arrays.asList(1, 5, 2, 8, 2, 5);
        List<Integer> top3 = N.top(numbers, 3);
        Collections.sort(top3, Comparator.reverseOrder());
        assertEquals(Arrays.asList(8, 5, 5), top3);

        Integer[] arrKeepOrder = { 1, 5, 2, 8, 2, 6 };
        List<Integer> top3KeepOrder = N.top(arrKeepOrder, 3, true);
        assertEquals(Arrays.asList(5, 8, 6), top3KeepOrder);

        List<Integer> topAllKeepOrder = N.top(arrKeepOrder, 10, true);
        assertEquals(Arrays.asList(1, 5, 2, 8, 2, 6), topAllKeepOrder);

        List<Integer> top0KeepOrder = N.top(arrKeepOrder, 0, true);
        assertTrue(top0KeepOrder.isEmpty());
    }

    @Test
    public void testTopShort() {
        assertArrayEquals(new short[] { 4, 5 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 2));
        assertArrayEquals(new short[] { 2, 1 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new short[] { 3, 4 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertArrayEquals(new short[] { 3, 2 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2, Comparator.reverseOrder()));

        assertArrayEquals(new short[] { 1, 2, 3 }, N.top(new short[] { 1, 2, 3 }, 5));

        assertArrayEquals(new short[] {}, N.top(new short[] { 1, 2, 3 }, 0));

        assertArrayEquals(new short[] {}, N.top((short[]) null, 2));
    }

    @Test
    public void testTopCollection() {
        assertEquals(Arrays.asList(4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 2));
        assertEquals(Arrays.asList(2, 1), N.top(Arrays.asList(1, 2, 3, 4, 5), 2, Comparator.reverseOrder()));
        assertEquals(Arrays.asList(3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 2));
        assertEquals(Arrays.asList(3, 2), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 2, Comparator.reverseOrder()));

        assertEquals(Arrays.asList(3, 4, 5), N.top(Arrays.asList(1, 2, 3, null, 4, 5), 3));
        assertEquals(Arrays.asList(3, 4, 5), N.top(CommonUtil.toLinkedHashSet(1, 2, 3, null, 4, 5), 3));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 6, Comparator.reverseOrder()));
        assertEquals(Arrays.asList(2, 3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 6, Comparator.reverseOrder()));

        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 2, true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 2, Comparator.naturalOrder(), true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 1, 4, 2, true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 1, 4, 2, Comparator.naturalOrder(), true));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 6, Comparator.reverseOrder(), true));
        assertEquals(Arrays.asList(2, 3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 6, Comparator.reverseOrder(), true));
    }

    @Test
    public void testTopIntRangeRejectsNullComparator() {
        int[] arr = { 5, 1, 3, 4, 2 };
        assertThrows(IllegalArgumentException.class, () -> N.top(arr, 0, 5, 3, null));
    }

    @Test
    public void testTopIntRangeWithComparator_EmptyAndEdge() {
        // n=0 returns empty
        assertArrayEquals(new int[] {}, N.top(new int[] { 1, 2, 3 }, 0, 3, 0, Comparator.naturalOrder()));
        // null/empty array
        assertArrayEquals(new int[] {}, N.top((int[]) null, 0, 0, 2, Comparator.naturalOrder()));
        // n >= range size returns copy of range
        int[] arr = { 1, 2, 3 };
        int[] res = N.top(arr, 0, 3, 5, Comparator.naturalOrder());
        assertEquals(3, res.length);
    }

    @Test
    public void testTopNegativeN() {
        assertThrows(IllegalArgumentException.class, () -> N.top(new int[] { 1, 2, 3 }, -1));
    }

    @Test
    public void testTop_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.top(shortArray, -1));

        assertArrayEquals(new short[] {}, N.top(new short[] {}, 3));
        assertArrayEquals(new short[] {}, N.top(shortArray, 0));
        assertArrayEquals(shortArray.clone(), N.top(shortArray, 10));
        assertArrayEquals(new short[] { 3, 4, 5 }, N.top(shortArray, 3));
        assertArrayEquals(new short[] { 3, 4 }, N.top(shortArray, 1, 4, 2));
        assertArrayEquals(new short[] { 3, 4, 5 }, N.top(shortArray, 3, Comparator.naturalOrder()));
        assertArrayEquals(new short[] { 3, 4 }, N.top(shortArray, 1, 4, 2, Comparator.naturalOrder()));

        assertArrayEquals(new int[] { 3, 4, 5 }, N.top(intArray, 3));
        assertArrayEquals(new long[] { 3, 4, 5 }, N.top(longArray, 3));
        assertArrayEquals(new float[] { 3, 4, 5 }, N.top(floatArray, 3));
        assertArrayEquals(new double[] { 3, 4, 5 }, N.top(doubleArray, 3));
    }

    @Test
    public void testTop_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.top(stringArray, -1));

        List<String> result = N.top(stringArray, 3);
        assertEquals(3, result.size());
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));

        result = N.top(stringArray, 1, 4, 2);
        assertEquals(2, result.size());

        result = N.top(stringArray, 3, Comparator.naturalOrder());
        assertEquals(3, result.size());

        String[] arr = { "d", "b", "e", "a", "c" };
        result = N.top(arr, 3, true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(arr, 3, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(arr, 1, 4, 2, true);
        assertEquals(Arrays.asList("b", "e"), result);

        result = N.top(arr, 1, 4, 2, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("b", "e"), result);
    }

    @Test
    public void testTop_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.top(stringList, -1));

        List<String> result = N.top(stringList, 3);
        assertEquals(3, result.size());
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));

        result = N.top(stringList, 1, 4, 2);
        assertEquals(2, result.size());

        result = N.top(stringList, 3, Comparator.naturalOrder());
        assertEquals(3, result.size());

        List<String> list = Arrays.asList("d", "b", "e", "a", "c");
        result = N.top(list, 3, true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(list, 3, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(list, 1, 4, 2, true);
        assertEquals(Arrays.asList("b", "e"), result);

        result = N.top(list, 1, 4, 2, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("b", "e"), result);
    }

    @Test
    public void testPercentilesChar() {
        char[] sorted = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        Map<Percentage, Character> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
        assertTrue(percentiles.containsKey(Percentage._0_1));
        assertTrue(percentiles.containsKey(Percentage._1));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._99));
    }

    @Test
    public void testPercentilesByte() {
        byte[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Byte> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesShort() {
        short[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Short> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesInt() {
        int[] sorted = new int[100];
        for (int i = 0; i < 100; i++) {
            sorted[i] = i + 1;
        }
        Map<Percentage, Integer> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
        assertEquals(Integer.valueOf(2), percentiles.get(Percentage._1));
        assertEquals(Integer.valueOf(51), percentiles.get(Percentage._50));
        assertEquals(Integer.valueOf(100), percentiles.get(Percentage._99));
    }

    @Test
    public void testPercentilesLong() {
        long[] sorted = { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L };
        Map<Percentage, Long> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesFloat() {
        float[] sorted = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f };
        Map<Percentage, Float> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesDouble() {
        double[] sorted = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        Map<Percentage, Double> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesGeneric() {
        String[] sorted = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };
        Map<Percentage, String> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesList() {
        List<Integer> sorted = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Map<Percentage, Integer> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesIntArray() {
        int[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Integer> p = N.percentilesOfSorted(sorted);
        assertEquals(Integer.valueOf(6), p.get(Percentage._50));
        assertEquals(Integer.valueOf(10), p.get(Percentage._90));
        assertEquals(Integer.valueOf(1), p.get(Percentage._1));
        assertEquals(Integer.valueOf(10), p.get(Percentage._99));

        int[] single = { 5 };
        Map<Percentage, Integer> pSingle = N.percentilesOfSorted(single);
        assertEquals(Integer.valueOf(5), pSingle.get(Percentage._50));

        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new int[] {}));
    }

    @Test
    public void testPercentilesGenericList() {
        List<String> sorted = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        Map<Percentage, String> p = N.percentilesOfSorted(sorted);
        assertEquals("f", p.get(Percentage._50));
        assertEquals("j", p.get(Percentage._90));

        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(Collections.<String> emptyList()));

    }

    @Test
    public void testPercentilesEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new int[] {}));
    }

    @Test
    public void testPercentilesNullArray() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted((int[]) null));
    }

    @Test
    public void testPercentilesEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(Collections.emptyList()));
    }

    @Test
    public void testPercentiles_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new char[] {}));

        char[] sortedChars = { 'a', 'b', 'c', 'd', 'e' };
        Map<Percentage, Character> charPercentiles = N.percentilesOfSorted(sortedChars);
        assertNotNull(charPercentiles);
        assertEquals(Percentage.values().length, charPercentiles.size());

        byte[] sortedBytes = { 1, 2, 3, 4, 5 };
        Map<Percentage, Byte> bytePercentiles = N.percentilesOfSorted(sortedBytes);
        assertNotNull(bytePercentiles);
        assertEquals(Percentage.values().length, bytePercentiles.size());

        short[] sortedShorts = { 1, 2, 3, 4, 5 };
        Map<Percentage, Short> shortPercentiles = N.percentilesOfSorted(sortedShorts);
        assertNotNull(shortPercentiles);
        assertEquals(Percentage.values().length, shortPercentiles.size());

        int[] sortedInts = new int[100];
        for (int i = 0; i < 100; i++) {
            sortedInts[i] = i + 1;
        }
        Map<Percentage, Integer> intPercentiles = N.percentilesOfSorted(sortedInts);
        assertNotNull(intPercentiles);
        assertEquals(2, intPercentiles.get(Percentage._1).intValue());
        assertEquals(51, intPercentiles.get(Percentage._50).intValue());
        assertEquals(100, intPercentiles.get(Percentage._99).intValue());

        long[] sortedLongs = { 1L, 2L, 3L, 4L, 5L };
        Map<Percentage, Long> longPercentiles = N.percentilesOfSorted(sortedLongs);
        assertNotNull(longPercentiles);
        assertEquals(Percentage.values().length, longPercentiles.size());

        float[] sortedFloats = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Map<Percentage, Float> floatPercentiles = N.percentilesOfSorted(sortedFloats);
        assertNotNull(floatPercentiles);
        assertEquals(Percentage.values().length, floatPercentiles.size());

        double[] sortedDoubles = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Map<Percentage, Double> doublePercentiles = N.percentilesOfSorted(sortedDoubles);
        assertNotNull(doublePercentiles);
        assertEquals(Percentage.values().length, doublePercentiles.size());
    }

    @Test
    public void testPercentiles_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new String[] {}));

        String[] sortedStrings = { "a", "b", "c", "d", "e" };
        Map<Percentage, String> stringPercentiles = N.percentilesOfSorted(sortedStrings);
        assertNotNull(stringPercentiles);
        assertEquals(Percentage.values().length, stringPercentiles.size());
    }

    @Test
    public void testPercentiles_list() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(Collections.emptyList()));

        List<String> sortedList = Arrays.asList("a", "b", "c", "d", "e");
        Map<Percentage, String> listPercentiles = N.percentilesOfSorted(sortedList);
        assertNotNull(listPercentiles);
        assertEquals(Percentage.values().length, listPercentiles.size());
    }

    @Test
    public void testChainedOperations() {
        String[] data = { "apple", "banana", "apricot", "berry", "cherry", "date" };

        List<String> filtered = N.filter(data, s -> s.length() > 5);
        List<Character> mapped = N.map(filtered, s -> s.charAt(0));
        List<Character> distinct = N.distinct(mapped);

        assertEquals(3, distinct.size());
        assertTrue(distinct.contains('b'));
        assertTrue(distinct.contains('a'));
        assertTrue(distinct.contains('c'));
    }

    @Test
    public void testComplexPredicateCombinations() {
        Integer[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        Predicate<Integer> isEven = i -> i % 2 == 0;
        Predicate<Integer> isGreaterThan5 = i -> i > 5;
        Predicate<Integer> combined = i -> isEven.test(i) && isGreaterThan5.test(i);

        List<Integer> result = N.filter(numbers, combined);
        assertEquals(Arrays.asList(6, 8, 10), result);
    }

    @Test
    public void testUnicodeAndSpecialCharacters() {
        String[] unicodeStrings = { "café", "naïve", "résumé", "🎉", "😀" };

        List<String> filtered = N.filter(unicodeStrings, s -> s.contains("é"));
        assertEquals(2, filtered.size());

        List<Integer> lengths = N.map(unicodeStrings, String::length);
        assertEquals(Arrays.asList(4, 5, 6, 2, 2), lengths);
    }

    @Test
    public void testExtremeRanges() {
        int[] array = new int[100];
        for (int i = 0; i < 100; i++) {
            array[i] = i;
        }

        int[] filtered = N.filter(array, 0, 100, i -> i % 10 == 0);
        assertEquals(10, filtered.length);

        filtered = N.filter(array, 50, 51, i -> true);
        assertArrayEquals(new int[] { 50 }, filtered);

        filtered = N.filter(array, 99, 100, i -> true);
        assertArrayEquals(new int[] { 99 }, filtered);
    }

    @Test
    public void testWithCustomCollections() {
        CustomIterable<Integer> custom = new CustomIterable<>(Arrays.asList(1, 2, 3, 4, 5));

        List<Integer> filtered = N.filter(custom, i -> i % 2 == 0);
        assertEquals(Arrays.asList(2, 4), filtered);

        List<String> mapped = N.map(custom, i -> "num" + i);
        assertEquals(5, mapped.size());
    }

    @Test
    public void testLargeDatasetOperations() {
        int[] filtered = N.filter(largeIntArray, i -> i % 100 == 0);
        assertEquals(100, filtered.length);

        boolean[] mapped = N.mapToBoolean(largeIntList, i -> i % 2 == 0);
        assertEquals(10000, mapped.length);

        int count = N.count(largeIntArray, i -> i < 5000);
        assertEquals(5000, count);

        int[] manyDupes = new int[10000];
        Arrays.fill(manyDupes, 0, 5000, 1);
        Arrays.fill(manyDupes, 5000, 10000, 2);
        int[] distinct = N.distinct(manyDupes);
        assertEquals(2, distinct.length);
    }

    @Test
    public void testComplexPredicates() {
        class Product {
            String name;
            double price;
            String category;
            boolean inStock;

            Product(String name, double price, String category, boolean inStock) {
                this.name = name;
                this.price = price;
                this.category = category;
                this.inStock = inStock;
            }
        }

        Product[] products = { new Product("Laptop", 999.99, "Electronics", true), new Product("Mouse", 29.99, "Electronics", true),
                new Product("Desk", 299.99, "Furniture", false), new Product("Chair", 199.99, "Furniture", true),
                new Product("Monitor", 399.99, "Electronics", false) };

        List<Product> result = N.filter(products, p -> p.inStock && p.price < 500 && "Electronics".equals(p.category));
        assertEquals(1, result.size());
        assertEquals("Mouse", result.get(0).name);

        Map<String, List<Product>> byCategory = N.groupBy(products, p -> p.category);
        assertEquals(2, byCategory.size());
        assertEquals(3, byCategory.get("Electronics").size());
        assertEquals(2, byCategory.get("Furniture").size());

        Map<Boolean, Integer> stockCount = N.countBy(Arrays.asList(products), p -> p.inStock);
        assertEquals(Integer.valueOf(3), stockCount.get(true));
        assertEquals(Integer.valueOf(2), stockCount.get(false));
    }

    @Test
    public void testStatelessOperations() {
        int[] arr = { 1, 2, 3, 4, 5 };

        int[] result1 = N.filter(arr, i -> i > 2);
        int[] result2 = N.filter(arr, i -> i > 2);
        assertArrayEquals(result1, result2);

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, arr);
    }

    @Test
    public void testIteratorExhaustion() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Iterator<Integer> iter = list.iterator();

        assertEquals(Integer.valueOf(1), iter.next());

        List<Integer> result = N.filter(iter, i -> i > 1);
        assertEquals(Arrays.asList(2, 3), result);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testNullElementHandling() {
        String[] withNulls = { "a", null, "b", null, "c" };

        List<String> nonNulls = N.filter(withNulls, Objects::nonNull);
        assertEquals(Arrays.asList("a", "b", "c"), nonNulls);

        List<Integer> lengths = N.map(withNulls, s -> s == null ? -1 : s.length());
        assertEquals(Arrays.asList(1, -1, 1, -1, 1), lengths);

        String[] duplicatesWithNulls = { "a", null, "a", null, "b" };
        List<String> distinct = N.distinct(duplicatesWithNulls);
        assertEquals(3, distinct.size());
    }

    @Test
    public void testPrimitivePredicates() {
        char[] chars = { 'a', 'B', 'c', 'D', 'e' };
        char[] uppercase = N.filter(chars, Character::isUpperCase);
        assertArrayEquals(new char[] { 'B', 'D' }, uppercase);

        byte[] bytes = { -128, -1, 0, 1, 127 };
        byte[] positive = N.filter(bytes, b -> b > 0);
        assertArrayEquals(new byte[] { 1, 127 }, positive);

        float[] floats = { 1.5f, 2.0f, 2.5f, 3.0f, 3.5f };
        float[] integers = N.filter(floats, f -> f == (int) f);
        assertArrayEquals(new float[] { 2.0f, 3.0f }, integers, 0.001f);
    }

    @Test
    public void testNullHandling() {
        assertArrayEquals(new boolean[0], N.filter((boolean[]) null, b -> b));
        assertEquals(Collections.emptyList(), N.filter((String[]) null, s -> true));

        assertArrayEquals(new boolean[0], N.mapToBoolean((String[]) null, s -> true));
        assertEquals(Collections.emptyList(), N.map((String[]) null, s -> s));

        assertEquals(0, N.count((int[]) null, i -> true));
        assertEquals(0, N.count((Iterator<?>) null));

        assertArrayEquals(new boolean[0], N.distinct((boolean[]) null));
        assertEquals(Collections.emptyList(), N.distinct((String[]) null));
    }

    @Test
    public void testEmptyCollectionHandling() {
        String[] emptyArray = new String[0];
        assertEquals(Collections.emptyList(), N.filter(emptyArray, s -> true));
        assertEquals(Collections.emptyList(), N.map(emptyArray, s -> s));
        assertEquals(Collections.emptyList(), N.distinct(emptyArray));

        List<String> emptyList = Collections.emptyList();
        assertEquals(Collections.emptyList(), N.filter(emptyList, s -> true));
        assertEquals(Collections.emptyList(), N.map(emptyList, s -> s));
        assertEquals(Collections.emptyList(), N.distinct(emptyList));
    }

    @Test
    public void testSpecialNumberCases() {
        float[] floats = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f };
        float[] filtered = N.filter(floats, f -> !Float.isNaN(f) && Float.isFinite(f));
        assertArrayEquals(new float[] { 0.0f, -0.0f }, filtered, 0.001f);

        double[] doubles = { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -0.0 };
        int count = N.count(doubles, d -> Double.isInfinite(d));
        assertEquals(2, count);
    }

    @Test
    public void testFunctionalComposition() {
        String[] words = { "hello", "world", "java", "programming" };

        List<String> filtered = N.filter(words, s -> s.length() > 4);
        List<Integer> lengths = N.map(filtered.toArray(new String[0]), String::length);
        assertEquals(Arrays.asList(5, 5, 11), lengths);

        int[] wordLengths = N.mapToInt(words, String::length);
        int[] longLengths = N.filter(wordLengths, len -> len > 4);
        assertArrayEquals(new int[] { 5, 5, 11 }, longLengths);
    }

    @Test
    public void testConcurrentModificationScenarios() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

        assertThrows(ConcurrentModificationException.class, () -> N.filter(list, i -> {
            if (i == 3) {
                list.add(6);
            }
            return i % 2 == 0;
        }));

    }

    @Test
    public void testInvalidRangeFromIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, -1, 3, i -> true));
    }

    @Test
    public void testInvalidRangeToIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, 0, 10, i -> true));
    }

    @Test
    public void testInvalidRangeFromGreaterThanTo() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, 3, 2, i -> true));
    }

    @Test
    public void testPrimitiveArrayConversions() {
        long[] longs = { 1L, 2L, 3L, 4L, 5L };
        int[] ints = N.mapToInt(longs, l -> (int) (l * 2));
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, ints);

        int[] intArray = { 1, 2, 3 };
        long[] longArray = N.mapToLong(intArray, i -> i * 1000000L);
        assertArrayEquals(new long[] { 1000000L, 2000000L, 3000000L }, longArray);

        int[] scores = { 85, 90, 78, 92, 88 };
        double[] percentages = N.mapToDouble(scores, score -> score / 100.0);
        assertArrayEquals(new double[] { 0.85, 0.90, 0.78, 0.92, 0.88 }, percentages, 0.001);
    }

    @Test
    public void testWithDifferentCollectionTypes() {
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c"));
        List<String> upperLinked = N.map(linkedList, 0, 3, String::toUpperCase);
        assertEquals(Arrays.asList("A", "B", "C"), upperLinked);

        TreeSet<Integer> treeSet = new TreeSet<>(Arrays.asList(3, 1, 4, 1, 5, 9));
        List<Integer> filtered = N.filter(treeSet, i -> i > 3);
        assertEquals(Arrays.asList(4, 5, 9), filtered);

        ArrayDeque<String> deque = new ArrayDeque<>(Arrays.asList("first", "second", "third"));
        List<Integer> lengths = N.map(deque, String::length);
        assertEquals(Arrays.asList(5, 6, 5), lengths);
    }

    @Test
    public void testTakeWhile() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(2, 4), N.takeWhile(arr, IS_EVEN_INTEGER));
        assertEquals(List.of(2, 4), N.takeWhile(Arrays.asList(2, 4, 5, 6, 8), IS_EVEN_INTEGER));
        assertEquals(List.of(2, 4), N.takeWhile(Arrays.asList(2, 4, 5, 6, 8).iterator(), IS_EVEN_INTEGER));
        assertTrue(N.takeWhile((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
        assertEquals(List.of(1, 2, 3), N.takeWhile(integerArray, i -> i < 4));
        assertEquals(Collections.emptyList(), N.takeWhile(integerArray, i -> i < 1));
    }

    @Test
    public void testTakeWhileInclusive() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(arr, IS_EVEN_INTEGER));
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(Arrays.asList(2, 4, 5, 6, 8), IS_EVEN_INTEGER));
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(Arrays.asList(2, 4, 5, 6, 8).iterator(), IS_EVEN_INTEGER));
        assertEquals(List.of(1, 2, 3), N.takeWhileInclusive(integerArray, i -> i < 3));
        assertEquals(List.of(10), N.takeWhileInclusive(new Integer[] { 10, 1, 2 }, x -> x < 5));
        assertTrue(N.takeWhileInclusive((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testDropWhile() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(5, 6, 8), N.dropWhile(arr, IS_EVEN_INTEGER));
        assertEquals(List.of(5, 6, 8), N.dropWhile(Arrays.asList(2, 4, 5, 6, 8), IS_EVEN_INTEGER));
        assertEquals(List.of(5, 6, 8), N.dropWhile(Arrays.asList(2, 4, 5, 6, 8).iterator(), IS_EVEN_INTEGER));
        assertEquals(List.of(3, 4, 5), N.dropWhile(integerArray, i -> i < 3));
        assertEquals(Collections.emptyList(), N.dropWhile(integerArray, i -> i < 10));
        assertTrue(N.dropWhile((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testSkipUntil() {
        Integer[] arr = { 1, 3, 4, 5, 6 };
        assertEquals(List.of(4, 5, 6), N.skipUntil(arr, IS_EVEN_INTEGER));
        assertEquals(List.of(4, 5, 6), N.skipUntil(Arrays.asList(1, 3, 4, 5, 6), IS_EVEN_INTEGER));
        assertEquals(List.of(4, 5, 6), N.skipUntil(Arrays.asList(1, 3, 4, 5, 6).iterator(), IS_EVEN_INTEGER));
        assertEquals(Arrays.asList(4, 5), N.skipUntil(integerArray, i -> i > 3));
        assertTrue(N.skipUntil((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
        assertTrue(N.skipUntil(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testPrimitiveDistinct() {
        char[] chars = { 'a', 'b', 'a', 'c', 'b', 'c' };
        char[] distinctChars = N.distinct(chars);
        Arrays.sort(distinctChars);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, distinctChars);

        double[] doubles = { 1.1, 2.2, 1.1, 3.3, 2.2, 3.3 };
        double[] distinctDoubles = N.distinct(doubles);
        assertEquals(3, distinctDoubles.length);

        double[] special = { Double.NaN, 1.0, Double.POSITIVE_INFINITY, Double.NaN, 1.0 };
        double[] distinctSpecial = N.distinct(special);
        assertEquals(3, distinctSpecial.length);
    }

    @Test
    public void testAllMatch() {
        assertTrue(N.allMatch(new Integer[] { 2, 4, 6 }, IS_EVEN_INTEGER));
        assertTrue(N.allMatch(List.of(2, 4, 6), IS_EVEN_INTEGER));
        assertTrue(N.allMatch(List.of(2, 4, 6).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.allMatch(new Integer[] { 2, 3, 6 }, IS_EVEN_INTEGER));
        assertTrue(N.allMatch((Integer[]) null, IS_EVEN_INTEGER));
        assertTrue(N.allMatch(new Integer[0], i -> false));
        assertTrue(N.allMatch(new Integer[] { 1, 2, 3 }, i -> true));
        assertFalse(N.allMatch(new Integer[] { 1, 2, 3 }, i -> false));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(N.anyMatch(new Integer[] { 1, 3, 4 }, IS_EVEN_INTEGER));
        assertTrue(N.anyMatch(List.of(1, 3, 4), IS_EVEN_INTEGER));
        assertTrue(N.anyMatch(List.of(1, 3, 4).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.anyMatch(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER));
        assertFalse(N.anyMatch((Integer[]) null, IS_EVEN_INTEGER));
        assertTrue(N.anyMatch(integerArray, i -> i > 4));
        assertFalse(N.anyMatch(new Integer[0], i -> true));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(N.noneMatch(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER));
        assertTrue(N.noneMatch(List.of(1, 3, 5), IS_EVEN_INTEGER));
        assertTrue(N.noneMatch(List.of(1, 3, 5).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.noneMatch(new Integer[] { 1, 2, 5 }, IS_EVEN_INTEGER));
        assertTrue(N.noneMatch((Integer[]) null, IS_EVEN_INTEGER));
        assertTrue(N.noneMatch(integerArray, i -> i > 10));
        assertTrue(N.noneMatch(new Integer[0], i -> true));
        assertFalse(N.noneMatch(new Integer[] { 1, 2, 3 }, i -> true));
        assertTrue(N.noneMatch(new Integer[] { 1, 2, 3 }, i -> false));
    }

    @Test
    public void testAllTrue() {
        assertTrue(N.allTrue(new boolean[] { true, true, true }));
        assertFalse(N.allTrue(new boolean[] { true, false, true }));
        assertTrue(N.allTrue(null));
        assertTrue(N.allTrue(new boolean[0]));
    }

    @Test
    public void testBoundaryValues() {
        int maxSize = 1000;

        boolean[] allTrue = new boolean[maxSize];
        Arrays.fill(allTrue, true);
        assertTrue(N.allTrue(allTrue));
        assertFalse(N.anyFalse(allTrue));

        int[] numbers = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new int[0], N.filter(numbers, 3, 3, i -> true));
        assertEquals(0, N.count(numbers, 5, 5, i -> true));
    }

    @Test
    public void testAllFalse() {
        assertTrue(N.allFalse(new boolean[] { false, false, false }));
        assertFalse(N.allFalse(new boolean[] { false, true, false }));
        assertTrue(N.allFalse(null));
    }

    @Test
    public void testAnyTrue() {
        assertTrue(N.anyTrue(new boolean[] { false, true, false }));
        assertFalse(N.anyTrue(new boolean[] { false, false, false }));
        assertFalse(N.anyTrue(null));
    }

    @Test
    public void testAnyFalse() {
        assertTrue(N.anyFalse(new boolean[] { true, false, true }));
        assertFalse(N.anyFalse(new boolean[] { true, true, true }));
        assertFalse(N.anyFalse(null));
    }

    @Test
    public void testMemoryEfficientOperations() {
        int[] largeArray = new int[1000000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i;
        }

        final IntPredicate everyThousandth = i -> i % 1000 == 0;
        N.count(largeArray, everyThousandth); // Warm up the lambda and count implementation before measuring allocations.

        final java.lang.management.ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        final com.sun.management.ThreadMXBean allocationBean = threadBean instanceof com.sun.management.ThreadMXBean
                ? (com.sun.management.ThreadMXBean) threadBean
                : null;

        if (allocationBean != null && allocationBean.isThreadAllocatedMemorySupported() && !allocationBean.isThreadAllocatedMemoryEnabled()) {
            allocationBean.setThreadAllocatedMemoryEnabled(true);
        }

        final long threadId = Thread.currentThread().getId();
        final long startMemory = allocationBean == null ? -1 : allocationBean.getThreadAllocatedBytes(threadId);
        final int count = N.count(largeArray, everyThousandth);
        final long endMemory = allocationBean == null ? -1 : allocationBean.getThreadAllocatedBytes(threadId);

        assertEquals(1000, count);

        if (startMemory >= 0 && endMemory >= 0) {
            assertTrue((endMemory - startMemory) < 1000000, "Count allocated too much memory on the current thread");
        }
    }

    @Test
    public void testComplexMergeWithMultipleIterables() {
        List<List<Integer>> iterables = new ArrayList<>();
        iterables.add(Arrays.asList(1, 5, 9));
        iterables.add(Arrays.asList(2, 6, 10));
        iterables.add(Arrays.asList(3, 7, 11));
        iterables.add(Arrays.asList(4, 8, 12));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        List<Integer> result = N.merge(iterables, selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), result);
    }

    @Test
    public void testJsonTypeNullThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("{}", (com.landawn.abacus.type.Type<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> N.streamJson("[]", (com.landawn.abacus.type.Type<Object>) null));
    }

    @Test
    public void testUnzipIterable() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzipIterableWithSupplier() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        Pair<Set<String>, Set<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        }, HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), result.left());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result.right());
    }

    @Test
    public void testUnzipIterableWithSeparateSuppliers() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("a", 3));

        Pair<LinkedHashSet<String>, ArrayList<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        }, LinkedHashSet::new, ArrayList::new);

        assertTrue(result.left() instanceof LinkedHashSet);
        assertTrue(result.right() instanceof ArrayList);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b")), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzipIteratorWithSeparateSuppliers() {
        Iterator<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("a", 3)).iterator();

        Pair<LinkedHashSet<String>, ArrayList<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        }, LinkedHashSet::new, ArrayList::new);

        assertTrue(result.left() instanceof LinkedHashSet);
        assertTrue(result.right() instanceof ArrayList);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b")), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzipIterableComplexObjects() {
        List<String> strings = Arrays.asList("a:1", "b:2", "c:3");

        Pair<List<String>, List<Integer>> result = N.unzip(strings, (str, output) -> {
            String[] parts = str.split(":");
            output.setLeft(parts[0]);
            output.setRight(Integer.parseInt(parts[1]));
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzipComplexObjects() {
        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Sales"), new Person("Charlie", 35, "Marketing"));

        BiConsumer<Person, Pair<String, Integer>> unzipper = (person, pair) -> pair.set(person.getName(), person.getAge());

        Pair<List<String>, List<Integer>> result = N.unzip(people, unzipper);
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), result.left());
        assertEquals(Arrays.asList(25, 30, 35), result.right());
    }

    @Test
    public void testUnzip() {
        List<Pair<Integer, String>> pairs = Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"));
        Pair<List<Integer>, List<String>> result = N.unzip(pairs, (p, out) -> {
            out.setLeft(p.left());
            out.setRight(p.right());
        });
        assertEquals(Arrays.asList(1, 2, 3), result.left());
        assertEquals(Arrays.asList("a", "b", "c"), result.right());
    }

    @Test
    public void testUnzipWithTransformation() {
        List<String> items = Arrays.asList("a:1", "b:2", "c:3");
        Pair<List<String>, List<Integer>> result = N.unzip(items, (item, out) -> {
            String[] parts = item.split(":");
            out.setLeft(parts[0]);
            out.setRight(Integer.parseInt(parts[1]));
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzipIterableNull() {
        List<Pair<String, Integer>> pairs = null;

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipIterableEmpty() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipEmptyCollection() {
        List<Pair<String, Integer>> empty = Collections.emptyList();
        Pair<List<String>, List<Integer>> result = N.unzip(empty, (p, out) -> {
            out.setLeft(p.left());
            out.setRight(p.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipWithNullElements() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), null, Pair.of("c", 3));

        assertThrows(NullPointerException.class, () -> {
            N.unzip(pairs, (pair, output) -> {
                output.setLeft(pair.left());
                output.setRight(pair.right());
            });
        });
    }

    @Test
    public void testUnzippIterable() {
        List<Triple<String, Integer, Boolean>> triples = Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false), Triple.of("c", 3, true));

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.middle());
        assertEquals(Arrays.asList(true, false, true), result.right());
    }

    @Test
    public void testUnzippIterableWithSupplier() {
        List<Triple<String, Integer, Boolean>> triples = Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false));

        Triple<Set<String>, Set<Integer>, Set<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        }, HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList("a", "b")), result.left());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), result.middle());
        assertEquals(new HashSet<>(Arrays.asList(true, false)), result.right());
    }

    @Test
    public void testUnzippIterableWithSeparateSuppliers() {
        List<Triple<String, Integer, Boolean>> triples = Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false), Triple.of("a", 3, true));

        Triple<LinkedHashSet<String>, ArrayList<Integer>, ArrayDeque<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        }, LinkedHashSet::new, ArrayList::new, ArrayDeque::new);

        assertTrue(result.left() instanceof LinkedHashSet);
        assertTrue(result.middle() instanceof ArrayList);
        assertTrue(result.right() instanceof ArrayDeque);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b")), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.middle());
        assertEquals(Arrays.asList(true, false, true), new ArrayList<>(result.right()));
    }

    @Test
    public void testUnzippIteratorWithSeparateSuppliers() {
        Iterator<Triple<String, Integer, Boolean>> triples = Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false), Triple.of("a", 3, true))
                .iterator();

        Triple<LinkedHashSet<String>, ArrayList<Integer>, ArrayDeque<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        }, LinkedHashSet::new, ArrayList::new, ArrayDeque::new);

        assertTrue(result.left() instanceof LinkedHashSet);
        assertTrue(result.middle() instanceof ArrayList);
        assertTrue(result.right() instanceof ArrayDeque);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b")), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.middle());
        assertEquals(Arrays.asList(true, false, true), new ArrayList<>(result.right()));
    }

    @Test
    public void testUnzipp() {
        List<Triple<Integer, String, Double>> triples = Arrays.asList(Triple.of(1, "a", 1.0), Triple.of(2, "b", 2.0));
        Triple<List<Integer>, List<String>, List<Double>> result = N.unzip3(triples, (t, out) -> {
            out.setLeft(t.left());
            out.setMiddle(t.middle());
            out.setRight(t.right());
        });
        assertEquals(Arrays.asList(1, 2), result.left());
        assertEquals(Arrays.asList("a", "b"), result.middle());
        assertEquals(Arrays.asList(1.0, 2.0), result.right());
    }

    @Test
    public void testUnzippIterableNull() {
        List<Triple<String, Integer, Boolean>> triples = null;

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.middle().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzippIterableEmpty() {
        List<Triple<String, Integer, Boolean>> triples = new ArrayList<>();

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.middle().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testNestedGroupBy() {
        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Engineering"), new Person("Charlie", 25, "Sales"),
                new Person("David", 30, "Sales"));

        Map<String, List<Person>> byDept = N.groupBy(people, Person::getDepartment);

        Map<String, Map<Integer, List<Person>>> result = new HashMap<>();
        for (Map.Entry<String, List<Person>> entry : byDept.entrySet()) {
            result.put(entry.getKey(), N.groupBy(entry.getValue(), Person::getAge));
        }

        assertEquals(2, result.get("Engineering").size());
        assertEquals(1, result.get("Engineering").get(25).size());
        assertEquals("Alice", result.get("Engineering").get(25).get(0).getName());
    }

    @Test
    public void testGroupingFinishesMapsWithImmutableEntries() {
        final List<String> words = Arrays.asList("apple", "apricot", "banana");
        final Map<Character, Integer> counts = Map.of('a', 2, 'b', 1);
        final Map<Character, String> groups = Map.of('a', "apple,apricot", 'b', "banana");

        final ConcurrentSkipListMap<Character, Integer> iterableCounts = N.countBy(words, s -> s.charAt(0), ConcurrentSkipListMap::new);
        final ConcurrentSkipListMap<Character, Integer> iteratorCounts = N.countBy(words.iterator(), s -> s.charAt(0), ConcurrentSkipListMap::new);
        assertEquals(counts, iterableCounts);
        assertEquals(counts, iteratorCounts);
        assertEquals(groups, N.groupBy(words, s -> s.charAt(0), Collectors.joining(","), ConcurrentSkipListMap::new));
        assertEquals(groups, N.groupBy(words.iterator(), s -> s.charAt(0), Collectors.joining(","), ConcurrentSkipListMap::new));

        final List<String> empty = Collections.emptyList();
        assertEquals(Map.of(), N.countBy(empty, s -> s.charAt(0), Map::of));
        assertEquals(Map.of(), N.countBy(empty.iterator(), s -> s.charAt(0), Map::of));
        assertEquals(Map.of(), N.groupBy(empty, s -> s.charAt(0), Collectors.joining(","), Map::of));
        assertEquals(Map.of(), N.groupBy(empty.iterator(), s -> s.charAt(0), Collectors.joining(","), Map::of));
    }

    @Test
    public void testIterateArrayWithRange() {
        String[] arr = { "a", "b", "c", "d" };
        Iterator<String> iter = N.iterate(arr, 1, 3);
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(N.iterate(arr, 1, 1).hasNext());
    }

    @Test
    public void testComplexMapOperations() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("evens", Arrays.asList(2, 4, 6));
        map.put("odds", Arrays.asList(1, 3, 5));
        map.put("mixed", Arrays.asList(1, 2, 3));

        Iterator<Map.Entry<String, List<Integer>>> iter = N.iterate(map);
        int entryCount = 0;
        while (iter.hasNext()) {
            iter.next();
            entryCount++;
        }
        assertEquals(3, entryCount);
    }

    @Test
    public void testIterateArray() {
        String[] arr = { "a", "b" };
        Iterator<String> iter = N.iterate(arr);
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(N.iterate((String[]) null).hasNext());
        assertFalse(N.iterate(new String[0]).hasNext());
    }

    @Test
    public void testIterateMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Iterator<Map.Entry<String, Integer>> iter = N.iterate(map);
        int count = 0;
        while (iter.hasNext()) {
            Map.Entry<String, Integer> entry = iter.next();
            assertTrue(map.containsKey(entry.getKey()));
            assertEquals(map.get(entry.getKey()), entry.getValue());
            count++;
        }
        assertEquals(2, count);
        assertFalse(N.iterate((Map<String, Integer>) null).hasNext());
    }

    @Test
    public void testIterateIterable() {
        List<String> list = List.of("x", "y");
        Iterator<String> iter = N.iterate(list);
        assertEquals("x", iter.next());
        assertEquals("y", iter.next());
        assertFalse(iter.hasNext());
        assertFalse(N.iterate((Iterable<String>) null).hasNext());
    }

    @Test
    public void testIterateWithModification() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> iter = N.iterate(list);

        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());

        list.add("d");

        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void testIterateEach() {
        List<String> l1 = List.of("a", "b");
        List<String> l2 = List.of("c");
        List<String> l3 = Collections.emptyList();
        Collection<Iterable<String>> iterables = CommonUtil.toList(l1, l2, l3, null);

        List<ObjIterator<String>> resultIterators = N.iterateEach(iterables);
        assertEquals(4, resultIterators.size());

        Iterator<String> iter1 = resultIterators.get(0);
        assertEquals("a", iter1.next());
        assertEquals("b", iter1.next());
        assertFalse(iter1.hasNext());

        Iterator<String> iter2 = resultIterators.get(1);
        assertEquals("c", iter2.next());
        assertFalse(iter2.hasNext());

        Iterator<String> iter3 = resultIterators.get(2);
        assertFalse(iter3.hasNext());

        Iterator<String> iter4 = resultIterators.get(3);
        assertFalse(iter4.hasNext());

        assertTrue(N.iterateEach(null).isEmpty());
    }

    @Test
    public void testIterateAll() {
        List<String> l1 = List.of("a", "b");
        List<String> l2 = List.of("c");
        List<String> l3 = Collections.emptyList();
        Collection<Iterable<String>> iterables = CommonUtil.toList(l1, null, l2, l3);

        Iterator<String> combinedIter = N.iterateAll(iterables);
        assertEquals("a", combinedIter.next());
        assertEquals("b", combinedIter.next());
        assertEquals("c", combinedIter.next());
        assertFalse(combinedIter.hasNext());

        assertFalse(N.iterateAll(null).hasNext());
        assertFalse(N.iterateAll(Collections.emptyList()).hasNext());
    }

    @Test
    public void testIterateAllEmpty() {
        List<List<String>> empty = Arrays.asList(Collections.<String> emptyList(), Collections.<String> emptyList());

        ObjIterator<String> iter = N.iterateAll(empty);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterateAllMixed() {
        List<List<Integer>> mixed = Arrays.asList(Collections.<Integer> emptyList(), Arrays.asList(1, 2), Collections.<Integer> emptyList(),
                Arrays.asList(3, 4, 5), Collections.<Integer> emptyList());

        ObjIterator<Integer> iter = N.iterateAll(mixed);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDisjointWithDuplicates() {
        Integer[] a1 = { 1, 1, 2, 2, 3, 3 };
        Integer[] a2 = { 4, 4, 5, 5, 6, 6 };
        assertTrue(N.disjoint(a1, a2));

        Integer[] a3 = { 3, 3, 4, 4, 5, 5 };
        assertFalse(N.disjoint(a1, a3));
    }

    @Test
    public void testDisjointPerformance() {
        Set<Integer> bigSet = new HashSet<>();
        List<Integer> smallList = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            bigSet.add(i);
        }

        for (int i = 20000; i < 20010; i++) {
            smallList.add(i);
        }

        assertTrue(N.disjoint(bigSet, smallList));
        assertTrue(N.disjoint(smallList, bigSet));
    }

    @Test
    public void testDisjointArrays() {
        assertTrue(N.disjoint(new String[] { "a", "b" }, new String[] { "c", "d" }));
        assertFalse(N.disjoint(new String[] { "a", "b" }, new String[] { "b", "c" }));
        assertTrue(N.disjoint(null, new String[] { "a" }));
        assertTrue(N.disjoint(new String[] { "a" }, null));
        assertTrue(N.disjoint(new String[0], new String[] { "a" }));
    }

    @Test
    public void testDisjointCollections() {
        assertTrue(N.disjoint(List.of("a", "b"), Set.of("c", "d")));
        assertFalse(N.disjoint(Set.of("a", "b"), List.of("b", "c")));
        assertTrue(N.disjoint(null, List.of("a")));
        assertTrue(N.disjoint(Collections.emptySet(), List.of("a")));

        assertTrue(N.disjoint(new HashSet<>(List.of("a", "b")), new ArrayList<>(List.of("c", "d"))));
        assertFalse(N.disjoint(new ArrayList<>(List.of("c", "d", "a")), new HashSet<>(List.of("a", "b"))));
    }

    @Test
    public void testDisjointEmptyCollections() {
        assertTrue(N.disjoint(Collections.emptyList(), Collections.emptyList()));
        assertTrue(N.disjoint(integerList, Collections.emptyList()));
        assertTrue(N.disjoint(Collections.emptyList(), integerList));
    }

    @Test
    public void testDisjointWithNulls() {
        Object[] a1 = { 1, null, 3 };
        Object[] a2 = { 2, null, 4 };
        assertFalse(N.disjoint(a1, a2));

        Object[] a3 = { 2, 4, 6 };
        assertTrue(N.disjoint(a1, a3));
    }

    @Test
    public void test_json_optional() {
        final Map<String, Optional<Integer>> optionalMap = CommonUtil.asMap("a", Optional.of(12));
        assertEquals(optionalMap, N.fromJson(N.toJson(optionalMap), new TypeReference<Map<String, Optional<Integer>>>() {
        }.type()));

        final Map<String, OptionalDouble> doubleMap = CommonUtil.asMap("a", OptionalDouble.of(12));
        assertEquals(doubleMap, N.fromJson(N.toJson(doubleMap), new TypeReference<Map<String, OptionalDouble>>() {
        }.type()));

        final Map<String, Nullable<Integer>> nullableMap = CommonUtil.asMap("a", Nullable.of(12));
        assertEquals(nullableMap, N.fromJson(N.toJson(nullableMap), new TypeReference<Map<String, Nullable<Integer>>>() {
        }.type()));
    }

    @Test
    public void testJsonSerializationOfComplexObjects() {
        Map<String, Object> complex = new HashMap<>();
        complex.put("string", "value");
        complex.put("number", 42);
        complex.put("array", Arrays.asList(1, 2, 3));
        complex.put("nested", Collections.singletonMap("key", "value"));
        complex.put("null", null);

        String json = N.toJson(complex);
        Map<String, Object> deserialized = N.fromJson(json, Map.class);

        assertEquals("value", deserialized.get("string"));
        assertEquals(42, ((Number) deserialized.get("number")).intValue());
        assertEquals(Arrays.asList(1, 2, 3), deserialized.get("array"));
        assertEquals("value", ((Map) deserialized.get("nested")).get("key"));
        assertNull(deserialized.get("null"));
    }

    @Test
    public void streamJson_string_toClass() {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "," + getExpectedJsonForSampleBean(false) + "]";
        Stream<TestBean> stream = N.streamJson(jsonArray, Type.of(TestBean.class));
        List<TestBean> list = stream.toList();
        assertEquals(2, list.size());
        assertEquals(createSampleBean(), list.get(0));
        assertEquals(createSampleBean(), list.get(1));
    }

    @Test
    public void streamJson_file_toClass(@TempDir Path tempDir) throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        File inputFile = tempDir.resolve("input_array.json").toFile();
        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(jsonArray);
        }
        Stream<TestBean> stream = N.streamJson(inputFile, Type.of(TestBean.class));
        assertEquals(createSampleBean(), stream.first().orElse(null));
    }

    @Test
    public void streamJson_inputStream_toClass_autoClose() throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        final AtomicBoolean closed = new AtomicBoolean(false);
        InputStream inputStream = new ByteArrayInputStream(jsonArray.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                super.close();
                closed.set(true);
            }
        };

        try (Stream<TestBean> stream = N.streamJson(inputStream, true, Type.of(TestBean.class))) {
            assertEquals(createSampleBean(), stream.first().orElse(null));
        }
        assertTrue(closed.get(), "InputStream should be closed when stream is closed with autoClose=true");
    }

    @Test
    public void streamJson_reader_toClass_autoClose() throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        final AtomicBoolean closed = new AtomicBoolean(false);
        Reader reader = new StringReader(jsonArray) {
            @Override
            public void close() {
                super.close();
                closed.set(true);
            }
        };

        try (Stream<TestBean> stream = N.streamJson(reader, true, Type.of(TestBean.class))) {
            assertEquals(createSampleBean(), stream.first().orElse(null));
        }
        assertTrue(closed.get(), "Reader should be closed when stream is closed with autoClose=true");
    }

    @Test
    public void testStreamJson() {
        assertThrows(IllegalArgumentException.class, () -> N.streamJson(TEST_JSON_ARRAY, Type.of(Integer.class)));
    }

    @Test
    public void testStreamJson_InputStream_Type() throws java.io.IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        InputStream inputStream = new ByteArrayInputStream(jsonArray.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        com.landawn.abacus.util.stream.Stream<TestBean> stream = N.streamJson(inputStream, Type.of(TestBean.class));
        assertEquals(createSampleBean(), stream.first().orElse(null));
        inputStream.close();
    }

    @Test
    public void testStreamJson_Reader_Type() throws java.io.IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        Reader reader = new StringReader(jsonArray);
        com.landawn.abacus.util.stream.Stream<TestBean> stream = N.streamJson(reader, Type.of(TestBean.class));
        assertEquals(createSampleBean(), stream.first().orElse(null));
        reader.close();
    }

    @Test
    public void testSerialize5() {
        final List<Account> accounts = createAccountWithContact(Account.class, 100);
        final XmlDeserConfig cfg = XmlDeserConfig.create()
                .setElementType(Account.class)
                .setIgnoreUnmatchedProperty(true)
                .setIgnoredPropNames((Map<Class<?>, Set<String>>) null);

        final List<Account> fromAbacusXml = abacusXmlParser.deserialize(abacusXmlParser.serialize(accounts), cfg, List.class);
        assertEquals(CommonUtil.stringOf(accounts), CommonUtil.stringOf(fromAbacusXml));

        final List<Account> fromDom = abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(accounts), cfg, List.class);
        assertEquals(CommonUtil.stringOf(accounts), CommonUtil.stringOf(fromDom));
    }

    @Test
    public void formatJson_string() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String prettyJson = N.formatJson(uglyJson);
        assertTrue(prettyJson.contains("\n"));
        assertEquals(N.fromJson(uglyJson, Object.class), N.fromJson(prettyJson, Object.class));
    }

    @Test
    public void formatJson_string_withClass() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String prettyJson = N.formatJson(uglyJson, TestBean.class);
        assertEquals(getExpectedJsonForSampleBean(true), prettyJson);
    }

    @Test
    public void testFormatJson() {
        String compactJson = "{\"a\":1,\"b\":2}";
        String formatted = N.formatJson(compactJson);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    public void testFormatJsonWithTransferType() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        String json = N.toJson(map);

        String formatted = N.formatJson(json, Map.class);
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("John"));
    }

    @Test
    public void testFormatJson_Type() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String formatted = N.formatJson(uglyJson, Type.of(TestBean.class));
        assertNotNull(formatted);
        assertTrue(formatted.contains("testName"));
    }

    @Test
    public void testFormatJson_Config() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String formatted = N.formatJson(uglyJson, JsonSerConfig.create().setPrettyFormat(true));
        assertNotNull(formatted);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    public void testFormatJson_Config_Type() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String formatted = N.formatJson(uglyJson, JsonSerConfig.create(), Type.of(TestBean.class));
        assertNotNull(formatted);
        assertTrue(formatted.contains("testName"));
    }

    @Test
    public void testFormatXml() {
        String compactXml = "<root><a>1</a><b>2</b></root>";
        String formatted = N.formatXml(compactXml);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    public void testFormatXmlWithTransferType() {
        TestPerson person = new TestPerson("John", 30);
        String xml = N.toXml(person);

        String formatted = N.formatXml(xml, TestPerson.class);
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("John"));
    }

    @Test
    public void testFormatXml_Config_Class() {
        TestPerson person = new TestPerson("Alice", 25);
        String xml = N.toXml(person);
        XmlSerConfig config = XmlSerConfig.create().setPrettyFormat(true);
        String formatted = N.formatXml(xml, config, TestPerson.class);
        assertNotNull(formatted);
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("Alice"));
    }

    @Test
    public void testFormatXml_Config_Type() {
        TestPerson person = new TestPerson("Bob", 35);
        String xml = N.toXml(person);
        XmlSerConfig config = XmlSerConfig.create().setPrettyFormat(true);
        String formatted = N.formatXml(xml, config, Type.of(TestPerson.class));
        assertNotNull(formatted);
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("Bob"));
    }

    @Test
    public void test_xmlToJson() {
        final Account account = new Account();
        account.setFirstName("firstName1");
        account.setLastName("lastName1");

        final String xml = abacusXmlParser.serialize(account);
        final String json = jsonParser.serialize(account);
        assertNotNull(N.jsonToXml(json, Account.class));
        assertNotNull(N.xmlToJson(xml, Account.class));
    }

    @Test
    public void xmlToJson_string() {
        String xml = N.toXml(createSampleBean());
        String json = N.xmlToJson(xml);

        Map<String, Object> map = N.fromJson(json, Map.class);
        assertNotNull(map);
        assertEquals("testName", map.get("name"));
    }

    @Test
    public void testXml2Json() {
        String json = N.xmlToJson(TEST_XML);
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void test_xml2JSON_1() {
        final Account account = new Account();
        account.setFirstName("firstName1");
        account.setLastName("lastName1");

        final String xml = abacusXMLDOMParser.serialize(account);
        final String json = jsonParser.serialize(account);
        assertNotNull(N.jsonToXml(json, Account.class));
        assertNotNull(N.xmlToJson(xml, Account.class));
        assertNotNull(N.jsonToXml(json));
        assertNotNull(N.xmlToJson(xml));
    }

    @Test
    public void jsonToXml_string() {
        String json = N.toJson(createSampleBean());
        String xml = N.jsonToXml(json);

        MapEntity mapEntity = N.fromXml(xml, MapEntity.class);
        assertNotNull(mapEntity);
        assertEquals("testName", mapEntity.get("name"));
        assertEquals("123", mapEntity.get("value"));
    }

    @Test
    public void testJson2Xml() {
        String xml = N.jsonToXml(TEST_JSON);
        assertNotNull(xml);
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testLargeCollectionProcessing() {
        int size = 10000;
        List<Integer> largeList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            largeList.add(i);
        }

        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(largeList, 1000, 9000, sum::addAndGet);

        int expectedSum = 0;
        for (int i = 1000; i < 9000; i++) {
            expectedSum += i;
        }
        assertEquals(expectedSum, sum.get());
    }

    @Test
    public void testNestedForEachOperations() {
        List<String> outer = Arrays.asList("A", "B");
        List<Integer> inner = Arrays.asList(1, 2, 3);
        List<String> results = new ArrayList<>();

        N.forEach(outer, outerItem -> {
            N.forEach(inner, innerItem -> {
                N.forEach(0, innerItem, i -> {
                    results.add(outerItem + innerItem + i);
                });
            });
        });

        assertEquals(12, results.size());
    }

    @Test
    public void testExcludeAllToSet_emptyExclusionKeepsEncounterOrder() {
        final List<String> source = new ArrayList<>(Arrays.asList("c", "a", "b"));
        final List<String> expected = Arrays.asList("c", "a", "b");

        assertEquals(expected, new ArrayList<>(N.excludeAllToSet(source, Collections.emptyList())));
        assertEquals(expected, new ArrayList<>(N.excludeAllToSet(source, (Collection<?>) null)));
        assertEquals(expected, new ArrayList<>(N.excludeAllToSet(source, Arrays.asList("z"))));
        assertEquals(expected, new ArrayList<>(N.excludeAllToSet(source, Arrays.asList("y", "z"))));
        assertTrue(N.excludeAllToSet(source, Collections.emptyList()) instanceof LinkedHashSet);
        assertEquals(3, N.excludeAllToSet(source, Collections.emptyList()).size());

        // An unordered source still gets a plain HashSet.
        final Set<String> unordered = new HashSet<>(Arrays.asList("c", "a", "b"));
        assertFalse(N.excludeAllToSet(unordered, Collections.emptyList()) instanceof LinkedHashSet);
        assertEquals(3, N.excludeAllToSet(unordered, Collections.emptyList()).size());
    }

    @Test
    public void testMoveRange_outOfBoundsMessageIsNotArraySpecific() {
        final List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(list, 0, 2, 3));
        assertEquals("newPositionAfterMove 3 is out-of-bounds: [0, 1=(length/size - (toIndex - fromIndex))]", ex.getMessage());

        final IndexOutOfBoundsException arrayEx = assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new int[] { 1, 2, 3 }, 0, 2, 3));
        assertEquals("newPositionAfterMove 3 is out-of-bounds: [0, 1=(length/size - (toIndex - fromIndex))]", arrayEx.getMessage());
    }

    @Test
    public void testPercentilesOfSorted_keysAreExactlyThePercentageConstants() {
        final int[] sorted = new int[100];

        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i;
        }

        final java.util.Map<Percentage, Integer> percentiles = N.percentilesOfSorted(sorted);
        final List<Percentage> constants = Arrays.asList(Percentage.values());

        assertEquals(43, constants.size());
        assertEquals(43, percentiles.size());
        assertEquals(new java.util.LinkedHashSet<>(constants), new java.util.LinkedHashSet<>(percentiles.keySet()));
        // 10% is followed directly by 15%: there is no constant for every integer percent between 1% and 99%.
        assertEquals(constants.indexOf(Percentage._10) + 1, constants.indexOf(Percentage._15));
    }
}
