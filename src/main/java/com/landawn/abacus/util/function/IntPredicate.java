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
 * Represents a predicate (boolean-valued function) of one {@code int}-valued argument.
 * This is the {@code int}-consuming primitive type specialization of {@link java.util.function.Predicate}.
 *
 * <p>This interface extends both {@link java.util.function.IntPredicate} and
 * {@link Throwables.IntPredicate}, providing compatibility with the standard Java functional
 * interfaces while also supporting the Throwables framework.
 *
 * <p>This is a functional interface whose functional method is {@link #test(int)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @see java.util.function.Predicate
 */
@FunctionalInterface
public interface IntPredicate extends Throwables.IntPredicate<RuntimeException>, java.util.function.IntPredicate { //NOSONAR

    /**
     * A predicate that always evaluates to {@code true}.
     */
    IntPredicate ALWAYS_TRUE = value -> true;

    /**
     * A predicate that always evaluates to {@code false}.
     */
    IntPredicate ALWAYS_FALSE = value -> false;

    /**
     * A predicate that tests if an {@code int} value is zero.
     */
    IntPredicate IS_ZERO = value -> value == 0;

    /**
     * A predicate that tests if an {@code int} value is not zero.
     */
    IntPredicate NOT_ZERO = value -> value != 0;

    /**
     * A predicate that tests if an {@code int} value is positive (greater than zero).
     */
    IntPredicate IS_POSITIVE = value -> value > 0;

    /**
     * A predicate that tests if an {@code int} value is not positive (less than or equal to zero).
     */
    IntPredicate NOT_POSITIVE = value -> value <= 0;

    /**
     * A predicate that tests if an {@code int} value is negative (less than zero).
     */
    IntPredicate IS_NEGATIVE = value -> value < 0;

    /**
     * A predicate that tests if an {@code int} value is not negative (greater than or equal to zero).
     */
    IntPredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param value the input argument
     * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
     */
    @Override
    boolean test(int value);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    @Override
    default IntPredicate negate() {
        return value -> !test(value);
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
    @Override
    default IntPredicate and(final java.util.function.IntPredicate other) {
        return value -> test(value) && other.test(value);
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
    @Override
    default IntPredicate or(final java.util.function.IntPredicate other) {
        return value -> test(value) || other.test(value);
    }

    /**
     * Returns the specified {@code IntPredicate} instance. This method exists for symmetry with
     * other functional interface types.
     *
     * @param predicate the predicate to return
     * @return the specified predicate
     */
    static IntPredicate of(final IntPredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that tests if an {@code int} value is equal to the target value.
     *
     * @param targetInt the value to compare against
     * @return a predicate that tests if an {@code int} value is equal to {@code targetInt}
     */
    static IntPredicate equal(final int targetInt) { //NOSONAR
        return value -> value == targetInt;
    }

    /**
     * Returns a predicate that tests if an {@code int} value is not equal to the target value.
     *
     * @param targetInt the value to compare against
     * @return a predicate that tests if an {@code int} value is not equal to {@code targetInt}
     */
    static IntPredicate notEqual(final int targetInt) {
        return value -> value != targetInt;
    }

    /**
     * Returns a predicate that tests if an {@code int} value is greater than the target value.
     *
     * @param targetInt the value to compare against
     * @return a predicate that tests if an {@code int} value is greater than {@code targetInt}
     */
    static IntPredicate greaterThan(final int targetInt) {
        return value -> value > targetInt;
    }

    /**
     * Returns a predicate that tests if an {@code int} value is greater than or equal to the target value.
     *
     * @param targetInt the value to compare against
     * @return a predicate that tests if an {@code int} value is greater than or equal to {@code targetInt}
     */
    static IntPredicate greaterEqual(final int targetInt) {
        return value -> value >= targetInt;
    }

    /**
     * Returns a predicate that tests if an {@code int} value is less than the target value.
     *
     * @param targetInt the value to compare against
     * @return a predicate that tests if an {@code int} value is less than {@code targetInt}
     */
    static IntPredicate lessThan(final int targetInt) {
        return value -> value < targetInt;
    }

    /**
     * Returns a predicate that tests if an {@code int} value is less than or equal to the target value.
     *
     * @param targetInt the value to compare against
     * @return a predicate that tests if an {@code int} value is less than or equal to {@code targetInt}
     */
    static IntPredicate lessEqual(final int targetInt) {
        return value -> value <= targetInt;
    }

    /**
     * Returns a predicate that tests if an {@code int} value is between two values (exclusive).
     * The value must be strictly greater than {@code minValue} and strictly less than {@code maxValue}.
     *
     * @param minValue the lower bound (exclusive)
     * @param maxValue the upper bound (exclusive)
     * @return a predicate that tests if an {@code int} value is between {@code minValue} and {@code maxValue}
     * @throws IllegalArgumentException if {@code minValue} is greater than or equal to {@code maxValue}
     */
    static IntPredicate between(final int minValue, final int maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}