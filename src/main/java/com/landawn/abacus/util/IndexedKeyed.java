/*
 * Copyright (C) 2024 HaiYang Li
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
 * @param <K> the key type
 * @param <T>
 * @see Keyed
 * @see Wrapper
 */
@com.landawn.abacus.annotation.Immutable
public final class IndexedKeyed<K, T> extends Keyed<K, T> {

    private final int index;

    IndexedKeyed(int index, K key, T val) {
        super(key, val);
        this.index = index;
    }

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @param index
     * @param key
     * @param val
     * @return
     */
    public static <K, T> IndexedKeyed<K, T> of(final int index, final K key, final T val) {
        return new IndexedKeyed<>(index, key, val);
    }

    /**
     *
     *
     * @return
     */
    public int index() {
        return index;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return N.hashCode(index) * 31 + N.hashCode(key);
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj != null && IndexedKeyed.class.equals(obj.getClass())) {
            @SuppressWarnings("rawtypes")
            final IndexedKeyed another = (IndexedKeyed) obj;

            return N.equals(another.index, index) && N.equals(another.key, key);
        }

        return false;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return "{index=" + N.toString(index) + ", key=" + key + ", val=" + val + "}";
    }
}
