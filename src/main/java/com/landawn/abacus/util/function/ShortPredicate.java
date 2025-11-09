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
 * Represents a predicate (boolean-valued function) of one short-valued argument.
 * This is the short-consuming primitive type specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(short)}.
 *
 * @see java.util.function.Predicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ShortPredicate extends Throwables.ShortPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always returns {@code true} regardless of the input value.
     */
    ShortPredicate ALWAYS_TRUE = value -> true;
    /**
     * A predicate that always returns {@code false} regardless of the input value.
     */
    ShortPredicate ALWAYS_FALSE = value -> false;
    /**
     * A predicate that tests if a short value is zero.
     * Returns {@code true} if and only if the input value is 0.
     */
    ShortPredicate IS_ZERO = value -> value == 0;
    /**
     * A predicate that tests if a short value is not zero.
     * Returns {@code true} if and only if the input value is not 0.
     */
    ShortPredicate NOT_ZERO = value -> value != 0;
    /**
     * A predicate that tests if a short value is positive.
     * Returns {@code true} if and only if the input value is greater than 0.
     */
    ShortPredicate IS_POSITIVE = value -> value > 0;
    /**
     * A predicate that tests if a short value is not positive.
     * Returns {@code true} if and only if the input value is less than or equal to 0.
     */
    ShortPredicate NOT_POSITIVE = value -> value <= 0;
    /**
     * A predicate that tests if a short value is negative.
     * Returns {@code true} if and only if the input value is less than 0.
     */
    ShortPredicate IS_NEGATIVE = value -> value < 0;
    /**
     * A predicate that tests if a short value is not negative.
     * Returns {@code true} if and only if the input value is greater than or equal to 0.
     */
    ShortPredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     * Evaluates this predicate on the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortPredicate isPositive = ShortPredicate.IS_POSITIVE;
     * boolean result1 = isPositive.test((short) 10); // returns true
     * boolean result2 = isPositive.test((short) -5); // returns false
     *
     * ShortPredicate inRange = ShortPredicate.between((short) 10, (short) 100);
     * boolean result3 = inRange.test((short) 50); // returns true
     * boolean result4 = inRange.test((short) 5); // returns false
     *
     * ShortPredicate isEven = value -> value % 2 == 0;
     * boolean result5 = isEven.test((short) 4); // returns true
     * }</pre>
     *
     * @param value the input argument
     * @return {@code true} if the input argument matches the predicate, {@code false} otherwise
     */
    @Override
    boolean test(short value);

    /**
     * Returns the specified predicate instance.
     * This method exists primarily for consistency and readability in method chains.
     *
     * @param predicate the predicate to return
     * @return the specified predicate
     */
    static ShortPredicate of(final ShortPredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ShortPredicate negate() {
        return t -> !test(t);
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
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default ShortPredicate and(final ShortPredicate other) {
        return t -> test(t) && other.test(t);
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
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default ShortPredicate or(final ShortPredicate other) {
        return t -> test(t) || other.test(t);
    }

    /**
     * Returns a predicate that tests if a short value is equal to the specified target value.
     *
     * @param targetShort the value to compare against
     * @return a predicate that tests if the input is equal to {@code targetShort}
     */
    static ShortPredicate equal(final short targetShort) { //NOSONAR
        return value -> value == targetShort;
    }

    /**
     * Returns a predicate that tests if a short value is not equal to the specified target value.
     *
     * @param targetShort the value to compare against
     * @return a predicate that tests if the input is not equal to {@code targetShort}
     */
    static ShortPredicate notEqual(final short targetShort) {
        return value -> value != targetShort;
    }

    /**
     * Returns a predicate that tests if a short value is greater than the specified target value.
     *
     * @param targetShort the value to compare against
     * @return a predicate that tests if the input is greater than {@code targetShort}
     */
    static ShortPredicate greaterThan(final short targetShort) {
        return value -> value > targetShort;
    }

    /**
     * Returns a predicate that tests if a short value is greater than or equal to the specified target value.
     *
     * @param targetShort the value to compare against
     * @return a predicate that tests if the input is greater than or equal to {@code targetShort}
     */
    static ShortPredicate greaterEqual(final short targetShort) {
        return value -> value >= targetShort;
    }

    /**
     * Returns a predicate that tests if a short value is less than the specified target value.
     *
     * @param targetShort the value to compare against
     * @return a predicate that tests if the input is less than {@code targetShort}
     */
    static ShortPredicate lessThan(final short targetShort) {
        return value -> value < targetShort;
    }

    /**
     * Returns a predicate that tests if a short value is less than or equal to the specified target value.
     *
     * @param targetShort the value to compare against
     * @return a predicate that tests if the input is less than or equal to {@code targetShort}
     */
    static ShortPredicate lessEqual(final short targetShort) {
        return value -> value <= targetShort;
    }

    /**
     * Returns a predicate that tests if a short value is between the specified minimum and maximum values (exclusive).
     * The predicate returns {@code true} if and only if {@code minValue < value < maxValue}.
     *
     * @param minValue the exclusive lower bound
     * @param maxValue the exclusive upper bound
     * @return a predicate that tests if the input is strictly between {@code minValue} and {@code maxValue}
     */
    static ShortPredicate between(final short minValue, final short maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
