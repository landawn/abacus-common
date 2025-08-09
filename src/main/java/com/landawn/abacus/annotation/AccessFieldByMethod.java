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
 * Indicates that fields should be accessed via methods rather than direct field access.
 * This annotation can be applied to types, methods, or fields to control field access behavior.
 * 
 * <p>When applied to a type, all fields within that type will be accessed via methods.
 * When applied to a method or field specifically, only that element's access behavior is affected.</p>
 * 
 * @author HaiYang Li
 * @since 2019
 */
@Documented
@Target(value = { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RUNTIME)
public @interface AccessFieldByMethod {

    /**
     * Optional value to specify additional configuration for field access behavior.
     * 
     * @return the configuration value, empty string by default
     */
    String value() default "";
}
