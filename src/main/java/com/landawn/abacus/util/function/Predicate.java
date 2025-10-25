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
 * A functional interface that represents a predicate (boolean-valued function) of one argument.
 * 
 * <p>This interface extends both {@link Throwables.Predicate} and {@link java.util.function.Predicate},
 * providing compatibility with the standard Java functional interfaces while adding exception
 * handling capabilities through the Throwables framework.
 *
 * <p>This is a functional interface whose functional method is {@link #test(Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the input to the predicate
 * @see java.util.function.Predicate
 */
@FunctionalInterface
public interface Predicate<T> extends Throwables.Predicate<T, RuntimeException>, java.util.function.Predicate<T> { //NOSONAR

    /**
     * Evaluates this predicate on the given argument.
     *
     * <p>This method tests whether the input argument satisfies the condition
     * represented by this predicate. It should return {@code true} if the input
     * argument matches the predicate's criteria, {@code false} otherwise.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<String> isEmpty = str -> str.isEmpty();
     * Predicate<Integer> isPositive = num -> num > 0;
     * 
     * boolean result1 = isEmpty.test(""); // Returns true
     * boolean result2 = isPositive.test(5); // Returns true
     * }</pre>
     *
     * @param value the input argument to be tested
     * @return {@code true} if the input argument matches the predicate,
     *         otherwise {@code false} if the predicate evaluation fails
     */
    @Override
    boolean test(T value);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns
     * {@code false}, and vice versa. This is useful for inverting conditions without
     * having to write a new predicate implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<String> isEmpty = str -> str.isEmpty();
     * Predicate<String> isNotEmpty = isEmpty.negate();
     * 
     * boolean result = isNotEmpty.test("Hello"); // Returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    @Override
    default Predicate<T> negate() {
        return t -> !test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code false}, then the {@code other} predicate is
     * not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<String> isNotEmpty = str -> !str.isEmpty();
     * Predicate<String> startsWithA = str -> str.startsWith("A");
     * 
     * Predicate<String> isNotEmptyAndStartsWithA = isNotEmpty.and(startsWithA);
     * boolean result = isNotEmptyAndStartsWithA.test("Apple"); // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    @Override
    default Predicate<T> and(final java.util.function.Predicate<? super T> other) {
        return t -> test(t) && other.test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code true}, then the {@code other} predicate is
     * not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<Integer> isEven = num -> num % 2 == 0;
     * Predicate<Integer> isNegative = num -> num < 0;
     * 
     * Predicate<Integer> isEvenOrNegative = isEven.or(isNegative);
     * boolean result1 = isEvenOrNegative.test(4); // Returns true (even)
     * boolean result2 = isEvenOrNegative.test(-3); // Returns true (negative)
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    @Override
    default Predicate<T> or(final java.util.function.Predicate<? super T> other) {
        return t -> test(t) || other.test(t);
    }

    /**
     * Converts this predicate to a {@link Throwables.Predicate} that can throw
     * checked exceptions.
     *
     * <p>This method allows the predicate to be used in contexts where checked
     * exceptions need to be handled. The returned predicate will have the same
     * behavior as this predicate but with the ability to throw the specified
     * exception type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<String> validator = str -> str.matches("[A-Z]+");
     * Throwables.Predicate<String, IOException> throwableValidator = 
     *     validator.toThrowable();
     * 
     * // Can now be used in contexts that handle IOException
     * }</pre>
     *
     * @param <E> the type of exception that the returned predicate can throw
     * @return a {@link Throwables.Predicate} version of this predicate
     */
    default <E extends Throwable> Throwables.Predicate<T, E> toThrowable() {
        return (Throwables.Predicate<T, E>) this;
    }
}