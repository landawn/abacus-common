/*
 * Copyright (c) 2022, Haiyang Li.
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

import java.util.ListIterator;
import java.util.NoSuchElementException;

@SuppressWarnings({ "java:S6548" })
public abstract class ImmutableListIterator<T> extends ObjIterator<T> implements ListIterator<T> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableListIterator EMPTY = new ImmutableListIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }
    };

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> ImmutableListIterator<T> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> ImmutableListIterator<T> of(final ListIterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ImmutableListIterator) {
            return (ImmutableListIterator<T>) iter;
        }

        return new ImmutableListIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }

            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            @Override
            public T previous() {
                return iter.previous();
            }

            @Override
            public int nextIndex() {
                return iter.nextIndex();
            }

            @Override
            public int previousIndex() {
                return iter.previousIndex();
            }
        };
    }

    /**
     *
     *
     * @param e
     * @throws UnsupportedOperationException
     * @deprecated - UnsupportedOperationException
     */
    @Deprecated
    @Override
    public void set(T e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param e
     * @throws UnsupportedOperationException
     * @deprecated - UnsupportedOperationException
     */
    @Deprecated
    @Override
    public void add(T e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
