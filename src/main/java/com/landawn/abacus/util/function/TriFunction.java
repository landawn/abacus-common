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
import com.landawn.abacus.util.Throwables;

/**
 *
 * @since 0.8
 *
 * @author Haiyang Li
 */
public interface TriFunction<A, B, C, R> extends Throwables.TriFunction<A, B, C, R, RuntimeException> { //NOSONAR

    @Override
    R apply(A a, B b, C c);

    default <V> TriFunction<A, B, C, V> andThen(java.util.function.Function<? super R, ? extends V> after) {
        N.checkArgNotNull(after);

        return (a, b, c) -> after.apply(apply(a, b, c));
    }

    default <E extends Throwable> Throwables.TriFunction<A, B, C, R, E> toThrowable() {
        return (Throwables.TriFunction<A, B, C, R, E>) this;
    }
}
