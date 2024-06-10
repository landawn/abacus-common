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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 *
 * @author Haiyang Li
 * @param <E>
 * @since 0.8
 */
public class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSet EMPTY = new ImmutableSet(Set.of(), true);

    ImmutableSet(final Set<? extends E> set) {
        this(set, false);
    }

    ImmutableSet(final Set<? extends E> set, final boolean isUnmodifiable) {
        super(isUnmodifiable ? set : Collections.unmodifiableSet(set));
    }

    /**
     *
     * @param <E>
     * @return
     */
    public static <E> ImmutableSet<E> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <E>
     * @param e
     * @return
     */
    public static <E> ImmutableSet<E> just(E e) {
        return new ImmutableSet<>(Set.of(e), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @return
     */
    public static <E> ImmutableSet<E> of(final E e1) {
        return new ImmutableSet<>(Set.of(e1), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @return
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2) {
        return new ImmutableSet<>(Set.of(e1, e2), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableSet<>(Set.of(e1, e2, e3), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableSet<>(Set.of(e1, e2, e3, e4), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableSet<>(Set.of(e1, e2, e3, e4, e5), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @return
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableSet<>(Set.of(e1, e2, e3, e4, e5, e6), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @return
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7) {
        return new ImmutableSet<>(Set.of(e1, e2, e3, e4, e5, e6, e7), true);
    }

    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8) {
        return new ImmutableSet<>(Set.of(e1, e2, e3, e4, e5, e6, e7, e8), true);
    }

    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9) {
        return new ImmutableSet<>(Set.of(e1, e2, e3, e4, e5, e6, e7, e8, e9), true);
    }

    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9,
            final E e10) {
        return new ImmutableSet<>(Set.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10), true);
    }

    /**
     *
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <E> ImmutableSet<E> of(final E... a) {
        if (N.isEmpty(a)) {
            return empty();
        } else {
            return new ImmutableSet<>(Set.of(a), true);
        }
    }

    /**
     *
     * @param <E>
     * @param c
     * @return
     */
    public static <E> ImmutableSet<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableSet) {
            return (ImmutableSet<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return new ImmutableSet<>(Set.of((c instanceof List || c instanceof LinkedHashSet || c instanceof SortedSet) ? (E[]) N.newLinkedHashSet(c).toArray()
                    : (E[]) N.newHashSet(c).toArray()), true);
        }
    }

    /**
     *
     * @param <E>
     * @param set
     * @return an {@code ImmutableSet} backed by the specified {@code set}
     */
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
     *
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
     *
     *
     * @param <E>
     * @return
     */
    public static <E> Builder<E> builder() {
        return new Builder<>(new HashSet<>());
    }

    /**
     *
     *
     * @param <E>
     * @param holder
     * @return
     */
    public static <E> Builder<E> builder(final Set<E> holder) {
        return new Builder<>(holder);
    }

    public static final class Builder<E> {
        private final Set<E> set;

        Builder(final Set<E> holder) {
            this.set = holder;
        }

        /**
         *
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
         *
         * @param elements
         * @return
         */
        public Builder<E> add(final E... elements) {
            if (N.notEmpty(elements)) {
                set.addAll(Arrays.asList(elements));
            }

            return this;
        }

        /**
         *
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

        /**
         *
         *
         * @return
         */
        public ImmutableSet<E> build() {
            return new ImmutableSet<>(set);
        }
    }
}
