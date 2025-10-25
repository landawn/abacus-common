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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated type or method is immutable, meaning its state cannot be modified
 * after creation (for types) or that it does not modify any state (for methods).
 * 
 * <p><b>When applied to types (classes/interfaces):</b></p>
 * <ul>
 *   <li>All fields should be final and initialized during construction</li>
 *   <li>No setter methods or other mutator methods should exist</li>
 *   <li>Methods should return new instances rather than modifying the current instance</li>
 *   <li>Any mutable fields should be properly encapsulated (defensive copying)</li>
 *   <li>The class should be final to prevent subclassing that could add mutability</li>
 * </ul>
 * 
 * <p><b>When applied to methods:</b></p>
 * <ul>
 *   <li>The method does not modify any instance fields</li>
 *   <li>The method does not call any mutating methods</li>
 *   <li>The method is essentially a pure function with no side effects</li>
 * </ul>
 * 
 * <p><b>Benefits of immutability:</b></p>
 * <ul>
 *   <li>Thread-safety without synchronization</li>
 *   <li>Safe to share between multiple contexts</li>
 *   <li>Easier to reason about and debug</li>
 *   <li>Can be cached and reused safely</li>
 *   <li>Ideal for use as Map keys or Set elements</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * {@literal @}Immutable
 * public final class Point {
 *     private final int x;
 *     private final int y;
 *     
 *     public Point(int x, int y) {
 *         this.x = x;
 *         this.y = y;
 *     }
 *     
 *     {@literal @}Immutable
 *     public Point translate(int dx, int dy) {
 *         return new Point(x + dx, y + dy);
 *     }
 * }
 * }</pre>
 * 
 * <p>This annotation serves as documentation and can be used by static analysis tools
 * to verify immutability constraints and detect potential violations.</p>
 * 
 * @since 2020
 * @see Mutable
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface Immutable {
}
