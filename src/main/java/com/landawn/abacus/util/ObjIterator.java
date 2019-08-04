/*
 * Copyright (c) 2017, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Stream;

/**
 * 
 * @since 0.9
 * 
 * @author Haiyang Li
 */
public abstract class ObjIterator<T> extends ImmutableIterator<T> {
    @SuppressWarnings("rawtypes")
    private static final ObjIterator EMPTY = new ObjIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    public static <T> ObjIterator<T> empty() {
        return EMPTY;
    }

    public static <T> ObjIterator<T> just(final T val) {
        return new ObjIterator<T>() {
            private boolean done = false;

            @Override
            public boolean hasNext() {
                return done == false;
            }

            @Override
            public T next() {
                if (done) {
                    throw new NoSuchElementException();
                }

                done = true;

                return val;
            }
        };
    }

    @SafeVarargs
    public static <T> ObjIterator<T> of(final T... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static <T> ObjIterator<T> of(final T[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ObjIterator<T>() {
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
        };
    }

    public static <T> ObjIterator<T> of(final Iterator<T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIterator) {
            return (ObjIterator<T>) iter;
        }

        return new ObjIterator<T>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }
        };
    }

    public static <T> ObjIterator<T> of(final Collection<T> iterable) {
        return iterable == null ? ObjIterator.<T> empty() : of(iterable.iterator());
    }

    public static <T> ObjIterator<T> of(final Iterable<T> iterable) {
        return iterable == null ? ObjIterator.<T> empty() : of(iterable.iterator());
    }

    /**
     * Lazy evaluation.
     * 
     * @param iteratorSupplier
     * @return
     */
    public static <T> ObjIterator<T> of(final Supplier<? extends Iterator<? extends T>> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new ObjIterator<T>() {
            private Iterator<? extends T> iter = null;
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
    public static <T> ObjIterator<T> oF(final Supplier<T[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new ObjIterator<T>() {
            private T[] aar = null;
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
            public T next() {
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
     * Returns an infinite {@code ObjIterator}.
     * 
     * @param supplier
     * @return
     */
    public static <T> ObjIterator<T> generate(final Supplier<T> supplier) {
        N.checkArgNotNull(supplier);

        return new ObjIterator<T>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return supplier.get();
            }
        };
    }

    /**
     * 
     * @param hasNext
     * @param supplier
     * @return
     */
    public static <T> ObjIterator<T> generate(final BooleanSupplier hasNext, final Supplier<T> supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ObjIterator<T>() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return supplier.get();
            }
        };
    }

    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    public <A> A[] toArray(A[] a) {
        return toList().toArray(a);
    }

    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    public Stream<T> stream() {
        return Stream.of(this);
    }

    public <E extends Exception> void foreachRemaining(Try.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(next());
        }
    }
}
