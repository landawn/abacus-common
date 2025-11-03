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
 * Represents a predicate (boolean-valued function) of three byte-valued arguments.
 * This is the three-arity specialization of {@link java.util.function.Predicate} for byte values.
 *
 * <p>This is a functional interface whose functional method is {@link #test(byte, byte, byte)}.
 *
 * @see java.util.function.Predicate
 * @see BytePredicate
 * @see ByteBiPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteTriPredicate extends Throwables.ByteTriPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true}, regardless of the three byte input values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriPredicate alwaysTrue = ByteTriPredicate.ALWAYS_TRUE;
     * boolean result = alwaysTrue.test((byte) 1, (byte) 2, (byte) 3); // Returns true
     * }</pre>
     */
    ByteTriPredicate ALWAYS_TRUE = (a, b, c) -> true;
    /**
     * A predicate that always evaluates to {@code false}, regardless of the three byte input values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriPredicate alwaysFalse = ByteTriPredicate.ALWAYS_FALSE;
     * boolean result = alwaysFalse.test((byte) 1, (byte) 2, (byte) 3); // Returns false
     * }</pre>
     */
    ByteTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     * Evaluates this predicate on the given byte arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * boolean result = allPositive.test((byte) 1, (byte) 2, (byte) 3); // Returns true
     *
     * ByteTriPredicate inRange = (a, b, c) -> a >= b && a <= c;
     * boolean inBounds = inRange.test((byte) 5, (byte) 1, (byte) 10); // Returns true
     * }</pre>
     *
     * @param a the first byte input argument
     * @param b the second byte input argument
     * @param c the third byte input argument
     * @return {@code true} if the three input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(byte a, byte b, byte c);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * The returned predicate will return {@code true} when this predicate returns {@code false},
     * and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * ByteTriPredicate notAllPositive = allPositive.negate();
     * boolean result = notAllPositive.test((byte) -1, (byte) 2, (byte) 3); // Returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ByteTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * ByteTriPredicate allLessThan100 = (a, b, c) -> a < 100 && b < 100 && c < 100;
     * ByteTriPredicate combined = allPositive.and(allLessThan100);
     * boolean result = combined.test((byte) 10, (byte) 20, (byte) 30); // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate
     *         and the {@code other} predicate
     */
    default ByteTriPredicate and(final ByteTriPredicate other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriPredicate anyNegative = (a, b, c) -> a < 0 || b < 0 || c < 0;
     * ByteTriPredicate anyZero = (a, b, c) -> a == 0 || b == 0 || c == 0;
     * ByteTriPredicate combined = anyNegative.or(anyZero);
     * boolean result = combined.test((byte) 0, (byte) 5, (byte) 10); // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate
     *         and the {@code other} predicate
     */
    default ByteTriPredicate or(final ByteTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
