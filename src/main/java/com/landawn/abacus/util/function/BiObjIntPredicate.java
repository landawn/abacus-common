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
 * Represents a predicate (boolean-valued function) of two object-valued arguments and a single
 * {@code int}-valued argument. This is a specialization of predicate for two reference types
 * and one primitive {@code int} value.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(Object, Object, int)}.
 *
 * @param <T> the type of the first object argument to the predicate
 * @param <U> the type of the second object argument to the predicate
 */
@FunctionalInterface
public interface BiObjIntPredicate<T, U> extends Throwables.BiObjIntPredicate<T, U, RuntimeException> { //NOSONAR

    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param t the first input argument (object value)
     * @param u the second input argument (object value)
     * @param i the third input argument (int value)
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(T t, U u, int i);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default BiObjIntPredicate<T, U> negate() {
        return (t, u, i) -> !test(t, u, i);
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
    default BiObjIntPredicate<T, U> and(final BiObjIntPredicate<? super T, ? super U> other) {
        return (t, u, i) -> test(t, u, i) && other.test(t, u, i);
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
    default BiObjIntPredicate<T, U> or(final BiObjIntPredicate<? super T, ? super U> other) {
        return (t, u, i) -> test(t, u, i) || other.test(t, u, i);
    }
}
