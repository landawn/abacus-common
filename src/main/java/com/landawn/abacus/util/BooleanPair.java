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
import com.landawn.abacus.util.stream.Stream;

/**
 * 
 * @since 1.2
 * 
 * @author Haiyang Li
 */
public class BooleanPair {
    public final boolean _1;
    public final boolean _2;

    BooleanPair() {
        this(false, false);
    }

    BooleanPair(boolean _1, boolean _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public static BooleanPair of(boolean _1, boolean _2) {
        return new BooleanPair(_1, _2);
    }

    public BooleanPair reversed() {
        return new BooleanPair(_2, _1);
    }

    public boolean[] toArray() {
        return new boolean[] { _1, _2 };
    }

    public BooleanList toList() {
        return BooleanList.of(_1, _2);
    }

    public <E extends Exception> void forEach(Try.BooleanConsumer<E> comsumer) throws E {
        comsumer.accept(this._1);
        comsumer.accept(this._2);
    }

    public <E extends Exception> void accept(Try.Consumer<BooleanPair, E> action) throws E {
        action.accept(this);
    }

    public <U, E extends Exception> U map(Try.Function<BooleanPair, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    public <E extends Exception> Optional<BooleanPair> filter(final Try.Predicate<BooleanPair, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<BooleanPair> empty();
    }

    public Stream<Boolean> stream() {
        return Stream.of(_1, _2);
    }

    @Override
    public int hashCode() {
        return 31 * Boolean.valueOf(_1).hashCode() + Boolean.valueOf(_2).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof BooleanPair)) {
            return false;
        } else {
            BooleanPair other = (BooleanPair) obj;
            return this._1 == other._1 && this._2 == other._2;
        }
    }

    @Override
    public String toString() {
        return "[" + this._1 + ", " + this._2 + "]";
    }
}
