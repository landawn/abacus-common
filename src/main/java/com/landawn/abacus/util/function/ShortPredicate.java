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
public interface ShortPredicate extends Try.ShortPredicate<RuntimeException> {

    static final ShortPredicate ALWAYS_TRUE = new ShortPredicate() {
        @Override
        public boolean test(short value) {
            return true;
        }
    };

    static final ShortPredicate ALWAYS_FALSE = new ShortPredicate() {
        @Override
        public boolean test(short value) {
            return false;
        }
    };

    static final ShortPredicate IS_ZERO = new ShortPredicate() {
        @Override
        public boolean test(short value) {
            return value == 0;
        }
    };

    static final ShortPredicate NOT_ZERO = new ShortPredicate() {
        @Override
        public boolean test(short value) {
            return value != 0;
        }
    };

    static final ShortPredicate IS_POSITIVE = new ShortPredicate() {
        @Override
        public boolean test(short value) {
            return value > 0;
        }
    };

    static final ShortPredicate NOT_POSITIVE = new ShortPredicate() {
        @Override
        public boolean test(short value) {
            return value <= 0;
        }
    };

    static final ShortPredicate IS_NEGATIVE = new ShortPredicate() {
        @Override
        public boolean test(short value) {
            return value < 0;
        }
    };

    static final ShortPredicate NOT_NEGATIVE = new ShortPredicate() {
        @Override
        public boolean test(short value) {
            return value >= 0;
        }
    };

    @Override
    boolean test(short value);

    /**
     * Returns the specified instance
     * 
     * @param predicate
     * @return
     */
    static ShortPredicate of(final ShortPredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    default ShortPredicate negate() {
        return (t) -> !test(t);
    }

    default ShortPredicate and(ShortPredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) && other.test(t);
    }

    default ShortPredicate or(ShortPredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) || other.test(t);
    }

    static ShortPredicate equal(short targetShort) {
        return value -> value == targetShort;
    }

    static ShortPredicate notEqual(short targetShort) {
        return value -> value != targetShort;
    }

    static ShortPredicate greaterThan(short targetShort) {
        return value -> value > targetShort;
    }

    static ShortPredicate greaterEqual(short targetShort) {
        return value -> value >= targetShort;
    }

    static ShortPredicate lessThan(short targetShort) {
        return value -> value < targetShort;
    }

    static ShortPredicate lessEqual(short targetShort) {
        return value -> value <= targetShort;
    }

    static ShortPredicate between(short minValue, short maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
