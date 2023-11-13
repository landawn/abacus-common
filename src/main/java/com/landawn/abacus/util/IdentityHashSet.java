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
public final class IdentityHashSet<T> extends AbstractSet<T> {

    private static final Object VAL = Boolean.TRUE;

    private final IdentityHashMap<T, Object> map;

    /**
     *
     */
    public IdentityHashSet() {
        map = new IdentityHashMap<>();
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public IdentityHashSet(int initialCapacity) {
        map = N.newIdentityHashMap(initialCapacity);
    }

    /**
     *
     *
     * @param c
     */
    public IdentityHashSet(Collection<? extends T> c) {
        map = N.newIdentityHashMap(N.size(c));

        addAll(c); // NOSONAR
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public boolean add(T e) {
        return map.put(e, VAL) == null;
    }

    /**
     *
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
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

        return map.keySet().containsAll(c);
    }

    /**
     * Adds the all.
     *
     * @param c
     * @return
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean modified = false;

        if (N.notEmpty(c)) {
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
     * @return
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;

        if (N.notEmpty(c)) {
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
     * @return
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        if (N.isEmpty(c)) {
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
     * @return
     */
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    /**
     *
     *
     * @return
     */
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

    /**
     *
     *
     * @param o
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof IdentityHashSet) {
            return ((IdentityHashSet) o).map.equals(this.map);
        }

        return false;
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
     * @return
     */
    @Override
    public String toString() {
        return map.keySet().toString();
    }
}
