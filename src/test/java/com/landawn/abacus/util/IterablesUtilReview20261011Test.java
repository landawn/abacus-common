package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
public class IterablesUtilReview20261011Test extends TestBase {
    @Test
    public void unionSizeSaturatesWhenDistinctElementsExceedIntegerRange() {
        final Set<Integer> range = nonNegativeRange(Integer.MAX_VALUE);
        final Set<Integer> union = Iterables.union(range, Set.of(-1));

        assertEquals(Integer.MAX_VALUE, union.size());
        assertTrue(union.contains(-1));
        assertTrue(union.contains(Integer.MAX_VALUE - 1));
        assertFalse(union.contains(Integer.MAX_VALUE));
    }

    @Test
    public void unionSizeSaturatesWhenCountingCrossesIntegerRange() {
        assertEquals(Integer.MAX_VALUE, Iterables.union(nonNegativeRange(Integer.MAX_VALUE - 1), Set.of(-1, -2)).size());
        assertEquals(Integer.MAX_VALUE, Iterables.union(nonNegativeRange(Integer.MAX_VALUE - 1), Set.of(-1, 0)).size());
    }

    @Test
    @Tag("slow-test")
    public void symmetricDifferenceSizeSaturatesWhenDistinctElementsExceedIntegerRange() {
        // size() visits every element, even for a lazy set. Run this boundary test explicitly with
        // -Dtest=IterablesUtilReview20261011Test#symmetricDifferenceSizeSaturatesWhenDistinctElementsExceedIntegerRange.
        // AbacusCoreTestSuite excludes slow-test methods from the regular unit suite.
        final Set<Integer> range = nonNegativeRange(Integer.MAX_VALUE);
        final Set<Integer> difference = Iterables.symmetricDifference(range, Set.of(-1));

        assertEquals(Integer.MAX_VALUE, difference.size());
        assertTrue(difference.contains(-1));
        assertTrue(difference.contains(Integer.MAX_VALUE - 1));
        assertFalse(difference.contains(Integer.MAX_VALUE));
    }

    @Test
    public void setViewSizesRemainLiveAndCountOnlyTheirMembers() {
        final Set<Integer> first = new LinkedHashSet<>(List.of(1, 2));
        final Set<Integer> second = new LinkedHashSet<>(List.of(2, 3));
        final Set<Integer> union = Iterables.union(first, second);
        final Set<Integer> difference = Iterables.symmetricDifference(first, second);

        assertEquals(3, union.size());
        assertEquals(2, difference.size());
        second.add(1);
        assertEquals(3, union.size());
        assertEquals(1, difference.size());
        first.clear();
        assertEquals(3, union.size());
        assertEquals(3, difference.size());
    }

    private static Set<Integer> nonNegativeRange(final int endExclusive) {
        // A real lazy set avoids storing billions of entries; union.size() also avoids iterating this range.
        return new AbstractSet<>() {
            @Override
            public Iterator<Integer> iterator() {
                return IntStream.range(0, endExclusive).boxed().iterator();
            }

            @Override
            public int size() {
                return endExclusive;
            }

            @Override
            public boolean contains(final Object value) {
                return value instanceof Integer number && number >= 0 && number < endExclusive;
            }
        };
    }
}
