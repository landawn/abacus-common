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
 * Represents a predicate (boolean-valued function) of one {@code double}-valued argument.
 * This is the {@code double}-consuming primitive type specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * <p><b>Comparison semantics.</b> The relational predicates ({@link #IS_POSITIVE}, {@link #IS_NEGATIVE},
 * {@link #NOT_POSITIVE}, {@link #NOT_NEGATIVE}, {@link #greaterThan(double)}, {@link #greaterThanOrEqual(double)},
 * {@link #lessThan(double)} and {@link #lessThanOrEqual(double)}) use the primitive {@code <}, {@code <=},
 * {@code >}, {@code >=} operators, i.e. IEEE&nbsp;754 ordering: {@code NaN} satisfies none of them and
 * {@code -0.0} is treated as equal to {@code 0.0}. The equality-based predicates ({@link #IS_ZERO},
 * {@link #NOT_ZERO}, {@link #equal(double)}, {@link #notEqual(double)}) and {@link #between(double, double)}
 * instead use {@link N#equals(double, double)} / {@link N#compare(double, double)} (total ordering), which
 * treats {@code NaN} as equal to itself and distinguishes {@code -0.0} from {@code 0.0}. This split is
 * intentional: relational tests follow intuitive numeric ordering, while equality tests follow the library's
 * {@code equals}/{@code compare} contract. As a consequence, {@code between(min, max)} is not strictly
 * equivalent to {@code greaterThan(min).and(lessThan(max))} for {@code NaN} / {@code -0.0} inputs.</p>
 *
 * @see java.util.function.Predicate
 * @see java.util.function.DoublePredicate
 * @see DoubleBiPredicate
 * @see DoubleTriPredicate
 */
@FunctionalInterface
public interface DoublePredicate extends Throwables.DoublePredicate<RuntimeException>, java.util.function.DoublePredicate { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true}, regardless of the input value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = DoublePredicate.ALWAYS_TRUE.test(3.14);    // Always returns true
     * boolean result2 = DoublePredicate.ALWAYS_TRUE.test(-0.0);   // Always returns true
     * }</pre>
     *
     */
    DoublePredicate ALWAYS_TRUE = value -> true;
    /**
     * A predicate that always evaluates to {@code false}, regardless of the input value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = DoublePredicate.ALWAYS_FALSE.test(3.14);    // Always returns false
     * boolean result2 = DoublePredicate.ALWAYS_FALSE.test(-0.0);   // Always returns false
     * }</pre>
     *
     */
    DoublePredicate ALWAYS_FALSE = value -> false;
    /**
     * A predicate that tests if a double value is zero using {@link N#equals(double, double)}.
     * Returns {@code true} if the value equals 0.
     */
    DoublePredicate IS_ZERO = value -> N.equals(value, 0);
    /**
     * A predicate that tests if a double value is not zero using {@link N#compare(double, double)}.
     * Returns {@code true} if the value does not equal 0.
     */
    DoublePredicate NOT_ZERO = value -> N.compare(value, 0) != 0;
    /**
     * A predicate that tests if a double value is positive, i.e. {@code value > 0}.
     * Returns {@code true} if the value is greater than 0; {@code NaN} is not positive.
     */
    DoublePredicate IS_POSITIVE = value -> value > 0;
    /**
     * A predicate that tests if a double value is not positive, i.e. {@code value <= 0}.
     * Returns {@code true} if the value is less than or equal to 0; {@code NaN} yields {@code false}.
     */
    DoublePredicate NOT_POSITIVE = value -> value <= 0;
    /**
     * A predicate that tests if a double value is negative, i.e. {@code value < 0}.
     * Returns {@code true} if the value is less than 0; {@code NaN} and {@code -0.0} are not negative.
     */
    DoublePredicate IS_NEGATIVE = value -> value < 0;
    /**
     * A predicate that tests if a double value is not negative, i.e. {@code value >= 0}.
     * Returns {@code true} if the value is greater than or equal to 0; {@code NaN} yields {@code false}.
     */
    DoublePredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     * Evaluates this predicate on the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate isPositive = value -> value > 0.0;
     * boolean result = isPositive.test(3.14);   // Returns true
     * }</pre>
     *
     * @param value the input argument
     * @return {@code true} if the input argument matches the predicate, {@code false} otherwise
     */
    @Override
    boolean test(double value);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * The returned predicate will return {@code true} when this predicate returns {@code false},
     * and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate isPositive = value -> value > 0.0;
     * DoublePredicate isNotPositive = isPositive.negate();
     * boolean result = isNotPositive.test(-1.5);   // Returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    @Override
    default DoublePredicate negate() {
        return value -> !test(value);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other} predicate is not evaluated.
     *
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate isPositive = value -> value > 0.0;
     * DoublePredicate isFinite = value -> Double.isFinite(value);
     * DoublePredicate positiveAndFinite = isPositive.and(isFinite);
     * boolean result = positiveAndFinite.test(5.0);   // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     * @throws NullPointerException if {@code other} is null
     */
    @Override
    default DoublePredicate and(final java.util.function.DoublePredicate other) {
        Objects.requireNonNull(other);
        return value -> test(value) && other.test(value);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other} predicate is not evaluated.
     *
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate isNegative = value -> value < 0.0;
     * DoublePredicate isZero = value -> value == 0.0;
     * DoublePredicate nonPositive = isNegative.or(isZero);
     * boolean result = nonPositive.test(0.0);   // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     * @throws NullPointerException if {@code other} is null
     */
    @Override
    default DoublePredicate or(final java.util.function.DoublePredicate other) {
        Objects.requireNonNull(other);
        return value -> test(value) || other.test(value);
    }

    /**
     * Returns the specified {@code DoublePredicate} instance.
     * This method is useful for type inference or when you need to explicitly cast a lambda expression
     * to {@code DoublePredicate} (for example, to make the {@code negate}/{@code and}/{@code or}
     * default methods available on a bare lambda).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate pred = DoublePredicate.of(value -> value > 0.0);
     * boolean result = pred.test(3.14);   // Returns true
     * }</pre>
     *
     * @param predicate the predicate to return
     * @return the same predicate instance
     */
    static DoublePredicate of(final DoublePredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that tests if a double value is equal to the target value using {@link N#equals(double, double)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate isPi = DoublePredicate.equal(3.14159);
     * boolean result = isPi.test(3.14159);   // Returns true
     * }</pre>
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is equal to {@code targetDouble}
     */
    static DoublePredicate equal(final double targetDouble) { // NOSONAR
        return value -> N.equals(value, targetDouble);
    }

    /**
     * Returns a predicate that tests if a double value is not equal to the target value using {@link N#compare(double, double)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate notZero = DoublePredicate.notEqual(0.0);
     * boolean result = notZero.test(1.0);   // Returns true
     * }</pre>
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is not equal to {@code targetDouble}
     */
    static DoublePredicate notEqual(final double targetDouble) {
        return value -> N.compare(value, targetDouble) != 0;
    }

    /**
     * Returns a predicate that tests if a double value is greater than the target value, i.e. {@code value > targetDouble}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate greaterThan10 = DoublePredicate.greaterThan(10.0);
     * boolean result = greaterThan10.test(15.0);   // Returns true
     * }</pre>
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is greater than {@code targetDouble}
     */
    static DoublePredicate greaterThan(final double targetDouble) {
        return value -> value > targetDouble;
    }

    /**
     * Returns a predicate that tests if a double value is greater than or equal to the target value, i.e. {@code value >= targetDouble}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate atLeastZero = DoublePredicate.greaterThanOrEqual(0.0);
     * boolean result = atLeastZero.test(0.0);   // Returns true
     * }</pre>
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is greater than or equal to {@code targetDouble}
     */
    static DoublePredicate greaterThanOrEqual(final double targetDouble) {
        return value -> value >= targetDouble;
    }

    /**
     * Returns a predicate that tests if a double value is less than the target value, i.e. {@code value < targetDouble}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate lessThan100 = DoublePredicate.lessThan(100.0);
     * boolean result = lessThan100.test(50.0);   // Returns true
     * }</pre>
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is less than {@code targetDouble}
     */
    static DoublePredicate lessThan(final double targetDouble) {
        return value -> value < targetDouble;
    }

    /**
     * Returns a predicate that tests if a double value is less than or equal to the target value, i.e. {@code value <= targetDouble}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate maxTen = DoublePredicate.lessThanOrEqual(10.0);
     * boolean result = maxTen.test(10.0);   // Returns true
     * }</pre>
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is less than or equal to {@code targetDouble}
     */
    static DoublePredicate lessThanOrEqual(final double targetDouble) {
        return value -> value <= targetDouble;
    }

    /**
     * Returns a predicate that tests if a double value is between two values (exclusive) using {@link N#compare(double, double)}.
     * The predicate returns {@code true} if the value is greater than {@code minValue} and less than {@code maxValue}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate inRange = DoublePredicate.between(0.0, 100.0);
     * boolean result = inRange.test(50.0);   // Returns true
     * }</pre>
     *
     * @param minValue the lower bound (exclusive)
     * @param maxValue the upper bound (exclusive)
     * @return a predicate that tests if the input is between {@code minValue} and {@code maxValue}
     */
    static DoublePredicate between(final double minValue, final double maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
