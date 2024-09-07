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
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteSupplier;
import com.landawn.abacus.util.stream.ByteStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings({ "java:S6548" })
public abstract class ByteIterator extends ImmutableIterator<Byte> {

    public static final ByteIterator EMPTY = new ByteIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public byte nextByte() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     *
     *
     * @return
     */
    public static ByteIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static ByteIterator of(final byte... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * 
     *
     * @param a 
     * @param fromIndex 
     * @param toIndex 
     * @return 
     * @throws IndexOutOfBoundsException 
     */
    public static ByteIterator of(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
     * @param iteratorSupplier 
     * @return 
     * @throws IllegalArgumentException 
     */
    public static ByteIterator defer(final Supplier<? extends ByteIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

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
     * @throws IllegalArgumentException 
     */
    public static ByteIterator generate(final ByteSupplier supplier) throws IllegalArgumentException {
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
     *
     * @param hasNext 
     * @param supplier 
     * @return 
     * @throws IllegalArgumentException 
     */
    public static ByteIterator generate(final BooleanSupplier hasNext, final ByteSupplier supplier) throws IllegalArgumentException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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

    /**
     *
     *
     * @return
     */
    public abstract byte nextByte();

    /**
     * 
     *
     * @param n 
     * @return 
     * @throws IllegalArgumentException 
     */
    public ByteIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final ByteIterator iter = this;

        return new ByteIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextByte();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextByte();
                }

                skipped = true;
            }
        };
    }

    /**
     * 
     *
     * @param count 
     * @return 
     * @throws IllegalArgumentException 
     */
    public ByteIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return ByteIterator.EMPTY;
        }

        final ByteIterator iter = this;

        return new ByteIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextByte();
            }
        };
    }

    /**
     * 
     *
     * @param predicate 
     * @return 
     * @throws IllegalArgumentException 
     */
    public ByteIterator filter(final BytePredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final ByteIterator iter = this;

        return new ByteIterator() {
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextByte();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public byte nextByte() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        };
    }

    /**
     *
     *
     * @return
     */
    public OptionalByte first() {
        if (hasNext()) {
            return OptionalByte.of(nextByte());
        } else {
            return OptionalByte.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public OptionalByte last() {
        if (hasNext()) {
            byte next = nextByte();

            while (hasNext()) {
                next = nextByte();
            }

            return OptionalByte.of(next);
        } else {
            return OptionalByte.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public byte[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     *
     *
     * @return
     */
    public ByteList toList() {
        final ByteList list = new ByteList();

        while (hasNext()) {
            list.add(nextByte());
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public ByteStream stream() {
        return ByteStream.of(this);
    }

    /**
     *
     *
     * @return
     */
    @Beta
    public ObjIterator<IndexedByte> indexed() {
        return indexed(0);
    }

    /**
     *
     *
     * @param startIndex
     * @return
     */
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
     * @throws IllegalArgumentException 
     * @deprecated 
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Byte> action) throws IllegalArgumentException {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Throwables.ByteConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextByte());
        }
    }

    /**
     * 
     *
     * @param <E> 
     * @param action 
     * @throws IllegalArgumentException 
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(Throwables.IntByteConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextByte());
        }
    }
}
