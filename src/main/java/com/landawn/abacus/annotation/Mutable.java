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
 * Indicates that the annotated type has mutable state or that the annotated method mutates state.
 * Mutability alone does not determine thread safety: a mutable implementation may synchronize its
 * state, use lock-free coordination, or require external synchronization.
 *
 * <p><b>When applied to types (classes/interfaces):</b></p>
 * <ul>
 *   <li>The type's instances have modifiable state.</li>
 *   <li>Fields may change after object construction.</li>
 *   <li>Consult the type's own contract before sharing instances between threads.</li>
 * </ul>
 *
 * <p><b>When applied to methods:</b></p>
 * <ul>
 *   <li>The method modifies the object's state.</li>
 *   <li>The method has side effects.</li>
 *   <li>Concurrent-call safety is specified separately by the declaring type.</li>
 * </ul>
 *
 * <p><b>Important considerations for mutable types:</b></p>
 * <ul>
 *   <li>Not suitable as Map keys or Set elements (unless {@code equals}/{@code hashCode} are immutable).</li>
 *   <li>Require defensive copying when passing between untrusted code.</li>
 *   <li>Need an appropriate concurrency policy when accessed from multiple threads.</li>
 *   <li>May cause unexpected behavior if aliased (multiple references).</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @Mutable
 * public class Counter {
 *     private int value = 0;
 *
 *     @Mutable
 *     public void increment() {
 *         value++;
 *     }
 *
 *     public int getValue() {
 *         return value;
 *     }
 * }
 * }</pre>
 *
 * <p>This annotation has {@link RetentionPolicy#CLASS} retention and serves as documentation.
 * Static-analysis tools may consume it if configured to understand this declaration. It is the
 * counterpart of {@link Immutable}.</p>
 *
 * @see Immutable
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface Mutable {
}
