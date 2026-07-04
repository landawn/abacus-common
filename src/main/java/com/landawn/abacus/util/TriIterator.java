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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

/**
 * An abstract, immutable iterator over triples of values of types {@code A}, {@code B}, and {@code C}.
 *
 * <p>Each call to {@link #next()} returns a {@link Triple}{@code <A, B, C>} holding the
 * three values together. The more efficient {@link #forEachRemaining(TriConsumer)} and
 * {@link #foreachRemaining(Throwables.TriConsumer)} variants accept the three components
 * directly, avoiding the creation of intermediate {@code Triple} objects.</p>
 *
 * <p>Static factory methods support creation from arrays, iterables, and generator functions.
 * Transformation methods such as {@link #skip(long)}, {@link #limit(long)}, and
 * {@link #map(TriFunction)} are provided for common pipeline operations. Materialization
 * methods such as {@link #unzipToLists(Supplier)}, {@link #unzipToSets(Supplier)}, and
 * {@link #unzipToCollections(Supplier, Supplier, Supplier)} consume the remaining triples
 * and split the three components into separate result collections.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String[] names = {"Alice", "Bob"};
 * Integer[] ages  = {25, 30};
 * Boolean[] active = {true, false};
 *
 * TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);
 * iter.forEachRemaining((name, age, isActive) ->
 *     System.out.println(name + ", " + age + ", " + isActive));
 * }</pre>
 *
 * @param <A> the first type of elements returned by this iterator
 * @param <B> the second type of elements returned by this iterator
 * @param <C> the third type of elements returned by this iterator
 *
 * @see Triple
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 */
@SuppressWarnings({ "java:S6548" })
public abstract class TriIterator<A, B, C> extends ImmutableIterator<Triple<A, B, C>> {

    /**
     * Constructor for subclasses.
     */
    protected TriIterator() {
    }

    @SuppressWarnings("rawtypes")
    private static final TriIterator EMPTY = new TriIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        protected void next(final Throwables.TriConsumer action) throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public void forEachRemaining(final TriConsumer action) {
            N.checkArgNotNull(action, cs.action);
            // It's empty. Nothing to do.
        }

        @Override
        public void foreachRemaining(final Throwables.TriConsumer action) {
            N.checkArgNotNull(action, cs.action);
            // It's empty. Nothing to do.
        }

