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
import java.util.Iterator;
import java.util.function.Predicate;

import com.landawn.abacus.util.stream.Stream;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableCollection.
 *
 * @author Haiyang Li
 * @param <E> the element type
 * @since 0.8
 */
abstract class ImmutableCollection<E> extends AbstractCollection<E> {

    /** The coll. */
    final Collection<E> coll;

    /**
     * Instantiates a new immutable collection.
     *
     * @param c the c
     */
    protected ImmutableCollection(Collection<? extends E> c) {
        this.coll = (Collection<E>) c;
    }

    /**
     * Adds the.
     *
     * @param e the e
     * @return true, if successful
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds the all.
     *
     * @param newElements the new elements
     * @return true, if successful
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean addAll(Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the.
     *
     * @param object the object
     * @return true, if successful
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the if.
     *
     * @param filter the filter
     * @return true, if successful
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the all.
     *
     * @param oldElements the old elements
     * @return true, if successful
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean removeAll(Collection<?> oldElements) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retain all.
     *
     * @param elementsToKeep the elements to keep
     * @return true, if successful
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean retainAll(Collection<?> elementsToKeep) {
        throw new UnsupportedOperationException();
    }

    /**
     * Clear.
     *
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Contains.
     *
     * @param o the o
     * @return true, if successful
     */
    @Override
    public boolean contains(Object o) {
        return coll.contains(o);
    }

    /**
     * Iterator.
     *
     * @return the iterator
     */
    @Override
    public Iterator<E> iterator() {
        return coll.iterator();
    }

    /**
     * Size.
     *
     * @return the int
     */
    @Override
    public int size() {
        return coll.size();
    }

    /**
     * To array.
     *
     * @return the object[]
     */
    @Override
    public Object[] toArray() {
        return coll.toArray();
    }

    /**
     * To array.
     *
     * @param <T> the generic type
     * @param a the a
     * @return the t[]
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return coll.toArray(a);
    }

    /**
     * Streamm.
     *
     * @return the stream
     */
    public Stream<E> streamm() {
        return Stream.of(coll);
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof ImmutableCollection && ((ImmutableCollection<E>) obj).coll.equals(coll);
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return coll.hashCode();
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return coll.toString();
    }
}
