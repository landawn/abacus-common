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
public interface FloatBiPredicate extends Try.FloatBiPredicate<RuntimeException> {

    static final FloatBiPredicate ALWAYS_TRUE = new FloatBiPredicate() {
        @Override
        public boolean test(float t, float u) {
            return true;
        }
    };

    static final FloatBiPredicate ALWAYS_FALSE = new FloatBiPredicate() {
        @Override
        public boolean test(float t, float u) {
            return false;
        }
    };

    static final FloatBiPredicate EQUAL = new FloatBiPredicate() {
        @Override
        public boolean test(float t, float u) {
            return Float.compare(t, u) == 0;
        }
    };

    static final FloatBiPredicate NOT_EQUAL = new FloatBiPredicate() {
        @Override
        public boolean test(float t, float u) {
            return Float.compare(t, u) != 0;
        }
    };

    static final FloatBiPredicate GREATER_THAN = new FloatBiPredicate() {
        @Override
        public boolean test(float t, float u) {
            return Float.compare(t, u) > 0;
        }
    };

    static final FloatBiPredicate GREATER_EQUAL = new FloatBiPredicate() {
        @Override
        public boolean test(float t, float u) {
            return Float.compare(t, u) >= 0;
        }
    };

    static final FloatBiPredicate LESS_THAN = new FloatBiPredicate() {
        @Override
        public boolean test(float t, float u) {
            return Float.compare(t, u) < 0;
        }
    };

    static final FloatBiPredicate LESS_EQUAL = new FloatBiPredicate() {
        @Override
        public boolean test(float t, float u) {
            return Float.compare(t, u) <= 0;
        }
    };

    @Override
    boolean test(float t, float u);

    default FloatBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    default FloatBiPredicate and(FloatBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) && other.test(t, u);
    }

    default FloatBiPredicate or(FloatBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
