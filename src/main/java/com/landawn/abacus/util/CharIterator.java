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
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.CharStream;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public abstract class CharIterator extends ImmutableIterator<Character> {
    public static final CharIterator EMPTY = new CharIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public char nextChar() {
            throw new NoSuchElementException();
        }
    };

    public static CharIterator empty() {
        return EMPTY;
    }

    @SafeVarargs
    public static CharIterator of(final char... a) {
        return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    public static CharIterator of(final char[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return new CharIterator() {
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
            public char[] toArray() {
                return N.copyOfRange(a, cursor, toIndex);
            }

            @Override
            public CharList toList() {
                return CharList.of(N.copyOfRange(a, cursor, toIndex));
            }
        };
    }

    /**
     * Lazy evaluation.
     * 
     * @param iteratorSupplier
     * @return
     */
    public static CharIterator of(final Supplier<? extends CharIterator> iteratorSupplier) {
        N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

        return new CharIterator() {
            private CharIterator iter = null;
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
    public static CharIterator oF(final Supplier<char[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier, "arraySupplier");

        return new CharIterator() {
            private char[] aar = null;
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
            public char nextChar() {
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
     * Returns an infinite {@code CharIterator}.
     * 
     * @param supplier
     * @return
     */
    public static CharIterator generate(final CharSupplier supplier) {
        N.checkArgNotNull(supplier);

        return new CharIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public char nextChar() {
                return supplier.getAsChar();
            }
        };
    }

    /**
     * 
     * @param hasNext
     * @param supplier
     * @return
     */
    public static CharIterator generate(final BooleanSupplier hasNext, final CharSupplier supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new CharIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public char nextChar() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return supplier.getAsChar();
            }
        };
    }

    /**
     * 
     * @Deprecated use <code>nextChar()</code> instead.
     */
    @Deprecated
    @Override
    public Character next() {
        return nextChar();
    }

    public abstract char nextChar();

    public char[] toArray() {
        return toList().trimToSize().array();
    }

    public CharList toList() {
        final CharList list = new CharList();

        while (hasNext()) {
            list.add(nextChar());
        }

        return list;
    }

    public CharStream stream() {
        return CharStream.of(this);
    }

    public <E extends Exception> void foreachRemaining(Try.CharConsumer<E> action) throws E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextChar());
        }
    }

    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Character> action) {
        super.forEachRemaining(action);
    }
}
