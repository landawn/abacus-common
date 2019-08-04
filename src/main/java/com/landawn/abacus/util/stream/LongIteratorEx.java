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

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
@Internal
public abstract class LongIteratorEx extends LongIterator implements IteratorEx<Long> {
    public static final LongIteratorEx EMPTY = new LongIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public long nextLong() {
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
        public long[] toArray() {
            return N.EMPTY_LONG_ARRAY;
        }

        @Override
        public void close() {
            // Do nothing.
        }
    };

    public static LongIteratorEx empty() {
        return EMPTY;
    }

    @SafeVarargs
    public static LongIteratorEx of(final long... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static LongIteratorEx of(final long[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new LongIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
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
            public long[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public LongList toList() {
                return LongList.of(N.copyOfRange(a, cursor, toIndex));
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static LongIteratorEx of(final LongIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof LongIteratorEx) {
            return ((LongIteratorEx) iter);
        }

        return new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                return iter.nextLong();
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    /**
     * Lazy evaluation.
     * 
     * @param iteratorSupplier
     * @return
     */
    public static LongIteratorEx of(final Supplier<? extends LongIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new LongIteratorEx() {
            private LongIterator iter = null;
            private LongIteratorEx iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (isInitialized == false) {
                    init();
                }

                return iter.nextLong();
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
                    iterEx = iter instanceof LongIteratorEx ? (LongIteratorEx) iter : null;
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
    public static LongIteratorEx oF(final Supplier<long[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new LongIteratorEx() {
            private LongIteratorEx iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.hasNext();
            }

            @Override
            public long nextLong() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.nextLong();
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
                    long[] aar = arraySupplier.get();
                    iterEx = LongIteratorEx.of(aar);
                }
            }
        };
    }

    public static LongIteratorEx from(final Iterator<Long> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx) {
            final ObjIteratorEx<Long> iteratorEx = ((ObjIteratorEx<Long>) iter);

            return new LongIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public long nextLong() {
                    return iteratorEx.next();
                }

                @Override
                public void skip(long n) {
                    N.checkArgNotNegative(n, "n");

                    iteratorEx.skip(n);
                }

                @Override
                public long count() {
                    return iteratorEx.count();
                }

                @Override
                public void close() {
                    iteratorEx.close();
                }
            };
        } else {
            return new LongIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public long nextLong() {
                    return iter.next();
                }

                @Override
                public void close() {
                    // Do nothing.
                }
            };
        }
    }

    @Override
    public void skip(long n) {
        N.checkArgNotNegative(n, "n");

        while (n > 0 && hasNext()) {
            nextLong();
            n--;
        }
    }

    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextLong();
            result++;
        }

        return result;
    }

    @Override
    public void close() {
        // Do nothing.
    }
}
