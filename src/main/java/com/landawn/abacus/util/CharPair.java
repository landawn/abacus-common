/*
 * Copyright (C) 2018 HaiYang Li
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

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.CharStream;

// TODO: Auto-generated Javadoc
/**
 * The Class CharPair.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public final class CharPair extends PrimitivePair<CharPair> {

    /** The  1. */
    public final char _1;

    /** The  2. */
    public final char _2;

    /**
     * Instantiates a new char pair.
     */
    CharPair() {
        this((char) 0, (char) 0);
    }

    /**
     * Instantiates a new char pair.
     *
     * @param _1 the 1
     * @param _2 the 2
     */
    CharPair(char _1, char _2) {
        this._1 = _1;
        this._2 = _2;
    }

    /**
     *
     * @param _1 the 1
     * @param _2 the 2
     * @return
     */
    public static CharPair of(char _1, char _2) {
        return new CharPair(_1, _2);
    }

    /**
     *
     * @return
     */
    public char min() {
        return N.min(_1, _2);
    }

    /**
     *
     * @return
     */
    public char max() {
        return N.max(_1, _2);
    }

    /**
     *
     * @return
     */
    public int sum() {
        return _1 + _2;
    }

    /**
     *
     * @return
     */
    public double average() {
        return (0d + _1 + _2) / 2;
    }

    /**
     *
     * @return
     */
    public CharPair reverse() {
        return new CharPair(_2, _1);
    }

    /**
     *
     * @return
     */
    public char[] toArray() {
        return new char[] { _1, _2 };
    }

    /**
     *
     * @return
     */
    public CharList toList() {
        return CharList.of(_1, _2);
    }

    /**
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.CharConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Throwables.CharBiConsumer<E> action) throws E {
        action.accept(_1, _2);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(Throwables.CharBiFunction<U, E> mapper) throws E {
        return mapper.apply(_1, _2);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<CharPair> filter(final Throwables.CharBiPredicate<E> predicate) throws E {
        return predicate.test(_1, _2) ? Optional.of(this) : Optional.<CharPair> empty();
    }

    /**
     *
     * @return
     */
    public CharStream stream() {
        return CharStream.of(_1, _2);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return 31 * _1 + this._2;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CharPair)) {
            return false;
        } else {
            CharPair other = (CharPair) obj;
            return this._1 == other._1 && this._2 == other._2;
        }
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + "]";
    }
}
