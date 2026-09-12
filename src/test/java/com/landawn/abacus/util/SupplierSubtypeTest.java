package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import testfixtures.SupplierReviewFixtures.ConcreteRegisteredMap;
import testfixtures.SupplierReviewFixtures.ConcreteRegisteredSet;
import testfixtures.SupplierReviewFixtures.RegisteredConcurrentMap;
import testfixtures.SupplierReviewFixtures.RegisteredSet;

public class SupplierSubtypeTest extends TestBase {
    @Test
    void customInterfacesAndAbstractSortedTypesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Suppliers.ofCollection(CustomSortedSet.class));
        assertThrows(IllegalArgumentException.class, () -> Suppliers.ofCollection(AbstractSortedSet.class));
        assertThrows(IllegalArgumentException.class, () -> Suppliers.ofMap(CustomSortedMap.class));
        assertThrows(IllegalArgumentException.class, () -> Suppliers.ofMap(AbstractSortedMap.class));
        assertThrows(IllegalArgumentException.class, () -> Suppliers.ofMap(CustomConcurrentMap.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(CustomConcurrentMap.class));
    }

    @Test
    void failedLookupLeavesRoomForExplicitMatchingRegistration() {
        assertThrows(IllegalArgumentException.class, () -> Suppliers.ofCollection(RegisteredSet.class));
        assertTrue(Suppliers.registerForCollection(RegisteredSet.class, ConcreteRegisteredSet::new));
        Collection<String> set = Suppliers.<String> ofCollection(RegisteredSet.class).get();
        assertInstanceOf(RegisteredSet.class, set);
        set.add("\u4e2d");
        assertTrue(Suppliers.ofCollection(RegisteredSet.class).get().isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Suppliers.ofMap(RegisteredConcurrentMap.class));
        assertTrue(Suppliers.registerForMap(RegisteredConcurrentMap.class, ConcreteRegisteredMap::new));
        Map<String, Integer> map = Suppliers.<String, Integer> ofMap(RegisteredConcurrentMap.class).get();
        assertInstanceOf(RegisteredConcurrentMap.class, map);
        map.put("\ud83d\ude00", 1);
        assertTrue(Suppliers.ofMap(RegisteredConcurrentMap.class).get().isEmpty());
    }

    @Test
    void standardSortedAndConcurrentMappingsAndConcreteTypesRemainAvailable() {
        assertInstanceOf(TreeSet.class, Suppliers.ofCollection(SortedSet.class).get());
        assertInstanceOf(TreeSet.class, Suppliers.ofCollection(NavigableSet.class).get());
        assertInstanceOf(TreeMap.class, Suppliers.ofMap(SortedMap.class).get());
        assertInstanceOf(TreeMap.class, Suppliers.ofMap(NavigableMap.class).get());
        assertInstanceOf(ConcurrentSkipListMap.class, Suppliers.ofMap(ConcurrentNavigableMap.class).get());
        assertInstanceOf(ConcurrentSkipListMap.class, Suppliers.ofMap(ConcurrentSkipListMap.class).get());
        assertInstanceOf(ConcurrentSkipListSet.class, Suppliers.ofCollection(ConcurrentSkipListSet.class).get());
        assertEquals(ConcreteSortedSet.class, Suppliers.ofCollection(ConcreteSortedSet.class).get().getClass());
        assertEquals(ConcreteSortedMap.class, Suppliers.ofMap(ConcreteSortedMap.class).get().getClass());
    }

    @Test
    void immutableSortedFamiliesFallBackToSortedMutableTypes() {
        // ImmutableSortedSet/ImmutableNavigableSet are ImmutableSets and ImmutableSortedMap/
        // ImmutableNavigableMap are ImmutableMaps, so they must not fall through to HashSet/HashMap.
        assertInstanceOf(TreeSet.class, Suppliers.ofCollection(ImmutableSortedSet.class).get());
        assertInstanceOf(TreeSet.class, Suppliers.ofCollection(ImmutableNavigableSet.class).get());
        assertInstanceOf(TreeMap.class, Suppliers.ofMap(ImmutableSortedMap.class).get());
        assertInstanceOf(TreeMap.class, Suppliers.ofMap(ImmutableNavigableMap.class).get());

        // The unsorted immutable families keep their existing mappings.
        assertInstanceOf(ArrayList.class, Suppliers.ofCollection(ImmutableList.class).get());
        assertInstanceOf(HashSet.class, Suppliers.ofCollection(ImmutableSet.class).get());
        assertInstanceOf(HashMap.class, Suppliers.ofMap(ImmutableMap.class).get());
    }

    private interface CustomSortedSet extends SortedSet<String> {
    }

    private abstract static class AbstractSortedSet extends TreeSet<String> {
    }

    private interface CustomSortedMap extends SortedMap<String, Integer> {
    }

    private abstract static class AbstractSortedMap extends TreeMap<String, Integer> {
    }

    private interface CustomConcurrentMap extends ConcurrentNavigableMap<String, Integer> {
    }

    public static class ConcreteSortedSet extends TreeSet<String> {
    }

    public static class ConcreteSortedMap extends TreeMap<String, Integer> {
    }
}
