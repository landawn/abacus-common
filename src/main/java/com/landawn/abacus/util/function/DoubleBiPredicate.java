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
 * Represents a predicate (boolean-valued function) of two double-valued arguments.
 * This is the two-arity specialization of {@link java.util.function.Predicate}.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(double, double)}.
 *
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 * @see DoublePredicate
 */
@FunctionalInterface
public interface DoubleBiPredicate extends Throwables.DoubleBiPredicate<RuntimeException> { //NOSONAR

    /**
     * A predicate that always evaluates to {@code true}, regardless of the input values.
     */
    DoubleBiPredicate ALWAYS_TRUE = (t, u) -> true;

    /**
     * A predicate that always evaluates to {@code false}, regardless of the input values.
     */
    DoubleBiPredicate ALWAYS_FALSE = (t, u) -> false;

    /**
     * A predicate that tests if two double values are equal using {@link Double#compare(double, double)}.
     * Returns {@code true} if the comparison result is 0.
     */
    DoubleBiPredicate EQUAL = (t, u) -> Double.compare(t, u) == 0;

    /**
     * A predicate that tests if two double values are not equal using {@link Double#compare(double, double)}.
     * Returns {@code true} if the comparison result is not 0.
     */
    DoubleBiPredicate NOT_EQUAL = (t, u) -> Double.compare(t, u) != 0;

    /**
     * A predicate that tests if the first double value is greater than the second using {@link Double#compare(double, double)}.
     * Returns {@code true} if the first value is greater than the second.
     */
    DoubleBiPredicate GREATER_THAN = (t, u) -> Double.compare(t, u) > 0;

    /**
     * A predicate that tests if the first double value is greater than or equal to the second using {@link Double#compare(double, double)}.
     * Returns {@code true} if the first value is greater than or equal to the second.
     */
    DoubleBiPredicate GREATER_EQUAL = (t, u) -> Double.compare(t, u) >= 0;

    /**
     * A predicate that tests if the first double value is less than the second using {@link Double#compare(double, double)}.
     * Returns {@code true} if the first value is less than the second.
     */
    DoubleBiPredicate LESS_THAN = (t, u) -> Double.compare(t, u) < 0;

    /**
     * A predicate that tests if the first double value is less than or equal to the second using {@link Double#compare(double, double)}.
     * Returns {@code true} if the first value is less than or equal to the second.
     */
    DoubleBiPredicate LESS_EQUAL = (t, u) -> Double.compare(t, u) <= 0;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param t the first double input argument
     * @param u the second double input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(double t, double u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default DoubleBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other} predicate is not evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default DoubleBiPredicate and(final DoubleBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other} predicate is not evaluated.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default DoubleBiPredicate or(final DoubleBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
