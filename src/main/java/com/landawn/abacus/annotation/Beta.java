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
 * Indicates that the annotated API is experimental and subject to change or removal in future versions.
 * This annotation serves as a warning to developers that the API is not yet stable and should be used
 * with caution in production code.
 * 
 * <p>Beta APIs may have incomplete documentation, limited testing, or may undergo significant changes
 * without notice. They are provided to allow early access and feedback from developers.</p>
 * 
 * <p>This annotation can be applied to:</p>
 * <ul>
 *   <li>Types (classes, interfaces, enums, annotations)</li>
 *   <li>Methods and constructors</li>
 *   <li>Fields</li>
 *   <li>Other annotations</li>
 * </ul>
 * 
 * @author HaiYang Li
 * @since 2015
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Beta {

}
