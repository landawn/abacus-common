package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Multiset.Entry;

public class MultisetConversionTest extends MultisetTestSupport {

    private static Multiset<String> synchronizedIdentityMultiset() {
        final Supplier<Map<String, Object>> supplier = () -> Collections.synchronizedMap(new IdentityHashMap<>());
        return new Multiset<>(supplier);
    }

    private static long sumOf(final Map<?, Integer> map) {
        long sum = 0;
        for (final Integer v : map.values()) {
            sum += v;
        }
        return sum;
    }

    private static void assertToArrayThrowsIse(final Multiset<String> ms) {
        try {
            ms.toArray();
            fail("toArray() should throw IllegalStateException");
        } catch (final IllegalStateException expected) {
            // expected
        } catch (final OutOfMemoryError oom) {
            // Caught here: JUnit treats an escaping OutOfMemoryError as unrecoverable.
            fail("toArray() threw OutOfMemoryError instead of IllegalStateException: " + oom);
        }

        try {
            ms.toArray(new String[0]);
            fail("toArray(T[]) should throw IllegalStateException");
        } catch (final IllegalStateException expected) {
            // expected
        } catch (final OutOfMemoryError oom) {
            fail("toArray(T[]) threw OutOfMemoryError instead of IllegalStateException: " + oom);
        }
    }

