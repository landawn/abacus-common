/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.stream.Stream;

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
public abstract class BooleanIterator extends ImmutableIterator<Boolean> {

    public static final BooleanIterator EMPTY = new BooleanIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public boolean nextBoolean() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    @SuppressWarnings("SameReturnValue")
    public static BooleanIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    public static BooleanIterator of(final boolean... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static BooleanIterator of(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (a == null || fromIndex == toIndex) {
            return EMPTY;
        }

        return new BooleanIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public boolean nextBoolean() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public boolean[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public BooleanList toList() {
                return BooleanList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    /**
     * Returns an BooleanIterator instance that is created lazily using the provided Supplier.
     * The Supplier is responsible for producing the BooleanIterator instance when the first method in the returned {@code BooleanIterator} is called.
     *
     * @param iteratorSupplier A Supplier that provides the BooleanIterator when needed.
     * @return A BooleanIterator that is initialized on the first call to hasNext() or nextByte().
     * @throws IllegalArgumentException if iteratorSupplier is {@code null}.
     */
    public static BooleanIterator defer(final Supplier<? extends BooleanIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new BooleanIterator() {
            private BooleanIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextBoolean();
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
     * Returns an infinite {@code BooleanIterator}.
     *
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static BooleanIterator generate(final BooleanSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new BooleanIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public boolean nextBoolean() {
                return supplier.getAsBoolean();
            }
        };
    }

    /**
     *
     * @param hasNext
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static BooleanIterator generate(final BooleanSupplier hasNext, final BooleanSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new BooleanIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public boolean nextBoolean() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsBoolean();
            }
        };
    }

    /**
     *
     * @return
     * @throws NoSuchElementException if the iteration has no more elements
     * @deprecated use {@code nextBoolean()} instead.
     */
    @Deprecated
    @Override
    public Boolean next() {
        return nextBoolean();
    }

    public abstract boolean nextBoolean();

    /**
     *
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public BooleanIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final BooleanIterator iter = this;

        return new BooleanIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextBoolean();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextBoolean();
                }

                skipped = true;
            }
        };
    }

    /**
     *
     * @param count
     * @return
     * @throws IllegalArgumentException
     */
    public BooleanIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return BooleanIterator.EMPTY;
        }

        final BooleanIterator iter = this;

        return new BooleanIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextBoolean();
            }
        };
    }

    /**
     *
     * @param predicate
     * @return
     * @throws IllegalArgumentException
     */
    public BooleanIterator filter(final BooleanPredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final BooleanIterator iter = this;

        return new BooleanIterator() {
            private boolean hasNext = false;
            private boolean next = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextBoolean();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public boolean nextBoolean() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        };
    }

    public OptionalBoolean first() {
        if (hasNext()) {
            return OptionalBoolean.of(nextBoolean());
        } else {
            return OptionalBoolean.empty();
        }
    }

    public OptionalBoolean last() {
        if (hasNext()) {
            boolean next = nextBoolean();

            while (hasNext()) {
                next = nextBoolean();
            }

            return OptionalBoolean.of(next);
        } else {
            return OptionalBoolean.empty();
        }
    }

    public boolean[] toArray() {
        return toList().trimToSize().array();
    }

    public BooleanList toList() {
        final BooleanList list = new BooleanList();

        while (hasNext()) {
            list.add(nextBoolean());
        }

        return list;
    }

    public Stream<Boolean> stream() {
        return Stream.of(this);
    }

    @Beta
    public ObjIterator<IndexedBoolean> indexed() {
        return indexed(0);
    }

    /**
     *
     * @param startIndex
     * @return
     */
    @Beta
    public ObjIterator<IndexedBoolean> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final BooleanIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedBoolean next() {
                return IndexedBoolean.of(iter.nextBoolean(), idx++);
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
    public void forEachRemaining(final java.util.function.Consumer<? super Boolean> action) throws IllegalArgumentException {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(final Throwables.BooleanConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextBoolean());
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntBooleanConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextBoolean());
        }
    }
}
