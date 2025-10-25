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
 * Represents a predicate (boolean-valued function) of two {@code boolean}-valued arguments.
 * This is the primitive type specialization of {@link BiPredicate} for {@code boolean}.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(boolean, boolean)}.
 *
 */
@FunctionalInterface
public interface BooleanBiPredicate extends Throwables.BooleanBiPredicate<RuntimeException> { //NOSONAR

    /**
     * A predicate that always returns {@code true} regardless of the input arguments.
     */
    BooleanBiPredicate ALWAYS_TRUE = (t, u) -> true;

    /**
     * A predicate that always returns {@code false} regardless of the input arguments.
     */
    BooleanBiPredicate ALWAYS_FALSE = (t, u) -> false;

    /**
     * A predicate that returns {@code true} if both arguments are {@code true}.
     */
    BooleanBiPredicate BOTH_TRUE = (t, u) -> t && u;

    /**
     * A predicate that returns {@code true} if both arguments are {@code false}.
     */
    BooleanBiPredicate BOTH_FALSE = (t, u) -> !t && !u;

    /**
     * A predicate that returns {@code true} if both arguments are equal.
     */
    BooleanBiPredicate EQUAL = (t, u) -> t == u;

    /**
     * A predicate that returns {@code true} if the arguments are not equal.
     */
    BooleanBiPredicate NOT_EQUAL = (t, u) -> t != u;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanBiPredicate xor = (a, b) -> a != b;
     * boolean result = xor.test(true, false); // Returns true
     * }</pre>
     *
     * @param t the first input argument
     * @param u the second input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(boolean t, boolean u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanBiPredicate bothTrue = (a, b) -> a && b;
     * BooleanBiPredicate notBothTrue = bothTrue.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default BooleanBiPredicate negate() {
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
     * BooleanBiPredicate bothTrue = (a, b) -> a && b;
     * BooleanBiPredicate bothFalse = (a, b) -> !a && !b;
     * BooleanBiPredicate combined = bothTrue.and(bothFalse); // Always false
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default BooleanBiPredicate and(final BooleanBiPredicate other) {
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
     * BooleanBiPredicate anyTrue = (a, b) -> a || b;
     * BooleanBiPredicate allTrue = (a, b) -> a && b;
     * BooleanBiPredicate combined = anyTrue.or(allTrue);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default BooleanBiPredicate or(final BooleanBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}