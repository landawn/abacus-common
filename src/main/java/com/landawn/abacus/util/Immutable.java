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
 * A marker interface for immutable data structures in the Abacus framework.
 * Classes implementing this interface indicate that their instances are immutable
 * and cannot be modified after creation.
 * 
 * <p>This interface serves as a type marker for immutable collections, maps, and other
 * data structures. All mutating operations on implementing classes should throw
 * {@link UnsupportedOperationException}.</p>
 * 
 * <p>Immutable objects provide several benefits:
 * <ul>
 * <li>Thread-safety: Immutable objects can be shared between threads without synchronization</li>
 * <li>Simplicity: No need to worry about defensive copying or unexpected modifications</li>
 * <li>Caching: Immutable objects can be cached and reused safely</li>
 * <li>Hash stability: Hash codes never change, making them ideal for use as map keys</li>
 * </ul>
 * </p>
 * 
 * <p>Common implementations include:
 * <ul>
 * <li>{@link ImmutableList} - An immutable list implementation</li>
 * <li>{@link ImmutableSet} - An immutable set implementation</li>
 * <li>{@link ImmutableMap} - An immutable map implementation</li>
 * <li>{@link ImmutableArray} - An immutable array wrapper</li>
 * </ul>
 * </p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if an object is immutable
 * if (myCollection instanceof Immutable) {
 *     // Safe to share without defensive copying
 *     sharedCollection = myCollection;
 * }
 * }</pre>
 * </p>
 * 
 * @see ImmutableCollection
 * @see ImmutableList
 * @see ImmutableSet
 * @see ImmutableMap
 * @since 1.0
 */
@com.landawn.abacus.annotation.Immutable
public interface Immutable {

}