package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class MultimapComputeTest extends MultimapTestSupport {
    @Test
    public void testComputeIfAbsent_KeyPresent() {
        listMultimap.put("key1", 10);
        List<Integer> original = listMultimap.get("key1");

        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });

        assertEquals(original, result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(10), result.get(0));
    }

    @Test
    public void testComputeIfAbsent_KeyAbsent() {
        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testComputeIfAbsent_FunctionReturnsNull() {
        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> null);
        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testComputeIfAbsent_NullKey() {
        List<Integer> result = listMultimap.computeIfAbsent(null, k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testComputeIfAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Function<String, List<Integer>> mappingFunc = key -> new ArrayList<>(Arrays.asList(key.length()));

        List<Integer> valA = mm.computeIfAbsent("aaa", mappingFunc);
        assertEquals(Arrays.asList(3), valA);
        assertEquals(Arrays.asList(3), mm.get("aaa"));

        List<Integer> valAExisting = mm.computeIfAbsent("aaa", mappingFunc);
        assertEquals(Arrays.asList(3), valAExisting);
        assertSame(valA, valAExisting);

        mm.computeIfAbsent("b", k -> null);
        assertFalse(mm.containsKey("b"));
        mm.computeIfAbsent("c", k -> new ArrayList<>());
        assertFalse(mm.containsKey("c"));
        assertNull(mm.computeIfAbsent("c", k -> new ArrayList<>()));

        assertThrows(IllegalArgumentException.class, () -> mm.computeIfAbsent("d", null));
        assertThrows(IllegalArgumentException.class, () -> listMultimap.computeIfAbsent("key1", null));
    }

    @Test
    public void testComputeIfPresentWithAliasedCollection() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        List<Integer> result = multimap.computeIfPresent("key", (k, v) -> v.subList(1, v.size()));
        assertEquals(Arrays.asList(2, 3), result);
        assertEquals(Arrays.asList(2, 3), multimap.get("key"));
    }

    @Test
    public void testComputeIfPresent_KeyAbsent() {
        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testComputeIfPresent_KeyPresent() {
        listMultimap.put("key1", 10);

        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(99);
            return list;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(99));
    }

    @Test
    public void testComputeIfPresent_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> null);

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testComputeIfPresent_NullKey() {
        listMultimap.put(null, 10);
        List<Integer> result = listMultimap.computeIfPresent(null, (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(20);
            return list;
        });
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testComputeIfPresentRemovesKey() {
        multimap.put("key", 1);

        List<Integer> result = multimap.computeIfPresent("key", (k, v) -> null);
        assertNull(result);
        assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testComputeIfPresent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        BiFunction<String, List<Integer>, List<Integer>> remappingFunc = (key, oldValues) -> {
            List<Integer> newValues = new ArrayList<>(oldValues);
            newValues.add(key.length());
            return newValues;
        };

        assertNull(mm.computeIfPresent("absent", remappingFunc));

        mm.put("aaa", 1);
        List<Integer> valA = mm.computeIfPresent("aaa", remappingFunc);
        assertEquals(Arrays.asList(1, 3), valA);
        assertEquals(Arrays.asList(1, 3), mm.get("aaa"));

        mm.put("b", 10);
        assertNull(mm.computeIfPresent("b", (k, v) -> null));
        assertFalse(mm.containsKey("b"));

        mm.put("c", 20);
        assertNull(mm.computeIfPresent("c", (k, v) -> new ArrayList<>()));
        assertFalse(mm.containsKey("c"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> mm.computeIfPresent("d", null));
    }

    @Test
    public void testComputeIfPresentNullFunction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multimap.computeIfPresent("key", null));
    }

    @Test
    public void testComputeIfPresent_NullFunction() {
        listMultimap.put("key1", 1);
        assertThrows(IllegalArgumentException.class, () -> listMultimap.computeIfPresent("key1", null));
    }

    @Test
    public void testComputeWithAliasedCollection() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        List<Integer> result = multimap.compute("key", (k, v) -> v.subList(0, 2));
        assertEquals(Arrays.asList(1, 2), result);
        assertEquals(Arrays.asList(1, 2), multimap.get("key"));
    }

    @Test
    public void testCompute_KeyAbsent() {
        List<Integer> result = listMultimap.compute("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return list;
        });

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testCompute_KeyPresent() {
        listMultimap.put("key1", 10);

        List<Integer> result = listMultimap.compute("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(99);
            return list;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testCompute_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> result = listMultimap.compute("key1", (k, v) -> null);

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testCompute_NullKey() {
        List<Integer> result = listMultimap.compute(null, (k, v) -> {
            List<Integer> list = v == null ? new ArrayList<>() : new ArrayList<>(v);
            list.add(99);
            return list;
        });
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testComputeWithComplexScenario() {
        multimap.compute("key1", (k, v) -> Arrays.asList(1, 2));
        assertEquals(2, multimap.get("key1").size());

        multimap.compute("key1", (k, v) -> {
            List<Integer> newList = new ArrayList<>(v);
            newList.add(3);
            return newList;
        });
        assertEquals(3, multimap.get("key1").size());

        multimap.compute("key1", (k, v) -> null);
        assertFalse(multimap.containsKey("key1"));
    }

    @Test
    public void testCompute_SameReferenceReturned() {
        listMultimap.putValues("key1", Arrays.asList(1, 2, 3));
        List<Integer> original = listMultimap.get("key1");
        List<Integer> result = listMultimap.compute("key1", (k, v) -> v);
        assertSame(original, result);
        assertEquals(3, listMultimap.get("key1").size());
    }

    @Test
    public void testCompute_EmptyResultRemovesKey() {
        listMultimap.putValues("key1", Arrays.asList(1, 2, 3));
        List<Integer> result = listMultimap.compute("key1", (k, v) -> new ArrayList<>());
        assertNull(result);
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testCompute() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        BiFunction<String, List<Integer>, List<Integer>> remappingFunc = (key, oldValues) -> {
            if (oldValues == null) {
                return new ArrayList<>(Arrays.asList(key.length()));
            }
            List<Integer> newValues = new ArrayList<>(oldValues);
            newValues.add(key.length() * 2);
            return newValues;
        };

        List<Integer> valAbsent = mm.compute("new", remappingFunc);
        assertEquals(Arrays.asList(3), valAbsent);
        assertEquals(Arrays.asList(3), mm.get("new"));

        List<Integer> valPresent = mm.compute("new", remappingFunc);
        assertEquals(Arrays.asList(3, 6), valPresent);
        assertEquals(Arrays.asList(3, 6), mm.get("new"));
        assertSame(mm.get("new"), valPresent);

        assertEquals(Arrays.asList(1), mm.compute("presentToNull", (k, v) -> v == null ? Arrays.asList(1) : null));
        assertEquals(Arrays.asList(1), mm.get("presentToNull"));
        assertNull(mm.compute("presentToNull", (k, v) -> null));
        assertFalse(mm.containsKey("presentToNull"));

        assertThrows(IllegalArgumentException.class, () -> mm.compute("d", null));
    }

    @Test
    public void testComputeNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.compute("key", null));
    }

    @Test
    public void testCompute_NullFunction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.compute("key1", null));
    }

    @Test
    public void testComputeAndMergeEmptiedSameInstanceRemovesMapping() {
        // regression: the same-instance fast path shadowed the documented "empty result removes the
        // mapping" rule, leaving an empty value collection in the backing map
        final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.put("k", 1);
        mm.computeIfPresent("k", (k, v) -> {
            v.clear();
            return v;
        });
        assertFalse(mm.containsKey("k"));
        assertNull(mm.get("k"));

        final ListMultimap<String, Integer> mm2 = CommonUtil.newListMultimap();
        mm2.put("k", 1);
        mm2.compute("k", (k, v) -> {
            v.clear();
            return v;
        });
        assertFalse(mm2.containsKey("k"));

        final ListMultimap<String, Integer> mm3 = CommonUtil.newListMultimap();
        mm3.put("k", 1);
        mm3.merge("k", 9, (v, e) -> {
            v.clear();
            return v;
        });
        assertFalse(mm3.containsKey("k"));
    }

    @Test
    public void testComputeKeepsTheHeldCollectionInstanceForAPresentKey() {
        // stated in compute's method description: for a key that was already present the collection the
        // function returns is not stored - its contents are copied into the collection already held
        listMultimap.putValues("key1", Arrays.asList(1, 2));
        final List<Integer> held = listMultimap.get("key1");
        final List<Integer> produced = new ArrayList<>(Arrays.asList(7, 8));
        final List<Integer> returned = listMultimap.compute("key1", (k, v) -> produced);

        assertSame(held, returned);
        assertSame(held, listMultimap.get("key1"));
        assertEquals(Arrays.asList(7, 8), returned);
        produced.add(9);
        assertEquals(Arrays.asList(7, 8), listMultimap.get("key1"));

        // only a previously absent key gets a new collection, and it is not the function's own either
        final List<Integer> producedForAbsent = new ArrayList<>(Arrays.asList(5));
        final List<Integer> created = listMultimap.compute("fresh", (k, v) -> producedForAbsent);
        assertNotSame(producedForAbsent, created);
        assertSame(created, listMultimap.get("fresh"));
        assertEquals(Arrays.asList(5), created);
    }
}
