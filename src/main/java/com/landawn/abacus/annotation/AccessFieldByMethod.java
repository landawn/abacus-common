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
 * This annotation is primarily used in serialization/deserialization contexts and reflection-based
 * operations to ensure that field access goes through proper accessor methods, allowing for lazy
 * initialization, validation, or other custom logic defined in those methods.
 *
 * <p>When applied to a type (class or interface), all fields within that type will be accessed via
 * their corresponding getter/setter methods. When applied to a specific field or method, only that
 * element's access behavior is affected.</p>
 *
 * <p>This annotation is useful in scenarios where:</p>
 * <ul>
 *   <li>Fields have lazy initialization logic in their getters.</li>
 *   <li>Setters perform validation or transformation of incoming values.</li>
 *   <li>Direct field access would bypass important business logic.</li>
 *   <li>Compatibility with frameworks that require method-based access is needed.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @AccessFieldByMethod
 * public class User {
 *     private String name;
 *
 *     public String getName() {
 *         return name != null ? name : "Anonymous";
 *     }
 * }
 * }</pre>
 */
@Documented
@Target(value = { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RUNTIME)
public @interface AccessFieldByMethod {

    /**
     * Reserved for future use. Currently unused; the presence of the annotation alone
     * is sufficient to indicate that field access should be performed via getter/setter methods.
     *
     * @return always an empty string (the default)
     */
    String value() default "";
}
