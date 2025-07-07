package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Immutable100Test extends TestBase {

    @Test
    public void testImmutableInterface() {
        // Test that various immutable classes implement the Immutable interface
        Assertions.assertTrue(ImmutableList.empty() instanceof Immutable);
        Assertions.assertTrue(ImmutableSet.empty() instanceof Immutable);
        Assertions.assertTrue(ImmutableMap.empty() instanceof Immutable);
        Assertions.assertTrue(ImmutableCollection.wrap(N.emptyList()) instanceof Immutable);
        Assertions.assertTrue(ImmutableArray.of("a", "b") instanceof Immutable);
        
        // Test specific immutable implementations
        Assertions.assertTrue(ImmutableBiMap.empty() instanceof Immutable);
        Assertions.assertTrue(ImmutableSortedSet.empty() instanceof Immutable);
        Assertions.assertTrue(ImmutableSortedMap.empty() instanceof Immutable);
        Assertions.assertTrue(ImmutableNavigableSet.empty() instanceof Immutable);
        Assertions.assertTrue(ImmutableNavigableMap.empty() instanceof Immutable);
        Assertions.assertTrue(ImmutableEntry.of("key", "value") instanceof Immutable);
    }

    @Test
    public void testImmutableMarker() {
        // Create various immutable objects and verify they are marked as immutable
        ImmutableList<String> list = ImmutableList.of("a", "b");
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3);
        ImmutableMap<String, Integer> map = ImmutableMap.of("one", 1, "two", 2);
        
        // Use instanceof to check if they implement Immutable
        Assertions.assertTrue(list instanceof Immutable);
        Assertions.assertTrue(set instanceof Immutable);
        Assertions.assertTrue(map instanceof Immutable);
        
        // Test that we can use Immutable as a type
        Immutable immutableList = list;
        Immutable immutableSet = set;
        Immutable immutableMap = map;
        
        Assertions.assertNotNull(immutableList);
        Assertions.assertNotNull(immutableSet);
        Assertions.assertNotNull(immutableMap);
    }

    @Test
    public void testPolymorphism() {
        // Test that we can handle different immutable types polymorphically
        Immutable[] immutables = new Immutable[] {
            ImmutableList.of(1, 2, 3),
            ImmutableSet.of("a", "b"),
            ImmutableMap.of("key", "value"),
            ImmutableArray.of("x", "y", "z"),
            ImmutableEntry.of("k", "v")
        };
        
        for (Immutable immutable : immutables) {
            Assertions.assertNotNull(immutable);
            Assertions.assertTrue(immutable instanceof Immutable);
        }
    }
}