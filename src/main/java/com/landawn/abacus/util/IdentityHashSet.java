/*
 * Copyright (C) 2019 HaiYang Li
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

package com.landawn.abacus.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 *
 * @param <T>
 */
public class IdentityHashSet<T> extends AbstractSet<T> {

    private static final Object VAL = new Object();

    private final IdentityHashMap<T, Object> map;

    public IdentityHashSet() {
        map = new IdentityHashMap<>();
    }

    public IdentityHashSet(int initialCapacity) {
        map = new IdentityHashMap<>(initialCapacity);
    }

    public IdentityHashSet(Collection<? extends T> c) {
        map = new IdentityHashMap<>(N.size(c));

        addAll(c);
    }

    /**
     *
     * @param e
     * @return true, if successful
     */
    @Override
    public boolean add(T e) {
        return map.put(e, VAL) == null;
    }

    /**
     *
     * @param o
     * @return true, if successful
     */
    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    /**
     *
     * @param c
     * @return true, if successful
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        if (N.isNullOrEmpty(c)) {
            return true;
        }

        return map.keySet().containsAll(c);
    }

    /**
     * Adds the all.
     *
     * @param c
     * @return true, if successful
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean modified = false;

        if (N.notNullOrEmpty(c)) {
            for (T e : c) {
                if (add(e)) {
                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     * Removes the all.
     *
     * @param c
     * @return true, if successful
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;

        if (N.notNullOrEmpty(c)) {
            for (Object e : c) {
                if (remove(e)) {
                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     *
     * @param c
     * @return true, if successful
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        if (N.isNullOrEmpty(c)) {
            if (map.size() > 0) {
                map.clear();
                return true;
            }
        } else {
            final IdentityHashSet<T> kept = new IdentityHashSet<>(N.min(c.size(), size()));

            for (Object e : c) {
                if (this.contains(e)) {
                    kept.add((T) e);
                }
            }

            if (kept.size() < this.size()) {
                clear();
                addAll(kept);
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param o
     * @return true, if successful
     */
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    /**
     *
     * @param <A>
     * @param a
     * @return
     */
    @Override
    public <A> A[] toArray(A[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public int size() {
        return map.size();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        map.clear();
    }
}
