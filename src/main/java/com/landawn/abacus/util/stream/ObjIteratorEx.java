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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.cs;

/**
 * An extended iterator over objects with additional functionality.
 * This class extends {@link ObjIterator} and implements {@link IteratorEx} to provide
 * advanced iteration capabilities such as skipping elements, counting remaining elements,
 * and converting to arrays/lists.
 *
 * <p>This is an internal API marked with {@link Internal} annotation and is subject to change.
 * It provides factory methods for creating iterators from arrays, collections, and other sources,
 * as well as support for deferred initialization.
 *
 * @param <T> the type of elements returned by this iterator
 * @see ObjIterator
 * @see IteratorEx
 */
@SuppressWarnings({ "java:S6548" })
@Internal
public abstract class ObjIteratorEx<T> extends ObjIterator<T> implements IteratorEx<T> {

    /**
     * Constructs a new ObjIteratorEx.
     * This constructor is protected to allow subclassing.
     */
    protected ObjIteratorEx() {
    }

    /**
     * Returns an empty ObjIteratorEx with no elements.
     * This iterator's hasNext() will always return {@code false} and next() will throw NoSuchElementException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIteratorEx<String> iter = ObjIteratorEx.empty();
     * assertFalse(iter.hasNext());
     * }</pre>
     *
     * @param <T> the type of elements
     * @return an empty ObjIteratorEx instance
     */
    @SuppressWarnings({ "java:S1845", "SameReturnValue" })
    public static <T> ObjIteratorEx<T> empty() {
        return BufferedIterator.EMPTY;
    }

    /**
     * Creates an ObjIteratorEx from the given array.
     * Returns an empty iterator if the array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIteratorEx<String> iter = ObjIteratorEx.of("a", "b", "c");
     * assertEquals("a", iter.next());
     * }</pre>
     *
     * @param <T> the type of elements
     * @param a the array to iterate over (can be {@code null} or empty)
     * @return an ObjIteratorEx for the given array, or empty iterator if array is null/empty
     */
    @SafeVarargs
    public static <T> ObjIteratorEx<T> of(final T... a) {
        return a == null ? empty() : of(a, 0, a.length);
    }

    /**
     * Creates an ObjIteratorEx from a portion of the given array.
     * The iterator will include elements from fromIndex (inclusive) to toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"a", "b", "c", "d", "e"};
     * ObjIteratorEx<String> iter = ObjIteratorEx.of(arr, 1, 4);
     * assertEquals("b", iter.next());   // starts at index 1
     * }</pre>
     *
     * @param <T> the type of elements
     * @param a the array to iterate over
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return an ObjIteratorEx for the specified array range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length},
     *         or {@code fromIndex > toIndex}
     */
    public static <T> ObjIteratorEx<T> of(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return empty();
        }

        return new BufferedIterator<>(toIndex - fromIndex) {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() {
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
            public <A> A[] toArray(A[] output) {
                final int len = toIndex - cursor;

                if (output.length < len) {
                    output = N.copyOf(output, len);
                }

                N.copy(a, cursor, output, 0, len);

                if (output.length > len) {
                    output[len] = null;
                }

                cursor = toIndex; // Move cursor to the end after copying.

                return output;
            }

            @Override
            public List<T> toList() {
                return N.toList((T[]) toArray());
            }

        };
    }

    /**
     * Creates an ObjIteratorEx from a Collection.
     * Returns an empty iterator if the collection is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjIteratorEx<String> iter = ObjIteratorEx.of(list);
     * long count = iter.count();   // count is 3
     * }</pre>
     *
     * @param <T> the type of elements
     * @param c the collection to iterate over (can be {@code null})
     * @return an ObjIteratorEx over the collection, or an empty iterator if {@code c} is {@code null}
     */
    public static <T> ObjIteratorEx<T> of(final Collection<? extends T> c) {
        if (c == null) {
            return empty();
        }

        final Iterator<? extends T> iter = c.iterator();

        return new BufferedIterator<>(c.size()) {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Wraps an Iterator as an ObjIteratorEx.
     * If the iterator is already an ObjIteratorEx, it is returned as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
     * ObjIteratorEx<String> iterEx = ObjIteratorEx.of(iter);
     * }</pre>
     *
     * @param <T> the type of elements
     * @param iter the Iterator to wrap (can be null)
     * @return the same instance if {@code iter} is already an ObjIteratorEx, an ObjIteratorEx wrapping the given iterator, or an empty iterator if {@code iter} is null
     */
    public static <T> ObjIteratorEx<T> of(final Iterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx) {
            return ((ObjIteratorEx<T>) iter);
        }

        return new ObjIteratorEx<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }

