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
 * Represents a predicate (boolean-valued function) of three double-valued arguments.
 * This is the three-arity specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(double, double, double)}.</p>
 *
 * <p>This interface extends {@link Throwables.DoubleTriPredicate} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 *
 * <p>The interface provides two predefined constants:</p>
 * <ul>
 *   <li>{@link #ALWAYS_TRUE} - A predicate that always returns {@code true}</li>
 *   <li>{@link #ALWAYS_FALSE} - A predicate that always returns {@code false}</li>
 * </ul>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Predicate
 * @see java.util.function.DoublePredicate
 * @see java.util.function.BiPredicate
 */
@FunctionalInterface
public interface DoubleTriPredicate extends Throwables.DoubleTriPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true} regardless of the input values.
     * This constant is useful as a default predicate or in scenarios where all values should pass.
     */
    DoubleTriPredicate ALWAYS_TRUE = (a, b, c) -> true;
    /**
     * A predicate that always evaluates to {@code false} regardless of the input values.
     * This constant is useful as a default predicate or in scenarios where all values should fail.
     */
    DoubleTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     * Evaluates this predicate on the given three double arguments.
     *
     * <p>This method tests whether the three double values satisfy the condition
     * implemented by this predicate.</p>
     *
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Validating three-dimensional coordinates (e.g., checking if a point is within bounds)</li>
     *   <li>Testing mathematical relationships (e.g., triangle inequality: a + b &gt; c)</li>
     *   <li>Checking color values (e.g., RGB values within valid range)</li>
     *   <li>Validating measurement constraints</li>
     * </ul>
     *
     * @param a the first double argument to test
     * @param b the second double argument to test
     * @param c the third double argument to test
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(double a, double b, double c);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns {@code false},
     * and {@code false} when this predicate returns {@code true}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleTriPredicate isValidTriangle = (a, b, c) -> a + b > c && b + c > a && a + c > b;
     * DoubleTriPredicate isInvalidTriangle = isValidTriangle.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default DoubleTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate
     * and another. When evaluating the composed predicate, if this predicate is {@code false},
     * then the {@code other} predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the {@code other} predicate will not be evaluated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleTriPredicate isPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * DoubleTriPredicate sumLessThan100 = (a, b, c) -> a + b + c < 100;
     *
     * // Combined predicate: all positive AND sum less than 100
     * DoubleTriPredicate combined = isPositive.and(sumLessThan100);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default DoubleTriPredicate and(final DoubleTriPredicate other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate
     * and another. When evaluating the composed predicate, if this predicate is {@code true},
     * then the {@code other} predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the {@code other} predicate will not be evaluated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleTriPredicate allZero = (a, b, c) -> a == 0 && b == 0 && c == 0;
     * DoubleTriPredicate allOne = (a, b, c) -> a == 1 && b == 1 && c == 1;
     *
     * // Combined predicate: all zeros OR all ones
     * DoubleTriPredicate specialCase = allZero.or(allOne);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default DoubleTriPredicate or(final DoubleTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }

    /**
     * Converts this {@code DoubleTriPredicate} to a {@code Throwables.DoubleTriPredicate} that can throw a checked exception.
     * This method provides a way to use this predicate in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleTriPredicate predicate = (...) -> { ... };
     * var throwablePredicate = predicate.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned predicate can throw
     * @return a {@code Throwables.DoubleTriPredicate} view of this predicate that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.DoubleTriPredicate<E> toThrowable() {
        return (Throwables.DoubleTriPredicate<E>) this;
    }

}
