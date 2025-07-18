/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteSupplier;
import com.landawn.abacus.util.stream.ByteStream;

/**
 * An iterator specialized for primitive byte values, providing better performance
 * than {@code Iterator<Byte>} by avoiding boxing/unboxing overhead.
 * 
 * <p>This abstract class provides various static factory methods for creating
 * byte iterators from arrays, suppliers, and other sources. It also provides
 * transformation methods like {@code skip()}, {@code limit()}, {@code filter()},
 * and utility methods like {@code toArray()} and {@code stream()}.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
 * while (iter.hasNext()) {
 *     byte b = iter.nextByte();
 *     System.out.println(b);
 * }
 * }</pre>
 *
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 * @since 1.0
 */
@SuppressWarnings({ "java:S6548" })
public abstract class ByteIterator extends ImmutableIterator<Byte> {

    /**
     * A singleton empty ByteIterator instance.
     */
    public static final ByteIterator EMPTY = new ByteIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public byte nextByte() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Returns an empty {@code ByteIterator}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.empty();
     * System.out.println(iter.hasNext()); // false
     * }</pre>
     * 
     * @return an empty {@code ByteIterator}
     */
    @SuppressWarnings("SameReturnValue")
    public static ByteIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates a {@code ByteIterator} from the specified byte array.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * byte first = iter.nextByte(); // 1
     * }</pre>
     *
     * @param a the byte array
     * @return a new {@code ByteIterator} over the array elements
     */
    public static ByteIterator of(final byte... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a {@code ByteIterator} from a subsequence of the specified byte array.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * byte[] bytes = {1, 2, 3, 4, 5};
     * ByteIterator iter = ByteIterator.of(bytes, 1, 4);
     * // Iterates over 2, 3, 4
     * }</pre>
     *
     * @param a the byte array
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new {@code ByteIterator} over the specified range
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    public static ByteIterator of(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (a == null || fromIndex == toIndex) {
            return EMPTY;
        }

        return new ByteIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public byte[] toArray() {
                final byte[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public ByteList toList() {
                final ByteList ret = ByteList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }
        };
    }

    /**
     * Returns a ByteIterator instance created lazily using the provided Supplier.
     * The Supplier is invoked only when the first method of the returned iterator is called.
     * 
     * <p>This is useful for deferring expensive iterator creation until actually needed.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.defer(() -> ByteIterator.of((byte)1, (byte)2, (byte)3));
     * // Iterator is not created yet
     * if (iter.hasNext()) { // Supplier is invoked here
     *     byte b = iter.nextByte();
     * }
     * }</pre>
     *
     * @param iteratorSupplier A Supplier that provides the ByteIterator when needed
     * @return A ByteIterator that is initialized on first use
     * @throws IllegalArgumentException if iteratorSupplier is {@code null}
     */
    public static ByteIterator defer(final Supplier<? extends ByteIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new ByteIterator() {
            private ByteIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextByte();
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
     * Returns an infinite {@code ByteIterator} that generates values using the provided supplier.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.generate(() -> (byte)42);
     * // Infinite iterator that always returns 42
     * for (int i = 0; i < 5 && iter.hasNext(); i++) {
     *     System.out.print(iter.nextByte() + " "); // 42 42 42 42 42
     * }
     * }</pre>
     *
     * @param supplier the supplier function that generates byte values
     * @return an infinite {@code ByteIterator}
     * @throws IllegalArgumentException if supplier is {@code null}
     */
    public static ByteIterator generate(final ByteSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new ByteIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public byte nextByte() {
                return supplier.getAsByte();
            }
        };
    }

    /**
     * Returns a {@code ByteIterator} that generates values using the provided supplier
     * while the hasNext condition returns true.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int[] count = {0};
     * ByteIterator iter = ByteIterator.generate(
     *     () -> count[0] < 3,
     *     () -> (byte)(count[0]++)
     * );
     * // Generates 0, 1, 2
     * }</pre>
     *
     * @param hasNext the condition that determines if more elements are available
     * @param supplier the supplier function that generates byte values
     * @return a conditional {@code ByteIterator}
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public static ByteIterator generate(final BooleanSupplier hasNext, final ByteSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ByteIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsByte();
            }
        };
    }

    /**
     * Returns the next element as a Byte (boxed).
     * 
     * @return the next byte value as a Byte
     * @deprecated use {@code nextByte()} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Byte next() {
        return nextByte();
    }

    /**
     * Returns the next byte value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * byte b = iter.nextByte(); // 1
     * }</pre>
     * 
     * @return the next byte value
     * @throws NoSuchElementException if no more elements are available
     */
    public abstract byte nextByte();

    /**
     * Returns a new iterator that skips the first n elements.
     * 
     * <p>If n is greater than the number of remaining elements, all elements
     * are skipped and an empty iterator is effectively returned.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
     * ByteIterator skipped = iter.skip(2);
     * // skipped will iterate over 3, 4, 5
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new iterator with the first n elements skipped
     * @throws IllegalArgumentException if n is negative
     */
    public ByteIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final ByteIterator iter = this;

        return new ByteIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextByte();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextByte();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new iterator that limits the number of elements to the specified count.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
     * ByteIterator limited = iter.limit(3);
     * // limited will iterate over 1, 2, 3
     * }</pre>
     *
     * @param count the maximum number of elements to iterate
     * @return a new iterator limited to count elements
     * @throws IllegalArgumentException if count is negative
     */
    public ByteIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return ByteIterator.EMPTY;
        }

        final ByteIterator iter = this;

        return new ByteIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextByte();
            }
        };
    }

    /**
     * Returns a new iterator that includes only elements matching the specified predicate.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
     * ByteIterator evens = iter.filter(b -> b % 2 == 0);
     * // evens will iterate over 2, 4
     * }</pre>
     *
     * @param predicate the predicate to test elements
     * @return a new filtered iterator
     * @throws IllegalArgumentException if predicate is {@code null}
     */
    public ByteIterator filter(final BytePredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final ByteIterator iter = this;

        return new ByteIterator() {
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextByte();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public byte nextByte() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        };
    }

    /**
     * Returns the first element wrapped in an OptionalByte, or an empty OptionalByte if no elements are available.
     * 
     * <p>This method consumes the first element if present.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * OptionalByte first = iter.first(); // OptionalByte.of(1)
     * // Iterator now points to 2
     * }</pre>
     * 
     * @return an OptionalByte containing the first element, or empty if none
     */
    public OptionalByte first() {
        if (hasNext()) {
            return OptionalByte.of(nextByte());
        } else {
            return OptionalByte.empty();
        }
    }

    /**
     * Returns the last element wrapped in an OptionalByte, or an empty OptionalByte if no elements are available.
     * 
     * <p>This method consumes all remaining elements.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * OptionalByte last = iter.last(); // OptionalByte.of(3)
     * // Iterator is now exhausted
     * }</pre>
     * 
     * @return an OptionalByte containing the last element, or empty if none
     */
    public OptionalByte last() {
        if (hasNext()) {
            byte next = nextByte();

            while (hasNext()) {
                next = nextByte();
            }

            return OptionalByte.of(next);
        } else {
            return OptionalByte.empty();
        }
    }

    /**
     * Collects all remaining elements into a byte array.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * byte[] array = iter.toArray(); // [1, 2, 3]
     * }</pre>
     * 
     * @return a byte array containing all remaining elements
     */
    @SuppressWarnings("deprecation")
    public byte[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * Collects all remaining elements into a ByteList.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * ByteList list = iter.toList();
     * System.out.println(list); // [1, 2, 3]
     * }</pre>
     * 
     * @return a ByteList containing all remaining elements
     */
    public ByteList toList() {
        final ByteList list = new ByteList();

        while (hasNext()) {
            list.add(nextByte());
        }

        return list;
    }

    /**
     * Converts this iterator to a ByteStream.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * ByteStream stream = iter.stream();
     * int sum = stream.sum(); // 6
     * }</pre>
     * 
     * @return a ByteStream backed by this iterator
     */
    public ByteStream stream() {
        return ByteStream.of(this);
    }

    /**
     * Returns an iterator of IndexedByte elements, where each byte is paired with its index.
     * The index starts from 0.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)10, (byte)20, (byte)30);
     * ObjIterator<IndexedByte> indexed = iter.indexed();
     * while (indexed.hasNext()) {
     *     IndexedByte ib = indexed.next();
     *     System.out.println(ib.index() + ": " + ib.value());
     * }
     * // Output: 0: 10, 1: 20, 2: 30
     * }</pre>
     * 
     * @return an iterator of IndexedByte elements
     */
    @Beta
    public ObjIterator<IndexedByte> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator of IndexedByte elements, where each byte is paired with its index.
     * The index starts from the specified value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)10, (byte)20, (byte)30);
     * ObjIterator<IndexedByte> indexed = iter.indexed(100);
     * // Produces IndexedByte with indices: 100, 101, 102
     * }</pre>
     *
     * @param startIndex the starting index value
     * @return an iterator of IndexedByte elements
     * @throws IllegalArgumentException if startIndex is negative
     */
    @Beta
    public ObjIterator<IndexedByte> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final ByteIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedByte next() {
                return IndexedByte.of(iter.nextByte(), idx++);
            }
        };
    }

    /**
     * Performs the given action for each remaining element.
     *
     * @param action the action to perform on each element
     * @deprecated use {@link #foreachRemaining(Throwables.ByteConsumer)} instead to avoid boxing
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Byte> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * iter.foreachRemaining(b -> System.out.print(b + " ")); // 1 2 3
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachRemaining(final Throwables.ByteConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextByte());
        }
    }

    /**
     * Performs the given action for each remaining element along with its index.
     * 
     * <p>The index starts from 0 and increments for each element.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)10, (byte)20, (byte)30);
     * iter.foreachIndexed((index, b) -> 
     *     System.out.println(index + ": " + b)
     * );
     * // Output: 0: 10, 1: 20, 2: 30
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element and its index
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntByteConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextByte());
        }
    }
}
