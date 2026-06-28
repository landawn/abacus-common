/*
 * Copyright (C) 2021 HaiYang Li
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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.cs;

/**
 * A dynamically growing append-only buffer that uses a spine-and-chunk layout instead of a single
 * resizable backing array. The first chunk has size {@code initialCapacity}; subsequent chunks
 * have a fixed size and are tracked by a spine (an array of chunk arrays) which itself grows in
 * blocks. This avoids the O(n) copy cost of a doubling {@code ArrayList}-style buffer while
 * keeping per-element access cheap.
 *
 * <p>It implements {@link Consumer} so it can be passed directly as the accumulator for stream
 * collection operations.
 *
 * <p><b>Note:</b> This buffer is append-only — {@link #remove(Object)} and
 * {@link #removeAll(Collection)} both throw {@link UnsupportedOperationException}.
 *
 * @param <E> the type of elements in this buffer
 */
final class SpinedBuffer<E> extends AbstractCollection<E> implements Consumer<E> {
    private static final int CHUNK_SIZE = 9;
    private static final int SPINE_SIZE_TO_INCREASE = 8;

    private final int initialCapacity;
    private E[][] spine = null; //NOSONAR
    private E[] curChunk = (E[]) N.EMPTY_OBJECT_ARRAY; //NOSONAR
    private int size = 0; //NOSONAR

    /**
     * Constructs a SpinedBuffer with the default initial capacity.
     * The default capacity is optimized for typical stream operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a buffer with default capacity
     * SpinedBuffer<String> buffer = new SpinedBuffer<>();
     * buffer.add("element1");
     * buffer.add("element2");
     * System.out.println(buffer.size());   // prints 2
     * }</pre>
     *
     */
    public SpinedBuffer() {
        this(CHUNK_SIZE);
    }

    /**
     * Constructs a SpinedBuffer with the specified initial capacity.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a buffer with specific initial capacity
     * SpinedBuffer<Integer> buffer = new SpinedBuffer<>(100);
     * for (int i = 0; i < 150; i++) {
     *     buffer.add(i);   // adds an element, growing beyond initial capacity
     * }
     * System.out.println(buffer.size());   // prints 150
     * }</pre>
     *
     * @param initialCapacity the initial capacity for the buffer
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public SpinedBuffer(final int initialCapacity) throws IllegalArgumentException {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity); //NOSONAR

        this.initialCapacity = initialCapacity == 0 ? CHUNK_SIZE : initialCapacity;

        if (initialCapacity > 0) {
            curChunk = (E[]) new Object[initialCapacity];
        }
    }

    /**
     * Appends the specified element to this buffer.
     * This method delegates to {@link #add(Object)}.
     *
     * @param t the element to append
     */
    @Override
    public void accept(final E t) {
        this.add(t);
    }

    /**
     * Appends the specified element to this buffer.
     * The buffer grows automatically using the spine-and-chunk structure as needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SpinedBuffer<String> buffer = new SpinedBuffer<>();
     * buffer.add("hello");
     * buffer.add("world");
     * System.out.println(buffer.size());   // prints 2
     * }</pre>
     *
     * @param e the element to be appended
     * @return {@code true} always
     */
    @Override
    public boolean add(final E e) {
        if (size < initialCapacity) {
            if (curChunk.length == 0) {
                curChunk = (E[]) new Object[CHUNK_SIZE];
            }

            curChunk[size] = e;
        } else {
            if (spine == null) {
                spine = (E[][]) new Object[SPINE_SIZE_TO_INCREASE][];
                spine[0] = curChunk;
                spine[1] = (E[]) new Object[CHUNK_SIZE];
                curChunk = spine[1];
            } else {
                final int chunkIndex = (size - spine[0].length) / CHUNK_SIZE + 1;

                if (spine.length <= chunkIndex) {
                    spine = N.copyOf(spine, spine.length + SPINE_SIZE_TO_INCREASE);
                }

                if (spine[chunkIndex] == null) {
                    curChunk = (E[]) new Object[CHUNK_SIZE];
                    spine[chunkIndex] = curChunk;
                }
            }

            curChunk[(size - spine[0].length) % CHUNK_SIZE] = e;
        }

        size++;
        return true;
    }

