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
import com.landawn.abacus.util.stream.Stream;

// TODO: Auto-generated Javadoc
/**
 * The Class BooleanPair.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public class BooleanPair {

    /** The  1. */
    public final boolean _1;

    /** The  2. */
    public final boolean _2;

    /**
     * Instantiates a new boolean pair.
     */
    BooleanPair() {
        this(false, false);
    }

    /**
     * Instantiates a new boolean pair.
     *
     * @param _1 the  1
     * @param _2 the  2
     */
    BooleanPair(boolean _1, boolean _2) {
        this._1 = _1;
        this._2 = _2;
    }

    /**
     * Of.
     *
     * @param _1 the  1
     * @param _2 the  2
     * @return the boolean pair
     */
    public static BooleanPair of(boolean _1, boolean _2) {
        return new BooleanPair(_1, _2);
    }

    /**
     * Reversed.
     *
     * @return the boolean pair
     */
    public BooleanPair reversed() {
        return new BooleanPair(_2, _1);
    }

    /**
     * To array.
     *
     * @return the boolean[]
     */
    public boolean[] toArray() {
        return new boolean[] { _1, _2 };
    }

    /**
     * To list.
     *
     * @return the boolean list
     */
    public BooleanList toList() {
        return BooleanList.of(_1, _2);
    }

    /**
     * For each.
     *
     * @param <E> the element type
     * @param comsumer the comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Try.BooleanConsumer<E> comsumer) throws E {
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
    public <E extends Exception> void accept(Try.Consumer<BooleanPair, E> action) throws E {
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
    public <U, E extends Exception> U map(Try.Function<BooleanPair, U, E> mapper) throws E {
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
    public <E extends Exception> Optional<BooleanPair> filter(final Try.Predicate<BooleanPair, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<BooleanPair> empty();
    }

    /**
     * Stream.
     *
     * @return the stream
     */
    public Stream<Boolean> stream() {
        return Stream.of(_1, _2);
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return 31 * Boolean.valueOf(_1).hashCode() + Boolean.valueOf(_2).hashCode();
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
        } else if (!(obj instanceof BooleanPair)) {
            return false;
        } else {
            BooleanPair other = (BooleanPair) obj;
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
