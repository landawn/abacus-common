package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.Test;

public class MapsBackingMapTest extends MapsTestSupport {

    private record Box(int v) {
    }

    // A named class whose constructor puts an entry: its reflective new instance is pre-filled like a double-brace map's.
    public static class G117PrefillingMap extends HashMap<String, Integer> {
        private static final long serialVersionUID = 1L;

        public G117PrefillingMap() {
            put("dflt", 0);
        }
    }

    // Empty anonymous maps also use a fallback; sorted maps still keep their comparator.
    @Test
    public void testNewTargetMap_anonymousFallbackAndSortedComparator() {
        final Map<String, Integer> m = new LinkedHashMap<>() {
            private static final long serialVersionUID = 1L;
        };
        m.put("y", 2);
        m.put("z", 3);

        final Map<String, Integer> filtered = Maps.filter(m, (k, v) -> v > 2);
        assertSame(LinkedHashMap.class, filtered.getClass());
        assertEquals(CommonUtil.asMap("z", 3), filtered);
        Maps.replaceKeys(m, k -> k.toUpperCase());
        assertEquals("{Y=2, Z=3}", m.toString());

        final TreeMap<String, Integer> sorted = new TreeMap<>(Comparator.reverseOrder());
        sorted.put("a", 1);
        sorted.put("b", 2);
        final Map<String, Integer> sortedResult = Maps.filter(sorted, (k, v) -> true);
        assertSame(TreeMap.class, sortedResult.getClass());
        assertSame(sorted.comparator(), ((TreeMap<String, Integer>) sortedResult).comparator());
        assertEquals("{b=2, a=1}", sortedResult.toString());
    }

    // copyOf

    // U16-06: BiMap.copyOf(ConcurrentSkipListMap) mirrors the concurrent sorted map on both sides
    @Test
    public void testU1606_copyOfConcurrentSkipListMapMirrorsOnBothSides() {
        final Comparator<String> reverse = Comparator.reverseOrder();
        final ConcurrentSkipListMap<String, Integer> source = new ConcurrentSkipListMap<>(reverse);
        source.put("a", 1);
        source.put("c", 3);
        source.put("b", 2);

        final BiMap<String, Integer> copy = BiMap.copyOf(source);
        assertEquals(ConcurrentSkipListMap.class, copy.keyMap.getClass());
        assertSame(reverse, ((SortedMap<String, Integer>) copy.keyMap).comparator());
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(copy.keySet()));
        assertEquals(LinkedHashMap.class, copy.valueMap.getClass());

