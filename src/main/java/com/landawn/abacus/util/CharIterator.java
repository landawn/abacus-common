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
 * <p>Example usage:</p>
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
 * @since 1.0
 */
@SuppressWarnings({ "java:S6548" })
public abstract class CharIterator extends ImmutableIterator<Character> {

    /**
     * A singleton empty CharIterator instance.
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
     * Returns an empty {@code CharIterator}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.empty();
     * System.out.println(iter.hasNext()); // false
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
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * char first = iter.nextChar(); // 'a'
     * }</pre>
     *
     * @param a the char array
     * @return a new {@code CharIterator} over the array elements
     */
    public static CharIterator of(final char... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a {@code CharIterator} from a subsequence of the specified char array.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * char[] chars = {'a', 'b', 'c', 'd', 'e'};
     * CharIterator iter = CharIterator.of(chars, 1, 4);
     * // Iterates over 'b', 'c', 'd'
     * }</pre>
     *
     * @param a the char array
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new {@code CharIterator} over the specified range
     * @throws IndexOutOfBoundsException if the indices are out of range
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
     * Returns a CharIterator instance created lazily using the provided Supplier.
     * The Supplier is invoked only when the first method of the returned iterator is called.
     * 
     * <p>This is useful for deferring expensive iterator creation until actually needed.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.defer(() -> CharIterator.of('a', 'b', 'c'));
     * // Iterator is not created yet
     * if (iter.hasNext()) { // Supplier is invoked here
     *     char ch = iter.nextChar();
     * }
     * }</pre>
     *
     * @param iteratorSupplier A Supplier that provides the CharIterator when needed
     * @return A CharIterator that is initialized on first use
     * @throws IllegalArgumentException if iteratorSupplier is {@code null}
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
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.generate(() -> 'X');
     * // Infinite iterator that always returns 'X'
     * for (int i = 0; i < 5 && iter.hasNext(); i++) {
     *     System.out.print(iter.nextChar()); // XXXXX
     * }
     * }</pre>
     *
     * @param supplier the supplier function that generates char values
     * @return an infinite {@code CharIterator}
     * @throws IllegalArgumentException if supplier is {@code null}
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
     * while the hasNext condition returns true.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int count = 0;
     * CharIterator iter = CharIterator.generate(
     *     () -> count < 3,
     *     () -> (char)('A' + count++)
     * );
     * // Generates 'A', 'B', 'C'
     * }</pre>
     *
     * @param hasNext the condition that determines if more elements are available
     * @param supplier the supplier function that generates char values
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
     * Returns the next element as a Character (boxed).
     * 
     * @return the next char value as a Character
     * @deprecated use {@code nextChar()} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Character next() {
        return nextChar();
    }

    /**
     * Returns the next char value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * char ch = iter.nextChar(); // 'a'
     * }</pre>
     * 
     * @return the next char value
     * @throws NoSuchElementException if no more elements are available
     */
    public abstract char nextChar();

    /**
     * Returns the first element wrapped in an OptionalChar, or an empty OptionalChar if no elements are available.
     * 
     * <p>This method consumes the first element if present.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * OptionalChar first = iter.first(); // OptionalChar.of('a')
     * // Iterator now points to 'b'
     * }</pre>
     * 
     * @return an OptionalChar containing the first element, or empty if none
     */
    public OptionalChar first() {
        if (hasNext()) {
            return OptionalChar.of(nextChar());
        } else {
            return OptionalChar.empty();
        }
    }

    /**
     * Returns the last element wrapped in an OptionalChar, or an empty OptionalChar if no elements are available.
     * 
     * <p>This method consumes all remaining elements.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * OptionalChar last = iter.last(); // OptionalChar.of('c')
     * // Iterator is now exhausted
     * }</pre>
     * 
     * @return an OptionalChar containing the last element, or empty if none
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
     * Collects all remaining elements into a char array.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * char[] array = iter.toArray(); // ['a', 'b', 'c']
     * }</pre>
     * 
     * @return a char array containing all remaining elements
     */
    @SuppressWarnings("deprecation")
    public char[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * Collects all remaining elements into a CharList.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * CharList list = iter.toList();
     * System.out.println(list); // [a, b, c]
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
     * Converts this iterator to a CharStream.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * CharStream stream = iter.stream();
     * String result = stream.mapToObj(String::valueOf)
     *                      .collect(Collectors.joining()); // "abc"
     * }</pre>
     * 
     * @return a CharStream backed by this iterator
     */
    public CharStream stream() {
        return CharStream.of(this);
    }

    /**
     * Returns an iterator of IndexedChar elements, where each character is paired with its index.
     * The index starts from 0.
     * 
     * <p>Example:</p>
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
     * @return an iterator of IndexedChar elements
     */
    @Beta
    public ObjIterator<IndexedChar> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator of IndexedChar elements, where each character is paired with its index.
     * The index starts from the specified value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * ObjIterator<IndexedChar> indexed = iter.indexed(10);
     * // Produces IndexedChar with indices: 10, 11, 12
     * }</pre>
     *
     * @param startIndex the starting index value
     * @return an iterator of IndexedChar elements
     * @throws IllegalArgumentException if startIndex is negative
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
     * @param action the action to perform on each element
     * @deprecated use {@link #foreachRemaining(Throwables.CharConsumer)} instead to avoid boxing
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Character> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * iter.foreachRemaining(ch -> System.out.print(ch)); // abc
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element
     * @throws E if the action throws an exception
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
     * <p>The index starts from 0 and increments for each element.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * iter.foreachIndexed((index, ch) -> 
     *     System.out.println(index + ": " + ch)
     * );
     * // Output: 0: a, 1: b, 2: c
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element and its index
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntCharConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextChar());
        }
    }
}
