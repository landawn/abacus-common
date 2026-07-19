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
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.N;

/**
 * An extended iterator over primitive long values with additional functionality.
 * This class extends {@link LongIterator} and implements {@link IteratorEx} to provide
 * advanced iteration capabilities such as skipping elements, counting remaining elements,
 * and converting to arrays/lists.
 *
 * <p>This is an internal API marked with {@link Internal} annotation and is subject to change.
 * It provides factory methods for creating iterators from arrays and other iterators.
 *
 * @see LongIterator
 * @see IteratorEx
 */
@SuppressWarnings({ "java:S6548" })
@Internal
public abstract class LongIteratorEx extends LongIterator implements IteratorEx<Long> {

    /**
     * Constructs a new LongIteratorEx.
     * This constructor is protected to allow subclassing.
     */
    protected LongIteratorEx() {
    }

    /**
     * An empty LongIteratorEx instance that contains no elements.
     * Calling {@code hasNext()} always returns {@code false}, and calling {@code nextLong()}
     * throws a {@link NoSuchElementException}.
     */
    @SuppressWarnings({ "java:S1845" })
    public static final LongIteratorEx EMPTY = new LongIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public long nextLong() {
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
        public long[] toArray() {
            return N.EMPTY_LONG_ARRAY;
        }

    };

    /**
     * Returns an empty LongIteratorEx with no elements.
     * This iterator's hasNext() will always return {@code false} and nextLong() will throw NoSuchElementException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIteratorEx iter = LongIteratorEx.empty();
     * assertFalse(iter.hasNext());
     * }</pre>
     *
     * @return an empty LongIteratorEx instance
     */
    @SuppressWarnings({ "java:S1845", "SameReturnValue" })
    public static LongIteratorEx empty() {
        return EMPTY;
    }

    /**
     * Creates a LongIteratorEx from the given long array.
     * Returns an empty iterator if the array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L);
     * assertEquals(1L, iter.nextLong());
     * }</pre>
     *
     * @param a the long array to iterate over (can be {@code null} or empty)
     * @return a LongIteratorEx for the given array, or empty iterator if array is null/empty
     */
    public static LongIteratorEx of(final long... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a LongIteratorEx from a portion of the given long array.
     * The iterator will include elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] arr = {1L, 2L, 3L, 4L, 5L};
     * LongIteratorEx iter = LongIteratorEx.of(arr, 1, 4);
     * assertEquals(2L, iter.nextLong());   // starts at index 1
     * }</pre>
     *
     * @param a the long array to iterate over
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a LongIteratorEx for the specified array range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
     */
    public static LongIteratorEx of(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new LongIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
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
            public long[] toArray() {
                final long[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public LongList toList() {
                final LongList ret = LongList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

        };
    }

    /**
     * Wraps a LongIterator as a LongIteratorEx.
     * If the iterator is already a LongIteratorEx, it is returned as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter = LongIterator.of(1L, 2L, 3L);
     * LongIteratorEx iterEx = LongIteratorEx.of(iter);
     * }</pre>
     *
     * @param iter the LongIterator to wrap (can be null)
     * @return the same instance if {@code iter} is already a LongIteratorEx, a LongIteratorEx wrapping the given iterator, or an empty iterator if {@code iter} is null
     */
    public static LongIteratorEx of(final LongIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof LongIteratorEx) {
            return ((LongIteratorEx) iter);
        }

        return new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                return iter.nextLong();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Creates a LongIteratorEx from an Iterator of Long objects.
     * Automatically unwraps Long objects to primitive longs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Long> iter = List.of(1L, 2L, 3L).iterator();
     * LongIteratorEx iterEx = LongIteratorEx.from(iter);
     * }</pre>
     *
     * @param iter the Iterator of Long objects (can be null)
     * @return a LongIteratorEx unwrapping the given iterator, or empty iterator if iter is null
     * @throws NullPointerException during iteration if any element returned by the source iterator is {@code null}
     */
    public static LongIteratorEx from(final Iterator<Long> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx<Long> iteratorEx) {

            return new LongIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public long nextLong() {
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
            return new LongIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public long nextLong() {
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
     * This default implementation calls {@link #nextLong()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L, 4L, 5L);
     * iter.advance(3);
     * assertEquals(4L, iter.nextLong());
     *
     * // Non-positive values are ignored
     * LongIteratorEx iter2 = LongIteratorEx.of(1L, 2L);
     * iter2.advance(0);
     * assertEquals(1L, iter2.nextLong());
     *
     * // Advancing beyond available elements exhausts the iterator
     * LongIteratorEx iter3 = LongIteratorEx.of(1L, 2L);
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
            nextLong();
            n--;
        }
    }

    /**
     * Counts the remaining elements by consuming them all.
     * After this call the iterator is exhausted.
     * This default implementation calls {@link #nextLong()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIteratorEx iter = LongIteratorEx.of(1L, 2L, 3L);
     * assertEquals(3, iter.count());
     * assertFalse(iter.hasNext()); // iterator is exhausted after count()
     *
     * // Empty iterator returns 0
     * LongIteratorEx empty = LongIteratorEx.empty();
     * assertEquals(0, empty.count());
     * }</pre>
     *
     * @return the number of elements that remained before this call
     */
    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextLong();
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
     * LongIteratorEx iter = LongIteratorEx.of(1L, 2L);
     * iter.closeResource();         // releases resources (a no-op for this implementation)
     * assertTrue(iter.hasNext());   // closeResource() does not consume elements
     *
     * // Safe to call closeResource() multiple times
     * LongIteratorEx iter2 = LongIteratorEx.of(3L, 4L);
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
