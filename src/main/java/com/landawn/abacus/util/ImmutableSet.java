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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
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
    private static final ImmutableSet EMPTY = new ImmutableSet(Collections.EMPTY_SET);

    ImmutableSet(Set<? extends E> set) {
        super(Collections.unmodifiableSet(set));
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
        return new ImmutableSet<>(Collections.singleton(e));
    }

    /**
     *
     * @param <E>
     * @param e
     * @return
     */
    public static <E> ImmutableSet<E> of(E e) {
        return new ImmutableSet<>(Collections.singleton(e));
    }

    /**
     *
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <E> ImmutableSet<E> of(E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return new ImmutableSet<>(Collections.singleton(a[0]));
        } else {
            return new ImmutableSet<>(N.asLinkedHashSet(a));
        }
    }

    /**
     *
     * @param <E>
     * @param set the elements in this <code>Set</code> are shared by the returned ImmutableSet.
     * @return
     */
    public static <E> ImmutableSet<E> of(final Set<? extends E> set) {
        if (set == null) {
            return empty();
        } else if (set instanceof ImmutableSet) {
            return (ImmutableSet<E>) set;
        }

        return new ImmutableSet<>(set);
    }

    /**
     *
     * @param <E>
     * @param set
     * @return
     */
    public static <E> ImmutableSet<E> copyOf(final Collection<? extends E> set) {
        if (N.isNullOrEmpty(set)) {
            return empty();
        }

        return new ImmutableSet<>((set instanceof LinkedHashSet || set instanceof SortedSet) ? N.newLinkedHashSet(set) : N.newHashSet(set));
    }

    /**
     *
     * @param <E>
     * @param c
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <E> ImmutableCollection<E> of(final Collection<? extends E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
