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
@SuppressWarnings({ "java:S6548" })
public abstract class IntIterator extends ImmutableIterator<Integer> {

    public static final IntIterator EMPTY = new IntIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public int nextInt() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     *
     *
     * @return
     */
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
    public static IntIterator of(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
     * @param iteratorSupplier 
     * @return 
     * @throws IllegalArgumentException 
     */
    public static IntIterator defer(final Supplier<? extends IntIterator> iteratorSupplier) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException 
     */
    public static IntIterator generate(final IntSupplier supplier) throws IllegalArgumentException {
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
     *
     * @param hasNext 
     * @param supplier 
     * @return 
     * @throws IllegalArgumentException 
     */
    public static IntIterator generate(final BooleanSupplier hasNext, final IntSupplier supplier) throws IllegalArgumentException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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

    /**
     *
     *
     * @return
     */
    public abstract int nextInt();

    /**
     * 
     *
     * @param n 
     * @return 
     * @throws IllegalArgumentException 
     */
    public IntIterator skip(final long n) throws IllegalArgumentException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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

    /**
     * 
     *
     * @param count 
     * @return 
     * @throws IllegalArgumentException 
     */
    public IntIterator limit(final long count) throws IllegalArgumentException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextInt();
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
    public IntIterator filter(final IntPredicate predicate) throws IllegalArgumentException {
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
    public OptionalInt first() {
        if (hasNext()) {
            return OptionalInt.of(nextInt());
        } else {
            return OptionalInt.empty();
        }
    }

    /**
     *
     *
     * @return
     */
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

    /**
     *
     *
     * @return
     */
    public int[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     *
     *
     * @return
     */
    public IntList toList() {
        final IntList list = new IntList();

        while (hasNext()) {
            list.add(nextInt());
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public IntStream stream() {
        return IntStream.of(this);
    }

    /**
     *
     *
     * @return
     */
    @Beta
    public ObjIterator<IndexedInt> indexed() {
        return indexed(0);
    }

    /**
     *
     *
     * @param startIndex
     * @return
     */
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
     * @throws IllegalArgumentException 
     * @deprecated 
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Integer> action) throws IllegalArgumentException {
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
     *
     * @param <E> 
     * @param action 
     * @throws IllegalArgumentException 
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(Throwables.IntIntConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextInt());
        }
    }
}
