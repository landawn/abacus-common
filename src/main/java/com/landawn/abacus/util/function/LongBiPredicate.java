/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * Represents a predicate (boolean-valued function) of two {@code long}-valued
 * arguments. This is the two-arity primitive type specialization of
 * {@link java.util.function.Predicate}.
 *
 * <p>This interface extends {@link Throwables.LongBiPredicate} with
 * {@link RuntimeException}, providing compatibility with the abacus-common framework's
 * exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #test(long, long)}.
 *
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongBiPredicate extends Throwables.LongBiPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true} regardless of the input values.
     * This can be useful as a default predicate or in testing scenarios.
     */
    LongBiPredicate ALWAYS_TRUE = (t, u) -> true;
    /**
     * A predicate that always evaluates to {@code false} regardless of the input values.
     * This can be useful as a default predicate or in testing scenarios.
     */
    LongBiPredicate ALWAYS_FALSE = (t, u) -> false;
    /**
     * A predicate that tests if two long values are equal.
     * Returns {@code true} if {@code t == u}, {@code false} otherwise.
     */
    LongBiPredicate EQUAL = (t, u) -> t == u;
    /**
     * A predicate that tests if two long values are not equal.
     * Returns {@code true} if {@code t != u}, {@code false} otherwise.
     */
    LongBiPredicate NOT_EQUAL = (t, u) -> t != u;
    /**
     * A predicate that tests if the first long value is greater than the second.
     * Returns {@code true} if {@code t > u}, {@code false} otherwise.
     */
    LongBiPredicate GREATER_THAN = (t, u) -> t > u;
    /**
     * A predicate that tests if the first long value is greater than or equal to the second.
     * Returns {@code true} if {@code t >= u}, {@code false} otherwise.
     */
    LongBiPredicate GREATER_EQUAL = (t, u) -> t >= u;
    /**
     * A predicate that tests if the first long value is less than the second.
     * Returns {@code true} if {@code t < u}, {@code false} otherwise.
     */
    LongBiPredicate LESS_THAN = (t, u) -> t < u;
    /**
     * A predicate that tests if the first long value is less than or equal to the second.
     * Returns {@code true} if {@code t <= u}, {@code false} otherwise.
     */
    LongBiPredicate LESS_EQUAL = (t, u) -> t <= u;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p>The predicate tests two long values and returns a boolean result.
     * Common use cases include:
     * <ul>
     *   <li>Comparing two long values (equality, ordering)</li>
     *   <li>Range checking (e.g., value within bounds)</li>
     *   <li>Validating relationships between two long values</li>
     *   <li>Filtering pairs of long values in stream operations</li>
     *   <li>Implementing custom comparison logic for long pairs</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongBiPredicate sumGreaterThan100 = (a, b) -> a + b > 100;
     * boolean result = sumGreaterThan100.test(50L, 60L);   // Returns true
     * }</pre>
     *
     * @param t the first input argument
     * @param u the second input argument
     * @return {@code true} if the input arguments match the predicate, {@code false} otherwise
     */
    @Override
    boolean test(long t, long u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>If this predicate returns {@code true} for given inputs, the negated
     * predicate will return {@code false}, and vice versa.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default LongBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code false}, then the {@code other} predicate
     * is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongBiPredicate isPositive = (a, b) -> a > 0 && b > 0;
     * LongBiPredicate sumLessThan100 = (a, b) -> a + b < 100;
     * LongBiPredicate combined = isPositive.and(sumLessThan100);
     * boolean result = combined.test(10L, 20L);   // true (both positive AND sum < 100)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate.
     *              Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default LongBiPredicate and(final LongBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code true}, then the {@code other} predicate
     * is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongBiPredicate isZero = (a, b) -> a == 0 || b == 0;
     * LongBiPredicate isNegative = (a, b) -> a < 0 || b < 0;
     * LongBiPredicate combined = isZero.or(isNegative);
     * boolean result = combined.test(-5L, 10L);   // true (at least one is negative)
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate.
     *              Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default LongBiPredicate or(final LongBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
