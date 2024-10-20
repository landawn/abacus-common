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
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

/**
 *
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 *
 */
@SuppressWarnings({ "java:S6548" })
public abstract class LongIterator extends ImmutableIterator<Long> {

    public static final LongIterator EMPTY = new LongIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public long nextLong() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     *
     *
     * @return
     */
    public static LongIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static LongIterator of(final long... a) {
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
    public static LongIterator of(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new LongIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public long[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public LongList toList() {
                return LongList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    /**
     * Returns an LongIterator instance that is created lazily using the provided Supplier.
     * The Supplier is responsible for producing the LongIterator instance when the LongIterator's methods are first called.
     *
     * @param iteratorSupplier A Supplier that provides the LongIterator when needed.
     * @return A LongIterator that is initialized on the first call to hasNext() or nextByte().
     * @throws IllegalArgumentException if iteratorSupplier is {@code null}.
     */
    public static LongIterator defer(final Supplier<? extends LongIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new LongIterator() {
            private LongIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextLong();
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
     * Returns an infinite {@code LongIterator}.
     *
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static LongIterator generate(final LongSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new LongIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                return supplier.getAsLong();
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
    public static LongIterator generate(final BooleanSupplier hasNext, final LongSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new LongIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsLong();
            }
        };
    }

    /**
     *
     * @return
     * @deprecated use {@code nextLong()} instead.
     */
    @Deprecated
    @Override
    public Long next() {
        return nextLong();
    }

    /**
     *
     *
     * @return
     */
    public abstract long nextLong();

    /**
     *
     *
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public LongIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final LongIterator iter = this;

        return new LongIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextLong();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextLong();
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
    public LongIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return LongIterator.EMPTY;
        }

        final LongIterator iter = this;

        return new LongIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextLong();
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
    public LongIterator filter(final LongPredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final LongIterator iter = this;

        return new LongIterator() {
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextLong();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public long nextLong() {
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
    public OptionalLong first() {
        if (hasNext()) {
            return OptionalLong.of(nextLong());
        } else {
            return OptionalLong.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public OptionalLong last() {
        if (hasNext()) {
            long next = nextLong();

            while (hasNext()) {
                next = nextLong();
            }

            return OptionalLong.of(next);
        } else {
            return OptionalLong.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public long[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     *
     *
     * @return
     */
    public LongList toList() {
        final LongList list = new LongList();

        while (hasNext()) {
            list.add(nextLong());
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public LongStream stream() {
        return LongStream.of(this);
    }

    /**
     *
     *
     * @return
     */
    @Beta
    public ObjIterator<IndexedLong> indexed() {
        return indexed(0);
    }

    /**
     *
     *
     * @param startIndex
     * @return
     */
    @Beta
    public ObjIterator<IndexedLong> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final LongIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedLong next() {
                return IndexedLong.of(iter.nextLong(), idx++);
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
    public void forEachRemaining(final java.util.function.Consumer<? super Long> action) throws IllegalArgumentException {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(final Throwables.LongConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextLong());
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
    public <E extends Exception> void foreachIndexed(final Throwables.IntLongConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextLong());
        }
    }
}
