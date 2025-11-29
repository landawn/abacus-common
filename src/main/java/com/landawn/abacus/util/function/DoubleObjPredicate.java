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
 * Represents a predicate (boolean-valued function) of a double-valued argument and an object-valued argument.
 * This is the (double, reference) specialization of {@link java.util.function.BiPredicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(double, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the predicate
 *
 * @see java.util.function.BiPredicate
 */
@FunctionalInterface
public interface DoubleObjPredicate<T> extends Throwables.DoubleObjPredicate<T, RuntimeException> { // NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleObjPredicate<String> hasLength = (val, str) -> str.length() == (int)val;
     * boolean result = hasLength.test(5.0, "hello");  // Returns true
     *
     * DoubleObjPredicate<List<Double>> contains = (val, list) -> list.contains(val);
     * boolean result2 = contains.test(3.14, Arrays.asList(1.0, 2.0, 3.14));  // Returns true
     * }</pre>
     *
     * @param t the double input argument
     * @param u the object input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(double t, T u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default DoubleObjPredicate<T> negate() {
        return (i, t) -> !test(i, t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other} predicate is not evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default DoubleObjPredicate<T> and(final DoubleObjPredicate<T> other) {
        return (i, t) -> test(i, t) && other.test(i, t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other} predicate is not evaluated.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default DoubleObjPredicate<T> or(final DoubleObjPredicate<T> other) {
        return (i, t) -> test(i, t) || other.test(i, t);
    }
}
