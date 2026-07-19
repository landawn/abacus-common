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
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.N;

/**
 * An extended iterator over primitive double values with additional functionality.
 * This class extends {@link DoubleIterator} and implements {@link IteratorEx} to provide
 * advanced iteration capabilities such as skipping elements, counting remaining elements,
 * and converting to arrays/lists.
 *
 * <p>This is an internal API marked with {@link Internal} annotation and is subject to change.
 * It provides factory methods for creating iterators from arrays and other iterators.
 *
 * @see DoubleIterator
 * @see IteratorEx
 */
@SuppressWarnings({ "java:S6548" })
@Internal
public abstract class DoubleIteratorEx extends DoubleIterator implements IteratorEx<Double> {

    /**
     * Constructs a new DoubleIteratorEx.
     * This constructor is protected to allow subclassing.
     */
    protected DoubleIteratorEx() {
    }

    /**
     * An empty DoubleIteratorEx instance that contains no elements.
     * Calling {@code hasNext()} always returns {@code false}, and calling {@code nextDouble()}
     * throws a {@link NoSuchElementException}.
     */
    @SuppressWarnings({ "java:S1845" })
    public static final DoubleIteratorEx EMPTY = new DoubleIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public double nextDouble() {
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
        public double[] toArray() {
            return N.EMPTY_DOUBLE_ARRAY;
        }

    };

    /**
     * Returns an empty DoubleIteratorEx with no elements.
     * This iterator's hasNext() will always return {@code false} and nextDouble() will throw NoSuchElementException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIteratorEx iter = DoubleIteratorEx.empty();
     * assertFalse(iter.hasNext());
     * }</pre>
     *
     * @return an empty DoubleIteratorEx instance
     */
    @SuppressWarnings({ "java:S1845", "SameReturnValue" })
    public static DoubleIteratorEx empty() {
        return EMPTY;
    }

    /**
     * Creates a DoubleIteratorEx from the given double array.
     * Returns an empty iterator if the array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIteratorEx iter = DoubleIteratorEx.of(1.0, 2.5, 3.14);
     * assertEquals(1.0, iter.nextDouble(), 0.0);
     * }</pre>
     *
     * @param a the double array to iterate over (can be {@code null} or empty)
     * @return a DoubleIteratorEx for the given array, or empty iterator if array is null/empty
     */
    public static DoubleIteratorEx of(final double... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a DoubleIteratorEx from a portion of the given double array.
     * The iterator will include elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.0, 3.0, 4.0, 5.0};
     * DoubleIteratorEx iter = DoubleIteratorEx.of(arr, 1, 4);
     * assertEquals(2.0, iter.nextDouble(), 0.0);   // starts at index 1
     * }</pre>
     *
     * @param a the double array to iterate over
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a DoubleIteratorEx for the specified array range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
     */
    public static DoubleIteratorEx of(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new DoubleIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
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
            public double[] toArray() {
                final double[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public DoubleList toList() {
                final DoubleList ret = DoubleList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

        };
    }

    /**
     * Wraps a DoubleIterator as a DoubleIteratorEx.
     * If the iterator is already a DoubleIteratorEx, it is returned as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
     * DoubleIteratorEx iterEx = DoubleIteratorEx.of(iter);
     * }</pre>
     *
     * @param iter the DoubleIterator to wrap (can be null)
     * @return the same instance if {@code iter} is already a DoubleIteratorEx, a DoubleIteratorEx wrapping the given iterator, or an empty iterator if {@code iter} is null
     */
    public static DoubleIteratorEx of(final DoubleIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof DoubleIteratorEx) {
            return ((DoubleIteratorEx) iter);
        }

        return new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                return iter.nextDouble();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Creates a DoubleIteratorEx from an Iterator of Double objects.
     * Automatically unwraps Double objects to primitive doubles.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Double> iter = List.of(1.0, 2.5, 3.14).iterator();
     * DoubleIteratorEx iterEx = DoubleIteratorEx.from(iter);
     * }</pre>
     *
     * @param iter the Iterator of Double objects (can be null)
     * @return a DoubleIteratorEx unwrapping the given iterator, or empty iterator if iter is null
     * @throws NullPointerException during iteration if any element returned by the source iterator is {@code null}
     */
    public static DoubleIteratorEx from(final Iterator<Double> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx<Double> iteratorEx) {

            return new DoubleIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public double nextDouble() {
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
            return new DoubleIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public double nextDouble() {
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
     * This default implementation calls {@link #nextDouble()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIteratorEx iter = DoubleIteratorEx.of(1.0, 2.0, 3.0, 4.0, 5.0);
     * iter.advance(3);
     * assertEquals(4.0, iter.nextDouble(), 0.0);
     *
     * // Non-positive values are ignored
     * DoubleIteratorEx iter2 = DoubleIteratorEx.of(1.0, 2.0);
     * iter2.advance(0);
     * assertEquals(1.0, iter2.nextDouble(), 0.0);
     *
     * // Advancing beyond available elements exhausts the iterator
     * DoubleIteratorEx iter3 = DoubleIteratorEx.of(1.0, 2.0);
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
            nextDouble();
            n--;
        }
    }

    /**
     * Counts the remaining elements by consuming them all.
     * After this call the iterator is exhausted.
     * This default implementation calls {@link #nextDouble()} repeatedly.
     * Subclasses may override for a more efficient implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIteratorEx iter = DoubleIteratorEx.of(1.0, 2.0, 3.0);
     * assertEquals(3, iter.count());
     * assertFalse(iter.hasNext()); // iterator is exhausted after count()
     *
     * // Empty iterator returns 0
     * DoubleIteratorEx empty = DoubleIteratorEx.empty();
     * assertEquals(0, empty.count());
     * }</pre>
     *
     * @return the number of elements that remained before this call
     */
    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextDouble();
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
     * DoubleIteratorEx iter = DoubleIteratorEx.of(1.0, 2.0);
     * iter.closeResource();         // releases resources (a no-op for this implementation)
     * assertTrue(iter.hasNext());   // closeResource() does not consume elements
     *
     * // Safe to call closeResource() multiple times
     * DoubleIteratorEx iter2 = DoubleIteratorEx.of(3.0, 4.0);
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
