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
import com.landawn.abacus.util.stream.FloatStream;

/**
 * 
 * @since 1.2
 * 
 * @author Haiyang Li
 */
public final class FloatTriple {
    public final float _1;
    public final float _2;
    public final float _3;

    FloatTriple() {
        this(0, 0, 0);
    }

    FloatTriple(float _1, float _2, float _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    public static FloatTriple of(float _1, float _2, float _3) {
        return new FloatTriple(_1, _2, _3);
    }

    public float min() {
        return N.min(_1, _2, _3);
    }

    public float max() {
        return N.max(_1, _2, _3);
    }

    public float median() {
        return N.median(_1, _2, _3);
    }

    public float sum() {
        return N.sum(_1, _2, _3);
    }

    public double average() {
        return N.average(_1, _2, _3);
    }

    public FloatTriple reversed() {
        return new FloatTriple(_3, _2, _1);
    }

    public float[] toArray() {
        return new float[] { _1, _2, _3 };
    }

    public FloatList toList() {
        return FloatList.of(_1, _2, _3);
    }

    public <E extends Exception> void forEach(Try.FloatConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
        comsumer.accept(this._3);
    }

    public <E extends Exception> void accept(Try.Consumer<FloatTriple, E> action) throws E {
        action.accept(this);
    }

    public <U, E extends Exception> U map(Try.Function<FloatTriple, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    public <E extends Exception> Optional<FloatTriple> filter(final Try.Predicate<FloatTriple, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<FloatTriple> empty();
    }

    public FloatStream stream() {
        return FloatStream.of(_1, _2, _3);
    }

    @Override
    public int hashCode() {
        return (int) ((31 * (31 * _1 + this._2)) + _3);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof FloatTriple)) {
            return false;
        } else {
            FloatTriple other = (FloatTriple) obj;
            return this._1 == other._1 && this._2 == other._2 && this._3 == other._3;
        }
    }

    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + ", " + this._3 + "]";
    }
}
