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

import java.util.Collection;
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
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * An abstract, single-use iterator over pairs of values of types {@code A} and {@code B}.
 * Its position advances as elements are consumed; the iterator is read-only only in the sense that
 * {@link #remove()} is unsupported, not that its iteration state is immutable.
 *
 * <p>Each call to {@link #next()} returns a {@link Pair}{@code <A, B>} holding the two values
 * together. The more efficient {@link #forEachRemaining(java.util.function.BiConsumer)} and
 * {@link #foreachRemaining(Throwables.BiConsumer)} variants accept the two components
 * directly, avoiding the creation of intermediate {@code Pair} objects.</p>
 *
 * <p>Static factory methods support creation from maps, arrays, iterables, and
 * generator functions. Transformation methods such as {@link #skip(long)},
 * {@link #limit(long)}, and {@link #map(BiFunction)} are provided for common
 * pipeline operations. Materialization methods such as {@link #unzipToLists(Supplier)},
 * {@link #unzipToSets(Supplier)}, and {@link #unzipToCollections(Supplier, Supplier)}
 * consume the remaining pairs and split the two components into separate result collections.</p>
 *
 * @param <A> the type of the first element in each pair
 * @param <B> the type of the second element in each pair
 *
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 * @see Pair
 */
@SuppressWarnings("java:S6548")
public abstract class BiIterator<A, B> extends ImmutableIterator<Pair<A, B>> {

    /**
     * Constructs a new {@code BiIterator}.
     * Intended for use by subclasses only.
     */
    protected BiIterator() {
    }

    /**
     * Resets the reused output holder to two {@code null} values, so that a component the
     * generator does not set is {@code null} rather than a leftover from the previous iteration.
     *
     * @param output the holder to clear
     */
    private static void clearOutput(final Pair<?, ?> output) {
        output.set(null, null);
    }

    /**
     * A singleton empty BiIterator instance that contains no elements.
     * This iterator's hasNext() always returns {@code false}, and any attempt to retrieve elements throws NoSuchElementException.
     *
     * @see #empty()
     */
    @SuppressWarnings("rawtypes")
    private static final BiIterator EMPTY = new BiIterator() {
        /**
         * Always returns {@code false} because this iterator is empty.
         *
         * @return {@code false}
         */
        @Override
        public boolean hasNext() {
            return false;
        }

        /**
         * {@inheritDoc}
         * @throws NoSuchElementException if no pair or source entry remains
         */
        @Override
        public Object next() throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        /**
         * @throws IllegalArgumentException if {@code action} is {@code null}.
         * @throws NoSuchElementException if {@code action} is non-null, because this iterator is empty
         */
        @Override
        protected void next(final Throwables.BiConsumer action) throws IllegalArgumentException, NoSuchElementException {
            // Validated before exhaustion is reported, so that a null action is rejected here exactly as the
            // other next(action) implementations in this class reject it - with IllegalArgumentException.
            N.checkArgNotNull(action, cs.action);

            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        /**
         * @throws IllegalArgumentException if {@code action} is {@code null}.
         */
        @Override
        public void forEachRemaining(final BiConsumer action) throws IllegalArgumentException {
            N.checkArgNotNull(action, cs.action);

            // It's empty. Nothing to do.
        }

        /**
         * @throws IllegalArgumentException if {@code action} is {@code null}.
         */
        @Override
        public void foreachRemaining(final Throwables.BiConsumer action) throws IllegalArgumentException {
            N.checkArgNotNull(action, cs.action);

            // It's empty. Nothing to do.
        }

        /**
         * @throws IllegalArgumentException if {@code mapper} is {@code null}.
         */
        @Override
        public ObjIterator map(final BiFunction mapper) throws IllegalArgumentException {
            N.checkArgNotNull(mapper, cs.mapper);

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
     * boolean has = empty.hasNext();   // returns false
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

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws NullPointerException if the source iterator returns a {@code null} map entry.
             */
            @Override
            public Pair<K, V> next() throws NoSuchElementException, NullPointerException {
                return Pair.from(nextEntry());
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws NullPointerException if the source iterator returns a null entry
             * @throws E if {@code action} throws while processing the next pair
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super K, ? super V, E> action)
                    throws IllegalArgumentException, NoSuchElementException, NullPointerException, E {
                N.checkArgNotNull(action, cs.action);

                final Map.Entry<K, V> entry = nextEntry();

                action.accept(entry.getKey(), entry.getValue());
            }

            /**
             * Delegates straight to {@code iter.next()}: exhaustion is reported by the wrapped entry
             * iterator, not normalised to {@code ERROR_MSG_FOR_NO_SUCH_EX}, so a message-less
             * {@code NoSuchElementException} from the JDK collections - or {@code IllegalStateException} /
             * {@code ConcurrentModificationException} from another source - reaches the caller unchanged.
             * @throws NoSuchElementException if no pair or source entry remains
             */
            private Map.Entry<K, V> nextEntry() throws NoSuchElementException {
                return iter.next();
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws NullPointerException if the source iterator returns a null entry
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super K, ? super V> action) throws IllegalArgumentException, NullPointerException {
                N.checkArgNotNull(action, cs.action);

                Map.Entry<K, V> entry = null;

                while (iter.hasNext()) {
                    entry = iter.next();
                    action.accept(entry.getKey(), entry.getValue());
                }
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws NullPointerException if the source iterator returns a null entry
             * @throws E if {@code action} throws while processing a remaining entry
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super K, ? super V, E> action)
                    throws IllegalArgumentException, NullPointerException, E {
                N.checkArgNotNull(action, cs.action);

                Map.Entry<K, V> entry = null;

                while (iter.hasNext()) {
                    entry = iter.next();
                    action.accept(entry.getKey(), entry.getValue());
                }
            }

            /**
             * @throws IllegalArgumentException if {@code mapper} is {@code null}.
             */
            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super K, ? super V, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper, cs.mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    /**
                     * {@inheritDoc}
                     * @throws NoSuchElementException if no pair or source entry remains
                     * @throws NullPointerException if the source iterator returns a null entry
                     */
                    @Override
                    public R next() throws NoSuchElementException, NullPointerException {
                        final Map.Entry<K, V> entry = nextEntry();

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
     * that limit the iteration (e.g., {@link #limit(long)}) to avoid infinite loops.</p>
     *
     * <p>The output consumer receives a mutable {@code Pair} object that should be populated
     * with the next pair of values using {@code pair.set(a, b)} or {@code pair.setLeft(a)} and {@code pair.setRight(b)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * // The explicit type witness is required: chaining limit(..) removes the target type that
     * // would otherwise let <A, B> be inferred from the assignment.
     * BiIterator<Integer, String> iter = BiIterator.<Integer, String> generate(pair -> {
     *     int n = counter.incrementAndGet();
     *     pair.set(n, "value" + n);
     * }).limit(5);
     * }</pre>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param output a Consumer that populates a Pair with the next values on each iteration
     * @return an infinite BiIterator that uses the output Consumer to generate its elements
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @see #generate(BooleanSupplier, Consumer)
     */
    public static <A, B> BiIterator<A, B> generate(final Consumer<Pair<A, B>> output) throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        return generate(com.landawn.abacus.util.function.BooleanSupplier.TRUE, output);
    }

    /**
     * Generates a {@code BiIterator} with elements produced by the output consumer while the hasNext supplier returns {@code true}.
     * The hasNext supplier controls when the iteration should stop, and the output consumer populates each pair of values.
     *
     * <p>This method provides full control over iteration termination and element generation.
     * The hasNext supplier is called before each element is produced to determine if iteration should continue.</p>
     *
     * <p>The holder is reused and cleared to two {@code null} values before each invocation, so an
     * omitted component is {@code null}, never a value left by the previous iteration. The holder
     * must not be retained by the consumer. Once {@code hasNext} returns {@code false}, the iterator
     * is permanently exhausted.</p>
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
     * @param hasNext a BooleanSupplier that returns {@code true} if the iterator should have more elements
     * @param output a Consumer that populates a Pair with the next values on each iteration
     * @return a BiIterator that uses the hasNext supplier and output consumer to generate its elements
     * @throws IllegalArgumentException if any of {@code hasNext}, {@code output} is {@code null}.
     */
    public static <A, B> BiIterator<A, B> generate(final BooleanSupplier hasNext, final Consumer<Pair<A, B>> output) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext, cs.hasNext);
        N.checkArgNotNull(output, cs.output);

        return new BiIterator<>() {
            private final Pair<A, B> tmp = new Pair<>();
            private boolean hasNextFlag = false;
            private boolean exhausted = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag && !exhausted) {
                    if (hasNext.getAsBoolean()) {
                        hasNextFlag = true;
                    } else {
                        exhausted = true;
                    }
                }

                return hasNextFlag;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             */
            @Override
            public Pair<A, B> next() throws NoSuchElementException {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // The available element is handed to the producer.
                clearOutput(tmp);
                output.accept(tmp);

                return Pair.of(tmp.left(), tmp.right());
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws E if {@code action} throws while processing the next pair
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // The available element is handed to the producer.
                clearOutput(tmp);
                output.accept(tmp);

                action.accept(tmp.left(), tmp.right());
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                while (hasNextFlag || (!exhausted && hasNext.getAsBoolean())) {
                    hasNextFlag = false; // The available element is handed to the producer.
                    clearOutput(tmp);
                    output.accept(tmp);

                    action.accept(tmp.left(), tmp.right());
                }
                exhausted = true;
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while processing a remaining pair
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                while (hasNextFlag || (!exhausted && hasNext.getAsBoolean())) {
                    hasNextFlag = false; // The available element is handed to the producer.
                    clearOutput(tmp);
                    output.accept(tmp);

                    action.accept(tmp.left(), tmp.right());
                }
                exhausted = true;
            }

            /**
             * @throws IllegalArgumentException if {@code mapper} is {@code null}.
             */
            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper, cs.mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag && !exhausted) {
                            if (hasNext.getAsBoolean()) {
                                hasNextFlag = true;
                            } else {
                                exhausted = true;
                            }
                        }

                        return hasNextFlag;
                    }

                    /**
                     * {@inheritDoc}
                     * @throws NoSuchElementException if no pair or source entry remains
                     */
                    @Override
                    public R next() throws NoSuchElementException {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // The available element is handed to the producer.
                        clearOutput(tmp);
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
     * populated with the values corresponding to that index. The holder is reused and cleared to two
     * {@code null} values before each invocation, so an omitted component is {@code null} rather than a
     * leftover from the previous index.</p>
     *
     * <p><b>API Note:</b> the index advances only after {@code output} returns normally. If {@code output}
     * throws, the exception propagates to the caller and the index is <i>not</i> consumed, so the next
     * call re-invokes {@code output} for the same index. A generator that fails deterministically at some
     * index therefore keeps failing there rather than silently skipping that pair; guard against that in
     * the generator if a failure should be skipped instead of retried.</p>
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
     * @param toIndex the ending index (exclusive), must not be less than {@code fromIndex}
     * @param output an IntObjConsumer that accepts an index and a Pair to populate with values
     * @return a BiIterator that generates elements for each index in the range [fromIndex, toIndex)
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative or {@code fromIndex} is greater than {@code toIndex}
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     */
    public static <A, B> BiIterator<A, B> generate(final int fromIndex, final int toIndex, final IntObjConsumer<Pair<A, B>> output)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        // Checked here rather than via N.checkFromToIndex(from, to, Integer.MAX_VALUE): this is an index
        // range, not a view over a container, so the array-oriented "out-of-bounds for length ..." wording
        // that helper produces would be misleading.
        if (fromIndex < 0 || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException(
                    "Invalid index range: fromIndex = " + fromIndex + ", toIndex = " + toIndex + ". Expected: 0 <= fromIndex <= toIndex");
        }

        N.checkArgNotNull(output, cs.output);

        return new BiIterator<>() {
            private int cursor = fromIndex;
            private final Pair<A, B> tmp = new Pair<>();

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             */
            @Override
            public Pair<A, B> next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                clearOutput(tmp);
                // Advance only AFTER the generator returns: if it throws, the index is not consumed,
                // so a retry re-runs this index rather than silently dropping the pair.
                output.accept(cursor, tmp);
                cursor++;

                return Pair.of(tmp.left(), tmp.right());
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws E if {@code action} throws while processing the next pair
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                clearOutput(tmp);
                // Advance only AFTER the generator returns: if it throws, the index is not consumed,
                // so a retry re-runs this index rather than silently dropping the pair.
                output.accept(cursor, tmp);
                cursor++;

                action.accept(tmp.left(), tmp.right());
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                while (cursor < toIndex) {
                    clearOutput(tmp);
                    output.accept(cursor, tmp);
                    cursor++;

                    action.accept(tmp.left(), tmp.right());
                }
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while processing a remaining pair
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                while (cursor < toIndex) {
                    clearOutput(tmp);
                    output.accept(cursor, tmp);
                    cursor++;

                    action.accept(tmp.left(), tmp.right());
                }
            }

            /**
             * @throws IllegalArgumentException if {@code mapper} is {@code null}.
             */
            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper, cs.mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return cursor < toIndex;
                    }

                    /**
                     * {@inheritDoc}
                     * @throws NoSuchElementException if no pair or source entry remains
                     */
                    @Override
                    public R next() throws NoSuchElementException {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        clearOutput(tmp);
                        output.accept(cursor, tmp);
                        cursor++;

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
     * <p><b>API Note:</b> the two sources are pulled left-to-right within one step. If pulling the second
     * source throws, the element already taken from the first source is lost - the iterators are left
     * out of step and cannot be resynchronised.</p>
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
            // Once either source runs dry the zip is permanently exhausted, so stop probing the sources.
            // Without this latch every hasNext() call re-invokes iterA.hasNext() forever, which is a real
            // cost for a source whose hasNext() is expensive or has side effects. TriIterator.zip and
            // BiIterator.generate already work this way.
            private boolean exhausted = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag && !exhausted) {
                    if (iterA.hasNext() && iterB.hasNext()) {
                        hasNextFlag = true;
                    } else {
                        exhausted = true;
                    }
                }

                return hasNextFlag;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             */
            @Override
            public Pair<A, B> next() throws NoSuchElementException {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Pair.of(iterA.next(), iterB.next());
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws E if {@code action} throws while processing the next pair
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                action.accept(iterA.next(), iterB.next());
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                while (hasNext()) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iterA.next(), iterB.next());
                }
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while processing a remaining pair
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                while (hasNext()) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iterA.next(), iterB.next());
                }

            }

            /**
             * @throws IllegalArgumentException if {@code mapper} is {@code null}.
             */
            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper, cs.mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag && !exhausted) {
                            if (iterA.hasNext() && iterB.hasNext()) {
                                hasNextFlag = true;
                            } else {
                                exhausted = true;
                            }
                        }

                        return hasNextFlag;
                    }

                    /**
                     * {@inheritDoc}
                     * @throws NoSuchElementException if no pair or source entry remains
                     */
                    @Override
                    public R next() throws NoSuchElementException {
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
     * <p><b>API Note:</b> the two sources are pulled left-to-right within one step. If pulling the second
     * source throws, the element already taken from the first source is lost - the iterators are left
     * out of step and cannot be resynchronised.</p>
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
            // See the latch note on zip(Iterator, Iterator): once both sources are dry this iterator is
            // permanently exhausted, so stop re-probing them on every hasNext().
            private boolean exhausted = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag && !exhausted) {
                    if (iter1.hasNext() || iter2.hasNext()) {
                        hasNextFlag = true;
                    } else {
                        exhausted = true;
                    }
                }

                return hasNextFlag;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             */
            @Override
            public Pair<A, B> next() throws NoSuchElementException {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Pair.of(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws E if {@code action} throws while processing the next pair
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                while (hasNext()) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                }
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while processing a remaining pair
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                while (hasNext()) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                }
            }

            /**
             * @throws IllegalArgumentException if {@code mapper} is {@code null}.
             */
            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper, cs.mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag && !exhausted) {
                            if (iter1.hasNext() || iter2.hasNext()) {
                                hasNextFlag = true;
                            } else {
                                exhausted = true;
                            }
                        }

                        return hasNextFlag;
                    }

                    /**
                     * {@inheritDoc}
                     * @throws NoSuchElementException if no pair or source entry remains
                     */
                    @Override
                    public R next() throws NoSuchElementException {
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
     * @param iter the iterator to unzip, may be {@code null}; returns an empty {@code BiIterator} when {@code null}
     * @param unzipFunction a {@code BiConsumer} that splits each element into a pair by populating the provided {@code Pair} object
     * @return a {@code BiIterator} of pairs produced by the unzip function, or an empty {@code BiIterator} if {@code iter} is {@code null}
     * @throws IllegalArgumentException if {@code unzipFunction} is {@code null}.
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzipFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(unzipFunction, cs.unzipFunction);

        if (iter == null) {
            return BiIterator.empty();
        }

        final BooleanSupplier booleanSupplier = iter::hasNext;

        final Consumer<Pair<A, B>> output = out -> unzipFunction.accept(iter.next(), out);

        return BiIterator.generate(booleanSupplier, output);
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
     * @param iter the iterable to unzip, may be {@code null}; returns an empty {@code BiIterator} when {@code null}
     * @param unzipFunction a {@code BiConsumer} that splits each element into a pair by populating the provided {@code Pair} object
     * @return a {@code BiIterator} of pairs produced by the unzip function, or an empty {@code BiIterator} if {@code iter} is {@code null}
     * @throws IllegalArgumentException if {@code unzipFunction} is {@code null}.
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterable<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzipFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(unzipFunction, cs.unzipFunction);

        if (iter == null) {
            return BiIterator.empty();
        }

        return unzip(iter.iterator(), unzipFunction);
    }

    /**
     * Unzips an iterator into two separate collections using the provided unzip function and independent collection suppliers.
     *
     * <p><b>Note:</b> because the iterator's length is unknown, the {@code leftSupplier} and
     * {@code rightSupplier} {@code IntFunction}s are always invoked with a size hint of {@code 0}.
     * A pre-allocating supplier such as {@code size -> new ArrayList<>(size)} will therefore be
     * given capacity {@code 0}. Use the {@code Iterable} overload (which extracts the size from a
     * {@code Collection}) when an accurate size hint is required.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a=1", "b=2", "a=3").iterator();
     * Pair<Set<String>, List<Integer>> result = BiIterator.unzip(iter, (s, out) -> {
     *     String[] parts = s.split("=");
     *     out.set(parts[0], Integer.parseInt(parts[1]));
     * }, size -> new LinkedHashSet<>(), size -> new ArrayList<>(size));
     * // result.left() is [a, b], result.right() is [1, 2, 3]
     * }</pre>
     *
     * @param <T> the type of elements in the iterator
     * @param <A> the first type of elements produced by the unzip function
     * @param <B> the second type of elements produced by the unzip function
     * @param <LC> the type of the first output collection
     * @param <RC> the type of the second output collection
     * @param iter the iterator to unzip, may be {@code null}; returns empty output collections when {@code null}
     * @param unzipFunction a {@code BiConsumer} that splits each element into a pair by populating the provided {@code Pair} object
     * @param leftSupplier a function that provides the first output collection; always called with a size hint of {@code 0}
     * @param rightSupplier a function that provides the second output collection; always called with a size hint of {@code 0}
     * @return a {@code Pair} containing the two output collections
     * @throws IllegalArgumentException if a callback or supplier is null, either supplier returns null, or the suppliers return the same output collection
     * @throws UnsupportedOperationException if a pair remains and a supplied output collection does not support adding its component
     */
    public static <T, A, B, LC extends Collection<A>, RC extends Collection<B>> Pair<LC, RC> unzip(final Iterator<? extends T> iter,
            final BiConsumer<? super T, Pair<A, B>> unzipFunction, final IntFunction<? extends LC> leftSupplier, final IntFunction<? extends RC> rightSupplier)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(unzipFunction, cs.unzipFunction);
        N.checkArgNotNull(leftSupplier, cs.leftSupplier);
        N.checkArgNotNull(rightSupplier, cs.rightSupplier);

        return unzip(iter, unzipFunction).unzipToCollections(() -> leftSupplier.apply(0), () -> rightSupplier.apply(0));
    }

    /**
     * Unzips an iterable into two separate collections using the provided unzip function and independent collection suppliers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> data = Arrays.asList("a=1", "b=2", "a=3");
     * Pair<Set<String>, List<Integer>> result = BiIterator.unzip(data, (s, out) -> {
     *     String[] parts = s.split("=");
     *     out.set(parts[0], Integer.parseInt(parts[1]));
     * }, size -> new LinkedHashSet<>(), size -> new ArrayList<>(size));
     * // result.left() is [a, b], result.right() is [1, 2, 3]
     * }</pre>
     *
     * @param <T> the type of elements in the iterable
     * @param <A> the first type of elements produced by the unzip function
     * @param <B> the second type of elements produced by the unzip function
     * @param <LC> the type of the first output collection
     * @param <RC> the type of the second output collection
     * @param iter the iterable to unzip, may be {@code null}; returns empty output collections when {@code null}
     * @param unzipFunction a {@code BiConsumer} that splits each element into a pair by populating the provided {@code Pair} object
     * @param leftSupplier a function that provides the first output collection
     * @param rightSupplier a function that provides the second output collection
     * @return a {@code Pair} containing the two output collections
     * @throws IllegalArgumentException if a callback or supplier is null, either supplier returns null, or the suppliers return the same output collection
     * @throws UnsupportedOperationException if a pair remains and a supplied output collection does not support adding its component
     */
    public static <T, A, B, LC extends Collection<A>, RC extends Collection<B>> Pair<LC, RC> unzip(final Iterable<? extends T> iter,
            final BiConsumer<? super T, Pair<A, B>> unzipFunction, final IntFunction<? extends LC> leftSupplier, final IntFunction<? extends RC> rightSupplier)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(unzipFunction, cs.unzipFunction);
        N.checkArgNotNull(leftSupplier, cs.leftSupplier);
        N.checkArgNotNull(rightSupplier, cs.rightSupplier);

        final int len = iter instanceof Collection ? ((Collection<?>) iter).size() : 0;

        return unzip(iter, unzipFunction).unzipToCollections(() -> leftSupplier.apply(len), () -> rightSupplier.apply(len));
    }

    /**
     * Advances to the next pair of elements and passes the two components directly to the
     * provided action, avoiding the creation of an intermediate {@code Pair} object.
     *
     * @param <E> the type of exception that the action may throw
     * @param action a {@code BiConsumer} that receives the first and second values of the next pair, must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws NoSuchElementException if no pair or source entry remains
     * @throws E if {@code action} throws while processing the next pair
     */
    protected abstract <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
            throws IllegalArgumentException, NoSuchElementException, E;

    /**
     * Performs the given action for each remaining element in the iterator until all elements
     * have been processed or the action throws an exception.
     *
     * <p>This overload wraps each pair in a {@link Pair} object before passing it to the action.
     * Prefer {@link #forEachRemaining(BiConsumer)} to avoid creating the unnecessary {@code Pair} objects.</p>
     *
     * @param action the action to be performed for each {@code Pair} element
     * @throws NullPointerException if {@code action} is {@code null}, as specified by {@link java.util.Iterator#forEachRemaining(java.util.function.Consumer)}.
     * @deprecated use {@link #forEachRemaining(BiConsumer)} to avoid creating unnecessary {@code Pair} objects.
     * @see #forEachRemaining(BiConsumer)
     */
    @Deprecated
    @Override
    public void forEachRemaining(final Consumer<? super Pair<A, B>> action) throws NullPointerException {
        N.requireNonNull(action, cs.action);

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
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @implSpec The default implementation forwards to {@link #foreachRemaining(Throwables.BiConsumer)};
     *           subclasses only need to override it to provide a faster traversal.
     */
    public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
        N.checkArgNotNull(action, cs.action);

        foreachRemaining((Throwables.BiConsumer<A, B, RuntimeException>) action::accept);
    }

    /**
     * Performs the given action for each remaining pair of elements in this iterator.
     * This variant supports actions that may throw checked exceptions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * iter.foreachRemaining((key, value) -> {
     *     processEntry(key, value);
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to perform for each remaining pair of elements, must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws E if {@code action} throws while processing a remaining pair
     * @implSpec The default implementation repeatedly calls {@link #next(Throwables.BiConsumer)} while
     *           {@link #hasNext()} reports more pairs; subclasses only need to override it to provide a
     *           faster traversal.
     */
    public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) // NOSONAR
            throws IllegalArgumentException, E {
        N.checkArgNotNull(action, cs.action);

        while (hasNext()) {
            next(action);
        }
    }

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
     * <p>The pairs are skipped lazily, on the first call to {@link #hasNext()},
     * {@link #next()}, or one of the {@code forEachRemaining} variants. Creating a mapped view with {@link #map(BiFunction)}
     * also remains lazy; accessing that view triggers the skip. Calling {@code skip(n)} itself does not consume pairs.</p>
     *
     * <p><b>API Note:</b> If producing a skipped pair throws, previously completed skips remain
     * recorded and a later operation resumes the remaining skip count. Side effects or partial
     * consumption inside the failing source cannot be rolled back.</p>
     *
     * @param n the number of pairs to skip from the beginning, must be non-negative
     * @return a new BiIterator that begins after skipping {@code n} pairs, or this iterator if {@code n} is 0
     * @throws IllegalArgumentException if {@code n} is negative.
     */
    public BiIterator<A, B> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final BiIterator<A, B> iter = this;

        return new BiIterator<>() {
            private boolean skipped = false;
            private long remaining = n;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             */
            @Override
            public Pair<A, B> next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws E if {@code action} throws while processing the next pair
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if (!skipped) {
                    skip();
                }

                iter.next(action);
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                if (!skipped) {
                    skip();
                }

                iter.forEachRemaining(action);
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while processing a remaining pair
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                if (!skipped) {
                    skip();
                }

                iter.foreachRemaining(action);
            }

            // No map(..) override: the inherited default routes through this iterator's next(action)
            // and hasNext(), which keeps the pending skip lazy instead of performing it when map() is
            // called. (An override that skipped here would consume `n` source pairs at composition time.)

            private void skip() {
                @SuppressWarnings("UnnecessaryLocalVariable")
                final Throwables.BiConsumer<A, B, RuntimeException> action = DO_NOTHING;

                while (remaining > 0 && iter.hasNext()) {
                    iter.next(action);
                    remaining--;
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
     * <p><b>API Note:</b> the remaining quota is decremented only once the source has actually
     * produced a pair. A source that fails while producing does not consume quota, so a retry can
     * still yield the full {@code count} pairs. The quota is shared with every view derived from
     * the returned iterator (notably {@link #map(BiFunction)}), so all of them together yield at
     * most {@code count} pairs.</p>
     *
     * @param count the maximum number of pairs to include, must be non-negative
     * @return a new BiIterator limited to {@code count} pairs, or an empty iterator if {@code count} is 0
     * @throws IllegalArgumentException if {@code count} is negative.
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

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             */
            @Override
            public Pair<A, B> next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                // Decrement only AFTER the source has produced the pair: if `iter.next()` throws, the
                // quota must stay intact so a retry can still deliver the full `count` pairs.
                final Pair<A, B> result = iter.next();
                cnt--;
                return result;
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws E if {@code action} throws while processing the next pair
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                // Counting inside the callback charges the quota only for a pair the source really
                // produced, while still charging it when the downstream action itself fails.
                iter.next((a, b) -> {
                    cnt--;
                    action.accept(a, b);
                });
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                final Throwables.BiConsumer<A, B, RuntimeException> counting = (a, b) -> {
                    cnt--;
                    action.accept(a, b);
                };

                while (hasNext()) {
                    iter.next(counting);
                }
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while processing a remaining pair
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                final Throwables.BiConsumer<A, B, E> counting = (a, b) -> {
                    cnt--;
                    action.accept(a, b);
                };

                while (hasNext()) {
                    iter.next(counting);
                }
            }

            // No map(..) override: the inherited default routes through this iterator's next(action),
            // so the mapped view shares `cnt` instead of getting a second, independent budget.
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
     * // Produces only pairs where value > 1: (b,2), (c,3) (order may vary; Map.of iteration order is unspecified)
     * }</pre>
     *
     * @param predicate the predicate to test each pair
     * @return a new BiIterator containing only pairs that satisfy the predicate
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public BiIterator<A, B> filter(final BiPredicate<? super A, ? super B> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

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

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             */
            @Override
            public Pair<A, B> next() throws NoSuchElementException {
                if (!hasNextFlag && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false;

                return next.copy();
            }

            /**
             * {@inheritDoc}
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws NoSuchElementException if no pair or source entry remains
             * @throws E if {@code action} throws while processing the next pair
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);

                if (!hasNextFlag && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false;

                action.accept(next.left(), next.right());
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             */
            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action, cs.action);

                while (hasNextFlag || hasNext()) {
                    hasNextFlag = false;

                    action.accept(next.left(), next.right());
                }
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}.
             * @throws E if {@code action} throws while processing a remaining pair
             */
            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action, cs.action);

                while (hasNextFlag || hasNext()) {
                    hasNextFlag = false;

                    action.accept(next.left(), next.right());
                }
            }

            // No map(..) override: the inherited default routes through this iterator's next(action),
            // so the mapped view sees exactly the pairs that pass the predicate and shares this
            // iterator's look-ahead state.
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
     * // Produces: "a=1", "b=2" (order may vary; Map.of iteration order is unspecified)
     * }</pre>
     *
     * <p><b>API Note:</b> the returned iterator and this iterator share a single consumption cursor:
     * consuming either one advances the other, and any limit imposed on this iterator applies to the
     * two of them together. The mapper is applied lazily, as elements are pulled.</p>
     *
     * @param <R> the type of elements in the resulting iterator
     * @param mapper the function to apply to each pair of elements, must not be {@code null}
     * @return an ObjIterator containing the results of applying the mapper to each pair
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     * @implSpec The default implementation pulls through {@link #next(Throwables.BiConsumer)} into a
     *           single reused {@link Pair} holder; subclasses only need to override it to avoid that
     *           holder. An override must keep routing through this iterator so that shared state
     *           (a remaining-limit counter, a pending-skip) stays in sync.
     */
    public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        final BiIterator<A, B> iter = this;

        return new ObjIterator<>() {
            private final Pair<A, B> tmp = new Pair<>();
            private final Throwables.BiConsumer<A, B, RuntimeException> setNext = tmp::set;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no pair or source entry remains
             */
            @Override
            public R next() throws NoSuchElementException {
                iter.next(setNext); // throws NoSuchElementException when exhausted

                return mapper.apply(tmp.left(), tmp.right());
            }
        };
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
     * @return an {@code EntryStream} containing the remaining pairs in this {@code BiIterator}
     * @see #stream(BiFunction)
     */
    public EntryStream<A, B> stream() {
        return EntryStream.of(this);
    }

    /**
     * Converts this {@code BiIterator} into a {@link Stream} by applying the mapper function to each pair.
     * The resulting stream will contain the mapped values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * Stream<String> stream = iter.stream((k, v) -> k + "=" + v);
     * }</pre>
     *
     * @param <R> the type of elements in the resulting {@code Stream}
     * @param mapper the function to apply to each pair of elements
     * @return a {@code Stream} containing the elements produced by applying {@code mapper} to each pair
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     * @see #stream()
     * @see #map(BiFunction)
     */
    public <R> Stream<R> stream(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        return Stream.of(map(mapper));
    }

    /**
     * Unzips all remaining pairs in this {@code BiIterator} into two {@code List}s.
     *
     * <p>This terminal operation consumes the iterator. The returned {@code Pair} holds
     * the first component values in {@link Pair#left()} and the second component values in
     * {@link Pair#right()}, preserving the iterator order within each list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.zip(new String[] { "a", "b" }, new Integer[] { 1, 2 });
     * Pair<List<String>, List<Integer>> lists = iter.unzipToLists(ArrayList::new);
     * // lists.left() is ["a", "b"], lists.right() is [1, 2]
     * }</pre>
     *
     * <p><b>API Note:</b> one supplier produces both lists, so its element type cannot be checked
     * against {@code A} and {@code B} - the parameter is deliberately raw. Passing, say,
     * {@code () -> new ArrayList<Double>()} compiles without a warning and hands back a list that is
     * declared {@code List<Double>} but holds {@code A} values, so the failure surfaces later as a
     * {@link ClassCastException} somewhere else. Use {@link #unzipToCollections(Supplier, Supplier)},
     * whose per-component suppliers are fully type-checked, whenever the supplier is anything other
     * than a plain constructor reference such as {@code ArrayList::new}.</p>
     *
     * <p>Output collections must be distinct instances; aliasing is rejected with IllegalArgumentException before consumption.
     * Callers must also avoid distinct wrappers that share mutable backing storage.</p>
     *
     * @param supplier a supplier invoked twice to create the left and right lists; each call must return a {@code non-null} {@code List}
     * @return a {@code Pair} whose left list contains all first components and whose right list contains all second components
     * @throws IllegalArgumentException if a collection supplier is {@code null}, returns {@code null}, or returns the same collection for both sides.
     * @throws UnsupportedOperationException if a pair remains and a supplied output collection does not support adding its component
     * @see #unzipToSets(Supplier)
     * @see #unzipToCollections(Supplier, Supplier)
     */
    public Pair<List<A>, List<B>> unzipToLists(@SuppressWarnings("rawtypes") final Supplier<? extends List> supplier)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(supplier, cs.supplier);

        final List<A> listA = N.checkArgNotNull(supplier.get(), "supplier.get()");
        final List<B> listB = N.checkArgNotNull(supplier.get(), "supplier.get()");
        N.checkArgument(listA != listB, "Output collections must be distinct instances");

        this.foreachRemaining((a, b) -> {
            listA.add(a);
            listB.add(b);
        });

        return Pair.of(listA, listB);
    }

    /**
     * Unzips all remaining pairs in this {@code BiIterator} into two independently supplied collections.
     *
     * <p>This terminal operation consumes the iterator. Use this overload when the first
     * and second components should be collected into different collection implementations
     * or when each component needs a separate sizing policy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.unzip(Arrays.asList("a=1", "b=2"), (s, out) -> {
     *     String[] parts = s.split("=");
     *     out.set(parts[0], Integer.parseInt(parts[1]));
     * });
     * Pair<Set<String>, List<Integer>> result = iter.unzipToCollections(LinkedHashSet::new, ArrayList::new);
     * }</pre>
     *
     * <p>Output collections must be distinct instances; aliasing is rejected with IllegalArgumentException before consumption.
     * Callers must also avoid distinct wrappers that share mutable backing storage.</p>
     *
     * @param <LC> the type of the first output collection
     * @param <RC> the type of the second output collection
     * @param leftSupplier a supplier that provides the collection for first components; must not return {@code null}
     * @param rightSupplier a supplier that provides the collection for second components; must not return {@code null}
     * @return a {@code Pair} whose left collection contains all first components and whose right collection contains all second components
     * @throws IllegalArgumentException if a collection supplier is {@code null}, returns {@code null}, or returns the same collection for both sides.
     * @throws UnsupportedOperationException if a pair remains and a supplied output collection does not support adding its component
     * @see #unzipToLists(Supplier)
     * @see #unzipToSets(Supplier)
     */
    public <LC extends Collection<A>, RC extends Collection<B>> Pair<LC, RC> unzipToCollections(final Supplier<? extends LC> leftSupplier,
            final Supplier<? extends RC> rightSupplier) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(leftSupplier, cs.leftSupplier);
        N.checkArgNotNull(rightSupplier, cs.rightSupplier);

        final LC collectionA = N.checkArgNotNull(leftSupplier.get(), "leftSupplier.get()");
        final RC collectionB = N.checkArgNotNull(rightSupplier.get(), "rightSupplier.get()");
        N.checkArgument(collectionA != collectionB, "Output collections must be distinct instances");

        this.foreachRemaining((a, b) -> {
            collectionA.add(a);
            collectionB.add(b);
        });

        return Pair.of(collectionA, collectionB);
    }

    /**
     * Unzips all remaining pairs in this {@code BiIterator} into two {@code Set}s.
     *
     * <p>This terminal operation consumes the iterator. Duplicate values are removed
     * independently for each component according to the supplied {@code Set} implementation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.zip(new String[] { "a", "b", "a" }, new Integer[] { 1, 2, 1 });
     * Pair<Set<String>, Set<Integer>> sets = iter.unzipToSets(LinkedHashSet::new);
     * }</pre>
     *
     * <p><b>API Note:</b> one supplier produces both sets, so its element type cannot be checked
     * against {@code A} and {@code B} - the parameter is deliberately raw, with the same caveat
     * described on {@link #unzipToLists(Supplier)}. Prefer
     * {@link #unzipToCollections(Supplier, Supplier)} when the supplier is anything other than a
     * plain constructor reference such as {@code LinkedHashSet::new}.</p>
     *
     * <p>Output collections must be distinct instances; aliasing is rejected with IllegalArgumentException before consumption.
     * Callers must also avoid distinct wrappers that share mutable backing storage.</p>
     *
     * @param supplier a supplier invoked twice to create the left and right sets; each call must return a {@code non-null} {@code Set}
     * @return a {@code Pair} whose left set contains the distinct first components and whose right set contains the distinct second components
     * @throws IllegalArgumentException if a collection supplier is {@code null}, returns {@code null}, or returns the same collection for both sides.
     * @throws UnsupportedOperationException if a pair remains and a supplied output collection does not support adding its component
     * @see #unzipToLists(Supplier)
     * @see #unzipToCollections(Supplier, Supplier)
     */
    public Pair<Set<A>, Set<B>> unzipToSets(@SuppressWarnings("rawtypes") final Supplier<? extends Set> supplier)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Set<A> setA = N.checkArgNotNull(supplier.get(), "supplier.get()");
        final Set<B> setB = N.checkArgNotNull(supplier.get(), "supplier.get()");
        N.checkArgument(setA != setB, "Output collections must be distinct instances");

        this.foreachRemaining((a, b) -> {
            setA.add(a);
            setB.add(b);
        });

        return Pair.of(setA, setB);
    }

    /**
     * Converts all remaining pairs in this {@code BiIterator} to an array of {@code Pair} objects.
     * This method consumes the entire iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter = BiIterator.of(Map.of("a", 1, "b", 2));
     * Pair<String, Integer>[] array = iter.toArray();
     * System.out.println("Array length: " + array.length); // prints: Array length: 2
     * }</pre>
     *
     * @return an array containing all remaining pairs from this BiIterator
     */
    @SuppressWarnings("deprecation")
    public Pair<A, B>[] toArray() {
        return toArray(new Pair[0]);
    }

    /**
     * Converts all remaining pairs in this {@code BiIterator} to an array of the specified type.
     * This method consumes the entire iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new LinkedHashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * Pair<String, Integer>[] arr = BiIterator.of(map).toArray(new Pair[0]); // length is 2
     * // arr[0] -> (a, 1), arr[1] -> (b, 2)
     * }</pre>
     *
     * @param <T> the type of the array elements; it should be a super type of {@code Pair}
     * @param a the array into which the elements of this {@code BiIterator} are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose
     * @return an array containing all remaining pairs from this {@code BiIterator}
     * @throws NullPointerException if {@code a} is {@code null}; rejected before consuming any elements
     * @throws ArrayStoreException if a returned pair cannot be stored in the runtime component type of {@code a}
     * @deprecated This method is deprecated. Use {@link #toArray()} or {@link #toList()} instead.
     */
    @Deprecated
    public <T> T[] toArray(final T[] a) throws NullPointerException, ArrayStoreException {
        N.requireNonNull(a, cs.a);
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
}
