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

import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.DoubleSupplier;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.DoubleStream;

// TODO: Auto-generated Javadoc
/**
 * The Class DoubleIterator.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class DoubleIterator extends ImmutableIterator<Double> {

    /** The Constant EMPTY. */
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
     * Empty.
     *
     * @return
     */
    public static DoubleIterator empty() {
        return EMPTY;
    }

    /**
     * Of.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static DoubleIterator of(final double... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Of.
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

    /**
     * Lazy evaluation.
     *
     * @param iteratorSupplier
     * @return
     */
    public static DoubleIterator of(final Supplier<? extends DoubleIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new DoubleIterator() {
            private DoubleIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (isInitialized == false) {
                    init();
                }

                return iter.nextDouble();
            }

            private void init() {
                if (isInitialized == false) {
                    isInitialized = true;
                    iter = iteratorSupplier.get();
                }
            }
        };
    }

    /**
     * Lazy evaluation.
     *
     * @param arraySupplier
     * @return
     */
    public static DoubleIterator oF(final Supplier<double[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new DoubleIterator() {
            private double[] aar = null;
            private int len = 0;
            private int cur = 0;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return cur < len;
            }

            @Override
            public double nextDouble() {
                if (isInitialized == false) {
                    init();
                }

                if (cur >= len) {
                    throw new NoSuchElementException();
                }

                return aar[cur++];
            }

            private void init() {
                if (isInitialized == false) {
                    isInitialized = true;
                    aar = arraySupplier.get();
                    len = N.len(aar);
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
     * Generate.
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return supplier.getAsDouble();
            }
        };
    }

    /**
     * Next.
     *
     * @return
     * @Deprecated use <code>nextDouble()</code> instead.
     */
    @Deprecated
    @Override
    public Double next() {
        return nextDouble();
    }

    /**
     * Next double.
     *
     * @return
     */
    public abstract double nextDouble();

    /**
     * To array.
     *
     * @return
     */
    public double[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * To list.
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
     * Stream.
     *
     * @return
     */
    public DoubleStream stream() {
        return DoubleStream.of(this);
    }

    /**
     * Foreach remaining.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Try.DoubleConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextDouble());
        }
    }

    /**
     * For each remaining.
     *
     * @param action
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Double> action) {
        super.forEachRemaining(action);
    }
}
