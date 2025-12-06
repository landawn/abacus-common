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
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.stream.CharStream;

/**
 * An iterator specialized for primitive char values, providing better performance
 * than {@code Iterator<Character>} by avoiding boxing/unboxing overhead.
 * 
 * <p>This abstract class provides various static factory methods for creating
 * char iterators from arrays, suppliers, and other sources. It also provides
 * transformation methods like {@code indexed()} and utility methods like
 * {@code toArray()} and {@code stream()}.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * CharIterator iter = CharIterator.of('a', 'b', 'c');
 * while (iter.hasNext()) {
 *     char ch = iter.nextChar();
 *     System.out.println(ch);
 * }
 * }</pre>
 *
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 */
@SuppressWarnings({ "java:S6548" })
public abstract class CharIterator extends ImmutableIterator<Character> {

    /**
     * Constructs a new CharIterator.
     *
     * <p>This constructor is {@code protected} to allow subclasses to create custom
     * implementations of {@code CharIterator}. Subclasses should override the abstract
     * {@link #nextChar()} method and typically also override {@link #hasNext()}.</p>
     */
    protected CharIterator() {
    }

    /**
     * A singleton empty CharIterator instance that contains no elements.
     * This iterator's hasNext() always returns false, and nextChar() always throws NoSuchElementException.
     *
     * @see #empty()
     */
    public static final CharIterator EMPTY = new CharIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public char nextChar() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Returns an empty {@code CharIterator} with no elements.
     *
     * <p>The returned iterator's {@code hasNext()} will always return {@code false},
     * and calling {@code nextChar()} will always throw a {@code NoSuchElementException}.</p>
     *
     * <p>This method always returns the same singleton instance for efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.empty();
     * System.out.println(iter.hasNext());   // false
     * }</pre>
     *
     * @return an empty {@code CharIterator}
     */
    @SuppressWarnings("SameReturnValue")
    public static CharIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates a {@code CharIterator} from the specified char array.
     *
     * <p>If the array is {@code null} or empty, returns an empty iterator.
     * The iterator will iterate over all elements in the array from start to end.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * char first = iter.nextChar();   // 'a'
     * }</pre>
     *
     * @param a the char array (may be {@code null})
     * @return a new {@code CharIterator} over the array elements, or an empty iterator if the array is {@code null} or empty
     */
    public static CharIterator of(final char... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a {@code CharIterator} from a subsequence of the specified char array.
     *
     * <p>The iterator will iterate over elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive). If {@code fromIndex} equals {@code toIndex}, an empty
     * iterator is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'a', 'b', 'c', 'd', 'e'};
     * CharIterator iter = CharIterator.of(chars, 1, 4);
     * // Iterates over 'b', 'c', 'd'
     * }</pre>
     *
     * @param a the char array (may be {@code null})
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new {@code CharIterator} over the specified range, or an empty iterator if the array is {@code null} or the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length} or {@code fromIndex > toIndex}
     */
    public static CharIterator of(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (a == null || fromIndex == toIndex) {
            return EMPTY;
        }

        return new CharIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public char[] toArray() {
                final char[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public CharList toList() {
                final CharList ret = CharList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }
        };
    }

    /**
     * Returns a {@code CharIterator} instance created lazily using the provided Supplier.
     *
     * <p>The Supplier is invoked only when the first method ({@code hasNext()} or {@code nextChar()})
     * of the returned iterator is called. This is useful for deferring expensive iterator creation
     * until it is actually needed, or for scenarios where iterator creation depends on runtime conditions.</p>
     *
     * <p>The supplier is called at most once, and the resulting iterator is cached for subsequent calls.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.defer(() -> CharIterator.of('a', 'b', 'c'));
     * // Iterator is not created yet
     * if (iter.hasNext()) { // Supplier is invoked here
     *     char ch = iter.nextChar();
     * }
     * }</pre>
     *
     * @param iteratorSupplier a Supplier that provides the CharIterator when needed, must not be {@code null}
     * @return a {@code CharIterator} that is initialized on first use
     * @throws IllegalArgumentException if {@code iteratorSupplier} is {@code null}
     */
    public static CharIterator defer(final Supplier<? extends CharIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new CharIterator() {
            private CharIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextChar();
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
     * Returns an infinite {@code CharIterator} that generates values using the provided supplier.
     *
     * <p>The returned iterator's {@code hasNext()} method will always return {@code true}.
     * Each call to {@code nextChar()} invokes the supplier to generate the next value.</p>
     *
     * <p><strong>Warning:</strong> This iterator is infinite. Be careful when using methods
     * that consume all elements (like {@code toArray()} or {@code toList()}), as they will
     * never terminate and will eventually cause an {@code OutOfMemoryError}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.generate(() -> 'X');
     * // Infinite iterator that always returns 'X'
     * for (int i = 0; i < 5 && iter.hasNext(); i++) {
     *     System.out.print(iter.nextChar());   // XXXXX
     * }
     * }</pre>
     *
     * @param supplier the supplier function that generates char values, must not be {@code null}
     * @return an infinite {@code CharIterator}
     * @throws IllegalArgumentException if {@code supplier} is {@code null}
     */
    public static CharIterator generate(final CharSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new CharIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public char nextChar() {
                return supplier.getAsChar();
            }
        };
    }

    /**
     * Returns a {@code CharIterator} that generates values using the provided supplier
     * while the {@code hasNext} condition returns {@code true}.
     *
     * <p>Each call to the iterator's {@code hasNext()} method will invoke the provided
     * {@code hasNext} supplier to determine if more elements are available. If it returns
     * {@code true}, the {@code supplier} will be invoked to generate the next value.</p>
     *
     * <p>Note: The {@code hasNext} supplier may be called multiple times for the same element
     * (once by the user, once internally for validation), so it should be idempotent or
     * designed to handle multiple calls.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int count = 0;
     * CharIterator iter = CharIterator.generate(
     *     () -> count < 3,
     *     () -> (char)('A' + count++)
     * );
     * // Generates 'A', 'B', 'C'
     * }</pre>
     *
     * @param hasNext the condition that determines if more elements are available, must not be {@code null}
     * @param supplier the supplier function that generates char values, must not be {@code null}
     * @return a conditional {@code CharIterator}
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public static CharIterator generate(final BooleanSupplier hasNext, final CharSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new CharIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public char nextChar() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsChar();
            }
        };
    }

    /**
     * Returns the next element as a boxed {@code Character}.
     *
     * <p>This method is provided for compatibility with the standard {@code Iterator<Character>}
     * interface, but incurs boxing overhead. For better performance, use {@link #nextChar()} instead.</p>
     *
     * @return the next char value as a {@code Character} object
     * @throws NoSuchElementException if no more elements are available
     * @deprecated use {@link #nextChar()} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Character next() {
        return nextChar();
    }

    /**
     * Returns the next char value in the iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * char first = iter.nextChar();    // 'a'
     * char second = iter.nextChar();   // 'b'
     * }</pre>
     *
     * @return the next char value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract char nextChar();

    /**
     * Returns the first element wrapped in an {@code OptionalChar}, or an empty {@code OptionalChar}
     * if no elements are available.
     *
     * <p>This method consumes the first element from the iterator if present. After calling this method,
     * the iterator will be positioned at the second element (if it exists).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * OptionalChar first = iter.first();   // OptionalChar.of('a')
     * // Iterator now points to 'b'
     * }</pre>
     *
     * @return an {@code OptionalChar} containing the first element, or {@code OptionalChar.empty()} if the iterator is empty
     */
    public OptionalChar first() {
        if (hasNext()) {
            return OptionalChar.of(nextChar());
        } else {
            return OptionalChar.empty();
        }
    }

    /**
     * Returns the last element wrapped in an {@code OptionalChar}, or an empty {@code OptionalChar}
     * if no elements are available.
     *
     * <p>This method consumes all remaining elements from the iterator. After calling this method,
     * the iterator will be exhausted ({@code hasNext()} will return {@code false}).</p>
     *
     * <p><strong>Warning:</strong> For infinite iterators, this method will never return and will
     * eventually cause an {@code OutOfMemoryError} or run indefinitely.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * OptionalChar last = iter.last();   // OptionalChar.of('c')
     * // Iterator is now exhausted
     * }</pre>
     *
     * @return an {@code OptionalChar} containing the last element, or {@code OptionalChar.empty()} if the iterator is empty
     */
    public OptionalChar last() {
        if (hasNext()) {
            char next = nextChar();

            while (hasNext()) {
                next = nextChar();
            }

            return OptionalChar.of(next);
        } else {
            return OptionalChar.empty();
        }
    }

    /**
     * Converts the remaining elements to a char array.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] array = CharIterator.of('a', 'b', 'c', 'd', 'e').toArray();
     * // array = ['a', 'b', 'c', 'd', 'e']
     *
     * // Empty iterator returns empty array
     * char[] empty = CharIterator.empty().toArray();   // empty.length == 0
     * }</pre>
     *
     * @return a char array containing all remaining elements
     */
    @SuppressWarnings("deprecation")
    public char[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * Converts the remaining elements to a CharList.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty CharList.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharIterator.of('a', 'b', 'c', 'd', 'e').toList();
     * // list contains ['a', 'b', 'c', 'd', 'e']
     *
     * // Empty iterator returns empty list
     * CharList empty = CharIterator.empty().toList();   // empty.size() == 0
     * }</pre>
     *
     * @return a CharList containing all remaining elements
     */
    public CharList toList() {
        final CharList list = new CharList();

        while (hasNext()) {
            list.add(nextChar());
        }

        return list;
    }

    /**
     * Converts this iterator to a {@code CharStream}.
     *
     * <p>The returned stream is backed by this iterator, so consuming elements from the stream
     * will also consume them from the iterator. Once the stream is created, the iterator
     * should not be used directly to avoid unpredictable behavior.</p>
     *
     * <p>The stream does not support parallel execution and is not thread-safe unless the
     * underlying iterator is thread-safe.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * CharStream stream = iter.stream();
     * String result = stream.mapToObj(String::valueOf)
     *                       .collect(Collectors.joining());   // "abc"
     * }</pre>
     *
     * @return a {@code CharStream} backed by this iterator
     */
    public CharStream stream() {
        return CharStream.of(this);
    }

    /**
     * Returns an iterator of {@code IndexedChar} elements, where each character is paired with its index.
     *
     * <p>The index starts from 0 and increments by 1 for each element. This is equivalent to
     * calling {@code indexed(0)}.</p>
     *
     * <p>This method is marked as {@code @Beta} and may be subject to change in future versions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * ObjIterator<IndexedChar> indexed = iter.indexed();
     * while (indexed.hasNext()) {
     *     IndexedChar ic = indexed.next();
     *     System.out.println(ic.index() + ": " + ic.value());
     * }
     * // Output: 0: a, 1: b, 2: c
     * }</pre>
     *
     * @return an {@code ObjIterator} of {@code IndexedChar} elements with indices starting from 0
     */
    @Beta
    public ObjIterator<IndexedChar> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator of {@code IndexedChar} elements, where each character is paired with its index.
     *
     * <p>The index starts from the specified {@code startIndex} value and increments by 1 for each element.
     * This is useful when you need custom index numbering (e.g., starting from 1 instead of 0, or continuing
     * from a previous count).</p>
     *
     * <p>This method is marked as {@code @Beta} and may be subject to change in future versions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * ObjIterator<IndexedChar> indexed = iter.indexed(10);
     * // Produces IndexedChar with indices: 10, 11, 12
     * }</pre>
     *
     * @param startIndex the starting index value, must not be negative
     * @return an {@code ObjIterator} of {@code IndexedChar} elements with indices starting from {@code startIndex}
     * @throws IllegalArgumentException if {@code startIndex} is negative
     */
    @Beta
    public ObjIterator<IndexedChar> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final CharIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedChar next() {
                return IndexedChar.of(iter.nextChar(), idx++);
            }
        };
    }

    /**
     * Performs the given action for each remaining element.
     *
     * <p>This method is provided for compatibility with the standard {@code Iterator<Character>}
     * interface, but incurs boxing overhead for each element. For better performance,
     * use {@link #foreachRemaining(Throwables.CharConsumer)} instead.</p>
     *
     * @param action the action to perform on each element, must not be {@code null}
     * @deprecated use {@link #foreachRemaining(Throwables.CharConsumer)} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Character> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element without boxing overhead.
     *
     * <p>This method consumes all remaining elements from the iterator. After calling this method,
     * the iterator will be exhausted.</p>
     *
     * <p>The action is applied to each char value directly without boxing to {@code Character}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * iter.foreachRemaining(ch -> System.out.print(ch));   // abc
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element, must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception during processing
     */
    public <E extends Exception> void foreachRemaining(final Throwables.CharConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextChar());
        }
    }

    /**
     * Performs the given action for each remaining element along with its index.
     *
     * <p>The index starts from 0 and increments by 1 for each element. This method consumes
     * all remaining elements from the iterator. After calling this method, the iterator will
     * be exhausted.</p>
     *
     * <p>The action receives two parameters: the index (starting from 0) and the char value
     * without boxing overhead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * iter.foreachIndexed((index, ch) ->
     *     System.out.println(index + ": " + ch)
     * );
     * // Output: 0: a, 1: b, 2: c
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element and its index, must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception during processing
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntCharConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }
            action.accept(idx++, nextChar());
        }
    }
}
