package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class MultimapCopyTest extends MultimapTestSupport {

    @Test
    public void testC355_baseCopy_nonEmptySupplierResult_IAE() {
        final AtomicInteger calls = new AtomicInteger();
        final Supplier<List<Integer>> supplier = () -> calls.incrementAndGet() == 1 ? new ArrayList<>() : new ArrayList<>(List.of(99));
        final Multimap<String, Integer, List<Integer>> mm = CommonUtil.newMultimap(LinkedHashMap::new, supplier);
        mm.put("a", 1);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, mm::copy);
        assertTrue(ex.getMessage().contains("non-empty"), ex.getMessage());
        assertEquals(List.of(1), mm.get("a"));
    }

    // copy

    @Test
    public void testCopy() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.putValues("b", Arrays.asList(2, 3));

        Multimap<String, Integer, List<Integer>> copy = mm.copy();
        assertNotSame(mm, copy);
        assertEquals(mm, copy);

        assertNotSame(mm.get("a"), copy.get("a"));
        assertEquals(mm.get("a"), copy.get("a"));

        copy.put("a", 100);
        assertTrue(mm.get("a").contains(1));
        assertFalse(mm.get("a").contains(100));
    }

    @Test
    public void testCopy_EmptyMultimap() {
        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();
        assertNotNull(copy);
        assertTrue(copy.isEmpty());
    }

    @Test
    public void testCopy_Independence() {
        listMultimap.put("key1", 10);
        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();

        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(1, copy.get("key1").size());
        assertNull(copy.get("key2"));
    }

    @Test
    public void testCopy_ListMultimapSpecific() {
        listMultimap.put("a", 1);
        ListMultimap<String, Integer> copy = listMultimap.copy();
        assertNotSame(listMultimap, copy);
        assertEquals(listMultimap, copy);
        assertTrue(copy instanceof ListMultimap);
        assertEquals(Arrays.asList(1), copy.get("a"));

        assertNotSame(listMultimap.get("a"), copy.get("a"));
        assertEquals(listMultimap.get("a"), copy.get("a"));

        copy.get("a").add(2);
        assertEquals(Arrays.asList(1), listMultimap.get("a"));
    }

    // G053-02: pre-sized identity index in copyEntriesInto - copy/toMap of many keys incl. an emptied key
    @Test
    public void testCopy_manyKeysWithEmptiedKey() {
        final ListMultimap<Integer, Integer> mm = CommonUtil.newListMultimap();

        for (int i = 0; i < 1000; i++) {
            mm.put(i, i);
            mm.put(i, -i);
        }

        mm.get(7).clear();

        final ListMultimap<Integer, Integer> copy = mm.copy();
        assertEquals(mm, copy);
        assertEquals(1000, copy.keyCount());
        assertTrue(copy.get(7).isEmpty());

        for (int i = 0; i < 1000; i++) {
            assertTrue(copy.get(i) != mm.get(i));
        }

        final Map<Integer, List<Integer>> map = mm.toMap();
        assertEquals(1000, map.size());
        assertEquals(Arrays.asList(3, -3), map.get(3));
        assertTrue(map.get(3) != mm.get(3));
        assertTrue(CommonUtil.<Integer, Integer> newListMultimap().copy().isEmpty());
    }

    @Test
    public void testCopy_SetMultimap() {
        setMultimap.put("a", 1);
        setMultimap.put("a", 2);
        setMultimap.put("b", 3);

        Multimap copy = setMultimap.copy();
        assertEquals(setMultimap.totalValueCount(), copy.totalValueCount());
        assertEquals(setMultimap.keyCount(), copy.keyCount());
        // Modifications to copy should not affect original
        copy.put("c", 99);
        assertFalse(setMultimap.containsKey("c"));
    }

    @Test
    public void testCopy_SetMultimapSpecific() {
        setMultimap.put("a", 1);
        setMultimap.put("a", 1);
        SetMultimap<String, Integer> copy = setMultimap.copy();
        assertNotSame(setMultimap, copy);
        assertEquals(setMultimap, copy);
        assertTrue(copy instanceof SetMultimap);
        assertEquals(Collections.singleton(1), copy.get("a"));

        assertNotSame(setMultimap.get("a"), copy.get("a"));
        assertEquals(setMultimap.get("a"), copy.get("a"));

        copy.get("a").add(2);
        assertEquals(Collections.singleton(1), setMultimap.get("a"));
    }

    @Test
    public void testC355_copy_validSuppliers_emptyKey_equalsSource() {
        final ListMultimap<String, Integer> mm = listOf("a", 1, "b", 2);
        mm.get("b").clear();

        final ListMultimap<String, Integer> copy = mm.copy();
        assertEquals(mm, copy);
        assertTrue(copy.containsKey("b"));
        assertNotSame(mm.get("a"), copy.get("a"));

        assertTrue(CommonUtil.<String, Integer> newListMultimap().copy().isEmpty());
    }

    @Test
    public void testCopy_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();
        assertEquals(2, copy.keySet().size());
        assertEquals(2, copy.get("key1").size());
        assertEquals(1, copy.get("key2").size());

        copy.put("key1", 99);
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(3, copy.get("key1").size());
    }

    @Test
    public void testC355_copy_supplierReturnsLiveSourceCollection_IAE_sourceUntouched() {
        final List<Integer> live = new ArrayList<>(List.of(1));
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", live);
        backing.put("b", new ArrayList<>(List.of(2)));
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing, () -> live);

        assertThrows(IllegalArgumentException.class, mm::copy);
        assertEquals(List.of(1), live);
        assertEquals(List.of(2), mm.get("b"));
    }

    @Test
    public void testC355_listCopy_sharedSupplierResult_IAE() {
        final List<Integer> shared = new ArrayList<>();
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", new ArrayList<>(List.of(1)));
        backing.put("b", new ArrayList<>(List.of(2)));
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing, () -> shared);

        assertThrows(IllegalArgumentException.class, mm::copy);
        assertThrows(IllegalArgumentException.class, mm::toMap);
        assertThrows(IllegalArgumentException.class, () -> mm.toMap(HashMap::new));
        assertEquals(List.of(1), mm.get("a"));
        assertEquals(List.of(2), mm.get("b"));
        assertEquals(List.of(1), shared, "only the first copy was filled");
    }
}
