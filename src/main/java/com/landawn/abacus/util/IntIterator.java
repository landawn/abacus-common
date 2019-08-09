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
import com.landawn.abacus.util.function.IntSupplier;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.IntStream;

// TODO: Auto-generated Javadoc
/**
 * The Class IntIterator.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class IntIterator extends ImmutableIterator<Integer> {

    /** The Constant EMPTY. */
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

    /**
     * Empty.
     *
     * @return the int iterator
     */
    public static IntIterator empty() {
        return EMPTY;
    }

    /**
     * Of.
     *
     * @param a the a
     * @return the int iterator
     */
    @SafeVarargs
    public static IntIterator of(final int... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Of.
     *
     * @param a the a
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the int iterator
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

    /**
     * Lazy evaluation.
     *
     * @param iteratorSupplier the iterator supplier
     * @return the int iterator
     */
    public static IntIterator of(final Supplier<? extends IntIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new IntIterator() {
            private IntIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (isInitialized == false) {
                    init();
                }

                return iter.nextInt();
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
     * @param arraySupplier the array supplier
     * @return the int iterator
     */
    public static IntIterator oF(final Supplier<int[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new IntIterator() {
            private int[] aar = null;
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
            public int nextInt() {
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
     * Returns an infinite {@code IntIterator}.
     *
     * @param supplier the supplier
     * @return the int iterator
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
     * Generate.
     *
     * @param hasNext the has next
     * @param supplier the supplier
     * @return the int iterator
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return supplier.getAsInt();
            }
        };
    }

    /**
     * Next.
     *
     * @return the integer
     * @Deprecated use <code>nextInt()</code> instead.
     */
    @Deprecated
    @Override
    public Integer next() {
        return nextInt();
    }

    /**
     * Next int.
     *
     * @return the int
     */
    public abstract int nextInt();

    /**
     * To array.
     *
     * @return the int[]
     */
    public int[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * To list.
     *
     * @return the int list
     */
    public IntList toList() {
        final IntList list = new IntList();

        while (hasNext()) {
            list.add(nextInt());
        }

        return list;
    }

    /**
     * Stream.
     *
     * @return the int stream
     */
    public IntStream stream() {
        return IntStream.of(this);
    }

    /**
     * Foreach remaining.
     *
     * @param <E> the element type
     * @param action the action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Try.IntConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextInt());
        }
    }

    /**
     * For each remaining.
     *
     * @param action the action
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Integer> action) {
        super.forEachRemaining(action);
    }
}
