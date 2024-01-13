/*
 * Copyright (C) 2017 HaiYang Li
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

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Haiyang Li
 * @param <E>
 * @since 1.1.4
 */
@SuppressWarnings("java:S2160")
public class ImmutableSortedSet<E> extends ImmutableSet<E> implements SortedSet<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSortedSet EMPTY = new ImmutableSortedSet(N.emptySortedSet());

    private final SortedSet<E> sortedSet;

    ImmutableSortedSet(SortedSet<? extends E> sortedSet) {
        super(sortedSet);
        this.sortedSet = (SortedSet<E>) sortedSet;
    }

    /**
     *
     * @param <E>
     * @return
     */
    public static <E> ImmutableSortedSet<E> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableSortedSet<T> just(T e) {
        return new ImmutableSortedSet<>(new TreeSet<>(N.asList(e)));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableSortedSet<T> of(final T e1) {
        return new ImmutableSortedSet<>(new TreeSet<>(N.asList(e1)));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableSortedSet<T> of(final T e1, final T e2) {
        return new ImmutableSortedSet<>(new TreeSet<>(N.asList(e1, e2)));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableSortedSet<T> of(final T e1, final T e2, final T e3) {
        return new ImmutableSortedSet<>(new TreeSet<>(N.asList(e1, e2, e3)));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableSortedSet<T> of(final T e1, final T e2, final T e3, final T e4) {
        return new ImmutableSortedSet<>(new TreeSet<>(N.asList(e1, e2, e3, e4)));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableSortedSet<T> of(final T e1, final T e2, final T e3, final T e4, final T e5) {
        return new ImmutableSortedSet<>(new TreeSet<>(N.asList(e1, e2, e3, e4, e5)));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableSortedSet<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        return new ImmutableSortedSet<>(new TreeSet<>(N.asList(e1, e2, e3, e4, e5, e6)));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableSortedSet<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6,
            final T e7) {
        return new ImmutableSortedSet<>(new TreeSet<>(N.asList(e1, e2, e3, e4, e5, e6, e7)));
    }

    /**
     *
     * @param <E>
     * @param sortedSet
     * @return
     */
    public static <E> ImmutableSortedSet<E> copyOf(final SortedSet<? extends E> sortedSet) {
        if (N.isEmpty(sortedSet)) {
            return empty();
        } else if (sortedSet instanceof ImmutableSortedSet) {
            return (ImmutableSortedSet<E>) sortedSet;
        }

        return new ImmutableSortedSet<>(new TreeSet<>(sortedSet));
    }

    /**
     *
     * @param <E>
     * @param sortedSet
     * @return an {@code ImmutableSortedSet} backed by the specified {@code sortedSet}
     * @deprecated the ImmutableSortedSet may be modified through the specified {@code sortedSet}
     */
    @Deprecated
    public static <E> ImmutableSortedSet<E> wrap(final SortedSet<? extends E> sortedSet) {
        if (sortedSet == null) {
            return empty();
        } else if (sortedSet instanceof ImmutableSortedSet) {
            return (ImmutableSortedSet<E>) sortedSet;
        }

        return new ImmutableSortedSet<>(sortedSet);
    }

    /**
     *
     *
     * @param <E>
     * @param set
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <E> ImmutableSet<E> wrap(final Set<? extends E> set) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Comparator<? super E> comparator() {
        return sortedSet.comparator();
    }

    /**
     *
     * @param fromElement
     * @param toElement
     * @return
     */
    @Override
    public ImmutableSortedSet<E> subSet(E fromElement, E toElement) {
        return wrap(sortedSet.subSet(fromElement, toElement));
    }

    /**
     *
     * @param toElement
     * @return
     */
    @Override
    public ImmutableSortedSet<E> headSet(E toElement) {
        return wrap(sortedSet.headSet(toElement));
    }

    /**
     *
     * @param fromElement
     * @return
     */
    @Override
    public ImmutableSortedSet<E> tailSet(E fromElement) {
        return wrap(sortedSet.tailSet(fromElement));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public E first() {
        return sortedSet.first();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public E last() {
        return sortedSet.last();
    }
}
