/*
 * Copyright (c) 2020, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Enumerations {

    @SuppressWarnings("rawtypes")
    private static final Enumeration EMPTY = new Enumeration() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public Object nextElement() {
            throw new NoSuchElementException();
        }
    };

    private Enumerations() {
        // singleton
    }

    public static <T> Enumeration<T> empty() {
        return EMPTY;
    }

    public static <T> Enumeration<T> just(final T single) {
        return new Enumeration<T>() {
            private boolean hasNext = true;

            @Override
            public boolean hasMoreElements() {
                return hasNext;
            }

            @Override
            public T nextElement() {
                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return single;
            }
        };
    }

    public static <T> Enumeration<T> of(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new Enumeration<T>() {
            private final int len = a.length;
            private int cursor = 0;

            @Override
            public boolean hasMoreElements() {
                return cursor < len;
            }

            @Override
            public T nextElement() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }
        };
    }

    public static <T> Enumeration<T> of(final Collection<? extends T> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        return from(c.iterator());
    }

    public static <T> Enumeration<T> from(final Iterator<? extends T> iter) {
        return new Enumeration<T>() {
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
        if (N.isNullOrEmpty(a)) {
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
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        return new Enumeration<T>() {
            private final Iterator<? extends Enumeration<? extends T>> iter = c.iterator();
            private Enumeration<? extends T> cur;

            @Override
            public boolean hasMoreElements() {
                while ((cur == null || cur.hasMoreElements() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasMoreElements();
            }

            @Override
            public T nextElement() {
                if ((cur == null || cur.hasMoreElements() == false) && hasMoreElements() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextElement();
            }
        };
    }
}