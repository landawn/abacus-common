package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;

import com.landawn.abacus.TestBase;

public abstract class uTestSupport extends TestBase {

    @Nested
    public class OptionalTest extends TestBase {
    }

    @Nested
    public class OptionalIntTest extends TestBase {
    }

    @Nested
    public class NullableTest extends TestBase {
    }

    // --- Additional coverage tests for u.Nullable ---

    // ===================== Additional Missing Tests =====================

    // --- OptionalBoolean: getAsBoolean() ---

    // --- OptionalChar: additional missing ---

    // --- OptionalInt: additional missing ---

    // --- OptionalFloat: additional missing ---

    // --- OptionalLong: additional missing ---

    // --- OptionalDouble: additional missing ---

    // --- Optional<T>: additional missing ---

    // --- Nullable<T>: additional missing ---

    // --- Code-review-driven coverage tests (semantic guards) ---

    protected static <T extends Comparable<T>> void assertOptionalOrdering(final T empty, final T lower, final T higher) {
        assertEquals(0, empty.compareTo(empty));
        assertEquals(0, lower.compareTo(lower));
        assertTrue(empty.compareTo(lower) < 0);
        assertTrue(lower.compareTo(empty) > 0);
        assertTrue(lower.compareTo(higher) < 0);
        assertTrue(higher.compareTo(lower) > 0);
    }

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
