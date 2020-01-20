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
 * The Class BooleanTriple.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public class BooleanTriple {

    /** The  1. */
    public final boolean _1;

    /** The  2. */
    public final boolean _2;

    /** The  3. */
    public final boolean _3;

    /**
     * Instantiates a new boolean triple.
     */
    BooleanTriple() {
        this(false, false, false);
    }

    /**
     * Instantiates a new boolean triple.
     *
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     */
    BooleanTriple(boolean _1, boolean _2, boolean _3) {
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
    public static BooleanTriple of(boolean _1, boolean _2, boolean _3) {
        return new BooleanTriple(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public BooleanTriple reverse() {
        return new BooleanTriple(_3, _2, _1);
    }

    /**
     *
     * @return
     */
    public boolean[] toArray() {
        return new boolean[] { _1, _2, _3 };
    }

    /**
     *
     * @return
     */
    public BooleanList toList() {
        return BooleanList.of(_1, _2, _3);
    }

    /**
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.BooleanConsumer<E> comsumer) throws E {
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
    public <E extends Exception> void accept(Throwables.Consumer<BooleanTriple, E> action) throws E {
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
    public <U, E extends Exception> U map(Throwables.Function<BooleanTriple, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<BooleanTriple> filter(final Throwables.Predicate<BooleanTriple, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<BooleanTriple> empty();
    }

    /**
     *
     * @return
     */
    public Stream<Boolean> stream() {
        return Stream.of(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (31 * (31 * Boolean.valueOf(_1).hashCode() + Boolean.valueOf(_2).hashCode())) + Boolean.valueOf(_3).hashCode();
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
        } else if (!(obj instanceof BooleanTriple)) {
            return false;
        } else {
            BooleanTriple other = (BooleanTriple) obj;
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
