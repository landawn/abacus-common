package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;
import com.landawn.abacus.util.function.BiPredicate;

public class DifferenceMapTest extends DifferenceTestSupport {

    @Test
    public void testOf() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = CommonUtil.asMap("b", 2, "c", 4, "d", 5);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);
        assertEquals(CommonUtil.asMap("b", 2), diff.common());
        assertEquals(CommonUtil.asMap("a", 1), diff.onlyOnLeft());
        assertEquals(CommonUtil.asMap("d", 5), diff.onlyOnRight());
        assertEquals(Pair.of(3, 4), diff.differentValues().get("c"));
        assertFalse(diff.areEqual());
        assertTrue(diff.toString().contains("differentValues"));
        assertTrue(diff.toString().contains("areEqual"));

        Map<String, Integer> linked1 = new LinkedHashMap<>();
        linked1.put("a", 1);
        linked1.put("b", 2);
        Map<String, Integer> linked2 = new LinkedHashMap<>();
        linked2.put("b", 3);
        linked2.put("c", 4);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> linked = MapDifference.of(linked1, linked2);
        assertTrue(linked.common() instanceof LinkedHashMap);
        assertTrue(linked.onlyOnLeft() instanceof LinkedHashMap);
        assertTrue(linked.differentValues() instanceof LinkedHashMap);
        assertTrue(MapDifference.of(new TreeMap<>(linked1), new TreeMap<>(linked2)).common() instanceof LinkedHashMap);

        assertTrue(MapDifference.of(new HashMap<String, Integer>(), new HashMap<String, Integer>()).areEqual());
        assertTrue(MapDifference.of((Map<String, Integer>) null, (Map<String, Integer>) null).areEqual());

        Map<String, Integer> nulls1 = new HashMap<>();
        nulls1.put("a", 1);
        nulls1.put("b", null);
        Map<String, Integer> nulls2 = new HashMap<>();
        nulls2.put("a", 2);
        nulls2.put("b", null);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> nullValues = MapDifference.of(nulls1, nulls2);
        assertNull(nullValues.common().get("b"));
        assertEquals(Pair.of(1, 2), nullValues.differentValues().get("a"));

        Map<String, Integer> one = CommonUtil.asMap("a", 1, "b", 2);
        assertEquals(2, MapDifference.of(one, null).onlyOnLeft().size());
        assertEquals(2, MapDifference.of(null, one).onlyOnRight().size());
    }

    @Test
    public void testOf_KeysToCompare() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = CommonUtil.asMap("a", 1, "b", 5, "d", 4);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2,
                Arrays.asList("a", "b"));
        assertEquals(CommonUtil.asMap("a", 1), diff.common());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertEquals(Pair.of(2, 5), diff.differentValues().get("b"));

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> oneNull = MapDifference.of(map1, null,
                Arrays.asList("a"));
        assertTrue(oneNull.onlyOnLeft().containsKey("a"));
        assertFalse(oneNull.onlyOnLeft().containsKey("b"));
        assertTrue(MapDifference.of(null, map1, Arrays.asList("a")).onlyOnRight().containsKey("a"));

        String firstKey = new String("key");
        String secondKey = new String("key");
        Map<String, Integer> values = new IdentityHashMap<>();
        values.put(firstKey, 1);
        values.put(secondKey, 2);
        for (Map<String, Integer> empty : Arrays.asList(null, Collections.<String, Integer> emptyMap())) {
            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(values, empty, List.of("key")));
            IdentityHashSet<String> identitySelection = new IdentityHashSet<>();
            identitySelection.add(firstKey);
            assertEquals(Map.of("key", 1), MapDifference.of(values, empty, identitySelection).onlyOnLeft());
        }
    }

    @Test
    public void testOf_Equivalence() {
        Map<String, String> map1 = CommonUtil.asMap("a", "HELLO", "b", "WORLD");
        Map<String, String> map2 = CommonUtil.asMap("a", "hello", "b", "world");
        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> caseInsensitive = MapDifference.ofByEntries(map1, map2, null,
                (k, v1, v2) -> v1 != null && v2 != null && v1.equalsIgnoreCase(v2));
        assertEquals(2, caseInsensitive.common().size());
        assertTrue(caseInsensitive.areEqual());

        Map<String, Integer> nums1 = CommonUtil.asMap("threshold", 100, "value", 50);
        Map<String, Integer> nums2 = CommonUtil.asMap("threshold", 105, "value", 200);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> tri = MapDifference.ofByEntries(nums1, nums2,
                (k, v1, v2) -> {
                    if ("threshold".equals(k)) {
                        return Math.abs(v1 - v2) <= 10;
                    }
                    return v1.equals(v2);
                });
        assertTrue(tri.common().containsKey("threshold"));
        assertEquals(Pair.of(50, 200), tri.differentValues().get("value"));

        Map<String, Integer> keyed1 = CommonUtil.asMap("a", 100, "b", 200, "c", 300);
        Map<String, Integer> keyed2 = CommonUtil.asMap("a", 105, "b", 250, "c", 310, "d", 400);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> keyed = MapDifference.ofByEntries(keyed1, keyed2,
                Arrays.asList("a", "b"), (k, v1, v2) -> "a".equals(k) ? Math.abs(v1 - v2) <= 10 : v1.equals(v2));
        assertTrue(keyed.common().containsKey("a"));
        assertEquals(Pair.of(200, 250), keyed.differentValues().get("b"));
        assertTrue(keyed.onlyOnRight().isEmpty());

        Map<String, String> names1 = CommonUtil.asMap("name", "Alice", "city", "NYC");
        Map<String, String> names2 = CommonUtil.asMap("name", "alice", "city", "LA");
        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> nameDiff = MapDifference.ofByEntries(names1, names2,
                (k, v1, v2) -> "name".equals(k) ? v1.equalsIgnoreCase(v2) : v1.equals(v2));
        assertTrue(nameDiff.common().containsKey("name"));
        assertEquals(Pair.of("NYC", "LA"), nameDiff.differentValues().get("city"));

        assertThrows(IllegalArgumentException.class,
                () -> MapDifference.ofByValues(new HashMap<String, Integer>(), new HashMap<String, Integer>(), (BiPredicate<Integer, Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> MapDifference.ofByEntries(new HashMap<String, Integer>(), new HashMap<String, Integer>(),
                (com.landawn.abacus.util.function.TriPredicate<String, Integer, Integer>) null));
    }

    @Test
    public void testOfCollections() {
        List<Map<String, Object>> a = Arrays.asList(CommonUtil.asMap("id", 1, "n", "x"), CommonUtil.asMap("id", 2, "n", "y"));
        List<Map<String, Object>> b = Arrays.asList(CommonUtil.asMap("id", 2, "n", "z"), CommonUtil.asMap("id", 3, "n", "w"));
        MapDifference<List<Map<String, Object>>, List<Map<String, Object>>, Map<Object, MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> coll = MapDifference
                .of(a, b, m -> m.get("id"));
        assertEquals(1, coll.onlyOnLeft().size());
        assertEquals(1, coll.onlyOnRight().size());
        assertEquals(1, coll.differentValues().size());

        List<Map<String, Object>> empty = Collections.emptyList();
        assertTrue(MapDifference.of(empty, empty, m -> m.get("id")).areEqual());
        assertEquals(2, MapDifference.of(a, empty, m -> m.get("id")).onlyOnLeft().size());
        assertEquals(2, MapDifference.of(empty, a, m -> m.get("id")).onlyOnRight().size());

        List<Map<String, Object>> leftIds = Arrays.asList(CommonUtil.asMap("k", "a", "v", 1));
        List<Map<String, Object>> rightIds = Arrays.asList(CommonUtil.asMap("code", "a", "v", 2));
        MapDifference<?, ?, ?> differentExtractors = MapDifference.of(leftIds, rightIds, m -> m.get("k"), m -> m.get("code"));
        assertEquals(1, ((Map<?, ?>) differentExtractors.differentValues()).size());
    }

    @Test
    public void testEqualsHashCodeToString() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = CommonUtil.asMap("b", 3, "c", 4);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> d1 = MapDifference.of(map1, map2);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> d2 = MapDifference.of(map1, map2);
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertEquals("{areEqual=false, common={}, onlyOnLeft={a=1}, onlyOnRight={c=4}, differentValues={b=(2, 3)}}", d1.toString());
    }

    @Test
    public void testArrayValues() {
        assertTrue(MapDifference.of(mapWithArrayValues(1, new byte[] { 1, 2, 3 }), mapWithArrayValues(1, new byte[] { 1, 2, 3 })).areEqual());
        assertTrue(MapDifference.of(mapWithArrayValues(1, new byte[] { 1, 2, 3 }), mapWithArrayValues(1, new byte[] { 1, 2, 4 }))
                .differentValues()
                .containsKey("data"));
        assertTrue(MapDifference.of(mapWithArrayValues(1, new byte[] { 1, 2, 3 }), mapWithArrayValues(2, new byte[] { 1, 2, 3 }), Arrays.asList("data"))
                .areEqual());

        Map<String, Object> nested1 = new LinkedHashMap<>();
        nested1.put("matrix", new int[][] { { 1, 2 }, { 3 } });
        Map<String, Object> nested2 = new LinkedHashMap<>();
        nested2.put("matrix", new int[][] { { 1, 2 }, { 3 } });
        assertTrue(MapDifference.of(nested1, nested2).areEqual());
        nested2.put("matrix", new int[][] { { 1, 2 }, { 4 } });
        assertFalse(MapDifference.of(nested1, nested2).areEqual());

        Function<Map<String, Object>, Object> id = m -> m.get("id");
        assertTrue(MapDifference
                .of(Arrays.asList(mapWithArrayValues(1, new byte[] { 1, 2, 3 })), Arrays.asList(mapWithArrayValues(1, new byte[] { 1, 2, 3 })), id, id)
                .areEqual());
        assertFalse(MapDifference
                .of(Arrays.asList(mapWithArrayValues(1, new byte[] { 1, 2, 3 })), Arrays.asList(mapWithArrayValues(1, new byte[] { 1, 2, 4 })), id, id)
                .areEqual());
    }

    @Test
    public void testMapDifferenceIsNotEqualToBeanDifference() {
        MapDifference<?, ?, ?> mapDiff = MapDifference.of(CommonUtil.asMap("name", (Object) "John"), CommonUtil.asMap("name", (Object) "John"));
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> beanDiff = BeanDifference
                .of(new NullPropBean("John", null, null), new NullPropBean("John", null, null));
        assertEquals(mapDiff.common(), beanDiff.common());
        assertFalse(mapDiff.equals(beanDiff));
        assertFalse(beanDiff.equals(mapDiff));
    }

    /**
     * Contract pin for the third rejection shape documented on {@link MapDifference#of(Map, Map)}: two
     * {@code equals}-equal but distinct keys are rejected only when both would be filed into the <i>same</i>
     * result map. When they are filed into different result maps nothing is lost, so the call succeeds, both
     * entries are reported, and each result map reports the key object its own side stores - which is why the
     * same key text then shows up in two result maps.
     */
    @Test
    public void testOf_EqualButDistinctKeysRejectedOnlyWhenTheyShareOneResultMap() {
        final String kA = new String("dup"); // NOSONAR - distinct instances are the point of the test
        final String kB = new String("dup"); // NOSONAR
        assertEquals(kA, kB);
        assertNotSame(kA, kB);

        final Map<String, Integer> identity = new IdentityHashMap<>();
        identity.put(kA, 1);
        identity.put(kB, 2);
        assertEquals(2, identity.size());

        final Map<String, Integer> empty = new LinkedHashMap<>();
        final Map<String, Integer> unrelated = new LinkedHashMap<>();
        unrelated.put("zzz", 9);

        // both colliding entries head for one result map, so one would overwrite the other: rejected,
        // including when the other map is empty
        assertThrows(IllegalArgumentException.class, () -> MapDifference.of(empty, identity));
        assertThrows(IllegalArgumentException.class, () -> MapDifference.of(identity, empty));
        assertThrows(IllegalArgumentException.class, () -> MapDifference.of(unrelated, identity));

        // kA matches a key of map1 and kB does not, so the two entries are filed into different result maps
        final Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put(kA, 1);
        final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, identity);
        assertEquals(1, diff.common().size());
        assertEquals(1, diff.onlyOnRight().size());
        assertEquals(0, diff.onlyOnLeft().size());
        assertEquals(0, diff.differentValues().size());
        assertEquals(CommonUtil.asMap("dup", 1), diff.common());
        assertEquals(CommonUtil.asMap("dup", 2), diff.onlyOnRight());
        assertFalse(diff.areEqual());

        // each result map reports the key object its own side stores ...
        assertSame(kA, diff.common().keySet().iterator().next());
        assertSame(kB, diff.onlyOnRight().keySet().iterator().next());
        // ... so the same key text appears in two of the result maps
        assertEquals(diff.common().keySet(), diff.onlyOnRight().keySet());
    }
}
