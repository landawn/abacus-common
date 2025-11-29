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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a predicate (boolean-valued function) of two arguments.
 * This is the two-arity specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(Object, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the first argument to the predicate
 * @param <U> the type of the second argument to the predicate
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
@FunctionalInterface
public interface BiPredicate<T, U> extends Throwables.BiPredicate<T, U, RuntimeException>, java.util.function.BiPredicate<T, U> { //NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiPredicate<String, Integer> lengthEquals = (str, len) -> str.length() == len;
     * boolean result = lengthEquals.test("hello", 5);  // Returns true
     * }</pre>
     *
     * @param t the first input argument
     * @param u the second input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(T t, U u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiPredicate<String, String> equals = (s1, s2) -> s1.equals(s2);
     * BiPredicate<String, String> notEquals = equals.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    @Override
    default BiPredicate<T, U> negate() {
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
     * BiPredicate<String, String> notNull = (s1, s2) -> s1 != null && s2 != null;
     * BiPredicate<String, String> notEmpty = (s1, s2) -> !s1.isEmpty() && !s2.isEmpty();
     * BiPredicate<String, String> valid = notNull.and(notEmpty);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed {@code BiPredicate} that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    @Override
    default BiPredicate<T, U> and(final java.util.function.BiPredicate<? super T, ? super U> other) {
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
     * BiPredicate<String, Integer> isLong = (str, len) -> str.length() > 100;
     * BiPredicate<String, Integer> hasMinLength = (str, len) -> str.length() >= len;
     * BiPredicate<String, Integer> combined = isLong.or(hasMinLength);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed {@code BiPredicate} that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    @Override
    default BiPredicate<T, U> or(final java.util.function.BiPredicate<? super T, ? super U> other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }

    /**
     * Converts this {@code BiPredicate} to a {@code Throwables.BiPredicate} that can throw a checked exception.
     * This method provides a way to use this predicate in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiPredicate<String, String> fileExists = (dir, name) -> Files.exists(Paths.get(dir, name));
     * var throwablePredicate = fileExists.toThrowable();
     * }</pre>
     *
     * @param <E> the type of exception that the returned predicate can throw
     * @return a {@code Throwables.BiPredicate} view of this predicate that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.BiPredicate<T, U, E> toThrowable() {
        return (Throwables.BiPredicate<T, U, E>) this;
    }
}
