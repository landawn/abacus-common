/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
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

package com.landawn.abacus.util.stream;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
@Internal
public abstract class ObjIteratorEx<T> extends ObjIterator<T> implements IteratorEx<T> {
    @SuppressWarnings("rawtypes")
    public static final ObjIteratorEx EMPTY = new QueuedIterator(0) {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void skip(long n) {
            // Do nothing.
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void close() {
            // Do nothing.
        }
    };

    public static <T> ObjIteratorEx<T> empty() {
        return EMPTY;
    }

    @SafeVarargs
    public static <T> ObjIteratorEx<T> of(final T... a) {
        return a == null ? EMPTY : of(a, 0, a.length);
    }

    public static <T> ObjIteratorEx<T> of(final T[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new QueuedIterator<T>(toIndex - fromIndex) {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public <A> A[] toArray(A[] output) {
                if (output.length < toIndex - cursor) {
                    output = N.copyOf(output, toIndex - cursor);
                }

                N.copy(a, cursor, output, 0, toIndex - cursor);

                return output;
            }

            @Override
            public List<T> toList() {
                return N.asList((T[]) toArray());
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static <T> ObjIteratorEx<T> of(final Collection<T> c) {
        if (c == null) {
            return empty();
        }

        final Iterator<? extends T> iter = c.iterator();

        return new QueuedIterator<T>(c.size()) {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static <T> ObjIteratorEx<T> of(final Iterator<T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx) {
            return ((ObjIteratorEx<T>) iter);
        }

        return new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static <T> ObjIteratorEx<T> of(final Iterable<T> iterable) {
        return iterable == null ? ObjIteratorEx.<T> empty() : of(iterable.iterator());
    }

    /**
     * Lazy evaluation.
     * 
     * @param iteratorSupplier
     * @return
     */
    public static <T> ObjIteratorEx<T> of(final Supplier<? extends Iterator<? extends T>> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new ObjIteratorEx<T>() {
            private Iterator<? extends T> iter = null;
            private ObjIteratorEx<? extends T> iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (isInitialized == false) {
                    init();
                }

                return iter.next();
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                if (isInitialized == false) {
                    init();
                }

                if (iterEx != null) {
                    iterEx.skip(n);
                } else {
                    super.skip(n);
                }
            }

            @Override
            public long count() {
                if (isInitialized == false) {
                    init();
                }

                if (iterEx != null) {
                    return iterEx.count();
                } else {
                    return super.count();
                }
            }

            @Override
            public void close() {
                if (isInitialized == false) {
                    init();
                }

                if (iterEx != null) {
                    iterEx.close();
                }
            }

            private void init() {
                if (isInitialized == false) {
                    isInitialized = true;
                    iter = iteratorSupplier.get();
                    iterEx = iter instanceof ObjIteratorEx ? (ObjIteratorEx<T>) iter : null;
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
    public static <T> ObjIteratorEx<T> oF(final Supplier<T[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new ObjIteratorEx<T>() {
            private ObjIteratorEx<T> iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.hasNext();
            }

            @Override
            public T next() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.next();
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                if (isInitialized == false) {
                    init();
                }

                iterEx.skip(n);
            }

            @Override
            public long count() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.count();
            }

            private void init() {
                if (isInitialized == false) {
                    isInitialized = true;
                    iterEx = ObjIteratorEx.of(arraySupplier.get());
                }
            }
        };
    }

    @Override
    public void skip(long n) {
        N.checkArgNotNegative(n, "n");

        while (n > 0 && hasNext()) {
            next();
            n--;
        }
    }

    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            next();
            result++;
        }

        return result;
    }

    @Override
    public void close() {
        // Do nothing.
    }

    static abstract class QueuedIterator<T> extends ObjIteratorEx<T> {
        private final int max;

        QueuedIterator(int max) {
            this.max = max;
        }

        public int max() {
            return max;
        }
    }
}
