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
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.Supplier;

/**
 *  
 */
@Internal
public abstract class FloatIteratorEx extends FloatIterator implements IteratorEx<Float> {
    public static final FloatIteratorEx EMPTY = new FloatIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public float nextFloat() {
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
        public float[] toArray() {
            return N.EMPTY_FLOAT_ARRAY;
        }

        @Override
        public void close() {
            // Do nothing.
        }
    };

    public static FloatIteratorEx empty() {
        return EMPTY;
    }

    @SafeVarargs
    public static FloatIteratorEx of(final float... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static FloatIteratorEx of(final float[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new FloatIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
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
            public float[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public FloatList toList() {
                return FloatList.of(N.copyOfRange(a, cursor, toIndex));
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static FloatIteratorEx of(final FloatIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof FloatIteratorEx) {
            return ((FloatIteratorEx) iter);
        }

        return new FloatIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                return iter.nextFloat();
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
    public static FloatIteratorEx of(final Supplier<? extends FloatIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new FloatIteratorEx() {
            private FloatIterator iter = null;
            private FloatIteratorEx iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (isInitialized == false) {
                    init();
                }

                return iter.nextFloat();
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
                    iterEx = iter instanceof FloatIteratorEx ? (FloatIteratorEx) iter : null;
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
    public static FloatIteratorEx oF(final Supplier<float[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new FloatIteratorEx() {
            private FloatIteratorEx iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.hasNext();
            }

            @Override
            public float nextFloat() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.nextFloat();
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
                    float[] aar = arraySupplier.get();
                    iterEx = FloatIteratorEx.of(aar);
                }
            }
        };
    }

    public static FloatIteratorEx from(final Iterator<Float> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx) {
            final ObjIteratorEx<Float> iteratorEx = ((ObjIteratorEx<Float>) iter);

            return new FloatIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public float nextFloat() {
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
            return new FloatIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public float nextFloat() {
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
            nextFloat();
            n--;
        }
    }

    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextFloat();
            result++;
        }

        return result;
    }

    @Override
    public void close() {
        // Do nothing.
    }
}
