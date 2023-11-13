/*
 * Copyright (c) 2015, Haiyang Li.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * It's designed to supported primitive/object array.
 * The elements in the array must not be modified after the array is added into the set.
 *
 * @author Haiyang Li
 * @param <E>
 * @since 0.8
 */
public sealed class ArrayHashSet<E> implements Set<E> permits LinkedArrayHashSet {

    private final Set<Wrapper<E>> set;

    /**
     *
     */
    public ArrayHashSet() {
        this.set = N.newHashSet();
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public ArrayHashSet(final int initialCapacity) {
        this.set = N.newHashSet(initialCapacity);
    }

    /**
     *
     *
     * @param setType
     */
    @SuppressWarnings("rawtypes")
    public ArrayHashSet(final Class<? extends Set> setType) {
        this.set = (Set) N.newCollection(setType);
    }

    /**
     *
     *
     * @param coll
     */
    public ArrayHashSet(final Collection<? extends E> coll) {
        if (N.isEmpty(coll)) {
            set = N.newHashSet();
        } else {
            set = N.newHashSet(coll.size());
        }

        addAll(coll); // NOSONAR
    }

    ArrayHashSet(Set<Wrapper<E>> set) {
        this.set = set;
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public boolean add(E e) {
        return set.add(Wrapper.of(e));
    }

    /**
     * Adds the all.
     *
     * @param c
     * @return
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        boolean result = false;

        for (E e : c) {
            result |= add(e);
        }

        return result;
    }

    /**
     *
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        return set.remove(Wrapper.of(o));
    }

    /**
     * Removes the all.
     *
     * @param c
     * @return
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        boolean result = false;

        for (Object e : c) {
            result |= remove(e);
        }

        return result;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        if (N.isEmpty(c)) {
            if (set.isEmpty()) {
                return false;
            } else {
                set.clear();

                return true;
            }
        }

        List<Wrapper<?>> list = new ArrayList<>(c.size());

        for (Object e : c) {
            list.add(Wrapper.of(e));
        }

        return set.retainAll(list);
    }

    /**
     *
     * @param o
     * @return
     */
    @Override
    public boolean contains(Object o) {
        return set.contains(Wrapper.of(o));
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        if (N.isEmpty(c)) {
            return true;
        }

        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Iterator<E> iterator() {
        return new Itr<>(set.iterator());
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Object[] toArray() {
        final int size = size();

        if (size == 0) {
            return N.EMPTY_OBJECT_ARRAY;
        }

        final Object[] result = new Object[size];
        int i = 0;

        for (Wrapper<E> e : set) {
            result[i++] = e.value();
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @Override
    public <T> T[] toArray(T[] a) {
        final int size = size();

        if (a.length < size) {
            a = N.newArray(a.getClass().getComponentType(), size);
        }

        final Object[] result = a;
        int i = 0;

        for (Wrapper<E> e : set) {
            result[i++] = e.value();
        }

        return a;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int size() {
        return set.size();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        set.clear();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return set.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ArrayHashSet && ((ArrayHashSet<E>) obj).set.equals(set));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return set.toString();
    }

    /**
     * The Class Itr.
     *
     * @param <T>
     */
    static class Itr<T> implements Iterator<T> {

        /** The it. */
        private final Iterator<Wrapper<T>> it;

        /**
         * Instantiates a new itr.
         *
         * @param it
         */
        Itr(Iterator<Wrapper<T>> it) {
            this.it = it;
        }

        /**
         * Checks for next.
         *
         * @return
         */
        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        /**
         *
         * @return
         */
        @Override
        public T next() {
            return it.next().value();
        }

        /**
         * Removes the.
         */
        @Override
        public void remove() {
            it.remove();
        }
    }
}
