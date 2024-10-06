/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 *
 *
 * @author Haiyang Li
 */
public interface CharPredicate extends Throwables.CharPredicate<RuntimeException> { //NOSONAR

    CharPredicate ALWAYS_TRUE = value -> true;

    CharPredicate ALWAYS_FALSE = value -> false;

    CharPredicate IS_ZERO = value -> value == 0;

    CharPredicate NOT_ZERO = value -> value != 0;

    /**
     *
     *
     * @param value
     * @return
     */
    @Override
    boolean test(char value);

    /**
     * Returns the specified instance.
     *
     * @param predicate
     * @return
     */
    static CharPredicate of(final CharPredicate predicate) {
        return predicate;
    }

    /**
     *
     *
     * @return
     */
    default CharPredicate negate() {
        return t -> !test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default CharPredicate and(final CharPredicate other) {
        return t -> test(t) && other.test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default CharPredicate or(final CharPredicate other) {
        return t -> test(t) || other.test(t);
    }

    /**
     *
     *
     * @param targetChar
     * @return
     */
    static CharPredicate equal(final char targetChar) { //NOSONAR
        return value -> value == targetChar;
    }

    /**
     *
     *
     * @param targetChar
     * @return
     */
    static CharPredicate notEqual(final char targetChar) {
        return value -> value != targetChar;
    }

    /**
     *
     *
     * @param targetChar
     * @return
     */
    static CharPredicate greaterThan(final char targetChar) {
        return value -> value > targetChar;
    }

    /**
     *
     *
     * @param targetChar
     * @return
     */
    static CharPredicate greaterEqual(final char targetChar) {
        return value -> value >= targetChar;
    }

    /**
     *
     *
     * @param targetChar
     * @return
     */
    static CharPredicate lessThan(final char targetChar) {
        return value -> value < targetChar;
    }

    /**
     *
     *
     * @param targetChar
     * @return
     */
    static CharPredicate lessEqual(final char targetChar) {
        return value -> value <= targetChar;
    }

    /**
     *
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    static CharPredicate between(final char minValue, final char maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
