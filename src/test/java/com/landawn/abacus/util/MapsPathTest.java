package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

public class MapsPathTest extends MapsTestSupport {

    // flatten

    @Test
    public void testFlatten() {
        Map<String, Object> map = new HashMap<>();
        map.put("simple", "value");

        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", "innerValue");
        map.put("nested", nested);

        Map<String, Object> deepNested = new HashMap<>();
        deepNested.put("deep", "deepValue");
        nested.put("level2", deepNested);

        Map<String, Object> flattened = Maps.flatten(map);
        assertEquals(3, flattened.size());
        assertEquals("value", flattened.get("simple"));
        assertEquals("innerValue", flattened.get("nested.inner"));
        assertEquals("deepValue", flattened.get("nested.level2.deep"));

        Map<String, Object> deep = CommonUtil.asMap("a", CommonUtil.asMap("b", CommonUtil.asMap("c", CommonUtil.asMap("d", 4), "c2", 3), "b2", 2), "a2", 1);
        assertEquals(deep, Maps.unflatten(Maps.flatten(deep)));
        assertTrue(Maps.flatten(null).isEmpty());
        assertTrue(Maps.flatten(new HashMap<>()).isEmpty());
    }

    @Test
    public void testFlatten_preservesEmptyNestedKeyPathSegment() {
        final Map<String, Object> nestedUnderEmptyKey = new LinkedHashMap<>();
        nestedUnderEmptyKey.put("", CommonUtil.asMap("a", 1));

        final Map<String, Object> flattened = Maps.flatten(nestedUnderEmptyKey);
        assertEquals(CommonUtil.asMap(".a", 1), flattened);
        assertEquals(nestedUnderEmptyKey, Maps.unflatten(flattened));
    }

    @Test
    public void testFlatten_rejectsCyclesAndFlattenedKeyCollisions() {
        final Map<String, Object> cyclic = new HashMap<>();
        cyclic.put("self", cyclic);
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(cyclic));

