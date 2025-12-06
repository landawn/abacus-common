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
 * Represents a predicate (boolean-valued function) of two char-valued arguments.
 * This is the two-arity specialization of {@link CharPredicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(char, char)}.
 *
 * @see java.util.function.BiPredicate
 * @see CharPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharBiPredicate extends Throwables.CharBiPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true}, regardless of the two char input values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharBiPredicate.ALWAYS_TRUE.test('a', 'b');    // Always returns true
     * boolean result2 = CharBiPredicate.ALWAYS_TRUE.test('x', 'x');   // Always returns true
     * }</pre>
     */
    CharBiPredicate ALWAYS_TRUE = (t, u) -> true;
    /**
     * A predicate that always evaluates to {@code false}, regardless of the two char input values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharBiPredicate.ALWAYS_FALSE.test('a', 'b');    // Always returns false
     * boolean result2 = CharBiPredicate.ALWAYS_FALSE.test('x', 'x');   // Always returns false
     * }</pre>
     */
    CharBiPredicate ALWAYS_FALSE = (t, u) -> false;
    /**
     * A predicate that tests if the two char values are equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharBiPredicate.EQUAL.test('a', 'a');    // Returns true
     * boolean result2 = CharBiPredicate.EQUAL.test('a', 'b');   // Returns false
     * }</pre>
     */
    CharBiPredicate EQUAL = (t, u) -> t == u;
    /**
     * A predicate that tests if the two char values are not equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharBiPredicate.NOT_EQUAL.test('a', 'b');    // Returns true
     * boolean result2 = CharBiPredicate.NOT_EQUAL.test('a', 'a');   // Returns false
     * }</pre>
     */
    CharBiPredicate NOT_EQUAL = (t, u) -> t != u;
    /**
     * A predicate that tests if the first char value is greater than the second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharBiPredicate.GREATER_THAN.test('b', 'a');    // Returns true
     * boolean result2 = CharBiPredicate.GREATER_THAN.test('a', 'b');   // Returns false
     * }</pre>
     */
    CharBiPredicate GREATER_THAN = (t, u) -> t > u;
    /**
     * A predicate that tests if the first char value is greater than or equal to the second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharBiPredicate.GREATER_EQUAL.test('b', 'a');    // Returns true
     * boolean result2 = CharBiPredicate.GREATER_EQUAL.test('a', 'a');   // Returns true
     * boolean result3 = CharBiPredicate.GREATER_EQUAL.test('a', 'b');   // Returns false
     * }</pre>
     */
    CharBiPredicate GREATER_EQUAL = (t, u) -> t >= u;
    /**
     * A predicate that tests if the first char value is less than the second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharBiPredicate.LESS_THAN.test('a', 'b');    // Returns true
     * boolean result2 = CharBiPredicate.LESS_THAN.test('b', 'a');   // Returns false
     * }</pre>
     */
    CharBiPredicate LESS_THAN = (t, u) -> t < u;
    /**
     * A predicate that tests if the first char value is less than or equal to the second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharBiPredicate.LESS_EQUAL.test('a', 'b');    // Returns true
     * boolean result2 = CharBiPredicate.LESS_EQUAL.test('a', 'a');   // Returns true
     * boolean result3 = CharBiPredicate.LESS_EQUAL.test('b', 'a');   // Returns false
     * }</pre>
     */
    CharBiPredicate LESS_EQUAL = (t, u) -> t <= u;

    /**
     * Evaluates this predicate on the given char arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharBiPredicate areEqual = (c1, c2) -> c1 == c2;
     * boolean result = areEqual.test('a', 'a');   // Returns true
     *
     * CharBiPredicate areBothLetters = (c1, c2) -> Character.isLetter(c1) && Character.isLetter(c2);
     * boolean result2 = areBothLetters.test('a', 'b');   // Returns true
     * }</pre>
     *
     * @param t the first char input argument
     * @param u the second char input argument
     * @return {@code true} if the two input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(char t, char u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * The returned predicate will return {@code true} when this predicate returns {@code false},
     * and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharBiPredicate areEqual = (c1, c2) -> c1 == c2;
     * CharBiPredicate areNotEqual = areEqual.negate();
     * boolean result = areNotEqual.test('a', 'b');   // Returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default CharBiPredicate negate() {
        return (t, u) -> !test(t, u);
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
     * CharBiPredicate bothLetters = (c1, c2) -> Character.isLetter(c1) && Character.isLetter(c2);
     * CharBiPredicate bothUpperCase = (c1, c2) -> Character.isUpperCase(c1) && Character.isUpperCase(c2);
     * CharBiPredicate combined = bothLetters.and(bothUpperCase);
     * boolean result = combined.test('A', 'B');   // Returns true
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate
     *         and the {@code other} predicate
     */
    default CharBiPredicate and(final CharBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
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
     * CharBiPredicate areEqual = (c1, c2) -> c1 == c2;
     * CharBiPredicate bothDigits = (c1, c2) -> Character.isDigit(c1) && Character.isDigit(c2);
     * CharBiPredicate combined = areEqual.or(bothDigits);
     * boolean result = combined.test('1', '2');   // Returns true (both are digits)
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate
     *         and the {@code other} predicate
     */
    default CharBiPredicate or(final CharBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
