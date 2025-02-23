/*
 * Copyright (c) 2020, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This is a utility class that provides a set of static methods to work with Java Enumerations.
 * It includes methods for creating, concatenating, and converting Enumerations.
 *
 * @see com.landawn.abacus.util.ObjIterator
 * @see com.landawn.abacus.util.Iterators
 */
public final class Enumerations {

    @SuppressWarnings("rawtypes")
    private static final Enumeration EMPTY = new Enumeration() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public Object nextElement() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    private Enumerations() {
        // singleton
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Enumeration<T> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <T>
     * @param single
     * @return
     */
    public static <T> Enumeration<T> just(final T single) {
        return new Enumeration<>() {
            private boolean hasNext = true;

            @Override
            public boolean hasMoreElements() {
                return hasNext;
            }

            @Override
            public T nextElement() {
                if (!hasNext) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return single;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Enumeration<T> of(final T... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return new Enumeration<>() {
            private final int len = a.length;
            private int cursor = 0;

            @Override
            public boolean hasMoreElements() {
                return cursor < len;
            }

            @Override
            public T nextElement() {
                if (cursor >= len) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }
        };
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Enumeration<T> create(final Collection<? extends T> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return create(c.iterator());
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> Enumeration<T> create(final Iterator<? extends T> iter) {
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            @Override
            public T nextElement() {
                return iter.next();
            }
        };
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Enumeration<T> concat(final Enumeration<? extends T>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Enumeration<T> concat(final Collection<? extends Enumeration<? extends T>> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return new Enumeration<>() {
            private final Iterator<? extends Enumeration<? extends T>> iter = c.iterator();
            private Enumeration<? extends T> cur;

            @Override
            public boolean hasMoreElements() {
                while ((cur == null || !cur.hasMoreElements()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasMoreElements();
            }

            @Override
            public T nextElement() {
                if ((cur == null || !cur.hasMoreElements()) && !hasMoreElements()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextElement();
            }
        };
    }

    /**
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> ObjIterator<T> toIterator(final Enumeration<? extends T> e) {
        if (e == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return e.hasMoreElements();
            }

            @Override
            public T next() { // NOSONAR
                return e.nextElement();
            }
        };
    }

    /**
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> List<T> toList(final Enumeration<? extends T> e) {
        if (e == null) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();

        while (e.hasMoreElements()) {
            result.add(e.nextElement());
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> Set<T> toSet(final Enumeration<? extends T> e) {
        if (e == null) {
            return new HashSet<>();
        }

        final Set<T> result = new HashSet<>();

        while (e.hasMoreElements()) {
            result.add(e.nextElement());
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param e
     * @param supplier
     * @return
     */
    public static <T, C extends Collection<T>> C toCollection(final Enumeration<? extends T> e, final Supplier<? extends C> supplier) {
        final C c = supplier.get();

        if (e != null) {
            while (e.hasMoreElements()) {
                c.add(e.nextElement());
            }
        }

        return c;
    }
}
