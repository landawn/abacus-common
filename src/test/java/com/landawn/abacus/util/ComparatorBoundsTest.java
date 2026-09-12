package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("unit")
public class ComparatorBoundsTest {
    @TempDir
    Path directory;

    @Test
    void allNaturalFactoriesRejectForeignComparableButAcceptBaseComparable() throws Exception {
        String[] assignments = { "Comparator<X> c = Comparators.naturalOrder();", "Comparator<X> c = Comparators.nullsFirst();",
                "Comparator<X> c = Comparators.nullsLast();", "Comparator<X> c = Comparators.reverseOrder();",
                "Comparator<X> c = Comparators.nullsFirstBy(x -> x);", "Comparator<X> c = Comparators.nullsLastBy(x -> x);",
                "Comparator<X> c = Comparators.comparingBy(x -> x);", "Comparator<X> c = Comparators.comparingByIfNotNullOrElseNullsFirst(x -> x);",
                "Comparator<X> c = Comparators.comparingByIfNotNullOrElseNullsLast(x -> x);", "Comparator<X> c = Comparators.reversedComparingBy(x -> x);",
                "Comparator<X> c = Comparators.reversedComparingByIfNotNullOrElseNullsFirst(x -> x);",
                "Comparator<X> c = Comparators.reversedComparingByIfNotNullOrElseNullsLast(x -> x);", "Comparator<X[]> c = Comparators.comparingArray();",
                "Comparator<List<X>> c = Comparators.comparingCollection();", "Comparator<Iterable<X>> c = Comparators.comparingIterable();",
                "Comparator<Iterator<X>> c = Comparators.comparingIterator();", "Comparator<Map<X, String>> c = Comparators.comparingMapByKey();",
                "Comparator<Map<String, X>> c = Comparators.comparingMapByValue();",
                "java.util.function.BiFunction<X, X, MergeResult> c = MergeResult.minFirst();",
                "java.util.function.BiFunction<X, X, MergeResult> c = MergeResult.maxFirst();" };
        for (int i = 0; i < assignments.length; i++) {
            UtilCycle2CompilationSupport.compile(directory, "Foreign" + i,
                    "class X implements Comparable<String> { public int compareTo(String s) { return 0; } }\n" + assignments[i], false);
            UtilCycle2CompilationSupport.compile(directory, "Valid" + i,
                    "class Base implements Comparable<Base> { public int compareTo(Base b) { return 0; } } class X extends Base {}\n" + assignments[i], true);
        }
    }

    @Test
    void naturalOrderAndNullPoliciesRemain() {
        Comparator<Integer> ascending = Comparators.naturalOrder();
        assertTrue(ascending.compare(null, 1) < 0);
        assertTrue(ascending.compare(1, 2) < 0);
        assertTrue(Comparators.<Integer> reverseOrder().compare(1, 2) > 0);
        assertTrue(Comparators.<Integer> nullsLast().compare(null, 1) > 0);
        assertEquals(MergeResult.TAKE_FIRST, MergeResult.<Integer> minFirst().apply(2, 2));
        assertEquals(MergeResult.TAKE_FIRST, MergeResult.<Integer> maxFirst().apply(2, 2));
    }
}
