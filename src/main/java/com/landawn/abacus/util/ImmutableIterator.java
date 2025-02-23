/*
 * Copyright (c) 2017, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Fn.Suppliers;

/**
 *
 * @param <T>
 */
@com.landawn.abacus.annotation.Immutable
abstract class ImmutableIterator<T> implements java.util.Iterator<T>, Immutable {

    /**
     *
     * @throws UnsupportedOperationException
     * @deprecated - UnsupportedOperationException
     */
    @Deprecated
    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Set<T> toSet() {
        return toCollection(Suppliers.ofSet());
    }

    /**
     *
     * @param <C>
     * @param supplier
     * @return
     */
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier) {
        final C c = supplier.get();

        while (hasNext()) {
            c.add(next());
        }

        return c;
    }

    public ImmutableList<T> toImmutableList() {
        return ImmutableList.wrap(toCollection(Suppliers.ofList()));
    }

    public ImmutableSet<T> toImmutableSet() {
        return ImmutableSet.wrap(toSet());
    }

    /**
     * Returns the number of elements in this
     *
     * @return
     */
    @Beta
    public long count() {
        long count = 0;

        while (hasNext()) {
            next();
            count++;
        }

        return count;
    }

    // Not efficient for primitive iterators because function is not for primitives.
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param keyExtractor
    //     * @return
    //     * @throws E the e
    //     */
    //    public <K, E extends Exception> Map<K, T> toMap(final Try.Function<? super T, K, E> keyExtractor) throws E {
    //        return Iterators.toMap(this, keyExtractor);
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Try.Function<? super T, K, E> keyExtractor,
    //            final Try.Function<? super T, ? extends V, E2> valueExtractor) throws E, E2 {
    //        return Iterators.toMap(this, keyExtractor, valueExtractor);
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <M>
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Try.Function<? super T, K, E> keyExtractor,
    //            final Try.Function<? super T, ? extends V, E2> valueExtractor, final Supplier<? extends M> mapSupplier) throws E, E2 {
    //        return Iterators.toMap(this, keyExtractor, valueExtractor, mapSupplier);
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param keyExtractor
    //     * @return
    //     * @throws E the e
    //     */
    //    public <K, E extends Exception> ImmutableMap<K, T> toImmutableMap(final Try.Function<? super T, K, E> keyExtractor) throws E {
    //        return ImmutableMap.wrap(toMap(keyExtractor));
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    public <K, V, E extends Exception, E2 extends Exception> ImmutableMap<K, V> toImmutableMap(final Try.Function<? super T, K, E> keyExtractor,
    //            final Try.Function<? super T, ? extends V, E2> valueExtractor) throws E, E2 {
    //        return ImmutableMap.wrap(toMap(keyExtractor, valueExtractor));
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <M>
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> ImmutableMap<K, V> toImmutableMap(
    //            final Try.Function<? super T, K, E> keyExtractor, final Try.Function<? super T, ? extends V, E2> valueExtractor,
    //            final Supplier<? extends M> mapSupplier) throws E, E2 {
    //        return ImmutableMap.wrap(toMap(keyExtractor, valueExtractor, mapSupplier));
    //    }
}
