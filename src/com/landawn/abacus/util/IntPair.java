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
import com.landawn.abacus.util.stream.IntStream;

/**
 * 
 * @since 1.2
 * 
 * @author Haiyang Li
 */
public final class IntPair {
    public final int _1;
    public final int _2;

    IntPair() {
        this(0, 0);
    }

    IntPair(int _1, int _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public static IntPair of(int _1, int _2) {
        return new IntPair(_1, _2);
    }

    public int min() {
        return N.min(_1, _2);
    }

    public int max() {
        return N.max(_1, _2);
    }

    public int sum() {
        return N.toIntExact(0L + _1 + _2);
    }

    public double average() {
        return (0d + _1 + _2) / 2;
    }

    public IntPair reversed() {
        return new IntPair(_2, _1);
    }

    public int[] toArray() {
        return new int[] { _1, _2 };
    }

    public IntList toList() {
        return IntList.of(_1, _2);
    }

    public <E extends Exception> void forEach(Try.IntConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
    }

    public <E extends Exception> void accept(Try.Consumer<IntPair, E> action) throws E {
        action.accept(this);
    }

    public <U, E extends Exception> U map(Try.Function<IntPair, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    public <E extends Exception> Optional<IntPair> filter(final Try.Predicate<IntPair, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<IntPair> empty();
    }

    public IntStream stream() {
        return IntStream.of(_1, _2);
    }

    @Override
    public int hashCode() {
        return 31 * _1 + this._2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof IntPair)) {
            return false;
        } else {
            IntPair other = (IntPair) obj;
            return this._1 == other._1 && this._2 == other._2;
        }
    }

    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + "]";
    }
}
