/*
 * Copyright (C) 2018 HaiYang Li
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
 * Indicates that the annotated element is null-safe and properly handles null values.
 * This annotation can be applied to constructors, methods, and parameters to document
 * that they have been designed to handle null inputs without throwing NullPointerException.
 * 
 * <p>When applied to:</p>
 * <ul>
 *   <li>A method or constructor: indicates it can safely handle null arguments</li>
 *   <li>A parameter: indicates that null is an acceptable value for that parameter</li>
 * </ul>
 * 
 * <p>This annotation is for documentation and may be used by static analysis tools
 * to verify null-safety contracts.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER })
public @interface NullSafe {
}
