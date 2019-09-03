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
 * The Class CharTriple.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public final class CharTriple {

    /** The  1. */
    public final char _1;

    /** The  2. */
    public final char _2;

    /** The  3. */
    public final char _3;

    /**
     * Instantiates a new char triple.
     */
    CharTriple() {
        this((char) 0, (char) 0, (char) 0);
    }

    /**
     * Instantiates a new char triple.
     *
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     */
    CharTriple(char _1, char _2, char _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    /**
     *
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @return
     */
    public static CharTriple of(char _1, char _2, char _3) {
        return new CharTriple(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public char min() {
        return N.min(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public char max() {
        return N.max(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public char median() {
        return N.median(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public int sum() {
        return _1 + _2 + _3;
    }

    /**
     *
     * @return
     */
    public double average() {
        return (0d + _1 + _2 + _3) / 3;
    }

    /**
     *
     * @return
     */
    public CharTriple reversed() {
        return new CharTriple(_3, _2, _1);
    }

    /**
     *
     * @return
     */
    public char[] toArray() {
        return new char[] { _1, _2, _3 };
    }

    /**
     *
     * @return
     */
    public CharList toList() {
        return CharList.of(_1, _2, _3);
    }

    /**
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Try.CharConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
        comsumer.accept(this._3);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Try.Consumer<CharTriple, E> action) throws E {
        action.accept(this);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(Try.Function<CharTriple, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<CharTriple> filter(Try.Predicate<CharTriple, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<CharTriple> empty();
    }

    /**
     *
     * @return
     */
    public CharStream stream() {
        return CharStream.of(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (31 * (31 * _1 + this._2)) + _3;
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
        } else if (!(obj instanceof CharTriple)) {
            return false;
        } else {
            CharTriple other = (CharTriple) obj;
            return this._1 == other._1 && this._2 == other._2 && this._3 == other._3;
        }
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + ", " + this._3 + "]";
    }
}
