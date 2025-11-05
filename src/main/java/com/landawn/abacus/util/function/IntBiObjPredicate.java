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
 * Represents a predicate (boolean-valued function) of one {@code int}-valued and two object-valued arguments.
 * This is a three-arity specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(int, Object, Object)}.
 *
 * @param <T> the type of the first object argument to the predicate
 * @param <U> the type of the second object argument to the predicate
 * @see java.util.function.Predicate
 * @see IntObjPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntBiObjPredicate<T, U> extends Throwables.IntBiObjPredicate<T, U, RuntimeException> { //NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param i the {@code int} argument
     * @param t the first object argument
     * @param u the second object argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(int i, T t, U u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default IntBiObjPredicate<T, U> negate() {
        return (i, t, u) -> !test(i, t, u);
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
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and
     *         the {@code other} predicate
     */
    default IntBiObjPredicate<T, U> and(final IntBiObjPredicate<? super T, ? super U> other) {
        return (i, t, u) -> test(i, t, u) && other.test(i, t, u);
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
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and
     *         the {@code other} predicate
     */
    default IntBiObjPredicate<T, U> or(final IntBiObjPredicate<? super T, ? super U> other) {
        return (i, t, u) -> test(i, t, u) || other.test(i, t, u);
    }

    /**
     * Converts this {@code IntBiObjPredicate} to a {@code Throwables.IntBiObjPredicate} that can throw a checked exception.
     * This method provides a way to use this predicate in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntBiObjPredicate predicate = (...) -> { ... };
     * var throwablePredicate = predicate.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned predicate can throw
     * @return a {@code Throwables.IntBiObjPredicate} view of this predicate that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.IntBiObjPredicate<T, U, E> toThrowable() {
        return (Throwables.IntBiObjPredicate<T, U, E>) this;
    }

}
