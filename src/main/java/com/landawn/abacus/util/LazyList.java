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
import java.util.List;
import java.util.ListIterator;

import com.landawn.abacus.util.function.Supplier;

public final class LazyList<T> extends LazyCollection<T> implements List<T> {
    private List<T> list;

    LazyList(final Supplier<? extends List<T>> supplier) {
        super(supplier);
    }

    @Override
    protected void init() {
        if (isInitialized == false) {
            super.init();
            list = (List<T>) coll;
        }
    }

    public static <T> LazyList<T> of(final ListSupplier<T> supplier) {
        return new LazyList<T>(supplier);
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
    public T get(int index) {
        if (isInitialized == false) {
            init();
        }

        return list.get(index);
    }

    @Override
    public T set(int index, T element) {
        if (isInitialized == false) {
            init();
        }

        return list.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        if (isInitialized == false) {
            init();
        }

        list.add(index, element);

    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        if (isInitialized == false) {
            init();
        }

        return list.addAll(index, c);
    }

    @Override
    public T remove(int index) {
        if (isInitialized == false) {
            init();
        }

        return list.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        if (isInitialized == false) {
            init();
        }

        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        if (isInitialized == false) {
            init();
        }

        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        if (isInitialized == false) {
            init();
        }

        return list.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        if (isInitialized == false) {
            init();
        }

        return list.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        if (isInitialized == false) {
            init();
        }

        return list.subList(fromIndex, toIndex);
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return list.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return list.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return list.toString();
    }

    public static interface ListSupplier<T> extends Supplier<List<T>> {

        @Override
        List<T> get();

        public static <T> ListSupplier<T> of(ListSupplier<T> supplier) {
            return supplier;
        }
    }
}
