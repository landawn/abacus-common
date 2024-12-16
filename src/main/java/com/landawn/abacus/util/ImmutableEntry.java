/*
 * Copyright (C) 2017 HaiYang Li
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

import java.io.Serial;
import java.util.AbstractMap;
import java.util.Map;

/**
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@com.landawn.abacus.annotation.Immutable
public final class ImmutableEntry<K, V> extends AbstractMap.SimpleImmutableEntry<K, V> implements Immutable {

    @Serial
    private static final long serialVersionUID = -7667037689002186862L;

    ImmutableEntry(final K key, final V value) {
        super(key, value);
    }

    ImmutableEntry(final Map.Entry<? extends K, ? extends V> entry) {
        super(entry);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @param value
     * @return
     */
    public static <K, V> ImmutableEntry<K, V> of(final K key, final V value) {
        return new ImmutableEntry<>(key, value);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param entry
     * @return
     */
    public static <K, V> ImmutableEntry<K, V> copyOf(final Map.Entry<? extends K, ? extends V> entry) {
        return new ImmutableEntry<>(entry.getKey(), entry.getValue());
    }

    /**
     * Always throw UnsupportedOperationException.
     *
     * @param v
     * @return
     * @throws UnsupportedOperationException the unsupported operation exception
     * @deprecated
     */
    @Deprecated
    @Override
    public V setValue(final V v) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
