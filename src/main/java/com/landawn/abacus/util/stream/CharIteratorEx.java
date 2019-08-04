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
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.Supplier;

/** 
 * 
 */
@Internal
public abstract class CharIteratorEx extends CharIterator implements IteratorEx<Character> {
    public static final CharIteratorEx EMPTY = new CharIteratorEx() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public char nextChar() {
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
        public char[] toArray() {
            return N.EMPTY_CHAR_ARRAY;
        }

        @Override
        public void close() {
            // Do nothing.
        }
    };

    public static CharIteratorEx empty() {
        return EMPTY;
    }

    @SafeVarargs
    public static CharIteratorEx of(final char... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static CharIteratorEx of(final char[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new CharIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
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
            public char[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public CharList toList() {
                return CharList.of(N.copyOfRange(a, cursor, toIndex));
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    public static CharIteratorEx of(final CharIterator iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof CharIteratorEx) {
            return ((CharIteratorEx) iter);
        }

        return new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                return iter.nextChar();
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
    public static CharIteratorEx of(final Supplier<? extends CharIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new CharIteratorEx() {
            private CharIterator iter = null;
            private CharIteratorEx iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                if (isInitialized == false) {
                    init();
                }

                return iter.nextChar();
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
                    iterEx = iter instanceof CharIteratorEx ? (CharIteratorEx) iter : null;
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
    public static CharIteratorEx oF(final Supplier<char[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new CharIteratorEx() {
            private CharIteratorEx iterEx = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.hasNext();
            }

            @Override
            public char nextChar() {
                if (isInitialized == false) {
                    init();
                }

                return iterEx.nextChar();
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
                    char[] aar = arraySupplier.get();
                    iterEx = CharIteratorEx.of(aar);
                }
            }
        };
    }

    public static CharIteratorEx from(final Iterator<Character> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjIteratorEx) {
            final ObjIteratorEx<Character> iteratorEx = ((ObjIteratorEx<Character>) iter);

            return new CharIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iteratorEx.hasNext();
                }

                @Override
                public char nextChar() {
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
            return new CharIteratorEx() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public char nextChar() {
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
            nextChar();
            n--;
        }
    }

    @Override
    public long count() {
        long result = 0;

        while (hasNext()) {
            nextChar();
            result++;
        }

        return result;
    }

    @Override
    public void close() {
        // Do nothing.
    }
}
