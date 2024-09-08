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
import com.landawn.abacus.util.stream.IntStream;

/**
 *
 * @author Haiyang Li
 * @since 1.2
 */
@Beta
@com.landawn.abacus.annotation.Immutable
public final class IntTriple implements Immutable {

    public final int _1; //NOSONAR
    public final int _2; //NOSONAR
    public final int _3; //NOSONAR

    IntTriple() {
        this(0, 0, 0);
    }

    IntTriple(final int _1, final int _2, final int _3) {
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
    public static IntTriple of(final int _1, final int _2, final int _3) {
        return new IntTriple(_1, _2, _3);
    }

    /**
     *
     *
     * @return
     */
    public int min() {
        return N.min(_1, _2, _3);
    }

    /**
     *
     *
     * @return
     */
    public int max() {
        return N.max(_1, _2, _3);
    }

    /**
     *
     *
     * @return
     */
    public int sum() {
        return _1 + _2 + _3;
    }

    /**
     *
     *
     * @return
     */
    public double average() {
        return ((double) sum()) / 2;
    }

    /**
     *
     *
     * @return
     */
    public IntTriple reverse() {
        return new IntTriple(_3, _2, _1);
    }

    /**
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(final Throwables.IntConsumer<E> comsumer) throws E {
        comsumer.accept(_1);
        comsumer.accept(_2);
        comsumer.accept(_3);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(final Throwables.IntTriConsumer<E> action) throws E {
        action.accept(_1, _2, _3);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(final Throwables.IntTriFunction<U, E> mapper) throws E {
        return mapper.apply(_1, _2, _3);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<IntTriple> filter(final Throwables.IntTriPredicate<E> predicate) throws E {
        return predicate.test(_1, _2, _3) ? Optional.of(this) : Optional.<IntTriple> empty();
    }

    /**
     *
     *
     * @return
     */
    public IntStream stream() {
        return IntStream.of(_1, _2, _3);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return 31 * (31 * _1 + _2) + _3;
    }

    /**
     *
     * @param obj
     * @return
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof final IntTriple other)) {
            return false;
        } else {
            return _1 == other._1 && _2 == other._2 && _3 == other._3;
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return "[" + _1 + ", " + _2 + ", " + _3 + "]";
    }
}
