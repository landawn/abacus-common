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
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.9
 */
public abstract class ObjIterator<T> extends ImmutableIterator<T> {

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
     *
     * @param <T>
     * @return
     */
    public static <T> ObjIterator<T> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <T>
     * @param val
     * @return
     */
    public static <T> ObjIterator<T> just(final T val) {
        return new ObjIterator<>() {
            private boolean done = false;

            @Override
            public boolean hasNext() {
                return !done;
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
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ObjIterator<T> of(final T... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> ObjIterator<T> of(final T[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ObjIterator<>() {
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
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> ObjIterator<T> of(final Iterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIterator) {
            return (ObjIterator<T>) iter;
        }

        return new ObjIterator<>() {
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
     *
     * @param <T>
     * @param iterable
     * @return
     */
    public static <T> ObjIterator<T> of(final Collection<? extends T> iterable) {
        return iterable == null ? ObjIterator.<T> empty() : of(iterable.iterator());
    }

    /**
     *
     * @param <T>
     * @param iterable
     * @return
     */
    public static <T> ObjIterator<T> of(final Iterable<? extends T> iterable) {
        return iterable == null ? ObjIterator.<T> empty() : of(iterable.iterator());
    }

    //    /**
    //     * Lazy evaluation.
    //     *
    //     * @param <T>
    //     * @param arraySupplier
    //     * @return
    //     */
    //    public static <T> ObjIterator<T> from(final Supplier<T[]> arraySupplier) {
    //        N.checkArgNotNull(arraySupplier, "arraySupplier");
    //
    //        return new ObjIterator<>() {
    //            private T[] aar = null;
    //            private int len = 0;
    //            private int cur = 0;
    //            private boolean isInitialized = false;
    //
    //            @Override
    //            public boolean hasNext() {
    //                if (!isInitialized) {
    //                    init();
    //                }
    //
    //                return cur < len;
    //            }
    //
    //            @Override
    //            public T next() {
    //                if (!isInitialized) {
    //                    init();
    //                }
    //
    //                if (cur >= len) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                return aar[cur++];
    //            }
    //
    //            private void init() {
    //                if (!isInitialized) {
    //                    isInitialized = true;
    //                    aar = arraySupplier.get();
    //                    len = N.len(aar);
    //                }
    //            }
    //        };
    //    }

    /**
     * Lazy evaluation.
     *
     * @param <T>
     * @param iteratorSupplier
     * @return
     */
    public static <T> ObjIterator<T> defer(final Supplier<? extends Iterator<? extends T>> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new ObjIterator<>() {
            private Iterator<? extends T> iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (!isInitialized) {
                    init();
                }

                return iter.next();
            }

            private void init() {
                if (!isInitialized) {
                    isInitialized = true;
                    iter = iteratorSupplier.get();
                }
            }
        };
    }

    /**
     * Returns an infinite {@code ObjIterator}.
     *
     * @param <T>
     * @param supplier
     * @return
     */
    public static <T> ObjIterator<T> generate(final Supplier<? extends T> supplier) {
        N.checkArgNotNull(supplier);

        return new ObjIterator<>() {
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
     *
     * @param <T>
     * @param hasNext
     * @param supplier
     * @return
     */
    public static <T> ObjIterator<T> generate(final BooleanSupplier hasNext, final Supplier<? extends T> supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return supplier.get();
            }
        };
    }

    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param n
    //     * @return
    //     * @see Iterators#skip(Iterator, long)
    //     */
    //    public ObjIterator<T> skip(final long n) {
    //        return Iterators.skip(this, n);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param count
    //     * @return
    //     * @see Iterators#limit(Iterator, long)
    //     */
    //    public ObjIterator<T> limit(final long count) {
    //        return Iterators.limit(this, count);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param offset
    //     * @param count
    //     * @return
    //     * @see Iterators#skipAndLimit(Iterator, long, long)
    //     */
    //    public ObjIterator<T> skipAndLimit(final long offset, final long count) {
    //        return Iterators.skipAndLimit(this, offset, count);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param filter
    //     * @return
    //     * @see Iterators#filter(Iterator, Predicate)
    //     */
    //    public ObjIterator<T> filter(final Predicate<? super T> filter) {
    //        return Iterators.filter(this, filter);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param filter
    //     * @return
    //     * @see Iterators#takeWhile(Iterator, Predicate)
    //     */
    //    public ObjIterator<T> takeWhile(final Predicate<? super T> filter) {
    //        return Iterators.takeWhile(this, filter);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param filter
    //     * @return
    //     * @see Iterators#takeWhileInclusive(Iterator, Predicate)
    //     */
    //    public ObjIterator<T> takeWhileInclusive(final Predicate<? super T> filter) {
    //        return Iterators.takeWhileInclusive(this, filter);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param filter
    //     * @return
    //     * @see Iterators#dropWhile(Iterator, Predicate)
    //     */
    //    public ObjIterator<T> dropWhile(final Predicate<? super T> filter) {
    //        return Iterators.dropWhile(this, filter);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param filter
    //     * @return
    //     * @see Iterators#skipUntil(Iterator, Predicate)
    //     */
    //    public ObjIterator<T> skipUntil(final Predicate<? super T> filter) {
    //        return Iterators.skipUntil(this, filter);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param <U>
    //     * @param mapper
    //     * @return
    //     * @see Iterators#map(Iterator, Function)
    //     */
    //    public <U> ObjIterator<U> map(final Function<? super T, U> mapper) {
    //        return Iterators.map(this, mapper);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param <U>
    //     * @param mapper
    //     * @return
    //     * @see Iterators#flatMap(Iterator, Function)
    //     */
    //    public <U> ObjIterator<U> flatMap(final Function<? super T, ? extends Collection<? extends U>> mapper) {
    //        return Iterators.flatMap(this, mapper);
    //    }
    //
    //    /**
    //     * Returns a new {@code ObjIterator}.
    //     *
    //     * @param <U>
    //     * @param mapper
    //     * @return
    //     * @see Iterators#flatmap(Iterator, Function)
    //     */
    //    public <U> ObjIterator<U> flatmap(final Function<? super T, ? extends U[]> mapper) {
    //        return Iterators.flatmap(this, mapper);
    //    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param init
     * @param hasNext
     * @param supplier
     * @return
     */
    public static <T, U> ObjIterator<T> generate(final U init, final Predicate<? super U> hasNext, final Function<? super U, T> supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return hasNext.test(init);
            }

            @Override
            public T next() {
                return supplier.apply(init);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param init
     * @param hasNext
     * @param supplier
     * @return
     */
    public static <T, U> ObjIterator<T> generate(final U init, final BiPredicate<? super U, T> hasNext, final BiFunction<? super U, T, T> supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ObjIterator<>() {
            private T prev = null;

            @Override
            public boolean hasNext() {
                return hasNext.test(init, prev);
            }

            @Override
            public T next() {
                return (prev = supplier.apply(init, prev));
            }
        };
    }

    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    public <A> A[] toArray(A[] a) {
        return toList().toArray(a);
    }

    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    @Beta
    public ObjIterator<T> filter(final Predicate<? super T> filter) {
        return Iterators.filter(this, filter);
    }

    @Beta
    public <U> ObjIterator<U> map(final Function<? super T, U> mapper) {
        return Iterators.map(this, mapper);
    }

    public Stream<T> stream() {
        return Stream.of(this);
    }

    @Beta
    public ObjIterator<Indexed<T>> indexed() {
        return indexed(0);
    }

    @Beta
    public ObjIterator<Indexed<T>> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final ObjIterator<T> iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Indexed<T> next() {
                return Indexed.of(iter.next(), idx++);
            }
        };
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Throwables.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(next());
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(Throwables.IndexedConsumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, next());
        }
    }
}
