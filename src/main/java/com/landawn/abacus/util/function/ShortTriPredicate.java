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
 * Represents a predicate (boolean-valued function) of three short-valued arguments.
 * This is the three-arity specialization of predicate for short values.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(short, short, short)}.
 * 
 * @see ShortPredicate
 * @see ShortBiPredicate
 * @see java.util.function.Predicate
 */
@FunctionalInterface
public interface ShortTriPredicate extends Throwables.ShortTriPredicate<RuntimeException> { //NOSONAR

    /**
     * A predicate that always returns {@code true} regardless of the input values.
     */
    ShortTriPredicate ALWAYS_TRUE = (a, b, c) -> true;

    /**
     * A predicate that always returns {@code false} regardless of the input values.
     */
    ShortTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * boolean result1 = allPositive.test((short) 1, (short) 2, (short) 3); // returns true
     * boolean result2 = allPositive.test((short) -1, (short) 2, (short) 3); // returns false
     *
     * ShortTriPredicate isValidTriangle = (a, b, c) ->
     *     a + b > c && a + c > b && b + c > a;
     * boolean result3 = isValidTriangle.test((short) 3, (short) 4, (short) 5); // returns true
     *
     * ShortTriPredicate inRange = (value, min, max) ->
     *     value >= min && value <= max;
     * boolean result4 = inRange.test((short) 50, (short) 0, (short) 100); // returns true
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false} if the predicate evaluation fails
     */
    @Override
    boolean test(short a, short b, short c);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * 
     * @return a predicate that represents the logical negation of this predicate
     */
    default ShortTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
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
    default ShortTriPredicate and(final ShortTriPredicate other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
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
    default ShortTriPredicate or(final ShortTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}