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
import com.landawn.abacus.util.stream.DoubleStream;

/**
 * The Class DoublePair.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public final class DoublePair extends PrimitivePair<DoublePair> {

    /** The  1. */
    public final double _1;

    /** The  2. */
    public final double _2;

    /**
     * Instantiates a new double pair.
     */
    DoublePair() {
        this(0, 0);
    }

    /**
     * Instantiates a new double pair.
     *
     * @param _1 the 1
     * @param _2 the 2
     */
    DoublePair(double _1, double _2) {
        this._1 = _1;
        this._2 = _2;
    }

    /**
     *
     * @param _1 the 1
     * @param _2 the 2
     * @return
     */
    public static DoublePair of(double _1, double _2) {
        return new DoublePair(_1, _2);
    }

    /**
     *
     * @return
     */
    public double min() {
        return N.min(_1, _2);
    }

    /**
     *
     * @return
     */
    public double max() {
        return N.max(_1, _2);
    }

    /**
     *
     * @return
     */
    public double sum() {
        return N.sum(_1, _2);
    }

    /**
     *
     * @return
     */
    public double average() {
        return N.average(_1, _2);
    }

    /**
     *
     * @return
     */
    public DoublePair reverse() {
        return new DoublePair(_2, _1);
    }

    /**
     *
     * @return
     */
    public double[] toArray() {
        return new double[] { _1, _2 };
    }

    /**
     *
     * @return
     */
    public DoubleList toList() {
        return DoubleList.of(_1, _2);
    }

    /**
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.DoubleConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Throwables.DoubleBiConsumer<E> action) throws E {
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
    public <U, E extends Exception> U map(Throwables.DoubleBiFunction<U, E> mapper) throws E {
        return mapper.apply(_1, _2);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<DoublePair> filter(final Throwables.DoubleBiPredicate<E> predicate) throws E {
        return predicate.test(_1, _2) ? Optional.of(this) : Optional.<DoublePair> empty();
    }

    /**
     *
     * @return
     */
    public DoubleStream stream() {
        return DoubleStream.of(_1, _2);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (int) (31 * _1 + this._2);
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
        } else if (!(obj instanceof DoublePair)) {
            return false;
        } else {
            DoublePair other = (DoublePair) obj;
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
