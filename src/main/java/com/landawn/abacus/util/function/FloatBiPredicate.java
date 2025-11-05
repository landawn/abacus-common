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
 * Represents a predicate (boolean-valued function) of two float-valued arguments.
 * This is the two-arity specialization of {@link java.util.function.Predicate} for {@code float} values.
 *
 * <p>This is a functional interface whose functional method is {@link #test(float, float)}.</p>
 *
 * <p>This interface extends {@link Throwables.FloatBiPredicate} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 *
 * <p>The interface provides several predefined constants for common comparison operations:</p>
 * <ul>
 *   <li>{@link #ALWAYS_TRUE} - Always returns {@code true}</li>
 *   <li>{@link #ALWAYS_FALSE} - Always returns {@code false}</li>
 *   <li>{@link #EQUAL} - Tests if two float values are equal</li>
 *   <li>{@link #NOT_EQUAL} - Tests if two float values are not equal</li>
 *   <li>{@link #GREATER_THAN} - Tests if the first value is greater than the second</li>
 *   <li>{@link #GREATER_EQUAL} - Tests if the first value is greater than or equal to the second</li>
 *   <li>{@link #LESS_THAN} - Tests if the first value is less than the second</li>
 *   <li>{@link #LESS_EQUAL} - Tests if the first value is less than or equal to the second</li>
 * </ul>
 *
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 * @see FloatPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface FloatBiPredicate extends Throwables.FloatBiPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true} regardless of the input values.
     * This constant is useful as a default predicate or in scenarios where all value pairs should pass.
     */
    FloatBiPredicate ALWAYS_TRUE = (t, u) -> true;
    /**
     * A predicate that always evaluates to {@code false} regardless of the input values.
     * This constant is useful as a default predicate or in scenarios where all value pairs should fail.
     */
    FloatBiPredicate ALWAYS_FALSE = (t, u) -> false;
    /**
     * A predicate that tests if two float values are equal.
     * Uses {@link Float#compare(float, float)} to properly handle NaN and signed zero values.
     *
     * <p>Note: This comparison follows IEEE 754 standard where:</p>
     * <ul>
     *   <li>NaN is considered equal to itself</li>
     *   <li>0.0f and -0.0f are considered different</li>
     * </ul>
     */
    FloatBiPredicate EQUAL = (t, u) -> Float.compare(t, u) == 0;
    /**
     * A predicate that tests if two float values are not equal.
     * Uses {@link Float#compare(float, float)} to properly handle NaN and signed zero values.
     */
    FloatBiPredicate NOT_EQUAL = (t, u) -> Float.compare(t, u) != 0;
    /**
     * A predicate that tests if the first float value is greater than the second.
     * Uses {@link Float#compare(float, float)} for consistent comparison handling special float values.
     */
    FloatBiPredicate GREATER_THAN = (t, u) -> Float.compare(t, u) > 0;
    /**
     * A predicate that tests if the first float value is greater than or equal to the second.
     * Uses {@link Float#compare(float, float)} for consistent comparison handling special float values.
     */
    FloatBiPredicate GREATER_EQUAL = (t, u) -> Float.compare(t, u) >= 0;
    /**
     * A predicate that tests if the first float value is less than the second.
     * Uses {@link Float#compare(float, float)} for consistent comparison handling special float values.
     */
    FloatBiPredicate LESS_THAN = (t, u) -> Float.compare(t, u) < 0;
    /**
     * A predicate that tests if the first float value is less than or equal to the second.
     * Uses {@link Float#compare(float, float)} for consistent comparison handling special float values.
     */
    FloatBiPredicate LESS_EQUAL = (t, u) -> Float.compare(t, u) <= 0;

    /**
     * Evaluates this predicate on the given two float arguments.
     *
     * <p>This method tests whether the two float values satisfy the condition
     * implemented by this predicate.</p>
     *
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Comparing two float values for equality or ordering</li>
     *   <li>Checking if two values are within a certain range of each other</li>
     *   <li>Testing mathematical relationships between two values</li>
     *   <li>Validating constraints on pairs of measurements</li>
     * </ul>
     *
     * @param t the first float argument to test
     * @param u the second float argument to test
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(float t, float u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns {@code false},
     * and {@code false} when this predicate returns {@code true}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatBiPredicate isClose = (a, b) -> Math.abs(a - b) < 0.001f;
     * FloatBiPredicate notClose = isClose.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default FloatBiPredicate negate() {
        return (t, u) -> !test(t, u);
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
     * FloatBiPredicate bothPositive = (a, b) -> a > 0 && b > 0;
     * FloatBiPredicate sumLessThan10 = (a, b) -> a + b < 10;
     *
     * // Combined: both positive AND sum less than 10
     * FloatBiPredicate combined = bothPositive.and(sumLessThan10);
     * boolean result = combined.test(3.0f, 4.0f); // true
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default FloatBiPredicate and(final FloatBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
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
     * FloatBiPredicate eitherZero = (a, b) -> a == 0 || b == 0;
     * FloatBiPredicate bothNegative = (a, b) -> a < 0 && b < 0;
     *
     * // Combined: either zero OR both negative
     * FloatBiPredicate combined = eitherZero.or(bothNegative);
     * boolean result = combined.test(-1.0f, -2.0f); // true
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default FloatBiPredicate or(final FloatBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }

    /**
     * Converts this {@code FloatBiPredicate} to a {@code Throwables.FloatBiPredicate} that can throw a checked exception.
     * This method provides a way to use this predicate in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatBiPredicate predicate = (...) -> { ... };
     * var throwablePredicate = predicate.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned predicate can throw
     * @return a {@code Throwables.FloatBiPredicate} view of this predicate that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.FloatBiPredicate<E> toThrowable() {
        return (Throwables.FloatBiPredicate<E>) this;
    }

}
