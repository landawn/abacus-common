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
 * Represents a predicate (boolean-valued function) of three long-valued arguments.
 * This is the three-arity primitive specialization of {@code Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(long, long, long)}.
 *
 * <p>The interface extends {@code Throwables.LongTriPredicate} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * LongTriPredicate isTriangle = (a, b, c) ->
 *     a + b > c && a + c > b && b + c > a;
 * boolean result = isTriangle.test(3L, 4L, 5L);   // returns true
 *
 * LongTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
 * LongTriPredicate sumGreaterThan100 = (a, b, c) -> a + b + c > 100;
 * LongTriPredicate combined = allPositive.and(sumGreaterThan100);
 * }</pre>
 *
 * @see java.util.function.Predicate
 * @see java.util.function.LongPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongTriPredicate extends Throwables.LongTriPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always returns {@code true} regardless of the input values.
     *
     * <p>This constant is useful as a default predicate or for testing purposes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongTriPredicate alwaysTrue = LongTriPredicate.ALWAYS_TRUE;
     * alwaysTrue.test(1L, 2L, 3L);    // returns true
     * alwaysTrue.test(-1L, 0L, 1L);   // returns true
     * }</pre>
     */
    LongTriPredicate ALWAYS_TRUE = (a, b, c) -> true;
    /**
     * A predicate that always returns {@code false} regardless of the input values.
     *
     * <p>This constant is useful as a default predicate or for testing purposes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongTriPredicate alwaysFalse = LongTriPredicate.ALWAYS_FALSE;
     * alwaysFalse.test(1L, 2L, 3L);    // returns false
     * alwaysFalse.test(-1L, 0L, 1L);   // returns false
     * }</pre>
     */
    LongTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p>This method takes three long values and evaluates them according to the
     * predicate's implementation, returning a boolean result.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Validating relationships between three values</li>
     *   <li>Checking if three values form a valid triangle</li>
     *   <li>Testing if three coordinates are within bounds</li>
     *   <li>Verifying business rules involving three numeric parameters</li>
     * </ul>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(long a, long b, long c);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns {@code false},
     * and {@code false} when this predicate returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * LongTriPredicate notAllPositive = allPositive.negate();
     * notAllPositive.test(1L, -2L, 3L);   // returns {@code true} (not all positive)
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default LongTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
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
     * LongTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * LongTriPredicate sumLessThan100 = (a, b, c) -> a + b + c < 100;
     * LongTriPredicate combined = allPositive.and(sumLessThan100);
     * combined.test(10L, 20L, 30L);   // returns {@code true} (all positive AND sum < 100)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default LongTriPredicate and(final LongTriPredicate other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
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
     * LongTriPredicate anyZero = (a, b, c) -> a == 0 || b == 0 || c == 0;
     * LongTriPredicate sumGreaterThan1000 = (a, b, c) -> a + b + c > 1000;
     * LongTriPredicate combined = anyZero.or(sumGreaterThan1000);
     * combined.test(0L, 50L, 50L);   // returns {@code true} (first value is zero)
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default LongTriPredicate or(final LongTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
