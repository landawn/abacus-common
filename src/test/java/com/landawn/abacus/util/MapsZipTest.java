package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

public class MapsZipTest extends MapsTestSupport {

    // zip

    @Test
    public void testMapSupplierBehavior() {
        final boolean[] supplierCalled = { false };
        IntFunction<HashMap<String, String>> trackingSupplier = size -> {
            supplierCalled[0] = true;
            return new HashMap<>(size);
        };

        List<String> keys = Arrays.asList("a", "b");
        List<String> values = Arrays.asList("1", "2");

        Maps.zip(keys, values, trackingSupplier);
        assertTrue(supplierCalled[0]);
    }

    @Test
    public void testZip() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(3), result.get("c"));

        List<String> longerKeys = Arrays.asList("a", "b", "c", "d");
        Map<String, Integer> result2 = Maps.zip(longerKeys, values);
        assertEquals(3, result2.size());

        assertTrue(Maps.zip(null, values).isEmpty());
        assertTrue(Maps.zip(keys, null).isEmpty());
        assertTrue(Maps.zip(new ArrayList<>(), values).isEmpty());
    }

    @Test
    public void testZip_BasicCase() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
    }

    @Test
    public void testZip_emptyKeys_withMapSupplier() {
        List<String> emptyKeys = new ArrayList<>();
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = Maps.zip(emptyKeys, values, HashMap::new);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZip_iterables() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        assertEquals(expected, Maps.zip(keys, values));

        assertTrue(Maps.zip(null, values).isEmpty());
        assertTrue(Maps.zip(keys, null).isEmpty());
        assertTrue(Maps.zip(Arrays.asList(), values).isEmpty());

        List<String> shortKeys = Arrays.asList("a");
        assertEquals(Map.of("a", 1), Maps.zip(shortKeys, values));
    }

    @Test
    public void testZip_iterables_withMergeAndSupplier() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);
        BiFunction<Integer, Integer, Integer> merger = Integer::sum;
        IntFunction<HashMap<String, Integer>> supplier = HashMap::new;

        Map<String, Integer> result = Maps.zip(keys, values, merger, supplier);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1 + 3);
        expected.put("b", 2);
        assertEquals(expected, result);
    }

    @Test
    public void testZip_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("k1", "k2", "k3").iterator();
        Iterable<Integer> values = () -> Arrays.asList(1, 2).iterator();

        Map<String, Integer> result = Maps.zip(keys, values);

        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("k1"));
        assertEquals(Integer.valueOf(2), result.get("k2"));
        assertFalse(result.containsKey("k3"));
    }

    @Test
    public void testZip_UnequalSizes() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    public void testZip_validatesFunctionsAndSuppliedMapForEmptyInputs() {
        final List<String> emptyKeys = Collections.emptyList();
        final List<Integer> values = Arrays.asList(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> Maps.zip(emptyKeys, values, (IntFunction<Map<String, Integer>>) null));
        assertThrows(NullPointerException.class, () -> Maps.zip(emptyKeys, values, ignored -> (Map<String, Integer>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Maps.zip(emptyKeys, values, (BiFunction<Integer, Integer, Integer>) null, HashMap::new));
        assertThrows(NullPointerException.class, () -> Maps.zip(emptyKeys, values, Integer::sum, ignored -> (Map<String, Integer>) null));
    }

    @Test
    public void testZip_WithMergeAndSupplier() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = Maps.zip(keys, values, (v1, v2) -> v1 + v2, HashMap::new);
        assertEquals(Integer.valueOf(4), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void testZipDuplicateKeysWithUnequalLengths() {
        final List<String> keys = Arrays.asList("a", "a", "unpaired");
        final List<Integer> values = Arrays.asList(1, 2);

        assertEquals(Map.of("a", 2), Maps.zip(keys, values));
        assertEquals(Map.of("a", 2), Maps.zip(keys, values, LinkedHashMap::new));
        assertEquals(Map.of("a", 3), Maps.zip(keys, values, Integer::sum, HashMap::new));
        assertTrue(Maps.zip(keys, values, (left, right) -> null, HashMap::new).isEmpty());
    }

    @Test
    public void testZipWithEmptyCollections() {
        List<String> emptyKeys = new ArrayList<>();
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result1 = Maps.zip(emptyKeys, values);
        assertTrue(result1.isEmpty());

        Map<String, Integer> result2 = Maps.zip(emptyKeys, values, (v1, v2) -> v1 + v2, HashMap::new);
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testZipWithMapSupplier() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, LinkedHashMap::new);
        assertEquals(3, result.size());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testZipWithMapSupplier_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("a", "b").iterator();
        Iterable<Integer> values = () -> Arrays.asList(10, 20, 30).iterator();

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, ignored -> new LinkedHashMap<>());

        assertTrue(result instanceof LinkedHashMap);
        assertEquals(Map.of("a", 10, "b", 20), result);
    }

    @Test
    public void testZipWithMergeFunction() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values, Integer::sum, HashMap::new);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(4), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void testZipWithMergeFunction_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("dup", "dup", "tail").iterator();
        Iterable<Integer> values = () -> Arrays.asList(1, 2, 3).iterator();

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, Integer::sum, ignored -> new LinkedHashMap<>());

        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("dup"));
        assertEquals(Integer.valueOf(3), result.get("tail"));
    }
}
