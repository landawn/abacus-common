/*
 * Copyright (C) 2018, 2019 HaiYang Li
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
 * Indicates that the annotated method or type must be executed sequentially
 * and does not support parallel execution. This annotation documents that the
 * implementation is not thread-safe and requires sequential access to maintain correctness.
 * 
 * <p>When applied to a method, it indicates that the method must not be called
 * from multiple threads simultaneously. When applied to a type, it indicates that
 * the type's operations must be executed sequentially.</p>
 * 
 * <p>This annotation is used to mark:</p>
 * <ul>
 *   <li>Stream operations that cannot be parallelized</li>
 *   <li>Stateful operations that require sequential processing</li>
 *   <li>Algorithms that depend on processing order</li>
 *   <li>Non-thread-safe utility methods</li>
 * </ul>
 * 
 * @see ParallelSupported
 * @since 2018
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD, ElementType.TYPE })
public @interface SequentialOnly {

}
