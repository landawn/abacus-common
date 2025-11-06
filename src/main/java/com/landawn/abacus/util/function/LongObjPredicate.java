/*
 * Copyright (C) 2024 HaiYang Li
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
 * Represents a predicate (boolean-valued function) of a long-valued argument and an object argument.
 * This is a two-arity specialization of {@code Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(long, Object)}.
 *
 * <p>The interface extends {@code Throwables.LongObjPredicate} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * @param <T> the type of the object argument to the predicate
 *
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongObjPredicate<T> extends Throwables.LongObjPredicate<T, RuntimeException> { // NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p>This method takes a primitive long value as the first argument and an object of type T
     * as the second argument, then evaluates them to produce a boolean result.
     *
     * @param t the long-valued first argument
     * @param u the object second argument of type T
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(long t, T u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns {@code false},
     * and {@code false} when this predicate returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongObjPredicate<String> isLongGreaterThanLength = (l, s) -> l > s.length();
     * LongObjPredicate<String> isLongNotGreaterThanLength = isLongGreaterThanLength.negate();
     * // isLongNotGreaterThanLength.test(3L, "hello") returns {@code true} (3 is not > 5)
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default LongObjPredicate<T> negate() {
        return (i, t) -> !test(i, t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another.
     *
     * <p>When evaluating the composed predicate, if this predicate is {@code false},
     * then the {@code other} predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongObjPredicate<String> isPositive = (l, s) -> l > 0;
     * LongObjPredicate<String> isLongerThanLong = (l, s) -> s.length() > l;
     * LongObjPredicate<String> both = isPositive.and(isLongerThanLong);
     * // both.test(3L, "hello") returns {@code true} (3 > 0 AND "hello".length() > 3)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default LongObjPredicate<T> and(final LongObjPredicate<T> other) {
        return (i, t) -> test(i, t) && other.test(i, t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.
     *
     * <p>When evaluating the composed predicate, if this predicate is {@code true},
     * then the {@code other} predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongObjPredicate<String> isZero = (l, s) -> l == 0;
     * LongObjPredicate<String> isEmpty = (l, s) -> s.isEmpty();
     * LongObjPredicate<String> either = isZero.or(isEmpty);
     * // either.test(0L, "hello") returns {@code true} (0 == 0)
     * // either.test(5L, "") returns {@code true} (string is empty)
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default LongObjPredicate<T> or(final LongObjPredicate<T> other) {
        return (i, t) -> test(i, t) || other.test(i, t);
    }

    /**
     * Converts this {@code LongObjPredicate} to a {@code Throwables.LongObjPredicate} that can throw a checked exception.
     * This method provides a way to use this predicate in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongObjPredicate predicate = (...) -> { ... };
     * var throwablePredicate = predicate.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned predicate can throw
     * @return a {@code Throwables.LongObjPredicate} view of this predicate that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.LongObjPredicate<T, E> toThrowable() {
        return (Throwables.LongObjPredicate<T, E>) this;
    }

}
