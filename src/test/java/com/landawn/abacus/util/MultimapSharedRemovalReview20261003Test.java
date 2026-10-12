package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class MultimapSharedRemovalReview20261003Test extends TestBase {
    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void removesEverySelectedKeyWhoseSharedCollectionBecomesEmpty(boolean setValues, boolean entryPredicate) {
        final Multimap<String, Integer, ?> values = sharedValues(setValues);
        final Collection<?> retained = values.get("a");
        assertSame(retained, values.get("b"));

        assertTrue(removeAllSelected(values, entryPredicate, List.of(1, 2)));

        assertTrue(retained.isEmpty());
        assertNull(values.get("a"));
        assertNull(values.get("b"));
        assertEquals(0, values.keyCount());
        assertEquals(0, values.totalValueCount());
        assertTrue(values.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void partialRemovalRetainsSharedCollectionAndBothKeys(boolean setValues, boolean entryPredicate) {
        final Multimap<String, Integer, ?> values = sharedValues(setValues);
        final Collection<?> retained = values.get("a");

        assertTrue(removeAllSelected(values, entryPredicate, List.of(1)));

        assertSame(retained, values.get("a"));
        assertSame(retained, values.get("b"));
        assertEquals(List.of(2), new ArrayList<>(retained));
        assertEquals(2, values.keyCount());
        assertEquals(2, values.totalValueCount());
        assertFalse(removeAllSelected(values, entryPredicate, List.of(1)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void doesNotRemoveUnselectedAlias(boolean entryPredicate) {
        final Multimap<String, Integer, ?> values = sharedValues(false);
        final Collection<?> retained = values.get("b");

        assertTrue(entryPredicate ? values.removeValuesIf((key, collection) -> key.equals("a"), List.of(1, 2))
                : values.removeValuesIf(key -> key.equals("a"), List.of(1, 2)));

        assertNull(values.get("a"));
        assertSame(retained, values.get("b"));
        assertTrue(retained.isEmpty());
        assertEquals(Set.of("b"), values.keySet());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void preservesPreexistingEmptyKeysWithNoMatchingValues(boolean entryPredicate) {
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        final ListMultimap<String, Integer> values = ListMultimap.wrap(backing);
        final List<Integer> empty = Collections.emptyList();
        backing.put("empty", empty);
        values.put("present", 9);

        assertFalse(removeAllSelected(values, entryPredicate, List.of(1, 2)));

        assertSame(empty, values.get("empty"));
        assertEquals(List.of(9), values.get("present"));
        assertEquals(2, values.keyCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void unmatchedUnmodifiableCollectionsRemainUntouched(boolean entryPredicate) {
        final List<Integer> shared = List.of(1, 2);
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", shared);
        backing.put("b", shared);
        final ListMultimap<String, Integer> values = ListMultimap.wrap(backing);

        assertFalse(removeAllSelected(values, entryPredicate, List.of(99)));

        assertSame(shared, values.get("a"));
        assertSame(shared, values.get("b"));
        assertEquals(2, values.keyCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void removesDistinctIdentityKeysEvenWhenTheyAreEqual(boolean entryPredicate) {
        final List<Integer> shared = new ArrayList<>(List.of(1, 2));
        final Map<String, List<Integer>> backing = new IdentityHashMap<>();
        final String first = new String("same");
        final String second = new String("same");
        backing.put(first, shared);
        backing.put(second, shared);
        final ListMultimap<String, Integer> values = ListMultimap.wrap(backing);

        assertTrue(removeAllSelected(values, entryPredicate, List.of(1, 2)));

        assertTrue(backing.isEmpty());
        assertFalse(values.containsKey(first));
        assertFalse(values.containsKey(second));
        assertTrue(shared.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void alreadyEmptiedUnmodifiableViewNeedsOnlyKeyRemoval(boolean entryPredicate) {
        final List<Integer> shared = new ArrayList<>(List.of(1, 2));
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", shared);
        backing.put("b", Collections.unmodifiableList(shared));
        final ListMultimap<String, Integer> values = ListMultimap.wrap(backing);

        assertTrue(removeAllSelected(values, entryPredicate, List.of(1, 2)));

        assertTrue(backing.isEmpty());
        assertTrue(shared.isEmpty());
    }

    private static boolean removeAllSelected(Multimap<String, Integer, ?> values, boolean entryPredicate, Collection<Integer> removal) {
        return entryPredicate ? values.removeValuesIf((key, collection) -> true, removal) : values.removeValuesIf(key -> true, removal);
    }

    private static Multimap<String, Integer, ?> sharedValues(boolean setValues) {
        if (setValues) {
            final Set<Integer> shared = new LinkedHashSet<>(List.of(1, 2));
            final Map<String, Set<Integer>> backing = new LinkedHashMap<>();
            backing.put("a", shared);
            backing.put("b", shared);
            return SetMultimap.wrap(backing);
        }

        final List<Integer> shared = new ArrayList<>(List.of(1, 2));
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", shared);
        backing.put("b", shared);
        return ListMultimap.wrap(backing);
    }
}
