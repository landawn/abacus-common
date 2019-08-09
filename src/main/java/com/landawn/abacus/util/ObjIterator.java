/*
 * Copyright (c) 2017, Haiyang Li.
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Stream;

// TODO: Auto-generated Javadoc
/**
 * The Class ObjIterator.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.9
 */
public abstract class ObjIterator<T> extends ImmutableIterator<T> {

    /** The Constant EMPTY. */
    @SuppressWarnings("rawtypes")
    private static final ObjIterator EMPTY = new ObjIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    /**
     * Empty.
     *
     * @param <T> the generic type
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> empty() {
        return EMPTY;
    }

    /**
     * Just.
     *
     * @param <T> the generic type
     * @param val the val
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> just(final T val) {
        return new ObjIterator<T>() {
            private boolean done = false;

            @Override
            public boolean hasNext() {
                return done == false;
            }

            @Override
            public T next() {
                if (done) {
                    throw new NoSuchElementException();
                }

                done = true;

                return val;
            }
        };
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param a the a
     * @return the obj iterator
     */
    @SafeVarargs
    public static <T> ObjIterator<T> of(final T... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param a the a
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> of(final T[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ObjIterator<T>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }

            @Override
            public <A> A[] toArray(A[] output) {
                if (output.length < toIndex - cursor) {
                    output = N.copyOf(output, toIndex - cursor);
                }

                N.copy(a, cursor, output, 0, toIndex - cursor);

                return output;
            }

            @Override
            public List<T> toList() {
                return N.asList((T[]) toArray());
            }
        };
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param iter the iter
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> of(final Iterator<T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIterator) {
            return (ObjIterator<T>) iter;
        }

        return new ObjIterator<T>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }
        };
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param iterable the iterable
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> of(final Collection<T> iterable) {
        return iterable == null ? ObjIterator.<T> empty() : of(iterable.iterator());
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param iterable the iterable
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> of(final Iterable<T> iterable) {
        return iterable == null ? ObjIterator.<T> empty() : of(iterable.iterator());
    }

    /**
     * Lazy evaluation.
     *
     * @param <T> the generic type
     * @param iteratorSupplier the iterator supplier
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> of(final Supplier<? extends Iterator<? extends T>> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new ObjIterator<T>() {
            private Iterator<? extends T> iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (isInitialized == false) {
                    init();
                }

                return iter.next();
            }

            private void init() {
                if (isInitialized == false) {
                    isInitialized = true;
                    iter = iteratorSupplier.get();
                }
            }
        };
    }

    /**
     * Lazy evaluation.
     *
     * @param <T> the generic type
     * @param arraySupplier the array supplier
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> oF(final Supplier<T[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new ObjIterator<T>() {
            private T[] aar = null;
            private int len = 0;
            private int cur = 0;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return cur < len;
            }

            @Override
            public T next() {
                if (isInitialized == false) {
                    init();
                }

                if (cur >= len) {
                    throw new NoSuchElementException();
                }

                return aar[cur++];
            }

            private void init() {
                if (isInitialized == false) {
                    isInitialized = true;
                    aar = arraySupplier.get();
                    len = N.len(aar);
                }
            }
        };
    }

    /**
     * Returns an infinite {@code ObjIterator}.
     *
     * @param <T> the generic type
     * @param supplier the supplier
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> generate(final Supplier<T> supplier) {
        N.checkArgNotNull(supplier);

        return new ObjIterator<T>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return supplier.get();
            }
        };
    }

    /**
     * Generate.
     *
     * @param <T> the generic type
     * @param hasNext the has next
     * @param supplier the supplier
     * @return the obj iterator
     */
    public static <T> ObjIterator<T> generate(final BooleanSupplier hasNext, final Supplier<T> supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ObjIterator<T>() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return supplier.get();
            }
        };
    }

    /**
     * To array.
     *
     * @return the object[]
     */
    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    /**
     * To array.
     *
     * @param <A> the generic type
     * @param a the a
     * @return the a[]
     */
    public <A> A[] toArray(A[] a) {
        return toList().toArray(a);
    }

    /**
     * To list.
     *
     * @return the list
     */
    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    /**
     * Stream.
     *
     * @return the stream
     */
    public Stream<T> stream() {
        return Stream.of(this);
    }

    /**
     * Foreach remaining.
     *
     * @param <E> the element type
     * @param action the action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Try.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(next());
        }
    }
}
