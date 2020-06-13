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

import java.util.Comparator;
import java.util.SortedSet;

import com.landawn.abacus.util.function.Supplier;

public class LazySortedSet<T> extends LazySet<T> implements SortedSet<T> {
    private SortedSet<T> sortedSet;

    LazySortedSet(final Supplier<? extends SortedSet<T>> supplier) {
        super(supplier);
    }

    @Override
    protected void init() {
        if (isInitialized == false) {
            super.init();
            sortedSet = (SortedSet<T>) coll;
        }
    }

    public static <T> LazySortedSet<T> of(final SortedSetSupplier<T> supplier) {
        return new LazySortedSet<T>(supplier);
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
    public static <T> LazySet<T> of(final SetSupplier<T> supplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Comparator<? super T> comparator() {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.comparator();
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.headSet(toElement);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.tailSet(fromElement);
    }

    @Override
    public T first() {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.first();
    }

    @Override
    public T last() {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.last();
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return sortedSet.toString();
    }

    public static interface SortedSetSupplier<T> extends Supplier<SortedSet<T>> {

        @Override
        SortedSet<T> get();

        public static <T> SortedSetSupplier<T> of(SortedSetSupplier<T> supplier) {
            return supplier;
        }
    }
}
