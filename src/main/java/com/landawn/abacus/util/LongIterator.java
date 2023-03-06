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
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class LongIterator extends ImmutableIterator<Long> {

    public static final LongIterator EMPTY = new LongIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public long nextLong() {
            throw new NoSuchElementException();
        }
    };

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
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static LongIterator of(final long[] a, final int fromIndex, final int toIndex) {
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
                    throw new NoSuchElementException();
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

    //    /**
    //     * Lazy evaluation.
    //     *
    //     * @param arraySupplier
    //     * @return
    //     */
    //    public static LongIterator from(final Supplier<long[]> arraySupplier) {
    //        N.checkArgNotNull(arraySupplier, "arraySupplier");
    //
    //        return new LongIterator() {
    //            private long[] aar = null;
    //            private int len = 0;
    //            private int cur = 0;
    //            private boolean isInitialized = false;
    //
    //            @Override
    //            public boolean hasNext() {
    //                if (!isInitialized) {
    //                    init();
    //                }
    //
    //                return cur < len;
    //            }
    //
    //            @Override
    //            public long nextLong() {
    //                if (!isInitialized) {
    //                    init();
    //                }
    //
    //                if (cur >= len) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                return aar[cur++];
    //            }
    //
    //            private void init() {
    //                if (!isInitialized) {
    //                    isInitialized = true;
    //                    aar = arraySupplier.get();
    //                    len = N.len(aar);
    //                }
    //            }
    //        };
    //    }

    /**
     * Lazy evaluation.
     *
     * @param iteratorSupplier
     * @return
     */
    public static LongIterator defer(final Supplier<? extends LongIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

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
     */
    public static LongIterator generate(final LongSupplier supplier) {
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
     * @param hasNext
     * @param supplier
     * @return
     */
    public static LongIterator generate(final BooleanSupplier hasNext, final LongSupplier supplier) {
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
                    throw new NoSuchElementException();
                }

                return supplier.getAsLong();
            }
        };
    }

    /**
     *
     * @return
     * @deprecated use <code>nextLong()</code> instead.
     */
    @Deprecated
    @Override
    public Long next() {
        return nextLong();
    }

    public abstract long nextLong();

    public LongIterator skip(final long n) {
        N.checkArgNotNegative(n, "n");

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
                    throw new NoSuchElementException();
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

    public LongIterator limit(final long count) {
        N.checkArgNotNegative(count, "count");

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
                    throw new NoSuchElementException();
                }

                cnt--;
                return iter.nextLong();
            }
        };
    }

    public LongIterator filter(final LongPredicate predicate) {
        N.checkArgNotNull(predicate, "predicate");

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
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }
        };
    }

    public OptionalLong first() {
        if (hasNext()) {
            return OptionalLong.of(nextLong());
        } else {
            return OptionalLong.empty();
        }
    }

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

    public long[] toArray() {
        return toList().trimToSize().array();
    }

    public LongList toList() {
        final LongList list = new LongList();

        while (hasNext()) {
            list.add(nextLong());
        }

        return list;
    }

    public LongStream stream() {
        return LongStream.of(this);
    }

    @Beta
    public ObjIterator<IndexedLong> indexed() {
        return indexed(0);
    }

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
     * @deprecated
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Long> action) {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Throwables.LongConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextLong());
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(Throwables.IndexedLongConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextLong());
        }
    }
}
