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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @see ObjListIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 * @see Throwables.Iterator
 * @param <T>
 */
@SuppressWarnings({ "java:S6548" })
public abstract class ObjIterator<T> extends ImmutableIterator<T> {

    @SuppressWarnings("rawtypes")
    private static final ObjIterator EMPTY = new ObjIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public ObjIterator skipNulls() {
            return this;
        }
    };

    /**
     * Returns an empty ObjIterator instance.
     *
     * @return an ObjIterator with no elements
     */
    public static <T> ObjIterator<T> empty() {
        return EMPTY;
    }

    /**
     * Returns an ObjIterator with a single element.
     *
     * @param <T> The type of the element in the iterator.
     * @param val The single value to be iterated.
     * @return An ObjIterator with a single element.
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                done = true;

                return val;
            }
        };
    }

    /**
     * Returns an ObjIterator for the given array.
     *
     * @param <T> The type of the elements in the array.
     * @param a The array to be iterated.
     * @return An ObjIterator for the given array.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> of(final T... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Returns an ObjIterator for the given array, starting from the specified index and ending at the specified index.
     *
     * @param <T> The type of the elements in the array.
     * @param a The array to be iterated.
     * @param fromIndex The index to start the iteration from.
     * @param toIndex The index to end the iteration at.
     * @return An ObjIterator for the given array starting from the specified index and ending at the specified index.
     * @throws IndexOutOfBoundsException If fromIndex is negative, toIndex is greater than the length of the array, or fromIndex is greater than toIndex.
     */
    public static <T> ObjIterator<T> of(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
     * Returns an ObjIterator(wrapper) for the given Iterator. If the Iterator is {@code null}, returns an empty ObjIterator.
     *
     * @param <T> The type of the elements in the iterator.
     * @param iter The Iterator to be converted into an ObjIterator.
     * @return An ObjIterator for the given Iterator.
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
     * Returns an ObjIterator for the given Collection. If the Collection is empty, returns an empty ObjIterator.
     *
     * @param <T> The type of the elements in the collection.
     * @param iterable The Collection to be converted into an ObjIterator.
     * @return An ObjIterator for the given Collection.
     */
    public static <T> ObjIterator<T> of(final Collection<? extends T> iterable) {
        return iterable == null ? ObjIterator.empty() : of(iterable.iterator());
    }

    /**
     * Returns an ObjIterator for the given Iterable. If the Iterable is {@code null}, returns an empty ObjIterator.
     *
     * @param <T> The type of the elements in the iterable.
     * @param iterable The Iterable to be converted into an ObjIterator.
     * @return An ObjIterator for the given Iterable.
     */
    public static <T> ObjIterator<T> of(final Iterable<? extends T> iterable) {
        return iterable == null ? ObjIterator.empty() : of(iterable.iterator());
    }

    /**
     * Returns an ObjIterator instance that is created lazily using the provided Supplier.
     * The Supplier is responsible for producing the ObjIterator instance when the first method in the returned {@code ObjIterator} is called.
     *
     * @param <T> the type of elements returned by this iterator
     * @param iteratorSupplier A Supplier that produces an Iterator instance when called.
     * @return An ObjIterator<T> that uses the provided Supplier to generate its Iterator instance.
     * @throws IllegalArgumentException If iteratorSupplier is {@code null}.
     */
    public static <T> ObjIterator<T> defer(final Supplier<? extends Iterator<? extends T>> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

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
     * Generates an infinite {@code ObjIterator} instance with the provided supplier.
     * The supplier is responsible for producing the next element on each iteration.
     *
     * @param <T> the type of elements returned by this iterator
     * @param supplier A Supplier that produces the next element on each iteration.
     * @return An ObjIterator<T> that uses the provided supplier to generate its elements.
     * @throws IllegalArgumentException If supplier is {@code null}.
     * @see ObjIterator#generate(BooleanSupplier, Supplier)
     */
    public static <T> ObjIterator<T> generate(final Supplier<? extends T> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() { // NOSONAR
                return supplier.get();
            }
        };
    }

    /**
     * Generates an ObjIterator instance with the provided hasNext BooleanSupplier and supplier.
     * The hasNext BooleanSupplier is used to determine if the iterator has more elements.
     * The supplier is responsible for producing the next element on each iteration.
     *
     * @param <T> the type of elements returned by this iterator
     * @param hasNext A BooleanSupplier that returns {@code true} if the iterator has more elements.
     * @param supplier A Supplier that produces the next element on each iteration.
     * @return An ObjIterator<T> that uses the provided hasNext BooleanSupplier and supplier to generate its elements.
     * @throws IllegalArgumentException If hasNext or supplier is {@code null}.
     */
    public static <T> ObjIterator<T> generate(final BooleanSupplier hasNext, final Supplier<? extends T> supplier) throws IllegalArgumentException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
     * Generates an ObjIterator instance with the provided initial value, hasNext Predicate, and supplier.
     * The hasNext Predicate is used to determine if the iterator has more elements.
     * The supplier is responsible for producing the next element on each iteration.
     *
     * @param <T> the type of elements returned by this iterator
     * @param <U> the type of the initial value
     * @param init The value for the first call from <i>hasNext</i> and <i>supplier</> functions.
     * @param hasNext A Predicate that accepts an instance of U and returns {@code true} if the iterator has more elements.
     * @param supplier A Function that accepts an instance of U and produces the next element on each iteration.
     * @return An ObjIterator<T> that uses the provided initial value, hasNext Predicate, and supplier to generate its elements.
     * @throws IllegalArgumentException If hasNext or supplier is {@code null}.
     */
    public static <T, U> ObjIterator<T> generate(final U init, final Predicate<? super U> hasNext, final Function<? super U, T> supplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return hasNext.test(init);
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return supplier.apply(init);
            }
        };
    }

    /**
     * Generates an ObjIterator instance with the provided initial value, hasNext BiPredicate, and supplier.
     * The hasNext BiPredicate is used to determine if the iterator has more elements.
     * The supplier is responsible for producing the next element on each iteration.
     *
     * @param <T> the type of elements returned by this iterator
     * @param <U> the type of the initial value
     * @param init The value for the first call from <i>hasNext</i> and <i>supplier</> functions.
     * @param hasNext A BiPredicate that accepts an instance of U and T and returns {@code true} if the iterator has more elements.
     * @param supplier A BiFunction that accepts an instance of U and T and produces the next element on each iteration.
     * @return An ObjIterator<T> that uses the provided initial value, hasNext BiPredicate, and supplier to generate its elements.
     * @throws IllegalArgumentException If hasNext or supplier is {@code null}.
     */
    public static <T, U> ObjIterator<T> generate(final U init, final BiPredicate<? super U, T> hasNext, final BiFunction<? super U, T, T> supplier)
            throws IllegalArgumentException {
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
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return (prev = supplier.apply(init, prev));
            }
        };
    }

    /**
     * Returns a new ObjIterator with <i>n</i> elements skipped from the beginning of this iterator.
     *
     * @param n The number of elements to skip.
     * @return A new ObjIterator that skips the first <i>n</i> elements.
     * @throws IllegalArgumentException If <i>n</i> is negative.
     */
    public ObjIterator<T> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final ObjIterator<T> iter = this;

        return new ObjIterator<>() {
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
     * Returns a new ObjIterator with <i>count</i> elements from the beginning of this iterator.
     *
     * @param count The number of elements to limit.
     * @return A new ObjIterator that includes the first <i>count</i> elements.
     * @throws IllegalArgumentException If <i>count</i> is negative.
     */
    public ObjIterator<T> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return ObjIterator.empty();
        }

        final ObjIterator<T> iter = this;

        return new ObjIterator<>() {
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
        };
    }

    /**
     * Returns a new ObjIterator that skips the first <i>offset</i> elements and includes the next <i>count</i> elements.
     *
     * @param offset The number of elements to skip from the beginning of this iterator.
     * @param count The number of elements to include after skipping.
     * @return A new ObjIterator that skips the first <i>offset</i> elements and includes the next <i>count</i> elements.
     * @throws IllegalArgumentException If <i>offset</i> or <i>count</i> is negative.
     */
    public ObjIterator<T> skipAndLimit(final long offset, final long count) {
        return Iterators.skipAndLimit(this, offset, count);
    }

    /**
     * Returns a new ObjIterator that includes only the elements that satisfy the provided predicate.
     *
     * @param predicate The Predicate to apply to each element.
     * @return A new ObjIterator that includes only the elements that satisfy the provided predicate.
     */
    public ObjIterator<T> filter(final Predicate<? super T> predicate) {
        return Iterators.filter(this, predicate);
    }

    /**
     * Transforms the elements in this ObjIterator by applying the provided mapping function.
     *
     * @param <U> The type of the elements in the resulting ObjIterator.
     * @param mapper The function to apply to each element in this ObjIterator.
     * @return A new ObjIterator that includes the transformed elements.
     */
    @Beta
    public <U> ObjIterator<U> map(final Function<? super T, U> mapper) {
        return Iterators.map(this, mapper);
    }

    /**
     * Returns a new ObjIterator with distinct elements based on their natural equality.
     *
     * @return
     */
    public ObjIterator<T> distinct() {
        final Set<T> elements = new HashSet<>();
        return filter(elements::add);
    }

    /**
     * Returns a new ObjIterator with distinct elements based on the keyExtractor.
     * The keyExtractor is used to extract the key for each element, and the key is used to determine the uniqueness of the element.
     * If the keyExtractor returns the same key for multiple elements, only the first element with that key is included in the resulting ObjIterator.
     * @param keyExtractor
     * @return
     */
    public ObjIterator<T> distinctBy(final Function<? super T, ?> keyExtractor) {
        final Set<Object> elements = new HashSet<>();
        return filter(e -> elements.add(keyExtractor.apply(e)));
    }

    /**
     * Returns the first element in the ObjIterator, if it exists.
     *
     * @return A {@code Nullable} containing the first element if it exists, otherwise an empty {@code Nullable}.
     */
    public Nullable<T> first() {
        if (hasNext()) {
            return Nullable.of(next());
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Returns the first {@code non-null} element in the ObjIterator, if it exists.
     *
     * @return An Optional containing the first {@code non-null} element if it exists, otherwise an empty Optional.
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
     * Returns the last element in the ObjIterator, if it exists.
     *
     * @return A {@code Nullable} containing the last element if it exists, otherwise an empty {@code Nullable}.
     */
    public Nullable<T> last() {
        if (hasNext()) {
            T next = next();

            while (hasNext()) {
                next = next();
            }

            return Nullable.of(next);
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Returns a new ObjIterator that excludes {@code null} elements from the original ObjIterator.
     *
     * @return A new ObjIterator that excludes {@code null} elements.
     */
    public ObjIterator<T> skipNulls() {
        return Iterators.skipNulls(this);
    }

    /**
     * Converts the remaining elements in the ObjIterator to an array.
     *
     * @return An array containing the remaining elements in the ObjIterator.
     */
    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Converts the remaining elements in the ObjIterator to an array of the type specified by the input parameter.
     *
     * @param <A> The type of the elements in the resulting array.
     * @param a An array that determines the component type of the returned array.
     * @return An array containing the remaining elements in the ObjIterator. The runtime type of the returned array is that of the specified array.
     */
    public <A> A[] toArray(final A[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts the remaining elements in the ObjIterator to a List.
     *
     * @return A List containing the remaining elements in the ObjIterator.
     */
    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    /**
     * Returns a Stream of the elements in this ObjIterator.
     *
     * @return A Stream containing the remaining elements in the ObjIterator.
     */
    public Stream<T> stream() {
        return Stream.of(this);
    }

    /**
     * Returns a new ObjIterator that includes the index with each element.
     * The index starts from 0 and increments by 1 for each element.
     *
     * @return A new ObjIterator of Indexed<T> where each Indexed<T> contains the element and its index.
     */
    @Beta
    public ObjIterator<Indexed<T>> indexed() {
        return indexed(0);
    }

    /**
     * Returns a new ObjIterator that includes the index with each element.
     * The index starts from the specified start index and increments by 1 for each element.
     *
     * @param startIndex The start index for the indexing.
     * @return A new ObjIterator of Indexed<T> where each Indexed<T> contains the element and its index.
     * @throws IllegalArgumentException If startIndex is negative.
     */
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
     * Executes the provided action for each remaining element in the iterator.
     *
     * @param <E> The type of the exception that may be thrown by the action.
     * @param action The action to be performed for each remaining element.
     * @throws IllegalArgumentException if the action is {@code null}.
     * @throws E Any exception thrown by the action.
     */
    public <E extends Exception> void foreachRemaining(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(next());
        }
    }

    /**
     * Executes the provided action for each element in the iterator, providing the index of the element as the first argument to the action, and the second argument is the element itself.
     *
     * @param <E> The type of the exception that may be thrown by the action.
     * @param action The action to be performed for each element, which accepts the index of the element and the element itself.
     * @throws IllegalArgumentException if the action is {@code null}.
     * @throws E Any exception thrown by the action.
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, next());
        }
    }
}
