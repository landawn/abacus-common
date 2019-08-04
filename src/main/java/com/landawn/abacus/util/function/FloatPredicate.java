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
public interface FloatPredicate extends Try.FloatPredicate<RuntimeException> {

    static final FloatPredicate ALWAYS_TRUE = new FloatPredicate() {
        @Override
        public boolean test(float value) {
            return true;
        }
    };

    static final FloatPredicate ALWAYS_FALSE = new FloatPredicate() {
        @Override
        public boolean test(float value) {
            return false;
        }
    };

    static final FloatPredicate IS_ZERO = new FloatPredicate() {
        @Override
        public boolean test(float value) {
            return value == 0;
        }
    };

    static final FloatPredicate NOT_ZERO = new FloatPredicate() {
        @Override
        public boolean test(float value) {
            return value != 0;
        }
    };

    static final FloatPredicate IS_POSITIVE = new FloatPredicate() {
        @Override
        public boolean test(float value) {
            return value > 0;
        }
    };

    static final FloatPredicate NOT_POSITIVE = new FloatPredicate() {
        @Override
        public boolean test(float value) {
            return value <= 0;
        }
    };

    static final FloatPredicate IS_NEGATIVE = new FloatPredicate() {
        @Override
        public boolean test(float value) {
            return value < 0;
        }
    };

    static final FloatPredicate NOT_NEGATIVE = new FloatPredicate() {
        @Override
        public boolean test(float value) {
            return value >= 0;
        }
    };

    @Override
    boolean test(float value);

    /**
     * Returns the specified instance
     * 
     * @param predicate
     * @return
     */
    static FloatPredicate of(final FloatPredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    default FloatPredicate negate() {
        return (t) -> !test(t);
    }

    default FloatPredicate and(FloatPredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) && other.test(t);
    }

    default FloatPredicate or(FloatPredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) || other.test(t);
    }

    static FloatPredicate equal(float targetFloat) {
        return value -> value == targetFloat;
    }

    static FloatPredicate notEqual(float targetFloat) {
        return value -> value != targetFloat;
    }

    static FloatPredicate greaterThan(float targetFloat) {
        return value -> N.compare(value, targetFloat) > 0;
    }

    static FloatPredicate greaterEqual(float targetFloat) {
        return value -> N.compare(value, targetFloat) >= 0;
    }

    static FloatPredicate lessThan(float targetFloat) {
        return value -> N.compare(value, targetFloat) < 0;
    }

    static FloatPredicate lessEqual(float targetFloat) {
        return value -> N.compare(value, targetFloat) <= 0;
    }

    static FloatPredicate between(float minValue, float maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
