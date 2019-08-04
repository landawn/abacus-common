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
 * 
 * @since 1.2
 * 
 * @author Haiyang Li
 */
public final class DoubleTriple {
    public final double _1;
    public final double _2;
    public final double _3;

    DoubleTriple() {
        this(0, 0, 0);
    }

    DoubleTriple(double _1, double _2, double _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    public static DoubleTriple of(double _1, double _2, double _3) {
        return new DoubleTriple(_1, _2, _3);
    }

    public double min() {
        return N.min(_1, _2, _3);
    }

    public double max() {
        return N.max(_1, _2, _3);
    }

    public double median() {
        return N.median(_1, _2, _3);
    }

    public double sum() {
        return N.sum(_1, _2, _3);
    }

    public double average() {
        return N.average(_1, _2, _3);
    }

    public DoubleTriple reversed() {
        return new DoubleTriple(_3, _2, _1);
    }

    public double[] toArray() {
        return new double[] { _1, _2, _3 };
    }

    public DoubleList toList() {
        return DoubleList.of(_1, _2, _3);
    }

    public <E extends Exception> void forEach(Try.DoubleConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
        comsumer.accept(this._3);
    }

    public <E extends Exception> void accept(Try.Consumer<DoubleTriple, E> action) throws E {
        action.accept(this);
    }

    public <U, E extends Exception> U map(Try.Function<DoubleTriple, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    public <E extends Exception> Optional<DoubleTriple> filter(final Try.Predicate<DoubleTriple, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<DoubleTriple> empty();
    }

    public DoubleStream stream() {
        return DoubleStream.of(_1, _2, _3);
    }

    @Override
    public int hashCode() {
        return (int) ((31 * (31 * _1 + this._2)) + _3);
    }

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

    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + ", " + this._3 + "]";
    }
}
