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
 * <p><b>Name collision:</b> this annotation is {@code com.landawn.abacus.annotation.Immutable}.
 * It is distinct from {@code com.landawn.abacus.util.Immutable}, which is a utility namespace,
 * and from similarly named static-analysis annotations such as {@code net.jcip.annotations.Immutable},
 * {@code javax.annotation.concurrent.Immutable}, and {@code com.google.errorprone.annotations.Immutable}.
 * Those annotations are interpreted by their own tools and are not interchangeable with this declaration;
 * check the fully qualified import when multiple immutability APIs are available.</p>
 *
 * <p><b>When applied to types (classes/interfaces):</b></p>
 * <ul>
 *   <li>All fields should be final and initialized during construction.</li>
 *   <li>No setter methods or other mutator methods should exist.</li>
 *   <li>Methods should return new instances rather than modifying the current instance.</li>
 *   <li>Any mutable fields should be properly encapsulated (defensive copying).</li>
 *   <li>The class should be final to prevent subclassing that could add mutability.</li>
 * </ul>
 *
 * <p><b>When applied to methods:</b></p>
 * <ul>
 *   <li>The method does not modify any instance fields.</li>
 *   <li>The method does not call any mutating methods.</li>
 *   <li>The method is essentially a pure function with no side effects.</li>
 * </ul>
 *
 * <p><b>Benefits of immutability:</b></p>
 * <ul>
 *   <li>Thread-safety without synchronization.</li>
 *   <li>Safe to share between multiple contexts.</li>
 *   <li>Easier to reason about and debug.</li>
 *   <li>Can be cached and reused safely.</li>
 *   <li>Ideal for use as Map keys or Set elements.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @Immutable
 * public final class Point {
 *     private final int x;
 *     private final int y;
 *
 *     public Point(int x, int y) {
 *         this.x = x;
 *         this.y = y;
 *     }
 *
 *     @Immutable
 *     public Point translate(int dx, int dy) {
 *         return new Point(x + dx, y + dy);
 *     }
 * }
 * }</pre>
 *
 * <p>This annotation has {@link RetentionPolicy#CLASS} retention: it serves as documentation
 * and is consumed by static analysis tools (e.g., SpotBugs's {@code @ThreadSafe}/{@code Immutable}
 * checks) to verify immutability constraints. The abacus framework itself applies it on many
 * value-style classes — for example {@code com.landawn.abacus.util.Seid} and various stream
 * support types — to signal "shareable across threads without external synchronization".</p>
 *
 * @see Mutable
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface Immutable {
}
