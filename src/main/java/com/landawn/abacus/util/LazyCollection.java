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
import java.util.Iterator;
import java.util.Objects;

import com.landawn.abacus.util.function.Supplier;

public class LazyCollection<T> implements Collection<T> {
    protected final Supplier<? extends Collection<T>> supplier;
    protected Collection<T> coll;
    protected boolean isInitialized = false;

    protected LazyCollection(final Supplier<? extends Collection<T>> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        this.supplier = supplier;
    }

    protected void init() {
        if (isInitialized == false) {
            isInitialized = true;
            coll = Objects.requireNonNull(supplier.get());
        }
    }

    public static <T> LazyCollection<T> of(final Supplier<? extends Collection<T>> supplier) {
        return new LazyCollection<T>(supplier);
    }

    @Override
    public int size() {
        if (isInitialized == false) {
            init();
        }

        return coll.size();
    }

    @Override
    public boolean isEmpty() {
        if (isInitialized == false) {
            init();
        }

        return coll.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (isInitialized == false) {
            init();
        }

        return coll.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        if (isInitialized == false) {
            init();
        }

        return coll.iterator();
    }

    @Override
    public Object[] toArray() {
        if (isInitialized == false) {
            init();
        }

        return coll.toArray();
    }

    @Override
    public <A> A[] toArray(A[] a) {
        if (isInitialized == false) {
            init();
        }

        return coll.toArray(a);
    }

    @Override
    public boolean add(T e) {
        if (isInitialized == false) {
            init();
        }

        return coll.add(e);
    }

    @Override
    public boolean remove(Object o) {
        if (isInitialized == false) {
            init();
        }

        return coll.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (isInitialized == false) {
            init();
        }

        return coll.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (isInitialized == false) {
            init();
        }

        return coll.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (isInitialized == false) {
            init();
        }

        return coll.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (isInitialized == false) {
            init();
        }

        return coll.retainAll(c);
    }

    @Override
    public void clear() {
        if (isInitialized == false) {
            init();
        }

        coll.clear();
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return coll.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return obj instanceof LazyCollection && coll.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return coll.toString();
    }
}
