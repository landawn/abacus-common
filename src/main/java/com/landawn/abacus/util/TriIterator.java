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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

/**
 * The TriIterator class is an abstract class that extends ImmutableIterator.
 * It represents an iterator over a triple of values of type A, B, and C.
 * This class provides a blueprint for classes that need to implement a tri-directional iterator.
 *
 * @param <A> the first type of elements returned by this iterator
 * @param <B> the second type of elements returned by this iterator
 * @param <C> the third type of elements returned by this iterator
 *
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
            // It's empty. Nothing to do.
        }

        @Override
        public void foreachRemaining(final Throwables.TriConsumer action) {
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
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @return an empty TriIterator
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
     * @param output A Consumer that accepts a Triple&lt;A, B, C&gt; and produces the next Triple&lt;A, B, C&gt; on each iteration.
     * @return A TriIterator&lt;A, B, C&gt; that uses the provided output Consumer to generate its elements.
     * @see  #generate(BooleanSupplier, Consumer)
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
     *     () -> counter.get() < 5,  // hasNext logic
     *     triple -> {               // output logic
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
     * @param hasNext A BooleanSupplier that returns {@code true} if the iterator has more elements.
     * @param output A Consumer that accepts a Triple&lt;A, B, C&gt; and produces the next Triple&lt;A, B, C&gt; on each iteration.
     * @return A TriIterator&lt;A, B, C&gt; that uses the provided hasNext BooleanSupplier and output Consumer to generate its elements.
     * @throws IllegalArgumentException If hasNext or output is {@code null}.
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
            public Triple<A, B, C> next() throws IllegalArgumentException {
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
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
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
     * @param fromIndex The starting index of the iterator (inclusive).
     * @param toIndex The ending index of the iterator (exclusive).
     * @param output An IntObjConsumer that accepts an integer index and a Triple&lt;A, B, C&gt; to populate on each iteration.
     * @return A TriIterator&lt;A, B, C&gt; that uses the provided fromIndex, toIndex, and output IntObjConsumer to generate its elements.
     * @throws IllegalArgumentException If fromIndex is greater than toIndex.
     * @throws IndexOutOfBoundsException If fromIndex or toIndex is out of range.
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
            public Triple<A, B, C> next() throws IllegalArgumentException {
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
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
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
     * If any of arrays is {@code null}, returns an empty TriIterator.
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
     * @param a the first array
     * @param b the second array
     * @param c the third array
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
     * @param a the first array
     * @param b the second array
     * @param c the third array
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
     * If any of iterable is {@code null}, returns an empty TriIterator.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param a the first iterable
     * @param b the second iterable
     * @param c the third iterable
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
     * @param a the first iterable
     * @param b the second iterable
     * @param c the third iterable
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
     * If any of iterator is {@code null}, returns an empty TriIterator.
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
     * @param iterA the first iterator
     * @param iterB the second iterator
     * @param iterC the third iterator
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
            public Triple<A, B, C> next() throws IllegalArgumentException {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Triple.of(iterA.next(), iterB.next(), iterC.next());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                // N.checkArgNotNull(action);
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
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
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
     * @param iterA the first iterator
     * @param iterB the second iterator
     * @param iterC the third iterator
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
            public Triple<A, B, C> next() throws IllegalArgumentException {
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
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
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
     * @param iter the input iterable
     * @param unzipFunction a BiConsumer that accepts an element of type T and a {@code Triple<A, B, C>} and populates the triple with the unzipped values
     * @return a TriIterator that iterates over the unzipped elements
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzip(final Iterable<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunction) {
        if (iter == null) {
            return TriIterator.empty();
        }

        return unzip(iter.iterator(), unzipFunction);
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
     *     triple.setLeft(parts[0]);         // name
     *     triple.setMiddle(parts[1]);       // role
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
     * @param iter the input iterator
     * @param unzipFunction a BiConsumer that accepts an element of type T and a Triple&lt;A, B, C&gt; and populates the triple with the unzipped values
     * @return a TriIterator that iterates over the unzipped elements
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunction) {
        if (iter == null) {
            return TriIterator.empty();
        }

        final BooleanSupplier booleanSupplier = iter::hasNext;

        final Consumer<Triple<A, B, C>> output = out -> unzipFunction.accept(iter.next(), out);

        return TriIterator.generate(booleanSupplier, output);
    }

    /**
     * Retrieves the next triple of elements from this iterator and passes them to the specified action.
     * This is an optimized version of {@link #next()} that avoids creating a Triple object.
     *
     * <p>This protected method is designed for subclass implementations to efficiently process
     * the next triple of elements without the overhead of object creation. The three elements
     * are passed directly to the action consumer.</p>
     *
     * <p>Example implementation:
     * 
     * <p><b>Usage Examples:</b></p>
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
     * Performs the given action for each remaining element in the iterator until all elements have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each element
     * @deprecated use {@code forEachRemaining(TriConsumer)} to avoid creating the unnecessary {@code Triple} Objects.
     * @see #forEachRemaining(TriConsumer)
     */
    @Deprecated
    @Override
    public void forEachRemaining(final Consumer<? super Triple<A, B, C>> action) {
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
     * @param action the action to be performed for each element
     */
    public abstract void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action);

    /**
     * Performs the given action for each remaining element in the iterator until all elements
     * have been processed or the action throws an exception.
     *
     * <p>This method is similar to {@link #forEachRemaining(TriConsumer)} but supports actions that may throw
     * checked exceptions. This is particularly useful when the iteration logic involves I/O operations,
     * database access, or other operations that declare checked exceptions, eliminating the need to
     * wrap exceptions in unchecked RuntimeExceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] ids = {"user1", "user2", "user3"};
     * String[] names = {"Alice", "Bob", "Charlie"};
     * String[] emails = {"alice@example.com", "bob@example.com", "charlie@example.com"};
     * TriIterator<String, String, String> iter = TriIterator.zip(ids, names, emails);
     *
     * // Example with IOException - no try-catch needed at call site
     * iter.foreachRemaining((id, name, email) -> {
     *     writeToFile(id, name, email);  // method that throws IOException
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element
     * @throws E if the action throws an exception
     */
    public abstract <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E; // NOSONAR

    @SuppressWarnings("rawtypes")
    private static final Throwables.TriConsumer DO_NOTHING = (a, b, c) -> {
        // do nothing;
    };

    /**
     * Returns a new TriIterator with <i>n</i> elements skipped from the beginning of this TriIterator.
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
     * @param n the number of elements to skip
     * @return A new TriIterator that skips the first <i>n</i> elements.
     * @throws IllegalArgumentException If <i>n</i> is negative.
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
     * @param count the maximum number of elements to include in the resulting TriIterator
     * @return a new TriIterator that contains at most the specified number of elements
     * @throws IllegalArgumentException If <i>count</i> is negative.
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
     * Returns a new TriIterator that includes only the elements that satisfy the provided predicate.
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
     * @param predicate the predicate to apply to each triple of elements
     * @return a new TriIterator containing only the elements that match the predicate
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
     * @param mapper the function to apply to each triple of elements
     * @return an ObjIterator containing the elements produced by the mapper function
     */
    public abstract <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper);

    /**
     * Returns an Optional containing the first triple of elements in the iterator.
     * If the iterator is empty, returns an empty Optional.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * String[] cities = {"NYC", "LA", "Chicago"};
     * TriIterator<String, Integer, String> iter = TriIterator.zip(names, ages, cities);
     * Optional<Triple<String, Integer, String>> firstTriple = iter.first();
     * firstTriple.ifPresent(triple ->
     *     System.out.println(triple.left() + " is " + triple.middle() + " from " + triple.right()));
     * // Output: Alice is 25 from NYC
     * }</pre>
     *
     * @return an Optional containing the first triple of elements, or an empty Optional if the iterator is empty
     */
    public Optional<Triple<A, B, C>> first() {
        if (hasNext()) {
            return Optional.of(next());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns an Optional containing the last triple of elements in the iterator.
     * If the iterator is empty, returns an empty Optional.
     *
     * <p><strong>Note:</strong> This method consumes all elements in the iterator.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * String[] cities = {"NYC", "LA", "Chicago"};
     * TriIterator<String, Integer, String> iter = TriIterator.zip(names, ages, cities);
     * Optional<Triple<String, Integer, String>> lastTriple = iter.last();
     * lastTriple.ifPresent(triple ->
     *     System.out.println(triple.left() + " is " + triple.middle() + " from " + triple.right()));
     * // Output: Charlie is 35 from Chicago
     * }</pre>
     *
     * @return an Optional containing the last triple of elements, or an empty Optional if the iterator is empty
     */
    public Optional<Triple<A, B, C>> last() {
        if (hasNext()) {
            final Triple<A, B, C> next = new Triple<>();
            final Throwables.TriConsumer<A, B, C, RuntimeException> setNext = next::set;

            foreachRemaining(setNext);

            return Optional.of(next);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns a Stream of elements produced by applying the given mapper function to each triple of elements in this TriIterator.
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
     * @param mapper the function to apply to each triple of elements
     * @return a Stream containing the elements produced by the mapper function
     */
    public <R> Stream<R> stream(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
        return Stream.of(map(mapper));
    }

    /**
     * Converts the elements in this TriIterator to an array of Triple objects.
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
     * @return An array containing the remaining triples of elements in this TriIterator.
     */
    @SuppressWarnings("deprecation")
    public Triple<A, B, C>[] toArray() {
        return toArray(new Triple[0]);
    }

    /**
     * Converts the elements in this TriIterator to an array of the specified type.
     *
     * @param <T> the type of the array elements. It should be a super type of Triple.
     * @param a the array into which the elements of this TriIterator are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose.
     * @return an array containing the elements of this TriIterator
     * @deprecated This method is deprecated. Use {@link #toArray()} or {@link #toList()} instead.
     */
    @Deprecated
    public <T> T[] toArray(final T[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts the elements in this TriIterator to a List of Triple objects.
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
     * @return a List containing all triples of elements in this TriIterator
     */
    public List<Triple<A, B, C>> toList() {
        return toCollection(Suppliers.ofList());
    }

    /**
     * Converts the elements in this TriIterator to three separate lists of type A, B, and C.
     * The resulting Triple contains three lists, each containing the elements of the corresponding type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     * Boolean[] active = {true, false, true};
     * Triple<List<String>, List<Integer>, List<Boolean>> result =
     *     TriIterator.zip(names, ages, active).toMultiList(ArrayList::new);
     * List<String> nameList = result.left();       // [Alice, Bob, Charlie]
     * List<Integer> ageList = result.middle();     // [25, 30, 35]
     * List<Boolean> activeList = result.right();   // [true, false, true]
     * }</pre>
     *
     * @param supplier a Supplier that provides new instances of List for storing the elements
     * @return a Triple containing three lists of elements of type A, B, and C
     */
    public Triple<List<A>, List<B>, List<C>> toMultiList(@SuppressWarnings("rawtypes") final Supplier<? extends List> supplier) {
        final List<A> listA = supplier.get();
        final List<B> listB = supplier.get();
        final List<C> listC = supplier.get();

        this.foreachRemaining((a, b, c) -> {
            listA.add(a);
            listB.add(b);
            listC.add(c);
        });

        return Triple.of(listA, listB, listC);
    }

    /**
     * Converts the elements in this TriIterator to three separate sets of type A, B, and C.
     * The resulting Triple contains three sets, each containing the unique elements of the corresponding type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Alice"};
     * Integer[] ages = {25, 30, 25};
     * Boolean[] active = {true, false, true};
     * Triple<Set<String>, Set<Integer>, Set<Boolean>> result =
     *     TriIterator.zip(names, ages, active).toMultiSet(HashSet::new);
     * Set<String> nameSet = result.left();       // [Alice, Bob] (duplicates removed)
     * Set<Integer> ageSet = result.middle();     // [25, 30]
     * Set<Boolean> activeSet = result.right();   // [true, false]
     * }</pre>
     *
     * @param supplier a Supplier that provides new instances of Set for storing the elements
     * @return a Triple containing three sets of elements of type A, B, and C
     */
    public Triple<Set<A>, Set<B>, Set<C>> toMultiSet(@SuppressWarnings("rawtypes") final Supplier<? extends Set> supplier) {
        final Set<A> listA = supplier.get();
        final Set<B> listB = supplier.get();
        final Set<C> listC = supplier.get();

        this.foreachRemaining((a, b, c) -> {
            listA.add(a);
            listB.add(b);
            listC.add(c);
        });

        return Triple.of(listA, listB, listC);
    }
}
