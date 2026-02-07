/*
 * Copyright (C) 2024 HaiYang Li
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
 * An immutable holder of {@code index}, {@code key}, and {@code value}.
 *
 * <p>This type extends {@link Keyed} and adds positional information. Equality and hash code are
 * based on {@code index} and {@code key} only; {@code value} is intentionally ignored.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * IndexedKeyed<String, User> indexed = IndexedKeyed.of("user123", new User("John"), 0);
 * System.out.println(indexed.index()); // 0
 * System.out.println(indexed.key());   // user123
 * System.out.println(indexed.val());   // User("John")
 * }</pre>
 *
 * @param <K> key type
 * @param <T> value type
 * @see Keyed
 * @see Wrapper
 */
@com.landawn.abacus.annotation.Immutable
public final class IndexedKeyed<K, T> extends Keyed<K, T> {

    private final int index;

    IndexedKeyed(final K key, final T val, final int index) {
        super(key, val);
        this.index = index;
    }

    /**
     * Creates an {@code IndexedKeyed} with the specified key, value, and index.
     *
     * @param key key component (nullable)
     * @param val value component (nullable)
     * @param index index component
     * @param <K> key type
     * @param <T> value type
     * @return a new immutable {@code IndexedKeyed}
     */
    public static <K, T> IndexedKeyed<K, T> of(final K key, final T val, final int index) {
        return new IndexedKeyed<>(key, val, index);
    }

    /**
     * Returns the index component.
     *
     * @return index value
     */
    public int index() {
        return index;
    }

    /**
     * Returns a hash code based on {@code index} and {@code key}.
     *
     * <p>{@code value} is not included.</p>
     *
     * @return hash code for this object
     */
    @Override
    public int hashCode() {
        return N.hashCode(index) * 31 + N.hashCode(key);
    }

    /**
     * Returns {@code true} if {@code obj} is an {@code IndexedKeyed} with equal {@code index} and {@code key}.
     *
     * <p>{@code value} is not considered.</p>
     *
     * @param obj object to compare with
     * @return {@code true} if equal by index and key; otherwise {@code false}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof IndexedKeyed another) {
            return N.equals(another.index, index) && N.equals(another.key, key);
        }

        return false;
    }

    /**
     * Returns a string representation in the form {@code "{index=..., key=..., val=...}"}.
     *
     * @return string form of this object
     */
    @Override
    public String toString() {
        return "{index=" + index + ", key=" + key + ", val=" + val + "}";
    }
}
