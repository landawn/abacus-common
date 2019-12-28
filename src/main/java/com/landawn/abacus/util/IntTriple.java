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

// TODO: Auto-generated Javadoc
/**
 * The Class IntTriple.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public final class IntTriple {

    /** The  1. */
    public final int _1;

    /** The  2. */
    public final int _2;

    /** The  3. */
    public final int _3;

    /**
     * Instantiates a new int triple.
     */
    IntTriple() {
        this(0, 0, 0);
    }

    /**
     * Instantiates a new int triple.
     *
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     */
    IntTriple(int _1, int _2, int _3) {
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
    public static IntTriple of(int _1, int _2, int _3) {
        return new IntTriple(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public int min() {
        return N.min(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public int max() {
        return N.max(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public int median() {
        return N.median(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    public int sum() {
        return _1 + _2 + _3;
    }

    /**
     *
     * @return
     */
    public double average() {
        return ((double) sum()) / 3;
    }

    /**
     *
     * @return
     */
    public IntTriple reversed() {
        return new IntTriple(_3, _2, _1);
    }

    /**
     *
     * @return
     */
    public int[] toArray() {
        return new int[] { _1, _2, _3 };
    }

    /**
     *
     * @return
     */
    public IntList toList() {
        return IntList.of(_1, _2, _3);
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
        comsumer.accept(this._3);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Throwables.Consumer<IntTriple, E> action) throws E {
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
    public <U, E extends Exception> U map(Throwables.Function<IntTriple, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<IntTriple> filter(final Throwables.Predicate<IntTriple, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<IntTriple> empty();
    }

    /**
     *
     * @return
     */
    public IntStream stream() {
        return IntStream.of(_1, _2, _3);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (31 * (31 * _1 + this._2)) + _3;
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
        } else if (!(obj instanceof IntTriple)) {
            return false;
        } else {
            IntTriple other = (IntTriple) obj;
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
