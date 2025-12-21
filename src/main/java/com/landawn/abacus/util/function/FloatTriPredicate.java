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
 * Represents a predicate (boolean-valued function) of three float-valued arguments.
 * This is a functional interface whose functional method is {@link #test(float, float, float)}.
 *
 * <p>This is a primitive type specialization of predicate for three {@code float} arguments.
 * This interface is similar to {@link java.util.function.BiPredicate} but accepts three arguments
 * instead of two.</p>
 *
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 * @see FloatPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface FloatTriPredicate extends Throwables.FloatTriPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true} regardless of the input values.
     */
    FloatTriPredicate ALWAYS_TRUE = (a, b, c) -> true;
    /**
     * A predicate that always evaluates to {@code false} regardless of the input values.
     */
    FloatTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     * Evaluates this predicate on the given float arguments.
     *
     * <p>The implementation should define the condition under which the three float
     * arguments satisfy this predicate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * boolean result = allPositive.test(1.0f, 2.0f, 3.0f); // Returns true
     *
     * FloatTriPredicate sumGreaterThan10 = (a, b, c) -> (a + b + c) > 10.0f;
     * boolean check = sumGreaterThan10.test(4.0f, 5.0f, 2.0f); // Returns true
     * }</pre>
     *
     * @param a the first float input argument
     * @param b the second float input argument
     * @param c the third float input argument
     * @return {@code true} if the input arguments match the predicate,
     *         otherwise {@code false}
     */
    @Override
    boolean test(float a, float b, float c);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * FloatTriPredicate notAllPositive = allPositive.negate();
     * boolean result = notAllPositive.test(-1.0f, 2.0f, 3.0f);   // Returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default FloatTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another. When evaluating the composed
     * predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * FloatTriPredicate sumLessThan100 = (a, b, c) -> a + b + c < 100.0f;
     * FloatTriPredicate combined = allPositive.and(sumLessThan100);
     * boolean result = combined.test(10.0f, 20.0f, 30.0f);   // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default FloatTriPredicate and(final FloatTriPredicate other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another. When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatTriPredicate anyNegative = (a, b, c) -> a < 0 || b < 0 || c < 0;
     * FloatTriPredicate anyZero = (a, b, c) -> a == 0 || b == 0 || c == 0;
     * FloatTriPredicate combined = anyNegative.or(anyZero);
     * boolean result = combined.test(0.0f, 5.0f, 10.0f);   // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default FloatTriPredicate or(final FloatTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
