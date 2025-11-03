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
import com.landawn.abacus.util.u.Nullable;
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
 *   <li>Additional utility methods like first(), last(), toList(), etc.</li>
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
 * ObjIterator<Integer> counter = ObjIterator.generate(
 *     () -> count++ < 10,  // hasNext
 *     () -> count          // next
 * );
 * }</pre>
 *
 * @param <T> the type of elements returned by this iterator
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
     * Returns an empty ObjIterator that has no elements.
     * The returned iterator's hasNext() method always returns {@code false}.
     *
     * @param <T> the type of elements (not) returned by the iterator
     * @return an empty ObjIterator
     */
    public static <T> ObjIterator<T> empty() {
        return EMPTY;
    }

    /**
     * Returns an ObjIterator containing a single element.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> single = ObjIterator.just("Hello");
     * single.next(); // Returns "Hello"
     * single.hasNext(); // Returns false
     * }</pre>
     *
     * @param <T> the type of the element
     * @param val the single element to be returned by the iterator
     * @return an ObjIterator containing exactly one element
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
     * Returns an ObjIterator over the specified array.
     * The iterator will traverse all elements in the array in order.
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
     * @return an ObjIterator over the array elements
     */
    @SafeVarargs
    public static <T> ObjIterator<T> of(final T... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Returns an ObjIterator over a portion of the specified array.
     * The iterator will traverse elements from fromIndex (inclusive) to toIndex (exclusive).
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
     * @return an ObjIterator over the specified range of array elements
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; array length, or fromIndex &gt; toIndex
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

                cursor = toIndex; // Move cursor to the end after copying.

                return output;
            }

            @Override
            public List<T> toList() {
                return N.asList((T[]) toArray());
            }
        };
    }

    /**
     * Returns an ObjIterator that wraps the specified Iterator.
     * If the Iterator is {@code null}, returns an empty ObjIterator.
     * If the Iterator is already an ObjIterator, returns it as-is.
     * 
     * <p>This method is useful for converting standard Java iterators
     * to ObjIterator to access additional functionality.</p>
     *
     * @param <T> the type of elements in the iterator
     * @param iter the Iterator to wrap
     * @return an ObjIterator wrapping the given Iterator
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
     * Returns an ObjIterator over the elements in the specified Collection.
     * If the Collection is {@code null} or empty, returns an empty ObjIterator.
     *
     * @param <T> the type of elements in the collection
     * @param iterable the Collection whose elements are to be iterated
     * @return an ObjIterator over the collection elements
     */
    public static <T> ObjIterator<T> of(final Collection<? extends T> iterable) {
        return iterable == null ? ObjIterator.empty() : of(iterable.iterator());
    }

    /**
     * Returns an ObjIterator over the elements in the specified Iterable.
     * If the Iterable is {@code null}, returns an empty ObjIterator.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjIterator<String> iter = ObjIterator.of(list);
     * }</pre>
     *
     * @param <T> the type of elements in the iterable
     * @param iterable the Iterable whose elements are to be iterated
     * @return an ObjIterator over the iterable elements
     */
    public static <T> ObjIterator<T> of(final Iterable<? extends T> iterable) {
        return iterable == null ? ObjIterator.empty() : of(iterable.iterator());
    }

    /**
     * Returns an ObjIterator that defers creation of the underlying iterator
     * until the first method call. This is useful for lazy initialization.
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
     * @param iteratorSupplier a Supplier that produces the Iterator when needed
     * @return an ObjIterator that lazily initializes using the supplier
     * @throws IllegalArgumentException if iteratorSupplier is null
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
     * Returns an infinite ObjIterator that generates elements using the provided supplier.
     * The iterator will never return {@code false} from hasNext().
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Random random = new Random();
     * ObjIterator<Integer> randomNumbers = ObjIterator.generate(random::nextInt);
     * // Generates infinite random numbers
     * }</pre>
     *
     * @param <T> the type of elements returned by this iterator
     * @param supplier a Supplier that produces the next element on each call
     * @return an infinite ObjIterator
     * @throws IllegalArgumentException if supplier is null
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
     * Returns an ObjIterator that generates elements while the hasNext condition is {@code true}.
     * This allows for finite generated sequences.
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
     * @param hasNext a BooleanSupplier that returns {@code true} if more elements should be generated
     * @param supplier a Supplier that produces the next element
     * @return an ObjIterator that generates elements conditionally
     * @throws IllegalArgumentException if hasNext or supplier is null
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
     * Returns an ObjIterator that generates elements using stateful generation.
     * The initial value is passed to predicates and functions for state-based generation.
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
     * @param init the initial state value
     * @param hasNext a Predicate that tests the state to determine if more elements exist
     * @param supplier a Function that generates the next element from the state
     * @return an ObjIterator that generates elements based on state
     * @throws IllegalArgumentException if hasNext or supplier is null
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
     * Returns an ObjIterator that generates elements using both state and the previously generated value.
     * This allows for complex stateful generation patterns.
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
     * @param init the initial state value
     * @param hasNext a BiPredicate that tests the state and previous value
     * @param supplier a BiFunction that generates the next element from state and previous value
     * @return an ObjIterator that generates elements based on state and previous values
     * @throws IllegalArgumentException if hasNext or supplier is null
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
     * Returns a new ObjIterator that skips the first n elements.
     * If n is greater than or equal to the number of remaining elements,
     * the returned iterator will be empty.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5);
     * ObjIterator<Integer> skipped = iter.skip(2);
     * // Iterates over: 3, 4, 5
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new ObjIterator that skips the first n elements
     * @throws IllegalArgumentException if n is negative
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
     * Returns a new ObjIterator that yields at most the first count elements.
     * If count is 0, returns an empty iterator.
     * If count is greater than the number of remaining elements,
     * returns all remaining elements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c", "d", "e");
     * ObjIterator<String> limited = iter.limit(3);
     * // Iterates over: "a", "b", "c"
     * }</pre>
     *
     * @param count the maximum number of elements to return
     * @return a new ObjIterator limited to count elements
     * @throws IllegalArgumentException if count is negative
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
     * Returns a new ObjIterator that skips the first offset elements and then
     * yields at most count elements.
     * 
     * <p>This is equivalent to calling skip(offset).limit(count) but may be more efficient.</p>
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
     * @return a new ObjIterator with skip and limit applied
     * @throws IllegalArgumentException if offset or count is negative
     */
    public ObjIterator<T> skipAndLimit(final long offset, final long count) {
        return Iterators.skipAndLimit(this, offset, count);
    }

    /**
     * Returns a new ObjIterator that yields only elements matching the given predicate.
     * This is a lazy operation - the predicate is applied as elements are requested.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<Integer> numbers = ObjIterator.of(1, 2, 3, 4, 5, 6);
     * ObjIterator<Integer> evens = numbers.filter(n -> n % 2 == 0);
     * // Iterates over: 2, 4, 6
     * }</pre>
     *
     * @param predicate the predicate to apply to each element
     * @return a new ObjIterator yielding only matching elements
     */
    public ObjIterator<T> filter(final Predicate<? super T> predicate) {
        return Iterators.filter(this, predicate);
    }

    /**
     * Returns a new ObjIterator that transforms each element using the provided mapping function.
     * This is a lazy operation - the function is applied as elements are requested.
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
     * @return a new ObjIterator with transformed elements
     */
    @Beta
    public <U> ObjIterator<U> map(final Function<? super T, U> mapper) {
        return Iterators.map(this, mapper);
    }

    /**
     * Returns a new ObjIterator containing only distinct elements.
     * Elements are considered distinct based on their natural equality (equals method).
     * The first occurrence of each element is retained.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "a", "c", "b");
     * ObjIterator<String> distinct = iter.distinct();
     * // Iterates over: "a", "b", "c"
     * }</pre>
     *
     * @return a new ObjIterator with distinct elements
     */
    public ObjIterator<T> distinct() {
        final Set<T> elements = new HashSet<>();
        return filter(elements::add);
    }

    /**
     * Returns a new ObjIterator containing only distinct elements based on a key extractor.
     * Elements are considered distinct based on the keys extracted by the function.
     * The first element with each distinct key is retained.
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
     * @param keyExtractor the function to extract the key for comparison
     * @return a new ObjIterator with distinct elements based on extracted keys
     */
    public ObjIterator<T> distinctBy(final Function<? super T, ?> keyExtractor) {
        final Set<Object> elements = new HashSet<>();
        return filter(e -> elements.add(keyExtractor.apply(e)));
    }

    /**
     * Returns the first element of this iterator wrapped in a {@code Nullable}.
     * If the iterator is empty, returns an empty {@code Nullable}.
     * This operation consumes the first element if present.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("first", "second");
     * Nullable<String> first = iter.first(); // Nullable.of("first")
     * 
     * ObjIterator<String> empty = ObjIterator.empty();
     * Nullable<String> none = empty.first(); // Nullable.empty()
     * }</pre>
     *
     * @return a {@code Nullable} containing the first element, or empty if no elements
     */
    public Nullable<T> first() {
        if (hasNext()) {
            return Nullable.of(next());
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Returns the first {@code non-null} element of this iterator wrapped in an Optional.
     * If no {@code non-null} element is found, returns an empty Optional.
     * This operation may consume multiple elements until a {@code non-null} element is found.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of(null, null, "found", "next");
     * Optional<String> first = iter.firstNonNull(); // Optional.of("found")
     * }</pre>
     *
     * @return an Optional containing the first {@code non-null} element, or empty if none found
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
     * Returns the last element of this iterator wrapped in a {@code Nullable}.
     * If the iterator is empty, returns an empty {@code Nullable}.
     * This operation consumes all remaining elements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4);
     * Nullable<Integer> last = iter.last(); // Nullable.of(4)
     * }</pre>
     *
     * @return a {@code Nullable} containing the last element, or empty if no elements
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
     * Returns a new ObjIterator that skips {@code null} elements.
     * Only {@code non-null} elements will be returned by the resulting iterator.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", null, "b", null, "c");
     * ObjIterator<String> nonNulls = iter.skipNulls();
     * // Iterates over: "a", "b", "c"
     * }</pre>
     *
     * @return a new ObjIterator that filters out {@code null} elements
     */
    public ObjIterator<T> skipNulls() {
        return Iterators.skipNulls(this);
    }

    /**
     * Converts the remaining elements in this iterator to an Object array.
     * This operation consumes all remaining elements.
     *
     * @return an array containing all remaining elements
     */
    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Converts the remaining elements in this iterator to an array of the specified type.
     * If the provided array is large enough, it is used directly. Otherwise, a new
     * array of the same type is allocated.
     * This operation consumes all remaining elements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * String[] array = iter.toArray(new String[0]); // {"a", "b", "c"}
     * }</pre>
     *
     * @param <A> the component type of the array
     * @param a the array into which elements are stored, if big enough
     * @return an array containing all remaining elements
     */
    public <A> A[] toArray(final A[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts the remaining elements in this iterator to a List.
     * This operation consumes all remaining elements.
     * The returned list is mutable and can be modified.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3);
     * List<Integer> list = iter.toList(); // [1, 2, 3]
     * }</pre>
     *
     * @return a List containing all remaining elements
     */
    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    /**
     * Returns a Stream containing the remaining elements of this iterator.
     * This operation creates a new Stream that will consume elements from this iterator.
     * The iterator should not be used after calling this method.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * long count = iter.stream()
     *     .filter(s -> s.length() > 0)
     *     .count(); // 3
     * }</pre>
     *
     * @return a Stream of the remaining elements
     */
    public Stream<T> stream() {
        return Stream.of(this);
    }

    /**
     * Returns a new ObjIterator where each element is paired with its index.
     * The index starts from 0 and increments for each element.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * ObjIterator<Indexed<String>> indexed = iter.indexed();
     * // Iterates over: Indexed(0, "a"), Indexed(1, "b"), Indexed(2, "c")
     * }</pre>
     *
     * @return a new ObjIterator of Indexed elements
     */
    @Beta
    public ObjIterator<Indexed<T>> indexed() {
        return indexed(0);
    }

    /**
     * Returns a new ObjIterator where each element is paired with its index,
     * starting from the specified start index.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * ObjIterator<Indexed<String>> indexed = iter.indexed(10);
     * // Iterates over: Indexed(10, "a"), Indexed(11, "b"), Indexed(12, "c")
     * }</pre>
     *
     * @param startIndex the starting index value
     * @return a new ObjIterator of Indexed elements
     * @throws IllegalArgumentException if startIndex is negative
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
     * @param <E> the type of exception that may be thrown
     * @param action the action to perform for each element
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
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
     * @param <E> the type of exception that may be thrown
     * @param action the action to perform for each element and its index
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }
            action.accept(idx++, next());
        }
    }
}
