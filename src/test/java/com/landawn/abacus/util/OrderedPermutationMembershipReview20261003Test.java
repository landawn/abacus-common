package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class OrderedPermutationMembershipReview20261003Test extends TestBase {
    @ParameterizedTest
    @CsvSource({ "true,true", "true,false", "false,true", "false,false" })
    void equalRepresentativesMayHaveDifferentOrderingKeys(final boolean natural, final boolean reverseCandidate) {
        final List<Item> input = List.of(new Item(1, 1), new Item(2, 2));
        final Collection<List<Item>> permutations = natural ? Iterables.orderedPermutations(input)
                : Iterables.orderedPermutations(input, Comparator.comparingInt(item -> item.rank));
        final List<Item> candidate = reverseCandidate ? List.of(new Item(2, 0), new Item(1, 3)) : List.of(new Item(1, 3), new Item(2, 0));

        final List<List<Item>> emitted = new ArrayList<>(permutations);
        assertEquals(2, emitted.size());
        assertTrue(emitted.contains(candidate));
        assertTrue(permutations.contains(candidate));
        assertTrue(permutations.contains(new LinkedList<>(candidate)));
        assertEquals(List.of(new Item(1, 1), new Item(2, 2)), input);
    }

    @Test
    void finerOrderingRetainsEqualsMultiplicity() {
        final List<Item> input = List.of(new Item(1, 1), new Item(2, 2), new Item(1, 3));
        final Collection<List<Item>> permutations = Iterables.orderedPermutations(input);
        final List<List<Item>> emitted = new ArrayList<>(permutations);
        assertEquals(6, permutations.size());
        for (final List<Item> candidate : emitted) {
            assertTrue(permutations.contains(candidate));
        }
        assertFalse(permutations.contains(List.of(new Item(1, 1), new Item(2, 2), new Item(2, 3))));
    }

    @Test
    void equalForeignRepresentativesNeedNotBeAcceptedByTheComparator() {
        final Collection<List<Item>> permutations = Iterables.orderedPermutations(List.of(new Item(1, 1), new Item(2, 2)));
        final List<ForeignItem> candidate = List.of(new ForeignItem(2), new ForeignItem(1));
        assertTrue(new ArrayList<>(permutations).contains(candidate));
        assertTrue(permutations.contains(candidate));
    }

    @Test
    void ordinaryOrderingChecksDuplicateCountsAndForeignValues() {
        final Collection<List<Integer>> permutations = Iterables.orderedPermutations(List.of(1, 1, 2));
        assertEquals(3, permutations.size());
        assertTrue(permutations.contains(List.of(2, 1, 1)));
        assertFalse(permutations.contains(List.of(2, 2, 1)));
        assertFalse(permutations.contains(List.of(1, 2)));
        assertFalse(permutations.contains(List.of("a", "b", "c")));
        assertFalse(permutations.contains("not a list"));
        assertFalse(permutations.contains(null));
    }

    @Test
    void nullValuesUseOrdinaryEquality() {
        final Collection<List<Integer>> permutations = Iterables.orderedPermutations(Arrays.asList(null, 1, null),
                Comparator.nullsFirst(Comparator.naturalOrder()));
        assertEquals(3, permutations.size());
        assertTrue(permutations.contains(Arrays.asList(null, null, 1)));
        assertTrue(permutations.contains(Arrays.asList(1, null, null)));
        assertFalse(permutations.contains(Arrays.asList(1, 1, null)));
    }

    @Test
    void unequalComparatorTiesStillRequireAnEmittedArrangement() {
        final List<String> input = List.of("a", "A", "b", "B");
        final Collection<List<String>> permutations = Iterables.orderedPermutations(input, String.CASE_INSENSITIVE_ORDER);
        final List<List<String>> emitted = new ArrayList<>(permutations);
        assertEquals(6, permutations.size());
        for (final List<String> candidate : Iterables.permutations(input)) {
            assertEquals(emitted.contains(candidate), permutations.contains(candidate), candidate.toString());
        }
        assertFalse(permutations.contains(List.of("a", "a", "b", "B")));
    }

    @Test
    void emptyAndSingletonMembershipRemainEqualsBased() {
        final Collection<List<Integer>> empty = Iterables.orderedPermutations(List.<Integer> of());
        assertEquals(1, empty.size());
        assertTrue(empty.contains(List.of()));
        assertFalse(empty.contains(List.of(1)));
        final Collection<List<Item>> singleton = Iterables.orderedPermutations(List.of(new Item(1, 1)));
        assertTrue(singleton.contains(List.of(new Item(1, 99))));
        assertFalse(singleton.contains(List.of(new Item(2, 1))));
    }

    @Test
    void sourceAndReturnedTuplesAreIndependent() {
        final List<Integer> input = new ArrayList<>(List.of(2, 1));
        final Collection<List<Integer>> permutations = Iterables.orderedPermutations(input);
        input.clear();
        permutations.iterator().next().clear();
        assertEquals(List.of(List.of(1, 2), List.of(2, 1)), new ArrayList<>(permutations));
        assertTrue(permutations.contains(List.of(2, 1)));
    }

    private interface Identified {
        int id();
    }

    private record ForeignItem(int id) implements Identified {
        @Override
        public boolean equals(final Object other) {
            return other instanceof Identified value && id == value.id();
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    private static final class Item implements Comparable<Item>, Identified {
        private final int id;
        private final int rank;

        private Item(final int id, final int rank) {
            this.id = id;
            this.rank = rank;
        }

        @Override
        public int compareTo(final Item other) {
            return Integer.compare(rank, other.rank);
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Identified item && id == item.id();
        }

        @Override
        public int hashCode() {
            return id;
        }
    }
}