            @Override
            public void closeResource() {
                ObjIteratorEx.closeResource(iter);
            }
        };
    }

    /**
     * Creates an ObjIteratorEx from an Iterable.
     * Returns an empty iterator if the iterable is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterable<String> iterable = Arrays.asList("a", "b", "c");
     * ObjIteratorEx<String> iter = ObjIteratorEx.of(iterable);
     * iter.toList();
     * }</pre>
     *
     * @param <T> the type of elements
     * @param iterable the Iterable to iterate over (can be {@code null})
     * @return an ObjIteratorEx over the iterable, or an empty iterator if {@code iterable} is {@code null}
     */
    public static <T> ObjIteratorEx<T> of(final Iterable<? extends T> iterable) {
        return iterable == null ? ObjIteratorEx.empty() : of(iterable.iterator());
    }

    /**
     * Creates a deferred ObjIteratorEx that initializes the underlying iterator lazily.
     * The iterator is created only when iteration begins (first call to hasNext or next).
     * This is useful for expensive initialization operations that should only occur when actually needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example: Lazy file loading - file is only opened when iteration starts
     * ObjIteratorEx<String> iter = ObjIteratorEx.defer(() -> {
     *     try {
     *         return Files.lines(Paths.get("large-file.txt")).iterator();
     *     } catch (IOException e) {
     *         throw new UncheckedIOException(e);
     *     }
     * });
     * // File is not opened yet, no I/O has occurred
     *
     * // Later, when iteration begins...
     * if (someCondition) {
     *     iter.hasNext();   // creates the iterator and opens the file here
     *     while (iter.hasNext()) {
     *         process(iter.next());
     *     }
     *     iter.closeResource();   // closes the underlying iterator if it implements IteratorEx or AutoCloseable
     * }
     * // If someCondition is false, file is never opened
     * }</pre>
     *
     * <p>Note: {@code closeResource()} closes the supplied iterator only when that iterator implements
     * {@link IteratorEx} or {@link AutoCloseable}. A plain {@code java.util.stream.Stream.iterator()} (such as
     * {@code Files.lines(...).iterator()}) is neither; to release its resource, supply an iterator that
     * closes the stream (or close the stream yourself).</p>
     *
     * @param <T> the type of elements
     * @param iteratorSupplier the supplier that provides the iterator
     * @return a deferred ObjIteratorEx
     * @throws IllegalArgumentException if iteratorSupplier is null
     */
    public static <T> ObjIteratorEx<T> defer(final Supplier<? extends Iterator<? extends T>> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new ObjIteratorEx<>() {
            private Iterator<? extends T> iter = null;
            private IteratorEx<? extends T> iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (!isInitialized) {
                    init();
                }

                return iter.next();
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!isInitialized) {
                    init();
                }

                if (iterEx != null) {
                    iterEx.advance(n);
                } else {
                    super.advance(n);
                }
            }

            @Override
            public long count() {
                if (!isInitialized) {
                    init();
                }

                if (iterEx != null) {
                    return iterEx.count();
                } else {
                    return super.count();
                }
            }

            @Override
            public void closeResource() {
                // Don't force init() here. Closing a deferred iterator that was never iterated
                // should be a no-op; otherwise we'd open a resource (file/cursor/connection)
                // just to immediately close it, defeating the lazy-on-first-use contract.
                if (isInitialized) {
                    if (iterEx != null) {
                        iterEx.closeResource();
                    } else if (iter instanceof AutoCloseable) {
                        ObjIteratorEx.closeResource(iter);
                    }
                }
            }

            private void init() {
                if (!isInitialized) {
                    isInitialized = true;
                    iter = iteratorSupplier.get();
                    iterEx = iter instanceof IteratorEx ? (IteratorEx<T>) iter : null;
                }
            }
        };
    }

    static void closeResource(final Object iter) {
        if (iter instanceof IteratorEx) {
            ((IteratorEx<?>) iter).closeResource();
        } else if (iter instanceof AutoCloseable) {
            try {
                ((AutoCloseable) iter).close();
            } catch (Exception e) {
                throw N.toRuntimeException(e);
            }
        }
    }

    /**
     * Closes this iterator and releases any resources it holds.
     * The default implementation is a no-op. Subclasses that hold
     * resources should override this method.
     */
    @Override
    public void closeResource() {
        // Do nothing.
    }

    /**
     * An {@link ObjIteratorEx} that carries a known/expected element count so downstream
     * collectors can size their buffers up-front (avoiding repeated growth/copy when sinking
     * into arrays or lists).
     *
     * <p>The count is exposed via {@link #max()} and is a capacity hint only — the iterator
     * may yield fewer elements (the exact count is not enforced).
     *
     * @param <T> the type of elements returned by this iterator
     */
    abstract static class BufferedIterator<T> extends ObjIteratorEx<T> {
        /**
         * A shared empty {@link BufferedIterator} instance whose {@link #max()} reports {@code 0}.
         * {@link #hasNext()} always returns {@code false} and {@link #next()} throws
         * {@link NoSuchElementException}.
         */
        @SuppressWarnings({ "java:S1845", "rawtypes" })
        public static final BufferedIterator EMPTY = new BufferedIterator(0) {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Object next() {
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

        };

        private final int bufferSize;

        /**
         * Constructs a BufferedIterator carrying the given expected element count.
         *
         * @param bufferSize the expected/upper-bound number of elements; used by downstream
         *        consumers as a sizing hint (see {@link #max()})
         */
        BufferedIterator(final int bufferSize) {
            this.bufferSize = bufferSize;
        }

        /**
         * Returns an empty BufferedIterator instance with no elements.
         * This iterator has no elements and all terminal operations return immediately.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BufferedIterator<String> iter = BufferedIterator.empty();
         * iter.hasNext();   // false
         * }</pre>
         *
         * @param <T> the type of elements
         * @return an empty BufferedIterator
         */
        @SuppressWarnings({ "java:S1845", "SameReturnValue" })
        public static <T> BufferedIterator<T> empty() {
            return EMPTY;
        }

        /**
         * Returns the upper-bound element count carried by this iterator.
         * This is the value supplied at construction time and is intended as a sizing
         * hint for downstream consumers; the iterator may actually yield fewer elements.
         *
         * @return the expected/upper-bound number of remaining elements
         */
        public int max() {
            return bufferSize;
        }
    }
}
