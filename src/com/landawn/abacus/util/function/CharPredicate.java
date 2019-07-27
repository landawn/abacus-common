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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Try;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface CharPredicate extends Try.CharPredicate<RuntimeException> {

    static final CharPredicate ALWAYS_TRUE = new CharPredicate() {
        @Override
        public boolean test(char value) {
            return true;
        }
    };

    static final CharPredicate ALWAYS_FALSE = new CharPredicate() {
        @Override
        public boolean test(char value) {
            return false;
        }
    };

    static final CharPredicate IS_ZERO = new CharPredicate() {
        @Override
        public boolean test(char value) {
            return value == 0;
        }
    };

    static final CharPredicate NOT_ZERO = new CharPredicate() {
        @Override
        public boolean test(char value) {
            return value != 0;
        }
    };

    @Override
    boolean test(char value);

    /**
     * Returns the specified instance
     * 
     * @param predicate
     * @return
     */
    static CharPredicate of(final CharPredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    default CharPredicate negate() {
        return (t) -> !test(t);
    }

    default CharPredicate and(CharPredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) && other.test(t);
    }

    default CharPredicate or(CharPredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) || other.test(t);
    }

    static CharPredicate equal(char targetChar) {
        return value -> value == targetChar;
    }

    static CharPredicate notEqual(char targetChar) {
        return value -> value != targetChar;
    }

    static CharPredicate greaterThan(char targetChar) {
        return value -> value > targetChar;
    }

    static CharPredicate greaterEqual(char targetChar) {
        return value -> value >= targetChar;
    }

    static CharPredicate lessThan(char targetChar) {
        return value -> value < targetChar;
    }

    static CharPredicate lessEqual(char targetChar) {
        return value -> value <= targetChar;
    }

    static CharPredicate between(char minValue, char maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
