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
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.N;

/**
 * An extended iterator over primitive int values with additional functionality.
 * This class extends {@link IntIterator} and implements {@link IteratorEx} to provide
 * advanced iteration capabilities such as skipping elements, counting remaining elements,
 * and converting to arrays/lists.
 *
 * <p>This is an internal API marked with {@link Internal} annotation and is subject to change.
 * It provides factory methods for creating iterators from arrays and other iterators.
 *
 * @see IntIterator
 * @see IteratorEx
 */
@SuppressWarnings({ "java:S6548" })
@Internal
public abstract class IntIteratorEx extends IntIterator implements IteratorEx<Integer> {

    /**
     * Constructs a new IntIteratorEx.
     * This constructor is protected to allow subclassing.
     */
    protected IntIteratorEx() {
    }

    /**
     * An empty IntIteratorEx instance that contains no elements.
     * Calling {@code hasNext()} always returns {@code false}, and calling {@code nextInt()}
     * throws a {@link NoSuchElementException}.
     */
    @SuppressWarnings({ "java:S1845" })
    public static final IntIteratorEx EMPTY = new IntIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public int nextInt() {
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
        public int[] toArray() {
            return N.EMPTY_INT_ARRAY;
        }

    };

    /**
     * Returns an empty IntIteratorEx with no elements.
     * This iterator's hasNext() will always return {@code false} and nextInt() will throw NoSuchElementException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIteratorEx iter = IntIteratorEx.empty();
     * assertFalse(iter.hasNext());
     * }</pre>
     *
     * @return an empty IntIteratorEx instance
     */
    @SuppressWarnings({ "java:S1845", "SameReturnValue" })
    public static IntIteratorEx empty() {
        return EMPTY;
    }

    /**
     * Creates an IntIteratorEx from the given int array.
     * Returns an empty iterator if the array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIteratorEx iter = IntIteratorEx.of(1, 2, 3);
     * assertEquals(1, iter.nextInt());
     * }</pre>
     *
     * @param a the int array to iterate over (can be {@code null} or empty)
     * @return an IntIteratorEx for the given array, or empty iterator if array is null/empty
     */
    public static IntIteratorEx of(final int... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates an IntIteratorEx from a portion of the given int array.
     * The iterator will include elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr = {1, 2, 3, 4, 5};
     * IntIteratorEx iter = IntIteratorEx.of(arr, 1, 4);
     * assertEquals(2, iter.nextInt());   // starts at index 1
     * }</pre>
     *
     * @param a the int array to iterate over
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return an IntIteratorEx for the specified array range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
     */
    public static IntIteratorEx of(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new IntIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() {
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
            public int[] toArray() {
                final int[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public IntList toList() {
                final IntList ret = IntList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

        };
    }

    /**
     * Wraps an IntIterator as an IntIteratorEx.
     * If the iterator is already an IntIteratorEx, it is returned as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3);
     * IntIteratorEx iterEx = IntIteratorEx.of(iter);
     * }</pre>
     *
     * @param iter the IntIterator to wrap (can be null)
     * @return the same instance if {@code iter} is already an IntIteratorEx, an IntIteratorEx wrapping the given iterator, or an empty iterator if {@code iter} is null
     */
    public static IntIteratorEx of(final IntIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof IntIteratorEx) {
            return ((IntIteratorEx) iter);
        }

        return new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                return iter.nextInt();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Creates an IntIteratorEx from an Iterator of Integer objects.
     * Automatically unwraps Integer objects to primitive ints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = List.of(1, 2, 3).iterator();
     * IntIteratorEx iterEx = IntIteratorEx.from(iter);
     * }</pre>
     *
     * @param iter the Iterator of Integer objects (can be null)
     * @return an IntIteratorEx unwrapping the given iterator, or empty iterator if iter is null
     * @throws NullPointerException during iteration if any element returned by the source iterator is {@code null}
     */
    public static IntIteratorEx from(final Iterator<Integer> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx<Integer> iteratorEx) {

            return new IntIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public int nextInt() {
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
            return new IntIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public int nextInt() {
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
     * This default implementation calls {@link #nextInt()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIteratorEx iter = IntIteratorEx.of(1, 2, 3, 4, 5);
     * iter.advance(3);
     * assertEquals(4, iter.nextInt());
     *
     * // Non-positive values are ignored
     * IntIteratorEx iter2 = IntIteratorEx.of(1, 2);
     * iter2.advance(0);
     * assertEquals(1, iter2.nextInt());
     *
     * // Advancing beyond available elements exhausts the iterator
     * IntIteratorEx iter3 = IntIteratorEx.of(1, 2);
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
            nextInt();
            n--;
        }
    }

    /**
     * Counts the remaining elements by consuming them all.
     * After this call the iterator is exhausted.
     * This default implementation calls {@link #nextInt()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIteratorEx iter = IntIteratorEx.of(1, 2, 3);
     * assertEquals(3, iter.count());
     * assertFalse(iter.hasNext()); // iterator is exhausted after count()
     *
     * // Empty iterator returns 0
     * IntIteratorEx empty = IntIteratorEx.empty();
     * assertEquals(0, empty.count());
     * }</pre>
     *
     * @return the number of elements that remained before this call
     */
    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextInt();
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
     * IntIteratorEx iter = IntIteratorEx.of(1, 2);
     * iter.closeResource();         // releases resources (a no-op for this implementation)
     * assertTrue(iter.hasNext());   // closeResource() does not consume elements
     *
     * // Safe to call closeResource() multiple times
     * IntIteratorEx iter2 = IntIteratorEx.of(3, 4);
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
