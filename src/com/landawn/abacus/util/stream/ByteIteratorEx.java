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
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.Supplier;

/** 
 * 
 */
@Internal
public abstract class ByteIteratorEx extends ByteIterator implements IteratorEx<Byte> {
    public static final ByteIteratorEx EMPTY = new ByteIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public byte nextByte() {
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
        public byte[] toArray() {
            return N.EMPTY_BYTE_ARRAY;
        }

        @Override
        public void close() {
            // Do nothing.
        }
    };

    public static ByteIteratorEx empty() {
        return EMPTY;
    }

    @SafeVarargs
    public static ByteIteratorEx of(final byte... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static ByteIteratorEx of(final byte[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new ByteIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
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
            public byte[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public ByteList toList() {
                return ByteList.of(N.copyOfRange(a, cursor, toIndex));
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static ByteIteratorEx of(final ByteIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ByteIteratorEx) {
            return ((ByteIteratorEx) iter);
        }

        return new ByteIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                return iter.nextByte();
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
    public static ByteIteratorEx of(final Supplier<? extends ByteIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new ByteIteratorEx() {
            private ByteIterator iter = null;
            private ByteIteratorEx iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (isInitialized == false) {
                    init();
                }

                return iter.nextByte();
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
                    iterEx = iter instanceof ByteIteratorEx ? (ByteIteratorEx) iter : null;
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
    public static ByteIteratorEx oF(final Supplier<byte[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new ByteIteratorEx() {
            private ByteIteratorEx iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.hasNext();
            }

            @Override
            public byte nextByte() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.nextByte();
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
                    byte[] aar = arraySupplier.get();
                    iterEx = ByteIteratorEx.of(aar);
                }
            }
        };
    }

    public static ByteIteratorEx from(final Iterator<Byte> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx) {
            final ObjIteratorEx<Byte> iteratorEx = ((ObjIteratorEx<Byte>) iter);

            return new ByteIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public byte nextByte() {
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
            return new ByteIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public byte nextByte() {
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
            nextByte();
            n--;
        }
    }

    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextByte();
            result++;
        }

        return result;
    }

    @Override
    public void close() {
        // Do nothing.
    }
}
