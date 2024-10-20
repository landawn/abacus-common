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
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 *
 */
public interface ObjIntConsumer<T> extends Throwables.ObjIntConsumer<T, RuntimeException>, java.util.function.ObjIntConsumer<T> { // NOSONAR
    /**
     *
     *
     * @param t
     * @param value
     */
    @Override
    void accept(T t, int value);

    /**
     *
     *
     * @param after
     * @return
     */
    default ObjIntConsumer<T> andThen(final ObjIntConsumer<? super T> after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}
