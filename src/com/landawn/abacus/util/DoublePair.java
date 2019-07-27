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
public final class DoublePair {
    public final double _1;
    public final double _2;

    DoublePair() {
        this(0, 0);
    }

    DoublePair(double _1, double _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public static DoublePair of(double _1, double _2) {
        return new DoublePair(_1, _2);
    }

    public double min() {
        return N.min(_1, _2);
    }

    public double max() {
        return N.max(_1, _2);
    }

    public double sum() {
        return N.sum(_1, _2);
    }

    public double average() {
        return N.average(_1, _2);
    }

    public DoublePair reversed() {
        return new DoublePair(_2, _1);
    }

    public double[] toArray() {
        return new double[] { _1, _2 };
    }

    public DoubleList toList() {
        return DoubleList.of(_1, _2);
    }

    public <E extends Exception> void forEach(Try.DoubleConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
    }

    public <E extends Exception> void accept(Try.Consumer<DoublePair, E> action) throws E {
        action.accept(this);
    }

    public <U, E extends Exception> U map(Try.Function<DoublePair, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    public <E extends Exception> Optional<DoublePair> filter(final Try.Predicate<DoublePair, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<DoublePair> empty();
    }

    public DoubleStream stream() {
        return DoubleStream.of(_1, _2);
    }

    @Override
    public int hashCode() {
        return (int) (31 * _1 + this._2);
    }

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

    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + "]";
    }
}
