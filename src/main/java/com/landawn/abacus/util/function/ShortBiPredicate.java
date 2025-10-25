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
 * Represents a predicate (boolean-valued function) of two short-valued arguments.
 * This is the two-arity specialization of predicate for short values.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(short, short)}.
 * 
 * @see java.util.function.BiPredicate
 */
@FunctionalInterface
public interface ShortBiPredicate extends Throwables.ShortBiPredicate<RuntimeException> { //NOSONAR

    /**
     * A predicate that always returns {@code true} regardless of the input values.
     */
    ShortBiPredicate ALWAYS_TRUE = (t, u) -> true;

    /**
     * A predicate that always returns {@code false} regardless of the input values.
     */
    ShortBiPredicate ALWAYS_FALSE = (t, u) -> false;

    /**
     * A predicate that tests if two short values are equal.
     * Returns {@code true} if and only if the first argument equals the second argument.
     */
    ShortBiPredicate EQUAL = (t, u) -> t == u;

    /**
     * A predicate that tests if two short values are not equal.
     * Returns {@code true} if and only if the first argument does not equal the second argument.
     */
    ShortBiPredicate NOT_EQUAL = (t, u) -> t != u;

    /**
     * A predicate that tests if the first short value is greater than the second.
     * Returns {@code true} if and only if the first argument is strictly greater than the second argument.
     */
    ShortBiPredicate GREATER_THAN = (t, u) -> t > u;

    /**
     * A predicate that tests if the first short value is greater than or equal to the second.
     * Returns {@code true} if and only if the first argument is greater than or equal to the second argument.
     */
    ShortBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

    /**
     * A predicate that tests if the first short value is less than the second.
     * Returns {@code true} if and only if the first argument is strictly less than the second argument.
     */
    ShortBiPredicate LESS_THAN = (t, u) -> t < u;

    /**
     * A predicate that tests if the first short value is less than or equal to the second.
     * Returns {@code true} if and only if the first argument is less than or equal to the second argument.
     */
    ShortBiPredicate LESS_EQUAL = (t, u) -> t <= u;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p>This method tests whether the two input short values satisfy the condition
     * represented by this predicate. It returns {@code true} if the arguments match
     * the predicate's criteria, {@code false} otherwise.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiPredicate equals = ShortBiPredicate.EQUAL;
     * boolean result1 = equals.test((short) 5, (short) 5); // returns true
     * boolean result2 = equals.test((short) 3, (short) 7); // returns false
     *
     * ShortBiPredicate lessThan = ShortBiPredicate.LESS_THAN;
     * boolean result3 = lessThan.test((short) 3, (short) 7); // returns true
     *
     * ShortBiPredicate inRange = (value, max) -> value >= 0 && value <= max;
     * boolean result4 = inRange.test((short) 50, (short) 100); // returns true
     * }</pre>
     *
     * @param t the first input argument
     * @param u the second input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false} if the predicate evaluation fails
     */
    @Override
    boolean test(short t, short u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns
     * {@code false}, and vice versa. This is useful for inverting comparison logic
     * without rewriting the condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiPredicate equals = ShortBiPredicate.EQUAL;
     * ShortBiPredicate notEquals = equals.negate();
     *
     * boolean result1 = equals.test((short) 5, (short) 5); // returns true
     * boolean result2 = notEquals.test((short) 5, (short) 5); // returns false
     * boolean result3 = notEquals.test((short) 3, (short) 7); // returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ShortBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiPredicate isPositive = (a, b) -> a > 0 && b > 0;
     * ShortBiPredicate lessThan100 = (a, b) -> a < 100 && b < 100;
     *
     * ShortBiPredicate inRange = isPositive.and(lessThan100);
     * boolean result1 = inRange.test((short) 10, (short) 20); // returns true (both positive and < 100)
     * boolean result2 = inRange.test((short) -5, (short) 50); // returns false (not both positive)
     * boolean result3 = inRange.test((short) 150, (short) 200); // returns false (not both < 100)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default ShortBiPredicate and(final ShortBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiPredicate equals = ShortBiPredicate.EQUAL;
     * ShortBiPredicate bothZero = (a, b) -> a == 0 && b == 0;
     *
     * ShortBiPredicate equalsOrBothZero = equals.or(bothZero);
     * boolean result1 = equalsOrBothZero.test((short) 5, (short) 5); // returns true (equal)
     * boolean result2 = equalsOrBothZero.test((short) 0, (short) 0); // returns true (both zero AND equal)
     * boolean result3 = equalsOrBothZero.test((short) 3, (short) 7); // returns false
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default ShortBiPredicate or(final ShortBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}