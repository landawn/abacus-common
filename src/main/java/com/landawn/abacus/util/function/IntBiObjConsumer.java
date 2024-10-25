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

public interface IntBiObjConsumer<T, U> extends Throwables.IntBiObjConsumer<T, U, RuntimeException> { //NOSONAR

    /**
     *
     *
     * @param i
     * @param t
     * @param u
     */
    @Override
    void accept(int i, T t, U u);

    /**
     *
     *
     * @param after
     * @return
     */
    default IntBiObjConsumer<T, U> andThen(final IntBiObjConsumer<? super T, ? super U> after) {
        return (i, t, u) -> {
            accept(i, t, u);
            after.accept(i, t, u);
        };
    }
}
