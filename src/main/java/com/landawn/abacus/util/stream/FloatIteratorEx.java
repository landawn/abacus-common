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
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.N;

/**
 * An extended iterator over primitive float values with additional functionality.
 * This class extends {@link FloatIterator} and implements {@link IteratorEx} to provide
 * advanced iteration capabilities such as skipping elements, counting remaining elements,
 * and converting to arrays/lists.
 *
 * <p>This is an internal API marked with {@link Internal} annotation and is subject to change.
 * It provides factory methods for creating iterators from arrays and other iterators.
 *
 * @see FloatIterator
 * @see IteratorEx
 */
@SuppressWarnings({ "java:S6548" })
@Internal
public abstract class FloatIteratorEx extends FloatIterator implements IteratorEx<Float> {

    /**
     * Constructs a new FloatIteratorEx.
     * This constructor is protected to allow subclassing.
     */
    protected FloatIteratorEx() {
    }

    /**
     * An empty FloatIteratorEx instance that contains no elements.
     * Calling {@code hasNext()} always returns {@code false}, and calling {@code nextFloat()}
     * throws a {@link NoSuchElementException}.
     */
    @SuppressWarnings({ "java:S1845" })
    public static final FloatIteratorEx EMPTY = new FloatIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public float nextFloat() {
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
        public float[] toArray() {
            return N.EMPTY_FLOAT_ARRAY;
        }

    };

    /**
     * Returns an empty FloatIteratorEx with no elements.
     * This iterator's hasNext() will always return {@code false} and nextFloat() will throw NoSuchElementException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIteratorEx iter = FloatIteratorEx.empty();
     * assertFalse(iter.hasNext());
     * }</pre>
     *
     * @return an empty FloatIteratorEx instance
     */
    @SuppressWarnings({ "java:S1845", "SameReturnValue" })
    public static FloatIteratorEx empty() {
        return EMPTY;
    }

    /**
     * Creates a FloatIteratorEx from the given float array.
     * Returns an empty iterator if the array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIteratorEx iter = FloatIteratorEx.of(1.0f, 2.5f, 3.14f);
     * assertEquals(1.0f, iter.nextFloat(), 0.0f);
     * }</pre>
     *
     * @param a the float array to iterate over (can be {@code null} or empty)
     * @return a FloatIteratorEx for the given array, or empty iterator if array is null/empty
     */
    public static FloatIteratorEx of(final float... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a FloatIteratorEx from a portion of the given float array.
     * The iterator will include elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
     * FloatIteratorEx iter = FloatIteratorEx.of(arr, 1, 4);
     * assertEquals(2.0f, iter.nextFloat(), 0.0f);   // starts at index 1
     * }</pre>
     *
     * @param a the float array to iterate over
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a FloatIteratorEx for the specified array range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
     */
    public static FloatIteratorEx of(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new FloatIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
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
            public float[] toArray() {
                final float[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public FloatList toList() {
                final FloatList ret = FloatList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

        };
    }

    /**
     * Wraps a FloatIterator as a FloatIteratorEx.
     * If the iterator is already a FloatIteratorEx, it is returned as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1f, 2f, 3f);
     * FloatIteratorEx iterEx = FloatIteratorEx.of(iter);
     * }</pre>
     *
     * @param iter the FloatIterator to wrap (can be null)
     * @return the same instance if {@code iter} is already a FloatIteratorEx, a FloatIteratorEx wrapping the given iterator, or an empty iterator if {@code iter} is null
     */
    public static FloatIteratorEx of(final FloatIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof FloatIteratorEx) {
            return ((FloatIteratorEx) iter);
        }

        return new FloatIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                return iter.nextFloat();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Creates a FloatIteratorEx from an Iterator of Float objects.
     * Automatically unwraps Float objects to primitive floats.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Float> iter = List.of(1.0f, 2.5f, 3.14f).iterator();
     * FloatIteratorEx iterEx = FloatIteratorEx.from(iter);
     * }</pre>
     *
     * @param iter the Iterator of Float objects (can be null)
     * @return a FloatIteratorEx unwrapping the given iterator, or empty iterator if iter is null
     * @throws NullPointerException during iteration if any element returned by the source iterator is {@code null}
     */
    public static FloatIteratorEx from(final Iterator<Float> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx<Float> iteratorEx) {

            return new FloatIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public float nextFloat() {
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
            return new FloatIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public float nextFloat() {
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
     * This default implementation calls {@link #nextFloat()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIteratorEx iter = FloatIteratorEx.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
     * iter.advance(3);
     * assertEquals(4.0f, iter.nextFloat(), 0.0f);
     *
     * // Non-positive values are ignored
     * FloatIteratorEx iter2 = FloatIteratorEx.of(1.0f, 2.0f);
     * iter2.advance(0);
     * assertEquals(1.0f, iter2.nextFloat(), 0.0f);
     *
     * // Advancing beyond available elements exhausts the iterator
     * FloatIteratorEx iter3 = FloatIteratorEx.of(1.0f, 2.0f);
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
            nextFloat();
            n--;
        }
    }

    /**
     * Counts the remaining elements by consuming them all.
     * After this call the iterator is exhausted.
     * This default implementation calls {@link #nextFloat()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIteratorEx iter = FloatIteratorEx.of(1.0f, 2.0f, 3.0f);
     * assertEquals(3, iter.count());
     * assertFalse(iter.hasNext()); // iterator is exhausted after count()
     *
     * // Empty iterator returns 0
     * FloatIteratorEx empty = FloatIteratorEx.empty();
     * assertEquals(0, empty.count());
     * }</pre>
     *
     * @return the number of elements that remained before this call
     */
    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextFloat();
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
     * FloatIteratorEx iter = FloatIteratorEx.of(1.0f, 2.0f);
     * iter.closeResource(); // releases resources (a no-op for this implementation)
     * assertTrue(iter.hasNext()); // closeResource() does not consume elements
     *
     * // Safe to call closeResource() multiple times
     * FloatIteratorEx iter2 = FloatIteratorEx.of(3.0f, 4.0f);
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
