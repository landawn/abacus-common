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
 * Represents a predicate (boolean-valued function) of two {@code byte}-valued arguments.
 * This is the primitive type specialization of {@link BiPredicate} for {@code byte}.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(byte, byte)}.
 *
 */
@FunctionalInterface
public interface ByteBiPredicate extends Throwables.ByteBiPredicate<RuntimeException> { //NOSONAR

    /**
     * A predicate that always returns {@code true} regardless of the input arguments.
     */
    ByteBiPredicate ALWAYS_TRUE = (t, u) -> true;

    /**
     * A predicate that always returns {@code false} regardless of the input arguments.
     */
    ByteBiPredicate ALWAYS_FALSE = (t, u) -> false;

    /**
     * A predicate that returns {@code true} if both arguments are equal.
     */
    ByteBiPredicate EQUAL = (t, u) -> t == u;

    /**
     * A predicate that returns {@code true} if the arguments are not equal.
     */
    ByteBiPredicate NOT_EQUAL = (t, u) -> t != u;

    /**
     * A predicate that returns {@code true} if the first argument is greater than the second.
     */
    ByteBiPredicate GREATER_THAN = (t, u) -> t > u;

    /**
     * A predicate that returns {@code true} if the first argument is greater than or equal to the second.
     */
    ByteBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

    /**
     * A predicate that returns {@code true} if the first argument is less than the second.
     */
    ByteBiPredicate LESS_THAN = (t, u) -> t < u;

    /**
     * A predicate that returns {@code true} if the first argument is less than or equal to the second.
     */
    ByteBiPredicate LESS_EQUAL = (t, u) -> t <= u;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(byte t, byte u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ByteBiPredicate negate() {
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
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default ByteBiPredicate and(final ByteBiPredicate other) {
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
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default ByteBiPredicate or(final ByteBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
