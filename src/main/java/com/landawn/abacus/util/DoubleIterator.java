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
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
    public static DoubleIterator of(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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

    /**
     * Lazy evaluation.
     *
     * @param iteratorSupplier
     * @return
     * @throws IllegalArgumentException
     */
    public static DoubleIterator defer(final Supplier<? extends DoubleIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

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
     * @throws IllegalArgumentException
     */
    public static DoubleIterator generate(final DoubleSupplier supplier) throws IllegalArgumentException {
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
     *
     * @param hasNext
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static DoubleIterator generate(final BooleanSupplier hasNext, final DoubleSupplier supplier) throws IllegalArgumentException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
     * @throws IllegalArgumentException
     */
    public DoubleIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
     * @throws IllegalArgumentException
     */
    public DoubleIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
     * @throws IllegalArgumentException
     */
    public DoubleIterator filter(final DoublePredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

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
     * @throws IllegalArgumentException
     * @deprecated
     */
    @Override
    @Deprecated
    public void forEachRemaining(final java.util.function.Consumer<? super Double> action) throws IllegalArgumentException {
        super.forEachRemaining(action);
    }

    /**
     *
     *
     * @param <E>
     * @param action
     * @throws E
     */
    public <E extends Exception> void foreachRemaining(final Throwables.DoubleConsumer<E> action) throws E {//NOSONAR
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
     * @throws IllegalArgumentException
     * @throws E
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntDoubleConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextDouble());
        }
    }
}
