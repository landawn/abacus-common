/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.function.Predicate;

/**
 *
 * @author Haiyang Li
 * @param <E>
 * @since 0.8
 */
@com.landawn.abacus.annotation.Immutable
public class ImmutableCollection<E> extends AbstractCollection<E> implements Immutable {

    final Collection<E> coll;

    protected ImmutableCollection(Collection<? extends E> c) {
        this.coll = (Collection<E>) c;
    }

    /**
     *
     * @param <E>
     * @param c
     * @return an {@code ImmutableCollection} backed by the specified {@code Collection}
     */
    public static <E> ImmutableCollection<E> wrap(final Collection<? extends E> c) {
        if (c == null) {
            return ImmutableList.empty();
        } else if (c instanceof ImmutableCollection) {
            return (ImmutableCollection<E>) c;
        }

        return new ImmutableCollection<>(c);
    }

    /**
     *
     *
     * @param e
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean add(E e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds the all.
     *
     * @param newElements
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean addAll(Collection<? extends E> newElements) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param object
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean remove(Object object) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the if.
     *
     * @param filter
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public boolean removeIf(Predicate<? super E> filter) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the all.
     *
     * @param oldElements
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean removeAll(Collection<?> oldElements) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param elementsToKeep
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final boolean retainAll(Collection<?> elementsToKeep) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public final void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    @Override
    public boolean contains(final Object valueToFind) {
        return coll.contains(valueToFind);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public ObjIterator<E> iterator() {
        return ObjIterator.of(coll.iterator());
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int size() {
        return coll.size();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Object[] toArray() {
        return coll.toArray();
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return coll.toArray(a);
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Collection && coll.equals(obj);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return coll.hashCode();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return coll.toString();
    }
}
