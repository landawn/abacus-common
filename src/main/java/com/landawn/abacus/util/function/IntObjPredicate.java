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
 * Represents a predicate (boolean-valued function) of an {@code int}-valued and an object-valued argument.
 * This is the {@code (int, reference)} specialization of {@link java.util.function.BiPredicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(int, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the predicate
 * @see java.util.function.BiPredicate
 * @see IntPredicate
 */
@FunctionalInterface
public interface IntObjPredicate<T> extends Throwables.IntObjPredicate<T, RuntimeException> { // NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntObjPredicate<String> indexInRange = (index, str) -> index >= 0 && index < str.length();
     * boolean valid = indexInRange.test(5, "Hello World");  // Returns true
     * }</pre>
     *
     * @param t the {@code int} argument
     * @param u the object argument
     * @return {@code true} if the input arguments match the predicate, {@code false} otherwise
     */
    @Override
    boolean test(int t, T u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default IntObjPredicate<T> negate() {
        return (i, t) -> !test(i, t);
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
    default IntObjPredicate<T> and(final IntObjPredicate<T> other) {
        return (i, t) -> test(i, t) && other.test(i, t);
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
    default IntObjPredicate<T> or(final IntObjPredicate<T> other) {
        return (i, t) -> test(i, t) || other.test(i, t);
    }
}
