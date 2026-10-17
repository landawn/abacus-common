package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

/** Parallel mapping does not promise encounter order; multiplicity and consumption still matter. */
final class IteratorContractAssertions {
    private IteratorContractAssertions() {
    }

    static <T extends Comparable<? super T>> void assertValues(String name, List<T> expected, List<T> actual) {
        if (unordered(name)) {
            assertEquals(expected.stream().sorted().toList(), actual.stream().sorted().toList(), name);
        } else {
            assertEquals(expected, actual, name);
        }
    }

    static <T extends Comparable<? super T>> void assertRemaining(String name, List<T> expected, long skipped, List<T> actual) {
        int offset = (int) Math.min(skipped, expected.size());
        if (unordered(name) && offset > 0) {
            assertEquals(expected.size() - offset, actual.size(), name);
            List<T> unvisited = new ArrayList<>(expected);
            for (T value : actual) {
                assertTrue(unvisited.remove(value), () -> name + ": unexpected or repeated value " + value);
            }
        } else {
            assertValues(name, expected.subList(offset, expected.size()), actual);
        }
    }

    private static boolean unordered(String name) {
        return name.startsWith("parallel/") && name.contains("/3/") && !name.endsWith("reverseSorted");
    }
}
