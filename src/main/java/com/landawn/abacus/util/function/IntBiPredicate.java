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
 * Represents a predicate (boolean-valued function) of two {@code int}-valued arguments.
 * This is the two-arity specialization of {@link java.util.function.Predicate} for {@code int} values.
 *
 * <p>This is a functional interface whose functional method is {@link #test(int, int)}.
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Predicate
 * @see IntPredicate
 */
@FunctionalInterface
public interface IntBiPredicate extends Throwables.IntBiPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true}.
     */
    IntBiPredicate ALWAYS_TRUE = (a, b) -> true;
    /**
     * A predicate that always evaluates to {@code false}.
     */
    IntBiPredicate ALWAYS_FALSE = (a, b) -> false;
    /**
     * A predicate that tests if two {@code int} values are equal.
     */
    IntBiPredicate EQUAL = (a, b) -> a == b;
    /**
     * A predicate that tests if two {@code int} values are not equal.
     */
    IntBiPredicate NOT_EQUAL = (a, b) -> a != b;
    /**
     * A predicate that tests if the first {@code int} value is greater than the second.
     */
    IntBiPredicate GREATER_THAN = (a, b) -> a > b;
    /**
     * A predicate that tests if the first {@code int} value is greater than or equal to the second.
     */
    IntBiPredicate GREATER_THAN_OR_EQUAL = (a, b) -> a >= b;
    /**
     * A predicate that tests if the first {@code int} value is less than the second.
     */
    IntBiPredicate LESS_THAN = (a, b) -> a < b;
    /**
     * A predicate that tests if the first {@code int} value is less than or equal to the second.
     */
    IntBiPredicate LESS_THAN_OR_EQUAL = (a, b) -> a <= b;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntBiPredicate sumGreaterThan10 = (a, b) -> a + b > 10;
     * boolean result = sumGreaterThan10.test(5, 6);   // Returns true
     * }</pre>
     *
     * @param a the first {@code int} argument
     * @param b the second {@code int} argument
     * @return {@code true} if the input arguments match the predicate, {@code false} otherwise
     */
    @Override
    boolean test(int a, int b);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default IntBiPredicate negate() {
        return (a, b) -> !test(a, b);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and
     *         the {@code other} predicate
     */
    default IntBiPredicate and(final IntBiPredicate other) {
        return (a, b) -> test(a, b) && other.test(a, b);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and
     *         the {@code other} predicate
     */
    default IntBiPredicate or(final IntBiPredicate other) {
        return (a, b) -> test(a, b) || other.test(a, b);
    }
}
