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

// TODO: Auto-generated Javadoc
/**
 * The Class DoubleTriple.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public final class DoubleTriple {

    /** The  1. */
    public final double _1;

    /** The  2. */
    public final double _2;

    /** The  3. */
    public final double _3;

    /**
     * Instantiates a new double triple.
     */
    DoubleTriple() {
        this(0, 0, 0);
    }

    /**
     * Instantiates a new double triple.
     *
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     */
    DoubleTriple(double _1, double _2, double _3) {
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
    public static DoubleTriple of(double _1, double _2, double _3) {
        return new DoubleTriple(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public double min() {
        return N.min(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public double max() {
        return N.max(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public double median() {
        return N.median(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public double sum() {
        return N.sum(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public double average() {
        return N.average(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public DoubleTriple reversed() {
        return new DoubleTriple(_3, _2, _1);
    }

    /**
     *
     * @return
     */
    public double[] toArray() {
        return new double[] { _1, _2, _3 };
    }

    /**
     *
     * @return
     */
    public DoubleList toList() {
        return DoubleList.of(_1, _2, _3);
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
        comsumer.accept(this._3);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Throwables.Consumer<DoubleTriple, E> action) throws E {
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
    public <U, E extends Exception> U map(Throwables.Function<DoubleTriple, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<DoubleTriple> filter(final Throwables.Predicate<DoubleTriple, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<DoubleTriple> empty();
    }

    /**
     *
     * @return
     */
    public DoubleStream stream() {
        return DoubleStream.of(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (int) ((31 * (31 * _1 + this._2)) + _3);
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
        } else if (!(obj instanceof DoubleTriple)) {
            return false;
        } else {
            DoubleTriple other = (DoubleTriple) obj;
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
