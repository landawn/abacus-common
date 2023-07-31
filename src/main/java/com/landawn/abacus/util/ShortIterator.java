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
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortSupplier;
import com.landawn.abacus.util.stream.ShortStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings({ "java:S6548" })
public abstract class ShortIterator extends ImmutableIterator<Short> {

    public static final ShortIterator EMPTY = new ShortIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public short nextShort() {
            throw new NoSuchElementException();
        }
    };

    /**
     *
     *
     * @return
     */
    public static ShortIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static ShortIterator of(final short... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static ShortIterator of(final short[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ShortIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }

            @Override
            public short[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public ShortList toList() {
                return ShortList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    //    /**
    //     * Lazy evaluation.
    //     *
    //     * @param arraySupplier
    //     * @return
    //     */
    //    public static ShortIterator from(final Supplier<short[]> arraySupplier) {
    //        N.checkArgNotNull(arraySupplier, "arraySupplier");
    //
    //        return new ShortIterator() {
    //            private short[] aar = null;
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
    //            public short nextShort() {
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
    public static ShortIterator defer(final Supplier<? extends ShortIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new ShortIterator() {
            private ShortIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextShort();
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
     * Returns an infinite {@code ShortIterator}.
     *
     * @param supplier
     * @return
     */
    public static ShortIterator generate(final ShortSupplier supplier) {
        N.checkArgNotNull(supplier);

        return new ShortIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public short nextShort() {
                return supplier.getAsShort();
            }
        };
    }

    /**
     *
     * @param hasNext
     * @param supplier
     * @return
     */
    public static ShortIterator generate(final BooleanSupplier hasNext, final ShortSupplier supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ShortIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return supplier.getAsShort();
            }
        };
    }

    /**
     *
     * @return
     * @deprecated use <code>nextShort()</code> instead.
     */
    @Deprecated
    @Override
    public Short next() {
        return nextShort();
    }

    /**
     *
     *
     * @return
     */
    public abstract short nextShort();

    /**
     *
     *
     * @param n
     * @return
     */
    public ShortIterator skip(final long n) {
        N.checkArgNotNegative(n, "n");

        if (n <= 0) {
            return this;
        }

        final ShortIterator iter = this;

        return new ShortIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return iter.nextShort();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextShort();
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
     */
    public ShortIterator limit(final long count) {
        N.checkArgNotNegative(count, "count");

        if (count == 0) {
            return ShortIterator.EMPTY;
        }

        final ShortIterator iter = this;

        return new ShortIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                cnt--;
                return iter.nextShort();
            }
        };
    }

    /**
     *
     *
     * @param predicate
     * @return
     */
    public ShortIterator filter(final ShortPredicate predicate) {
        N.checkArgNotNull(predicate, "predicate");

        final ShortIterator iter = this;

        return new ShortIterator() {
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextShort();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public short nextShort() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException();
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
    public OptionalShort first() {
        if (hasNext()) {
            return OptionalShort.of(nextShort());
        } else {
            return OptionalShort.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public OptionalShort last() {
        if (hasNext()) {
            short next = nextShort();

            while (hasNext()) {
                next = nextShort();
            }

            return OptionalShort.of(next);
        } else {
            return OptionalShort.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public short[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     *
     *
     * @return
     */
    public ShortList toList() {
        final ShortList list = new ShortList();

        while (hasNext()) {
            list.add(nextShort());
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public ShortStream stream() {
        return ShortStream.of(this);
    }

    /**
     *
     *
     * @return
     */
    @Beta
    public ObjIterator<IndexedShort> indexed() {
        return indexed(0);
    }

    /**
     *
     *
     * @param startIndex
     * @return
     */
    @Beta
    public ObjIterator<IndexedShort> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final ShortIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedShort next() {
                return IndexedShort.of(iter.nextShort(), idx++);
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
    public void forEachRemaining(java.util.function.Consumer<? super Short> action) {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Throwables.ShortConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextShort());
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(Throwables.IndexedShortConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextShort());
        }
    }
}
