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
 * Indicates that the annotated type or method represents mutable data or operations.
 * This annotation explicitly documents that state can be changed after creation,
 * which is important for understanding thread-safety and usage patterns.
 * 
 * <p><b>When applied to types (classes/interfaces):</b></p>
 * <ul>
 *   <li>The type's instances have modifiable state</li>
 *   <li>Fields may change after object construction</li>
 *   <li>The type is NOT thread-safe without external synchronization</li>
 *   <li>Instances should not be shared between threads without proper synchronization</li>
 * </ul>
 * 
 * <p><b>When applied to methods:</b></p>
 * <ul>
 *   <li>The method modifies the object's state</li>
 *   <li>The method has side effects</li>
 *   <li>Calling the method may produce different results over time</li>
 *   <li>The method is not safe to call concurrently without synchronization</li>
 * </ul>
 * 
 * <p><b>Important considerations for mutable types:</b></p>
 * <ul>
 *   <li>Not suitable as Map keys or Set elements (unless equals/hashCode are immutable)</li>
 *   <li>Require defensive copying when passing between untrusted code</li>
 *   <li>Need synchronization for thread-safe access</li>
 *   <li>May cause unexpected behavior if aliased (multiple references)</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * {@literal @}Mutable
 * public class Counter {
 *     private int value = 0;
 *     
 *     {@literal @}Mutable
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
 * <p>This annotation serves as documentation and can be used by static analysis tools
 * to verify mutability contracts and detect potential issues with concurrent access
 * or inappropriate usage patterns.</p>
 * 
 * @since 2020
 * @see Immutable
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface Mutable {
}
