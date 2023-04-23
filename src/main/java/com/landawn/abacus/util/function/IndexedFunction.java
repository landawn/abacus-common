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
public interface IndexedFunction<T, R> extends Throwables.IndexedFunction<T, R, RuntimeException> { //NOSONAR

    /**
    * 
    *
    * @param idx 
    * @param e 
    * @return 
    */
    @Override
    R apply(int idx, T e);

    /**
     * 
     *
     * @param <V> 
     * @param before 
     * @return 
     */
    default <V> IndexedFunction<V, R> compose(IndexedFunction<? super V, ? extends T> before) {
        N.checkArgNotNull(before);

        return (idx, v) -> apply(idx, before.apply(idx, v));
    }

    /**
     * 
     *
     * @param <V> 
     * @param after 
     * @return 
     */
    default <V> IndexedFunction<T, V> andThen(IndexedFunction<? super R, ? extends V> after) {
        N.checkArgNotNull(after);

        return (idx, t) -> after.apply(idx, apply(idx, t));
    }
}