        @Override
        public ObjIterator map(final TriFunction mapper) {
            return ObjIterator.empty();
        }
    };

    /**
     * Returns an empty TriIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
     * boolean has = iter.hasNext();                                // returns false
     * List<Triple<String, Integer, Boolean>> list = iter.toList(); // returns empty list
     * }</pre>
     *
     * <p>This is a singleton instance that is reused for all empty TriIterator requests,
     * making it efficient for representing empty iterations.</p>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @return an empty {@code TriIterator}; {@link #hasNext()} always returns {@code false}
     */
    public static <A, B, C> TriIterator<A, B, C> empty() {
        return EMPTY;
    }

    /**
     * Generates an infinite {@code TriIterator} instance with the provided output Consumer.
     * The output Consumer is responsible for producing the next Triple&lt;A, B, C&gt; on each iteration.
     *
     * <p><strong>Warning:</strong> This method creates an infinite iterator. Always use with
     * {@link #limit(long)} or other terminating operations to avoid infinite loops.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(triple -> {
     *     int n = counter.getAndIncrement();
     *     triple.setLeft(n);
     *     triple.setMiddle(n * 2);
     *     triple.setRight(n * 3);
     * });
     * // Use with limit to avoid infinite iteration
     * iter.limit(10).forEachRemaining((a, b, c) -> System.out.println(a + ", " + b + ", " + c));
     * }</pre>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @param output a {@link java.util.function.Consumer Consumer} that accepts a {@code Triple<A, B, C>} and populates it with the next values on each iteration
     * @return a {@code TriIterator<A, B, C>} that uses the provided {@code output} consumer to generate its elements
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @see #generate(BooleanSupplier, Consumer)
     * @see #generate(int, int, IntObjConsumer)
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final Consumer<Triple<A, B, C>> output) {
        return generate(com.landawn.abacus.util.function.BooleanSupplier.TRUE, output);
    }

    /**
     * Generates a TriIterator instance with the provided hasNext BooleanSupplier and output Consumer.
     * The hasNext BooleanSupplier is used to determine if the iterator has more elements.
     * The output Consumer is responsible for producing the next Triple&lt;A, B, C&gt; on each iteration.
     *
     * <p>This method allows creating custom iterators with controlled termination logic.
     * The output consumer should populate the provided Triple object with new values on each call.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * TriIterator<Integer, String, Boolean> iter = TriIterator.generate(
     *     () -> counter.get() < 5,
     *     triple -> {
     *         int n = counter.getAndIncrement();
     *         triple.setLeft(n);
     *         triple.setMiddle("Item " + n);
     *         triple.setRight(n % 2 == 0);
     *     }
     * );
     * iter.forEachRemaining((num, str, bool) ->
     *     System.out.println(num + ": " + str + " (even: " + bool + ")"));
     * }</pre>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @param hasNext a {@link BooleanSupplier} that returns {@code true} if the iterator has more elements
     * @param output a {@link java.util.function.Consumer Consumer} that accepts a {@code Triple<A, B, C>} and populates it with the next values on each iteration
     * @return a {@code TriIterator<A, B, C>} driven by the provided {@code hasNext} and {@code output}
     * @throws IllegalArgumentException if {@code hasNext} or {@code output} is {@code null}
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final BooleanSupplier hasNext, final Consumer<Triple<A, B, C>> output)
            throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(output);

        return new TriIterator<>() {
            private final Triple<A, B, C> tmp = new Triple<>();
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = hasNext.getAsBoolean();
                }

                return hasNextFlag;
            }

            @Override
            public Triple<A, B, C> next() {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                output.accept(tmp);

                return Triple.of(tmp.left(), tmp.middle(), tmp.right());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                output.accept(tmp);

                action.accept(tmp.left(), tmp.middle(), tmp.right());
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNextFlag || hasNext()) {
                    hasNextFlag = false;

                    output.accept(tmp);

                    action.accept(tmp.left(), tmp.middle(), tmp.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNextFlag || hasNext()) {
                    hasNextFlag = false;

                    output.accept(tmp);

                    action.accept(tmp.left(), tmp.middle(), tmp.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
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

                        return mapper.apply(tmp.left(), tmp.middle(), tmp.right());
                    }
                };
            }
        };
    }

    /**
     * Generates a TriIterator instance with the provided fromIndex, toIndex, and output IntObjConsumer.
     * The fromIndex and toIndex define the range and size of the returned iterator.
     * The output IntObjConsumer is responsible for producing the next Triple&lt;A, B, C&gt; on each iteration,
     * receiving the current index and a Triple to populate.
     *
     * <p>This is useful for generating indexed sequences where each triple depends on its position.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 5, (index, triple) -> {
     *     triple.setLeft(index);
     *     triple.setMiddle("Index: " + index);
     *     triple.setRight(Math.sqrt(index));
     * });
     * iter.forEachRemaining((idx, str, sqrt) ->
     *     System.out.println(str + " -> sqrt = " + sqrt));
     * }</pre>
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @param fromIndex the starting index of the iteration range (inclusive)
     * @param toIndex the ending index of the iteration range (exclusive)
     * @param output an {@link IntObjConsumer} that accepts the current integer index and a {@code Triple<A, B, C>} to populate on each iteration
     * @return a {@code TriIterator<A, B, C>} that iterates over indices {@code [fromIndex, toIndex)} and populates each triple via {@code output}
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative or {@code fromIndex} is greater than {@code toIndex}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final int fromIndex, final int toIndex, final IntObjConsumer<Triple<A, B, C>> output)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
        N.checkArgNotNull(output);

        return new TriIterator<>() {
            private final MutableInt cursor = MutableInt.of(fromIndex);
            private final Triple<A, B, C> tmp = new Triple<>();

            @Override
            public boolean hasNext() {
                return cursor.value() < toIndex;
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                return Triple.of(tmp.left(), tmp.middle(), tmp.right());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                action.accept(tmp.left(), tmp.middle(), tmp.right());
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left(), tmp.middle(), tmp.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left(), tmp.middle(), tmp.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {

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

                        return mapper.apply(tmp.left(), tmp.middle(), tmp.right());
                    }
                };
            }
        };
    }

    /**
     * Zips three arrays into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements from the three arrays.
     * If the arrays have different lengths, the resulting TriIterator will have the length of the shortest array.
     * If any of the arrays is {@code null}, returns an empty TriIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * Boolean[] active = {true, false, true};
     * TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);
     * iter.forEachRemaining((name, age, isActive) ->
     *     System.out.println(name + " is " + age + " years old, active: " + isActive));
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <C> the type of elements in the third array
     * @param a the first array, may be {@code null}
     * @param b the second array, may be {@code null}
     * @param c the third array, may be {@code null}
     * @return a TriIterator that iterates over the elements of the three arrays in parallel
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final A[] a, final B[] b, final C[] c) {
        return zip(Array.asList(a), Array.asList(b), Array.asList(c));
    }

    /**
     * Zips three arrays into a TriIterator with specified default values for missing elements.
     * The resulting TriIterator will iterate over triples of elements from the three arrays.
     * If the arrays have different lengths, the resulting TriIterator will continue with the default values
     * for the shorter array until the longest array is exhausted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob"};
     * Integer[] ages = {25, 30, 35};
     * Boolean[] active = {true};
     * TriIterator<String, Integer, Boolean> iter = TriIterator.zip(
     *     names, ages, active, "Unknown", 0, false);
     * iter.forEachRemaining((name, age, isActive) ->
     *     System.out.println(name + ", " + age + ", " + isActive));
     * // Output:
     * // Alice, 25, true
     * // Bob, 30, false     (default for active)
     * // Unknown, 35, false (defaults for name and active)
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <C> the type of elements in the third array
     * @param a the first array, may be {@code null}
     * @param b the second array, may be {@code null}
     * @param c the third array, may be {@code null}
     * @param valueForNoneA the default value for missing elements in the first array
     * @param valueForNoneB the default value for missing elements in the second array
     * @param valueForNoneC the default value for missing elements in the third array
     * @return a TriIterator that iterates over the elements of the three arrays in parallel, using default values for missing elements
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC) {
        return zip(Array.asList(a), Array.asList(b), Array.asList(c), valueForNoneA, valueForNoneB, valueForNoneC);
    }

    /**
     * Zips three iterables into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements from the three iterables.
     * If the iterables have different lengths, the resulting TriIterator will have the length of the shortest iterable.
     * If any of the iterables is {@code null}, returns an empty TriIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
     * List<Integer> ages = Arrays.asList(25, 30);
     * List<String> cities = Arrays.asList("NYC", "LA", "Chicago");
     * TriIterator<String, Integer, String> iter = TriIterator.zip(names, ages, cities);
     * List<Triple<String, Integer, String>> list = iter.toList(); // stops at shortest: size is 2
     * // list -> [(Alice, 25, NYC), (Bob, 30, LA)]
     *
     * TriIterator<String, Integer, String> none = TriIterator.zip((List<String>) null, ages, cities);
     * boolean has = none.hasNext();                 // returns false (null iterable -> empty)
     * }</pre>
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param a the first iterable, may be {@code null}
     * @param b the second iterable, may be {@code null}
     * @param c the third iterable, may be {@code null}
     * @return a TriIterator that iterates over the elements of the three iterables in parallel
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), c == null ? null : c.iterator());
    }

    /**
     * Zips three iterables into a TriIterator with specified default values for missing elements.
     * The resulting TriIterator will iterate over triples of elements from the three iterables.
     * If the iterables have different lengths, the resulting TriIterator will continue with the default values
     * for the shorter iterable until the longest iterable is exhausted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice", "Bob");
     * List<Integer> ages = Arrays.asList(25, 30, 35);
     * List<Boolean> active = Arrays.asList(true);
     * TriIterator<String, Integer, Boolean> iter = TriIterator.zip(
     *     names, ages, active, "Unknown", 0, false);
     * iter.forEachRemaining((name, age, isActive) ->
     *     System.out.println(name + ", " + age + ", " + isActive));
     * // Output:
     * // Alice, 25, true
     * // Bob, 30, false     (default for active)
     * // Unknown, 35, false (defaults for name and active)
     * }</pre>
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param a the first iterable, may be {@code null}
     * @param b the second iterable, may be {@code null}
     * @param c the third iterable, may be {@code null}
     * @param valueForNoneA the default value for missing elements in the first iterable
     * @param valueForNoneB the default value for missing elements in the second iterable
     * @param valueForNoneC the default value for missing elements in the third iterable
     * @return a TriIterator that iterates over the elements of the three iterables in parallel, using default values for missing elements
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), c == null ? null : c.iterator(), valueForNoneA, valueForNoneB,
                valueForNoneC);
    }

    /**
     * Zips three iterators into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements from the three iterators.
     * If the iterators have different lengths, the resulting TriIterator will have the length of the shortest iterator.
     * If any of the iterators is {@code null}, returns an empty TriIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
     * List<Integer> ages = Arrays.asList(25, 30, 35);
     * List<String> cities = Arrays.asList("NYC", "LA", "Chicago");
     * TriIterator<String, Integer, String> iter = TriIterator.zip(
     *     names.iterator(), ages.iterator(), cities.iterator());
     * iter.forEachRemaining((name, age, city) ->
     *     System.out.println(name + " is " + age + " from " + city));
     * }</pre>
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <C> the type of elements in the third iterator
     * @param iterA the first iterator, may be {@code null}
     * @param iterB the second iterator, may be {@code null}
     * @param iterC the third iterator, may be {@code null}
     * @return a TriIterator that iterates over the elements of the three iterators in parallel
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterator<A> iterA, final Iterator<B> iterB, final Iterator<C> iterC) {
        if (iterA == null || iterB == null || iterC == null) {
            return empty();
        }

        return new TriIterator<>() {
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
                }

                return hasNextFlag;
            }

            @Override
            public Triple<A, B, C> next() {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Triple.of(iterA.next(), iterB.next(), iterC.next());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                action.accept(iterA.next(), iterB.next(), iterC.next());
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {

                while (hasNextFlag || (iterA.hasNext() && iterB.hasNext() && iterC.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iterA.next(), iterB.next(), iterC.next());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNextFlag || (iterA.hasNext() && iterB.hasNext() && iterC.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iterA.next(), iterB.next(), iterC.next());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            hasNextFlag = iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // Reset for the next call

                        return mapper.apply(iterA.next(), iterB.next(), iterC.next());
                    }
                };
            }
        };
    }

    /**
     * Zips three iterators into a TriIterator with specified default values for missing elements.
     * The resulting TriIterator will iterate over triples of elements from the three iterators.
     * If the iterators have different lengths, the resulting TriIterator will continue with the default values
     * for the shorter iterator until the longest iterator is exhausted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice", "Bob");
     * List<Integer> ages = Arrays.asList(25, 30, 35);
     * List<String> cities = Arrays.asList("NYC");
     * TriIterator<String, Integer, String> iter = TriIterator.zip(
     *     names.iterator(), ages.iterator(), cities.iterator(),
     *     "Unknown", 0, "N/A");
     * iter.forEachRemaining((name, age, city) ->
     *     System.out.println(name + ", " + age + ", " + city));
     * // Output:
     * // Alice, 25, NYC
     * // Bob, 30, N/A       (default for city)
     * // Unknown, 35, N/A   (defaults for name and city)
     * }</pre>
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <C> the type of elements in the third iterator
     * @param iterA the first iterator, may be {@code null}
     * @param iterB the second iterator, may be {@code null}
     * @param iterC the third iterator, may be {@code null}
     * @param valueForNoneA the default value for missing elements in the first iterator
     * @param valueForNoneB the default value for missing elements in the second iterator
     * @param valueForNoneC the default value for missing elements in the third iterator
     * @return a TriIterator that iterates over the elements of the three iterators in parallel, using default values for missing elements
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterator<A> iterA, final Iterator<B> iterB, final Iterator<C> iterC, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC) {
        final Iterator<A> iter1 = iterA == null ? ObjIterator.empty() : iterA;
        final Iterator<B> iter2 = iterB == null ? ObjIterator.empty() : iterB;
        final Iterator<C> iter3 = iterC == null ? ObjIterator.empty() : iterC;

        return new TriIterator<>() {
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = iter1.hasNext() || iter2.hasNext() || iter3.hasNext();
                }

                return hasNextFlag;
            }

            @Override
            public Triple<A, B, C> next() {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Triple.of(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                        iter3.hasNext() ? iter3.next() : valueForNoneC);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                        iter3.hasNext() ? iter3.next() : valueForNoneC);
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNextFlag || (iter1.hasNext() || iter2.hasNext() || iter3.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                            iter3.hasNext() ? iter3.next() : valueForNoneC);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNextFlag || (iter1.hasNext() || iter2.hasNext() || iter3.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                            iter3.hasNext() ? iter3.next() : valueForNoneC);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            hasNextFlag = iter1.hasNext() || iter2.hasNext() || iter3.hasNext();
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // Reset for the next call

                        return mapper.apply(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                                iter3.hasNext() ? iter3.next() : valueForNoneC);
                    }
                };
            }
        };
    }

    /**
     * Unzips an iterator of elements into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements produced by the unzip function.
     * If the iterator is {@code null}, an empty TriIterator is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Unzip an iterator of complex objects into three components
     * List<String> data = Arrays.asList("John:Developer:5", "Jane:Manager:8", "Bob:Analyst:3");
     * TriIterator<String, String, Integer> iter = TriIterator.unzip(data.iterator(), (item, triple) -> {
     *     String[] parts = item.split(":");
     *     triple.setLeft(parts[0]);                    // name
     *     triple.setMiddle(parts[1]);                  // role
     *     triple.setRight(Integer.parseInt(parts[2])); // years
     * });
     * iter.forEachRemaining((name, role, years) ->
     *     System.out.println(name + " works as " + role + " for " + years + " years"));
     * }</pre>
     *
     * @param <T> the type of elements in the input iterator
     * @param <A> the type of elements in the first component of the triple
     * @param <B> the type of elements in the second component of the triple
     * @param <C> the type of elements in the third component of the triple
     * @param iter the input iterator, may be {@code null}; returns an empty {@code TriIterator} when {@code null}
     * @param unzipFunction a BiConsumer that accepts an element of type T and a Triple&lt;A, B, C&gt; and populates the triple with the unzipped values, must not be {@code null}
     * @return a TriIterator that iterates over the unzipped elements
     * @throws IllegalArgumentException if {@code unzipFunction} is {@code null}
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunction) {
        N.checkArgNotNull(unzipFunction, cs.function);

        if (iter == null) {
            return TriIterator.empty();
        }

        final BooleanSupplier booleanSupplier = iter::hasNext;

        final Consumer<Triple<A, B, C>> output = out -> unzipFunction.accept(iter.next(), out);

        return TriIterator.generate(booleanSupplier, output);
    }

    /**
     * Unzips an iterable of elements into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements produced by the unzip function.
     * If the iterable is {@code null}, an empty TriIterator is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Unzip a list of CSV strings into three separate components
     * List<String> records = Arrays.asList("Alice,25,NYC", "Bob,30,LA", "Charlie,35,Chicago");
     * TriIterator<String, Integer, String> iter = TriIterator.unzip(records, (record, triple) -> {
     *     String[] parts = record.split(",");
     *     triple.setLeft(parts[0]);
     *     triple.setMiddle(Integer.parseInt(parts[1]));
     *     triple.setRight(parts[2]);
     * });
     * iter.forEachRemaining((name, age, city) ->
     *     System.out.println(name + " is " + age + " from " + city));
     * }</pre>
     *
     * @param <T> the type of elements in the input iterable
     * @param <A> the type of elements in the first component of the triple
     * @param <B> the type of elements in the second component of the triple
     * @param <C> the type of elements in the third component of the triple
     * @param iter the input iterable, may be {@code null}; returns an empty {@code TriIterator} when {@code null}
     * @param unzipFunction a BiConsumer that accepts an element of type T and a {@code Triple<A, B, C>} and populates the triple with the unzipped values, must not be {@code null}
     * @return a TriIterator that iterates over the unzipped elements
     * @throws IllegalArgumentException if {@code unzipFunction} is {@code null}
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzip(final Iterable<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunction) {
        N.checkArgNotNull(unzipFunction, cs.function);

        if (iter == null) {
            return TriIterator.empty();
        }

        return unzip(iter.iterator(), unzipFunction);
    }

    /**
     * Unzips an iterator into three separate collections using the provided unzip function and independent collection suppliers.
     *
     * <p><b>Note:</b> because the iterator's length is unknown, the {@code leftSupplier},
     * {@code middleSupplier}, and {@code rightSupplier} {@code IntFunction}s are always invoked
     * with a size hint of {@code 0}. A pre-allocating supplier such as {@code size -> new ArrayList<>(size)}
     * will therefore be given capacity {@code 0}. Use the {@code Iterable} overload (which extracts
     * the size from a {@code Collection}) when an accurate size hint is required.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> records = Arrays.asList("Alice,25,NYC", "Bob,30,LA").iterator();
     * Triple<Set<String>, List<Integer>, Queue<String>> result = TriIterator.unzip(records, (record, out) -> {
     *     String[] parts = record.split(",");
     *     out.set(parts[0], Integer.parseInt(parts[1]), parts[2]);
     * }, size -> new LinkedHashSet<>(), size -> new ArrayList<>(size), size -> new ArrayDeque<>(size));
     * }</pre>
     *
     * @param <T> the type of elements in the input iterator
     * @param <A> the type of elements in the first output collection
     * @param <B> the type of elements in the second output collection
     * @param <C> the type of elements in the third output collection
     * @param <LC> the type of the first output collection
     * @param <MC> the type of the second output collection
     * @param <RC> the type of the third output collection
     * @param iter the input iterator, may be {@code null}; returns empty output collections when {@code null}
     * @param unzipFunction a BiConsumer that accepts an element of type T and a {@code Triple<A, B, C>} and populates the triple with the unzipped values, must not be {@code null}
     * @param leftSupplier a function that provides the first output collection; always called with a size hint of {@code 0}
     * @param middleSupplier a function that provides the second output collection; always called with a size hint of {@code 0}
     * @param rightSupplier a function that provides the third output collection; always called with a size hint of {@code 0}
     * @return a {@code Triple} containing the three output collections
     * @throws IllegalArgumentException if {@code unzipFunction}, any supplier, or any supplied collection is {@code null}
     */
    public static <T, A, B, C, LC extends Collection<A>, MC extends Collection<B>, RC extends Collection<C>> Triple<LC, MC, RC> unzip(
            final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunction, final IntFunction<? extends LC> leftSupplier,
            final IntFunction<? extends MC> middleSupplier, final IntFunction<? extends RC> rightSupplier) {
        N.checkArgNotNull(leftSupplier, cs.supplier);
        N.checkArgNotNull(middleSupplier, cs.supplier);
        N.checkArgNotNull(rightSupplier, cs.supplier);

        return unzip(iter, unzipFunction).unzipToCollections(() -> leftSupplier.apply(0), () -> middleSupplier.apply(0), () -> rightSupplier.apply(0));
    }

    /**
     * Unzips an iterable into three separate collections using the provided unzip function and independent collection suppliers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> records = Arrays.asList("Alice,25,NYC", "Bob,30,LA");
     * Triple<Set<String>, List<Integer>, Queue<String>> result = TriIterator.unzip(records, (record, out) -> {
     *     String[] parts = record.split(",");
     *     out.set(parts[0], Integer.parseInt(parts[1]), parts[2]);
     * }, size -> new LinkedHashSet<>(), size -> new ArrayList<>(size), size -> new ArrayDeque<>(size));
     * }</pre>
     *
     * @param <T> the type of elements in the input iterable
     * @param <A> the type of elements in the first output collection
     * @param <B> the type of elements in the second output collection
     * @param <C> the type of elements in the third output collection
     * @param <LC> the type of the first output collection
     * @param <MC> the type of the second output collection
     * @param <RC> the type of the third output collection
     * @param iter the input iterable, may be {@code null}; returns empty output collections when {@code null}
     * @param unzipFunction a BiConsumer that accepts an element of type T and a {@code Triple<A, B, C>} and populates the triple with the unzipped values, must not be {@code null}
     * @param leftSupplier a function that provides the first output collection
     * @param middleSupplier a function that provides the second output collection
     * @param rightSupplier a function that provides the third output collection
     * @return a {@code Triple} containing the three output collections
     * @throws IllegalArgumentException if {@code unzipFunction}, any supplier, or any supplied collection is {@code null}
     */
    public static <T, A, B, C, LC extends Collection<A>, MC extends Collection<B>, RC extends Collection<C>> Triple<LC, MC, RC> unzip(
            final Iterable<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunction, final IntFunction<? extends LC> leftSupplier,
            final IntFunction<? extends MC> middleSupplier, final IntFunction<? extends RC> rightSupplier) {
        N.checkArgNotNull(leftSupplier, cs.supplier);
        N.checkArgNotNull(middleSupplier, cs.supplier);
        N.checkArgNotNull(rightSupplier, cs.supplier);

        final int len = iter instanceof Collection ? ((Collection<?>) iter).size() : 0;

        return unzip(iter, unzipFunction).unzipToCollections(() -> leftSupplier.apply(len), () -> middleSupplier.apply(len), () -> rightSupplier.apply(len));
    }

    /**
     * Retrieves the next triple of elements from this iterator and passes them to the specified action.
     * This is an optimized version of {@link #next()} that avoids creating a Triple object.
     *
     * <p>This protected method is designed for subclass implementations to efficiently process
     * the next triple of elements without the overhead of object creation. The three elements
     * are passed directly to the action consumer.</p>
     *
     * <p><b>Example implementation:</b></p>
     * <pre>{@code
     * protected <E extends Exception> void next(Throwables.TriConsumer<A, B, C, E> action) {
     *     if (!hasNext()) {
     *         throw new NoSuchElementException();
     *     }
     *     A first = getNextA();
     *     B second = getNextB();
     *     C third = getNextC();
     *     action.accept(first, second, third);
     * }
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action a TriConsumer that accepts the next three elements from this iterator
     * @throws NoSuchElementException if the iteration has no more elements
     * @throws E if the action throws an exception
     */
    protected abstract <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
            throws NoSuchElementException, E;

    /**
     * Performs the given action for each remaining element in the iterator until all elements
     * have been processed or the action throws an exception.
     *
     * <p>This overload wraps each triple in a {@link Triple} object before passing it to the action.
     * Prefer {@link #forEachRemaining(TriConsumer)} to avoid creating the unnecessary {@code Triple} objects.</p>
     *
     * @param action the action to be performed for each {@code Triple} element, must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @deprecated use {@link #forEachRemaining(TriConsumer)} to avoid creating the unnecessary {@code Triple} objects.
     * @see #forEachRemaining(TriConsumer)
     */
    @Deprecated
    @Override
    public void forEachRemaining(final Consumer<? super Triple<A, B, C>> action) {
        N.checkArgNotNull(action, cs.action);

        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element in the iterator until all elements
     * have been processed or the action throws an exception.
     *
     * <p>This is the preferred method for iterating over triples compared to calling {@link #next()}
     * repeatedly, as it avoids the overhead of creating Triple objects. The three elements are passed
     * directly to the action consumer, providing better performance and more convenient access to the
     * individual components.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * String[] cities = {"NYC", "LA", "Chicago"};
     * TriIterator<String, Integer, String> iter = TriIterator.zip(names, ages, cities);
     * iter.forEachRemaining((name, age, city) ->
     *     System.out.println(name + " (" + age + ") lives in " + city));
     * }</pre>
     *
     * @param action the action to be performed for each remaining triple, must not be {@code null}
     */
    public abstract void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action);

    /**
     * Performs the given action for each remaining triple in this iterator.
     * This variant supports actions that may throw checked exceptions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriIterator<String, Integer, String> iter = TriIterator.zip(
     *         List.of("u1", "u2"),
     *         List.of(20, 30),
     *         List.of("A", "B"));
     * iter.foreachRemaining((id, age, level) -> {
     *     consume(id, age, level);
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each remaining triple, must not be {@code null}
     * @throws E if the action throws an exception
     */
    public abstract <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E; // NOSONAR

    @SuppressWarnings("rawtypes")
    private static final Throwables.TriConsumer DO_NOTHING = (a, b, c) -> {
        // do nothing;
    };

    /**
     * Returns a new {@code TriIterator} that skips the first {@code n} triples of elements.
     * The resulting iterator will begin iteration after discarding the specified number of elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] nums = {1, 2, 3, 4, 5};
     * String[] strs = {"a", "b", "c", "d", "e"};
     * Boolean[] flags = {true, false, true, false, true};
     * TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(2);
     * iter.forEachRemaining((num, str, flag) ->
     *     System.out.println(num + ", " + str + ", " + flag));
     * // Output: (3, c, true), (4, d, false), (5, e, true)
     * }</pre>
     *
     * @param n the number of triples to skip from the beginning, must be non-negative
     * @return a new TriIterator that begins after skipping {@code n} triples, or this iterator if {@code n} is 0
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public TriIterator<A, B, C> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final TriIterator<A, B, C> iter = this;

        return new TriIterator<>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!skipped) {
                    skip();
                }

                iter.next(action);
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                if (!skipped) {
                    skip();
                }

                iter.forEachRemaining(action);
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                if (!skipped) {
                    skip();
                }

                iter.foreachRemaining(action);
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                N.checkArgNotNull(mapper, cs.mapper);

                if (!skipped) {
                    skip();
                }

                return iter.map(mapper);
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.next(DO_NOTHING);
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new TriIterator with a limited number of elements.
     * The resulting TriIterator will contain at most the specified number of elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an infinite iterator and limit it
     * AtomicInteger counter = new AtomicInteger(0);
     * TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(triple -> {
     *     int n = counter.getAndIncrement();
     *     triple.set(n, n * 2, n * 3);
     * }).limit(5);
     * iter.forEachRemaining((a, b, c) ->
     *     System.out.println(a + ", " + b + ", " + c));
     * // Output: (0,0,0), (1,2,3), (2,4,6), (3,6,9), (4,8,12)
     * }</pre>
     *
     * @param count the maximum number of triples to include, must be non-negative
     * @return a new TriIterator limited to {@code count} triples, or an empty iterator if {@code count} is 0
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public TriIterator<A, B, C> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return TriIterator.empty();
        }

        final TriIterator<A, B, C> iter = this;

        return new TriIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                iter.next(action);
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNext()) {
                    cnt--;
                    iter.next(action);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNext()) {
                    cnt--;
                    iter.next(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                if (cnt > 0) {
                    return iter.<R> map(mapper).limit(cnt);
                } else {
                    return ObjIterator.empty();
                }
            }
        };
    }

    /**
     * Returns a new {@code TriIterator} that includes only the triples satisfying the provided predicate.
     * Triples for which the predicate returns {@code false} are skipped during iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] nums = {1, 2, 3, 4, 5};
     * String[] strs = {"a", "bb", "ccc", "dddd", "eeeee"};
     * Boolean[] flags = {true, false, true, false, true};
     * TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags)
     *     .filter((num, str, flag) -> flag && num > 2);
     * iter.forEachRemaining((num, str, flag) ->
     *     System.out.println(num + ", " + str + ", " + flag));
     * // Output: (3, ccc, true), (5, eeeee, true)
     * }</pre>
     *
     * @param predicate the predicate to apply to each triple of elements, must not be {@code null}
     * @return a new {@code TriIterator} containing only the triples that satisfy the predicate
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     */
    public TriIterator<A, B, C> filter(final TriPredicate<? super A, ? super B, ? super C> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        final TriIterator<A, B, C> iter = this;

        return new TriIterator<>() {
            private final Triple<A, B, C> next = new Triple<>();
            private final Throwables.TriConsumer<A, B, C, RuntimeException> setNext = next::set;

            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    while (iter.hasNext()) {
                        iter.next(setNext);

                        if (predicate.test(next.left(), next.middle(), next.right())) {
                            hasNextFlag = true;
                            break;
                        }
                    }
                }

                return hasNextFlag;
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNextFlag && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false;

                return next.copy();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!hasNextFlag && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false;

                action.accept(next.left(), next.middle(), next.right());
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNext()) {
                    hasNextFlag = false;

                    action.accept(next.left(), next.middle(), next.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNext()) {
                    hasNextFlag = false;

                    action.accept(next.left(), next.middle(), next.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            while (iter.hasNext()) {
                                iter.next(setNext);

                                if (predicate.test(next.left(), next.middle(), next.right())) {
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

                        return mapper.apply(next.left(), next.middle(), next.right());
                    }
                };
            }
        };
    }

    /**
     * Transforms the elements of this TriIterator using the given mapper function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * String[] cities = {"NYC", "LA", "Chicago"};
     * ObjIterator<String> iter = TriIterator.zip(names, ages, cities)
     *     .map((name, age, city) -> name + " (" + age + ") from " + city);
     * iter.forEachRemaining(System.out::println);
     * // Output:
     * // Alice (25) from NYC
     * // Bob (30) from LA
     * // Charlie (35) from Chicago
     * }</pre>
     *
     * @param <R> the type of elements in the resulting ObjIterator
     * @param mapper the function to apply to each triple of elements, must not be {@code null}
     * @return an {@code ObjIterator} containing the elements produced by applying {@code mapper} to each triple
     */
    public abstract <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper);

    /**
     * Converts this {@code TriIterator} into a {@link Stream} of {@link Triple}s for further stream processing.
     * The resulting stream will contain all remaining triples from this iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * String[] cities = {"NYC", "LA", "Chicago"};
     * Stream<Triple<String, Integer, String>> stream = TriIterator.zip(names, ages, cities).stream();
     * stream.forEach(System.out::println);
     * }</pre>
     *
     * @return a {@code Stream} containing the remaining triples in this {@code TriIterator}
     * @see #stream(TriFunction)
     */
    @Beta
    public Stream<Triple<A, B, C>> stream() {
        return stream(Triple::of);
    }

    /**
     * Returns a {@link Stream} of elements produced by applying the given mapper function to each
     * triple of elements in this {@code TriIterator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * String[] cities = {"NYC", "LA", "Chicago"};
     * Stream<String> stream = TriIterator.zip(names, ages, cities)
     *     .stream((name, age, city) -> name + " (" + age + ") - " + city);
     * stream.forEach(System.out::println);
     * // Output:
     * // Alice (25) - NYC
     * // Bob (30) - LA
     * // Charlie (35) - Chicago
     * }</pre>
     *
     * @param <R> the type of elements in the resulting Stream
     * @param mapper the function to apply to each triple of elements, must not be {@code null}
     * @return a {@code Stream} containing the elements produced by applying {@code mapper} to each triple
     * @see #stream()
     * @see #map(TriFunction)
     */
    public <R> Stream<R> stream(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
        return Stream.of(map(mapper));
    }

    /**
     * Unzips all remaining triples in this {@code TriIterator} into three {@code List}s.
     *
     * <p>This terminal operation consumes the iterator. The returned {@code Triple} holds
     * the first component values in {@link Triple#left()}, the second component values in
     * {@link Triple#middle()}, and the third component values in {@link Triple#right()},
     * preserving the iterator order within each list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * Boolean[] active = {true, false, true};
     * Triple<List<String>, List<Integer>, List<Boolean>> result =
     *     TriIterator.zip(names, ages, active).unzipToLists(ArrayList::new);
     * List<String> nameList = result.left();       // returns [Alice, Bob, Charlie]
     * List<Integer> ageList = result.middle();     // returns [25, 30, 35]
     * List<Boolean> activeList = result.right();   // returns [true, false, true]
     * }</pre>
     *
     * @param supplier a supplier invoked three times to create the left, middle, and right lists; each call must return a non-null {@code List}
     * @return a {@code Triple} whose left, middle, and right lists contain all first, second, and third components, respectively
     * @throws IllegalArgumentException if {@code supplier} is {@code null} or any call returns {@code null}
     * @see #unzipToCollections(Supplier, Supplier, Supplier)
     */
    public Triple<List<A>, List<B>, List<C>> unzipToLists(@SuppressWarnings("rawtypes") final Supplier<? extends List> supplier) {
        N.checkArgNotNull(supplier, cs.supplier);

        final List<A> listA = N.checkArgNotNull(supplier.get(), cs.supplier);
        final List<B> listB = N.checkArgNotNull(supplier.get(), cs.supplier);
        final List<C> listC = N.checkArgNotNull(supplier.get(), cs.supplier);

        this.foreachRemaining((a, b, c) -> {
            listA.add(a);
            listB.add(b);
            listC.add(c);
        });

        return Triple.of(listA, listB, listC);
    }

    /**
     * Unzips all remaining triples in this {@code TriIterator} into three independently supplied collections.
     *
     * <p>This terminal operation consumes the iterator. Use this overload when the three
     * components should be collected into different collection implementations or when each
     * component needs a separate sizing policy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriIterator<String, Integer, Boolean> iter = TriIterator.zip(
     *     new String[] { "a", "b" }, new Integer[] { 1, 2 }, new Boolean[] { true, false });
     * Triple<Set<String>, List<Integer>, List<Boolean>> result = iter.unzipToCollections(LinkedHashSet::new, ArrayList::new, ArrayList::new);
     * }</pre>
     *
     * @param <LC> the type of the first output collection
     * @param <MC> the type of the second output collection
     * @param <RC> the type of the third output collection
     * @param leftSupplier a supplier that provides the collection for first components, must not be {@code null} or return {@code null}
     * @param middleSupplier a supplier that provides the collection for second components, must not be {@code null} or return {@code null}
     * @param rightSupplier a supplier that provides the collection for third components, must not be {@code null} or return {@code null}
     * @return a {@code Triple} whose left, middle, and right collections contain all first, second, and third components, respectively
     * @throws IllegalArgumentException if any supplier or supplied collection is {@code null}
     * @see #unzipToLists(Supplier)
     * @see #unzipToSets(Supplier)
     */
    public <LC extends Collection<A>, MC extends Collection<B>, RC extends Collection<C>> Triple<LC, MC, RC> unzipToCollections(
            final Supplier<? extends LC> leftSupplier, final Supplier<? extends MC> middleSupplier, final Supplier<? extends RC> rightSupplier) {
        N.checkArgNotNull(leftSupplier, cs.supplier);
        N.checkArgNotNull(middleSupplier, cs.supplier);
        N.checkArgNotNull(rightSupplier, cs.supplier);

        final LC collectionA = N.checkArgNotNull(leftSupplier.get(), cs.supplier);
        final MC collectionB = N.checkArgNotNull(middleSupplier.get(), cs.supplier);
        final RC collectionC = N.checkArgNotNull(rightSupplier.get(), cs.supplier);

        this.foreachRemaining((a, b, c) -> {
            collectionA.add(a);
            collectionB.add(b);
            collectionC.add(c);
        });

        return Triple.of(collectionA, collectionB, collectionC);
    }

    /**
     * Unzips all remaining triples in this {@code TriIterator} into three {@code Set}s.
     *
     * <p>This terminal operation consumes the iterator. Duplicate values are removed
     * independently for each component according to the supplied {@code Set} implementation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Alice"};
     * Integer[] ages = {25, 30, 25};
     * Boolean[] active = {true, false, true};
     * Triple<Set<String>, Set<Integer>, Set<Boolean>> result =
     *     TriIterator.zip(names, ages, active).unzipToSets(HashSet::new);
     * Set<String> nameSet = result.left();       // returns [Alice, Bob] (duplicates removed)
     * Set<Integer> ageSet = result.middle();     // returns [25, 30]
     * Set<Boolean> activeSet = result.right();   // returns [true, false]
     * }</pre>
     *
     * @param supplier a supplier invoked three times to create the left, middle, and right sets; each call must return a non-null {@code Set}
     * @return a {@code Triple} whose left, middle, and right sets contain the distinct first, second, and third components, respectively
     * @throws IllegalArgumentException if {@code supplier} is {@code null} or any call returns {@code null}
     * @see #unzipToCollections(Supplier, Supplier, Supplier)
     */
    public Triple<Set<A>, Set<B>, Set<C>> unzipToSets(@SuppressWarnings("rawtypes") final Supplier<? extends Set> supplier) {
        N.checkArgNotNull(supplier, cs.supplier);

        final Set<A> setA = N.checkArgNotNull(supplier.get(), cs.supplier);
        final Set<B> setB = N.checkArgNotNull(supplier.get(), cs.supplier);
        final Set<C> setC = N.checkArgNotNull(supplier.get(), cs.supplier);

        this.foreachRemaining((a, b, c) -> {
            setA.add(a);
            setB.add(b);
            setC.add(c);
        });

        return Triple.of(setA, setB, setC);
    }

    /**
     * Converts all remaining triples in this TriIterator to an array of Triple objects.
     * This method consumes the entire iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * String[] cities = {"NYC", "LA", "Chicago"};
     * Triple<String, Integer, String>[] array = TriIterator.zip(names, ages, cities).toArray();
     * for (Triple<String, Integer, String> triple : array) {
     *     System.out.println(triple.left() + " - " + triple.middle() + " - " + triple.right());
     * }
     * }</pre>
     *
     * @return an array containing all remaining triples from this TriIterator
     */
    @SuppressWarnings("deprecation")
    public Triple<A, B, C>[] toArray() {
        return toArray(new Triple[0]);
    }

    /**
     * Converts all remaining triples in this TriIterator to an array of the specified type.
     * This method consumes the entire iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob"};
     * Integer[] ages = {25, 30};
     * String[] cities = {"NYC", "LA"};
     * Triple<String, Integer, String>[] arr =
     *     TriIterator.zip(names, ages, cities).toArray(new Triple[0]); // length is 2
     * // arr[0] -> (Alice, 25, NYC), arr[1] -> (Bob, 30, LA)
     * }</pre>
     *
     * @param <T> the type of the array elements; it should be a super type of {@code Triple}
     * @param a the array into which the elements of this TriIterator are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose
     * @return an array containing all remaining triples from this TriIterator
     * @deprecated This method is deprecated. Use {@link #toArray()} or {@link #toList()} instead.
     */
    @Deprecated
    public <T> T[] toArray(final T[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts all remaining triples in this TriIterator to a List of Triple objects.
     * This method consumes the entire iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * String[] cities = {"NYC", "LA", "Chicago"};
     * List<Triple<String, Integer, String>> list = TriIterator.zip(names, ages, cities).toList();
     * list.forEach(triple ->
     *     System.out.println(triple.left() + " is " + triple.middle() + " from " + triple.right()));
     * }</pre>
     *
     * @return a List containing all remaining triples from this TriIterator
     */
    public List<Triple<A, B, C>> toList() {
        return toCollection(Suppliers.ofList());
    }
}
