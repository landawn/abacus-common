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
public interface IndexedBiConsumer<T, U> extends Throwables.IndexedBiConsumer<T, U, RuntimeException> {

    @Override
    void accept(int idx, T e, U u);

    default IndexedBiConsumer<T, U> andThen(IndexedBiConsumer<? super T, ? super U> after) {
        N.checkArgNotNull(after);

        return (idx, e, u) -> {
            accept(idx, e, u);
            after.accept(idx, e, u);
        };
    }
}
