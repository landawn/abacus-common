/*
 * Copyright (c) 2018, Haiyang Li.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * The BiIterator class is an abstract class that extends ImmutableIterator.
 * It represents an iterator over a pair of values of type A and B.
 * This class provides a blueprint for classes that need to implement a bidirectional iterator.
 *
 * @param <A> the first type of elements returned by this iterator
 * @param <B> the second type of elements returned by this iterator
 *
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 */
@SuppressWarnings({ "java:S6548" })
public abstract class BiIterator<A, B> extends ImmutableIterator<Pair<A, B>> {

    /**
     * Constructs a new BiIterator.
     */
    protected BiIterator() {
    }

    /**
     * A singleton empty BiIterator instance that contains no elements.
     * This iterator's hasNext() always returns false, and any attempt to retrieve elements throws NoSuchElementException.
     *
     * @see #empty()
     */
    @SuppressWarnings("rawtypes")
    private static final BiIterator EMPTY = new BiIterator() {
        /**
         * Constructs an empty BiIterator.
         */
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        protected void next(final Throwables.BiConsumer action) throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public void forEachRemaining(final BiConsumer action) {
            // It's empty. Nothing to do.
        }

        @Override
        public void foreachRemaining(final Throwables.BiConsumer action) {
            // It's empty. Nothing to do.
        }

        @Override
        public ObjIterator map(final BiFunction mapper) {
            return ObjIterator.empty();
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Throwables.BiConsumer DO_NOTHING = (a, b) -> {
        // do nothing;
    };

    /**
     * Returns an empty {@code BiIterator} instance.
     * The returned iterator has no elements and all operations that expect elements will throw {@code NoSuchElementException}.
     *
     * <p>This is a singleton instance that is reused for all empty BiIterator requests,
     * making it efficient for representing empty iterations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> empty = BiIterator.empty();
     * assertFalse(empty.hasNext());   // true - no elements
     * }</pre>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @return an empty BiIterator instance that contains no elements
     */
    public static <A, B> BiIterator<A, B> empty() {
        return EMPTY;
    }

    /**
     * Creates a {@code BiIterator} from the given map's entries.
     * The iterator will yield pairs of keys and values from the map.
     * If the map is {@code null} or empty, returns an empty {@code BiIterator}.
     *
     * <p>The iteration order depends on the map's implementation. For example,
     * a {@code HashMap} provides no iteration order guarantees, while a {@code LinkedHashMap}
     * maintains insertion order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2);
     * BiIterator<String, Integer> iter = BiIterator.of(map);
     * iter.forEachRemaining((key, value) -> System.out.println(key + "=" + value));
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map to create the BiIterator from, may be {@code null}
     * @return a BiIterator over the entries of the map, or an empty BiIterator if the map is {@code null} or empty
     */
    public static <K, V> BiIterator<K, V> of(final Map<K, V> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.entrySet().iterator());
    }

    /**
     * Creates a {@code BiIterator} from an iterator of map entries.
     * The iterator will yield pairs of keys and values extracted from each {@code Map.Entry}.
     * If the iterator is {@code null}, returns an empty {@code BiIterator}.
     *
     * <p>This method is useful when you already have an iterator over map entries
     * and want to process keys and values separately without creating intermediate {@code Pair} objects.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("a", 1, "b", 2);
     * Iterator<Map.Entry<String, Integer>> entryIter = map.entrySet().iterator();
     * BiIterator<String, Integer> iter = BiIterator.of(entryIter);
     * }</pre>
     *
     * @param <K> the type of keys in the map entries
     * @param <V> the type of values in the map entries
     * @param iter the iterator of map entries to create the BiIterator from, may be {@code null}
     * @return a BiIterator over the entries of the iterator, or an empty BiIterator if the iterator is {@code null}
     */
    public static <K, V> BiIterator<K, V> of(final Iterator<Map.Entry<K, V>> iter) {
        if (iter == null) {
            return empty();
        }

        return new BiIterator<>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Pair<K, V> next() {
                return Pair.create(iter.next());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super K, ? super V, E> action) throws NoSuchElementException, E {
                final Map.Entry<K, V> entry = iter.next();

                action.accept(entry.getKey(), entry.getValue());
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super K, ? super V> action) {
                Map.Entry<K, V> entry = null;

                while (iter.hasNext()) {
                    entry = iter.next();
                    action.accept(entry.getKey(), entry.getValue());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super K, ? super V, E> action) throws E {
                Map.Entry<K, V> entry = null;

                while (iter.hasNext()) {
                    entry = iter.next();
                    action.accept(entry.getKey(), entry.getValue());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super K, ? super V, ? extends R> mapper) {
                return new ObjIterator<>() {
                    private Map.Entry<K, V> entry = null;

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public R next() {
                        entry = iter.next();

                        return mapper.apply(entry.getKey(), entry.getValue());
                    }
                };
            }
        };
    }

    /**
     * Generates an infinite {@code BiIterator} with elements produced by the given output consumer.
     * The output consumer is invoked for each iteration to populate a {@code Pair} with the next values.
     *
     * <p><strong>Warning:</strong> This creates an infinite iterator. Always use with operations
     * that limit the iteration (e.g., {@link #limit(long)}, {@link #filter(BiPredicate)}) to avoid infinite loops.</p>
     *
     * <p>The output consumer receives a mutable {@code Pair} object that should be populated
     * with the next pair of values using {@code pair.set(a, b)} or {@code pair.setLeft(a)} and {@code pair.setRight(b)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * BiIterator<Integer, String> iter = BiIterator.generate(pair -> {
     *     int n = counter.incrementAndGet();
     *     pair.set(n, "value" + n);
     * }).limit(5);
     * }</pre>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param output a Consumer that populates a Pair with the next values on each iteration, must not be {@code null}
     * @return an infinite BiIterator that uses the output Consumer to generate its elements
     * @throws IllegalArgumentException if output is {@code null}
     * @see #generate(BooleanSupplier, Consumer)
     */
    public static <A, B> BiIterator<A, B> generate(final Consumer<Pair<A, B>> output) {
        return generate(com.landawn.abacus.util.function.BooleanSupplier.TRUE, output);
    }

    /**
     * Generates a {@code BiIterator} with elements produced by the output consumer while the hasNext supplier returns {@code true}.
     * The hasNext supplier controls when the iteration should stop, and the output consumer populates each pair of values.
     *
     * <p>This method provides full control over iteration termination and element generation.
     * The hasNext supplier is called before each element is produced to determine if iteration should continue.</p>
     *
     * <p>The output consumer receives a mutable {@code Pair} object that should be populated
     * with the next pair of values using {@code pair.set(a, b)} or {@code pair.setLeft(a)} and {@code pair.setRight(b)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * BiIterator<Integer, String> iter = BiIterator.generate(
     *     () -> counter.get() < 5,
     *     pair -> {
     *         int n = counter.incrementAndGet();
     *         pair.set(n, "value" + n);
     *     }
     * );
     * }</pre>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param hasNext a BooleanSupplier that returns {@code true} if the iterator should have more elements, must not be {@code null}
     * @param output a Consumer that populates a Pair with the next values on each iteration, must not be {@code null}
     * @return a BiIterator that uses the hasNext supplier and output consumer to generate its elements
     * @throws IllegalArgumentException if hasNext or output is {@code null}
     */
    public static <A, B> BiIterator<A, B> generate(final BooleanSupplier hasNext, final Consumer<Pair<A, B>> output) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(output);

        return new BiIterator<>() {
            private final Pair<A, B> tmp = new Pair<>();
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = hasNext.getAsBoolean();
                }

                return hasNextFlag;
            }

            @Override
            public Pair<A, B> next() {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                output.accept(tmp);

                return Pair.of(tmp.left(), tmp.right());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                output.accept(tmp);

                action.accept(tmp.left(), tmp.right());
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                while (hasNextFlag || hasNext.getAsBoolean()) {
                    hasNextFlag = false; // Reset for the next call

                    output.accept(tmp);

                    action.accept(tmp.left(), tmp.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {

                while (hasNextFlag || hasNext.getAsBoolean()) {
                    hasNextFlag = false; // Reset for the next call

                    output.accept(tmp);

                    action.accept(tmp.left(), tmp.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            hasNextFlag = hasNext.getAsBoolean();
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // Reset for the next call

                        output.accept(tmp);

                        return mapper.apply(tmp.left(), tmp.right());
                    }
                };
            }
        };
    }

    /**
     * Generates a {@code BiIterator} over an index range, with elements produced by the output consumer for each index.
     * The iterator will produce {@code toIndex - fromIndex} elements, with indices ranging from {@code fromIndex} (inclusive)
     * to {@code toIndex} (exclusive).
     *
     * <p>This method is useful for generating pairs based on an index, such as creating coordinate pairs,
     * index-value pairs, or any other index-dependent data.</p>
     *
     * <p>The output consumer receives the current index and a mutable {@code Pair} object that should be
     * populated with the values corresponding to that index.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate pairs of (index, square of index)
     * BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> {
     *     pair.set(i, i * i);
     * });
     * // Produces: (0,0), (1,1), (2,4), (3,9), (4,16)
     * }</pre>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param fromIndex the starting index (inclusive), must be non-negative and not greater than {@code toIndex}
     * @param toIndex the ending index (exclusive), must be non-negative
     * @param output an IntObjConsumer that accepts an index and a Pair to populate with values, must not be {@code null}
     * @return a BiIterator that generates elements for each index in the range [fromIndex, toIndex)
     * @throws IllegalArgumentException if fromIndex is greater than toIndex, or if output is {@code null}
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is negative or exceeds {@code Integer.MAX_VALUE}
     */
    public static <A, B> BiIterator<A, B> generate(final int fromIndex, final int toIndex, final IntObjConsumer<Pair<A, B>> output)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
        N.checkArgNotNull(output);

        return new BiIterator<>() {
            private final MutableInt cursor = MutableInt.of(fromIndex);
            private final Pair<A, B> tmp = new Pair<>();

            @Override
            public boolean hasNext() {
                return cursor.value() < toIndex;
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                return Pair.of(tmp.left(), tmp.right());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                action.accept(tmp.left(), tmp.right());
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left(), tmp.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left(), tmp.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return cursor.value() < toIndex;
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        output.accept(cursor.getAndIncrement(), tmp);

                        return mapper.apply(tmp.left(), tmp.right());
                    }
                };
            }
        };
    }

    /**
     * Zips two arrays into a {@code BiIterator} by pairing elements at corresponding indices.
     * The resulting iterator will produce pairs from matching positions in both arrays.
     * If the arrays have different lengths, iteration stops when the shorter array is exhausted.
     * If either array is {@code null}, returns an empty {@code BiIterator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * BiIterator<String, Integer> iter = BiIterator.zip(names, ages);
     * iter.forEachRemaining((name, age) -> System.out.println(name + ": " + age));
     * // Output:
     * // Alice: 25
     * // Bob: 30
     * // Charlie: 35
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param a the first array, may be {@code null}
     * @param b the second array, may be {@code null}
     * @return a BiIterator that produces pairs of elements at matching indices from both arrays
     */
    public static <A, B> BiIterator<A, B> zip(final A[] a, final B[] b) {
        return zip(Array.asList(a), Array.asList(b));
    }

    /**
     * Zips two arrays into a {@code BiIterator} with default values for missing elements.
     * The resulting iterator continues until both arrays are exhausted, using default values
     * when one array is shorter than the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob"};
     * Integer[] ages = {25, 30, 35, 40};
     * BiIterator<String, Integer> iter = BiIterator.zip(names, ages, "Unknown", 0);
     * // Produces: (Alice,25), (Bob,30), (Unknown,35), (Unknown,40)
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param a the first array, may be {@code null}
     * @param b the second array, may be {@code null}
     * @param valueForNoneA the default value used when the first array is shorter
     * @param valueForNoneB the default value used when the second array is shorter
     * @return a BiIterator that produces pairs until both arrays are exhausted
     */
    public static <A, B> BiIterator<A, B> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB) {
        return zip(Array.asList(a), Array.asList(b), valueForNoneA, valueForNoneB);
    }

    /**
     * Zips two iterables into a {@code BiIterator} by pairing elements at corresponding positions.
     * Iteration stops when the shorter iterable is exhausted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = List.of("Alice", "Bob", "Charlie");
     * List<Integer> ages = List.of(25, 30, 35);
     * BiIterator<String, Integer> iter = BiIterator.zip(names, ages);
     * iter.forEachRemaining((name, age) -> System.out.println(name + ": " + age));
     * // Output:
     * // Alice: 25
     * // Bob: 30
     * // Charlie: 35
     * }</pre>
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param a the first iterable, may be {@code null}
     * @param b the second iterable, may be {@code null}
     * @return a BiIterator over pairs of elements, or empty if either iterable is {@code null}
     */
    public static <A, B> BiIterator<A, B> zip(final Iterable<A> a, final Iterable<B> b) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator());
    }

    /**
     * Zips two iterables into a {@code BiIterator} with default values for missing elements.
     * Iteration continues until both iterables are exhausted, using defaults for the shorter one.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = List.of("Alice", "Bob");
     * List<Integer> ages = List.of(25, 30, 35, 40);
     * BiIterator<String, Integer> iter = BiIterator.zip(names, ages, "Unknown", 0);
     * iter.forEachRemaining((name, age) -> System.out.println(name + ": " + age));
     * // Output:
     * // Alice: 25
     * // Bob: 30
     * // Unknown: 35
     * // Unknown: 40
     * }</pre>
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param a the first iterable, may be {@code null}
     * @param b the second iterable, may be {@code null}
     * @param valueForNoneA the default value when the first iterable is shorter
     * @param valueForNoneB the default value when the second iterable is shorter
     * @return a BiIterator that produces pairs until both iterables are exhausted
     */
    public static <A, B> BiIterator<A, B> zip(final Iterable<A> a, final Iterable<B> b, final A valueForNoneA, final B valueForNoneB) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), valueForNoneA, valueForNoneB);
    }

    /**
     * Zips two iterators into a {@code BiIterator} by pairing elements at corresponding positions.
     * Iteration stops when the shorter iterator is exhausted. Elements from the longer iterator
     * beyond that point are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> names = List.of("Alice", "Bob").iterator();
     * Iterator<Integer> ages = List.of(25, 30, 35).iterator();
     * BiIterator<String, Integer> iter = BiIterator.zip(names, ages);
     * // Produces: (Alice,25), (Bob,30) - third age is ignored
     * }</pre>
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param iterA the first iterator, may be {@code null}
     * @param iterB the second iterator, may be {@code null}
     * @return a BiIterator over pairs of elements, or empty if either iterator is {@code null}
     */
    public static <A, B> BiIterator<A, B> zip(final Iterator<A> iterA, final Iterator<B> iterB) {
        if (iterA == null || iterB == null) {
            return empty();
        }

        return new BiIterator<>() {
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = iterA.hasNext() && iterB.hasNext();
                }

                return hasNextFlag;
            }

            @Override
            public Pair<A, B> next() {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Pair.of(iterA.next(), iterB.next());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                action.accept(iterA.next(), iterB.next());
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                while (hasNextFlag || (iterA.hasNext() && iterB.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iterA.next(), iterB.next());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNextFlag || (iterA.hasNext() && iterB.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iterA.next(), iterB.next());
                }

            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            hasNextFlag = iterA.hasNext() && iterB.hasNext();
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // Reset for the next call

                        return mapper.apply(iterA.next(), iterB.next());
                    }
                };
            }
        };
    }

    /**
     * Zips two iterators into a {@code BiIterator} with default values for missing elements.
     * Iteration continues until both iterators are exhausted, using the specified default values
     * when one iterator is shorter than the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> names = List.of("Alice", "Bob").iterator();
     * Iterator<Integer> ages = List.of(25, 30, 35).iterator();
     * BiIterator<String, Integer> iter = BiIterator.zip(names, ages, "Unknown", 0);
     * // Produces: (Alice,25), (Bob,30), (Unknown,35)
     * }</pre>
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param iterA the first iterator, may be {@code null}
     * @param iterB the second iterator, may be {@code null}
     * @param valueForNoneA the default value used when the first iterator is exhausted
     * @param valueForNoneB the default value used when the second iterator is exhausted
     * @return a BiIterator that produces pairs until both iterators are exhausted
     */
    public static <A, B> BiIterator<A, B> zip(final Iterator<A> iterA, final Iterator<B> iterB, final A valueForNoneA, final B valueForNoneB) {
        final Iterator<A> iter1 = iterA == null ? ObjIterator.empty() : iterA;
        final Iterator<B> iter2 = iterB == null ? ObjIterator.empty() : iterB;

        return new BiIterator<>() {
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = iter1.hasNext() || iter2.hasNext();
                }

                return hasNextFlag;
            }

            @Override
            public Pair<A, B> next() {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Pair.of(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                while (hasNextFlag || (iter1.hasNext() || iter2.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNextFlag || (iter1.hasNext() || iter2.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            hasNextFlag = iter1.hasNext() || iter2.hasNext();
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // Reset for the next call

                        return mapper.apply(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                    }
                };
            }
        };
    }

    /**
     * Unzips an iterable into a {@code BiIterator} by splitting each element into a pair using the unzip function.
     * This is the inverse operation of zipping - it transforms single elements into pairs of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> data = List.of("Alice:25", "Bob:30");
     * BiIterator<String, Integer> iter = BiIterator.unzip(data, (s, pair) -> {
     *     String[] parts = s.split(":");
     *     pair.set(parts[0], Integer.parseInt(parts[1]));
     * });
     * // Produces: (Alice,25), (Bob,30)
     * }</pre>
     *
     * @param <T> the type of elements in the iterable
     * @param <A> the first type of elements in the resulting pairs
     * @param <B> the second type of elements in the resulting pairs
     * @param iter the iterable to unzip, may be {@code null}
     * @param unzipFunction a BiConsumer that splits each element into a pair by populating the provided Pair object
     * @return a BiIterator of pairs produced by the unzip function, or empty if iter is {@code null}
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterable<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzipFunction) {
        if (iter == null) {
            return BiIterator.empty();
        }

        return unzip(iter.iterator(), unzipFunction);
    }

    /**
     * Unzips an iterator into a {@code BiIterator} by splitting each element into a pair using the unzip function.
     * This is the inverse operation of zipping - it transforms single elements into pairs of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> data = List.of("Alice:25", "Bob:30", "Charlie:35");
     * BiIterator<String, Integer> iter = BiIterator.unzip(data.iterator(), (s, pair) -> {
     *     String[] parts = s.split(":");
     *     pair.set(parts[0], Integer.parseInt(parts[1]));
     * });
     * iter.forEachRemaining((name, age) -> System.out.println(name + ": " + age));
     * // Output:
     * // Alice: 25
     * // Bob: 30
     * // Charlie: 35
     * }</pre>
     *
     * @param <T> the type of elements in the iterator
     * @param <A> the first type of elements in the resulting pairs
     * @param <B> the second type of elements in the resulting pairs
     * @param iter the iterator to unzip, may be {@code null}
     * @param unzipFunction a BiConsumer that splits each element into a pair by populating the provided Pair object
     * @return a BiIterator of pairs produced by the unzip function, or empty if iter is {@code null}
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzipFunction) {
        N.checkArgNotNull(unzipFunction, cs.function);

        if (iter == null) {
            return BiIterator.empty();
        }

        final BooleanSupplier booleanSupplier = iter::hasNext;

        final Consumer<Pair<A, B>> output = out -> unzipFunction.accept(iter.next(), out);

        return BiIterator.generate(booleanSupplier, output);
    }

    /**
     * Processes the next pair of elements in the iterator using the provided action.
     *
     * @param <E> the type of exception that the action may throw
     * @param action a BiConsumer that processes the next pair of elements
     * @throws NoSuchElementException if there are no more elements in the iterator
     * @throws E if the action throws an exception
     */
    protected abstract <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E;

    /**
     * Performs the given action for each remaining element in the iterator until all elements have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each element
     * @deprecated use {@code forEachRemaining(BiConsumer)} to avoid creating the unnecessary {@code Pair} Objects.
     * @see #forEachRemaining(BiConsumer)
     */
    @Deprecated
    @Override
    public void forEachRemaining(final Consumer<? super Pair<A, B>> action) {
        N.checkArgNotNull(action, cs.action);

        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining pair of elements in this iterator.
     * The action is executed for each pair until all elements are consumed or the action throws an exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * iter.forEachRemaining((key, value) -> System.out.println(key + "=" + value));
     * }</pre>
     *
     * @param action the action to be performed for each pair of elements, must not be {@code null}
     */
    public abstract void forEachRemaining(final BiConsumer<? super A, ? super B> action);

    /**
     * Performs the given action for each remaining pair of elements in this iterator.
     * This variant allows the action to throw checked exceptions.
     *
     * @param <E> the type of exception that the action may throw
     * @param action a BiConsumer that processes each pair of elements, must not be {@code null}
     * @throws E if the action throws an exception
     */
    public abstract <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E; // NOSONAR

    /**
     * Returns a new {@code BiIterator} that skips the first {@code n} pairs of elements.
     * The resulting iterator will begin iteration after discarding the specified number of elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2, "c", 3));
     * BiIterator<String, Integer> skipped = iter.skip(1);
     * // Skips first pair, iterates over remaining pairs
     * }</pre>
     *
     * @param n the number of pairs to skip from the beginning, must be non-negative
     * @return a new BiIterator that begins after skipping {@code n} pairs, or this iterator if {@code n} is 0
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public BiIterator<A, B> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final BiIterator<A, B> iter = this;

        return new BiIterator<>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!skipped) {
                    skip();
                }

                iter.next(action);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                if (!skipped) {
                    skip();
                }

                iter.forEachRemaining(action);
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                if (!skipped) {
                    skip();
                }

                iter.foreachRemaining(action);
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                N.checkArgNotNull(mapper, cs.mapper);

                if (!skipped) {
                    skip();
                }

                return iter.map(mapper);
            }

            private void skip() {
                long idx = 0;

                @SuppressWarnings("UnnecessaryLocalVariable")
                final Throwables.BiConsumer<A, B, RuntimeException> action = DO_NOTHING;

                while (idx++ < n && iter.hasNext()) {
                    iter.next(action);
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new {@code BiIterator} that is limited to at most {@code count} pairs of elements.
     * The resulting iterator will produce at most the specified number of pairs, even if more are available.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<Integer, Integer> infinite = BiIterator.generate(pair -> pair.set(1, 2));
     * BiIterator<Integer, Integer> limited = infinite.limit(5);
     * // Produces exactly 5 pairs
     * }</pre>
     *
     * @param count the maximum number of pairs to include, must be non-negative
     * @return a new BiIterator limited to {@code count} pairs, or an empty iterator if {@code count} is 0
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public BiIterator<A, B> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return BiIterator.empty();
        }

        final BiIterator<A, B> iter = this;

        return new BiIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                iter.next(action);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                final Throwables.BiConsumer<? super A, ? super B, RuntimeException> actionE = Fnn.from(action);

                while (hasNext()) {
                    cnt--;
                    iter.next(actionE);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNext()) {
                    cnt--;
                    iter.next(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                if (cnt > 0) {
                    return iter.<R> map(mapper).limit(cnt);
                } else {
                    return ObjIterator.empty();
                }
            }
        };
    }

    /**
     * Returns a new {@code BiIterator} that includes only pairs satisfying the given predicate.
     * Pairs for which the predicate returns {@code false} are skipped during iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2, "c", 3));
     * BiIterator<String, Integer> filtered = iter.filter((k, v) -> v > 1);
     * // Produces only pairs where value > 1: (b,2), (c,3)
     * }</pre>
     *
     * @param predicate the predicate to test each pair, must not be {@code null}
     * @return a new BiIterator containing only pairs that satisfy the predicate
     * @throws IllegalArgumentException if predicate is {@code null}
     */
    public BiIterator<A, B> filter(final BiPredicate<? super A, ? super B> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        final BiIterator<A, B> iter = this;

        return new BiIterator<>() {
            private final Pair<A, B> next = new Pair<>();
            private final Throwables.BiConsumer<A, B, RuntimeException> setNext = next::set;

            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    while (iter.hasNext()) {
                        iter.next(setNext);

                        if (predicate.test(next.left(), next.right())) {
                            hasNextFlag = true;
                            break;
                        }
                    }
                }

                return hasNextFlag;
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNextFlag && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false;

                return next.copy();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!hasNextFlag && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false;

                action.accept(next.left(), next.right());
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                while (hasNextFlag || hasNext()) {
                    hasNextFlag = false;

                    action.accept(next.left(), next.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNextFlag || hasNext()) {
                    hasNextFlag = false;

                    action.accept(next.left(), next.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            while (iter.hasNext()) {
                                iter.next(setNext);

                                if (predicate.test(next.left(), next.right())) {
                                    hasNextFlag = true;
                                    break;
                                }
                            }
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!hasNextFlag && !hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false;

                        return mapper.apply(next.left(), next.right());
                    }
                };
            }
        };
    }

    /**
     * Transforms each pair of elements in this {@code BiIterator} using the given mapper function,
     * producing an {@code ObjIterator} of the mapped results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * ObjIterator<String> mapped = iter.map((k, v) -> k + "=" + v);
     * // Produces: "a=1", "b=2"
     * }</pre>
     *
     * @param <R> the type of elements in the resulting iterator
     * @param mapper the function to apply to each pair of elements, must not be {@code null}
     * @return an ObjIterator containing the results of applying the mapper to each pair
     * @throws IllegalArgumentException if mapper is {@code null}
     */
    public abstract <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper);

    /**
     * Returns an {@code Optional} containing the first pair of elements, or empty if this iterator has no elements.
     * This method consumes the first pair from the iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * Optional<Pair<String, Integer>> first = iter.first();
     * if (first.isPresent()) {
     *     System.out.println("First pair: " + first.get().left() + "=" + first.get().right());
     * }
     * }</pre>
     *
     * @return an Optional containing the first pair if available, otherwise empty
     */
    public Optional<Pair<A, B>> first() {
        if (hasNext()) {
            return Optional.of(next());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns an {@code Optional} containing the last pair of elements, or empty if this iterator has no elements.
     * This method consumes all remaining pairs in the iterator to find the last one.
     *
     * <p><strong>Note:</strong> This operation is expensive for large iterators as it must iterate through all elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2, "c", 3));
     * Optional<Pair<String, Integer>> last = iter.last();
     * if (last.isPresent()) {
     *     System.out.println("Last pair: " + last.get().left() + "=" + last.get().right());
     * }
     * }</pre>
     *
     * @return an Optional containing the last pair if the iterator is non-empty, otherwise empty
     */
    public Optional<Pair<A, B>> last() {
        if (hasNext()) {
            final Pair<A, B> next = new Pair<>();
            final BiConsumer<A, B> setNext = next::set;

            forEachRemaining(setNext);

            return Optional.of(next);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Converts this {@code BiIterator} into an {@code EntryStream} for further stream processing.
     * The resulting stream will contain all remaining pairs from this iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * EntryStream<String, Integer> stream = iter.stream();
     * Map<String, Integer> result = stream.toMap();
     * }</pre>
     *
     * @return an EntryStream containing the remaining pairs in this BiIterator
     */
    public EntryStream<A, B> stream() {
        return EntryStream.of(this);
    }

    /**
     * Converts this {@code BiIterator} into a {@code Stream} by applying the mapper function to each pair.
     * The resulting stream will contain the mapped values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * Stream<String> stream = iter.stream((k, v) -> k + "=" + v);
     * }</pre>
     *
     * @param <R> the type of elements in the resulting Stream
     * @param mapper the function to apply to each pair, must not be {@code null}
     * @return a Stream containing the elements produced by the mapper function
     */
    public <R> Stream<R> stream(final BiFunction<? super A, ? super B, ? extends R> mapper) {
        return Stream.of(map(mapper));
    }

    /**
     * Converts all remaining pairs in this {@code BiIterator} to an array of {@code Pair} objects.
     * This method consumes the entire iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * Pair<String, Integer>[] array = iter.toArray();
     * System.out.println("Array length: " + array.length); // 2
     * }</pre>
     *
     * @return an array containing all remaining pairs from this BiIterator
     */
    @SuppressWarnings("deprecation")
    public Pair<A, B>[] toArray() {
        return toArray(new Pair[0]);
    }

    /**
     * Converts the elements in this BiIterator to an array of the specified type.
     *
     * @param <T> the type of the array elements. It should be a super type of Pair.
     * @param a the array into which the elements of this BiIterator are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose.
     * @return an array containing the elements of this BiIterator
     * @deprecated This method is deprecated. Use {@link #toArray()} or {@link #toList()} instead.
     */
    @Deprecated
    public <T> T[] toArray(final T[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts all remaining pairs in this {@code BiIterator} to a {@code List} of {@code Pair} objects.
     * This method consumes the entire iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * List<Pair<String, Integer>> list = iter.toList();
     * list.forEach(pair -> System.out.println(pair.left() + "=" + pair.right()));
     * }</pre>
     *
     * @return a List containing all remaining pairs from this BiIterator
     */
    public List<Pair<A, B>> toList() {
        return toCollection(Suppliers.ofList());
    }

    /**
     * Converts all remaining pairs in this {@code BiIterator} to a {@code Pair} of {@code List}s.
     * The first list contains all first elements from the pairs, and the second list contains all second elements.
     * This method consumes the entire iterator and "unzips" the pairs into separate lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * Pair<List<String>, List<Integer>> lists = iter.toMultiList(ArrayList::new);
     * // lists.left() = ["a", "b"], lists.right() = [1, 2]
     * }</pre>
     *
     * @param supplier a Supplier that provides new List instances, must not be {@code null}
     * @return a Pair containing two Lists: the first with all first elements, the second with all second elements
     */
    public Pair<List<A>, List<B>> toMultiList(@SuppressWarnings("rawtypes") final Supplier<? extends List> supplier) {
        final List<A> listA = supplier.get();
        final List<B> listB = supplier.get();

        this.foreachRemaining((a, b) -> {
            listA.add(a);
            listB.add(b);
        });

        return Pair.of(listA, listB);
    }

    /**
     * Converts all remaining pairs in this {@code BiIterator} to a {@code Pair} of {@code Set}s.
     * The first set contains all unique first elements from the pairs, and the second set contains all unique second elements.
     * This method consumes the entire iterator and "unzips" the pairs into separate sets, removing duplicates.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * Pair<Set<String>, Set<Integer>> sets = iter.toMultiSet(HashSet::new);
     * }</pre>
     *
     * @param supplier a Supplier that provides new Set instances, must not be {@code null}
     * @return a Pair containing two Sets: the first with all unique first elements, the second with all unique second elements
     */
    public Pair<Set<A>, Set<B>> toMultiSet(@SuppressWarnings("rawtypes") final Supplier<? extends Set> supplier) {
        final Set<A> listA = supplier.get();
        final Set<B> listB = supplier.get();

        this.foreachRemaining((a, b) -> {
            listA.add(a);
            listB.add(b);
        });

        return Pair.of(listA, listB);
    }
}
