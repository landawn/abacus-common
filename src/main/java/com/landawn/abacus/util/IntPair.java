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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.u.Optional;

/**
 *
 * @author Haiyang Li
 * @since 1.2
 */
@Beta
@com.landawn.abacus.annotation.Immutable
public final class IntPair implements Immutable {

    public final int _1;
    public final int _2;

    IntPair() {
        this(0, 0);
    }

    IntPair(int _1, int _2) {
        this._1 = _1;
        this._2 = _2;
    }

    /**
     *
     * @param _1 the 1
     * @param _2 the 2
     * @return
     */
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
        return _1 + _2;
    }

    public double average() {
        return ((double) sum()) / 2;
    }

    public IntPair reverse() {
        return new IntPair(_2, _1);
    }

    /**
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.IntConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Throwables.IntBiConsumer<E> action) throws E {
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
    public <U, E extends Exception> U map(Throwables.IntBiFunction<U, E> mapper) throws E {
        return mapper.apply(_1, _2);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<IntPair> filter(final Throwables.IntBiPredicate<E> predicate) throws E {
        return predicate.test(_1, _2) ? Optional.of(this) : Optional.<IntPair> empty();
    }

    @Override
    public int hashCode() {
        return 31 * _1 + this._2;
    }

    /**
     *
     * @param obj
     * @return
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof IntPair other)) {
            return false;
        } else {
            return this._1 == other._1 && this._2 == other._2;
        }
    }

    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + "]";
    }
}
