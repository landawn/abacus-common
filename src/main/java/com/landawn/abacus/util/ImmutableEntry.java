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

import java.util.AbstractMap;
import java.util.Map;

/**
 * 
 * @since 0.9
 * 
 * @author Haiyang Li
 */
public class ImmutableEntry<K, V> extends AbstractMap.SimpleImmutableEntry<K, V> implements Map.Entry<K, V> {

    private static final long serialVersionUID = -7667037689002186862L;

    public ImmutableEntry(K key, V value) {
        super(key, value);
    }

    public ImmutableEntry(Map.Entry<? extends K, ? extends V> entry) {
        super(entry);
    }

    public static <K, V> ImmutableEntry<K, V> of(K key, V value) {
        return new ImmutableEntry<>(key, value);
    }

    public static <K, V> ImmutableEntry<K, V> from(Map.Entry<? extends K, ? extends V> entry) {
        return new ImmutableEntry<K, V>(entry.getKey(), entry.getValue());
    }

    /**
     * Always throw UnsupportedOperationException.
     * 
     * @deprecated
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public V setValue(V v) {
        throw new UnsupportedOperationException();
    }
}
