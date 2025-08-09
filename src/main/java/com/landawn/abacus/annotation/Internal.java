/*
 * Copyright (C) 2015 HaiYang Li
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
 * Indicates that the annotated element is for internal use only and should not be used
 * by external code or client applications. Internal APIs may change without notice
 * and are not considered part of the public API contract.
 * 
 * <p>This annotation serves as documentation to discourage external usage and
 * can be used by IDEs or build tools to generate warnings when internal APIs are accessed
 * from outside the intended scope.</p>
 * 
 * <p>Elements marked with this annotation may:</p>
 * <ul>
 *   <li>Change or be removed in any release without deprecation</li>
 *   <li>Have incomplete or missing documentation</li>
 *   <li>Not follow the same stability guarantees as public APIs</li>
 * </ul>
 * 
 * @author HaiYang Li
 * @since 2015
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Internal {

}
