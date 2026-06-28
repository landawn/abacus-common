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
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.N;

/**
 * An extended iterator over primitive byte values with additional functionality.
 * This class extends {@link ByteIterator} and implements {@link IteratorEx} to provide
 * advanced iteration capabilities such as skipping elements, counting remaining elements,
 * and converting to arrays/lists.
 *
 * <p>This is an internal API marked with {@link Internal} annotation and is subject to change.
 * It provides factory methods for creating iterators from arrays and other iterators.
 *
 * @see ByteIterator
 * @see IteratorEx
 */
@SuppressWarnings({ "java:S6548" })
@Internal
public abstract class ByteIteratorEx extends ByteIterator implements IteratorEx<Byte> {

    /**
     * Constructs a new ByteIteratorEx.
     * This constructor is protected to allow subclassing.
     */
    protected ByteIteratorEx() {
    }

    /**
     * An empty ByteIteratorEx instance that contains no elements.
     * Calling {@code hasNext()} always returns {@code false}, and calling {@code nextByte()}
     * throws a {@link NoSuchElementException}.
     */
    @SuppressWarnings({ "java:S1845" })
    public static final ByteIteratorEx EMPTY = new ByteIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public byte nextByte() {
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
        public byte[] toArray() {
            return N.EMPTY_BYTE_ARRAY;
        }

    };

    /**
     * Returns an empty ByteIteratorEx with no elements.
     * This iterator's hasNext() will always return {@code false} and nextByte() will throw NoSuchElementException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIteratorEx iter = ByteIteratorEx.empty();
     * assertFalse(iter.hasNext());
     * }</pre>
     *
     * @return an empty ByteIteratorEx instance
     */
    @SuppressWarnings({ "java:S1845", "SameReturnValue" })
    public static ByteIteratorEx empty() {
        return EMPTY;
    }

    /**
     * Creates a ByteIteratorEx from the given byte array.
     * Returns an empty iterator if the array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
     * assertEquals(1, iter.nextByte());
     * }</pre>
     *
     * @param a the byte array to iterate over (can be {@code null} or empty)
     * @return a ByteIteratorEx for the given array, or empty iterator if array is null/empty
     */
    public static ByteIteratorEx of(final byte... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a ByteIteratorEx from a portion of the given byte array.
     * The iterator will include elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr = {1, 2, 3, 4, 5};
     * ByteIteratorEx iter = ByteIteratorEx.of(arr, 1, 4);
     * assertEquals(2, iter.nextByte());   // starts at index 1
     * }</pre>
     *
     * @param a the byte array to iterate over
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a ByteIteratorEx for the specified array range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
     */
    public static ByteIteratorEx of(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ByteIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
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
     * Wraps a ByteIterator as a ByteIteratorEx.
     * If the iterator is already a ByteIteratorEx, it is returned as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
     * ByteIteratorEx iterEx = ByteIteratorEx.of(iter);
     * }</pre>
     *
     * @param iter the ByteIterator to wrap (can be null)
     * @return the same instance if {@code iter} is already a ByteIteratorEx, a ByteIteratorEx wrapping the given iterator, or an empty iterator if {@code iter} is null
     */
    public static ByteIteratorEx of(final ByteIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ByteIteratorEx) {
            return ((ByteIteratorEx) iter);
        }

        return new ByteIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                return iter.nextByte();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Creates a ByteIteratorEx from an Iterator of Byte objects.
     * Automatically unwraps Byte objects to primitive bytes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Byte> iter = List.of((byte) 1, (byte) 2).iterator();
     * ByteIteratorEx iterEx = ByteIteratorEx.from(iter);
     * }</pre>
     *
     * @param iter the Iterator of Byte objects (can be null)
     * @return a ByteIteratorEx unwrapping the given iterator, or empty iterator if iter is null
     * @throws NullPointerException during iteration if any element returned by the source iterator is {@code null}
     */
    public static ByteIteratorEx from(final Iterator<Byte> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx<Byte> iteratorEx) {

            return new ByteIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public byte nextByte() {
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
            return new ByteIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public byte nextByte() {
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
     * This default implementation calls {@link #nextByte()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
     * iter.advance(3);
     * assertEquals((byte) 4, iter.nextByte());
     *
     * // Non-positive values are ignored
     * ByteIteratorEx iter2 = ByteIteratorEx.of((byte) 1, (byte) 2);
     * iter2.advance(0);
     * assertEquals((byte) 1, iter2.nextByte());
     *
     * // Advancing beyond available elements exhausts the iterator
     * ByteIteratorEx iter3 = ByteIteratorEx.of((byte) 1, (byte) 2);
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
            nextByte();
            n--;
        }
    }

    /**
     * Counts the remaining elements by consuming them all.
     * After this call the iterator is exhausted.
     * This default implementation calls {@link #nextByte()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
     * assertEquals(3, iter.count());
     * assertFalse(iter.hasNext()); // iterator is exhausted after count()
     *
     * // Empty iterator returns 0
     * ByteIteratorEx empty = ByteIteratorEx.empty();
     * assertEquals(0, empty.count());
     * }</pre>
     *
     * @return the number of elements that remained before this call
     */
    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextByte();
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
     * ByteIteratorEx iter = ByteIteratorEx.of((byte) 1, (byte) 2);
     * iter.closeResource(); // releases resources (a no-op for this implementation)
     * assertTrue(iter.hasNext()); // closeResource() does not consume elements
     *
     * // Safe to call closeResource() multiple times
     * ByteIteratorEx iter2 = ByteIteratorEx.of((byte) 3, (byte) 4);
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
