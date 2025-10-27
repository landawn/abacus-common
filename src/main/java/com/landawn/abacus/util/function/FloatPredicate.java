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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a predicate (boolean-valued function) of one float-valued argument.
 * This is a functional interface whose functional method is {@link #test(float)}.
 * 
 * <p>This is a primitive type specialization of {@link java.util.function.Predicate} for {@code float}.</p>
 * 
 * @since 1.0
 * @see java.util.function.Predicate
 */
@FunctionalInterface
public interface FloatPredicate extends Throwables.FloatPredicate<RuntimeException> { //NOSONAR

    /**
     * A predicate that always evaluates to {@code true}.
     */
    FloatPredicate ALWAYS_TRUE = value -> true;

    /**
     * A predicate that always evaluates to {@code false}.
     */
    FloatPredicate ALWAYS_FALSE = value -> false;

    /**
     * A predicate that tests if the float value is equal to zero.
     * Uses {@link N#equals(float, float)} for comparison to handle floating-point precision.
     */
    FloatPredicate IS_ZERO = value -> N.equals(value, 0);

    /**
     * A predicate that tests if the float value is not equal to zero.
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     */
    FloatPredicate NOT_ZERO = value -> N.compare(value, 0) != 0;

    /**
     * A predicate that tests if the float value is positive (greater than zero).
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     */
    FloatPredicate IS_POSITIVE = value -> N.compare(value, 0) > 0;

    /**
     * A predicate that tests if the float value is not positive (less than or equal to zero).
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     */
    FloatPredicate NOT_POSITIVE = value -> N.compare(value, 0) <= 0;

    /**
     * A predicate that tests if the float value is negative (less than zero).
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     */
    FloatPredicate IS_NEGATIVE = value -> N.compare(value, 0) < 0;

    /**
     * A predicate that tests if the float value is not negative (greater than or equal to zero).
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     */
    FloatPredicate NOT_NEGATIVE = value -> N.compare(value, 0) >= 0;

    /**
     * Evaluates this predicate on the given float value.
     *
     * @param value the float value to test
     * @return {@code true} if the input argument matches the predicate,
     *         otherwise {@code false}
     */
    @Override
    boolean test(float value);

    /**
     * Returns the specified predicate instance.
     * 
     * <p>This method is useful for explicit type declaration or method references.</p>
     *
     * @param predicate the predicate to return
     * @return the specified predicate
     */
    static FloatPredicate of(final FloatPredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default FloatPredicate negate() {
        return t -> !test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another. When evaluating the composed
     * predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default FloatPredicate and(final FloatPredicate other) {
        return t -> test(t) && other.test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another. When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default FloatPredicate or(final FloatPredicate other) {
        return t -> test(t) || other.test(t);
    }

    /**
     * Returns a predicate that tests if the float value is equal to the target value.
     * Uses {@link N#equals(float, float)} for comparison to handle floating-point precision.
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is equal to {@code targetFloat}
     */
    static FloatPredicate equal(final float targetFloat) { // NOSONAR
        return value -> N.equals(value, targetFloat);
    }

    /**
     * Returns a predicate that tests if the float value is not equal to the target value.
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is not equal to {@code targetFloat}
     */
    static FloatPredicate notEqual(final float targetFloat) {
        return value -> N.compare(value, targetFloat) != 0;
    }

    /**
     * Returns a predicate that tests if the float value is greater than the target value.
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is greater than {@code targetFloat}
     */
    static FloatPredicate greaterThan(final float targetFloat) {
        return value -> N.compare(value, targetFloat) > 0;
    }

    /**
     * Returns a predicate that tests if the float value is greater than or equal to the target value.
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is greater than or equal to {@code targetFloat}
     */
    static FloatPredicate greaterEqual(final float targetFloat) {
        return value -> N.compare(value, targetFloat) >= 0;
    }

    /**
     * Returns a predicate that tests if the float value is less than the target value.
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is less than {@code targetFloat}
     */
    static FloatPredicate lessThan(final float targetFloat) {
        return value -> N.compare(value, targetFloat) < 0;
    }

    /**
     * Returns a predicate that tests if the float value is less than or equal to the target value.
     * Uses {@link N#compare(float, float)} for comparison to handle floating-point precision.
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is less than or equal to {@code targetFloat}
     */
    static FloatPredicate lessEqual(final float targetFloat) {
        return value -> N.compare(value, targetFloat) <= 0;
    }

    /**
     * Returns a predicate that tests if the float value is strictly between the specified bounds.
     * The test is exclusive, meaning the value must be greater than {@code minValue} and
     * less than {@code maxValue}. Uses {@link N#compare(float, float)} for comparison
     * to handle floating-point precision.
     *
     * @param minValue the exclusive lower bound
     * @param maxValue the exclusive upper bound
     * @return a predicate that tests if the input is between {@code minValue} and {@code maxValue}
     * @throws IllegalArgumentException if {@code minValue} is greater than or equal to {@code maxValue}
     */
    static FloatPredicate between(final float minValue, final float maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
