/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
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

package com.landawn.abacus.util.stream;

import static com.landawn.abacus.util.stream.StreamBase.ERROR_MSG_FOR_NO_SUCH_EX;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.N;

/**
 * An extended iterator over primitive char values with additional functionality.
 * This class extends {@link CharIterator} and implements {@link IteratorEx} to provide
 * advanced iteration capabilities such as skipping elements, counting remaining elements,
 * and converting to arrays/lists.
 *
 * <p>This is an internal API marked with {@link Internal} annotation and is subject to change.
 * It provides factory methods for creating iterators from arrays and other iterators.
 *
 * @see CharIterator
 * @see IteratorEx
 */
@SuppressWarnings({ "java:S6548" })
@Internal
public abstract class CharIteratorEx extends CharIterator implements IteratorEx<Character> {

    /**
     * Constructs a new CharIteratorEx.
     * This constructor is protected to allow subclassing.
     */
    protected CharIteratorEx() {
    }

    /**
     * An empty CharIteratorEx instance that contains no elements.
     * Calling {@code hasNext()} always returns {@code false}, and calling {@code nextChar()}
     * throws a {@link NoSuchElementException}.
     */
    @SuppressWarnings({ "java:S1845" })
    public static final CharIteratorEx EMPTY = new CharIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public char nextChar() {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public void advance(final long n) {
            // Do nothing.
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public char[] toArray() {
            return N.EMPTY_CHAR_ARRAY;
        }

    };

    /**
     * Returns an empty CharIteratorEx with no elements.
     * This iterator's hasNext() will always return {@code false} and nextChar() will throw NoSuchElementException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIteratorEx iter = CharIteratorEx.empty();
     * assertFalse(iter.hasNext());
     * }</pre>
     *
     * @return an empty CharIteratorEx instance
     */
    @SuppressWarnings({ "java:S1845", "SameReturnValue" })
    public static CharIteratorEx empty() {
        return EMPTY;
    }

    /**
     * Creates a CharIteratorEx from the given char array.
     * Returns an empty iterator if the array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
     * assertEquals('a', iter.nextChar());
     * }</pre>
     *
     * @param a the char array to iterate over (can be {@code null} or empty)
     * @return a CharIteratorEx for the given array, or empty iterator if array is null/empty
     */
    public static CharIteratorEx of(final char... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a CharIteratorEx from a portion of the given char array.
     * The iterator will include elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'a', 'b', 'c', 'd', 'e'};
     * CharIteratorEx iter = CharIteratorEx.of(arr, 1, 4);
     * assertEquals('b', iter.nextChar());   // starts at index 1
     * }</pre>
     *
     * @param a the char array to iterate over
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a CharIteratorEx for the specified array range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
     */
    public static CharIteratorEx of(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new CharIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public void advance(final long n) {
                if (n <= 0 || cursor >= toIndex) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
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
     * Wraps a CharIterator as a CharIteratorEx.
     * If the iterator is already a CharIteratorEx, it is returned as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * CharIteratorEx iterEx = CharIteratorEx.of(iter);
     * }</pre>
     *
     * @param iter the CharIterator to wrap (can be null)
     * @return the same instance if {@code iter} is already a CharIteratorEx, a CharIteratorEx wrapping the given iterator, or an empty iterator if {@code iter} is null
     */
    public static CharIteratorEx of(final CharIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof CharIteratorEx) {
            return ((CharIteratorEx) iter);
        }

        return new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                return iter.nextChar();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Creates a CharIteratorEx from an Iterator of Character objects.
     * Automatically unwraps Character objects to primitive chars.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Character> iter = List.of('a', 'b', 'c').iterator();
     * CharIteratorEx iterEx = CharIteratorEx.from(iter);
     * }</pre>
     *
     * @param iter the Iterator of Character objects (can be null)
     * @return a CharIteratorEx unwrapping the given iterator, or empty iterator if iter is null
     * @throws NullPointerException during iteration if any element returned by the source iterator is {@code null}
     */
    public static CharIteratorEx from(final Iterator<Character> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx<Character> iteratorEx) {

            return new CharIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public char nextChar() {
                    return iteratorEx.next();
                }

                @Override
                public void advance(final long n) {
                    if (n <= 0) {
                        return;
                    }

                    iteratorEx.advance(n);
                }

                @Override
                public long count() {
                    return iteratorEx.count();
                }

                @Override
                public void closeResource() {
                    iteratorEx.closeResource();
                }
            };
        } else {
            return new CharIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public char nextChar() {
                    return iter.next();
                }

                @Override
                public void closeResource() {
                    ObjIteratorEx.closeResource(iter);
                }
            };
        }
    }

    /**
     * Advances the iterator by skipping up to {@code n} elements.
     * This default implementation calls {@link #nextChar()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c', 'd', 'e');
     * iter.advance(3);
     * assertEquals('d', iter.nextChar());
     *
     * // Non-positive values are ignored
     * CharIteratorEx iter2 = CharIteratorEx.of('x', 'y');
     * iter2.advance(0);
     * assertEquals('x', iter2.nextChar());
     *
     * // Advancing beyond available elements exhausts the iterator
     * CharIteratorEx iter3 = CharIteratorEx.of('a', 'b');
     * iter3.advance(100);
     * assertFalse(iter3.hasNext());
     * }</pre>
     *
     * @param n the number of elements to skip; non-positive values are ignored
     */
    @Override
    public void advance(long n) {
        if (n <= 0) {
            return;
        }

        while (n > 0 && hasNext()) {
            nextChar();
            n--;
        }
    }

    /**
     * Counts the remaining elements by consuming them all.
     * After this call the iterator is exhausted.
     * This default implementation calls {@link #nextChar()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIteratorEx iter = CharIteratorEx.of('a', 'b', 'c');
     * assertEquals(3, iter.count());
     * assertFalse(iter.hasNext()); // iterator is exhausted after count()
     *
     * // Empty iterator returns 0
     * CharIteratorEx empty = CharIteratorEx.empty();
     * assertEquals(0, empty.count());
     * }</pre>
     *
     * @return the number of elements that remained before this call
     */
    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextChar();
            result++;
        }

        return result;
    }

    /**
     * Closes this iterator and releases any resources it holds.
     * The default implementation is a no-op. Subclasses that hold
     * resources should override this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIteratorEx iter = CharIteratorEx.of('a', 'b');
     * iter.closeResource(); // releases resources (a no-op for this implementation)
     * assertTrue(iter.hasNext()); // closeResource() does not consume elements
     *
     * // Safe to call closeResource() multiple times
     * CharIteratorEx iter2 = CharIteratorEx.of('x', 'y');
     * iter2.closeResource();
     * iter2.closeResource(); // leaves the iterator unchanged
     * }</pre>
     *
     */
    @Override
    public void closeResource() {
        // Do nothing.
    }
}
