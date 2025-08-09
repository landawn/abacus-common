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
 * Marks a method as a terminal operation in stream-like or pipeline processing.
 * Terminal operations consume the stream or pipeline and produce a final result,
 * triggering the execution of any pending intermediate operations.
 * 
 * <p>This annotation is used for documentation purposes and to indicate methods that:</p>
 * <ul>
 *   <li>Consume the entire stream or pipeline</li>
 *   <li>Produce a final result or side effect</li>
 *   <li>Trigger execution of lazy intermediate operations</li>
 *   <li>Cannot be chained with other operations</li>
 * </ul>
 * 
 * <p>Examples of terminal operations include collect, reduce, forEach, findFirst, etc.</p>
 * 
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html">java.util.Stream</a>
 * @see IntermediateOp
 * @author HaiYang Li
 * @since 2018
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD })
public @interface TerminalOp {

}
