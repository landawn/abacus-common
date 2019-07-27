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
public interface CharBiPredicate extends Try.CharBiPredicate<RuntimeException> {

    static final CharBiPredicate ALWAYS_TRUE = new CharBiPredicate() {
        @Override
        public boolean test(char t, char u) {
            return true;
        }
    };

    static final CharBiPredicate ALWAYS_FALSE = new CharBiPredicate() {
        @Override
        public boolean test(char t, char u) {
            return false;
        }
    };

    static final CharBiPredicate EQUAL = new CharBiPredicate() {
        @Override
        public boolean test(char t, char u) {
            return t == u;
        }
    };

    static final CharBiPredicate NOT_EQUAL = new CharBiPredicate() {
        @Override
        public boolean test(char t, char u) {
            return t != u;
        }
    };

    static final CharBiPredicate GREATER_THAN = new CharBiPredicate() {
        @Override
        public boolean test(char t, char u) {
            return t > u;
        }
    };

    static final CharBiPredicate GREATER_EQUAL = new CharBiPredicate() {
        @Override
        public boolean test(char t, char u) {
            return t >= u;
        }
    };

    static final CharBiPredicate LESS_THAN = new CharBiPredicate() {
        @Override
        public boolean test(char t, char u) {
            return t < u;
        }
    };

    static final CharBiPredicate LESS_EQUAL = new CharBiPredicate() {
        @Override
        public boolean test(char t, char u) {
            return t <= u;
        }
    };

    @Override
    boolean test(char t, char u);

    default CharBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    default CharBiPredicate and(CharBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) && other.test(t, u);
    }

    default CharBiPredicate or(CharBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
