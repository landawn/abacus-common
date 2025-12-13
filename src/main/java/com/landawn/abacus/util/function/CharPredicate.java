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
 * Represents a predicate (boolean-valued function) of one char-valued argument.
 * This is the char-consuming primitive type specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(char)}.
 *
 * @see java.util.function.Predicate
 * @see CharBiPredicate
 * @see CharTriPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharPredicate extends Throwables.CharPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true}, regardless of the input char value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharPredicate.ALWAYS_TRUE.test('a');    // Always returns true
     * boolean result2 = CharPredicate.ALWAYS_TRUE.test('5');   // Always returns true
     * }</pre>
     */
    CharPredicate ALWAYS_TRUE = value -> true;
    /**
     * A predicate that always evaluates to {@code false}, regardless of the input char value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharPredicate.ALWAYS_FALSE.test('a');    // Always returns false
     * boolean result2 = CharPredicate.ALWAYS_FALSE.test('5');   // Always returns false
     * }</pre>
     */
    CharPredicate ALWAYS_FALSE = value -> false;
    /**
     * A predicate that tests if the char value is equal to the {@code null} character ('\0').
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharPredicate.IS_ZERO.test('\0');   // Returns true
     * boolean result2 = CharPredicate.IS_ZERO.test('a');   // Returns false
     * }</pre>
     */
    CharPredicate IS_ZERO = value -> value == 0;
    /**
     * A predicate that tests if the char value is not equal to the {@code null} character ('\0').
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean result = CharPredicate.NOT_ZERO.test('a');     // Returns true
     * boolean result2 = CharPredicate.NOT_ZERO.test('\0');   // Returns false
     * }</pre>
     */
    CharPredicate NOT_ZERO = value -> value != 0;

    /**
     * Evaluates this predicate on the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate isDigit = Character::isDigit;
     * boolean result = isDigit.test('5');   // Returns true
     * }</pre>
     *
     * @param value the input argument
     * @return {@code true} if the input argument matches the predicate, {@code false} otherwise
     */
    @Override
    boolean test(char value);

    /**
     * Returns the specified CharPredicate instance.
     * This method is useful for type inference or when you need to explicitly cast a lambda expression.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate pred = CharPredicate.of(c -> c > 'A');
     * boolean result = pred.test('B');   // Returns true
     * }</pre>
     *
     * @param predicate the predicate to return
     * @return the same predicate instance
     */
    static CharPredicate of(final CharPredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * The returned predicate will return {@code true} when this predicate returns {@code false}, and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate isDigit = Character::isDigit;
     * CharPredicate isNotDigit = isDigit.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default CharPredicate negate() {
        return t -> !test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other} predicate is not evaluated.
     *
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate isLetter = Character::isLetter;
     * CharPredicate isUpperCase = Character::isUpperCase;
     * CharPredicate isUpperCaseLetter = isLetter.and(isUpperCase);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default CharPredicate and(final CharPredicate other) {
        return t -> test(t) && other.test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other} predicate is not evaluated.
     *
     * <p>If evaluation of either operation throws an exception, it is relayed to the caller of the composed operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate isDigit = Character::isDigit;
     * CharPredicate isLetter = Character::isLetter;
     * CharPredicate isAlphanumeric = isDigit.or(isLetter);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default CharPredicate or(final CharPredicate other) {
        return t -> test(t) || other.test(t);
    }

    /**
     * Returns a predicate that tests if the char value is equal to the specified target value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate isA = CharPredicate.equal('A');
     * boolean result = isA.test('A');   // Returns true
     * }</pre>
     *
     * @param targetChar the value to compare against
     * @return a predicate that tests if the input is equal to {@code targetChar}
     */
    static CharPredicate equal(final char targetChar) { //NOSONAR
        return value -> value == targetChar;
    }

    /**
     * Returns a predicate that tests if the char value is not equal to the specified target value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate notSpace = CharPredicate.notEqual(' ');
     * boolean result = notSpace.test('A');   // Returns true
     * }</pre>
     *
     * @param targetChar the value to compare against
     * @return a predicate that tests if the input is not equal to {@code targetChar}
     */
    static CharPredicate notEqual(final char targetChar) {
        return value -> value != targetChar;
    }

    /**
     * Returns a predicate that tests if the char value is greater than the specified target value.
     * Comparison is based on Unicode code point values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate afterM = CharPredicate.greaterThan('M');
     * boolean result = afterM.test('Z');   // Returns true
     * }</pre>
     *
     * @param targetChar the value to compare against
     * @return a predicate that tests if the input is greater than {@code targetChar}
     */
    static CharPredicate greaterThan(final char targetChar) {
        return value -> value > targetChar;
    }

    /**
     * Returns a predicate that tests if the char value is greater than or equal to the specified target value.
     * Comparison is based on Unicode code point values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate atLeastA = CharPredicate.greaterEqual('A');
     * boolean result = atLeastA.test('A');   // Returns true
     * }</pre>
     *
     * @param targetChar the value to compare against
     * @return a predicate that tests if the input is greater than or equal to {@code targetChar}
     */
    static CharPredicate greaterEqual(final char targetChar) {
        return value -> value >= targetChar;
    }

    /**
     * Returns a predicate that tests if the char value is less than the specified target value.
     * Comparison is based on Unicode code point values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate beforeZ = CharPredicate.lessThan('Z');
     * boolean result = beforeZ.test('A');   // Returns true
     * }</pre>
     *
     * @param targetChar the value to compare against
     * @return a predicate that tests if the input is less than {@code targetChar}
     */
    static CharPredicate lessThan(final char targetChar) {
        return value -> value < targetChar;
    }

    /**
     * Returns a predicate that tests if the char value is less than or equal to the specified target value.
     * Comparison is based on Unicode code point values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate maxZ = CharPredicate.lessEqual('Z');
     * boolean result = maxZ.test('Z');   // Returns true
     * }</pre>
     *
     * @param targetChar the value to compare against
     * @return a predicate that tests if the input is less than or equal to {@code targetChar}
     */
    static CharPredicate lessEqual(final char targetChar) {
        return value -> value <= targetChar;
    }

    /**
     * Returns a predicate that tests if the char value is between the specified minimum and maximum values (exclusive).
     * The test returns {@code true} if the value is greater than {@code minValue} AND less than {@code maxValue}.
     * Comparison is based on Unicode code point values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharPredicate inRange = CharPredicate.between('A', 'Z');
     * boolean result = inRange.test('M');   // Returns true
     * }</pre>
     *
     * @param minValue the exclusive lower bound
     * @param maxValue the exclusive upper bound
     * @return a predicate that tests if the input is between {@code minValue} and {@code maxValue} (exclusive)
     */
    static CharPredicate between(final char minValue, final char maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
