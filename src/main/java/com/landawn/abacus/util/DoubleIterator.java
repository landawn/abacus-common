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
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings({ "java:S6548" })
public abstract class DoubleIterator extends ImmutableIterator<Double> {

    public static final DoubleIterator EMPTY = new DoubleIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public double nextDouble() {
            throw new NoSuchElementException();
        }
    };

    /**
     *
     *
     * @return
     */
    public static DoubleIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static DoubleIterator of(final double... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static DoubleIterator of(final double[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new DoubleIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }

            @Override
            public double[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public DoubleList toList() {
                return DoubleList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    //    /**
    //     * Lazy evaluation.
    //     *
    //     * @param arraySupplier
    //     * @return
    //     */
    //    public static DoubleIterator from(final Supplier<double[]> arraySupplier) {
    //        N.checkArgNotNull(arraySupplier, "arraySupplier");
    //
    //        return new DoubleIterator() {
    //            private double[] aar = null;
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
    //            public double nextDouble() {
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
    public static DoubleIterator defer(final Supplier<? extends DoubleIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new DoubleIterator() {
            private DoubleIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextDouble();
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
     * Returns an infinite {@code DoubleIterator}.
     *
     * @param supplier
     * @return
     */
    public static DoubleIterator generate(final DoubleSupplier supplier) {
        N.checkArgNotNull(supplier);

        return new DoubleIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double nextDouble() {
                return supplier.getAsDouble();
            }
        };
    }

    /**
     *
     * @param hasNext
     * @param supplier
     * @return
     */
    public static DoubleIterator generate(final BooleanSupplier hasNext, final DoubleSupplier supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new DoubleIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public double nextDouble() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return supplier.getAsDouble();
            }
        };
    }

    /**
     *
     * @return
     * @deprecated use <code>nextDouble()</code> instead.
     */
    @Deprecated
    @Override
    public Double next() {
        return nextDouble();
    }

    /**
     *
     *
     * @return
     */
    public abstract double nextDouble();

    /**
     *
     *
     * @param n
     * @return
     */
    public DoubleIterator skip(final long n) {
        N.checkArgNotNegative(n, "n");

        if (n <= 0) {
            return this;
        }

        final DoubleIterator iter = this;

        return new DoubleIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return iter.nextDouble();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextDouble();
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
    public DoubleIterator limit(final long count) {
        N.checkArgNotNegative(count, "count");

        if (count == 0) {
            return DoubleIterator.EMPTY;
        }

        final DoubleIterator iter = this;

        return new DoubleIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                cnt--;
                return iter.nextDouble();
            }
        };
    }

    /**
     *
     *
     * @param predicate
     * @return
     */
    public DoubleIterator filter(final DoublePredicate predicate) {
        N.checkArgNotNull(predicate, "predicate");

        final DoubleIterator iter = this;

        return new DoubleIterator() {
            private boolean hasNext = false;
            private double next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextDouble();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public double nextDouble() {
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
    public OptionalDouble first() {
        if (hasNext()) {
            return OptionalDouble.of(nextDouble());
        } else {
            return OptionalDouble.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public OptionalDouble last() {
        if (hasNext()) {
            double next = nextDouble();

            while (hasNext()) {
                next = nextDouble();
            }

            return OptionalDouble.of(next);
        } else {
            return OptionalDouble.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public double[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     *
     *
     * @return
     */
    public DoubleList toList() {
        final DoubleList list = new DoubleList();

        while (hasNext()) {
            list.add(nextDouble());
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public DoubleStream stream() {
        return DoubleStream.of(this);
    }

    /**
     *
     *
     * @return
     */
    @Beta
    public ObjIterator<IndexedDouble> indexed() {
        return indexed(0);
    }

    /**
     *
     *
     * @param startIndex
     * @return
     */
    @Beta
    public ObjIterator<IndexedDouble> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final DoubleIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedDouble next() {
                return IndexedDouble.of(iter.nextDouble(), idx++);
            }
        };
    }

    /**
     *
     *
     * @param action
     * @deprecated
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Double> action) {
        super.forEachRemaining(action);
    }

    /**
     *
     *
     * @param <E>
     * @param action
     * @throws E
     */
    public <E extends Exception> void foreachRemaining(Throwables.DoubleConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextDouble());
        }
    }

    /**
     *
     *
     * @param <E>
     * @param action
     * @throws E
     */
    public <E extends Exception> void foreachIndexed(Throwables.IndexedDoubleConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextDouble());
        }
    }
}
