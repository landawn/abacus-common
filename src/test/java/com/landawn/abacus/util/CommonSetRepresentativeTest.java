package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CommonSetRepresentativeTest extends TestBase {
    @Test
    void unorderedFirstInputSuppliesActualRepresentativesEvenWhenSecondIsSmaller() {
        SimpleImmutableEntry<String, Integer> first = new SimpleImmutableEntry<>("\u4e2d\ud83d\ude00", 1);
        SimpleEntry<String, Integer> second = new SimpleEntry<>(first);
        Set<SimpleImmutableEntry<String, Integer>> source = new HashSet<>(List.of(first, new SimpleImmutableEntry<>("other", 2)));
        Set<SimpleImmutableEntry<String, Integer>> result = N.commonSet(source, List.of(second));
        assertEquals(1, result.size());
        assertSame(first, result.iterator().next());
        assertEquals(HashSet.class, result.getClass());
        Set<Map.Entry<String, Integer>> all = N.commonSet(List.of(source, List.of(second), List.of(second)));
        assertSame(first, all.iterator().next());
    }

    @Test
    void orderedInputsKeepTheirFirstEqualInstanceAndNullPosition() {
        String first = new String("equal");
        String duplicate = new String("equal");
        List<String> source = Arrays.asList("z", null, first, duplicate, "a");
        List<String> other = Arrays.asList("a", "equal", null);
        Set<String> result = N.commonSet(source, other);
        assertEquals(Arrays.asList(null, "equal", "a"), new ArrayList<>(result));
        assertSame(first, new ArrayList<>(result).get(1));
        assertEquals(LinkedHashSet.class, result.getClass());
        assertEquals(result, N.commonSet(new LinkedHashSet<>(source), other));
        assertSame(first, new ArrayList<>(N.commonSet(List.of(source, other, other))).get(1));
    }

    @Test
    void emptySingletonAndRepeatedCollectionReferencesRetainMembershipRules() {
        assertTrue(N.commonSet(null, List.of(1)).isEmpty());
        assertTrue(N.commonSet(List.of(1), null).isEmpty());
        assertTrue(N.commonSet(List.of(1), List.of()).isEmpty());
        assertTrue(N.commonSet((Collection<Collection<Object>>) null).isEmpty());
        assertTrue(N.commonSet(List.<Collection<Object>> of()).isEmpty());
        assertTrue(N.commonSet(Arrays.<Collection<Integer>> asList(List.of(1), null)).isEmpty());
        assertTrue(N.commonSet(Arrays.<Collection<Integer>> asList((Collection<Integer>) null)).isEmpty());
        List<Integer> first = Arrays.asList(2, 1, 2, null);
        assertEquals(Arrays.asList(2, 1, null), new ArrayList<>(N.commonSet(List.of(first))));
        assertEquals(Arrays.asList(2, 1, null), new ArrayList<>(N.commonSet(List.of(first, first, first))));
        List<Integer> smallest = List.of(2);
        assertEquals(Set.of(2), N.commonSet(List.of(first, smallest, smallest)));
        assertEquals(Set.of(2), N.commonSet(List.of(smallest, first, smallest)));
    }
}
