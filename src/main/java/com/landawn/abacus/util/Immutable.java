/*
 * Copyright (C) 2020 HaiYang Li
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
 * Marker interface for objects whose own structure cannot be modified through their public API.
 *
 * <p>This is a <em>shallow</em> contract. An implementation may contain or expose mutable values,
 * or may be an unmodifiable view backed by a mutable object. Consequently, this marker alone does
 * not guarantee deep immutability, thread safety, safe publication, or a stable hash code. Consult
 * the implementing class's contract before sharing an instance between threads or using it as a
 * map key.</p>
 *
 * <p>Collection and map implementations carrying this marker reject their own mutating operations,
 * normally with {@link UnsupportedOperationException}. Mutating an element, a value, or a backing
 * collection may still change their observable contents.</p>
 *
 * <p>Common implementations include:</p>
 * <ul>
 * <li>{@link ImmutableList} - An immutable list implementation</li>
 * <li>{@link ImmutableSet} - An immutable set implementation</li>
 * <li>{@link ImmutableMap} - An immutable map implementation</li>
 * <li>{@link ImmutableArray} - An immutable array wrapper</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check whether the object's public API is structurally read-only.
 * if (myCollection instanceof Immutable) {
 *     // Inspect the concrete type's element/backing-store contract before sharing it.
 * }
 * }</pre>
 *
 * @see ImmutableCollection
 * @see ImmutableList
 * @see ImmutableSet
 * @see ImmutableMap
 */
@com.landawn.abacus.annotation.Immutable
public interface Immutable {

}
