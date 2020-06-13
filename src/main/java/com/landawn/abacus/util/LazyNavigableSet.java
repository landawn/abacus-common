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

import java.util.Iterator;
import java.util.NavigableSet;

import com.landawn.abacus.util.function.Supplier;

public class LazyNavigableSet<T> extends LazySortedSet<T> implements NavigableSet<T> {
    private NavigableSet<T> navigableSet;

    LazyNavigableSet(final Supplier<? extends NavigableSet<T>> supplier) {
        super(supplier);
    }

    @Override
    protected void init() {
        if (isInitialized == false) {
            super.init();
            navigableSet = (NavigableSet<T>) coll;
        }
    }

    public static <T> LazyNavigableSet<T> of(final NavigableSetSupplier<T> supplier) {
        return new LazyNavigableSet<T>(supplier);
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
    public static <T> LazySortedSet<T> of(final SortedSetSupplier<T> supplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public T lower(T e) {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.lower(e);
    }

    @Override
    public T floor(T e) {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.floor(e);
    }

    @Override
    public T ceiling(T e) {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.ceiling(e);
    }

    @Override
    public T higher(T e) {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.higher(e);
    }

    @Override
    public T pollFirst() {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.pollFirst();
    }

    @Override
    public T pollLast() {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.pollLast();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.descendingSet();
    }

    @Override
    public Iterator<T> descendingIterator() {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.descendingIterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.subSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.headSet(toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.tailSet(fromElement, inclusive);
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return navigableSet.toString();
    }

    public static interface NavigableSetSupplier<T> extends Supplier<NavigableSet<T>> {

        @Override
        NavigableSet<T> get();

        public static <T> NavigableSetSupplier<T> of(NavigableSetSupplier<T> supplier) {
            return supplier;
        }
    }
}
