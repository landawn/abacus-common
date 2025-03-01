/*
 * Copyright (c) 2019, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.util.function.Supplier;

final class LazyInitializer<T> implements com.landawn.abacus.util.function.Supplier<T> {
    private final Supplier<T> supplier;
    private volatile boolean initialized = false;
    private volatile T value = null; //NOSONAR

    LazyInitializer(final Supplier<T> supplier) {
        N.checkArgNotNull(supplier, cs.supplier);

        this.supplier = supplier;
    }

    /**
     *
     * @param <T>
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> LazyInitializer<T> of(final Supplier<T> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        if (supplier instanceof LazyInitializer) {
            return (LazyInitializer<T>) supplier;
        }

        return new LazyInitializer<>(supplier);
    }

    @Override
    public T get() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    value = supplier.get();

                    initialized = true;
                }

            }
        }

        return value;
    }
}
