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
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 *
 * @author Haiyang Li
 */
public interface BiIntObjFunction<T, R> extends Throwables.BiIntObjFunction<T, R, RuntimeException> { // NOSONAR

    /**
     *
     * @param i
     * @param j
     * @param t
     */
    //NOSONAR
    @Override
    R apply(int i, int j, T t);

    /**
     *
     *
     * @param <V>
     * @param after
     * @return
     */
    default <V> BiIntObjFunction<T, V> andThen(java.util.function.Function<? super R, ? extends V> after) {
        N.checkArgNotNull(after);

        return (i, j, t) -> after.apply(apply(i, j, t));
    }
}