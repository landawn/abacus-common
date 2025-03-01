/*
 * Copyright 2017 Haiyang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

@FunctionalInterface
public interface QuadPredicate<A, B, C, D> extends Throwables.QuadPredicate<A, B, C, D, RuntimeException> { //NOSONAR

    /**
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @return
     */
    @Override
    boolean test(A a, B b, C c, D d);

    default QuadPredicate<A, B, C, D> negate() {
        return (a, b, c, d) -> !test(a, b, c, d);
    }

    /**
     *
     * @param other
     * @return
     */
    default QuadPredicate<A, B, C, D> and(final QuadPredicate<? super A, ? super B, ? super C, ? super D> other) {
        return (a, b, c, d) -> test(a, b, c, d) && other.test(a, b, c, d);
    }

    /**
     *
     * @param other
     * @return
     */
    default QuadPredicate<A, B, C, D> or(final QuadPredicate<? super A, ? super B, ? super C, ? super D> other) {
        return (a, b, c, d) -> test(a, b, c, d) || other.test(a, b, c, d);
    }
}
