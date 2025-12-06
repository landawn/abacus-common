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
 * Represents a predicate (boolean-valued function) of one double-valued argument.
 * This is the double-consuming primitive type specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
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
     */
    DoublePredicate ALWAYS_TRUE = value -> true;
    /**
     * A predicate that always evaluates to {@code false}, regardless of the input value.
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
     * A predicate that tests if a double value is positive using {@link N#compare(double, double)}.
     * Returns {@code true} if the value is greater than 0.
     */
    DoublePredicate IS_POSITIVE = value -> N.compare(value, 0) > 0;
    /**
     * A predicate that tests if a double value is not positive using {@link N#compare(double, double)}.
     * Returns {@code true} if the value is less than or equal to 0.
     */
    DoublePredicate NOT_POSITIVE = value -> N.compare(value, 0) <= 0;
    /**
     * A predicate that tests if a double value is negative using {@link N#compare(double, double)}.
     * Returns {@code true} if the value is less than 0.
     */
    DoublePredicate IS_NEGATIVE = value -> N.compare(value, 0) < 0;
    /**
     * A predicate that tests if a double value is not negative using {@link N#compare(double, double)}.
     * Returns {@code true} if the value is greater than or equal to 0.
     */
    DoublePredicate NOT_NEGATIVE = value -> N.compare(value, 0) >= 0;

    /**
     * Evaluates this predicate on the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoublePredicate isPositive = value -> value > 0.0;
     * boolean result = isPositive.test(3.14);   // Returns true
     * }</pre>
     *
     * @param value the double input argument
     * @return {@code true} if the input argument matches the predicate, {@code false} otherwise
     */
    @Override
    boolean test(double value);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
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
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    @Override
    default DoublePredicate and(final java.util.function.DoublePredicate other) {
        return value -> test(value) && other.test(value);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other} predicate is not evaluated.
     *
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    @Override
    default DoublePredicate or(final java.util.function.DoublePredicate other) {
        return value -> test(value) || other.test(value);
    }

    /**
     * Returns the specified {@code DoublePredicate} instance.
     * This method exists primarily for API consistency.
     *
     * @param predicate the predicate to return
     * @return the specified predicate
     */
    static DoublePredicate of(final DoublePredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that tests if a double value is equal to the target value using {@link N#equals(double, double)}.
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
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is not equal to {@code targetDouble}
     */
    static DoublePredicate notEqual(final double targetDouble) {
        return value -> N.compare(value, targetDouble) != 0;
    }

    /**
     * Returns a predicate that tests if a double value is greater than the target value using {@link N#compare(double, double)}.
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is greater than {@code targetDouble}
     */
    static DoublePredicate greaterThan(final double targetDouble) {
        return value -> N.compare(value, targetDouble) > 0;
    }

    /**
     * Returns a predicate that tests if a double value is greater than or equal to the target value using {@link N#compare(double, double)}.
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is greater than or equal to {@code targetDouble}
     */
    static DoublePredicate greaterEqual(final double targetDouble) {
        return value -> N.compare(value, targetDouble) >= 0;
    }

    /**
     * Returns a predicate that tests if a double value is less than the target value using {@link N#compare(double, double)}.
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is less than {@code targetDouble}
     */
    static DoublePredicate lessThan(final double targetDouble) {
        return value -> N.compare(value, targetDouble) < 0;
    }

    /**
     * Returns a predicate that tests if a double value is less than or equal to the target value using {@link N#compare(double, double)}.
     *
     * @param targetDouble the value to compare against
     * @return a predicate that tests if the input is less than or equal to {@code targetDouble}
     */
    static DoublePredicate lessEqual(final double targetDouble) {
        return value -> N.compare(value, targetDouble) <= 0;
    }

    /**
     * Returns a predicate that tests if a double value is between two values (exclusive) using {@link N#compare(double, double)}.
     * The predicate returns {@code true} if the value is greater than {@code minValue} and less than {@code maxValue}.
     *
     * @param minValue the lower bound (exclusive)
     * @param maxValue the upper bound (exclusive)
     * @return a predicate that tests if the input is between {@code minValue} and {@code maxValue}
     */
    static DoublePredicate between(final double minValue, final double maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
