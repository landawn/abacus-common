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
public interface ByteTriPredicate extends Throwables.ByteTriPredicate<RuntimeException> { //NOSONAR

    ByteTriPredicate ALWAYS_TRUE = (a, b, c) -> true;

    ByteTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    @Override
    boolean test(byte a, byte b, byte c);

    /**
     *
     *
     * @return
     */
    default ByteTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default ByteTriPredicate and(final ByteTriPredicate other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default ByteTriPredicate or(final ByteTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
