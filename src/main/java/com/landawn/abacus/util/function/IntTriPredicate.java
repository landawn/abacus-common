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
 * Represents a predicate (boolean-valued function) of three {@code int}-valued
 * arguments. This is the three-arity specialization of {@link java.util.function.Predicate}.
 *
 * <p>This interface extends {@link Throwables.IntTriPredicate} with
 * {@link RuntimeException}, providing compatibility with the abacus-common framework's
 * exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #test(int, int, int)}.
 *
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntTriPredicate extends Throwables.IntTriPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true} regardless of the input values.
     * This can be useful as a default predicate or in testing scenarios.
     */
    IntTriPredicate ALWAYS_TRUE = (a, b, c) -> true;
    /**
     * A predicate that always evaluates to {@code false} regardless of the input values.
     * This can be useful as a default predicate or in testing scenarios.
     */
    IntTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p>The predicate tests three int values and returns a boolean result.
     * Common use cases include:
     * <ul>
     *   <li>Validating three-dimensional coordinates (e.g., checking if a point is within bounds)</li>
     *   <li>Testing relationships between three values (e.g., triangle inequality)</li>
     *   <li>Checking RGB color values for validity</li>
     *   <li>Verifying conditions on three indices or counters</li>
     *   <li>Implementing three-way comparison logic</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntTriPredicate isValidTriangle = (a, b, c) -> a + b > c && a + c > b && b + c > a;
     * boolean result = isValidTriangle.test(3, 4, 5); // Returns true
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @return {@code true} if the input arguments match the predicate, {@code false} otherwise
     */
    @Override
    boolean test(int a, int b, int c);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>If this predicate returns {@code true} for given inputs, the negated
     * predicate will return {@code false}, and vice versa.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default IntTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
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
     * @param other a predicate that will be logically-ANDed with this predicate.
     *              Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default IntTriPredicate and(final IntTriPredicate other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
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
     * @param other a predicate that will be logically-ORed with this predicate.
     *              Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default IntTriPredicate or(final IntTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
