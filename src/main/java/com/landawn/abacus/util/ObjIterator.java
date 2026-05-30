/*
 * Copyright (c) 2017, Haiyang Li.
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
import com.landawn.abacus.util.stream.Stream;

/**
 * An abstract iterator implementation that provides additional functional operations
 * beyond the standard Iterator interface. This class extends {@link ImmutableIterator}
 * and provides methods for transformation, filtering, and collection operations.
 *
 * <p>ObjIterator is designed to be a lightweight, functional alternative to streams
 * for simple iteration scenarios. It provides lazy evaluation and can be more
 * efficient than streams for simple operations.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Lazy evaluation of operations like map, filter, skip, and limit</li>
 *   <li>Convenient factory methods for creating iterators from various sources</li>
 *   <li>Additional utility methods like toList(), toArray(), etc.</li>
 *   <li>Support for indexed iteration</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create an iterator from an array
 * ObjIterator<String> iter = ObjIterator.of("a", "b", "c", "d");
 *
 * // Transform and filter
 * List<Integer> lengths = iter
 *     .map(String::length)
 *     .filter(len -> len > 0)
 *     .toList();
 *
 * // Generate values
 * int[] count = {0};
 * ObjIterator<Integer> counter = ObjIterator.generate(
 *     () -> count[0] < 10,  // hasNext
 *     () -> count[0]++      // next
 * );
 * }</pre>
 *
 * @param <T> the type of elements returned by this iterator
 * @see ImmutableIterator
 * @see ObjListIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 * @see Throwables.Iterator
 */
@SuppressWarnings({ "java:S6548" })
public abstract class ObjIterator<T> extends ImmutableIterator<T> {

    /**
     * Protected constructor for subclasses.
     */
    protected ObjIterator() {
    }

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
     * Returns an empty {@code ObjIterator} that has no elements.
     * The returned iterator's {@code hasNext()} method always returns {@code false},
     * and {@code next()} always throws {@link NoSuchElementException}. A shared
     * singleton instance is returned.
     *
     * @param <T> the type of elements (not) returned by the iterator
     * @return an empty {@code ObjIterator}
     */
    public static <T> ObjIterator<T> empty() {
        return EMPTY;
    }

