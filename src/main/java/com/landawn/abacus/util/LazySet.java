/*
 * Copyright (c) 2020, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Set;

import com.landawn.abacus.util.function.Supplier;

public class LazySet<T> extends LazyCollection<T> implements Set<T> {
    private Set<T> set;

    LazySet(final Supplier<? extends Set<T>> supplier) {
        super(supplier);
    }

    @Override
    protected void init() {
        if (isInitialized == false) {
            super.init();
            set = (Set<T>) coll;
        }
    }

    public static <T> LazySet<T> of(final SetSupplier<T> supplier) {
        return new LazySet<T>(supplier);
    }

    /**
     * 
     * @param <T>
     * @param supplier
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <T> LazyCollection<T> of(final Supplier<? extends Collection<T>> supplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return set.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return set.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return set.toString();
    }

    public static interface SetSupplier<T> extends Supplier<Set<T>> {

        @Override
        Set<T> get();

        public static <T> SetSupplier<T> of(SetSupplier<T> supplier) {
            return supplier;
        }
    }
}
