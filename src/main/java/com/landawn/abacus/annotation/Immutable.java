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
 * Indicates that the annotated type is immutable or that the annotated method returns an immutable
 * value. It does not, by itself, claim that a method is pure or that it has no other side effects.
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
 *   <li>The object's own value structure cannot be changed through its public API.</li>
 *   <li>No setter methods or other mutator methods should exist.</li>
 *   <li>Methods should return new instances rather than modifying the current instance.</li>
 *   <li>Mutable implementation state that is part of the annotated value should be encapsulated.</li>
 *   <li>Implementations should prevent subclasses from exposing mutability; making a concrete
 *       implementation final is one common approach.</li>
 * </ul>
 *
 * <p>This is a shallow structural contract. An annotated value may contain mutable elements or be
 * an unmodifiable view backed by mutable storage when that behavior is documented. The annotation
 * therefore does not by itself promise deep immutability, thread safety, safe publication, or a
 * stable hash code.</p>
 *
 * <p><b>When applied to methods:</b></p>
 * <ul>
 *   <li>The returned value is immutable (or an immutable view, as documented by the method).</li>
 *   <li>Callers must not infer that the method is side-effect-free solely from this annotation.</li>
 * </ul>
 *
 * <p><b>Benefits of immutability:</b></p>
 * <ul>
 *   <li>Structurally immutable APIs reduce accidental mutation.</li>
 *   <li>Easier to reason about and debug.</li>
 *   <li>Values whose elements and backing storage are also immutable can be safely cached and shared.</li>
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
 * <p>This annotation has {@link RetentionPolicy#CLASS} retention and serves as API documentation.
 * A static-analysis tool may consume it if explicitly configured to understand this annotation,
 * but it is not the same declaration as a tool-specific {@code @Immutable} annotation.</p>
 *
 * @see Mutable
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface Immutable {
}
