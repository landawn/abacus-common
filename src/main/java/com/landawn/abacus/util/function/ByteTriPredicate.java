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
public interface ByteTriPredicate extends Try.ByteTriPredicate<RuntimeException> {

    public static final ByteTriPredicate ALWAYS_TRUE = new ByteTriPredicate() {
        @Override
        public boolean test(byte a, byte b, byte c) {
            return true;
        }
    };

    public static final ByteTriPredicate ALWAYS_FALSE = new ByteTriPredicate() {
        @Override
        public boolean test(byte a, byte b, byte c) {
            return false;
        }
    };

    @Override
    boolean test(byte a, byte b, byte c);

    default ByteTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    default ByteTriPredicate and(ByteTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    default ByteTriPredicate or(ByteTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
