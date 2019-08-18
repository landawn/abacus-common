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
import com.landawn.abacus.util.stream.ShortStream;

// TODO: Auto-generated Javadoc
/**
 * The Class ShortTriple.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public class ShortTriple {

    /** The  1. */
    public final short _1;

    /** The  2. */
    public final short _2;

    /** The  3. */
    public final short _3;

    /**
     * Instantiates a new short triple.
     */
    ShortTriple() {
        this((short) 0, (short) 0, (short) 0);
    }

    /**
     * Instantiates a new short triple.
     *
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     */
    ShortTriple(short _1, short _2, short _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    /**
     * Of.
     *
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @return
     */
    public static ShortTriple of(short _1, short _2, short _3) {
        return new ShortTriple(_1, _2, _3);
    }

    /**
     * Min.
     *
     * @return
     */
    public short min() {
        return N.min(_1, _2, _3);
    }

    /**
     * Max.
     *
     * @return
     */
    public short max() {
        return N.max(_1, _2, _3);
    }

    /**
     * Median.
     *
     * @return
     */
    public short median() {
        return N.median(_1, _2, _3);
    }

    /**
     * Sum.
     *
     * @return
     */
    public int sum() {
        return _1 + _2 + _3;
    }

    /**
     * Average.
     *
     * @return
     */
    public double average() {
        return (0d + _1 + _2 + _3) / 3;
    }

    /**
     * Reversed.
     *
     * @return
     */
    public ShortTriple reversed() {
        return new ShortTriple(_3, _2, _1);
    }

    /**
     * To array.
     *
     * @return
     */
    public short[] toArray() {
        return new short[] { _1, _2, _3 };
    }

    /**
     * To list.
     *
     * @return
     */
    public ShortList toList() {
        return ShortList.of(_1, _2, _3);
    }

    /**
     * For each.
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Try.ShortConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
        comsumer.accept(this._3);
    }

    /**
     * Accept.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Try.Consumer<ShortTriple, E> action) throws E {
        action.accept(this);
    }

    /**
     * Map.
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(Try.Function<ShortTriple, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     * Filter.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<ShortTriple> filter(final Try.Predicate<ShortTriple, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<ShortTriple> empty();
    }

    /**
     * Stream.
     *
     * @return
     */
    public ShortStream stream() {
        return ShortStream.of(_1, _2, _3);
    }

    /**
     * Hash code.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (31 * (31 * _1 + this._2)) + _3;
    }

    /**
     * Equals.
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ShortTriple)) {
            return false;
        } else {
            ShortTriple other = (ShortTriple) obj;
            return this._1 == other._1 && this._2 == other._2 && this._3 == other._3;
        }
    }

    /**
     * To string.
     *
     * @return
     */
    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + ", " + this._3 + "]";
    }

}
