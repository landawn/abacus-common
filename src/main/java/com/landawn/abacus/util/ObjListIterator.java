/*
 * Copyright (c) 2024, Haiyang Li.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.stream.Stream;


@SuppressWarnings({ "java:S6548" })
public abstract class ObjListIterator<T> extends ImmutableIterator<T> implements ListIterator<T> {

    @SuppressWarnings("rawtypes")
    private static final ObjListIterator EMPTY = new ObjListIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }

        /**
         *
         * @throws UnsupportedOperationException
         * @deprecated - UnsupportedOperationException
         */
        @Deprecated
        @Override
        public void set(final Object e) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @throws UnsupportedOperationException
         * @deprecated - UnsupportedOperationException
         */
        @Deprecated
        @Override
        public void add(final Object e) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    };

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> ObjListIterator<T> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <T>
     * @param val
     * @return
     */
    public static <T> ObjListIterator<T> just(final T val) {
        return of(List.of(val));
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ObjListIterator<T> of(final T... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return of(List.of(a));
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> ObjListIterator<T> of(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return empty();
        } else if (fromIndex == 0 && toIndex == a.length) {
            return of(Arrays.asList(a));
        } else {
            return of(Arrays.asList(a).subList(fromIndex, toIndex));
        }
    }

    /**
     *
     * @param <T>
     * @param list
     * @return
     */
    public static <T> ObjListIterator<T> of(final List<? extends T> list) {
        return list == null ? ObjListIterator.<T> empty() : of(list.listIterator());
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> ObjListIterator<T> of(final ListIterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjListIterator) {
            return (ObjListIterator<T>) iter;
        }

        return new ObjListIterator<>() {
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

            /**
             *
             * @throws UnsupportedOperationException
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void set(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             *
             * @throws UnsupportedOperationException
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void add(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     *
     *
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public ObjListIterator<T> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final ObjListIterator<T> iter = this;

        return new ObjListIterator<>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

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

            /**
             *
             * @throws UnsupportedOperationException
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void set(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             *
             * @throws UnsupportedOperationException
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void add(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.next();
                }

                skipped = true;
            }
        };
    }

    /**
     *
     *
     * @param count
     * @return
     * @throws IllegalArgumentException
     */
    public ObjListIterator<T> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return ObjListIterator.<T> empty();
        }

        final ObjListIterator<T> iter = this;

        return new ObjListIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
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

            /**
             *
             * @throws UnsupportedOperationException
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void set(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             *
             * @throws UnsupportedOperationException
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void add(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     *
     *
     * @return
     */
    public Nullable<T> first() {
        if (hasNext()) {
            return Nullable.of(next());
        } else {
            return Nullable.<T> empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public u.Optional<T> firstNonNull() {
        T next = null;

        while (hasNext()) {
            next = next();

            if (next != null) {
                return u.Optional.of(next);
            }
        }

        return u.Optional.empty();
    }

    /**
     *
     *
     * @return
     */
    public Nullable<T> last() {
        if (hasNext()) {
            T next = next();

            while (hasNext()) {
                next = next();
            }

            return Nullable.of(next);
        } else {
            return Nullable.<T> empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    /**
     *
     *
     * @param <A>
     * @param a
     * @return
     */
    public <A> A[] toArray(final A[] a) {
        return toList().toArray(a);
    }

    /**
     *
     *
     * @return
     */
    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public Stream<T> stream() {
        return Stream.of(this);
    }

    /**
     *
     *
     * @param <E>
     * @param action
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(next());
        }
    }

    /**
     *
     *
     * @param <E>
     * @param action
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, next());
        }
    }
}
