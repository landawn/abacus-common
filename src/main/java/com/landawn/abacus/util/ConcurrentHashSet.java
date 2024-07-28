/*
 * Copyright (c) 2024, Haiyang Li.
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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class ConcurrentHashSet<E> extends AbstractSet<E> {

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = N.NULL_MASK;

    private final ConcurrentHashMap<E, Object> map;

    /**
     *
     */
    public ConcurrentHashSet() {
        map = new ConcurrentHashMap<>();
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public ConcurrentHashSet(int initialCapacity) {
        map = new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     *
     *
     * @param initialCapacity
     * @param loadFactor
     */
    public ConcurrentHashSet(int initialCapacity, float loadFactor) {
        map = new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }

    /**
     *
     *
     * @param c
     */
    public ConcurrentHashSet(Collection<? extends E> c) {
        map = new ConcurrentHashMap<>(Math.max((int) (c.size() / .75f) + 1, 16));
        addAll(c);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     *
     *
     * @param e
     * @return
     */
    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    /**
     *
     *
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    /**
     *
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     *
     *
     * @param valueToFind
     * @return
     */
    @Override
    public boolean contains(final Object valueToFind) {
        return map.containsKey(valueToFind);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return map.keySet().hashCode();
    }

    /**
     *
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        return (o instanceof ConcurrentHashSet) && ((ConcurrentHashSet<E>) o).map.keySet().equals(map.keySet());
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return map.keySet().toString();
    }
}
