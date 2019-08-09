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
public final class CharPair {

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
     * @param _1 the  1
     * @param _2 the  2
     */
    CharPair(char _1, char _2) {
        this._1 = _1;
        this._2 = _2;
    }

    /**
     * Of.
     *
     * @param _1 the  1
     * @param _2 the  2
     * @return the char pair
     */
    public static CharPair of(char _1, char _2) {
        return new CharPair(_1, _2);
    }

    /**
     * Min.
     *
     * @return the char
     */
    public char min() {
        return N.min(_1, _2);
    }

    /**
     * Max.
     *
     * @return the char
     */
    public char max() {
        return N.max(_1, _2);
    }

    /**
     * Sum.
     *
     * @return the int
     */
    public int sum() {
        return _1 + _2;
    }

    /**
     * Average.
     *
     * @return the double
     */
    public double average() {
        return (0d + _1 + _2) / 2;
    }

    /**
     * Reversed.
     *
     * @return the char pair
     */
    public CharPair reversed() {
        return new CharPair(_2, _1);
    }

    /**
     * To array.
     *
     * @return the char[]
     */
    public char[] toArray() {
        return new char[] { _1, _2 };
    }

    /**
     * To list.
     *
     * @return the char list
     */
    public CharList toList() {
        return CharList.of(_1, _2);
    }

    /**
     * For each.
     *
     * @param <E> the element type
     * @param comsumer the comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Try.CharConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
    }

    /**
     * Accept.
     *
     * @param <E> the element type
     * @param action the action
     * @throws E the e
     */
    public <E extends Exception> void accept(Try.Consumer<CharPair, E> action) throws E {
        action.accept(this);
    }

    /**
     * Map.
     *
     * @param <U> the generic type
     * @param <E> the element type
     * @param mapper the mapper
     * @return the u
     * @throws E the e
     */
    public <U, E extends Exception> U map(Try.Function<CharPair, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     */
    public <E extends Exception> Optional<CharPair> filter(Try.Predicate<CharPair, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<CharPair> empty();
    }

    /**
     * Stream.
     *
     * @return the char stream
     */
    public CharStream stream() {
        return CharStream.of(_1, _2);
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return 31 * _1 + this._2;
    }

    /**
     * Equals.
     *
     * @param obj the obj
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
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + "]";
    }
}
