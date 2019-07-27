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
public interface FloatTriPredicate extends Try.FloatTriPredicate<RuntimeException> {

    public static final FloatTriPredicate ALWAYS_TRUE = new FloatTriPredicate() {
        @Override
        public boolean test(float a, float b, float c) {
            return true;
        }
    };

    public static final FloatTriPredicate ALWAYS_FALSE = new FloatTriPredicate() {
        @Override
        public boolean test(float a, float b, float c) {
            return false;
        }
    };

    @Override
    boolean test(float a, float b, float c);

    default FloatTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    default FloatTriPredicate and(FloatTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    default FloatTriPredicate or(FloatTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