        final Map<String, Object> colliding = new LinkedHashMap<>();
        colliding.put("a.b", 1);
        colliding.put("a", CommonUtil.asMap("b", 2));
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(colliding));

        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(new HashMap<>(), "", HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(new HashMap<>(), ".", (IntFunction<Map<String, Object>>) null));
        assertThrows(NullPointerException.class, () -> Maps.flatten(new HashMap<>(), ".", ignored -> (Map<String, Object>) null));
    }

    @Test
    public void testFlatten_withSupplier() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", CommonUtil.asMap("b", 1));
        LinkedHashMap<String, Object> result = Maps.flatten(map, LinkedHashMap::new);
        assertEquals(1, result.get("a.b"));
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testFlattenAndUnflatten() {
        Map<String, Object> nestedMap = new HashMap<>();
        Map<String, Object> inner = new HashMap<>();
        inner.put("c", 3);
        nestedMap.put("a", 1);
        nestedMap.put("b", inner);

        Map<String, Object> flat = Maps.flatten(nestedMap);
        assertEquals(1, flat.get("a"));
        assertEquals(3, flat.get("b.c"));
        assertEquals(2, flat.size());

        Map<String, Object> unflattened = Maps.unflatten(flat);
        assertEquals(1, unflattened.get("a"));
        assertTrue(unflattened.get("b") instanceof Map);
        assertEquals(3, ((Map<?, ?>) unflattened.get("b")).get("c"));

        Map<String, Object> flatCustom = Maps.flatten(nestedMap, "_", HashMap::new);
        assertEquals(3, flatCustom.get("b_c"));
        Map<String, Object> unflattenedCustom = Maps.unflatten(flatCustom, "_", HashMap::new);
        assertEquals(3, ((Map<?, ?>) unflattenedCustom.get("b")).get("c"));
    }

    @Test
    public void testFlattenUnflatten_intFunctionSupplier() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "John");
        final Map<String, Object> address = new LinkedHashMap<>();
        address.put("city", "NYC");
        map.put("address", address);

        final int[] sizeHint = { -1 };
        final IntFunction<LinkedHashMap<String, Object>> supplier = size -> {
            sizeHint[0] = size;
            return new LinkedHashMap<>();
        };

        final LinkedHashMap<String, Object> flat = Maps.flatten(map, supplier);
        assertEquals("John", flat.get("name"));
        assertEquals("NYC", flat.get("address.city"));
        assertEquals(map.size(), sizeHint[0]); // top-level expected-size hint forwarded to the supplier

        @SuppressWarnings("unchecked")
        final Map<String, Object> round = (Map<String, Object>) Maps.unflatten(flat, LinkedHashMap::new).get("address");
        assertEquals("NYC", round.get("city"));
    }

    @Test
    public void testFlattenWithCustomDelimiter() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", "value");
        map.put("outer", nested);

        Map<String, Object> flattened = Maps.flatten(map, "_", HashMap::new);
        assertEquals("value", flattened.get("outer_inner"));
    }

    @Test
    public void testFlattenWithSupplier() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("key", "value");
        map.put("outer", nested);

        LinkedHashMap<String, Object> flattened = Maps.flatten(map, size -> new LinkedHashMap<>());
        assertTrue(flattened instanceof LinkedHashMap);
        assertEquals("value", flattened.get("outer.key"));
    }

    @Test
    public void testNestedMapConversions() {
        Map<String, Object> deepMap = new HashMap<>();
        Map<String, Object> level1 = new HashMap<>();
        Map<String, Object> level2 = new HashMap<>();
        Map<String, Object> level3 = new HashMap<>();

        level3.put("deep", "value");
        level2.put("level3", level3);
        level1.put("level2", level2);
        deepMap.put("level1", level1);

        Map<String, Object> flattened = Maps.flatten(deepMap);
        assertEquals("value", flattened.get("level1.level2.level3.deep"));

        Map<String, Object> unflattened = Maps.unflatten(flattened);
        assertEquals(deepMap, unflattened);
    }

    // transpose

    @Test
    public void testFlatToMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5, 6));
        map.put("c", Arrays.asList(7, 8));

        List<Map<String, Integer>> result = Maps.transpose(map);
        assertEquals(3, result.size());

        Map<String, Integer> first = result.get(0);
        assertEquals(Integer.valueOf(1), first.get("a"));
        assertEquals(Integer.valueOf(4), first.get("b"));
        assertEquals(Integer.valueOf(7), first.get("c"));

        Map<String, Integer> second = result.get(1);
        assertEquals(Integer.valueOf(2), second.get("a"));
        assertEquals(Integer.valueOf(5), second.get("b"));
        assertEquals(Integer.valueOf(8), second.get("c"));

        Map<String, Integer> third = result.get(2);
        assertEquals(Integer.valueOf(3), third.get("a"));
        assertEquals(Integer.valueOf(6), third.get("b"));
        assertFalse(third.containsKey("c"));
    }

    @Test
    public void testFlatToMap_Basic() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5));

        List<Map<String, Integer>> result = Maps.transpose(map);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).get("a"));
        assertEquals(Integer.valueOf(4), result.get(0).get("b"));
        assertEquals(Integer.valueOf(2), result.get(1).get("a"));
        assertEquals(Integer.valueOf(5), result.get(1).get("b"));
        assertEquals(Integer.valueOf(3), result.get(2).get("a"));
        assertNull(result.get(2).get("b"));
        assertEquals(0, Maps.transpose(null).size());
    }

    @Test
    public void testFlatToMap_emptyMap() {
        List<Map<String, String>> result = Maps.transpose(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatToMap_EntryWithEmptyCollection_Skipped() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", new ArrayList<>()); // empty collection - should be skipped
        map.put("c", Arrays.asList(3));
        List<Map<String, Integer>> result = Maps.transpose(map);
        assertEquals(2, result.size());
        // "b" not in results since it had empty collection
        assertFalse(result.get(0).containsKey("b"));
    }

    @Test
    public void testFlatToMap_nullMap() {
        List<Map<String, String>> result = Maps.transpose(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransposeBiMapWithSharedElementAtSamePosition() {
        final BiMap<String, List<Integer>> columns = new BiMap<>();
        columns.put("a", Arrays.asList(1, 2));
        columns.put("b", Arrays.asList(1, 3));

        final List<Map<String, Integer>> rows = Maps.transpose(columns);

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).get("a"));
        assertEquals(1, rows.get(0).get("b"));
        assertEquals(2, rows.get(1).get("a"));
        assertEquals(3, rows.get(1).get("b"));
    }

    // unflatten

    @Test
    public void testUnflatten() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("simple", "value");
        flat.put("nested.inner", "innerValue");
        flat.put("nested.level2.deep", "deepValue");

        Map<String, Object> unflattened = Maps.unflatten(flat);
        assertEquals(2, unflattened.size());
        assertEquals("value", unflattened.get("simple"));

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) unflattened.get("nested");
        assertNotNull(nested);
        assertEquals("innerValue", nested.get("inner"));

        @SuppressWarnings("unchecked")
        Map<String, Object> level2 = (Map<String, Object>) nested.get("level2");
        assertNotNull(level2);
        assertEquals("deepValue", level2.get("deep"));
        assertTrue(Maps.unflatten(null).isEmpty());
        assertTrue(Maps.unflatten(new HashMap<>()).isEmpty());
    }

    @Test
    public void testUnflatten_rejectsConflictingPathsInEitherOrder() {
        final Map<String, Object> scalarFirst = new LinkedHashMap<>();
        scalarFirst.put("a", null);
        scalarFirst.put("a.b", 2);
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(scalarFirst));

        final Map<String, Object> nestedFirst = new LinkedHashMap<>();
        nestedFirst.put("a.b", 2);
        nestedFirst.put("a", 1);
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(nestedFirst));

        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(new HashMap<>(), "", HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(new HashMap<>(), ".", (IntFunction<Map<String, Object>>) null));
        assertThrows(NullPointerException.class, () -> Maps.unflatten(new HashMap<>(), ".", ignored -> (Map<String, Object>) null));
    }

    @Test
    public void testUnflatten_rejectsSupplierMapIdentityReuse() {
        final Map<String, Object> oneLevel = new LinkedHashMap<>();
        oneLevel.put("a.b", 1);

        final Map<String, Object> reusedRoot = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(oneLevel, ignored -> reusedRoot));

        final Map<String, Object> deep = new LinkedHashMap<>();
        deep.put("a.b.c", 1);
        final Map<String, Object> deepRoot = new LinkedHashMap<>();
        final Map<String, Object> reusedNested = new LinkedHashMap<>();
        final int[] deepCalls = { 0 };
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(deep, ignored -> deepCalls[0]++ == 0 ? deepRoot : reusedNested));

        final Map<String, Object> siblings = new LinkedHashMap<>();
        siblings.put("a.x", 1);
        siblings.put("b.y", 2);
        final Map<String, Object> siblingRoot = new LinkedHashMap<>();
        final Map<String, Object> reusedSibling = new LinkedHashMap<>();
        final int[] siblingCalls = { 0 };
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(siblings, ignored -> siblingCalls[0]++ == 0 ? siblingRoot : reusedSibling));

        final Map<String, Object> inputRoot = new LinkedHashMap<>();
        inputRoot.put("a.b", 1);
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(inputRoot, ignored -> inputRoot));
        assertEquals(CommonUtil.asMap("a.b", 1), inputRoot);

        final Map<String, Object> separateRoot = new LinkedHashMap<>();
        final int[] inputCalls = { 0 };
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(inputRoot, ignored -> inputCalls[0]++ == 0 ? separateRoot : inputRoot));
        assertEquals(CommonUtil.asMap("a.b", 1), inputRoot);
    }

    @Test
    public void testUnflatten_withSupplier() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("a.b", 1);
        LinkedHashMap<String, Object> result = Maps.unflatten(flat, LinkedHashMap::new);
        assertNotNull(result.get("a"));
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testUnflattenPreservesNonconflictingMapLeafAliases() {
        final Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("value", 1);
        final Map<String, Object> flat = new LinkedHashMap<>();
        flat.put("first.leaf", leaf);
        flat.put("first.sibling", 2);
        flat.put("second", leaf);

        final Map<String, Object> result = Maps.unflatten(flat, ".", LinkedHashMap::new);
        final Map<?, ?> first = (Map<?, ?>) result.get("first");
        assertSame(leaf, first.get("leaf"));
        assertSame(leaf, result.get("second"));
        assertEquals(2, first.get("sibling"));
        assertEquals(Map.of("value", 1), leaf);
        assertEquals(3, flat.size());
    }

    @Test
    public void testUnflattenRejectsMapValuedLeafConflictsWithoutChangingInput() {
        for (final String delimiter : Arrays.asList(".", "/")) {
            for (final String prefix : Arrays.asList("a", "a" + delimiter + "b")) {
                for (final boolean leafFirst : Arrays.asList(true, false)) {
                    final Map<String, Object> leaf = new LinkedHashMap<>();
                    leaf.put("original", 1);
                    final Map<String, Object> flat = new LinkedHashMap<>();
                    if (leafFirst) {
                        flat.put(prefix, leaf);
                        flat.put(prefix + delimiter + "child", 2);
                    } else {
                        flat.put(prefix + delimiter + "child", 2);
                        flat.put(prefix, leaf);
                    }

                    assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(flat, delimiter, LinkedHashMap::new));
                    assertEquals(Map.of("original", 1), leaf);
                    assertEquals(2, flat.size());
                }
            }
        }

        final Map<String, Object> selfLeaf = new LinkedHashMap<>();
        selfLeaf.put("a", selfLeaf);
        selfLeaf.put("a.child", 2);
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(selfLeaf));
        assertEquals(2, selfLeaf.size());
        assertSame(selfLeaf, selfLeaf.get("a"));
    }

    @Test
    public void testUnflattenWithCustomDelimiter() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("outer_inner", "value");

        Map<String, Object> unflattened = Maps.unflatten(flat, "_", HashMap::new);
        @SuppressWarnings("unchecked")
        Map<String, Object> outer = (Map<String, Object>) unflattened.get("outer");
        assertNotNull(outer);
        assertEquals("value", outer.get("inner"));
    }

    @Test
    public void testUnflattenWithSupplier() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("a.b.c", "value");

        Map<String, Object> unflattened = Maps.unflatten(flat, size -> new LinkedHashMap<>());
        assertTrue(unflattened instanceof LinkedHashMap);

        @SuppressWarnings("unchecked")
        Map<String, Object> a = (Map<String, Object>) unflattened.get("a");
        assertNotNull(a);

        @SuppressWarnings("unchecked")
        Map<String, Object> b = (Map<String, Object>) a.get("b");
        assertNotNull(b);
        assertEquals("value", b.get("c"));
    }
}
