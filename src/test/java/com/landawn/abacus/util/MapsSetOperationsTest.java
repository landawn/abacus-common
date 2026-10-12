package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.u.Nullable;

public class MapsSetOperationsTest extends MapsTestSupport {

    private static Lru<String, Integer> lru10() {
        final Lru<String, Integer> lru = new Lru<>(10);

        for (int i = 0; i < 10; i++) {
            lru.put("k" + i, i);
        }

        return lru;
    }

    // difference

    @Test
    public void test_difference() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = CommonUtil.asMap("a", 1, "b", 3);

        assertEquals(CommonUtil.asMap("a", 1), Maps.intersection(map, map2));

        assertEquals(CommonUtil.asMap("b", Pair.of(2, Nullable.of(3)), "c", Pair.of(3, Nullable.empty())), Maps.difference(map, map2));

        assertEquals(CommonUtil.asMap("b", Pair.of(Nullable.of(2), Nullable.of(3)), "c", Pair.of(Nullable.of(3), Nullable.empty())),
                Maps.symmetricDifference(map, map2));

    }

    @Test
    public void testDifference() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "different");
        map2.put("d", "4");

        Map<String, Pair<String, Nullable<String>>> result = Maps.difference(map1, map2);
        assertEquals(2, result.size());

        Pair<String, Nullable<String>> pairA = result.get("a");
        assertEquals("1", pairA.left());
        assertFalse(pairA.right().isPresent());

        Pair<String, Nullable<String>> pairC = result.get("c");
        assertEquals("3", pairC.left());
        assertTrue(pairC.right().isPresent());
        assertEquals("different", pairC.right().get());

        assertNull(result.get("b"));

        Map<String, Pair<String, Nullable<String>>> result2 = Maps.difference(map1, null);
        assertEquals("1", result2.get("a").left());
        assertEquals("3", result2.get("c").left());

        assertTrue(Maps.difference(null, intMap).isEmpty());
        assertEquals(0, Maps.difference(null, null).size());
    }

    @Test
    public void testDifference_arrayValuesAreComparedByContent() {
        final Map<String, Object> map1 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });
        final Map<String, Object> map2 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 }); // equal content, distinct references

        assertTrue(Maps.difference(map1, map2).isEmpty());

        final Map<String, Object> map3 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 4 });
        final Map<String, Pair<Object, Nullable<Object>>> diff = Maps.difference(map1, map3);

        assertEquals(1, diff.size());
        assertTrue(diff.containsKey("data"));
    }

    @Test
    public void testDifference_nestedArrayValuesAreComparedByContent() {
        final Map<String, Object> map1 = CommonUtil.asMap("matrix", new int[][] { { 1, 2 }, { 3 } });
        final Map<String, Object> map2 = CommonUtil.asMap("matrix", new int[][] { { 1, 2 }, { 3 } });

        assertTrue(Maps.difference(map1, map2).isEmpty());

        final Map<String, Object> map3 = CommonUtil.asMap("matrix", new int[][] { { 1, 2 }, { 4 } });
        assertEquals(1, Maps.difference(map1, map3).size());
    }

    @Test
    public void testDifferenceSymmetricDifference_covariantSecondMap() {
        final Map<String, Number> first = new LinkedHashMap<>();
        first.put("a", 1);
        first.put("b", 2);

        // Integer <: Number — only compiles because the second param was widened to Map<? extends K, ? extends V>
        final Map<String, Integer> second = new LinkedHashMap<>();
        second.put("a", 1);
        second.put("c", 9);

        final Map<String, Pair<Number, Nullable<Number>>> diff = Maps.difference(first, second);
        assertFalse(diff.containsKey("a")); // identical value -> excluded
        assertTrue(diff.containsKey("b")); // only in first
        assertEquals(2, diff.get("b").left().intValue());
        assertFalse(diff.get("b").right().isPresent());

        final Map<String, Pair<Nullable<Number>, Nullable<Number>>> sym = Maps.symmetricDifference(first, second);
        assertFalse(sym.containsKey("a")); // identical
        assertTrue(sym.containsKey("b")); // only in first
        assertTrue(sym.containsKey("c")); // only in second
        assertEquals(9, sym.get("c").right().get().intValue());
        assertFalse(sym.get("c").left().isPresent());
    }

    // intersection

    @Test
    public void testC381_emptyIntersectionOfLruTemplateAcceptsLaterPuts() {
        final Map<String, Integer> result = Maps.intersection(lru10(), new HashMap<>());

        assertTrue(result.isEmpty());
        result.put("x", 1);
        assertEquals(Collections.singletonMap("x", 1), result);
    }

    @Test
    public void testIntersection() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "3");
        map2.put("d", "4");

        Map<String, String> result = Maps.intersection(map1, map2);
        assertEquals(2, result.size());
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
        assertNull(result.get("a"));
        assertNull(result.get("d"));

        assertTrue(Maps.intersection(null, map2).isEmpty());
        assertTrue(Maps.intersection(map1, null).isEmpty());
        assertTrue(Maps.intersection(map1, new HashMap<>()).isEmpty());
        assertTrue(Maps.intersection(new HashMap<>(), intMap).isEmpty());
        assertTrue(Maps.intersection(new HashMap<String, Integer>(), new HashMap<String, Integer>()).isEmpty());
    }

    @Test
    public void testIntersection_arrayValuesAreComparedByContent() {
        final Map<String, Object> map1 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });
        final Map<String, Object> map2 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });

        // intersection must agree with difference: an equal-content entry belongs to exactly one of them
        assertEquals(2, Maps.intersection(map1, map2).size());
        assertTrue(Maps.difference(map1, map2).isEmpty());

        final Map<String, Object> map3 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 4 });
        assertEquals(1, Maps.intersection(map1, map3).size());
        assertEquals(1, Maps.difference(map1, map3).size());
    }

    @Test
    public void testIntersection_NoCommon() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        Map<String, Integer> intersection = Maps.intersection(map1, map2);
        assertTrue(intersection.isEmpty());
    }

    @Test
    public void testIntersection_stillWorksWithCompatibleMaps() {
        final Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2);
        final Map<String, Integer> map2 = CommonUtil.asMap("a", 1, "b", 9);

        assertEquals(CommonUtil.asMap("a", 1), Maps.intersection(map1, map2));
        assertTrue(Maps.intersection(map1, null).isEmpty());
        assertTrue(Maps.intersection(null, map2).isEmpty());
    }

    @Test
    public void testC381_lruTemplateKeepsEveryResultEntry() {
        final Lru<String, Integer> lru = lru10();

        final Map<String, Integer> inter = Maps.intersection(lru, new HashMap<>(lru));
        assertEquals(10, inter.size());
        assertTrue(inter instanceof Lru);

        final Map<String, Integer> other = new HashMap<>();
        other.put("k0", 99);
        assertEquals(10, Maps.difference(lru, other).size());

        final Map<String, Integer> disjoint = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            disjoint.put("d" + i, i);
        }
        assertEquals(20, Maps.symmetricDifference(lru, disjoint).size());

        assertEquals(10, Maps.filter(lru, (k, v) -> true).size());
    }

    @Test
    public void test_MapDifference() {
        Account account1 = Beans.newRandomBean(Account.class);
        Account account2 = Beans.newRandomBean(Account.class);
        account2.setFirstName(account1.getFirstName());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> mapDiff = BeanDifference.of(account1, account2);
        assertEquals(account1.getFirstName(), mapDiff.common().get("firstName"));
        assertNotNull(mapDiff.onlyOnLeft());
        assertNotNull(mapDiff.onlyOnRight());
        assertNotNull(mapDiff.differentValues());

        mapDiff = BeanDifference.of(account1, account2, CommonUtil.toList("id", "firstName", "lastName2"));
        assertEquals(account1.getFirstName(), mapDiff.common().get("firstName"));
        assertFalse(mapDiff.common().containsKey("lastName2"));
    }

    // symmetricDifference

    @Test
    public void testC374_emptyFirstMapMirrorsSecondMapLikeNull() {
        final TreeMap<String, Integer> ci = caseInsensitive("A", 1, "b", 2);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> fromEmpty = Maps.symmetricDifference(new HashMap<>(), ci);
        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> fromNull = Maps.symmetricDifference(null, ci);

        assertTrue(fromEmpty instanceof TreeMap, fromEmpty.getClass().getName());
        assertEquals(fromNull.getClass(), fromEmpty.getClass());
        // map2's case-insensitive key semantics are kept, so a lookup in the other case finds the entry
        assertNotNull(fromEmpty.get("a"));
        assertTrue(fromEmpty.get("a").left().isEmpty());
        assertEquals(Nullable.of(1), fromEmpty.get("a").right());
        assertEquals(new ArrayList<>(fromNull.keySet()), new ArrayList<>(fromEmpty.keySet()));
        assertEquals(fromNull.get("B"), fromEmpty.get("B"));
    }

    @Test
    public void testC374_emptyFirstMapUnicodeKeys() {
        final TreeMap<String, Integer> ci = caseInsensitive("Ärger", 1, "été", 2);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(new LinkedHashMap<>(), ci);

        assertEquals(2, result.size());
        assertEquals(Nullable.of(1), result.get("ärger").right());
        assertEquals(Nullable.of(2), result.get("ÉTÉ").right());
    }

    @Test
    public void testC374_emptyOrNullBothSides() {
        // nothing supplies keys: the first map keeps its own type, a null pair still yields an empty map
        assertTrue(Maps.symmetricDifference(new TreeMap<String, Integer>(), null) instanceof TreeMap);
        assertTrue(Maps.symmetricDifference(new TreeMap<String, Integer>(), null).isEmpty());
        assertTrue(Maps.symmetricDifference((Map<String, Integer>) null, (Map<String, Integer>) null).isEmpty());
        assertTrue(Maps.symmetricDifference(new HashMap<String, Integer>(), caseInsensitive()) instanceof TreeMap);
    }

    @Test
    public void testMapOperationsWithIdentityHashMap() {
        IdentityHashMap<String, String> identityMap1 = new IdentityHashMap<>();
        String key1 = new String("key");
        String key2 = new String("key");
        identityMap1.put(key1, "value1");

        IdentityHashMap<String, String> identityMap2 = new IdentityHashMap<>();
        identityMap2.put(key2, "value2");

        Map<String, Pair<Nullable<String>, Nullable<String>>> diff = Maps.symmetricDifference(identityMap1, identityMap2);
        assertTrue(diff instanceof IdentityHashMap);
    }

    @Test
    public void testC374_nonEmptyFirstMapStillDictatesTemplate() {
        final Map<String, Integer> first = new LinkedHashMap<>();
        first.put("a", 1);

        final Map<String, Integer> second = new HashMap<>();
        second.put("a", 1);
        second.put("z", 9);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(first, second);

        assertTrue(result instanceof LinkedHashMap);
        assertFalse(result.containsKey("a"));
        assertEquals(Nullable.of(9), result.get("z").right());
        assertTrue(result.get("z").left().isEmpty());
    }

    @Test
    public void testSymmetricDifference() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "different");
        map2.put("d", "4");

        Map<String, Pair<Nullable<String>, Nullable<String>>> result = Maps.symmetricDifference(map1, map2);
        assertEquals(3, result.size());

        Pair<Nullable<String>, Nullable<String>> pairA = result.get("a");
        assertTrue(pairA.left().isPresent());
        assertEquals("1", pairA.left().get());
        assertFalse(pairA.right().isPresent());

        Pair<Nullable<String>, Nullable<String>> pairD = result.get("d");
        assertFalse(pairD.left().isPresent());
        assertTrue(pairD.right().isPresent());
        assertEquals("4", pairD.right().get());

        Pair<Nullable<String>, Nullable<String>> pairC = result.get("c");
        assertTrue(pairC.left().isPresent());
        assertEquals("3", pairC.left().get());
        assertTrue(pairC.right().isPresent());
        assertEquals("different", pairC.right().get());

        assertNull(result.get("b"));
    }

    @Test
    public void testSymmetricDifference_arrayValuesAreComparedByContent() {
        final Map<String, Object> map1 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });
        final Map<String, Object> map2 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });

        assertTrue(Maps.symmetricDifference(map1, map2).isEmpty());

        final Map<String, Object> map3 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 4 });
        final Map<String, Pair<Nullable<Object>, Nullable<Object>>> diff = Maps.symmetricDifference(map1, map3);

        assertEquals(1, diff.size());
        assertTrue(diff.containsKey("data"));
    }

    @Test
    public void testSymmetricDifference_firstMapStillDictatesKeySemantics() {
        final IdentityHashMap<String, Integer> identityMap = new IdentityHashMap<>();
        identityMap.put("a", 1);
        final Map<String, Integer> hashMap = CommonUtil.asMap("b", 2);

        assertTrue(Maps.symmetricDifference(identityMap, hashMap) instanceof IdentityHashMap);
        // and it agrees with its siblings, which derive the same thing from the first map
        assertTrue(Maps.difference(identityMap, hashMap) instanceof IdentityHashMap);
        assertTrue(Maps.intersection(identityMap, hashMap) instanceof IdentityHashMap);
    }

    @Test
    public void testSymmetricDifference_Map2Empty_AllEntriesInResult() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("x", 10);
        map1.put("y", 20);
        Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(map1, new HashMap<>());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("x"));
        assertTrue(result.get("x").left().isPresent());
        assertFalse(result.get("x").right().isPresent());
    }

    @Test
    public void testSymmetricDifference_nullFirstMapMirrorsSecond() {
        final TreeMap<String, Integer> sorted = new TreeMap<>();
        sorted.put("z", 1);
        sorted.put("a", 2);

        // every key comes from map2 here, so map2 is the right template
        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(null, sorted);
        assertTrue(result instanceof TreeMap);
        assertEquals(CommonUtil.asList("a", "z"), new ArrayList<>(result.keySet()));

        assertNotNull(Maps.symmetricDifference((Map<String, Integer>) null, (Map<String, Integer>) null));
        assertTrue(Maps.symmetricDifference((Map<String, Integer>) null, (Map<String, Integer>) null).isEmpty());
    }

    @Test
    public void testSymmetricDifference_secondMapDoesNotDictateKeySemantics() {
        final Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put(new String("a"), 1);

        final IdentityHashMap<String, Integer> identityMap = new IdentityHashMap<>();
        identityMap.put("b", 2);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(hashMap, identityMap);

        // previously an IdentityHashMap, so the entry keyed by hashMap's own "a" could not be looked up
        assertFalse(result instanceof IdentityHashMap);
        assertEquals(2, result.size());
        assertNotNull(result.get("a"));
        assertEquals(Nullable.of(1), result.get("a").left());
        assertTrue(result.get("a").right().isEmpty());
    }

    @Test
    public void testSymmetricDifferenceBiMapWithEqualValuesOnlyInSecondMap() {
        final BiMap<String, Integer> first = new BiMap<>();
        first.put("a", 1);

        final Map<String, Integer> second = new LinkedHashMap<>();
        second.put("x", 9);
        second.put("y", 9);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(first, second);

        assertEquals(3, result.size());
        assertEquals(Pair.of(Nullable.of(1), Nullable.empty()), result.get("a"));
        assertEquals(Pair.of(Nullable.empty(), Nullable.of(9)), result.get("x"));
        assertEquals(Pair.of(Nullable.empty(), Nullable.of(9)), result.get("y"));
    }
}
