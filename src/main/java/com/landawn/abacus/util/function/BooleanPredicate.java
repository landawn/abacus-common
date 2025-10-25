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
 * Represents a predicate (boolean-valued function) of one {@code boolean}-valued argument.
 * This is the {@code boolean}-consuming primitive type specialization of {@link java.util.function.Predicate}.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(boolean)}.
 *
 */
@FunctionalInterface
public interface BooleanPredicate extends Throwables.BooleanPredicate<RuntimeException> { //NOSONAR

    /**
     * A predicate that always returns {@code true} regardless of the input value.
     */
    BooleanPredicate ALWAYS_TRUE = value -> true;

    /**
     * A predicate that always returns {@code false} regardless of the input value.
     */
    BooleanPredicate ALWAYS_FALSE = value -> false;

    /**
     * A predicate that returns the input value itself.
     * Returns {@code true} if the input is {@code true}, {@code false} if the input is {@code false}.
     */
    BooleanPredicate IS_TRUE = value -> value;

    /**
     * A predicate that returns the negation of the input value.
     * Returns {@code true} if the input is {@code false}, {@code false} if the input is {@code true}.
     */
    BooleanPredicate IS_FALSE = value -> !value;

    /**
     * Evaluates this predicate on the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanPredicate isTrue = value -> value;
     * boolean result = isTrue.test(true); // Returns true
     * }</pre>
     *
     * @param value the input argument
     * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
     */
    @Override
    boolean test(boolean value);

    /**
     * Returns the specified predicate instance.
     * This method is useful for type inference in lambda expressions and method references.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanPredicate pred = BooleanPredicate.of(v -> v);
     * boolean result = pred.test(true);
     * }</pre>
     *
     * @param predicate the predicate to return
     * @return the specified predicate
     */
    static BooleanPredicate of(final BooleanPredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanPredicate isTrue = value -> value;
     * BooleanPredicate isFalse = isTrue.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default BooleanPredicate negate() {
        return t -> !test(t);
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
     * BooleanPredicate isTrue = value -> value;
     * BooleanPredicate alwaysTrue = value -> true;
     * BooleanPredicate combined = isTrue.and(alwaysTrue);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default BooleanPredicate and(final BooleanPredicate other) {
        return t -> test(t) && other.test(t);
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
     * BooleanPredicate isTrue = value -> value;
     * BooleanPredicate alwaysFalse = value -> false;
     * BooleanPredicate combined = isTrue.or(alwaysFalse);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default BooleanPredicate or(final BooleanPredicate other) {
        return t -> test(t) || other.test(t);
    }
}