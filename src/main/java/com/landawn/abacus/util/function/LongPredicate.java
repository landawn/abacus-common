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
 * Represents a predicate (boolean-valued function) of one long-valued argument.
 *
 * <p>This is a functional interface whose functional method is {@link #test(long)}.
 *
 * <p>The interface extends both {@code Throwables.LongPredicate} with {@code RuntimeException}
 * and {@code java.util.function.LongPredicate}, providing compatibility with the Java standard library
 * while adding additional utility methods and predefined predicates.
 *
 * @see java.util.function.LongPredicate
 * @see java.util.function.Predicate
 * @see LongBiPredicate
 * @see LongTriPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongPredicate extends Throwables.LongPredicate<RuntimeException>, java.util.function.LongPredicate { //NOSONAR
    /**
     * A predicate that always returns {@code true} regardless of the input value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate alwaysPass = LongPredicate.ALWAYS_TRUE;
     * alwaysPass.test(42L);   // returns true
     * alwaysPass.test(-1L);   // returns true
     * }</pre>
     */
    LongPredicate ALWAYS_TRUE = value -> true;
    /**
     * A predicate that always returns {@code false} regardless of the input value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate alwaysFail = LongPredicate.ALWAYS_FALSE;
     * alwaysFail.test(42L);   // returns false
     * alwaysFail.test(-1L);   // returns false
     * }</pre>
     */
    LongPredicate ALWAYS_FALSE = value -> false;
    /**
     * A predicate that tests if the input value is zero.
     *
     * <p>Returns {@code true} if and only if the input value equals 0L.
     */
    LongPredicate IS_ZERO = value -> value == 0;
    /**
     * A predicate that tests if the input value is not zero.
     *
     * <p>Returns {@code true} if the input value is any value other than 0L.
     */
    LongPredicate NOT_ZERO = value -> value != 0;
    /**
     * A predicate that tests if the input value is positive.
     *
     * <p>Returns {@code true} if the input value is greater than 0L.
     */
    LongPredicate IS_POSITIVE = value -> value > 0;
    /**
     * A predicate that tests if the input value is not positive.
     *
     * <p>Returns {@code true} if the input value is less than or equal to 0L.
     */
    LongPredicate NOT_POSITIVE = value -> value <= 0;
    /**
     * A predicate that tests if the input value is negative.
     *
     * <p>Returns {@code true} if the input value is less than 0L.
     */
    LongPredicate IS_NEGATIVE = value -> value < 0;
    /**
     * A predicate that tests if the input value is not negative.
     *
     * <p>Returns {@code true} if the input value is greater than or equal to 0L.
     */
    LongPredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     * Evaluates this predicate on the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate isEven = value -> value % 2 == 0;
     * boolean result = isEven.test(4L);  // Returns true
     * }</pre>
     *
     * @param value the input argument
     * @return {@code true} if the input argument matches the predicate, {@code false} otherwise
     */
    @Override
    boolean test(long value);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns {@code false},
     * and {@code false} when this predicate returns {@code true}.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    @Override
    default LongPredicate negate() {
        return value -> !test(value);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another.
     *
     * <p>When evaluating the composed predicate, if this predicate is {@code false},
     * then the {@code other} predicate is not evaluated.
     *
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    @Override
    default LongPredicate and(final java.util.function.LongPredicate other) {
        return value -> test(value) && other.test(value);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.
     *
     * <p>When evaluating the composed predicate, if this predicate is {@code true},
     * then the {@code other} predicate is not evaluated.
     *
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    @Override
    default LongPredicate or(final java.util.function.LongPredicate other) {
        return value -> test(value) || other.test(value);
    }

    /**
     * Returns the specified predicate instance.
     *
     * <p>This is a utility method that simply returns the input predicate,
     * useful for method chaining or type conversion contexts.
     *
     * @param predicate the predicate to return
     * @return the specified predicate
     */
    static LongPredicate of(final LongPredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that tests if the input value is equal to the specified target value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate isFortyTwo = LongPredicate.equal(42L);
     * isFortyTwo.test(42L);   // returns true
     * isFortyTwo.test(41L);   // returns false
     * }</pre>
     *
     * @param targetLong the value to compare against
     * @return a predicate that tests if the input equals {@code targetLong}
     */
    static LongPredicate equal(final long targetLong) { //NOSONAR
        return value -> value == targetLong;
    }

    /**
     * Returns a predicate that tests if the input value is not equal to the specified target value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate notFortyTwo = LongPredicate.notEqual(42L);
     * notFortyTwo.test(42L);   // returns false
     * notFortyTwo.test(41L);   // returns true
     * }</pre>
     *
     * @param targetLong the value to compare against
     * @return a predicate that tests if the input does not equal {@code targetLong}
     */
    static LongPredicate notEqual(final long targetLong) {
        return value -> value != targetLong;
    }

    /**
     * Returns a predicate that tests if the input value is greater than the specified target value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate greaterThanTen = LongPredicate.greaterThan(10L);
     * greaterThanTen.test(11L);   // returns true
     * greaterThanTen.test(10L);   // returns false
     * }</pre>
     *
     * @param targetLong the value to compare against
     * @return a predicate that tests if the input is greater than {@code targetLong}
     */
    static LongPredicate greaterThan(final long targetLong) {
        return value -> value > targetLong;
    }

    /**
     * Returns a predicate that tests if the input value is greater than or equal to the specified target value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate atLeastTen = LongPredicate.greaterEqual(10L);
     * atLeastTen.test(10L);   // returns true
     * atLeastTen.test(9L);    // returns false
     * }</pre>
     *
     * @param targetLong the value to compare against
     * @return a predicate that tests if the input is greater than or equal to {@code targetLong}
     */
    static LongPredicate greaterEqual(final long targetLong) {
        return value -> value >= targetLong;
    }

    /**
     * Returns a predicate that tests if the input value is less than the specified target value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate lessThanTen = LongPredicate.lessThan(10L);
     * lessThanTen.test(9L);    // returns true
     * lessThanTen.test(10L);   // returns false
     * }</pre>
     *
     * @param targetLong the value to compare against
     * @return a predicate that tests if the input is less than {@code targetLong}
     */
    static LongPredicate lessThan(final long targetLong) {
        return value -> value < targetLong;
    }

    /**
     * Returns a predicate that tests if the input value is less than or equal to the specified target value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate atMostTen = LongPredicate.lessEqual(10L);
     * atMostTen.test(10L);   // returns true
     * atMostTen.test(11L);   // returns false
     * }</pre>
     *
     * @param targetLong the value to compare against
     * @return a predicate that tests if the input is less than or equal to {@code targetLong}
     */
    static LongPredicate lessEqual(final long targetLong) {
        return value -> value <= targetLong;
    }

    /**
     * Returns a predicate that tests if the input value is between the specified minimum and maximum values (exclusive).
     *
     * <p>The predicate returns {@code true} if the input value is strictly greater than {@code minValue}
     * and strictly less than {@code maxValue}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongPredicate inRange = LongPredicate.between(10L, 20L);
     * inRange.test(15L);   // returns true
     * inRange.test(10L);   // returns false (not greater than min)
     * inRange.test(20L);   // returns false (not less than max)
     * }</pre>
     *
     * @param minValue the exclusive lower bound
     * @param maxValue the exclusive upper bound
     * @return a predicate that tests if the input is between {@code minValue} and {@code maxValue} (exclusive)
     */
    static LongPredicate between(final long minValue, final long maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
