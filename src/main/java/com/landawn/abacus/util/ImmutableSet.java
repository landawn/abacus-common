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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableSet.
 *
 * @author Haiyang Li
 * @param <E> the element type
 * @since 0.8
 */
public class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {

    /** The Constant EMPTY. */
    @SuppressWarnings("rawtypes")
    private static final ImmutableSet EMPTY = new ImmutableSet(Collections.EMPTY_SET);

    /**
     * Instantiates a new immutable set.
     *
     * @param set the set
     */
    ImmutableSet(Set<? extends E> set) {
        super(Collections.unmodifiableSet(set));
    }

    /**
     * Empty.
     *
     * @param <E> the element type
     * @return the immutable set
     */
    public static <E> ImmutableSet<E> empty() {
        return EMPTY;
    }

    /**
     * Just.
     *
     * @param <E> the element type
     * @param e the e
     * @return the immutable set
     */
    public static <E> ImmutableSet<E> just(E e) {
        return new ImmutableSet<>(Collections.singleton(e));
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param e the e
     * @return the immutable set
     */
    public static <E> ImmutableSet<E> of(E e) {
        return new ImmutableSet<>(Collections.singleton(e));
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param a the a
     * @return the immutable set
     */
    @SafeVarargs
    public static <E> ImmutableSet<E> of(E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableSet<>(N.asLinkedHashSet(a));
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param set the elements in this <code>Set</code> are shared by the returned ImmutableSet.
     * @return the immutable set
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
     * Copy of.
     *
     * @param <E> the element type
     * @param set the set
     * @return the immutable set
     */
    public static <E> ImmutableSet<E> copyOf(final Collection<? extends E> set) {
        if (N.isNullOrEmpty(set)) {
            return empty();
        }

        return new ImmutableSet<>((set instanceof LinkedHashSet || set instanceof SortedSet) ? new LinkedHashSet<>(set) : new HashSet<>(set));
    }
}