    /**
     * Returns an {@code ObjIterator} containing exactly one element.
     * The given value (which may be {@code null}) is returned by the first call
     * to {@code next()}; thereafter the iterator is exhausted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> single = ObjIterator.just("Hello");
     * single.next();      // Returns "Hello"
     * single.hasNext();   // Returns false
     * }</pre>
     *
     * @param <T> the type of the element
     * @param val the single element to be returned by the iterator
     * @return an {@code ObjIterator} containing exactly one element
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
     * Returns an {@code ObjIterator} over the specified array.
     * The iterator traverses all elements of the array in order.
     * If the array is {@code null} or empty, an empty iterator is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = {"one", "two", "three"};
     * ObjIterator<String> iter = ObjIterator.of(array);
     * while (iter.hasNext()) {
     *     System.out.println(iter.next());
     * }
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array whose elements are to be iterated
     * @return an {@code ObjIterator} over the array elements
     */
    @SafeVarargs
    public static <T> ObjIterator<T> of(final T... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Returns an {@code ObjIterator} over a portion of the specified array.
     * The iterator traverses elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive). If the array is {@code null}/empty or the
     * range is empty, an empty iterator is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] numbers = {1, 2, 3, 4, 5};
     * ObjIterator<Integer> iter = ObjIterator.of(numbers, 1, 4);
     * // Iterates over: 2, 3, 4
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array whose elements are to be iterated
     * @param fromIndex the index of the first element to iterate (inclusive)
     * @param toIndex the index after the last element to iterate (exclusive)
     * @return an {@code ObjIterator} over the specified range of array elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
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
                final int remaining = toIndex - cursor;

                if (output.length < remaining) {
                    output = N.copyOf(output, remaining);
                } else if (output.length > remaining) {
                    // Collection.toArray(T[]) contract: when the array is larger than the
                    // collection, set array[size()] to null as the end-of-data sentinel.
                    output[remaining] = null;
                }

                N.copy(a, cursor, output, 0, remaining);

                cursor = toIndex; // Move cursor to the end after copying.

                return output;
            }

            @Override
            public List<T> toList() {
                return N.toList((T[]) toArray());
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that wraps the specified {@code Iterator}.
     * If the iterator is {@code null}, an empty {@code ObjIterator} is returned.
     * If the iterator is already an {@code ObjIterator}, it is returned as-is
     * (no wrapping is performed).
     *
     * <p>This method is useful for converting a standard Java {@code Iterator}
     * to an {@code ObjIterator} to access the additional functional operations.</p>
     *
     * @param <T> the type of elements in the iterator
     * @param iter the {@code Iterator} to wrap
     * @return an {@code ObjIterator} wrapping the given iterator
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
     * Returns an {@code ObjIterator} over the elements in the specified {@code Collection}.
     * If the collection is {@code null}, an empty {@code ObjIterator} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("apple", "banana", "cherry");
     * ObjIterator<String> iter = ObjIterator.of(list);
     * iter.toList();   // Returns ["apple", "banana", "cherry"]
     *
     * Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
     * ObjIterator<Integer> setIter = ObjIterator.of(set);
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param iterable the {@code Collection} whose elements are to be iterated
     * @return an {@code ObjIterator} over the collection elements
     */
    public static <T> ObjIterator<T> of(final Collection<? extends T> iterable) {
        return iterable == null ? ObjIterator.empty() : of(iterable.iterator());
    }

    /**
     * Returns an {@code ObjIterator} over the elements in the specified {@code Iterable}.
     * If the iterable is {@code null}, an empty {@code ObjIterator} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjIterator<String> iter = ObjIterator.of(list);
     * }</pre>
     *
     * @param <T> the type of elements in the iterable
     * @param iterable the {@code Iterable} whose elements are to be iterated
     * @return an {@code ObjIterator} over the iterable elements
     */
    public static <T> ObjIterator<T> of(final Iterable<? extends T> iterable) {
        return iterable == null ? ObjIterator.empty() : of(iterable.iterator());
    }

    /**
     * Returns an {@code ObjIterator} that defers creation of the underlying
     * iterator until the first call to {@code hasNext()} or {@code next()}.
     * This is useful for lazy initialization. If the supplier returns
     * {@code null}, an empty iterator is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> lazy = ObjIterator.defer(() -> {
     *     System.out.println("Creating iterator");
     *     return Arrays.asList("a", "b", "c").iterator();
     * });
     * // "Creating iterator" is printed only when hasNext() or next() is called
     * }</pre>
     *
     * @param <T> the type of elements returned by this iterator
     * @param iteratorSupplier a {@code Supplier} that produces the {@code Iterator} when needed
     * @return an {@code ObjIterator} that lazily initializes using the supplier
     * @throws IllegalArgumentException if {@code iteratorSupplier} is {@code null}
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

                    if (iter == null) {
                        iter = ObjIterator.empty();
                    }
                }
            }
        };
    }

    /**
     * Returns an infinite {@code ObjIterator} that generates elements using the
     * provided supplier. The iterator's {@code hasNext()} always returns
     * {@code true}, and {@code next()} returns {@code supplier.get()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Random random = new Random();
     * ObjIterator<Integer> randomNumbers = ObjIterator.generate(random::nextInt);
     * // Generates infinite random numbers
     * }</pre>
     *
     * @param <T> the type of elements returned by this iterator
     * @param supplier a {@code Supplier} that produces the next element on each call
     * @return an infinite {@code ObjIterator}
     * @throws IllegalArgumentException if {@code supplier} is {@code null}
     * @see #generate(BooleanSupplier, Supplier)
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
     * Returns an {@code ObjIterator} that generates elements while the
     * {@code hasNext} condition evaluates to {@code true}. This allows for
     * finite generated sequences. Calling {@code next()} when {@code hasNext}
     * is {@code false} throws {@link NoSuchElementException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] counter = {0};
     * ObjIterator<Integer> counting = ObjIterator.generate(
     *     () -> counter[0] < 10,           // hasNext
     *     () -> counter[0]++               // supplier
     * );
     * // Generates: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9
     * }</pre>
     *
     * @param <T> the type of elements returned by this iterator
     * @param hasNext a {@code BooleanSupplier} that returns {@code true} if more elements should be generated
     * @param supplier a {@code Supplier} that produces the next element
     * @return an {@code ObjIterator} that generates elements conditionally
     * @throws IllegalArgumentException if {@code hasNext} or {@code supplier} is {@code null}
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

    /**
     * Returns an {@code ObjIterator} that generates elements using stateful
     * generation. The {@code init} state value is passed to the {@code hasNext}
     * predicate and the {@code supplier} function on each step. Calling
     * {@code next()} when {@code hasNext} returns {@code false} throws
     * {@link NoSuchElementException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate Fibonacci numbers
     * ObjIterator<Integer> fib = ObjIterator.generate(
     *     new int[] {0, 1},                     // initial state
     *     state -> state[1] < 100,             // continue while < 100
     *     state -> {                           // generate next
     *         int next = state[0] + state[1];
     *         state[0] = state[1];
     *         state[1] = next;
     *         return state[0];
     *     }
     * );
     * }</pre>
     *
     * @param <T> the type of elements returned by this iterator
     * @param <U> the type of the state/seed value
     * @param init the initial state value (may be {@code null})
     * @param hasNext a {@code Predicate} that tests the state to determine if more elements exist
     * @param supplier a {@code Function} that generates the next element from the state
     * @return an {@code ObjIterator} that generates elements based on state
     * @throws IllegalArgumentException if {@code hasNext} or {@code supplier} is {@code null}
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
     * Returns an {@code ObjIterator} that generates elements using both the
     * state value and the previously generated element. This allows for
     * complex stateful generation patterns. The previous value is {@code null}
     * before the first element is generated. Calling {@code next()} when
     * {@code hasNext} returns {@code false} throws {@link NoSuchElementException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate a sequence where each element depends on the previous
     * ObjIterator<String> iter = ObjIterator.generate(
     *     "",                                      // initial state
     *     (state, prev) -> prev == null || prev.length() < 5,  // continue condition
     *     (state, prev) -> prev == null ? "a" : prev + "a"     // generation
     * );
     * // Generates: "a", "aa", "aaa", "aaaa", "aaaaa"
     * }</pre>
     *
     * @param <T> the type of elements returned by this iterator
     * @param <U> the type of the state/seed value
     * @param init the initial state value (may be {@code null})
     * @param hasNext a {@code BiPredicate} that tests the state and previously generated value
     * @param supplier a {@code BiFunction} that generates the next element from the state and previous value
     * @return an {@code ObjIterator} that generates elements based on state and previous values
     * @throws IllegalArgumentException if {@code hasNext} or {@code supplier} is {@code null}
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
     * Returns a new {@code ObjIterator} that skips the first {@code n} elements
     * of this iterator. If {@code n} is greater than or equal to the number of
     * remaining elements, the returned iterator will be empty. The skipping is
     * performed lazily on the first call to {@code hasNext()} or {@code next()}.
     * If {@code n == 0}, this iterator is returned unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5);
     * ObjIterator<Integer> skipped = iter.skip(2);
     * // Iterates over: 3, 4, 5
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new {@code ObjIterator} that skips the first {@code n} elements
     * @throws IllegalArgumentException if {@code n} is negative
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
     * Returns a new {@code ObjIterator} that yields at most the first
     * {@code count} elements of this iterator. If {@code count} is {@code 0},
     * an empty iterator is returned. If {@code count} is greater than the
     * number of remaining elements, all remaining elements are returned. This
     * is a lazy operation; elements are consumed from this iterator only as
     * requested.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c", "d", "e");
     * ObjIterator<String> limited = iter.limit(3);
     * // Iterates over: "a", "b", "c"
     * }</pre>
     *
     * @param count the maximum number of elements to return
     * @return a new {@code ObjIterator} limited to {@code count} elements
     * @throws IllegalArgumentException if {@code count} is negative
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
     * Returns a new {@code ObjIterator} that skips the first {@code offset}
     * elements and then yields at most {@code count} elements.
     *
     * <p>This is equivalent to {@code skip(offset).limit(count)} but may be more
     * efficient.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5, 6, 7);
     * ObjIterator<Integer> sliced = iter.skipAndLimit(2, 3);
     * // Iterates over: 3, 4, 5
     * }</pre>
     *
     * @param offset the number of elements to skip
     * @param count the maximum number of elements to return after skipping
     * @return a new {@code ObjIterator} with skip and limit applied
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative
     * @see #skip(long)
     * @see #limit(long)
     */
    public ObjIterator<T> skipAndLimit(final long offset, final long count) {
        return Iterators.skipAndLimit(this, offset, count);
    }

    /**
     * Returns a new {@code ObjIterator} that yields only the elements matching
     * the given predicate. This is a lazy operation; the predicate is applied
     * to elements as they are requested.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<Integer> numbers = ObjIterator.of(1, 2, 3, 4, 5, 6);
     * ObjIterator<Integer> evens = numbers.filter(n -> n % 2 == 0);
     * // Iterates over: 2, 4, 6
     * }</pre>
     *
     * @param predicate the predicate to apply to each element
     * @return a new {@code ObjIterator} yielding only the matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @see Iterators#filter(Iterator, Predicate)
     */
    public ObjIterator<T> filter(final Predicate<? super T> predicate) {
        return Iterators.filter(this, predicate);
    }

    /**
     * Returns a new {@code ObjIterator} that transforms each element using the
     * provided mapping function. This is a lazy operation; the function is
     * applied to elements as they are requested.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> words = ObjIterator.of("hello", "world");
     * ObjIterator<Integer> lengths = words.map(String::length);
     * // Iterates over: 5, 5
     * }</pre>
     *
     * @param <U> the type of elements in the returned iterator
     * @param mapper the function to apply to each element
     * @return a new {@code ObjIterator} with transformed elements
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @see Iterators#map(Iterator, Function)
     */
    @Beta
    public <U> ObjIterator<U> map(final Function<? super T, U> mapper) {
        return Iterators.map(this, mapper);
    }

    /**
     * Returns a new {@code ObjIterator} containing only the distinct elements
     * of this iterator. Elements are compared using {@link Object#equals(Object)}
     * and {@link Object#hashCode()}; the first occurrence of each element is
     * retained and subsequent duplicates are skipped. This is a lazy operation,
     * but seen elements are accumulated in memory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "a", "c", "b");
     * ObjIterator<String> distinct = iter.distinct();
     * // Iterates over: "a", "b", "c"
     * }</pre>
     *
     * @return a new {@code ObjIterator} with distinct elements
     * @see #distinctBy(Function)
     */
    public ObjIterator<T> distinct() {
        final Set<T> elements = new HashSet<>();
        return filter(elements::add);
    }

    /**
     * Returns a new {@code ObjIterator} containing only the elements whose
     * extracted keys are distinct. The key for each element is produced by
     * {@code keyExtractor}; keys are compared using {@link Object#equals(Object)}
     * and {@link Object#hashCode()}. The first element with each distinct key is
     * retained. This is a lazy operation, but seen keys are accumulated in memory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String name;
     *     int age;
     *     // constructor, getters...
     * }
     *
     * ObjIterator<Person> people = ObjIterator.of(
     *     new Person("Alice", 30),
     *     new Person("Bob", 25),
     *     new Person("Alice", 35)
     * );
     * ObjIterator<Person> uniqueNames = people.distinctBy(Person::getName);
     * // Iterates over: Person("Alice", 30), Person("Bob", 25)
     * }</pre>
     *
     * @param keyExtractor the function to extract the comparison key from each element
     * @return a new {@code ObjIterator} with elements distinct by the extracted key
     * @see #distinct()
     */
    public ObjIterator<T> distinctBy(final Function<? super T, ?> keyExtractor) {
        final Set<Object> elements = new HashSet<>();
        return filter(e -> elements.add(keyExtractor.apply(e)));
    }

    /**
     * Returns the first {@code non-null} element of this iterator wrapped in an
     * {@link u.Optional}. If no {@code non-null} element is found, an empty
     * {@code Optional} is returned. This is a terminal operation that consumes
     * elements until a {@code non-null} element is found (or the iterator is
     * exhausted), so the {@code null} elements skipped before it are discarded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of(null, null, "found", "next");
     * Optional<String> first = iter.firstNonNull();   // Optional.of("found")
     * }</pre>
     *
     * @return an {@code Optional} containing the first {@code non-null} element,
     *         or an empty {@code Optional} if none is found
     * @deprecated This method may leave the iterator in a partially consumed state;
     *         elements consumed before the first {@code non-null} element are discarded,
     *         which can cause inconsistent results. Use {@link #skipNulls()} instead.
     */
    @Deprecated
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
     * Returns a new {@code ObjIterator} that skips {@code null} elements.
     * Only {@code non-null} elements are returned by the resulting iterator.
     * This is a lazy operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", null, "b", null, "c");
     * ObjIterator<String> nonNulls = iter.skipNulls();
     * // Iterates over: "a", "b", "c"
     * }</pre>
     *
     * @return a new {@code ObjIterator} that filters out {@code null} elements
     * @see Iterators#skipNulls(Iterator)
     */
    public ObjIterator<T> skipNulls() {
        return Iterators.skipNulls(this);
    }

    /**
     * Converts the remaining elements of this iterator to an {@code Object[]}.
     * This is a terminal operation that consumes all remaining elements.
     * An empty array is returned if no elements remain.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("x", "y", "z");
     * Object[] array = iter.toArray();   // Object[]{"x", "y", "z"}
     *
     * ObjIterator<Integer> numbers = ObjIterator.of(10, 20, 30);
     * Object[] numArray = numbers.toArray();   // Object[]{10, 20, 30}
     * }</pre>
     *
     * @return an {@code Object[]} containing all remaining elements
     * @see #toArray(Object[])
     * @see #toList()
     */
    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Converts the remaining elements of this iterator to an array of the
     * specified type. If the provided array is large enough, it is used
     * directly (following the {@link java.util.Collection#toArray(Object[])}
     * contract); otherwise a new array of the same runtime type is allocated.
     * This is a terminal operation that consumes all remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * String[] array = iter.toArray(new String[0]);   // {"a", "b", "c"}
     * }</pre>
     *
     * @param <A> the component type of the array
     * @param a the array into which the elements are stored, if it is big enough;
     *          otherwise a new array of the same runtime type is allocated
     * @return an array containing all remaining elements
     * @throws NullPointerException if {@code a} is {@code null}
     */
    public <A> A[] toArray(final A[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts the remaining elements of this iterator to a {@code List}.
     * This is a terminal operation that consumes all remaining elements.
     * The returned list is a new mutable {@link ArrayList}; an empty list is
     * returned if no elements remain.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3);
     * List<Integer> list = iter.toList();   // [1, 2, 3]
     * }</pre>
     *
     * @return a mutable {@code List} containing all remaining elements
     * @see #toArray()
     */
    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    /**
     * Returns a {@link Stream} backed by the remaining elements of this
     * iterator. The stream consumes elements lazily from this iterator as it is
     * traversed; this iterator should not be used directly after this method is
     * called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * long count = iter.stream()
     *     .filter(s -> s.length() > 0)
     *     .count();   // 3
     * }</pre>
     *
     * @return a {@code Stream} of the remaining elements
     */
    public Stream<T> stream() {
        return Stream.of(this);
    }

    /**
     * Returns a new {@code ObjIterator} where each element is paired with its
     * index, starting from {@code 0} and incrementing by one for each element.
     * This is a lazy operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * ObjIterator<Indexed<String>> indexed = iter.indexed();
     * // Iterates over: Indexed(0, "a"), Indexed(1, "b"), Indexed(2, "c")
     * }</pre>
     *
     * @return a new {@code ObjIterator} of {@link Indexed} elements
     * @see #indexed(long)
     */
    @Beta
    public ObjIterator<Indexed<T>> indexed() {
        return indexed(0);
    }

    /**
     * Returns a new {@code ObjIterator} where each element is paired with its
     * index, with the index of the first element being {@code startIndex} and
     * incrementing by one for each subsequent element. This is a lazy operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * ObjIterator<Indexed<String>> indexed = iter.indexed(10);
     * // Iterates over: Indexed(10, "a"), Indexed(11, "b"), Indexed(12, "c")
     * }</pre>
     *
     * @param startIndex the index to assign to the first element
     * @return a new {@code ObjIterator} of {@link Indexed} elements
     * @throws IllegalArgumentException if {@code startIndex} is negative
     * @see #indexed()
     */
    @Beta
    public ObjIterator<Indexed<T>> indexed(final long startIndex) {
        N.checkArgNotNegative(startIndex, cs.startIndex);

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
     * Performs the given action for each remaining element in this iterator.
     * This is a terminal operation that consumes all remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * iter.foreachRemaining(System.out::println);
     * // Prints:
     * // a
     * // b
     * // c
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to perform for each remaining element
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     * @see #foreachIndexed(Throwables.IntObjConsumer)
     */
    public <E extends Exception> void foreachRemaining(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(next());
        }
    }

    /**
     * Performs the given action for each remaining element, providing both the element
     * and its index to the action. The index starts from 0.
     * This is a terminal operation that consumes all remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * iter.foreachIndexed((index, value) ->
     *     System.out.println(index + ": " + value)
     * );
     * // Prints:
     * // 0: a
     * // 1: b
     * // 2: c
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to perform for each element and its index
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IllegalStateException if the iterator yields more than
     *         {@link Integer#MAX_VALUE} elements (index overflow)
     * @throws E if the action throws an exception
     * @see #foreachRemaining(Throwables.Consumer)
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }

            action.accept(idx, next());

            idx++;
        }
    }
}
