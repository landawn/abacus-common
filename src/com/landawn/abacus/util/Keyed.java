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

/**
 * It's designed for performance improvement by only hash/compare {@code key} in {@code hashCode/equals} method.
 * 
 * @author HaiYang Li
 *
 * @param <K>
 * @param <T>
 */
public class Keyed<K, T> {
    private final K key;
    private final T val;

    Keyed(K key, T val) {
        this.key = key;
        this.val = val;
    }

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

    @Override
    public boolean equals(Object val) {
        if (val == this) {
            return true;
        }

        if (val instanceof Keyed) {
            final Keyed<K, T> other = (Keyed<K, T>) val;
            return N.equals(key, other.key);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{key=" + N.toString(key) + ", val=" + val + "}";
    }
}
