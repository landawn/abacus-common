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
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatSupplier;
import com.landawn.abacus.util.stream.FloatStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings({ "java:S6548" })
public abstract class FloatIterator extends ImmutableIterator<Float> {

    public static final FloatIterator EMPTY = new FloatIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public float nextFloat() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     *
     *
     * @return
     */
    public static FloatIterator empty() { //NOSONAR
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static FloatIterator of(final float... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static FloatIterator of(final float[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new FloatIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public float[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public FloatList toList() {
                return FloatList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    /**
     * Lazy evaluation.
     *
     * @param iteratorSupplier
     * @return
     */
    public static FloatIterator defer(final Supplier<? extends FloatIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new FloatIterator() {
            private FloatIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextFloat();
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
     * Returns an infinite {@code FloatIterator}.
     *
     * @param supplier
     * @return
     */
    public static FloatIterator generate(final FloatSupplier supplier) {
        N.checkArgNotNull(supplier);

        return new FloatIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public float nextFloat() {
                return supplier.getAsFloat();
            }
        };
    }

    /**
     *
     * @param hasNext
     * @param supplier
     * @return
     */
    public static FloatIterator generate(final BooleanSupplier hasNext, final FloatSupplier supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new FloatIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public float nextFloat() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsFloat();
            }
        };
    }

    /**
     *
     * @return
     * @deprecated use <code>nextFloat()</code> instead.
     */
    @Deprecated
    @Override
    public Float next() {
        return nextFloat();
    }

    /**
     *
     *
     * @return
     */
    public abstract float nextFloat();

    /**
     *
     *
     * @param n
     * @return
     */
    public FloatIterator skip(final long n) {
        N.checkArgNotNegative(n, "n");

        if (n <= 0) {
            return this;
        }

        final FloatIterator iter = this;

        return new FloatIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextFloat();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextFloat();
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
    public FloatIterator limit(final long count) {
        N.checkArgNotNegative(count, "count");

        if (count == 0) {
            return FloatIterator.EMPTY;
        }

        final FloatIterator iter = this;

        return new FloatIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextFloat();
            }
        };
    }

    /**
     *
     *
     * @param predicate
     * @return
     */
    public FloatIterator filter(final FloatPredicate predicate) {
        N.checkArgNotNull(predicate, "predicate");

        final FloatIterator iter = this;

        return new FloatIterator() {
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextFloat();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public float nextFloat() {
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
    public OptionalFloat first() {
        if (hasNext()) {
            return OptionalFloat.of(nextFloat());
        } else {
            return OptionalFloat.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public OptionalFloat last() {
        if (hasNext()) {
            float next = nextFloat();

            while (hasNext()) {
                next = nextFloat();
            }

            return OptionalFloat.of(next);
        } else {
            return OptionalFloat.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public float[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     *
     *
     * @return
     */
    public FloatList toList() {
        final FloatList list = new FloatList();

        while (hasNext()) {
            list.add(nextFloat());
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public FloatStream stream() {
        return FloatStream.of(this);
    }

    /**
     *
     *
     * @return
     */
    @Beta
    public ObjIterator<IndexedFloat> indexed() {
        return indexed(0);
    }

    /**
     *
     *
     * @param startIndex
     * @return
     */
    @Beta
    public ObjIterator<IndexedFloat> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final FloatIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedFloat next() {
                return IndexedFloat.of(iter.nextFloat(), idx++);
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
    public void forEachRemaining(java.util.function.Consumer<? super Float> action) {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(Throwables.FloatConsumer<E> action) throws E { //NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextFloat());
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachIndexed(Throwables.IndexedFloatConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextFloat());
        }
    }
}
