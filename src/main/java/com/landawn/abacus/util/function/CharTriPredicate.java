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
 * Represents a predicate (boolean-valued function) of three char-valued arguments.
 * This is the three-arity specialization of {@link CharPredicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(char, char, char)}.
 *
 * @see java.util.function.Predicate
 * @see CharPredicate
 * @see CharBiPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharTriPredicate extends Throwables.CharTriPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true}, regardless of the three char input values.
     */
    CharTriPredicate ALWAYS_TRUE = (a, b, c) -> true;
    /**
     * A predicate that always evaluates to {@code false}, regardless of the three char input values.
     */
    CharTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     * Evaluates this predicate on the given char arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTriPredicate allLetters = (c1, c2, c3) ->
     *     Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3);
     * boolean result = allLetters.test('a', 'b', 'c'); // Returns true
     *
     * CharTriPredicate allSame = (c1, c2, c3) -> c1 == c2 && c2 == c3;
     * boolean result2 = allSame.test('x', 'x', 'x'); // Returns true
     * }</pre>
     *
     * @param a the first char input argument
     * @param b the second char input argument
     * @param c the third char input argument
     * @return {@code true} if the three input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(char a, char b, char c);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * The returned predicate will return {@code true} when this predicate returns {@code false},
     * and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTriPredicate allSame = (c1, c2, c3) -> c1 == c2 && c2 == c3;
     * CharTriPredicate notAllSame = allSame.negate();
     * boolean result = notAllSame.test('a', 'b', 'c'); // Returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default CharTriPredicate negate() {
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
     * CharTriPredicate allLetters = (c1, c2, c3) ->
     *     Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3);
     * CharTriPredicate allUpperCase = (c1, c2, c3) ->
     *     Character.isUpperCase(c1) && Character.isUpperCase(c2) && Character.isUpperCase(c3);
     * CharTriPredicate combined = allLetters.and(allUpperCase);
     * boolean result = combined.test('A', 'B', 'C'); // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate
     *         and the {@code other} predicate
     */
    default CharTriPredicate and(final CharTriPredicate other) {
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
     * CharTriPredicate allSame = (c1, c2, c3) -> c1 == c2 && c2 == c3;
     * CharTriPredicate allDigits = (c1, c2, c3) ->
     *     Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3);
     * CharTriPredicate combined = allSame.or(allDigits);
     * boolean result = combined.test('1', '2', '3'); // Returns true (all digits)
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate
     *         and the {@code other} predicate
     */
    default CharTriPredicate or(final CharTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