    @Test
    public void testC341_recommendedAlternativesPreserveAllCounts() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);

        final IdentityHashMap<String, Integer> identity = ms.toMap(size -> new IdentityHashMap<>());
        assertEquals(2, identity.size());
        assertEquals(3, sumOf(identity));

        long entrySum = 0;
        for (final Multiset.Entry<String> entry : ms.entrySet()) {
            entrySum += entry.count();
        }
        assertEquals(3, entrySum);
    }

    @Test
    public void testC341_emptyFallbackMultisetGivesEmptyMap() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        assertTrue(ms.toMap().isEmpty());
        assertTrue(ms.toImmutableMap().isEmpty());
    }

    @Test
    public void testC342_supplierGetsDistinctCountAndResultIsReturned() {
        final Multiset<String> ms = Multiset.of("b", "a", "a", "c");
        final int[] hint = { -1 };
        final TreeMap<String, Integer> target = new TreeMap<>();
        final TreeMap<String, Integer> result = ms.toMap(n -> {
            hint[0] = n;
            return target;
        });
        assertSame(target, result);
        assertEquals(3, hint[0]);
        assertEquals("{a=2, b=1, c=1}", result.toString());

        final ImmutableMap<String, Integer> imm = ms.toImmutableMap(n -> new LinkedHashMap<>());
        assertEquals(2, imm.get("a"));
        assertEquals(3, imm.size());

        assertTrue(new Multiset<String>().toImmutableMap(n -> new HashMap<>()).isEmpty());
    }

    @Test
    public void testC341_instantiableIdentityBackingKeepsItsType() {
        final Supplier<Map<String, Object>> supplier = IdentityHashMap::new;
        final Multiset<String> ms = new Multiset<>(supplier);
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);

        final Map<String, Integer> map = ms.toMap();
        assertTrue(map instanceof IdentityHashMap, map.getClass().getName());
        assertEquals(2, map.size());
        assertEquals(3, sumOf(map));
    }

    @Test
    public void testC341_nonCollidingFallbackStillCopies() {
        final Supplier<Map<String, Object>> supplier = () -> Collections.synchronizedMap(new HashMap<>());
        final Multiset<String> ms = new Multiset<>(supplier);
        ms.add("a", 2);
        ms.add(null, 1);
        ms.add("é", 3);

        final Map<String, Integer> map = ms.toMap();
        assertEquals(3, map.size());
        assertEquals(2, map.get("a"));
        assertEquals(1, map.get(null));
        assertEquals(3, map.get("é"));
        assertEquals(map, ms.toImmutableMap());
    }

    @Test
    public void testC342_nullSupplierStillRejected() {
        final Multiset<String> ms = Multiset.of("a");
        assertThrows(IllegalArgumentException.class, () -> ms.toMap((java.util.function.IntFunction<Map<String, Integer>>) null));
        assertThrows(IllegalArgumentException.class, () -> ms.toImmutableMap(null));
    }

    @Test
    public void testC342_toMapSupplierReturningNullOnNonEmptyMultisetThrowsNpe() {
        final Multiset<String> ms = Multiset.of("a", "a", "b");
        final NullPointerException e = assertThrows(NullPointerException.class, () -> ms.<Map<String, Integer>> toMap(n -> null));
        assertEquals("supplier returned null", e.getMessage());
    }

    @Test
    public void testC341_sortedByOccurrencesMessageQualifiesToMapRecommendation() {
        final Supplier<Map<String, Object>> supplier = IdentityHashMap::new;
        final Multiset<String> ms = new Multiset<>(supplier);
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);

        final IllegalStateException e = assertThrows(IllegalStateException.class, ms::toMapSortedByOccurrences);
        // toMap() is only lossless when the backing map's type can be instantiated; the message must say so
        // instead of promising that toMap() always preserves the backing map's equivalence.
        assertFalse(e.getMessage().contains("which preserve the backing map's own equivalence"), e.getMessage());
        assertTrue(e.getMessage().contains("instantiated"), e.getMessage());
        assertTrue(e.getMessage().contains("toMap(IntFunction)"), e.getMessage());
        assertTrue(e.getMessage().contains("entrySet()"), e.getMessage());
    }

    // toArray

    @Test
    public void testToArray() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        Object[] array = multiset.toArray();
        assertEquals(3, array.length);
    }

    @Test
    public void testToArray_Empty() {
        Object[] array = multiset.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_empty() {
        Multiset<String> multiset = new Multiset<>();
        Object[] array = multiset.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_Generic() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");

        String[] arraySmall = new String[2];
        String[] resultSmall = multiset.toArray(arraySmall);
        assertNotSame(arraySmall, resultSmall);
        assertEquals(4, resultSmall.length);
        List<String> listSmall = Arrays.asList(resultSmall);
        assertEquals(2, Collections.frequency(listSmall, "a"));
        assertEquals(1, Collections.frequency(listSmall, "b"));
        assertEquals(1, Collections.frequency(listSmall, "c"));

        String[] arrayExact = new String[4];
        String[] resultExact = multiset.toArray(arrayExact);
        assertSame(arrayExact, resultExact);
        List<String> listExact = Arrays.asList(resultExact);
        assertEquals(2, Collections.frequency(listExact, "a"));
        assertEquals(1, Collections.frequency(listExact, "b"));
        assertEquals(1, Collections.frequency(listExact, "c"));

        String[] arrayLarge = new String[6];
        String[] resultLarge = multiset.toArray(arrayLarge);
        assertSame(arrayLarge, resultLarge);
        List<String> listLarge = Arrays.asList(resultLarge);
        assertEquals(2, Collections.frequency(listLarge.subList(0, 4), "a"));
        assertEquals(1, Collections.frequency(listLarge.subList(0, 4), "b"));
        assertEquals(1, Collections.frequency(listLarge.subList(0, 4), "c"));
        if (arrayLarge.length > multiset.size()) {
            Arrays.fill(arrayLarge, null);
            multiset.toArray(arrayLarge);
            assertNull(arrayLarge[4]);
            assertNull(arrayLarge[5]);
        }
    }

    @Test
    public void testToArray_Generic_emptyMultiset() {
        Multiset<String> multiset = new Multiset<>();
        String[] a = new String[0];
        String[] result = multiset.toArray(a);
        assertSame(a, result);
        assertEquals(0, result.length);

        String[] b = new String[5];
        Arrays.fill(b, "test");
        String[] resultB = multiset.toArray(b);
        assertSame(b, resultB);
        assertNull(b[0]);
    }

    @Test
    public void testToArray_Generic_nullArray() {
        Multiset<String> multiset = Multiset.of("a");
        // Collection.toArray(T[]) specifies NullPointerException.
        assertThrows(NullPointerException.class, () -> multiset.toArray((String[]) null));
    }

    @Test
    public void testToArray_rejectsUnrepresentableOccurrenceCountBeforeAllocation() {
        final Multiset<String> multiset = new Multiset<>();
        multiset.setCount("a", Integer.MAX_VALUE);
        multiset.setCount("b", 1);

        assertThrows(IllegalStateException.class, multiset::toArray);
        assertThrows(IllegalStateException.class, () -> multiset.toArray(new String[0]));
    }

    @Test
    public void testToArray_Typed() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        String[] array = multiset.toArray(new String[0]);
        assertEquals(3, array.length);
    }

    @Test
    public void testToArray_Typed_ExactSize() {
        multiset.add("a", 2);
        String[] array = new String[2];
        String[] result = multiset.toArray(array);
        assertEquals(array, result);
        assertEquals(2, result.length);
    }

    @Test
    public void testToArray_Typed_Null() {
        assertThrows(NullPointerException.class, () -> multiset.toArray((String[]) null));
    }

    @Test
    public void testC348_toArraySmallAndEmptyUnchanged() {
        final Multiset<String> ms = Multiset.of("a", "a", null);
        assertEquals(3, ms.toArray().length);
        assertEquals(0, new Multiset<String>().toArray().length);

        final String[] big = { "x", "x", "x", "x", "x" };
        final String[] out = ms.toArray(big);
        assertSame(big, out);
        assertEquals(null, out[3]);
    }

    @Test
    public void testToArrayTyped() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        String[] array = multiset.toArray(new String[0]);
        assertEquals(3, array.length);

        List<String> arrayList = Arrays.asList(array);
        assertEquals(2, Collections.frequency(arrayList, "a"));
        assertEquals(1, Collections.frequency(arrayList, "b"));

        String[] largeArray = multiset.toArray(new String[10]);
        assertEquals(10, largeArray.length);
        assertNull(largeArray[3]);

        assertThrows(NullPointerException.class, () -> multiset.toArray((String[]) null));
    }

    @Test
    public void testC348_toArrayAboveIntRangeThrowsIse() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", Integer.MAX_VALUE);
        ms.add(null, Integer.MAX_VALUE);
        assertToArrayThrowsIse(ms);
    }

    @Test
    public void testC348_toArrayAtIntegerMaxValueThrowsIse() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", Integer.MAX_VALUE);
        assertToArrayThrowsIse(ms);
    }

    @Test
    public void testC348_toArrayJustAboveVmLimitThrowsIse() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("é", Integer.MAX_VALUE - 7);
        assertToArrayThrowsIse(ms);
    }

    // toImmutableMap

    @Test
    public void testToImmutableMap() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        ImmutableMap<String, Integer> map = multiset.toImmutableMap();
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
    }

    @Test
    public void testToImmutableMap_Empty() {
        ImmutableMap<String, Integer> map = multiset.toImmutableMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToImmutableMap_WithSupplier() {
        multiset.add("a", 3);
        ImmutableMap<String, Integer> map = multiset.toImmutableMap(HashMap::new);
        assertEquals(1, map.size());
    }

    @Test
    public void testToImmutableMapIsUnmodifiable() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        ImmutableMap<String, Integer> map = multiset.toImmutableMap();
        assertThrows(UnsupportedOperationException.class, () -> map.put("c", 1));
    }

    @Test
    public void testC342_toImmutableMapSupplierReturningNullThrowsNpe() {
        final Multiset<String> empty = new Multiset<>();
        NullPointerException e = assertThrows(NullPointerException.class, () -> empty.toImmutableMap(n -> null));
        assertEquals("mapSupplier returned null", e.getMessage());

        final Multiset<String> nonEmpty = Multiset.of("中", "中");
        e = assertThrows(NullPointerException.class, () -> nonEmpty.toImmutableMap(n -> null));
        assertEquals("mapSupplier returned null", e.getMessage());
    }

    @Test
    public void testToImmutableMapWithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        ImmutableMap<String, Integer> immutableMap = multiset.toImmutableMap(HashMap::new);
        assertEquals(2, immutableMap.size());
        assertEquals(Integer.valueOf(3), immutableMap.get("a"));
        assertEquals(Integer.valueOf(2), immutableMap.get("b"));
    }

    @Test
    public void testC341_toImmutableMapThrowsWhenFallbackHashMapWouldCollapseCounts() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);

        assertThrows(IllegalStateException.class, ms::toImmutableMap);
    }

    // toMap

    @Test
    public void testToMap() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        Map<String, Integer> map = multiset.toMap();
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testToMap_Empty() {
        Map<String, Integer> map = multiset.toMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMap_empty() {
        Multiset<String> multiset = new Multiset<>();
        Map<String, Integer> map = multiset.toMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMap_WithLinkedHashMapSupplier() {
        multiset.add("b", 2);
        multiset.add("a", 3);
        Map<String, Integer> map = multiset.toMap(LinkedHashMap::new);
        assertEquals(2, map.size());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testToMap_WithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        Map<String, Integer> map = multiset.toMap(HashMap::new);
        assertEquals(2, map.size());
        assertTrue(map instanceof HashMap);
    }

    // U22-03: toMap() of a ConcurrentSkipListMap backing yields a ConcurrentSkipListMap
    @Test
    public void testU2203_toMapOfAConcurrentSkipListMapBackingYieldsAConcurrentSkipListMap() {
        final Supplier<Map<String, Object>> concurrentSorted = () -> new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        final Multiset<String> ms = new Multiset<>(concurrentSorted);
        ms.add("b", 2);
        ms.add("A", 1);
        ms.add("a", 1);

        final Map<String, Integer> map = ms.toMap();
        assertEquals(ConcurrentSkipListMap.class, map.getClass());
        assertSame(String.CASE_INSENSITIVE_ORDER, ((SortedMap<String, Integer>) map).comparator());
        assertEquals(List.of("A", "b"), new ArrayList<>(map.keySet()));
        assertEquals(Integer.valueOf(2), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("B"));

        // a TreeMap backing still yields a TreeMap with the same comparator
        final Comparator<String> reverse = Comparator.reverseOrder();
        final Supplier<Map<String, Object>> sorted = () -> new TreeMap<>(reverse);
        final Multiset<String> tree = new Multiset<>(sorted);
        tree.add("a");
        tree.add("b");
        final Map<String, Integer> treeMap = tree.toMap();
        assertEquals(TreeMap.class, treeMap.getClass());
        assertSame(reverse, ((SortedMap<String, Integer>) treeMap).comparator());
        assertEquals(List.of("b", "a"), new ArrayList<>(treeMap.keySet()));
    }

    @Test
    public void testToMapWithSupplier() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        TreeMap<String, Integer> map = multiset.toMap(size -> new TreeMap<>());
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    // toMapSortedByKey

    @Test
    public void testToMapSortedByKey() {
        multiset.add("c", 1);
        multiset.add("a", 1);
        multiset.add("b", 1);
        Map<String, Integer> map = multiset.toMapSortedByKey(String::compareTo);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals("a", keys.get(0));
        assertEquals("b", keys.get(1));
        assertEquals("c", keys.get(2));
    }

    @Test
    public void testToMapSortedByKey_Empty() {
        Map<String, Integer> map = multiset.toMapSortedByKey(String::compareTo);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMapSortedByKey_ReverseOrder() {
        multiset.add("c", 1);
        multiset.add("a", 3);
        multiset.add("b", 2);
        Map<String, Integer> map = multiset.toMapSortedByKey(Comparator.reverseOrder());
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals("c", keys.get(0));
        assertEquals("b", keys.get(1));
        assertEquals("a", keys.get(2));
    }

    // toMapSortedByOccurrences

    @Test
    public void testToMapSortedByOccurrences() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Map<String, Integer> map = multiset.toMapSortedByOccurrences();

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals("b", keys.get(0));
        assertEquals("c", keys.get(1));
        assertEquals("a", keys.get(2));
    }

    @Test
    public void testToMapSortedByOccurrences_AscendingOrder() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Map<String, Integer> map = multiset.toMapSortedByOccurrences(Comparator.naturalOrder());
        List<Integer> values = new ArrayList<>(map.values());
        assertEquals(Integer.valueOf(1), values.get(0));
        assertEquals(Integer.valueOf(2), values.get(1));
        assertEquals(Integer.valueOf(3), values.get(2));
    }

    @Test
    public void testToMapSortedByOccurrences_Comparator() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("c", 3);
        multiset.add("a", 1);
        multiset.add("b", 2);

        Map<String, Integer> sortedMapDesc = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
        Iterator<Map.Entry<String, Integer>> itDesc = sortedMapDesc.entrySet().iterator();
        assertEquals("c", itDesc.next().getKey());
        assertEquals("b", itDesc.next().getKey());
        assertEquals("a", itDesc.next().getKey());
        assertFalse(itDesc.hasNext());
    }

    @Test
    public void testToMapSortedByOccurrences_DescendingOrder() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Map<String, Integer> map = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
        List<Integer> values = new ArrayList<>(map.values());
        assertEquals(Integer.valueOf(3), values.get(0));
        assertEquals(Integer.valueOf(2), values.get(1));
        assertEquals(Integer.valueOf(1), values.get(2));
    }

    @Test
    public void testToMapSortedByOccurrences_empty() {
        Multiset<String> multiset = new Multiset<>();
        Map<String, Integer> map = multiset.toMapSortedByOccurrences();
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testToMapSortedByOccurrences_WithComparator() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Map<String, Integer> map = multiset.toMapSortedByOccurrences((i1, i2) -> i2.compareTo(i1));

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals("a", keys.get(0));
        assertEquals("c", keys.get(1));
        assertEquals("b", keys.get(2));
    }

    @Test
    public void testToMapSortedByOccurrencesRejectsNullComparatorWhenEmpty() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new Multiset<String>().toMapSortedByOccurrences(null));
    }

    @Test
    public void testToMapSortedByOccurrencesWithComparator() {
        multiset.add("a", 1);
        multiset.add("b", 3);
        multiset.add("c", 2);

        Map<String, Integer> sorted = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(sorted.entrySet());

        assertEquals(3, entries.get(0).getValue().intValue());
        assertEquals(2, entries.get(1).getValue().intValue());
        assertEquals(1, entries.get(2).getValue().intValue());
    }

    @Test
    public void testC342_toMapSupplierReturningNullOnEmptyMultisetThrowsNpe() {
        final Multiset<String> ms = new Multiset<>();
        final NullPointerException e = assertThrows(NullPointerException.class, () -> ms.<Map<String, Integer>> toMap(n -> null));
        assertEquals("supplier returned null", e.getMessage());
    }

    @Test
    public void testC341_toMapThrowsWhenFallbackHashMapWouldCollapseCounts() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);
        assertEquals(2, ms.countOfDistinctElements());
        assertEquals(3, ms.sumOfOccurrences());

        final IllegalStateException e = assertThrows(IllegalStateException.class, ms::toMap);
        assertTrue(e.getMessage().contains("toMap(IntFunction)"), e.getMessage());
        assertTrue(e.getMessage().contains("entrySet()"), e.getMessage());
    }

    @Test
    public void testToString() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        String str = multiset.toString();
        assertNotNull(str);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
    }

    @Test
    public void testToString_Empty() {
        String str = multiset.toString();
        assertNotNull(str);
    }

    @Test
    public void testC341_unicodeElementsCollapseDetected() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        ms.add(new String("日本😀"), 4);
        ms.add(new String("日本😀"), 5);

        assertThrows(IllegalStateException.class, ms::toMap);
    }
}
