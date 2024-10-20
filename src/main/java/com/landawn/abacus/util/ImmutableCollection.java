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

import com.landawn.abacus.annotation.Beta;

/**
 * ImmutableCollection is a class that extends AbstractCollection and implements the Immutable interface.
 * This class represents a collection that cannot be modified once created.
 * Any attempt to modify the collection will result in an UnsupportedOperationException.
 *
 * @param <E> the type of elements in this collection
 */
@com.landawn.abacus.annotation.Immutable
public class ImmutableCollection<E> extends AbstractCollection<E> implements Immutable {

    final Collection<E> coll;

    protected ImmutableCollection(final Collection<? extends E> c) {
        coll = (Collection<E>) c;
    }

    /**
     * Wraps the given collection into an ImmutableCollection. If the given collection is {@code null}, an empty ImmutableList is returned.
     * If the given collection is already an instance of ImmutableCollection, it is directly returned.
     * Otherwise, returns a new ImmutableCollection backed by the provided Collection. Changes to the specified Collection will be reflected in the ImmutableCollection.
     *
     * @param <E> the type of elements in the collection
     * @param c the collection to be wrapped into an ImmutableCollection
     * @return an ImmutableCollection that contains the elements of the given collection
     */
    @Beta
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
    public final boolean add(final E e) throws UnsupportedOperationException {
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
    public final boolean addAll(final Collection<? extends E> newElements) throws UnsupportedOperationException {
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
    public final boolean remove(final Object object) throws UnsupportedOperationException {
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
    public boolean removeIf(final Predicate<? super E> filter) throws UnsupportedOperationException {
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
    public final boolean removeAll(final Collection<?> oldElements) throws UnsupportedOperationException {
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
    public final boolean retainAll(final Collection<?> elementsToKeep) throws UnsupportedOperationException {
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
    public <T> T[] toArray(final T[] a) {
        return coll.toArray(a);
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
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