    /**
     * This operation is not supported.
     *
     * @param o the element to remove (not used)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public boolean remove(final Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("SpinedBuffer does not support element removal");
    }

    /**
     * This operation is not supported.
     *
     * @param c the collection of elements to remove (not used)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public boolean removeAll(final Collection<?> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("SpinedBuffer does not support element removal");
    }

    /**
     * Returns an iterator over the elements in this buffer in proper sequence.
     *
     * @return an iterator over the elements in this buffer
     */
    @Override
    public Iterator<E> iterator() {
        if (N.isEmpty(spine)) {
            return ObjIterator.of(curChunk, 0, size);
        } else {
            final int localSize = size();

            return new ObjIterator<>() {
                private final E[] firstChunk = spine[0];
                private final int firstChunkLen = firstChunk.length;
                private int cursor = 0;
                private E next = null;

                @Override
                public boolean hasNext() {
                    return cursor < localSize;
                }

                @Override
                public E next() {
                    if (cursor >= localSize) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    if (cursor < firstChunkLen) {
                        return firstChunk[cursor++];
                    } else {
                        next = spine[(cursor - firstChunkLen) / CHUNK_SIZE + 1][(cursor - firstChunkLen) % CHUNK_SIZE];

                        cursor++;

                        return next;
                    }
                }
            };
        }
    }

    /**
     * Returns the number of elements in this buffer.
     *
     * @return the number of elements in this buffer
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * A specialized SpinedBuffer for primitive int values.
     * This class avoids boxing overhead by storing primitive ints directly.
     * Similar to the generic SpinedBuffer, it uses a spine-and-chunk structure
     * for efficient memory management.
     */
    static class OfInt implements IntConsumer {
        private final int initialCapacity;
        private int[][] spine = null;
        private int[] curChunk = N.EMPTY_INT_ARRAY;
        private int size = 0;

        /**
         * Constructs a SpinedBuffer.OfInt with the default initial capacity.
         * The default capacity is optimized for typical stream operations.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create an int buffer with default capacity
         * SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();
         * buffer.add(1);
         * buffer.add(2);
         * buffer.add(3);
         * System.out.println(buffer.size());   // prints 3
         * }</pre>
         *
         */
        public OfInt() {
            this(CHUNK_SIZE);
        }

        /**
         * Constructs a SpinedBuffer.OfInt with the specified initial capacity.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create an int buffer with specific initial capacity
         * SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt(50);
         * for (int i = 0; i < 100; i++) {
         *     buffer.add(i);   // adds an element, growing beyond initial capacity
         * }
         * System.out.println(buffer.size());   // prints 100
         * }</pre>
         *
         * @param initialCapacity the initial capacity for the buffer
         * @throws IllegalArgumentException if the initial capacity is negative
         */
        public OfInt(final int initialCapacity) throws IllegalArgumentException {
            N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

            this.initialCapacity = initialCapacity == 0 ? CHUNK_SIZE : initialCapacity;

            if (initialCapacity > 0) {
                curChunk = new int[initialCapacity];
            }
        }

        /**
         * Appends the specified int value to this buffer.
         * This method delegates to {@link #add(int)}.
         *
         * @param t the int value to append
         */
        @Override
        public void accept(final int t) {
            this.add(t);
        }

        /**
         * Adds the specified int value to this buffer.
         * The buffer automatically grows as needed using the spine-and-chunk structure.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();
         * buffer.add(10);
         * buffer.add(20);
         * buffer.add(30);
         *
         * // Use with IntStream operations
         * IntStream.range(0, 10).forEach(buffer::accept);
         * System.out.println(buffer.size());   // prints 13
         * }</pre>
         *
         * @param e the int value to be added
         * @return {@code true} always
         */
        @SuppressWarnings("SameReturnValue")
        public boolean add(final int e) {
            if (size < initialCapacity) {
                if (curChunk.length == 0) {
                    curChunk = new int[CHUNK_SIZE];
                }

                curChunk[size] = e;
            } else {
                if (spine == null) {
                    spine = new int[SPINE_SIZE_TO_INCREASE][];
                    spine[0] = curChunk;
                    spine[1] = new int[CHUNK_SIZE];
                    curChunk = spine[1];
                } else {
                    final int chunkIndex = (size - spine[0].length) / CHUNK_SIZE + 1;

                    if (spine.length <= chunkIndex) {
                        spine = N.copyOf(spine, spine.length + SPINE_SIZE_TO_INCREASE);
                    }

                    if (spine[chunkIndex] == null) {
                        curChunk = new int[CHUNK_SIZE];
                        spine[chunkIndex] = curChunk;
                    }
                }

                curChunk[(size - spine[0].length) % CHUNK_SIZE] = e;
            }

            size++;
            return true;
        }

