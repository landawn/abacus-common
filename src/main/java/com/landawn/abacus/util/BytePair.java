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
import com.landawn.abacus.util.stream.ByteStream;

// TODO: Auto-generated Javadoc
/**
 * The Class BytePair.
 *
 * @author Haiyang Li
 * @since 1.2
 */
public class BytePair {

    /** The  1. */
    public final byte _1;

    /** The  2. */
    public final byte _2;

    /**
     * Instantiates a new byte pair.
     */
    BytePair() {
        this((byte) 0, (byte) 0);
    }

    /**
     * Instantiates a new byte pair.
     *
     * @param _1 the 1
     * @param _2 the 2
     */
    BytePair(byte _1, byte _2) {
        this._1 = _1;
        this._2 = _2;
    }

    /**
     *
     * @param _1 the 1
     * @param _2 the 2
     * @return
     */
    public static BytePair of(byte _1, byte _2) {
        return new BytePair(_1, _2);
    }

    /**
     *
     * @return
     */
    public byte min() {
        return N.min(_1, _2);
    }

    /**
     *
     * @return
     */
    public byte max() {
        return N.max(_1, _2);
    }

    /**
     *
     * @return
     */
    public int sum() {
        return _1 + _2;
    }

    /**
     *
     * @return
     */
    public double average() {
        return (0d + _1 + _2) / 2;
    }

    /**
     *
     * @return
     */
    public BytePair reversed() {
        return new BytePair(_2, _1);
    }

    /**
     *
     * @return
     */
    public byte[] toArray() {
        return new byte[] { _1, _2 };
    }

    /**
     *
     * @return
     */
    public ByteList toList() {
        return ByteList.of(_1, _2);
    }

    /**
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.ByteConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Throwables.Consumer<BytePair, E> action) throws E {
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
    public <U, E extends Exception> U map(Throwables.Function<BytePair, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<BytePair> filter(final Throwables.Predicate<BytePair, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<BytePair> empty();
    }

    /**
     *
     * @return
     */
    public ByteStream stream() {
        return ByteStream.of(_1, _2);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return 31 * _1 + this._2;
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
        } else if (!(obj instanceof BytePair)) {
            return false;
        } else {
            BytePair other = (BytePair) obj;
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
