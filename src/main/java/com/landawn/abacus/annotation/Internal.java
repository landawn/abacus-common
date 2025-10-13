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
 * by external code or client applications. Elements marked as internal are part of the
 * implementation details and not the public API contract.
 * 
 * <p><b>Important considerations:</b></p>
 * <ul>
 *   <li>Internal APIs may change or be removed in any release without notice</li>
 *   <li>No deprecation period is provided for internal APIs</li>
 *   <li>Breaking changes can occur in minor or patch releases</li>
 *   <li>Documentation may be incomplete or focused on implementation details</li>
 *   <li>No compatibility guarantees are provided</li>
 * </ul>
 * 
 * <p><b>This annotation can be applied to:</b></p>
 * <ul>
 *   <li>Classes and interfaces that are implementation details</li>
 *   <li>Constructors that should not be called externally</li>
 *   <li>Methods that are meant for framework use only</li>
 *   <li>Fields that should not be accessed directly</li>
 *   <li>Nested types used for internal organization</li>
 * </ul>
 * 
 * <p><b>Tool support:</b></p>
 * <p>IDEs and build tools may use this annotation to:</p>
 * <ul>
 *   <li>Generate warnings when internal APIs are accessed</li>
 *   <li>Exclude internal elements from public API documentation</li>
 *   <li>Enforce access restrictions in modular systems</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
 * <pre>
 * {@literal @}Internal
 * public class InternalHelper {
 *     // This class should not be used by external code
 * }
 * 
 * public class PublicAPI {
 *     {@literal @}Internal
 *     public void frameworkMethod() {
 *         // This method is for framework use only
 *     }
 * }
 * </pre>
 * 
 * @since 2015
 * @see Beta
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Internal {

}
