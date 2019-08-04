/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
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

package com.landawn.abacus.util.stream;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
public interface Collector<T, A, R> extends java.util.stream.Collector<T, A, R> {

    @Override
    Supplier<A> supplier();

    @Override
    BiConsumer<A, T> accumulator();

    @Override
    BinaryOperator<A> combiner();

    @Override
    Function<A, R> finisher();

    @Override
    Set<Characteristics> characteristics();

    public static <T, A, R> Collector<T, A, R> from(java.util.stream.Collector<T, A, R> collector) {
        N.checkArgNotNull(collector);

        final Supplier<A> supplier = () -> collector.supplier().get();
        final BiConsumer<A, T> accumulator = (t, u) -> collector.accumulator().accept(t, u);
        final BinaryOperator<A> combiner = (t, u) -> collector.combiner().apply(t, u);
        final Function<A, R> finisher = t -> collector.finisher().apply(t);

        return new Collectors.CollectorImpl<>(supplier, accumulator, combiner, finisher, collector.characteristics());
    }

    /**
     * 
     * @param collector
     * @return
     * @deprecated replaced by {@link #from(java.util.stream.Collector)}
     */
    @Deprecated
    public static <T, A, R> Collector<T, A, R> of(java.util.stream.Collector<T, A, R> collector) {
        return from(collector);
    }

    @SafeVarargs
    public static <T, R> Collector<T, R, R> of(Supplier<R> supplier, BiConsumer<R, T> accumulator, BinaryOperator<R> combiner,
            Characteristics... characteristics) {
        N.checkArgNotNull(supplier);
        N.checkArgNotNull(accumulator);
        N.checkArgNotNull(combiner);
        N.checkArgNotNull(characteristics);

        final Set<Characteristics> cs = (characteristics.length == 0) ? Collectors.CH_ID
                : Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH, characteristics));

        return new Collectors.CollectorImpl<>(supplier, accumulator, combiner, cs);
    }

    @SafeVarargs
    public static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher,
            Characteristics... characteristics) {
        N.checkArgNotNull(supplier);
        N.checkArgNotNull(accumulator);
        N.checkArgNotNull(combiner);
        N.checkArgNotNull(characteristics);

        Set<Characteristics> cs = Collectors.CH_NOID;

        if (characteristics.length > 0) {
            cs = EnumSet.noneOf(Characteristics.class);
            Collections.addAll(cs, characteristics);
            cs = Collections.unmodifiableSet(cs);
        }

        return new Collectors.CollectorImpl<>(supplier, accumulator, combiner, finisher, cs);
    }
}
