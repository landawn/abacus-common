/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

/**
 * It's designed for performance improvement by only hash/compare {@code key} in {@code hashCode/equals} method.
 *
 * @param <K> the key type
 * @param <T>
 * @see IndexedKeyed
 * @see Wrapper
 */
@com.landawn.abacus.annotation.Immutable
public sealed class Keyed<K, T> implements Immutable permits IndexedKeyed {

    protected final K key;

    protected final T val;

    Keyed(final K key, final T val) {
        this.key = key;
        this.val = val;
    }

    /**
     * @param <K> the key type
     * @param <T>
     * @param key
     * @param val
     * @return
     */
    public static <K, T> Keyed<K, T> of(final K key, final T val) {
        return new Keyed<>(key, val);
    }

    public K key() {
        return key;
    }

    public T val() {
        return val;
    }

    @Override
    public int hashCode() {
        return N.hashCode(key);
    }

    /**
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj != null && this.getClass().equals(obj.getClass())) {
            return N.equals(((Keyed<K, T>) obj).key, key);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{key=" + N.toString(key) + ", val=" + val + "}";
    }
}
