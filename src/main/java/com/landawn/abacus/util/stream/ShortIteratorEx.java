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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;

/**
 * An extended iterator over primitive short values with additional functionality.
 * This class extends {@link ShortIterator} and implements {@link IteratorEx} to provide
 * advanced iteration capabilities such as skipping elements, counting remaining elements,
 * and converting to arrays/lists.
 *
 * <p>This is an internal API marked with {@link Internal} annotation and is subject to change.
 * It provides factory methods for creating iterators from arrays and other iterators.
 *
 * @see ShortIterator
 * @see IteratorEx
 */
@SuppressWarnings({ "java:S6548" })
@Internal
public abstract class ShortIteratorEx extends ShortIterator implements IteratorEx<Short> {

    /**
     * Constructs a new ShortIteratorEx.
     * This constructor is protected to allow subclassing.
     */
    protected ShortIteratorEx() {
    }

    /**
     * An empty ShortIteratorEx instance that contains no elements.
     * Calling {@code hasNext()} always returns {@code false}, and calling {@code nextShort()}
     * throws a {@link NoSuchElementException}.
     */
    @SuppressWarnings({ "java:S1845" })
    public static final ShortIteratorEx EMPTY = new ShortIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public short nextShort() {
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
        public short[] toArray() {
            return N.EMPTY_SHORT_ARRAY;
        }

    };

    /**
     * Returns an empty ShortIteratorEx with no elements.
     * This iterator's hasNext() will always return {@code false} and nextShort() will throw NoSuchElementException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIteratorEx iter = ShortIteratorEx.empty();
     * assertFalse(iter.hasNext());
     * }</pre>
     *
     * @return an empty ShortIteratorEx instance
     */
    @SuppressWarnings({ "java:S1845", "SameReturnValue" })
    public static ShortIteratorEx empty() {
        return EMPTY;
    }

    /**
     * Creates a ShortIteratorEx from the given short array.
     * Returns an empty iterator if the array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3);
     * assertEquals(1, iter.nextShort());
     * }</pre>
     *
     * @param a the short array to iterate over (can be {@code null} or empty)
     * @return a ShortIteratorEx for the given array, or empty iterator if array is null/empty
     */
    public static ShortIteratorEx of(final short... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a ShortIteratorEx from a portion of the given short array.
     * The iterator will include elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] arr = {1, 2, 3, 4, 5};
     * ShortIteratorEx iter = ShortIteratorEx.of(arr, 1, 4);
     * assertEquals(2, iter.nextShort());   // starts at index 1
     * }</pre>
     *
     * @param a the short array to iterate over
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a ShortIteratorEx for the specified array range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
     */
    public static ShortIteratorEx of(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ShortIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
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
            public short[] toArray() {
                final short[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public ShortList toList() {
                final ShortList ret = ShortList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

        };
    }

    /**
     * Wraps a ShortIterator as a ShortIteratorEx.
     * If the iterator is already a ShortIteratorEx, it is returned as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.of((short) 1, (short) 2, (short) 3);
     * ShortIteratorEx iterEx = ShortIteratorEx.of(iter);
     * }</pre>
     *
     * @param iter the ShortIterator to wrap (can be null)
     * @return the same instance if {@code iter} is already a ShortIteratorEx, a ShortIteratorEx wrapping the given iterator, or an empty iterator if {@code iter} is null
     */
    public static ShortIteratorEx of(final ShortIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ShortIteratorEx) {
            return ((ShortIteratorEx) iter);
        }

        return new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                return iter.nextShort();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Creates a ShortIteratorEx from an Iterator of Short objects.
     * Automatically unwraps Short objects to primitive shorts.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Short> iter = List.of((short) 1, (short) 2, (short) 3).iterator();
     * ShortIteratorEx iterEx = ShortIteratorEx.from(iter);
     * }</pre>
     *
     * @param iter the Iterator of Short objects (can be null)
     * @return a ShortIteratorEx unwrapping the given iterator, or empty iterator if iter is null
     * @throws NullPointerException during iteration if any element returned by the source iterator is {@code null}
     */
    public static ShortIteratorEx from(final Iterator<Short> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx<Short> iteratorEx) {

            return new ShortIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public short nextShort() {
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
            return new ShortIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public short nextShort() {
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
     * This default implementation calls {@link #nextShort()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
     * iter.advance(3);
     * assertEquals((short) 4, iter.nextShort());
     *
     * // Non-positive values are ignored
     * ShortIteratorEx iter2 = ShortIteratorEx.of((short) 1, (short) 2);
     * iter2.advance(0);
     * assertEquals((short) 1, iter2.nextShort());
     *
     * // Advancing beyond available elements exhausts the iterator
     * ShortIteratorEx iter3 = ShortIteratorEx.of((short) 1, (short) 2);
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
            nextShort();
            n--;
        }
    }

    /**
     * Counts the remaining elements by consuming them all.
     * After this call the iterator is exhausted.
     * This default implementation calls {@link #nextShort()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2, (short) 3);
     * assertEquals(3, iter.count());
     * assertFalse(iter.hasNext()); // iterator is exhausted after count()
     *
     * // Empty iterator returns 0
     * ShortIteratorEx empty = ShortIteratorEx.empty();
     * assertEquals(0, empty.count());
     * }</pre>
     *
     * @return the number of elements that remained before this call
     */
    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextShort();
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
     * ShortIteratorEx iter = ShortIteratorEx.of((short) 1, (short) 2);
     * iter.closeResource();         // releases resources (a no-op for this implementation)
     * assertTrue(iter.hasNext());   // closeResource() does not consume elements
     *
     * // Safe to call closeResource() multiple times
     * ShortIteratorEx iter2 = ShortIteratorEx.of((short) 3, (short) 4);
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
