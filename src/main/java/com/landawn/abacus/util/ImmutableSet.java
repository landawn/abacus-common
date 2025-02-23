/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.landawn.abacus.annotation.Beta;

/**
 *
 * @param <E>
 */
public class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSet EMPTY = new ImmutableSet(N.emptySet(), false);

    /**
     * Constructs an ImmutableSet instance with the provided set.
     *
     * @param set the set of elements to be included in the ImmutableSet
     */
    ImmutableSet(final Set<? extends E> set) {
        this(set, ClassUtil.isPossibleImmutable(set.getClass()));
    }

    /**
     * Constructs an ImmutableSet instance.
     *
     * @param set the set of elements to be included in the ImmutableSet
     * @param isUnmodifiable a boolean value indicating if the set is unmodifiable
     */
    ImmutableSet(final Set<? extends E> set, final boolean isUnmodifiable) {
        super(isUnmodifiable ? set : Collections.unmodifiableSet(set));
    }

    /**
     * Returns an empty ImmutableSet.
     *
     * @param <E> the type of elements in this list
     * @return an empty ImmutableSet
     */
    public static <E> ImmutableSet<E> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableSet containing just the provided element.
     *
     * @param <E> the type of the element
     * @param e the element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided element
     */
    public static <E> ImmutableSet<E> just(final E e) {
        return new ImmutableSet<>(N.asLinkedHashSet(e), false);
    }

    /**
     * Returns an ImmutableSet containing just the provided element.
     *
     * @param <E> the type of the element
     * @param e1 the element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided element
     */
    public static <E> ImmutableSet<E> of(final E e1) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @param e3 the third element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @param e3 the third element to be included in the ImmutableSet
     * @param e4 the fourth element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @param e3 the third element to be included in the ImmutableSet
     * @param e4 the fourth element to be included in the ImmutableSet
     * @param e5 the fifth element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @param e3 the third element to be included in the ImmutableSet
     * @param e4 the fourth element to be included in the ImmutableSet
     * @param e5 the fifth element to be included in the ImmutableSet
     * @param e6 the sixth element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @param e3 the third element to be included in the ImmutableSet
     * @param e4 the fourth element to be included in the ImmutableSet
     * @param e5 the fifth element to be included in the ImmutableSet
     * @param e6 the sixth element to be included in the ImmutableSet
     * @param e7 the seventh element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6, e7), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @param e3 the third element to be included in the ImmutableSet
     * @param e4 the fourth element to be included in the ImmutableSet
     * @param e5 the fifth element to be included in the ImmutableSet
     * @param e6 the sixth element to be included in the ImmutableSet
     * @param e7 the seventh element to be included in the ImmutableSet
     * @param e8 the eighth element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6, e7, e8), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @param e3 the third element to be included in the ImmutableSet
     * @param e4 the fourth element to be included in the ImmutableSet
     * @param e5 the fifth element to be included in the ImmutableSet
     * @param e6 the sixth element to be included in the ImmutableSet
     * @param e7 the seventh element to be included in the ImmutableSet
     * @param e8 the eighth element to be included in the ImmutableSet
     * @param e9 the ninth element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6, e7, e8, e9), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableSet
     * @param e2 the second element to be included in the ImmutableSet
     * @param e3 the third element to be included in the ImmutableSet
     * @param e4 the fourth element to be included in the ImmutableSet
     * @param e5 the fifth element to be included in the ImmutableSet
     * @param e6 the sixth element to be included in the ImmutableSet
     * @param e7 the seventh element to be included in the ImmutableSet
     * @param e8 the eighth element to be included in the ImmutableSet
     * @param e9 the ninth element to be included in the ImmutableSet
     * @param e10 the tenth element to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9,
            final E e10) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10), false);
    }

    /**
     * Returns an ImmutableSet containing the provided elements. And it's not backed by the specified array.
     * If the specified array is {@code null} or empty, an empty {@code ImmutableSet} is returned.
     *
     * @param <E> the type of the elements
     * @param a the array of elements to be included in the ImmutableSet
     * @return an ImmutableSet containing the provided elements
     * @see Set#of(Object...)
     */
    @SafeVarargs
    public static <E> ImmutableSet<E> of(final E... a) {
        if (N.isEmpty(a)) {
            return empty();
        } else {
            return new ImmutableSet<>(N.asLinkedHashSet(a), false);
        }
    }

    /**
     * Returns an ImmutableSet containing the elements of the specified collection.
     * If the provided collection is already an instance of ImmutableSet, it is directly returned.
     * If the provided collection is {@code null} or empty, an empty ImmutableSet is returned.
     * Otherwise, a new ImmutableSet is created with the elements of the provided collection.
     *
     * @param <E> the type of elements in the collection
     * @param c the collection whose elements are to be placed into this set
     * @return an ImmutableSet containing the elements of the specified collection
     */
    public static <E> ImmutableSet<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableSet) {
            return (ImmutableSet<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return new ImmutableSet<>((c instanceof List || c instanceof LinkedHashSet || c instanceof SortedSet) ? N.newLinkedHashSet(c) : N.newHashSet(c),
                    false);
        }
    }

    /**
     * Wraps the provided set into an ImmutableSet. Changes to the specified Set will be reflected in the ImmutableSet.
     * If the provided set is already an instance of ImmutableSet, it is directly returned.
     * If the set is {@code null}, an empty ImmutableSet is returned.
     * Otherwise, returns a new ImmutableSet backed by the provided set.
     *
     * @param <E> the type of elements in the set
     * @param set the set to be wrapped into an ImmutableSet
     * @return an ImmutableSet backed by the provided set
     */
    @Beta
    public static <E> ImmutableSet<E> wrap(final Set<? extends E> set) {
        if (set instanceof ImmutableSet) {
            return (ImmutableSet<E>) set;
        } else if (set == null) {
            return empty();
        } else {
            return new ImmutableSet<>(set);
        }
    }

    /**
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     *
     * @param <E>
     * @param c
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <E> ImmutableCollection<E> wrap(final Collection<? extends E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new Builder instance for constructing an ImmutableSet.
     *
     * @param <E> the type of elements maintained by the set
     * @return a new Builder instance
     */
    public static <E> Builder<E> builder() {
        return new Builder<>(new HashSet<>());
    }

    /**
     * Creates a new Builder instance for constructing an ImmutableSet with the provided set as the holder.
     *
     * @param <E> the type of elements maintained by the set
     * @param holder the set to be used as the holder for the Builder
     * @return a new Builder instance
     */
    public static <E> Builder<E> builder(final Set<E> holder) {
        return new Builder<>(holder);
    }

    public static final class Builder<E> {
        private final Set<E> set;

        Builder(final Set<E> holder) {
            set = holder;
        }

        /**
         *
         * @param element
         * @return
         */
        public Builder<E> add(final E element) {
            set.add(element);

            return this;
        }

        /**
         *
         * @param elements
         * @return
         */
        @SafeVarargs
        public final Builder<E> add(final E... elements) {
            if (N.notEmpty(elements)) {
                set.addAll(Arrays.asList(elements));
            }

            return this;
        }

        /**
         *
         * @param c
         * @return
         */
        public Builder<E> addAll(final Collection<? extends E> c) {
            if (N.notEmpty(c)) {
                set.addAll(c);
            }

            return this;
        }

        /**
         *
         * @param iter
         * @return
         */
        public Builder<E> addAll(final Iterator<? extends E> iter) {
            if (iter != null) {
                while (iter.hasNext()) {
                    set.add(iter.next());
                }
            }

            return this;
        }

        public ImmutableSet<E> build() {
            return new ImmutableSet<>(set);
        }
    }
}
