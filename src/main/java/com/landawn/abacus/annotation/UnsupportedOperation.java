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
package com.landawn.abacus.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks methods that represent unsupported operations in an implementation.
 * This annotation indicates that the annotated method will throw an {@link UnsupportedOperationException}
 * when invoked, typically used in partial implementations or read-only views of data structures.
 * 
 * <p>Common use cases include:</p>
 * <ul>
 *   <li>Immutable collection implementations where modification methods are unsupported</li>
 *   <li>Read-only wrappers or views that prohibit state changes</li>
 *   <li>Interface implementations where certain operations are not applicable</li>
 *   <li>Abstract base classes with optional operations that subclasses may not support</li>
 * </ul>
 * 
 * <p>This annotation serves as documentation for API users, making it clear at compile-time
 * which operations are not supported, rather than discovering it at runtime.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class ImmutableList<E> implements List<E> {
 *     @UnsupportedOperation
 *     @Override
 *     public boolean add(E element) {
 *         throw new UnsupportedOperationException("Cannot modify an immutable list");
 *     }
 *     
 *     @UnsupportedOperation
 *     @Override
 *     public E remove(int index) {
 *         throw new UnsupportedOperationException("Cannot modify an immutable list");
 *     }
 *
 *     @UnsupportedOperation
 *     @Override
 *     public void clear() {
 *         throw new UnsupportedOperationException("Cannot modify an immutable list");
 *     }
 * }
 * }</pre>
 *
 * @see UnsupportedOperationException
 * @see Immutable
 * @see ReadOnly
 */
@Documented
@Target(value = { METHOD })
@Retention(RUNTIME)
public @interface UnsupportedOperation {

}