        // copy() rebuilds the key side through the supplier: same kind, same comparator (was a TreeMap)
        final BiMap<String, Integer> copyOfCopy = copy.copy();
        assertEquals(ConcurrentSkipListMap.class, copyOfCopy.keyMap.getClass());
        assertSame(reverse, ((SortedMap<String, Integer>) copyOfCopy.keyMap).comparator());
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(copyOfCopy.keySet()));

        // the inverse view shares the live maps, and its copy swaps the suppliers
        final BiMap<Integer, String> inverse = copy.inverse();
        assertSame(copy.keyMap, inverse.valueMap);
        assertEquals(ConcurrentSkipListMap.class, inverse.copy().valueMap.getClass());
        assertEquals(LinkedHashMap.class, inverse.copy().keyMap.getClass());

        // independence
        copyOfCopy.put("d", 4);
        assertFalse(copy.containsKey("d"));
        assertFalse(source.containsKey("d"));
    }

    @Test
    public void testU1606_copyOfConcurrentSkipListMapWithNonComparableKeysCopiesWithoutCCE() {
        final Comparator<Box> byV = Comparator.comparingInt(Box::v);
        final ConcurrentSkipListMap<Box, String> source = new ConcurrentSkipListMap<>(byV);
        source.put(new Box(2), "two");
        source.put(new Box(1), "one");

        final BiMap<Box, String> copy = BiMap.copyOf(source);
        final BiMap<Box, String> copyOfCopy = copy.copy();
        assertEquals(Arrays.asList(new Box(1), new Box(2)), new ArrayList<>(copyOfCopy.keySet()));
        assertEquals(ConcurrentSkipListMap.class, copyOfCopy.keyMap.getClass());
        assertEquals(Arrays.asList("one", "two"), new ArrayList<>(copy.inverse().copy().keySet()));
    }

    @Test
    public void testU1606_copyOfTreeMapStillYieldsTreeMapOnBothSides() {
        final Comparator<String> reverse = Comparator.reverseOrder();
        final TreeMap<String, Integer> tree = new TreeMap<>(reverse);
        tree.put("a", 1);
        tree.put("b", 2);

        final BiMap<String, Integer> fromTree = BiMap.copyOf(tree);
        assertEquals(TreeMap.class, fromTree.keyMap.getClass());
        assertEquals(TreeMap.class, fromTree.copy().keyMap.getClass());
        assertSame(reverse, ((SortedMap<String, Integer>) fromTree.copy().keyMap).comparator());
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(fromTree.copy().keySet()));
    }

    // U16-01: BiMap.copyOf / ImmutableBiMap.copyOf of an unconstructible ordered source keep its order
    @Test
    public void testU1601_copyOfUnconstructibleOrderedSourceKeepsOrder() {
        final List<String> keys = Arrays.asList("zeta", "alpha", "mu", "beta", "omega");
        final Map<String, Integer> unmodifiable = Collections.unmodifiableMap(ordered(keys.toArray(new String[0])));

        final BiMap<String, Integer> copy = BiMap.copyOf(unmodifiable);
        assertEquals(keys, new ArrayList<>(copy.keySet()));
        assertEquals(LinkedHashMap.class, copy.keyMap.getClass());
        assertEquals(LinkedHashMap.class, copy.valueMap.getClass());
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), new ArrayList<>(copy.inverse().keySet()));
        // the copy's own copies keep the order too (the suppliers are derived from the LinkedHashMap)
        assertEquals(keys, new ArrayList<>(copy.copy().keySet()));

        final ImmutableBiMap<String, Integer> immutable = ImmutableBiMap.copyOf(unmodifiable);
        assertEquals(keys, new ArrayList<>(immutable.keySet()));

        assertEquals(keys, new ArrayList<>(BiMap.copyOf(Collections.synchronizedMap(ordered(keys.toArray(new String[0])))).keySet()));
        assertEquals(keys, new ArrayList<>(ImmutableBiMap.copyOf(ImmutableMap.copyOf(ordered(keys.toArray(new String[0])))).keySet()));
    }

    @Test
    public void testNewOrderingMap_SortedAndUnmodifiableMap() {
        TreeMap<String, Integer> sorted = new TreeMap<>(Comparator.reverseOrder());
        sorted.put("b", 2);
        sorted.put("a", 1);

        Map<?, ?> sortedResult = Maps.newOrderingMap(sorted);
        Map<?, ?> fallbackResult = Maps.newOrderingMap(Collections.unmodifiableMap(new LinkedHashMap<>(Map.of("x", 1))));

        assertTrue(sortedResult instanceof LinkedHashMap);
        assertEquals(LinkedHashMap.class, fallbackResult.getClass());
    }

    // newTargetMap

    @Test
    public void testNewTargetMap_PreservesSortedComparator() {
        TreeMap<String, Integer> source = new TreeMap<>(Comparator.reverseOrder());
        source.put("b", 2);
        source.put("a", 1);

        Map<?, ?> target = Maps.newTargetMap(source, 4);

        assertTrue(target instanceof TreeMap);
        assertEquals(source.comparator(), ((TreeMap<?, ?>) target).comparator());
    }

    @Test
    public void testNewTargetMapAndNewOrderingMap_FallbackForNonInstantiableMap() {
        Map<String, Integer> source = new NonInstantiableHashMap<>("blocked");
        source.put("a", 1);

        Map<?, ?> target = Maps.newTargetMap(source, 3);
        Map<?, ?> ordering = Maps.newOrderingMap(source);

        // LinkedHashMap since the 2026-09-24 review (C-377): the HashMap fallback dropped the template's order
        assertEquals(LinkedHashMap.class, target.getClass());
        assertTrue(target.isEmpty());
        assertEquals(LinkedHashMap.class, ordering.getClass());
        assertTrue(ordering.isEmpty());
    }

    @Test
    public void testC377_nullTemplateAndCapacityClassesUnchanged() {
        assertEquals(HashMap.class, Maps.newTargetMap(null, 4).getClass());
        assertEquals(HashMap.class, Maps.newTargetMap(new HashMap<>(), 4).getClass());
        assertEquals(LinkedHashMap.class, Maps.newTargetMap(new LinkedHashMap<>(), 4).getClass());
        assertEquals(java.util.IdentityHashMap.class, Maps.newTargetMap(new java.util.IdentityHashMap<>(), 4).getClass());
        assertEquals(ConcurrentHashMap.class, Maps.newTargetMap(new ConcurrentHashMap<>(), 4).getClass());
    }

    // Pre-filled template: symmetricDifference, transpose rows and a named pre-filling class get an empty target map too.
    @Test
    public void testNewTargetMap_prefilledTemplate_otherCallers() {
        final Map<String, Integer> m = g053DoubleBraceMapWithXRemoved();
        assertEquals(CommonUtil.asSet("z", "w"), Maps.symmetricDifference(m, CommonUtil.asMap("y", 2, "w", 9)).keySet());

        final Map<String, List<Integer>> rows = new LinkedHashMap<>() {
            private static final long serialVersionUID = 1L;

            {
                put("q", Arrays.asList(9));
            }
        };
        rows.remove("q");
        rows.put("a", Arrays.asList(1, 2));
        rows.put("b", Arrays.asList(3));
        assertEquals(Arrays.asList(CommonUtil.asMap("a", 1, "b", 3), CommonUtil.asMap("a", 2)), Maps.transpose(rows));

        final G117PrefillingMap named = new G117PrefillingMap();
        named.remove("dflt");
        named.put("y", 2);
        assertEquals(CommonUtil.asMap("y", 2), Maps.filter(named, (k, v) -> true));
        Maps.replaceKeys(named, k -> k.toUpperCase());
        assertEquals(CommonUtil.asMap("Y", 2), named);
    }

    @Test
    public void testU1601_valueToKeyMapIsHashMapOnlyForAHashMapSource() {
        final BiMap<String, Integer> fromHash = BiMap.copyOf(new HashMap<>(ordered("a", "b")));
        assertEquals(HashMap.class, fromHash.keyMap.getClass());
        assertEquals(HashMap.class, fromHash.valueMap.getClass());

        final Hashtable<String, Integer> table = new Hashtable<>();
        table.put("a", 1);
        final BiMap<String, Integer> fromTable = BiMap.copyOf(table);
        assertEquals(Hashtable.class, fromTable.keyMap.getClass());
        assertEquals(LinkedHashMap.class, fromTable.valueMap.getClass());

        final BiMap<String, Integer> fromLinked = BiMap.copyOf(ordered("b", "a"));
        assertEquals(LinkedHashMap.class, fromLinked.keyMap.getClass());
        assertEquals(LinkedHashMap.class, fromLinked.valueMap.getClass());
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(fromLinked.keySet()));
    }
}
