/*
 * Copyright (C) 2019 HaiYang Li
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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that fields should be accessed via getter/setter methods rather than direct field access.
 * This annotation is primarily used in serialization/deserialization contexts and reflection-based operations
 * to ensure that field access goes through proper accessor methods, allowing for lazy initialization,
 * validation, or other custom logic.
 * 
 * <p>When applied to a type (class or interface), all fields within that type will be accessed via their
 * corresponding getter/setter methods. When applied to a specific method or field, only that element's
 * access behavior is affected.</p>
 * 
 * <p>This annotation is useful in scenarios where:</p>
 * <ul>
 *   <li>Fields have lazy initialization logic in their getters</li>
 *   <li>Setters perform validation or transformation of values</li>
 *   <li>Direct field access would bypass important business logic</li>
 *   <li>Compatibility with frameworks that require method-based access</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
 * <pre>
 * {@literal @}AccessFieldByMethod
 * public class User {
 *     private String name;
 *     
 *     public String getName() {
 *         return name != null ? name : "Anonymous";
 *     }
 * }
 * </pre>
 * 
 * @since 2019
 * @see java.lang.reflect.Field
 * @see java.lang.reflect.Method
 */
@Documented
@Target(value = { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RUNTIME)
public @interface AccessFieldByMethod {

    /**
     * Optional value to specify additional configuration for field access behavior.
     * This value can be used by frameworks to provide custom access strategies or configurations.
     * 
     * <p>The interpretation of this value is framework-specific. Common uses include:</p>
     * <ul>
     *   <li>Specifying a custom accessor pattern (e.g., "fluent" for fluent-style accessors)</li>
     *   <li>Providing hints for method name generation</li>
     *   <li>Enabling/disabling specific access behaviors</li>
     * </ul>
     * 
     * @return the configuration value, empty string by default (which uses standard getter/setter conventions)
     */
    String value() default "";
}
