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
 * An immutable holder of {@code index}, {@code key}, and {@code val}.
 *
 * <p>This type extends {@link Keyed} and adds positional information. Equality and hash code are
 * based on {@code index} and {@code key} only; {@code val} is intentionally ignored.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * IndexedKeyed<String, User> indexed = IndexedKeyed.of("user123", new User("John"), 0);
 * System.out.println(indexed.index()); // 0
 * System.out.println(indexed.key());   // user123
 * System.out.println(indexed.val());   // User("John")
 * }</pre>
 *
 * @param <K> the type of the key component
 * @param <T> the type of the value component
 * @see Keyed
 * @see Wrapper
 * @see Indexed
 */
@com.landawn.abacus.annotation.Immutable
public final class IndexedKeyed<K, T> extends Keyed<K, T> {

    /** The index position associated with this object. */
    private final int index;

    /**
     * Constructs a new {@code IndexedKeyed} with the specified key, value, and index.
     * This constructor has package-private visibility; use {@link #of(Object, Object, int)} to create instances.
     *
     * @param key the key component (can be {@code null})
     * @param val the value component (can be {@code null})
     * @param index the index component
     */
    IndexedKeyed(final K key, final T val, final int index) {
        super(key, val);
        this.index = index;
    }

    /**
     * Creates an {@code IndexedKeyed} with the specified key, value, and index.
     *
     * @param <K> the type of the key component
     * @param <T> the type of the value component
     * @param key the key component (can be {@code null})
     * @param val the value component (can be {@code null})
     * @param index the index component
     * @return a new immutable {@code IndexedKeyed} containing the specified key, value, and index
     */
    public static <K, T> IndexedKeyed<K, T> of(final K key, final T val, final int index) {
        return new IndexedKeyed<>(key, val, index);
    }

    /**
     * Returns the index component of this {@code IndexedKeyed}.
     *
     * @return the index value
     */
    public int index() {
        return index;
    }

    /**
     * Returns a hash code based on {@code index} and {@code key}.
     *
     * <p>The {@code val} component is not included in the hash code computation,
     * consistent with the equality contract of this class.</p>
     *
     * @return hash code for this object
     */
    @Override
    public int hashCode() {
        return N.hashCode(index) * 31 + N.hashCode(key);
    }

    /**
     * Returns {@code true} if {@code obj} is an {@code IndexedKeyed} with the same {@code index} and {@code key}.
     *
     * <p>The {@code val} component is intentionally excluded from equality comparison.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if equal by index and key; {@code false} otherwise
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
     * Returns a string representation in the form {@code "{index=<index>, key=<key>, val=<val>}"}.
     *
     * <p>For example: {@code {index=0, key=user123, val=John}}</p>
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "{index=" + index + ", key=" + key + ", val=" + val + "}";
    }
}
