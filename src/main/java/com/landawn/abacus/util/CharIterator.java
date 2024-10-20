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
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.stream.CharStream;

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
public abstract class CharIterator extends ImmutableIterator<Character> {

    public static final CharIterator EMPTY = new CharIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public char nextChar() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     *
     *
     * @return
     */
    public static CharIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static CharIterator of(final char... a) {
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
    public static CharIterator of(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
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
     * Returns an CharIterator instance that is created lazily using the provided Supplier.
     * The Supplier is responsible for producing the CharIterator instance when the CharIterator's methods are first called.
     *
     * @param iteratorSupplier A Supplier that provides the CharIterator when needed.
     * @return A CharIterator that is initialized on the first call to hasNext() or nextByte().
     * @throws IllegalArgumentException if iteratorSupplier is {@code null}.
     */
    public static CharIterator defer(final Supplier<? extends CharIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new CharIterator() {
            private CharIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextChar();
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
     * Returns an infinite {@code CharIterator}.
     *
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static CharIterator generate(final CharSupplier supplier) throws IllegalArgumentException {
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
     *
     * @param hasNext
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static CharIterator generate(final BooleanSupplier hasNext, final CharSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new CharIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public char nextChar() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsChar();
            }
        };
    }

    /**
     *
     * @return
     * @deprecated use {@code nextChar()} instead.
     */
    @Deprecated
    @Override
    public Character next() {
        return nextChar();
    }

    /**
     *
     *
     * @return
     */
    public abstract char nextChar();

    /**
     *
     *
     * @return
     */
    public OptionalChar first() {
        if (hasNext()) {
            return OptionalChar.of(nextChar());
        } else {
            return OptionalChar.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public OptionalChar last() {
        if (hasNext()) {
            char next = nextChar();

            while (hasNext()) {
                next = nextChar();
            }

            return OptionalChar.of(next);
        } else {
            return OptionalChar.empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public char[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     *
     *
     * @return
     */
    public CharList toList() {
        final CharList list = new CharList();

        while (hasNext()) {
            list.add(nextChar());
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public CharStream stream() {
        return CharStream.of(this);
    }

    /**
     *
     *
     * @return
     */
    @Beta
    public ObjIterator<IndexedChar> indexed() {
        return indexed(0);
    }

    /**
     *
     *
     * @param startIndex
     * @return
     */
    @Beta
    public ObjIterator<IndexedChar> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final CharIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedChar next() {
                return IndexedChar.of(iter.nextChar(), idx++);
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
    public void forEachRemaining(final java.util.function.Consumer<? super Character> action) throws IllegalArgumentException {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void foreachRemaining(final Throwables.CharConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextChar());
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
    public <E extends Exception> void foreachIndexed(final Throwables.IntCharConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextChar());
        }
    }
}
