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
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class IntIterator extends ImmutableIterator<Integer> {

    public static final IntIterator EMPTY = new IntIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public int nextInt() {
            throw new NoSuchElementException();
        }
    };

    public static IntIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static IntIterator of(final int... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static IntIterator of(final int[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new IntIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }

            @Override
            public int[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public IntList toList() {
                return IntList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    //    /**
    //     * Lazy evaluation.
    //     *
    //     * @param arraySupplier
    //     * @return
    //     */
    //    public static IntIterator from(final Supplier<int[]> arraySupplier) {
    //        N.checkArgNotNull(arraySupplier, "arraySupplier");
    //
    //        return new IntIterator() {
    //            private int[] aar = null;
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
    //            public int nextInt() {
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
    public static IntIterator defer(final Supplier<? extends IntIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new IntIterator() {
            private IntIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextInt();
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
     * Returns an infinite {@code IntIterator}.
     *
     * @param supplier
     * @return
     */
    public static IntIterator generate(final IntSupplier supplier) {
        N.checkArgNotNull(supplier);

        return new IntIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                return supplier.getAsInt();
            }
        };
    }

    /**
     *
     * @param hasNext
     * @param supplier
     * @return
     */
    public static IntIterator generate(final BooleanSupplier hasNext, final IntSupplier supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new IntIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return supplier.getAsInt();
            }
        };
    }

    /**
     *
     * @return
     * @deprecated use <code>nextInt()</code> instead.
     */
    @Deprecated
    @Override
    public Integer next() {
        return nextInt();
    }

    public abstract int nextInt();

    public IntIterator skip(final long n) {
        N.checkArgNotNegative(n, "n");

        if (n <= 0) {
            return this;
        }

        final IntIterator iter = this;

        return new IntIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return iter.nextInt();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextInt();
                }

                skipped = true;
            }
        };
    }

    public IntIterator limit(final long count) {
        N.checkArgNotNegative(count, "count");

        if (count == 0) {
            return IntIterator.EMPTY;
        }

        final IntIterator iter = this;

        return new IntIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                cnt--;
                return iter.nextInt();
            }
        };
    }

    public IntIterator filter(final IntPredicate predicate) {
        N.checkArgNotNull(predicate, "predicate");

        final IntIterator iter = this;

        return new IntIterator() {
            private boolean hasNext = false;
            private int next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextInt();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public int nextInt() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }
        };
    }

    public OptionalInt first() {
        if (hasNext()) {
            return OptionalInt.of(nextInt());
        } else {
            return OptionalInt.empty();
        }
    }

    public OptionalInt last() {
        if (hasNext()) {
            int next = nextInt();

            while (hasNext()) {
                next = nextInt();
            }

            return OptionalInt.of(next);
        } else {
            return OptionalInt.empty();
        }
    }

    public int[] toArray() {
        return toList().trimToSize().array();
    }

    public IntList toList() {
        final IntList list = new IntList();

        while (hasNext()) {
            list.add(nextInt());
        }

        return list;
    }

    public IntStream stream() {
        return IntStream.of(this);
    }

    @Beta
    public ObjIterator<IndexedInt> indexed() {
        return indexed(0);
    }

    @Beta
    public ObjIterator<IndexedInt> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final IntIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedInt next() {
                return IndexedInt.of(iter.nextInt(), idx++);
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
    public void forEachRemaining(java.util.function.Consumer<? super Integer> action) {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Throwables.IntConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextInt());
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(Throwables.IndexedIntConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextInt());
        }
    }
}
