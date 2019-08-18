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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableSortedSet.
 *
 * @author Haiyang Li
 * @param <E>
 * @since 1.1.4
 */
public class ImmutableSortedSet<E> extends ImmutableSet<E> implements SortedSet<E> {

    /** The Constant EMPTY. */
    @SuppressWarnings("rawtypes")
    private static final ImmutableSortedSet EMPTY = new ImmutableSortedSet(N.emptySortedSet());

    /** The sorted set. */
    private final SortedSet<E> sortedSet;

    /**
     * Instantiates a new immutable sorted set.
     *
     * @param sortedSet
     */
    ImmutableSortedSet(SortedSet<? extends E> sortedSet) {
        super(sortedSet);
        this.sortedSet = (SortedSet<E>) sortedSet;
    }

    /**
     * Empty.
     *
     * @param <E>
     * @return
     */
    public static <E> ImmutableSortedSet<E> empty() {
        return EMPTY;
    }

    /**
     * Of.
     *
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(a)));
    }

    /**
     * Of.
     *
     * @param <E>
     * @param sortedSet the elements in this <code>Set</code> are shared by the returned ImmutableSortedSet.
     * @return
     */
    public static <E> ImmutableSortedSet<E> of(final SortedSet<? extends E> sortedSet) {
        if (sortedSet == null) {
            return empty();
        } else if (sortedSet instanceof ImmutableSortedSet) {
            return (ImmutableSortedSet<E>) sortedSet;
        }

        return new ImmutableSortedSet<>(sortedSet);
    }

    /**
     * Copy of.
     *
     * @param <E>
     * @param sortedSet
     * @return
     */
    public static <E> ImmutableSortedSet<E> copyOf(final SortedSet<? extends E> sortedSet) {
        if (N.isNullOrEmpty(sortedSet)) {
            return empty();
        }

        return new ImmutableSortedSet<>(new TreeSet<>(sortedSet));
    }

    /**
     * Of.
     *
     * @param <E>
     * @param set
     * @return
     */
    @Deprecated
    public static <E> ImmutableSet<E> of(final Set<? extends E> set) {
        throw new UnsupportedOperationException();
    }

    /**
     * Copy of.
     *
     * @param <E>
     * @param set
     * @return
     */
    @Deprecated
    public static <E> ImmutableSet<E> copyOf(final Collection<? extends E> set) {
        throw new UnsupportedOperationException();
    }

    /**
     * Comparator.
     *
     * @return
     */
    @Override
    public Comparator<? super E> comparator() {
        return sortedSet.comparator();
    }

    /**
     * Sub set.
     *
     * @param fromElement
     * @param toElement
     * @return
     */
    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return of(sortedSet.subSet(fromElement, toElement));
    }

    /**
     * Head set.
     *
     * @param toElement
     * @return
     */
    @Override
    public SortedSet<E> headSet(E toElement) {
        return of(sortedSet.headSet(toElement));
    }

    /**
     * Tail set.
     *
     * @param fromElement
     * @return
     */
    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return of(sortedSet.tailSet(fromElement));
    }

    /**
     * First.
     *
     * @return
     */
    @Override
    public E first() {
        return sortedSet.first();
    }

    /**
     * Last.
     *
     * @return
     */
    @Override
    public E last() {
        return sortedSet.last();
    }
}
