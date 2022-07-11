/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.landawn.abacus.util.function.ByteSupplier;
import com.landawn.abacus.util.stream.ByteStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class ByteIterator extends ImmutableIterator<Byte> {

    public static final ByteIterator EMPTY = new ByteIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public byte nextByte() {
            throw new NoSuchElementException();
        }
    };

    public static ByteIterator empty() {
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static ByteIterator of(final byte... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static ByteIterator of(final byte[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ByteIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }

            @Override
            public byte[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public ByteList toList() {
                return ByteList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    /**
     * Lazy evaluation.
     *
     * @param arraySupplier
     * @return
     */
    public static ByteIterator from(final Supplier<byte[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new ByteIterator() {
            private byte[] aar = null;
            private int len = 0;
            private int cur = 0;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return cur < len;
            }

            @Override
            public byte nextByte() {
                if (!isInitialized) {
                    init();
                }

                if (cur >= len) {
                    throw new NoSuchElementException();
                }

                return aar[cur++];
            }

            private void init() {
                if (!isInitialized) {
                    isInitialized = true;
                    aar = arraySupplier.get();
                    len = N.len(aar);
                }
            }
        };
    }

    /**
     * Lazy evaluation.
     *
     * @param iteratorSupplier
     * @return
     */
    public static ByteIterator defer(final Supplier<? extends ByteIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new ByteIterator() {
            private ByteIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextByte();
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
     * Returns an infinite {@code ByteIterator}.
     *
     * @param supplier
     * @return
     */
    public static ByteIterator generate(final ByteSupplier supplier) {
        N.checkArgNotNull(supplier);

        return new ByteIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public byte nextByte() {
                return supplier.getAsByte();
            }
        };
    }

    /**
     *
     * @param hasNext
     * @param supplier
     * @return
     */
    public static ByteIterator generate(final BooleanSupplier hasNext, final ByteSupplier supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ByteIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return supplier.getAsByte();
            }
        };
    }

    /**
     *
     * @return
     * @deprecated use <code>nextByte()</code> instead.
     */
    @Deprecated
    @Override
    public Byte next() {
        return nextByte();
    }

    public abstract byte nextByte();

    public byte[] toArray() {
        return toList().trimToSize().array();
    }

    public ByteList toList() {
        final ByteList list = new ByteList();

        while (hasNext()) {
            list.add(nextByte());
        }

        return list;
    }

    public ByteStream stream() {
        return ByteStream.of(this);
    }

    @Beta
    public ObjIterator<IndexedByte> indexed() {
        return indexed(0);
    }

    @Beta
    public ObjIterator<IndexedByte> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final ByteIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedByte next() {
                return IndexedByte.of(iter.nextByte(), idx++);
            }
        };
    }

    /**
     * For each remaining.
     *
     * @param action
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Byte> action) {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Throwables.ByteConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextByte());
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(Throwables.IndexedByteConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextByte());
        }
    }
}