        /**
         * Returns an iterator over the int values in this buffer in proper sequence.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();
         * buffer.add(100);
         * buffer.add(200);
         * buffer.add(300);
         *
         * // Iterate over int values
         * IntIterator iter = buffer.iterator();
         * while (iter.hasNext()) {
         *     System.out.println(iter.nextInt());
         * }
         * }</pre>
         *
         * @return an IntIterator over the int values in this buffer
         */
        public IntIterator iterator() {
            if (N.isEmpty(spine)) {
                return IntIterator.of(curChunk, 0, size);
            } else {
                final int localSize = size();

                return new IntIterator() {
                    private final int[] firstChunk = spine[0];
                    private final int firstChunkLen = firstChunk.length;
                    private int cursor = 0;
                    private int next = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor < localSize;
                    }

                    @Override
                    public int nextInt() {
                        if (cursor >= localSize) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        if (cursor < firstChunkLen) {
                            return firstChunk[cursor++];
                        } else {
                            next = spine[(cursor - firstChunkLen) / CHUNK_SIZE + 1][(cursor - firstChunkLen) % CHUNK_SIZE];

                            cursor++;

                            return next;
                        }
                    }
                };
            }
        }

        /**
         * Returns the number of int values in this buffer.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();
         * System.out.println(buffer.size());   // prints 0
         *
         * buffer.add(5);
         * buffer.add(10);
         * System.out.println(buffer.size());   // prints 2
         * }</pre>
         *
         * @return the number of int values in this buffer
         */
        public int size() {
            return size;
        }
    }

    /**
     * A specialized SpinedBuffer for primitive long values.
     * This class avoids boxing overhead by storing primitive longs directly.
     * Similar to the generic SpinedBuffer, it uses a spine-and-chunk structure
     * for efficient memory management.
     */
    static class OfLong implements LongConsumer {
        private final int initialCapacity;
        private long[][] spine = null;
        private long[] curChunk = N.EMPTY_LONG_ARRAY;
        private int size = 0;

        /**
         * Constructs a SpinedBuffer.OfLong with the default initial capacity.
         * The default capacity is optimized for typical stream operations.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create a long buffer with default capacity
         * SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();
         * buffer.add(1000L);
         * buffer.add(2000L);
         * buffer.add(3000L);
         * System.out.println(buffer.size());   // prints 3
         * }</pre>
         *
         */
        public OfLong() {
            this(CHUNK_SIZE);
        }

        /**
         * Constructs a SpinedBuffer.OfLong with the specified initial capacity.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create a long buffer with specific initial capacity
         * SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong(50);
         * for (long i = 0; i < 100; i++) {
         *     buffer.add(i * 1000L);   // adds an element, growing beyond initial capacity
         * }
         * System.out.println(buffer.size());   // prints 100
         * }</pre>
         *
         * @param initialCapacity the initial capacity for the buffer
         * @throws IllegalArgumentException if the initial capacity is negative
         */
        public OfLong(final int initialCapacity) throws IllegalArgumentException {
            N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

            this.initialCapacity = initialCapacity == 0 ? CHUNK_SIZE : initialCapacity;

            if (initialCapacity > 0) {
                curChunk = new long[initialCapacity];
            }
        }

        /**
         * Appends the specified long value to this buffer.
         * This method delegates to {@link #add(long)}.
         *
         * @param t the long value to append
         */
        @Override
        public void accept(final long t) {
            this.add(t);
        }

        /**
         * Adds the specified long value to this buffer.
         * The buffer automatically grows as needed using the spine-and-chunk structure.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();
         * buffer.add(100L);
         * buffer.add(200L);
         * buffer.add(300L);
         *
         * // Use with LongStream operations
         * LongStream.range(0, 10).forEach(buffer::accept);
         * System.out.println(buffer.size());   // prints 13
         * }</pre>
         *
         * @param e the long value to be added
         * @return {@code true} always
         */
        @SuppressWarnings("SameReturnValue")
        public boolean add(final long e) {
            if (size < initialCapacity) {
                if (curChunk.length == 0) {
                    curChunk = new long[CHUNK_SIZE];
                }

                curChunk[size] = e;
            } else {
                if (spine == null) {
                    spine = new long[SPINE_SIZE_TO_INCREASE][];
                    spine[0] = curChunk;
                    spine[1] = new long[CHUNK_SIZE];
                    curChunk = spine[1];
                } else {
                    final int chunkIndex = (size - spine[0].length) / CHUNK_SIZE + 1;

                    if (spine.length <= chunkIndex) {
                        spine = N.copyOf(spine, spine.length + SPINE_SIZE_TO_INCREASE);
                    }

                    if (spine[chunkIndex] == null) {
                        curChunk = new long[CHUNK_SIZE];
                        spine[chunkIndex] = curChunk;
                    }
                }

                curChunk[(size - spine[0].length) % CHUNK_SIZE] = e;
            }

            size++;
            return true;
        }

        /**
         * Returns an iterator over the long values in this buffer in proper sequence.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();
         * buffer.add(1000L);
         * buffer.add(2000L);
         * buffer.add(3000L);
         *
         * // Iterate over long values
         * LongIterator iter = buffer.iterator();
         * while (iter.hasNext()) {
         *     System.out.println(iter.nextLong());
         * }
         * }</pre>
         *
         * @return a LongIterator over the long values in this buffer
         */
        public LongIterator iterator() {
            if (N.isEmpty(spine)) {
                return LongIterator.of(curChunk, 0, size);
            } else {
                final int localSize = size();

                return new LongIterator() {
                    private final long[] firstChunk = spine[0];
                    private final int firstChunkLen = firstChunk.length;
                    private int cursor = 0;
                    private long next = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor < localSize;
                    }

                    @Override
                    public long nextLong() {
                        if (cursor >= localSize) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        if (cursor < firstChunkLen) {
                            return firstChunk[cursor++];
                        } else {
                            next = spine[(cursor - firstChunkLen) / CHUNK_SIZE + 1][(cursor - firstChunkLen) % CHUNK_SIZE];

                            cursor++;

                            return next;
                        }
                    }
                };
            }
        }

        /**
         * Returns the number of long values in this buffer.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();
         * System.out.println(buffer.size());   // prints 0
         *
         * buffer.add(5000L);
         * buffer.add(10000L);
         * System.out.println(buffer.size());   // prints 2
         * }</pre>
         *
         * @return the number of long values in this buffer
         */
        public int size() {
            return size;
        }
    }

    /**
     * A specialized SpinedBuffer for primitive double values.
     * This class avoids boxing overhead by storing primitive doubles directly.
     * Similar to the generic SpinedBuffer, it uses a spine-and-chunk structure
     * for efficient memory management.
     */
    static class OfDouble implements DoubleConsumer {
        private final int initialCapacity;
        private double[][] spine = null;
        private double[] curChunk = N.EMPTY_DOUBLE_ARRAY;
        private int size = 0;

        /**
         * Constructs a SpinedBuffer.OfDouble with the default initial capacity.
         * The default capacity is optimized for typical stream operations.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create a double buffer with default capacity
         * SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();
         * buffer.add(1.5);
         * buffer.add(2.5);
         * buffer.add(3.5);
         * System.out.println(buffer.size());   // prints 3
         * }</pre>
         *
         */
        public OfDouble() {
            this(CHUNK_SIZE);
        }

        /**
         * Constructs a SpinedBuffer.OfDouble with the specified initial capacity.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create a double buffer with specific initial capacity
         * SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble(50);
         * for (int i = 0; i < 100; i++) {
         *     buffer.add(i * 0.5);   // adds an element, growing beyond initial capacity
         * }
         * System.out.println(buffer.size());   // prints 100
         * }</pre>
         *
         * @param initialCapacity the initial capacity for the buffer
         * @throws IllegalArgumentException if the initial capacity is negative
         */
        public OfDouble(final int initialCapacity) throws IllegalArgumentException {
            N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

            this.initialCapacity = initialCapacity == 0 ? CHUNK_SIZE : initialCapacity;

            if (initialCapacity > 0) {
                curChunk = new double[initialCapacity];
            }
        }

        /**
         * Appends the specified double value to this buffer.
         * This method delegates to {@link #add(double)}.
         *
         * @param t the double value to append
         */
        @Override
        public void accept(final double t) {
            this.add(t);
        }

        /**
         * Adds the specified double value to this buffer.
         * The buffer automatically grows as needed using the spine-and-chunk structure.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();
         * buffer.add(10.5);
         * buffer.add(20.5);
         * buffer.add(30.5);
         *
         * // Use with DoubleStream operations
         * DoubleStream.of(1.1, 2.2, 3.3).forEach(buffer::accept);
         * System.out.println(buffer.size());   // prints 6
         * }</pre>
         *
         * @param e the double value to be added
         * @return {@code true} always
         */
        @SuppressWarnings("SameReturnValue")
        public boolean add(final double e) {
            if (size < initialCapacity) {
                if (curChunk.length == 0) {
                    curChunk = new double[CHUNK_SIZE];
                }

                curChunk[size] = e;
            } else {
                if (spine == null) {
                    spine = new double[SPINE_SIZE_TO_INCREASE][];
                    spine[0] = curChunk;
                    spine[1] = new double[CHUNK_SIZE];
                    curChunk = spine[1];
                } else {
                    final int chunkIndex = (size - spine[0].length) / CHUNK_SIZE + 1;

                    if (spine.length <= chunkIndex) {
                        spine = N.copyOf(spine, spine.length + SPINE_SIZE_TO_INCREASE);
                    }

                    if (spine[chunkIndex] == null) {
                        curChunk = new double[CHUNK_SIZE];
                        spine[chunkIndex] = curChunk;
                    }
                }

                curChunk[(size - spine[0].length) % CHUNK_SIZE] = e;
            }

            size++;
            return true;
        }

        /**
         * Returns an iterator over the double values in this buffer in proper sequence.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();
         * buffer.add(1.1);
         * buffer.add(2.2);
         * buffer.add(3.3);
         *
         * // Iterate over double values
         * DoubleIterator iter = buffer.iterator();
         * while (iter.hasNext()) {
         *     System.out.println(iter.nextDouble());
         * }
         * }</pre>
         *
         * @return a DoubleIterator over the double values in this buffer
         */
        public DoubleIterator iterator() {
            if (N.isEmpty(spine)) {
                return DoubleIterator.of(curChunk, 0, size);
            } else {
                final int localSize = size();

                return new DoubleIterator() {
                    private final double[] firstChunk = spine[0];
                    private final int firstChunkLen = firstChunk.length;
                    private int cursor = 0;
                    private double next = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor < localSize;
                    }

                    @Override
                    public double nextDouble() {
                        if (cursor >= localSize) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        if (cursor < firstChunkLen) {
                            return firstChunk[cursor++];
                        } else {
                            next = spine[(cursor - firstChunkLen) / CHUNK_SIZE + 1][(cursor - firstChunkLen) % CHUNK_SIZE];

                            cursor++;

                            return next;
                        }
                    }
                };
            }
        }

        /**
         * Returns the number of double values in this buffer.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();
         * System.out.println(buffer.size());   // prints 0
         *
         * buffer.add(1.5);
         * buffer.add(2.5);
         * System.out.println(buffer.size());   // prints 2
         * }</pre>
         *
         * @return the number of double values in this buffer
         */
        public int size() {
            return size;
        }
    }

}
