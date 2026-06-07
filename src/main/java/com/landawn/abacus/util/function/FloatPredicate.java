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

import java.util.Objects;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a predicate (boolean-valued function) of one {@code float}-valued argument.
 * This is the {@code float}-consuming primitive type specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(float)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * <p><b>Comparison semantics.</b> The relational predicates ({@link #IS_POSITIVE}, {@link #IS_NEGATIVE},
 * {@link #NOT_POSITIVE}, {@link #NOT_NEGATIVE}, {@link #greaterThan(float)}, {@link #greaterThanOrEqual(float)},
 * {@link #lessThan(float)} and {@link #lessThanOrEqual(float)}) use the primitive {@code <}, {@code <=},
 * {@code >}, {@code >=} operators, i.e. IEEE&nbsp;754 ordering: {@code NaN} satisfies none of them and
 * {@code -0.0f} is treated as equal to {@code 0.0f}. The equality-based predicates ({@link #IS_ZERO},
 * {@link #NOT_ZERO}, {@link #equal(float)}, {@link #notEqual(float)}) and {@link #between(float, float)}
 * instead use {@link N#equals(float, float)} / {@link N#compare(float, float)} (total ordering), which
 * treats {@code NaN} as equal to itself and distinguishes {@code -0.0f} from {@code 0.0f}. This split is
 * intentional: relational tests follow intuitive numeric ordering, while equality tests follow the library's
 * {@code equals}/{@code compare} contract. As a consequence, {@code between(min, max)} is not strictly
 * equivalent to {@code greaterThan(min).and(lessThan(max))} for {@code NaN} / {@code -0.0f} inputs.</p>
 *
 * @see java.util.function.Predicate
 * @see FloatBiPredicate
 * @see FloatTriPredicate
 */
@FunctionalInterface
public interface FloatPredicate extends Throwables.FloatPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = FloatPredicate.ALWAYS_TRUE.test(3.14f);    // Always returns true
     * boolean result2 = FloatPredicate.ALWAYS_TRUE.test(-0.0f);   // Always returns true
     * }</pre>
     *
     */
    FloatPredicate ALWAYS_TRUE = value -> true;
    /**
     * A predicate that always evaluates to {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = FloatPredicate.ALWAYS_FALSE.test(3.14f);    // Always returns false
     * boolean result2 = FloatPredicate.ALWAYS_FALSE.test(-0.0f);   // Always returns false
     * }</pre>
     *
     */
    FloatPredicate ALWAYS_FALSE = value -> false;
    /**
     * A predicate that tests if the float value is equal to zero.
     * Uses {@link N#equals(float, float)} for total-ordering equality (see class documentation).
     */
    FloatPredicate IS_ZERO = value -> N.equals(value, 0);
    /**
     * A predicate that tests if the float value is not equal to zero.
     * Uses {@link N#compare(float, float)} for total-ordering comparison (see class documentation).
     */
    FloatPredicate NOT_ZERO = value -> N.compare(value, 0) != 0;
    /**
     * A predicate that tests if the float value is positive, i.e. {@code value > 0}.
     * Returns {@code true} if the value is greater than 0; {@code NaN} is not positive.
     */
    FloatPredicate IS_POSITIVE = value -> value > 0;
    /**
     * A predicate that tests if the float value is not positive, i.e. {@code value <= 0}.
     * Returns {@code true} if the value is less than or equal to 0; {@code NaN} yields {@code false}.
     */
    FloatPredicate NOT_POSITIVE = value -> value <= 0;
    /**
     * A predicate that tests if the float value is negative, i.e. {@code value < 0}.
     * Returns {@code true} if the value is less than 0; {@code NaN} and {@code -0.0f} are not negative.
     */
    FloatPredicate IS_NEGATIVE = value -> value < 0;
    /**
     * A predicate that tests if the float value is not negative, i.e. {@code value >= 0}.
     * Returns {@code true} if the value is greater than or equal to 0; {@code NaN} yields {@code false}.
     */
    FloatPredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     * Evaluates this predicate on the given float value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate isPositive = value -> value > 0.0f;
     * boolean result = isPositive.test(3.14f);   // Returns true
     * }</pre>
     *
     * @param value the float value to test
     * @return {@code true} if the input argument matches the predicate, {@code false} otherwise
     */
    @Override
    boolean test(float value);

    /**
     * Returns the specified predicate instance.
     *
     * <p>This method is useful for explicit type declaration or method references.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate pred = FloatPredicate.of(value -> value > 0.0f);
     * boolean result = pred.test(3.14f);   // Returns true
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate isPositive = value -> value > 0.0f;
     * FloatPredicate isNotPositive = isPositive.negate();
     * boolean result = isNotPositive.test(-1.5f);   // Returns true
     * }</pre>
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
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate isPositive = value -> value > 0.0f;
     * FloatPredicate isFinite = value -> Float.isFinite(value);
     * FloatPredicate positiveAndFinite = isPositive.and(isFinite);
     * boolean result = positiveAndFinite.test(5.0f);   // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     * @throws NullPointerException if {@code other} is null
     */
    default FloatPredicate and(final FloatPredicate other) {
        Objects.requireNonNull(other);
        return t -> test(t) && other.test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another. When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate isNegative = value -> value < 0.0f;
     * FloatPredicate isZero = value -> value == 0.0f;
     * FloatPredicate nonPositive = isNegative.or(isZero);
     * boolean result = nonPositive.test(0.0f);   // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     * @throws NullPointerException if {@code other} is null
     */
    default FloatPredicate or(final FloatPredicate other) {
        Objects.requireNonNull(other);
        return t -> test(t) || other.test(t);
    }

    /**
     * Returns a predicate that tests if the float value is equal to the target value.
     * Uses {@link N#equals(float, float)} for total-ordering equality (see class documentation).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate isPi = FloatPredicate.equal(3.14f);
     * boolean result = isPi.test(3.14f);   // Returns true
     * }</pre>
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is equal to {@code targetFloat}
     */
    static FloatPredicate equal(final float targetFloat) { // NOSONAR
        return value -> N.equals(value, targetFloat);
    }

    /**
     * Returns a predicate that tests if the float value is not equal to the target value.
     * Uses {@link N#compare(float, float)} for total-ordering comparison (see class documentation).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate notZero = FloatPredicate.notEqual(0.0f);
     * boolean result = notZero.test(1.0f);   // Returns true
     * }</pre>
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is not equal to {@code targetFloat}
     */
    static FloatPredicate notEqual(final float targetFloat) {
        return value -> N.compare(value, targetFloat) != 0;
    }

    /**
     * Returns a predicate that tests if the float value is greater than the target value, i.e. {@code value > targetFloat}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate greaterThan10 = FloatPredicate.greaterThan(10.0f);
     * boolean result = greaterThan10.test(15.0f);   // Returns true
     * }</pre>
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is greater than {@code targetFloat}
     */
    static FloatPredicate greaterThan(final float targetFloat) {
        return value -> value > targetFloat;
    }

    /**
     * Returns a predicate that tests if the float value is greater than or equal to the target value, i.e. {@code value >= targetFloat}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate atLeastZero = FloatPredicate.greaterThanOrEqual(0.0f);
     * boolean result = atLeastZero.test(0.0f);   // Returns true
     * }</pre>
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is greater than or equal to {@code targetFloat}
     */
    static FloatPredicate greaterThanOrEqual(final float targetFloat) {
        return value -> value >= targetFloat;
    }

    /**
     * Returns a predicate that tests if the float value is less than the target value, i.e. {@code value < targetFloat}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate lessThan100 = FloatPredicate.lessThan(100.0f);
     * boolean result = lessThan100.test(50.0f);   // Returns true
     * }</pre>
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is less than {@code targetFloat}
     */
    static FloatPredicate lessThan(final float targetFloat) {
        return value -> value < targetFloat;
    }

    /**
     * Returns a predicate that tests if the float value is less than or equal to the target value, i.e. {@code value <= targetFloat}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate maxTen = FloatPredicate.lessThanOrEqual(10.0f);
     * boolean result = maxTen.test(10.0f);   // Returns true
     * }</pre>
     *
     * @param targetFloat the value to compare against
     * @return a predicate that tests if the input is less than or equal to {@code targetFloat}
     */
    static FloatPredicate lessThanOrEqual(final float targetFloat) {
        return value -> value <= targetFloat;
    }

    /**
     * Returns a predicate that tests if the float value is strictly between the specified bounds.
     * The test is exclusive, meaning the value must be greater than {@code minValue} and
     * less than {@code maxValue}. Uses {@link N#compare(float, float)} for total-ordering
     * comparison (see class documentation).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatPredicate inRange = FloatPredicate.between(0.0f, 100.0f);
     * boolean result = inRange.test(50.0f);   // Returns true
     * }</pre>
     *
     * @param minValue the exclusive lower bound
     * @param maxValue the exclusive upper bound
     * @return a predicate that tests if the input is between {@code minValue} and {@code maxValue}
     */
    static FloatPredicate between(final float minValue, final float maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
