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
public interface QuadConsumer<A, B, C, D> extends Throwables.QuadConsumer<A, B, C, D, RuntimeException> { //NOSONAR

    /**
     *
     * @param a
     * @param b
     * @param c
     * @param d
     */
    @Override
    void accept(A a, B b, C c, D d);

    /**
     *
     * @param after
     * @return
     */
    default QuadConsumer<A, B, C, D> andThen(final QuadConsumer<? super A, ? super B, ? super C, ? super D> after) {
        return (a, b, c, d) -> {
            accept(a, b, c, d);
            after.accept(a, b, c, d);
        };
    }
}
